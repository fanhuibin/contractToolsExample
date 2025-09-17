package com.zhaoxinms.contract.tools.ocrcompare.compare;

import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;

// PDF处理相关导入
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
import com.zhaoxinms.contract.tools.ocrcompare.concurrent.GPUOCRTaskQueue;

/**
 * GPU OCR比对服务 - 基于DotsOcrCompareDemoTest的完整比对功能
 */
@Service
public class GPUOCRCompareService {

	// 内部类：包装OCR识别结果和错误信息
	private static class RecognitionResult {
		public final List<CharBox> charBoxes;
		public final List<String> failedPages;

		public RecognitionResult(List<CharBox> charBoxes, List<String> failedPages) {
			this.charBoxes = charBoxes;
			this.failedPages = failedPages;
		}
	}

	@Autowired
	private GPUOCRConfig gpuOcrConfig;

	@Autowired
	private ZxcmConfig zxcmConfig;

	@Autowired
	private OcrImageSaver ocrImageSaver;

	@Autowired
	private GPUOCRTaskQueue taskQueue;

	private final ConcurrentHashMap<String, GPUOCRCompareTask> tasks = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, GPUOCRCompareResult> results = new ConcurrentHashMap<>();
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
			Path resultsDir = Paths.get(uploadRootPath, "gpu-ocr-compare", "results");

			if (Files.exists(resultsDir)) {
				Files.list(resultsDir).filter(path -> path.toString().endsWith(".json")).forEach(jsonFile -> {
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
				Files.list(frontendResultsDir).filter(path -> path.toString().endsWith(".json")).forEach(jsonFile -> {
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
			try (var oldInputStream = oldFile.getInputStream(); var newInputStream = newFile.getInputStream()) {
				Files.copy(oldInputStream, oldFilePath);
				Files.copy(newInputStream, newFilePath);
			}

			System.out.println("文件已保存到系统上传目录:");
			System.out.println("  原文档: " + oldFilePath.toAbsolutePath());
			System.out.println("  新文档: " + newFilePath.toAbsolutePath());

			// 使用新的任务队列执行比对任务
			boolean submitted = taskQueue.submitTask(
					() -> executeCompareTaskWithPaths(task, oldFilePath.toString(), newFilePath.toString(), options),
					taskId);

			if (!submitted) {
				task.setStatus(GPUOCRCompareTask.Status.FAILED);
				task.setErrorMessage("任务队列已满，无法提交任务");
				System.err.println("任务队列已满，任务 " + taskId + " 提交失败");
			}

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

		// 使用新的任务队列执行比对任务
		boolean submitted = taskQueue
				.submitTask(() -> executeCompareTaskWithPaths(task, oldFilePath, newFilePath, options), taskId);

		if (!submitted) {
			task.setStatus(GPUOCRCompareTask.Status.FAILED);
			task.setErrorMessage("任务队列已满，无法提交任务");
			System.err.println("任务队列已满，任务 " + taskId + " 提交失败");
		}

		return taskId;
	}

	/**
	 * 调试模式：使用已有任务结果进行重新分析
	 */
	public String debugCompareWithTaskId(String taskId, GPUOCRCompareOptions options) {
		// 重置调试计数器
		DiffProcessingUtil.resetDebugCounter();
		
		// Debug模式直接使用原任务ID，不创建新ID
		GPUOCRCompareTask existingTask = getTaskStatus(taskId);
		if (existingTask == null) {
			// 如果原任务不存在，创建一个基本的任务对象用于debug处理
			existingTask = new GPUOCRCompareTask(taskId);
			existingTask.setOldFileName("debug_old.pdf");
			existingTask.setNewFileName("debug_new.pdf");
			tasks.put(taskId, existingTask);
		}

		// 重置任务状态为调试模式
		existingTask.setStatus(GPUOCRCompareTask.Status.PENDING);
		existingTask.setErrorMessage(null);

		// 为lambda使用创建最终引用，确保effectively final
		final GPUOCRCompareTask taskToRun = existingTask;

		// 使用新的任务队列执行调试比对任务，使用原始任务ID
		boolean submitted = taskQueue
				.submitTask(() -> executeDebugCompareTaskWithExistingResult(taskToRun, taskId, options), taskId);

		if (!submitted) {
			existingTask.setStatus(GPUOCRCompareTask.Status.FAILED);
			existingTask.setErrorMessage("任务队列已满，无法提交调试任务");
			System.err.println("任务队列已满，调试任务 " + taskId + " 提交失败");
		}

		return taskId; // 返回原始任务ID
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
	 * 获取Canvas版本的前端比对结果（包含图片列表和原始坐标）
	 */
	public Map<String, Object> getCanvasFrontendResult(String taskId) {
		Map<String, Object> originalResult = getRawFrontendResult(taskId);
		if (originalResult == null) {
			return null;
		}

		// 获取任务信息
		GPUOCRCompareTask task = getTaskStatus(taskId);
		if (task == null) {
			return originalResult;
		}

		// 创建Canvas版本的结果
		Map<String, Object> canvasResult = new HashMap<>(originalResult);

		try {
			// 获取图片信息
			DocumentImageInfo oldImageInfo = getDocumentImageInfo(taskId, "old");
			DocumentImageInfo newImageInfo = getDocumentImageInfo(taskId, "new");

			// 添加图片信息
			canvasResult.put("oldImageInfo", oldImageInfo);
			canvasResult.put("newImageInfo", newImageInfo);

			// 更新文件URL为图片列表
			String baseUploadPath = "/api/gpu-ocr/files";
			canvasResult.put("oldImageBaseUrl", baseUploadPath + "/gpu-ocr-compare/tasks/" + taskId + "/images/old");
			canvasResult.put("newImageBaseUrl", baseUploadPath + "/gpu-ocr-compare/tasks/" + taskId + "/images/new");

			// 保持原有的PDF URL作为备用
			canvasResult.put("oldPdfUrl", originalResult.get("oldPdfUrl"));
			canvasResult.put("newPdfUrl", originalResult.get("newPdfUrl"));

			System.out.println("Canvas前端结果创建成功，包含图片信息");

		} catch (Exception e) {
			System.err.println("获取Canvas前端结果失败: " + e.getMessage());
			// 出错时返回原始结果
			return originalResult;
		}

		return canvasResult;
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
		Path imagesDir = Paths.get(uploadRootPath, "gpu-ocr-compare", "tasks", taskId, "images", mode);

		System.out.println("获取文档图片信息 - 任务ID: " + taskId + ", 模式: " + mode);
		System.out.println("上传根路径: " + uploadRootPath);
		System.out.println("图片目录路径: " + imagesDir);
		System.out.println("图片目录是否存在: " + Files.exists(imagesDir));

		if (!Files.exists(imagesDir)) {
			// 列出父目录内容，帮助调试
			Path parentDir = imagesDir.getParent();
			if (Files.exists(parentDir)) {
				System.out.println("父目录存在，内容如下:");
				try (var stream = Files.list(parentDir)) {
					stream.forEach(path -> System.out.println("  - " + path.getFileName()));
				}
			} else {
				System.out.println("父目录也不存在: " + parentDir);
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

		String baseUploadPath = "/api/gpu-ocr/files";
		String baseUrl = baseUploadPath + "/gpu-ocr-compare/tasks/" + taskId + "/images/" + mode;

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

		System.out.println("获取文档图片信息完成: " + mode + ", 共" + docInfo.getTotalPages() + "页");
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
	public List<GPUOCRCompareTask> getAllTasks() {
		return new ArrayList<>(tasks.values());
	}

	/**
	 * 获取任务队列状态信息
	 */
	public GPUOCRTaskQueue.TaskQueueStats getQueueStats() {
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
		GPUOCRCompareTask task = tasks.remove(taskId);
		return task != null;
	}

	/**
	 * 执行比对任务（文件路径）
	 */
	private void executeCompareTaskWithPaths(GPUOCRCompareTask task, String oldFilePath, String newFilePath,
			GPUOCRCompareOptions options) {
		long startTime = System.currentTimeMillis();
		System.out.println("开始GPU OCR正常比对任务: " + task.getTaskId());

		try {
			task.setStatus(GPUOCRCompareTask.Status.OCR_PROCESSING);
			task.updateProgress(1, "初始化OCR客户端");

			// 创建OCR客户端
			DotsOcrClient client = new DotsOcrClient.Builder().baseUrl(gpuOcrConfig.getOcrBaseUrl())
					.defaultModel(gpuOcrConfig.getOcrModel()).build();

			task.updateProgress(2, "OCR识别第一个文档");

			// OCR识别第一个文档
			Path oldPath = Paths.get(oldFilePath);

			// 保存第一个文档的OCR图片
			if (gpuOcrConfig.isSaveOcrImages()) {
				try {
					System.out.println("开始保存第一个文档OCR图片，任务ID: " + task.getTaskId());
					Path savedPath = ocrImageSaver.saveOcrImages(oldPath, task.getTaskId(), "old");
					System.out.println("第一个文档OCR图片保存成功，路径: " + savedPath);
				} catch (Exception e) {
					System.err.println("保存第一个文档OCR图片失败: " + e.getMessage());
					e.printStackTrace();
				}
			} else {
				System.out.println("OCR图片保存功能已关闭，跳过保存第一个文档");
			}

			RecognitionResult resultA = recognizePdfAsCharSeq(client, oldPath, null, false, options);
			List<CharBox> seqA = resultA.charBoxes;

			task.updateProgress(3, "OCR识别第二个文档");

			// OCR识别第二个文档
			Path newPath = Paths.get(newFilePath);

			// 保存第二个文档的OCR图片
			if (gpuOcrConfig.isSaveOcrImages()) {
				try {
					System.out.println("开始保存第二个文档OCR图片，任务ID: " + task.getTaskId());
					Path savedPath = ocrImageSaver.saveOcrImages(newPath, task.getTaskId(), "new");
					System.out.println("第二个文档OCR图片保存成功，路径: " + savedPath);
				} catch (Exception e) {
					System.err.println("保存第二个文档OCR图片失败: " + e.getMessage());
					e.printStackTrace();
				}
			} else {
				System.out.println("OCR图片保存功能已关闭，跳过保存第二个文档");
			}

			RecognitionResult resultB = recognizePdfAsCharSeq(client, newPath, null, false, options);
			List<CharBox> seqB = resultB.charBoxes;

			long ocrTime = System.currentTimeMillis() - startTime;
			task.updateProgress(4, "OCR识别完成，开始文本比对");

			System.out.println(String.format("OCR完成。A=%d字符, B=%d字符, 耗时=%dms", seqA.size(), seqB.size(), ocrTime));

			// 文本处理和差异分析（使用TextNormalizer进行完整预处理）
			String normA = preprocessTextForComparison(joinWithLineBreaks(seqA), options);
			String normB = preprocessTextForComparison(joinWithLineBreaks(seqB), options);

			task.updateProgress(5, "执行差异分析");

			DiffUtil dmp = new DiffUtil();
			dmp.Diff_EditCost = 6;
			LinkedList<DiffUtil.Diff> diffs = dmp.diff_main(normA, normB);
			dmp.diff_cleanupEfficiency(diffs);

			task.updateProgress(6, "生成差异块");

			List<DiffBlock> rawBlocks = DiffProcessingUtil.splitDiffsByBounding(diffs, seqA, seqB, false); // 正常模式不开启调试
			List<DiffBlock> filteredBlocks = DiffProcessingUtil.filterIgnoredDiffBlocks(rawBlocks, seqA, seqB);

			task.updateProgress(7, "合并差异块");

			System.out.println("开始合并差异块，filteredBlocks大小: " + filteredBlocks.size());
			List<DiffBlock> merged = mergeBlocksByBbox(filteredBlocks);
			System.out.println("合并完成，merged大小: " + merged.size());

			task.updateProgress(8, "比对完成");

			System.out.println(String.format("差异分析完成。原始差异块=%d, 过滤后=%d, 合并后=%d", rawBlocks.size(), filteredBlocks.size(),
					merged.size()));

			// （正常比对模式不输出调试日志）

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
			DotsOcrClient renderClient = new DotsOcrClient.Builder().baseUrl(gpuOcrConfig.getOcrBaseUrl())
					.defaultModel(gpuOcrConfig.getOcrModel()).build();
			int dpi = gpuOcrConfig.getRenderDpi();
			System.out.println("渲染页面大小...");
			PageImageSizeProvider sizeA = renderPageSizes(oldPath, dpi);
			PageImageSizeProvider sizeB = renderPageSizes(newPath, dpi);
			System.out.println("页面大小渲染完成");

			// 4) 标注并输出PDF到系统上传目录
			String uploadRootPath = zxcmConfig.getFileUpload().getRootPath();
			Path annotatedDir = Paths.get(uploadRootPath, "gpu-ocr-compare", "annotated", task.getTaskId());
			Files.createDirectories(annotatedDir);

			System.out.println("开始执行保存比对结果步骤...");
			task.updateProgress(12, "保存比对结果");

			try {
				// 保存结果到任务
				System.out.println("创建GPUOCRCompareResult对象...");
				GPUOCRCompareResult result = new GPUOCRCompareResult(task.getTaskId());
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
					double oldImageWidth = sizeA.widths != null && sizeA.widths.length > 0 ? sizeA.widths[0]
							: oldPdfWidth;
					double oldImageHeight = sizeA.heights != null && sizeA.heights.length > 0 ? sizeA.heights[0]
							: oldPdfHeight;
					double newImageWidth = sizeB.widths != null && sizeB.widths.length > 0 ? sizeB.widths[0]
							: newPdfWidth;
					double newImageHeight = sizeB.heights != null && sizeB.heights.length > 0 ? sizeB.heights[0]
							: newPdfHeight;

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

					System.out.println(String.format(
							"坐标转换参数计算成功: old[%.2f,%.2f]->[%.2f,%.2f] scale[%.3f,%.3f], new[%.2f,%.2f]->[%.2f,%.2f] scale[%.3f,%.3f]",
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
				List<Map<String, Object>> formattedDifferences = convertDiffBlocksToMapFormat(merged, false, null, null);

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
				frontendResult.put("oldPdfUrl", baseUploadPath + "/gpu-ocr-compare/tasks/" + task.getTaskId() + "/old_"
						+ oldPath.getFileName().toString());
				frontendResult.put("newPdfUrl", baseUploadPath + "/gpu-ocr-compare/tasks/" + task.getTaskId() + "/new_"
						+ newPath.getFileName().toString());
				frontendResult.put("annotatedOldPdfUrl",
						baseUploadPath + "/gpu-ocr-compare/annotated/" + task.getTaskId() + "/old_annotated.pdf");
				frontendResult.put("annotatedNewPdfUrl",
						baseUploadPath + "/gpu-ocr-compare/annotated/" + task.getTaskId() + "/new_annotated.pdf");
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
	private void executeDebugCompareTaskWithExistingResult(GPUOCRCompareTask task, String originalTaskId,
			GPUOCRCompareOptions options) {
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

			// 查找原任务的PDF文件；若不存在，则从已保存的OCR JSON推断基名以直接解析JSON
			Path oldPdfPath = findTaskPdfFile(taskDir, "old");
			Path newPdfPath = findTaskPdfFile(taskDir, "new");

			if (oldPdfPath == null || newPdfPath == null) {
				System.out.println("未找到PDF文件，尝试从OCR JSON推断基名进行调试解析...");
				Path[] jsonBases = findOcrJsonBases(taskDir);
				if (jsonBases == null || jsonBases.length < 2 || jsonBases[0] == null || jsonBases[1] == null) {
					throw new RuntimeException("无法找到原任务的PDF或OCR JSON基文件，目录: " + taskDir);
				}
				// 使用推断的基名路径充当pdfPath基准（parseCharBoxesFromSavedJson只依赖“基名.page-N.ocr.json”）
				oldPdfPath = jsonBases[0];
				newPdfPath = jsonBases[1];
				System.out.println("使用OCR JSON基名进行调试: ");
				System.out.println("  旧文档基名: " + oldPdfPath);
				System.out.println("  新文档基名: " + newPdfPath);
			} else {
				System.out.println("找到原任务PDF文件:");
				System.out.println("  旧文档: " + oldPdfPath);
				System.out.println("  新文档: " + newPdfPath);

				// Debug模式复用原始任务的图片，不需要重新保存
				System.out.println("Debug模式：复用原始任务 " + originalTaskId + " 的OCR图片资源");
			}

			task.updateProgress(2, "解析OCR数据");

			// 从OCR结果中提取CharBox数据（使用与正常比对相同的方法）
			RecognitionResult resultA = recognizePdfAsCharSeq(null, oldPdfPath, null, true, options);
			RecognitionResult resultB = recognizePdfAsCharSeq(null, newPdfPath, null, true, options);
			List<CharBox> seqA = resultA.charBoxes;
			List<CharBox> seqB = resultB.charBoxes;

			if (seqA.isEmpty() || seqB.isEmpty()) {
				throw new RuntimeException("无法从OCR结果中提取字符数据");
			}

			long ocrTime = System.currentTimeMillis() - startTime;
			task.updateProgress(3, "OCR数据解析完成，开始文本比对");

			System.out.println(String.format("OCR数据解析完成。A=%d字符, B=%d字符, 耗时=%dms", seqA.size(), seqB.size(), ocrTime));

			// 文本处理和差异分析（使用TextNormalizer进行完整预处理）
			String joinedA = joinWithLineBreaks(seqA);
			String joinedB = joinWithLineBreaks(seqB);
			String normA = preprocessTextForComparison(joinedA, options);
			String normB = preprocessTextForComparison(joinedB, options);

			// 调试：检查各阶段文本长度变化（仅Debug模式）
			System.out.println("[DEBUG] seqA长度=" + seqA.size() + ", joinedA长度=" + joinedA.length() + ", normA长度=" + normA.length());
			System.out.println("[DEBUG] seqB长度=" + seqB.size() + ", joinedB长度=" + joinedB.length() + ", normB长度=" + normB.length());
			System.out.println("[DEBUG] joinWithLineBreaks增加了 " + (joinedA.length() - seqA.size()) + " 个字符(A), " + (joinedB.length() - seqB.size()) + " 个字符(B)");

			task.updateProgress(4, "执行差异分析");

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

			task.updateProgress(7, "比对完成");

			// 创建比对结果对象
			GPUOCRCompareResult result = new GPUOCRCompareResult();
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

			// 构建PDF文件URL（使用原始任务ID的资源路径）
			String baseUploadPath = "/api/gpu-ocr/files";
			try {
				if (oldPdfPath != null && oldPdfPath.toString().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
					result.setOldPdfUrl(baseUploadPath + "/gpu-ocr-compare/tasks/" + originalTaskId + "/"
							+ oldPdfPath.getFileName().toString());
				} else {
					result.setOldPdfUrl("");
				}
				if (newPdfPath != null && newPdfPath.toString().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
					result.setNewPdfUrl(baseUploadPath + "/gpu-ocr-compare/tasks/" + originalTaskId + "/"
							+ newPdfPath.getFileName().toString());
				} else {
					result.setNewPdfUrl("");
				}
			} catch (Exception ignore) {
				result.setOldPdfUrl("");
				result.setNewPdfUrl("");
			}
			result.setAnnotatedOldPdfUrl(
					baseUploadPath + "/gpu-ocr-compare/annotated/" + originalTaskId + "/old_annotated.pdf");
			result.setAnnotatedNewPdfUrl(
					baseUploadPath + "/gpu-ocr-compare/annotated/" + originalTaskId + "/new_annotated.pdf");
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
				double oldImageHeight = sizeA.heights != null && sizeA.heights.length > 0 ? sizeA.heights[0]
						: oldPdfHeight;
				double newImageWidth = sizeB.widths != null && sizeB.widths.length > 0 ? sizeB.widths[0] : newPdfWidth;
				double newImageHeight = sizeB.heights != null && sizeB.heights.length > 0 ? sizeB.heights[0]
						: newPdfHeight;

				// 3) 计算缩放比例：图像坐标到PDF坐标的转换比例（图像尺寸 / PDF尺寸）
				double oldScaleX = oldImageWidth / oldPdfWidth;
				double oldScaleY = oldImageHeight / oldPdfHeight;
				double newScaleX = newImageWidth / newPdfWidth;
				double newScaleY = newImageHeight / newPdfHeight;

				result.setOldPdfScaleX(oldScaleX);
				result.setOldPdfScaleY(oldScaleY);
				result.setNewPdfScaleX(newScaleX);
				result.setNewPdfScaleY(newScaleY);

//                System.out.println(String.format(
//                    "[DEBUG坐标] 参数: old[%.2f,%.2f]->[%.2f,%.2f] scale[%.3f,%.3f], new[%.2f,%.2f]->[%.2f,%.2f] scale[%.3f,%.3f]",
//                        oldImageWidth, oldImageHeight, oldPdfWidth, oldPdfHeight, oldScaleX, oldScaleY,
//                        newImageWidth, newImageHeight, newPdfWidth, newPdfHeight, newScaleX, newScaleY));
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
			List<Map<String, Object>> formattedDifferences = convertDiffBlocksToMapFormat(merged, true, seqA, seqB);

			// 创建包装对象用于返回前端期望的格式
			System.out.println("创建前端结果对象...");
			Map<String, Object> frontendResult = new HashMap<>();
			frontendResult.put("taskId", originalTaskId); // Debug模式使用原始任务ID
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

			// 保存前端格式的结果（Debug模式使用原始任务ID）
			System.out.println("保存结果到缓存...");
			results.put(originalTaskId, result);
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

			long totalTime = System.currentTimeMillis() - startTime;
			task.setStatus(GPUOCRCompareTask.Status.COMPLETED);
			task.updateProgress(8, "调试比对完成");

			System.out
					.println(String.format("GPU OCR调试比对完成。差异数量=%d, 总耗时=%dms", formattedDifferences.size(), totalTime));
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
	 * @param text    原始文本
	 * @param options 比对选项
	 * @return 预处理后的文本
	 */
	private String preprocessTextForComparison(String text, GPUOCRCompareOptions options) {
		if (text == null || text.isEmpty()) {
			return "";
		}

		// 调试：处理前长度
		try {
			System.out.println("[PREPROCESS] before length=" + text.length());
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
			System.out.println("[PREPROCESS] after length=" + normalized.length());
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

	/**
	 * 从保存的JSON文件中解析CharBox数据（用于debug模式）
	 */
	private RecognitionResult parseCharBoxesFromSavedJsonWithErrors(Path pdfPath, GPUOCRCompareOptions options) {
		List<String> failedPages = new ArrayList<>();
		String documentName = pdfPath.getFileName().toString();

		try {
			boolean hasPdf = pdfPath != null && pdfPath.toString().toLowerCase(Locale.ROOT).endsWith(".pdf");

			int totalPages;
			TextExtractionUtil.PageLayout[] ordered;

			if (hasPdf) {
				// 计算PDF页数
				totalPages = countPdfPages(pdfPath);
				// 解析每一页的OCR结果
				ordered = new TextExtractionUtil.PageLayout[totalPages];
				for (int page = 1; page <= totalPages; page++) {
					try {
						ordered[page - 1] = parseOnePageFromSavedJson(pdfPath, page);
					} catch (Exception e) {
						System.err.println("解析第" + page + "页OCR结果失败: " + e.getMessage());
						ordered[page - 1] = createEmptyPageLayout(page);
						failedPages.add(documentName + "-第" + page + "页: " + e.getMessage());
					}
				}
			} else {
				// 无PDF：统计符合"基名.page-*.ocr.json"的页数
				String prefix = pdfPath.toAbsolutePath().toString() + ".page-";
				try (var stream = Files.list(pdfPath.getParent())) {
					totalPages = (int) stream.filter(p -> {
						String name = p.getFileName().toString();
						return name.startsWith(pdfPath.getFileName().toString() + ".page-")
								&& name.endsWith(".ocr.json");
					}).count();
				}
				if (totalPages <= 0) {
					throw new RuntimeException("未找到任何OCR JSON分页文件: 基名=" + pdfPath);
				}
				ordered = new TextExtractionUtil.PageLayout[totalPages];
				for (int page = 1; page <= totalPages; page++) {
					try {
						ordered[page - 1] = parseOnePageFromSavedJson(pdfPath, page);
					} catch (Exception e) {
						System.err.println("解析第" + page + "页OCR结果失败: " + e.getMessage());
						ordered[page - 1] = createEmptyPageLayout(page);
						failedPages.add(documentName + "-第" + page + "页: " + e.getMessage());
					}
				}
			}

			// 计算页面高度信息用于页眉页脚检测
			double[] pageHeights;
			if (hasPdf) {
				pageHeights = calculatePageHeights(pdfPath);
			} else {
				// 无PDF：尝试使用PageLayout中的imageHeight作为高度（点与像素不一致，但足以用于百分比判断）
				pageHeights = new double[ordered.length];
				for (int i = 0; i < ordered.length; i++) {
					TextExtractionUtil.PageLayout pl = ordered[i];
					pageHeights[i] = (pl != null && pl.imageHeight > 0) ? pl.imageHeight : 0;
				}
			}

			// 使用新的按顺序读取方法解析文本和位置，支持基于位置的页眉页脚检测
			List<CharBox> charBoxes = TextExtractionUtil.parseTextAndPositionsFromResults(ordered,
					TextExtractionUtil.ExtractionStrategy.SEQUENTIAL, options.isIgnoreHeaderFooter(),
					options.getHeaderHeightPercent(), options.getFooterHeightPercent(), pageHeights);

			return new RecognitionResult(charBoxes, failedPages);

		} catch (Exception e) {
			System.err.println("解析OCR JSON文件失败: " + e.getMessage());
			failedPages.add(documentName + ": 解析失败 - " + e.getMessage());
			return new RecognitionResult(new ArrayList<>(), failedPages);
		}
	}

	/**
	 * 从保存的JSON文件中解析CharBox数据（保持向后兼容）
	 */
	private List<CharBox> parseCharBoxesFromSavedJson(Path pdfPath, GPUOCRCompareOptions options) {
		RecognitionResult result = parseCharBoxesFromSavedJsonWithErrors(pdfPath, options);
		return result.charBoxes;
	}

	/**
	 * 从保存的JSON文件中解析CharBox数据（原方法，保持不变）
	 */
	private List<CharBox> parseCharBoxesFromSavedJsonOriginal(Path pdfPath, GPUOCRCompareOptions options) {
		List<CharBox> charBoxes = new ArrayList<>();

		try {
			boolean hasPdf = pdfPath != null && pdfPath.toString().toLowerCase(Locale.ROOT).endsWith(".pdf");

			int totalPages;
			TextExtractionUtil.PageLayout[] ordered;

			if (hasPdf) {
				// 计算PDF页数
				totalPages = countPdfPages(pdfPath);
				// 解析每一页的OCR结果
				ordered = new TextExtractionUtil.PageLayout[totalPages];
				for (int page = 1; page <= totalPages; page++) {
					try {
						ordered[page - 1] = parseOnePageFromSavedJson(pdfPath, page);
					} catch (Exception e) {
						System.err.println("解析第" + page + "页OCR结果失败: " + e.getMessage());
						ordered[page - 1] = null;
					}
				}
			} else {
				// 无PDF：统计符合“基名.page-*.ocr.json”的页数
				String prefix = pdfPath.toAbsolutePath().toString() + ".page-";
				try (var stream = Files.list(pdfPath.getParent())) {
					totalPages = (int) stream.filter(p -> {
						String name = p.getFileName().toString();
						return name.startsWith(pdfPath.getFileName().toString() + ".page-")
								&& name.endsWith(".ocr.json");
					}).count();
				}
				if (totalPages <= 0) {
					throw new RuntimeException("未找到任何OCR JSON分页文件: 基名=" + pdfPath);
				}
				ordered = new TextExtractionUtil.PageLayout[totalPages];
				for (int page = 1; page <= totalPages; page++) {
					try {
						ordered[page - 1] = parseOnePageFromSavedJson(pdfPath, page);
					} catch (Exception e) {
						System.err.println("解析第" + page + "页OCR结果失败: " + e.getMessage());
						ordered[page - 1] = null;
					}
				}
			}

			// 计算页面高度信息用于页眉页脚检测
			double[] pageHeights;
			if (hasPdf) {
				pageHeights = calculatePageHeights(pdfPath);
			} else {
				// 无PDF：尝试使用PageLayout中的imageHeight作为高度（点与像素不一致，但足以用于百分比判断）
				pageHeights = new double[ordered.length];
				for (int i = 0; i < ordered.length; i++) {
					TextExtractionUtil.PageLayout pl = ordered[i];
					pageHeights[i] = (pl != null && pl.imageHeight > 0) ? pl.imageHeight : 0;
				}
			}

			// 使用新的解析方法提取CharBox，支持基于位置的页眉页脚检测
			charBoxes = TextExtractionUtil.parseTextAndPositionsFromResults(ordered,
					TextExtractionUtil.ExtractionStrategy.SEQUENTIAL, options.isIgnoreHeaderFooter(),
					options.getHeaderHeightPercent(), options.getFooterHeightPercent(), pageHeights);

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

	/**
	 * 计算PDF每页的高度（用于页眉页脚百分比计算）
	 * 
	 * @param pdfPath PDF文件路径
	 * @return 每页的高度数组（单位：点，72 DPI）
	 */
	private double[] calculatePageHeights(Path pdfPath) {
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

			System.out.println("计算PDF页面高度完成: " + pdfPath.getFileName() + ", 页数: " + pageCount + ", 首页高度: "
					+ (heights.length > 0 ? heights[0] : 0) + "点");

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
		// 获取图片尺寸信息（不修改OCR JSON，直接用于PageLayout）
		int imgW = 0;
		int imgH = 0;
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(pngBytes);
			BufferedImage image = ImageIO.read(bais);
			if (image != null) {
				imgW = image.getWidth();
				imgH = image.getHeight();
				System.out.println("第" + page + "页图片尺寸: " + imgW + "x" + imgH);
			}
		} catch (Exception e) {
			System.err.println("获取第" + page + "页图片尺寸失败: " + e.getMessage());
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
		} catch (Exception ignore) {
		}
		return new TextExtractionUtil.PageLayout(page, items, imgW, imgH);
	}

	private List<byte[]> renderAllPagesToPng(DotsOcrClient client, Path pdfPath) throws Exception {
		// 加载PDF文档并计算页数
		try (PDDocument doc = PDDocument.load(pdfPath.toFile())) {
			int pageCount = doc.getNumberOfPages();

			// 使用固定DPI（来自配置）
			int dpi = gpuOcrConfig.getRenderDpi();
			System.out.println("文档页数: " + pageCount + ", 使用固定DPI: " + dpi);

			boolean saveImages = client.isSaveRenderedImages();
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
					list.add(bytes);

				}
			}

			return list;
		}
	}

	// 保留方法签名（如有调用），但改为固定DPI返回
	private int calculateDynamicDpi(int pageCount) {
		return gpuOcrConfig.getRenderDpi();
	}

	private List<TextExtractionUtil.LayoutItem> extractLayoutItems(JsonNode root) {
		return TextExtractionUtil.extractLayoutItems(root);
	}

	private List<CharBox> parseTextAndPositionsFromResults(TextExtractionUtil.PageLayout[] ordered,
			TextExtractionUtil.ExtractionStrategy strategy, boolean ignoreHeaderFooter) {
		return TextExtractionUtil.parseTextAndPositionsFromResults(ordered, strategy, ignoreHeaderFooter);
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
			boolean resumeFromStep4, GPUOCRCompareOptions options) throws Exception {
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
			// Step 1: render PDF to images（默认旧流程）
			List<byte[]> pages = renderAllPagesToPng(client, pdf);
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
						return parseOnePage(client, img, pageNo, prompt, pdf);
					} catch (Exception e) {
						System.err.println("OCR识别第" + pageNo + "页失败: " + e.getMessage());
						return createEmptyPageLayout(pageNo);
					}
				});
			}

			// 收集结果，处理超时和异常
			ordered = new TextExtractionUtil.PageLayout[total];
			for (int i = 0; i < total; i++) {
				try {
					TextExtractionUtil.PageLayout p = ecs.take().get();
					if (p != null) {
						ordered[p.page - 1] = p;
						// 检查是否为空页面布局（表示识别失败）
						if (isEmptyPageLayout(p)) {
							failedPages.add(documentName + "-第" + p.page + "页: OCR识别失败");
						}
					} else {
						// 不应该发生，但为了安全起见
						ordered[i] = createEmptyPageLayout(i + 1);
						failedPages.add(documentName + "-第" + (i + 1) + "页: 返回null结果");
					}
				} catch (Exception e) {
					System.err.println("获取OCR识别结果失败: " + e.getMessage());
					// 创建空页面布局
					TextExtractionUtil.PageLayout emptyPage = createEmptyPageLayout(i + 1);
					ordered[i] = emptyPage;

					String errorMsg = e.getMessage();
					if (errorMsg != null && errorMsg.contains("timeout")) {
						failedPages.add(documentName + "-第" + (i + 1) + "页: 超时错误");
					} else {
						failedPages.add(documentName + "-第" + (i + 1) + "页: " + errorMsg);
					}
				}
			}
			pool.shutdownNow();
		}

		long ocrAllCost = System.currentTimeMillis() - ocrAllStartAt;
		try {
			int pages = ordered == null ? 0 : ordered.length;
			double avg = pages > 0 ? (ocrAllCost * 1.0 / pages) : 0.0;
			System.out.println(String.format("OCR识别完成: file=%s, 页数=%d, 总用时=%dms, 平均每页=%.1fms",
					pdf == null ? "-" : pdf.getFileName().toString(), pages, ocrAllCost, avg));
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

		return new RecognitionResult(out, failedPages);
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

	// 额外过滤：移除空内容且无坐标/范围的差异块
	private List<DiffBlock> removeEmptyBlocks(List<DiffBlock> blocks) {
		if (blocks == null || blocks.isEmpty())
			return blocks;
		List<DiffBlock> out = new ArrayList<>();
		for (DiffBlock b : blocks) {
			if (b == null)
				continue;
			boolean noText = (b.oldText == null || b.oldText.isBlank()) && (b.newText == null || b.newText.isBlank());
			boolean noBbox = (b.oldBboxes == null || b.oldBboxes.isEmpty()) && (b.newBboxes == null || b.newBboxes.isEmpty());
			boolean noRanges = (b.diffRangesA == null || b.diffRangesA.isEmpty()) && (b.diffRangesB == null || b.diffRangesB.isEmpty());
			if (noText && noBbox && noRanges) {
				// 丢弃
				continue;
			}
			out.add(b);
		}
		return out;
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

	private boolean safeEqualsForDebug(String a, String b) {
		if (a == null && b == null)
			return true;
		if (a == null || b == null)
			return false;
		// 忽略首尾空白差异
		return a.strip().equals(b.strip());
	}

	private String truncateForLog(String s) {
		if (s == null)
			return "";
		int max = 200;
		return s.length() <= max ? s : s.substring(0, max) + "...";
	}

	/**
	 * 合并具有相同bbox的块（保持原有顺序，不跳过IGNORED块）
	 */
	private List<DiffBlock> mergeSameBboxBlocks(List<DiffBlock> blocks) {
		List<DiffBlock> result = new ArrayList<>();
		boolean[] processed = new boolean[blocks.size()];

		for (int i = 0; i < blocks.size(); i++) {
			if (processed[i])
				continue;

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
				if (processed[j])
					continue;

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
		if (group.isEmpty())
			return null;
		if (group.size() == 1)
			return group.get(0);

		DiffBlock first = group.get(0);
		DiffBlock merged = new DiffBlock();
		merged.type = first.type;
		merged.page = first.page;
		// 合并页码数组
		merged.pageA = new ArrayList<>();
		merged.pageB = new ArrayList<>();
		for (DiffBlock block : group) {
			if (block.pageA != null)
				merged.pageA.addAll(block.pageA);
			if (block.pageB != null)
				merged.pageB.addAll(block.pageB);
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
			if (firstWithPrev2 == null && ((block.prevOldBboxes != null && !block.prevOldBboxes.isEmpty())
					|| (block.prevNewBboxes != null && !block.prevNewBboxes.isEmpty()))) {
				firstWithPrev2 = block;
			}
			if (firstWithPrev == null && ((block.prevOldBboxes != null && !block.prevOldBboxes.isEmpty())
					|| (block.prevNewBboxes != null && !block.prevNewBboxes.isEmpty()))) {
				firstWithPrev = block;
			}
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

			// 对于相同bbox合并，合并所有块的差异范围
			// 因为相同bbox的块可能有不同的差异范围，需要全部合并
			if (block.diffRangesA != null) {
				for (DiffBlock.TextRange range : block.diffRangesA) {
					// 检查是否已存在相同的范围，避免重复添加
					boolean exists = false;
					for (DiffBlock.TextRange existing : diffRangesA) {
						if (existing.start == range.start && existing.end == range.end
								&& existing.type.equals(range.type)) {
							exists = true;
							break;
						}
					}
					if (!exists) {
						diffRangesA.add(new DiffBlock.TextRange(range.start, // 保持原有位置，不加偏移
								range.end, // 保持原有位置，不加偏移
								range.type));
					}
				}
			}
			if (block.diffRangesB != null) {
				for (DiffBlock.TextRange range : block.diffRangesB) {
					// 检查是否已存在相同的范围，避免重复添加
					boolean exists = false;
					for (DiffBlock.TextRange existing : diffRangesB) {
						if (existing.start == range.start && existing.end == range.end
								&& existing.type.equals(range.type)) {
							exists = true;
							break;
						}
					}
					if (!exists) {
						diffRangesB.add(new DiffBlock.TextRange(range.start, // 保持原有位置，不加偏移
								range.end, // 保持原有位置，不加偏移
								range.type));
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
			merged.prevOldBboxes = firstWithPrev2.prevOldBboxes == null ? null
					: new ArrayList<>(firstWithPrev2.prevOldBboxes);
			merged.prevNewBboxes = firstWithPrev2.prevNewBboxes == null ? null
					: new ArrayList<>(firstWithPrev2.prevNewBboxes);
		}
		// 保留第一个带 prev* 的来源，保证前端跳转链
		if (firstWithPrev != null) {
			merged.prevOldBboxes = firstWithPrev.prevOldBboxes == null ? null
					: new ArrayList<>(firstWithPrev.prevOldBboxes);
			merged.prevNewBboxes = firstWithPrev.prevNewBboxes == null ? null
					: new ArrayList<>(firstWithPrev.prevNewBboxes);
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
		if (block == null)
			return false;
		if (block.oldBboxes != null)
			count += block.oldBboxes.size();
		if (block.newBboxes != null)
			count += block.newBboxes.size();
		return count == 1;
	}

	private boolean haveIdenticalBbox(DiffBlock a, DiffBlock b) {
		// 如果任意一侧（old/new）的任意 bbox 在两个 block 中完全一致，则认为存在相同内容块
		List<double[]> aAll = a.getAllBboxes();
		List<double[]> bAll = b.getAllBboxes();
		if (aAll == null || bAll == null || aAll.isEmpty() || bAll.isEmpty())
			return false;
		for (double[] x : aAll) {
			for (double[] y : bAll) {
				if (bboxEquals(x, y))
					return true;
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
	 * 向目标列表追加bbox并去重（按坐标容差 / IoU 近似去重）
	 */
	private void addBboxesUnique(List<double[]> target, List<double[]> incoming, double tolPx) {
		if (incoming == null || incoming.isEmpty())
			return;
		if (target == null)
			return;
		for (double[] cand : incoming) {
			boolean exists = false;
			for (double[] exist : target) {
				if (approxSameBox(exist, cand, tolPx)) {
					exists = true;
					break;
				}
			}
			if (!exists)
				target.add(cand);
		}
	}

	private boolean approxSameBox(double[] a, double[] b, double tolPx) {
		if (a == null || b == null || a.length < 4 || b.length < 4)
			return false;
		boolean close = Math.abs(a[0] - b[0]) <= tolPx && Math.abs(a[1] - b[1]) <= tolPx
				&& Math.abs(a[2] - b[2]) <= tolPx && Math.abs(a[3] - b[3]) <= tolPx;
		if (close)
			return true;
		// 再用 IoU 判定高度重合
		double inter = intersectArea(a, b);
		if (inter <= 0)
			return false;
		double areaA = Math.max(0, (a[2] - a[0])) * Math.max(0, (a[3] - a[1]));
		double areaB = Math.max(0, (b[2] - b[0])) * Math.max(0, (b[3] - b[1]));
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
            // 页码数组：跨页时取最小页码作为主显示页
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
			Path imagePath = Paths.get(uploadRootPath, "gpu-ocr-compare", "tasks", taskId, "images", mode, "page-" + pageNumber + ".png");
			
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
	 * 从保存的图片文件中读取图片宽度
	 * @param pdfPath PDF文件路径（用于推断任务ID）
	 * @param pageNumber 页码（从1开始）
	 * @return 图片宽度，如果读取失败返回0
	 */
	private double getImageWidthFromSavedFile(Path pdfPath, int pageNumber) {
		try {
			// 从PDF路径推断任务ID和文档类型
			String taskId = extractTaskIdFromPath(pdfPath);
			String mode = extractModeFromPath(pdfPath);
			
			if (taskId == null || mode == null) {
				return 0;
			}
			
			// 构建图片文件路径
			String uploadRootPath = zxcmConfig.getFileUpload().getRootPath();
			Path imagePath = Paths.get(uploadRootPath, "gpu-ocr-compare", "tasks", taskId, "images", mode, "page-" + pageNumber + ".png");
			
			if (!Files.exists(imagePath)) {
				return 0;
			}
			
			BufferedImage image = ImageIO.read(imagePath.toFile());
			if (image != null) {
				return image.getWidth();
			}
		} catch (Exception e) {
			System.err.println("读取图片宽度失败: " + e.getMessage());
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
}
