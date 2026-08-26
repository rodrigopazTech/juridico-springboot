import time
from pathlib import Path
from playwright.sync_api import Page, expect
from tests_e2e.config.settings import BASE_URL, SCREENSHOTS_DIR, BROWSER_TIMEOUT

class BasePage:
    """Clase base para todos los Page Objects."""
    def __init__(self, page: Page):
        self.page = page
        self.base_url = BASE_URL
        self.page.set_default_timeout(BROWSER_TIMEOUT)

    def navigate_to(self, path: str):
        full_url = f"{self.base_url}{path}" if path.startswith("/") else path
        return self.page.goto(full_url, wait_until="domcontentloaded")

    def take_screenshot(self, name: str) -> Path:
        timestamp = time.strftime("%Y%m%d_%H%M%S")
        filename = f"{name}_{timestamp}.png"
        filepath = SCREENSHOTS_DIR / filename
        self.page.screenshot(path=str(filepath), full_page=True)
        return filepath

    def get_current_path(self) -> str:
        url = self.page.url
        # Extraer el path relativo (e.g. /views/dashboard o /dashboard)
        if self.base_url in url:
            return url.replace(self.base_url, "")
        return url

    def wait_for_seconds(self, seconds: float):
        time.sleep(seconds)
