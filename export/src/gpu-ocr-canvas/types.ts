/**
 * GPU OCR Canvas 比对相关的类型定义
 */

// 页面布局信息
export interface PageLayout {
  y: number
  height: number
  scale: number
  visible: boolean
}

// 图片信息
export interface PageImageInfo {
  width: number
  height: number
  imageUrl?: string
}

// 文档图片信息
export interface DocumentImageInfo {
  pages: PageImageInfo[]
  totalPages: number
}

// 差异项数据
export interface DifferenceItem {
  operation: 'DELETE' | 'INSERT'
  pageA?: number
  pageB?: number
  page?: number
  pageAList?: number[]
  pageBList?: number[]
  oldBbox?: number[]
  newBbox?: number[]
  prevOldBbox?: number[]
  prevNewBbox?: number[]
  oldBboxes?: number[][]
  newBboxes?: number[][]
  allTextA?: string[]
  allTextB?: string[]
  diffRangesA?: any[]
  diffRangesB?: any[]
  oldText?: string
  newText?: string
  textStartIndexA?: number
  textStartIndexB?: number
}

// 位置信息
export interface Position {
  x: number
  y: number
  width?: number
  height?: number
  page: number
  bbox?: number[]
}

// 点击区域信息
export interface ClickableArea {
  x: number
  y: number
  width: number
  height: number
  diffIndex: number
  operation: string
  bbox: number[]
  originalDiff: DifferenceItem
}

// 可见范围信息
export interface VisibleRange {
  start: number
  end: number
  visiblePages: number[]
}

// Canvas渲染模式
export type CanvasMode = 'old' | 'new'

// 滚动侧
export type ScrollSide = 'old' | 'new'

// 筛选模式
export type FilterMode = 'ALL' | 'DELETE' | 'INSERT'
