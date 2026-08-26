from playwright.sync_api import Page
from tests_e2e.pages.base_page import BasePage

class LoginPage(BasePage):
    """Page Object para la pantalla de inicio de sesion (Auth)."""
    
    # Selectores
    INPUT_USERNAME = "input#username"
    INPUT_PASSWORD = "input#password"
    BTN_SUBMIT = "button[type='submit']"
    ALERT_ERROR = "div[role='alert']"
    SIDEBAR_LOGO = "#logo-sidebar"

    def __init__(self, page: Page):
        super().__init__(page)

    def load(self):
        return self.navigate_to("/login")

    def login(self, username: str, password: str):
        self.page.fill(self.INPUT_USERNAME, username)
        self.page.fill(self.INPUT_PASSWORD, password)
        self.page.click(self.BTN_SUBMIT)
        self.page.wait_for_load_state("domcontentloaded")

    def is_login_successful(self) -> bool:
        # Tras login exitoso, debe redirigir a /dashboard o mostrar el sidebar
        try:
            self.page.wait_for_selector(self.SIDEBAR_LOGO, timeout=8000)
            return True
        except Exception:
            return "/dashboard" in self.page.url or ("/login" not in self.page.url and "/" != self.get_current_path())

    def get_error_message(self) -> str:
        try:
            return self.page.text_content(self.ALERT_ERROR, timeout=3000) or ""
        except Exception:
            return ""

    def logout(self):
        self.navigate_to("/logout")
        self.page.wait_for_load_state("domcontentloaded")
