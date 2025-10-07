package com.zhaoxinms.contract.tools.extract.utils;

import lombok.extern.slf4j.Slf4j;

/**
 * 分块大小计算器
 * 帮助用户根据不同场景选择最优的maxCharBuffer设置
 */
@Slf4j
public class ChunkSizeCalculator {
    
    /**
     * 根据文档特征和性能需求计算最优块大小
     */
    public static class ChunkSizeRecommendation {
        private final int recommendedSize;
        private final int estimatedChunks;
        private final String reasoning;
        private final PerformanceImpact performance;
        
        public ChunkSizeRecommendation(int recommendedSize, int estimatedChunks, 
                                     String reasoning, PerformanceImpact performance) {
            this.recommendedSize = recommendedSize;
            this.estimatedChunks = estimatedChunks;
            this.reasoning = reasoning;
            this.performance = performance;
        }
        
        public int getRecommendedSize() { return recommendedSize; }
        public int getEstimatedChunks() { return estimatedChunks; }
        public String getReasoning() { return reasoning; }
        public PerformanceImpact getPerformance() { return performance; }
        
        @Override
        public String toString() {
            return String.format("推荐块大小: %d字符, 预计块数: %d, 原因: %s, 性能影响: %s",
                recommendedSize, estimatedChunks, reasoning, performance);
        }
    }
    
    public enum PerformanceImpact {
        HIGH_SPEED("高速度，可能影响准确性"),
        BALANCED("平衡速度和准确性"),
        HIGH_ACCURACY("高准确性，较慢处理"),
        MEMORY_OPTIMIZED("内存优化");
        
        private final String description;
        
        PerformanceImpact(String description) {
            this.description = description;
        }
        
        @Override
        public String toString() {
            return description;
        }
    }
    
    /**
     * 计算最优块大小
     */
    public static ChunkSizeRecommendation calculateOptimalChunkSize(
            int documentLength, 
            String modelName, 
            String priority) {
        
        // 基础推荐值
        int baseSize = getModelBaseSize(modelName);
        
        // 根据优先级调整
        int adjustedSize = adjustByPriority(baseSize, priority, documentLength);
        
        // 计算预计块数
        int estimatedChunks = (documentLength + adjustedSize - 1) / adjustedSize;
        
        // 生成推理说明
        String reasoning = generateReasoning(documentLength, modelName, priority, adjustedSize);
        
        // 确定性能影响
        PerformanceImpact impact = determinePerformanceImpact(adjustedSize, estimatedChunks);
        
        return new ChunkSizeRecommendation(adjustedSize, estimatedChunks, reasoning, impact);
    }
    
    private static int getModelBaseSize(String modelName) {
        if (modelName == null) return 1000;
        
        String model = modelName.toLowerCase();
        if (model.contains("gpt-4")) return 2000;
        if (model.contains("gpt-3.5")) return 1500;
        if (model.contains("qwen")) return 1200;
        if (model.contains("claude")) return 1800;
        if (model.contains("ollama")) return 800;
        return 1000;
    }
    
    private static int adjustByPriority(int baseSize, String priority, int docLength) {
        if (priority == null) return baseSize;
        
        switch (priority.toLowerCase()) {
            case "speed":
            case "快速":
                return Math.min(baseSize * 2, 3000); // 增大块，减少API调用
                
            case "accuracy":
            case "准确性":
                return Math.max(baseSize / 2, 500); // 减小块，提高精度
                
            case "memory":
            case "内存":
                return Math.min(baseSize, 800); // 小块，节省内存
                
            case "balanced":
            case "平衡":
            default:
                // 根据文档大小动态调整
                if (docLength < 5000) return docLength; // 小文档不分块
                if (docLength > 100000) return baseSize * 3 / 2; // 大文档稍微增大
                return baseSize;
        }
    }
    
    private static String generateReasoning(int docLength, String modelName, 
                                          String priority, int finalSize) {
        StringBuilder reasoning = new StringBuilder();
        
        reasoning.append(String.format("文档长度%d字符", docLength));
        
        if (modelName != null) {
            reasoning.append(String.format("，使用%s模型", modelName));
        }
        
        if (priority != null) {
            reasoning.append(String.format("，优先%s", priority));
        }
        
        if (docLength <= finalSize) {
            reasoning.append("，建议不分块处理");
        } else {
            reasoning.append(String.format("，建议分块大小%d字符", finalSize));
        }
        
        return reasoning.toString();
    }
    
    private static PerformanceImpact determinePerformanceImpact(int chunkSize, int numChunks) {
        if (chunkSize >= 2500) {
            return PerformanceImpact.HIGH_SPEED;
        } else if (chunkSize <= 600) {
            return PerformanceImpact.HIGH_ACCURACY;
        } else if (chunkSize <= 800) {
            return PerformanceImpact.MEMORY_OPTIMIZED;
        } else {
            return PerformanceImpact.BALANCED;
        }
    }
    
    /**
     * 打印详细的分块分析报告
     */
    public static void printChunkAnalysis(int documentLength, String modelName) {
        log.info("=".repeat(60));
        log.info("📊 分块大小分析报告");
        log.info("=".repeat(60));
        log.info("📄 文档长度: {} 字符", documentLength);
        log.info("🤖 模型: {}", modelName != null ? modelName : "默认");
        log.info("");
        
        String[] priorities = {"speed", "balanced", "accuracy", "memory"};
        String[] priorityNames = {"速度优先", "平衡模式", "准确性优先", "内存优化"};
        
        for (int i = 0; i < priorities.length; i++) {
            ChunkSizeRecommendation rec = calculateOptimalChunkSize(
                documentLength, modelName, priorities[i]);
            
            log.info("🎯 {}: {}", priorityNames[i], rec);
            
            // 计算API调用次数和预估耗时
            double apiCalls = rec.getEstimatedChunks();
            double estimatedTime = apiCalls * 2.5; // 假设每次API调用2.5秒
            
            log.info("   📞 API调用次数: {} 次", (int)apiCalls);
            log.info("   ⏱️  预估处理时间: {:.1f} 秒", estimatedTime);
            log.info("");
        }
        
        log.info("=".repeat(60));
    }
    
    /**
     * 为您的100页合同提供专门建议
     */
    public static ChunkSizeRecommendation getContractRecommendation(int pageCount) {
        // 假设每页约5000字符
        int documentLength = pageCount * 5000;
        
        // 合同文档推荐使用准确性优先的平衡模式
        int recommendedSize = 1800; // 适合合同的复杂语义
        
        if (documentLength < 50000) {
            recommendedSize = 2200; // 较小合同可以用更大块
        } else if (documentLength > 200000) {
            recommendedSize = 1500; // 超大合同用较小块保证精度
        }
        
        int estimatedChunks = (documentLength + recommendedSize - 1) / recommendedSize;
        
        String reasoning = String.format(
            "%d页合同文档，考虑合同语义复杂性和准确性要求，推荐使用%d字符块大小", 
            pageCount, recommendedSize);
        
        return new ChunkSizeRecommendation(
            recommendedSize, 
            estimatedChunks, 
            reasoning, 
            PerformanceImpact.BALANCED
        );
    }
}
