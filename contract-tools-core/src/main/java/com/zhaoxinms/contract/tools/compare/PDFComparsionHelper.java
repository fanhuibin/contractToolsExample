package com.zhaoxinms.contract.tools.compare;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationTextMarkup;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import com.zhaoxinms.contract.tools.common.exception.ServiceException;
import com.zhaoxinms.contract.tools.compare.DiffUtil.Operation;
import com.zhaoxinms.contract.tools.compare.result.CompareResult;
import com.zhaoxinms.contract.tools.compare.result.Position;

import cn.hutool.core.util.StrUtil;


/**
 * 处理文本变更的新增部分
 * 
 * @author huibi
 *
 */
public class PDFComparsionHelper extends PDFTextStripper {
 
    private final CompareOptions options;

    public List<TextPosition> position = new ArrayList<TextPosition>(); // 所有文本的位置
    public List<Integer> pageInfo = new ArrayList<Integer>();

    public PDFComparsionHelper() throws IOException {
        this(new CompareOptions());
    }

    public PDFComparsionHelper(CompareOptions options) throws IOException {
        super();
        this.options = options == null ? new CompareOptions() : options;
    }

    /** 在给定索引附近寻找最近的非空字符位置（优先向前）。返回下标，找不到则返回 -1 */
    private static int findNearestBackward(List<TextPosition> positions, int fromIndex) {
        if (positions == null || positions.isEmpty()) return -1;
        int start = Math.min(Math.max(fromIndex, 0), positions.size() - 1);
        for (int i = start; i >= 0; i--) {
            if (positions.get(i) != null) return i;
        }
        return -1;
    }

    /** 在给定索引附近向后寻找第一个非空字符位置。返回下标，找不到则返回 -1 */
    private static int findNearestForward(List<TextPosition> positions, int fromIndex) {
        if (positions == null || positions.isEmpty()) return -1;
        int start = Math.max(fromIndex, 0);
        for (int i = start; i < positions.size(); i++) {
            if (positions.get(i) != null) return i;
        }
        return -1;
    }

    private CompareOptions optionsForExtraction() {
        // 抽取文本阶段不需要颜色，但需要忽略大小写/符号和页眉页脚。
        return this.options;
    }

    // 添加Annotation
    public static void addAnnotation(PDDocument doc, int pageNum, float minX, float minY, float maxX, float maxY,
        String prefix, String tips, float[] rgb) {
        if (StrUtil.isBlank(tips)) {
            return;
        }

        PDPage page = (PDPage)doc.getDocumentCatalog().getPages().get(pageNum);
        List<PDAnnotation> annots = null;
        float height = page.getMediaBox().getHeight();

        try {
            annots = page.getAnnotations();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        PDAnnotationTextMarkup markup = new PDAnnotationTextMarkup(PDAnnotationTextMarkup.SUB_TYPE_HIGHLIGHT);
        float[] color = (rgb != null && rgb.length == 3) ? rgb : new float[]{1, 1, 0};
        PDColor hl = new PDColor(color, PDDeviceRGB.INSTANCE);
        markup.setColor(hl);
        PDRectangle position = new PDRectangle();
        position.setLowerLeftX(minX); // 离页面左下角的x坐标
        position.setLowerLeftY(height - minY); // 离页面左下角的y坐标
        position.setUpperRightX(maxX); //
        position.setUpperRightY(height - maxY);
        markup.setRectangle(position);

        float[] quads = new float[8];

        quads[0] = position.getLowerLeftX(); // x1
        quads[1] = position.getUpperRightY() - 2; // y1
        quads[2] = position.getUpperRightX(); // x2
        quads[3] = quads[1]; // y2
        quads[4] = quads[0]; // x3
        quads[5] = position.getLowerLeftY() - 2; // y3
        quads[6] = quads[2]; // x4
        quads[7] = quads[5]; // y5

        markup.setQuadPoints(quads);
        markup.setSubtype("Highlight");
        markup.setModifiedDate(Calendar.getInstance().getTime().toString());
        markup.setContents(prefix + tips);
        if (annots != null) {
            annots.add(markup);
        }
    }

    public static void main(String args[]) throws Exception {
        // D:\java project\document\Resources/temp\2022/11/16/bbcc5bc252b54cf789c8d60824cdd239.pdf
        // D:\java project\document\Resources/temp\2022/11/16/719d6efc557d4425a96412272e2a4f8c.pdf
        String file1 = "D:\\java project\\document\\Resources/temp\\2022/11/16/bbcc5bc252b54cf789c8d60824cdd239.pdf";
        String file2 = "D:\\java project\\document\\Resources/temp\\2022/11/16/719d6efc557d4425a96412272e2a4f8c.pdf";
        String file3 = "D:\\contractPRO\\contract1\\ruoyi-vue-plus\\zxcm-comparsion\\file\\result.pdf";
        String file4 = "D:\\contractPRO\\contract1\\ruoyi-vue-plus\\zxcm-comparsion\\file\\result1.pdf";

        PDFComparsionHelper.compare(file1, file2, file3, file4);
    }

    public static List<CompareResult> compare(String oldFile, String newFile, String destOld, String destNew)
        throws Exception {
        return compare(oldFile, newFile, destOld, destNew, new CompareOptions());
    }

    public static List<CompareResult> compare(String oldFile, String newFile, String destOld, String destNew, CompareOptions options)
        throws Exception {
        PDDocument oldDocument = null;
        PDDocument newDocument = null;
        String oldContent = "";
        String newContent = "";
        // 创建比对结果集
        List<CompareResult> results = new ArrayList<CompareResult>();
        // 原始文档数据
        List<TextPosition> oldPosition = new ArrayList<TextPosition>();
        List<Integer> oldPageInfo = new ArrayList<Integer>();
        int oldCurrentPosition = 0;
        // 新文档数据
        List<TextPosition> newPosition = new ArrayList<TextPosition>();
        List<Integer> newPageInfo = new ArrayList<Integer>();
        int newCurrentPosition = 0;

        try {
            oldDocument = PDDocument.load(new File(oldFile));
            PDFComparsionHelper stripper = new PDFComparsionHelper(options);
            stripper.setSortByPosition(true);
            // 恢复为 0，保持与旧实现一致（pageInfo 仍使用 getCurrentPageNo()-1 转为 0 基）
            stripper.setStartPage(0);
            stripper.setEndPage(oldDocument.getNumberOfPages());
            oldContent = stripper.getText(oldDocument);
            oldPosition = stripper.getPosition();
            oldPageInfo = stripper.getPageInfo();

            newDocument = PDDocument.load(new File(newFile));
            PDFComparsionHelper stripper2 = new PDFComparsionHelper(options);
            stripper2.setSortByPosition(true);
            stripper2.setStartPage(0);
            stripper2.setEndPage(newDocument.getNumberOfPages());
            newContent = stripper2.getText(newDocument);
            newPosition = stripper2.getPosition();
            newPageInfo = stripper2.getPageInfo();

            // System.out.println(content);
            DiffUtil dmp = new DiffUtil();
            String left = oldContent == null ? "" : oldContent;
            String right = newContent == null ? "" : newContent;
            // 保持与 TextPosition 数组长度一致：仅做大小写归一，不移除符号/空白
            if (stripper.options.isIgnoreCase()) {
                left = left.toLowerCase();
                right = right.toLowerCase();
            }
            LinkedList<DiffUtil.Diff> diff = dmp.diff_main(left, right);
            dmp.diff_cleanupSemantic(diff);

            // 如果文本差异数量过大，比对没有意义
            if (diff.size() > 10000) {
                throw new ServiceException("文档差异过大，比对失败");
            }

            for (int di = 0; di < diff.size(); di++) {
                DiffUtil.Diff d = diff.get(di);
                String realText = d.text.replaceAll("¶", "").replaceAll("\r", "").replaceAll("\n", "");
                String normalized = realText;
                if (stripper.options.isIgnoreSymbols()) {
                    normalized = normalized.replaceAll("[\\p{Punct}\\s]+", "");
                }
                if (stripper.options.isIgnoreCase()) {
                    normalized = normalized.toLowerCase();
                }

                // 记录比对结果
                // 处理空列表的情况
                if (oldPosition.isEmpty() || newPosition.isEmpty()) {
                    continue; // 跳过这个差异，因为没有对应的位置信息
                }

                if (oldCurrentPosition >= oldPosition.size()) {
                    oldCurrentPosition = Math.max(0, oldPosition.size() - 1);
                }
                if (newCurrentPosition >= newPosition.size()) {
                    newCurrentPosition = Math.max(0, newPosition.size() - 1);
                }

                // 确保索引不为负数且在有效范围内
                int oldIndex = Math.max(0, Math.min(oldCurrentPosition, oldPosition.size() - 1));
                int newIndex = Math.max(0, Math.min(newCurrentPosition, newPosition.size() - 1));

                TextPosition p1 = oldPosition.get(oldIndex);
                TextPosition p2 = newPosition.get(newIndex);
                int page1 = oldPageInfo.get(oldIndex);
                int page2 = newPageInfo.get(newIndex);
                // 首先做基础的空位前后补偿（原有：向后）
                if (p1 == null) {
                    int forward = findNearestForward(oldPosition, oldCurrentPosition + 1);
                    if (forward >= 0) {
                        p1 = oldPosition.get(forward);
                        page1 = oldPageInfo.get(forward);
                    }
                }
                if (p2 == null) {
                    int forward = findNearestForward(newPosition, newCurrentPosition + 1);
                    if (forward >= 0) {
                        p2 = newPosition.get(forward);
                        page2 = newPageInfo.get(forward);
                    }
                }

                // 针对删除/新增的定位优化：
                // - DELETE: 新文档没有对应内容，优先取“新文档中当前索引之前”的最近字符作为锚点，避免跳到很靠下的位置
                // - INSERT: 旧文档没有对应内容，优先取“旧文档中当前索引之前”的最近字符作为锚点
                if (d.operation == Operation.DELETE) {
                    int prev = findNearestBackward(newPosition, newCurrentPosition - 1);
                    if (prev >= 0) {
                        p2 = newPosition.get(prev);
                        page2 = newPageInfo.get(prev);
                    } else {
                        // 仍然找不到，则保持之前的前向补偿结果
                    }
                } else if (d.operation == Operation.INSERT) {
                    int prev = findNearestBackward(oldPosition, oldCurrentPosition - 1);
                    if (prev >= 0) {
                        p1 = oldPosition.get(prev);
                        page1 = oldPageInfo.get(prev);
                    }
                }
                if (StrUtil.isNotBlank(realText)) {
                    // 仅保留增删差异；EQUAL 不入结果
                    if (d.operation == Operation.INSERT || d.operation == Operation.DELETE) {
                        // 结果过滤：若差异文本全部由 ignoredSymbols 构成，则忽略（不返回、不标注）。
                        String ignored = options.getIgnoredSymbols();
                        if (ignored != null && !ignored.isEmpty()) {
                            boolean allIgnored = true;
                            for (int ci = 0; ci < realText.length(); ci++) {
                                char ch = realText.charAt(ci);
                                // 同时考虑全角/半角下划线；以及常见空白
                                if (ch == '_' || ch == '＿') {
                                    continue;
                                }
                                if (Character.isWhitespace(ch)) {
                                    continue;
                                }
                                // 其他受配置控制的符号
                                if (ignored.indexOf(ch) >= 0) {
                                    continue;
                                }
                                allIgnored = false; break;
                            }
                            if (allIgnored) {
                                // 同步推进索引，但不记录结果、不做标注
                                if (d.operation == Operation.INSERT) {
                                    newCurrentPosition += realText.length();
                                } else if (d.operation == Operation.DELETE) {
                                    oldCurrentPosition += realText.length();
                                }
                                continue;
                            }
                        }

                        CompareResult result = new CompareResult(new Position(p1, page1), new Position(p2, page2), d);
                        results.add(result);
                    }
                }

                // 文档匹配算法中没有处理\n，所以多行的内容可能认为是一行
                if (d.operation == Operation.EQUAL) {
                    // 坐标跳转
                    oldCurrentPosition += realText.length();
                    newCurrentPosition += realText.length();
                }

                // 判断是否需要跳过标注
                boolean skipAnnotate = false;
                // 大小写或符号导致的替换：DELETE 紧跟 INSERT，且归一化后相同，则跳过两侧标注
                if (!skipAnnotate && stripper.options.isIgnoreCase()) {
                    if (d.operation == Operation.DELETE && di + 1 < diff.size()) {
                        DiffUtil.Diff next = diff.get(di + 1);
                        if (next.operation == Operation.INSERT) {
                            String a = d.text;
                            String b = next.text;
                            if (stripper.options.isIgnoreSymbols()) {
                                a = a.replaceAll("[\\p{Punct}\\s]+", "");
                                b = b.replaceAll("[\\p{Punct}\\s]+", "");
                            }
                            if (a.equalsIgnoreCase(b)) {
                                skipAnnotate = true; // 当前DELETE不标注
                            }
                        }
                    }
                    if (d.operation == Operation.INSERT && di - 1 >= 0) {
                        DiffUtil.Diff prev = diff.get(di - 1);
                        if (prev.operation == Operation.DELETE) {
                            String a = prev.text;
                            String b = d.text;
                            if (a.equalsIgnoreCase(b)) {
                                skipAnnotate = true; // 当前INSERT不标注
                            }
                        }
                    }
                }

                if (d.operation == Operation.INSERT) {
                    // 添加
                    if (!skipAnnotate) {
                        stripper2.findTextAndAnn(newDocument, newPosition, newPageInfo, newCurrentPosition, realText, "新增：\r\n", stripper.options.getInsertRGB());
                    }
                    // 坐标跳转
                    newCurrentPosition += realText.length();
                }

                if (d.operation == Operation.DELETE) {
                    // System.out.println(d);
                    if (!skipAnnotate) {
                        stripper.findTextAndAnn(oldDocument, oldPosition, oldPageInfo, oldCurrentPosition, realText, "删除：\r\n", stripper.options.getDeleteRGB());
                    }
                    // 坐标跳转
                    oldCurrentPosition += realText.length();
                }
            }

            oldDocument.save(destOld);
            newDocument.save(destNew);
        } catch (ServiceException e) {
            if (oldDocument != null) {
                oldDocument.close();
            }
            if (newDocument != null) {
                newDocument.close();
            }
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (oldDocument != null) {
                oldDocument.close();
            }
            if (newDocument != null) {
                newDocument.close();
            }

        }
        return results;
    }

    // 找到文本内容，并且添加注释
    private void findTextAndAnn(PDDocument oldDocument, List<TextPosition> oldPosition,
        List<Integer> oldPageInfo, int oldCurrentPosition, String realText, String typeName, float[] rgb) {
        if (oldCurrentPosition >= oldPageInfo.size()) {
            return; // 特殊情况处理，发生原因未知，不能删除
        }

        StringBuilder s = new StringBuilder();
        String tips = "";
        TextPosition p = null;

        for (int i = 0; i < realText.length(); i++) {
            // 修复文档数组溢出bug
            int location = oldCurrentPosition + i;
            if (location >= oldPosition.size()) {
                location = oldPosition.size() - 1;
            }
            p = oldPosition.get(location);

            if (p != null) {
                s.append(oldPosition.get(location).getUnicode());
            }
        }
        if (s.toString().length() > 160) {
            tips = s.subSequence(0, 150) + "...";
        } else {
            tips = s.toString();
        }

        float minX = 0, minY = 0, maxX = 0, maxY = 0, height = 0;
        int prePage = oldPageInfo.get(oldCurrentPosition);// 获取初始字符的页码

        // 如果文本内容过大会很占用内存，而且这俩个文档差距过大比对没有意义
        if (realText.length() > 10000) {
            throw new ServiceException("文档差异过大，比对失败");
        }
        for (int i = 0; i < realText.length(); i++) {
            int location = oldCurrentPosition + i;
            if (location >= oldPosition.size()) {
                location = oldPosition.size() - 1;
            }
            p = oldPosition.get(location);
            int currentPage = oldPageInfo.get(location);
            if (currentPage != prePage) {
                // 当出现分页的时候，分开标注，每页都做标记
                addAnnotation(oldDocument, prePage, minX, minY, maxX, maxY, typeName, tips, rgb);
                // 重置坐标信息,重置页码信息
                minX = 0;
                minY = 0;
                maxX = 0;
                maxY = 0;
                height = 0;
                prePage = currentPage;
            }
            if (p != null) {
                if (maxY == 0) {
                    maxY = p.getYDirAdj();
                }

                // 判断y坐标是否相同，如果不同认为是一个新行
                if (p.getY() != maxY) {
                    addAnnotation(oldDocument, prePage, minX, minY, maxX, maxY, typeName, s.toString(), rgb);
                    // 重置坐标信息
                    minX = 0;
                    minY = 0;
                    maxX = 0;
                    height = 0;
                    maxY = p.getYDirAdj();
                }

                if (p.getHeight() * 2 > height) {
                    // 判断字体大小，默认取最大的字体
                    // System.out.println(p.getX()+"---"+p.getWidth() + "--" +height
                    // +"--"+p.getWidth()+"--"+p.getHeight());
                    height = p.getHeight() * 2;

                }

                if (minX == 0 || p.getXDirAdj() < minX) {
                    minX = p.getXDirAdj();
                }
                if (maxX == 0 || (p.getXDirAdj() + p.getWidth()) > maxX) {
                    maxX = p.getXDirAdj() + p.getWidth();
                }
                if (minY == 0 || (p.getYDirAdj() - height) < minY) {
                    minY = p.getYDirAdj() - height;
                }
            }
        }
        addAnnotation(oldDocument, prePage, minX, minY, maxX, maxY, typeName, tips, rgb);
    }

    @Override
    protected void writeWordSeparator() throws IOException {
        position.add(null);
        pageInfo.add(this.getCurrentPageNo() - 1);
        output.write(getWordSeparator());
    }

    /**
     * Override the default functionality of PDFTextStripper.writeString()
     */
    @Override
    protected void writeString(String string, List<TextPosition> textPositions) throws IOException {
        if (textPositions.size() > 0) {
            TextPosition text = textPositions.get(0);
            // 判断文字的位置，如果文字位置过高或者过低，我们认为是页眉页脚内容。
            // 页眉和页脚不计算到内容比对中。
            // A4纸大小210mm x 297mm，一般页眉和页脚的高度都是25.4mm
            // 实际使用中发现经常会低于25.4，基于日常经验设置20左右。
            float height = text.getPageHeight();
            float y = text.getYDirAdj();
            if (options.isIgnoreHeaderFooter()) {
                float headerR = options.headerRatio();
                float footerStartR = options.footerStartRatio();
                if (y / height < headerR) {
                    return;
                }
                if (y / height > footerStartR) {
                    return;
                }
            }
        }

        for (TextPosition text : textPositions) {
            position.add(text);
            pageInfo.add(this.getCurrentPageNo() - 1);
        }
        super.writeString(string);
    }

    public List<TextPosition> getPosition() {
        return position;
    }

    public void setPosition(List<TextPosition> position) {
        this.position = position;
    }

    public List<Integer> getPageInfo() {
        return pageInfo;
    }

    public void setPageInfo(List<Integer> pageInfo) {
        this.pageInfo = pageInfo;
    }
}
