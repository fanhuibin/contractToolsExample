package com.zhaoxinms.contract.tools.extract.core.chunking;

import com.zhaoxinms.contract.tools.extract.core.data.CharInterval;
import com.zhaoxinms.contract.tools.extract.core.data.Document;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文本块迭代器
 * 将大文档智能分割成适合LLM处理的文本块
 * 对应Python版本的ChunkIterator类
 */
@Slf4j
public class ChunkIterator implements Iterator<TextChunk> {
    
    private final Document document;
    private final int maxCharBuffer;
    private final List<SentenceBoundary> sentences;
    private int currentSentenceIndex;
    private int currentChunkIndex;
    
    // 句子边界检测的正则表达式
    private static final Pattern SENTENCE_PATTERN = Pattern.compile(
        // 中文句号、问号、感叹号
        "[。！？]+" +
        "|" +
        // 英文句号、问号、感叹号（后面跟空格或行尾）
        "[.!?]+(?=\\s|$)" +
        "|" +
        // 换行符作为句子边界
        "\\n+"
    );
    
    /**
     * 句子边界信息
     */
    private static class SentenceBoundary {
        final int startPos;
        final int endPos;
        final boolean isNewlineBreak;
        
        SentenceBoundary(int startPos, int endPos, boolean isNewlineBreak) {
            this.startPos = startPos;
            this.endPos = endPos;
            this.isNewlineBreak = isNewlineBreak;
        }
        
        @Override
        public String toString() {
            return String.format("Sentence[%d-%d, newline=%s]", startPos, endPos, isNewlineBreak);
        }
    }
    
    public ChunkIterator(Document document, int maxCharBuffer) {
        this.document = document;
        this.maxCharBuffer = maxCharBuffer;
        this.sentences = findSentenceBoundaries(document.getContent());
        this.currentSentenceIndex = 0;
        this.currentChunkIndex = 0;
        
        log.debug("初始化ChunkIterator: 文档长度={}, maxCharBuffer={}, 句子数={}",
            document.getContent().length(), maxCharBuffer, sentences.size());
    }
    
    /**
     * 查找文本中的句子边界
     */
    private List<SentenceBoundary> findSentenceBoundaries(String text) {
        List<SentenceBoundary> boundaries = new ArrayList<>();
        
        if (text == null || text.isEmpty()) {
            return boundaries;
        }
        
        Matcher matcher = SENTENCE_PATTERN.matcher(text);
        int lastEnd = 0;
        
        while (matcher.find()) {
            int sentenceStart = lastEnd;
            int sentenceEnd = matcher.end();
            
            // 检查是否是换行符分割
            boolean isNewlineBreak = matcher.group().contains("\n");
            
            if (sentenceEnd > sentenceStart) {
                boundaries.add(new SentenceBoundary(sentenceStart, sentenceEnd, isNewlineBreak));
                lastEnd = sentenceEnd;
            }
        }
        
        // 处理最后一个句子（如果没有以句号等结尾）
        if (lastEnd < text.length()) {
            boundaries.add(new SentenceBoundary(lastEnd, text.length(), false));
        }
        
        // 如果没有找到任何句子边界，将整个文本作为一个句子
        if (boundaries.isEmpty()) {
            boundaries.add(new SentenceBoundary(0, text.length(), false));
        }
        
        log.debug("找到 {} 个句子边界", boundaries.size());
        return boundaries;
    }
    
    @Override
    public boolean hasNext() {
        return currentSentenceIndex < sentences.size();
    }
    
    @Override
    public TextChunk next() {
        if (!hasNext()) {
            throw new IllegalStateException("没有更多的文本块");
        }
        
        // 从当前句子开始构建块
        SentenceBoundary startSentence = sentences.get(currentSentenceIndex);
        int chunkStart = startSentence.startPos;
        int chunkEnd = startSentence.endPos;
        
        // 尝试添加更多句子直到达到最大缓冲区大小
        int nextSentenceIndex = currentSentenceIndex + 1;
        
        while (nextSentenceIndex < sentences.size()) {
            SentenceBoundary nextSentence = sentences.get(nextSentenceIndex);
            int potentialEnd = nextSentence.endPos;
            
            // 检查添加下一个句子是否会超出缓冲区限制
            if (potentialEnd - chunkStart > maxCharBuffer) {
                // 如果当前块只有一个句子且仍然超出限制，需要分割句子
                if (nextSentenceIndex == currentSentenceIndex + 1 && 
                    chunkEnd - chunkStart > maxCharBuffer) {
                    chunkEnd = splitLongSentence(chunkStart, chunkEnd);
                }
                break;
            }
            
            chunkEnd = potentialEnd;
            nextSentenceIndex++;
        }
        
        // 创建文本块
        TextChunk chunk = TextChunk.builder()
            .charInterval(CharInterval.builder()
                .startPos(chunkStart)
                .endPos(chunkEnd)
                .build())
            .sourceDocument(document)
            .chunkIndex(currentChunkIndex)
            .chunkId(String.format("%s-chunk-%d", document.getId(), currentChunkIndex))
            .build();
        
        // 更新状态
        currentSentenceIndex = nextSentenceIndex;
        currentChunkIndex++;
        
        log.debug("生成文本块: {}", chunk);
        return chunk;
    }
    
    /**
     * 分割过长的句子
     */
    private int splitLongSentence(int sentenceStart, int sentenceEnd) {
        String text = document.getContent();
        int maxLength = maxCharBuffer;
        
        // 优先在换行符处分割
        for (int i = sentenceStart + maxLength - 1; i >= sentenceStart + maxLength / 2; i--) {
            if (i < text.length() && text.charAt(i) == '\n') {
                return i + 1;
            }
        }
        
        // 其次在空格处分割
        for (int i = sentenceStart + maxLength - 1; i >= sentenceStart + maxLength / 2; i--) {
            if (i < text.length() && Character.isWhitespace(text.charAt(i))) {
                return i + 1;
            }
        }
        
        // 最后在标点符号处分割
        for (int i = sentenceStart + maxLength - 1; i >= sentenceStart + maxLength / 2; i--) {
            if (i < text.length()) {
                char c = text.charAt(i);
                if (c == '，' || c == '；' || c == '：' || c == '、' || 
                    c == ',' || c == ';' || c == ':') {
                    return i + 1;
                }
            }
        }
        
        // 如果找不到合适的分割点，直接按最大长度分割
        int splitPoint = Math.min(sentenceStart + maxLength, sentenceEnd);
        log.warn("无法找到合适的句子分割点，强制分割在位置: {}", splitPoint);
        return splitPoint;
    }
    
    /**
     * 获取预估的块数量
     */
    public int getEstimatedChunkCount() {
        if (document.getContent() == null) {
            return 0;
        }
        int textLength = document.getContent().length();
        return (textLength + maxCharBuffer - 1) / maxCharBuffer; // 向上取整
    }
    
    /**
     * 创建所有文本块的列表（一次性处理）
     */
    public List<TextChunk> getAllChunks() {
        List<TextChunk> chunks = new ArrayList<>();
        while (hasNext()) {
            chunks.add(next());
        }
        return chunks;
    }
}
