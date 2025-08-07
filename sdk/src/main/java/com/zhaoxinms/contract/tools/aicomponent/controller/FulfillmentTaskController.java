package com.zhaoxinms.contract.tools.aicomponent.controller;

import com.zhaoxinms.contract.tools.aicomponent.model.FulfillmentExtractResult;
import com.zhaoxinms.contract.tools.aicomponent.service.FulfillmentAiService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 履约任务处理控制器
 * 提供文件上传和AI识别相关接口
 */
@RestController
@RequestMapping("/api/fulfillment")
@Api(tags = "履约任务处理")
@Slf4j
public class FulfillmentTaskController {

    @Autowired
    private FulfillmentAiService fulfillmentAiService;

    @PostMapping("/extract")
    @ApiOperation("从合同文件中提取履约任务")
    public ResponseEntity<?> extractFulfillmentTasks(
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "templateId", required = false) Long templateId
    ) {
        try {
            // 调用AI服务提取履约任务
            List<FulfillmentExtractResult> tasks = fulfillmentAiService.extractFulfillmentTasks(file, templateId);

            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("tasks", tasks);
            response.put("message", "识别成功");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("履约任务提取失败", e);

            // 构建错误响应
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "识别失败：" + e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/config")
    @ApiOperation("获取履约任务配置")
    public ResponseEntity<?> getFulfillmentConfig() {
        // 暂时返回空配置，后续可以扩展
        Map<String, Object> config = new HashMap<>();
        config.put("aiModel", "qwen-long");
        config.put("extractionEnabled", true);

        return ResponseEntity.ok(config);
    }

    @PostMapping("/config")
    @ApiOperation("保存履约任务配置")
    public ResponseEntity<?> saveFulfillmentConfig(
        @RequestBody Map<String, Object> configData
    ) {
        // 暂时不实现具体保存逻辑，后续可以扩展
        return ResponseEntity.ok(configData);
    }
}
