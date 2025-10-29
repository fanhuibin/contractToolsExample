package com.zhaoxinms.contract.template.sdk.service.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zhaoxinms.contract.template.sdk.entity.FileInfoRecord;
import com.zhaoxinms.contract.template.sdk.mapper.FileInfoRecordMapper;
import com.zhaoxinms.contract.tools.common.entity.FileInfo;
import com.zhaoxinms.contract.tools.common.exception.FileOperationException;
import com.zhaoxinms.contract.tools.common.service.FileInfoService;
import com.zhaoxinms.contract.tools.common.util.FileStorageUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * 文件信息Service实现类
 * SDK项目中的具体实现
 */
@Slf4j
@Service
public class FileInfoServiceImpl implements FileInfoService {

    @Value("${server.port:8081}")
    private String serverPort;
    
    @Value("${zxcm.file-upload.root-path:./uploads}")
    private String uploadRootPath;

    @Autowired(required = false)
    private FileInfoRecordMapper fileInfoRecordMapper;

    @PostConstruct
    public void init() {
        // 仅确保目录存在，不再写入模拟数据
        String absUploadPath = Paths.get(uploadRootPath).toAbsolutePath().toString();
        try {
            java.io.File uploadDir = new java.io.File(absUploadPath);
            if (!uploadDir.exists()) uploadDir.mkdirs();
        } catch (Exception ignore) {}
    }

    @Override
    public FileInfo getById(String id) {
        if (fileInfoRecordMapper == null) {
            log.warn("fileInfoRecordMapper is null");
            return null;
        }
        if (id == null || id.trim().isEmpty()) {
            log.warn("文件ID为空");
            return null;
        }
        
        try {
            // 提取原始ID（去掉年月前缀）
            String originalId = FileStorageUtils.extractOriginalId(id.trim());
            Long longId = Long.parseLong(originalId);
            FileInfoRecord rec = fileInfoRecordMapper.selectById(longId);
            if (rec == null) {
                log.warn("未找到文件记录，文件ID: {}", id);
                return null;
            }
            FileInfo info = new FileInfo();
            Long dbId = rec.getId();
            info.setId(dbId);
            info.setFileId(FileStorageUtils.generateFileId(dbId)); // 生成带年月前缀的完整ID
            info.setOriginalName(rec.getOriginalName());
            info.setFileName(rec.getFileName());
            info.setFileExtension(rec.getFileExtension());
            info.setFileSize(rec.getFileSize());
            info.setStorePath(rec.getStorePath() != null ? rec.getStorePath() : rec.getFilePath());
            info.setStatus(rec.getStatus());
            info.setCreateTime(rec.getCreateTime());
            info.setUpdateTime(rec.getUpdateTime());
            info.setOnlyofficeKey(rec.getOnlyofficeKey());
            return info;
        } catch (NumberFormatException e) {
            log.error("文件ID格式错误: {}", id, e);
            return null;
        }
    }

    // no-op helper removed

    @Override
    public String getFileDownloadUrl(String fileId) {
        // 构建文件下载URL
        return "http://localhost:" + serverPort + "/api/template/file/download/" + fileId;
    }

    @Override
    public String getFileDiskPath(String fileId) {
        log.info("获取文件磁盘路径，文件ID: {}", fileId);
        FileInfo fileInfo = getById(fileId);
        if (fileInfo == null) {
            log.warn("文件不存在，文件ID: {}", fileId);
            return null;
        }
        String storePath = fileInfo.getStorePath();
        log.info("数据库中的文件路径: {}", storePath);
        
        // 如果storePath是相对路径，转换为绝对路径
        Path path = Paths.get(storePath);
        if (!path.isAbsolute()) {
            // 相对路径，拼接uploadRootPath
            path = Paths.get(uploadRootPath, storePath).toAbsolutePath().normalize();
            log.info("相对路径转换为绝对路径: {} -> {}", storePath, path);
            storePath = path.toString();
        }
        
        // 检查文件是否实际存在
        if (storePath != null && !storePath.isEmpty()) {
            java.io.File file = new java.io.File(storePath);
            if (!file.exists()) {
                log.error("文件路径存在于数据库但磁盘上不存在: {}", storePath);
            } else {
                log.info("文件存在，文件大小: {} bytes", file.length());
            }
        }
        
        return storePath;
    }

    @Override
    public boolean saveFile(String fileId, InputStream inputStream) throws IOException {
        if (fileId == null || fileId.trim().isEmpty()) {
            throw new IllegalArgumentException("文件ID不能为空");
        }
        
        if (inputStream == null) {
            throw new IllegalArgumentException("文件输入流不能为空");
        }
        
        FileInfo fileInfo = getById(fileId);
        if (fileInfo == null) {
            throw new IllegalArgumentException("文件不存在，文件ID: " + fileId);
        }
        
        // 使用 getFileDiskPath 获取绝对路径（自动处理相对路径转换）
        String filePath = getFileDiskPath(fileId);
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalStateException("文件存储路径为空，文件ID: " + fileId);
        }
        
        java.io.File targetFile = new java.io.File(filePath);
        
        // 确保父目录存在
        java.io.File parentDir = targetFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            boolean dirsCreated = parentDir.mkdirs();
            if (!dirsCreated) {
                throw new IOException("无法创建文件存储目录: " + parentDir.getAbsolutePath());
            }
            log.info("创建文件存储目录: {}", parentDir.getAbsolutePath());
        }
        
        // 创建临时文件，避免写入过程中文件损坏
        java.io.File tempFile = new java.io.File(filePath + ".tmp");
        
        try {
            // 将输入流内容写入临时文件
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile);
                 java.io.BufferedOutputStream bos = new java.io.BufferedOutputStream(fos)) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytes = 0;
                
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    bos.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;
                }
                
                bos.flush();
                log.info("成功写入临时文件，文件ID: {}, 大小: {} bytes", fileId, totalBytes);
            }
            
            // 原子性地替换原文件
            if (targetFile.exists()) {
                // 备份原文件
                java.io.File backupFile = new java.io.File(filePath + ".bak");
                if (backupFile.exists()) {
                    backupFile.delete();
                }
                
                boolean renamed = targetFile.renameTo(backupFile);
                if (!renamed) {
                    log.warn("无法创建备份文件: {}", backupFile.getAbsolutePath());
                }
            }
            
            // 将临时文件重命名为目标文件
            boolean success = tempFile.renameTo(targetFile);
            if (!success) {
                throw new IOException("无法将临时文件重命名为目标文件: " + filePath);
            }
            
            // 更新文件信息
            fileInfo.setFileSize(targetFile.length());
            fileInfo.setUpdateTime(LocalDateTime.now());
            if (fileInfoRecordMapper != null) {
                FileInfoRecord rec = fileInfoRecordMapper.selectById(fileId);
                if (rec != null) {
                    rec.setFileSize(targetFile.length());
                    rec.setUpdateTime(LocalDateTime.now());
                    fileInfoRecordMapper.updateById(rec);
                }
            }
            
            log.info("文件保存成功，文件ID: {}, 路径: {}, 大小: {} bytes", 
                    fileId, filePath, targetFile.length());
            
            // 删除备份文件
            java.io.File backupFile = new java.io.File(filePath + ".bak");
            if (backupFile.exists()) {
                backupFile.delete();
            }
            
            return true;
            
        } catch (Exception e) {
            // 清理临时文件
            if (tempFile.exists()) {
                tempFile.delete();
            }
            
            // 如果有备份文件，尝试恢复
            java.io.File backupFile = new java.io.File(filePath + ".bak");
            if (backupFile.exists()) {
                backupFile.renameTo(targetFile);
                log.info("已从备份文件恢复: {}", filePath);
            }
            
            log.error("文件保存失败，文件ID: {}, 错误: {}", fileId, e.getMessage(), e);
            throw new FileOperationException(fileId, "SAVE", "文件保存失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public FileInfo generateOnlyofficeKey(String fileId) {
        FileInfo fileInfo = getById(fileId);
        if (fileInfo == null) {
            throw new IllegalArgumentException("文件不存在，文件ID: " + fileId);
        }
        
        // 生成新的OnlyOffice key，格式为：文件id + 分隔符 + 雪花id
        String onlyofficeKey = generateOnlyOfficeKeyForFile(fileId);
        fileInfo.setOnlyofficeKey(onlyofficeKey);
        fileInfo.setUpdateTime(LocalDateTime.now());
        
        if (fileInfoRecordMapper != null) {
            FileInfoRecord rec = fileInfoRecordMapper.selectById(fileId);
            if (rec != null) {
                rec.setOnlyofficeKey(onlyofficeKey);
                rec.setUpdateTime(LocalDateTime.now());
                fileInfoRecordMapper.updateById(rec);
            }
        }
        
        log.info("为文件生成OnlyOffice key，文件ID: {}, key: {}", fileId, onlyofficeKey);
        return fileInfo;
    }
    
    @Override
    public List<FileInfo> getAllFiles() {
        if (fileInfoRecordMapper == null) return new ArrayList<>();
        List<FileInfoRecord> list = fileInfoRecordMapper.selectList(new QueryWrapper<>());
        List<FileInfo> out = new ArrayList<>();
        for (FileInfoRecord r : list) {
            FileInfo i = new FileInfo();
            i.setId(r.getId());
            i.setOriginalName(r.getOriginalName());
            i.setFileName(r.getFileName());
            i.setFileExtension(r.getFileExtension());
            i.setFileSize(r.getFileSize());
            i.setStorePath(r.getStorePath() != null ? r.getStorePath() : r.getFilePath());
            i.setStatus(r.getStatus());
            i.setCreateTime(r.getCreateTime());
            i.setUpdateTime(r.getUpdateTime());
            i.setOnlyofficeKey(r.getOnlyofficeKey());
            i.setModule(r.getModule());
            out.add(i);
        }
        return out;
    }

    @Override
    public List<FileInfo> getFilesByModule(String module) {
        if (fileInfoRecordMapper == null) return new ArrayList<>();
        QueryWrapper<FileInfoRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("module", module);
        List<FileInfoRecord> list = fileInfoRecordMapper.selectList(wrapper);
        List<FileInfo> out = new ArrayList<>();
        for (FileInfoRecord r : list) {
            FileInfo i = new FileInfo();
            i.setId(r.getId());
            i.setOriginalName(r.getOriginalName());
            i.setFileName(r.getFileName());
            i.setFileExtension(r.getFileExtension());
            i.setFileSize(r.getFileSize());
            i.setStorePath(r.getStorePath() != null ? r.getStorePath() : r.getFilePath());
            i.setStatus(r.getStatus());
            i.setCreateTime(r.getCreateTime());
            i.setUpdateTime(r.getUpdateTime());
            i.setOnlyofficeKey(r.getOnlyofficeKey());
            i.setModule(r.getModule());
            out.add(i);
        }
        return out;
    }

    @Override
    public FileInfo registerFile(String originalName, String extension, String absolutePath, long fileSize) {
        if (absolutePath == null || absolutePath.trim().isEmpty()) {
            throw new IllegalArgumentException("absolutePath 不能为空");
        }
        try {
            if (fileInfoRecordMapper == null) throw new IllegalStateException("文件表未配置");
            FileInfoRecord rec = new FileInfoRecord();
            rec.setOriginalName(originalName != null ? originalName : new java.io.File(absolutePath).getName());
            rec.setFileName(rec.getOriginalName());
            rec.setFileExtension(extension);
            rec.setFileSize(fileSize >= 0 ? fileSize : (new java.io.File(absolutePath).length()));
            rec.setStorePath(absolutePath);
            rec.setStatus(0);
            rec.setCreateTime(LocalDateTime.now());
            rec.setUpdateTime(LocalDateTime.now());
            // 先插入记录以获取ID
            fileInfoRecordMapper.insert(rec);
            // 使用ID生成唯一的onlyofficeKey，避免缓存冲突
            String onlyofficeKey = generateOnlyOfficeKeyForFile(String.valueOf(rec.getId()));
            rec.setOnlyofficeKey(onlyofficeKey);
            // 更新onlyofficeKey
            fileInfoRecordMapper.updateById(rec);
            
            FileInfo out = new FileInfo();
            // 生成带年月前缀的文件ID
            Long dbId = rec.getId();
            String fileIdWithYearMonth = FileStorageUtils.generateFileId(dbId);
            
            out.setId(dbId);
            out.setFileId(fileIdWithYearMonth); // 新增：带年月前缀的完整文件ID
            out.setOriginalName(rec.getOriginalName());
            out.setFileName(rec.getFileName());
            out.setFileExtension(rec.getFileExtension());
            out.setFileSize(rec.getFileSize());
            out.setStorePath(rec.getStorePath());
            out.setStatus(rec.getStatus());
            out.setCreateTime(rec.getCreateTime());
            out.setUpdateTime(rec.getUpdateTime());
            out.setOnlyofficeKey(rec.getOnlyofficeKey());
            
            log.info("注册新文件，数据库ID: {}, 文件ID: {}, onlyofficeKey: {}, 原始名称: {}", 
                dbId, fileIdWithYearMonth, onlyofficeKey, originalName);
            return out;
        } catch (Exception e) {
            throw new RuntimeException("注册文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 为指定文件ID生成OnlyOffice key，格式为：文件id + 分隔符 + 雪花id
     */
    private String generateOnlyOfficeKeyForFile(String fileId) {
        // 使用UUID确保key的唯一性，避免OnlyOffice缓存问题
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return fileId + "_" + uuid;
    }

    /**
     * 保存上传的新文件
     */
    @Override
    public FileInfo saveNewFile(MultipartFile file) throws IOException {
        return saveNewFile(file, null);
    }

    @Override
    public FileInfo saveNewFile(MultipartFile file, String module) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.trim().isEmpty()) {
            originalName = "unknown_file";
        }

        // 获取文件扩展名
        String extension = "";
        int lastDotIndex = originalName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < originalName.length() - 1) {
            extension = originalName.substring(lastDotIndex + 1);
        }

        // 生成唯一的文件名
        String uniqueFileName = UUID.randomUUID().toString() + "_" + originalName;
        
        // 第一步：保存到临时目录
        String tempPath = FileStorageUtils.getTempUploadPath(uploadRootPath);
        Path tempDir = Paths.get(tempPath).toAbsolutePath();
        if (!Files.exists(tempDir)) {
            Files.createDirectories(tempDir);
        }
        
        Path tempFilePath = tempDir.resolve(uniqueFileName);
        
        // 保存文件到临时目录
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, tempFilePath, StandardCopyOption.REPLACE_EXISTING);
        }
        
        log.info("文件已上传到临时目录: {}", tempFilePath);
        
        // 第二步：注册到数据库，同时复制到永久存储目录
        FileInfo fileInfo;
        try {
            // 构建永久存储路径
            String yearMonthPath = FileStorageUtils.getYearMonthPath();
            Path permanentDir = Paths.get(uploadRootPath, "files", yearMonthPath).toAbsolutePath();
            if (!Files.exists(permanentDir)) {
                Files.createDirectories(permanentDir);
            }
            
            Path permanentFilePath = permanentDir.resolve(uniqueFileName);
            
            // 复制到永久存储目录
            Files.copy(tempFilePath, permanentFilePath, StandardCopyOption.REPLACE_EXISTING);
            
            log.info("文件已复制到永久存储: {}", permanentFilePath);
            
            // 计算相对路径（相对于uploadRootPath）
            Path rootPath = Paths.get(uploadRootPath).toAbsolutePath().normalize();
            String relativePath = rootPath.relativize(permanentFilePath.toAbsolutePath().normalize())
                .toString()
                .replace('\\', '/');  // 统一使用正斜杠，便于跨平台
            
            log.info("文件相对路径: {}", relativePath);
            
            // 注册文件信息到数据库（使用相对路径）
            fileInfo = registerFileWithModule(originalName, extension, relativePath, file.getSize(), module);
            
            log.info("文件已注册到数据库，文件ID: {}, 模块: {}", fileInfo.getId(), module);
            
        } finally {
            // 第三步：清理临时文件
            try {
                if (Files.exists(tempFilePath)) {
                    Files.delete(tempFilePath);
                    log.info("临时文件已清理: {}", tempFilePath);
                }
            } catch (Exception e) {
                log.warn("清理临时文件失败: {}", tempFilePath, e);
            }
        }
        
        log.info("保存新文件完成，文件ID: {}, 原始名称: {}, 模块: {}, 永久路径: {}", 
                fileInfo.getId(), originalName, module, fileInfo.getStorePath());
        
        return fileInfo;
    }

    /**
     * 注册文件并指定模块（支持相对路径）
     * 
     * @param originalName 原始文件名
     * @param extension 文件扩展名
     * @param filePath 文件路径（可以是相对路径或绝对路径）
     * @param fileSize 文件大小
     * @param module 所属模块
     * @return 文件信息
     */
    @Override
    public FileInfo registerFile(String originalName, String extension, String filePath, long fileSize, String module) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("filePath 不能为空");
        }
        
        // 如果是绝对路径，转换为相对路径
        String storePath = filePath;
        File file = new File(filePath);
        if (file.isAbsolute()) {
            try {
                Path rootPath = Paths.get(uploadRootPath).toAbsolutePath().normalize();
                Path absoluteFilePath = Paths.get(filePath).toAbsolutePath().normalize();
                storePath = rootPath.relativize(absoluteFilePath)
                    .toString()
                    .replace('\\', '/');  // 统一使用正斜杠
                log.info("绝对路径转相对路径: {} -> {}", filePath, storePath);
            } catch (Exception e) {
                log.warn("无法将绝对路径转换为相对路径，使用原路径: {}", filePath, e);
            }
        }
        
        return registerFileWithModule(originalName, extension, storePath, fileSize, module);
    }
    
    /**
     * 内部方法：注册文件并指定模块
     */
    private FileInfo registerFileWithModule(String originalName, String extension, String storePath, long fileSize, String module) {
        if (storePath == null || storePath.trim().isEmpty()) {
            throw new IllegalArgumentException("storePath 不能为空");
        }
        try {
            if (fileInfoRecordMapper == null) throw new IllegalStateException("文件表未配置");
            FileInfoRecord rec = new FileInfoRecord();
            rec.setOriginalName(originalName != null ? originalName : new java.io.File(storePath).getName());
            rec.setFileName(rec.getOriginalName());
            rec.setFileExtension(extension);
            rec.setFileSize(fileSize);
            rec.setStorePath(storePath);  // 存储相对路径
            rec.setModule(module); // 设置模块
            rec.setStatus(0);
            rec.setCreateTime(LocalDateTime.now());
            rec.setUpdateTime(LocalDateTime.now());
            // 先插入记录以获取ID
            fileInfoRecordMapper.insert(rec);
            // 使用ID生成唯一的onlyofficeKey，避免缓存冲突
            String onlyofficeKey = generateOnlyOfficeKeyForFile(String.valueOf(rec.getId()));
            rec.setOnlyofficeKey(onlyofficeKey);
            // 更新onlyofficeKey
            fileInfoRecordMapper.updateById(rec);
            
            FileInfo out = new FileInfo();
            // 生成带年月前缀的文件ID
            Long dbId = rec.getId();
            String fileIdWithYearMonth = FileStorageUtils.generateFileId(dbId);
            out.setId(dbId);
            out.setFileId(fileIdWithYearMonth);
            out.setOriginalName(rec.getOriginalName());
            out.setFileName(rec.getFileName());
            out.setFileExtension(rec.getFileExtension());
            out.setFileSize(rec.getFileSize());
            out.setStorePath(rec.getStorePath());
            out.setModule(rec.getModule());
            out.setStatus(rec.getStatus());
            out.setCreateTime(rec.getCreateTime());
            out.setUpdateTime(rec.getUpdateTime());
            out.setOnlyofficeKey(rec.getOnlyofficeKey());
            
            log.info("注册新文件，数据库ID: {}, 文件ID: {}, 模块: {}, onlyofficeKey: {}, 原始名称: {}, 存储路径: {}", 
                dbId, fileIdWithYearMonth, module, onlyofficeKey, originalName, storePath);
            return out;
        } catch (Exception e) {
            throw new RuntimeException("注册文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 注册克隆的文件
     */
    @Override
    public FileInfo registerClonedFile(Path filePath, String originalName) throws IOException {
        if (filePath == null || !Files.exists(filePath)) {
            throw new IllegalArgumentException("文件路径不存在: " + filePath);
        }

        if (originalName == null || originalName.trim().isEmpty()) {
            originalName = filePath.getFileName().toString();
        }

        // 获取文件扩展名
        String extension = "";
        int lastDotIndex = originalName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < originalName.length() - 1) {
            extension = originalName.substring(lastDotIndex + 1);
        }

        // 获取文件大小
        long fileSize = Files.size(filePath);
        
        // 使用年月路径
        String yearMonthPath = FileStorageUtils.getYearMonthPath();
        Path uploadDir = Paths.get(uploadRootPath, "files", yearMonthPath).toAbsolutePath();
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }
        
        Path targetPath = filePath;
        if (!filePath.startsWith(uploadDir)) {
            // 生成唯一的文件名并移动到上传目录
            String uniqueFileName = UUID.randomUUID().toString() + "_" + originalName;
            targetPath = uploadDir.resolve(uniqueFileName);
            Files.move(filePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("文件已移动到上传目录: {} -> {}", filePath, targetPath);
        }
        
        // 注册文件信息到数据库
        FileInfo fileInfo = registerFile(originalName, extension, targetPath.toString(), fileSize);
        
        log.info("注册克隆文件成功，文件ID: {}, 原始名称: {}, 存储路径: {}", 
                fileInfo.getId(), originalName, targetPath);
        
        return fileInfo;
    }

    /**
     * 根据ID删除文件
     */
    @Override
    public boolean deleteById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return false;
        }
        
        try {
            if (fileInfoRecordMapper != null) {
                // 将String类型的id转换为Long类型
                Long longId = Long.parseLong(id);
                int result = fileInfoRecordMapper.deleteById(longId);
                log.info("删除文件记录，文件ID: {}, 结果: {}", id, result > 0 ? "成功" : "失败");
                return result > 0;
            }
            return false;
        } catch (NumberFormatException e) {
            log.error("文件ID格式错误: {}", id, e);
            return false;
        } catch (Exception e) {
            log.error("删除文件记录失败，文件ID: {}", id, e);
            return false;
        }
    }

    /**
     * 统计指定日期范围内的文件数量
     */
    @Override
    public int countFilesByDateRange(LocalDate startDate, LocalDate endDate, List<String> modules) {
        try {
            if (fileInfoRecordMapper == null) {
                log.warn("FileInfoRecordMapper未配置，无法统计数据库记录");
                return 0;
            }
            
            QueryWrapper<FileInfoRecord> queryWrapper = buildDateRangeQuery(startDate, endDate, modules);
            long count = fileInfoRecordMapper.selectCount(queryWrapper);
            log.info("统计文件记录数量: {}, 日期范围: {} - {}, 模块: {}", 
                count, startDate, endDate, modules);
            return (int) count;
        } catch (Exception e) {
            log.error("统计文件记录失败", e);
            return 0;
        }
    }

    /**
     * 获取指定日期范围内的文件列表（用于统计文件大小）
     */
    @Override
    public List<FileInfo> getFilesByDateRange(LocalDate startDate, LocalDate endDate, List<String> modules) {
        try {
            if (fileInfoRecordMapper == null) {
                log.warn("FileInfoRecordMapper未配置，无法查询文件");
                return new ArrayList<>();
            }
            
            QueryWrapper<FileInfoRecord> queryWrapper = buildDateRangeQuery(startDate, endDate, modules);
            List<FileInfoRecord> records = fileInfoRecordMapper.selectList(queryWrapper);
            
            // 转换为 FileInfo
            List<FileInfo> fileInfos = new ArrayList<>();
            for (FileInfoRecord record : records) {
                FileInfo info = new FileInfo();
                info.setId(record.getId());
                info.setOriginalName(record.getOriginalName());
                info.setFileExtension(record.getFileExtension());
                info.setFileSize(record.getFileSize());
                info.setCreateTime(record.getCreateTime());
                info.setModule(record.getModule());
                fileInfos.add(info);
            }
            
            log.debug("查询到 {} 个文件记录, 日期范围: {} - {}, 模块: {}", 
                fileInfos.size(), startDate, endDate, modules);
            return fileInfos;
        } catch (Exception e) {
            log.error("查询文件列表失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 删除指定日期范围内的文件记录（同时删除物理文件）
     */
    @Override
    public int deleteFilesByDateRange(LocalDate startDate, LocalDate endDate, List<String> modules) {
        try {
            if (fileInfoRecordMapper == null) {
                log.warn("FileInfoRecordMapper未配置，无法删除数据库记录");
                return 0;
            }
            
            // 第一步：查询要删除的记录
            QueryWrapper<FileInfoRecord> queryWrapper = buildDateRangeQuery(startDate, endDate, modules);
            List<FileInfoRecord> recordsToDelete = fileInfoRecordMapper.selectList(queryWrapper);
            
            if (recordsToDelete == null || recordsToDelete.isEmpty()) {
                log.info("没有找到需要删除的文件记录");
                return 0;
            }
            
            log.info("找到 {} 条文件记录需要删除", recordsToDelete.size());
            
            // 第二步：删除物理文件
            int filesDeleted = 0;
            int filesNotFound = 0;
            int filesDeleteFailed = 0;
            
            for (FileInfoRecord record : recordsToDelete) {
                try {
                    String filePath = getFileDiskPath(String.valueOf(record.getId()));
                    if (filePath != null && !filePath.isEmpty()) {
                        java.io.File file = new java.io.File(filePath);
                        if (file.exists()) {
                            if (file.delete()) {
                                filesDeleted++;
                                log.debug("删除文件成功: {}", filePath);
                            } else {
                                filesDeleteFailed++;
                                log.warn("删除文件失败（无权限或被占用）: {}", filePath);
                            }
                        } else {
                            filesNotFound++;
                            log.debug("文件不存在（可能已被删除）: {}", filePath);
                        }
                    }
                } catch (Exception e) {
                    filesDeleteFailed++;
                    log.warn("删除文件异常: {}, 错误: {}", record.getOriginalName(), e.getMessage());
                }
            }
            
            log.info("物理文件删除结果 - 成功: {}, 不存在: {}, 失败: {}", 
                filesDeleted, filesNotFound, filesDeleteFailed);
            
            // 第三步：删除数据库记录
            QueryWrapper<FileInfoRecord> deleteWrapper = buildDateRangeQuery(startDate, endDate, modules);
            int deleted = fileInfoRecordMapper.delete(deleteWrapper);
            log.info("数据库记录删除: {} 条, 日期范围: {} - {}, 模块: {}", 
                deleted, startDate, endDate, modules);
            
            return deleted;
        } catch (Exception e) {
            log.error("删除文件记录失败", e);
            return 0;
        }
    }

    /**
     * 构建日期范围查询条件
     */
    private QueryWrapper<FileInfoRecord> buildDateRangeQuery(LocalDate startDate, LocalDate endDate, 
                                                              List<String> modules) {
        QueryWrapper<FileInfoRecord> queryWrapper = new QueryWrapper<>();
        
        // 时间范围条件
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay(); // 包含结束日期当天
        
        queryWrapper.ge("create_time", startDateTime);
        queryWrapper.lt("create_time", endDateTime);
        
        // 模块条件
        if (modules != null && !modules.isEmpty()) {
            queryWrapper.in("module", modules);
        }
        
        // ⚠️ 重要：永远不清理 template-design 模块的文件（模板永久保存）
        queryWrapper.ne("module", "template-design");
        
        return queryWrapper;
    }
} 