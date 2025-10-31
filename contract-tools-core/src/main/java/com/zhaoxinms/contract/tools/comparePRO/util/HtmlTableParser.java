package com.zhaoxinms.contract.tools.comparePRO.util;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTML表格解析工具类
 * 
 * 用于规则抽取场景下解析MinerU返回的HTML表格
 * 
 * @author zhaoxin
 * @since 2025-10-13
 */
@Slf4j
public class HtmlTableParser {
    
    /**
     * 解析HTML表格为二维数组
     * 
     * @param htmlTable HTML表格字符串
     * @return 二维数组，每个元素是一个单元格的文本内容
     */
    public static List<List<String>> parseTableToArray(String htmlTable) {
        List<List<String>> result = new ArrayList<>();
        
        if (htmlTable == null || htmlTable.trim().isEmpty()) {
            return result;
        }
        
        try {
            // 提取所有的 <tr>...</tr> 行
            Pattern rowPattern = Pattern.compile("<tr[^>]*>(.*?)</tr>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Matcher rowMatcher = rowPattern.matcher(htmlTable);
            
            while (rowMatcher.find()) {
                String rowHtml = rowMatcher.group(1);
                List<String> row = new ArrayList<>();
                
                // 提取行内的所有 <td> 或 <th> 单元格
                Pattern cellPattern = Pattern.compile("<(td|th)[^>]*>(.*?)</(td|th)>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
                Matcher cellMatcher = cellPattern.matcher(rowHtml);
                
                while (cellMatcher.find()) {
                    String cellContent = cellMatcher.group(2);  // 第2个组是内容
                    // 去除HTML标签，保留文本
                    String cleanText = removeHtmlTags(cellContent).trim();
                    row.add(cleanText);
                }
                
                if (!row.isEmpty()) {
                    result.add(row);
                }
            }
            
            log.debug("解析HTML表格完成: 共{}行", result.size());
        } catch (Exception e) {
            log.error("解析HTML表格失败: {}", e.getMessage(), e);
        }
        
        return result;
    }
    
    /**
     * 解析HTML表格为Map格式
     * 
     * Map格式：
     * {
     *   "headers": ["列1", "列2", "列3"],  // 第一行作为表头（可选）
     *   "rows": [
     *     ["值1", "值2", "值3"],
     *     ["值4", "值5", "值6"]
     *   ],
     *   "totalRows": 3,
     *   "totalColumns": 3
     * }
     * 
     * @param htmlTable HTML表格字符串
     * @param hasHeader 第一行是否为表头
     * @return Map对象
     */
    public static Map<String, Object> parseTableToMap(String htmlTable, boolean hasHeader) {
        Map<String, Object> result = new LinkedHashMap<>();
        
        List<List<String>> tableArray = parseTableToArray(htmlTable);
        
        if (tableArray.isEmpty()) {
            result.put("headers", new ArrayList<String>());
            result.put("rows", new ArrayList<List<String>>());
            result.put("totalRows", 0);
            result.put("totalColumns", 0);
            return result;
        }
        
        List<String> headers = new ArrayList<>();
        List<List<String>> rows = new ArrayList<>();
        
        int startRow = 0;
        
        // 处理表头
        if (hasHeader && !tableArray.isEmpty()) {
            List<String> headerRow = tableArray.get(0);
            headers.addAll(headerRow);
            startRow = 1;
        } else {
            // 如果没有表头，使用列索引作为表头
            int columnCount = tableArray.get(0).size();
            for (int i = 0; i < columnCount; i++) {
                headers.add("列" + (i + 1));
            }
        }
        
        // 处理数据行
        for (int i = startRow; i < tableArray.size(); i++) {
            List<String> rowData = tableArray.get(i);
            rows.add(new ArrayList<>(rowData));
        }
        
        result.put("headers", headers);
        result.put("rows", rows);
        result.put("totalRows", tableArray.size());
        result.put("totalColumns", headers.size());
        
        return result;
    }
    
    /**
     * 获取指定单元格的值
     * 
     * @param htmlTable HTML表格字符串
     * @param row 行索引（从0开始）
     * @param col 列索引（从0开始）
     * @return 单元格内容，如果不存在返回null
     */
    public static String getCellValue(String htmlTable, int row, int col) {
        List<List<String>> tableArray = parseTableToArray(htmlTable);
        
        if (row < 0 || row >= tableArray.size()) {
            log.warn("行索引超出范围: row={}, totalRows={}", row, tableArray.size());
            return null;
        }
        
        List<String> rowData = tableArray.get(row);
        
        if (col < 0 || col >= rowData.size()) {
            log.warn("列索引超出范围: col={}, totalColumns={}", col, rowData.size());
            return null;
        }
        
        return rowData.get(col);
    }
    
    /**
     * 根据表头名称获取列的所有值
     * 
     * @param htmlTable HTML表格字符串
     * @param headerName 表头名称
     * @return 该列的所有值（不包括表头）
     */
    public static List<String> getColumnByHeader(String htmlTable, String headerName) {
        List<String> result = new ArrayList<>();
        
        List<List<String>> tableArray = parseTableToArray(htmlTable);
        
        if (tableArray.isEmpty()) {
            return result;
        }
        
        // 第一行作为表头
        List<String> headers = tableArray.get(0);
        
        // 查找表头索引
        int colIndex = -1;
        for (int i = 0; i < headers.size(); i++) {
            if (headerName.equals(headers.get(i)) || headerName.trim().equals(headers.get(i).trim())) {
                colIndex = i;
                break;
            }
        }
        
        if (colIndex == -1) {
            log.warn("未找到表头: {}", headerName);
            return result;
        }
        
        // 提取该列的所有值
        for (int i = 1; i < tableArray.size(); i++) {
            List<String> row = tableArray.get(i);
            if (colIndex < row.size()) {
                result.add(row.get(colIndex));
            } else {
                result.add("");
            }
        }
        
        return result;
    }
    
    /**
     * 根据关键字查找行，返回整行数据
     * 
     * @param htmlTable HTML表格字符串
     * @param keyword 关键字
     * @param columnIndex 在哪一列查找（-1表示所有列）
     * @return 包含关键字的行数据列表
     */
    public static List<List<String>> findRowsByKeyword(String htmlTable, String keyword, int columnIndex) {
        List<List<String>> result = new ArrayList<>();
        
        List<List<String>> tableArray = parseTableToArray(htmlTable);
        
        for (List<String> row : tableArray) {
            boolean found = false;
            
            if (columnIndex == -1) {
                // 在所有列中查找
                for (String cell : row) {
                    if (cell != null && cell.contains(keyword)) {
                        found = true;
                        break;
                    }
                }
            } else {
                // 在指定列中查找
                if (columnIndex >= 0 && columnIndex < row.size()) {
                    String cell = row.get(columnIndex);
                    if (cell != null && cell.contains(keyword)) {
                        found = true;
                    }
                }
            }
            
            if (found) {
                result.add(new ArrayList<>(row));
            }
        }
        
        return result;
    }
    
    /**
     * 去除HTML标签
     * 
     * @param html HTML字符串
     * @return 纯文本
     */
    private static String removeHtmlTags(String html) {
        if (html == null || html.trim().isEmpty()) {
            return "";
        }
        
        // 1. 替换 <br>、<br/>、</tr> 为换行符
        String text = html.replaceAll("(?i)<br\\s*/?>", "\n");
        
        // 2. 移除所有其他HTML标签
        text = text.replaceAll("<[^>]+>", "");
        
        // 3. 解码HTML实体
        text = text.replace("&nbsp;", " ");
        text = text.replace("&lt;", "<");
        text = text.replace("&gt;", ">");
        text = text.replace("&amp;", "&");
        text = text.replace("&quot;", "\"");
        text = text.replace("&apos;", "'");
        
        // 4. 清理多余的空白
        text = text.replaceAll("[ \\t]+", " ");
        text = text.replaceAll("\\n\\s*\\n", "\n");
        
        return text.trim();
    }
    
    /**
     * 获取表格的行数和列数
     * 
     * @param htmlTable HTML表格字符串
     * @return Map对象，包含 rowCount 和 columnCount
     */
    public static Map<String, Integer> getTableDimensions(String htmlTable) {
        Map<String, Integer> result = new LinkedHashMap<>();
        
        List<List<String>> tableArray = parseTableToArray(htmlTable);
        
        int rowCount = tableArray.size();
        int columnCount = 0;
        
        if (!tableArray.isEmpty()) {
            // 取第一行的列数
            columnCount = tableArray.get(0).size();
        }
        
        result.put("rowCount", rowCount);
        result.put("columnCount", columnCount);
        
        return result;
    }
    
    /**
     * 检查表格的表头是否匹配指定的特征
     * 
     * 支持两种匹配模式：
     * 1. 包含匹配：表头包含所有指定的列名即可（顺序不限）
     * 2. 顺序匹配：表头按照指定顺序包含这些列名
     * 
     * @param htmlTable HTML表格字符串
     * @param headerFeature 表头特征，使用"|"分隔多个列名，例如："序号|货物名称|规格型号|产地"
     * @param strictOrder 是否严格按顺序匹配（true=顺序匹配，false=包含匹配）
     * @return 是否匹配
     */
    public static boolean matchTableHeaderFeature(String htmlTable, String headerFeature, boolean strictOrder) {
        if (htmlTable == null || htmlTable.trim().isEmpty() || headerFeature == null || headerFeature.trim().isEmpty()) {
            return false;
        }
        
        List<List<String>> tableArray = parseTableToArray(htmlTable);
        
        if (tableArray.isEmpty()) {
            return false;
        }
        
        // 获取表头（第一行）
        List<String> headers = tableArray.get(0);
        
        // 解析特征列名
        String[] featureColumns = headerFeature.split("\\|");
        
        if (strictOrder) {
            // 顺序匹配模式：检查特征列是否按顺序出现
            return matchHeadersInOrder(headers, featureColumns);
        } else {
            // 包含匹配模式：检查表头是否包含所有特征列
            return matchHeadersContains(headers, featureColumns);
        }
    }
    
    /**
     * 检查表头是否包含所有特征列（顺序不限）
     * 支持多级容错匹配：
     * 1. 完全匹配
     * 2. 忽略空格匹配
     * 3. 忽略全角/半角括号匹配
     * 4. 部分包含匹配
     */
    private static boolean matchHeadersContains(List<String> headers, String[] featureColumns) {
        for (String featureColumn : featureColumns) {
            String trimmedFeature = featureColumn.trim();
            boolean found = false;
            String matchType = null;
            
            for (String header : headers) {
                String trimmedHeader = header.trim();
                
                // 1. 完全匹配（精确匹配）
                if (trimmedHeader.equals(trimmedFeature)) {
                    found = true;
                    matchType = "完全匹配";
                    break;
                }
                
                // 2. 忽略空格匹配
                String headerNoSpace = trimmedHeader.replaceAll("\\s+", "");
                String featureNoSpace = trimmedFeature.replaceAll("\\s+", "");
                if (headerNoSpace.equals(featureNoSpace)) {
                    found = true;
                    matchType = "忽略空格匹配";
                    log.debug("列 '{}' 通过忽略空格匹配到 '{}'", trimmedFeature, trimmedHeader);
                    break;
                }
                
                // 3. 忽略全角/半角括号匹配
                String headerNormalized = normalizeBrackets(trimmedHeader);
                String featureNormalized = normalizeBrackets(trimmedFeature);
                if (headerNormalized.equals(featureNormalized)) {
                    found = true;
                    matchType = "忽略括号差异匹配";
                    log.debug("列 '{}' 通过忽略括号差异匹配到 '{}'", trimmedFeature, trimmedHeader);
                    break;
                }
                
                // 4. 部分包含匹配（表头包含特征列名，或特征列名包含表头）
                if (trimmedHeader.contains(trimmedFeature) || trimmedFeature.contains(trimmedHeader)) {
                    // 额外检查：避免太短的字符串导致误匹配
                    int minLen = Math.min(trimmedHeader.length(), trimmedFeature.length());
                    if (minLen >= 2) {  // 至少2个字符才认为是有效匹配
                        found = true;
                        matchType = "部分包含匹配";
                        log.debug("列 '{}' 通过部分包含匹配到 '{}'", trimmedFeature, trimmedHeader);
                        break;
                    }
                }
            }
            
            if (!found) {
                log.debug("表头特征匹配失败：未找到列 '{}'", trimmedFeature);
                log.debug("实际表头: {}", String.join(", ", headers));
                return false;
            } else if (matchType != null && !matchType.equals("完全匹配")) {
                log.info("表头列 '{}' 使用容错匹配成功（{}）", trimmedFeature, matchType);
            }
        }
        
        log.debug("表头特征匹配成功（包含模式）");
        return true;
    }
    
    /**
     * 标准化括号（统一全角和半角括号）
     */
    private static String normalizeBrackets(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("[（(]", "(")
                   .replaceAll("[）)]", ")")
                   .replaceAll("[【\\[]", "[")
                   .replaceAll("[】\\]]", "]")
                   .replaceAll("[｛{]", "{")
                   .replaceAll("[｝}]", "}");
    }
    
    /**
     * 检查特征列是否按顺序出现在表头中
     * 支持多级容错匹配：
     * 1. 完全匹配
     * 2. 忽略空格匹配
     * 3. 忽略全角/半角括号匹配
     * 4. 部分包含匹配
     */
    private static boolean matchHeadersInOrder(List<String> headers, String[] featureColumns) {
        int lastIndex = -1;
        
        for (String featureColumn : featureColumns) {
            String trimmedFeature = featureColumn.trim();
            boolean found = false;
            String matchType = null;
            
            for (int i = lastIndex + 1; i < headers.size(); i++) {
                String trimmedHeader = headers.get(i).trim();
                
                // 1. 完全匹配（精确匹配）
                if (trimmedHeader.equals(trimmedFeature)) {
                    lastIndex = i;
                    found = true;
                    matchType = "完全匹配";
                    break;
                }
                
                // 2. 忽略空格匹配
                String headerNoSpace = trimmedHeader.replaceAll("\\s+", "");
                String featureNoSpace = trimmedFeature.replaceAll("\\s+", "");
                if (headerNoSpace.equals(featureNoSpace)) {
                    lastIndex = i;
                    found = true;
                    matchType = "忽略空格匹配";
                    log.debug("列 '{}' 通过忽略空格匹配到 '{}'（顺序模式）", trimmedFeature, trimmedHeader);
                    break;
                }
                
                // 3. 忽略全角/半角括号匹配
                String headerNormalized = normalizeBrackets(trimmedHeader);
                String featureNormalized = normalizeBrackets(trimmedFeature);
                if (headerNormalized.equals(featureNormalized)) {
                    lastIndex = i;
                    found = true;
                    matchType = "忽略括号差异匹配";
                    log.debug("列 '{}' 通过忽略括号差异匹配到 '{}'（顺序模式）", trimmedFeature, trimmedHeader);
                    break;
                }
                
                // 4. 部分包含匹配
                if (trimmedHeader.contains(trimmedFeature) || trimmedFeature.contains(trimmedHeader)) {
                    int minLen = Math.min(trimmedHeader.length(), trimmedFeature.length());
                    if (minLen >= 2) {
                        lastIndex = i;
                        found = true;
                        matchType = "部分包含匹配";
                        log.debug("列 '{}' 通过部分包含匹配到 '{}'（顺序模式）", trimmedFeature, trimmedHeader);
                        break;
                    }
                }
            }
            
            if (!found) {
                log.debug("表头特征匹配失败（顺序模式）：未找到列 '{}' 或顺序不符", trimmedFeature);
                log.debug("实际表头: {}", String.join(", ", headers));
                return false;
            } else if (matchType != null && !matchType.equals("完全匹配")) {
                log.info("表头列 '{}' 使用容错匹配成功（{}，顺序模式）", trimmedFeature, matchType);
            }
        }
        
        log.debug("表头特征匹配成功（顺序模式）");
        return true;
    }
    
    /**
     * 获取表格的表头（第一行）
     * 
     * @param htmlTable HTML表格字符串
     * @return 表头列名列表
     */
    public static List<String> getTableHeaders(String htmlTable) {
        if (htmlTable == null || htmlTable.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        List<List<String>> tableArray = parseTableToArray(htmlTable);
        
        if (tableArray.isEmpty()) {
            return Collections.emptyList();
        }
        
        return tableArray.get(0); 
    } 
}

