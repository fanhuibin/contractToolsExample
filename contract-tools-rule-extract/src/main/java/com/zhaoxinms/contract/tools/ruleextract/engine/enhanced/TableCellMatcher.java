package com.zhaoxinms.contract.tools.ruleextract.engine.enhanced;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.zhaoxinms.contract.tools.comparePRO.util.HtmlTableParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 表格单元格匹配器（支持 MinerU HTML 表格格式）
 * 
 * 配置格式：
 * 
 * 模式1 - 抽取单个单元格：
 * {
 *   "extractMode": "cell",               // cell: 抽取单元格, table: 抽取整表
 *   "headerPattern": "商品名称|数量|单价", // 表头特征（用于识别目标表格）
 *   "targetColumn": "商品名称",           // 目标列名
 *   "rowMarker": "合计",                  // 行标识（可选）
 *   "columnIndex": 2,                     // 或直接指定列索引（从1开始）
 *   "rowIndex": 3,                        // 或直接指定数据行索引（从1开始，不包含表头）
 *   "occurrence": 1                       // 提取第几个匹配项
 * }
 * 
 * 注意：rowIndex 是数据行索引，不包含表头行
 * 例如：rowIndex=1 表示第1行数据（表格的第2行，因为第1行是表头）
 * 
 * 模式2 - 抽取整个表格：
 * {
 *   "extractMode": "table",              // 抽取整表模式
 *   "headerPattern": "商品名称|数量|单价", // 表头特征（用于识别目标表格）
 *   "format": "json"                      // 返回格式: json/html/markdown
 * }
 *
 * @author zhaoxin
 * @since 2024-10-09
 */
@Slf4j
@Component
public class TableCellMatcher {
    
    /**
     * 表格数据结构
     */
    private static class TableData {
        List<String> headers = new ArrayList<>();
        List<List<String>> rows = new ArrayList<>();
        String rawHtml;
        int startPosition = -1;  // 表格在原文中的起始位置
        int endPosition = -1;    // 表格在原文中的结束位置
        
        /**
         * 转换为 JSON 格式
         */
        JSONArray toJSON() {
            JSONArray result = new JSONArray();
            for (List<String> row : rows) {
                JSONObject rowObj = new JSONObject();
                for (int i = 0; i < headers.size() && i < row.size(); i++) {
                    rowObj.put(headers.get(i), row.get(i));
                }
                result.add(rowObj);
            }
            return result;
        }
        
        /**
         * 转换为 Markdown 格式
         */
        String toMarkdown() {
            StringBuilder sb = new StringBuilder();
            
            // 表头
            sb.append("|");
            for (String header : headers) {
                sb.append(" ").append(header).append(" |");
            }
            sb.append("\n");
            
            // 分隔线
            sb.append("|");
            for (int i = 0; i < headers.size(); i++) {
                sb.append(" --- |");
            }
            sb.append("\n");
            
            // 数据行
            for (List<String> row : rows) {
                sb.append("|");
                for (String cell : row) {
                    sb.append(" ").append(cell != null ? cell : "").append(" |");
                }
                sb.append("\n");
            }
            
            return sb.toString();
        }
    }

    public ExtractionResult extract(String text, JSONObject config, boolean debug) {
        ExtractionResult result = new ExtractionResult();
        result.setSuccess(false);
        result.setDebugInfo(new ArrayList<>());

        try {
            // 获取提取模式
            String extractMode = getOrDefault(config, "extractMode", "cell");
            String headerPattern = config.getString("headerPattern");
            
            if (StrUtil.isBlank(headerPattern)) {
                return ExtractionResult.failure("表头特征不能为空");
            }

            if (debug) {
                result.addDebugInfo("提取模式: " + extractMode);
                result.addDebugInfo("表头特征: " + headerPattern);
            }

            // 解析所有 HTML 表格
            List<TableData> tables = parseAllTables(text, debug, result);
            
            if (tables.isEmpty()) {
                return ExtractionResult.failure("未找到任何表格");
            }

            if (debug) {
                result.addDebugInfo("找到 " + tables.size() + " 个表格");
            }

            // 根据表头特征查找目标表格
            TableData targetTable = findTableByHeader(tables, headerPattern, debug, result);
            
            if (targetTable == null) {
                return ExtractionResult.failure("未找到匹配表头特征的表格: " + headerPattern);
            }

            if (debug) {
                result.addDebugInfo("找到目标表格，表头: " + String.join(", ", targetTable.headers));
                result.addDebugInfo("数据行数: " + targetTable.rows.size());
            }

            // 根据模式进行提取
            if ("table".equals(extractMode)) {
                return extractWholeTable(targetTable, config, debug, result);
            } else {
                return extractCell(targetTable, config, debug, result);
            }

        } catch (Exception e) {
            log.error("表格提取失败", e);
            return ExtractionResult.failure("提取失败: " + e.getMessage());
        }
    }

    /**
     * 解析文本中的所有 HTML 表格
     */
    private List<TableData> parseAllTables(String text, boolean debug, ExtractionResult result) {
        List<TableData> tables = new ArrayList<>();
        
        // 使用正则提取所有 <table>...</table>
        Pattern tablePattern = Pattern.compile("<table[^>]*>(.*?)</table>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher tableMatcher = tablePattern.matcher(text);
        
        while (tableMatcher.find()) {
            String tableHtml = tableMatcher.group(0);
            int startPos = tableMatcher.start();
            int endPos = tableMatcher.end();
            
            if (debug) {
                result.addDebugInfo("找到表格HTML: " + tableHtml.substring(0, Math.min(100, tableHtml.length())) + "...");
                result.addDebugInfo("表格位置: " + startPos + " - " + endPos);
            }
            
            // 使用修复后的 HtmlTableParser 解析表格
            TableData tableData = parseTableWithHtmlParser(tableHtml, debug, result);
            if (tableData != null && !tableData.headers.isEmpty()) {
                // 设置表格位置信息
                tableData.startPosition = startPos;
                tableData.endPosition = endPos;
                
                tables.add(tableData);
                
                if (debug) {
                    result.addDebugInfo("解析表格成功，表头: " + String.join(", ", tableData.headers));
                    result.addDebugInfo("数据行数: " + tableData.rows.size());
                }
            }
        }
        
        return tables;
    }
    
    /**
     * 使用修复后的 HtmlTableParser 解析表格
     */
    private TableData parseTableWithHtmlParser(String tableHtml, boolean debug, ExtractionResult result) {
        try {
            // 使用修复后的 HtmlTableParser 解析
            List<List<String>> tableArray = HtmlTableParser.parseTableToArray(tableHtml);
            
            if (tableArray.isEmpty()) {
                if (debug) {
                    result.addDebugInfo("表格解析结果为空");
                }
                return null;
            }
            
            TableData tableData = new TableData();
            tableData.rawHtml = tableHtml;
            
            // 第一行作为表头
            tableData.headers = new ArrayList<>(tableArray.get(0));
            
            // 其余行作为数据
            for (int i = 1; i < tableArray.size(); i++) {
                tableData.rows.add(tableArray.get(i));
            }
            
            if (debug) {
                result.addDebugInfo("使用HtmlTableParser解析成功，表头: " + String.join(", ", tableData.headers));
                result.addDebugInfo("数据行数: " + tableData.rows.size());
            }
            
            return tableData;
            
        } catch (Exception e) {
            if (debug) {
                result.addDebugInfo("HtmlTableParser解析失败: " + e.getMessage());
            }
            log.warn("HtmlTableParser解析表格失败，回退到原始解析方法", e);
            // 回退到原始解析方法
            return parseTable(tableHtml);
        }
    }
    
    /**
     * 解析单个 HTML 表格（原始方法，作为备用）
     */
    private TableData parseTable(String tableHtml) {
        TableData table = new TableData();
        table.rawHtml = tableHtml;
        
        // 提取表头 (th 或第一个 tr)
        Pattern headerPattern = Pattern.compile("<th[^>]*>(.*?)</th>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher headerMatcher = headerPattern.matcher(tableHtml);
        
        while (headerMatcher.find()) {
            String cellContent = cleanHtmlTags(headerMatcher.group(1));
            table.headers.add(cellContent.trim());
        }
        
        // 如果没有 th，尝试从第一个 tr 提取表头
        if (table.headers.isEmpty()) {
            Pattern trPattern = Pattern.compile("<tr[^>]*>(.*?)</tr>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
            Matcher trMatcher = trPattern.matcher(tableHtml);
            
            if (trMatcher.find()) {
                String firstRow = trMatcher.group(1);
                Pattern tdPattern = Pattern.compile("<td[^>]*>(.*?)</td>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
                Matcher tdMatcher = tdPattern.matcher(firstRow);
                
                while (tdMatcher.find()) {
                    String cellContent = cleanHtmlTags(tdMatcher.group(1));
                    table.headers.add(cellContent.trim());
                }
            }
        }
        
        // 提取所有数据行
        Pattern trPattern = Pattern.compile("<tr[^>]*>(.*?)</tr>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher trMatcher = trPattern.matcher(tableHtml);
        
        boolean isFirstRow = true;
        while (trMatcher.find()) {
            String rowHtml = trMatcher.group(1);
            
            // 跳过表头行（如果有 th 标签）
            if (rowHtml.contains("<th")) {
                continue;
            }
            
            // 如果第一行被用作表头，跳过
            if (isFirstRow && !tableHtml.contains("<th")) {
                isFirstRow = false;
                continue;
            }
            
            List<String> row = new ArrayList<>();
            Pattern tdPattern = Pattern.compile("<td[^>]*>(.*?)</td>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
            Matcher tdMatcher = tdPattern.matcher(rowHtml);
            
            while (tdMatcher.find()) {
                String cellContent = cleanHtmlTags(tdMatcher.group(1));
                row.add(cellContent.trim());
            }
            
            if (!row.isEmpty()) {
                table.rows.add(row);
            }
        }
        
        return table;
    }
    
    /**
     * 清理 HTML 标签
     */
    private String cleanHtmlTags(String html) {
        if (html == null) {
            return "";
        }
        // 移除所有 HTML 标签
        String text = html.replaceAll("<[^>]+>", "");
        // 解码 HTML 实体
        text = text.replace("&nbsp;", " ")
                   .replace("&lt;", "<")
                   .replace("&gt;", ">")
                   .replace("&amp;", "&")
                   .replace("&quot;", "\"");
        return text;
    }
    
    /**
     * 根据表头特征查找表格
     */
    private TableData findTableByHeader(List<TableData> tables, String headerPattern, boolean debug, ExtractionResult result) {
        for (TableData table : tables) {
            if (debug) {
                result.addDebugInfo("检查表格，表头: " + String.join(", ", table.headers));
            }
            
            // 使用修复后的 HtmlTableParser 进行精确匹配
            boolean matched = HtmlTableParser.matchTableHeaderFeature(table.rawHtml, headerPattern, false);
            
            if (matched) {
                if (debug) {
                    result.addDebugInfo("✅ 表头特征匹配成功: " + headerPattern);
                    result.addDebugInfo("匹配的表格表头: " + String.join(", ", table.headers));
                }
                return table;
            } else {
                if (debug) {
                    result.addDebugInfo("❌ 表头特征不匹配: " + headerPattern);
                }
            }
        }
        
        if (debug) {
            result.addDebugInfo("未找到匹配表头特征的表格: " + headerPattern);
        }
        
        return null;
    }
    
    /**
     * 提取整个表格
     */
    private ExtractionResult extractWholeTable(TableData table, JSONObject config, boolean debug, ExtractionResult result) {
        String format = getOrDefault(config, "format", "json");
        
        String value;
        Object tableData;
        
        switch (format.toLowerCase()) {
            case "json":
                tableData = table.toJSON();
                value = JSON.toJSONString(tableData);
                result.setTableData(tableData);  // 存储结构化数据
                break;
                
            case "markdown":
                value = table.toMarkdown();
                result.setTableData(table.toJSON());
                break;
                
            case "html":
                value = table.rawHtml;
                result.setTableData(table.toJSON());
                break;
                
            default:
                value = JSON.toJSONString(table.toJSON());
                result.setTableData(table.toJSON());
        }
        
        result.setSuccess(true);
        result.setValue(value);
        result.setConfidence(95);
        
        // 设置表格的位置信息
        if (table.startPosition != -1 && table.endPosition != -1) {
            result.setStartPosition(table.startPosition);
            result.setEndPosition(table.endPosition);
        }
        
        if (debug) {
            result.addDebugInfo("提取整表成功，格式: " + format);
            result.addDebugInfo("行数: " + table.rows.size() + ", 列数: " + table.headers.size());
            if (table.startPosition != -1) {
                result.addDebugInfo("表格位置: " + table.startPosition + " - " + table.endPosition);
            }
            // 添加表格预览到调试信息
            result.addDebugInfo("表格预览:\n" + table.toMarkdown());
        }
        
        return result;
    }
    
    /**
     * 提取单个单元格
     */
    private ExtractionResult extractCell(TableData table, JSONObject config, boolean debug, ExtractionResult result) {
        String targetColumn = config.getString("targetColumn");
        String rowMarker = config.getString("rowMarker");
        Integer columnIndex = config.getInteger("columnIndex");
        Integer rowIndex = config.getInteger("rowIndex");
        Integer occurrence = getOrDefault(config, "occurrence", 1);
        
        // 确定列索引
        int colIndex = -1;
        if (StrUtil.isNotBlank(targetColumn)) {
            for (int i = 0; i < table.headers.size(); i++) {
                if (table.headers.get(i).contains(targetColumn)) {
                    colIndex = i;
                    break;
                }
            }
            if (colIndex == -1) {
                return ExtractionResult.failure("未找到目标列: " + targetColumn);
            }
        } else if (columnIndex != null && columnIndex > 0) {
            colIndex = columnIndex - 1;
        } else {
            return ExtractionResult.failure("未指定目标列");
        }
        
        if (debug) {
            result.addDebugInfo("目标列索引: " + colIndex + " (" + table.headers.get(colIndex) + ")");
        }
        
        // 提取单元格值
        List<String> allMatches = new ArrayList<>();
        String extracted = null;
        
        if (rowIndex != null && rowIndex > 0) {
            // 直接按行索引提取
            if (rowIndex <= table.rows.size()) {
                List<String> row = table.rows.get(rowIndex - 1);
                if (colIndex < row.size()) {
                    extracted = row.get(colIndex);
                }
            }
        } else if (StrUtil.isNotBlank(rowMarker)) {
            // 按行标记提取
            for (List<String> row : table.rows) {
                String rowText = String.join(" ", row);
                if (rowText.contains(rowMarker)) {
                    if (colIndex < row.size()) {
                        extracted = row.get(colIndex);
                    }
                    break;
                }
            }
        } else {
            // 提取列中所有值
            for (List<String> row : table.rows) {
                if (colIndex < row.size()) {
                    String cellValue = row.get(colIndex);
                    if (StrUtil.isNotBlank(cellValue)) {
                        allMatches.add(cellValue);
                    }
                }
            }
            
            if (occurrence > 0 && occurrence <= allMatches.size()) {
                extracted = allMatches.get(occurrence - 1);
            }
        }
        
        if (extracted == null || extracted.trim().isEmpty()) {
            return ExtractionResult.failure("未提取到单元格内容");
        }
        
        result.setSuccess(true);
        result.setValue(extracted.trim());
        result.setConfidence(90);
        
        // 设置表格的位置信息（单元格抽取使用整个表格的位置）
        if (table.startPosition != -1 && table.endPosition != -1) {
            result.setStartPosition(table.startPosition);
            result.setEndPosition(table.endPosition);
        }
        
        if (debug) {
            result.addDebugInfo("提取单元格成功: " + extracted.trim());
            if (table.startPosition != -1) {
                result.addDebugInfo("表格位置: " + table.startPosition + " - " + table.endPosition);
            }
            // 添加表格预览
            result.addDebugInfo("表格预览:\n" + table.toMarkdown());
        }
        
        return result;
    }
    
    /**
     * 获取配置值或默认值
     */
    @SuppressWarnings("unchecked")
    private <T> T getOrDefault(JSONObject config, String key, T defaultValue) {
        if (config == null) {
            return defaultValue;
        }

        Object value = config.get(key);
        if (value == null) {
            return defaultValue;
        }

        try {
            return (T) value;
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }
}

