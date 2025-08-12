import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/layout/index.vue'),
    redirect: '/onlyoffice',
    children: [
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
        path: '/rule-settings',
        name: 'RuleSettings',
        component: () => import('@/views/contracts/RuleSettings.vue'),
        meta: { title: '提取规则设置' }
      },
      
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router 