const { chromium } = require('playwright');

const baseUrl = process.env.QA_BASE_URL || 'http://localhost:8080';
const password = process.env.QA_PASSWORD || '12345';

const accounts = [
  ['ADMIN', 'admin@sistema.com'],
  ['DIRECTOR_PRINCIPAL', 'director.principal@juridico.com'],
  ['DIRECTOR', 'director@juridico.com'],
  ['SUBDIRECTOR', 'subdirector@juridico.com'],
  ['GERENTE', 'gerente@juridico.com'],
  ['JEFE_DEPTO', 'jefe@juridico.com'],
  ['ABOGADO', 'abogado@juridico.com'],
];

const pages = [
  { id: 'DASH', path: '/dashboard', mustContain: ['Dashboard'] },
  { id: 'AGE', path: '/agenda', mustContain: ['Agenda'] },
  { id: 'CAL', path: '/calendario', mustContain: ['Calendario'] },
  { id: 'TAR', path: '/tareas', mustContain: ['Tareas'] },
  { id: 'EXP', path: '/expedientes', mustContain: ['Expedientes'] },
  { id: 'TER', path: '/terminos', mustContain: ['Términos'] },
  { id: 'AUD', path: '/audiencias', mustContain: ['Audiencias'] },
  { id: 'CAT', path: '/catalogos', mustContain: ['Catálogos'] },
  { id: 'PERF', path: '/perfil', mustContain: ['Perfil'] },
  { id: 'NOTIF', path: '/notificaciones', mustContain: ['Notificaciones'] },
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
    const roleResult = { role, email, pages: [] };

    await login(page, email);

    for (const testPage of pages) {
      const issues = [];
      const onConsole = msg => {
        if (msg.type() === 'error') issues.push(msg.text());
      };
      const onPageError = err => issues.push(`pageerror: ${err.message}`);
      page.on('console', onConsole);
      page.on('pageerror', onPageError);

      const response = await page.goto(`${baseUrl}${testPage.path}`, { waitUntil: 'domcontentloaded' }).catch(err => {
        issues.push(`goto: ${err.message}`);
        return null;
      });
      await page.waitForLoadState('networkidle', { timeout: 7000 }).catch(() => null);

      let body = '';
      try {
        body = await page.locator('body').innerText({ timeout: 5000 });
      } catch (err) {
        issues.push(`body: ${err.message}`);
      }

      page.off('console', onConsole);
      page.off('pageerror', onPageError);

      const hasMustText = testPage.mustContain.some(text => body.includes(text));
      roleResult.pages.push({
        id: testPage.id,
        path: testPage.path,
        status: response ? response.status() : null,
        finalUrl: page.url(),
        ok: response && response.status() === 200 && hasMustText && issues.length === 0,
        hasMustText,
        includesCivilExpediente: body.includes('022/2024'),
        includesOutOfScopeExpediente: body.includes('QA-FUERA-001'),
        issues,
      });
    }

    await context.close();
    results.push(roleResult);
  }

  await browser.close();
  console.log(JSON.stringify(results, null, 2));
})();
