import baseRequest from '@/utils/request'

// === AI 返回格式（前端强约束）===
export interface Evidence {
  text?: string
  page?: number
  paragraphIndex?: number
  startOffset?: number
  endOffset?: number
}

export interface RiskItem {
  clauseType: string
  pointId: string | number
  algorithmType: string
  decisionType: string
  statusType: 'ERROR' | 'WARNING' | 'INFO' | string
  message?: string
  actions?: any[]
  evidence: Evidence[]
}

export interface AiReviewResponse {
  traceId?: string
  elapsedMs?: number
  docMeta?: any
  results: RiskItem[]
}

// 提示后端/AI：必须返回包含 evidence 的锚点信息，优先 paragraphIndex/startOffset/endOffset
export const AI_SCHEMA_HINT: string = JSON.stringify({
  version: '1.0',
  required: ['traceId', 'elapsedMs', 'docMeta', 'results'],
  results: {
    item: {
      required: ['clauseType', 'pointId', 'algorithmType', 'decisionType', 'statusType', 'evidence'],
      evidenceItem: {
        // 建议：提供段内精确定位三元组；text 用作兜底搜索
        optional: ['text', 'page', 'paragraphIndex', 'startOffset', 'endOffset']
      }
    }
  },
  anchors: 'Each evidence corresponds to an anchor. Prefer paragraphIndex/startOffset/endOffset; include text as fallback.'
})

export const riskApi = {
  getClauseTypes(enabled?: boolean) {
    const query = enabled === undefined ? '' : `?enabled=${enabled ? 1 : 0}`
    return baseRequest.get(`/ai/review-lib/clause-types${query}`)
  },
  getPointsByClause(clauseTypeId: number, keyword?: string, enabled?: boolean) {
    const params: string[] = []
    if (keyword) params.push(`keyword=${encodeURIComponent(keyword)}`)
    if (enabled !== undefined) params.push(`enabled=${enabled ? 1 : 0}`)
    const q = params.length ? `?${params.join('&')}` : ''
    return baseRequest.get(`/ai/review-lib/clause/${clauseTypeId}/points${q}`)
  },
  getTree(enabled?: boolean) {
    const query = enabled === undefined ? '' : `?enabled=${enabled ? 1 : 0}`
    return baseRequest.get(`/ai/review-lib/tree${query}`)
  },
  getTreePrompts(enabled?: boolean) {
    const query = enabled === undefined ? '' : `?enabled=${enabled ? 1 : 0}`
    return baseRequest.get(`/ai/review-lib/tree-prompts${query}`)
  },
  previewSelection(pointIds: number[]) {
    return baseRequest.post('/ai/review-lib/selection/preview', pointIds)
  },
  // ---- manage: clause types ----
  createClauseType(data: any) { return baseRequest.post('/ai/review-lib/clause-type', data) },
  updateClauseType(id: number, data: any) { return baseRequest.put(`/ai/review-lib/clause-type/${id}`, data) },
  deleteClauseType(id: number, force?: boolean) { return baseRequest.delete(`/ai/review-lib/clause-type/${id}${force ? '?force=1' : ''}`) },
  enableClauseType(id: number, value: boolean) { return baseRequest.patch(`/ai/review-lib/clause-type/${id}/enabled?value=${value ? 1 : 0}`) },
  reorderClauseTypes(items: Array<{id:number, sortOrder:number}>) { return baseRequest.put('/ai/review-lib/clause-types/reorder', items) },

  // ---- manage: points ----
  createPoint(data: any) { return baseRequest.post('/ai/review-lib/point', data) },
  updatePoint(id: number, data: any) { return baseRequest.put(`/ai/review-lib/point/${id}`, data) },
  deletePoint(id: number, force?: boolean) { return baseRequest.delete(`/ai/review-lib/point/${id}${force ? '?force=1' : ''}`) },
  enablePoint(id: number, value: boolean) { return baseRequest.patch(`/ai/review-lib/point/${id}/enabled?value=${value ? 1 : 0}`) },
  reorderPoints(clauseTypeId: number, items: Array<{id:number, sortOrder:number}>) { return baseRequest.put(`/ai/review-lib/points/reorder?clauseTypeId=${clauseTypeId}`, items) },

  // ---- manage: prompts ----
  listPrompts(pointId: number) { return baseRequest.get(`/ai/review-lib/point/${pointId}/prompts`) },
  createPrompt(data: any) { return baseRequest.post('/ai/review-lib/prompt', data) },
  updatePrompt(id: number, data: any) { return baseRequest.put(`/ai/review-lib/prompt/${id}`, data) },
  deletePrompt(id: number, force?: boolean) { return baseRequest.delete(`/ai/review-lib/prompt/${id}${force ? '?force=1' : ''}`) },
  enablePrompt(id: number, value: boolean) { return baseRequest.patch(`/ai/review-lib/prompt/${id}/enabled?value=${value ? 1 : 0}`) },
  reorderPrompts(pointId: number, items: Array<{id:number, sortOrder:number}>) { return baseRequest.put(`/ai/review-lib/prompts/reorder?pointId=${pointId}`, items) },

  // ---- manage: actions ----
  listActions(promptId: number) { return baseRequest.get(`/ai/review-lib/prompt/${promptId}/actions`) },
  createAction(data: any) { return baseRequest.post('/ai/review-lib/action', data) },
  updateAction(id: number, data: any) { return baseRequest.put(`/ai/review-lib/action/${id}`, data) },
  deleteAction(id: number) { return baseRequest.delete(`/ai/review-lib/action/${id}`) },
  enableAction(id: number, value: boolean) { return baseRequest.patch(`/ai/review-lib/action/${id}/enabled?value=${value ? 1 : 0}`) },
  reorderActions(promptId: number, items: Array<{id:number, sortOrder:number}>) { return baseRequest.put(`/ai/review-lib/actions/reorder?promptId=${promptId}`, items) },

  // ---- manage: profiles ----
  listProfiles() { return baseRequest.get('/ai/review-lib/profiles') },
  createProfile(data: any) { return baseRequest.post('/ai/review-lib/profile', data) },
  updateProfile(id: number, data: any) { return baseRequest.put(`/ai/review-lib/profile/${id}`, data) },
  deleteProfile(id: number, force?: boolean) {
    const q = force ? '?force=1' : ''
    return baseRequest.delete(`/ai/review-lib/profile/${id}${q}`)
  },
  setDefaultProfile(id: number, value: boolean) { return baseRequest.patch(`/ai/review-lib/profile/${id}/default?value=${value ? 1 : 0}`) },
  listProfileItems(id: number) { return baseRequest.get(`/ai/review-lib/profile/${id}/items`) },
  saveProfileItems(id: number, items: any[]) { return baseRequest.post(`/ai/review-lib/profile/${id}/items`, items) }
  ,
  // ---- execute review (placeholder backend)
  // 必须携带文件（强制AI分支）
  executeReview(profileId?: number, pointIds?: number[], file?: File) {
    const url = profileId ? `/ai/review-lib/review/execute?profileId=${profileId}` : '/ai/review-lib/review/execute'
    const form = new FormData()
    if (!file) throw new Error('缺少审核文件')
    form.append('file', file)
    if (pointIds && pointIds.length) {
      form.append('pointIds', new Blob([JSON.stringify(pointIds)], { type: 'application/json' }))
    }
    return baseRequest.post(url, form, { headers: { 'Content-Type': 'multipart/form-data' } })
  },

  /**
   * 上传文件以供智能审核使用
   * @param file 文件对象
   */
  uploadFileForReview(file: File) {
    const formData = new FormData()
    formData.append('file', file)
    
    // baseRequest has baseURL '/api', so here MUST NOT prefix with '/api' again
    return baseRequest.post('/review/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  }
}

export default riskApi


