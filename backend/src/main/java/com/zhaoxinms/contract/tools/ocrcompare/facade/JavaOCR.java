package com.zhaoxinms.contract.tools.ocrcompare.facade;

import com.fasterxml.jackson.databind.JsonNode;
import com.zhaoxinms.contract.tools.ocrcompare.model.OCRTask;
import com.zhaoxinms.contract.tools.ocrcompare.client.OCRHttpClient;
import com.zhaoxinms.contract.tools.ocrcompare.service.OCRTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JavaOCR {

    @Autowired
    private OCRTaskService ocrTaskService;

    @Autowired
    private OCRHttpClient ocrHttpClient;

    public String submitOCRTask(String pdfPath) { return ocrTaskService.submitOCRTask(pdfPath); }

    public OCRTask getTaskStatus(String taskId) { return ocrTaskService.getTaskStatus(taskId); }

    public OCRTask waitForTaskCompletion(String taskId, int timeoutMinutes) {
        try {
            ocrHttpClient.waitForTaskCompletion(taskId, timeoutMinutes);
            return ocrTaskService.getTaskStatus(taskId);
        } catch (Exception e) { throw new RuntimeException("等待任务完成失败: " + e.getMessage(), e); }
    }

    public JsonNode readOCRResult(String taskId) {
        try { return ocrHttpClient.getOCRResult(taskId); }
        catch (Exception e) { throw new RuntimeException("读取OCR结果失败: " + e.getMessage(), e); }
    }

    public void displayTaskResult(String taskId, boolean showDetails) {
        JsonNode result = readOCRResult(taskId);
        if (result == null) { System.out.println("无法读取任务结果: " + taskId); return; }
        try {
            JsonNode jsonData = result.path("json_data");
            if (!jsonData.isMissingNode() && jsonData.path("pages").isArray()) {
                System.out.println("\n===== OCR识别结果 =====");
                System.out.println("任务ID: " + taskId);
                System.out.println("PDF文件: " + jsonData.path("pdf").asText());
                System.out.println("总页数: " + jsonData.path("pages").size());
                if (showDetails) {
                    for (JsonNode page : jsonData.path("pages")) {
                        int pageIndex = page.path("page_index").asInt();
                        System.out.println("\n----- 第 " + pageIndex + " 页 -----");
                        JsonNode items = page.path("items");
                        if (items.isArray()) {
                            System.out.println("识别项目数: " + items.size());
                            for (JsonNode item : items) {
                                String text = item.path("text").asText("");
                                double score = item.path("score").asDouble(0.0);
                                System.out.println("  文字: '" + text + "' (置信度: " + String.format("%.3f", score) + ")");
                            }
                        }
                    }
                }
            }
            String textContent = result.path("text_content").asText("");
            if (!textContent.isEmpty()) {
                System.out.println("\n===== 识别文本内容 =====");
                System.out.println("文本长度: " + textContent.length() + " 字符");
                if (showDetails) { System.out.println(textContent); }
            }
        } catch (Exception e) { System.err.println("显示OCR结果失败: " + e.getMessage()); }
    }

    public boolean getOCRServiceStatus() { return ocrHttpClient.healthCheck(); }
}


