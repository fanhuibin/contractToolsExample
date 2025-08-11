package com.zhaoxinms.contract.tools.aicomponent.service;

import java.nio.file.Path;

public interface AutoFulfillmentService {
    String processFile(Path filePath, String prompt, Long templateId);
}


