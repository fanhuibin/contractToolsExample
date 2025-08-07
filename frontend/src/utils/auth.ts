/**
 * 认证相关工具函数
 */

// Token存储键名
const TOKEN_KEY = 'access_token'

/**
 * 获取token
 */
export function getToken(): string {
  return localStorage.getItem(TOKEN_KEY) || ''
}

/**
 * 设置token
 */
export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

/**
 * 移除token
 */
export function removeToken(): void {
  localStorage.removeItem(TOKEN_KEY)
}

/**
 * 检查是否有token
 */
export function hasToken(): boolean {
  return !!getToken()
} 