package com.zhaoxinms.contract.tools.extract.core.data;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.HashMap;

/**
 * 文档数据模型
 * 对应Python版本的Document类
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Document {
    
    /**
     * 文档唯一标识符
     */
    private String id;
    
    /**
     * 文档内容
     */
    private String content;
    
    /**
     * 文档元数据
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    
    /**
     * 文档来源
     */
    private String source;
    
    /**
     * 文档类型
     */
    private String type;
    
    /**
     * 文档语言
     */
    private String language;
    
    /**
     * 创建时间戳
     */
    private Long createdAt;
    
    /**
     * 获取文档长度
     */
    public int getLength() {
        return content != null ? content.length() : 0;
    }
    
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
     * 检查是否为空文档
     */
    public boolean isEmpty() {
        return content == null || content.trim().isEmpty();
    }
}
