package com.zhaoxinms.contract.tools.aicomponent.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhaoxinms.contract.tools.aicomponent.model.FulfillmentConfig;
import com.zhaoxinms.contract.tools.aicomponent.model.FulfillmentExtractResult;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.*;

@Service
public class FulfillmentTaskService {
    private static FulfillmentConfig config;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    static {
        // 默认配置
        config = new FulfillmentConfig();
        config.setTaskTypes(Arrays.asList("开票履约", "合同召回提醒", "到期履约-合同到期提醒", "应付任务-合同款付款", "应收任务-合同款收款", "到期任务"));
        config.setKeywords(Arrays.asList("签署日期", "到期日期", "金额", "验收合格后", "开票对象"));
        config.setTimeRules(Arrays.asList("签署后", "生效后", "之日起", "日内", "个自然日", "个工作日"));
        config.setSelectedTaskTypes(new ArrayList<>(config.getTaskTypes()));
        config.setSelectedKeywords(new ArrayList<>(config.getKeywords()));
        config.setSelectedTimeRules(new ArrayList<>(config.getTimeRules()));
    }
    public FulfillmentConfig getConfig() {
        return config;
    }
    public void saveConfig(FulfillmentConfig newConfig) {
        config = newConfig;
    }
    public FulfillmentExtractResult extract(MultipartFile file, String taskTypesJson, String keywordsJson, String timeRulesJson) throws IOException {
        // 伪AI识别逻辑，实际应调用通义千问long
        List<String> taskTypes = objectMapper.readValue(taskTypesJson, new TypeReference<List<String>>(){});
        List<String> keywords = objectMapper.readValue(keywordsJson, new TypeReference<List<String>>(){});
        List<String> timeRules = objectMapper.readValue(timeRulesJson, new TypeReference<List<String>>(){});
        // 这里只返回一个伪任务
        Map<String, Object> task = new HashMap<>();
        task.put("task_type", taskTypes.isEmpty() ? "开票履约" : taskTypes.get(0));
        task.put("keyword", keywords.isEmpty() ? "签署日期" : keywords.get(0));
        task.put("time_rule", timeRules.isEmpty() ? "签署后" : timeRules.get(0));
        task.put("original_file", file.getOriginalFilename());
        FulfillmentExtractResult result = new FulfillmentExtractResult();
        result.setSuccess(true);
        result.setMessage("识别成功（伪数据，待接入通义千问long）");
        result.setTasks(Collections.singletonList(task));
        return result;
    }
}
