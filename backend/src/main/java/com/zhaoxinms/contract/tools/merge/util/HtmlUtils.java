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
            System.err.println("HTML内容处理异常: " + e.getMessage());
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
        String tagName = element.tagName().toLowerCase();
        String text = element.text();
        
        if (text.trim().isEmpty()) {
            return;
        }
        
        CTR run = sdtContentRun.addNewR();
        CTText ctText = run.addNewT();
        ctText.setStringValue(text);
        
        // 根据HTML标签设置文本样式
        CTRPr runProperties = run.addNewRPr();
        
        switch (tagName) {
            case "b":
            case "strong":
                runProperties.addNewB().setVal(true);
                break;
            case "i":
            case "em":
                runProperties.addNewI().setVal(true);
                break;
            case "u":
                runProperties.addNewU().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STUnderline.SINGLE);
                break;
            case "span":
                // 处理span标签的style属性
                String style = element.attr("style");
                processInlineStyleToContentControl(runProperties, style);
                break;
            case "font":
                // 支持 <font style="color: ..."> 与 <font color="...">
                String fontStyle = element.attr("style");
                processInlineStyleToContentControl(runProperties, fontStyle);
                String fontColorAttr = element.attr("color");
                if (fontColorAttr != null && !fontColorAttr.trim().isEmpty()) {
                    String hex = toHexColor(fontColorAttr.trim());
                    if (hex != null) {
                        CTColor color = runProperties.addNewColor();
                        color.setVal(hex);
                    }
                }
                break;
            case "p":
                // 段落标签处理 - 可能需要添加换行
                break;
            case "br":
                // 换行标签 - 添加换行符
                run.addNewBr();
                break;
            default:
                // 其他标签作为普通文本处理
                break;
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
                        if ("underline".equals(value)) {
                            runProperties.addNewU().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STUnderline.SINGLE);
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
                        // 可以根据需要处理字体大小
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