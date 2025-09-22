package com.zhaoxinms.contract.tools.comparePRO.model;

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
        return page == charBox.page &&
               ch == charBox.ch &&
               java.util.Arrays.equals(bbox, charBox.bbox) &&
               java.util.Objects.equals(category, charBox.category);
    }

    @Override
    public int hashCode() {
        int result = java.util.Objects.hash(page, ch, category);
        result = 31 * result + java.util.Arrays.hashCode(bbox);
        return result;
    }
}
