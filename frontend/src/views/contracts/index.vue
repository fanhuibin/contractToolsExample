<template>
  <div class="contracts-container">
    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="合同名称">
          <el-input
            v-model="searchForm.contractName"
            placeholder="请输入合同名称"
            clearable
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 操作栏 -->
    <el-card class="action-card">
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        新增合同
      </el-button>
      <el-button type="success" @click="handleExtract">
        <el-icon><Document /></el-icon>
        合同信息提取
      </el-button>
      <el-button @click="handleTest">
        <el-icon><Connection /></el-icon>
        测试接口
      </el-button>
    </el-card>

    <!-- 数据表格 -->
    <el-card class="table-card">
      <el-table
        v-loading="loading"
        :data="tableData"
        border
        style="width: 100%"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="contractNo" label="合同编号" width="150" />
        <el-table-column prop="contractName" label="合同名称" min-width="200" />
        <el-table-column prop="contractType" label="合同类型" width="120" />
        <el-table-column prop="amount" label="合同金额" width="120">
          <template #default="{ row }">
            {{ row.amount ? `¥${row.amount}` : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="partyA" label="甲方" width="150" />
        <el-table-column prop="partyB" label="乙方" width="150" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="handleDialogClose"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="合同名称" prop="contractName">
          <el-input v-model="form.contractName" placeholder="请输入合同名称" />
        </el-form-item>
        <el-form-item label="合同类型" prop="contractType">
          <el-select v-model="form.contractType" placeholder="请选择合同类型" style="width: 100%">
            <el-option label="采购合同" value="采购合同" />
            <el-option label="销售合同" value="销售合同" />
            <el-option label="服务合同" value="服务合同" />
            <el-option label="租赁合同" value="租赁合同" />
          </el-select>
        </el-form-item>
        <el-form-item label="合同金额" prop="amount">
          <el-input-number
            v-model="form.amount"
            :precision="2"
            :step="1000"
            :min="0"
            style="width: 100%"
            placeholder="请输入合同金额"
          />
        </el-form-item>
        <el-form-item label="甲方" prop="partyA">
          <el-input v-model="form.partyA" placeholder="请输入甲方" />
        </el-form-item>
        <el-form-item label="乙方" prop="partyB">
          <el-input v-model="form.partyB" placeholder="请输入乙方" />
        </el-form-item>
        <el-form-item label="签订日期" prop="signDate">
          <el-date-picker
            v-model="form.signDate"
            type="date"
            placeholder="选择签订日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="生效日期" prop="effectiveDate">
          <el-date-picker
            v-model="form.effectiveDate"
            type="date"
            placeholder="选择生效日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="到期日期" prop="expireDate">
          <el-date-picker
            v-model="form.expireDate"
            type="date"
            placeholder="选择到期日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus, Connection, Document } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import {
  getContractPage,
  createContract,
  updateContract,
  deleteContract,
  testContract,
  type Contract
} from '@/api/contract'

// 搜索表单
const searchForm = reactive({
  contractName: ''
})

// 分页参数
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

// 表格数据
const tableData = ref<Contract[]>([])
const loading = ref(false)

// 路由
const router = useRouter()

// 对话框相关
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref<FormInstance>()
const form = reactive<Contract>({
  contractName: '',
  contractType: '',
  amount: 0,
  partyA: '',
  partyB: '',
  signDate: '',
  effectiveDate: '',
  expireDate: '',
  remark: ''
})

// 表单验证规则
const rules: FormRules = {
  contractName: [
    { required: true, message: '请输入合同名称', trigger: 'blur' }
  ],
  contractType: [
    { required: true, message: '请选择合同类型', trigger: 'change' }
  ]
}

// 获取合同列表
const getList = async () => {
  loading.value = true
  try {
    const params = {
      current: pagination.current,
      size: pagination.size,
      contractName: searchForm.contractName || undefined
    }
    const res = await getContractPage(params)
    tableData.value = res.data.records
    pagination.total = res.data.total
  } catch (error) {
    console.error('获取合同列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.current = 1
  getList()
}

// 重置
const handleReset = () => {
  searchForm.contractName = ''
  pagination.current = 1
  getList()
}

// 新增
const handleAdd = () => {
  dialogTitle.value = '新增合同'
  dialogVisible.value = true
  resetForm()
}

// 编辑
const handleEdit = (row: Contract) => {
  dialogTitle.value = '编辑合同'
  dialogVisible.value = true
  Object.assign(form, row)
}

// 删除
const handleDelete = async (row: Contract) => {
  try {
    await ElMessageBox.confirm('确定要删除这个合同吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await deleteContract(row.id!)
    ElMessage.success('删除成功')
    getList()
  } catch (error) {
    console.error('删除失败:', error)
  }
}

// 跳转到合同信息提取页面
const handleExtract = () => {
  router.push('/contract-extract')
}

// 测试接口
const handleTest = async () => {
  try {
    const res = await testContract()
    ElMessage.success(res.message)
  } catch (error) {
    console.error('测试失败:', error)
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    
    if (form.id) {
      // 编辑
      await updateContract(form.id, form)
      ElMessage.success('更新成功')
    } else {
      // 新增
      await createContract(form)
      ElMessage.success('创建成功')
    }
    
    dialogVisible.value = false
    getList()
  } catch (error) {
    console.error('提交失败:', error)
  }
}

// 重置表单
const resetForm = () => {
  Object.assign(form, {
    id: undefined,
    contractName: '',
    contractType: '',
    amount: 0,
    partyA: '',
    partyB: '',
    signDate: '',
    effectiveDate: '',
    expireDate: '',
    remark: ''
  })
}

// 对话框关闭
const handleDialogClose = () => {
  resetForm()
}

// 分页大小改变
const handleSizeChange = (size: number) => {
  pagination.size = size
  pagination.current = 1
  getList()
}

// 当前页改变
const handleCurrentChange = (current: number) => {
  pagination.current = current
  getList()
}

// 获取状态类型
const getStatusType = (status?: number) => {
  switch (status) {
    case 0: return 'info'
    case 1: return 'success'
    case 2: return 'danger'
    default: return 'info'
  }
}

// 获取状态文本
const getStatusText = (status?: number) => {
  switch (status) {
    case 0: return '草稿'
    case 1: return '生效'
    case 2: return '终止'
    default: return '未知'
  }
}

// 页面加载时获取数据
onMounted(() => {
  getList()
})
</script>

<style scoped>
.contracts-container {
  .search-card {
    margin-bottom: 16px;
  }
  
  .action-card {
    margin-bottom: 16px;
  }
  
  .table-card {
    .pagination {
      margin-top: 16px;
      display: flex;
      justify-content: flex-end;
    }
  }
}
</style> 