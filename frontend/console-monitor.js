import { chromium } from '@playwright/test';

async function monitorConsole() {
  const browser = await chromium.launch({ headless: false });
  const context = await browser.newContext();
  const page = await context.newPage();
  
  // 监听控制台输出
  page.on('console', msg => {
    const timestamp = new Date().toLocaleTimeString();
    console.log(`[${timestamp}] [${msg.type().toUpperCase()}] ${msg.text()}`);
  });
  
  // 监听页面错误
  page.on('pageerror', error => {
    const timestamp = new Date().toLocaleTimeString();
    console.log(`[${timestamp}] [PAGE ERROR] ${error.message}`);
  });
  
  try {
    await page.goto('http://localhost:3002/home');
    console.log('页面已打开，开始监控控制台输出...');
    console.log('按 Ctrl+C 退出监控');
    
    // 保持页面打开
    await page.waitForTimeout(3600000); // 等待1小时
  } catch (error) {
    console.error('监控过程中出错:', error);
  } finally {
    await browser.close();
  }
}

monitorConsole().catch(console.error);
