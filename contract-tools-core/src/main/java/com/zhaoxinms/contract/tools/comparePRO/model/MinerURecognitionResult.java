package com.zhaoxinms.contract.tools.comparePRO.model;

import com.zhaoxinms.contract.tools.comparePRO.util.TextExtractionUtil;

/**
 * MinerU 识别结果包装类
 * 
 * 包含 PageLayout 数组和跨页表格管理器
 * 
 * @author zhaoxin
 * @version 1.0
 * @since 2025-10-13
 */
public class MinerURecognitionResult {
    
    /** PageLayout 数组（与 dots.ocr 格式兼容） */
    public final TextExtractionUtil.PageLayout[] layouts;
    
    /** 跨页表格管理器 */
    public final CrossPageTableManager tableManager;
    
    public MinerURecognitionResult(TextExtractionUtil.PageLayout[] layouts, CrossPageTableManager tableManager) {
        this.layouts = layouts;
        this.tableManager = tableManager;
    }
    
    /**
     * 获取页数
     */
    public int getPageCount() {
        return layouts != null ? layouts.length : 0;
    }
    
    /**
     * 是否有跨页表格
     */
    public boolean hasCrossPageTables() {
        return tableManager != null && tableManager.getTableGroupCount() > 0;
    }
}

