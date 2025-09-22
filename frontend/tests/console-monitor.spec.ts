import { test, expect } from '@playwright/test'

test('控制台监控测试', async ({ page }) => {
  // 监听控制台输出
  const consoleMessages: string[] = []
  
  page.on('console', msg => {
    const timestamp = new Date().toLocaleTimeString()
    const message = `[${timestamp}] [${msg.type().toUpperCase()}] ${msg.text()}`
    consoleMessages.push(message)
    console.log(message)
  })
  
  // 监听页面错误
  page.on('pageerror', error => {
    const timestamp = new Date().toLocaleTimeString()
    const message = `[${timestamp}] [PAGE ERROR] ${error.message}`
    consoleMessages.push(message)
    console.log(message)
  })
  
  // 导航到应用
  await page.goto('/home')
  
  // 等待页面加载
  await page.waitForLoadState('networkidle')
  
  console.log('📋 页面已加载，开始监控控制台输出...')
  console.log('📋 在页面中进行操作，控制台输出将显示在此处')
  console.log('📋 按 Ctrl+C 停止测试')
  
  // 保持页面打开用于手动测试
  await page.waitForTimeout(300000) // 等待5分钟
})
