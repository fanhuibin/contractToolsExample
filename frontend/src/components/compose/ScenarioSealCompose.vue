<template>
  <div class="scenario-config">
    <el-card shadow="never" class="config-card">
      <template #header>
        <div class="scenario-header">
          <el-icon><Promotion /></el-icon>
          <span>盖章与附件设置</span>
        </div>
      </template>

      <div class="demo-resources">
        <el-divider content-position="left">演示资料下载</el-divider>
        <el-space wrap>
          <el-link
            v-for="item in demoDownloadItems"
            :key="item.key"
            :href="item.url"
            target="_blank"
            :download="item.downloadName"
            type="primary"
          >
            {{ item.label }}
          </el-link>
        </el-space>
        <p class="resource-tip">以上文件来自演示后台，可直接下载查看效果。</p>
      </div>

      <el-form label-width="140px" label-position="left" class="seal-form">
        <el-form-item label="公章控制">
          <el-checkbox v-model="localState.includeBuyerSeal">买方公章</el-checkbox>
          <el-checkbox v-model="localState.includeSellerSeal">卖方公章</el-checkbox>
        </el-form-item>

        <el-form-item label="骑缝章">
          <el-switch
            v-model="localState.includeRidingStamp"
            active-text="合并附件后盖骑缝章"
            inactive-text="不盖骑缝章"
          />
        </el-form-item>

        <el-divider content-position="left">印章尺寸设置</el-divider>
        <el-form-item label="公章尺寸(px)" class="seal-size-item">
          <div class="seal-size-control">
            <el-slider v-model="localState.sealSize" :min="50" :max="200" :step="10" style="flex: 1; margin-right: 12px;" />
            <el-input-number 
              v-model="localState.sealSize" 
              :min="50" 
              :max="200" 
              :step="10" 
              size="small"
              controls-position="right"
              style="width: 100px;"
            />
          </div>
          <div class="size-tip">建议范围 80-150px</div>
        </el-form-item>
        <el-form-item label="骑缝章尺寸(px)" class="seal-size-item">
          <div class="seal-size-control">
            <el-slider v-model="localState.ridingSealSize" :min="50" :max="200" :step="10" style="flex: 1; margin-right: 12px;" />
            <el-input-number 
              v-model="localState.ridingSealSize" 
              :min="50" 
              :max="200" 
              :step="10" 
              size="small"
              controls-position="right"
              style="width: 100px;"
            />
          </div>
          <div class="size-tip">建议范围 80-150px</div>
        </el-form-item>

        <el-divider content-position="left">条款内容</el-divider>
        <el-form-item label="条款内容" class="clause-item">
          <div class="clause-preview" v-html="clausePreviewHtml" />
        </el-form-item>

        <el-divider content-position="left">条款变量</el-divider>
        <el-form-item label="交付日期">
          <el-date-picker
            v-model="localState.deliveryDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择交付日期"
          />
        </el-form-item>
        <el-form-item label="违约金费率(%)">
          <el-slider v-model="localState.penaltyRate" :min="0.1" :max="2" :step="0.1" show-input />
        </el-form-item>

        <el-divider content-position="left">附件合并</el-divider>
        <el-form-item label="选择附件">
          <el-checkbox-group v-model="localState.selectedAttachments">
            <el-checkbox
              v-for="item in attachmentOptions"
              :key="item.value"
              :label="item.value"
            >
              {{ item.label }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { Promotion } from '@element-plus/icons-vue'
import { attachmentLibrary, futureDateString, stampLibrary } from './shared.js'

const DEFAULT_STATE = {
  includeBuyerSeal: true,
  includeSellerSeal: true,
  includeRidingStamp: true,
  deliveryDate: futureDateString(20),
  penaltyRate: 0.6,
  selectedAttachments: ['techSpec', 'qualification'],
  sealSize: 100,          // 公章尺寸，默认100px
  ridingSealSize: 100     // 骑缝章尺寸，默认100px
}

const props = defineProps<{ state: Record<string, any> }>()
const emit = defineEmits<{ (e: 'update', value: Record<string, any>): void }>()

const ensureAttachments = (values?: unknown) => {
  const availableKeys = Object.keys(attachmentLibrary)
  if (!Array.isArray(values)) {
    return DEFAULT_STATE.selectedAttachments.slice()
  }
  const filtered = values.filter((key) => typeof key === 'string' && availableKeys.includes(key))
  return filtered.length ? filtered : DEFAULT_STATE.selectedAttachments.slice()
}

const createStateFromProps = (value: Record<string, any> = {}) => ({
  ...DEFAULT_STATE,
  ...value,
  includeBuyerSeal: value.includeBuyerSeal ?? DEFAULT_STATE.includeBuyerSeal,
  includeSellerSeal: value.includeSellerSeal ?? DEFAULT_STATE.includeSellerSeal,
  includeRidingStamp: value.includeRidingStamp ?? DEFAULT_STATE.includeRidingStamp,
  deliveryDate: value.deliveryDate || DEFAULT_STATE.deliveryDate,
  penaltyRate: Number.isFinite(Number(value.penaltyRate)) ? Number(value.penaltyRate) : DEFAULT_STATE.penaltyRate,
  selectedAttachments: ensureAttachments(value.selectedAttachments)
})

const localState = reactive(createStateFromProps(props.state))
const syncingFromParent = ref(false)

watch(
  () => props.state,
  (value) => {
    syncingFromParent.value = true
    Object.assign(localState, createStateFromProps(value || {}))
    nextTick(() => {
      syncingFromParent.value = false
    })
  },
  { deep: true }
)

watch(
  localState,
  () => {
    if (syncingFromParent.value) return
    emit('update', { ...localState, selectedAttachments: [...localState.selectedAttachments] })
  },
  { deep: true }
)

const attachmentOptions = computed(() =>
  Object.entries(attachmentLibrary).map(([value, item]) => ({
    value,
    label: item.label
  }))
)

const penaltyRateText = computed(() => {
  const raw = Number(localState.penaltyRate)
  return Number.isFinite(raw) ? raw.toFixed(1) : Number(DEFAULT_STATE.penaltyRate).toFixed(1)
})

const clausePreviewHtml = computed(() => {
  const attachments = localState.selectedAttachments
    .map((key: string) => attachmentLibrary[key]?.label || key)
    .join('、') || '无'
  const seals = [
    localState.includeBuyerSeal ? '买方公章' : null,
    localState.includeSellerSeal ? '卖方公章' : null
  ]
    .filter(Boolean)
    .join('、') || '未选择'

  const deliveryDate = localState.deliveryDate || '（未设置）'
  const ridingStamp = localState.includeRidingStamp ? '是' : '否'

  return `
    <p><strong>交付与违约条款示例</strong></p>
    <p>
      双方确认货物应于 <strong>${deliveryDate}</strong> 前完成交付；
      如遇延迟，将按合同总价的 <strong>${penaltyRateText.value}%</strong> 支付违约金，
      且最迟不得晚于验收付款到账之日。
    </p>
    <p class="clause-extra">
      公章：${seals}；骑缝章：${ridingStamp}；
      附件：${attachments}。
    </p>
  `
})

const demoDownloadItems = computed(() => {
  const items: Array<{ key: string; label: string; url: string; downloadName?: string }> = []
  const attachmentKeys = ['techSpec', 'qualification'] as const
  attachmentKeys.forEach((key) => {
    const info = attachmentLibrary[key]
    if (info?.url) {
      items.push({
        key,
        label: `${info.label}（PDF）`,
        url: info.url,
        downloadName: info.url.split('/').pop()
      })
    }
  })

  const stampUrl = stampLibrary.buyer
  if (stampUrl) {
    items.push({
      key: 'stamp',
      label: '演示印章（PNG）',
      url: stampUrl,
      downloadName: stampUrl.split('/').pop()
    })
  }

  return items
})
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

.demo-resources {
  margin-bottom: 16px;
}

.resource-tip {
  margin: 6px 0 0;
  font-size: 12px;
  color: #909399;
}

.seal-form :deep(.el-divider) {
  margin: 18px 0;
}

.clause-item :deep(.el-form-item__content) {
  display: block;
}

.clause-preview {
  padding: 12px 16px;
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  color: #303133;
  line-height: 1.8;
}

.clause-preview strong {
  color: #1f64ff;
}

.clause-preview .clause-extra {
  margin-top: 12px;
  color: #606266;
}

.seal-size-item :deep(.el-form-item__content) {
  display: block !important;
}

.seal-size-control {
  display: flex;
  align-items: center;
  width: 100%;
  max-width: 100%;
}

.size-tip {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}
</style>
