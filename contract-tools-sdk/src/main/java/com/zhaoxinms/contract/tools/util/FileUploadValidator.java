package com.zhaoxinms.contract.tools.util;

import com.zhaoxinms.contract.tools.config.DemoModeConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 文件上传验证工具
 * 
 * @author 山西肇新科技有限公司
 */
@Slf4j
@Component
public class FileUploadValidator {

    @Autowired
    private DemoModeConfig demoModeConfig;

    /**
     * 验证结果
     */
    public static class ValidationResult {
        private boolean valid;
        private String errorMessage;

        public static ValidationResult success() {
            ValidationResult result = new ValidationResult();
            result.valid = true;
            return result;
        }

        public static ValidationResult failure(String errorMessage) {
            ValidationResult result = new ValidationResult();
            result.valid = false;
            result.errorMessage = errorMessage;
            return result;
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * 验证PDF文件上传
     * 
     * @param file 上传的文件
     * @return 验证结果
     */
    public ValidationResult validatePdfFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ValidationResult.failure("文件不能为空");
        }

        // 如果未启用演示模式，直接通过验证（不限制）
        if (!demoModeConfig.isEnabled()) {
            log.debug("[文件验证通过] 演示模式未启用，不限制文件上传");
            return ValidationResult.success();
        }

        // 获取文件大小（字节）
        long fileSizeBytes = file.getSize();
        double fileSizeMb = fileSizeBytes / (1024.0 * 1024.0);

        // 检查文件大小
        int maxFileSizeMb = demoModeConfig.getMaxFileSizeMb();
        if (fileSizeMb > maxFileSizeMb) {
            String errorMsg = String.format("演示模式最大允许上传 %dMB 的文件，当前文件大小为 %.2fMB",
                    maxFileSizeMb, fileSizeMb);
            log.warn("[文件上传限制] {}", errorMsg);
            return ValidationResult.failure(errorMsg);
        }

        // 检查PDF页数
        int maxPages = demoModeConfig.getMaxPages();
        try {
            int pageCount = getPdfPageCount(file);
            if (pageCount > maxPages) {
                String errorMsg = String.format("演示模式最大允许 %d 页的PDF文件，当前文件有 %d 页",
                        maxPages, pageCount);
                log.warn("[文件上传限制] {}", errorMsg);
                return ValidationResult.failure(errorMsg);
            }
            log.debug("[文件验证通过] 演示模式 - 文件大小: {:.2f}MB, 页数: {}", fileSizeMb, pageCount);
        } catch (IOException e) {
            log.error("[PDF页数检查失败] 文件: {}, 错误: {}", file.getOriginalFilename(), e.getMessage());
            return ValidationResult.failure("PDF文件格式错误，无法读取页数");
        }

        return ValidationResult.success();
    }

    /**
     * 获取PDF文件页数
     * 
     * @param file PDF文件
     * @return 页数
     * @throws IOException 读取PDF失败
     */
    private int getPdfPageCount(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            return document.getNumberOfPages();
        }
    }

    /**
     * 获取当前模式描述
     */
    public String getCurrentModeDescription() {
        if (demoModeConfig.isEnabled()) {
            return String.format("演示模式（最大 %dMB，最多 %d 页）",
                    demoModeConfig.getMaxFileSizeMb(),
                    demoModeConfig.getMaxPages());
        } else {
            return "正式模式（不限制文件大小和页数）";
        }
    }
}

