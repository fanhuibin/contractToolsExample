package com.zhaoxin.demo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务文件名映射
 * 用于保存任务ID和原始文件名的对应关系
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskFileMapping {
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 原始旧文件名
     */
    private String oldFileName;
    
    /**
     * 原始新文件名
     */
    private String newFileName;
    
    /**
     * 创建时间
     */
    private Long createTime;
}

