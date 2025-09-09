package com.zhaoxinms.contract.tools.ocr;

import com.fasterxml.jackson.databind.JsonNode;
import com.zhaoxinms.contract.tools.ocr.model.CharBox;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * PDF文本提取工具类
 * 提供从OCR识别结果中提取文本和坐标信息的多种方法
 *
 * @author 系统生成
 * @version 1.0
 */
public class TextExtractionUtil {

    /**
     * 文本抽取策略枚举
     */
    public enum ExtractionStrategy {
        /**
         * 按顺序抽取：按照页面和布局项的自然顺序读取
         */
        SEQUENTIAL,

        /**
         * 按位置抽取：根据bbox坐标位置进行排序后读取
         */
        POSITION_BASED
    }

    /**
     * 从PDF识别结果中提取纯文本内容（按顺序读取，不包含位置信息）
     * @param ordered PDF页面布局数组
     * @return 提取的纯文本字符串
     */
    public static String extractTextFromResults(PageLayout[] ordered) {
        return extractTextFromResults(ordered, ExtractionStrategy.SEQUENTIAL);
    }

    /**
     * 从PDF识别结果中提取纯文本内容
     * @param ordered PDF页面布局数组
     * @param strategy 抽取策略
     * @return 提取的纯文本字符串
     */
    public static String extractTextFromResults(PageLayout[] ordered, ExtractionStrategy strategy) {
        StringBuilder text = new StringBuilder();

        List<PageLayout> processedLayouts = prepareLayouts(ordered, strategy);

        for (PageLayout pl : processedLayouts) {
            if (pl == null) continue;

            for (LayoutItem it : pl.items) {
                if (it.text != null && !it.text.isEmpty()) {
                    text.append(it.text);
                }
            }
        }

        return text.toString();
    }

    /**
     * 从PDF识别结果中提取带页码的文本内容
     * @param ordered PDF页面布局数组
     * @return 带页码标记的文本字符串
     */
    public static String extractTextWithPageMarkers(PageLayout[] ordered) {
        return extractTextWithPageMarkers(ordered, ExtractionStrategy.SEQUENTIAL);
    }

    /**
     * 从PDF识别结果中提取带页码的文本内容
     * @param ordered PDF页面布局数组
     * @param strategy 抽取策略
     * @return 带页码标记的文本字符串
     */
    public static String extractTextWithPageMarkers(PageLayout[] ordered, ExtractionStrategy strategy) {
        StringBuilder text = new StringBuilder();

        List<PageLayout> processedLayouts = prepareLayouts(ordered, strategy);

        for (PageLayout pl : processedLayouts) {
            if (pl == null) continue;

            text.append("=== PAGE ").append(pl.page).append(" ===\n");

            for (LayoutItem it : pl.items) {
                if (it.text != null && !it.text.isEmpty()) {
                    text.append(it.text);
                }
            }
        }

        return text.toString();
    }

    /**
     * 从PDF识别结果中解析文本和位置信息（默认按顺序读取）
     * @param ordered PDF页面布局数组
     * @return 字符框列表，包含文本和位置信息
     */
    public static List<CharBox> parseTextAndPositionsFromResults(PageLayout[] ordered) {
        return parseTextAndPositionsFromResults(ordered, ExtractionStrategy.SEQUENTIAL);
    }

    /**
     * 从PDF识别结果中解析文本和位置信息
     * @param ordered PDF页面布局数组
     * @param strategy 抽取策略
     * @return 字符框列表，包含文本和位置信息
     */
    public static List<CharBox> parseTextAndPositionsFromResults(PageLayout[] ordered, ExtractionStrategy strategy) {
        List<CharBox> out = new ArrayList<>();

        List<PageLayout> processedLayouts = prepareLayouts(ordered, strategy);

        for (PageLayout pl : processedLayouts) {
            if (pl == null) continue;

            // 按顺序遍历每一页中的布局项
            for (LayoutItem it : pl.items) {
                if (it.text == null || it.text.isEmpty()) continue;

                String s = it.text;
                // 按顺序为每个字符创建CharBox，使用布局项的bbox
                for (int i = 0; i < s.length(); i++) {
                    char ch = s.charAt(i);
                    out.add(new CharBox(pl.page, ch, it.bbox, it.category));
                }
            }
        }

        return out;
    }

    /**
     * 准备布局数据，根据策略进行预处理
     * @param ordered 原始布局数组
     * @param strategy 抽取策略
     * @return 处理后的布局列表
     */
    private static List<PageLayout> prepareLayouts(PageLayout[] ordered, ExtractionStrategy strategy) {
        List<PageLayout> layouts = new ArrayList<>();

        for (PageLayout pl : ordered) {
            if (pl == null) continue;

            if (strategy == ExtractionStrategy.POSITION_BASED) {
                // 按位置排序的处理
                PageLayout sortedLayout = sortLayoutItemsByPosition(pl);
                layouts.add(sortedLayout);
            } else {
                // 按顺序保持原有顺序
                layouts.add(pl);
            }
        }

        return layouts;
    }

    /**
     * 按位置对布局项进行排序
     * @param layout 原始页面布局
     * @return 排序后的页面布局
     */
    private static PageLayout sortLayoutItemsByPosition(PageLayout layout) {
        List<LayoutItem> sortedItems = new ArrayList<>(layout.items);

        // 按bbox位置排序：先按y坐标（从上到下），再按x坐标（从左到右）
        sortedItems.sort((a, b) -> {
            if (a.bbox == null && b.bbox == null) return 0;
            if (a.bbox == null) return 1;
            if (b.bbox == null) return -1;

            // 比较y坐标（从上到下）
            double y1 = a.bbox[1];
            double y2 = b.bbox[1];
            int yCompare = Double.compare(y1, y2);
            if (yCompare != 0) return yCompare;

            // y坐标相同时，比较x坐标（从左到右）
            double x1 = a.bbox[0];
            double x2 = b.bbox[0];
            return Double.compare(x1, x2);
        });

        return new PageLayout(layout.page, sortedItems);
    }

    /**
     * 将一个布局元素的text展开成字符（按bbox位置解析）
     * @param page 页码
     * @param it 布局项
     * @return 字符框列表
     */
    public static List<CharBox> expandToCharsByPosition(int page, LayoutItem it) {
        List<CharBox> out = new ArrayList<>();
        if (it.text == null || it.text.isEmpty()) return out;

        String s = it.text;
        // 保持每个字符所属的 bbox 等于布局项的整体 bbox，便于基于 bbox 的换行
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            out.add(new CharBox(page, ch, it.bbox, it.category));
        }
        return out;
    }

    // ---------- JSON解析方法 ----------

    /**
     * 从OCR返回的JSON结果中提取布局项
     * @param root OCR返回的JSON根节点
     * @return 布局项列表
     */
    public static List<LayoutItem> extractLayoutItems(JsonNode root) {
        List<LayoutItem> out = new ArrayList<>();
        Deque<JsonNode> queue = new ArrayDeque<>();
        queue.addLast(root);

        while (!queue.isEmpty()) {
            JsonNode n = queue.pollFirst();
            if (n.isObject()) {
                JsonNode bbox = n.get("bbox");
                JsonNode cat = n.get("category");
                JsonNode text = n.get("text");
                if (bbox != null && bbox.isArray() && bbox.size() == 4 && cat != null && cat.isTextual()) {
                    double x1 = bbox.get(0).asDouble();
                    double y1 = bbox.get(1).asDouble();
                    double x2 = bbox.get(2).asDouble();
                    double y2 = bbox.get(3).asDouble();
                    String category = cat.asText();
                    String tx = (text != null && !text.isNull()) ? text.asText("") : "";
                    out.add(new LayoutItem(new double[]{x1, y1, x2, y2}, category, tx));
                }
                n.fields().forEachRemaining(e -> queue.addLast(e.getValue()));
            } else if (n.isArray()) {
                for (JsonNode c : n) queue.addLast(c);
            }
        }

        // 保持原始顺序，不进行排序
        return out;
    }

    // ---------- CharBox工具方法 ----------

    /**
     * 从CharBox列表中提取纯文本
     * @param charBoxes CharBox列表
     * @return 纯文本字符串
     */
    public static String extractText(List<CharBox> charBoxes) {
        if (charBoxes == null || charBoxes.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (CharBox cb : charBoxes) {
            sb.append(cb.ch);
        }
        return sb.toString();
    }

    /**
     * 从CharBox列表中提取带页标记的文本
     * @param charBoxes CharBox列表
     * @return 带页标记的文本
     */
    public static String extractTextWithPageMarkers(List<CharBox> charBoxes) {
        if (charBoxes == null || charBoxes.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int currentPage = -1;
        for (CharBox cb : charBoxes) {
            if (cb.page != currentPage) {
                if (currentPage != -1) {
                    sb.append("\n");
                }
                sb.append("[Page ").append(cb.page).append("]\n");
                currentPage = cb.page;
            }
            sb.append(cb.ch);
        }
        return sb.toString();
    }

    /**
     * 过滤指定页面的CharBox
     * @param charBoxes CharBox列表
     * @param page 页面号
     * @return 指定页面的CharBox列表
     */
    public static List<CharBox> filterByPage(List<CharBox> charBoxes, int page) {
        if (charBoxes == null || charBoxes.isEmpty()) {
            return new ArrayList<>();
        }
        return charBoxes.stream()
                .filter(cb -> cb.page == page)
                .collect(Collectors.toList());
    }

    /**
     * 获取CharBox列表中的所有页面号
     * @param charBoxes CharBox列表
     * @return 页面号集合
     */
    public static Set<Integer> getAllPages(List<CharBox> charBoxes) {
        if (charBoxes == null || charBoxes.isEmpty()) {
            return new HashSet<>();
        }
        return charBoxes.stream()
                .map(cb -> cb.page)
                .collect(Collectors.toSet());
    }

    // ---------- 数据模型类 ----------

    /**
     * 页面布局类
     */
    public static class PageLayout {
        public final int page;
        public final List<LayoutItem> items;

        public PageLayout(int page, List<LayoutItem> items) {
            this.page = page;
            this.items = items;
        }
    }

    /**
     * 布局项类
     */
    public static class LayoutItem {
        public final double[] bbox; // [x1,y1,x2,y2]
        public final String category;
        public final String text; // may be empty for Picture

        public LayoutItem(double[] bbox, String category, String text) {
            this.bbox = bbox;
            this.category = category;
            this.text = text;
        }
    }


    /**
     * 使用示例：演示如何使用 TextExtractionUtil
     */
    public static void usageExample() {
        System.out.println("=== TextExtractionUtil 使用示例 ===\n");

        // 创建示例数据
        List<LayoutItem> items1 = new ArrayList<>();
        items1.add(new LayoutItem(new double[]{10, 20, 100, 30}, "text", "Hello World"));
        items1.add(new LayoutItem(new double[]{10, 40, 100, 50}, "text", "Second line"));

        List<LayoutItem> items2 = new ArrayList<>();
        items2.add(new LayoutItem(new double[]{15, 25, 105, 35}, "text", "Modified text"));

        PageLayout[] layouts = new PageLayout[]{
            new PageLayout(1, items1),
            new PageLayout(2, items2)
        };

        // 1. 提取纯文本（默认顺序读取）
        String plainText = extractTextFromResults(layouts);
        System.out.println("1. 纯文本提取:");
        System.out.println(plainText);
        System.out.println();

        // 2. 提取带页码的文本
        String textWithPages = extractTextWithPageMarkers(layouts);
        System.out.println("2. 带页码文本提取:");
        System.out.println(textWithPages);
        System.out.println();

        // 3. 解析文本和位置信息（顺序读取）
        List<CharBox> charBoxes = parseTextAndPositionsFromResults(layouts);
        System.out.println("3. 文本和位置解析（顺序读取）:");
        for (CharBox cb : charBoxes) {
            System.out.println(String.format("  页%d: '%c' at [%.1f,%.1f,%.1f,%.1f]",
                cb.page, cb.ch, cb.bbox[0], cb.bbox[1], cb.bbox[2], cb.bbox[3]));
        }
        System.out.println();

        // 4. 解析文本和位置信息（按位置排序）
        List<CharBox> charBoxesPositioned = parseTextAndPositionsFromResults(layouts, ExtractionStrategy.POSITION_BASED);
        System.out.println("4. 文本和位置解析（按位置排序）:");
        for (CharBox cb : charBoxesPositioned) {
            System.out.println(String.format("  页%d: '%c' at [%.1f,%.1f,%.1f,%.1f]",
                cb.page, cb.ch, cb.bbox[0], cb.bbox[1], cb.bbox[2], cb.bbox[3]));
        }
        System.out.println();

        System.out.println("=== 示例完成 ===");
    }
}
