package com.zhaoxinms.contract.tools.common.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件信息实体类
 * 使用文件系统存储，不依赖数据库
 */
@Data
public class FileInfo {
    
    /**
     * 文件ID（雪花ID）
     */
    private Long id;
    
    /**
     * 原始文件名
     */
    private String originalName;
    
    /**
     * 存储文件名
     */
    private String fileName;
    
    /**
     * 文件相对路径
     */
    private String filePath;
    
    /**
     * 文件大小（字节）
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
     * 文件MD5值
     */
    private String fileMd5;
    
    /**
     * 文件状态（0：正常，1：已删除）
     */
    private Integer status;
    
    /**
     * 上传时间
     */
    private LocalDateTime uploadTime;
    
    /**
     * 修改时间
     */
    private LocalDateTime updateTime;
    
    /**
     * OnlyOffice文档key（用于协同编辑）
     */
    private String onlyofficeKey;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 获取存储路径（用于文件访问）
     */
    public String getStorePath() {
        return this.filePath;
    }
} 