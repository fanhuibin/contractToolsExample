package com.zhaoxinms.contract.tools.comparePRO.service;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhaoxinms.contract.tools.comparePRO.config.ZxOcrConfig;
import com.zhaoxinms.contract.tools.comparePRO.model.CompareOptions;
import com.zhaoxinms.contract.tools.comparePRO.model.CrossPageTableManager;
import com.zhaoxinms.contract.tools.comparePRO.model.MinerURecognitionResult;
import com.zhaoxinms.contract.tools.comparePRO.util.MinerUCoordinateConverter;
import com.zhaoxinms.contract.tools.comparePRO.util.TextExtractionUtil;

import lombok.extern.slf4j.Slf4j;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 基于MinerU的OCR识别服务
 * 用于合同比对功能
 * 
 * @author zhaoxin
 * @date 2025-10-07
 */
@Slf4j
@Service
public class MinerUOCRService {
    
    @Autowired
    private ZxOcrConfig zxOcrConfig;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 识别PDF并返回dots.ocr兼容的格式（包含跨页表格管理器）
     * 
     * @param pdfFile PDF文件
     * @param taskId 任务ID
     * @param outputDir 输出目录
     * @param docMode 文档模式（old/new）
     * @param options 比对选项（包含页眉页脚设置）
     * @return MinerU识别结果（包含PageLayout数组和跨页表格管理器）
     */
    public MinerURecognitionResult recognizePdf(
            File pdfFile, 
            String taskId, 
            File outputDir,
            String docMode,
            CompareOptions options) throws Exception {
        
        log.info("使用MinerU识别PDF: {}, 任务ID: {}, 模式: {}", pdfFile.getName(), taskId, docMode);
        
        long startTime = System.currentTimeMillis();
        
        // 并行处理：1. 提交PDF到MinerU识别  2. 拆分PDF为图片
        CompletableFuture<String> recognitionFuture = CompletableFuture.supplyAsync(() -> {
            try {
                log.info("并行处理：提交PDF识别和生成图片");
                return callMinerUAPI(pdfFile);
            } catch (Exception e) {
                throw new RuntimeException("MinerU识别失败", e);
            }
        });
        
        CompletableFuture<List<Map<String, Object>>> imagesFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return generatePageImages(pdfFile, outputDir, taskId, docMode);
            } catch (Exception e) {
                throw new RuntimeException("生成页面图片失败", e);
            }
        });
        
        // 等待两个任务完成
        String apiResult = recognitionFuture.get();
        List<Map<String, Object>> pageImages = imagesFuture.get();
        
        log.info("MinerU识别完成，解析结果...");
        
        // 保存MinerU原始响应JSON
        saveRawResponse(apiResult, outputDir, taskId, docMode);
        
        // 保存格式化的 content_list（方便调试 bbox）
        saveFormattedContentList(apiResult, outputDir, taskId, docMode);
        
        // 创建跨页表格管理器
        CrossPageTableManager tableManager = new CrossPageTableManager();
        
        // 转换为dots.ocr兼容的PageLayout格式（同时识别跨页表格）
        TextExtractionUtil.PageLayout[] layouts = convertToPageLayouts(apiResult, pageImages, pdfFile, options, tableManager, docMode);
        
        long endTime = System.currentTimeMillis();
        log.info("MinerU OCR识别完成，共{}页，耗时{}ms", layouts.length, endTime - startTime);
        
        // 输出跨页表格统计信息
        if (tableManager.getTableGroupCount() > 0) {
            log.info("📊 跨页表格识别统计: {}", tableManager.getStatistics());
        }
        
        return new MinerURecognitionResult(layouts, tableManager);
    }
    
    /**
     * 调用MinerU API进行识别
     */
    private String callMinerUAPI(File pdfFile) throws Exception {
        String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
        
        URL url = new URL(zxOcrConfig.getApiUrl() + "/file_parse");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        conn.setConnectTimeout(60000);
        conn.setReadTimeout(1800000);
        
        // 构建请求体
        try (OutputStream os = conn.getOutputStream();
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, "UTF-8"), true)) {
            
            // 添加文件
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"files\"; filename=\"")
                  .append(pdfFile.getName()).append("\"\r\n");
            writer.append("Content-Type: application/pdf\r\n\r\n");
            writer.flush();
            
            try (FileInputStream fis = new FileInputStream(pdfFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }
            os.flush();
            writer.append("\r\n");
            
            // 设置backend
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"backend\"\r\n\r\n");
            writer.append(zxOcrConfig.getBackend()).append("\r\n");
            
            // 如果使用vlm-http-client，添加server_url
            if ("vlm-http-client".equals(zxOcrConfig.getBackend())) {
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"server_url\"\r\n\r\n");
                writer.append(zxOcrConfig.getVllmServerUrl()).append("\r\n");
            }
            
            // 返回content_list（最终的结构化列表）
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"return_content_list\"\r\n\r\n");
            writer.append("true\r\n");
            
            // 返回middle_json（MinerU 原始中间 JSON，包含布局分析等原始数据）
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"return_middle_json\"\r\n\r\n");
            writer.append("true\r\n");
            
            writer.append("--").append(boundary).append("--\r\n");
            writer.flush();
        }
        
        // 读取响应
        int responseCode = conn.getResponseCode();
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
            throw new IOException("MinerU API调用失败，状态码: " + responseCode + "\n" + response.toString());
        }
        
        return response.toString();
    }
    
    /**
     * 生成PDF页面图片（缓存优化 + 串行渲染）
     * 
     * 注意：PDFRenderer 不是线程安全的，必须使用串行渲染
     * 
     * @param pdfFile PDF文件
     * @param outputDir 输出目录（任务目录）
     * @param taskId 任务ID
     * @param docMode 文档模式（old/new）
     */
    private List<Map<String, Object>> generatePageImages(File pdfFile, File outputDir, String taskId, String docMode) throws IOException {
        List<Map<String, Object>> pageImages = new ArrayList<>();
        
        // 图片保存到 images/old 或 images/new 目录
        File imagesDir = new File(outputDir, "images/" + docMode);
        if (!imagesDir.exists()) {
            imagesDir.mkdirs();
        }
        
        int renderDpi = zxOcrConfig.getRenderDpi();
        long startTime = System.currentTimeMillis();
        
        // 图片尺寸限制配置
        final int MAX_IMAGE_WIDTH = 2000;   // 最大宽度2000像素
        final int MAX_IMAGE_HEIGHT = 3000;  // 最大高度3000像素
        
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFRenderer renderer = new PDFRenderer(document);
            int pageCount = document.getNumberOfPages();
            
            log.info("开始生成{}个页面图片，DPI: {}, 最大尺寸限制: {}x{}", 
                pageCount, renderDpi, MAX_IMAGE_WIDTH, MAX_IMAGE_HEIGHT);
            
            int cachedCount = 0;
            int renderedCount = 0;
            
            // 获取图片格式配置（PNG 无损格式）
            String imageFormat = zxOcrConfig.getImageFormat() != null ? 
                zxOcrConfig.getImageFormat().toUpperCase() : "PNG";
            String imageExt = ".png";
            
            log.info("图片格式: {}", imageFormat);
            
            // PDFRenderer 不是线程安全的，必须串行处理
            // 【内存优化】逐页处理并立即释放内存
            for (int i = 0; i < pageCount; i++) {
                File imageFile = new File(imagesDir, "page-" + (i + 1) + imageExt);
                BufferedImage image = null;
                int imageWidth = 0;
                int imageHeight = 0;
                
                // 获取当前页面尺寸信息
                PDPage page = document.getPage(i);
                PDRectangle mediaBox = page.getMediaBox();
                float pageWidthPt = mediaBox.getWidth();   // PDF点数单位
                float pageHeightPt = mediaBox.getHeight(); // PDF点数单位
                
                // 转换为英寸和毫米
                float pageWidthInch = pageWidthPt / 72f;
                float pageHeightInch = pageHeightPt / 72f;
                float pageWidthMm = pageWidthInch * 25.4f;
                float pageHeightMm = pageHeightInch * 25.4f;
                
                // 计算使用默认DPI时的预期图片尺寸
                int expectedWidth = (int)(pageWidthInch * renderDpi);
                int expectedHeight = (int)(pageHeightInch * renderDpi);
                
                // 计算实际使用的DPI（考虑最大尺寸限制）
                int actualDpi = renderDpi;
                if (expectedWidth > MAX_IMAGE_WIDTH || expectedHeight > MAX_IMAGE_HEIGHT) {
                    // 计算缩放比例（取宽度和高度缩放比例中的较小值，保持原始比例）
                    float scaleWidth = (float) MAX_IMAGE_WIDTH / expectedWidth;
                    float scaleHeight = (float) MAX_IMAGE_HEIGHT / expectedHeight;
                    float scale = Math.min(scaleWidth, scaleHeight);
                    
                    // 调整DPI
                    actualDpi = (int)(renderDpi * scale);
                    
                    // 重新计算调整后的图片尺寸
                    expectedWidth = (int)(pageWidthInch * actualDpi);
                    expectedHeight = (int)(pageHeightInch * actualDpi);
                    
                    log.warn("⚠️ 页面{} 尺寸过大，自动调整DPI以保持原始比例:", i + 1);
                    log.warn("  - PDF页面尺寸: {:.1f} × {:.1f} 点 ({:.0f} × {:.0f} mm)", 
                        pageWidthPt, pageHeightPt, pageWidthMm, pageHeightMm);
                    log.warn("  - 原DPI {} 预期尺寸: {} × {} 像素 (超出限制)", 
                        renderDpi, (int)(pageWidthInch * renderDpi), (int)(pageHeightInch * renderDpi));
                    log.warn("  - 调整后DPI: {} → {}", renderDpi, actualDpi);
                    log.warn("  - 实际生成尺寸: {} × {} 像素", expectedWidth, expectedHeight);
                } else {
                    // 尺寸正常，输出信息日志
                    log.info("📄 页面{} 尺寸信息:", i + 1);
                    log.info("  - PDF页面尺寸: {:.1f} × {:.1f} 点 ({:.0f} × {:.0f} mm)", 
                        pageWidthPt, pageHeightPt, pageWidthMm, pageHeightMm);
                    log.info("  - 使用DPI: {}", actualDpi);
                    log.info("  - 预期生成尺寸: {} × {} 像素", expectedWidth, expectedHeight);
                }
                
                try {
                    // 缓存检查：如果图片已存在且可读取，直接复用
                    if (imageFile.exists()) {
                        try {
                            image = ImageIO.read(imageFile);
                            if (image != null) {
                                imageWidth = image.getWidth();
                                imageHeight = image.getHeight();
                                log.debug("复用已有图片: {}, 尺寸: {}x{}, 大小: {}KB", 
                                    imageFile.getName(), imageWidth, imageHeight,
                                    imageFile.length() / 1024);
                                cachedCount++;
                            } else {
                                // 文件损坏，重新生成
                                log.warn("图片文件损坏，重新生成: {}", imageFile.getName());
                                image = renderer.renderImageWithDPI(i, actualDpi, ImageType.RGB);
                                imageWidth = image.getWidth();
                                imageHeight = image.getHeight();
                                saveImage(image, imageFile);
                                log.info("✅ 重新生成页面图片: {}, 实际尺寸: {}x{}, 大小: {}KB", 
                                    imageFile.getName(), imageWidth, imageHeight,
                                    imageFile.length() / 1024);
                                renderedCount++;
                            }
                        } catch (IOException e) {
                            // 读取失败，重新生成
                            log.warn("读取已有图片失败，重新生成: {}, 原因: {}", 
                                imageFile.getName(), e.getMessage());
                            image = renderer.renderImageWithDPI(i, actualDpi, ImageType.RGB);
                            imageWidth = image.getWidth();
                            imageHeight = image.getHeight();
                            saveImage(image, imageFile);
                            log.info("✅ 重新生成页面图片: {}, 实际尺寸: {}x{}, 大小: {}KB", 
                                imageFile.getName(), imageWidth, imageHeight,
                                imageFile.length() / 1024);
                            renderedCount++;
                        }
                    } else {
                        // 生成新图片
                        image = renderer.renderImageWithDPI(i, actualDpi, ImageType.RGB);
                        imageWidth = image.getWidth();
                        imageHeight = image.getHeight();
                        saveImage(image, imageFile);
                        log.info("✅ 生成页面图片: {}, 实际尺寸: {}x{}, 大小: {}KB", 
                            imageFile.getName(), imageWidth, imageHeight,
                            imageFile.length() / 1024);
                        renderedCount++;
                    }
                    
                    // 构建页面信息（只保存元数据，不保存图片对象）
                    Map<String, Object> pageInfo = new HashMap<>();
                    pageInfo.put("pageIndex", i);
                    pageInfo.put("imagePath", imageFile.getAbsolutePath());
                    pageInfo.put("imageWidth", imageWidth);
                    pageInfo.put("imageHeight", imageHeight);
                    pageImages.add(pageInfo);
                    
                } finally {
                    // 【关键】立即释放BufferedImage内存
                    if (image != null) {
                        image.flush();
                        image = null;
                    }
                    
                    // 【关键】每处理3页建议进行一次垃圾回收
                    // 这样可以及时释放内存，避免峰值过高
                    if ((i + 1) % 3 == 0) {
                        System.gc();
                        // 给GC一点时间
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            }
            
            long endTime = System.currentTimeMillis();
            log.info("页面图片生成完成，共{}页（缓存{}页，渲染{}页），耗时{}ms（平均每页{}ms）", 
                pageCount, cachedCount, renderedCount, endTime - startTime, (endTime - startTime) / pageCount);
        }
        
        return pageImages;
    }
    
    /**
     * 提取content_list字段
     */
    private JsonNode extractContentList(JsonNode root) throws Exception {
        // 先从results中查找
        JsonNode resultsNode = root.get("results");
        if (resultsNode != null && resultsNode.isObject()) {
            JsonNode firstResult = resultsNode.elements().next();
            if (firstResult != null) {
                JsonNode contentListNode = firstResult.get("content_list");
                if (contentListNode != null) {
                    if (contentListNode.isTextual()) {
                        return objectMapper.readTree(contentListNode.asText());
                    }
                    return contentListNode;
                }
            }
        }
        
        // 直接从根节点查找
        JsonNode contentListNode = root.get("content_list");
        if (contentListNode != null) {
            if (contentListNode.isTextual()) {
                return objectMapper.readTree(contentListNode.asText());
            }
            return contentListNode;
        }
        
        return null;
    }
    
    /**
     * 转换MinerU结果为dots.ocr兼容的PageLayout格式
     * 
     * 【重要】返回的格式与dots.ocr完全一致，可以复用所有后续处理逻辑
     * 
     * @param apiResult MinerU API 返回结果
     * @param pageImages 页面图片信息
     * @param pdfFile PDF 文件
     * @param options 比对选项
     * @param tableManager 跨页表格管理器（用于识别和管理跨页表格）
     * @param docMode 文档模式（old/new/extract），用于决定是否保留表格HTML
     * @return PageLayout 数组
     */
    private TextExtractionUtil.PageLayout[] convertToPageLayouts(
            String apiResult,
            List<Map<String, Object>> pageImages,
            File pdfFile,
            CompareOptions options,
            CrossPageTableManager tableManager,
            String docMode) throws Exception {
        
        JsonNode root = objectMapper.readTree(apiResult);
        JsonNode contentListNode = extractContentList(root);
        if (contentListNode == null || !contentListNode.isArray()) {
            throw new Exception("未找到有效的content_list数据");
        }
        
        // 解析 middle_json 以获取表格的精确 bbox
        JsonNode middleJsonNode = extractMiddleJson(root);
        log.info("📊 [convertToPageLayouts] extractMiddleJson 返回: {}", 
            middleJsonNode != null ? (middleJsonNode.isArray() ? "数组[" + middleJsonNode.size() + "页]" : "非数组对象") : "null");
        
        // 获取PDF尺寸信息
        int totalPages = pageImages.size();
        Map<Integer, double[]> pdfPageSizes = new HashMap<>();
        Map<Integer, Map<String, Object>> pageImageMap = new HashMap<>();
        
        for (Map<String, Object> pageImage : pageImages) {
            int pageIdx = (Integer) pageImage.get("pageIndex");
            double[] pdfSize = MinerUCoordinateConverter.getPdfPageSize(pdfFile, pageIdx);
            pdfPageSizes.put(pageIdx, pdfSize);
            pageImageMap.put(pageIdx, pageImage);
        }
        
        // 按页面组织LayoutItem
        Map<Integer, List<TextExtractionUtil.LayoutItem>> pageLayoutItems = new HashMap<>();
        
        // 跟踪已经匹配过的middle_json表格，防止多个content_list表格匹配到同一个middle_json表格
        // key: "pageIdx-tableIndex", value: true
        Set<String> matchedMiddleJsonTables = new HashSet<>();
        
        int contentListIndex = 0;
        for (JsonNode item : contentListNode) {
            int pageIdx = item.has("page_idx") ? item.get("page_idx").asInt() : 0;
            
            // 过滤页眉页脚
            if (options.isIgnoreHeaderFooter() && isHeaderFooterOrPageNumber(item)) {
                String itemType = item.has("type") ? item.get("type").asText() : "unknown";
                log.debug("🚫 过滤 MinerU 识别的页眉页脚 - 第{}页, 类型:{}", pageIdx + 1, itemType);
                contentListIndex++;
                continue;
            }
            
            // 识别跨页表格并建立关联
            if (tableManager != null && "table".equals(item.has("type") ? item.get("type").asText() : "")) {
                identifyCrossPageTable(item, contentListIndex, pageIdx, 
                    pageImageMap.get(pageIdx), pdfPageSizes.get(pageIdx), tableManager);
            }
            
            // 转换为LayoutItem
            List<TextExtractionUtil.LayoutItem> items = convertToLayoutItems(
                item,
                pageImageMap.get(pageIdx),
                pdfPageSizes.get(pageIdx),
                middleJsonNode,
                pageIdx,
                docMode,
                matchedMiddleJsonTables
            );
            
            if (!pageLayoutItems.containsKey(pageIdx)) {
                pageLayoutItems.put(pageIdx, new ArrayList<>());
            }
            pageLayoutItems.get(pageIdx).addAll(items);
            
            contentListIndex++;
        }
        
        // 构建PageLayout数组
        TextExtractionUtil.PageLayout[] layouts = new TextExtractionUtil.PageLayout[totalPages];
        for (int i = 0; i < totalPages; i++) {
            List<TextExtractionUtil.LayoutItem> items = pageLayoutItems.getOrDefault(i, new ArrayList<>());
            Map<String, Object> pageImage = pageImageMap.get(i);
            int imgW = (Integer) pageImage.get("imageWidth");
            int imgH = (Integer) pageImage.get("imageHeight");
            
            // 注意：MinerU 的 page_idx 是 0-based，但 PageLayout.page 应该是 1-based（与 dots.ocr 一致）
            layouts[i] = new TextExtractionUtil.PageLayout(i + 1, items, imgW, imgH);
        }
        
        return layouts;
    }
    
    /**
     * 识别跨页表格并建立关联
     * 
     * 识别规则：
     * - 如果 table_caption 为空或不存在
     * - 且 table_footnote 为空或不存在
     * - 且 table_body 为空或不存在
     * - 则认为是上一个表格的跨页延续部分
     * 
     * @param item content_list 中的表格项
     * @param contentListIndex 在 content_list 中的索引
     * @param pageIdx 页码（0-based）
     * @param pageImage 页面图片信息
     * @param pdfPageSize PDF 页面尺寸
     * @param tableManager 跨页表格管理器
     */
    private void identifyCrossPageTable(JsonNode item, int contentListIndex, int pageIdx,
                                        Map<String, Object> pageImage, double[] pdfPageSize,
                                        CrossPageTableManager tableManager) {
        if (item == null || tableManager == null) {
            return;
        }
        
        // 检查是否有 table_caption
        boolean hasCaption = false;
        if (item.has("table_caption")) {
            JsonNode captionNode = item.get("table_caption");
            hasCaption = captionNode != null && captionNode.isArray() && captionNode.size() > 0 
                && captionNode.get(0) != null && !captionNode.get(0).asText().trim().isEmpty();
        }
        
        // 检查是否有 table_footnote
        boolean hasFootnote = false;
        if (item.has("table_footnote")) {
            JsonNode footnoteNode = item.get("table_footnote");
            hasFootnote = footnoteNode != null && footnoteNode.isArray() && footnoteNode.size() > 0
                && footnoteNode.get(0) != null && !footnoteNode.get(0).asText().trim().isEmpty();
        }
        
        // 检查是否有 table_body
        boolean hasBody = false;
        if (item.has("table_body")) {
            String tableBody = item.get("table_body").asText();
            hasBody = tableBody != null && !tableBody.trim().isEmpty();
        }
        
        // 获取 bbox（转换为图片坐标系）
        double[] bbox = null;
        if (item.has("bbox") && item.get("bbox").isArray() && item.get("bbox").size() >= 4) {
            JsonNode bboxNode = item.get("bbox");
            int imageWidth = pageImage != null ? (Integer) pageImage.get("imageWidth") : 0;
            int imageHeight = pageImage != null ? (Integer) pageImage.get("imageHeight") : 0;
            double pdfWidth = pdfPageSize != null ? pdfPageSize[0] : 0;
            double pdfHeight = pdfPageSize != null ? pdfPageSize[1] : 0;
            
            double[] mineruBbox = extractBbox(bboxNode, pdfWidth, pdfHeight);
            bbox = convertAndValidateBbox(mineruBbox, pdfWidth, pdfHeight, imageWidth, imageHeight);
        }
        
        // 提取文本内容
        String text = "";
        if (hasBody && item.has("table_body")) {
            text = item.get("table_body").asText();
        }
        
        // 添加到跨页表格管理器
        String groupId = tableManager.addTableItem(contentListIndex, pageIdx, bbox, 
            hasCaption, hasFootnote, hasBody, text);
        
        // 记录日志
        if (!hasCaption && !hasFootnote && !hasBody) {
            log.info("📋 识别到跨页表格延续部分: 第{}页, 组ID: {}", pageIdx + 1, groupId);
        } else {
            log.debug("📋 识别到主表格: 第{}页, 组ID: {}", pageIdx + 1, groupId);
        }
    }
    
    /**
     * 转换MinerU的item为LayoutItem列表
     * 处理所有类型：普通文本、列表、表格、图片、代码等
     */
    private List<TextExtractionUtil.LayoutItem> convertToLayoutItems(
            JsonNode item,
            Map<String, Object> pageImage,
            double[] pdfPageSize,
            JsonNode middleJsonNode,
            int pageIdx,
            String docMode,
            Set<String> matchedMiddleJsonTables) {
        
        List<TextExtractionUtil.LayoutItem> items = new ArrayList<>();
        
        int imageWidth = (Integer) pageImage.get("imageWidth");
        int imageHeight = (Integer) pageImage.get("imageHeight");
        double pdfWidth = pdfPageSize[0];
        double pdfHeight = pdfPageSize[1];
        
        String itemType = item.has("type") ? item.get("type").asText() : "";
        
        log.debug("处理 MinerU 内容项，类型: {}", itemType);
        
        // 处理表格类型
        if ("table".equals(itemType)) {
            log.info("📊 [表格检测] 页{} 检测到表格，将从 middle_json 获取精确 bbox", pageIdx + 1);
            log.debug("📊 [表格检测] content_list 表格数据: {}", item.toString());
            items.addAll(handleTableItem(item, imageWidth, imageHeight, pdfWidth, pdfHeight, middleJsonNode, pageIdx, docMode, matchedMiddleJsonTables));
        }
        // 处理图片类型
        else if ("image".equals(itemType)) {
            items.addAll(handleImageItem(item, imageWidth, imageHeight, pdfWidth, pdfHeight));
        }
        // 处理代码类型
        else if ("code".equals(itemType)) {
            items.addAll(handleCodeItem(item, imageWidth, imageHeight, pdfWidth, pdfHeight));
        }
        // 处理列表类型
        else if ("list".equals(itemType) || item.has("list_items")) {
            items.addAll(handleListItem(item, imageWidth, imageHeight, pdfWidth, pdfHeight, middleJsonNode, pageIdx));
        }
        // 处理公式类型
        else if ("isolate_formula".equals(itemType) || "isolated".equals(itemType)) {
            items.addAll(handleFormulaItem(item, imageWidth, imageHeight, pdfWidth, pdfHeight));
        }
        // 处理标题类型（作为文本处理，但可以区分）
        else if ("title".equals(itemType)) {
            items.addAll(handleTextItem(item, imageWidth, imageHeight, pdfWidth, pdfHeight));
        }
        // 处理普通文本
        else if (item.has("text")) {
            items.addAll(handleTextItem(item, imageWidth, imageHeight, pdfWidth, pdfHeight, middleJsonNode, pageIdx));
        }
        
        return items;
    }
    
    /**
     * 处理表格类型的内容
     * 包括 table_caption, table_body, table_footnote
     * 从 middle_json 中获取各部分的精确 bbox
     * 
     * @param docMode 文档模式：extract表示规则抽取模式（保留HTML），old/new表示合同比对模式（去除HTML）
     */
    private List<TextExtractionUtil.LayoutItem> handleTableItem(
            JsonNode item,
            int imageWidth, int imageHeight,
            double pdfWidth, double pdfHeight,
            JsonNode middleJsonNode,
            int pageIdx,
            String docMode,
            Set<String> matchedMiddleJsonTables) {
        
        List<TextExtractionUtil.LayoutItem> items = new ArrayList<>();
        
        log.info("📊 [表格处理] ========== 开始处理表格，页{} ==========", pageIdx + 1);
        log.info("📊 [表格处理] 图片尺寸: {}x{}, PDF尺寸: {}x{}", imageWidth, imageHeight, pdfWidth, pdfHeight);
        
        // 从 middle_json 中查找对应页面的表格块
        TableBlockInfo tableBlockInfo = findTableBlocksInMiddleJson(middleJsonNode, pageIdx, item, matchedMiddleJsonTables);
        
        if (tableBlockInfo != null && tableBlockInfo.blocks != null && 
            tableBlockInfo.blocks.isArray() && tableBlockInfo.blocks.size() > 0) {
            // 使用 middle_json 中的精确 bbox 处理 table_caption 和 table_footnote
            log.info("📊 [表格处理] ✅ 从 middle_json 中找到表格精确 bbox，页{}, 子块数量: {}, middle_json页面尺寸: {}x{}", 
                pageIdx + 1, tableBlockInfo.blocks.size(), tableBlockInfo.pageWidth, tableBlockInfo.pageHeight);
            
            // 先收集所有表格子项（caption、body、footnote），然后按index排序
            // index是MinerU确定的正确阅读顺序
            List<TableSubItem> subItems = new ArrayList<>();
            int captionCount = 0, footnoteCount = 0;
            
            // 第一步：处理 middle_json 中的 caption 和 footnote
            for (int i = 0; i < tableBlockInfo.blocks.size(); i++) {
                JsonNode block = tableBlockInfo.blocks.get(i);
                String blockType = block.has("type") ? block.get("type").asText() : "";
                
                // 获取index字段（MinerU的阅读顺序）
                int blockIndex = block.has("index") ? block.get("index").asInt() : i;
                
                // 跳过 table_body，它将在后面用 content_list 逻辑处理
                if ("table_body".equals(blockType)) {
                    log.info("📊 [表格处理] 跳过 table_body（将使用 content_list 逻辑处理），index={}", blockIndex);
                    continue;
                }
                
                // 统计caption和footnote数量
                if ("table_caption".equals(blockType)) {
                    captionCount++;
                } else if ("table_footnote".equals(blockType)) {
                    footnoteCount++;
                }
                
                JsonNode bboxNode = block.get("bbox");
                
                log.info("📊 [表格处理] 处理子块 {}/{}: type={}, index={}", i + 1, tableBlockInfo.blocks.size(), blockType, blockIndex);
                
                if (bboxNode == null || !bboxNode.isArray() || bboxNode.size() < 4) {
                    log.warn("📊 [表格处理] ⚠️  子块 {} 缺少有效 bbox，跳过", blockType);
                    continue;
                }
                
                // middle_json 的 bbox 是基于页面实际尺寸的，需要先归一化到 1000x1000
                double[] rawBbox = new double[]{
                    bboxNode.get(0).asDouble(),
                    bboxNode.get(1).asDouble(),
                    bboxNode.get(2).asDouble(),
                    bboxNode.get(3).asDouble()
                };
                log.info("📊 [表格处理] middle_json 原始 bbox (页面坐标): [{}, {}, {}, {}]", 
                    rawBbox[0], rawBbox[1], rawBbox[2], rawBbox[3]);
                
                // 归一化到 1000x1000（与 content_list 保持一致）
                double[] mineruBbox = new double[]{
                    rawBbox[0] * 1000.0 / tableBlockInfo.pageWidth,
                    rawBbox[1] * 1000.0 / tableBlockInfo.pageHeight,
                    rawBbox[2] * 1000.0 / tableBlockInfo.pageWidth,
                    rawBbox[3] * 1000.0 / tableBlockInfo.pageHeight
                };
                log.info("📊 [表格处理] 归一化后 bbox (MinerU归一化): [{}, {}, {}, {}]", 
                    mineruBbox[0], mineruBbox[1], mineruBbox[2], mineruBbox[3]);
                
                // 转换到图片坐标系（使用与 content_list 相同的转换逻辑）
                double[] imageBbox = convertAndValidateBbox(mineruBbox, pdfWidth, pdfHeight, imageWidth, imageHeight);
                log.info("📊 [表格处理] 转换后 bbox (图片坐标): [{}, {}, {}, {}]", 
                    imageBbox[0], imageBbox[1], imageBbox[2], imageBbox[3]);
                
                // 提取文本内容
                String text = extractTextFromMiddleJsonBlock(block);
                
                if (text != null && !text.trim().isEmpty()) {
                    // 添加到临时列表，使用index作为排序依据
                    subItems.add(new TableSubItem(blockType, imageBbox, text, blockIndex));
                    log.info("📊 [表格处理] 收集表格子块: type={}, index={}, 文本预览: {}", 
                        blockType, blockIndex,
                        text.length() > 50 ? text.substring(0, 50) + "..." : text);
                } else {
                    log.warn("📊 [表格处理] ⚠️  子块 {} 文本为空，跳过", blockType);
                    log.info("📊 [表格处理] 子块详细信息: {}", block.toString());
                }
            }
            log.info("📊 [表格处理] 从 middle_json 收集到 {} 个子块（caption: {}, footnote: {}）", subItems.size(), captionCount, footnoteCount);
            
            // 第二步：处理 table_body（使用 content_list 的逻辑）
            log.info("📊 [表格处理] 开始处理 table_body（使用 content_list）");
            
            // 从middle_json的blocks中找到table_body的index
            int tableBodyIndex = -1;
            for (int i = 0; i < tableBlockInfo.blocks.size(); i++) {
                JsonNode block = tableBlockInfo.blocks.get(i);
                String blockType = block.has("type") ? block.get("type").asText() : "";
                if ("table_body".equals(blockType)) {
                    tableBodyIndex = block.has("index") ? block.get("index").asInt() : i;
                    log.info("📊 [表格处理] 找到 table_body 的 index={}", tableBodyIndex);
                    break;
                }
            }
            
            JsonNode bboxNode = item.get("bbox");
            
            if (bboxNode != null && bboxNode.isArray() && bboxNode.size() >= 4) {
                double[] mineruBbox = extractBbox(bboxNode, pdfWidth, pdfHeight);
                double[] imageBbox = convertAndValidateBbox(mineruBbox, pdfWidth, pdfHeight, imageWidth, imageHeight);
                
                // 处理 table_body（HTML 表格）
                if (item.has("table_body")) {
                    String tableBodyHtml = item.get("table_body").asText();
                    if (tableBodyHtml != null && !tableBodyHtml.trim().isEmpty()) {
                        // 根据 docMode 决定是否保留HTML
                        String readableTableBody = convertLatexToReadableText(tableBodyHtml);
                        
                        // 将table_body也添加到临时列表，用于排序
                        if ("extract".equals(docMode)) {
                            subItems.add(new TableSubItem("table_body", imageBbox, readableTableBody + "\n", tableBodyIndex, tableBodyHtml));
                            log.info("📊 [表格处理] 收集 table_body（保留HTML）: index={}, HTML长度={}", 
                                tableBodyIndex, tableBodyHtml.length());
                        } else {
                            subItems.add(new TableSubItem("table_body", imageBbox, readableTableBody + "\n", tableBodyIndex, null));
                            log.info("📊 [表格处理] 收集 table_body（去除HTML）: index={}, HTML长度={}", 
                                tableBodyIndex, tableBodyHtml.length());
                        }
                    }
                }
            } else {
                log.warn("📊 [表格处理] ⚠️  content_list 中缺少 bbox 信息，无法处理 table_body");
            }
            
            // 第三步：按index排序所有子项（确保顺序：caption -> body -> footnote）
            subItems.sort((a, b) -> Integer.compare(a.index, b.index));
            log.info("📊 [表格处理] 已按index排序 {} 个子项", subItems.size());
            
            // 第四步：按排序后的顺序添加到items列表
            for (TableSubItem subItem : subItems) {
                if ("table_body".equals(subItem.type)) {
                    // table_body 使用 Table 类型
                    if (subItem.htmlContent != null) {
                        items.add(new TextExtractionUtil.LayoutItem(subItem.bbox, "Table", subItem.text, subItem.htmlContent));
                    } else {
                        items.add(new TextExtractionUtil.LayoutItem(subItem.bbox, "Table", subItem.text));
                    }
                    log.info("📊 [表格处理] ✅ 添加 table_body: index={}, bbox=[{}, {}, {}, {}]", 
                        subItem.index, subItem.bbox[0], subItem.bbox[1], subItem.bbox[2], subItem.bbox[3]);
                } else {
                    // caption 和 footnote 使用 Text 类型
                    items.add(new TextExtractionUtil.LayoutItem(subItem.bbox, "Text", subItem.text));
                    log.info("📊 [表格处理] ✅ 添加 {}: index={}, bbox=[{}, {}, {}, {}], 文本预览: {}", 
                        subItem.type, subItem.index, subItem.bbox[0], subItem.bbox[1], subItem.bbox[2], subItem.bbox[3],
                        subItem.text.length() > 50 ? subItem.text.substring(0, 50) + "..." : subItem.text);
                }
            }
            log.info("📊 [表格处理] 共添加 {} 个表格子项（已按index排序）", items.size());
        } else {
            // 如果未找到 middle_json 数据，使用 content_list 中的合并 bbox（降级处理）
            log.warn("📊 [表格处理] ⚠️  未从 middle_json 中找到表格精确 bbox，使用 content_list 的合并 bbox（降级模式）");
            JsonNode bboxNode = item.get("bbox");
            
            if (bboxNode == null || !bboxNode.isArray() || bboxNode.size() < 4) {
                log.warn("📊 [表格处理] ⚠️  表格缺少 bbox 信息");
                return items;
            }
            
            double[] mineruBbox = extractBbox(bboxNode, pdfWidth, pdfHeight);
            log.info("📊 [表格处理] content_list bbox (PDF坐标): [{}, {}, {}, {}]", 
                mineruBbox[0], mineruBbox[1], mineruBbox[2], mineruBbox[3]);
            
            double[] imageBbox = convertAndValidateBbox(mineruBbox, pdfWidth, pdfHeight, imageWidth, imageHeight);
            log.info("📊 [表格处理] 转换后 bbox (图片坐标): [{}, {}, {}, {}]", 
                imageBbox[0], imageBbox[1], imageBbox[2], imageBbox[3]);
            
            // 1. 处理 table_caption（如果有）
            if (item.has("table_caption")) {
                JsonNode captionNode = item.get("table_caption");
                if (captionNode.isArray() && captionNode.size() > 0) {
                    for (JsonNode caption : captionNode) {
                        String captionText = caption.asText().trim();
                        if (!captionText.isEmpty()) {
                            // 表格标题放在表格上方
                            double captionHeight = (imageBbox[3] - imageBbox[1]) * 0.1; // 估计标题高度
                            double[] captionBbox = new double[]{
                                imageBbox[0],
                                imageBbox[1],
                                imageBbox[2],
                                imageBbox[1] + captionHeight
                            };
                            // 转换 LaTeX 格式为可读文本
                            String readableCaptionText = convertLatexToReadableText(captionText);
                            items.add(new TextExtractionUtil.LayoutItem(captionBbox, "Text", readableCaptionText + "\n"));
                        }
                    }
                }
            }
            
            // 2. 处理 table_body (HTML格式处理)
            if (item.has("table_body")) {
                String tableBody = item.get("table_body").asText();
                log.debug("表格原始HTML长度: {}", tableBody.length());
                
                // 根据 docMode 决定是否保留HTML
                if ("extract".equals(docMode)) {
                    // 规则抽取模式：保留原始HTML，同时提供可读文本
                    String cleanText = removeHtmlTags(tableBody);
                    cleanText = convertLatexToReadableText(cleanText);
                    items.add(new TextExtractionUtil.LayoutItem(imageBbox, "Table", cleanText, tableBody));
                    log.info("📝 表格保留HTML，文本长度: {}, 预览: {}", 
                        cleanText.length(), 
                        cleanText.length() > 100 ? cleanText.substring(0, 100) + "..." : cleanText);
                } else {
                    // 合同比对模式：去除HTML标签，转换为纯文本
                    String cleanText = removeHtmlTags(tableBody);
                    cleanText = convertLatexToReadableText(cleanText);
                    items.add(new TextExtractionUtil.LayoutItem(imageBbox, "Table", cleanText));
                    log.info("📝 表格去除HTML后文本长度: {}, 预览: {}", 
                        cleanText.length(), 
                        cleanText.length() > 100 ? cleanText.substring(0, 100) + "..." : cleanText);
                }
            } else {
                log.warn("⚠️  表格缺少 table_body 字段");
            }
            
            // 3. 处理 table_footnote（如果有）
            if (item.has("table_footnote")) {
                JsonNode footnoteNode = item.get("table_footnote");
                if (footnoteNode.isArray() && footnoteNode.size() > 0) {
                    for (JsonNode footnote : footnoteNode) {
                        String footnoteText = footnote.asText().trim();
                        if (!footnoteText.isEmpty()) {
                            // 表格注释放在表格下方
                            double footnoteHeight = (imageBbox[3] - imageBbox[1]) * 0.1;
                            double[] footnoteBbox = new double[]{
                                imageBbox[0],
                                imageBbox[3] - footnoteHeight,
                                imageBbox[2],
                                imageBbox[3]
                            };
                            // 转换 LaTeX 格式为可读文本
                            String readableFootnoteText = convertLatexToReadableText(footnoteText);
                            items.add(new TextExtractionUtil.LayoutItem(footnoteBbox, "Text", readableFootnoteText + "\n"));
                            log.info("📊 [表格处理] ✅ 添加 table_footnote（降级模式）: bbox=[{}, {}, {}, {}], 文本: {}", 
                                footnoteBbox[0], footnoteBbox[1], footnoteBbox[2], footnoteBbox[3], readableFootnoteText);
                        }
                    }
                }
            }
            log.info("📊 [表格处理] 从 content_list（降级模式）共添加 {} 个子块", items.size());
        }
        
        log.info("📊 [表格处理] ========== 表格处理完成，页{}，共 {} 个 LayoutItem ==========", pageIdx + 1, items.size());
        return items;
    }
    
    /**
     * 处理图片类型的内容
     * 包括 figure_caption
     */
    private List<TextExtractionUtil.LayoutItem> handleImageItem(
            JsonNode item,
            int imageWidth, int imageHeight,
            double pdfWidth, double pdfHeight) {
        
        List<TextExtractionUtil.LayoutItem> items = new ArrayList<>();
        JsonNode bboxNode = item.get("bbox");
        
        if (bboxNode == null || !bboxNode.isArray() || bboxNode.size() < 4) {
            return items;
        }
        
        double[] mineruBbox = extractBbox(bboxNode, pdfWidth, pdfHeight);
        double[] imageBbox = convertAndValidateBbox(mineruBbox, pdfWidth, pdfHeight, imageWidth, imageHeight);
        
        // 处理 figure_caption（如果有）
        if (item.has("figure_caption")) {
            JsonNode captionNode = item.get("figure_caption");
            if (captionNode.isArray() && captionNode.size() > 0) {
                for (JsonNode caption : captionNode) {
                    String captionText = caption.asText().trim();
                    if (!captionText.isEmpty()) {
                        // 图片说明文字
                        double captionHeight = (imageBbox[3] - imageBbox[1]) * 0.15;
                        double[] captionBbox = new double[]{
                            imageBbox[0],
                            imageBbox[3] - captionHeight,
                            imageBbox[2],
                            imageBbox[3]
                        };
                        // 转换 LaTeX 格式为可读文本
                        String readableCaptionText = convertLatexToReadableText(captionText);
                        items.add(new TextExtractionUtil.LayoutItem(captionBbox, "Text", readableCaptionText + "\n"));
                    }
                }
            }
        }
        
        // 注意：图片本身不提取文本，只提取caption
        log.debug("处理图片，bbox: [{}, {}, {}, {}]", imageBbox[0], imageBbox[1], imageBbox[2], imageBbox[3]);
        
        return items;
    }
    
    /**
     * 处理代码类型的内容
     * 包括 code_caption 和 code_body
     */
    private List<TextExtractionUtil.LayoutItem> handleCodeItem(
            JsonNode item,
            int imageWidth, int imageHeight,
            double pdfWidth, double pdfHeight) {
        
        List<TextExtractionUtil.LayoutItem> items = new ArrayList<>();
        JsonNode bboxNode = item.get("bbox");
        
        if (bboxNode == null || !bboxNode.isArray() || bboxNode.size() < 4) {
            return items;
        }
        
        double[] mineruBbox = extractBbox(bboxNode, pdfWidth, pdfHeight);
        double[] imageBbox = convertAndValidateBbox(mineruBbox, pdfWidth, pdfHeight, imageWidth, imageHeight);
        
        // 1. 处理 code_caption（如果有）
        if (item.has("code_caption")) {
            JsonNode captionNode = item.get("code_caption");
            if (captionNode.isArray() && captionNode.size() > 0) {
                for (JsonNode caption : captionNode) {
                    String captionText = caption.asText().trim();
                    if (!captionText.isEmpty()) {
                        double captionHeight = (imageBbox[3] - imageBbox[1]) * 0.1;
                        double[] captionBbox = new double[]{
                            imageBbox[0],
                            imageBbox[1],
                            imageBbox[2],
                            imageBbox[1] + captionHeight
                        };
                        // 转换 LaTeX 格式为可读文本
                        String readableCaptionText = convertLatexToReadableText(captionText);
                        items.add(new TextExtractionUtil.LayoutItem(captionBbox, "Text", readableCaptionText + "\n"));
                    }
                }
            }
        }
        
        // 2. 处理 code_body
        if (item.has("code_body")) {
            String codeBody = item.get("code_body").asText();
            if (!codeBody.trim().isEmpty()) {
                // 转换 LaTeX 格式为可读文本
                String readableCodeBody = convertLatexToReadableText(codeBody);
                items.add(new TextExtractionUtil.LayoutItem(imageBbox, "Text", readableCodeBody + "\n"));
            }
        }
        
        return items;
    }
    
    /**
     * 处理列表类型的内容
     * 优先从 middle_json 中获取每个列表项的精确 bbox
     */
    private List<TextExtractionUtil.LayoutItem> handleListItem(
            JsonNode item,
            int imageWidth, int imageHeight,
            double pdfWidth, double pdfHeight,
            JsonNode middleJsonNode,
            int pageIdx) {
        
        List<TextExtractionUtil.LayoutItem> items = new ArrayList<>();
        JsonNode listItemsNode = item.get("list_items");
        
        if (listItemsNode == null || !listItemsNode.isArray()) {
            return items;
        }
        
        JsonNode bboxNode = item.get("bbox");
        if (bboxNode == null || !bboxNode.isArray() || bboxNode.size() < 4) {
            return items;
        }
        
        // 从 middle_json 中获取列表数据（以 middle_json 为准）
        ListBlockInfo listBlockInfo = findListBlockInMiddleJson(middleJsonNode, pageIdx, item);
        
        if (listBlockInfo == null) {
            log.error("📋 [列表处理] ❌ 未能从 middle_json 找到列表数据，页{}，跳过该列表", pageIdx + 1);
            return items;
        }
        
        if (listBlockInfo.blocks == null || !listBlockInfo.blocks.isArray()) {
            log.error("📋 [列表处理] ❌ middle_json 列表块数据无效，页{}，跳过该列表", pageIdx + 1);
            return items;
        }
        
        int blockCount = listBlockInfo.blocks.size();
        int contentListItemCount = listItemsNode.size();
        
        log.info("📋 [列表处理] ✅ 从 middle_json 中找到列表数据，页{}, middle_json 块数量: {}, content_list 项数量: {}, 页面尺寸: {}x{}", 
            pageIdx + 1, blockCount, contentListItemCount, listBlockInfo.pageWidth, listBlockInfo.pageHeight);
        
        if (blockCount != contentListItemCount) {
            log.warn("📋 [列表处理] ⚠️  middle_json ({}) 与 content_list ({}) 数量不一致，以 middle_json 为准（使用 middle_json 的文本和坐标）", 
                blockCount, contentListItemCount);
        }
        
        // 【关键】以 middle_json 为准，遍历所有 blocks
        for (int i = 0; i < blockCount; i++) {
            JsonNode block = listBlockInfo.blocks.get(i);
            
            // 从 middle_json 的 block 中提取文本
            String itemText = extractTextFromMiddleJsonBlock(block);
            
            if (itemText == null || itemText.trim().isEmpty()) {
                log.warn("📋 [列表处理] ⚠️  列表项 {} 文本为空，跳过", i + 1);
                continue;
            }
            
            JsonNode blockBboxNode = block.get("bbox");
            if (blockBboxNode == null || !blockBboxNode.isArray() || blockBboxNode.size() < 4) {
                log.warn("📋 [列表处理] ⚠️  列表项 {} 缺少有效 bbox，跳过", i + 1);
                continue;
            }
            
            // middle_json 的 bbox 是基于页面实际尺寸的，需要先归一化到 1000x1000
            double[] rawBbox = new double[]{
                blockBboxNode.get(0).asDouble(),
                blockBboxNode.get(1).asDouble(),
                blockBboxNode.get(2).asDouble(),
                blockBboxNode.get(3).asDouble()
            };
            log.info("📋 [列表处理] 列表项 {} - middle_json 原始 bbox (页面坐标): [{}, {}, {}, {}]", 
                i + 1, rawBbox[0], rawBbox[1], rawBbox[2], rawBbox[3]);
            
            // 归一化到 1000x1000（与 content_list 保持一致）
            double[] mineruBbox = new double[]{
                rawBbox[0] * 1000.0 / listBlockInfo.pageWidth,
                rawBbox[1] * 1000.0 / listBlockInfo.pageHeight,
                rawBbox[2] * 1000.0 / listBlockInfo.pageWidth,
                rawBbox[3] * 1000.0 / listBlockInfo.pageHeight
            };
            log.info("📋 [列表处理] 列表项 {} - 归一化后 bbox (MinerU归一化): [{}, {}, {}, {}]", 
                i + 1, mineruBbox[0], mineruBbox[1], mineruBbox[2], mineruBbox[3]);
            
            // 转换到图片坐标系（使用与 content_list 相同的转换逻辑）
            double[] itemBbox = convertAndValidateBbox(mineruBbox, pdfWidth, pdfHeight, imageWidth, imageHeight);
            log.info("📋 [列表处理] 列表项 {} - 转换后 bbox (图片坐标): [{}, {}, {}, {}]", 
                i + 1, itemBbox[0], itemBbox[1], itemBbox[2], itemBbox[3]);
            
            // 转换 LaTeX 格式为可读文本
            String readableItemText = convertLatexToReadableText(itemText);
            
            items.add(new TextExtractionUtil.LayoutItem(itemBbox, "Text", readableItemText + "\n"));
            
            log.info("📋 [列表处理] 列表项 {}/{}: 文本预览（来自 middle_json）: {}", 
                i + 1, blockCount,
                readableItemText.length() > 50 ? readableItemText.substring(0, 50) + "..." : readableItemText);
        }
        
        log.info("📋 [列表处理] 完成，共处理 {} 个列表项（全部来自 middle_json）", blockCount);
        
        return items;
    }
     
    /**
     * 处理普通文本类型的内容 
     */
    private List<TextExtractionUtil.LayoutItem> handleTextItem(
            JsonNode item,
            int imageWidth, int imageHeight,
            double pdfWidth, double pdfHeight) {
        return handleTextItem(item, imageWidth, imageHeight, pdfWidth, pdfHeight, null, -1);
    }
    
    private List<TextExtractionUtil.LayoutItem> handleTextItem(
            JsonNode item,
            int imageWidth, int imageHeight,
            double pdfWidth, double pdfHeight,
            JsonNode middleJsonNode,
            int pageIdx) {
        
        List<TextExtractionUtil.LayoutItem> items = new ArrayList<>();
        JsonNode bboxNode = item.get("bbox");
        
        if (bboxNode == null || !bboxNode.isArray() || bboxNode.size() < 4) {
            return items;
        }
        
        double[] mineruBbox = extractBbox(bboxNode, pdfWidth, pdfHeight);
        double[] imageBbox = convertAndValidateBbox(mineruBbox, pdfWidth, pdfHeight, imageWidth, imageHeight);
        
        // 直接使用 content_list 的 text 字段（旧逻辑）
        // content_list 中的 text 字段已经包含了正确的 LaTeX 格式公式（用 $ 符号包围）
        String text = item.has("text") ? item.get("text").asText() : "";
        
        // 转换 LaTeX/Markdown 格式为可读文本
        String cleanText = convertLatexToReadableText(text);
        
        items.add(new TextExtractionUtil.LayoutItem(imageBbox, "Text", cleanText));
        
        return items;
    }
    
    /**
     * 处理公式类型的内容
     * 包括 isolate_formula（行间公式）和 formula_caption（公式标号）
     */
    private List<TextExtractionUtil.LayoutItem> handleFormulaItem(
            JsonNode item,
            int imageWidth, int imageHeight,
            double pdfWidth, double pdfHeight) {
        
        List<TextExtractionUtil.LayoutItem> items = new ArrayList<>();
        JsonNode bboxNode = item.get("bbox");
        
        if (bboxNode == null || !bboxNode.isArray() || bboxNode.size() < 4) {
            return items;
        }
        
        double[] mineruBbox = extractBbox(bboxNode, pdfWidth, pdfHeight);
        double[] imageBbox = convertAndValidateBbox(mineruBbox, pdfWidth, pdfHeight, imageWidth, imageHeight);
        
        // 处理公式内容（LaTeX格式）
        if (item.has("latex_text")) {
            String latexText = item.get("latex_text").asText();
            if (!latexText.trim().isEmpty()) {
                // 转换 LaTeX 格式为可读文本
                String readableText = convertLatexToReadableText(latexText);
                items.add(new TextExtractionUtil.LayoutItem(imageBbox, "Formula", readableText + "\n"));
            }
        } else if (item.has("text")) {
            String text = item.get("text").asText();
            if (!text.trim().isEmpty()) {
                // 转换 LaTeX 格式为可读文本
                String readableText = convertLatexToReadableText(text);
                items.add(new TextExtractionUtil.LayoutItem(imageBbox, "Formula", readableText + "\n"));
            }
        }
        
        // 处理公式标号（如果有）
        if (item.has("formula_caption")) {
            JsonNode captionNode = item.get("formula_caption");
            if (captionNode.isArray() && captionNode.size() > 0) {
                for (JsonNode caption : captionNode) {
                    String captionText = caption.asText().trim();
                    if (!captionText.isEmpty()) {
                        double captionHeight = (imageBbox[3] - imageBbox[1]) * 0.1;
                        double[] captionBbox = new double[]{
                            imageBbox[2] - 50,  // 通常公式标号在右侧
                            imageBbox[1],
                            imageBbox[2],
                            imageBbox[1] + captionHeight
                        };
                        // 转换公式标号中的 LaTeX 格式
                        String readableCaptionText = convertLatexToReadableText(captionText);
                        items.add(new TextExtractionUtil.LayoutItem(captionBbox, "text", readableCaptionText));
                    }
                }
            }
        }
        
        return items;
    }
    
    /**
     * 从JsonNode提取bbox坐标
     */
    private double[] extractBbox(JsonNode bboxNode, double pdfWidth, double pdfHeight) {
        double[] bbox = new double[]{
            bboxNode.get(0).asDouble(),
            bboxNode.get(1).asDouble(),
            bboxNode.get(2).asDouble(),
            bboxNode.get(3).asDouble()
        };
        
        // MinerU 使用 1000x1000 归一化坐标系统
        // 不应该用 PDF 尺寸来限制坐标！
        // 坐标范围应该是 0-1000，而不是 0-pdfWidth/pdfHeight
        final double MINERU_MAX = 1000.0;
        
        // 只修正明显异常的坐标（例如负数或超出1000）
        bbox[0] = Math.max(0, Math.min(bbox[0], MINERU_MAX));
        bbox[1] = Math.max(0, Math.min(bbox[1], MINERU_MAX));
        bbox[2] = Math.max(bbox[0], Math.min(bbox[2], MINERU_MAX));
        bbox[3] = Math.max(bbox[1], Math.min(bbox[3], MINERU_MAX));
        
        return bbox;
    }
    
    /**
     * 转换并验证bbox坐标
     */
    private double[] convertAndValidateBbox(
            double[] mineruBbox,
            double pdfWidth, double pdfHeight,
            int imageWidth, int imageHeight) {
        
        // 转换坐标
        int[] imageBbox = MinerUCoordinateConverter.convertToImageCoordinates(
            mineruBbox, pdfWidth, pdfHeight, imageWidth, imageHeight);
        
        // 修正可能的舍入误差
        if (!MinerUCoordinateConverter.isValidBbox(imageBbox, imageWidth, imageHeight)) {
            imageBbox = MinerUCoordinateConverter.clampBbox(imageBbox, imageWidth, imageHeight);
        }
        
        return new double[]{
            (double) imageBbox[0],
            (double) imageBbox[1],
            (double) imageBbox[2],
            (double) imageBbox[3]
        };
    }
    
    /**
     * 去除HTML标签，将表格HTML转换为纯文本
     * 参考 dots.ocr 的处理方式
     * 
     * @param html HTML格式的表格内容
     * @return 纯文本内容
     */
    private String removeHtmlTags(String html) {
        if (html == null || html.trim().isEmpty()) {
            return "";
        }
        
        // 1. 替换 <br>、<br/>、</tr> 为换行符
        String text = html.replaceAll("(?i)<br\\s*/?>", "\n");
        text = text.replaceAll("(?i)</tr>", "\n");
        
        // 2. 替换 <td>、<th> 的结束标签为制表符或空格
        text = text.replaceAll("(?i)</td>", "\t");
        text = text.replaceAll("(?i)</th>", "\t");
        
        // 3. 移除所有其他HTML标签
        text = text.replaceAll("<[^>]+>", "");
        
        // 4. 解码HTML实体
        text = text.replace("&nbsp;", " ");
        text = text.replace("&lt;", "<");
        text = text.replace("&gt;", ">");
        text = text.replace("&amp;", "&");
        text = text.replace("&quot;", "\"");
        text = text.replace("&apos;", "'");
        
        // 5. 清理多余的空白
        text = text.replaceAll("[ \\t]+", " ");  // 多个空格/制表符合并
        text = text.replaceAll("\\n\\s*\\n", "\n");  // 多个换行合并
        
        return text.trim();
    }
    
    /**
     * 判断是否为页眉页脚或页码
     * 
     * 【重要】仅基于MinerU明确识别的类型进行过滤，不根据位置过滤
     * MinerU已经通过VLM AI模型识别出内容类型，我们应该信任它的判断
     * 
     * 过滤以下类型（参考MinerU文档的discarded_blocks）：
     * - header: 页眉
     * - footer: 页脚
     * - page_number: 页码
     * - aside_text: 旁注文本
     * - page_footnote: 页面脚注
     * 
     * 其他所有类型（包括list, text, table, image, code等）都保留
     * 
     * @param item MinerU识别的内容块
     * @return true表示应该过滤，false表示保留
     */
    private boolean isHeaderFooterOrPageNumber(JsonNode item) {
        String type = item.has("type") ? item.get("type").asText() : "";
        
        // 仅基于MinerU识别的类型判断，过滤所有丢弃类型
        return "header".equals(type) 
            || "footer".equals(type) 
            || "page_number".equals(type)
            || "aside_text".equals(type)
            || "page_footnote".equals(type);
    }
    
    /**
     * 保存MinerU原始响应JSON
     */
    private void saveRawResponse(String apiResult, File outputDir, String taskId, String docMode) {
        try {
            // 创建统一的中间结果目录：mineru_intermediate
            File intermediateDir = new File(outputDir, "mineru_intermediate/" + docMode);
            if (!intermediateDir.exists()) {
                intermediateDir.mkdirs();
            }
            
            // 保存原始响应（完整的 MinerU API 响应）
            File rawFile = new File(intermediateDir, "01_mineru_raw_response.json");
            JsonNode jsonNode = objectMapper.readTree(apiResult);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(rawFile, jsonNode);
            
            log.info("✅ 保存 MinerU 原始响应: {}", rawFile.getAbsolutePath());
        } catch (Exception e) {
            log.error("❌ 保存 MinerU 原始响应失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 保存格式化的 content_list（方便调试 bbox）
     */
    private void saveFormattedContentList(String apiResult, File outputDir, String taskId, String docMode) {
        try {
            // 创建统一的中间结果目录：mineru_intermediate
            File intermediateDir = new File(outputDir, "mineru_intermediate/" + docMode);
            if (!intermediateDir.exists()) {
                intermediateDir.mkdirs();
            }
            
            JsonNode root = objectMapper.readTree(apiResult);
            
            // 【关键】同时保存两个独立的文件，不要覆盖
            
            // 1. 保存 middle_json（MinerU 原始中间 JSON）
            saveMiddleJson(root, intermediateDir, docMode);
            
            // 2. 保存 content_list（最终的结构化列表）
            JsonNode contentListNode = extractContentList(root);
            
            if (contentListNode != null && contentListNode.isArray()) {
                // 2.1 保存格式化的 content_list（MinerU 原始结构）
                File contentListFile = new File(intermediateDir, "02_content_list.json");
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(contentListFile, contentListNode);
                
                log.info("✅ 保存格式化的 content_list: {}, 共{}个内容项", 
                    contentListFile.getAbsolutePath(), contentListNode.size());
                
                // 2.2 保存易读格式的 content_list（包含完整文本和坐标信息）
                saveReadableContentList(contentListNode, intermediateDir, docMode);
                
                // 2.3 额外保存一个带统计信息的版本
                saveContentListWithStats(contentListNode, intermediateDir, docMode);
            } else {
                log.warn("⚠️  未找到有效的 content_list 数据，请检查 API 参数 return_content_list");
            }
        } catch (Exception e) {
            log.error("❌ 保存格式化 content_list 失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 保存 middle_json（MinerU 原始中间 JSON）
     * 
     * 【重要】这个文件和 content_list 是独立的：
     * - middle_json: MinerU 的原始布局分析数据（layout detection, OCR results 等）
     * - content_list: 经过后处理的结构化内容列表
     * 
     * 两者都保存，互不覆盖
     */
    private void saveMiddleJson(JsonNode root, File intermediateDir, String docMode) {
        try {
            // 从 results 中提取 middle_json
            JsonNode resultsNode = root.get("results");
            if (resultsNode != null && resultsNode.isObject()) {
                JsonNode firstResult = resultsNode.elements().next();
                if (firstResult != null) {
                    JsonNode middleJsonNode = firstResult.get("middle_json");
                    if (middleJsonNode != null) {
                        log.info("📊 [saveMiddleJson] middle_json 类型: {}", 
                            middleJsonNode.isTextual() ? "字符串" : (middleJsonNode.isArray() ? "数组" : "对象"));
                        
                        // 如果 middle_json 是字符串，解析为 JSON
                        if (middleJsonNode.isTextual()) {
                            JsonNode parsedMiddleJson = objectMapper.readTree(middleJsonNode.asText());
                            log.info("📊 [saveMiddleJson] 解析后类型: {}, 是否为数组: {}, 大小: {}", 
                                parsedMiddleJson.getNodeType(), 
                                parsedMiddleJson.isArray(),
                                parsedMiddleJson.isArray() ? parsedMiddleJson.size() : "N/A");
                            
                            File middleJsonFile = new File(intermediateDir, "05_middle_json.json");
                            objectMapper.writerWithDefaultPrettyPrinter().writeValue(middleJsonFile, parsedMiddleJson);
                            log.info("✅ 保存 MinerU middle_json（原始布局分析数据）: {}", middleJsonFile.getAbsolutePath());
                        } else {
                            // 如果已经是 JSON 对象，直接保存
                            log.info("📊 [saveMiddleJson] 直接保存，是否为数组: {}, 大小: {}", 
                                middleJsonNode.isArray(),
                                middleJsonNode.isArray() ? middleJsonNode.size() : "N/A");
                            
                            File middleJsonFile = new File(intermediateDir, "05_middle_json.json");
                            objectMapper.writerWithDefaultPrettyPrinter().writeValue(middleJsonFile, middleJsonNode);
                            log.info("✅ 保存 MinerU middle_json（原始布局分析数据）: {}", middleJsonFile.getAbsolutePath());
                        }
                        return;
                    }
                }
            }
            
            log.warn("⚠️  未找到 middle_json 数据（MinerU API 可能未返回此字段，请检查 API 参数 return_middle_json）");
            
        } catch (Exception e) {
            log.error("❌ 保存 middle_json 失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 保存易读格式的 content_list
     * 展开所有字段，方便查看和调试
     */
    private void saveReadableContentList(JsonNode contentListNode, File intermediateDir, String docMode) {
        try {
            List<Map<String, Object>> readableList = new ArrayList<>();
            
            int index = 0;
            for (JsonNode item : contentListNode) {
                Map<String, Object> readableItem = new LinkedHashMap<>();
                
                // 基本信息
                readableItem.put("序号", ++index);
                readableItem.put("页码", item.has("page_idx") ? item.get("page_idx").asInt() + 1 : "未知");
                readableItem.put("类型", item.has("type") ? item.get("type").asText() : "unknown");
                
                if (item.has("sub_type")) {
                    readableItem.put("子类型", item.get("sub_type").asText());
                }
                
                // 坐标信息（MinerU 使用 1000x1000 归一化坐标）
                if (item.has("bbox") && item.get("bbox").isArray() && item.get("bbox").size() >= 4) {
                    JsonNode bbox = item.get("bbox");
                    Map<String, Object> bboxInfo = new LinkedHashMap<>();
                    bboxInfo.put("x0", bbox.get(0).asDouble());
                    bboxInfo.put("y0", bbox.get(1).asDouble());
                    bboxInfo.put("x1", bbox.get(2).asDouble());
                    bboxInfo.put("y1", bbox.get(3).asDouble());
                    bboxInfo.put("宽度", bbox.get(2).asDouble() - bbox.get(0).asDouble());
                    bboxInfo.put("高度", bbox.get(3).asDouble() - bbox.get(1).asDouble());
                    bboxInfo.put("说明", "MinerU归一化坐标系（0-1000）");
                    readableItem.put("坐标", bboxInfo);
                }
                
                // 文本内容（根据类型提取）
                String contentType = item.has("type") ? item.get("type").asText() : "";
                Map<String, Object> contentInfo = new LinkedHashMap<>();
                
                if ("text".equals(contentType) || "title".equals(contentType)) {
                    if (item.has("text")) {
                        contentInfo.put("文本", item.get("text").asText());
                    }
                } else if ("table".equals(contentType)) {
                    if (item.has("table_caption")) {
                        contentInfo.put("表格标题", item.get("table_caption"));
                    }
                    if (item.has("table_body")) {
                        String tableBody = item.get("table_body").asText();
                        contentInfo.put("表格内容_长度", tableBody.length());
                        contentInfo.put("表格内容_预览", tableBody.length() > 200 ? 
                            tableBody.substring(0, 200) + "..." : tableBody);
                    }
                    if (item.has("table_footnote")) {
                        contentInfo.put("表格注释", item.get("table_footnote"));
                    }
                } else if ("list".equals(contentType)) {
                    if (item.has("list_items")) {
                        contentInfo.put("列表项", item.get("list_items"));
                    }
                } else if ("image".equals(contentType)) {
                    if (item.has("figure_caption")) {
                        contentInfo.put("图片标题", item.get("figure_caption"));
                    }
                } else if ("code".equals(contentType)) {
                    if (item.has("code_caption")) {
                        contentInfo.put("代码标题", item.get("code_caption"));
                    }
                    if (item.has("code_body")) {
                        String codeBody = item.get("code_body").asText();
                        contentInfo.put("代码内容_长度", codeBody.length());
                        contentInfo.put("代码内容_预览", codeBody.length() > 200 ? 
                            codeBody.substring(0, 200) + "..." : codeBody);
                    }
                } else if ("isolate_formula".equals(contentType) || "isolated".equals(contentType)) {
                    if (item.has("latex_text")) {
                        contentInfo.put("公式LaTeX", item.get("latex_text").asText());
                    }
                    if (item.has("formula_caption")) {
                        contentInfo.put("公式标号", item.get("formula_caption"));
                    }
                } else if (item.has("text")) {
                    contentInfo.put("文本", item.get("text").asText());
                }
                
                if (!contentInfo.isEmpty()) {
                    readableItem.put("内容", contentInfo);
                }
                
                readableList.add(readableItem);
            }
            
            // 保存易读格式的 JSON
            File readableFile = new File(intermediateDir, "03_content_list_readable.json");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(readableFile, readableList);
            
            log.info("✅ 保存易读格式的 content_list: {}", readableFile.getAbsolutePath());
            
        } catch (Exception e) {
            log.error("❌ 保存易读格式 content_list 失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 保存带统计信息的 content_list
     */
    private void saveContentListWithStats(JsonNode contentListNode, File intermediateDir, String docMode) {
        try {
            File statsFile = new File(intermediateDir, "04_content_list_stats.txt");
            
            StringBuilder stats = new StringBuilder();
            stats.append("=".repeat(80)).append("\n");
            stats.append("MinerU Content List 统计信息\n");
            stats.append("=".repeat(80)).append("\n\n");
            
            // 统计各类型数量
            Map<String, Integer> typeCount = new HashMap<>();
            Map<Integer, Integer> pageCount = new HashMap<>();
            
            for (JsonNode item : contentListNode) {
                String type = item.has("type") ? item.get("type").asText() : "unknown";
                int pageIdx = item.has("page_idx") ? item.get("page_idx").asInt() : 0;
                
                typeCount.put(type, typeCount.getOrDefault(type, 0) + 1);
                pageCount.put(pageIdx, pageCount.getOrDefault(pageIdx, 0) + 1);
            }
            
            stats.append("总内容项数: ").append(contentListNode.size()).append("\n\n");
            
            stats.append("按类型统计:\n");
            stats.append("-".repeat(40)).append("\n");
            typeCount.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .forEach(entry -> stats.append(String.format("  %-20s: %d\n", entry.getKey(), entry.getValue())));
            
            stats.append("\n按页面统计:\n");
            stats.append("-".repeat(40)).append("\n");
            pageCount.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> stats.append(String.format("  第%d页: %d个内容项\n", entry.getKey() + 1, entry.getValue())));
            
            stats.append("\n").append("=".repeat(80)).append("\n");
            stats.append("详细内容项信息\n");
            stats.append("=".repeat(80)).append("\n\n");
            
            // 详细列出每个内容项
            int index = 0;
            for (JsonNode item : contentListNode) {
                index++;
                String type = item.has("type") ? item.get("type").asText() : "unknown";
                int pageIdx = item.has("page_idx") ? item.get("page_idx").asInt() : 0;
                
                stats.append(String.format("[%d] 第%d页 - 类型: %s\n", index, pageIdx + 1, type));
                
                // bbox 信息
                if (item.has("bbox")) {
                    JsonNode bbox = item.get("bbox");
                    if (bbox.isArray() && bbox.size() >= 4) {
                        stats.append(String.format("    bbox: [%.1f, %.1f, %.1f, %.1f]\n",
                            bbox.get(0).asDouble(),
                            bbox.get(1).asDouble(),
                            bbox.get(2).asDouble(),
                            bbox.get(3).asDouble()));
                        
                        // 计算宽高
                        double width = bbox.get(2).asDouble() - bbox.get(0).asDouble();
                        double height = bbox.get(3).asDouble() - bbox.get(1).asDouble();
                        stats.append(String.format("    尺寸: %.1f x %.1f\n", width, height));
                    }
                }
                
                // 文本预览
                if (item.has("text")) {
                    String text = item.get("text").asText();
                    String preview = text.length() > 100 ? text.substring(0, 100) + "..." : text;
                    stats.append("    文本: ").append(preview).append("\n");
                } else if (item.has("list_items")) {
                    JsonNode listItems = item.get("list_items");
                    stats.append("    列表项数: ").append(listItems.size()).append("\n");
                    if (listItems.size() > 0) {
                        String firstItem = listItems.get(0).asText();
                        String preview = firstItem.length() > 50 ? firstItem.substring(0, 50) + "..." : firstItem;
                        stats.append("    第一项: ").append(preview).append("\n");
                    }
                } else if (item.has("table_body")) {
                    String tableBody = item.get("table_body").asText();
                    stats.append("    表格HTML长度: ").append(tableBody.length()).append("\n");
                }
                
                // sub_type
                if (item.has("sub_type")) {
                    stats.append("    子类型: ").append(item.get("sub_type").asText()).append("\n");
                }
                
                stats.append("\n");
            }
            
            Files.write(statsFile.toPath(), stats.toString().getBytes(StandardCharsets.UTF_8));
            
            log.info("✅ 保存 content_list 统计信息: {}", statsFile.getAbsolutePath());
        } catch (Exception e) {
            log.error("❌ 保存统计信息失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 将 LaTeX/Markdown 格式转换为人类可读的纯文本
     * 参考 dots.ocr 的公式处理方式
     * 
     * 关键设计：只处理 $...$ 或 $$...$$ 包裹的公式内容
     * 
     * @param text 包含 LaTeX/Markdown 格式的文本
     * @return 转换后的纯文本
     */
    private String convertLatexToReadableText(String text) { 
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        log.debug("🔍 [公式转换] 输入文本: {}", text.length() > 200 ? text.substring(0, 200) + "..." : text);
        
        StringBuilder result = new StringBuilder();
        int i = 0;
        int formulaCount = 0;
        
        while (i < text.length()) {
            // 检查是否是行间公式 $$...$$
            if (i < text.length() - 1 && text.charAt(i) == '$' && text.charAt(i + 1) == '$') {
                int endPos = text.indexOf("$$", i + 2);
                if (endPos != -1) {
                    // 提取公式内容（不包括 $$ 符号）
                    String formula = text.substring(i + 2, endPos);
                    log.debug("  📐 发现行间公式 $$...$$: {}", formula);
                    // 转换公式内容
                    String converted = convertLatexFormula(formula);
                    log.debug("  ✅ 转换结果: {}", converted);
                    result.append(converted);
                    i = endPos + 2;  // 跳过结束的 $$
                    formulaCount++;
                    continue;
                }
            }
            
            // 检查是否是行内公式 $...$
            if (text.charAt(i) == '$') {
                int endPos = text.indexOf('$', i + 1);
                if (endPos != -1) {
                    // 提取公式内容（不包括 $ 符号）
                    String formula = text.substring(i + 1, endPos);
                    log.debug("  📐 发现行内公式 $...$: {}", formula);
                    // 转换公式内容
                    String converted = convertLatexFormula(formula);
                    log.debug("  ✅ 转换结果: {}", converted);
                    result.append(converted);
                    i = endPos + 1;  // 跳过结束的 $
                    formulaCount++;
                    continue;
                }
            }
            
            // 不是公式，直接添加字符
            result.append(text.charAt(i));
            i++;
        }
        
        String finalResult = result.toString();
        
        // 清理多余的空格
        finalResult = finalResult.replaceAll("\\s+", " ");
        finalResult = finalResult.trim();
        
        log.debug("🎯 [公式转换] 共处理 {} 个公式, 最终结果: {}", formulaCount, 
            finalResult.length() > 200 ? finalResult.substring(0, 200) + "..." : finalResult);
        
        return finalResult;
    }
    
    /**
     * 保存图片到文件（PNG 无损格式）
     * 
     * @param image 图片对象
     * @param imageFile 输出文件
     */
    private void saveImage(BufferedImage image, File imageFile) throws IOException {
        // 保存为 PNG 格式（无损）
        ImageIO.write(image, "PNG", imageFile);
    }
    
    /**
     * 转换 LaTeX 公式内容（不包括 $ 符号）
     * 
     * @param formula 公式内容
     * @return 转换后的可读文本
     */
    private String convertLatexFormula(String formula) {
        log.debug("    🔧 [convertLatexFormula] 输入: {}", formula);
        String result = formula;
        
        // 0. 先处理双反斜杠的特殊情况（在公式内常见）
        // 注意：必须先处理具体的命令，再处理通用的双反斜杠
        result = result.replace("\\\\text\\{", "PLACEHOLDER_TEXT_START");
        result = result.replace("\\\\mathrm\\{", "PLACEHOLDER_MATHRM_START");
        result = result.replace("\\\\mathbb\\{", "PLACEHOLDER_MATHBB_START");
        result = result.replace("\\\\mathcal\\{", "PLACEHOLDER_MATHCAL_START");
        result = result.replace("\\\\textbf\\{", "PLACEHOLDER_TEXTBF_START");
        result = result.replace("\\\\textit\\{", "PLACEHOLDER_TEXTIT_START");
        result = result.replace("\\\\underline\\{", "PLACEHOLDER_UNDERLINE_START");
        
        if (!result.equals(formula)) {
            log.debug("      → 替换双反斜杠命令后: {}", result);
        }
        
        // 处理省略号
        result = result.replace("\\\\ldots", "PLACEHOLDER_LDOTS");
        result = result.replace("\\\\cdots", "PLACEHOLDER_CDOTS");
        result = result.replace("\\\\dots", "PLACEHOLDER_DOTS");
        
        // 处理特殊符号
        result = result.replace("\\\\%", "PLACEHOLDER_PERCENT");
        result = result.replace("\\\\sim", "PLACEHOLDER_SIM");
        result = result.replace("\\\\cdot", "PLACEHOLDER_CDOT");
        result = result.replace("\\\\,", "PLACEHOLDER_THINSPACE");
        result = result.replace("\\\\:", "PLACEHOLDER_MEDSPACE");
        result = result.replace("\\\\;", "PLACEHOLDER_THICKSPACE");
        result = result.replace("\\\\quad", "PLACEHOLDER_QUAD");
        result = result.replace("\\\\qquad", "PLACEHOLDER_QQUAD");
        result = result.replace("\\\\ ", "PLACEHOLDER_SPACE");
        result = result.replace("\\\\\\\\", "PLACEHOLDER_DOUBLEBACKSLASH");
        
        // 1. 处理分数 \frac{a}{b} 转为 a/b
        result = result.replaceAll("\\\\frac\\s*\\{([^}]+)\\}\\s*\\{([^}]+)\\}", "$1/$2");
        // 处理简写的分数 \frac12 -> 1/2, \frac34 -> 3/4
        result = result.replaceAll("\\\\frac(\\d)(\\d)", "$1/$2");
        
        // 2. 处理常见的 LaTeX 数学符号
        // 希腊字母
        result = result.replaceAll("\\\\Phi\\b", "Φ");
        result = result.replaceAll("\\\\phi\\b", "φ");
        result = result.replaceAll("\\\\alpha\\b", "α");
        result = result.replaceAll("\\\\beta\\b", "β");
        result = result.replaceAll("\\\\gamma\\b", "γ");
        result = result.replaceAll("\\\\Gamma\\b", "Γ");
        result = result.replaceAll("\\\\delta\\b", "δ");
        result = result.replaceAll("\\\\Delta\\b", "Δ");
        result = result.replaceAll("\\\\epsilon\\b", "ε");
        result = result.replaceAll("\\\\theta\\b", "θ");
        result = result.replaceAll("\\\\Theta\\b", "Θ");
        result = result.replaceAll("\\\\lambda\\b", "λ");
        result = result.replaceAll("\\\\Lambda\\b", "Λ");
        result = result.replaceAll("\\\\mu\\b", "μ");
        result = result.replaceAll("\\\\pi\\b", "π");
        result = result.replaceAll("\\\\Pi\\b", "Π");
        result = result.replaceAll("\\\\sigma\\b", "σ");
        result = result.replaceAll("\\\\Sigma\\b", "Σ");
        result = result.replaceAll("\\\\omega\\b", "ω");
        result = result.replaceAll("\\\\Omega\\b", "Ω");
        
        // 3. 处理比较符号
        result = result.replaceAll("\\\\leq\\b", "≤");
        result = result.replaceAll("\\\\leqslant\\b", "≤");  // \leqslant 也是小于等于
        result = result.replaceAll("\\\\geq\\b", "≥");
        result = result.replaceAll("\\\\geqslant\\b", "≥");  // \geqslant 也是大于等于
        result = result.replaceAll("\\\\neq\\b", "≠");
        result = result.replaceAll("\\\\approx\\b", "≈");
        result = result.replaceAll("\\\\equiv\\b", "≡");
        result = result.replaceAll("\\\\times\\b", "×");
        result = result.replaceAll("\\\\div\\b", "÷");
        result = result.replaceAll("\\\\pm\\b", "±");
        result = result.replaceAll("\\\\mp\\b", "∓");
        
        // 4. 处理特殊数学符号
        result = result.replaceAll("\\\\sim\\b", "~");
        result = result.replaceAll("\\\\cdot\\b", "·");
        result = result.replaceAll("\\\\circ\\b", "°");
        result = result.replaceAll("\\\\infty\\b", "∞");
        result = result.replaceAll("\\\\partial\\b", "∂");
        result = result.replaceAll("\\\\nabla\\b", "∇");
        
        // 5. 处理省略号（单反斜杠版本）
        result = result.replaceAll("\\\\ldots\\b", "…");
        result = result.replaceAll("\\\\cdots\\b", "⋯");
        result = result.replaceAll("\\\\dots\\b", "…");
        
        // 6. 处理求和、积分等符号
        result = result.replaceAll("\\\\sum\\b", "∑");
        result = result.replaceAll("\\\\int\\b", "∫");
        result = result.replaceAll("\\\\prod\\b", "∏");
        result = result.replaceAll("\\\\lim\\b", "lim");
        
        // 7. 处理平方根
        result = result.replaceAll("\\\\sqrt\\{([^}]+)\\}", "√($1)");
        result = result.replaceAll("\\\\sqrt\\[([^]]+)\\]\\{([^}]+)\\}", "$1√($2)");
        
        // 8. 处理箭头
        result = result.replaceAll("\\\\rightarrow\\b", "→");
        result = result.replaceAll("\\\\leftarrow\\b", "←");
        result = result.replaceAll("\\\\Rightarrow\\b", "⇒");
        result = result.replaceAll("\\\\Leftarrow\\b", "⇐");
        result = result.replaceAll("\\\\leftrightarrow\\b", "↔");
        result = result.replaceAll("\\\\Leftrightarrow\\b", "⇔");
        
        // 9. 处理下标 _{...} 和 ^{...}
        result = result.replaceAll("_\\{([^}]+)\\}", "$1");
        result = result.replaceAll("\\^\\{([^}]+)\\}", "$1");
        result = result.replaceAll("_([a-zA-Z0-9])", "$1");
        result = result.replaceAll("\\^([a-zA-Z0-9])", "$1");
        
        // 10. 处理单反斜杠的文本命令
        result = result.replaceAll("\\\\text\\{([^}]+)\\}", "$1");
        result = result.replaceAll("\\\\text([^a-zA-Z])", "$1");
        result = result.replaceAll("\\\\mathbb\\{([^}]+)\\}", "$1");
        result = result.replaceAll("\\\\mathcal\\{([^}]+)\\}", "$1");
        result = result.replaceAll("\\\\mathcalL", "L");
        result = result.replaceAll("\\\\mathrm\\{([^}]+)\\}", "$1");
        result = result.replaceAll("\\\\textbf\\{([^}]+)\\}", "$1");
        result = result.replaceAll("\\\\textit\\{([^}]+)\\}", "$1");
        
        // 恢复占位符为最终内容（去掉命令,只保留内容）
        // 使用正则表达式提取 PLACEHOLDER_XXX_START{...} 中的内容
        result = result.replaceAll("PLACEHOLDER_TEXT_START\\{([^}]+)\\}", "$1");
        result = result.replaceAll("PLACEHOLDER_MATHRM_START\\{([^}]+)\\}", "$1");
        result = result.replaceAll("PLACEHOLDER_MATHBB_START\\{([^}]+)\\}", "$1");
        result = result.replaceAll("PLACEHOLDER_MATHCAL_START\\{([^}]+)\\}", "$1");
        result = result.replaceAll("PLACEHOLDER_TEXTBF_START\\{([^}]+)\\}", "$1");
        result = result.replaceAll("PLACEHOLDER_TEXTIT_START\\{([^}]+)\\}", "$1");
        result = result.replaceAll("PLACEHOLDER_UNDERLINE_START\\{([^}]+)\\}", "$1");
        
        // 11. 处理下划线
        result = result.replaceAll("\\\\underline\\{([^}]+)\\}", "$1");
        result = result.replaceAll("\\\\underline\\s+", "");
        
        // 12. 处理左右括号
        result = result.replaceAll("\\\\left\\(", "(");
        result = result.replaceAll("\\\\right\\)", ")");
        result = result.replaceAll("\\\\left\\[", "[");
        result = result.replaceAll("\\\\right\\]", "]");
        result = result.replaceAll("\\\\left\\{", "{");
        result = result.replaceAll("\\\\right\\}", "}");
        result = result.replaceAll("\\\\left\\|", "|");
        result = result.replaceAll("\\\\right\\|", "|");
        
        // 13. 恢复占位符
        result = result.replace("PLACEHOLDER_LDOTS", "…");
        result = result.replace("PLACEHOLDER_CDOTS", "⋯");
        result = result.replace("PLACEHOLDER_DOTS", "…");
        result = result.replace("PLACEHOLDER_PERCENT", "%");
        result = result.replace("PLACEHOLDER_SIM", "~");
        result = result.replace("PLACEHOLDER_CDOT", "·");
        result = result.replace("PLACEHOLDER_THINSPACE", " ");
        result = result.replace("PLACEHOLDER_MEDSPACE", " ");
        result = result.replace("PLACEHOLDER_THICKSPACE", " ");
        result = result.replace("PLACEHOLDER_QUAD", " ");
        result = result.replace("PLACEHOLDER_QQUAD", "  ");
        result = result.replace("PLACEHOLDER_SPACE", " ");
        result = result.replace("PLACEHOLDER_DOUBLEBACKSLASH", "");
        
        // 14. 处理特殊符号（单反斜杠的情况）
        result = result.replace("\\%", "%");
        result = result.replace("\\&", "&");
        result = result.replace("\\#", "#");
        result = result.replace("\\_", "_");
        result = result.replace("\\$", "$");
        result = result.replace("\\{", "{");
        result = result.replace("\\}", "}");
        
        // 15. 清理多余的空格
        result = result.replaceAll("\\s+", " ");
        result = result.trim();
        
        log.debug("    🎯 [convertLatexFormula] 输出: {}", result);
        
        return result;
    }
    
    /**
     * 从 API 结果中提取 middle_json
     */
    private JsonNode extractMiddleJson(JsonNode root) {
        log.info("📊 [middle_json] 开始提取 middle_json");
        log.debug("📊 [middle_json] root 字段列表: {}", root.fieldNames());
        try {
            JsonNode resultsNode = root.get("results");
            log.info("📊 [middle_json] resultsNode 是否为 null: {}", resultsNode == null);
            if (resultsNode != null) {
                log.info("📊 [middle_json] resultsNode 类型: {}", resultsNode.getNodeType());
                log.info("📊 [middle_json] resultsNode 是否为对象: {}", resultsNode.isObject());
                log.debug("📊 [middle_json] resultsNode 字段: {}", resultsNode.fieldNames());
            }
            if (resultsNode != null && resultsNode.isObject()) {
                log.info("📊 [middle_json] 找到 results 节点");
                JsonNode firstResult = resultsNode.elements().next();
                if (firstResult != null) {
                    log.info("📊 [middle_json] 找到第一个 result");
                    log.debug("📊 [middle_json] firstResult 字段: {}", firstResult.fieldNames());
                    JsonNode middleJsonNode = firstResult.get("middle_json");
                    if (middleJsonNode != null) {
                        log.info("📊 [middle_json] 找到 middle_json 节点，类型: {}", 
                            middleJsonNode.isTextual() ? "字符串" : "对象");
                        // 如果 middle_json 是字符串，解析为 JSON
                        if (middleJsonNode.isTextual()) {
                            JsonNode parsed = objectMapper.readTree(middleJsonNode.asText());
                            log.info("📊 [middle_json] 解析后类型: {}, 是否为数组: {}", 
                                parsed.getNodeType(), parsed.isArray());
                            if (parsed.isArray()) {
                                log.info("📊 [middle_json] ✅ 成功解析 middle_json（数组），页数: {}", parsed.size());
                                return parsed;
                            } else if (parsed.isObject()) {
                                log.info("📊 [middle_json] 解析出的是对象，检查是否包含 pdf_info 字段");
                                // MinerU 的 middle_json 格式: { "pdf_info": [...] }
                                if (parsed.has("pdf_info")) {
                                    JsonNode pdfInfo = parsed.get("pdf_info");
                                    if (pdfInfo != null && pdfInfo.isArray()) {
                                        log.info("📊 [middle_json] ✅ 成功提取 pdf_info 数组，页数: {}", pdfInfo.size());
                                        return pdfInfo;
                                    } else {
                                        log.warn("📊 [middle_json] ⚠️  pdf_info 不是数组");
                                    }
                                } else {
                                    log.warn("📊 [middle_json] ⚠️  对象中没有 pdf_info 字段");
                                }
                                return null;
                            } else {
                                log.warn("📊 [middle_json] ⚠️  未知类型: {}", parsed.getNodeType());
                                return null;
                            }
                        }
                        log.info("📊 [middle_json] ✅ 成功获取 middle_json，是否为数组: {}, 页数: {}", 
                            middleJsonNode.isArray(),
                            middleJsonNode.isArray() ? middleJsonNode.size() : "N/A");
                        return middleJsonNode;
                    } else {
                        log.warn("📊 [middle_json] ⚠️  result 中没有 middle_json 字段");
                    }
                } else {
                    log.warn("📊 [middle_json] ⚠️  results 节点为空");
                }
            } else {
                log.warn("📊 [middle_json] ⚠️  root 中没有 results 节点或不是对象");
            }
        } catch (Exception e) {
            log.error("📊 [middle_json] ❌ 提取失败: {}", e.getMessage(), e);
        }
        log.warn("📊 [middle_json] ❌ 未能提取 middle_json，将使用降级模式");
        return null;
    }
    
    /**
     * 从 middle_json 中查找对应页面的表格块
     * @param middleJsonNode middle_json 数据
     * @param pageIdx 页索引（0-based）
     * @param contentItem content_list 中的表格项（用于匹配）
     * @param matchedMiddleJsonTables 已匹配的middle_json表格集合（防止重复匹配）
     * @return 表格块信息（包含子块数组和页面尺寸）
     */
    private TableBlockInfo findTableBlocksInMiddleJson(JsonNode middleJsonNode, int pageIdx, JsonNode contentItem, Set<String> matchedMiddleJsonTables) {
        log.info("📊 [表格匹配] 开始在 middle_json 中查找表格，页{}", pageIdx + 1);
        
        if (middleJsonNode == null || !middleJsonNode.isArray()) {
            log.warn("📊 [表格匹配] ⚠️  middle_json 为空或不是数组");
            return null;
        }
        
        log.info("📊 [表格匹配] middle_json 总页数: {}", middleJsonNode.size());
        
        // 检查content_list中是否有table_footnote
        boolean hasContentListFootnote = false;
        if (contentItem.has("table_footnote")) {
            JsonNode footnoteNode = contentItem.get("table_footnote");
            if (footnoteNode.isArray() && footnoteNode.size() > 0) {
                hasContentListFootnote = true;
                log.info("📊 [表格匹配] content_list中有table_footnote: {}", footnoteNode.toString());
            }
        }
        
        try {
            // 提取 content_list 中表格的文本内容（用于匹配）
            String tableBodyText = "";
            if (contentItem.has("table_body")) {
                tableBodyText = contentItem.get("table_body").asText();
                // 去除HTML标签
                tableBodyText = removeHtmlTags(tableBodyText).trim();
                log.info("📊 [表格匹配] content_list 表格文本长度: {}, 预览: {}", 
                    tableBodyText.length(), 
                    tableBodyText.length() > 100 ? tableBodyText.substring(0, 100) + "..." : tableBodyText);
            } else {
                log.warn("📊 [表格匹配] ⚠️  content_list 中没有 table_body 字段");
            }
            
            // 遍历 middle_json 中的页面（pdf_info 数组）
            if (pageIdx < middleJsonNode.size()) {
                JsonNode pageNode = middleJsonNode.get(pageIdx);
                
                // 提取页面尺寸信息（用于坐标转换）
                JsonNode pageSizeNode = pageNode.get("page_size");
                double middleJsonPageWidth = 0;
                double middleJsonPageHeight = 0;
                if (pageSizeNode != null && pageSizeNode.isArray() && pageSizeNode.size() >= 2) {
                    middleJsonPageWidth = pageSizeNode.get(0).asDouble();
                    middleJsonPageHeight = pageSizeNode.get(1).asDouble();
                    log.info("📊 [表格匹配] middle_json 页面尺寸: {}x{}", middleJsonPageWidth, middleJsonPageHeight);
                }
                
                // MinerU middle_json 字段名是 para_blocks（不是 preproc_blocks）
                JsonNode paraBlocks = pageNode.get("para_blocks");
                
                log.info("📊 [表格匹配] 页{} para_blocks 块数量: {}", 
                    pageIdx + 1, 
                    paraBlocks != null ? paraBlocks.size() : 0);
                
                if (paraBlocks != null && paraBlocks.isArray()) {
                    int tableCount = 0;
                    // 先检查content_list中表格是否有caption或footnote
                    boolean hasContentListCaption = contentItem.has("table_caption") && 
                        contentItem.get("table_caption").isArray() && 
                        contentItem.get("table_caption").size() > 0;
                    boolean hasContentListFootnoteLocal = hasContentListFootnote;
                    
                    // 遍历页面中的所有块
                    for (int i = 0; i < paraBlocks.size(); i++) {
                        JsonNode block = paraBlocks.get(i);
                        String blockType = block.has("type") ? block.get("type").asText() : "";
                        
                        if ("table".equals(blockType)) {
                            tableCount++;
                            int blockIndex = block.has("index") ? block.get("index").asInt() : i;
                            String tableKey = pageIdx + "-" + blockIndex;
                            
                            log.info("📊 [表格匹配] 找到第 {} 个表格块（块索引 {}, index={}）", tableCount, i, blockIndex);
                            
                            // 检查这个表格是否已经被匹配过
                            if (matchedMiddleJsonTables.contains(tableKey)) {
                                log.info("📊 [表格匹配] ⚠️  表格 {} 已被匹配使用，跳过", tableKey);
                                continue;
                            }
                            
                            JsonNode subBlocks = block.get("blocks");
                            if (subBlocks != null && subBlocks.isArray()) {
                                log.info("📊 [表格匹配] 表格有 {} 个子块", subBlocks.size());
                                
                                // 如果content_list有caption/footnote，优先匹配有多个子块的表格
                                if ((hasContentListCaption || hasContentListFootnoteLocal) && subBlocks.size() == 1) {
                                    log.info("📊 [表格匹配] ⚠️  content_list有caption/footnote但此表格只有1个子块，跳过（可能是跨页表格延续）");
                                    continue;
                                }
                                
                                // 检查这个表格是否匹配（通过对比 table_body 的文本）
                                for (JsonNode subBlock : subBlocks) {
                                    String subType = subBlock.has("type") ? subBlock.get("type").asText() : "";
                                    if ("table_body".equals(subType)) {
                                        String middleBodyText = extractTextFromMiddleJsonBlock(subBlock);
                                        log.info("📊 [表格匹配] table_body 文本长度: {}, 预览: {}", 
                                            middleBodyText != null ? middleBodyText.length() : 0,
                                            middleBodyText != null && middleBodyText.length() > 100 ? 
                                                middleBodyText.substring(0, 100) + "..." : middleBodyText);
                                        
                                        // 简单匹配：如果文本内容相似，认为是同一个表格
                                        if (middleBodyText != null && middleBodyText.length() >= 50 && 
                                            tableBodyText.contains(middleBodyText.substring(0, Math.min(50, middleBodyText.length())))) {
                                            // 标记为已匹配
                                            matchedMiddleJsonTables.add(tableKey);
                                            log.info("📊 [表格匹配] ✅ 匹配成功！标记表格 {} 为已使用，返回表格子块", tableKey);
                                            return new TableBlockInfo(subBlocks, middleJsonPageWidth, middleJsonPageHeight);
                                        } else if (middleBodyText != null && middleBodyText.length() < 50 && 
                                                   tableBodyText.contains(middleBodyText)) {
                                            // 标记为已匹配
                                            matchedMiddleJsonTables.add(tableKey);
                                            log.info("📊 [表格匹配] ✅ 匹配成功（短文本）！标记表格 {} 为已使用，返回表格子块", tableKey);
                                            return new TableBlockInfo(subBlocks, middleJsonPageWidth, middleJsonPageHeight);
                                        } else {
                                            log.warn("📊 [表格匹配] ❌ 文本不匹配，继续查找");
                                        }
                                    }
                                }
                            } else {
                                log.warn("📊 [表格匹配] ⚠️  表格块没有子块");
                            }
                        }
                    }
                    log.warn("📊 [表格匹配] 页{} 共找到 {} 个表格块，但都不匹配", pageIdx + 1, tableCount);
                } else {
                    log.warn("📊 [表格匹配] ⚠️  页{} 没有 para_blocks", pageIdx + 1);
                }
            } else {
                log.warn("📊 [表格匹配] ⚠️  页索引 {} 超出范围（总页数: {}）", pageIdx, middleJsonNode.size());
            }
        } catch (Exception e) {
            log.error("📊 [表格匹配] ❌ 查找失败: {}", e.getMessage(), e);
        }
        
        log.warn("📊 [表格匹配] ❌ 未找到匹配的表格");
        return null;
    }
    
    /**
     * 表格块信息包装类（包含块数据和页面尺寸）
     */
    private static class TableBlockInfo {
        JsonNode blocks;
        double pageWidth;
        double pageHeight;
        
        public TableBlockInfo(JsonNode blocks, double pageWidth, double pageHeight) {
            this.blocks = blocks;
            this.pageWidth = pageWidth;
            this.pageHeight = pageHeight;
        }
    }
    
    /**
     * 列表块信息包装类（包含块数据和页面尺寸）
     */
    private static class ListBlockInfo {
        JsonNode blocks;  // 列表的子块数组（每个子块对应一个列表项）
        double pageWidth;
        double pageHeight;
        
        public ListBlockInfo(JsonNode blocks, double pageWidth, double pageHeight) {
            this.blocks = blocks;
            this.pageWidth = pageWidth;
            this.pageHeight = pageHeight;
        }
    }
    
    /**
     * 从 middle_json 中查找列表块
     * 
     * @param middleJsonNode middle_json 数据
     * @param pageIdx 页面索引
     * @param contentItem content_list 中的列表项
     * @return 列表块信息（包含子块数组和页面尺寸）
     */
    private ListBlockInfo findListBlockInMiddleJson(JsonNode middleJsonNode, int pageIdx, JsonNode contentItem) {
        log.debug("📋 [列表匹配] 开始在 middle_json 中查找列表，页{}", pageIdx + 1);
        
        if (middleJsonNode == null || !middleJsonNode.isArray()) {
            log.warn("📋 [列表匹配] ⚠️  middle_json 为空或不是数组");
            return null;
        }
        
        if (pageIdx < 0 || pageIdx >= middleJsonNode.size()) {
            log.warn("📋 [列表匹配] ⚠️  页面索引 {} 超出范围 (总页数: {})", pageIdx, middleJsonNode.size());
            return null;
        }
        
        JsonNode listItemsNode = contentItem.get("list_items");
        if (listItemsNode == null || !listItemsNode.isArray() || listItemsNode.size() == 0) {
            log.warn("📋 [列表匹配] ⚠️  content_list 中没有 list_items");
            return null;
        }
        
        // 获取content_list中的bbox（归一化坐标 1000x1000）
        JsonNode contentBboxNode = contentItem.get("bbox");
        if (contentBboxNode == null || !contentBboxNode.isArray() || contentBboxNode.size() < 4) {
            log.warn("📋 [列表匹配] ⚠️  content_list 中没有有效的 bbox");
            return null;
        }
        double[] contentBbox = new double[]{
            contentBboxNode.get(0).asDouble(),
            contentBboxNode.get(1).asDouble(),
            contentBboxNode.get(2).asDouble(),
            contentBboxNode.get(3).asDouble()
        };
        log.debug("📋 [列表匹配] content_list bbox (归一化1000x1000): [{}, {}, {}, {}]", 
            contentBbox[0], contentBbox[1], contentBbox[2], contentBbox[3]);
        
        try {
            JsonNode pageNode = middleJsonNode.get(pageIdx);
            
            if (pageNode == null || !pageNode.isObject()) {
                log.warn("📋 [列表匹配] ⚠️  页{} 的数据为空或格式错误", pageIdx + 1);
                return null;
            }
            
            // 获取页面尺寸
            double middleJsonPageWidth = 0;
            double middleJsonPageHeight = 0;
            
            if (pageNode.has("page_info") && pageNode.get("page_info").isObject()) {
                JsonNode pageInfo = pageNode.get("page_info");
                if (pageInfo.has("page_size") && pageInfo.get("page_size").isArray()) {
                    JsonNode pageSize = pageInfo.get("page_size");
                    if (pageSize.size() >= 2) {
                        middleJsonPageWidth = pageSize.get(0).asDouble();
                        middleJsonPageHeight = pageSize.get(1).asDouble();
                        log.debug("📋 [列表匹配] middle_json 页面尺寸: {}x{}", middleJsonPageWidth, middleJsonPageHeight);
                    }
                }
            } else if (pageNode.has("page_size") && pageNode.get("page_size").isArray()) {
                // 尝试直接从 pageNode 获取 page_size
                JsonNode pageSize = pageNode.get("page_size");
                if (pageSize.size() >= 2) {
                    middleJsonPageWidth = pageSize.get(0).asDouble();
                    middleJsonPageHeight = pageSize.get(1).asDouble();
                    log.debug("📋 [列表匹配] middle_json 页面尺寸（直接获取）: {}x{}", middleJsonPageWidth, middleJsonPageHeight);
                }
            }
            
            // 验证页面尺寸
            if (middleJsonPageWidth <= 0 || middleJsonPageHeight <= 0) {
                log.warn("📋 [列表匹配] ⚠️  页{} 的 page_size 无效或缺失 ({}x{})，无法进行坐标转换", 
                    pageIdx + 1, middleJsonPageWidth, middleJsonPageHeight);
                return null;
            }
            
            // 获取 para_blocks
            JsonNode paraBlocks = pageNode.get("para_blocks");
            
            log.debug("📋 [列表匹配] 页{} para_blocks 块数量: {}", 
                pageIdx + 1, 
                paraBlocks != null ? paraBlocks.size() : 0);
            
            if (paraBlocks != null && paraBlocks.isArray()) {
                // 【修复】遍历页面中的所有块，查找 list 类型，通过bbox坐标匹配
                ListBlockInfo firstListBlock = null; // 记录第一个找到的list块作为fallback
                ListBlockInfo bestMatchBlock = null;
                double bestMatchScore = Double.MAX_VALUE; // 最小距离得分（越小越好）
                
                for (int i = 0; i < paraBlocks.size(); i++) {
                    JsonNode block = paraBlocks.get(i);
                    String blockType = block.has("type") ? block.get("type").asText() : "";
                    
                    if ("list".equals(blockType)) {
                        JsonNode subBlocks = block.get("blocks");
                        
                        if (subBlocks != null && subBlocks.isArray()) {
                            int actualBlockCount = subBlocks.size();
                            
                            // 记录第一个找到的list块
                            if (firstListBlock == null) {
                                firstListBlock = new ListBlockInfo(subBlocks, middleJsonPageWidth, middleJsonPageHeight);
                            }
                            
                            // 获取middle_json中的bbox（页面坐标系）
                            JsonNode middleBboxNode = block.get("bbox");
                            if (middleBboxNode == null || !middleBboxNode.isArray() || middleBboxNode.size() < 4) {
                                log.debug("📋 [列表匹配] 列表块 {} 没有有效bbox，跳过匹配", i);
                                continue;
                            }
                            
                            // middle_json的bbox是页面坐标系，需要归一化到1000x1000
                            double[] middleBbox = new double[]{
                                middleBboxNode.get(0).asDouble() * 1000.0 / middleJsonPageWidth,
                                middleBboxNode.get(1).asDouble() * 1000.0 / middleJsonPageHeight,
                                middleBboxNode.get(2).asDouble() * 1000.0 / middleJsonPageWidth,
                                middleBboxNode.get(3).asDouble() * 1000.0 / middleJsonPageHeight
                            };
                            
                            log.info("📋 [列表匹配] 页{}, 块索引: {}, 列表项数量: {}", 
                                pageIdx + 1, i, actualBlockCount);
                            log.info("📋 [列表匹配]   content_list bbox: [{}, {}, {}, {}]", 
                                contentBbox[0], contentBbox[1], contentBbox[2], contentBbox[3]);
                            log.info("📋 [列表匹配]   middle_json  bbox: [{}, {}, {}, {}] (归一化后)", 
                                middleBbox[0], middleBbox[1], middleBbox[2], middleBbox[3]);
                            
                            // 【关键】计算bbox的匹配度（使用曼哈顿距离）
                            double matchScore = Math.abs(contentBbox[0] - middleBbox[0]) +
                                              Math.abs(contentBbox[1] - middleBbox[1]) +
                                              Math.abs(contentBbox[2] - middleBbox[2]) +
                                              Math.abs(contentBbox[3] - middleBbox[3]);
                            
                            log.info("📋 [列表匹配]   匹配得分 (距离): {}", matchScore);
                            
                            // 更新最佳匹配
                            if (matchScore < bestMatchScore) {
                                bestMatchScore = matchScore;
                                bestMatchBlock = new ListBlockInfo(subBlocks, middleJsonPageWidth, middleJsonPageHeight);
                                log.info("📋 [列表匹配]   ✅ 更新最佳匹配！");
                            }
                        }
                    }
                }
                
                // 如果找到了bbox匹配的块，并且匹配得分在合理范围内（阈值可以调整）
                if (bestMatchBlock != null && bestMatchScore < 100) { // 阈值：归一化坐标系下的距离和 < 100
                    log.info("📋 [列表匹配] ✅ 找到bbox匹配的列表块！匹配得分: {}", bestMatchScore);
                    return bestMatchBlock;
                }
                
                // 如果没有找到bbox匹配的，返回第一个找到的list块（兼容旧逻辑）
                if (firstListBlock != null) {
                    log.warn("📋 [列表匹配] ⚠️  未找到bbox匹配的列表块（最佳得分: {}），使用第一个找到的列表块", bestMatchScore);
                    return firstListBlock;
                }
                
                log.warn("📋 [列表匹配] ⚠️  未找到列表块，页{}", pageIdx + 1);
            }
            
        } catch (Exception e) {
            log.error("📋 [列表匹配] 查找列表块时发生错误: ", e);
        }
        
        return null;
    }
    
    /**
     * 从 middle_json 中根据 bbox 查找对应的文本块并提取文本
     */
    private String extractTextFromMiddleJsonByBbox(JsonNode middleJsonNode, int pageIdx, double[] targetBbox) {
        try {
            JsonNode pdfInfo = middleJsonNode.get("pdf_info");
            if (pdfInfo == null || !pdfInfo.isArray() || pageIdx >= pdfInfo.size()) {
                return null;
            }
            
            JsonNode pageNode = pdfInfo.get(pageIdx);
            JsonNode paraBlocks = pageNode.get("preproc_blocks");
            if (paraBlocks == null || !paraBlocks.isArray()) {
                return null;
            }
            
            // 遍历所有文本块，找到 bbox 匹配的块
            for (JsonNode block : paraBlocks) {
                String blockType = block.has("type") ? block.get("type").asText() : "";
                
                // 处理 text 类型的块
                if ("text".equals(blockType)) {
                    JsonNode bboxNode = block.get("bbox");
                    if (bboxNode != null && bboxNode.isArray() && bboxNode.size() == 4) {
                        double[] blockBbox = new double[]{
                            bboxNode.get(0).asDouble(),
                            bboxNode.get(1).asDouble(),
                            bboxNode.get(2).asDouble(),
                            bboxNode.get(3).asDouble()
                        };
                        
                        // 检查 bbox 是否匹配（允许小误差）
                        if (isBboxMatching(targetBbox, blockBbox, 2.0)) {
                            log.debug("📝 [文本匹配] 在 middle_json 中找到匹配的文本块，bbox: [{}, {}, {}, {}]", 
                                blockBbox[0], blockBbox[1], blockBbox[2], blockBbox[3]);
                            return extractTextFromMiddleJsonBlock(block);
                        }
                    }
                }
                // 处理 list 类型的块
                else if ("list".equals(blockType)) {
                    JsonNode bboxNode = block.get("bbox");
                    if (bboxNode != null && bboxNode.isArray() && bboxNode.size() == 4) {
                        double[] blockBbox = new double[]{
                            bboxNode.get(0).asDouble(),
                            bboxNode.get(1).asDouble(),
                            bboxNode.get(2).asDouble(),
                            bboxNode.get(3).asDouble()
                        };
                        
                        // 检查 bbox 是否匹配（允许小误差）
                        if (isBboxMatching(targetBbox, blockBbox, 2.0)) {
                            log.debug("📝 [文本匹配] 在 middle_json 中找到匹配的列表块，bbox: [{}, {}, {}, {}]", 
                                blockBbox[0], blockBbox[1], blockBbox[2], blockBbox[3]);
                            return extractTextFromListBlock(block);
                        }
                    }
                }
            }
            
            log.debug("📝 [文本匹配] 未在 middle_json 中找到匹配的文本块，bbox: [{}, {}, {}, {}]", 
                targetBbox[0], targetBbox[1], targetBbox[2], targetBbox[3]);
        } catch (Exception e) {
            log.warn("从 middle_json 提取文本失败: {}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 检查两个 bbox 是否匹配（允许小误差）
     */
    private boolean isBboxMatching(double[] bbox1, double[] bbox2, double tolerance) {
        for (int i = 0; i < 4; i++) {
            if (Math.abs(bbox1[i] - bbox2[i]) > tolerance) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 从 middle_json 的块中提取文本内容
     */
    private String extractTextFromMiddleJsonBlock(JsonNode block) {
        StringBuilder text = new StringBuilder();
        
        try {
            String blockType = block.has("type") ? block.get("type").asText() : "unknown";
            JsonNode lines = block.get("lines");
            
            if (lines != null && lines.isArray()) {
                log.debug("📝 [extractTextFromMiddleJsonBlock] 块类型: {}, lines数量: {}", blockType, lines.size());
                for (JsonNode line : lines) {
                    JsonNode spans = line.get("spans");
                    if (spans != null && spans.isArray()) {
                        for (JsonNode span : spans) {
                            String content = span.has("content") ? span.get("content").asText() : "";
                            if (!content.isEmpty()) {
                                // 检查是否是公式类型的span
                                String spanType = span.has("type") ? span.get("type").asText() : "";
                                
                                if ("inline_equation".equals(spanType) || "interline_equation".equals(spanType)) {
                                    // 这是一个公式，需要添加 $ 符号
                                    if ("interline_equation".equals(spanType)) {
                                        text.append("$$").append(content).append("$$");
                                        log.debug("📝 [extractTextFromMiddleJsonBlock] 提取到行间公式: $${}", content);
                                    } else {
                                        text.append("$").append(content).append("$");
                                        log.debug("📝 [extractTextFromMiddleJsonBlock] 提取到行内公式: ${}", content);
                                    }
                                } else {
                                    text.append(content);
                                    log.debug("📝 [extractTextFromMiddleJsonBlock] 提取到文本: {}", content);
                                }
                            }
                        }
                    } else {
                        log.debug("📝 [extractTextFromMiddleJsonBlock] line没有spans或不是数组");
                    }
                }
            } else {
                log.debug("📝 [extractTextFromMiddleJsonBlock] 块{}没有lines或不是数组", blockType);
            }
        } catch (Exception e) {
            log.warn("提取块文本失败: {}", e.getMessage(), e);
        }
        
        String result = text.toString().trim();
        log.debug("📝 [extractTextFromMiddleJsonBlock] 最终提取的文本长度: {}, 内容: {}", 
            result.length(), result.length() > 100 ? result.substring(0, 100) + "..." : result);
        return result;
    }
    
    /**
     * 从 middle_json 的 list 块中提取文本内容
     * list 块包含 blocks 数组，每个 block 包含 lines -> spans 结构
     */
    private String extractTextFromListBlock(JsonNode listBlock) {
        StringBuilder text = new StringBuilder();
        
        try {
            JsonNode blocks = listBlock.get("blocks");
            
            if (blocks != null && blocks.isArray()) {
                log.debug("📝 [extractTextFromListBlock] list块包含 {} 个子块", blocks.size());
                
                for (JsonNode block : blocks) {
                    JsonNode lines = block.get("lines");
                    
                    if (lines != null && lines.isArray()) {
                        for (JsonNode line : lines) {
                            JsonNode spans = line.get("spans");
                            if (spans != null && spans.isArray()) {
                                for (JsonNode span : spans) {
                                    String content = span.has("content") ? span.get("content").asText() : "";
                                    if (!content.isEmpty()) {
                                        // 检查是否是公式类型的span
                                        String spanType = span.has("type") ? span.get("type").asText() : "";
                                        
                                        if ("inline_equation".equals(spanType)) {
                                            // 这是一个行内公式，需要添加 $ 符号
                                            text.append("$").append(content).append("$");
                                            log.debug("📝 [extractTextFromListBlock] 提取到行内公式: ${}", content);
                                        } else if ("interline_equation".equals(spanType)) {
                                            // 这是一个行间公式，需要添加 $$ 符号
                                            text.append("$$").append(content).append("$$");
                                            log.debug("📝 [extractTextFromListBlock] 提取到行间公式: $${}", content);
                                        } else {
                                            text.append(content);
                                            log.debug("📝 [extractTextFromListBlock] 提取到文本: {}", content);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                log.debug("📝 [extractTextFromListBlock] list块没有blocks或不是数组");
            }
        } catch (Exception e) {
            log.warn("提取list块文本失败: {}", e.getMessage(), e);
        }
        
        String result = text.toString().trim();
        log.debug("📝 [extractTextFromListBlock] 最终提取的文本长度: {}, 内容: {}", 
            result.length(), result.length() > 100 ? result.substring(0, 100) + "..." : result);
        return result;
    }
    
    /**
     * 表格子项包装类（用于排序）
     */
    private static class TableSubItem {
        String type;         // table_caption, table_body, table_footnote
        double[] bbox;       // 图片坐标
        String text;         // 文本内容
        int index;           // MinerU的index字段（阅读顺序）
        String htmlContent;  // HTML内容（仅table_body有）
        
        public TableSubItem(String type, double[] bbox, String text, int index) {
            this.type = type;
            this.bbox = bbox;
            this.text = text;
            this.index = index;
            this.htmlContent = null;
        }
        
        public TableSubItem(String type, double[] bbox, String text, int index, String htmlContent) {
            this.type = type;
            this.bbox = bbox;
            this.text = text;
            this.index = index;
            this.htmlContent = htmlContent;
        }
    }
}

