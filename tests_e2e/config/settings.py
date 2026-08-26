import os
from pathlib import Path
from dotenv import load_dotenv

# Cargar variables de entorno desde el .env del proyecto raiz si existe
ROOT_DIR = Path(__file__).resolve().parent.parent.parent
load_dotenv(ROOT_DIR / ".env")

BASE_URL = os.getenv("QA_BASE_URL", "http://localhost:8080").rstrip("/")
DEFAULT_PASSWORD = os.getenv("QA_PASSWORD", "12345")
HEADLESS = os.getenv("QA_HEADLESS", "true").lower() in ("true", "1", "yes")
BROWSER_TIMEOUT = int(os.getenv("QA_TIMEOUT_MS", "15000"))

# Rutas de artefactos y reportes
REPORTS_DIR = ROOT_DIR / "tests_e2e" / "reports"
SCREENSHOTS_DIR = REPORTS_DIR / "screenshots"
SCREENSHOTS_DIR.mkdir(parents=True, exist_ok=True)

# Rutas de logs del backend
BACKEND_LOG_PATH = ROOT_DIR / "server_log.txt"
if not BACKEND_LOG_PATH.exists():
    BACKEND_LOG_PATH = ROOT_DIR / "app.log"
