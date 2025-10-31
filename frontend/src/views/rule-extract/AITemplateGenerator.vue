<template>
  <div class="ai-template-generator">
    <h2>
      <el-icon><Refresh /></el-icon>
      AI 模板生成助手
    </h2>

    <!-- 步骤指示器 -->
    <el-steps :active="currentStep - 1" align-center finish-status="success" class="steps">
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

    <!-- 步骤1: 上传文档 -->
    <el-card v-show="currentStep === 1" class="step-card">
      <template #header>
        <span>
          <el-icon style="vertical-align: middle; margin-right: 4px;"><Upload /></el-icon>
          步骤1: 上传合同文档
        </span>
      </template>

      <el-alert
        type="info"
        :closable="false"
        style="margin-bottom: 24px; border-radius: 12px;">
        <template #title>
          <div style="font-size: 15px; font-weight: 500;">
            📄 第一步：上传您的合同文档
          </div>
        </template>
        <div style="margin-top: 8px; line-height: 1.8;">
          系统将使用先进的 MinerU OCR 技术自动识别文档内容，为 AI 分析做准备。
          <br/>
          💡 <strong>小提示：</strong>文档内容越完整，AI 生成的模板质量越高
        </div>
      </el-alert>
      
      <el-upload
        ref="upload"
        class="upload-demo"
        drag
        :on-change="handleFileChange"
        :show-file-list="false"
        :auto-upload="false"
        :disabled="extracting"
        accept=".pdf">
        <div v-if="extracting" style="padding: 40px;">
          <el-icon class="is-loading" style="font-size: 50px; color: #409EFF;">
            <Loading />
          </el-icon>
          <div style="margin-top: 16px; color: #606266; font-size: 16px;">
            正在提取文档内容，请稍候...
          </div>
        </div>
        <template v-else>
          <el-icon class="el-icon--upload"><Upload /></el-icon>
          <div class="el-upload__text">
            拖拽 PDF 文件到这里，或<em>点击选择文件</em>
          </div>
        </template>
        <template #tip>
          <div v-if="!extracting" class="el-upload__tip" style="margin-top: 12px; color: #909399; font-size: 13px;">
            ✓ 支持 PDF 格式 &nbsp;&nbsp; ✓ 建议文件大小 &lt; 20MB &nbsp;&nbsp; ✓ 推荐包含完整合同内容
          </div>
        </template>
      </el-upload>

      <div v-if="documentText" class="text-preview">
        <el-divider content-position="left">文档内容预览</el-divider>
        
        <el-alert 
          type="warning" 
          :closable="false"
          style="margin-bottom: 16px; border-radius: 8px;">
          <template #title>
            <span style="font-size: 13px; font-weight: 500;">🔍 重要提示：请先查看文档预览！</span>
          </template>
          <div style="font-size: 12px; line-height: 1.8;">
            在步骤3粘贴 JSON 之前，请在下方预览中：<br/>
            1️⃣ <strong>搜索关键词</strong>（如"合同编号"），确认它在文档中确实存在<br/>
            2️⃣ <strong>复制准确格式</strong>（包括冒号、空格等），例如："合同编号：" 还是 "合同编号 :"<br/>
            3️⃣ <strong>注意符号差异</strong>：中文冒号 <code>：</code> vs 英文冒号 <code>:</code><br/>
            💡 如果关键词格式不匹配，即使 JSON 正确也无法提取数据！
          </div>
        </el-alert>
        
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
            <el-icon style="margin-right: 4px;"><ArrowRight /></el-icon>
            下一步：准备AI提示词
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 步骤2: 准备提示词 -->
    <el-card v-show="currentStep === 2" class="step-card">
      <template #header>
        <span>
          <el-icon style="vertical-align: middle; margin-right: 4px;"><Edit /></el-icon>
          步骤2: 准备AI提示词
        </span>
      </template>

      <el-alert
        type="info"
        :closable="false"
        style="margin-bottom: 24px; border-radius: 12px;">
        <template #title>
          <div style="font-size: 15px; font-weight: 500;">
            🤖 第二步：配置 AI 提示词
          </div>
        </template>
        <div style="margin-top: 8px; line-height: 1.8;">
          选择预设模板，输入需要提取的字段，系统会自动生成完整的 AI 提示词。
          <br/>
          💡 <strong>小提示：</strong>复制生成的提示词到您喜欢的 AI 工具（如 ChatGPT、通义千问等）
        </div>
      </el-alert>

      <el-form label-width="140px" label-position="left">
        <el-form-item label="🎯 提示词模板">
          <el-tag size="large" type="success" effect="plain" style="padding: 8px 16px; font-size: 14px;">
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
            style="font-size: 14px;">
          </el-input>
          <div class="field-hint">
            <el-alert
              type="info"
              :closable="false"
              style="margin-top: 12px; border-radius: 8px;">
              <template #title>
                <span style="font-size: 14px;">💡 字段输入技巧</span>
              </template>
              <p>✓ 每行输入一个字段名称（中文）</p>
              <p>✓ AI 会自动生成对应的英文字段名和提取规则</p>
              <p>✓ 常见字段：合同编号、甲方、乙方、金额、日期、联系人、有效期等</p>
            </el-alert>
          </div>
        </el-form-item>

        <el-divider content-position="left">
          <el-icon><DocumentCopy /></el-icon>
          <span style="margin-left: 8px;">生成的完整提示词</span>
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
            <el-tag type="info" size="small" style="margin-left: 8px;">
              准备就绪 ✓
            </el-tag>
          </div>
        </el-form-item>
      </el-form>

      <el-alert
        title="操作指引"
        type="warning"
        show-icon
        :closable="false"
        class="usage-alert">
        <div class="usage-steps">
          <p><strong>接下来的操作：</strong></p>
          <ol>
            <li>点击下方"复制提示词"按钮</li>
            <li>打开AI工具（ChatGPT、通义千问、文心一言、Kimi等）</li>
            <li>粘贴完整提示词到AI对话框</li>
            <li>等待AI生成JSON配置（通常10-30秒）</li>
            <li>复制AI返回的完整JSON内容</li>
            <li>返回本页面，点击"下一步"继续</li>
          </ol>
        </div>
      </el-alert>

      <div class="step-actions">
        <el-button size="large" @click="prevStep">
          <el-icon style="margin-right: 4px;"><ArrowRight style="transform: rotate(180deg);" /></el-icon>
          上一步
        </el-button>
        <el-button size="large" type="success" @click="copyPrompt" :disabled="!fullPrompt">
          <el-icon style="margin-right: 4px;"><DocumentCopy /></el-icon>
          复制提示词到剪贴板
        </el-button>
        <el-button size="large" type="primary" @click="nextStep">
          <el-icon style="margin-right: 4px;"><ArrowRight /></el-icon>
          下一步：导入JSON模板
        </el-button>
      </div>
    </el-card>

    <!-- 步骤3: 导入JSON -->
    <el-card v-show="currentStep === 3" class="step-card">
      <template #header>
        <span>
          <el-icon style="vertical-align: middle; margin-right: 4px;"><DocumentAdd /></el-icon>
          步骤3: 导入AI生成的JSON
        </span>
      </template>

      <el-alert
        type="info"
        :closable="false"
        style="margin-bottom: 24px; border-radius: 12px;">
        <template #title>
          <div style="font-size: 15px; font-weight: 500;">
            📋 第三步：导入 AI 生成的模板
          </div>
        </template>
        <div style="margin-top: 8px; line-height: 1.8;">
          从 AI 工具（ChatGPT、通义千问等）获取生成的 JSON 内容，粘贴到下方输入框。
          <br/>
          💡 <strong>小提示：</strong>只粘贴 JSON 代码块内容（大括号{}之间的部分），不要包含 AI 的解释文字或 markdown 标记
        </div>
      </el-alert>

      <el-alert 
        type="error" 
        :closable="false"
        style="margin-bottom: 16px; border-radius: 8px;">
        <template #title>
          <span style="font-size: 14px; font-weight: 500;">🚨 提取失败？90% 是关键词格式不匹配！</span>
        </template>
        <div style="font-size: 13px; line-height: 1.8;">
          <strong style="color: #f56c6c;">常见问题：</strong><br/>
          • AI 生成 <code>"keyword": "合同编号："</code>（中文冒号）<br/>
          • 但文档实际是 <code>合同编号:</code>（英文冒号）或 <code>合同编号  </code>（无冒号）<br/><br/>
          
          <strong style="color: #e6a23c;">解决方法：</strong><br/>
          1️⃣ 回到<strong>步骤1</strong>，在文档预览中用 Ctrl+F 搜索关键词<br/>
          2️⃣ 复制文档中的<strong>准确格式</strong>（包括冒号、空格）<br/>
          3️⃣ 修改下方 JSON 的 <code>keyword</code> 字段<br/>
          4️⃣ 或查看"📋 正确格式示例"，使用正则表达式增加容错
        </div>
      </el-alert>

      <el-alert 
        type="warning" 
        :closable="false"
        style="margin-bottom: 16px; border-radius: 8px;">
        <template #title>
          <span style="font-size: 14px; font-weight: 500;">⚠️ JSON 必须包含以下字段</span>
        </template>
        <div style="font-size: 13px; line-height: 1.8;">
          <strong>1. templateName</strong>: 模板名称（字符串）<br/>
          <strong>2. fields</strong>: 字段数组，每个字段必须包含：<br/>
          &nbsp;&nbsp;&nbsp;&nbsp;• <code>fieldName</code>: 英文字段名（驼峰命名）<br/>
          &nbsp;&nbsp;&nbsp;&nbsp;• <code>fieldLabel</code>: 中文字段名<br/>
          &nbsp;&nbsp;&nbsp;&nbsp;• <code>extractRules</code>: 提取规则对象，必须包含：<br/>
          &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;- <code>type</code>: 规则类型（"keyword" / "regex" / "table"）<br/>
          &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;- <code>keyword</code>: 关键词（type为keyword时必填）<br/>
          &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;- <code>pattern</code>: 正则表达式（type为regex时必填）<br/>
          &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;- <code>tableRules</code>: 表格规则对象（type为table时必填）<br/>
          &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;• <code>tableKeyword</code>: 表格定位关键词<br/>
          &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;• <code>columns</code>: 表格列名数组
        </div>
      </el-alert>

      <el-tabs v-model="jsonTab" style="margin-bottom: 20px;">
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
            class="json-textarea"
            style="background: #f5f7fa;">
          </el-input>
          <el-button 
            type="primary" 
            size="small" 
            @click="copyExample"
            style="margin-top: 12px;">
            <el-icon style="margin-right: 4px;"><DocumentCopy /></el-icon>
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
          style="margin-top: 10px">
          <ul>
            <li v-for="(warning, index) in validationResult.warnings" :key="index">{{ warning }}</li>
          </ul>
        </el-alert>
      </div>

      <div class="step-actions">
        <el-button size="large" @click="prevStep">
          <el-icon style="margin-right: 4px;"><ArrowRight style="transform: rotate(180deg);" /></el-icon>
          上一步
        </el-button>
        <el-button size="large" type="warning" @click="validateJSON" :disabled="!aiGeneratedJSON">
          <el-icon style="margin-right: 4px;"><Check /></el-icon>
          验证JSON格式
        </el-button>
        <el-button
          size="large"
          type="primary"
          @click="importTemplate"
          :loading="importing"
          :disabled="!validationResult || !validationResult.valid">
          <el-icon style="margin-right: 4px;"><UploadFilled /></el-icon>
          {{ importing ? '正在导入...' : '导入模板到系统' }}
        </el-button>
      </div>
    </el-card>

    <!-- 步骤4: 完成 -->
    <el-card v-show="currentStep === 4" class="step-card success-card">
      <template #header>
        <span>
          <el-icon style="vertical-align: middle; margin-right: 4px;"><SuccessFilled /></el-icon>
          步骤4: 导入成功
        </span>
      </template>

      <el-result
        icon="success"
        title="🎉 模板创建成功！"
        :subTitle="'模板名称: ' + (importResult ? importResult.templateName : '')">
        <template #extra>
          <div class="result-info">
            <p style="font-size: 16px; margin-bottom: 16px;">
              <strong>✅ 模板已成功导入系统</strong>
            </p>
            <p style="font-size: 15px;">
              <el-icon style="vertical-align: middle; color: #67c23a;"><Check /></el-icon>
              字段数量: <strong>{{ importResult ? importResult.fieldCount : 0 }} 个</strong>
            </p>
            <p v-if="importResult && importResult.warnings && importResult.warnings.length > 0" style="margin-top: 16px;">
              <el-tag type="warning" size="large" effect="plain">⚠️ 需要注意</el-tag>
              <ul class="warnings-list">
                <li v-for="(warning, index) in importResult.warnings" :key="index">{{ warning }}</li>
              </ul>
            </p>
          </div>

          <div style="margin-top: 32px; display: flex; gap: 16px; justify-content: center; flex-wrap: wrap;">
            <el-button type="primary" size="large" @click="goToEditor">
              <el-icon style="margin-right: 4px;"><Edit /></el-icon>
              前往模板编辑器精调规则
            </el-button>
            <el-button type="success" size="large" @click="resetGenerator">
              <el-icon style="margin-right: 4px;"><RefreshLeft /></el-icon>
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
import axios from 'axios'
import { 
  Upload, 
  Edit, 
  DocumentAdd, 
  SuccessFilled, 
  ArrowRight, 
  DocumentCopy, 
  Check, 
  UploadFilled, 
  Refresh, 
  RefreshLeft,
  Loading
} from '@element-plus/icons-vue'

export default {
  name: 'AITemplateGenerator',
  components: {
    Upload,
    Edit,
    DocumentAdd,
    SuccessFilled,
    ArrowRight,
    DocumentCopy,
    Check,
    UploadFilled,
    Refresh,
    RefreshLeft,
    Loading
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
      "fieldName": "contractAmount",
      "fieldLabel": "合同金额",
      "fieldType": "text",
      "required": true,
      "extractRules": {
        "type": "regex",
        "pattern": "合同金额[：:\\\\s]*[￥¥RMB]*\\\\s*(\\\\d+(?:,\\\\d{3})*(?:\\\\.\\\\d{1,2})?)"
      },
      "note": "匹配多种金额格式（带逗号、货币符号等）"
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
      // 处理文件选择事件
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
      // 跳转到模板编辑器
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
.ai-template-generator {
  min-height: 100vh;
  background: #ffffff;
  padding: 40px 20px;
  
  > h2 {
    max-width: 1200px;
    margin: 0 auto 48px;
    text-align: center;
    font-size: 32px;
    font-weight: 600;
    color: #2c3e50;
    letter-spacing: 0.5px;
    padding: 0 20px;
    position: relative;
    
    .el-icon {
      vertical-align: middle;
      margin-right: 12px;
      font-size: 34px;
      color: #409EFF;
      animation: rotate 3s linear infinite;
    }
    
    &::after {
      content: '';
      display: block;
      width: 60px;
      height: 3px;
      background: linear-gradient(90deg, #409EFF, #67c23a);
      margin: 16px auto 0;
      border-radius: 2px;
    }
  }
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.steps {
  max-width: 1200px;
  margin: 0 auto 40px;
  padding: 32px;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  border: 1px solid #e4e7ed;
  
  ::v-deep(.el-step__title) {
    font-size: 16px;
    font-weight: 500;
  }
  
  ::v-deep(.el-step__description) {
    font-size: 13px;
  }
  
  ::v-deep(.el-step.is-finish .el-step__icon) {
    background: #667eea;
    border-color: #667eea;
  }
  
  ::v-deep(.el-step.is-process .el-step__icon) {
    background: #667eea;
    border-color: #667eea;
  }
}

.step-card {
  max-width: 1200px;
  margin: 0 auto 24px;
  min-height: 500px;
  border-radius: 16px;
  border: none;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  background: #ffffff;
  
  &:hover {
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
    transform: translateY(-2px);
  }
  
  ::v-deep(.el-card__header) {
    background: #f8f9fa;
    border-bottom: 2px solid #f0f2f5;
    padding: 24px 32px;
    
    span {
      font-size: 18px;
      font-weight: 600;
      color: #2c3e50;
      
      .el-icon {
        margin-right: 8px;
        color: #667eea;
      }
    }
  }
  
  ::v-deep(.el-card__body) {
    padding: 32px;
  }

  .upload-demo {
    margin: 32px 0;
    
    ::v-deep(.el-upload-dragger) {
      border: 2px dashed #d9d9d9;
      border-radius: 12px;
      background: #fafbfc;
      transition: all 0.3s ease;
      padding: 40px 20px;
      
      &:hover {
        border-color: #667eea;
        background: #f8f9fe;
        
        .el-icon {
          color: #667eea;
          transform: scale(1.1);
        }
      }
      
      .el-icon {
        font-size: 80px;
        color: #c0c4cc;
        margin: 0 0 16px;
        transition: all 0.3s ease;
      }
      
      .el-upload__text {
        font-size: 16px;
        color: #606266;
        
        em {
          color: #667eea;
          font-style: normal;
          font-weight: 500;
        }
      }
    }
  }

  .text-preview {
    margin-top: 32px;
    animation: fadeIn 0.5s ease;
    
    .el-divider {
      margin: 24px 0;
    }

    .preview-textarea {
      margin-bottom: 16px;
      font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
      border-radius: 8px;
      
      ::v-deep(textarea) {
        line-height: 1.6;
        background: #f8f9fa;
      }
    }

    .preview-info {
      margin: 16px 0 24px 0;
      display: flex;
      gap: 12px;
      flex-wrap: wrap;

      .el-tag {
        padding: 8px 16px;
        font-size: 14px;
        border-radius: 20px;
        border: none;
        
        &:first-child {
          background: #667eea;
          color: white;
        }
      }
    }
  }

  .field-hint {
    margin-top: 12px;
    
    .el-alert {
      border-radius: 8px;
      
      p {
        margin: 6px 0;
        font-size: 13px;
        line-height: 1.6;
      }
    }
  }

  .prompt-textarea {
    font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
    font-size: 13px;
    
    ::v-deep(textarea) {
      line-height: 1.8;
      background: #f8f9fa;
      border-radius: 8px;
    }
  }

  .prompt-info {
    margin-top: 12px;
    
    .el-tag {
      padding: 6px 14px;
      border-radius: 16px;
    }
  }

  .usage-alert {
    margin: 24px 0;
    border-radius: 12px;
    
    .usage-steps {
      ol {
        margin: 12px 0;
        padding-left: 24px;

        li {
          margin: 10px 0;
          line-height: 1.8;
          color: #606266;
          
          strong {
            color: #2c3e50;
          }
        }
      }
    }
  }

  .json-hint {
    margin-bottom: 20px;
    border-radius: 12px;

    p {
      margin: 6px 0;
      line-height: 1.6;
    }
  }

  .json-textarea {
    font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
    font-size: 13px;
    margin-bottom: 20px;
    
    ::v-deep(textarea) {
      line-height: 1.8;
      background: #f8f9fa;
      border-radius: 8px;
      border: 1px solid #e4e7ed;
      transition: all 0.3s ease;
      
      &:focus {
        border-color: #667eea;
        background: #ffffff;
      }
    }
  }

  .validation-result {
    margin-bottom: 24px;
    
    .el-alert {
      border-radius: 12px;
      margin-bottom: 12px;
    }

    ul {
      margin: 12px 0;
      padding-left: 24px;

      li {
        margin: 8px 0;
        line-height: 1.6;
      }
    }
  }

  .step-actions {
    margin-top: 32px;
    text-align: center;
    padding: 24px 0 0;
    border-top: 2px solid #f0f2f5;

    .el-button {
      margin: 0 8px;
      padding: 12px 28px;
      font-size: 15px;
      border-radius: 8px;
      font-weight: 500;
      transition: all 0.3s ease;
      
      &.el-button--primary {
        background: #667eea;
        border: none;
        box-shadow: 0 4px 12px rgba(102, 126, 234, 0.35);
        
        &:hover {
          background: #5568d3;
          transform: translateY(-2px);
          box-shadow: 0 6px 20px rgba(102, 126, 234, 0.45);
        }
        
        &:active {
          background: #4451b8;
          transform: translateY(0);
        }
      }
      
      &.el-button--success {
        background: #67c23a;
        border: none;
        box-shadow: 0 4px 12px rgba(103, 194, 58, 0.35);
        
        &:hover {
          background: #5daf34;
          transform: translateY(-2px);
          box-shadow: 0 6px 20px rgba(103, 194, 58, 0.45);
        }
        
        &:active {
          background: #529b2e;
        }
      }
      
      &.el-button--default {
        &:hover {
          color: #667eea;
          border-color: #667eea;
          background: #f8f9fe;
        }
      }
    }
  }
}

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

// Element Plus 组件样式优化
::v-deep(.el-select) {
  width: 100%;
  
  .el-input__wrapper {
    border-radius: 8px;
    transition: all 0.3s ease;
    
    &:hover {
      box-shadow: 0 0 0 1px #667eea inset;
    }
  }
}

::v-deep(.el-input__wrapper),
::v-deep(.el-textarea__inner) {
  border-radius: 8px;
  transition: all 0.3s ease;
  
  &:hover {
    border-color: #667eea;
  }
  
  &:focus,
  &.is-focus {
    border-color: #667eea;
    box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.1);
  }
}

// 上传拖拽区域的背景已在 .upload-demo 中定义

::v-deep(.el-divider) {
  margin: 32px 0;
  
  .el-divider__text {
    background: transparent;
    font-size: 15px;
    font-weight: 500;
    color: #606266;
    display: flex;
    align-items: center;
  }
}

::v-deep(.el-result) {
  padding: 48px 0;
  
  .el-result__icon svg {
    width: 80px;
    height: 80px;
  }
  
  .el-result__title {
    font-size: 28px;
    margin-top: 24px;
  }
  
  .el-result__subtitle {
    font-size: 16px;
    margin-top: 12px;
  }
}

::v-deep(.el-alert) {
  &.el-alert--info {
    background: #e1effe;
    border: 1px solid #bfdbfe;
    
    .el-alert__title {
      color: #1e40af;
    }
  }
  
  &.el-alert--success {
    background: #d1fae5;
    border: 1px solid #86efac;
    
    .el-alert__title {
      color: #166534;
    }
  }
  
  &.el-alert--warning {
    background: #fef3c7;
    border: 1px solid #fde047;
    
    .el-alert__title {
      color: #92400e;
    }
  }
}

// 加载动画优化
::v-deep(.el-loading-mask) {
  background-color: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(5px);
}

.success-card {
  background: #f0fdf4;
  border: 2px solid #86efac;
  
  ::v-deep(.el-card__header) {
    background: #dcfce7;
    border-bottom-color: #86efac;
    
    span {
      color: #166534;
      
      .el-icon {
        color: #16a34a;
        animation: pulse 2s ease infinite;
      }
    }
  }
  
  .result-info {
    text-align: left;
    margin: 24px 0;
    animation: fadeIn 0.6s ease;

    p {
      margin: 12px 0;
      font-size: 15px;
      line-height: 1.8;
      color: #166534;
      
      strong {
        color: #15803d;
        font-weight: 600;
      }
    }

    .warnings-list {
      margin-top: 16px;
      padding: 16px;
      padding-left: 36px;
      background: #fef3c7;
      border-left: 4px solid #f59e0b;
      border-radius: 8px;
      color: #92400e;

      li {
        margin: 8px 0;
        line-height: 1.6;
      }
    }
  }
  
  .step-actions {
    border-top-color: #86efac;
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

.usage-guide {
  line-height: 2;
  color: #4b5563;
  font-size: 14px;
  
  ::v-deep(h3) {
    color: #1f2937;
    margin-top: 24px;
    margin-bottom: 12px;
    font-size: 18px;
    font-weight: 600;
  }
  
  ::v-deep(h4) {
    color: #374151;
    margin-top: 20px;
    margin-bottom: 10px;
    font-size: 16px;
    font-weight: 600;
  }
  
  ::v-deep(p) {
    margin: 10px 0;
    line-height: 1.8;
  }
  
  ::v-deep(ul), ::v-deep(ol) {
    margin: 12px 0;
    padding-left: 28px;
    
    li {
      margin: 8px 0;
      line-height: 1.8;
    }
  }
  
  ::v-deep(strong) {
    color: #667eea;
    font-weight: 600;
  }
}

// code 标签样式
code {
  background: #f5f7fa;
  color: #e83e8c;
  padding: 2px 6px;
  border-radius: 3px;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 0.9em;
  border: 1px solid #e4e7ed;
}

// 响应式设计
@media (max-width: 768px) {
  .ai-template-generator {
    padding: 20px 12px;
  }
  
  .header-card {
    ::v-deep(.el-card__body) {
      padding: 32px 20px;
    }
    
    h2 {
      font-size: 28px;
    }
    
    .subtitle {
      font-size: 14px;
    }
  }
  
  .steps {
    padding: 20px 16px;
    
    ::v-deep(.el-step__title) {
      font-size: 14px;
    }
  }
  
  .step-card {
    ::v-deep(.el-card__header) {
      padding: 20px 16px;
    }
    
    ::v-deep(.el-card__body) {
      padding: 20px 16px;
    }
    
    .step-actions {
      .el-button {
        margin: 4px;
        padding: 10px 20px;
        font-size: 14px;
      }
    }
  }
}

// 打印样式
@media print {
  .ai-template-generator {
    background: white;
    padding: 0;
    
    &::before {
      display: none;
    }
  }
  
  .step-actions {
    display: none;
  }
}
</style>

