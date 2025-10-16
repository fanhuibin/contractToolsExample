package com.zhaoxinms.contract.tools.comparePRO.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.zhaoxinms.contract.tools.comparePRO.model.CharBox;
import com.zhaoxinms.contract.tools.comparePRO.util.LaTeXToUnicodeConverter;
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
        return parseTextAndPositionsFromResults(ordered, ExtractionStrategy.SEQUENTIAL, false);
    }

    /**
     * 从PDF识别结果中解析文本和位置信息
     * @param ordered PDF页面布局数组
     * @param strategy 抽取策略
     * @return 字符框列表，包含文本和位置信息
     */
    public static List<CharBox> parseTextAndPositionsFromResults(PageLayout[] ordered, ExtractionStrategy strategy) {
        return parseTextAndPositionsFromResults(ordered, strategy, false);
    }

    /**
     * 从PDF识别结果中解析文本和位置信息
     * @param ordered PDF页面布局数组
     * @param strategy 抽取策略
     * @param ignoreHeaderFooter 是否忽略页眉页脚
     * @return 字符框列表，包含文本和位置信息
     */
    public static List<CharBox> parseTextAndPositionsFromResults(PageLayout[] ordered, ExtractionStrategy strategy, boolean ignoreHeaderFooter) {
        return parseTextAndPositionsFromResults(ordered, strategy, ignoreHeaderFooter, 5.0, 5.0, null);
    }

    /**
     * 从PDF识别结果中解析文本和位置信息（支持基于位置的页眉页脚检测）
     * @param ordered PDF页面布局数组
     * @param strategy 抽取策略
     * @param ignoreHeaderFooter 是否忽略页眉页脚
     * @param headerHeightPercent 页眉高度百分比（页面顶部）
     * @param footerHeightPercent 页脚高度百分比（页面底部）
     * @param pageHeights 每页的高度信息（用于百分比计算），如果为null则回退到category检测
     * @return 字符框列表，包含文本和位置信息
     */
    public static List<CharBox> parseTextAndPositionsFromResults(
            PageLayout[] ordered, 
            ExtractionStrategy strategy, 
            boolean ignoreHeaderFooter, 
            double headerHeightPercent, 
            double footerHeightPercent, 
            double[] pageHeights) {
        List<CharBox> out = new ArrayList<>();

        List<PageLayout> processedLayouts = prepareLayouts(ordered, strategy);

        for (PageLayout pl : processedLayouts) {
            if (pl == null) continue;

            // 获取当前页面的高度信息（优先使用图片高度，回退到PDF高度）
            double currentPageHeight = 0;
            if (pl.imageHeight > 0) {
                // 使用OCR结果中的实际图片高度
                currentPageHeight = pl.imageHeight;
//                System.out.println("使用图片高度进行页眉页脚检测 - 页面" + pl.page + 
//                    ", 图片尺寸: " + pl.imageWidth + "x" + pl.imageHeight + "像素");
            } else if (pageHeights != null && pl.page >= 1 && pl.page <= pageHeights.length) {
                // 回退到pageHeights数组中的高度（可能是PDF高度或图片高度）
                currentPageHeight = pageHeights[pl.page - 1]; // 页面索引从1开始，数组从0开始
                if (currentPageHeight > 1500) {
                    // 如果高度大于1500，很可能是图片像素高度
//                    System.out.println("使用传入的图片高度进行页眉页脚检测 - 页面" + pl.page + 
//                        ", 图片高度: " + currentPageHeight + "像素");
                } else if (currentPageHeight > 0) {
                    // 如果高度在0-1500之间，很可能是PDF点高度
//                    System.out.println("回退使用PDF高度进行页眉页脚检测 - 页面" + pl.page + 
//                        ", PDF高度: " + currentPageHeight + "点");
                }
            } else {
//                System.out.println("无法获取页面高度信息 - 页面" + pl.page + 
//                    ", 跳过位置检测，使用category检测");
            }

            // 按顺序遍历每一页中的布局项
            for (LayoutItem it : pl.items) {
                if (it.text == null || it.text.isEmpty()) continue;
                

                // 如果启用忽略页眉页脚功能，进行检测
                if (ignoreHeaderFooter) {
                    boolean isHeaderOrFooter = false;
                    
                    // 双重条件检查：必须同时满足类型检查和位置检查
                    boolean isCategoryMatch = it.category != null && 
                        ("Page-header".equals(it.category) || "Page-footer".equals(it.category));
                    
                    if (isCategoryMatch && currentPageHeight > 0 && it.bbox != null && it.bbox.length >= 4) {
                        // 基于位置的页眉页脚检测（新算法）：要求bbox完全包含在页眉或页脚区域内
                        double bboxMinY = it.bbox[1]; // bbox的顶部Y坐标
                        double bboxMaxY = it.bbox[3]; // bbox的底部Y坐标
                        
                        // 计算位置百分比
                        double topPercent = (bboxMinY / currentPageHeight) * 100;
                        double bottomPercent = (bboxMaxY / currentPageHeight) * 100;
                        
                        // 页眉检测：bbox完全包含在页眉区域内（顶部和底部都在页眉区域）
                        if ("Page-header".equals(it.category) && 
                            topPercent >= 0 && bottomPercent <= headerHeightPercent) {
                            isHeaderOrFooter = true;
                        }
                        // 页脚检测：bbox完全包含在页脚区域内（顶部和底部都在页脚区域）
                        else if ("Page-footer".equals(it.category) && 
                                 topPercent >= (100 - footerHeightPercent) && bottomPercent <= 100) {
                            isHeaderOrFooter = true;
                        }
                    }
                    
                    if (isHeaderOrFooter) {
                        continue; // 跳过页眉页脚内容
                    }
                }

                String s = it.text;
                
                // 应用文本处理规则（按顺序编号）
                s = applyTextProcessingRules(s, it.category);
                
                // 如果处理规则返回null，表示应跳过该文本
                if (s == null) {
                    continue;
                }
                
                // 按顺序为每个字符创建CharBox，使用布局项的bbox
                for (int i = 0; i < s.length(); i++) {
                    char ch = s.charAt(i);
                    out.add(new CharBox(pl.page, ch, it.bbox, it.category));
                }
            }
        }

        // 输出页眉页脚检测统计结果
//        System.out.println("=== 页眉页脚检测统计 ===");
//        System.out.println("总布局项目数: " + totalItems);
//        System.out.println("检测到页眉项目: " + headerItems);
//        System.out.println("检测到页脚项目: " + footerItems);
//        System.out.println("被忽略项目总数: " + ignoredItems);
//        System.out.println("保留项目数: " + (totalItems - ignoredItems));
//        if (totalItems > 0) {
//            System.out.println("页眉检测率: " + String.format("%.2f", (double)headerItems / totalItems * 100) + "%");
//            System.out.println("页脚检测率: " + String.format("%.2f", (double)footerItems / totalItems * 100) + "%");
//            System.out.println("忽略率: " + String.format("%.2f", (double)ignoredItems / totalItems * 100) + "%");
//        }
//        System.out.println("========================");

        return out;
    }

    /**
     * 应用文本处理规则（按顺序编号）
     * @param text 原始文本
     * @param category 文本类别
     * @return 处理后的文本，如果返回null表示应跳过该文本
     */
    private static String applyTextProcessingRules(String text, String category) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        String s = text;
        
        // 规则1: 如果category是Table类型，去掉HTML标签，只保留文本内容
        if ("Table".equals(category)) {
            s = removeHtmlTags(s);
        }
        
        // 规则2: LaTeX数学公式转Unicode（在HTML标签移除后进行）
        if (LaTeXToUnicodeConverter.containsLatexCommands(s)) {
            s = LaTeXToUnicodeConverter.convertToUnicode(s);
        }
        
        // 规则3: 移除文本开头连续出现的#号及其前置空白
        s = s.replaceFirst("^\\s*#*\\s*", "");
        
        // 规则4: 去掉文本头部的列表标记（- 和 *）
        // 处理多行情况，每行开头可能有 - 或 * 号
        s = s.replaceAll("(?m)^\\s*[-*]\\s*", "");
        
        // 规则5: 移除所有换行符（\r 和 \n）
        s = s.replace("\r", "").replace("\n", "");
        
        // 规则6: 去掉markdown格式标记
        // 6.1 去掉 **文本** 格式的加粗标记
        s = s.replaceAll("\\*\\*([^*]+?)\\*\\*", "$1");
        // 6.2 去掉 __*文本*__ 格式的下划线包围斜体标记
        s = s.replaceAll("__\\*([^*]+?)\\*__", "$1");
        // 6.3 去掉 **_文本_** 格式的星号包围加粗标记
        s = s.replaceAll("\\*\\*_([^_]+?)_\\*\\*", "$1");
        // 6.4 去掉单独的 *文本* 格式的斜体标记
        s = s.replaceAll("\\*([^*]+?)\\*", "$1");
        // 6.5 去掉单独的 _文本_ 格式的斜体标记
        s = s.replaceAll("_([^_]+?)_", "$1");
        
        // 规则7: 将任意长度的连续下划线统一为三个下划线
        // 说明：把一段中出现的 1 个或多个连续下划线都规范为 "___"
        s = s.replaceAll("_+", "___");

        // 规则8: 将括号内的小于100的正整数视为编号，去掉括号，仅保留数字
        // 支持中文括号（（ ））与英文括号 ( )，匹配(1)~(99)与（1）~（99）
        s = s.replaceAll("[\\(（]([1-9][0-9]?)[\\)）]", "$1");
        
        // 规则9: 忽略以 "Thisimagedoesnotcontainanytext." 开头的文本
        // 去掉空格后检查是否以此字符串开头，如果是则返回null表示跳过
        if (s.replaceAll("\\s+", "").startsWith("Thisimagedoesnotcontainanytext.")) {
            return null; // 返回null表示应跳过该文本
        }
        
        return s;
    }
    
    /**
     * 移除HTML标签，只保留文本内容，多个空格替换为单个空格
     * @param htmlText 包含HTML标签的文本
     * @return 清理后的纯文本
     */
    private static String removeHtmlTags(String htmlText) {
        if (htmlText == null || htmlText.isEmpty()) {
            return htmlText;
        }
        
        // 移除HTML标签
        String textOnly = htmlText.replaceAll("<[^>]+>", " ");
        
        // 将多个连续空格替换为单个空格
        textOnly = textOnly.replaceAll("\\s+", " ");
        
        // 去除首尾空格
        textOnly = textOnly.trim();
        
        return textOnly;
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
        public final int imageWidth;  // 实际图片宽度
        public final int imageHeight; // 实际图片高度

        public PageLayout(int page, List<LayoutItem> items) {
            this(page, items, 0, 0);
        }

        public PageLayout(int page, List<LayoutItem> items, int imageWidth, int imageHeight) {
            this.page = page;
            this.items = items;
            this.imageWidth = imageWidth;
            this.imageHeight = imageHeight;
        }
    }

    /**
     * 布局项类
     */
    public static class LayoutItem {
        public final double[] bbox; // [x1,y1,x2,y2]
        public final String category;
        public final String text; // may be empty for Picture
        public final String rawHtml; // 原始HTML内容（仅用于表格类型）

        public LayoutItem(double[] bbox, String category, String text) {
            this(bbox, category, text, null);
        }
        
        public LayoutItem(double[] bbox, String category, String text, String rawHtml) {
            this.bbox = bbox;
            this.category = category;
            this.text = text;
            this.rawHtml = rawHtml;
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
        List<CharBox> charBoxesPositioned = parseTextAndPositionsFromResults(layouts, ExtractionStrategy.SEQUENTIAL);
        System.out.println("4. 文本和位置解析（按位置排序）:");
        for (CharBox cb : charBoxesPositioned) {
            System.out.println(String.format("  页%d: '%c' at [%.1f,%.1f,%.1f,%.1f]",
                cb.page, cb.ch, cb.bbox[0], cb.bbox[1], cb.bbox[2], cb.bbox[3]));
        }
        System.out.println();

        System.out.println("=== 示例完成 ===");
    }
    
}
