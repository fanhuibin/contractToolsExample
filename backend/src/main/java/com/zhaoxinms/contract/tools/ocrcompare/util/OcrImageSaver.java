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
import com.zhaoxinms.contract.tools.ocrcompare.compare.GPUOCRCompareOptions;

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

    @Autowired
    private WatermarkRemover watermarkRemover;

    /**
     * 保存PDF的OCR图片到指定目录
     * @param pdfPath PDF文件路径
     * @param taskId 任务ID
     * @param mode 模式标识（如"old", "new", "gradio"等）
     * @return 保存的图片目录路径
     */
    public Path saveOcrImages(Path pdfPath, String taskId, String mode) throws Exception {
        return saveOcrImages(pdfPath, taskId, mode, null);
    }

    /**
     * 保存PDF的OCR图片到指定目录（支持去水印选项）
     * @param pdfPath PDF文件路径
     * @param taskId 任务ID
     * @param mode 模式标识（如"old", "new", "gradio"等）
     * @param options 比对选项（包含去水印设置）
     * @return 保存的图片目录路径
     */
    public Path saveOcrImages(Path pdfPath, String taskId, String mode, GPUOCRCompareOptions options) throws Exception {
        // 调试日志：记录去水印设置
        System.out.println("[" + mode + "] OcrImageSaver收到的去水印设置: " + (options != null ? options.isRemoveWatermark() : "options为null"));
        
        // 检查是否启用图片保存功能
        if (!gpuOcrConfig.isSaveOcrImages()) {
            System.out.println("[" + mode + "] OCR图片保存功能已关闭，跳过保存");
            return null;
        }
        
        // 使用固定DPI
        int dpi = gpuOcrConfig.getRenderDpi();
        System.out.println("[" + mode + "] 使用固定DPI: " + dpi);
        
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
                    
                    // 去水印处理
                    if (options != null && options.isRemoveWatermark()) {
                        try {
                            String strength = options.getWatermarkRemovalStrength();
                            System.out.println("[" + mode + "] 第" + (i + 1) + "页开始去水印，强度: " + strength);
                            
                            boolean watermarkRemoved = false;
                            switch (strength) {
                                case "default":
                                    watermarkRemoved = watermarkRemover.removeWatermark(imagePath.toString());
                                    break;
                                case "extended":
                                    watermarkRemoved = watermarkRemover.removeWatermarkExtended(imagePath.toString());
                                    break;
                                case "loose":
                                    watermarkRemoved = watermarkRemover.removeWatermarkLoose(imagePath.toString());
                                    break;
                                case "smart":
                                default:
                                    watermarkRemoved = watermarkRemover.removeWatermarkSmart(imagePath.toString());
                                    break;
                            }
                            
                            if (watermarkRemoved) {
                                System.out.println("[" + mode + "] 第" + (i + 1) + "页去水印处理完成(" + strength + "): " + imagePath.toString());
                            } else {
                                System.out.println("[" + mode + "] 第" + (i + 1) + "页水印去除失败(" + strength + "): " + imagePath.toString());
                            }
                        } catch (Exception e) {
                            System.err.println("[" + mode + "] 第" + (i + 1) + "页水印去除异常: " + e.getMessage());
                        }
                    }
                    
                    //System.out.println("[" + mode + "] OCR图片已保存: " + imagePath.toString());
                }
            }
            
            //System.out.println("[" + mode + "] OCR图片保存完成，目录: " + imagesDir.toString());
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
}
