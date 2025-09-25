package com.zhaoxinms.contract.tools.stamp;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.itextpdf.text.pdf.parser.ImageRenderInfo;
import com.itextpdf.text.pdf.parser.LineSegment;
import com.itextpdf.text.pdf.parser.RenderListener;
import com.itextpdf.text.pdf.parser.TextRenderInfo;
import com.itextpdf.text.pdf.parser.Vector;

/**
 * PDF文字位置定位器
 * 使用iText库实现精确的文字位置定位
 * 参考PDFComparsionHelper的定位思路
 */
public class PdfTextLocationStrategy implements RenderListener {

    private static final Logger logger = LoggerFactory.getLogger(PdfTextLocationStrategy.class);

    // 存储所有文本位置信息
    private List<TextLocationInfo> textInfos = new ArrayList<>();
    // 当前要查找的关键词列表
    private List<String> targetKeywords = new ArrayList<>();
    // 找到的关键词位置信息
    private List<TextLocationInfo> foundLocations = new ArrayList<>();
    // 当前页码（从0开始，对齐 iText 渲染回调）
    private int currentPageNumber = 0;
    // 完整文本内容
    private StringBuilder fullText = new StringBuilder();

    /**
     * 设置当前页码
     */
    public void setCurrentPageNumber(int pageNumber) {
        this.currentPageNumber = pageNumber;
    }

    /**
     * 设置要查找的关键词
     */
    public void setTargetKeywords(List<String> keywords) {
        this.targetKeywords = keywords != null ? new ArrayList<>(keywords) : new ArrayList<>();
        this.foundLocations.clear();
    }

    /**
     * 获取找到的关键词位置信息
     */
    public List<TextLocationInfo> getFoundLocations() {
        return new ArrayList<>(foundLocations);
    }

    /**
     * 清空所有数据，准备处理新文档
     */
    public void clear() {
        textInfos.clear();
        foundLocations.clear();
        fullText.setLength(0);
        currentPageNumber = 0;
    }

    @Override
    public void beginTextBlock() {
        // 开始文本块处理
    }

    @Override
    public void endTextBlock() {
        // 结束文本块处理
    }

    @Override
    public void renderText(TextRenderInfo renderInfo) {
        String text = renderInfo.getText();
        if (text != null && text.trim().length() > 0) {
            // 获取文本的基线和上升线
            LineSegment baseline = renderInfo.getBaseline();
            LineSegment ascentLine = renderInfo.getAscentLine();

            // 获取文本位置信息
            Vector startPoint = baseline.getStartPoint();
            Vector endPoint = baseline.getEndPoint();

            float x = startPoint.get(Vector.I1);
            float y = startPoint.get(Vector.I2);
            float width = endPoint.get(Vector.I1) - startPoint.get(Vector.I1);
            float height = ascentLine.getStartPoint().get(Vector.I2) - baseline.getStartPoint().get(Vector.I2);

            // 过滤页眉页脚内容（A4页面高度）
            float pageHeight = 842f;
            if (y / pageHeight < 20f / 297f || y / pageHeight > (297f - 20f) / 297f) {
                return; // 跳过页眉页脚
            }

            // 创建文本位置信息
            TextLocationInfo textInfo = new TextLocationInfo();
            textInfo.setText(text);
            textInfo.setPageNumber(currentPageNumber);
            textInfo.setX(x);
            textInfo.setY(y);
            textInfo.setWidth(Math.abs(width));
            textInfo.setHeight(Math.abs(height));
            textInfo.setCenterX(x + width / 2);
            textInfo.setCenterY(y + height / 2);
            textInfo.setTextStartIndex(fullText.length());
            textInfo.setTextLength(text.length());

            textInfos.add(textInfo);
            fullText.append(text);
        }
    }

    @Override
    public void renderImage(ImageRenderInfo renderInfo) {
        // 图像渲染处理（暂不需要）
    }

    /**
     * 查找关键词位置（在所有文本提取完成后调用）
     */
    public void findKeywordLocations() {
        if (targetKeywords.isEmpty() || textInfos.isEmpty()) {
            return;
        }

        String text = fullText.toString();
        for (String keyword : targetKeywords) {
            if (keyword == null || keyword.trim().isEmpty()) {
                continue;
            }

            String trimmedKeyword = keyword.trim();
            int startIndex = 0;
            while (true) {
                int foundIndex = text.indexOf(trimmedKeyword, startIndex);
                if (foundIndex == -1) {
                    break;
                }

                TextLocationInfo locationInfo = getLocationInfoByTextIndex(foundIndex, trimmedKeyword.length());
                if (locationInfo != null) {
                    locationInfo.setKeyword(trimmedKeyword);
                    foundLocations.add(locationInfo);
                } else {
                    logger.warn("无法获取关键词位置: {}", trimmedKeyword);
                }

                startIndex = foundIndex + 1; // 继续查找下一个匹配
            }
        }
    }

    /**
     * 根据文本索引获取位置信息
     */
    private TextLocationInfo getLocationInfoByTextIndex(int textIndex, int keywordLength) {
        TextLocationInfo startInfo = null;
        TextLocationInfo endInfo = null;
        for (TextLocationInfo info : textInfos) {
            int startIdx = info.getTextStartIndex();
            int endIdx = startIdx + info.getTextLength();
            if (startInfo == null && textIndex >= startIdx && textIndex < endIdx) {
                startInfo = info;
            }
            int keywordEndIndex = textIndex + keywordLength - 1;
            if (keywordEndIndex >= startIdx && keywordEndIndex < endIdx) {
                endInfo = info;
                break;
            }
        }
        if (startInfo == null) {
            logger.warn("无法定位文本起点索引: {}", textIndex);
            return null;
        }
        if (endInfo == null) endInfo = startInfo;

        TextLocationInfo result = new TextLocationInfo();
        result.setPageNumber(startInfo.getPageNumber());
        float minX = Math.min(startInfo.getX(), endInfo.getX());
        float maxX = Math.max(startInfo.getX() + startInfo.getWidth(), endInfo.getX() + endInfo.getWidth());
        float minY = Math.min(startInfo.getY(), endInfo.getY());
        float maxY = Math.max(startInfo.getY() + startInfo.getHeight(), endInfo.getY() + endInfo.getHeight());
        result.setX(minX);
        result.setY(minY);
        result.setWidth(maxX - minX);
        result.setHeight(maxY - minY);
        result.setCenterX(minX + (maxX - minX) / 2);
        result.setCenterY(minY + (maxY - minY) / 2);
        return result;
    }

    /**
     * 文字位置信息
     */
    public static class TextLocationInfo {
        private String keyword;
        private String text;
        private int pageNumber;
        private float x;
        private float y;
        private float width;
        private float height;
        private float centerX;
        private float centerY;
        private int textStartIndex; // 在完整文本中的起始索引
        private int textLength; // 文本长度

        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public int getPageNumber() { return pageNumber; }
        public void setPageNumber(int pageNumber) { this.pageNumber = pageNumber; }
        public float getX() { return x; }
        public void setX(float x) { this.x = x; }
        public float getY() { return y; }
        public void setY(float y) { this.y = y; }
        public float getWidth() { return width; }
        public void setWidth(float width) { this.width = width; }
        public float getHeight() { return height; }
        public void setHeight(float height) { this.height = height; }
        public float getCenterX() { return centerX; }
        public void setCenterX(float centerX) { this.centerX = centerX; }
        public float getCenterY() { return centerY; }
        public void setCenterY(float centerY) { this.centerY = centerY; }
        public int getTextStartIndex() { return textStartIndex; }
        public void setTextStartIndex(int textStartIndex) { this.textStartIndex = textStartIndex; }
        public int getTextLength() { return textLength; }
        public void setTextLength(int textLength) { this.textLength = textLength; }

        @Override
        public String toString() {
            return String.format("TextLocationInfo{keyword='%s', text='%s', page=%d, pos=(%.2f,%.2f), size=%.2fx%.2f, center=(%.2f,%.2f)}",
                keyword, text, pageNumber, x, y, width, height, centerX, centerY);
        }
    }
} 