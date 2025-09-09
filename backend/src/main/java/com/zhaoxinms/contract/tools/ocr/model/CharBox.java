package com.zhaoxinms.contract.tools.ocr.model;

/**
 * 字符框类 - OCR结果中的字符及其位置信息
 *
 * @author zhaoxin
 * @version 1.0
 * @since 2025-01-14
 */
public class CharBox {
    public final int page;
    public final char ch;
    public final double[] bbox;
    public final String category;

    public CharBox(int page, char ch, double[] bbox, String category) {
        this.page = page;
        this.ch = ch;
        this.bbox = bbox;
        this.category = category;
    }

    @Override
    public String toString() {
        return String.format("CharBox{page=%d, ch='%c', bbox=[%.1f,%.1f,%.1f,%.1f], category='%s'}",
            page, ch, bbox[0], bbox[1], bbox[2], bbox[3], category);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        CharBox charBox = (CharBox) obj;

        if (page != charBox.page) return false;
        if (ch != charBox.ch) return false;
        if (category != null ? !category.equals(charBox.category) : charBox.category != null) return false;

        if (bbox != null && charBox.bbox != null) {
            if (bbox.length != charBox.bbox.length) return false;
            for (int i = 0; i < bbox.length; i++) {
                if (Double.compare(bbox[i], charBox.bbox[i]) != 0) return false;
            }
        } else if (bbox != charBox.bbox) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = page;
        result = 31 * result + (int) ch;
        if (bbox != null) {
            for (double v : bbox) {
                long temp = Double.doubleToLongBits(v);
                result = 31 * result + (int) (temp ^ (temp >>> 32));
            }
        }
        result = 31 * result + (category != null ? category.hashCode() : 0);
        return result;
    }
}
