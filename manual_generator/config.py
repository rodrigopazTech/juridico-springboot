"""
Configuración central del generador de manual SIAJ.
Todos los parámetros institucionales se definen aquí.
"""

import os

# ─── RUTAS ───────────────────────────────────────────────────────────────────
BASE_DIR        = os.path.dirname(os.path.abspath(__file__))
ASSETS_DIR      = os.path.join(BASE_DIR, "assets")
SCREENSHOTS_DIR = os.path.join(ASSETS_DIR, "screenshots")
LOGO_PATH       = os.path.join(ASSETS_DIR, "logo.jpg")
OUTPUT_PATH     = os.path.join(BASE_DIR, "..", "Manual_Usuario_SIAJ_v1.0.docx")

# ─── SERVIDOR ────────────────────────────────────────────────────────────────
BASE_URL = "http://30.0.0.151"      # Servidor de pruebas (app corriendo)
# BASE_URL = "https://siaj.finabien"  # Producción (cuando esté listo)

# ─── CREDENCIALES ROL ABOGADO Y ADMIN ─────────────────────────────────────────
LOGIN_USER = "qa.abogado"
LOGIN_PASS = "password123"
ADMIN_USER = "qa.admin"
ADMIN_PASS = "password123"

# ─── DATOS INSTITUCIONALES ───────────────────────────────────────────────────
INSTITUCION   = "Financiera para el Bienestar"
SISTEMA       = "Sistema Integral de Asuntos Jurídicos"
SIGLAS        = "SIAJ"
VERSION       = "Versión 1.0"
AREA          = "Área Jurídica"
ROL_MANUAL    = "Abogado / Operativo"

# ─── COLORES INSTITUCIONALES (RGB para python-docx) ──────────────────────────
COLOR_VERDE_OSCURO  = "006B53"   # Verde oscuro institucional
COLOR_VERDE_MEDIO   = "008C45"   # Verde medio
COLOR_DORADO        = "C29A4A"   # Dorado institucional
COLOR_DORADO_CLARO  = "D8C08A"   # Dorado claro
COLOR_BLANCO        = "FFFFFF"
COLOR_GRIS_TEXTO    = "333333"
COLOR_GRIS_SUAVE    = "F5F5F5"

# ─── DIMENSIONES LOGO (px originales: 506 x 101) ─────────────────────────────
# Relación de aspecto: 5.01:1 → a 6 cm de ancho → alto ≈ 1.20 cm
LOGO_WIDTH_CM  = 6.0
LOGO_HEIGHT_CM = round(6.0 * (101 / 506), 2)  # ≈ 1.20 cm

# ─── VIEWPORT DE PLAYWRIGHT ──────────────────────────────────────────────────
VIEWPORT = {"width": 1440, "height": 900}
SCREENSHOT_TIMEOUT = 8000   # ms de espera antes de capturar

# ─── PANTALLAS A CAPTURAR ────────────────────────────────────────────────────
# Cada entrada: (slug_archivo, ruta_relativa, accion_extra)
# accion_extra puede ser None o una función que se aplica en screenshots.py
SCREENS = [
    ("00_login",           "/login",        None),
    ("01_dashboard",       "/dashboard",    None),
    ("02_expedientes",     "/expedientes",  None),
    ("03_audiencias",      "/audiencias",   None),
    ("04_terminos",        "/terminos",     None),
    ("05_trazabilidad",    "/trazabilidad", None),
    ("06_documentos",      "/documentos",   None),
    ("07_catalogos",       "/catalogos",    None),
    ("08_perfil",          "/perfil",       None),
    ("09_tareas",          "/tareas",       None),
    ("10_usuarios",        "/usuarios",     None),
    ("11_config_semaforos", "/catalogos",   None),
]
