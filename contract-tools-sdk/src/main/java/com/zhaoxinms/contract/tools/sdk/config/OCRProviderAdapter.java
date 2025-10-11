package com.zhaoxinms.contract.tools.sdk.config;

import com.zhaoxinms.contract.tools.ocr.service.OCRService;
import com.zhaoxinms.contract.tools.ocr.service.UnifiedOCRService;
import com.zhaoxinms.contract.tools.ruleextract.ocr.OCRProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * OCR提供者适配器
 * 将SDK的UnifiedOCRService适配为rule-extract需要的OCRProvider接口
 * 
 * @author zhaoxin
 * @since 2024-10-09
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OCRProviderAdapter implements OCRProvider {

    private final UnifiedOCRService unifiedOCRService;

    @Override
    public OCRResult recognizePdf(File pdfFile) throws Exception {
        log.info("OCR适配器：开始识别PDF文件: {}", pdfFile.getName());
        
        try {
            // 调用SDK的OCR服务
            OCRService.OCRResult sdkResult = unifiedOCRService.recognizePdf(pdfFile);
            
            // 转换为rule-extract的OCRResult
            OCRResult result = new OCRResult();
            result.setContent(sdkResult.getContent());
            result.setMetadata(sdkResult);  // 保存完整的SDK结果作为元数据
            
            log.info("OCR适配器：识别完成，文本长度: {}", 
                result.getContent() != null ? result.getContent().length() : 0);
            
            return result;
            
        } catch (Exception e) {
            log.error("OCR适配器：识别失败", e);
            throw e;
        }
    }
}

