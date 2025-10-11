import request from '@/utils/request'

/**
 * 规则测试API
 */

/**
 * 测试提取规则
 */
export function testExtractRule(data: any) {
  return request({
    url: '/rule-extract/test/extract',
    method: 'post',
    data
  })
}

/**
 * 获取常用Pattern列表
 */
export function getCommonPatterns() {
  return request({
    url: '/rule-extract/test/patterns',
    method: 'get'
  })
}

