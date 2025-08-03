<template>
  <div class="documents-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>文档管理</span>
        </div>
      </template>
      <div class="content">
        <el-empty description="文档管理功能开发中..." />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
// 文档管理页面
</script>

<style scoped>
.documents-container {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  
  .content {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 400px;
  }
}
</style> 