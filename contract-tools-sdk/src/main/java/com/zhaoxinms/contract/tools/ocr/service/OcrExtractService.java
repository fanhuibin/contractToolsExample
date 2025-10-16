package com.zhaoxinms.contract.tools.ocr.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.util.Map;

/**
 * OCR提取服务接口
 */
public interface OcrExtractService {
    
    /**
     * 提取PDF文件
     * 
     * @param file PDF文件
     * @param ignoreHeaderFooter 是否忽略页眉页脚
     * @param headerHeightPercent 页眉高度百分比
     * @param footerHeightPercent 页脚高度百分比
     * @return 任务ID
     */
    String extractPdf(MultipartFile file, Boolean ignoreHeaderFooter, 
                     Double headerHeightPercent, Double footerHeightPercent) throws Exception;
    
    /**
     * 获取任务状态
     * 
     * @param taskId 任务ID
     * @return 任务状态信息
     */
    Map<String, Object> getTaskStatus(String taskId);
    
    /**
     * 获取任务结果
     * 
     * @param taskId 任务ID
     * @return 提取结果
     */
    Map<String, Object> getTaskResult(String taskId) throws Exception;
    
    /**
     * 获取页面图片
     * 
     * @param taskId 任务ID
     * @param pageNum 页码
     * @return 图片文件
     */
    File getPageImage(String taskId, int pageNum);
    
    /**
     * 获取TextBox数据
     * 
     * @param taskId 任务ID
     * @return TextBox数据
     */
    Object getTextBoxes(String taskId) throws Exception;
    
    /**
     * 删除任务
     * 
     * @param taskId 任务ID
     */
    void deleteTask(String taskId);
}

