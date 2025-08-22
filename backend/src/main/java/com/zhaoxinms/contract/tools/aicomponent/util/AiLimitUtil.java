package com.zhaoxinms.contract.tools.aicomponent.util;

import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.TimedCache;
import com.zhaoxinms.contract.tools.aicomponent.config.AiProperties;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * AI接口限流
 *
 * @author zhaoxinms
 */
@Component
public class AiLimitUtil {

    private final AiProperties aiProperties;

    private final TimedCache<String, AtomicInteger> limit;

    private static final String TOTAL_KEY = "ai_limit_total";

    public AiLimitUtil(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
        this.limit = CacheUtil.newTimedCache(aiProperties.getUserLimitTime().toMillis());
        // 一分钟清理一次无用数据
        this.limit.schedulePrune(1000L * 60);
    }

    /**
     * 是否可以请求
     * @param userId 用户ID
     * @return 是否可以请求
     */
    public boolean tryAcquire(String userId) {
        if (StringUtil.isNotEmpty(userId)) {
            // 按用户限制
            AtomicInteger userCount = limit.get(userId, false, AtomicInteger::new);
            if (userCount.incrementAndGet() > aiProperties.getUserLimitCount()) {
                return false;
            } 
        }
        // 所有请求限制
        AtomicInteger totalCount = limit.get(TOTAL_KEY, false, aiProperties.getTotalLimitTime().toMillis(), AtomicInteger::new);
        return totalCount.incrementAndGet() <= aiProperties.getTotalLimitCount();
    }
}