package com.zhaoxinms.contract.tools.ruleextract.model;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 规则抽取任务模型（JSON存储）
 * 
 * @author zhaoxin
 * @since 2024-10-09
 */
@Data
public class RuleExtractTaskModel implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 模板ID
     */
    private String templateId;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * OCR提供商
     */
    private String ocrProvider;

    /**
     * 任务状态
     */
    private String status;

    /**
     * 进度
     */
    private Integer progress;

    /**
     * 状态消息
     */
    private String message;

    /**
     * OCR结果路径
     */
    private String ocrResultPath;

    /**
     * 提取结果（JSON字符串）
     */
    private String resultJson;

    /**
     * 位置映射数据
     */
    private String bboxMappings;

    /**
     * 字符框数据
     */
    private String charBoxes;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 开始处理时间
     */
    private LocalDateTime startedAt;

    /**
     * 完成时间
     */
    private LocalDateTime completedAt;

    /**
     * 处理耗时（秒）
     */
    private Integer durationSeconds;
}

