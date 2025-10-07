package com.zhaoxinms.contract.tools.comparePRO.util;

import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

import lombok.extern.slf4j.Slf4j;

/**
 * MinerU坐标转换工具类
 * 
 * MinerU使用PDF坐标系统（左下角原点，Y轴向上），需要转换到图片坐标系统（左上角原点，Y轴向下）
 * 
 * 坐标系统说明：
 * - PDF坐标系：原点在左下角，Y轴向上，单位是点（1/72英寸）
 * - 图片坐标系：原点在左上角，Y轴向下，单位是像素
 * 
 * @author zhaoxin
 * @date 2025-10-07
 */
@Slf4j
public class MinerUCoordinateConverter {
    
    /**
     * 从PDF文件中获取指定页面的尺寸
     * 
     * @param pdfFile PDF文件
     * @param pageIndex 页面索引（从0开始）
     * @return [width, height] PDF页面的原始尺寸（点）
     */
    public static double[] getPdfPageSize(File pdfFile, int pageIndex) throws IOException {
        try (PDDocument document = PDDocument.load(pdfFile)) {
            if (pageIndex >= document.getNumberOfPages()) {
                throw new IllegalArgumentException("页面索引超出范围: " + pageIndex);
            }
            
            PDPage page = document.getPage(pageIndex);
            float width = page.getMediaBox().getWidth();
            float height = page.getMediaBox().getHeight();
            
            return new double[]{width, height};
        }
    }
    
    /**
     * 转换MinerU坐标到图片坐标
     * 
     * 关键发现：MinerU 返回的坐标是基于"宽度和高度都归一化为1000"的正方形坐标系统
     * 
     * MinerU 坐标系统：
     * - 宽度固定为 1000
     * - 高度固定为 1000
     * - 所有 PDF 页面都映射到 1000x1000 的正方形空间
     * 
     * 因此缩放比例是：
     * - scaleX = imageWidth / 1000
     * - scaleY = imageHeight / 1000
     * 
     * @param mineruBbox MinerU的bbox [x1, y1, x2, y2]（基于1000x1000坐标系）
     * @param pdfWidth PDF页面原始宽度（点）- 仅用于日志
     * @param pdfHeight PDF页面原始高度（点）- 仅用于日志
     * @param imageWidth 渲染后图片宽度（像素）
     * @param imageHeight 渲染后图片高度（像素）
     * @return 转换后的bbox [x1, y1, x2, y2]（图片坐标系）
     */
    public static int[] convertToImageCoordinates(
            double[] mineruBbox,
            double pdfWidth,
            double pdfHeight,
            int imageWidth,
            int imageHeight) {
        
        // MinerU 使用 1000x1000 的正方形归一化坐标系统
        final double MINERU_NORMALIZED_SIZE = 1000.0;
        
        // X 和 Y 分别独立缩放
        double scaleX = imageWidth / MINERU_NORMALIZED_SIZE;
        double scaleY = imageHeight / MINERU_NORMALIZED_SIZE;
        
        log.info("🔧 坐标转换 - PDF尺寸: {}x{}, 图片尺寸: {}x{}, MinerU归一化: 1000x1000, 缩放比例: scaleX={}, scaleY={}", 
            pdfWidth, pdfHeight, imageWidth, imageHeight, 
            String.format("%.3f", scaleX), String.format("%.3f", scaleY));
        log.info("📍 MinerU原始bbox: [{}, {}, {}, {}]", 
            mineruBbox[0], mineruBbox[1], mineruBbox[2], mineruBbox[3]);
        
        int[] imageBbox = new int[4];
        imageBbox[0] = (int) Math.round(mineruBbox[0] * scaleX);
        imageBbox[1] = (int) Math.round(mineruBbox[1] * scaleY);
        imageBbox[2] = (int) Math.round(mineruBbox[2] * scaleX);
        imageBbox[3] = (int) Math.round(mineruBbox[3] * scaleY);
        
        log.info("✅ 转换后图片bbox: [{}, {}, {}, {}]", 
            imageBbox[0], imageBbox[1], imageBbox[2], imageBbox[3]);
        
        return imageBbox;
    }
    
    /**
     * 计算坐标转换的缩放比例
     * 
     * @param pdfWidth PDF页面宽度
     * @param pdfHeight PDF页面高度
     * @param imageWidth 图片宽度
     * @param imageHeight 图片高度
     * @return [scaleX, scaleY]
     */
    public static double[] calculateScale(
            double pdfWidth,
            double pdfHeight,
            int imageWidth,
            int imageHeight) {
        
        return new double[]{
            imageWidth / pdfWidth,
            imageHeight / pdfHeight
        };
    }
    
    /**
     * 验证坐标是否在有效范围内
     * 
     * @param bbox 坐标框 [x1, y1, x2, y2]
     * @param imageWidth 图片宽度
     * @param imageHeight 图片高度
     * @return 是否有效
     */
    public static boolean isValidBbox(int[] bbox, int imageWidth, int imageHeight) {
        return bbox[0] >= 0 && bbox[0] < imageWidth &&
               bbox[1] >= 0 && bbox[1] < imageHeight &&
               bbox[2] > bbox[0] && bbox[2] <= imageWidth &&
               bbox[3] > bbox[1] && bbox[3] <= imageHeight;
    }
    
    /**
     * 修正超出边界的坐标
     * 
     * @param bbox 坐标框 [x1, y1, x2, y2]
     * @param imageWidth 图片宽度
     * @param imageHeight 图片高度
     * @return 修正后的坐标
     */
    public static int[] clampBbox(int[] bbox, int imageWidth, int imageHeight) {
        int[] clamped = new int[4];
        clamped[0] = Math.max(0, Math.min(bbox[0], imageWidth - 1));
        clamped[1] = Math.max(0, Math.min(bbox[1], imageHeight - 1));
        clamped[2] = Math.max(clamped[0] + 1, Math.min(bbox[2], imageWidth));
        clamped[3] = Math.max(clamped[1] + 1, Math.min(bbox[3], imageHeight));
        return clamped;
    }
}

