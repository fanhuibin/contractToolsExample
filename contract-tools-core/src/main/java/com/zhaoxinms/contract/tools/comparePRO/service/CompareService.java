package com.zhaoxinms.contract.tools.comparePRO.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhaoxinms.contract.tools.compare.DiffUtil;
import com.zhaoxinms.contract.tools.compare.util.TextNormalizer;
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
import com.zhaoxinms.contract.tools.comparePRO.util.DiffProcessingUtil;
import com.zhaoxinms.contract.tools.comparePRO.util.TextExtractionUtil;
import com.zhaoxinms.contract.tools.comparePRO.util.WatermarkRemover;
import com.zhaoxinms.contract.tools.config.ZxcmConfig;
import com.zhaoxinms.contract.tools.watermark.OpenCVWatermarkUtil; // 直接调用OpenCV去水印

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
    private CompareTaskQueue taskQueue;
    
    @Autowired(required = false)
    private MinerUOCRService mineruOcrService;
    
    @Autowired
    private CompareResultExportService exportService;
    
    @Autowired
    private CompareImageService imageService;

    @Autowired
    private WatermarkRemover watermarkRemover;

    // PDFWatermarkRemovalService 已废弃，现在直接在 recognizePdfWithMinerU 中实现水印去除
    // 新流程：拆分图片（一次）→ 去水印 → 合成PDF → MinerU复用图片

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
        
        // 检查MinerU服务
        if (mineruOcrService != null) {
            System.out.println("✅ MinerU OCR服务已注入并可用");
            System.out.println("   MinerU API: " + gpuOcrConfig.getMineru().getApiUrl());
            System.out.println("   Backend: " + gpuOcrConfig.getMineru().getBackend());
        } else {
            System.out.println("⚠️  MinerU OCR服务未注入（可选）");
        }
        
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
			CompareImageService.DocumentImageInfo oldImageInfo = imageService.getDocumentImageInfo(taskId, "old");
			CompareImageService.DocumentImageInfo newImageInfo = imageService.getDocumentImageInfo(taskId, "new");

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
	 * 获取文档图片信息
	 * 
	 * @deprecated 已迁移到 CompareImageService，请使用 imageService.getDocumentImageInfo()
	 */
	@Deprecated
	public CompareImageService.DocumentImageInfo getDocumentImageInfo(String taskId, String mode) throws Exception {
		// 委托给 CompareImageService
		return imageService.getDocumentImageInfo(taskId, mode);
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
            
            // 如果options为null，使用默认配置
            if (options == null) {
                options = CompareOptions.createDefault();
            }
            
            // 【关键】使用配置文件中的OCR服务，忽略前端传递的值
            String configuredOcrService = gpuOcrConfig.getDefaultOcrService();
            options.setOcrServiceType(configuredOcrService);
            
            System.out.println("🔍 OCR服务配置: " + configuredOcrService);
            progressManager.logStepDetail("使用配置文件指定的OCR服务: {}", configuredOcrService);
            
            // 根据options选择OCR服务
            boolean useThirdPartyOcr = options.isUseThirdPartyOcr();
            boolean useMinerU = options.isUseMinerU();
            
            System.out.println("🔍 DEBUG: 最终判断 - useMinerU = " + useMinerU + ", useThirdPartyOcr = " + useThirdPartyOcr);
            System.out.println("🔍 DEBUG: mineruOcrService == null? " + (mineruOcrService == null));
            
                // 使用MinerU OCR
                if (mineruOcrService == null) {
                    throw new RuntimeException("MinerU服务未启用，请检查配置");
                }
                System.out.println("✅ DEBUG: 将使用MinerU OCR服务");
                progressManager.logStepDetail("✅ 使用MinerU OCR服务");
            
            progressManager.completeStep(TaskStep.INIT);

            // 注意：水印去除逻辑已整合到 recognizePdfWithMinerU() 方法中
            // 新流程：拆分图片 → 去水印 → 合成PDF → MinerU复用图片（一次拆分）

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
            
            // 注意：图片保存已集成到OCR识别流程中
            
			RecognitionResult resultA;
			    // 使用MinerU OCR
			    progressManager.logStepDetail("使用MinerU OCR识别原文档");
			    resultA = recognizePdfWithMinerU(oldPath, options, progressManager, task.getTaskId(), "old", task);
			List<CharBox> seqA = resultA.charBoxes;
			progressManager.completeStep(TaskStep.OCR_FIRST_DOC);

            // 步骤3: OCR识别新文档
            progressManager.startStep(TaskStep.OCR_SECOND_DOC);
            
            // 注意：图片保存和去水印已集成到OCR识别流程中

			RecognitionResult resultB;
			    // 使用MinerU OCR
			    progressManager.logStepDetail("使用MinerU OCR识别新文档");
			    resultB = recognizePdfWithMinerU(newPath, options, progressManager, task.getTaskId(), "new", task);
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
                // 计算实际页数（取两个文档的最大页数）
                int actualTotalPages = Math.max(resultA.totalPages, resultB.totalPages);
                progressManager.logStepDetail("文档页数信息: 原文档{}页, 新文档{}页, 使用最大值{}页", 
                    resultA.totalPages, resultB.totalPages, actualTotalPages);
                
                // 设置任务的总页数
                task.setTotalPages(actualTotalPages);
                
                progressManager.logStepDetail("🚀 开始OCR验证（已优化并行处理）: {}个差异块", merged.size());
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
		// 
		// 【重要修正】保护金额中的小数点和千分位逗号，避免误删除
		// 策略：改进正则表达式，排除"数字.数字"和"数字,数字"模式
		{
			// 方案：使用负向零宽断言（negative lookbehind/lookahead）排除金额相关的点和逗号
			// 正则说明：
			// - (?<!\\d) : 前面不是数字
			// - [\\s\\p{Punct}，。；：、！？…·•]+ : 空格或标点符号（一个或多个）
			// - (?!\\d) : 后面不是数字
			// 这样可以避免匹配"103400.00"中的点，同时匹配" . "这样的孤立标点
			Pattern wsPunct = Pattern.compile("(?<!\\d)[\\s\\p{Punct}，。；：、！？…·•]+(?!\\d)");
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

    /**
     * 将DiffBlock列表转换为前端期望的Map格式（保留原始图像坐标）
     * 
     * 注意：此方法已改为 public，供 CompareResultExportService 使用
     * TODO: 后续可以移到 CompareResultFormatter 服务中
     */
	public List<Map<String, Object>> convertDiffBlocksToMapFormat(List<DiffBlock> diffBlocks, boolean isDebugMode, List<CharBox> seqA, List<CharBox> seqB) {
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
	 * 导出比对报告
	 * 
	 * 重构说明：导出功能已迁移到 CompareResultExportService
	 */
	public byte[] exportReport(ExportRequest request) throws Exception {
		String taskId = request.getTaskId();
		
		// 获取任务数据
		CompareResult result = getCompareResult(taskId);
		if (result == null) {
			throw new RuntimeException("任务结果不存在: " + taskId);
		}

		// 委托给导出服务
		return exportService.exportReport(result, request);
	}

	

	/**
	 * 从实际文件动态获取图片信息（基于yml配置的文件根目录）
	 * 
	 * @deprecated 已迁移到 CompareImageService，请使用 imageService.generateActualImageInfo()
	 */
	@Deprecated
	public Map<String, Object> generateActualImageInfo(String mode, String taskId) {
		// 委托给 CompareImageService
		return imageService.generateActualImageInfo(mode, taskId);
	}
	
	/**
	 * 使用MinerU OCR识别PDF文档
	 * 
	 * 【中间结果保存】
	 * MinerU 识别过程会自动保存以下中间结果，方便调试和分析：
	 * 
	 * 1. MinerU 中间结果目录：{taskDir}/mineru_intermediate/{docMode}/
	 *    - 01_mineru_raw_response.json    : MinerU API 原始响应（完整 JSON）
	 *    - 02_content_list.json           : 格式化的 content_list（MinerU 原始结构）
	 *    - 03_content_list_readable.json  : 易读格式的 content_list（中文字段名）
	 *    - 04_content_list_stats.txt      : 统计信息（类型分布、页面分布）
	 * 
	 * 2. 页面图片：{taskDir}/images/{docMode}/
	 *    - page-1.png, page-2.png, ...    : PDF 渲染的高清图片（300 DPI）
	 * 
	 * 3. OCR 页面结果：{taskDir}/ocr_pages/
	 *    - {docMode}_page_001.json        : 每页的 OCR 识别结果（dots.ocr 格式）
	 * 
	 * 4. 提取的全文：{taskDir}/
	 *    - old_xxx.pdf.extracted.txt              : 带页面标记的全文
	 *    - old_xxx.pdf.extracted.compare.txt      : 无页面标记的全文（用于比对）
	 * 
	 * 详细说明请参阅：contract-tools-core/MINERU_INTERMEDIATE_RESULTS_README.md
	 * 
	 * @param pdfPath PDF文件路径
	 * @param options 比对选项
	 * @param progressManager 进度管理器
	 * @param taskId 任务ID
	 * @param docMode 文档模式（old/new）
	 * @param task 任务对象
	 * @return 识别结果
	 */
	private RecognitionResult recognizePdfWithMinerU(
			Path pdfPath, 
			CompareOptions options,
			CompareTaskProgressManager progressManager,
			String taskId,
			String docMode,
			CompareTask task) {
		
		List<CharBox> charBoxes = new ArrayList<>();
		List<String> failedPages = new ArrayList<>();
		int totalPages = 0;
		
		try {
			if (mineruOcrService == null) {
				throw new RuntimeException("MinerU服务未初始化");
			}
			
			// 准备输出目录
			Path taskDir = Paths.get(gpuOcrConfig.getUploadPath(), "compare-pro", "tasks", taskId);
			java.io.File outputDir = taskDir.toFile();
			if (!outputDir.exists()) {
				outputDir.mkdirs();
			}
			
			// ==================== 优化后的水印去除逻辑 ====================
			// 策略：先拆分图片（一次），对图片去水印，合成新PDF，MinerU复用图片
			java.io.File pdfFileToProcess = pdfPath.toFile();
			
			if (options.isRemoveWatermark()) {
				progressManager.logStepDetail("检测到去除水印选项，开始图片预处理...");
				logger.info("📝 开始去除PDF水印: {}, 模式: {}", pdfPath.getFileName(), docMode);
				
				try {
					// 1. 先拆分PDF为图片（复用MinerU的图片，避免重复拆分）
					logger.info("步骤1：拆分PDF为图片（DPI=300）");
					progressManager.logStepDetail("拆分PDF为图片...");
					
					java.io.File imagesDir = new java.io.File(outputDir, "images/" + docMode);
					if (!imagesDir.exists()) {
						imagesDir.mkdirs();
					}
					
					List<java.io.File> imageFiles = new ArrayList<>();
					try (org.apache.pdfbox.pdmodel.PDDocument document = 
						org.apache.pdfbox.pdmodel.PDDocument.load(pdfPath.toFile())) {
						
						org.apache.pdfbox.rendering.PDFRenderer renderer = 
							new org.apache.pdfbox.rendering.PDFRenderer(document);
						int pageCount = document.getNumberOfPages();
						
						logger.info("开始拆分PDF，共 {} 页", pageCount);
						
						for (int i = 0; i < pageCount; i++) {
							java.awt.image.BufferedImage image = null;
							try {
								// 渲染为高清图片（DPI=300）
								image = renderer.renderImageWithDPI(i, 300, 
									org.apache.pdfbox.rendering.ImageType.RGB);
								
								// 保存为PNG
								java.io.File imageFile = new java.io.File(imagesDir, 
									"page-" + (i + 1) + ".png");
								javax.imageio.ImageIO.write(image, "PNG", imageFile);
								imageFiles.add(imageFile);
								
								logger.debug("页面 {} 拆分完成: {}x{}", 
									i + 1, image.getWidth(), image.getHeight());
								
							} finally {
								if (image != null) {
									image.flush();
									image = null;
								}
								// 每3页GC
								if ((i + 1) % 3 == 0) {
									System.gc();
								}
							}
						}
					}
					
					logger.info("✅ PDF拆分完成，共 {} 页", imageFiles.size());
					progressManager.logStepDetail("PDF拆分完成，共{}页", imageFiles.size());
					
				// 2. 对图片进行水印去除
				logger.info("步骤2：对图片进行水印去除");
				progressManager.logStepDetail("正在去除图片水印...");
				
				// 获取水印强度（直接使用字符串，不再依赖PDFWatermarkRemovalService）
				String strengthStr = options.getWatermarkRemovalStrength();
				if (strengthStr == null || strengthStr.trim().isEmpty()) {
					strengthStr = "default"; // 默认值
				}
				strengthStr = strengthStr.toLowerCase(); // 统一转为小写
				
				logger.info("水印去除强度: {}", strengthStr);
				
				// 直接对已拆分的图片去水印（使用OpenCVWatermarkUtil）
				int successCount = 0;
				OpenCVWatermarkUtil opencvUtil = new OpenCVWatermarkUtil();
				
				for (java.io.File imageFile : imageFiles) {
					try {
						boolean success = false;
						String imagePath = imageFile.getAbsolutePath();
						
						// 根据强度字符串调用对应的OpenCV方法
						switch (strengthStr) {
							case "default":
								success = opencvUtil.removeWatermark(imagePath);
								break;
							case "extended":
								success = opencvUtil.removeWatermarkExtended(imagePath);
								break;
							case "loose":
								success = opencvUtil.removeWatermarkLoose(imagePath);
								break;
							case "smart":
								success = opencvUtil.removeWatermarkSmart(imagePath);
								break;
							default:
								logger.warn("未知的水印强度: {}, 使用default模式", strengthStr);
								success = opencvUtil.removeWatermark(imagePath);
						}
						
						if (success) {
							successCount++;
						}
					} catch (Exception e) {
						logger.warn("图片去水印失败: {}, 原因: {}", 
							imageFile.getName(), e.getMessage());
					}
				}
					
					logger.info("✅ 图片去水印完成，成功处理 {}/{} 张", successCount, imageFiles.size());
					progressManager.logStepDetail("图片去水印完成，成功{}/{}张", successCount, imageFiles.size());
					
					// 3. 将去水印后的图片合成为新PDF
					logger.info("步骤3：合成去水印后的PDF");
					progressManager.logStepDetail("合成去水印PDF...");
					
					String watermarkFreeFileName = pdfPath.getFileName().toString()
						.replace(".pdf", "_nowatermark.pdf");
					java.io.File watermarkFreePdf = new java.io.File(outputDir, watermarkFreeFileName);
					
					try (org.apache.pdfbox.pdmodel.PDDocument newDoc = 
						new org.apache.pdfbox.pdmodel.PDDocument()) {
						
						for (java.io.File imageFile : imageFiles) {
							java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(imageFile);
							if (image == null) {
								logger.warn("无法读取图片: {}", imageFile.getName());
								continue;
							}
							
							float width = image.getWidth();
							float height = image.getHeight();
							org.apache.pdfbox.pdmodel.PDPage page = 
								new org.apache.pdfbox.pdmodel.PDPage(
									new org.apache.pdfbox.pdmodel.common.PDRectangle(width, height));
							newDoc.addPage(page);
							
							org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject pdImage = 
								org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory
									.createFromImage(newDoc, image);
							
							try (org.apache.pdfbox.pdmodel.PDPageContentStream contentStream = 
								new org.apache.pdfbox.pdmodel.PDPageContentStream(newDoc, page)) {
								contentStream.drawImage(pdImage, 0, 0, width, height);
							}
						}
						
						newDoc.save(watermarkFreePdf);
					}
					
					logger.info("✅ PDF合成成功: {}", watermarkFreePdf.getName());
					progressManager.logStepDetail("PDF合成成功，使用去水印PDF进行识别");
					
					// 4. 使用去水印后的PDF（图片已保存，MinerU会复用）
					pdfFileToProcess = watermarkFreePdf;
					
				} catch (OutOfMemoryError oom) {
					logger.error("❌ 内存不足无法去除水印，使用原始PDF继续: {}", oom.getMessage());
					progressManager.logStepDetail("内存不足，跳过水印去除，使用原始PDF");
					System.gc();
					try {
						Thread.sleep(100);
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
					}
				} catch (Exception e) {
					logger.error("❌ 水印去除过程出错: {}, 使用原始PDF继续", e.getMessage(), e);
					progressManager.logStepDetail("水印去除出错: {}, 使用原始PDF", e.getMessage());
				}
			}
			// ==================== 水印去除逻辑结束 ====================
			
			// 调用MinerU识别，返回dots.ocr兼容的PageLayout格式
			// 注意：这里使用 pdfFileToProcess（可能是去水印后的PDF）
			TextExtractionUtil.PageLayout[] layouts = mineruOcrService.recognizePdf(
				pdfFileToProcess,
				taskId,
				outputDir,
				docMode,
				options
			);
			
			totalPages = layouts.length;
			
			// 使用与dots.ocr完全相同的处理逻辑
			// TextExtractionUtil.parseTextAndPositionsFromResults 会将PageLayout转为CharBox
			charBoxes = TextExtractionUtil.parseTextAndPositionsFromResults(layouts);
			
			// 保存抽取的全文（与dots.ocr相同格式）
			saveExtractedText(layouts, pdfPath);
			
			// 保存每页的JSON（调试用）
			savePageLayoutsJson(layouts, outputDir, docMode);
			
			progressManager.logStepDetail("MinerU识别完成: {}页, {}个CharBox", totalPages, charBoxes.size());
			
		} catch (Exception e) {
			logger.error("MinerU识别失败: " + e.getMessage(), e);
			// 记录所有页面为失败
			for (int i = 0; i < totalPages; i++) {
				failedPages.add(pdfPath.getFileName() + "-第" + (i + 1) + "页: " + e.getMessage());
			}
		}
		
		return new RecognitionResult(charBoxes, failedPages, totalPages);
	}
	
	/**
	 * 保存抽取的全文（与dots.ocr格式相同）
	 */
	private void saveExtractedText(TextExtractionUtil.PageLayout[] layouts, Path pdfPath) {
		try {
			// 使用正确的方法名：extractTextFromResults 和 extractTextWithPageMarkers
			String extractedWithPages = TextExtractionUtil.extractTextWithPageMarkers(layouts);
			String extractedNoPages = TextExtractionUtil.extractTextFromResults(layouts);
			
			String txtOut = pdfPath.toAbsolutePath().toString() + ".extracted.txt";
			String txtOutCompare = pdfPath.toAbsolutePath().toString() + ".extracted.compare.txt";
			
			Files.write(Path.of(txtOut), extractedWithPages.getBytes(StandardCharsets.UTF_8));
			Files.write(Path.of(txtOutCompare), extractedNoPages.getBytes(StandardCharsets.UTF_8));
			
			System.out.println("Extracted text saved: " + txtOut);
			System.out.println("Extracted text (no page markers) saved: " + txtOutCompare);
		} catch (Exception e) {
			System.err.println("Failed to write extracted text: " + e.getMessage());
		}
	}
	
	/**
	 * 保存每页的PageLayout为JSON（调试用，与dots.ocr格式相同）
	 */
	private void savePageLayoutsJson(TextExtractionUtil.PageLayout[] layouts, java.io.File outputDir, String docMode) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			java.io.File jsonDir = new java.io.File(outputDir, "ocr_pages");
			if (!jsonDir.exists()) {
				jsonDir.mkdirs();
			}
			
			for (int i = 0; i < layouts.length; i++) {
				TextExtractionUtil.PageLayout layout = layouts[i];
				if (layout == null) continue;
				
			// 构建JSON对象
			Map<String, Object> pageJson = new HashMap<>();
			pageJson.put("page", layout.page);
			pageJson.put("imgW", layout.imageWidth);
			pageJson.put("imgH", layout.imageHeight);
				
				// 转换items为JSON友好格式
				List<Map<String, Object>> itemsJson = new ArrayList<>();
				if (layout.items != null) {
					for (TextExtractionUtil.LayoutItem item : layout.items) {
						Map<String, Object> itemMap = new HashMap<>();
						itemMap.put("bbox", item.bbox);
						itemMap.put("category", item.category);
						itemMap.put("text", item.text);
						itemsJson.add(itemMap);
					}
				}
				pageJson.put("items", itemsJson);
				pageJson.put("itemCount", itemsJson.size());
				
				// 保存到文件
				String fileName = String.format("%s_page_%03d.json", docMode, layout.page);
				java.io.File jsonFile = new java.io.File(jsonDir, fileName);
				mapper.writerWithDefaultPrettyPrinter().writeValue(jsonFile, pageJson);
			}
			
			System.out.println("📄 MinerU每页JSON已保存到: " + jsonDir.getAbsolutePath() + " (共" + layouts.length + "页)");
		} catch (Exception e) {
			System.err.println("保存每页JSON失败: " + e.getMessage());
		}
	}
	

	/**
	 * 从指定路径复制任务图片（返回复制的图片数量）
	 * 
	 * @deprecated 已迁移到 CompareImageService，请使用 imageService.copyTaskImagesFromPath()
	 */
	@Deprecated
	public int copyTaskImagesFromPath(Path taskPath, Path oldImagesDir, Path newImagesDir) throws IOException {
		// 委托给 CompareImageService
		return imageService.copyTaskImagesFromPath(taskPath, oldImagesDir, newImagesDir);
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

}
