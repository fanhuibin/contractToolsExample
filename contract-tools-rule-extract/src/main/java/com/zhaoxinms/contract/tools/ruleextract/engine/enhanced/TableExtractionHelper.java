package com.zhaoxinms.contract.tools.ruleextract.engine.enhanced;

import com.zhaoxinms.contract.tools.comparePRO.util.HtmlTableParser;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 表格抽取辅助类
 * 
 * 用于规则抽取引擎中的表格数据处理 
 * 
 * @author zhaoxin
 * @since 2025-10-13
 */
@Slf4j
public class TableExtractionHelper {
    
    /**
     * 从HTML表格中抽取单元格值
     * 
     * @param htmlTable HTML表格字符串
     * @param row 行索引（从0开始）
     * @param col 列索引（从0开始）
     * @return 单元格内容
     */
    public static String extractCellValue(String htmlTable, int row, int col) {
        if (htmlTable == null || htmlTable.trim().isEmpty()) {
            return null;
        }
        
        String value = HtmlTableParser.getCellValue(htmlTable, row, col);
        log.debug("提取单元格值: row={}, col={}, value={}", row, col, value);
        return value;
    }
    
    /**
     * 根据表头名称抽取列的所有值
     * 
     * @param htmlTable HTML表格字符串
     * @param headerName 表头名称
     * @return 该列的所有值
     */
    public static List<String> extractColumnByHeader(String htmlTable, String headerName) {
        if (htmlTable == null || htmlTable.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        List<String> values = HtmlTableParser.getColumnByHeader(htmlTable, headerName);
        log.debug("根据表头提取列值: header={}, count={}", headerName, values.size());
        return values;
    }
    
    /**
     * 根据关键字查找行，返回指定列的值
     * 
     * @param htmlTable HTML表格字符串
     * @param keyword 关键字
     * @param searchColumnIndex 在哪一列查找（-1表示所有列）
     * @param resultColumnIndex 返回哪一列的值（-1表示返回整行）
     * @return 找到的值列表
     */
    public static List<String> extractByKeyword(String htmlTable, String keyword, int searchColumnIndex, int resultColumnIndex) {
        if (htmlTable == null || htmlTable.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        List<List<String>> matchedRows = HtmlTableParser.findRowsByKeyword(htmlTable, keyword, searchColumnIndex);
        
        if (resultColumnIndex == -1) {
            // 返回整行（拼接）
            return matchedRows.stream()
                    .map(row -> String.join(" | ", row))
                    .collect(Collectors.toList());
        } else {
            // 返回指定列的值
            return matchedRows.stream()
                    .filter(row -> resultColumnIndex < row.size())
                    .map(row -> row.get(resultColumnIndex))
                    .collect(Collectors.toList());
        }
    }
    
    /**
     * 将HTML表格转换为Map格式（方便JSON序列化）
     * 
     * @param htmlTable HTML表格字符串
     * @param hasHeader 第一行是否为表头
     * @return Map格式的表格数据
     */
    public static Map<String, Object> convertTableToMap(String htmlTable, boolean hasHeader) {
        if (htmlTable == null || htmlTable.trim().isEmpty()) {
            Map<String, Object> emptyResult = new HashMap<>();
            emptyResult.put("headers", Collections.emptyList());
            emptyResult.put("rows", Collections.emptyList());
            emptyResult.put("totalRows", 0); 
            emptyResult.put("totalColumns", 0); 
            return emptyResult;
        }
        
        return HtmlTableParser.parseTableToMap(htmlTable, hasHeader);
    }
    
    /**
     * 从表格中抽取键值对
     * 
     * 适用于两列表格，第一列是键，第二列是值
     * 
     * @param htmlTable HTML表格字符串
     * @param keyColumnIndex 键所在列索引
     * @param valueColumnIndex 值所在列索引
     * @param skipFirstRow 是否跳过第一行（表头）
     * @return 键值对Map
     */
    public static Map<String, String> extractKeyValuePairs(String htmlTable, int keyColumnIndex, int valueColumnIndex, boolean skipFirstRow) {
        Map<String, String> result = new HashMap<>();
        
        if (htmlTable == null || htmlTable.trim().isEmpty()) {
            return result;
        }
        
        List<List<String>> tableArray = HtmlTableParser.parseTableToArray(htmlTable);
        
        int startRow = skipFirstRow ? 1 : 0;
        
        for (int i = startRow; i < tableArray.size(); i++) {
            List<String> row = tableArray.get(i);
            
            if (keyColumnIndex < row.size() && valueColumnIndex < row.size()) {
                String key = row.get(keyColumnIndex);
                String value = row.get(valueColumnIndex);
                
                if (key != null && !key.trim().isEmpty()) {
                    result.put(key.trim(), value != null ? value.trim() : "");
                }
            }
        }
        
        log.debug("提取键值对: count={}", result.size());
        return result;
    }
    
    /**
     * 检查表格是否包含指定关键字
     * 
     * @param htmlTable HTML表格字符串
     * @param keyword 关键字
     * @return 是否包含
     */
    public static boolean containsKeyword(String htmlTable, String keyword) {
        if (htmlTable == null || htmlTable.trim().isEmpty()) {
            return false;
        }
        
        List<List<String>> tableArray = HtmlTableParser.parseTableToArray(htmlTable);
        
        for (List<String> row : tableArray) {
            for (String cell : row) {
                if (cell != null && cell.contains(keyword)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * 获取表格的维度信息
     * 
     * @param htmlTable HTML表格字符串
     * @return Map包含 rowCount 和 columnCount
     */
    public static Map<String, Integer> getTableDimensions(String htmlTable) {
        if (htmlTable == null || htmlTable.trim().isEmpty()) {
            Map<String, Integer> result = new HashMap<>();
            result.put("rowCount", 0);
            result.put("columnCount", 0);
            return result;
        }
        
        return HtmlTableParser.getTableDimensions(htmlTable);
    }
    
    /**
     * 检查表格的表头是否匹配指定的特征
     * 
     * 用于识别特定类型的表格
     *  
     * @param htmlTable HTML表格字符串
     * @param headerFeature 表头特征，使用"|"分隔多个列名，例如："序号|货物名称|规格型号|产地"
     * @param strictOrder 是否严格按顺序匹配（true=顺序匹配，false=包含匹配）
     * @return 是否匹配
     */
    public static boolean matchTableHeaderFeature(String htmlTable, String headerFeature, boolean strictOrder) {
        if (htmlTable == null || htmlTable.trim().isEmpty()) {
            return false;
        }
        
        boolean matched = HtmlTableParser.matchTableHeaderFeature(htmlTable, headerFeature, strictOrder);
        log.debug("表头特征匹配结果: feature={}, strictOrder={}, matched={}", headerFeature, strictOrder, matched);
        return matched;
    }
    
    /**
     * 获取表格的表头列名
     * 
     * @param htmlTable HTML表格字符串
     * @return 表头列名列表
     */
    public static List<String> getTableHeaders(String htmlTable) {
        if (htmlTable == null || htmlTable.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        return HtmlTableParser.getTableHeaders(htmlTable);
    }
}

