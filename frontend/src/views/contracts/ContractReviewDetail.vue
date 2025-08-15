<template>
  <div class="contract-review-detail">
    <!-- 顶部导航区域 -->
    <div class="page-header">
      <div class="left">
        <el-button @click="goBack" type="primary" plain>
          <el-icon><ArrowLeft /></el-icon> 返回
        </el-button>
        <h2 class="file-name">{{ fileName || '文档审核结果' }}</h2>
      </div>
      <div class="right">
        <el-tag type="success" v-if="reviewDate">审核时间: {{ reviewDate }}</el-tag>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="main-content">
      <!-- 左侧文档显示区域 -->
      <div class="document-area">
        <div v-if="loading" class="loading-container">
          <el-skeleton :rows="10" animated />
        </div>
        <div v-else-if="!fileId" class="no-file">
          <el-empty description="未找到文件，请返回重新选择" />
        </div>
        <div v-else class="onlyoffice-container">
          <!-- 使用OnlyOffice编辑器 -->
          <OnlyOfficeEditor
            :file-id="fileId"
            :can-edit="false"
            :height="documentHeight"
            ref="documentEditor"
            @ready="handleEditorReady"
            @error="handleEditorError"
            @documentStateChange="handleDocumentStateChange"
          />
        </div>
      </div>

      <!-- 右侧风险提示区域 -->
      <div class="risk-panel">
        <div class="panel-header">
          <h3>风险提示 <el-badge :value="totalRisks" :max="99" class="risk-badge" /></h3>
          <div class="risk-summary">
            <el-tag type="danger" v-if="highRisks">高风险: {{ highRisks }}</el-tag>
            <el-tag type="warning" v-if="mediumRisks">中风险: {{ mediumRisks }}</el-tag>
            <el-tag type="info" v-if="lowRisks">低风险: {{ lowRisks }}</el-tag>
          </div>
        </div>

        <el-scrollbar height="calc(100vh - 220px)">
          <div v-if="loading" class="loading-risks">
            <el-skeleton :rows="5" animated />
          </div>
          <div v-else-if="risks.length === 0" class="no-risks">
            <el-empty description="未发现风险" />
          </div>
          <div v-else class="risk-list">
            <div 
              v-for="(risk, index) in risks" 
              :key="index"
              class="risk-item"
              :class="getRiskClass(risk.level)"
            >
              <div class="risk-header">
                <el-tag :type="getRiskType(risk.level)" size="small">{{ getRiskLevelText(risk.level) }}</el-tag>
                <span class="risk-title">{{ risk.title }}</span>
              </div>
              <div class="risk-content">
                <p class="risk-description">{{ risk.description }}</p>
                <div class="risk-suggestion">
                  <strong>建议：</strong> {{ risk.suggestion }}
                </div>
                <div class="risk-location">
                  <span>位置：第 {{ risk.page || '?' }} 页</span>
                  <el-button 
                    type="primary" 
                    size="small" 
                    @click="locateRisk(risk)"
                    :disabled="!risk.position"
                  >
                    定位
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </el-scrollbar>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import OnlyOfficeEditor from '@/components/onlyoffice/OnlyOfficeEditor.vue'

// 路由相关
const route = useRoute()
const router = useRouter()
const fileId = ref(route.params.fileId as string)

// 基础数据
const fileName = ref('')
const reviewDate = ref('')
const loading = ref(true)
const documentHeight = ref('calc(100vh - 180px)')

// 风险数据
const risks = ref<any[]>([])
const documentEditor = ref()

// 计算属性
const totalRisks = computed(() => risks.value.length)
const highRisks = computed(() => risks.value.filter(r => r.level === 'high').length)
const mediumRisks = computed(() => risks.value.filter(r => r.level === 'medium').length)
const lowRisks = computed(() => risks.value.filter(r => r.level === 'low').length)

// 方法
const goBack = () => {
  router.push('/contract-review')
}

const getRiskClass = (level: string) => {
  switch (level) {
    case 'high': return 'risk-high'
    case 'medium': return 'risk-medium'
    case 'low': return 'risk-low'
    default: return ''
  }
}

const getRiskType = (level: string) => {
  switch (level) {
    case 'high': return 'danger'
    case 'medium': return 'warning'
    case 'low': return 'info'
    default: return 'info'
  }
}

const getRiskLevelText = (level: string) => {
  switch (level) {
    case 'high': return '高风险'
    case 'medium': return '中风险'
    case 'low': return '低风险'
    default: return '未知'
  }
}

// 定位到文档中的风险位置
const locateRisk = (risk: any) => {
  if (!risk.position) {
    ElMessage.warning('无法定位，该风险没有位置信息')
    return
  }

  // 调用OnlyOffice API定位到文档位置
  if (documentEditor.value) {
    try {
      // 使用OnlyOffice的postToPlugin方法发送定位命令
      documentEditor.value.postToPlugin({
        action: 'locate',
        // 传递位置信息
        position: {
          page: risk.page || risk.position.page || 1,
          paragraph: risk.position.paragraph || 1,
          text: risk.description || risk.title // 可以用来搜索文本
        },
        highlight: true
      })
      
      // 创建一个临时的内容控件来高亮显示风险位置
      // 注意：这需要OnlyOffice插件支持
      documentEditor.value.createContentControl(
        `risk_${risk.id}`, 
        `风险: ${risk.title}`, 
        risk.description,
        true // 只读
      )
      
      ElMessage.success('已定位到风险位置')
    } catch (error) {
      console.error('定位失败:', error)
      ElMessage.warning('定位失败，请确保文档已加载完成')
    }
  } else {
    ElMessage.warning('文档编辑器未就绪，请稍后再试')
  }
}

// 加载审核结果数据
const loadReviewData = async () => {
  loading.value = true
  
  try {
    // 尝试从localStorage获取数据
    const savedData = localStorage.getItem('review_result_data')
    
    if (savedData) {
      // 解析保存的数据
      const parsedData = JSON.parse(savedData)
      
      fileName.value = parsedData.fileName || '未命名文件'
      reviewDate.value = parsedData.reviewDate || new Date().toLocaleString()
      
      // 转换审核结果为风险列表
      if (Array.isArray(parsedData.results)) {
        risks.value = parsedData.results.map((result: any, index: number) => {
          return {
            id: index + 1,
            title: result.title || result.name || `风险项 #${index + 1}`,
            description: result.description || result.content || '未提供描述',
            suggestion: result.suggestion || result.recommendation || '无具体建议',
            level: result.level || result.severity || 'medium',
            page: result.page || result.location?.page || Math.floor(Math.random() * 5) + 1,
            position: result.position || result.location || { page: Math.floor(Math.random() * 5) + 1, paragraph: Math.floor(Math.random() * 10) + 1 }
          }
        })
      } else {
        // 使用模拟数据
        useDefaultRiskData()
      }
      
      // 使用完后清除localStorage中的数据
      localStorage.removeItem('review_result_data')
    } else {
      // 如果没有保存的数据，则使用模拟数据
      useDefaultRiskData()
    }
    
    // 使用路由中的文件ID，这应该是一个真实存在的文件ID
    // 如果需要，可以在这里设置默认文件ID
    if (!fileId.value || fileId.value === 'example' || fileId.value === 'tempalteDesign') {
      // 使用一个已知存在的文件ID
      fileId.value = 'templateDesign'
    }
    
  } catch (error) {
    console.error('加载审核结果失败', error)
    ElMessage.error('加载审核结果失败')
    useDefaultRiskData()
  } finally {
    loading.value = false
  }
}

// 使用默认的模拟风险数据
const useDefaultRiskData = () => {
  fileName.value = '示例合同.docx'
  reviewDate.value = new Date().toLocaleString()
  
  // 模拟风险数据
  risks.value = [
    {
      id: 1,
      title: '付款条款不明确',
      description: '合同中未明确约定付款时间和付款方式',
      suggestion: '建议在合同中明确约定付款时间、付款方式和付款条件，避免产生纠纷',
      level: 'high',
      page: 2,
      position: { page: 2, paragraph: 3 }
    },
    {
      id: 2,
      title: '违约责任条款缺失',
      description: '合同中未约定违约责任',
      suggestion: '建议在合同中明确约定违约责任，包括违约金计算方式和支付时间',
      level: 'high',
      page: 3,
      position: { page: 3, paragraph: 5 }
    },
    {
      id: 3,
      title: '合同期限表述不清',
      description: '合同履行期限表述模糊，可能导致争议',
      suggestion: '建议明确约定合同开始日期和结束日期，或者明确约定合同期限',
      level: 'medium',
      page: 1,
      position: { page: 1, paragraph: 2 }
    },
    {
      id: 4,
      title: '合同主体信息不完整',
      description: '乙方营业执照信息不完整',
      suggestion: '建议补充乙方的营业执照号码、法定代表人等信息',
      level: 'medium',
      page: 1,
      position: { page: 1, paragraph: 1 }
    },
    {
      id: 5,
      title: '条款表述不规范',
      description: '第三条中使用了"尽快"等模糊表述',
      suggestion: '建议使用具体的时间或期限代替"尽快"等模糊表述',
      level: 'low',
      page: 4,
      position: { page: 4, paragraph: 2 }
    }
  ]
}

// 处理编辑器就绪事件
const handleEditorReady = () => {
  console.log('编辑器已就绪')
  ElMessage.success('文档加载完成')
}

// 处理编辑器错误事件
const handleEditorError = (error) => {
  console.error('编辑器错误:', error)
  ElMessage.error('文档加载失败: ' + (error.message || '未知错误'))
}

// 处理文档状态变化事件
const handleDocumentStateChange = (event) => {
  console.log('文档状态变化:', event)
}

// 生命周期钩子
onMounted(() => {
  if (!fileId.value) {
    ElMessage.warning('未指定文件ID')
    return
  }
  
  loadReviewData()
})
</script>

<style scoped>
.contract-review-detail {
  padding: 20px;
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header .left {
  display: flex;
  align-items: center;
  gap: 15px;
}

.file-name {
  margin: 0;
  font-size: 20px;
  color: #303133;
}

.main-content {
  display: flex;
  flex: 1;
  gap: 20px;
  height: calc(100vh - 100px);
  overflow: hidden;
}

.document-area {
  flex: 7;
  border: 1px solid #EBEEF5;
  border-radius: 4px;
  overflow: hidden;
  background-color: #fff;
}

.risk-panel {
  flex: 3;
  border: 1px solid #EBEEF5;
  border-radius: 4px;
  background-color: #fff;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel-header {
  padding: 15px;
  border-bottom: 1px solid #EBEEF5;
}

.panel-header h3 {
  margin: 0 0 10px 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.risk-summary {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.risk-list {
  padding: 15px;
}

.risk-item {
  margin-bottom: 20px;
  padding: 15px;
  border-radius: 4px;
  border-left: 4px solid #909399;
  background-color: #F5F7FA;
}

.risk-high {
  border-left-color: #F56C6C;
  background-color: #FEF0F0;
}

.risk-medium {
  border-left-color: #E6A23C;
  background-color: #FDF6EC;
}

.risk-low {
  border-left-color: #909399;
  background-color: #F4F4F5;
}

.risk-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.risk-title {
  font-weight: bold;
  font-size: 16px;
}

.risk-content {
  margin-left: 5px;
}

.risk-description {
  margin: 5px 0;
  color: #606266;
}

.risk-suggestion {
  margin: 10px 0;
  color: #303133;
}

.risk-location {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
  color: #909399;
}

.loading-container,
.no-file,
.loading-risks,
.no-risks {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  padding: 20px;
}

.onlyoffice-container {
  height: 100%;
}

.risk-badge {
  margin-left: 8px;
}
</style>