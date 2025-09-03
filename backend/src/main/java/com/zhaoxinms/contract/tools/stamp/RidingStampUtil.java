package com.zhaoxinms.contract.tools.stamp;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;

/**
 * 骑缝章工具类 - 分段盖章策略
 * 参考平阳项目实现
 */
public class RidingStampUtil {
	private static final Logger logger = LoggerFactory.getLogger(RidingStampUtil.class);

	private static final int MAX_PAGES_PER_STAMP = 80;
	private static final int STAMP_OVERLAP_PAGES = 10;

	public static class RidingStampConfig {
		private String stampImagePath;
		private float stampWidth = 80f;
		private float stampHeight = 80f;
		private float transparency = 0.8f;
		private boolean debugMode = false;
		private String debugOutputDir = null;
		private int marginTop = 0;
		private int marginBottom = 0;
		private int marginLeft = 0;
		private int marginRight = 0;
		private boolean highQualityMode = true;
		private float imageQuality = 1.0f;
		private boolean enhanceSmallFragments = true;
		private int smallFragmentThreshold = 3;
		private int stampCount = 1;

		public RidingStampConfig(String stampImagePath) { this.stampImagePath = stampImagePath; }
		public String getStampImagePath() { return stampImagePath; }
		public void setStampImagePath(String stampImagePath) { this.stampImagePath = stampImagePath; }
		public float getStampWidth() { return stampWidth; }
		public void setStampWidth(float stampWidth) { this.stampWidth = stampWidth; }
		public float getStampHeight() { return stampHeight; }
		public void setStampHeight(float stampHeight) { this.stampHeight = stampHeight; }
		public float getTransparency() { return transparency; }
		public void setTransparency(float transparency) { this.transparency = Math.max(0.0f, Math.min(1.0f, transparency)); }
		public boolean isDebugMode() { return debugMode; }
		public void setDebugMode(boolean debugMode) { this.debugMode = debugMode; }
		public String getDebugOutputDir() { return debugOutputDir; }
		public void setDebugOutputDir(String debugOutputDir) { this.debugOutputDir = debugOutputDir; }
		public int getMarginTop() { return marginTop; }
		public void setMarginTop(int marginTop) { this.marginTop = Math.max(0, marginTop); }
		public int getMarginBottom() { return marginBottom; }
		public void setMarginBottom(int marginBottom) { this.marginBottom = Math.max(0, marginBottom); }
		public int getMarginLeft() { return marginLeft; }
		public void setMarginLeft(int marginLeft) { this.marginLeft = Math.max(0, marginLeft); }
		public int getMarginRight() { return marginRight; }
		public void setMarginRight(int marginRight) { this.marginRight = Math.max(0, marginRight); }
		public int getStampCount() { return stampCount; }
		public void setStampCount(int stampCount) { this.stampCount = Math.max(1, stampCount); }
		public boolean isHighQualityMode() { return highQualityMode; }
		public void setHighQualityMode(boolean highQualityMode) { this.highQualityMode = highQualityMode; }
		public float getImageQuality() { return imageQuality; }
		public void setImageQuality(float imageQuality) { this.imageQuality = Math.max(0.0f, Math.min(1.0f, imageQuality)); }
		public boolean isEnhanceSmallFragments() { return enhanceSmallFragments; }
		public void setEnhanceSmallFragments(boolean enhanceSmallFragments) { this.enhanceSmallFragments = enhanceSmallFragments; }
		public int getSmallFragmentThreshold() { return smallFragmentThreshold; }
		public void setSmallFragmentThreshold(int smallFragmentThreshold) { this.smallFragmentThreshold = Math.max(1, smallFragmentThreshold); }
	}

	private static class StampSegment {
		private final int startPage;
		private final int endPage;
		private final int stampIndex;
		private final float yOffset;
		public StampSegment(int startPage, int endPage, int stampIndex, float yOffset) {
			this.startPage = startPage; this.endPage = endPage; this.stampIndex = stampIndex; this.yOffset = yOffset;
		}
		public int getStartPage() { return startPage; }
		public int getEndPage() { return endPage; }
		public int getStampIndex() { return stampIndex; }
		public float getYOffset() { return yOffset; }
		public int getPageCount() { return endPage - startPage + 1; }
	}

	public static void addRidingStamp(String inputPath, String outputPath, String stampImagePath) throws IOException, DocumentException {
		RidingStampConfig config = new RidingStampConfig(stampImagePath);
		addRidingStamp(inputPath, outputPath, config);
	}

	public static void addRidingStamp(String inputPath, String outputPath, RidingStampConfig config) throws IOException, DocumentException {
		PdfReader reader = null;
		PdfStamper stamper = null;
		try {
			reader = new PdfReader(inputPath);
			stamper = new PdfStamper(reader, new FileOutputStream(outputPath));
			BufferedImage stampImage = loadOriginalStampImage(config.getStampImagePath());
			if (stampImage == null) {
				throw new IllegalArgumentException("无法加载印章图片: " + config.getStampImagePath());
			}
			int totalPages = reader.getNumberOfPages();
			logger.info("开始添加骑缝章，总页数:{}", totalPages);
			List<StampSegment> segments = calculateStampSegments(totalPages, config.getStampCount(), config.getStampHeight());
			for (StampSegment segment : segments) {
				addSegmentRidingStamp(reader, stamper, stampImage, segment, config);
			}
			logger.info("骑缝章添加完成，输出文件:{}", outputPath);
		} finally {
			if (stamper != null) stamper.close();
			if (reader != null) reader.close();
		}
	}

	private static int calculateRequiredStampCount(int totalPages) {
		if (totalPages <= 0) return 1;
		int count = 1; int covered = MAX_PAGES_PER_STAMP;
		while (covered < totalPages) { count++; covered += (MAX_PAGES_PER_STAMP - STAMP_OVERLAP_PAGES); }
		return count;
	}

	private static List<StampSegment> calculateStampSegments(int totalPages, int stampCount, float stampHeight) {
		List<StampSegment> segments = new ArrayList<>();
		if (stampCount == 1 && totalPages > MAX_PAGES_PER_STAMP) {
			stampCount = calculateRequiredStampCount(totalPages);
			logger.info("自动调整印章数量:{}", stampCount);
		}
		if (stampCount == 1) {
			segments.add(new StampSegment(1, totalPages, 0, 0));
			return segments;
		}
		int currentStartPage = 1;
		for (int stampIndex = 0; stampIndex < stampCount; stampIndex++) {
			int segmentStartPage = currentStartPage;
			int segmentEndPage = (stampIndex == stampCount - 1) ? totalPages : Math.min(segmentStartPage + MAX_PAGES_PER_STAMP - 1, totalPages);
			float yOffset = -stampIndex * stampHeight;
			segments.add(new StampSegment(segmentStartPage, segmentEndPage, stampIndex, yOffset));
			if (stampIndex < stampCount - 1) currentStartPage = Math.max(1, segmentEndPage - STAMP_OVERLAP_PAGES + 1);
		}
		return segments;
	}

	private static void addSegmentRidingStamp(PdfReader reader, PdfStamper stamper, BufferedImage originalStampImage, StampSegment segment, RidingStampConfig config) {
		int pageCount = segment.getPageCount();
		int sourceWidth = originalStampImage.getWidth();
		int sourceHeight = originalStampImage.getHeight();
		int effectiveSourceX = config.getMarginLeft();
		int effectiveSourceY = config.getMarginTop();
		int effectiveSourceWidth = sourceWidth - config.getMarginLeft() - config.getMarginRight();
		int effectiveSourceHeight = sourceHeight - config.getMarginTop() - config.getMarginBottom();
		for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
			int pageNum = segment.getStartPage() + pageIndex;
			try {
				Rectangle pageSize = reader.getPageSize(pageNum);
				PdfContentByte canvas = stamper.getOverContent(pageNum);
				canvas.setGState(createTransparencyState(config.getTransparency()));
				int partSourceX, partSourceWidth;
				if (pageCount == 1) {
					partSourceX = effectiveSourceX;
					partSourceWidth = effectiveSourceWidth;
				} else {
					int baseWidthPerPage = effectiveSourceWidth / pageCount;
					int remainingPixels = effectiveSourceWidth - (baseWidthPerPage * pageCount);
					int firstPageExtraPixels = (remainingPixels + 1) / 2;
					int lastPageExtraPixels = remainingPixels / 2;
					if (pageIndex == 0) {
						partSourceX = effectiveSourceX;
						partSourceWidth = baseWidthPerPage + firstPageExtraPixels;
					} else if (pageIndex == pageCount - 1) {
						partSourceWidth = baseWidthPerPage + lastPageExtraPixels;
						partSourceX = effectiveSourceX + effectiveSourceWidth - partSourceWidth;
					} else {
						partSourceWidth = baseWidthPerPage;
						int firstPageWidth = baseWidthPerPage + firstPageExtraPixels;
						int middlePagesBefore = pageIndex - 1;
						partSourceX = effectiveSourceX + firstPageWidth + (middlePagesBefore * baseWidthPerPage);
					}
				}
				if (partSourceWidth <= 0) partSourceWidth = 1;
				if (partSourceX + partSourceWidth > effectiveSourceX + effectiveSourceWidth) {
					partSourceWidth = (effectiveSourceX + effectiveSourceWidth) - partSourceX;
					if (partSourceWidth <= 0) { partSourceWidth = 1; partSourceX = effectiveSourceX + effectiveSourceWidth - 1; }
				}
				if (partSourceX < effectiveSourceX) partSourceX = effectiveSourceX;
				BufferedImage rawPart = originalStampImage.getSubimage(partSourceX, effectiveSourceY, partSourceWidth, effectiveSourceHeight);
				File tempFile = File.createTempFile("riding_part_", ".png");
				ImageIO.write(rawPart, "PNG", tempFile);
				Image partImage = Image.getInstance(tempFile.getAbsolutePath());
				float pdfDisplayWidth = config.getStampWidth() * ((float)partSourceWidth / (float)effectiveSourceWidth);
				float pdfDisplayHeight = config.getStampHeight();
				partImage.scaleAbsolute(pdfDisplayWidth, pdfDisplayHeight);
				float x = pageSize.getWidth() - pdfDisplayWidth;
				float y = pageSize.getHeight() / 2 - pdfDisplayHeight / 2 + segment.getYOffset();
				partImage.setAbsolutePosition(x, y);
				canvas.addImage(partImage);
				tempFile.delete();
			} catch (Exception e) {
				logger.error("为第 {} 页添加骑缝章失败: {}", pageNum, e.getMessage());
			}
		}
	}

	private static Image convertToHighQualityImage(BufferedImage bufferedImage, RidingStampConfig config) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			if (config.isHighQualityMode()) {
				javax.imageio.ImageWriter writer = null;
				javax.imageio.stream.ImageOutputStream ios = null;
				try {
					java.util.Iterator<javax.imageio.ImageWriter> writers = javax.imageio.ImageIO.getImageWritersByFormatName("png");
					if (!writers.hasNext()) throw new RuntimeException("没有找到PNG图片编码器");
					writer = writers.next();
					ios = javax.imageio.ImageIO.createImageOutputStream(baos);
					writer.setOutput(ios);
					javax.imageio.ImageWriteParam param = writer.getDefaultWriteParam();
					if (param.canWriteCompressed()) {
						param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
						param.setCompressionQuality(config.getImageQuality());
					}
					writer.write(null, new javax.imageio.IIOImage(bufferedImage, null, null), param);
				} finally {
					if (writer != null) writer.dispose();
					if (ios != null) ios.close();
				}
			} else {
				ImageIO.write(bufferedImage, "PNG", baos);
			}
			byte[] imageBytes = baos.toByteArray();
			Image image = Image.getInstance(imageBytes);
			image.setInterpolation(true);
			return image;
		} finally {
			baos.close();
		}
	}

	private static BufferedImage loadOriginalStampImage(String imagePath) {
		try {
			return ImageIO.read(new File(imagePath));
		} catch (IOException e) {
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

	private static BufferedImage scaleImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
		if (originalImage == null) return null;
		BufferedImage scaledImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = scaledImage.createGraphics();
		try {
			g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			g2d.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, java.awt.RenderingHints.VALUE_RENDER_QUALITY);
			g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
			g2d.setRenderingHint(java.awt.RenderingHints.KEY_COLOR_RENDERING, java.awt.RenderingHints.VALUE_COLOR_RENDER_QUALITY);
			g2d.setRenderingHint(java.awt.RenderingHints.KEY_DITHERING, java.awt.RenderingHints.VALUE_DITHER_ENABLE);
			g2d.setRenderingHint(java.awt.RenderingHints.KEY_FRACTIONALMETRICS, java.awt.RenderingHints.VALUE_FRACTIONALMETRICS_ON);
			g2d.setRenderingHint(java.awt.RenderingHints.KEY_ALPHA_INTERPOLATION, java.awt.RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
			g2d.setRenderingHint(java.awt.RenderingHints.KEY_STROKE_CONTROL, java.awt.RenderingHints.VALUE_STROKE_PURE);
			g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
		} finally {
			g2d.dispose();
		}
		return scaledImage;
	}
}
