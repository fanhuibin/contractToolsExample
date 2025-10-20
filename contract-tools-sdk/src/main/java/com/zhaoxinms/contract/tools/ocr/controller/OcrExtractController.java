package com.zhaoxinms.contract.tools.ocr.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhaoxinms.contract.tools.api.common.ApiCode;
import com.zhaoxinms.contract.tools.api.common.ApiResponse;
import com.zhaoxinms.contract.tools.api.exception.BusinessException;
import com.zhaoxinms.contract.tools.auth.annotation.RequireFeature;
import com.zhaoxinms.contract.tools.auth.enums.ModuleType;
import com.zhaoxinms.contract.tools.ocr.service.OcrExtractService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

/**
 * 智能文档解析控制器
 * 
 * 基于GPU OCR的文档智能解析功能，支持页眉页脚过滤和图文对照显示
 * 
 * @author zhaoxin
 * @since 2024-10-09
 */
@RestController
@RequestMapping("/api/ocr/extract")
@Slf4j
@RequireFeature(module = ModuleType.SMART_DOCUMENT_PARSE, message = "智能文档解析功能需要授权")
@Api(tags = "智能文档解析")
public class OcrExtractController {

    @Autowired
    private OcrExtractService ocrExtractService;

    @Value("${zxcm.file-upload.root-path:./uploads}")
    private String uploadRootPath;

    /**
     * 上传PDF文件进行智能解析
     */
    @PostMapping("/upload")
    @ApiOperation(value = "上传文件进行智能解析", notes = "支持页眉页脚过滤，返回任务ID")
    public ApiResponse<Map<String, Object>> uploadFile(
            @ApiParam(value = "PDF文件", required = true) @RequestParam("file") MultipartFile file,
            @ApiParam(value = "是否忽略页眉页脚", example = "true") @RequestParam(value = "ignoreHeaderFooter", defaultValue = "true") Boolean ignoreHeaderFooter,
            @ApiParam(value = "页眉高度百分比", example = "12.0") @RequestParam(value = "headerHeightPercent", defaultValue = "12.0") Double headerHeightPercent,
            @ApiParam(value = "页脚高度百分比", example = "12.0") @RequestParam(value = "footerHeightPercent", defaultValue = "12.0") Double footerHeightPercent) throws Exception {

        log.info("接收到智能解析请求，文件: {}, 忽略页眉页脚: {}, 页眉高度: {}%, 页脚高度: {}%",
                file.getOriginalFilename(), ignoreHeaderFooter, headerHeightPercent, footerHeightPercent);

        // 验证文件
        if (file == null || file.isEmpty()) {
            throw BusinessException.of(ApiCode.FILE_EMPTY);
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase().endsWith(".pdf")) {
            throw BusinessException.of(ApiCode.FILE_TYPE_NOT_SUPPORTED, "只支持PDF格式文件");
        }

        // 调用服务进行OCR提取
        String taskId = ocrExtractService.extractPdf(
                file, 
                ignoreHeaderFooter, 
                headerHeightPercent, 
                footerHeightPercent
        );

        return ApiResponse.success("文件上传成功，开始智能解析", Map.of(
            "taskId", taskId,
            "message", "文件上传成功，开始智能解析..."
        ));
    }

    /**
     * 获取任务状态
     */
    @GetMapping("/status/{taskId}")
    @ApiOperation(value = "查询任务状态", notes = "获取解析任务的执行状态和进度")
    public ApiResponse<Map<String, Object>> getTaskStatus(
            @ApiParam(value = "任务ID", required = true) @PathVariable String taskId) throws Exception {
        
        log.info("查询智能解析任务状态，任务ID: {}", taskId);

        Map<String, Object> status = ocrExtractService.getTaskStatus(taskId);
        if (status == null) {
            throw BusinessException.of(ApiCode.PARSE_TASK_NOT_FOUND, "任务不存在: " + taskId);
        }

        return ApiResponse.success(status);
    }

    /**
     * 获取解析结果
     */
    @GetMapping("/result/{taskId}")
    @ApiOperation(value = "获取解析结果", notes = "获取解析完成后的完整结果数据")
    public ApiResponse<Map<String, Object>> getResult(
            @ApiParam(value = "任务ID", required = true) @PathVariable String taskId) throws Exception {
        
        log.info("获取智能解析结果，任务ID: {}", taskId);

        // 检查任务状态
        Map<String, Object> status = ocrExtractService.getTaskStatus(taskId);
        if (status == null) {
            throw BusinessException.of(ApiCode.PARSE_TASK_NOT_FOUND, "任务不存在: " + taskId);
        }

        String taskStatus = (String) status.get("status");
        
        // 如果任务未完成，返回状态信息
        if (!"completed".equals(taskStatus)) {
            return ApiResponse.success(Map.of(
                "status", taskStatus,
                "message", status.get("message"),
                "progress", status.get("progress")
            ));
        }

        // 加载结果数据
        Map<String, Object> result = ocrExtractService.getTaskResult(taskId);
        if (result == null) {
            throw BusinessException.of(ApiCode.PARSE_RESULT_NOT_FOUND, "解析结果不存在: " + taskId);
        }

        return ApiResponse.success(result);
    }

    /**
     * 获取页面图片
     */
    @GetMapping("/page-image/{taskId}/{pageNum}")
    @ApiOperation(value = "获取页面图片", notes = "获取文档指定页的渲染图片")
    public ResponseEntity<Resource> getPageImage(
            @ApiParam(value = "任务ID", required = true) @PathVariable String taskId,
            @ApiParam(value = "页码", required = true, example = "1") @PathVariable int pageNum) {
        
        log.debug("获取页面图片，任务ID: {}, 页码: {}", taskId, pageNum);

        File imageFile = ocrExtractService.getPageImage(taskId, pageNum);
        if (imageFile == null || !imageFile.exists()) {
            throw BusinessException.of(ApiCode.FILE_NOT_FOUND, 
                String.format("页面图片不存在: taskId=%s, page=%d", taskId, pageNum));
        }

        Resource resource = new FileSystemResource(imageFile);
        
        // 根据文件扩展名设置Content-Type
        String contentType = MediaType.IMAGE_PNG_VALUE;
        if (imageFile.getName().toLowerCase().endsWith(".jpg") || 
            imageFile.getName().toLowerCase().endsWith(".jpeg")) {
            contentType = MediaType.IMAGE_JPEG_VALUE;
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + imageFile.getName() + "\"")
                .body(resource);
    }

    /**
     * 获取TextBox数据
     */
    @GetMapping("/textboxes/{taskId}")
    @ApiOperation(value = "获取TextBox数据", notes = "获取文档的文本块标注数据")
    public ApiResponse<Object> getTextBoxes(
            @ApiParam(value = "任务ID", required = true) @PathVariable String taskId) throws Exception {
        
        Object textBoxes = ocrExtractService.getTextBoxes(taskId);
        if (textBoxes == null) {
            throw BusinessException.of(ApiCode.PARSE_RESULT_NOT_FOUND, "TextBox数据不存在: " + taskId);
        }

        return ApiResponse.success(textBoxes);
    }

    /**
     * 获取Bbox映射数据（用于处理跨页表格等）
     */
    @GetMapping("/bbox-mappings/{taskId}")
    @ApiOperation(value = "获取Bbox映射数据", notes = "获取跨页表格等特殊布局的边界框映射关系")
    public ApiResponse<Object> getBboxMappings(
            @ApiParam(value = "任务ID", required = true) @PathVariable String taskId) {
        
        log.info("获取Bbox映射数据，任务ID: {}", taskId);
        
        File taskDir = new File(uploadRootPath, "ocr-extract-tasks/" + taskId);
        File bboxMappingFile = new File(taskDir, "bbox_mappings.json");
        
        if (!bboxMappingFile.exists()) {
            log.warn("BboxMapping文件不存在: {}", bboxMappingFile.getAbsolutePath());
            // 返回空数组而不是抛异常，因为bbox_mappings是可选的
            return ApiResponse.success(new ArrayList<>());
        }
        
        try {
            // 读取BboxMapping数据
            ObjectMapper objectMapper = new ObjectMapper();
            Object bboxMappingData = objectMapper.readValue(bboxMappingFile, Object.class);
            
            log.debug("成功获取BboxMapping数据: {}", taskId);
            return ApiResponse.success(bboxMappingData);
            
        } catch (Exception e) {
            log.error("读取BboxMapping数据失败: {}", taskId, e);
            throw BusinessException.of(ApiCode.FILE_READ_ERROR, "读取数据失败: " + e.getMessage());
        }
    }

    /**
     * 删除任务
     */
    @DeleteMapping("/task/{taskId}")
    @ApiOperation(value = "删除任务", notes = "删除解析任务及相关文件")
    public ApiResponse<Void> deleteTask(
            @ApiParam(value = "任务ID", required = true) @PathVariable String taskId) {
        
        log.info("删除智能解析任务，任务ID: {}", taskId);

        try {
            ocrExtractService.deleteTask(taskId);
            return ApiResponse.success();
        } catch (Exception e) {
            log.error("删除任务失败: {}", taskId, e);
            throw BusinessException.of(ApiCode.SERVER_ERROR, "删除任务失败: " + e.getMessage());
        }
    }
}
