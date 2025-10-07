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
        path: '/compare',
        name: 'CompareUpload',
        component: () => import('@/views/documents/Compare.vue'),
        meta: { title: 'PDF合同比对' }
      },
      {
        path: '/compare/result/:id',
        name: 'CompareResult',
        component: () => import('@/views/documents/CompareResult.vue'),
        meta: { title: 'PDF合同比对结果' }
      },
      {
        path: '/gpu-ocr-compare',
        name: 'GPUOCRCompare',
        component: () => import('@/views/documents/GPUOCRCompare.vue'),
        meta: { title: 'GPU OCR文档比对' }
      },
      {
        path: '/gpu-ocr-compare/canvas-result/:taskId',
        name: 'GPUOCRCanvasCompareResult',
        component: () => import('@/views/documents/GPUOCRCanvasCompareResult.vue'),
        meta: { title: 'GPU OCR文档比对结果 (Canvas版本)' }
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
        meta: { title: 'OnlyOffice演示' }
      },
      {
        path: '/auto-fulfillment',
        name: 'AutoFulfillment',
        component: () => import('@/views/contracts/AutoFulfillment.vue'),
        meta: { title: '自动履约任务' }
      },
      {
        path: '/contract-extract',
        name: 'ContractExtract',
        component: () => import('@/views/contracts/ContractExtract.vue'),
        meta: { title: '合同信息提取' }
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
      {
        path: '/info-extract',
        name: 'ContractInfoExtract',
        component: () => import('@/views/extract/ContractInfoExtract.vue'),
        meta: { title: '智能信息提取' }
      },
      {
        path: '/info-extract-enhanced',
        name: 'EnhancedContractInfoExtract',
        component: () => import('@/views/extract/EnhancedContractInfoExtract.vue'),
        meta: { title: '智能信息提取 - 增强版' }
      },
      
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router 