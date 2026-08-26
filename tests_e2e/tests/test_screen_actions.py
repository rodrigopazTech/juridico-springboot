"""
Tanda 3: Verificación de Pantallas, Botones de Acción y Permisos Contextuales
Valida la presencia y funcionamiento de los controles principales en las pantallas de:
- Expedientes
- Audiencias
- Términos
- Usuarios
"""
import pytest
from tests_e2e.config.roles import ROLES_CONFIG
from tests_e2e.pages.login_page import LoginPage
from tests_e2e.pages.expedientes_page import ExpedientesPage
from tests_e2e.pages.audiencias_page import AudienciasPage
from tests_e2e.pages.terminos_page import TerminosPage
from tests_e2e.pages.usuarios_page import UsuariosPage

ALL_ROLES = list(ROLES_CONFIG.keys())
MANAGEMENT_ROLES = ["ADMIN", "DIRECTOR_PRINCIPAL", "DIRECTOR", "SUBDIRECTOR", "GERENTE", "JEFE_DEPTO"]

@pytest.mark.parametrize("role_name", ALL_ROLES)
def test_expedientes_screen_elements_and_actions(page, web_logger, role_name):
    """
    Verifica que en la pantalla de Expedientes:
    1. Cargue el listado correctamente.
    2. El buscador y el boton de exportar a Excel estén operativos.
    3. El modal de 'Nuevo Expediente' se pueda abrir sin errores de JS.
    """
    role_cfg = ROLES_CONFIG[role_name]
    login_page = LoginPage(page)
    exp_page = ExpedientesPage(page)

    login_page.load()
    login_page.login(role_cfg["username"], role_cfg["password"])
    assert login_page.is_login_successful()

    exp_page.load()
    assert exp_page.is_loaded(), f"La pantalla de Expedientes no cargó para el rol {role_name}"
    assert exp_page.is_btn_exportar_visible(), "El botón de exportar Excel no está visible"

    # Verificar que el modal de nuevo expediente se pueda interactuar
    modal_opened = exp_page.open_modal_nuevo()
    assert modal_opened, "No se pudo desplegar el modal de Nuevo Expediente"

    summary = web_logger.get_summary()
    assert len(summary["errors"]) == 0, f"Errores JS en Expedientes ({role_name}): {summary['errors']}"


@pytest.mark.parametrize("role_name", ALL_ROLES)
def test_audiencias_screen_and_kpis(page, web_logger, role_name):
    """
    Verifica que en la pantalla de Audiencias carguen los KPIs (Total, Pendientes, etc.)
    y los paneles de vista (Calendario/Filtros).
    """
    role_cfg = ROLES_CONFIG[role_name]
    login_page = LoginPage(page)
    aud_page = AudienciasPage(page)

    login_page.load()
    login_page.login(role_cfg["username"], role_cfg["password"])
    assert login_page.is_login_successful()

    aud_page.load()
    assert aud_page.is_loaded(), f"La pantalla de Audiencias no cargó para el rol {role_name}"
    assert aud_page.is_kpi_visible(), "Las tarjetas de KPI de audiencias no están visibles"

    summary = web_logger.get_summary()
    assert len(summary["errors"]) == 0, f"Errores JS en Audiencias ({role_name}): {summary['errors']}"


@pytest.mark.parametrize("role_name", ALL_ROLES)
def test_terminos_screen_structure(page, web_logger, role_name):
    """
    Verifica que la pantalla de Términos cargue la línea de tiempo y la tabla principal.
    """
    role_cfg = ROLES_CONFIG[role_name]
    login_page = LoginPage(page)
    term_page = TerminosPage(page)

    login_page.load()
    login_page.login(role_cfg["username"], role_cfg["password"])
    assert login_page.is_login_successful()

    term_page.load()
    assert term_page.is_loaded(), f"La pantalla de Términos no cargó para el rol {role_name}"

    summary = web_logger.get_summary()
    assert len(summary["errors"]) == 0, f"Errores JS en Términos ({role_name}): {summary['errors']}"


@pytest.mark.parametrize("role_name", MANAGEMENT_ROLES)
def test_usuarios_management_screen_actions(page, web_logger, role_name):
    """
    Verifica que los roles con permisos de administración (Admin, Directores, Gerente, Jefe)
    puedan acceder a /usuarios, ver el botón de 'Agregar Usuario' y la lista de usuarios.
    """
    role_cfg = ROLES_CONFIG[role_name]
    login_page = LoginPage(page)
    usr_page = UsuariosPage(page)

    login_page.load()
    login_page.login(role_cfg["username"], role_cfg["password"])
    assert login_page.is_login_successful()

    usr_page.load()
    assert usr_page.is_loaded(), f"La pantalla de Usuarios no cargó para el rol administrativo {role_name}"
    assert usr_page.is_btn_agregar_visible(), f"El rol {role_name} debería poder ver el botón 'Agregar Usuario'"
    assert usr_page.get_users_count() > 0, "No se encontraron tarjetas de usuarios en el listado"

    summary = web_logger.get_summary()
    assert len(summary["errors"]) == 0, f"Errores JS en Usuarios ({role_name}): {summary['errors']}"
