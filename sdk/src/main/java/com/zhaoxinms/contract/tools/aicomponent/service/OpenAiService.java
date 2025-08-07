package com.zhaoxinms.contract.tools.aicomponent.service;

import java.util.List;

/**
 * AI服务工具
 * @author zhaoxinms
 */
public interface OpenAiService {

    /**
     * 简单对话
     * @param prompt 提示词
     * @return AI回复
     */
    String completion(String prompt);

    /**
     * 连续对话
     * @param messages 历史对话内容
     * @return AI回复
     */
    String completion(List<ChatMessage> messages);

    /**
     * 生成表单
     * @param businessName 业务名称
     * @return 表单JSON字符串
     */
    String generatorModelStr(String businessName);

    /**
     * 从PDF抽取文本
     * @param pdfContent PDF文件内容
     * @return 抽取的文本
     */
    String extractTextFromPdf(byte[] pdfContent);
}