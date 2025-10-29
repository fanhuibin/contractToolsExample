package com.zhaoxinms.contract.tools.common.util;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文件存储工具类
 * 统一管理文件ID生成和年月路径
 * 
 * @author zhaoxin
 */
public class FileStorageUtils {
    
    // 年月格式化器
    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    
    // ID格式：yyyyMM_原始ID （如：202410_123456）
    private static final Pattern ID_PATTERN = Pattern.compile("^(\\d{6})_(.+)$");
    
    /**
     * 生成带年月前缀的文件ID
     * 格式：yyyyMM_原始ID
     * 
     * @param originalId 原始ID（如数据库自增ID或UUID）
     * @return 带年月前缀的ID（如：202410_123456）
     */
    public static String generateFileId(String originalId) {
        if (originalId == null || originalId.isEmpty()) {
            throw new IllegalArgumentException("原始ID不能为空");
        }
        
        String yearMonth = LocalDateTime.now().format(YEAR_MONTH_FORMATTER);
        return yearMonth + "_" + originalId;
    }
    
    /**
     * 生成带年月前缀的文件ID（Long类型）
     * 
     * @param originalId 原始ID
     * @return 带年月前缀的ID
     */
    public static String generateFileId(Long originalId) {
        if (originalId == null) {
            throw new IllegalArgumentException("原始ID不能为空");
        }
        return generateFileId(String.valueOf(originalId));
    }
    
    /**
     * 从带年月前缀的ID中提取原始ID
     * 
     * @param fileId 文件ID（如：202410_123456）
     * @return 原始ID（如：123456），如果没有前缀则返回原值
     */
    public static String extractOriginalId(String fileId) {
        if (fileId == null || fileId.isEmpty()) {
            return fileId;
        }
        
        Matcher matcher = ID_PATTERN.matcher(fileId);
        if (matcher.matches()) {
            return matcher.group(2); // 返回原始ID部分
        }
        
        // 如果不匹配，说明是旧格式的ID，直接返回
        return fileId;
    }
    
    /**
     * 从带年月前缀的ID中提取年月信息
     * 
     * @param fileId 文件ID（如：202410_123456）
     * @return 年月（如：202410），如果没有前缀则返回当前年月
     */
    public static String extractYearMonth(String fileId) {
        if (fileId == null || fileId.isEmpty()) {
            return LocalDateTime.now().format(YEAR_MONTH_FORMATTER);
        }
        
        Matcher matcher = ID_PATTERN.matcher(fileId);
        if (matcher.matches()) {
            return matcher.group(1); // 返回年月部分
        }
        
        // 如果不匹配，说明是旧格式的ID，返回当前年月
        return LocalDateTime.now().format(YEAR_MONTH_FORMATTER);
    }
    
    /**
     * 检查ID是否包含年月前缀
     * 
     * @param fileId 文件ID
     * @return true如果包含年月前缀
     */
    public static boolean hasYearMonthPrefix(String fileId) {
        if (fileId == null || fileId.isEmpty()) {
            return false;
        }
        return ID_PATTERN.matcher(fileId).matches();
    }
    
    /**
     * 获取年月路径（yyyy/MM格式）
     * 
     * @return 年月路径（如：2024/10）
     */
    public static String getYearMonthPath() {
        LocalDateTime now = LocalDateTime.now();
        return now.getYear() + File.separator + String.format("%02d", now.getMonthValue());
    }
    
    /**
     * 根据年月字符串获取年月路径
     * 
     * @param yearMonth 年月字符串（如：202410）
     * @return 年月路径（如：2024/10）
     */
    public static String getYearMonthPath(String yearMonth) {
        if (yearMonth == null || yearMonth.length() != 6) {
            return getYearMonthPath();
        }
        
        String year = yearMonth.substring(0, 4);
        String month = yearMonth.substring(4, 6);
        return year + File.separator + month;
    }
    
    /**
     * 根据年月字符串获取年月URL路径（使用正斜杠）
     * 用于构建Web URL，确保跨平台兼容
     * 
     * @param yearMonth 年月字符串（如：202410）
     * @return 年月URL路径（如：2024/10）
     */
    public static String getYearMonthUrlPath(String yearMonth) {
        if (yearMonth == null || yearMonth.length() != 6) {
            LocalDateTime now = LocalDateTime.now();
            return now.getYear() + "/" + String.format("%02d", now.getMonthValue());
        }
        
        String year = yearMonth.substring(0, 4);
        String month = yearMonth.substring(4, 6);
        return year + "/" + month;
    }
    
    /**
     * 根据文件ID获取年月路径
     * 
     * @param fileId 文件ID（如：202410_123456）
     * @return 年月路径（如：2024/10）
     */
    public static String getYearMonthPathFromFileId(String fileId) {
        String yearMonth = extractYearMonth(fileId);
        return getYearMonthPath(yearMonth);
    }
    
    /**
     * 构建带年月的模块路径
     * 
     * @param baseDir 基础目录（如：./uploads）
     * @param module 模块名（如：files、templates）
     * @return 完整路径（如：./uploads/files/2024/10）
     */
    public static String buildModulePath(String baseDir, String module) {
        String yearMonthPath = getYearMonthPath();
        return baseDir + File.separator + module + File.separator + yearMonthPath;
    }
    
    /**
     * 构建带年月的模块路径（指定年月）
     * 
     * @param baseDir 基础目录
     * @param module 模块名
     * @param fileId 文件ID（用于提取年月）
     * @return 完整路径
     */
    public static String buildModulePathFromFileId(String baseDir, String module, String fileId) {
        String yearMonthPath = getYearMonthPathFromFileId(fileId);
        return baseDir + File.separator + module + File.separator + yearMonthPath;
    }
    
    /**
     * 获取临时上传目录路径
     * 所有文件先上传到这里，使用时再复制到对应模块
     * 目录结构：temp-uploads/{年}/{月}/{日期}
     * 
     * @param baseDir 基础目录（如：./uploads）
     * @return 临时上传目录路径
     */
    public static String getTempUploadPath(String baseDir) {
        LocalDateTime now = LocalDateTime.now();
        String year = String.valueOf(now.getYear());
        String month = String.format("%02d", now.getMonthValue());
        String day = String.format("%02d", now.getDayOfMonth());
        return baseDir + File.separator + "temp-uploads" + File.separator + year + File.separator + month + File.separator + day;
    }
    
    /**
     * 获取临时上传目录的根路径（用于批量清理）
     * 
     * @param baseDir 基础目录
     * @return 临时上传根目录路径
     */
    public static String getTempUploadRoot(String baseDir) {
        return baseDir + File.separator + "temp-uploads";
    }
}

