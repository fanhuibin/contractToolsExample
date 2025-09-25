package com.zhaoxinms.contract.tools.compare.result;

import org.apache.pdfbox.text.TextPosition;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class Position {
    private final float x;
    private final float y;
    private final float pageHeight;
    private final float pageWidth;
    private final int page;
    // 可选：用于标注的矩形宽高（pt）。当>0时，优先用于高亮框尺寸。
    private float rectWidth;
    private float rectHeight;
    // 可选：多段高亮矩形（支持跨页），每个 float[4] 依次为 xTopLeft, yTop, width, height（单位：pt，自上而下y）
    private List<float[]> rects;
    // 每个矩形对应的页面索引（与rects一一对应，0-based）
    private List<Integer> rectPages;
    public Position(TextPosition p, int page) {
        if(p == null) {
            this.x = 0;
            this.y = 0;
            this.pageHeight = 0;
            this.pageWidth = 0;
        } else {
            this.x = p.getXDirAdj();
            this.y = p.getYDirAdj();
            this.pageHeight = p.getPageHeight();
            this.pageWidth = p.getPageWidth();
        }

        this.page = page;
        this.rectWidth = 0;
        this.rectHeight = 0;
        this.rects = new ArrayList<float[]>();
        this.rectPages = new ArrayList<Integer>();
    }

    /**
     * 构造函数：直接使用坐标与页面尺寸（用于OCR坐标换算到PDF坐标后注入）
     * 说明：x/y 采用与 PDFBox TextPosition.getYDirAdj 相同的"自上而下"的坐标系，
     * pageWidth/pageHeight 为 PDF 页面尺寸（单位：pt），page 为 0-based 页码。
     */
    public Position(float x, float y, float pageWidth, float pageHeight, int page) {
        this.x = x;
        this.y = y;
        this.pageWidth = pageWidth;
        this.pageHeight = pageHeight;
        this.page = page;
        this.rectWidth = 0;
        this.rectHeight = 0;
        this.rects = new ArrayList<float[]>();
        this.rectPages = new ArrayList<Integer>();
    }

    public Position addRect(float xTopLeft, float yTop, float width, float height) {
        return addRect(xTopLeft, yTop, width, height, this.page);
    }
    
    public Position addRect(float xTopLeft, float yTop, float width, float height, int pageIndex) {
        if (this.rects == null) {
            this.rects = new ArrayList<float[]>();
            this.rectPages = new ArrayList<Integer>();
        }
        this.rects.add(new float[]{xTopLeft, yTop, width, height});
        this.rectPages.add(pageIndex);
        return this;
    }
}
