"""
main.py — Orquestador del generador de Manual de Usuario SIAJ.
Ejecuta las capturas de pantalla y luego construye el documento Word.

Uso:
    python main.py                # Captura + genera documento
    python main.py --skip-shots  # Solo genera documento (usa capturas existentes)
"""

import sys
import os
import time

# Asegurar que los imports locales funcionen correctamente
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from config import SCREENSHOTS_DIR, SCREENS, OUTPUT_PATH


def print_banner():
    print("\n" + "=" * 60)
    print("  GENERADOR DE MANUAL DE USUARIO - SIAJ")
    print("  Financiera para el Bienestar")
    print("=" * 60)


def check_screenshots_exist() -> list:
    """Retorna lista de slugs que YA tienen captura guardada."""
    existing = []
    for slug, _, _ in SCREENS:
        path = os.path.join(SCREENSHOTS_DIR, f"{slug}.png")
        if os.path.exists(path):
            existing.append(slug)
    return existing


def main():
    print_banner()
    skip_shots = "--skip-shots" in sys.argv

    # ─── PASO 1: CAPTURAS ─────────────────────────────────────────────────
    if skip_shots:
        print("\n[SKIP] Omitiendo capturas (--skip-shots). Usando imagenes existentes.")
        captured = check_screenshots_exist()
        print(f"   Capturas disponibles: {len(captured)}/{len(SCREENS)}")
    else:
        print("\n[1/2] Capturando pantallas con Playwright...")
        t0 = time.time()
        from screenshots import capture_all
        captured = capture_all()
        elapsed = round(time.time() - t0, 1)
        print(f"\n   Tiempo de captura: {elapsed}s")

    # ─── PASO 2: GENERAR DOCUMENTO ────────────────────────────────────────
    print("\n[2/2] Construyendo documento Word...")
    t0 = time.time()
    from builder import build_document
    out_path = build_document(captured)
    elapsed = round(time.time() - t0, 1)
    print(f"   Tiempo de generacion: {elapsed}s")

    # ─── RESUMEN ─────────────────────────────────────────────────────────
    print("\n" + "=" * 60)
    print(f"[OK] MANUAL GENERADO EXITOSAMENTE")
    print(f"   Archivo: {os.path.abspath(out_path)}")
    print(f"   Capturas incluidas: {len(captured)}/{len(SCREENS)}")
    print("=" * 60 + "\n")


if __name__ == "__main__":
    main()
