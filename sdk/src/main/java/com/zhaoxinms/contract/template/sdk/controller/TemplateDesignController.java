package com.zhaoxinms.contract.template.sdk.controller;

import com.zhaoxinms.contract.tools.api.dto.FieldResponse;
import com.zhaoxinms.contract.tools.api.dto.TemplateDesignRequest;
import com.zhaoxinms.contract.tools.api.dto.TemplateDesignResponse;
import com.zhaoxinms.contract.tools.common.Result;
import com.zhaoxinms.contract.tools.api.service.TemplateDesignService;
import com.zhaoxinms.contract.tools.common.service.FileInfoService;
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
    
    @Autowired
    private FileInfoService fileInfoService; // 预留给其他接口使用
    
    @Value("${zxcm.file.upload.root-path:./uploads}")
    private String uploadRootPath;

    @Autowired(required = false)
    private TemplateDesignRecordService designRecordService;

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
    
    // removed unused helper
} 