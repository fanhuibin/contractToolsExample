package com.zhaoxinms.contract.tools.aicomponent.service;

import java.nio.file.Path;

public interface AutoFulfillmentService {
    String processFile(Path filePath, String prompt, Long templateId);
    String processFile(Path filePath, String prompt, Long templateId, java.util.List<Long> taskTypeIds, java.util.List<String> keywords);
    java.util.List<String> processFileBatch(Path filePath, String prompt, java.util.List<Long> templateIds, java.util.List<Long> taskTypeIds, java.util.List<String> keywords);
    String processFileMerged(Path filePath, String prompt, java.util.List<Long> templateIds, java.util.List<Long> taskTypeIds, java.util.List<String> keywords);
}


