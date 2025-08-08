import request from '@/utils/request'

export function listModels() {
  return request({ url: '/ai/rules/models', method: 'get' })
}

export function readRule(contractType: string) {
  return request({ url: `/ai/rules/${contractType}`, method: 'get' })
}

export function saveRule(contractType: string, content: any) {
  return request({ url: `/ai/rules/${contractType}`, method: 'put', data: content })
}


