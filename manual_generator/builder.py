"""
builder.py — Construcción del documento Word con python-docx.
Genera el Manual de Usuario SIAJ v1.0 con estilos institucionales
de Financiera para el Bienestar.
"""

import os
from datetime import date
from docx import Document
from docx.shared import Cm, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_ALIGN_VERTICAL, WD_TABLE_ALIGNMENT
from docx.oxml.ns import qn
from docx.oxml import OxmlElement

from config import (
    LOGO_PATH, OUTPUT_PATH, SCREENSHOTS_DIR,
    LOGO_WIDTH_CM, LOGO_HEIGHT_CM,
    COLOR_VERDE_OSCURO, COLOR_VERDE_MEDIO,
    COLOR_DORADO, COLOR_DORADO_CLARO,
    COLOR_BLANCO, COLOR_GRIS_TEXTO, COLOR_GRIS_SUAVE,
    INSTITUCION, SISTEMA, SIGLAS, VERSION, AREA, ROL_MANUAL
)
from content import (
    INTRO_SISTEMA, INTRO_OBJETIVO, INTRO_ALCANCE, REQUISITOS_NAVEGADOR,
    GLOSARIO, SCREENS_CONTENT, FAQ, CONTACTO
)


# ─── HELPERS DE FORMATO ──────────────────────────────────────────────────────

def _hex_to_rgb(hex_color: str):
    h = hex_color.lstrip("#")
    return RGBColor(int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16))


def _set_cell_bg(cell, hex_color: str):
    """Establece el color de fondo de una celda."""
    tc = cell._tc
    tcPr = tc.get_or_add_tcPr()
    shd = OxmlElement("w:shd")
    shd.set(qn("w:val"), "clear")
    shd.set(qn("w:color"), "auto")
    shd.set(qn("w:fill"), hex_color.lstrip("#"))
    tcPr.append(shd)


def _set_cell_margins(cell, top=100, bottom=100, left=150, right=150):
    """Establece el padding interior de una celda en dxa."""
    tcPr = cell._tc.get_or_add_tcPr()
    tcMar = OxmlElement('w:tcMar')
    for m, val in [('top', top), ('bottom', bottom), ('left', left), ('right', right)]:
        node = OxmlElement(f'w:{m}')
        node.set(qn('w:w'), str(val))
        node.set(qn('w:type'), 'dxa')
        tcMar.append(node)
    tcPr.append(tcMar)


def _add_horizontal_rule(doc, color_hex: str = COLOR_VERDE_OSCURO, thickness: int = 12):
    """Agrega una línea horizontal decorativa."""
    p = doc.add_paragraph()
    p.paragraph_format.space_before = Pt(0)
    p.paragraph_format.space_after  = Pt(6)
    pPr = p._p.get_or_add_pPr()
    pBdr = OxmlElement("w:pBdr")
    bottom = OxmlElement("w:bottom")
    bottom.set(qn("w:val"),   "single")
    bottom.set(qn("w:sz"),    str(thickness))
    bottom.set(qn("w:space"), "1")
    bottom.set(qn("w:color"), color_hex.lstrip("#"))
    pBdr.append(bottom)
    pPr.append(pBdr)
    return p


def _set_all_margins(doc, top=2.5, bottom=2.5, left=2.5, right=2.5):
    """Ajusta los márgenes del documento."""
    for section in doc.sections:
        section.top_margin    = Cm(top)
        section.bottom_margin = Cm(bottom)
        section.left_margin   = Cm(left)
        section.right_margin  = Cm(right)


# ─── ENCABEZADO Y PIE DE PÁGINA ──────────────────────────────────────────────

def _build_header(doc):
    """
    Encabezado: logo a la izquierda | nombre del sistema a la derecha.
    """
    for section in doc.sections:
        header = section.header
        header.is_linked_to_previous = False

        tbl = header.add_table(1, 2, width=Cm(16))
        tbl.alignment = WD_TABLE_ALIGNMENT.CENTER

        # Ocultar bordes
        tbl_xml = tbl._tbl
        tblPr = OxmlElement("w:tblPr")
        tbl_borders = OxmlElement("w:tblBorders")
        for side in ("top", "left", "bottom", "right", "insideH", "insideV"):
            b = OxmlElement(f"w:{side}")
            b.set(qn("w:val"), "none")
            b.set(qn("w:sz"), "0")
            b.set(qn("w:space"), "0")
            b.set(qn("w:color"), "auto")
            tbl_borders.append(b)
        tblPr.append(tbl_borders)
        tbl_xml.insert(0, tblPr)

        cell_logo = tbl.cell(0, 0)
        cell_logo.width = Cm(8)
        cell_logo.vertical_alignment = WD_ALIGN_VERTICAL.CENTER
        p_logo = cell_logo.paragraphs[0]
        p_logo.alignment = WD_ALIGN_PARAGRAPH.LEFT
        run_logo = p_logo.add_run()
        if os.path.exists(LOGO_PATH):
            run_logo.add_picture(LOGO_PATH, width=Cm(LOGO_WIDTH_CM))

        cell_text = tbl.cell(0, 1)
        cell_text.width = Cm(8)
        cell_text.vertical_alignment = WD_ALIGN_VERTICAL.CENTER
        p_text = cell_text.paragraphs[0]
        p_text.alignment = WD_ALIGN_PARAGRAPH.RIGHT
        run_text = p_text.add_run(SISTEMA)
        run_text.font.name  = "Noto Sans"
        run_text.font.size  = Pt(8)
        run_text.font.bold  = True
        run_text.font.color.rgb = _hex_to_rgb(COLOR_VERDE_OSCURO)

        p_sep = header.add_paragraph()
        p_sep.paragraph_format.space_before = Pt(2)
        p_sep.paragraph_format.space_after  = Pt(0)
        pPr = p_sep._p.get_or_add_pPr()
        pBdr = OxmlElement("w:pBdr")
        bottom = OxmlElement("w:bottom")
        bottom.set(qn("w:val"),   "single")
        bottom.set(qn("w:sz"),    "6")
        bottom.set(qn("w:space"), "1")
        bottom.set(qn("w:color"), COLOR_VERDE_OSCURO.lstrip("#"))
        pBdr.append(bottom)
        pPr.append(pBdr)


def _build_footer(doc):
    """Pie de página: institución | confidencial | número de página."""
    for section in doc.sections:
        footer = section.footer
        footer.is_linked_to_previous = False
        p = footer.paragraphs[0] if footer.paragraphs else footer.add_paragraph()
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p.clear()

        pPr = p._p.get_or_add_pPr()
        pBdr = OxmlElement("w:pBdr")
        top = OxmlElement("w:top")
        top.set(qn("w:val"),   "single")
        top.set(qn("w:sz"),    "4")
        top.set(qn("w:space"), "1")
        top.set(qn("w:color"), COLOR_DORADO.lstrip("#"))
        pBdr.append(top)
        pPr.append(pBdr)

        run = p.add_run(f"{INSTITUCION}  |  {SISTEMA}  |  {VERSION}  |  Página ")
        run.font.size = Pt(7)
        run.font.color.rgb = _hex_to_rgb(COLOR_GRIS_TEXTO)
        run.font.name = "Noto Sans"

        fld = OxmlElement("w:fldChar")
        fld.set(qn("w:fldCharType"), "begin")
        r = OxmlElement("w:r")
        r.append(fld)
        p._p.append(r)

        instr = OxmlElement("w:instrText")
        instr.set(qn("xml:space"), "preserve")
        instr.text = " PAGE "
        r2 = OxmlElement("w:r")
        r2.append(instr)
        p._p.append(r2)

        fld2 = OxmlElement("w:fldChar")
        fld2.set(qn("w:fldCharType"), "end")
        r3 = OxmlElement("w:r")
        r3.append(fld2)
        p._p.append(r3)


# ─── PORTADA ─────────────────────────────────────────────────────────────────

def _build_cover(doc):
    """Página de portada con diseño institucional elegante (Página 1)."""
    p_logo = doc.add_paragraph()
    p_logo.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_logo.paragraph_format.space_before = Cm(1.5)
    p_logo.paragraph_format.space_after  = Cm(0.8)
    if os.path.exists(LOGO_PATH):
        run = p_logo.add_run()
        run.add_picture(LOGO_PATH, width=Cm(9.5))

    _add_horizontal_rule(doc, COLOR_DORADO, 20)

    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Cm(1)
    p.paragraph_format.space_after  = Pt(6)
    run = p.add_run(SISTEMA)
    run.font.name  = "Noto Sans"
    run.font.size  = Pt(26)
    run.font.bold  = True
    run.font.color.rgb = _hex_to_rgb(COLOR_VERDE_OSCURO)

    p2 = doc.add_paragraph()
    p2.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p2.paragraph_format.space_after = Pt(4)
    run2 = p2.add_run(f"({SIGLAS})")
    run2.font.name  = "Noto Sans"
    run2.font.size  = Pt(16)
    run2.font.bold  = False
    run2.font.color.rgb = _hex_to_rgb(COLOR_VERDE_MEDIO)

    p3 = doc.add_paragraph()
    p3.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p3.paragraph_format.space_after = Cm(1.8)
    run3 = p3.add_run("Manual de Usuario")
    run3.font.name  = "Noto Sans"
    run3.font.size  = Pt(20)
    run3.font.bold  = True
    run3.font.color.rgb = _hex_to_rgb(COLOR_DORADO)

    # Tabla con metadatos estructurada y limpia
    tbl = doc.add_table(4, 2)
    tbl.alignment = WD_TABLE_ALIGNMENT.CENTER
    tbl.style = "Table Grid"
    datos = [
        ("Institución",        INSTITUCION),
        ("Perfil de usuario",  ROL_MANUAL),
        ("Versión",            VERSION),
        ("Fecha de emisión",   date.today().strftime("%d de %B de %Y")),
    ]
    for i, (label, value) in enumerate(datos):
        c_label = tbl.cell(i, 0)
        c_value = tbl.cell(i, 1)
        c_label.width = Cm(5.5)
        c_value.width = Cm(10.5)
        _set_cell_bg(c_label, COLOR_VERDE_OSCURO)
        _set_cell_bg(c_value, COLOR_GRIS_SUAVE)
        _set_cell_margins(c_label, top=120, bottom=120, left=180, right=180)
        _set_cell_margins(c_value, top=120, bottom=120, left=180, right=180)

        p_l = c_label.paragraphs[0]
        p_l.alignment = WD_ALIGN_PARAGRAPH.LEFT
        r_label = p_l.add_run(label)
        r_label.font.name  = "Noto Sans"
        r_label.font.size  = Pt(11)
        r_label.font.bold  = True
        r_label.font.color.rgb = _hex_to_rgb(COLOR_BLANCO)

        p_v = c_value.paragraphs[0]
        p_v.alignment = WD_ALIGN_PARAGRAPH.LEFT
        r_value = p_v.add_run(value)
        r_value.font.name  = "Noto Sans"
        r_value.font.size  = Pt(11)
        r_value.font.color.rgb = _hex_to_rgb(COLOR_GRIS_TEXTO)

    doc.add_page_break()


# ─── CAPÍTULO: INTRODUCCIÓN ──────────────────────────────────────────────────

def _build_intro(doc):
    _chapter_heading(doc, "1. Introducción", 1)

    _section_heading(doc, "1.1 Descripción del Sistema", 2)
    _body_text(doc, INTRO_SISTEMA)

    _section_heading(doc, "1.2 Objetivo de este Manual", 2)
    _body_text(doc, INTRO_OBJETIVO)

    _section_heading(doc, "1.3 Alcance", 2)
    _body_text(doc, INTRO_ALCANCE)

    _section_heading(doc, "1.4 Navegadores Web Compatibles", 2)
    _body_text(doc, REQUISITOS_NAVEGADOR)

    _section_heading(doc, "1.5 Glosario de Términos", 2)
    tbl = doc.add_table(len(GLOSARIO) + 1, 2)
    tbl.style = "Table Grid"
    tbl.alignment = WD_TABLE_ALIGNMENT.LEFT

    # Encabezado
    _set_cell_bg(tbl.cell(0, 0), COLOR_VERDE_OSCURO)
    _set_cell_bg(tbl.cell(0, 1), COLOR_VERDE_OSCURO)
    for col, txt in enumerate(["Término", "Definición"]):
        cell = tbl.cell(0, col)
        _set_cell_margins(cell, top=100, bottom=100, left=150, right=150)
        p_c = cell.paragraphs[0]
        p_c.alignment = WD_ALIGN_PARAGRAPH.CENTER
        r = p_c.add_run(txt)
        r.font.bold = True
        r.font.color.rgb = _hex_to_rgb(COLOR_BLANCO)
        r.font.name = "Noto Sans"
        r.font.size = Pt(10)

    for i, (term, defn) in enumerate(GLOSARIO, start=1):
        bg = COLOR_GRIS_SUAVE if i % 2 == 0 else COLOR_BLANCO
        c0 = tbl.cell(i, 0)
        c1 = tbl.cell(i, 1)
        c0.width = Cm(4.5)
        c1.width = Cm(11.5)
        _set_cell_bg(c0, bg)
        _set_cell_bg(c1, bg)
        _set_cell_margins(c0, top=90, bottom=90, left=120, right=120)
        _set_cell_margins(c1, top=90, bottom=90, left=120, right=120)

        p0 = c0.paragraphs[0]
        p0.alignment = WD_ALIGN_PARAGRAPH.LEFT
        r0 = p0.add_run(term)
        r0.font.bold = True
        r0.font.name = "Noto Sans"
        r0.font.size = Pt(10)

        p1 = c1.paragraphs[0]
        p1.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
        r1 = p1.add_run(defn)
        r1.font.name = "Noto Sans"
        r1.font.size = Pt(10)

    doc.add_paragraph()


# ─── CAPÍTULOS DE MÓDULOS ────────────────────────────────────────────────────

def _build_module_chapter(doc, chapter_num: int, slug: str):
    """Genera un capítulo completo para un módulo."""
    content = SCREENS_CONTENT.get(slug)
    if not content:
        return

    _chapter_heading(doc, f"{chapter_num}. {content['titulo']}", 1)

    # Captura de pantalla
    img_path = os.path.join(SCREENSHOTS_DIR, f"{slug}.png")
    if os.path.exists(img_path) and os.path.getsize(img_path) > 10000:
        p_img = doc.add_paragraph()
        p_img.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p_img.paragraph_format.space_before = Pt(4)
        p_img.paragraph_format.space_after  = Pt(4)
        run_img = p_img.add_run()
        run_img.add_picture(img_path, width=Cm(15.5))
        p_caption = doc.add_paragraph(f"Figura {chapter_num - 1}. {content['titulo']}")
        p_caption.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p_caption.paragraph_format.space_after = Pt(8)
        run_cap = p_caption.runs[0]
        run_cap.font.size   = Pt(9)
        run_cap.font.italic = True
        run_cap.font.color.rgb = _hex_to_rgb(COLOR_GRIS_TEXTO)
        run_cap.font.name   = "Noto Sans"
    else:
        p_img = doc.add_paragraph(f"[Captura de pantalla: {slug}]")
        p_img.alignment = WD_ALIGN_PARAGRAPH.CENTER

    _add_horizontal_rule(doc, COLOR_DORADO_CLARO, 6)

    _section_heading(doc, f"{chapter_num}.1 Descripción", 2)
    _body_text(doc, content["descripcion"])

    _section_heading(doc, f"{chapter_num}.2 Elementos de la Pantalla", 2)
    elementos = content.get("elementos", [])
    if elementos:
        tbl = doc.add_table(len(elementos) + 1, 2)
        tbl.style = "Table Grid"
        tbl.alignment = WD_TABLE_ALIGNMENT.LEFT
        _set_cell_bg(tbl.cell(0, 0), COLOR_VERDE_MEDIO)
        _set_cell_bg(tbl.cell(0, 1), COLOR_VERDE_MEDIO)
        for col, txt in enumerate(["Elemento", "Descripción"]):
            cell = tbl.cell(0, col)
            _set_cell_margins(cell, top=100, bottom=100, left=150, right=150)
            p_c = cell.paragraphs[0]
            p_c.alignment = WD_ALIGN_PARAGRAPH.CENTER
            r = p_c.add_run(txt)
            r.font.bold = True
            r.font.color.rgb = _hex_to_rgb(COLOR_BLANCO)
            r.font.name = "Noto Sans"
            r.font.size = Pt(10)

        for i, (elem, desc) in enumerate(elementos, start=1):
            bg = COLOR_GRIS_SUAVE if i % 2 == 0 else COLOR_BLANCO
            c0 = tbl.cell(i, 0)
            c1 = tbl.cell(i, 1)
            c0.width = Cm(4.5)
            c1.width = Cm(11.5)
            _set_cell_bg(c0, bg)
            _set_cell_bg(c1, bg)
            _set_cell_margins(c0, top=90, bottom=90, left=120, right=120)
            _set_cell_margins(c1, top=90, bottom=90, left=120, right=120)

            p0 = c0.paragraphs[0]
            p0.alignment = WD_ALIGN_PARAGRAPH.LEFT
            r0 = p0.add_run(elem)
            r0.font.bold = True
            r0.font.name = "Noto Sans"
            r0.font.size = Pt(10)

            p1 = c1.paragraphs[0]
            p1.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
            r1 = p1.add_run(desc)
            r1.font.name = "Noto Sans"
            r1.font.size = Pt(10)

    doc.add_paragraph()

    _section_heading(doc, f"{chapter_num}.3 Pasos de Operación", 2)
    for i, paso in enumerate(content.get("pasos", []), start=1):
        clean_paso = " ".join(line.strip() for line in paso.splitlines() if line.strip())
        p = doc.add_paragraph(style="List Number")
        p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
        p.paragraph_format.space_after = Pt(4)
        p.paragraph_format.line_spacing = 1.15
        run = p.add_run(clean_paso)
        run.font.name = "Noto Sans"
        run.font.size = Pt(11)
        run.font.color.rgb = _hex_to_rgb(COLOR_GRIS_TEXTO)

    nota = content.get("nota")
    if nota:
        clean_nota = " ".join(line.strip() for line in nota.splitlines() if line.strip())
        doc.add_paragraph()
        p_nota = doc.add_paragraph()
        p_nota.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
        p_nota.paragraph_format.left_indent = Cm(1)
        p_nota.paragraph_format.right_indent = Cm(1)
        p_nota.paragraph_format.line_spacing = 1.15
        pPr = p_nota._p.get_or_add_pPr()
        pBdr = OxmlElement("w:pBdr")
        left = OxmlElement("w:left")
        left.set(qn("w:val"),   "single")
        left.set(qn("w:sz"),    "12")
        left.set(qn("w:space"), "4")
        left.set(qn("w:color"), COLOR_DORADO.lstrip("#"))
        pBdr.append(left)
        pPr.append(pBdr)
        run_n = p_nota.add_run("Nota: ")
        run_n.font.bold  = True
        run_n.font.name  = "Noto Sans"
        run_n.font.size  = Pt(11)
        run_n.font.color.rgb = _hex_to_rgb(COLOR_DORADO)
        run_t = p_nota.add_run(clean_nota)
        run_t.font.name  = "Noto Sans"
        run_t.font.size  = Pt(11)
        run_t.font.color.rgb = _hex_to_rgb(COLOR_GRIS_TEXTO)

    doc.add_page_break()


# ─── CAPÍTULO: FAQ ───────────────────────────────────────────────────────────

def _build_faq(doc, chapter_num: int):
    _chapter_heading(doc, f"{chapter_num}. Preguntas Frecuentes", 1)
    for i, (pregunta, respuesta) in enumerate(FAQ, start=1):
        clean_resp = " ".join(line.strip() for line in respuesta.splitlines() if line.strip())
        p_q = doc.add_paragraph()
        r_q = p_q.add_run(f"P{i}. {pregunta}")
        r_q.font.bold  = True
        r_q.font.name  = "Noto Sans"
        r_q.font.size  = Pt(11)
        r_q.font.color.rgb = _hex_to_rgb(COLOR_VERDE_OSCURO)
        p_r = doc.add_paragraph()
        p_r.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
        p_r.paragraph_format.left_indent = Cm(0.5)
        p_r.paragraph_format.space_after = Pt(10)
        p_r.paragraph_format.line_spacing = 1.15
        r_r = p_r.add_run(f"R: {clean_resp}")
        r_r.font.name  = "Noto Sans"
        r_r.font.size  = Pt(11)
        r_r.font.color.rgb = _hex_to_rgb(COLOR_GRIS_TEXTO)
    doc.add_page_break()


# ─── CAPÍTULO: CONTACTO ──────────────────────────────────────────────────────

def _build_contact(doc, chapter_num: int):
    _chapter_heading(doc, f"{chapter_num}. Contacto y Soporte", 1)
    _body_text(doc, (
        "Para reportar incidencias, solicitar accesos o resolver dudas sobre el "
        "funcionamiento del sistema, comuníquese con el área de soporte técnico:"
    ))
    tbl = doc.add_table(3, 2)
    tbl.style = "Table Grid"
    tbl.alignment = WD_TABLE_ALIGNMENT.LEFT
    datos = [
        ("Área de soporte", CONTACTO["area"]),
        ("Correo electrónico", CONTACTO["correo"]),
        ("Sitio del sistema",  CONTACTO["sistema"]),
    ]
    for i, (lbl, val) in enumerate(datos):
        c0 = tbl.cell(i, 0)
        c1 = tbl.cell(i, 1)
        c0.width = Cm(5.0)
        c1.width = Cm(11.0)
        _set_cell_bg(c0, COLOR_VERDE_OSCURO)
        _set_cell_bg(c1, COLOR_GRIS_SUAVE)
        _set_cell_margins(c0, top=100, bottom=100, left=150, right=150)
        _set_cell_margins(c1, top=100, bottom=100, left=150, right=150)

        p0 = c0.paragraphs[0]
        p0.alignment = WD_ALIGN_PARAGRAPH.LEFT
        r0 = p0.add_run(lbl)
        r0.font.bold = True
        r0.font.color.rgb = _hex_to_rgb(COLOR_BLANCO)
        r0.font.name = "Noto Sans"
        r0.font.size = Pt(10)

        p1 = c1.paragraphs[0]
        p1.alignment = WD_ALIGN_PARAGRAPH.LEFT
        r1 = p1.add_run(val)
        r1.font.name = "Noto Sans"
        r1.font.size = Pt(10)
        r1.font.color.rgb = _hex_to_rgb(COLOR_GRIS_TEXTO)


# ─── ANEXO: MÓDULOS DE ADMINISTRACIÓN Y SUPERVISIÓN ──────────────────────────

def _build_module_subentry(doc, slug: str, prefix: str):
    """Genera una subsección de módulo dentro de un anexo."""
    content = SCREENS_CONTENT.get(slug)
    if not content:
        return

    _section_heading(doc, f"Anexo {prefix}: {content['titulo'].replace('Anexo A.1: ', '').replace('Anexo A.2: ', '')}", 2)

    img_path = os.path.join(SCREENSHOTS_DIR, f"{slug}.png")
    if os.path.exists(img_path) and os.path.getsize(img_path) > 10000:
        p_img = doc.add_paragraph()
        p_img.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p_img.paragraph_format.space_before = Pt(4)
        p_img.paragraph_format.space_after  = Pt(4)
        run_img = p_img.add_run()
        run_img.add_picture(img_path, width=Cm(15.5))
        p_caption = doc.add_paragraph(f"Figura {prefix}. {content['titulo']}")
        p_caption.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p_caption.paragraph_format.space_after = Pt(8)
        run_cap = p_caption.runs[0]
        run_cap.font.size   = Pt(9)
        run_cap.font.italic = True
        run_cap.font.color.rgb = _hex_to_rgb(COLOR_GRIS_TEXTO)
        run_cap.font.name   = "Noto Sans"

    _add_horizontal_rule(doc, COLOR_DORADO_CLARO, 6)

    _section_heading(doc, f"Anexo {prefix}.1 Descripción", 3)
    _body_text(doc, content["descripcion"])

    _section_heading(doc, f"Anexo {prefix}.2 Elementos de la Pantalla", 3)
    elementos = content.get("elementos", [])
    if elementos:
        tbl = doc.add_table(len(elementos) + 1, 2)
        tbl.style = "Table Grid"
        tbl.alignment = WD_TABLE_ALIGNMENT.LEFT
        _set_cell_bg(tbl.cell(0, 0), COLOR_VERDE_MEDIO)
        _set_cell_bg(tbl.cell(0, 1), COLOR_VERDE_MEDIO)
        for col, txt in enumerate(["Elemento", "Descripción"]):
            cell = tbl.cell(0, col)
            _set_cell_margins(cell, top=100, bottom=100, left=150, right=150)
            p_c = cell.paragraphs[0]
            p_c.alignment = WD_ALIGN_PARAGRAPH.CENTER
            r = p_c.add_run(txt)
            r.font.bold = True
            r.font.color.rgb = _hex_to_rgb(COLOR_BLANCO)
            r.font.name = "Noto Sans"
            r.font.size = Pt(10)

        for i, (elem, desc) in enumerate(elementos, start=1):
            bg = COLOR_GRIS_SUAVE if i % 2 == 0 else COLOR_BLANCO
            c0 = tbl.cell(i, 0)
            c1 = tbl.cell(i, 1)
            c0.width = Cm(4.5)
            c1.width = Cm(11.5)
            _set_cell_bg(c0, bg)
            _set_cell_bg(c1, bg)
            _set_cell_margins(c0, top=90, bottom=90, left=120, right=120)
            _set_cell_margins(c1, top=90, bottom=90, left=120, right=120)

            p0 = c0.paragraphs[0]
            p0.alignment = WD_ALIGN_PARAGRAPH.LEFT
            r0 = p0.add_run(elem)
            r0.font.bold = True
            r0.font.name = "Noto Sans"
            r0.font.size = Pt(10)

            p1 = c1.paragraphs[0]
            p1.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
            r1 = p1.add_run(desc)
            r1.font.name = "Noto Sans"
            r1.font.size = Pt(10)

    doc.add_paragraph()

    _section_heading(doc, f"Anexo {prefix}.3 Pasos de Operación", 3)
    for i, paso in enumerate(content.get("pasos", []), start=1):
        clean_paso = " ".join(line.strip() for line in paso.splitlines() if line.strip())
        p = doc.add_paragraph(style="List Number")
        p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
        p.paragraph_format.space_after = Pt(4)
        p.paragraph_format.line_spacing = 1.15
        run = p.add_run(clean_paso)
        run.font.name = "Noto Sans"
        run.font.size = Pt(11)
        run.font.color.rgb = _hex_to_rgb(COLOR_GRIS_TEXTO)

    nota = content.get("nota")
    if nota:
        clean_nota = " ".join(line.strip() for line in nota.splitlines() if line.strip())
        doc.add_paragraph()
        p_nota = doc.add_paragraph()
        p_nota.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
        p_nota.paragraph_format.left_indent = Cm(1)
        p_nota.paragraph_format.right_indent = Cm(1)
        p_nota.paragraph_format.line_spacing = 1.15
        pPr = p_nota._p.get_or_add_pPr()
        pBdr = OxmlElement("w:pBdr")
        left = OxmlElement("w:left")
        left.set(qn("w:val"),   "single")
        left.set(qn("w:sz"),    "12")
        left.set(qn("w:space"), "4")
        left.set(qn("w:color"), COLOR_DORADO.lstrip("#"))
        pBdr.append(left)
        pPr.append(pBdr)
        run_n = p_nota.add_run("Nota: ")
        run_n.font.bold  = True
        run_n.font.name  = "Noto Sans"
        run_n.font.size  = Pt(11)
        run_n.font.color.rgb = _hex_to_rgb(COLOR_DORADO)
        run_t = p_nota.add_run(clean_nota)
        run_t.font.name  = "Noto Sans"
        run_t.font.size  = Pt(11)
        run_t.font.color.rgb = _hex_to_rgb(COLOR_GRIS_TEXTO)

    doc.add_page_break()


def _build_annex(doc):
    doc.add_page_break()
    _chapter_heading(doc, "Anexo A: Módulos de Administración y Supervisión (Jefatura, Gerencia y Administración)", 1)
    _body_text(doc, (
        "El presente anexo documenta de forma complementaria los módulos y herramientas del SIAJ "
        "reservados para los niveles de supervisión, gerencia y administración (Jefes de Departamento, "
        "Gerentes, Subdirectores, Directores y Administradores del Sistema)."
    ))
    _add_horizontal_rule(doc, COLOR_DORADO, 12)

    # Anexo A.1: Gestión de Usuarios
    _build_module_subentry(doc, "10_usuarios", "A.1")

    # Anexo A.2: Configuración Gerencial de Semáforos
    _build_module_subentry(doc, "11_config_semaforos", "A.2")


# ─── ESTILOS DE TEXTO ────────────────────────────────────────────────────────

def _chapter_heading(doc, text: str, level: int):
    p = doc.add_heading(text, level=level)
    p.paragraph_format.space_before = Pt(16)
    p.paragraph_format.space_after  = Pt(8)
    for run in p.runs:
        run.font.name  = "Noto Sans"
        run.font.color.rgb = _hex_to_rgb(COLOR_VERDE_OSCURO)
        if level == 1:
            run.font.size = Pt(18)
    return p


def _section_heading(doc, text: str, level: int):
    p = doc.add_heading(text, level=level)
    p.paragraph_format.space_before = Pt(10)
    p.paragraph_format.space_after  = Pt(4)
    for run in p.runs:
        run.font.name  = "Noto Sans"
        run.font.color.rgb = _hex_to_rgb(COLOR_VERDE_MEDIO)
        run.font.size  = Pt(13)
    return p


def _body_text(doc, text: str):
    if not text:
        return None
    blocks = [b.strip() for b in text.split("\n\n") if b.strip()]
    last_p = None
    for block in blocks:
        lines = [l.strip() for l in block.splitlines() if l.strip()]
        if any(l.startswith("•") or l.startswith("-") for l in lines):
            for l in lines:
                if l.startswith("•") or l.startswith("-"):
                    p = doc.add_paragraph()
                    p.alignment = WD_ALIGN_PARAGRAPH.LEFT
                    p.paragraph_format.left_indent = Cm(0.75)
                    p.paragraph_format.space_after = Pt(3)
                    p.paragraph_format.line_spacing = 1.15
                    run = p.add_run(l)
                else:
                    p = doc.add_paragraph()
                    p.alignment = WD_ALIGN_PARAGRAPH.LEFT
                    p.paragraph_format.space_after = Pt(4)
                    p.paragraph_format.line_spacing = 1.15
                    run = p.add_run(l)
                run.font.name = "Noto Sans"
                run.font.size = Pt(11)
                run.font.color.rgb = _hex_to_rgb(COLOR_GRIS_TEXTO)
                last_p = p
        else:
            clean_text = " ".join(lines)
            p = doc.add_paragraph()
            p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
            p.paragraph_format.space_after = Pt(8)
            p.paragraph_format.line_spacing = 1.15
            run = p.add_run(clean_text)
            run.font.name = "Noto Sans"
            run.font.size = Pt(11)
            run.font.color.rgb = _hex_to_rgb(COLOR_GRIS_TEXTO)
            last_p = p
    return last_p


# ─── FUNCIÓN PRINCIPAL ───────────────────────────────────────────────────────

def build_document(captured_screens: list):
    """Construye el documento Word completo."""
    doc = Document()
    _set_all_margins(doc, top=2.5, bottom=2.5, left=2.5, right=2.5)

    doc.styles["Normal"].font.name = "Noto Sans"
    doc.styles["Normal"].font.size = Pt(11)

    _build_header(doc)
    _build_footer(doc)

    doc.sections[0].different_first_page_header_footer = True
    _build_cover(doc)
    _build_intro(doc)

    chapter = 2
    regular_slugs = [
        "00_login", "01_dashboard", "02_expedientes", "03_audiencias",
        "04_terminos", "05_trazabilidad", "06_documentos", "07_catalogos",
        "08_perfil", "09_tareas"
    ]
    for slug in regular_slugs:
        _build_module_chapter(doc, chapter, slug)
        chapter += 1

    _build_faq(doc, chapter)
    chapter += 1

    _build_contact(doc, chapter)
    _build_annex(doc)

    out_path = os.path.abspath(OUTPUT_PATH)
    try:
        doc.save(out_path)
    except PermissionError:
        out_path = os.path.abspath(OUTPUT_PATH.replace(".docx", "_nuevo.docx"))
        doc.save(out_path)
    print(f"\n[OK] Documento generado:\n   {out_path}")
    return out_path


if __name__ == "__main__":
    from config import SCREENS
    slugs = [s[0] for s in SCREENS]
    build_document(slugs)
