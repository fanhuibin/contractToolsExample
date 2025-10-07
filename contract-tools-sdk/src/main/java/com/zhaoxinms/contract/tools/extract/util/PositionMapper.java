package com.zhaoxinms.contract.tools.extract.util;

import com.zhaoxinms.contract.tools.extract.model.CharBox;
import com.zhaoxinms.contract.tools.extract.core.data.CharInterval;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 位置映射工具类
 * 将文本中的字符位置映射到OCR识别的bbox坐标
 */
@Slf4j
public class PositionMapper {

    /**
     * 根据字符区间获取对应的bbox列表
     * 
     * @param intervals 字符区间列表
     * @param charBoxes OCR识别的字符框列表
     * @param content 完整文本内容
     * @return bbox映射结果
     */
    public static List<BboxMapping> mapIntervalsToBboxes(List<CharInterval> intervals, List<CharBox> charBoxes, String content) {
        if (intervals == null || intervals.isEmpty() || charBoxes == null || charBoxes.isEmpty()) {
            return Collections.emptyList();
        }

        log.debug("开始映射字符区间到bbox，区间数: {}, CharBox数: {}", intervals.size(), charBoxes.size());

        // 构建字符位置到CharBox的索引映射
        Map<Integer, CharBox> positionToCharBox = buildPositionIndex(charBoxes, content);
        
        List<BboxMapping> mappings = new ArrayList<>();
        
        for (CharInterval interval : intervals) {
            BboxMapping mapping = mapSingleInterval(interval, positionToCharBox, content);
            if (mapping != null) {
                mappings.add(mapping);
                log.debug("映射成功: {} -> {} 个bbox", interval, mapping.getBboxes().size());
            } else {
                log.warn("映射失败: {}", interval);
            }
        }
        
        log.info("位置映射完成，成功映射 {}/{} 个区间", mappings.size(), intervals.size());
        return mappings;
    }

    /**
     * 构建字符位置到CharBox的索引映射
     */
    private static Map<Integer, CharBox> buildPositionIndex(List<CharBox> charBoxes, String content) {
        Map<Integer, CharBox> positionIndex = new HashMap<>();
        
        if (content == null || content.isEmpty()) {
            log.warn("文本内容为空，无法构建位置索引");
            return positionIndex;
        }
        
        // 按页面和位置排序CharBox
        List<CharBox> sortedCharBoxes = new ArrayList<>(charBoxes);
        sortedCharBoxes.sort((a, b) -> {
            if (a.page != b.page) {
                return Integer.compare(a.page, b.page);
            }
            // 按Y坐标排序（从上到下），然后按X坐标排序（从左到右）
            if (Math.abs(a.bbox[1] - b.bbox[1]) > 5) { // Y坐标差异超过5像素
                return Double.compare(a.bbox[1], b.bbox[1]);
            }
            return Double.compare(a.bbox[0], b.bbox[0]);
        });
        
        // 重建文本并建立位置映射
        StringBuilder rebuiltText = new StringBuilder();
        for (int i = 0; i < sortedCharBoxes.size(); i++) {
            CharBox charBox = sortedCharBoxes.get(i);
            if (charBox.bbox != null) {
                rebuiltText.append(charBox.ch);
                positionIndex.put(rebuiltText.length() - 1, charBox);
            }
        }
        
        log.debug("构建位置索引完成，原文本长度: {}, 重建文本长度: {}, 索引项: {}", 
            content.length(), rebuiltText.length(), positionIndex.size());
        
        return positionIndex;
    }

    /**
     * 映射单个字符区间到bbox
     */
    private static BboxMapping mapSingleInterval(CharInterval interval, Map<Integer, CharBox> positionIndex, String content) {
        List<BboxInfo> bboxes = new ArrayList<>();
        Set<Integer> pages = new HashSet<>();
        
        int start = interval.getStartPos();
        int end = interval.getEndPos();
        
        // 确保区间在有效范围内
        if (start < 0 || end > content.length() || start >= end) {
            log.warn("无效的字符区间: start={}, end={}, contentLength={}", start, end, content.length());
            return null;
        }
        
        // 收集区间内所有字符的bbox
        for (int pos = start; pos < end; pos++) {
            CharBox charBox = positionIndex.get(pos);
            if (charBox != null && charBox.bbox != null) {
                bboxes.add(new BboxInfo(
                    charBox.page,
                    charBox.bbox.clone(),
                    charBox.category,
                    charBox.ch
                ));
                pages.add(charBox.page);
            }
        }
        
        if (bboxes.isEmpty()) {
            log.warn("区间 [{}, {}) 没有找到对应的bbox", start, end);
            return null;
        }
        
        // 合并相邻的bbox（可选优化）
        List<BboxInfo> mergedBboxes = mergeBboxes(bboxes);
        
        return new BboxMapping(
            interval,
            content.substring(start, end),
            mergedBboxes,
            new ArrayList<>(pages)
        );
    }

    /**
     * 合并相邻的bbox（简单实现，可以进一步优化）
     */
    private static List<BboxInfo> mergeBboxes(List<BboxInfo> bboxes) {
        if (bboxes.size() <= 1) {
            return bboxes;
        }
        
        // 按页面和位置排序
        bboxes.sort((a, b) -> {
            if (a.getPage() != b.getPage()) {
                return Integer.compare(a.getPage(), b.getPage());
            }
            if (Math.abs(a.getBbox()[1] - b.getBbox()[1]) > 5) {
                return Double.compare(a.getBbox()[1], b.getBbox()[1]);
            }
            return Double.compare(a.getBbox()[0], b.getBbox()[0]);
        });
        
        // 简单合并：相同行的相邻字符
        List<BboxInfo> merged = new ArrayList<>();
        BboxInfo current = bboxes.get(0);
        StringBuilder currentText = new StringBuilder().append(current.getCharacter());
        
        for (int i = 1; i < bboxes.size(); i++) {
            BboxInfo next = bboxes.get(i);
            
            // 如果在同一页面且Y坐标相近（同一行）
            if (current.getPage() == next.getPage() && 
                Math.abs(current.getBbox()[1] - next.getBbox()[1]) <= 5) {
                
                // 扩展当前bbox
                current = new BboxInfo(
                    current.getPage(),
                    new double[]{
                        Math.min(current.getBbox()[0], next.getBbox()[0]),
                        Math.min(current.getBbox()[1], next.getBbox()[1]),
                        Math.max(current.getBbox()[2], next.getBbox()[2]),
                        Math.max(current.getBbox()[3], next.getBbox()[3])
                    },
                    current.getCategory(),
                    currentText.append(next.getCharacter()).toString().charAt(currentText.length() - 1)
                );
            } else {
                // 保存当前合并的bbox，开始新的
                merged.add(current);
                current = next;
                currentText = new StringBuilder().append(current.getCharacter());
            }
        }
        
        merged.add(current); // 添加最后一个
        return merged;
    }

    /**
     * Bbox信息类
     */
    public static class BboxInfo {
        private final int page;
        private final double[] bbox;
        private final String category;
        private final char character;

        public BboxInfo(int page, double[] bbox, String category, char character) {
            this.page = page;
            this.bbox = bbox;
            this.category = category;
            this.character = character;
        }

        public int getPage() { return page; }
        public double[] getBbox() { return bbox; }
        public String getCategory() { return category; }
        public char getCharacter() { return character; }

        @Override
        public String toString() {
            return String.format("BboxInfo{page=%d, bbox=[%.1f,%.1f,%.1f,%.1f], category='%s', char='%c'}", 
                page, bbox[0], bbox[1], bbox[2], bbox[3], category, character);
        }
    }

    /**
     * Bbox映射结果类
     */
    public static class BboxMapping {
        private final CharInterval interval;
        private final String text;
        private final List<BboxInfo> bboxes;
        private final List<Integer> pages;

        public BboxMapping(CharInterval interval, String text, List<BboxInfo> bboxes, List<Integer> pages) {
            this.interval = interval;
            this.text = text;
            this.bboxes = bboxes;
            this.pages = pages;
        }

        public CharInterval getInterval() { return interval; }
        public String getText() { return text; }
        public List<BboxInfo> getBboxes() { return bboxes; }
        public List<Integer> getPages() { return pages; }

        @Override
        public String toString() {
            return String.format("BboxMapping{interval=%s, text='%s', bboxes=%d, pages=%s}", 
                interval, text, bboxes.size(), pages);
        }
    }
}
