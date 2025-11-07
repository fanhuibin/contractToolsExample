package com.zhaoxinms.contract.tools.common.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 文件信息实体类
 */
@Data
public class FileInfo {
    
    /**
     * 文件ID（数据库ID，雪花算法生成）
     */
    private Long id;
    
    /**
     * 完整文件ID（带年月前缀，如：202411_244043953257713664）
     * 用于API传输和前端展示，避免JavaScript精度丢失
     */
    private String fileId;
    
    /**
     * 原始文件名
     */
    private String originalName;
    
    /**
     * 文件名
     */
    private String fileName;
    
    /**
     * 文件路径
     */
    private String filePath;
    
    /**
     * 文件存储路径
     */
    private String storePath;
    
    /**
     * 文件大小
     */
    private Long fileSize;
    
    /**
     * 文件类型
     */
    private String fileType;
    
    /**
     * 文件扩展名
     */
    private String fileExtension;
    
    /**
     * 文件MD5
     */
    private String fileMd5;
    
    /**
     * 状态（0-正常，1-删除）
     */
    private Integer status;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 上传时间
     */
    private LocalDateTime uploadTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * OnlyOffice文档key
     */
    private String onlyofficeKey;
    
    /**
     * 所属模块（用于区分文件来源）
     * 例如：onlyoffice-demo, file-manager, template-design 等
     */
    private String module;
    
    /**
     * 获取文件存储路径
     */
    public String getStorePath() {
        return storePath != null ? storePath : filePath;
    }
} 