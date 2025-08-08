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
        meta: { title: '模板在线设计', fullscreen: true }
      },
      {
        path: '/onlyoffice',
        name: 'OnlyOffice',
        component: () => import('@/views/onlyoffice/OnlyOfficeDemo.vue'),
        meta: { title: 'OnlyOffice演示' }
      },
      {
        path: '/contract-extract',
        name: 'ContractExtract',
        component: () => import('@/views/contracts/ContractExtract.vue'),
        meta: { title: '合同信息提取' }
      },
      {
        path: '/fulfillment',
        name: 'FulfillmentTask',
        component: () => import('@/views/contracts/FulfillmentTask.vue'),
        meta: { title: '合同履约任务' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router 