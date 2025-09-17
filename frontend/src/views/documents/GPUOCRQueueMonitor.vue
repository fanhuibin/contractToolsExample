<template>
  <div class="queue-monitor">
    <div class="monitor-header">
      <h2>GPU OCR 任务队列监控</h2>
      <div class="header-actions">
        <el-button @click="refreshStats" :loading="loading" type="primary">
          <el-icon><Refresh /></el-icon>
          刷新状态
        </el-button>
        <el-button @click="autoRefresh = !autoRefresh" :type="autoRefresh ? 'success' : 'info'">
          <el-icon><Timer /></el-icon>
          {{ autoRefresh ? '停止自动刷新' : '开启自动刷新' }}
        </el-button>
      </div>
    </div>

    <!-- 队列总览 -->
    <div class="stats-overview">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-item">
              <div class="stat-value" :class="{ 'busy': queueStats.isBusy }">
                {{ queueStats.activeThreads }}/{{ queueStats.maxThreads }}
              </div>
              <div class="stat-label">活跃线程</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-item">
              <div class="stat-value">{{ queueStats.currentQueueSize }}</div>
              <div class="stat-label">队列大小</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-item">
              <div class="stat-value">{{ queueStats.totalSubmitted }}</div>
              <div class="stat-label">总提交任务</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-item">
              <div class="stat-value">{{ queueStats.totalCompleted }}</div>
              <div class="stat-label">总完成任务</div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 队列状态指示器 -->
    <div class="queue-status">
      <el-card>
        <template #header>
          <span>队列状态</span>
          <el-tag :type="queueStats.isBusy ? 'danger' : 'success'" style="float: right;">
            {{ queueStats.isBusy ? '繁忙' : '空闲' }}
          </el-tag>
        </template>
        
        <div class="status-details">
          <el-row :gutter="20">
            <el-col :span="12">
              <div class="status-item">
                <span class="label">线程使用率:</span>
                <el-progress 
                  :percentage="threadUsagePercent" 
                  :color="getProgressColor(threadUsagePercent)"
                  :show-text="true"
                />
              </div>
            </el-col>
            <el-col :span="12">
              <div class="status-item">
                <span class="label">队列使用率:</span>
                <el-progress 
                  :percentage="queueUsagePercent" 
                  :color="getProgressColor(queueUsagePercent)"
                  :show-text="true"
                />
              </div>
            </el-col>
          </el-row>
        </div>
      </el-card>
    </div>

    <!-- 并发控制 -->
    <div class="concurrency-control">
      <el-card>
        <template #header>
          <span>并发控制</span>
        </template>
        
        <div class="control-panel">
          <el-row :gutter="20" align="middle">
            <el-col :span="12">
              <div class="control-item">
                <span class="label">最大并发线程数:</span>
                <el-input-number
                  v-model="newMaxThreads"
                  :min="1"
                  :max="20"
                  :step="1"
                  size="small"
                  style="width: 120px; margin-left: 10px;"
                />
              </div>
            </el-col>
            <el-col :span="12">
              <el-button 
                @click="adjustConcurrency" 
                :loading="adjusting"
                type="primary"
                :disabled="newMaxThreads === queueStats.maxThreads"
              >
                应用设置
              </el-button>
              <el-button @click="resetConcurrency" size="default">
                重置
              </el-button>
            </el-col>
          </el-row>
        </div>
      </el-card>
    </div>

    <!-- 详细统计信息 -->
    <div class="detailed-stats">
      <el-card>
        <template #header>
          <span>详细统计</span>
        </template>
        
        <el-descriptions :column="2" border>
          <el-descriptions-item label="总提交任务数">
            {{ queueStats.totalSubmitted }}
          </el-descriptions-item>
          <el-descriptions-item label="总完成任务数">
            {{ queueStats.totalCompleted }}
          </el-descriptions-item>
          <el-descriptions-item label="总拒绝任务数">
            <span :class="{ 'error-text': queueStats.totalRejected > 0 }">
              {{ queueStats.totalRejected }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="执行器完成任务数">
            {{ queueStats.executorCompletedTasks }}
          </el-descriptions-item>
          <el-descriptions-item label="执行器总任务数">
            {{ queueStats.executorTotalTasks }}
          </el-descriptions-item>
          <el-descriptions-item label="成功率">
            <el-tag :type="getSuccessRateType(successRate)">
              {{ successRate }}%
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>
    </div>

    <!-- 实时日志（如果需要的话可以添加） -->
    <div class="queue-tips">
      <el-card>
        <template #header>
          <span>使用说明</span>
        </template>
        
        <ul class="tips-list">
          <li>线程使用率或队列使用率超过80%时，系统将被标记为"繁忙"状态</li>
          <li>建议根据服务器性能和OCR服务能力调整最大并发线程数</li>
          <li>过高的并发可能导致OCR服务过载，过低则影响处理效率</li>
          <li>任务被拒绝时会尝试在调用线程中执行（降级策略）</li>
        </ul>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Timer } from '@element-plus/icons-vue'
import { getQueueStats, checkQueueBusy, adjustMaxConcurrency } from '@/api/gpu-ocr-compare'

// 响应式数据
const loading = ref(false)
const adjusting = ref(false)
const autoRefresh = ref(false)
const newMaxThreads = ref(4)

// 队列统计数据
const queueStats = reactive({
  totalSubmitted: 0,
  totalCompleted: 0,
  totalRejected: 0,
  currentQueueSize: 0,
  activeThreads: 0,
  maxThreads: 4,
  executorCompletedTasks: 0,
  executorTotalTasks: 0,
  isBusy: false
})

// 自动刷新定时器
let refreshTimer: number | null = null

// 计算属性
const threadUsagePercent = computed(() => {
  if (queueStats.maxThreads === 0) return 0
  return Math.round((queueStats.activeThreads / queueStats.maxThreads) * 100)
})

const queueUsagePercent = computed(() => {
  const maxQueueSize = 100 // 队列容量
  return Math.round((queueStats.currentQueueSize / maxQueueSize) * 100)
})

const successRate = computed(() => {
  if (queueStats.totalSubmitted === 0) return 100
  const successCount = queueStats.totalSubmitted - queueStats.totalRejected
  return Math.round((successCount / queueStats.totalSubmitted) * 100)
})

// 方法
const getProgressColor = (percentage: number) => {
  if (percentage >= 80) return '#f56c6c'
  if (percentage >= 60) return '#e6a23c'
  return '#67c23a'
}

const getSuccessRateType = (rate: number) => {
  if (rate >= 95) return 'success'
  if (rate >= 85) return 'warning'
  return 'danger'
}

const refreshStats = async () => {
  loading.value = true
  try {
    // 获取详细统计
    const statsResponse = await getQueueStats()
    if (statsResponse.success) {
      Object.assign(queueStats, statsResponse.data)
      newMaxThreads.value = queueStats.maxThreads
    }
    
    // 获取繁忙状态
    const busyResponse = await checkQueueBusy()
    if (busyResponse.success) {
      queueStats.isBusy = busyResponse.data.isBusy
    }
    
  } catch (error) {
    console.error('刷新队列状态失败:', error)
    ElMessage.error('刷新队列状态失败')
  } finally {
    loading.value = false
  }
}

const adjustConcurrency = async () => {
  if (newMaxThreads.value === queueStats.maxThreads) {
    return
  }
  
  adjusting.value = true
  try {
    const response = await adjustMaxConcurrency(newMaxThreads.value)
    if (response.success) {
      ElMessage.success(response.message)
      await refreshStats()
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    console.error('调整并发线程数失败:', error)
    ElMessage.error('调整并发线程数失败')
  } finally {
    adjusting.value = false
  }
}

const resetConcurrency = () => {
  newMaxThreads.value = queueStats.maxThreads
}

const startAutoRefresh = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
  refreshTimer = setInterval(refreshStats, 3000) // 每3秒刷新一次
}

const stopAutoRefresh = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

// 监听自动刷新状态变化
const handleAutoRefreshChange = (value: boolean) => {
  if (value) {
    startAutoRefresh()
  } else {
    stopAutoRefresh()
  }
}

// 生命周期
onMounted(() => {
  refreshStats()
  
  // 监听自动刷新变化
  const unwatch = () => {}
  return unwatch
})

onUnmounted(() => {
  stopAutoRefresh()
})

// 监听autoRefresh变化
const unwatchAutoRefresh = () => {}
</script>

<style scoped>
.queue-monitor {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.monitor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.monitor-header h2 {
  margin: 0;
  color: #303133;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.stats-overview {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
}

.stat-item {
  padding: 10px;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 5px;
}

.stat-value.busy {
  color: #f56c6c;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.queue-status {
  margin-bottom: 20px;
}

.status-details {
  padding: 10px 0;
}

.status-item {
  margin-bottom: 15px;
}

.status-item .label {
  display: inline-block;
  width: 100px;
  font-weight: 500;
  color: #606266;
}

.concurrency-control {
  margin-bottom: 20px;
}

.control-panel {
  padding: 10px 0;
}

.control-item {
  display: flex;
  align-items: center;
}

.control-item .label {
  font-weight: 500;
  color: #606266;
}

.detailed-stats {
  margin-bottom: 20px;
}

.error-text {
  color: #f56c6c;
  font-weight: bold;
}

.queue-tips {
  margin-bottom: 20px;
}

.tips-list {
  margin: 0;
  padding-left: 20px;
  color: #606266;
  line-height: 1.6;
}

.tips-list li {
  margin-bottom: 8px;
}

:deep(.el-card__header) {
  background-color: #fafafa;
  border-bottom: 1px solid #ebeef5;
}

:deep(.el-progress-bar__outer) {
  border-radius: 10px;
}

:deep(.el-descriptions__label) {
  font-weight: 500;
}
</style>
