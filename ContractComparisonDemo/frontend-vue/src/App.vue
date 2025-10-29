<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script setup>
// 全局配置
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  width: 100%;
  height: 100%;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, 
               "Helvetica Neue", Arial, sans-serif;
  line-height: 1.6;
  color: #333;
}
</style>

