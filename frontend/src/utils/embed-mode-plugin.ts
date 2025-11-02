/**
 * 嵌入模式插件
 * 
 * 这个插件会拦截所有的 router.back() 调用，
 * 在嵌入模式下自动发送 postMessage 通知父页面
 */

import type { Router } from 'vue-router'

export function setupEmbedModePlugin(router: Router) {
  // 保存原始的 back 方法
  const originalBack = router.back.bind(router)
  const originalPush = router.push.bind(router)
  
  /**
   * 检测是否为嵌入模式
   */
  const isEmbedMode = () => {
    const urlParams = new URLSearchParams(window.location.search)
    return urlParams.get('embed') === 'true'
  }
  
  /**
   * 发送返回消息到父页面
   */
  const sendBackMessage = () => {
    console.log('🔙 [嵌入模式] 拦截返回操作，发送 postMessage')
    window.parent.postMessage({
      type: 'NAVIGATE_BACK',
      source: 'zhaoxin-sdk',
      payload: {
        from: router.currentRoute.value.path,
        query: router.currentRoute.value.query,
        timestamp: Date.now()
      }
    }, '*')
  }
  
  /**
   * 重写 router.back() 方法
   */
  router.back = function() {
    if (isEmbedMode()) {
      // 嵌入模式：发送消息到父页面
      sendBackMessage()
    } else {
      // 独立模式：执行原始的 back 方法
      originalBack()
    }
  }
  
  /**
   * 拦截特定的 push 操作（可选）
   * 如果 push 的目标是返回上一页的路由，也应该被拦截
   */
  router.push = function(to: any) {
    // 这里可以添加额外的逻辑
    // 例如：检测是否正在导航回父页面
    return originalPush(to)
  }
  
  console.log('✅ 嵌入模式插件已安装')
}

