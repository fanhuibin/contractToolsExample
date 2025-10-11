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
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhaoxinms.contract.tools.comparePRO.config.ZxOcrConfig;
import com.zhaoxinms.contract.tools.comparePRO.model.CompareOptions;
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
     * 识别PDF并返回dots.ocr兼容的格式
     * 
     * @param pdfFile PDF文件
     * @param taskId 任务ID
     * @param outputDir 输出目录
     * @param docMode 文档模式（old/new）
     * @param options 比对选项（包含页眉页脚设置）
     * @return PageLayout数组（与dots.ocr格式完全一致）
     */
    public TextExtractionUtil.PageLayout[] recognizePdf(
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
        
        // 转换为dots.ocr兼容的PageLayout格式
        TextExtractionUtil.PageLayout[] layouts = convertToPageLayouts(apiResult, pageImages, pdfFile, options);
        
        long endTime = System.currentTimeMillis();
        log.info("MinerU OCR识别完成，共{}页，耗时{}ms", layouts.length, endTime - startTime);
        
        return layouts;
    }
    
    /**
     * 调用MinerU API进行识别
     */
    private String callMinerUAPI(File pdfFile) throws Exception {
        String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
        
        ZxOcrConfig.MinerUConfig mineruConfig = zxOcrConfig.getMineru();
        URL url = new URL(mineruConfig.getApiUrl() + "/file_parse");
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
            writer.append(mineruConfig.getBackend()).append("\r\n");
            
            // 如果使用vlm-http-client，添加server_url
            if ("vlm-http-client".equals(mineruConfig.getBackend())) {
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"server_url\"\r\n\r\n");
                writer.append(mineruConfig.getVllmServerUrl()).append("\r\n");
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
        
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFRenderer renderer = new PDFRenderer(document);
            int pageCount = document.getNumberOfPages();
            
            log.info("开始生成{}个页面图片，DPI: {}", pageCount, renderDpi);
            
            int cachedCount = 0;
            int renderedCount = 0;
            
            // 获取图片格式配置（PNG 或 JPEG）
            String imageFormat = zxOcrConfig.getImageFormat() != null ? 
                zxOcrConfig.getImageFormat().toUpperCase() : "PNG";
            float jpegQuality = zxOcrConfig.getJpegQuality();
            String imageExt = imageFormat.equalsIgnoreCase("JPEG") ? ".jpg" : ".png";
            
            log.info("图片格式: {}, JPEG质量: {}", imageFormat, jpegQuality);
            
            // PDFRenderer 不是线程安全的，必须串行处理
            // 【内存优化】逐页处理并立即释放内存
            for (int i = 0; i < pageCount; i++) {
                File imageFile = new File(imagesDir, "page-" + (i + 1) + imageExt);
                BufferedImage image = null;
                int imageWidth = 0;
                int imageHeight = 0;
                
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
                                image = renderer.renderImageWithDPI(i, renderDpi, ImageType.RGB);
                                imageWidth = image.getWidth();
                                imageHeight = image.getHeight();
                                saveImage(image, imageFile, imageFormat, jpegQuality);
                                log.debug("重新生成页面图片: {}, 尺寸: {}x{}, 大小: {}KB", 
                                    imageFile.getName(), imageWidth, imageHeight,
                                    imageFile.length() / 1024);
                                renderedCount++;
                            }
                        } catch (IOException e) {
                            // 读取失败，重新生成
                            log.warn("读取已有图片失败，重新生成: {}, 原因: {}", 
                                imageFile.getName(), e.getMessage());
                            image = renderer.renderImageWithDPI(i, renderDpi, ImageType.RGB);
                            imageWidth = image.getWidth();
                            imageHeight = image.getHeight();
                            saveImage(image, imageFile, imageFormat, jpegQuality);
                            log.debug("重新生成页面图片: {}, 尺寸: {}x{}, 大小: {}KB", 
                                imageFile.getName(), imageWidth, imageHeight,
                                imageFile.length() / 1024);
                            renderedCount++;
                        }
                    } else {
                        // 生成新图片
                        image = renderer.renderImageWithDPI(i, renderDpi, ImageType.RGB);
                        imageWidth = image.getWidth();
                        imageHeight = image.getHeight();
                        saveImage(image, imageFile, imageFormat, jpegQuality);
                        log.debug("生成页面图片: {}, 尺寸: {}x{}, 大小: {}KB", 
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
     */
    private TextExtractionUtil.PageLayout[] convertToPageLayouts(
            String apiResult,
            List<Map<String, Object>> pageImages,
            File pdfFile,
            CompareOptions options) throws Exception {
        
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
        
        for (JsonNode item : contentListNode) {
            int pageIdx = item.has("page_idx") ? item.get("page_idx").asInt() : 0;
            
            // 过滤页眉页脚
            if (options.isIgnoreHeaderFooter() && isHeaderFooterOrPageNumber(item)) {
                String itemType = item.has("type") ? item.get("type").asText() : "unknown";
                log.debug("🚫 过滤 MinerU 识别的页眉页脚 - 第{}页, 类型:{}", pageIdx + 1, itemType);
                continue;
            }
            
            // 转换为LayoutItem
            List<TextExtractionUtil.LayoutItem> items = convertToLayoutItems(
                item,
                pageImageMap.get(pageIdx),
                pdfPageSizes.get(pageIdx),
                middleJsonNode,
                pageIdx
            );
            
            if (!pageLayoutItems.containsKey(pageIdx)) {
                pageLayoutItems.put(pageIdx, new ArrayList<>());
            }
            pageLayoutItems.get(pageIdx).addAll(items);
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
     * 转换MinerU的item为LayoutItem列表
     * 处理所有类型：普通文本、列表、表格、图片、代码等
     */
    private List<TextExtractionUtil.LayoutItem> convertToLayoutItems(
            JsonNode item,
            Map<String, Object> pageImage,
            double[] pdfPageSize,
            JsonNode middleJsonNode,
            int pageIdx) {
        
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
            items.addAll(handleTableItem(item, imageWidth, imageHeight, pdfWidth, pdfHeight, middleJsonNode, pageIdx));
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
            items.addAll(handleListItem(item, imageWidth, imageHeight, pdfWidth, pdfHeight));
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
            items.addAll(handleTextItem(item, imageWidth, imageHeight, pdfWidth, pdfHeight));
        }
        
        return items;
    }
    
    /**
     * 处理表格类型的内容
     * 包括 table_caption, table_body, table_footnote
     * 从 middle_json 中获取各部分的精确 bbox
     */
    private List<TextExtractionUtil.LayoutItem> handleTableItem(
            JsonNode item,
            int imageWidth, int imageHeight,
            double pdfWidth, double pdfHeight,
            JsonNode middleJsonNode,
            int pageIdx) {
        
        List<TextExtractionUtil.LayoutItem> items = new ArrayList<>();
        
        log.info("📊 [表格处理] ========== 开始处理表格，页{} ==========", pageIdx + 1);
        log.info("📊 [表格处理] 图片尺寸: {}x{}, PDF尺寸: {}x{}", imageWidth, imageHeight, pdfWidth, pdfHeight);
        
        // 从 middle_json 中查找对应页面的表格块
        TableBlockInfo tableBlockInfo = findTableBlocksInMiddleJson(middleJsonNode, pageIdx, item);
        
        if (tableBlockInfo != null && tableBlockInfo.blocks != null && 
            tableBlockInfo.blocks.isArray() && tableBlockInfo.blocks.size() > 0) {
            // 使用 middle_json 中的精确 bbox 处理 table_caption 和 table_footnote
            log.info("📊 [表格处理] ✅ 从 middle_json 中找到表格精确 bbox，页{}, 子块数量: {}, middle_json页面尺寸: {}x{}", 
                pageIdx + 1, tableBlockInfo.blocks.size(), tableBlockInfo.pageWidth, tableBlockInfo.pageHeight);
            
            // 只处理 table_caption 和 table_footnote，table_body 使用 content_list 的逻辑
            for (int i = 0; i < tableBlockInfo.blocks.size(); i++) {
                JsonNode block = tableBlockInfo.blocks.get(i);
                String blockType = block.has("type") ? block.get("type").asText() : "";
                
                // 跳过 table_body，它将在后面用 content_list 逻辑处理
                if ("table_body".equals(blockType)) {
                    log.info("📊 [表格处理] 跳过 table_body（将使用 content_list 逻辑处理）");
                    continue;
                }
                
                JsonNode bboxNode = block.get("bbox");
                
                log.info("📊 [表格处理] 处理子块 {}/{}: type={}", i + 1, tableBlockInfo.blocks.size(), blockType);
                
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
                    // table_caption 和 table_footnote 都设置为 Text 类型
                    items.add(new TextExtractionUtil.LayoutItem(imageBbox, "Text", text));
                    log.info("📊 [表格处理] ✅ 添加表格子块: type={}, category=Text, bbox=[{}, {}, {}, {}], 文本长度={}, 文本预览: {}", 
                        blockType, imageBbox[0], imageBbox[1], imageBbox[2], imageBbox[3], text.length(),
                        text.length() > 50 ? text.substring(0, 50) + "..." : text);
                } else {
                    log.warn("📊 [表格处理] ⚠️  子块 {} 文本为空，跳过", blockType);
                }
            }
            log.info("📊 [表格处理] 从 middle_json 共添加 {} 个子块（caption/footnote）", items.size());
            
            // 现在处理 table_body（使用 content_list 的逻辑）
            log.info("📊 [表格处理] 开始处理 table_body（使用 content_list）");
            JsonNode bboxNode = item.get("bbox");
            
            if (bboxNode != null && bboxNode.isArray() && bboxNode.size() >= 4) {
                double[] mineruBbox = extractBbox(bboxNode, pdfWidth, pdfHeight);
                double[] imageBbox = convertAndValidateBbox(mineruBbox, pdfWidth, pdfHeight, imageWidth, imageHeight);
                
                // 处理 table_body（HTML 表格）
                if (item.has("table_body")) {
                    String tableBodyHtml = item.get("table_body").asText();
                    if (tableBodyHtml != null && !tableBodyHtml.trim().isEmpty()) {
                        String readableTableBody = convertLatexToReadableText(tableBodyHtml);
                        items.add(new TextExtractionUtil.LayoutItem(imageBbox, "Table", readableTableBody + "\n"));
                        log.info("📊 [表格处理] ✅ 添加 table_body: bbox=[{}, {}, {}, {}], HTML长度={}", 
                            imageBbox[0], imageBbox[1], imageBbox[2], imageBbox[3], tableBodyHtml.length());
                    }
                }
            } else {
                log.warn("📊 [表格处理] ⚠️  content_list 中缺少 bbox 信息，无法处理 table_body");
            }
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
            
            // 2. 处理 table_body (HTML格式需要去除标签)
            if (item.has("table_body")) {
                String tableBody = item.get("table_body").asText();
                log.debug("表格原始HTML长度: {}", tableBody.length());
                // 去除HTML标签，转换为纯文本
                String cleanText = removeHtmlTags(tableBody);
                // 转换 LaTeX 格式为可读文本
                cleanText = convertLatexToReadableText(cleanText);
                log.info("📝 表格去除HTML后文本长度: {}, 预览: {}", 
                    cleanText.length(), 
                    cleanText.length() > 100 ? cleanText.substring(0, 100) + "..." : cleanText);
                if (!cleanText.trim().isEmpty()) {
                    items.add(new TextExtractionUtil.LayoutItem(imageBbox, "Table", cleanText));
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
                            items.add(new TextExtractionUtil.LayoutItem(footnoteBbox, "text", readableFootnoteText + "\n"));
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
     */
    private List<TextExtractionUtil.LayoutItem> handleListItem(
            JsonNode item,
            int imageWidth, int imageHeight,
            double pdfWidth, double pdfHeight) {
        
        List<TextExtractionUtil.LayoutItem> items = new ArrayList<>();
        JsonNode listItemsNode = item.get("list_items");
        
        if (listItemsNode == null || !listItemsNode.isArray()) {
            return items;
        }
        
        JsonNode bboxNode = item.get("bbox");
        if (bboxNode == null || !bboxNode.isArray() || bboxNode.size() < 4) {
            return items;
        }
        
        double[] mineruBbox = extractBbox(bboxNode, pdfWidth, pdfHeight);
        double[] imageBbox = convertAndValidateBbox(mineruBbox, pdfWidth, pdfHeight, imageWidth, imageHeight);
        
        // 计算每个列表项的高度
        double totalHeight = imageBbox[3] - imageBbox[1];
        double itemHeight = totalHeight / listItemsNode.size();
        
        // 为每个列表项创建LayoutItem
        for (int i = 0; i < listItemsNode.size(); i++) {
            String itemText = listItemsNode.get(i).asText();
            
            // 计算列表项bbox
            double[] itemBbox = new double[]{
                imageBbox[0],
                imageBbox[1] + i * itemHeight,
                imageBbox[2],
                imageBbox[1] + (i + 1) * itemHeight
            };
            
            // 转换 LaTeX 格式为可读文本
            String readableItemText = convertLatexToReadableText(itemText);
            
            items.add(new TextExtractionUtil.LayoutItem(itemBbox, "Text", readableItemText + "\n"));
        }
        
        return items;
    }
    
    /**
     * 处理普通文本类型的内容
     */
    private List<TextExtractionUtil.LayoutItem> handleTextItem(
            JsonNode item,
            int imageWidth, int imageHeight,
            double pdfWidth, double pdfHeight) {
        
        List<TextExtractionUtil.LayoutItem> items = new ArrayList<>();
        String text = item.get("text").asText();
        JsonNode bboxNode = item.get("bbox");
        
        if (bboxNode == null || !bboxNode.isArray() || bboxNode.size() < 4) {
            return items;
        }
        
        double[] mineruBbox = extractBbox(bboxNode, pdfWidth, pdfHeight);
        double[] imageBbox = convertAndValidateBbox(mineruBbox, pdfWidth, pdfHeight, imageWidth, imageHeight);
        
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
            log.warn("保存 MinerU 原始响应失败: {}", e.getMessage());
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
            }
        } catch (Exception e) {
            log.warn("保存格式化 content_list 失败: {}", e.getMessage());
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
            
            log.debug("未找到 middle_json 数据（MinerU API 可能未返回此字段）");
            
        } catch (Exception e) {
            log.warn("保存 middle_json 失败: {}", e.getMessage());
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
            log.warn("保存易读格式 content_list 失败: {}", e.getMessage());
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
            
            log.info("保存 content_list 统计信息: {}", statsFile.getAbsolutePath());
        } catch (Exception e) {
            log.warn("保存统计信息失败: {}", e.getMessage());
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
        
        StringBuilder result = new StringBuilder();
        int i = 0;
        
        while (i < text.length()) {
            // 检查是否是行间公式 $$...$$
            if (i < text.length() - 1 && text.charAt(i) == '$' && text.charAt(i + 1) == '$') {
                int endPos = text.indexOf("$$", i + 2);
                if (endPos != -1) {
                    // 提取公式内容（不包括 $$ 符号）
                    String formula = text.substring(i + 2, endPos);
                    // 转换公式内容
                    String converted = convertLatexFormula(formula);
                    result.append(converted);
                    i = endPos + 2;  // 跳过结束的 $$
                    continue;
                }
            }
            
            // 检查是否是行内公式 $...$
            if (text.charAt(i) == '$') {
                int endPos = text.indexOf('$', i + 1);
                if (endPos != -1) {
                    // 提取公式内容（不包括 $ 符号）
                    String formula = text.substring(i + 1, endPos);
                    // 转换公式内容
                    String converted = convertLatexFormula(formula);
                    result.append(converted);
                    i = endPos + 1;  // 跳过结束的 $
                    continue;
                }
            }
            
            // 不是公式，直接添加字符
            result.append(text.charAt(i));
            i++;
        }
        
        String finalResult = result.toString();
        
        // 处理公式外的一些通用格式（如连续反斜杠、千分号等）
        finalResult = finalResult.replace("\\text‰", "‰");
        finalResult = finalResult.replaceAll("\\\\{4,}", "");  // 清理连续的多个反斜杠（4个或以上）
        finalResult = finalResult.replaceAll("\\s+", " ");
        finalResult = finalResult.trim();
        
        return finalResult;
    }
    
    /**
     * 保存图片（支持 PNG 和 JPEG 格式）
     * 
     * @param image 图片对象
     * @param imageFile 输出文件
     * @param format 图片格式（PNG 或 JPEG）
     * @param jpegQuality JPEG 质量（0.0-1.0）
     */
    private void saveImage(BufferedImage image, File imageFile, String format, float jpegQuality) throws IOException {
        if ("JPEG".equalsIgnoreCase(format) || "JPG".equalsIgnoreCase(format)) {
            // 保存为 JPEG 格式，使用指定质量
            ImageWriter writer = ImageIO.getImageWritersByFormatName("JPEG").next();
            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            writeParam.setCompressionQuality(jpegQuality);
            
            try (ImageOutputStream outputStream = ImageIO.createImageOutputStream(imageFile)) {
                writer.setOutput(outputStream);
                writer.write(null, new javax.imageio.IIOImage(image, null, null), writeParam);
            } finally {
                writer.dispose();
            }
        } else {
            // 保存为 PNG 格式（无损）
            ImageIO.write(image, "PNG", imageFile);
        }
    }
    
    /**
     * 转换 LaTeX 公式内容（不包括 $ 符号）
     * 
     * @param formula 公式内容
     * @return 转换后的可读文本
     */
    private String convertLatexFormula(String formula) {
        String result = formula;
        
        // 0. 先处理双反斜杠的特殊情况（在公式内常见）
        // \\% -> \% -> %
        // \\sim -> \sim -> ~
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
        
        // 5. 处理求和、积分等符号
        result = result.replaceAll("\\\\sum\\b", "∑");
        result = result.replaceAll("\\\\int\\b", "∫");
        result = result.replaceAll("\\\\prod\\b", "∏");
        result = result.replaceAll("\\\\lim\\b", "lim");
        
        // 6. 处理平方根
        result = result.replaceAll("\\\\sqrt\\{([^}]+)\\}", "√($1)");
        result = result.replaceAll("\\\\sqrt\\[([^]]+)\\]\\{([^}]+)\\}", "$1√($2)");
        
        // 7. 处理箭头
        result = result.replaceAll("\\\\rightarrow\\b", "→");
        result = result.replaceAll("\\\\leftarrow\\b", "←");
        result = result.replaceAll("\\\\Rightarrow\\b", "⇒");
        result = result.replaceAll("\\\\Leftarrow\\b", "⇐");
        result = result.replaceAll("\\\\leftrightarrow\\b", "↔");
        result = result.replaceAll("\\\\Leftrightarrow\\b", "⇔");
        
        // 8. 处理下标 _{...} 和 ^{...}
        result = result.replaceAll("_\\{([^}]+)\\}", "$1");
        result = result.replaceAll("\\^\\{([^}]+)\\}", "$1");
        result = result.replaceAll("_([a-zA-Z0-9])", "$1");
        result = result.replaceAll("\\^([a-zA-Z0-9])", "$1");
        
        // 9. 处理文本命令
        result = result.replaceAll("\\\\text\\{([^}]+)\\}", "$1");
        result = result.replaceAll("\\\\text([^a-zA-Z])", "$1");
        result = result.replaceAll("\\\\mathbb\\{([^}]+)\\}", "$1");
        result = result.replaceAll("\\\\mathcal\\{([^}]+)\\}", "$1");
        result = result.replaceAll("\\\\mathcalL", "L");
        result = result.replaceAll("\\\\mathrm\\{([^}]+)\\}", "$1");
        result = result.replaceAll("\\\\textbf\\{([^}]+)\\}", "$1");
        result = result.replaceAll("\\\\textit\\{([^}]+)\\}", "$1");
        
        // 10. 处理下划线
        result = result.replaceAll("\\\\underline\\{([^}]+)\\}", "$1");
        result = result.replaceAll("\\\\underline\\s+", "");
        
        // 11. 处理左右括号
        result = result.replaceAll("\\\\left\\(", "(");
        result = result.replaceAll("\\\\right\\)", ")");
        result = result.replaceAll("\\\\left\\[", "[");
        result = result.replaceAll("\\\\right\\]", "]");
        result = result.replaceAll("\\\\left\\{", "{");
        result = result.replaceAll("\\\\right\\}", "}");
        result = result.replaceAll("\\\\left\\|", "|");
        result = result.replaceAll("\\\\right\\|", "|");
        
        // 12. 恢复占位符
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
        
        // 13. 处理特殊符号（单反斜杠的情况）
        result = result.replace("\\%", "%");
        result = result.replace("\\&", "&");
        result = result.replace("\\#", "#");
        result = result.replace("\\_", "_");
        result = result.replace("\\$", "$");
        result = result.replace("\\{", "{");
        result = result.replace("\\}", "}");
        
        // 14. 清理多余的空格
        result = result.replaceAll("\\s+", " ");
        result = result.trim();
        
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
     * @return 表格块信息（包含子块数组和页面尺寸）
     */
    private TableBlockInfo findTableBlocksInMiddleJson(JsonNode middleJsonNode, int pageIdx, JsonNode contentItem) {
        log.info("📊 [表格匹配] 开始在 middle_json 中查找表格，页{}", pageIdx + 1);
        
        if (middleJsonNode == null || !middleJsonNode.isArray()) {
            log.warn("📊 [表格匹配] ⚠️  middle_json 为空或不是数组");
            return null;
        }
        
        log.info("📊 [表格匹配] middle_json 总页数: {}", middleJsonNode.size());
        
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
                    // 遍历页面中的所有块
                    for (int i = 0; i < paraBlocks.size(); i++) {
                        JsonNode block = paraBlocks.get(i);
                        String blockType = block.has("type") ? block.get("type").asText() : "";
                        
                        if ("table".equals(blockType)) {
                            tableCount++;
                            log.info("📊 [表格匹配] 找到第 {} 个表格块（块索引 {}）", tableCount, i);
                            
                            JsonNode subBlocks = block.get("blocks");
                            if (subBlocks != null && subBlocks.isArray()) {
                                log.info("📊 [表格匹配] 表格有 {} 个子块", subBlocks.size());
                                
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
                                            log.info("📊 [表格匹配] ✅ 匹配成功！返回表格子块");
                                            return new TableBlockInfo(subBlocks, middleJsonPageWidth, middleJsonPageHeight);
                                        } else if (middleBodyText != null && middleBodyText.length() < 50 && 
                                                   tableBodyText.contains(middleBodyText)) {
                                            log.info("📊 [表格匹配] ✅ 匹配成功（短文本）！返回表格子块");
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
     * 从 middle_json 的块中提取文本内容
     */
    private String extractTextFromMiddleJsonBlock(JsonNode block) {
        StringBuilder text = new StringBuilder();
        
        try {
            JsonNode lines = block.get("lines");
            if (lines != null && lines.isArray()) {
                for (JsonNode line : lines) {
                    JsonNode spans = line.get("spans");
                    if (spans != null && spans.isArray()) {
                        for (JsonNode span : spans) {
                            String content = span.has("content") ? span.get("content").asText() : "";
                            text.append(content);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("提取块文本失败: {}", e.getMessage());
        }
        
        return text.toString().trim();
    }
}

