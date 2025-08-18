package com.zhaoxinms.cankao;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBookmark;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTMarkupRange;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * 参考示例：为 DOCX 文档按“第X章/条/节/款/附录/附件/章节标题格式”自动添加书签。
 *
 * 规则：
 * - 匹配常见中文合同标题与编号样式，例如：第1条、第1.2条、第一章、2.3 节、附件一、附录A 等。
 * - 默认将书签起止都落在匹配段落的开头（围住整段）。
 * - 书签命名规范化：bookmark_chapter_001、bookmark_article_001_002 等，避免重复。
 *
 * 使用方式：
 *   java -jar docx-bookmark-adder.jar "C:\\Users\\91088\\Desktop\\1.0.肇新合同系统源码销售合同（二次）.docx"
 * 若不传参数，则使用默认上面的路径。
 */
public class BookmarkAdder {

	private static final Pattern CHAPTER_PATTERN = Pattern.compile("^\\s*(第[一二三四五六七八九十百千]+章|第\\d+章|[A-Za-z]\\.\\s+.+|\\d+\\.\\s+.+)\\s*$");
	private static final Pattern ARTICLE_PATTERN = Pattern.compile("^\\s*(第[一二三四五六七八九十百千]+条|第\\d+(?:[\\.．]\\d+)*条|[（(]?\\d+[）)]|\\d+(?:[\\.．]\\d+)+)\\s*.*$");
	private static final Pattern SECTION_PATTERN = Pattern.compile("^\\s*(第[一二三四五六七八九十百千]+节|第\\d+(?:[\\.．]\\d+)*节)\\s*.*$");
	private static final Pattern APPENDIX_PATTERN = Pattern.compile("^\\s*(附录[一二三四五六七八九十百千A-Za-z0-9]+|附件[一二三四五六七八九十百千A-Za-z0-9]+)\\s*.*$");
	private static final Pattern CN_INDEX_PATTERN = Pattern.compile("^\\s*([一二三四五六七八九十百千]+、|（[一二三四五六七八九十百千]+）)\\s*.*$");

	public static void main(String[] args) throws Exception {
		String defaultPath = "C:\\Users\\91088\\Desktop\\1.0.肇新合同系统源码销售合同（二次）.docx";
		String inputPath = args != null && args.length > 0 && StringUtils.isNotBlank(args[0]) ? args[0] : defaultPath;

		Path in = Paths.get(inputPath);
		if (!Files.exists(in)) {
			System.err.println("文件不存在: " + inputPath);
			return;
		}
		String outputPath = buildOutputPath(in);

		int added;
		try (FileInputStream fis = new FileInputStream(in.toFile());
			 XWPFDocument document = new XWPFDocument(fis)) {

			added = addBookmarks(document);

			try (FileOutputStream fos = new FileOutputStream(outputPath)) {
				document.write(fos);
			}
		}

		System.out.println("已生成带书签的文件: " + outputPath + "，新增书签数量: " + added);
	}

	private static String buildOutputPath(Path input) {
		String fileName = input.getFileName().toString();
		int dot = fileName.lastIndexOf('.');
		String base = dot >= 0 ? fileName.substring(0, dot) : fileName;
		String ext = dot >= 0 ? fileName.substring(dot) : ".docx";
		File parent = input.toFile().getParentFile();
		return new File(parent, base + "-bookmarked" + ext).getAbsolutePath();
	}

	private static int addBookmarks(XWPFDocument document) {
		AtomicInteger bookmarkIdSeq = new AtomicInteger(1);
		Set<String> usedNames = new HashSet<>();
		AtomicInteger chapterSeq = new AtomicInteger(1);
		AtomicInteger articleSeq = new AtomicInteger(1);
		AtomicInteger sectionSeq = new AtomicInteger(1);
		AtomicInteger appendixSeq = new AtomicInteger(1);
		AtomicInteger addedCount = new AtomicInteger(0);

		for (XWPFParagraph paragraph : document.getParagraphs()) {
			String text = paragraph.getText();
			if (StringUtils.isBlank(text)) {
				continue;
			}
			String trimmed = text.trim();

			String type = null;
			String normName = null;

			if (CHAPTER_PATTERN.matcher(trimmed).matches()) {
				type = "chapter";
				normName = String.format("bookmark_chapter_%03d", chapterSeq.getAndIncrement());
			} else if (ARTICLE_PATTERN.matcher(trimmed).matches()) {
				type = "article";
				normName = String.format("bookmark_article_%03d", articleSeq.getAndIncrement());
			} else if (SECTION_PATTERN.matcher(trimmed).matches()) {
				type = "section";
				normName = String.format("bookmark_section_%03d", sectionSeq.getAndIncrement());
			} else if (APPENDIX_PATTERN.matcher(trimmed).matches()) {
				type = "appendix";
				normName = String.format("bookmark_appendix_%03d", appendixSeq.getAndIncrement());
			} else if (CN_INDEX_PATTERN.matcher(trimmed).matches()) {
				// 中文常见编号样式：一、 二、 （一） 等
				type = "article";
				normName = String.format("bookmark_article_%03d", articleSeq.getAndIncrement());
			}

			if (type == null) {
				continue;
			}

			String uniqueName = ensureUnique(normName, usedNames);
			ensureParagraphHasRun(paragraph);
			insertBookmarkAroundParagraph(paragraph, bookmarkIdSeq.getAndAdd(2), uniqueName);
			addedCount.incrementAndGet();
		}
		return addedCount.get();
	}

	private static String ensureUnique(String base, Set<String> used) {
		String name = base;
		int suffix = 1;
		while (used.contains(name)) {
			name = base + "_" + suffix;
			suffix++;
		}
		used.add(name);
		return name;
	}

	/**
	 * 在段落头部与尾部插入书签开始/结束标记。
	 */
	private static void insertBookmarkAroundParagraph(XWPFParagraph paragraph, int startId, String bookmarkName) {
		CTP ctp = paragraph.getCTP();

		CTBookmark bookmarkStart = ctp.addNewBookmarkStart();
		bookmarkStart.setId(BigInteger.valueOf(startId));
		bookmarkStart.setName(bookmarkName);

		CTMarkupRange bookmarkEnd = ctp.addNewBookmarkEnd();
		bookmarkEnd.setId(BigInteger.valueOf(startId));
	}

	private static void ensureParagraphHasRun(XWPFParagraph paragraph) {
		if (paragraph.getRuns() == null || paragraph.getRuns().isEmpty()) {
			XWPFRun run = paragraph.createRun();
			run.setText(" ");
		}
	}
}


