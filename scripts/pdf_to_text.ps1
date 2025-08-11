param(
  [string]$Base = 'http://localhost:8080',
  [string]$File = ''
)

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$duijie = Join-Path $repoRoot 'duijie'

if ([string]::IsNullOrWhiteSpace($File)) {
  $item = Get-ChildItem -LiteralPath $duijie -Filter *.pdf | Select-Object -First 1
  if (-not $item) { Write-Error 'duijie 目录未找到任何 PDF 文件'; exit 1 }
  $File = $item.FullName
}

if (-not (Test-Path -LiteralPath $File)) { Write-Error ("文件不存在: " + $File); exit 1 }

$resp = Invoke-RestMethod -Method Post -Uri ("$Base/api/ai/pdf/extract") -UseBasicParsing -Form @{ file = Get-Item -LiteralPath $File }
if (-not $resp.success) { $resp | ConvertTo-Json -Depth 20 | Write-Output; exit 2 }

$taskId = $resp.taskId
do {
  Start-Sleep -Seconds 2
  $st = Invoke-RestMethod -Uri ("$Base/api/ai/pdf/status/" + $taskId) -UseBasicParsing
  $status = $st.task.status
} while ($status -ne 'completed' -and $status -ne 'failed')

if ($status -eq 'completed') {
  $st.task.result | Write-Output
} else {
  $st | ConvertTo-Json -Depth 20 | Write-Output
}




