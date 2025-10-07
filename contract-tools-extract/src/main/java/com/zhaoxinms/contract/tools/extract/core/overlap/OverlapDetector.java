package com.zhaoxinms.contract.tools.extract.core.overlap;

import com.zhaoxinms.contract.tools.extract.core.data.Extraction;
import com.zhaoxinms.contract.tools.extract.core.data.CharInterval;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 重叠检测器 - 实现LangExtract的重叠检测和非重叠合并功能
 * 用于处理多轮提取中可能出现的重叠结果
 */
@Slf4j
public class OverlapDetector {
    
    private static final double DEFAULT_OVERLAP_THRESHOLD = 0.3;
    private static final double HIGH_CONFIDENCE_THRESHOLD = 0.8;
    
    /**
     * 检测两个提取结果是否重叠
     */
    public boolean hasOverlap(Extraction extraction1, Extraction extraction2) {
        if (extraction1 == null || extraction2 == null) {
            return false;
        }
        
        CharInterval interval1 = extraction1.getCharInterval();
        CharInterval interval2 = extraction2.getCharInterval();
        
        if (interval1 == null || interval2 == null) {
            return false;
        }
        
        return interval1.overlapsWith(interval2);
    }
    
    /**
     * 检测两个提取结果是否显著重叠
     */
    public boolean hasSignificantOverlap(Extraction extraction1, Extraction extraction2) {
        return hasSignificantOverlap(extraction1, extraction2, DEFAULT_OVERLAP_THRESHOLD);
    }
    
    /**
     * 检测两个提取结果是否显著重叠（自定义阈值）
     */
    public boolean hasSignificantOverlap(Extraction extraction1, Extraction extraction2, double threshold) {
        if (!hasOverlap(extraction1, extraction2)) {
            return false;
        }
        
        CharInterval interval1 = extraction1.getCharInterval();
        CharInterval interval2 = extraction2.getCharInterval();
        
        double overlapRatio = interval1.getOverlapRatio(interval2);
        return overlapRatio >= threshold;
    }
    
    /**
     * 从多轮提取结果中合并非重叠的提取结果
     * 这是LangExtract的核心功能之一
     */
    public List<Extraction> mergeNonOverlappingExtractions(List<List<Extraction>> allPassExtractions) {
        if (allPassExtractions == null || allPassExtractions.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 将所有提取结果合并到一个列表中，并按字段分组
        Map<String, List<Extraction>> extractionsByField = new HashMap<>();
        
        for (int passIndex = 0; passIndex < allPassExtractions.size(); passIndex++) {
            List<Extraction> passExtractions = allPassExtractions.get(passIndex);
            if (passExtractions == null) continue;
            
            for (Extraction extraction : passExtractions) {
                if (extraction == null || extraction.getField() == null) continue;
                
                // 添加通过信息到元数据
                extraction.addMetadata("extraction_pass", passIndex + 1);
                
                extractionsByField.computeIfAbsent(extraction.getField(), k -> new ArrayList<>())
                    .add(extraction);
            }
        }
        
        List<Extraction> mergedResults = new ArrayList<>();
        
        // 对每个字段的提取结果进行去重和合并
        for (Map.Entry<String, List<Extraction>> entry : extractionsByField.entrySet()) {
            String fieldName = entry.getKey();
            List<Extraction> fieldExtractions = entry.getValue();
            
            log.debug("处理字段 '{}' 的 {} 个提取结果", fieldName, fieldExtractions.size());
            
            List<Extraction> nonOverlappingExtractions = resolveOverlappingExtractions(fieldExtractions);
            mergedResults.addAll(nonOverlappingExtractions);
        }
        
        // 按位置排序
        mergedResults.sort(this::compareByPosition);
        
        log.info("合并完成，最终结果包含 {} 个非重叠提取", mergedResults.size());
        
        return mergedResults;
    }
    
    /**
     * 解决单个字段的重叠提取结果
     */
    private List<Extraction> resolveOverlappingExtractions(List<Extraction> extractions) {
        if (extractions.size() <= 1) {
            return new ArrayList<>(extractions);
        }
        
        // 按位置排序
        List<Extraction> sortedExtractions = extractions.stream()
            .filter(e -> e.getCharInterval() != null && e.getCharInterval().isValid())
            .sorted(this::compareByPosition)
            .collect(Collectors.toList());
        
        List<Extraction> result = new ArrayList<>();
        
        for (Extraction current : sortedExtractions) {
            boolean shouldAdd = true;
            List<Extraction> toRemove = new ArrayList<>();
            
            // 检查与已有结果的重叠
            for (Extraction existing : result) {
                if (hasSignificantOverlap(current, existing)) {
                    // 选择更好的提取结果
                    Extraction better = chooseBetterExtraction(current, existing);
                    
                    if (better == current) {
                        toRemove.add(existing);
                    } else {
                        shouldAdd = false;
                        break;
                    }
                }
            }
            
            if (shouldAdd) {
                result.removeAll(toRemove);
                result.add(current);
            }
        }
        
        return result;
    }
    
    /**
     * 在两个重叠的提取结果中选择更好的一个
     */
    private Extraction chooseBetterExtraction(Extraction extraction1, Extraction extraction2) {
        // 1. 优先选择置信度更高的
        Double conf1 = extraction1.getConfidence();
        Double conf2 = extraction2.getConfidence();
        
        if (conf1 != null && conf2 != null) {
            if (Math.abs(conf1 - conf2) > 0.1) {
                return conf1 > conf2 ? extraction1 : extraction2;
            }
        }
        
        // 2. 优先选择对齐置信度更高的
        Double alignConf1 = extraction1.getAlignmentConfidence();
        Double alignConf2 = extraction2.getAlignmentConfidence();
        
        if (alignConf1 != null && alignConf2 != null) {
            if (Math.abs(alignConf1 - alignConf2) > 0.1) {
                return alignConf1 > alignConf2 ? extraction1 : extraction2;
            }
        }
        
        // 3. 优先选择文本更长的（通常更完整）
        int len1 = extraction1.getTextLength();
        int len2 = extraction2.getTextLength();
        
        if (Math.abs(len1 - len2) > 5) {
            return len1 > len2 ? extraction1 : extraction2;
        }
        
        // 4. 优先选择来自更早轮次的（通常更准确）
        Integer pass1 = (Integer) extraction1.getMetadata("extraction_pass");
        Integer pass2 = (Integer) extraction2.getMetadata("extraction_pass");
        
        if (pass1 != null && pass2 != null) {
            return pass1 <= pass2 ? extraction1 : extraction2;
        }
        
        // 5. 默认选择第一个
        return extraction1;
    }
    
    /**
     * 按位置比较提取结果
     */
    private int compareByPosition(Extraction e1, Extraction e2) {
        Integer start1 = e1.getStartPositionFromInterval();
        Integer start2 = e2.getStartPositionFromInterval();
        
        if (start1 == null && start2 == null) return 0;
        if (start1 == null) return 1;
        if (start2 == null) return -1;
        
        int startComparison = start1.compareTo(start2);
        if (startComparison != 0) return startComparison;
        
        // 如果起始位置相同，按结束位置排序
        Integer end1 = e1.getEndPositionFromInterval();
        Integer end2 = e2.getEndPositionFromInterval();
        
        if (end1 == null && end2 == null) return 0;
        if (end1 == null) return 1;
        if (end2 == null) return -1;
        
        return end1.compareTo(end2);
    }
    
    /**
     * 查找所有重叠的提取结果组
     */
    public List<List<Extraction>> findOverlappingGroups(List<Extraction> extractions) {
        List<List<Extraction>> groups = new ArrayList<>();
        Set<Extraction> processed = new HashSet<>();
        
        for (Extraction extraction : extractions) {
            if (processed.contains(extraction)) continue;
            
            List<Extraction> group = new ArrayList<>();
            group.add(extraction);
            processed.add(extraction);
            
            // 查找所有与当前提取结果重叠的其他结果
            for (Extraction other : extractions) {
                if (processed.contains(other)) continue;
                
                boolean overlapWithGroup = false;
                for (Extraction groupMember : group) {
                    if (hasSignificantOverlap(groupMember, other)) {
                        overlapWithGroup = true;
                        break;
                    }
                }
                
                if (overlapWithGroup) {
                    group.add(other);
                    processed.add(other);
                }
            }
            
            groups.add(group);
        }
        
        return groups;
    }
    
    /**
     * 计算重叠统计信息
     */
    public OverlapStatistics calculateOverlapStatistics(List<Extraction> extractions) {
        int totalExtractions = extractions.size();
        int overlappingCount = 0;
        int highConfidenceCount = 0;
        double averageOverlapRatio = 0.0;
        
        List<List<Extraction>> groups = findOverlappingGroups(extractions);
        
        for (List<Extraction> group : groups) {
            if (group.size() > 1) {
                overlappingCount += group.size();
                
                // 计算组内平均重叠比例
                double groupOverlapSum = 0.0;
                int pairCount = 0;
                
                for (int i = 0; i < group.size(); i++) {
                    for (int j = i + 1; j < group.size(); j++) {
                        Extraction e1 = group.get(i);
                        Extraction e2 = group.get(j);
                        groupOverlapSum += e1.getOverlapRatio(e2);
                        pairCount++;
                    }
                }
                
                if (pairCount > 0) {
                    averageOverlapRatio += groupOverlapSum / pairCount;
                }
            }
            
            // 统计高置信度提取
            for (Extraction extraction : group) {
                if (extraction.isConfidentEnough(HIGH_CONFIDENCE_THRESHOLD)) {
                    highConfidenceCount++;
                }
            }
        }
        
        if (groups.size() > 0) {
            averageOverlapRatio /= groups.size();
        }
        
        return new OverlapStatistics(
            totalExtractions,
            overlappingCount,
            groups.size(),
            highConfidenceCount,
            averageOverlapRatio
        );
    }
    
    /**
     * 重叠统计信息
     */
    public static class OverlapStatistics {
        public final int totalExtractions;
        public final int overlappingExtractions;
        public final int overlapGroups;
        public final int highConfidenceExtractions;
        public final double averageOverlapRatio;
        
        public OverlapStatistics(int totalExtractions, int overlappingExtractions, 
                               int overlapGroups, int highConfidenceExtractions, 
                               double averageOverlapRatio) {
            this.totalExtractions = totalExtractions;
            this.overlappingExtractions = overlappingExtractions;
            this.overlapGroups = overlapGroups;
            this.highConfidenceExtractions = highConfidenceExtractions;
            this.averageOverlapRatio = averageOverlapRatio;
        }
        
        @Override
        public String toString() {
            return String.format(
                "OverlapStatistics{total=%d, overlapping=%d, groups=%d, highConf=%d, avgOverlap=%.2f}",
                totalExtractions, overlappingExtractions, overlapGroups, 
                highConfidenceExtractions, averageOverlapRatio
            );
        }
    }
}
