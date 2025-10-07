package com.zhaoxinms.contract.tools.test;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * MinerU API 测试工具类
 * 用于测试调用 MinerU 服务处理 PDF 文档
 * 
 * @author zhaoxin
 * @date 2025-10-06
 */
public class MinerUTest {
    
    // MinerU 服务地址（Web API）
    private static final String MINERU_URL = "http://192.168.0.100:8000";
    
    
    // 测试文件路径
    private static final String TEST_PDF_PATH = "I:\\测试\\1.大连二手房\\大连二手房空白.pdf";
    
    // vLLM Server 地址
    private static final String VLLM_SERVER_URL = "http://192.168.0.100:30000";
    
    // 后端类型配置
    // 使用 vlm-http-client 连接已启动的 vLLM Server
    private static final String BACKEND = "vlm-http-client";
    
    public static void main(String[] args) {
        try {
            System.out.println("=== MinerU API 测试开始 ===");
            System.out.println("服务地址: " + MINERU_URL);
            System.out.println("后端模式: " + BACKEND);
            System.out.println("vLLM Server: " + VLLM_SERVER_URL);
            System.out.println("测试文件: " + TEST_PDF_PATH);
            
            // 读取PDF文件
            File pdfFile = new File(TEST_PDF_PATH);
            if (!pdfFile.exists()) {
                System.err.println("错误: 文件不存在 - " + TEST_PDF_PATH);
                return;
            }
            
            System.out.println("文件大小: " + (pdfFile.length() / 1024) + " KB");
            
            // 记录开始时间
            long startTime = System.currentTimeMillis();
            
            // 调用MinerU API
            String result = callMinerUAPI(pdfFile);
            
            // 计算耗时
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            System.out.println("\n=== 处理耗时 ===");
            System.out.println("总耗时: " + (duration / 1000) + " 秒 (" + duration + " 毫秒)");
            
            // 输出结果摘要
            System.out.println("\n=== MinerU 处理结果 ===");
            System.out.println("结果长度: " + result.length() + " 字符");
            
            // 保存完整结果到文件
            File outputFile = new File("mineru_result.json");
            Files.write(outputFile.toPath(), result.getBytes("UTF-8"));
            System.out.println("完整结果已保存到: " + outputFile.getAbsolutePath());
            
            // 解析并提取middle_json和content_list数据
            extractMiddleJson(result);
            extractContentList(result);
            
            System.out.println("\n=== 测试完成 ===");
            
        } catch (Exception e) {
            System.err.println("错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 提取并保存middle_json数据（包含bbox信息）
     * 
     * @param jsonResult API返回的JSON结果
     */
    private static void extractMiddleJson(String jsonResult) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonResult);
            
            // 先尝试从results中查找
            JsonNode resultsNode = root.get("results");
            if (resultsNode != null && resultsNode.isObject()) {
                // 获取第一个结果（通常是文件名作为key）
                JsonNode firstResult = resultsNode.elements().next();
                if (firstResult != null) {
                    JsonNode middleJsonNode = firstResult.get("middle_json");
                    
                    if (middleJsonNode != null) {
                        String middleJsonContent;
                        
                        // 判断middle_json是字符串还是对象
                        if (middleJsonNode.isTextual()) {
                            // 如果是字符串，先解析再格式化
                            String jsonString = middleJsonNode.asText();
                            JsonNode parsedJson = mapper.readTree(jsonString);
                            middleJsonContent = mapper.writerWithDefaultPrettyPrinter()
                                .writeValueAsString(parsedJson);
                        } else {
                            // 如果是对象，直接格式化
                            middleJsonContent = mapper.writerWithDefaultPrettyPrinter()
                                .writeValueAsString(middleJsonNode);
                        }
                        
                        // 保存到文件
                        File middleJsonFile = new File("mineru_middle_json.json");
                        Files.write(middleJsonFile.toPath(), middleJsonContent.getBytes("UTF-8"));
                        
                        System.out.println("Middle JSON已保存到: " + middleJsonFile.getAbsolutePath());
                        System.out.println("Middle JSON大小: " + (middleJsonContent.length() / 1024) + " KB");
                        return;
                    }
                }
            }
            
            // 如果在results中没找到，直接在根节点查找
            JsonNode middleJsonNode = root.get("middle_json");
            if (middleJsonNode != null) {
                String middleJsonContent;
                
                if (middleJsonNode.isTextual()) {
                    // 如果是字符串，先解析再格式化
                    String jsonString = middleJsonNode.asText();
                    JsonNode parsedJson = mapper.readTree(jsonString);
                    middleJsonContent = mapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(parsedJson);
                } else {
                    // 如果是对象，直接格式化
                    middleJsonContent = mapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(middleJsonNode);
                }
                
                File middleJsonFile = new File("mineru_middle_json.json");
                Files.write(middleJsonFile.toPath(), middleJsonContent.getBytes("UTF-8"));
                
                System.out.println("Middle JSON已保存到: " + middleJsonFile.getAbsolutePath());
                System.out.println("Middle JSON大小: " + (middleJsonContent.length() / 1024) + " KB");
            } else {
                System.out.println("警告: 结果中未找到middle_json字段");
            }
            
        } catch (Exception e) {
            System.err.println("提取middle_json失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 提取并保存content_list数据
     * 
     * @param jsonResult API返回的JSON结果
     */
    private static void extractContentList(String jsonResult) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonResult);
            
            // 先尝试从results中查找
            JsonNode resultsNode = root.get("results");
            if (resultsNode != null && resultsNode.isObject()) {
                // 获取第一个结果（通常是文件名作为key）
                JsonNode firstResult = resultsNode.elements().next();
                if (firstResult != null) {
                    JsonNode contentListNode = firstResult.get("content_list");
                    
                    if (contentListNode != null) {
                        String contentListContent;
                        
                        // 判断content_list是字符串还是对象
                        if (contentListNode.isTextual()) {
                            // 如果是字符串，先解析再格式化
                            String jsonString = contentListNode.asText();
                            JsonNode parsedJson = mapper.readTree(jsonString);
                            contentListContent = mapper.writerWithDefaultPrettyPrinter()
                                .writeValueAsString(parsedJson);
                        } else {
                            // 如果是对象，直接格式化
                            contentListContent = mapper.writerWithDefaultPrettyPrinter()
                                .writeValueAsString(contentListNode);
                        }
                        
                        // 保存到文件
                        File contentListFile = new File("mineru_content_list.json");
                        Files.write(contentListFile.toPath(), contentListContent.getBytes("UTF-8"));
                        
                        System.out.println("Content List已保存到: " + contentListFile.getAbsolutePath());
                        System.out.println("Content List大小: " + (contentListContent.length() / 1024) + " KB");
                        return;
                    }
                }
            }
            
            // 如果在results中没找到，直接在根节点查找
            JsonNode contentListNode = root.get("content_list");
            if (contentListNode != null) {
                String contentListContent;
                
                if (contentListNode.isTextual()) {
                    // 如果是字符串，先解析再格式化
                    String jsonString = contentListNode.asText();
                    JsonNode parsedJson = mapper.readTree(jsonString);
                    contentListContent = mapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(parsedJson);
                } else {
                    // 如果是对象，直接格式化
                    contentListContent = mapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(contentListNode);
                }
                
                File contentListFile = new File("mineru_content_list.json");
                Files.write(contentListFile.toPath(), contentListContent.getBytes("UTF-8"));
                
                System.out.println("Content List已保存到: " + contentListFile.getAbsolutePath());
                System.out.println("Content List大小: " + (contentListContent.length() / 1024) + " KB");
            } else {
                System.out.println("提示: 结果中未找到content_list字段");
            }
            
        } catch (Exception e) {
            System.err.println("提取content_list失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 调用 MinerU API 处理 PDF 文件
     * 
     * @param pdfFile PDF文件
     * @return API响应结果
     * @throws IOException IO异常
     */
    private static String callMinerUAPI(File pdfFile) throws IOException {
        String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
        
        // 创建HTTP连接 - 修正接口路径为 /file_parse
        URL url = new URL(MINERU_URL + "/file_parse");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        conn.setConnectTimeout(60000);  // 60秒连接超时
        conn.setReadTimeout(1800000);   // 30分钟读取超时（vlm-vllm-async-engine处理较慢）
        
        System.out.println("\n正在上传文件到 MinerU...");
        
        // 构建multipart/form-data请求体
        try (OutputStream os = conn.getOutputStream();
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, "UTF-8"), true)) {
            
            // 添加文件字段 - 字段名改为 files
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"files\"; filename=\"")
                  .append(pdfFile.getName()).append("\"\r\n");
            writer.append("Content-Type: application/pdf\r\n\r\n");
            writer.flush();
            
            // 写入文件内容
            try (FileInputStream fis = new FileInputStream(pdfFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }
            os.flush();
            
            writer.append("\r\n");
            
            // 设置backend为vlm-http-client
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"backend\"\r\n\r\n");
            writer.append(BACKEND).append("\r\n");
            
            // 设置vLLM Server地址
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"server_url\"\r\n\r\n");
            writer.append(VLLM_SERVER_URL).append("\r\n");
            
            // 添加可选参数 - 返回Markdown
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"return_md\"\r\n\r\n");
            writer.append("true\r\n");
            
            // 返回中间JSON（可能包含bbox）
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"return_middle_json\"\r\n\r\n");
            writer.append("true\r\n");
            
            // 返回模型输出（可能包含更详细的位置信息）
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"return_model_output\"\r\n\r\n");
            writer.append("true\r\n");
            
            // 返回内容列表
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"return_content_list\"\r\n\r\n");
            writer.append("true\r\n");
            
            // 结束边界
            writer.append("--").append(boundary).append("--\r\n");
            writer.flush();
        }
        
        System.out.println("上传完成，等待处理结果...");
        
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

