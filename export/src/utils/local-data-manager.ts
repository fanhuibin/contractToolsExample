/**
 * 本地数据管理器 - 替代后端API调用
 * 从本地JSON文件和图片文件中读取数据
 */

import type { 
  GPUOCRCompareTaskStatus, 
  GPUOCRCompareResult 
} from '@/api/gpu-ocr-compare'
import type { DocumentImageInfo } from '@/gpu-ocr-canvas/types'

// 声明全局变量类型
declare global {
  interface Window {
    TASK_STATUS_DATA: any
    COMPARE_RESULT_DATA: any
  }
}

export class LocalDataManager {
  private baseUrl = './data/current'

  /**
   * 从全局变量获取内嵌数据（避免file://协议CORS问题）
   */
  private getEmbeddedData(type: 'task-status' | 'compare-result'): any {
    if (type === 'task-status') {
      return window.TASK_STATUS_DATA
    } else if (type === 'compare-result') {
      return window.COMPARE_RESULT_DATA
    }
    throw new Error(`未知的数据类型: ${type}`)
  }

  /**
   * 获取任务状态
   */
  async getTaskStatus(): Promise<{ code: number; data: GPUOCRCompareTaskStatus; message: string }> {
    try {
      const data = this.getEmbeddedData('task-status')
      if (!data) {
        throw new Error('任务状态数据未找到')
      }
      console.log('Local data manager - task status loaded:', data)
      return { 
        code: 200, 
        data, 
        message: '获取任务状态成功' 
      }
    } catch (error) {
      console.error('获取任务状态失败:', error)
      throw new Error(`无法获取当前任务的状态信息`)
    }
  }

  /**
   * 获取比对结果
   */
  async getCompareResult(): Promise<{ code: number; data: any; message: string }> {
    try {
      const result = this.getEmbeddedData('compare-result')
      if (!result) {
        throw new Error('比对结果数据未找到')
      }
      
      // 设置固定的图片基路径供Vue组件使用
      result.oldImageBaseUrl = `./data/current/images/old`
      result.newImageBaseUrl = `./data/current/images/new`
      
      console.log('Local data manager - compare result loaded:', result)
      return { 
        code: 200, 
        data: result, 
        message: '获取比对结果成功' 
      }
    } catch (error) {
      console.error('获取比对结果失败:', error)
      throw new Error(`无法获取当前任务的比对结果`)
    }
  }

  /**
   * 获取文档图片信息
   */
  async getDocumentImages(mode: 'old' | 'new'): Promise<{ code: number; data: DocumentImageInfo; message: string }> {
    const result = await this.getCompareResult()
    const imageInfo = mode === 'old' ? result.data.oldImageInfo : result.data.newImageInfo
    
    if (!imageInfo) {
      throw new Error(`当前任务的 ${mode} 文档图片信息不存在`)
    }
    
    console.log(`Local data manager - ${mode} image info loaded:`, imageInfo)
    return { 
      code: 200, 
      data: imageInfo, 
      message: `获取${mode === 'old' ? '原' : '新'}文档图片信息成功` 
    }
  }

  /**
   * 构建图片URL
   */
  getImageUrl(mode: 'old' | 'new', pageNumber: number): string {
    return `${this.baseUrl}/images/${mode}/page-${pageNumber}.png`
  }

  /**
   * 检查图片是否存在
   */
  async checkImageExists(mode: 'old' | 'new', pageNumber: number): Promise<boolean> {
    try {
      const imageUrl = this.getImageUrl(mode, pageNumber)
      return new Promise((resolve) => {
        const xhr = new XMLHttpRequest()
        xhr.open('HEAD', imageUrl, true)
        xhr.onload = function() {
          resolve(xhr.status === 200 || xhr.status === 0)
        }
        xhr.onerror = function() {
          resolve(false)
        }
        xhr.send()
      })
    } catch {
      return false
    }
  }

  /**
   * 获取任务基本信息
   */
  async getTaskInfo(): Promise<any> {
    try {
      try {
        return await this.loadJsonFile(`${this.baseUrl}/task-info.json`)
      } catch {
        // 如果没有单独的task-info.json，从task-status.json获取基本信息
        return await this.loadJsonFile(`${this.baseUrl}/task-status.json`)
      }
    } catch (error) {
      console.error('获取任务信息失败:', error)
      throw new Error(`无法获取当前任务的基本信息`)
    }
  }
}

// 单例实例
export const localDataManager = new LocalDataManager()

// 导出默认实例
export default localDataManager
