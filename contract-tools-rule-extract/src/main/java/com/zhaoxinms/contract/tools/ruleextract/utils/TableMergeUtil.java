package com.zhaoxinms.contract.tools.ruleextract.utils;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 跨页表格合并工具类
 * 用于合并MinerU识别的跨页表格（MinerU未识别为同一表格的情况）
 * 
 * @author zhaoxin
 * @since 2025-10-22
 */
@Slf4j
public class TableMergeUtil {
    
    // 允许的中间类型（除了表格之外，只能有这些类型在两个表格之间）
    private static final Set<String> ALLOWED_MIDDLE_TYPES = new HashSet<>(Arrays.asList(
        "page_number", "header", "footer"
    ));
    
    // 数据类型识别正则
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("^[¥￥$]?\\s*[\\d,]+\\.?\\d*\\s*$");
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}[-/年]\\d{1,2}[-/月]\\d{1,2}[日]?$");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("^\\d+$");
    private static final Pattern PERCENTAGE_PATTERN = Pattern.compile("^\\d+\\.?\\d*%$");
    
    /**
     * 合并跨页表格
     * 
     * @param contentList MinerU的content_list数据
     * @return 合并后的content_list（会删除被合并的表格）
     */
    public static JSONArray mergeCrossPageTables(JSONArray contentList) {
        if (contentList == null || contentList.size() == 0) {
            return contentList;
        }
        
        log.info("🔄 开始检查跨页表格合并，共{}个内容项", contentList.size());
        
        JSONArray result = new JSONArray();
        int mergeCount = 0;
        int tableCount = 0;
        
        for (int i = 0; i < contentList.size(); i++) {
            JSONObject current = contentList.getJSONObject(i);
            
            // 如果当前项是表格，检查是否可以与下一个表格合并
            if ("table".equals(current.getString("type"))) {
                tableCount++;
                log.debug("📋 发现表格#{}: 页{}, 索引{}", tableCount, current.getInteger("page_idx") + 1, i);
                
                JSONObject nextTable = findNextTable(contentList, i);
                
                if (nextTable != null) {
                    int nextTableIndex = contentList.indexOf(nextTable);
                    log.debug("  → 找到下一个表格: 页{}, 索引{}, 中间距离{}", 
                        nextTable.getInteger("page_idx") + 1, nextTableIndex, nextTableIndex - i);
                    
                    // 检查是否可以合并
                    if (canMergeTables(current, nextTable, contentList, i, nextTableIndex)) {
                        log.info("✅ 合并跨页表格: 页{} -> 页{}", 
                            current.getInteger("page_idx") + 1,
                            nextTable.getInteger("page_idx") + 1);
                        
                        // 合并表格
                        JSONObject mergedTable = mergeTwoTables(current, nextTable);
                        result.add(mergedTable);
                        mergeCount++;
                        
                        // 跳过中间的所有项（包括下一个表格）
                        i = nextTableIndex;
                        continue;
                    } else {
                        log.debug("  ✗ 不满足合并条件");
                    }
                } else {
                    log.debug("  → 没有后续表格");
                }
            }
            
            result.add(current);
        }
        
        log.info("📊 表格检查完成: 共{}个表格，成功合并{}对", tableCount, mergeCount);
        
        return result;
    }
    
    /**
     * 查找下一个表格
     */
    private static JSONObject findNextTable(JSONArray contentList, int currentIndex) {
        for (int i = currentIndex + 1; i < contentList.size(); i++) {
            JSONObject item = contentList.getJSONObject(i);
            if ("table".equals(item.getString("type"))) {
                return item;
            }
        }
        return null;
    }
    
    /**
     * 判断两个表格是否可以合并
     * 
     * 规则：
     * 1. 两个表格中间只能有page_number、header、footer类型的内容
     * 2. 两个表格的列结构相同
     * 3. 后一个表格的第一行数据格式与前一个表格的最后一行格式相似
     */
    private static boolean canMergeTables(JSONObject table1, JSONObject table2, 
                                         JSONArray contentList, int index1, int index2) {
        // 规则1：检查中间的内容类型
        if (!checkMiddleContent(contentList, index1, index2)) {
            return false;
        }
        
        // 解析表格HTML
        List<List<String>> rows1 = parseTableHtml(table1.getString("table_body"));
        List<List<String>> rows2 = parseTableHtml(table2.getString("table_body"));
        
        if (rows1.isEmpty() || rows2.isEmpty()) {
            log.debug("❌ 表格为空，无法合并");
            return false;
        }
        
        // 规则2：检查列数是否相同
        int colCount1 = rows1.get(0).size();
        int colCount2 = rows2.get(0).size();
        
        if (colCount1 != colCount2) {
            log.debug("  ❌ 列数不同，无法合并: table1={}, table2={}", colCount1, colCount2);
            return false;
        }
        
        // 规则3：检查数据格式是否相似
        List<String> lastRow1 = rows1.get(rows1.size() - 1);
        List<String> firstRow2 = rows2.get(0);
        
        log.debug("  检查行相似度:");
        log.debug("    - 表1最后一行: {}", lastRow1);
        log.debug("    - 表2第一行: {}", firstRow2);
        
        if (!checkRowSimilarity(lastRow1, firstRow2)) {
            log.debug("  ❌ 行数据格式不相似，无法合并");
            return false;
        }
        
        log.debug("  ✅ 表格可以合并: 列数={}, 页码={}->{}", 
            colCount1, 
            table1.getInteger("page_idx") + 1,
            table2.getInteger("page_idx") + 1);
        
        return true;
    }
    
    /**
     * 检查两个表格之间的内容
     */
    private static boolean checkMiddleContent(JSONArray contentList, int startIndex, int endIndex) {
        for (int i = startIndex + 1; i < endIndex; i++) {
            JSONObject item = contentList.getJSONObject(i);
            String type = item.getString("type");
            
            if (!ALLOWED_MIDDLE_TYPES.contains(type)) {
                log.debug("❌ 表格中间存在其他类型内容: {}", type);
                return false;
            }
        }
        return true;
    }
    
    /**
     * 解析表格HTML，提取行数据
     */
    private static List<List<String>> parseTableHtml(String tableHtml) {
        List<List<String>> rows = new ArrayList<>();
        
        try {
            Document doc = Jsoup.parse(tableHtml);
            Elements trElements = doc.select("tr");
            
            for (Element tr : trElements) {
                Elements tdElements = tr.select("td, th");
                List<String> row = new ArrayList<>();
                
                for (Element td : tdElements) {
                    String text = td.text().trim();
                    row.add(text);
                }
                
                if (!row.isEmpty()) {
                    rows.add(row);
                }
            }
        } catch (Exception e) {
            log.error("解析表格HTML失败", e);
        }
        
        return rows;
    }
    
    /**
     * 检查两行数据的格式是否相似
     * 
     * 比较每一列的数据类型（金额、日期、数字、百分比、文本等）
     */
    private static boolean checkRowSimilarity(List<String> row1, List<String> row2) {
        if (row1.size() != row2.size()) {
            log.debug("    行长度不同: {} vs {}", row1.size(), row2.size());
            return false;
        }
        
        int matchCount = 0;
        int totalColumns = row1.size();
        
        for (int i = 0; i < totalColumns; i++) {
            String cell1 = row1.get(i);
            String cell2 = row2.get(i);
            
            String type1 = detectDataType(cell1);
            String type2 = detectDataType(cell2);
            
            log.debug("    列{}: [{}]({}) vs [{}]({})", i, cell1, type1, cell2, type2);
            
            if (type1.equals(type2)) {
                matchCount++;
            }
        }
        
        // 至少80%的列数据类型相同
        double similarity = (double) matchCount / totalColumns;
        boolean isSimilar = similarity >= 0.8;
        
        log.debug("    📊 行相似度: {}/{} = {:.2f}%, 阈值80%: {}", 
            matchCount, totalColumns, similarity * 100, isSimilar ? "通过" : "未通过");
        
        return isSimilar;
    }
    
    /**
     * 检测单元格数据类型
     */
    private static String detectDataType(String cell) {
        if (cell == null || cell.trim().isEmpty()) {
            return "EMPTY";
        }
        
        cell = cell.trim();
        
        // 金额格式
        if (AMOUNT_PATTERN.matcher(cell).matches()) {
            return "AMOUNT";
        }
        
        // 日期格式
        if (DATE_PATTERN.matcher(cell).matches()) {
            return "DATE";
        }
        
        // 百分比格式
        if (PERCENTAGE_PATTERN.matcher(cell).matches()) {
            return "PERCENTAGE";
        }
        
        // 纯数字
        if (NUMBER_PATTERN.matcher(cell).matches()) {
            return "NUMBER";
        }
        
        // 其他文本
        return "TEXT";
    }
    
    /**
     * 合并两个表格
     * 将table2的内容合并到table1中，创建一个新的合并表格
     */
    private static JSONObject mergeTwoTables(JSONObject table1, JSONObject table2) {
        JSONObject merged = new JSONObject();
        
        // 复制第一个表格的基本信息
        merged.put("type", "table");
        merged.put("page_idx", table1.getInteger("page_idx")); // 使用第一个表格的页码
        merged.put("img_path", table1.getString("img_path")); // 使用第一个表格的图片
        
        // 合并bbox（取两个表格bbox的并集）
        JSONArray bbox1 = table1.getJSONArray("bbox");
        JSONArray bbox2 = table2.getJSONArray("bbox");
        JSONArray mergedBbox = mergeBbox(bbox1, bbox2);
        merged.put("bbox", mergedBbox);
        
        // 合并表格标题
        JSONArray caption1 = table1.getJSONArray("table_caption");
        JSONArray caption2 = table2.getJSONArray("table_caption");
        JSONArray mergedCaption = new JSONArray();
        if (caption1 != null && !caption1.isEmpty()) {
            mergedCaption.addAll(caption1);
        }
        if (caption2 != null && !caption2.isEmpty()) {
            mergedCaption.addAll(caption2);
        }
        merged.put("table_caption", mergedCaption);
        
        // 合并表格注释
        JSONArray footnote1 = table1.getJSONArray("table_footnote");
        JSONArray footnote2 = table2.getJSONArray("table_footnote");
        JSONArray mergedFootnote = new JSONArray();
        if (footnote1 != null && !footnote1.isEmpty()) {
            mergedFootnote.addAll(footnote1);
        }
        if (footnote2 != null && !footnote2.isEmpty()) {
            mergedFootnote.addAll(footnote2);
        }
        merged.put("table_footnote", mergedFootnote);
        
        // 合并表格内容（HTML）
        String mergedTableBody = mergeTableBodies(
            table1.getString("table_body"),
            table2.getString("table_body")
        );
        merged.put("table_body", mergedTableBody);
        
        // 添加合并标记
        merged.put("_merged", true);
        merged.put("_merged_from_pages", Arrays.asList(
            table1.getInteger("page_idx"),
            table2.getInteger("page_idx")
        ));
        
        return merged;
    }
    
    /**
     * 合并两个bbox（取并集）
     */
    private static JSONArray mergeBbox(JSONArray bbox1, JSONArray bbox2) {
        if (bbox1 == null || bbox1.size() < 4) return bbox2;
        if (bbox2 == null || bbox2.size() < 4) return bbox1;
        
        // bbox格式: [x1, y1, x2, y2]
        double x1 = Math.min(bbox1.getDoubleValue(0), bbox2.getDoubleValue(0));
        double y1 = Math.min(bbox1.getDoubleValue(1), bbox2.getDoubleValue(1));
        double x2 = Math.max(bbox1.getDoubleValue(2), bbox2.getDoubleValue(2));
        double y2 = Math.max(bbox1.getDoubleValue(3), bbox2.getDoubleValue(3));
        
        JSONArray merged = new JSONArray();
        merged.add(x1);
        merged.add(y1);
        merged.add(x2);
        merged.add(y2);
        
        return merged;
    }
    
    /**
     * 合并两个表格的HTML内容
     * 保持紧凑格式（无缩进和换行），与原始OCR文本的格式一致
     */
    private static String mergeTableBodies(String html1, String html2) {
        try {
            Document doc1 = Jsoup.parse(html1);
            Document doc2 = Jsoup.parse(html2);
            
            Element table1 = doc1.select("table").first();
            Element table2 = doc2.select("table").first();
            
            if (table1 == null || table2 == null) {
                log.warn("无法解析表格HTML");
                return html1 + html2;
            }
            
            // 将table2的所有行追加到table1
            Elements rows2 = table2.select("tr");
            for (Element row : rows2) {
                table1.appendChild(row.clone());
            }
            
            // 设置输出格式为紧凑格式，与原始OCR文本保持一致
            doc1.outputSettings()
                .prettyPrint(false)  // 禁用格式化
                .indentAmount(0)      // 无缩进
                .syntax(Document.OutputSettings.Syntax.xml); // 使用XML语法，避免自动添加tbody等HTML5标签
            
            // 获取HTML并移除Jsoup可能添加的tbody标签，保持与原始格式一致
            String mergedHtml = table1.outerHtml();
            // 移除 <tbody> 和 </tbody> 标签（包括可能的空格）
            mergedHtml = mergedHtml.replaceAll("<tbody>|</tbody>", "");
            
            return mergedHtml;
        } catch (Exception e) {
            log.error("合并表格HTML失败", e);
            return html1 + html2;
        }
    }
}

