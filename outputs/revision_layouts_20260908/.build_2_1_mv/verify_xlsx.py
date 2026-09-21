from pathlib import Path
from zipfile import ZipFile
from xml.etree import ElementTree as ET

out = Path(__file__).parents[1] / "Layouts_2_1_MV"
ns = {"m": "http://schemas.openxmlformats.org/spreadsheetml/2006/main", "r": "http://schemas.openxmlformats.org/officeDocument/2006/relationships"}
expected = {
    "Layout_Catalogos_V2_MV.xlsx": ["CONTROL", "GERENCIAS", "MATERIAS", "SEDES", "PRIORIDADES", "TIPOS_EXPEDIENTE", "ORGANOS", "TIPOS_AUDIENCIA", "TIPOS_TERMINO", "INSTRUCCIONES", "DICCIONARIO", "CATALOGOS_REFERENCIA"],
    "Layout_Usuarios_V2_MV.xlsx": ["CONTROL", "CAPTURA", "INSTRUCCIONES", "DICCIONARIO", "CATALOGOS_REFERENCIA"],
    "Layout_Expedientes_V2_MV.xlsx": ["CONTROL", "CAPTURA", "INSTRUCCIONES", "DICCIONARIO", "CATALOGOS_REFERENCIA"],
    "Layout_Audiencias_V2_MV.xlsx": ["CONTROL", "CAPTURA", "INSTRUCCIONES", "DICCIONARIO", "CATALOGOS_REFERENCIA"],
    "Layout_Terminos_V2_MV.xlsx": ["CONTROL", "CAPTURA", "INSTRUCCIONES", "DICCIONARIO", "CATALOGOS_REFERENCIA"],
    "Layout_Documentos_Expediente_V2_MV.xlsx": ["CONTROL", "CAPTURA", "INSTRUCCIONES", "DICCIONARIO", "CATALOGOS_REFERENCIA"],
}

for name, sheet_names in expected.items():
    path = out / name
    assert path.exists() and path.stat().st_size > 0, name
    with ZipFile(path) as z:
        root = ET.fromstring(z.read("xl/workbook.xml"))
        actual = [node.attrib["name"] for node in root.find("m:sheets", ns)]
        assert actual == sheet_names, (name, actual)
        validation_count = 0
        table_parts = 0
        for member in z.namelist():
            if member.startswith("xl/worksheets/sheet") and member.endswith(".xml"):
                ws = ET.fromstring(z.read(member))
                node = ws.find("m:dataValidations", ns)
                if node is not None:
                    validation_count += int(node.attrib.get("count", "0"))
                tables = ws.find("m:tableParts", ns)
                if tables is not None:
                    table_parts += int(tables.attrib.get("count", "0"))
        assert table_parts >= (1 + (8 if name.startswith("Layout_Catalogos") else 1)), (name, table_parts)
        assert validation_count > 0, (name, validation_count)
        print(f"OK|{name}|sheets={len(actual)}|tables={table_parts}|validations={validation_count}")

