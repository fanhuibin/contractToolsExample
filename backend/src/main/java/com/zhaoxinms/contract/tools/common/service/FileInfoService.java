package com.zhaoxinms.contract.tools.common.service;

import com.zhaoxinms.contract.tools.common.entity.FileInfo;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 文件信息Service接口
 */
public interface FileInfoService {
    
    /**
     * 上传文件
     * @param file 上传的文件
     * @param subPath 子目录路径
     * @return 文件信息
     */
    FileInfo uploadFile(MultipartFile file, String subPath) throws IOException;
    
    /**
     * 上传文件（使用默认路径）
     */
    FileInfo uploadFile(MultipartFile file) throws IOException;
    
    /**
     * 删除文件
     * @param id 文件ID
     * @return 是否删除成功
     */
    boolean deleteFile(Long id);
    
    /**
     * 根据ID获取文件信息
     * @param id 文件ID
     * @return 文件信息
     */
    FileInfo getById(Long id);
    
    /**
     * 根据文件路径获取文件信息
     * @param filePath 文件路径
     * @return 文件信息
     */
    FileInfo getByFilePath(String filePath);
    
    /**
     * 更新文件内容
     * @param fileId 文件ID
     * @param inputStream 文件输入流
     * @return 是否更新成功
     */
    boolean updateFileContent(Long fileId, java.io.InputStream inputStream) throws IOException;
    
    /**
     * 分页获取文件信息
     * @param page 页码
     * @param size 每页大小
     * @return 分页结果
     */
    Map<String, Object> getFileInfoPage(int page, int size);
    
    /**
     * 搜索文件信息
     * @param originalName 原始文件名
     * @return 匹配的文件信息列表
     */
    List<FileInfo> searchByOriginalName(String originalName);
    
    /**
     * 生成OnlyOffice文档key
     * @param fileId 文件ID
     * @return 更新后的文件信息
     */
    FileInfo generateOnlyofficeKey(Long fileId);
} 