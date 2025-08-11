<template>
  <div class="tag-editor">
    <el-space wrap>
      <el-tag
        v-for="(item, idx) in items"
        :key="idx"
        closable
        @close="remove(idx)"
        type="info"
      >
        <el-input v-model="items[idx]" placeholder="请输入" size="small" />
      </el-tag>
      <el-input v-model="draft" :placeholder="placeholder" size="small" style="width: 260px" @keyup.enter="add" />
      <el-button size="small" @click="add">新增</el-button>
    </el-space>
  </div>
</template>

<script lang="ts" setup>
import { ref, watch } from 'vue'

const props = withDefaults(defineProps<{ items?: string[]; placeholder?: string }>(), {
  items: () => []
})
const emit = defineEmits(['update:items'])

const items = ref<string[]>(Array.isArray(props.items) ? [...props.items] : [])
const draft = ref('')

watch(() => props.items, (val) => {
  items.value = Array.isArray(val) ? [...val] : []
}, { deep: true })

function add() {
  const v = draft.value.trim()
  if (!v) return
  const next = [...items.value, v]
  items.value = next
  emit('update:items', next)
  draft.value = ''
}
function remove(i: number) {
  const next = items.value.slice()
  next.splice(i, 1)
  items.value = next
  emit('update:items', next)
}
</script>

<style scoped>
.tag-editor { display: block; }
</style>


