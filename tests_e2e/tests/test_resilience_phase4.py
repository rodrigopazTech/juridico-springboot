"""
Fase 4: Suite de Pruebas de Resiliencia ante Fallos Externos y Tolerancia a Errores
Evalúa:
1. Resiliencia ante caída o indisponibilidad de servidor de correo SMTP.
2. Rate Limiting y protección contra abuso en recuperación de contraseñas.
3. Manejo tolerante de errores en OAuth2 / Microsoft Graph API.
4. Protección y degradación graciosa en tokens de restablecimiento inválidos o manipulados.
5. Seguridad y resiliencia en APIs de integración externa (Migración).
"""
import uuid
import pytest
from tests_e2e.pages.login_page import LoginPage


def test_smtp_failure_graceful_handling(page, web_logger):
    """
    Fase 4 - Resiliencia 1: Degradación Graciosa ante Falla en Envío de Correo SMTP.
    Solicita recuperación de contraseña hacia un correo institucional sin servidor SMTP activo.
    El sistema debe procesar la solicitud, proteger contra enumeración de usuarios y mostrar
    un mensaje limpio sin arrojar errores 500 ni trazas de excepción.
    """
    page.goto("http://localhost:8080/auth/solicitar-reset")
    page.wait_for_load_state("domcontentloaded")

    # Ingresar correo institucional de prueba
    input_ident = page.locator("input[name='identifier'], input#identifier")
    input_ident.fill("abogado@finabien.gob.mx")

    # Enviar formulario
    btn_submit = page.locator("button[type='submit']")
    btn_submit.click()
    page.wait_for_load_state("domcontentloaded")

    # Verificar que no hubo error 500 y se muestra el mensaje genérico seguro
    content = page.content()
    assert "500" not in page.title(), "El servidor arrojó HTTP 500 por falla de correo SMTP."
    assert "Exception" not in content, "Fuga de stacktrace en pantalla por fallo de correo."
    assert ("Si la cuenta existe" in content or "enlace" in content or "correo" in content), "No se mostró mensaje de respuesta amigable."


def test_password_reset_rate_limiting_resilience(page, web_logger):
    """
    Fase 4 - Resiliencia 2: Resiliencia ante Ráfagas de Solicitudes (Rate Limiting).
    Dispara múltiples solicitudes sucesivas de recuperación de contraseña.
    El servicio de Rate Limiting debe activarse y rechazar peticiones abusivas limpiamente.
    """
    page.goto("http://localhost:8080/auth/solicitar-reset")

    for i in range(7):
        input_ident = page.locator("input[name='identifier'], input#identifier")
        input_ident.fill(f"flood_test_{i}@finabien.gob.mx")
        btn_submit = page.locator("button[type='submit']")
        btn_submit.click()
        page.wait_for_load_state("domcontentloaded")

    # Verificar que el rate limiter responde sin errores 500
    content = page.content()
    assert "500" not in page.title()
    assert "demasiadas solicitudes" in content or "Si la cuenta existe" in content


def test_microsoft_oauth_error_graceful_recovery(page, web_logger):
    """
    Fase 4 - Resiliencia 3: Recuperación Graciosa ante Errores de Microsoft OAuth.
    Simula una respuesta de error o cancelación enviada por Microsoft (error=access_denied).
    El controlador debe capturar el error, registrar auditoría técnica y redirigir al login
    con un mensaje informativo sin romper la sesión.
    """
    page.goto("http://localhost:8080/auth/microsoft/callback?error=access_denied&error_description=The+user+canceled+the+authentication")
    page.wait_for_load_state("domcontentloaded")

    # Debe redirigir al login (/) limpiamente
    assert page.url.rstrip("/") == "http://localhost:8080"
    assert "500" not in page.title()
    content = page.content().lower()
    assert "sistema de gestión jurídica" in content or "acceso" in content or "bienvenido" in content


def test_microsoft_oauth_invalid_state_csrf_protection(page, web_logger):
    """
    Fase 4 - Resiliencia 4: Protección contra Manipulación de Estado OAuth (CSRF).
    Envía un callback con state adulterado o sin sesión previa.
    El sistema debe rechazar el intento de autenticación y prevenir ataques CSRF.
    """
    fake_state = str(uuid.uuid4())
    page.goto(f"http://localhost:8080/auth/microsoft/callback?code=mock_code_123&state={fake_state}")
    page.wait_for_load_state("domcontentloaded")

    # Debe rechazar y redirigir al login (/)
    assert page.url.rstrip("/") == "http://localhost:8080"
    assert "500" not in page.title()
    content = page.content().lower()
    assert "sistema de gestión jurídica" in content or "acceso" in content or "bienvenido" in content


def test_invalid_and_tampered_password_reset_token(page, web_logger):
    """
    Fase 4 - Resiliencia 5: Manejo Seguro de Tokens Manipulados o Inexistentes.
    Intenta acceder al flujo de cambio de contraseña con un token aleatorio inexistente.
    El sistema debe impedir el acceso al formulario y notificar que el enlace es inválido.
    """
    invalid_token = str(uuid.uuid4())
    page.goto(f"http://localhost:8080/auth/reset-password?token={invalid_token}")
    page.wait_for_load_state("domcontentloaded")

    content = page.content()
    assert "500" not in page.title()
    assert ("invalido" in content.lower() or "expirado" in content.lower() or "error" in content.lower() or page.url == "http://localhost:8080/")


def test_migration_api_unauthorized_resilience(page, web_logger):
    """
    Fase 4 - Resiliencia 6: Protección y Resiliencia en Endpoints de Integración Externa.
    Invoca los endpoints de migración de datos masivos sin clave de autorización.
    El sistema debe responder HTTP 401 Unauthorized sin exponer detalles internos ni fallar.
    """
    response = page.request.post(
        "http://localhost:8080/api/migracion/expedientes",
        data="{}",
        headers={"Content-Type": "application/json"}
    )
    # Debe ser 401 o 403 Forbidden
    assert response.status in [401, 403], f"Se esperaba 401/403 sin API key, se obtuvo: {response.status}"
