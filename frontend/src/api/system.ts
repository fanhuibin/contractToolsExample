import request from '@/utils/request'

/**
 * 获取系统版本信息
 */
export function getSystemVersion() {
  return request({
    url: '/system/version',
    method: 'get'
  })
}

/**
 * 获取系统配置信息
 */
export function getSystemConfig() {
  return request({
    url: '/system/config',
    method: 'get'
  })
}

