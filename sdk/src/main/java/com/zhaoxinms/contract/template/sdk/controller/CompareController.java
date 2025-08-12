package com.zhaoxinms.contract.template.sdk.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhaoxinms.contract.template.sdk.entity.CompareRecord;
import com.zhaoxinms.contract.template.sdk.service.CompareRecordService;
import com.zhaoxinms.contract.tools.common.Result;
import com.zhaoxinms.contract.tools.compare.CompareOptions;
import com.zhaoxinms.contract.tools.compare.PDFComparsionHelper;
import com.zhaoxinms.contract.tools.config.ZxcmConfig;

import cn.hutool.core.util.StrUtil;

@RestController
@RequestMapping("/api/compare")
public class CompareController {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    @Value("${file.upload.root-path:./uploads}")
    private String uploadRootPath;

    @Autowired(required = false)
    private com.zhaoxinms.contract.tools.onlyoffice.ChangeFileToPDFService changeFileToPDFService;
    @Autowired(required = false)
    private ZxcmConfig zxcmConfig;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CompareRecordService compareRecordService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, Object>> uploadAndCompare(
            @RequestPart("oldFile") MultipartFile oldFile,
            @RequestPart("newFile") MultipartFile newFile,
            @RequestParam(value = "ignoreHeaderFooter", required = false, defaultValue = "true") boolean ignoreHeaderFooter,
            @RequestParam(value = "headerHeightMm", required = false, defaultValue = "20") float headerHeightMm,
            @RequestParam(value = "footerHeightMm", required = false, defaultValue = "20") float footerHeightMm,
            @RequestParam(value = "ignoreCase", required = false, defaultValue = "true") boolean ignoreCase,
            @RequestParam(value = "ignoredSymbols", required = false, defaultValue = "_＿") String ignoredSymbols,
            HttpServletRequest request
    ) {
        try {
            String ts = TS.format(LocalDateTime.now());
            File workDir = ensureWorkDir();
            // 保存原始文件（保留后缀）
            String oldExt = getExt(oldFile.getOriginalFilename());
            String newExt = getExt(newFile.getOriginalFilename());
            File oldSrc = new File(workDir, "old_" + ts + (oldExt.isEmpty() ? "" : ("." + oldExt)));
            File newSrc = new File(workDir, "new_" + ts + (newExt.isEmpty() ? "" : ("." + newExt)));
            ensureParent(oldSrc);
            ensureParent(newSrc);
            oldFile.transferTo(oldSrc);
            newFile.transferTo(newSrc);

            // 转为PDF
            File oldPdf = new File(workDir, "old_" + ts + ".pdf");
            File newPdf = new File(workDir, "new_" + ts + ".pdf");
            ensurePdf(request, oldSrc, oldPdf);
            ensurePdf(request, newSrc, newPdf);

            // 生成对比结果PDF
            File outOld = new File(workDir, "out_old_" + ts + ".pdf");
            File outNew = new File(workDir, "out_new_" + ts + ".pdf");
            ensureParent(outOld);
            ensureParent(outNew);
            CompareOptions options = new CompareOptions()
                .setIgnoreHeaderFooter(ignoreHeaderFooter)
                .setHeaderHeightMm(headerHeightMm)
                .setFooterHeightMm(footerHeightMm)
                .setIgnoreCase(ignoreCase)
                // compare阶段不删符号
                .setIgnoredSymbols(ignoredSymbols);
            List<com.zhaoxinms.contract.tools.compare.result.CompareResult> results =
                PDFComparsionHelper.compare(oldPdf.getAbsolutePath(), newPdf.getAbsolutePath(), outOld.getAbsolutePath(), outNew.getAbsolutePath(), options);

            // 保存结果到数据库
            CompareRecord rec = new CompareRecord();
            rec.setBizId(ts);
            rec.setOldPdfName(outOld.getName());
            rec.setNewPdfName(outNew.getName());
            rec.setResultsJson(objectMapper.writeValueAsString(results));
            rec.setCreatedAt(java.time.LocalDateTime.now());
            compareRecordService.save(rec);

            Map<String, Object> data = new HashMap<>();
            data.put("id", ts);
            data.putAll(buildResponseUrls(request, outOld, outNew));
            data.put("results", results);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("合同比对失败: " + e.getMessage());
        }
    }

    @PostMapping("/byUrls")
    public Result<Map<String, Object>> compareByUrls(@RequestParam("oldUrl") String oldUrl,
                                                     @RequestParam("newUrl") String newUrl,
                                                      @RequestParam(value = "ignoreHeaderFooter", required = false, defaultValue = "true") boolean ignoreHeaderFooter,
                                                      @RequestParam(value = "headerHeightMm", required = false, defaultValue = "20") float headerHeightMm,
                                                      @RequestParam(value = "footerHeightMm", required = false, defaultValue = "20") float footerHeightMm,
                                                      @RequestParam(value = "ignoreCase", required = false, defaultValue = "true") boolean ignoreCase,
                                                      @RequestParam(value = "ignoredSymbols", required = false, defaultValue = "_＿") String ignoredSymbols,
                                                     HttpServletRequest request) {
        if (StrUtil.isBlank(oldUrl) || StrUtil.isBlank(newUrl)) {
            return Result.error("参数错误：oldUrl/newUrl 不能为空");
        }
        try {
            String ts = TS.format(LocalDateTime.now());
            File workDir = ensureWorkDir();
            // 判断URL后缀并转PDF
            File oldPdf = new File(workDir, "old_" + ts + ".pdf");
            File newPdf = new File(workDir, "new_" + ts + ".pdf");
            ensureParent(oldPdf);
            ensureParent(newPdf);
            if (isPdf(oldUrl)) {
                downloadTo(oldUrl, oldPdf);
            } else {
                convertUrlToPdf(oldUrl, oldPdf);
            }
            if (isPdf(newUrl)) {
                downloadTo(newUrl, newPdf);
            } else {
                convertUrlToPdf(newUrl, newPdf);
            }

            File outOld = new File(workDir, "out_old_" + ts + ".pdf");
            File outNew = new File(workDir, "out_new_" + ts + ".pdf");
            ensureParent(outOld);
            ensureParent(outNew);
            CompareOptions options = new CompareOptions()
                .setIgnoreHeaderFooter(ignoreHeaderFooter)
                .setHeaderHeightMm(headerHeightMm)
                .setFooterHeightMm(footerHeightMm)
                .setIgnoreCase(ignoreCase)
                .setIgnoredSymbols(ignoredSymbols);
            List<com.zhaoxinms.contract.tools.compare.result.CompareResult> results =
                PDFComparsionHelper.compare(oldPdf.getAbsolutePath(), newPdf.getAbsolutePath(), outOld.getAbsolutePath(), outNew.getAbsolutePath(), options);

            // 保存结果
            CompareRecord rec = new CompareRecord();
            rec.setBizId(ts);
            rec.setOldPdfName(outOld.getName());
            rec.setNewPdfName(outNew.getName());
            rec.setResultsJson(objectMapper.writeValueAsString(results));
            rec.setCreatedAt(java.time.LocalDateTime.now());
            compareRecordService.save(rec);

            Map<String, Object> data = new HashMap<>();
            data.put("id", ts);
            data.putAll(buildResponseUrls(request, outOld, outNew));
            data.put("results", results);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("合同比对失败: " + e.getMessage());
        }
    }

    private File ensureWorkDir() {
        try {
            Path root = Paths.get(uploadRootPath).toAbsolutePath().normalize();
            Files.createDirectories(root);
            Path work = root.resolve("compare");
            Files.createDirectories(work);
            return work.toFile();
        } catch (Exception e) {
            File fallback = new File(System.getProperty("java.io.tmpdir"), "uploads/compare");
            fallback.mkdirs();
            return fallback;
        }
    }

    private void downloadTo(String url, File target) throws Exception {
        ensureParent(target);
        try (InputStream in = new URL(url).openStream(); FileOutputStream out = new FileOutputStream(target)) {
            StreamUtils.copy(in, out);
        }
    }

    private void ensurePdf(HttpServletRequest request, File src, File destPdf) throws Exception {
        String ext = getExt(src.getName());
        if ("pdf".equalsIgnoreCase(ext)) {
            // 直接复制/重命名为目标PDF
            try (java.io.InputStream in = new java.io.FileInputStream(src);
                 java.io.FileOutputStream out = new java.io.FileOutputStream(destPdf)) {
                StreamUtils.copy(in, out);
            }
            return;
        }
        convertLocalToPdf(request, src, destPdf);
    }

    private void convertLocalToPdf(HttpServletRequest request, File src, File destPdf) throws Exception {
        if (changeFileToPDFService == null) {
            throw new IllegalStateException("未启用OnlyOffice转换服务，无法将非PDF文件转换为PDF");
        }
        // 用 onlyoffice.callback.url 来推导可被 OnlyOffice 访问的对外地址
        String callbackUrl = (zxcmConfig != null && zxcmConfig.getOnlyOffice() != null && zxcmConfig.getOnlyOffice().getCallback() != null)
            ? zxcmConfig.getOnlyOffice().getCallback().getUrl() : null;
        String origin;
        String ctxPath;
        if (callbackUrl != null && !callbackUrl.isEmpty()) {
            java.net.URI cb = java.net.URI.create(callbackUrl);
            origin = cb.getScheme() + "://" + cb.getHost() + (cb.getPort() > 0 ? ":" + cb.getPort() : "");
            String p = cb.getPath() == null ? "" : cb.getPath();
            int idx = p.indexOf("/onlyoffice/");
            ctxPath = idx >= 0 ? p.substring(0, idx) : "";
        } else {
            origin = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
            ctxPath = request.getContextPath();
        }
        String srcUrl = origin + (ctxPath == null ? "" : ctxPath) + "/compare/source?name=" + urlEncode(src.getName());
        String path = changeFileToPDFService.covertToPdf(srcUrl, destPdf.getAbsolutePath());
        if (path == null) {
            throw new IllegalStateException("文件转换为PDF失败: " + src.getName());
        }
    }

    private void ensureParent(File file) throws Exception {
        File p = file.getParentFile();
        if (p != null && !p.exists()) {
            if (!p.mkdirs() && !p.exists()) {
                throw new Exception("无法创建目录: " + p.getAbsolutePath());
            }
        }
    }

    private void convertUrlToPdf(String sourceUrl, File destPdf) throws Exception {
        if (changeFileToPDFService == null) {
            // 退化：直接下载（若非PDF将无法使用）
            downloadTo(sourceUrl, destPdf);
            return;
        }
        String path = changeFileToPDFService.covertToPdf(sourceUrl, destPdf.getAbsolutePath());
        if (path == null) {
            throw new IllegalStateException("URL 转PDF失败: " + sourceUrl);
        }
    }

    private boolean isPdf(String urlOrName) {
        String ext = getExt(urlOrName);
        return "pdf".equalsIgnoreCase(ext);
    }

    private String getExt(String name) {
        if (name == null) return "";
        String n = name;
        int q = n.indexOf('?');
        if (q >= 0) n = n.substring(0, q);
        int s = n.lastIndexOf('/');
        if (s >= 0) n = n.substring(s + 1);
        int dot = n.lastIndexOf('.');
        if (dot < 0) return "";
        return n.substring(dot + 1);
    }

    private Map<String, String> buildResponseUrls(HttpServletRequest request, File outOld, File outNew) {
        // 返回无查询串的路径，避免 viewer 对 '?' 编码为 %3F 导致 404
        String contextPath = request.getContextPath() == null ? "" : request.getContextPath();
        String pathPrefix = contextPath + "/api/compare";
        Map<String, String> data = new HashMap<>();
        data.put("oldPdf", pathPrefix + "/file/" + outOld.getName());
        data.put("newPdf", pathPrefix + "/file/" + outNew.getName());
        return data;
    }

    @GetMapping("/download")
    public void download(@RequestParam("name") String name, javax.servlet.http.HttpServletResponse response) {
        File workDir = ensureWorkDir();
        File target = new File(workDir, name);
        if (!target.exists() || target.isDirectory()) {
            response.setStatus(404);
            return;
        }
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "inline; filename=\"" + name + "\"");
        response.setHeader("Accept-Ranges", "bytes");
        long fileLength = target.length();

        String range = null;
        try {
            org.springframework.web.context.request.RequestAttributes ra = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (ra instanceof org.springframework.web.context.request.ServletRequestAttributes) {
                javax.servlet.http.HttpServletRequest req = ((org.springframework.web.context.request.ServletRequestAttributes) ra).getRequest();
                range = req.getHeader("Range");
            }
        } catch (Exception ignore) { }

        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(target, "r");
             java.io.OutputStream out = response.getOutputStream()) {
            long start = 0;
            long end = fileLength - 1;
            boolean isPartial = false;
            if (range != null && range.startsWith("bytes=")) {
                // e.g. bytes=0-1023 or bytes=1024-
                String spec = range.substring("bytes=".length());
                String[] parts = spec.split(","); // 不支持多段，取第一段
                String first = parts[0].trim();
                String[] se = first.split("-");
                try {
                    if (!se[0].isEmpty()) start = Long.parseLong(se[0]);
                    if (se.length > 1 && !se[1].isEmpty()) end = Long.parseLong(se[1]);
                    if (se.length > 1 && se[1].isEmpty()) end = fileLength - 1;
                    if (start < 0) start = 0;
                    if (end >= fileLength) end = fileLength - 1;
                    if (start <= end) {
                        isPartial = true;
                    }
                } catch (NumberFormatException ignore) { /* 回退整文件 */ }
            }

            long contentLength = end - start + 1;
            if (isPartial) {
                response.setStatus(206);
                response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
                response.setHeader("Content-Length", String.valueOf(contentLength));
            } else {
                response.setHeader("Content-Length", String.valueOf(fileLength));
                start = 0;
                end = fileLength - 1;
            }

            raf.seek(start);
            byte[] buffer = new byte[8192];
            long remaining = end - start + 1;
            while (remaining > 0) {
                int read = raf.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (read == -1) break;
                out.write(buffer, 0, read);
                remaining -= read;
            }
            out.flush();
        } catch (Exception e) {
            response.setStatus(500);
        }
    }

    // 供 OnlyOffice 文档服务访问本地源文件进行转换
    @GetMapping("/source")
    public void source(@RequestParam("name") String name, javax.servlet.http.HttpServletResponse response) {
        File workDir = ensureWorkDir();
        File target = new File(workDir, name);
        if (!target.exists() || target.isDirectory()) {
            response.setStatus(404);
            return;
        }
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + name + "\"");
        try (java.io.InputStream in = new java.io.FileInputStream(target)) {
            StreamUtils.copy(in, response.getOutputStream());
        } catch (Exception e) {
            response.setStatus(500);
        }
    }

    // 供前端 viewer 以无查询串的路径加载 PDF，避免 %3F 引发的 404
    @GetMapping("/file/{name:.+}")
    public void file(@PathVariable("name") String name, javax.servlet.http.HttpServletResponse response) {
        download(name, response);
    }

    private String urlEncode(String s) {
        try {
            return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return s;
        }
    }

    @GetMapping("/result/{id}")
    public Result<Map<String, Object>> getResult(@PathVariable("id") String id, HttpServletRequest request) {
        try {
            CompareRecord rec = compareRecordService.getByBizId(id);
            if (rec == null) return Result.error("结果不存在或已过期");
            File dir = ensureWorkDir();
            String oldName = rec.getOldPdfName();
            String newName = rec.getNewPdfName();
            Map<String, Object> data = new HashMap<>();
            data.put("id", id);
            data.putAll(buildResponseUrls(request, new File(dir, oldName), new File(dir, newName)));
            data.put("results", objectMapper.readValue(rec.getResultsJson(), new com.fasterxml.jackson.core.type.TypeReference<java.util.List<com.zhaoxinms.contract.tools.compare.result.CompareResult>>(){}));
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("读取结果失败");
        }
    }
}


