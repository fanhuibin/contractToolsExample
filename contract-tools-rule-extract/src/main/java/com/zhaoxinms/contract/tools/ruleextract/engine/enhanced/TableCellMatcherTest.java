package com.zhaoxinms.contract.tools.ruleextract.engine.enhanced;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

/**
 * TableCellMatcher 测试类
 */
public class TableCellMatcherTest {
    
    public static void main(String[] args) {
        // 用户提供的参数
        String text = "<table><tr><td>序号</td><td>货物名称</td><td>规格型号</td><td>产地</td><td>数量</td><td>单价</td><td>合计</td><td>备注</td></tr><tr><td>1</td><td>达撒</td><td>Sdsadas</td><td>达撒</td><td>32</td><td>23</td><td>23123</td><td></td></tr><tr><td>2</td><td></td><td></td><td></td><td></td><td></td><td></td><td></td></tr><tr><td>3</td><td></td><td></td><td></td><td></td><td></td><td></td><td></td></tr><tr><td>4</td><td></td><td></td><td></td><td></td><td></td><td></td><td></td></tr></table>";
        
        String configJson = "{\"extractMode\":\"cell\",\"headerPattern\":\"序号|货物名称|规格型号|产地\",\"targetColumn\":\"\",\"columnIndex\":2,\"rowMarker\":\"\",\"rowIndex\":2,\"format\":\"json\",\"matchMode\":\"single\",\"occurrence\":1,\"returnAll\":false}";
        
        JSONObject config = JSON.parseObject(configJson);
        
        System.out.println("=== TableCellMatcher 测试 ===");
        System.out.println("文本长度: " + text.length());
        System.out.println("配置: " + config.toJSONString());
        System.out.println();
        
        // 创建 TableCellMatcher 实例
        TableCellMatcher matcher = new TableCellMatcher();
        
        // 执行提取（开启调试）
        ExtractionResult result = matcher.extract(text, config, true);
        
        System.out.println("=== 提取结果 ===");
        System.out.println("成功: " + result.getSuccess());
        System.out.println("值: " + result.getValue());
        System.out.println("置信度: " + result.getConfidence());
        
        if (result.getDebugInfo() != null && !result.getDebugInfo().isEmpty()) {
            System.out.println("\n=== 调试信息 ===");
            for (String debug : result.getDebugInfo()) {
                System.out.println(debug);
            }
        }
        
        System.out.println("\n=== 分析配置参数 ===");
        System.out.println("extractMode: " + config.getString("extractMode"));
        System.out.println("headerPattern: " + config.getString("headerPattern"));
        System.out.println("targetColumn: '" + config.getString("targetColumn") + "'");
        System.out.println("columnIndex: " + config.getInteger("columnIndex"));
        System.out.println("rowIndex: " + config.getInteger("rowIndex"));
        System.out.println("occurrence: " + config.getInteger("occurrence"));
        System.out.println("returnAll: " + config.getBoolean("returnAll"));
        
        // 分析问题
        System.out.println("\n=== 问题分析 ===");
        if (config.getString("targetColumn") == null || config.getString("targetColumn").isEmpty()) {
            System.out.println("⚠️  targetColumn 为空，将使用 columnIndex: " + config.getInteger("columnIndex"));
            if (config.getInteger("columnIndex") != null) {
                int colIndex = config.getInteger("columnIndex") - 1;  // 转换为0-based索引
                System.out.println("   实际列索引: " + colIndex + " (0-based)");
                System.out.println("   对应列名: 规格型号 (第3列)");
            }
        }
        
        if (config.getInteger("rowIndex") != null) {
            int rowIdx = config.getInteger("rowIndex");
            System.out.println("✅ 指定行索引: " + rowIdx + " (数据行索引，不包含表头)");
            System.out.println("   对应数据行" + rowIdx + "（表格的第" + (rowIdx + 1) + "行，包含表头）");
        }
        
        System.out.println("\n=== 预期结果 ===");
        System.out.println("应该提取: 数据行2第2列的值");
        System.out.println("表格数据:");
        System.out.println("  表头:     [序号, 货物名称, 规格型号, 产地, 数量, 单价, 合计, 备注]");
        System.out.println("  数据行1:  [1, 达撒, Sdsadas, 达撒, 32, 23, 23123, ]");
        System.out.println("  数据行2:  [2, , , , , , , ]  ← rowIndex=2 指向这里");
        System.out.println("预期提取值: 空字符串 (数据行2第2列为空)");
        
        System.out.println("=== 测试完成 ===");
    }
}
