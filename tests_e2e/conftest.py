import os
import base64
import pytest
from pathlib import Path
from playwright.sync_api import sync_playwright, Browser, Page, BrowserContext

from tests_e2e.config.settings import HEADLESS, BASE_URL, BACKEND_LOG_PATH, SCREENSHOTS_DIR, REPORTS_DIR
from tests_e2e.utils.logger_observer import BrowserConsoleObserver, BackendLogObserver

@pytest.fixture(scope="session")
def playwright_instance():
    with sync_playwright() as p:
        yield p

@pytest.fixture(scope="session")
def browser(playwright_instance) -> Browser:
    browser = playwright_instance.chromium.launch(
        headless=HEADLESS,
        args=["--disable-dev-shm-usage", "--no-sandbox"]
    )
    yield browser
    browser.close()

@pytest.fixture(scope="function")
def context(browser: Browser) -> BrowserContext:
    context = browser.new_context(
        viewport={"width": 1440, "height": 900},
        ignore_https_errors=True
    )
    yield context
    context.close()

@pytest.fixture(scope="function")
def backend_logger():
    """Observador del log de Spring Boot."""
    observer = BackendLogObserver(BACKEND_LOG_PATH)
    observer.start_recording()
    return observer

@pytest.fixture(scope="function")
def web_logger():
    """Observador de la consola de JavaScript."""
    return BrowserConsoleObserver()

@pytest.fixture(scope="function")
def page(context: BrowserContext, web_logger: BrowserConsoleObserver, backend_logger: BackendLogObserver) -> Page:
    page = context.new_page()
    web_logger.attach_to_page(page)
    yield page


@pytest.hookimpl(hookwrapper=True)
def pytest_runtest_makereport(item, call):
    """
    Hook de Pytest para capturar screenshots en alta resolución, incrustarlas en base64
    y generar un bloque de explicación claro con el resultado y observabilidad de cada test.
    """
    outcome = yield
    report = outcome.get_result()
    extra = getattr(report, "extra", [])

    if report.when == "call":
        web_logger = item.funcargs.get("web_logger") if hasattr(item, "funcargs") else None
        backend_logger = item.funcargs.get("backend_logger") if hasattr(item, "funcargs") else None
        page = item.funcargs.get("page") if hasattr(item, "funcargs") else None

        web_summary = web_logger.get_summary() if web_logger else {"errors": [], "warnings": []}
        backend_errors = backend_logger.get_errors() if backend_logger else []
        
        docstring = item.function.__doc__.strip() if item.function.__doc__ else "Sin descripción disponible."
        doc_html = "<br>".join([line.strip() for line in docstring.splitlines() if line.strip()])

        img_html = ""
        if page:
            clean_nodeid = item.nodeid.replace("::", "_").replace("/", "_").replace("\\", "_").replace(".py", "")
            # Limpiar caracteres especiales de parametros
            for c in ["[", "]", "/", " ", "-"]:
                clean_nodeid = clean_nodeid.replace(c, "_")
            
            screenshot_file = SCREENSHOTS_DIR / f"{clean_nodeid}_{report.outcome}.png"
            try:
                page.screenshot(path=str(screenshot_file), full_page=True)
                if screenshot_file.exists():
                    with open(screenshot_file, "rb") as f:
                        b64_img = base64.b64encode(f.read()).decode("utf-8")
                    
                    img_html = f"""
                    <div style="margin-top: 14px;">
                        <span style="font-weight: 700; font-size: 13px; color: #334155; display: block; margin-bottom: 6px;">
                            📸 Captura de Pantalla al finalizar el test:
                        </span>
                        <a href="data:image/png;base64,{b64_img}" target="_blank" title="Haz clic para ver en tamaño completo">
                            <img src="data:image/png;base64,{b64_img}" 
                                 style="max-width: 100%; width: 680px; max-height: 400px; object-fit: contain; background: #1e293b; border: 2px solid #cbd5e1; border-radius: 8px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); cursor: pointer;" />
                        </a>
                        <p style="font-size: 11px; color: #64748b; margin-top: 4px;">🔍 (Haz clic sobre la imagen para verla en pantalla completa en una nueva pestaña)</p>
                    </div>
                    """
            except Exception as e:
                img_html = f"<p style='color: red;'>No se pudo tomar la captura: {e}</p>"

        # Estado visual
        badge_status = '<span style="background: #22c55e; color: white; padding: 2px 8px; border-radius: 12px; font-weight: bold; font-size: 11px;">EXITOSO (PASSED)</span>' if report.outcome == 'passed' else '<span style="background: #ef4444; color: white; padding: 2px 8px; border-radius: 12px; font-weight: bold; font-size: 11px;">FALLIDO (FAILED)</span>'
        
        web_badge = '<span style="color: #16a34a; font-weight: bold;">0 errores JS</span>' if len(web_summary["errors"]) == 0 else f'<span style="color: #dc2626; font-weight: bold;">{len(web_summary["errors"])} errores JS detectados</span>'
        backend_badge = '<span style="color: #16a34a; font-weight: bold;">0 excepciones Spring Boot</span>' if len(backend_errors) == 0 else f'<span style="color: #dc2626; font-weight: bold;">{len(backend_errors)} excepciones en backend</span>'

        html_block = f"""
        <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background: #ffffff; border: 1px solid #e2e8f0; border-radius: 10px; padding: 16px; margin: 12px 0; box-shadow: 0 1px 3px rgba(0,0,0,0.05);">
            <div style="display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #f1f5f9; padding-bottom: 10px; margin-bottom: 12px;">
                <span style="font-size: 15px; font-weight: 700; color: #0f172a;">📋 Resumen de la Prueba</span>
                <div>{badge_status}</div>
            </div>
            
            <div style="font-size: 13px; line-height: 1.6; color: #334155; margin-bottom: 12px; background: #f8fafc; padding: 10px 14px; border-radius: 6px; border-left: 4px solid #3b82f6;">
                <strong>🎯 Qué se evaluó en este test:</strong><br>
                {doc_html}
            </div>

            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 10px; font-size: 12px; margin-bottom: 12px;">
                <div style="background: #f8fafc; padding: 8px 12px; border-radius: 6px; border: 1px solid #e2e8f0;">
                    <strong>🌐 Consola Web (JavaScript):</strong> {web_badge}
                </div>
                <div style="background: #f8fafc; padding: 8px 12px; border-radius: 6px; border: 1px solid #e2e8f0;">
                    <strong>⚙️ Servidor Backend (Spring Boot):</strong> {backend_badge}
                </div>
            </div>

            {img_html}
        </div>
        """

        pytest_html = item.config.pluginmanager.getplugin("html")
        if pytest_html:
            extra.append(pytest_html.extras.html(html_block))

        report.extra = extra
