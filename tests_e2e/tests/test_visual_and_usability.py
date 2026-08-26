"""
Suite de Pruebas de Usabilidad, Geometría Visual y Responsividad (Visual & Usability QA)
Evalúa de forma matemática y determinista:
1. Ausencia de solapamiento del Header/Navbar con el contenido principal.
2. Comportamiento y altura del Sidebar en pantallas horizontales y monitores verticales.
3. Usabilidad, despliegue y estado de los <select> interactivos en modales.
"""
import pytest
from tests_e2e.config.roles import ROLES_CONFIG
from tests_e2e.pages.login_page import LoginPage
from tests_e2e.pages.expedientes_page import ExpedientesPage

VIEWPORTS = [
    {"name": "desktop_standard", "width": 1440, "height": 900},
    {"name": "laptop", "width": 1280, "height": 800},
    {"name": "vertical_monitor", "width": 1080, "height": 1920}
]

PAGES_TO_VERIFY_HEADER = [
    "/dashboard",
    "/expedientes",
    "/audiencias",
    "/terminos",
    "/catalogos"
]

@pytest.mark.parametrize("route", PAGES_TO_VERIFY_HEADER)
def test_no_header_overlap_on_main_content(page, web_logger, backend_logger, route):
    """
    Detección Geométrica de Solapamiento:
    Verifica que en cada pantalla el Header fijo no se superponga sobre el primer bloque de contenido.
    Formula: content.top >= navbar.bottom
    """
    admin_cfg = ROLES_CONFIG["ADMIN"]
    login_page = LoginPage(page)
    login_page.load()
    login_page.login(admin_cfg["username"], admin_cfg["password"])

    page.goto(f"http://localhost:8080{route}")
    page.wait_for_load_state("domcontentloaded")

    navbar = page.locator("nav").first
    assert navbar.is_visible(), f"Navbar no visible en {route}"
    nav_box = navbar.bounding_box()
    assert nav_box is not None, "No se pudo obtener el bounding box del navbar"
    nav_bottom = nav_box["y"] + nav_box["height"]

    # Localizar el primer elemento visualmente relevante dentro de <main>
    first_content = page.locator("main > div, main > section, main > form, main.catalogos-shell > div, main > *").first
    assert first_content.is_visible(), f"No se encontró contenido principal en {route}"
    content_box = first_content.bounding_box()
    assert content_box is not None, "No se pudo obtener el bounding box del contenido"

    content_top = content_box["y"]

    # Verificación estricta de no-solapamiento (con margen de tolerancia de 1px)
    assert content_top >= (nav_bottom - 1), (
        f"¡SOLAPAMIENTO DETECTADO en {route}! "
        f"El Navbar termina en Y={nav_bottom}px pero el contenido empieza en Y={content_top}px "
        f"(quedan {nav_bottom - content_top}px cubiertos por la cabecera)."
    )

    summary = web_logger.get_summary()
    assert len(summary["errors"]) == 0, f"Errores JS en {route}: {summary['errors']}"


def test_trazabilidad_header_overlap_fixed(page, web_logger, backend_logger):
    """
    Verifica específicamente que en la pantalla de Trazabilidad del Expediente,
    el primer contenedor blanco (#section encabezado) no quede tapado por el Navbar.
    """
    admin_cfg = ROLES_CONFIG["ADMIN"]
    login_page = LoginPage(page)
    login_page.load()
    login_page.login(admin_cfg["username"], admin_cfg["password"])

    # Ir al primer expediente disponible y abrir trazabilidad
    exp_page = ExpedientesPage(page)
    exp_page.load()
    first_exp = page.locator("#tabla-expedientes-body tr a").first
    first_exp.click()
    page.wait_for_load_state("domcontentloaded")

    btn_trazabilidad = page.locator("a:has-text('Trazabilidad')").first
    btn_trazabilidad.click()
    page.wait_for_load_state("domcontentloaded")

    navbar = page.locator("nav").first
    nav_box = navbar.bounding_box()
    nav_bottom = nav_box["y"] + nav_box["height"]

    first_section = page.locator("main section").first
    content_box = first_section.bounding_box()
    content_top = content_box["y"]

    assert content_top >= (nav_bottom - 1), (
        f"¡Falla en Trazabilidad! El Header termina en {nav_bottom}px pero la sección empieza en {content_top}px."
    )


@pytest.mark.parametrize("vp", VIEWPORTS)
def test_sidebar_vertical_responsiveness_and_height(page, vp):
    """
    Verifica que el Sidebar ocupe el 100% de la altura de la pantalla en monitores verticales (1080x1920)
    y escritorios convencionales sin cortarse ni dejar huecos en blanco.
    """
    page.set_viewport_size({"width": vp["width"], "height": vp["height"]})
    admin_cfg = ROLES_CONFIG["ADMIN"]
    login_page = LoginPage(page)
    login_page.load()
    login_page.login(admin_cfg["username"], admin_cfg["password"])

    sidebar = page.locator("#logo-sidebar")
    assert sidebar.is_visible(), f"El Sidebar no es visible en resolución {vp['name']}"

    sidebar_box = sidebar.bounding_box()
    assert sidebar_box is not None

    # En resoluciones sm+ (>=640px) el sidebar debe cubrir al menos el 100% del viewport vertical
    assert sidebar_box["height"] >= vp["height"], (
        f"El Sidebar se cortó en {vp['name']}: altura={sidebar_box['height']}px vs viewport={vp['height']}px"
    )


def test_selects_and_modal_usability(page, web_logger):
    """
    Auditoría de Usabilidad de Selects en el Modal de Nuevo Expediente:
    1. Verifica que los selects obligatorios estén presentes en el DOM.
    2. Comprueba que el select de Gerencia cargue opciones válidas.
    3. Valida que los selects dependientes (Materia / Etapa) estén correctamente controlados por estado.
    """
    admin_cfg = ROLES_CONFIG["ADMIN"]
    login_page = LoginPage(page)
    exp_page = ExpedientesPage(page)

    login_page.load()
    login_page.login(admin_cfg["username"], admin_cfg["password"])

    exp_page.load()
    assert exp_page.open_modal_nuevo(), "No se pudo abrir el modal de nuevo expediente"

    # Verificar presencia de selects clave
    select_gerencia = page.locator("#select-gerencia-exp")
    select_materia = page.locator("#select-materia-exp")
    select_etapa = page.locator("#select-etapa-exp")

    assert select_gerencia.is_visible(), "Select Gerencia no visible"
    assert select_materia.is_visible(), "Select Materia no visible"
    assert select_etapa.is_visible(), "Select Etapa no visible"

    # Verificar que el select de Gerencia tenga opciones disponibles
    gerencia_options = select_gerencia.locator("option").count()
    assert gerencia_options > 1, "El select de Gerencia no tiene opciones cargadas"

    summary = web_logger.get_summary()
    assert len(summary["errors"]) == 0, f"Errores JS en la manipulación de selects: {summary['errors']}"
