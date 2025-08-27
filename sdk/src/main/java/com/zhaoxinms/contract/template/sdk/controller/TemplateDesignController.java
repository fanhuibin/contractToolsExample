package com.zhaoxinms.contract.template.sdk.controller;

import com.zhaoxinms.contract.tools.api.dto.FieldResponse;
import com.zhaoxinms.contract.tools.api.dto.TemplateDesignRequest;
import com.zhaoxinms.contract.tools.api.dto.TemplateDesignResponse;
import com.zhaoxinms.contract.tools.common.Result;
import com.zhaoxinms.contract.tools.api.service.TemplateDesignService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
// removed unused imports after refactor
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
// removed unused imports after refactor
import com.zhaoxinms.contract.template.sdk.entity.TemplateDesignRecord;
import com.zhaoxinms.contract.template.sdk.service.TemplateDesignRecordService;
import com.zhaoxinms.contract.tools.common.service.FileInfoService;
// removed unused imports after refactor

/**
 * 模板设计API控制器
 * SDK项目的主要控制器，与Frontend配合实现功能
 */
@RestController
@RequestMapping("/api/template")
@Api(tags = "模板设计API")
public class TemplateDesignController { 

    @Autowired
    private TemplateDesignService templateDesignService;
    
    // FileInfoService 预留字段移除，避免未使用告警
    
    @Value("${zxcm.file.upload.root-path:./uploads}")
    private String uploadRootPath;

    @Autowired(required = false)
    private TemplateDesignRecordService designRecordService;

    @Autowired(required = false)
    private FileInfoService fileInfoService;

    @GetMapping("/fields")
    @ApiOperation("获取字段信息")
    public Result<FieldResponse> getFields() {
        try {
            FieldResponse response = templateDesignService.getFields();
            return Result.success(response);
        } catch (Exception e) {
            return Result.error("获取字段信息失败：" + e.getMessage());
        }
    }

    @PostMapping("/design/save")
    @ApiOperation("保存模板设计元素")
    public Result<TemplateDesignRecord> saveDesign(@RequestBody TemplateDesignRecord body) {
        if (designRecordService == null) {
            return Result.error("保存失败：设计记录服务不可用");
        }
        TemplateDesignRecord saved = designRecordService.save(body.getId(), body.getTemplateId(), body.getFileId(), body.getElementsJson());
        return Result.success(saved);
    }

    @GetMapping("/design/{id}")
    @ApiOperation("获取模板设计明细")
    public Result<TemplateDesignRecord> getDesignDetail(@PathVariable("id") String id) {
        if (designRecordService == null) {
            return Result.error("查询失败：设计记录服务不可用");
        }
        TemplateDesignRecord record = designRecordService.getById(id);
        if (record == null) {
            return Result.error("未找到设计记录");
        }
        return Result.success(record);
    }

    @GetMapping("/design/byTemplate/{templateId}")
    @ApiOperation("根据模板ID获取模板设计明细")
    public Result<TemplateDesignRecord> getDesignDetailByTemplateId(@PathVariable("templateId") String templateId) {
        if (designRecordService == null) {
            return Result.error("查询失败：设计记录服务不可用");
        }
        TemplateDesignRecord record = designRecordService.getByTemplateId(templateId);
        if (record == null) {
            return Result.error(404, "未找到模板设计记录");
        }
        return Result.success(record);
    }

    // 删除模板设计里的下载功能，统一走通用文件接口

    @PostMapping("/design/start")
    @ApiOperation("发起模板设计")
    public Result<TemplateDesignResponse> startTemplateDesign(
            @ApiParam(value = "模板设计请求", required = true) 
            @Valid @RequestBody TemplateDesignRequest request) {
        try {
            TemplateDesignResponse response = templateDesignService.startTemplateDesign(request);
            return Result.success(response);
        } catch (Exception e) {
            return Result.error("发起模板设计失败：" + e.getMessage());
        }
    }

    // ========== 新增：模板docx上传/列表/删除 ==========
    @PostMapping(value = "/design/upload", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiOperation("上传模板docx并保存记录（仅支持docx）")
    public Result<TemplateDesignRecord> uploadTemplate(
            @ApiParam("模板ID") @RequestParam("templateId") String templateId,
            @ApiParam("Word模板文件，仅支持docx") @RequestPart("file") org.springframework.web.multipart.MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return Result.error("请选择文件");
            }
            if (templateId == null || templateId.trim().isEmpty()) {
                return Result.error("模板ID不能为空");
            }
            String filename = file.getOriginalFilename();
            String ext = filename != null && filename.contains(".") ? filename.substring(filename.lastIndexOf('.') + 1).toLowerCase() : "";
            if (!"docx".equals(ext)) {
                return Result.error("仅支持docx文件，请将doc转换为docx再上传");
            }

            // 保存到uploads目录
            java.nio.file.Path root = java.nio.file.Paths.get(uploadRootPath).toAbsolutePath().normalize();
            java.nio.file.Files.createDirectories(root);
            String ts = String.valueOf(System.currentTimeMillis());
            java.io.File dest = root.resolve("templates/" + ts + ".docx").toFile();
            if (!dest.getParentFile().exists()) dest.getParentFile().mkdirs();
            file.transferTo(dest);

            // 注册文件
            if (fileInfoService == null) {
                return Result.error("文件服务不可用，无法注册上传文件");
            }
            com.zhaoxinms.contract.tools.common.entity.FileInfo info = fileInfoService.registerFile(filename, "docx", dest.getAbsolutePath(), dest.length());

            // 保存设计记录（elementsJson 初始为空结构）
            if (designRecordService == null) {
                return Result.error("保存失败：设计记录服务不可用");
            }
            String elementsJson = "{\"elements\":[]}";
            TemplateDesignRecord saved;
            try {
                saved = designRecordService.save(null, templateId.trim(), String.valueOf(info.getId()), elementsJson);
            } catch (IllegalArgumentException ex) {
                return Result.error(ex.getMessage());
            }
            return Result.success(saved);
        } catch (Exception e) {
            return Result.error("上传失败：" + e.getMessage());
        }
    }

    @GetMapping("/design/list")
    @ApiOperation("模板设计记录列表")
    public Result<java.util.List<TemplateDesignRecord>> listDesigns() {
        if (designRecordService == null) {
            return Result.error("查询失败：设计记录服务不可用");
        }
        return Result.success(designRecordService.listAll());
    }

    @DeleteMapping("/design/{id}")
    @ApiOperation("删除模板设计记录")
    public Result<Boolean> deleteDesign(@PathVariable("id") String id) {
        if (designRecordService == null) {
            return Result.error("删除失败：设计记录服务不可用");
        }
        boolean ok = designRecordService.deleteById(id);
        return ok ? Result.success(true) : Result.error("删除失败或记录不存在");
    }
    
    // removed unused helper
} 