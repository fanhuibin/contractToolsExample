package com.zhaoxinms.contract.tools.stamp;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import com.itextpdf.text.pdf.parser.SimpleTextExtractionStrategy;

/**
 * PDF盖章工具类
 * 支持骑缝章、普通印章和自动识别盖章位置
 * 所有关键词和图片路径都需要用户提供
 * 
 * @author ruoyi
 */
public class PdfStampUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(PdfStampUtil.class);
    
    // 印章配置类
    public static class StampConfig {
        private String stampImagePath;           // 印章图片路径（必填）
        private List<String> keywords;          // 需要盖章的关键词（自动盖章时必填）
        private float stampWidth = 80f;         // 印章宽度（默认80点）
        private float stampHeight = 80f;        // 印章高度（默认80点）
        private float transparency = 0.8f;      // 透明度（0-1，默认0.8）
        private boolean replaceKeyword = false; // 是否替换关键词文本（默认false）
        
        /**
         * 构造函数（用于骑缝章和普通印章）
         * @param stampImagePath 印章图片路径
         */
        public StampConfig(String stampImagePath) {
            if (stampImagePath == null || stampImagePath.trim().isEmpty()) {
                throw new IllegalArgumentException("印章图片路径不能为空");
            }
            this.stampImagePath = stampImagePath;
            this.keywords = new ArrayList<>();
        }
        
        /**
         * 构造函数（用于自动识别盖章）
         * @param stampImagePath 印章图片路径
         * @param keywords 关键词列表
         */
        public StampConfig(String stampImagePath, List<String> keywords) {
            if (stampImagePath == null || stampImagePath.trim().isEmpty()) {
                throw new IllegalArgumentException("印章图片路径不能为空");
            }
            if (keywords == null || keywords.isEmpty()) {
                throw new IllegalArgumentException("关键词列表不能为空");
            }
            this.stampImagePath = stampImagePath;
            this.keywords = new ArrayList<>(keywords);
        }
        
        // Getters and Setters
        public String getStampImagePath() { return stampImagePath; }
        public void setStampImagePath(String stampImagePath) { 
            if (stampImagePath == null || stampImagePath.trim().isEmpty()) {
                throw new IllegalArgumentException("印章图片路径不能为空");
            }
            this.stampImagePath = stampImagePath; 
        }
        
        public List<String> getKeywords() { return keywords; }
        public void setKeywords(List<String> keywords) { 
            if (keywords == null) {
                this.keywords = new ArrayList<>();
            } else {
                this.keywords = new ArrayList<>(keywords);
            }
        }
        public void addKeyword(String keyword) { 
            if (keyword != null && !keyword.trim().isEmpty()) {
                this.keywords.add(keyword); 
            }
        }
        
        public float getStampWidth() { return stampWidth; }
        public void setStampWidth(float stampWidth) { this.stampWidth = stampWidth; }
        
        public float getStampHeight() { return stampHeight; }
        public void setStampHeight(float stampHeight) { this.stampHeight = stampHeight; }
        
        public float getTransparency() { return transparency; }
        public void setTransparency(float transparency) { 
            this.transparency = Math.max(0f, Math.min(1f, transparency)); 
        }
        
        public boolean isReplaceKeyword() { return replaceKeyword; }
        public void setReplaceKeyword(boolean replaceKeyword) { this.replaceKeyword = replaceKeyword; }
    }
    
    /**
     * 添加骑缝章（在每页右边缘添加印章）
     * 
     * @param inputPath 输入PDF文件路径
     * @param outputPath 输出PDF文件路径
     * @param stampImagePath 印章图片路径
     * @throws IOException
     * @throws DocumentException
     */
    public static void addRidingStamp(String inputPath, String outputPath, String stampImagePath) 
            throws IOException, DocumentException {
        
        RidingStampUtil.addRidingStamp(inputPath, outputPath, stampImagePath);
    }
    
    /**
     * 添加骑缝章（使用配置对象）
     * 
     * @param inputPath 输入PDF文件路径
     * @param outputPath 输出PDF文件路径
     * @param config 印章配置
     * @throws IOException
     * @throws DocumentException
     */
    public static void addRidingStamp(String inputPath, String outputPath, StampConfig config) 
            throws IOException, DocumentException {
        
        // 转换为RidingStampConfig
        RidingStampUtil.RidingStampConfig ridingConfig = new RidingStampUtil.RidingStampConfig(config.getStampImagePath());
        ridingConfig.setStampWidth(config.getStampWidth());
        ridingConfig.setStampHeight(config.getStampHeight());
        ridingConfig.setTransparency(config.getTransparency());
        
        // 调用专门的骑缝章工具类
        RidingStampUtil.addRidingStamp(inputPath, outputPath, ridingConfig);
    }
    
    /**
     * 添加普通印章（在指定位置添加印章）
     * 
     * @param inputPath 输入PDF文件路径
     * @param outputPath 输出PDF文件路径
     * @param stampImagePath 印章图片路径
     * @param pageNumber 页码（从1开始）
     * @param x X坐标
     * @param y Y坐标
     * @throws IOException
     * @throws DocumentException
     */
    public static void addStamp(String inputPath, String outputPath, String stampImagePath, 
                               int pageNumber, float x, float y) throws IOException, DocumentException {
        
        StampConfig config = new StampConfig(stampImagePath);
        addStamp(inputPath, outputPath, config, pageNumber, x, y);
    }
    
    /**
     * 添加普通印章（使用配置对象）
     * 
     * @param inputPath 输入PDF文件路径
     * @param outputPath 输出PDF文件路径
     * @param config 印章配置
     * @param pageNumber 页码（从1开始）
     * @param x X坐标
     * @param y Y坐标
     * @throws IOException
     * @throws DocumentException
     */
    public static void addStamp(String inputPath, String outputPath, StampConfig config, 
                               int pageNumber, float x, float y) throws IOException, DocumentException {
        
        PdfReader reader = null;
        PdfStamper stamper = null;
        
        try {
            reader = new PdfReader(inputPath);
            stamper = new PdfStamper(reader, new FileOutputStream(outputPath));
            
            // 验证页码
            if (pageNumber < 1 || pageNumber > reader.getNumberOfPages()) {
                throw new IllegalArgumentException("页码超出范围: " + pageNumber);
            }
            
            // 加载印章图片
            Image stampImage = loadStampImage(config.getStampImagePath());
            if (stampImage == null) {
                throw new IllegalArgumentException("无法加载印章图片: " + config.getStampImagePath() + "，请检查文件路径是否正确");
            }
            
            // 设置印章大小和透明度
            stampImage.scaleAbsolute(config.getStampWidth(), config.getStampHeight());
            
            PdfContentByte canvas = stamper.getOverContent(pageNumber);
            
            // 设置透明度
            canvas.setGState(createTransparencyState(config.getTransparency()));
            
            // 添加印章
            stampImage.setAbsolutePosition(x, y);
            canvas.addImage(stampImage);
            
            // 注释掉印章添加完成的详细日志
            // logger.info("在第 {} 页位置 ({}, {}) 添加印章完成", pageNumber, x, y);
            
        } finally {
            if (stamper != null) {
                stamper.close();
            }
            if (reader != null) {
                reader.close();
            }
        }
    }
    
    /**
     * 自动识别并添加印章（根据关键词自动定位盖章位置）
     * 
     * @param inputPath 输入PDF文件路径
     * @param outputPath 输出PDF文件路径
     * @param stampImagePath 印章图片路径
     * @param keywords 关键词列表
     * @throws IOException
     * @throws DocumentException
     */
    public static void addAutoStamp(String inputPath, String outputPath, String stampImagePath, List<String> keywords) 
            throws IOException, DocumentException {
        
        StampConfig config = new StampConfig(stampImagePath, keywords);
        addAutoStamp(inputPath, outputPath, config);
    }
    
    /**
     * 自动识别并添加印章（使用配置对象）
     * 
     * @param inputPath 输入PDF文件路径
     * @param outputPath 输出PDF文件路径
     * @param config 印章配置
     * @throws IOException
     * @throws DocumentException
     */
    public static void addAutoStamp(String inputPath, String outputPath, StampConfig config) 
            throws IOException, DocumentException {
        
        if (config.getKeywords().isEmpty()) {
            throw new IllegalArgumentException("自动盖章需要提供关键词列表");
        }
        
        PdfReader reader = null;
        PdfStamper stamper = null;
        
        try {
            reader = new PdfReader(inputPath);
            stamper = new PdfStamper(reader, new FileOutputStream(outputPath));
            
            // 加载印章图片
            Image stampImage = loadStampImage(config.getStampImagePath());
            if (stampImage == null) {
                throw new IllegalArgumentException("无法加载印章图片: " + config.getStampImagePath() + "，请检查文件路径是否正确");
            }
            
            // 设置印章大小
            stampImage.scaleAbsolute(config.getStampWidth(), config.getStampHeight());
            
            int totalPages = reader.getNumberOfPages();
            int stampCount = 0;
            
            // 注释掉自动识别盖章位置的详细日志
            // logger.debug("开始自动识别盖章位置，总页数: {}，关键词: {}", totalPages, config.getKeywords());
            
            // 创建文本定位策略
            PdfTextLocationStrategy locationStrategy = new PdfTextLocationStrategy();
            locationStrategy.setTargetKeywords(config.getKeywords());
            
            // 创建内容解析器
            PdfReaderContentParser parser = new PdfReaderContentParser(reader);
            
            // 遍历每一页提取文本
            for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
                // 注释掉页面文本提取日志，避免大量输出
                // logger.debug("提取第 {} 页文本", pageNum);
                locationStrategy.setCurrentPageNumber(pageNum - 1); // 页码从0开始
                parser.processContent(pageNum, locationStrategy);
            }
            
            // 查找所有关键词位置
            locationStrategy.findKeywordLocations();
            List<PdfTextLocationStrategy.TextLocationInfo> foundLocations = locationStrategy.getFoundLocations();
            
            if (foundLocations.isEmpty()) {
                logger.warn("使用精确定位未找到任何关键词，尝试简单文本匹配");
                
                // 使用简单文本提取作为备选方案
                for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
                    String pageText = PdfTextExtractor.getTextFromPage(reader, pageNum, new SimpleTextExtractionStrategy());
                    
                    for (String keyword : config.getKeywords()) {
                        if (pageText.contains(keyword)) {
                            // 注释掉关键词发现的详细日志
                            // logger.debug("在第 {} 页发现关键词（简单匹配）: {}", pageNum, keyword);
                            
                            // 简单匹配时，尝试估算关键词位置
                            Rectangle pageSize = reader.getPageSize(pageNum);
                            
                            // 查找关键词在文本中的位置，估算大概的坐标
                            int keywordIndex = pageText.indexOf(keyword);
                            float estimatedX, estimatedY;
                            
                            if (keywordIndex > 0) {
                                // 根据关键词在文本中的位置估算坐标
                                float textProgress = (float)keywordIndex / pageText.length();
                                estimatedX = pageSize.getWidth() * 0.2f + (pageSize.getWidth() * 0.6f * textProgress);
                                estimatedY = pageSize.getHeight() * 0.8f - (pageSize.getHeight() * 0.6f * textProgress);
                            } else {
                                // 如果无法估算，使用页面中央偏右下的位置
                                estimatedX = pageSize.getWidth() * 0.7f;
                                estimatedY = pageSize.getHeight() * 0.3f;
                            }
                            
                            // 调整为印章中心位置
                            float x = estimatedX - config.getStampWidth() / 2;
                            float y = estimatedY - config.getStampHeight() / 2;
                            
                            // 确保印章不超出页面边界
                            if (x < 5) x = 5;
                            if (x + config.getStampWidth() > pageSize.getWidth() - 5) {
                                x = pageSize.getWidth() - config.getStampWidth() - 5;
                            }
                            if (y < 5) y = 5;
                            if (y + config.getStampHeight() > pageSize.getHeight() - 5) {
                                y = pageSize.getHeight() - config.getStampHeight() - 5;
                            }
                            
                            PdfContentByte canvas = stamper.getOverContent(pageNum);
                            canvas.setGState(createTransparencyState(config.getTransparency()));
                            
                            stampImage.setAbsolutePosition(x, y);
                            canvas.addImage(stampImage);
                            
                            stampCount++;
                            // 注释掉印章添加的详细日志
                            // logger.debug("在第 {} 页位置 ({}, {}) 添加印章（估算位置），关键词: {}", 
                            //     pageNum, x, y, keyword);
                            
                            break;
                        }
                    }
                }
            } else {
                // 为每个找到的关键词位置添加印章
                for (PdfTextLocationStrategy.TextLocationInfo locationInfo : foundLocations) {
                    int pageNum = locationInfo.getPageNumber() + 1; // 转换为1基页码
                    String keyword = locationInfo.getKeyword();
                    
                    // 注释掉印章添加的详细日志
                    // logger.debug("在第 {} 页为关键词 '{}' 添加印章: {}", pageNum, keyword, locationInfo);
                    
                    PdfContentByte canvas = stamper.getOverContent(pageNum);
                    
                    // 设置透明度
                    canvas.setGState(createTransparencyState(config.getTransparency()));
                    
                    // 如果配置了替换关键词，先用白色矩形覆盖原文本
                    if (config.isReplaceKeyword()) {
                        canvas.setColorFill(BaseColor.WHITE);
                        canvas.rectangle(locationInfo.getX(), locationInfo.getY(), 
                                       locationInfo.getWidth(), locationInfo.getHeight());
                        canvas.fill();
                        // logger.debug("用白色矩形覆盖关键词: '{}'", keyword);
                    }
                    
                    // 计算印章位置（印章中心覆盖到关键词中心）
                    float x = locationInfo.getCenterX() - config.getStampWidth() / 2;
                    float y = locationInfo.getCenterY() - config.getStampHeight() / 2;
                    
                    // 确保印章不超出页面边界
                    Rectangle pageSize = reader.getPageSize(pageNum);
                    if (x < 0) {
                        x = 5;
                    }
                    if (x + config.getStampWidth() > pageSize.getWidth()) {
                        x = pageSize.getWidth() - config.getStampWidth() - 5;
                    }
                    if (y < 0) {
                        y = 5;
                    }
                    if (y + config.getStampHeight() > pageSize.getHeight()) {
                        y = pageSize.getHeight() - config.getStampHeight() - 5;
                    }
                    
                    // 添加印章
                    stampImage.setAbsolutePosition(x, y);
                    canvas.addImage(stampImage);
                    
                    stampCount++;
                    // 注释掉印章添加的详细日志
                    // logger.debug("在第 {} 页位置 ({}, {}) 添加印章，覆盖关键词: '{}'", pageNum, x, y, keyword);
                }
            }
            
            logger.info("自动盖章完成，共添加 {} 个印章，输出文件: {}", stampCount, outputPath);
            
        } finally {
            if (stamper != null) {
                stamper.close();
            }
            if (reader != null) {
                reader.close();
            }
        }
    }
    
    /**
     * 加载印章图片
     * 
     * @param imagePath 图片路径
     * @return Image对象
     */
    private static Image loadStampImage(String imagePath) {
        try {
            // 首先尝试作为文件路径加载
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                return Image.getInstance(imagePath);
            }
            
            // 尝试从classpath加载
            try {
                java.io.InputStream inputStream = PdfStampUtil.class.getClassLoader().getResourceAsStream(imagePath);
                if (inputStream != null) {
                    byte[] imageBytes = new byte[inputStream.available()];
                    inputStream.read(imageBytes);
                    inputStream.close();
                    return Image.getInstance(imageBytes);
                }
            } catch (Exception e) {
                logger.warn("从classpath加载图片失败: {}", e.getMessage());
            }
            
            // 如果都失败了，返回null
            return null;
            
        } catch (Exception e) {
            logger.error("加载印章图片失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 创建透明度状态
     * 
     * @param transparency 透明度（0-1）
     * @return PdfGState对象
     */
    private static com.itextpdf.text.pdf.PdfGState createTransparencyState(float transparency) {
        com.itextpdf.text.pdf.PdfGState gState = new com.itextpdf.text.pdf.PdfGState();
        gState.setFillOpacity(transparency);
        gState.setStrokeOpacity(transparency);
        return gState;
    }
    

    

} 