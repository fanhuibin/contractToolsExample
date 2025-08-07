package com.zhaoxinms.contract.tools.aicomponent.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
     * 消息内容
     */
    private String content;

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
}