import axios from 'axios'

/**
 * 获取演示文档列表
 */
export function getDemoDocuments() {
  return axios.get('/api/demo/documents')
}

/**
 * 下载演示文档
 */
export function downloadDemoDocument(path) {
  return axios.get('/api/demo/documents/download', {
    params: { path },
    responseType: 'blob'
  })
}

