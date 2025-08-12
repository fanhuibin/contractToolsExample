<template>
  <div class="risk-lib-page">
    <el-row :gutter="16">
      <!-- 左侧：条款树与筛选 -->
      <el-col :span="8">
        <el-card class="side-card">
          <template #header>
            <div class="card-header between">
              <span>合同智能审核 · 条款库</span>
              <div class="header-actions">
                <el-button size="small" type="primary" link @click="goBack">返回</el-button>
                <el-switch v-model="manageMode" active-text="管理模式" size="small" />
              </div>
            </div>
          </template>
          <div class="toolbar">
            <el-input v-model="keyword" placeholder="搜索提示/风险点/算法类型/编号" clearable @input="loadTree" />
            <div class="toolbar-row">
              <el-switch v-model="showDisabled" active-text="显示停用" size="small" @change="loadTree" />
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
            />
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：分栏标签 -->
      <el-col :span="16">
        <el-card>
          <el-tabs v-model="activeTab">
            <el-tab-pane label="详情" name="detail">
              <div v-if="currentPoint">
                <el-descriptions :column="2" size="small" border>
                  <el-descriptions-item label="名称">{{ currentPoint.pointName }}</el-descriptions-item>
                  <el-descriptions-item label="算法类型">{{ currentPoint.algorithmType }}</el-descriptions-item>
                  <el-descriptions-item label="所属分类">{{ currentClause?.clauseName }}</el-descriptions-item>
                </el-descriptions>
                <div v-if="manageMode" class="btn-line">
                  <el-button type="primary" size="small" @click="openEditPoint(currentPoint)">编辑</el-button>
                  <el-button type="warning" size="small" @click="togglePointEnabled(currentPoint)">{{ currentPoint.enabled ? '停用' : '启用' }}</el-button>
                  <el-popconfirm title="确定删除该风险点？" @confirm="deletePoint(currentPoint)">
                    <template #reference>
                      <el-button type="danger" size="small">删除</el-button>
                    </template>
                  </el-popconfirm>
                  <el-button type="primary" plain size="small" @click="openCreatePrompt(currentPoint)">新增提示</el-button>
                </div>
              </div>
              <div v-else class="muted">请选择左侧的风险点查看详情</div>
            </el-tab-pane>

            <el-tab-pane label="提示与动作" name="prompts">
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
            </el-tab-pane>

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
      </el-col>
    </el-row>
    <!-- 编辑风险点 -->
    <el-dialog v-model="pointDlg.visible" :title="pointDlg.mode==='create' ? '新增风险点' : '编辑风险点'" width="520px">
      <el-form :model="pointDlg.form" label-width="100px">
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
import { ref, onMounted, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import riskApi from '@/api/ai/risk'
import { ElMessage, ElMessageBox } from 'element-plus'

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
  const activeTab = ref('detail')
// 返回到合同智能审核页
const router = useRouter()
function goBack() {
  router.push('/contract-review')
}
  const pointDlg = reactive<any>({ visible: false, mode: 'create', form: { id: null, clauseTypeId: null, pointCode: '', pointName: '', algorithmType: '', sortOrder: 1, enabled: true } })
  const promptDlg = reactive<any>({ visible: false, mode: 'create', form: { id: null, pointId: null, promptKey: '', name: '', statusType: 'INFO', message: '', sortOrder: 1, enabled: true } })
  const actionDlg = reactive<any>({ visible: false, mode: 'create', form: { id: null, promptId: null, actionId: '', actionType: 'COPY', actionMessage: '', sortOrder: 1, enabled: true } })

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
        prompts = prompts.filter((pr: any) => baseHit || JSON.stringify(pr).includes(keyword))
      }
      if (!prompts.length) continue
      const pointNode: any = { id: `p-${p.id}`, type: 'POINT', label: `${p.pointName}` , raw: p, parentClause: clause, children: [] as any[] }
      for (const pr of prompts) {
        pointNode.children.push({
          id: `r-${pr.id}`,
          type: 'PROMPT',
          label: `${pr.name}${pr.statusType ? '（' + pr.statusType + '）' : ''}`,
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
  // 仅当当前选中为分类节点时允许快速新建
  const sel: any = (treeRef.value?.getCurrentNode && treeRef.value.getCurrentNode()) || null
  if (!sel || !String(sel.id).startsWith('c-')) return
  pointDlg.mode = 'create'
  pointDlg.form = { id: null, clauseTypeId: Number(String(sel.id).slice(2)), pointCode: '', pointName: '', algorithmType: '', sortOrder: 1, enabled: true }
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
  await riskApi.deletePoint(p.id)
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
async function deletePrompt(pr: any) { await riskApi.deletePrompt(pr.id); await loadPrompts() }

function openCreateAction(pr: any) {
  actionDlg.mode = 'create'
  actionDlg.form = { id: null, promptId: pr.id, actionId: 'action' + Date.now(), actionType: 'COPY', actionMessage: '', sortOrder: 1, enabled: true }
  actionDlg.visible = true
}

async function savePoint() {
  if (pointDlg.mode === 'create') {
    await riskApi.createPoint(pointDlg.form)
  } else {
    await riskApi.updatePoint(pointDlg.form.id, pointDlg.form)
  }
  pointDlg.visible = false
  await loadTree()
  ElMessage.success('已保存风险点')
}

async function savePrompt() {
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
.risk-lib-page { padding: 8px; }
.card-header { font-weight: 600; display:flex; align-items:center; justify-content:space-between; }
.card-header.between { display:flex; align-items:center; justify-content:space-between; }
.tree-wrap { max-height: 520px; overflow: auto; margin-top: 8px; }
.actions { margin-top: 12px; display: flex; gap: 8px; }
.toolbar { display:flex; flex-direction:column; gap:8px; }
.toolbar-row { display:flex; align-items:center; gap:8px; }
.pane-toolbar { display:flex; align-items:center; gap:8px; margin-bottom:8px; }
.pane-toolbar .spacer { flex:1; }
.btn-line { display:flex; gap:8px; margin-top:12px; }
.mt12 { margin-top: 12px; }
.muted { color: #909399; padding: 16px; }
.side-card :deep(.el-card__body){ padding-top: 10px; }
</style>


