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
import org.springframework.web.bind.annotation.DeleteMapping;
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
import com.zhaoxinms.contract.tools.onlyoffice.exception.OnlyOfficeServiceUnavailableException;

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
    
    // 注入OCR比对服务
    @Autowired(required = false)
    private com.zhaoxinms.contract.tools.ocrcompare.compare.OCRCompareService ocrCompareService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, Object>> uploadAndCompare(
            @RequestPart("oldFile") MultipartFile oldFile,
            @RequestPart("newFile") MultipartFile newFile,
            @RequestParam(value = "ignoreHeaderFooter", required = false, defaultValue = "true") boolean ignoreHeaderFooter,
            @RequestParam(value = "headerHeightMm", required = false, defaultValue = "20") float headerHeightMm,
            @RequestParam(value = "footerHeightMm", required = false, defaultValue = "20") float footerHeightMm,
            @RequestParam(value = "ignoreCase", required = false, defaultValue = "true") boolean ignoreCase,
            @RequestParam(value = "ignoredSymbols", required = false, defaultValue = "_＿") String ignoredSymbols,
            @RequestParam(value = "useOCR", required = false, defaultValue = "false") boolean useOCR,
            @RequestParam(value = "ignoreSpaces", required = false, defaultValue = "false") boolean ignoreSpaces,
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
            
            // 保存文件并验证
            try {
                oldFile.transferTo(oldSrc);
                newFile.transferTo(newSrc);
                
                // 等待文件系统同步
                Thread.sleep(100);
                
            } catch (Exception e) {
                throw new IllegalStateException("文件保存失败: " + e.getMessage(), e);
            }
            
            // 验证文件保存是否成功
            System.out.println("文件保存验证:");
            System.out.println("  旧文件: " + oldSrc.getAbsolutePath() + " (大小: " + oldSrc.length() + " bytes)");
            System.out.println("  新文件: " + newSrc.getAbsolutePath() + " (大小: " + newSrc.length() + " bytes)");
            
            if (!oldSrc.exists()) {
                throw new IllegalStateException("旧文件保存失败，文件不存在: " + oldSrc.getAbsolutePath());
            }
            if (!newSrc.exists()) {
                throw new IllegalStateException("新文件保存失败，文件不存在: " + newSrc.getAbsolutePath());
            }
            
            if (oldSrc.length() == 0) {
                throw new IllegalStateException("旧文件保存失败，文件大小为0: " + oldSrc.getAbsolutePath());
            }
            if (newSrc.length() == 0) {
                throw new IllegalStateException("新文件保存失败，文件大小为0: " + newSrc.getAbsolutePath());
            }
            
            // 验证文件权限
            if (!oldSrc.canRead()) {
                throw new IllegalStateException("旧文件无读权限: " + oldSrc.getAbsolutePath());
            }
            if (!newSrc.canRead()) {
                throw new IllegalStateException("新文件无读权限: " + newSrc.getAbsolutePath());
            }

            // 转为PDF
            File oldPdf = new File(workDir, "old_" + ts + ".pdf");
            File newPdf = new File(workDir, "new_" + ts + ".pdf");
            
            System.out.println("开始PDF转换:");
            System.out.println("  旧文件 -> PDF: " + oldSrc.getName() + " -> " + oldPdf.getName());
            ensurePdf(request, oldSrc, oldPdf);
            System.out.println("  新文件 -> PDF: " + newSrc.getName() + " -> " + newPdf.getName());
            ensurePdf(request, newSrc, newPdf);
            
            // 验证PDF转换结果
            System.out.println("PDF转换结果验证:");
            System.out.println("  旧PDF: " + oldPdf.getAbsolutePath() + " (大小: " + oldPdf.length() + " bytes)");
            System.out.println("  新PDF: " + newPdf.getAbsolutePath() + " (大小: " + newPdf.length() + " bytes)");
            
            if (oldPdf.length() == 0 || newPdf.length() == 0) {
                throw new IllegalStateException("PDF转换失败，生成的PDF文件为空");
            }

            // 如果使用OCR比对
            if (useOCR) {
                try {
                    // 检查OCR比对服务是否可用
                    if (ocrCompareService == null) {
                        return Result.error("OCR比对服务未配置，请检查服务依赖");
                    }
                    
                    // 创建OCR比对选项
                    com.zhaoxinms.contract.tools.ocrcompare.compare.OCRCompareOptions ocrOptions = 
                        new com.zhaoxinms.contract.tools.ocrcompare.compare.OCRCompareOptions();
                    ocrOptions.setIgnoreHeaderFooter(ignoreHeaderFooter);
                    ocrOptions.setIgnoreCase(ignoreCase);
                    ocrOptions.setIgnoreSpaces(ignoreSpaces);
                    
                                         // 重要：OCR比对服务需要PDF文件路径，而不是原始文件路径
                     // 我们已经转换了文件为PDF，现在传递PDF文件路径
                     String oldPdfPath = oldPdf.getAbsolutePath();
                     String newPdfPath = newPdf.getAbsolutePath();
                     
                     System.out.println("提交OCR比对任务:");
                     System.out.println("  旧PDF: " + oldPdfPath);
                     System.out.println("  新PDF: " + newPdfPath);
                     
                     // 调用OCR比对服务，传递PDF文件路径
                     String taskId = ocrCompareService.submitCompareTaskWithPaths(oldPdfPath, newPdfPath, ocrOptions);
                    
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", taskId);
                    data.put("message", "OCR比对任务已提交，请等待处理完成");
                    data.put("useOCR", true);
                    data.put("taskType", "OCR_COMPARE");
                    data.put("status", "PROCESSING");
                    
                    return Result.success(data);
                    
                } catch (Exception e) {
                    return Result.error("OCR比对失败: " + e.getMessage());
                }
            }

            // 普通比对逻辑
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
            data.put("useOCR", false);
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
            try {
                // 确保目标目录存在
                ensureParent(destPdf);
                
                // 验证源文件
                if (!src.exists()) {
                    throw new IllegalStateException("源文件不存在: " + src.getAbsolutePath());
                }
                if (src.length() == 0) {
                    throw new IllegalStateException("源文件为空: " + src.getAbsolutePath());
                }
                
                // 如果源文件和目标文件是同一个文件，直接返回
                if (src.getAbsolutePath().equals(destPdf.getAbsolutePath())) {
                    System.out.println("源文件和目标文件相同，跳过复制: " + src.getName());
                    return;
                }
                
                System.out.println("开始复制PDF文件: " + src.getName() + " (大小: " + src.length() + " bytes) -> " + destPdf.getName());
                
                // 使用缓冲区复制，确保数据正确传输
                try (java.io.FileInputStream in = new java.io.FileInputStream(src);
                     java.io.FileOutputStream out = new java.io.FileOutputStream(destPdf)) {
                    
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                    out.flush();
                }
                
                // 验证复制后的文件
                if (destPdf.length() == 0) {
                    throw new IllegalStateException("PDF文件复制失败，目标文件为空: " + destPdf.getAbsolutePath());
                }
                
                System.out.println("PDF文件复制成功: " + src.getName() + " -> " + destPdf.getName() + 
                                 " (大小: " + src.length() + " -> " + destPdf.length() + " bytes)");
                                 
            } catch (Exception e) {
                // 如果复制失败，尝试使用转换服务
                System.out.println("PDF文件直接复制失败，尝试使用转换服务: " + e.getMessage());
                
                // 对于PDF文件，如果转换服务不可用，直接抛出错误
                if (changeFileToPDFService == null) {
                    throw new IllegalStateException("PDF文件复制失败且转换服务不可用: " + e.getMessage());
                }
                
                convertLocalToPdf(request, src, destPdf);
            }
            return;
        }
        convertLocalToPdf(request, src, destPdf);
    }

    private void convertLocalToPdf(HttpServletRequest request, File src, File destPdf) throws Exception {
        if (changeFileToPDFService == null) {
            throw new IllegalStateException("未启用OnlyOffice转换服务，无法将非PDF文件转换为PDF");
        }
        
        System.out.println("开始转换文件为PDF: " + src.getName() + " -> " + destPdf.getName());
        
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
        
        System.out.println("调用OnlyOffice转换服务，源文件URL: " + srcUrl);
        
        try {
            String path = changeFileToPDFService.covertToPdf(srcUrl, destPdf.getAbsolutePath());
            if (path == null) {
                throw new IllegalStateException("文件转换为PDF失败: " + src.getName());
            }
            System.out.println("文件转换完成: " + src.getName() + " -> " + destPdf.getName());
        } catch (OnlyOfficeServiceUnavailableException e) {
            throw new IllegalStateException("OnlyOffice服务不可用，无法转换文档格式: " + src.getName() + "。请检查OnlyOffice服务状态或联系管理员。", e);
        }
    }

    private void ensureParent(File file) throws Exception {
        File p = file.getParentFile();
        if (p != null && !p.exists()) {
            System.out.println("创建目录: " + p.getAbsolutePath());
            if (!p.mkdirs()) {
                // 等待一下再检查
                try { Thread.sleep(100); } catch (InterruptedException ignore) {}
                if (!p.exists()) {
                    throw new Exception("无法创建目录: " + p.getAbsolutePath());
                }
            }
            System.out.println("目录创建成功: " + p.getAbsolutePath());
        }
        
        // 验证目录权限
        if (p != null && p.exists()) {
            if (!p.canWrite()) {
                throw new Exception("目录无写权限: " + p.getAbsolutePath());
            }
            if (!p.canRead()) {
                throw new Exception("目录无读权限: " + p.getAbsolutePath());
            }
        }
    }

    private void convertUrlToPdf(String sourceUrl, File destPdf) throws Exception {
        if (changeFileToPDFService == null) {
            // 退化：直接下载（若非PDF将无法使用）
            downloadTo(sourceUrl, destPdf);
            return;
        }
        try {
            String path = changeFileToPDFService.covertToPdf(sourceUrl, destPdf.getAbsolutePath());
            if (path == null) {
                throw new IllegalStateException("URL 转PDF失败: " + sourceUrl);
            }
        } catch (OnlyOfficeServiceUnavailableException e) {
            throw new IllegalStateException("OnlyOffice服务不可用，无法转换URL文档: " + sourceUrl + "。请检查OnlyOffice服务状态或联系管理员。", e);
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

    /**
     * 查询OCR比对任务状态
     */
    @GetMapping("/ocr-task/{taskId}/status")
    public Result<Object> getOCRTaskStatus(@PathVariable String taskId) {
        try {
            if (ocrCompareService == null) {
                return Result.error("OCR比对服务未配置");
            }
            
            Object taskStatus = ocrCompareService.getTaskStatus(taskId);
            if (taskStatus != null) {
                return Result.success(taskStatus);
            } else {
                return Result.error("OCR比对任务不存在");
            }
        } catch (Exception e) {
            return Result.error("查询OCR比对任务状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取OCR比对结果
     */
    @GetMapping("/ocr-task/{taskId}/result")
    public Result<Object> getOCRTaskResult(@PathVariable String taskId) {
        try {
            if (ocrCompareService == null) {
                return Result.error("OCR比对服务未配置");
            }
            
            Object result = ocrCompareService.getCompareResult(taskId);
            if (result != null) {
                return Result.success(result);
            } else {
                return Result.error("OCR比对结果不存在");
            }
        } catch (Exception e) {
            return Result.error("获取OCR比对结果失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有OCR比对任务
     */
    @GetMapping("/ocr-task/list")
    public Result<List<Object>> getAllOCRTasks() {
        try {
            if (ocrCompareService == null) {
                return Result.error("OCR比对服务未配置");
            }
            
            List<?> tasks = ocrCompareService.getAllTasks();
            return Result.success(tasks.stream().map(task -> (Object) task).collect(java.util.stream.Collectors.toList()));
        } catch (Exception e) {
            return Result.error("获取OCR比对任务列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除OCR比对任务
     */
    @DeleteMapping("/ocr-task/{taskId}")
    public Result<Boolean> deleteOCRTask(@PathVariable String taskId) {
        try {
            if (ocrCompareService == null) {
                return Result.error("OCR比对服务未配置");
            }
            
            boolean deleted = ocrCompareService.deleteTask(taskId);
            if (deleted) {
                return Result.success(true);
            } else {
                return Result.error("OCR比对任务不存在");
            }
        } catch (Exception e) {
            return Result.error("删除OCR比对任务失败: " + e.getMessage());
        }
    }
}


