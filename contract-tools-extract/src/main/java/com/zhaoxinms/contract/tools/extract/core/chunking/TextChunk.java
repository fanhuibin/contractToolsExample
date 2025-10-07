package com.zhaoxinms.contract.tools.extract.core.chunking;

import com.zhaoxinms.contract.tools.extract.core.data.CharInterval;
import com.zhaoxinms.contract.tools.extract.core.data.Document;
import lombok.Data;
import lombok.Builder;

/**
 * 表示文档的一个文本块
 * 对应Python版本的TextChunk类
 */
@Data
@Builder
public class TextChunk {
    
    /**
     * 块在原始文档中的字符区间
     */
    private CharInterval charInterval;
    
    /**
     * 源文档引用
     */
    private Document sourceDocument;
    
    /**
     * 块的文本内容（缓存）
     */
    private String chunkText;
    
    /**
     * 块的索引（在文档中的顺序）
     */
    private int chunkIndex;
    
    /**
     * 块的ID（用于追踪）
     */
    private String chunkId;
    
    /**
     * 获取块的文本内容
     */
    public String getChunkText() {
        if (chunkText == null && sourceDocument != null && charInterval != null) {
            String fullText = sourceDocument.getContent();
            if (fullText != null && charInterval.isValid()) {
                int start = Math.max(0, charInterval.getStartPos());
                int end = Math.min(fullText.length(), charInterval.getEndPos());
                chunkText = fullText.substring(start, end);
            }
        }
        return chunkText;
    }
    
    /**
     * 获取清理后的块文本（移除多余空白字符）
     */
    public String getSanitizedChunkText() {
        String text = getChunkText();
        if (text == null) {
            return null;
        }
        // 将所有空白字符替换为单个空格，并去除首尾空格
        return text.replaceAll("\\s+", " ").trim();
    }
    
    /**
     * 获取文档ID
     */
    public String getDocumentId() {
        return sourceDocument != null ? sourceDocument.getId() : null;
    }
    
    /**
     * 检查块是否有效
     */
    public boolean isValid() {
        return charInterval != null && charInterval.isValid() && 
               sourceDocument != null && getChunkText() != null && !getChunkText().trim().isEmpty();
    }
    
    /**
     * 获取块的长度
     */
    public int getLength() {
        String text = getChunkText();
        return text != null ? text.length() : 0;
    }
    
    @Override
    public String toString() {
        return String.format("TextChunk[chunkId=%s, docId=%s, interval=%s, length=%d]",
            chunkId, getDocumentId(), charInterval, getLength());
    }
}
