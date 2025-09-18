package com.zhaoxinms.contract.tools.ocr;

import com.zhaoxinms.contract.tools.common.exception.ServiceException;
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
	 * 将差异列表转换为DiffBlock列表（向后兼容版本）
	 *
	 * @param diffs DiffUtil.diff_main生成的差异列表
	 * @param seqA  文档A的字符序列
	 * @param seqB  文档B的字符序列
	 * @return DiffBlock列表，每个代表一个差异单元
	 */
	public static List<DiffBlock> splitDiffsByBounding(LinkedList<DiffUtil.Diff> diffs, List<CharBox> seqA,
			List<CharBox> seqB) {
		return splitDiffsByBounding(diffs, seqA, seqB, false);
	}

	/**
	 * 将差异列表转换为DiffBlock列表
	 *
	 * 根据差异操作类型（DELETE, INSERT, EQUAL）将差异对象转换为对应的DiffBlock对象。
	 * 每个DiffBlock包含对应的bbox坐标、文本内容和索引信息。
	 *
	 * @param diffs DiffUtil.diff_main生成的差异列表
	 * @param seqA  文档A的字符序列
	 * @param seqB  文档B的字符序列
	 * @param debugMode 是否开启调试模式，开启时会验证diff文本与字符序列的一致性
	 * @return DiffBlock列表，每个代表一个差异单元
	 */
	public static List<DiffBlock> splitDiffsByBounding(LinkedList<DiffUtil.Diff> diffs, List<CharBox> seqA,
			List<CharBox> seqB, boolean debugMode) {
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
			String txt = d.text;
			int len = txt.length();

		List<CharBox> aSeg = Collections.emptyList();
		List<CharBox> bSeg = Collections.emptyList();
		
		// 记录开始位置（在更新索引之前）
		int startAIdx = aIdx;
		int startBIdx = bIdx;
		
		if (d.operation == DiffUtil.Operation.DELETE) {
			aSeg = subChars(seqA, aIdx, aIdx + len);
			
			// 调试模式：验证diff文本与seqA字符的一致性
			if (debugMode) {
				//validateDiffTextConsistency(d, aSeg, "seqA", aIdx);
			}
			
			aIdx += len;
		} else if (d.operation == DiffUtil.Operation.INSERT) {
			bSeg = subChars(seqB, bIdx, bIdx + len);
			
			// 调试模式：验证diff文本与seqB字符的一致性
			if (debugMode) {
				//validateDiffTextConsistency(d, bSeg, "seqB", bIdx);
			}
			
			bIdx += len;
		} else if (d.operation == DiffUtil.Operation.EQUAL) {
			// EQUAL operation also needs to handle bbox mapping to ensure correct indexing
			aSeg = subChars(seqA, aIdx, aIdx + len);
			bSeg = subChars(seqB, bIdx, bIdx + len);
			
			// 调试模式：验证diff文本与seqA和seqB字符的一致性
			if (debugMode) {
				//validateDiffTextConsistency(d, aSeg, "seqA", aIdx);
				//validateDiffTextConsistency(d, bSeg, "seqB", bIdx);
			}
			
			aIdx += len;
			bIdx += len;
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

				// 计算 A 侧差异范围：DELETE操作，基于全局文本索引
				{
					java.util.List<DiffBlock.TextRange> rangesA = new java.util.ArrayList<>();
					
					// 对于DELETE操作，使用全局索引：从startAIdx开始，长度为len
					int globalStart = startAIdx;   // DELETE操作的全局起始位置
					int globalEnd = startAIdx + len; // DELETE操作的全局结束位置
					
					if (globalEnd > globalStart) {
						rangesA.add(new DiffBlock.TextRange(globalStart, globalEnd, "DIFF"));
						System.out.println("[DELETE全局DiffRange] 全局范围: [" + globalStart + "," + globalEnd + "], 长度=" + (globalEnd - globalStart));
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

				// 计算 B 侧差异范围：INSERT操作，基于全局文本索引
				{
					java.util.List<DiffBlock.TextRange> rangesB = new java.util.ArrayList<>();
					
					// 对于INSERT操作，使用全局索引：从startBIdx开始，长度为len
					int globalStart = startBIdx;   // INSERT操作的全局起始位置
					int globalEnd = startBIdx + len; // INSERT操作的全局结束位置
					
					if (globalEnd > globalStart) {
						rangesB.add(new DiffBlock.TextRange(globalStart, globalEnd, "DIFF"));
						System.out.println("[INSERT全局DiffRange] 全局范围: [" + globalStart + "," + globalEnd + "], 长度=" + (globalEnd - globalStart));
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

			// 计算第一个bbox在全局文本中的开始位置
			Integer textStartIndexA = null;
			Integer textStartIndexB = null;
			
			if (d.operation == DiffUtil.Operation.DELETE || d.operation == DiffUtil.Operation.EQUAL) {
				// DELETE和EQUAL操作有A侧文本
				if (!aGroups.isEmpty()) {
					// 找到第一个bbox在全局序列seqA中的位置
					textStartIndexA = findFirstBboxGlobalIndex(aGroups, seqA);
				}
			}
			
			if (d.operation == DiffUtil.Operation.INSERT || d.operation == DiffUtil.Operation.EQUAL) {
				// INSERT和EQUAL操作有B侧文本
				if (!bGroups.isEmpty()) {
					// 找到第一个bbox在全局序列seqB中的位置
					textStartIndexB = findFirstBboxGlobalIndex(bGroups, seqB);
				}
			}
			
			// 创建包含oldBboxes和newBboxes的DiffBlock
			// 处理null值避免自动拆箱错误
			int safeTextStartIndexA = (textStartIndexA != null) ? textStartIndexA : 0;
			int safeTextStartIndexB = (textStartIndexB != null) ? textStartIndexB : 0;
			
			DiffBlock blk = DiffBlock.of(dt, pageAList, pageBList, oldBboxes, newBboxes, category, allOldText.toString(),
					allNewText.toString(), safeTextStartIndexA, safeTextStartIndexB, d);

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

			// 设置前一个块的bboxes用于同步
			if (prevBlock != null) {
				if (d.operation == DiffUtil.Operation.INSERT) {
					// INSERT操作：优先使用前一个块的oldBboxes，如果没有则继承prevOldBboxes
					if (prevBlock.oldBboxes != null && !prevBlock.oldBboxes.isEmpty()) {
						double[] last = prevBlock.oldBboxes.get(prevBlock.oldBboxes.size() - 1);
						blk.prevOldBboxes = new ArrayList<>();
						blk.prevOldBboxes.add(last);
					} else if (prevBlock.prevOldBboxes != null && !prevBlock.prevOldBboxes.isEmpty()) {
						double[] last = prevBlock.prevOldBboxes.get(prevBlock.prevOldBboxes.size() - 1);
						blk.prevOldBboxes = new ArrayList<>();
						blk.prevOldBboxes.add(last);
					} else {
						blk.prevOldBboxes = null;
					}
					// prevNewBboxes使用前一个块的newBboxes（仅保留最后一个）
					if (prevBlock.newBboxes != null && !prevBlock.newBboxes.isEmpty()) {
						double[] lastNew = prevBlock.newBboxes.get(prevBlock.newBboxes.size() - 1);
						blk.prevNewBboxes = new ArrayList<>();
						blk.prevNewBboxes.add(lastNew);
					} else if (prevBlock.prevNewBboxes != null && !prevBlock.prevNewBboxes.isEmpty()) {
						double[] lastNew = prevBlock.prevNewBboxes.get(prevBlock.prevNewBboxes.size() - 1);
						blk.prevNewBboxes = new ArrayList<>();
						blk.prevNewBboxes.add(lastNew);
					} else {
						blk.prevNewBboxes = null;
					}
					// INSERT操作的pageA应该继承前一个块的pageA（仅保留最后一个，用于跳转参考）
					if (prevBlock.pageA != null && !prevBlock.pageA.isEmpty()) {
						blk.pageA = new ArrayList<>();
						blk.pageA.add(prevBlock.pageA.get(prevBlock.pageA.size() - 1));
					}
				} else if (d.operation == DiffUtil.Operation.DELETE) {
					// DELETE操作：优先使用前一个块的newBboxes，如果没有则继承prevNewBboxes
					if (prevBlock.newBboxes != null && !prevBlock.newBboxes.isEmpty()) {
						double[] last = prevBlock.newBboxes.get(prevBlock.newBboxes.size() - 1);
						blk.prevNewBboxes = new ArrayList<>();
						blk.prevNewBboxes.add(last);
					} else if (prevBlock.prevNewBboxes != null && !prevBlock.prevNewBboxes.isEmpty()) {
						double[] last = prevBlock.prevNewBboxes.get(prevBlock.prevNewBboxes.size() - 1);
						blk.prevNewBboxes = new ArrayList<>();
						blk.prevNewBboxes.add(last);
					} else {
						blk.prevNewBboxes = null;
					}
					// prevOldBboxes使用前一个块的oldBboxes（仅保留最后一个）
					if (prevBlock.oldBboxes != null && !prevBlock.oldBboxes.isEmpty()) {
						double[] lastOld = prevBlock.oldBboxes.get(prevBlock.oldBboxes.size() - 1);
						blk.prevOldBboxes = new ArrayList<>();
						blk.prevOldBboxes.add(lastOld);
					} else if (prevBlock.prevOldBboxes != null && !prevBlock.prevOldBboxes.isEmpty()) {
						double[] lastOld = prevBlock.prevOldBboxes.get(prevBlock.prevOldBboxes.size() - 1);
						blk.prevOldBboxes = new ArrayList<>();
						blk.prevOldBboxes.add(lastOld);
					} else {
						blk.prevOldBboxes = null;
					}
					// DELETE操作的pageB应该继承前一个块的pageB（仅保留最后一个，用于跳转参考）
					if (prevBlock.pageB != null && !prevBlock.pageB.isEmpty()) {
						blk.pageB = new ArrayList<>();
						blk.pageB.add(prevBlock.pageB.get(prevBlock.pageB.size() - 1));
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
				if (blk.oldBboxes != null && !blk.oldBboxes.isEmpty()) {
					double[] last = blk.oldBboxes.get(blk.oldBboxes.size() - 1);
					blk.prevOldBboxes = new ArrayList<>();
					blk.prevOldBboxes.add(last);
				} else {
					blk.prevOldBboxes = new ArrayList<>();
				}
			}
			if (blk.prevNewBboxes == null || blk.prevNewBboxes.isEmpty()) {
				// 对于DELETE操作，如果prevNewBboxes为空，使用当前块的newBboxes（通常为空）
				// 对于INSERT操作，使用当前块的newBboxes
				if (blk.newBboxes != null && !blk.newBboxes.isEmpty()) {
					double[] last = blk.newBboxes.get(blk.newBboxes.size() - 1);
					blk.prevNewBboxes = new ArrayList<>();
					blk.prevNewBboxes.add(last);
				} else {
					blk.prevNewBboxes = new ArrayList<>();
				}
			}

			// 页码已经在创建blk对象时设置，这里不需要再次同步
			
			// 调试输出：验证页码和prevBboxes的设置
			//System.out.println("DEBUG DiffBlock创建 - 操作: " + d.operation + ", pageA: " + pageAList + ", pageB: " + pageBList);
			//System.out.println("DEBUG 页码设置逻辑 - 当前块页码基于实际bbox设置，prevBboxes用于跳转参考");
//			if (prevBlock != null) {
//				System.out.println("DEBUG 前一个块 - pageA: " + prevBlock.pageA + ", pageB: " + prevBlock.pageB + 
//					", oldBboxes: " + (prevBlock.oldBboxes != null ? prevBlock.oldBboxes.size() : 0) + 
//					", newBboxes: " + (prevBlock.newBboxes != null ? prevBlock.newBboxes.size() : 0));
//			}
//			if (blk.prevOldBboxes != null && !blk.prevOldBboxes.isEmpty()) {
//				System.out.println("DEBUG prevOldBboxes: " + blk.prevOldBboxes.size() + "个, 第一个: [" + 
//					blk.prevOldBboxes.get(0)[0] + "," + blk.prevOldBboxes.get(0)[1] + "," + 
//					blk.prevOldBboxes.get(0)[2] + "," + blk.prevOldBboxes.get(0)[3] + "]");
//			} else {
//				System.out.println("DEBUG prevOldBboxes: 空");
//			}
//			if (blk.prevNewBboxes != null && !blk.prevNewBboxes.isEmpty()) {
//				System.out.println("DEBUG prevNewBboxes: " + blk.prevNewBboxes.size() + "个, 第一个: [" + 
//					blk.prevNewBboxes.get(0)[0] + "," + blk.prevNewBboxes.get(0)[1] + "," + 
//					blk.prevNewBboxes.get(0)[2] + "," + blk.prevNewBboxes.get(0)[3] + "]");
//			} else {
//				System.out.println("DEBUG prevNewBboxes: 空");
//			}

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
			} else {
				retainedCount++;
			}
		}

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
	 * 找到第一个bbox在全局序列中的开始位置
	 * @param groups bbox分组（按bbox key分组的CharBox列表）
	 * @param globalSequence 全局字符序列（seqA或seqB）
	 * @return 第一个bbox的第一个字符在全局序列中的位置
	 */
	private static Integer findFirstBboxGlobalIndex(Map<String, List<CharBox>> groups, List<CharBox> globalSequence) {
		if (groups.isEmpty() || globalSequence.isEmpty()) {
			return null;
		}
		
		// 获取第一个bbox的key（包含页码和坐标信息）
		String firstBboxKey = null;
		for (String bboxKey : groups.keySet()) {
			List<CharBox> chars = groups.get(bboxKey);
			if (!chars.isEmpty()) {
				firstBboxKey = bboxKey;
				break;
			}
		}
		
		if (firstBboxKey == null) {
			return null;
		}
		
		// 在全局序列中找到这个bbox第一次出现的位置
		for (int i = 0; i < globalSequence.size(); i++) {
			CharBox globalChar = globalSequence.get(i);
			if (globalChar.bbox != null) {
				String globalBboxKey = key(globalChar.page, globalChar.bbox);
				if (firstBboxKey.equals(globalBboxKey)) {
					System.out.println("[TextStartIndex] 找到第一个bbox在全局序列中的位置: 索引=" + i + 
						", bboxKey=" + firstBboxKey + ", 字符='" + globalChar.ch + "'");
					return i;
				}
			}
		}
		
		// 如果没找到，返回null
		System.out.println("[TextStartIndex] 未找到bbox在全局序列中的位置: bboxKey=" + firstBboxKey);
		return null;
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

		// 新增：忽略仅由空格和标点组成的片段，或空格紧邻单个标点（例如："  ;"、"  ，"、" ; ")
		if (isOnlySpacesAndPunct(diff.text) || isSpacesPlusSinglePunct(diff.text)) {
			return "空格与标点噪声";
		}
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

		// 新增规则：多个下划线+一个符号(, ; : . 。) 视为忽略
		if (isUnderscoresPlusOnePunct(diff.text)) {
			return "下划线与单符号组合";
		}

		// 第二阶段：检查是否为目标标点符号（可能与相邻差异配对过滤）
		// 修正：小数点"."不应该被忽略，因为它可能是数字的一部分
		if (isTargetPunct(diff.text) && !".".equals(diff.text)) {
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
		return c == ',' || c == '、' || c == '.' || c == ';' || c == ':' || c == '。' || c == '；' || c == '：';
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
			if (c != ' ' && c != '.' && c != '。')
				return false;
		}
		return true;
	}

	/**
	 * 仅由空白与标点组成（常见中英文标点）
	 * 修正：只有当标点符号前面有空格时才忽略，避免误判数字中的小数点
	 */
	private static boolean isOnlySpacesAndPunct(String s) {
		if (s == null || s.isEmpty()) return false;
		
		// 如果字符串中包含数字，则不忽略（避免误判如"1.2.3"这样的内容）
		boolean hasDigit = false;
		for (int i = 0; i < s.length(); i++) {
			if (Character.isDigit(s.charAt(i))) {
				hasDigit = true;
				break;
			}
		}
		if (hasDigit) {
			return false;
		}
		
		// 检查是否只包含空格和标点符号
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (Character.isWhitespace(c)) continue; // 空格/制表/换行
			if (isPunctuation(c)) continue;
			return false;
		}
		return true;
	}

	/**
	 * 空白紧邻单个标点（例："  ;" 或 ";  ")
	 * 修正：只有真正的"空格+标点"组合才忽略，避免误判数字相关的点号
	 */
	private static boolean isSpacesPlusSinglePunct(String s) {
		if (s == null || s.isEmpty()) return false;
		String trimmed = s.trim();
		if (trimmed.length() != 1) return false;
		char c = trimmed.charAt(0);
		
		// 如果是小数点，需要更严格的判断：必须前后都是空格才忽略
		if (c == '.') {
			// 只有当小数点前后都有空格时才忽略（如" . "），避免误判"1.2"中的点
			return s.length() > 1 && 
				   Character.isWhitespace(s.charAt(0)) && 
				   Character.isWhitespace(s.charAt(s.length() - 1));
		}
		
		boolean edgeSpace = Character.isWhitespace(s.charAt(0)) || Character.isWhitespace(s.charAt(s.length() - 1));
		return isPunctuation(c) && edgeSpace;
	}

	/**
	 * 是否为标点符号（覆盖Unicode标点类别与常见中文符号）
	 */
	private static boolean isPunctuation(char c) {
		int t = Character.getType(c);
		if (t == Character.CONNECTOR_PUNCTUATION
				|| t == Character.DASH_PUNCTUATION
				|| t == Character.START_PUNCTUATION
				|| t == Character.END_PUNCTUATION
				|| t == Character.INITIAL_QUOTE_PUNCTUATION
				|| t == Character.FINAL_QUOTE_PUNCTUATION
				|| t == Character.OTHER_PUNCTUATION) {
			return true;
		}
		// 补充常见中文/特殊标点与英文基础标点
		switch (c) {
			case '，': case '。': case '；': case '：': case '！': case '？': case '、':
			case '·': case '…': case '—':
				return true;
			default:
				return ",.;:!?".indexOf(c) >= 0;
		}
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
	 * 检查是否为“多个下划线 + 一个符号(, ; : . 。)”的模式
	 */
	private static boolean isUnderscoresPlusOnePunct(String s) {
		if (s == null || s.length() < 2) return false;
		// 允许的单个符号集合：英文逗号、分号、冒号、点；中文句号。
		char last = s.charAt(s.length() - 1);
		boolean isAllowedPunct = (last == ',' || last == ';' || last == ':' || last == '.' || last == '。');
		if (!isAllowedPunct) return false;
		// 前面必须全部为下划线
		for (int i = 0; i < s.length() - 1; i++) {
			if (s.charAt(i) != '_') return false;
		}
		return true;
	}

	// 调试计数器，限制输出前30条
	private static int debugConsistencyCheckCount = 0;
	private static final int MAX_DEBUG_OUTPUT = 110;
	
	/**
	 * 重置调试计数器（在每次新的比对任务开始时调用）
	 */
	public static void resetDebugCounter() {
		debugConsistencyCheckCount = 0;
	}

	/**
	 * 验证diff文本与字符序列的一致性（调试模式专用）
	 * 
	 * @param diff 差异对象
	 * @param charSegment 对应的字符片段
	 * @param seqName 序列名称（seqA或seqB）
	 * @param startIndex 起始索引
	 */
	private static void validateDiffTextConsistency(DiffUtil.Diff diff, List<CharBox> charSegment, String seqName, int startIndex) {
		// 如果已经输出30条，则不再输出
		if (debugConsistencyCheckCount >= MAX_DEBUG_OUTPUT) {
			return;
		}
		
		String diffText = diff.text;
		
		// 从字符片段构建实际文本
		StringBuilder actualText = new StringBuilder();
		for (CharBox cb : charSegment) {
			actualText.append(cb.ch);
		}
		String actualStr = actualText.toString();
		
		// 比较diff文本与实际字符序列
		if (!diffText.equals(actualStr)) {
			debugConsistencyCheckCount++;
			
			System.out.println("=== [调试] 文本一致性检查失败 #" + debugConsistencyCheckCount + " ===");
			System.out.println("操作类型: " + diff.operation);
			System.out.println("序列: " + seqName);
			System.out.println("起始索引: " + startIndex);
			System.out.println("长度: " + diffText.length() + " vs " + actualStr.length());
			System.out.println("Diff文本: '" + escapeString(diffText) + "'");
			System.out.println("实际文本: '" + escapeString(actualStr) + "'");
			
			// 逐字符比较，找出不一致的位置
			int minLen = Math.min(diffText.length(), actualStr.length());
			for (int i = 0; i < minLen; i++) {
				char diffChar = diffText.charAt(i);
				char actualChar = actualStr.charAt(i);
				if (diffChar != actualChar) {
					System.out.println("第" + i + "个字符不一致: '" + escapeChar(diffChar) + "' vs '" + escapeChar(actualChar) + "'");
					break;
				}
			}
			
			if (diffText.length() != actualStr.length()) {
				System.out.println("长度不一致: diff=" + diffText.length() + ", actual=" + actualStr.length());
			}
			System.out.println("=====================================");
			
			// 如果达到最大输出数量，提示用户
			if (debugConsistencyCheckCount >= MAX_DEBUG_OUTPUT) {
				System.out.println("=== [调试] 已达到最大输出限制(" + MAX_DEBUG_OUTPUT + "条)，后续不一致情况将不再显示 ===");
			}
		}
	}
	
	/**
	 * 转义字符串中的特殊字符，便于调试输出
	 */
	private static String escapeString(String str) {
		return str.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
	}
	
	/**
	 * 转义单个字符，便于调试输出
	 */
	private static String escapeChar(char c) {
		switch (c) {
			case '\n': return "\\n";
			case '\r': return "\\r";
			case '\t': return "\\t";
			case ' ': return "SPACE";
			default: return String.valueOf(c);
		}
	}

}
