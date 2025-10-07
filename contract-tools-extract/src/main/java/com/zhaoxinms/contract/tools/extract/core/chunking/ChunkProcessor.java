package com.zhaoxinms.contract.tools.extract.core.chunking;

import com.zhaoxinms.contract.tools.extract.core.data.CharInterval;
import com.zhaoxinms.contract.tools.extract.core.data.Document;
import com.zhaoxinms.contract.tools.extract.core.data.Extraction;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文本块处理器
 * 负责处理文本块的提取结果合并和字符位置映射
 */
@Slf4j
public class ChunkProcessor {
    
    /**
     * 合并多个文本块的提取结果
     * 将块级别的字符位置映射回原始文档位置
     */
    public static List<Extraction> mergeChunkExtractions(
            List<TextChunk> chunks, 
            List<List<Extraction>> chunkExtractions) {
        
        if (chunks.size() != chunkExtractions.size()) {
            throw new IllegalArgumentException("文本块数量与提取结果数量不匹配");
        }
        
        List<Extraction> mergedExtractions = new ArrayList<>();
        
        for (int i = 0; i < chunks.size(); i++) {
            TextChunk chunk = chunks.get(i);
            List<Extraction> extractions = chunkExtractions.get(i);
            
            if (extractions == null || extractions.isEmpty()) {
                continue;
            }
            
            // 将块内的字符位置映射到原始文档位置
            List<Extraction> mappedExtractions = mapExtractionsToDocument(chunk, extractions);
            mergedExtractions.addAll(mappedExtractions);
        }
        
        log.debug("合并了 {} 个文本块的提取结果，总计 {} 个提取项", 
            chunks.size(), mergedExtractions.size());
        
        return mergedExtractions;
    }
    
    /**
     * 将文本块内的提取结果映射到原始文档位置
     */
    private static List<Extraction> mapExtractionsToDocument(
            TextChunk chunk, List<Extraction> chunkExtractions) {
        
        CharInterval chunkInterval = chunk.getCharInterval();
        if (chunkInterval == null || !chunkInterval.isValid()) {
            log.warn("文本块 {} 的字符区间无效", chunk.getChunkId());
            return new ArrayList<>();
        }
        
        int chunkStartInDoc = chunkInterval.getStartPos();
        
        return chunkExtractions.stream()
            .map(extraction -> mapExtractionToDocument(extraction, chunkStartInDoc, chunk))
            .filter(extraction -> extraction != null && extraction.getCharInterval() != null)
            .collect(Collectors.toList());
    }
    
    /**
     * 将单个提取结果映射到原始文档位置
     */
    private static Extraction mapExtractionToDocument(
            Extraction chunkExtraction, int chunkStartInDoc, TextChunk chunk) {
        
        CharInterval chunkCharInterval = chunkExtraction.getCharInterval();
        if (chunkCharInterval == null || !chunkCharInterval.isValid()) {
            log.debug("提取结果没有有效的字符区间: {}", chunkExtraction.getValue());
            return chunkExtraction; // 返回原提取结果，但位置信息可能不准确
        }
        
        // 映射字符位置到原始文档
        int docStartPos = chunkStartInDoc + chunkCharInterval.getStartPos();
        int docEndPos = chunkStartInDoc + chunkCharInterval.getEndPos();
        
        // 验证映射后的位置是否在文档范围内
        Document sourceDoc = chunk.getSourceDocument();
        if (sourceDoc != null && sourceDoc.getContent() != null) {
            int docLength = sourceDoc.getContent().length();
            if (docEndPos > docLength) {
                log.warn("映射后的字符位置超出文档范围: docEndPos={}, docLength={}, chunk={}", 
                    docEndPos, docLength, chunk.getChunkId());
                docEndPos = docLength;
            }
            if (docStartPos >= docEndPos) {
                log.warn("映射后的字符位置无效: startPos={}, endPos={}", docStartPos, docEndPos);
                return null;
            }
        }
        
        // 创建映射后的字符区间
        CharInterval documentCharInterval = CharInterval.builder()
            .startPos(docStartPos)
            .endPos(docEndPos)
            .build();
        
        // 验证映射后的文本是否匹配
        if (sourceDoc != null && sourceDoc.getContent() != null) {
            String expectedText = String.valueOf(chunkExtraction.getValue());
            String actualText = sourceDoc.getContent().substring(docStartPos, docEndPos);
            
            if (!expectedText.equals(actualText)) {
                log.debug("映射后的文本不匹配 - 预期: '{}', 实际: '{}', 位置: [{}-{}]", 
                    expectedText, actualText, docStartPos, docEndPos);
                
                // 尝试重新对齐（简单的前后搜索）
                CharInterval realignedInterval = realignText(sourceDoc.getContent(), expectedText, 
                    docStartPos, docEndPos);
                if (realignedInterval != null) {
                    documentCharInterval = realignedInterval;
                    log.debug("重新对齐成功: {}", realignedInterval);
                }
            }
        }
        
        // 创建新的提取结果，使用映射后的位置
        return Extraction.builder()
            .field(chunkExtraction.getField())
            .value(chunkExtraction.getValue())
            .charInterval(documentCharInterval)
            .confidence(chunkExtraction.getConfidence())
            .metadata(chunkExtraction.getMetadata())
            .documentId(chunk.getDocumentId())
            .createdAt(chunkExtraction.getCreatedAt())
            .method(chunkExtraction.getMethod())
            .build();
    }
    
    /**
     * 简单的文本重新对齐
     */
    private static CharInterval realignText(String documentText, String targetText, 
            int approximateStart, int approximateEnd) {
        
        if (targetText == null || targetText.trim().isEmpty()) {
            return null;
        }
        
        // 在近似位置周围搜索
        int searchRadius = Math.min(50, targetText.length());
        int searchStart = Math.max(0, approximateStart - searchRadius);
        int searchEnd = Math.min(documentText.length(), approximateEnd + searchRadius);
        
        String searchArea = documentText.substring(searchStart, searchEnd);
        int foundIndex = searchArea.indexOf(targetText);
        
        if (foundIndex >= 0) {
            int realStart = searchStart + foundIndex;
            int realEnd = realStart + targetText.length();
            return CharInterval.builder()
                .startPos(realStart)
                .endPos(realEnd)
                .build();
        }
        
        return null;
    }
    
    /**
     * 创建文档的所有文本块
     */
    public static List<TextChunk> createChunks(Document document, int maxCharBuffer) {
        ChunkIterator iterator = new ChunkIterator(document, maxCharBuffer);
        return iterator.getAllChunks();
    }
    
    /**
     * 获取推荐的块大小
     */
    public static int getRecommendedChunkSize(String modelName) {
        // 根据不同模型返回推荐的块大小
        if (modelName == null) {
            return 1000; // 默认值
        }
        
        String model = modelName.toLowerCase();
        if (model.contains("gpt-4")) {
            return 2000; // GPT-4有更大的上下文窗口
        } else if (model.contains("gpt-3.5")) {
            return 1500;
        } else if (model.contains("qwen")) {
            return 1200; // 千问模型
        } else if (model.contains("claude")) {
            return 1800;
        } else {
            return 1000; // 保守默认值
        }
    }
    
    /**
     * 检查文档是否需要分块
     */
    public static boolean needsChunking(Document document, int maxCharBuffer) {
        if (document == null || document.getContent() == null) {
            return false;
        }
        return document.getContent().length() > maxCharBuffer;
    }
}
