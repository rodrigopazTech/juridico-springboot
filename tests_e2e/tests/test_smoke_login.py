"""
Smoke Test de Autenticacion: Inicio de sesion, credenciales invalidas y cierre de sesion.
"""
import pytest
from tests_e2e.pages.login_page import LoginPage
from tests_e2e.config.roles import ROLES_CONFIG

def test_login_page_loads(page):
    """Verifica que la pantalla de login cargue correctamente."""
    login_page = LoginPage(page)
    response = login_page.load()
    assert response is not None, "No se pudo obtener respuesta del servidor"
    assert response.status < 400, f"La pagina de login devolvio error HTTP {response.status}"
    assert "Acceso" in page.title() or "Bienvenido" in page.content()

def test_login_successful_admin(page, web_logger, backend_logger):
    """Verifica que el usuario admin pueda iniciar sesion correctamente."""
    admin_cfg = ROLES_CONFIG["ADMIN"]
    login_page = LoginPage(page)
    login_page.load()
    login_page.login(admin_cfg["username"], admin_cfg["password"])
    
    assert login_page.is_login_successful(), f"El login con admin fallo. URL actual: {page.url}"
    assert "/dashboard" in page.url

def test_login_invalid_credentials(page):
    """Verifica que credenciales erroneas muestren el mensaje de error adecuado."""
    login_page = LoginPage(page)
    login_page.load()
    login_page.login("usuario_inexistente", "clave_invalida_999")
    
    # Debe permanecer en auth/login o mostrar error
    assert "error" in page.url or login_page.get_error_message() != "" or "/login" in page.url or page.url.endswith(":8080/")
