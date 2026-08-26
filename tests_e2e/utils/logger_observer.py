import os
import time
from typing import List, Dict, Any
from pathlib import Path
from playwright.sync_api import Page

class BrowserConsoleObserver:
    """
    Observador y recolector de eventos de la consola de JavaScript en el navegador.
    Captura console.error, console.warn, excepciones no controladas y fallos de red.
    """
    def __init__(self):
        self.logs: List[Dict[str, Any]] = []
        self.errors: List[str] = []
        self.warnings: List[str] = []

    def attach_to_page(self, page: Page):
        self.logs.clear()
        self.errors.clear()
        self.warnings.clear()

        def on_console(msg):
            entry = {
                "type": msg.type,
                "text": msg.text,
                "location": msg.location
            }
            self.logs.append(entry)
            
            # Filtrar advertencias benignas conocidas (ej. Tailwind CDN banner)
            is_benign_warning = "cdn.tailwindcss.com should not be used in production" in msg.text

            if msg.type == "error":
                self.errors.append(f"[JS_ERROR] {msg.text} ({msg.location})")
            elif msg.type == "warning" and not is_benign_warning:
                self.warnings.append(f"[JS_WARN] {msg.text} ({msg.location})")

        def on_page_error(exc):
            err_msg = f"[PAGE_EXCEPTION] {str(exc)}"
            self.logs.append({"type": "exception", "text": str(exc)})
            self.errors.append(err_msg)

        def on_request_failed(request):
            failure = request.failure
            fail_msg = f"[NETWORK_FAIL] {request.method} {request.url} - {failure}"
            self.logs.append({"type": "network_fail", "text": fail_msg})
            # Solo considerar error si no es por cancelación normal de navegación o websocket cerrado
            if failure and "net::ERR_ABORTED" not in failure and "ERR_CONNECTION_REFUSED" not in failure:
                self.errors.append(fail_msg)

        page.on("console", on_console)
        page.on("pageerror", on_page_error)
        page.on("requestfailed", on_request_failed)

    def get_summary(self) -> Dict[str, Any]:
        return {
            "total_logs": len(self.logs),
            "errors_count": len(self.errors),
            "warnings_count": len(self.warnings),
            "errors": self.errors,
            "warnings": self.warnings
        }


class BackendLogObserver:
    """
    Observador de los logs del backend de Spring Boot.
    Lee las líneas emitidas durante la ejecución de cada prueba para detectar 
    excepciones (ERROR, FATAL, Exception, Stacktraces).
    """
    def __init__(self, log_path: Path):
        self.log_path = Path(log_path)
        self._start_pos = 0

    def start_recording(self):
        """Marca el punto de inicio de la prueba en el archivo de log."""
        if self.log_path.exists():
            try:
                self._start_pos = self.log_path.stat().st_size
            except Exception:
                self._start_pos = 0
        else:
            self._start_pos = 0

    def get_new_logs(self) -> str:
        """Obtiene las nuevas líneas generadas desde el start_recording."""
        if not self.log_path.exists():
            return ""
        try:
            with open(self.log_path, "r", encoding="utf-8", errors="replace") as f:
                f.seek(self._start_pos)
                return f.read()
        except Exception as e:
            return f"Error leyendo log de backend: {e}"

    def get_errors(self) -> List[str]:
        """Filtra y devuelve solo las líneas que contienen errores o excepciones reales."""
        content = self.get_new_logs()
        if not content:
            return []
        error_lines = []
        for line in content.splitlines():
            # Excluir advertencias o logs benignos
            if any(k in line for k in ("ERROR", "FATAL", "NullPointerException", "500 Internal Server Error")):
                if "WebPushService" not in line:  # Ignorar warning informativo de webpush si aparece como error
                    error_lines.append(line.strip())
        return error_lines
