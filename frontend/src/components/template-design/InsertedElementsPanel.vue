<template>
  <div class="custom-card fill elements-card">
    <div class="card-header">
      <div class="elements-header">
        <div class="header-top">
          <span>已插入元素</span>
        </div>
        <div class="header-bottom">
          <el-input v-model="keyword" placeholder="检索已插入元素" size="small" clearable prefix-icon="Search" style="width:100%" />
        </div>
      </div>
    </div>
    <div class="card-body">
      <div class="custom-scroll list">
        <el-empty v-if="elements.length === 0" description="暂无元素" />
        <div v-for="(el, idx) in filteredElements" :key="el.key" class="element-item">
          <div class="element-main" @click="$emit('locate', el)">
            <div class="line display-line">
              <el-tooltip class="display-name-wrap" :content="el.customName || el.name" placement="top">
                <span class="display-name">{{ el.customName || el.name }}</span>
              </el-tooltip>
              <el-tag v-if="el.type === 'party'" size="small" type="warning">签约方{{ el.meta?.partyIndex || '1' }}</el-tag>
              <el-tag v-else size="small" :type="typeTag(el.type)">{{ typeLabel(el.type) }}</el-tag>
            </div>
            <div class="line meta-line">
              <div class="meta-left">
                <span class="meta-name">{{ el.name }}</span>
                <span v-if="el?.meta?.code" class="meta-code">（{{ el.meta.code }}）</span>
              </div>
            </div>
            <div v-if="el.type === 'clause'" class="line clause-line">
              <div class="elem-clause-preview" v-html="resolveClauseHtml(el.meta?.content || '')"></div>
            </div>
          </div>
          <div class="actions">
            <el-button size="small" text type="primary" @click.stop="$emit('edit', idx)">编辑</el-button>
            <el-popconfirm title="确认删除该元素？" confirm-button-text="删除" cancel-button-text="取消" @confirm="$emit('delete', idx, el)">
              <template #reference>
                <el-button size="small" text type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'

const props = defineProps<{ elements: any[]; fields: any; }>()
defineEmits<{
  (e: 'locate', el: any): void
  (e: 'edit', idx: number): void
  (e: 'delete', idx: number, el: any): void
}>()

const keyword = ref('')
const filteredElements = computed(() => (props.elements || []).filter((e: any) => {
  if (!keyword.value) return true
  const kw = keyword.value.toLowerCase()
  return (
    (e.customName || '').toLowerCase().includes(kw) ||
    (e.name || '').toLowerCase().includes(kw) ||
    (e.type || '').toLowerCase().includes(kw) ||
    (e.tag || '').toLowerCase().includes(kw)
  )
}))

const typeLabel = (type: string) => {
  switch (type) {
    case 'base': return '字段'
    case 'clause': return '条款'
    case 'party': return '签约方'
    case 'seal': return '印章'
    default: return type
  }
}
const typeTag = (type: string) => {
  switch (type) {
    case 'base': return 'success'
    case 'clause': return 'info'
    case 'party': return 'warning'
    case 'seal': return 'danger'
    default: return 'info'
  }
}

const resolveClauseHtml = (raw: string) => {
  const esc = (s: string) => s.replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' } as any)[c])
  if (!raw) return ''
  const dict: Record<string, string> = {}
  ;(props.fields.baseFields || []).forEach((f: any) => { dict[f.code] = f.name })
  ;(props.fields.counterpartyFields || []).forEach((f: any) => { dict[f.code] = f.name })
  const safe = esc(raw)
  return safe.replace(/\$\{([^}]+)\}/g, (_: any, rawKey: string) => {
    let label = ''
    let partyIndex = ''
    if (rawKey.includes('#')) {
      const [a, b] = rawKey.split('#')
      if (/^\d+$/.test(a)) { partyIndex = a; label = dict[b] || b }
      else if (/^\d+$/.test(b)) { partyIndex = b; label = dict[a] || a }
      else { label = dict[rawKey] || rawKey }
    } else {
      label = dict[rawKey] || rawKey
      if (rawKey.startsWith('subject_')) partyIndex = '1'
    }
    const partyTag = partyIndex ? `<span class=\"var-tag\">签约方${partyIndex}</span>` : ''
    return `${partyTag}<span class=\"var-tag\">${label}</span>`
  })
}
</script>

<style scoped>
.elements-card { height: 100%; }
.custom-card.fill { display: flex; flex-direction: column; height: 100%; }
.card-header { display: flex; align-items: center; justify-content: space-between; padding: 10px; border-bottom: 1px solid #ebeef5; }
.card-body { flex: 1; padding: 10px; min-height: 0; display:flex; flex-direction:column; overflow:hidden; }
/* 让滚动容器充满可用空间，恢复滚动条 */
.list { flex:1; min-height:0; overflow:auto; }

.element-item { display:flex; align-items:center; justify-content:space-between; padding:12px 10px; border:1px solid #ebeef5; border-radius:8px; margin-bottom:10px; background:#fff; transition: box-shadow .2s ease, border-color .2s ease; }
.element-item:hover { box-shadow: 0 2px 10px rgba(0,0,0,.06); border-color:#e4e7ed; }
.element-main { flex:1; cursor:pointer; width:100%; }
.line { display:flex; align-items:center; width:100%; }
.display-line { margin-bottom:6px; gap:6px; }
.display-name { font-weight:700; color:#303133; display:inline-block; max-width:100%; overflow:hidden; white-space:nowrap; text-overflow:ellipsis; font-size:15px; line-height:20px; }
.meta-line { justify-content:flex-start; margin-bottom:4px; font-size:12px; color:#606266; }
.meta-left { min-width:0; overflow:hidden; white-space:nowrap; text-overflow:ellipsis; color:#606266; font-size:12px; }
.meta-code { color:#909399; margin-left:4px; }
.clause-line { margin-top:6px; }
.elem-clause-preview { color:#606266; font-size:12px; line-height:18px; display:-webkit-box; -webkit-line-clamp:2; line-clamp:2; -webkit-box-orient:vertical; overflow:hidden; }
.display-line :deep(.el-tag) { margin-left:6px; }
.element-item .actions { display:flex; gap:2px; padding:2px 0 0; justify-content:flex-end; width:100%; }
.element-item .actions :deep(.el-button) { font-size:12px; height:20px; padding:0 4px; min-width:0; }

:deep(.var-tag){ display:inline-block; padding:0 6px; height:18px; line-height:18px; background:#e8f3ff; border:1px solid #bfe1ff; color:#1677ff; border-radius:3px; font-size:12px; margin:0 2px; }
</style>



