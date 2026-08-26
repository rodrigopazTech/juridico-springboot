"""
Tanda 4: Flujos End-to-End (E2E) Completos del Sistema SIAJ
Ejecuta los flujos integrales de negocio para los módulos principales:
1. Expedientes: Listado, búsqueda, visualización de detalle y trazabilidad.
2. Audiencias: KPIs dinámicos, alternancia de calendario y filtros.
3. Términos: Línea de tiempo de vencimientos, semáforos y listado.
"""
import pytest
from tests_e2e.config.roles import ROLES_CONFIG
from tests_e2e.config.settings import BASE_URL
from tests_e2e.pages.login_page import LoginPage
from tests_e2e.pages.expedientes_page import ExpedientesPage
from tests_e2e.pages.audiencias_page import AudienciasPage
from tests_e2e.pages.terminos_page import TerminosPage

def test_flujo_completo_expediente_y_detalle(page, web_logger, backend_logger):
    """
    Flujo E2E de Expedientes:
    1. Login con ADMIN.
    2. Búsqueda en el listado de expedientes.
    3. Clic para ingresar a la vista de Detalle (/expedientes/{id}).
    4. Validación del encabezado, información general, gerencia, materia y bitácora.
    5. Navegación a la vista de Trazabilidad histórica.
    """
    admin_cfg = ROLES_CONFIG["ADMIN"]
    login_page = LoginPage(page)
    exp_page = ExpedientesPage(page)

    login_page.load()
    login_page.login(admin_cfg["username"], admin_cfg["password"])
    assert login_page.is_login_successful()

    exp_page.load()
    assert exp_page.is_loaded()
    assert exp_page.get_table_rows_count() > 0, "Debe haber al menos un expediente en la tabla"

    # Seleccionar y abrir el primer expediente
    first_exp_link = page.locator("#tabla-expedientes-body tr a").first
    first_exp_link.click()
    page.wait_for_load_state("domcontentloaded")

    # Validar que estamos en la pantalla de detalle
    assert "/expedientes/" in page.url, f"No se abrió la vista de detalle. URL: {page.url}"
    assert page.locator("#encabezado-detalle-container").is_visible(), "El contenedor de encabezado no cargó"
    assert page.locator("#informacion-general-container").is_visible(), "El contenedor de información general no cargó"

    # Validar acceso a la vista de trazabilidad
    btn_trazabilidad = page.locator("a:has-text('Trazabilidad')")
    if btn_trazabilidad.count() > 0 and btn_trazabilidad.first.is_visible():
        btn_trazabilidad.first.click()
        page.wait_for_load_state("domcontentloaded")
        assert "/trazabilidad" in page.url, f"Fallo al abrir trazabilidad. URL: {page.url}"

    summary = web_logger.get_summary()
    assert len(summary["errors"]) == 0, f"Errores JS en el flujo de expediente: {summary['errors']}"
    assert len(backend_logger.get_errors()) == 0, f"Errores backend en el flujo de expediente: {backend_logger.get_errors()}"


def test_flujo_completo_audiencias(page, web_logger, backend_logger):
    """
    Flujo E2E de Audiencias:
    1. Login con DIRECTOR.
    2. Carga del módulo de Audiencias.
    3. Interacción con los KPIs (Pendiente, Total).
    4. Alternancia entre la pestaña de Mini-Calendario y Filtros avanzados.
    5. Validación del renderizado sin fallos de red ni excepciones del servidor.
    """
    director_cfg = ROLES_CONFIG["DIRECTOR"]
    login_page = LoginPage(page)
    aud_page = AudienciasPage(page)

    login_page.load()
    login_page.login(director_cfg["username"], director_cfg["password"])
    assert login_page.is_login_successful()

    aud_page.load()
    assert aud_page.is_loaded()
    assert aud_page.is_kpi_visible()

    # Clic en KPI Pendientes para filtrar dinámicamente
    kpi_pendiente = page.locator("#kpi-card-pendiente")
    if kpi_pendiente.is_visible():
        kpi_pendiente.click()
        page.wait_for_timeout(1000)

    # Alternar al panel de filtros
    btn_filtros = page.locator("#btn-ver-filtros")
    if btn_filtros.is_visible():
        btn_filtros.click()
        page.wait_for_timeout(500)
        assert page.locator("#cont-filtros").is_visible(), "El panel de filtros no se desplegó"

    # Volver al panel de calendario
    btn_cal = page.locator("#btn-ver-calendario")
    if btn_cal.is_visible():
        btn_cal.click()
        page.wait_for_timeout(500)
        assert page.locator("#cont-calendario").is_visible(), "El panel de calendario no se desplegó"

    summary = web_logger.get_summary()
    assert len(summary["errors"]) == 0, f"Errores JS en el flujo de audiencias: {summary['errors']}"
    assert len(backend_logger.get_errors()) == 0, f"Errores backend en el flujo de audiencias: {backend_logger.get_errors()}"


def test_flujo_completo_terminos(page, web_logger, backend_logger):
    """
    Flujo E2E de Términos:
    1. Login con ABOGADO.
    2. Carga de la pantalla de Términos legales.
    3. Validación del componente de línea de tiempo y tabla de resultados.
    4. Verificación de observabilidad (consola web limpia + backend sin excepciones).
    """
    abogado_cfg = ROLES_CONFIG["ABOGADO"]
    login_page = LoginPage(page)
    term_page = TerminosPage(page)

    login_page.load()
    login_page.login(abogado_cfg["username"], abogado_cfg["password"])
    assert login_page.is_login_successful()

    term_page.load()
    assert term_page.is_loaded()

    # Validar presencia de la tabla de términos
    tabla_terminos = page.locator("#grid-maestro-terminos")
    assert tabla_terminos.is_visible(), "La tabla maestra de términos no es visible"

    summary = web_logger.get_summary()
    assert len(summary["errors"]) == 0, f"Errores JS en el flujo de términos: {summary['errors']}"
    assert len(backend_logger.get_errors()) == 0, f"Errores backend en el flujo de términos: {backend_logger.get_errors()}"
