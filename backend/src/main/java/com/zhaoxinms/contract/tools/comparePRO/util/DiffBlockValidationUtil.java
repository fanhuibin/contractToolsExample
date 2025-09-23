package com.zhaoxinms.contract.tools.comparePRO.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
// 移除不再使用的Arrays导入
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.imageio.ImageIO;

// 移除不再使用的PDFBox导入，直接使用已保存的图片
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.zhaoxinms.contract.tools.comparePRO.model.DiffBlock;
import com.zhaoxinms.contract.tools.comparePRO.client.RapidOcrClient;
import com.zhaoxinms.contract.tools.comparePRO.service.RapidOcrService;
import com.zhaoxinms.contract.tools.comparePRO.config.ZxOcrConfig;
import com.zhaoxinms.contract.tools.config.ZxcmConfig;
import com.zhaoxinms.contract.tools.comparePRO.util.TextSimilarityCalculator;

/**
 * DiffBlock验证工具类
 * 
 * 主要功能：
 * 1. 分析DiffBlock合并结果，决定是否启动RapidOCR校验
 * 2. 基于bbox截取PDF页面图片
 * 3. 使用RapidOCR识别截取的图片内容
 * 4. 比对识别结果与原始DiffBlock内容
 */
@Component
public class DiffBlockValidationUtil {

    private static final Logger logger = LoggerFactory.getLogger(DiffBlockValidationUtil.class);
    
    /**
     * 触发RapidOCR校验的字符数阈值
     */
    private static final int TEXT_LENGTH_THRESHOLD = 30;
    
    /**
     * 触发RapidOCR校验的bbox数量上限（1或2个bbox的才验证）
     */
    private static final int BBOX_COUNT_MAX_THRESHOLD = 2;
    
    /**
     * Debug 模式开关，控制详细日志输出
     */
    private boolean debugMode = false;
    
    // 不再需要DEFAULT_DPI，直接使用已保存的图片

    @Autowired(required = false)
    private RapidOcrService rapidOcrService;
    
    @Autowired
    private ZxOcrConfig zxOcrConfig;
    
    @Autowired
    private ZxcmConfig zxcmConfig;
    
    // OCR验证专用线程池
    private ExecutorService ocrValidationExecutor;
    private static final int DEFAULT_THREAD_POOL_SIZE = 4;
    private static final String THREAD_NAME_PREFIX = "RapidOCR-Validation-";

    /**
     * 初始化线程池
     */
    @PostConstruct
    public void initializeThreadPool() {
        ocrValidationExecutor = Executors.newFixedThreadPool(DEFAULT_THREAD_POOL_SIZE, r -> {
            Thread t = new Thread(r, THREAD_NAME_PREFIX + System.currentTimeMillis());
            t.setDaemon(false);
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        });
        logger.info("🚀 RapidOCR验证线程池初始化完成，线程数: {}", DEFAULT_THREAD_POOL_SIZE);
    }
    
    /**
     * 销毁线程池
     */
    @PreDestroy
    public void destroyThreadPool() {
        if (ocrValidationExecutor != null && !ocrValidationExecutor.isShutdown()) {
            logger.info("⚡ 正在关闭RapidOCR验证线程池...");
            ocrValidationExecutor.shutdown();
            try {
                if (!ocrValidationExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    logger.warn("线程池未在30秒内完成关闭，强制关闭");
                    ocrValidationExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                logger.warn("等待线程池关闭时被中断");
                ocrValidationExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            logger.info("✅ RapidOCR验证线程池已关闭");
        }
    }
    
    /**
     * 设置 debug 模式
     * @param debugMode true 启用详细日志输出，false 只输出关键日志
     */
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }
    
    /**
     * 分析DiffBlock并决定是否需要RapidOCR校验
     * 
     * @param mergedBlocks 合并后的差异块列表
     * @param taskId 任务ID
     * @param debugMode 是否启用 debug 模式
     * @param totalPages 文档总页数
     * @return 校验结果
     */
    public DiffBlockValidationResult analyzeDiffBlocks(List<DiffBlock> mergedBlocks, String taskId, 
            boolean debugMode, int totalPages) {
        
        this.debugMode = debugMode;
        
        if (debugMode) {
            logger.info("🔍 开始分析DiffBlock，merged数量: {}, 幻觉校验配置: {}", 
                       mergedBlocks.size(), zxOcrConfig.getHallucinationValidation());
        }
        
        DiffBlockValidationResult result = new DiffBlockValidationResult();
        result.setTaskId(taskId);
        result.setTotalMergedCount(mergedBlocks.size());
        
        // 检查是否启用了幻觉校验功能
        if (!zxOcrConfig.getHallucinationValidation().isEnabled()) {
            if (debugMode) {
                logger.info("⚠️ 幻觉校验功能已禁用，跳过RapidOCR校验");
            }
            result.setEligibleBlockCount(0);
            result.setTotalPages(totalPages);
            result.setPageThreshold(0);
            result.setValidationTriggered(false);
            result.setValidationItems(new ArrayList<>());
            result.setRemovedBlockCount(0);
            return result;
        }
        
        // 第一步：筛选符合条件的DiffBlock和它们在原始数组中的索引
        List<Integer> eligibleIndices = new ArrayList<>();
        List<DiffBlock> eligibleBlocks = filterEligibleBlocksWithIndices(mergedBlocks, eligibleIndices);
        if (debugMode) {
            logger.info("符合初步条件的DiffBlock数量: {}", eligibleBlocks.size());
        }
        
        // 第二步：使用配置的页数阈值倍数
        int pageThreshold = Math.max(1, totalPages * zxOcrConfig.getHallucinationValidation().getPageThresholdMultiplier());
        
        if (debugMode) {
            logger.debug("页数计算详情: merged块数={}, 传入页数={}, 页数阈值={}", 
                mergedBlocks.size(), totalPages, pageThreshold);
            // 输出每个块的页码信息用于调试
            for (int i = 0; i < mergedBlocks.size(); i++) {
                DiffBlock block = mergedBlocks.get(i);
                logger.debug("块{}: pageA={}, pageB={}, page={}", 
                    i, block.pageA, block.pageB, block.page);
            }
        }
        
        boolean validationTriggered = eligibleBlocks.size() < pageThreshold;
        
        // 设置结果字段
        result.setEligibleBlockCount(eligibleBlocks.size());
        result.setTotalPages(totalPages);
        result.setPageThreshold(pageThreshold);
        result.setValidationTriggered(validationTriggered);
        
        if (debugMode) {
            logger.info("总页数: {}, 页数阈值: {}, 符合条件的块数: {}, 是否触发验证: {}", 
                    totalPages, pageThreshold, eligibleBlocks.size(), validationTriggered);
        }
        
        if (!validationTriggered) {
            if (debugMode) {
                logger.info("符合条件的DiffBlock数量({})达到或超过页数阈值({})，跳过RapidOCR校验", 
                        eligibleBlocks.size(), pageThreshold);
            }
            // 即使没有触发验证，也要设置原始列表作为过滤后的列表
            result.setFilteredBlocks(mergedBlocks);
            result.setRemovedBlockCount(0);
            return result;
        }
        
        if (rapidOcrService == null || !rapidOcrService.isServiceAvailable()) {
            logger.warn("RapidOCR服务不可用，跳过校验");
            result.setValidationSkipped(true);
            result.setSkipReason("RapidOCR服务不可用");
            // 即使服务不可用，也要设置原始列表作为过滤后的列表
            result.setFilteredBlocks(mergedBlocks);
            result.setRemovedBlockCount(0);
            return result;
        }
        
        if (debugMode) {
            logger.info("启动RapidOCR校验过程...");
        }
        
        try {
            // 创建子图片目录
            createSubImageDirectories(taskId);
            
            // 处理符合条件的DiffBlock
            List<DiffBlockValidationItem> validationItems = new ArrayList<>();
            
            for (int i = 0; i < eligibleBlocks.size(); i++) {
                DiffBlock block = eligibleBlocks.get(i);
                int originalIndex = eligibleIndices.get(i);
                if (debugMode) {
                    logger.info("处理符合条件的DiffBlock {}/{}: {} (原始索引: {})", i + 1, eligibleBlocks.size(), block.type, originalIndex);
                }
                
                try {
                    DiffBlockValidationItem item = processDiffBlock(block, taskId, originalIndex);
                    if (item != null) {
                        validationItems.add(item);
                    }
                } catch (Exception e) {
                    logger.error("处理DiffBlock {}失败", originalIndex, e);
                }
            }
            
            result.setValidationItems(validationItems);
            result.setValidationSuccess(true);
            
            // 创建过滤后的DiffBlock列表（移除验证通过的幻觉块）
            List<DiffBlock> filteredBlocks = createFilteredBlocks(mergedBlocks, validationItems);
            result.setFilteredBlocks(filteredBlocks);
            
            // 统计被移除的块数量
            int removedCount = mergedBlocks.size() - filteredBlocks.size();
            result.setRemovedBlockCount(removedCount);
            
            if (debugMode) {
                logger.info("RapidOCR校验完成，处理了{}个DiffBlock，移除了{}个幻觉块", validationItems.size(), removedCount);
            }
            
        } catch (Exception e) {
            logger.error("RapidOCR校验过程失败", e);
            result.setValidationSuccess(false);
            result.setErrorMessage(e.getMessage());
            // 即使验证失败，也要设置原始列表作为过滤后的列表
            result.setFilteredBlocks(mergedBlocks);
            result.setRemovedBlockCount(0);
        }
        
        return result;
    }
    
    /**
     * 筛选符合条件的DiffBlock
     * 条件1：差别项目小于10个字
     * 条件2：bbox只有1或2个
     */
    private List<DiffBlock> filterEligibleBlocks(List<DiffBlock> blocks) {
        List<DiffBlock> eligibleBlocks = new ArrayList<>();
        
        for (DiffBlock block : blocks) {
            if (isBlockEligible(block)) {
                eligibleBlocks.add(block);
                if (debugMode) {
                    logger.debug("DiffBlock符合条件: type={}, textLength={}, bboxCount={}", 
                            block.type, getTextLength(block), getBboxCount(block));
                }
            }
        }
        
        return eligibleBlocks;
    }
    
    /**
     * 判断单个DiffBlock是否符合验证条件
     */
    private boolean isBlockEligible(DiffBlock block) {
        // 条件1：差别项目小于30个字
        int textLength = getTextLength(block);
        if (textLength >= TEXT_LENGTH_THRESHOLD) {
            return false;
        }
        
        // 条件2：bbox只有1或2个
        int bboxCount = getBboxCount(block);
        if (bboxCount < 1 || bboxCount > BBOX_COUNT_MAX_THRESHOLD) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 获取DiffBlock的文本长度
     * 新增判断newText长度，删除判断oldText长度
     */
    private int getTextLength(DiffBlock block) {
        if (block.type == DiffBlock.DiffType.ADDED) {
            // 新增：判断newText长度
            return block.newText != null ? block.newText.length() : 0;
        } else if (block.type == DiffBlock.DiffType.DELETED) {
            // 删除：判断oldText长度
            return block.oldText != null ? block.oldText.length() : 0;
        } else {
            // 其他类型（如EQUAL, REPLACED等）：取两者的最大值
            int oldLength = block.oldText != null ? block.oldText.length() : 0;
            int newLength = block.newText != null ? block.newText.length() : 0;
            return Math.max(oldLength, newLength);
        }
    }
    
    /**
     * 获取DiffBlock的bbox数量
     */
    private int getBboxCount(DiffBlock block) {
        int oldBboxCount = block.oldBboxes != null ? block.oldBboxes.size() : 0;
        int newBboxCount = block.newBboxes != null ? block.newBboxes.size() : 0;
        return Math.max(oldBboxCount, newBboxCount);
    }
    
    /**
     * 计算总页数
     */
    private int calculateTotalPages(List<DiffBlock> blocks) {
        int maxPage = 0;
        
        for (DiffBlock block : blocks) {
            // 检查pageA
            if (block.pageA != null) {
                for (Integer page : block.pageA) {
                    if (page != null && page > maxPage) {
                        maxPage = page;
                    }
                }
            }
            
            // 检查pageB
            if (block.pageB != null) {
                for (Integer page : block.pageB) {
                    if (page != null && page > maxPage) {
                        maxPage = page;
                    }
                }
            }
            
            // 兼容旧版本：检查单一page字段
            if (block.page > 0 && block.page > maxPage) {
                maxPage = block.page;
            }
        }
        
        // 如果没有找到任何页码信息，返回1作为默认值
        return Math.max(1, maxPage);
    }
    
    /**
     * 处理单个DiffBlock
     */
    private DiffBlockValidationItem processDiffBlock(DiffBlock block, String taskId, int blockIndex) throws IOException {
        
        DiffBlockValidationItem item = new DiffBlockValidationItem();
        item.setBlockIndex(blockIndex);
        item.setOperationType(block.type.toString());
        item.setOriginalOldText(String.join(" ", block.allTextA != null ? block.allTextA : List.of()));
        item.setOriginalNewText(String.join(" ", block.allTextB != null ? block.allTextB : List.of()));
        
        // 获取bbox数量
        int bboxCount = getBboxCount(block);
        if (debugMode) {
            logger.info("========== DiffBlock {} 验证开始 ==========", blockIndex);
            logger.info("操作类型: {}, bbox数量: {}", block.type, bboxCount);
        }
        
        // 处理旧文档的bbox
        String recognizedOldText = "";
        List<String> recognizedOldTexts = new ArrayList<>();
        if (block.oldBboxes != null && !block.oldBboxes.isEmpty() && block.pageA != null) {
            if (debugMode) {
                logger.debug("处理旧文档bbox，数量: {}", block.oldBboxes.size());
            }
            List<String> oldImagePaths = extractBboxImages(block.oldBboxes, block.pageA, 
                    taskId, "old", blockIndex);
            item.setOldImagePaths(oldImagePaths);
            
            // 分别识别每个bbox的内容
            recognizedOldTexts = recognizeIndividualImages(oldImagePaths);
            recognizedOldText = String.join(" ", recognizedOldTexts); // 合并用于兼容性
            item.setRecognizedOldText(recognizedOldText);
        }
        
        // 处理新文档的bbox
        String recognizedNewText = "";
        List<String> recognizedNewTexts = new ArrayList<>();
        if (block.newBboxes != null && !block.newBboxes.isEmpty() && block.pageB != null) {
            if (debugMode) {
                logger.debug("处理新文档bbox，数量: {}", block.newBboxes.size());
            }
            List<String> newImagePaths = extractBboxImages(block.newBboxes, block.pageB, 
                    taskId, "new", blockIndex);
            item.setNewImagePaths(newImagePaths);
            
            // 分别识别每个bbox的内容
            recognizedNewTexts = recognizeIndividualImages(newImagePaths);
            recognizedNewText = String.join(" ", recognizedNewTexts); // 合并用于兼容性
            item.setRecognizedNewText(recognizedNewText);
        }
        
        // 输出bbox提取内容和差异文本
        String diffText = getDiffText(block);
        if (debugMode) {
            logger.info("bbox提取内容: 旧文档=\"{}\", 新文档=\"{}\"", recognizedOldText, recognizedNewText);
            logger.info("差异文本: {}", diffText);
        }
        
        // 根据bbox数量进行特殊验证
        ValidationResult validationResult = performSpecialValidation(block, bboxCount, recognizedOldTexts, recognizedNewTexts, diffText, blockIndex);
        
        // 设置验证结果
        item.setValidationPassed(validationResult.isPassed());
        item.setValidationMethod(validationResult.getMethod());
        
        // TODO: 基于识别结果和原DiffBlock做比对处理数据（先注释掉，一会完善）
        // item.setComparisonResult(compareRecognizedWithOriginal(item));
        
        if (debugMode) {
            logger.info("========== DiffBlock {} 验证结束 ==========", blockIndex);
        }
        return item;
    }
    
    /**
     * 获取差异文本（根据操作类型）
     */
    private String getDiffText(DiffBlock block) {
        if (block.type == DiffBlock.DiffType.ADDED) {
            return block.newText != null ? block.newText : "";
        } else if (block.type == DiffBlock.DiffType.DELETED) {
            return block.oldText != null ? block.oldText : "";
        } else {
            // 对于其他类型，显示新旧文本
            String oldText = block.oldText != null ? block.oldText : "";
            String newText = block.newText != null ? block.newText : "";
            return "旧: \"" + oldText + "\" -> 新: \"" + newText + "\"";
        }
    }
    
    /**
     * 验证结果内部类
     */
    private static class ValidationResult {
        private boolean passed;
        private String method;
        
        public ValidationResult(boolean passed, String method) {
            this.passed = passed;
            this.method = method;
        }
        
        public boolean isPassed() { return passed; }
        public String getMethod() { return method; }
    }
    
    /**
     * 通用的文本组合验证逻辑（单bbox和双bbox共用）
     * @return 验证结果，包含是否通过和验证方法
     */
    private ValidationResult performSpecialValidation(DiffBlock block, int bboxCount, 
            List<String> recognizedOldTexts, List<String> recognizedNewTexts, String diffText, int blockIndex) {
        
        if (debugMode) {
            logger.info("--- 通用验证 (Block {}, bbox数量: {}) ---", blockIndex, bboxCount);
        }
        
        if (block.type == DiffBlock.DiffType.ADDED) {
            // 新增：验证 合并的bbox识别内容 + 差异文本 = allTextB
            String allTextB = String.join("", block.allTextB != null ? block.allTextB : List.of());
            String mergedBboxText = String.join("", recognizedNewTexts); // 合并所有bbox文本
            
            if (debugMode) {
                logger.info("新增验证: 合并bbox文本=\"{}\", 差异文本=\"{}\", 目标allTextB=\"{}\"", 
                        mergedBboxText, diffText, allTextB);
            }
            
            // 使用通用算法验证
            ValidationResult result = tryUniversalTextCombination(mergedBboxText, diffText, allTextB, "新增", bboxCount, recognizedNewTexts);
            
            if (!result.isPassed()) {
                logger.warn("✗ 新增验证失败: 无法找到匹配的组合方式");
            }
            
            return result;
            
        } else if (block.type == DiffBlock.DiffType.DELETED) {
            // 删除：验证 合并的bbox识别内容 + 差异文本 = allTextA
            String allTextA = String.join("", block.allTextA != null ? block.allTextA : List.of());
            String mergedBboxText = String.join("", recognizedOldTexts); // 合并所有bbox文本
            
            if (debugMode) {
                logger.info("删除验证: 合并bbox文本=\"{}\", 差异文本=\"{}\", 目标allTextA=\"{}\"", 
                        mergedBboxText, diffText, allTextA);
            }
            
            // 使用通用算法验证
            ValidationResult result = tryUniversalTextCombination(mergedBboxText, diffText, allTextA, "删除", bboxCount, recognizedOldTexts);
            
            if (!result.isPassed()) {
                logger.warn("✗ 删除验证失败: 无法找到匹配的组合方式");
            }
            
            return result;
        }
        
        // 其他类型的DiffBlock不验证
        return new ValidationResult(false, "不支持的操作类型");
    }
    
    /**
     * 简化的通用文本验证算法
     * 核心思路：目标文本 - 差异文本 = bbox文本
     * 支持OCR识别错误导致的空格差异容错处理
     */
    private ValidationResult tryUniversalTextCombination(String mergedBboxText, String diffText, String targetText, String validationType, int bboxCount, List<String> originalBboxTexts) {
        if (mergedBboxText == null) mergedBboxText = "";
        if (diffText == null) diffText = "";
        if (targetText == null) targetText = "";
        
        if (debugMode) {
            logger.debug("开始简化验证算法: 目标文本 - 差异文本 = bbox文本");
            logger.debug("bbox文本: \"{}\"", mergedBboxText);
            logger.debug("差异文本: \"{}\"", diffText);
            logger.debug("目标文本: \"{}\"", targetText);
        }
        
        // 首先尝试精确匹配
        ValidationResult exactMatch = trySubtractionMatch(mergedBboxText, diffText, targetText, validationType, false, bboxCount, originalBboxTexts);
        if (exactMatch.isPassed()) {
            return exactMatch;
        }
        
        // 如果精确匹配失败，尝试忽略空格的匹配
        if (debugMode) {
            logger.debug("精确匹配失败，尝试忽略空格的容错匹配...");
        }
        return trySubtractionMatch(mergedBboxText, diffText, targetText, validationType, true, bboxCount, originalBboxTexts);
    }
    
    /**
     * 简化的减法匹配验证
     * 核心算法：目标文本 - 差异文本 = bbox文本
     * 
     * @param mergedBboxText 合并的bbox识别文本
     * @param diffText 差异文本
     * @param targetText 目标文本（allTextA或allTextB）
     * @param validationType 验证类型（用于日志）
     * @param spaceNormalization 是否启用空格标准化
     * @param bboxCount bbox数量
     * @param originalBboxTexts 原始的bbox文本列表
     * @return 验证是否成功
     */
    private ValidationResult trySubtractionMatch(String mergedBboxText, String diffText, String targetText, String validationType, boolean spaceNormalization, int bboxCount, List<String> originalBboxTexts) {
        String workingBboxText = mergedBboxText;
        String workingDiffText = diffText;
        String workingTargetText = targetText;
        
        // 如果启用空格标准化，对所有文本进行标准化
        if (spaceNormalization) {
            workingBboxText = normalizeSpaces(mergedBboxText);
            workingDiffText = normalizeSpaces(diffText);
            workingTargetText = normalizeSpaces(targetText);
            
            if (debugMode) {
                logger.debug("空格标准化后:");
                logger.debug("  bbox文本: \"{}\"", workingBboxText);
                logger.debug("  差异文本: \"{}\"", workingDiffText);
                logger.debug("  目标文本: \"{}\"", workingTargetText);
            }
        }
        
        // 核心验证：尝试移除目标文本中每一个差异文本出现位置，看是否有匹配的
        return tryRemoveAllOccurrences(workingTargetText, workingDiffText, workingBboxText, validationType, spaceNormalization, bboxCount, originalBboxTexts);
    }
    
    /**
     * 尝试移除目标文本中差异文本的所有出现位置，验证是否有匹配的
     */
    private ValidationResult tryRemoveAllOccurrences(String targetText, String diffText, String bboxText, String validationType, boolean spaceNormalization, int bboxCount, List<String> originalBboxTexts) {
        if (diffText.isEmpty()) {
            // 如果差异文本为空，直接比较目标文本和bbox文本
            if (targetText.equals(bboxText)) {
                String matchType = spaceNormalization ? "空格容错" : "精确";
                String method = matchType + "减法匹配(差异文本为空)";
                if (debugMode) {
                    logger.info("✓ {} 验证通过 ({}减法匹配): 差异文本为空，目标文本 = bbox文本", validationType, matchType);
                }
                return new ValidationResult(true, method);
            }
            return new ValidationResult(false, "差异文本为空但文本不匹配");
        }
        
        // 查找差异文本在目标文本中的所有出现位置
        List<Integer> occurrences = findAllOccurrences(targetText, diffText);
        
        if (occurrences.isEmpty()) {
            if (debugMode) {
                logger.debug("无法在目标文本中找到差异文本: \"{}\"", diffText);
            }
            return new ValidationResult(false, "无法在目标文本中找到差异文本");
        }
        
        if (debugMode) {
            logger.debug("差异文本\"{}\"在目标文本中出现{}次，位置: {}", diffText, occurrences.size(), occurrences);
        }
        
        // 尝试移除每一个出现位置的差异文本，看移除后的剩余文本是否能匹配bbox文本
        for (int i = 0; i < occurrences.size(); i++) {
            int diffIndex = occurrences.get(i);
            String before = targetText.substring(0, diffIndex);
            String after = targetText.substring(diffIndex + diffText.length());
            String remaining = before + after;
            
            if (debugMode) {
                logger.debug("尝试移除第{}个出现位置(索引{})，剩余文本: \"{}\"", i + 1, diffIndex, remaining);
            }
            
            // 检查移除后的剩余文本是否匹配bbox文本
            if (remaining.equals(bboxText)) {
                String matchType = spaceNormalization ? "空格容错" : "精确";
                String method = matchType + "减法匹配(移除第" + (i + 1) + "个差异)";
                if (debugMode) {
                    logger.info("✓ {} 验证通过 ({}减法匹配): 移除第{}个\"{}\"(位置{}) = bbox文本", 
                            validationType, matchType, i + 1, diffText, diffIndex);
                }
                return new ValidationResult(true, method);
            }
            
            // 新规则1：单个bbox且大于5个字符时，验证尾部5个字符匹配（限制：差异文本必须在完整文本最后面）
            boolean tailMatched = tryTailMatchValidation(targetText, remaining, bboxText, validationType, spaceNormalization, bboxCount, i + 1, diffText, diffIndex);
            if (tailMatched) {
                String matchType = spaceNormalization ? "空格容错" : "精确";
                return new ValidationResult(true, matchType + "尾部5字符匹配");
            }
            
            // 新规则2：双bbox时，验证bbox1尾部2字符+bbox2头部2字符 = 差异文本出现处前2字符+差异文本出现处后2字符
            boolean dualMatched = tryDualBboxTailHeadMatch(targetText, bboxText, validationType, spaceNormalization, bboxCount, originalBboxTexts, i + 1, diffText, diffIndex);
            if (dualMatched) {
                String matchType = spaceNormalization ? "空格容错" : "精确";
                return new ValidationResult(true, matchType + "双bbox尾头匹配");
            }
            
            // 新规则3：小文本相似度验证（适用于小于200字符的文本），该算法弊大于利，经常会导致丢失细节。
//            ValidationResult similarityResult = trySimilarityValidation(targetText, bboxText, validationType, spaceNormalization, diffText, i + 1);
//            if (similarityResult.passed) {
//                return similarityResult;
//            }
        }
        
        String matchType = spaceNormalization ? "容错" : "精确";
        if (debugMode) {
            logger.debug("✗ {}减法匹配失败: 尝试移除{}个出现位置都无法匹配bbox文本", 
                    matchType, occurrences.size());
        }
        
        return new ValidationResult(false, matchType + "减法匹配失败");
    }
    
    /**
     * 查找目标文本中所有差异文本的出现位置
     */
    private List<Integer> findAllOccurrences(String targetText, String diffText) {
        List<Integer> occurrences = new ArrayList<>();
        int index = 0;
        
        while ((index = targetText.indexOf(diffText, index)) != -1) {
            occurrences.add(index);
            index += 1; // 移动到下一个可能的位置（支持重叠查找）
        }
        
        return occurrences;
    }
    
    /**
     * 尾部匹配验证
     * 新规则：如果是单个bbox并且大于5个字符，验证bbox取最后5个字符 = （完整文本-差异文本）取最后5个字符
     * 限制条件：差异文本必须在完整文本的最后面才能使用此规则
     */
    private boolean tryTailMatchValidation(String originalTargetText, String remaining, String bboxText, String validationType, 
            boolean spaceNormalization, int bboxCount, int occurrenceIndex, String diffText, int diffIndex) {
        
        // 只对单个bbox且文本长度大于5的情况启用此规则
        if (bboxCount != 1 || bboxText.length() <= 5 || remaining.length() <= 5) {
            return false;
        }
        
        // 新增限制：差异文本必须在完整文本的最后面
        // 检查差异文本是否在原始目标文本的末尾
        boolean isDiffAtEnd = originalTargetText.endsWith(diffText);
        if (!isDiffAtEnd) {
            if (debugMode) {
                logger.debug("尾部匹配验证: 差异文本\"{}\"不在完整文本最后面，跳过尾部匹配验证", diffText);
            }
            return false;
        }
        
        // 提取bbox文本的最后5个字符
        String bboxTail = bboxText.substring(bboxText.length() - 5);
        
        // 提取剩余文本（目标文本-差异文本）的最后5个字符
        String remainingTail = remaining.substring(remaining.length() - 5);
        
        if (debugMode) {
            logger.debug("尾部匹配验证: 差异文本在完整文本最后面，执行尾部匹配");
            logger.debug("  bbox尾部5字符=\"{}\", 剩余文本尾部5字符=\"{}\"", bboxTail, remainingTail);
        }
        
        if (bboxTail.equals(remainingTail)) {
            String matchType = spaceNormalization ? "空格容错" : "精确";
            if (debugMode) {
                logger.info("✓ {} 验证通过 ({}尾部5字符匹配): 移除末尾差异\"{}\"后，尾部5字符匹配", 
                        validationType, matchType, diffText);
                logger.info("   bbox尾部=\"{}\", 剩余尾部=\"{}\"", bboxTail, remainingTail);
            }
            return true;
        }
        
        return false;
    }
    
    /**
     * 双bbox尾部+头部匹配验证
     * 新规则：如果是双bbox，验证bbox1尾部5字符+bbox2头部5字符 = 差异文本出现处前5字符+差异文本出现处后5字符
     */
    private boolean tryDualBboxTailHeadMatch(String originalTargetText, String bboxText, String validationType, 
            boolean spaceNormalization, int bboxCount, List<String> originalBboxTexts, 
            int occurrenceIndex, String diffText, int diffIndex) {
        
        // 只对双bbox且两个bbox文本都大于2字符的情况启用此规则
        if (bboxCount != 2 || originalBboxTexts.size() < 2) {
            return false;
        }
        
        String bbox1Text = originalBboxTexts.get(0);
        String bbox2Text = originalBboxTexts.get(1);
        
        // 检查bbox文本长度是否足够
        if (bbox1Text.length() <= 2 || bbox2Text.length() <= 2) {
            return false;
        }
        
        // 检查差异文本出现位置前后是否有足够的字符
        if (diffIndex < 2 || diffIndex + diffText.length() + 2 > originalTargetText.length()) {
            if (debugMode) {
                logger.debug("双bbox尾头匹配验证: 差异文本前后字符不足，需要前后各2个字符");
            }
            return false;
        }
        
        // 提取bbox1的最后2个字符和bbox2的前2个字符
        String bbox1Tail = bbox1Text.substring(bbox1Text.length() - 2);
        String bbox2Head = bbox2Text.substring(0, 2);
        String bboxCombined = bbox1Tail + bbox2Head;
        
        // 从原始目标文本中提取差异文本出现处前2个字符和后2个字符
        String diffBeforeChars = originalTargetText.substring(diffIndex - 2, diffIndex);
        String diffAfterChars = originalTargetText.substring(diffIndex + diffText.length(), diffIndex + diffText.length() + 2);
        String diffPositionCombined = diffBeforeChars + diffAfterChars;
        
        if (debugMode) {
            logger.debug("双bbox尾头匹配验证:");
            logger.debug("  bbox1尾部2字符=\"{}\", bbox2头部2字符=\"{}\"", bbox1Tail, bbox2Head);
            logger.debug("  bbox组合=\"{}\"", bboxCombined);
            logger.debug("  差异前2字符=\"{}\", 差异后2字符=\"{}\"", diffBeforeChars, diffAfterChars);
            logger.debug("  差异位置组合=\"{}\"", diffPositionCombined);
        }
        
        if (bboxCombined.equals(diffPositionCombined)) {
            String matchType = spaceNormalization ? "空格容错" : "精确";
            if (debugMode) {
                logger.info("✓ {} 验证通过 ({}双bbox尾头匹配): 移除第{}个\"{}\"(位置{})后，bbox1尾部+bbox2头部 = 差异位置前后字符", 
                        validationType, matchType, occurrenceIndex, diffText, diffIndex);
                logger.info("   bbox1尾部+bbox2头部=\"{}\", 差异位置前后=\"{}\"", bboxCombined, diffPositionCombined);
            }
            return true;
        }
        
        return false;
    }
    
    /**
     * 新规则3：相似度验证（适用于小于200字符的文本）
     * 
     * 基本逻辑：
     * 1. 检查目标文本长度是否小于200字符
     * 2. 计算原始文本与bbox文本的相似度
     * 3. 复用现有的移除逻辑，对每个差异文本出现位置计算移除后的相似度
     * 4. 如果任何一个移除位置的相似度比原始相似度高，则验证通过
     */
    private ValidationResult trySimilarityValidation(String originalTargetText, String bboxText, 
            String validationType, boolean spaceNormalization, String diffText, int currentOccurrenceIndex) {
        
        // 规则3只适用于小于200字符的文本
        if (originalTargetText.length() >= 200) {
            if (debugMode) {
                logger.debug("相似度验证: 文本长度({})超过200字符，跳过相似度验证", originalTargetText.length());
            }
            return new ValidationResult(false, "文本长度超过200字符");
        }
        
        if (debugMode) {
            logger.debug("相似度验证: 开始验证，文本长度: {}", originalTargetText.length());
            logger.debug("  原始目标文本: \"{}\"", originalTargetText);
            logger.debug("  bbox文本: \"{}\"", bboxText);
            logger.debug("  差异文本: \"{}\"", diffText);
        }
        
        // 准备要比较的文本
        String targetText = spaceNormalization ? normalizeSpaces(originalTargetText) : originalTargetText;
        String normalizedBboxText = spaceNormalization ? normalizeSpaces(bboxText) : bboxText;
        String normalizedDiffText = spaceNormalization ? normalizeSpaces(diffText) : diffText;
        
        // 计算原始文本与bbox文本的相似度
        double originalSimilarity = TextSimilarityCalculator.calculateLevenshteinSimilarity(targetText, normalizedBboxText);
        
        if (debugMode) {
            logger.debug("  原始文本相似度: {}", String.format("%.4f", originalSimilarity));
        }
        
        // 复用现有的查找差异文本所有出现位置的逻辑
        List<Integer> occurrences = findAllOccurrences(targetText, normalizedDiffText);
        
        if (occurrences.isEmpty()) {
            if (debugMode) {
                logger.debug("相似度验证: 无法在目标文本中找到差异文本");
            }
            return new ValidationResult(false, "无法在目标文本中找到差异文本");
        }
        
        // 对每个出现位置，计算移除后的相似度
        for (int i = 0; i < occurrences.size(); i++) {
            int diffIndex = occurrences.get(i);
            
            // 复用现有的文本移除逻辑
            String before = targetText.substring(0, diffIndex);
            String after = targetText.substring(diffIndex + normalizedDiffText.length());
            String textWithoutDiff = before + after;
            
            // 计算去掉差异文本后与bbox文本的相似度
            double similarityWithoutDiff = TextSimilarityCalculator.calculateLevenshteinSimilarity(textWithoutDiff, normalizedBboxText);
            
            if (debugMode) {
                logger.debug("相似度验证第{}个位置(索引{}):", i + 1, diffIndex);
                logger.debug("  去掉差异文本: \"{}\"", textWithoutDiff);
                logger.debug("  去除差异后相似度: {}", String.format("%.4f", similarityWithoutDiff));
                logger.debug("  相似度变化: {}", String.format("%+.4f", similarityWithoutDiff - originalSimilarity));
            }
            
            // 判断验证是否通过：去掉差异文本后的相似度应该比原始相似度高
            if (similarityWithoutDiff > originalSimilarity) {
                String matchType = spaceNormalization ? "空格容错" : "精确";
                double improvement = similarityWithoutDiff - originalSimilarity;
                
                if (debugMode) {
                    logger.info("✓ {} 验证通过 ({}相似度验证): 移除第{}个\"{}\"(位置{})后相似度从{}提升到{}(+{})", 
                            validationType, matchType, i + 1, diffText, diffIndex,
                            String.format("%.4f", originalSimilarity), 
                            String.format("%.4f", similarityWithoutDiff), 
                            String.format("%.4f", improvement));
                }
                
                return new ValidationResult(true, String.format("%s相似度验证(移除第%d个,提升%.4f)", matchType, i + 1, improvement));
            }
        }
        
        if (debugMode) {
            logger.debug("✗ 相似度验证失败: 所有移除位置的相似度都没有超过原始相似度({})", String.format("%.4f", originalSimilarity));
        }
        return new ValidationResult(false, "相似度验证失败");
    }
    
    // 移除复杂的旧验证方法，已被简化的减法匹配算法替代
    
    /**
     * 标准化空格：将连续的空格替换为单个空格，去除首尾空格
     * 特别处理中文字符间的多余空格
     */
    private String normalizeSpaces(String text) {
        if (text == null) return "";
        
        // 先将所有连续空白字符替换为单个空格
        String normalized = text.replaceAll("\\s+", " ").trim();
        
        // 移除中文字符之间的空格（OCR经常在中文字符间错误插入空格）
        // 匹配模式：中文字符 + 空格 + 中文字符
        normalized = normalized.replaceAll("([\\u4e00-\\u9fff])\\s+([\\u4e00-\\u9fff])", "$1$2");
        
        return normalized;
    }
    
    
    /**
     * 基于bbox从已保存的页面图片中截取区域
     * 直接使用GPU OCR系统已处理好的图片，避免重复渲染和DPI问题
     */
    private List<String> extractBboxImages(List<double[]> bboxes, List<Integer> pages, 
            String taskId, String docType, int blockIndex) throws IOException {
        
        List<String> imagePaths = new ArrayList<>();
        
        // 按页面分组处理bbox
        Map<Integer, List<Integer>> pageToBoxIndices = new HashMap<>();
        for (int i = 0; i < bboxes.size() && i < pages.size(); i++) {
            int page = pages.get(i);
            pageToBoxIndices.computeIfAbsent(page, k -> new ArrayList<>()).add(i);
        }
        
        for (Map.Entry<Integer, List<Integer>> entry : pageToBoxIndices.entrySet()) {
            int pageNum = entry.getKey();
            List<Integer> boxIndices = entry.getValue();
            
            // 读取GPU OCR系统已保存的页面图片
            BufferedImage pageImage = loadExistingPageImage(taskId, docType, pageNum);
            if (pageImage == null) {
                logger.warn("无法加载页面图片: taskId={}, docType={}, page={}", taskId, docType, pageNum);
                continue;
            }
            
            if (debugMode) {
                logger.debug("加载现有页面图片: taskId={}, docType={}, page={}, 尺寸: {}x{}", 
                        taskId, docType, pageNum, pageImage.getWidth(), pageImage.getHeight());
            }
            
            // 截取每个bbox区域
            for (int boxIdx : boxIndices) {
                double[] bbox = bboxes.get(boxIdx);
                
                try {
                    BufferedImage croppedImage = cropImageByBbox(pageImage, bbox);
                    String imagePath = saveSubImage(croppedImage, taskId, docType, blockIndex, pageNum, boxIdx);
                    imagePaths.add(imagePath);
                    
                    if (debugMode) {
                        logger.debug("保存子图片: {}, bbox: [{}, {}, {}, {}]", 
                                imagePath, bbox[0], bbox[1], bbox[2], bbox[3]);
                    }
                    
                } catch (Exception e) {
                    logger.warn("截取bbox图片失败: page={}, bbox=[{}, {}, {}, {}]", 
                            pageNum, bbox[0], bbox[1], bbox[2], bbox[3], e);
                }
            }
        }
        
        return imagePaths;
    }
    
    /**
     * 加载GPU OCR系统已保存的页面图片
     */
    private BufferedImage loadExistingPageImage(String taskId, String docType, int pageNum) {
        try {
            String uploadRootPath = zxcmConfig.getFileUpload().getRootPath();
            Path imagePath = Paths.get(uploadRootPath, "compare-pro", "tasks", taskId, "images", docType, "page-" + pageNum + ".png");
            
            if (!Files.exists(imagePath)) {
                logger.warn("页面图片不存在: {}", imagePath);
                return null;
            }
            
            BufferedImage image = ImageIO.read(imagePath.toFile());
            if (debugMode) {
                logger.debug("成功加载页面图片: {}, 尺寸: {}x{}", imagePath, image.getWidth(), image.getHeight());
            }
            return image;
            
        } catch (IOException e) {
            logger.error("加载页面图片失败: taskId={}, docType={}, page={}", taskId, docType, pageNum, e);
            return null;
        }
    }
    
    /**
     * 根据bbox坐标截取图片
     */
    private BufferedImage cropImageByBbox(BufferedImage pageImage, double[] bbox) {
        // bbox格式: [x1, y1, x2, y2]
        int x = (int) Math.max(0, Math.min(bbox[0], bbox[2]));
        int y = (int) Math.max(0, Math.min(bbox[1], bbox[3]));
        int width = (int) Math.abs(bbox[2] - bbox[0]);
        int height = (int) Math.abs(bbox[3] - bbox[1]);
        
        // 确保坐标在图片范围内
        x = Math.min(x, pageImage.getWidth() - 1);
        y = Math.min(y, pageImage.getHeight() - 1);
        width = Math.min(width, pageImage.getWidth() - x);
        height = Math.min(height, pageImage.getHeight() - y);
        
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Invalid bbox dimensions: " + width + "x" + height);
        }
        
        return pageImage.getSubimage(x, y, width, height);
    }
    
    /**
     * 保存子图片到指定目录
     */
    private String saveSubImage(BufferedImage image, String taskId, String docType, 
            int blockIndex, int pageNum, int bboxIndex) throws IOException {
        
        String subImageDir = getSubImageDirectory(taskId, docType);
        String fileName = String.format("block_%d_page_%d_bbox_%d.png", blockIndex, pageNum, bboxIndex);
        Path imagePath = Paths.get(subImageDir, fileName);
        
        ImageIO.write(image, "PNG", imagePath.toFile());
        
        return imagePath.toString();
    }
    
    /**
     * 分别识别每个图片的文本内容（并行版本）
     */
    private List<String> recognizeIndividualImages(List<String> imagePaths) {
        if (imagePaths == null || imagePaths.isEmpty()) {
            return new ArrayList<>();
        }
        
        if (rapidOcrService == null) {
            logger.warn("RapidOCR服务不可用，跳过图片识别");
            return java.util.Collections.nCopies(imagePaths.size(), "");
        }
        
        // 如果只有一个图片，直接串行处理
        if (imagePaths.size() == 1) {
            return recognizeIndividualImagesSerial(imagePaths);
        }
        
        // 多个图片时使用并行处理
        return recognizeIndividualImagesParallel(imagePaths);
    }
    
    /**
     * 串行识别图片（用于单个图片或作为fallback）
     */
    private List<String> recognizeIndividualImagesSerial(List<String> imagePaths) {
        List<String> results = new ArrayList<>();
        
        for (String imagePath : imagePaths) {
            String recognizedText = recognizeSingleImage(imagePath);
            results.add(recognizedText);
        }
        
        return results;
    }
    
    /**
     * 并行识别图片（使用统一线程池）
     */
    private List<String> recognizeIndividualImagesParallel(List<String> imagePaths) {
        int imageCount = imagePaths.size();
        
        // 检查线程池是否可用
        if (ocrValidationExecutor == null || ocrValidationExecutor.isShutdown()) {
            logger.warn("RapidOCR验证线程池不可用，降级为串行处理");
            return recognizeIndividualImagesSerial(imagePaths);
        }
        
        ExecutorCompletionService<String> completionService = 
            new ExecutorCompletionService<>(ocrValidationExecutor);
        
        if (debugMode) {
            logger.debug("🚀 开始并行OCR验证: {}个图片，使用统一线程池(最大{}线程)", 
                imageCount, DEFAULT_THREAD_POOL_SIZE);
        }
        
        // 提交所有任务
        Map<Future<String>, Integer> futureIndexMap = new HashMap<>();
        for (int i = 0; i < imageCount; i++) {
            final String imagePath = imagePaths.get(i);
            final int index = i;
            try {
                Future<String> future = completionService.submit(() -> {
                    return recognizeSingleImage(imagePath);
                });
                futureIndexMap.put(future, index);
            } catch (Exception e) {
                logger.warn("提交OCR验证任务失败: {}, 图片: {}", e.getMessage(), imagePath);
                futureIndexMap.put(null, index); // 占位符，后续处理为空结果
            }
        }
        
        // 收集结果（保持原始顺序）
        String[] results = new String[imageCount];
        int completedTasks = 0;
        
        for (Map.Entry<Future<String>, Integer> entry : futureIndexMap.entrySet()) {
            Future<String> future = entry.getKey();
            Integer index = entry.getValue();
            
            if (future == null) {
                results[index] = ""; // 提交失败的任务
                continue;
            }
            
            try {
                String result = future.get(30, TimeUnit.SECONDS); // 30秒超时
                results[index] = result;
                completedTasks++;
                
                if (debugMode) {
                    logger.debug("🚀 并行OCR验证进度 [{}/{}] 完成图片: {}", 
                        completedTasks, imageCount, imagePaths.get(index));
                }
            } catch (TimeoutException e) {
                logger.warn("图片识别超时: {}", imagePaths.get(index));
                results[index] = "";
                future.cancel(true); // 取消超时任务
            } catch (Exception e) {
                logger.warn("图片识别任务执行失败: {}, 图片: {}", e.getMessage(), imagePaths.get(index));
                results[index] = "";
            }
        }
        
        // 确保所有结果都有值（防止null）
        List<String> resultList = new ArrayList<>();
        for (int i = 0; i < imageCount; i++) {
            resultList.add(results[i] != null ? results[i] : "");
        }
        
        if (debugMode) {
            logger.debug("✅ 并行OCR验证完成: {}/{}个任务成功", completedTasks, imageCount);
        }
        
        return resultList;
    }
    
    /**
     * 识别单个图片的文本内容
     */
    private String recognizeSingleImage(String imagePath) {
        try {
            File imageFile = new File(imagePath);
            List<RapidOcrClient.RapidOcrTextBox> textBoxes = rapidOcrService.recognizeFile(imageFile);
            
            StringBuilder imageText = new StringBuilder();
            for (RapidOcrClient.RapidOcrTextBox box : textBoxes) {
                if (box.text != null && !box.text.trim().isEmpty()) {
                    if (imageText.length() > 0) {
                        imageText.append(" ");
                    }
                    imageText.append(box.text.trim());
                }
            }
            
            String recognizedText = imageText.toString().trim();
            
            if (debugMode) {
                logger.debug("单独识别图片: {}, 文本: \"{}\"", imagePath, recognizedText);
            }
            
            return recognizedText;
            
        } catch (Exception e) {
            logger.warn("识别图片文本失败: {}", imagePath, e);
            return ""; // 返回空字符串保持索引对应
        }
    }
    
    /**
     * 创建子图片目录
     */
    private void createSubImageDirectories(String taskId) throws IOException {
        String oldDir = getSubImageDirectory(taskId, "old");
        String newDir = getSubImageDirectory(taskId, "new");
        
        Files.createDirectories(Paths.get(oldDir));
        Files.createDirectories(Paths.get(newDir));
        
        if (debugMode) {
            logger.info("创建子图片目录: {} 和 {}", oldDir, newDir);
        }
    }
    
    /**
     * 获取子图片目录路径
     * 与GPU OCR系统保持一致的路径结构
     */
    private String getSubImageDirectory(String taskId, String docType) {
        String uploadRootPath = zxcmConfig.getFileUpload().getRootPath();
        Path subImageDir = Paths.get(uploadRootPath, "compare-pro", "tasks", taskId, "subimages", docType);
        return subImageDir.toString();
    }
    
    /**
     * 筛选符合条件的DiffBlock并记录它们在原始数组中的索引
     */
    private List<DiffBlock> filterEligibleBlocksWithIndices(List<DiffBlock> blocks, List<Integer> indices) {
        List<DiffBlock> eligibleBlocks = new ArrayList<>();
        
        for (int i = 0; i < blocks.size(); i++) {
            DiffBlock block = blocks.get(i);
            if (isBlockEligible(block)) {
                eligibleBlocks.add(block);
                indices.add(i);
                if (debugMode) {
                    logger.debug("DiffBlock符合条件: type={}, textLength={}, bboxCount={}, 原始索引={}", 
                            block.type, getTextLength(block), getBboxCount(block), i);
                }
            }
        }
        
        return eligibleBlocks;
    }
    
    /**
     * 创建过滤后的DiffBlock列表，移除验证通过的幻觉块
     */
    private List<DiffBlock> createFilteredBlocks(List<DiffBlock> originalBlocks, List<DiffBlockValidationItem> validationItems) {
        List<DiffBlock> filteredBlocks = new ArrayList<>();
        Set<Integer> removedIndices = new HashSet<>();
        
        // 收集所有验证通过的块索引
        for (DiffBlockValidationItem item : validationItems) {
            if (item.isValidationPassed()) {
                removedIndices.add(item.getBlockIndex());
                if (debugMode) {
                    logger.info("将移除验证通过的幻觉块: 索引={}, 类型={}, 方法={}", 
                            item.getBlockIndex(), item.getOperationType(), item.getValidationMethod());
                }
            }
        }
        
        // 创建过滤后的列表
        for (int i = 0; i < originalBlocks.size(); i++) {
            if (!removedIndices.contains(i)) {
                filteredBlocks.add(originalBlocks.get(i));
            }
        }
        
        return filteredBlocks;
    }
    
    /**
     * DiffBlock验证结果
     */
    public static class DiffBlockValidationResult {
        private String taskId;
        private int totalMergedCount;
        private int eligibleBlockCount;
        private int totalPages;
        private int pageThreshold;
        private boolean validationTriggered;
        private boolean validationSkipped;
        private String skipReason;
        private boolean validationSuccess;
        private String errorMessage;
        private List<DiffBlockValidationItem> validationItems;
        
        // 新增字段：过滤后的DiffBlock列表（移除验证通过的幻觉块）
        private List<DiffBlock> filteredBlocks;
        private int removedBlockCount;
        private List<Integer> removedBlockIndices;
        
        // Getters and Setters
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        
        public int getTotalMergedCount() { return totalMergedCount; }
        public void setTotalMergedCount(int totalMergedCount) { this.totalMergedCount = totalMergedCount; }
        
        public int getEligibleBlockCount() { return eligibleBlockCount; }
        public void setEligibleBlockCount(int eligibleBlockCount) { this.eligibleBlockCount = eligibleBlockCount; }
        
        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
        
        public int getPageThreshold() { return pageThreshold; }
        public void setPageThreshold(int pageThreshold) { this.pageThreshold = pageThreshold; }
        
        public boolean isValidationTriggered() { return validationTriggered; }
        public void setValidationTriggered(boolean validationTriggered) { this.validationTriggered = validationTriggered; }
        
        public boolean isValidationSkipped() { return validationSkipped; }
        public void setValidationSkipped(boolean validationSkipped) { this.validationSkipped = validationSkipped; }
        
        public String getSkipReason() { return skipReason; }
        public void setSkipReason(String skipReason) { this.skipReason = skipReason; }
        
        public boolean isValidationSuccess() { return validationSuccess; }
        public void setValidationSuccess(boolean validationSuccess) { this.validationSuccess = validationSuccess; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public List<DiffBlockValidationItem> getValidationItems() { return validationItems; }
        public void setValidationItems(List<DiffBlockValidationItem> validationItems) { this.validationItems = validationItems; }
        
        public List<DiffBlock> getFilteredBlocks() { return filteredBlocks; }
        public void setFilteredBlocks(List<DiffBlock> filteredBlocks) { this.filteredBlocks = filteredBlocks; }
        
        public int getRemovedBlockCount() { return removedBlockCount; }
        public void setRemovedBlockCount(int removedBlockCount) { this.removedBlockCount = removedBlockCount; }
        
        public List<Integer> getRemovedBlockIndices() { return removedBlockIndices; }
        public void setRemovedBlockIndices(List<Integer> removedBlockIndices) { this.removedBlockIndices = removedBlockIndices; }
    }
    
    /**
     * 单个DiffBlock的验证项
     */
    public static class DiffBlockValidationItem {
        private int blockIndex;
        private String operationType;
        private String originalOldText;
        private String originalNewText;
        private String recognizedOldText;
        private String recognizedNewText;
        private List<String> oldImagePaths;
        private List<String> newImagePaths;
        
        // 新增字段：标记是否验证通过（认为是模型幻觉）
        private boolean validationPassed;
        private String validationMethod; // 记录通过哪种验证方法
        // private ComparisonResult comparisonResult; // TODO: 后续实现
        
        // Getters and Setters
        public int getBlockIndex() { return blockIndex; }
        public void setBlockIndex(int blockIndex) { this.blockIndex = blockIndex; }
        
        public String getOperationType() { return operationType; }
        public void setOperationType(String operationType) { this.operationType = operationType; }
        
        public String getOriginalOldText() { return originalOldText; }
        public void setOriginalOldText(String originalOldText) { this.originalOldText = originalOldText; }
        
        public String getOriginalNewText() { return originalNewText; }
        public void setOriginalNewText(String originalNewText) { this.originalNewText = originalNewText; }
        
        public String getRecognizedOldText() { return recognizedOldText; }
        public void setRecognizedOldText(String recognizedOldText) { this.recognizedOldText = recognizedOldText; }
        
        public String getRecognizedNewText() { return recognizedNewText; }
        public void setRecognizedNewText(String recognizedNewText) { this.recognizedNewText = recognizedNewText; }
        
        public List<String> getOldImagePaths() { return oldImagePaths; }
        public void setOldImagePaths(List<String> oldImagePaths) { this.oldImagePaths = oldImagePaths; }
        
        public List<String> getNewImagePaths() { return newImagePaths; }
        public void setNewImagePaths(List<String> newImagePaths) { this.newImagePaths = newImagePaths; }
        
        public boolean isValidationPassed() { return validationPassed; }
        public void setValidationPassed(boolean validationPassed) { this.validationPassed = validationPassed; }
        
        public String getValidationMethod() { return validationMethod; }
        public void setValidationMethod(String validationMethod) { this.validationMethod = validationMethod; }
        
        @Override
        public String toString() {
            return String.format("DiffBlockValidationItem{blockIndex=%d, type=%s, oldText='%s', newText='%s', recognizedOld='%s', recognizedNew='%s', passed=%s, method='%s'}", 
                    blockIndex, operationType, originalOldText, originalNewText, recognizedOldText, recognizedNewText, validationPassed, validationMethod);
        }
    }
}
