<template>
  <DemoLayout
    category="compose"
    @doc-select="handleDemoDocSelect"
    @manage-template="handleManageTemplate"
  >
    <div class="compose-content-wrapper">
      <el-card class="main-card">
        <div class="steps-section">
          <el-steps :active="currentStep" align-center>
            <el-step title="选择演示" description="从左侧选择体验场景" />
            <el-step title="配置体验" description="调整参数并预览效果" />
            <el-step title="生成合同" description="下载体验文档" />
          </el-steps>
        </div>

        <!-- Step 0 -->
        <div v-if="currentStep === 0" class="step-content">
          <div class="content-body" :class="{ centered: !selectedScenario || scenarioLoading }">
            <div v-if="scenarioLoading" class="scenario-loading">
              <el-icon class="loading-icon is-loading">
                <Loading />
              </el-icon>
              <p class="loading-text">正在加载演示场景...</p>
            </div>
            <div v-else-if="!selectedScenario" class="select-template-placeholder">
              <el-icon :size="80" class="placeholder-icon">
                <FolderOpened />
              </el-icon>
              <div class="placeholder-text">
                <p class="main-text">请先选择一个演示场景</p>
                <p class="sub-text">左侧列出了四种典型的合同合成体验</p>
              </div>
            </div>

            <div v-else>
              <el-alert
                v-if="scenarioError"
                type="warning"
                :closable="false"
                class="scenario-alert"
              >
                <template #title>{{ scenarioError }}</template>
              </el-alert>
              <el-descriptions :title="selectedScenario.title" :column="2" border>
                <el-descriptions-item label="适用场景">
                  {{ selectedScenario.subtitle }}
                </el-descriptions-item>
                <el-descriptions-item label="模板名称">
                  {{ selectedTemplate?.templateName || '加载中' }}
                </el-descriptions-item>
                <el-descriptions-item label="亮点能力" :span="2">
                  <el-space wrap>
              <el-tag
                      v-for="feature in selectedScenario.highlights"
                      :key="feature"
                      type="success"
                    >
                      {{ feature }}
              </el-tag>
                  </el-space>
                </el-descriptions-item>
              </el-descriptions>

              <el-card class="scenario-brief" shadow="never">
                <template #header>
                  <div class="scenario-header">
                    <el-icon><Promotion /></el-icon>
                    <span>体验说明</span>
                  </div>
                </template>
                <p v-for="(tip, idx) in selectedScenario.description" :key="idx" class="scenario-tip">
                  {{ tip }}
                </p>
                <el-divider />
                <div class="scenario-ready">
                  <el-icon><MagicStick /></el-icon>
                  <span>准备就绪，可进入配置体验</span>
                </div>
              </el-card>
            </div>
          </div>

          <div class="step-actions">
            <el-button
              type="primary"
              :disabled="!selectedScenario || scenarioLoading"
              @click="goToNextStep"
            >
              下一步：配置体验
              <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>
        </div>

        <!-- Step 1 -->
        <div v-if="currentStep === 1" class="step-content">
          <template v-if="selectedScenario && selectedTemplate">
            <div class="scenario-config-body">
              <component
                :is="selectedScenario.component"
                :key="selectedDocId"
                :state="scenarioState"
                @update="updateScenarioState"
              />
          </div>

          <div class="step-actions">
            <el-button @click="goToPrevStep">
              <el-icon><ArrowLeft /></el-icon>
              上一步
            </el-button>
            <el-button
              type="primary"
              @click="handleGenerate"
              :loading="generating"
            >
              <el-icon><DocumentChecked /></el-icon>
              生成合同
            </el-button>
          </div>
          </template>

          <el-empty
            v-else
            description="未找到对应模板，请检查模板是否已发布"
          />
        </div>

        <!-- Step 2 -->
        <div v-if="currentStep === 2" class="step-content">
          <div class="content-body">
            <el-result
              icon="success"
              title="合同生成成功！"
              sub-title="您可以下载体验合同的不同版本"
            >
            <template #extra>
                <el-space direction="vertical" :size="20" style="width:100%;">
                <el-descriptions border :column="2">
                    <el-descriptions-item label="演示场景">
                      {{ selectedScenario?.title }}
                    </el-descriptions-item>
                    <el-descriptions-item label="生成时间">
                      {{ generateTime }}
                    </el-descriptions-item>
                    <el-descriptions-item label="可用版本">
                      {{ availableVersionsText }}
                    </el-descriptions-item>
                    <el-descriptions-item label="体验重点" :span="2">
                      <el-space direction="vertical" style="width:100%;">
                        <span v-for="item in scenarioComputedInfo.highlights" :key="item">
                          • {{ item }}
                        </span>
                      </el-space>
                    </el-descriptions-item>
                </el-descriptions>

                  <el-card shadow="never" class="download-card">
                  <template #header>
                      <div class="preview-header">
                        <el-icon><Promotion /></el-icon>
                        <span>下载体验版本</span>
                    </div>
                  </template>

                    <el-space direction="vertical" :size="12" style="width:100%;">
                    <div class="file-version-item">
                      <div class="version-info">
                        <el-icon color="#409EFF" :size="24"><Document /></el-icon>
                        <div class="version-details">
                          <div class="version-name">DOCX 原始文档</div>
                            <div class="version-desc">可编辑的 Word 合同</div>
                        </div>
                      </div>
                      <el-button
                        type="primary"
                        @click="handleDownload('docx')"
                        :loading="downloadingType === 'docx'"
                      >
                        <el-icon><Download /></el-icon>
                        下载 DOCX
                      </el-button>
                    </div>

                      <div
                        v-if="generateResult?.pdfPath"
                        class="file-version-item"
                      >
                      <div class="version-info">
                        <el-icon color="#67C23A" :size="24"><DocumentCopy /></el-icon>
                        <div class="version-details">
                            <div class="version-name">PDF 标准版</div>
                            <div class="version-desc">便于传阅与归档</div>
                        </div>
                      </div>
                      <el-button
                        type="success"
                        @click="handleDownload('pdf')"
                        :loading="downloadingType === 'pdf'"
                      >
                        <el-icon><Download /></el-icon>
                        下载 PDF
                      </el-button>
                    </div>

                      <div
                        v-if="generateResult?.stampedPdfPath"
                        class="file-version-item"
                      >
                      <div class="version-info">
                        <el-icon color="#E6A23C" :size="24"><Stamp /></el-icon>
                        <div class="version-details">
                            <div class="version-name">PDF 盖章版</div>
                            <div class="version-desc">展示公章用印效果</div>
                        </div>
                      </div>
                      <el-button
                        type="warning"
                        @click="handleDownload('stamped')"
                        :loading="downloadingType === 'stamped'"
                      >
                        <el-icon><Download /></el-icon>
                        下载盖章版
                      </el-button>
                    </div>

                      <div
                        v-if="generateResult?.ridingStampPdfPath"
                        class="file-version-item"
                      >
                      <div class="version-info">
                        <el-icon color="#F56C6C" :size="24"><Postcard /></el-icon>
                        <div class="version-details">
                            <div class="version-name">PDF 骑缝章版</div>
                            <div class="version-desc">展示附件合并与骑缝章</div>
                        </div>
                      </div>
                      <el-button
                        type="danger"
                        @click="handleDownload('riding')"
                        :loading="downloadingType === 'riding'"
                      >
                        <el-icon><Download /></el-icon>
                        下载骑缝章版
                      </el-button>
                    </div>
                  </el-space>
                </el-card>

                <el-space>
                    <el-button size="large" @click="startNewCompose">
                    <el-icon><Plus /></el-icon>
                      继续体验
                  </el-button>
                </el-space>
              </el-space>
            </template>
            </el-result>
          </div>
        </div>
      </el-card>
    </div>

    <IframeDialog
      v-model="templateDialogVisible"
      :url="templateManageUrl"
      title="模板管理"
      width="90%"
      @close="onTemplateDialogClose"
    />
  </DemoLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch, markRaw } from 'vue'
import {
  ArrowLeft,
  ArrowRight,
  Document,
  DocumentChecked,
  DocumentCopy,
  Download,
  FolderOpened,
  Loading,
  MagicStick,
  Postcard,
  Promotion,
  Stamp
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import DemoLayout from '@/components/DemoLayout.vue'
import IframeDialog from '@/components/IframeDialog.vue'
import {
  listComposeTemplates,
  getTemplateDetailById,
  getTemplateDetailByTemplateId,
  generateContract,
  downloadContract
} from '@/api/compose'
import { ZHAOXIN_CONFIG } from '@/config'
import axios from 'axios'
import ScenarioTextCompose from '@/components/compose/ScenarioTextCompose.vue'
import ScenarioTableCompose from '@/components/compose/ScenarioTableCompose.vue'
import ScenarioSealCompose from '@/components/compose/ScenarioSealCompose.vue'
import {
  stampLibrary,
  ridingStampLibrary,
  attachmentLibrary,
  pad2,
  formatDateTimeDisplay,
  futureDateString,
  todayCode,
  todayShortCode,
  timestampForFile,
  toTimestamp
} from '@/components/compose/shared.js'

/**
 * 预置字段说明：
 * - 演示模板采用 templateCode `caigou`
 * - ContentControl 标签统一以 tag 为唯一键
 */
const BASE_TEMPLATE_CODE = 'caigou'

const templateDialogVisible = ref(false)
const currentStep = ref(0)
const templates = ref([])
const loadingTemplates = ref(false)
const selectedTemplateId = ref('')
const selectedDocId = ref('')
const scenarioState = reactive({})
const generateResult = ref(null)
const generateTime = ref('')
const generating = ref(false)
const downloadingType = ref('')
const codeToTagMap = ref({}) // code -> tag 映射表

const scenarioComputedInfo = reactive({
  highlights: [],
  resources: []
})
const scenarioLoading = ref(false)
const scenarioError = ref('')

const getScenario1StyleConfig = (state = {}) => ({
  bold: Boolean(state.styleBold),
  italic: Boolean(state.styleItalic),
  underline: Boolean(state.styleUnderline),
  strike: Boolean(state.styleStrike),
  color: state.styleColor || '#303133',
  highlight: state.styleHighlight || '',
  fontSize: Number.isFinite(Number(state.styleFontSize)) ? Number(state.styleFontSize) : 16
})

const applyStyledText = (text, styleConfig) => {
  if (text === undefined || text === null) return ''
  const normalized = typeof text === 'number' ? String(text) : String(text)
  const decoration = []
  if (styleConfig.underline) decoration.push('underline')
  if (styleConfig.strike) decoration.push('line-through')

  const cssParts = []
  cssParts.push(`color: ${styleConfig.color}`)
  cssParts.push(`font-size: ${styleConfig.fontSize}pt`)
  cssParts.push(`font-weight: ${styleConfig.bold ? 'bold' : 'normal'}`)
  cssParts.push(`font-style: ${styleConfig.italic ? 'italic' : 'normal'}`)
  cssParts.push(`text-decoration: ${decoration.length ? decoration.join(' ') : 'none'}`)
  if (styleConfig.highlight) {
    cssParts.push(`background-color: ${styleConfig.highlight}`)
  }

  return `<span style="${cssParts.join('; ')}">${normalized}</span>`
}

const describeScenario1Style = (styleConfig) => {
  const features = []
  const emphasis = []

  if (styleConfig.bold) emphasis.push('加粗')
  if (styleConfig.italic) emphasis.push('斜体')
  if (styleConfig.underline) emphasis.push('下划线')
  if (styleConfig.strike) emphasis.push('删除线')
  if (emphasis.length) features.push(emphasis.join('、'))

  features.push(`颜色 ${styleConfig.color}`)
  if (styleConfig.highlight) {
    features.push(`高亮 ${styleConfig.highlight}`)
  }
  features.push(`字号 ${styleConfig.fontSize}pt`)

  return features.join('、')
}

/**
 * 场景配置定义
 */
const scenarioConfigs = {
  compose_demo_1: {
    title: '文字带样式合成',
    subtitle: '富文本样式一键组合',
    highlights: ['粗体 / 下划线 / 删除线', '自定义字体颜色', '高亮标注'],
    description: [
      '该场景演示如何通过按钮配置字体样式，生成富文本内容并写入模板。',
      '点击右侧按钮可随时查看段落预览效果，无需了解 HTML 语法。'
    ],
    component: markRaw(ScenarioTextCompose)
  },
  compose_demo_2: {
    title: '表格数据合成',
    subtitle: '多行明细动态填充',
    highlights: ['动态表格渲染', '表格样式自动匹配模板', '多套数据切换'],
    description: [
      '选择不同的采购清单，系统将自动生成标准化的表格结构。',
      '无需填写 JSON 字符串，演示明细行的增删以及金额小计。'
    ],
    component: markRaw(ScenarioTableCompose)
  },
  compose_demo_3: {
    title: '印章条款与附件',
    subtitle: '盖章、条款变量与附件合并',
    highlights: ['公章与骑缝章分离控制', '条款变量自动带入', '附加 PDF 合并'],
    description: [
      '勾选需要的公章与骑缝章，系统会按照模板字段定位完成盖章。',
      '可选择需要合并的附件，并在合成后于骑缝章版本中查看效果。'
    ],
    component: markRaw(ScenarioSealCompose)
  }
}

/**
 * 选中场景
 */
const selectedScenario = computed(() => {
  if (!selectedDocId.value) return null
  return scenarioConfigs[selectedDocId.value] || null
})

/**
 * 当前模板
 */
const selectedTemplate = computed(() => {
  if (!selectedTemplateId.value) return null
  return templates.value.find(item => item.id === selectedTemplateId.value) || null
})

/**
 * 可用版本文本
 */
const availableVersionsText = computed(() => {
  if (!generateResult.value) return '-'
  const versions = ['DOCX']
  if (generateResult.value.pdfPath) versions.push('PDF')
  if (generateResult.value.stampedPdfPath) versions.push('盖章版')
  if (generateResult.value.ridingStampPdfPath) versions.push('骑缝章版')
  return versions.join('、')
})

/**
 * 加载模板列表
 */
const loadTemplates = async () => {
  try {
    loadingTemplates.value = true
    const res = await listComposeTemplates({ status: 'PUBLISHED' })
    if (res.data.code === 200) {
      const outer = res.data.data
      const inner = outer && outer.data !== undefined ? outer.data : outer
      templates.value = Array.isArray(inner) ? inner : []
      } else {
      throw new Error(res.data.message || '加载模板失败')
    }
  } catch (error) {
    console.error('加载模板失败', error)
  } finally {
    loadingTemplates.value = false
  }
}

/**
 * 创建场景默认状态
 */
const initScenarioState = (docId) => {
  Object.keys(scenarioState).forEach(key => {
    delete scenarioState[key]
  })

  switch (docId) {
    case 'compose_demo_1':
      Object.assign(scenarioState, {
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
      })
      break
    case 'compose_demo_2':
      Object.assign(scenarioState, {
        datasetKey: 'standard',
        useCustomRows: false,
        customRows: [
          { name: '工业控制服务器', spec: '2U机架式 / 双路', origin: '上海', quantity: 3, price: 120000, total: 360000 },
          { name: '运维管理终端', spec: 'i7 / 32G / 1TB SSD', origin: '深圳', quantity: 10, price: 12000, total: 120000 },
          { name: '安全防护网关', spec: '千兆 / 双冗余', origin: '杭州', quantity: 2, price: 26000, total: 52000 }
        ],
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
        summaryTextColor: '#606266',
        projectName: '绝热材料采购项目',
        bidNumber: 'ZB-2025-001',
        contractCode: 'HT-2025-001',
        signLocation: '北京市',
        buyerName: '中铁隧道集团沈阳地铁二号线有限公司第十二合同段项目经理部',
        sellerName: '盘锦瑞普杜欣设备再制造技术开发合作联社'
      })
      break
    case 'compose_demo_3':
      Object.assign(scenarioState, {
        includeBuyerSeal: true,
        includeSellerSeal: true,
        includeRidingStamp: true,
        deliveryDate: futureDateString(25),
        penaltyRate: 0.6,
        selectedAttachments: ['techSpec', 'qualification'],
        sealSize: 100,
        ridingSealSize: 100,
        projectName: '绝热材料采购项目',
        bidNumber: 'ZB-2025-001',
        contractCode: 'HT-2025-001',
        signLocation: '北京市',
        acceptanceAmount: '300000.00',
        buyerName: '中铁隧道集团沈阳地铁二号线有限公司第十二合同段项目经理部',
        sellerName: '盘锦瑞普杜欣设备再制造技术开发合作联社'
      })
      break
    default:
      break
  }
  updateScenarioComputedInfo()
}

/**
 * 更新 computed info
 */
const updateScenarioComputedInfo = () => {
  scenarioComputedInfo.highlights = []
  scenarioComputedInfo.resources = []

  switch (selectedDocId.value) {
    case 'compose_demo_1':
      scenarioComputedInfo.highlights = [
        `采购项目名称：${scenarioState.projectName || '-'}`,
        `招标编号：${scenarioState.bidNumber || '-'}`,
        `买卖双方：${scenarioState.buyerName || '-'} / ${scenarioState.sellerName || '-'}`,
        `样式方案：${describeScenario1Style(getScenario1StyleConfig(scenarioState))}`
      ]
      break
    case 'compose_demo_2':
      scenarioComputedInfo.highlights = [
        scenarioState.useCustomRows ? '使用自定义表格数据' : `数据模板：${scenarioState.datasetKey === 'standard' ? '标准采购清单' : '二期扩展清单'}`,
        `表头对齐：${{ left: '居左', center: '居中', right: '居右' }[scenarioState.alignHeader || 'center']}`,
        `表格边框：标准实线，${scenarioState.stripe ? '已启用斑马纹' : '普通行样式'}`
      ]
      break
    case 'compose_demo_3':
      scenarioComputedInfo.highlights = [
        `公章控制：${[
          scenarioState.includeBuyerSeal ? '买方公章' : null,
          scenarioState.includeSellerSeal ? '卖方公章' : null
        ].filter(Boolean).join('、') || '未选择公章'}`,
        scenarioState.includeRidingStamp ? '将附件合并后盖骑缝章' : '不盖骑缝章',
        `条款变量：交付日期 ${scenarioState.deliveryDate}，违约金费率 ${scenarioState.penaltyRate.toFixed(1)}%`
      ]
      scenarioComputedInfo.resources = [
        ...(scenarioState.includeBuyerSeal ? ['买方公章图片'] : []),
        ...(scenarioState.includeSellerSeal ? ['卖方公章图片'] : []),
        ...(scenarioState.includeRidingStamp ? ['骑缝章图片'] : []),
        ...scenarioState.selectedAttachments.map(key => attachmentLibrary[key]?.label || key)
      ]
      break
    default:
      break
  }
}

/**
 * 生成 HTML 预览（场景 1）
 */
const buildStyledHtml = (state) => {
  const styleConfig = getScenario1StyleConfig(state)
  const richValues = {}

  const pushIfPresent = (key, value) => {
    if (value !== undefined && value !== null && value !== '') {
      richValues[key] = applyStyledText(value, styleConfig)
    }
  }

  pushIfPresent('base_projectName', state.projectName)
  pushIfPresent('base_bidNumber', state.bidNumber)
  pushIfPresent('base_contractCode', state.contractCode)
  pushIfPresent('base_signLocation', state.signLocation)
  pushIfPresent('base_acceptanceAmount', state.acceptanceAmount)
  pushIfPresent('party_a_name', state.buyerName)
  pushIfPresent('party_b_name', state.sellerName)

  return richValues
}

/**
 * 生成表格 HTML（场景 2）
 */
const buildTableHtml = (state) => {
  const datasets = {
    standard: [
      {
        name: '<b>工业控制服务器</b>',
        spec: '2U机架式 / 双路',
        origin: '上海',
        quantity: 3,
        price: 120000,
        total: 360000,
        nameStyle: 'font-weight: 600;'
      },
      {
        name: '运维管理终端',
        spec: 'i7 / 32G / 1TB SSD',
        origin: '深圳',
        quantity: 10,
        price: 12000,
        total: 120000
      },
      {
        name: '安全防护网关',
        spec: '千兆 / 双冗余',
        origin: '杭州',
        quantity: 2,
        price: 26000,
        total: 52000,
        rowStyle: 'background: #fffdf5;'
      }
    ],
    expansion: [
      {
        name: '中控一体机',
        spec: '55英寸 / 4K',
        origin: '北京',
        quantity: 4,
        price: 8900,
        total: 35600
      },
      {
        name: '应急备份服务器',
        spec: '4U / RAID10',
        origin: '南京',
        quantity: 2,
        price: 98000,
        total: 196000,
        priceStyle: 'color: #e6a23c;'
      },
      {
        name: '工程现场终端',
        spec: '防尘防水 IP65',
        origin: '重庆',
        quantity: 12,
        price: 5600,
        total: 67200
      },
      {
        name: '运维工具包',
        spec: '含线缆 / 工具箱',
        origin: '广州',
        quantity: 6,
        price: 1800,
        total: 10800,
        totalStyle: 'color: #67c23a; font-weight: 600;'
      }
    ]
  }

  const data = state.useCustomRows && Array.isArray(state.customRows) && state.customRows.length
    ? state.customRows
    : (datasets[state.datasetKey] || datasets.standard)

  const headerAlign = state.alignHeader || 'center'
  const emphasizeTotal = state.emphasizeTotal !== false
  const totalLabel = state.totalLabel || '合计金额'
  const headerBg = state.headerBg || '#f5f7fa'
  const headerColor = state.headerColor || '#303133'
  const stripe = state.stripe
  const stripeColor = state.stripeColor || 'rgba(64,158,255,0.04)'
  const bodyColor = state.bodyColor || '#303133'
  const bodyFontSize = (() => {
    if (typeof state.bodyFontSize === 'number') return `${state.bodyFontSize}px`
    if (typeof state.bodyFontSize === 'string' && /^\d+$/.test(state.bodyFontSize)) {
      return `${state.bodyFontSize}px`
    }
    return state.bodyFontSize || '13px'
  })()
  const cellPadding = state.cellPadding || '6px 8px'
  const priceColor = state.priceColor || '#409eff'
  const priceBold = state.priceBold !== false
  const totalBg = state.totalBg || (emphasizeTotal ? 'rgba(64,158,255,0.08)' : '#f5f7fa')
  const totalColor = state.totalColor || (emphasizeTotal ? '#1f64ff' : '#303133')
  const totalBold = state.totalBold !== false && emphasizeTotal
  const summaryBg = state.summaryBg || '#f5f7fa'
  const summaryTextColor = state.summaryTextColor || '#606266'

  const baseCellStyle = `padding: ${cellPadding}; font-size: ${bodyFontSize}; color: ${bodyColor}; border: 1px solid #dcdfe6;`
  const mergeStyle = (base, extra) => (extra ? `${base} ${extra}` : base)
  const seqCellStyle = `${baseCellStyle} text-align: center;`
  const textCellStyle = `${baseCellStyle} text-align: left;`
  const rightCellStyle = `${baseCellStyle} text-align: right;`
  const priceCellStyle = `${rightCellStyle} color: ${priceColor};${priceBold ? ' font-weight: 600;' : ''}`
  const totalCellBase = `${rightCellStyle} background: ${totalBg}; color: ${totalColor};${totalBold ? ' font-weight: 600;' : ''}`

  const tableRows = data
    .map((item, index) => {
      const rowStyles = []
      if (stripe && index % 2 === 1) {
        rowStyles.push(`background: ${stripeColor};`)
      }
      if (item.rowStyle) {
        rowStyles.push(item.rowStyle)
      }
      const rowStyleAttr = rowStyles.join(' ')

      return `
        <tr style="${rowStyleAttr}">
          <td style="${seqCellStyle}">${index + 1}</td>
          <td style="${mergeStyle(textCellStyle, item.nameStyle)}">${item.name || ''}</td>
          <td style="${mergeStyle(textCellStyle, item.specStyle)}">${item.spec || ''}</td>
          <td style="${mergeStyle(textCellStyle, item.originStyle)}">${item.origin || ''}</td>
          <td style="${mergeStyle(rightCellStyle, item.quantityStyle)}">${item.quantity ?? ''}</td>
          <td style="${mergeStyle(priceCellStyle, item.priceStyle)}">${numberFormat(item.price)} 元</td>
          <td style="${mergeStyle(priceCellStyle, item.totalStyle)}">${numberFormat(item.total)} 元</td>
        </tr>
      `
    })
    .join('')

  const totalAmount = data.reduce((sum, item) => sum + Number(item.total || 0), 0)

  const summaryRow = state.includeSummary && state.summaryText
    ? `
      <tr>
        <td colspan="7" style="${baseCellStyle} background: ${summaryBg}; color: ${summaryTextColor}; line-height: 1.6;">
          ${state.summaryText}
        </td>
      </tr>
    `
    : ''

  return `
    <table style="width: 100%; border-collapse: collapse; border: 1px solid #dcdfe6; font-size: 13px; color: #303133;">
      <thead>
        <tr style="background: ${headerBg}; color: ${headerColor};">
          <th style="padding: 8px 10px; border: 1px solid #dcdfe6; text-align: ${headerAlign};">序号</th>
          <th style="padding: 8px 10px; border: 1px solid #dcdfe6; text-align: ${headerAlign};">货物名称</th>
          <th style="padding: 8px 10px; border: 1px solid #dcdfe6; text-align: ${headerAlign};">规格型号</th>
          <th style="padding: 8px 10px; border: 1px solid #dcdfe6; text-align: ${headerAlign};">产地</th>
          <th style="padding: 8px 10px; border: 1px solid #dcdfe6; text-align: ${headerAlign};">数量</th>
          <th style="padding: 8px 10px; border: 1px solid #dcdfe6; text-align: ${headerAlign};">单价</th>
          <th style="padding: 8px 10px; border: 1px solid #dcdfe6; text-align: ${headerAlign};">合计</th>
        </tr>
      </thead>
      <tbody>
        ${tableRows}
        <tr>
          <td colspan="7" style="${totalCellBase}">
            ${totalLabel}：${numberFormat(totalAmount)} 元
          </td>
        </tr>
        ${summaryRow}
      </tbody>
    </table>
  `
}

/**
 * 条款预览（场景 3）
 */
const buildClausePreview = (state) => {
  return `
    <div style="font-size: 14px; line-height: 1.8em; color: #303133;">
      <p><strong>交付与违约条款示例</strong></p>
      <p>
        双方确认货物应于 <strong>${state.deliveryDate}</strong> 前完成交付；
        如遇延迟，将按合同总价的 <strong>${state.penaltyRate.toFixed(1)}%</strong> 支付违约金，且最迟不得晚于验收付款到账之日。
      </p>
      <p style="margin-top: 12px; color: #606266;">
        公章：${[
          state.includeBuyerSeal ? '买方公章' : null,
          state.includeSellerSeal ? '卖方公章' : null
        ].filter(Boolean).join('、') || '未选择'}；
        骑缝章：${state.includeRidingStamp ? '是' : '否'}；
        附件：${state.selectedAttachments
          .map(key => attachmentLibrary[key]?.label || key)
          .join('、') || '无'}。
      </p>
    </div>
  `
}

/**
 * 极速模式预览（场景 4）
 */
const buildSummaryPreview = (state) => {
  return `
    <div style="font-size: 14px; line-height: 1.8em;">
      <p><strong>项目：</strong>${state.projectName || '（未填写）'}</p>
      <p><strong>买方：</strong>${state.buyerName || '（未填写）'}</p>
      <p><strong>卖方：</strong>${state.sellerName || '（未填写）'}</p>
      <p><strong>合同金额：</strong>￥${state.totalAmount || '0.00'}</p>
      <p><strong>交付日期：</strong>${state.deliverDate || '（未填写）'}</p>
      <p style="margin-top: 12px; color: #606266;">
        所有其他字段将由系统自动填充，确保文档结构完整。
      </p>
    </div>
  `
}

/**
 * 更新场景状态
 */
const updateScenarioState = (payload) => {
  Object.assign(scenarioState, payload)
  updateScenarioComputedInfo()
}

/**
 * 将 code 转换为 tag
 */
const codeToTag = (code) => {
  return codeToTagMap.value[code] || code
}

/**
 * 将 values 对象的 key 从 code 转换为 tag
 */
const convertValuesToTags = (values) => {
  const result = {}
  Object.entries(values).forEach(([code, value]) => {
    const tag = codeToTag(code)
    result[tag] = value
  })
  return result
}

/**
 * 生成请求数据
 */
const buildComposePayload = () => {
  if (!selectedScenario.value) {
    throw new Error('请先选择演示场景')
  }

  // 使用 code 作为临时键构建基础值（仅包含模板中存在的字段）
  const baseValuesWithCode = {
    base_projectName: '绝热材料采购项目',
    base_contractCode: 'HT-2025-001',
    base_bidNumber: 'ZB-2025-001',
    base_signLocation: '北京市',
    party_a_name: '中铁隧道集团沈阳地铁二号线有限公司第十二合同段项目经理部',
    party_b_name: '盘锦瑞普杜欣设备再制造技术开发合作联社',
    base_acceptanceAmount: '300000.00'
  }

  const payload = {
    templateCode: BASE_TEMPLATE_CODE,
    values: convertValuesToTags(baseValuesWithCode)
  }

  switch (selectedDocId.value) {
    case 'compose_demo_1': {
      // 构建 code -> value 映射
      const codeValues = {
        base_projectName: scenarioState.projectName,
        base_bidNumber: scenarioState.bidNumber,
        base_contractCode: scenarioState.contractCode,
        base_signLocation: scenarioState.signLocation,
        base_acceptanceAmount: scenarioState.acceptanceAmount,
        party_a_name: scenarioState.buyerName,
        party_b_name: scenarioState.sellerName
      }
      // 转换为 tag -> value 并合并
      Object.assign(payload.values, convertValuesToTags(codeValues))

      // 如果有样式，应用样式化的 HTML（覆盖纯文本）
      const styledHtml = buildStyledHtml(scenarioState)
      Object.assign(payload.values, convertValuesToTags(styledHtml))
      break
    }
    case 'compose_demo_2': {
      const codeValues = {
        base_projectName: scenarioState.projectName ?? '绝热材料采购项目',
        base_bidNumber: scenarioState.bidNumber ?? 'ZB-2025-001',
        base_contractCode: scenarioState.contractCode ?? 'HT-2025-001',
        base_signLocation: scenarioState.signLocation ?? '北京市',
        party_a_name: scenarioState.buyerName ?? '中铁隧道集团沈阳地铁二号线有限公司第十二合同段项目经理部',
        party_b_name: scenarioState.sellerName ?? '盘锦瑞普杜欣设备再制造技术开发合作联社',
        base_productTable: buildTableHtml(scenarioState)
      }
      // 注意：base_totalAmount 和 base_remark 在当前模板中不存在，已移除
      Object.assign(payload.values, convertValuesToTags(codeValues))
      break
    }
    case 'compose_demo_3': {
      const codeValues = {
        base_projectName: scenarioState.projectName ?? '绝热材料采购项目',
        base_bidNumber: scenarioState.bidNumber ?? 'ZB-2025-001',
        base_contractCode: scenarioState.contractCode ?? 'HT-2025-001',
        base_signLocation: scenarioState.signLocation ?? '北京市',
        party_a_name: scenarioState.buyerName ?? '中铁隧道集团沈阳地铁二号线有限公司第十二合同段项目经理部',
        party_b_name: scenarioState.sellerName ?? '盘锦瑞普杜欣设备再制造技术开发合作联社',
        base_acceptanceAmount: scenarioState.acceptanceAmount ?? '300000.00',
        clause_customVariables: `双方确认货物应于${scenarioState.deliveryDate}前完成交付；如遇延迟，将按合同总价的${scenarioState.penaltyRate.toFixed(1)}%支付违约金，且最迟不得晚于${scenarioState.acceptanceAmount ?? baseValuesWithCode.base_acceptanceAmount}到账之日。`
      }
      // 注意：deliveryDate 和 delayPenaltyRate 不是独立字段，它们作为变量嵌入到 clause_customVariables 中
      Object.assign(payload.values, convertValuesToTags(codeValues))

      // 处理印章字段（stampImageUrls 的 key 也需要转换为 tag）
      const stampImageUrls = {}
      const sealSize = Number.isFinite(Number(scenarioState.sealSize)) ? Number(scenarioState.sealSize) : 100
      if (scenarioState.includeBuyerSeal) {
        const sealTag = codeToTag('seal_party_a')
        // ⚠️ 重要：印章字段必须在values中占位，后端才会注入marker
        payload.values[sealTag] = ''
        stampImageUrls[sealTag] = { 
          normal: stampLibrary.buyer,
          width: sealSize,
          height: sealSize
        }
      }
      if (scenarioState.includeSellerSeal) {
        const sealTag = codeToTag('seal_party_b')
        // ⚠️ 重要：印章字段必须在values中占位，后端才会注入marker
        payload.values[sealTag] = ''
        stampImageUrls[sealTag] = { 
          normal: stampLibrary.seller,
          width: sealSize,
          height: sealSize
        }
      }
      if (Object.keys(stampImageUrls).length > 0) {
        payload.stampImageUrls = stampImageUrls
      }

      if (scenarioState.includeRidingStamp) {
        const ridingSealSize = Number.isFinite(Number(scenarioState.ridingSealSize)) ? Number(scenarioState.ridingSealSize) : 100
        payload.ridingStampUrl = ridingStampLibrary.standard
        payload.ridingStampWidth = ridingSealSize
        payload.ridingStampHeight = ridingSealSize
      }

      if (Array.isArray(scenarioState.selectedAttachments) && scenarioState.selectedAttachments.length > 0) {
        payload.extraFiles = scenarioState.selectedAttachments
          .map(key => attachmentLibrary[key]?.url)
          .filter(Boolean)
      }
      break
    }
    default:
      break
  }

  return payload
}

/**
 * 计算表格总金额
 */
const calculateTableTotal = (state) => {
  if (state.useCustomRows && Array.isArray(state.customRows)) {
    return state.customRows.reduce((sum, row) => sum + Number(row.total || 0), 0).toFixed(2)
  }

  const datasets = {
    standard: [
      { total: 360000 },
      { total: 120000 },
      { total: 52000 }
    ],
    expansion: [
      { total: 35600 },
      { total: 196000 },
      { total: 67200 },
      { total: 10800 }
    ]
  }

  const data = datasets[state.datasetKey] || datasets.standard
  const total = data.reduce((sum, item) => sum + Number(item.total || 0), 0)
  return total.toFixed(2)
}

/**
 * 数字格式化
 */
const numberFormat = (value) => {
  if (value === undefined || value === null) return '0.00'
  return Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/**
 * 生成合同
 */
const handleGenerate = async () => {
  try {
    generating.value = true
    const requestData = buildComposePayload()
    console.log('📤 合成请求 (完整数据)', JSON.stringify(requestData, null, 2))
    console.log('📋 code -> tag 映射表', codeToTagMap.value)
    const res = await generateContract(requestData)
    if (res.data.code === 200) {
      const outer = res.data.data
      generateResult.value = outer && outer.data !== undefined ? outer.data : outer
      if (!generateResult.value || !generateResult.value.fileId) {
        throw new Error('未返回文件ID')
      }
      generateTime.value = formatDateTimeDisplay(new Date())
      currentStep.value = 2
    } else {
      throw new Error(res.data.message || '合成失败')
    }
  } catch (error) {
    console.error('合成失败', error)
    ElMessage.error(`生成失败：${error.message || '未知错误'}`)
  } finally {
    generating.value = false
  }
}

/**
 * 下载合同
 */
const handleDownload = async (type = 'docx') => {
  try {
    downloadingType.value = type
    let filePath
    let fileExt = 'docx'
    switch (type) {
      case 'docx':
        filePath = generateResult.value.fileId
        fileExt = 'docx'
        break
      case 'pdf':
        filePath = generateResult.value.pdfPath
        fileExt = 'pdf'
        break
      case 'stamped':
        filePath = generateResult.value.stampedPdfPath
        fileExt = 'pdf'
        break
      case 'riding':
        filePath = generateResult.value.ridingStampPdfPath
        fileExt = 'pdf'
        break
      default:
        throw new Error('未知文件类型')
    }

    if (!filePath) {
      throw new Error('文件路径不存在')
    }

    const fileName = `${selectedScenario.value.title}_${timestampForFile()}.${fileExt}`
    let res
    if (type === 'docx') {
      res = await downloadContract(filePath, fileName)
    } else {
      res = await axios.get('/api/compose/download-by-path', {
        params: { path: filePath },
        responseType: 'blob'
      })
    }

    const url = window.URL.createObjectURL(new Blob([res.data]))
    const link = document.createElement('a')
    link.href = url
    link.download = fileName
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  } catch (error) {
    console.error('下载失败', error)
    ElMessage.error(`下载失败：${error.message || '未知错误'}`)
  } finally {
    downloadingType.value = ''
  }
}

/**
 * 步骤导航
 */
const goToNextStep = () => {
  if (currentStep.value < 2) currentStep.value += 1
}

const goToPrevStep = () => {
  if (currentStep.value > 0) currentStep.value -= 1
}

/**
 * 重置体验
 */
const startNewCompose = () => {
  currentStep.value = 0
  selectedDocId.value = ''
  selectedTemplateId.value = ''
  generateResult.value = null
  generateTime.value = ''
  Object.keys(scenarioState).forEach(key => delete scenarioState[key])
  updateScenarioComputedInfo()
}

/**
 * 模板管理
 */
const handleManageTemplate = () => {
  templateDialogVisible.value = true
}

const onTemplateDialogClose = () => {
  loadTemplates()
}

const templateManageUrl = computed(() => {
  const base = (ZHAOXIN_CONFIG.customFieldsBaseUrl || ZHAOXIN_CONFIG.demoBaseUrl || window.location.origin || '').replace(/\/$/, '')
  const fieldsConfigEndpoint = `${base}/api/custom-fields/config?configId=insulation_material_purchase_contract`
  const encoded = encodeURIComponent(fieldsConfigEndpoint)
  return `${ZHAOXIN_CONFIG.frontendUrl}/templates?fieldsConfigUrl=${encoded}&fieldsConfigId=insulation_material_purchase_contract`
})

/**
 * 解析模板的 elementsJson，构建 code -> tag 映射
 */
const buildCodeToTagMap = (template) => {
  try {
    if (!template || !template.elementsJson) {
      console.warn('⚠️ 模板或 elementsJson 不存在')
      return {}
    }

    const parsed = typeof template.elementsJson === 'string'
      ? JSON.parse(template.elementsJson)
      : template.elementsJson

    const elements = Array.isArray(parsed) ? parsed : (parsed.elements || [])

    console.log('📋 解析到的 elements 数量:', elements.length)

    const map = {}
    elements.forEach((el, index) => {
      // code 在 meta.code 中，tag 在根级别的 tag 字段
      const code = el.meta?.code || el.code
      const tag = el.tag

      if (code && tag) {
        map[code] = tag
        console.log(`  [${index}] ${code} -> ${tag}`)
      } else {
        console.warn(`  [${index}] 跳过：缺少 code 或 tag`, { code, tag, el })
      }
    })

    console.log('📋 构建 code -> tag 映射完成，共 ' + Object.keys(map).length + ' 个映射')
    return map
  } catch (error) {
    console.error('❌ 解析 elementsJson 失败', error)
    return {}
  }
}

/**
 * 选择演示文档
 */
const handleDemoDocSelect = async (doc) => {
  // ===== 第一步：立即更新 UI，不阻塞界面 =====
  selectedDocId.value = doc.id
  initScenarioState(doc.id)
  updateScenarioComputedInfo()
  currentStep.value = 0
  scenarioError.value = ''
  scenarioLoading.value = true

  // ===== 第二步：使用 nextTick 确保界面先渲染，然后异步加载数据 =====
  await new Promise(resolve => setTimeout(resolve, 0)) // 让出主线程，确保 UI 更新

  try {
    // 异步加载模板列表（如果需要）
    if (!templates.value.length) {
      await loadTemplates()
    }

    const matched = templates.value
      .filter(item => item.templateCode === (doc.templateCode || BASE_TEMPLATE_CODE))
      .sort((a, b) => {
        return toTimestamp(b.updatedAt) - toTimestamp(a.updatedAt)
      })[0]

    if (matched) {
      selectedTemplateId.value = matched.id
      let detail = null

      // 获取模板详情以获取 elementsJson
      try {
        if (matched.id) {
          const detailRes = await getTemplateDetailById(matched.id)
          console.log('📄 原始响应', detailRes.data)

          // 处理双层嵌套：Demo后端包了一层 ApiResponse，主系统也包了一层
          if (detailRes.data.code === 200 && detailRes.data.data) {
            const innerData = detailRes.data.data
            // 如果 innerData 还有 data 字段，说明是主系统的响应格式
            if (innerData.data) {
              detail = innerData.data
            } else {
              detail = innerData
            }
          } else {
            console.warn('按记录ID获取模板详情失败', detailRes.data.message)
          }
        }

        if (!detail && matched.templateId) {
          const detailByTemplateIdRes = await getTemplateDetailByTemplateId(matched.templateId)
          if (detailByTemplateIdRes.data.code === 200 && detailByTemplateIdRes.data.data) {
            const innerData = detailByTemplateIdRes.data.data
            if (innerData.data) {
              detail = innerData.data
            } else {
              detail = innerData
            }
          } else {
            console.warn('按模板ID获取模板详情失败', detailByTemplateIdRes.data.message)
          }
        }
      } catch (error) {
        console.error('获取模板详情异常', error)
        throw error // 向外抛出，统一处理
      }

      const templateDetail = detail || matched
      console.log('📄 解析后的模板详情', templateDetail)
      console.log('📄 elementsJson', templateDetail?.elementsJson)
      codeToTagMap.value = buildCodeToTagMap(templateDetail)
      scenarioError.value = ''
    } else {
      selectedTemplateId.value = ''
      codeToTagMap.value = {}
      scenarioError.value = '未找到已发布的演示模板，请检查模板状态'
      console.warn(scenarioError.value)
    }
  } catch (error) {
    scenarioError.value = '加载演示场景失败：' + (error.message || '未知错误')
    console.error('handleDemoDocSelect 错误:', error)
  } finally {
    scenarioLoading.value = false
  }
}

onMounted(() => {
  loadTemplates()
})
</script>

<style scoped lang="scss">
@import '@/styles/demo-common.scss';

.compose-content-wrapper {
  padding: $spacing-lg;
  height: 100%;
  overflow-y: auto;

  .main-card {
    @include main-card;
    max-width: 1200px;

      .step-content {
      margin-top: 0;
        padding: 0;

        .content-body {
          padding: 32px 40px;
          &.centered {
            display: flex;
            align-items: center;
            justify-content: center;
          min-height: 320px;
        }
      }

      .scenario-config-body {
        display: block;
        padding: 32px 40px;

        .config-card {
          width: 100%;
          max-width: 560px;
          }
        }

        .select-template-placeholder {
          display: flex;
          flex-direction: column;
          align-items: center;
        gap: 18px;
          padding: 60px 40px;
          border: 2px dashed #dcdfe6;
          border-radius: 12px;
          width: 100%;

          .placeholder-icon {
            color: #909399;
          }

            .main-text {
              font-size: 18px;
              color: #303133;
          margin: 0;
              font-weight: 500;
            }

            .sub-text {
              font-size: 14px;
              color: #909399;
              margin: 0;
            }
          }

        .scenario-loading {
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          gap: 12px;
          padding: 60px 40px;
          color: #606266;

          .loading-icon {
            font-size: 36px;
            color: #409eff;
          }

          .loading-text {
            margin: 0;
            font-size: 14px;
          }
        }

        .scenario-alert {
          margin-bottom: 16px;
        }

      .scenario-brief {
        margin-top: 24px;

        .scenario-tip {
          margin: 0 0 8px 0;
          color: #606266;
          line-height: 1.6;
        }

        .scenario-ready {
          display: flex;
          align-items: center;
          gap: 8px;
          color: #1f64ff;
          font-weight: 500;
        }
        }

      .scenario-header {
          display: flex;
          align-items: center;
        gap: 8px;
        font-weight: 600;
        color: #303133;
      }

      .download-card {
        background: #f5f7fa;
        }

        .file-version-item {
          display: flex;
          align-items: center;
          justify-content: space-between;
        padding: 14px;
          background: white;
          border-radius: 8px;
          border: 1px solid #e4e7ed;

          .version-info {
            display: flex;
            align-items: center;
            gap: 12px;

              .version-name {
                font-size: 16px;
                font-weight: 500;
                color: #303133;
              }

              .version-desc {
                font-size: 13px;
                color: #909399;
          }
        }
      }
    }

    .step-actions {
      @include action-buttons-section;
      padding: 24px 40px;
    }
  }
}

@media (max-width: 1024px) {
  .compose-content-wrapper {
    padding: 16px;

      .main-card {
        .step-content {
        .scenario-config-body {
            flex-direction: column;

          .config-card {
              width: 100%;
          }
        }
      }
    }
  }
}
</style>

