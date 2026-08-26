"""
Pruebas de Roles y Permisos (RBAC) para los 7 roles del sistema SIAJ:
- ADMIN
- DIRECTOR_PRINCIPAL
- DIRECTOR
- SUBDIRECTOR
- GERENTE
- JEFE_DEPTO
- ABOGADO

Verifica visibilidad de Sidebar, control de acceso a rutas restringidas y salud general de navegacion.
"""
import pytest
from tests_e2e.config.roles import ROLES_CONFIG
from tests_e2e.config.settings import BASE_URL
from tests_e2e.pages.login_page import LoginPage
from tests_e2e.pages.dashboard_page import DashboardPage

ALL_ROLES = list(ROLES_CONFIG.keys())

# Construir lista de tuplas (rol, ruta_prohibida) para pruebas de seguridad
FORBIDDEN_SCENARIOS = []
for r_name, r_cfg in ROLES_CONFIG.items():
    for f_route in r_cfg.get("forbidden_routes", []):
        FORBIDDEN_SCENARIOS.append((r_name, f_route))

@pytest.mark.parametrize("role_name", ALL_ROLES)
def test_role_sidebar_menu_visibility(page, web_logger, role_name):
    """
    Verifica que cada rol vea únicamente las opciones de menú autorizadas en el Sidebar
    y que las opciones restringidas permanezcan ocultas.
    """
    role_cfg = ROLES_CONFIG[role_name]
    login_page = LoginPage(page)
    dashboard_page = DashboardPage(page)

    # 1. Iniciar sesion
    login_page.load()
    login_page.login(role_cfg["username"], role_cfg["password"])
    assert login_page.is_login_successful(), f"Fallo inicio de sesion para el rol {role_name}"

    # 2. Verificar que los menús permitidos estén visibles
    for menu in role_cfg["allowed_menus"]:
        is_vis = dashboard_page.is_menu_visible(menu)
        assert is_vis, f"El rol {role_name} DEBE ver el menu '{menu}' en el Sidebar, pero esta oculto o no existe."

    # 3. Verificar que los menús ocultos NO estén visibles
    for menu in role_cfg["hidden_menus"]:
        is_vis = dashboard_page.is_menu_visible(menu)
        assert not is_vis, f"El rol {role_name} NO debe ver el menu '{menu}' en el Sidebar, pero esta visible."

    # 4. Verificar que no haya errores criticos de JavaScript en la consola
    summary = web_logger.get_summary()
    assert len(summary["errors"]) == 0, f"Se detectaron errores JS en dashboard para rol {role_name}: {summary['errors']}"


@pytest.mark.parametrize("role_name,forbidden_route", FORBIDDEN_SCENARIOS)
def test_role_forbidden_routes_security(page, role_name, forbidden_route):
    """
    Prueba Negativa de Seguridad:
    Verifica que al intentar forzar el acceso directo por URL a rutas restringidas,
    el backend/frontend bloquee el acceso (HTTP 403, 404 o redireccion segura).
    """
    role_cfg = ROLES_CONFIG[role_name]
    login_page = LoginPage(page)

    # 1. Iniciar sesion
    login_page.load()
    login_page.login(role_cfg["username"], role_cfg["password"])
    assert login_page.is_login_successful(), f"Fallo inicio de sesion para {role_name}"

    # 2. Intentar acceder directamente a la URL prohibida
    target_url = f"{BASE_URL}{forbidden_route}"
    response = page.goto(target_url, wait_until="domcontentloaded")

    # 3. Validar que la respuesta sea de bloqueo o redirección fuera del recurso protegido
    current_url = page.url
    status_code = response.status if response else None

    is_blocked = (
        status_code in (403, 404, 302) or 
        "error" in current_url or 
        forbidden_route not in current_url or
        "/dashboard" in current_url or
        "Acceso denegado" in page.content() or
        "Forbidden" in page.content()
    )

    assert is_blocked, (
        f"ALERTA DE SEGURIDAD: El rol '{role_name}' pudo acceder a la ruta prohibida '{forbidden_route}' "
        f"con estado HTTP {status_code}. URL actual: {current_url}"
    )


@pytest.mark.parametrize("role_name", ALL_ROLES)
def test_role_allowed_routes_accessibility(page, web_logger, role_name):
    """
    Verifica que cada rol pueda navegar limpiamente por sus rutas asignadas
    sin experimentar errores HTTP 500 ni excepciones no controladas de frontend.
    """
    role_cfg = ROLES_CONFIG[role_name]
    login_page = LoginPage(page)

    login_page.load()
    login_page.login(role_cfg["username"], role_cfg["password"])
    assert login_page.is_login_successful(), f"Fallo inicio de sesion para {role_name}"

    # Navegar por las rutas clave
    for route in role_cfg["allowed_routes"]:
        target_url = f"{BASE_URL}{route}"
        resp = page.goto(target_url, wait_until="domcontentloaded")
        assert resp is not None, f"Sin respuesta al navegar a {route} con rol {role_name}"
        assert resp.status < 500, f"Error del servidor HTTP {resp.status} al acceder a {route} con rol {role_name}"
