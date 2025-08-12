package com.zhaoxinms.contract.template.sdk.entity;

import lombok.Data;

@Data
public class CompareRecord {
    private Long id;
    private String bizId; // 如时间戳字符串
    private String oldPdfName;
    private String newPdfName;
    private String resultsJson;
    private java.time.LocalDateTime createdAt;
}


