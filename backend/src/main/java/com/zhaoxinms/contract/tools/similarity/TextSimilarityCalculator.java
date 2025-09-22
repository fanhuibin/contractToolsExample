package com.zhaoxinms.contract.tools.similarity;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 文本相似度计算器
 * 提供多种相似度算法来比较文本相似性
 */
public class TextSimilarityCalculator {
    
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    private static final Pattern PUNCTUATION_PATTERN = Pattern.compile("[\\p{Punct}\\s]+");
    
    /**
     * 相似度计算结果
     */
    public static class SimilarityResult {
        private final String candidateText;
        private final double jaccardSimilarity;
        private final double cosineSimilarity;
        private final double levenshteinSimilarity;
        private final double characterSimilarity;
        private final double overallSimilarity;
        private final int rank;
        
        public SimilarityResult(String candidateText, double jaccardSimilarity, 
                              double cosineSimilarity, double levenshteinSimilarity,
                              double characterSimilarity, double overallSimilarity, int rank) {
            this.candidateText = candidateText;
            this.jaccardSimilarity = jaccardSimilarity;
            this.cosineSimilarity = cosineSimilarity;
            this.levenshteinSimilarity = levenshteinSimilarity;
            this.characterSimilarity = characterSimilarity;
            this.overallSimilarity = overallSimilarity;
            this.rank = rank;
        }
        
        // Getters
        public String getCandidateText() { return candidateText; }
        public double getJaccardSimilarity() { return jaccardSimilarity; }
        public double getCosineSimilarity() { return cosineSimilarity; }
        public double getLevenshteinSimilarity() { return levenshteinSimilarity; }
        public double getCharacterSimilarity() { return characterSimilarity; }
        public double getOverallSimilarity() { return overallSimilarity; }
        public int getRank() { return rank; }
        
        @Override
        public String toString() {
            return String.format("排名: %d, 综合相似度: %.3f, 文本: \"%s\"", 
                rank, overallSimilarity, candidateText);
        }
        
        public String getDetailedReport() {
            return String.format(
                "排名: %d\n" +
                "文本: \"%s\"\n" +
                "综合相似度: %.3f\n" +
                "  - Jaccard相似度: %.3f\n" +
                "  - 余弦相似度: %.3f\n" +
                "  - 编辑距离相似度: %.3f\n" +
                "  - 字符相似度: %.3f\n",
                rank, candidateText, overallSimilarity,
                jaccardSimilarity, cosineSimilarity, levenshteinSimilarity, characterSimilarity
            );
        }
    }
    
    /**
     * 计算源文本与多个候选文本的相似度，并按相似度排序
     * 
     * @param sourceText 源文本
     * @param candidateTexts 候选文本列表
     * @return 按相似度排序的结果列表（从高到低）
     */
    public static List<SimilarityResult> calculateSimilarities(String sourceText, List<String> candidateTexts) {
        if (sourceText == null || candidateTexts == null || candidateTexts.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<SimilarityResult> results = new ArrayList<>();
        
        for (String candidate : candidateTexts) {
            if (candidate == null) continue;
            
            double jaccard = calculateJaccardSimilarity(sourceText, candidate);
            double cosine = calculateCosineSimilarity(sourceText, candidate);
            double levenshtein = calculateLevenshteinSimilarity(sourceText, candidate);
            double character = calculateCharacterSimilarity(sourceText, candidate);
            
            // 加权计算综合相似度
            double overall = calculateOverallSimilarity(jaccard, cosine, levenshtein, character);
            
            results.add(new SimilarityResult(candidate, jaccard, cosine, levenshtein, character, overall, 0));
        }
        
        // 按综合相似度排序（从高到低）
        results.sort((a, b) -> Double.compare(b.getOverallSimilarity(), a.getOverallSimilarity()));
        
        // 设置排名
        for (int i = 0; i < results.size(); i++) {
            SimilarityResult old = results.get(i);
            results.set(i, new SimilarityResult(
                old.getCandidateText(),
                old.getJaccardSimilarity(),
                old.getCosineSimilarity(),
                old.getLevenshteinSimilarity(),
                old.getCharacterSimilarity(),
                old.getOverallSimilarity(),
                i + 1
            ));
        }
        
        return results;
    }
    
    /**
     * 加权计算综合相似度
     */
    private static double calculateOverallSimilarity(double jaccard, double cosine, double levenshtein, double character) {
        // 权重分配：字符相似度40%，编辑距离30%，Jaccard 20%，余弦10%
        return character * 0.4 + levenshtein * 0.3 + jaccard * 0.2 + cosine * 0.1;
    }
    
    /**
     * 计算Jaccard相似度（基于词汇集合）
     */
    public static double calculateJaccardSimilarity(String text1, String text2) {
        Set<String> set1 = getWordSet(text1);
        Set<String> set2 = getWordSet(text2);
        
        if (set1.isEmpty() && set2.isEmpty()) return 1.0;
        if (set1.isEmpty() || set2.isEmpty()) return 0.0;
        
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);
        
        return (double) intersection.size() / union.size();
    }
    
    /**
     * 计算余弦相似度（基于词频向量）
     */
    public static double calculateCosineSimilarity(String text1, String text2) {
        Map<String, Integer> freq1 = getWordFrequency(text1);
        Map<String, Integer> freq2 = getWordFrequency(text2);
        
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
    
    /**
     * 计算编辑距离相似度（Levenshtein距离的归一化）
     */
    public static double calculateLevenshteinSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) return 0.0;
        if (text1.equals(text2)) return 1.0;
        
        int distance = calculateLevenshteinDistance(text1, text2);
        int maxLength = Math.max(text1.length(), text2.length());
        
        if (maxLength == 0) return 1.0;
        
        return 1.0 - (double) distance / maxLength;
    }
    
    /**
     * 计算字符级相似度（考虑字符顺序和位置）
     */
    public static double calculateCharacterSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) return 0.0;
        if (text1.equals(text2)) return 1.0;
        
        // 预处理：标准化空白字符
        String norm1 = normalizeText(text1);
        String norm2 = normalizeText(text2);
        
        if (norm1.equals(norm2)) return 1.0;
        
        // 计算最长公共子序列长度
        int lcsLength = calculateLCS(norm1, norm2);
        int maxLength = Math.max(norm1.length(), norm2.length());
        
        if (maxLength == 0) return 1.0;
        
        // 基于LCS的相似度
        double lcsSimilarity = (double) lcsLength / maxLength;
        
        // 计算字符匹配度（考虑位置）
        double positionSimilarity = calculatePositionSimilarity(norm1, norm2);
        
        // 组合两种相似度
        return lcsSimilarity * 0.7 + positionSimilarity * 0.3;
    }
    
    /**
     * 计算位置相似度（考虑字符在相似位置的匹配）
     */
    private static double calculatePositionSimilarity(String text1, String text2) {
        int minLength = Math.min(text1.length(), text2.length());
        int maxLength = Math.max(text1.length(), text2.length());
        
        if (maxLength == 0) return 1.0;
        
        int matches = 0;
        for (int i = 0; i < minLength; i++) {
            if (text1.charAt(i) == text2.charAt(i)) {
                matches++;
            }
        }
        
        return (double) matches / maxLength;
    }
    
    /**
     * 计算最长公共子序列长度
     */
    private static int calculateLCS(String text1, String text2) {
        int m = text1.length();
        int n = text2.length();
        
        int[][] dp = new int[m + 1][n + 1];
        
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (text1.charAt(i - 1) == text2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }
        
        return dp[m][n];
    }
    
    /**
     * 计算编辑距离（Levenshtein距离）
     */
    private static int calculateLevenshteinDistance(String text1, String text2) {
        int m = text1.length();
        int n = text2.length();
        
        int[][] dp = new int[m + 1][n + 1];
        
        // 初始化边界条件
        for (int i = 0; i <= m; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= n; j++) {
            dp[0][j] = j;
        }
        
        // 填充动态规划表
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (text1.charAt(i - 1) == text2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]);
                }
            }
        }
        
        return dp[m][n];
    }
    
    /**
     * 获取文本的词汇集合（改进的中文分词）
     */
    private static Set<String> getWordSet(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new HashSet<>();
        }
        
        Set<String> wordSet = new HashSet<>();
        String normalized = normalizeText(text);
        
        // 1. 字符级分割（对中文文本特别重要）
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            if (!Character.isWhitespace(ch)) {
                wordSet.add(String.valueOf(ch));
            }
        }
        
        // 2. 空格分割的词汇
        String[] words = WHITESPACE_PATTERN.split(normalized);
        for (String word : words) {
            if (!word.isEmpty()) {
                wordSet.add(word);
            }
        }
        
        // 3. 数字序列
        addNumberSequences(normalized, wordSet);
        
        // 4. 标点符号和特殊字符组合
        addPunctuationPatterns(normalized, wordSet);
        
        return wordSet;
    }
    
    /**
     * 获取文本的词频统计（改进的中文分词）
     */
    private static Map<String, Integer> getWordFrequency(String text) {
        Map<String, Integer> frequency = new HashMap<>();
        
        if (text == null || text.trim().isEmpty()) {
            return frequency;
        }
        
        String normalized = normalizeText(text);
        
        // 1. 字符级频率
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            if (!Character.isWhitespace(ch)) {
                String charStr = String.valueOf(ch);
                frequency.put(charStr, frequency.getOrDefault(charStr, 0) + 1);
            }
        }
        
        // 2. 空格分割的词汇频率
        String[] words = WHITESPACE_PATTERN.split(normalized);
        for (String word : words) {
            if (!word.isEmpty()) {
                frequency.put(word, frequency.getOrDefault(word, 0) + 1);
            }
        }
        
        // 3. 数字序列频率
        addNumberSequencesToFreq(normalized, frequency);
        
        return frequency;
    }
    
    /**
     * 标准化文本（去除多余空白，保留基本结构）
     */
    private static String normalizeText(String text) {
        if (text == null) return "";
        
        // 将连续的空白字符替换为单个空格
        String normalized = WHITESPACE_PATTERN.matcher(text.trim()).replaceAll(" ");
        
        return normalized;
    }
    
    /**
     * 生成详细的相似度分析报告
     */
    public static String generateDetailedReport(String sourceText, List<String> candidateTexts) {
        List<SimilarityResult> results = calculateSimilarities(sourceText, candidateTexts);
        
        StringBuilder report = new StringBuilder();
        report.append("=".repeat(80)).append("\n");
        report.append("文本相似度分析报告\n");
        report.append("=".repeat(80)).append("\n");
        report.append("源文本: \"").append(sourceText).append("\"\n");
        report.append("-".repeat(80)).append("\n");
        
        for (SimilarityResult result : results) {
            report.append(result.getDetailedReport()).append("\n");
        }
        
        return report.toString();
    }
    
    /**
     * 添加数字序列到词汇集合
     */
    private static void addNumberSequences(String text, Set<String> wordSet) {
        StringBuilder number = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (Character.isDigit(ch)) {
                number.append(ch);
            } else {
                if (number.length() > 0) {
                    wordSet.add(number.toString());
                    number.setLength(0);
                }
            }
        }
        if (number.length() > 0) {
            wordSet.add(number.toString());
        }
    }
    
    /**
     * 添加标点符号模式到词汇集合
     */
    private static void addPunctuationPatterns(String text, Set<String> wordSet) {
        // 添加连续的下划线、点号等特殊字符
        StringBuilder special = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '_' || ch == '.' || ch == '，' || ch == '。' || ch == '：' || ch == ':') {
                special.append(ch);
            } else {
                if (special.length() > 0) {
                    wordSet.add(special.toString());
                    special.setLength(0);
                }
            }
        }
        if (special.length() > 0) {
            wordSet.add(special.toString());
        }
    }
    
    /**
     * 添加数字序列到频率统计
     */
    private static void addNumberSequencesToFreq(String text, Map<String, Integer> frequency) {
        StringBuilder number = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (Character.isDigit(ch)) {
                number.append(ch);
            } else {
                if (number.length() > 0) {
                    String numStr = number.toString();
                    frequency.put(numStr, frequency.getOrDefault(numStr, 0) + 1);
                    number.setLength(0);
                }
            }
        }
        if (number.length() > 0) {
            String numStr = number.toString();
            frequency.put(numStr, frequency.getOrDefault(numStr, 0) + 1);
        }
    }
}
