<template>
  <div class="custom-card fill fields-card">
    <div class="card-header">
      <span>可插入元素</span>
      <el-tag size="small">{{ elementsCount }}项</el-tag>
    </div>
    <div class="card-body">
      <div class="fields-tabs custom-tabs">
        <div class="tabs-nav is-left">
          <button class="tab-item" :class="{ active: activeTab === 'base' }" @click="activeTab = 'base'" :aria-pressed="activeTab==='base'">
            <span class="tab-label"><el-icon><Document /></el-icon> 字段</span>
          </button>
          <button class="tab-item" :class="{ active: activeTab === 'clause' }" @click="activeTab = 'clause'" :aria-pressed="activeTab==='clause'">
            <span class="tab-label"><el-icon><Tickets /></el-icon> 条款</span>
          </button>
          <button class="tab-item" :class="{ active: activeTab === 'party' }" @click="activeTab = 'party'" :aria-pressed="activeTab==='party'">
            <span class="tab-label"><el-icon><User /></el-icon> 签约方</span>
          </button>
          <button class="tab-item" :class="{ active: activeTab === 'seal' }" @click="activeTab = 'seal'" :aria-pressed="activeTab==='seal'">
            <span class="tab-label"><el-icon><Stamp /></el-icon> 印章</span>
          </button>
        </div>
        <div class="tabs-content">
          <div v-show="activeTab === 'base'" class="tab-pane-body">
            <div class="custom-search"><input v-model="keywordBase" placeholder="搜索字段" class="search-input" /></div>
            <div class="custom-scroll list">
              <div v-if="filteredBaseFields.length === 0" class="empty">暂无数据</div>
              <div v-for="f in filteredBaseFields" :key="f.code" class="field-item custom">
                <div class="meta">
                  <div class="item-head">
                    <div class="name">
                      {{ f.name }}
                      <span v-if="f.isRichText" class="badge-rich">富文本</span>
                    </div>
                    <button class="link-btn inline" @click="$emit('insert','base', f)">插入</button>
                  </div>
                  <div class="code"><span class="badge">{{ f.code }}</span></div>
                </div>
              </div>
            </div>
          </div>

          <div v-show="activeTab === 'clause'" class="tab-pane-body">
            <div class="custom-search"><input v-model="keywordClause" placeholder="搜索条款" class="search-input" /></div>
            <div class="custom-scroll list">
              <div v-if="filteredClauseFields.length === 0" class="empty">暂无数据</div>
              <div v-for="c in filteredClauseFields" :key="c.code" class="field-item custom clause-item">
                <div class="meta">
                  <div class="item-head">
                    <div class="name">{{ c.name }}</div>
                    <button class="link-btn inline" @click="$emit('insert','clause', c)">插入</button>
                  </div>
                  <div class="code"><span class="badge">{{ c.code }}</span></div>
                  <div class="clause-preview" v-html="resolveClauseHtml(c.content)"></div>
                  <div class="full-preview" v-html="resolveClauseHtml(c.content)"></div>
                </div>
              </div>
            </div>
          </div>

          <div v-show="activeTab === 'party'" class="tab-pane-body">
            <div class="custom-search"><input v-model="keywordParty" placeholder="搜索签约方" class="search-input" /></div>
            <div class="custom-scroll list">
              <div v-if="filteredPartyFields.length === 0" class="empty">暂无数据</div>
              <div v-for="p in filteredPartyFields" :key="p.code" class="field-item custom">
                <div class="meta">
                  <div class="item-head party">
                    <div class="name">{{ p.name }}</div>
                    <button class="link-btn inline" @click="$emit('insert','party', p)">插入</button>
                  </div>
                  <div class="code"><span class="badge">{{ p.code }}</span></div>
                </div>
              </div>
            </div>
          </div>

          <div v-show="activeTab === 'seal'" class="tab-pane-body">
            <div class="custom-search"><input v-model="keywordSeal" placeholder="搜索印章" class="search-input" /></div>
            <div class="custom-scroll list">
              <div v-if="filteredSealFields.length === 0" class="empty">暂无数据</div>
              <div v-for="s in filteredSealFields" :key="s.code" class="field-item custom">
                <div class="meta">
                  <div class="item-head">
                    <div class="name">{{ s.name }}</div>
                    <button class="link-btn inline" @click="$emit('insert','seal', s)">插入</button>
                  </div>
                  <div class="code"><span class="badge">{{ s.code }}</span></div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { Document, Tickets, User, Stamp } from '@element-plus/icons-vue'

const props = defineProps<{ fields: any }>()
defineEmits<{ (e: 'insert', type: string, item: any): void }>()

const activeTab = ref<'base' | 'clause' | 'party' | 'seal'>('base')
const keywordBase = ref('')
const keywordClause = ref('')
const keywordParty = ref('')
const keywordSeal = ref('')

const elementsCount = computed(() => (props.fields.baseFields?.length || 0) + (props.fields.clauseFields?.length || 0) + (props.fields.sealFields?.length || 0) + (props.fields.counterpartyFields?.length || 0))
const filteredBaseFields = computed(() => (props.fields.baseFields || []).filter((f: any) => !keywordBase.value || f.name.includes(keywordBase.value) || f.code.includes(keywordBase.value)))
const filteredClauseFields = computed(() => (props.fields.clauseFields || []).filter((c: any) => !keywordClause.value || c.name.includes(keywordClause.value) || c.code.includes(keywordClause.value)))
const filteredPartyFields = computed(() => (props.fields.counterpartyFields || []).filter((p: any) => !keywordParty.value || p.name.includes(keywordParty.value) || p.code.includes(keywordParty.value)))
const filteredSealFields = computed(() => (props.fields.sealFields || []).filter((s: any) => !keywordSeal.value || s.name.includes(keywordSeal.value) || s.code.includes(keywordSeal.value)))

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
.custom-card.fill { display: flex; flex-direction: column; height: 100%; }
.card-header { display: flex; align-items: center; justify-content: space-between; padding: 10px; border-bottom: 1px solid #ebeef5; }
.card-body { flex: 1; padding: 10px; min-height: 0; }

.custom-tabs { display: grid; grid-template-columns: 52px 1fr; height: 100%; }
.custom-tabs .tabs-nav { width: 52px; border-right: 1px solid #ebeef5; padding: 4px; }
.custom-tabs .tab-item { width: 100%; height: 48px; padding: 4px; margin: 4px 0; display:flex; align-items:center; justify-content:center; gap:6px; border-radius:8px; border:1px solid transparent; background:transparent; color:#606266; cursor:pointer; }
.custom-tabs .tab-item:hover { background:#f7f8fa; }
.custom-tabs .tab-item.active { background:#f0f5ff; color:#3370ff; box-shadow: inset 2px 0 0 #3370ff; border-color:#c6dbff; }
.tab-label { display:inline-flex; flex-direction:column; align-items:center; gap:4px; font-size:11px; color:#606266; }
.tab-label :deep(svg){ font-size:14px; }
.tabs-content { padding-left:0; height:100%; display:flex; flex-direction:column; overflow:hidden; }
.tab-pane-body { padding:8px 10px; flex:1; display:flex; flex-direction:column; overflow:hidden; }
.list { flex:1; overflow:auto; }
.custom-search { padding:6px 0; }
.search-input { width:100%; height:28px; border:1px solid #e4e7ed; border-radius:6px; padding:0 10px; outline:none; }
.search-input:focus { border-color:#3370ff; box-shadow:0 0 0 2px rgba(51,112,255,.1); }
.empty { color:#c0c4cc; text-align:center; padding:12px 0; }
.field-item { padding:8px 6px; border-radius:6px; }
.field-item .meta { display:flex; flex-direction:column; gap:4px; }
.item-head { display:grid; grid-template-columns:1fr auto; align-items:center; gap:6px; }
.item-head .name { font-weight:600; color:#303133; overflow:hidden; white-space:nowrap; text-overflow:ellipsis; }
.link-btn.inline { padding:2px 6px; height:24px; border-radius:4px; border:1px solid #d9e6ff; font-size:12px; line-height:1; }
.field-item .code { font-size:12px; color:#909399; }
.badge { display:inline-block; padding:0 6px; background:#f2f3f5; border-radius:4px; font-size:12px; color:#606266; }
.badge-rich { display:inline-block; padding:0 6px; background:#fff1f0; color:#f5222d; border:1px solid #ffccc7; border-radius:4px; font-size:12px; }
.clause-item .clause-preview { margin-top:4px; color:#606266; font-size:12px; line-height:18px; display:-webkit-box; -webkit-line-clamp:3; line-clamp:3; -webkit-box-orient:vertical; overflow:hidden; }
:deep(.var-tag){ display:inline-block; padding:0 6px; height:18px; line-height:18px; background:#e8f3ff; border:1px solid #bfe1ff; color:#1677ff; border-radius:3px; font-size:12px; margin:0 2px; }
.clause-item .full-preview { display:none; margin-top:4px; color:#606266; font-size:12px; line-height:18px; white-space:pre-wrap; }
.clause-item:hover .clause-preview { display:none; }
.clause-item:hover .full-preview { display:block; }
</style>



