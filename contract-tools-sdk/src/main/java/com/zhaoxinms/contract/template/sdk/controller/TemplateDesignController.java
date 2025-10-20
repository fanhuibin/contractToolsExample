package com.zhaoxinms.contract.template.sdk.controller;

import com.zhaoxinms.contract.tools.api.common.ApiCode;
import com.zhaoxinms.contract.tools.api.common.ApiResponse;
import com.zhaoxinms.contract.tools.api.dto.FieldResponse;
import com.zhaoxinms.contract.tools.api.dto.TemplateDesignRequest;
import com.zhaoxinms.contract.tools.api.dto.TemplateDesignResponse;
import com.zhaoxinms.contract.tools.api.exception.BusinessException;
import com.zhaoxinms.contract.tools.auth.annotation.RequireFeature;
import com.zhaoxinms.contract.tools.auth.enums.ModuleType;
import com.zhaoxinms.contract.tools.api.service.TemplateDesignService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import com.zhaoxinms.contract.template.sdk.entity.TemplateDesignRecord;
import com.zhaoxinms.contract.template.sdk.service.TemplateDesignRecordService;
import com.zhaoxinms.contract.tools.common.service.FileInfoService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 文档在线编辑控制器
 * 
 * 基于OnlyOffice的在线文档编辑器，支持Word模板设计和管理
 * 
 * @author zhaoxin
 * @since 2024-10-09
 */
@RestController
@RequestMapping("/api/template")
@Api(tags = "文档在线编辑")
@RequireFeature(module = ModuleType.DOCUMENT_ONLINE_EDIT, message = "文档在线编辑功能需要授权")
public class TemplateDesignController { 

    @Autowired
    private TemplateDesignService templateDesignService;
    
    @Value("${zxcm.file-upload.root-path:./uploads}")
    private String uploadRootPath;

    @Autowired(required = false)
    private TemplateDesignRecordService designRecordService;

    @Autowired(required = false)
    private FileInfoService fileInfoService;

    /**
     * 获取字段信息
     */
    @GetMapping("/fields")
    @ApiOperation(value = "获取字段信息", notes = "获取模板设计中可使用的字段列表")
    public ApiResponse<FieldResponse> getFields() {
        FieldResponse response = templateDesignService.getFields();
        return ApiResponse.success(response);
    }

    /**
     * 保存模板设计元素
     */
    @PostMapping("/design/save")
    @ApiOperation(value = "保存模板设计", notes = "保存模板设计的元素配置")
    public ApiResponse<TemplateDesignRecord> saveDesign(
            @ApiParam(value = "设计记录", required = true) @RequestBody TemplateDesignRecord body) {
        
        if (designRecordService == null) {
            throw BusinessException.of(ApiCode.SERVER_ERROR, "设计记录服务不可用");
        }
        
        TemplateDesignRecord saved = designRecordService.save(
            body.getId(), 
            body.getTemplateId(), 
            body.getFileId(), 
            body.getElementsJson()
        );
        
        return ApiResponse.success("保存成功", saved);
    }

    /**
     * 获取模板设计明细
     */
    @GetMapping("/design/{id}")
    @ApiOperation(value = "获取设计详情", notes = "根据记录ID获取模板设计详情")
    public ApiResponse<TemplateDesignRecord> getDesignDetail(
            @ApiParam(value = "记录ID", required = true) @PathVariable("id") String id) {
        
        if (designRecordService == null) {
            throw BusinessException.of(ApiCode.SERVER_ERROR, "设计记录服务不可用");
        }
        
        TemplateDesignRecord record = designRecordService.getById(id);
        if (record == null) {
            throw BusinessException.of(ApiCode.NOT_FOUND, "未找到设计记录: " + id);
        }
        
        return ApiResponse.success(record);
    }

    /**
     * 根据模板ID获取模板设计明细
     */
    @GetMapping("/design/byTemplate/{templateId}")
    @ApiOperation(value = "根据模板ID获取设计", notes = "根据模板ID获取对应的设计记录")
    public ApiResponse<TemplateDesignRecord> getDesignDetailByTemplateId(
            @ApiParam(value = "模板ID", required = true) @PathVariable("templateId") String templateId) {
        
        if (designRecordService == null) {
            throw BusinessException.of(ApiCode.SERVER_ERROR, "设计记录服务不可用");
        }
        
        TemplateDesignRecord record = designRecordService.getByTemplateId(templateId);
        if (record == null) {
            throw BusinessException.of(ApiCode.TEMPLATE_NOT_FOUND, "未找到模板设计记录: " + templateId);
        }
        
        return ApiResponse.success(record);
    }

    /**
     * 发起模板设计
     */
    @PostMapping("/design/start")
    @ApiOperation(value = "发起模板设计", notes = "启动OnlyOffice编辑器进行模板设计")
    public ApiResponse<TemplateDesignResponse> startTemplateDesign(
            @ApiParam(value = "模板设计请求", required = true) 
            @Valid @RequestBody TemplateDesignRequest request) {
        
        TemplateDesignResponse response = templateDesignService.startTemplateDesign(request);
        return ApiResponse.success(response);
    }

    /**
     * 上传模板文档
     */
    @PostMapping(value = "/design/upload", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiOperation(value = "上传模板文档", notes = "上传Word模板文件（仅支持docx格式）")
    public ApiResponse<TemplateDesignRecord> uploadTemplate(
            @ApiParam("模板ID") @RequestParam("templateId") String templateId,
            @ApiParam("Word模板文件，仅支持docx") @RequestPart("file") org.springframework.web.multipart.MultipartFile file) {
        
        // 参数校验
        if (file == null || file.isEmpty()) {
            throw BusinessException.of(ApiCode.FILE_EMPTY);
        }
        
        if (templateId == null || templateId.trim().isEmpty()) {
            throw BusinessException.of(ApiCode.PARAM_ERROR, "模板ID不能为空");
        }
        
        // 文件格式验证
        String filename = file.getOriginalFilename();
        String ext = filename != null && filename.contains(".") 
            ? filename.substring(filename.lastIndexOf('.') + 1).toLowerCase() 
            : "";
        
        if (!"docx".equals(ext)) {
            throw BusinessException.of(ApiCode.FILE_TYPE_NOT_SUPPORTED, 
                "仅支持docx文件，请将doc转换为docx再上传");
        }

        try {
            // 保存到uploads目录
            Path root = Paths.get(uploadRootPath).toAbsolutePath().normalize();
            Files.createDirectories(root);
            String ts = String.valueOf(System.currentTimeMillis());
            java.io.File dest = root.resolve("templates/" + ts + ".docx").toFile();
            if (!dest.getParentFile().exists()) dest.getParentFile().mkdirs();
            file.transferTo(dest);

            // 注册文件
            if (fileInfoService == null) {
                throw BusinessException.of(ApiCode.SERVER_ERROR, "文件服务不可用，无法注册上传文件");
            }
            com.zhaoxinms.contract.tools.common.entity.FileInfo info = 
                fileInfoService.registerFile(filename, "docx", dest.getAbsolutePath(), dest.length());

            // 保存设计记录（elementsJson 初始为空结构）
            if (designRecordService == null) {
                throw BusinessException.of(ApiCode.SERVER_ERROR, "设计记录服务不可用");
            }
            
            String elementsJson = "{\"elements\":[]}";
            TemplateDesignRecord saved;
            try {
                saved = designRecordService.save(null, templateId.trim(), String.valueOf(info.getId()), elementsJson);
            } catch (IllegalArgumentException ex) {
                throw BusinessException.of(ApiCode.PARAM_ERROR, ex.getMessage());
            }
            
            return ApiResponse.success("上传成功", saved);
            
        } catch (BusinessException e) {
            throw e;  // 重新抛出业务异常
        } catch (Exception e) {
            throw BusinessException.of(ApiCode.FILE_UPLOAD_ERROR, "上传失败: " + e.getMessage());
        }
    }

    /**
     * 模板设计记录列表
     */
    @GetMapping("/design/list")
    @ApiOperation(value = "获取设计列表", notes = "获取所有模板设计记录列表")
    public ApiResponse<List<TemplateDesignRecord>> listDesigns() {
        if (designRecordService == null) {
            throw BusinessException.of(ApiCode.SERVER_ERROR, "设计记录服务不可用");
        }
        
        List<TemplateDesignRecord> list = designRecordService.listAll();
        return ApiResponse.success(list);
    }

    /**
     * 删除模板设计记录
     */
    @DeleteMapping("/design/{id}")
    @ApiOperation(value = "删除设计记录", notes = "删除指定的模板设计记录")
    public ApiResponse<Void> deleteDesign(
            @ApiParam(value = "记录ID", required = true) @PathVariable("id") String id) {
        
        if (designRecordService == null) {
            throw BusinessException.of(ApiCode.SERVER_ERROR, "设计记录服务不可用");
        }
        
        boolean ok = designRecordService.deleteById(id);
        if (!ok) {
            throw BusinessException.of(ApiCode.NOT_FOUND, "删除失败或记录不存在: " + id);
        }
        
        return ApiResponse.success();
    }
}
