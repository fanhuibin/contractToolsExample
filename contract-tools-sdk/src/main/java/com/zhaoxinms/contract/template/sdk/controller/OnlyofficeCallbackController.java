package com.zhaoxinms.contract.template.sdk.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhaoxinms.contract.tools.onlyoffice.dto.Track;
import com.zhaoxinms.contract.tools.onlyoffice.util.JwtManager;
import com.zhaoxinms.contract.tools.onlyoffice.callbacks.CallbackHandler;
import org.springframework.web.bind.annotation.RequestBody;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.json.simple.JSONObject;
// removed unused imports
import com.zhaoxinms.contract.tools.api.common.ApiResponse;
import com.zhaoxinms.contract.tools.common.entity.FileInfo;
import com.zhaoxinms.contract.tools.common.service.FileInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OnlyOffice回调控制器 SDK项目中的OnlyOffice回调处理，接收OnlyOffice的回调请求
 */
@RestController
@RequestMapping("/api/onlyoffice/callback")
public class OnlyofficeCallbackController {

    private static final Logger log = LoggerFactory.getLogger(OnlyofficeCallbackController.class);

	@Autowired
	private ObjectMapper objectMapper;
	@Autowired
	private JwtManager jwtManager;
	@Autowired
	private CallbackHandler callbackHandler;
	@Autowired
	@Qualifier("fileInfoServiceImpl")
	private FileInfoService fileInfoService;

	@Value("${zxcm.file-upload.root-path:./uploads}")
	private String uploadRootPath;

	/**
	 * 兼容 query 参数的保存接口，支持 /save?fileId=1
	 * Onlyoffice 回调配置如需用 query 参数时可用。
	 */
	@PostMapping("/save")
	@ResponseBody
	public String saveFileByQuery(@RequestParam("fileId") String fileId, @RequestBody Track body, HttpServletRequest request) {
		try {
			String bodyString = objectMapper.writeValueAsString(body);
			String header = request.getHeader("Authorization");
			if (bodyString.isEmpty()) {
				return "{\"error\":1,\"message\":\"Request payload is empty\"}";
			}
			JSONObject bodyCheck = jwtManager.parseBody(bodyString, header);
			Track track = objectMapper.readValue(bodyCheck.toJSONString(), Track.class);
			int error = callbackHandler.handle(track, fileId);
			return "{\"error\":" + error + "}";
		} catch (Exception e) {
			e.printStackTrace();
			return "{\"error\":1,\"message\":\"" + e.getMessage() + "\"}";
		}
	}

	/**
	 * 下载文档
	 * 
	 * @param fileId 文件ID
	 * @return 文件流
	 */
    @GetMapping("/download/{fileId}")
    public ResponseEntity<Resource> download(@PathVariable String fileId) {
		try {
            // 获取文件信息；若拿不到且是模板设计固定文件，走兜底
            FileInfo fileInfo = fileInfoService.getById(fileId);
            java.io.File file = null;
            String downloadName = null;
            if (fileInfo == null) {
                if ("templateDesign".equals(fileId) || "templateDesign.docx".equalsIgnoreCase(fileId)) {
                    file = resolveTemplateDesignFile();
                    downloadName = "templateDesign.docx";
                    if (file == null || !file.exists()) {
                        throw new RuntimeException("模板设计示例文件不存在，请在uploads或sdk/uploads放置templateDesign.docx或test_document.docx");
                    }
                } else {
                    throw new RuntimeException("文件不存在，文件ID: " + fileId);
                }
            } else {
                // 优先使用storePath，其次使用默认uploads目录
                String storePath = fileInfo.getStorePath();
                if (storePath != null) {
                    file = new java.io.File(storePath);
                }
                if (file == null || !file.exists()) {
                    java.nio.file.Path filePath = java.nio.file.Paths.get(uploadRootPath, fileInfo.getFileName());
                    file = filePath.toFile();
                }
                if (!file.exists()) {
                    throw new RuntimeException("文件不存在于磁盘，文件路径: " + (storePath != null ? storePath : (uploadRootPath + "/" + fileInfo.getFileName())));
                }
                downloadName = fileInfo.getOriginalName();
            }
			// 创建文件资源
			Resource resource = new FileSystemResource(file);
			
			// 设置响应头
			HttpHeaders headers = new HttpHeaders();
            String originalName = downloadName != null ? downloadName : file.getName();
            String encodedFileName = java.net.URLEncoder.encode(originalName, "UTF-8").replace("+", "%20");
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + originalName + "\"; filename*=UTF-8''" + encodedFileName);
			
			// 根据文件扩展名设置Content-Type
            String contentType = getContentType("docx");
			headers.setContentType(MediaType.parseMediaType(contentType));
			
			return ResponseEntity.ok()
					.headers(headers)
					.body(resource);
		} catch (Exception e) {
			throw new RuntimeException("文件下载失败：" + e.getMessage());
		}
	}

    private java.io.File resolveTemplateDesignFile() {
        try {
            String absUploadPath = java.nio.file.Paths.get(uploadRootPath).toAbsolutePath().toString();
            String[] candidates = new String[] {
                absUploadPath + "/templateDesign.docx",
                absUploadPath + "/test_document.docx",
                java.nio.file.Paths.get("sdk", "uploads", "templateDesign.docx").toAbsolutePath().toString(),
                java.nio.file.Paths.get("sdk", "uploads", "test_document.docx").toAbsolutePath().toString(),
                java.nio.file.Paths.get("..", "sdk", "uploads", "templateDesign.docx").toAbsolutePath().toString(),
                java.nio.file.Paths.get("..", "sdk", "uploads", "test_document.docx").toAbsolutePath().toString(),
                java.nio.file.Paths.get("..", "..", "sdk", "uploads", "templateDesign.docx").toAbsolutePath().toString(),
                java.nio.file.Paths.get("..", "..", "sdk", "uploads", "test_document.docx").toAbsolutePath().toString()
            };
            for (String p : candidates) {
                java.io.File f = new java.io.File(p);
                if (f.exists() && f.isFile()) return f;
            }
        } catch (Exception ignore) {}
        return null;
    }

	/**
	 * 根据文件扩展名获取Content-Type
	 */
	private String getContentType(String fileExtension) {
		if (fileExtension == null) {
			return "application/octet-stream";
		}
		switch (fileExtension.toLowerCase()) {
		case "docx":
			return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
		case "doc":
			return "application/msword";
		case "xlsx":
			return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
		case "xls":
			return "application/vnd.ms-excel";
		case "pptx":
			return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
		case "ppt":
			return "application/vnd.ms-powerpoint";
		case "pdf":
			return "application/pdf";
		case "txt":
			return "text/plain";
		default:
			return "application/octet-stream";
		}
	}

	/**
	 * 健康检查
	 * 
	 * @return 健康状态
	 */
	@GetMapping("/health")
	public ApiResponse<String> health() {
		return ApiResponse.<String>success("OnlyOffice回调服务正常运行", "OK");
	}
}