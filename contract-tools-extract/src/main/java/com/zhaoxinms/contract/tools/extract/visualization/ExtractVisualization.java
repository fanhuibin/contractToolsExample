package com.zhaoxinms.contract.tools.extract.visualization;

import com.zhaoxinms.contract.tools.extract.core.data.CharInterval;
import com.zhaoxinms.contract.tools.extract.core.data.Document;
import com.zhaoxinms.contract.tools.extract.core.data.Extraction;
import com.zhaoxinms.contract.tools.extract.core.data.ExtractionSchema;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 提取结果可视化工具
 * 生成HTML格式的可视化报告，展示文本提取结果和字符位置锚定
 */
@Slf4j
public class ExtractVisualization {
    
    private static final String[] HIGHLIGHT_COLORS = {
        "#FFE6E6", "#E6F3FF", "#E6FFE6", "#FFF0E6", "#F0E6FF",
        "#E6FFFF", "#FFFFE6", "#FFE6F0", "#F0FFE6", "#E6E6FF"
    };
    
    /**
     * 生成可视化HTML报告
     */
    public static String generateVisualizationHTML(Document document, ExtractionSchema schema, 
                                                  List<Extraction> extractions) {
        StringBuilder html = new StringBuilder();
        
        // HTML头部
        html.append(generateHTMLHeader(schema.getName()));
        
        // 生成统计信息
        html.append(generateStatistics(extractions, schema));
        
        // 生成高亮文本
        html.append(generateHighlightedText(document, extractions));
        
        // 生成提取结果表格
        html.append(generateExtractionTable(extractions));
        
        // 生成字符位置详情
        html.append(generateCharIntervalDetails(extractions));
        
        // HTML尾部
        html.append(generateHTMLFooter());
        
        return html.toString();
    }
    
    /**
     * 生成HTML头部
     */
    private static String generateHTMLHeader(String title) {
        return String.format(
            "<!DOCTYPE html>\n" +
            "<html lang=\"zh-CN\">\n" +
            "<head>\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
            "    <title>文本信息提取可视化 - %s</title>\n" +
            "    <style>\n" +
            "        * {\n" +
            "            margin: 0;\n" +
            "            padding: 0;\n" +
            "            box-sizing: border-box;\n" +
            "        }\n" +
            "        \n" +
            "        body {\n" +
            "            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;\n" +
            "            line-height: 1.6;\n" +
            "            color: #333;\n" +
            "            background-color: #f5f7fa;\n" +
            "            padding: 20px;\n" +
            "        }\n" +
            "        \n" +
            "        .container {\n" +
            "            max-width: 1200px;\n" +
            "            margin: 0 auto;\n" +
            "            background: white;\n" +
            "            border-radius: 12px;\n" +
            "            box-shadow: 0 4px 20px rgba(0,0,0,0.1);\n" +
            "            overflow: hidden;\n" +
            "        }\n" +
            "        \n" +
            "        .header {\n" +
            "            background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);\n" +
            "            color: white;\n" +
            "            padding: 30px;\n" +
            "            text-align: center;\n" +
            "        }\n" +
            "        \n" +
            "        .header h1 {\n" +
            "            font-size: 2.5em;\n" +
            "            margin-bottom: 10px;\n" +
            "            font-weight: 300;\n" +
            "        }\n" +
            "        \n" +
            "        .header .subtitle {\n" +
            "            font-size: 1.1em;\n" +
            "            opacity: 0.9;\n" +
            "        }\n" +
            "        \n" +
            "        .content {\n" +
            "            padding: 30px;\n" +
            "        }\n" +
            "        \n" +
            "        .section {\n" +
            "            margin-bottom: 40px;\n" +
            "        }\n" +
            "        \n" +
            "        .section-title {\n" +
            "            font-size: 1.8em;\n" +
            "            color: #2c3e50;\n" +
            "            margin-bottom: 20px;\n" +
            "            padding-bottom: 10px;\n" +
            "            border-bottom: 3px solid #3498db;\n" +
            "        }\n" +
            "        \n" +
            "        .stats-grid {\n" +
            "            display: grid;\n" +
            "            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));\n" +
            "            gap: 20px;\n" +
            "            margin-bottom: 30px;\n" +
            "        }\n" +
            "        \n" +
            "        .stat-card {\n" +
            "            background: #f8f9fa;\n" +
            "            padding: 20px;\n" +
            "            border-radius: 8px;\n" +
            "            text-align: center;\n" +
            "            border-left: 4px solid #3498db;\n" +
            "        }\n" +
            "        \n" +
            "        .stat-number {\n" +
            "            font-size: 2.5em;\n" +
            "            font-weight: bold;\n" +
            "            color: #2980b9;\n" +
            "            display: block;\n" +
            "        }\n" +
            "        \n" +
            "        .stat-label {\n" +
            "            color: #7f8c8d;\n" +
            "            margin-top: 5px;\n" +
            "        }\n" +
            "        \n" +
            "        .text-container {\n" +
            "            background: #fdfdfd;\n" +
            "            border: 1px solid #e1e8ed;\n" +
            "            border-radius: 8px;\n" +
            "            padding: 25px;\n" +
            "            font-size: 1.1em;\n" +
            "            line-height: 1.8;\n" +
            "            font-family: 'SF Mono', Monaco, 'Cascadia Code', 'Roboto Mono', Consolas, 'Courier New', monospace;\n" +
            "            white-space: pre-wrap;\n" +
            "            word-break: break-word;\n" +
            "            max-height: 600px;\n" +
            "            overflow-y: auto;\n" +
            "        }\n" +
            "        \n" +
            "        .highlight {\n" +
            "            padding: 2px 4px;\n" +
            "            border-radius: 3px;\n" +
            "            font-weight: 500;\n" +
            "            border: 1px solid rgba(0,0,0,0.1);\n" +
            "            position: relative;\n" +
            "            cursor: pointer;\n" +
            "            transition: all 0.2s ease;\n" +
            "        }\n" +
            "        \n" +
            "        .highlight:hover {\n" +
            "            transform: scale(1.02);\n" +
            "            box-shadow: 0 2px 8px rgba(0,0,0,0.15);\n" +
            "            z-index: 10;\n" +
            "        }\n" +
            "        \n" +
            "        .table-container {\n" +
            "            overflow-x: auto;\n" +
            "            border-radius: 8px;\n" +
            "            border: 1px solid #e1e8ed;\n" +
            "        }\n" +
            "        \n" +
            "        table {\n" +
            "            width: 100%%;\n" +
            "            border-collapse: collapse;\n" +
            "            background: white;\n" +
            "        }\n" +
            "        \n" +
            "        th, td {\n" +
            "            padding: 12px 15px;\n" +
            "            text-align: left;\n" +
            "            border-bottom: 1px solid #e1e8ed;\n" +
            "        }\n" +
            "        \n" +
            "        th {\n" +
            "            background: #f8f9fa;\n" +
            "            font-weight: 600;\n" +
            "            color: #2c3e50;\n" +
            "            position: sticky;\n" +
            "            top: 0;\n" +
            "            z-index: 10;\n" +
            "        }\n" +
            "        \n" +
            "        tr:hover {\n" +
            "            background: #f8f9fa;\n" +
            "        }\n" +
            "        \n" +
            "        .confidence-bar {\n" +
            "            width: 100px;\n" +
            "            height: 8px;\n" +
            "            background: #ecf0f1;\n" +
            "            border-radius: 4px;\n" +
            "            overflow: hidden;\n" +
            "        }\n" +
            "        \n" +
            "        .confidence-fill {\n" +
            "            height: 100%%;\n" +
            "            background: linear-gradient(90deg, #e74c3c, #f39c12, #27ae60);\n" +
            "            border-radius: 4px;\n" +
            "            transition: width 0.3s ease;\n" +
            "        }\n" +
            "        \n" +
            "        .position-info {\n" +
            "            background: #ecf0f1;\n" +
            "            padding: 15px;\n" +
            "            border-radius: 6px;\n" +
            "            margin: 10px 0;\n" +
            "            font-family: monospace;\n" +
            "        }\n" +
            "        \n" +
            "        .legend {\n" +
            "            display: flex;\n" +
            "            flex-wrap: wrap;\n" +
            "            gap: 15px;\n" +
            "            margin-bottom: 20px;\n" +
            "        }\n" +
            "        \n" +
            "        .legend-item {\n" +
            "            display: flex;\n" +
            "            align-items: center;\n" +
            "            gap: 8px;\n" +
            "            padding: 8px 12px;\n" +
            "            background: #f8f9fa;\n" +
            "            border-radius: 20px;\n" +
            "            font-size: 0.9em;\n" +
            "        }\n" +
            "        \n" +
            "        .legend-color {\n" +
            "            width: 16px;\n" +
            "            height: 16px;\n" +
            "            border-radius: 3px;\n" +
            "            border: 1px solid rgba(0,0,0,0.1);\n" +
            "        }\n" +
            "        \n" +
            "        .footer {\n" +
            "            background: #34495e;\n" +
            "            color: #ecf0f1;\n" +
            "            text-align: center;\n" +
            "            padding: 20px;\n" +
            "            font-size: 0.9em;\n" +
            "        }\n" +
            "        \n" +
            "        @media (max-width: 768px) {\n" +
            "            .container {\n" +
            "                margin: 10px;\n" +
            "                border-radius: 8px;\n" +
            "            }\n" +
            "            \n" +
            "            .content {\n" +
            "                padding: 20px;\n" +
            "            }\n" +
            "            \n" +
            "            .header {\n" +
            "                padding: 20px;\n" +
            "            }\n" +
            "            \n" +
            "            .header h1 {\n" +
            "                font-size: 2em;\n" +
            "            }\n" +
            "            \n" +
            "            .stats-grid {\n" +
            "                grid-template-columns: 1fr;\n" +
            "            }\n" +
            "        }\n" +
            "    </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "    <div class=\"container\">\n" +
            "        <div class=\"header\">\n" +
            "            <h1>%s</h1>\n" +
            "            <div class=\"subtitle\">基于LangExtract算法的文本信息提取可视化</div>\n" +
            "            <div class=\"subtitle\">生成时间: %s</div>\n" +
            "        </div>\n" +
            "        <div class=\"content\">\n",
            title, title, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }
    
    /**
     * 生成统计信息
     */
    private static String generateStatistics(List<Extraction> extractions, ExtractionSchema schema) {
        int totalFields = schema.getFields().size();
        int extractedFields = (int) extractions.stream().map(Extraction::getField).distinct().count();
        long highConfidenceCount = extractions.stream().filter(e -> e.isConfidentEnough(0.8)).count();
        long withPositionCount = extractions.stream().filter(Extraction::hasHighQualityPosition).count();
        
        double completeness = totalFields > 0 ? (double) extractedFields / totalFields * 100 : 0;
        double avgConfidence = extractions.stream()
            .mapToDouble(e -> e.getConfidence() != null ? e.getConfidence() : 0.0)
            .average().orElse(0.0) * 100;
        
        String stats = 
            "<div class=\"section\">\n" +
            "    <h2 class=\"section-title\">📊 提取统计</h2>\n" +
            "    <div class=\"stats-grid\">\n" +
            "        <div class=\"stat-card\">\n" +
            "            <span class=\"stat-number\">%d</span>\n" +
            "            <div class=\"stat-label\">提取字段数</div>\n" +
            "        </div>\n" +
            "        <div class=\"stat-card\">\n" +
            "            <span class=\"stat-number\">%.1f%%</span>\n" +
            "            <div class=\"stat-label\">完成度</div>\n" +
            "        </div>\n" +
            "        <div class=\"stat-card\">\n" +
            "            <span class=\"stat-number\">%.1f%%</span>\n" +
            "            <div class=\"stat-label\">平均置信度</div>\n" +
            "        </div>\n" +
            "        <div class=\"stat-card\">\n" +
            "            <span class=\"stat-number\">%d</span>\n" +
            "            <div class=\"stat-label\">高置信度结果</div>\n" +
            "        </div>\n" +
            "        <div class=\"stat-card\">\n" +
            "            <span class=\"stat-number\">%d</span>\n" +
            "            <div class=\"stat-label\">精确定位结果</div>\n" +
            "        </div>\n" +
            "    </div>\n" +
            "</div>\n";
        
        return String.format(stats, extractedFields, completeness, avgConfidence, highConfidenceCount, withPositionCount);
    }
    
    /**
     * 生成高亮文本
     */
    private static String generateHighlightedText(Document document, List<Extraction> extractions) {
        StringBuilder html = new StringBuilder();
        html.append(
            "<div class=\"section\">\n" +
            "    <h2 class=\"section-title\">📝 文本高亮显示</h2>\n"
        );
        
        // 生成图例
        html.append("<div class=\"legend\">");
        Map<String, String> fieldColors = new HashMap<>();
        int colorIndex = 0;
        
        for (Extraction extraction : extractions) {
            if (!fieldColors.containsKey(extraction.getField())) {
                String color = HIGHLIGHT_COLORS[colorIndex % HIGHLIGHT_COLORS.length];
                fieldColors.put(extraction.getField(), color);
                
                html.append(String.format(
                    "<div class=\"legend-item\">\n" +
                    "    <div class=\"legend-color\" style=\"background-color: %s;\"></div>\n" +
                    "    <span>%s</span>\n" +
                    "</div>\n", color, extraction.getField()));
                
                colorIndex++;
            }
        }
        html.append("</div>");
        
        // 生成高亮文本
        String highlightedText = highlightTextWithExtractions(document.getContent(), extractions, fieldColors);
        html.append(String.format(
            "<div class=\"text-container\">%s</div>\n" +
            "</div>\n", highlightedText));
        
        return html.toString();
    }
    
    /**
     * 在文本中高亮显示提取结果
     */
    private static String highlightTextWithExtractions(String originalText, List<Extraction> extractions, 
                                                      Map<String, String> fieldColors) {
        if (originalText == null || extractions.isEmpty()) {
            return escapeHtml(originalText);
        }
        
        // 按位置排序提取结果
        List<Extraction> sortedExtractions = extractions.stream()
            .filter(e -> e.getCharInterval() != null && e.getCharInterval().isValid())
            .sorted((e1, e2) -> Integer.compare(e1.getCharInterval().getStartPos(), e2.getCharInterval().getStartPos()))
            .collect(Collectors.toList());
        
        StringBuilder result = new StringBuilder();
        int lastIndex = 0;
        
        for (Extraction extraction : sortedExtractions) {
            CharInterval interval = extraction.getCharInterval();
            int start = interval.getStartPos();
            int end = interval.getEndPos();
            
            // 确保位置在文本范围内
            if (start < lastIndex || start >= originalText.length() || end > originalText.length()) {
                continue;
            }
            
            // 添加未高亮的文本
            if (start > lastIndex) {
                result.append(escapeHtml(originalText.substring(lastIndex, start)));
            }
            
            // 添加高亮文本
            String highlightedText = originalText.substring(start, end);
            String color = fieldColors.get(extraction.getField());
            String confidence = extraction.getConfidence() != null ? 
                String.format("%.1f%%", extraction.getConfidence() * 100) : "N/A";
            String alignmentConf = extraction.getAlignmentConfidence() != null ?
                String.format("%.1f%%", extraction.getAlignmentConfidence() * 100) : "N/A";
            
            result.append(String.format(
                "<span class=\"highlight\" style=\"background-color: %s;\" " +
                "title=\"字段: %s | 值: %s | 置信度: %s | 对齐: %s | 位置: %d-%d\">%s</span>", 
                color, extraction.getField(), extraction.getValue(), 
                confidence, alignmentConf, start, end, escapeHtml(highlightedText)));
            
            lastIndex = end;
        }
        
        // 添加剩余文本
        if (lastIndex < originalText.length()) {
            result.append(escapeHtml(originalText.substring(lastIndex)));
        }
        
        return result.toString();
    }
    
    /**
     * 生成提取结果表格
     */
    private static String generateExtractionTable(List<Extraction> extractions) {
        StringBuilder html = new StringBuilder();
        html.append(
            "<div class=\"section\">\n" +
            "    <h2 class=\"section-title\">📋 提取结果详情</h2>\n" +
            "    <div class=\"table-container\">\n" +
            "        <table>\n" +
            "            <thead>\n" +
            "                <tr>\n" +
            "                    <th>字段</th>\n" +
            "                    <th>提取值</th>\n" +
            "                    <th>置信度</th>\n" +
            "                    <th>对齐置信度</th>\n" +
            "                    <th>字符位置</th>\n" +
            "                    <th>文本长度</th>\n" +
            "                </tr>\n" +
            "            </thead>\n" +
            "            <tbody>\n"
        );
        
        for (Extraction extraction : extractions) {
            String field = escapeHtml(extraction.getField());
            String value = escapeHtml(String.valueOf(extraction.getValue()));
            double confidence = extraction.getConfidence() != null ? extraction.getConfidence() : 0.0;
            Double alignmentConf = extraction.getAlignmentConfidence();
            
            String positionInfo = "未定位";
            int textLength = 0;
            
            if (extraction.getCharInterval() != null && extraction.getCharInterval().isValid()) {
                CharInterval interval = extraction.getCharInterval();
                positionInfo = String.format("%d - %d", interval.getStartPos(), interval.getEndPos());
                textLength = interval.getLength();
            }
            
            String alignmentConfStr = alignmentConf != null ? String.format("%.1f%%", alignmentConf * 100) : "N/A";
            
            html.append(String.format(
                "<tr>\n" +
                "    <td><strong>%s</strong></td>\n" +
                "    <td>%s</td>\n" +
                "    <td>\n" +
                "        <div class=\"confidence-bar\">\n" +
                "            <div class=\"confidence-fill\" style=\"width: %.1f%%;\"></div>\n" +
                "        </div>\n" +
                "        %.1f%%\n" +
                "    </td>\n" +
                "    <td>%s</td>\n" +
                "    <td><code>%s</code></td>\n" +
                "    <td>%d 字符</td>\n" +
                "</tr>\n", field, value, confidence * 100, confidence * 100, 
                alignmentConfStr, positionInfo, textLength));
        }
        
        html.append(
            "            </tbody>\n" +
            "        </table>\n" +
            "    </div>\n" +
            "</div>\n"
        );
        
        return html.toString();
    }
    
    /**
     * 生成字符位置详情
     */
    private static String generateCharIntervalDetails(List<Extraction> extractions) {
        StringBuilder html = new StringBuilder();
        html.append(
            "<div class=\"section\">\n" +
            "    <h2 class=\"section-title\">🎯 字符位置锚定详情</h2>\n"
        );
        
        List<Extraction> withPosition = extractions.stream()
            .filter(e -> e.getCharInterval() != null && e.getCharInterval().isValid())
            .collect(Collectors.toList());
        
        if (withPosition.isEmpty()) {
            html.append("<p>暂无字符位置信息</p>");
        } else {
            for (Extraction extraction : withPosition) {
                CharInterval interval = extraction.getCharInterval();
                html.append(String.format(
                    "<div class=\"position-info\">\n" +
                    "    <strong>字段:</strong> %s<br>\n" +
                    "    <strong>提取值:</strong> %s<br>\n" +
                    "    <strong>原文片段:</strong> \"%s\"<br>\n" +
                    "    <strong>字符位置:</strong> %d - %d (长度: %d)<br>\n" +
                    "    <strong>对齐置信度:</strong> %.2f<br>\n" +
                    "    <strong>重叠检测:</strong> %s\n" +
                    "</div>\n", 
                    escapeHtml(extraction.getField()),
                    escapeHtml(String.valueOf(extraction.getValue())),
                    escapeHtml(interval.getSourceText()),
                    interval.getStartPos(),
                    interval.getEndPos(),
                    interval.getLength(),
                    interval.getAlignmentConfidence(),
                    interval.isValid() ? "✅ 有效" : "❌ 无效"
                ));
            }
        }
        
        html.append("</div>");
        return html.toString();
    }
    
    /**
     * 生成HTML尾部
     */
    private static String generateHTMLFooter() {
        return 
            "        </div>\n" +
            "        <div class=\"footer\">\n" +
            "            <p>由 LangExtract Java版本 生成 | 基于阿里云通义千问 | 字符级位置锚定技术</p>\n" +
            "        </div>\n" +
            "    </div>\n" +
            "\n" +
            "    <script>\n" +
            "        // 添加交互功能\n" +
            "        document.addEventListener('DOMContentLoaded', function() {\n" +
            "            // 高亮文本点击事件\n" +
            "            document.querySelectorAll('.highlight').forEach(function(element) {\n" +
            "                element.addEventListener('click', function() {\n" +
            "                    alert('字段详情:\\n' + this.getAttribute('title'));\n" +
            "                });\n" +
            "            });\n" +
            "            \n" +
            "            // 表格排序功能\n" +
            "            document.querySelectorAll('th').forEach(function(header, index) {\n" +
            "                header.style.cursor = 'pointer';\n" +
            "                header.addEventListener('click', function() {\n" +
            "                    sortTable(index);\n" +
            "                });\n" +
            "            });\n" +
            "        });\n" +
            "        \n" +
            "        function sortTable(columnIndex) {\n" +
            "            // 简单的表格排序实现\n" +
            "            console.log('排序列:', columnIndex);\n" +
            "        }\n" +
            "    </script>\n" +
            "</body>\n" +
            "</html>\n";
    }
    
    /**
     * HTML转义
     */
    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#x27;");
    }
    
    /**
     * 保存HTML到文件
     */
    public static void saveToFile(String htmlContent, String filePath) throws IOException {
        Path path = Paths.get(filePath);
        Files.createDirectories(path.getParent());
        Files.write(path, htmlContent.getBytes("UTF-8"));
        log.info("可视化HTML已保存到: {}", path.toAbsolutePath());
    }
}
