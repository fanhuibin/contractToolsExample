package com.zhaoxinms.contract.template.sdk.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhaoxinms.contract.template.sdk.validator.FieldConfigValidator;
import com.zhaoxinms.contract.tools.api.common.ApiCode;
import com.zhaoxinms.contract.tools.api.common.ApiResponse;
import com.zhaoxinms.contract.tools.api.dto.BaseField;
import com.zhaoxinms.contract.tools.api.dto.ClauseField;
import com.zhaoxinms.contract.tools.api.dto.CounterpartyField;
import com.zhaoxinms.contract.tools.api.dto.FieldResponse;
import com.zhaoxinms.contract.tools.api.dto.SealField;
import com.zhaoxinms.contract.tools.api.dto.TemplateDesignRequest;
import com.zhaoxinms.contract.tools.api.dto.TemplateDesignResponse;
import com.zhaoxinms.contract.tools.api.exception.BusinessException;
import com.zhaoxinms.contract.tools.auth.annotation.RequireFeature;
import com.zhaoxinms.contract.tools.auth.enums.ModuleType;
import com.zhaoxinms.contract.tools.api.service.TemplateDesignService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.validation.Valid;
import com.zhaoxinms.contract.tools.template.entity.TemplateDesignRecord;
import com.zhaoxinms.contract.tools.template.service.TemplateDesignRecordService;
import com.zhaoxinms.contract.tools.common.service.FileInfoService;
import com.zhaoxinms.contract.tools.config.DemoModeConfig;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 文档在线编辑控制器
 * 
 * 基于OnlyOffice的在线文档编辑器，支持Word模板设计和管理
 * 
 * @author zhaoxin
 * @since 2024-10-09
 */
@RestController
@RequestMapping("/api/template")
@Api(tags = "文档在线编辑")
@RequireFeature(module = ModuleType.DOCUMENT_ONLINE_EDIT, message = "文档在线编辑功能需要授权")
public class TemplateDesignController { 

    @Autowired
    private TemplateDesignService templateDesignService;
    
    @Value("${zxcm.file-upload.root-path:./uploads}")
    private String uploadRootPath;

    @Autowired(required = false)
    private TemplateDesignRecordService designRecordService;

    @Autowired(required = false)
    private FileInfoService fileInfoService;

    @Autowired
    private DemoModeConfig demoModeConfig;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 获取字段信息
     */
    @GetMapping("/fields")
    @ApiOperation(value = "获取字段信息", notes = "获取模板设计中可使用的字段列表")
    public ApiResponse<FieldResponse> getFields() {
        FieldResponse response = templateDesignService.getFields();
        return ApiResponse.success(response);
    }

    /**
     * 通过外部URL获取自定义字段配置
     */
    @GetMapping("/fields/custom")
    @ApiOperation(value = "获取外部自定义字段配置", notes = "通过外部提供的URL获取自定义字段配置，并返回给前端使用")
    public ApiResponse<FieldResponse> getCustomFields(
            @ApiParam(value = "自定义字段配置URL", required = true) @RequestParam("url") String url) {

        if (!StringUtils.hasText(url)) {
            throw BusinessException.of(ApiCode.PARAM_ERROR, "自定义字段配置URL不能为空");
        }

        try {
            String decodedUrl = URLDecoder.decode(url, StandardCharsets.UTF_8);
            ResponseEntity<String> response = restTemplate.exchange(decodedUrl, HttpMethod.GET, null, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw BusinessException.of(ApiCode.SERVER_ERROR, "获取自定义字段配置失败，HTTP状态：" + response.getStatusCodeValue());
            }

            Map<String, Object> body = objectMapper.readValue(response.getBody(),
                    new TypeReference<Map<String, Object>>() {});

            Object data = body.containsKey("data") ? body.get("data") : body;

            Map<String, Object> result = objectMapper.convertValue(
                    data, new TypeReference<Map<String, Object>>() {});

            validateCustomFields(result);

            FieldResponse mapped = mapToFieldResponse(result);

            return ApiResponse.success(mapped);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw BusinessException.of(ApiCode.SERVER_ERROR, "获取自定义字段配置失败：" + e.getMessage());
        }
    }

    private void validateCustomFields(Map<String, Object> config) {
        // 使用统一的校验器进行校验
        FieldConfigValidator.ValidationResult result = FieldConfigValidator.validateMapConfig(config);
        if (!result.isValid()) {
            throw BusinessException.of(ApiCode.PARAM_ERROR, "自定义字段配置不完整: " + result.getErrorMessage());
        }
    }

    private FieldResponse mapToFieldResponse(Map<String, Object> config) {
        FieldResponse response = new FieldResponse();
        response.setBaseFields(mapBaseFields(config.get("baseFields")));
        response.setCounterpartyFields(mapCounterpartyFields(config.get("counterpartyFields")));
        response.setClauseFields(mapClauseFields(config.get("clauseFields")));
        response.setSealFields(mapSealFields(config.get("sealFields")));
        return response;
    }

    @SuppressWarnings("unchecked")
    private List<BaseField> mapBaseFields(Object obj) {
        List<BaseField> result = new ArrayList<>();
        if (!(obj instanceof List)) {
            return result;
        }
        int idx = 1;
        for (Map<String, Object> item : (List<Map<String, Object>>) obj) {
            BaseField field = new BaseField();
            String tag = stringValue(item.get("tag"));
            String code = stringValue(item.get("code"), tag);
            field.setId(StringUtils.hasText(tag) ? tag : "base_" + idx);
            field.setName(stringValue(item.get("name"), field.getId()));
            field.setCode(code);
            field.setIsRichText(booleanValue(item.get("richText")));
            field.setSampleValue(extractSample(item));
            result.add(field);
            idx++;
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<CounterpartyField> mapCounterpartyFields(Object obj) {
        List<CounterpartyField> result = new ArrayList<>();
        if (!(obj instanceof List)) {
            return result;
        }
        int idx = 1;
        for (Map<String, Object> item : (List<Map<String, Object>>) obj) {
            CounterpartyField field = new CounterpartyField();
            String tag = stringValue(item.get("tag"));
            String code = stringValue(item.get("code"), tag);
            field.setId(StringUtils.hasText(tag) ? tag : "party_" + idx);
            field.setName(stringValue(item.get("name"), field.getId()));
            field.setCode(code);
            Integer counterpartyIndex = intValue(item.get("counterpartyIndex"));
            if (counterpartyIndex == null) {
                counterpartyIndex = inferPartyIndex(code, tag);
            }
            field.setCounterpartyIndex(counterpartyIndex);
            field.setSampleValue(extractSample(item));
            result.add(field);
            idx++;
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<ClauseField> mapClauseFields(Object obj) {
        List<ClauseField> result = new ArrayList<>();
        if (!(obj instanceof List)) {
            return result;
        }
        int idx = 1;
        for (Map<String, Object> item : (List<Map<String, Object>>) obj) {
            ClauseField field = new ClauseField();
            String tag = stringValue(item.get("tag"));
            String code = stringValue(item.get("code"), tag);
            field.setId(StringUtils.hasText(tag) ? tag : "clause_" + idx);
            field.setName(stringValue(item.get("name"), field.getId()));
            field.setCode(code);
            field.setContent(stringValue(item.get("content"), extractSample(item)));
            field.setType(stringValue(item.get("type"), "custom"));
            field.setTypeName(stringValue(item.get("typeName"), "自定义条款"));
            field.setSampleValue(extractSample(item));
            result.add(field);
            idx++;
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<SealField> mapSealFields(Object obj) {
        List<SealField> result = new ArrayList<>();
        if (!(obj instanceof List)) {
            return result;
        }
        int idx = 1;
        for (Map<String, Object> item : (List<Map<String, Object>>) obj) {
            SealField field = new SealField();
            String tag = stringValue(item.get("tag"));
            String code = stringValue(item.get("code"), tag);
            field.setId(StringUtils.hasText(tag) ? tag : "seal_" + idx);
            field.setName(stringValue(item.get("name"), field.getId()));
            field.setCode(code);
            field.setType(stringValue(item.get("sealType"), "custom"));
            field.setOrderIndex(intValue(item.get("orderIndex")) != null ? intValue(item.get("orderIndex")) : idx);
            field.setWidth(floatValue(item.get("width")));
            field.setHeight(floatValue(item.get("height")));
            result.add(field);
            idx++;
        }
        return result;
    }

    private String extractSample(Map<String, Object> item) {
        String sample = stringValue(item.get("sampleValue"));
        if (!StringUtils.hasText(sample)) {
            sample = stringValue(item.get("sample"));
        }
        if (!StringUtils.hasText(sample)) {
            sample = stripExamplePrefix(stringValue(item.get("description")));
        }
        return sample;
    }

    private String stripExamplePrefix(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String trimmed = text.trim();
        if (trimmed.startsWith("示例：") || trimmed.startsWith("示例:")) {
            return trimmed.substring(3);
        }
        return trimmed;
    }

    private String stringValue(Object value) {
        return stringValue(value, "");
    }

    private String stringValue(Object value, String defaultValue) {
        String str = value == null ? "" : String.valueOf(value).trim();
        return StringUtils.hasText(str) ? str : defaultValue;
    }

    private boolean booleanValue(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        String str = stringValue(value);
        return "true".equalsIgnoreCase(str) || "1".equals(str);
    }

    private Integer intValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        String str = stringValue(value);
        if (!StringUtils.hasText(str)) {
            return null;
        }
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Float floatValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        String str = stringValue(value);
        if (!StringUtils.hasText(str)) {
            return null;
        }
        try {
            return Float.parseFloat(str);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int inferPartyIndex(String code, String tag) {
        String source = (StringUtils.hasText(code) ? code : "") + (StringUtils.hasText(tag) ? tag : "");
        String lower = source.toLowerCase();
        if (lower.contains("party_b") || lower.contains("supplier") || lower.contains("_b") || lower.contains("seller")) {
            return 2;
        }
        return 1;
    }

    /**
     * 保存模板设计元素
     */
    @PostMapping("/design/save")
    @ApiOperation(value = "保存模板设计", notes = "保存模板设计的元素配置")
    public ApiResponse<TemplateDesignRecord> saveDesign(
            @ApiParam(value = "设计记录", required = true) @RequestBody TemplateDesignRecord body) {
        
        // 演示模式检查
        if (demoModeConfig.isDemoMode()) {
            throw BusinessException.of(ApiCode.FORBIDDEN, "演示环境不允许修改模板");
        }
        
        if (designRecordService == null) {
            throw BusinessException.of(ApiCode.SERVER_ERROR, "设计记录服务不可用");
        }
        
        // 使用新版saveTemplate方法，支持完整字段保存
        TemplateDesignRecord saved = designRecordService.saveTemplate(body);
        
        return ApiResponse.success("保存成功", saved);
    }

    /**
     * 获取模板设计明细
     */
    @GetMapping("/design/detail/{id}")
    @ApiOperation(value = "获取设计详情", notes = "根据记录ID获取模板设计详情")
    public ApiResponse<TemplateDesignRecord> getDesignDetail(
            @ApiParam(value = "记录ID", required = true) @PathVariable("id") String id) {
        
        if (designRecordService == null) {
            throw BusinessException.of(ApiCode.SERVER_ERROR, "设计记录服务不可用");
        }
        
        TemplateDesignRecord record = designRecordService.getById(id);
        if (record == null) {
            throw BusinessException.of(ApiCode.NOT_FOUND, "未找到设计记录: " + id);
        }
        
        return ApiResponse.success(record);
    }

    /**
     * 根据模板ID获取模板设计明细
     */
    @GetMapping("/design/byTemplate/{templateId}")
    @ApiOperation(value = "根据模板ID获取设计", notes = "根据模板ID获取对应的设计记录")
    public ApiResponse<TemplateDesignRecord> getDesignDetailByTemplateId(
            @ApiParam(value = "模板ID", required = true) @PathVariable("templateId") String templateId) {
        
        if (designRecordService == null) {
            throw BusinessException.of(ApiCode.SERVER_ERROR, "设计记录服务不可用");
        }
        
        TemplateDesignRecord record = designRecordService.getByTemplateId(templateId);
        if (record == null) {
            throw BusinessException.of(ApiCode.TEMPLATE_NOT_FOUND, "未找到模板设计记录: " + templateId);
        }
        
        return ApiResponse.success(record);
    }

    /**
     * 发起模板设计
     */
    @PostMapping("/design/start")
    @ApiOperation(value = "发起模板设计", notes = "启动OnlyOffice编辑器进行模板设计")
    public ApiResponse<TemplateDesignResponse> startTemplateDesign(
            @ApiParam(value = "模板设计请求", required = true) 
            @Valid @RequestBody TemplateDesignRequest request) {
        
        TemplateDesignResponse response = templateDesignService.startTemplateDesign(request);
        return ApiResponse.success(response);
    }

    /**
     * 上传模板文档
     */
    @PostMapping(value = "/design/upload", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiOperation(value = "上传模板文档", notes = "上传Word模板文件（仅支持docx格式）")
    public ApiResponse<TemplateDesignRecord> uploadTemplate(
            @ApiParam("模板ID") @RequestParam("templateId") String templateId,
            @ApiParam("Word模板文件，仅支持docx") @RequestPart("file") org.springframework.web.multipart.MultipartFile file) {
        
        // 参数校验
        if (file == null || file.isEmpty()) {
            throw BusinessException.of(ApiCode.FILE_EMPTY);
        }
        
        if (templateId == null || templateId.trim().isEmpty()) {
            throw BusinessException.of(ApiCode.PARAM_ERROR, "模板ID不能为空");
        }
        
        // 文件格式验证
        String filename = file.getOriginalFilename();
        String ext = filename != null && filename.contains(".") 
            ? filename.substring(filename.lastIndexOf('.') + 1).toLowerCase() 
            : "";
        
        if (!"docx".equals(ext)) {
            throw BusinessException.of(ApiCode.FILE_TYPE_NOT_SUPPORTED, 
                "仅支持docx文件，请将doc转换为docx再上传");
        }

        try {
            // 使用标准三阶段上传流程：临时目录 -> files/{年}/{月}/ -> 清理临时文件
            if (fileInfoService == null) {
                throw BusinessException.of(ApiCode.SERVER_ERROR, "文件服务不可用，无法注册上传文件");
            }
            
            // saveNewFile 会自动处理：1.保存到temp-uploads 2.复制到files/{年}/{月}/ 3.注册数据库 4.删除临时文件
            com.zhaoxinms.contract.tools.common.entity.FileInfo info = 
                fileInfoService.saveNewFile(file, "template-design");

            // 保存设计记录（elementsJson 初始为空结构）
            if (designRecordService == null) {
                throw BusinessException.of(ApiCode.SERVER_ERROR, "设计记录服务不可用");
            }
            
            String elementsJson = "{\"elements\":[]}";
            TemplateDesignRecord saved;
            try {
                saved = designRecordService.save(null, templateId.trim(), String.valueOf(info.getId()), elementsJson);
            } catch (IllegalArgumentException ex) {
                throw BusinessException.of(ApiCode.PARAM_ERROR, ex.getMessage());
            }
            
            return ApiResponse.success("上传成功", saved);
            
        } catch (BusinessException e) {
            throw e;  // 重新抛出业务异常
        } catch (Exception e) {
            throw BusinessException.of(ApiCode.FILE_UPLOAD_ERROR, "上传失败: " + e.getMessage());
        }
    }

    /**
     * 模板设计记录列表
     * 
     * @param status 可选的状态筛选参数，支持：PUBLISHED（已发布）、DRAFT（草稿）、DISABLED（已禁用）、DELETED（已删除）
     * @return 模板设计记录列表
     */
    @GetMapping("/design/list")
    @ApiOperation(value = "获取设计列表", notes = "获取模板设计记录列表，可按状态筛选")
    public ApiResponse<List<TemplateDesignRecord>> listDesigns(
            @ApiParam(value = "状态筛选（可选）：PUBLISHED、DRAFT、DISABLED、DELETED", required = false)
            @RequestParam(required = false) String status) {
        if (designRecordService == null) {
            throw BusinessException.of(ApiCode.SERVER_ERROR, "设计记录服务不可用");
        }
        
        List<TemplateDesignRecord> list;
        if (status != null && !status.trim().isEmpty()) {
            // 按状态筛选
            list = designRecordService.listAll().stream()
                    .filter(record -> status.equalsIgnoreCase(record.getStatus()))
                    .collect(java.util.stream.Collectors.toList());
        } else {
            // 返回所有
            list = designRecordService.listAll();
        }
        
        return ApiResponse.success(list);
    }

    /**
     * 删除模板设计记录（软删除）
     */
    @DeleteMapping("/design/detail/{id}")
    @ApiOperation(value = "删除设计记录", notes = "删除指定的模板设计记录")
    public ApiResponse<Void> deleteDesign(
            @ApiParam(value = "记录ID", required = true) @PathVariable("id") String id) {
        
        if (designRecordService == null) {
            throw BusinessException.of(ApiCode.SERVER_ERROR, "设计记录服务不可用");
        }
        
        boolean ok = designRecordService.deleteById(id);
        if (!ok) {
            throw BusinessException.of(ApiCode.NOT_FOUND, "删除失败或记录不存在: " + id);
        }
        
        return ApiResponse.success();
    }

    /**
     * 创建新版本（基于现有版本复制）
     */
    @PostMapping("/design/version/create")
    @ApiOperation(value = "创建新版本", notes = "基于现有版本创建新版本，复制文件和设计元素")
    public ApiResponse<TemplateDesignRecord> createNewVersion(
            @ApiParam(value = "源版本ID", required = true) @RequestParam("sourceId") String sourceId,
            @ApiParam(value = "新版本号", required = true) @RequestParam("newVersion") String newVersion) {
        
        if (designRecordService == null) {
            throw BusinessException.of(ApiCode.SERVER_ERROR, "设计记录服务不可用");
        }
        
        try {
            TemplateDesignRecord newVersionRecord = designRecordService.createNewVersion(sourceId, newVersion);
            return ApiResponse.success("新版本创建成功", newVersionRecord);
        } catch (IllegalArgumentException e) {
            throw BusinessException.of(ApiCode.PARAM_ERROR, e.getMessage());
        } catch (RuntimeException e) {
            throw BusinessException.of(ApiCode.SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * 发布版本
     */
    @PostMapping("/design/version/publish/{id}")
    @ApiOperation(value = "发布版本", notes = "发布指定版本，同时将同编码的其他已发布版本设为草稿")
    public ApiResponse<TemplateDesignRecord> publishVersion(
            @ApiParam(value = "记录ID", required = true) @PathVariable("id") String id) {
        
        if (designRecordService == null) {
            throw BusinessException.of(ApiCode.SERVER_ERROR, "设计记录服务不可用");
        }
        
        try {
            TemplateDesignRecord published = designRecordService.publishVersion(id);
            return ApiResponse.success("版本发布成功", published);
        } catch (IllegalArgumentException e) {
            throw BusinessException.of(ApiCode.PARAM_ERROR, e.getMessage());
        }
    }

    /**
     * 更新状态
     */
    @PostMapping("/design/status/update")
    @ApiOperation(value = "更新状态", notes = "更新模板状态：DRAFT-草稿, PUBLISHED-已发布, DISABLED-已禁用, DELETED-已删除")
    public ApiResponse<TemplateDesignRecord> updateStatus(
            @ApiParam(value = "记录ID", required = true) @RequestParam("id") String id,
            @ApiParam(value = "状态", required = true) @RequestParam("status") String status) {
        
        if (designRecordService == null) {
            throw BusinessException.of(ApiCode.SERVER_ERROR, "设计记录服务不可用");
        }
        
        try {
            TemplateDesignRecord updated = designRecordService.updateStatus(id, status);
            return ApiResponse.success("状态更新成功", updated);
        } catch (IllegalArgumentException e) {
            throw BusinessException.of(ApiCode.PARAM_ERROR, e.getMessage());
        }
    }

    /**
     * 获取模板所有版本
     */
    @GetMapping("/design/versions/{templateCode}")
    @ApiOperation(value = "获取所有版本", notes = "获取指定模板编码的所有版本")
    public ApiResponse<List<TemplateDesignRecord>> getVersions(
            @ApiParam(value = "模板编码", required = true) @PathVariable("templateCode") String templateCode) {
        
        if (designRecordService == null) {
            throw BusinessException.of(ApiCode.SERVER_ERROR, "设计记录服务不可用");
        }
        
        List<TemplateDesignRecord> versions = designRecordService.getVersionsByCode(templateCode);
        return ApiResponse.success(versions);
    }

    /**
     * 获取最新版本
     */
    @GetMapping("/design/version/latest/{templateCode}")
    @ApiOperation(value = "获取最新版本", notes = "获取指定模板编码的最新版本")
    public ApiResponse<TemplateDesignRecord> getLatestVersion(
            @ApiParam(value = "模板编码", required = true) @PathVariable("templateCode") String templateCode) {
        
        if (designRecordService == null) {
            throw BusinessException.of(ApiCode.SERVER_ERROR, "设计记录服务不可用");
        }
        
        TemplateDesignRecord latest = designRecordService.getLatestByCode(templateCode);
        if (latest == null) {
            throw BusinessException.of(ApiCode.NOT_FOUND, "未找到模板: " + templateCode);
        }
        
        return ApiResponse.success(latest);
    }

    /**
     * 获取已发布版本
     */
    @GetMapping("/design/version/published/{templateCode}")
    @ApiOperation(value = "获取已发布版本", notes = "获取指定模板编码的已发布版本")
    public ApiResponse<TemplateDesignRecord> getPublishedVersion(
            @ApiParam(value = "模板编码", required = true) @PathVariable("templateCode") String templateCode) {
        
        if (designRecordService == null) {
            throw BusinessException.of(ApiCode.SERVER_ERROR, "设计记录服务不可用");
        }
        
        TemplateDesignRecord published = designRecordService.getPublishedByCode(templateCode);
        if (published == null) {
            throw BusinessException.of(ApiCode.NOT_FOUND, "未找到已发布的模板: " + templateCode);
        }
        
        return ApiResponse.success(published);
    }
}
