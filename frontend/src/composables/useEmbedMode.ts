import { computed } from 'vue'
import { useRoute } from 'vue-router'

/**
 * 嵌入模式检测和管理
 * 
 * 提供统一的嵌入模式检测、返回按钮控制和返回处理逻辑
 * 
 * @returns {Object} { isEmbedMode, shouldHideBack, handleBack }
 * 
 * @example
 * ```typescript
 * import { useEmbedMode } from '@/composables/useEmbedMode'
 * 
 * const { shouldHideBack, handleBack } = useEmbedMode()
 * 
 * // 在模板中条件渲染返回按钮
 * <el-button v-if="!shouldHideBack" @click="handleBack(() => router.push('/'))">
 *   返回
 * </el-button>
 * ```
 */
export function useEmbedMode() {
  const route = useRoute()
  
  /**
   * 检测是否为嵌入模式
   * 通过URL参数 embed=true 判断
   */
  const isEmbedMode = computed(() => route.query.embed === 'true')
  
  /**
   * 检测是否应该隐藏返回按钮
   * 通过URL参数 hideBack=true 判断
   */
  const shouldHideBack = computed(() => route.query.hideBack === 'true')
  
  /**
   * 统一的返回处理逻辑
   * 
   * 在嵌入模式下，发送postMessage消息到父窗口
   * 在独立模式下，执行默认的返回处理函数
   * 
   * @param {Function} defaultBackHandler - 默认的返回处理函数（独立模式下执行）
   * 
   * @example
   * ```typescript
   * const handleBack = () => {
   *   embedHandleBack(() => {
   *     // 默认返回逻辑
   *     router.push('/rule-extract')
   *   })
   * }
   * ```
   */
  const handleBack = (defaultBackHandler: () => void) => {
    if (isEmbedMode.value) {
      // 嵌入模式：发送消息到父页面
      console.log('🔙 [嵌入模式] 发送返回消息到父页面', {
        from: route.path,
        query: route.query
      })
      
      window.parent.postMessage({
        type: 'NAVIGATE_BACK',
        source: 'zhaoxin-sdk',
        payload: { 
          from: route.path,
          query: route.query,
          timestamp: Date.now()
        }
      }, '*')
    } else {
      // 独立模式：使用默认处理
      console.log('🔙 [独立模式] 使用默认返回处理')
      defaultBackHandler()
    }
  }
  
  return {
    /**
     * 是否为嵌入模式
     */
    isEmbedMode,
    
    /**
     * 是否应该隐藏返回按钮
     */
    shouldHideBack,
    
    /**
     * 统一的返回处理函数
     */
    handleBack
  }
}

