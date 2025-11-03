<template>
  <div class="ai-template-generator-page">
    <!-- 使用 PageHeader 组件 -->
    <PageHeader 
      title="AI 模板生成助手" 
      description="通过AI技术自动分析文档内容，快速生成高质量的提取模板，提升模板创建效率"
      :icon="Refresh"
      tag="AI辅助"
      tag-type="warning"
    />

    <!-- 重要提示 -->
    <el-alert
      type="warning"
      :closable="false"
      show-icon
      class="important-notice">
      <div class="notice-content">
        AI 生成的模板需要<strong>人工二次确认和优化</strong>，包括：关键词准确性、正则表达式、提取规则等。
      </div>
    </el-alert>

    <!-- 步骤指示器 -->
    <el-card class="steps-card">
      <el-steps :active="currentStep - 1" align-center finish-status="success" class="steps-indicator">
        <el-step title="上传文档">
          <template #icon><el-icon><Upload /></el-icon></template>
        </el-step>
        <el-step title="准备提示词">
          <template #icon><el-icon><Edit /></el-icon></template>
        </el-step>
        <el-step title="导入JSON">
          <template #icon><el-icon><DocumentAdd /></el-icon></template>
        </el-step>
        <el-step title="完成">
          <template #icon><el-icon><SuccessFilled /></el-icon></template>
        </el-step>
      </el-steps>
    </el-card>

    <!-- 步骤1: 上传文档 -->
    <el-card v-show="currentStep === 1" class="step-card">
      <template #header>
        <div class="card-header">
          <el-icon><Upload /></el-icon>
          <span>步骤1: 上传合同文档</span>
        </div>
      </template>

      <el-alert
        type="info"
        :closable="false"
        class="step-alert">
        <template #title>
          <span class="alert-title">📄 第一步：上传您的合同文档</span>
        </template>
        系统将使用先进的OCR 技术自动识别文档内容，为 AI 分析做准备。<br/>
        💡 <strong>小提示：</strong>文档内容越完整，AI 生成的模板质量越高
      </el-alert>
      
      <el-upload
        ref="upload"
        class="upload-dragger"
        drag
        :on-change="handleFileChange"
        :show-file-list="false"
        :auto-upload="false"
        :disabled="extracting"
        accept=".pdf">
        <div v-if="extracting" class="uploading-content">
          <el-icon class="is-loading loading-icon">
            <Loading />
          </el-icon>
          <div class="uploading-text">正在提取文档内容，请稍候...</div>
        </div>
        <template v-else>
          <el-icon class="el-icon--upload"><Upload /></el-icon>
          <div class="el-upload__text">
            拖拽 PDF 文件到这里，或<em>点击选择文件</em>
          </div>
          <div class="el-upload__tip">
            ✓ 支持 PDF 格式 &nbsp;&nbsp; ✓ 建议文件大小 &lt; 20MB &nbsp;&nbsp; ✓ 推荐包含完整合同内容
          </div>
        </template>
      </el-upload>

      <div v-if="documentText" class="text-preview">
        <el-divider content-position="left">
          <el-icon><Document /></el-icon>
          <span>文档内容预览</span>
        </el-divider>
        
        <el-input
          type="textarea"
          :rows="10"
          v-model="documentText"
          readonly
          class="preview-textarea">
        </el-input>
        
        <div class="preview-info">
          <el-tag>文件名: {{ fileName }}</el-tag>
          <el-tag type="info">字符数: {{ documentText.length }}</el-tag>
          <el-tag type="success">预估页数: {{ pageCount }}</el-tag>
        </div>
        
        <div class="step-actions">
          <el-button size="large" type="primary" @click="nextStep">
            下一步：准备AI提示词
            <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 步骤2: 准备提示词 -->
    <el-card v-show="currentStep === 2" class="step-card">
      <template #header>
        <div class="card-header">
          <el-icon><Edit /></el-icon>
          <span>步骤2: 准备AI提示词</span>
        </div>
      </template>

      <el-alert
        type="info"
        :closable="false"
        class="step-alert">
        <template #title>
          <span class="alert-title">🤖 第二步：配置 AI 提示词</span>
        </template>
        选择预设模板，输入需要提取的字段，系统会自动生成完整的 AI 提示词。<br/>
        💡 <strong>小提示：</strong>复制生成的提示词到您喜欢的 AI 工具（如 ChatGPT、通义千问等）
      </el-alert>

      <el-form label-width="140px" label-position="left" class="prompt-form">
        <el-form-item label="🎯 提示词模板">
          <el-tag size="large" type="success" effect="plain" class="template-tag">
            标准提取模板 - 支持文本字段和表格数据提取
          </el-tag>
        </el-form-item>

        <el-form-item label="📝 需要提取的字段">
          <el-input
            type="textarea"
            :rows="8"
            v-model="fieldsList"
            @input="generateFullPrompt"
            placeholder="请每行输入一个字段名称，例如：&#10;&#10;合同编号&#10;甲方名称&#10;乙方名称&#10;合同金额&#10;签订日期&#10;付款方式&#10;有效期限"
            class="fields-textarea">
          </el-input>
          <div class="field-hint-text">
            💡 每行输入一个字段名称（中文），AI 会自动生成对应的英文字段名和提取规则
          </div>
        </el-form-item>

        <el-divider content-position="left">
          <el-icon><DocumentCopy /></el-icon>
          <span>生成的完整提示词</span>
        </el-divider>

        <el-form-item label="🤖 完整AI提示词">
          <el-input
            type="textarea"
            :rows="15"
            v-model="fullPrompt"
            readonly
            class="prompt-textarea"
            placeholder="配置好字段后，这里会自动生成完整的 AI 提示词">
          </el-input>
          <div class="prompt-info">
            <el-tag type="success" size="small" effect="dark">
              字符数：{{ fullPrompt.length }} 字符
            </el-tag>
            <el-tag type="info" size="small">
              准备就绪 ✓
            </el-tag>
          </div>
        </el-form-item>
      </el-form>

      <div class="step-actions">
        <el-button size="large" @click="prevStep">
          <el-icon><ArrowLeft /></el-icon>
          上一步
        </el-button>
        <el-button size="large" type="success" @click="copyPrompt" :disabled="!fullPrompt">
          <el-icon><DocumentCopy /></el-icon>
          复制提示词到剪贴板
        </el-button>
        <el-button size="large" type="primary" @click="nextStep">
          下一步：导入JSON模板
          <el-icon><ArrowRight /></el-icon>
        </el-button>
      </div>
    </el-card>

    <!-- 步骤3: 导入JSON -->
    <el-card v-show="currentStep === 3" class="step-card">
      <template #header>
        <div class="card-header">
          <el-icon><DocumentAdd /></el-icon>
          <span>步骤3: 导入AI生成的JSON</span>
        </div>
      </template>

      <el-alert
        type="info"
        :closable="false"
        class="step-alert">
        <template #title>
          <span class="alert-title">📋 第三步：导入 AI 生成的模板</span>
        </template>
        从 AI 工具获取生成的 JSON 内容，粘贴到下方输入框，然后点击"验证JSON格式"按钮。
      </el-alert>

      <el-tabs v-model="jsonTab" class="json-tabs">
        <el-tab-pane label="粘贴 AI 生成的 JSON" name="input">
          <el-input
            type="textarea"
            :rows="18"
            v-model="aiGeneratedJSON"
            placeholder="请粘贴AI生成的JSON内容..."
            class="json-textarea">
          </el-input>
        </el-tab-pane>
        <el-tab-pane label="📋 正确格式示例" name="example">
          <el-input
            type="textarea"
            :rows="18"
            :value="jsonExample"
            readonly
            class="json-textarea readonly-textarea">
          </el-input>
          <el-button 
            type="primary" 
            size="small" 
            @click="copyExample"
            class="copy-example-btn">
            <el-icon><DocumentCopy /></el-icon>
            复制示例到输入框
          </el-button>
        </el-tab-pane>
      </el-tabs>

      <!-- 验证结果 -->
      <div v-if="validationResult" class="validation-result">
        <el-alert
          :title="validationResult.valid ? '✓ 格式验证通过' : '✗ 格式验证失败'"
          :type="validationResult.valid ? 'success' : 'error'"
          show-icon
          :closable="false">
          <ul v-if="validationResult.errors && validationResult.errors.length > 0">
            <li v-for="(error, index) in validationResult.errors" :key="index">{{ error }}</li>
          </ul>
        </el-alert>

        <el-alert
          v-if="validationResult.warnings && validationResult.warnings.length > 0"
          title="⚠ 注意事项"
          type="warning"
          show-icon
          :closable="false"
          class="warnings-alert">
          <ul>
            <li v-for="(warning, index) in validationResult.warnings" :key="index">{{ warning }}</li>
          </ul>
        </el-alert>
      </div>

      <div class="step-actions">
        <el-button size="large" @click="prevStep">
          <el-icon><ArrowLeft /></el-icon>
          上一步
        </el-button>
        <el-button size="large" type="warning" @click="validateJSON" :disabled="!aiGeneratedJSON">
          <el-icon><Check /></el-icon>
          验证JSON格式
        </el-button>
        <el-button
          size="large"
          type="primary"
          @click="importTemplate"
          :loading="importing"
          :disabled="!validationResult || !validationResult.valid">
          <el-icon v-if="!importing"><UploadFilled /></el-icon>
          {{ importing ? '正在导入...' : '导入模板到系统' }}
        </el-button>
      </div>
    </el-card>

    <!-- 步骤4: 完成 -->
    <el-card v-show="currentStep === 4" class="step-card success-card">
      <template #header>
        <div class="card-header">
          <el-icon><SuccessFilled /></el-icon>
          <span>步骤4: 导入成功</span>
        </div>
      </template>

      <el-result
        icon="success"
        title="🎉 模板创建成功！"
        :subTitle="'模板名称: ' + (importResult ? importResult.templateName : '')">
        <template #extra>
          <div class="result-info">
            <p class="result-main-text">
              <strong>✅ 模板已成功导入系统</strong>
            </p>
            <p class="result-detail">
              <el-icon class="success-icon"><Check /></el-icon>
              字段数量: <strong>{{ importResult ? importResult.fieldCount : 0 }} 个</strong>
            </p>
            <div v-if="importResult && importResult.warnings && importResult.warnings.length > 0" class="warnings-section">
              <el-tag type="warning" size="large" effect="plain">⚠️ 需要注意</el-tag>
              <ul class="warnings-list">
                <li v-for="(warning, index) in importResult.warnings" :key="index">{{ warning }}</li>
              </ul>
            </div>
          </div>

          <div class="result-actions">
            <el-button type="primary" size="large" @click="goToEditor">
              <el-icon><Edit /></el-icon>
              返回
            </el-button>
            <el-button type="success" size="large" @click="resetGenerator">
              <el-icon><RefreshLeft /></el-icon>
              继续生成新模板
            </el-button>
          </div>
        </template>
      </el-result>
    </el-card>

    <!-- 使用说明对话框 -->
    <el-dialog title="使用说明" :visible.sync="showGuide" width="60%">
      <div v-html="usageGuideHtml" class="usage-guide"></div>
    </el-dialog>
  </div>
</template>

<script>
// @ts-nocheck
import { markRaw } from 'vue'
import axios from 'axios'
import PageHeader from '@/components/common/PageHeader.vue'
import { 
  Upload, 
  Edit, 
  DocumentAdd, 
  SuccessFilled, 
  ArrowRight,
  ArrowLeft,
  DocumentCopy, 
  Check, 
  UploadFilled, 
  Refresh as RefreshIcon, 
  RefreshLeft,
  Loading,
  Document
} from '@element-plus/icons-vue'

export default {
  name: 'AITemplateGenerator',
  components: {
    PageHeader,
    Upload,
    Edit,
    DocumentAdd,
    SuccessFilled,
    ArrowRight,
    ArrowLeft,
    DocumentCopy,
    Check,
    UploadFilled,
    Refresh: RefreshIcon,
    RefreshLeft,
    Loading,
    Document
  },
  computed: {
    Refresh() {
      return markRaw(RefreshIcon)
    }
  },
  data() {
    return {
      currentStep: 1,
      fileName: '',
      documentText: '',
      pageCount: 0,
      selectedPromptId: 'default',
      promptTemplates: [],
      fieldsList: '合同编号\n甲方名称\n乙方名称\n合同金额\n签订日期',
      fullPrompt: '',
      aiGeneratedJSON: '',
      jsonTab: 'input',
      jsonExample: `{
  "templateName": "采购合同模板",
  "description": "标准采购合同信息提取",
  "fields": [
    {
      "fieldName": "contractNo",
      "fieldLabel": "合同编号",
      "fieldType": "text",
      "required": true,
      "extractRules": {
        "type": "keyword",
        "keyword": "合同编号：",
        "offset": 0,
        "length": 30
      }
    },
    {
      "fieldName": "contractNoRegex",
      "fieldLabel": "合同编号（正则容错版）",
      "fieldType": "text",
      "required": true,
      "extractRules": {
        "type": "regex",
        "pattern": "合同编号[：:\\\\s]*([A-Z0-9\\\\-]+)"
      },
      "note": "使用正则匹配多种冒号格式"
    },
    {
      "fieldName": "partyA",
      "fieldLabel": "甲方名称",
      "fieldType": "text",
      "required": true,
      "extractRules": {
        "type": "keyword",
        "keyword": "甲方：",
        "offset": 0,
        "length": 50
      }
    },
    {
      "fieldName": "partyBAddress",
      "fieldLabel": "乙方地址（第2个）",
      "fieldType": "text",
      "required": false,
      "extractRules": {
        "type": "keyword",
        "keyword": "地址：",
        "offset": 0,
        "length": 100,
        "occurrence": 2
      },
      "note": "当"地址："出现多次时，提取第2个"
    },
    {
      "fieldName": "contractAmount",
      "fieldLabel": "合同金额",
      "fieldType": "text",
      "required": true,
      "extractRules": {
        "type": "keyword",
        "keyword": "合同金额：",
        "offset": 0,
        "length": 50,
        "occurrence": 1,
        "pattern": "\\\\d+(?:,\\\\d{3})*(?:\\\\.\\\\d{2})?"
      },
      "note": "使用关键词+正则提取，支持千分位格式"
    },
    {
      "fieldName": "productList",
      "fieldLabel": "货物清单",
      "fieldType": "table",
      "required": false,
      "extractRules": {
        "type": "table",
        "tableRules": {
          "tableKeyword": "货物清单",
          "columns": ["序号", "名称", "规格型号", "数量", "单价", "金额"]
        }
      },
      "note": "表格类型，自动提取整张表格数据"
    }
  ]
}`,
      validationResult: null,
      importing: false,
      importResult: null,
      showGuide: false,
      usageGuideHtml: '',
      extracting: false
    }
  },
  mounted() {
    this.loadPromptTemplates()
  },
  methods: {
    async loadPromptTemplates() {
      try {
        const response = await axios.get('/api/rule-extract/ai-template/prompt-templates')
        if (response.data.success) {
          this.promptTemplates = response.data.data.templates
          this.usageGuideHtml = response.data.data.usageGuide.replace(/\n/g, '<br>')
          this.generateFullPrompt()
        }
      } catch (error) {
        this.$message.error('加载提示词模板失败: ' + error.message)
      }
    },

    handleFileChange(file) {
      if (file && file.raw) {
        this.handleUpload(file.raw)
      }
    },

    async handleUpload(file) {
      this.extracting = true
      const formData = new FormData()
      formData.append('file', file)
      formData.append('format', 'plain')

      try {
        const response = await axios.post('/api/rule-extract/ai-template/extract-document-text', formData)
        if (response.data.success) {
          this.fileName = response.data.data.fileName
          this.documentText = response.data.data.textContent
          this.pageCount = response.data.data.pageCount
          this.$message.success('文档提取成功')
        } else {
          this.$message.error(response.data.message || '文档提取失败')
        }
      } catch (error) {
        console.error('文档提取错误:', error)
        this.$message.error('文档提取失败: ' + (error.response?.data?.message || error.message))
      } finally {
        this.extracting = false
      }
    },

    onPromptTemplateChange() {
      this.generateFullPrompt()
    },

    generateFullPrompt() {
      const template = this.promptTemplates.find(t => t.id === this.selectedPromptId)
      if (template) {
        this.fullPrompt = template.promptText
          .replace('{FIELD_LIST}', this.fieldsList)
          .replace('{DOCUMENT_CONTENT}', this.documentText)
      }
    },

    async copyPrompt() {
      try {
        await navigator.clipboard.writeText(this.fullPrompt)
        this.$message.success('提示词已复制到剪贴板！请前往AI工具使用')
      } catch (error) {
        this.$message.error('复制失败，请手动复制')
      }
    },

    copyExample() {
      this.aiGeneratedJSON = this.jsonExample
      this.jsonTab = 'input'
      this.$message.success('示例已复制到输入框，您可以参考修改')
    },

    async validateJSON() {
      if (!this.aiGeneratedJSON.trim()) {
        this.$message.warning('请先粘贴JSON内容')
        return
      }

      try {
        const response = await axios.post('/api/rule-extract/ai-template/validate-json', {
          jsonContent: this.aiGeneratedJSON
        })
        if (response.data.success) {
          this.validationResult = response.data.data
          if (this.validationResult.valid) {
            this.$message.success('JSON格式验证通过')
          } else {
            this.$message.error('JSON格式验证失败，请查看错误提示')
          }
        }
      } catch (error) {
        this.$message.error('验证失败: ' + error.message)
      }
    },

    async importTemplate() {
      if (!this.validationResult || !this.validationResult.valid) {
        this.$message.warning('请先验证JSON格式')
        return
      }

      this.importing = true
      try {
        const response = await axios.post('/api/rule-extract/ai-template/import-template', {
          jsonContent: this.aiGeneratedJSON
        })
        if (response.data.success) {
          this.importResult = response.data.data
          this.$message.success('模板导入成功！')
          this.currentStep = 4
        } else {
          this.$message.error(response.data.message)
        }
      } catch (error) {
        this.$message.error('导入失败: ' + error.message)
      } finally {
        this.importing = false
      }
    },

    goToEditor() {
      if (this.importResult && this.importResult.templateId) {
        this.$router.push('/rule-extract/templates?id=' + this.importResult.templateId)
      } else {
        this.$router.push('/rule-extract/templates')
      }
    },

    resetGenerator() {
      this.currentStep = 1
      this.documentText = ''
      this.fileName = ''
      this.aiGeneratedJSON = ''
      this.validationResult = null
      this.importResult = null
      this.fieldsList = '合同编号\n甲方名称\n乙方名称\n合同金额\n签订日期'
    },

    nextStep() {
      if (this.currentStep < 4) {
        this.currentStep++
        if (this.currentStep === 2) {
          this.generateFullPrompt()
        }
      }
    },

    prevStep() {
      if (this.currentStep > 1) {
        this.currentStep--
      }
    }
  }
}
</script>

<style scoped lang="scss">
.ai-template-generator-page {
  padding: var(--zx-spacing-md);
}

/* 重要提示 */
.important-notice {
  margin-bottom: var(--zx-spacing-md);
  border-radius: var(--zx-radius-md);
  border-left: 4px solid var(--zx-warning);
  
  .notice-title {
    font-size: var(--zx-font-base);
    font-weight: var(--zx-font-semibold);
    color: var(--zx-warning-dark-2);
  }
  
  .notice-content {
    margin-top: var(--zx-spacing-sm);
    font-size: var(--zx-font-sm);
    line-height: var(--zx-leading-relaxed);
    color: var(--zx-text-regular);
    
    strong {
      color: var(--zx-warning-dark-2);
      font-weight: var(--zx-font-semibold);
    }
    
    ul {
      margin: var(--zx-spacing-sm) 0;
      padding-left: var(--zx-spacing-xl);
      
      li {
        margin: var(--zx-spacing-xs) 0;
        line-height: var(--zx-leading-relaxed);
        
        strong {
          color: var(--zx-primary);
        }
      }
    }
  }
  
  :deep(.el-alert__content) {
    width: 100%;
  }
}

/* 步骤指示器卡片 */
.steps-card {
  margin-bottom: var(--zx-spacing-md);
  border-radius: var(--zx-radius-md);
  box-shadow: var(--zx-shadow-sm);
  transition: box-shadow var(--zx-transition-base);
  
  &:hover {
    box-shadow: var(--zx-shadow-md);
  }
  
  :deep(.el-card__body) {
    padding: var(--zx-spacing-xl);
  }
}

.steps-indicator {
  :deep(.el-step__title) {
    font-size: var(--zx-font-base);
    font-weight: var(--zx-font-medium);
  }
  
  :deep(.el-step.is-finish .el-step__icon) {
    background: var(--zx-primary);
    border-color: var(--zx-primary);
  }
  
  :deep(.el-step.is-process .el-step__icon) {
    background: var(--zx-primary);
    border-color: var(--zx-primary);
  }
}

/* 步骤卡片 */
.step-card {
  margin-bottom: var(--zx-spacing-md);
  border-radius: var(--zx-radius-md);
  box-shadow: var(--zx-shadow-sm);
  transition: all var(--zx-transition-base);
  
  &:hover {
    box-shadow: var(--zx-shadow-md);
  }
  
  .card-header {
    display: flex;
    align-items: center;
    gap: var(--zx-spacing-sm);
    font-weight: var(--zx-font-semibold);
    font-size: var(--zx-font-base);
    
    .el-icon {
      font-size: 18px;
      color: var(--zx-primary);
    }
  }
  
  :deep(.el-card__body) {
    padding: var(--zx-spacing-xl);
  }
}

/* Alert 样式 */
.step-alert {
  margin-bottom: var(--zx-spacing-lg);
  border-radius: var(--zx-radius-sm);
  
  .alert-title {
    font-size: var(--zx-font-base);
    font-weight: var(--zx-font-medium);
  }
  
  :deep(.el-alert__content) {
    font-size: var(--zx-font-sm);
    line-height: var(--zx-leading-relaxed);
  }
}

/* 上传区域 */
.upload-dragger {
  margin: var(--zx-spacing-xl) 0;
  
  :deep(.el-upload-dragger) {
    padding: var(--zx-spacing-2xl);
    border-radius: var(--zx-radius-md);
    transition: all var(--zx-transition-base);
    
    &:hover {
      border-color: var(--zx-primary);
      background-color: var(--zx-primary-light-9);
    }
    
    .el-icon--upload {
      font-size: 48px;
      color: var(--zx-text-placeholder);
      margin-bottom: var(--zx-spacing-md);
    }
    
    .el-upload__text {
      font-size: var(--zx-font-base);
      color: var(--zx-text-regular);
      
      em {
        color: var(--zx-primary);
        font-style: normal;
        font-weight: var(--zx-font-medium);
      }
    }
    
    .el-upload__tip {
      font-size: var(--zx-font-sm);
      color: var(--zx-text-secondary);
      margin-top: var(--zx-spacing-sm);
    }
  }
}

.uploading-content {
  padding: var(--zx-spacing-2xl);
  
  .loading-icon {
    font-size: 50px;
    color: var(--zx-primary);
  }
  
  .uploading-text {
    margin-top: var(--zx-spacing-md);
    color: var(--zx-text-regular);
    font-size: var(--zx-font-base);
  }
}

/* 文档预览 */
.text-preview {
  margin-top: var(--zx-spacing-xl);
  animation: fadeIn 0.3s ease-out;
  
  .el-divider {
    margin: var(--zx-spacing-xl) 0;
    
    :deep(.el-divider__text) {
      display: flex;
      align-items: center;
      gap: var(--zx-spacing-xs);
      font-size: var(--zx-font-base);
      font-weight: var(--zx-font-medium);
      color: var(--zx-text-regular);
    }
  }
  
  .preview-textarea {
    margin-bottom: var(--zx-spacing-md);
    
    :deep(textarea) {
      font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
      line-height: var(--zx-leading-relaxed);
      background: var(--zx-bg-light);
      font-size: var(--zx-font-sm);
    }
  }
  
  .preview-info {
    margin: var(--zx-spacing-md) 0;
    display: flex;
    gap: var(--zx-spacing-sm);
    flex-wrap: wrap;
  }
}

/* 表单样式 */
.prompt-form {
  .template-tag {
    padding: var(--zx-spacing-sm) var(--zx-spacing-md);
    font-size: var(--zx-font-sm);
  }
  
  .fields-textarea,
  .prompt-textarea {
    :deep(textarea) {
      font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
      font-size: var(--zx-font-sm);
      line-height: var(--zx-leading-relaxed);
    }
  }
  
  .field-hint-text {
    margin-top: var(--zx-spacing-sm);
    padding: var(--zx-spacing-sm);
    font-size: var(--zx-font-sm);
    color: var(--zx-text-secondary);
    line-height: var(--zx-leading-relaxed);
  }
  
  .prompt-info {
    margin-top: var(--zx-spacing-sm);
    display: flex;
    gap: var(--zx-spacing-sm);
  }
}

/* JSON 标签页 */
.json-tabs {
  margin-bottom: var(--zx-spacing-lg);
  
  .json-textarea {
    :deep(textarea) {
      font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
      font-size: var(--zx-font-sm);
      line-height: var(--zx-leading-relaxed);
      background: var(--zx-bg-light);
    }
  }
  
  .readonly-textarea {
    :deep(textarea) {
      background: var(--zx-bg-light);
      cursor: default;
    }
  }
  
  .copy-example-btn {
    margin-top: var(--zx-spacing-sm);
  }
}

/* 验证结果 */
.validation-result {
  margin-bottom: var(--zx-spacing-lg);
  
  .el-alert {
    border-radius: var(--zx-radius-sm);
    margin-bottom: var(--zx-spacing-sm);
  }
  
  .warnings-alert {
    margin-top: var(--zx-spacing-sm);
  }
  
  ul {
    margin: var(--zx-spacing-sm) 0;
    padding-left: var(--zx-spacing-lg);
    
    li {
      margin: var(--zx-spacing-xs) 0;
      line-height: var(--zx-leading-relaxed);
      font-size: var(--zx-font-sm);
    }
  }
}

/* 步骤操作按钮 */
.step-actions {
  margin-top: var(--zx-spacing-xl);
  padding-top: var(--zx-spacing-lg);
  border-top: 1px solid var(--zx-border-lighter);
  text-align: center;
  display: flex;
  gap: var(--zx-spacing-sm);
  justify-content: center;
  flex-wrap: wrap;
  
  .el-button {
    padding: var(--zx-spacing-sm) var(--zx-spacing-lg);
    font-size: var(--zx-font-base);
    border-radius: var(--zx-radius-sm);
    font-weight: var(--zx-font-medium);
    transition: all var(--zx-transition-fast);
  }
}

/* 成功卡片 */
.success-card {
  background: var(--zx-success-light-9);
  border: 1px solid var(--zx-success-light-5);
  
  .card-header {
    color: var(--zx-success-dark-2);
    
    .el-icon {
      color: var(--zx-success);
      animation: pulse 2s ease infinite;
    }
  }
  
  .result-info {
    text-align: left;
    margin: var(--zx-spacing-lg) 0;
    
    .result-main-text {
      margin: var(--zx-spacing-md) 0;
      font-size: var(--zx-font-lg);
      color: var(--zx-success-dark-2);
      font-weight: var(--zx-font-semibold);
    }
    
    .result-detail {
      margin: var(--zx-spacing-sm) 0;
      font-size: var(--zx-font-base);
      color: var(--zx-text-regular);
      display: flex;
      align-items: center;
      gap: var(--zx-spacing-xs);
      
      .success-icon {
        color: var(--zx-success);
      }
    }
    
    .warnings-section {
      margin-top: var(--zx-spacing-md);
      
      .warnings-list {
        margin-top: var(--zx-spacing-sm);
        padding: var(--zx-spacing-md);
        padding-left: var(--zx-spacing-xl);
        background: var(--zx-warning-light-9);
        border-left: 3px solid var(--zx-warning);
        border-radius: var(--zx-radius-sm);
        
        li {
          margin: var(--zx-spacing-xs) 0;
          line-height: var(--zx-leading-relaxed);
          font-size: var(--zx-font-sm);
          color: var(--zx-warning-dark-2);
        }
      }
    }
  }
  
  .result-actions {
    margin-top: var(--zx-spacing-xl);
    display: flex;
    gap: var(--zx-spacing-md);
    justify-content: center;
    flex-wrap: wrap;
  }
}

/* 动画 */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.1);
  }
}

/* code 标签样式 */
code {
  background: var(--zx-bg-light);
  color: var(--zx-danger);
  padding: 2px 6px;
  border-radius: var(--zx-radius-xs);
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 0.9em;
  border: 1px solid var(--zx-border-lighter);
}

/* 使用说明对话框 */
.usage-guide {
  line-height: var(--zx-leading-relaxed);
  color: var(--zx-text-regular);
  font-size: var(--zx-font-sm);
  
  :deep(h3) {
    color: var(--zx-text-primary);
    margin-top: var(--zx-spacing-lg);
    margin-bottom: var(--zx-spacing-sm);
    font-size: var(--zx-font-lg);
    font-weight: var(--zx-font-semibold);
  }
  
  :deep(h4) {
    color: var(--zx-text-regular);
    margin-top: var(--zx-spacing-md);
    margin-bottom: var(--zx-spacing-sm);
    font-size: var(--zx-font-base);
    font-weight: var(--zx-font-medium);
  }
  
  :deep(p) {
    margin: var(--zx-spacing-sm) 0;
    line-height: var(--zx-leading-relaxed);
  }
  
  :deep(ul), :deep(ol) {
    margin: var(--zx-spacing-sm) 0;
    padding-left: var(--zx-spacing-xl);
    
    li {
      margin: var(--zx-spacing-xs) 0;
      line-height: var(--zx-leading-relaxed);
    }
  }
  
  :deep(strong) {
    color: var(--zx-primary);
    font-weight: var(--zx-font-semibold);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .ai-template-generator-page {
    padding: var(--zx-spacing-sm);
  }
  
  .step-card {
    :deep(.el-card__body) {
      padding: var(--zx-spacing-md);
    }
  }
  
  .step-actions {
    flex-direction: column;
    
    .el-button {
      width: 100%;
    }
  }
}
</style>

