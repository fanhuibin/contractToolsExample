package com.zhaoxinms.contract.tools.merge;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.zhaoxinms.contract.tools.common.Result;
import com.zhaoxinms.contract.tools.common.entity.FileInfo;
import com.zhaoxinms.contract.tools.common.service.FileInfoService;
import com.zhaoxinms.contract.tools.merge.mergeImpl.ContentControlMerge;
import com.zhaoxinms.contract.tools.merge.model.DocContent;
import com.zhaoxinms.contract.tools.onlyoffice.ChangeFileToPDFService;
import com.zhaoxinms.contract.tools.stamp.PdfStampUtil;
import com.zhaoxinms.contract.tools.stamp.RidingStampUtil;
import com.zhaoxinms.contract.tools.stamp.config.StampRule;
import com.zhaoxinms.contract.tools.stamp.config.StampRulesConfig;
import com.zhaoxinms.contract.tools.stamp.config.StampRulesLoader;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 合同合成控制器：根据模板文件与tag->value替换，生成DOCX并转换/注册PDF，返回fileId
 */
@RestController
@RequestMapping("/api/compose")
public class ComposeController {
	private static final Logger logger = LoggerFactory.getLogger(ComposeController.class);
    private static final OkHttpClient HTTP = new OkHttpClient();

    @Value("${zxcm.file-upload.root-path:./uploads}")
    private String uploadRootPath;

    @Autowired(required = false)
    private FileInfoService fileInfoService;
 
    @Autowired
    private ChangeFileToPDFService changeFileToPDFService;

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    @PostMapping(value = "/sdt", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Result<ComposeResponse> composeBySdt(@RequestBody ComposeRequest req) {
        try {
            if (req == null || req.getTemplateFileId() == null || req.getTemplateFileId().trim().isEmpty()) {
                return Result.error("templateFileId 不能为空");
            }
            if (req.getValues() == null || req.getValues().isEmpty()) {
                return Result.error("values 不能为空");
            }

            // 解析模板本地路径（统一通过文件服务获取，不做硬编码）
            String templatePath = fileInfoService != null ? fileInfoService.getFileDiskPath(req.getTemplateFileId()) : null;
            
            if (templatePath == null || templatePath.trim().isEmpty()) {
                return Result.error("无法获取模板文件路径");
            }

            // 生成工作目录与输出文件
            String ts = TS.format(LocalDateTime.now());
            File workDir = ensureWorkDir();
            File outDocx = new File(workDir, "compose_" + ts + ".docx");

            // 构造DocContent列表（使用SDT的tag作为key）
            // 平阳原理：对印章元素在合成前注入隐藏标识，确保PDF文本可被关键词精确定位
            java.util.Map<String, String> mergeValues = new java.util.HashMap<>();
            if (req.getValues() != null) mergeValues.putAll(req.getValues());
            java.util.LinkedHashSet<String> sealKeywordPrefixesSet = new java.util.LinkedHashSet<>();
            java.util.Map<String, String> tagToMarker = new java.util.HashMap<>();

            // 加载规则
            StampRulesConfig rulesCfg = StampRulesLoader.load();
            java.util.Map<String,String> codeToMarkerPrefix = new java.util.HashMap<>();
            java.util.Map<String,String> codeToInsertValue = new java.util.HashMap<>();
            if (rulesCfg.getRules() != null) {
                for (StampRule r : rulesCfg.getRules()) {
                    if (r.getCode() != null && !r.getCode().trim().isEmpty()) {
                        codeToMarkerPrefix.put(r.getCode().toLowerCase(), ("SEAL_" + r.getCode().toUpperCase() + "_"));
                        if (r.getInsertValue() != null) codeToInsertValue.put(r.getCode().toLowerCase(), r.getInsertValue());
                    }
                }
            }

            if (req.getValues() != null) {
                for (Map.Entry<String, String> e : req.getValues().entrySet()) {
                    String tag = e.getKey();
                    String val = e.getValue() == null ? "" : e.getValue();
                    if (tag != null && tag.startsWith("tagElement")) {
                        String keyFull = tag.substring("tagElement".length());
                        String[] parts = keyFull.split("_");
                        if (parts.length >= 3) {
                            // 取除最后两段（时间戳与随机后缀）之外作为 codePrefix（兼容 company_seal 等）
                            StringBuilder codePrefixBuilder = new StringBuilder();
                            for (int i = 0; i < parts.length - 2; i++) {
                                if (i > 0) codePrefixBuilder.append('_');
                                codePrefixBuilder.append(parts[i]);
                            }
                            String codePrefix = codePrefixBuilder.toString();
                            String codeKey = codePrefix.toLowerCase();
                            if (codeKey.contains("seal")) {
                                // 标识前缀
                                String markerPrefix = codeToMarkerPrefix.getOrDefault(codeKey, ("SEAL_" + codePrefix.toUpperCase() + "_"));
                                String marker = markerPrefix + ts;
                                sealKeywordPrefixesSet.add(marker);
                                tagToMarker.put(tag, marker);
                                String markerHtml = "<font style=\"color: white;\">" + marker + "</font>";
                                // 若该印章字段为空且规则给了 insertValue，则写入 insertValue；同时追加隐藏标识
                                String newVal = val;
                                boolean usedInsertValue = false;
                                if ((newVal == null || newVal.trim().isEmpty()) && codeToInsertValue.containsKey(codeKey)) {
                                    newVal = codeToInsertValue.get(codeKey);
                                    usedInsertValue = true;
                                }
                                String visiblePart = (newVal == null ? "" : newVal);
                                if (usedInsertValue && !visiblePart.isEmpty()) {
                                    visiblePart = "<font style=\"color: white;\">" + visiblePart + "</font>";
                                }
                                mergeValues.put(tag, visiblePart + markerHtml);
                            }
                        }
                    }
                }
            }

            List<DocContent> contents = new ArrayList<>();
            for (Map.Entry<String, String> e : mergeValues.entrySet()) {
                contents.add(new DocContent(e.getKey(), e.getValue()));
            }

            // 执行合成
            Merge merger = new ContentControlMerge();
            merger.doMerge(templatePath, outDocx.getAbsolutePath(), contents);

            // 注册文件到文件服务，返回fileId
            FileInfo registered = registerComposedFile(outDocx);
            if (registered == null) {
                return Result.error("注册合成文件失败");
            }

            // === 转PDF并按用户URL优先执行盖章 ===
            String docxRelPath = getRelativePath(outDocx);
            String pdfRelPath = null;
            String stampedRelPath = null;
            String ridingRelPath = null;
            try {
                String pdfPath = changeFileToPDFService != null ? changeFileToPDFService.covertToPdf(registered) : null;
                if (pdfPath != null && !pdfPath.trim().isEmpty()) {
                    File workDir2 = ensureWorkDir();
                    String currentPdf = pdfPath;
                    pdfRelPath = getRelativePath(new File(pdfPath));
                    int normalIdx = 0;

                    // 1) 若前端提供了URL，则直接按URL执行，不再按规则/默认
                    boolean usedUserUrl = req.getStampImageUrls() != null && !req.getStampImageUrls().isEmpty();
                    if (usedUserUrl) {
                        // 普通章：逐个tag应用对应的normal URL，按该tag注入的marker精准盖章
                        for (Map.Entry<String, ComposeRequest.StampImagePair> ent : req.getStampImageUrls().entrySet()) {
                            String tag = ent.getKey();
                            ComposeRequest.StampImagePair pair = ent.getValue();
                            if (pair == null) continue;
                            String normalUrl = pair.getNormal();
                            String marker = tagToMarker.get(tag);
                            if (normalUrl != null && !normalUrl.trim().isEmpty() && marker != null) {
                                String localImg = downloadImageTo(workDir2, normalUrl.trim());
                                if (localImg != null) {
                                    String out = new File(workDir2, "compose_" + ts + "_stamped_" + (++normalIdx) + ".pdf").getAbsolutePath();
                                    PdfStampUtil.addAutoStamp(currentPdf, out, localImg, java.util.Arrays.asList(marker));
                                    currentPdf = out;
                                    stampedRelPath = getRelativePath(new File(out));
                                }
                            }
                        }
                        // 骑缝章：取第一个提供的riding URL
                        String ridingUrl = null;
                        for (ComposeRequest.StampImagePair pair : req.getStampImageUrls().values()) {
                            if (pair != null && pair.getRiding() != null && !pair.getRiding().trim().isEmpty()) { ridingUrl = pair.getRiding().trim(); break; }
                        }
                        if (ridingUrl != null) {
                            String localImg = downloadImageTo(workDir2, ridingUrl);
                            if (localImg != null) {
                                String out = new File(workDir2, "compose_" + ts + "_riding_1.pdf").getAbsolutePath();
                                logger.info("使用骑缝章图片(用户URL): {}", new java.io.File(localImg).getName());
                                RidingStampUtil.addRidingStamp(currentPdf, out, localImg);
                                currentPdf = out;
                                ridingRelPath = getRelativePath(new File(out));
                            }
                        }
                    } else {
                        // 2) 若未提供URL，保留原有规则/默认逻辑
                        String defaultImage = java.nio.file.Paths.get(uploadRootPath, "stamp.png").toString();
                        int ridingIdx = 0;
                        StampRulesConfig rulesCfg2 = rulesCfg;
                        if (rulesCfg2.getRules() != null && !rulesCfg2.getRules().isEmpty()) {
                            for (StampRule r : rulesCfg2.getRules()) {
                                String type = r.getType() == null ? "" : r.getType().toLowerCase();
                                if ("normal".equals(type)) {
                                    String imagePath = resolveImagePath(r.getImage(), defaultImage);
                                    java.util.List<String> kws;
                                    if (r.getKeywords() != null && !r.getKeywords().isEmpty()) {
                                        kws = r.getKeywords();
                                    } else {
                                        // 优先使用注入的 SEAL_ 前缀
                                        kws = sealKeywordPrefixesSet.isEmpty() ? java.util.Arrays.asList("（公章）","（签章）","公章","签章") : new java.util.ArrayList<>(sealKeywordPrefixesSet);
                                    }
                                    String out = new File(workDir2, "compose_" + ts + "_stamped_" + (++normalIdx) + ".pdf").getAbsolutePath();
                                    PdfStampUtil.addAutoStamp(currentPdf, out, imagePath, kws);
                                    currentPdf = out;
                                    stampedRelPath = getRelativePath(new File(out));
                                } else if ("riding".equals(type)) {
                                    String imagePath = resolveImagePath(r.getImage(), defaultImage);
                                    try { logger.info("使用骑缝章图片: {}", new java.io.File(imagePath).getName()); } catch (Exception ignore) {}
                                    String out = new File(workDir2, "compose_" + ts + "_riding_" + (++ridingIdx) + ".pdf").getAbsolutePath();
                                    RidingStampUtil.addRidingStamp(currentPdf, out, imagePath);
                                    currentPdf = out;
                                    ridingRelPath = getRelativePath(new File(out));
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignore) { }

            ComposeResponse resp = new ComposeResponse();
            resp.setFileId(String.valueOf(registered.getId()));
            resp.setDocxPath(docxRelPath);
            resp.setPdfPath(pdfRelPath);
            resp.setStampedPdfPath(stampedRelPath);
            resp.setRidingStampPdfPath(ridingRelPath);
            return Result.success(resp);
        } catch (Exception e) {
            return Result.error("合成失败: " + e.getMessage());
        }
    }

    @PostMapping(value = "/compose-with-stamp", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Result<ComposeWithStampResponse> composeWithStamp(@RequestBody ComposeRequest req) {
        try {
            if (req == null || req.getTemplateFileId() == null || req.getTemplateFileId().trim().isEmpty()) {
                return Result.error("templateFileId 不能为空");
            }
            if (req.getValues() == null || req.getValues().isEmpty()) {
                return Result.error("values 不能为空");
            }

            // 1. 合成DOCX
            String templatePath = fileInfoService != null ? fileInfoService.getFileDiskPath(req.getTemplateFileId()) : null;
            if (templatePath == null || templatePath.trim().isEmpty()) {
                return Result.error("无法获取模板文件路径");
            }

            String ts = TS.format(LocalDateTime.now());
            File workDir = ensureWorkDir();
            File outDocx = new File(workDir, "compose_" + ts + ".docx");

            List<DocContent> contents = new ArrayList<>();
            for (Map.Entry<String, String> e : req.getValues().entrySet()) {
                contents.add(new DocContent(e.getKey(), e.getValue()));
            }

            Merge merger = new ContentControlMerge();
            merger.doMerge(templatePath, outDocx.getAbsolutePath(), contents);

            // 2. 转换为PDF
            String pdfPath = changeFileToPDFService.covertToPdf(outDocx);
            if (pdfPath == null) {
                return Result.error("DOCX转PDF失败");
            }
            File outPdf = new File(pdfPath);

            // 3. 自动识别盖章
            File stampedPdf = new File(workDir, "compose_" + ts + "_stamped.pdf");
            // NOTE: stamp.png should exist in uploads directory for this to work.
            String stampImagePath = Paths.get(uploadRootPath, "stamp.png").toString();
            List<String> keywords = Arrays.asList("（公章）", "（签章）");
            PdfStampUtil.addAutoStamp(outPdf.getAbsolutePath(), stampedPdf.getAbsolutePath(), stampImagePath, keywords);
            
            // 4. 骑缝章
            File ridingStampPdf = new File(workDir, "compose_" + ts + "_riding.pdf");
            try { logger.info("使用骑缝章图片: {}", new java.io.File(stampImagePath).getName()); } catch (Exception ignore) {}
            RidingStampUtil.addRidingStamp(stampedPdf.getAbsolutePath(), ridingStampPdf.getAbsolutePath(), stampImagePath);

            ComposeWithStampResponse resp = new ComposeWithStampResponse();
            resp.setDocxPath(getRelativePath(outDocx));
            resp.setPdfPath(getRelativePath(outPdf)); // Now this is a real PDF
            resp.setStampedPdfPath(getRelativePath(stampedPdf));
            resp.setRidingStampPdfPath(getRelativePath(ridingStampPdf));
            
            return Result.success(resp);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("合成或盖章失败: " + e.getMessage());
        }
    }


    private File ensureWorkDir() {
        try {
            java.nio.file.Path root = java.nio.file.Paths.get(uploadRootPath).toAbsolutePath().normalize();
            java.nio.file.Files.createDirectories(root);
            java.nio.file.Path work = root.resolve("compose");
            java.nio.file.Files.createDirectories(work);
            return work.toFile();
        } catch (Exception e) {
            File fallback = new File(System.getProperty("java.io.tmpdir"), "uploads/compose");
            fallback.mkdirs();
            return fallback;
        }
    }

    private FileInfo registerComposedFile(File outDocx) {
        if (fileInfoService == null) return null;
        String originalName = outDocx.getName();
        String ext = "docx";
        long size = outDocx.length();
        return fileInfoService.registerFile(originalName, ext, outDocx.getAbsolutePath(), size);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ComposeRequest {
        /** 模板文件ID（来自文件服务） */
        @JsonProperty("templateFileId")
        private String templateFileId;
        /** tag->value 映射 */
        @JsonProperty("values")
        private Map<String, String> values;
        /** 用户提供的图片URL映射：key为SDT的tag，normal为普通章URL，riding为骑缝章URL */
        @JsonProperty("stampImageUrls")
        private Map<String, StampImagePair> stampImageUrls;
        public String getTemplateFileId() { return templateFileId; }
        public void setTemplateFileId(String templateFileId) { this.templateFileId = templateFileId; }
        public Map<String, String> getValues() { return values; }
        public void setValues(Map<String, String> values) { this.values = values; }
        public Map<String, StampImagePair> getStampImageUrls() { return stampImageUrls; }
        public void setStampImageUrls(Map<String, StampImagePair> stampImageUrls) { this.stampImageUrls = stampImageUrls; }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class StampImagePair {
            @JsonProperty("normal")
            private String normal;
            @JsonProperty("riding")
            private String riding;
            public String getNormal() { return normal; }
            public void setNormal(String normal) { this.normal = normal; }
            public String getRiding() { return riding; }
            public void setRiding(String riding) { this.riding = riding; }
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ComposeResponse {
        /** 合成后文件ID（用于OnlyOffice或文件下载预览） */
        @JsonProperty("fileId")
        private String fileId;
        @JsonProperty("docxPath")
        private String docxPath;
        @JsonProperty("pdfPath")
        private String pdfPath;
        @JsonProperty("stampedPdfPath")
        private String stampedPdfPath;
        @JsonProperty("ridingStampPdfPath")
        private String ridingStampPdfPath;
        public String getFileId() { return fileId; }
        public void setFileId(String fileId) { this.fileId = fileId; }
        public String getDocxPath() { return docxPath; }
        public void setDocxPath(String docxPath) { this.docxPath = docxPath; }
        public String getPdfPath() { return pdfPath; }
        public void setPdfPath(String pdfPath) { this.pdfPath = pdfPath; }
        public String getStampedPdfPath() { return stampedPdfPath; }
        public void setStampedPdfPath(String stampedPdfPath) { this.stampedPdfPath = stampedPdfPath; }
        public String getRidingStampPdfPath() { return ridingStampPdfPath; }
        public void setRidingStampPdfPath(String ridingStampPdfPath) { this.ridingStampPdfPath = ridingStampPdfPath; }
    }
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ComposeWithStampResponse {
        @JsonProperty("docxPath")
        private String docxPath;
        @JsonProperty("pdfPath")
        private String pdfPath;
        @JsonProperty("stampedPdfPath")
        private String stampedPdfPath;
        @JsonProperty("ridingStampPdfPath")
        private String ridingStampPdfPath;

        public String getDocxPath() { return docxPath; }
        public void setDocxPath(String docxPath) { this.docxPath = docxPath; }
        public String getPdfPath() { return pdfPath; }
        public void setPdfPath(String pdfPath) { this.pdfPath = pdfPath; }
        public String getStampedPdfPath() { return stampedPdfPath; }
        public void setStampedPdfPath(String stampedPdfPath) { this.stampedPdfPath = stampedPdfPath; }
        public String getRidingStampPdfPath() { return ridingStampPdfPath; }
        public void setRidingStampPdfPath(String ridingStampPdfPath) { this.ridingStampPdfPath = ridingStampPdfPath; }
    }
    
    private String getRelativePath(File file) {
        Path rootPath = Paths.get(uploadRootPath).toAbsolutePath();
        Path filePath = file.toPath().toAbsolutePath();
        return rootPath.relativize(filePath).toString().replace(File.separator, "/");
    }
    
    // 解析规则中的图片路径；优先使用绝对路径，其次尝试 uploadRoot 下的相对路径，失败回退默认图片
    private String resolveImagePath(String configuredImage, String defaultImage) {
        try {
            if (configuredImage == null || configuredImage.trim().isEmpty()) {
                return defaultImage;
            }
            java.io.File f = new java.io.File(configuredImage);
            if (f.isAbsolute() && f.exists()) {
                return f.getAbsolutePath();
            }
            java.io.File underUpload = java.nio.file.Paths.get(uploadRootPath, configuredImage).toFile();
            if (underUpload.exists()) {
                return underUpload.getAbsolutePath();
            }
        } catch (Exception ignore) { }
        return defaultImage;
    }

    private String downloadImageTo(File workDir, String url) {
        try {
            Request req = new Request.Builder().url(url).build();
            try (Response resp = HTTP.newCall(req).execute()) {
                if (!resp.isSuccessful()) { logger.warn("下载图片失败: {} -> status {}", url, resp.code()); return null; }
                ResponseBody body = resp.body();
                if (body == null) return null;
                byte[] bytes = body.bytes();
                String ext = guessExtFrom(resp.header("Content-Type"));
                File out = File.createTempFile("stamp_", ext, workDir);
                java.nio.file.Files.write(out.toPath(), bytes);
                return out.getAbsolutePath();
            }
        } catch (Exception ex) {
            logger.warn("下载图片异常: {} -> {}", url, ex.getMessage());
            return null;
        }
    }

    private String guessExtFrom(String contentType) {
        if (contentType == null) return ".img";
        String ct = contentType.toLowerCase();
        if (ct.contains("png")) return ".png";
        if (ct.contains("jpeg") || ct.contains("jpg")) return ".jpg";
        if (ct.contains("gif")) return ".gif";
        return ".img";
    }
}


