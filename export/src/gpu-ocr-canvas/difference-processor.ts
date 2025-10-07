/**
 * 差异数据预处理器
 * 将原始差异数据按页面分组，并进行去重处理
 */

import type { DifferenceItem } from './types'

/**
 * 预处理后的差异项
 */
export interface ProcessedDifferenceItem extends DifferenceItem {
  singleBbox: number[]
  bboxIndex: number
}

/**
 * 预处理差异数据，按页面分组bbox并去重
 * @param differences 原始差异数据
 * @returns 按页面分组的差异数据 Map<pageNum, ProcessedDifferenceItem[]>
 */
export function preprocessDifferences(differences: DifferenceItem[]): Map<number, ProcessedDifferenceItem[]> {
  const pageMap = new Map<number, ProcessedDifferenceItem[]>()
  
  differences.forEach(diff => {
    if (diff.operation === 'DELETE') {
      // DELETE操作：处理原文档页面
      let bboxes: number[][] = []
      let pageNums: number[] = []
      
      // 优先使用数组形式的数据
      if (diff.pageAList && diff.oldBboxes) {
        bboxes = diff.oldBboxes
        pageNums = diff.pageAList
      } 
      // 如果没有数组数据，使用单个数据
      else if (diff.oldBbox && (diff.pageA || diff.page)) {
        bboxes = [diff.oldBbox]
        pageNums = [diff.pageA || diff.page || 1]
      }
      
      bboxes.forEach((bbox: number[], index: number) => {
        const pageNum = pageNums[index]
        if (pageNum && bbox && bbox.length >= 4) {
          if (!pageMap.has(pageNum)) pageMap.set(pageNum, [])
          
          // 检查是否已存在完全相同的bbox（去重）
          const existing = pageMap.get(pageNum)!
          const isDuplicate = existing.some(item => 
            item.singleBbox && 
            item.singleBbox[0] === bbox[0] && 
            item.singleBbox[1] === bbox[1] && 
            item.singleBbox[2] === bbox[2] && 
            item.singleBbox[3] === bbox[3]
          )
          
          if (!isDuplicate) {
            pageMap.get(pageNum)!.push({
              ...diff,
              singleBbox: bbox,
              bboxIndex: index
            })
          }
        }
      })
    } else if (diff.operation === 'INSERT') {
      // INSERT操作：处理新文档页面
      let bboxes: number[][] = []
      let pageNums: number[] = []
      
      // 优先使用数组形式的数据
      if (diff.pageBList && diff.newBboxes) {
        bboxes = diff.newBboxes
        pageNums = diff.pageBList
      } 
      // 如果没有数组数据，使用单个数据
      else if (diff.newBbox && (diff.pageB || diff.page)) {
        bboxes = [diff.newBbox]
        pageNums = [diff.pageB || diff.page || 1]
      }
      
      bboxes.forEach((bbox: number[], index: number) => {
        const pageNum = pageNums[index]
        if (pageNum && bbox && bbox.length >= 4) {
          if (!pageMap.has(pageNum)) pageMap.set(pageNum, [])
          
          // 检查是否已存在完全相同的bbox（去重）
          const existing = pageMap.get(pageNum)!
          const isDuplicate = existing.some(item => 
            item.singleBbox && 
            item.singleBbox[0] === bbox[0] && 
            item.singleBbox[1] === bbox[1] && 
            item.singleBbox[2] === bbox[2] && 
            item.singleBbox[3] === bbox[3]
          )
          
          if (!isDuplicate) {
            pageMap.get(pageNum)!.push({
              ...diff,
              singleBbox: bbox,
              bboxIndex: index
            })
          }
        }
      })
    }
  })
  
  
  return pageMap
}
