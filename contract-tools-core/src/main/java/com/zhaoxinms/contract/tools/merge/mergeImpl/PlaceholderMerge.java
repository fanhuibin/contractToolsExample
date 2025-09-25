package com.zhaoxinms.contract.tools.merge.mergeImpl;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import com.zhaoxinms.contract.tools.merge.Merge;
import com.zhaoxinms.contract.tools.merge.model.DocContent;

import cn.hutool.core.io.FileTypeUtil;

public class PlaceholderMerge implements Merge {

    @Override
    public void doMerge(String inputFile, String outputFile, List<DocContent> contents) {
        String type = FileTypeUtil.getTypeByPath(inputFile);
        if ("doc".equals(type)) {
            throw new RuntimeException("doc文档不支持");
        }

        if ("zip".equals(type) || "docx".equals(type)) {
            try {
                this.mergeDocx(inputFile, outputFile, contents);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                System.out.println("文件未找到");
            } catch (IOException e) {
                System.out.println("IO异常");
            }
        }
    }

    private void mergeDocx(String inputFile, String outputFile, List<DocContent> contents) throws FileNotFoundException, IOException {
        // 加载Word文档
        try (FileInputStream fis = new FileInputStream(inputFile);
             XWPFDocument document = new XWPFDocument(fis)) {

            // 遍历文档中的所有段落
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                replacePlaceholderInParagraph(paragraph, contents);
            }

            // 遍历文档中的所有表格
            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        for (XWPFParagraph paragraph : cell.getParagraphs()) {
                            replacePlaceholderInParagraph(paragraph, contents);
                        }
                    }
                }
            }

            // 保存修改后的文档
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                document.write(fos);
            }
        }
    }

    private void replacePlaceholderInParagraph(XWPFParagraph paragraph, List<DocContent> contents) {
        for (DocContent docContent : contents) {
            String placeholder = docContent.getKey();
            String replacement = docContent.getContent();

            for (XWPFRun run : paragraph.getRuns()) {
                String text = run.getText(0);
                if (text != null && text.contains(placeholder)) {
                    // 替换占位符并保留样式
                    text = text.replace(placeholder, replacement);
                    run.setText(text, 0);
                }
            }
        }
    }
}