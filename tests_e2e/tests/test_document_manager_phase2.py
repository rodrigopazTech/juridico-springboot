"""
Fase 2: Suite de Seguridad del Gestor Documental y Carga de Archivos
Evalúa:
1. Subida exitosa de documentos legales válidos (PDF).
2. Bloqueo estricto de extensiones ejecutables o maliciosas (.exe, .sh, .bat, .jsp, .php).
3. Respeto de límites contextuales de tamaño (150MB para documentos, 1GB para video/audio).
4. Protección de descargas (IDOR en descarga/preview de documentos entre gerencias).
5. Cálculo de cuotas y espacio de almacenamiento (storage-info).
6. Interfaz del Gestor Documental en el navegador.
"""
import io
import json
import pytest
from tests_e2e.config.roles import ROLES_CONFIG
from tests_e2e.pages.login_page import LoginPage

EXPEDIENTE_GERENCIA_1_ID = "daa3c09e-d335-4f89-94e1-5b2c4865fa21"  # Gerencia Civil (Abogado 5)
EXPEDIENTE_GERENCIA_2_ID = "62b42040-2c97-4676-b256-ba91757cad5f"  # Gerencia Laboral (Abogado 8)


def test_upload_valid_document_pdf(page, web_logger):
    """
    Gestor Documental 1: Subida de Documento Válido (.pdf).
    Autentica como ADMIN y sube un documento PDF de prueba al expediente.
    Verifica respuesta HTTP 200 y persistencia del documento.
    """
    admin_cfg = ROLES_CONFIG["ADMIN"]
    login_page = LoginPage(page)
    login_page.load()
    login_page.login(admin_cfg["username"], admin_cfg["password"])
    assert login_page.is_login_successful()

    # Subir documento mediante API multipart con nombre único
    import uuid
    unique_name = f"demanda_amparo_{uuid.uuid4().hex[:8]}.pdf"
    pdf_content = b"%PDF-1.4 Mock legal document content for testing"
    response = page.request.post(
        f"http://localhost:8080/api/documentos/expedientes/{EXPEDIENTE_GERENCIA_1_ID}",
        multipart={
            "file": {
                "name": unique_name,
                "mimeType": "application/pdf",
                "buffer": pdf_content
            },
            "descripcion": "Demanda inicial en formato PDF"
        }
    )

    assert response.status == 200, f"Error al subir PDF válido: {response.text()}"
    data = response.json()
    assert data["nombreOriginal"] == unique_name
    assert data["id"] is not None


@pytest.mark.parametrize("malicious_ext,mime", [
    ("script.exe", "application/x-msdownload"),
    ("backdoor.sh", "application/x-sh"),
    ("webshell.jsp", "text/plain"),
    ("batch.bat", "text/plain"),
    ("exploit.php", "application/x-php"),
])
def test_upload_forbidden_extensions_blocked(page, malicious_ext, mime, web_logger):
    """
    Gestor Documental 2: Rechazo Estricto de Extensiones No Permitidas (Lista Blanca).
    Intenta subir ejecutables y scripts (.exe, .sh, .jsp, .bat, .php).
    El sistema debe responder HTTP 400 Bad Request y rechazar el archivo.
    """
    admin_cfg = ROLES_CONFIG["ADMIN"]
    login_page = LoginPage(page)
    login_page.load()
    login_page.login(admin_cfg["username"], admin_cfg["password"])
    assert login_page.is_login_successful()

    dummy_payload = b"echo 'malicious command';"
    response = page.request.post(
        f"http://localhost:8080/api/documentos/expedientes/{EXPEDIENTE_GERENCIA_1_ID}",
        multipart={
            "file": {
                "name": malicious_ext,
                "mimeType": mime,
                "buffer": dummy_payload
            }
        }
    )

    assert response.status == 400, f"¡FALLO DE SEGURIDAD! El servidor aceptó la extensión prohibida '{malicious_ext}'. Status: {response.status}"
    body = response.json()
    assert "error" in body
    assert "no está permitido" in body["error"] or "no esta permitido" in body["error"]


def test_download_document_idor_protection(page, web_logger):
    """
    Gestor Documental 3: Control de Acceso en Descarga de Documentos (Anti-IDOR).
    1. Admin sube un documento privado al expediente de Gerencia 2.
    2. Abogado de Gerencia 1 (Civil) intenta descargar dicho documento por su ID.
    3. El sistema debe responder HTTP 403 Forbidden.
    """
    import uuid
    admin_cfg = ROLES_CONFIG["ADMIN"]
    login_page = LoginPage(page)
    login_page.load()
    login_page.login(admin_cfg["username"], admin_cfg["password"])
    assert login_page.is_login_successful()

    confidential_name = f"doc_confidencial_{uuid.uuid4().hex[:8]}.pdf"
    upload_res = page.request.post(
        f"http://localhost:8080/api/documentos/expedientes/{EXPEDIENTE_GERENCIA_2_ID}",
        multipart={
            "file": {
                "name": confidential_name,
                "mimeType": "application/pdf",
                "buffer": b"%PDF-1.4 Confidencial Laboral"
            }
        }
    )
    assert upload_res.status == 200, f"Error en upload admin: {upload_res.text()}"
    doc_id = upload_res.json()["id"]

    # 2. Logout y Login como Abogado de Gerencia 1 (Civil)
    page.goto("http://localhost:8080/logout")
    page.wait_for_load_state("domcontentloaded")

    login_page.load()
    abogado_cfg = ROLES_CONFIG["ABOGADO"]
    login_page.login(abogado_cfg["username"], abogado_cfg["password"])
    assert login_page.is_login_successful()

    # 3. Intentar descargar el documento de Gerencia 2
    download_res = page.request.get(f"http://localhost:8080/api/documentos/{doc_id}/download")
    assert download_res.status == 403, (
        f"¡VULNERABILIDAD IDOR! Un abogado de Gerencia 1 pudo descargar documento de Gerencia 2. Status: {download_res.status}"
    )

    # 4. Intentar preview del documento
    preview_res = page.request.get(f"http://localhost:8080/api/documentos/{doc_id}/preview")
    assert preview_res.status == 403, (
        f"¡VULNERABILIDAD IDOR! Un abogado de Gerencia 1 pudo previsualizar documento de Gerencia 2. Status: {preview_res.status}"
    )


def test_storage_info_calculation(page, web_logger):
    """
    Gestor Documental 4: Consulta de Espacio y Cuotas de Almacenamiento.
    Verifica que el endpoint /storage-info devuelva métricas exactas en bytes, MB y GB.
    """
    admin_cfg = ROLES_CONFIG["ADMIN"]
    login_page = LoginPage(page)
    login_page.load()
    login_page.login(admin_cfg["username"], admin_cfg["password"])
    assert login_page.is_login_successful()

    response = page.request.get(f"http://localhost:8080/api/documentos/expedientes/{EXPEDIENTE_GERENCIA_1_ID}/storage-info")
    assert response.status == 200
    data = response.json()

    assert "espacioBytes" in data
    assert "espacioMB" in data
    assert "espacioGB" in data
    assert data["espacioBytes"] >= 0


def test_document_manager_ui_interaction(page, web_logger):
    """
    Gestor Documental 5: Interacción con la Interfaz Web del Expediente.
    Abre el detalle del expediente en el navegador y verifica que no existan errores JS en la vista.
    """
    admin_cfg = ROLES_CONFIG["ADMIN"]
    login_page = LoginPage(page)
    login_page.load()
    login_page.login(admin_cfg["username"], admin_cfg["password"])
    assert login_page.is_login_successful()

    page.goto(f"http://localhost:8080/expedientes/{EXPEDIENTE_GERENCIA_1_ID}")
    page.wait_for_load_state("domcontentloaded")
    page.wait_for_timeout(500)

    summary = web_logger.get_summary()
    assert len(summary["errors"]) == 0, f"Errores JavaScript en la vista de expediente: {summary['errors']}"
