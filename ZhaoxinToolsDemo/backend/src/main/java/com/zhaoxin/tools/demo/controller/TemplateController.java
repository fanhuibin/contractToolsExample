package com.zhaoxin.tools.demo.controller;

import com.zhaoxin.tools.demo.model.response.ApiResponse;
import com.zhaoxin.tools.demo.service.ZhaoxinApiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 模板管理控制器
 * 
 * 功能：
 * 1. 获取模板详情
 */
@Slf4j
@RestController
@RequestMapping("/api/template")
public class TemplateController {
    
    @Autowired
    private ZhaoxinApiClient apiClient;
    
    /**
     * 获取模板设计详情
     * 
     * @param templateId 模板ID
     * @return 模板详情（包含 elementsJson）
     */
    @GetMapping("/design/detail/{templateId}")
    public ApiResponse<Map<String, Object>> getTemplateDetail(
            @PathVariable String templateId) {
        try {
            log.info("获取模板详情: templateId={}", templateId);
            Map<String, Object> result = apiClient.getTemplateDetail(templateId);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("获取模板详情失败: templateId={}", templateId, e);
            return ApiResponse.error("获取模板详情失败: " + e.getMessage());
        }
    }
}

