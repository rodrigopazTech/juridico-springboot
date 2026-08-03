const { chromium } = require('playwright');

const baseUrl = process.env.QA_BASE_URL || 'http://localhost:8080';
const password = process.env.QA_PASSWORD || '12345';

const roles = [
  {
    role: 'ADMIN',
    email: 'admin',
    visible: ['Dashboard', 'Agenda Historial', 'Calendario', 'Mis Tareas', 'Expedientes', 'Términos', 'Audiencias', 'Usuarios', 'Migración Inteligente', 'Catálogos Generales'],
    hidden: [],
  },
  {
    role: 'DIRECTOR_PRINCIPAL',
    email: 'director.principal',
    visible: ['Dashboard', 'Agenda Historial', 'Calendario', 'Mis Tareas', 'Expedientes', 'Términos', 'Audiencias', 'Usuarios', 'Catálogos Generales'],
    hidden: ['Migración Inteligente'],
  },
  {
    role: 'DIRECTOR',
    email: 'director',
    visible: ['Dashboard', 'Agenda Historial', 'Calendario', 'Mis Tareas', 'Expedientes', 'Términos', 'Audiencias', 'Usuarios', 'Catálogos Generales'],
    hidden: ['Migración Inteligente'],
  },
  {
    role: 'SUBDIRECTOR',
    email: 'subdirector',
    visible: ['Dashboard', 'Agenda Historial', 'Calendario', 'Mis Tareas', 'Expedientes', 'Términos', 'Audiencias', 'Usuarios', 'Catálogos Generales'],
    hidden: ['Migración Inteligente'],
  },
  {
    role: 'GERENTE',
    email: 'gerente',
    visible: ['Dashboard', 'Agenda Historial', 'Calendario', 'Mis Tareas', 'Expedientes', 'Términos', 'Audiencias', 'Usuarios', 'Catálogos Generales'],
    hidden: ['Migración Inteligente'],
  },
  {
    role: 'JEFE_DEPTO',
    email: 'jefe',
    visible: ['Dashboard', 'Agenda Historial', 'Calendario', 'Mis Tareas', 'Expedientes', 'Términos', 'Audiencias', 'Usuarios', 'Catálogos Generales'],
    hidden: ['Migración Inteligente'],
  },
  {
    role: 'ABOGADO',
    email: 'abogado',
    visible: ['Dashboard', 'Agenda Historial', 'Calendario', 'Mis Tareas', 'Expedientes', 'Términos', 'Audiencias', 'Catálogos Generales'],
    hidden: ['Usuarios', 'Migración Inteligente'],
  },
];

const routeCases = [
  { path: '/dashboard', expected: 'allow_all' },
  { path: '/agenda', expected: 'allow_all' },
  { path: '/calendario', expected: 'allow_all' },
  { path: '/tareas', expected: 'allow_all' },
  { path: '/expedientes', expected: 'allow_all' },
  { path: '/terminos', expected: 'allow_all' },
  { path: '/audiencias', expected: 'allow_all' },
  { path: '/catalogos', expected: 'allow_all' },
  { path: '/usuarios', expected: 'no_abogado' },
  { path: '/admin/importar', expected: 'admin_only' },
  { path: '/usuarios-admin', expected: 'not_found' },
  { path: '/abogados', expected: 'not_found' },
];

function expectedAllowed(route, role) {
  if (route.expected === 'allow_all') return true;
  if (route.expected === 'no_abogado') return role !== 'ABOGADO';
  if (route.expected === 'admin_only') return role === 'ADMIN';
  return false;
}

function statusOkFor(route, role, status) {
  if (route.expected === 'not_found') return status === 404;
  if (expectedAllowed(route, role)) return status === 200;
  return status === 403 || status === 404 || status === 302;
}

(async () => {
  const browser = await chromium.launch({ headless: true });
  const results = [];

  for (const account of roles) {
    const context = await browser.newContext({ viewport: { width: 1366, height: 900 } });
    const page = await context.newPage();
    const consoleIssues = [];

    page.on('pageerror', err => consoleIssues.push(`pageerror: ${err.message}`));
    page.on('console', msg => {
      if (['error'].includes(msg.type())) {
        consoleIssues.push(`console ${msg.type()}: ${msg.text()}`);
      }
    });

    const roleResult = {
      role: account.role,
      email: account.email,
      login: { ok: false, url: '', status: null },
      menu: [],
      routes: [],
      consoleIssues,
    };

    try {
      const loginResponse = await page.goto(`${baseUrl}/`, { waitUntil: 'domcontentloaded' });
      await page.fill('#username', account.email);
      await page.fill('#password', password);
      await Promise.all([
        page.waitForURL(url => !url.toString().endsWith('/'), { timeout: 10000 }).catch(() => null),
        page.click('button[type="submit"]'),
      ]);
      await page.waitForLoadState('networkidle', { timeout: 10000 }).catch(() => null);
      roleResult.login = {
        ok: page.url().includes('/dashboard'),
        url: page.url(),
        status: loginResponse ? loginResponse.status() : null,
      };

      const bodyText = await page.locator('body').innerText({ timeout: 5000 });
      for (const item of account.visible) {
        roleResult.menu.push({ item, expected: 'visible', ok: bodyText.includes(item) });
      }
      for (const item of account.hidden) {
        roleResult.menu.push({ item, expected: 'hidden', ok: !bodyText.includes(item) });
      }

      for (const route of routeCases) {
        const response = await page.goto(`${baseUrl}${route.path}`, { waitUntil: 'domcontentloaded' }).catch(err => {
          roleResult.routes.push({ path: route.path, status: null, finalUrl: page.url(), ok: false, error: err.message });
          return null;
        });
        if (!response) continue;
        await page.waitForLoadState('networkidle', { timeout: 5000 }).catch(() => null);
        const status = response.status();
        roleResult.routes.push({
          path: route.path,
          status,
          finalUrl: page.url(),
          expected: route.expected,
          ok: statusOkFor(route, account.role, status),
        });
      }
    } catch (err) {
      roleResult.error = err.message;
    } finally {
      await context.close();
    }

    results.push(roleResult);
  }

  await browser.close();
  console.log(JSON.stringify(results, null, 2));
})();
