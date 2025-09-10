package com.zhaoxinms.contract.tools.ocr;

import com.zhaoxinms.contract.tools.compare.DiffUtil;
import com.zhaoxinms.contract.tools.ocr.model.CharBox;
import com.zhaoxinms.contract.tools.ocr.model.DiffBlock;

import java.util.*;

/**
 * 差异处理工具类
 *
 * 提供文本差异分析和处理的核心功能，包括：
 * - 将Google diff-match-patch算法生成的差异对象转换为结构化的DiffBlock对象
 * - 应用自定义过滤规则过滤掉不需要的差异内容
 * - 提供详细的差异分析和统计
 *
 * @author zhaoxin
 * @version 1.0
 * @since 2025-01-14
 */
public class DiffProcessingUtil {

	/**
	 * 将差异列表转换为DiffBlock列表
	 *
	 * 根据差异操作类型（DELETE, INSERT, EQUAL）将差异对象转换为对应的DiffBlock对象。
	 * 每个DiffBlock包含对应的bbox坐标、文本内容和索引信息。
	 *
	 * @param diffs DiffUtil.diff_main生成的差异列表
	 * @param seqA  文档A的字符序列
	 * @param seqB  文档B的字符序列
	 * @return DiffBlock列表，每个代表一个差异单元
	 */
	public static List<DiffBlock> splitDiffsByBounding(LinkedList<DiffUtil.Diff> diffs, List<CharBox> seqA,
			List<CharBox> seqB) {
		List<DiffBlock> result = new ArrayList<>();
		DiffBlock prevBlock = null; // 记录上一个block用于设置prevBboxes

		// 预计算：整个文档中每个bbox的完整文本
		Map<String, StringBuilder> aAllText = new LinkedHashMap<>();
		Map<String, StringBuilder> bAllText = new LinkedHashMap<>();
		for (CharBox c : seqA) {
			if (c.bbox != null) { 
				String k = key(c.page, c.bbox);
				aAllText.computeIfAbsent(k, kk -> new StringBuilder()).append(c.ch);
			}
		}
		for (CharBox c : seqB) {
			if (c.bbox != null) {
				String k = key(c.page, c.bbox);
				bAllText.computeIfAbsent(k, kk -> new StringBuilder()).append(c.ch);
			}
		}

		int aIdx = 0, bIdx = 0;
		for (DiffUtil.Diff d : diffs) {
			String txt = d.text.replaceAll("\n", "");
			int len = txt.length();

		List<CharBox> aSeg = Collections.emptyList();
		List<CharBox> bSeg = Collections.emptyList();
		
		// 预先计算实际长度，供后续差异范围计算使用
		int actualLenA = 0;
		int actualLenB = 0;

		if (d.operation == DiffUtil.Operation.DELETE) {
			actualLenA = calculateActualLength(seqA, aIdx, len);
			aSeg = subChars(seqA, aIdx, aIdx + actualLenA);
			aIdx += actualLenA;
		} else if (d.operation == DiffUtil.Operation.INSERT) {
			actualLenB = calculateActualLength(seqB, bIdx, len);
			bSeg = subChars(seqB, bIdx, bIdx + actualLenB);
			bIdx += actualLenB;
		} else if (d.operation == DiffUtil.Operation.EQUAL) {
			// EQUAL operation also needs to handle bbox mapping to ensure correct indexing
			actualLenA = calculateActualLength(seqA, aIdx, len);
			actualLenB = calculateActualLength(seqB, bIdx, len);
			aSeg = subChars(seqA, aIdx, aIdx + actualLenA);
			bSeg = subChars(seqB, bIdx, bIdx + actualLenB);
			aIdx += actualLenA;
			bIdx += actualLenB;
		}

			// Create DiffBlock containing all related bboxes for this diff
			Map<String, List<CharBox>> aGroups = groupByBox(aSeg);
			Map<String, List<CharBox>> bGroups = groupByBox(bSeg);

			// Collect bboxes involved in this diff
			List<double[]> oldBboxes = new ArrayList<>();
			List<double[]> newBboxes = new ArrayList<>();
			List<String> allTextAList = new ArrayList<>();
			List<String> allTextBList = new ArrayList<>();
			StringBuilder allOldText = new StringBuilder();
			StringBuilder allNewText = new StringBuilder();
			String category = "";
			int pageA = 1; // 文档A页码
			int pageB = 1; // 文档B页码
		// 页码数组
		List<Integer> pageAList = new ArrayList<>();
		List<Integer> pageBList = new ArrayList<>();
			// 预计算的差异范围（在创建blk之后再写入）
			List<DiffBlock.TextRange> computedRangesA = java.util.Collections.emptyList();
			List<DiffBlock.TextRange> computedRangesB = java.util.Collections.emptyList();

			// 根据操作类型分别处理aGroups和bGroups
			if (d.operation == DiffUtil.Operation.DELETE) {
				// DELETE操作：只处理文档A的bboxes
				for (String k : aGroups.keySet()) {
					List<CharBox> aa = aGroups.get(k);
					if (aa.isEmpty())
						continue;

					double[] bbox;
					try {
						bbox = parseBoxKey(k);
					} catch (IllegalArgumentException e) {
						System.err.println("Warning: Skipping invalid bbox key in splitDiffsByBounding: " + k + " - "
								+ e.getMessage());
						continue;
					}

					String oldText = join(aa);
					oldBboxes.add(bbox);
					allTextAList.add(aAllText.get(k) == null ? "" : aAllText.get(k).toString());
					allOldText.append(oldText);

					if (category.isEmpty()) {
						category = pickCategory(aa, java.util.Collections.emptyList());
					}

					// 设置页码：将每个bbox的页码添加到页码数组
					int currentPageA = pageOf(aa);
					pageAList.add(currentPageA);
					// 向后兼容：设置第一个页码
					if (pageAList.size() == 1) {
						pageA = currentPageA;
					}
				}

				// 计算 A 侧差异范围：基于你的逻辑（先缓存，后写入）
				{
					java.util.List<DiffBlock.TextRange> rangesA = new java.util.ArrayList<>();
					int prefix = 0;
					for (String k : aGroups.keySet()) {
						java.util.List<CharBox> aa = aGroups.get(k);
						String full = aAllText.get(k) == null ? "" : aAllText.get(k).toString();
						
						// 找到该 bbox 在 seqA 中的文本段起点
						int textSegmentStart = findBboxStartInSequence(seqA, k);
						System.out.println("DEBUG A侧 - bbox: " + k + ", 文本段起点: " + textSegmentStart + ", aIdx: " + aIdx + ", actualLenA: " + actualLenA);
						if (textSegmentStart >= 0 && aIdx >= textSegmentStart) {
							// 计算差异在该文本段内的相对偏移和长度
							int diffStartInText = aIdx - textSegmentStart - actualLenA;  // 差异在文本段内的起始位置
							int diffLength = actualLenA;  // 差异的长度
							
							// 添加范围：prefix + diffStartInText 到 prefix + diffStartInText + diffLength
							if (diffLength > 0) {
								rangesA.add(new DiffBlock.TextRange(prefix + diffStartInText, prefix + diffStartInText + diffLength, "DIFF"));
								System.out.println("DEBUG A侧范围 - 文本段内偏移: " + diffStartInText + ", 长度: " + diffLength + ", 最终范围: [" + (prefix + diffStartInText) + ", " + (prefix + diffStartInText + diffLength) + "]");
							}
						}
						prefix += full.length();
					}
					computedRangesA = rangesA;
				}

			} else if (d.operation == DiffUtil.Operation.INSERT) {
				// INSERT操作：只处理文档B的bboxes
				for (String k : bGroups.keySet()) {
					List<CharBox> bb = bGroups.get(k);
					if (bb.isEmpty())
						continue;

					double[] bbox;
					try {
						bbox = parseBoxKey(k);
					} catch (IllegalArgumentException e) {
						System.err.println("Warning: Skipping invalid bbox key in splitDiffsByBounding: " + k + " - "
								+ e.getMessage());
						continue;
					}

					String newText = join(bb);
					newBboxes.add(bbox);
					allTextBList.add(bAllText.get(k) == null ? "" : bAllText.get(k).toString());
					allNewText.append(newText);

					if (category.isEmpty()) {
						category = pickCategory(java.util.Collections.emptyList(), bb);
					}

					// 设置页码：将每个bbox的页码添加到页码数组
					int currentPageB = pageOf(bb);
					pageBList.add(currentPageB);
					// 向后兼容：设置第一个页码
					if (pageBList.size() == 1) {
						pageB = currentPageB;
					}
				}

				// 计算 B 侧差异范围：基于你的逻辑（先缓存，后写入）
				{
					java.util.List<DiffBlock.TextRange> rangesB = new java.util.ArrayList<>();
					int prefixB = 0;
					for (String k : bGroups.keySet()) {
						java.util.List<CharBox> bb = bGroups.get(k);
						String full = bAllText.get(k) == null ? "" : bAllText.get(k).toString();
						
						// 找到该 bbox 在 seqB 中的文本段起点
						int textSegmentStart = findBboxStartInSequence(seqB, k);
						System.out.println("DEBUG B侧 - bbox: " + k + ", 文本段起点: " + textSegmentStart + ", bIdx: " + bIdx + ", actualLenB: " + actualLenB);
						
						if (textSegmentStart >= 0 && bIdx >= textSegmentStart) {
							// 计算差异在该文本段内的相对偏移和长度
							int diffStartInText = bIdx - textSegmentStart - actualLenB;  // 差异在文本段内的起始位置
							int diffLength = actualLenB;  // 差异的长度
							
							// 添加范围：prefixB + diffStartInText 到 prefixB + diffStartInText + diffLength
							if (diffLength > 0) {
								rangesB.add(new DiffBlock.TextRange(prefixB + diffStartInText, prefixB + diffStartInText + diffLength, "DIFF"));
								System.out.println("DEBUG B侧范围 - 文本段内偏移: " + diffStartInText + ", 长度: " + diffLength + ", 最终范围: [" + (prefixB + diffStartInText) + ", " + (prefixB + diffStartInText + diffLength) + "]");
							}
						}
						prefixB += full.length();
					}
					computedRangesB = rangesB;
				}

			} else if (d.operation == DiffUtil.Operation.EQUAL) {
				// EQUAL操作：分别处理aGroups和bGroups以确保正确的bbox对应关系

				// 处理文档A的所有bboxes
				for (String k : aGroups.keySet()) {
					List<CharBox> aa = aGroups.get(k);
					if (aa.isEmpty())
						continue;

					double[] bbox;
					try {
						bbox = parseBoxKey(k);
					} catch (IllegalArgumentException e) {
						System.err.println("Warning: Skipping invalid bbox key in splitDiffsByBounding: " + k + " - "
								+ e.getMessage());
						continue;
					}

					String oldText = join(aa);
					oldBboxes.add(bbox);
					allTextAList.add(aAllText.get(k) == null ? "" : aAllText.get(k).toString());
					allOldText.append(oldText);

					if (category.isEmpty()) {
						category = pickCategory(aa, Collections.emptyList());
					}

					// 设置页码：将每个bbox的页码添加到页码数组
					int currentPageA = pageOf(aa);
					pageAList.add(currentPageA);
					// 向后兼容：设置第一个页码
					if (pageAList.size() == 1) {
						pageA = currentPageA;
					}
				}

				// 处理文档B的所有bboxes
				for (String k : bGroups.keySet()) {
					List<CharBox> bb = bGroups.get(k);
					if (bb.isEmpty())
						continue;

					double[] bbox;
					try {
						bbox = parseBoxKey(k);
					} catch (IllegalArgumentException e) {
						System.err.println("Warning: Skipping invalid bbox key in splitDiffsByBounding: " + k + " - "
								+ e.getMessage());
						continue;
					}

					String newText = join(bb);
					newBboxes.add(bbox);
					allTextBList.add(bAllText.get(k) == null ? "" : bAllText.get(k).toString());
					allNewText.append(newText);

					if (category.isEmpty()) {
						category = pickCategory(Collections.emptyList(), bb);
					}

					// 设置页码：将每个bbox的页码添加到页码数组
					int currentPageB = pageOf(bb);
					pageBList.add(currentPageB);
					// 向后兼容：设置第一个页码
					if (pageBList.size() == 1) {
						pageB = currentPageB;
					}
				}
			}

			// 根据原始差异操作类型设置新类型
			DiffBlock.DiffType dt;
			if (d.operation == DiffUtil.Operation.DELETE) {
				dt = DiffBlock.DiffType.DELETED;
			} else if (d.operation == DiffUtil.Operation.INSERT) {
				dt = DiffBlock.DiffType.ADDED;
			} else if (d.operation == DiffUtil.Operation.EQUAL) {
				dt = DiffBlock.DiffType.IGNORED; // EQUAL操作标记为IGNORED，无需在PDF中标记
			} else {
				continue; // 未知操作类型，跳过
			}

			// 创建包含oldBboxes和newBboxes的DiffBlock
			// textStartIndexA和textStartIndexB直接设置为当前差异在文本中的起始位置aIdx和bIdx
			DiffBlock blk = DiffBlock.of(dt, pageAList, pageBList, oldBboxes, newBboxes, category, allOldText.toString(),
					allNewText.toString(), aIdx, bIdx, d);

			// 直接设置索引信息（简化逻辑）
			blk.indexA = (d.operation == DiffUtil.Operation.DELETE || d.operation == DiffUtil.Operation.EQUAL) ? aIdx
					: -1;
			blk.indexB = (d.operation == DiffUtil.Operation.INSERT || d.operation == DiffUtil.Operation.EQUAL) ? bIdx
					: -1;

			blk.allTextA = allTextAList;
			blk.allTextB = allTextBList;
			// 写入预计算的差异范围
			blk.diffRangesA = computedRangesA;
			blk.diffRangesB = computedRangesB;

			if (allNewText.toString().contains("广州市国际会展中心有限公司")) {
				System.out.println("-----------------------------到了");
			}
			
			// 设置前一个块的bboxes用于同步
			if (prevBlock != null) {
				if (d.operation == DiffUtil.Operation.INSERT) {
					// INSERT操作：优先使用前一个块的oldBboxes，如果没有则继承prevOldBboxes
					if (prevBlock.oldBboxes != null && !prevBlock.oldBboxes.isEmpty()) {
						blk.prevOldBboxes = new ArrayList<>(prevBlock.oldBboxes);
					} else {
						blk.prevOldBboxes = (prevBlock.prevOldBboxes == null) ? null : new ArrayList<>(prevBlock.prevOldBboxes);
					}
					// prevNewBboxes使用前一个块的newBboxes
					blk.prevNewBboxes = (prevBlock.newBboxes == null) ? null : new ArrayList<>(prevBlock.newBboxes);
					// INSERT操作的pageA应该继承前一个块的pageA（用于跳转参考）
					if (prevBlock.pageA != null && !prevBlock.pageA.isEmpty()) {
						blk.pageA = new ArrayList<>(prevBlock.pageA);
					}
				} else if (d.operation == DiffUtil.Operation.DELETE) {
					// DELETE操作：优先使用前一个块的newBboxes，如果没有则继承prevNewBboxes
					if (prevBlock.newBboxes != null && !prevBlock.newBboxes.isEmpty()) {
						blk.prevNewBboxes = new ArrayList<>(prevBlock.newBboxes);
					} else {
						blk.prevNewBboxes = (prevBlock.prevNewBboxes == null) ? null : new ArrayList<>(prevBlock.prevNewBboxes);
					}
					// prevOldBboxes使用前一个块的oldBboxes
					blk.prevOldBboxes = (prevBlock.oldBboxes == null) ? null : new ArrayList<>(prevBlock.oldBboxes);
					// DELETE操作的pageB应该继承前一个块的pageB（用于跳转参考）
					if (prevBlock.pageB != null && !prevBlock.pageB.isEmpty()) {
						blk.pageB = new ArrayList<>(prevBlock.pageB);
					}
				} else {
					// EQUAL操作：正常继承
					blk.prevOldBboxes = (prevBlock.oldBboxes == null) ? null : new ArrayList<>(prevBlock.oldBboxes);
					blk.prevNewBboxes = (prevBlock.newBboxes == null) ? null : new ArrayList<>(prevBlock.newBboxes);
				}
			}
			
			// 兜底逻辑：确保始终有可用于对齐跳转的prevOldBboxes/prevNewBboxes
			if (blk.prevOldBboxes == null || blk.prevOldBboxes.isEmpty()) {
				// 对于INSERT操作，如果prevOldBboxes为空，使用当前块的oldBboxes（通常为空）
				// 对于DELETE操作，使用当前块的oldBboxes
				blk.prevOldBboxes = (blk.oldBboxes == null) ? new ArrayList<>() : new ArrayList<>(blk.oldBboxes);
			}
			if (blk.prevNewBboxes == null || blk.prevNewBboxes.isEmpty()) {
				// 对于DELETE操作，如果prevNewBboxes为空，使用当前块的newBboxes（通常为空）
				// 对于INSERT操作，使用当前块的newBboxes
				blk.prevNewBboxes = (blk.newBboxes == null) ? new ArrayList<>() : new ArrayList<>(blk.newBboxes);
			}

			// 页码已经在创建blk对象时设置，这里不需要再次同步
			
			// 调试输出：验证页码和prevBboxes的设置
			System.out.println("DEBUG DiffBlock创建 - 操作: " + d.operation + ", pageA: " + pageAList + ", pageB: " + pageBList);
			System.out.println("DEBUG 页码设置逻辑 - 当前块页码基于实际bbox设置，prevBboxes用于跳转参考");
			if (prevBlock != null) {
				System.out.println("DEBUG 前一个块 - pageA: " + prevBlock.pageA + ", pageB: " + prevBlock.pageB + 
					", oldBboxes: " + (prevBlock.oldBboxes != null ? prevBlock.oldBboxes.size() : 0) + 
					", newBboxes: " + (prevBlock.newBboxes != null ? prevBlock.newBboxes.size() : 0));
			}
			if (blk.prevOldBboxes != null && !blk.prevOldBboxes.isEmpty()) {
				System.out.println("DEBUG prevOldBboxes: " + blk.prevOldBboxes.size() + "个, 第一个: [" + 
					blk.prevOldBboxes.get(0)[0] + "," + blk.prevOldBboxes.get(0)[1] + "," + 
					blk.prevOldBboxes.get(0)[2] + "," + blk.prevOldBboxes.get(0)[3] + "]");
			} else {
				System.out.println("DEBUG prevOldBboxes: 空");
			}
			if (blk.prevNewBboxes != null && !blk.prevNewBboxes.isEmpty()) {
				System.out.println("DEBUG prevNewBboxes: " + blk.prevNewBboxes.size() + "个, 第一个: [" + 
					blk.prevNewBboxes.get(0)[0] + "," + blk.prevNewBboxes.get(0)[1] + "," + 
					blk.prevNewBboxes.get(0)[2] + "," + blk.prevNewBboxes.get(0)[3] + "]");
			} else {
				System.out.println("DEBUG prevNewBboxes: 空");
			}

			result.add(blk);

			// 更新前一个块引用用于下一次迭代
			prevBlock = blk;
		}
		return result;
	}

	/**
	 * 根据diff_cleanupCustomIgnore规则过滤DiffBlock列表
	 *
	 * 简化的过滤方法：直接使用rawBlocks的数据，只应用过滤规则来标记被忽略的块。
	 * 使用originalDiff.text内容来确定块是否应该被忽略。rawBlocks已经计算了
	 * textStartIndexA、textStartIndexB等，无需重新计算。
	 *
	 * @param blocks 来自splitDiffsByBounding方法的原始DiffBlock列表
	 * @param seqA   文档A的字符序列
	 * @param seqB   文档B的字符序列
	 * @return 过滤后的DiffBlock列表，包含所有差异但标记被忽略的项目
	 */
	public static List<DiffBlock> filterIgnoredDiffBlocks(List<DiffBlock> blocks, List<CharBox> seqA,
			List<CharBox> seqB) {
		// 简化的过滤方法：直接使用每个块的originalDiff.text来确定是否应该被忽略
		if (blocks.isEmpty())
			return blocks;

		int ignoredCount = 0;
		int retainedCount = 0;

		// 输出过滤开始信息
		System.out.println("=== Difference Filtering Statistics ===");
		System.out.println("Total blocks to process: " + blocks.size());

		// 根据originalDiff.text内容标记应该被忽略的块
		for (DiffBlock block : blocks) {
			if (block.originalDiff == null) {
				// 没有原始差异信息，保留该块
				retainedCount++;
				continue;
			}

			// 根据文本内容检查该块是否应该被忽略
			String ignoreReason = getIgnoreReason(block.originalDiff);
			boolean shouldIgnore = !"".equals(ignoreReason); // 如果有原因，应该被忽略

			if (shouldIgnore) {
				block.type = DiffBlock.DiffType.IGNORED;
				ignoredCount++;
//				System.out.println(String.format("Ignored block: Operation=%s, Text='%s' (Length=%d) -> Reason: %s",
//						block.originalDiff.operation.name(), block.originalDiff.text.replace("\n", "\\n"),
//						block.originalDiff.text.length(), ignoreReason));
			} else {
				retainedCount++;
			}
		}

		// Output final filtering statistics
//		System.out.println("Filtering completed: Retained=" + retainedCount + ", Ignored=" + ignoredCount + ", Total="
//				+ blocks.size());

		return blocks;
	}

	// 辅助方法：从DotsOcrCompareDemoTest复制
	private static String key(int page, double[] box) {
		return page + "|" + (int) box[0] + "," + (int) box[1] + "," + (int) box[2] + "," + (int) box[3];
	}

	private static double[] parseBoxKey(String key) {
		String[] parts = key.split("\\|");
		if (parts.length != 2) {
			throw new IllegalArgumentException("Invalid bbox key format: " + key);
		}

		String[] coords = parts[1].split(",");
		if (coords.length != 4) {
			throw new IllegalArgumentException("Invalid bbox coordinates: " + parts[1]);
		}

		return new double[] { Double.parseDouble(coords[0]), Double.parseDouble(coords[1]),
				Double.parseDouble(coords[2]), Double.parseDouble(coords[3]) };
	}

	private static List<CharBox> subChars(List<CharBox> seq, int start, int end) {
		if (start >= seq.size())
			return Collections.emptyList();
		end = Math.min(end, seq.size());
		return seq.subList(start, end);
	}

	private static Map<String, List<CharBox>> groupByBox(List<CharBox> chars) {
		Map<String, List<CharBox>> groups = new LinkedHashMap<>();
		for (CharBox c : chars) {
			if (c.bbox != null) {
				String key = key(c.page, c.bbox);
				groups.computeIfAbsent(key, k -> new ArrayList<>()).add(c);
			}
		}
		return groups;
	}

	/**
	 * 在序列中找到指定 bbox 的第一次出现位置（文本段起点）
	 * @param sequence 完整的CharBox序列 
	 * @param bboxKey bbox的key (page + bbox坐标)
	 * @return 该bbox在序列中第一次出现的位置，找不到返回-1
	 */
	private static int findBboxStartInSequence(List<CharBox> sequence, String bboxKey) {
		if (sequence == null || bboxKey == null) return -1;
		
		for (int i = 0; i < sequence.size(); i++) {
			CharBox c = sequence.get(i);
			if (c.bbox != null && bboxKey.equals(key(c.page, c.bbox))) {
				return i;  // 找到第一次出现的位置
			}
		}
		return -1;  // 没找到
	}

	private static String join(List<CharBox> chars) {
		StringBuilder sb = new StringBuilder();
		for (CharBox c : chars) {
			sb.append(c.ch);
		}
		return sb.toString();
	}

	private static int pageOf(List<CharBox> chars) {
		if (chars.isEmpty())
			return 1;
		return chars.get(0).page;
	}

	private static String pickCategory(List<CharBox> a, List<CharBox> b) {
		List<CharBox> chars = !a.isEmpty() ? a : b;
		if (chars.isEmpty())
			return "unknown";

		// 简单分类逻辑：检查字符类型
		boolean hasDigits = false;
		boolean hasLetters = false;
		boolean hasSymbols = false;

		for (CharBox c : chars) {
			char ch = c.ch;
			if (Character.isDigit(ch)) {
				hasDigits = true;
			} else if (Character.isLetter(ch)) {
				hasLetters = true;
			} else if (!Character.isWhitespace(ch)) {
				hasSymbols = true;
			}
		}

		if (hasDigits && !hasLetters && !hasSymbols)
			return "digits";
		if (hasLetters && !hasDigits && !hasSymbols)
			return "letters";
		if (hasSymbols && !hasDigits && !hasLetters)
			return "symbols";
		return "mixed";
	}

	/**
	 * 分析差异被忽略的原因 - 参考diff_cleanupCustomIgnore的完整实现逻辑
	 */
	private static String getIgnoreReason(DiffUtil.Diff diff) {
		// 处理空文本
		if (diff.text == null || diff.text.isEmpty()) {
			return "空文本";
		}

		// 第一阶段：检查是否是需要完全过滤的内容类型
		if (isAllSpaces(diff.text)) {
			return "全为空格字符";
		}

		if (isAllUnderscores(diff.text)) {
			return "全为下划线字符";
		}

		if (isAllSpacesOrUnderscores(diff.text)) {
			return "空格和下划线混合";
		}

		if (isAllSpacesUnderscoresNewlines(diff.text)) {
			return "包含空格、下划线和换行符";
		}

		// 第二阶段：检查是否为目标标点符号（可能与相邻差异配对过滤）
		if (isTargetPunct(diff.text)) {
			return "单个标点符号（可能与相邻差异配对过滤）";
		}

		// 检查是否为纯数字
//		if (isPureDigits(diff.text)) {
//			return "纯数字";
//		}

		// 检查是否为常见的格式化字符
		if (isFormattingChars(diff.text)) {
			return "格式化字符";
		}

		// 检查是否只包含空格和句号
		if (isSpacesAndDots(diff.text)) {
			return "空格和句号混合";
		}

		// 检查是否全为#号
		if (isAllHashes(diff.text)) {
			return "全为#号字符";
		}

		// 检查是否只包含#号和空格
		if (isHashesAndSpaces(diff.text)) {
			return "#号和空格混合";
		}

		// 检查是否只包含#号和句号
		if (isHashesAndDots(diff.text)) {
			return "#号和句号混合";
		}

		// 如果不是上述情况，但操作是EQUAL，说明是相等内容
		if (diff.operation == DiffUtil.Operation.EQUAL) {
			return "相等内容，无需处理";
		}

		return "";
	}

	/**
	 * 检查字符串是否全为空格
	 */
	private static boolean isAllSpaces(String s) {
		if (s == null || s.isEmpty())
			return false;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c != ' ')
				return false;
		}
		return true;
	}

	/**
	 * 检查字符串是否全为下划线
	 */
	private static boolean isAllUnderscores(String s) {
		if (s == null || s.isEmpty())
			return false;
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) != '_')
				return false;
		}
		return true;
	}

	/**
	 * 检查字符串是否只包含空格和下划线
	 */
	private static boolean isAllSpacesOrUnderscores(String s) {
		if (s == null || s.isEmpty())
			return false;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c != ' ' && c != '_')
				return false;
		}
		return true;
	}

	/**
	 * 检查字符串是否只包含空格、下划线和换行符
	 */
	private static boolean isAllSpacesUnderscoresNewlines(String s) {
		if (s == null || s.isEmpty())
			return false;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c != ' ' && c != '_' && c != '\n' && c != '\r')
				return false;
		}
		return true;
	}

	/**
	 * 检查是否为目标标点符号（支持配对过滤）
	 */
	private static boolean isTargetPunct(String s) {
		if (s == null || s.length() != 1)
			return false;
		char c = s.charAt(0);
		return c == ',' || c == '、' || c == '.';
	}

	/**
	 * 检查是否为常见的格式化字符
	 */
	private static boolean isFormattingChars(String s) {
		if (s == null || s.isEmpty())
			return false;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			// 常见的格式化字符：制表符、换页符等
			if (c != '\t' && c != '\f' && c != '\u00A0' && c != '\u2007' && c != '\u202F') {
				return false;
			}
		}
		return true;
	}

	/**
	 * 检查字符串是否只包含空格和句号
	 */
	private static boolean isSpacesAndDots(String s) {
		if (s == null || s.isEmpty())
			return false;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c != ' ' && c != '.')
				return false;
		}
		return true;
	}

	/**
	 * 检查字符串是否全为#号
	 */
	private static boolean isAllHashes(String s) {
		if (s == null || s.isEmpty())
			return false;
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) != '#')
				return false;
		}
		return true;
	}

	/**
	 * 检查字符串是否只包含#号和空格
	 */
	private static boolean isHashesAndSpaces(String s) {
		if (s == null || s.isEmpty())
			return false;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c != '#' && c != ' ')
				return false;
		}
		return true;
	}

	/**
	 * 检查字符串是否只包含#号和句号
	 */
	private static boolean isHashesAndDots(String s) {
		if (s == null || s.isEmpty())
			return false;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c != '#' && c != '.')
				return false;
		}
		return true;
	}

	/**
	 * 计算CharBox片段的实际长度，包括所有换行符 用于处理diff文本中换行符被移除但CharBox中仍然存在的情况 逐步增加长度，每次只处理新增的换行符
	 *
	 * @param fullSequence 完整的CharBox序列
	 * @param startIndex   起始索引
	 * @param baseLength   基础长度（移除换行符后的文本长度）
	 * @return 实际需要的长度
	 */
	private static int calculateActualLength(List<CharBox> fullSequence, int startIndex, int baseLength) {
		int currentLength = baseLength;
		int maxIterations = 100; // 防止无限循环
		int iteration = 0;

		while (iteration < maxIterations) {
			// 基于当前长度获取片段
			List<CharBox> segment = subChars(fullSequence, startIndex, startIndex + currentLength);

			// 如果片段为空，直接返回当前长度
			if (segment == null || segment.isEmpty()) {
				return currentLength;
			}

			// 检查片段是否包含换行符
			boolean hasNewlines = segment.stream().anyMatch(cb -> cb.ch == '\n');

			if (!hasNewlines) {
				// 没有换行符，当前长度就是实际长度
				return currentLength;
			}

			if (iteration == 0) {
				long count = segment.stream().filter(cb -> cb.ch == '\n').count();

				currentLength += count;
			} else {
				// 判断增加的是不是'\n'
				if (segment.get(segment.size() - 1).ch == '\n') {
					currentLength += 1;
				} else {
					return currentLength;
				}
			}
			iteration++;
		}

		// 如果达到最大迭代次数，返回当前长度
		System.err.println("Warning: Reached maximum iterations in calculateActualLength, length=" + currentLength);
		return currentLength;
	}
}
