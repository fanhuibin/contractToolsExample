package com.zhaoxinms.contract.tools.merge;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

/**
 * 合同合成控制器：根据模板文件与tag->value替换，生成DOCX并转换/注册PDF，返回fileId
 */
@RestController
@RequestMapping("/api/compose")
public class ComposeController {

    @Value("${file.upload.root-path:./uploads}")
    private String uploadRootPath;

    @Autowired(required = false)
    private FileInfoService fileInfoService;

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
            List<DocContent> contents = new ArrayList<>();
            for (Map.Entry<String, String> e : req.getValues().entrySet()) {
                contents.add(new DocContent(e.getKey(), e.getValue()));
            }

            // 执行合成
            Merge merger = new ContentControlMerge();
            merger.doMerge(templatePath, outDocx.getAbsolutePath(), contents);

            // 可选：转换为PDF；如果前端使用OnlyOffice在线预览DOCX也可直接注册DOCX。
            // 此处直接注册DOCX与PDF一致由OnlyOffice预览渲染。

            // 注册文件到文件服务，返回fileId
            FileInfo registered = registerComposedFile(outDocx);
            if (registered == null) {
                return Result.error("注册合成文件失败");
            }

            ComposeResponse resp = new ComposeResponse();
            resp.setFileId(String.valueOf(registered.getId()));
            return Result.success(resp);
        } catch (Exception e) {
            return Result.error("合成失败: " + e.getMessage());
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
        public String getTemplateFileId() { return templateFileId; }
        public void setTemplateFileId(String templateFileId) { this.templateFileId = templateFileId; }
        public Map<String, String> getValues() { return values; }
        public void setValues(Map<String, String> values) { this.values = values; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ComposeResponse {
        /** 合成后文件ID（用于OnlyOffice或文件下载预览） */
        @JsonProperty("fileId")
        private String fileId;
        public String getFileId() { return fileId; }
        public void setFileId(String fileId) { this.fileId = fileId; }
    }
}


