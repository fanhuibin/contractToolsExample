package com.zhaoxinms.contract.tools.aicomponent.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhaoxinms.contract.tools.aicomponent.model.FulfillmentTemplate;
import com.zhaoxinms.contract.tools.aicomponent.service.FulfillmentTemplateService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation; 
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 履约任务模板管理控制器
 * 提供模板的增删改查REST接口
 */
@RestController
@RequestMapping("/api/fulfillment/template")
@Api(tags = "履约任务模板管理")
public class FulfillmentTemplateController {

    @Autowired
    @Qualifier("fulfillmentTemplateServiceImpl")
    private FulfillmentTemplateService templateService;

    @GetMapping("/list")
    @ApiOperation("获取模板列表")
    public ResponseEntity<?> listTemplates(
        @ApiParam("合同类型") @RequestParam(required = false) String contractType,
        @ApiParam("用户ID") @RequestParam(required = false) String userId
    ) {
        List<FulfillmentTemplate> templates;
        if (StrUtil.isNotBlank(contractType)) {
            templates = templateService.listTemplatesByType(contractType, userId);
        } else {
            templates = templateService.list(
                Wrappers.<FulfillmentTemplate>lambdaQuery()
                    .eq(StrUtil.isNotBlank(userId), FulfillmentTemplate::getUserId, userId)
            );
        }
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/type/{contractType}")
    @ApiOperation("根据合同类型获取模板")
    public ResponseEntity<?> listTemplatesByType(
        @PathVariable String contractType,
        @ApiParam("用户ID") @RequestParam(required = false) String userId
    ) {
        List<FulfillmentTemplate> templates = templateService.listTemplatesByType(contractType, userId);
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/default/{contractType}")
    @ApiOperation("获取指定合同类型的默认模板")
    public ResponseEntity<?> getDefaultTemplate(
        @PathVariable String contractType,
        @ApiParam("用户ID") @RequestParam(required = false) String userId
    ) {
        FulfillmentTemplate template = templateService.getDefaultTemplate(contractType, userId);
        return ResponseEntity.ok(template);
    }

    @PostMapping("/create")
    @ApiOperation("创建新模板")
    public ResponseEntity<?> createTemplate(
        @RequestBody FulfillmentTemplate template
    ) {
        FulfillmentTemplate createdTemplate = templateService.createTemplate(template);
        return ResponseEntity.ok(createdTemplate);
    }

    @PutMapping("/{id}")
    @ApiOperation("更新模板")
    public ResponseEntity<?> updateTemplate(
        @PathVariable Long id,
        @RequestBody FulfillmentTemplate template
    ) {
        template.setId(id);
        FulfillmentTemplate updatedTemplate = templateService.updateTemplate(template);
        return ResponseEntity.ok(updatedTemplate);
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除模板")
    public ResponseEntity<?> deleteTemplate(
        @PathVariable Long id
    ) {
        boolean deleted = templateService.removeById(id);
        return ResponseEntity.ok(deleted);
    }

    @PostMapping("/{id}/copy")
    @ApiOperation("复制模板")
    public ResponseEntity<?> copyTemplate(
        @PathVariable Long id,
        @RequestParam String newName,
        @RequestParam String userId
    ) {
        FulfillmentTemplate copiedTemplate = templateService.copyTemplate(id, newName, userId);
        return ResponseEntity.ok(copiedTemplate);
    }

    @PostMapping("/{id}/set-default")
    @ApiOperation("设置默认模板")
    public ResponseEntity<?> setDefaultTemplate(
        @PathVariable Long id,
        @RequestParam String contractType
    ) {
        FulfillmentTemplate defaultTemplate = templateService.setDefaultTemplate(id, contractType);
        return ResponseEntity.ok(defaultTemplate);
    }

    @GetMapping("/contract-types")
    @ApiOperation("获取所有合同类型")
    public ResponseEntity<?> getContractTypes() {
        // 这里可以从数据库或配置文件读取合同类型
        // 暂时硬编码，后续可以改为从配置或数据库读取
        return ResponseEntity.ok(new String[]{
            "开票履约", "付款履约", "收款履约", 
            "到期提醒", "事件触发"
        });
    }
}
