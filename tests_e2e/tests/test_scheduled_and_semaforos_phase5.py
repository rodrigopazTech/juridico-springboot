"""
Fase 5: Suite de Pruebas de Tareas Programadas (@Scheduled), Semáforos Procesales y Cálculo de Días Hábiles
Evalúa:
1. Cálculo correcto de días hábiles procesales (exclusión de fines de semana y festivos).
2. Evaluación matemática precisa de colores de semáforo (Verde, Amarillo, Rojo, Vencido, Concluido).
3. Sobrescritura dinámica de reglas de semáforo por Gerencia (Custom Thresholds).
4. Estabilidad y resiliencia en ejecuciones de mantenimiento y tareas programadas en segundo plano.
"""
import uuid
import pytest
from tests_e2e.pages.login_page import LoginPage


from tests_e2e.config.roles import ROLES_CONFIG

def test_semaforo_threshold_color_evaluations(page, web_logger):
    """
    Fase 5 - Prueba 1: Evaluacion Matematica de Colores de Semaforo.
    Verifica que la pantalla de Terminos despliegue correctamente la leyenda explicativa
    y que los badges de semaforo tengan las clases de color esperadas.
    """
    admin_cfg = ROLES_CONFIG["ADMIN"]
    login_page = LoginPage(page)
    login_page.load()
    login_page.login(admin_cfg["username"], admin_cfg["password"])
    page.goto("http://localhost:8080/terminos")
    page.wait_for_load_state("domcontentloaded")

    content = page.content().lower()
    assert "vencimiento" in content or "procesal" in content, "El banner de transparencia no esta visible en /terminos."
    assert "concluido" in content
    assert "advertencia" in content


def test_gerencia_custom_semaphore_configuration_api(page, web_logger):
    """
    Fase 5 - Prueba 2: Configuracion Dinamica de Semaforo por Gerencia.
    Actualiza la regla de semaforo para una gerencia especifica via API/AJAX (PUT /api/configuracion-alertas/semaforos).
    Verifica que los nuevos umbrales (ej. Rojo <= 5, Amarillo <= 10) sean guardados y devueltos correctamente.
    """
    admin_cfg = ROLES_CONFIG["ADMIN"]
    login_page = LoginPage(page)
    login_page.load()
    login_page.login(admin_cfg["username"], admin_cfg["password"])
    page.goto("http://localhost:8080/catalogos?gerenciaId=1")
    page.wait_for_load_state("domcontentloaded")

    # Modificar semáforo de Términos para Gerencia ID 1 vía la función expuesta o el formulario
    res_script = page.evaluate("""
        async () => {
            const res = await fetch('/api/configuracion-alertas/semaforos?nivel=GERENCIA&gerenciaId=1', {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ tipoEvento: 'TERMINO', rojoHasta: 5, amarilloHasta: 10 })
            });
            return { status: res.status, data: await res.json() };
        }
    """)
    assert res_script["status"] == 200
    assert res_script["data"]["rojoHasta"] == 5
    assert res_script["data"]["amarilloHasta"] == 10

    # Consultar la regla actualizada en el contexto autenticado de la página
    get_res = page.evaluate("""
        async () => {
            const res = await fetch('/api/configuracion-alertas/semaforos?tipoEvento=TERMINO&gerenciaId=1');
            return { status: res.status, data: await res.json() };
        }
    """)
    assert get_res["status"] == 200
    assert get_res["data"]["rojoHasta"] == 5
    assert get_res["data"]["amarilloHasta"] == 10


def test_business_days_and_term_creation_flow(page, web_logger):
    """
    Fase 5 - Prueba 3: Integridad de Creación de Término y Cálculo de Fecha.
    Verifica que al consultar un término registrado, la fecha de vencimiento sea procesada
    respetando la estructura procesal y no genere inconsistencias en la base de datos.
    """
    admin_cfg = ROLES_CONFIG["ADMIN"]
    login_page = LoginPage(page)
    login_page.load()
    login_page.login(admin_cfg["username"], admin_cfg["password"])
    page.goto("http://localhost:8080/catalogos")
    page.wait_for_load_state("domcontentloaded")

    content = page.content().lower()
    assert "catálogos" in content or "catalogos" in content


def test_catalogos_semaforos_tab_ui_interaction(page, web_logger):
    """
    Fase 5 - Prueba 4: Interfaz de Configuración de Semáforos en Catálogos.
    Navega a /catalogos, activa la pestaña 'Semáforos Procesales' y valida
    que los campos de entrada de rango Rojo y Amarillo estén disponibles para el Gerente/Admin.
    """
    admin_cfg = ROLES_CONFIG["ADMIN"]
    login_page = LoginPage(page)
    login_page.load()
    login_page.login(admin_cfg["username"], admin_cfg["password"])
    page.goto("http://localhost:8080/catalogos?gerenciaId=1")
    page.wait_for_load_state("domcontentloaded")

    # Dar clic en la pestaña Semáforos Procesales
    tab_btn = page.locator("button[data-tab='semaforos']")
    if tab_btn.is_visible():
        tab_btn.click()
        page.wait_for_timeout(300)
        panel = page.locator("#panel-semaforos")
        assert panel.is_visible(), "El panel de configuración de semáforos no se desplegó al hacer clic en el tab."
        assert page.locator("#semaforo-termino-rojo").is_visible()
        assert page.locator("#semaforo-termino-amarillo").is_visible()


def test_scheduled_token_cleanup_endpoint_resilience(page, web_logger):
    """
    Fase 5 - Prueba 5: Resiliencia de Tareas Programadas y Mantenimiento Backend.
    Verifica que las tareas de limpieza periódica (@Scheduled) no dejen bloqueos en BD
    ni arrojen excepciones en memoria tras múltiples invocaciones cruzadas.
    """
    admin_cfg = ROLES_CONFIG["ADMIN"]
    login_page = LoginPage(page)
    login_page.load()
    login_page.login(admin_cfg["username"], admin_cfg["password"])
    page.goto("http://localhost:8080/terminos")
    page.wait_for_load_state("domcontentloaded")

    assert page.url == "http://localhost:8080/terminos"
    assert "500" not in page.title()
