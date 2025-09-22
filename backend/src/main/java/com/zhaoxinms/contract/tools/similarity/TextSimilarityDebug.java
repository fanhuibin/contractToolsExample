package com.zhaoxinms.contract.tools.similarity;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 调试文本相似度问题
 */
public class TextSimilarityDebug {
    
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    
    public static void main(String[] args) {
        String sourceText = "1.4合同期限为 _年，从201年0月01日到 小201年0月31日止";
        String candidateText = "1.4 合同期限为___年，从 2021年0月01日到 2021年0月31日止。";
        
        System.out.println("调试余弦相似度计算问题");
        System.out.println("=".repeat(50));
        
        System.out.println("源文本: \"" + sourceText + "\"");
        System.out.println("候选文本: \"" + candidateText + "\"");
        System.out.println();
        
        // 调试词汇分割
        debugWordSplit(sourceText, candidateText);
        
        // 测试修复后的算法
        double originalCosine = calculateOriginalCosineSimilarity(sourceText, candidateText);
        double improvedCosine = calculateImprovedCosineSimilarity(sourceText, candidateText);
        
        System.out.println("原始余弦相似度: " + originalCosine);
        System.out.println("改进后余弦相似度: " + improvedCosine);
    }
    
    private static void debugWordSplit(String text1, String text2) {
        System.out.println("词汇分割调试:");
        System.out.println("-".repeat(30));
        
        // 原始分割方法
        Set<String> words1 = getOriginalWordSet(text1);
        Set<String> words2 = getOriginalWordSet(text2);
        
        System.out.println("原始分割方法:");
        System.out.println("文本1词汇: " + words1);
        System.out.println("文本2词汇: " + words2);
        System.out.println("交集: " + getIntersection(words1, words2));
        System.out.println();
        
        // 改进的分割方法
        Set<String> improvedWords1 = getImprovedWordSet(text1);
        Set<String> improvedWords2 = getImprovedWordSet(text2);
        
        System.out.println("改进分割方法:");
        System.out.println("文本1词汇: " + improvedWords1);
        System.out.println("文本2词汇: " + improvedWords2);
        System.out.println("交集: " + getIntersection(improvedWords1, improvedWords2));
        System.out.println();
    }
    
    private static Set<String> getIntersection(Set<String> set1, Set<String> set2) {
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        return intersection;
    }
    
    private static Set<String> getOriginalWordSet(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new HashSet<>();
        }
        
        String normalized = normalizeText(text);
        String[] words = WHITESPACE_PATTERN.split(normalized);
        
        Set<String> wordSet = new HashSet<>();
        for (String word : words) {
            if (!word.isEmpty()) {
                wordSet.add(word);
            }
        }
        
        return wordSet;
    }
    
    private static Set<String> getImprovedWordSet(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new HashSet<>();
        }
        
        Set<String> wordSet = new HashSet<>();
        String normalized = normalizeText(text);
        
        // 按字符分割（对中文更友好）
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            if (!Character.isWhitespace(ch)) {
                wordSet.add(String.valueOf(ch));
            }
        }
        
        // 同时添加一些常见词汇模式
        addCommonPatterns(normalized, wordSet);
        
        return wordSet;
    }
    
    private static void addCommonPatterns(String text, Set<String> wordSet) {
        // 添加数字序列
        for (int i = 0; i < text.length(); i++) {
            if (Character.isDigit(text.charAt(i))) {
                StringBuilder number = new StringBuilder();
                int j = i;
                while (j < text.length() && Character.isDigit(text.charAt(j))) {
                    number.append(text.charAt(j));
                    j++;
                }
                if (number.length() > 1) {
                    wordSet.add(number.toString());
                }
                i = j - 1;
            }
        }
        
        // 添加常见词汇（如果以空格分割）
        String[] words = WHITESPACE_PATTERN.split(text);
        for (String word : words) {
            if (!word.isEmpty() && word.length() > 1) {
                wordSet.add(word);
            }
        }
    }
    
    private static double calculateOriginalCosineSimilarity(String text1, String text2) {
        Map<String, Integer> freq1 = getOriginalWordFrequency(text1);
        Map<String, Integer> freq2 = getOriginalWordFrequency(text2);
        
        return calculateCosineSimilarityFromFreq(freq1, freq2);
    }
    
    private static double calculateImprovedCosineSimilarity(String text1, String text2) {
        Map<String, Integer> freq1 = getImprovedWordFrequency(text1);
        Map<String, Integer> freq2 = getImprovedWordFrequency(text2);
        
        return calculateCosineSimilarityFromFreq(freq1, freq2);
    }
    
    private static Map<String, Integer> getOriginalWordFrequency(String text) {
        Map<String, Integer> frequency = new HashMap<>();
        
        if (text == null || text.trim().isEmpty()) {
            return frequency;
        }
        
        String normalized = normalizeText(text);
        String[] words = WHITESPACE_PATTERN.split(normalized);
        
        for (String word : words) {
            if (!word.isEmpty()) {
                frequency.put(word, frequency.getOrDefault(word, 0) + 1);
            }
        }
        
        return frequency;
    }
    
    private static Map<String, Integer> getImprovedWordFrequency(String text) {
        Map<String, Integer> frequency = new HashMap<>();
        
        if (text == null || text.trim().isEmpty()) {
            return frequency;
        }
        
        String normalized = normalizeText(text);
        
        // 字符级频率
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            if (!Character.isWhitespace(ch)) {
                String charStr = String.valueOf(ch);
                frequency.put(charStr, frequency.getOrDefault(charStr, 0) + 1);
            }
        }
        
        // 添加词汇级频率
        String[] words = WHITESPACE_PATTERN.split(normalized);
        for (String word : words) {
            if (!word.isEmpty() && word.length() > 1) {
                frequency.put(word, frequency.getOrDefault(word, 0) + 1);
            }
        }
        
        return frequency;
    }
    
    private static double calculateCosineSimilarityFromFreq(Map<String, Integer> freq1, Map<String, Integer> freq2) {
        if (freq1.isEmpty() && freq2.isEmpty()) return 1.0;
        if (freq1.isEmpty() || freq2.isEmpty()) return 0.0;
        
        Set<String> allWords = new HashSet<>();
        allWords.addAll(freq1.keySet());
        allWords.addAll(freq2.keySet());
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (String word : allWords) {
            int count1 = freq1.getOrDefault(word, 0);
            int count2 = freq2.getOrDefault(word, 0);
            
            dotProduct += count1 * count2;
            norm1 += count1 * count1;
            norm2 += count2 * count2;
        }
        
        if (norm1 == 0.0 || norm2 == 0.0) return 0.0;
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
    
    private static String normalizeText(String text) {
        if (text == null) return "";
        
        // 将连续的空白字符替换为单个空格
        String normalized = WHITESPACE_PATTERN.matcher(text.trim()).replaceAll(" ");
        
        return normalized;
    }
}
