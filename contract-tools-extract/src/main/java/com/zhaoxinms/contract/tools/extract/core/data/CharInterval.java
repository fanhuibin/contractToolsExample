package com.zhaoxinms.contract.tools.extract.core.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 字符区间 - 表示提取结果在原文中的精确位置
 * 这是LangExtract的核心特性，用于实现100%可溯源
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CharInterval {
    
    /**
     * 起始字符位置（包含）
     */
    private Integer startPos;
    
    /**
     * 结束字符位置（不包含）
     */
    private Integer endPos;
    
    /**
     * 原文中的确切文本片段
     */
    private String sourceText;
    
    /**
     * 对齐置信度 (0.0 - 1.0)
     * 表示提取结果与原文对齐的准确程度
     */
    @Builder.Default
    private Double alignmentConfidence = 1.0;
    
    /**
     * 检查字符区间是否有效
     */
    public boolean isValid() {
        return startPos != null && endPos != null && 
               startPos >= 0 && endPos > startPos;
    }
    
    /**
     * 获取区间长度
     */
    public int getLength() {
        if (!isValid()) {
            return 0;
        }
        return endPos - startPos;
    }
    
    /**
     * 检查与另一个区间是否重叠
     */
    public boolean overlapsWith(CharInterval other) {
        if (!this.isValid() || !other.isValid()) {
            return false;
        }
        
        // 两个区间重叠的条件：一个区间的开始位置小于另一个区间的结束位置
        return this.startPos < other.endPos && other.startPos < this.endPos;
    }
    
    /**
     * 计算与另一个区间的重叠长度
     */
    public int getOverlapLength(CharInterval other) {
        if (!overlapsWith(other)) {
            return 0;
        }
        
        int overlapStart = Math.max(this.startPos, other.startPos);
        int overlapEnd = Math.min(this.endPos, other.endPos);
        
        return overlapEnd - overlapStart;
    }
    
    /**
     * 计算重叠比例 (0.0 - 1.0)
     */
    public double getOverlapRatio(CharInterval other) {
        int overlapLength = getOverlapLength(other);
        if (overlapLength == 0) {
            return 0.0;
        }
        
        int minLength = Math.min(this.getLength(), other.getLength());
        return (double) overlapLength / minLength;
    }
    
    /**
     * 检查是否包含另一个区间
     */
    public boolean contains(CharInterval other) {
        if (!this.isValid() || !other.isValid()) {
            return false;
        }
        
        return this.startPos <= other.startPos && this.endPos >= other.endPos;
    }
    
    /**
     * 检查是否被另一个区间包含
     */
    public boolean isContainedBy(CharInterval other) {
        return other.contains(this);
    }
    
    /**
     * 扩展区间到指定位置
     */
    public CharInterval expand(int newStart, int newEnd) {
        return CharInterval.builder()
            .startPos(Math.min(this.startPos, newStart))
            .endPos(Math.max(this.endPos, newEnd))
            .sourceText(this.sourceText)
            .alignmentConfidence(this.alignmentConfidence * 0.9) // 扩展后置信度略微降低
            .build();
    }
    
    /**
     * 合并两个相邻或重叠的区间
     */
    public CharInterval merge(CharInterval other) {
        if (!this.isValid() || !other.isValid()) {
            return this.isValid() ? this : other;
        }
        
        return CharInterval.builder()
            .startPos(Math.min(this.startPos, other.startPos))
            .endPos(Math.max(this.endPos, other.endPos))
            .sourceText(this.sourceText) // 保持原始文本
            .alignmentConfidence(Math.min(this.alignmentConfidence, other.alignmentConfidence))
            .build();
    }
    
    @Override
    public String toString() {
        if (!isValid()) {
            return "CharInterval[invalid]";
        }
        return String.format("CharInterval[%d-%d, length=%d, confidence=%.2f]", 
                           startPos, endPos, getLength(), alignmentConfidence);
    }
}
