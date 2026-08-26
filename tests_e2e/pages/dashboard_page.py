from typing import List
from playwright.sync_api import Page
from tests_e2e.pages.base_page import BasePage

class DashboardPage(BasePage):
    """Page Object para el Dashboard y navegación del menú lateral (Sidebar)."""

    SIDEBAR = "#logo-sidebar"
    MENU_ITEMS = "#logo-sidebar ul li a"
    USER_PROFILE_MENU = "#user-menu-button"

    # Mapeo de nombres legibles a paths de los enlaces
    MENU_PATH_MAPPING = {
        "Dashboard": "/dashboard",
        "Expedientes": "/expedientes",
        "Términos": "/terminos",
        "Audiencias": "/audiencias",
        "Calendario": "/calendario",
        "Mis Tareas": "/tareas",
        "Concluidos": "/agenda",
        "Catálogos": "/catalogos",
        "Usuarios": "/usuarios",
        "Migración Inteligente": "/admin/importar",
        "Auditoria Tecnica": "/admin/auditoria"
    }

    def __init__(self, page: Page):
        super().__init__(page)

    def is_loaded(self) -> bool:
        try:
            self.page.wait_for_selector(self.SIDEBAR, timeout=8000)
            return True
        except Exception:
            return False

    def get_visible_menu_texts(self) -> List[str]:
        """Obtiene la lista de textos de los menús visibles en el sidebar."""
        self.page.wait_for_selector(self.SIDEBAR, timeout=5000)
        items = self.page.locator(self.MENU_ITEMS).all()
        texts = []
        for item in items:
            if item.is_visible():
                text = item.inner_text().strip()
                if text:
                    texts.append(text)
        return texts

    def get_visible_menu_hrefs(self) -> List[str]:
        """Obtiene la lista de rutas href de los enlaces visibles en el sidebar."""
        self.page.wait_for_selector(self.SIDEBAR, timeout=5000)
        items = self.page.locator(self.MENU_ITEMS).all()
        hrefs = []
        for item in items:
            if item.is_visible():
                href = item.get_attribute("href")
                if href:
                    hrefs.append(href)
        return hrefs

    def is_menu_visible(self, menu_name: str) -> bool:
        """Verifica si un menú específico (por nombre o ruta) está visible en el sidebar."""
        target_path = self.MENU_PATH_MAPPING.get(menu_name, menu_name)
        selector = f"{self.SIDEBAR} a[href*='{target_path}']"
        try:
            loc = self.page.locator(selector)
            count = loc.count()
            if count == 0:
                return False
            # Si hay varios elementos (ej. uno oculto y uno visible), verificar si alguno es visible
            return any(loc.nth(i).is_visible() for i in range(count))
        except Exception:
            return False
