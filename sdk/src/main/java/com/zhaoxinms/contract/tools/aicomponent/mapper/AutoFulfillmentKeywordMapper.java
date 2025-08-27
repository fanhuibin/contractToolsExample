package com.zhaoxinms.contract.tools.aicomponent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaoxinms.contract.tools.aicomponent.model.AutoFulfillmentKeyword;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AutoFulfillmentKeywordMapper extends BaseMapper<AutoFulfillmentKeyword> {

    @Select({
            "<script>",
            "SELECT k.* FROM auto_fulfillment_keyword k ",
            "JOIN auto_fulfillment_task_type_keyword m ON m.keyword_id = k.id ",
            "WHERE m.task_type_id IN ",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach> ",
            "GROUP BY k.id",
            "</script>"
    })
    List<AutoFulfillmentKeyword> selectByTaskTypeIds(@Param("ids") List<Long> taskTypeIds);
}


