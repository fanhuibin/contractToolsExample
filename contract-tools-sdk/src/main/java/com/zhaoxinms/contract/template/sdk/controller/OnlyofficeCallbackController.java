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
            // 获取文件信息
            FileInfo fileInfo = fileInfoService.getById(fileId);
            if (fileInfo == null) {
                throw new RuntimeException("文件不存在，文件ID: " + fileId);
            }
            
            // 使用 fileInfoService.getFileDiskPath 方法，自动处理相对路径转绝对路径
            String filePath = fileInfoService.getFileDiskPath(fileId);
            if (filePath == null || filePath.isEmpty()) {
                throw new RuntimeException("无法获取文件路径，文件ID: " + fileId);
            }
            
            java.io.File file = new java.io.File(filePath);
            if (!file.exists()) {
                throw new RuntimeException("文件不存在于磁盘，文件路径: " + filePath);
            }
            
			// 创建文件资源
			Resource resource = new FileSystemResource(file);
			
			// 设置响应头
			HttpHeaders headers = new HttpHeaders();
            String originalName = fileInfo.getOriginalName();
            String encodedFileName = java.net.URLEncoder.encode(originalName, "UTF-8").replace("+", "%20");
            // 只使用RFC 5987标准的filename*，避免中文在filename部分导致编码错误
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName);
			
			// 根据文件扩展名设置Content-Type
            String contentType = getContentType(fileInfo.getFileExtension());
			headers.setContentType(MediaType.parseMediaType(contentType));
			
			return ResponseEntity.ok()
					.headers(headers)
					.body(resource);
		} catch (Exception e) {
			throw new RuntimeException("文件下载失败：" + e.getMessage());
		}
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