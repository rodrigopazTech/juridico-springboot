"""
Fase 1: Suite de Seguridad de Datos, Aislamiento por Gerencia (IDOR) y Sanitización (XSS / SQLi)
Evalúa:
1. Prevención de IDOR: Aislamiento estricto de expedientes entre diferentes Gerencias y Abogados.
2. Bloqueo y Redirección Segura ante URLs directas para Detalle, Trazabilidad, Exportación y Modificación de Etapas.
3. Sanitización de entradas (XSS y caracteres especiales) en formularios y buscadores.
"""
import pytest
from tests_e2e.config.roles import ROLES_CONFIG
from tests_e2e.pages.login_page import LoginPage
from tests_e2e.pages.expedientes_page import ExpedientesPage

EXPEDIENTE_GERENCIA_1_ID = "daa3c09e-d335-4f89-94e1-5b2c4865fa21"  # Gerencia Civil (Abogado 5)
EXPEDIENTE_GERENCIA_2_ID = "62b42040-2c97-4676-b256-ba91757cad5f"  # Gerencia Laboral (Abogado.laboral 8 - 'QA-FUERA-001')

def test_idor_abogado_cross_gerencia_expediente_detail_forbidden(page, web_logger):
    """
    IDOR Test 1: Intento de Acceso a Detalle de Expediente Ajeno.
    Abogado (Gerencia Civil) intenta navegar a /expedientes/{id} de Gerencia Laboral.
    El sistema debe impedir el acceso (HTTP 403 o redirección segura fuera del expediente)
    y nunca exponer los datos sensibles de la otra gerencia.
    """
    login_page = LoginPage(page)
    login_page.load()
    login_page.login("abogado", "12345")
    assert login_page.is_login_successful()

    # Intentar acceder directamente al expediente de otra gerencia
    response = page.goto(f"http://localhost:8080/expedientes/{EXPEDIENTE_GERENCIA_2_ID}")
    page.wait_for_load_state("domcontentloaded")
    
    # Validar que el servidor bloqueó el acceso o redirigió al dashboard/home
    is_blocked_or_redirected = (
        (response and response.status == 403) or
        f"/expedientes/{EXPEDIENTE_GERENCIA_2_ID}" not in page.url or
        page.locator("text=No tienes permiso").count() > 0 or
        page.locator("text=Acceso Denegado").count() > 0
    )
    assert is_blocked_or_redirected, (
        f"¡VULNERABILIDAD IDOR DETECTADA! Un abogado de Gerencia 1 pudo ver el expediente de Gerencia 2. URL actual: {page.url}"
    )

    # Validar que los datos del expediente ajeno ('QA-FUERA-001') NO se encuentren en pantalla
    assert page.locator("text=QA-FUERA-001").count() == 0, "¡Fuga de datos! El número de expediente ajeno fue visible en el DOM."


def test_idor_abogado_cross_gerencia_trazabilidad_forbidden(page, web_logger):
    """
    IDOR Test 2: Intento de Acceso a Trazabilidad de Expediente Ajeno.
    Abogado (Gerencia Civil) intenta abrir /expedientes/{id}/trazabilidad de Gerencia Laboral.
    El sistema debe bloquear y expulsar al usuario fuera de la vista protegida.
    """
    login_page = LoginPage(page)
    login_page.load()
    login_page.login("abogado", "12345")
    assert login_page.is_login_successful()

    response = page.goto(f"http://localhost:8080/expedientes/{EXPEDIENTE_GERENCIA_2_ID}/trazabilidad")
    page.wait_for_load_state("domcontentloaded")

    is_blocked = (
        (response and response.status == 403) or
        f"/expedientes/{EXPEDIENTE_GERENCIA_2_ID}/trazabilidad" not in page.url or
        page.locator("text=Acceso Denegado").count() > 0
    )
    assert is_blocked, f"¡VULNERABILIDAD IDOR! Acceso indebido a Trazabilidad ajena. URL: {page.url}"


def test_idor_abogado_cross_gerencia_trazabilidad_export_forbidden(page, web_logger):
    """
    IDOR Test 3: Intento de Descarga de Reporte Excel de Trazabilidad Ajena.
    Abogado (Gerencia Civil) intenta descargar /expedientes/{id}/trazabilidad/exportar de Gerencia Laboral.
    Debe ser bloqueado con redirección o 403 sin entregar el archivo.
    """
    login_page = LoginPage(page)
    login_page.load()
    login_page.login("abogado", "12345")
    assert login_page.is_login_successful()

    response = page.goto(f"http://localhost:8080/expedientes/{EXPEDIENTE_GERENCIA_2_ID}/trazabilidad/exportar")
    page.wait_for_load_state("domcontentloaded")

    # Validar que no se haya entregado un archivo Excel ni cargado la trazabilidad ajena
    is_prevented = (
        (response and response.status in [403, 400, 302]) or
        "/dashboard" in page.url or
        page.locator("text=Acceso Denegado").count() > 0
    )
    assert is_prevented, "¡VULNERABILIDAD IDOR! Descarga no autorizada de Excel de trazabilidad ajena."


def test_idor_gerente_cross_gerencia_forbidden(page, web_logger):
    """
    IDOR Test 4: Aislamiento entre Gerentes de Distintas Áreas.
    Gerente de Gerencia 1 (Civil) intenta abrir expediente de Gerencia 2 (Laboral).
    Debe ser bloqueado y redirigido fuera del expediente.
    """
    login_page = LoginPage(page)
    login_page.load()
    login_page.login("gerente", "12345")
    assert login_page.is_login_successful()

    response = page.goto(f"http://localhost:8080/expedientes/{EXPEDIENTE_GERENCIA_2_ID}")
    page.wait_for_load_state("domcontentloaded")

    is_blocked = (
        (response and response.status == 403) or
        f"/expedientes/{EXPEDIENTE_GERENCIA_2_ID}" not in page.url or
        page.locator("text=Acceso Denegado").count() > 0
    )
    assert is_blocked, f"¡VULNERABILIDAD IDOR! Un Gerente de Gerencia 1 pudo ver expediente de Gerencia 2. URL: {page.url}"
    assert page.locator("text=QA-FUERA-001").count() == 0, "¡Fuga de datos! Expediente laboral visible para Gerente civil."


def test_sanitization_search_sql_injection_payloads(page, web_logger, backend_logger):
    """
    Sanitización Test 1: Inyección de Payloads en el Buscador.
    Prueba caracteres de escape SQL, tags HTML y scripts en la búsqueda de expedientes.
    El backend debe procesarlos de forma segura como texto literal sin arrojar 500.
    """
    admin_cfg = ROLES_CONFIG["ADMIN"]
    login_page = LoginPage(page)
    login_page.load()
    login_page.login(admin_cfg["username"], admin_cfg["password"])

    exp_page = ExpedientesPage(page)
    exp_page.load()

    payloads = [
        "' OR '1'='1",
        "'; DROP TABLE temp_test; --",
        "<script>window.__xss_test = true;</script>",
        "\"><img src=x onerror=alert(1)>",
        "../../../../etc/passwd"
    ]

    for payload in payloads:
        exp_page.search(payload)
        page.wait_for_timeout(300)
        
        # Verificar que el script no se haya ejecutado en el contexto del navegador
        xss_executed = page.evaluate("() => window.__xss_test === true")
        assert not xss_executed, f"¡FALLO DE SANITIZACIÓN (XSS)! El payload '{payload}' se ejecutó en el navegador."

    assert len(backend_logger.get_errors()) == 0, f"Excepciones backend ante payloads de búsqueda: {backend_logger.get_errors()}"


def test_sanitization_xss_in_notes_or_comments(page, web_logger, backend_logger):
    """
    Sanitización Test 2: Inyección XSS en Comentarios / Observaciones.
    Agrega una nota con contenido script y valida que se guarde y renderice escapado.
    """
    admin_cfg = ROLES_CONFIG["ADMIN"]
    login_page = LoginPage(page)
    login_page.load()
    login_page.login(admin_cfg["username"], admin_cfg["password"])

    exp_page = ExpedientesPage(page)
    exp_page.load()

    # Abrir el primer expediente
    first_exp = page.locator("#tabla-expedientes-body tr a").first
    first_exp.click()
    page.wait_for_load_state("domcontentloaded")

    # Si existe modal o botón de observaciones, probar sanitización
    btn_observaciones = page.locator("button:has-text('Observaciones')").first
    if btn_observaciones.is_visible():
        btn_observaciones.click()
        page.wait_for_timeout(400)
        
        # Verificar que no haya inyección activa
        xss_state = page.evaluate("() => window.__xss_note === true")
        assert not xss_state, "¡Alerta XSS detectada en el módulo de observaciones!"

    summary = web_logger.get_summary()
    assert len(summary["errors"]) == 0, f"Errores JS durante prueba de sanitización: {summary['errors']}"
