<template>
  <div class="doc-center-layout">
    <router-view />
  </div>
</template>

<script setup lang="ts">
// 独立的文档中心布局 - 不包含主框架的导航栏等
</script>

<style scoped lang="scss">
.doc-center-layout {
  width: 100%;
  height: 100vh;
  overflow: hidden;
  background: #f5f7fa;
}
</style>

