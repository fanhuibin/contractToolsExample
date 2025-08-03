package com.zhaoxinms.contract.tools.onlyoffice.controller;

import com.zhaoxinms.contract.tools.onlyoffice.callbacks.CallbackHandler;
import com.zhaoxinms.contract.tools.onlyoffice.dto.Track;
import com.zhaoxinms.contract.tools.onlyoffice.util.JwtManager;
import com.zhaoxinms.contract.tools.common.entity.FileInfo;
import com.zhaoxinms.contract.tools.common.service.FileInfoService;
import com.zhaoxinms.contract.tools.common.Result;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * OnlyOffice回调控制器
 * 处理OnlyOffice文档编辑器的回调请求
 */
@Validated
@RestController
@RequestMapping("/onlyoffice/callback")
public class OnlyofficeCallbackController {

    @Autowired
    private FileInfoService fileInfoService;
    
    @Autowired
    private JwtManager jwtManager;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private CallbackHandler callbackHandler;

    @Value("${file.upload.root-path}")
    private String uploadPath;

    /**
     * 文档保存回调接口
     * OnlyOffice在文档保存时会调用此接口
     * 
     * @param request HTTP请求
     * @param body 回调数据
     * @param fileId 文件ID
     * @return 处理结果
     */
    @RequestMapping(value = "/save", method = RequestMethod.POST)
    @ResponseBody
    public String save(HttpServletRequest request, @RequestBody Track body, String fileId) {
        try {
            // 验证请求体
            String bodyString = objectMapper.writeValueAsString(body);
            if (bodyString.isEmpty()) {
                return "{\"error\":1,\"message\":\"Request payload is empty\"}";
            }

            // 验证JWT签名（如果启用）
            String header = request.getHeader("Authorization");
            try {
                org.json.simple.JSONObject bodyCheck = jwtManager.parseBody(bodyString, header);
                body = objectMapper.readValue(bodyCheck.toJSONString(), Track.class);
            } catch (Exception e) {
                // JWT验证失败，根据配置决定是否继续处理
                System.err.println("JWT validation failed: " + e.getMessage());
            }

            // 处理回调
            int error = callbackHandler.handle(body, fileId, null);
            return "{\"error\":" + error + "}";
            
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\":1,\"message\":\"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 文件下载接口
     * OnlyOffice访问此接口下载文档内容
     * 
     * @param fileId 文件ID
     * @param request HTTP请求
     * @param response HTTP响应
     * @throws IOException IO异常
     */
    @GetMapping("/download/{fileId}")
    public void download(@PathVariable String fileId, 
                        HttpServletRequest request, 
                        HttpServletResponse response) throws IOException {
        
        // 获取文件信息
        FileInfo fileInfo = fileInfoService.getById(Long.valueOf(fileId));
        if (fileInfo == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("File not found");
            return;
        }

        // 构建文件路径
        Path filePath = Paths.get(uploadPath, fileInfo.getStorePath());
        if (!Files.exists(filePath)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("Physical file not found");
            return;
        }

        // 设置响应头
        response.setContentType(getContentType(fileInfo.getOriginalName()));
        response.setHeader("Content-Disposition", 
                          "attachment; filename=\"" + fileInfo.getOriginalName() + "\"");
        response.setContentLengthLong(Files.size(filePath));

        // 输出文件内容
        Files.copy(filePath, response.getOutputStream());
        response.getOutputStream().flush();
    }

    /**
     * 健康检查接口
     * 
     * @return 服务状态
     */
    @GetMapping("/health")
    @ResponseBody
    public Result<String> health() {
        return Result.success("OnlyOffice callback service is running");
    }

    /**
     * 根据文件名获取MIME类型
     * 
     * @param fileName 文件名
     * @return MIME类型
     */
    private String getContentType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        switch (extension) {
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
            default:
                return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
    }
}