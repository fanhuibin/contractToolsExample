package com.zhaoxinms.contract.tools.similarity;

import java.util.Arrays;
import java.util.List;

/**
 * 快速测试相似度算法
 */
public class QuickTest {
    
    public static void main(String[] args) {
        System.out.println("快速测试文本相似度算法");
        System.out.println("========================");
        
        String sourceText = "1.4合同期限为 _年，从201年0月01日到 小201年0月31日止";
        
        List<String> candidateTexts = Arrays.asList(
            "1.4 合同期限为___年，从 2021年0月01日到 2021年0月31日止。",
            "1.4 合同期限为___年，从 021年0月01日到 2021年0月31日止。",
            "1.4 合同期限为___年，从 201年0月01日到 2021年0月31日止。"
        );
        
        System.out.println("源文本: \"" + sourceText + "\"");
        System.out.println();
        
        for (int i = 0; i < candidateTexts.size(); i++) {
            String candidate = candidateTexts.get(i);
            
            double jaccard = TextSimilarityCalculator.calculateJaccardSimilarity(sourceText, candidate);
            double cosine = TextSimilarityCalculator.calculateCosineSimilarity(sourceText, candidate);
            double levenshtein = TextSimilarityCalculator.calculateLevenshteinSimilarity(sourceText, candidate);
            double character = TextSimilarityCalculator.calculateCharacterSimilarity(sourceText, candidate);
            
            System.out.println("候选文本 " + (i + 1) + ": \"" + candidate + "\"");
            System.out.println("  Jaccard相似度:     " + String.format("%.4f", jaccard));
            System.out.println("  余弦相似度:        " + String.format("%.4f", cosine));
            System.out.println("  编辑距离相似度:    " + String.format("%.4f", levenshtein));
            System.out.println("  字符相似度:        " + String.format("%.4f", character));
            System.out.println();
        }
        
        // 测试综合相似度排序
        System.out.println("综合相似度排序结果:");
        System.out.println("-".repeat(50));
        
        List<TextSimilarityCalculator.SimilarityResult> results = 
            TextSimilarityCalculator.calculateSimilarities(sourceText, candidateTexts);
        
        for (TextSimilarityCalculator.SimilarityResult result : results) {
            System.out.println("排名 " + result.getRank() + ": " + 
                String.format("%.4f", result.getOverallSimilarity()) + 
                " - \"" + result.getCandidateText() + "\"");
        }
    }
}
