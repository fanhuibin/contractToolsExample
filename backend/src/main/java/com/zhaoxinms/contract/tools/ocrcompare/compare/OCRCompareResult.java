package com.zhaoxinms.contract.tools.ocrcompare.compare;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * OCR比对结果实体类
 */
public class OCRCompareResult {
    
    private String taskId;                           // 任务ID
    private String oldPdfUrl;                        // 旧文档PDF URL
    private String newPdfUrl;                        // 新文档PDF URL
    private List<Map<String, Object>> differences;  // 差异列表
    private int totalDifferences;                    // 总差异数
    private double similarity;                       // 相似度
    private LocalDateTime compareTime;               // 比对时间
    private String compareSummary;                   // 比对摘要
    private Map<String, Object> summary;            // 前端需要的summary字段
    
    public OCRCompareResult() {
        this.compareTime = LocalDateTime.now();
    }
    
    public OCRCompareResult(String taskId) {
        this();
        this.taskId = taskId;
    }
    
    // Getters and Setters
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    
    public String getOldPdfUrl() { return oldPdfUrl; }
    public void setOldPdfUrl(String oldPdfUrl) { this.oldPdfUrl = oldPdfUrl; }
    
    public String getNewPdfUrl() { return newPdfUrl; }
    public void setNewPdfUrl(String newPdfUrl) { this.newPdfUrl = newPdfUrl; }
    
    public List<Map<String, Object>> getDifferences() { return differences; }
    public void setDifferences(List<Map<String, Object>> differences) { 
        this.differences = differences; 
        this.totalDifferences = differences != null ? differences.size() : 0;
    }
    
    public int getTotalDifferences() { return totalDifferences; }
    public void setTotalDifferences(int totalDifferences) { this.totalDifferences = totalDifferences; }
    
    public double getSimilarity() { return similarity; }
    public void setSimilarity(double similarity) { this.similarity = similarity; }
    
    public LocalDateTime getCompareTime() { return compareTime; }
    public void setCompareTime(LocalDateTime compareTime) { this.compareTime = compareTime; }
    
    public String getCompareSummary() { return compareSummary; }
    public void setCompareSummary(String compareSummary) { this.compareSummary = compareSummary; }
    
    public Map<String, Object> getSummary() { return summary; }
    public void setSummary(Map<String, Object> summary) { this.summary = summary; }
    
    /**
     * 获取差异统计信息
     */
    public Map<String, Object> getDifferenceStats() {
        if (differences == null) {
            return Map.of("total", 0, "inserts", 0, "deletes", 0, "modifies", 0);
        }
        
        int inserts = 0, deletes = 0, modifies = 0;
        for (Map<String, Object> diff : differences) {
            String operation = (String) diff.get("operation");
            if ("INSERT".equals(operation)) {
                inserts++;
            } else if ("DELETE".equals(operation)) {
                deletes++;
            } else if ("MODIFY".equals(operation)) {
                modifies++;
            }
        }
        
        return Map.of(
            "total", totalDifferences,
            "inserts", inserts,
            "deletes", deletes,
            "modifies", modifies
        );
    }
    
    /**
     * 生成比对摘要
     */
    public void generateSummary() {
        if (differences == null || differences.isEmpty()) {
            this.compareSummary = "文档完全一致，无差异";
            return;
        }
        
        Map<String, Object> stats = getDifferenceStats();
        int total = (Integer) stats.get("total");
        int inserts = (Integer) stats.get("inserts");
        int deletes = (Integer) stats.get("deletes");
        
        StringBuilder summary = new StringBuilder();
        summary.append("发现 ").append(total).append(" 处差异：");
        
        if (inserts > 0) {
            summary.append("新增 ").append(inserts).append(" 处");
        }
        if (deletes > 0) {
            if (inserts > 0) summary.append("，");
            summary.append("删除 ").append(deletes).append(" 处");
        }
        
        summary.append("。相似度：").append(String.format("%.2f%%", similarity * 100));
        
        this.compareSummary = summary.toString();
    }
    
    @Override
    public String toString() {
        return "OCRCompareResult{" +
                "taskId='" + taskId + '\'' +
                ", totalDifferences=" + totalDifferences +
                ", similarity=" + similarity +
                ", compareTime=" + compareTime +
                ", compareSummary='" + compareSummary + '\'' +
                '}';
    }
}
