from playwright.sync_api import Page
from tests_e2e.pages.base_page import BasePage

class UsuariosPage(BasePage):
    """Page Object para la gestion de Usuarios."""

    PATH = "/usuarios"
    BTN_AGREGAR = "button:has-text('Agregar Usuario')"
    INPUT_SEARCH = "#input-search-usuarios"
    USERS_CARDS = "#lista-abogados .abogado-card"

    def __init__(self, page: Page):
        super().__init__(page)

    def load(self):
        return self.navigate_to(self.PATH)

    def is_loaded(self) -> bool:
        try:
            self.page.wait_for_selector(self.INPUT_SEARCH, timeout=8000)
            return True
        except Exception:
            return False

    def is_btn_agregar_visible(self) -> bool:
        try:
            return self.page.locator(self.BTN_AGREGAR).is_visible()
        except Exception:
            return False

    def get_users_count(self) -> int:
        return self.page.locator(self.USERS_CARDS).count()
