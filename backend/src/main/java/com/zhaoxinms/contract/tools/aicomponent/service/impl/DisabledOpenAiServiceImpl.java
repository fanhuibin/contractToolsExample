package com.zhaoxinms.contract.tools.aicomponent.service.impl;

import com.zhaoxinms.contract.tools.aicomponent.service.ChatMessage;
import com.zhaoxinms.contract.tools.aicomponent.service.OpenAiService;

import java.util.List;

/**
 * AI服务禁用实现
 * 当AI功能被禁用时使用此实现
 *
 * @author zhaoxinms
 */
public class DisabledOpenAiServiceImpl implements OpenAiService {

    private static final String DISABLED_MESSAGE = "AI功能已禁用，请联系管理员开启";

    @Override
    public String completion(String prompt) {
        return DISABLED_MESSAGE;
    }

    @Override
    public String completion(List<ChatMessage> messages) {
        return DISABLED_MESSAGE;
    }

    @Override
    public String generatorModelStr(String businessName) {
        return DISABLED_MESSAGE;
    }

    @Override
    public String extractTextFromPdf(byte[] pdfContent) {
        return DISABLED_MESSAGE;
    }
}