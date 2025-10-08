package com.zhaoxinms.contract.tools.comparePRO.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhaoxinms.contract.tools.comparePRO.model.CharBox;
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
            
            // 2. 添加基本信息
            addBasicInfo(document, result, request);
            
            // 3. 添加差异详情表格
            addDifferenceTable(document, result, request);
            
            // 4. 写入输出流
            document.write(baos);
            
            logger.info("✅ DOCX报告生成完成，大小: {} KB", baos.size() / 1024);
            
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
        Path taskPath = Paths.get(uploadRootPath, "compare-pro", "tasks", taskId);
        
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
            taskStatus.put("status", "completed");
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
        // 创建基本信息段落
        org.apache.poi.xwpf.usermodel.XWPFParagraph para = document.createParagraph();
        org.apache.poi.xwpf.usermodel.XWPFRun run = para.createRun();
        run.addBreak();
        run.setText("基本信息");
        run.setBold(true);
        run.setFontSize(14);
        run.addBreak();
        run.addBreak();
        
        // 添加文件名信息
        run = para.createRun();
        run.setText("原文档: " + result.getOldFileName());
        run.addBreak();
        run.setText("新文档: " + result.getNewFileName());
        run.addBreak();
        run.setText("差异数量: " + result.getTotalDiffCount());
        run.addBreak();
        run.addBreak();
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
        
        if (differences.isEmpty()) {
            org.apache.poi.xwpf.usermodel.XWPFParagraph para = document.createParagraph();
            org.apache.poi.xwpf.usermodel.XWPFRun run = para.createRun();
            run.setText("无差异");
            return;
        }
        
        // 创建表格
        org.apache.poi.xwpf.usermodel.XWPFTable table = document.createTable();
        
        // 添加表头
        org.apache.poi.xwpf.usermodel.XWPFTableRow headerRow = table.getRow(0);
        headerRow.getCell(0).setText("序号");
        headerRow.addNewTableCell().setText("操作类型");
        headerRow.addNewTableCell().setText("原文本");
        headerRow.addNewTableCell().setText("新文本");
        
        // 添加差异行
        int index = 1;
        for (Map<String, Object> diff : differences) {
            // 跳过已忽略的差异
            Boolean isIgnored = (Boolean) diff.get("ignored");
            if (isIgnored != null && isIgnored) {
                continue;
            }
            
            org.apache.poi.xwpf.usermodel.XWPFTableRow row = table.createRow();
            row.getCell(0).setText(String.valueOf(index++));
            row.getCell(1).setText((String) diff.get("operation"));
            row.getCell(2).setText((String) diff.getOrDefault("oldText", ""));
            row.getCell(3).setText((String) diff.getOrDefault("newText", ""));
        }
    }
}

