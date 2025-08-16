package com.zhaoxinms.contract.tools.ocrcompare.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.zhaoxinms.contract.tools.ocrcompare.client.OCRHttpClient;
import com.zhaoxinms.contract.tools.ocrcompare.facade.JavaOCR;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * OCR HTTP客户端测试类
 * 用于测试Python OCR服务的连接和功能
 */
@Component
public class OCRHttpClientTest {
    
    @Autowired
    private OCRHttpClient ocrHttpClient;
    
    @Autowired
    private JavaOCR javaOCR;
    
    /**
     * 测试OCR服务连接
     */
    public void testOCRServiceConnection() {
        System.out.println("=== 测试OCR服务连接 ===");
        
        try {
            boolean isHealthy = ocrHttpClient.healthCheck();
            if (isHealthy) {
                System.out.println("✅ OCR服务连接正常");
                System.out.println("服务地址: " + ocrHttpClient.getOcrServiceUrl());
                System.out.println("超时设置: " + ocrHttpClient.getTimeoutMs() + "ms");
            } else {
                System.out.println("❌ OCR服务连接失败");
                System.out.println("请确保Python OCR服务已启动在端口9898");
            }
        } catch (Exception e) {
            System.err.println("❌ 测试OCR服务连接异常: " + e.getMessage());
        }
    }
    
    /**
     * 测试OCR任务提交
     */
    public void testOCRTaskSubmission(String pdfPath) {
        System.out.println("\n=== 测试OCR任务提交 ===");
        
        try {
            // 构建OCR选项
            Map<String, Object> options = new HashMap<>();
            options.put("dpi", 150);
            options.put("min_score", 0.5);
            
            // 提交任务
            String taskId = ocrHttpClient.submitOCRTask(pdfPath, "pdf", options);
            System.out.println("✅ OCR任务提交成功");
            System.out.println("任务ID: " + taskId);
            
            // 查询任务状态
            testTaskStatusQuery(taskId);
            
        } catch (Exception e) {
            System.err.println("❌ 测试OCR任务提交失败: " + e.getMessage());
        }
    }
    
    /**
     * 测试任务状态查询
     */
    public void testTaskStatusQuery(String taskId) {
        System.out.println("\n=== 测试任务状态查询 ===");
        
        try {
            JsonNode taskStatus = ocrHttpClient.getTaskStatus(taskId);
            System.out.println("✅ 任务状态查询成功");
            System.out.println("任务ID: " + taskStatus.path("id").asText());
            System.out.println("状态: " + taskStatus.path("status").asText());
            System.out.println("进度: " + taskStatus.path("progress").asDouble() + "%");
            System.out.println("当前步骤: " + taskStatus.path("current_step").asText());
            
        } catch (Exception e) {
            System.err.println("❌ 测试任务状态查询失败: " + e.getMessage());
        }
    }
    
    /**
     * 测试等待任务完成
     */
    public void testWaitForTaskCompletion(String taskId) {
        System.out.println("\n=== 测试等待任务完成 ===");
        
        try {
            System.out.println("等待任务完成，最多等待5分钟...");
            JsonNode result = ocrHttpClient.waitForTaskCompletion(taskId, 5);
            System.out.println("✅ 任务完成");
            System.out.println("结果路径: " + result.path("result_path").asText());
            
            // 显示文本内容摘要
            String textContent = result.path("text_content").asText("");
            if (!textContent.isEmpty()) {
                System.out.println("识别文本长度: " + textContent.length() + " 字符");
                System.out.println("文本预览: " + textContent.substring(0, Math.min(200, textContent.length())) + "...");
            }
            
        } catch (Exception e) {
            System.err.println("❌ 测试等待任务完成失败: " + e.getMessage());
        }
    }
    
    /**
     * 测试历史任务查询
     */
    public void testHistoryQuery() {
        System.out.println("\n=== 测试历史任务查询 ===");
        
        try {
            JsonNode history = ocrHttpClient.getOCRHistory(1, 10, null);
            System.out.println("✅ 历史任务查询成功");
            
            JsonNode pagination = history.path("pagination");
            System.out.println("总任务数: " + pagination.path("total").asInt());
            System.out.println("当前页: " + pagination.path("page").asInt());
            System.out.println("每页大小: " + pagination.path("size").asInt());
            
            JsonNode tasks = history.path("tasks");
            System.out.println("当前页任务数: " + tasks.size());
            
        } catch (Exception e) {
            System.err.println("❌ 测试历史任务查询失败: " + e.getMessage());
        }
    }
    
    /**
     * 运行完整测试
     */
    public void runFullTest(String pdfPath) {
        System.out.println("🚀 开始OCR HTTP客户端完整测试");
        System.out.println("=".repeat(50));
        
        // 1. 测试服务连接
        testOCRServiceConnection();
        
        // 2. 测试任务提交
        testOCRTaskSubmission(pdfPath);
        
        // 3. 测试历史查询
        testHistoryQuery();
        
        System.out.println("\n" + "=".repeat(50));
        System.out.println("🎉 OCR HTTP客户端测试完成");
    }
    
    /**
     * 运行快速连接测试
     */
    public void runQuickConnectionTest() {
        System.out.println("🔍 快速连接测试");
        testOCRServiceConnection();
    }
}
