package com.zhaoxinms.contract.tools.similarity;

import java.util.Arrays;
import java.util.List;

/**
 * 文本相似度算法测试类
 */
public class TextSimilarityTest {
    
    public static void main(String[] args) {
        // 您提供的测试数据
        String sourceText = "1.4合同期限为 _年，从201年0月01日到 小201年0月31日止";
        
        List<String> candidateTexts = Arrays.asList(
            "1.4 合同期限为___年，从 2021年0月01日到 2021年0月31日止。",
            "1.4 合同期限为___年，从 021年0月01日到 2021年0月31日止。",
            "1.4 合同期限为___年，从 201年0月01日到 2021年0月31日止。",
            "1.4 合同期限为___年，从 2021年0月01日到 021年0月31日止。",
            "1.4 合同期限为___年，从 2021年0月01日到 201年0月31日止。"
        );
        
        System.out.println("测试文本相似度算法");
        System.out.println("==================");
        
        // 生成详细报告
        String report = TextSimilarityCalculator.generateDetailedReport(sourceText, candidateTexts);
        System.out.println(report);
        
        // 获取排序结果
        List<TextSimilarityCalculator.SimilarityResult> results = 
            TextSimilarityCalculator.calculateSimilarities(sourceText, candidateTexts);
        
        System.out.println("排序结果概览:");
        System.out.println("-".repeat(80));
        for (TextSimilarityCalculator.SimilarityResult result : results) {
            System.out.println(result);
        }
        
        // 显示最相似的文本
        if (!results.isEmpty()) {
            TextSimilarityCalculator.SimilarityResult best = results.get(0);
            System.out.println("\n最相似的文本:");
            System.out.println("综合相似度: " + String.format("%.3f", best.getOverallSimilarity()));
            System.out.println("文本: \"" + best.getCandidateText() + "\"");
        }
        
        // 测试各种相似度算法
        System.out.println("\n=".repeat(80));
        System.out.println("各算法详细对比:");
        System.out.println("=".repeat(80));
        
        for (int i = 0; i < candidateTexts.size(); i++) {
            String candidate = candidateTexts.get(i);
            System.out.println("候选文本 " + (i + 1) + ": \"" + candidate + "\"");
            
            double jaccard = TextSimilarityCalculator.calculateJaccardSimilarity(sourceText, candidate);
            double cosine = TextSimilarityCalculator.calculateCosineSimilarity(sourceText, candidate);
            double levenshtein = TextSimilarityCalculator.calculateLevenshteinSimilarity(sourceText, candidate);
            double character = TextSimilarityCalculator.calculateCharacterSimilarity(sourceText, candidate);
            
            System.out.println("  Jaccard相似度:     " + String.format("%.3f", jaccard));
            System.out.println("  余弦相似度:        " + String.format("%.3f", cosine));
            System.out.println("  编辑距离相似度:    " + String.format("%.3f", levenshtein));
            System.out.println("  字符相似度:        " + String.format("%.3f", character));
            System.out.println();
        }
    }
    
    /**
     * 测试边界情况
     */
    public static void testEdgeCases() {
        System.out.println("测试边界情况:");
        System.out.println("-".repeat(40));
        
        // 完全相同的文本
        String text = "测试文本";
        System.out.println("相同文本相似度: " + 
            TextSimilarityCalculator.calculateCharacterSimilarity(text, text));
        
        // 空文本
        System.out.println("空文本相似度: " + 
            TextSimilarityCalculator.calculateCharacterSimilarity("", ""));
        
        // 一个为空
        System.out.println("一个空文本相似度: " + 
            TextSimilarityCalculator.calculateCharacterSimilarity("测试", ""));
        
        // 完全不同的文本
        System.out.println("完全不同文本相似度: " + 
            TextSimilarityCalculator.calculateCharacterSimilarity("ABCD", "1234"));
    }
}
