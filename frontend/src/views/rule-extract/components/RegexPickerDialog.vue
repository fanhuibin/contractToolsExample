<template>
  <el-dialog
    v-model="dialogVisible"
    title="选择常用正则表达式"
    width="900px"
    :close-on-click-modal="false"
  >
    <div class="regex-picker">
      <!-- 搜索框 -->
      <el-input
        v-model="searchKeyword"
        placeholder="搜索正则表达式（名称或描述）"
        clearable
        style="margin-bottom: 16px;"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>

      <!-- 分类列表 -->
      <el-tabs v-model="activeTab">
        <el-tab-pane label="全部" name="all">
          <div class="regex-list">
            <div
              v-for="item in filteredAllPatterns"
              :key="item.key"
              class="regex-item"
              @click="selectPattern(item)"
            >
              <div class="regex-header">
                <span class="regex-name">{{ item.name }}</span>
                <el-tag :type="getCategoryColor(item.category)" size="small">{{ item.category }}</el-tag>
              </div>
              <div class="regex-pattern">{{ item.pattern }}</div>
              <div class="regex-description">{{ item.description }}</div>
              <div class="regex-examples" v-if="item.examples && item.examples.length > 0">
                <span class="examples-label">示例：</span>
                <el-tag v-for="(ex, idx) in item.examples" :key="idx" size="small" type="info" style="margin-right: 4px;">{{ ex }}</el-tag>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="数字匹配" name="number">
          <div class="regex-list">
            <div
              v-for="item in getPatternsByCategory('数字匹配')"
              :key="item.key"
              class="regex-item"
              @click="selectPattern(item)"
            >
              <div class="regex-header">
                <span class="regex-name">{{ item.name }}</span>
              </div>
              <div class="regex-pattern">{{ item.pattern }}</div>
              <div class="regex-description">{{ item.description }}</div>
              <div class="regex-examples" v-if="item.examples && item.examples.length > 0">
                <span class="examples-label">示例：</span>
                <el-tag v-for="(ex, idx) in item.examples" :key="idx" size="small" type="info" style="margin-right: 4px;">{{ ex }}</el-tag>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="金额匹配" name="amount">
          <div class="regex-list">
            <div
              v-for="item in getPatternsByCategory('金额匹配')"
              :key="item.key"
              class="regex-item"
              @click="selectPattern(item)"
            >
              <div class="regex-header">
                <span class="regex-name">{{ item.name }}</span>
              </div>
              <div class="regex-pattern">{{ item.pattern }}</div>
              <div class="regex-description">{{ item.description }}</div>
              <div class="regex-examples" v-if="item.examples && item.examples.length > 0">
                <span class="examples-label">示例：</span>
                <el-tag v-for="(ex, idx) in item.examples" :key="idx" size="small" type="info" style="margin-right: 4px;">{{ ex }}</el-tag>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="日期匹配" name="date">
          <div class="regex-list">
            <div
              v-for="item in getPatternsByCategory('日期匹配')"
              :key="item.key"
              class="regex-item"
              @click="selectPattern(item)"
            >
              <div class="regex-header">
                <span class="regex-name">{{ item.name }}</span>
              </div>
              <div class="regex-pattern">{{ item.pattern }}</div>
              <div class="regex-description">{{ item.description }}</div>
              <div class="regex-examples" v-if="item.examples && item.examples.length > 0">
                <span class="examples-label">示例：</span>
                <el-tag v-for="(ex, idx) in item.examples" :key="idx" size="small" type="info" style="margin-right: 4px;">{{ ex }}</el-tag>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="文本匹配" name="text">
          <div class="regex-list">
            <div
              v-for="item in getPatternsByCategory('文本匹配')"
              :key="item.key"
              class="regex-item"
              @click="selectPattern(item)"
            >
              <div class="regex-header">
                <span class="regex-name">{{ item.name }}</span>
              </div>
              <div class="regex-pattern">{{ item.pattern }}</div>
              <div class="regex-description">{{ item.description }}</div>
              <div class="regex-examples" v-if="item.examples && item.examples.length > 0">
                <span class="examples-label">示例：</span>
                <el-tag v-for="(ex, idx) in item.examples" :key="idx" size="small" type="info" style="margin-right: 4px;">{{ ex }}</el-tag>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="特殊格式" name="special">
          <div class="regex-list">
            <div
              v-for="item in getPatternsByCategory('特殊格式')"
              :key="item.key"
              class="regex-item"
              @click="selectPattern(item)"
            >
              <div class="regex-header">
                <span class="regex-name">{{ item.name }}</span>
              </div>
              <div class="regex-pattern">{{ item.pattern }}</div>
              <div class="regex-description">{{ item.description }}</div>
              <div class="regex-examples" v-if="item.examples && item.examples.length > 0">
                <span class="examples-label">示例：</span>
                <el-tag v-for="(ex, idx) in item.examples" :key="idx" size="small" type="info" style="margin-right: 4px;">{{ ex }}</el-tag>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Search } from '@element-plus/icons-vue'

interface RegexPattern {
  key: string
  name: string
  category: string
  pattern: string
  description: string
  examples?: string[]
}

interface Props {
  modelValue: boolean
}

const props = defineProps<Props>()
const emit = defineEmits(['update:modelValue', 'select'])

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const searchKeyword = ref('')
const activeTab = ref('all')

// 完整的正则表达式库（35种）
const regexPatterns: RegexPattern[] = [
  // 数字匹配（5种）
  {
    key: 'integer',
    name: '整数',
    category: '数字匹配',
    pattern: '-?\\d+',
    description: '匹配整数，包括正数和负数',
    examples: ['123', '-456', '0']
  },
  {
    key: 'positiveInteger',
    name: '正整数',
    category: '数字匹配',
    pattern: '\\d+',
    description: '匹配正整数（不包括0）',
    examples: ['123', '456', '789']
  },
  {
    key: 'decimal',
    name: '小数',
    category: '数字匹配',
    pattern: '-?\\d+\\.\\d+',
    description: '匹配小数，包括正数和负数',
    examples: ['123.45', '-67.89', '0.123']
  },
  {
    key: 'positiveDecimal',
    name: '正小数',
    category: '数字匹配',
    pattern: '\\d+\\.\\d+',
    description: '匹配正小数',
    examples: ['123.45', '0.99', '3.14']
  },
  {
    key: 'number',
    name: '数字（整数或小数）',
    category: '数字匹配',
    pattern: '-?\\d+(\\.\\d+)?',
    description: '匹配任意数字，包括整数和小数',
    examples: ['123', '456.78', '-90.12']
  },

  // 金额匹配（9种）
  {
    key: 'amountUniversal',
    name: '通用金额（推荐）⭐',
    category: '金额匹配',
    pattern: '[¥$￥]?\\s*([\\d,]+(\\.\\d{1,2})?)\\s*[千万亿]?\\s*元?',
    description: '通用金额正则，支持：纯数字、千分位、货币符号、中文单位、元字等多种格式，可匹配任意长度的数字',
    examples: ['123', '31321132', '1,234.56', '¥1,234.00', '100万', '12.5万元', '$1234', '1,234,567.89元', '￥31321132']
  },
  {
    key: 'amountSimple',
    name: '纯数字金额',
    category: '金额匹配',
    pattern: '\\d+(\\.\\d{1,2})?',
    description: '匹配纯数字金额，整数或带1-2位小数',
    examples: ['123', '123.45', '1234567.89', '99.9']
  },
  {
    key: 'amountWithUnit',
    name: '带中文单位金额',
    category: '金额匹配',
    pattern: '\\d+(\\.\\d+)?[千万亿]',
    description: '匹配带中文单位的金额（千/万/亿）',
    examples: ['12万', '3.5亿', '500万', '2千', '1.5万']
  },
  {
    key: 'amountWithYuan',
    name: '带"元"字金额',
    category: '金额匹配',
    pattern: '\\d+(\\.\\d{1,2})?元',
    description: '匹配带"元"字的金额',
    examples: ['123元', '1236.01元', '99.9元']
  },
  {
    key: 'amountWithUnitYuan',
    name: '带单位和元字',
    category: '金额匹配',
    pattern: '\\d+(\\.\\d+)?[千万亿]元',
    description: '匹配带中文单位和"元"字的金额',
    examples: ['12万元', '3.5亿元', '2500万元']
  },
  {
    key: 'amountWithComma',
    name: '带千分位的金额',
    category: '金额匹配',
    pattern: '[\\d,]+(\\.\\d{1,2})?',
    description: '匹配带千分位分隔符的金额（也可以无千分位）',
    examples: ['1,234.56', '1,234,567.89', '123.45', '1234567.89']
  },
  {
    key: 'amountWithSymbol',
    name: '带货币符号的金额',
    category: '金额匹配',
    pattern: '[¥$€£]\\s?\\d+(\\.\\d{1,2})?',
    description: '匹配带货币符号的金额',
    examples: ['¥123.45', '$1234.56', '€ 100.00']
  },
  {
    key: 'amountChineseUpper',
    name: '中文大写金额',
    category: '金额匹配',
    pattern: '[壹贰叁肆伍陆柒捌玖拾佰仟万亿圆角分整]+',
    description: '匹配中文大写金额（财务用）',
    examples: ['壹万贰仟叁佰肆拾伍元整', '伍佰元整']
  },
  {
    key: 'amountChineseLower',
    name: '中文小写金额',
    category: '金额匹配',
    pattern: '[一二三四五六七八九十百千万亿]+',
    description: '匹配中文小写数字金额',
    examples: ['一千二百', '三万五千', '五百万']
  },

  // 日期匹配（4种）
  {
    key: 'dateYMD',
    name: 'YYYY-MM-DD格式',
    category: '日期匹配',
    pattern: '\\d{4}-\\d{1,2}-\\d{1,2}',
    description: '匹配YYYY-MM-DD格式的日期',
    examples: ['2024-10-10', '2024-1-1', '2023-12-31']
  },
  {
    key: 'dateYMDSlash',
    name: 'YYYY/MM/DD格式',
    category: '日期匹配',
    pattern: '\\d{4}/\\d{1,2}/\\d{1,2}',
    description: '匹配YYYY/MM/DD格式的日期',
    examples: ['2024/10/10', '2024/1/1', '2023/12/31']
  },
  {
    key: 'dateChinese',
    name: '中文日期',
    category: '日期匹配',
    pattern: '\\d{4}年\\d{1,2}月\\d{1,2}日',
    description: '匹配中文格式的日期',
    examples: ['2024年10月10日', '2024年1月1日']
  },
  {
    key: 'dateTimeFull',
    name: '完整日期时间',
    category: '日期匹配',
    pattern: '\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2}',
    description: '匹配完整的日期时间',
    examples: ['2024-10-10 14:30:00', '2023-12-31 23:59:59']
  },

  // 文本匹配（6种）
  {
    key: 'chinese',
    name: '中文字符',
    category: '文本匹配',
    pattern: '[\\u4e00-\\u9fa5]+',
    description: '匹配中文字符',
    examples: ['中国', '合同', '甲方']
  },
  {
    key: 'english',
    name: '英文字符',
    category: '文本匹配',
    pattern: '[a-zA-Z]+',
    description: '匹配英文字符',
    examples: ['Hello', 'Contract', 'ABC']
  },
  {
    key: 'uppercase',
    name: '大写英文',
    category: '文本匹配',
    pattern: '[A-Z]+',
    description: '匹配大写英文字符',
    examples: ['ABC', 'CONTRACT', 'NAME']
  },
  {
    key: 'lowercase',
    name: '小写英文',
    category: '文本匹配',
    pattern: '[a-z]+',
    description: '匹配小写英文字符',
    examples: ['abc', 'contract', 'name']
  },
  {
    key: 'alphanumeric',
    name: '字母数字',
    category: '文本匹配',
    pattern: '[a-zA-Z0-9]+',
    description: '匹配字母和数字的组合',
    examples: ['abc123', 'Contract2024', 'ID12345']
  },
  {
    key: 'noPunctuation',
    name: '非标点符号',
    category: '文本匹配',
    pattern: '[\\u4e00-\\u9fa5a-zA-Z0-9]+',
    description: '匹配中文、字母和数字（排除标点和空格）',
    examples: ['合同内容', 'ContractName', '甲方名称123']
  },

  // 特殊格式（6种）
  {
    key: 'email',
    name: '电子邮箱',
    category: '特殊格式',
    pattern: '[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}',
    description: '匹配电子邮箱地址',
    examples: ['user@example.com', 'test.user@domain.co.cn']
  },
  {
    key: 'phone',
    name: '手机号码',
    category: '特殊格式',
    pattern: '1[3-9]\\d{9}',
    description: '匹配中国大陆手机号码',
    examples: ['13812345678', '18900001111', '19912345678']
  },
  {
    key: 'idCard',
    name: '身份证号',
    category: '特殊格式',
    pattern: '[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]',
    description: '匹配18位身份证号码',
    examples: ['110101199001011234', '44030119900101001X']
  },
  {
    key: 'taxId',
    name: '纳税人识别号（通用）',
    category: '特殊格式',
    pattern: '[0-9A-Z]{15,20}',
    description: '匹配各种格式的纳税人识别号（统一社会信用代码、旧版税号等），支持15-20位数字和字母组合',
    examples: ['91510100MA61WHB260', '91140105MAOLLW8Q1D', '123456789012345', '12345678901234567A']
  },
  {
    key: 'url',
    name: 'URL地址',
    category: '特殊格式',
    pattern: 'https?://[\\w\\-]+(\\.[\\w\\-]+)+([\\w\\-.,@?^=%&:/~+#]*[\\w\\-@?^=%&/~+#])?',
    description: '匹配HTTP/HTTPS URL',
    examples: ['http://example.com', 'https://www.example.com/path']
  },
  {
    key: 'ipAddress',
    name: 'IP地址',
    category: '特殊格式',
    pattern: '\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b',
    description: '匹配IPv4地址',
    examples: ['192.168.1.1', '10.0.0.1', '127.0.0.1']
  },

  // 其他（2种）
  {
    key: 'anyText',
    name: '任意文本',
    category: '其他',
    pattern: '.*',
    description: '匹配任意文本内容',
    examples: ['任何内容', 'Any text', '12345']
  },
  {
    key: 'nonEmpty',
    name: '非空文本',
    category: '其他',
    pattern: '.+',
    description: '匹配至少一个字符的文本',
    examples: ['a', '文本', '123']
  }
]

const filteredAllPatterns = computed(() => {
  if (!searchKeyword.value) {
    return regexPatterns
  }
  const keyword = searchKeyword.value.toLowerCase()
  return regexPatterns.filter(item => 
    item.name.toLowerCase().includes(keyword) ||
    item.description.toLowerCase().includes(keyword) ||
    item.pattern.toLowerCase().includes(keyword)
  )
})

const getPatternsByCategory = (category: string) => {
  return regexPatterns.filter(item => item.category === category)
}

const getCategoryColor = (category: string): string => {
  const colorMap: Record<string, string> = {
    '数字匹配': 'primary',
    '金额匹配': 'success',
    '日期匹配': 'warning',
    '文本匹配': 'info',
    '特殊格式': 'danger',
    '其他': ''
  }
  return colorMap[category] || ''
}

const selectPattern = (item: RegexPattern) => {
  emit('select', item.pattern)
  dialogVisible.value = false
}
</script>

<style scoped lang="scss">
.regex-picker {
  .regex-list {
    max-height: 500px;
    overflow-y: auto;
  }

  .regex-item {
    padding: 12px;
    margin-bottom: 8px;
    border: 1px solid #e4e7ed;
    border-radius: 4px;
    cursor: pointer;
    transition: all 0.3s;

    &:hover {
      border-color: #409eff;
      background-color: #ecf5ff;
    }

    .regex-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;

      .regex-name {
        font-weight: 600;
        font-size: 14px;
        color: #303133;
      }
    }

    .regex-pattern {
      padding: 8px;
      background-color: #f5f7fa;
      border-radius: 4px;
      font-family: 'Consolas', 'Monaco', monospace;
      font-size: 13px;
      color: #606266;
      margin-bottom: 8px;
      word-break: break-all;
    }

    .regex-description {
      font-size: 12px;
      color: #909399;
      margin-bottom: 8px;
    }

    .regex-examples {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 4px;

      .examples-label {
        font-size: 12px;
        color: #909399;
        margin-right: 4px;
      }
    }
  }
}
</style>

