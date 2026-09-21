"""
screenshots.py — Captura de pantallas con Playwright.
Navega por los módulos del SIAJ (Abogado y Administrador)
y guarda las imágenes en assets/screenshots/.
"""

import os
import time
from playwright.sync_api import sync_playwright, TimeoutError as PlaywrightTimeout
from config import (
    BASE_URL, LOGIN_USER, LOGIN_PASS, ADMIN_USER, ADMIN_PASS,
    SCREENSHOTS_DIR, SCREENS, VIEWPORT, SCREENSHOT_TIMEOUT
)


def _wait_and_screenshot(page, slug: str, delay_ms: int = SCREENSHOT_TIMEOUT):
    """Espera que la red esté inactiva y toma captura de pantalla."""
    try:
        page.wait_for_load_state("networkidle", timeout=15000)
    except Exception:
        pass
    time.sleep(delay_ms / 1000)
    path = os.path.join(SCREENSHOTS_DIR, f"{slug}.png")
    page.screenshot(path=path, full_page=False)
    print(f"  OK  {slug}.png  ({VIEWPORT['width']}x{VIEWPORT['height']}px)")
    return path


def capture_login(page):
    """Captura la pantalla de login ANTES de autenticarse."""
    page.goto(f"{BASE_URL}/login", wait_until="domcontentloaded", timeout=20000)
    _wait_and_screenshot(page, "00_login", delay_ms=2500)


def do_login(page, username: str = LOGIN_USER, password: str = LOGIN_PASS) -> bool:
    """Realiza login con Spring Security en /perform_login."""
    page.goto(f"{BASE_URL}/login", wait_until="domcontentloaded", timeout=20000)

    try:
        page.wait_for_selector("#username", timeout=8000)
    except PlaywrightTimeout:
        print("  ERROR: No se encontro el campo #username. Verificar servidor.")
        return False

    page.fill("#username", username)
    page.fill("#password", password)
    page.click("button[type='submit']")

    try:
        page.wait_for_url(f"**/{'{'}*{'}'}", timeout=15000)
        time.sleep(2)
    except PlaywrightTimeout:
        time.sleep(3)

    current = page.url
    if "/login" in current or "/auth" in current:
        print(f"  ERROR: Despues del submit sigue en login ({current}).")
        return False

    print(f"  OK  Sesion iniciada como {username}. URL actual: {current}")
    return True


def action_tab_semaforos(page):
    """Navega a la pestaña o sección de Semáforos en la pantalla de Catálogos."""
    try:
        tab = page.query_selector("button:has-text('Semáforo'), button:has-text('Semáforos'), a:has-text('Semáforos'), [data-tab='semaforos']")
        if tab:
            tab.click()
            time.sleep(1.5)
    except Exception as e:
        print(f"  AVISO en tab_semaforos: {e}")


def _obtener_ruta_expediente(page) -> str:
    """Obtiene la ruta de un expediente existente desde /expedientes."""
    page.goto(f"{BASE_URL}/expedientes", wait_until="networkidle", timeout=20000)
    time.sleep(2)
    try:
        href = page.evaluate("""() => {
            const links = Array.from(document.querySelectorAll('a[href*="/expedientes/"]'));
            const match = links.find(a => {
                const h = a.getAttribute('href');
                return h && !h.includes('/exportar') && !h.includes('/nuevo');
            });
            return match ? match.getAttribute('href') : null;
        }""")
        if href:
            return href.split('?')[0].rstrip('/')
    except Exception as e:
        print(f"  AVISO al buscar expediente: {e}")
    return None


def capture_all():
    """Función principal: captura todas las pantallas definidas en config.py."""
    os.makedirs(SCREENSHOTS_DIR, exist_ok=True)
    captured = []

    with sync_playwright() as pw:
        browser = pw.chromium.launch(
            headless=True,
            args=["--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu"]
        )
        context = browser.new_context(
            viewport=VIEWPORT,
            locale="es-MX",
            timezone_id="America/Mexico_City",
        )
        page = context.new_page()

        # 1. Capturar login (sin sesión activa)
        print("\n-- Capturando pantalla de login (sin sesion) --")
        capture_login(page)
        captured.append("00_login")

        # 2. Autenticar como Abogado para pantallas estándar
        print(f"\n-- Iniciando sesion como {LOGIN_USER} --")
        if not do_login(page, LOGIN_USER, LOGIN_PASS):
            print("  FALLO: No se pudo iniciar sesion como Abogado.")

        current_user = LOGIN_USER

        # Cache de ruta de expediente para trazabilidad y documentos
        expediente_path = None

        # 3. Capturar pantallas
        for slug, route, extra_action in SCREENS:
            if slug == "00_login":
                continue

            # Pantallas administrativas o de expediente con datos requieren sesión de ADMIN
            is_admin_screen = slug in ("05_trazabilidad", "06_documentos", "10_usuarios", "11_config_semaforos")
            target_user = ADMIN_USER if is_admin_screen else LOGIN_USER
            target_pass = ADMIN_PASS if is_admin_screen else LOGIN_PASS

            if current_user != target_user:
                print(f"\n-- Cambiando sesion a {target_user} --")
                page.goto(f"{BASE_URL}/logout", wait_until="domcontentloaded", timeout=10000)
                time.sleep(1)
                do_login(page, target_user, target_pass)
                current_user = target_user

            # Resolver rutas dinámicas para trazabilidad y documentos
            target_route = route
            if slug in ("05_trazabilidad", "06_documentos"):
                if not expediente_path:
                    expediente_path = _obtener_ruta_expediente(page)
                if expediente_path:
                    if slug == "05_trazabilidad":
                        target_route = f"{expediente_path}/trazabilidad"
                    elif slug == "06_documentos":
                        target_route = expediente_path

            print(f"\n-- Capturando: {slug} -> {target_route} --")
            try:
                page.goto(f"{BASE_URL}{target_route}", wait_until="domcontentloaded", timeout=20000)

                if "/login" in page.url or "/auth" in page.url:
                    print(f"  AVISO: {target_route} redirigio a login. Re-autenticando...")
                    if do_login(page, target_user, target_pass):
                        page.goto(f"{BASE_URL}{target_route}", wait_until="domcontentloaded", timeout=20000)
                    else:
                        print(f"  SKIP: {slug} omitido.")
                        continue

                if slug == "11_config_semaforos":
                    action_tab_semaforos(page)
                elif extra_action:
                    extra_action(page)

                _wait_and_screenshot(page, slug)
                captured.append(slug)

            except Exception as e:
                print(f"  ERROR capturando {slug}: {e}")

        browser.close()

    print(f"\n[OK] Capturas completadas: {len(captured)}/{len(SCREENS)}")
    return captured


if __name__ == "__main__":
    capture_all()
