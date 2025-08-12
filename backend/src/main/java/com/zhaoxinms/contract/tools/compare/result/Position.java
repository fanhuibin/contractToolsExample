package com.zhaoxinms.contract.tools.compare.result;

import org.apache.pdfbox.text.TextPosition;

import lombok.Data;

@Data
public class Position {
    private final float x;
    private final float y;
    private final float pageHeight;
    private final float pageWidth;
    private final int page;
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
    }
}
