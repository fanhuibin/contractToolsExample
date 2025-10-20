package com.zhaoxinms.contract.tools.api.example;

import com.zhaoxinms.contract.tools.api.common.ApiCode;
import com.zhaoxinms.contract.tools.api.common.ApiResponse;
import com.zhaoxinms.contract.tools.api.common.PageData;
import com.zhaoxinms.contract.tools.api.common.PageQuery;
import com.zhaoxinms.contract.tools.api.exception.BusinessException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

/**
 * 示例Controller - 展示API规范的标准用法
 * 
 * 本Controller展示了所有常见场景的API实现方式：
 * 1. CRUD操作
 * 2. 分页查询
 * 3. 文件上传/下载
 * 4. 批量操作
 * 5. 异常处理
 * 
 * @author zhaoxin
 * @since 2025-01-18
 */
@Slf4j
@RestController
@RequestMapping("/api/example")
@RequiredArgsConstructor
@Api(tags = "示例API（仅供参考）")
public class ExampleController {
    
    // ==================== CRUD操作 ====================
    
    /**
     * 查询列表（不分页）
     */
    @GetMapping
    @ApiOperation(value = "查询列表", notes = "获取所有资源列表")
    public ApiResponse<List<ExampleDTO>> list(
            @ApiParam("关键词") @RequestParam(required = false) String keyword,
            @ApiParam("状态") @RequestParam(required = false) String status) {
        
        // 业务逻辑
        List<ExampleDTO> list = List.of(
            new ExampleDTO("1", "示例1"),
            new ExampleDTO("2", "示例2")
        );
        
        return ApiResponse.success(list);
    }
    
    /**
     * 分页查询
     */
    @GetMapping("/page")
    @ApiOperation(value = "分页查询", notes = "支持分页、排序、搜索")
    public ApiResponse<PageData<ExampleDTO>> page(@Valid PageQuery query) {
        
        // 模拟分页数据
        List<ExampleDTO> records = List.of(
            new ExampleDTO("1", "示例1"),
            new ExampleDTO("2", "示例2")
        );
        
        PageData<ExampleDTO> pageData = new PageData<>(
            records, 
            query.getCurrent(), 
            query.getSize(), 
            100L
        );
        
        return ApiResponse.success(pageData);
    }
    
    /**
     * 查询详情
     */
    @GetMapping("/{id}")
    @ApiOperation(value = "查询详情", notes = "根据ID获取资源详情")
    public ApiResponse<ExampleDTO> get(
            @ApiParam(value = "资源ID", required = true) @PathVariable String id) {
        
        // 模拟查询
        ExampleDTO dto = new ExampleDTO(id, "示例名称");
        
        // 如果不存在，抛出异常
        if (dto == null) {
            throw BusinessException.of(ApiCode.NOT_FOUND, "资源不存在: " + id);
        }
        
        return ApiResponse.success(dto);
    }
    
    /**
     * 创建资源
     */
    @PostMapping
    @ApiOperation(value = "创建资源", notes = "创建新的资源")
    public ApiResponse<ExampleDTO> create(
            @ApiParam(value = "请求参数", required = true) 
            @Valid @RequestBody CreateRequest request) {
        
        // 参数校验（@Valid会自动处理，这里是额外的业务校验）
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw BusinessException.of(ApiCode.PARAM_ERROR, "名称不能为空");
        }
        
        // 业务逻辑
        ExampleDTO dto = new ExampleDTO("new-id", request.getName());
        
        return ApiResponse.success("创建成功", dto);
    }
    
    /**
     * 更新资源（完整更新）
     */
    @PutMapping("/{id}")
    @ApiOperation(value = "更新资源", notes = "完整更新资源信息")
    public ApiResponse<ExampleDTO> update(
            @ApiParam(value = "资源ID", required = true) @PathVariable String id,
            @ApiParam(value = "请求参数", required = true) 
            @Valid @RequestBody UpdateRequest request) {
        
        // 检查资源是否存在
        ExampleDTO existing = new ExampleDTO(id, "旧名称");
        if (existing == null) {
            throw BusinessException.of(ApiCode.NOT_FOUND, "资源不存在: " + id);
        }
        
        // 更新逻辑
        ExampleDTO updated = new ExampleDTO(id, request.getName());
        
        return ApiResponse.success("更新成功", updated);
    }
    
    /**
     * 部分更新资源
     */
    @PatchMapping("/{id}")
    @ApiOperation(value = "部分更新", notes = "仅更新提供的字段")
    public ApiResponse<ExampleDTO> patch(
            @ApiParam(value = "资源ID", required = true) @PathVariable String id,
            @ApiParam(value = "请求参数") @RequestBody Map<String, Object> updates) {
        
        // 部分更新逻辑
        ExampleDTO updated = new ExampleDTO(id, "更新后的名称");
        
        return ApiResponse.success("更新成功", updated);
    }
    
    /**
     * 删除资源
     */
    @DeleteMapping("/{id}")
    @ApiOperation(value = "删除资源", notes = "根据ID删除资源")
    public ApiResponse<Void> delete(
            @ApiParam(value = "资源ID", required = true) @PathVariable String id) {
        
        // 删除逻辑
        log.info("删除资源: {}", id);
        
        return ApiResponse.success();
    }
    
    // ==================== 文件操作 ====================
    
    /**
     * 上传文件
     */
    @PostMapping("/upload")
    @ApiOperation(value = "上传文件", notes = "支持PDF、Word、图片等格式")
    public ApiResponse<Map<String, String>> upload(
            @ApiParam(value = "文件", required = true) @RequestParam("file") MultipartFile file,
            @ApiParam("文件类型") @RequestParam(required = false) String type) {
        
        // 文件校验
        if (file == null || file.isEmpty()) {
            throw BusinessException.of(ApiCode.FILE_EMPTY);
        }
        
        // 文件大小校验（例如：最大100MB）
        long maxSize = 100 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw BusinessException.of(ApiCode.FILE_SIZE_EXCEEDED, 
                String.format("文件大小超过限制（最大%dMB）", maxSize / 1024 / 1024));
        }
        
        // 文件类型校验
        String contentType = file.getContentType();
        List<String> allowedTypes = List.of("application/pdf", "image/png", "image/jpeg");
        if (contentType != null && !allowedTypes.contains(contentType)) {
            throw BusinessException.of(ApiCode.FILE_TYPE_NOT_SUPPORTED, 
                "不支持的文件类型: " + contentType);
        }
        
        // 上传逻辑
        log.info("上传文件: {}, 大小: {}", file.getOriginalFilename(), file.getSize());
        String fileId = "file-" + System.currentTimeMillis();
        
        return ApiResponse.success("上传成功", Map.of(
            "fileId", fileId,
            "fileName", file.getOriginalFilename(),
            "fileSize", String.valueOf(file.getSize())
        ));
    }
    
    /**
     * 下载文件
     */
    @GetMapping("/download/{fileId}")
    @ApiOperation(value = "下载文件", notes = "根据文件ID下载文件")
    public ResponseEntity<Resource> download(
            @ApiParam(value = "文件ID", required = true) @PathVariable String fileId) {
        
        // 获取文件资源
        // Resource resource = fileService.getResource(fileId);
        
        // 如果文件不存在
        // if (resource == null || !resource.exists()) {
        //     throw BusinessException.of(ApiCode.FILE_NOT_FOUND, "文件不存在: " + fileId);
        // }
        
        // 返回文件
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"example.pdf\"")
                .body(null);  // 实际应返回resource
    }
    
    // ==================== 批量操作 ====================
    
    /**
     * 批量创建
     */
    @PostMapping("/batch")
    @ApiOperation(value = "批量创建", notes = "批量创建多个资源")
    public ApiResponse<List<ExampleDTO>> batchCreate(
            @ApiParam(value = "请求列表", required = true) 
            @Valid @RequestBody List<CreateRequest> requests) {
        
        if (requests == null || requests.isEmpty()) {
            throw BusinessException.of(ApiCode.PARAM_ERROR, "请求列表不能为空");
        }
        
        // 批量创建逻辑
        List<ExampleDTO> results = List.of();
        
        return ApiResponse.success("批量创建成功", results);
    }
    
    /**
     * 批量删除
     */
    @DeleteMapping("/batch")
    @ApiOperation(value = "批量删除", notes = "根据ID列表批量删除")
    public ApiResponse<Void> batchDelete(
            @ApiParam(value = "ID列表", required = true) @RequestBody List<String> ids) {
        
        if (ids == null || ids.isEmpty()) {
            throw BusinessException.of(ApiCode.PARAM_ERROR, "ID列表不能为空");
        }
        
        // 批量删除逻辑
        log.info("批量删除: {}", ids);
        
        return ApiResponse.success();
    }
    
    // ==================== 异步任务 ====================
    
    /**
     * 提交异步任务
     */
    @PostMapping("/task")
    @ApiOperation(value = "提交任务", notes = "提交异步处理任务")
    public ApiResponse<Map<String, String>> submitTask(
            @ApiParam(value = "任务参数") @Valid @RequestBody TaskRequest request) {
        
        // 创建任务
        String taskId = "task-" + System.currentTimeMillis();
        
        // 异步执行任务
        log.info("提交任务: {}", taskId);
        
        return ApiResponse.success("任务已提交", Map.of("taskId", taskId));
    }
    
    /**
     * 查询任务状态
     */
    @GetMapping("/task/{taskId}")
    @ApiOperation(value = "查询任务状态", notes = "获取任务执行状态和进度")
    public ApiResponse<TaskStatus> getTaskStatus(
            @ApiParam(value = "任务ID", required = true) @PathVariable String taskId) {
        
        // 查询任务状态
        TaskStatus status = new TaskStatus();
        status.setTaskId(taskId);
        status.setStatus("processing");
        status.setProgress(50);
        status.setMessage("任务执行中...");
        
        return ApiResponse.success(status);
    }
    
    // ==================== DTO定义 ====================
    
    @Data
    public static class ExampleDTO {
        private String id;
        private String name;
        
        public ExampleDTO(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }
    
    @Data
    public static class CreateRequest {
        @NotBlank(message = "名称不能为空")
        private String name;
        
        private String description;
    }
    
    @Data
    public static class UpdateRequest {
        @NotBlank(message = "名称不能为空")
        private String name;
        
        private String description;
    }
    
    @Data
    public static class TaskRequest {
        @NotBlank(message = "任务类型不能为空")
        private String type;
        
        private Map<String, Object> params;
    }
    
    @Data
    public static class TaskStatus {
        private String taskId;
        private String status;
        private Integer progress;
        private String message;
    }
}

