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

async function createTask(page, title) {
  await page.goto(`${baseUrl}/tareas`, { waitUntil: 'domcontentloaded' });
  await page.click('text=Nueva tarea');
  await page.fill('#titulo', title);
  await page.fill('#fechaRecordatorio', '2026-07-28');
  await page.fill('#horaRecordatorio', '10:30');
  await page.selectOption('#prioridadId', '2');
  await page.fill('#detalles', 'Creada por prueba automatizada de rol');
  await Promise.all([
    page.waitForURL('**/tareas', { timeout: 10000 }).catch(() => null),
    page.click('#form-tarea button[type="submit"]'),
  ]);
  await page.waitForLoadState('networkidle', { timeout: 10000 }).catch(() => null);
}

(async () => {
  const browser = await chromium.launch({ headless: true });
  const results = [];

  for (const [role, email] of accounts) {
    const context = await browser.newContext({ viewport: { width: 1366, height: 900 } });
    const page = await context.newPage();
    const title = `QA Tarea ${role} ${Date.now()}`;
    const roleResult = { role, email, title, createVisible: false, created: false, completed: false, deleted: false, issues: [] };

    try {
      await login(page, email);
      await createTask(page, title);
      let body = await page.locator('body').innerText();
      roleResult.created = body.includes(title);

      roleResult.createVisible = true;
      await page.locator(`tr:has-text("${title}") button`).first().click();
      await page.waitForLoadState('networkidle', { timeout: 10000 }).catch(() => null);
      body = await page.locator('body').innerText();
      roleResult.completed = body.includes(title) && body.includes('Completada');

      await page.locator(`tr:has-text("${title}") button[title="Eliminar"]`).first().click();
      await page.locator('.swal2-confirm').click();
      await page.waitForLoadState('networkidle', { timeout: 10000 }).catch(() => null);
      body = await page.locator('body').innerText();
      roleResult.deleted = !body.includes(title);
    } catch (err) {
      roleResult.issues.push(err.message);
    } finally {
      await context.close();
    }

    results.push(roleResult);
  }

  const adminContext = await browser.newContext({ viewport: { width: 1366, height: 900 } });
  const adminPage = await adminContext.newPage();
  const adminTitle = `QA Tarea Admin Ajena ${Date.now()}`;
  await login(adminPage, 'admin@sistema.com');
  await createTask(adminPage, adminTitle);
  await adminContext.close();

  const abogadoContext = await browser.newContext({ viewport: { width: 1366, height: 900 } });
  const abogadoPage = await abogadoContext.newPage();
  await login(abogadoPage, 'abogado@juridico.com');
  await abogadoPage.goto(`${baseUrl}/tareas`, { waitUntil: 'domcontentloaded' });
  const before = await abogadoPage.locator('body').innerText();
  await abogadoContext.close();

  results.push({
    role: 'ABOGADO',
    crossUserSetup: adminTitle,
    abogadoCanSeeAdminTask: before.includes(adminTitle),
    note: 'El intento de borrado por URL directa se ejecuta con curl/BD fuera del navegador para obtener id exacto.',
  });

  await browser.close();
  console.log(JSON.stringify(results, null, 2));
})();
