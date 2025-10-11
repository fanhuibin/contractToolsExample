package com.zhaoxinms.contract.tools.ruleextract.engine.matcher;

import lombok.Data;

/**
 * 匹配结果
 * 
 * @author zhaoxin
 * @since 2024-10-09
 */
@Data
public class MatchResult {
    
    /**
     * 是否匹配成功
     */
    private boolean matched;
    
    /**
     * 提取的值
     */
    private String value;
    
    /**
     * 起始位置
     */
    private Integer startPos;
    
    /**
     * 结束位置
     */
    private Integer endPos;
    
    /**
     * 置信度 (0-100)
     */
    private Integer confidence;
    
    /**
     * 附加信息
     */
    private Object metadata;

    public MatchResult() {
        this.matched = false;
        this.confidence = 0;
    }

    public MatchResult(boolean matched, String value) {
        this.matched = matched;
        this.value = value;
        this.confidence = matched ? 100 : 0;
    }

    public MatchResult(boolean matched, String value, Integer startPos, Integer endPos) {
        this.matched = matched;
        this.value = value;
        this.startPos = startPos;
        this.endPos = endPos;
        this.confidence = matched ? 100 : 0;
    }

    public static MatchResult success(String value, Integer startPos, Integer endPos) {
        return new MatchResult(true, value, startPos, endPos);
    }

    public static MatchResult failed() {
        return new MatchResult(false, null);
    }
}
