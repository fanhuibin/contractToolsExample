<template>
  <div class="embed-lib">
    <el-input v-model="keyword" placeholder="搜索风险点/算法类型/编号" clearable @input="loadTree" />
    <div class="tree-wrap">
      <el-tree
        ref="treeRef"
        :data="treeData"
        node-key="id"
        :props="{ label: 'label', children: 'children' }"
        show-checkbox
        :check-on-click-node="true"
        highlight-current
        default-expand-all
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import riskApi from '@/api/ai/risk'

const treeRef = ref()
const treeData = ref<any[]>([])
const keyword = ref('')

function buildTree(raw: any[], kw: string) {
  const out: any[] = []
  for (const node of raw) {
    const clause = node.clauseType
    const points = (node.points || []).filter((p: any) => !kw || JSON.stringify(p).includes(kw))
    out.push({ id: `c-${clause.id}`, label: clause.clauseName, children: points.map((p: any) => ({ id: p.id, label: `${p.pointName}（${p.pointCode}）`, raw: p, parent: clause })) })
  }
  return out
}

async function loadTree() {
  const res: any = await riskApi.getTree(true)
  const raw = res?.data || []
  treeData.value = buildTree(raw, keyword.value.trim())
}

function getCheckedPointIds(): number[] {
  const tree = treeRef.value
  const keys = (tree?.getCheckedKeys?.() || []) as any[]
  const nodes = (tree?.getCheckedNodes?.(true) || []) as any[]
  // 包含父类选中：若选中分类节点，则将其所有子点加入
  const childFromParent: number[] = []
  for (const n of nodes) {
    if (typeof n.id === 'string' && n.id.startsWith('c-')) {
      const children = (n.children || []) as any[]
      for (const ch of children) if (typeof ch.id === 'number') childFromParent.push(ch.id)
    }
  }
  const onlyLeafIds = keys.filter((k: any) => typeof k === 'number') as number[]
  const merged = Array.from(new Set([...onlyLeafIds, ...childFromParent]))
  return merged
}

onMounted(loadTree)

defineExpose({ getCheckedPointIds })
</script>

<style scoped>
.embed-lib { padding: 8px; }
.tree-wrap { max-height: 520px; overflow: auto; margin-top: 8px; }
</style>

