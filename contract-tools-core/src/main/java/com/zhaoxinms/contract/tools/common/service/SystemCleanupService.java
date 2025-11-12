package com.zhaoxinms.contract.tools.common.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zhaoxinms.contract.tools.common.model.CleanupRequest;
import com.zhaoxinms.contract.tools.common.model.CleanupResult;
import com.zhaoxinms.contract.tools.common.model.CleanupResult.ModuleCleanupStat;
import com.zhaoxinms.contract.tools.config.ZxcmConfig;
import com.zhaoxinms.contract.tools.config.DemoModeConfig;

/**
 * 系统文件清理服务
 * 
 * 功能：
 * 1. 按模块清理历史数据
 * 2. 按时间范围清理
 * 3. 同步清理数据库和文件系统
 * 4. 支持预览模式
 * 
 * @author AI Assistant
 * @since 2025-10-29
 */
@Service
public class SystemCleanupService {
    
    private static final Logger logger = LoggerFactory.getLogger(SystemCleanupService.class);
    
    @Autowired
    private ZxcmConfig zxcmConfig;
    
    @Autowired
    private FileInfoService fileInfoService;
    
    @Autowired
    private DemoModeConfig demoModeConfig;
    
    @Autowired(required = false)
    private com.zhaoxinms.contract.tools.comparePRO.service.CompareService compareService;
    
    /**
     * 支持的模块列表
     * ⚠️ 重要：清理时会自动跳过templates目录！
     */
    private static final List<String> SUPPORTED_MODULES = Arrays.asList(
        "rule-extract",      // 智能文档抽取（会跳过templates目录）
        "compare-pro",       // 智能文档比对
        "ocr-extract",       // 智能文档解析
        "compose",           // 智能合同合成
        "onlyoffice-demo",   // 文档在线编辑
        "temp-uploads"       // 临时上传
    );
    
    /**
     * 受保护的模块列表 - 这些模块的数据永远不会被清理
     */
    private static final List<String> PROTECTED_MODULES = Arrays.asList(
        "template-design"    // 模板设计文件（永久保存）
    );
    
    /**
     * 执行清理操作
     */
    @Transactional(rollbackFor = Exception.class)
    public CleanupResult cleanup(CleanupRequest request) {
        logger.info("========== 开始系统清理 ==========");
        logger.info("清理模式: {}", request.getMode());
        logger.info("时间范围: {} 至 {}", request.getStartDate(), request.getEndDate());
        logger.info("清理模块: {}", request.getModules() != null ? request.getModules() : "全部");
        
        CleanupResult result = new CleanupResult();
        result.setMode(request.getMode());
        result.setStartTime(System.currentTimeMillis());
        
        try {
            if ("execute".equalsIgnoreCase(request.getMode()) && demoModeConfig.isDemoMode()) {
                String message = "演示模式下禁止执行数据清理操作";
                result.setSuccess(false);
                result.setMessage(message);
                result.addError(message);
                logger.warn(message);
                return result;
            }
            
            // 验证请求
            request.validate();
            
            // 确定要清理的模块
            List<String> modulesToClean = determineModules(request);
            result.addLog("将清理以下模块: " + modulesToClean);
            
            // 获取文件根目录
            String uploadRootPath = zxcmConfig.getFileUpload().getRootPath();
            result.addLog("文件根目录: " + uploadRootPath);
            
            // 清理各个模块
            for (String module : modulesToClean) {
                try {
                    ModuleCleanupStat stat = cleanModule(module, request, uploadRootPath);
                    result.getModuleStats().put(module, stat);
                    
                    // 更新总计
                    result.getFileSystemStat().addDeletedFiles(stat.getFileCount());
                    result.getFileSystemStat().addDeletedSize(stat.getFileSize());
                    result.getFileSystemStat().addDeletedDirs(stat.getDirCount());
                    
                    result.addLog(String.format("模块 [%s]: 文件 %d 个, 大小 %.2f MB, 目录 %d 个",
                        module, stat.getFileCount(), stat.getFileSize() / (1024.0 * 1024.0), stat.getDirCount()));
                    
                    // 特殊处理：清理 compare-pro 模块的内存任务数据
                    if ("compare-pro".equals(module) && "execute".equals(request.getMode())) {
                        int deletedTasks = cleanCompareTasks(request);
                        if (deletedTasks > 0) {
                            result.addLog(String.format("模块 [%s]: 清理内存中的比对任务 %d 个", module, deletedTasks));
                        }
                    }
                } catch (Exception e) {
                    String error = "清理模块 " + module + " 失败: " + e.getMessage();
                    logger.error(error, e);
                    result.addError(error);
                }
            }
            
            // 清理数据库
            if (Boolean.TRUE.equals(request.getCleanDatabase())) {
                try {
                    ModuleCleanupStat dbStat = cleanDatabase(request);
                    result.getDatabaseStat().setDeletedRecords((int)dbStat.getFileCount());
                    result.addLog(String.format("数据库清理: %d 条记录, 同步删除文件: %d 个 (%.2f MB)", 
                        dbStat.getFileCount(), dbStat.getFileCount(), dbStat.getFileSize() / (1024.0 * 1024.0)));
                    
                    // 将数据库清理的文件也计入总数
                    result.getFileSystemStat().addDeletedFiles((int)dbStat.getFileCount());
                    result.getFileSystemStat().addDeletedSize(dbStat.getFileSize());
                } catch (Exception e) {
                    String error = "清理数据库失败: " + e.getMessage();
                    logger.error(error, e);
                    result.addError(error);
                }
            }
            
            result.setSuccess(true);
            
            if ("preview".equals(request.getMode())) {
                result.setMessage("预览完成。总计: " + result.getFileSystemStat().getDeletedFiles() + " 个文件, " 
                    + String.format("%.2f MB", result.getFileSystemStat().getDeletedSize() / (1024.0 * 1024.0))
                    + ", " + result.getDatabaseStat().getDeletedRecords() + " 条数据库记录");
            } else {
                result.setMessage("清理完成。已删除: " + result.getFileSystemStat().getDeletedFiles() + " 个文件, " 
                    + String.format("%.2f MB", result.getFileSystemStat().getDeletedSize() / (1024.0 * 1024.0))
                    + ", " + result.getDatabaseStat().getDeletedRecords() + " 条数据库记录");
            }
            
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("清理失败: " + e.getMessage());
            result.addError(e.getMessage());
            logger.error("系统清理失败", e);
        } finally {
            result.setEndTime(System.currentTimeMillis());
            logger.info("========== 清理完成，耗时: {}ms ==========", result.getDuration());
        }
        
        return result;
    }
    
    /**
     * 确定要清理的模块
     */
    private List<String> determineModules(CleanupRequest request) {
        if (request.getModules() == null || request.getModules().isEmpty()) {
            return new ArrayList<>(SUPPORTED_MODULES);
        }
        
        // 验证模块名称
        for (String module : request.getModules()) {
            if (!SUPPORTED_MODULES.contains(module)) {
                throw new IllegalArgumentException("不支持的模块: " + module + 
                    ", 支持的模块: " + SUPPORTED_MODULES);
            }
        }
        
        return request.getModules();
    }
    
    /**
     * 清理单个模块
     */
    private ModuleCleanupStat cleanModule(String module, CleanupRequest request, String uploadRootPath) 
            throws IOException {
        logger.info("清理模块: {}", module);
        
        ModuleCleanupStat stat = new ModuleCleanupStat(module);
        
        if (!Boolean.TRUE.equals(request.getCleanFileSystem())) {
            logger.info("跳过文件系统清理（cleanFileSystem=false）");
            return stat;
        }
        
        // 获取模块根目录
        String moduleDir = getModuleDirectory(module, uploadRootPath);
        Path modulePath = Paths.get(moduleDir);
        
        if (!Files.exists(modulePath)) {
            logger.info("模块目录不存在: {}", moduleDir);
            return stat;
        }
        
        // 清理按年月组织的目录结构
        cleanYearMonthStructure(modulePath, request, stat);
        
        return stat;
    }
    
    /**
     * 获取模块目录路径
     * 注意：
     * - rule-extract-data/templates/ 目录会被自动跳过
     * - uploads/templates/ 根目录不在清理范围内
     * - 只清理各模块的任务数据和临时文件
     */
    private String getModuleDirectory(String module, String uploadRootPath) {
        switch (module) {
            case "rule-extract":
                return uploadRootPath + File.separator + "rule-extract-data";
            case "compare-pro":
                return uploadRootPath + File.separator + "compare-pro";
            case "ocr-extract":
                return uploadRootPath + File.separator + "ocr-extract-tasks";
            case "compose":
                // 只清理compose目录（合成结果），不清理templates目录（合同模板）
                return uploadRootPath + File.separator + "compose";
            case "onlyoffice-demo":
                return uploadRootPath + File.separator + "onlyoffice";
            case "temp-uploads":
                return uploadRootPath + File.separator + "temp-uploads";
            case "file-info":
                // 只清理files目录，不包括根目录下的templates
                return uploadRootPath + File.separator + "files";
            default:
                throw new IllegalArgumentException("未知模块: " + module);
        }
    }
    
    /**
     * 清理年/月目录结构
     */
    private void cleanYearMonthStructure(Path modulePath, CleanupRequest request, ModuleCleanupStat stat) 
            throws IOException {
        
        int startYear = request.getStartDate().getYear();
        int endYear = request.getEndDate().getYear();
        
        // 遍历年份目录
        try (DirectoryStream<Path> yearStream = Files.newDirectoryStream(modulePath)) {
            for (Path yearPath : yearStream) {
                if (!Files.isDirectory(yearPath)) {
                    continue;
                }
                
                String yearStr = yearPath.getFileName().toString();
                
                // 特殊处理：templates 目录完全跳过，不清理任何模板
                if ("templates".equals(yearStr)) {
                    logger.info("跳过模板目录，不清理: {}", yearPath);
                    continue;
                }
                
                // 特殊处理：tasks 目录按年月清理
                if ("tasks".equals(yearStr)) {
                    cleanSubDirectory(yearPath, request, stat);
                    continue;
                }
                
                // 检查是否是年份目录
                if (!yearStr.matches("\\d{4}")) {
                    continue;
                }
                
                int year = Integer.parseInt(yearStr);
                if (year < startYear || year > endYear) {
                    continue;
                }
                
                // 遍历月份目录
                cleanMonthDirectories(yearPath, year, request, stat);
            }
        }
    }
    
    /**
     * 清理子目录（如templates、tasks）
     */
    private void cleanSubDirectory(Path subPath, CleanupRequest request, ModuleCleanupStat stat) 
            throws IOException {
        try (DirectoryStream<Path> yearStream = Files.newDirectoryStream(subPath)) {
            for (Path yearPath : yearStream) {
                if (!Files.isDirectory(yearPath)) {
                    continue;
                }
                
                String yearStr = yearPath.getFileName().toString();
                if (!yearStr.matches("\\d{4}")) {
                    continue;
                }
                
                int year = Integer.parseInt(yearStr);
                int startYear = request.getStartDate().getYear();
                int endYear = request.getEndDate().getYear();
                
                if (year < startYear || year > endYear) {
                    continue;
                }
                
                cleanMonthDirectories(yearPath, year, request, stat);
            }
        }
    }
    
    /**
     * 清理月份目录
     */
    private void cleanMonthDirectories(Path yearPath, int year, CleanupRequest request, ModuleCleanupStat stat) 
            throws IOException {
        
        try (DirectoryStream<Path> monthStream = Files.newDirectoryStream(yearPath)) {
            for (Path monthPath : monthStream) {
                if (!Files.isDirectory(monthPath)) {
                    continue;
                }
                
                String monthStr = monthPath.getFileName().toString();
                if (!monthStr.matches("\\d{2}")) {
                    continue;
                }
                
                int month = Integer.parseInt(monthStr);
                
                // 检查是否在时间范围内
                LocalDate dirDate = LocalDate.of(year, month, 1);
                LocalDate startDate = request.getStartDate().withDayOfMonth(1);
                LocalDate endDate = request.getEndDate().withDayOfMonth(1);
                
                if (dirDate.isBefore(startDate) || dirDate.isAfter(endDate)) {
                    continue;
                }
                
                // 清理或统计目录
                try {
                    if ("execute".equals(request.getMode())) {
                        deleteDirectory(monthPath, stat);
                    } else {
                        countDirectory(monthPath, stat);
                    }
                } catch (IOException e) {
                    logger.error("处理目录失败: {}", monthPath, e);
                    throw e;
                }
            }
        }
        
        // 如果年份目录为空，也删除
        if ("execute".equals(request.getMode()) && isEmptyDirectory(yearPath)) {
            try {
                Files.delete(yearPath);
                stat.incrementDirCount();
                logger.debug("删除空年份目录: {}", yearPath);
            } catch (IOException e) {
                logger.warn("删除空年份目录失败: {}", yearPath, e);
            }
        }
    }
    
    /**
     * 递归删除目录
     */
    private void deleteDirectory(Path directory, ModuleCleanupStat stat) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        
        Files.walk(directory)
            .sorted((p1, p2) -> -p1.compareTo(p2))  // 逆序，先删文件后删目录
            .forEach(path -> {
                try {
                    if (Files.isRegularFile(path)) {
                        long size = Files.size(path);
                        Files.delete(path);
                        stat.incrementFileCount();
                        stat.addFileSize(size);
                    } else if (Files.isDirectory(path)) {
                        Files.delete(path);
                        stat.incrementDirCount();
                    }
                } catch (IOException e) {
                    logger.error("删除失败: {}", path, e);
                }
            });
    }
    
    /**
     * 统计目录（预览模式）
     */
    private void countDirectory(Path directory, ModuleCleanupStat stat) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        
        Files.walk(directory).forEach(path -> {
            try {
                if (Files.isRegularFile(path)) {
                    stat.incrementFileCount();
                    stat.addFileSize(Files.size(path));
                } else if (Files.isDirectory(path) && !path.equals(directory)) {
                    stat.incrementDirCount();
                }
            } catch (IOException e) {
                logger.error("统计文件失败: {}", path, e);
            }
        });
    }
    
    /**
     * 检查目录是否为空
     */
    private boolean isEmptyDirectory(Path directory) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            return !stream.iterator().hasNext();
        }
    }
    
    /**
     * 清理数据库
     */
    private ModuleCleanupStat cleanDatabase(CleanupRequest request) {
        logger.info("清理数据库记录...");
        
        ModuleCleanupStat stat = new ModuleCleanupStat("file-info");
        
        // 过滤掉受保护的模块
        List<String> modulesToClean = filterProtectedModules(request.getModules());
        
        if (modulesToClean.isEmpty()) {
            logger.info("所有选择的模块都受保护，跳过数据库清理");
            return stat;
        }
        
        // 先获取文件列表以统计大小
        List<com.zhaoxinms.contract.tools.common.entity.FileInfo> files = 
            fileInfoService.getFilesByDateRange(
                request.getStartDate(), 
                request.getEndDate(),
                modulesToClean
            );
        
        // 统计文件数量和总大小
        int fileCount = files.size();
        long totalSize = 0;
        for (com.zhaoxinms.contract.tools.common.entity.FileInfo file : files) {
            if (file.getFileSize() != null) {
                totalSize += file.getFileSize();
            }
        }
        
        stat.setFileCount(fileCount);
        stat.setFileSize(totalSize);
        
        logger.info("数据库中找到 {} 个文件记录, 总大小: {} MB", 
            fileCount, totalSize / (1024.0 * 1024.0));
        
        if ("execute".equals(request.getMode())) {
            // 执行模式：删除（会同步删除物理文件）
            int deleted = fileInfoService.deleteFilesByDateRange(
                request.getStartDate(), 
                request.getEndDate(),
                modulesToClean  // 使用过滤后的模块列表
            );
            logger.info("已删除 {} 条数据库记录及其对应的物理文件", deleted);
        }
        
        return stat;
    }
    
    /**
     * 过滤掉受保护的模块
     */
    private List<String> filterProtectedModules(List<String> modules) {
        if (modules == null || modules.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<String> filtered = new ArrayList<>();
        for (String module : modules) {
            if (!PROTECTED_MODULES.contains(module)) {
                filtered.add(module);
            } else {
                logger.warn("模块 [{}] 受保护，已从清理列表中移除", module);
            }
        }
        return filtered;
    }
    
    /**
     * 获取支持的模块列表
     */
    public List<String> getSupportedModules() {
        return new ArrayList<>(SUPPORTED_MODULES);
    }
    
    /**
     * 获取受保护的模块列表
     */
    public List<String> getProtectedModules() {
        return new ArrayList<>(PROTECTED_MODULES);
    }
    
    /**
     * 清理比对任务的内存数据
     * 根据时间范围删除内存中的比对任务
     */
    private int cleanCompareTasks(CleanupRequest request) {
        if (compareService == null) {
            logger.warn("CompareService 未注入，跳过清理比对任务内存数据");
            return 0;
        }
        
        try {
            // 获取所有任务
            List<com.zhaoxinms.contract.tools.comparePRO.model.CompareTask> allTasks = compareService.getAllTasks();
            
            // 转换为 LocalDate 以便比较
            java.time.LocalDate startDate = request.getStartDate();
            java.time.LocalDate endDate = request.getEndDate();
            
            int deletedCount = 0;
            
            // 遍历所有任务，删除在时间范围内的任务
            for (com.zhaoxinms.contract.tools.comparePRO.model.CompareTask task : allTasks) {
                if (task.getStartTime() == null) {
                    continue;
                }
                
                // 将 LocalDateTime 转换为 LocalDate
                java.time.LocalDate taskDate = task.getStartTime().toLocalDate();
                
                // 检查任务时间是否在清理范围内
                if (!taskDate.isBefore(startDate) && !taskDate.isAfter(endDate)) {
                    // 删除任务
                    boolean deleted = compareService.deleteTask(task.getTaskId());
                    if (deleted) {
                        deletedCount++;
                        logger.debug("删除比对任务: taskId={}, startTime={}", task.getTaskId(), task.getStartTime());
                    }
                }
            }
            
            logger.info("清理比对任务内存数据完成: 删除了 {} 个任务", deletedCount);
            return deletedCount;
            
        } catch (Exception e) {
            logger.error("清理比对任务内存数据失败", e);
            return 0;
        }
    }
}

