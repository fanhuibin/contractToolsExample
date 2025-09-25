package com.zhaoxinms.contract.template.sdk.mapper;

import com.zhaoxinms.contract.template.sdk.entity.CompareRecord;
import org.apache.ibatis.annotations.*;

public interface CompareRecordMapper {

    @Insert("INSERT INTO compare_record(biz_id, old_pdf_name, new_pdf_name, results_json, created_at) VALUES(#{bizId}, #{oldPdfName}, #{newPdfName}, #{resultsJson}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CompareRecord record);

    @Select("SELECT id, biz_id, old_pdf_name, new_pdf_name, results_json, created_at FROM compare_record WHERE biz_id = #{bizId} LIMIT 1")
    CompareRecord findByBizId(@Param("bizId") String bizId);
}


