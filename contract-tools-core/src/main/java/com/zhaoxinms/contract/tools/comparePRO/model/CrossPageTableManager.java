package com.zhaoxinms.contract.tools.comparePRO.model;

import java.util.*;

/**
 * 跨页表格关联管理器
 * 
 * 用于管理和追踪 MinerU 表格解析中的跨页表格关联关系。
 * 
 * 识别规则：
 * - 如果一个 type="table" 的 content_list 项满足以下条件，则认为是跨页表格的延续部分：
 *   1. table_caption 为空或不存在
 *   2. table_footnote 为空或不存在
 *   3. table_body 为空或不存在
 * 
 * @author zhaoxin
 * @version 1.0
 * @since 2025-10-13
 */
public class CrossPageTableManager {
    
    /**
     * 跨页表格组
     * 每个组包含一个主表格和多个跨页部分
     */
    public static class TableGroup {
        /** 表格组ID */
        public String groupId;
        
        /** 主表格信息（第一个完整的表格） */
        public TablePart mainTable;
        
        /** 跨页部分列表（按页码顺序） */
        public List<TablePart> continuationParts;
        
        /** 所有相关的页码（去重后） */
        public Set<Integer> allPages;
        
        public TableGroup(String groupId) {
            this.groupId = groupId;
            this.continuationParts = new ArrayList<>();
            this.allPages = new HashSet<>();
        }
        
        /**
         * 添加主表格
         */
        public void setMainTable(TablePart mainTable) {
            this.mainTable = mainTable;
            if (mainTable != null) {
                allPages.add(mainTable.pageIdx);
            }
        }
        
        /**
         * 添加跨页部分
         */
        public void addContinuationPart(TablePart part) {
            this.continuationParts.add(part);
            if (part != null) {
                allPages.add(part.pageIdx);
            }
        }
        
        /**
         * 获取所有表格部分（主表格 + 跨页部分）
         */
        public List<TablePart> getAllParts() {
            List<TablePart> all = new ArrayList<>();
            if (mainTable != null) {
                all.add(mainTable);
            }
            all.addAll(continuationParts);
            return all;
        }
        
        /**
         * 获取所有 bbox（按页码组织）
         */
        public Map<Integer, List<double[]>> getAllBboxesByPage() {
            Map<Integer, List<double[]>> bboxesByPage = new HashMap<>();
            for (TablePart part : getAllParts()) {
                if (!bboxesByPage.containsKey(part.pageIdx)) {
                    bboxesByPage.put(part.pageIdx, new ArrayList<>());
                }
                if (part.bbox != null) {
                    bboxesByPage.get(part.pageIdx).add(part.bbox);
                }
            }
            return bboxesByPage;
        }
    }
    
    /**
     * 表格部分信息
     */
    public static class TablePart {
        /** 在 content_list 中的索引 */
        public int contentListIndex;
        
        /** 页码（0-based） */
        public int pageIdx;
        
        /** bbox 坐标（图片坐标系） */
        public double[] bbox;
        
        /** 是否为主表格 */
        public boolean isMainTable;
        
        /** 文本内容 */
        public String text;
        
        public TablePart(int contentListIndex, int pageIdx, double[] bbox, boolean isMainTable, String text) {
            this.contentListIndex = contentListIndex;
            this.pageIdx = pageIdx;
            this.bbox = bbox;
            this.isMainTable = isMainTable;
            this.text = text;
        }
    }
    
    /** 所有表格组（按组ID索引） */
    private Map<String, TableGroup> tableGroups;
    
    /** bbox 到表格组的映射（用于快速查找） */
    private Map<String, String> bboxToGroupId;
    
    /** 当前文档的最后一个表格组（用于关联跨页部分） */
    private TableGroup lastTableGroup;
    
    public CrossPageTableManager() {
        this.tableGroups = new LinkedHashMap<>();
        this.bboxToGroupId = new HashMap<>();
        this.lastTableGroup = null;
    }
    
    /**
     * 添加一个表格项
     * 
     * @param contentListIndex content_list 中的索引
     * @param pageIdx 页码（0-based）
     * @param bbox bbox 坐标（图片坐标系）
     * @param hasCaption 是否有 table_caption
     * @param hasFootnote 是否有 table_footnote
     * @param hasBody 是否有 table_body
     * @param text 文本内容
     * @return 该表格项所属的表格组ID
     */
    public String addTableItem(int contentListIndex, int pageIdx, double[] bbox, 
                                boolean hasCaption, boolean hasFootnote, boolean hasBody, 
                                String text) {
        
        // 判断是否为跨页表格的延续部分
        boolean isContinuation = !hasCaption && !hasFootnote && !hasBody;
        
        if (isContinuation && lastTableGroup != null) {
            // 这是跨页表格的延续部分，添加到最后一个表格组
            TablePart part = new TablePart(contentListIndex, pageIdx, bbox, false, text);
            lastTableGroup.addContinuationPart(part);
            
            // 建立 bbox 到表格组的映射
            String bboxKey = createBboxKey(pageIdx, bbox);
            bboxToGroupId.put(bboxKey, lastTableGroup.groupId);
            
            return lastTableGroup.groupId;
            
        } else {
            // 这是一个新的主表格，创建新的表格组
            String groupId = "table_group_" + tableGroups.size();
            TableGroup group = new TableGroup(groupId);
            
            TablePart mainPart = new TablePart(contentListIndex, pageIdx, bbox, true, text);
            group.setMainTable(mainPart);
            
            tableGroups.put(groupId, group);
            lastTableGroup = group;
            
            // 建立 bbox 到表格组的映射
            String bboxKey = createBboxKey(pageIdx, bbox);
            bboxToGroupId.put(bboxKey, groupId);
            
            return groupId;
        }
    }
    
    /**
     * 根据 bbox 查找所属的表格组
     * 
     * @param pageIdx 页码（0-based 或 1-based，会自动尝试两种）
     * @param bbox bbox 坐标
     * @return 表格组，如果不属于任何表格组则返回 null
     */
    public TableGroup findTableGroupByBbox(int pageIdx, double[] bbox) {
        if (bbox == null) {
            return null;
        }
        
        // 尝试 0-based 和 1-based 两种页码
        String bboxKey0 = createBboxKey(pageIdx, bbox);
        String bboxKey1 = createBboxKey(pageIdx - 1, bbox);
        
        String groupId = bboxToGroupId.get(bboxKey0);
        if (groupId == null) {
            groupId = bboxToGroupId.get(bboxKey1);
        }
        
        return groupId != null ? tableGroups.get(groupId) : null;
    }
    
    /**
     * 获取所有表格组
     */
    public Collection<TableGroup> getAllTableGroups() {
        return tableGroups.values();
    }
    
    /**
     * 获取表格组数量
     */
    public int getTableGroupCount() {
        return tableGroups.size();
    }
    
    /**
     * 创建 bbox 的唯一键（用于映射查找）
     */
    private String createBboxKey(int pageIdx, double[] bbox) {
        if (bbox == null || bbox.length < 4) {
            return "";
        }
        // 使用页码和 bbox 坐标创建唯一键（保留2位小数以允许小的浮点误差）
        return String.format("%d_%.2f_%.2f_%.2f_%.2f", 
            pageIdx, bbox[0], bbox[1], bbox[2], bbox[3]);
    }
    
    /**
     * 重置管理器（用于处理新文档）
     */
    public void reset() {
        tableGroups.clear();
        bboxToGroupId.clear();
        lastTableGroup = null;
    }
    
    /**
     * 获取统计信息
     */
    public String getStatistics() {
        int totalGroups = tableGroups.size();
        int groupsWithContinuation = 0;
        int totalContinuationParts = 0;
        
        for (TableGroup group : tableGroups.values()) {
            if (!group.continuationParts.isEmpty()) {
                groupsWithContinuation++;
                totalContinuationParts += group.continuationParts.size();
            }
        }
        
        return String.format("表格组总数: %d, 包含跨页的组: %d, 跨页部分总数: %d", 
            totalGroups, groupsWithContinuation, totalContinuationParts);
    }
}

