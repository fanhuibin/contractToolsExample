package com.zhaoxinms.contract.tools.ocr.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zhaoxinms.contract.tools.compare.DiffUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 差异块类 - 表示文档比较中的一个差异单元
 *
 * @author zhaoxin
 * @version 1.0
 * @since 2025-01-14
 */
public class DiffBlock {
    public DiffType type;
    public int pageA; // 原文档A的页面号（1-based）
    public int pageB; // 新文档B的页面号（1-based）
    public int page; // 为了向后兼容，保留原有的page字段（主要用于显示）
    public List<double[]> oldBboxes; // DELETE操作对应的bbox（来自原文档A）
    public List<double[]> newBboxes; // INSERT操作对应的bbox（来自新文档B）
    public List<double[]> prevOldBboxes; // 上一个block的oldBboxes，用于同步跳转
    public List<double[]> prevNewBboxes; // 上一个block的newBboxes，用于同步跳转
    public String category;
    public String oldText;
    public String newText;
    // 新增：在全文字符序列中的首次索引（a/b），以及该 diff 的完整文本（a/b）
    public int indexA; // -1 表示在 A 中不存在
    public int indexB; // -1 表示在 B 中不存在
    public List<String> allTextA; // A 文档该 diff 各bbox的完整文本
    public List<String> allTextB; // B 文档该 diff 各bbox的完整文本
    
    // 新增：差异文本在完整文本中的位置标记
    public List<TextRange> diffRangesA; // A文档中差异文本的范围列表
    public List<TextRange> diffRangesB; // B文档中差异文本的范围列表

    // 新增：当前差别项在文本序列中的起始位置索引
    public int textStartIndexA; // 差别项在文档A文本中的起始字符索引，-1表示不存在
    public int textStartIndexB; // 差别项在文档B文本中的起始字符索引，-1表示不存在

    // 新增：对应的谷歌diff算法生成的原始Diff对象
    public DiffUtil.Diff originalDiff;

    // 新增：支持内嵌DiffBlock，用于表示复杂合并后的结构
    public List<DiffBlock> nestedBlocks;

    /**
     * 文本范围类 - 表示差异文本在完整文本中的位置
     */
    public static class TextRange {
        public int start; // 起始位置（包含）
        public int end;   // 结束位置（不包含）
        public String type; // 差异类型：DELETE, INSERT, MODIFY
        
        public TextRange(int start, int end, String type) {
            this.start = start;
            this.end = end;
            this.type = type;
        }
        
        public TextRange() {}
    }

    /**
     * 差异类型枚举
     */
    public enum DiffType {
        MODIFIED, ADDED, DELETED, IGNORED
    }

    /**
     * 创建DiffBlock的静态工厂方法
     */
    public static DiffBlock of(DiffType type, int pageA, int pageB, List<double[]> oldBboxes, List<double[]> newBboxes,
            String category, String oldText, String newText) {
        DiffBlock r = new DiffBlock();
        r.type = type;
        r.pageA = pageA;
        r.pageB = pageB;
        r.page = pageA; // 向后兼容，使用pageA作为主要页面
        r.oldBboxes = oldBboxes;
        r.newBboxes = newBboxes;
        r.category = category;
        r.oldText = oldText;
        r.newText = newText;
        // 初始化新的索引字段为默认值
        r.textStartIndexA = -1;
        r.textStartIndexB = -1;
        r.originalDiff = null;
        r.prevOldBboxes = null;
        r.prevNewBboxes = null;
        return r;
    }

    /**
     * 向后兼容的创建DiffBlock的静态工厂方法（使用单个page）
     */
    public static DiffBlock of(DiffType type, int page, List<double[]> oldBboxes, List<double[]> newBboxes,
            String category, String oldText, String newText) {
        return of(type, page, page, oldBboxes, newBboxes, category, oldText, newText);
    }

    /**
     * 支持设置文本起始索引的of方法
     */
    public static DiffBlock of(DiffType type, int pageA, int pageB, List<double[]> oldBboxes, List<double[]> newBboxes,
            String category, String oldText, String newText, int textStartIndexA, int textStartIndexB) {
        DiffBlock r = of(type, pageA, pageB, oldBboxes, newBboxes, category, oldText, newText);
        r.textStartIndexA = textStartIndexA;
        r.textStartIndexB = textStartIndexB;
        return r;
    }

    /**
     * 向后兼容的支持设置文本起始索引的of方法
     */
    public static DiffBlock of(DiffType type, int page, List<double[]> oldBboxes, List<double[]> newBboxes,
            String category, String oldText, String newText, int textStartIndexA, int textStartIndexB) {
        return of(type, page, page, oldBboxes, newBboxes, category, oldText, newText, textStartIndexA, textStartIndexB);
    }

    /**
     * 支持设置文本起始索引和原始Diff对象的of方法
     */
    public static DiffBlock of(DiffType type, int pageA, int pageB, List<double[]> oldBboxes, List<double[]> newBboxes,
            String category, String oldText, String newText, int textStartIndexA, int textStartIndexB, DiffUtil.Diff originalDiff) {
        DiffBlock r = of(type, pageA, pageB, oldBboxes, newBboxes, category, oldText, newText, textStartIndexA, textStartIndexB);
        r.originalDiff = originalDiff;
        return r;
    }

    /**
     * 向后兼容的支持设置文本起始索引和原始Diff对象的of方法
     */
    public static DiffBlock of(DiffType type, int page, List<double[]> oldBboxes, List<double[]> newBboxes,
            String category, String oldText, String newText, int textStartIndexA, int textStartIndexB, DiffUtil.Diff originalDiff) {
        return of(type, page, page, oldBboxes, newBboxes, category, oldText, newText, textStartIndexA, textStartIndexB, originalDiff);
    }

    /**
     * 为了向后兼容，提供单个bbox的方法
     */
    public static DiffBlock of(DiffType type, int page, double[] bbox, String category, String oldText, String newText) {
        List<double[]> bboxes = new ArrayList<>();
        bboxes.add(bbox);
        if (type == DiffType.DELETED) {
            return of(type, page, page, bboxes, new ArrayList<>(), category, oldText, newText);
        } else if (type == DiffType.ADDED) {
            return of(type, page, page, new ArrayList<>(), bboxes, category, oldText, newText);
        } else {
            return of(type, page, page, bboxes, bboxes, category, oldText, newText);
        }
    }

    /**
     * 获取主要bbox（用于排序和比较）
     */
    public double[] getPrimaryBbox() {
        if (oldBboxes != null && !oldBboxes.isEmpty()) {
            return oldBboxes.get(0);
        }
        if (newBboxes != null && !newBboxes.isEmpty()) {
            return newBboxes.get(0);
        }
        return null;
    }

    /**
     * 获取所有bbox（向后兼容）
     */
    public List<double[]> getAllBboxes() {
        List<double[]> all = new ArrayList<>();
        if (oldBboxes != null)
            all.addAll(oldBboxes);
        if (newBboxes != null)
            all.addAll(newBboxes);
        return all;
    }

    /**
     * 转换为JSON格式
     */
    public JsonNode toJson(ObjectNode n) {
        n.put("type", type.name());
        n.put("page", page);
        n.put("pageA", pageA);
        n.put("pageB", pageB);

        // 处理bbox信息
        if (oldBboxes != null && !oldBboxes.isEmpty()) {
            if (oldBboxes.size() == 1) {
                // 单个old bbox
                ArrayNode b = ((ObjectNode) n).arrayNode();
                b.add(oldBboxes.get(0)[0]).add(oldBboxes.get(0)[1]).add(oldBboxes.get(0)[2])
                        .add(oldBboxes.get(0)[3]);
                n.set("oldBbox", b);
            } else {
                // 多个old bbox
                ArrayNode bboxArray = ((ObjectNode) n).arrayNode();
                for (double[] bbox : oldBboxes) {
                    ArrayNode b = ((ObjectNode) n).arrayNode();
                    b.add(bbox[0]).add(bbox[1]).add(bbox[2]).add(bbox[3]);
                    bboxArray.add(b);
                }
                n.set("oldBboxes", bboxArray);
            }
        }

        if (newBboxes != null && !newBboxes.isEmpty()) {
            if (newBboxes.size() == 1) {
                // 单个new bbox
                ArrayNode b = ((ObjectNode) n).arrayNode();
                b.add(newBboxes.get(0)[0]).add(newBboxes.get(0)[1]).add(newBboxes.get(0)[2])
                        .add(newBboxes.get(0)[3]);
                n.set("newBbox", b);
            } else {
                // 多个new bbox
                ArrayNode bboxArray = ((ObjectNode) n).arrayNode();
                for (double[] bbox : newBboxes) {
                    ArrayNode b = ((ObjectNode) n).arrayNode();
                    b.add(bbox[0]).add(bbox[1]).add(bbox[2]).add(bbox[3]);
                    bboxArray.add(b);
                }
                n.set("newBboxes", bboxArray);
            }
        }

        // 为了向后兼容，也提供统一的bboxes字段
        List<double[]> allBboxes = getAllBboxes();
        if (!allBboxes.isEmpty()) {
            if (allBboxes.size() == 1) {
                ArrayNode b = ((ObjectNode) n).arrayNode();
                b.add(allBboxes.get(0)[0]).add(allBboxes.get(0)[1]).add(allBboxes.get(0)[2])
                        .add(allBboxes.get(0)[3]);
                n.set("bbox", b);
            } else {
                ArrayNode bboxArray = ((ObjectNode) n).arrayNode();
                for (double[] bbox : allBboxes) {
                    ArrayNode b = ((ObjectNode) n).arrayNode();
                    b.add(bbox[0]).add(bbox[1]).add(bbox[2]).add(bbox[3]);
                    bboxArray.add(b);
                }
                n.set("bboxes", bboxArray);
            }
        }

        n.put("category", category);
        n.put("oldText", oldText);
        n.put("newText", newText);
        n.put("indexA", indexA);
        n.put("indexB", indexB);
        n.put("textStartIndexA", textStartIndexA);
        n.put("textStartIndexB", textStartIndexB);

        // 处理上一个block的bbox信息
        if (prevOldBboxes != null && !prevOldBboxes.isEmpty()) {
            if (prevOldBboxes.size() == 1) {
                ArrayNode b = ((ObjectNode) n).arrayNode();
                b.add(prevOldBboxes.get(0)[0]).add(prevOldBboxes.get(0)[1]).add(prevOldBboxes.get(0)[2])
                        .add(prevOldBboxes.get(0)[3]);
                n.set("prevOldBbox", b);
            } else {
                ArrayNode bboxArray = ((ObjectNode) n).arrayNode();
                for (double[] bbox : prevOldBboxes) {
                    ArrayNode b = ((ObjectNode) n).arrayNode();
                    b.add(bbox[0]).add(bbox[1]).add(bbox[2]).add(bbox[3]);
                    bboxArray.add(b);
                }
                n.set("prevOldBboxes", bboxArray);
            }
        }

        if (prevNewBboxes != null && !prevNewBboxes.isEmpty()) {
            if (prevNewBboxes.size() == 1) {
                ArrayNode b = ((ObjectNode) n).arrayNode();
                b.add(prevNewBboxes.get(0)[0]).add(prevNewBboxes.get(0)[1]).add(prevNewBboxes.get(0)[2])
                        .add(prevNewBboxes.get(0)[3]);
                n.set("prevNewBbox", b);
            } else {
                ArrayNode bboxArray = ((ObjectNode) n).arrayNode();
                for (double[] bbox : prevNewBboxes) {
                    ArrayNode b = ((ObjectNode) n).arrayNode();
                    b.add(bbox[0]).add(bbox[1]).add(bbox[2]).add(bbox[3]);
                    bboxArray.add(b);
                }
                n.set("prevNewBboxes", bboxArray);
            }
        }

        // 处理完整文本信息
        if (allTextA != null && !allTextA.isEmpty()) {
            if (allTextA.size() == 1) {
                n.put("allTextA", allTextA.get(0));
            } else {
                                    ArrayNode allTextAArray = ((ObjectNode) n).arrayNode();
                for (String text : allTextA) {
                    allTextAArray.add(text);
                }
                n.set("allTextA", allTextAArray);
            }
        }

        if (allTextB != null && !allTextB.isEmpty()) {
            if (allTextB.size() == 1) {
                n.put("allTextB", allTextB.get(0));
            } else {
                                    ArrayNode allTextBArray = ((ObjectNode) n).arrayNode();
                for (String text : allTextB) {
                    allTextBArray.add(text);
                }
                n.set("allTextB", allTextBArray);
            }
        }

        // 添加原始Diff对象信息
        if (originalDiff != null) {
            ObjectNode diffNode = ((ObjectNode) n).objectNode();
            diffNode.put("operation", originalDiff.operation.name());
            diffNode.put("text", originalDiff.text);
            n.set("originalDiff", diffNode);
        }

        return n;
    }
}
