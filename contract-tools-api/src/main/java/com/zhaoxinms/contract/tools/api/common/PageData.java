package com.zhaoxinms.contract.tools.api.common;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 统一分页响应数据
 * 
 * @author zhaoxin
 * @since 2025-01-18
 * @param <T> 数据类型
 */
@Data
@ApiModel("分页数据")
public class PageData<T> implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 数据列表
     */
    @ApiModelProperty(value = "数据列表", required = true)
    private List<T> records;
    
    /**
     * 当前页码（从1开始）
     */
    @ApiModelProperty(value = "当前页码", required = true, example = "1")
    private Long current;
    
    /**
     * 每页大小
     */
    @ApiModelProperty(value = "每页大小", required = true, example = "10")
    private Long size;
    
    /**
     * 总记录数
     */
    @ApiModelProperty(value = "总记录数", required = true, example = "100")
    private Long total;
    
    /**
     * 总页数
     */
    @ApiModelProperty(value = "总页数", required = true, example = "10")
    private Long pages;
    
    /**
     * 是否有上一页
     */
    @ApiModelProperty(value = "是否有上一页", example = "false")
    private Boolean hasPrevious;
    
    /**
     * 是否有下一页
     */
    @ApiModelProperty(value = "是否有下一页", example = "true")
    private Boolean hasNext;
    
    public PageData() {
    }
    
    public PageData(List<T> records, Long current, Long size, Long total) {
        this.records = records;
        this.current = current;
        this.size = size;
        this.total = total;
        this.pages = (total + size - 1) / size;
        this.hasPrevious = current > 1;
        this.hasNext = current < pages;
    }
    
    /**
     * 创建空分页
     */
    public static <T> PageData<T> empty(Long current, Long size) {
        return new PageData<>(List.of(), current, size, 0L);
    }
    
    /**
     * 从MyBatis-Plus的Page对象转换
     */
    public static <T> PageData<T> from(com.baomidou.mybatisplus.extension.plugins.pagination.Page<T> page) {
        return new PageData<>(
            page.getRecords(),
            page.getCurrent(),
            page.getSize(),
            page.getTotal()
        );
    }
}

