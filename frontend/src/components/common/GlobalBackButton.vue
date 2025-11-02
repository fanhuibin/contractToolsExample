<template>
  <el-button 
    v-if="!shouldHideBack"
    text 
    @click="handleBackClick"
    v-bind="$attrs"
  >
    <el-icon><Back /></el-icon>
    {{ buttonText }}
  </el-button>
</template>

<script setup lang="ts">
import { Back } from '@element-plus/icons-vue'
import { useEmbedMode } from '@/composables/useEmbedMode'

interface Props {
  /** 返回按钮文本 */
  buttonText?: string
  /** 默认返回处理函数（非嵌入模式下执行） */
  defaultHandler?: () => void
}

const props = withDefaults(defineProps<Props>(), {
  buttonText: '返回',
  defaultHandler: undefined
})

const { shouldHideBack, handleBack } = useEmbedMode()

/**
 * 处理返回点击事件
 */
const handleBackClick = () => {
  handleBack(() => {
    // 如果提供了自定义处理函数，使用它；否则使用浏览器后退
    if (props.defaultHandler) {
      props.defaultHandler()
    } else {
      window.history.back()
    }
  })
}
</script>

<style scoped>
/* 继承父组件样式 */
</style>

