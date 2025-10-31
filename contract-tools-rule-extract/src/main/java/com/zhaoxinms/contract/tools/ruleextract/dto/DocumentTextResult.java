package com.zhaoxinms.contract.tools.ruleextract.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 文档文本提取结果
 * 
 * @author 山西肇新科技有限公司
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentTextResult {
    
    /**
     * 文件名
     */
    private String fileName;
    
    /**
     * 页数
     */
    private Integer pageCount;
    
    /**
     * 提取时间
     */
    private String extractTime;
    
    /**
     * 纯文本内容
     */
    private String textContent;
    
    /**
     * 结构化数据（可选）
     */
    private StructuredData structuredData;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StructuredData {
        /**
         * 按页分组的内容
         */
        private List<PageContent> pages;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageContent {
        /**
         * 页码
         */
        private Integer pageNo;
        
        /**
         * 页面内容
         */
        private String content;
        
        /**
         * 关键词列表
         */
        private List<String> keywords;
    }
}

