package com.zhaoxinms.contract.tools.comparePRO.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.zhaoxinms.contract.tools.comparePRO.model.CharBox;
import com.zhaoxinms.contract.tools.comparePRO.model.CompareResult;
import com.zhaoxinms.contract.tools.comparePRO.model.CompareTask;
import com.zhaoxinms.contract.tools.comparePRO.model.DiffBlock;
import com.zhaoxinms.contract.tools.comparePRO.service.CompareImageService.DocumentImageInfo;

/**
 * 比对结果格式化服务
 * 
 * 职责:
 * - 将DiffBlock转换为前端格式
 * - 构建前端结果对象
 * - 格式化差异类型
 * - 提取坐标文本
 * 
 * 重构说明:
 * 本服务从 CompareService 中分离出来，专门处理比对结果的格式转换。
 * 统一管理格式化逻辑，便于复用和维护。
 * 
 * @author AI Assistant
 * @since 2025-10-08
 */
@Service
public class CompareResultFormatter {
    
    private static final Logger logger = LoggerFactory.getLogger(CompareResultFormatter.class);
    
    /**
     * 构建前端结果对象
     * 
     * @param task 比对任务
     * @param result 比对结果
     * @param oldImageInfo 旧文档图片信息
     * @param newImageInfo 新文档图片信息
     * @return 前端结果Map
     */
    public Map<String, Object> buildFrontendResult(
            CompareTask task,
            CompareResult result,
            DocumentImageInfo oldImageInfo,
            DocumentImageInfo newImageInfo) {
        
        Map<String, Object> frontendResult = new HashMap<>();
        
        // 基本信息
        frontendResult.put("taskId", result.getTaskId());
        frontendResult.put("oldFileName", result.getOldFileName());
        frontendResult.put("newFileName", result.getNewFileName());
        
        // 差异信息
        List<Map<String, Object>> formattedDifferences;
        if (result.getFormattedDifferences() != null && !result.getFormattedDifferences().isEmpty()) {
            formattedDifferences = result.getFormattedDifferences();
        } else {
            // 需要转换DiffBlock为Map格式
            formattedDifferences = convertDiffBlocksToMapFormat(result.getDifferences(), false, null, null);
        }
        frontendResult.put("differences", formattedDifferences);
        frontendResult.put("totalDiffCount", formattedDifferences.size());
        
        // 图片信息
        frontendResult.put("oldImageInfo", convertImageInfoToMap(oldImageInfo));
        frontendResult.put("newImageInfo", convertImageInfoToMap(newImageInfo));
        
        // 时间信息
        if (task.getStepDurations() != null && !task.getStepDurations().isEmpty()) {
            frontendResult.put("stepDurations", task.getStepDurations());
        }
        if (task.getTotalDuration() != null) {
            frontendResult.put("totalDuration", task.getTotalDuration());
        }
        if (task.getStartTime() != null) {
            frontendResult.put("startTime", task.getStartTime().toString());
        }
        if (task.getEndTime() != null) {
            frontendResult.put("endTime", task.getEndTime().toString());
        }
        
        // 失败页面信息
        if (task.getFailedPages() != null && !task.getFailedPages().isEmpty()) {
            frontendResult.put("failedPages", task.getFailedPages());
            frontendResult.put("failedPagesCount", task.getFailedPages().size());
        } else {
            frontendResult.put("failedPages", new ArrayList<>());
            frontendResult.put("failedPagesCount", 0);
        }
        
        // 统计信息
        if (task.getStatistics() != null && !task.getStatistics().isEmpty()) {
            frontendResult.put("statistics", task.getStatistics());
        }
        
        return frontendResult;
    }
    
    /**
     * 将DiffBlock列表转换为前端期望的Map格式
     * 
     * @param diffBlocks 差异块列表
     * @param isDebugMode 是否调试模式
     * @param seqA 原文档字符序列
     * @param seqB 新文档字符序列
     * @return Map格式的差异列表
     */
    public List<Map<String, Object>> convertDiffBlocksToMapFormat(
            List<DiffBlock> diffBlocks,
            boolean isDebugMode,
            List<CharBox> seqA,
            List<CharBox> seqB) {
        
        List<Map<String, Object>> mapResult = new ArrayList<>();
        
        if (diffBlocks == null) {
            return mapResult;
        }
        
        for (DiffBlock block : diffBlocks) {
            Map<String, Object> diffMap = new HashMap<>();
            
            // 转换操作类型
            String operation = convertDiffTypeToOperation(block.type);
            diffMap.put("operation", operation);
            
            // 添加文本内容
            diffMap.put("oldText", block.oldText != null ? block.oldText : "");
            diffMap.put("newText", block.newText != null ? block.newText : "");
            
            // 添加页面信息
            diffMap.put("page", block.page);
            
            // 页码处理：对于INSERT/DELETE操作，需要特殊处理pageA/pageB
            if ("INSERT".equals(operation)) {
                if (block.pageA != null && !block.pageA.isEmpty()) {
                    diffMap.put("pageA", java.util.Collections.min(block.pageA));
                } else if (block.prevOldBboxPages != null && !block.prevOldBboxPages.isEmpty()) {
                    diffMap.put("pageA", java.util.Collections.min(block.prevOldBboxPages));
                } else {
                    diffMap.put("pageA", block.page);
                }
                if (block.pageB != null && !block.pageB.isEmpty()) {
                    diffMap.put("pageB", java.util.Collections.min(block.pageB));
                } else {
                    diffMap.put("pageB", block.page);
                }
            } else if ("DELETE".equals(operation)) {
                if (block.pageA != null && !block.pageA.isEmpty()) {
                    diffMap.put("pageA", java.util.Collections.min(block.pageA));
                } else {
                    diffMap.put("pageA", block.page);
                }
                if (block.pageB != null && !block.pageB.isEmpty()) {
                    diffMap.put("pageB", java.util.Collections.min(block.pageB));
                } else if (block.prevNewBboxPages != null && !block.prevNewBboxPages.isEmpty()) {
                    diffMap.put("pageB", java.util.Collections.min(block.prevNewBboxPages));
                } else {
                    diffMap.put("pageB", block.page);
                }
            } else {
                if (block.pageA != null && !block.pageA.isEmpty()) {
                    diffMap.put("pageA", java.util.Collections.min(block.pageA));
                } else {
                    diffMap.put("pageA", block.page);
                }
                if (block.pageB != null && !block.pageB.isEmpty()) {
                    diffMap.put("pageB", java.util.Collections.min(block.pageB));
                } else {
                    diffMap.put("pageB", block.page);
                }
            }
            
            // 添加完整的页码数组供前端使用
            diffMap.put("pageAList", block.pageA);
            diffMap.put("pageBList", block.pageB);
            
            // 添加bbox信息
            diffMap.put("oldBboxes", block.oldBboxes);
            diffMap.put("newBboxes", block.newBboxes);
            diffMap.put("prevOldBboxes", block.prevOldBboxes);
            diffMap.put("prevNewBboxes", block.prevNewBboxes);
            
            // 添加分类和索引信息
            if (block.category != null) {
                diffMap.put("category", block.category);
            }
            diffMap.put("indexA", block.indexA);
            diffMap.put("indexB", block.indexB);
            
            mapResult.add(diffMap);
        }
        
        logger.debug("转换DiffBlock为Map格式: 输入={}, 输出={}", 
            diffBlocks.size(), mapResult.size());
        
        return mapResult;
    }
    
    /**
     * 转换差异类型为操作类型
     * 
     * @param diffType 差异类型
     * @return 操作类型字符串
     */
    public String convertDiffTypeToOperation(DiffBlock.DiffType diffType) {
        if (diffType == null) {
            return "UNKNOWN";
        }
        
        switch (diffType) {
            case ADDED:
                return "INSERT";
            case DELETED:
                return "DELETE";
            case MODIFIED:
                return "MODIFY";
            case IGNORED:
                return "IGNORE";
            default:
                return "UNKNOWN";
        }
    }
    
    /**
     * 将图片信息转换为Map格式
     * 
     * @param imageInfo 图片信息对象
     * @return Map格式的图片信息
     */
    private Map<String, Object> convertImageInfoToMap(DocumentImageInfo imageInfo) {
        Map<String, Object> map = new HashMap<>();
        
        if (imageInfo == null) {
            map.put("totalPages", 0);
            map.put("pages", new ArrayList<>());
            return map;
        }
        
        map.put("totalPages", imageInfo.getTotalPages());
        
        List<Map<String, Object>> pagesList = new ArrayList<>();
        for (var page : imageInfo.getPages()) {
            Map<String, Object> pageMap = new HashMap<>();
            pageMap.put("pageNumber", page.getPageNumber());
            pageMap.put("imageUrl", page.getImageUrl());
            pageMap.put("width", page.getWidth());
            pageMap.put("height", page.getHeight());
            pagesList.add(pageMap);
        }
        map.put("pages", pagesList);
        
        return map;
    }
    
    /**
     * 从CharBox序列中提取文本
     * 
     * @param charBoxes 字符框列表
     * @param startIndex 起始索引
     * @param endIndex 结束索引
     * @return 提取的文本
     */
    public String extractTextFromCharBoxes(List<CharBox> charBoxes, int startIndex, int endIndex) {
        if (charBoxes == null || startIndex < 0 || endIndex > charBoxes.size()) {
            return "";
        }
        
        StringBuilder text = new StringBuilder();
        for (int i = startIndex; i < endIndex; i++) {
            text.append(charBoxes.get(i).ch);
        }
        
        return text.toString();
    }
    
    /**
     * 格式化差异统计信息
     * 
     * @param differences 差异列表
     * @return 统计信息Map
     */
    public Map<String, Object> formatDifferenceStatistics(List<Map<String, Object>> differences) {
        Map<String, Object> stats = new HashMap<>();
        
        int insertCount = 0;
        int deleteCount = 0;
        int modifyCount = 0;
        int ignoredCount = 0;
        
        for (Map<String, Object> diff : differences) {
            String operation = (String) diff.get("operation");
            
            switch (operation) {
                case "INSERT":
                    insertCount++;
                    break;
                case "DELETE":
                    deleteCount++;
                    break;
                case "MODIFY":
                    modifyCount++;
                    break;
                case "IGNORE":
                    ignoredCount++;
                    break;
            }
        }
        
        stats.put("total", differences.size());
        stats.put("insert", insertCount);
        stats.put("delete", deleteCount);
        stats.put("modify", modifyCount);
        stats.put("ignored", ignoredCount);
        stats.put("valid", insertCount + deleteCount + modifyCount);
        
        return stats;
    }
}

