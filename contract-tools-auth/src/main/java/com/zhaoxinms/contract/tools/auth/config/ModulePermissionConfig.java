package com.zhaoxinms.contract.tools.auth.config;

import com.zhaoxinms.contract.tools.auth.enums.ModuleType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 模块权限配置
 */
@Configuration
@ConditionalOnProperty(prefix = "zhaoxin.auth", name = "enabled", havingValue = "true", matchIfMissing = false)
public class ModulePermissionConfig {
    
    /**
     * 模块功能映射配置
     * 定义每个模块包含的具体功能
     */
    private static final Map<ModuleType, Set<String>> MODULE_FEATURES = new HashMap<>();
    
    static {
        // 合同模板设计模块
        MODULE_FEATURES.put(ModuleType.CONTRACT_TEMPLATE_DESIGN, Set.of(
            "template_create", "template_edit", "template_delete", 
            "template_preview", "template_export", "template_import"
        ));
        
        // 合同合成模块
        MODULE_FEATURES.put(ModuleType.CONTRACT_SYNTHESIS, Set.of(
            "contract_generate", "contract_merge", "contract_batch_generate",
            "contract_data_binding", "contract_preview"
        ));
        
        // 合同比对PRO模块
        MODULE_FEATURES.put(ModuleType.CONTRACT_COMPARE_PRO, Set.of(
            "contract_compare", "compare_report_generate", "compare_export",
            "gpu_ocr_compare", "intelligent_diff", "compare_batch"
        ));
        
        // 合同信息抽取模块
        MODULE_FEATURES.put(ModuleType.CONTRACT_INFO_EXTRACTION, Set.of(
            "info_extract", "entity_recognition", "key_info_extract",
            "extract_export", "extract_batch", "ocr_extract"
        ));
        
        // 合同智能审核模块
        MODULE_FEATURES.put(ModuleType.CONTRACT_INTELLIGENT_REVIEW, Set.of(
            "contract_review", "risk_analysis", "compliance_check",
            "review_report", "auto_review", "review_rules_config"
        ));
        
        // 履约任务生成模块
        MODULE_FEATURES.put(ModuleType.PERFORMANCE_TASK_GENERATION, Set.of(
            "task_generate", "task_schedule", "task_remind",
            "task_track", "task_export", "task_template"
        ));
    }
    
    /**
     * 获取模块包含的功能列表
     */
    public static Set<String> getModuleFeatures(ModuleType moduleType) {
        return MODULE_FEATURES.getOrDefault(moduleType, Set.of());
    }
    
    /**
     * 检查功能是否属于指定模块
     */
    public static boolean isFeatureInModule(String feature, ModuleType moduleType) {
        Set<String> features = MODULE_FEATURES.get(moduleType);
        return features != null && features.contains(feature);
    }
    
    /**
     * 根据功能名称查找所属模块
     */
    public static ModuleType findModuleByFeature(String feature) {
        for (Map.Entry<ModuleType, Set<String>> entry : MODULE_FEATURES.entrySet()) {
            if (entry.getValue().contains(feature)) {
                return entry.getKey();
            }
        }
        return null;
    }
}
