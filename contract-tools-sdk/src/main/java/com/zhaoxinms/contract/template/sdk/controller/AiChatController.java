package com.zhaoxinms.contract.template.sdk.controller;

import com.zhaoxinms.contract.tools.aicomponent.service.ChatMessage;
import com.zhaoxinms.contract.tools.aicomponent.service.OpenAiService;
import com.zhaoxinms.contract.tools.aicomponent.util.AiLimitUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import com.zhaoxinms.contract.tools.common.Result;
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
    @Qualifier("defaultOpenAiServiceImpl")
    private OpenAiService openAiService;

    @Autowired
    private AiLimitUtil aiLimitUtil;

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
    public Result<Map<String, Object>> sendMessage(@RequestParam String message, 
                                                        @RequestParam(required = false) String sessionId) {
        log.info("收到聊天请求，会话ID: {}, 消息: {}", sessionId, message);
        
        // 检查限流
        if (!aiLimitUtil.tryAcquire("system")) {
            return Result.error("请求过于频繁，请稍后再试");
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
            
            // 检查AI服务是否返回错误信息
            if (reply != null && (reply.startsWith("API调用失败") || reply.startsWith("AI服务异常") || reply.startsWith("请求过于频繁"))) {
                return Result.error(reply);
            }
            
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
            
            Map<String, Object> result = new HashMap<>();
            result.put("reply", reply);
            result.put("sessionId", sessionId);
            return Result.success("成功", result);
        } catch (Exception e) {
            log.error("处理聊天请求时发生错误", e);
            return Result.error("服务器错误: " + e.getMessage());
        }
    }
    /**
     * 获取会话列表
     *
     * @return 会话列表
     */
    @GetMapping("/sessions")
    public Result<List<Map<String, Object>>> getSessions() {
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
                    Object content = message.getContent();
                    if (content instanceof String) {
                        title = (String) content;
                    } else {
                        title = "多模态消息";
                    }
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
        
        return Result.success("成功", sessions);
    }
    
    /**
     * 删除会话
     *
     * @param sessionId 会话ID
     * @return 操作结果
     */
    @DeleteMapping("/session/{sessionId}")
    public Result<Void> deleteSession(@PathVariable String sessionId) {
        if (sessionMessages.containsKey(sessionId)) {
            sessionMessages.remove(sessionId);
            return Result.success("会话已删除", null);
        } else {
            return Result.error("会话不存在");
        }
    }
    
    /**
     * 测试图片OCR识别功能
     *
     * @return OCR识别结果
     */
    @GetMapping("/test")
    public Result<String> testImageOcr() {
        log.info("开始测试图片OCR识别功能");
        
        try {
            // 本地图片路径
            String imagePath = "C:\\Users\\91088\\Desktop\\微信图片_20250708153126_30.jpg";
            
            // 检查文件是否存在
            java.io.File imageFile = new java.io.File(imagePath);
            if (!imageFile.exists()) {
                return Result.error("图片文件不存在: " + imagePath);
            }
            
            log.info("图片文件存在，大小: {} bytes", imageFile.length());
            
            // 读取图片文件并转换为Base64
            byte[] imageBytes = java.nio.file.Files.readAllBytes(imageFile.toPath());
            String base64Image = java.util.Base64.getEncoder().encodeToString(imageBytes);
            
            log.info("图片已转换为Base64，长度: {}", base64Image.length());
            
            // 构建OCR请求消息
            List<ChatMessage> messages = new ArrayList<>();
            
            // 构建多模态消息内容
            List<Map<String, Object>> content = new ArrayList<>();
            
            // 添加图片内容
            Map<String, Object> imageContent = new HashMap<>();
            imageContent.put("type", "image_url");
            imageContent.put("image_url", "data:image/jpeg;base64," + base64Image);
            imageContent.put("min_pixels", 3136);
            imageContent.put("max_pixels", 6422528);
            content.add(imageContent);
            
            // 添加文本提示
            Map<String, Object> textContent = new HashMap<>();
            textContent.put("type", "text");
            textContent.put("text", "请提取车票图像中的发票号码、车次、起始站、终点站、发车日期和时间点、座位号、席别类型、票价、身份证号码、购票人姓名。要求准确无误的提取上述关键信息、不要遗漏和捏造虚假信息，模糊或者强光遮挡的单个文字可以用英文问号?代替。返回数据格式以json方式输出，格式为：{'发票号码'：'xxx', '车次'：'xxx', '起始站'：'xxx', '终点站'：'xxx', '发车日期和时间点'：'xxx', '座位号'：'xxx', '席别类型'：'xxx','票价':'xxx', '身份证号码'：'xxx', '购票人姓名'：'xxx'}");
            content.add(textContent);
            
            // 创建多模态消息
            ChatMessage ocrMessage = new ChatMessage(ChatMessage.ROLE_USER, content);
            messages.add(ocrMessage);
            
            log.info("开始调用OCR API，消息数量: {}", messages.size());
            
            // 调用AI服务进行OCR识别
            String reply = openAiService.completion(messages);
            
            log.info("OCR API调用完成，返回结果长度: {}", reply != null ? reply.length() : 0);
            
            if (reply.startsWith("API调用失败") || reply.startsWith("AI服务异常") || reply.startsWith("请求过于频繁")) {
                return Result.error("OCR识别失败: " + reply);
            } else {
                return Result.success("OCR识别成功", reply);
            }
        } catch (Exception e) {
            log.error("测试OCR识别时发生错误", e);
            return Result.error("OCR识别测试失败: " + e.getMessage());
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
    // 统一返回格式，删除旧的 createResponse
}