package com.zhaoxinms.contract.tools.comparePRO.service;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;

// PDF处理相关导入
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.json.JsonReadFeature;
// JSON处理相关导入
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhaoxinms.contract.tools.compare.DiffUtil;
import com.zhaoxinms.contract.tools.compare.util.TextNormalizer;
import com.zhaoxinms.contract.tools.comparePRO.client.DotsOcrClient;
import com.zhaoxinms.contract.tools.comparePRO.config.ZxOcrConfig;
import com.zhaoxinms.contract.tools.comparePRO.model.CharBox;
import com.zhaoxinms.contract.tools.comparePRO.model.CompareOptions;
import com.zhaoxinms.contract.tools.comparePRO.model.CompareResult;
import com.zhaoxinms.contract.tools.comparePRO.model.CompareTask;
import com.zhaoxinms.contract.tools.comparePRO.model.DiffBlock;
import com.zhaoxinms.contract.tools.comparePRO.model.ExportRequest;
import com.zhaoxinms.contract.tools.comparePRO.util.CompareTaskProgressManager;
import com.zhaoxinms.contract.tools.comparePRO.util.CompareTaskProgressManager.TaskStep;
import com.zhaoxinms.contract.tools.comparePRO.util.CompareTaskQueue;
import com.zhaoxinms.contract.tools.comparePRO.util.DiffBlockValidationUtil;
import com.zhaoxinms.contract.tools.comparePRO.util.DiffProcessingUtil;
import com.zhaoxinms.contract.tools.comparePRO.util.OcrImageSaver;
import com.zhaoxinms.contract.tools.comparePRO.util.TextExtractionUtil;
import com.zhaoxinms.contract.tools.comparePRO.util.WatermarkRemover;
import com.zhaoxinms.contract.tools.config.ZxcmConfig;

/**
 * GPU OCR比对服务 - 基于DotsOcrCompareDemoTest的完整比对功能
 */
@Service
public class CompareService {
    
    private static final Logger logger = LoggerFactory.getLogger(CompareService.class);

	// 内部类：包装OCR识别结果和错误信息
	private static class RecognitionResult {
		public final List<CharBox> charBoxes;
		public final List<String> failedPages;
		public final int totalPages;

		public RecognitionResult(List<CharBox> charBoxes, List<String> failedPages, int totalPages) {
			this.charBoxes = charBoxes;
			this.failedPages = failedPages;
			this.totalPages = totalPages;
		}
		
	}

    @Autowired
    private ZxOcrConfig gpuOcrConfig;
    
    @Autowired
    private ZxcmConfig zxcmConfig;

    @Autowired
    private OcrImageSaver ocrImageSaver;

    @Autowired
    private CompareTaskQueue taskQueue;
    
    @Autowired
    private DiffBlockValidationUtil diffBlockValidationUtil;

    @Autowired
    private WatermarkRemover watermarkRemover;

    @Autowired(required = false)
    private ThirdPartyOcrService thirdPartyOcrService;

    private final ConcurrentHashMap<String, CompareTask> tasks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CompareResult> results = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Map<String, Object>> frontendResults = new ConcurrentHashMap<>();
    private static final ObjectMapper M = new ObjectMapper()
            .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature())
            .enable(JsonReadFeature.ALLOW_MISSING_VALUES.mappedFeature());

    @PostConstruct
    public void init() {
		// 调整任务队列的最大线程数
		taskQueue.adjustMaxPoolSize(gpuOcrConfig.getParallelThreads());
		System.out.println("GPU OCR比对服务初始化完成，最大并发线程数: " + gpuOcrConfig.getParallelThreads());
        
        // 启动时加载已完成的任务到内存中
        loadCompletedTasks();

		// 输出当前队列状态
		System.out.println("当前任务队列状态:");
		System.out.println(taskQueue.getStats());
    }
    
    /**
     * 加载已完成的任务到内存中
     */
    private void loadCompletedTasks() {
        try {
            String uploadRootPath = zxcmConfig.getFileUpload().getRootPath();
            Path resultsDir = Paths.get(uploadRootPath, "compare-pro", "results");
            
            if (Files.exists(resultsDir)) {
				Files.list(resultsDir).filter(path -> path.toString().endsWith(".json")).forEach(jsonFile -> {
                        try {
                            String fileName = jsonFile.getFileName().toString();
                            String taskId = fileName.substring(0, fileName.lastIndexOf(".json"));
                            
                            // 加载任务状态到内存
                            CompareTask task = loadTaskFromFile(taskId);
                            if (task != null) {
                                tasks.put(taskId, task);
                            }
                        } catch (Exception e) {
                            System.err.println("加载任务失败: " + jsonFile + ", error=" + e.getMessage());
                        }
                    });
            }
            
            // 也检查前端结果目录
            Path frontendResultsDir = Paths.get(uploadRootPath, "compare-pro", "frontend-results");
            if (Files.exists(frontendResultsDir)) {
				Files.list(frontendResultsDir).filter(path -> path.toString().endsWith(".json")).forEach(jsonFile -> {
                        try {
                            String fileName = jsonFile.getFileName().toString();
                            String taskId = fileName.substring(0, fileName.lastIndexOf(".json"));
                            
                            // 如果内存中还没有这个任务，加载它
                            if (!tasks.containsKey(taskId)) {
                                CompareTask task = loadTaskFromFile(taskId);
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
    public String submitCompareTask(MultipartFile oldFile, MultipartFile newFile, CompareOptions options) {
        String taskId = UUID.randomUUID().toString();

        CompareTask task = new CompareTask(taskId);
        task.setOldFileName(oldFile.getOriginalFilename());
        task.setNewFileName(newFile.getOriginalFilename());
        task.setStatus(CompareTask.Status.PENDING);

        tasks.put(taskId, task);

        try {
            // 同步保存文件到系统上传目录，避免异步处理时文件流被关闭
            String uploadRootPath = zxcmConfig.getFileUpload().getRootPath();
            Path uploadDir = Paths.get(uploadRootPath, "compare-pro", "tasks", taskId);
            Files.createDirectories(uploadDir);

            Path oldFilePath = uploadDir.resolve("old_" + oldFile.getOriginalFilename());
            Path newFilePath = uploadDir.resolve("new_" + newFile.getOriginalFilename());

            // 同步保存文件，确保文件流被正确关闭
			try (var oldInputStream = oldFile.getInputStream(); var newInputStream = newFile.getInputStream()) {
                Files.copy(oldInputStream, oldFilePath);
                Files.copy(newInputStream, newFilePath);
            }

            logger.info("文件已保存到系统上传目录:");
            logger.info("  原文档: {}", oldFilePath.toAbsolutePath());
            logger.info("  新文档: {}", newFilePath.toAbsolutePath());

			// 使用新的任务队列执行比对任务
			boolean submitted = taskQueue.submitTask(
					() -> executeCompareTaskWithPaths(task, oldFilePath.toString(), newFilePath.toString(), options),
					taskId);

			if (!submitted) {
				task.setStatus(CompareTask.Status.FAILED);
				task.setErrorMessage("任务队列已满，无法提交任务");
				System.err.println("任务队列已满，任务 " + taskId + " 提交失败");
			}

        } catch (Exception e) {
            task.setStatus(CompareTask.Status.FAILED);
            task.setErrorMessage("文件保存失败: " + e.getMessage());
            System.err.println("文件保存失败: " + e.getMessage());
            e.printStackTrace();
        }

        return taskId;
    }

    /**
     * 提交比对任务（文件路径）
     */
    public String submitCompareTaskWithPaths(String oldFilePath, String newFilePath, CompareOptions options) {
        String taskId = UUID.randomUUID().toString();

        CompareTask task = new CompareTask(taskId);
        task.setOldFileName(Paths.get(oldFilePath).getFileName().toString());
        task.setNewFileName(Paths.get(newFilePath).getFileName().toString());
        task.setStatus(CompareTask.Status.PENDING);

        tasks.put(taskId, task);

		// 使用新的任务队列执行比对任务
		boolean submitted = taskQueue
				.submitTask(() -> executeCompareTaskWithPaths(task, oldFilePath, newFilePath, options), taskId);

		if (!submitted) {
			task.setStatus(CompareTask.Status.FAILED);
			task.setErrorMessage("任务队列已满，无法提交任务");
			System.err.println("任务队列已满，任务 " + taskId + " 提交失败");
		}

        return taskId;
    }

    /**
     * 调试模式：使用已有任务结果进行重新分析
     */
    public String debugCompareWithTaskId(String taskId, CompareOptions options) {
		// 重置调试计数器
		DiffProcessingUtil.resetDebugCounter();
		
		// Debug模式直接使用原任务ID，不创建新ID
		CompareTask existingTask = getTaskStatus(taskId);
		if (existingTask == null) {
			// 如果原任务不存在，创建一个基本的任务对象用于debug处理
			existingTask = new CompareTask(taskId);
			existingTask.setOldFileName("debug_old.pdf");
			existingTask.setNewFileName("debug_new.pdf");
			tasks.put(taskId, existingTask);
		}

		// 重置任务状态为调试模式
		existingTask.setStatus(CompareTask.Status.PENDING);
		existingTask.setErrorMessage(null);

		// 为lambda使用创建最终引用，确保effectively final
		final CompareTask taskToRun = existingTask;

		// 使用新的任务队列执行调试比对任务，使用原始任务ID
		boolean submitted = taskQueue
				.submitTask(() -> executeDebugCompareTaskWithExistingResult(taskToRun, taskId, options), taskId);

		if (!submitted) {
			existingTask.setStatus(CompareTask.Status.FAILED);
			existingTask.setErrorMessage("任务队列已满，无法提交调试任务");
			System.err.println("任务队列已满，调试任务 " + taskId + " 提交失败");
		}

		return taskId; // 返回原始任务ID
    }

    /**
     * 获取任务状态
     */
    public CompareTask getTaskStatus(String taskId) {
        // 首先从内存中获取
        CompareTask task = tasks.get(taskId);
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
    private CompareTask loadTaskFromFile(String taskId) {
        try {
            // 检查任务目录是否存在
            String uploadRootPath = zxcmConfig.getFileUpload().getRootPath();
            Path taskDir = Paths.get(uploadRootPath, "compare-pro", "tasks", taskId);
            if (!Files.exists(taskDir)) {
                return null;
            }
            
            // 检查是否有result.json文件（表示任务已完成）
            Path resultJsonPath = Paths.get(uploadRootPath, "compare-pro", "results", taskId + ".json");
            if (Files.exists(resultJsonPath)) {
                // 从result.json中提取任务信息
                byte[] bytes = Files.readAllBytes(resultJsonPath);
                @SuppressWarnings("unchecked")
                Map<String, Object> resultData = M.readValue(bytes, Map.class);
                
                CompareTask task = new CompareTask(taskId);
                task.setOldFileName((String) resultData.get("oldFileName"));
                task.setNewFileName((String) resultData.get("newFileName"));
                task.setStatus(CompareTask.Status.COMPLETED);
                
                // 从result.json中恢复时间信息
                try {
                    // 读取开始时间
                    String startTimeStr = (String) resultData.get("startTime");
                    if (startTimeStr != null) {
                        task.setStartTime(java.time.LocalDateTime.parse(startTimeStr));
                        logger.debug("从文件恢复开始时间: {}", startTimeStr);
                    }
                    
                    // 读取结束时间（如果存在）
                    String endTimeStr = (String) resultData.get("endTime");
                    if (endTimeStr != null) {
                        task.setEndTime(java.time.LocalDateTime.parse(endTimeStr));
                        logger.debug("从文件恢复结束时间: {}", endTimeStr);
                    }
                    
                    // 读取总耗时（如果存在）
                    Object totalDurationObj = resultData.get("totalDuration");
                    if (totalDurationObj != null) {
                        Long totalDuration = null;
                        if (totalDurationObj instanceof Number) {
                            totalDuration = ((Number) totalDurationObj).longValue();
                        }
                        if (totalDuration != null) {
                            task.setTotalDuration(totalDuration);
                            logger.debug("从文件恢复总耗时: {}ms", totalDuration);
                        }
                    }
                    
                    logger.info("✅ 从result.json恢复任务时间信息: {}", taskId);
                    
                } catch (Exception e) {
                    logger.warn("恢复任务时间信息时出错，使用默认值: {}", e.getMessage());
                }
                
                return task;
            }
            
            // 检查是否有前端结果文件
            Path frontendResultPath = getFrontendResultJsonPath(taskId);
            if (Files.exists(frontendResultPath)) {
                byte[] bytes = Files.readAllBytes(frontendResultPath);
                @SuppressWarnings("unchecked")
                Map<String, Object> frontendData = M.readValue(bytes, Map.class);
                
                CompareTask task = new CompareTask(taskId);
                task.setOldFileName((String) frontendData.get("oldFileName"));
                task.setNewFileName((String) frontendData.get("newFileName"));
                task.setStatus(CompareTask.Status.COMPLETED);
                // 不再需要设置PDF URL，全部使用画布显示
                
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
    public CompareResult getCompareResult(String taskId) {
        CompareTask task = getTaskStatus(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }

        if (!task.isCompleted()) {
            throw new RuntimeException("任务未完成");
        }

        // 首先尝试从结果存储中获取完整结果
        CompareResult result = results.get(taskId);
        if (result != null) {
            return result;
        }

        // 如果没有找到完整结果，尝试从文件中加载并转换为CompareResult
        try {
            Map<String, Object> rawData = getRawFrontendResult(taskId);
            if (rawData != null) {
                logger.info("🔍 从文件加载原始比对结果，转换为CompareResult对象");
                result = convertRawDataToCompareResult(rawData, taskId);
                logger.info("✅ 成功转换，差异数量: {}", 
                    result.getDifferences() != null ? result.getDifferences().size() : 0);
                
                // 将结果放入缓存以便后续使用
                results.put(taskId, result);
                return result;
            }
        } catch (Exception e) {
            logger.error("从文件加载并转换比对结果失败: {}", e.getMessage());
        }

        // 如果文件也不存在，构造一个基本的返回结果
        logger.warn("⚠️ 未找到比对结果文件，创建空的结果对象");
        result = new CompareResult(taskId);
        result.setOldFileName(task.getOldFileName());
        result.setNewFileName(task.getNewFileName());
        // 不再需要设置PDF URL，全部使用画布显示

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
	 * 获取Canvas版本的前端比对结果（包含图片列表和原始坐标）
	 */
	public Map<String, Object> getCanvasFrontendResult(String taskId) {
		Map<String, Object> originalResult = getRawFrontendResult(taskId);
		if (originalResult == null) {
            return null;
        }

		// 获取任务信息
		CompareTask task = getTaskStatus(taskId);
		if (task == null) {
			return originalResult;
		}

		// 创建Canvas版本的结果
		Map<String, Object> canvasResult = new HashMap<>(originalResult);
		
		// 添加时间统计信息
		if (task.getStepDurations() != null && !task.getStepDurations().isEmpty()) {
			canvasResult.put("stepDurations", task.getStepDurations());
		}
		if (task.getTotalDuration() != null) {
			canvasResult.put("totalDuration", task.getTotalDuration());
		}
		if (task.getStartTime() != null) {
			canvasResult.put("startTime", task.getStartTime().toString());
		}
		if (task.getEndTime() != null) {
			canvasResult.put("endTime", task.getEndTime().toString());
		}
		
		// 添加失败页面信息
		if (task.getFailedPages() != null && !task.getFailedPages().isEmpty()) {
			canvasResult.put("failedPages", task.getFailedPages());
			canvasResult.put("failedPagesCount", task.getFailedPages().size());
		} else {
			canvasResult.put("failedPages", new ArrayList<>());
			canvasResult.put("failedPagesCount", 0);
		}
		
		// 添加统计信息
		if (task.getStatistics() != null && !task.getStatistics().isEmpty()) {
			canvasResult.put("statistics", task.getStatistics());
		}

		try {
			// 获取图片信息
			DocumentImageInfo oldImageInfo = getDocumentImageInfo(taskId, "old");
			DocumentImageInfo newImageInfo = getDocumentImageInfo(taskId, "new");

			// 添加图片信息
			canvasResult.put("oldImageInfo", oldImageInfo);
			canvasResult.put("newImageInfo", newImageInfo);

			// 更新文件URL为图片列表
			String baseUploadPath = "/api/compare-pro/files";
			canvasResult.put("oldImageBaseUrl", baseUploadPath + "/tasks/" + taskId + "/images/old");
			canvasResult.put("newImageBaseUrl", baseUploadPath + "/tasks/" + taskId + "/images/new");

			// 不再需要PDF URL，全部使用画布显示

			//System.out.println("Canvas前端结果创建成功，包含图片信息");

		} catch (Exception e) {
			System.err.println("获取Canvas前端结果失败: " + e.getMessage());
			// 出错时返回原始结果
			return originalResult;
		}

		return canvasResult;
	}

	/**
	 * 保存用户修改（直接修改后端存储的数据）
	 */
	public void saveUserModifications(String taskId, com.zhaoxinms.contract.tools.comparePRO.controller.GPUCompareController.UserModificationsRequest modifications) {
		System.out.println("💾 直接修改后端数据 - 任务 " + taskId + ": 忽略" + 
			(modifications.getIgnoredDifferences() != null ? modifications.getIgnoredDifferences().size() : 0) + 
			"项, 备注" + 
			(modifications.getRemarks() != null ? modifications.getRemarks().size() : 0) + "项");
		
		// 1. 从 frontendResults 获取原始数据
		Map<String, Object> frontendResult = frontendResults.get(taskId);
		if (frontendResult == null) {
			// 尝试从文件读取
			frontendResult = getRawFrontendResult(taskId);
			if (frontendResult == null) {
				throw new RuntimeException("任务 " + taskId + " 的前端结果不存在");
			}
		}
		
		// 2. 获取差异列表
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> differences = (List<Map<String, Object>>) frontendResult.get("differences");
		if (differences == null || differences.isEmpty()) {
			System.out.println("⚠️ 任务 " + taskId + " 没有差异项，无需修改");
			return;
		}
		
		int originalCount = differences.size();
		List<Integer> ignoredIndices = modifications.getIgnoredDifferences();
		Map<Integer, String> remarks = modifications.getRemarks();
		
		// 3. 修改 differences 列表（标记忽略项，不删除）
		for (int i = 0; i < differences.size(); i++) {
			Map<String, Object> diff = differences.get(i);
			
			// 检查是否被忽略 - 标记而不是删除
			if (ignoredIndices != null && ignoredIndices.contains(i)) {
				diff.put("ignored", true);
				System.out.println("  ⊗ 标记差异项 " + i + " 为已忽略");
			} else {
				// 移除忽略标记（如果之前被忽略，现在取消忽略）
				diff.remove("ignored");
			}
			
			// 添加或移除备注
			if (remarks != null && remarks.containsKey(i)) {
				String remark = remarks.get(i);
				diff.put("remark", remark);
				System.out.println("  📝 为差异项 " + i + " 添加备注: " + remark);
			} else {
				// 移除备注（如果之前有备注，现在删除）
				diff.remove("remark");
			}
		}
		
		// 4. 重新计算统计信息（只统计未忽略的项）
		int totalCount = 0;
		int deleteCount = 0;
		int insertCount = 0;
		int ignoredCount = 0;
		
		for (Map<String, Object> diff : differences) {
			Boolean isIgnored = (Boolean) diff.get("ignored");
			if (isIgnored != null && isIgnored) {
				ignoredCount++;
				continue; // 跳过忽略项的统计
			}
			
			totalCount++;
			String operation = (String) diff.get("operation");
			if ("DELETE".equals(operation)) {
				deleteCount++;
			} else if ("INSERT".equals(operation)) {
				insertCount++;
			}
		}
		
		frontendResult.put("totalDiffCount", totalCount);
		frontendResult.put("deleteCount", deleteCount);
		frontendResult.put("insertCount", insertCount);
		frontendResult.put("ignoredCount", ignoredCount);
		
		System.out.println("✅ 修改已保存: 总" + originalCount + "项, 有效" + totalCount + "项, 已忽略" + ignoredCount + "项");
		
		// 6. 保存修改后的数据回 frontendResults 缓存
		frontendResults.put(taskId, frontendResult);
		
		// 7. 保存修改后的数据到文件
		try {
			Path jsonPath = getFrontendResultJsonPath(taskId);
			Files.createDirectories(jsonPath.getParent());
			byte[] json = M.writerWithDefaultPrettyPrinter().writeValueAsBytes(frontendResult);
			Files.write(jsonPath, json);
			System.out.println("💾 数据已持久化到文件: " + jsonPath.toAbsolutePath());
		} catch (Exception e) {
			System.err.println("❌ 持久化失败: " + e.getMessage());
			throw new RuntimeException("保存用户修改到文件失败: " + e.getMessage(), e);
		}
	}

	/**
	 * 获取用户修改（从文件重新读取数据即可，因为已经被直接修改过）
	 */
	public com.zhaoxinms.contract.tools.comparePRO.controller.GPUCompareController.UserModificationsRequest getUserModifications(String taskId) {
		// 数据已经被直接修改，返回空对象即可
		com.zhaoxinms.contract.tools.comparePRO.controller.GPUCompareController.UserModificationsRequest request = 
			new com.zhaoxinms.contract.tools.comparePRO.controller.GPUCompareController.UserModificationsRequest();
		request.setIgnoredDifferences(new ArrayList<>());
		request.setRemarks(new HashMap<>());
		return request;
	}

	/**
	 * 文档图片信息类
	 */
	public static class DocumentImageInfo {
		private int totalPages;
		private List<PageImageInfo> pages;

		public DocumentImageInfo(int totalPages) {
			this.totalPages = totalPages;
			this.pages = new ArrayList<>();
		}

		public int getTotalPages() {
			return totalPages;
		}

		public void setTotalPages(int totalPages) {
			this.totalPages = totalPages;
		}

		public List<PageImageInfo> getPages() {
			return pages;
		}

		public void setPages(List<PageImageInfo> pages) {
			this.pages = pages;
		}

		public void addPage(PageImageInfo page) {
			this.pages.add(page);
		}
	}

	/**
	 * 页面图片信息类
	 */
	public static class PageImageInfo {
		private int pageNumber;
		private String imageUrl;
		private int width;
		private int height;

		public PageImageInfo(int pageNumber, String imageUrl, int width, int height) {
			this.pageNumber = pageNumber;
			this.imageUrl = imageUrl;
			this.width = width;
			this.height = height;
		}

		public int getPageNumber() {
			return pageNumber;
		}

		public void setPageNumber(int pageNumber) {
			this.pageNumber = pageNumber;
		}

		public String getImageUrl() {
			return imageUrl;
		}

		public void setImageUrl(String imageUrl) {
			this.imageUrl = imageUrl;
		}

		public int getWidth() {
			return width;
		}

		public void setWidth(int width) {
			this.width = width;
		}

		public int getHeight() {
			return height;
		}

		public void setHeight(int height) {
			this.height = height;
		}
	}

	/**
	 * 获取文档图片信息
	 */
	public DocumentImageInfo getDocumentImageInfo(String taskId, String mode) throws Exception {
		String uploadRootPath = zxcmConfig.getFileUpload().getRootPath();
		Path imagesDir = Paths.get(uploadRootPath, "compare-pro", "tasks", taskId, "images", mode);

		if (!Files.exists(imagesDir)) {
			// 列出父目录内容，帮助调试
			Path parentDir = imagesDir.getParent();
			if (Files.exists(parentDir)) {
				logger.debug("父目录存在，内容如下:");
				try (var stream = Files.list(parentDir)) {
					stream.forEach(path -> logger.debug("  - {}", path.getFileName()));
				}
                } else {
				logger.debug("父目录也不存在: {}", parentDir);
			}
			throw new RuntimeException("图片目录不存在: " + imagesDir);
		}

		// 获取所有页面图片
		List<Path> imageFiles = new ArrayList<>();
		try (var stream = Files.list(imagesDir)) {
			stream.filter(path -> path.toString().toLowerCase().endsWith(".png"))
					.filter(path -> path.getFileName().toString().startsWith("page-")).sorted((a, b) -> {
						// 按页码排序
						String aName = a.getFileName().toString();
						String bName = b.getFileName().toString();
						int aPage = extractPageNumber(aName);
						int bPage = extractPageNumber(bName);
						return Integer.compare(aPage, bPage);
					}).forEach(imageFiles::add);
		}

		DocumentImageInfo docInfo = new DocumentImageInfo(imageFiles.size());

		String baseUploadPath = "/api/compare-pro/files";
		String baseUrl = baseUploadPath + "/compare-pro/tasks/" + taskId + "/images/" + mode;

		for (Path imagePath : imageFiles) {
			String fileName = imagePath.getFileName().toString();
			int pageNumber = extractPageNumber(fileName);

			try {
				// 读取图片尺寸
				BufferedImage image = ImageIO.read(imagePath.toFile());
				int width = image.getWidth();
				int height = image.getHeight();

				String imageUrl = baseUrl + "/" + fileName;
				PageImageInfo pageInfo = new PageImageInfo(pageNumber, imageUrl, width, height);
				docInfo.addPage(pageInfo);

			} catch (Exception e) {
				System.err.println("读取图片尺寸失败: " + imagePath + ", error=" + e.getMessage());
				// 使用默认尺寸
				String imageUrl = baseUrl + "/" + fileName;
				PageImageInfo pageInfo = new PageImageInfo(pageNumber, imageUrl, 1000, 1400);
				docInfo.addPage(pageInfo);
			}
		}

		return docInfo;
	}

	/**
	 * 从文件名中提取页码
	 */
	private int extractPageNumber(String fileName) {
		try {
			// 文件名格式: page-1.png, page-2.png, etc.
			String numberPart = fileName.substring(5, fileName.lastIndexOf('.'));
			return Integer.parseInt(numberPart);
		} catch (Exception e) {
			return 1; // 默认页码
		}
    }

    /**
     * 获取所有任务
     */
    public List<CompareTask> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }

	/**
	 * 获取任务队列状态信息
	 */
	public CompareTaskQueue.TaskQueueStats getQueueStats() {
		return taskQueue.getStats();
	}

	/**
	 * 检查队列是否繁忙
	 */
	public boolean isQueueBusy() {
		return taskQueue.isBusy();
	}

	/**
	 * 动态调整最大并发线程数
	 */
	public void adjustMaxConcurrency(int maxThreads) {
		taskQueue.adjustMaxPoolSize(maxThreads);
		System.out.printf("GPU OCR最大并发线程数已调整为: %d%n", maxThreads);
    }

    /**
     * 删除任务
     */
    public boolean deleteTask(String taskId) {
        CompareTask task = tasks.remove(taskId);
        return task != null;
    }

    /**
     * 执行比对任务（文件路径）
     */
	private void executeCompareTaskWithPaths(CompareTask task, String oldFilePath, String newFilePath,
			CompareOptions options) { 
        
        // 调试日志：记录去水印设置
        System.out.println("Service收到的去水印设置: " + (options != null ? options.isRemoveWatermark() : "options为null"));
        
        // 创建进度管理器（正常模式，非调试模式）
        CompareTaskProgressManager progressManager = new CompareTaskProgressManager(task, false);
        
        // 设置任务开始时间
        task.setStartTime(java.time.LocalDateTime.now());
        
        // 在方法开始处定义frontendResult，以便在整个方法中使用
        Map<String, Object> frontendResult = null;
        
        // 记录文档基本信息
        Path oldPath = Paths.get(oldFilePath);
        Path newPath = Paths.get(newFilePath);
        progressManager.logBasicStats("开始文档比对: {} vs {}", 
            oldPath.getFileName(), newPath.getFileName());

        try {
            task.setStatus(CompareTask.Status.OCR_PROCESSING);
            
            // 步骤1: 初始化
            progressManager.startStep(TaskStep.INIT);
            
            // 根据options选择OCR服务
            boolean useThirdPartyOcr = options != null && options.isUseThirdPartyOcr();
            DotsOcrClient client = null;
            
            if (useThirdPartyOcr) {
                // 验证第三方OCR服务是否可用
                if (thirdPartyOcrService == null) {
                    throw new RuntimeException("第三方OCR服务未启用，请检查配置：zxcm.compare.third-party-ocr.enabled=true");
                }
                if (!thirdPartyOcrService.isAvailable()) {
                    throw new RuntimeException("第三方OCR服务不可用，请检查API密钥和网络连接");
                }
                progressManager.logStepDetail("使用第三方OCR服务 (阿里云Dashscope)");
            } else {
                // 使用DotsOCR服务
                client = new DotsOcrClient.Builder().baseUrl(gpuOcrConfig.getOcrBaseUrl())
                        .defaultModel(gpuOcrConfig.getOcrModel()).build();
                progressManager.logStepDetail("使用DotsOCR服务");
            }
            
            progressManager.completeStep(TaskStep.INIT);

            // 步骤2: OCR识别原文档
            progressManager.startStep(TaskStep.OCR_FIRST_DOC);
            
            // 提前获取PDF页数信息用于进度计算
            try (org.apache.pdfbox.pdmodel.PDDocument oldDoc = org.apache.pdfbox.pdmodel.PDDocument.load(oldPath.toFile());
                 org.apache.pdfbox.pdmodel.PDDocument newDoc = org.apache.pdfbox.pdmodel.PDDocument.load(newPath.toFile())) {
                int oldPages = oldDoc.getNumberOfPages();
                int newPages = newDoc.getNumberOfPages();
                int totalPages = Math.max(oldPages, newPages);
                
                // 分别设置两个文档的页数
                task.setOldDocPages(oldPages);
                task.setNewDocPages(newPages);
                task.setTotalPages(totalPages);
                
                progressManager.logStepDetail("📄 文档页数: 原文档{}页, 新文档{}页, 设置总页数为{}页", oldPages, newPages, totalPages);
            }
            
            // 注意：图片保存和去水印已集成到OCR识别流程中
            
			RecognitionResult resultA;
			if (useThirdPartyOcr) {
			    resultA = recognizePdfAsCharSeqWithThirdParty(oldPath, null, false, options, progressManager, task.getTaskId(), "old", task);
			} else {
			    resultA = recognizePdfAsCharSeq(client, oldPath, null, false, options, progressManager, task.getTaskId(), "old", task);
			}
			List<CharBox> seqA = resultA.charBoxes;
			progressManager.completeStep(TaskStep.OCR_FIRST_DOC);

            // 步骤3: OCR识别新文档
            progressManager.startStep(TaskStep.OCR_SECOND_DOC);
            
            // 注意：图片保存和去水印已集成到OCR识别流程中

			RecognitionResult resultB;
			if (useThirdPartyOcr) {
			    resultB = recognizePdfAsCharSeqWithThirdParty(newPath, null, false, options, progressManager, task.getTaskId(), "new", task);
			} else {
			    resultB = recognizePdfAsCharSeq(client, newPath, null, false, options, progressManager, task.getTaskId(), "new", task);
			}
			List<CharBox> seqB = resultB.charBoxes;
			progressManager.completeStep(TaskStep.OCR_SECOND_DOC);

            // 步骤4: OCR完成
            progressManager.startStep(TaskStep.OCR_COMPLETE);
            long ocrDuration = progressManager.getTotalDuration();
            progressManager.logOCRStats(seqA.size(), seqB.size(), ocrDuration);
            progressManager.completeStep(TaskStep.OCR_COMPLETE);

            // 步骤5: 文本比对
            progressManager.startStep(TaskStep.TEXT_COMPARE);
            String normA = preprocessTextForComparison(joinWithLineBreaks(seqA), options);
            String normB = preprocessTextForComparison(joinWithLineBreaks(seqB), options);

            DiffUtil dmp = new DiffUtil();
            dmp.Diff_EditCost = 6;
            LinkedList<DiffUtil.Diff> diffs = dmp.diff_main(normA, normB);
            dmp.diff_cleanupEfficiency(diffs);
            progressManager.completeStep(TaskStep.TEXT_COMPARE);

            // 步骤6: 差异分析
            progressManager.startStep(TaskStep.DIFF_ANALYSIS);
			List<DiffBlock> rawBlocks = DiffProcessingUtil.splitDiffsByBounding(diffs, seqA, seqB, false); // 正常模式不开启调试
            List<DiffBlock> filteredBlocks = DiffProcessingUtil.filterIgnoredDiffBlocks(rawBlocks, seqA, seqB);
            progressManager.completeStep(TaskStep.DIFF_ANALYSIS);

            // 步骤7: 差异块合并
            progressManager.startStep(TaskStep.BLOCK_MERGE);
            progressManager.logStepDetail("开始合并差异块，filteredBlocks大小: {}", filteredBlocks.size());
            List<DiffBlock> merged = mergeBlocksByBbox(filteredBlocks);
            progressManager.logStepDetail("合并完成，merged大小: {}", merged.size());
            progressManager.completeStep(TaskStep.BLOCK_MERGE);

            // 步骤8: OCR验证
            progressManager.startStep(TaskStep.OCR_VALIDATION);
            try {
                // 计算实际页数（取两个文档的最大页数）
                int actualTotalPages = Math.max(resultA.totalPages, resultB.totalPages);
                progressManager.logStepDetail("文档页数信息: 原文档{}页, 新文档{}页, 使用最大值{}页", 
                    resultA.totalPages, resultB.totalPages, actualTotalPages);
                
                // 设置任务的总页数
                task.setTotalPages(actualTotalPages);
                
                progressManager.logStepDetail("🚀 开始OCR验证（已优化并行处理）: {}个差异块", merged.size());
                DiffBlockValidationUtil.DiffBlockValidationResult validationResult = 
                    diffBlockValidationUtil.analyzeDiffBlocks(merged, task.getTaskId(), false, actualTotalPages);
                
                // 记录验证统计信息
                progressManager.logValidationStats(
                    validationResult.getTotalMergedCount(),
                    validationResult.getEligibleBlockCount(), 
                    validationResult.getTotalPages(),
                    validationResult.getPageThreshold(),
                    validationResult.isValidationTriggered(),
                    validationResult.getRemovedBlockCount());
                
                progressManager.logStepDetail("RapidOCR验证状态: {}", 
                    validationResult.isValidationTriggered() ? "已启动" : "未触发");
                
                // 总是使用过滤后的列表（无论验证是否被触发）
                if (validationResult.getFilteredBlocks() != null) {
                    merged = validationResult.getFilteredBlocks();
                    progressManager.logStepDetail("已使用过滤后的DiffBlock列表，剩余{}个块", merged.size());
                }
                
            } catch (Exception e) {
                progressManager.logError("RapidOCR验证过程出错: " + e.getMessage(), e);
            }
            progressManager.completeStep(TaskStep.OCR_VALIDATION);

            // 步骤9: 结果生成
            progressManager.startStep(TaskStep.RESULT_GENERATION);
            
            // 记录最终差异统计
            progressManager.logDiffStats(rawBlocks.size(), filteredBlocks.size(), merged.size());

            try {
                // 保存结果到任务
                progressManager.logStepDetail("创建CompareResult对象...");
                CompareResult result = new CompareResult(task.getTaskId());
                result.setOldFileName(task.getOldFileName());
                result.setNewFileName(task.getNewFileName());
                
                // 不再需要设置PDF URL，全部使用画布显示

				// 添加失败页面信息
				List<String> allFailedPages = new ArrayList<>();
				if (resultA != null && resultA.failedPages != null) {
					allFailedPages.addAll(resultA.failedPages);
				}
				if (resultB != null && resultB.failedPages != null) {
					allFailedPages.addAll(resultB.failedPages);
				}
				result.setFailedPages(allFailedPages);

                // 将DiffBlock列表转换为前端期望的Map格式（保留原始图像坐标，坐标转换在接口层进行）
                // 转换DiffBlock格式的信息通过进度管理器输出
				List<Map<String, Object>> formattedDifferences = convertDiffBlocksToMapFormat(merged, false, null, null);

                result.setDifferences(merged); // 保留原始的DiffBlock格式用于后端处理
                result.setFormattedDifferences(formattedDifferences); // 保存前端格式的差异数据

                // 不再需要baseUploadPath，全部使用画布显示

                // 创建包装对象用于返回前端期望的格式
                // 创建前端结果对象的信息通过进度管理器输出
                frontendResult = new HashMap<>();
                frontendResult.put("taskId", task.getTaskId());
                frontendResult.put("oldFileName", task.getOldFileName());
                frontendResult.put("newFileName", task.getNewFileName());
                // 不再需要PDF URL，全部使用画布显示
                frontendResult.put("differences", formattedDifferences);
                frontendResult.put("totalDiffCount", formattedDifferences.size());
                
                // 添加时间统计信息
                if (task.getStepDurations() != null && !task.getStepDurations().isEmpty()) {
                    frontendResult.put("stepDurations", task.getStepDurations());
                }
                if (task.getTotalDuration() != null) {
                    frontendResult.put("totalDuration", task.getTotalDuration());
                }
                if (task.getStartTime() != null) {
                    frontendResult.put("startTime", task.getStartTime().toString());
                }
                if (task.getEndTime() != null) {
                    frontendResult.put("endTime", task.getEndTime().toString());
                }
                
                // 添加失败页面信息
                if (task.getFailedPages() != null && !task.getFailedPages().isEmpty()) {
                    frontendResult.put("failedPages", task.getFailedPages());
                    frontendResult.put("failedPagesCount", task.getFailedPages().size());
                } else {
                    frontendResult.put("failedPages", new ArrayList<>());
                    frontendResult.put("failedPagesCount", 0);
                }
                
                // 添加统计信息
                if (task.getStatistics() != null && !task.getStatistics().isEmpty()) {
                    frontendResult.put("statistics", task.getStatistics());
                }

                // 不再需要页面高度，画布使用图片实际像素尺寸

                // 保存前端格式的结果
                // 保存结果到缓存的信息通过进度管理器输出
                results.put(task.getTaskId(), result);
                // 暂时不保存frontendResult，等时间信息完整后再保存

                progressManager.logStepDetail("比对结果保存完成");
            } catch (Exception ex) {
                progressManager.logError("保存比对结果失败: " + ex.getMessage(), ex);
            }
            
            progressManager.completeStep(TaskStep.RESULT_GENERATION);

            // 步骤10: 任务完成
            progressManager.startStep(TaskStep.TASK_COMPLETE);
            
            // 添加失败页面信息（从OCR结果中收集）
            List<String> allFailedPages = new ArrayList<>();
            if (resultA != null && resultA.failedPages != null) {
                allFailedPages.addAll(resultA.failedPages);
            }
            if (resultB != null && resultB.failedPages != null) {
                allFailedPages.addAll(resultB.failedPages);
            }
            progressManager.addFailedPages(allFailedPages);
            
            task.setStatus(CompareTask.Status.COMPLETED);
            progressManager.completeStep(TaskStep.TASK_COMPLETE);
            
            // 完成任务并同步统计信息（包括设置endTime）
            progressManager.completeTask();
            
            // 现在时间信息已完整，更新frontendResult并保存到文件
            if (frontendResult != null) {
                if (task.getTotalDuration() != null) {
                    frontendResult.put("totalDuration", task.getTotalDuration());
                }
                if (task.getStartTime() != null) {
                    frontendResult.put("startTime", task.getStartTime().toString());
                }
                if (task.getEndTime() != null) {
                    frontendResult.put("endTime", task.getEndTime().toString());
                }
            }
            
            // 保存包含完整时间信息的frontendResult
            frontendResults.put(task.getTaskId(), frontendResult);
            
            // 持久化写入磁盘，供前端或服务重启后读取
            try {
                Path jsonPath = getFrontendResultJsonPath(task.getTaskId());
                Files.createDirectories(jsonPath.getParent());
                byte[] json = M.writerWithDefaultPrettyPrinter().writeValueAsBytes(frontendResult);
                Files.write(jsonPath, json);
                progressManager.logStepDetail("✅ 前端结果已写入文件（包含完整时间信息）: {}", jsonPath.toAbsolutePath());
                logger.info("✅ 任务时间信息已持久化: startTime={}, endTime={}, duration={}ms", 
                    task.getStartTime(), task.getEndTime(), task.getTotalDuration());
            } catch (Exception ioEx) {
                progressManager.logError("写入前端结果JSON失败: " + ioEx.getMessage(), ioEx);
            }
            
            // 输出任务完成总结
            progressManager.logTaskSummary();

        } catch (Exception e) {
            task.setStatus(CompareTask.Status.FAILED);
            task.setErrorMessage("比对过程出错: " + e.getMessage());
            progressManager.logError("GPU OCR比对失败: " + e.getMessage(), e);
        }
    }

    /**
     * 执行调试比对任务 - 使用已有任务结果，跳过OCR，保留后续分析步骤
     */
	private void executeDebugCompareTaskWithExistingResult(CompareTask task, String originalTaskId,
			CompareOptions options) {
        
        // 创建进度管理器（调试模式）
        CompareTaskProgressManager progressManager = new CompareTaskProgressManager(task, true);
        
        // 设置任务开始时间
        task.setStartTime(java.time.LocalDateTime.now());
        
        progressManager.logBasicStats("开始调试比对任务: {} (原任务ID: {})", task.getTaskId(), originalTaskId);

        try {
            task.setStatus(CompareTask.Status.OCR_PROCESSING);
            
            // 步骤1: 读取原任务OCR结果
            progressManager.startStep(TaskStep.INIT);
            
            // 查找原任务的文件路径（从上传目录查找）
            String uploadRootPath = zxcmConfig.getFileUpload().getRootPath();
            Path taskDir = Paths.get(uploadRootPath, "compare-pro", "tasks", originalTaskId);
            
            if (!Files.exists(taskDir)) {
                throw new RuntimeException("原任务目录不存在: " + taskDir);
            }
            
			// 查找原任务的PDF文件；若不存在，则从已保存的OCR JSON推断基名以直接解析JSON
            Path oldPdfPath = findTaskPdfFile(taskDir, "old");
            Path newPdfPath = findTaskPdfFile(taskDir, "new");
            
            if (oldPdfPath == null || newPdfPath == null) {
				progressManager.logStepDetail("未找到PDF文件，尝试从OCR JSON推断基名进行调试解析...");
				Path[] jsonBases = findOcrJsonBases(taskDir);
				if (jsonBases == null || jsonBases.length < 2 || jsonBases[0] == null || jsonBases[1] == null) {
					throw new RuntimeException("无法找到原任务的PDF或OCR JSON基文件，目录: " + taskDir);
				}
				// 使用推断的基名路径充当pdfPath基准（parseCharBoxesFromSavedJson只依赖"基名.page-N.ocr.json"）
				oldPdfPath = jsonBases[0];
				newPdfPath = jsonBases[1];
				progressManager.logStepDetail("使用OCR JSON基名进行调试:");
				progressManager.logStepDetail("  原文档基名: {}", oldPdfPath);
				progressManager.logStepDetail("  新文档基名: {}", newPdfPath);
			} else {
                progressManager.logStepDetail("找到原任务PDF文件:");
                progressManager.logStepDetail("  原文档: {}", oldPdfPath);
                progressManager.logStepDetail("  新文档: {}", newPdfPath);

				// Debug模式复用原始任务的图片，不需要重新保存
				progressManager.logStepDetail("Debug模式：复用原始任务 {} 的OCR图片资源", originalTaskId);
            }
            progressManager.completeStep(TaskStep.INIT);

            // 步骤2: 解析OCR数据
            progressManager.startStep(TaskStep.OCR_FIRST_DOC); // 复用步骤枚举
            
            // 提前获取PDF页数信息用于进度计算（DEBUG模式）
            try (org.apache.pdfbox.pdmodel.PDDocument oldDoc = org.apache.pdfbox.pdmodel.PDDocument.load(oldPdfPath.toFile());
                 org.apache.pdfbox.pdmodel.PDDocument newDoc = org.apache.pdfbox.pdmodel.PDDocument.load(newPdfPath.toFile())) {
                int oldPages = oldDoc.getNumberOfPages();
                int newPages = newDoc.getNumberOfPages();
                int totalPages = Math.max(oldPages, newPages);
                
                // 分别设置两个文档的页数
                task.setOldDocPages(oldPages);
                task.setNewDocPages(newPages);
                task.setTotalPages(totalPages);
                
                System.out.println("[DEBUG] 文档页数: 原文档" + oldPages + "页, 新文档" + newPages + "页, 设置总页数为" + totalPages + "页");
            }
            
			// 从OCR结果中提取CharBox数据（使用与正常比对相同的方法）
			RecognitionResult resultA = recognizePdfAsCharSeq(null, oldPdfPath, null, true, options, null, null, "old", task);
			RecognitionResult resultB = recognizePdfAsCharSeq(null, newPdfPath, null, true, options, null, null, "new", task);
			List<CharBox> seqA = resultA.charBoxes;
			List<CharBox> seqB = resultB.charBoxes;
            
            if (seqA.isEmpty() || seqB.isEmpty()) {
                throw new RuntimeException("无法从OCR结果中提取字符数据");
            }

            long ocrDuration = progressManager.getTotalDuration();
            progressManager.logOCRStats(seqA.size(), seqB.size(), ocrDuration);
            progressManager.completeStep(TaskStep.OCR_FIRST_DOC);

            // 步骤3: 文本比对
            progressManager.startStep(TaskStep.TEXT_COMPARE);
            
            // 文本处理和差异分析（使用TextNormalizer进行完整预处理）
			String joinedA = joinWithLineBreaks(seqA);
			String joinedB = joinWithLineBreaks(seqB);
			String normA = preprocessTextForComparison(joinedA, options);
			String normB = preprocessTextForComparison(joinedB, options);

			// 调试：检查各阶段文本长度变化（仅Debug模式）
			progressManager.logStepDetail("seqA长度={}, joinedA长度={}, normA长度={}", seqA.size(), joinedA.length(), normA.length());
			progressManager.logStepDetail("seqB长度={}, joinedB长度={}, normB长度={}", seqB.size(), joinedB.length(), normB.length());
			progressManager.logStepDetail("joinWithLineBreaks增加了 {} 个字符(A), {} 个字符(B)", 
			    (joinedA.length() - seqA.size()), (joinedB.length() - seqB.size()));
            progressManager.completeStep(TaskStep.TEXT_COMPARE);

            // 步骤4: 差异分析
            progressManager.startStep(TaskStep.DIFF_ANALYSIS);

            DiffUtil dmp = new DiffUtil();
            dmp.Diff_EditCost = 6;
            LinkedList<DiffUtil.Diff> diffs = dmp.diff_main(normA, normB);
            dmp.diff_cleanupEfficiency(diffs);
            // 调试输出：仅打印新增/删除，不打印相等
            try {
                int ins = 0, del = 0;
				int diffIndex = 1;
                for (DiffUtil.Diff d : diffs) {
					if (d == null)
						continue;
					if (d.operation == DiffUtil.Operation.INSERT) {
						ins++;
						System.out.println(String.format("[DIFF][INSERT #%d] %s", diffIndex, d.text));
					} else if (d.operation == DiffUtil.Operation.DELETE) {
						del++;
						System.out.println(String.format("[DIFF][DELETE #%d] %s", diffIndex, d.text));
					}
					diffIndex++;
                }
                System.out.println("[DIFF] INSERTs=" + ins + ", DELETEs=" + del + ", TOTAL=" + diffs.size());
			} catch (Exception ignore) {
			}

            task.updateProgress(5, "生成差异块");

			List<DiffBlock> rawBlocks = DiffProcessingUtil.splitDiffsByBounding(diffs, seqA, seqB, true); // Debug模式开启调试
            List<DiffBlock> filteredBlocks = DiffProcessingUtil.filterIgnoredDiffBlocks(rawBlocks, seqA, seqB);

            task.updateProgress(6, "合并差异块");

            System.out.println("开始合并差异块，filteredBlocks大小: " + filteredBlocks.size());

            List<DiffBlock> merged = mergeBlocksByBbox(filteredBlocks);

			System.out.println(String.format("差异分析完成。原始差异块=%d, 过滤后=%d, 合并后=%d", rawBlocks.size(), filteredBlocks.size(),
					merged.size()));

            // RapidOCR验证（DEBUG模式）
            try {
                // 计算实际页数（取两个文档的最大页数）
                int actualTotalPages = Math.max(resultA.totalPages, resultB.totalPages);
                System.out.println("[DEBUG] 文档页数信息: 原文档" + resultA.totalPages + "页, 新文档" + resultB.totalPages + "页, 使用最大值" + actualTotalPages + "页");
                
                // 设置任务的总页数
                task.setTotalPages(actualTotalPages);
                
                DiffBlockValidationUtil.DiffBlockValidationResult validationResult = 
                    diffBlockValidationUtil.analyzeDiffBlocks(merged, originalTaskId, true, actualTotalPages);
                
                // 显示验证统计信息
                System.out.println("[DEBUG] 🔍 验证统计: 总merged=" + validationResult.getTotalMergedCount() + 
                    ", 符合条件=" + validationResult.getEligibleBlockCount() + 
                    ", 总页数=" + validationResult.getTotalPages() + 
                    ", 页数阈值=" + validationResult.getPageThreshold() +
                    ", 移除幻觉块=" + validationResult.getRemovedBlockCount());
                
                if (validationResult.isValidationTriggered()) {
                    System.out.println("[DEBUG] RapidOCR验证已启动，处理了 " + 
                        (validationResult.getValidationItems() != null ? validationResult.getValidationItems().size() : 0) + " 个DiffBlock");
                    
                    // 输出详细的验证结果
                    if (validationResult.getValidationItems() != null) {
                        for (DiffBlockValidationUtil.DiffBlockValidationItem item : validationResult.getValidationItems()) {
                            System.out.println("[DEBUG] " + item.toString());
                        }
                    }
                } else {
                    System.out.println("[DEBUG] RapidOCR验证未触发"); 
                }
                
                // 总是使用过滤后的列表（无论验证是否被触发）
                if (validationResult.getFilteredBlocks() != null) {
                    merged = validationResult.getFilteredBlocks();
                    System.out.println("[DEBUG] 已使用过滤后的DiffBlock列表，剩余" + merged.size() + "个块");
                }
            } catch (Exception e) {
                System.err.println("[DEBUG] RapidOCR验证过程出错: " + e.getMessage());
                e.printStackTrace();
            }

            task.updateProgress(7, "比对完成");

            // 创建比对结果对象
            CompareResult result = new CompareResult();
			result.setTaskId(originalTaskId); // Debug模式使用原始任务ID
                result.setOldFileName(task.getOldFileName());
                result.setNewFileName(task.getNewFileName());

			// 添加失败页面信息
			List<String> allFailedPages = new ArrayList<>();
			if (resultA != null && resultA.failedPages != null) {
				allFailedPages.addAll(resultA.failedPages);
			}
			if (resultB != null && resultB.failedPages != null) {
				allFailedPages.addAll(resultB.failedPages);
			}
			result.setFailedPages(allFailedPages);

			// 不再需要PDF URL，全部使用画布显示
            result.setDifferences(merged);
            result.setTotalDiffCount(merged.size());

            // 转换为前端格式（保存为原始图像坐标，实际坐标转换在getFrontendResult中统一进行）
			List<Map<String, Object>> formattedDifferences = convertDiffBlocksToMapFormat(merged, true, seqA, seqB);

                // 创建包装对象用于返回前端期望的格式
                // 创建前端结果对象的信息通过进度管理器输出
                Map<String, Object> frontendResult = new HashMap<>();
			frontendResult.put("taskId", originalTaskId); // Debug模式使用原始任务ID
                frontendResult.put("oldFileName", task.getOldFileName());
                frontendResult.put("newFileName", task.getNewFileName());
            // 不再需要PDF URL，全部使用画布显示
                frontendResult.put("differences", formattedDifferences);
                frontendResult.put("totalDiffCount", formattedDifferences.size());
                
                // 添加时间统计信息
                if (task.getStepDurations() != null && !task.getStepDurations().isEmpty()) {
                    frontendResult.put("stepDurations", task.getStepDurations());
                }
                if (task.getTotalDuration() != null) {
                    frontendResult.put("totalDuration", task.getTotalDuration());
                }
                if (task.getStartTime() != null) {
                    frontendResult.put("startTime", task.getStartTime().toString());
                }
                if (task.getEndTime() != null) {
                    frontendResult.put("endTime", task.getEndTime().toString());
                }
                
                // 添加失败页面信息
                if (task.getFailedPages() != null && !task.getFailedPages().isEmpty()) {
                    frontendResult.put("failedPages", task.getFailedPages());
                    frontendResult.put("failedPagesCount", task.getFailedPages().size());
                } else {
                    frontendResult.put("failedPages", new ArrayList<>());
                    frontendResult.put("failedPagesCount", 0);
                }
                
                // 添加统计信息
                if (task.getStatistics() != null && !task.getStatistics().isEmpty()) {
                    frontendResult.put("statistics", task.getStatistics());
                }

            // 不再需要页面高度，画布使用图片实际像素尺寸

			// 保存前端格式的结果（Debug模式使用原始任务ID）
                // 保存结果到缓存的信息通过进度管理器输出
			results.put(originalTaskId, result);
			// 暂时不保存frontendResult，等时间信息完整后再保存

            task.setStatus(CompareTask.Status.COMPLETED);
            task.updateProgress(8, "调试比对完成");
            
            // 完成任务并同步统计信息（包括设置endTime）
            progressManager.completeTask();
            
            // 现在时间信息已完整，更新frontendResult并保存到文件
            if (frontendResult != null) {
                if (task.getTotalDuration() != null) {
                    frontendResult.put("totalDuration", task.getTotalDuration());
                }
                if (task.getStartTime() != null) {
                    frontendResult.put("startTime", task.getStartTime().toString());
                }
                if (task.getEndTime() != null) {
                    frontendResult.put("endTime", task.getEndTime().toString());
                }
            }
            
            // 保存包含完整时间信息的frontendResult
            frontendResults.put(originalTaskId, frontendResult);

            // 调试模式也需要生成前端结果文件，供前端查看
                try {
				Path jsonPath = getFrontendResultJsonPath(originalTaskId);
                    Files.createDirectories(jsonPath.getParent());
                    byte[] json = M.writerWithDefaultPrettyPrinter().writeValueAsBytes(frontendResult);
                    Files.write(jsonPath, json);
                System.out.println("调试模式前端结果已写入文件: " + jsonPath.toAbsolutePath());
                } catch (Exception ioEx) {
                System.err.println("调试模式写入前端结果JSON失败: " + ioEx.getMessage());
            }

            task.setStatus(CompareTask.Status.COMPLETED);
            task.updateProgress(8, "调试比对完成");
            
            // 完成任务并同步统计信息
            progressManager.completeTask();
            
            long totalTime = progressManager.getTotalDuration();
			System.out
					.println(String.format("GPU OCR调试比对完成。差异数量=%d, 总耗时=%dms", formattedDifferences.size(), totalTime));
            System.out.println("GPU OCR调试比对完成，使用画布显示结果");

        } catch (Exception e) {
            System.err.println("GPU OCR调试比对过程中发生异常:");
            System.err.println("当前步骤: " + task.getCurrentStep() + " - " + task.getCurrentStepDesc());
            System.err.println("错误信息: " + e.getMessage());

            task.setStatus(CompareTask.Status.FAILED);
            task.setErrorMessage("调试比对失败 [步骤" + task.getCurrentStep() + "]: " + e.getMessage());
            task.updateProgress(task.getCurrentStep(), "比对失败: " + e.getMessage());

            e.printStackTrace();
        }
    }

    /**
     * 使用TextNormalizer进行文本预处理，用于比对
     * 
	 * @param text    原始文本
     * @param options 比对选项
     * @return 预处理后的文本
     */
    private String preprocessTextForComparison(String text, CompareOptions options) {
        if (text == null || text.isEmpty()) {
            return "";
        }

		// 调试：处理前长度
		try {
        // 预处理长度信息在调试模式下通过日志输出
        //logger.debug("[PREPROCESS] before length={}", text.length());
		} catch (Exception ignore) {
        }
        
        // 1. 使用TextNormalizer进行标点符号标准化
        String normalized = TextNormalizer.normalizePunctuation(text);
        
        // 2. 清理OCR识别中常见的特殊字符问题
        normalized = normalized.replace('$', ' ').replace('_', ' ');

		// 4. 处理规则：空格 + 标点符号 场景替换为等长空格串，保持字符位移一致
		// 示例：" ;"、" 。"、" \t, "、" . ." → 用相同长度的空格替换
		// 说明：用正则逐段匹配并按匹配长度替换，避免位移差异
		{
			Pattern wsPunct = Pattern.compile("[\\s\\p{Punct}，。；：、！？…·•]+");
			Matcher m = wsPunct.matcher(normalized);
			StringBuffer sb = new StringBuffer();
			while (m.find()) {
				int len = m.end() - m.start();
				String spaces = " ".repeat(len);
				m.appendReplacement(sb, Matcher.quoteReplacement(spaces));
			}
			m.appendTail(sb);
			normalized = sb.toString();
		}
        
        // 3. 根据选项处理大小写
        if (options.isIgnoreCase()) {
            normalized = normalized.toLowerCase();
        }

		// 调试：处理后长度
		try {
        // 预处理长度信息在调试模式下通过日志输出
        //logger.debug("[PREPROCESS] after length={}", normalized.length());
		} catch (Exception ignore) {
        }
        
        return normalized;
    }

    /**
     * 查找任务目录中的PDF文件
     */
    private Path findTaskPdfFile(Path taskDir, String type) {
        try (var stream = Files.list(taskDir)) {
			return stream.filter(path -> path.toString().toLowerCase().endsWith(".pdf")).filter(path -> {
                    String fileName = path.getFileName().toString().toLowerCase();
                    return fileName.startsWith(type + "_");
			}).findFirst().orElse(null);
        } catch (Exception e) {
            System.err.println("查找" + type + "PDF文件失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
	 * 在任务目录中尝试推断old/new的OCR JSON基名（即去掉.page-N.ocr.json之前的部分） 约定：存在形如
	 * old*.page-1.ocr.json 或 new*.page-1.ocr.json 的文件
	 * 若未显式包含old/new前缀，则回退为任取两条不同前缀的page-1.ocr.json 返回长度为2的数组：[oldBase,
	 * newBase]，若失败返回null
	 */
	private Path[] findOcrJsonBases(Path taskDir) {
		try {
			if (taskDir == null || !Files.exists(taskDir) || !Files.isDirectory(taskDir))
				return null;

			Path oldBase = null;
			Path newBase = null;

			// 优先匹配含有old标识的第一页OCR结果
			try (var s = Files.list(taskDir)) {
				Path candidate = s.filter(p -> {
					String name = p.getFileName().toString().toLowerCase(Locale.ROOT);
					return name.contains("old") && name.endsWith(".page-1.ocr.json");
				}).findFirst().orElse(null);
				if (candidate != null) {
					String cs = candidate.toAbsolutePath().toString();
					int idx = cs.lastIndexOf(".page-1.ocr.json");
					if (idx > 0)
						oldBase = Path.of(cs.substring(0, idx));
				}
			} catch (Exception ignore) {
			}

			// 优先匹配含有new标识的第一页OCR结果
			try (var s = Files.list(taskDir)) {
				Path candidate = s.filter(p -> {
					String name = p.getFileName().toString().toLowerCase(Locale.ROOT);
					return name.contains("new") && name.endsWith(".page-1.ocr.json");
				}).findFirst().orElse(null);
				if (candidate != null) {
					String cs = candidate.toAbsolutePath().toString();
					int idx = cs.lastIndexOf(".page-1.ocr.json");
					if (idx > 0)
						newBase = Path.of(cs.substring(0, idx));
				}
			} catch (Exception ignore) {
			}

			// 回退：任取两条不同前缀的第一页OCR结果
			if (oldBase == null || newBase == null) {
				List<Path> firstPages = new ArrayList<>();
				try (var s = Files.list(taskDir)) {
					s.filter(p -> p.getFileName().toString().endsWith(".page-1.ocr.json")).forEach(firstPages::add);
				}
				if (firstPages.size() >= 2) {
					String a = firstPages.get(0).toAbsolutePath().toString();
					String b = firstPages.get(1).toAbsolutePath().toString();
					int ia = a.lastIndexOf(".page-1.ocr.json");
					int ib = b.lastIndexOf(".page-1.ocr.json");
					if (ia > 0 && ib > 0) {
						if (oldBase == null)
							oldBase = Path.of(a.substring(0, ia));
						if (newBase == null)
							newBase = Path.of(b.substring(0, ib));
					}
				}
			}

			if (oldBase != null && newBase != null) {
				return new Path[] { oldBase, newBase };
			}
		} catch (Exception ignore) {
		}
		return null;
	}

    // ---------- OCR辅助方法 ----------

    private int countPdfPages(Path pdfPath) throws Exception {
        try (PDDocument doc = PDDocument.load(pdfPath.toFile())) {
            return doc.getNumberOfPages();
        }
    }

	/**
	 * 计算PDF每页的高度（用于页眉页脚百分比计算）
	 * 
	 * @param pdfPath PDF文件路径
	 * @return 每页的高度数组（单位：点，72 DPI）
	 */
	private double[] calculatePageHeights(Path pdfPath) {
        return calculatePageHeights(pdfPath, null);
    }
    
    private double[] calculatePageHeights(Path pdfPath, CompareTaskProgressManager progressManager) {
		if (pdfPath == null) {
			return new double[0];
		}

		try (PDDocument doc = PDDocument.load(pdfPath.toFile())) {
			int pageCount = doc.getNumberOfPages();
			double[] heights = new double[pageCount];

			for (int i = 0; i < pageCount; i++) {
				PDPage page = doc.getPage(i);
				PDRectangle mediaBox = page.getMediaBox();
				heights[i] = mediaBox.getHeight(); // 页面高度（点单位）
			}

            if (progressManager != null) {
                progressManager.logStepDetail("计算PDF页面高度完成: {}, 页数: {}, 首页高度: {}点", 
                    pdfPath.getFileName(), pageCount, (heights.length > 0 ? heights[0] : 0));
            }

			return heights;

		} catch (Exception e) {
			System.err.println("计算PDF页面高度失败: " + e.getMessage());
			return new double[0];
        }
    }

    private TextExtractionUtil.PageLayout parseOnePageFromSavedJson(Path pdfPath, int page) throws Exception {
        String pageJsonPath = pdfPath.toAbsolutePath().toString() + ".page-" + page + ".ocr.json";
        byte[] bytes = Files.readAllBytes(Path.of(pageJsonPath));
        JsonNode root = M.readTree(bytes);
        List<TextExtractionUtil.LayoutItem> items = extractLayoutItems(root);
		// 从已保存的PNG读取图片尺寸（如果存在同名PNG）
		int imgW = 0;
		int imgH = 0;
		try {
			Path pngPath = pdfPath.getParent().resolve(pdfPath.getFileName().toString() + ".page-" + page + ".png");
			if (Files.exists(pngPath)) {
				BufferedImage img = ImageIO.read(pngPath.toFile());
				if (img != null) {
					imgW = img.getWidth();
					imgH = img.getHeight();
				}
			}
		} catch (Exception ignore) {
		}
		return new TextExtractionUtil.PageLayout(page, items, imgW, imgH);
	}

	private TextExtractionUtil.PageLayout parseOnePage(DotsOcrClient client, byte[] pngBytes, int page, String prompt,
			Path pdfPath) throws Exception {
        return parseOnePage(client, pngBytes, page, prompt, pdfPath, null);
    }
    
    private TextExtractionUtil.PageLayout parseOnePage(DotsOcrClient client, byte[] pngBytes, int page, String prompt,
			Path pdfPath, CompareTaskProgressManager progressManager) throws Exception {
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
                if (progressManager != null) {
                    progressManager.logStepDetail("JSON修复成功 - 页面: {}", page);
                }
            } catch (Exception e2) {
                System.err.println("JSON修复失败: " + e2.getMessage());
                throw new RuntimeException("JSON解析失败 (页面=" + page + "): " + e.getMessage(), e);
            }
        }
		// 获取图片尺寸信息（不修改OCR JSON，直接用于PageLayout）
		int imgW = 0;
		int imgH = 0;
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(pngBytes);
			BufferedImage image = ImageIO.read(bais);
			if (image != null) {
				imgW = image.getWidth();
				imgH = image.getHeight();
                if (progressManager != null) {
                    progressManager.logStepDetail("第{}页图片尺寸: {}x{}", page, imgW, imgH);
                }
			}
		} catch (Exception e) {
			System.err.println("获取第" + page + "页图片尺寸失败: " + e.getMessage());
		}

        // 保存每页识别的 JSON 结果，便于后续从第4步直接开始
        try {
            String pageJsonPath = pdfPath.toAbsolutePath().toString() + ".page-" + page + ".ocr.json";
            Files.write(Path.of(pageJsonPath), M.writerWithDefaultPrettyPrinter().writeValueAsBytes(root));
            if (progressManager != null) {
                progressManager.logStepDetail("Saved OCR JSON: {}", pageJsonPath);
            }
        } catch (Exception e) {
            System.err.println("Failed to save OCR JSON for page " + page + ": " + e.getMessage());
        }
        List<TextExtractionUtil.LayoutItem> items = extractLayoutItems(root);
        long pageCost = System.currentTimeMillis() - pageStartAt;
        try {
            if (progressManager != null) {
                progressManager.logStepDetail("OCR单页完成: file={}, page={}, 用时={}ms", 
                    pdfPath == null ? "-" : pdfPath.getFileName().toString(), page, pageCost);
            }
            
            // 计算识别到的字符数
            int charCount = 0;
            for (TextExtractionUtil.LayoutItem item : items) {
                if (item.text != null) {
                    charCount += item.text.length();
                }
            }
            System.out.println("📖 第" + page + "页OCR识别完成，识别到 " + charCount + " 个字符，用时: " + pageCost + "ms");
            
		} catch (Exception ignore) {
		}
		return new TextExtractionUtil.PageLayout(page, items, imgW, imgH);
    }

    private List<byte[]> renderAllPagesToPng(DotsOcrClient client, Path pdfPath) throws Exception {
        return renderAllPagesToPng(client, pdfPath, null);
    }

    private List<byte[]> renderAllPagesToPng(DotsOcrClient client, Path pdfPath, CompareOptions options) throws Exception {
        return renderAllPagesToPng(client, pdfPath, options, null, null);
    }

    /**
     * PDF转图片，可选去水印和保存
     * @param client OCR客户端
     * @param pdfPath PDF路径
     * @param options 比对选项
     * @param taskId 任务ID（用于保存图片）
     * @param mode 模式（old/new，用于保存图片）
     * @return 处理后的图片字节数组列表
     */
    private List<byte[]> renderAllPagesToPng(DotsOcrClient client, Path pdfPath, CompareOptions options, 
                                           String taskId, String mode) throws Exception {
		// 加载PDF文档并计算页数
		try (PDDocument doc = PDDocument.load(pdfPath.toFile())) {
			int pageCount = doc.getNumberOfPages();

			// 使用固定DPI（来自配置）
        int dpi = gpuOcrConfig.getRenderDpi();
        
        // 判断是否需要保存图片
        boolean shouldSaveImages = (taskId != null && mode != null && gpuOcrConfig.isSaveOcrImages());
        Path imagesDir = null;
        if (shouldSaveImages) {
            String uploadRootPath = zxcmConfig.getFileUpload().getRootPath();
            imagesDir = Paths.get(uploadRootPath, "compare-pro", "tasks", taskId, "images", mode);
            Files.createDirectories(imagesDir);
            System.out.println("[" + mode + "] 创建图片保存目录: " + imagesDir);
        }
        
        // 判断是否需要去水印
        boolean shouldRemoveWatermark = (options != null && options.isRemoveWatermark());
        String watermarkStrength = shouldRemoveWatermark ? options.getWatermarkRemovalStrength() : null;
        
        System.out.println("📄 PDF转图片流程开始 - 页数: " + pageCount + ", DPI: " + dpi + ", 保存图片: " + shouldSaveImages);
        
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
						g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
								RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        g2d.drawImage(image, 0, 0, newW, newH, null);
                        g2d.dispose();
                        image.flush();
                        image = scaled;
                    }
                }
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    ImageIO.write(image, "png", baos);
                    byte[] bytes = baos.toByteArray();
                    
                    // 如果开启去水印，对图片字节进行去水印处理
                    if (shouldRemoveWatermark) {
                        System.out.println("🧹 [" + mode + "] 第" + (i + 1) + "页开始去水印处理，强度: " + watermarkStrength);
                        bytes = applyWatermarkRemoval(bytes, watermarkStrength, i + 1, mode);
                    }
                    
                    // 如果需要保存图片，保存到磁盘
                    if (shouldSaveImages) {
                        Path imagePath = imagesDir.resolve("page-" + (i + 1) + ".png");
                        Files.write(imagePath, bytes);
                        System.out.println("💾 [" + mode + "] 图片处理进度 [" + (i + 1) + "/" + pageCount + "] 第" + (i + 1) + "页已保存");
                    } else {
                        System.out.println("📄 [" + mode + "] 图片转换进度 [" + (i + 1) + "/" + pageCount + "] 第" + (i + 1) + "页完成");
                    }
                    
                    // 添加到返回列表供OCR使用
                    list.add(bytes);
                    
                }
            }
            
            return list;
        }
    }

    /**
     * 对图片字节应用去水印处理
     */
    private byte[] applyWatermarkRemoval(byte[] imageBytes, String strength, int pageNo, String mode) {
        String modePrefix = mode != null ? "[" + mode + "] " : "";
        try {
            // 使用系统配置的根路径创建临时文件夹（避免中文路径问题）
            String uploadRootPath = zxcmConfig.getFileUpload().getRootPath();
            Path tempDir = Paths.get(uploadRootPath, "temp");
            if (!Files.exists(tempDir)) {
                Files.createDirectories(tempDir);
            }
            
            // 使用英文路径避免OpenCV读取问题
            String tempFileName = "watermark_removal_" + System.currentTimeMillis() + "_" + pageNo + ".png";
            Path tempFile = tempDir.resolve(tempFileName);
            Files.write(tempFile, imageBytes);
            
            // 去水印开始日志已在调用处显示
            
            boolean success = false;
            switch (strength) {
                case "default":
                    success = watermarkRemover.removeWatermark(tempFile.toString());
                    break;
                case "extended":
                    success = watermarkRemover.removeWatermarkExtended(tempFile.toString());
                    break;
                case "loose":
                    success = watermarkRemover.removeWatermarkLoose(tempFile.toString());
                    break;
                case "smart":
                default:
                    success = watermarkRemover.removeWatermarkSmart(tempFile.toString());
                    break;
            }
            
            if (success) {
                System.out.println("✅ " + modePrefix + "第" + pageNo + "页去水印成功(" + strength + ")");
                // 读取处理后的图片
                byte[] processedBytes = Files.readAllBytes(tempFile);
                Files.deleteIfExists(tempFile);
                return processedBytes;
            } else {
                System.out.println("❌ " + modePrefix + "第" + pageNo + "页去水印失败(" + strength + ")，使用原图");
                Files.deleteIfExists(tempFile);
                return imageBytes;
            }
            
        } catch (Exception e) {
            System.err.println(modePrefix + "第" + pageNo + "页去水印处理异常: " + e.getMessage());
            return imageBytes; // 出错时返回原始图片
        }
    }

    private List<TextExtractionUtil.LayoutItem> extractLayoutItems(JsonNode root) {
        return TextExtractionUtil.extractLayoutItems(root);
    }

	// 辅助方法：创建空页面布局（用于处理识别失败的页面）
	private TextExtractionUtil.PageLayout createEmptyPageLayout(int pageNo) {
		List<TextExtractionUtil.LayoutItem> emptyItems = new ArrayList<>();
		return new TextExtractionUtil.PageLayout(pageNo, emptyItems, 0, 0);
	}

	// 辅助方法：检查是否为空页面布局
	private boolean isEmptyPageLayout(TextExtractionUtil.PageLayout layout) {
		return layout.items == null || layout.items.isEmpty();
	}

    // 以下方法是从DotsOcrCompareDemoTest复制并适配的

	private RecognitionResult recognizePdfAsCharSeq(DotsOcrClient client, Path pdf, String prompt,
			boolean resumeFromStep4, CompareOptions options) throws Exception {
        return recognizePdfAsCharSeq(client, pdf, prompt, resumeFromStep4, options, null, null, null, null);
    }
    
    private RecognitionResult recognizePdfAsCharSeq(DotsOcrClient client, Path pdf, String prompt,
			boolean resumeFromStep4, CompareOptions options, CompareTaskProgressManager progressManager) throws Exception {
        return recognizePdfAsCharSeq(client, pdf, prompt, resumeFromStep4, options, progressManager, null, null, null);
    }
    
    private RecognitionResult recognizePdfAsCharSeq(DotsOcrClient client, Path pdf, String prompt,
			boolean resumeFromStep4, CompareOptions options, CompareTaskProgressManager progressManager,
			String taskId, String mode, CompareTask task) throws Exception {
        TextExtractionUtil.PageLayout[] ordered;
		List<String> failedPages = new ArrayList<>();
		String documentName = pdf.getFileName().toString();

        long ocrAllStartAt = System.currentTimeMillis();
        if (resumeFromStep4) {
            // Step 1 (count pages) + Step 2 skipped; load Step 3 results (saved JSON)
            int total = countPdfPages(pdf);
            ordered = new TextExtractionUtil.PageLayout[total];
            for (int i = 0; i < total; i++) {
                final int pageNo = i + 1;
				try {
                TextExtractionUtil.PageLayout p = parseOnePageFromSavedJson(pdf, pageNo);
                ordered[pageNo - 1] = p;
				} catch (Exception e) {
					System.err.println("解析第" + pageNo + "页OCR结果失败: " + e.getMessage());
					ordered[pageNo - 1] = createEmptyPageLayout(pageNo);
					failedPages.add(documentName + "-第" + pageNo + "页: " + e.getMessage());
				}
            }
        } else {
                // Step 1: render PDF to images (集成去水印和保存)
                List<byte[]> pages = renderAllPagesToPng(client, pdf, options, taskId, mode);
                int total = pages.size();
                int parallel = Math.max(1, gpuOcrConfig.getParallelThreads()); // 使用配置的并行线程数
                java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors
                        .newFixedThreadPool(Math.min(parallel, total));
			java.util.concurrent.ExecutorCompletionService<TextExtractionUtil.PageLayout> ecs = new java.util.concurrent.ExecutorCompletionService<>(
					pool);

			// 提交所有任务，包装异常处理
                for (int i = 0; i < total; i++) {
                    final int pageNo = i + 1;
                    final byte[] img = pages.get(i);
				ecs.submit(() -> {
					try {
						return parseOnePage(client, img, pageNo, prompt, pdf, progressManager);
					} catch (Exception e) {
						System.err.println("OCR识别第" + pageNo + "页失败: " + e.getMessage());
						return createEmptyPageLayout(pageNo);
					}
				});
			}

			// 收集结果，处理超时和异常
                ordered = new TextExtractionUtil.PageLayout[total];
                System.out.println("📊 开始收集OCR识别结果，共 " + total + " 页");
                
                for (int i = 0; i < total; i++) {
				try {
                    TextExtractionUtil.PageLayout p = ecs.take().get();
					if (p != null) {
                    ordered[p.page - 1] = p;
                    System.out.println("📋 OCR收集进度 [" + (i + 1) + "/" + total + "] 第" + p.page + "页结果已收集");
                    
                    // 更新CompareTask的页面进度
                    if (task != null && mode != null) {
                        if ("old".equals(mode)) {
                            task.setCurrentPageOld(p.page);
                            task.setCompletedPagesOld(i + 1);
                        } else if ("new".equals(mode)) {
                            task.setCurrentPageNew(p.page);
                            task.setCompletedPagesNew(i + 1);
                        }
                    }
                    
						// 检查是否为空页面布局（表示识别失败）
						if (isEmptyPageLayout(p)) {
							failedPages.add(documentName + "-第" + p.page + "页: OCR识别失败");
						}
					} else {
						// 不应该发生，但为了安全起见
						ordered[i] = createEmptyPageLayout(i + 1);
						failedPages.add(documentName + "-第" + (i + 1) + "页: 返回null结果");
                        System.out.println("⚠️  OCR收集进度 [" + (i + 1) + "/" + total + "] 第" + (i + 1) + "页返回null结果");
					}
				} catch (Exception e) {
					System.err.println("❌ OCR收集进度 [" + (i + 1) + "/" + total + "] 第" + (i + 1) + "页识别失败: " + e.getMessage());
					// 创建空页面布局
					TextExtractionUtil.PageLayout emptyPage = createEmptyPageLayout(i + 1);
					ordered[i] = emptyPage;
					
					// 即使失败也要更新页面进度
                    if (task != null && mode != null) {
                        if ("old".equals(mode)) {
                            task.setCurrentPageOld(i + 1);
                            task.setCompletedPagesOld(i + 1);
                        } else if ("new".equals(mode)) {
                            task.setCurrentPageNew(i + 1);
                            task.setCompletedPagesNew(i + 1);
                        }
                    }

					String errorMsg = e.getMessage();
					if (errorMsg != null && errorMsg.contains("timeout")) {
						failedPages.add(documentName + "-第" + (i + 1) + "页: 超时错误");
					} else {
						failedPages.add(documentName + "-第" + (i + 1) + "页: " + errorMsg);
					}
				}
                }
                pool.shutdownNow();
                System.out.println("🎉 OCR识别结果收集完成，共处理 " + total + " 页");
        }

        long ocrAllCost = System.currentTimeMillis() - ocrAllStartAt;
        try {
            int pages = ordered == null ? 0 : ordered.length;
            double avg = pages > 0 ? (ocrAllCost * 1.0 / pages) : 0.0;
            if (progressManager != null) {
                progressManager.logStepDetail("OCR识别完成: file={}, 页数={}, 总用时={}ms, 平均每页={:.1f}ms", 
                    pdf == null ? "-" : pdf.getFileName().toString(), pages, ocrAllCost, avg);
            }
		} catch (Exception ignore) {
		}

		// 计算页面高度信息用于页眉页脚检测
		double[] pageHeights;
		if (resumeFromStep4) {
			// Debug模式：从保存的图片文件中读取宽高信息
			pageHeights = new double[ordered.length];
			for (int i = 0; i < ordered.length; i++) {
				TextExtractionUtil.PageLayout pl = ordered[i];
				if (pl != null && pl.imageHeight > 0) {
					// 如果OCR结果中有imageHeight，直接使用
					pageHeights[i] = pl.imageHeight;
				} else {
					// 从保存的图片文件中读取高度
					double imageHeight = getImageHeightFromSavedFile(pdf, i + 1);
					pageHeights[i] = imageHeight;
				}
			}
			System.out.println("Debug模式：从保存的图片文件中读取宽高信息进行页眉页脚检测");
		} else {
			pageHeights = calculatePageHeights(pdf);
		}

		// 使用新的按顺序读取方法解析文本和位置，支持基于位置的页眉页脚检测
		List<CharBox> out = TextExtractionUtil.parseTextAndPositionsFromResults(ordered,
				TextExtractionUtil.ExtractionStrategy.SEQUENTIAL, options.isIgnoreHeaderFooter(),
				options.getHeaderHeightPercent(), options.getFooterHeightPercent(), pageHeights);

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

		int totalPages = ordered == null ? 0 : ordered.length;
		return new RecognitionResult(out, failedPages, totalPages);
    }

    /**
     * 使用第三方OCR服务识别PDF文档
     * 基于阿里云Dashscope的通义千问VL模型进行识别
     */
    private RecognitionResult recognizePdfAsCharSeqWithThirdParty(Path pdf, String prompt, boolean resumeFromStep4, 
                                                                  CompareOptions options, CompareTaskProgressManager progressManager, 
                                                                  String taskId, String mode, CompareTask task) {
        List<String> failedPages = new ArrayList<>();
        
        try {
            if (pdf == null) {
                throw new RuntimeException("PDF路径为空");
            }

            // 判断是否需要保存图片
            boolean shouldSaveImages = (taskId != null && mode != null && gpuOcrConfig.isSaveOcrImages());
            Path imagesDir = null;
            if (shouldSaveImages) {
                String uploadRootPath = zxcmConfig.getFileUpload().getRootPath();
                imagesDir = Paths.get(uploadRootPath, "compare-pro", "tasks", taskId, "images", mode);
                Files.createDirectories(imagesDir);
                progressManager.logStepDetail("[{}] 创建图片保存目录: {}", mode, imagesDir);
            }

            // 步骤1: 将PDF转换为图片
            progressManager.logStepDetail("开始PDF转图片处理: {}", pdf.getFileName());
            List<byte[]> pngPages = renderAllPagesToPng(null, pdf, options, taskId, mode);
            
            if (pngPages.isEmpty()) {
                throw new RuntimeException("PDF转图片失败，未生成任何页面");
            }

            progressManager.logStepDetail("PDF转图片完成，共{}页", pngPages.size());

            // 步骤2: 使用第三方OCR并行识别所有页面
            int total = pngPages.size();
            String documentName = pdf.getFileName().toString();
            
            progressManager.logStepDetail("🚀 开始第三方OCR识别: {}页面", total);
            
            // 创建页面布局数组
            TextExtractionUtil.PageLayout[] ordered = new TextExtractionUtil.PageLayout[total];
            
            // 使用并发处理提高效率，并发数由ThirdPartyOcrClient控制
            ExecutorService executor = Executors.newFixedThreadPool(Math.min(total, 8)); // 限制最大8个线程
            List<Future<Void>> futures = new ArrayList<>();
            
            progressManager.logStepDetail("🚀 启动并发OCR处理，最大并发数: {}", Math.min(total, 8));
            
            for (int i = 0; i < total; i++) {
                final int pageIndex = i;
                final int pageNum = i + 1;
                final byte[] pngBytes = pngPages.get(i);
                
                Future<Void> future = executor.submit(() -> {
                    try {
                        progressManager.logStepDetail("正在识别第{}页...", pageNum);
                        
                        // 先获取图片尺寸（用于坐标转换）
                        int imgW = 0, imgH = 0;
                        try {
                            ByteArrayInputStream bais = new ByteArrayInputStream(pngBytes);
                            BufferedImage image = ImageIO.read(bais);
                            if (image != null) {
                                imgW = image.getWidth();
                                imgH = image.getHeight();
                                progressManager.logStepDetail("第{}页图片尺寸: {}x{}", pageNum, imgW, imgH);
                            }
                        } catch (Exception e) {
                            progressManager.logStepDetail("获取第{}页图片尺寸失败: {}", pageNum, e.getMessage());
                            // 使用默认尺寸
                            imgW = 1000;
                            imgH = 1400;
                        }
                        
                        // 调用第三方OCR服务（传递图片尺寸用于坐标转换）
                        List<CharBox> charBoxes = thirdPartyOcrService.performOCR(pngBytes, "image/png", pageNum, imgW, imgH);
                        
                        // 将CharBox转换为LayoutItem格式
                        List<TextExtractionUtil.LayoutItem> items = convertCharBoxesToLayoutItems(charBoxes);
                        
                        // 创建页面布局
                        TextExtractionUtil.PageLayout pageLayout = new TextExtractionUtil.PageLayout(pageNum, items, imgW, imgH);
                        ordered[pageIndex] = pageLayout;
                        
                        // 保存OCR结果为JSON（与DotsOCR格式兼容）
                        if (shouldSaveImages) {
                            saveThirdPartyOcrResult(pdf, pageNum, items, progressManager);
                        }
                        
                        // 更新进度
                        if (task != null && mode != null) {
                            if ("old".equals(mode)) {
                                task.setCurrentPageOld(pageNum);
                                task.setCompletedPagesOld(pageNum);
                            } else if ("new".equals(mode)) {
                                task.setCurrentPageNew(pageNum);
                                task.setCompletedPagesNew(pageNum);
                            }
                        }
                        
                        progressManager.logStepDetail("第{}页识别完成，识别到 {} 个文本块", pageNum, charBoxes.size());
                        
                    } catch (Exception e) {
                        progressManager.logStepDetail("第{}页识别失败: {}", pageNum, e.getMessage());
                        
                        // 创建空页面布局
                        ordered[pageIndex] = createEmptyPageLayout(pageNum);
                        String errorMsg = e.getMessage();
                        if (errorMsg != null && errorMsg.contains("timeout")) {
                            failedPages.add(documentName + "-第" + pageNum + "页: 超时错误");
                        } else {
                            failedPages.add(documentName + "-第" + pageNum + "页: " + errorMsg);
                        }
                        
                        // 即使失败也要更新页面进度
                        if (task != null && mode != null) {
                            if ("old".equals(mode)) {
                                task.setCurrentPageOld(pageNum);
                                task.setCompletedPagesOld(pageNum);
                            } else if ("new".equals(mode)) {
                                task.setCurrentPageNew(pageNum);
                                task.setCompletedPagesNew(pageNum);
                            }
                        }
                    }
                    return null;
                });
                
                futures.add(future);
            }
            
            // 等待所有任务完成
            for (Future<Void> future : futures) {
                try {
                    future.get(); // 等待任务完成
                } catch (Exception e) {
                    progressManager.logStepDetail("页面处理异常: {}", e.getMessage());
                }
            }
            
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    progressManager.logStepDetail("OCR处理超时，强制关闭线程池");
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }

            progressManager.logStepDetail("第三方OCR识别完成，共处理 {} 页", total);

            // 计算页面高度信息用于页眉页脚检测
            double[] pageHeights = calculatePageHeights(pdf, progressManager);

            // 使用现有的文本解析逻辑
            List<CharBox> out = TextExtractionUtil.parseTextAndPositionsFromResults(ordered,
                    TextExtractionUtil.ExtractionStrategy.SEQUENTIAL, options.isIgnoreHeaderFooter(),
                    options.getHeaderHeightPercent(), options.getFooterHeightPercent(), pageHeights);

            // 保存提取的纯文本
            try {
                String extractedWithPages = TextExtractionUtil.extractTextWithPageMarkers(out);
                String extractedNoPages = TextExtractionUtil.extractText(out);

                String txtOut = pdf.toAbsolutePath().toString() + ".extracted.thirdparty.txt";
                String txtOutCompare = pdf.toAbsolutePath().toString() + ".extracted.thirdparty.compare.txt";

                Files.write(Path.of(txtOut), extractedWithPages.getBytes(StandardCharsets.UTF_8));
                Files.write(Path.of(txtOutCompare), extractedNoPages.getBytes(StandardCharsets.UTF_8));

                progressManager.logStepDetail("第三方OCR提取文本已保存: {}", txtOut);
            } catch (Exception e) {
                progressManager.logStepDetail("保存第三方OCR提取文本失败: {}", e.getMessage());
            }

            int totalPages = ordered.length;
            return new RecognitionResult(out, failedPages, totalPages);
            
        } catch (Exception e) {
            progressManager.logStepDetail("第三方OCR识别过程发生异常: {}", e.getMessage());
            throw new RuntimeException("第三方OCR识别失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将CharBox列表转换为LayoutItem列表
     * 将分散的字符CharBox重新组织为文本块LayoutItem
     */
    private List<TextExtractionUtil.LayoutItem> convertCharBoxesToLayoutItems(List<CharBox> charBoxes) {
        List<TextExtractionUtil.LayoutItem> items = new ArrayList<>();
        
        if (charBoxes.isEmpty()) {
            return items;
        }
        
        // 将连续的同类别字符合并为文本块
        StringBuilder currentText = new StringBuilder();
        String currentCategory = null;
        double[] currentBbox = null;
        
        for (CharBox charBox : charBoxes) {
            // 如果类别变化或者是新的开始，创建新的LayoutItem
            if (currentCategory == null || !currentCategory.equals(charBox.category)) {
                // 保存上一个LayoutItem
                if (currentCategory != null && currentText.length() > 0 && currentBbox != null) {
                    TextExtractionUtil.LayoutItem item = new TextExtractionUtil.LayoutItem(
                            currentBbox.clone(), currentCategory, currentText.toString());
                    items.add(item);
                }
                
                // 开始新的LayoutItem
                currentCategory = charBox.category;
                currentText = new StringBuilder();
                currentBbox = charBox.bbox.clone();
            }
            
            // 添加字符到当前文本块
            currentText.append(charBox.ch);
            
            // 扩展边界框
            if (currentBbox != null) {
                currentBbox[0] = Math.min(currentBbox[0], charBox.bbox[0]); // min x
                currentBbox[1] = Math.min(currentBbox[1], charBox.bbox[1]); // min y
                currentBbox[2] = Math.max(currentBbox[2], charBox.bbox[2]); // max x
                currentBbox[3] = Math.max(currentBbox[3], charBox.bbox[3]); // max y
            }
        }
        
        // 保存最后一个LayoutItem
        if (currentCategory != null && currentText.length() > 0 && currentBbox != null) {
            TextExtractionUtil.LayoutItem item = new TextExtractionUtil.LayoutItem(
                    currentBbox.clone(), currentCategory, currentText.toString());
            items.add(item);
        }
        
        return items;
    }

    /**
     * 保存第三方OCR结果为JSON格式（与DotsOCR格式兼容）
     */
    private void saveThirdPartyOcrResult(Path pdfPath, int page, List<TextExtractionUtil.LayoutItem> items, CompareTaskProgressManager progressManager) {
        try {
            // 构建与DotsOCR兼容的JSON格式
            List<Map<String, Object>> jsonItems = new ArrayList<>();
            for (TextExtractionUtil.LayoutItem item : items) {
                if (item.bbox != null && item.text != null && !item.text.trim().isEmpty()) {
                    Map<String, Object> jsonItem = new HashMap<>();
                    jsonItem.put("bbox", item.bbox);
                    jsonItem.put("category", item.category);
                    jsonItem.put("text", item.text);
                    jsonItems.add(jsonItem);
                }
            }
            
            String pageJsonPath = pdfPath.toAbsolutePath().toString() + ".page-" + page + ".ocr.json";
            Files.write(Path.of(pageJsonPath), M.writerWithDefaultPrettyPrinter().writeValueAsBytes(jsonItems));
            
            if (progressManager != null) {
                progressManager.logStepDetail("第三方OCR结果已保存: page-{}.ocr.json", page);
            }
        } catch (Exception e) {
            if (progressManager != null) {
                progressManager.logStepDetail("保存第三方OCR结果失败 (page {}): {}", page, e.getMessage());
            }
        }
    }


    private String joinWithLineBreaks(List<CharBox> cs) {
		if (cs.isEmpty())
			return "";

        StringBuilder sb = new StringBuilder();

        for (CharBox c : cs) {
            if (c.bbox != null) {
                sb.append(c.ch);
            }
        }
        return sb.toString();
    }

    private List<DiffBlock> mergeBlocksByBbox(List<DiffBlock> blocks) {
		if (blocks.isEmpty())
			return blocks;

        // 1. 应用bbox相同合并算法
        List<DiffBlock> result1 = mergeSameBboxBlocks(blocks);
        
        // 2. 应用连续新增/删除合并算法
		// List<DiffBlock> result2 = mergeConsecutiveInsertDelete(result1);

        // 最终结果中去掉所有 IGNORED 块
        List<DiffBlock> finalResult = new ArrayList<>();
        for (DiffBlock b : result1) {
            if (b != null && b.type != DiffBlock.DiffType.IGNORED) {
                finalResult.add(b);
            }
        }

        // 统计IGNORED块数量
        long ignoredCount = blocks.stream().filter(b -> b != null && b.type == DiffBlock.DiffType.IGNORED).count();
        
        logger.info("📊 差异块合并统计: 合并前={}, bbox合并后={}, 连续合并后={}, 去除IGNORED后={}, 实际合并的块数={}, 原始IGNORED块数量={}", 
                   blocks.size(), result1.size(), result1.size(), finalResult.size(), 
                   (blocks.size() - finalResult.size()), ignoredCount);

        return finalResult;
    }

	// 提取：依据DiffBlock的bbox在对应序列上拼接文本
	private String extractTextByBboxes(DiffBlock b, List<CharBox> seq, boolean useOld) {
		try {
			List<double[]> boxes = useOld ? b.oldBboxes : b.newBboxes;
			List<Integer> pages = useOld ? b.pageA : b.pageB;
			if (boxes == null || boxes.isEmpty() || seq == null || seq.isEmpty())
				return "";
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < boxes.size(); i++) {
				double[] box = boxes.get(i);
				int page = b.page > 0 ? b.page : 1;
				if (pages != null && i < pages.size() && pages.get(i) != null && pages.get(i) > 0) {
					page = pages.get(i);
				}
				for (CharBox c : seq) {
					if (c == null || c.bbox == null)
						continue;
					if (c.page != page)
						continue;
					double[] cb = c.bbox;
					boolean inside = cb[0] >= box[0] && cb[1] >= box[1] && cb[2] <= box[2] && cb[3] <= box[3];
					if (inside)
						sb.append(c.ch);
				}
				if (i < boxes.size() - 1)
					sb.append(' ');
			}
			return sb.toString();
		} catch (Exception e) {
			return "";
		}
	}

	/**
	 * 合并具有相同bbox的块（新优化逻辑：基于连通图的传递性合并）
	 */
	private List<DiffBlock> mergeSameBboxBlocks(List<DiffBlock> blocks) {
		List<DiffBlock> result = new ArrayList<>();
		boolean[] processed = new boolean[blocks.size()];

		// 首先处理IGNORED块和无bbox的块
		for (int i = 0; i < blocks.size(); i++) {
			DiffBlock block = blocks.get(i);
			if (block == null) {
				processed[i] = true;
				continue;
			}

			// IGNORED块直接添加，不参与合并
			if (block.type == DiffBlock.DiffType.IGNORED) {
				result.add(block);
				processed[i] = true;
				continue;
			}

			// 没有bbox的块直接添加
			if (getBboxCount(block) == 0) {
				result.add(block);
				processed[i] = true;
				continue;
			}
		}

		// 对有bbox的块进行传递性合并
		while (true) {
			// 寻找下一个未处理的块
			int startIndex = -1;
			for (int i = 0; i < blocks.size(); i++) {
				if (!processed[i]) {
					startIndex = i;
					break;
				}
			}
			
			if (startIndex == -1) {
				break; // 所有块都已处理
			}

			// 使用BFS找到所有传递连通的块
			List<Integer> connectedGroup = findConnectedBlocks(blocks, processed, startIndex);
			
			if (connectedGroup.size() == 1) {
				// 只有一个块，直接添加
				result.add(blocks.get(connectedGroup.get(0)));
			} else {
				// 多个连通的块，进行合并
				List<DiffBlock> groupBlocks = new ArrayList<>();
				for (int index : connectedGroup) {
					groupBlocks.add(blocks.get(index));
				}
				//System.out.println("[传递性合并] 找到连通组，包含块: " + connectedGroup);
				result.add(mergeSameBboxGroup(groupBlocks));
			}

			// 标记这些块为已处理
			for (int index : connectedGroup) {
				processed[index] = true;
			}
		}

		return result;
	}

	/**
	 * 使用BFS找到所有传递连通的块
	 */
	private List<Integer> findConnectedBlocks(List<DiffBlock> blocks, boolean[] processed, int startIndex) {
		List<Integer> connected = new ArrayList<>();
		Queue<Integer> queue = new LinkedList<>();
		boolean[] visited = new boolean[blocks.size()];
		
		queue.offer(startIndex);
		visited[startIndex] = true;
		
		while (!queue.isEmpty()) {
			int current = queue.poll();
			connected.add(current);
			
			// 检查所有其他未处理且未访问的块
			for (int i = 0; i < blocks.size(); i++) {
				if (processed[i] || visited[i] || i == current) {
					continue;
				}
				
				DiffBlock currentBlock = blocks.get(current);
				DiffBlock otherBlock = blocks.get(i);
				
				// 跳过IGNORED块和无bbox的块
				if (otherBlock.type == DiffBlock.DiffType.IGNORED || getBboxCount(otherBlock) == 0) {
					continue;
				}
				
				// 检查是否连通（相同类型且有匹配bbox）
				if (currentBlock.type == otherBlock.type && 
					getBboxCount(otherBlock) > 0 && 
					hasMatchingBboxWithPage(currentBlock, otherBlock)) {
					
					//System.out.println("[传递性合并] 发现连通: 块#" + current + " 与 块#" + i);
					queue.offer(i);
					visited[i] = true;
				}
			}
		}
		
        return connected;
    }

	/**
	 * 合并全局diffRanges - 基于全局文本索引，直接合并不需要重新计算
	 * @param group 要合并的块列表
	 * @param isOldText true表示处理diffRangesA，false表示处理diffRangesB
	 * @return 合并后的TextRange列表（保持全局索引）
	 */
	private List<DiffBlock.TextRange> mergeGlobalDiffRanges(List<DiffBlock> group, boolean isOldText) {
		List<DiffBlock.TextRange> mergedRanges = new ArrayList<>();
		Set<String> rangeKeys = new HashSet<>(); // 用于去重
		
		for (DiffBlock block : group) {
			List<DiffBlock.TextRange> ranges = isOldText ? block.diffRangesA : block.diffRangesB;
			if (ranges != null && !ranges.isEmpty()) {
				for (DiffBlock.TextRange range : ranges) {
					// 创建唯一键用于去重（基于起始位置、结束位置和类型）
					String key = range.start + ":" + range.end + ":" + range.type;
					if (!rangeKeys.contains(key)) {
						rangeKeys.add(key);
						// 直接使用全局索引，不需要转换
						mergedRanges.add(new DiffBlock.TextRange(range.start, range.end, range.type));
					}
				}
			}
		}
		
//		System.out.println("[全局DiffRanges合并] " + (isOldText ? "diffRangesA" : "diffRangesB") + 
//			" 合并前总数=" + group.stream().mapToInt(b -> {
//				List<DiffBlock.TextRange> r = isOldText ? b.diffRangesA : b.diffRangesB;
//				return r != null ? r.size() : 0;
//			}).sum() + ", 合并后数量=" + mergedRanges.size());
		
		return mergedRanges;
	}
	
	/**
	 * 获取最小的textStartIndex（全局文本中的开始位置）
	 * @param group 要合并的块列表  
	 * @param isOldText true表示获取textStartIndexA，false表示获取textStartIndexB
	 * @return 最小的textStartIndex（全局文本中的位置）
	 */
	private Integer getMinTextStartIndex(List<DiffBlock> group, boolean isOldText) {
		Integer minIndex = null;
		
		for (DiffBlock block : group) {
			Integer index = isOldText ? block.textStartIndexA : block.textStartIndexB;
			if (index != null) {
				if (minIndex == null || index < minIndex) {
					minIndex = index;
				}
			}
		}
		
//		System.out.println("[TextStartIndex合并] " + (isOldText ? "textStartIndexA" : "textStartIndexB") + 
//			" 最小值=" + minIndex + " (全局文本位置)");
		
		return minIndex;
	}
    
    /**
     * 将全局索引的diffRanges转换为相对索引供前端使用
     * @param globalRanges 基于全局文本索引的TextRange列表
     * @param textStartIndex 当前块的文本起始索引（全局位置）
     * @return 转换为相对索引的TextRange列表
     */
    private List<DiffBlock.TextRange> convertToRelativeDiffRanges(List<DiffBlock.TextRange> globalRanges, Integer textStartIndex) {
        if (globalRanges == null || globalRanges.isEmpty() || textStartIndex == null) {
            return new ArrayList<>();
        }
        
        List<DiffBlock.TextRange> relativeRanges = new ArrayList<>();
        
        for (DiffBlock.TextRange globalRange : globalRanges) {
            // 将全局索引转换为相对于当前块的索引
            int relativeStart = Math.max(0, globalRange.start - textStartIndex);
            int relativeEnd = Math.max(0, globalRange.end - textStartIndex);
            
            // 只有在范围有效时才添加
            if (relativeStart < relativeEnd) {
                relativeRanges.add(new DiffBlock.TextRange(relativeStart, relativeEnd, globalRange.type));
            }
        }
        
        return relativeRanges;
    }

    /**
     * 合并一组具有相同bbox的块
     */
    private DiffBlock mergeSameBboxGroup(List<DiffBlock> group) {
		if (group.isEmpty())
			return null;
		if (group.size() == 1)
			return group.get(0);
        
        DiffBlock first = group.get(0);
        DiffBlock merged = new DiffBlock();
        merged.type = first.type;
        merged.page = first.page;

		// 合并所有bbox（去重）和对应的页码、文本
		Set<String> mergedOldBboxKeys = new HashSet<>();
		Set<String> mergedNewBboxKeys = new HashSet<>();
        merged.oldBboxes = new ArrayList<>();
        merged.newBboxes = new ArrayList<>();
		merged.pageA = new ArrayList<>();
		merged.pageB = new ArrayList<>();
		merged.allTextA = new ArrayList<>();
		merged.allTextB = new ArrayList<>();
		
		
		// 文本内容合并
        StringBuilder oldTextBuilder = new StringBuilder();
        StringBuilder newTextBuilder = new StringBuilder();
        Set<String> seenOldSegments = new HashSet<>();
        Set<String> seenNewSegments = new HashSet<>();

		// 遍历所有块进行合并
        for (DiffBlock block : group) {
			// 合并oldBboxes、pageA、allTextA
			if (block.oldBboxes != null && block.pageA != null && block.allTextA != null) {
				for (int i = 0; i < block.oldBboxes.size() && i < block.pageA.size() && i < block.allTextA.size(); i++) {
					double[] bbox = block.oldBboxes.get(i);
					int page = block.pageA.get(i);
					String text = block.allTextA.get(i);
					
					// 创建唯一键：页码+bbox坐标
					String key = page + ":" + bbox[0] + "," + bbox[1] + "," + bbox[2] + "," + bbox[3];
					if (!mergedOldBboxKeys.contains(key)) {
						mergedOldBboxKeys.add(key);
						merged.oldBboxes.add(bbox);
						merged.pageA.add(page);
						merged.allTextA.add(text);
					}
				}
			}
			
			// 合并newBboxes、pageB、allTextB
			if (block.newBboxes != null && block.pageB != null && block.allTextB != null) {
				for (int i = 0; i < block.newBboxes.size() && i < block.pageB.size() && i < block.allTextB.size(); i++) {
					double[] bbox = block.newBboxes.get(i);
					int page = block.pageB.get(i);
					String text = block.allTextB.get(i);
					
					// 创建唯一键：页码+bbox坐标
					String key = page + ":" + bbox[0] + "," + bbox[1] + "," + bbox[2] + "," + bbox[3];
					if (!mergedNewBboxKeys.contains(key)) {
						mergedNewBboxKeys.add(key);
						merged.newBboxes.add(bbox);
						merged.pageB.add(page);
						merged.allTextB.add(text);
					}
				}
			}
			
			// 合并文本内容
            if (block.oldText != null && !block.oldText.trim().isEmpty()) {
                String seg = block.oldText.trim();
                if (!seenOldSegments.contains(seg)) {
					if (oldTextBuilder.length() > 0)
						oldTextBuilder.append(" ");
                    oldTextBuilder.append(seg);
                    seenOldSegments.add(seg);
                }
            }
            if (block.newText != null && !block.newText.trim().isEmpty()) {
                String segN = block.newText.trim();
                if (!seenNewSegments.contains(segN)) {
					if (newTextBuilder.length() > 0)
						newTextBuilder.append(" ");
                    newTextBuilder.append(segN);
                    seenNewSegments.add(segN);
                }
            }
            
            // 注意：diffRanges将在合并完成后重新计算，这里暂不处理
        }
        
        merged.oldText = oldTextBuilder.toString();
        merged.newText = newTextBuilder.toString();
		
		// 合并diffRanges - 直接合并所有块的全局索引范围
		merged.diffRangesA = mergeGlobalDiffRanges(group, true);
		merged.diffRangesB = mergeGlobalDiffRanges(group, false);
		
		// 设置textStartIndex - 取所有块中的最小值
		merged.textStartIndexA = getMinTextStartIndex(group, true);
		merged.textStartIndexB = getMinTextStartIndex(group, false);
		
		// 处理prevBboxes和对应的页码 - 根据操作类型选择排序靠前的DiffBlock
		DiffBlock firstBlock = group.get(0); // 排序靠前的block
		if ("ADDED".equals(merged.type.toString())) {
			// ADDED操作：prevOldBboxes以排序靠前的为准
			merged.prevOldBboxes = firstBlock.prevOldBboxes == null ? null : new ArrayList<>(firstBlock.prevOldBboxes);
			merged.prevNewBboxes = firstBlock.prevNewBboxes == null ? null : new ArrayList<>(firstBlock.prevNewBboxes);
			// 同时复制对应的页码信息
			merged.prevOldBboxPages = firstBlock.prevOldBboxPages == null ? null : new ArrayList<>(firstBlock.prevOldBboxPages);
			merged.prevNewBboxPages = firstBlock.prevNewBboxPages == null ? null : new ArrayList<>(firstBlock.prevNewBboxPages);
		} else if ("DELETED".equals(merged.type.toString())) {
			// DELETED操作：prevNewBboxes以排序靠前的为准
			merged.prevNewBboxes = firstBlock.prevNewBboxes == null ? null : new ArrayList<>(firstBlock.prevNewBboxes);
			merged.prevOldBboxes = firstBlock.prevOldBboxes == null ? null : new ArrayList<>(firstBlock.prevOldBboxes);
			// 同时复制对应的页码信息
			merged.prevNewBboxPages = firstBlock.prevNewBboxPages == null ? null : new ArrayList<>(firstBlock.prevNewBboxPages);
			merged.prevOldBboxPages = firstBlock.prevOldBboxPages == null ? null : new ArrayList<>(firstBlock.prevOldBboxPages);
		}
		
		// 合并nestedBlocks
		merged.nestedBlocks = new ArrayList<>();
		for (DiffBlock block : group) {
			if (block.nestedBlocks != null && !block.nestedBlocks.isEmpty()) {
				merged.nestedBlocks.addAll(block.nestedBlocks);
			}
		}

//		System.out.println("=== 传递性合并策略：基于连通图的智能合并 ===");
//		System.out.println("合并 " + merged.type + " 类型块: " + group.size() + "个块 -> 1个块");
//		System.out.println("合并后oldBboxes: " + (merged.oldBboxes != null ? merged.oldBboxes.size() : 0) + "个");
//		System.out.println("合并后newBboxes: " + (merged.newBboxes != null ? merged.newBboxes.size() : 0) + "个");
//		System.out.println("合并后allTextA: " + (merged.allTextA != null ? merged.allTextA.size() : 0) + "条");
//		System.out.println("合并后allTextB: " + (merged.allTextB != null ? merged.allTextB.size() : 0) + "条");
//		System.out.println("合并后oldText: " + merged.oldText);
//		System.out.println("合并后newText: " + merged.newText);
//		System.out.println("合并后nestedBlocks: " + (merged.nestedBlocks != null ? merged.nestedBlocks.size() : 0) + "个");
//		System.out.println("=== 传递性合并完成 ===");

        return merged;
    }

	/**
	 * 获取一个差异块的bbox总数量
	 */
	private int getBboxCount(DiffBlock block) {
		int count = 0;
		if (block == null)
			return 0;
		if (block.oldBboxes != null)
			count += block.oldBboxes.size();
		if (block.newBboxes != null)
			count += block.newBboxes.size();
		return count;
    }

	/**
	 * 检查两个DiffBlock是否有匹配的bbox（同一页面上的相同bbox）
	 */
	private boolean hasMatchingBboxWithPage(DiffBlock a, DiffBlock b) {
		if (a.type != b.type) {
			return false;
		}
		
		if ("ADDED".equals(a.type.toString())) {
			// ADDED操作：比较newBbox和对应的pageB
			boolean result = hasMatchingBboxes(a.newBboxes, a.pageB, b.newBboxes, b.pageB);
			return result;
		} else if ("DELETED".equals(a.type.toString())) {
			// DELETED操作：比较oldBbox和对应的pageA
			boolean result = hasMatchingBboxes(a.oldBboxes, a.pageA, b.oldBboxes, b.pageA);
			return result;
		}
		
		return false;
	}
	
	/**
	 * 检查两组bbox是否有匹配（相同bbox在相同页面上）
	 */
	private boolean hasMatchingBboxes(List<double[]> bboxes1, List<Integer> pages1, 
			List<double[]> bboxes2, List<Integer> pages2) {
		if (bboxes1 == null || bboxes2 == null || pages1 == null || pages2 == null) {
			return false;
		}
		
		// 检查每个bbox1是否在bbox2中有匹配（相同bbox且在相同页面）
		for (int i = 0; i < bboxes1.size() && i < pages1.size(); i++) {
			double[] bbox1 = bboxes1.get(i);
			int page1 = pages1.get(i);
			
			for (int j = 0; j < bboxes2.size() && j < pages2.size(); j++) {
				double[] bbox2 = bboxes2.get(j);
				int page2 = pages2.get(j);
				
				boolean pageMatch = page1 == page2;
				boolean bboxMatch = bboxEquals(bbox1, bbox2);
				
				
				// 如果找到相同的bbox在相同页面上，就认为匹配
				if (pageMatch && bboxMatch) {
					return true;
				}
			}
		}
		
		return false;
	}

    private boolean bboxEquals(double[] a, double[] b) {
		if (a == null || b == null || a.length < 4 || b.length < 4)
			return false;
        final double EPS = 1e-3; // 容差
		return Math.abs(a[0] - b[0]) < EPS && Math.abs(a[1] - b[1]) < EPS && Math.abs(a[2] - b[2]) < EPS
            && Math.abs(a[3] - b[3]) < EPS;
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
		
        for (int i = 0; i < seq.size(); i++) {
            CharBox c = seq.get(i);
            if (c.bbox != null) {
                base.append(c.ch);
                idxMap.add(i);
            }
        }

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
	 * 
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
	 * 
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

		return String.format("%d_%.1f_%.1f_%.1f_%.1f_%s", rect.pageIndex0, x1, y1, x2, y2, rect.op.toString());
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
		DotsOcrClient client = DotsOcrClient.builder().baseUrl(gpuOcrConfig.getOcrBaseUrl())
				.defaultModel(gpuOcrConfig.getOcrModel()).build();

        try (PDDocument doc = PDDocument.load(pdf.toFile())) {
			int pageCount = doc.getNumberOfPages();
			// 使用固定DPI计算页面尺寸
			int dynamicDpi = gpuOcrConfig.getRenderDpi();
			System.out.println("计算页面尺寸使用固定DPI: " + dynamicDpi + " (页数: " + pageCount + ")");

            PDFRenderer r = new PDFRenderer(doc);
            int n = doc.getNumberOfPages();
            int[] ws = new int[n];
            int[] hs = new int[n];
            for (int i = 0; i < n; i++) {
				BufferedImage img = r.renderImageWithDPI(i, dynamicDpi);
                ws[i] = img.getWidth();
                hs[i] = img.getHeight();
            }
            return new PageImageSizeProvider(n, ws, hs);
        }
    }

    /**
     * 将DiffBlock列表转换为前端期望的Map格式（保留原始图像坐标）
     */
	private List<Map<String, Object>> convertDiffBlocksToMapFormat(List<DiffBlock> diffBlocks, boolean isDebugMode, List<CharBox> seqA, List<CharBox> seqB) {
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

			// 调试：按bbox提取文本，并回传对比字段，便于前端定位问题（仅在Debug模式下添加）
			if (isDebugMode && seqA != null && seqB != null) {
				try {
					String byOld = extractTextByBboxes(block, seqA, true);
					String byNew = extractTextByBboxes(block, seqB, false);
					if (byOld != null && !byOld.isEmpty()) {
						diffMap.put("oldTextByBbox", byOld);
					}
					if (byNew != null && !byNew.isEmpty()) {
						diffMap.put("newTextByBbox", byNew);
					}
				} catch (Exception ignore) {}
			}

            // 添加页面信息
            diffMap.put("page", block.page);
            
            // 页码处理：对于INSERT/DELETE操作，需要特殊处理pageA/pageB
            if ("INSERT".equals(operation)) {
                // INSERT操作：pageA应该基于prevOldBboxPages的页码，pageB基于新增内容的页码
                if (block.pageA != null && !block.pageA.isEmpty()) {
                    diffMap.put("pageA", java.util.Collections.min(block.pageA));
                } else if (block.prevOldBboxPages != null && !block.prevOldBboxPages.isEmpty()) {
                    // 使用prevOldBboxPages的页码
                    diffMap.put("pageA", java.util.Collections.min(block.prevOldBboxPages));
                } else {
                    diffMap.put("pageA", block.page);
                }
                if (block.pageB != null && !block.pageB.isEmpty()) {
                    diffMap.put("pageB", java.util.Collections.min(block.pageB));
                } else {
                    diffMap.put("pageB", block.page);
                }
            } else if ("DELETE".equals(operation)) {
                // DELETE操作：pageA基于删除内容的页码，pageB应该基于prevNewBboxPages的页码
                if (block.pageA != null && !block.pageA.isEmpty()) {
                    diffMap.put("pageA", java.util.Collections.min(block.pageA));
                } else {
                    diffMap.put("pageA", block.page);
                }
                if (block.pageB != null && !block.pageB.isEmpty()) {
                    diffMap.put("pageB", java.util.Collections.min(block.pageB));
                } else if (block.prevNewBboxPages != null && !block.prevNewBboxPages.isEmpty()) {
                    // 使用prevNewBboxPages的页码
                    diffMap.put("pageB", java.util.Collections.min(block.prevNewBboxPages));
                } else {
                    diffMap.put("pageB", block.page);
                }
            } else {
                // REPLACE等其他操作：使用原来的逻辑
                if (block.pageA != null && !block.pageA.isEmpty()) {
                    diffMap.put("pageA", java.util.Collections.min(block.pageA));
                } else {
                    diffMap.put("pageA", block.page);
                }
                if (block.pageB != null && !block.pageB.isEmpty()) {
                    diffMap.put("pageB", java.util.Collections.min(block.pageB));
                } else {
                    diffMap.put("pageB", block.page);
                }
            }
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
            
            // 转换全局索引的diffRanges为相对索引供前端使用
            diffMap.put("diffRangesA", convertToRelativeDiffRanges(block.diffRangesA, block.textStartIndexA));
            diffMap.put("diffRangesB", convertToRelativeDiffRanges(block.diffRangesB, block.textStartIndexB));

            mapResult.add(diffMap);
        }

        return mapResult;
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

    // 移除getPdfPageHeight方法，不再需要PDF页面高度

    // 移除getPdfPageWidth方法，不再需要PDF页面宽度

    private Path getFrontendResultJsonPath(String taskId) {
        // 基于系统配置的上传根目录保存结果文件
        String uploadRootPath = zxcmConfig.getFileUpload().getRootPath();
        Path base = Paths.get(uploadRootPath, "compare-pro", "results");
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
		result = result.replace("\\n", "\\n").replace("\\t", "\\t").replace("\\r", "\\r");
        
        return result;
    }

    /**
	 * 归一化模型输出的JSON： - 去除```json/```包裹 - 去掉Windows换行中的回车
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
			if (idx >= 0)
				s = s.substring(0, idx);
        }
        // normalize line endings
        s = s.replace("\r\n", "\n");
        // strip BOM and zero-width
		if (!s.isEmpty() && s.charAt(0) == '\uFEFF')
			s = s.substring(1);
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
			if (escaped) {
				escaped = false;
				continue;
			}
			if (c == '\\') {
				escaped = true;
				continue;
			}
			if (c == '"') {
				inString = !inString;
				continue;
			}
			if (inString)
				continue;
			if (c == '{')
				depth++;
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
		if (objects.isEmpty())
			return "[]";
        String joined = String.join(",", objects);
        return "[" + joined + "]";
    }

    private boolean isValidLayoutObject(String json) {
        try {
            JsonNode node = M.readTree(json);
			if (!node.isObject())
				return false;
            JsonNode bbox = node.get("bbox");
			if (bbox == null || !bbox.isArray() || bbox.size() != 4)
				return false;
			for (int i = 0; i < 4; i++)
				if (!bbox.get(i).isNumber())
					return false;
            JsonNode cat = node.get("category");
			if (cat == null || !cat.isTextual())
				return false;
            String category = cat.asText();
			java.util.Set<String> allow = new java.util.HashSet<>(
					java.util.Arrays.asList("Caption", "Footnote", "Formula", "List-item", "Page-footer", "Page-header",
							"Picture", "Section-header", "Table", "Text", "Title"));
			if (!allow.contains(category))
				return false;
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

	/**
	 * 从保存的图片文件中读取图片高度
	 * @param pdfPath PDF文件路径（用于推断任务ID）
	 * @param pageNumber 页码（从1开始）
	 * @return 图片高度，如果读取失败返回0
	 */
	private double getImageHeightFromSavedFile(Path pdfPath, int pageNumber) {
		try {
			// 从PDF路径推断任务ID和文档类型
			String taskId = extractTaskIdFromPath(pdfPath);
			String mode = extractModeFromPath(pdfPath);
			
			if (taskId == null || mode == null) {
				return 0;
			}
			
			// 构建图片文件路径
			String uploadRootPath = zxcmConfig.getFileUpload().getRootPath();
			Path imagePath = Paths.get(uploadRootPath, "compare-pro", "tasks", taskId, "images", mode, "page-" + pageNumber + ".png");
			
			if (!Files.exists(imagePath)) {
				System.out.println("图片文件不存在: " + imagePath);
				return 0;
			}
			
			BufferedImage image = ImageIO.read(imagePath.toFile());
			if (image != null) {
				return image.getHeight();
			}
		} catch (Exception e) {
			System.err.println("读取图片高度失败: " + e.getMessage());
		}
		return 0;
	}

	/**
	 * 从PDF路径中提取任务ID
	 * @param pdfPath PDF文件路径
	 * @return 任务ID，如果提取失败返回null
	 */
	private String extractTaskIdFromPath(Path pdfPath) {
		try {
			// PDF路径通常是: .../tasks/{taskId}/old_xxx.pdf 或 .../tasks/{taskId}/new_xxx.pdf
			String pathStr = pdfPath.toAbsolutePath().toString();
			String[] parts = pathStr.split("tasks");
			if (parts.length >= 2) {
				String afterTasks = parts[1];
				if (afterTasks.startsWith("/") || afterTasks.startsWith("\\")) {
					afterTasks = afterTasks.substring(1);
				}
				String[] pathParts = afterTasks.split("[/\\\\]");
				if (pathParts.length > 0) {
					return pathParts[0]; // 任务ID
				}
			}
		} catch (Exception e) {
			System.err.println("提取任务ID失败: " + e.getMessage());
		}
		return null;
	}

	/**
	 * 从PDF路径中提取文档模式（old或new）
	 * @param pdfPath PDF文件路径
	 * @return 文档模式，如果提取失败返回null
	 */
	private String extractModeFromPath(Path pdfPath) {
		try {
			String fileName = pdfPath.getFileName().toString().toLowerCase();
			if (fileName.startsWith("old")) {
				return "old";
			} else if (fileName.startsWith("new")) {
				return "new";
			}
		} catch (Exception e) {
			System.err.println("提取文档模式失败: " + e.getMessage());
		}
		return null;
	}

	/**
	 * 导出比对报告
	 */
	public byte[] exportReport(ExportRequest request) throws Exception {
		String taskId = request.getTaskId();
		List<String> formats = request.getFormats();
		
		// 获取任务数据
		CompareResult result = getCompareResult(taskId);
		if (result == null) {
			throw new RuntimeException("任务结果不存在: " + taskId);
		}

		// 根据格式数量决定返回类型
		if (formats.size() == 1) {
			String format = formats.get(0);
			if ("html".equals(format)) {
				return generateHTMLReport(result, request);
			} else if ("doc".equals(format)) {
				return generateDOCXReport(result, request);
			} else {
				throw new IllegalArgumentException("不支持的导出格式: " + format);
			}
		} else {
			// 多种格式，返回ZIP包含所有格式
			return generateMultiFormatReport(result, request);
		}
	}

	/**
	 * 生成HTML格式报告（ZIP包）- 基于export项目模板的完整实现
	 * 实现和 embed-json-data.cjs 一样的JSON内嵌逻辑和自动化打包功能
	 */
	private byte[] generateHTMLReport(CompareResult result, ExportRequest request) throws Exception {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			String taskId = request.getTaskId();
			long startTime = System.currentTimeMillis();
			
			logger.info("🔄 Java后端 - 开始HTML自动化导出流程");
			logger.info("📋 任务信息: ID={}, 原文档={}, 新文档={}", taskId, result.getOldFileName(), result.getNewFileName());
			
			// 1. 获取文件根目录和模板路径
			String uploadRootPath = zxcmConfig.getFileUpload().getRootPath();
			String templatePath = resolveTemplatePath(uploadRootPath);
			String tempDirPath = resolveTempDirPath(uploadRootPath, taskId);
			
			logger.info("📁 路径配置:");
			logger.info("  - 文件根目录: {}", uploadRootPath);
			logger.info("  - HTML模板: {}", templatePath);
			logger.info("  - 临时目录: {}", tempDirPath);
			
			Path tempDir = Paths.get(tempDirPath);
			Files.createDirectories(tempDir);
			
			try {
				// 2. 准备JSON数据（和 embed-json-data.cjs 相同的数据结构）
				logger.info("📊 准备JSON数据...");
				String compareResultJson = generateCompareResultJsonForExport(result);
				// 使用增强的任务状态生成方法（根据实际情况生成完整数据）
				String taskStatusJson = generateTaskStatusJsonFromCompareResult(result, request, compareResultJson);
				
				// 输出数据统计（和前端脚本一样的格式）
				logDataStatistics(result, taskStatusJson, compareResultJson);
				
				// 3. 读取HTML模板文件
				logger.info("📄 读取HTML模板文件: {}", templatePath);
				Path templateFile = Paths.get(templatePath);
				
				if (!Files.exists(templateFile)) {
					throw new RuntimeException("HTML模板文件不存在: " + templatePath + 
						"，请确保模板文件存在于: {文件根目录}/templates/export/index.html");
				}
				
				String htmlTemplate = Files.readString(templateFile, StandardCharsets.UTF_8);
				logger.info("✅ 读取HTML模板文件成功 (大小: {} KB)", Files.size(templateFile) / 1024);
				
				// 4. 执行JSON数据内嵌（和 embed-json-data.cjs 完全相同的逻辑）
				logger.info("🔧 执行JSON数据内嵌...");
				String finalHtml = embedJsonDataIntoHtml(htmlTemplate, taskStatusJson, compareResultJson);
				logger.info("✅ JSON数据内嵌完成 (内嵌数据大小: {} KB)", 
					(taskStatusJson.length() + compareResultJson.length()) / 1024);
				
				// 5. 自动化复制和替换图片文件
				logger.info("🖼️ 自动化处理图片文件...");
				int copiedImages = copyAndReplaceTaskImages(taskId, tempDir);
				logger.info("✅ 图片文件处理完成 (复制了 {} 个图片文件)", copiedImages);
				
				// 6. 创建自动化ZIP包
				logger.info("📦 创建自动化ZIP包...");
				java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(baos);
				
				// 添加内嵌后的HTML文件
				zos.putNextEntry(new java.util.zip.ZipEntry("index.html"));
				zos.write(finalHtml.getBytes(StandardCharsets.UTF_8));
				zos.closeEntry();
				logger.info("  ✓ 添加HTML文件到ZIP (大小: {} KB)", finalHtml.getBytes(StandardCharsets.UTF_8).length / 1024);
				
				// 添加图片文件到ZIP（保持和前端一致的目录结构）
				int zipImages = addTempImagesToZip(zos, tempDir);
				logger.info("  ✓ 添加图片文件到ZIP (数量: {})", zipImages);
				
				zos.close();
				
				long duration = System.currentTimeMillis() - startTime;
				logger.info("🎉 Java后端 - HTML自动化导出完成!");
				logger.info("📈 导出统计: 耗时 {}ms, ZIP大小 {} KB", duration, baos.size() / 1024);
				
				return baos.toByteArray();
				
			} finally {
				// 7. 清理临时文件夹
				deleteTempDirectory(tempDir);
				logger.info("🧹 临时文件夹已清理");
			}
		}
	}
	
	/**
	 * 解析HTML模板文件路径（基于文件根目录）
	 */
	private String resolveTemplatePath(String uploadRootPath) {
		Path templatePath = Paths.get(uploadRootPath, "templates", "export", "index.html");
		return templatePath.toAbsolutePath().toString();
	}
	
	
	/**
	 * 解析临时目录路径（基于文件根目录）
	 */
	private String resolveTempDirPath(String uploadRootPath, String taskId) {
		Path tempPath = Paths.get(uploadRootPath, "html-export-temp", taskId + "-" + System.currentTimeMillis());
		return tempPath.toAbsolutePath().toString();
	}
	
	/**
	 * 输出数据统计信息（和 embed-json-data.cjs 相同的格式）
	 */
	private void logDataStatistics(CompareResult result, String taskStatusJson, String compareResultJson) {
		logger.info("📊 数据统计:");
		logger.info("  - 任务状态: {} vs {}", result.getOldFileName(), result.getNewFileName());
		
		// 解析比对结果以获取页面总数
		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode compareData = mapper.readTree(compareResultJson);
			int oldPages = compareData.path("oldImageInfo").path("totalPages").asInt(0);
			int newPages = compareData.path("newImageInfo").path("totalPages").asInt(0);
			int differences = compareData.path("differences").size();
			int failedPages = compareData.path("failedPagesCount").asInt(0);
			
			logger.info("  - 页面总数: 原文档 {} 页, 新文档 {} 页", oldPages, newPages);
			logger.info("  - 差异数量: {} 个", differences);
			logger.info("  - 失败页面: {} 个", failedPages);
			logger.info("  - JSON大小: 任务状态 {} KB, 比对结果 {} KB", 
				taskStatusJson.length() / 1024, compareResultJson.length() / 1024);
		} catch (Exception e) {
			logger.warn("解析数据统计时出错: {}", e.getMessage());
		}
	}
	
	/**
	 * 将JSON数据内嵌到HTML中（和 embed-json-data.cjs 完全相同的逻辑）
	 */
	private String embedJsonDataIntoHtml(String htmlTemplate, String taskStatusJson, String compareResultJson) {
		// 创建内嵌脚本（和前端脚本完全相同的格式）
		String inlineScript = String.format(
			"<script>\n" +
			"// 内联数据，避免file://协议的CORS问题\n" +
			"// 由 Java后端自动生成，逻辑等同于 export/embed-json-data.cjs\n" +
			"window.TASK_STATUS_DATA = %s;\n" +
			"window.COMPARE_RESULT_DATA = %s;\n" +
			"console.log('内嵌数据已加载:', { taskStatus: window.TASK_STATUS_DATA, compareResult: window.COMPARE_RESULT_DATA });\n" +
			"</script>",
			taskStatusJson,
			compareResultJson
		);
		
		// 检查是否已经包含内嵌数据（和前端脚本相同的逻辑）
		if (htmlTemplate.contains("window.TASK_STATUS_DATA")) {
			logger.info("⚠️ HTML文件已包含内嵌数据，将替换现有数据");
			// 移除现有的内嵌脚本
			htmlTemplate = htmlTemplate.replaceAll("<script>[\\s\\S]*?window\\.TASK_STATUS_DATA[\\s\\S]*?</script>", "");
		}
		
		// 将脚本插入到</head>标签之前（和前端脚本相同的逻辑）
		return htmlTemplate.replace("</head>", inlineScript + "\n</head>");
	}

	/**
	 * 生成DOCX格式报告
	 */
	private byte[] generateDOCXReport(CompareResult result, ExportRequest request) throws Exception {
		logger.info("📄 开始生成DOCX格式比对报告");
		
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
			 org.apache.poi.xwpf.usermodel.XWPFDocument document = new org.apache.poi.xwpf.usermodel.XWPFDocument()) {
			
			// 1. 添加标题
			org.apache.poi.xwpf.usermodel.XWPFParagraph titlePara = document.createParagraph();
			titlePara.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER);
			org.apache.poi.xwpf.usermodel.XWPFRun titleRun = titlePara.createRun();
			titleRun.setText("合同比对报告");
			titleRun.setBold(true);
			titleRun.setFontSize(18);
			titleRun.setFontFamily("宋体");
			
			// 2. 添加基本信息部分
			addBasicInfo(document, result, request);
			
			// 3. 添加差异详细信息标题
			org.apache.poi.xwpf.usermodel.XWPFParagraph detailTitlePara = document.createParagraph();
			org.apache.poi.xwpf.usermodel.XWPFRun detailTitleRun = detailTitlePara.createRun();
			detailTitleRun.setText("差异详细信息");
			detailTitleRun.setBold(true);
			detailTitleRun.setFontSize(14);
			detailTitleRun.setFontFamily("宋体");
			
			// 4. 添加差异详细表格
			addDifferenceTable(document, result, request);
			
			// 5. 写入到字节数组
			document.write(baos);
			logger.info("✅ DOCX报告生成成功，大小: {} KB", baos.size() / 1024);
			
			return baos.toByteArray();
		}
	}
	
	/**
	 * 添加基本信息部分
	 */
	private void addBasicInfo(org.apache.poi.xwpf.usermodel.XWPFDocument document, CompareResult result, ExportRequest request) {
		// 获取差异数据（使用保留的原始格式或转换后的数据）
		List<Map<String, Object>> differences;
		if (result.getFormattedDifferences() != null && !result.getFormattedDifferences().isEmpty()) {
			differences = result.getFormattedDifferences();
		} else {
			differences = result.getDifferences() != null ? 
				convertDiffBlocksToMapFormat(result.getDifferences(), false, null, null) : 
				new ArrayList<>();
		}
		
		// 计算有效差异和已忽略差异
		long validDiffCount = differences.stream()
			.filter(diff -> {
				Boolean ignored = (Boolean) diff.get("ignored");
				return ignored == null || !ignored;
			})
			.count();
		long ignoredDiffCount = differences.size() - validDiffCount;
		
		// 比对编号
		org.apache.poi.xwpf.usermodel.XWPFParagraph p1 = document.createParagraph();
		org.apache.poi.xwpf.usermodel.XWPFRun r1 = p1.createRun();
		r1.setText("比对编号: " + request.getTaskId());
		r1.setFontFamily("宋体");
		r1.setFontSize(12);
		
		// 比对结果
		org.apache.poi.xwpf.usermodel.XWPFParagraph p2 = document.createParagraph();
		org.apache.poi.xwpf.usermodel.XWPFRun r2 = p2.createRun();
		r2.setText("比对结果: " + (validDiffCount > 0 ? "有差异" : "无差异"));
		r2.setFontFamily("宋体");
		r2.setFontSize(12);
		
		// 差异统计（如果有被忽略的项，显示统计信息）
		if (ignoredDiffCount > 0) {
			org.apache.poi.xwpf.usermodel.XWPFParagraph p2_1 = document.createParagraph();
			org.apache.poi.xwpf.usermodel.XWPFRun r2_1 = p2_1.createRun();
			r2_1.setText("差异统计: 有效差异 " + validDiffCount + " 项，已忽略差异 " + ignoredDiffCount + " 项");
			r2_1.setFontFamily("宋体");
			r2_1.setFontSize(12);
			r2_1.setColor("666666"); // 灰色显示统计信息
		}
		
		// 基准文档名称
		org.apache.poi.xwpf.usermodel.XWPFParagraph p3 = document.createParagraph();
		org.apache.poi.xwpf.usermodel.XWPFRun r3 = p3.createRun();
		r3.setText("基准文档名称: " + result.getOldFileName());
		r3.setFontFamily("宋体");
		r3.setFontSize(12);
		
		// 比对创建时间
		org.apache.poi.xwpf.usermodel.XWPFParagraph p4 = document.createParagraph();
		org.apache.poi.xwpf.usermodel.XWPFRun r4 = p4.createRun();
		java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		String formattedTime = java.time.LocalDateTime.now().format(formatter);
		r4.setText("比对创建时间: " + formattedTime);
		r4.setFontFamily("宋体");
		r4.setFontSize(12);
		
		// 空行
		document.createParagraph();
	}
	
	/**
	 * 添加差异详细表格
	 */
	private void addDifferenceTable(org.apache.poi.xwpf.usermodel.XWPFDocument document, CompareResult result, ExportRequest request) {
		// 获取差异数据
		List<Map<String, Object>> differences;
		if (result.getFormattedDifferences() != null && !result.getFormattedDifferences().isEmpty()) {
			differences = result.getFormattedDifferences();
		} else {
			differences = result.getDifferences() != null ? 
				convertDiffBlocksToMapFormat(result.getDifferences(), false, null, null) : 
				new ArrayList<>();
		}
		
		// 不再过滤被忽略的差异，显示所有差异（包括被忽略的）
		if (differences.isEmpty()) {
			org.apache.poi.xwpf.usermodel.XWPFParagraph noDiffPara = document.createParagraph();
			org.apache.poi.xwpf.usermodel.XWPFRun noDiffRun = noDiffPara.createRun();
			noDiffRun.setText("未发现差异");
			noDiffRun.setFontFamily("宋体");
			noDiffRun.setFontSize(12);
			return;
		}
		
		// 检查是否有备注
		boolean hasRemark = differences.stream()
			.anyMatch(diff -> {
				String remark = (String) diff.get("remark");
				return remark != null && !remark.isEmpty();
			});
		
		// 创建表格: 根据是否有备注决定列数
		// 有备注: 6列 (比对文档名称, 序号, 页码, 文档修改内容, 差异类型, 备注)
		// 无备注: 5列 (比对文档名称, 序号, 页码, 文档修改内容, 差异类型)
		org.apache.poi.xwpf.usermodel.XWPFTable table = document.createTable();
		table.setWidth("100%");
		
		// 设置表格边框
		org.apache.poi.xwpf.usermodel.XWPFTableRow headerRow = table.getRow(0);
		
		// 表头
		setCellText(headerRow.getCell(0), "比对文档名称", true, true);
		headerRow.addNewTableCell();
		setCellText(headerRow.getCell(1), "序号", true, true);
		headerRow.addNewTableCell();
		setCellText(headerRow.getCell(2), "页码", true, true);
		headerRow.addNewTableCell();
		setCellText(headerRow.getCell(3), "文档修改内容", true, true);
		headerRow.addNewTableCell();
		setCellText(headerRow.getCell(4), "差异类型", true, true);
		
		// 如果有备注，添加备注列
		if (hasRemark) {
			headerRow.addNewTableCell();
			setCellText(headerRow.getCell(5), "备注", true, true);
		}
		
		// 添加数据行（包括被忽略的差异）
		for (int i = 0; i < differences.size(); i++) {
			Map<String, Object> diff = differences.get(i);
			Boolean isIgnored = (Boolean) diff.get("ignored");
			boolean ignored = isIgnored != null && isIgnored;
			
			org.apache.poi.xwpf.usermodel.XWPFTableRow row = table.createRow();
			
			// 比对文档名称（合并行）
			if (i == 0) {
				setCellText(row.getCell(0), result.getNewFileName(), false, false, ignored);
			}
			
			// 序号
			setCellText(row.getCell(1), String.valueOf(i + 1), false, false, ignored);
			
			// 页码
			Object pageObj = diff.get("page");
			String pageText = pageObj != null ? pageObj.toString() : "";
			setCellText(row.getCell(2), pageText, false, false, ignored);
			
			// 文档修改内容 (合并显示，用背景色高亮差异)
			addMergedDifferenceContent(row.getCell(3), diff, ignored);
			
			// 差异类型
			String diffType = getDifferenceType(diff);
			org.apache.poi.xwpf.usermodel.XWPFTableCell typeCell = row.getCell(4);
			typeCell.removeParagraph(0);
			org.apache.poi.xwpf.usermodel.XWPFParagraph typePara = typeCell.addParagraph();
			
			// 添加差异类型文本（如果被忽略，显示"xxx（已忽略）"）
			org.apache.poi.xwpf.usermodel.XWPFRun typeRun = typePara.createRun();
			if (ignored) {
				typeRun.setText(diffType + "（已忽略）");
				typeRun.setColor("999999"); // 灰色
			} else {
				typeRun.setText(diffType);
			}
			typeRun.setFontFamily("宋体");
			typeRun.setFontSize(10);
			
			// 如果有备注列，填充备注内容
			if (hasRemark) {
				String remark = (String) diff.get("remark");
				if (remark != null && !remark.isEmpty()) {
					setCellText(row.getCell(5), remark, false, false, ignored);
				} else {
					setCellText(row.getCell(5), "", false, false, ignored);
				}
			}
		}
		
		logger.info("✅ 添加了 {} 个差异项到表格（包含已忽略项）", differences.size());
	}
	
	/**
	 * 设置单元格文本（不带忽略标记）
	 */
	private void setCellText(org.apache.poi.xwpf.usermodel.XWPFTableCell cell, String text, boolean bold, boolean center) {
		setCellText(cell, text, bold, center, false);
	}
	
	/**
	 * 设置单元格文本（支持忽略标记）
	 */
	private void setCellText(org.apache.poi.xwpf.usermodel.XWPFTableCell cell, String text, boolean bold, boolean center, boolean ignored) {
		cell.removeParagraph(0); // 移除默认段落
		org.apache.poi.xwpf.usermodel.XWPFParagraph para = cell.addParagraph();
		if (center) {
			para.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER);
		}
		org.apache.poi.xwpf.usermodel.XWPFRun run = para.createRun();
		run.setText(text);
		run.setFontFamily("宋体");
		run.setFontSize(10);
		if (bold) {
			run.setBold(true);
		}
		if (ignored) {
			run.setColor("999999"); // 灰色显示被忽略项
		}
	}
	
	/**
	 * 添加合并的差异内容（使用背景色高亮）
	 * 显示完整的上下文句子，并只对差异部分进行背景色高亮
	 */
	private void addMergedDifferenceContent(
		org.apache.poi.xwpf.usermodel.XWPFTableCell cell, 
		Map<String, Object> diff,
		boolean ignored) {
		
		String operation = (String) diff.get("operation");
		
		cell.removeParagraph(0);
		org.apache.poi.xwpf.usermodel.XWPFParagraph para = cell.addParagraph();
		
		if ("DELETE".equals(operation)) {
			// 删除：显示完整的旧文本，对差异部分红色背景高亮
			String fullText = getFullTextFromDiff(diff, "old");
			List<Map<String, Object>> diffRanges = getDiffRangesFromDiff(diff, "old");
			addTextWithHighlight(para, fullText, diffRanges, "FFCCCC", ignored);
			
		} else if ("INSERT".equals(operation)) {
			// 新增：显示完整的新文本，对差异部分绿色背景高亮
			String fullText = getFullTextFromDiff(diff, "new");
			List<Map<String, Object>> diffRanges = getDiffRangesFromDiff(diff, "new");
			addTextWithHighlight(para, fullText, diffRanges, "CCFFCC", ignored);
			
		} else if ("MODIFY".equals(operation)) {
			// 修改：显示"完整旧文本→完整新文本"，差异部分分别用不同背景色
			String oldFullText = getFullTextFromDiff(diff, "old");
			List<Map<String, Object>> oldDiffRanges = getDiffRangesFromDiff(diff, "old");
			addTextWithHighlight(para, oldFullText, oldDiffRanges, "FFCCCC", ignored);
			
			// 箭头
			org.apache.poi.xwpf.usermodel.XWPFRun arrowRun = para.createRun();
			arrowRun.setText(" → ");
			arrowRun.setFontFamily("宋体");
			arrowRun.setFontSize(10);
			if (ignored) {
				arrowRun.setColor("999999");
			}
			
			String newFullText = getFullTextFromDiff(diff, "new");
			List<Map<String, Object>> newDiffRanges = getDiffRangesFromDiff(diff, "new");
			addTextWithHighlight(para, newFullText, newDiffRanges, "CCFFCC", ignored);
		}
	}
	
	/**
	 * 添加带高亮的文本（完整文本 + 差异范围高亮）
	 */
	private void addTextWithHighlight(
		org.apache.poi.xwpf.usermodel.XWPFParagraph para,
		String fullText,
		List<Map<String, Object>> diffRanges,
		String highlightColor,
		boolean ignored) {
		
		if (fullText == null || fullText.isEmpty()) {
			return;
		}
		
		// 如果没有差异范围，整个文本都是差异
		if (diffRanges == null || diffRanges.isEmpty()) {
			org.apache.poi.xwpf.usermodel.XWPFRun run = para.createRun();
			run.setText(fullText);
			run.setFontFamily("宋体");
			run.setFontSize(10);
			setRunBackgroundColor(run, highlightColor);
			if (ignored) {
				run.setColor("999999");
			}
			return;
		}
		
		// 按差异范围分段显示
		int currentPos = 0;
		for (Map<String, Object> range : diffRanges) {
			int start = getIntValue(range.get("start"), 0);
			int end = getIntValue(range.get("end"), fullText.length());
			
			// 确保索引有效
			start = Math.max(0, Math.min(start, fullText.length()));
			end = Math.max(start, Math.min(end, fullText.length()));
			
			// 添加差异前的普通文本
			if (currentPos < start) {
				String normalText = fullText.substring(currentPos, start);
				org.apache.poi.xwpf.usermodel.XWPFRun normalRun = para.createRun();
				normalRun.setText(normalText);
				normalRun.setFontFamily("宋体");
				normalRun.setFontSize(10);
				if (ignored) {
					normalRun.setColor("999999");
				}
			}
			
			// 添加高亮的差异文本
			if (start < end) {
				String diffText = fullText.substring(start, end);
				org.apache.poi.xwpf.usermodel.XWPFRun diffRun = para.createRun();
				diffRun.setText(diffText);
				diffRun.setFontFamily("宋体");
				diffRun.setFontSize(10);
				setRunBackgroundColor(diffRun, highlightColor);
				if (ignored) {
					diffRun.setColor("999999");
				}
			}
			
			currentPos = end;
		}
		
		// 添加最后剩余的普通文本
		if (currentPos < fullText.length()) {
			String remainingText = fullText.substring(currentPos);
			org.apache.poi.xwpf.usermodel.XWPFRun remainingRun = para.createRun();
			remainingRun.setText(remainingText);
			remainingRun.setFontFamily("宋体");
			remainingRun.setFontSize(10);
			if (ignored) {
				remainingRun.setColor("999999");
			}
		}
	}
	
	/**
	 * 从差异项中获取完整文本
	 */
	private String getFullTextFromDiff(Map<String, Object> diff, String type) {
		if ("old".equals(type)) {
			// 先尝试 allTextA
			Object allTextA = diff.get("allTextA");
			if (allTextA != null) {
				if (allTextA instanceof List) {
					List<?> textList = (List<?>) allTextA;
					if (!textList.isEmpty()) {
						return String.join("", textList.stream()
							.map(Object::toString)
							.toArray(String[]::new));
					}
				} else {
					String text = allTextA.toString();
					if (!text.isEmpty()) {
						return text;
					}
				}
			}
			// 回退到 oldText
			return getTextFromDiff(diff, "old");
		} else {
			// 先尝试 allTextB
			Object allTextB = diff.get("allTextB");
			if (allTextB != null) {
				if (allTextB instanceof List) {
					List<?> textList = (List<?>) allTextB;
					if (!textList.isEmpty()) {
						return String.join("", textList.stream()
							.map(Object::toString)
							.toArray(String[]::new));
					}
				} else {
					String text = allTextB.toString();
					if (!text.isEmpty()) {
						return text;
					}
				}
			}
			// 回退到 newText
			return getTextFromDiff(diff, "new");
		}
	}
	
	/**
	 * 从差异项中获取差异范围
	 */
	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> getDiffRangesFromDiff(Map<String, Object> diff, String type) {
		String rangeKey = "old".equals(type) ? "diffRangesA" : "diffRangesB";
		Object ranges = diff.get(rangeKey);
		
		if (ranges instanceof List) {
			try {
				return (List<Map<String, Object>>) ranges;
			} catch (ClassCastException e) {
				logger.warn("无法转换差异范围: {}", e.getMessage());
			}
		}
		
		return new ArrayList<>();
	}
	
	/**
	 * 安全地从Object转换为int
	 */
	private int getIntValue(Object obj, int defaultValue) {
		if (obj == null) {
			return defaultValue;
		}
		if (obj instanceof Number) {
			return ((Number) obj).intValue();
		}
		try {
			return Integer.parseInt(obj.toString());
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}
	
	/**
	 * 设置Run的背景色
	 */
	private void setRunBackgroundColor(org.apache.poi.xwpf.usermodel.XWPFRun run, String color) {
		org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr rPr = run.getCTR().getRPr();
		if (rPr == null) {
			rPr = run.getCTR().addNewRPr();
		}
		org.openxmlformats.schemas.wordprocessingml.x2006.main.CTShd shd = rPr.addNewShd();
		shd.setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STShd.CLEAR);
		shd.setColor("auto");
		shd.setFill(color);
	}
	
	/**
	 * 添加格式化的差异内容（支持删除线和下划线，支持忽略标记）
	 * 保留旧方法以备不时之需
	 */
	private void addFormattedDifferenceContent(
		org.apache.poi.xwpf.usermodel.XWPFTableCell oldCell, 
		org.apache.poi.xwpf.usermodel.XWPFTableCell newCell, 
		Map<String, Object> diff,
		boolean ignored) {
		
		String operation = (String) diff.get("operation");
		String textColor = ignored ? "999999" : "FF0000"; // 被忽略项使用灰色，否则使用红色
		
		oldCell.removeParagraph(0);
		newCell.removeParagraph(0);
		
		org.apache.poi.xwpf.usermodel.XWPFParagraph oldPara = oldCell.addParagraph();
		org.apache.poi.xwpf.usermodel.XWPFParagraph newPara = newCell.addParagraph();
		
		if ("DELETE".equals(operation)) {
			// 删除：旧文档显示删除线，新文档为空
			String oldText = getTextFromDiff(diff, "old");
			org.apache.poi.xwpf.usermodel.XWPFRun oldRun = oldPara.createRun();
			oldRun.setText(oldText);
			oldRun.setFontFamily("宋体");
			oldRun.setFontSize(10);
			oldRun.setStrikeThrough(true);
			oldRun.setColor(textColor);
			
		} else if ("INSERT".equals(operation)) {
			// 新增：旧文档为空，新文档显示下划线
			String newText = getTextFromDiff(diff, "new");
			org.apache.poi.xwpf.usermodel.XWPFRun newRun = newPara.createRun();
			newRun.setText(newText);
			newRun.setFontFamily("宋体");
			newRun.setFontSize(10);
			newRun.setUnderline(org.apache.poi.xwpf.usermodel.UnderlinePatterns.SINGLE);
			newRun.setColor(textColor);
			
		} else if ("MODIFY".equals(operation)) {
			// 修改：旧文档删除线，新文档下划线
			String oldText = getTextFromDiff(diff, "old");
			String newText = getTextFromDiff(diff, "new");
			
			org.apache.poi.xwpf.usermodel.XWPFRun oldRun = oldPara.createRun();
			oldRun.setText(oldText);
			oldRun.setFontFamily("宋体");
			oldRun.setFontSize(10);
			oldRun.setStrikeThrough(true);
			oldRun.setColor(textColor);
			
			org.apache.poi.xwpf.usermodel.XWPFRun newRun = newPara.createRun();
			newRun.setText(newText);
			newRun.setFontFamily("宋体");
			newRun.setFontSize(10);
			newRun.setUnderline(org.apache.poi.xwpf.usermodel.UnderlinePatterns.SINGLE);
			newRun.setColor(textColor);
		}
	}
	
	/**
	 * 从差异项中获取文本
	 */
	private String getTextFromDiff(Map<String, Object> diff, String type) {
		if ("old".equals(type)) {
			Object text = diff.get("oldText");
			if (text != null && !text.toString().isEmpty()) {
				return text.toString();
			}
			text = diff.get("textA");
			return text != null ? text.toString() : "";
		} else {
			Object text = diff.get("newText");
			if (text != null && !text.toString().isEmpty()) {
				return text.toString();
			}
			text = diff.get("textB");
			return text != null ? text.toString() : "";
		}
	}
	
	/**
	 * 获取差异类型显示文本
	 */
	private String getDifferenceType(Map<String, Object> diff) {
		String operation = (String) diff.get("operation");
		if ("DELETE".equals(operation)) {
			return "删除";
		} else if ("INSERT".equals(operation)) {
			return "新增";
		} else if ("MODIFY".equals(operation)) {
			return "修改";
		} else {
			return operation != null ? operation : "未知";
		}
	}

	/**
	 * 生成多格式报告（ZIP包含HTML和DOCX）
	 */
	private byte[] generateMultiFormatReport(CompareResult result, ExportRequest request) throws Exception {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(baos);
			
			// 添加HTML格式
			if (request.getFormats().contains("html")) {
				byte[] htmlZip = generateHTMLReport(result, request);
				zos.putNextEntry(new java.util.zip.ZipEntry("html_report.zip"));
				zos.write(htmlZip);
				zos.closeEntry();
			}
			
			// 添加DOCX格式
			if (request.getFormats().contains("doc")) {
				byte[] docxData = generateDOCXReport(result, request);
				zos.putNextEntry(new java.util.zip.ZipEntry("report.docx"));
				zos.write(docxData);
				zos.closeEntry();
			}
			
			zos.close();
			return baos.toByteArray();
		}
	}

	/**
	 * 生成适用于export的任务状态JSON
	 */
	/**
	 * 从比对结果生成任务状态JSON（增强版本，根据实际情况生成完整数据）
	 */
	private String generateTaskStatusJsonFromCompareResult(CompareResult result, ExportRequest request, String compareResultJson) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			
			// 解析比对结果以获取页面信息
			JsonNode compareData = mapper.readTree(compareResultJson);
			int oldPages = compareData.path("oldImageInfo").path("totalPages").asInt(0);
			int newPages = compareData.path("newImageInfo").path("totalPages").asInt(0);
			int totalPages = Math.max(oldPages, newPages);
			
			// 生成默认的完成状态
			Map<String, Object> taskStatus = new HashMap<>();
			String now = java.time.Instant.now().toString();
			
			taskStatus.put("taskId", request.getTaskId());
			taskStatus.put("status", "COMPLETED");
			taskStatus.put("progress", 100);
			taskStatus.put("progressPercentage", 100);
			taskStatus.put("currentStep", 10);
			taskStatus.put("totalSteps", 8);
			taskStatus.put("currentStepDescription", "已完成所有比对任务");
			taskStatus.put("currentStepDesc", "任务完成");
			taskStatus.put("statusDescription", "比对完成");
			taskStatus.put("progressDescription", "比对完成");
			taskStatus.put("oldFileName", result.getOldFileName());
			taskStatus.put("newFileName", result.getNewFileName());
			taskStatus.put("totalPages", totalPages);
			taskStatus.put("oldDocPages", oldPages);
			taskStatus.put("newDocPages", newPages);
			taskStatus.put("currentPageOld", oldPages);
			taskStatus.put("currentPageNew", newPages);
			taskStatus.put("completedPagesOld", oldPages);
			taskStatus.put("completedPagesNew", newPages);
			taskStatus.put("failedPages", new ArrayList<>());
			taskStatus.put("failedPagesCount", 0);
			taskStatus.put("createdTime", now);
			taskStatus.put("startTime", now);
			taskStatus.put("updatedTime", now);
			taskStatus.put("endTime", now);
			taskStatus.put("totalDuration", 0);
			taskStatus.put("estimatedTotalTime", "0秒");
			taskStatus.put("remainingTime", "0秒");
			
			logger.info("✅ 从比对结果生成任务状态: {} 页 (原文档: {}, 新文档: {})", totalPages, oldPages, newPages);
			
			return mapper.writeValueAsString(taskStatus);
		} catch (Exception e) {
			throw new RuntimeException("从比对结果生成任务状态JSON失败", e);
		}
	}
	
	/**
	 * 生成适用于export的比对结果JSON
	 */
	private String generateCompareResultJsonForExport(CompareResult result) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			Map<String, Object> exportResult = new HashMap<>();
			
		// 基本信息
		exportResult.put("failedPages", result.getFailedPages() != null ? result.getFailedPages() : new ArrayList<>());
		exportResult.put("failedPagesCount", result.getFailedPages() != null ? result.getFailedPages().size() : 0);
		
		// 使用保留的原始格式差异数据，如果没有则使用转换后的数据
		List<Map<String, Object>> differencesToExport;
		if (result.getFormattedDifferences() != null && !result.getFormattedDifferences().isEmpty()) {
			differencesToExport = result.getFormattedDifferences();
			logger.info("✅ 使用原始格式的差异数据，包含 {} 个差异项", differencesToExport.size());
		} else {
			// 转换 DiffBlock 列表为 Map 格式
			List<DiffBlock> diffBlocks = result.getDifferences();
			if (diffBlocks != null && !diffBlocks.isEmpty()) {
				differencesToExport = convertDiffBlocksToMapFormat(diffBlocks, false, null, null);
				logger.warn("⚠️ 使用转换后的差异数据，已转换为Map格式");
			} else {
				differencesToExport = new ArrayList<>();
				logger.warn("⚠️ 无差异数据可导出");
			}
		}
		
		// 统计被忽略的差异项（但不过滤掉，保留给前端显示）
		int ignoredCount = 0;
		int validCount = 0;
		for (Map<String, Object> diff : differencesToExport) {
			Boolean isIgnored = (Boolean) diff.get("ignored");
			if (isIgnored != null && isIgnored) {
				ignoredCount++;
		} else {
				validCount++;
			}
		}
		
		// 导出全部差异项（包括被忽略的），让前端根据ignored字段控制显示
		exportResult.put("differences", differencesToExport);
		
		// 记录导出统计
		logger.info("✅ 导出包含 {} 个差异项（有效 {} 项，已忽略 {} 项）", 
			differencesToExport.size(), validCount, ignoredCount);
		exportResult.put("oldFileName", result.getOldFileName());
		exportResult.put("newFileName", result.getNewFileName());
		exportResult.put("startTime", System.currentTimeMillis()); // 使用当前时间
		
		// 图片信息 - 从实际文件动态获取图片信息
		exportResult.put("oldImageInfo", generateActualImageInfo("old", result.getTaskId()));
		exportResult.put("newImageInfo", generateActualImageInfo("new", result.getTaskId()));
			
		// 图片基路径供Vue组件使用
		exportResult.put("oldImageBaseUrl", "./data/current/images/old");
		exportResult.put("newImageBaseUrl", "./data/current/images/new");
			
			return mapper.writeValueAsString(exportResult);
		} catch (Exception e) {
			throw new RuntimeException("生成比对结果JSON失败", e);
		}
	}

	/**
	 * 从实际文件动态获取图片信息（基于yml配置的文件根目录）
	 */
	private Map<String, Object> generateActualImageInfo(String mode, String taskId) {
		Map<String, Object> info = new HashMap<>();
		List<Map<String, Object>> pages = new ArrayList<>();
		
		// 使用配置的文件根目录，参考存图片的逻辑
		String uploadRootPath = zxcmConfig.getFileUpload().getRootPath();
		Path actualImageDir = Paths.get(uploadRootPath, "compare-pro", "tasks", taskId, "images", mode);
		
		logger.info("🔍 读取图片目录: {}", actualImageDir);
		
		if (!Files.exists(actualImageDir) || !Files.isDirectory(actualImageDir)) {
			logger.warn("⚠️ 图片目录不存在: {}，返回空图片信息", actualImageDir);
			info.put("totalPages", 0);
			info.put("pages", pages);
			return info;
		}
		
		// 读取实际的图片文件
		try {
			// 先尝试新格式 (page-N.png)
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(actualImageDir, "page-*.png")) {
				for (Path imagePath : stream) {
					int pageNum = extractPageNumber(imagePath.getFileName().toString(), "page-", ".png");
					if (pageNum > 0) {
						Map<String, Object> page = createPageInfo(pageNum, mode, imagePath);
						pages.add(page);
					}
				}
			}
			
			// 如果没有找到新格式，尝试旧格式 (old_N.png, new_N.png)
			if (pages.isEmpty()) {
				try (DirectoryStream<Path> stream = Files.newDirectoryStream(actualImageDir, mode + "_*.png")) {
					for (Path imagePath : stream) {
						int pageNum = extractPageNumber(imagePath.getFileName().toString(), mode + "_", ".png");
						if (pageNum > 0) {
							Map<String, Object> page = createPageInfo(pageNum, mode, imagePath);
							pages.add(page);
						}
					}
				}
			}
			
		} catch (IOException e) {
			logger.error("读取图片文件时出错: {}", e.getMessage());
			info.put("totalPages", 0);
			info.put("pages", new ArrayList<>());
			return info;
		}
		
		// 按页码排序
		pages.sort((a, b) -> {
			Integer pageA = (Integer) a.get("pageNum");
			Integer pageB = (Integer) b.get("pageNum");
			return Integer.compare(pageA, pageB);
		});
		
		info.put("totalPages", pages.size());
		info.put("pages", pages);
		
		logger.info("✅ 实际获取 {} 图片信息: {} 页", mode, pages.size());
		return info;
	}
	
	/**
	 * 创建单页图片信息
	 */
	private Map<String, Object> createPageInfo(int pageNum, String mode, Path imagePath) {
		Map<String, Object> page = new HashMap<>();
		page.put("pageNum", pageNum);
		page.put("imageUrl", String.format("./data/current/images/%s/page-%d.png", mode, pageNum));
		
		// 尝试获取实际图片尺寸
		try {
			if (Files.exists(imagePath)) {
				// 使用 Java 内置的图片读取功能获取尺寸
				java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(imagePath.toFile());
				if (image != null) {
					page.put("width", image.getWidth());
					page.put("height", image.getHeight());
					logger.debug("获取图片尺寸: {} -> {}x{}", imagePath.getFileName(), image.getWidth(), image.getHeight());
				} else {
					// 如果无法读取图片，使用默认尺寸
					page.put("width", 1322);
					page.put("height", 1870);
					logger.warn("无法读取图片 {}，使用默认尺寸", imagePath.getFileName());
				}
			} else {
				// 文件不存在，使用默认尺寸
				page.put("width", 1322);
				page.put("height", 1870);
			}
		} catch (Exception e) {
			// 读取图片出错，使用默认尺寸
			page.put("width", 1322);
			page.put("height", 1870);
			logger.warn("读取图片 {} 尺寸时出错: {}，使用默认尺寸", imagePath.getFileName(), e.getMessage());
		}
		
		return page;
	}
	
	/**
	 * 从文件名中提取页码
	 */
	private int extractPageNumber(String fileName, String prefix, String suffix) {
		try {
			if (fileName.startsWith(prefix) && fileName.endsWith(suffix)) {
				String numberPart = fileName.substring(prefix.length(), fileName.length() - suffix.length());
				return Integer.parseInt(numberPart);
			}
		} catch (NumberFormatException e) {
			logger.warn("无法从文件名 {} 提取页码", fileName);
		}
		return -1;
	}

	/**
	 * 生成HTML内容
	 */
	private String generateHTMLContent(CompareResult result, ExportRequest request) {
		StringBuilder html = new StringBuilder();
		html.append("<!doctype html>\n");
		html.append("<html>\n");
		html.append("  <head>\n");
		html.append("    <title>比对结果</title>\n");
		html.append("    <link rel=\"stylesheet\" href=\"./antd.css\">\n");
		html.append("    <link rel=\"stylesheet\" href=\"./table.css\">\n");
		html.append("  </head>\n");
		html.append("  <body>\n");
		html.append("    <div id=\"root\"></div>\n");
		html.append("    <script>\n");
		html.append("      var queryResultJson = ").append(generateQueryResultJson(result)).append(";\n");
		html.append("      var compareResultJson = ").append(generateCompareResultJson(result)).append(";\n");
		html.append("    </script>\n");
		html.append("    <script src=\"./index.js\"></script>\n");
		html.append("  </body>\n");
		html.append("</html>");
		return html.toString();
	}

	/**
	 * 生成CSS内容
	 */
	private String generateCSSContent() {
		// 返回基础的CSS样式
		return "/* Ant Design CSS - 简化版本 */\n" +
			   "body { font-family: 'Segoe UI', Arial, sans-serif; margin: 0; padding: 20px; }\n" +
			   ".ant-table { border: 1px solid #f0f0f0; }\n" +
			   ".ant-btn { padding: 4px 15px; border: 1px solid #d9d9d9; }\n";
	}

	/**
	 * 生成表格CSS
	 */
	private String generateTableCSS() {
		return "/* 表格样式 */\n" +
			   "table { width: 100%; border-collapse: collapse; }\n" +
			   "th, td { padding: 8px; border: 1px solid #ddd; text-align: left; }\n" +
			   "th { background-color: #f5f5f5; }\n";
	}

	/**
	 * 生成JavaScript内容
	 */
	private String generateJSContent(CompareResult result) {
		// 返回基础的JavaScript代码来渲染比对结果
		return "// 比对结果展示脚本\n" +
			   "function renderResults() {\n" +
			   "  const root = document.getElementById('root');\n" +
			   "  let html = '<h1>比对结果</h1>';\n" +
			   "  html += '<p>原文档: ' + queryResultJson.response.data.left_filename + '</p>';\n" +
			   "  html += '<p>新文档: ' + queryResultJson.response.data.right_filename + '</p>';\n" +
			   "  html += '<p>差异总数: ' + (queryResultJson.response.data.differences ? queryResultJson.response.data.differences.length : 0) + '</p>';\n" +
			   "  root.innerHTML = html;\n" +
			   "}\n" +
			   "document.addEventListener('DOMContentLoaded', renderResults);\n";
	}

	/**
	 * 生成查询结果JSON
	 */
	private String generateQueryResultJson(CompareResult result) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			Map<String, Object> queryResult = new HashMap<>();
			queryResult.put("result", 1);
			queryResult.put("message", "success");
			
			Map<String, Object> response = new HashMap<>();
			Map<String, Object> data = new HashMap<>();
			data.put("id", result.getTaskId());
			data.put("left_filename", result.getOldFileName());
			data.put("right_filename", result.getNewFileName());
			data.put("differences", result.getDifferences());
			
			response.put("data", data);
			queryResult.put("response", response);
			
			return mapper.writeValueAsString(queryResult);
		} catch (Exception e) {
			return "{}";
		}
	}

	/**
	 * 生成比对结果JSON
	 */
	private String generateCompareResultJson(CompareResult result) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			Map<String, Object> compareResult = new HashMap<>();
			// 简化的比对结果JSON结构
			compareResult.put("result", 1);
			compareResult.put("message", "success");
			return mapper.writeValueAsString(compareResult);
		} catch (Exception e) {
			return "{}";
		}
	}

	/**
	 * 添加图片到ZIP
	 */
	private void addImagesToZip(java.util.zip.ZipOutputStream zos, CompareResult result) throws Exception {
		// 这里需要实现图片文件的添加逻辑
		// 根据result中的图片信息，将对应的图片文件添加到ZIP中
		
		// 临时实现：创建示例图片文件夹结构
		try {
			// 创建示例图片目录
			String taskId = result.getTaskId();
			
			// 添加左侧文档图片
			for (int i = 1; i <= 3; i++) { // 假设有3页
				String imagePath = "image/" + taskId + "_left/" + String.format("%03d.png", i);
				zos.putNextEntry(new java.util.zip.ZipEntry(imagePath));
				// 这里需要读取实际的图片文件
				byte[] imageData = new byte[100]; // 临时示例数据
				zos.write(imageData);
				zos.closeEntry();
			}
			
			// 添加右侧文档图片
			for (int i = 1; i <= 3; i++) { // 假设有3页
				String imagePath = "image/" + taskId + "_right/" + String.format("%03d.png", i);
				zos.putNextEntry(new java.util.zip.ZipEntry(imagePath));
				// 这里需要读取实际的图片文件
				byte[] imageData = new byte[100]; // 临时示例数据
				zos.write(imageData);
				zos.closeEntry();
			}
		} catch (Exception e) {
			logger.warn("添加图片到ZIP时出错: " + e.getMessage());
		}
	}

	/**
	 * 复制任务图片到临时目录
	 */
	/**
	 * 自动化复制和替换任务图片（增强版本，支持自动化替换）
	 */
	private int copyAndReplaceTaskImages(String taskId, Path tempDir) throws IOException {
		return copyTaskImagesToTemp(taskId, tempDir);
	}
	
	/**
	 * 复制任务图片到临时目录
	 */
	private int copyTaskImagesToTemp(String taskId, Path tempDir) throws IOException {
		String projectRoot = System.getProperty("user.dir");
		
		logger.info("  🔍 搜索任务图片文件，任务ID: {}", taskId);
		
		// 创建图片目录 - 按照前端期望的路径结构（和 embed-json-data.cjs 一致）
		Path oldImagesDir = tempDir.resolve("data").resolve("current").resolve("images").resolve("old");
		Path newImagesDir = tempDir.resolve("data").resolve("current").resolve("images").resolve("new");
		Files.createDirectories(oldImagesDir);
		Files.createDirectories(newImagesDir);
		logger.info("  📁 创建目录结构: data/current/images/{{old,new}}");
		
		// 复制图片文件 - 根据当前工作目录调整路径
		String[] possiblePaths;
		if (projectRoot.endsWith("contract-tools-sdk")) {
			// 当前在contract-tools-sdk目录下
			possiblePaths = new String[] {
				projectRoot + "/uploads/compare-pro/tasks/" + taskId,
				projectRoot + "/uploads/compare-pro/" + taskId,
				projectRoot + "/../uploads/compare-pro/tasks/" + taskId,
				projectRoot + "/../backend/uploads/compare-pro/tasks/" + taskId
			};
		} else {
			// 当前在项目根目录下
			possiblePaths = new String[] {
				projectRoot + "/contract-tools-sdk/uploads/compare-pro/tasks/" + taskId,
				projectRoot + "/uploads/compare-pro/tasks/" + taskId,
				projectRoot + "/sdk/uploads/compare-pro/tasks/" + taskId,
				projectRoot + "/backend/uploads/compare-pro/tasks/" + taskId,
				projectRoot + "/contract-tools-sdk/uploads/compare-pro/" + taskId,
				projectRoot + "/uploads/compare-pro/" + taskId
			};
		}
		
		for (String basePath : possiblePaths) {
			Path taskPath = Paths.get(basePath);
			if (Files.exists(taskPath)) {
				logger.info("  ✓ 找到任务图片目录: {}", basePath);
				int copiedCount = copyTaskImagesFromPath(taskPath, oldImagesDir, newImagesDir);
				logger.info("  ✅ 自动化复制完成: {} 个图片文件", copiedCount);
				return copiedCount;
			} else {
				logger.debug("  ✗ 路径不存在: {}", basePath);
			}
		}
		
		logger.warn("  ⚠️ 未找到任务图片文件，任务ID: {}，将使用默认图片", taskId);
		return 0;
	}

	/**
	 * 从指定路径复制任务图片（返回复制的图片数量）
	 */
	private int copyTaskImagesFromPath(Path taskPath, Path oldImagesDir, Path newImagesDir) throws IOException {
		int totalCopied = 0;
		
		// 检查是否存在images子目录结构
		Path oldImagesPath = taskPath.resolve("images").resolve("old");
		Path newImagesPath = taskPath.resolve("images").resolve("new");
		
		if (Files.exists(oldImagesPath)) {
			// 新的目录结构：tasks/{taskId}/images/old/page-N.png
			logger.info("  📂 使用新的图片目录结构: {}", oldImagesPath);
			int oldCopied = copyImagesFromDirectory(oldImagesPath, oldImagesDir, "page-*.png");
			int newCopied = copyImagesFromDirectory(newImagesPath, newImagesDir, "page-*.png");
			totalCopied = oldCopied + newCopied;
			logger.info("    ✓ 原文档图片: {} 个, 新文档图片: {} 个", oldCopied, newCopied);
		} else {
			// 旧的文件结构：tasks/{taskId}/old_*.png, new_*.png
			logger.info("  📂 使用旧的图片文件结构: {}", taskPath);
			
			// 复制原文档图片
			int oldPageNum = 1;
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(taskPath, "old_*.png")) {
				for (Path imagePath : stream) {
					Files.copy(imagePath, oldImagesDir.resolve("page-" + oldPageNum + ".png"), StandardCopyOption.REPLACE_EXISTING);
					oldPageNum++;
					totalCopied++;
				}
			}
			
			// 复制新文档图片
			int newPageNum = 1;
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(taskPath, "new_*.png")) {
				for (Path imagePath : stream) {
					Files.copy(imagePath, newImagesDir.resolve("page-" + newPageNum + ".png"), StandardCopyOption.REPLACE_EXISTING);
					newPageNum++;
					totalCopied++;
				}
			}
			
			logger.info("    ✓ 原文档图片: {} 个, 新文档图片: {} 个", oldPageNum - 1, newPageNum - 1);
		}
		
		return totalCopied;
	}
	
	/**
	 * 从指定目录复制图片文件（返回复制的数量）
	 */
	private int copyImagesFromDirectory(Path sourceDir, Path targetDir, String pattern) throws IOException {
		if (!Files.exists(sourceDir)) {
			logger.warn("    ✗ 源图片目录不存在: {}", sourceDir);
			return 0;
		}
		
		int copiedCount = 0;
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(sourceDir, pattern)) {
			for (Path imagePath : stream) {
				String fileName = imagePath.getFileName().toString();
				Files.copy(imagePath, targetDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
				copiedCount++;
				logger.debug("      复制图片: {} -> {}", fileName, targetDir.resolve(fileName));
			}
		}
		
		return copiedCount;
	}

	/**
	 * 将临时目录中的图片添加到ZIP（返回添加的文件数量）
	 */
	private int addTempImagesToZip(java.util.zip.ZipOutputStream zos, Path tempDir) throws IOException {
		Path dataDir = tempDir.resolve("data");
		if (!Files.exists(dataDir)) {
			logger.warn("  ✗ 数据目录不存在: {}", dataDir);
			return 0;
		}
		
		final int[] addedCount = {0}; // 使用数组来在lambda中修改值
		
		try {
			Files.walk(dataDir)
				.filter(Files::isRegularFile)
				.forEach(imagePath -> {
					try {
						String relativePath = tempDir.relativize(imagePath).toString().replace("\\", "/");
						zos.putNextEntry(new java.util.zip.ZipEntry(relativePath));
						Files.copy(imagePath, zos);
						zos.closeEntry();
						addedCount[0]++;
						logger.debug("    添加到ZIP: {}", relativePath);
					} catch (IOException e) {
						throw new RuntimeException("添加图片到ZIP失败: " + imagePath, e);
					}
				});
		} catch (IOException e) {
			throw new IOException("遍历数据目录失败: " + dataDir, e);
		}
		
		return addedCount[0];
	}

	/**
	 * 删除临时目录
	 */
	private void deleteTempDirectory(Path tempDir) {
		try {
			if (Files.exists(tempDir)) {
				Files.walk(tempDir)
					.map(Path::toFile)
					.sorted((o1, o2) -> -o1.compareTo(o2))
					.forEach(File::delete);
			}
		} catch (IOException e) {
			logger.warn("清理临时目录失败: {}", tempDir, e);
		}
	}

	/**
	 * 将原始JSON数据转换为CompareResult对象
	 * 处理字段不一致的问题，确保数据完整性
	 */
	private CompareResult convertRawDataToCompareResult(Map<String, Object> rawData, String taskId) {
		CompareResult result = new CompareResult(taskId);
		
		try {
			// 基本信息
			result.setOldFileName((String) rawData.get("oldFileName"));
			result.setNewFileName((String) rawData.get("newFileName"));
			result.setTotalDiffCount((Integer) rawData.getOrDefault("totalDiffCount", 0));
			
			// 失败页面信息
			@SuppressWarnings("unchecked")
			List<String> failedPages = (List<String>) rawData.getOrDefault("failedPages", new ArrayList<>());
			result.setFailedPages(failedPages);
			
			// 差异数据 - 保留原始格式供前端使用
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> rawDifferences = (List<Map<String, Object>>) rawData.get("differences");
			if (rawDifferences != null && !rawDifferences.isEmpty()) {
				// 将原始差异数据转换为DiffBlock对象（用于统计）
				List<DiffBlock> differences = convertRawDifferencesToDiffBlocks(rawDifferences);
				result.setDifferences(differences);
				
				// 同时保留原始格式的差异数据（用于前端显示）
				result.setFormattedDifferences(rawDifferences);
				logger.info("🔄 转换了 {} 个差异项，保留原始格式供前端使用", differences.size());
			}
			
			// 计算统计信息
			if (result.getDifferences() != null) {
				int deleteCount = 0, insertCount = 0;
				for (DiffBlock diff : result.getDifferences()) {
					if (diff.type == DiffBlock.DiffType.DELETED) {
						deleteCount++;
					} else if (diff.type == DiffBlock.DiffType.ADDED) {
						insertCount++;
					}
				}
				result.setDeleteCount(deleteCount);
				result.setInsertCount(insertCount);
			}
			
			// 生成摘要
			result.generateSummary();
			
			logger.info("✅ CompareResult转换完成: 差异{}个, 删除{}个, 新增{}个", 
				result.getTotalDiffCount(), result.getDeleteCount(), result.getInsertCount());
				
		} catch (Exception e) {
			logger.error("转换原始数据为CompareResult时出错: {}", e.getMessage());
			throw new RuntimeException("数据转换失败", e);
		}
		
		return result;
	}
	
	/**
	 * 将原始差异数据转换为DiffBlock对象列表
	 */
	@SuppressWarnings("unchecked")
	private List<DiffBlock> convertRawDifferencesToDiffBlocks(List<Map<String, Object>> rawDifferences) {
		List<DiffBlock> differences = new ArrayList<>();
		
		for (Map<String, Object> rawDiff : rawDifferences) {
			try {
				DiffBlock diff = new DiffBlock();
				
				// 基本信息
				diff.page = (Integer) rawDiff.getOrDefault("page", 1);
				String operation = (String) rawDiff.getOrDefault("operation", "UNKNOWN");
				diff.oldText = (String) rawDiff.getOrDefault("oldText", "");
				diff.newText = (String) rawDiff.getOrDefault("newText", "");
				
				// 坐标信息
				if (rawDiff.containsKey("oldBbox")) {
					List<Double> bbox = (List<Double>) rawDiff.get("oldBbox");
					if (bbox != null && bbox.size() >= 4) {
						// 转换List<Double>为double[]
						double[] bboxArray = new double[4];
						for (int i = 0; i < 4; i++) {
							bboxArray[i] = bbox.get(i);
						}
						// 创建oldBboxes列表
						if (diff.oldBboxes == null) {
							diff.oldBboxes = new ArrayList<>();
						}
						diff.oldBboxes.add(bboxArray);
					}
				}
				
				if (rawDiff.containsKey("newBbox")) {
					List<Double> bbox = (List<Double>) rawDiff.get("newBbox");
					if (bbox != null && bbox.size() >= 4) {
						// 转换List<Double>为double[]
						double[] bboxArray = new double[4];
						for (int i = 0; i < 4; i++) {
							bboxArray[i] = bbox.get(i);
						}
						// 创建newBboxes列表
						if (diff.newBboxes == null) {
							diff.newBboxes = new ArrayList<>();
						}
						diff.newBboxes.add(bboxArray);
					}
				}
				
				// 设置类型
				if ("DELETE".equals(operation)) {
					diff.type = DiffBlock.DiffType.DELETED;
				} else if ("INSERT".equals(operation)) {
					diff.type = DiffBlock.DiffType.ADDED;
				} else {
					diff.type = DiffBlock.DiffType.MODIFIED;
				}
				
				differences.add(diff);
				
			} catch (Exception e) {
				logger.warn("转换单个差异项时出错，跳过: {}", e.getMessage());
			}
		}
		
		return differences;
	}

	/**
	 * 将DiffBlock对象转换为前端期望的JSON格式
	 * 确保字段名和数据结构与前端Vue组件完全匹配
	 */
	private Map<String, Object> convertDiffBlockToFrontendFormat(DiffBlock diff) {
		Map<String, Object> frontendDiff = new HashMap<>();
		
		// 基本信息
		frontendDiff.put("page", diff.page);
		frontendDiff.put("oldText", diff.oldText != null ? diff.oldText : "");
		frontendDiff.put("newText", diff.newText != null ? diff.newText : "");
		
		// 操作类型 - 转换为前端期望的格式
		String operation = "UNKNOWN";
		if (diff.type == DiffBlock.DiffType.DELETED) {
			operation = "DELETE";
		} else if (diff.type == DiffBlock.DiffType.ADDED) {
			operation = "INSERT";
		} else if (diff.type == DiffBlock.DiffType.MODIFIED) {
			operation = "MODIFY";
		}
		frontendDiff.put("operation", operation);
		
		// 页面信息 - 设置具体的页码
		if (diff.pageA != null && !diff.pageA.isEmpty()) {
			frontendDiff.put("pageA", diff.pageA.get(0));
			frontendDiff.put("pageAList", diff.pageA);
		} else {
			frontendDiff.put("pageA", diff.page);
			frontendDiff.put("pageAList", List.of(diff.page));
		}
		
		if (diff.pageB != null && !diff.pageB.isEmpty()) {
			frontendDiff.put("pageB", diff.pageB.get(0));
			frontendDiff.put("pageBList", diff.pageB);
		} else {
			frontendDiff.put("pageB", diff.page);
			frontendDiff.put("pageBList", List.of(diff.page));
		}
		
		// 坐标信息 - 转换为前端期望的单个bbox格式
		if (diff.oldBboxes != null && !diff.oldBboxes.isEmpty()) {
			frontendDiff.put("oldBbox", diff.oldBboxes.get(0)); // 前端期望单个bbox
			frontendDiff.put("oldBboxes", diff.oldBboxes);       // 保留数组格式以防需要
		}
		
		if (diff.newBboxes != null && !diff.newBboxes.isEmpty()) {
			frontendDiff.put("newBbox", diff.newBboxes.get(0)); // 前端期望单个bbox
			frontendDiff.put("newBboxes", diff.newBboxes);       // 保留数组格式以防需要
		}
		
		// 文本信息
		frontendDiff.put("textStartIndexA", diff.textStartIndexA);
		frontendDiff.put("textStartIndexB", diff.textStartIndexB);
		
		// 完整文本信息
		if (diff.allTextA != null && !diff.allTextA.isEmpty()) {
			frontendDiff.put("allTextA", diff.allTextA);
		} else {
			frontendDiff.put("allTextA", diff.oldText != null ? List.of(diff.oldText) : new ArrayList<>());
		}
		
		if (diff.allTextB != null && !diff.allTextB.isEmpty()) {
			frontendDiff.put("allTextB", diff.allTextB);
		} else {
			frontendDiff.put("allTextB", diff.newText != null ? List.of(diff.newText) : new ArrayList<>());
		}
		
		// 差异范围信息 - 如果没有则创建默认的
		if (diff.diffRangesA != null) {
			frontendDiff.put("diffRangesA", diff.diffRangesA);
		} else if (diff.oldText != null && !diff.oldText.isEmpty()) {
			// 创建默认的差异范围
			Map<String, Object> range = new HashMap<>();
			range.put("start", 0);
			range.put("end", diff.oldText.length());
			range.put("type", "DIFF");
			frontendDiff.put("diffRangesA", List.of(range));
		} else {
			frontendDiff.put("diffRangesA", new ArrayList<>());
		}
		
		if (diff.diffRangesB != null) {
			frontendDiff.put("diffRangesB", diff.diffRangesB);
		} else if (diff.newText != null && !diff.newText.isEmpty()) {
			// 创建默认的差异范围
			Map<String, Object> range = new HashMap<>();
			range.put("start", 0);
			range.put("end", diff.newText.length());
			range.put("type", "DIFF");
			frontendDiff.put("diffRangesB", List.of(range));
		} else {
			frontendDiff.put("diffRangesB", new ArrayList<>());
		}
		
		// 上一个位置信息 - 如果没有则设置为null或默认值
		frontendDiff.put("prevOldBbox", diff.prevOldBboxes != null && !diff.prevOldBboxes.isEmpty() ? 
			diff.prevOldBboxes.get(0) : null);
		frontendDiff.put("prevNewBbox", diff.prevNewBboxes != null && !diff.prevNewBboxes.isEmpty() ? 
			diff.prevNewBboxes.get(0) : null);
		
		logger.debug("转换差异项: {} -> {}", operation, frontendDiff);
		return frontendDiff;
	}
}
