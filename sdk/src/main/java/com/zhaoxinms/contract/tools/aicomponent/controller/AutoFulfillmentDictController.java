package com.zhaoxinms.contract.tools.aicomponent.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhaoxinms.contract.tools.aicomponent.mapper.AutoFulfillmentKeywordMapper;
import com.zhaoxinms.contract.tools.aicomponent.mapper.AutoFulfillmentTaskTypeMapper;
import com.zhaoxinms.contract.tools.aicomponent.model.AutoFulfillmentKeyword;
import com.zhaoxinms.contract.tools.aicomponent.model.AutoFulfillmentTaskType;
import com.zhaoxinms.contract.tools.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/ai/auto-fulfillment/dicts")
@RequiredArgsConstructor
public class AutoFulfillmentDictController {

    private final AutoFulfillmentTaskTypeMapper taskTypeMapper;
    private final AutoFulfillmentKeywordMapper keywordMapper;

    @GetMapping("/task-types")
    public Result<List<Map<String, Object>>> taskTypesTree() {
        List<AutoFulfillmentTaskType> all = taskTypeMapper.selectList(new LambdaQueryWrapper<AutoFulfillmentTaskType>()
                .orderByAsc(AutoFulfillmentTaskType::getParentId)
                .orderByAsc(AutoFulfillmentTaskType::getSortOrder));
        Map<Long, Map<String, Object>> idToNode = new LinkedHashMap<>();
        List<Map<String, Object>> roots = new ArrayList<>();
        for (AutoFulfillmentTaskType t : all) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", t.getId());
            node.put("label", t.getName());
            node.put("children", new ArrayList<>());
            idToNode.put(t.getId(), node);
        }
        for (AutoFulfillmentTaskType t : all) {
            if (t.getParentId() == null) {
                roots.add(idToNode.get(t.getId()));
            } else {
                List<Map<String, Object>> ch = (List<Map<String, Object>>) idToNode.get(t.getParentId()).get("children");
                ch.add(idToNode.get(t.getId()));
            }
        }
        return Result.success(roots);
    }

    @GetMapping("/keywords")
    public Result<List<Map<String, Object>>> keywordsByTaskType(@RequestParam("taskTypeIds") List<Long> taskTypeIds) {
        if (taskTypeIds == null || taskTypeIds.isEmpty()) return Result.success(Collections.emptyList());
        List<AutoFulfillmentKeyword> keywords = keywordMapper.selectByTaskTypeIds(taskTypeIds);
        List<Map<String, Object>> data = new ArrayList<>();
        for (AutoFulfillmentKeyword k : keywords) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", k.getId());
            m.put("name", k.getName());
            data.add(m);
        }
        return Result.success(data);
    }
}


