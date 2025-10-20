package com.zhaoxinms.contract.template.sdk.controller;

import java.util.HashMap;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.zhaoxinms.contract.template.sdk.service.impl.FileInfoServiceImpl;
import com.zhaoxinms.contract.tools.api.common.ApiResponse;
import com.zhaoxinms.contract.tools.common.entity.FileInfo;
import com.zhaoxinms.contract.tools.onlyoffice.ChangeFileToPDFService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/api/review")
@Api(tags = "智能审核API")
public class ReviewController {

    @Autowired
    private FileInfoServiceImpl fileInfoService;

    @Autowired(required = false)
    private ChangeFileToPDFService changeFileToPDFService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiOperation("上传文件以供智能审核")
    public ApiResponse<Map<String, String>> uploadForReview(@RequestPart("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ApiResponse.paramError("上传的文件不能为空");
        }
        try {
            FileInfo fileInfo = fileInfoService.saveNewFile(file);
            // 可选：上传后将文件转换为 PDF 并注册（保留原 fileId 作为源，PDF 另行返回）
            Map<String, String> data = new HashMap<>();
            data.put("fileId", String.valueOf(fileInfo.getId()));
            return ApiResponse.success("上传成功", data);
        } catch (Exception e) {
            return ApiResponse.<Map<String, String>>serverError().errorDetail("文件上传失败: " + e.getMessage());
        }
    }

    @PostMapping("/persist-pdf-anchors-from-results")
    @ApiOperation("将 DOCX 源转 PDF，并根据 AI results 在 PDF 中插入书签（标题=卡片名），返回 PDF 文件ID")
    public ApiResponse<Map<String, String>> persistPdfAnchorsFromResults(@RequestParam("fileId") String fileId,
                                                                     @RequestBody AiResultsRequest request) {
        try {
            FileInfo src = fileInfoService.getById(fileId);
            if (src == null) return ApiResponse.notFound("fileId 不存在");
            // 1) 先转 PDF
            java.nio.file.Path outPdf = java.nio.file.Files.createTempFile("review_pdf_", ".pdf");
            String downloadUrl = fileInfoService.getFileDownloadUrl(fileId);
            if (changeFileToPDFService == null) return ApiResponse.businessError("PDF转换服务不可用");
            String pdfPath = changeFileToPDFService.covertToPdf(downloadUrl, outPdf.toString());
            if (pdfPath == null) return ApiResponse.businessError("PDF转换失败");

            // 2) 打开 PDF 并插入书签（页级，标题=decisionType 或 pointId）
            try (PDDocument doc = PDDocument.load(new java.io.File(pdfPath))) {
                PDDocumentOutline outline = new PDDocumentOutline();
                doc.getDocumentCatalog().setDocumentOutline(outline);
                java.util.List<AiResultItem> items = request != null ? request.getResults() : null;
                if (items != null) {
                    for (AiResultItem item : items) {
                        java.util.List<Evidence> evs = item.getEvidence();
                        if (evs == null || evs.isEmpty()) continue;
                        for (int i = 0; i < evs.size(); i++) {
                            Evidence ev = evs.get(i);
                            if (ev.getPage() == null || ev.getPage() <= 0 || ev.getPage() > doc.getNumberOfPages()) continue;
                            PDPage page = doc.getPage(ev.getPage() - 1);
                            PDPageXYZDestination dest = new PDPageXYZDestination();
                            dest.setPage(page);
                            dest.setZoom(0); // 保持当前缩放
                            dest.setTop(0);  // 跳到页顶（简单稳妥），如需精确可做文本坐标检索
                            PDOutlineItem bookmark = new PDOutlineItem();
                            String title = (item.getDecisionType() != null && !item.getDecisionType().isEmpty())
                                    ? item.getDecisionType()
                                    : String.valueOf(item.getPointId());
                            bookmark.setTitle(title);
                            bookmark.setDestination(dest);
                            outline.addLast(bookmark);
                        }
                    }
                }
                outline.openNode();
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(pdfPath)) {
                    doc.save(fos);
                }
            }

            // 3) 将 PDF 注册为系统新文件并返回 ID
            FileInfo newInfo = fileInfoService.registerClonedFile(java.nio.file.Paths.get(pdfPath),
                    (src.getOriginalName() != null ? src.getOriginalName().replaceAll("\\.docx$", ".pdf") : "converted.pdf"));
            Map<String, String> data = new HashMap<>();
            data.put("fileId", String.valueOf(newInfo.getId()));
            data.put("fileName", newInfo.getFileName());
            return ApiResponse.success("PDF已生成并写入书签", data);
        } catch (Exception e) {
            return ApiResponse.<Map<String, String>>serverError().errorDetail("PDF书签持久化失败: " + e.getMessage());
        }
    }

    @PostMapping("/persist-anchors")
    @ApiOperation("克隆当前文件并将 anchors 持久化为书签，返回新文件ID")
    public ApiResponse<Map<String, String>> persistAnchors(@RequestParam("fileId") String fileId,
                                                      @RequestBody PersistAnchorsRequest request) {
        try {
            FileInfo newInfo = persistOnClonedFile(fileId, anchorsFromRequest(request));
            java.util.Map<String, String> data = new java.util.HashMap<>();
            data.put("fileId", String.valueOf(newInfo.getId()));
            data.put("fileName", newInfo.getFileName());
            return ApiResponse.success("书签已写入并生成新文件", data);
        } catch (Exception e) {
            return ApiResponse.<Map<String, String>>serverError().errorDetail("持久化书签失败: " + e.getMessage());
        }
    }

    @PostMapping("/persist-anchors-from-results")
    @ApiOperation("根据AI返回的results JSON生成书签并克隆新文件")
    public ApiResponse<Map<String, String>> persistAnchorsFromResults(@RequestParam("fileId") String fileId,
                                                                 @RequestBody AiResultsRequest request) {
        try {
            java.util.List<Anchor> anchors = new java.util.ArrayList<>();
            if (request != null && request.getResults() != null) {
                for (AiResultItem item : request.getResults()) {
                    if (item.getEvidence() == null) continue;
                    for (Evidence ev : item.getEvidence()) {
                        Anchor a = new Anchor();
                        a.setAnchorId(String.valueOf(item.getPointId()) + "_" + anchors.size());
                        a.setParagraphIndex(ev.getParagraphIndex());
                        a.setStartOffset(ev.getStartOffset());
                        a.setEndOffset(ev.getEndOffset());
                        a.setText(ev.getText());
                        anchors.add(a);
                    }
                }
            }
            FileInfo newInfo = persistOnClonedFile(fileId, anchors);
            java.util.Map<String, String> data = new java.util.HashMap<>();
            data.put("fileId", String.valueOf(newInfo.getId()));
            data.put("fileName", newInfo.getFileName());
            return ApiResponse.success("书签已写入并生成新文件", data);
        } catch (Exception e) {
            return ApiResponse.<Map<String, String>>serverError().errorDetail("持久化书签失败: " + e.getMessage());
        }
    }

    private java.util.List<Anchor> anchorsFromRequest(PersistAnchorsRequest req) {
        if (req != null && req.getAnchors() != null) return req.getAnchors();
        return java.util.Collections.<Anchor>emptyList();
    }

    private FileInfo persistOnClonedFile(String fileId, java.util.List<Anchor> anchors) throws Exception {
        FileInfo src = fileInfoService.getById(fileId);
        if (src == null) throw new IllegalArgumentException("fileId 不存在");
        java.nio.file.Path srcPath = java.nio.file.Paths.get(src.getStorePath());
        java.nio.file.Path temp = java.nio.file.Files.createTempFile("review_clone_", ".docx");
        java.nio.file.Files.copy(srcPath, temp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        try (java.io.FileInputStream fis = new java.io.FileInputStream(temp.toFile());
             org.apache.poi.xwpf.usermodel.XWPFDocument doc = new org.apache.poi.xwpf.usermodel.XWPFDocument(fis)) {
            java.util.List<org.apache.poi.xwpf.usermodel.XWPFParagraph> allParas = flattenParagraphs(doc);
            int idSeq = 1;
            for (Anchor a : (anchors != null ? anchors : java.util.Collections.<Anchor>emptyList())) {
                int paraIdx = resolveParagraphIndex(allParas, a);
                if (paraIdx < 0 || paraIdx >= allParas.size()) continue;
                org.apache.poi.xwpf.usermodel.XWPFParagraph p = allParas.get(paraIdx);
                insertBookmarkWithOffsets(p, safe(a.getAnchorId()), a.getStartOffset(), a.getEndOffset(), idSeq);
                idSeq += 2;
            }
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(temp.toFile())) { doc.write(fos); }
        }
        return fileInfoService.registerClonedFile(temp, src.getOriginalName());
    }

    private java.util.List<org.apache.poi.xwpf.usermodel.XWPFParagraph> flattenParagraphs(org.apache.poi.xwpf.usermodel.XWPFDocument doc) {
        java.util.List<org.apache.poi.xwpf.usermodel.XWPFParagraph> list = new java.util.ArrayList<>();
        for (org.apache.poi.xwpf.usermodel.IBodyElement be : doc.getBodyElements()) {
            if (be instanceof org.apache.poi.xwpf.usermodel.XWPFParagraph) {
                list.add((org.apache.poi.xwpf.usermodel.XWPFParagraph) be);
            } else if (be instanceof org.apache.poi.xwpf.usermodel.XWPFTable) {
                org.apache.poi.xwpf.usermodel.XWPFTable t = (org.apache.poi.xwpf.usermodel.XWPFTable) be;
                for (org.apache.poi.xwpf.usermodel.XWPFTableRow row : t.getRows()) {
                    for (org.apache.poi.xwpf.usermodel.XWPFTableCell cell : row.getTableCells()) {
                        list.addAll(cell.getParagraphs());
                    }
                }
            }
        }
        return list;
    }

    private int resolveParagraphIndex(java.util.List<org.apache.poi.xwpf.usermodel.XWPFParagraph> allParas, Anchor a) {
        Integer pi = a.getParagraphIndex();
        if (pi != null && pi >= 0 && pi < allParas.size()) return pi;
        String txt = a.getText();
        if (txt != null && !txt.isEmpty()) {
            for (int i = 0; i < allParas.size(); i++) {
                String ptxt = allParas.get(i).getText();
                if (ptxt != null && ptxt.contains(txt.substring(0, Math.min(12, txt.length())))) return i;
            }
        }
        return -1;
    }

    private void insertBookmarkWithOffsets(org.apache.poi.xwpf.usermodel.XWPFParagraph paragraph, String rawName,
                                           Integer startOffset, Integer endOffset, int id) {
        String name = "risk_anchor_" + rawName;
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP ctp = paragraph.getCTP();
        java.util.List<org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR> rList = ctp.getRList();
        int startRun = 0; int endRun = rList.size() - 1;
        if (startOffset != null && endOffset != null && endOffset >= startOffset && !paragraph.getRuns().isEmpty()) {
            int[] runs = findRunRangeByOffsets(paragraph, startOffset, endOffset);
            if (runs[0] != -1) startRun = runs[0];
            if (runs[1] != -1) endRun = Math.min(runs[1], rList.size() - 1);
        }
        int startPos = findCtpIndexByRun(ctp, paragraph, startRun);
        int endPos = findCtpIndexByRun(ctp, paragraph, endRun) + 1; // after end run
        if (startPos < 0) startPos = 0;
        if (endPos < 0) endPos = rList.size();
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBookmark start = ctp.insertNewBookmarkStart(startPos);
        start.setId(java.math.BigInteger.valueOf(id));
        start.setName(name);
        // adjust end position if list grew
        int adjustedEnd = Math.min(endPos + 1, ctp.sizeOfRArray() + ctp.sizeOfBookmarkStartArray() + ctp.sizeOfBookmarkEndArray());
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTMarkupRange end = ctp.addNewBookmarkEnd();
        end.setId(java.math.BigInteger.valueOf(id));
    }

    private int[] findRunRangeByOffsets(org.apache.poi.xwpf.usermodel.XWPFParagraph p, int start, int end) {
        int acc = 0; int startRun = -1; int endRun = -1;
        java.util.List<org.apache.poi.xwpf.usermodel.XWPFRun> runs = p.getRuns();
        for (int i = 0; i < runs.size(); i++) {
            String t = runs.get(i).text();
            int len = t != null ? t.length() : 0;
            if (startRun == -1 && acc + len >= start) startRun = i;
            if (acc + len >= end) { endRun = i; break; }
            acc += len;
        }
        if (endRun == -1) endRun = runs.size() - 1;
        return new int[]{startRun, endRun};
    }

    private int findCtpIndexByRun(org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP ctp,
                                   org.apache.poi.xwpf.usermodel.XWPFParagraph p, int runIdx) {
        if (runIdx < 0 || runIdx >= p.getRuns().size()) return -1;
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR target = p.getRuns().get(runIdx).getCTR();
        java.util.List<org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR> list = ctp.getRList();
        for (int i = 0; i < list.size(); i++) { if (list.get(i) == target) return i; }
        return -1;
    }

    @lombok.Data
    public static class PersistAnchorsRequest {
        private java.util.List<Anchor> anchors;
    }
    @lombok.Data
    public static class Anchor {
        private String anchorId;
        private Integer paragraphIndex;
        private Integer startOffset;
        private Integer endOffset;
        private String text;
    }

    private static String safe(String s) {
        if (s == null) return "";
        return s.replaceAll("[^A-Za-z0-9_]", "_");
    }

    @lombok.Data
    public static class AiResultsRequest {
        private java.util.List<AiResultItem> results;
    }
    @lombok.Data
    public static class AiResultItem {
        private String clauseType;
        private Object pointId;
        private String algorithmType;
        private String decisionType;
        private String statusType;
        private String message;
        private java.util.List<Evidence> evidence;
    }
    @lombok.Data
    public static class Evidence {
        private String text;
        private Integer page;
        private Integer paragraphIndex;
        private Integer startOffset;
        private Integer endOffset;
    }
}
