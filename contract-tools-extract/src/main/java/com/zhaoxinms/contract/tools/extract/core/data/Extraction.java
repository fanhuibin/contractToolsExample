package com.zhaoxinms.contract.tools.extract.core.data;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.HashMap;

/**
 * 提取结果数据模型
 * 对应Python版本的Extraction类
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Extraction {
    
    /**
     * 提取结果唯一标识符
     */
    private String id;
    
    /**
     * 关联的文档ID
     */
    private String documentId;
    
    /**
     * 提取的字段名
     */
    private String field;
    
    /**
     * 提取的值
     */
    private Object value;
    
    /**
     * 字符区间 - 提取结果在原文中的精确位置
     * 这是LangExtract的核心特性，用于实现100%可溯源
     */
    private CharInterval charInterval;
    
    /**
     * 提取的原始文本（已弃用，使用charInterval.sourceText）
     * @deprecated 使用 getSourceTextFromInterval() 替代
     */
    @Deprecated
    private String sourceText;
    
    /**
     * 在文档中的起始位置（已弃用，使用charInterval.startPos）
     * @deprecated 使用 getStartPositionFromInterval() 替代
     */
    @Deprecated
    private Integer startPosition;
    
    /**
     * 在文档中的结束位置（已弃用，使用charInterval.endPos）
     * @deprecated 使用 getEndPositionFromInterval() 替代
     */
    @Deprecated
    private Integer endPosition;
    
    /**
     * 置信度分数 (0.0 - 1.0)
     */
    private Double confidence;
    
    /**
     * 提取方法/模型
     */
    private String method;
    
    /**
     * 额外的元数据
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    
    /**
     * 创建时间戳
     */
    private Long createdAt;
    
    /**
     * 添加元数据
     */
    public void addMetadata(String key, Object value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
    }
    
    /**
     * 获取元数据
     */
    public Object getMetadata(String key) {
        return metadata != null ? metadata.get(key) : null;
    }
    
    /**
     * 获取源文本（从字符区间）
     */
    public String getSourceTextFromInterval() {
        return charInterval != null ? charInterval.getSourceText() : sourceText;
    }
    
    /**
     * 获取起始位置（从字符区间）
     */
    public Integer getStartPositionFromInterval() {
        return charInterval != null ? charInterval.getStartPos() : startPosition;
    }
    
    /**
     * 获取结束位置（从字符区间）
     */
    public Integer getEndPositionFromInterval() {
        return charInterval != null ? charInterval.getEndPos() : endPosition;
    }
    
    /**
     * 获取对齐置信度
     */
    public Double getAlignmentConfidence() {
        return charInterval != null ? charInterval.getAlignmentConfidence() : null;
    }
    
    /**
     * 检查是否有位置信息
     */
    public boolean hasPosition() {
        return (charInterval != null && charInterval.isValid()) || 
               (startPosition != null && endPosition != null);
    }
    
    /**
     * 获取文本长度
     */
    public int getTextLength() {
        String text = getSourceTextFromInterval();
        return text != null ? text.length() : 0;
    }
    
    /**
     * 检查与另一个提取结果是否重叠
     */
    public boolean overlapsWith(Extraction other) {
        if (this.charInterval == null || other.charInterval == null) {
            return false;
        }
        return this.charInterval.overlapsWith(other.charInterval);
    }
    
    /**
     * 计算与另一个提取结果的重叠比例
     */
    public double getOverlapRatio(Extraction other) {
        if (this.charInterval == null || other.charInterval == null) {
            return 0.0;
        }
        return this.charInterval.getOverlapRatio(other.charInterval);
    }
    
    /**
     * 检查是否具有高质量的位置信息
     */
    public boolean hasHighQualityPosition() {
        return charInterval != null && 
               charInterval.isValid() && 
               charInterval.getAlignmentConfidence() > 0.7;
    }
    
    /**
     * 检查置信度是否达到阈值
     */
    public boolean isConfidentEnough(double threshold) {
        return confidence != null && confidence >= threshold;
    }
}
