import request from '@/utils/request';

// 定义接口返回的数据类型
export interface FulfillmentTemplate {
  id?: number;
  name: string;
  contractType: string;
  taskTypes: string[];
  keywords: string[];
  timeRules: string[];
  type?: 'system' | 'user';
  isDefault?: boolean;
  userId?: string;
}

export interface FulfillmentTask {
  contractName: string;
  fulfillmentName: string;
  dueDate: string;
  fulfillmentMethod: string;
}

export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data?: T;
  tasks?: FulfillmentTask[];
}

// 履约任务模板管理API
export function getFulfillmentTemplates(userId?: string): Promise<ApiResponse<FulfillmentTemplate[]>> {
  const params = userId ? `?userId=${userId}` : '';
  return request({
    url: `/fulfillment/template/list${params}`,
    method: 'get'
  });
}

export function getFulfillmentTemplatesByType(contractType: string, userId?: string): Promise<ApiResponse<FulfillmentTemplate[]>> {
  const params = userId ? `?userId=${userId}` : '';
  return request({
    url: `/fulfillment/template/type/${contractType}${params}`,
    method: 'get'
  });
}

export function createFulfillmentTemplate(template: FulfillmentTemplate): Promise<ApiResponse<FulfillmentTemplate>> {
  return request({
    url: '/fulfillment/template/create',
    method: 'post',
    data: template
  });
}

export function updateFulfillmentTemplate(id: number, template: FulfillmentTemplate): Promise<ApiResponse<FulfillmentTemplate>> {
  return request({
    url: `/fulfillment/template/${id}`,
    method: 'put',
    data: template
  });
}

export function deleteFulfillmentTemplate(id: number): Promise<ApiResponse<boolean>> {
  return request({
    url: `/fulfillment/template/${id}`,
    method: 'delete'
  });
}

export function copyFulfillmentTemplate(id: number, newName: string, userId: string): Promise<ApiResponse<FulfillmentTemplate>> {
  return request({
    url: `/fulfillment/template/${id}/copy?newName=${encodeURIComponent(newName)}&userId=${userId}`,
    method: 'post'
  });
}

export function getFulfillmentContractTypes(): Promise<ApiResponse<Record<string, string>>> {
  return request({
    url: '/fulfillment/template/contract-types',
    method: 'get'
  });
}

export function getFulfillmentHistory(userId?: string): Promise<ApiResponse<any[]>> {
  const params = userId ? `?userId=${userId}` : '';
  return request({
    url: `/fulfillment/history/list${params}`,
    method: 'get'
  });
}

export function extractFulfillmentTask(formData: FormData): Promise<ApiResponse<FulfillmentTask[]>> {
  return request({
    url: '/fulfillment/extract',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  });
}

export function getFulfillmentConfig(): Promise<ApiResponse<any>> {
  return request({
    url: '/fulfillment/config',
    method: 'get'
  });
}

export function saveFulfillmentConfig(data: any): Promise<ApiResponse<any>> {
  return request({
    url: '/fulfillment/config',
    method: 'post',
    data
  });
}

export default {
  getFulfillmentTemplates,
  getFulfillmentTemplatesByType,
  createFulfillmentTemplate,
  updateFulfillmentTemplate,
  deleteFulfillmentTemplate,
  copyFulfillmentTemplate,
  getFulfillmentContractTypes,
  getFulfillmentHistory,
  extractFulfillmentTask,
  getFulfillmentConfig,
  saveFulfillmentConfig
};
