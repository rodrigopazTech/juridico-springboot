const { chromium } = require('playwright');

const baseUrl = process.env.QA_BASE_URL || 'http://localhost:8080';
const password = process.env.QA_PASSWORD || '12345';
const civilExpedienteId = 'daa3c09e-d335-4f89-94e1-5b2c4865fa21';
const outOfScopeExpedienteId = '62b42040-2c97-4676-b256-ba91757cad5f';

const accounts = [
  ['ADMIN', 'admin'],
  ['GERENTE', 'gerente'],
  ['JEFE_DEPTO', 'jefe'],
  ['ABOGADO', 'abogado'],
];

async function login(page, email) {
  await page.goto(`${baseUrl}/`, { waitUntil: 'domcontentloaded' });
  await page.fill('#username', email);
  await page.fill('#password', password);
  await Promise.all([
    page.waitForURL('**/dashboard', { timeout: 10000 }).catch(() => null),
    page.click('button[type="submit"]'),
  ]);
  await page.waitForLoadState('networkidle', { timeout: 10000 }).catch(() => null);
}

(async () => {
  const browser = await chromium.launch({ headless: true });
  const results = [];

  for (const [role, email] of accounts) {
    const context = await browser.newContext({ viewport: { width: 1366, height: 900 } });
    const page = await context.newPage();
    await login(page, email);

    const roleResult = { role, email, checks: [] };
    for (const [label, expedienteId] of [['civil', civilExpedienteId], ['fuera', outOfScopeExpedienteId]]) {
      const response = await page.goto(`${baseUrl}/expedientes/${expedienteId}`, { waitUntil: 'domcontentloaded' });
      await page.waitForLoadState('networkidle', { timeout: 5000 }).catch(() => null);
      const text = await page.locator('body').innerText({ timeout: 5000 }).catch(() => '');
      roleResult.checks.push({
        label,
        status: response.status(),
        finalUrl: page.url(),
        containsCivil: text.includes('022/2024'),
        containsOutOfScope: text.includes('QA-FUERA-001'),
        containsDenied: /denegado|forbidden|403|no tienes/i.test(text),
      });
    }

    await context.close();
    results.push(roleResult);
  }

  await browser.close();
  console.log(JSON.stringify(results, null, 2));
})();
