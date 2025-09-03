package com.zhaoxinms.contract.tools.stamp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
 * 支持骑缝章、普通印章和自动识别盖章
 */
public class PdfStampUtil {
	private static final Logger logger = LoggerFactory.getLogger(PdfStampUtil.class);

	public static class StampConfig {
		private String stampImagePath;
		private List<String> keywords;
		private float stampWidth = 80f;
		private float stampHeight = 80f;
		private float transparency = 0.8f;
		private boolean replaceKeyword = false;

		public StampConfig(String stampImagePath) {
			if (stampImagePath == null || stampImagePath.trim().isEmpty()) throw new IllegalArgumentException("印章图片路径不能为空");
			this.stampImagePath = stampImagePath;
			this.keywords = new ArrayList<>();
		}
		public StampConfig(String stampImagePath, List<String> keywords) {
			if (stampImagePath == null || stampImagePath.trim().isEmpty()) throw new IllegalArgumentException("印章图片路径不能为空");
			if (keywords == null || keywords.isEmpty()) throw new IllegalArgumentException("关键词列表不能为空");
			this.stampImagePath = stampImagePath;
			this.keywords = new ArrayList<>(keywords);
		}
		public String getStampImagePath() { return stampImagePath; }
		public void setStampImagePath(String stampImagePath) { if (stampImagePath == null || stampImagePath.trim().isEmpty()) throw new IllegalArgumentException("印章图片路径不能为空"); this.stampImagePath = stampImagePath; }
		public List<String> getKeywords() { return keywords; }
		public void setKeywords(List<String> keywords) { this.keywords = (keywords == null) ? new ArrayList<>() : new ArrayList<>(keywords); }
		public void addKeyword(String keyword) { if (keyword != null && !keyword.trim().isEmpty()) this.keywords.add(keyword); }
		public float getStampWidth() { return stampWidth; }
		public void setStampWidth(float stampWidth) { this.stampWidth = stampWidth; }
		public float getStampHeight() { return stampHeight; }
		public void setStampHeight(float stampHeight) { this.stampHeight = stampHeight; }
		public float getTransparency() { return transparency; }
		public void setTransparency(float transparency) { this.transparency = Math.max(0f, Math.min(1f, transparency)); }
		public boolean isReplaceKeyword() { return replaceKeyword; }
		public void setReplaceKeyword(boolean replaceKeyword) { this.replaceKeyword = replaceKeyword; }
	}

	public static void addRidingStamp(String inputPath, String outputPath, String stampImagePath) throws IOException, DocumentException {
		RidingStampUtil.addRidingStamp(inputPath, outputPath, stampImagePath);
	}

	public static void addRidingStamp(String inputPath, String outputPath, StampConfig config) throws IOException, DocumentException {
		RidingStampUtil.RidingStampConfig ridingConfig = new RidingStampUtil.RidingStampConfig(config.getStampImagePath());
		ridingConfig.setStampWidth(config.getStampWidth());
		ridingConfig.setStampHeight(config.getStampHeight());
		ridingConfig.setTransparency(config.getTransparency());
		RidingStampUtil.addRidingStamp(inputPath, outputPath, ridingConfig);
	}

	public static void addStamp(String inputPath, String outputPath, String stampImagePath, int pageNumber, float x, float y) throws IOException, DocumentException {
		StampConfig config = new StampConfig(stampImagePath);
		addStamp(inputPath, outputPath, config, pageNumber, x, y);
	}

	public static void addStamp(String inputPath, String outputPath, StampConfig config, int pageNumber, float x, float y) throws IOException, DocumentException {
		PdfReader reader = null; PdfStamper stamper = null;
		try {
			reader = new PdfReader(inputPath);
			stamper = new PdfStamper(reader, new FileOutputStream(outputPath));
			if (pageNumber < 1 || pageNumber > reader.getNumberOfPages()) throw new IllegalArgumentException("页码超出范围: " + pageNumber);
			Image stampImage = loadStampImage(config.getStampImagePath());
			if (stampImage == null) throw new IllegalArgumentException("无法加载印章图片: " + config.getStampImagePath());
			stampImage.scaleAbsolute(config.getStampWidth(), config.getStampHeight());
			PdfContentByte canvas = stamper.getOverContent(pageNumber);
			canvas.setGState(createTransparencyState(config.getTransparency()));
			stampImage.setAbsolutePosition(x, y);
			canvas.addImage(stampImage);
		} finally {
			if (stamper != null) stamper.close();
			if (reader != null) reader.close();
		}
	}

	public static void addAutoStamp(String inputPath, String outputPath, String stampImagePath, List<String> keywords) throws IOException, DocumentException {
		StampConfig config = new StampConfig(stampImagePath, keywords);
		addAutoStamp(inputPath, outputPath, config);
	}

	public static void addAutoStamp(String inputPath, String outputPath, StampConfig config) throws IOException, DocumentException {
		if (config.getKeywords().isEmpty()) throw new IllegalArgumentException("自动盖章需要提供关键词列表");
		PdfReader reader = null; PdfStamper stamper = null;
		try {
			reader = new PdfReader(inputPath);
			stamper = new PdfStamper(reader, new FileOutputStream(outputPath));
			Image stampImage = loadStampImage(config.getStampImagePath());
			if (stampImage == null) throw new IllegalArgumentException("无法加载印章图片: " + config.getStampImagePath());
			stampImage.scaleAbsolute(config.getStampWidth(), config.getStampHeight());
			int totalPages = reader.getNumberOfPages();
			PdfTextLocationStrategy locationStrategy = new PdfTextLocationStrategy();
			locationStrategy.setTargetKeywords(config.getKeywords());
			PdfReaderContentParser parser = new PdfReaderContentParser(reader);
			for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
				locationStrategy.setCurrentPageNumber(pageNum - 1);
				parser.processContent(pageNum, locationStrategy);
			}
			locationStrategy.findKeywordLocations();
			List<PdfTextLocationStrategy.TextLocationInfo> foundLocations = locationStrategy.getFoundLocations();
			if (foundLocations.isEmpty()) {
				// 简单匹配回退
				for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
					String pageText = PdfTextExtractor.getTextFromPage(reader, pageNum, new SimpleTextExtractionStrategy());
					for (String keyword : config.getKeywords()) {
						if (pageText.contains(keyword)) {
							Rectangle pageSize = reader.getPageSize(pageNum);
							int keywordIndex = pageText.indexOf(keyword);
							float estimatedX, estimatedY;
							if (keywordIndex > 0) {
								float textProgress = (float) keywordIndex / pageText.length();
								estimatedX = pageSize.getWidth() * 0.2f + (pageSize.getWidth() * 0.6f * textProgress);
								estimatedY = pageSize.getHeight() * 0.8f - (pageSize.getHeight() * 0.6f * textProgress);
							} else {
								estimatedX = pageSize.getWidth() * 0.7f;
								estimatedY = pageSize.getHeight() * 0.3f;
							}
							float x = estimatedX - config.getStampWidth() / 2;
							float y = estimatedY - config.getStampHeight() / 2;
							if (x < 5) x = 5;
							if (x + config.getStampWidth() > pageSize.getWidth() - 5) x = pageSize.getWidth() - config.getStampWidth() - 5;
							if (y < 5) y = 5;
							if (y + config.getStampHeight() > pageSize.getHeight() - 5) y = pageSize.getHeight() - config.getStampHeight() - 5;
							PdfContentByte canvas = stamper.getOverContent(pageNum);
							canvas.setGState(createTransparencyState(config.getTransparency()));
							stampImage.setAbsolutePosition(x, y);
							canvas.addImage(stampImage);
							break;
						}
					}
				}
			} else {
				for (PdfTextLocationStrategy.TextLocationInfo locationInfo : foundLocations) {
					int pageNum = locationInfo.getPageNumber() + 1;
					PdfContentByte canvas = stamper.getOverContent(pageNum);
					canvas.setGState(createTransparencyState(config.getTransparency()));
					if (config.isReplaceKeyword()) {
						canvas.setColorFill(BaseColor.WHITE);
						canvas.rectangle(locationInfo.getX(), locationInfo.getY(), locationInfo.getWidth(), locationInfo.getHeight());
						canvas.fill();
					}
					float x = locationInfo.getCenterX() - config.getStampWidth() / 2;
					float y = locationInfo.getCenterY() - config.getStampHeight() / 2;
					Rectangle pageSize = reader.getPageSize(pageNum);
					if (x < 0) x = 5;
					if (x + config.getStampWidth() > pageSize.getWidth()) x = pageSize.getWidth() - config.getStampWidth() - 5;
					if (y < 0) y = 5;
					if (y + config.getStampHeight() > pageSize.getHeight()) y = pageSize.getHeight() - config.getStampHeight() - 5;
					stampImage.setAbsolutePosition(x, y);
					canvas.addImage(stampImage);
				}
			}
			logger.info("自动盖章完成，输出:{}", outputPath);
		} finally {
			if (stamper != null) stamper.close();
			if (reader != null) reader.close();
		}
	}

	private static Image loadStampImage(String imagePath) {
		try {
			File imageFile = new File(imagePath);
			if (imageFile.exists()) return Image.getInstance(imagePath);
			try (java.io.InputStream inputStream = PdfStampUtil.class.getClassLoader().getResourceAsStream(imagePath)) {
				if (inputStream != null) {
					byte[] bytes = new byte[inputStream.available()];
					inputStream.read(bytes);
					return Image.getInstance(bytes);
				}
			}
			return null;
		} catch (Exception e) {
			logger.error("加载印章图片失败: {}", e.getMessage());
			return null;
		}
	}

	private static com.itextpdf.text.pdf.PdfGState createTransparencyState(float transparency) {
		com.itextpdf.text.pdf.PdfGState gState = new com.itextpdf.text.pdf.PdfGState();
		gState.setFillOpacity(transparency);
		gState.setStrokeOpacity(transparency);
		return gState;
	}

	public static void main(String[] args) {
		logger.info("=== PDF盖章工具测试 ===");
		String testDir = System.getProperty("java.io.tmpdir");
		String inputPdf = testDir + "/test.pdf";
		String stampImage = testDir + "/stamp.png";
		try {
			if (new File(inputPdf).exists()) {
				String outputRiding = testDir + "/test_riding_stamp.pdf";
				RidingStampUtil.RidingStampConfig ridingConfig = new RidingStampUtil.RidingStampConfig(stampImage);
				ridingConfig.setStampWidth(60f);
				ridingConfig.setStampHeight(60f);
				ridingConfig.setTransparency(0.7f);
				RidingStampUtil.addRidingStamp(inputPdf, outputRiding, ridingConfig);
				logger.info("骑缝章测试完成: {}", outputRiding);
				String outputAuto = testDir + "/test_auto_stamp.pdf";
				List<String> keywords = Arrays.asList("（公章）", "（签章）");
				StampConfig autoConfig = new StampConfig(stampImage, keywords);
				addAutoStamp(inputPdf, outputAuto, autoConfig);
				logger.info("自动盖章测试完成: {}", outputAuto);
			} else {
				logger.warn("测试PDF文件不存在: {}", inputPdf);
			}
		} catch (Exception e) {
			logger.error("测试出错: {}", e.getMessage(), e);
		}
	}
}
