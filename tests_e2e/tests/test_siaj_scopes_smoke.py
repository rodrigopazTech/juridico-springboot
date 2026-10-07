import pytest

from tests_e2e.config.roles import ROLES_CONFIG
from tests_e2e.pages.login_page import LoginPage


@pytest.mark.parametrize("role_name", ["ADMIN", "JEFE_DEPTO", "ABOGADO"])
def test_modulos_siaj_2_y_permisos_de_configuracion(page, web_logger, role_name):
    """Valida módulos SIAJ 2.0 y la matriz visual de Plazos y alertas."""
    role = ROLES_CONFIG[role_name]
    login = LoginPage(page)
    login.load()
    login.login(role["username"], role["password"])
    assert login.is_login_successful()

    for ruta, texto in [
        ("/terminos", "TÉRMINOS"),
        ("/audiencias", "AUDIENCIAS"),
        ("/asuntos-independientes", "Asuntos Independientes"),
        ("/administracion/plazos-alertas", "Plazos y alertas"),
    ]:
        response = page.goto(f"http://localhost:8080{ruta}", wait_until="domcontentloaded")
        assert response is not None and response.status < 400, f"{ruta} devolvió {response.status if response else 'sin respuesta'}"
        assert texto.lower() in page.locator("body").inner_text().lower()
        errores = web_logger.get_summary()["errors"]
        assert errores == [], f"Errores de navegador en {ruta}: {errores}"

    puede_gestionar = role_name in ("ADMIN", "JEFE_DEPTO")
    assert page.locator("#form-tipo-termino").is_visible() == puede_gestionar
    assert page.locator("#form-regla-termino").is_visible() == puede_gestionar
    assert page.locator("#form-tipo-audiencia").is_visible() == puede_gestionar
    assert page.locator("#form-regla-audiencia").is_visible() == puede_gestionar

    puede_calendario = role_name == "ADMIN"
    assert page.locator('form[action="/administracion/plazos-alertas/calendario"]').is_visible() == puede_calendario
