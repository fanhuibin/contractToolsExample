package com.zhaoxinms.contract.tools.api.common;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.baomidou.mybatisplus.core.metadata.OrderItem;

import java.io.Serializable;


/**
 * 统一分页查询参数
 * 
 * @author zhaoxin
 * @since 2025-01-18
 */
@Data
@ApiModel("分页查询参数")
public class PageQuery implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 当前页码（从1开始）
     */
    @ApiModelProperty(value = "当前页码", required = true, example = "1")
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码不能小于1")
    private Long current = 1L;
    
    /**
     * 每页大小
     */
    @ApiModelProperty(value = "每页大小", required = true, example = "10")
    @NotNull(message = "每页大小不能为空")
    @Min(value = 1, message = "每页大小不能小于1")
    @Max(value = 100, message = "每页大小不能大于100")
    private Long size = 10L;
    
    /**
     * 排序字段
     */
    @ApiModelProperty(value = "排序字段", example = "createTime")
    private String sortField;
    
    /**
     * 排序方向（ASC/DESC）
     */
    @ApiModelProperty(value = "排序方向", example = "DESC", allowableValues = "ASC,DESC")
    private String sortOrder = "DESC";
    
    /**
     * 搜索关键词
     */
    @ApiModelProperty(value = "搜索关键词")
    private String keyword;
    
    /**
     * 转换为MyBatis-Plus的Page对象
     */
    public <T> com.baomidou.mybatisplus.extension.plugins.pagination.Page<T> toPage() {
        return new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(current, size);
    }
    
    /**
     * 转换为MyBatis-Plus的Page对象（带排序）
     */
    public <T> com.baomidou.mybatisplus.extension.plugins.pagination.Page<T> toPage(Class<T> clazz) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<T> page = 
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(current, size);
        
        if (sortField != null && !sortField.isEmpty()) {
            boolean isAsc = "ASC".equalsIgnoreCase(sortOrder);
            if (isAsc) {
                page.addOrder(OrderItem.asc(sortField));
            } else {
                page.addOrder(OrderItem.desc(sortField));
            }
        }
        
        return page;
    }
}

