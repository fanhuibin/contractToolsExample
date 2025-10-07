package com.zhaoxinms.contract.tools.extract.core.alignment;

import com.zhaoxinms.contract.tools.extract.core.data.CharInterval;
import lombok.extern.slf4j.Slf4j;

/**
 * 文本对齐器 - 实现LangExtract的核心字符级位置锚定功能
 * 简化版本，专注于核心的字符级对齐，能够正确处理中文
 */
@Slf4j
public class TextAligner {
    
    private static final double MIN_ALIGNMENT_CONFIDENCE = 0.3;
    private static final double EXACT_MATCH_CONFIDENCE = 1.0;
    private static final double FUZZY_MATCH_CONFIDENCE = 0.8;
    private static final double PARTIAL_MATCH_CONFIDENCE = 0.6;
    
    /**
     * 在源文本中查找提取文本的精确位置 - 按照Google LangExtract的逻辑实现
     * 
     * @param sourceText 原始文档文本
     * @param extractedText 提取的文本内容
     * @return 字符区间，如果找不到则返回null
     */
    public CharInterval findTextPosition(String sourceText, String extractedText) {
        if (sourceText == null || extractedText == null || extractedText.trim().isEmpty()) {
            return null;
        }
        
        // 按照Google的实现，优先使用精确匹配，无复杂的文本清理
        // 1. 直接精确匹配（最高优先级）
        CharInterval exactMatch = findDirectExactMatch(sourceText, extractedText);
        if (exactMatch != null) {
            log.debug("找到直接精确匹配: {}", exactMatch);
            return exactMatch;
        }
        
        // 2. 去除首尾空格后匹配
        String trimmedExtracted = extractedText.trim();
        if (!trimmedExtracted.equals(extractedText)) {
            CharInterval trimmedMatch = findDirectExactMatch(sourceText, trimmedExtracted);
            if (trimmedMatch != null) {
                log.debug("找到去空格精确匹配: {}", trimmedMatch);
                return trimmedMatch;
            }
        }
        
        // 3. 标准化空格后匹配
        String normalizedExtracted = normalizeSpaces(extractedText);
        String normalizedSource = normalizeSpaces(sourceText);
        CharInterval normalizedMatch = findNormalizedMatch(sourceText, normalizedSource, normalizedExtracted);
        if (normalizedMatch != null) {
            log.debug("找到标准化匹配: {}", normalizedMatch);
            return normalizedMatch;
        }
        
        // 4. 模糊匹配（允许少量字符差异）
        CharInterval fuzzyMatch = findBestFuzzyMatch(sourceText, extractedText);
        if (fuzzyMatch != null) {
            log.debug("找到模糊匹配: {}", fuzzyMatch);
            return fuzzyMatch;
        }
        
        // 5. 子串匹配（最后选择）
        CharInterval substringMatch = findBestSubstringMatch(sourceText, extractedText);
        if (substringMatch != null) {
            log.debug("找到子串匹配: {}", substringMatch);
            return substringMatch;
        }
        
        log.warn("无法在源文本中找到提取文本的位置: {}", extractedText);
        return null;
    }
    
    /**
     * 直接精确匹配 - 按照Google实现
     */
    private CharInterval findDirectExactMatch(String sourceText, String extractedText) {
        int index = sourceText.indexOf(extractedText);
        if (index != -1) {
            return CharInterval.builder()
                .startPos(index)
                .endPos(index + extractedText.length())
                .sourceText(extractedText)
                .alignmentConfidence(EXACT_MATCH_CONFIDENCE)
                .build();
        }
        return null;
    }
    
    /**
     * 标准化空格（按照Google的逻辑）
     */
    private String normalizeSpaces(String text) {
        return text.replaceAll("\\s+", " ");
    }
    
    /**
     * 标准化匹配 - 在标准化后的文本中查找，但返回原文位置
     */
    private CharInterval findNormalizedMatch(String originalText, String normalizedSource, String normalizedExtracted) {
        int normalizedIndex = normalizedSource.indexOf(normalizedExtracted);
        if (normalizedIndex == -1) {
            return null;
        }
        
        // 将标准化后的位置映射回原文位置
        int[] originalPositions = mapNormalizedToOriginal(originalText, normalizedSource, normalizedIndex, normalizedExtracted.length());
        if (originalPositions != null) {
            String actualText = originalText.substring(originalPositions[0], originalPositions[1]);
            return CharInterval.builder()
                .startPos(originalPositions[0])
                .endPos(originalPositions[1])
                .sourceText(actualText)
                .alignmentConfidence(EXACT_MATCH_CONFIDENCE * 0.95) // 轻微降低置信度
                .build();
        }
        
        return null;
    }
    
    /**
     * 将标准化文本的位置映射回原文位置
     */
    private int[] mapNormalizedToOriginal(String originalText, String normalizedText, int normalizedStart, int normalizedLength) {
        int originalIndex = 0;
        int normalizedIndex = 0;
        int mappedStart = -1;
        int mappedEnd = -1;
        
        // 找到开始位置
        while (originalIndex < originalText.length() && normalizedIndex < normalizedText.length()) {
            if (normalizedIndex == normalizedStart) {
                mappedStart = originalIndex;
                break;
            }
            
            char originalChar = originalText.charAt(originalIndex);
            char normalizedChar = normalizedText.charAt(normalizedIndex);
            
            if (Character.isWhitespace(originalChar)) {
                // 原文中的空白字符可能被标准化为单个空格
                originalIndex++;
                if (normalizedChar == ' ') {
                    normalizedIndex++;
                }
            } else if (originalChar == normalizedChar) {
                originalIndex++;
                normalizedIndex++;
            } else {
                return null; // 字符不匹配
            }
        }
        
        if (mappedStart == -1) {
            return null;
        }
        
        // 找到结束位置
        int targetNormalizedEnd = normalizedStart + normalizedLength;
        while (originalIndex < originalText.length() && normalizedIndex < targetNormalizedEnd) {
            char originalChar = originalText.charAt(originalIndex);
            char normalizedChar = normalizedIndex < normalizedText.length() ? normalizedText.charAt(normalizedIndex) : ' ';
            
            if (Character.isWhitespace(originalChar)) {
                originalIndex++;
                if (normalizedChar == ' ') {
                    normalizedIndex++;
                }
            } else if (originalChar == normalizedChar) {
                originalIndex++;
                normalizedIndex++;
            } else {
                break;
            }
        }
        
        mappedEnd = originalIndex;
        return new int[]{mappedStart, mappedEnd};
    }
    
    /**
     * 最佳模糊匹配 - 按照Google的滑动窗口算法
     */
    private CharInterval findBestFuzzyMatch(String sourceText, String extractedText) {
        int minDistance = Integer.MAX_VALUE;
        int bestStart = -1;
        int searchLength = extractedText.length();
        
        // 允许20%的编辑距离
        double maxAllowedDistance = Math.max(1, searchLength * 0.2);
        
        // 滑动窗口搜索
        for (int i = 0; i <= sourceText.length() - searchLength; i++) {
            String window = sourceText.substring(i, i + searchLength);
            int distance = calculateEditDistance(extractedText, window);
            
            if (distance < minDistance && distance <= maxAllowedDistance) {
                minDistance = distance;
                bestStart = i;
            }
        }
        
        if (bestStart != -1) {
            double confidence = 1.0 - (double) minDistance / searchLength;
            confidence = Math.max(confidence, MIN_ALIGNMENT_CONFIDENCE);
            
            String actualText = sourceText.substring(bestStart, bestStart + searchLength);
            return CharInterval.builder()
                .startPos(bestStart)
                .endPos(bestStart + searchLength)
                .sourceText(actualText)
                .alignmentConfidence(confidence * FUZZY_MATCH_CONFIDENCE)
                .build();
        }
        
        return null;
    }
    
    /**
     * 最佳子串匹配 - 查找最长公共子串
     */
    private CharInterval findBestSubstringMatch(String sourceText, String extractedText) {
        String longestCommon = findLongestCommonSubstring(sourceText, extractedText);
        
        // 至少需要匹配50%的内容，或者至少3个字符
        int minLength = Math.max(3, (int)(extractedText.length() * 0.5));
        
        if (longestCommon.length() >= minLength) {
            int index = sourceText.indexOf(longestCommon);
            if (index != -1) {
                double confidence = (double) longestCommon.length() / extractedText.length();
                confidence = Math.max(confidence, MIN_ALIGNMENT_CONFIDENCE);
                
                return CharInterval.builder()
                    .startPos(index)
                    .endPos(index + longestCommon.length())
                    .sourceText(longestCommon)
                    .alignmentConfidence(confidence * PARTIAL_MATCH_CONFIDENCE)
                    .build();
            }
        }
        
        return null;
    }
    
    
    /**
     * 查找最长公共子串
     */
    private String findLongestCommonSubstring(String s1, String s2) {
        int m = s1.length();
        int n = s2.length();
        
        int maxLength = 0;
        int endPos = 0;
        
        int[][] dp = new int[m + 1][n + 1];
        
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                    if (dp[i][j] > maxLength) {
                        maxLength = dp[i][j];
                        endPos = i;
                    }
                }
            }
        }
        
        return maxLength > 0 ? s1.substring(endPos - maxLength, endPos) : "";
    }
    
    /**
     * 关键词匹配 - 基于字符子串查找，适用于中文
     */
    private CharInterval findKeywordMatch(String sourceText, String extractedText) {
        // 对于短文本，直接查找子串
        if (extractedText.length() <= 3) {
            int index = sourceText.indexOf(extractedText);
            if (index != -1) {
                return CharInterval.builder()
                    .startPos(index)
                    .endPos(index + extractedText.length())
                    .sourceText(extractedText)
                    .alignmentConfidence(0.5) // 短文本匹配置信度中等
                    .build();
            }
        }
        
        // 查找包含最多字符的区域
        int windowSize = Math.min(extractedText.length() * 2, 100);
        int bestStart = -1;
        int maxMatchCount = 0;
        
        for (int i = 0; i <= sourceText.length() - windowSize; i++) {
            String window = sourceText.substring(i, i + windowSize);
            int matchCount = 0;
            
            // 统计匹配的字符数
            for (char c : extractedText.toCharArray()) {
                if (window.indexOf(c) != -1) {
                    matchCount++;
                }
            }
            
            if (matchCount > maxMatchCount) {
                maxMatchCount = matchCount;
                bestStart = i;
            }
        }
        
        if (bestStart != -1 && maxMatchCount > extractedText.length() * 0.3) {
            double confidence = (double) maxMatchCount / extractedText.length();
            confidence = Math.max(confidence, MIN_ALIGNMENT_CONFIDENCE);
            
            return CharInterval.builder()
                .startPos(bestStart)
                .endPos(bestStart + windowSize)
                .sourceText(sourceText.substring(bestStart, bestStart + windowSize))
                .alignmentConfidence(confidence * 0.4) // 关键词匹配置信度较低
                .build();
        }
        
        return null;
    }
    
    
    
    /**
     * 计算编辑距离（Levenshtein距离）
     */
    private int calculateEditDistance(String s1, String s2) {
        int m = s1.length();
        int n = s2.length();
        
        int[][] dp = new int[m + 1][n + 1];
        
        for (int i = 0; i <= m; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= n; j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]);
                }
            }
        }
        
        return dp[m][n];
    }
    
    /**
     * 计算对齐置信度
     */
    public double calculateAlignmentConfidence(String sourceText, String extractedText, CharInterval interval) {
        if (interval == null || !interval.isValid()) {
            return 0.0;
        }
        
        String actualText = interval.getSourceText();
        if (actualText == null) {
            return 0.0;
        }
        
        // 基于编辑距离计算相似度
        String normalizedExtracted = normalizeSpaces(extractedText);
        String normalizedActual = normalizeSpaces(actualText);
        
        int editDistance = calculateEditDistance(normalizedExtracted, normalizedActual);
        int maxLength = Math.max(normalizedExtracted.length(), normalizedActual.length());
        
        if (maxLength == 0) {
            return 1.0;
        }
        
        double similarity = 1.0 - (double) editDistance / maxLength;
        return Math.max(similarity, 0.0);
    }
}
