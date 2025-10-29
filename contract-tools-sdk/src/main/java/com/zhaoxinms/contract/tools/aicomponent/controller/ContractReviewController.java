package com.zhaoxinms.contract.tools.aicomponent.controller;

import com.zhaoxinms.contract.tools.api.common.ApiResponse;
import com.zhaoxinms.contract.tools.config.ZxcmConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.zhaoxinms.contract.tools.common.service.FileInfoService;
import com.zhaoxinms.contract.tools.common.util.FileStorageUtils;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/review")
public class ContractReviewController {

    @Autowired
    private FileInfoService fileInfoService;

    @PostMapping("/upload")
    @ResponseBody
    public ApiResponse<UploadResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ApiResponse.paramError("上传的文件不能为空");
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String fileId = UUID.randomUUID().toString().replace("-", "") + fileExtension;
            String uploadDir = fileInfoService.getFileDiskPath("").substring(0, fileInfoService.getFileDiskPath("").lastIndexOf(File.separator));
            // 使用review子目录存储合同审查上传的文件，添加年月路径
            String yearMonthPath = FileStorageUtils.getYearMonthPath();
            File reviewDir = new File(uploadDir + File.separator + "review" + File.separator + yearMonthPath);
            if (!reviewDir.exists()) {
                reviewDir.mkdirs();
            }
            File dest = new File(reviewDir, fileId);

            if (!dest.getParentFile().exists()) {
                dest.getParentFile().mkdirs();
            }

            file.transferTo(dest);
            log.info("文件已上传至: {}", dest.getAbsolutePath());

            return ApiResponse.success(new UploadResponse(fileId));

        } catch (IOException e) {
            log.error("文件上传失败", e);
            return ApiResponse.<UploadResponse>serverError().errorDetail("文件上传失败: " + e.getMessage());
        }
    }

    @Data
    public static class UploadResponse {
        private String fileId;

        public UploadResponse(String fileId) {
            this.fileId = fileId;
        }
    }
}
