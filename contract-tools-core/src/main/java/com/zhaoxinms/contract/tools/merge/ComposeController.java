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
import com.zhaoxinms.contract.tools.api.common.ApiResponse;
import com.zhaoxinms.contract.tools.auth.annotation.RequireFeature;
import com.zhaoxinms.contract.tools.auth.enums.ModuleType;
import com.zhaoxinms.contract.tools.common.entity.FileInfo;
import com.zhaoxinms.contract.tools.common.service.FileInfoService;
import com.zhaoxinms.contract.tools.merge.mergeImpl.ContentControlMerge;
import com.zhaoxinms.contract.tools.merge.model.DocContent;
import com.zhaoxinms.contract.tools.onlyoffice.ChangeFileToPDFService;
import com.zhaoxinms.contract.tools.stamp.PdfStampUtil;
import com.zhaoxinms.contract.tools.stamp.RidingStampUtil;
import com.zhaoxinms.contract.tools.common.util.FileStorageUtils;
import com.zhaoxinms.contract.tools.template.entity.TemplateDesignRecord;
import com.zhaoxinms.contract.tools.template.service.TemplateDesignRecordService;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 合同合成控制器：根据模板文件与tag->value替换，生成DOCX并转换/注册PDF，返回fileId
 */
@RestController
@RequestMapping("/api/compose")
@RequireFeature(module = ModuleType.SMART_CONTRACT_SYNTHESIS, message = "智能合同合成功能需要授权")
public class ComposeController {
	private static final Logger logger = LoggerFactory.getLogger(ComposeController.class);
    private static final OkHttpClient HTTP = new OkHttpClient();

    @Value("${zxcm.file-upload.root-path:./uploads}")
    private String uploadRootPath;

    @Autowired(required = false)
    private FileInfoService fileInfoService;
 
    @Autowired
    private ChangeFileToPDFService changeFileToPDFService;
    
    @Autowired(required = false)
    private TemplateDesignRecordService templateDesignRecordService;

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    @PostMapping(value = "/sdt", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<ComposeResponse> composeBySdt(@RequestBody ComposeRequest req) {
        try {
            logger.info("开始合成合同，templateFileId: {}, templateCode: {}", 
                req != null ? req.getTemplateFileId() : "null",
                req != null ? req.getTemplateCode() : "null");
            
            // 参数校验：templateFileId 和 templateCode 至少提供一个
            if (req == null) {
                return ApiResponse.paramError("请求参数不能为空");
            }
            
            String actualTemplateFileId = null;
            
            // 优先使用模板编号（templateCode），支持多版本
            if (req.getTemplateCode() != null && !req.getTemplateCode().trim().isEmpty()) {
                logger.info("使用模板编号查找模板: {}", req.getTemplateCode());
                
                if (templateDesignRecordService == null) {
                    logger.error("模板设计记录服务不可用，无法通过模板编号查找");
                    return ApiResponse.paramError("模板设计记录服务不可用，请使用 templateFileId");
                }
                
                try {
                    // 优先获取已发布的版本，如果没有则获取最新版本
                    TemplateDesignRecord template = 
                        templateDesignRecordService.getPublishedByCode(req.getTemplateCode().trim());
                    
                    if (template == null) {
                        // 如果没有已发布的版本，尝试获取最新版本
                        logger.info("未找到已发布的模板，尝试获取最新版本");
                        template = templateDesignRecordService.getLatestByCode(req.getTemplateCode().trim());
                    }
                    
                    if (template == null) {
                        logger.error("未找到模板，templateCode: {}", req.getTemplateCode());
                        return ApiResponse.paramError("未找到模板，模板编号: " + req.getTemplateCode());
                    }
                    
                    if (template.getFileId() == null) {
                        logger.error("模板文件ID为空，templateCode: {}", req.getTemplateCode());
                        return ApiResponse.paramError("模板文件ID为空，模板编号: " + req.getTemplateCode());
                    }
                    
                    actualTemplateFileId = String.valueOf(template.getFileId());
                    logger.info("通过模板编号找到模板，templateCode: {}, fileId: {}, version: {}", 
                        req.getTemplateCode(), actualTemplateFileId, template.getVersion());
                } catch (Exception e) {
                    logger.error("通过模板编号查找模板失败: {}", req.getTemplateCode(), e);
                    return ApiResponse.paramError("查找模板失败: " + e.getMessage());
                }
            } else if (req.getTemplateFileId() != null && !req.getTemplateFileId().trim().isEmpty()) {
                // 使用模板文件ID（向后兼容）
                actualTemplateFileId = req.getTemplateFileId().trim();
                logger.info("使用模板文件ID: {}", actualTemplateFileId);
            } else {
                logger.error("templateFileId 和 templateCode 都为空");
                return ApiResponse.paramError("templateFileId 或 templateCode 至少提供一个");
            }
            
            // values 可以为空（对于纯静态模板或只有印章的模板）
            if (req.getValues() == null) {
                logger.warn("values 为 null，使用空 Map");
                req.setValues(new java.util.HashMap<>());
            }

            // 解析模板本地路径（统一通过文件服务获取，不做硬编码）
            logger.info("查询模板文件路径，templateFileId: {}", actualTemplateFileId);
            String templatePath = fileInfoService != null ? fileInfoService.getFileDiskPath(actualTemplateFileId) : null;
            logger.info("获取到的模板路径: {}", templatePath);
            
            if (templatePath == null || templatePath.trim().isEmpty()) {
                logger.error("无法获取模板文件路径，templateFileId: {}", actualTemplateFileId);
                return ApiResponse.paramError("文件不存在：无法获取模板文件路径，文件ID: " + actualTemplateFileId);
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

            if (req.getValues() != null) {
                for (Map.Entry<String, String> e : req.getValues().entrySet()) {
                    ensureSealMarker(e.getKey(), e.getValue(), ts, mergeValues, tagToMarker, sealKeywordPrefixesSet);
                }
            }
            if (req.getStampImageUrls() != null && !req.getStampImageUrls().isEmpty()) {
                for (String tag : req.getStampImageUrls().keySet()) {
                    if (!tagToMarker.containsKey(tag)) {
                        ensureSealMarker(tag, null, ts, mergeValues, tagToMarker, sealKeywordPrefixesSet);
                    }
                }
            }

            // 打印所有接收到的变量值（调试用）
            logger.info("===== 接收到的所有变量值 =====");
            for (Map.Entry<String, String> e : mergeValues.entrySet()) {
                logger.info("变量: {} = {}", e.getKey(), e.getValue());
            }
            logger.info("===== 变量值列表结束 =====");
            
            // 条款变量替换预处理
            // 对于包含 ${variable} 或 {{variable}} 格式的字段，先替换变量
            java.util.Map<String, String> processedValues = new java.util.HashMap<>();
            for (Map.Entry<String, String> e : mergeValues.entrySet()) {
                String tag = e.getKey();
                String value = e.getValue();
                
                // 检查是否包含变量（${...} 或 {{...}}）
                if (value != null && (value.contains("${") || value.contains("{{"))) {
                    logger.info("检测到条款字段包含变量，tag: {}, 原始值: {}", tag, value);
                    
                    // 替换 ${variable} 格式的变量
                    String processed = value;
                    java.util.regex.Pattern pattern1 = java.util.regex.Pattern.compile("\\$\\{([\\w]+)\\}");
                    java.util.regex.Matcher matcher1 = pattern1.matcher(value);
                    StringBuffer sb1 = new StringBuffer();
                    while (matcher1.find()) {
                        String varName = matcher1.group(1);
                        String varValue = mergeValues.getOrDefault(varName, "");
                        // 如果变量值本身也包含 $，需要转义
                        String replacement = varValue.replace("$", "\\$").replace("\\", "\\\\");
                        matcher1.appendReplacement(sb1, replacement);
                        logger.info("替换变量 ${{{}}}: {} -> {}", varName, varName, varValue);
                    }
                    matcher1.appendTail(sb1);
                    processed = sb1.toString();
                    
                    // 替换 {{variable}} 格式的变量
                    java.util.regex.Pattern pattern2 = java.util.regex.Pattern.compile("\\{\\{([\\w]+)\\}\\}");
                    java.util.regex.Matcher matcher2 = pattern2.matcher(processed);
                    StringBuffer sb2 = new StringBuffer();
                    while (matcher2.find()) {
                        String varName = matcher2.group(1);
                        String varValue = mergeValues.getOrDefault(varName, "");
                        String replacement = varValue.replace("$", "\\$").replace("\\", "\\\\");
                        matcher2.appendReplacement(sb2, replacement);
                        logger.info("替换变量 {{{{{}}}}}: {} -> {}", varName, varName, varValue);
                    }
                    matcher2.appendTail(sb2);
                    processed = sb2.toString();
                    
                    logger.info("条款变量替换完成，tag: {}, 替换后: {}", tag, processed);
                    processedValues.put(tag, processed);
                } else {
                    processedValues.put(tag, value);
                }
            }

            List<DocContent> contents = new ArrayList<>();
            for (Map.Entry<String, String> e : processedValues.entrySet()) {
                contents.add(new DocContent(e.getKey(), e.getValue()));
            }

            // 执行合成
            Merge merger = new ContentControlMerge();
            merger.doMerge(templatePath, outDocx.getAbsolutePath(), contents);

            // 注册文件到文件服务，返回fileId
            FileInfo registered = registerComposedFile(outDocx);
            if (registered == null) {
                return ApiResponse.<ComposeResponse>serverError().errorDetail("注册合成文件失败");
            }

            // === 转PDF、合并额外PDF、并按用户URL优先执行盖章 ===
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
                    
                    // === 合并额外的PDF文件（如果有） ===
                    if (req.getExtraFiles() != null && !req.getExtraFiles().isEmpty()) {
                        logger.info("开始合并额外PDF文件，共{}个", req.getExtraFiles().size());
                        try {
                            List<String> extraPdfPaths = new ArrayList<>();
                            // 下载所有额外的PDF文件
                            for (String extraFileUrl : req.getExtraFiles()) {
                                if (extraFileUrl == null || extraFileUrl.trim().isEmpty()) {
                                    logger.warn("跳过空的PDF文件URL");
                                    continue;
                                }
                                String localPdf = downloadPdfTo(workDir2, extraFileUrl.trim());
                                if (localPdf != null) {
                                    extraPdfPaths.add(localPdf);
                                    logger.info("成功下载额外PDF文件: {}", localPdf);
                                } else {
                                    logger.warn("下载额外PDF文件失败: {}", extraFileUrl);
                                }
                            }
                            
                            // 合并PDF文件
                            if (!extraPdfPaths.isEmpty()) {
                                String mergedPdfPath = new File(workDir2, "compose_" + ts + "_merged.pdf").getAbsolutePath();
                                mergePdfFiles(currentPdf, extraPdfPaths, mergedPdfPath);
                                currentPdf = mergedPdfPath;
                                pdfRelPath = getRelativePath(new File(mergedPdfPath));
                                logger.info("PDF合并完成，合并后文件: {}", mergedPdfPath);
                            }
                        } catch (Exception e) {
                            logger.error("合并额外PDF文件失败，继续使用原始PDF", e);
                            // 合并失败不影响主流程，继续使用原始PDF
                        }
                    }
                    
                    int normalIdx = 0;

                    // 1) 普通章：若前端提供了URL，则按URL执行
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
                                    // 创建印章配置，设置自定义尺寸
                                    PdfStampUtil.StampConfig stampConfig = new PdfStampUtil.StampConfig(
                                        localImg,
                                        java.util.Arrays.asList(marker)
                                    );
                                    // 设置印章尺寸（如果有提供）
                                    if (pair.getWidth() != null && pair.getWidth() > 0) {
                                        stampConfig.setStampWidth(pair.getWidth());
                                    }
                                    if (pair.getHeight() != null && pair.getHeight() > 0) {
                                        stampConfig.setStampHeight(pair.getHeight());
                                    }
                                    PdfStampUtil.addAutoStamp(currentPdf, out, stampConfig);
                                    currentPdf = out;
                                    stampedRelPath = getRelativePath(new File(out));
                                }
                            }
                        }
                    }
                    
                    // 2) 骑缝章：如果明确提供了ridingStampUrl，则盖骑缝章
                    if (req.getRidingStampUrl() != null && !req.getRidingStampUrl().trim().isEmpty()) {
                        String ridingUrl = req.getRidingStampUrl().trim();
                        String localImg = downloadImageTo(workDir2, ridingUrl);
                        if (localImg != null) {
                            String out = new File(workDir2, "compose_" + ts + "_riding_1.pdf").getAbsolutePath();
                            logger.info("使用骑缝章图片(用户URL): {}", new java.io.File(localImg).getName());
                            // 创建骑缝章配置，设置自定义尺寸
                            RidingStampUtil.RidingStampConfig ridingConfig = new RidingStampUtil.RidingStampConfig(localImg);
                            // 设置骑缝章尺寸（如果有提供）
                            if (req.getRidingStampWidth() != null && req.getRidingStampWidth() > 0) {
                                ridingConfig.setStampWidth(req.getRidingStampWidth());
                            }
                            if (req.getRidingStampHeight() != null && req.getRidingStampHeight() > 0) {
                                ridingConfig.setStampHeight(req.getRidingStampHeight());
                            }
                            RidingStampUtil.addRidingStamp(currentPdf, out, ridingConfig);
                            currentPdf = out;
                            ridingRelPath = getRelativePath(new File(out));
                        } else {
                            logger.warn("下载骑缝章图片失败: {}", ridingUrl);
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
            return ApiResponse.success(resp);
        } catch (Exception e) {
            logger.error("===== 合成失败，详细错误信息 =====", e);
            logger.error("错误消息: {}", e.getMessage());
            logger.error("错误类型: {}", e.getClass().getName());
            if (e.getCause() != null) {
                logger.error("根本原因: {}", e.getCause().getMessage());
            }
            logger.error("===== 错误堆栈跟踪 =====");
            e.printStackTrace();
            return ApiResponse.<ComposeResponse>serverError().errorDetail("合成失败: " + e.getMessage());
        }
    }

    @PostMapping(value = "/compose-with-stamp", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<ComposeWithStampResponse> composeWithStamp(@RequestBody ComposeRequest req) {
        try {
            if (req == null || req.getTemplateFileId() == null || req.getTemplateFileId().trim().isEmpty()) {
                return ApiResponse.paramError("templateFileId 不能为空");
            }
            if (req.getValues() == null || req.getValues().isEmpty()) {
                return ApiResponse.paramError("values 不能为空");
            }

            // 1. 合成DOCX
            String templatePath = fileInfoService != null ? fileInfoService.getFileDiskPath(req.getTemplateFileId()) : null;
            if (templatePath == null || templatePath.trim().isEmpty()) {
                return ApiResponse.paramError("无法获取模板文件路径");
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
                return ApiResponse.<ComposeWithStampResponse>serverError().errorDetail("DOCX转PDF失败");
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
            
            return ApiResponse.success(resp);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.<ComposeWithStampResponse>serverError().errorDetail("合成或盖章失败: " + e.getMessage());
        }
    }

    private void ensureSealMarker(String tag,
                                   String rawValue,
                                   String ts,
                                   java.util.Map<String, String> mergeValues,
                                   java.util.Map<String, String> tagToMarker,
                                   java.util.LinkedHashSet<String> sealKeywordPrefixesSet) {
        if (tag == null || !tag.startsWith("tagElement") || tagToMarker.containsKey(tag)) {
            return;
        }
        String keyFull = tag.substring("tagElement".length());
        String[] parts = keyFull.split("_");
        if (parts.length < 3) {
            return;
        }
        StringBuilder codePrefixBuilder = new StringBuilder();
        for (int i = 0; i < parts.length - 2; i++) {
            if (i > 0) {
                codePrefixBuilder.append('_');
            }
            codePrefixBuilder.append(parts[i]);
        }
        String codePrefix = codePrefixBuilder.toString();
        if (!codePrefix.toLowerCase().contains("seal")) {
            return;
        }
        String markerPrefix = "SEAL_" + codePrefix.toUpperCase() + "_";
        String marker = markerPrefix + ts;
        sealKeywordPrefixesSet.add(marker);
        tagToMarker.put(tag, marker);
        String markerHtml = "<font style=\"color: white;\">" + marker + "</font>";
        String visiblePart = rawValue == null ? "" : rawValue;
        if (!visiblePart.isEmpty()) {
            visiblePart = "<font style=\"color: white;\">" + visiblePart + "</font>";
        }
        mergeValues.put(tag, visiblePart + markerHtml);
    }


    private File ensureWorkDir() {
        try {
            java.nio.file.Path root = java.nio.file.Paths.get(uploadRootPath).toAbsolutePath().normalize();
            java.nio.file.Files.createDirectories(root);
            String yearMonthPath = FileStorageUtils.getYearMonthPath();
            java.nio.file.Path work = root.resolve("compose").resolve(yearMonthPath);
            java.nio.file.Files.createDirectories(work);
            return work.toFile();
        } catch (Exception e) {
            String yearMonthPath = FileStorageUtils.getYearMonthPath();
            File fallback = new File(System.getProperty("java.io.tmpdir"), "uploads/compose/" + yearMonthPath);
            fallback.mkdirs();
            return fallback;
        }
    }

    private FileInfo registerComposedFile(File outDocx) {
        if (fileInfoService == null) return null;
        String originalName = outDocx.getName();
        String ext = "docx";
        long size = outDocx.length();
        // 指定模块为 "compose"，使用相对路径存储
        return fileInfoService.registerFile(originalName, ext, outDocx.getAbsolutePath(), size, "compose");
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ComposeRequest {
        /** 模板文件ID（来自文件服务，向后兼容） */
        @JsonProperty("templateFileId")
        private String templateFileId;
        /** 模板编号（推荐使用，支持多版本，优先获取已发布版本） */
        @JsonProperty("templateCode")
        private String templateCode;
        /** tag->value 映射 */
        @JsonProperty("values")
        private Map<String, String> values;
        /** 用户提供的图片URL映射：key为SDT的tag，normal为普通章URL */
        @JsonProperty("stampImageUrls")
        private Map<String, StampImagePair> stampImageUrls;
        /** 需要合并的额外PDF文件URL列表（会在合成后合并，合并后再盖骑缝章） */
        @JsonProperty("extraFiles")
        private List<String> extraFiles;
        /** 骑缝章图片URL（可选，如果提供则会在合并后的PDF上盖骑缝章） */
        @JsonProperty("ridingStampUrl")
        private String ridingStampUrl;
        /** 骑缝章宽度（可选，单位：点，默认80） */
        @JsonProperty("ridingStampWidth")
        private Float ridingStampWidth;
        /** 骑缝章高度（可选，单位：点，默认80） */
        @JsonProperty("ridingStampHeight")
        private Float ridingStampHeight;
        public String getTemplateFileId() { return templateFileId; }
        public void setTemplateFileId(String templateFileId) { this.templateFileId = templateFileId; }
        public String getTemplateCode() { return templateCode; }
        public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }
        public Map<String, String> getValues() { return values; }
        public void setValues(Map<String, String> values) { this.values = values; }
        public Map<String, StampImagePair> getStampImageUrls() { return stampImageUrls; }
        public void setStampImageUrls(Map<String, StampImagePair> stampImageUrls) { this.stampImageUrls = stampImageUrls; }
        public List<String> getExtraFiles() { return extraFiles; }
        public void setExtraFiles(List<String> extraFiles) { this.extraFiles = extraFiles; }
        public String getRidingStampUrl() { return ridingStampUrl; }
        public void setRidingStampUrl(String ridingStampUrl) { this.ridingStampUrl = ridingStampUrl; }
        public Float getRidingStampWidth() { return ridingStampWidth; }
        public void setRidingStampWidth(Float ridingStampWidth) { this.ridingStampWidth = ridingStampWidth; }
        public Float getRidingStampHeight() { return ridingStampHeight; }
        public void setRidingStampHeight(Float ridingStampHeight) { this.ridingStampHeight = ridingStampHeight; }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class StampImagePair {
            @JsonProperty("normal")
            private String normal;
            /** 印章宽度（可选，单位：点，默认80） */
            @JsonProperty("width")
            private Float width;
            /** 印章高度（可选，单位：点，默认80） */
            @JsonProperty("height")
            private Float height;
            public String getNormal() { return normal; }
            public void setNormal(String normal) { this.normal = normal; }
            public Float getWidth() { return width; }
            public void setWidth(Float width) { this.width = width; }
            public Float getHeight() { return height; }
            public void setHeight(Float height) { this.height = height; }
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
    
    /**
     * 下载PDF文件到指定目录
     */
    private String downloadPdfTo(File workDir, String url) {
        try {
            Request req = new Request.Builder().url(url).build();
            try (Response resp = HTTP.newCall(req).execute()) {
                if (!resp.isSuccessful()) {
                    logger.warn("下载PDF失败: {} -> status {}", url, resp.code());
                    return null;
                }
                ResponseBody body = resp.body();
                if (body == null) return null;
                byte[] bytes = body.bytes();
                File out = File.createTempFile("extra_pdf_", ".pdf", workDir);
                java.nio.file.Files.write(out.toPath(), bytes);
                logger.info("成功下载PDF文件: {} -> {}", url, out.getAbsolutePath());
                return out.getAbsolutePath();
            }
        } catch (Exception ex) {
            logger.warn("下载PDF异常: {} -> {}", url, ex.getMessage());
            return null;
        }
    }
    
    /**
     * 合并PDF文件
     * @param mainPdfPath 主PDF文件路径（合成的合同PDF）
     * @param extraPdfPaths 需要合并的额外PDF文件路径列表
     * @param outputPath 输出文件路径
     */
    private void mergePdfFiles(String mainPdfPath, List<String> extraPdfPaths, String outputPath) {
        try {
            logger.info("开始合并PDF文件，主文件: {}, 额外文件数: {}", mainPdfPath, extraPdfPaths.size());
            
            // 使用PDFBox合并PDF
            org.apache.pdfbox.pdmodel.PDDocument mergedDoc = new org.apache.pdfbox.pdmodel.PDDocument();
            
            // 1. 添加主PDF的所有页面
            try (org.apache.pdfbox.pdmodel.PDDocument mainDoc = org.apache.pdfbox.pdmodel.PDDocument.load(new File(mainPdfPath))) {
                org.apache.pdfbox.multipdf.PDFMergerUtility merger = new org.apache.pdfbox.multipdf.PDFMergerUtility();
                merger.appendDocument(mergedDoc, mainDoc);
                logger.info("已添加主PDF，共{}页", mainDoc.getNumberOfPages());
            }
            
            // 2. 添加所有额外PDF的所有页面
            for (String extraPdfPath : extraPdfPaths) {
                try (org.apache.pdfbox.pdmodel.PDDocument extraDoc = org.apache.pdfbox.pdmodel.PDDocument.load(new File(extraPdfPath))) {
                    org.apache.pdfbox.multipdf.PDFMergerUtility merger = new org.apache.pdfbox.multipdf.PDFMergerUtility();
                    merger.appendDocument(mergedDoc, extraDoc);
                    logger.info("已添加额外PDF: {}，共{}页", new File(extraPdfPath).getName(), extraDoc.getNumberOfPages());
                } catch (Exception e) {
                    logger.warn("添加额外PDF失败: {}，跳过该文件", extraPdfPath, e);
                }
            }
            
            // 3. 保存合并后的PDF
            int totalPages = mergedDoc.getNumberOfPages();
            mergedDoc.save(new File(outputPath));
            mergedDoc.close();
            
            logger.info("PDF合并完成，输出文件: {}，总页数: {}", outputPath, totalPages);
        } catch (Exception e) {
            logger.error("合并PDF文件失败", e);
            throw new RuntimeException("合并PDF文件失败: " + e.getMessage(), e);
        }
    }
}


