<template>
  <div class="ai-chat">
    <el-drawer
      v-model="isOpen"
      title="AI助手"
      size="80%"
      :before-close="handleClose"
      destroy-on-close
    >
      <div class="chat-container">
        <div class="sessions-panel">
          <div class="sessions-header">
            <h3>会话列表</h3>
            <el-button type="primary" size="small" @click="createNewSession">
              <el-icon><plus /></el-icon> 新会话
            </el-button>
          </div>
          
          <div class="sessions-list">
            <div
              v-for="session in sessions"
              :key="session.id"
              class="session-item"
              :class="{ active: currentSessionId === session.id }"
              @click="switchSession(session.id)"
            >
              <div class="session-title">{{ session.title }}</div>
              <div class="session-actions">
                <el-popconfirm
                  title="确定要删除此会话吗？"
                  @confirm="deleteSession(session.id)"
                  width="200"
                >
                  <template #reference>
                    <el-button 
                      type="danger" 
                      size="small" 
                      circle 
                      @click.stop
                    >
                      <el-icon><delete /></el-icon>
                    </el-button>
                  </template>
                </el-popconfirm>
              </div>
            </div>
          </div>
        </div>
        
        <div class="chat-panel">
          <div class="chat-messages" ref="messagesContainer">
            <div v-if="messages.length === 0" class="empty-messages">
              <div class="welcome-message">
                <h3>欢迎使用AI助手</h3>
                <p>您可以向我询问关于合同工具集的任何问题</p>
              </div>
              <div class="quick-questions">
                <h4>快速提问</h4>
                <el-button 
                  v-for="(question, index) in quickQuestions" 
                  :key="index"
                  @click="sendMessage(question)"
                  size="small"
                >
                  {{ question }}
                </el-button>
              </div>
            </div>
            
            <template v-else>
              <div
                v-for="(message, index) in messages"
                :key="index"
                class="message"
                :class="message.role === 'user' ? 'user-message' : 'assistant-message'"
              >
                <div class="message-avatar">
                  <el-avatar 
                    :icon="message.role === 'user' ? UserFilled : Service" 
                    :size="36"
                  />
                </div>
                <div class="message-content">
                  <div class="message-text">{{ message.content }}</div>
                  <div class="message-actions">
                    <el-button 
                      size="small" 
                      text 
                      @click="copyMessage(message.content)"
                    >
                      复制
                    </el-button>
                    <el-button 
                      v-if="message.role === 'assistant'"
                      size="small" 
                      text 
                      @click="regenerateResponse(index)"
                    >
                      重新生成
                    </el-button>
                  </div>
                </div>
              </div>
            </template>
            
            <div v-if="loading" class="loading-message">
              <el-icon class="is-loading"><loading /></el-icon>
              AI正在思考中...
            </div>
          </div>
          
          <div class="chat-input">
            <el-input
              v-model="inputMessage"
              type="textarea"
              :rows="3"
              placeholder="输入您的问题..."
              resize="none"
              @keydown.enter.exact.prevent="sendMessage()"
            >
              <template #append>
                <el-button 
                  type="primary" 
                  :disabled="!inputMessage.trim() || loading"
                  @click="sendMessage()"
                >
                  发送
                </el-button>
              </template>
            </el-input>
            <div class="input-tips">
              按Enter发送，Shift+Enter换行
            </div>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted, nextTick, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { Plus, Delete, UserFilled, Service, Loading } from '@element-plus/icons-vue';
import { aiChat } from '@/api/ai';

// 定义属性
const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  }
});

// 定义事件
const emit = defineEmits(['update:modelValue']);

// 计算属性
const isOpen = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
});

// 会话相关
const sessions = ref<any[]>([]);
const currentSessionId = ref<string | null>(null);
const messages = ref<any[]>([]);
const inputMessage = ref('');
const loading = ref(false);
const messagesContainer = ref<HTMLElement | null>(null);

// 快速提问
const quickQuestions = [
  '如何使用合同模板功能？',
  '如何进行合同比对？',
  '如何导出合同为PDF？',
  '如何设置合同审批流程？'
];

// 加载会话列表
const loadSessions = async () => {
  try {
    const response = await aiChat.getSessions();
    if (response.success) {
      sessions.value = response.sessions || [];
    }
  } catch (error) {
    console.error('加载会话失败', error);
  }
};

// 创建新会话
const createNewSession = async () => {
  currentSessionId.value = null;
  messages.value = [];
};

// 切换会话
const switchSession = (sessionId: string) => {
  if (currentSessionId.value === sessionId) return;
  
  currentSessionId.value = sessionId;
  messages.value = [];
  // 在实际应用中，这里应该加载会话历史记录
};

// 删除会话
const deleteSession = async (sessionId: string) => {
  try {
    const response = await aiChat.deleteSession(sessionId);
    if (response.success) {
      ElMessage.success('会话已删除');
      if (currentSessionId.value === sessionId) {
        currentSessionId.value = null;
        messages.value = [];
      }
      await loadSessions();
    } else {
      ElMessage.error(response.message || '删除失败');
    }
  } catch (error: any) {
    ElMessage.error(error.message || '删除过程中发生错误');
  }
};

// 发送消息
const sendMessage = async (content?: string) => {
  const message = content || inputMessage.value.trim();
  if (!message || loading.value) return;
  
  // 添加用户消息
  messages.value.push({
    role: 'user',
    content: message
  });
  
  inputMessage.value = '';
  loading.value = true;
  
  // 滚动到底部
  await nextTick();
  scrollToBottom();
  
  try {
    const response = await aiChat.sendMessage(message, currentSessionId.value || undefined);
    
    if (response.success) {
      // 如果是新会话，更新会话ID
      if (!currentSessionId.value && response.sessionId) {
        currentSessionId.value = response.sessionId;
        await loadSessions();
      }
      
      // 添加AI回复
      messages.value.push({
        role: 'assistant',
        content: response.data
      });
    } else {
      ElMessage.error(response.message || '发送失败');
    }
  } catch (error: any) {
    ElMessage.error(error.message || '发送过程中发生错误');
  } finally {
    loading.value = false;
    
    // 滚动到底部
    await nextTick();
    scrollToBottom();
  }
};

// 重新生成回复
const regenerateResponse = async (index: number) => {
  if (loading.value) return;
  
  // 找到上一个用户消息
  let userMessageIndex = index - 1;
  while (userMessageIndex >= 0 && messages.value[userMessageIndex].role !== 'user') {
    userMessageIndex--;
  }
  
  if (userMessageIndex < 0) return;
  
  const userMessage = messages.value[userMessageIndex].content;
  
  // 移除当前AI回复
  messages.value.splice(index, 1);
  
  loading.value = true;
  
  try {
    const response = await aiChat.sendMessage(userMessage, currentSessionId.value || undefined);
    
    if (response.success) {
      // 添加新的AI回复
      messages.value.push({
        role: 'assistant',
        content: response.data
      });
    } else {
      ElMessage.error(response.message || '重新生成失败');
    }
  } catch (error: any) {
    ElMessage.error(error.message || '重新生成过程中发生错误');
  } finally {
    loading.value = false;
    
    // 滚动到底部
    await nextTick();
    scrollToBottom();
  }
};

// 复制消息
const copyMessage = (content: string) => {
  navigator.clipboard.writeText(content)
    .then(() => {
      ElMessage.success('已复制到剪贴板');
    })
    .catch(() => {
      ElMessage.error('复制失败，请手动复制');
    });
};

// 滚动到底部
const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
  }
};

// 关闭抽屉
const handleClose = () => {
  isOpen.value = false;
};

// 监听抽屉打开
watch(() => isOpen.value, (newValue) => {
  if (newValue) {
    loadSessions();
  }
});

// 组件挂载
onMounted(() => {
  if (isOpen.value) {
    loadSessions();
  }
});

// 导入计算属性
import { computed } from 'vue';
</script>

<style scoped>
.chat-container {
  display: flex;
  height: 100%;
}

.sessions-panel {
  width: 250px;
  border-right: 1px solid #e6e6e6;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.sessions-header {
  padding: 15px;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.sessions-header h3 {
  margin: 0;
  font-size: 16px;
}

.sessions-list {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

.session-item {
  padding: 10px;
  border-radius: 4px;
  margin-bottom: 8px;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.session-item:hover {
  background-color: #f5f7fa;
}

.session-item.active {
  background-color: #ecf5ff;
}

.session-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-actions {
  opacity: 0;
  transition: opacity 0.3s;
}

.session-item:hover .session-actions {
  opacity: 1;
}

.chat-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.empty-messages {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.welcome-message {
  text-align: center;
  margin-bottom: 30px;
}

.welcome-message h3 {
  margin-bottom: 10px;
}

.quick-questions {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.quick-questions h4 {
  margin-bottom: 15px;
}

.quick-questions .el-button {
  margin: 5px;
}

.message {
  display: flex;
  margin-bottom: 20px;
}

.user-message {
  flex-direction: row-reverse;
}

.message-avatar {
  margin: 0 10px;
}

.message-content {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 8px;
  position: relative;
}

.user-message .message-content {
  background-color: #ecf5ff;
}

.assistant-message .message-content {
  background-color: #f5f7fa;
}

.message-text {
  word-break: break-word;
  white-space: pre-wrap;
}

.message-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 5px;
  opacity: 0;
  transition: opacity 0.3s;
}

.message:hover .message-actions {
  opacity: 1;
}

.loading-message {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 10px;
  color: #909399;
}

.loading-message .el-icon {
  margin-right: 8px;
}

.chat-input {
  padding: 15px;
  border-top: 1px solid #e6e6e6;
}

.input-tips {
  margin-top: 5px;
  font-size: 12px;
  color: #909399;
  text-align: right;
}
</style>