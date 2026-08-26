from playwright.sync_api import Page
from tests_e2e.pages.base_page import BasePage

class AudienciasPage(BasePage):
    """Page Object para la pantalla de Audiencias."""

    PATH = "/audiencias"
    GRID_MAESTRO = "#grid-maestro-audiencias"
    KPI_TOTAL = "#kpi-card-total"
    BTN_CALENDARIO = "#btn-ver-calendario"
    BTN_FILTROS = "#btn-ver-filtros"

    def __init__(self, page: Page):
        super().__init__(page)

    def load(self):
        return self.navigate_to(self.PATH)

    def is_loaded(self) -> bool:
        try:
            self.page.wait_for_selector(self.GRID_MAESTRO, timeout=8000)
            return True
        except Exception:
            return False

    def is_kpi_visible(self) -> bool:
        try:
            return self.page.locator(self.KPI_TOTAL).is_visible()
        except Exception:
            return False
