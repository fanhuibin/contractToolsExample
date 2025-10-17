package com.zhaoxinms.contract.tools.extract.model;

/**
 * 文本框类 - OCR结果中的文本块及其位置信息
 * 用于OCR文本提取的可视化，一个TextBox代表一个文本块（如一行文字、一个单元格）
 */
public class TextBox {
    public final int page;
    public final String text;
    public final double[] bbox; // [x1, y1, x2, y2]
    public final String category;
    public final int startPos; // 在完整文本中的起始字符索引
    public final int endPos;   // 在完整文本中的结束字符索引

    public TextBox(int page, String text, double[] bbox, String category, int startPos, int endPos) {
        this.page = page;
        this.text = text;
        this.bbox = bbox;
        this.category = category;
        this.startPos = startPos;
        this.endPos = endPos;
    }

    // Getter方法 - Jackson序列化需要
    public int getPage() {
        return page;
    }

    public String getText() {
        return text;
    }

    public double[] getBbox() {
        return bbox;
    }

    public String getCategory() {
        return category;
    }

    public int getStartPos() {
        return startPos;
    }

    public int getEndPos() {
        return endPos;
    }

    @Override
    public String toString() {
        return String.format("TextBox{page=%d, text='%s', bbox=[%.1f,%.1f,%.1f,%.1f], category='%s', pos=[%d-%d]}",
            page, text.length() > 20 ? text.substring(0, 20) + "..." : text, 
            bbox[0], bbox[1], bbox[2], bbox[3], category, startPos, endPos);
    }
}

