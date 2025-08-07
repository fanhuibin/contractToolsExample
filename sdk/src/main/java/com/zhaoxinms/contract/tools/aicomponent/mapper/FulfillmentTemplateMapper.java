package com.zhaoxinms.contract.tools.aicomponent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaoxinms.contract.tools.aicomponent.model.FulfillmentTemplate;
import org.apache.ibatis.annotations.Mapper;

/**
 * 履约任务模板数据库映射接口
 * 继承MyBatis-Plus的BaseMapper，提供基础的CRUD操作
 */
@Mapper
public interface FulfillmentTemplateMapper extends BaseMapper<FulfillmentTemplate> {
    // 可以在此添加自定义的复杂查询方法
    
    /**
     * 根据合同类型和用户ID查找默认模板
     * @param contractType 合同类型
     * @param userId 用户ID
     * @return 默认模板
     */
    FulfillmentTemplate selectDefaultTemplate(String contractType, String userId);
}
