package com.zhaoxinms.contract.tools.common.service;

import com.zhaoxinms.contract.tools.common.entity.FileInfo;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/**
 * 文件信息Service接口
 * 在Backend项目中定义，由SDK项目实现
 */
public interface FileInfoService {
    
    /**
     * 根据ID获取文件信息
     * @param id 文件ID
     * @return 文件信息
     */
    FileInfo getById(String id);
    
    /**
     * 获取文件下载URL
     * @param fileId 文件ID
     * @return 文件下载URL
     */
    String getFileDownloadUrl(String fileId);
    
    /**
     * 获取文件磁盘路径
     * @param fileId 文件ID
     * @return 文件磁盘路径
     */
    String getFileDiskPath(String fileId);
    
    /**
     * 保存文件（OnlyOffice回调使用）
     * @param fileId 文件ID
     * @param inputStream 文件输入流
     * @return 是否保存成功
     */
    boolean saveFile(String fileId, InputStream inputStream) throws IOException;
    
    /**
     * 生成OnlyOffice文档key
     * @param fileId 文件ID
     * @return 更新后的文件信息
     */
    FileInfo generateOnlyofficeKey(String fileId);

    /**
     * 获取所有文件信息
     * @return 文件信息列表
     */
    List<FileInfo> getAllFiles();

    /**
     * 根据模块获取文件列表
     * @param module 模块名称
     * @return 文件信息列表
     */
    List<FileInfo> getFilesByModule(String module);

    /**
     * 注册一个已存在于磁盘的文件到文件服务，返回文件信息
     * @param originalName 原始文件名（用于下载展示）
     * @param extension 文件扩展名（不含点）
     * @param absolutePath 磁盘绝对路径
     * @param fileSize 文件大小（字节）
     * @return 文件信息（包含生成的ID）
     */
    FileInfo registerFile(String originalName, String extension, String absolutePath, long fileSize);

    /**
     * 注册一个已存在于磁盘的文件到文件服务，并指定所属模块
     * @param originalName 原始文件名（用于下载展示）
     * @param extension 文件扩展名（不含点）
     * @param filePath 文件路径（相对路径或绝对路径）
     * @param fileSize 文件大小（字节）
     * @param module 所属模块
     * @return 文件信息（包含生成的ID）
     */
    FileInfo registerFile(String originalName, String extension, String filePath, long fileSize, String module);

    /**
     * 保存上传的新文件
     * @param file 上传的文件
     * @return 文件信息
     * @throws IOException 文件操作异常
     */
    FileInfo saveNewFile(MultipartFile file) throws IOException;

    /**
     * 保存上传的新文件（指定模块）
     * @param file 上传的文件
     * @param module 所属模块
     * @return 文件信息
     * @throws IOException 文件操作异常
     */
    FileInfo saveNewFile(MultipartFile file, String module) throws IOException;

    /**
     * 注册克隆的文件
     * @param filePath 文件路径
     * @param originalName 原始文件名
     * @return 文件信息
     * @throws IOException 文件操作异常
     */
    FileInfo registerClonedFile(Path filePath, String originalName) throws IOException;

    /**
     * 根据ID删除文件
     * @param id 文件ID
     * @return 是否删除成功
     */
    boolean deleteById(String id);

    /**
     * 统计指定日期范围内的文件数量
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param modules 模块列表（null表示所有模块）
     * @return 文件数量
     */
    int countFilesByDateRange(LocalDate startDate, LocalDate endDate, List<String> modules);

    /**
     * 获取指定日期范围内的文件列表（用于统计文件大小）
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param modules 模块列表（null表示所有模块）
     * @return 文件信息列表
     */
    List<FileInfo> getFilesByDateRange(LocalDate startDate, LocalDate endDate, List<String> modules);

    /**
     * 删除指定日期范围内的文件记录（同时删除物理文件）
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param modules 模块列表（null表示所有模块）
     * @return 删除的记录数
     */
    int deleteFilesByDateRange(LocalDate startDate, LocalDate endDate, List<String> modules);
} 