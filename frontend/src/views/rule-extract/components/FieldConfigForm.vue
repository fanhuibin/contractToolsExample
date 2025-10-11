<template>
  <div class="field-config-form">
    <!-- 基本信息 -->
    <div class="form-section">
      <div class="section-title">基本信息</div>
      <el-form label-width="90px" size="small">
        <el-form-item label="字段名称" required>
          <el-input 
            :model-value="fieldName" 
            @input="handleFieldNameChange"
            placeholder="如：合同名称" 
          />
        </el-form-item>
        <el-form-item label="字段编码" required>
          <el-input 
            :model-value="fieldCode" 
            @input="handleFieldCodeChange"
            placeholder="如：contract_name" 
          />
        </el-form-item>
        <el-form-item label="字段类型">
          <el-select 
            :model-value="fieldType" 
            @change="handleFieldTypeChange" 
            style="width: 100%"
          >
            <el-option label="string - 字符串" value="string" />
            <el-option label="number - 数字" value="number" />
            <el-option label="date - 日期" value="date" />
            <el-option label="amount - 金额" value="amount" />
            <el-option label="boolean - 布尔值" value="boolean" />
            <el-option label="table - 表格" value="table" />
          </el-select>
        </el-form-item>
        <el-form-item label="必填">
          <el-switch 
            :model-value="required" 
            @change="handleRequiredChange"
          />
        </el-form-item>
      </el-form>
    </div>

    <el-divider />

    <!-- 规则类型选择 -->
    <div class="form-section">
      <div class="section-title">提取规则</div>
      <el-radio-group 
        :model-value="ruleType" 
        @change="handleRuleTypeChange" 
        class="rule-type-selector" 
        size="small"
      >
        <el-radio-button label="KEYWORD_ANCHOR">关键词锚点</el-radio-button>
        <el-radio-button label="CONTEXT_BOUNDARY">上下文边界</el-radio-button>
        <el-radio-button label="REGEX_PATTERN">正则表达式</el-radio-button>
        <el-radio-button label="TABLE_CELL">表格抽取</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 规则配置 -->
    <div class="form-section rule-config-section">
      <!-- 关键词锚点 -->
      <template v-if="ruleType === 'KEYWORD_ANCHOR'">
        <el-alert type="info" :closable="false" style="margin-bottom: 12px;">
          先找到关键词位置，然后在其附近提取内容
        </el-alert>
        
        <el-form label-width="90px" size="small">
          <el-form-item label="锚点关键词">
            <el-input 
              :model-value="getConfigValue('anchor')" 
              @input="(val: string) => handleConfigChange('anchor', val)"
              placeholder="如：合同名称"
            >
              <template #append>
                <el-tooltip content="支持用 | 分隔多个关键词" placement="top">
                  <el-icon><QuestionFilled /></el-icon>
                </el-tooltip>
              </template>
            </el-input>
          </el-form-item>
          
          <el-form-item label="提取方向">
            <el-select 
              :model-value="getConfigValue('direction')" 
              @change="(val: string) => handleConfigChange('direction', val)" 
              style="width: 100%"
            >
              <el-option label="之后 (after)" value="after" />
              <el-option label="之前 (before)" value="before" />
              <el-option label="前后都找 (both)" value="both" />
            </el-select>
          </el-form-item>
          
          <el-form-item label="提取方法">
            <el-select 
              :model-value="getConfigValue('extractMethod')" 
              @change="(val: string) => handleConfigChange('extractMethod', val)" 
              style="width: 100%"
            >
              <el-option label="正则表达式" value="regex" />
              <el-option label="按行提取" value="line" />
              <el-option label="按分隔符" value="delimiter" />
            </el-select>
          </el-form-item>
          
          <el-form-item v-if="getConfigValue('extractMethod') === 'regex'" label="正则表达式">
            <el-input 
              :model-value="getConfigValue('pattern')" 
              @input="(val: string) => handleConfigChange('pattern', val)"
              placeholder="如：.*" 
            >
              <template #append>
                <el-button @click="openRegexPicker">
                  <el-icon><Collection /></el-icon>
                  选择常用正则
                </el-button>
              </template>
            </el-input>
            <div class="hint" style="margin-top: 8px; color: #909399;">
              点击右侧按钮可选择30+种常用正则表达式，或直接输入自定义正则
            </div>
          </el-form-item>
          
          <el-form-item v-if="getConfigValue('extractMethod') === 'line'" label="行后处理">
            <el-input 
              :model-value="getConfigValue('pattern')" 
              @input="(val: string) => handleConfigChange('pattern', val)"
              placeholder="留空则返回整行，或用正则进一步提取" 
            >
              <template #append>
                <el-button @click="openRegexPicker">
                  <el-icon><Collection /></el-icon>
                  选择常用正则
                </el-button>
              </template>
            </el-input>
            <div class="hint" style="margin-top: 8px; color: #909399;">
              示例：行内容为"合同金额：1000元"，用正则 <code>\d+</code> 可提取出"1000"
            </div>
          </el-form-item>
          
          <el-form-item v-if="getConfigValue('extractMethod') === 'delimiter'" label="分隔符">
            <el-input 
              :model-value="getConfigValue('delimiter')" 
              @input="(val: string) => handleConfigChange('delimiter', val)"
              placeholder="如：：" 
              style="width: 150px;" 
            />
          </el-form-item>
          
          <el-form-item v-if="getConfigValue('extractMethod') === 'delimiter'" label="分隔后处理">
            <el-input 
              :model-value="getConfigValue('pattern')" 
              @input="(val: string) => handleConfigChange('pattern', val)"
              placeholder="留空则返回分隔后的内容，或用正则进一步提取" 
            >
              <template #append>
                <el-button @click="openRegexPicker">
                  <el-icon><Collection /></el-icon>
                  选择常用正则
                </el-button>
              </template>
            </el-input>
            <div class="hint" style="margin-top: 8px; color: #909399;">
              对分隔后的内容再应用正则表达式提取
            </div>
          </el-form-item>
          
          <el-form-item label="最大距离">
            <el-input-number 
              :model-value="getConfigValue('maxDistance')" 
              @change="(val: number) => handleConfigChange('maxDistance', val)"
              :min="10" 
              :max="1000" 
              style="width: 100%;" 
            />
          </el-form-item>
          
          <el-form-item label="多行匹配">
            <el-switch 
              :model-value="getConfigValue('multiline')" 
              @change="(val: boolean) => handleConfigChange('multiline', val)"
            />
          </el-form-item>

          <MatchModeConfig 
            :model-value="ruleConfig" 
            @update:model-value="handleRuleConfigUpdate"
          />
        </el-form>
      </template>

      <!-- 上下文边界 -->
      <template v-else-if="ruleType === 'CONTEXT_BOUNDARY'">
        <el-alert type="info" :closable="false" style="margin-bottom: 12px;">
          提取两个明确标记之间的内容
        </el-alert>
        
        <el-form label-width="90px" size="small">
          <el-form-item label="开始边界">
            <el-input 
              :model-value="getConfigValue('startBoundary')" 
              @input="(val: string) => handleConfigChange('startBoundary', val)"
              placeholder="如：合同主要内容如下" 
            />
          </el-form-item>
          
          <el-form-item label="结束边界">
            <el-input 
              :model-value="getConfigValue('endBoundary')" 
              @input="(val: string) => handleConfigChange('endBoundary', val)"
              placeholder="如：甲方（盖章）" 
            />
          </el-form-item>
          
          <el-form-item label="提取Pattern">
            <el-input 
              :model-value="getConfigValue('extractPattern')" 
              @input="(val: string) => handleConfigChange('extractPattern', val)"
              placeholder="留空则返回全部，或输入正则表达式" 
            >
              <template #append>
                <el-button @click="openRegexPickerForExtractPattern">
                  <el-icon><Collection /></el-icon>
                  选择常用正则
                </el-button>
              </template>
            </el-input>
            <div class="hint" style="margin-top: 8px; color: #909399;">
              留空则返回边界内的全部内容，或使用正则表达式进一步提取特定内容
            </div>
          </el-form-item>
          
          <el-form-item label="多行匹配">
            <el-switch 
              :model-value="getConfigValue('multiline')" 
              @change="(val: boolean) => handleConfigChange('multiline', val)"
            />
          </el-form-item>

          <MatchModeConfig 
            :model-value="ruleConfig" 
            @update:model-value="handleRuleConfigUpdate"
          />
        </el-form>
      </template>

      <!-- 正则表达式 -->
      <template v-else-if="ruleType === 'REGEX_PATTERN'">
        <el-alert type="info" :closable="false" style="margin-bottom: 12px;">
          直接用正则匹配，适合熟悉正则的用户
        </el-alert>
        
        <el-form label-width="90px" size="small">
          <el-form-item label="正则表达式">
            <el-input 
              :model-value="getConfigValue('pattern')" 
              @input="(val: string) => handleConfigChange('pattern', val)"
              type="textarea"
              :rows="3"
              placeholder="输入正则表达式，如：.*（匹配任意字符）" 
            />
            <div style="margin-top: 8px;">
              <el-button size="small" type="primary" @click="openRegexPicker">
                <el-icon><Collection /></el-icon>
                选择常用正则表达式（30+种）
              </el-button>
              <span style="margin-left: 8px; font-size: 12px; color: #909399;">
                点击按钮可选择数字、金额、日期、邮箱等常用正则
              </span>
            </div>
          </el-form-item>
          
          <el-form-item label="捕获组">
            <el-input-number 
              :model-value="getConfigValue('group')" 
              @change="(val: number) => handleConfigChange('group', val)"
              :min="0" 
              :max="10" 
              style="width: 100%;" 
            />
            <div class="hint">默认0（整个匹配），1表示第一个括号内的内容</div>
          </el-form-item>
          
          <el-form-item label="多行模式">
            <el-switch 
              :model-value="getConfigValue('multiline')" 
              @change="(val: boolean) => handleConfigChange('multiline', val)"
            />
            <div class="hint">开启后，^和$可以匹配每一行的开始和结束</div>
          </el-form-item>

          <MatchModeConfig 
            :model-value="ruleConfig" 
            @update:model-value="handleRuleConfigUpdate"
          />
        </el-form>
      </template>

      <!-- 表格抽取 -->
      <template v-else-if="ruleType === 'TABLE_CELL'">
        <el-alert type="info" :closable="false" style="margin-bottom: 12px;">
          从HTML表格中提取数据，支持单元格和整表两种模式
        </el-alert>
        
        <el-form label-width="100px" size="small">
          <el-form-item label="提取模式">
            <el-radio-group 
              :model-value="getConfigValue('extractMode')" 
              @change="(val: string) => handleConfigChange('extractMode', val)"
            >
              <el-radio label="cell">单元格 - 提取特定单元格的值</el-radio>
              <el-radio label="table">整表 - 提取完整表格数据</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="表头特征" required>
            <el-input 
              :model-value="getConfigValue('headerPattern')" 
              @input="(val: string) => handleConfigChange('headerPattern', val)"
              placeholder="如：商品名称|数量|单价"
            >
              <template #append>
                <el-tooltip placement="top">
                  <template #content>
                    <div style="max-width: 300px;">
                      <p>用于识别目标表格的表头关键词</p>
                      <p>使用 | 分隔多个关键词</p>
                      <p>示例：商品名称|数量|单价</p>
                    </div>
                  </template>
                  <el-icon><QuestionFilled /></el-icon>
                </el-tooltip>
              </template>
            </el-input>
          </el-form-item>

          <template v-if="getConfigValue('extractMode') === 'cell'">
            <el-divider content-position="left">单元格定位</el-divider>
            
            <el-form-item label="目标列">
              <el-input 
                :model-value="getConfigValue('targetColumn')" 
                @input="(val: string) => handleConfigChange('targetColumn', val)"
                placeholder="如：商品名称"
              />
            </el-form-item>

            <el-form-item label="列索引">
              <el-input-number 
                :model-value="getConfigValue('columnIndex')" 
                @change="(val: number | null) => handleConfigChange('columnIndex', val)"
                :min="1" 
                :max="50" 
                style="width: 100%;"
              />
              <div class="hint">留空则使用目标列名</div>
            </el-form-item>

            <el-form-item label="行标识">
              <el-input 
                :model-value="getConfigValue('rowMarker')" 
                @input="(val: string) => handleConfigChange('rowMarker', val)"
                placeholder="如：合计"
              />
            </el-form-item>

            <el-form-item label="行索引">
              <el-input-number 
                :model-value="getConfigValue('rowIndex')" 
                @change="(val: number | null) => handleConfigChange('rowIndex', val)"
                :min="1" 
                :max="1000"
                style="width: 100%;"
              />
              <div class="hint">留空则使用行标识</div>
            </el-form-item>

            <MatchModeConfig 
              :model-value="ruleConfig" 
              @update:model-value="handleRuleConfigUpdate"
            />
          </template>

          <template v-else-if="getConfigValue('extractMode') === 'table'">
            <el-divider content-position="left">输出格式</el-divider>
            
            <el-form-item label="格式">
              <el-select 
                :model-value="getConfigValue('format')" 
                @change="(val: string) => handleConfigChange('format', val)" 
                style="width: 100%"
              >
                <el-option label="JSON - 结构化数据，适合程序处理" value="json" />
                <el-option label="Markdown - 表格格式，易读" value="markdown" />
                <el-option label="HTML - 原始HTML格式" value="html" />
              </el-select>
            </el-form-item>

            <el-alert type="success" :closable="false" style="margin-top: 12px;">
              <template #title>
                <div style="font-size: 13px;">
                  整表模式将返回完整的表格数据，调试时会显示表格预览
                </div>
              </template>
            </el-alert>
          </template>
        </el-form>
      </template>
    </div>

    <!-- 正则表达式选择对话框 -->
    <RegexPickerDialog
      v-model="regexPickerVisible"
      @select="handleRegexSelectWithContext"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { QuestionFilled, Collection } from '@element-plus/icons-vue'
import MatchModeConfig from './MatchModeConfig.vue'
import RegexPickerDialog from './RegexPickerDialog.vue'

interface FieldData {
  fieldName: string
  fieldCode: string
  fieldType: string
  required: boolean
  ruleType: string
  ruleConfig: any
}

interface Props {
  modelValue: FieldData
}

const props = defineProps<Props>()

// 事件处理
const emit = defineEmits<{
  'update:modelValue': [value: FieldData]
}>()

// 使用 computed 读取值
const fieldName = computed(() => props.modelValue.fieldName)
const fieldCode = computed(() => props.modelValue.fieldCode)
const fieldType = computed(() => props.modelValue.fieldType || 'string')
const required = computed(() => props.modelValue.required || false)
const ruleType = computed(() => props.modelValue.ruleType || 'KEYWORD_ANCHOR')
const ruleConfig = computed(() => props.modelValue.ruleConfig || {})

// 正则表达式选择对话框
const regexPickerVisible = ref(false)

// 初始化时确保有默认值
onMounted(() => {
  // 如果没有 ruleType，设置默认值
  if (!props.modelValue.ruleType) {
    emit('update:modelValue', {
      ...props.modelValue,
      ruleType: 'KEYWORD_ANCHOR',
      fieldType: props.modelValue.fieldType || 'string',
      required: props.modelValue.required || false
    })
  }
})

// 常用正则表达式库（用于快捷按钮）
const PATTERNS: Record<string, string> = {
  integer: '-?\\d+',
  number: '-?\\d+(\\.\\d+)?',
  amountSimple: '\\d+(\\.\\d{1,2})?',
  amountWithUnit: '\\d+(\\.\\d+)?[千万亿]',
  amountWithYuan: '\\d+(\\.\\d{1,2})?元',
  amountWithComma: '\\d{1,3}(,\\d{3})*(\\.\\d{1,2})?',
  dateYMD: '\\d{4}-\\d{1,2}-\\d{1,2}',
  dateChinese: '\\d{4}年\\d{1,2}月\\d{1,2}日',
  chinese: '[\\u4e00-\\u9fa5]+',
  email: '[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}',
  phone: '1[3-9]\\d{9}'
}

// 获取配置值
const getConfigValue = (key: string) => {
  return ruleConfig.value?.[key] ?? getDefaultValue(ruleType.value, key)
}

// 获取默认值
const getDefaultValue = (type: string, key: string) => {
  const defaults: Record<string, any> = {
    KEYWORD_ANCHOR: {
      anchor: '',
      direction: 'after',
      extractMethod: 'regex',
      pattern: '.*',
      maxDistance: 200,
      delimiter: '：',
      multiline: false,
      matchMode: 'single',
      occurrence: 1,
      returnAll: false
    },
    CONTEXT_BOUNDARY: {
      startBoundary: '',
      endBoundary: '',
      extractPattern: '',
      multiline: false,
      matchMode: 'single',
      occurrence: 1,
      returnAll: false
    },
    REGEX_PATTERN: {
      pattern: '.*',
      group: 0,
      multiline: false,
      matchMode: 'single',
      occurrence: 1,
      returnAll: false
    },
    TABLE_CELL: {
      extractMode: 'cell',
      headerPattern: '',
      targetColumn: '',
      columnIndex: null,
      rowMarker: '',
      rowIndex: null,
      format: 'json',
      matchMode: 'single',
      occurrence: 1,
      returnAll: false
    }
  }
  return defaults[type]?.[key]
}

// 初始化规则配置
const initRuleConfig = (type: string) => {
  const defaults: Record<string, any> = {
    KEYWORD_ANCHOR: {
      anchor: '',
      direction: 'after',
      extractMethod: 'regex',
      pattern: '.*',
      maxDistance: 200,
      delimiter: '：',
      multiline: false,
      matchMode: 'single',
      occurrence: 1,
      returnAll: false
    },
    CONTEXT_BOUNDARY: {
      startBoundary: '',
      endBoundary: '',
      extractPattern: '',
      multiline: false,
      matchMode: 'single',
      occurrence: 1,
      returnAll: false
    },
    REGEX_PATTERN: {
      pattern: '.*',
      group: 0,
      multiline: false,
      matchMode: 'single',
      occurrence: 1,
      returnAll: false
    },
    TABLE_CELL: {
      extractMode: 'cell',
      headerPattern: '',
      targetColumn: '',
      columnIndex: null,
      rowMarker: '',
      rowIndex: null,
      format: 'json',
      matchMode: 'single',
      occurrence: 1,
      returnAll: false
    }
  }
  return defaults[type] || {}
}

// 暴露 getData 方法
defineExpose({
  getData: () => {
    return {
      fieldName: fieldName.value,
      fieldCode: fieldCode.value,
      fieldType: fieldType.value,
      required: required.value,
      ruleType: ruleType.value,
      ruleConfig: ruleConfig.value
    }
  }
})

const emitUpdate = (updates: Partial<FieldData>) => {
  emit('update:modelValue', {
    ...props.modelValue,
    ...updates
  })
}

const handleFieldNameChange = (value: string) => {
  emitUpdate({ fieldName: value })
}

const handleFieldCodeChange = (value: string) => {
  emitUpdate({ fieldCode: value })
}

const handleFieldTypeChange = (value: string) => {
  emitUpdate({ fieldType: value })
}

const handleRequiredChange = (value: boolean) => {
  emitUpdate({ required: value })
}

const handleRuleTypeChange = (value: string) => {
  emitUpdate({
    ruleType: value,
    ruleConfig: initRuleConfig(value)
  })
}

const handleConfigChange = (key: string, value: any) => {
  emitUpdate({
    ruleConfig: {
      ...ruleConfig.value,
      [key]: value
    }
  })
}

const handleRuleConfigUpdate = (newConfig: any) => {
  emitUpdate({ ruleConfig: newConfig })
}

const applyPattern = (key: string) => {
  if (PATTERNS[key]) {
    handleConfigChange('pattern', PATTERNS[key])
  }
}

const openRegexPicker = () => {
  regexPickerVisible.value = true
}

const handleRegexSelect = (pattern: string) => {
  handleConfigChange('pattern', pattern)
}

const openRegexPickerForExtractPattern = () => {
  regexPickerVisible.value = true
  // 标记当前是为 extractPattern 选择
  isSelectingForExtractPattern.value = true
}

const isSelectingForExtractPattern = ref(false)

// 监听正则选择，根据上下文决定更新哪个字段
const handleRegexSelectWithContext = (pattern: string) => {
  if (isSelectingForExtractPattern.value) {
    handleConfigChange('extractPattern', pattern)
    isSelectingForExtractPattern.value = false
  } else {
    handleConfigChange('pattern', pattern)
  }
}
</script>

<style scoped lang="scss">
.field-config-form {
  .form-section {
    margin-bottom: 20px;

    .section-title {
      font-size: 15px;
      font-weight: 600;
      color: #303133;
      margin-bottom: 12px;
      padding-bottom: 8px;
      border-bottom: 2px solid #409eff;
    }
  }

  .rule-type-selector {
    width: 100%;
    display: flex;
    flex-wrap: wrap;
  }

  .rule-config-section {
    background: #f5f7fa;
    padding: 16px;
    border-radius: 4px;
  }

  .hint {
    font-size: 12px;
    color: #909399;
    margin-top: 4px;
  }
}
</style>

