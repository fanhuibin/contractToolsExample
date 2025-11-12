<template>
  <div class="scenario-config">
    <el-card shadow="never" class="config-card">
      <template #header>
        <div class="scenario-header">
          <el-icon><Promotion /></el-icon>
          <span>明细表格设置</span>
        </div>
      </template>

      <el-form label-width="130px" label-position="left">
        <el-divider content-position="left">数据来源</el-divider>
        <el-form-item label="使用自定义数据">
          <el-switch v-model="localState.useCustomRows" />
          <span class="hint">开启后可直接编辑表格行</span>
        </el-form-item>
        <el-form-item v-if="!localState.useCustomRows" label="模板数据">
          <el-radio-group v-model="localState.datasetKey">
            <el-radio-button label="standard">标准采购清单</el-radio-button>
            <el-radio-button label="expansion">二期扩展清单</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-divider content-position="left">表头样式</el-divider>
        <el-form-item label="表头背景">
          <el-space>
            <el-color-picker v-model="localState.headerBg" />
            <el-input v-model="localState.headerBg" style="width: 160px" />
          </el-space>
        </el-form-item>
        <el-form-item label="表头字体颜色">
          <el-space>
            <el-color-picker v-model="localState.headerColor" />
            <el-input v-model="localState.headerColor" style="width: 160px" />
          </el-space>
        </el-form-item>
        <el-form-item label="对齐方式">
          <el-radio-group v-model="localState.alignHeader">
            <el-radio-button label="left">居左</el-radio-button>
            <el-radio-button label="center">居中</el-radio-button>
            <el-radio-button label="right">居右</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="条纹行">
          <el-checkbox v-model="localState.stripe">启用条纹背景</el-checkbox>
          <template v-if="localState.stripe">
            <span class="hint">条纹背景</span>
            <el-color-picker v-model="localState.stripeColor" />
            <el-input v-model="localState.stripeColor" style="width: 140px" />
          </template>
        </el-form-item>

        <el-divider content-position="left">表体样式</el-divider>
        <el-form-item label="文字颜色">
          <el-space>
            <el-color-picker v-model="localState.bodyColor" />
            <el-input v-model="localState.bodyColor" style="width: 160px" />
          </el-space>
        </el-form-item>
        <el-form-item label="文字字号">
          <el-input-number v-model="localState.bodyFontSize" :min="10" :max="20" />
          <span class="hint">单位：px，自动转换为pt</span>
        </el-form-item>
        <el-form-item label="单元格内边距">
          <el-input v-model="localState.cellPadding" style="width: 180px;" placeholder="例如 6px 8px" />
        </el-form-item>
        <el-form-item label="金额列样式">
          <el-space>
            <el-color-picker v-model="localState.priceColor" />
            <el-input v-model="localState.priceColor" style="width: 140px" />
            <el-checkbox v-model="localState.priceBold">金额加粗</el-checkbox>
          </el-space>
        </el-form-item>

        <el-divider content-position="left">总计与摘要</el-divider>
        <el-form-item label="总计行背景">
          <el-space>
            <el-color-picker v-model="localState.totalBg" />
            <el-input v-model="localState.totalBg" style="width: 140px" />
          </el-space>
        </el-form-item>
        <el-form-item label="总计文字颜色">
          <el-space>
            <el-color-picker v-model="localState.totalColor" />
            <el-input v-model="localState.totalColor" style="width: 140px" />
          </el-space>
          <el-checkbox v-model="localState.totalBold" style="margin-left: 16px;">总计加粗</el-checkbox>
        </el-form-item>

        <el-divider content-position="left">明细数据</el-divider>
        <div v-if="localState.useCustomRows" class="custom-toolbar">
          <el-button type="primary" size="small" @click="addRow">新增一行</el-button>
        </div>
        <el-table :data="displayRows" border size="small" style="width: 100%;">
          <el-table-column type="index" label="序号" width="60" />
          <el-table-column label="货物名称">
            <template #default="scope">
              <el-input
                v-if="localState.useCustomRows"
                v-model="localState.customRows[scope.$index].name"
                placeholder="请输入货物名称"
              />
              <span v-else>{{ scope.row.name }}</span>
            </template>
          </el-table-column>
          <el-table-column label="规格型号">
            <template #default="scope">
              <el-input
                v-if="localState.useCustomRows"
                v-model="localState.customRows[scope.$index].spec"
                placeholder="请输入规格"
              />
              <span v-else>{{ scope.row.spec }}</span>
            </template>
          </el-table-column>
          <el-table-column label="产地" width="140">
            <template #default="scope">
              <el-input
                v-if="localState.useCustomRows"
                v-model="localState.customRows[scope.$index].origin"
                placeholder="产地"
              />
              <span v-else>{{ scope.row.origin }}</span>
            </template>
          </el-table-column>
          <el-table-column label="数量" width="100" align="right">
            <template #default="scope">
              <el-input-number
                v-if="localState.useCustomRows"
                v-model="localState.customRows[scope.$index].quantity"
                :min="0"
                :step="1"
                controls-position="right"
              />
              <span v-else>{{ scope.row.quantity }}</span>
            </template>
          </el-table-column>
          <el-table-column label="单价" width="140" align="right">
            <template #default="scope">
              <el-input-number
                v-if="localState.useCustomRows"
                v-model="localState.customRows[scope.$index].price"
                :min="0"
                :step="100"
                controls-position="right"
              />
              <span v-else>{{ Number(scope.row.price).toLocaleString() }}</span>
            </template>
          </el-table-column>
          <el-table-column label="合计" width="160" align="right">
            <template #default="scope">
              <span>{{ Number(scope.row.total).toLocaleString() }} 元</span>
            </template>
          </el-table-column>
          <el-table-column v-if="localState.useCustomRows" label="操作" width="90" align="center">
            <template #default="scope">
              <el-button type="text" size="small" @click="removeRow(scope.$index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-divider content-position="left">汇总说明</el-divider>
        <el-form-item label="统计标签">
          <el-input v-model="localState.totalLabel" style="width: 220px;" />
          <el-checkbox v-model="localState.emphasizeTotal" style="margin-left: 16px;">突出显示</el-checkbox>
        </el-form-item>
        <el-form-item label="摘要说明">
          <el-switch v-model="localState.includeSummary" />
          <el-input
            v-if="localState.includeSummary"
            v-model="localState.summaryText"
            type="textarea"
            :rows="3"
            maxlength="220"
            show-word-limit
            class="summary-input"
          />
          <template v-if="localState.includeSummary">
            <el-space style="margin-top: 12px;">
              <div class="summary-style-setting">
                <span>背景：</span>
                <el-color-picker v-model="localState.summaryBg" />
                <el-input v-model="localState.summaryBg" style="width: 120px" />
              </div>
              <div class="summary-style-setting">
                <span>文字：</span>
                <el-color-picker v-model="localState.summaryTextColor" />
                <el-input v-model="localState.summaryTextColor" style="width: 120px" />
              </div>
            </el-space>
          </template>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import { Promotion } from '@element-plus/icons-vue'

const datasets: Record<string, Array<Record<string, number | string>>> = {
  standard: [
    { name: '工业控制服务器', spec: '2U机架式 / 双路', origin: '上海', quantity: 3, price: 120000, total: 360000 },
    { name: '运维管理终端', spec: 'i7 / 32G / 1TB SSD', origin: '深圳', quantity: 10, price: 12000, total: 120000 },
    { name: '安全防护网关', spec: '千兆 / 双冗余', origin: '杭州', quantity: 2, price: 26000, total: 52000 }
  ],
  expansion: [
    { name: '中控一体机', spec: '55英寸 / 4K', origin: '北京', quantity: 4, price: 8900, total: 35600 },
    { name: '应急备份服务器', spec: '4U / RAID10', origin: '南京', quantity: 2, price: 98000, total: 196000 },
    { name: '工程现场终端', spec: '防尘防水 IP65', origin: '重庆', quantity: 12, price: 5600, total: 67200 },
    { name: '运维工具包', spec: '含线缆 / 工具箱', origin: '广州', quantity: 6, price: 1800, total: 10800 }
  ]
}

type TableRow = {
  name: string
  spec: string
  origin: string
  quantity: number
  price: number
  total: number
}

const createEmptyRow = (): TableRow => ({
  name: '',
  spec: '',
  origin: '',
  quantity: 1,
  price: 0,
  total: 0
})

const DEFAULT_STATE = {
  datasetKey: 'standard',
  useCustomRows: false,
  customRows: [createEmptyRow(), createEmptyRow()],
  includeSummary: true,
  alignHeader: 'center',
  emphasizeTotal: true,
  headerBg: '#f5f7fa',
  headerColor: '#303133',
  stripe: true,
  stripeColor: 'rgba(64,158,255,0.04)',
  totalLabel: '合计金额',
  summaryText: '合同签订后支付预付款30%，设备验收合格后支付60%，余款10%于验收后30日内付清。',
  bodyColor: '#303133',
  bodyFontSize: 13,
  cellPadding: '6px 8px',
  priceColor: '#409eff',
  priceBold: true,
  totalBg: '#edf4ff',
  totalColor: '#1f64ff',
  totalBold: true,
  summaryBg: '#f5f7fa',
  summaryTextColor: '#606266'
}

const props = defineProps<{ state: Record<string, any> }>()
const emit = defineEmits<{ (e: 'update', value: Record<string, any>): void }>()

const localState = reactive({
  ...DEFAULT_STATE,
  ...props.state,
  customRows: Array.isArray(props.state.customRows) && props.state.customRows.length
    ? props.state.customRows.map((row: TableRow) => ({ ...createEmptyRow(), ...row }))
    : DEFAULT_STATE.customRows.map((row) => ({ ...row }))
})

const emitChange = () => {
  emit('update', {
    datasetKey: localState.datasetKey,
    useCustomRows: localState.useCustomRows,
    customRows: localState.customRows,
    includeSummary: localState.includeSummary,
    alignHeader: localState.alignHeader,
    emphasizeTotal: localState.emphasizeTotal,
    headerBg: localState.headerBg,
    headerColor: localState.headerColor,
    stripe: localState.stripe,
    stripeColor: localState.stripeColor,
    totalLabel: localState.totalLabel,
    summaryText: localState.summaryText,
    bodyColor: localState.bodyColor,
    bodyFontSize: localState.bodyFontSize,
    cellPadding: localState.cellPadding,
    priceColor: localState.priceColor,
    priceBold: localState.priceBold,
    totalBg: localState.totalBg,
    totalColor: localState.totalColor,
    totalBold: localState.totalBold,
    summaryBg: localState.summaryBg,
    summaryTextColor: localState.summaryTextColor
  })
}

const recalcTotals = () => {
  localState.customRows.forEach((row: TableRow) => {
    const quantity = Number(row.quantity) || 0
    const price = Number(row.price) || 0
    row.total = quantity * price
  })
}

watch(
  () => props.state,
  (value) => {
    if (!value) return
    Object.assign(localState, {
      ...DEFAULT_STATE,
      ...value
    })
    if (Array.isArray(value.customRows) && value.customRows.length) {
      localState.customRows.splice(0, localState.customRows.length, ...value.customRows)
    } else if (!localState.customRows.length) {
      localState.customRows.push(createEmptyRow())
    }
    recalcTotals()
  },
  { deep: true }
)

watch(
  localState,
  () => {
    recalcTotals()
    emitChange()
  },
  { deep: true }
)

const displayRows = computed(() => {
  if (localState.useCustomRows) {
    return localState.customRows
  }
  return datasets[localState.datasetKey] || []
})

const addRow = () => {
  localState.customRows.push(createEmptyRow())
}

const removeRow = (index: number) => {
  if (localState.customRows.length === 1) {
    localState.customRows.splice(0, 1, createEmptyRow())
    return
  }
  localState.customRows.splice(index, 1)
}
</script>

<style scoped>
.scenario-config {
  width: 100%;
}

.config-card {
  width: 100%;
}

.scenario-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #303133;
}

.hint {
  margin-left: 8px;
  color: #909399;
}

.custom-toolbar {
  margin-bottom: 12px;
}

.summary-input {
  margin-left: 16px;
  width: 100%;
}

.summary-style-setting {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
