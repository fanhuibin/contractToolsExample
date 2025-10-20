<template>
  <div class="license-view">
    <el-card class="page-header">
      <template #header>
        <div class="card-header">
          <div class="header-title">
            <el-icon><Key /></el-icon>
            <span>系统授权信息</span>
          </div>
          <div class="header-actions">
            <el-button type="primary" size="small" @click="refreshLicenseInfo">
              <el-icon><Refresh /></el-icon>
              刷新授权信息
            </el-button>
            <el-button type="success" size="small" @click="downloadMachineInfo">
              <el-icon><Download /></el-icon>
              下载机器信息
            </el-button>
          </div>
        </div>
      </template>
      <div class="header-content">
        <p>查看系统授权状态、服务器硬件信息及授权模块详情</p>
        <el-alert 
          v-if="apiError" 
          type="warning" 
          :closable="false"
          show-icon
          class="api-error-alert"
        >
          <template #title>
            后端连接异常
          </template>
          <p>无法连接到授权服务器，显示的是默认数据。请检查：</p>
        </el-alert>
      </div>
    </el-card>

    <!-- 授权状态概览 -->
    <el-row :gutter="20" class="status-cards">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="status-item">
            <el-icon :class="licenseStatus.valid ? 'status-icon success' : 'status-icon error'">
              <CircleCheck v-if="licenseStatus.valid" />
              <CircleClose v-else />
            </el-icon>
            <div class="status-text">
              <div class="status-label">授权状态</div>
              <div class="status-value">{{ licenseStatus.valid ? '已授权' : '未授权' }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="status-item">
            <el-icon class="status-icon info"><Calendar /></el-icon>
            <div class="status-text">
              <div class="status-label">授权到期</div>
              <div class="status-value">{{ formatDate(licenseInfo?.expireDate) }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="status-item">
            <el-icon class="status-icon warning"><Timer /></el-icon>
            <div class="status-text">
              <div class="status-label">剩余天数</div>
              <div class="status-value">{{ remainingDays }} 天</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="status-item">
            <el-icon class="status-icon primary"><Grid /></el-icon>
            <div class="status-text">
              <div class="status-label">授权模块</div>
              <div class="status-value">{{ authorizedModuleCount }} / 6</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <!-- 授权信息 -->
      <el-col :span="12">
        <el-card class="info-card">
          <template #header>
            <div class="card-header">
              <el-icon><Document /></el-icon>
              <span>授权信息</span>
            </div>
          </template>
          <div v-if="loading" class="loading-container">
            <el-skeleton :rows="6" animated />
          </div>
          <div v-else-if="licenseInfo" class="info-content">
            <div class="info-item">
              <span class="info-label">授权码：</span>
              <span class="info-value">{{ licenseInfo?.licenseCode || '未获取' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">授权单位：</span>
              <span class="info-value">{{ licenseInfo?.companyName || '未获取' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">联系人：</span>
              <span class="info-value">{{ licenseInfo?.contactPerson || '未获取' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">联系电话：</span>
              <span class="info-value">{{ licenseInfo?.contactPhone || '未获取' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">生效时间：</span>
              <span class="info-value">{{ formatDate(licenseInfo?.startDate) }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">到期时间：</span>
              <span class="info-value" :class="{ 'expire-warning': isExpiringSoon }">
                {{ formatDate(licenseInfo?.expireDate) }}
                <el-tag v-if="isExpiringSoon" type="warning" size="small" class="ml-2">即将到期</el-tag>
              </span>
            </div>
            <div class="info-item">
              <span class="info-label">硬件绑定：</span>
              <span class="info-value">
                <el-tag :type="licenseInfo?.hardwareBound ? 'success' : 'info'" size="small">
                  {{ licenseInfo?.hardwareBound ? '已绑定' : '未绑定' }}
                </el-tag>
              </span>
            </div>
          </div>
          <el-empty v-else description="无法获取授权信息" />
        </el-card>
      </el-col>

      <!-- 服务器硬件信息 -->
      <el-col :span="12">
        <el-card class="info-card">
          <template #header>
            <div class="card-header">
              <el-icon><Monitor /></el-icon>
              <span>服务器硬件信息</span>
            </div>
          </template>
          <div v-if="loading" class="loading-container">
            <el-skeleton :rows="6" animated />
          </div>
          <div v-else-if="hardwareInfo" class="info-content">
            <div class="info-item">
              <span class="info-label">操作系统：</span>
              <span class="info-value">{{ hardwareInfo?.osName || '未知' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">主板序列号：</span>
              <div class="info-value-wrapper">
                <span class="info-value monospace">{{ hardwareInfo?.mainBoardSerial || '未获取' }}</span>
                <el-button 
                  v-if="hardwareInfo?.mainBoardSerial" 
                  size="small" 
                  text 
                  @click="copyToClipboard(hardwareInfo.mainBoardSerial!)"
                >
                  <el-icon><DocumentCopy /></el-icon>
                </el-button>
              </div>
            </div>
            <div class="info-item">
              <span class="info-label">CPU序列号：</span>
              <div class="info-value-wrapper">
                <span class="info-value monospace">{{ hardwareInfo?.cpuSerial || '未获取' }}</span>
                <el-button 
                  v-if="hardwareInfo?.cpuSerial" 
                  size="small" 
                  text 
                  @click="copyToClipboard(hardwareInfo.cpuSerial!)"
                >
                  <el-icon><DocumentCopy /></el-icon>
                </el-button>
              </div>
            </div>
            <div class="info-item">
              <span class="info-label">网卡MAC地址：</span>
              <div class="info-value-list">
                <div v-for="(mac, index) in validMacAddresses" :key="index" class="mac-item">
                  <span class="monospace">{{ mac }}</span>
                  <el-button size="small" text @click="copyToClipboard(mac)">
                    <el-icon><DocumentCopy /></el-icon>
                  </el-button>
                </div>
                <span v-if="validMacAddresses.length === 0" class="info-value">无可用MAC地址</span>
              </div>
            </div>
            <div v-if="licenseInfo?.hardwareBound" class="info-item">
              <span class="info-label">硬件匹配状态：</span>
              <span class="info-value">
                <el-tag :type="hardwareMatched ? 'success' : 'danger'" size="small">
                  {{ hardwareMatched ? '✓ 匹配成功' : '✗ 不匹配' }}
                </el-tag>
              </span>
            </div>
          </div>
          <el-empty v-else description="无法获取硬件信息" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 授权模块列表 -->
    <el-card class="modules-card">
      <template #header>
        <div class="card-header">
          <el-icon><Grid /></el-icon>
          <span>授权模块列表</span>
        </div>
      </template>
      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="3" animated />
      </div>
      <div v-else class="modules-grid">
        <div
          v-for="module in allModules"
          :key="module.code"
          :class="['module-item', { authorized: module.authorized, unauthorized: !module.authorized }]"
        >
          <div class="module-icon">
            <el-icon v-if="module.authorized" class="icon-authorized">
              <CircleCheck />
            </el-icon>
            <el-icon v-else class="icon-unauthorized">
              <CircleClose />
            </el-icon>
          </div>
          <div class="module-info">
            <div class="module-name">{{ module.name }}</div>
            <div class="module-status">
              <el-tag :type="module.authorized ? 'success' : 'info'" size="small">
                {{ module.authorized ? '已授权' : '未授权' }}
              </el-tag>
            </div>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Key,
  CircleCheck,
  CircleClose,
  Calendar,
  Timer,
  Grid,
  Document,
  Monitor,
  DocumentCopy,
  Refresh,
  Download
} from '@element-plus/icons-vue'
import { getLicenseInfo, getHardwareInfo, validateLicense, checkModules } from '@/api/license'

interface LicenseInfo {
  licenseCode: string
  companyName: string
  contactPerson: string
  contactPhone: string
  startDate?: string  // 可选字段
  expireDate?: string  // 可选字段
  hardwareBound: boolean
  authorizedModules: string[]
  maxUsers: number
}

interface HardwareInfo {
  osName: string
  mainBoardSerial: string
  cpuSerial: string
  macAddress: string[]
}

interface ModuleInfo {
  code: string
  name: string
  authorized: boolean
}

const loading = ref(true)
const licenseInfo = ref<LicenseInfo | null>(null)
const hardwareInfo = ref<HardwareInfo | null>(null)
const hardwareMatched = ref(false)
const licenseStatus = ref({ valid: false })
const apiError = ref(false)

// 所有模块定义
const MODULE_DEFINITIONS = [
  { code: 'smart_document_extraction', name: '智能文档抽取' },
  { code: 'smart_document_compare', name: '智能文档比对' },
  { code: 'smart_contract_synthesis', name: '智能合同合成' },
  { code: 'smart_document_parse', name: '智能文档解析' },
  { code: 'document_online_edit', name: '文档在线编辑' },
  { code: 'document_format_convert', name: '文档格式转换' }
]

const allModules = ref<ModuleInfo[]>([])

// 计算属性
const remainingDays = computed(() => {
  if (!licenseInfo.value || !licenseInfo.value.expireDate) return 0
  try {
    const now = new Date()
    const expire = new Date(licenseInfo.value.expireDate)
    const diff = expire.getTime() - now.getTime()
    return Math.max(0, Math.ceil(diff / (1000 * 60 * 60 * 24)))
  } catch {
    return 0
  }
})

const isExpiringSoon = computed(() => {
  return remainingDays.value > 0 && remainingDays.value <= 30
})

const authorizedModuleCount = computed(() => {
  return allModules.value.filter(m => m.authorized).length
})

// 有效的MAC地址列表（过滤掉null值）
const validMacAddresses = computed(() => {
  if (!hardwareInfo.value || !hardwareInfo.value.macAddress) return []
  return hardwareInfo.value.macAddress.filter(mac => mac !== null && mac !== undefined && mac !== '')
})

// 格式化日期
const formatDate = (dateStr: string | undefined | null) => {
  if (!dateStr) return '未设置'
  try {
    const date = new Date(dateStr)
    if (isNaN(date.getTime())) return '无效日期'
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch {
    return '日期错误'
  }
}

// 复制到剪贴板
const copyToClipboard = async (text: string) => {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败')
  }
}

// 刷新授权信息
const refreshLicenseInfo = async () => {
  loading.value = true
  apiError.value = false
  
  try {
    await Promise.all([
      fetchLicenseInfo(),
      fetchHardwareInfo(),
      fetchModulePermissions()
    ])
    
    // 如果所有数据都是默认值，说明API调用可能失败
    if (licenseInfo.value?.licenseCode === '未获取' || 
        licenseInfo.value?.licenseCode === '演示版本') {
      apiError.value = true
      console.warn('⚠️ 检测到使用默认数据，可能是后端服务未启动或授权未配置')
    } else {
      // 只在手动刷新时显示成功提示，首次加载不提示
      console.log('✅ 授权信息加载成功')
    }
  } catch (error: any) {
    apiError.value = true
    console.warn('⚠️ 授权信息加载失败，显示默认数据:', error.message || '未知错误')
  } finally {
    loading.value = false
  }
}

// 获取授权信息
const fetchLicenseInfo = async () => {
  try {
    const response = await getLicenseInfo()
    console.log('授权信息响应:', response)
    
    // 统一使用标准响应格式：{code: 200, message: "...", data: {...}}
    if (response.data && response.data.code === 200) {
      licenseInfo.value = response.data.data
      console.log('✅ 授权信息获取成功')
    } else {
      // 使用默认数据（不设置日期）
      licenseInfo.value = {
        licenseCode: '演示版本',
        companyName: '肇新科技',
        contactPerson: '未设置',
        contactPhone: '未设置',
        startDate: undefined,  // 不设置日期
        expireDate: undefined,  // 不设置日期
        hardwareBound: false,
        authorizedModules: [],
        maxUsers: 10
      }
    }
    
    // 验证授权状态
    try {
      const validateResp = await validateLicense()
      if (validateResp.data && validateResp.data.code === 200) {
        licenseStatus.value.valid = true
      }
    } catch (validateError) {
      console.warn('验证授权失败:', validateError)
      licenseStatus.value.valid = false
    }
  } catch (error) {
    console.warn('⚠️ 授权API未响应，使用默认数据:', error)
    // 使用默认数据，避免空白页面（不设置日期，显示为"未设置"）
    licenseInfo.value = {
      licenseCode: '未获取',
      companyName: '未获取',
      contactPerson: '未设置',
      contactPhone: '未设置',
      startDate: undefined,  // 不设置日期
      expireDate: undefined,  // 不设置日期
      hardwareBound: false,
      authorizedModules: [],
      maxUsers: 1
    }
    licenseStatus.value.valid = false
  }
}

// 获取硬件信息
const fetchHardwareInfo = async () => {
  try {
    const response = await getHardwareInfo()
    console.log('硬件信息响应:', response)
    
    // 统一使用标准响应格式：{code: 200, message: "...", data: {...}}
    if (response.data && response.data.code === 200) {
      hardwareInfo.value = response.data.data
      console.log('✅ 硬件信息获取成功:', response.data.data)
    } else {
      // 使用默认数据
      hardwareInfo.value = {
        osName: '未获取',
        mainBoardSerial: '未获取',
        cpuSerial: '未获取',
        macAddress: []
      }
    }
  } catch (error) {
    console.warn('⚠️ 硬件信息API未响应，使用默认数据:', error)
    // 使用默认数据，避免空白
    hardwareInfo.value = {
      osName: '无法获取',
      mainBoardSerial: '无法获取',
      cpuSerial: '无法获取',
      macAddress: []
    }
  }
}

// 获取模块权限
const fetchModulePermissions = async () => {
  try {
    const moduleCodes = MODULE_DEFINITIONS.map(m => m.code)
    const response = await checkModules(moduleCodes)
    console.log('模块权限响应:', response)
    
    // 统一使用标准响应格式：{code: 200, message: "...", data: {...}}
    if (response.data && response.data.code === 200) {
      const permissions = response.data.data || {}
      allModules.value = MODULE_DEFINITIONS.map(def => ({
        ...def,
        authorized: permissions[def.code] || false
      }))
      console.log('✅ 模块权限获取成功')
    } else {
      // 默认所有模块未授权
      allModules.value = MODULE_DEFINITIONS.map(def => ({
        ...def,
        authorized: false
      }))
    }
  } catch (error) {
    console.warn('⚠️ 模块权限API未响应，默认所有模块未授权:', error)
    // 默认所有模块未授权，但仍然显示列表
    allModules.value = MODULE_DEFINITIONS.map(def => ({
      ...def,
      authorized: false
    }))
  }
}

// 下载机器信息
const downloadMachineInfo = async () => {
  try {
    // 获取最新的硬件信息
    const response = await getHardwareInfo()
    
    if (!response.data || response.data.code !== 200) {
      ElMessage.warning('无法获取硬件信息，请稍后再试')
      return
    }
    
    const data = response.data.data
    
    // 构造机器信息JSON
    const machineInfo = {
      osName: data.osName || '',
      mainBoardSerial: data.mainBoardSerial || '',
      cpuSerial: data.cpuSerial || '',
      macAddress: (data.macAddress || []).filter((mac: string | null) => 
        mac !== null && mac !== undefined && mac !== ''
      ),
      exportTime: new Date().toISOString(),
      note: '此文件用于生成授权码，请将此文件提供给授权服务商'
    }
    
    // 创建文件并下载
    const jsonStr = JSON.stringify(machineInfo, null, 2)
    const blob = new Blob([jsonStr], { type: 'application/json;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `machine-info-${new Date().getTime()}.json`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
    
    ElMessage.success('机器信息已下载，请将此文件提供给授权服务商')
  } catch (error) {
    console.error('下载机器信息失败:', error)
    ElMessage.error('下载机器信息失败，请重试')
  }
}

// 初始化
onMounted(async () => {
  await refreshLicenseInfo()
})
</script>

<style scoped lang="scss">
.license-view {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 20px;

  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    
    .header-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 18px;
      font-weight: 600;
    }
    
    .header-actions {
      display: flex;
      gap: 8px;
    }
  }

  .header-content {
    color: #606266;
    font-size: 14px;

    .api-error-alert {
      margin-top: 16px;

      ul {
        margin: 8px 0;
        padding-left: 20px;
      }

      li {
        margin: 4px 0;
      }

      .console-hint {
        margin-top: 8px;
        font-size: 12px;
        color: #909399;
      }
    }
  }
}

.status-cards {
  margin-bottom: 20px;

  .status-item {
    display: flex;
    align-items: center;
    gap: 16px;

    .status-icon {
      font-size: 40px;
      
      &.success {
        color: #67c23a;
      }
      
      &.error {
        color: #f56c6c;
      }
      
      &.warning {
        color: #e6a23c;
      }
      
      &.info {
        color: #909399;
      }
      
      &.primary {
        color: #409eff;
      }
    }

    .status-text {
      flex: 1;

      .status-label {
        font-size: 12px;
        color: #909399;
        margin-bottom: 4px;
      }

      .status-value {
        font-size: 20px;
        font-weight: 600;
        color: #303133;
      }
    }
  }
}

.info-card {
  margin-bottom: 20px;

  .card-header {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 16px;
    font-weight: 600;
  }

  .loading-container {
    padding: 20px;
  }

  .info-content {
    .info-item {
      display: flex;
      align-items: flex-start;
      padding: 12px 0;
      border-bottom: 1px solid #f0f0f0;

      &:last-child {
        border-bottom: none;
      }

      .info-label {
        flex-shrink: 0;
        width: 120px;
        color: #606266;
        font-size: 14px;
      }

      .info-value {
        color: #303133;
        font-size: 14px;

        &.monospace {
          font-family: 'Courier New', monospace;
          font-size: 13px;
        }

        &.expire-warning {
          color: #e6a23c;
          font-weight: 600;
        }
      }

      .info-value-wrapper {
        flex: 1;
        display: flex;
        align-items: center;
        gap: 8px;

        .info-value {
          flex: 0 1 auto;
        }

        .el-button {
          flex-shrink: 0;
        }
      }

      .info-value-list {
        flex: 1;

        .mac-item {
          display: flex;
          align-items: center;
          gap: 8px;
          margin-bottom: 8px;

          &:last-child {
            margin-bottom: 0;
          }

          .monospace {
            font-family: 'Courier New', monospace;
            font-size: 13px;
          }

          .el-button {
            flex-shrink: 0;
          }
        }
      }
    }
  }
}

.modules-card {
  margin-bottom: 20px;

  .card-header {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 16px;
    font-weight: 600;
  }

  .modules-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 16px;
    padding: 10px 0;

    .module-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 16px;
      border-radius: 8px;
      border: 2px solid #e4e7ed;
      transition: all 0.3s;

      &.authorized {
        border-color: #67c23a;
        background-color: #f0f9ff;
      }

      &.unauthorized {
        border-color: #dcdfe6;
        background-color: #f5f7fa;
        opacity: 0.7;
      }

      .module-icon {
        font-size: 32px;

        .icon-authorized {
          color: #67c23a;
        }

        .icon-unauthorized {
          color: #909399;
        }
      }

      .module-info {
        flex: 1;

        .module-name {
          font-size: 14px;
          font-weight: 600;
          color: #303133;
          margin-bottom: 6px;
        }

        .module-status {
          font-size: 12px;
        }
      }
    }
  }
}

.ml-2 {
  margin-left: 8px;
}
</style>

