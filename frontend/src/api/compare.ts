import request from '@/utils/request'

export function uploadCompare(formData: FormData & {
  ignoreHeaderFooter?: string | boolean
  headerHeightMm?: string | number
  footerHeightMm?: string | number
  ignoreCase?: string | boolean
  ignoredSymbols?: string
}) {
  return request({
    url: '/compare/upload',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    // 比对与转换可能较久，延长超时（10分钟）
    timeout: 600000
  })
}

export function compareByUrls(params: {
  oldUrl: string;
  newUrl: string;
  ignoreHeaderFooter?: boolean;
  headerHeightMm?: number;
  footerHeightMm?: number;
  ignoreCase?: boolean;
  ignoredSymbols?: string;
}) {
  return request({
    url: '/compare/byUrls',
    method: 'post',
    params,
    // 比对与转换可能较久，延长超时（10分钟）
    timeout: 600000
  })
}


