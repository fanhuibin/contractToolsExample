<template>
  <div class="risk-lib-page">
    <PageHeader 
      title="合同智能审核 · 条款库"
      description="管理审核条款、风险点和审核方案"
      :icon="DocumentChecked"
    >
      <template #actions>
        <el-button size="small" type="primary" @click="goBack">返回</el-button>
        <el-switch v-model="manageMode" active-text="管理模式" size="small" />
      </template>
    </PageHeader>

    <el-row :gutter="16">
      <!-- 左侧：条款树与筛选（加宽至 10/24） -->
      <el-col :span="10" class="left-col">
        <el-card class="side-card">
          <div class="toolbar">
            <el-input v-model="keyword" placeholder="搜索提示/风险点/算法类型/编号" clearable @input="loadTree" />
            <div class="toolbar-row">
              <el-switch v-model="showDisabled" active-text="显示停用" size="small" @change="loadTree" />
              <div class="spacer" />
              <el-button v-if="manageMode" type="primary" size="small" @click="openCreatePointFromClause">新增风险点</el-button>
              <el-button type="primary" size="small" plain @click="onPreviewSelection">生成清单</el-button>
            </div>
          </div>
          <div class="tree-wrap">
            <el-tree
              ref="treeRef"
              :data="treeData"
              node-key="id"
              :props="treeProps"
              show-checkbox
              highlight-current
              default-expand-all
              @node-click="onNodeClick"
              :render-content="renderTreeNode"
            />
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：上部=清单预览/方案管理；下部=显示详情（隐藏，点击显示详情才出现） -->
      <el-col :span="14" class="right-col">
        <el-card class="main-card">
          <el-tabs v-model="mainTab">
            <el-tab-pane label="清单预览" name="preview">
              <div class="pane-toolbar">
                <div class="spacer" />
                <el-input v-model="profileName" placeholder="方案名称" size="small" style="width:220px" />
                <el-button type="primary" size="small" plain @click="onPreviewSelection">重新生成</el-button>
                <el-button type="success" size="small" :disabled="!previewList.length" @click="savePreviewAsProfile">保存方案</el-button>
              </div>
              <el-table :data="previewList" size="small" border>
                <el-table-column prop="clauseType" label="分类" width="160"/>
                <el-table-column prop="pointName" label="风险点"/>
                <el-table-column prop="algorithmType" label="算法类型" width="240"/>
              </el-table>
              <EmptyState 
                v-if="previewList.length === 0"
                title="暂无清单"
                description="请在左侧勾选条款后生成清单"
              />
            </el-tab-pane>
            <el-tab-pane label="方案管理" name="profiles">
              <div class="pane-toolbar">
                <el-input v-model="profileFilter" placeholder="搜索方案名称" size="small" style="width:220px" />
                <div class="spacer" />
                <el-button size="small" @click="createQuickProfile">新建空方案</el-button>
              </div>
              <el-table :data="filteredProfiles" size="small" border>
                <el-table-column prop="profileName" label="名称" min-width="220">
                  <template #default="{row}">
                    <span>{{ row.profileName }}</span>
                    <el-tag v-if="row.isDefault" size="small" type="success" class="ml8">默认</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="description" label="描述" />
                <el-table-column label="操作" width="240" fixed="right">
                  <template #default="{row}">
                    <el-dropdown trigger="click">
                      <el-button size="small">操作</el-button>
                      <template #dropdown>
                        <el-dropdown-menu>
                          <el-dropdown-item @click.native="applyProfile(row)">应用到左侧</el-dropdown-item>
                          <el-dropdown-item :disabled="!previewList.length" @click.native="overwriteProfileWithPreview(row)">用预览覆盖</el-dropdown-item>
                          <el-dropdown-item @click.native="renameProfile(row)">重命名</el-dropdown-item>
                          <el-dropdown-item @click.native="setDefaultRow(row)">设为默认</el-dropdown-item>
                          <el-dropdown-item divided @click.native="deleteProfileRow(row)">删除</el-dropdown-item>
                        </el-dropdown-menu>
                      </template>
                    </el-dropdown>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>
          </el-tabs>
        </el-card>
        <el-card v-if="showDetail" class="detail-card mt12">
          <template #header>
            <div class="card-header between"><span>详情</span></div>
          </template>
          <div v-if="currentPoint">
            <el-descriptions :column="2" size="small" border>
              <el-descriptions-item label="名称">{{ currentPoint.pointName }}</el-descriptions-item>
              <el-descriptions-item label="算法类型">{{ currentPoint.algorithmType }}</el-descriptions-item>
              <el-descriptions-item label="所属分类">{{ currentClause?.clauseName }}</el-descriptions-item>
            </el-descriptions>
            <div class="mt12" />
            <el-table :data="prompts" size="small" border>
              <el-table-column prop="promptKey" label="Key" width="160" />
              <el-table-column prop="name" label="名称" min-width="180" />
              <el-table-column prop="statusType" label="级别" width="90" />
              <el-table-column prop="enabled" label="启用" width="80">
                <template #default="{row}"><el-tag :type="row.enabled?'success':'info'">{{ row.enabled? '是':'否' }}</el-tag></template>
              </el-table-column>
              <el-table-column label="操作" width="160" fixed="right">
                <template #default="{row}">
                  <el-dropdown trigger="click">
                    <el-button size="small">操作</el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item @click.native="openEditPrompt(row)">编辑</el-dropdown-item>
                        <el-dropdown-item @click.native="togglePromptEnabled(row)">{{ row.enabled? '停用':'启用' }}</el-dropdown-item>
                        <el-dropdown-item @click.native="openCreateAction(row)">新增动作</el-dropdown-item>
                        <el-dropdown-item divided @click.native="deletePrompt(row)">删除</el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </template>
              </el-table-column>
            </el-table>
          </div>
          <div v-else class="muted">未选择风险点</div>
        </el-card>
      </el-col>
    </el-row>
    <!-- 编辑风险点 -->
    <el-dialog v-model="pointDlg.visible" :title="pointDlgTitle" width="520px">
      <el-form :model="pointDlg.form" label-width="100px">
        <el-form-item label="父节点">
          <el-select v-model="pointDlg.parentKey" placeholder="请选择父节点（分类/风险点），不支持选择提示" style="width:100%" @change="onPointParentChange">
            <el-option :key="'root'" label="（无父节点：新增分类）" value="" />
            <el-option v-for="opt in parentOptions" :key="opt.value" :label="opt.label" :value="opt.value" :disabled="opt.disabled" />
          </el-select>
        </el-form-item>
        <el-form-item label="编号"><el-input v-model="pointDlg.form.pointCode" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="pointDlg.form.pointName" /></el-form-item>
        <el-form-item label="算法类型"><el-input v-model="pointDlg.form.algorithmType" /></el-form-item>
        <el-form-item label="排序"><el-input v-model.number="pointDlg.form.sortOrder" type="number" /></el-form-item>
        <el-form-item label="启用"><el-switch v-model="pointDlg.form.enabled" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pointDlg.visible=false">取消</el-button>
        <el-button type="primary" @click="savePoint">保存</el-button>
      </template>
    </el-dialog>

    <!-- 编辑提示 -->
    <el-dialog v-model="promptDlg.visible" :title="promptDlg.mode==='create' ? '新增提示' : '编辑提示'" width="640px">
      <el-form :model="promptDlg.form" label-width="100px">
        <el-form-item label="Key"><el-input v-model="promptDlg.form.promptKey" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="promptDlg.form.name" /></el-form-item>
        <el-form-item label="级别">
          <el-select v-model="promptDlg.form.statusType" style="width:160px">
            <el-option label="INFO" value="INFO" />
            <el-option label="WARNING" value="WARNING" />
            <el-option label="ERROR" value="ERROR" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序"><el-input v-model.number="promptDlg.form.sortOrder" type="number" /></el-form-item>
        <el-form-item label="启用"><el-switch v-model="promptDlg.form.enabled" /></el-form-item>
        <el-form-item label="消息"><el-input v-model="promptDlg.form.message" type="textarea" rows="6" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="promptDlg.visible=false">取消</el-button>
        <el-button type="primary" @click="savePrompt">保存</el-button>
      </template>
    </el-dialog>

    <!-- 编辑动作 -->
    <el-dialog v-model="actionDlg.visible" :title="actionDlg.mode==='create' ? '新增动作' : '编辑动作'" width="640px">
      <el-form :model="actionDlg.form" label-width="100px">
        <el-form-item label="动作ID"><el-input v-model="actionDlg.form.actionId" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="actionDlg.form.actionType" style="width:160px">
            <el-option label="COPY" value="COPY" />
            <el-option label="REPLACE" value="REPLACE" />
            <el-option label="LINK" value="LINK" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序"><el-input v-model.number="actionDlg.form.sortOrder" type="number" /></el-form-item>
        <el-form-item label="启用"><el-switch v-model="actionDlg.form.enabled" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="actionDlg.form.actionMessage" type="textarea" rows="6" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="actionDlg.visible=false">取消</el-button>
        <el-button type="primary" @click="saveAction">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, computed, h } from 'vue'
import { useRouter } from 'vue-router'
import { DocumentChecked } from '@element-plus/icons-vue'
import riskApi from '@/api/ai/risk'
import { ElMessage, ElMessageBox, ElDropdown, ElDropdownMenu, ElDropdownItem, ElButton } from 'element-plus'
import { PageHeader, EmptyState } from '@/components/common'

const treeRef = ref()
const treeData = ref<any[]>([])
const treeProps = { label: 'label', children: 'children' }
const keyword = ref('')
const currentClause = ref<any>(null)
const currentPoint = ref<any>(null)
const previewList = ref<any[]>([])
  const manageMode = ref(false)
  const showDisabled = ref(false)
  const prompts = ref<any[]>([])
  const profiles = ref<any[]>([])
  const profileName = ref('')
  const profileFilter = ref('')
  const mainTab = ref('preview')
  const showDetail = ref(false)
// 返回到合同智能审核页
const router = useRouter()
function goBack() {
  router.push('/contract-review')
}
  const pointDlg = reactive<any>({ visible: false, mode: 'create', parentKey: '', form: { id: null, clauseTypeId: null, pointCode: '', pointName: '', algorithmType: '', sortOrder: 1, enabled: true } })
  const promptDlg = reactive<any>({ visible: false, mode: 'create', form: { id: null, pointId: null, promptKey: '', name: '', statusType: 'INFO', message: '', sortOrder: 1, enabled: true } })
  const actionDlg = reactive<any>({ visible: false, mode: 'create', form: { id: null, promptId: null, actionId: '', actionType: 'COPY', actionMessage: '', sortOrder: 1, enabled: true } })

// 动态标题：根据父节点选择显示新增分类/风险点/提示
const pointDlgTitle = computed(() => {
  const pk = String(pointDlg.parentKey || '')
  if (!pk) return pointDlg.mode === 'create' ? '新增分类' : '编辑'
  if (pk.startsWith('c-')) return pointDlg.mode === 'create' ? '新增风险点' : '编辑风险点'
  if (pk.startsWith('p-')) return '新增提示'
  return '新增/编辑'
})

// 父节点选项（显示现有所有节点：分类/风险点；提示节点禁用）
const parentOptions = computed(() => {
  const opts: Array<{ value: string; label: string; disabled?: boolean }> = []
  for (const c of treeData.value as any[]) {
    opts.push({ value: String(c.id), label: `[分类] ${c.label}` })
    for (const p of (c.children || [])) {
      if (p?.type === 'POINT') {
        opts.push({ value: String(p.id), label: `  └ [风险点] ${p.label}` })
        for (const r of (p.children || [])) {
          if (r?.type === 'PROMPT') {
            opts.push({ value: String(r.id), label: `    └ [提示] ${r.label}`, disabled: true })
          }
        }
      }
    }
  }
  return opts
})

function getClauseIdFromNodeKey(key: string | undefined | null): number | null {
  if (!key) return null
  const k = String(key)
  if (k.startsWith('c-')) return Number(k.slice(2))
  if (k.startsWith('p-')) {
    // 若 key 为点节点，则优先取该点的 parentClause.id；找不到则回退扫描树
    for (const c of treeData.value as any[]) {
      for (const p of (c.children || [])) {
        if (String(p.id) === k) return Number(String(c.id).slice(2))
      }
    }
  }
  if (k.startsWith('r-')) {
    // 若 key 为提示节点，优先取其 parentPoint 的父分类；回退扫描树
    for (const c of treeData.value as any[]) {
      for (const p of (c.children || [])) {
        for (const r of (p.children || [])) {
          if (String(r.id) === k) return Number(String(c.id).slice(2))
        }
      }
    }
  }
  return null
}

function onPointParentChange(val: string) {
  const clauseId = getClauseIdFromNodeKey(val)
  pointDlg.form.clauseTypeId = clauseId
}

function buildTree(raw: any[], kw: string) {
  const out: any[] = []
  const keyword = (kw || '').trim()
  for (const node of raw) {
    const clause = node.clauseType
    const pointNodes = Array.isArray(node.points) ? node.points : []
    const clauseNode: any = { id: `c-${clause.id}`, type: 'CLAUSE', label: clause.clauseName, children: [] as any[] }
    for (const pn of pointNodes) {
      const p = pn.point
      let prompts = Array.isArray(pn.prompts) ? pn.prompts : []
      // 关键字过滤：命中提示或命中点/分类
      if (keyword) {
        const baseHit = JSON.stringify({ clause, p }).includes(keyword)
        if (!baseHit) {
          prompts = prompts.filter((pr: any) => JSON.stringify(pr).includes(keyword))
        }
        // 若关键字不命中点/分类，且过滤后提示为空，则跳过该点
        if (!baseHit && prompts.length === 0) {
          continue
        }
      }
      const pointNode: any = { id: `p-${p.id}`, type: 'POINT', label: `${p.pointName}` , raw: p, parentClause: clause, children: [] as any[] }
      for (const pr of prompts) {
        pointNode.children.push({
          id: `r-${pr.id}`,
          type: 'PROMPT',
          label: `${(pr.name && String(pr.name).trim()) || pr.promptKey || '未命名提示'}${pr.statusType ? '（' + pr.statusType + '）' : ''}`,
          raw: pr,
          parentPoint: p,
          parentClause: clause,
          leaf: true
        })
      }
      clauseNode.children.push(pointNode)
    }
    if (clauseNode.children.length) out.push(clauseNode)
  }
  return out
}

async function loadTree() {
  const res: any = await riskApi.getTreePrompts(showDisabled.value ? undefined : true)
  const raw = res?.data || []
  treeData.value = buildTree(raw, keyword.value.trim())
}

function onNodeClick(data: any) {
  if (!data) return
  if (data.type === 'PROMPT') {
    currentPoint.value = data.parentPoint
    currentClause.value = data.parentClause
    if (manageMode.value) loadPrompts()
  } else if (data.type === 'POINT') {
    currentPoint.value = data.raw
    currentClause.value = data.parentClause
    if (manageMode.value) loadPrompts()
  } else {
    currentPoint.value = null
    currentClause.value = null
  }
}

// 自定义树节点：右侧只保留“操作”下拉，包含 显示详情/编辑/停用或启用/删除/新增提示
const renderTreeNode = (_: any, { data }: any) => {
  const label = data.label as string
  const type = data.type as string
  const id = data.id as string

  const onShowDetail = (e: MouseEvent) => {
    e.stopPropagation()
    if (type === 'POINT') {
      currentPoint.value = data.raw
      currentClause.value = data.parentClause
      showDetail.value = true
      loadPrompts()
    } else if (type === 'PROMPT') {
      currentPoint.value = data.parentPoint
      currentClause.value = data.parentClause
      showDetail.value = true
      loadPrompts()
    }
  }
  const onEdit = async (e: MouseEvent) => {
    e.stopPropagation()
    if (type === 'POINT') openEditPoint(data.raw)
    else if (type === 'PROMPT') openEditPrompt(data.raw)
  }
  const onToggleClick = async (e: MouseEvent) => { e.stopPropagation(); await onToggle(data) }
  const onDeleteClick = async (e: MouseEvent) => {
    e.stopPropagation()
    if (type === 'PROMPT') await deletePromptByKey(id)
    else if (type === 'POINT') await deletePointByKey(id)
    else if (type === 'CLAUSE') await deleteClauseByKey(id)
  }
  const onAddPrompt = async (e: MouseEvent) => {
    e.stopPropagation()
    if (type === 'POINT') openCreatePrompt(data.raw)
  }

  // 右侧“操作”下拉（使用 Element Plus 组件渲染）
  const menu = h(ElDropdown, { trigger: 'click', onClick: (e: MouseEvent)=> e.stopPropagation() }, {
    default: () => h(ElButton, { size: 'small' }, '操作'),
    dropdown: () => h(ElDropdownMenu, null, [
      h(ElDropdownItem, { onClick: onShowDetail }, () => '显示详情'),
      type !== 'CLAUSE' ? h(ElDropdownItem, { onClick: onEdit }, () => '编辑') : null,
      h(ElDropdownItem, { onClick: onToggleClick }, () => (data.enabled===false?'启用':'停用')),
      type === 'POINT' ? h(ElDropdownItem, { onClick: onAddPrompt }, () => '新增提示') : null,
      h(ElDropdownItem, { divided: true, class: 'danger', onClick: onDeleteClick }, () => '删除')
    ].filter(Boolean) as any)
  })

  return h('span', { class: 'custom-tree-node', style: 'display:flex;align-items:center;justify-content:space-between;width:100%' }, [
    h('span', null, label),
    menu
  ])
}

async function onToggle(data: any) {
  const type = data.type as string
  const id = data.id as string
  if (type === 'CLAUSE') {
    const cid = Number(String(id).slice(2))
    const to = !(data.enabled !== false)
    await riskApi.enableClauseType(cid, to)
    data.enabled = to
  } else if (type === 'POINT') {
    const pid = Number(String(id).slice(2))
    const to = !(data.raw?.enabled !== false)
    await riskApi.enablePoint(pid, to)
    if (data.raw) data.raw.enabled = to
  } else if (type === 'PROMPT') {
    const rid = Number(String(id).slice(2))
    const to = !(data.raw?.enabled !== false)
    await riskApi.enablePrompt(rid, to)
    if (data.raw) data.raw.enabled = to
  }
  await loadTree()
}

async function deleteClauseByKey(key: string) {
  const cid = String(key).startsWith('c-') ? Number(String(key).slice(2)) : null
  if (!cid) return
  try {
    await ElMessageBox.confirm('删除分类将清除其下所有风险点/提示/动作，是否继续？','确认',{type:'warning'})
    // 调后端分类删除（带强制）
    await riskApi.deleteClauseType(cid, true as any)
    ElMessage.success('已删除分类')
    await loadTree()
  } catch {}
}

async function deletePointByKey(key: string) {
  const pid = String(key).startsWith('p-') ? Number(String(key).slice(2)) : null
  if (!pid) return
  try {
    await ElMessageBox.confirm('是否删除该风险点及其下的提示/动作（强制）？','确认',{type:'warning'})
    await riskApi.deletePoint(pid, true)
    ElMessage.success('已删除风险点')
    await loadTree()
  } catch {}
}

async function deletePromptByKey(key: string) {
  const rid = String(key).startsWith('r-') ? Number(String(key).slice(2)) : null
  if (!rid) return
  try {
    await ElMessageBox.confirm('是否删除该提示及其动作（强制）？','确认',{type:'warning'})
    await riskApi.deletePrompt(rid, true)
    ElMessage.success('已删除提示')
    await loadTree()
  } catch {}
}

async function onPreviewSelection() {
  const nodes: any[] = (treeRef.value?.getCheckedNodes && treeRef.value.getCheckedNodes(true)) || []
  const pointIds = new Set<number>()
  for (const n of nodes) {
    if (n.type === 'PROMPT') {
      const pid = n.parentPoint?.id
      if (typeof pid === 'number') pointIds.add(pid)
    } else if (n.type === 'POINT') {
      const pid = n.raw?.id
      if (typeof pid === 'number') pointIds.add(pid)
    } else if (n.type === 'CLAUSE') {
      for (const pNode of (n.children || [])) {
        if (pNode?.type === 'POINT' && typeof pNode?.raw?.id === 'number') pointIds.add(pNode.raw.id)
      }
    }
  }
  const idArr = Array.from(pointIds)
  if (!idArr.length) { previewList.value = []; return }
  const res: any = await riskApi.previewSelection(idArr)
  previewList.value = res?.data || []
}

async function loadProfiles() {
  const res: any = await riskApi.listProfiles()
  profiles.value = res?.data || []
}

onMounted(async () => { await loadTree(); await loadProfiles() })

// ---- manage helpers ----
function openCreatePointFromClause() {
  // 允许在三种情况下新增：
  // 1) 当前选中为分类节点 → 直接挂到该分类
  // 2) 当前选中为点/提示 → 挂到其父分类
  // 3) 未选中任何节点 → 选择第一个分类作为默认（或弹确认）
  const sel: any = (treeRef.value?.getCurrentNode && treeRef.value.getCurrentNode()) || null
  let defaultKey = ''
  if (sel && sel.id) {
    const sid = String(sel.id)
    if (sid.startsWith('c-')) defaultKey = sid
    else if (sid.startsWith('p-')) defaultKey = sid
    else if (sid.startsWith('r-')) defaultKey = `p-${sel.parentPoint?.id}`
  } else if (treeData.value.length && String(treeData.value[0].id).startsWith('c-')) {
    defaultKey = String(treeData.value[0].id)
  }
  const clauseId = getClauseIdFromNodeKey(defaultKey)
  pointDlg.mode = 'create'
  pointDlg.parentKey = defaultKey
  pointDlg.form = { id: null, clauseTypeId: clauseId, pointCode: '', pointName: '', algorithmType: '', sortOrder: 1, enabled: true }
  pointDlg.visible = true
}

async function loadPrompts() {
  if (!currentPoint.value) { prompts.value = []; return }
  const res: any = await riskApi.listPrompts(currentPoint.value.id)
  prompts.value = res?.data || []
}

function openEditPoint(p: any) {
  pointDlg.mode = 'edit'
  pointDlg.form = { id: p.id, clauseTypeId: p.clauseTypeId, pointCode: p.pointCode, pointName: p.pointName, algorithmType: p.algorithmType, sortOrder: p.sortOrder ?? 1, enabled: p.enabled !== false }
  pointDlg.visible = true
}
async function togglePointEnabled(p: any) {
  await riskApi.enablePoint(p.id, !p.enabled)
  p.enabled = !p.enabled
  ElMessage.success('已更新状态')
}
async function deletePoint(p: any) {
  try {
    await riskApi.deletePoint(p.id)
  } catch (e:any) {
    try {
      await ElMessageBox.confirm('该风险点包含提示/被方案引用，是否强制删除？','确认',{type:'warning'})
      await riskApi.deletePoint(p.id, true)
      ElMessage.success('已强制删除风险点')
    } catch { return }
  }
  await loadTree(); currentPoint.value = null
}

function openCreatePrompt(p: any) {
  promptDlg.mode = 'create'
  promptDlg.form = { id: null, pointId: p.id, promptKey: '', name: '', statusType: 'INFO', message: '', sortOrder: (prompts.value?.length || 0) + 1, enabled: true }
  promptDlg.visible = true
}
function openEditPrompt(pr: any) {
  promptDlg.mode = 'edit'
  promptDlg.form = { id: pr.id, pointId: pr.pointId, promptKey: pr.promptKey, name: pr.name, statusType: pr.statusType, message: pr.message, sortOrder: pr.sortOrder ?? 1, enabled: pr.enabled !== false }
  promptDlg.visible = true
}
async function togglePromptEnabled(pr: any) { await riskApi.enablePrompt(pr.id, !pr.enabled); pr.enabled = !pr.enabled }
// 删除提示；若删除失败则提供强制删除选项
async function deletePrompt(pr: any) {
  try {
    await riskApi.deletePrompt(pr.id)
  } catch (e:any) {
    try {
      await ElMessageBox.confirm('该提示包含动作引用，是否强制删除？','确认',{type:'warning'})
      await riskApi.deletePrompt(pr.id, true)
      ElMessage.success('已强制删除提示')
    } catch { return }
  }
  await loadPrompts()
}

function openCreateAction(pr: any) {
  actionDlg.mode = 'create'
  actionDlg.form = { id: null, promptId: pr.id, actionId: 'action' + Date.now(), actionType: 'COPY', actionMessage: '', sortOrder: 1, enabled: true }
  actionDlg.visible = true
}

async function savePoint() {
  // 前端兜底：校验并生成默认值
  const f = pointDlg.form
  const name = String(f.pointName || '').trim()
  if (!name) { ElMessage.warning('请填写风险点名称'); return }
  // 移除前端自动编号分配，让后端统一分配 ZX-XXXX 编码
  if (!f.algorithmType || String(f.algorithmType).trim() === '') {
    f.algorithmType = name
  }
  // 分三种：
  // 1) parentKey 为空 → 新增分类
  // 2) parentKey 为 c-xxx → 在该分类下新增风险点
  // 3) parentKey 为 p-xxx → 在该风险点下新增“提示”（不是风险点）
  const pk = String(pointDlg.parentKey || '')
  if (!pk) {
    // 新增分类
    const code = 'c_' + Date.now()
    const data = { clauseCode: code, clauseName: f.pointName, sortOrder: f.sortOrder ?? 1, enabled: f.enabled !== false }
    await riskApi.createClauseType(data)
    ElMessage.success('已新增分类')
  } else if (pk.startsWith('c-')) {
    // 在分类下新增风险点
    if (!f.clauseTypeId) { ElMessage.warning('请选择父分类'); return }
    if (pointDlg.mode === 'create') {
      await riskApi.createPoint(pointDlg.form)
    } else {
      await riskApi.updatePoint(pointDlg.form.id, pointDlg.form)
    }
    ElMessage.success('已保存风险点')
  } else if (pk.startsWith('p-')) {
    // 在风险点下新增提示
    const parentPointId = Number(pk.slice(2))
    if (!Number.isFinite(parentPointId)) { ElMessage.warning('父风险点无效'); return }
    // 复用当前输入：名称→提示名称；算法类型/编号忽略
    const pr = { pointId: parentPointId, promptKey: 'pr' + Date.now(), name: f.pointName, statusType: 'INFO', message: '', sortOrder: 1, enabled: true }
    await riskApi.createPrompt(pr)
    ElMessage.success('已在父风险点下新增提示')
  }
  pointDlg.visible = false
  await loadTree()
  // 尝试定位到新建/编辑的点节点
  try {
    const pid = pointDlg.form.id
    const cid = pointDlg.form.clauseTypeId
    if (treeRef.value && cid) {
      // 展开目标分类
      const clauseKey = `c-${cid}`
      // el-tree 无直接展开API，这里通过默认展开全部已满足
      // 将当前节点设置为刚保存的点（如果能在树中找到）
      const all = (treeRef.value.getNode && treeRef.value.getNode(clauseKey)) ? treeRef.value : null
    }
  } catch {}
  ElMessage.success('已保存风险点')
}

async function savePrompt() {
  // 兜底：保证名称与Key不为空，避免树上只显示级别
  const f = promptDlg.form
  const nm = String(f.name || '').trim()
  const pk = String(f.promptKey || '').trim()
  if (!nm && !pk) {
    f.promptKey = 'pr' + Date.now()
    f.name = '未命名提示'
  } else if (!nm) {
    f.name = pk
  } else if (!pk) {
    f.promptKey = 'pr' + Date.now()
  }
  if (promptDlg.mode === 'create') {
    await riskApi.createPrompt(promptDlg.form)
  } else {
    await riskApi.updatePrompt(promptDlg.form.id, promptDlg.form)
  }
  promptDlg.visible = false
  await loadPrompts()
  ElMessage.success('已保存提示')
}

async function saveAction() {
  if (actionDlg.mode === 'create') {
    await riskApi.createAction(actionDlg.form)
  } else {
    await riskApi.updateAction(actionDlg.form.id, actionDlg.form)
  }
  actionDlg.visible = false
  await loadPrompts()
  ElMessage.success('已保存动作')
}

// ===== profiles manage =====
const filteredProfiles = computed(() => {
  const kw = profileFilter.value.trim()
  if (!kw) return profiles.value
  return profiles.value.filter((p: any) => (p.profileName || '').includes(kw))
})

async function savePreviewAsProfile() {
  if (!previewList.value.length) { ElMessage.warning('请先生成审核清单'); return }
  const name = profileName.value?.trim() || ('方案_' + new Date().toLocaleString())
  const res: any = await riskApi.createProfile({ profileCode: 'p_' + Date.now(), profileName: name, isDefault: false, description: '从清单管理保存' })
  const pf = res?.data
  if (pf?.id) {
    const items = previewList.value.map((it: any, idx: number) => ({ profileId: pf.id, clauseTypeId: 0, pointId: it.pointId, sortOrder: idx + 1 }))
    await riskApi.saveProfileItems(pf.id, items)
    ElMessage.success('已保存方案')
    profileName.value = ''
    await loadProfiles()
  }
}

async function applyProfile(row: any) {
  const res: any = await riskApi.listProfileItems(row.id)
  const items = res?.data || []
  const pointIds: number[] = items.map((x: any) => x.pointId).filter((v: any) => typeof v === 'number')
  // 将 pointIds 映射为树上对应点下的所有提示叶子 key（r-<id>）
  const promptKeys: string[] = []
  for (const clause of treeData.value as any[]) {
    for (const pNode of clause.children || []) {
      if (pNode.type === 'POINT') {
        const pid = pNode.raw?.id
        if (pointIds.includes(pid)) {
          for (const r of pNode.children || []) {
            if (r.type === 'PROMPT') promptKeys.push(r.id)
          }
        }
      }
    }
  }
  if (treeRef.value?.setCheckedKeys) treeRef.value.setCheckedKeys(promptKeys)
  await onPreviewSelection()
  ElMessage.success('已应用到左侧勾选')
}

async function overwriteProfileWithPreview(row: any) {
  if (!previewList.value.length) { ElMessage.warning('请先生成审核清单'); return }
  const items = previewList.value.map((it: any, idx: number) => ({ profileId: row.id, clauseTypeId: 0, pointId: it.pointId, sortOrder: idx + 1 }))
  await riskApi.saveProfileItems(row.id, items)
  ElMessage.success('已用当前预览覆盖该方案')
}

async function renameProfile(row: any) {
  try {
    const { value } = await ElMessageBox.prompt('请输入新的方案名称', '重命名', { inputValue: row.profileName })
    const name = String(value || '').trim()
    if (!name) return
    await riskApi.updateProfile(row.id, { profileName: name })
    await loadProfiles()
    ElMessage.success('已重命名')
  } catch {}
}

async function deleteProfileRow(row: any) {
  try {
    await riskApi.deleteProfile(row.id)
    ElMessage.success('已删除方案')
  } catch (e:any) {
    // 强制删除兜底
    try {
      await ElMessageBox.confirm('该方案包含清单条目，是否强制删除？','确认',{type:'warning'})
      await riskApi.deleteProfile(row.id, true)
      ElMessage.success('已强制删除方案')
    } catch {}
  } finally {
    await loadProfiles()
  }
}

async function setDefaultRow(row: any) {
  await riskApi.setDefaultProfile(row.id, true)
  await loadProfiles()
  ElMessage.success('已设为默认')
}

async function createQuickProfile() {
  const code = 'p_' + Date.now()
  const name = '方案_' + new Date().toLocaleString()
  await riskApi.createProfile({ profileCode: code, profileName: name, isDefault: false, description: '' })
  await loadProfiles()
  ElMessage.success('已创建方案')
}
</script>

<style scoped>
/* 全局变量定义 */
:root {
  --primary-color: #409eff;
  --primary-light: #ecf5ff;
  --border-radius: 6px;
  --card-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  --hover-shadow: 0 4px 16px 0 rgba(0, 0, 0, 0.15);
  --transition-time: 0.3s;
}

/* 基础布局 */
.risk-lib-page { 
  padding: 16px; 
  display: flex; 
  flex-direction: column; 
  background-color: #f5f7fa;
}

.risk-lib-page > .el-row { 
  flex: 1; 
  min-height: 0; 
}

.left-col { 
  display: flex; 
}

/* 卡片样式增强 */
.side-card { 
  height: calc(100vh - 32px); 
  display: flex; 
  flex-direction: column; 
  width: 100%; 
  border-radius: var(--border-radius);
  box-shadow: var(--card-shadow);
  transition: box-shadow var(--transition-time);
}

.side-card:hover {
  box-shadow: var(--hover-shadow);
}

.side-card :deep(.el-card__body) { 
  padding-top: 10px; 
  display: flex; 
  flex-direction: column; 
  flex: 1; 
  min-height: 0; 
}

/* 卡片头部样式 */
.card-header { 
  font-weight: 600; 
  display: flex; 
  align-items: center; 
  justify-content: space-between; 
  padding: 4px 0;
  color: #303133;
}

.card-header.between { 
  display: flex; 
  align-items: center; 
  justify-content: space-between; 
}

/* 树形控件样式增强 */
.tree-wrap { 
  flex: 1; 
  min-height: 0; 
  overflow: auto; 
  margin-top: 8px; 
  padding: 4px;
  border-radius: var(--border-radius);
}

/* 树节点悬停效果 */
.tree-wrap :deep(.el-tree-node__content) {
  border-radius: 4px;
  transition: background-color var(--transition-time);
}

.tree-wrap :deep(.el-tree-node__content:hover) {
  background-color: var(--primary-light);
}

/* 自定义树节点样式 */
.custom-tree-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 2px 0;
}

/* 按钮和工具栏样式 */
.actions { 
  margin-top: 12px; 
  display: flex; 
  gap: 8px; 
}

.toolbar { 
  display: flex; 
  flex-direction: column; 
  gap: 8px; 
  margin-bottom: 12px;
}

.toolbar-row { 
  display: flex; 
  align-items: center; 
  gap: 8px; 
  padding: 4px 0;
}

.toolbar-row .spacer { 
  flex: 1; 
}

.pane-toolbar { 
  display: flex; 
  align-items: center; 
  gap: 8px; 
  margin-bottom: 12px; 
  padding: 4px 8px;
  background-color: #f5f7fa;
  border-radius: var(--border-radius);
}

.pane-toolbar .spacer { 
  flex: 1; 
}

/* 按钮悬停效果增强 */
.btn-line { 
  display: flex; 
  gap: 8px; 
  margin-top: 12px; 
}

.btn-line :deep(.el-button),
.actions :deep(.el-button),
.toolbar-row :deep(.el-button),
.pane-toolbar :deep(.el-button) {
  transition: transform var(--transition-time), box-shadow var(--transition-time);
}

.btn-line :deep(.el-button:hover),
.actions :deep(.el-button:hover),
.toolbar-row :deep(.el-button:hover),
.pane-toolbar :deep(.el-button:hover) {
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* 表格悬停效果 */
:deep(.el-table__row) {
  transition: background-color var(--transition-time);
}

:deep(.el-table__row:hover) {
  background-color: var(--primary-light) !important;
}

/* 标签页样式增强 */
:deep(.el-tabs__item) {
  transition: all var(--transition-time);
}

:deep(.el-tabs__item:hover) {
  color: var(--primary-color);
  transform: translateY(-2px);
}

/* 表单项悬停效果 */
:deep(.el-form-item) {
  transition: transform var(--transition-time);
}

:deep(.el-form-item:hover) {
  transform: translateY(-2px);
}

.mt12 { 
  margin-top: 12px; 
}

.muted { 
  color: #909399; 
  padding: 16px; 
  font-style: italic;
  border-radius: var(--border-radius);
  background-color: #f5f7fa;
}

/* 树节点"操作"下拉的样式 */
.danger { 
  color: #f56c6c; 
}

/* 下拉菜单悬停效果 */
:deep(.el-dropdown-menu__item) {
  transition: background-color var(--transition-time), transform var(--transition-time);
}

:deep(.el-dropdown-menu__item:hover) {
  background-color: var(--primary-light);
  transform: translateX(4px);
}

:deep(.el-dropdown-menu__item.danger:hover) {
  background-color: #fef0f0;
}
</style>


