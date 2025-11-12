package com.zhaoxinms.contract.tools.merge.util;

import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSdtContentRun;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTText;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTColor;

/**
 * HTML内容处理工具类
 * 将HTML内容转换为Word文档格式
 */
public class HtmlUtils {
    
    /**
     * 将HTML内容插入到Word段落中
     * 
     * @param paragraph Word段落对象
     * @param htmlContent HTML内容字符串
     */
    public static void insertHtmlToParagraph(XWPFParagraph paragraph, String htmlContent) {
        try {
            // 解析HTML内容
            Document doc = Jsoup.parse(htmlContent);
            
            // 处理HTML body中的内容
            Element body = doc.body();
            if (body != null) {
                processHtmlElement(paragraph, body);
            } else {
                // 如果没有body标签，直接处理整个文档
                processHtmlElement(paragraph, doc);
            }
        } catch (Exception e) {
            System.err.println("HTML内容处理异常: " + e.getMessage());
            // 如果HTML解析失败，则作为纯文本处理
            XWPFRun run = paragraph.createRun();
            run.setText(htmlContent);
        }
    }
    
    /**
     * 递归处理HTML元素
     * 
     * @param paragraph Word段落对象
     * @param element HTML元素
     */
    private static void processHtmlElement(XWPFParagraph paragraph, Element element) {
        for (Node child : element.childNodes()) {
            if (child instanceof TextNode) {
                // 处理文本节点
                TextNode textNode = (TextNode) child;
                String text = textNode.text().trim();
                if (!text.isEmpty()) {
                    XWPFRun run = paragraph.createRun();
                    run.setText(text);
                }
            } else if (child instanceof Element) {
                Element childElement = (Element) child;
                processHtmlTag(paragraph, childElement);
            }
        }
    }
    
    /**
     * 处理具体的HTML标签
     * 
     * @param paragraph Word段落对象
     * @param element HTML元素
     */
    private static void processHtmlTag(XWPFParagraph paragraph, Element element) {
        String tagName = element.tagName().toLowerCase();
        String text = element.text();
        
        if (text.trim().isEmpty()) {
            return;
        }
        
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        
        // 根据HTML标签设置文本样式
        switch (tagName) {
            case "b":
            case "strong":
                run.setBold(true);
                break;
            case "i":
            case "em":
                run.setItalic(true);
                break;
            case "u":
                run.setUnderline(org.apache.poi.xwpf.usermodel.UnderlinePatterns.SINGLE);
                break;
            case "span":
                // 处理span标签的style属性
                String style = element.attr("style");
                processInlineStyle(run, style);
                break;
            case "font":
                // 支持 <font style="color: ..."> 与 <font color="...">
                String fontStyle = element.attr("style");
                processInlineStyle(run, fontStyle);
                String fontColorAttr = element.attr("color");
                if (fontColorAttr != null && !fontColorAttr.trim().isEmpty()) {
                    String hex = toHexColor(fontColorAttr.trim());
                    if (hex != null) run.setColor(hex);
                }
                break;
            case "p":
                // 段落标签，在文本前后添加换行
                if (!paragraph.getRuns().isEmpty()) {
                    run.addBreak();
                }
                break;
            case "br":
                // 换行标签
                run.addBreak();
                break;
            default:
                // 其他标签作为普通文本处理
                break;
        }
    }
    
    /**
     * 处理内联样式
     * 
     * @param run Word文本运行对象
     * @param style CSS样式字符串
     */
    private static void processInlineStyle(XWPFRun run, String style) {
        if (style == null || style.trim().isEmpty()) {
            return;
        }
        
        // 解析CSS样式
        String[] styles = style.split(";");
        for (String s : styles) {
            String[] keyValue = s.split(":");
            if (keyValue.length == 2) {
                String property = keyValue[0].trim().toLowerCase();
                String value = keyValue[1].trim().toLowerCase();
                
                switch (property) {
                    case "font-weight":
                        if ("bold".equals(value) || "700".equals(value) || "800".equals(value) || "900".equals(value)) {
                            run.setBold(true);
                        }
                        break;
                    case "font-style":
                        if ("italic".equals(value)) {
                            run.setItalic(true);
                        }
                        break;
                    case "text-decoration":
                        if ("underline".equals(value)) {
                            run.setUnderline(org.apache.poi.xwpf.usermodel.UnderlinePatterns.SINGLE);
                        }
                        break;
                    case "color":
                        String hex = toHexColor(value);
                        if (hex != null) run.setColor(hex);
                        break;
                    case "font-size":
                        // 可以根据需要处理字体大小
                        break;
                }
            }
        }
    }
    
    /**
     * 将HTML内容插入到ContentControl中
     * 
     * @param sdtContentRun ContentControl运行对象
     * @param parent 父段落对象
     * @param htmlContent HTML内容字符串
     */
    public static void insertHtmlToContentControl(CTSdtContentRun sdtContentRun, XWPFParagraph parent, String htmlContent) {
        try {
            // 解析HTML内容
            Document doc = Jsoup.parse(htmlContent);
            
            // 处理HTML body中的内容
            Element body = doc.body();
            if (body != null) {
                processHtmlElementToContentControl(sdtContentRun, parent, body);
            } else {
                // 如果没有body标签，直接处理整个文档
                processHtmlElementToContentControl(sdtContentRun, parent, doc);
            }
        } catch (Exception e) {
            System.err.println("[HTML处理异常] " + e.getMessage());
            // 如果HTML解析失败，则作为纯文本处理
            CTR run = sdtContentRun.addNewR();
            CTText text = run.addNewT();
            text.setStringValue(htmlContent);
        }
    }
    
    /**
     * 递归处理HTML元素到ContentControl
     * 
     * @param sdtContentRun ContentControl运行对象
     * @param parent 父段落对象
     * @param element HTML元素
     */
    private static void processHtmlElementToContentControl(CTSdtContentRun sdtContentRun, XWPFParagraph parent, Element element) {
        for (Node child : element.childNodes()) {
            if (child instanceof TextNode) {
                // 处理文本节点
                TextNode textNode = (TextNode) child;
                String text = textNode.text().trim();
                if (!text.isEmpty()) {
                    CTR run = sdtContentRun.addNewR();
                    CTText ctText = run.addNewT();
                    ctText.setStringValue(text);
                }
            } else if (child instanceof Element) {
                Element childElement = (Element) child;
                processHtmlTagToContentControl(sdtContentRun, parent, childElement);
            }
        }
    }
    
    /**
     * 处理具体的HTML标签到ContentControl
     * 
     * @param sdtContentRun ContentControl运行对象
     * @param parent 父段落对象
     * @param element HTML元素
     */
    private static void processHtmlTagToContentControl(CTSdtContentRun sdtContentRun, XWPFParagraph parent, Element element) {
        processHtmlTagToContentControlWithStyle(sdtContentRun, parent, element, null);
    }
    
    /**
     * 处理具体的HTML标签到ContentControl（带样式继承）
     * 
     * @param sdtContentRun ContentControl运行对象
     * @param parent 父段落对象
     * @param element HTML元素
     * @param inheritedStyle 继承的样式（来自父元素）
     */
    private static void processHtmlTagToContentControlWithStyle(CTSdtContentRun sdtContentRun, XWPFParagraph parent, 
                                                                  Element element, CTRPr inheritedStyle) {
        String tagName = element.tagName().toLowerCase();
        
        // 特殊标签处理
        if ("br".equals(tagName)) {
            CTR run = sdtContentRun.addNewR();
            run.addNewBr();
            return;
        }
        
        // 创建当前元素的样式（基于继承样式）
        CTRPr currentStyle = createMergedStyle(element, inheritedStyle);
        
        // 递归处理子节点
        for (Node child : element.childNodes()) {
            if (child instanceof TextNode) {
                // 处理文本节点
                TextNode textNode = (TextNode) child;
                String text = textNode.text();
                if (!text.trim().isEmpty()) {
                    CTR run = sdtContentRun.addNewR();
                    CTText ctText = run.addNewT();
                    ctText.setStringValue(text);
                    
                    // 应用合并后的样式
                    if (currentStyle != null) {
                        run.setRPr((CTRPr) currentStyle.copy());
                    }
                }
            } else if (child instanceof Element) {
                // 递归处理子元素，传递当前样式
                Element childElement = (Element) child;
                processHtmlTagToContentControlWithStyle(sdtContentRun, parent, childElement, currentStyle);
            }
        }
    }
    
    /**
     * 创建合并后的样式（当前元素样式 + 继承样式）
     * 
     * @param element HTML元素
     * @param inheritedStyle 继承的样式
     * @return 合并后的样式
     */
    private static CTRPr createMergedStyle(Element element, CTRPr inheritedStyle) {
        CTRPr style = CTRPr.Factory.newInstance();
        
        // 1. 先复制继承的样式
        if (inheritedStyle != null) {
            try {
                style.set(inheritedStyle);
            } catch (Exception e) {
                // 忽略复制错误
            }
        }
        
        // 2. 应用当前元素的样式（会覆盖继承的样式）
        applyElementStyleToRunProperties(style, element);
        
        return style;
    }
    
    /**
     * 将元素的样式应用到RunProperties
     * 
     * @param runProperties CTRPr对象
     * @param element HTML元素
     */
    private static void applyElementStyleToRunProperties(CTRPr runProperties, Element element) {
        String tagName = element.tagName().toLowerCase();
        
        // 标记是否已设置颜色
        boolean colorSet = false;
        
        // 根据标签类型应用样式
        switch (tagName) {
            case "b":
            case "strong":
                if (runProperties.getBArray().length == 0) {
                    runProperties.addNewB().setVal(true);
                }
                break;
            case "i":
            case "em":
                if (runProperties.getIArray().length == 0) {
                    runProperties.addNewI().setVal(true);
                }
                break;
            case "u":
                if (runProperties.getUArray().length == 0) {
                    runProperties.addNewU().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STUnderline.SINGLE);
                }
                break;
            case "s":
            case "strike":
            case "del":
                // 删除线
                if (runProperties.getStrikeArray().length == 0) {
                    runProperties.addNewStrike().setVal(true);
                }
                break;
            case "sup":
                // 上标
                if (runProperties.getVertAlignArray().length == 0) {
                    runProperties.addNewVertAlign().setVal(org.openxmlformats.schemas.officeDocument.x2006.sharedTypes.STVerticalAlignRun.SUPERSCRIPT);
                }
                break;
            case "sub":
                // 下标
                if (runProperties.getVertAlignArray().length == 0) {
                    runProperties.addNewVertAlign().setVal(org.openxmlformats.schemas.officeDocument.x2006.sharedTypes.STVerticalAlignRun.SUBSCRIPT);
                }
                break;
            case "mark":
                // HTML <mark> 标签表示高亮文本，默认使用黄色背景
                if (runProperties.getShdArray().length == 0) {
                    org.openxmlformats.schemas.wordprocessingml.x2006.main.CTShd shd = runProperties.addNewShd();
                    shd.setFill("FFFF00"); // 默认黄色
                    shd.setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STShd.CLEAR);
                }
                break;
        }
        
        // 处理style属性（所有标签都支持style属性）
        String style = element.attr("style");
        if (style != null && !style.trim().isEmpty()) {
            if (style.contains("color")) {
                colorSet = true;
            }
            processInlineStyleToContentControl(runProperties, style);
        }
        
        // 特殊处理font标签的color属性
        if ("font".equals(tagName)) {
            String fontColorAttr = element.attr("color");
            if (fontColorAttr != null && !fontColorAttr.trim().isEmpty()) {
                String hex = toHexColor(fontColorAttr.trim());
                if (hex != null) {
                    // 清除现有颜色
                    if (runProperties.getColorList() != null && !runProperties.getColorList().isEmpty()) {
                        runProperties.getColorList().clear();
                    }
                    CTColor color = runProperties.addNewColor();
                    color.setVal(hex);
                    colorSet = true;
                }
            }
        }
        
        // 如果没有设置颜色，默认设置为黑色
        if (!colorSet && runProperties.getColorList().isEmpty()) {
            CTColor color = runProperties.addNewColor();
            color.setVal("000000");
        }
    }
    
    /**
     * 处理内联样式到ContentControl
     * 
     * @param runProperties 运行属性对象
     * @param style CSS样式字符串
     */
    private static void processInlineStyleToContentControl(CTRPr runProperties, String style) {
        if (style == null || style.trim().isEmpty()) {
            return;
        }
        
        // 解析CSS样式
        String[] styles = style.split(";");
        for (String s : styles) {
            String[] keyValue = s.split(":");
            if (keyValue.length == 2) {
                String property = keyValue[0].trim().toLowerCase();
                String value = keyValue[1].trim().toLowerCase();
                
                switch (property) {
                    case "font-weight":
                        if ("bold".equals(value) || "700".equals(value) || "800".equals(value) || "900".equals(value)) {
                            runProperties.addNewB().setVal(true);
                        }
                        break;
                    case "font-style":
                        if ("italic".equals(value)) {
                            runProperties.addNewI().setVal(true);
                        }
                        break;
                case "text-decoration":
                    // 支持多值组合，如 "underline line-through"
                    if (value.contains("underline")) {
                        runProperties.addNewU().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STUnderline.SINGLE);
                    }
                    if (value.contains("line-through")) {
                        runProperties.addNewStrike().setVal(true);
                    }
                    break;
                case "background-color":
                case "background":
                    // 处理背景色（高亮）
                    String bgHex = toHexColor(value);
                    if (bgHex != null) {
                        // 清除现有背景色
                        if (runProperties.getShdList() != null && !runProperties.getShdList().isEmpty()) {
                            runProperties.getShdList().clear();
                        }
                        // 设置文字背景色（着色）
                        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTShd shd = runProperties.addNewShd();
                        shd.setFill(bgHex);
                        shd.setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STShd.CLEAR);
                    }
                    break;
                    case "color":
                        String hex = toHexColor(value);
                        if (hex != null) {
                            CTColor color = runProperties.addNewColor();
                            color.setVal(hex);
                        }
                        break;
                    case "font-size":
                        // 处理字体大小
                        try {
                            // 提取数值和单位
                            String sizeStr = value.trim();
                            double fontSize = 0;
                            String unit = "px"; // 默认单位
                            
                            // 尝试解析数值和单位
                            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(px|pt|em|rem)?");
                            java.util.regex.Matcher matcher = pattern.matcher(sizeStr);
                            
                            if (matcher.find()) {
                                fontSize = Double.parseDouble(matcher.group(1));
                                if (matcher.group(2) != null && !matcher.group(2).isEmpty()) {
                                    unit = matcher.group(2).toLowerCase();
                                }
                                
                                // 转换字体大小到Word单位（半磅）
                                int wordFontSize;
                                if ("px".equals(unit)) {
                                    wordFontSize = (int) Math.round(fontSize * 0.75); // 1px ≈ 0.75pt
                                } else if ("pt".equals(unit)) {
                                    wordFontSize = (int) Math.round(fontSize);
                                } else if ("em".equals(unit) || "rem".equals(unit)) {
                                    wordFontSize = (int) Math.round(fontSize * 12); // 1em ≈ 12pt
                                } else {
                                    wordFontSize = (int) Math.round(fontSize);
                                }
                                
                                // 确保字体大小在合理范围内 (1-1638 pt)
                                wordFontSize = Math.max(1, Math.min(1638, wordFontSize));
                                
                                // Word使用半磅单位，所以需要乘以2
                                runProperties.addNewSz().setVal(java.math.BigInteger.valueOf(wordFontSize * 2));
                                runProperties.addNewSzCs().setVal(java.math.BigInteger.valueOf(wordFontSize * 2));
                            }
                        } catch (Exception e) {
                            System.err.println("字体大小解析异常: " + e.getMessage());
                        }
                        break;
                }
            }
        }
    }
    
    /**
     * 将简单的HTML标签转换为纯文本（保留格式信息）
     * 
     * @param htmlContent HTML内容
     * @return 转换后的文本内容
     */
    public static String htmlToPlainText(String htmlContent) {
        try {
            Document doc = Jsoup.parse(htmlContent);
            return doc.text();
        } catch (Exception e) {
            System.err.println("HTML转换为纯文本异常: " + e.getMessage());
            return htmlContent; // 返回原始内容
        }
    }

    // 将 CSS/HTML 颜色表示转为 Word 需要的 hex（不含#）。返回null表示无法解析。
    private static String toHexColor(String color) {
        if (color == null) return null;
        String c = color.trim();
        if (c.startsWith("#")) {
            String hex = c.substring(1);
            return hex.length() == 3 ? expandHex3(hex) : hex;
        }
        String lc = c.toLowerCase();
        if (lc.startsWith("rgb(" ) && lc.endsWith(")")) {
            try {
                String inner = lc.substring(4, lc.length() - 1);
                String[] parts = inner.split(",");
                if (parts.length >= 3) {
                    int r = Integer.parseInt(parts[0].trim());
                    int g = Integer.parseInt(parts[1].trim());
                    int b = Integer.parseInt(parts[2].trim());
                    return String.format("%02X%02X%02X", clamp(r), clamp(g), clamp(b));
                }
            } catch (Exception ignore) {}
        }
        // 常用命名色
        switch (lc) {
            case "white": return "FFFFFF";
            case "black": return "000000";
            case "red": return "FF0000";
            case "green": return "00FF00";
            case "blue": return "0000FF";
        }
        return null;
    }

    private static String expandHex3(String hex3) {
        if (hex3 == null || hex3.length() != 3) return hex3;
        char r = hex3.charAt(0), g = hex3.charAt(1), b = hex3.charAt(2);
        return ("" + r + r + g + g + b + b).toUpperCase();
    }

    private static int clamp(int v) {
        if (v < 0) return 0; if (v > 255) return 255; return v;
    }
} 