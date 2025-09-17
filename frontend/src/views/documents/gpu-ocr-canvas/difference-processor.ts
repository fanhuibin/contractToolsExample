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
  console.log(`📊 [预处理开始] 收到${differences.length}个差异项`)
  const pageMap = new Map<number, ProcessedDifferenceItem[]>()
  
  differences.forEach(diff => {
    if (diff.operation === 'DELETE' && diff.pageAList && diff.oldBboxes) {
      // DELETE操作：处理旧文档页面
      diff.oldBboxes.forEach((bbox: number[], index: number) => {
        const pageNum = (diff.pageAList as number[])[index]
        if (pageNum) {
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
    } else if (diff.operation === 'INSERT' && diff.pageBList && diff.newBboxes) {
      // INSERT操作：处理新文档页面
      diff.newBboxes.forEach((bbox: number[], index: number) => {
        const pageNum = (diff.pageBList as number[])[index]
        if (pageNum) {
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
  
  console.log(`📊 [预处理完成] 生成${pageMap.size}个页面的差异数据`)
  pageMap.forEach((diffs, pageNum) => {
    console.log(`📄 页面${pageNum}: ${diffs.length}个差异项`)
  })
  
  return pageMap
}
