package com.zhaoxinms.contract.tools.ocrcompare.compare;

import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;

// PDF处理相关导入
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationTextMarkup;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.PostConstruct;
import org.springframework.web.multipart.MultipartFile;

// JSON处理相关导入
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.zhaoxinms.contract.tools.compare.DiffUtil;
import com.zhaoxinms.contract.tools.compare.util.TextNormalizer;
import com.zhaoxinms.contract.tools.ocr.DiffProcessingUtil;
import com.zhaoxinms.contract.tools.ocr.TextExtractionUtil;
import com.zhaoxinms.contract.tools.ocr.dotsocr.DotsOcrClient;
import com.zhaoxinms.contract.tools.ocr.model.CharBox;
import com.zhaoxinms.contract.tools.ocr.model.DiffBlock;
import com.zhaoxinms.contract.tools.ocrcompare.config.GPUOCRConfig;
import com.zhaoxinms.contract.tools.config.ZxcmConfig;
import com.zhaoxinms.contract.tools.ocrcompare.util.OcrImageSaver;

/**
 * GPU OCR比对服务 - 基于DotsOcrCompareDemoTest的完整比对功能
 */
@Service
public class GPUOCRCompareService {

    @Autowired
    private GPUOCRConfig gpuOcrConfig;
    
    @Autowired
    private ZxcmConfig zxcmConfig;

    @Autowired
    private OcrImageSaver ocrImageSaver;

    private final ConcurrentHashMap<String, GPUOCRCompareTask> tasks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, GPUOCRCompareResult> results = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Map<String, Object>> frontendResults = new ConcurrentHashMap<>();
    private ExecutorService executorService;
    private static final ObjectMapper M = new ObjectMapper()
            .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature())
            .enable(JsonReadFeature.ALLOW_MISSING_VALUES.mappedFeature());

    @PostConstruct
    public void init() {
        // 使用配置的并行线程数初始化线程池
        this.executorService = Executors.newFixedThreadPool(gpuOcrConfig.getParallelThreads());
        System.out.println("GPU OCR比对服务初始化完成，线程池大小: " + gpuOcrConfig.getParallelThreads());
        
        // 启动时加载已完成的任务到内存中
        loadCompletedTasks();
    }
    
    /**
     * 加载已完成的任务到内存中
     */
    private void loadCompletedTasks() {
        try {
            String uploadRootPath = zxcmConfig.getFileUpload().getRootPath();
            Path resultsDir = Paths.get(uploadRootPath, "gpu-ocr-compare", "results");
            
            if (Files.exists(resultsDir)) {
                Files.list(resultsDir)
                    .filter(path -> path.toString().endsWith(".json"))
                    .forEach(jsonFile -> {
                        try {
                            String fileName = jsonFile.getFileName().toString();
                            String taskId = fileName.substring(0, fileName.lastIndexOf(".json"));
                            
                            // 加载任务状态到内存
                            GPUOCRCompareTask task = loadTaskFromFile(taskId);
                            if (task != null) {
                                tasks.put(taskId, task);
                                System.out.println("启动时加载任务: " + taskId);
                            }
                        } catch (Exception e) {
                            System.err.println("加载任务失败: " + jsonFile + ", error=" + e.getMessage());
                        }
                    });
            }
            
            // 也检查前端结果目录
            Path frontendResultsDir = Paths.get(uploadRootPath, "gpu-ocr-compare", "frontend-results");
            if (Files.exists(frontendResultsDir)) {
                Files.list(frontendResultsDir)
                    .filter(path -> path.toString().endsWith(".json"))
                    .forEach(jsonFile -> {
                        try {
                            String fileName = jsonFile.getFileName().toString();
                            String taskId = fileName.substring(0, fileName.lastIndexOf(".json"));
                            
                            // 如果内存中还没有这个任务，加载它
                            if (!tasks.containsKey(taskId)) {
                                GPUOCRCompareTask task = loadTaskFromFile(taskId);
                                if (task != null) {
                                    tasks.put(taskId, task);
                                    System.out.println("启动时加载任务(前端结果): " + taskId);
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("加载任务失败: " + jsonFile + ", error=" + e.getMessage());
                        }
                    });
            }
            
            System.out.println("启动时共加载了 " + tasks.size() + " 个已完成的任务");
            
        } catch (Exception e) {
            System.err.println("启动时加载任务失败: " + e.getMessage());
        }
    }

    /**
     * 提交比对任务（文件上传）
     */
    public String submitCompareTask(MultipartFile oldFile, MultipartFile newFile, GPUOCRCompareOptions options) {
        String taskId = UUID.randomUUID().toString();

        GPUOCRCompareTask task = new GPUOCRCompareTask(taskId);
        task.setOldFileName(oldFile.getOriginalFilename());
        task.setNewFileName(newFile.getOriginalFilename());
        task.setStatus(GPUOCRCompareTask.Status.PENDING);

        tasks.put(taskId, task);

        try {
            // 同步保存文件到系统上传目录，避免异步处理时文件流被关闭
            String uploadRootPath = zxcmConfig.getFileUpload().getRootPath();
            Path uploadDir = Paths.get(uploadRootPath, "gpu-ocr-compare", "tasks", taskId);
            Files.createDirectories(uploadDir);

            Path oldFilePath = uploadDir.resolve("old_" + oldFile.getOriginalFilename());
            Path newFilePath = uploadDir.resolve("new_" + newFile.getOriginalFilename());

            // 同步保存文件，确保文件流被正确关闭
            try (var oldInputStream = oldFile.getInputStream();
                 var newInputStream = newFile.getInputStream()) {
                Files.copy(oldInputStream, oldFilePath);
                Files.copy(newInputStream, newFilePath);
            }

            System.out.println("文件已保存到系统上传目录:");
            System.out.println("  原文档: " + oldFilePath.toAbsolutePath());
            System.out.println("  新文档: " + newFilePath.toAbsolutePath());

            // 异步执行比对任务（使用文件路径而不是MultipartFile）
            executorService.submit(() -> executeCompareTaskWithPaths(task, oldFilePath.toString(), newFilePath.toString(), options));

        } catch (Exception e) {
            task.setStatus(GPUOCRCompareTask.Status.FAILED);
            task.setErrorMessage("文件保存失败: " + e.getMessage());
            System.err.println("文件保存失败: " + e.getMessage());
            e.printStackTrace();
        }

        return taskId;
    }

    /**
     * 提交比对任务（文件路径）
     */
    public String submitCompareTaskWithPaths(String oldFilePath, String newFilePath, GPUOCRCompareOptions options) {
        String taskId = UUID.randomUUID().toString();

        GPUOCRCompareTask task = new GPUOCRCompareTask(taskId);
        task.setOldFileName(Paths.get(oldFilePath).getFileName().toString());
        task.setNewFileName(Paths.get(newFilePath).getFileName().toString());
        task.setStatus(GPUOCRCompareTask.Status.PENDING);

        tasks.put(taskId, task);

        // 异步执行比对任务
        executorService.submit(() -> executeCompareTaskWithPaths(task, oldFilePath, newFilePath, options));

        return taskId;
    }

    /**
     * 调试模式：使用已有任务结果进行重新分析
     */
    public String debugCompareWithTaskId(String taskId, GPUOCRCompareOptions options) {
        String debugTaskId = UUID.randomUUID().toString();

        GPUOCRCompareTask debugTask = new GPUOCRCompareTask(debugTaskId);
        debugTask.setOldFileName("debug_old.pdf");
        debugTask.setNewFileName("debug_new.pdf");
        debugTask.setStatus(GPUOCRCompareTask.Status.PENDING);

        tasks.put(debugTaskId, debugTask);

        // 异步执行调试比对任务
        executorService.submit(() -> executeDebugCompareTaskWithExistingResult(debugTask, taskId, options));

        return debugTaskId;
    }

    /**
     * 获取任务状态
     */
    public GPUOCRCompareTask getTaskStatus(String taskId) {
        // 首先从内存中获取
        GPUOCRCompareTask task = tasks.get(taskId);
        if (task != null) {
            return task;
        }
        
        // 如果内存中没有，尝试从文件加载
        task = loadTaskFromFile(taskId);
        if (task != null) {
            // 加载到内存中，避免重复文件读取
            tasks.put(taskId, task);
            return task;
        }
        
        return null;
    }

    /**
     * 从文件加载任务状态
     */
    private GPUOCRCompareTask loadTaskFromFile(String taskId) {
        try {
            // 检查任务目录是否存在
            String uploadRootPath = zxcmConfig.getFileUpload().getRootPath();
            Path taskDir = Paths.get(uploadRootPath, "gpu-ocr-compare", "tasks", taskId);
            if (!Files.exists(taskDir)) {
                return null;
            }
            
            // 检查是否有result.json文件（表示任务已完成）
            Path resultJsonPath = Paths.get(uploadRootPath, "gpu-ocr-compare", "results", taskId + ".json");
            if (Files.exists(resultJsonPath)) {
                // 从result.json中提取任务信息
                byte[] bytes = Files.readAllBytes(resultJsonPath);
                @SuppressWarnings("unchecked")
                Map<String, Object> resultData = M.readValue(bytes, Map.class);
                
                GPUOCRCompareTask task = new GPUOCRCompareTask(taskId);
                task.setOldFileName((String) resultData.get("oldFileName"));
                task.setNewFileName((String) resultData.get("newFileName"));
                task.setStatus(GPUOCRCompareTask.Status.COMPLETED);
                task.setOldPdfUrl((String) resultData.get("oldPdfUrl"));
                task.setNewPdfUrl((String) resultData.get("newPdfUrl"));
                task.setAnnotatedOldPdfUrl((String) resultData.get("annotatedOldPdfUrl"));
                task.setAnnotatedNewPdfUrl((String) resultData.get("annotatedNewPdfUrl"));
                
                System.out.println("从文件加载任务状态: " + taskId + " (已完成)");
                return task;
            }
            
            // 检查是否有前端结果文件
            Path frontendResultPath = getFrontendResultJsonPath(taskId);
            if (Files.exists(frontendResultPath)) {
                byte[] bytes = Files.readAllBytes(frontendResultPath);
                @SuppressWarnings("unchecked")
                Map<String, Object> frontendData = M.readValue(bytes, Map.class);
                
                GPUOCRCompareTask task = new GPUOCRCompareTask(taskId);
                task.setOldFileName((String) frontendData.get("oldFileName"));
                task.setNewFileName((String) frontendData.get("newFileName"));
                task.setStatus(GPUOCRCompareTask.Status.COMPLETED);
                task.setOldPdfUrl((String) frontendData.get("oldPdfUrl"));
                task.setNewPdfUrl((String) frontendData.get("newPdfUrl"));
                task.setAnnotatedOldPdfUrl((String) frontendData.get("annotatedOldPdfUrl"));
                task.setAnnotatedNewPdfUrl((String) frontendData.get("annotatedNewPdfUrl"));
                
                System.out.println("从文件加载任务状态: " + taskId + " (前端结果)");
                return task;
            }
            
        } catch (Exception e) {
            System.err.println("从文件加载任务状态失败: taskId=" + taskId + ", error=" + e.getMessage());
        }
        
        return null;
    }

    /**
     * 获取比对结果
     */
    public GPUOCRCompareResult getCompareResult(String taskId) {
        GPUOCRCompareTask task = getTaskStatus(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }

        if (!task.isCompleted()) {
            throw new RuntimeException("任务未完成");
        }

        // 首先尝试从结果存储中获取完整结果
        GPUOCRCompareResult result = results.get(taskId);
        if (result != null) {
            return result;
        }

        // 如果没有找到完整结果（可能是旧任务），构造一个基本的返回结果
        result = new GPUOCRCompareResult(taskId);
        result.setOldFileName(task.getOldFileName());
        result.setNewFileName(task.getNewFileName());
        result.setOldPdfUrl(task.getOldPdfUrl());
        result.setNewPdfUrl(task.getNewPdfUrl());
        result.setAnnotatedOldPdfUrl(task.getAnnotatedOldPdfUrl());
        result.setAnnotatedNewPdfUrl(task.getAnnotatedNewPdfUrl());

        return result;
    }

    /**
     * 获取原始前端格式的比对结果（未经坐标转换）
     */
    public Map<String, Object> getRawFrontendResult(String taskId) {
        Map<String, Object> cached = frontendResults.get(taskId);
        if (cached != null) {
            return cached;
        }
        // 尝试从文件加载
        try {
            Path p = getFrontendResultJsonPath(taskId);
            if (Files.exists(p)) {
                byte[] bytes = Files.readAllBytes(p);
                @SuppressWarnings("unchecked")
                Map<String, Object> fromFile = M.readValue(bytes, Map.class);
                // 放入缓存以便后续快速读取
                frontendResults.put(taskId, fromFile);
                System.out.println("前端结果已从文件读取: " + p.toAbsolutePath());
                return fromFile;
            }
        } catch (Exception e) {
            System.err.println("读取前端结果JSON文件失败: taskId=" + taskId + ", error=" + e.getMessage());
        }
        return null;
    }

    /**
     * 获取前端格式的比对结果（在接口层进行坐标转换）
     */
    public Map<String, Object> getFrontendResult(String taskId) {

        Map<String, Object> originalFrontendResult = getRawFrontendResult(taskId);
        if (originalFrontendResult == null) {
            return null;
        }

        // 获取对应的GPUOCRCompareResult以获取缩放参数
        GPUOCRCompareResult result = results.get(taskId);
        if (result == null) {
            return originalFrontendResult; // 如果没有result对象，直接返回原始数据
        }

        // 输出缩放参数信息
//        System.out.println(String.format("坐标转换调试 - 缩放参数: old[%.4f, %.4f, height=%.2f], new[%.4f, %.4f, height=%.2f]",
//            result.getOldPdfScaleX(), result.getOldPdfScaleY(), result.getOldPdfPageHeight(),
//            result.getNewPdfScaleX(), result.getNewPdfScaleY(), result.getNewPdfPageHeight()));

        // 对differences中的bbox坐标进行转换
        // 深拷贝，避免修改缓存/文件中的原始数据
        Map<String, Object> convertedFrontendResult = new HashMap<>(originalFrontendResult);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> differences = (List<Map<String, Object>>) convertedFrontendResult.get("differences");
        if (differences != null && !differences.isEmpty()) {

            List<Map<String, Object>> convertedDifferences = convertDifferencesToPdfCoords(differences, result);
            convertedFrontendResult.put("differences", convertedDifferences);
        } else {
            //System.out.println("坐标转换调试 - differences为空或没有差异项");
        }

        return convertedFrontendResult;
    }

    /**
     * 将differences中的图像坐标转换为PDF坐标
     */
    private List<Map<String, Object>> convertDifferencesToPdfCoords(List<Map<String, Object>> differences, GPUOCRCompareResult result) {

        List<Map<String, Object>> converted = new ArrayList<>();

        for (int i = 0; i < differences.size(); i++) {
            Map<String, Object> diff = differences.get(i);
            Map<String, Object> convertedDiff = new HashMap<>(diff);

            // 转换oldBbox坐标
            if (diff.containsKey("oldBbox")) {
                Object oldBboxObj = diff.get("oldBbox");
                if (oldBboxObj instanceof double[]) {
                    double[] originalBbox = (double[]) oldBboxObj;
                    double[] pdfBbox = convertImageCoordsToPdfCoords(originalBbox,
                        result.getOldPdfScaleX(), result.getOldPdfScaleY(), result.getOldPdfPageHeight());
                    convertedDiff.put("oldBbox", pdfBbox);

                } else {
                    //System.out.println("坐标转换调试 - oldBbox不是double[]类型: " + oldBboxObj.getClass());
                }
            }

            // 转换newBbox坐标
            if (diff.containsKey("newBbox")) {
                Object newBboxObj = diff.get("newBbox");
                if (newBboxObj instanceof double[]) {
                    double[] originalBbox = (double[]) newBboxObj;

                    double[] pdfBbox = convertImageCoordsToPdfCoords(originalBbox,
                        result.getNewPdfScaleX(), result.getNewPdfScaleY(), result.getNewPdfPageHeight());
                    convertedDiff.put("newBbox", pdfBbox);

                } else {
                    //System.out.println("坐标转换调试 - newBbox不是double[]类型: " + newBboxObj.getClass());
                }
            }

            // 转换prevOldBbox坐标
            if (diff.containsKey("prevOldBbox")) {
                Object prevOldBboxObj = diff.get("prevOldBbox");
                if (prevOldBboxObj instanceof double[]) {
                    double[] originalBbox = (double[]) prevOldBboxObj;

                    double[] pdfBbox = convertImageCoordsToPdfCoords(originalBbox,
                        result.getOldPdfScaleX(), result.getOldPdfScaleY(), result.getOldPdfPageHeight());
                    convertedDiff.put("prevOldBbox", pdfBbox);

                } else {
                    //System.out.println("坐标转换调试 - prevOldBbox不是double[]类型: " + prevOldBboxObj.getClass());
                }
            }

            // 转换prevNewBbox坐标
            if (diff.containsKey("prevNewBbox")) {
                Object prevNewBboxObj = diff.get("prevNewBbox");
                if (prevNewBboxObj instanceof double[]) {
                    double[] originalBbox = (double[]) prevNewBboxObj;

                    double[] pdfBbox = convertImageCoordsToPdfCoords((double[]) prevNewBboxObj,
                        result.getNewPdfScaleX(), result.getNewPdfScaleY(), result.getNewPdfPageHeight());
                    convertedDiff.put("prevNewBbox", pdfBbox);

                } else {
                    //System.out.println("坐标转换调试 - prevNewBbox不是double[]类型: " + prevNewBboxObj.getClass());
                }
            }

            converted.add(convertedDiff);
        }
        //System.out.println("坐标转换调试 - differences坐标转换完成，共处理" + converted.size() + "个差异项");
        return converted;
    }

    /**
     * 获取所有任务
     */
    public List<GPUOCRCompareTask> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }

    /**
     * 删除任务
     */
    public boolean deleteTask(String taskId) {
        GPUOCRCompareTask task = tasks.remove(taskId);
        return task != null;
    }


    /**
     * 执行比对任务（文件路径）
     */
    private void executeCompareTaskWithPaths(GPUOCRCompareTask task, String oldFilePath, String newFilePath, GPUOCRCompareOptions options) {
        long startTime = System.currentTimeMillis();
        System.out.println("开始GPU OCR正常比对任务: " + task.getTaskId());

        try {
            task.setStatus(GPUOCRCompareTask.Status.OCR_PROCESSING);
            task.updateProgress(1, "初始化OCR客户端");

            // 创建OCR客户端
            DotsOcrClient client = new DotsOcrClient.Builder()
                    .baseUrl(gpuOcrConfig.getOcrBaseUrl())
                    .defaultModel(gpuOcrConfig.getOcrModel())
                    .build();

            task.updateProgress(2, "OCR识别第一个文档");

            // OCR识别第一个文档
            Path oldPath = Paths.get(oldFilePath);
            
            // 保存第一个文档的OCR图片
            if (gpuOcrConfig.isSaveOcrImages()) {
                try {
                    ocrImageSaver.saveOcrImages(oldPath, task.getTaskId(), "old");
                } catch (Exception e) {
                    System.err.println("保存第一个文档OCR图片失败: " + e.getMessage());
                }
            }
            
            List<CharBox> seqA = recognizePdfAsCharSeq(client, oldPath, null, false, options);

            task.updateProgress(3, "OCR识别第二个文档");

            // OCR识别第二个文档
            Path newPath = Paths.get(newFilePath);
            
            // 保存第二个文档的OCR图片
            if (gpuOcrConfig.isSaveOcrImages()) {
                try {
                    ocrImageSaver.saveOcrImages(newPath, task.getTaskId(), "new");
                } catch (Exception e) {
                    System.err.println("保存第二个文档OCR图片失败: " + e.getMessage());
                }
            }
            
            List<CharBox> seqB = recognizePdfAsCharSeq(client, newPath, null, false, options);

            long ocrTime = System.currentTimeMillis() - startTime;
            task.updateProgress(4, "OCR识别完成，开始文本比对");

            System.out.println(String.format("OCR完成。A=%d字符, B=%d字符, 耗时=%dms",
                    seqA.size(), seqB.size(), ocrTime));

            // 文本处理和差异分析（使用TextNormalizer进行完整预处理）
            String normA = preprocessTextForComparison(joinWithLineBreaks(seqA), options);
            String normB = preprocessTextForComparison(joinWithLineBreaks(seqB), options);

            task.updateProgress(5, "执行差异分析");

            DiffUtil dmp = new DiffUtil();
            dmp.Diff_EditCost = 6;
            LinkedList<DiffUtil.Diff> diffs = dmp.diff_main(normA, normB);
            dmp.diff_cleanupEfficiency(diffs);

            task.updateProgress(6, "生成差异块");

            List<DiffBlock> rawBlocks = DiffProcessingUtil.splitDiffsByBounding(diffs, seqA, seqB);
            List<DiffBlock> filteredBlocks = DiffProcessingUtil.filterIgnoredDiffBlocks(rawBlocks, seqA, seqB);

            task.updateProgress(7, "合并差异块");

            System.out.println("开始合并差异块，filteredBlocks大小: " + filteredBlocks.size());
            List<DiffBlock> merged = mergeBlocksByBbox(filteredBlocks);
            System.out.println("合并完成，merged大小: " + merged.size());

            task.updateProgress(8, "比对完成");

            System.out.println(String.format("差异分析完成。原始差异块=%d, 过滤后=%d, 合并后=%d",
                rawBlocks.size(), filteredBlocks.size(), merged.size()));

            // ===== 将比对结果映射为坐标并标注到PDF上 =====
            System.out.println("开始PDF标注步骤...");
            task.updateProgress(9, "开始PDF标注");

            // 1) 为 normA/normB 构建索引映射（规范化文本位置 → 原始 CharBox 索引）
            System.out.println("构建索引映射...");
            IndexMap mapA = buildNormalizedIndexMap(seqA);
            IndexMap mapB = buildNormalizedIndexMap(seqB);
            System.out.println("索引映射构建完成");

            // 2) 收集每个 diff 对应的一组矩形（可能跨多个 bbox）
            System.out.println("收集差异矩形...");
            List<RectOnPage> rectsA = collectRectsForDiffBlocks(merged, mapA, seqA, true);
            List<RectOnPage> rectsB = collectRectsForDiffBlocks(merged, mapB, seqB, false);
            System.out.println("矩形收集完成，rectsA=" + rectsA.size() + ", rectsB=" + rectsB.size());

            // 3) 渲染每页图像以获取像素尺寸，用于像素→PDF坐标换算
            System.out.println("创建渲染客户端...");
            DotsOcrClient renderClient = new DotsOcrClient.Builder()
                    .baseUrl(gpuOcrConfig.getOcrBaseUrl())
                    .defaultModel(gpuOcrConfig.getOcrModel())
                    .build();
            int dpi = gpuOcrConfig.getRenderDpi();
            System.out.println("渲染页面大小...");
            PageImageSizeProvider sizeA = renderPageSizes(oldPath, dpi);
            PageImageSizeProvider sizeB = renderPageSizes(newPath, dpi);
            System.out.println("页面大小渲染完成");

            // 4) 标注并输出PDF到系统上传目录
            String uploadRootPath = zxcmConfig.getFileUpload().getRootPath();
            Path annotatedDir = Paths.get(uploadRootPath, "gpu-ocr-compare", "annotated", task.getTaskId());
            Files.createDirectories(annotatedDir);
            
            String outPdfA = annotatedDir.resolve("old_annotated.pdf").toString();
            String outPdfB = annotatedDir.resolve("new_annotated.pdf").toString();
            System.out.println("Start annotation. rectsA=" + rectsA.size() + ", rectsB=" + rectsB.size());

            task.updateProgress(10, "标注PDF A");

            try {
                annotatePDF(oldPath, outPdfA, rectsA, sizeA);
                System.out.println("PDF A标注完成: " + outPdfA);
            } catch (Exception e) {
                System.err.println("PDF A标注失败: " + e.getMessage());
                e.printStackTrace();
            }

            task.updateProgress(11, "标注PDF B");

            try {
                annotatePDF(newPath, outPdfB, rectsB, sizeB);
                System.out.println("PDF B标注完成: " + outPdfB);
            } catch (Exception e) {
                System.err.println("PDF B标注失败: " + e.getMessage());
                e.printStackTrace();
            }

            System.out.println("开始执行保存比对结果步骤...");
            task.updateProgress(12, "保存比对结果");

            try {
                // 保存结果到任务
                System.out.println("创建GPUOCRCompareResult对象...");
                GPUOCRCompareResult result = new GPUOCRCompareResult(task.getTaskId());
                result.setOldFileName(task.getOldFileName());
                result.setNewFileName(task.getNewFileName());

                // 使用已有的页面尺寸信息计算坐标转换比例
                try {
                    // 获取PDF的实际页面尺寸（像素，72 DPI）
                    double oldPdfWidth = getPdfPageWidth(oldPath);
                    double oldPdfHeight = getPdfPageHeight(oldPath);
                    double newPdfWidth = getPdfPageWidth(newPath);
                    double newPdfHeight = getPdfPageHeight(newPath);

                    result.setOldPdfPageHeight(oldPdfHeight);
                    result.setNewPdfPageHeight(newPdfHeight);

                    // 使用已有的图像尺寸信息计算准确的缩放比例
                    // 注意：这里假设第一页的尺寸代表整个文档
                    double oldImageWidth = sizeA.widths != null && sizeA.widths.length > 0 ? sizeA.widths[0] : oldPdfWidth;
                    double oldImageHeight = sizeA.heights != null && sizeA.heights.length > 0 ? sizeA.heights[0] : oldPdfHeight;
                    double newImageWidth = sizeB.widths != null && sizeB.widths.length > 0 ? sizeB.widths[0] : newPdfWidth;
                    double newImageHeight = sizeB.heights != null && sizeB.heights.length > 0 ? sizeB.heights[0] : newPdfHeight;

                    // 计算缩放比例：图像坐标到PDF坐标的转换比例
                    // scaleX = imageWidth / pdfWidth
                    // 例如：如果图像宽度是PDF宽度的2倍，则scaleX = 2.0
                    // 从图像坐标转换到PDF坐标时，需要除以scaleX（而不是乘以）
                    double oldScaleX = oldImageWidth / oldPdfWidth;
                    double oldScaleY = oldImageHeight / oldPdfHeight;
                    double newScaleX = newImageWidth / newPdfWidth;
                    double newScaleY = newImageHeight / newPdfHeight;

                    result.setOldPdfScaleX(oldScaleX);
                    result.setOldPdfScaleY(oldScaleY);
                    result.setNewPdfScaleX(newScaleX);
                    result.setNewPdfScaleY(newScaleY);

                    System.out.println(String.format("坐标转换参数计算成功: old[%.2f,%.2f]->[%.2f,%.2f] scale[%.3f,%.3f], new[%.2f,%.2f]->[%.2f,%.2f] scale[%.3f,%.3f]",
                        oldImageWidth, oldImageHeight, oldPdfWidth, oldPdfHeight, oldScaleX, oldScaleY,
                        newImageWidth, newImageHeight, newPdfWidth, newPdfHeight, newScaleX, newScaleY));
                } catch (Exception ex) {
                    System.err.println("计算坐标转换参数失败: " + ex.getMessage());
                    // 设置默认值
                    result.setOldPdfPageHeight(1122.52);
                    result.setNewPdfPageHeight(1122.52);
                    result.setOldPdfScaleX(1.0);
                    result.setOldPdfScaleY(1.0);
                    result.setNewPdfScaleX(1.0);
                    result.setNewPdfScaleY(1.0);
                }

                // 将DiffBlock列表转换为前端期望的Map格式（保留原始图像坐标，坐标转换在接口层进行）
                System.out.println("转换DiffBlock格式，merged大小: " + merged.size());
                List<Map<String, Object>> formattedDifferences = convertDiffBlocksToMapFormat(merged);

                result.setDifferences(merged); // 保留原始的DiffBlock格式用于后端处理
                result.setFormattedDifferences(formattedDifferences); // 保存前端格式的差异数据

                // 设置PDF文件路径（相对于前端可以访问的路径）
                String baseUploadPath = "/api/gpu-ocr/files";

                // 创建包装对象用于返回前端期望的格式
                System.out.println("创建前端结果对象...");
                Map<String, Object> frontendResult = new HashMap<>();
                frontendResult.put("taskId", task.getTaskId());
                frontendResult.put("oldFileName", task.getOldFileName());
                frontendResult.put("newFileName", task.getNewFileName());
                frontendResult.put("oldPdfUrl", baseUploadPath + "/gpu-ocr-compare/tasks/" + task.getTaskId() + "/old_" + oldPath.getFileName().toString());
                frontendResult.put("newPdfUrl", baseUploadPath + "/gpu-ocr-compare/tasks/" + task.getTaskId() + "/new_" + newPath.getFileName().toString());
                frontendResult.put("annotatedOldPdfUrl", baseUploadPath + "/gpu-ocr-compare/annotated/" + task.getTaskId() + "/old_annotated.pdf");
                frontendResult.put("annotatedNewPdfUrl", baseUploadPath + "/gpu-ocr-compare/annotated/" + task.getTaskId() + "/new_annotated.pdf");
                frontendResult.put("differences", formattedDifferences);
                frontendResult.put("totalDiffCount", formattedDifferences.size());

                // 添加页面高度（坐标已预先转换为PDF坐标系）
                frontendResult.put("oldPdfPageHeight", result.getOldPdfPageHeight());
                frontendResult.put("newPdfPageHeight", result.getNewPdfPageHeight());

                // 保存前端格式的结果
                System.out.println("保存结果到缓存...");
                results.put(task.getTaskId(), result);
                frontendResults.put(task.getTaskId(), frontendResult);

                // 持久化写入磁盘，供前端或服务重启后读取
                try {
                    Path jsonPath = getFrontendResultJsonPath(task.getTaskId());
                    Files.createDirectories(jsonPath.getParent());
                    byte[] json = M.writerWithDefaultPrettyPrinter().writeValueAsBytes(frontendResult);
                    Files.write(jsonPath, json);
                    System.out.println("前端结果已写入文件: " + jsonPath.toAbsolutePath());
                } catch (Exception ioEx) {
                    System.err.println("写入前端结果JSON失败: " + ioEx.getMessage());
                }

                // 同时保存到task中（兼容现有逻辑）
                task.setOldPdfUrl(frontendResult.get("oldPdfUrl").toString());
                task.setNewPdfUrl(frontendResult.get("newPdfUrl").toString());
                task.setAnnotatedOldPdfUrl(frontendResult.get("annotatedOldPdfUrl").toString());
                task.setAnnotatedNewPdfUrl(frontendResult.get("annotatedNewPdfUrl").toString());

                System.out.println("比对结果保存完成");
            } catch (Exception ex) {
                System.err.println("保存比对结果失败: " + ex.getMessage());
                ex.printStackTrace();
            }

            task.updateProgress(13, "完成比对");

            long processingTime = System.currentTimeMillis() - startTime;
            System.out.println(String.format("GPU OCR比对完成，总耗时: %dms", processingTime));

            // 设置任务结果
            task.setStatus(GPUOCRCompareTask.Status.COMPLETED);

        } catch (Exception e) {
            task.setStatus(GPUOCRCompareTask.Status.FAILED);
            task.setErrorMessage("比对过程出错: " + e.getMessage());
            System.err.println("GPU OCR比对失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 执行调试比对任务 - 使用已有任务结果，跳过OCR，保留后续分析步骤
     */
    private void executeDebugCompareTaskWithExistingResult(GPUOCRCompareTask task, String originalTaskId, GPUOCRCompareOptions options) {
        long startTime = System.currentTimeMillis();
        System.out.println("开始GPU OCR调试比对任务: " + task.getTaskId() + ", 使用原任务ID: " + originalTaskId);

        try {
            task.setStatus(GPUOCRCompareTask.Status.OCR_PROCESSING);
            task.updateProgress(1, "读取原任务OCR结果");

            // 查找原任务的文件路径（从上传目录查找）
            String uploadRootPath = zxcmConfig.getFileUpload().getRootPath();
            Path taskDir = Paths.get(uploadRootPath, "gpu-ocr-compare", "tasks", originalTaskId);
            
            if (!Files.exists(taskDir)) {
                throw new RuntimeException("原任务目录不存在: " + taskDir);
            }
            
            // 查找原任务的PDF文件
            Path oldPdfPath = findTaskPdfFile(taskDir, "old");
            Path newPdfPath = findTaskPdfFile(taskDir, "new");
            
            if (oldPdfPath == null || newPdfPath == null) {
                throw new RuntimeException("无法找到原任务的PDF文件，目录: " + taskDir);
            }
            
            System.out.println("找到原任务PDF文件:");
            System.out.println("  旧文档: " + oldPdfPath);
            System.out.println("  新文档: " + newPdfPath);

            // 为debug模式保存OCR图片
            if (gpuOcrConfig.isSaveOcrImages()) {
                try {
                    ocrImageSaver.saveOcrImages(oldPdfPath, task.getTaskId(), "debug_old");
                    ocrImageSaver.saveOcrImages(newPdfPath, task.getTaskId(), "debug_new");
                } catch (Exception e) {
                    System.err.println("Debug模式保存OCR图片失败: " + e.getMessage());
                    // 不中断流程，继续执行
                }
            }

            task.updateProgress(2, "解析OCR数据");
            
            // 从OCR结果中提取CharBox数据（使用现有的解析方法）
            List<CharBox> seqA = parseCharBoxesFromSavedJson(oldPdfPath, options);
            List<CharBox> seqB = parseCharBoxesFromSavedJson(newPdfPath, options);
            
            if (seqA.isEmpty() || seqB.isEmpty()) {
                throw new RuntimeException("无法从OCR结果中提取字符数据");
            }

            long ocrTime = System.currentTimeMillis() - startTime;
            task.updateProgress(3, "OCR数据解析完成，开始文本比对");

            System.out.println(String.format("OCR数据解析完成。A=%d字符, B=%d字符, 耗时=%dms",
                    seqA.size(), seqB.size(), ocrTime));

            // 文本处理和差异分析（使用TextNormalizer进行完整预处理）
            String normA = preprocessTextForComparison(joinWithLineBreaks(seqA), options);
            String normB = preprocessTextForComparison(joinWithLineBreaks(seqB), options);

            task.updateProgress(4, "执行差异分析");

            DiffUtil dmp = new DiffUtil();
            dmp.Diff_EditCost = 6;
            LinkedList<DiffUtil.Diff> diffs = dmp.diff_main(normA, normB);
            dmp.diff_cleanupEfficiency(diffs);
            // 调试输出：仅打印新增/删除，不打印相等
            try {
                int ins = 0, del = 0;
                for (DiffUtil.Diff d : diffs) {
                    if (d == null) continue;
                    if (d.operation == DiffUtil.Operation.INSERT) { ins++; System.out.println("[DIFF][INSERT] " + d.text); }
                    else if (d.operation == DiffUtil.Operation.DELETE) { del++; System.out.println("[DIFF][DELETE] " + d.text); }
                }
                System.out.println("[DIFF] INSERTs=" + ins + ", DELETEs=" + del + ", TOTAL=" + diffs.size());
            } catch (Exception ignore) {}

            task.updateProgress(5, "生成差异块");

            List<DiffBlock> rawBlocks = DiffProcessingUtil.splitDiffsByBounding(diffs, seqA, seqB);
            List<DiffBlock> filteredBlocks = DiffProcessingUtil.filterIgnoredDiffBlocks(rawBlocks, seqA, seqB);

            task.updateProgress(6, "合并差异块");

            System.out.println("开始合并差异块，filteredBlocks大小: " + filteredBlocks.size());

            List<DiffBlock> merged = mergeBlocksByBbox(filteredBlocks);

            System.out.println(String.format("差异分析完成。原始差异块=%d, 过滤后=%d, 合并后=%d",
                rawBlocks.size(), filteredBlocks.size(), merged.size()));

            task.updateProgress(7, "比对完成");

            // 创建比对结果对象
            GPUOCRCompareResult result = new GPUOCRCompareResult();
            result.setTaskId(task.getTaskId());
                result.setOldFileName(task.getOldFileName());
                result.setNewFileName(task.getNewFileName());

            // 构建PDF文件URL（使用原任务目录，因为文件实际存储在那里）
            String baseUploadPath = "/api/gpu-ocr/files";
            result.setOldPdfUrl(baseUploadPath + "/gpu-ocr-compare/tasks/" + originalTaskId + "/" + oldPdfPath.getFileName().toString());
            result.setNewPdfUrl(baseUploadPath + "/gpu-ocr-compare/tasks/" + originalTaskId + "/" + newPdfPath.getFileName().toString());
            result.setAnnotatedOldPdfUrl(baseUploadPath + "/gpu-ocr-compare/annotated/" + originalTaskId + "/old_annotated.pdf");
            result.setAnnotatedNewPdfUrl(baseUploadPath + "/gpu-ocr-compare/annotated/" + originalTaskId + "/new_annotated.pdf");
            result.setDifferences(merged);
            result.setTotalDiffCount(merged.size());

            // 设置页面高度和坐标转换参数（与正常比对一致）
            try {
                // 1) 计算PDF实际页面尺寸（像素，72 DPI）
                double oldPdfWidth = getPdfPageWidth(oldPdfPath);
                double oldPdfHeight = getPdfPageHeight(oldPdfPath);
                double newPdfWidth = getPdfPageWidth(newPdfPath);
                double newPdfHeight = getPdfPageHeight(newPdfPath);

                    result.setOldPdfPageHeight(oldPdfHeight);
                    result.setNewPdfPageHeight(newPdfHeight);

                // 2) 渲染页面以获取图像尺寸，用于像素→PDF坐标换算
                int dpi = gpuOcrConfig.getRenderDpi();
                PageImageSizeProvider sizeA = renderPageSizes(oldPdfPath, dpi);
                PageImageSizeProvider sizeB = renderPageSizes(newPdfPath, dpi);

                    double oldImageWidth = sizeA.widths != null && sizeA.widths.length > 0 ? sizeA.widths[0] : oldPdfWidth;
                    double oldImageHeight = sizeA.heights != null && sizeA.heights.length > 0 ? sizeA.heights[0] : oldPdfHeight;
                    double newImageWidth = sizeB.widths != null && sizeB.widths.length > 0 ? sizeB.widths[0] : newPdfWidth;
                    double newImageHeight = sizeB.heights != null && sizeB.heights.length > 0 ? sizeB.heights[0] : newPdfHeight;

                // 3) 计算缩放比例：图像坐标到PDF坐标的转换比例（图像尺寸 / PDF尺寸）
                    double oldScaleX = oldImageWidth / oldPdfWidth;
                    double oldScaleY = oldImageHeight / oldPdfHeight;
                    double newScaleX = newImageWidth / newPdfWidth;
                    double newScaleY = newImageHeight / newPdfHeight;

                    result.setOldPdfScaleX(oldScaleX);
                    result.setOldPdfScaleY(oldScaleY);
                    result.setNewPdfScaleX(newScaleX);
                    result.setNewPdfScaleY(newScaleY);

                System.out.println(String.format(
                    "[DEBUG坐标] 参数: old[%.2f,%.2f]->[%.2f,%.2f] scale[%.3f,%.3f], new[%.2f,%.2f]->[%.2f,%.2f] scale[%.3f,%.3f]",
                        oldImageWidth, oldImageHeight, oldPdfWidth, oldPdfHeight, oldScaleX, oldScaleY,
                        newImageWidth, newImageHeight, newPdfWidth, newPdfHeight, newScaleX, newScaleY));
                } catch (Exception ex) {
                System.err.println("[DEBUG坐标] 计算坐标转换参数失败: " + ex.getMessage());
                // 设置默认值，避免前端崩溃
                    result.setOldPdfPageHeight(1122.52);
                    result.setNewPdfPageHeight(1122.52);
                    result.setOldPdfScaleX(1.0);
                    result.setOldPdfScaleY(1.0);
                    result.setNewPdfScaleX(1.0);
                    result.setNewPdfScaleY(1.0);
                }

            // 转换为前端格式（保存为原始图像坐标，实际坐标转换在getFrontendResult中统一进行）
                List<Map<String, Object>> formattedDifferences = convertDiffBlocksToMapFormat(merged);

                // 创建包装对象用于返回前端期望的格式
                System.out.println("创建前端结果对象...");
                Map<String, Object> frontendResult = new HashMap<>();
                frontendResult.put("taskId", task.getTaskId());
                frontendResult.put("oldFileName", task.getOldFileName());
                frontendResult.put("newFileName", task.getNewFileName());
            frontendResult.put("oldPdfUrl", result.getOldPdfUrl());
            frontendResult.put("newPdfUrl", result.getNewPdfUrl());
            frontendResult.put("annotatedOldPdfUrl", result.getAnnotatedOldPdfUrl());
            frontendResult.put("annotatedNewPdfUrl", result.getAnnotatedNewPdfUrl());
                frontendResult.put("differences", formattedDifferences);
                frontendResult.put("totalDiffCount", formattedDifferences.size());

            // 添加页面高度（供前端坐标转换使用）
                frontendResult.put("oldPdfPageHeight", result.getOldPdfPageHeight());
                frontendResult.put("newPdfPageHeight", result.getNewPdfPageHeight());

                // 保存前端格式的结果
                System.out.println("保存结果到缓存...");
                results.put(task.getTaskId(), result);
                frontendResults.put(task.getTaskId(), frontendResult);

            // 调试模式也需要生成前端结果文件，供前端查看
                try {
                    Path jsonPath = getFrontendResultJsonPath(task.getTaskId());
                    Files.createDirectories(jsonPath.getParent());
                    byte[] json = M.writerWithDefaultPrettyPrinter().writeValueAsBytes(frontendResult);
                    Files.write(jsonPath, json);
                System.out.println("调试模式前端结果已写入文件: " + jsonPath.toAbsolutePath());
                } catch (Exception ioEx) {
                System.err.println("调试模式写入前端结果JSON失败: " + ioEx.getMessage());
            }

            long totalTime = System.currentTimeMillis() - startTime;
            task.setStatus(GPUOCRCompareTask.Status.COMPLETED);
            task.updateProgress(8, "调试比对完成");

            System.out.println(String.format("GPU OCR调试比对完成。差异数量=%d, 总耗时=%dms",
                    formattedDifferences.size(), totalTime));
            System.out.println("结果文件: A=" + frontendResult.get("oldPdfUrl") + ", B=" + frontendResult.get("newPdfUrl"));

        } catch (Exception e) {
            System.err.println("GPU OCR调试比对过程中发生异常:");
            System.err.println("当前步骤: " + task.getCurrentStep() + " - " + task.getCurrentStepDesc());
            System.err.println("错误信息: " + e.getMessage());

            task.setStatus(GPUOCRCompareTask.Status.FAILED);
            task.setErrorMessage("调试比对失败 [步骤" + task.getCurrentStep() + "]: " + e.getMessage());
            task.updateProgress(task.getCurrentStep(), "比对失败: " + e.getMessage());

            e.printStackTrace();
        }
    }

    /**
     * 使用TextNormalizer进行文本预处理，用于比对
     * 
     * @param text 原始文本
     * @param options 比对选项
     * @return 预处理后的文本
     */
    private String preprocessTextForComparison(String text, GPUOCRCompareOptions options) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        // 1. 使用TextNormalizer进行标点符号标准化
        String normalized = TextNormalizer.normalizePunctuation(text);
        
        // 2. 清理OCR识别中常见的特殊字符问题
        normalized = normalized.replace('$', ' ').replace('_', ' ');
        
        // 3. 根据选项处理大小写
        if (options.isIgnoreCase()) {
            normalized = normalized.toLowerCase();
        }
        
        return normalized;
    }

    /**
     * 查找任务目录中的PDF文件
     */
    private Path findTaskPdfFile(Path taskDir, String type) {
        try (var stream = Files.list(taskDir)) {
            return stream
                .filter(path -> path.toString().toLowerCase().endsWith(".pdf"))
                .filter(path -> {
                    String fileName = path.getFileName().toString().toLowerCase();
                    return fileName.startsWith(type + "_");
                })
                .findFirst()
                .orElse(null);
        } catch (Exception e) {
            System.err.println("查找" + type + "PDF文件失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 从保存的JSON文件中解析CharBox数据
     */
    private List<CharBox> parseCharBoxesFromSavedJson(Path pdfPath, GPUOCRCompareOptions options) {
        List<CharBox> charBoxes = new ArrayList<>();
        
        try {
            // 计算PDF页数
            int totalPages = countPdfPages(pdfPath);
            
            // 解析每一页的OCR结果
            TextExtractionUtil.PageLayout[] ordered = new TextExtractionUtil.PageLayout[totalPages];
            for (int page = 1; page <= totalPages; page++) {
                try {
                    ordered[page - 1] = parseOnePageFromSavedJson(pdfPath, page);
                } catch (Exception e) {
                    System.err.println("解析第" + page + "页OCR结果失败: " + e.getMessage());
                    ordered[page - 1] = null;
                }
            }
            
            // 使用现有的解析方法提取CharBox
            charBoxes = parseTextAndPositionsFromResults(ordered, TextExtractionUtil.ExtractionStrategy.POSITION_BASED, options.isIgnoreHeaderFooter());
            
            System.out.println("从保存的JSON文件中解析出" + charBoxes.size() + "个字符");
            
        } catch (Exception e) {
            System.err.println("解析保存的JSON文件失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return charBoxes;
    }
    



    // ---------- OCR辅助方法 ----------

    private int countPdfPages(Path pdfPath) throws Exception {
        try (PDDocument doc = PDDocument.load(pdfPath.toFile())) {
            return doc.getNumberOfPages();
        }
    }

    private TextExtractionUtil.PageLayout parseOnePageFromSavedJson(Path pdfPath, int page) throws Exception {
        String pageJsonPath = pdfPath.toAbsolutePath().toString() + ".page-" + page + ".ocr.json";
        byte[] bytes = Files.readAllBytes(Path.of(pageJsonPath));
        JsonNode root = M.readTree(bytes);
        List<TextExtractionUtil.LayoutItem> items = extractLayoutItems(root);
        return new TextExtractionUtil.PageLayout(page, items);
    }

    private TextExtractionUtil.PageLayout parseOnePage(DotsOcrClient client, byte[] pngBytes, int page, String prompt, Path pdfPath)
            throws Exception {
        long pageStartAt = System.currentTimeMillis();
        String raw;
        if (prompt == null) {
            // 使用DotsOcrClient的默认prompt
            raw = client.ocrImageBytesWithDefaultPrompt(pngBytes, null, "image/png", false);
        } else {
            raw = client.ocrImageBytes(pngBytes, prompt, null, "image/png", false);
        }
        JsonNode env = M.readTree(raw);
        String content = env.path("choices").path(0).path("message").path("content").asText("");
        if (content == null || content.isBlank())
            throw new RuntimeException("模型未返回内容(page=" + page + ")");
        
        // 添加JSON解析错误处理和调试信息
        JsonNode root;
        try {
            String normalized = normalizeModelJson(content);
            root = M.readTree(normalized);
        } catch (Exception e) {
            System.err.println("JSON解析失败 - 页面: " + page);
            System.err.println("原始内容长度: " + content.length());
            System.err.println("内容预览 (前500字符): " + content.substring(0, Math.min(500, content.length())));
            System.err.println("内容预览 (后500字符): " + content.substring(Math.max(0, content.length() - 500)));
            System.err.println("错误详情: " + e.getMessage());
            
            // 尝试修复常见的JSON问题
            String fixedContent = fixJsonContent(content);
            System.err.println("尝试修复后的内容长度: " + fixedContent.length());
            
            try {
                String normalized2 = normalizeModelJson(fixedContent);
                try {
                    root = M.readTree(normalized2);
                } catch (Exception eTry2) {
                    // 最后兜底：按花括号深度切分对象，重建为合法的 [obj,obj,...]
                    String rebuilt = rebuildJsonArrayByBraces(normalized2);
                    root = M.readTree(rebuilt);
                }
                System.out.println("JSON修复成功 - 页面: " + page);
            } catch (Exception e2) {
                System.err.println("JSON修复失败: " + e2.getMessage());
                throw new RuntimeException("JSON解析失败 (页面=" + page + "): " + e.getMessage(), e);
            }
        }
        // 保存每页识别的 JSON 结果，便于后续从第4步直接开始
        try {
            String pageJsonPath = pdfPath.toAbsolutePath().toString() + ".page-" + page + ".ocr.json";
            Files.write(Path.of(pageJsonPath), M.writerWithDefaultPrettyPrinter().writeValueAsBytes(root));
            System.out.println("Saved OCR JSON: " + pageJsonPath);
        } catch (Exception e) {
            System.err.println("Failed to save OCR JSON for page " + page + ": " + e.getMessage());
        }
        List<TextExtractionUtil.LayoutItem> items = extractLayoutItems(root);
        long pageCost = System.currentTimeMillis() - pageStartAt;
        try {
            System.out.println(String.format("OCR单页完成: file=%s, page=%d, 用时=%dms", 
                pdfPath == null ? "-" : pdfPath.getFileName().toString(), page, pageCost));
        } catch (Exception ignore) {}
        return new TextExtractionUtil.PageLayout(page, items);
    }



    private List<byte[]> renderAllPagesToPng(DotsOcrClient client, Path pdfPath) throws Exception {
        int dpi = gpuOcrConfig.getRenderDpi();
        boolean saveImages = client.isSaveRenderedImages();
        
        try (PDDocument doc = PDDocument.load(pdfPath.toFile())) {
            PDFRenderer renderer = new PDFRenderer(doc);
            List<byte[]> list = new ArrayList<>();
            long minPixels = gpuOcrConfig.getMinPixels();
            long maxPixels = gpuOcrConfig.getMaxPixels();
            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, dpi);
                // 像素裁剪：保持比例缩放到[minPixels, maxPixels]区间内
                if (image != null && (minPixels > 0 || maxPixels > 0)) {
                    long pixels = (long) image.getWidth() * (long) image.getHeight();
                    double scale = 1.0;
                    if (maxPixels > 0 && pixels > maxPixels) {
                        scale = Math.sqrt((double) maxPixels / pixels);
                    } else if (minPixels > 0 && pixels < minPixels) {
                        scale = Math.sqrt((double) minPixels / Math.max(1.0, pixels));
                    }
                    if (scale > 0 && Math.abs(scale - 1.0) > 1e-6) {
                        int newW = Math.max(1, (int) Math.round(image.getWidth() * scale));
                        int newH = Math.max(1, (int) Math.round(image.getHeight() * scale));
                        BufferedImage scaled = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
                        Graphics2D g2d = scaled.createGraphics();
                        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        g2d.drawImage(image, 0, 0, newW, newH, null);
                        g2d.dispose();
                        image.flush();
                        image = scaled;
                    }
                }
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    ImageIO.write(image, "png", baos);
                    byte[] bytes = baos.toByteArray();
                    list.add(bytes);
                    
                    // 原有的保存逻辑（如果启用）
                    if (saveImages) {
                        Path out = pdfPath.getParent()
                                .resolve(pdfPath.getFileName().toString() + ".page-" + (i + 1) + ".png");
                        Files.write(out, bytes);
                    }
                }
            }
            
            return list;
        }
    }

    private List<TextExtractionUtil.LayoutItem> extractLayoutItems(JsonNode root) {
        return TextExtractionUtil.extractLayoutItems(root);
    }

    private List<CharBox> parseTextAndPositionsFromResults(TextExtractionUtil.PageLayout[] ordered, TextExtractionUtil.ExtractionStrategy strategy, boolean ignoreHeaderFooter) {
        return TextExtractionUtil.parseTextAndPositionsFromResults(ordered, strategy, ignoreHeaderFooter);
    }


    // 以下方法是从DotsOcrCompareDemoTest复制并适配的

    private List<CharBox> recognizePdfAsCharSeq(DotsOcrClient client, Path pdf, String prompt, boolean resumeFromStep4, GPUOCRCompareOptions options)
            throws Exception {
        TextExtractionUtil.PageLayout[] ordered;
        long ocrAllStartAt = System.currentTimeMillis();
        if (resumeFromStep4) {
            // Step 1 (count pages) + Step 2 skipped; load Step 3 results (saved JSON)
            int total = countPdfPages(pdf);
            ordered = new TextExtractionUtil.PageLayout[total];
            for (int i = 0; i < total; i++) {
                final int pageNo = i + 1;
                TextExtractionUtil.PageLayout p = parseOnePageFromSavedJson(pdf, pageNo);
                ordered[pageNo - 1] = p;
            }
        } else {
            // 如果开启Gradio队列，直接走demo_gradio流程生成每页JSON
            if (gpuOcrConfig.isUseGradioQueue()) {
                // 为Gradio模式也保存OCR图片（需要从外部传入taskId）
                // 这里暂时跳过，因为Gradio模式在recognizePdfAsCharSeq内部处理
                
                GradioWorkflowClient gw = new GradioWorkflowClient(gpuOcrConfig.getGradioBaseUrl());
                System.out.println("[Gradio] upload ... " + pdf);
                String serverPath = gw.uploadFile(pdf); // 直接返回 /tmp/gradio/.../xxx.pdf
                System.out.println("[Gradio] join queue ... serverPath=" + serverPath);
                GradioWorkflowClient.JoinInfo join = gw.joinQueue(
                        serverPath,
                        "prompt_layout_all_en",
                        "127.0.0.1",
                        8000,
                        gpuOcrConfig.getMinPixels(),
                        gpuOcrConfig.getMaxPixels(),
                        true
                );
                System.out.println("[Gradio] eventId=" + join.eventId + ", sessionHash=" + join.sessionHash);
                String result = gw.pollResult(join);
                System.out.println("[Gradio] data length=" + (result==null?0:result.length()));
                // 解析结果并保存为 .page-N.ocr.json
                int pagesSaved = saveGradioPagesFromResult(gw, result, pdf);
                if (pagesSaved <= 0) throw new RuntimeException("Gradio data 无可用页面JSON");
                // 装载
                int total = countPdfPages(pdf);
                ordered = new TextExtractionUtil.PageLayout[total];
                for (int i = 0; i < total; i++) {
                    final int pageNo = i + 1;
                    TextExtractionUtil.PageLayout p = parseOnePageFromSavedJson(pdf, pageNo);
                    ordered[pageNo - 1] = p;
                }
            } else {
                // Step 1: render PDF to images（默认旧流程）
                List<byte[]> pages = renderAllPagesToPng(client, pdf);
                int total = pages.size();
                int parallel = Math.max(1, gpuOcrConfig.getParallelThreads()); // 使用配置的并行线程数
                java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors
                        .newFixedThreadPool(Math.min(parallel, total));
                java.util.concurrent.ExecutorCompletionService<TextExtractionUtil.PageLayout> ecs =
                        new java.util.concurrent.ExecutorCompletionService<>(pool);
                for (int i = 0; i < total; i++) {
                    final int pageNo = i + 1;
                    final byte[] img = pages.get(i);
                    ecs.submit(() -> parseOnePage(client, img, pageNo, prompt, pdf));
                }
                ordered = new TextExtractionUtil.PageLayout[total];
                for (int i = 0; i < total; i++) {
                    TextExtractionUtil.PageLayout p = ecs.take().get();
                    ordered[p.page - 1] = p;
                }
                pool.shutdownNow();
            }
        }

        long ocrAllCost = System.currentTimeMillis() - ocrAllStartAt;
        try {
            int pages = ordered == null ? 0 : ordered.length;
            double avg = pages > 0 ? (ocrAllCost * 1.0 / pages) : 0.0;
            System.out.println(String.format("OCR识别完成: file=%s, 页数=%d, 总用时=%dms, 平均每页=%.1fms", 
                pdf == null ? "-" : pdf.getFileName().toString(), pages, ocrAllCost, avg));
        } catch (Exception ignore) {}

        // 使用新的按顺序读取方法解析文本和位置
        List<CharBox> out = parseTextAndPositionsFromResults(ordered, TextExtractionUtil.ExtractionStrategy.POSITION_BASED, options.isIgnoreHeaderFooter());

        // Step 3: 保存提取的纯文本（含/不含页标记），便于开发调试
        try {
            String extractedWithPages = TextExtractionUtil.extractTextWithPageMarkers(out);
            String extractedNoPages = TextExtractionUtil.extractText(out);

            String txtOut = pdf.toAbsolutePath().toString() + ".extracted.txt";
            String txtOutCompare = pdf.toAbsolutePath().toString() + ".extracted.compare.txt";

            Files.write(Path.of(txtOut), extractedWithPages.getBytes(StandardCharsets.UTF_8));
            Files.write(Path.of(txtOutCompare), extractedNoPages.getBytes(StandardCharsets.UTF_8));

            System.out.println("Extracted text saved: " + txtOut);
            System.out.println("Extracted text (no page markers) saved: " + txtOutCompare);
        } catch (Exception e) {
            System.err.println("Failed to write extracted text: " + e.getMessage());
        }
        return out;
    }

    private String joinWithLineBreaks(List<CharBox> cs) {
        if (cs.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        String lastKey = null;

        for (CharBox c : cs) {
            if (c.bbox != null) {
                String currentKey = c.page + "|" + (int) c.bbox[0] + "," + (int) c.bbox[1] + "," + (int) c.bbox[2] + "," + (int) c.bbox[3];
                if (lastKey != null && !lastKey.equals(currentKey)) {
                    sb.append('\n');
                }
                sb.append(c.ch);
                lastKey = currentKey;
            }
        }
        sb.append('\n');
        return sb.toString();
    }

    private List<DiffBlock> mergeBlocksByBbox(List<DiffBlock> blocks) {
        if (blocks.isEmpty()) return blocks;

        // 1. 应用bbox相同合并算法
        List<DiffBlock> result1 = mergeSameBboxBlocks(blocks);
        
        // 2. 应用连续新增/删除合并算法
        //List<DiffBlock> result2 = mergeConsecutiveInsertDelete(result1);

        // 最终结果中去掉所有 IGNORED 块
        List<DiffBlock> finalResult = new ArrayList<>();
        for (DiffBlock b : result1) {
            if (b != null && b.type != DiffBlock.DiffType.IGNORED) {
                finalResult.add(b);
            }
        }

        System.out.println("合并前 blocks 数量: " + blocks.size());
        System.out.println("bbox合并后数量: " + result1.size());
        System.out.println("连续合并后数量: " + result1.size());
        System.out.println("去除IGNORED后数量: " + finalResult.size());
        System.out.println("实际合并的块数: " + (blocks.size() - finalResult.size()));
        
        // 统计IGNORED块数量
        long ignoredCount = blocks.stream().filter(b -> b != null && b.type == DiffBlock.DiffType.IGNORED).count();
        System.out.println("原始IGNORED块数量: " + ignoredCount);

        return finalResult;
    }

    /**
     * 合并具有相同bbox的块（保持原有顺序，不跳过IGNORED块）
     */
    private List<DiffBlock> mergeSameBboxBlocks(List<DiffBlock> blocks) {
        List<DiffBlock> result = new ArrayList<>();
        boolean[] processed = new boolean[blocks.size()];
        
        for (int i = 0; i < blocks.size(); i++) {
            if (processed[i]) continue;
            
            DiffBlock current = blocks.get(i);
            if (current == null) {
                processed[i] = true;
                continue;
            }
            
            // IGNORED块直接添加，不参与合并
            if (current.type == DiffBlock.DiffType.IGNORED) {
                result.add(current);
                processed[i] = true;
                continue;
            }

            // 仅限于"只有一个bbox"的情况才允许进入合并流程
            if (!isSingleBbox(current)) {
                result.add(current);
                processed[i] = true;
                continue;
            }

            List<DiffBlock> sameBboxGroup = new ArrayList<>();
            sameBboxGroup.add(current);
            processed[i] = true;
            
            // 查找所有具有相同bbox的块（只查找非IGNORED块）
            for (int j = i + 1; j < blocks.size(); j++) {
                if (processed[j]) continue;
                
                DiffBlock other = blocks.get(j);
                if (other == null) {
                    processed[j] = true;
                    continue;
                }
                
                // IGNORED块跳过，不参与合并
                if (other.type == DiffBlock.DiffType.IGNORED) {
                    continue;
                }
                
                // 仅当对方也满足"只有一个bbox"且bbox完全相同，才加入合并组
                if (isSingleBbox(other) && haveIdenticalBbox(current, other)) {
                    sameBboxGroup.add(other);
                    processed[j] = true;
                }
            }
            
            // 如果只有一个块，直接添加
            if (sameBboxGroup.size() == 1) {
                result.add(current);
            } else {
                // 合并多个相同bbox的块
                result.add(mergeSameBboxGroup(sameBboxGroup));
            }
        }
        
        return result;
    }

    /**
     * 合并一组具有相同bbox的块
     */
    private DiffBlock mergeSameBboxGroup(List<DiffBlock> group) {
        if (group.isEmpty()) return null;
        if (group.size() == 1) return group.get(0);
        
        DiffBlock first = group.get(0);
        DiffBlock merged = new DiffBlock();
        merged.type = first.type;
        merged.page = first.page;
        // 合并页码数组
        merged.pageA = new ArrayList<>();
        merged.pageB = new ArrayList<>();
        for (DiffBlock block : group) {
            if (block.pageA != null) merged.pageA.addAll(block.pageA);
            if (block.pageB != null) merged.pageB.addAll(block.pageB);
        }
        
        // 合并所有bbox
        merged.oldBboxes = new ArrayList<>();
        merged.newBboxes = new ArrayList<>();
        
        // 合并文本内容和范围
        StringBuilder oldTextBuilder = new StringBuilder();
        StringBuilder newTextBuilder = new StringBuilder();
        Set<String> seenOldSegments = new HashSet<>();
        Set<String> seenNewSegments = new HashSet<>();
        // allTextA/allTextB 不进行合并，保持第一个块的内容
        List<DiffBlock.TextRange> diffRangesA = new ArrayList<>();
        List<DiffBlock.TextRange> diffRangesB = new ArrayList<>();
        
        int currentPosA = 0;
        int currentPosB = 0;
        
        DiffBlock firstWithPrev = null;
        DiffBlock firstWithPrev2 = null;
        for (DiffBlock block : group) {
            if (block.oldBboxes != null) {
                addBboxesUnique(merged.oldBboxes, block.oldBboxes, 0.5);
            }
            if (block.newBboxes != null) {
                addBboxesUnique(merged.newBboxes, block.newBboxes, 0.5);
            }
            if (firstWithPrev2 == null && ((block.prevOldBboxes != null && !block.prevOldBboxes.isEmpty()) || (block.prevNewBboxes != null && !block.prevNewBboxes.isEmpty()))) {
                firstWithPrev2 = block;
            }
            if (firstWithPrev == null && ((block.prevOldBboxes != null && !block.prevOldBboxes.isEmpty()) || (block.prevNewBboxes != null && !block.prevNewBboxes.isEmpty()))) {
                firstWithPrev = block;
            }
            if (block.oldText != null && !block.oldText.trim().isEmpty()) {
                String seg = block.oldText.trim();
                if (!seenOldSegments.contains(seg)) {
                    if (oldTextBuilder.length() > 0) oldTextBuilder.append(" ");
                    oldTextBuilder.append(seg);
                    seenOldSegments.add(seg);
                }
            }
            if (block.newText != null && !block.newText.trim().isEmpty()) {
                String segN = block.newText.trim();
                if (!seenNewSegments.contains(segN)) {
                    if (newTextBuilder.length() > 0) newTextBuilder.append(" ");
                    newTextBuilder.append(segN);
                    seenNewSegments.add(segN);
                }
            }
            
            
            // 对于相同bbox合并，合并所有块的差异范围
            // 因为相同bbox的块可能有不同的差异范围，需要全部合并
            if (block.diffRangesA != null) {
                for (DiffBlock.TextRange range : block.diffRangesA) {
                    // 检查是否已存在相同的范围，避免重复添加
                    boolean exists = false;
                    for (DiffBlock.TextRange existing : diffRangesA) {
                        if (existing.start == range.start && existing.end == range.end && 
                            existing.type.equals(range.type)) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        diffRangesA.add(new DiffBlock.TextRange(
                            range.start,  // 保持原有位置，不加偏移
                            range.end,    // 保持原有位置，不加偏移
                            range.type
                        ));
                    }
                }
            }
            if (block.diffRangesB != null) {
                for (DiffBlock.TextRange range : block.diffRangesB) {
                    // 检查是否已存在相同的范围，避免重复添加
                    boolean exists = false;
                    for (DiffBlock.TextRange existing : diffRangesB) {
                        if (existing.start == range.start && existing.end == range.end && 
                            existing.type.equals(range.type)) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        diffRangesB.add(new DiffBlock.TextRange(
                            range.start,  // 保持原有位置，不加偏移
                            range.end,    // 保持原有位置，不加偏移
                            range.type
                        ));
                    }
                }
            }
            
            // 对于相同bbox合并，不更新位置偏移
            // 因为文本内容是重复的，不需要累加长度
        }
        
        merged.oldText = oldTextBuilder.toString();
        merged.newText = newTextBuilder.toString();
        
        // 调试输出：验证bbox合并后的文本和范围
        System.out.println("DEBUG bbox合并后 - 合并了" + group.size() + "个相同bbox的块");
        System.out.println("DEBUG bbox合并后 - allTextA保持首块, allTextB保持首块");
        System.out.println("DEBUG bbox合并后 - diffRangesA: " + diffRangesA);
        System.out.println("DEBUG bbox合并后 - diffRangesB: " + diffRangesB);
        System.out.println("DEBUG bbox合并后 - 原始块的diffRanges已保持原有位置（无偏移调整）");
        
        if (firstWithPrev2 != null) {
            merged.prevOldBboxes = firstWithPrev2.prevOldBboxes == null ? null : new ArrayList<>(firstWithPrev2.prevOldBboxes);
            merged.prevNewBboxes = firstWithPrev2.prevNewBboxes == null ? null : new ArrayList<>(firstWithPrev2.prevNewBboxes);
        }
        // 保留第一个带 prev* 的来源，保证前端跳转链
        if (firstWithPrev != null) {
            merged.prevOldBboxes = firstWithPrev.prevOldBboxes == null ? null : new ArrayList<>(firstWithPrev.prevOldBboxes);
            merged.prevNewBboxes = firstWithPrev.prevNewBboxes == null ? null : new ArrayList<>(firstWithPrev.prevNewBboxes);
        }
        merged.allTextA = first.allTextA == null ? null : new ArrayList<>(first.allTextA);
        merged.allTextB = first.allTextB == null ? null : new ArrayList<>(first.allTextB);
        merged.diffRangesA = diffRangesA;
        merged.diffRangesB = diffRangesB;
        
        System.out.println("合并相同bbox块: " + group.size() + "个块 -> 1个块");
        System.out.println("合并后oldText: " + merged.oldText);
        System.out.println("合并后newText: " + merged.newText);

        return merged;
    }

    /**
     * 判断一个差异块是否仅包含一个bbox（old/new 两侧合计仅1个）
     */
    private boolean isSingleBbox(DiffBlock block) {
        int count = 0;
        if (block == null) return false;
        if (block.oldBboxes != null) count += block.oldBboxes.size();
        if (block.newBboxes != null) count += block.newBboxes.size();
        return count == 1;
    }

    private boolean haveIdenticalBbox(DiffBlock a, DiffBlock b) {
        // 如果任意一侧（old/new）的任意 bbox 在两个 block 中完全一致，则认为存在相同内容块
        List<double[]> aAll = a.getAllBboxes();
        List<double[]> bAll = b.getAllBboxes();
        if (aAll == null || bAll == null || aAll.isEmpty() || bAll.isEmpty()) return false;
        for (double[] x : aAll) {
            for (double[] y : bAll) {
                if (bboxEquals(x, y)) return true;
            }
        }
        return false; 
    }

    private boolean bboxEquals(double[] a, double[] b) {
        if (a == null || b == null || a.length < 4 || b.length < 4) return false;
        final double EPS = 1e-3; // 容差
        return Math.abs(a[0] - b[0]) < EPS
            && Math.abs(a[1] - b[1]) < EPS
            && Math.abs(a[2] - b[2]) < EPS
            && Math.abs(a[3] - b[3]) < EPS;
    }

    /**
     * 向目标列表追加bbox并去重（按坐标容差 / IoU 近似去重）
     */
    private void addBboxesUnique(List<double[]> target, List<double[]> incoming, double tolPx) {
        if (incoming == null || incoming.isEmpty()) return;
        if (target == null) return;
        for (double[] cand : incoming) {
            boolean exists = false;
            for (double[] exist : target) {
                if (approxSameBox(exist, cand, tolPx)) { exists = true; break; }
            }
            if (!exists) target.add(cand);
        }
    }

    private boolean approxSameBox(double[] a, double[] b, double tolPx) {
        if (a == null || b == null || a.length < 4 || b.length < 4) return false;
        boolean close = Math.abs(a[0]-b[0]) <= tolPx && Math.abs(a[1]-b[1]) <= tolPx
                && Math.abs(a[2]-b[2]) <= tolPx && Math.abs(a[3]-b[3]) <= tolPx;
        if (close) return true;
        // 再用 IoU 判定高度重合
        double inter = intersectArea(a, b);
        if (inter <= 0) return false;
        double areaA = Math.max(0, (a[2]-a[0])) * Math.max(0, (a[3]-a[1]));
        double areaB = Math.max(0, (b[2]-b[0])) * Math.max(0, (b[3]-b[1]));
        double iou = inter / Math.max(1e-6, (areaA + areaB - inter));
        return iou > 0.9; // 高重合视为相同
    }

    private double intersectArea(double[] a, double[] b) {
        double x1 = Math.max(a[0], b[0]);
        double y1 = Math.max(a[1], b[1]);
        double x2 = Math.min(a[2], b[2]);
        double y2 = Math.min(a[3], b[3]);
        double w = Math.max(0, x2 - x1);
        double h = Math.max(0, y2 - y1);
        return w * h;
    }



    // ---------- PDF标注相关方法 ----------

    private static String key(int page, double[] box) {
        if (box == null || box.length < 4) {
            throw new IllegalArgumentException("Invalid bbox: " + (box == null ? "null" : "length=" + box.length));
        }
        return page + "|" + String.format(Locale.ROOT, "%.3f,%.3f,%.3f,%.3f", box[0], box[1], box[2], box[3]);
    }

    private static class IndexMap {
        final String normalized; // 与 diff 使用的同构文本（仅做 $/_ → 空格 与 标点归一）
        final int[] seqIndex; // normalized 中每个字符位置对应的 CharBox 索引；无对应时为 -1（如换行）

        IndexMap(String normalized, int[] seqIndex) {
            this.normalized = normalized;
            this.seqIndex = seqIndex;
        }
    }

    private static IndexMap buildNormalizedIndexMap(List<CharBox> seq) {
        // 构建与 joinWithLineBreaks 一致的基础串，同时记录每个字符对应的 CharBox 索引
        StringBuilder base = new StringBuilder();
        List<Integer> idxMap = new ArrayList<>();
        String lastKey = null;
        for (int i = 0; i < seq.size(); i++) {
            CharBox c = seq.get(i);
            if (c.bbox != null) {
                String currentKey = key(c.page, c.bbox);
                if (lastKey != null && !lastKey.equals(currentKey)) {
                    base.append('\n');
                    idxMap.add(-1);
                }
                base.append(c.ch);
                idxMap.add(i);
                lastKey = currentKey;
            }
        }
        // 收尾换行
        base.append('\n');
        idxMap.add(-1);

        String norm = TextNormalizer.normalizePunctuation(base.toString()).replace('$', ' ').replace('_', ' ');
        // 规范化步骤不改变长度的假设（标点归一/替换为空格）。若未来改变长度，此映射将失配。
        int[] map = new int[idxMap.size()];
        for (int i = 0; i < idxMap.size(); i++)
            map[i] = idxMap.get(i);
        return new IndexMap(norm, map);
    }

    private static class RectOnPage {
        final int pageIndex0; // 0-based
        final double[] bbox; // [x1,y1,x2,y2] 图像像素坐标
        final DiffUtil.Operation op; // INSERT/DELETE/MODIFIED 用于着色

        RectOnPage(int pageIndex0, double[] bbox, DiffUtil.Operation op) {
            this.pageIndex0 = pageIndex0;
            this.bbox = bbox;
            this.op = op;
        }
    }

    private static List<RectOnPage> collectRectsForDiffBlocks(List<DiffBlock> blocks, IndexMap map, List<CharBox> seq,
            boolean isLeft) {
        List<RectOnPage> out = new ArrayList<>();

        for (DiffBlock block : blocks) {
            // 跳过被忽略的差异，不为它们生成标记
            if (block.type == DiffBlock.DiffType.IGNORED) {
                continue;
            }

            // 根据block类型决定是否处理本侧
            DiffUtil.Operation op = null;
            if (block.type == DiffBlock.DiffType.DELETED && isLeft) {
                op = DiffUtil.Operation.DELETE;
            } else if (block.type == DiffBlock.DiffType.ADDED && !isLeft) {
                op = DiffUtil.Operation.INSERT;
            }

            if (op == null)
                continue; // 跳过不需要在本侧标记的块

            // 根据操作类型选择要处理的bbox
            List<double[]> bboxesToProcess = new ArrayList<>();
            if (block.type == DiffBlock.DiffType.DELETED && isLeft && block.oldBboxes != null) {
                // DELETE操作且是左侧文档：处理oldBboxes
                bboxesToProcess.addAll(block.oldBboxes);
            } else if (block.type == DiffBlock.DiffType.ADDED && !isLeft && block.newBboxes != null) {
                // INSERT操作且是右侧文档：处理newBboxes
                bboxesToProcess.addAll(block.newBboxes);
            }

            if (bboxesToProcess.isEmpty()) {
                continue; // 没有需要处理的bbox，跳过
            }

            // 直接使用 DiffBlock 自带的 bbox 列表标注，每个bbox使用对应的页码
            List<Integer> pageList = (op == DiffUtil.Operation.DELETE) ? block.pageA : block.pageB;
            for (int i = 0; i < bboxesToProcess.size(); i++) {
                double[] bbox = bboxesToProcess.get(i);
                int pageIndex0;
                if (pageList != null && i < pageList.size()) {
                    pageIndex0 = Math.max(0, pageList.get(i) - 1);
                } else {
                    // 兜底：使用最后一个页码或默认页码
                    pageIndex0 = Math.max(0, (block.page > 0 ? block.page : 1) - 1);
                }
                out.add(new RectOnPage(pageIndex0, bbox, op));
            }
        }

        // 对收集到的矩形进行去重
        List<RectOnPage> deduplicatedRects = deduplicateRects(out);
        System.out.println("矩形去重完成，原始数量: " + out.size() + ", 去重后数量: " + deduplicatedRects.size());
        
        return deduplicatedRects;
    }

    /**
     * 对矩形列表进行去重，基于页面、坐标和操作类型
     * @param rects 原始矩形列表
     * @return 去重后的矩形列表
     */
    private static List<RectOnPage> deduplicateRects(List<RectOnPage> rects) {
        if (rects == null || rects.isEmpty()) {
            return rects;
        }

        List<RectOnPage> result = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();

        for (RectOnPage rect : rects) {
            // 生成唯一键：页面索引 + 坐标 + 操作类型
            String key = generateRectKey(rect);
            
            if (!seenKeys.contains(key)) {
                seenKeys.add(key);
                result.add(rect);
            }
        }

        return result;
    }

    /**
     * 为矩形生成唯一键，用于去重判断
     * @param rect 矩形对象
     * @return 唯一键字符串
     */
    private static String generateRectKey(RectOnPage rect) {
        if (rect == null || rect.bbox == null || rect.bbox.length < 4) {
            return "";
        }

        // 使用坐标容差进行近似匹配（1像素容差）
        final double TOLERANCE = 1.0;
        double x1 = Math.round(rect.bbox[0] / TOLERANCE) * TOLERANCE;
        double y1 = Math.round(rect.bbox[1] / TOLERANCE) * TOLERANCE;
        double x2 = Math.round(rect.bbox[2] / TOLERANCE) * TOLERANCE;
        double y2 = Math.round(rect.bbox[3] / TOLERANCE) * TOLERANCE;

        return String.format("%d_%.1f_%.1f_%.1f_%.1f_%s", 
            rect.pageIndex0, x1, y1, x2, y2, rect.op.toString());
    }

    private static class PageImageSizeProvider {
        final int pageCount;
        final int[] widths;
        final int[] heights;

        PageImageSizeProvider(int pageCount, int[] widths, int[] heights) {
            this.pageCount = pageCount;
            this.widths = widths;
            this.heights = heights;
        }
    }

    private PageImageSizeProvider renderPageSizes(Path pdf, int dpi) throws Exception {
        DotsOcrClient client = DotsOcrClient.builder()
                .baseUrl(gpuOcrConfig.getOcrBaseUrl())
                .defaultModel(gpuOcrConfig.getOcrModel())
                .build();

        try (PDDocument doc = PDDocument.load(pdf.toFile())) {
            PDFRenderer r = new PDFRenderer(doc);
            int n = doc.getNumberOfPages();
            int[] ws = new int[n];
            int[] hs = new int[n];
            for (int i = 0; i < n; i++) {
                BufferedImage img = r.renderImageWithDPI(i, dpi);
                ws[i] = img.getWidth();
                hs[i] = img.getHeight();
            }
            return new PageImageSizeProvider(n, ws, hs);
        }
    }

    private static void annotatePDF(Path sourcePdf, String outPath, List<RectOnPage> rects, PageImageSizeProvider sizes)
            throws Exception {
        try (PDDocument doc = PDDocument.load(sourcePdf.toFile())) {
            for (RectOnPage rp : rects) {
                int p = Math.max(0, Math.min(rp.pageIndex0, doc.getNumberOfPages() - 1));
                PDPage page = doc.getPage(p);
                PDRectangle mediaBox = page.getMediaBox();
                float pageW = mediaBox.getWidth();
                float pageH = mediaBox.getHeight();

                int imgW = (sizes != null && p < sizes.pageCount) ? sizes.widths[p] : (int) pageW;
                int imgH = (sizes != null && p < sizes.pageCount) ? sizes.heights[p] : (int) pageH;
                float scaleX = pageW / Math.max(1f, imgW);
                float scaleY = pageH / Math.max(1f, imgH);

                double[] b = rp.bbox; // [x1,y1,x2,y2] 顶点像素坐标（左上为原点）
                float x1 = (float) b[0] * scaleX;
                float y1Top = (float) b[1] * scaleY;
                float x2 = (float) b[2] * scaleX;
                float y2Top = (float) b[3] * scaleY;
                float rx = Math.min(x1, x2);
                float ryTop = Math.min(y1Top, y2Top);
                float rw = Math.abs(x2 - x1);
                float rh = Math.abs(y2Top - y1Top);

                PDAnnotationTextMarkup m = new PDAnnotationTextMarkup(PDAnnotationTextMarkup.SUB_TYPE_HIGHLIGHT);
                float[] color = rp.op == DiffUtil.Operation.DELETE ? new float[] { 1f, 0f, 0f }
                        : (rp.op == DiffUtil.Operation.INSERT ? new float[] { 0f, 1f, 0f }
                                : new float[] { 1f, 1f, 0f });
                m.setColor(new PDColor(color, PDDeviceRGB.INSTANCE));
                // 设置透明度（0=完全透明，1=不透明）。为适配不同 PDFBox 版本，直接写 COS 字典 CA 条目。
                try {
                    m.getCOSObject().setFloat(org.apache.pdfbox.cos.COSName.CA, 0.25f);
                } catch (Throwable ignore) {
                    // 忽略
                }

                PDRectangle pr = new PDRectangle();
                pr.setLowerLeftX(rx);
                pr.setLowerLeftY(Math.max(0, pageH - (ryTop + rh)));
                pr.setUpperRightX(rx + rw);
                pr.setUpperRightY(Math.max(0, Math.min(pageH, pageH - ryTop)));
                m.setRectangle(pr);
                float[] qu = new float[] { pr.getLowerLeftX(), pr.getUpperRightY(), pr.getUpperRightX(),
                        pr.getUpperRightY(), pr.getLowerLeftX(), pr.getLowerLeftY(), pr.getUpperRightX(),
                        pr.getLowerLeftY() };
                m.setQuadPoints(qu);
                m.setSubtype("Highlight");

                java.util.List<org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation> anns = page
                        .getAnnotations();
                if (anns == null)
                    anns = new ArrayList<>();
                anns.add(m);
                page.setAnnotations(anns);
            }
            doc.save(outPath);
        }
    }

    /**
     * 将DiffBlock列表转换为前端期望的Map格式（保留原始图像坐标）
     */
    private List<Map<String, Object>> convertDiffBlocksToMapFormat(List<DiffBlock> diffBlocks) {
        List<Map<String, Object>> mapResult = new ArrayList<>();

        if (diffBlocks == null) {
            return mapResult;
        }

        for (DiffBlock block : diffBlocks) {
            Map<String, Object> diffMap = new HashMap<>();

            // 转换操作类型
            String operation = convertDiffTypeToOperation(block.type);
            diffMap.put("operation", operation);

            // 添加文本内容
            diffMap.put("oldText", block.oldText != null ? block.oldText : "");
            diffMap.put("newText", block.newText != null ? block.newText : "");

            // 添加页面信息
            diffMap.put("page", block.page);
            // 页码数组：前端需要最后一个页码用于显示
            diffMap.put("pageA", block.pageA != null && !block.pageA.isEmpty() ? block.pageA.get(block.pageA.size() - 1) : block.page);
            diffMap.put("pageB", block.pageB != null && !block.pageB.isEmpty() ? block.pageB.get(block.pageB.size() - 1) : block.page);
            // 添加完整的页码数组供前端使用
            diffMap.put("pageAList", block.pageA);
            diffMap.put("pageBList", block.pageB);

            // 添加bbox信息（保留原始图像坐标）
            if (block.oldBboxes != null && !block.oldBboxes.isEmpty()) {
                diffMap.put("oldBbox", block.oldBboxes.get(0)); // 第一个bbox用于跳转
                diffMap.put("oldBboxes", block.oldBboxes); // 所有bbox用于PDF标注
            }
            if (block.newBboxes != null && !block.newBboxes.isEmpty()) {
                diffMap.put("newBbox", block.newBboxes.get(0)); // 第一个bbox用于跳转
                diffMap.put("newBboxes", block.newBboxes); // 所有bbox用于PDF标注
            }

            // 添加上一个block的bbox信息，用于同步跳转
            if (block.prevOldBboxes != null && !block.prevOldBboxes.isEmpty()) {
                diffMap.put("prevOldBbox", block.prevOldBboxes.get(block.prevOldBboxes.size() - 1));
            }
            if (block.prevNewBboxes != null && !block.prevNewBboxes.isEmpty()) {
                diffMap.put("prevNewBbox", block.prevNewBboxes.get(block.prevNewBboxes.size() - 1));
            }

            // 添加索引信息
            diffMap.put("textStartIndexA", block.textStartIndexA);
            diffMap.put("textStartIndexB", block.textStartIndexB);

            // 添加完整文本和差异范围信息
            diffMap.put("allTextA", block.allTextA != null ? block.allTextA : new ArrayList<>());
            diffMap.put("allTextB", block.allTextB != null ? block.allTextB : new ArrayList<>());
            diffMap.put("diffRangesA", block.diffRangesA != null ? block.diffRangesA : new ArrayList<>());
            diffMap.put("diffRangesB", block.diffRangesB != null ? block.diffRangesB : new ArrayList<>());

            // 调试输出：打印每个差异块的文本与范围信息，便于前端高亮问题排查
            try {
                if (block.allTextA != null && !block.allTextA.isEmpty()) {
                    System.out.println("  allTextA[0]=" + block.allTextA.get(0));
                }
                if (block.allTextB != null && !block.allTextB.isEmpty()) {
                    System.out.println("  allTextB[0]=" + block.allTextB.get(0));
                }
                if (block.diffRangesA != null && !block.diffRangesA.isEmpty()) {
                    System.out.println("  diffRangesA=" + block.diffRangesA);
                }
                if (block.diffRangesB != null && !block.diffRangesB.isEmpty()) {
                    System.out.println("  diffRangesB=" + block.diffRangesB);
                }
            } catch (Exception logEx) {
                System.err.println("前端映射调试日志打印失败: " + logEx.getMessage());
            }

            mapResult.add(diffMap);
        }

        return mapResult;
    }

    /**
     * 将图像坐标转换为PDF坐标系
     * @param imageBbox 图像坐标系的bbox [x1, y1, x2, y2]
     * @param scaleX X轴缩放比例
     * @param scaleY Y轴缩放比例
     * @param pdfPageHeight PDF页面高度
     * @return PDF坐标系的bbox [x1, y1, x2, y2]
     */
    private double[] convertImageCoordsToPdfCoords(double[] imageBbox, double scaleX, double scaleY, double pdfPageHeight) {
        if (imageBbox == null || imageBbox.length < 4) {
            return imageBbox;
        }

        // 应用缩放比例（图像坐标 → PDF坐标）
        // 注意：scaleX = imageWidth / pdfWidth，所以从图像坐标到PDF坐标应该是除法
        double pdfX1 = imageBbox[0] / scaleX;
        double pdfY1 = imageBbox[1] / scaleY;
        double pdfX2 = imageBbox[2] / scaleX;
        double pdfY2 = imageBbox[3] / scaleY;

        // 不做Y轴翻转，返回顶左原点PDF坐标（供前端统一做翻转）
        double[] topLeftPdfCoords = new double[]{pdfX1, pdfY1, pdfX2, pdfY2};

        return topLeftPdfCoords;
    }

    /**
     * 将DiffType转换为前端期望的操作类型
     */
    private String convertDiffTypeToOperation(DiffBlock.DiffType diffType) {
        switch (diffType) {
            case DELETED:
                return "DELETE";
            case ADDED:
                return "INSERT";
            case MODIFIED:
                return "MODIFY";
            case IGNORED:
                return "IGNORE";
            default:
                return "UNKNOWN";
        }
    }

    /**
     * 获取PDF第一页的高度（像素）
     */
    private double getPdfPageHeight(Path pdfPath) throws Exception {
        try (PDDocument document = PDDocument.load(pdfPath.toFile())) {
            if (document.getNumberOfPages() > 0) {
                PDPage page = document.getPage(0);
                PDRectangle mediaBox = page.getMediaBox();
                // 返回页面高度（以像素为单位，72 DPI）
                return mediaBox.getHeight();
            }
        }
        throw new RuntimeException("PDF文档没有页面");
    }

    /**
     * 获取PDF第一页的宽度（像素）
     */
    private double getPdfPageWidth(Path pdfPath) throws Exception {
        try (PDDocument document = PDDocument.load(pdfPath.toFile())) {
            if (document.getNumberOfPages() > 0) {
                PDPage page = document.getPage(0);
                PDRectangle mediaBox = page.getMediaBox();
                // 返回页面宽度（以像素为单位，72 DPI）
                return mediaBox.getWidth();
            }
        }
        throw new RuntimeException("PDF文档没有页面");
    }

    private Path getFrontendResultJsonPath(String taskId) {
        // 基于系统配置的上传根目录保存结果文件
        String uploadRootPath = zxcmConfig.getFileUpload().getRootPath();
        Path base = Paths.get(uploadRootPath, "gpu-ocr-compare", "results");
        return base.resolve(taskId + ".json");
    }

    /**
     * 修复常见的JSON格式问题
     */
    private String fixJsonContent(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        
        StringBuilder fixed = new StringBuilder(content);
        
        // 1. 检查是否以 [ 开始，如果不是，尝试找到第一个 [
        int startBracket = fixed.indexOf("[");
        if (startBracket > 0) {
            fixed = new StringBuilder(fixed.substring(startBracket));
        }
        
        // 2. 检查是否以 ] 结束，如果不是，尝试添加
        int lastBracket = fixed.lastIndexOf("]");
        if (lastBracket == -1 || lastBracket < fixed.length() - 10) {
            // 找到最后一个完整的对象
            int lastCompleteObject = findLastCompleteObject(fixed.toString());
            if (lastCompleteObject > 0) {
                fixed = new StringBuilder(fixed.substring(0, lastCompleteObject));
                fixed.append("]");
            }
        }
        
        // 3. 修复未闭合的字符串
        String result = fixUnclosedStrings(fixed.toString());
        
        // 4. 修复转义字符问题
        result = result.replace("\\n", "\\n")
                      .replace("\\t", "\\t")
                      .replace("\\r", "\\r");
        
        return result;
    }

    /**
     * 归一化模型输出的JSON：
     * - 去除```json/```包裹
     * - 去掉Windows换行中的回车
     */
    private String normalizeModelJson(String content) {
        String s = content;
        // strip code fences
        if (s.startsWith("```")) {
            s = s.replaceFirst("^```json\\s*", "");
            s = s.replaceFirst("^```\\s*", "");
        }
        if (s.endsWith("```")) {
            int idx = s.lastIndexOf("```");
            if (idx >= 0) s = s.substring(0, idx);
        }
        // normalize line endings
        s = s.replace("\r\n", "\n");
        // strip BOM and zero-width
        if (!s.isEmpty() && s.charAt(0) == '\uFEFF') s = s.substring(1);
        s = s.replace("\u200B", "");
        return s.trim();
    }

    /**
     * 通过括号深度重建对象数组：提取每个完整 { ... } 片段，逐个校验解析后再重组
     */
    private String rebuildJsonArrayByBraces(String input) {
        String s = input;
        StringBuilder current = new StringBuilder();
        java.util.List<String> objects = new java.util.ArrayList<>();
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            current.append(c);
            if (escaped) { escaped = false; continue; }
            if (c == '\\') { escaped = true; continue; }
            if (c == '"') { inString = !inString; continue; }
            if (inString) continue;
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) {
                    String obj = current.toString();
                    int start = obj.indexOf('{');
                    int end = obj.lastIndexOf('}');
                    if (start >= 0 && end > start) {
                        String candidate = obj.substring(start, end + 1);
                        if (isValidLayoutObject(candidate)) {
                            objects.add(candidate);
                        }
                    }
                    current.setLength(0);
                }
            }
        }
        if (objects.isEmpty()) return "[]";
        String joined = String.join(",", objects);
        return "[" + joined + "]";
    }

    private boolean isValidLayoutObject(String json) {
        try {
            JsonNode node = M.readTree(json);
            if (!node.isObject()) return false;
            JsonNode bbox = node.get("bbox");
            if (bbox == null || !bbox.isArray() || bbox.size() != 4) return false;
            for (int i = 0; i < 4; i++) if (!bbox.get(i).isNumber()) return false;
            JsonNode cat = node.get("category");
            if (cat == null || !cat.isTextual()) return false;
            String category = cat.asText();
            java.util.Set<String> allow = new java.util.HashSet<>(java.util.Arrays.asList(
                "Caption","Footnote","Formula","List-item","Page-footer","Page-header",
                "Picture","Section-header","Table","Text","Title"
            ));
            if (!allow.contains(category)) return false;
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 找到最后一个完整的JSON对象
     */
    private int findLastCompleteObject(String content) {
        int braceCount = 0;
        int lastCompleteEnd = -1;
        boolean inString = false;
        boolean escaped = false;
        
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            
            if (escaped) {
                escaped = false;
                continue;
            }
            
            if (c == '\\') {
                escaped = true;
                continue;
            }
            
            if (c == '"' && !escaped) {
                inString = !inString;
                continue;
            }
            
            if (!inString) {
                if (c == '{') {
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                    if (braceCount == 0) {
                        lastCompleteEnd = i + 1;
                    }
                }
            }
        }
        
        return lastCompleteEnd;
    }
    
    // 解析 gradio data，下载 zip 或单页 json
    private int saveGradioPagesFromResult(GradioWorkflowClient gw, String resultBody, Path pdfPath) throws Exception {
        // 1) 优先找 zip
        String zipUrl = gw.findFirstZipUrl(resultBody);
        Path baseDir = pdfPath.getParent();
        if (zipUrl != null) {
            Path zip = gw.downloadTo(baseDir, zipUrl);
            Path out = baseDir.resolve("gradio_pages");
            gw.extractZip(zip, out);
            // 将 out 下的 *.json 拷贝/重命名为 .page-N.ocr.json（按文件名中 page 或自然序）
            java.util.List<Path> jsons = new java.util.ArrayList<>();
            try (java.util.stream.Stream<Path> st = java.nio.file.Files.walk(out)) {
                st.filter(p -> p.toString().endsWith(".json")).forEach(jsons::add);
            }
            jsons.sort(java.util.Comparator.comparing(Path::toString));
            int idx = 1;
            for (Path j : jsons) {
                Path target = Path.of(pdfPath.toAbsolutePath().toString() + ".page-" + idx + ".ocr.json");
                java.nio.file.Files.copy(j, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                idx++;
            }
            return jsons.size();
        }
        // 2) 逐个 json url
        java.util.List<String> urls = gw.findJsonUrls(resultBody);
        int idx = 1;
        for (String u : urls) {
            Path target = Path.of(pdfPath.toAbsolutePath().toString() + ".page-" + idx + ".ocr.json");
            Path tmp = gw.downloadTo(baseDir, u);
            java.nio.file.Files.move(tmp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            idx++;
        }
        return urls.size();
    }
    /**
     * 修复未闭合的字符串
     */
    private String fixUnclosedStrings(String content) {
        StringBuilder result = new StringBuilder();
        boolean inString = false;
        boolean escaped = false;
        
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            
            if (escaped) {
                result.append(c);
                escaped = false;
                continue;
            }
            
            if (c == '\\') {
                result.append(c);
                escaped = true;
                continue;
            }
            
            if (c == '"') {
                if (inString) {
                    // 检查是否是字符串结束
                    inString = false;
                    result.append(c);
                } else {
                    // 字符串开始
                    inString = true;
                    result.append(c);
                }
            } else {
                result.append(c);
            }
        }
        
        // 如果字符串未闭合，添加闭合引号
        if (inString) {
            result.append('"');
        }
        
        return result.toString();
    }
}

