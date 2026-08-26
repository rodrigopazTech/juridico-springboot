const fs = require('fs');
const path = require('path');
const http = require('http');
const https = require('https');
const { spawn } = require('child_process');
const { chromium } = require('playwright');

if (process.argv.includes('--help') || process.argv.includes('-h')) {
  console.log(`
Uso:
  npm run qa:browser-basic

Variables utiles:
  QA_BASE_URL=http://localhost:8080
  QA_USERNAME=admin
  QA_PASSWORD=admin123
  QA_HEADLESS=false
  QA_START_APP=auto|true|false
  QA_APP_COMMAND="mvnw.cmd spring-boot:run"

Salidas:
  test-results/qa-browser-basic/<timestamp>/results.json
  test-results/qa-browser-basic/<timestamp>/browser-console.jsonl
  test-results/qa-browser-basic/<timestamp>/app-console.log
  test-results/qa-browser-basic/<timestamp>/final-page.png
`);
  process.exit(0);
}

const rootDir = path.resolve(__dirname, '..');
const baseUrl = (process.env.QA_BASE_URL || 'http://localhost:8080').replace(/\/$/, '');
const username = process.env.QA_USERNAME || 'admin';
const password = process.env.QA_PASSWORD || 'admin123';
const headless = process.env.QA_HEADLESS !== 'false';
const startApp = process.env.QA_START_APP || 'auto';
const startupTimeoutMs = Number(process.env.QA_STARTUP_TIMEOUT_MS || 120000);
const actionTimeoutMs = Number(process.env.QA_ACTION_TIMEOUT_MS || 10000);

const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
const artifactsDir = path.join(rootDir, 'test-results', 'qa-browser-basic', timestamp);
fs.mkdirSync(artifactsDir, { recursive: true });

const browserConsolePath = path.join(artifactsDir, 'browser-console.jsonl');
const appConsolePath = path.join(artifactsDir, 'app-console.log');
const resultsPath = path.join(artifactsDir, 'results.json');
const screenshotPath = path.join(artifactsDir, 'final-page.png');

const results = {
  startedAt: new Date().toISOString(),
  baseUrl,
  username,
  headless,
  artifactsDir,
  appStartedByScript: false,
  steps: [],
  browserEvents: [],
  appLog: appConsolePath,
  screenshot: screenshotPath,
  issues: [],
};

function appendJsonLine(file, value) {
  fs.appendFileSync(file, `${JSON.stringify(value)}\n`, 'utf8');
}

function appendAppLog(line) {
  fs.appendFileSync(appConsolePath, line, 'utf8');
}

function requestUrl(url) {
  return new Promise((resolve) => {
    const client = url.startsWith('https:') ? https : http;
    const req = client.get(url, (res) => {
      res.resume();
      resolve({ ok: res.statusCode >= 200 && res.statusCode < 500, status: res.statusCode });
    });
    req.on('error', (err) => resolve({ ok: false, error: err.message }));
    req.setTimeout(3000, () => {
      req.destroy();
      resolve({ ok: false, error: 'timeout' });
    });
  });
}

async function waitForApp() {
  const deadline = Date.now() + startupTimeoutMs;
  let lastStatus = null;

  while (Date.now() < deadline) {
    lastStatus = await requestUrl(`${baseUrl}/`);
    if (lastStatus.ok) {
      return lastStatus;
    }
    await new Promise((resolve) => setTimeout(resolve, 1500));
  }

  throw new Error(`La app no respondio en ${baseUrl} despues de ${startupTimeoutMs}ms. Ultimo estado: ${JSON.stringify(lastStatus)}`);
}

async function startSpringBootIfNeeded() {
  const currentStatus = await requestUrl(`${baseUrl}/`);
  if (currentStatus.ok && startApp !== 'true') {
    appendAppLog(`[qa] La app ya estaba respondiendo en ${baseUrl}; no se inicio un proceso nuevo.\n`);
    appendAppLog('[qa] Para capturar consola desde el arranque usa QA_START_APP=true con el puerto libre.\n');
    results.appAlreadyRunning = true;
    return null;
  }

  if (startApp === 'false') {
    await waitForApp();
    appendAppLog(`[qa] QA_START_APP=false; se usara la app existente en ${baseUrl}.\n`);
    return null;
  }

  const customCommand = process.env.QA_APP_COMMAND;
  const command = customCommand || (process.platform === 'win32' ? 'cmd.exe' : './mvnw');
  const args = customCommand
    ? []
    : (process.platform === 'win32' ? ['/c', 'mvnw.cmd', 'spring-boot:run'] : ['spring-boot:run']);

  appendAppLog(`[qa] Iniciando app: ${customCommand || [command, ...args].join(' ')}\n`);
  const child = spawn(command, args, {
    cwd: rootDir,
    shell: Boolean(customCommand),
    env: process.env,
  });

  results.appStartedByScript = true;
  child.stdout.on('data', (data) => appendAppLog(data.toString()));
  child.stderr.on('data', (data) => appendAppLog(data.toString()));
  child.on('exit', (code, signal) => appendAppLog(`\n[qa] Proceso app finalizado. code=${code} signal=${signal}\n`));

  await waitForApp();
  return child;
}

async function safeStep(name, fn) {
  const step = { name, ok: false, startedAt: new Date().toISOString() };
  results.steps.push(step);
  try {
    const data = await fn();
    Object.assign(step, data || {});
    step.ok = data && data.assertOk === false ? false : true;
    if (!step.ok) {
      results.issues.push(`${name}: ${data.failure || 'validacion fallida'}`);
    }
  } catch (err) {
    step.ok = false;
    step.error = err.message;
    results.issues.push(`${name}: ${err.message}`);
  } finally {
    step.finishedAt = new Date().toISOString();
  }
  return step;
}

async function maybeLogin(page) {
  await page.goto(`${baseUrl}/`, { waitUntil: 'domcontentloaded' });
  await page.waitForLoadState('networkidle', { timeout: actionTimeoutMs }).catch(() => null);

  const usernameInput = page.locator('#username, input[name="username"], input[type="email"]').first();
  const passwordInput = page.locator('#password, input[name="password"], input[type="password"]').first();
  if ((await usernameInput.count()) === 0 || (await passwordInput.count()) === 0) {
    return { attempted: false, reason: 'No se encontro formulario de login en la pagina inicial.' };
  }

  await usernameInput.fill(username);
  await passwordInput.fill(password);
  await Promise.all([
    page.waitForURL('**/dashboard', { timeout: actionTimeoutMs }).catch(() => null),
    page.locator('button[type="submit"], input[type="submit"]').first().click(),
  ]);
  await page.waitForLoadState('networkidle', { timeout: actionTimeoutMs }).catch(() => null);

  const finalUrl = page.url();
  const body = await page.locator('body').innerText({ timeout: actionTimeoutMs }).catch(() => '');
  const authenticated = !/[?&]error=true/.test(finalUrl) && !/INICIAR SESION|INICIAR SESI.N/i.test(body);

  return {
    attempted: true,
    finalUrl,
    authenticated,
    assertOk: authenticated,
    failure: authenticated ? null : 'No se pudo autenticar con las credenciales configuradas.',
  };
}

async function visitPage(page, label, urlPath, expectedText) {
  const response = await page.goto(`${baseUrl}${urlPath}`, { waitUntil: 'domcontentloaded' });
  await page.waitForLoadState('networkidle', { timeout: actionTimeoutMs }).catch(() => null);
  const body = await page.locator('body').innerText({ timeout: actionTimeoutMs }).catch(() => '');
  const assertOk = Boolean(response && response.status() < 400 && (!expectedText || body.includes(expectedText)));
  return {
    label,
    path: urlPath,
    status: response ? response.status() : null,
    finalUrl: page.url(),
    title: await page.title().catch(() => ''),
    containsExpectedText: expectedText ? body.includes(expectedText) : true,
    assertOk,
    failure: !assertOk && expectedText
      ? `No se encontro el texto esperado "${expectedText}" o la respuesta no fue exitosa.`
      : null,
    bodySample: body.replace(/\s+/g, ' ').slice(0, 300),
  };
}

async function clickFirstNavigationLink(page) {
  const links = page.locator('a[href]:visible');
  const count = Math.min(await links.count(), 12);
  for (let i = 0; i < count; i += 1) {
    const link = links.nth(i);
    const href = await link.getAttribute('href').catch(() => null);
    const text = (await link.innerText().catch(() => '')).trim();
    if (!href || href === '#' || href.startsWith('javascript:') || href.includes('/logout')) {
      continue;
    }
    await Promise.all([
      page.waitForLoadState('domcontentloaded', { timeout: actionTimeoutMs }).catch(() => null),
      link.click(),
    ]);
    await page.waitForLoadState('networkidle', { timeout: actionTimeoutMs }).catch(() => null);
    return { clicked: true, text, href, finalUrl: page.url() };
  }
  return { clicked: false, reason: 'No se encontro link navegable visible.' };
}

(async () => {
  let appProcess = null;
  let browser = null;

  try {
    appProcess = await startSpringBootIfNeeded();

    browser = await chromium.launch({ headless });
    const context = await browser.newContext({ viewport: { width: 1366, height: 900 } });
    const page = await context.newPage();

    page.on('console', (msg) => {
      const event = {
        at: new Date().toISOString(),
        type: msg.type(),
        text: msg.text(),
        location: msg.location(),
      };
      results.browserEvents.push(event);
      appendJsonLine(browserConsolePath, event);
    });
    page.on('pageerror', (err) => {
      const event = { at: new Date().toISOString(), type: 'pageerror', text: err.message };
      results.browserEvents.push(event);
      appendJsonLine(browserConsolePath, event);
    });
    page.on('requestfailed', (request) => {
      const event = {
        at: new Date().toISOString(),
        type: 'requestfailed',
        method: request.method(),
        url: request.url(),
        failure: request.failure(),
      };
      results.browserEvents.push(event);
      appendJsonLine(browserConsolePath, event);
    });

    await safeStep('abrir-login', async () => visitPage(page, 'Login', '/', null));
    await safeStep('login', async () => maybeLogin(page));

    const pagesToVisit = [
      ['Dashboard', '/dashboard', 'Dashboard'],
      ['Expedientes', '/expedientes', 'Expedientes'],
      ['Agenda', '/agenda', 'Agenda'],
    ];

    for (const [label, urlPath, expectedText] of pagesToVisit) {
      await safeStep(`visitar-${label.toLowerCase()}`, async () => visitPage(page, label, urlPath, expectedText));
    }

    await safeStep('click-primer-link', async () => clickFirstNavigationLink(page));
    await page.screenshot({ path: screenshotPath, fullPage: true }).catch((err) => {
      results.issues.push(`screenshot: ${err.message}`);
    });
    await context.close();
  } catch (err) {
    results.issues.push(err.message);
    process.exitCode = 1;
  } finally {
    results.finishedAt = new Date().toISOString();
    results.ok = results.issues.length === 0;
    fs.writeFileSync(resultsPath, JSON.stringify(results, null, 2), 'utf8');

    if (browser) {
      await browser.close().catch(() => null);
    }
    if (appProcess) {
      appProcess.kill('SIGTERM');
    }

    console.log(`QA terminado. OK=${results.ok}`);
    console.log(`Resultados: ${resultsPath}`);
    console.log(`Consola navegador: ${browserConsolePath}`);
    console.log(`Consola app: ${appConsolePath}`);
    console.log(`Screenshot: ${screenshotPath}`);
  }
})();
