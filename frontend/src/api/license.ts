import request from '@/utils/request'

/**
 * 获取授权信息（静默模式，不弹错误提示）
 */
export const getLicenseInfo = () => {
  return request({
    url: '/auth/license-info',
    method: 'get',
    skipErrorNotification: true  // 静默模式
  })
}

/**
 * 验证授权（静默模式）
 */
export const validateLicense = () => {
  return request({
    url: '/auth/validate',
    method: 'get',
    skipErrorNotification: true
  })
}

/**
 * 获取授权详细信息（静默模式）
 */
export const getLicenseDetails = () => {
  return request({
    url: '/auth/license-details',
    method: 'get',
    skipErrorNotification: true
  })
}

/**
 * 检查单个模块权限（静默模式）
 */
export const checkModule = (moduleCode: string) => {
  return request({
    url: '/auth/check-module',
    method: 'get',
    params: { moduleCode },
    skipErrorNotification: true
  })
}

/**
 * 批量检查模块权限（静默模式）
 */
export const checkModules = (moduleCodes: string[]) => {
  return request({
    url: '/auth/check-modules',
    method: 'post',
    data: moduleCodes,
    skipErrorNotification: true
  })
}

/**
 * 获取所有可用模块（静默模式）
 */
export const getAvailableModules = () => {
  return request({
    url: '/auth/modules',
    method: 'get',
    skipErrorNotification: true
  })
}

/**
 * 获取服务器硬件信息（静默模式）
 */
export const getHardwareInfo = () => {
  return request({
    url: '/license/getServerInfos',
    method: 'get',
    skipErrorNotification: true
  })
}

/**
 * 验证硬件匹配（静默模式）
 */
export const validateHardware = () => {
  return request({
    url: '/auth/hardware-validation',
    method: 'get',
    skipErrorNotification: true
  })
}

