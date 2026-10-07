const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage();
  
  // Login
  await page.goto('http://127.0.0.1:8080/login');
  await page.fill('#username', 'agendajuridicodbdev');
  await page.fill('#password', 'DBag3$#38');
  await page.click('button[type="submit"]');
  await page.waitForTimeout(2000);
  
  // Capture Dashboard
  await page.screenshot({ path: 'evidencia_dashboard.png', fullPage: true });
  
  // Go to Expedientes
  await page.goto('http://127.0.0.1:8080/expedientes');
  await page.waitForTimeout(2000);
  await page.screenshot({ path: 'evidencia_expedientes.png', fullPage: true });

  await browser.close();
})();
