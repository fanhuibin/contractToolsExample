import { ref, onMounted } from 'vue'

/**
 * 百度统计组合函数 - 极简版
 * 自动加载百度统计代码
 */
export function useBaiduAnalytics() {
  const isLoaded = ref(false)
  const isEnabled = ref(false)
  const error = ref(null)

  /**
   * 加载百度统计
   */
  const loadBaiduAnalytics = async () => {
    try {
      const response = await fetch('/api/demo/baidu-analytics/js-code')
      const result = await response.json()
      
      if (result.code === 200 && result.data) {
        // 创建script标签并执行
        const script = document.createElement('script')
        script.text = result.data
        document.head.appendChild(script)
        
        isLoaded.value = true
        isEnabled.value = true
        console.log('✅ 百度统计已加载')
      } else {
        console.log('ℹ️ 百度统计未启用')
      }
    } catch (err) {
      error.value = err
      console.error('❌ 百度统计加载失败:', err)
    }
  }

  /**
   * 页面访问统计
   */
  const trackPageView = (page) => {
    if (isLoaded.value && window._hmt) {
      window._hmt.push(['_trackPageview', page])
      console.log('📊 页面访问统计:', page)
    }
  }

  /**
   * 事件统计
   */
  const trackEvent = (category, action, label, value) => {
    if (isLoaded.value && window._hmt) {
      window._hmt.push(['_trackEvent', category, action, label, value])
      console.log('📊 事件统计:', { category, action, label, value })
    }
  }

  onMounted(() => {
    loadBaiduAnalytics()
  })

  return {
    isLoaded,
    isEnabled,
    error,
    trackPageView,
    trackEvent,
    loadBaiduAnalytics
  }
}
