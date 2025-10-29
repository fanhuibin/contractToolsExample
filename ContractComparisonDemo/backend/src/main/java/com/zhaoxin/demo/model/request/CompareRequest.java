package com.zhaoxin.demo.model.request;

import lombok.Data;

/**
 * 比对请求
 */
@Data
public class CompareRequest {
    
    /**
     * 原文件URL
     */
    private String oldFileUrl;
    
    /**
     * 新文件URL
     */
    private String newFileUrl;
    
    /**
     * 原文件名（用于显示）
     */
    private String oldFileName;
    
    /**
     * 新文件名（用于显示）
     */
    private String newFileName;
    
    /**
     * 是否去除水印
     */
    private Boolean removeWatermark;
}

