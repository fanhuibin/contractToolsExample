<template>
  <div class="risk-card-panel">
    <div class="panel-header">
      <div class="header-left">
        <el-input v-model="searchQuery" placeholder="搜索风险点..." clearable class="search-input">
          <template #prepend>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>
      <div class="header-right">
        <el-radio-group v-model="filterStatus" size="small">
          <el-radio-button label="">全部 ({{ totalCount }})</el-radio-button>
          <el-radio-button label="ERROR">高风险 ({{ errorCount }})</el-radio-button>
          <el-radio-button label="WARNING">中风险 ({{ warningCount }})</el-radio-button>
          <el-radio-button label="INFO">提示 ({{ infoCount }})</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <div class="panel-body" v-if="filteredResults.length">
      <div v-for="(item, idx) in filteredResults" :key="`${item.pointId}-${idx}`" class="risk-card" @click="handleCardClick(item)">
        <div class="risk-card-header">
          <el-tag :type="getStatusType(item.statusType)" effect="dark" size="small" class="status-tag">{{ item.statusType }}</el-tag>
          <span class="decision-type">{{ item.decisionType }}</span>
          <el-badge :value="(item.evidence && item.evidence.length) ? item.evidence.length : 0" class="evidence-badge" type="info" />
        </div>
        <div class="risk-card-body">
          <p class="message">{{ item.message || '' }}</p>
        </div>
        <div class="risk-card-footer">
          <span class="clause-type">{{ item.clauseType }}</span>
          <span class="algorithm-type">{{ item.algorithmType }}</span>
        </div>
      </div>
    </div>
    <div v-else class="panel-empty">
      <el-empty description="未发现相关风险点" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, toRefs } from 'vue';
import { ElInput, ElRadioGroup, ElRadioButton, ElTag, ElBadge, ElEmpty, ElIcon } from 'element-plus';
import { Search } from '@element-plus/icons-vue';

// 本地定义与后端返回匹配的数据结构，避免跨模块类型不一致
interface Evidence {
  text?: string;
  page?: number;
  paragraphIndex?: number;
  startOffset?: number;
  endOffset?: number;
}

interface RiskItem {
  clauseType: string;
  pointId: string | number;
  algorithmType: string;
  decisionType: string;
  statusType: 'ERROR' | 'WARNING' | 'INFO' | string;
  message?: string;
  evidence: Evidence[];
}

interface AnchorRiskItem extends RiskItem {
  anchorId: string;
  message: string;
}

const props = defineProps<{
  results?: RiskItem[];
}>();

const emit = defineEmits<{
  (e: 'goto', anchorId: string): void;
}>();

const { results } = toRefs(props);
const searchQuery = ref('');
const filterStatus = ref('');

const getStatusType = (status: string) => {
  if (status === 'ERROR') return 'danger';
  if (status === 'WARNING') return 'warning';
  return 'info';
};

const processedResults = computed((): AnchorRiskItem[] => {
  const arr = Array.isArray(results.value) ? results.value : [];
  return arr.map((item) => ({
    ...item,
    // 与 anchors 生成规则保持一致：指向首条 evidence（索引0）
    anchorId: `${item.pointId}_0`,
    // 规范化可选字段，避免模板类型告警
    message: item.message ?? ''
  }));
});

const filteredResults = computed(() => {
  let data = processedResults.value;
  if (filterStatus.value) {
    data = data.filter(item => item.statusType === filterStatus.value);
  }
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase();
    data = data.filter(item => 
      item.decisionType.toLowerCase().includes(query) ||
      item.message.toLowerCase().includes(query) ||
      item.clauseType.toLowerCase().includes(query)
    );
  }
  return data;
});

const totalCount = computed(() => processedResults.value.length);
const errorCount = computed(() => processedResults.value.filter(i => i.statusType === 'ERROR').length);
const warningCount = computed(() => processedResults.value.filter(i => i.statusType === 'WARNING').length);
const infoCount = computed(() => processedResults.value.filter(i => i.statusType === 'INFO').length);

const handleCardClick = (item: AnchorRiskItem) => {
  emit('goto', item.anchorId);
};

</script>

<style scoped>
.risk-card-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: #f9fafb;
}

.panel-header {
  padding: 10px;
  border-bottom: 1px solid #e4e7ed;
  background-color: #fff;
}
.header-left {
  margin-bottom: 10px;
}
.search-input .el-input-group__prepend {
  background-color: #fff;
}

.panel-body {
  flex-grow: 1;
  overflow-y: auto;
  padding: 10px;
}

.risk-card {
  background-color: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 12px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: box-shadow 0.2s ease-in-out;
}

.risk-card:hover {
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.risk-card-header {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.status-tag {
  margin-right: 8px;
}

.decision-type {
  font-weight: bold;
  color: #303133;
  flex-grow: 1;
}

.evidence-badge {
  margin-left: 8px;
}

.risk-card-body .message {
  font-size: 14px;
  color: #606266;
  margin: 0 0 8px 0;
  line-height: 1.5;
}

.risk-card-footer {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #909399;
}

.panel-empty {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
}
</style>
