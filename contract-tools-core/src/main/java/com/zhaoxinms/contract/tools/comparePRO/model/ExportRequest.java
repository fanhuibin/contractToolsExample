package com.zhaoxinms.contract.tools.comparePRO.model;

import lombok.Data;

import java.util.List;

/**
 * 导出报告请求DTO
 * 用于service层，避免依赖controller层
 */
@Data
public class ExportRequest {
    
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 导出格式列表
     * 支持: "html", "doc"
     */
    private List<String> formats;
    
    /**
     * 是否包含图片
     */
    private boolean includeImages = true;
    
    /**
     * 报告标题
     */
    private String title;
    
    /**
     * 导出选项
     */
    private ExportOptions options;
    
    /**
     * 导出选项内部类
     */
    @Data
    public static class ExportOptions {
        /**
         * 是否包含统计信息
         */
        private boolean includeStatistics = true;
        
        /**
         * 是否包含详细差异
         */
        private boolean includeDetailedDiffs = true;
        
        /**
         * 是否包含页面预览
         */
        private boolean includePagePreview = true;
        
        /**
         * 图片质量 (1-100)
         */
        private int imageQuality = 80;
        
        /**
         * 最大导出差异数量 (-1表示无限制)
         */
        private int maxDifferences = -1;
    }
    
    /**
     * 默认构造函数
     */
    public ExportRequest() {
        this.options = new ExportOptions();
    }
    
    /**
     * 构造函数
     * @param taskId 任务ID
     * @param formats 导出格式列表
     */
    public ExportRequest(String taskId, List<String> formats) {
        this.taskId = taskId;
        this.formats = formats;
        this.options = new ExportOptions();
    }
}
