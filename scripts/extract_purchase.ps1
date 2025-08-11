param(
  [string]$FilePath
)

$ErrorActionPreference = 'Stop'

if (-not $FilePath -or [string]::IsNullOrWhiteSpace($FilePath)) {
  $repoRoot = Split-Path -Parent $PSScriptRoot
  $FilePath = Join-Path $repoRoot 'duijie\四川莞蓉科技有限公司.pdf'
}

if (-not (Test-Path -LiteralPath $FilePath)) {
  Write-Error ("文件不存在: " + $FilePath)
  exit 1
}

$base = 'http://localhost:8080'

# 购销合同提示词（字段清单 + 严格JSON返回要求）
$purchasePrompt = @'
请按照购销合同的字段提取以下信息，并严格以有效 JSON 返回。若无法提取某字段，值置为 null：
- 合同名称（返回纯名称，不包含“合同/合同书”字样）
- 合同编号
- 甲方公司名称
- 乙方公司名称
- 甲方通信地址
- 乙方通信地址
- 币种
- 合同总金额小写
- 合同总金额大写
- 付款节点
- 付款比例
- 付款金额（小写）
- 付款金额（大写）
- 合同生效日期（yyyy-MM-dd）
- 合同终止日期（yyyy-MM-dd）
- 合同有效期
- 交货单位
- 交货地点
- 交货时间（yyyy-MM-dd）
- 验收时间（yyyy-MM-dd）
- 运输方式
- 运输费用
- 增值率税率
- 税额
- 发票类型
- 服务期限
- 保证金金额（小写）
- 保证金金额（大写）
- 保证金比例
- 保证金归还节点
- 争议仲裁地
- 甲方纳税人识别号
- 乙方纳税人识别号
- 甲方注册地址
- 乙方注册地址
- 甲方登记电话
- 乙方登记电话
- 甲方开户行
- 乙方开户行
- 甲方银行账号名称
- 乙方银行账号名称
- 甲方银行账号
- 乙方银行账号
- 违约内容
- 甲方邮政编码
- 乙方邮政编码
- 甲方法人
- 乙方法人
- 甲方业务联系人
- 乙方业务联系人
- 甲方业务联系人电话
- 甲方业务联系人邮箱
- 乙方业务联系人电话
- 乙方业务联系人邮箱
- 甲方签署日期（yyyy-MM-dd）
- 乙方签署日期（yyyy-MM-dd）
- 甲方法人或代表人是否签字（true/false）
- 乙方法人或代表人是否签字（true/false）

请仅输出 JSON，不要包含解释性文字。
'@

# 提交提取任务（不依赖模板ID，直接用购销提示）
$resp = Invoke-RestMethod -Method Post -Uri ("$base/api/ai/contract/extract") -Form @{ 
  file = Get-Item -LiteralPath $FilePath; 
  prompt = $purchasePrompt 
}

if (-not $resp.success) {
  $resp | ConvertTo-Json -Depth 20 | Write-Output
  exit 2
}

$taskId = $resp.taskId

do {
  Start-Sleep -Seconds 2
  $st = Invoke-RestMethod -Uri ("$base/api/ai/contract/status/" + $taskId)
  $status = $st.task.status
} while ($status -ne 'completed' -and $status -ne 'failed')

if ($status -eq 'completed') {
  $jsonStr = $st.task.result
  try {
    $obj = $jsonStr | ConvertFrom-Json -ErrorAction Stop
  } catch {
    # 返回原始字符串
    $jsonStr | Write-Output
    exit 0
  }

  # 规则清洗：合同名称去除“合同/合同书”后缀与禁词
  if ($obj.PSObject.Properties.Name -contains '合同名称' -and $obj.'合同名称') {
    $name = $obj.'合同名称'.ToString().Trim()
    $name = ($name -replace '合同书$', '')
    $name = ($name -replace '合同$', '')
    $obj.'合同名称' = $name.Trim()
  }

  $obj | ConvertTo-Json -Depth 20 | Write-Output
} else {
  $st | ConvertTo-Json -Depth 20 | Write-Output
}




