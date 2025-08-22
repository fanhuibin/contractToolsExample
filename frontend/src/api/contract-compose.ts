import request from '@/utils/request'

export interface ComposeRequest {
  templateFileId: string
  values: Record<string, string>
}

export function composeContract(data: ComposeRequest): Promise<{
  code: number
  message: string
  data: { fileId: string }
}> {
  return request({
    url: '/compose/sdt',
    method: 'post',
    data
  })
}


