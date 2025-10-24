package com.zhaoxinms.contract.tools.watermark;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * 去水印工具类
 * 使用OpenCV去除图片中的水印
 * 
 * 【完全复制自 com.zhaoxinms.contract.tools.comparePRO.util.WatermarkRemover】
 */
public class OpenCVWatermarkUtil {

    private static final Logger logger = LoggerFactory.getLogger(OpenCVWatermarkUtil.class);

    // 默认水印颜色范围（BGR色彩空间）- 对应原算法的[160,160,160]到[255,255,255]
    private static final Scalar DEFAULT_LOWER_BGR = new Scalar(160, 160, 160);
    private static final Scalar DEFAULT_UPPER_BGR = new Scalar(255, 255, 255);
    
    // 扩展颜色范围 - 适用于更多水印类型
    private static final Scalar EXTENDED_LOWER_BGR = new Scalar(120, 120, 120);
    private static final Scalar EXTENDED_UPPER_BGR = new Scalar(255, 255, 255);
    
    // 宽松颜色范围 - 适用于深色半透明水印
    private static final Scalar LOOSE_LOWER_BGR = new Scalar(80, 80, 80);
    private static final Scalar LOOSE_UPPER_BGR = new Scalar(255, 255, 255);
    
    // 高斯模糊核大小
    private static final Size GAUSSIAN_KERNEL_SIZE = new Size(1, 1);
    private static final double GAUSSIAN_SIGMA = 0;
    
    // 替换颜色（白色）
    private static final Scalar REPLACEMENT_COLOR = new Scalar(255, 255, 255);

    /**
     * 加载OpenCV库
     */
    static {
        try {
            nu.pattern.OpenCV.loadLocally();
            logger.info("OpenCV库加载成功");
        } catch (Exception e) {
            logger.error("OpenCV库加载失败", e);
        }
    }

    /**
     * 去除图片水印（使用默认参数）
     * 
     * @param imagePath 图片路径
     * @return 是否成功
     */
    public boolean removeWatermark(String imagePath) {
        return removeWatermark(imagePath, DEFAULT_LOWER_BGR, DEFAULT_UPPER_BGR);
    }

    /**
     * 去除图片水印（扩展颜色范围）
     * 
     * @param imagePath 图片路径
     * @return 是否成功
     */
    public boolean removeWatermarkExtended(String imagePath) {
        return removeWatermark(imagePath, EXTENDED_LOWER_BGR, EXTENDED_UPPER_BGR);
    }

    /**
     * 去除图片水印（宽松颜色范围）
     * 
     * @param imagePath 图片路径
     * @return 是否成功
     */
    public boolean removeWatermarkLoose(String imagePath) {
        return removeWatermark(imagePath, LOOSE_LOWER_BGR, LOOSE_UPPER_BGR);
    }

    /**
     * 智能去除水印（尝试多种颜色范围）
     * 
     * @param imagePath 图片路径
     * @return 是否成功
     */
    public boolean removeWatermarkSmart(String imagePath) {
        
        // 先尝试默认范围
        if (removeWatermark(imagePath, DEFAULT_LOWER_BGR, DEFAULT_UPPER_BGR)) {
            return true;
        }
        
        // 如果默认范围效果不好，尝试扩展范围
        if (removeWatermark(imagePath, EXTENDED_LOWER_BGR, EXTENDED_UPPER_BGR)) {
            return true;
        }
        
        // 最后尝试宽松范围
        if (removeWatermark(imagePath, LOOSE_LOWER_BGR, LOOSE_UPPER_BGR)) {
            return true;
        }
        
        return false;
    }

    /**
     * 去除图片水印
     * 
     * @param imagePath 图片路径
     * @param lowerBgr 水印颜色下限（BGR）
     * @param upperBgr 水印颜色上限（BGR）
     * @return 是否成功
     */
    public boolean removeWatermark(String imagePath, Scalar lowerBgr, Scalar upperBgr) {
        try {
            File imageFile = new File(imagePath);
            if (!imageFile.exists()) {
                logger.warn("图片文件不存在: {}", imagePath);
                return false;
            }

            logger.debug("开始处理图片去水印: {}", imagePath);

            // 读取图片
            Mat img = Imgcodecs.imread(imagePath);
            if (img.empty()) {
                logger.error("无法读取图片: {}", imagePath);
                return false;
            }

            // 创建掩码
            Mat mask = new Mat();
            Core.inRange(img, lowerBgr, upperBgr, mask);

            // 高斯模糊处理掩码
            Mat blurredMask = new Mat();
            Imgproc.GaussianBlur(mask, blurredMask, GAUSSIAN_KERNEL_SIZE, GAUSSIAN_SIGMA);

            // 将掩码区域替换为白色
            img.setTo(REPLACEMENT_COLOR, blurredMask);

            // 保存处理后的图片
            boolean success = Imgcodecs.imwrite(imagePath, img);

            // 释放内存
            img.release();
            mask.release();
            blurredMask.release();

            if (success) {
                logger.debug("图片去水印完成: {}", imagePath);
            } else {
                logger.error("保存去水印图片失败: {}", imagePath);
            }

            return success;

        } catch (Exception e) {
            logger.error("图片去水印处理失败: {}", imagePath, e);
            return false;
        }
    }

    /**
     * 批量去除水印
     * 
     * @param imagePaths 图片路径列表
     * @return 成功处理的图片数量
     */
    public int removeWatermarkBatch(String[] imagePaths) {
        int successCount = 0;
        for (String imagePath : imagePaths) {
            if (removeWatermark(imagePath)) {
                successCount++;
            }
        }
        logger.info("批量去水印完成，成功处理 {}/{} 张图片", successCount, imagePaths.length);
        return successCount;
    }

    /**
     * 去除目录下所有图片的水印
     * 
     * @param directoryPath 目录路径
     * @return 成功处理的图片数量
     */
    public int removeWatermarkInDirectory(String directoryPath) {
        try {
            File directory = new File(directoryPath);
            if (!directory.exists() || !directory.isDirectory()) {
                logger.warn("目录不存在或不是有效目录: {}", directoryPath);
                return 0;
            }

            File[] imageFiles = directory.listFiles((dir, name) -> {
                String lowerName = name.toLowerCase();
                return lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || 
                       lowerName.endsWith(".png") || lowerName.endsWith(".bmp");
            });

            if (imageFiles == null || imageFiles.length == 0) {
                logger.info("目录中没有找到图片文件: {}", directoryPath);
                return 0;
            }

            int successCount = 0;
            for (File imageFile : imageFiles) {
                if (removeWatermark(imageFile.getAbsolutePath())) {
                    successCount++;
                }
            }

            logger.info("目录去水印完成，成功处理 {}/{} 张图片", successCount, imageFiles.length);
            return successCount;

        } catch (Exception e) {
            logger.error("批量去水印处理失败: {}", directoryPath, e);
            return 0;
        }
    }

    /**
     * 自定义水印检测参数去除水印
     * 
     * @param imagePath 图片路径
     * @param config 水印检测配置
     * @return 是否成功
     */
    public boolean removeWatermarkWithConfig(String imagePath, WatermarkConfig config) {
        Scalar lowerBgr = new Scalar(config.getLowerB(), config.getLowerG(), config.getLowerR());
        Scalar upperBgr = new Scalar(config.getUpperB(), config.getUpperG(), config.getUpperR());
        return removeWatermark(imagePath, lowerBgr, upperBgr);
    }

    /**
     * 水印检测配置类
     */
    public static class WatermarkConfig {
        private int lowerR = 160;
        private int lowerG = 160;
        private int lowerB = 160;
        private int upperR = 255;
        private int upperG = 255;
        private int upperB = 255;

        public WatermarkConfig() {}

        public WatermarkConfig(int lowerR, int lowerG, int lowerB, int upperR, int upperG, int upperB) {
            this.lowerR = lowerR;
            this.lowerG = lowerG;
            this.lowerB = lowerB;
            this.upperR = upperR;
            this.upperG = upperG;
            this.upperB = upperB;
        }

        // Getters and Setters
        public int getLowerR() { return lowerR; }
        public void setLowerR(int lowerR) { this.lowerR = lowerR; }

        public int getLowerG() { return lowerG; }
        public void setLowerG(int lowerG) { this.lowerG = lowerG; }

        public int getLowerB() { return lowerB; }
        public void setLowerB(int lowerB) { this.lowerB = lowerB; }

        public int getUpperR() { return upperR; }
        public void setUpperR(int upperR) { this.upperR = upperR; }

        public int getUpperG() { return upperG; }
        public void setUpperG(int upperG) { this.upperG = upperG; }

        public int getUpperB() { return upperB; }
        public void setUpperB(int upperB) { this.upperB = upperB; }
    }
}

