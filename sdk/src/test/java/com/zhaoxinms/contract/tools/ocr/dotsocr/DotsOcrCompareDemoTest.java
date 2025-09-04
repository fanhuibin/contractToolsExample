package com.zhaoxinms.contract.tools.ocr.dotsocr;

import com.alibaba.druid.util.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.difflib.text.DiffRow;
import com.github.difflib.text.DiffRowGenerator;
import com.zhaoxinms.contract.tools.compare.DiffUtil;
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
 * Dots.OCR 对比演示：
 * 1) 分别识别两份PDF（并发页级识别，提取文字与bbox/category）
 * 2) 将整份PDF展平为字符序列（每个字符携带page/bbox/category）
 * 3) 使用 java-diff-utils 做差异，按bbox分裂或合并差异块，输出 修改/新增/删除
 */
public class DotsOcrCompareDemoTest {

    private static final ObjectMapper M = new ObjectMapper();

    private OkHttpClient httpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(Duration.ofMinutes(5))
                .readTimeout(Duration.ofMinutes(5))
                .writeTimeout(Duration.ofMinutes(5))
                .build();
    }

    private DotsOcrClient newClient(String baseUrl) {
        return DotsOcrClient.builder()
                .baseUrl(baseUrl)
                .defaultModel("model")
                .httpClient(httpClient())
                .build();
    }

    @Test
    public void compareTwoPdfs_Demo() throws Exception {
        boolean resumeFromStep4 = true;
        String baseUrl = "http://192.168.0.100:8000";
        Path fileA = Paths.get("C:\\Users\\范慧斌\\Desktop\\hetong比对前端\\test1.pdf");
        Path fileB = Paths.get("C:\\Users\\范慧斌\\Desktop\\hetong比对前端\\test2.pdf");

        String prompt = "Please output the layout information from the PDF image, including each layout element's bbox, its category, and the corresponding text content within the bbox.\n\n" +
                "1. Bbox format: [x1, y1, x2, y2]\n\n" +
                "2. Layout Categories: The possible categories are ['Caption', 'Footnote', 'Formula', 'List-item', 'Page-footer', 'Page-header', 'Picture', 'Section-header', 'Table', 'Text', 'Title'].\n\n" +
                "3. Text Extraction & Formatting Rules:\n" +
                "    - Picture: For the 'Picture' category, the text field should be omitted.\n" +
                "    - Formula: Format its text as LaTeX.\n" +
                "    - Table: Format its text as HTML.\n" +
                "    - All Others (Text, Title, etc.): Format their text as Plain text.\n\n" +
                "4. Constraints:\n" +
                "    - The output text must be the original text from the image, with no translation.\n" +
                "    - All layout elements must be sorted according to human reading order.\n\n" +
                "5. Final Output: The entire output must be a single JSON object.";

        DotsOcrClient client = newClient(baseUrl);

        long t0 = System.currentTimeMillis();
        List<CharBox> seqA = recognizePdfAsCharSeq(client, fileA, prompt, resumeFromStep4);
        List<CharBox> seqB = recognizePdfAsCharSeq(client, fileB, prompt, resumeFromStep4);
        long t1 = System.currentTimeMillis();
        System.out.println(String.format(Locale.ROOT, "OCR done. A=%d chars, B=%d chars, cost=%d ms", seqA.size(), seqB.size(), (t1 - t0)));

        // 3) diff（引入 TextNormalizer 忽略常见标点识别差异，保证 1:1 字符映射）
        // 保持原有的行结构，按行进行diff比对
        String normA = TextNormalizer.normalizePunctuation(joinWithLineBreaks(seqA));
        String normB = TextNormalizer.normalizePunctuation(joinWithLineBreaks(seqB));
        // 使用专业 Markdown 处理，按需求仅清理行首型语法（保留行内，仅保留 ** 粗体替换为空格）
        normA = normA.replace('$',' ').replace('_',' ');
        normB = normB.replace('$',' ').replace('_',' ');

        DiffUtil dmp = new DiffUtil();
        dmp.Diff_EditCost = 10; // Use Efficiency Cleanup with edit cost 10
        LinkedList<DiffUtil.Diff> diffs = dmp.diff_main(normA, normB);
        dmp.diff_cleanupEfficiency(diffs);
     
        List<DiffBlock> rawBlocks = splitDiffsByBounding(diffs, seqA, seqB);

        // 在索引计算完成后应用自定义过滤，过滤掉不需要标记的差异
        List<DiffBlock> filteredBlocks = filterIgnoredDiffBlocks(diffs, rawBlocks, seqA, seqB);

        // 合并同bbox内相邻或重叠的块，并判定类型
        List<DiffBlock> merged = mergeBlocksByBbox(filteredBlocks);

        // 输出并保存
        ObjectNode out = M.createObjectNode();
        out.put("fileA", fileA.toAbsolutePath().toString());
        out.put("fileB", fileB.toAbsolutePath().toString());
        ArrayNode arr = M.createArrayNode();
        for (DiffBlock b : merged) arr.add(b.toJson());
        out.set("diffs", arr);

        String pretty = M.writerWithDefaultPrettyPrinter().writeValueAsString(out);
        String outPath = System.getProperty("dotsocr.compare.out", fileA.toAbsolutePath().toString() + "__VS__" + fileB.getFileName().toString() + ".compare.json");
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

    private List<CharBox> recognizePdfAsCharSeq(DotsOcrClient client, Path pdf, String prompt, boolean resumeFromStep4) throws Exception {
        PageLayout[] ordered;
        if (resumeFromStep4) {
            // Step 1 (count pages) + Step 2 skipped; load Step 3 results (saved JSON)
            int total = countPdfPages(pdf);
            ordered = new PageLayout[total];
            for (int i = 0; i < total; i++) {
                final int pageNo = i + 1;
                PageLayout p = parseOnePageFromSavedJson(pdf, pageNo);
                ordered[pageNo - 1] = p;
            }
        } else {
            // Step 1: render PDF to images
            List<byte[]> pages = renderAllPagesToPng(client, pdf);
            int total = pages.size();
            int parallel = Math.max(1, Integer.parseInt(System.getProperty("dotsocr.parallel", "4")));
            java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(Math.min(parallel, total));
            java.util.concurrent.ExecutorCompletionService<PageLayout> ecs = new java.util.concurrent.ExecutorCompletionService<>(pool);
            for (int i = 0; i < total; i++) {
                final int pageNo = i + 1;
                final byte[] img = pages.get(i);
                ecs.submit(() -> parseOnePage(client, img, pageNo, prompt, pdf));
            }
            ordered = new PageLayout[total];
            for (int i = 0; i < total; i++) {
                PageLayout p = ecs.take().get();
                ordered[p.page - 1] = p;
            }
            pool.shutdownNow();
        }

        List<CharBox> out = new ArrayList<>();
        for (PageLayout pl : ordered) {
            if (pl == null) continue;
            for (LayoutItem it : pl.items) {
                out.addAll(expandToChars(pl.page, it));
            }
        }

        // Step 3: 保存提取的纯文本（含/不含页标记），便于开发调试
        try {
            StringBuilder extracted = new StringBuilder();
            StringBuilder extractedNoPage = new StringBuilder();
            for (PageLayout pl : ordered) {
                if (pl == null) continue;
                extracted.append("=== PAGE ").append(pl.page).append(" ===\n");
                for (LayoutItem it : pl.items) {
                    if (it.text != null && !it.text.isEmpty()) {
                        // 确保每行末尾都有换行符，处理可能包含换行符的文本
                        String[] lines = it.text.split("\n", -1); // -1 保留空行
                        for (String line : lines) {
                            extracted.append(line).append('\n');
                            extractedNoPage.append(line).append('\n');
                        }
                    }
                }
            }
            String txtOut = pdf.toAbsolutePath().toString() + ".extracted.txt";
            String txtOutCompare = pdf.toAbsolutePath().toString() + ".extracted.compare.txt";
            Files.write(Path.of(txtOut), extracted.toString().getBytes(StandardCharsets.UTF_8));
            Files.write(Path.of(txtOutCompare), extractedNoPage.toString().getBytes(StandardCharsets.UTF_8));
            System.out.println("Extracted text saved: " + txtOut);
            System.out.println("Extracted text (no page markers) saved: " + txtOutCompare);
        } catch (Exception e) {
            System.err.println("Failed to write extracted text: " + e.getMessage());
        }
        return out;
    }



    private PageLayout parseOnePage(DotsOcrClient client, byte[] pngBytes, int page, String prompt, Path pdfPath) throws Exception {
        String raw = client.ocrImageBytes(pngBytes, prompt, null, "image/png", false);
        JsonNode env = M.readTree(raw);
        String content = env.path("choices").path(0).path("message").path("content").asText("");
        if (content == null || content.isBlank()) throw new RuntimeException("模型未返回内容(page=" + page + ")");
        JsonNode root = M.readTree(content);
        // 保存每页识别的 JSON 结果，便于后续从第4步直接开始
        try {
            String pageJsonPath = pdfPath.toAbsolutePath().toString() + ".page-" + page + ".ocr.json";
            Files.write(Path.of(pageJsonPath), M.writerWithDefaultPrettyPrinter().writeValueAsBytes(root));
            System.out.println("Saved OCR JSON: " + pageJsonPath);
        } catch (Exception e) {
            System.err.println("Failed to save OCR JSON for page " + page + ": " + e.getMessage());
        }
        List<LayoutItem> items = extractLayoutItems(root);
        return new PageLayout(page, items);
    }

    private static String pdfPathOf(Path pdf) {
        return pdf.toAbsolutePath().toString();
    }


    // 从已保存的 JSON 结果加载单页版面（便于直接从第4步开始调试）
    private PageLayout parseOnePageFromSavedJson(Path pdfPath, int page) throws Exception {
        String pageJsonPath = pdfPath.toAbsolutePath().toString() + ".page-" + page + ".ocr.json";
        byte[] bytes = Files.readAllBytes(Path.of(pageJsonPath));
        JsonNode root = M.readTree(bytes);
        List<LayoutItem> items = extractLayoutItems(root);
        return new PageLayout(page, items);
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
                        Path out = pdfPath.getParent().resolve(pdfPath.getFileName().toString() + ".page-" + (i + 1) + ".png");
                        Files.write(out, bytes);
                    }
                }
            }
            return list;
        }
    }

    // 遍历JSON，提取 {bbox[4], category, text?}
    private List<LayoutItem> extractLayoutItems(JsonNode root) {
        List<LayoutItem> out = new ArrayList<>();
        Deque<JsonNode> queue = new ArrayDeque<>();
        queue.addLast(root);
        while (!queue.isEmpty()) {
            JsonNode n = queue.pollFirst();
            if (n.isObject()) {
                JsonNode bbox = n.get("bbox");
                JsonNode cat = n.get("category");
                JsonNode text = n.get("text");
                if (bbox != null && bbox.isArray() && bbox.size() == 4 && cat != null && cat.isTextual()) {
                    double x1 = bbox.get(0).asDouble();
                    double y1 = bbox.get(1).asDouble();
                    double x2 = bbox.get(2).asDouble();
                    double y2 = bbox.get(3).asDouble();
                    String category = cat.asText();
                    String tx = (text != null && !text.isNull()) ? text.asText("") : "";
                    out.add(new LayoutItem(new double[]{x1, y1, x2, y2}, category, tx));
                }
                n.fields().forEachRemaining(e -> queue.addLast(e.getValue()));
            } else if (n.isArray()) {
                for (JsonNode c : n) queue.addLast(c);
            }
        }
        // 近似“从上到下、从左到右”的阅读顺序：按 y1、再按 x1 升序
        out.sort(java.util.Comparator
                .comparingDouble((LayoutItem it) -> it.bbox[1])
                .thenComparingDouble(it -> it.bbox[0]));
        return out;
    }

    // 将一个布局元素的text展开成字符，按bbox宽度均分（简化近似）
    private List<CharBox> expandToChars(int page, LayoutItem it) {
        List<CharBox> out = new ArrayList<>();
        if (it.text == null || it.text.isEmpty()) return out;
        String s = it.text;
        // 保持每个字符所属的 bbox 等于布局项的整体 bbox，便于基于 bbox 的换行
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            out.add(new CharBox(page, ch, it.bbox, it.category));
        }
        return out;
    }

    // ---------- DIFF 分裂/合并 ----------

    private List<DiffBlock> splitDiffsByBounding(LinkedList<DiffUtil.Diff> diffs, List<CharBox> a, List<CharBox> b) {
        List<DiffBlock> result = new ArrayList<>();
        // 预计算：整份文档中每个 bbox 的首次出现索引与该 bbox 的完整文本
        Map<String, Integer> aFirstIndex = new LinkedHashMap<>();
        Map<String, Integer> bFirstIndex = new LinkedHashMap<>();
        Map<String, StringBuilder> aAllText = new LinkedHashMap<>();
        Map<String, StringBuilder> bAllText = new LinkedHashMap<>();
        for (int i = 0; i < a.size(); i++) {
            CharBox c = a.get(i);
            String k = key(c.page, c.bbox);
            aFirstIndex.putIfAbsent(k, i);
            aAllText.computeIfAbsent(k, kk -> new StringBuilder()).append(c.ch);
        }
        for (int i = 0; i < b.size(); i++) {
            CharBox c = b.get(i);
            String k = key(c.page, c.bbox);
            bFirstIndex.putIfAbsent(k, i);
            bAllText.computeIfAbsent(k, kk -> new StringBuilder()).append(c.ch);
        }
        int aIdx = 0, bIdx = 0;
        for (DiffUtil.Diff d : diffs) {
            String txt = d.text.replaceAll("\n", "");
            int len = txt.length();
            if (d.operation == DiffUtil.Operation.EQUAL) {
                aIdx += len; bIdx += len; continue;
            }
            List<CharBox> aSeg = Collections.emptyList();
            List<CharBox> bSeg = Collections.emptyList();
            if (d.operation == DiffUtil.Operation.DELETE) {
                aSeg = subChars(a, aIdx, aIdx + len); aIdx += len;
            } else if (d.operation == DiffUtil.Operation.INSERT) {
                bSeg = subChars(b, bIdx, bIdx + len); bIdx += len;
            }
            // INSERT/DELETE也可能紧邻EQUAL使另一侧有对应字符，尝试就地扩展对齐
            Map<String, List<CharBox>> aGroups = groupByBox(aSeg);
            Map<String, List<CharBox>> bGroups = groupByBox(bSeg);
            Set<String> keys = new LinkedHashSet<>();
            keys.addAll(aGroups.keySet()); keys.addAll(bGroups.keySet());
            for (String k : keys) {
                List<CharBox> aa = aGroups.getOrDefault(k, Collections.emptyList());
                List<CharBox> bb = bGroups.getOrDefault(k, Collections.emptyList());
                String category = pickCategory(aa, bb);
                double[] bbox = parseBoxKey(k);
                String oldText = join(aa);
                String newText = join(bb);

                // 直接根据原始diff操作类型设置新的类型
                DiffType dt;
                if (d.operation == DiffUtil.Operation.DELETE) {
                    dt = DiffType.DELETED;
                } else if (d.operation == DiffUtil.Operation.INSERT) {
                    dt = DiffType.ADDED;
                } else {
                    // 不应该到达这里，因为EQUAL在上面已经continue了
                    continue;
                }

                DiffBlock blk = DiffBlock.of(dt, aa.isEmpty() ? pageOf(bb) : pageOf(aa), bbox, category, oldText, newText);
                Integer idxA = aFirstIndex.get(k);
                Integer idxB = bFirstIndex.get(k);
                blk.indexA = idxA == null ? -1 : idxA;
                blk.indexB = idxB == null ? -1 : idxB;
                StringBuilder allA = aAllText.get(k);
                StringBuilder allB = bAllText.get(k);
                blk.allTextA = allA == null ? "" : allA.toString();
                blk.allTextB = allB == null ? "" : allB.toString();
                result.add(blk);
            }
        }
        return result;
    }

    /**
     * 根据diff_cleanupCustomIgnore规则过滤DiffBlock列表
     * 不影响索引计算，只过滤最终结果
     */
    private List<DiffBlock> filterIgnoredDiffBlocks(LinkedList<DiffUtil.Diff> originalDiffs, List<DiffBlock> blocks, List<CharBox> seqA, List<CharBox> seqB) {
        if (blocks.isEmpty()) return blocks;

        // 创建diffs的深拷贝，避免影响原始索引计算
        LinkedList<DiffUtil.Diff> diffsCopy = new LinkedList<>();
        for (DiffUtil.Diff d : originalDiffs) {
            diffsCopy.add(new DiffUtil.Diff(d.operation, d.text));
        }

        // 应用自定义过滤规则
        DiffUtil dmp = new DiffUtil();
        dmp.diff_cleanupCustomIgnore(diffsCopy);

        // 如果过滤后diffs没有变化，直接返回原始blocks
        if (diffsCopy.size() == originalDiffs.size()) {
            return blocks;
        }

        // 根据过滤后的diffs重建索引映射
        Map<String, Boolean> shouldKeepMap = new HashMap<>();
        int aIdx = 0, bIdx = 0;

        for (DiffUtil.Diff d : diffsCopy) {
            String txt = d.text.replaceAll("\n", "");
            int len = txt.length();

            if (d.operation == DiffUtil.Operation.EQUAL) {
                aIdx += len;
                bIdx += len;
                continue;
            }

            List<CharBox> aSeg = Collections.emptyList();
            List<CharBox> bSeg = Collections.emptyList();
            if (d.operation == DiffUtil.Operation.DELETE) {
                aSeg = subChars(seqA, aIdx, aIdx + len);
                aIdx += len;
            } else if (d.operation == DiffUtil.Operation.INSERT) {
                bSeg = subChars(seqB, bIdx, bIdx + len);
                bIdx += len;
            }

            // 标记这些bbox应该保留
            for (CharBox c : aSeg) {
                String k = key(c.page, c.bbox);
                shouldKeepMap.put(k, true);
            }
            for (CharBox c : bSeg) {
                String k = key(c.page, c.bbox);
                shouldKeepMap.put(k, true);
            }
        }

        // 根据映射过滤blocks
        List<DiffBlock> filtered = new ArrayList<>();
        for (DiffBlock block : blocks) {
            String k = key(block.page, block.bbox);
            if (shouldKeepMap.containsKey(k)) {
                filtered.add(block);
            }
        }

        return filtered;
    }

    private List<DiffBlock> mergeBlocksByBbox(List<DiffBlock> blocks) {
        // 合并相同 page+bbox 的多个差异块
        Map<String, DiffBlock> map = new LinkedHashMap<>();
        for (DiffBlock b : blocks) {
            String k = key(b.page, b.bbox);
            DiffBlock exist = map.get(k);
            if (exist == null) map.put(k, b);
            else {
                // 合并文本与类型：若存在 old 与 new 同时变化，视为 MODIFIED
                exist.oldText = exist.oldText + b.oldText;
                exist.newText = exist.newText + b.newText;
            }
        }
        return new ArrayList<>(map.values());
    }

    private static List<CharBox> subChars(List<CharBox> list, int start, int end) {
        start = Math.max(0, Math.min(start, list.size()));
        end = Math.max(0, Math.min(end, list.size()));
        if (start >= end) return Collections.emptyList();
        return new ArrayList<>(list.subList(start, end));
    }

    private static Map<String, List<CharBox>> groupByBox(List<CharBox> cs) {
        Map<String, List<CharBox>> m = new LinkedHashMap<>();
        for (CharBox c : cs) {
            String k = key(c.page, c.bbox);
            m.computeIfAbsent(k, kk -> new ArrayList<>()).add(c);
        }
        return m;
    }

    private static String key(int page, double[] box) {
        return page + "|" + String.format(Locale.ROOT, "%.3f,%.3f,%.3f,%.3f", box[0], box[1], box[2], box[3]);
    }

    private static double[] parseBoxKey(String k) {
        String[] parts = k.split("\\|");
        String[] ps = parts[1].split(",");
        return new double[]{Double.parseDouble(ps[0]), Double.parseDouble(ps[1]), Double.parseDouble(ps[2]), Double.parseDouble(ps[3])};
    }

    private static String pickCategory(List<CharBox> a, List<CharBox> b) {
        for (CharBox c : a) if (c.category != null && !c.category.isEmpty()) return c.category;
        for (CharBox c : b) if (c.category != null && !c.category.isEmpty()) return c.category;
        return "";
    }

    private static int pageOf(List<CharBox> cs) { return cs.isEmpty() ? 1 : cs.get(0).page; }

    private static String join(List<CharBox> cs) {
        StringBuilder sb = new StringBuilder();
        for (CharBox c : cs) sb.append(c.ch);
        return sb.toString();
    }

    // 保持按 bbox 的行结构：当检测到 bbox 变化时追加换行；最后一个 bbox 结束也补一个换行
    private static String joinWithLineBreaks(List<CharBox> cs) {
        if (cs.isEmpty()) return "";
        
        StringBuilder sb = new StringBuilder();
        String lastKey = null;
        
        for (CharBox c : cs) {
            String currentKey = key(c.page, c.bbox);
            
            // 如果切换到新的page+bbox，添加换行符
            if (lastKey != null && !lastKey.equals(currentKey)) {
                sb.append('\n');
            }
            
            sb.append(c.ch);
            lastKey = currentKey;
        }
        // 收尾：为最后一个 bbox 结束补一个换行
        sb.append('\n');
        return sb.toString();
    }

    // ---------- 坐标映射与PDF标注 ----------

    private static class IndexMap {
        final String normalized; // 与 diff 使用的同构文本（仅做 $/_ → 空格 与 标点归一）
        final int[] seqIndex;    // normalized 中每个字符位置对应的 CharBox 索引；无对应时为 -1（如换行）
        IndexMap(String normalized, int[] seqIndex) { this.normalized = normalized; this.seqIndex = seqIndex; }
    }

    private static IndexMap buildNormalizedIndexMap(List<CharBox> seq) {
        // 构建与 joinWithLineBreaks 一致的基础串，同时记录每个字符对应的 CharBox 索引
        StringBuilder base = new StringBuilder();
        List<Integer> idxMap = new ArrayList<>();
        String lastKey = null;
        for (int i = 0; i < seq.size(); i++) {
            CharBox c = seq.get(i);
            String currentKey = key(c.page, c.bbox);
            if (lastKey != null && !lastKey.equals(currentKey)) {
                base.append('\n');
                idxMap.add(-1);
            }
            base.append(c.ch);
            idxMap.add(i);
            lastKey = currentKey;
        }
        // 收尾换行
        base.append('\n');
        idxMap.add(-1);

        String norm = TextNormalizer.normalizePunctuation(base.toString()).replace('$',' ').replace('_',' ');
        // 规范化步骤不改变长度的假设（标点归一/替换为空格）。若未来改变长度，此映射将失配。
        int[] map = new int[idxMap.size()];
        for (int i = 0; i < idxMap.size(); i++) map[i] = idxMap.get(i);
        return new IndexMap(norm, map);
    }

    private static class RectOnPage {
        final int pageIndex0; // 0-based
        final double[] bbox;  // [x1,y1,x2,y2] 图像像素坐标
        final DiffUtil.Operation op; // INSERT/DELETE/MODIFIED 用于着色
        RectOnPage(int pageIndex0, double[] bbox, DiffUtil.Operation op) { this.pageIndex0 = pageIndex0; this.bbox = bbox; this.op = op; }
    }

    private static List<RectOnPage> collectRectsForDiffBlocks(List<DiffBlock> blocks, IndexMap map, List<CharBox> seq, boolean isLeft) {
        List<RectOnPage> out = new ArrayList<>();

        for (DiffBlock block : blocks) {
            // 根据block类型决定是否处理本侧
            DiffUtil.Operation op = null;
            if (block.type == DiffType.DELETED && isLeft) {
                op = DiffUtil.Operation.DELETE;
            } else if (block.type == DiffType.ADDED && !isLeft) {
                op = DiffUtil.Operation.INSERT;
            }

            if (op == null) continue; // 跳过不需要在本侧标记的块

            // 获取当前block对应的bbox
            String blockKey = key(block.page, block.bbox);

            // 在seq中找到所有属于这个bbox的CharBox
            Map<String, List<Integer>> byBox = new LinkedHashMap<>();
            for (int i = 0; i < seq.size(); i++) {
                CharBox c = seq.get(i);
                String k = key(c.page, c.bbox);
                if (k.equals(blockKey)) {
                    byBox.computeIfAbsent(k, kk -> new ArrayList<>()).add(i);
                }
            }

            // 为每个找到的box生成矩形
            for (Map.Entry<String, List<Integer>> e : byBox.entrySet()) {
                List<Integer> idxs = e.getValue();
                if (idxs.isEmpty()) continue;
                CharBox any = seq.get(idxs.get(0));
                // 使用该布局项的bbox（像素坐标系）
                out.add(new RectOnPage(any.page - 1, any.bbox, op));
            }
        }

        return out;
    }

    private static class PageImageSizeProvider {
        final int pageCount;
        final int[] widths;
        final int[] heights;
        PageImageSizeProvider(int pageCount, int[] widths, int[] heights) { this.pageCount = pageCount; this.widths = widths; this.heights = heights; }
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

    private static void annotatePDF(Path sourcePdf, String outPath, List<RectOnPage> rects, PageImageSizeProvider sizes) throws Exception {
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
                float[] color = rp.op == DiffUtil.Operation.DELETE ? new float[]{1f, 0f, 0f}
                                 : (rp.op == DiffUtil.Operation.INSERT ? new float[]{0f, 1f, 0f} : new float[]{1f, 1f, 0f});
                m.setColor(new PDColor(color, PDDeviceRGB.INSTANCE));

                PDRectangle pr = new PDRectangle();
                pr.setLowerLeftX(rx);
                pr.setLowerLeftY(Math.max(0, pageH - (ryTop + rh)));
                pr.setUpperRightX(rx + rw);
                pr.setUpperRightY(Math.max(0, Math.min(pageH, pageH - ryTop)));
                m.setRectangle(pr);
                float[] qu = new float[]{pr.getLowerLeftX(), pr.getUpperRightY(), pr.getUpperRightX(), pr.getUpperRightY(), pr.getLowerLeftX(), pr.getLowerLeftY(), pr.getUpperRightX(), pr.getLowerLeftY()};
                m.setQuadPoints(qu);
                m.setSubtype("Highlight");

                java.util.List<org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation> anns = page.getAnnotations();
                if (anns == null) anns = new ArrayList<>();
                anns.add(m);
                page.setAnnotations(anns);
            }
            doc.save(outPath);
        }
    }

    // ---------- Models ----------

    private static class PageLayout {
        final int page;
        final List<LayoutItem> items;
        PageLayout(int page, List<LayoutItem> items) { this.page = page; this.items = items; }
    }

    private static class LayoutItem {
        final double[] bbox; // [x1,y1,x2,y2]
        final String category;
        final String text; // may be empty for Picture
        LayoutItem(double[] bbox, String category, String text) { this.bbox = bbox; this.category = category; this.text = text; }
    }

    private static class CharBox {
        final int page;
        final char ch;
        final double[] bbox;
        final String category;
        CharBox(int page, char ch, double[] bbox, String category) { this.page = page; this.ch = ch; this.bbox = bbox; this.category = category; }
    }

    private enum DiffType { MODIFIED, ADDED, DELETED }

    private static class DiffBlock {
        DiffType type;
        int page;
        double[] bbox;
        String category;
        String oldText;
        String newText;
        // 新增：在全文字符序列中的首次索引（a/b），以及该 bbox 下的完整文本（a/b）
        int indexA; // -1 表示在 A 中不存在
        int indexB; // -1 表示在 B 中不存在
        String allTextA; // A 文档该 bbox 的完整文本
        String allTextB; // B 文档该 bbox 的完整文本

        static DiffBlock of(DiffType type, int page, double[] bbox, String category, String oldText, String newText) {
            DiffBlock r = new DiffBlock();
            r.type = type; r.page = page; r.bbox = bbox; r.category = category; r.oldText = oldText; r.newText = newText;
            return r;
        }

        JsonNode toJson() {
            ObjectNode n = M.createObjectNode();
            n.put("type", type.name());
            n.put("page", page);
            ArrayNode b = M.createArrayNode();
            b.add(bbox[0]).add(bbox[1]).add(bbox[2]).add(bbox[3]);
            n.set("bbox", b);
            n.put("category", category);
            n.put("oldText", oldText);
            n.put("newText", newText);
            n.put("indexA", indexA);
            n.put("indexB", indexB);
            n.put("allTextA", allTextA);
            n.put("allTextB", allTextB);
            return n;
        }
    }
}


