<template>
  <div class="contract-review-page">
    <PageHeader 
      title="合同智能审核"
      description="上传合同文件，选择审核清单，系统将进行智能风险预审并返回结果。"
      :icon="Document"
    />

    <el-row :gutter="20" class="main-content">
      <!-- Main -->
      <el-col :span="16">
        <el-card class="main-content-card">
          <template #header>
            <div class="card-header">
              <div class="card-header-left">
                <h3>上传合同并执行审核</h3>
                <span class="subtitle">右侧选择审核清单或从条款库勾选生成</span>
              </div>
            </div>
          </template>

          <!-- Editor View -->
          <div v-if="showEditor" class="editor-view">
            <div class="editor-toolbar">
              <span class="editor-file-name">正在预览: {{ selectedFile?.name }}</span>
              <div>
                <el-button type="primary" @click="startAuditInEditor" :loading="reviewing">
                  <el-icon><Search /></el-icon>
                  开始审核
                </el-button>
                <el-button @click="backToUpload">
                  <el-icon><ArrowLeft /></el-icon>
                  返回
                </el-button>
              </div>
            </div>
            <div class="editor-wrapper">
              <OnlyOfficeEditor
                ref="onlyofficeEditorRef"
                v-if="uploadedFileId"
                :file-id="uploadedFileId"
                :can-edit="true"
                @ready="onEditorReady"
                @error="onEditorError"
                height="calc(100vh - 350px)"
              />
              <div v-else class="editor-placeholder">
                 <el-icon class="placeholder-icon"><Document /></el-icon>
                 <p>文档编辑器将在这里显示</p>
                 <p class="placeholder-note">等待文件上传...</p>
              </div>
            </div>
          </div>
          
          <!-- Upload and Result View -->
          <div v-else>
          <!-- Upload Area -->
          <FileUploadZone
            accept=".pdf,.doc,.docx,.xls,.xlsx,.jpg,.jpeg,.png"
            tip="支持格式: PDF, Word, Excel, 图片"
            @change="handleFileChange"
          />

          <div v-if="selectedFile" class="file-info-actions">
            <div class="file-info">
              <el-icon class="file-icon"><Document /></el-icon>
              <div class="file-details">
                <span class="file-name">{{ selectedFile.name }}</span>
                <span class="file-size">{{ formatFileSize(selectedFile.size) }}</span>
              </div>
            </div>
            <div class="action-buttons">
              <el-button type="primary" class="start-button" @click="onStartClick" :loading="reviewing">
                <el-icon><CaretRight /></el-icon>
                开始审核
              </el-button>
                <el-button type="success" plain @click="openInEditor" :disabled="reviewing || !!uploadedFileId">
                <el-icon><Edit /></el-icon>
                在编辑器中打开
              </el-button>
            </div>
          </div>

          <!-- Progress / Error -->
          <div v-if="reviewing" class="extracting-status">
            <el-progress :percentage="progress" :status="progressStatus" :stroke-width="12" :show-text="false" />
            <div class="status-text">
              <el-icon class="status-icon" :class="{'rotating': reviewing && progress < 100}"><Loading /></el-icon>
              {{ statusText }}
            </div>
          </div>
          <div v-if="error" class="error-message">
            <el-alert :title="error" type="error" :closable="false" show-icon effect="dark" />
          </div>

          <!-- Result -->
          <div v-if="results.length" class="result-area">
            <div class="result-header">
              <h4><el-icon class="result-icon"><Check /></el-icon>审核结果</h4>
              <div class="result-actions">
                <el-button type="primary" size="small" @click="copyJson()">
                  <el-icon><CopyDocument /></el-icon>复制JSON
                </el-button>
              </div>
            </div>
            <el-alert v-if="traceId" :title="'TraceID: ' + traceId + '，耗时 ' + elapsedMs + 'ms'" type="info" show-icon class="mb10" effect="light" />
            <el-alert v-if="usage" :title="usageTitle" type="success" show-icon class="mb10" effect="light" />
            <div class="result-container">
              <pre class="result-text">{{ prettyJson }}</pre>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- Sidebar: Checklist selection -->
      <el-col :span="8">
        <el-card v-if="!showEditor" class="template-selector-card">
          <template #header>
            <div class="template-header">
              <h4><el-icon class="checklist-icon"><List /></el-icon>审核清单</h4>
              <el-button size="small" type="primary" plain @click="openChecklistManager">
                <el-icon><Setting /></el-icon>审核清单管理
              </el-button>
            </div>
          </template>

          <el-radio-group v-model="selectedProfileId" class="template-radio-group">
            <el-radio v-for="pf in profiles" :key="pf.id" :label="pf.id" class="template-radio-item" border>
              <div class="radio-content">
                <span>{{ pf.profileName }}</span>
                <el-tag v-if="pf.isDefault" size="small" type="success" effect="dark" class="template-tag">默认</el-tag>
              </div>
            </el-radio>
          </el-radio-group>

          <el-divider />
            <div class="actions-line">
              <el-button type="primary" plain size="small" @click="openSelectFromLibrary">
                <el-icon><Select /></el-icon>从条款库选择
              </el-button>
              <el-input v-model="selectionProfileName" placeholder="方案名称" size="small" class="profile-name-input" />
              <el-button type="success" plain size="small" @click="saveSelectionAsProfile" :disabled="!selectionPointIds.length">
                <el-icon><FolderAdd /></el-icon>保存为方案
              </el-button>
            </div>
          <el-divider content-position="left"><el-icon class="divider-icon"><Collection /></el-icon>当前选择</el-divider>
          <el-table :data="selectionPreview" size="small" border height="280">
            <el-table-column prop="clauseType" label="分类" width="140" />
            <el-table-column prop="pointName" label="风险点" />
          </el-table>
        </el-card>
        <el-card v-else class="template-selector-card">
          <template #header>
            <div class="template-header">
              <h4><el-icon class="checklist-icon"><List /></el-icon>风险审核</h4>
            </div>
          </template>
          <RiskCardPanel v-if="results.length" :results="results" @goto="handleGoToAnchor" />
          <el-empty v-else description="暂无数据，点击右侧“开始审核”" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 选择条款库弹窗：复用已有 RiskLibrary 树的预览接口 -->
    <el-dialog v-model="libVisible" title="从条款库选择风险点" width="820px" append-to-body>
      <RiskLibraryEmbed ref="libRef" />
      <template #footer>
        <el-button @click="libVisible=false">取消</el-button>
        <el-button type="primary" @click="applySelectionFromLibrary">应用所选</el-button>
      </template>
    </el-dialog>

    <!-- OnlyOffice编辑器区域 -->
    <!--
    <div v-if="showEditor_REMOVED" class="editor-overlay">
      <div class="editor-container">
        <div class="editor-header">
          <div class="editor-title">
            <el-icon><Edit /></el-icon>
            <span>文档编辑器</span>
            <el-tag v-if="selectedFile" size="small">{{ selectedFile.name }}</el-tag>
          </div>
          <div class="editor-actions">
            <el-button v-if="editorReady && !hasAudited" type="primary" @click="startAuditInEditor" :loading="reviewing">
              <el-icon><Search /></el-icon>
              开始审核
            </el-button>
            <el-button @click="backToUpload">
              <el-icon><ArrowLeft /></el-icon>
              返回上传
            </el-button>
          </div>
        </div>
        
        <div class="editor-content">
          <OnlyOfficeEditor
            v-if="uploadedFileId"
            :file-id="uploadedFileId"
            :can-edit="true"
            @ready="onEditorReady"
            @error="onEditorError"
            height="100%"
          />
          <div v-else class="editor-placeholder">
            <el-icon class="placeholder-icon"><Document /></el-icon>
            <p>文档编辑器将在这里显示</p>
            <p class="placeholder-note">等待文件上传...</p>
          </div>
        </div>

        <div v-if="hasAudited && results.length" class="audit-results-panel">
          <div class="panel-header">
            <h4><el-icon><Check /></el-icon>审核结果</h4>
            <div class="panel-actions">
              <el-button size="small" @click="exportAuditReport">
                <el-icon><Download /></el-icon>
                导出报告
              </el-button>
              <el-button size="small" type="primary" @click="saveAuditResult">
                保存结果
              </el-button>
            </div>
          </div>
          
          <div class="results-list">
            <div v-for="(result, index) in results" :key="index" class="result-item" :class="getRiskLevel(result.statusType)">
              <div class="result-header">
                <span class="result-type">{{ result.clauseType }}</span>
                <el-tag :type="tagType(result.statusType)" size="small">{{ result.statusType }}</el-tag>
              </div>
              <div class="result-content">
                <p class="result-decision">{{ result.decisionType }}</p>
                <p class="result-message">{{ result.message }}</p>
              </div>
              <div class="result-actions">
                <el-button size="small" text @click="locateRisk(result)">
                  <el-icon><Location /></el-icon>
                  定位
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    -->
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, defineAsyncComponent } from 'vue'
import { UploadFilled, Document, CaretRight, Loading, Check, CopyDocument, List, Setting, Select, FolderAdd, Collection, Edit, ArrowLeft, Search, Location, Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import riskApi from '@/api/ai/risk'
import { useRouter } from 'vue-router'
import OnlyOfficeEditor from '@/components/onlyoffice/OnlyOfficeEditor.vue'
import RiskCardPanel from '@/components/ai/RiskCardPanel.vue' // 引入新组件
import { PageHeader, FileUploadZone } from '@/components/common'

// 轻量内嵌的条款库选择器（最小实现：只提供选择和预览功能）
const RiskLibraryEmbed = defineAsyncComponent(() => import('./RiskLibraryEmbed.vue'))

const router = useRouter()

const selectedFile = ref<File | null>(null)
const reviewing = ref(false)
const progress = ref(0)
const progressStatus = ref<'success' | 'warning' | 'exception' | ''>('')
const statusText = ref('')
const error = ref('')

const profiles = ref<any[]>([])
const selectedProfileId = ref<number | null>(null)

const selectionPointIds = ref<number[]>([])
const selectionPreview = ref<any[]>([])
const selectionProfileName = ref('')
const traceId = ref('')
const elapsedMs = ref(0)
const docMeta = ref<any>({ pages: 0, paragraphs: 0 })
const results = ref<any[]>([])
const usage = ref<any | null>(null)

// 条款库选择相关
const libVisible = ref(false)
const libRef = ref<any>(null)

// 文档编辑器相关
const showEditor = ref(false)
const editorReady = ref(false)
const uploadedFileId = ref('')
const hasAudited = ref(false)
const onlyofficeEditorRef = ref<InstanceType<typeof OnlyOfficeEditor> | null>(null) // 引用编辑器实例

const prettyJson = computed(() => {
  try { return JSON.stringify({ traceId: traceId.value, elapsedMs: elapsedMs.value, docMeta: docMeta.value, results: results.value }, null, 2) }
  catch { return '' }
})

const usageTitle = computed(() => {
  if (!usage.value) return ''
  const u = usage.value as any
  const pt = Number(u.promptTokens ?? 0)
  const ct = Number(u.completionTokens ?? 0)
  const tt = Number(u.totalTokens ?? (pt + ct))
  const chars = Number(u.promptChars ?? 0)
  // 价格：输入 ¥0.012/1K，输出 ¥0.024/1K（用户给出的口径）
  const inCost = (pt / 1000) * 0.012
  const outCost = (ct / 1000) * 0.024
  const totalCost = inCost + outCost
  return `字数: ${chars}，输入tokens: ${pt} 输出tokens: ${ct}，本次调用费用约 ¥${totalCost.toFixed(2)}`
})

const canStartReview = computed(() => !!selectedFile.value && (!!selectedProfileId.value || selectionPointIds.value.length > 0))

onMounted(async () => {
  try {
    const res = await riskApi.listProfiles()
    profiles.value = (res as any)?.data || []
    const def = profiles.value.find((p: any) => p.isDefault)
    if (def) selectedProfileId.value = def.id
  } catch (e: any) {
    ElMessage.error(e?.message || '加载方案失败')
  }
})

function handleFileChange(file: File) {
  const isLt100M = file.size / 1024 / 1024 < 100
  if (!isLt100M) { ElMessage.error('文件大小不能超过100MB'); return }
  selectedFile.value = file
  error.value = ''
  traceId.value = ''
  results.value = []
  hasAudited.value = false
}

function formatFileSize(size: number) {
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(2)} KB`
  return `${(size / (1024 * 1024)).toFixed(2)} MB`
}

function tagType(t: string) {
  if (t === 'ERROR') return 'danger'
  if (t === 'WARNING') return 'warning'
  return 'info'
}

function copyJson() {
  try {
    const text = JSON.stringify({ traceId: traceId.value, elapsedMs: elapsedMs.value, docMeta: docMeta.value, results: results.value }, null, 2)
    navigator.clipboard.writeText(text)
    ElMessage.success('已复制JSON')
  } catch (e) {
    ElMessage.error('复制失败')
  }
}

const startReview = async () => {
  if (!selectedFile.value) {
    ElMessage.error('请先上传合同文件');
    return;
  }
  reviewing.value = true;
  results.value = []; // 清空旧结果

  // 模拟数据，用于前端测试
  const mockData = {
      "traceId": "review-3052779885888160836",
      "elapsedMs": 100,
      "docMeta": {
        "pages": 11,
        "paragraphs": 120
      },
      "results": [
        {
          "clauseType": "合同主体",
          "pointId": "3649",
          "algorithmType": "内部相对方名称规范核对",
          "decisionType": "首部未见内部相对方名称",
          "statusType": "ERROR",
          "message": "未在合同首部识别到内部相对方全称，建议补充营业执照一致的标准名称。",
          "actions": [
            { "actionID": "DEFAULT", "actionType": "HIGHLIGHT" }
          ],
          "evidence": [
            {
              "text": "甲方：  \n法定代表人 ：  \n住所地：",
              "page": 1,
              "paragraphIndex": 1,
              "startOffset": 0,
              "endOffset": 20
            }
          ]
        },
        {
          "clauseType": "合同主体",
          "pointId": "3649",
          "algorithmType": "内部相对方名称规范核对",
          "decisionType": "尾部内部相对方名称不规范",
          "statusType": "ERROR",
          "message": "尾部名称与登记信息不一致或缺少组织标识，建议统一至工商登记全称。",
          "actions": [
            { "actionID": "DEFAULT", "actionType": "HIGHLIGHT" }
          ],
          "evidence": [
            {
              "text": "| 甲方 (盖章)：  \n|法定代表人 （签字或盖章）：",
              "page": 11,
              "paragraphIndex": 100,
              "startOffset": 0,
              "endOffset": 40
            }
          ]
        },
        {
          "clauseType": "合同主体",
          "pointId": "3702",
          "algorithmType": "外部相对方名称规范核对",
          "decisionType": "外部相对方名称前后不一致",
          "statusType": "ERROR",
          "message": "首尾或条款内对方名称存在差异，建议统一到工商登记全称。",
          "actions": [
            { "actionID": "DEFAULT", "actionType": "HIGHLIGHT" }
          ],
          "evidence": [
            {
              "text": "乙方：山西肇新科技有限公司\n法定代表人：范忠\n住所地：太原市小店区平阳路 1号金茂大厦B座26层A2-B-C-D-E-F区",
              "page": 1,
              "paragraphIndex": 2,
              "startOffset": 0,
              "endOffset": 120
            },
            {
              "text": "|乙方 (盖章):   山西肇新科技有限公司",
              "page": 11,
              "paragraphIndex": 100,
              "startOffset": 0,
              "endOffset": 40
            }
          ]
        },
        {
          "clauseType": "法律条款引用",
          "pointId": "605",
          "algorithmType": "法规引用准确性核对",
          "decisionType": "法律全称引用核对",
          "statusType": "INFO"
        },
        {
          "clauseType": "法律条款引用",
          "pointId": "605",
          "algorithmType": "法规引用准确性核对",
          "decisionType": "法律简称引用核对",
          "statusType": "INFO"
        },
        {
          "clauseType": "法律条款引用",
          "pointId": "605",
          "algorithmType": "法规引用准确性核对",
          "decisionType": "仍引用已失效条文",
          "statusType": "INFO"
        },
        {
          "clauseType": "价款与支付",
          "pointId": "517",
          "algorithmType": "金额大小写一致性核验",
          "decisionType": "大小写金额不一致",
          "statusType": "ERROR",
          "message": "同一金额的中文大写与数字小写不一致，建议以书面约定的优先规则修正。",
          "actions": [
            { "actionID": "DEFAULT", "actionType": "HIGHLIGHT" }
          ],
          "evidence": [
            {
              "text": "本合同产品/服务合计金额为 ¥66000   元（含税，大写：人民币  壹拾壹万 圆整 ），税率【 1 %】，税额【】元。",
              "page": 7,
              "paragraphIndex": 60,
              "startOffset": 0,
              "endOffset": 120
            }
          ]
        },
        {
          "clauseType": "价款与支付",
          "pointId": "3521",
          "algorithmType": "含税/不含税约定核对",
          "decisionType": "含税价确认",
          "statusType": "WARNING",
          "message": "文本出现含税价表述，请确认税费口径、税率变化时的结算原则。",
          "actions": [
            { "actionID": "DEFAULT", "actionType": "HIGHLIGHT" }
          ],
          "evidence": [
            {
              "text": "本合同产品/服务合计金额为 ¥66000   元（含税，大写：人民币  壹拾壹万 圆整 ），税率【 1 %】，税额【】元。",
              "page": 7,
              "paragraphIndex": 60,
              "startOffset": 0,
              "endOffset": 120
            }
          ]
        },
        {
          "clauseType": "价款与支付",
          "pointId": "3549",
          "algorithmType": "税率约定核验",
          "decisionType": "税率缺失",
          "statusType": "WARNING",
          "message": "未明确适用税率，建议补充并注明随政策调整的处理方式。",
          "actions": [
            { "actionID": "DEFAULT", "actionType": "HIGHLIGHT" }
          ],
          "evidence": [
            {
              "text": "本合同产品/服务合计金额为 ¥66000   元（含税，大写：人民币  壹拾壹万 圆整 ），税率【 1 %】，税额【】元。",
              "page": 7,
              "paragraphIndex": 60,
              "startOffset": 0,
              "endOffset": 120
            }
          ]
        },
        {
          "clauseType": "价款与支付",
          "pointId": "704",
          "algorithmType": "支付节点与期限核对",
          "decisionType": "支付时间确认",
          "statusType": "INFO"
        },
        {
          "clauseType": "价款与支付",
          "pointId": "3524",
          "algorithmType": "支付方式与账户核对",
          "decisionType": "支付途径确认",
          "statusType": "INFO"
        },
        {
          "clauseType": "价款与支付",
          "pointId": "3523",
          "algorithmType": "发票类型约定核对",
          "decisionType": "发票类型缺失",
          "statusType": "WARNING",
          "message": "未明确发票类型，建议明确为专票或普票并注明开具条件。",
          "actions": [
            { "actionID": "DEFAULT", "actionType": "HIGHLIGHT" }
          ],
          "evidence": [
            {
              "text": "本合同产品/服务合计金额为 ¥66000   元（含税，大写：人民币  壹拾壹万 圆整 ），税率【 1 %】，税额【】元。",
              "page": 7,
              "paragraphIndex": 60,
              "startOffset": 0,
              "endOffset": 120
            }
          ]
        },
        {
          "clauseType": "价款与支付",
          "pointId": "3445",
          "algorithmType": "税率调整处理核对",
          "decisionType": "税收调整政策缺失",
          "statusType": "ERROR",
          "message": "未约定税率变化的处理原则，建议补充“价税分离/不变含税总价”等方案。",
          "actions": [
            { "actionID": "DEFAULT", "actionType": "HIGHLIGHT" }
          ],
          "evidence": [
            {
              "text": "本合同产品/服务合计金额为 ¥66000   元（含税，大写：人民币  壹拾壹万 圆整 ），税率【 1 %】，税额【】元。",
              "page": 7,
              "paragraphIndex": 60,
              "startOffset": 0,
              "endOffset": 120
            }
          ]
        }
      ]
    };

  // 模拟接口调用延迟
  await new Promise(resolve => setTimeout(resolve, 1000));

  try {
    // 直接使用模拟数据
    results.value = mockData.results;
    ElMessage.success('审核完成（模拟数据）');

    // 若当前不在编辑器视图、且已完成文件上传，则切换到编辑器以显示"风险审核"面板
    if (!showEditor.value && uploadedFileId.value) {
      showEditor.value = true;
    }

    // 审核完成后，处理定位锚点
    if (results.value && onlyofficeEditorRef.value) {
      const anchors = results.value.flatMap((item, itemIndex) =>
        (item.evidence || []).map((ev: any, evIndex: number) => ({
          anchorId: `${item.pointId}_${evIndex}`,
          text: ev.text,
          page: ev.page,
          paragraphIndex: ev.paragraphIndex,
          startOffset: ev.startOffset,
          endOffset: ev.endOffset,
        }))
      );
      await onlyofficeEditorRef.value.setAnchors(anchors);
    }
  } catch (error) {
    ElMessage.error('审核失败');
  } finally {
    reviewing.value = false;
  }

  /*
  // 真实的API调用逻辑（已注释）
  try {
    const response = await startFileReview(selectedFile.value, selectedTemplateId.value);
    if (response.code === 200 && response.data) {
      results.value = response.data.results;
      ElMessage.success('审核完成');

      // 审核完成后，处理定位锚点
      if (results.value && onlyofficeEditorRef.value) {
        const anchors = results.value.flatMap((item, itemIndex) =>
          (item.evidence || []).map((ev, evIndex) => ({
            anchorId: `${item.pointId}_${evIndex}`,
            text: ev.text,
            page: ev.page,
            paragraphIndex: ev.paragraphIndex,
            startOffset: ev.startOffset,
            endOffset: ev.endOffset,
          }))
        );
        await onlyofficeEditorRef.value.setAnchors(anchors);
      }
    } else {
      ElMessage.error(response.msg || '审核失败');
    }
  } catch (error) {
    console.error("审核失败:", error);
    ElMessage.error('审核请求异常，请查看控制台');
  } finally {
    reviewing.value = false;
  }
  */
};

function handleGoToAnchor(anchorId: string) {
  if (onlyofficeEditorRef.value) {
    onlyofficeEditorRef.value.gotoAnchor(anchorId);
    ElMessage.success(`正在定位到锚点: ${anchorId}`);
  }
}

function onStartClick() {
  if (!selectedFile.value) {
    ElMessage.warning('请先上传文件')
    return
  }
  if (!selectedProfileId.value && selectionPointIds.value.length === 0) {
    ElMessage.warning('请选择审核清单：可选方案，或从条款库勾选风险点')
    return
  }
  // 触发审核
  startReview()
}

function openSelectFromLibrary() { libVisible.value = true }

async function applySelectionFromLibrary() {
  if (!libRef.value || typeof libRef.value.getCheckedPointIds !== 'function') { libVisible.value = false; return }
  const ids: number[] = libRef.value.getCheckedPointIds()
  selectionPointIds.value = ids
  const res: any = await riskApi.previewSelection(ids)
  selectionPreview.value = res?.data || []
  libVisible.value = false
}

function openChecklistManager() { router.push({ path: '/risk-library', query: { from: 'contract-review' } }) }

async function saveSelectionAsProfile() {
  if (!selectionPointIds.value.length) return
  const code = 'p_' + Date.now()
  const name = selectionProfileName.value?.trim() || ('方案_' + new Date().toLocaleString())
  const res: any = await riskApi.createProfile({ profileCode: code, profileName: name, isDefault: false, description: '从选择生成' })
  const profile = res?.data
  if (profile?.id) {
    const items = selectionPreview.value.map((it: any, idx: number) => ({ profileId: profile.id, clauseTypeId: 0, pointId: it.pointId, sortOrder: idx + 1 }))
    await riskApi.saveProfileItems(profile.id, items)
    profiles.value = (await riskApi.listProfiles() as any).data || []
    selectedProfileId.value = profile.id
    selectionProfileName.value = ''
    ElMessage.success('已保存为方案')
  }
}

// 文档编辑器相关方法
async function openInEditor() {
  if (!selectedFile.value) {
    ElMessage.warning('请先上传文件')
    return
  }
  
  reviewing.value = true
  statusText.value = '正在上传文件并准备编辑器...'
  progress.value = 50

  // 如果已经上传过，直接显示编辑器
  if (uploadedFileId.value) {
    showEditor.value = true
    reviewing.value = false
    return
  }

  try {
    const res: any = await riskApi.uploadFileForReview(selectedFile.value as File)
    if (res.data && res.data.fileId) {
      uploadedFileId.value = res.data.fileId
      showEditor.value = true
      hasAudited.value = false
      progress.value = 100
      statusText.value = '文件准备就绪'
      ElMessage.success('文件已上传，正在加载编辑器...')
    } else {
      throw new Error('上传失败，无法获取文件ID')
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '文件上传失败')
    error.value = e?.message || '文件上传失败'
  } finally {
    reviewing.value = false
  }
}

function backToUpload() {
  showEditor.value = false
  editorReady.value = false
  // 保留hasAudited状态，这样返回后还能看到审核结果
}

function onEditorReady() {
  editorReady.value = true
  ElMessage.success('编辑器已就绪')
}

function onEditorError(error: any) {
  ElMessage.error('编辑器加载失败: ' + error.message)
  backToUpload()
}

function onDocumentStateChange(event: any) {
  // 文档状态变更处理
}

async function startAuditInEditor() {
  if (!editorReady.value) {
    ElMessage.warning('编辑器未就绪')
    return
  }
  
  if (!selectedProfileId.value && selectionPointIds.value.length === 0) {
    ElMessage.warning('请选择审核清单：可选方案，或从条款库勾选风险点')
    return
  }
  
  // 调用现有的审核逻辑
  await startReview()

  if (results.value.length > 0 && onlyofficeEditorRef.value) {
    const anchors = results.value.flatMap((item: any) => 
      item.evidence?.map((e: any, index: number) => ({
        anchorId: `${item.pointId}_${index}`,
        text: e.text,
        page: e.page,
        paragraphIndex: e.paragraphIndex,
        startOffset: e.startOffset,
        endOffset: e.endOffset,
      })) || []
    ).filter(Boolean);
    // 持久化：后端克隆文件并写入书签，返回新 fileId
    // 所见即所得：先在 OnlyOffice 中创建书签，再触发强制保存
    await onlyofficeEditorRef.value.setAnchors(anchors);
    await onlyofficeEditorRef.value.forceSave();
  }

  hasAudited.value = true
}

function getRiskLevel(statusType: string) {
  if (statusType === 'ERROR') return 'high'
  if (statusType === 'WARNING') return 'medium'
  return 'low'
}

function locateRisk(risk: any) {
  // 这里可以实现定位到文档具体位置的功能
  // 目前先显示提示信息
  ElMessage.info(`定位到风险项: ${risk.decisionType}`)
  
  // 未来可以通过OnlyOffice API实现具体定位
  // if (editorRef.value) {
  //   editorRef.value.locateToPosition(risk.position)
  // }
}

function exportAuditReport() {
  // 导出审核报告
  const reportData = {
    fileName: selectedFile.value?.name,
    auditTime: new Date().toLocaleString(),
    results: results.value,
    traceId: traceId.value
  }
  
  const blob = new Blob([JSON.stringify(reportData, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `审核报告_${selectedFile.value?.name}_${new Date().toISOString().slice(0, 10)}.json`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
  
  ElMessage.success('审核报告已导出')
}

function saveAuditResult() {
  // 保存审核结果到数据库
  ElMessage.success('审核结果已保存')
}
</script>

<style scoped>
/* 全局样式 */
.contract-review-page { padding: 20px; background-color: #f5f7fa; min-height: calc(100vh - 60px); }

/* 编辑器视图 */
.editor-view {
  display: flex;
  flex-direction: column;
}
.editor-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid #ebeef5;
}
.editor-file-name {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}
.editor-wrapper {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
}

/* 页面头部 */
.page-header-card { 
  margin-bottom: 20px; 
  border-radius: 8px; 
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05); 
  overflow: hidden;
  transition: all 0.3s ease;
}
.page-header-card:hover { box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1); }
.page-header { 
  padding: 16px 20px; 
  position: relative; 
  background: linear-gradient(135deg, var(--el-color-primary-light-7), var(--el-color-primary-light-9));
}
.header-content { position: relative; z-index: 2; }
.header-decoration { 
  position: absolute; 
  top: 0; 
  right: 0; 
  width: 150px; 
  height: 100%; 
  background: linear-gradient(135deg, transparent, var(--el-color-primary-light-5)); 
  opacity: 0.5;
  clip-path: polygon(100% 0, 0% 100%, 100% 100%);
}
.page-header h2 { 
  margin: 0; 
  font-size: 26px; 
  color: var(--el-color-primary-dark-2); 
  display: flex; 
  align-items: center;
  font-weight: 600;
}
.header-icon { 
  margin-right: 10px; 
  font-size: 24px; 
  color: var(--el-color-primary);
}
.page-header p { 
  margin: 10px 0 0; 
  color: #606266; 
  font-size: 15px; 
  max-width: 80%;
}

/* 主内容区 */
.main-content { margin-bottom: 20px; }
.main-content-card, .template-selector-card { 
  height: 100%; 
  border-radius: 8px; 
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05); 
  transition: all 0.3s ease;
}
.main-content-card:hover, .template-selector-card:hover { 
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1); 
}
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card-header-left h3 { margin: 0; font-weight: 600; color: var(--el-color-primary-dark-2); }
.subtitle { font-size: 14px; color: #909399; margin-top: 5px; }

/* 上传区域 */
.upload-area { 
  margin-bottom: 20px; 
  border: 2px dashed var(--el-color-primary-light-5); 
  border-radius: 8px; 
  transition: all 0.3s ease;
  background-color: var(--el-color-primary-light-9);
}
.upload-area:hover { 
  border-color: var(--el-color-primary); 
  background-color: var(--el-color-primary-light-8);
}
.upload-content { padding: 20px 0; }
.el-icon--upload { 
  font-size: 48px; 
  color: var(--el-color-primary); 
  margin-bottom: 10px;
  animation: pulse 2s infinite;
}
.el-upload__text { 
  font-size: 16px; 
  color: #606266; 
  margin-bottom: 8px;
}
.el-upload__text em { 
  color: var(--el-color-primary); 
  font-style: normal; 
  font-weight: 600;
  text-decoration: underline;
}
.upload-formats { 
  font-size: 13px; 
  color: #909399; 
  margin-top: 8px;
}

/* 文件信息 */
.file-info-actions { 
  display: flex; 
  justify-content: space-between; 
  align-items: center; 
  margin: 20px 0; 
  padding: 15px; 
  border: 1px solid var(--el-color-primary-light-7); 
  border-radius: 8px; 
  background-color: var(--el-color-primary-light-9);
  transition: all 0.3s ease;
}
.file-info-actions:hover { 
  border-color: var(--el-color-primary-light-3); 
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}
.file-info { 
  display: flex; 
  align-items: center; 
  gap: 12px; 
  color: #606266;
}
.file-icon { 
  font-size: 24px; 
  color: var(--el-color-primary);
}
.file-details { 
  display: flex; 
  flex-direction: column;
}
.file-name { 
  font-weight: 600; 
  color: #303133; 
  margin-bottom: 4px;
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.file-size { 
  font-size: 13px; 
  color: #909399;
}
.start-button { 
  padding: 12px 20px; 
  font-weight: 600; 
  transition: all 0.3s ease;
}
.start-button .el-icon { margin-right: 8px; }

/* 进度状态 */
.extracting-status { 
  margin-top: 20px; 
  padding: 15px; 
  border-radius: 8px; 
  background-color: var(--el-color-primary-light-9);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}
.status-text { 
  margin-top: 12px; 
  text-align: center; 
  color: var(--el-color-primary-dark-2); 
  font-weight: 600; 
  font-size: 15px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
.status-icon { 
  font-size: 18px; 
  color: var(--el-color-primary);
}
.rotating { 
  animation: rotate 1.5s linear infinite;
}
.error-message { margin-top: 20px; }

/* 结果区域 */
.result-area { 
  margin-top: 20px; 
  padding: 20px; 
  border-radius: 8px; 
  background-color: #fff; 
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  border: 1px solid var(--el-color-success-light-5);
}
.result-header { 
  display: flex; 
  justify-content: space-between; 
  align-items: center; 
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--el-color-success-light-5);
}
.result-header h4 { 
  margin: 0; 
  font-size: 18px; 
  color: var(--el-color-success-dark-2);
  display: flex;
  align-items: center;
  gap: 8px;
}
.result-icon { 
  color: var(--el-color-success);
}
.result-container { 
  margin-top: 15px; 
  max-height: 400px; 
  overflow-y: auto; 
  background-color: #f8f9fa; 
  border-radius: 6px; 
  border: 1px solid #ebeef5;
}
.result-text { 
  padding: 15px; 
  margin: 0; 
  font-family: 'Courier New', monospace; 
  font-size: 14px; 
  line-height: 1.5;
  color: #303133;
}

/* 模板选择器 */
.template-header { 
  display: flex; 
  justify-content: space-between; 
  align-items: center;
}
.template-header h4 { 
  margin: 0; 
  font-size: 18px; 
  color: var(--el-color-primary-dark-2);
  display: flex;
  align-items: center;
  gap: 8px;
}
.checklist-icon { 
  color: var(--el-color-primary);
}
.template-radio-group { 
  display: flex; 
  flex-direction: column; 
  gap: 10px; 
  margin: 10px 0;
}
.template-radio-item { 
  border-radius: 6px; 
  transition: all 0.3s ease;
}
.radio-content { 
  display: flex; 
  align-items: center; 
  justify-content: space-between; 
  width: 100%;
}
.template-tag { 
  margin-left: 8px; 
  font-size: 11px;
}
.actions-line { 
  display: flex; 
  gap: 8px; 
  margin: 15px 0;
  flex-wrap: wrap;
}
.profile-name-input { 
  width: 160px; 
  transition: all 0.3s ease;
}
.profile-name-input:focus { 
  width: 180px;
}
.divider-icon { 
  margin-right: 5px; 
  color: var(--el-color-primary);
}

/* 文档编辑器样式 */
/* .editor-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
} */

.editor-container {
  width: 95%;
  height: 95%;
  background-color: #fff;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  border-bottom: 1px solid #ebeef5;
  background-color: #f8f9fa;
}

.editor-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.editor-actions {
  display: flex;
  gap: 10px;
}

.editor-content {
  flex: 1;
  display: flex;
  position: relative;
}

.editor-placeholder {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #909399;
}

.placeholder-icon {
  font-size: 64px;
  margin-bottom: 20px;
  color: var(--el-color-primary-light-3);
}

.placeholder-note {
  font-size: 12px;
  margin-top: 10px;
}

/* 审核结果面板 */
/* .audit-results-panel {
  width: 300px;
  border-left: 1px solid #ebeef5;
  background-color: #f8f9fa;
  display: flex;
  flex-direction: column;
} */
/* 
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px;
  border-bottom: 1px solid #ebeef5;
}

.panel-header h4 {
  margin: 0;
  font-size: 14px;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 5px;
}

.panel-actions {
  display: flex;
  gap: 5px;
}

.results-list {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

.result-item {
  margin-bottom: 10px;
  padding: 10px;
  border-radius: 6px;
  border-left: 4px solid #dcdfe6;
  background-color: #fff;
}

.result-item.high {
  border-left-color: var(--el-color-danger);
}

.result-item.medium {
  border-left-color: var(--el-color-warning);
}

.result-item.low {
  border-left-color: var(--el-color-info);
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.result-type {
  font-size: 12px;
  font-weight: 600;
  color: #606266;
}

.result-content {
  margin-bottom: 8px;
}

.result-decision {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 4px 0;
}

.result-message {
  font-size: 12px;
  color: #606266;
  margin: 0;
  line-height: 1.4;
}

.result-actions {
  display: flex;
  justify-content: flex-end;
} */

/* 动画 */
@keyframes pulse {
  0% { transform: scale(1); }
  50% { transform: scale(1.1); }
  100% { transform: scale(1); }
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.mb10 { margin-bottom: 10px; }
.mr8 { margin-right: 8px; }
</style>
