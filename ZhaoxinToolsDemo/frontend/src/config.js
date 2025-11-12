/**
 * 肇新工具集 Demo 配置文件
 * 配置会从后端API动态加载
 */
export const ZHAOXIN_CONFIG = {
  // 肇新SDK前端地址（用于iframe嵌套）
  frontendUrl: 'http://localhost:3000',
  
  // 肇新SDK后端API地址
  apiBaseUrl: 'http://localhost:8080',
  
  // Demo后端地址（根据当前页面自动获取）
  demoBaseUrl: window.location.origin || 'http://localhost:8091',

  // 自定义字段配置访问基址（可选覆盖）
  customFieldsBaseUrl: ''
}

/**
 * 从后端加载配置
 * @returns {Promise<void>}
 */
export async function loadConfig() {
  try {
    // 使用代理，直接调用相对路径API
    const response = await fetch('/api/demo/config')
    const result = await response.json()
    
    if (result.code === 200 && result.data) {
      // 更新配置
      if (result.data.frontendUrl) {
        ZHAOXIN_CONFIG.frontendUrl = result.data.frontendUrl
      }
      if (result.data.apiBaseUrl) {
        ZHAOXIN_CONFIG.apiBaseUrl = result.data.apiBaseUrl
      }
      if (result.data.customFieldsBaseUrl) {
        ZHAOXIN_CONFIG.customFieldsBaseUrl = result.data.customFieldsBaseUrl
      }
      // 更新demoBaseUrl（使用当前域名，因为通过代理访问）
      ZHAOXIN_CONFIG.demoBaseUrl = window.location.origin
      console.log('✅ 配置加载成功:', ZHAOXIN_CONFIG)
    } else {
      console.warn('⚠️ 配置加载失败，使用默认配置:', result.message)
      // 即使加载失败，也更新demoBaseUrl
      ZHAOXIN_CONFIG.demoBaseUrl = window.location.origin
    }
  } catch (error) {
    console.error('❌ 配置加载失败，使用默认配置:', error)
    // 即使加载失败，也更新demoBaseUrl
    ZHAOXIN_CONFIG.demoBaseUrl = window.location.origin
  }
}

export default ZHAOXIN_CONFIG

