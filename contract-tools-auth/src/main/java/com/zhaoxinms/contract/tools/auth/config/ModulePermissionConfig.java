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
        // 智能文档抽取模块
        MODULE_FEATURES.put(ModuleType.SMART_DOCUMENT_EXTRACTION, Set.of(
            "info_extract", "entity_recognition", "key_info_extract",
            "extract_export", "extract_batch", "ocr_extract"
        ));
        
        // 智能文档比对模块
        MODULE_FEATURES.put(ModuleType.SMART_DOCUMENT_COMPARE, Set.of(
            "contract_compare", "compare_report_generate", "compare_export",
            "gpu_ocr_compare", "intelligent_diff", "compare_batch"
        ));
        
        // 智能合同合成模块
        MODULE_FEATURES.put(ModuleType.SMART_CONTRACT_SYNTHESIS, Set.of(
            "contract_generate", "contract_merge", "contract_batch_generate",
            "contract_data_binding", "contract_preview"
        ));
        
        // 智能文档解析模块
        MODULE_FEATURES.put(ModuleType.SMART_DOCUMENT_PARSE, Set.of(
            "document_parse", "ocr_recognition", "layout_analysis",
            "parse_export", "parse_batch"
        ));
        
        // 文档在线编辑模块
        MODULE_FEATURES.put(ModuleType.DOCUMENT_ONLINE_EDIT, Set.of(
            "template_create", "template_edit", "template_delete", 
            "template_preview", "template_export", "template_import"
        ));
        
        // 文档格式转换模块
        MODULE_FEATURES.put(ModuleType.DOCUMENT_FORMAT_CONVERT, Set.of(
            "format_convert", "pdf_convert", "word_convert",
            "convert_batch", "convert_preview"
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
