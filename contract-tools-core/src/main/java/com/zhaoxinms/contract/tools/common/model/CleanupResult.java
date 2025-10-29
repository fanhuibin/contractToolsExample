package com.zhaoxinms.contract.tools.common.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统文件清理结果
 * 
 * @author AI Assistant
 * @since 2025-10-29
 */
public class CleanupResult {
    
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 操作模式：preview 或 execute
     */
    private String mode;
    
    /**
     * 消息
     */
    private String message;
    
    /**
     * 各模块清理统计
     */
    private Map<String, ModuleCleanupStat> moduleStats = new HashMap<>();
    
    /**
     * 数据库清理统计
     */
    private DatabaseCleanupStat databaseStat = new DatabaseCleanupStat();
    
    /**
     * 文件系统清理统计
     */
    private FileSystemCleanupStat fileSystemStat = new FileSystemCleanupStat();
    
    /**
     * 清理详情日志
     */
    private List<String> logs = new ArrayList<>();
    
    /**
     * 错误信息
     */
    private List<String> errors = new ArrayList<>();
    
    /**
     * 开始时间
     */
    private long startTime;
    
    /**
     * 结束时间
     */
    private long endTime;
    
    // Getters and Setters
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMode() {
        return mode;
    }
    
    public void setMode(String mode) {
        this.mode = mode;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Map<String, ModuleCleanupStat> getModuleStats() {
        return moduleStats;
    }
    
    public void setModuleStats(Map<String, ModuleCleanupStat> moduleStats) {
        this.moduleStats = moduleStats;
    }
    
    public DatabaseCleanupStat getDatabaseStat() {
        return databaseStat;
    }
    
    public void setDatabaseStat(DatabaseCleanupStat databaseStat) {
        this.databaseStat = databaseStat;
    }
    
    public FileSystemCleanupStat getFileSystemStat() {
        return fileSystemStat;
    }
    
    public void setFileSystemStat(FileSystemCleanupStat fileSystemStat) {
        this.fileSystemStat = fileSystemStat;
    }
    
    public List<String> getLogs() {
        return logs;
    }
    
    public void setLogs(List<String> logs) {
        this.logs = logs;
    }
    
    public void addLog(String log) {
        this.logs.add(log);
    }
    
    public List<String> getErrors() {
        return errors;
    }
    
    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
    
    public void addError(String error) {
        this.errors.add(error);
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
    
    public long getEndTime() {
        return endTime;
    }
    
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }
    
    public long getDuration() {
        return endTime - startTime;
    }
    
    /**
     * 模块清理统计
     */
    public static class ModuleCleanupStat {
        private String moduleName;
        private int fileCount;
        private long fileSize;
        private int dirCount;
        
        public ModuleCleanupStat() {}
        
        public ModuleCleanupStat(String moduleName) {
            this.moduleName = moduleName;
        }
        
        public String getModuleName() {
            return moduleName;
        }
        
        public void setModuleName(String moduleName) {
            this.moduleName = moduleName;
        }
        
        public int getFileCount() {
            return fileCount;
        }
        
        public void setFileCount(int fileCount) {
            this.fileCount = fileCount;
        }
        
        public void incrementFileCount() {
            this.fileCount++;
        }
        
        public long getFileSize() {
            return fileSize;
        }
        
        public void setFileSize(long fileSize) {
            this.fileSize = fileSize;
        }
        
        public void addFileSize(long size) {
            this.fileSize += size;
        }
        
        public int getDirCount() {
            return dirCount;
        }
        
        public void setDirCount(int dirCount) {
            this.dirCount = dirCount;
        }
        
        public void incrementDirCount() {
            this.dirCount++;
        }
    }
    
    /**
     * 数据库清理统计
     */
    public static class DatabaseCleanupStat {
        private int deletedRecords;
        
        public int getDeletedRecords() {
            return deletedRecords;
        }
        
        public void setDeletedRecords(int deletedRecords) {
            this.deletedRecords = deletedRecords;
        }
        
        public void incrementDeletedRecords() {
            this.deletedRecords++;
        }
        
        public void addDeletedRecords(int count) {
            this.deletedRecords += count;
        }
    }
    
    /**
     * 文件系统清理统计
     */
    public static class FileSystemCleanupStat {
        private int deletedFiles;
        private long deletedSize;
        private int deletedDirs;
        
        public int getDeletedFiles() {
            return deletedFiles;
        }
        
        public void setDeletedFiles(int deletedFiles) {
            this.deletedFiles = deletedFiles;
        }
        
        public void incrementDeletedFiles() {
            this.deletedFiles++;
        }
        
        public void addDeletedFiles(int count) {
            this.deletedFiles += count;
        }
        
        public long getDeletedSize() {
            return deletedSize;
        }
        
        public void setDeletedSize(long deletedSize) {
            this.deletedSize = deletedSize;
        }
        
        public void addDeletedSize(long size) {
            this.deletedSize += size;
        }
        
        public int getDeletedDirs() {
            return deletedDirs;
        }
        
        public void setDeletedDirs(int deletedDirs) {
            this.deletedDirs = deletedDirs;
        }
        
        public void incrementDeletedDirs() {
            this.deletedDirs++;
        }
        
        public void addDeletedDirs(int count) {
            this.deletedDirs += count;
        }
    }
}

