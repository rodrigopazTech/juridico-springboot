"""
build_institutional_manual.py — Generador del Manual Institucional SIAJ.
Aplica la plantilla institucional de Financiera para el Bienestar
con las 6 secciones normativas y el encabezado/pie de página reglamentario.
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
    LOGO_PATH, SCREENSHOTS_DIR,
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

OUTPUT_INSTITUCIONAL = r"C:\Users\Rodrigo.DESKTOP-2MLHEH3\Desktop\Post\Manual_Usuario_SIAJ_v1.0 - Correcciones.docx"


def _hex_to_rgb(hex_color: str):
    h = hex_color.lstrip("#")
    return RGBColor(int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16))


def _set_cell_bg(cell, hex_color: str):
    tcPr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement("w:shd")
    shd.set(qn("w:val"), "clear")
    shd.set(qn("w:color"), "auto")
    shd.set(qn("w:fill"), hex_color.lstrip("#"))
    tcPr.append(shd)


def _set_cell_margins(cell, top=100, bottom=100, left=150, right=150):
    tcPr = cell._tc.get_or_add_tcPr()
    tcMar = OxmlElement("w:tcMar")
    for m, val in [("top", top), ("bottom", bottom), ("left", left), ("right", right)]:
        node = OxmlElement(f"w:{m}")
        node.set(qn("w:w"), str(val))
        node.set(qn("w:type"), "dxa")
        tcMar.append(node)
    tcPr.append(tcMar)


def _set_table_borders(table, border_color="CCCCCC"):
    tblPr = table._tbl.tblPr
    tblBorders = OxmlElement("w:tblBorders")
    for border_name in ["top", "left", "bottom", "right", "insideH", "insideV"]:
        border = OxmlElement(f"w:{border_name}")
        border.set(qn("w:val"), "single")
        border.set(qn("w:sz"), "4")
        border.set(qn("w:space"), "0")
        border.set(qn("w:color"), border_color)
        tblBorders.append(border)
    tblPr.append(tblBorders)


def _set_all_margins(doc, top=2.0, bottom=2.0, left=2.0, right=2.0):
    for section in doc.sections:
        section.top_margin    = Cm(top)
        section.bottom_margin = Cm(bottom)
        section.left_margin   = Cm(left)
        section.right_margin  = Cm(right)


def _build_institutional_header(doc):
    """Construye la tabla de encabezado institucional oficial."""
    for section in doc.sections:
        header = section.header
        header.is_linked_to_previous = False
        
        # Limpiar párrafos por defecto
        for p in header.paragraphs:
            p.text = ""

        # Tabla principal del encabezado (1 fila, 3 columnas)
        tbl = header.add_table(1, 3, width=Cm(17.5))
        tbl.alignment = WD_TABLE_ALIGNMENT.CENTER
        _set_table_borders(tbl, "CCCCCC")

        # Columnas: Logo (4.0cm) | Textos Institucionales (9.5cm) | Metadatos (4.0cm)
        cell_logo = tbl.cell(0, 0)
        cell_text = tbl.cell(0, 1)
        cell_meta = tbl.cell(0, 2)

        cell_logo.width = Cm(4.0)
        cell_text.width = Cm(9.5)
        cell_meta.width = Cm(4.0)

        cell_logo.vertical_alignment = WD_ALIGN_VERTICAL.CENTER
        cell_text.vertical_alignment = WD_ALIGN_VERTICAL.CENTER
        cell_meta.vertical_alignment = WD_ALIGN_VERTICAL.CENTER

        _set_cell_margins(cell_logo, top=60, bottom=60, left=100, right=100)
        _set_cell_margins(cell_text, top=60, bottom=60, left=100, right=100)
        _set_cell_margins(cell_meta, top=40, bottom=40, left=60, right=60)

        # 1. Logo
        p_logo = cell_logo.paragraphs[0]
        p_logo.alignment = WD_ALIGN_PARAGRAPH.CENTER
        if os.path.exists(LOGO_PATH):
            p_logo.add_run().add_picture(LOGO_PATH, width=Cm(3.5))

        # 2. Textos Institucionales
        p_t = cell_text.paragraphs[0]
        p_t.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p_t.paragraph_format.space_after = Pt(0)
        p_t.paragraph_format.line_spacing = 1.05

        r = p_t.add_run("Financiera para el Bienestar\n")
        r.font.name = "Arial"
        r.font.size = Pt(8.5)
        r.font.bold = True
        r.font.color.rgb = _hex_to_rgb(COLOR_VERDE_OSCURO)

        r2 = p_t.add_run("Dirección de Administración y Finanzas\nSubdirección de Tecnologías de la Información y Comunicaciones\nGerencia de Desarrollo de Sistemas Administrativos\n")
        r2.font.name = "Arial"
        r2.font.size = Pt(7.5)
        r2.font.color.rgb = _hex_to_rgb("555555")

        r3 = p_t.add_run("Manual de Usuario de Sistema Integral de Asuntos Jurídicos")
        r3.font.name = "Arial"
        r3.font.size = Pt(8)
        r3.font.bold = True
        r3.font.color.rgb = _hex_to_rgb(COLOR_VERDE_MEDIO)

        # 3. Metadatos (Subtabla 3x2)
        p_m = cell_meta.paragraphs[0]
        p_m.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p_m.paragraph_format.space_after = Pt(0)

        # Tabla de metadatos interior
        sub_tbl = cell_meta.add_table(3, 2)
        sub_tbl.alignment = WD_TABLE_ALIGNMENT.CENTER
        _set_table_borders(sub_tbl, "CCCCCC")

        meta_rows = [
            ("HOJA", "Página "),
            ("FECHA", "Septiembre 2026"),
            ("ID", "MGDSA/001/STIC/2026")
        ]
        for idx, (k, v) in enumerate(meta_rows):
            c_k = sub_tbl.cell(idx, 0)
            c_v = sub_tbl.cell(idx, 1)
            c_k.width = Cm(1.3)
            c_v.width = Cm(2.7)
            _set_cell_bg(c_k, COLOR_GRIS_SUAVE)
            _set_cell_margins(c_k, top=20, bottom=20, left=40, right=40)
            _set_cell_margins(c_v, top=20, bottom=20, left=40, right=40)

            p_k = c_k.paragraphs[0]
            p_k.alignment = WD_ALIGN_PARAGRAPH.CENTER
            r_k = p_k.add_run(k)
            r_k.font.name = "Arial"
            r_k.font.size = Pt(6.5)
            r_k.font.bold = True

            p_v = c_v.paragraphs[0]
            p_v.alignment = WD_ALIGN_PARAGRAPH.CENTER
            if k == "HOJA":
                r_v = p_v.add_run("Página ")
                r_v.font.name = "Arial"
                r_v.font.size = Pt(6.5)
                r_v.font.color.rgb = _hex_to_rgb("008C45")

                r_f1 = OxmlElement("w:r")
                fld1 = OxmlElement("w:fldChar")
                fld1.set(qn("w:fldCharType"), "begin")
                r_f1.append(fld1)
                p_v._p.append(r_f1)

                r_in = OxmlElement("w:r")
                instr = OxmlElement("w:instrText")
                instr.set(qn("xml:space"), "preserve")
                instr.text = " PAGE "
                r_in.append(instr)
                p_v._p.append(r_in)

                r_f2 = OxmlElement("w:r")
                fld2 = OxmlElement("w:fldChar")
                fld2.set(qn("w:fldCharType"), "end")
                r_f2.append(fld2)
                p_v._p.append(r_f2)
            else:
                r_v = p_v.add_run(v)
                r_v.font.name = "Arial"
                r_v.font.size = Pt(6.5)


def _heading_1(doc, text: str):
    p = doc.add_paragraph()
    p.paragraph_format.space_before = Pt(16)
    p.paragraph_format.space_after  = Pt(8)
    pPr = p._p.get_or_add_pPr()
    # Línea inferior decorativa
    pBdr = OxmlElement("w:pBdr")
    bottom = OxmlElement("w:bottom")
    bottom.set(qn("w:val"),   "single")
    bottom.set(qn("w:sz"),    "12")
    bottom.set(qn("w:space"), "2")
    bottom.set(qn("w:color"), COLOR_VERDE_OSCURO.lstrip("#"))
    pBdr.append(bottom)
    pPr.append(pBdr)

    run = p.add_run(text)
    run.font.name = "Arial"
    run.font.size = Pt(15)
    run.font.bold = True
    run.font.color.rgb = _hex_to_rgb(COLOR_VERDE_OSCURO)
    return p


def _heading_2(doc, text: str):
    p = doc.add_paragraph()
    p.paragraph_format.space_before = Pt(12)
    p.paragraph_format.space_after  = Pt(4)
    run = p.add_run(text)
    run.font.name = "Arial"
    run.font.size = Pt(12)
    run.font.bold = True
    run.font.color.rgb = _hex_to_rgb(COLOR_VERDE_MEDIO)
    return p


def _heading_3(doc, text: str):
    p = doc.add_paragraph()
    p.paragraph_format.space_before = Pt(8)
    p.paragraph_format.space_after  = Pt(3)
    run = p.add_run(text)
    run.font.name = "Arial"
    run.font.size = Pt(11)
    run.font.bold = True
    run.font.color.rgb = _hex_to_rgb(COLOR_DORADO)
    return p


def _body_text(doc, text: str):
    if not text:
        return
    blocks = [b.strip() for b in text.split("\n\n") if b.strip()]
    for block in blocks:
        lines = [l.strip() for l in block.splitlines() if l.strip()]
        if any(l.startswith("•") or l.startswith("-") for l in lines):
            for l in lines:
                p = doc.add_paragraph()
                p.alignment = WD_ALIGN_PARAGRAPH.LEFT
                p.paragraph_format.left_indent = Cm(0.75)
                p.paragraph_format.space_after = Pt(3)
                p.paragraph_format.line_spacing = 1.15
                run = p.add_run(l)
                run.font.name = "Noto Sans"
                run.font.size = Pt(10.5)
                run.font.color.rgb = _hex_to_rgb(COLOR_GRIS_TEXTO)
        else:
            clean_text = " ".join(lines)
            p = doc.add_paragraph()
            p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
            p.paragraph_format.space_after = Pt(8)
            p.paragraph_format.line_spacing = 1.15
            run = p.add_run(clean_text)
            run.font.name = "Noto Sans"
            run.font.size = Pt(10.5)
            run.font.color.rgb = _hex_to_rgb(COLOR_GRIS_TEXTO)


def build_institutional_document():
    doc = Document()
    _set_all_margins(doc, top=2.0, bottom=2.0, left=2.0, right=2.0)

    doc.styles["Normal"].font.name = "Noto Sans"
    doc.styles["Normal"].font.size = Pt(10.5)

    _build_institutional_header(doc)

    # ─── 1. PORTADA Y NOTA PROPÓSITO ──────────────────────────────────────────
    p_date = doc.add_paragraph()
    p_date.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_date.paragraph_format.space_before = Cm(2)
    p_date.paragraph_format.space_after = Cm(1.5)
    r_date = p_date.add_run("SEPTIEMBRE 2026")
    r_date.font.name = "Arial"
    r_date.font.size = Pt(22)
    r_date.font.bold = True
    r_date.font.color.rgb = _hex_to_rgb(COLOR_VERDE_OSCURO)

    # Cuadro con propósito del documento (Limpio y formal)
    tbl_purpose = doc.add_table(1, 1)
    tbl_purpose.alignment = WD_TABLE_ALIGNMENT.CENTER
    _set_table_borders(tbl_purpose, COLOR_VERDE_OSCURO)
    c_p = tbl_purpose.cell(0, 0)
    c_p.width = Cm(15.5)
    _set_cell_bg(c_p, COLOR_GRIS_SUAVE)
    _set_cell_margins(c_p, top=180, bottom=180, left=250, right=250)

    p_p = c_p.paragraphs[0]
    p_p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
    p_p.paragraph_format.line_spacing = 1.15
    r_p = p_p.add_run(
        "Este documento tiene como propósito brindar una explicación detallada y estructurada "
        "sobre el uso del sistema SIAJ. A través de este material, se busca orientar al personal que "
        "cuenta con este tipo de acceso, explicando de manera clara las funcionalidades disponibles, "
        "los alcances y las limitaciones propias del rol."
    )
    r_p.font.name = "Noto Sans"
    r_p.font.size = Pt(10.5)
    r_p.font.color.rgb = _hex_to_rgb(COLOR_GRIS_TEXTO)

    doc.add_page_break()

    # ─── 2. GLOSARIO DE TÉRMINOS, SIGLAS Y ACRÓNIMOS ──────────────────────────
    _heading_1(doc, "2. GLOSARIO DE TÉRMINOS, SIGLAS Y ACRÓNIMOS")
    _body_text(doc, "A continuación, se muestra el glosario de términos, siglas y acrónimos que pueden ser encontrados a lo largo del presente documento.")

    tbl_g = doc.add_table(len(GLOSARIO) + 1, 2)
    tbl_g.alignment = WD_TABLE_ALIGNMENT.CENTER
    _set_table_borders(tbl_g, "CCCCCC")

    _set_cell_bg(tbl_g.cell(0, 0), COLOR_VERDE_OSCURO)
    _set_cell_bg(tbl_g.cell(0, 1), COLOR_VERDE_OSCURO)
    for col, h_txt in enumerate(["Términos / Siglas / Acrónimos", "Significado"]):
        cell = tbl_g.cell(0, col)
        cell.width = Cm(5.0 if col == 0 else 11.5)
        _set_cell_margins(cell, top=100, bottom=100, left=150, right=150)
        p_c = cell.paragraphs[0]
        p_c.alignment = WD_ALIGN_PARAGRAPH.CENTER
        r = p_c.add_run(h_txt)
        r.font.name = "Arial"
        r.font.bold = True
        r.font.size = Pt(10)
        r.font.color.rgb = _hex_to_rgb(COLOR_BLANCO)

    for i, (term, defn) in enumerate(GLOSARIO, start=1):
        bg = COLOR_GRIS_SUAVE if i % 2 == 0 else COLOR_BLANCO
        c0 = tbl_g.cell(i, 0)
        c1 = tbl_g.cell(i, 1)
        c0.width = Cm(5.0)
        c1.width = Cm(11.5)
        _set_cell_bg(c0, bg)
        _set_cell_bg(c1, bg)
        _set_cell_margins(c0, top=80, bottom=80, left=120, right=120)
        _set_cell_margins(c1, top=80, bottom=80, left=120, right=120)

        p0 = c0.paragraphs[0]
        p0.alignment = WD_ALIGN_PARAGRAPH.CENTER
        r0 = p0.add_run(term)
        r0.font.name = "Arial"
        r0.font.bold = True
        r0.font.size = Pt(9.5)

        p1 = c1.paragraphs[0]
        p1.alignment = WD_ALIGN_PARAGRAPH.LEFT
        r1 = p1.add_run(defn)
        r1.font.name = "Noto Sans"
        r1.font.size = Pt(9.5)

    doc.add_paragraph()

    # ─── 3. LENGUAJE INCLUYENTE Y NO DISCRIMINATORIO ─────────────────────────
    _heading_1(doc, "3. LENGUAJE INCLUYENTE Y NO DISCRIMINATORIO")
    _body_text(doc, (
        "El lenguaje servidor público en el documento Manual de Usuario de Sistema Integral de Asuntos Jurídicos "
        "no busca generar ninguna distinción, ni marca diferencias entre hombres y mujeres, por lo que las referencias "
        "o alusiones en la redacción hechas hacia un género representa a ambos sexos."
    ))

    # ─── 4. INTRODUCCIÓN ──────────────────────────────────────────────────────
    _heading_1(doc, "4. INTRODUCCIÓN")
    _body_text(doc, (
        "Este documento proporciona la información esencial para operar y navegar por los diferentes apartados del "
        "Sistema Integral de Asuntos Jurídicos (SIAJ) a los que se puede acceder con los privilegios asignados al perfil del usuario."
    ))

    _heading_2(doc, "4.1 OBJETIVO GENERAL")
    _body_text(doc, (
        "Establecer una guía práctica, clara y estandarizada para la operación del Sistema Integral de Asuntos Jurídicos (SIAJ), "
        "orientando a las personas usuarias en la gestión, seguimiento y control eficiente de expedientes, audiencias, "
        "términos procesales, documentos y trazabilidad institucional."
    ))

    _heading_2(doc, "4.2 OBJETIVOS ESPECÍFICOS")
    _body_text(doc, (
        "• Proporcionar información clara y estructurada destacando las acciones operativas que se pueden llevar a cabo en cada sección del sistema.\n\n"
        "• Facilitar el aprendizaje y la navegación por la interfaz del SIAJ, asegurando el correcto registro de información legal y el seguimiento mediante semáforos de vencimiento.\n\n"
        "• Estandarizar la consulta de trazabilidad, carga de documentos y gestión de tareas para optimizar los procesos jurídicos institucionales."
    ))

    doc.add_page_break()

    # ─── 5. LINEAMIENTOS O DESARROLLO DEL DOCUMENTO ──────────────────────────
    _heading_1(doc, "5. LINEAMIENTOS O DESARROLLO DEL DOCUMENTO")
    _body_text(doc, (
        "En esta sección se detalla el funcionamiento de cada uno de los módulos operativos que integran el "
        "Sistema Integral de Asuntos Jurídicos (SIAJ), incluyendo sus componentes de interfaz visual, "
        "descripción funcional y los pasos detallados para la realización de operaciones."
    ))

    # Módulos del 5.1 al 5.10
    modules = [
        ("00_login",            "5.1 Acceso al Sistema (Login)"),
        ("01_dashboard",        "5.2 Tablero Principal (Dashboard)"),
        ("02_expedientes",      "5.3 Gestión de Expedientes"),
        ("03_audiencias",       "5.4 Gestión de Audiencias"),
        ("04_terminos",         "5.5 Semáforo de Términos Procesales"),
        ("05_trazabilidad",     "5.6 Historial y Trazabilidad"),
        ("06_documentos",       "5.7 Gestión Documental"),
        ("07_catalogos",        "5.8 Catálogos del Sistema"),
        ("08_perfil",           "5.9 Perfil de Usuario"),
        ("09_tareas",           "5.10 Módulo de Tareas y Recordatorios"),
    ]

    for slug, section_title in modules:
        content = SCREENS_CONTENT.get(slug)
        if not content:
            continue

        _heading_2(doc, section_title)

        # Imagen HD del módulo
        img_path = os.path.join(SCREENSHOTS_DIR, f"{slug}.png")
        if os.path.exists(img_path) and os.path.getsize(img_path) > 10000:
            p_img = doc.add_paragraph()
            p_img.alignment = WD_ALIGN_PARAGRAPH.CENTER
            p_img.paragraph_format.space_before = Pt(4)
            p_img.paragraph_format.space_after  = Pt(4)
            run_img = p_img.add_run()
            run_img.add_picture(img_path, width=Cm(15.5))

            p_cap = doc.add_paragraph(f"Figura. {content['titulo']}")
            p_cap.alignment = WD_ALIGN_PARAGRAPH.CENTER
            p_cap.paragraph_format.space_after = Pt(8)
            r_cap = p_cap.runs[0]
            r_cap.font.name = "Arial"
            r_cap.font.size = Pt(8.5)
            r_cap.font.italic = True
            r_cap.font.color.rgb = _hex_to_rgb("555555")

        # Descripción
        _heading_3(doc, "Descripción Funcional")
        _body_text(doc, content["descripcion"])

        # Elementos
        _heading_3(doc, "Elementos de la Pantalla")
        elementos = content.get("elementos", [])
        if elementos:
            tbl_elem = doc.add_table(len(elementos) + 1, 2)
            tbl_elem.alignment = WD_TABLE_ALIGNMENT.CENTER
            _set_table_borders(tbl_elem, "CCCCCC")

            _set_cell_bg(tbl_elem.cell(0, 0), COLOR_VERDE_MEDIO)
            _set_cell_bg(tbl_elem.cell(0, 1), COLOR_VERDE_MEDIO)
            for col, h_txt in enumerate(["Elemento", "Descripción"]):
                cell = tbl_elem.cell(0, col)
                cell.width = Cm(4.5 if col == 0 else 12.0)
                _set_cell_margins(cell, top=80, bottom=80, left=120, right=120)
                p_c = cell.paragraphs[0]
                p_c.alignment = WD_ALIGN_PARAGRAPH.CENTER
                r = p_c.add_run(h_txt)
                r.font.name = "Arial"
                r.font.bold = True
                r.font.size = Pt(9.5)
                r.font.color.rgb = _hex_to_rgb(COLOR_BLANCO)

            for idx_e, (elem, desc) in enumerate(elementos, start=1):
                bg = COLOR_GRIS_SUAVE if idx_e % 2 == 0 else COLOR_BLANCO
                c0 = tbl_elem.cell(idx_e, 0)
                c1 = tbl_elem.cell(idx_e, 1)
                c0.width = Cm(4.5)
                c1.width = Cm(12.0)
                _set_cell_bg(c0, bg)
                _set_cell_bg(c1, bg)
                _set_cell_margins(c0, top=70, bottom=70, left=100, right=100)
                _set_cell_margins(c1, top=70, bottom=70, left=100, right=100)

                p0 = c0.paragraphs[0]
                p0.alignment = WD_ALIGN_PARAGRAPH.LEFT
                r0 = p0.add_run(elem)
                r0.font.name = "Arial"
                r0.font.bold = True
                r0.font.size = Pt(9.5)

                p1 = c1.paragraphs[0]
                p1.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
                r1 = p1.add_run(desc)
                r1.font.name = "Noto Sans"
                r1.font.size = Pt(9.5)

        doc.add_paragraph()

        # Pasos de Operación
        _heading_3(doc, "Pasos de Operación")
        for idx_p, paso in enumerate(content.get("pasos", []), start=1):
            clean_paso = " ".join(line.strip() for line in paso.splitlines() if line.strip())
            p = doc.add_paragraph(style="List Number")
            p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
            p.paragraph_format.space_after = Pt(4)
            p.paragraph_format.line_spacing = 1.15
            run = p.add_run(clean_paso)
            run.font.name = "Noto Sans"
            run.font.size = Pt(10.5)
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
            run_n.font.name  = "Arial"
            run_n.font.size  = Pt(10)
            run_n.font.color.rgb = _hex_to_rgb(COLOR_DORADO)

            run_t = p_nota.add_run(clean_nota)
            run_t.font.name  = "Noto Sans"
            run_t.font.size  = Pt(10)
            run_t.font.color.rgb = _hex_to_rgb(COLOR_GRIS_TEXTO)

        doc.add_paragraph()

    # 5.11 Preguntas Frecuentes
    _heading_2(doc, "5.11 Preguntas Frecuentes (FAQ)")
    for i, (pregunta, respuesta) in enumerate(FAQ, start=1):
        clean_resp = " ".join(line.strip() for line in respuesta.splitlines() if line.strip())
        p_q = doc.add_paragraph()
        r_q = p_q.add_run(f"P{i}. {pregunta}")
        r_q.font.bold  = True
        r_q.font.name  = "Arial"
        r_q.font.size  = Pt(10.5)
        r_q.font.color.rgb = _hex_to_rgb(COLOR_VERDE_OSCURO)

        p_r = doc.add_paragraph()
        p_r.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
        p_r.paragraph_format.left_indent = Cm(0.5)
        p_r.paragraph_format.space_after = Pt(8)
        p_r.paragraph_format.line_spacing = 1.15
        r_r = p_r.add_run(f"R: {clean_resp}")
        r_r.font.name  = "Noto Sans"
        r_r.font.size  = Pt(10.5)
        r_r.font.color.rgb = _hex_to_rgb(COLOR_GRIS_TEXTO)

    # 5.12 Contacto y Soporte
    _heading_2(doc, "5.12 Contacto y Soporte Técnico")
    _body_text(doc, "Para reportar incidencias, solicitar accesos o resolver dudas sobre el funcionamiento del sistema, comuníquese con el área de soporte técnico:")

    tbl_c = doc.add_table(3, 2)
    tbl_c.alignment = WD_TABLE_ALIGNMENT.CENTER
    _set_table_borders(tbl_c, "CCCCCC")
    datos_c = [
        ("Área de soporte", CONTACTO["area"]),
        ("Correo electrónico", CONTACTO["correo"]),
        ("Sitio del sistema",  CONTACTO["sistema"]),
    ]
    for idx_c, (lbl, val) in enumerate(datos_c):
        c0 = tbl_c.cell(idx_c, 0)
        c1 = tbl_c.cell(idx_c, 1)
        c0.width = Cm(5.0)
        c1.width = Cm(11.5)
        _set_cell_bg(c0, COLOR_VERDE_OSCURO)
        _set_cell_bg(c1, COLOR_GRIS_SUAVE)
        _set_cell_margins(c0, top=80, bottom=80, left=120, right=120)
        _set_cell_margins(c1, top=80, bottom=80, left=120, right=120)

        p0 = c0.paragraphs[0]
        p0.alignment = WD_ALIGN_PARAGRAPH.LEFT
        r0 = p0.add_run(lbl)
        r0.font.bold = True
        r0.font.color.rgb = _hex_to_rgb(COLOR_BLANCO)
        r0.font.name = "Arial"
        r0.font.size = Pt(9.5)

        p1 = c1.paragraphs[0]
        p1.alignment = WD_ALIGN_PARAGRAPH.LEFT
        r1 = p1.add_run(val)
        r1.font.name = "Noto Sans"
        r1.font.size = Pt(9.5)
        r1.font.color.rgb = _hex_to_rgb(COLOR_GRIS_TEXTO)

    doc.add_paragraph()

    # 5.13 Anexo A: Módulos de Administración
    _heading_2(doc, "5.13 Anexo A: Módulos de Administración y Supervisión (Jefatura, Gerencia y Administración)")
    _body_text(doc, (
        "El presente anexo documenta de forma complementaria los módulos y herramientas del SIAJ "
        "reservados para los niveles de supervisión, gerencia y administración (Jefes de Departamento, "
        "Gerentes, Subdirectores, Directores y Administradores del Sistema)."
    ))

    annex_modules = [
        ("10_usuarios",         "Anexo A.1: Módulo de Gestión de Usuarios"),
        ("11_config_semaforos", "Anexo A.2: Configuración Gerencial de Semáforos Procesales"),
    ]

    for slug, section_title in annex_modules:
        content = SCREENS_CONTENT.get(slug)
        if not content:
            continue

        _heading_3(doc, section_title)
        img_path = os.path.join(SCREENSHOTS_DIR, f"{slug}.png")
        if os.path.exists(img_path) and os.path.getsize(img_path) > 10000:
            p_img = doc.add_paragraph()
            p_img.alignment = WD_ALIGN_PARAGRAPH.CENTER
            p_img.paragraph_format.space_before = Pt(4)
            p_img.paragraph_format.space_after  = Pt(4)
            run_img = p_img.add_run()
            run_img.add_picture(img_path, width=Cm(15.5))

            p_cap = doc.add_paragraph(f"Figura. {content['titulo']}")
            p_cap.alignment = WD_ALIGN_PARAGRAPH.CENTER
            p_cap.paragraph_format.space_after = Pt(8)
            r_cap = p_cap.runs[0]
            r_cap.font.name = "Arial"
            r_cap.font.size = Pt(8.5)
            r_cap.font.italic = True
            r_cap.font.color.rgb = _hex_to_rgb("555555")

        _body_text(doc, content["descripcion"])

    doc.add_page_break()

    # ─── 6. FIRMAS DE APROBACIÓN ──────────────────────────────────────────────
    _heading_1(doc, "6. FIRMAS DE APROBACIÓN")
    _body_text(doc, "El presente documento ha sido elaborado, revisado y aprobado por las áreas responsables:")

    tbl_sig = doc.add_table(2, 3)
    tbl_sig.alignment = WD_TABLE_ALIGNMENT.CENTER
    _set_table_borders(tbl_sig, "006B53")

    cols_titles = ["Elaboró", "Revisó", "Aprobó"]
    for c_idx, title_text in enumerate(cols_titles):
        cell_h = tbl_sig.cell(0, c_idx)
        cell_h.width = Cm(5.5)
        _set_cell_bg(cell_h, COLOR_VERDE_OSCURO)
        _set_cell_margins(cell_h, top=100, bottom=100, left=100, right=100)
        p_h = cell_h.paragraphs[0]
        p_h.alignment = WD_ALIGN_PARAGRAPH.CENTER
        r_h = p_h.add_run(title_text)
        r_h.font.name = "Arial"
        r_h.font.bold = True
        r_h.font.size = Pt(10)
        r_h.font.color.rgb = _hex_to_rgb(COLOR_BLANCO)

    # Fila de firmas y cargos
    roles_sig = [
        ("Área de Desarrollo de Sistemas", "Subdirección de Tecnologías de la Información"),
        ("Gerencia de Asuntos Jurídicos", "Área Jurídica"),
        ("Dirección de Administración y Finanzas", "Financiera para el Bienestar")
    ]
    for c_idx, (cargo, area) in enumerate(roles_sig):
        cell_b = tbl_sig.cell(1, c_idx)
        cell_b.width = Cm(5.5)
        _set_cell_bg(cell_b, COLOR_BLANCO)
        _set_cell_margins(cell_b, top=400, bottom=150, left=100, right=100)
        p_b = cell_b.paragraphs[0]
        p_b.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p_b.paragraph_format.line_spacing = 1.15

        r_line = p_b.add_run("___________________________\n\n")
        r_line.font.name = "Arial"
        r_line.font.size = Pt(9)
        r_line.font.color.rgb = _hex_to_rgb("888888")

        r_c = p_b.add_run(f"{cargo}\n")
        r_c.font.name = "Arial"
        r_c.font.bold = True
        r_c.font.size = Pt(9)
        r_c.font.color.rgb = _hex_to_rgb(COLOR_VERDE_OSCURO)

        r_a = p_b.add_run(area)
        r_a.font.name = "Arial"
        r_a.font.size = Pt(8.5)
        r_a.font.color.rgb = _hex_to_rgb(COLOR_GRIS_TEXTO)

    # Guardar
    os.makedirs(os.path.dirname(OUTPUT_INSTITUCIONAL), exist_ok=True)
    doc.save(OUTPUT_INSTITUCIONAL)
    print(f"[OK] Manual Institucional creado en:\n  {OUTPUT_INSTITUCIONAL}")


if __name__ == "__main__":
    build_institutional_document()
