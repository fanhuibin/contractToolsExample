<template>
  <div class="scenario-config">
    <el-card shadow="never" class="config-card">
      <template #header>
        <div class="scenario-header">
          <el-icon><Promotion /></el-icon>
          <span>基础文本字段</span>
        </div>
      </template>

      <el-form label-width="140px" label-position="left">
        <el-divider content-position="left">合同基础信息</el-divider>
        <el-form-item label="采购项目名称">
          <el-input v-model="localState.projectName" />
        </el-form-item>
        <el-form-item label="招标编号">
          <el-input v-model="localState.bidNumber" />
        </el-form-item>
        <el-form-item label="合同标号">
          <el-input v-model="localState.contractCode" />
        </el-form-item>
        <el-form-item label="签订地点">
          <el-input v-model="localState.signLocation" />
        </el-form-item>
        <el-form-item label="验收付款金额">
          <el-input v-model="localState.acceptanceAmount" />
        </el-form-item>

        <el-divider content-position="left">相对方信息</el-divider>
        <el-form-item label="买方公司名称">
          <el-input v-model="localState.buyerName" />
        </el-form-item>
        <el-form-item label="卖方公司名称">
          <el-input v-model="localState.sellerName" />
        </el-form-item>
        <el-divider content-position="left">统一样式设置</el-divider>
        <el-form-item label="字体样式">
          <el-space wrap>
            <el-checkbox v-model="localState.styleBold">加粗</el-checkbox>
            <el-checkbox v-model="localState.styleItalic">斜体</el-checkbox>
            <el-checkbox v-model="localState.styleUnderline">下划线</el-checkbox>
            <el-checkbox v-model="localState.styleStrike">删除线</el-checkbox>
          </el-space>
        </el-form-item>
        <el-form-item label="字体颜色">
          <el-color-picker v-model="localState.styleColor" :predefine="COLOR_PRESETS" />
        </el-form-item>
        <el-form-item label="高亮颜色">
          <el-color-picker
            v-model="localState.styleHighlight"
            :predefine="HIGHLIGHT_PRESETS"
            show-alpha
            :teleported="false"
          />
        </el-form-item>
        <el-form-item label="字体大小">
          <el-select v-model="localState.styleFontSize" style="width: 160px">
            <el-option v-for="size in FONT_SIZE_OPTIONS" :key="size" :label="`${size}pt`" :value="size" />
          </el-select>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, watch } from 'vue'
import { Promotion } from '@element-plus/icons-vue'

const DEFAULT_STATE = {
  projectName: '绝热材料采购项目',
  bidNumber: 'ZB-2025-001',
  contractCode: 'HT-2025-001',
  signLocation: '北京市',
  acceptanceAmount: '300000.00',
  buyerName: '中铁隧道集团沈阳地铁二号线有限公司第十二合同段项目经理部',
  sellerName: '盘锦瑞普杜欣设备再制造技术开发合作联社',
  styleBold: true,
  styleItalic: false,
  styleUnderline: false,
  styleStrike: false,
  styleColor: '#303133',
  styleHighlight: '',
  styleFontSize: 16
}

const FONT_SIZE_OPTIONS = [12, 14, 16, 18, 20, 24]
const COLOR_PRESETS = ['#303133', '#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399']
const HIGHLIGHT_PRESETS = ['#FFFF00', '#FFE58F', '#FFD666', '#FFF1B8', '#D9EDF7', '#FDE2E2', '']

const props = defineProps<{ state: Record<string, any> }>()
const emit = defineEmits<{ (e: 'update', value: Record<string, any>): void }>()

const localState = reactive({
  ...DEFAULT_STATE,
  ...props.state
})

watch(
  () => props.state,
  (value) => {
    if (!value) return
    Object.assign(localState, {
      ...DEFAULT_STATE,
      ...value
    })
  },
  { deep: true }
)

watch(
  localState,
  () => {
    emit('update', { ...localState })
  },
  { deep: true }
)
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
</style>
