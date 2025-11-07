package com.zhaoxinms.contract.tools.comparePRO.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * 去水印工具类
 * 使用 Java 原生图像处理去除图片中的水印
 */
@Component
public class WatermarkRemover {

    private static final Logger logger = LoggerFactory.getLogger(WatermarkRemover.class);

    /**
     * 去除图片水印（使用默认参数）
     * 
     * @param imagePath 图片路径
     * @return 是否成功
     */
    public boolean removeWatermark(String imagePath) {
        return removeWatermark(imagePath, 160, 160, 160, 255, 255, 255);
    }

    /**
     * 去除图片水印（扩展颜色范围）
     * 
     * @param imagePath 图片路径
     * @return 是否成功
     */
    public boolean removeWatermarkExtended(String imagePath) {
        return removeWatermark(imagePath, 120, 120, 120, 255, 255, 255);
    }

    /**
     * 去除图片水印（宽松颜色范围）
     * 
     * @param imagePath 图片路径
     * @return 是否成功
     */
    public boolean removeWatermarkLoose(String imagePath) {
        return removeWatermark(imagePath, 80, 80, 80, 255, 255, 255);
    }

    /**
     * 智能去除水印（尝试多种颜色范围）
     * 
     * @param imagePath 图片路径
     * @return 是否成功
     */
    public boolean removeWatermarkSmart(String imagePath) {
        // 先尝试默认范围
        if (removeWatermark(imagePath, 160, 160, 160, 255, 255, 255)) {
            return true;
        }
        
        // 如果默认范围效果不好，尝试扩展范围
        if (removeWatermark(imagePath, 120, 120, 120, 255, 255, 255)) {
            return true;
        }
        
        // 最后尝试宽松范围
        if (removeWatermark(imagePath, 80, 80, 80, 255, 255, 255)) {
            return true;
        }
        
        return false;
    }

    /**
     * 去除图片水印
     * 
     * @param imagePath 图片路径
     * @param lowerR 水印颜色下限 R
     * @param lowerG 水印颜色下限 G
     * @param lowerB 水印颜色下限 B
     * @param upperR 水印颜色上限 R
     * @param upperG 水印颜色上限 G
     * @param upperB 水印颜色上限 B
     * @return 是否成功
     */
    public boolean removeWatermark(String imagePath, int lowerR, int lowerG, int lowerB,
                                   int upperR, int upperG, int upperB) {
        try {
            File imageFile = new File(imagePath);
            if (!imageFile.exists()) {
                logger.warn("图片文件不存在: {}", imagePath);
                return false;
            }

            logger.debug("开始处理图片去水印: {}", imagePath);

            // 读取图片
            BufferedImage img = ImageIO.read(imageFile);
            if (img == null) {
                logger.error("无法读取图片: {}", imagePath);
                return false;
            }

            int width = img.getWidth();
            int height = img.getHeight();

            // 创建新图像用于处理
            BufferedImage processedImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            
            // 逐像素处理
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int rgb = img.getRGB(x, y);
                    
                    // 提取 RGB 分量
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    
                    // 检测是否在水印颜色范围内
                    if (r >= lowerR && r <= upperR &&
                        g >= lowerG && g <= upperG &&
                        b >= lowerB && b <= upperB) {
                        // 替换为白色
                        processedImg.setRGB(x, y, 0xFFFFFF);
                    } else {
                        // 保持原色
                        processedImg.setRGB(x, y, rgb);
                    }
                }
            }

            // 应用简单的模糊处理（可选，模拟高斯模糊效果）
            // 这里使用简单的平均模糊来柔化边缘
            BufferedImage blurredImg = applySimpleBlur(processedImg);

            // 确定输出格式
            String fileExtension = getFileExtension(imagePath);
            String formatName = "png";
            if (fileExtension != null) {
                if (fileExtension.equalsIgnoreCase("jpg") || fileExtension.equalsIgnoreCase("jpeg")) {
                    formatName = "jpg";
                } else if (fileExtension.equalsIgnoreCase("bmp")) {
                    formatName = "bmp";
                }
            }

            // 保存处理后的图片
            boolean success = ImageIO.write(blurredImg, formatName, imageFile);

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
     * 应用简单的模糊处理
     * 
     * @param img 原始图像
     * @return 模糊后的图像
     */
    private BufferedImage applySimpleBlur(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();
        BufferedImage blurred = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        
        // 简单的 3x3 平均模糊
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int sumR = 0, sumG = 0, sumB = 0;
                int count = 0;
                
                // 遍历 3x3 邻域
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int nx = x + dx;
                        int ny = y + dy;
                        
                        // 边界检查
                        if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                            int rgb = img.getRGB(nx, ny);
                            sumR += (rgb >> 16) & 0xFF;
                            sumG += (rgb >> 8) & 0xFF;
                            sumB += rgb & 0xFF;
                            count++;
                        }
                    }
                }
                
                // 计算平均值
                int avgR = sumR / count;
                int avgG = sumG / count;
                int avgB = sumB / count;
                
                // 设置像素值
                int newRgb = (avgR << 16) | (avgG << 8) | avgB;
                blurred.setRGB(x, y, newRgb);
            }
        }
        
        return blurred;
    }

    /**
     * 获取文件扩展名
     * 
     * @param filePath 文件路径
     * @return 扩展名
     */
    private String getFileExtension(String filePath) {
        int lastDot = filePath.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filePath.length() - 1) {
            return filePath.substring(lastDot + 1);
        }
        return null;
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
        return removeWatermark(imagePath, 
            config.getLowerR(), config.getLowerG(), config.getLowerB(),
            config.getUpperR(), config.getUpperG(), config.getUpperB());
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
