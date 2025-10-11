package com.zhaoxinms.contract.tools.ruleextract.engine.enhanced;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 提取结果
 *
 * @author zhaoxin
 * @since 2024-10-09
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractionResult implements Serializable {

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 提取到的值
     */
    private String value;

    /**
     * 所有匹配项（当配置为返回所有匹配时）
     */
    private List<String> allMatches;

    /**
     * 置信度 (0-100)
     */
    private Integer confidence;

    /**
     * 匹配的规则ID
     */
    private String matchedRuleId;

    /**
     * 匹配的开始位置
     */
    private Integer startPosition;

    /**
     * 匹配的结束位置
     */
    private Integer endPosition;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 调试信息
     */
    private List<String> debugInfo;

    /**
     * 表格数据（当提取整表时使用）
     */
    private Object tableData;

    /**
     * 添加调试信息
     */
    public void addDebugInfo(String info) {
        if (debugInfo == null) {
            debugInfo = new ArrayList<>();
        }
        debugInfo.add(info);
    }

    /**
     * 创建成功结果
     */
    public static ExtractionResult success(String value) {
        return ExtractionResult.builder()
                .success(true)
                .value(value)
                .confidence(100)
                .build();
    }

    /**
     * 创建失败结果
     */
    public static ExtractionResult failure(String errorMessage) {
        return ExtractionResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}

