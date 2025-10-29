package com.zhaoxinms.contract.tools.comparePRO.service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhaoxinms.contract.tools.comparePRO.model.CompareResult;
import com.zhaoxinms.contract.tools.comparePRO.model.DiffBlock;
import com.zhaoxinms.contract.tools.comparePRO.model.ExportRequest;
import com.zhaoxinms.contract.tools.config.ZxcmConfig;

/**
 * 比对结果导出服务
 * 
 * 职责:
 * - 导出HTML格式报告
 * - 导出DOCX格式报告
 * - 导出多格式报告（ZIP）
 * - JSON数据准备和格式化
 * 
 * 重构说明:
 * 本服务从 CompareService 中分离出来，专门处理比对结果的导出功能。
 * 将导出逻辑独立后，可以：
 * 1. 提高代码可维护性
 * 2. 便于新增导出格式
 * 3. 独立测试导出功能
 * 4. 降低 CompareService 的复杂度
 * 
 * @author AI Assistant
 * @since 2025-10-08
 */
@Service
public class CompareResultExportService {
    
    private static final Logger logger = LoggerFactory.getLogger(CompareResultExportService.class);
    
    @Autowired
    private ZxcmConfig zxcmConfig;
    
    @Autowired
    private CompareImageService imageService;
    
    @Autowired
    private CompareResultFormatter formatter;  // 需要访问图片信息等方法
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 导出比对报告（主入口）
     * 
     * @param result 比对结果
     * @param request 导出请求（包含格式、选项等）
     * @return 导出的文件字节数组
     */
    public byte[] exportReport(CompareResult result, ExportRequest request) throws Exception {
        String taskId = request.getTaskId();
        List<String> formats = request.getFormats();
        
        logger.info("开始导出比对报告: taskId={}, formats={}", taskId, formats);
        
        // 根据格式数量决定返回类型
        if (formats.size() == 1) {
            String format = formats.get(0);
            if ("html".equals(format)) {
                return generateHTMLReport(result, request);
            } else if ("doc".equals(format)) {
                return generateDOCXReport(result, request);
            } else {
                throw new IllegalArgumentException("不支持的导出格式: " + format);
            }
        } else {
            // 多种格式，返回ZIP包含所有格式
            return generateMultiFormatReport(result, request);
        }
    }
    
    /**
     * 生成HTML格式报告（ZIP包）
     * 基于export项目模板的完整实现
     */
    private byte[] generateHTMLReport(CompareResult result, ExportRequest request) throws Exception {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            String taskId = request.getTaskId();
            long startTime = System.currentTimeMillis();
            
            logger.info("🔄 Java后端 - 开始HTML自动化导出流程");
            logger.info("📋 任务信息: ID={}, 原文档={}, 新文档={}", 
                taskId, result.getOldFileName(), result.getNewFileName());
            
            // 1. 获取文件根目录和模板路径
            String uploadRootPath = zxcmConfig.getFileUpload().getRootPath();
            String templatePath = resolveTemplatePath(uploadRootPath);
            String tempDirPath = resolveTempDirPath(uploadRootPath, taskId);
            
            logger.info("📁 路径配置:");
            logger.info("  - 文件根目录: {}", uploadRootPath);
            logger.info("  - HTML模板: {}", templatePath);
            logger.info("  - 临时目录: {}", tempDirPath);
            
            Path tempDir = Paths.get(tempDirPath);
            Files.createDirectories(tempDir);
            
            try {
                // 2. 准备JSON数据
                logger.info("📊 准备JSON数据...");
                String compareResultJson = generateCompareResultJsonForExport(result);
                String taskStatusJson = generateTaskStatusJsonFromCompareResult(result, request, compareResultJson);
                
                // 输出数据统计
                logDataStatistics(result, taskStatusJson, compareResultJson);
                
                // 3. 读取HTML模板文件
                logger.info("📄 读取HTML模板文件: {}", templatePath);
                Path templateFile = Paths.get(templatePath);
                
                if (!Files.exists(templateFile)) {
                    throw new RuntimeException("HTML模板文件不存在: " + templatePath + 
                        "，请确保模板文件存在于: {文件根目录}/templates/export/index.html");
                }
                
                String htmlTemplate = Files.readString(templateFile, StandardCharsets.UTF_8);
                logger.info("✅ 读取HTML模板文件成功 (大小: {} KB)", Files.size(templateFile) / 1024);
                
                // 4. 执行JSON数据内嵌
                logger.info("🔧 执行JSON数据内嵌...");
                String finalHtml = embedJsonDataIntoHtml(htmlTemplate, taskStatusJson, compareResultJson);
                logger.info("✅ JSON数据内嵌完成 (内嵌数据大小: {} KB)", 
                    (taskStatusJson.length() + compareResultJson.length()) / 1024);
                
                // 5. 自动化复制和替换图片文件
                logger.info("🖼️ 自动化处理图片文件...");
                int copiedImages = copyAndReplaceTaskImages(taskId, tempDir);
                logger.info("✅ 图片文件处理完成 (复制了 {} 个图片文件)", copiedImages);
                
                // 6. 创建自动化ZIP包
                logger.info("📦 创建自动化ZIP包...");
                java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(baos);
                
                // 添加内嵌后的HTML文件
                zos.putNextEntry(new java.util.zip.ZipEntry("index.html"));
                zos.write(finalHtml.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
                logger.info("  ✓ 添加HTML文件到ZIP (大小: {} KB)", 
                    finalHtml.getBytes(StandardCharsets.UTF_8).length / 1024);
                
                // 添加JSON文件到ZIP
                zos.putNextEntry(new java.util.zip.ZipEntry("data/current/compare-result.json"));
                zos.write(compareResultJson.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
                logger.info("  ✓ 添加比对结果JSON到ZIP (大小: {} KB)", 
                    compareResultJson.getBytes(StandardCharsets.UTF_8).length / 1024);
                
                zos.putNextEntry(new java.util.zip.ZipEntry("data/current/task-status.json"));
                zos.write(taskStatusJson.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
                logger.info("  ✓ 添加任务状态JSON到ZIP (大小: {} KB)", 
                    taskStatusJson.getBytes(StandardCharsets.UTF_8).length / 1024);
                
                // 添加图片文件到ZIP
                int zipImages = addTempImagesToZip(zos, tempDir);
                logger.info("  ✓ 添加图片文件到ZIP (数量: {})", zipImages);
                
                zos.close();
                
                long duration = System.currentTimeMillis() - startTime;
                logger.info("🎉 Java后端 - HTML自动化导出完成!");
                logger.info("📈 导出统计: 耗时 {}ms, ZIP大小 {} KB", duration, baos.size() / 1024);
                
                return baos.toByteArray();
                
            } finally {
                // 7. 清理临时文件夹
                deleteTempDirectory(tempDir);
                logger.info("🧹 临时文件夹已清理");
            }
        }
    }
    
    /**
     * 生成DOCX格式报告
     */
    private byte[] generateDOCXReport(CompareResult result, ExportRequest request) throws Exception {
        logger.info("📄 开始生成DOCX格式比对报告");
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             org.apache.poi.xwpf.usermodel.XWPFDocument document = new org.apache.poi.xwpf.usermodel.XWPFDocument()) {
            
            // 1. 添加标题
            org.apache.poi.xwpf.usermodel.XWPFParagraph titlePara = document.createParagraph();
            titlePara.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER);
            org.apache.poi.xwpf.usermodel.XWPFRun titleRun = titlePara.createRun();
            titleRun.setText("合同比对报告");
            titleRun.setBold(true);
            titleRun.setFontSize(18);
            titleRun.setFontFamily("宋体");
            
            // 2. 添加基本信息部分
            addBasicInfo(document, result, request);
            
            // 3. 添加差异详细信息标题
            org.apache.poi.xwpf.usermodel.XWPFParagraph detailTitlePara = document.createParagraph();
            org.apache.poi.xwpf.usermodel.XWPFRun detailTitleRun = detailTitlePara.createRun();
            detailTitleRun.setText("差异详细信息");
            detailTitleRun.setBold(true);
            detailTitleRun.setFontSize(14);
            detailTitleRun.setFontFamily("宋体");
            
            // 4. 添加差异详细表格
            addDifferenceTable(document, result, request);
            
            // 5. 写入到字节数组
            document.write(baos);
            logger.info("✅ DOCX报告生成成功，大小: {} KB", baos.size() / 1024);
            
            return baos.toByteArray();
        }
    }
    
    /**
     * 生成多格式报告（ZIP包含HTML和DOCX）
     */
    private byte[] generateMultiFormatReport(CompareResult result, ExportRequest request) throws Exception {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(baos);
            
            // 添加HTML报告
            if (request.getFormats().contains("html")) {
                zos.putNextEntry(new java.util.zip.ZipEntry("report.html.zip"));
                zos.write(generateHTMLReport(result, request));
                zos.closeEntry();
            }
            
            // 添加DOCX报告
            if (request.getFormats().contains("doc")) {
                zos.putNextEntry(new java.util.zip.ZipEntry("report.docx"));
                zos.write(generateDOCXReport(result, request));
                zos.closeEntry();
            }
            
            zos.close();
            
            logger.info("✅ 多格式报告生成完成，大小: {} KB", baos.size() / 1024);
            
            return baos.toByteArray();
        }
    }
    
    // ========== 私有辅助方法 ==========
    
    /**
     * 解析HTML模板文件路径
     */
    private String resolveTemplatePath(String uploadRootPath) {
        Path templatePath = Paths.get(uploadRootPath, "templates", "export", "index.html");
        return templatePath.toAbsolutePath().toString();
    }
    
    /**
     * 解析临时目录路径
     */
    private String resolveTempDirPath(String uploadRootPath, String taskId) {
        Path tempPath = Paths.get(uploadRootPath, "html-export-temp", taskId + "-" + System.currentTimeMillis());
        return tempPath.toAbsolutePath().toString();
    }
    
    /**
     * 输出数据统计信息
     */
    private void logDataStatistics(CompareResult result, String taskStatusJson, String compareResultJson) {
        logger.info("📊 数据统计:");
        logger.info("  - 任务状态: {} vs {}", result.getOldFileName(), result.getNewFileName());
        
        try {
            JsonNode compareData = objectMapper.readTree(compareResultJson);
            int oldPages = compareData.path("oldImageInfo").path("totalPages").asInt(0);
            int newPages = compareData.path("newImageInfo").path("totalPages").asInt(0);
            int differences = compareData.path("differences").size();
            int failedPages = compareData.path("failedPagesCount").asInt(0);
            
            logger.info("  - 页面总数: 原文档 {} 页, 新文档 {} 页", oldPages, newPages);
            logger.info("  - 差异数量: {} 个", differences);
            logger.info("  - 失败页面: {} 个", failedPages);
            logger.info("  - JSON大小: 任务状态 {} KB, 比对结果 {} KB", 
                taskStatusJson.length() / 1024, compareResultJson.length() / 1024);
        } catch (Exception e) {
            logger.warn("解析数据统计时出错: {}", e.getMessage());
        }
    }
    
    /**
     * 将JSON数据内嵌到HTML中
     */
    private String embedJsonDataIntoHtml(String htmlTemplate, String taskStatusJson, String compareResultJson) {
        String inlineScript = String.format(
            "<script>\n" +
            "// 内联数据，避免file://协议的CORS问题\n" +
            "// 由 Java后端自动生成，逻辑等同于 export/embed-json-data.cjs\n" +
            "window.TASK_STATUS_DATA = %s;\n" +
            "window.COMPARE_RESULT_DATA = %s;\n" +
            "console.log('内嵌数据已加载:', { taskStatus: window.TASK_STATUS_DATA, compareResult: window.COMPARE_RESULT_DATA });\n" +
            "</script>",
            taskStatusJson,
            compareResultJson
        );
        
        // 检查是否已经包含内嵌数据
        if (htmlTemplate.contains("window.TASK_STATUS_DATA")) {
            logger.info("⚠️ HTML文件已包含内嵌数据，将替换现有数据");
            htmlTemplate = htmlTemplate.replaceAll("<script>[\\s\\S]*?window\\.TASK_STATUS_DATA[\\s\\S]*?</script>", "");
        }
        
        // 将脚本插入到</head>标签之前
        return htmlTemplate.replace("</head>", inlineScript + "\n</head>");
    }
    
    /**
     * 复制并替换任务图片到临时目录
     */
    private int copyAndReplaceTaskImages(String taskId, Path tempDir) throws Exception {
        String uploadRootPath = zxcmConfig.getFileUpload().getRootPath();
        String yearMonth = com.zhaoxinms.contract.tools.common.util.FileStorageUtils.extractYearMonth(taskId);
        String originalTaskId = com.zhaoxinms.contract.tools.common.util.FileStorageUtils.extractOriginalId(taskId);
        String yearMonthPath = com.zhaoxinms.contract.tools.common.util.FileStorageUtils.getYearMonthPath(yearMonth);
        Path taskPath = Paths.get(uploadRootPath, "compare-pro", yearMonthPath, originalTaskId, "ocr-intermediate");
        
        if (!Files.exists(taskPath)) {
            logger.warn("任务目录不存在: {}", taskPath);
            return 0;
        }
        
        // 创建目标图片目录
        Path oldImagesDir = tempDir.resolve("data/current/images/old");
        Path newImagesDir = tempDir.resolve("data/current/images/new");
        Files.createDirectories(oldImagesDir);
        Files.createDirectories(newImagesDir);
        
        // 复制图片文件
        return imageService.copyTaskImagesFromPath(taskPath, oldImagesDir, newImagesDir);
    }
    
    /**
     * 将临时目录中的图片添加到ZIP
     */
    private int addTempImagesToZip(java.util.zip.ZipOutputStream zos, Path tempDir) throws Exception {
        int count = 0;
        Path dataDir = tempDir.resolve("data");
        
        if (!Files.exists(dataDir)) {
            return 0;
        }
        
        // 递归添加data目录下的所有文件
        count += addDirectoryToZip(zos, dataDir, "data");
        
        return count;
    }
    
    /**
     * 递归添加目录到ZIP
     */
    private int addDirectoryToZip(java.util.zip.ZipOutputStream zos, Path sourceDir, String zipPath) throws Exception {
        int count = 0;
        
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(sourceDir)) {
            for (Path entry : stream) {
                String entryName = zipPath + "/" + entry.getFileName().toString();
                
                if (Files.isDirectory(entry)) {
                    // 递归处理子目录
                    count += addDirectoryToZip(zos, entry, entryName);
                } else {
                    // 添加文件到ZIP
                    zos.putNextEntry(new java.util.zip.ZipEntry(entryName));
                    Files.copy(entry, zos);
                    zos.closeEntry();
                    count++;
                }
            }
        }
        
        return count;
    }
    
    /**
     * 删除临时目录
     */
    private void deleteTempDirectory(Path tempDir) {
        try {
            if (Files.exists(tempDir)) {
                Files.walk(tempDir)
                    .sorted((p1, p2) -> -p1.compareTo(p2))  // 逆序删除（先删文件后删目录）
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (Exception e) {
                            logger.warn("删除临时文件失败: {}", path, e);
                        }
                    });
            }
        } catch (Exception e) {
            logger.warn("清理临时目录失败: {}", tempDir, e);
        }
    }
    
    /**
     * 生成适用于export的比对结果JSON
     */
    private String generateCompareResultJsonForExport(CompareResult result) {
        try {
            Map<String, Object> exportResult = new HashMap<>();
            
            // 基本信息
            exportResult.put("failedPages", result.getFailedPages() != null ? result.getFailedPages() : new ArrayList<>());
            exportResult.put("failedPagesCount", result.getFailedPages() != null ? result.getFailedPages().size() : 0);
            
            // 使用保留的原始格式差异数据
            List<Map<String, Object>> differencesToExport;
            if (result.getFormattedDifferences() != null && !result.getFormattedDifferences().isEmpty()) {
                differencesToExport = result.getFormattedDifferences();
                logger.info("✅ 使用原始格式的差异数据，包含 {} 个差异项", differencesToExport.size());
            } else {
                // 转换 DiffBlock 列表为 Map 格式
                List<DiffBlock> diffBlocks = result.getDifferences();
                if (diffBlocks != null && !diffBlocks.isEmpty()) {
                    differencesToExport = formatter.convertDiffBlocksToMapFormat(diffBlocks, false, null, null);
                    logger.warn("⚠️ 使用转换后的差异数据，已转换为Map格式");
                } else {
                    differencesToExport = new ArrayList<>();
                    logger.warn("⚠️ 无差异数据可导出");
                }
            }
            
            // 统计有效和已忽略的差异
            int ignoredCount = 0;
            int validCount = 0;
            for (Map<String, Object> diff : differencesToExport) {
                Boolean isIgnored = (Boolean) diff.get("ignored");
                if (isIgnored != null && isIgnored) {
                    ignoredCount++;
                } else {
                    validCount++;
                }
            }
            
            // 导出全部差异项（包括被忽略的），让前端根据ignored字段控制显示
            exportResult.put("differences", differencesToExport);
            
            logger.info("✅ 导出包含 {} 个差异项（有效 {} 项，已忽略 {} 项）", 
                differencesToExport.size(), validCount, ignoredCount);
            
            exportResult.put("oldFileName", result.getOldFileName());
            exportResult.put("newFileName", result.getNewFileName());
            exportResult.put("startTime", System.currentTimeMillis());
            
            // 图片信息 - 从实际文件动态获取
            exportResult.put("oldImageInfo", imageService.generateActualImageInfo("old", result.getTaskId()));
            exportResult.put("newImageInfo", imageService.generateActualImageInfo("new", result.getTaskId()));
            
            // 图片基路径供Vue组件使用
            exportResult.put("oldImageBaseUrl", "./data/current/images/old");
            exportResult.put("newImageBaseUrl", "./data/current/images/new");
            
            return objectMapper.writeValueAsString(exportResult);
        } catch (Exception e) {
            throw new RuntimeException("生成比对结果JSON失败", e);
        }
    }
    
    /**
     * 从比对结果生成任务状态JSON
     */
    private String generateTaskStatusJsonFromCompareResult(CompareResult result, ExportRequest request, String compareResultJson) {
        try {
            Map<String, Object> taskStatus = new HashMap<>();
            
            // 基本任务信息
            taskStatus.put("taskId", result.getTaskId());
            taskStatus.put("status", "COMPLETED");  // 必须大写，与前端期望的状态值一致
            taskStatus.put("oldFileName", result.getOldFileName());
            taskStatus.put("newFileName", result.getNewFileName());
            taskStatus.put("totalDiffCount", result.getTotalDiffCount());
            
            // 时间信息（使用当前时间）
            long currentTime = System.currentTimeMillis();
            taskStatus.put("startTime", currentTime);
            taskStatus.put("endTime", currentTime);
            taskStatus.put("totalDuration", 0);
            
            return objectMapper.writeValueAsString(taskStatus);
        } catch (Exception e) {
            throw new RuntimeException("生成任务状态JSON失败", e);
        }
    }
    
    /**
     * 添加基本信息到DOCX文档
     */
    private void addBasicInfo(org.apache.poi.xwpf.usermodel.XWPFDocument document, CompareResult result, ExportRequest request) {
        // 获取差异数据
        List<Map<String, Object>> differences;
        if (result.getFormattedDifferences() != null && !result.getFormattedDifferences().isEmpty()) {
            differences = result.getFormattedDifferences();
        } else {
            List<DiffBlock> diffBlocks = result.getDifferences();
            if (diffBlocks != null && !diffBlocks.isEmpty()) {
                differences = formatter.convertDiffBlocksToMapFormat(diffBlocks, false, null, null);
            } else {
                differences = new ArrayList<>();
            }
        }
        
        // 计算有效差异和已忽略差异
        long validDiffCount = differences.stream()
            .filter(diff -> {
                Boolean ignored = (Boolean) diff.get("ignored");
                return ignored == null || !ignored;
            })
            .count();
        long ignoredDiffCount = differences.size() - validDiffCount;
        
        // 比对编号
        org.apache.poi.xwpf.usermodel.XWPFParagraph p1 = document.createParagraph();
        org.apache.poi.xwpf.usermodel.XWPFRun r1 = p1.createRun();
        r1.setText("比对编号: " + request.getTaskId());
        r1.setFontFamily("宋体");
        r1.setFontSize(12);
        
        // 比对结果
        org.apache.poi.xwpf.usermodel.XWPFParagraph p2 = document.createParagraph();
        org.apache.poi.xwpf.usermodel.XWPFRun r2 = p2.createRun();
        r2.setText("比对结果: " + (validDiffCount > 0 ? "有差异" : "无差异"));
        r2.setFontFamily("宋体");
        r2.setFontSize(12);
        
        // 差异统计（如果有被忽略的项，显示统计信息）
        if (ignoredDiffCount > 0) {
            org.apache.poi.xwpf.usermodel.XWPFParagraph p2_1 = document.createParagraph();
            org.apache.poi.xwpf.usermodel.XWPFRun r2_1 = p2_1.createRun();
            r2_1.setText("差异统计: 有效差异 " + validDiffCount + " 项，已忽略差异 " + ignoredDiffCount + " 项");
            r2_1.setFontFamily("宋体");
            r2_1.setFontSize(12);
            r2_1.setColor("666666"); // 灰色显示统计信息
        }
        
        // 基准文档名称
        org.apache.poi.xwpf.usermodel.XWPFParagraph p3 = document.createParagraph();
        org.apache.poi.xwpf.usermodel.XWPFRun r3 = p3.createRun();
        r3.setText("基准文档名称: " + result.getOldFileName());
        r3.setFontFamily("宋体");
        r3.setFontSize(12);
        
        // 比对创建时间
        org.apache.poi.xwpf.usermodel.XWPFParagraph p4 = document.createParagraph();
        org.apache.poi.xwpf.usermodel.XWPFRun r4 = p4.createRun();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedTime = java.time.LocalDateTime.now().format(formatter);
        r4.setText("比对创建时间: " + formattedTime);
        r4.setFontFamily("宋体");
        r4.setFontSize(12);
        
        // 空行
        document.createParagraph();
    }
    
    /**
     * 添加差异详细表格到DOCX文档
     */
    private void addDifferenceTable(org.apache.poi.xwpf.usermodel.XWPFDocument document, CompareResult result, ExportRequest request) {
        // 获取差异数据
        List<Map<String, Object>> differences;
        if (result.getFormattedDifferences() != null && !result.getFormattedDifferences().isEmpty()) {
            differences = result.getFormattedDifferences();
        } else {
            List<DiffBlock> diffBlocks = result.getDifferences();
            if (diffBlocks != null && !diffBlocks.isEmpty()) {
                differences = formatter.convertDiffBlocksToMapFormat(diffBlocks, false, null, null);
            } else {
                differences = new ArrayList<>();
            }
        }
        
        // 不再过滤被忽略的差异，显示所有差异（包括被忽略的）
        if (differences.isEmpty()) {
            org.apache.poi.xwpf.usermodel.XWPFParagraph noDiffPara = document.createParagraph();
            org.apache.poi.xwpf.usermodel.XWPFRun noDiffRun = noDiffPara.createRun();
            noDiffRun.setText("未发现差异");
            noDiffRun.setFontFamily("宋体");
            noDiffRun.setFontSize(12);
            return;
        }
        
        // 检查是否有备注
        boolean hasRemark = differences.stream()
            .anyMatch(diff -> {
                String remark = (String) diff.get("remark");
                return remark != null && !remark.isEmpty();
            });
        
        // 创建表格: 根据是否有备注决定列数
        // 有备注: 6列 (比对文档名称, 序号, 页码, 文档修改内容, 差异类型, 备注)
        // 无备注: 5列 (比对文档名称, 序号, 页码, 文档修改内容, 差异类型)
        org.apache.poi.xwpf.usermodel.XWPFTable table = document.createTable();
        table.setWidth("100%");
        
        // 设置表格边框
        org.apache.poi.xwpf.usermodel.XWPFTableRow headerRow = table.getRow(0);
        
        // 表头
        setCellText(headerRow.getCell(0), "比对文档名称", true, true);
        headerRow.addNewTableCell();
        setCellText(headerRow.getCell(1), "序号", true, true);
        headerRow.addNewTableCell();
        setCellText(headerRow.getCell(2), "页码", true, true);
        headerRow.addNewTableCell();
        setCellText(headerRow.getCell(3), "文档修改内容", true, true);
        headerRow.addNewTableCell();
        setCellText(headerRow.getCell(4), "差异类型", true, true);
        
        // 如果有备注，添加备注列
        if (hasRemark) {
            headerRow.addNewTableCell();
            setCellText(headerRow.getCell(5), "备注", true, true);
        }
        
        // 添加数据行（包括被忽略的差异）
        for (int i = 0; i < differences.size(); i++) {
            Map<String, Object> diff = differences.get(i);
            Boolean isIgnored = (Boolean) diff.get("ignored");
            boolean ignored = isIgnored != null && isIgnored;
            
            org.apache.poi.xwpf.usermodel.XWPFTableRow row = table.createRow();
            
            // 比对文档名称（合并行）
            if (i == 0) {
                setCellText(row.getCell(0), result.getNewFileName(), false, false, ignored);
            }
            
            // 序号
            setCellText(row.getCell(1), String.valueOf(i + 1), false, false, ignored);
            
            // 页码
            Object pageObj = diff.get("page");
            String pageText = pageObj != null ? pageObj.toString() : "";
            setCellText(row.getCell(2), pageText, false, false, ignored);
            
            // 文档修改内容 (合并显示，用背景色高亮差异)
            addMergedDifferenceContent(row.getCell(3), diff, ignored);
            
            // 差异类型
            String diffType = getDifferenceType(diff);
            org.apache.poi.xwpf.usermodel.XWPFTableCell typeCell = row.getCell(4);
            typeCell.removeParagraph(0);
            org.apache.poi.xwpf.usermodel.XWPFParagraph typePara = typeCell.addParagraph();
            
            // 添加差异类型文本（如果被忽略，显示"xxx（已忽略）"）
            org.apache.poi.xwpf.usermodel.XWPFRun typeRun = typePara.createRun();
            if (ignored) {
                typeRun.setText(diffType + "（已忽略）");
                typeRun.setColor("999999"); // 灰色
            } else {
                typeRun.setText(diffType);
            }
            typeRun.setFontFamily("宋体");
            typeRun.setFontSize(10);
            
            // 如果有备注列，填充备注内容
            if (hasRemark) {
                String remark = (String) diff.get("remark");
                if (remark != null && !remark.isEmpty()) {
                    setCellText(row.getCell(5), remark, false, false, ignored);
                } else {
                    setCellText(row.getCell(5), "", false, false, ignored);
                }
            }
        }
        
        logger.info("✅ 添加了 {} 个差异项到表格（包含已忽略项）", differences.size());
    }
    
    // ========== DOCX辅助方法 ==========
    
    /**
     * 设置单元格文本（不带忽略标记）
     */
    private void setCellText(org.apache.poi.xwpf.usermodel.XWPFTableCell cell, String text, boolean bold, boolean center) {
        setCellText(cell, text, bold, center, false);
    }
    
    /**
     * 设置单元格文本（支持忽略标记）
     */
    private void setCellText(org.apache.poi.xwpf.usermodel.XWPFTableCell cell, String text, boolean bold, boolean center, boolean ignored) {
        cell.removeParagraph(0); // 移除默认段落
        org.apache.poi.xwpf.usermodel.XWPFParagraph para = cell.addParagraph();
        if (center) {
            para.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER);
        }
        org.apache.poi.xwpf.usermodel.XWPFRun run = para.createRun();
        run.setText(text);
        run.setFontFamily("宋体");
        run.setFontSize(10);
        if (bold) {
            run.setBold(true);
        }
        if (ignored) {
            run.setColor("999999"); // 灰色显示被忽略项
        }
    }
    
    /**
     * 添加合并的差异内容（使用背景色高亮）
     * 显示完整的上下文句子，并只对差异部分进行背景色高亮
     */
    private void addMergedDifferenceContent(
        org.apache.poi.xwpf.usermodel.XWPFTableCell cell, 
        Map<String, Object> diff,
        boolean ignored) {
        
        String operation = (String) diff.get("operation");
        
        cell.removeParagraph(0);
        org.apache.poi.xwpf.usermodel.XWPFParagraph para = cell.addParagraph();
        
        if ("DELETE".equals(operation)) {
            // 删除：显示完整的旧文本，对差异部分红色背景高亮
            String fullText = getFullTextFromDiff(diff, "old");
            List<Map<String, Object>> diffRanges = getDiffRangesFromDiff(diff, "old");
            addTextWithHighlight(para, fullText, diffRanges, "FFCCCC", ignored);
            
        } else if ("INSERT".equals(operation)) {
            // 新增：显示完整的新文本，对差异部分绿色背景高亮
            String fullText = getFullTextFromDiff(diff, "new");
            List<Map<String, Object>> diffRanges = getDiffRangesFromDiff(diff, "new");
            addTextWithHighlight(para, fullText, diffRanges, "CCFFCC", ignored);
            
        } else if ("MODIFY".equals(operation)) {
            // 修改：显示"完整旧文本→完整新文本"，差异部分分别用不同背景色
            String oldFullText = getFullTextFromDiff(diff, "old");
            List<Map<String, Object>> oldDiffRanges = getDiffRangesFromDiff(diff, "old");
            addTextWithHighlight(para, oldFullText, oldDiffRanges, "FFCCCC", ignored);
            
            // 箭头
            org.apache.poi.xwpf.usermodel.XWPFRun arrowRun = para.createRun();
            arrowRun.setText(" → ");
            arrowRun.setFontFamily("宋体");
            arrowRun.setFontSize(10);
            if (ignored) {
                arrowRun.setColor("999999");
            }
            
            String newFullText = getFullTextFromDiff(diff, "new");
            List<Map<String, Object>> newDiffRanges = getDiffRangesFromDiff(diff, "new");
            addTextWithHighlight(para, newFullText, newDiffRanges, "CCFFCC", ignored);
        }
    }
    
    /**
     * 添加带高亮的文本（完整文本 + 差异范围高亮）
     */
    private void addTextWithHighlight(
        org.apache.poi.xwpf.usermodel.XWPFParagraph para,
        String fullText,
        List<Map<String, Object>> diffRanges,
        String highlightColor,
        boolean ignored) {
        
        if (fullText == null || fullText.isEmpty()) {
            return;
        }
        
        // 如果没有差异范围，整个文本都是差异
        if (diffRanges == null || diffRanges.isEmpty()) {
            org.apache.poi.xwpf.usermodel.XWPFRun run = para.createRun();
            run.setText(fullText);
            run.setFontFamily("宋体");
            run.setFontSize(10);
            setRunBackgroundColor(run, highlightColor);
            if (ignored) {
                run.setColor("999999");
            }
            return;
        }
        
        // 按差异范围分段显示
        int currentPos = 0;
        for (Map<String, Object> range : diffRanges) {
            int start = getIntValue(range.get("start"), 0);
            int end = getIntValue(range.get("end"), fullText.length());
            
            // 确保索引有效
            start = Math.max(0, Math.min(start, fullText.length()));
            end = Math.max(start, Math.min(end, fullText.length()));
            
            // 添加差异前的普通文本
            if (currentPos < start) {
                String normalText = fullText.substring(currentPos, start);
                org.apache.poi.xwpf.usermodel.XWPFRun normalRun = para.createRun();
                normalRun.setText(normalText);
                normalRun.setFontFamily("宋体");
                normalRun.setFontSize(10);
                if (ignored) {
                    normalRun.setColor("999999");
                }
            }
            
            // 添加高亮的差异文本
            if (start < end) {
                String diffText = fullText.substring(start, end);
                org.apache.poi.xwpf.usermodel.XWPFRun diffRun = para.createRun();
                diffRun.setText(diffText);
                diffRun.setFontFamily("宋体");
                diffRun.setFontSize(10);
                setRunBackgroundColor(diffRun, highlightColor);
                if (ignored) {
                    diffRun.setColor("999999");
                }
            }
            
            currentPos = end;
        }
        
        // 添加最后剩余的普通文本
        if (currentPos < fullText.length()) {
            String remainingText = fullText.substring(currentPos);
            org.apache.poi.xwpf.usermodel.XWPFRun remainingRun = para.createRun();
            remainingRun.setText(remainingText);
            remainingRun.setFontFamily("宋体");
            remainingRun.setFontSize(10);
            if (ignored) {
                remainingRun.setColor("999999");
            }
        }
    }
    
    /**
     * 设置Run的背景色
     */
    private void setRunBackgroundColor(org.apache.poi.xwpf.usermodel.XWPFRun run, String color) {
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr rPr = run.getCTR().getRPr();
        if (rPr == null) {
            rPr = run.getCTR().addNewRPr();
        }
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTShd shd = rPr.addNewShd();
        shd.setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STShd.CLEAR);
        shd.setColor("auto");
        shd.setFill(color);
    }
    
    /**
     * 从差异项中获取完整文本
     */
    private String getFullTextFromDiff(Map<String, Object> diff, String type) {
        if ("old".equals(type)) {
            // 先尝试 allTextA
            Object allTextA = diff.get("allTextA");
            if (allTextA != null) {
                if (allTextA instanceof List) {
                    List<?> textList = (List<?>) allTextA;
                    if (!textList.isEmpty()) {
                        return String.join("", textList.stream()
                            .map(Object::toString)
                            .toArray(String[]::new));
                    }
                } else {
                    String text = allTextA.toString();
                    if (!text.isEmpty()) {
                        return text;
                    }
                }
            }
            // 回退到 oldText
            return getTextFromDiff(diff, "old");
        } else {
            // 先尝试 allTextB
            Object allTextB = diff.get("allTextB");
            if (allTextB != null) {
                if (allTextB instanceof List) {
                    List<?> textList = (List<?>) allTextB;
                    if (!textList.isEmpty()) {
                        return String.join("", textList.stream()
                            .map(Object::toString)
                            .toArray(String[]::new));
                    }
                } else {
                    String text = allTextB.toString();
                    if (!text.isEmpty()) {
                        return text;
                    }
                }
            }
            // 回退到 newText
            return getTextFromDiff(diff, "new");
        }
    }
    
    /**
     * 从差异项中获取差异范围
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getDiffRangesFromDiff(Map<String, Object> diff, String type) {
        String rangeKey = "old".equals(type) ? "diffRangesA" : "diffRangesB";
        Object ranges = diff.get(rangeKey);
        
        if (ranges instanceof List) {
            List<?> rangeList = (List<?>) ranges;
            List<Map<String, Object>> result = new ArrayList<>();
            
            for (Object item : rangeList) {
                Map<String, Object> rangeMap = new java.util.HashMap<>();
                
                if (item instanceof Map) {
                    // 已经是Map，直接使用
                    rangeMap.putAll((Map<String, Object>) item);
                } else if (item instanceof DiffBlock.TextRange) {
                    // 是TextRange对象，转换为Map
                    DiffBlock.TextRange textRange = (DiffBlock.TextRange) item;
                    rangeMap.put("start", textRange.start);
                    rangeMap.put("end", textRange.end);
                    rangeMap.put("type", textRange.type);
                } else {
                    // 尝试通过反射获取字段
                    try {
                        java.lang.reflect.Field startField = item.getClass().getField("start");
                        java.lang.reflect.Field endField = item.getClass().getField("end");
                        rangeMap.put("start", startField.get(item));
                        rangeMap.put("end", endField.get(item));
                        
                        try {
                            java.lang.reflect.Field typeField = item.getClass().getField("type");
                            rangeMap.put("type", typeField.get(item));
                        } catch (NoSuchFieldException ignored) {
                            // type字段可选
                        }
                    } catch (Exception e) {
                        logger.warn("无法转换差异范围对象: {}", e.getMessage());
                        continue;
                    }
                }
                
                result.add(rangeMap);
            }
            
            return result;
        }
        
        return new ArrayList<>();
    }
    
    /**
     * 安全地从Object转换为int
     */
    private int getIntValue(Object obj, int defaultValue) {
        if (obj == null) {
            return defaultValue;
        }
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }
        try {
            return Integer.parseInt(obj.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * 从差异项中获取文本
     */
    private String getTextFromDiff(Map<String, Object> diff, String type) {
        if ("old".equals(type)) {
            Object text = diff.get("oldText");
            if (text != null && !text.toString().isEmpty()) {
                return text.toString();
            }
            text = diff.get("textA");
            return text != null ? text.toString() : "";
        } else {
            Object text = diff.get("newText");
            if (text != null && !text.toString().isEmpty()) {
                return text.toString();
            }
            text = diff.get("textB");
            return text != null ? text.toString() : "";
        }
    }
    
    /**
     * 获取差异类型显示文本
     */
    private String getDifferenceType(Map<String, Object> diff) {
        String operation = (String) diff.get("operation");
        if ("DELETE".equals(operation)) {
            return "删除";
        } else if ("INSERT".equals(operation)) {
            return "新增";
        } else if ("MODIFY".equals(operation)) {
            return "修改";
        } else {
            return operation != null ? operation : "未知";
        }
    }
}

