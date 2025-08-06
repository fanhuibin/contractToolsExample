import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/layout/index.vue'),
    redirect: '/contracts',
    children: [
      {
        path: '/contracts',
        name: 'Contracts',
        component: () => import('@/views/contracts/index.vue'),
        meta: { title: '合同管理' }
      },
      {
        path: '/templates',
        name: 'Templates',
        component: () => import('@/views/templates/index.vue'),
        meta: { title: '模板管理' }
      },
      {
        path: '/documents',
        name: 'Documents',
        component: () => import('@/views/documents/index.vue'),
        meta: { title: '文档管理' }
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
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router 