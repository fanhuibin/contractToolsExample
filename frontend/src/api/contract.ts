import request from '@/utils/request'

export interface Contract {
  id?: number
  contractNo?: string
  contractName: string
  contractType?: string
  status?: number
  amount?: number
  partyA?: string
  partyB?: string
  signDate?: string
  effectiveDate?: string
  expireDate?: string
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface PageParams {
  current: number
  size: number
  contractName?: string
}

// 获取合同列表（分页）
export function getContractPage(params: PageParams) {
  return request({
    url: '/contracts/page',
    method: 'get',
    params
  })
}

// 获取所有合同列表
export function getContractList() {
  return request({
    url: '/contracts/list',
    method: 'get'
  })
}

// 根据ID获取合同详情
export function getContractById(id: number) {
  return request({
    url: `/contracts/${id}`,
    method: 'get'
  })
}

// 创建合同
export function createContract(data: Contract) {
  return request({
    url: '/contracts',
    method: 'post',
    data
  })
}

// 更新合同
export function updateContract(id: number, data: Contract) {
  return request({
    url: `/contracts/${id}`,
    method: 'put',
    data
  })
}

// 删除合同
export function deleteContract(id: number) {
  return request({
    url: `/contracts/${id}`,
    method: 'delete'
  })
}

// 测试接口
export function testContract() {
  return request({
    url: '/contracts/test',
    method: 'get'
  })
} 