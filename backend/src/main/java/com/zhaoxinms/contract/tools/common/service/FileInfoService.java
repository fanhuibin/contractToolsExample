package com.zhaoxinms.contract.tools.common.service;

import com.zhaoxinms.contract.tools.common.entity.FileInfo;

import java.io.IOException;
import java.io.InputStream;
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
} 