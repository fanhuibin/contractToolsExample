package com.zhaoxinms.contract.tools.ocrcompare.util;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zhaoxinms.contract.tools.ocrcompare.config.GPUOCRConfig;
import com.zhaoxinms.contract.tools.config.ZxcmConfig;

/**
 * OCR图片保存工具类
 * 负责将PDF页面渲染为图片并保存到指定目录
 */
@Component
public class OcrImageSaver {

    @Autowired
    private GPUOCRConfig gpuOcrConfig;

    @Autowired
    private ZxcmConfig zxcmConfig;

    /**
     * 保存PDF的OCR图片到指定目录
     * @param pdfPath PDF文件路径
     * @param taskId 任务ID
     * @param mode 模式标识（如"old", "new", "gradio"等）
     * @return 保存的图片目录路径
     */
    public Path saveOcrImages(Path pdfPath, String taskId, String mode) throws Exception {
        // 检查是否启用图片保存功能
        if (!gpuOcrConfig.isSaveOcrImages()) {
            System.out.println("[" + mode + "] OCR图片保存功能已关闭，跳过保存");
            return null;
        }
        
        // 计算动态DPI
        int dpi;
        try (PDDocument doc = PDDocument.load(pdfPath.toFile())) {
            int pageCount = doc.getNumberOfPages();
            dpi = calculateDynamicDpi(pageCount);
            System.out.println("[" + mode + "] 文档页数: " + pageCount + ", 使用动态DPI: " + dpi);
        }
        
        // 创建图片保存目录：task目录下的images子目录
        String uploadRootPath = zxcmConfig.getFileUpload().getRootPath();
        Path imagesDir = Paths.get(uploadRootPath, "gpu-ocr-compare", "tasks", taskId, "images", mode);
        Files.createDirectories(imagesDir);
        
        try (PDDocument doc = PDDocument.load(pdfPath.toFile())) {
            PDFRenderer renderer = new PDFRenderer(doc);
            long minPixels = gpuOcrConfig.getMinPixels();
            long maxPixels = gpuOcrConfig.getMaxPixels();
            
            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, dpi);
                // 像素裁剪：保持比例缩放到[minPixels, maxPixels]区间内
                if (image != null && (minPixels > 0 || maxPixels > 0)) {
                    long pixels = (long) image.getWidth() * (long) image.getHeight();
                    double scale = 1.0;
                    if (maxPixels > 0 && pixels > maxPixels) {
                        scale = Math.sqrt((double) maxPixels / pixels);
                    } else if (minPixels > 0 && pixels < minPixels) {
                        scale = Math.sqrt((double) minPixels / Math.max(1.0, pixels));
                    }
                    if (scale > 0 && Math.abs(scale - 1.0) > 1e-6) {
                        int newW = Math.max(1, (int) Math.round(image.getWidth() * scale));
                        int newH = Math.max(1, (int) Math.round(image.getHeight() * scale));
                        BufferedImage scaled = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
                        Graphics2D g2d = scaled.createGraphics();
                        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        g2d.drawImage(image, 0, 0, newW, newH, null);
                        g2d.dispose();
                        image.flush();
                        image = scaled;
                    }
                }
                
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    ImageIO.write(image, "png", baos);
                    byte[] bytes = baos.toByteArray();
                    
                    // 保存图片
                    Path imagePath = imagesDir.resolve("page-" + (i + 1) + ".png");
                    Files.write(imagePath, bytes);
                    System.out.println("[" + mode + "] OCR图片已保存: " + imagePath.toString());
                }
            }
            
            System.out.println("[" + mode + "] OCR图片保存完成，目录: " + imagesDir.toString());
            return imagesDir;
        }
    }

    /**
     * 保存PDF的OCR图片到指定目录（重载方法，使用默认模式）
     * @param pdfPath PDF文件路径
     * @param taskId 任务ID
     * @return 保存的图片目录路径
     */
    public Path saveOcrImages(Path pdfPath, String taskId) throws Exception {
        return saveOcrImages(pdfPath, taskId, "default");
    }

    /**
     * 基于页数动态计算DPI，避免Canvas像素限制问题
     * @param pageCount 文档页数
     * @return 动态调整后的DPI值
     */
    private int calculateDynamicDpi(int pageCount) {
        int baseDpi = gpuOcrConfig.getRenderDpi(); // 基础DPI (默认200)
        
        // 根据页数动态调整DPI
        if (pageCount <= 20) {
            // 小文档，保持高DPI
            return baseDpi;
        } else if (pageCount <= 50) {
            // 中等文档，适度降低DPI
            return (int) (baseDpi * 0.8); // 160 DPI
        } else if (pageCount <= 100) {
            // 大文档，显著降低DPI
            return (int) (baseDpi * 0.6); // 120 DPI
        } else {
            // 超大文档，大幅降低DPI
            return (int) (baseDpi * 0.4); // 80 DPI
        }
    }
}
