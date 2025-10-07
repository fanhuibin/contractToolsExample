package com.zhaoxinms.contract.tools.test;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.Base64;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * 直接调用 vLLM Server 标准接口测试工具
 * vLLM 提供 OpenAI 兼容的 API 接口
 * 
 * @author zhaoxin
 * @date 2025-10-06
 */
public class VLLMDirectTest {
    
    // vLLM Server 地址（OpenAI兼容接口）
    private static final String VLLM_SERVER_URL = "http://192.168.0.100:30000";
    
    // 测试文件路径
    private static final String TEST_PDF_PATH = "I:\\测试\\16建筑合同\\007-建设工程施工合同（2017版） (1).pdf";
    
    // 使用的模型名称（根据vLLM服务配置调整）
    private static final String MODEL_NAME = "/root/.cache/modelscope/hub/models/OpenDataLab/MinerU2___5-2509-1___2B";
    
    public static void main(String[] args) {
        try {
            System.out.println("=== vLLM 直接调用测试开始 ===");
            System.out.println("vLLM Server: " + VLLM_SERVER_URL);
            System.out.println("模型: " + MODEL_NAME);
            System.out.println("测试文件: " + TEST_PDF_PATH);
            
            // 检查vLLM服务
            if (!checkVLLMService()) {
                System.err.println("\n错误: 无法连接到vLLM服务！");
                System.err.println("请检查vLLM Server是否已启动: " + VLLM_SERVER_URL);
                return;
            }
            
            // 读取PDF文件并转换为Base64
            File pdfFile = new File(TEST_PDF_PATH);
            if (!pdfFile.exists()) {
                System.err.println("错误: 文件不存在 - " + TEST_PDF_PATH);
                return;
            }
            
            System.out.println("文件大小: " + (pdfFile.length() / 1024) + " KB");
            
            // 记录开始时间
            long startTime = System.currentTimeMillis();
            
            // 调用vLLM API
            String result = callVLLMAPI(pdfFile);
            
            // 计算耗时
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            System.out.println("\n=== 处理耗时 ===");
            System.out.println("总耗时: " + (duration / 1000) + " 秒 (" + duration + " 毫秒)");
            
            // 输出结果
            System.out.println("\n=== vLLM 处理结果 ===");
            System.out.println(result);
            
            // 保存结果到文件
            File outputFile = new File("vllm_result.json");
            Files.write(outputFile.toPath(), result.getBytes("UTF-8"));
            System.out.println("\n结果已保存到: " + outputFile.getAbsolutePath());
            
            System.out.println("\n=== 测试完成 ===");
            
        } catch (Exception e) {
            System.err.println("错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 检查vLLM服务是否可用
     */
    private static boolean checkVLLMService() {
        try {
            System.out.println("\n正在检查vLLM服务连接...");
            URL url = new URL(VLLM_SERVER_URL + "/v1/models");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            
            int responseCode = conn.getResponseCode();
            
            if (responseCode == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String response = br.lines().reduce("", String::concat);
                br.close();
                System.out.println("✓ vLLM 服务连接成功");
                System.out.println("可用模型: " + response);
                return true;
            } else {
                System.err.println("✗ vLLM 返回状态码: " + responseCode);
                return false;
            }
        } catch (Exception e) {
            System.err.println("✗ vLLM 连接失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 调用 vLLM API（OpenAI 兼容接口）
     * 
     * @param pdfFile PDF文件
     * @return API响应结果
     */
    private static String callVLLMAPI(File pdfFile) throws Exception {
        System.out.println("\n正在调用 vLLM API...");
        
        // 读取PDF并转换为Base64
        byte[] pdfBytes = Files.readAllBytes(pdfFile.toPath());
        String base64Pdf = Base64.getEncoder().encodeToString(pdfBytes);
        
        // 构建请求JSON（OpenAI兼容格式）
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode requestJson = mapper.createObjectNode();
        requestJson.put("model", MODEL_NAME);
        
        // 构建messages数组
        ArrayNode messages = mapper.createArrayNode();
        ObjectNode message = mapper.createObjectNode();
        message.put("role", "user");
        
        // 构建content数组（包含文本和图像）
        ArrayNode content = mapper.createArrayNode();
        
        // 文本提示
        ObjectNode textContent = mapper.createObjectNode();
        textContent.put("type", "text");
        textContent.put("text", "请解析这个PDF文档，提取所有文本内容，并识别文档的结构，包括标题、段落、表格等。同时提供每个文本块的位置信息（bbox坐标）。");
        content.add(textContent);
        
        // PDF图像（使用Base64）
        ObjectNode imageContent = mapper.createObjectNode();
        imageContent.put("type", "image_url");
        ObjectNode imageUrl = mapper.createObjectNode();
        imageUrl.put("url", "data:application/pdf;base64," + base64Pdf);
        imageContent.set("image_url", imageUrl);
        content.add(imageContent);
        
        message.set("content", content);
        messages.add(message);
        requestJson.set("messages", messages);
        
        // 其他参数
        requestJson.put("temperature", 0.1);
        requestJson.put("max_tokens", 4096);
        
        String requestBody = mapper.writeValueAsString(requestJson);
        
        // 发送HTTP请求
        URL url = new URL(VLLM_SERVER_URL + "/v1/chat/completions");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(60000);  // 60秒连接超时
        conn.setReadTimeout(1800000);   // 30分钟读取超时
        
        // 写入请求体
        try (OutputStream os = conn.getOutputStream()) {
            os.write(requestBody.getBytes("UTF-8"));
        }
        
        System.out.println("请求已发送，等待vLLM处理...");
        
        // 读取响应
        int responseCode = conn.getResponseCode();
        System.out.println("响应状态码: " + responseCode);
        
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                    responseCode == 200 ? conn.getInputStream() : conn.getErrorStream(),
                    "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line).append("\n");
            }
        }
        
        conn.disconnect();
        
        if (responseCode != 200) {
            throw new IOException("API调用失败，状态码: " + responseCode + "\n" + response.toString());
        }
        
        return response.toString();
    }
}

