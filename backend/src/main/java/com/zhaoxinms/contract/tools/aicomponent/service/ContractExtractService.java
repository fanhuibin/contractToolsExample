package com.zhaoxinms.contract.tools.aicomponent.service;

import java.io.File;
import java.nio.file.Path;

/**
 * 合同信息提取服务接口
 * @author zhaoxinms
 */
public interface ContractExtractService {

    /**
     * 上传文件到AI服务进行处理
     * @param filePath 文件路径
     * @return 文件对象ID
     */
    String uploadFile(Path filePath);

    /**
     * 使用AI分析文件内容并提取信息
     * @param fileId 文件ID
     * @param prompt 提取提示
     * @return 提取的文本内容
     */
    String extractInfo(String fileId, String prompt);

    /**
     * 一站式处理：上传文件并提取信息
     * @param filePath 文件路径
     * @param prompt 提取提示（可选，为null时使用默认提示）
     * @return 提取的文本内容
     */
    String processFile(Path filePath, String prompt);
}