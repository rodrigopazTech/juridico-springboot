"""
build_exact_institutional_manual.py — Generador con el formato visual exacto
del documento institucional (Calibri/Arial, líneas en títulos, encabezado oficial).
"""

import os
from docx import Document
from docx.shared import Cm, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_ALIGN_VERTICAL, WD_TABLE_ALIGNMENT
from docx.oxml.ns import qn
from docx.oxml import OxmlElement

from config import LOGO_PATH, SCREENSHOTS_DIR
from content import SCREENS_CONTENT, GLOSARIO, FAQ, CONTACTO

OUTPUT_PATH = r"C:\Users\Rodrigo.DESKTOP-2MLHEH3\Desktop\Post\Manual_Usuario_SIAJ_v1.0 - Correcciones.docx"


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


def _set_table_borders(table, border_color="000000", sz="4"):
    tblPr = table._tbl.tblPr
    tblBorders = OxmlElement("w:tblBorders")
    for border_name in ["top", "left", "bottom", "right", "insideH", "insideV"]:
        border = OxmlElement(f"w:{border_name}")
        border.set(qn("w:val"), "single")
        border.set(qn("w:sz"), sz)
        border.set(qn("w:space"), "0")
        border.set(qn("w:color"), border_color)
        tblBorders.append(border)
    tblPr.append(tblBorders)


def _add_heading_bottom_border(paragraph, color_hex="000000", thickness="12"):
    pPr = paragraph._p.get_or_add_pPr()
    pBdr = OxmlElement("w:pBdr")
    bottom = OxmlElement("w:bottom")
    bottom.set(qn("w:val"), "single")
    bottom.set(qn("w:sz"), str(thickness))
    bottom.set(qn("w:space"), "2")
    bottom.set(qn("w:color"), color_hex.lstrip("#"))
    pBdr.append(bottom)
    pPr.append(pBdr)


def _build_header(doc):
    for section in doc.sections:
        header = section.header
        header.is_linked_to_previous = False
        for p in header.paragraphs:
            p.text = ""

        tbl = header.add_table(1, 3, width=Cm(17.5))
        tbl.alignment = WD_TABLE_ALIGNMENT.CENTER
        _set_table_borders(tbl, "888888", "4")

        cell_logo = tbl.cell(0, 0)
        cell_text = tbl.cell(0, 1)
        cell_meta = tbl.cell(0, 2)

        cell_logo.width = Cm(3.8)
        cell_text.width = Cm(9.7)
        cell_meta.width = Cm(4.0)

        cell_logo.vertical_alignment = WD_ALIGN_VERTICAL.CENTER
        cell_text.vertical_alignment = WD_ALIGN_VERTICAL.CENTER
        cell_meta.vertical_alignment = WD_ALIGN_VERTICAL.CENTER

        _set_cell_margins(cell_logo, top=60, bottom=60, left=80, right=80)
        _set_cell_margins(cell_text, top=60, bottom=60, left=80, right=80)
        _set_cell_margins(cell_meta, top=20, bottom=20, left=40, right=40)

        # Logo
        p_l = cell_logo.paragraphs[0]
        p_l.alignment = WD_ALIGN_PARAGRAPH.CENTER
        if os.path.exists(LOGO_PATH):
            p_l.add_run().add_picture(LOGO_PATH, width=Cm(3.2))

        # Textos oficiales
        p_t = cell_text.paragraphs[0]
        p_t.alignment = WD_ALIGN_PARAGRAPH.LEFT
        p_t.paragraph_format.line_spacing = 1.05

        r1 = p_t.add_run("Financiera para el Bienestar\n")
        r1.font.name = "Calibri"
        r1.font.size = Pt(8.5)
        r1.font.bold = True
        r1.font.color.rgb = RGBColor(85, 85, 85)

        r2 = p_t.add_run("Dirección de Administración y Finanzas\nSubdirección de Tecnologías de la Información y Comunicaciones\nGerencia de Desarrollo de Sistemas Administrativos\n")
        r2.font.name = "Calibri"
        r2.font.size = Pt(7.5)
        r2.font.color.rgb = RGBColor(100, 100, 100)

        r3 = p_t.add_run("Manual de Usuario de Sistema Integral de Asuntos Jurídicos")
        r3.font.name = "Calibri"
        r3.font.size = Pt(8)
        r3.font.bold = True
        r3.font.color.rgb = RGBColor(60, 60, 60)

        # Subtabla metadatos
        sub_tbl = cell_meta.add_table(3, 2)
        sub_tbl.alignment = WD_TABLE_ALIGNMENT.CENTER
        _set_table_borders(sub_tbl, "888888", "4")

        meta_data = [
            ("HOJA", "Página "),
            ("FECHA", "Septiembre 2026"),
            ("ID o Nomenclatura", "MGDSA/001/STIC/2026")
        ]
        for idx, (k, v) in enumerate(meta_data):
            ck = sub_tbl.cell(idx, 0)
            cv = sub_tbl.cell(idx, 1)
            ck.width = Cm(1.3)
            cv.width = Cm(2.7)
            _set_cell_margins(ck, top=20, bottom=20, left=30, right=30)
            _set_cell_margins(cv, top=20, bottom=20, left=30, right=30)

            pk = ck.paragraphs[0]
            pk.alignment = WD_ALIGN_PARAGRAPH.CENTER
            rk = pk.add_run(k)
            rk.font.name = "Calibri"
            rk.font.size = Pt(6.5)
            rk.font.bold = True

            pv = cv.paragraphs[0]
            pv.alignment = WD_ALIGN_PARAGRAPH.CENTER
            if k == "HOJA":
                rv = pv.add_run("Página ")
                rv.font.name = "Calibri"
                rv.font.size = Pt(6.5)
                rv.font.color.rgb = RGBColor(0, 150, 180)

                rf1 = OxmlElement("w:r")
                fld1 = OxmlElement("w:fldChar")
                fld1.set(qn("w:fldCharType"), "begin")
                rf1.append(fld1)
                pv._p.append(rf1)

                rin = OxmlElement("w:r")
                instr = OxmlElement("w:instrText")
                instr.set(qn("xml:space"), "preserve")
                instr.text = " PAGE "
                rin.append(instr)
                pv._p.append(rin)

                rf2 = OxmlElement("w:r")
                fld2 = OxmlElement("w:fldChar")
                fld2.set(qn("w:fldCharType"), "end")
                rf2.append(fld2)
                pv._p.append(rf2)
            else:
                rv = pv.add_run(v)
                rv.font.name = "Calibri"
                rv.font.size = Pt(6.5)


def _sec_heading(doc, text: str):
    p = doc.add_paragraph()
    p.paragraph_format.space_before = Pt(16)
    p.paragraph_format.space_after = Pt(6)
    _add_heading_bottom_border(p, "000000", "12")

    r = p.add_run(text)
    r.font.name = "Calibri"
    r.font.size = Pt(16)
    r.font.bold = True
    r.font.color.rgb = RGBColor(0, 0, 0)
    return p


def _sub_heading(doc, text: str):
    p = doc.add_paragraph()
    p.paragraph_format.space_before = Pt(12)
    p.paragraph_format.space_after = Pt(4)

    r = p.add_run(text)
    r.font.name = "Calibri"
    r.font.size = Pt(12)
    r.font.bold = True
    r.font.underline = True
    r.font.color.rgb = RGBColor(0, 0, 0)
    return p


def _sub_sub_heading(doc, text: str):
    p = doc.add_paragraph()
    p.paragraph_format.space_before = Pt(8)
    p.paragraph_format.space_after = Pt(2)

    r = p.add_run(text)
    r.font.name = "Calibri"
    r.font.size = Pt(11)
    r.font.bold = True
    r.font.color.rgb = RGBColor(0, 0, 0)
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
                r = p.add_run(l)
                r.font.name = "Calibri"
                r.font.size = Pt(11)
                r.font.color.rgb = RGBColor(51, 51, 51)
        else:
            clean_text = " ".join(lines)
            p = doc.add_paragraph()
            p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
            p.paragraph_format.space_after = Pt(6)
            p.paragraph_format.line_spacing = 1.15
            r = p.add_run(clean_text)
            r.font.name = "Calibri"
            r.font.size = Pt(11)
            r.font.color.rgb = RGBColor(51, 51, 51)


def build_exact_institutional_manual():
    doc = Document()

    for sec in doc.sections:
        sec.top_margin    = Cm(2.0)
        sec.bottom_margin = Cm(2.0)
        sec.left_margin   = Cm(2.0)
        sec.right_margin  = Cm(2.0)

    doc.styles["Normal"].font.name = "Calibri"
    doc.styles["Normal"].font.size = Pt(11)

    _build_header(doc)

    # ─── PORTADA ─────────────────────────────────────────────────────────────
    p_date = doc.add_paragraph()
    p_date.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_date.paragraph_format.space_before = Cm(2.5)
    p_date.paragraph_format.space_after = Cm(1.5)
    r_date = p_date.add_run("SEPTIEMBRE 2026")
    r_date.font.name = "Calibri"
    r_date.font.size = Pt(22)
    r_date.font.bold = True
    r_date.font.color.rgb = RGBColor(0, 0, 0)

    # Cuadro con propósito (borde negro como en Screenshot 1)
    tbl_p = doc.add_table(1, 1)
    tbl_p.alignment = WD_TABLE_ALIGNMENT.CENTER
    _set_table_borders(tbl_p, "000000", "8")
    cp = tbl_p.cell(0, 0)
    cp.width = Cm(15.5)
    _set_cell_margins(cp, top=180, bottom=180, left=200, right=200)

    pp = cp.paragraphs[0]
    pp.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
    pp.paragraph_format.line_spacing = 1.15
    rp = pp.add_run(
        "Este documento tiene como propósito brindar una explicación detallada y estructurada "
        "sobre el uso del sistema SIAJ. A través de este material, se busca orientar al personal que "
        "cuenta con este tipo de acceso, explicando de manera clara las funcionalidades disponibles, "
        "los alcances y las limitaciones propias del rol."
    )
    rp.font.name = "Calibri"
    rp.font.size = Pt(11)
    rp.font.color.rgb = RGBColor(0, 0, 0)

    doc.add_page_break()

    # ─── 2. GLOSARIO DE TÉRMINOS, SIGLAS Y ACRÓNIMOS ──────────────────────────
    _sec_heading(doc, "2. GLOSARIO DE TÉRMINOS, SIGLAS Y ACRÓNIMOS")
    _body_text(doc, "A continuación, se muestra el glosario de términos, siglas y acrónimos que pueden ser encontrados a lo largo del presente documento.")

    tbl_g = doc.add_table(len(GLOSARIO) + 1, 2)
    tbl_g.alignment = WD_TABLE_ALIGNMENT.CENTER
    _set_table_borders(tbl_g, "000000", "6")

    _set_cell_bg(tbl_g.cell(0, 0), "E0E0E0")
    _set_cell_bg(tbl_g.cell(0, 1), "E0E0E0")
    for col, htxt in enumerate(["Términos / Siglas / Acrónimos", "Significado"]):
        cell = tbl_g.cell(0, col)
        cell.width = Cm(5.0 if col == 0 else 11.5)
        _set_cell_margins(cell, top=80, bottom=80, left=100, right=100)
        p = cell.paragraphs[0]
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        r = p.add_run(htxt)
        r.font.name = "Calibri"
        r.font.bold = True
        r.font.size = Pt(10.5)
        r.font.color.rgb = RGBColor(0, 0, 0)

    for idx, (term, defn) in enumerate(GLOSARIO, start=1):
        c0 = tbl_g.cell(idx, 0)
        c1 = tbl_g.cell(idx, 1)
        c0.width = Cm(5.0)
        c1.width = Cm(11.5)
        _set_cell_margins(c0, top=60, bottom=60, left=80, right=80)
        _set_cell_margins(c1, top=60, bottom=60, left=80, right=80)

        p0 = c0.paragraphs[0]
        p0.alignment = WD_ALIGN_PARAGRAPH.CENTER
        r0 = p0.add_run(term)
        r0.font.name = "Calibri"
        r0.font.bold = True
        r0.font.size = Pt(10)

        p1 = c1.paragraphs[0]
        p1.alignment = WD_ALIGN_PARAGRAPH.LEFT
        r1 = p1.add_run(defn)
        r1.font.name = "Calibri"
        r1.font.size = Pt(10)

    doc.add_paragraph()

    # ─── 3. LENGUAJE INCLUYENTE Y NO DISCRIMINATORIO ─────────────────────────
    _sec_heading(doc, "3. LENGUAJE INCLUYENTE Y NO DISCRIMINATORIO")
    _body_text(doc, (
        "El lenguaje servidor público en el documento Manual de Usuario de Sistema Integral de Asuntos Jurídicos "
        "no busca generar ninguna distinción, ni marca diferencias entre hombres y mujeres, por lo que las referencias "
        "o alusiones en la redacción hechas hacia un género representa a ambos sexos."
    ))

    # ─── 4. INTRODUCCIÓN ──────────────────────────────────────────────────────
    _sec_heading(doc, "4. INTRODUCCIÓN")
    _body_text(doc, (
        "Este documento proporciona la información esencial para operar y navegar por los diferentes apartados del "
        "sistema a los que se puede acceder con los privilegios asignados al perfil del usuario."
    ))

    _sub_heading(doc, "4.1 OBJETIVO GENERAL")
    _body_text(doc, (
        "Establecer una guía práctica, clara y estandarizada para la operación del Sistema Integral de Asuntos Jurídicos (SIAJ), "
        "orientando a las personas usuarias en la gestión, seguimiento y control eficiente de expedientes, audiencias, "
        "términos procesales, documentos y trazabilidad institucional."
    ))

    _sub_heading(doc, "4.2 OBJETIVOS ESPECÍFICOS")
    _body_text(doc, (
        "• Proporcionar información clara y estructurada destacando las acciones operativas que se pueden llevar a cabo en cada sección del sistema.\n\n"
        "• Facilitar el aprendizaje y la navegación por la interfaz del SIAJ, asegurando el correcto registro de información legal y el seguimiento mediante semáforos de vencimiento.\n\n"
        "• Estandarizar la consulta de trazabilidad, carga de documentos y gestión de tareas para optimizar los procesos jurídicos institucionales."
    ))

    doc.add_page_break()

    # ─── 5. LINEAMIENTOS O DESARROLLO DEL DOCUMENTO ──────────────────────────
    _sec_heading(doc, "5. LINEAMIENTOS O DESARROLLO DEL DOCUMENTO")
    _body_text(doc, (
        "En esta sección se detalla el funcionamiento de cada uno de los módulos operativos que integran el "
        "Sistema Integral de Asuntos Jurídicos (SIAJ), incluyendo sus componentes de interfaz visual, "
        "descripción funcional y los pasos detallados para la realización de operaciones."
    ))

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

        _sub_heading(doc, section_title)

        img_path = os.path.join(SCREENSHOTS_DIR, f"{slug}.png")
        if os.path.exists(img_path) and os.path.getsize(img_path) > 10000:
            p_img = doc.add_paragraph()
            p_img.alignment = WD_ALIGN_PARAGRAPH.CENTER
            p_img.paragraph_format.space_before = Pt(4)
            p_img.paragraph_format.space_after = Pt(4)
            r_img = p_img.add_run()
            r_img.add_picture(img_path, width=Cm(15.5))

            p_cap = doc.add_paragraph(f"Figura. {content['titulo']}")
            p_cap.alignment = WD_ALIGN_PARAGRAPH.CENTER
            p_cap.paragraph_format.space_after = Pt(8)
            r_cap = p_cap.runs[0]
            r_cap.font.name = "Calibri"
            r_cap.font.size = Pt(9)
            r_cap.font.italic = True
            r_cap.font.color.rgb = RGBColor(85, 85, 85)

        _sub_sub_heading(doc, "Descripción Funcional")
        _body_text(doc, content["descripcion"])

        _sub_sub_heading(doc, "Elementos de la Pantalla")
        elementos = content.get("elementos", [])
        if elementos:
            tbl_e = doc.add_table(len(elementos) + 1, 2)
            tbl_e.alignment = WD_TABLE_ALIGNMENT.CENTER
            _set_table_borders(tbl_e, "000000", "4")

            _set_cell_bg(tbl_e.cell(0, 0), "E0E0E0")
            _set_cell_bg(tbl_e.cell(0, 1), "E0E0E0")
            for col, htxt in enumerate(["Elemento", "Descripción"]):
                cell = tbl_e.cell(0, col)
                cell.width = Cm(4.5 if col == 0 else 12.0)
                _set_cell_margins(cell, top=60, bottom=60, left=80, right=80)
                p = cell.paragraphs[0]
                p.alignment = WD_ALIGN_PARAGRAPH.CENTER
                r = p.add_run(htxt)
                r.font.name = "Calibri"
                r.font.bold = True
                r.font.size = Pt(9.5)

            for idx_e, (elem, desc) in enumerate(elementos, start=1):
                c0 = tbl_e.cell(idx_e, 0)
                c1 = tbl_e.cell(idx_e, 1)
                c0.width = Cm(4.5)
                c1.width = Cm(12.0)
                _set_cell_margins(c0, top=50, bottom=50, left=60, right=60)
                _set_cell_margins(c1, top=50, bottom=50, left=60, right=60)

                p0 = c0.paragraphs[0]
                p0.alignment = WD_ALIGN_PARAGRAPH.LEFT
                r0 = p0.add_run(elem)
                r0.font.name = "Calibri"
                r0.font.bold = True
                r0.font.size = Pt(9.5)

                p1 = c1.paragraphs[0]
                p1.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
                r1 = p1.add_run(desc)
                r1.font.name = "Calibri"
                r1.font.size = Pt(9.5)

        doc.add_paragraph()

        _sub_sub_heading(doc, "Pasos de Operación")
        for idx_p, paso in enumerate(content.get("pasos", []), start=1):
            clean_paso = " ".join(line.strip() for line in paso.splitlines() if line.strip())
            p = doc.add_paragraph(style="List Number")
            p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
            p.paragraph_format.space_after = Pt(3)
            p.paragraph_format.line_spacing = 1.15
            r = p.add_run(clean_paso)
            r.font.name = "Calibri"
            r.font.size = Pt(11)
            r.font.color.rgb = RGBColor(51, 51, 51)

        nota = content.get("nota")
        if nota:
            clean_nota = " ".join(line.strip() for line in nota.splitlines() if line.strip())
            doc.add_paragraph()
            p_nota = doc.add_paragraph()
            p_nota.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
            p_nota.paragraph_format.left_indent = Cm(1)
            p_nota.paragraph_format.right_indent = Cm(1)
            p_nota.paragraph_format.line_spacing = 1.15

            rn = p_nota.add_run("Nota: ")
            rn.font.bold = True
            rn.font.name = "Calibri"
            rn.font.size = Pt(10.5)

            rt = p_nota.add_run(clean_nota)
            rt.font.name = "Calibri"
            rt.font.size = Pt(10.5)
            rt.font.color.rgb = RGBColor(51, 51, 51)

        doc.add_paragraph()

    # 5.11 FAQ
    _sub_heading(doc, "5.11 Preguntas Frecuentes (FAQ)")
    for i, (pregunta, respuesta) in enumerate(FAQ, start=1):
        clean_resp = " ".join(line.strip() for line in respuesta.splitlines() if line.strip())
        p_q = doc.add_paragraph()
        r_q = p_q.add_run(f"P{i}. {pregunta}")
        r_q.font.bold = True
        r_q.font.name = "Calibri"
        r_q.font.size = Pt(11)

        p_r = doc.add_paragraph()
        p_r.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
        p_r.paragraph_format.left_indent = Cm(0.5)
        p_r.paragraph_format.space_after = Pt(6)
        p_r.paragraph_format.line_spacing = 1.15
        r_r = p_r.add_run(f"R: {clean_resp}")
        r_r.font.name = "Calibri"
        r_r.font.size = Pt(11)
        r_r.font.color.rgb = RGBColor(51, 51, 51)

    # 5.12 Contacto
    _sub_heading(doc, "5.12 Contacto y Soporte Técnico")
    _body_text(doc, "Para reportar incidencias, solicitar accesos o resolver dudas sobre el funcionamiento del sistema, comuníquese con el área de soporte técnico:")

    tbl_c = doc.add_table(3, 2)
    tbl_c.alignment = WD_TABLE_ALIGNMENT.CENTER
    _set_table_borders(tbl_c, "000000", "4")
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
        _set_cell_bg(c0, "E0E0E0")
        _set_cell_margins(c0, top=60, bottom=60, left=80, right=80)
        _set_cell_margins(c1, top=60, bottom=60, left=80, right=80)

        p0 = c0.paragraphs[0]
        p0.alignment = WD_ALIGN_PARAGRAPH.LEFT
        r0 = p0.add_run(lbl)
        r0.font.bold = True
        r0.font.name = "Calibri"
        r0.font.size = Pt(9.5)

        p1 = c1.paragraphs[0]
        p1.alignment = WD_ALIGN_PARAGRAPH.LEFT
        r1 = p1.add_run(val)
        r1.font.name = "Calibri"
        r1.font.size = Pt(9.5)

    doc.add_paragraph()

    # 5.13 Anexos
    _sub_heading(doc, "5.13 Anexo A: Módulos de Administración y Supervisión (Jefatura, Gerencia y Administración)")
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

        _sub_sub_heading(doc, section_title)
        img_path = os.path.join(SCREENSHOTS_DIR, f"{slug}.png")
        if os.path.exists(img_path) and os.path.getsize(img_path) > 10000:
            p_img = doc.add_paragraph()
            p_img.alignment = WD_ALIGN_PARAGRAPH.CENTER
            p_img.paragraph_format.space_before = Pt(4)
            p_img.paragraph_format.space_after = Pt(4)
            r_img = p_img.add_run()
            r_img.add_picture(img_path, width=Cm(15.5))

            p_cap = doc.add_paragraph(f"Figura. {content['titulo']}")
            p_cap.alignment = WD_ALIGN_PARAGRAPH.CENTER
            p_cap.paragraph_format.space_after = Pt(8)
            r_cap = p_cap.runs[0]
            r_cap.font.name = "Calibri"
            r_cap.font.size = Pt(9)
            r_cap.font.italic = True
            r_cap.font.color.rgb = RGBColor(85, 85, 85)

        _body_text(doc, content["descripcion"])

    doc.add_page_break()

    # ─── 6. FIRMAS DE APROBACIÓN ──────────────────────────────────────────────
    _sec_heading(doc, "6. FIRMAS DE APROBACIÓN")
    _body_text(doc, "El presente proceso se acredita por:")

    tbl_sig = doc.add_table(2, 3)
    tbl_sig.alignment = WD_TABLE_ALIGNMENT.CENTER
    _set_table_borders(tbl_sig, "000000", "6")

    cols_titles = ["Elaboró", "Revisó", "Aprobó"]
    for c_idx, title_text in enumerate(cols_titles):
        cell_h = tbl_sig.cell(0, c_idx)
        cell_h.width = Cm(5.5)
        _set_cell_bg(cell_h, "E0E0E0")
        _set_cell_margins(cell_h, top=80, bottom=80, left=80, right=80)
        p_h = cell_h.paragraphs[0]
        p_h.alignment = WD_ALIGN_PARAGRAPH.CENTER
        r_h = p_h.add_run(title_text)
        r_h.font.name = "Calibri"
        r_h.font.bold = True
        r_h.font.size = Pt(10.5)

    roles_sig = [
        ("Área de Desarrollo de Sistemas", "Subdirección de Tecnologías de la Información"),
        ("Gerencia de Asuntos Jurídicos", "Área Jurídica"),
        ("Dirección de Administración y Finanzas", "Financiera para el Bienestar")
    ]
    for c_idx, (cargo, area) in enumerate(roles_sig):
        cell_b = tbl_sig.cell(1, c_idx)
        cell_b.width = Cm(5.5)
        _set_cell_margins(cell_b, top=350, bottom=120, left=80, right=80)
        p_b = cell_b.paragraphs[0]
        p_b.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p_b.paragraph_format.line_spacing = 1.15

        r_line = p_b.add_run("___________________________\n\n")
        r_line.font.name = "Calibri"
        r_line.font.size = Pt(9)
        r_line.font.color.rgb = RGBColor(120, 120, 120)

        r_c = p_b.add_run(f"{cargo}\n")
        r_c.font.name = "Calibri"
        r_c.font.bold = True
        r_c.font.size = Pt(9.5)

        r_a = p_b.add_run(area)
        r_a.font.name = "Calibri"
        r_a.font.size = Pt(8.5)
        r_a.font.color.rgb = RGBColor(88, 88, 88)

    try:
        doc.save(OUTPUT_PATH)
        print(f"[OK] Documento institucional visual exacto guardado en:\n  {OUTPUT_PATH}")
    except PermissionError:
        alt_path = OUTPUT_PATH.replace(".docx", "_Actualizado.docx")
        doc.save(alt_path)
        print(f"[OK] Documento abierto en Word, guardado alternativa en:\n  {alt_path}")


if __name__ == "__main__":
    build_exact_institutional_manual()
