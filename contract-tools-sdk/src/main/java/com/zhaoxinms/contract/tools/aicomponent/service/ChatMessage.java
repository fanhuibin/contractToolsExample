package com.zhaoxinms.contract.tools.aicomponent.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 聊天消息实体类
 * 
 * @author zhaoxinms
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    
    /**
     * 消息角色
     */
    private String role;
    
    /**
     * 消息内容 - 可以是字符串或对象列表（多模态）
     */
    private Object content;

    /**
     * 系统角色
     */
    public static final String ROLE_SYSTEM = "system";
    
    /**
     * 用户角色
     */
    public static final String ROLE_USER = "user";
    
    /**
     * 助手角色
     */
    public static final String ROLE_ASSISTANT = "assistant";
    
    /**
     * 构造函数 - 用于文本消息
     */
    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }
    
    /**
     * 构造函数 - 用于多模态消息
     */
    public ChatMessage(String role, List<Map<String, Object>> content) {
        this.role = role;
        this.content = content;
    }
}