package com.zhaoxinms.contract.template.sdk.controller;

import com.zhaoxinms.contract.tools.aicomponent.service.ChatMessage;
import com.zhaoxinms.contract.tools.aicomponent.service.OpenAiService;
import com.zhaoxinms.contract.tools.aicomponent.util.AiLimitUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * AI聊天控制器
 *
 * @author zhaoxinms
 */
@Slf4j
@RestController
@RequestMapping("/api/ai/chat")
public class AiChatController {

    @Autowired
    private OpenAiService openAiService;

    // 存储会话历史
    private final Map<String, List<ChatMessage>> sessionMessages = new HashMap<>();

    /**
     * 发送聊天消息
     *
     * @param message 用户消息
     * @param sessionId 会话ID
     * @return AI回复
     */
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendMessage(@RequestParam String message, 
                                                        @RequestParam(required = false) String sessionId) {
        log.info("收到聊天请求，会话ID: {}, 消息: {}", sessionId, message);
        
        // 检查限流
        if (!AiLimitUtil.tryAcquire("system")) {
            return ResponseEntity.ok(createResponse(false, "请求过于频繁，请稍后再试", null));
        }
        
        try {
            // 获取或创建会话
            if (sessionId == null || !sessionMessages.containsKey(sessionId)) {
                sessionId = UUID.randomUUID().toString();
                sessionMessages.put(sessionId, new ArrayList<>());
                // 添加系统提示
                sessionMessages.get(sessionId).add(new ChatMessage(ChatMessage.ROLE_SYSTEM, 
                        "你是一个专业、友好、有帮助的AI助手，专注于合同管理工具集的使用。"));
            }
            
            // 添加用户消息
            List<ChatMessage> messages = sessionMessages.get(sessionId);
            messages.add(new ChatMessage(ChatMessage.ROLE_USER, message));
            
            // 调用AI服务获取回复
            String reply = openAiService.completion(messages);
            
            // 添加AI回复到会话历史
            messages.add(new ChatMessage(ChatMessage.ROLE_ASSISTANT, reply));
            
            // 限制会话历史长度
            if (messages.size() > 20) {
                // 保留系统消息和最近的19条消息
                List<ChatMessage> recentMessages = new ArrayList<>();
                recentMessages.add(messages.get(0)); // 系统消息
                
                for (int i = messages.size() - 19; i < messages.size(); i++) {
                    recentMessages.add(messages.get(i));
                }
                
                sessionMessages.put(sessionId, recentMessages);
            }
            
            Map<String, Object> result = createResponse(true, "成功", reply);
            result.put("sessionId", sessionId);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("处理聊天请求时发生错误", e);
            return ResponseEntity.ok(createResponse(false, "服务器错误: " + e.getMessage(), null));
        }
    }
    
    /**
     * 获取会话列表
     *
     * @return 会话列表
     */
    @GetMapping("/sessions")
    public ResponseEntity<Map<String, Object>> getSessions() {
        List<Map<String, Object>> sessions = new ArrayList<>();
        
        for (Map.Entry<String, List<ChatMessage>> entry : sessionMessages.entrySet()) {
            Map<String, Object> session = new HashMap<>();
            session.put("id", entry.getKey());
            
            // 获取最后一条用户消息作为标题
            List<ChatMessage> messages = entry.getValue();
            String title = "新会话";
            for (int i = messages.size() - 1; i >= 0; i--) {
                ChatMessage message = messages.get(i);
                if (ChatMessage.ROLE_USER.equals(message.getRole())) {
                    title = message.getContent();
                    if (title.length() > 20) {
                        title = title.substring(0, 20) + "...";
                    }
                    break;
                }
            }
            
            session.put("title", title);
            session.put("messageCount", messages.size() - 1); // 减去系统消息
            sessions.add(session);
        }
        
        Map<String, Object> result = createResponse(true, "成功", null);
        result.put("sessions", sessions);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 删除会话
     *
     * @param sessionId 会话ID
     * @return 操作结果
     */
    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> deleteSession(@PathVariable String sessionId) {
        if (sessionMessages.containsKey(sessionId)) {
            sessionMessages.remove(sessionId);
            return ResponseEntity.ok(createResponse(true, "会话已删除", null));
        } else {
            return ResponseEntity.ok(createResponse(false, "会话不存在", null));
        }
    }
    
    /**
     * 创建响应对象
     *
     * @param success 是否成功
     * @param message 消息
     * @param data 数据
     * @return 响应对象
     */
    private Map<String, Object> createResponse(boolean success, String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", message);
        if (data != null) {
            response.put("data", data);
        }
        return response;
    }
}