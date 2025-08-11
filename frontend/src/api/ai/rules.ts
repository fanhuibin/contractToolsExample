import request from '@/utils/request'

// New API by templateId (query param variant)
export function readRuleByTemplateId(templateId: number) {
  return request({ url: `/ai/rules/by-template`, method: 'get', params: { templateId } })
}

export function saveRuleByTemplateId(templateId: number, content: any) {
  return request({ url: `/ai/rules/by-template`, method: 'put', params: { templateId }, data: content })
}


