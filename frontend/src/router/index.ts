import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/layout/index.vue'),
    redirect: '/home',
    children: [
      {
        path: '/home',
        name: 'HomePage',
        component: () => import('@/views/home/HomePage.vue'),
        meta: { title: '首页', hideAside: true }
      },
      {
        path: '/smart-compose',
        name: 'SmartContractCompose',
        redirect: '/compose/start',
        meta: { title: '智能合同合成' }
      },
      {
        path: '/compose/start',
        name: 'ComposeStart',
        component: () => import('@/views/compose/ComposeStart.vue'),
        meta: { title: '智能合同合成' }
      },
      {
        path: '/templates/new',
        name: 'NewTemplate',
        component: () => import('@/views/templates/NewTemplate.vue'),
        meta: { title: '新建模板' }
      },
      {
        path: '/templates',
        name: 'TemplatesIndex',
        component: () => import('@/views/templates/TemplatesLibrary.vue'),
        meta: { title: '模板管理' }
      },
      {
        path: '/template-design',
        name: 'TemplateDesign',
        component: () => import('@/views/templates/TemplateDesign.vue'),
        meta: { title: '模板在线设计', fullscreen: true },
        beforeEnter: (to, _from, next) => {
          const defaultTemplateId = 'demo'
          const defaultFileId = '9999'
          const id = (to.query.id as string) || defaultTemplateId
          const fileId = (to.query.fileId as string) || defaultFileId
          if (!to.query.id || !to.query.fileId) {
            next({ path: to.path, query: { ...to.query, id, fileId } })
          } else {
            next()
          }
        }
      },
      {
        path: '/gpu-ocr-compare',
        name: 'GPUOCRCompare',
        component: () => import('@/views/documents/GPUOCRCompare.vue'),
        meta: { title: '智能文档比对' }
      },
      {
        path: '/gpu-ocr-compare/canvas-result/:taskId',
        name: 'GPUOCRCanvasCompareResult',
        component: () => import('@/views/documents/GPUOCRCanvasCompareResult.vue'),
        meta: { title: '智能文档比对结果' }
      },
      {
        path: '/template-design-demo',
        name: 'TemplateDesignDemo',
        component: () => import('@/views/templates/TemplateDesignDemo.vue'),
        meta: { title: '模板设计演示' }
      },
      {
        path: '/onlyoffice',
        name: 'OnlyOffice',
        component: () => import('@/views/onlyoffice/OnlyOfficeDemo.vue'),
        meta: { title: '文档在线编辑' }
      },
      {
        path: '/contract-extract',
        name: 'ContractExtract',
        component: () => import('@/views/contracts/ContractExtract.vue'),
        meta: { title: '合同信息提取' }
      },
      {
        path: '/document-convert',
        name: 'DocumentConvert',
        component: () => import('@/views/documents/DocumentConvert.vue'),
        meta: { title: '文档格式转换' }
      },
      {
        path: '/contract-review',
        name: 'ContractReview',
        component: () => import('@/views/contracts/ContractReview.vue'),
        meta: { title: '合同智能审核执行' }
      },
      {
        path: '/risk-library',
        name: 'RiskLibrary',
        component: () => import('@/views/contracts/RiskLibrary.vue'),
        meta: { title: '清单管理', hidden: true }
      },
      {
        path: '/rule-settings',
        name: 'RuleSettings',
        component: () => import('@/views/contracts/RuleSettings.vue'),
        meta: { title: '提取规则设置' }
      },
      {
        path: '/contract-compose-frontend',
        name: 'ContractComposeFrontend',
        component: () => import('@/views/compose/ContractComposeFrontend.vue'),
        meta: { title: '前端合成' }
      },
      {
        path: '/contract-compose-frontend/result/:fileId',
        name: 'ContractComposeFrontendResult',
        component: () => import('@/views/compose/ContractComposeFrontendResult.vue'),
        meta: { title: '前端合成结果' }
      },
      {
        path: '/contract-compose',
        name: 'ContractCompose',
        component: () => import('@/views/compose/ContractCompose.vue'),
        meta: { title: '合同合成' }
      },
      {
        path: '/contract-compose/result/:fileId',
        name: 'ContractComposeResult',
        component: () => import('@/views/compose/ContractComposeResult.vue'),
        meta: { title: '合同合成结果' }
      },
      {
        path: '/contract-compose/stamp-result',
        name: 'ContractComposeStampResult',
        component: () => import('@/views/compose/ContractComposeStampResult.vue'),
        meta: { title: '合同合成盖章结果' }
      },
      // 智能文档抽取模块（重构版本）
      {
        path: '/rule-extract',
        name: 'RuleExtract',
        component: () => import('@/views/rule-extract/ExtractMain.vue'),
        meta: { title: '智能文档抽取' }
      },
      {
        path: '/rule-extract/templates',
        name: 'RuleExtractTemplates',
        component: () => import('@/views/rule-extract/TemplateList.vue'),
        meta: { title: '抽取模板管理', keepAlive: true }
      },
      {
        path: '/rule-extract/template/:id',
        name: 'RuleExtractTemplateDesign',
        component: () => import('@/views/rule-extract/TemplateDesigner.vue'),
        meta: { title: '模板设计' }
      },
      {
        path: '/rule-extract/result/:taskId',
        name: 'RuleExtractResult',
        component: () => import('@/views/rule-extract/RuleExtractResult.vue'),
        meta: { title: '抽取结果' }
      },
      // 智能文档解析
      {
        path: '/ocr-extract',
        name: 'OcrExtract',
        component: () => import('@/views/ocr/OcrExtract.vue'),
        meta: { title: '智能文档解析' }
      },
      // 授权信息
      {
        path: '/license',
        name: 'LicenseView',
        component: () => import('@/views/license/LicenseView.vue'),
        meta: { title: '授权信息' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router 