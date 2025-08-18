package com.zhaoxinms.contract.tools.ocrcompare.compare;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.zhaoxinms.contract.tools.ocrcompare.service.OCRTaskService;
import com.zhaoxinms.contract.tools.ocrcompare.model.OCRTask;

import java.io.IOException;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

// 新增导入
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationTextMarkup;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import com.zhaoxinms.contract.tools.compare.DiffUtil;
import com.zhaoxinms.contract.tools.compare.DiffUtil.Operation;
import com.zhaoxinms.contract.tools.compare.result.CompareResult;
import com.zhaoxinms.contract.tools.compare.result.Position;
import com.zhaoxinms.contract.tools.compare.util.TextNormalizer;
import cn.hutool.core.util.StrUtil;

/**
 * OCR比对服务
 */
@Service
public class OCRCompareService {
    
    private static final Logger log = LoggerFactory.getLogger(OCRCompareService.class);
    
    @Autowired
    private OCRTaskService ocrTaskService;
    
    @Value("${file.upload.root-path:./uploads}")
    private String uploadRootPath;
    
    // 任务存储（实际项目中应该使用数据库）
    private final ConcurrentHashMap<String, OCRCompareTask> tasks = new ConcurrentHashMap<>();
    
    // 结果存储（实际项目中应该使用数据库）
    private final ConcurrentHashMap<String, OCRCompareResult> results = new ConcurrentHashMap<>();
    
    /**
     * 提交比对任务（使用MultipartFile）
     */
    public String submitCompareTask(MultipartFile oldFile, MultipartFile newFile, 
                                   OCRCompareOptions options) throws IOException {
        String oldFileName = oldFile.getOriginalFilename();
        String newFileName = newFile.getOriginalFilename();
        
        if (oldFileName == null || newFileName == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        
        // 生成任务ID
        String taskId = generateTaskId();
        
        // 创建上传目录
        createDirectories();
        
        // 保存文件到临时目录
        String oldFilePath = saveFile(oldFile, taskId + "_old");
        String newFilePath = saveFile(newFile, taskId + "_new");
        
        // 创建比对任务
        OCRCompareTask task = new OCRCompareTask(
            taskId,
            oldFileName,
            newFileName,
            oldFilePath,
            newFilePath,
            options
        );
        
        tasks.put(taskId, task);
        
        // 异步执行比对任务
        CompletableFuture.runAsync(() -> executeCompareTask(task));
        
        return taskId;
    }
    
    /**
     * 提交比对任务（使用文件路径）
     */
    public String submitCompareTaskWithPaths(String oldFilePath, String newFilePath, 
                                           OCRCompareOptions options) {
        // 生成任务ID
        String taskId = generateTaskId();
        
        // 创建比对任务，直接使用传入的文件路径
        OCRCompareTask task = new OCRCompareTask(
            taskId,
            new File(oldFilePath).getName(),
            new File(newFilePath).getName(),
            oldFilePath,
            newFilePath,
            options
        );
        
        tasks.put(taskId, task);
        
        // 异步执行比对任务
        CompletableFuture.runAsync(() -> executeCompareTask(task));
        
        return taskId;
    }
    
    /**
     * 查询任务状态
     */
    public OCRCompareTask getTaskStatus(String taskId) {
        return tasks.get(taskId);
    }
    
    /**
     * 获取所有任务
     */
    public List<OCRCompareTask> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }
    
    /**
     * 获取比对结果
     */
    public OCRCompareResult getCompareResult(String taskId) {
        return results.get(taskId);
    }
    
    /**
     * 删除任务
     */
    public boolean deleteTask(String taskId) {
        OCRCompareTask task = tasks.remove(taskId);
        if (task != null) {
            results.remove(taskId);
            // 清理文件
            cleanupTaskFiles(task);
            return true;
        }
        return false;
    }
    
    /**
     * 执行文本比对并解析OCR结果中的位置信息
     * 按照文本顺序推进索引，参考PDFComparsionHelper中的逻辑
     */
    private List<CompareResult> enrichCompareResultsWithPositionInfo(String oldText, String newText, OCRCompareOptions options,
                                                                     String oldOcrTaskId, String newOcrTaskId,
                                                                     String oldPdfPath, String newPdfPath) {
        // 创建比对结果列表
        List<CompareResult> compareResults = new ArrayList<>();
        
        try {
            // 获取OCR结果的详细信息
            com.fasterxml.jackson.databind.JsonNode oldOcrResult = ocrTaskService.getOCRResultViaHttp(oldOcrTaskId);
            com.fasterxml.jackson.databind.JsonNode newOcrResult = ocrTaskService.getOCRResultViaHttp(newOcrTaskId);
            
            if (oldOcrResult == null || newOcrResult == null) {
                log.warn("无法获取OCR结果，oldResult={}, newResult={}", oldOcrResult != null, newOcrResult != null);
                return new ArrayList<>();
            }
            
            // 解析OCR结果中的页面和位置信息
            log.info("开始解析OCR结果位置信息... oldTaskId={}, newTaskId={}", oldOcrTaskId, newOcrTaskId);
            
            // 获取json_data
            com.fasterxml.jackson.databind.JsonNode oldJsonData = oldOcrResult.path("json_data");
            com.fasterxml.jackson.databind.JsonNode newJsonData = newOcrResult.path("json_data");
            if (oldJsonData.isMissingNode() || newJsonData.isMissingNode()) {
                log.warn("OCR结果缺少 json_data 字段，oldHasJsonData={} newHasJsonData={}", !oldJsonData.isMissingNode(), !newJsonData.isMissingNode());
                return new ArrayList<>();
            }
            
            // 解析旧/新文档的OCR结果
            List<PageTextInfo> oldPageInfos = parseOCRResultToPageInfos(oldJsonData);
            List<PageTextInfo> newPageInfos = parseOCRResultToPageInfos(newJsonData);
            log.info("OCR解析页信息：oldPages={}, newPages={}", oldPageInfos.size(), newPageInfos.size());
            
            // 读取PDF页尺寸（pt）用于坐标换算
            float[] oldPageWidths = null, oldPageHeights = null;
            float[] newPageWidths = null, newPageHeights = null;
            try (PDDocument oldDoc = PDDocument.load(new File(oldPdfPath));
                 PDDocument newDoc = PDDocument.load(new File(newPdfPath))) {
                int oldN = oldDoc.getNumberOfPages();
                oldPageWidths = new float[oldN];
                oldPageHeights = new float[oldN];
                for (int i = 0; i < oldN; i++) {
                    PDPage p = oldDoc.getPage(i);
                    PDRectangle mb = p.getMediaBox();
                    oldPageWidths[i] = mb.getWidth();
                    oldPageHeights[i] = mb.getHeight();
                }
                int newN = newDoc.getNumberOfPages();
                newPageWidths = new float[newN];
                newPageHeights = new float[newN];
                for (int i = 0; i < newN; i++) {
                    PDPage p = newDoc.getPage(i);
                    PDRectangle mb = p.getMediaBox();
                    newPageWidths[i] = mb.getWidth();
                    newPageHeights[i] = mb.getHeight();
                }
            } catch (Exception e) {
                log.warn("读取PDF页尺寸失败", e);
            }
            
            // 构建文本位置索引
            List<TextPositionInfo> oldTextPositions = buildTextPositionIndex(oldPageInfos);
            List<TextPositionInfo> newTextPositions = buildTextPositionIndex(newPageInfos);
            
            if (oldText == null || newText == null) {
                log.warn("OCR文本内容为空，无法进行位置匹配");
                return new ArrayList<>();
            }
            
            // 执行文本比对，获取差异
            DiffUtil dmp = new DiffUtil();
            String left = oldText == null ? "" : oldText;
            String right = newText == null ? "" : newText;
            
            // 使用TextNormalizer进行文本标准化，排除符号干扰
            boolean ignoreCase = options != null ? options.isIgnoreCase() : true;
            boolean ignoreWhitespace = options != null ? options.isIgnoreWhitespace() : true;
            boolean ignorePunctuation = options != null ? options.isIgnorePunctuation() : false;
            
            left = TextNormalizer.normalizeForComparison(left, ignoreCase, ignoreWhitespace, ignorePunctuation);
            right = TextNormalizer.normalizeForComparison(right, ignoreCase, ignoreWhitespace, ignorePunctuation);
            
            // 执行文本比对
            java.util.LinkedList<DiffUtil.Diff> diffList = dmp.diff_main(left, right);
            dmp.diff_cleanupSemantic(diffList);
            
            // 按照文本顺序推进索引，为每个差异分配位置信息
            int oldCurrentPosition = 0;
            int newCurrentPosition = 0;
            
            int assignedOld = 0;
            int assignedNew = 0;
            
            // 遍历所有差异，按顺序处理
            for (DiffUtil.Diff d : diffList) {
                String realText = d.text.replaceAll("¶", "").replaceAll("\r", "").replaceAll("\n", "");
                realText = realText.replaceAll(" ", "");
                if (d.operation == Operation.EQUAL) {
                    // 相等部分，同步推进两个索引,但是要忽略空格的影响。
                    oldCurrentPosition += realText.length();
                    newCurrentPosition += realText.length();
                    continue;
                }
                
                // 使用TextNormalizer过滤掉纯符号的差异
                String normalizedText = TextNormalizer.normalizeForComparison(realText, true, true, true);
                if (StrUtil.isBlank(normalizedText)) {
                    if (d.operation == Operation.DELETE) {
                        oldCurrentPosition += realText.length();
                    } else if (d.operation == Operation.INSERT) {
                        newCurrentPosition += realText.length();
                    }
                    continue;
                }
                
                // 处理差异部分
                if (d.operation == Operation.DELETE) {
                    // 创建比对结果，先设置默认位置
                    Position oldPos = new Position(null, 1);
                    Position newPos = new Position(null, 1);
                    CompareResult result = new CompareResult(oldPos, newPos, d);
                    
                    // 在旧文档中查找位置
                    TextPositionInfo posInfo = getTextPositionAtIndex(oldTextPositions, oldCurrentPosition);
                    if (posInfo != null) {
                        int pageIdx0 = Math.max(0, posInfo.pageIndex - 1); // 转为0-based
                        float pageW = (oldPageWidths != null && pageIdx0 < oldPageWidths.length) ? oldPageWidths[pageIdx0] : 595f;
                        float pageH = (oldPageHeights != null && pageIdx0 < oldPageHeights.length) ? oldPageHeights[pageIdx0] : 842f;
                        
                        // 坐标转换
                        float scaleX = pageW / Math.max(1f, posInfo.imageWidth);
                        float scaleY = pageH / Math.max(1f, posInfo.imageHeight);
                        float x = posInfo.x * scaleX;
                        float yTop = posInfo.y * scaleY;
                        
                        Position newOldPos = new Position(x, yTop, pageW, pageH, pageIdx0);
                        if (posInfo.width > 0 && posInfo.height > 0) {
                            newOldPos.setRectWidth(posInfo.width * scaleX);
                            newOldPos.setRectHeight(posInfo.height * scaleY);
                        }
                        
                        // 如果文本跨多行，添加多个矩形
                        List<TextPositionInfo> multiLinePositions = getMultiLinePositions(oldTextPositions, oldCurrentPosition, realText.length());
                        if (multiLinePositions.size() > 1) {
                            for (TextPositionInfo linePos : multiLinePositions) {
                                newOldPos.addRect(
                                    linePos.x * scaleX,
                                    linePos.y * scaleY,
                                    linePos.width * scaleX,
                                    linePos.height * scaleY
                                );
                            }
                        } else if (multiLinePositions.size() == 1) {
                        	newOldPos.setRectWidth(multiLinePositions.get(0).width * scaleX);
                        	newOldPos.setRectHeight(multiLinePositions.get(0).height * scaleY);
                        }
                        
                        result.setOldPosition(newOldPos);
                        assignedOld++;
                        log.debug("DELETE 定位成功: text='{}' page={} x={} y={}", 
                            abbreviate(realText, 60), pageIdx0 + 1, x, yTop);
                    }
                    
                    // 在新文档中查找当前索引位置的信息（用于设置新文档的位置参考）
                    TextPositionInfo newPosInfo = getTextPositionAtIndex(newTextPositions, Math.max(0, newCurrentPosition-1));
                    if (newPosInfo != null) {
                        int newPageIdx0 = Math.max(0, newPosInfo.pageIndex - 1); // 转为0-based
                        float newPageW = (newPageWidths != null && newPageIdx0 < newPageWidths.length) ? newPageWidths[newPageIdx0] : 595f;
                        float newPageH = (newPageHeights != null && newPageIdx0 < newPageHeights.length) ? newPageHeights[newPageIdx0] : 842f;
                        
                        // 坐标转换
                        float newScaleX = newPageW / Math.max(1f, newPosInfo.imageWidth);
                        float newScaleY = newPageH / Math.max(1f, newPosInfo.imageHeight);
                        float newX = newPosInfo.x * newScaleX;
                        float newYTop = newPosInfo.y * newScaleY;
                        
                        Position newNewPos = new Position(newX, newYTop, newPageW, newPageH, newPageIdx0);
                        if (newPosInfo.width > 0 && newPosInfo.height > 0) {
                            newNewPos.setRectWidth(newPosInfo.width * newScaleX);
                            newNewPos.setRectHeight(newPosInfo.height * newScaleY);
                        }
                        
                        result.setNewPosition(newNewPos);
                        log.debug("DELETE 新文档定位成功: page={} x={} y={}", 
                            newPageIdx0 + 1, newX, newYTop);
                    }
                    
                    compareResults.add(result);
                    // 推进旧文档索引
                    oldCurrentPosition += realText.length();
                    
                } else if (d.operation == Operation.INSERT) {
                    // 创建比对结果
                    Position oldPos = new Position(null, 1);
                    Position newPos = new Position(null, 1);
                    CompareResult result = new CompareResult(oldPos, newPos, d);
                    
                    // 在旧文档中查找当前索引位置的信息（用于设置旧文档的位置参考）
                    TextPositionInfo oldPosInfo = getTextPositionAtIndex(oldTextPositions, Math.max(0, oldCurrentPosition-1));
                    if (oldPosInfo != null) {
                        int newPageIdx0 = Math.max(0, oldPosInfo.pageIndex - 1); // 转为0-based
                        float newPageW = (newPageWidths != null && newPageIdx0 < newPageWidths.length) ? newPageWidths[newPageIdx0] : 595f;
                        float newPageH = (newPageHeights != null && newPageIdx0 < newPageHeights.length) ? newPageHeights[newPageIdx0] : 842f;
                        
                        // 坐标转换
                        float newScaleX = newPageW / Math.max(1f, oldPosInfo.imageWidth);
                        float newScaleY = newPageH / Math.max(1f, oldPosInfo.imageHeight);
                        float newX = oldPosInfo.x * newScaleX;
                        float newYTop = oldPosInfo.y * newScaleY;
                        
                        Position oldNewPos = new Position(newX, newYTop, newPageW, newPageH, newPageIdx0);
                        if (oldPosInfo.width > 0 && oldPosInfo.height > 0) {
                        	oldNewPos.setRectWidth(oldPosInfo.width * newScaleX);
                        	oldNewPos.setRectHeight(oldPosInfo.height * newScaleY);
                        }
                        
                        result.setOldPosition(oldNewPos);
                        log.debug("DELETE 新文档定位成功: page={} x={} y={}", 
                            newPageIdx0 + 1, newX, newYTop);
                    }
                    
                    // 在新文档中查找位置
                    TextPositionInfo posInfo = getTextPositionAtIndex(newTextPositions, newCurrentPosition);
                    if (posInfo != null) {
                        int pageIdx0 = Math.max(0, posInfo.pageIndex - 1); // 转为0-based
                        float pageW = (newPageWidths != null && pageIdx0 < newPageWidths.length) ? newPageWidths[pageIdx0] : 595f;
                        float pageH = (newPageHeights != null && pageIdx0 < newPageHeights.length) ? newPageHeights[pageIdx0] : 842f;
                        
                        // 坐标转换
                        float scaleX = pageW / Math.max(1f, posInfo.imageWidth);
                        float scaleY = pageH / Math.max(1f, posInfo.imageHeight);
                        float x = posInfo.x * scaleX;
                        float yTop = posInfo.y * scaleY;
                        
                        Position newNewPos = new Position(x, yTop, pageW, pageH, pageIdx0);
                        if (posInfo.width > 0 && posInfo.height > 0) {
                            newNewPos.setRectWidth(posInfo.width * scaleX);
                            newNewPos.setRectHeight(posInfo.height * scaleY);
                        }
                        
                        // 如果文本跨多行，添加多个矩形
                        List<TextPositionInfo> multiLinePositions = getMultiLinePositions(newTextPositions, newCurrentPosition, realText.length());
                        if (multiLinePositions.size() > 1) {
                            for (TextPositionInfo linePos : multiLinePositions) {
                                newNewPos.addRect(
                                    linePos.x * scaleX,
                                    linePos.y * scaleY,
                                    linePos.width * scaleX,
                                    linePos.height * scaleY
                                );
                            }
                        } else if (multiLinePositions.size() == 1) {
                        	newNewPos.setRectWidth(multiLinePositions.get(0).width * scaleX);
                            newNewPos.setRectHeight(multiLinePositions.get(0).height * scaleY);
                        }
                        
                        result.setNewPosition(newNewPos);
                        assignedNew++;
                        log.debug("INSERT 定位成功: text='{}' page={} x={} y={}", 
                            abbreviate(realText, 60), pageIdx0 + 1, x, yTop);
                    }
                    
                    compareResults.add(result);
                    // 推进新文档索引
                    newCurrentPosition += realText.length();
                }
            }
            
            log.info("OCR位置信息解析完成，共处理 {} 个比对结果，定位成功：old={}，new={}", results.size(), assignedOld, assignedNew);
            
            return compareResults;
            
        } catch (Exception e) {
            log.error("解析OCR位置信息失败", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 构建文本位置索引，将OCR结果转换为线性的文本位置序列
     */
    private List<TextPositionInfo> buildTextPositionIndex(List<PageTextInfo> pageInfos) {
        List<TextPositionInfo> positions = new ArrayList<>();
        
        for (PageTextInfo pageInfo : pageInfos) {
            for (TextItemInfo textItem : pageInfo.getTextItems()) {
                String text = textItem.getText();
                if (text == null || text.isEmpty()) continue;
                
                // 如果有字符级信息，使用字符级信息
                if (textItem.getCharText() != null && !textItem.getCharText().isEmpty() && 
                    textItem.getCharXs() != null && textItem.getCharYs() != null) {
                    
                    String charText = textItem.getCharText();
                    for (int i = 0; i < charText.length() && i < textItem.getCharXs().length; i++) {
                        float x = textItem.getCharXs()[i];
                        float y = textItem.getCharYs()[i];
                        float width = 0;
                        float height = 0;
                        
                        // 计算字符宽度
                        if (textItem.getCharX2s() != null && i < textItem.getCharX2s().length) {
                            width = textItem.getCharX2s()[i] - x;
                        }
                        
                        // 计算字符高度
                        if (textItem.getCharBottomYs() != null && i < textItem.getCharBottomYs().length) {
                            height = textItem.getCharBottomYs()[i] - y;
                        }
                        
                        if (width <= 0) width = 10; // 默认宽度
                        if (height <= 0) height = textItem.getHeight(); // 使用行高
                        
                        TextPositionInfo posInfo = new TextPositionInfo(
                            String.valueOf(charText.charAt(i)),
                            pageInfo.getPageIndex(),
                            x, y, width, height,
                            pageInfo.getImageWidth(),
                            pageInfo.getImageHeight()
                        );
                        positions.add(posInfo);
                    }
                } else {
                    // 没有字符级信息，使用行级信息
                    // 将文本拆分为单个字符，每个字符使用相同的行坐标，但水平位置按比例分配
                    float charWidth = textItem.getWidth() / Math.max(1, text.length());
                    for (int i = 0; i < text.length(); i++) {
                        float x = textItem.getX() + (i * charWidth);
                        TextPositionInfo posInfo = new TextPositionInfo(
                            String.valueOf(text.charAt(i)),
                            pageInfo.getPageIndex(),
                            x, textItem.getY(), charWidth, textItem.getHeight(),
                            pageInfo.getImageWidth(),
                            pageInfo.getImageHeight()
                        );
                        positions.add(posInfo);
                    }
                }
            }
        }
        
        return positions;
    }
    
    /**
     * 获取指定索引位置的文本位置信息
     */
    private TextPositionInfo getTextPositionAtIndex(List<TextPositionInfo> positions, int index) {
        if (positions == null || positions.isEmpty() || index < 0 || index >= positions.size()) {
            return null;
        }
        return positions.get(index);
    }
    
    /**
     * 获取跨多行的文本位置信息
     */
    private List<TextPositionInfo> getMultiLinePositions(List<TextPositionInfo> positions, int startIndex, int length) {
        List<TextPositionInfo> result = new ArrayList<>();
        if (positions == null || positions.isEmpty() || startIndex < 0 || length <= 0) {
            return result;
        }
        
        int endIndex = Math.min(startIndex + length - 1, positions.size() - 1);
        if (endIndex < startIndex) {
            return result;
        }
        
        // 按行分组（使用y坐标作为行标识，允许小误差）
        Map<Integer, List<TextPositionInfo>> lineGroups = new HashMap<>();
        final float tolerance = 8.0f; // 8像素的容差，适应常见字体大小
        
        for (int i = startIndex; i <= endIndex; i++) {
            TextPositionInfo pos = positions.get(i);
            if (pos == null) continue;
            
            // 使用四舍五入而不是截断，避免边界问题
            int lineKey = Math.round(pos.y / tolerance);
            if (!lineGroups.containsKey(lineKey)) {
                lineGroups.put(lineKey, new ArrayList<>());
            }
            lineGroups.get(lineKey).add(pos);
        }
        
        // 为每一行创建一个包围盒
        for (List<TextPositionInfo> line : lineGroups.values()) {
            if (line.isEmpty()) continue;
            
            float minX = Float.MAX_VALUE;
            float minY = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE;
            float maxY = -Float.MAX_VALUE;
            int pageIndex = line.get(0).pageIndex;
            int imageWidth = line.get(0).imageWidth;
            int imageHeight = line.get(0).imageHeight;
            
            for (TextPositionInfo pos : line) {
                minX = Math.min(minX, pos.x);
                minY = Math.min(minY, pos.y);
                maxX = Math.max(maxX, pos.x + pos.width);
                maxY = Math.max(maxY, pos.y + pos.height);
            }
            
            TextPositionInfo lineBox = new TextPositionInfo(
                "", // 不需要文本内容
                pageIndex,
                minX, minY, maxX - minX, maxY - minY,
                imageWidth, imageHeight
            );
            result.add(lineBox);
        }
        
        return result;
    }
    
    /**
     * 文本位置信息类（用于构建文本位置索引）
     */
    private static class TextPositionInfo {
        private final String text;
        private final int pageIndex; // 1-based
        private final float x; // 左上角x（px）
        private final float y; // 左上角y（px）
        private final float width;
        private final float height;
        private final int imageWidth;
        private final int imageHeight;
        
        public TextPositionInfo(String text, int pageIndex, float x, float y, float width, float height,
                                int imageWidth, int imageHeight) {
            this.text = text;
            this.pageIndex = pageIndex;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.imageWidth = imageWidth;
            this.imageHeight = imageHeight;
        }
    }
    
    
    /**
     * 执行OCR比对任务
     */
    private void executeCompareTask(OCRCompareTask task) {
        try {
            task.setStatus(OCRCompareTask.TaskStatus.OCR_PROCESSING);
            task.setStartTime(LocalDateTime.now());
            task.setCurrentStep(1, "开始OCR识别");
            
            // 步骤1: 检查文件是否存在
            task.updateProgress(5.0, "检查文档格式...");
            String oldFilePath = task.getOldFilePath();
            String newFilePath = task.getNewFilePath();
            
            // 检查文件是否存在
            if (!new File(oldFilePath).exists() || !new File(newFilePath).exists()) {
                task.setStatus(OCRCompareTask.TaskStatus.FAILED);
                task.setErrorMessage("源文件不存在");
                return;
            }
            
            // 步骤2: OCR识别旧文档
            task.setCurrentStep(2, "OCR识别旧文档");
            task.updateProgress(20.0, "OCR识别旧文档...");
            String oldOcrTaskId = ocrTaskService.submitOCRTask(oldFilePath, task.getOptions().isIgnoreSeals());
            task.setOldOcrTaskId(oldOcrTaskId);
            
            // 步骤3: OCR识别新文档
            task.setCurrentStep(3, "OCR识别新文档");
            task.updateProgress(35.0, "OCR识别新文档...");
            String newOcrTaskId = ocrTaskService.submitOCRTask(newFilePath, task.getOptions().isIgnoreSeals());
            task.setNewOcrTaskId(newOcrTaskId);
            
            // 等待OCR任务完成并监控进度
            waitForOCRCompletion(task);
            
            if (task.getStatus() == OCRCompareTask.TaskStatus.FAILED) {
                return;
            }
            
            // 步骤4: 执行文本比对
            task.setCurrentStep(4, "执行文本比对");
            task.updateProgress(70.0, "执行文本比对...");
            
            // 获取OCR结果
            String oldText = getOCRResultText(task.getOldOcrTaskId());
            String newText = getOCRResultText(task.getNewOcrTaskId());
            
            if (oldText == null || newText == null) {
                task.setStatus(OCRCompareTask.TaskStatus.FAILED);
                task.setErrorMessage("OCR识别结果为空");
                return;
            }
            
            // 执行文本比对并解析OCR结果中的位置信息（结合PDF页尺寸进行坐标换算）
            List<CompareResult> compareResults = enrichCompareResultsWithPositionInfo(
                oldText,
                newText,
                task.getOptions(),
                task.getOldOcrTaskId(),
                task.getNewOcrTaskId(),
                oldFilePath,
                newFilePath
            );
            
            // 步骤5: 将比对结果回写到PDF文件
            task.setCurrentStep(5, "回写比对结果到PDF");
            task.updateProgress(85.0, "回写比对结果到PDF...");
            
            String resultDir = uploadRootPath + "/ocr-compare/results/" + task.getTaskId();
            java.nio.file.Path resultDirPath = java.nio.file.Paths.get(resultDir);
            java.nio.file.Files.createDirectories(resultDirPath);
            
            String annotatedOldPdfPath = resultDir + "/old_annotated.pdf";
            String annotatedNewPdfPath = resultDir + "/new_annotated.pdf";
            
            // 回写比对结果到PDF
            annotatePDFWithResults(oldFilePath, annotatedOldPdfPath, compareResults, "DELETE");
            annotatePDFWithResults(newFilePath, annotatedNewPdfPath, compareResults, "INSERT");
            
            // 创建OCR比对结果
            OCRCompareResult result = new OCRCompareResult();
            result.setTaskId(task.getTaskId());
            result.setOldPdfUrl("/api/ocr-compare/files/" + task.getTaskId() + "/old_annotated.pdf");
            result.setNewPdfUrl("/api/ocr-compare/files/" + task.getTaskId() + "/new_annotated.pdf");
            result.setOldOcrTaskId(task.getOldOcrTaskId());
            result.setNewOcrTaskId(task.getNewOcrTaskId());
            result.setOldOcrTaskId(task.getOldOcrTaskId());
            result.setNewOcrTaskId(task.getNewOcrTaskId());
            
            // 转换比对结果为OCR比对结果格式
            List<Map<String, Object>> differences = convertCompareResultsToDifferences(compareResults);
            result.setDifferences(differences);
            
            // 计算相似度（简化计算，基于差异数量）
            double similarity = 1.0;
            if (differences != null && !differences.isEmpty()) {
                // 这里可以根据实际需求调整相似度计算逻辑
                similarity = Math.max(0.0, 1.0 - (differences.size() * 0.1));
            }
            result.setSimilarity(similarity);
            
            // 生成比对摘要
            result.generateSummary();
            
            // 设置前端需要的summary字段
            Map<String, Object> summary = new HashMap<>();
            summary.put("totalDifferences", differences != null ? differences.size() : 0);
            
            // 统计删除和新增的数量
            int deletions = 0, insertions = 0;
            if (differences != null) {
                for (Map<String, Object> diff : differences) {
                    String operation = (String) diff.get("operation");
                    if ("DELETE".equals(operation)) {
                        deletions++;
                    } else if ("INSERT".equals(operation)) {
                        insertions++;
                    }
                }
            }
            summary.put("deletions", deletions);
            summary.put("insertions", insertions);
            
            result.setSummary(summary);
            
            // 步骤6: 保存比对结果
            task.setCurrentStep(6, "保存比对结果");
            task.updateProgress(95.0, "保存比对结果...");
            
            results.put(task.getTaskId(), result);
            log.info("OCR比对结果生成完成：taskId={} diffs={} oldPdf={} newPdf={}", task.getTaskId(),
                differences == null ? 0 : differences.size(), result.getOldPdfUrl(), result.getNewPdfUrl());
            task.setStatus(OCRCompareTask.TaskStatus.COMPLETED);
            task.setCompletedTime(LocalDateTime.now());
            task.updateProgress(100.0, "比对完成");
            
            log.info("OCR比对任务完成: {}", task.getTaskId());
            
        } catch (Exception e) {
            task.setStatus(OCRCompareTask.TaskStatus.FAILED);
            task.setErrorMessage("OCR比对执行异常: " + e.getMessage());
            log.error("OCR比对任务执行失败: {}", task.getTaskId(), e);
        }
    }
    
    /**
     * 解析OCR结果JSON为页面文本信息列表
     */
    private List<PageTextInfo> parseOCRResultToPageInfos(com.fasterxml.jackson.databind.JsonNode ocrResult) {
        List<PageTextInfo> pageInfos = new ArrayList<>();
        
        try {
            if (ocrResult == null) {
                log.warn("OCR结果为空，无法解析页信息");
                return pageInfos;
            }
            com.fasterxml.jackson.databind.JsonNode pages = ocrResult.path("pages");
            if (pages.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode page : pages) {
                    int pageIndex = page.path("page_index").asInt(1);
                    com.fasterxml.jackson.databind.JsonNode items = page.path("items");
                    int imgW = page.path("image_width").asInt(0);
                    int imgH = page.path("image_height").asInt(0);
                    if (imgW <= 0 || imgH <= 0) {
                        log.debug("OCR页图像尺寸缺失或异常: page={} image_width={} image_height={}", pageIndex, imgW, imgH);
                    }
                    
                    List<TextItemInfo> textItems = new ArrayList<>();
                    if (items.isArray()) {
                        for (com.fasterxml.jackson.databind.JsonNode item : items) {
                            String text = item.path("text").asText("");
                            com.fasterxml.jackson.databind.JsonNode box = item.path("box");
                            
                            if (!text.isEmpty() && box.isArray() && box.size() >= 4) {
                                // 解析坐标框信息
                                // box 是四点矩形：[[x1,y1],[x2,y2],[x3,y3],[x4,y4]]，取外接矩形
                                double xMin = Math.min(Math.min(box.get(0).get(0).asDouble(0), box.get(1).get(0).asDouble(0)),
                                                       Math.min(box.get(2).get(0).asDouble(0), box.get(3).get(0).asDouble(0)));
                                double xMax = Math.max(Math.max(box.get(0).get(0).asDouble(0), box.get(1).get(0).asDouble(0)),
                                                       Math.max(box.get(2).get(0).asDouble(0), box.get(3).get(0).asDouble(0)));
                                double yMin = Math.min(Math.min(box.get(0).get(1).asDouble(0), box.get(1).get(1).asDouble(0)),
                                                       Math.min(box.get(2).get(1).asDouble(0), box.get(3).get(1).asDouble(0)));
                                double yMax = Math.max(Math.max(box.get(0).get(1).asDouble(0), box.get(1).get(1).asDouble(0)),
                                                       Math.max(box.get(2).get(1).asDouble(0), box.get(3).get(1).asDouble(0)));
                                float x = (float)xMin;
                                float y = (float)yMin; // OCR 坐标以左上为原点，y 越大越靠下
                                float width = (float)(xMax - xMin);
                                float height = (float)(yMax - yMin);
                                
                                // 尝试解析字符级位置信息 chars
                                StringBuilder charText = new StringBuilder();
                                java.util.List<Float> xs = new java.util.ArrayList<>();
                                java.util.List<Float> ys = new java.util.ArrayList<>();
                                java.util.List<Float> x2s = new java.util.ArrayList<>();
                                java.util.List<Float> bys = new java.util.ArrayList<>();
                                com.fasterxml.jackson.databind.JsonNode chars = item.path("chars");
                                if (chars.isArray()) {
                                    for (com.fasterxml.jackson.databind.JsonNode ch : chars) {
                                        String c = ch.path("ch").asText("");
                                        com.fasterxml.jackson.databind.JsonNode cbox = ch.path("box");
                                        if (c != null && !c.isBlank() && cbox.isArray() && cbox.size() >= 4 && cbox.get(0).isArray()) {
                                            // 四点：lt, rt, rb, lb
                                            float cx = (float) cbox.get(0).get(0).asDouble(0);
                                            float cy = (float) cbox.get(0).get(1).asDouble(0);
                                            float cx2 = (float) cbox.get(1).get(0).asDouble(0);
                                            // bottom y 可取 rb.y 与 lb.y 的较大值
                                            float cby = (float) Math.max(cbox.get(2).get(1).asDouble(0), cbox.get(3).get(1).asDouble(0));
                                            charText.append(c);
                                            xs.add(cx);
                                            ys.add(cy);
                                            x2s.add(cx2);
                                            bys.add(cby);
                                        }
                                    }
                                }
                                float[] xarr = null;
                                float[] yarr = null;
                                float[] x2arr = null;
                                float[] byarr = null;
                                if (!xs.isEmpty()) {
                                    xarr = new float[xs.size()];
                                    for (int i = 0; i < xs.size(); i++) {
                                        xarr[i] = xs.get(i);
                                    }
                                }
                                if (!ys.isEmpty()) {
                                    yarr = new float[ys.size()];
                                    for (int i = 0; i < ys.size(); i++) {
                                        yarr[i] = ys.get(i);
                                    }
                                }
                                if (!x2s.isEmpty()) {
                                    x2arr = new float[x2s.size()];
                                    for (int i = 0; i < x2s.size(); i++) x2arr[i] = x2s.get(i);
                                }
                                if (!bys.isEmpty()) {
                                    byarr = new float[bys.size()];
                                    for (int i = 0; i < bys.size(); i++) byarr[i] = bys.get(i);
                                }
                                TextItemInfo textItem = new TextItemInfo(text, x, y, width, height, imgW, imgH,
                                                                         charText.toString(), xarr, yarr, x2arr, byarr);
                                textItems.add(textItem);
                            }
                        }
                    } else {
                        log.debug("OCR页无 items 数据: page={}", pageIndex);
                    }
                    
                    PageTextInfo pageInfo = new PageTextInfo(pageIndex, imgW, imgH, textItems);
                    pageInfos.add(pageInfo);
                }
            } else {
                log.warn("OCR结果 'pages' 字段不是数组: {}", ocrResult.toString());
            }
        } catch (Exception e) {
            log.warn("解析OCR结果JSON失败", e);
        }
        
        return pageInfos;
    }
    
    /**
     * 在页面文本信息中查找指定文本的位置
     */
    // 原始匹配结果（用于后续按PDF页尺寸换算）
    private static class RawMatch {
        final int pageIndex; // 1-based
        final float x; // 像素坐标（左上为原点）
        final float y; // 像素坐标（左上为原点）
        final int imageWidth;
        final int imageHeight;
        RawMatch(int pageIndex, float x, float y, int imageWidth, int imageHeight) {
            this(pageIndex, x, y, imageWidth, imageHeight, 0, 0);
        }
        final float boxWidthPx;
        final float boxHeightPx;
        RawMatch(int pageIndex, float x, float y, int imageWidth, int imageHeight, float boxWidthPx, float boxHeightPx) {
            this.pageIndex = pageIndex;
            this.x = x;
            this.y = y;
            this.imageWidth = imageWidth;
            this.imageHeight = imageHeight;
            this.boxWidthPx = boxWidthPx;
            this.boxHeightPx = boxHeightPx;
        }
    }

    /**
     * 多段原始匹配（用于跨行分段矩形）
     */
    private static class RawMatchMulti {
        final int pageIndex; // 1-based
        final int imageWidth;
        final int imageHeight;
        final java.util.List<float[]> rectsPx; // 每个为 [x, yTop, w, h]，像素坐标
        RawMatchMulti(int pageIndex, int imageWidth, int imageHeight, java.util.List<float[]> rectsPx) {
            this.pageIndex = pageIndex;
            this.imageWidth = imageWidth;
            this.imageHeight = imageHeight;
            this.rectsPx = rectsPx;
        }
    }

    /**
     * 在OCR页文本中查找目标文本，返回第一个匹配的外接框左上角像素坐标与图像尺寸
     */
    private RawMatch findRawMatchInPages(String targetText, List<PageTextInfo> pageInfos) {
        if (targetText == null) return null;
        String cleanTargetText = normalize(targetText);
        if (cleanTargetText.isEmpty()) return null;

        for (PageTextInfo pageInfo : pageInfos) {
            for (TextItemInfo textItem : pageInfo.getTextItems()) {
                String t = normalize(textItem.getText());
                if (t == null || t.isEmpty()) continue;
                // 优先字符级匹配，提高定位精度
                String ct = textItem.getCharText();
                if (ct != null && !ct.isEmpty() && textItem.getCharXs() != null && textItem.getCharYs() != null) {
                    String nc = normalize(ct);
                    int idx = nc.indexOf(cleanTargetText);
                    if (idx >= 0 && idx < textItem.getCharXs().length && idx < textItem.getCharYs().length) {
                        // 取从 idx 开始，长度为 cleanTargetText.length 的字符序列的整体包围盒
                        int end = Math.min(idx + cleanTargetText.length() - 1, textItem.getCharXs().length - 1);
                        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
                        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
                        for (int k = idx; k <= end; k++) {
                            float ltX = textItem.getCharXs()[k];
                            float ltY = textItem.getCharYs()[k];
                            float rtX = (textItem.getCharX2s() != null && k < textItem.getCharX2s().length) ? textItem.getCharX2s()[k] : (ltX + 2);
                            float bY = (textItem.getCharBottomYs() != null && k < textItem.getCharBottomYs().length) ? textItem.getCharBottomYs()[k] : (ltY + textItem.getHeight());
                            minX = Math.min(minX, Math.min(ltX, rtX));
                            maxX = Math.max(maxX, Math.max(ltX, rtX));
                            minY = Math.min(minY, ltY);
                            maxY = Math.max(maxY, bY);
                        }
                        float bw = Math.max(2f, maxX - minX);
                        float bh = Math.max(2f, maxY - minY);
                        return new RawMatch(pageInfo.getPageIndex(), minX, minY, pageInfo.getImageWidth(), pageInfo.getImageHeight(), bw, bh);
                    }
                }
                // 回退：行级包含
                if (t.contains(cleanTargetText) || cleanTargetText.contains(t)) {
                    return new RawMatch(pageInfo.getPageIndex(), textItem.getX(), textItem.getY(), pageInfo.getImageWidth(), pageInfo.getImageHeight(), textItem.getWidth(), textItem.getHeight());
                }
            }
        }
        return null;
    }

    /**
     * 将字符级匹配扩展为“跨行分段”：
     * - 当命中的字符序列在同一行：合并为单段 [minX, minY, w, h]
     * - 当换行：按行拆分，返回多段
     */
    private RawMatchMulti findRawMatchMultiLine(String targetText, List<PageTextInfo> pageInfos) {
        if (targetText == null) return null;
        String cleanTargetText = normalize(targetText);
        if (cleanTargetText.isEmpty()) return null;
        for (PageTextInfo pageInfo : pageInfos) {
            for (TextItemInfo textItem : pageInfo.getTextItems()) {
                String ct = textItem.getCharText();
                if (ct == null || ct.isEmpty() || textItem.getCharXs() == null || textItem.getCharYs() == null) continue;
                String nc = normalize(ct);
                int idx = nc.indexOf(cleanTargetText);
                if (idx < 0) continue;
                int end = Math.min(idx + cleanTargetText.length() - 1, textItem.getCharXs().length - 1);
                // 按 yTop 分组（同一行的 yTop 近似相等，容差使用 2px）
                java.util.Map<Integer, java.util.List<Integer>> groups = new java.util.LinkedHashMap<>();
                final float tol = 2f;
                for (int k = idx; k <= end; k++) {
                    float yTop = textItem.getCharYs()[k];
                    // 归一化key
                    int key = Math.round(yTop / tol);
                    groups.computeIfAbsent(key, kk -> new java.util.ArrayList<>()).add(k);
                }
                java.util.List<float[]> rects = new java.util.ArrayList<>();
                for (java.util.List<Integer> ks : groups.values()) {
                    float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
                    float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
                    for (int id : ks) {
                        float ltX = textItem.getCharXs()[id];
                        float ltY = textItem.getCharYs()[id];
                        float rtX = (textItem.getCharX2s() != null && id < textItem.getCharX2s().length) ? textItem.getCharX2s()[id] : (ltX + 2);
                        float bY = (textItem.getCharBottomYs() != null && id < textItem.getCharBottomYs().length) ? textItem.getCharBottomYs()[id] : (ltY + textItem.getHeight());
                        minX = Math.min(minX, Math.min(ltX, rtX));
                        maxX = Math.max(maxX, Math.max(ltX, rtX));
                        minY = Math.min(minY, ltY);
                        maxY = Math.max(maxY, bY);
                    }
                    rects.add(new float[]{minX, minY, Math.max(2f, maxX - minX), Math.max(2f, maxY - minY)});
                }
                return new RawMatchMulti(pageInfo.getPageIndex(), pageInfo.getImageWidth(), pageInfo.getImageHeight(), rects);
            }
        }
        return null;
    }

    private String normalize(String s) {
        if (s == null) return "";
        // 使用TextNormalizer进行标准化处理
        String r = s.replaceAll("¶", "").replaceAll("\r", "").replaceAll("\n", "");
        // 使用TextNormalizer的标准化方法，排除符号干扰
        r = TextNormalizer.normalizeForComparison(r, true, true, false);
        return r;
    }

    private String abbreviate(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, Math.max(0, max - 3)) + "...";
    }
    
    /**
     * 页面文本信息类
     */
    private static class PageTextInfo {
        private final int pageIndex; // 1-based
        private final int imageWidth; // OCR图像宽（px）
        private final int imageHeight; // OCR图像高（px）
        private final List<TextItemInfo> textItems;
        
        public PageTextInfo(int pageIndex, int imageWidth, int imageHeight, List<TextItemInfo> textItems) {
            this.pageIndex = pageIndex;
            this.imageWidth = imageWidth;
            this.imageHeight = imageHeight;
            this.textItems = textItems;
        }
        
        public int getPageIndex() { return pageIndex; }
        public int getImageWidth() { return imageWidth; }
        public int getImageHeight() { return imageHeight; }
        public List<TextItemInfo> getTextItems() { return textItems; }
    }
    
    /**
     * 文本项信息类
     */
    private static class TextItemInfo {
        private final String text;
        private final float x; // 左上角x（px）
        private final float y; // 左上角y（px）
        private final float width;
        private final float height;
        private final int imageWidth;
        private final int imageHeight;
        // 字符级信息（可选，来自 chars）
        private final String charText; // 跳过空白后的字符序列
        private final float[] charXs;  // 与 charText 对齐的每个字符左上角x
        private final float[] charYs;  // 与 charText 对齐的每个字符左上角y（top）
        private final float[] charX2s; // 与 charText 对齐的每个字符右上角x（top-right）
        private final float[] charBottomYs; // 与 charText 对齐的每个字符底部y（bottom）
        
        public TextItemInfo(String text, float x, float y, float width, float height,
                             int imageWidth, int imageHeight,
                             String charText, float[] charXs, float[] charYs, float[] charX2s, float[] charBottomYs) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.imageWidth = imageWidth;
            this.imageHeight = imageHeight;
            this.charText = charText;
            this.charXs = charXs;
            this.charYs = charYs;
            this.charX2s = charX2s;
            this.charBottomYs = charBottomYs;
        }
        
        public String getText() { return text; }
        public float getX() { return x; }
        public float getY() { return y; }
        public float getWidth() { return width; }
        public float getHeight() { return height; }
        public int getImageWidth() { return imageWidth; }
        public int getImageHeight() { return imageHeight; }
        public String getCharText() { return charText; }
        public float[] getCharXs() { return charXs; }
        public float[] getCharYs() { return charYs; }
        public float[] getCharX2s() { return charX2s; }
        public float[] getCharBottomYs() { return charBottomYs; }
    }
    
    /**
     * 将比对结果回写到PDF文件
     */
    private void annotatePDFWithResults(String sourcePdfPath, String targetPdfPath, 
                                      List<CompareResult> results, String operationType) {
        try (PDDocument document = PDDocument.load(new File(sourcePdfPath))) {
            
            // 根据操作类型过滤结果
            List<CompareResult> filteredResults = new ArrayList<>();
            for (CompareResult result : results) {
                if (operationType.equals(result.getDiff().operation.toString())) {
                    filteredResults.add(result);
                }
            }
            
            // 为每个差异添加标注
            for (CompareResult result : filteredResults) {
                // 使用OCR结果中的真实位置信息进行标注
                addAnnotationToPDF(document, result, operationType);
            }
            
            // 保存标注后的PDF
            document.save(targetPdfPath);
            log.info("PDF标注完成: {} -> {}", sourcePdfPath, targetPdfPath);
            
        } catch (Exception e) {
            log.error("PDF标注失败: {} -> {}", sourcePdfPath, targetPdfPath, e);
            throw new RuntimeException("PDF标注失败: " + e.getMessage());
        }
    }
    
    /**
     * 向PDF添加标注
     */
    private void addAnnotationToPDF(PDDocument document, CompareResult result, String operationType) {
        try {
            // 按操作选择位置
            Position pos = "DELETE".equals(operationType) ? result.getOldPosition() : result.getNewPosition();
            if (pos == null) return;
            int pageIdx = Math.max(0, pos.getPage()); // Position.page 为0-based
            if (pageIdx >= document.getNumberOfPages()) pageIdx = document.getNumberOfPages() - 1;
            if (pageIdx < 0) return;

            PDPage page = document.getPage(pageIdx);
                
                // 创建文本标注
                PDAnnotationTextMarkup markup = new PDAnnotationTextMarkup(
                    PDAnnotationTextMarkup.SUB_TYPE_HIGHLIGHT);
                
                // 设置颜色
                float[] color;
                if ("DELETE".equals(operationType)) {
                    color = new float[]{1.0f, 0.0f, 0.0f}; // 红色
                } else {
                    color = new float[]{0.0f, 1.0f, 0.0f}; // 绿色
                }
                
                PDColor hl = new PDColor(color, PDDeviceRGB.INSTANCE);
                markup.setColor(hl);
                
                // 根据 Position 构建矩形（将 pos.y 视为“顶部Y”，若有 rectWidth/rectHeight 则精确使用）
                float px = Math.max(0, pos.getX());
                float yTop = Math.max(0, pos.getY()); // 顶部Y（自上而下）
                float pageH = page.getMediaBox().getHeight();
                float rectW = pos.getRectWidth() > 0 ? pos.getRectWidth() : 140f;
                float rectH = pos.getRectHeight() > 0 ? pos.getRectHeight() : 22f;

                // 如果有多段矩形，依次绘制多条标注；否则绘制单条
                java.util.List<float[]> rects = pos.getRects();
                if (rects != null && !rects.isEmpty()) {
                    // 设置内容
                    String content = operationType.equals("DELETE") ? "删除：" : "新增：";
                    content += result.getDiff().text;
                    // 添加到页面
                    List<PDAnnotation> annotations = page.getAnnotations();
                    if (annotations == null) {
                        annotations = new ArrayList<>();
                    }
                    for (float[] r : rects) {
                        float rx = r[0], ryTop = r[1], rw = r[2], rh = r[3];
                        PDRectangle pr = new PDRectangle();
                        pr.setLowerLeftX(rx);
                        pr.setLowerLeftY(Math.max(0, pageH - (ryTop + rh)));
                        pr.setUpperRightX(rx + rw);
                        pr.setUpperRightY(Math.max(0, Math.min(pageH, pageH - ryTop)));
                        PDAnnotationTextMarkup m = new PDAnnotationTextMarkup(PDAnnotationTextMarkup.SUB_TYPE_HIGHLIGHT);
                        m.setColor(hl);
                        m.setRectangle(pr);
                        float[] qu = new float[]{pr.getLowerLeftX(), pr.getUpperRightY(), pr.getUpperRightX(), pr.getUpperRightY(), pr.getLowerLeftX(), pr.getLowerLeftY(), pr.getUpperRightX(), pr.getLowerLeftY()};
                        m.setQuadPoints(qu);
                        m.setSubtype("Highlight");
                        m.setModifiedDate(java.util.Calendar.getInstance().getTime().toString());
                        m.setContents(content);
                        annotations.add(m);
                    }
                    page.setAnnotations(annotations);
                    return;
                } else {
                    PDRectangle position = new PDRectangle();
                    position.setLowerLeftX(px);
                    position.setLowerLeftY(Math.max(0, pageH - (yTop + rectH)));
                    position.setUpperRightX(px + rectW);
                    position.setUpperRightY(Math.max(0, Math.min(pageH, pageH - yTop)));
                    markup.setRectangle(position);
                    // 设置 QuadPoints 与矩形一致
                    float[] quads = new float[8];
                    quads[0] = position.getLowerLeftX();
                    quads[1] = position.getUpperRightY();
                    quads[2] = position.getUpperRightX();
                    quads[3] = position.getUpperRightY();
                    quads[4] = position.getLowerLeftX();
                    quads[5] = position.getLowerLeftY();
                    quads[6] = position.getUpperRightX();
                    quads[7] = position.getLowerLeftY();
                    markup.setQuadPoints(quads);
                }
                markup.setSubtype("Highlight");
                markup.setModifiedDate(java.util.Calendar.getInstance().getTime().toString());
                
                // 设置内容
                String content = operationType.equals("DELETE") ? "删除：" : "新增：";
                content += result.getDiff().text;
                markup.setContents(content);
                
                // 添加到页面
                List<PDAnnotation> annotations = page.getAnnotations();
                if (annotations == null) {
                    annotations = new ArrayList<>();
                }
                annotations.add(markup);
                page.setAnnotations(annotations);
            
        } catch (Exception e) {
            log.warn("添加PDF标注失败", e);
        }
    }
    
    /**
     * 转换比对结果为OCR比对结果格式
     */
    private List<Map<String, Object>> convertCompareResultsToDifferences(List<CompareResult> compareResults) {
        List<Map<String, Object>> differences = new ArrayList<>();
        
        for (CompareResult result : compareResults) {
            Map<String, Object> diff = new HashMap<>();
            diff.put("operation", result.getDiff().operation.toString());
            diff.put("text", result.getDiff().text);
            
            // 从OCR结果中获取真实的位置信息
            Position oldPos = result.getOldPosition();
            Position newPos = result.getNewPosition();
            
            // 输出嵌套的 oldPosition/newPosition，以便前端精准联动
            if (oldPos != null) {
                Map<String, Object> op = new HashMap<>();
                op.put("page", oldPos.getPage());
                op.put("x", oldPos.getX());
                op.put("y", oldPos.getY());
                op.put("pageWidth", oldPos.getPageWidth());
                op.put("pageHeight", oldPos.getPageHeight());
                diff.put("oldPosition", op);
            }
            if (newPos != null) {
                Map<String, Object> np = new HashMap<>();
                np.put("page", newPos.getPage());
                np.put("x", newPos.getX());
                np.put("y", newPos.getY());
                np.put("pageWidth", newPos.getPageWidth());
                np.put("pageHeight", newPos.getPageHeight());
                diff.put("newPosition", np);
            }
            
            differences.add(diff);
        }
        
        return differences;
    }
    
    /**
     * 等待OCR任务完成并监控进度
     */
    private void waitForOCRCompletion(OCRCompareTask task) {
        try {
            boolean oldCompleted = false;
            boolean newCompleted = false;
            
            // 最大等待时间（分钟）
            int maxWaitMinutes = 10;
            long startTime = System.currentTimeMillis();
            long maxWaitTime = maxWaitMinutes * 60 * 1000;
            
            while (!oldCompleted || !newCompleted) {
                // 检查是否超时
                if (System.currentTimeMillis() - startTime > maxWaitTime) {
                    task.setStatus(OCRCompareTask.TaskStatus.FAILED);
                    task.setErrorMessage("OCR任务超时（超过" + maxWaitMinutes + "分钟）");
                    return;
                }
                
                // 检查旧文档OCR进度
                if (!oldCompleted && task.getOldOcrTaskId() != null) {
                    OCRTask oldTask = ocrTaskService.getTaskStatus(task.getOldOcrTaskId());
                    if (oldTask != null) {
                        double oldProgress = oldTask.getProgress();
                        task.updateOCRProgress("old", oldProgress);
                        
                        // 更新总体进度
                        if (oldProgress > 0) {
                            double overallProgress = 20.0 + (oldProgress * 0.4); // 20-60% 范围
                            task.updateProgress(overallProgress, "OCR识别旧文档中...");
                        }
                        
                        if (oldTask.isCompleted()) {
                            oldCompleted = true;
                            if (oldTask.getStatus() != OCRTask.TaskStatus.COMPLETED) {
                                task.setStatus(OCRCompareTask.TaskStatus.FAILED);
                                task.setErrorMessage("旧文档OCR识别失败: " + oldTask.getErrorMessage());
                                return;
                            }
                        }
                    }
                }
                
                // 检查新文档OCR进度
                if (!newCompleted && task.getNewOcrTaskId() != null) {
                    OCRTask newTask = ocrTaskService.getTaskStatus(task.getNewOcrTaskId());
                    if (newTask != null) {
                        double newProgress = newTask.getProgress();
                        task.updateOCRProgress("new", newProgress);
                        
                        // 更新总体进度
                        if (newProgress > 0) {
                            double overallProgress = 40.0 + (newProgress * 0.4); // 40-80% 范围
                            task.updateProgress(overallProgress, "OCR识别新文档中...");
                        }
                        
                        if (newTask.isCompleted()) {
                            newCompleted = true;
                            if (newTask.getStatus() != OCRTask.TaskStatus.COMPLETED) {
                                task.setStatus(OCRCompareTask.TaskStatus.FAILED);
                                task.setErrorMessage("新文档OCR识别失败: " + newTask.getErrorMessage());
                                return;
                            }
                        }
                    }
                }
                
                // 等待一段时间再检查
                Thread.sleep(2000);
            }
            
            // 两个OCR任务都完成了
            task.updateProgress(60.0, "OCR识别完成，准备执行比对...");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            task.setStatus(OCRCompareTask.TaskStatus.FAILED);
            task.setErrorMessage("OCR任务被中断");
        }
    }
    
    /**
     * 获取OCR识别结果文本
     */
    private String getOCRResultText(String ocrTaskId) {
        if (ocrTaskId == null) {
            return null;
        }
        
        OCRTask ocrTask = ocrTaskService.getTaskStatus(ocrTaskId);
        if (ocrTask == null || !ocrTask.isCompleted() || ocrTask.getStatus() != OCRTask.TaskStatus.COMPLETED) {
            return null;
        }
        
        try {
            // 首先检查OCRTask中是否已经保存了文本内容
            String savedTextContent = ocrTask.getTextContent();
            if (savedTextContent != null && !savedTextContent.trim().isEmpty()) {
                log.info("使用已保存的OCR结果，任务ID: {}, 文本长度: {} 字符", ocrTaskId, savedTextContent.length());
                return savedTextContent;
            }
            
            // 如果没有保存的文本内容，通过HTTP接口获取OCR结果
            log.info("OCRTask中未保存文本内容，通过HTTP接口获取，任务ID: {}", ocrTaskId);
            com.fasterxml.jackson.databind.JsonNode result = ocrTaskService.getOCRResultViaHttp(ocrTaskId);
            if (result != null) {
                String textContent = result.path("text_content").asText("");
                if (textContent != null && !textContent.trim().isEmpty()) {
                    log.info("成功通过HTTP接口获取OCR结果，任务ID: {}, 文本长度: {} 字符", ocrTaskId, textContent.length());
                    // 将获取到的文本内容保存到OCRTask中，避免下次重复调用
                    ocrTask.setTextContent(textContent);
                    return textContent;
                } else {
                    log.warn("HTTP接口返回的OCR结果文本内容为空，任务ID: {}", ocrTaskId);
                    return null;
                }
            } else {
                log.warn("无法通过HTTP接口获取OCR结果，任务ID: {}", ocrTaskId);
                return null;
            }
            
        } catch (Exception e) {
            log.error("获取OCR结果失败，任务ID: {}", ocrTaskId, e);
            return null;
        }
    }
    
    /**
     * 生成任务ID
     */
    private String generateTaskId() {
        return "OCR_" + System.currentTimeMillis() + "_" + 
               UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * 创建必要的目录
     */
    private void createDirectories() {
        // 创建上传目录
        File uploadDir = new File(uploadRootPath, "ocr_uploads");
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
    }
    
    /**
     * 保存上传的文件
     */
    private String saveFile(MultipartFile file, String fileName) throws IOException {
        File uploadDir = new File(uploadRootPath, "ocr_uploads");
        File targetFile = new File(uploadDir, fileName);
        file.transferTo(targetFile);
        return targetFile.getAbsolutePath();
    }
    
    /**
     * 调试接口：使用已有的OCR结果进行比对
     * 跳过上传和OCR识别过程，直接使用已有的OCR任务ID进行比对
     */
    public String debugCompareWithExistingOCR(String oldOcrTaskId, String newOcrTaskId, OCRCompareOptions options) {
        try {
            // 验证OCR任务是否存在且已完成
            OCRTask oldTask = ocrTaskService.getTaskStatus(oldOcrTaskId);
            OCRTask newTask = ocrTaskService.getTaskStatus(newOcrTaskId);
            
            if (oldTask == null || newTask == null) {
                throw new IllegalArgumentException("OCR任务不存在");
            }
            
            if (!oldTask.isCompleted() || !newTask.isCompleted() || 
                oldTask.getStatus() != OCRTask.TaskStatus.COMPLETED || 
                newTask.getStatus() != OCRTask.TaskStatus.COMPLETED) {
                throw new IllegalArgumentException("OCR任务未完成");
            }
            
            // 获取PDF文件路径
            String oldFilePath = oldTask.getPdfPath();
            String newFilePath = newTask.getPdfPath();
            
            if (oldFilePath == null || newFilePath == null) {
                throw new IllegalArgumentException("OCR任务文件路径为空");
            }
            
            // 生成任务ID
            String taskId = generateTaskId();
            
            // 创建比对任务
            OCRCompareTask task = new OCRCompareTask(
                taskId,
                new File(oldFilePath).getName(),
                new File(newFilePath).getName(),
                oldFilePath,
                newFilePath,
                options
            );
            
            // 设置OCR任务ID
            task.setOldOcrTaskId(oldOcrTaskId);
            task.setNewOcrTaskId(newOcrTaskId);
            
            // 设置任务状态为OCR已完成
            task.setStatus(OCRCompareTask.TaskStatus.OCR_PROCESSING);
            task.setStartTime(LocalDateTime.now());
            task.setCurrentStep(3, "OCR识别已完成");
            task.updateProgress(60.0, "OCR识别已完成，准备执行比对...");
            task.updateOCRProgress("old", 100.0);
            task.updateOCRProgress("new", 100.0);
            
            tasks.put(taskId, task);
            
            // 异步执行比对任务（从比对步骤开始）
            CompletableFuture.runAsync(() -> executeDebugCompareTask(task));
            
            return taskId;
        } catch (Exception e) {
            log.error("调试比对任务创建失败", e);
            throw new RuntimeException("调试比对任务创建失败: " + e.getMessage());
        }
    }
    
    /**
     * 执行调试比对任务（跳过OCR识别步骤）
     */
    private void executeDebugCompareTask(OCRCompareTask task) {
        try {
            // 步骤4: 执行文本比对
            task.setCurrentStep(4, "执行文本比对");
            task.updateProgress(70.0, "执行文本比对...");
            
            // 获取OCR结果
            String oldText = getOCRResultText(task.getOldOcrTaskId());
            String newText = getOCRResultText(task.getNewOcrTaskId());
            
            if (oldText == null || newText == null) {
                task.setStatus(OCRCompareTask.TaskStatus.FAILED);
                task.setErrorMessage("OCR识别结果为空");
                return;
            }
            
            // 执行文本比对并解析OCR结果中的位置信息（结合PDF页尺寸进行坐标换算）
            List<CompareResult> compareResults = enrichCompareResultsWithPositionInfo(
                oldText,
                newText,
                task.getOptions(),
                task.getOldOcrTaskId(),
                task.getNewOcrTaskId(),
                task.getOldFilePath(),
                task.getNewFilePath()
            );
            
            // 步骤5: 将比对结果回写到PDF文件
            task.setCurrentStep(5, "回写比对结果到PDF");
            task.updateProgress(85.0, "回写比对结果到PDF...");
            
            String resultDir = uploadRootPath + "/ocr-compare/results/" + task.getTaskId();
            java.nio.file.Path resultDirPath = java.nio.file.Paths.get(resultDir);
            java.nio.file.Files.createDirectories(resultDirPath);
            
            String annotatedOldPdfPath = resultDir + "/old_annotated.pdf";
            String annotatedNewPdfPath = resultDir + "/new_annotated.pdf";
            
            // 回写比对结果到PDF
            annotatePDFWithResults(task.getOldFilePath(), annotatedOldPdfPath, compareResults, "DELETE");
            annotatePDFWithResults(task.getNewFilePath(), annotatedNewPdfPath, compareResults, "INSERT");
            
            // 创建OCR比对结果
            OCRCompareResult result = new OCRCompareResult();
            result.setTaskId(task.getTaskId());
            result.setOldPdfUrl("/api/ocr-compare/files/" + task.getTaskId() + "/old_annotated.pdf");
            result.setNewPdfUrl("/api/ocr-compare/files/" + task.getTaskId() + "/new_annotated.pdf");
            
            // 转换比对结果为OCR比对结果格式
            List<Map<String, Object>> differences = convertCompareResultsToDifferences(compareResults);
            result.setDifferences(differences);
            
            // 计算相似度（简化计算，基于差异数量）
            double similarity = 1.0;
            if (differences != null && !differences.isEmpty()) {
                // 这里可以根据实际需求调整相似度计算逻辑
                similarity = Math.max(0.0, 1.0 - (differences.size() * 0.1));
            }
            result.setSimilarity(similarity);
            
            // 生成比对摘要
            result.generateSummary();
            
            // 设置前端需要的summary字段
            Map<String, Object> summary = new HashMap<>();
            summary.put("totalDifferences", differences != null ? differences.size() : 0);
            
            // 统计删除和新增的数量
            int deletions = 0, insertions = 0;
            if (differences != null) {
                for (Map<String, Object> diff : differences) {
                    String operation = (String) diff.get("operation");
                    if ("DELETE".equals(operation)) {
                        deletions++;
                    } else if ("INSERT".equals(operation)) {
                        insertions++;
                    }
                }
            }
            summary.put("deletions", deletions);
            summary.put("insertions", insertions);
            
            result.setSummary(summary);
            
            // 步骤6: 保存比对结果
            task.setCurrentStep(6, "保存比对结果");
            task.updateProgress(95.0, "保存比对结果...");
            
            results.put(task.getTaskId(), result);
            log.info("调试模式OCR比对结果生成完成：taskId={} diffs={} oldPdf={} newPdf={}", task.getTaskId(),
                differences == null ? 0 : differences.size(), result.getOldPdfUrl(), result.getNewPdfUrl());
            task.setStatus(OCRCompareTask.TaskStatus.COMPLETED);
            task.setCompletedTime(LocalDateTime.now());
            task.updateProgress(100.0, "比对完成");
            
            log.info("调试模式OCR比对任务完成: {}", task.getTaskId());
            
        } catch (Exception e) {
            task.setStatus(OCRCompareTask.TaskStatus.FAILED);
            task.setErrorMessage("调试模式OCR比对执行异常: " + e.getMessage());
            log.error("调试模式OCR比对任务执行失败: {}", task.getTaskId(), e);
        }
    }
    
    /**
     * 清理任务相关文件
     */
    private void cleanupTaskFiles(OCRCompareTask task) {
        try {
            // 清理旧文件
            if (task.getOldFilePath() != null) {
                File oldFile = new File(task.getOldFilePath());
                if (oldFile.exists()) {
                    oldFile.delete();
                }
            }
            
            // 清理新文件
            if (task.getNewFilePath() != null) {
                File newFile = new File(task.getNewFilePath());
                if (newFile.exists()) {
                    newFile.delete();
                }
            }
        } catch (Exception e) {
            log.warn("清理任务文件失败", e);
        }
    }
}
