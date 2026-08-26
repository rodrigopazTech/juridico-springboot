from typing import List, Optional
from playwright.sync_api import Page
from tests_e2e.pages.base_page import BasePage

class ExpedientesPage(BasePage):
    """Page Object para la pantalla y gestion de Expedientes."""

    PATH = "/expedientes"
    INPUT_SEARCH = "#input-search-expedientes"
    BTN_SEARCH = "#form-search-expedientes button[type='submit']"
    BTN_CLEAR_SEARCH = "#btn-clear-search-expedientes"
    BTN_NUEVO = "button:has-text('Nuevo Expediente')"
    BTN_EXPORTAR = "a[title='Descargar Reporte Excel']"
    MODAL_NUEVO = "#modal-nuevo-exp"
    TABLA_FILAS = "#tabla-expedientes-body tr"

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

    def is_btn_nuevo_visible(self) -> bool:
        return self.page.locator(self.BTN_NUEVO).is_visible()

    def is_btn_exportar_visible(self) -> bool:
        return self.page.locator(self.BTN_EXPORTAR).is_visible()

    def search(self, keyword: str):
        self.page.fill(self.INPUT_SEARCH, keyword)
        self.page.keyboard.press("Enter")
        self.page.wait_for_load_state("domcontentloaded")

    def get_table_rows_count(self) -> int:
        return self.page.locator(self.TABLA_FILAS).count()

    def open_modal_nuevo(self) -> bool:
        if self.is_btn_nuevo_visible():
            self.page.click(self.BTN_NUEVO)
            try:
                self.page.wait_for_selector(self.MODAL_NUEVO, state="visible", timeout=3000)
                return True
            except Exception:
                return False
        return False
