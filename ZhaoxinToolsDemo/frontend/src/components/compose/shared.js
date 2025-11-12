import ZHAOXIN_CONFIG from '@/config'

const getDemoStaticBase = () => {
  const candidate = ZHAOXIN_CONFIG.customFieldsBaseUrl || ZHAOXIN_CONFIG.demoBaseUrl || (typeof window !== 'undefined' ? window.location.origin : '')
  return (candidate || '').replace(/\/$/, '')
}

const buildDemoResourceUrl = (path) => {
  const base = getDemoStaticBase()
  const normalized = String(path || '').replace(/^\/+/, '')
  if (!normalized) return base
  const encodedPath = normalized
    .split('/')
    .map(segment => encodeURIComponent(segment))
    .join('/')
  return `${base}/api/demo/documents/download?path=${encodedPath}`
}

export const stampLibrary = {
  get buyer() {
    return buildDemoResourceUrl('stamp.png')
  },
  get seller() {
    return buildDemoResourceUrl('stamp.png')
  }
}

export const ridingStampLibrary = {
  get standard() {
    return buildDemoResourceUrl('stamp.png')
  }
}

export const attachmentLibrary = {
  get techSpec() {
    return {
      label: '测试附件A',
      url: buildDemoResourceUrl('fujianA.pdf')
    }
  },
  get qualification() {
    return {
      label: '测试附件B',
      url: buildDemoResourceUrl('fujianB.pdf')
    }
  }
}

export const pad2 = (value) => String(value).padStart(2, '0')

export const formatDateOnly = (date) => {
  if (!(date instanceof Date) || Number.isNaN(date.getTime())) return '-'
  return `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())}`
}

export const formatDateTimeDisplay = (date) => {
  if (!(date instanceof Date) || Number.isNaN(date.getTime())) return '-'
  return `${formatDateOnly(date)} ${pad2(date.getHours())}:${pad2(date.getMinutes())}`
}

export const addDays = (date, days) => {
  const result = new Date(date.getTime())
  result.setDate(result.getDate() + days)
  return result
}

export const futureDateString = (days) => formatDateOnly(addDays(new Date(), days))

export const todayCode = () => {
  const now = new Date()
  return `${now.getFullYear()}${pad2(now.getMonth() + 1)}${pad2(now.getDate())}`
}

export const todayShortCode = () => {
  const now = new Date()
  return `${String(now.getFullYear()).slice(-2)}${pad2(now.getMonth() + 1)}${pad2(now.getDate())}`
}

export const timestampForFile = () => {
  const now = new Date()
  return `${todayCode()}_${pad2(now.getHours())}${pad2(now.getMinutes())}${pad2(now.getSeconds())}`
}

export const toTimestamp = (value) => {
  if (!value) return 0
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? 0 : date.getTime()
}
