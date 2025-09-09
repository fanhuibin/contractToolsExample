package com.zhaoxinms.contract.tools.ocr.dotsocr;

import com.alibaba.druid.util.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.difflib.text.DiffRow;
import com.github.difflib.text.DiffRowGenerator;
import com.zhaoxinms.contract.tools.compare.DiffUtil;
import com.zhaoxinms.contract.tools.ocr.DiffProcessingUtil;
import com.zhaoxinms.contract.tools.ocr.TextExtractionUtil;
import com.zhaoxinms.contract.tools.ocr.TextExtractionUtil.PageLayout;
import com.zhaoxinms.contract.tools.ocr.model.CharBox;
import com.zhaoxinms.contract.tools.ocr.model.DiffBlock;
import com.zhaoxinms.contract.tools.ocr.model.DiffBlock.DiffType;
import com.zhaoxinms.contract.tools.compare.util.TextNormalizer;
import okhttp3.OkHttpClient;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationTextMarkup;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.LinkedList;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Dots.OCR 对比演示： 1) 分别识别两份PDF（并发页级识别，提取文字与bbox/category） 2)
 * 将整份PDF展平为字符序列（每个字符携带page/bbox/category） 3) 使用 java-diff-utils
 * 做差异，按bbox分裂或合并差异块，输出 修改/新增/删除
 */
public class DotsOcrCompareDemoTest {

    private static final ObjectMapper M = new ObjectMapper();

    private OkHttpClient httpClient() {
		return new OkHttpClient.Builder().connectTimeout(Duration.ofMinutes(5)).readTimeout(Duration.ofMinutes(5))
				.writeTimeout(Duration.ofMinutes(5)).build();
    }

    private DotsOcrClient newClient(String baseUrl) {
		return DotsOcrClient.builder().baseUrl(baseUrl).defaultModel("model").httpClient(httpClient()).build();
    }

    @Test
    public void compareTwoPdfs_Demo() throws Exception {
        boolean resumeFromStep4 = true;
        String baseUrl = "http://192.168.0.100:8000";
        Path fileA = Paths.get("C:\\Users\\范慧斌\\Desktop\\hetong比对前端\\test1.pdf");
        Path fileB = Paths.get("C:\\Users\\范慧斌\\Desktop\\hetong比对前端\\test2.pdf");

		String prompt = "Please output the layout information from the PDF image, including each layout element's bbox, its category, and the corresponding text content within the bbox.\n\n"
				+ "1. Bbox format: [x1, y1, x2, y2]\n\n"
				+ "2. Layout Categories: The possible categories are ['Caption', 'Footnote', 'Formula', 'List-item', 'Page-footer', 'Page-header', 'Picture', 'Section-header', 'Table', 'Text', 'Title'].\n\n"
				+ "3. Text Extraction & Formatting Rules:\n"
				+ "    - Picture: For the 'Picture' category, the text field should be omitted.\n"
				+ "    - Formula: Format its text as LaTeX.\n" + "    - Table: Format its text as HTML.\n"
				+ "    - All Others (Text, Title, etc.): Format their text as Plain text.\n\n" + "4. Constraints:\n"
				+ "    - The output text must be the original text from the image, with no translation.\n"
				+ "    - All layout elements must be sorted according to human reading order.\n\n"
				+ "5. Final Output: The entire output must be a single JSON object.";

        DotsOcrClient client = newClient(baseUrl);

        long t0 = System.currentTimeMillis();
        List<CharBox> seqA = recognizePdfAsCharSeq(client, fileA, prompt, resumeFromStep4);
        List<CharBox> seqB = recognizePdfAsCharSeq(client, fileB, prompt, resumeFromStep4);
        long t1 = System.currentTimeMillis();
		System.out.println(String.format(Locale.ROOT, "OCR done. A=%d chars, B=%d chars, cost=%d ms", seqA.size(),
				seqB.size(), (t1 - t0)));

        // 3) diff（引入 TextNormalizer 忽略常见标点识别差异，保证 1:1 字符映射）
        // 保持原有的行结构，按行进行diff比对
        String normA = TextNormalizer.normalizePunctuation(joinWithLineBreaks(seqA));
        String normB = TextNormalizer.normalizePunctuation(joinWithLineBreaks(seqB));
        // 使用专业 Markdown 处理，按需求仅清理行首型语法（保留行内，仅保留 ** 粗体替换为空格）
		normA = normA.replace('$', ' ').replace('_', ' ');
		normB = normB.replace('$', ' ').replace('_', ' ');

        DiffUtil dmp = new DiffUtil();
        dmp.Diff_EditCost = 8; // Use Efficiency Cleanup with edit cost 10
        LinkedList<DiffUtil.Diff> diffs = dmp.diff_main(normA, normB);
        dmp.diff_cleanupEfficiency(diffs);

		// 输出diffs内容
		System.out.println("=== diff_main 和 diff_cleanupEfficiency 后的 diffs 内容 ===");
		for (int i = 0; i < diffs.size(); i++) {
			DiffUtil.Diff d = diffs.get(i);
			System.out.println(String.format("Diff[%d]: 操作=%s, 文本='%s'", i, d.operation.name(), d.text));
		}
		System.out.println("=== diffs 输出完成 ===\n");
     
        List<DiffBlock> rawBlocks = DiffProcessingUtil.splitDiffsByBounding(diffs, seqA, seqB);

		// 在索引计算完成后应用自定义过滤，过滤掉不需要标记的差异
		List<DiffBlock> filteredBlocks = DiffProcessingUtil.filterIgnoredDiffBlocks(rawBlocks, seqA, seqB);

		System.out.println("=== DiffBlock 详情 ===");

		for (int i = 0; i < filteredBlocks.size(); i++) {
			DiffBlock block = filteredBlocks.get(i);
			
//			if(!block.type.equals(DiffType.ADDED)) {
//				continue;
//			}
			
			// 显示bbox信息
			String bboxInfo = "";
			int totalBboxes = 0;

			if (block.oldBboxes != null && !block.oldBboxes.isEmpty()) {
				totalBboxes += block.oldBboxes.size();
				if (block.oldBboxes.size() == 1) {
					double[] bbox = block.oldBboxes.get(0);
					bboxInfo += String.format("old:[%.2f,%.2f,%.2f,%.2f]", bbox[0], bbox[1], bbox[2], bbox[3]);
				} else {
					bboxInfo += String.format("old:%d个", block.oldBboxes.size());
				}
			}

			if (block.newBboxes != null && !block.newBboxes.isEmpty()) {
				if (!bboxInfo.isEmpty())
					bboxInfo += ", ";
				totalBboxes += block.newBboxes.size();
				if (block.newBboxes.size() == 1) {
					double[] bbox = block.newBboxes.get(0);
					bboxInfo += String.format("new:[%.2f,%.2f,%.2f,%.2f]", bbox[0], bbox[1], bbox[2], bbox[3]);
				} else {
					bboxInfo += String.format("new:%d个", block.newBboxes.size());
				}
			}

			if (bboxInfo.isEmpty()) {
				bboxInfo = "无bbox信息";
			}

			System.out.println(String.format("Block[%d]: 页码=%d, 类型=%s, %s",
				i, block.page, block.type.name(), bboxInfo));
			System.out.println(String.format("  旧文本: '%s'", block.oldText));
			System.out.println(String.format("  新文本: '%s'", block.newText));

			// 输出文本起始索引信息
			String indexInfo = "";
			if (block.textStartIndexA >= 0) {
				indexInfo += String.format("文本起始索引A: %d", block.textStartIndexA);
			} else {
				indexInfo += "文本起始索引A: N/A";
			}

			if (block.textStartIndexB >= 0) {
				indexInfo += String.format(", 文本起始索引B: %d", block.textStartIndexB);
			} else {
				indexInfo += ", 文本起始索引B: N/A";
			}

			System.out.println(String.format("  %s", indexInfo));

			// 显示完整文本信息
			if (block.allTextA != null && !block.allTextA.isEmpty()) {
				if (block.allTextA.size() == 1) {
					System.out.println(String.format("  完整文本A: '%s'", block.allTextA.get(0)));
				} else {
					System.out.println("  完整文本A列表:");
					for (int j = 0; j < block.allTextA.size(); j++) {
						System.out.println(String.format("    A[%d]: '%s'", j, block.allTextA.get(j)));
					}
				}
			}

			if (block.allTextB != null && !block.allTextB.isEmpty()) {
				if (block.allTextB.size() == 1) {
					System.out.println(String.format("  完整文本B: '%s'", block.allTextB.get(0)));
				} else {
					System.out.println("  完整文本B列表:");
					for (int j = 0; j < block.allTextB.size(); j++) {
						System.out.println(String.format("    B[%d]: '%s'", j, block.allTextB.get(j)));
					}
				}
			}

			// 如果有多个bbox，显示详细信息
			if (totalBboxes > 0) {
				if (block.oldBboxes != null && block.oldBboxes.size() > 0) {
					System.out.println("  oldBboxes:");
					for (int j = 0; j < block.oldBboxes.size(); j++) {
						double[] bbox = block.oldBboxes.get(j);
						System.out.println(String.format("    old[%d]: [%.2f,%.2f,%.2f,%.2f]", j, bbox[0], bbox[1],
								bbox[2], bbox[3]));
					}
				}

				if (block.newBboxes != null && block.newBboxes.size() > 0) {
					System.out.println("  newBboxes:");
					for (int j = 0; j < block.newBboxes.size(); j++) {
						double[] bbox = block.newBboxes.get(j);
						System.out.println(String.format("    new[%d]: [%.2f,%.2f,%.2f,%.2f]", j, bbox[0], bbox[1],
								bbox[2], bbox[3]));
					}
				}
			}

			// 输出原始Diff对象信息
			if (block.originalDiff != null) {
				System.out.println(String.format("  原始Diff: 操作=%s, 文本='%s'",
					block.originalDiff.operation.name(), block.originalDiff.text.replaceAll("\n", "\\\\n")));
			} else {
				System.out.println("  原始Diff: 无");
			}

			System.out.println();

		}

        // 合并同bbox内相邻或重叠的块，并判定类型
		List<DiffBlock> merged = mergeBlocksByBbox(filteredBlocks);

		// 输出合并统计
		System.out.println("合并统计: " + filteredBlocks.size() + " -> " + merged.size() + " (合并了 "
				+ (filteredBlocks.size() - merged.size()) + " 个块)");

        // 输出并保存
        ObjectNode out = M.createObjectNode();
        out.put("fileA", fileA.toAbsolutePath().toString());
        out.put("fileB", fileB.toAbsolutePath().toString());
        ArrayNode arr = M.createArrayNode();
		for (DiffBlock b : merged)
			arr.add(b.toJson(M.createObjectNode()));
        out.set("diffs", arr);

        String pretty = M.writerWithDefaultPrettyPrinter().writeValueAsString(out);
		String outPath = System.getProperty("dotsocr.compare.out",
				fileA.toAbsolutePath().toString() + "__VS__" + fileB.getFileName().toString() + ".compare.json");
        Files.write(Path.of(outPath), pretty.getBytes(StandardCharsets.UTF_8));
        System.out.println("Compare result saved: " + outPath);

        // ===== 将比对结果映射为坐标并标注到PDF上 =====
        // 1) 为 normA/normB 构建索引映射（规范化文本位置 → 原始 CharBox 索引）
        IndexMap mapA = buildNormalizedIndexMap(seqA);
        IndexMap mapB = buildNormalizedIndexMap(seqB);

        // 2) 收集每个 diff 对应的一组矩形（可能跨多个 bbox）
		List<RectOnPage> rectsA = collectRectsForDiffBlocks(merged, mapA, seqA, true);
		List<RectOnPage> rectsB = collectRectsForDiffBlocks(merged, mapB, seqB, false);

        // 3) 渲染每页图像以获取像素尺寸，用于像素→PDF坐标换算
        int dpi = newClient(baseUrl).getRenderDpi();
        PageImageSizeProvider sizeA = renderPageSizes(fileA, dpi);
        PageImageSizeProvider sizeB = renderPageSizes(fileB, dpi);

        // 4) 标注并输出PDF
        String outPdfA = fileA.toAbsolutePath().toString() + ".annotated.pdf";
        String outPdfB = fileB.toAbsolutePath().toString() + ".annotated.pdf";
        System.out.println("Start annotation. rectsA=" + rectsA.size() + ", rectsB=" + rectsB.size());
        try {
            annotatePDF(fileA, outPdfA, rectsA, sizeA);
            System.out.println("Annotated A saved: " + outPdfA);
        } catch (Exception ex) {
            System.err.println("Annotate A failed: " + ex.getMessage());
        }
        try {
            annotatePDF(fileB, outPdfB, rectsB, sizeB);
            System.out.println("Annotated B saved: " + outPdfB);
        } catch (Exception ex) {
            System.err.println("Annotate B failed: " + ex.getMessage());
        }

        // 将标注PDF路径也写入对比结果JSON，便于外部使用
        try {
            ((ObjectNode) out).put("annotatedA", outPdfA);
            ((ObjectNode) out).put("annotatedB", outPdfB);
            Files.write(Path.of(outPath), M.writerWithDefaultPrettyPrinter().writeValueAsBytes(out));
            System.out.println("Compare result (with annotated paths) saved: " + outPath);
        } catch (Exception e) {
            System.err.println("Failed to append annotated paths to result JSON: " + e.getMessage());
        }

        assertNotNull(merged);
    }

    // ---------- OCR Helpers ----------

	private List<CharBox> recognizePdfAsCharSeq(DotsOcrClient client, Path pdf, String prompt, boolean resumeFromStep4)
			throws Exception {
		TextExtractionUtil.PageLayout[] ordered;
        if (resumeFromStep4) {
            // Step 1 (count pages) + Step 2 skipped; load Step 3 results (saved JSON)
            int total = countPdfPages(pdf);
			ordered = new TextExtractionUtil.PageLayout[total];
            for (int i = 0; i < total; i++) {
                final int pageNo = i + 1;
				TextExtractionUtil.PageLayout p = parseOnePageFromSavedJson(pdf, pageNo);
                ordered[pageNo - 1] = p;
            }
        } else {
            // Step 1: render PDF to images
            List<byte[]> pages = renderAllPagesToPng(client, pdf);
            int total = pages.size();
            int parallel = Math.max(1, Integer.parseInt(System.getProperty("dotsocr.parallel", "4")));
			java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors
					.newFixedThreadPool(Math.min(parallel, total));
			java.util.concurrent.ExecutorCompletionService<TextExtractionUtil.PageLayout> ecs = new java.util.concurrent.ExecutorCompletionService<>(
					pool);
            for (int i = 0; i < total; i++) {
                final int pageNo = i + 1;
                final byte[] img = pages.get(i);
                ecs.submit(() -> parseOnePage(client, img, pageNo, prompt, pdf));
            }
			ordered = new TextExtractionUtil.PageLayout[total];
            for (int i = 0; i < total; i++) {
				TextExtractionUtil.PageLayout p = ecs.take().get();
                ordered[p.page - 1] = p;
            }
            pool.shutdownNow();
        }

		// 使用新的按顺序读取方法解析文本和位置
		List<CharBox> out = parseTextAndPositionsFromResults(ordered);

        // Step 3: 保存提取的纯文本（含/不含页标记），便于开发调试
        try {
			String extractedWithPages = TextExtractionUtil.extractTextWithPageMarkers(out);
			String extractedNoPages = TextExtractionUtil.extractText(out);

            String txtOut = pdf.toAbsolutePath().toString() + ".extracted.txt";
            String txtOutCompare = pdf.toAbsolutePath().toString() + ".extracted.compare.txt";

			Files.write(Path.of(txtOut), extractedWithPages.getBytes(StandardCharsets.UTF_8));
			Files.write(Path.of(txtOutCompare), extractedNoPages.getBytes(StandardCharsets.UTF_8));

            System.out.println("Extracted text saved: " + txtOut);
            System.out.println("Extracted text (no page markers) saved: " + txtOutCompare);
        } catch (Exception e) {
            System.err.println("Failed to write extracted text: " + e.getMessage());
        }
        return out;
    }

	private PageLayout parseOnePage(DotsOcrClient client, byte[] pngBytes, int page, String prompt, Path pdfPath)
			throws Exception {
        String raw = client.ocrImageBytes(pngBytes, prompt, null, "image/png", false);
        JsonNode env = M.readTree(raw);
        String content = env.path("choices").path(0).path("message").path("content").asText("");
		if (content == null || content.isBlank())
			throw new RuntimeException("模型未返回内容(page=" + page + ")");
        JsonNode root = M.readTree(content);
        // 保存每页识别的 JSON 结果，便于后续从第4步直接开始
        try {
            String pageJsonPath = pdfPath.toAbsolutePath().toString() + ".page-" + page + ".ocr.json";
            Files.write(Path.of(pageJsonPath), M.writerWithDefaultPrettyPrinter().writeValueAsBytes(root));
            System.out.println("Saved OCR JSON: " + pageJsonPath);
        } catch (Exception e) {
            System.err.println("Failed to save OCR JSON for page " + page + ": " + e.getMessage());
        }
		List<TextExtractionUtil.LayoutItem> items = extractLayoutItems(root);
		return new TextExtractionUtil.PageLayout(page, items);
    }


    // 从已保存的 JSON 结果加载单页版面（便于直接从第4步开始调试）
	private TextExtractionUtil.PageLayout parseOnePageFromSavedJson(Path pdfPath, int page) throws Exception {
        String pageJsonPath = pdfPath.toAbsolutePath().toString() + ".page-" + page + ".ocr.json";
        byte[] bytes = Files.readAllBytes(Path.of(pageJsonPath));
        JsonNode root = M.readTree(bytes);
		List<TextExtractionUtil.LayoutItem> items = extractLayoutItems(root);
		return new TextExtractionUtil.PageLayout(page, items);
    }

    // 仅统计 PDF 页数（用于 resumeFromStep4）
    private static int countPdfPages(Path pdfPath) throws Exception {
        try (PDDocument doc = PDDocument.load(pdfPath.toFile())) {
            return doc.getNumberOfPages();
        }
    }

    private static List<byte[]> renderAllPagesToPng(DotsOcrClient client, Path pdfPath) throws Exception {
        int dpi = client.getRenderDpi();
        boolean saveImages = client.isSaveRenderedImages();
        try (PDDocument doc = PDDocument.load(pdfPath.toFile())) {
            PDFRenderer renderer = new PDFRenderer(doc);
            List<byte[]> list = new ArrayList<>();
            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, dpi);
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    ImageIO.write(image, "png", baos);
                    byte[] bytes = baos.toByteArray();
                    list.add(bytes);
                    if (saveImages) {
						Path out = pdfPath.getParent()
								.resolve(pdfPath.getFileName().toString() + ".page-" + (i + 1) + ".png");
                        Files.write(out, bytes);
                    }
                }
            }
            return list;
        }
    }

	// 使用TextExtractionUtil解析JSON
	private List<TextExtractionUtil.LayoutItem> extractLayoutItems(JsonNode root) {
		return TextExtractionUtil.extractLayoutItems(root);
	}

	// ---------- 文本解析方法（使用工具类）----------

	/**
	 * 从PDF识别结果中解析文本和位置信息（默认按顺序读取）
	 */
	private List<CharBox> parseTextAndPositionsFromResults(TextExtractionUtil.PageLayout[] ordered) {
		return TextExtractionUtil.parseTextAndPositionsFromResults(ordered);
	}

    private List<DiffBlock> mergeBlocksByBbox(List<DiffBlock> blocks) {
		if (blocks.isEmpty())
			return blocks;

		List<DiffBlock> merged = new ArrayList<>();

		for (DiffBlock current : blocks) {
			// 跳过被忽略的差异
			if (current.type == DiffBlock.DiffType.IGNORED) {
				continue;
			}

			// 查找是否可以与前一个块合并
			boolean mergedWithPrevious = false;

			if (!merged.isEmpty()) {
				DiffBlock last = merged.get(merged.size() - 1);

				// 检查是否可以合并：相同页面、相同类型、bbox相邻或重叠
				if (last.page == current.page && last.type == current.type
						&& areDiffBlocksAdjacentOrOverlapping(last, current)) {

					// 合并文本内容
					last.oldText += current.oldText;
					last.newText += current.newText;

					// 合并bbox列表
					// 合并oldBboxes
					if (last.oldBboxes == null) {
						last.oldBboxes = new ArrayList<>();
					}
					if (current.oldBboxes != null) {
						last.oldBboxes.addAll(current.oldBboxes);
					}

					// 合并newBboxes
					if (last.newBboxes == null) {
						last.newBboxes = new ArrayList<>();
					}
					if (current.newBboxes != null) {
						last.newBboxes.addAll(current.newBboxes);
					}

					// 合并完整文本列表
					if (last.allTextA == null) {
						last.allTextA = new ArrayList<>();
					}
					if (current.allTextA != null) {
						last.allTextA.addAll(current.allTextA);
					}

					if (last.allTextB == null) {
						last.allTextB = new ArrayList<>();
					}
					if (current.allTextB != null) {
						last.allTextB.addAll(current.allTextB);
					}

					mergedWithPrevious = true;
				}
			}

			// 如果没有与前一个块合并，则添加为新块
			if (!mergedWithPrevious) {
				merged.add(current);
			}
		}

		System.out.println("合并前 blocks 数量: " + blocks.size());
		System.out.println("合并后 merged 数量: " + merged.size());
		System.out.println("实际合并的块数: " + (blocks.size() - merged.size()));

		return merged;
	}

	/**
	 * 检查两个DiffBlock是否相邻或重叠
	 */
	private boolean areDiffBlocksAdjacentOrOverlapping(DiffBlock block1, DiffBlock block2) {
		// 获取所有bbox
		List<double[]> bboxes1 = block1.getAllBboxes();
		List<double[]> bboxes2 = block2.getAllBboxes();

		if (bboxes1.isEmpty() || bboxes2.isEmpty()) {
			return false;
		}

		// 检查两个block的所有bbox是否有任意一对相邻或重叠
		for (double[] bbox1 : bboxes1) {
			for (double[] bbox2 : bboxes2) {
				if (areBboxesAdjacentOrOverlapping(bbox1, bbox2)) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * 检查两个bbox是否相邻或重叠
	 */
	private boolean areBboxesAdjacentOrOverlapping(double[] bbox1, double[] bbox2) {
		// 简化的相邻检查：如果两个bbox在垂直方向上接近（y坐标相差不大）
		// 或者在水平方向上接近（x坐标相差不大），则认为是连续的

		double x1_min = bbox1[0], y1_min = bbox1[1], x1_max = bbox1[2], y1_max = bbox1[3];
		double x2_min = bbox2[0], y2_min = bbox2[1], x2_max = bbox2[2], y2_max = bbox2[3];

		// 检查垂直重叠或相邻
		boolean verticalOverlap = (y1_min <= y2_max && y2_min <= y1_max);
		// 检查水平相邻（x坐标接近）
		boolean horizontalAdjacent = Math.abs(x1_max - x2_min) < 50 || Math.abs(x2_max - x1_min) < 50;

		// 检查水平重叠或相邻
		boolean horizontalOverlap = (x1_min <= x2_max && x2_min <= x1_max);
		// 检查垂直相邻（y坐标接近）
		boolean verticalAdjacent = Math.abs(y1_max - y2_min) < 20 || Math.abs(y2_max - y1_min) < 20;

		return (verticalOverlap && horizontalAdjacent) || (horizontalOverlap && verticalAdjacent);
	}

	

    private static String key(int page, double[] box) {
		if (box == null || box.length < 4) {
			throw new IllegalArgumentException("Invalid bbox: " + (box == null ? "null" : "length=" + box.length));
		}
        return page + "|" + String.format(Locale.ROOT, "%.3f,%.3f,%.3f,%.3f", box[0], box[1], box[2], box[3]);
    }

    // 保持按 bbox 的行结构：当检测到 bbox 变化时追加换行；最后一个 bbox 结束也补一个换行
    private static String joinWithLineBreaks(List<CharBox> cs) {
		if (cs.isEmpty())
			return "";
        
        StringBuilder sb = new StringBuilder();
        String lastKey = null;
        
        for (CharBox c : cs) {
			if (c.bbox != null) {
            String currentKey = key(c.page, c.bbox);
            
            // 如果切换到新的page+bbox，添加换行符
            if (lastKey != null && !lastKey.equals(currentKey)) {
                sb.append('\n');
            }
            
            sb.append(c.ch);
            lastKey = currentKey;
			}
        }
        // 收尾：为最后一个 bbox 结束补一个换行
        sb.append('\n');
        return sb.toString();
    }

    // ---------- 坐标映射与PDF标注 ----------

    private static class IndexMap {
        final String normalized; // 与 diff 使用的同构文本（仅做 $/_ → 空格 与 标点归一）
		final int[] seqIndex; // normalized 中每个字符位置对应的 CharBox 索引；无对应时为 -1（如换行）

		IndexMap(String normalized, int[] seqIndex) {
			this.normalized = normalized;
			this.seqIndex = seqIndex;
		}
    }

    private static IndexMap buildNormalizedIndexMap(List<CharBox> seq) {
        // 构建与 joinWithLineBreaks 一致的基础串，同时记录每个字符对应的 CharBox 索引
        StringBuilder base = new StringBuilder();
        List<Integer> idxMap = new ArrayList<>();
        String lastKey = null;
        for (int i = 0; i < seq.size(); i++) {
            CharBox c = seq.get(i);
			if (c.bbox != null) {
            String currentKey = key(c.page, c.bbox);
            if (lastKey != null && !lastKey.equals(currentKey)) {
                base.append('\n');
                idxMap.add(-1);
            }
            base.append(c.ch);
            idxMap.add(i);
            lastKey = currentKey;
			}
        }
        // 收尾换行
        base.append('\n');
        idxMap.add(-1);

		String norm = TextNormalizer.normalizePunctuation(base.toString()).replace('$', ' ').replace('_', ' ');
        // 规范化步骤不改变长度的假设（标点归一/替换为空格）。若未来改变长度，此映射将失配。
        int[] map = new int[idxMap.size()];
		for (int i = 0; i < idxMap.size(); i++)
			map[i] = idxMap.get(i);
        return new IndexMap(norm, map);
    }

    private static class RectOnPage {
        final int pageIndex0; // 0-based
		final double[] bbox; // [x1,y1,x2,y2] 图像像素坐标
        final DiffUtil.Operation op; // INSERT/DELETE/MODIFIED 用于着色

		RectOnPage(int pageIndex0, double[] bbox, DiffUtil.Operation op) {
			this.pageIndex0 = pageIndex0;
			this.bbox = bbox;
			this.op = op;
		}
	}

	private static List<RectOnPage> collectRectsForDiffBlocks(List<DiffBlock> blocks, IndexMap map, List<CharBox> seq,
			boolean isLeft) {
        List<RectOnPage> out = new ArrayList<>();

		for (DiffBlock block : blocks) {
			// 跳过被忽略的差异，不为它们生成标记
			if (block.type == DiffBlock.DiffType.IGNORED) {
                continue;
            }

			// 根据block类型决定是否处理本侧
			DiffUtil.Operation op = null;
			if (block.type == DiffBlock.DiffType.DELETED && isLeft) {
				op = DiffUtil.Operation.DELETE;
			} else if (block.type == DiffBlock.DiffType.ADDED && !isLeft) {
				op = DiffUtil.Operation.INSERT;
			}

			if (op == null)
				continue; // 跳过不需要在本侧标记的块

			// 根据操作类型选择要处理的bbox
			List<double[]> bboxesToProcess = new ArrayList<>();
			if (block.type == DiffBlock.DiffType.DELETED && isLeft && block.oldBboxes != null) {
				// DELETE操作且是左侧文档：处理oldBboxes
				bboxesToProcess.addAll(block.oldBboxes);
			} else if (block.type == DiffBlock.DiffType.ADDED && !isLeft && block.newBboxes != null) {
				// INSERT操作且是右侧文档：处理newBboxes
				bboxesToProcess.addAll(block.newBboxes);
			}

			if (bboxesToProcess.isEmpty()) {
				continue; // 没有需要处理的bbox，跳过
			}

			// 为每个bbox生成标记
			for (double[] bbox : bboxesToProcess) {
				String blockKey = key(block.page, bbox);

				// 在seq中找到所有属于这个bbox的CharBox
            Map<String, List<Integer>> byBox = new LinkedHashMap<>();
				for (int i = 0; i < seq.size(); i++) {
					CharBox c = seq.get(i);
					if (c.bbox != null) {
                String k = key(c.page, c.bbox);
						if (k.equals(blockKey)) {
							byBox.computeIfAbsent(k, kk -> new ArrayList<>()).add(i);
            }
					}
				}

				// 为每个找到的box生成矩形
            for (Map.Entry<String, List<Integer>> e : byBox.entrySet()) {
                List<Integer> idxs = e.getValue();
					if (idxs.isEmpty())
						continue;
                CharBox any = seq.get(idxs.get(0));
					// 使用该布局项的bbox（像素坐标系）
					out.add(new RectOnPage(any.page - 1, any.bbox, op));
            }
        }
		}

        return out;
    }

    private static class PageImageSizeProvider {
        final int pageCount;
        final int[] widths;
        final int[] heights;

		PageImageSizeProvider(int pageCount, int[] widths, int[] heights) {
			this.pageCount = pageCount;
			this.widths = widths;
			this.heights = heights;
		}
    }

    private static PageImageSizeProvider renderPageSizes(Path pdf, int dpi) throws Exception {
        try (PDDocument doc = PDDocument.load(pdf.toFile())) {
            PDFRenderer r = new PDFRenderer(doc);
            int n = doc.getNumberOfPages();
            int[] ws = new int[n];
            int[] hs = new int[n];
            for (int i = 0; i < n; i++) {
                BufferedImage img = r.renderImageWithDPI(i, dpi);
                ws[i] = img.getWidth();
                hs[i] = img.getHeight();
            }
            return new PageImageSizeProvider(n, ws, hs);
        }
    }

	private static void annotatePDF(Path sourcePdf, String outPath, List<RectOnPage> rects, PageImageSizeProvider sizes)
			throws Exception {
        try (PDDocument doc = PDDocument.load(sourcePdf.toFile())) {
            for (RectOnPage rp : rects) {
                int p = Math.max(0, Math.min(rp.pageIndex0, doc.getNumberOfPages() - 1));
                PDPage page = doc.getPage(p);
                PDRectangle mediaBox = page.getMediaBox();
                float pageW = mediaBox.getWidth();
                float pageH = mediaBox.getHeight();

                int imgW = (sizes != null && p < sizes.pageCount) ? sizes.widths[p] : (int) pageW;
                int imgH = (sizes != null && p < sizes.pageCount) ? sizes.heights[p] : (int) pageH;
                float scaleX = pageW / Math.max(1f, imgW);
                float scaleY = pageH / Math.max(1f, imgH);

                double[] b = rp.bbox; // [x1,y1,x2,y2] 顶点像素坐标（左上为原点）
                float x1 = (float) b[0] * scaleX;
                float y1Top = (float) b[1] * scaleY;
                float x2 = (float) b[2] * scaleX;
                float y2Top = (float) b[3] * scaleY;
                float rx = Math.min(x1, x2);
                float ryTop = Math.min(y1Top, y2Top);
                float rw = Math.abs(x2 - x1);
                float rh = Math.abs(y2Top - y1Top);

                PDAnnotationTextMarkup m = new PDAnnotationTextMarkup(PDAnnotationTextMarkup.SUB_TYPE_HIGHLIGHT);
				float[] color = rp.op == DiffUtil.Operation.DELETE ? new float[] { 1f, 0f, 0f }
						: (rp.op == DiffUtil.Operation.INSERT ? new float[] { 0f, 1f, 0f }
								: new float[] { 1f, 1f, 0f });
                m.setColor(new PDColor(color, PDDeviceRGB.INSTANCE));

                PDRectangle pr = new PDRectangle();
                pr.setLowerLeftX(rx);
                pr.setLowerLeftY(Math.max(0, pageH - (ryTop + rh)));
                pr.setUpperRightX(rx + rw);
                pr.setUpperRightY(Math.max(0, Math.min(pageH, pageH - ryTop)));
                m.setRectangle(pr);
				float[] qu = new float[] { pr.getLowerLeftX(), pr.getUpperRightY(), pr.getUpperRightX(),
						pr.getUpperRightY(), pr.getLowerLeftX(), pr.getLowerLeftY(), pr.getUpperRightX(),
						pr.getLowerLeftY() };
                m.setQuadPoints(qu);
                m.setSubtype("Highlight");

				java.util.List<org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation> anns = page
						.getAnnotations();
				if (anns == null)
					anns = new ArrayList<>();
                anns.add(m);
                page.setAnnotations(anns);
            }
            doc.save(outPath);
        }
    }

    // ---------- Models ----------
}


