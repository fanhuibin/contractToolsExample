package com.zhaoxinms.contract.template.sdk.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("file_info")
public class FileInfoRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String originalName;
    private String fileName;
    private String filePath; // 兼容旧字段
    private String storePath;
    private Long fileSize;
    private String fileType;
    private String fileExtension;
    private String fileMd5;
    private Integer status; // 0 正常 1 删除
    private LocalDateTime createTime;
    private LocalDateTime uploadTime;
    private LocalDateTime updateTime;
    private String onlyofficeKey;
    private String module; // 所属模块（用于区分文件来源）
}


