package com.zhaoxinms.contract.tools.common.ocr;

import java.io.File;

/**
 * OCR服务提供者接口
 * 
 * @author zhaoxin
 * @since 2024-10-09
 */
public interface OCRProvider {

    /**
     * 识别PDF文件
     * 
     * @param pdfFile PDF文件
     * @return OCR识别结果
     * @throws Exception 识别失败
     */
    OCRResult recognizePdf(File pdfFile) throws Exception;

    /**
     * OCR识别结果
     */
    class OCRResult {
        private String content;
        private Object metadata;

        public OCRResult() {
        }

        public OCRResult(String content) {
            this.content = content;
        }

        public OCRResult(String content, Object metadata) {
            this.content = content;
            this.metadata = metadata;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public Object getMetadata() {
            return metadata;
        }

        public void setMetadata(Object metadata) {
            this.metadata = metadata;
        }
    }
}

