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
 * 简化版可视化工具
 * 兼容Java 11，不使用文本块语法
 */
@Slf4j
public class SimpleVisualization {
    
    private static final String[] HIGHLIGHT_COLORS = {
        "#FFE6E6", "#E6F3FF", "#E6FFE6", "#FFF0E6", "#F0E6FF",
        "#E6FFFF", "#FFFFE6", "#FFE6F0", "#F0FFE6", "#E6E6FF"
    };
    
    /**
     * 生成简化的可视化HTML报告
     */
    public static String generateSimpleHTML(Document document, ExtractionSchema schema, 
                                          List<Extraction> extractions) {
        StringBuilder html = new StringBuilder();
        
        // HTML头部
        html.append(generateHeader(schema.getName()));
        
        // 统计信息
        html.append(generateStats(extractions, schema));
        
        // 主要内容区域 - 左右分栏
        html.append("        <div class=\"content-wrapper\">\n");
        html.append("            <div class=\"left-panel\">\n");
        
        // 提取结果表格
        html.append(generateInteractiveResults(extractions, document));
        
        html.append("            </div>\n");
        html.append("            <div class=\"right-panel\">\n");
        
        // 原文显示区域
        html.append(generateOriginalTextPanel(document, extractions));
        
        html.append("            </div>\n");
        html.append("        </div>\n");
        
        // 位置信息
        html.append(generatePositions(extractions));
        
        // HTML尾部
        html.append(generateFooter());
        
        return html.toString();
    }
    
    private static String generateHeader(String title) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n");
        sb.append("<html lang=\"zh-CN\">\n");
        sb.append("<head>\n");
        sb.append("    <meta charset=\"UTF-8\">\n");
        sb.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        sb.append("    <title>文本信息提取可视化 - ").append(title).append("</title>\n");
        sb.append("    <style>\n");
        sb.append("        body { font-family: Arial, sans-serif; margin: 20px; background: #f5f5f5; }\n");
        sb.append("        .container { max-width: 1200px; margin: 0 auto; background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }\n");
        sb.append("        .header { text-align: center; margin-bottom: 30px; padding: 20px; background: linear-gradient(135deg, #667eea, #764ba2); color: white; border-radius: 8px; }\n");
        sb.append("        .content-wrapper { display: flex; gap: 20px; }\n");
        sb.append("        .left-panel { flex: 1; }\n");
        sb.append("        .right-panel { flex: 1; }\n");
        sb.append("        .section { margin-bottom: 30px; }\n");
        sb.append("        .section-title { font-size: 1.5em; color: #333; margin-bottom: 15px; border-bottom: 2px solid #667eea; padding-bottom: 5px; }\n");
        sb.append("        .stats { display: flex; flex-wrap: wrap; gap: 15px; margin-bottom: 20px; }\n");
        sb.append("        .stat-card { background: #f8f9fa; padding: 15px; border-radius: 5px; border-left: 4px solid #667eea; flex: 1; min-width: 150px; }\n");
        sb.append("        .stat-number { font-size: 2em; font-weight: bold; color: #667eea; }\n");
        sb.append("        .stat-label { color: #666; margin-top: 5px; }\n");
        sb.append("        table { width: 100%; border-collapse: collapse; margin-top: 10px; }\n");
        sb.append("        th, td { padding: 10px; text-align: left; border-bottom: 1px solid #ddd; }\n");
        sb.append("        th { background: #f8f9fa; font-weight: bold; }\n");
        sb.append("        tr:hover { background: #f5f5f5; }\n");
        sb.append("        .extraction-row { cursor: pointer; transition: all 0.2s ease; }\n");
        sb.append("        .extraction-row:hover { background: #e3f2fd !important; }\n");
        sb.append("        .extraction-row.active { background: #bbdefb !important; }\n");
        sb.append("        .original-text { background: #f8f9fa; border: 1px solid #ddd; border-radius: 5px; padding: 15px; font-family: monospace; line-height: 1.6; white-space: pre-wrap; word-wrap: break-word; max-height: 600px; overflow-y: auto; }\n");
        sb.append("        .text-highlight { background: #ffeb3b; padding: 2px; border-radius: 3px; transition: all 0.3s ease; }\n");
        sb.append("        .text-highlight.active { background: #ff9800; color: white; box-shadow: 0 2px 8px rgba(255,152,0,0.4); }\n");
        sb.append("        .position-info { background: #e9ecef; padding: 10px; border-radius: 5px; margin: 10px 0; font-family: monospace; }\n");
        sb.append("        .footer { text-align: center; margin-top: 30px; padding: 15px; background: #f8f9fa; border-radius: 5px; color: #666; }\n");
        sb.append("        .click-hint { color: #666; font-size: 0.9em; margin-top: 10px; font-style: italic; }\n");
        sb.append("    </style>\n");
        sb.append("</head>\n");
        sb.append("<body>\n");
        sb.append("    <div class=\"container\">\n");
        sb.append("        <div class=\"header\">\n");
        sb.append("            <h1>").append(escapeHtml(title)).append("</h1>\n");
        sb.append("            <p>LangExtract 文本信息提取可视化</p>\n");
        sb.append("            <p>生成时间: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("</p>\n");
        sb.append("        </div>\n");
        
        return sb.toString();
    }
    
    private static String generateStats(List<Extraction> extractions, ExtractionSchema schema) {
        int totalFields = schema.getFields().size();
        int extractedFields = (int) extractions.stream().map(Extraction::getField).distinct().count();
        long withPositionCount = extractions.stream().filter(Extraction::hasHighQualityPosition).count();
        double avgConfidence = extractions.stream()
            .mapToDouble(e -> e.getConfidence() != null ? e.getConfidence() : 0.0)
            .average().orElse(0.0) * 100;
        
        StringBuilder sb = new StringBuilder();
        sb.append("        <div class=\"section\">\n");
        sb.append("            <h2 class=\"section-title\">📊 提取统计</h2>\n");
        sb.append("            <div class=\"stats\">\n");
        sb.append("                <div class=\"stat-card\">\n");
        sb.append("                    <div class=\"stat-number\">").append(extractedFields).append("</div>\n");
        sb.append("                    <div class=\"stat-label\">提取字段数</div>\n");
        sb.append("                </div>\n");
        sb.append("                <div class=\"stat-card\">\n");
        sb.append("                    <div class=\"stat-number\">").append(String.format("%.1f%%", (double) extractedFields / totalFields * 100)).append("</div>\n");
        sb.append("                    <div class=\"stat-label\">完成度</div>\n");
        sb.append("                </div>\n");
        sb.append("                <div class=\"stat-card\">\n");
        sb.append("                    <div class=\"stat-number\">").append(String.format("%.1f%%", avgConfidence)).append("</div>\n");
        sb.append("                    <div class=\"stat-label\">平均置信度</div>\n");
        sb.append("                </div>\n");
        sb.append("                <div class=\"stat-card\">\n");
        sb.append("                    <div class=\"stat-number\">").append(withPositionCount).append("</div>\n");
        sb.append("                    <div class=\"stat-label\">精确定位结果</div>\n");
        sb.append("                </div>\n");
        sb.append("            </div>\n");
        sb.append("        </div>\n");
        
        return sb.toString();
    }
    
    private static String generateInteractiveResults(List<Extraction> extractions, Document document) {
        StringBuilder sb = new StringBuilder();
        sb.append("        <div class=\"section\">\n");
        sb.append("            <h2 class=\"section-title\">📋 提取结果详情</h2>\n");
        sb.append("            <table>\n");
        sb.append("                <thead>\n");
        sb.append("                    <tr>\n");
        sb.append("                        <th>字段</th>\n");
        sb.append("                        <th>提取值</th>\n");
        sb.append("                        <th>置信度</th>\n");
        sb.append("                        <th>字符位置</th>\n");
        sb.append("                        <th>状态</th>\n");
        sb.append("                    </tr>\n");
        sb.append("                </thead>\n");
        sb.append("                <tbody>\n");
        
        for (Extraction extraction : extractions) {
            String field = escapeHtml(extraction.getField());
            String value = escapeHtml(String.valueOf(extraction.getValue()));
            double confidence = extraction.getConfidence() != null ? extraction.getConfidence() * 100 : 0.0;
            
            String positionInfo = "未定位";
            String status = "❌";
            
            if (extraction.getCharInterval() != null && extraction.getCharInterval().isValid()) {
                CharInterval interval = extraction.getCharInterval();
                positionInfo = String.format("%d - %d", interval.getStartPos(), interval.getEndPos());
                status = "✅";
            }
            
            String extractionId = "extraction-" + extraction.hashCode();
            sb.append("                    <tr class=\"extraction-row\" data-extraction-id=\"").append(extractionId).append("\">\n");
            sb.append("                        <td><strong>").append(field).append("</strong></td>\n");
            sb.append("                        <td>").append(value).append("</td>\n");
            sb.append("                        <td>").append(String.format("%.1f%%", confidence)).append("</td>\n");
            sb.append("                        <td><code>").append(positionInfo).append("</code></td>\n");
            sb.append("                        <td>").append(status).append("</td>\n");
            sb.append("                    </tr>\n");
        }
        
        sb.append("                </tbody>\n");
        sb.append("            </table>\n");
        sb.append("            <div class=\"click-hint\">💡 点击表格行可在右侧原文中高亮显示对应位置</div>\n");
        sb.append("        </div>\n");
        
        return sb.toString();
    }
    
    private static String generatePositions(List<Extraction> extractions) {
        StringBuilder sb = new StringBuilder();
        sb.append("        <div class=\"section\">\n");
        sb.append("            <h2 class=\"section-title\">🎯 字符位置锚定详情</h2>\n");
        
        List<Extraction> withPosition = extractions.stream()
            .filter(e -> e.getCharInterval() != null && e.getCharInterval().isValid())
            .collect(Collectors.toList());
        
        if (withPosition.isEmpty()) {
            sb.append("            <p>暂无字符位置信息</p>\n");
        } else {
            for (Extraction extraction : withPosition) {
                CharInterval interval = extraction.getCharInterval();
                sb.append("            <div class=\"position-info\">\n");
                sb.append("                <strong>字段:</strong> ").append(escapeHtml(extraction.getField())).append("<br>\n");
                sb.append("                <strong>提取值:</strong> ").append(escapeHtml(String.valueOf(extraction.getValue()))).append("<br>\n");
                sb.append("                <strong>原文片段:</strong> \"").append(escapeHtml(interval.getSourceText())).append("\"<br>\n");
                sb.append("                <strong>字符位置:</strong> ").append(interval.getStartPos()).append(" - ").append(interval.getEndPos()).append(" (长度: ").append(interval.getLength()).append(")<br>\n");
                sb.append("                <strong>对齐置信度:</strong> ").append(String.format("%.2f", interval.getAlignmentConfidence())).append("<br>\n");
                sb.append("                <strong>状态:</strong> ").append(interval.isValid() ? "✅ 有效" : "❌ 无效").append("\n");
                sb.append("            </div>\n");
            }
        }
        
        sb.append("        </div>\n");
        return sb.toString();
    }
    
    private static String generateFooter() {
        StringBuilder sb = new StringBuilder();
        sb.append("        <div class=\"footer\">\n");
        sb.append("            <p>由 LangExtract Java版本 生成 | 基于阿里云通义千问 | 字符级位置锚定技术</p>\n");
        sb.append("        </div>\n");
        sb.append("    </div>\n");
        
        // 添加JavaScript交互功能
        sb.append("    <script>\n");
        sb.append("        document.addEventListener('DOMContentLoaded', function() {\n");
        sb.append("            // 获取所有提取结果行\n");
        sb.append("            const extractionRows = document.querySelectorAll('.extraction-row');\n");
        sb.append("            const originalText = document.getElementById('originalText');\n");
        sb.append("            \n");
        sb.append("            // 为每一行添加点击事件\n");
        sb.append("            extractionRows.forEach(function(row) {\n");
        sb.append("                row.addEventListener('click', function() {\n");
        sb.append("                    const extractionId = this.getAttribute('data-extraction-id');\n");
        sb.append("                    \n");
        sb.append("                    // 清除所有高亮状态\n");
        sb.append("                    clearAllHighlights();\n");
        sb.append("                    \n");
        sb.append("                    // 高亮当前选中的行\n");
        sb.append("                    this.classList.add('active');\n");
        sb.append("                    \n");
        sb.append("                    // 在原文中高亮对应文本\n");
        sb.append("                    const textSpan = originalText.querySelector(`[data-extraction-id=\"${extractionId}\"]`);\n");
        sb.append("                    if (textSpan) {\n");
        sb.append("                        textSpan.classList.add('active');\n");
        sb.append("                        \n");
        sb.append("                        // 滚动到对应位置\n");
        sb.append("                        textSpan.scrollIntoView({ \n");
        sb.append("                            behavior: 'smooth', \n");
        sb.append("                            block: 'center' \n");
        sb.append("                        });\n");
        sb.append("                        \n");
        sb.append("                        // 添加闪烁效果\n");
        sb.append("                        textSpan.style.animation = 'pulse 1s ease-in-out 2';\n");
        sb.append("                        setTimeout(() => {\n");
        sb.append("                            textSpan.style.animation = '';\n");
        sb.append("                        }, 2000);\n");
        sb.append("                    }\n");
        sb.append("                });\n");
        sb.append("            });\n");
        sb.append("            \n");
        sb.append("            // 原文中的高亮片段点击事件\n");
        sb.append("            const textHighlights = originalText.querySelectorAll('.text-highlight');\n");
        sb.append("            textHighlights.forEach(function(highlight) {\n");
        sb.append("                highlight.addEventListener('click', function() {\n");
        sb.append("                    const extractionId = this.getAttribute('data-extraction-id');\n");
        sb.append("                    \n");
        sb.append("                    // 清除所有高亮状态\n");
        sb.append("                    clearAllHighlights();\n");
        sb.append("                    \n");
        sb.append("                    // 高亮对应的表格行\n");
        sb.append("                    const row = document.querySelector(`.extraction-row[data-extraction-id=\"${extractionId}\"]`);\n");
        sb.append("                    if (row) {\n");
        sb.append("                        row.classList.add('active');\n");
        sb.append("                        row.scrollIntoView({ \n");
        sb.append("                            behavior: 'smooth', \n");
        sb.append("                            block: 'center' \n");
        sb.append("                        });\n");
        sb.append("                    }\n");
        sb.append("                    \n");
        sb.append("                    // 高亮当前文本\n");
        sb.append("                    this.classList.add('active');\n");
        sb.append("                });\n");
        sb.append("            });\n");
        sb.append("            \n");
        sb.append("            // 清除所有高亮状态的函数\n");
        sb.append("            function clearAllHighlights() {\n");
        sb.append("                // 清除表格行高亮\n");
        sb.append("                extractionRows.forEach(row => row.classList.remove('active'));\n");
        sb.append("                \n");
        sb.append("                // 清除文本高亮\n");
        sb.append("                textHighlights.forEach(highlight => highlight.classList.remove('active'));\n");
        sb.append("            }\n");
        sb.append("        });\n");
        sb.append("        \n");
        sb.append("        // 添加CSS动画\n");
        sb.append("        const style = document.createElement('style');\n");
        sb.append("        style.textContent = `\n");
        sb.append("            @keyframes pulse {\n");
        sb.append("                0%, 100% { transform: scale(1); }\n");
        sb.append("                50% { transform: scale(1.05); box-shadow: 0 0 20px rgba(255,152,0,0.6); }\n");
        sb.append("            }\n");
        sb.append("        `;\n");
        sb.append("        document.head.appendChild(style);\n");
        sb.append("    </script>\n");
        sb.append("</body>\n");
        sb.append("</html>\n");
        return sb.toString();
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
     * 生成原文显示面板
     */
    private static String generateOriginalTextPanel(Document document, List<Extraction> extractions) {
        StringBuilder sb = new StringBuilder();
        sb.append("        <div class=\"section\">\n");
        sb.append("            <h2 class=\"section-title\">📄 原文内容</h2>\n");
        sb.append("            <div class=\"original-text\" id=\"originalText\">\n");
        
        // 生成带高亮标记的文本
        String highlightedText = generateHighlightedOriginalText(document.getContent(), extractions);
        sb.append(highlightedText);
        
        sb.append("            </div>\n");
        sb.append("            <div class=\"click-hint\">💡 点击左侧提取结果可高亮显示对应文本位置</div>\n");
        sb.append("        </div>\n");
        
        return sb.toString();
    }
    
    /**
     * 生成带高亮标记的原文
     */
    private static String generateHighlightedOriginalText(String originalText, List<Extraction> extractions) {
        if (originalText == null || originalText.isEmpty()) {
            return "无原文内容";
        }
        
        // 创建高亮标记列表
        List<HighlightMark> marks = new ArrayList<>();
        
        for (Extraction extraction : extractions) {
            if (extraction.getCharInterval() != null && extraction.getCharInterval().isValid()) {
                CharInterval interval = extraction.getCharInterval();
                String extractionId = "extraction-" + extraction.hashCode();
                
                marks.add(new HighlightMark(
                    interval.getStartPos(), 
                    interval.getEndPos(), 
                    extractionId,
                    extraction.getField()
                ));
            }
        }
        
        // 按位置排序，避免重叠
        marks.sort((a, b) -> Integer.compare(a.start, b.start));
        
        // 生成带标记的HTML
        StringBuilder result = new StringBuilder();
        int lastIndex = 0;
        
        for (HighlightMark mark : marks) {
            // 跳过重叠的标记
            if (mark.start < lastIndex) {
                continue;
            }
            
            // 添加标记前的文本
            if (mark.start > lastIndex) {
                result.append(escapeHtml(originalText.substring(lastIndex, mark.start)));
            }
            
            // 添加高亮标记
            result.append("<span class=\"text-highlight\" data-extraction-id=\"")
                  .append(mark.extractionId)
                  .append("\" title=\"字段: ")
                  .append(escapeHtml(mark.fieldName))
                  .append("\">")
                  .append(escapeHtml(originalText.substring(mark.start, mark.end)))
                  .append("</span>");
            
            lastIndex = mark.end;
        }
        
        // 添加剩余文本
        if (lastIndex < originalText.length()) {
            result.append(escapeHtml(originalText.substring(lastIndex)));
        }
        
        return result.toString();
    }
    
    /**
     * 辅助类：高亮标记
     */
    private static class HighlightMark {
        int start;
        int end;
        String extractionId;
        String fieldName;
        
        HighlightMark(int start, int end, String extractionId, String fieldName) {
            this.start = start;
            this.end = end;
            this.extractionId = extractionId;
            this.fieldName = fieldName;
        }
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
