package com.zhaoxinms.contract.tools.auth.core.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 日志辅助类
 */
public class LoggerHelper {
    
    private static final Logger logger = LoggerFactory.getLogger(LoggerHelper.class);
    
    public static void error(String message, Throwable throwable) {
        logger.error(message, throwable);
    }
    
    public static void error(String message) {
        logger.error(message);
    }
    
    public static void warn(String message) {
        logger.warn(message);
    }
    
    public static void info(String message) {
        logger.info(message);
    }
    
    public static void debug(String message) {
        logger.debug(message);
    }
}