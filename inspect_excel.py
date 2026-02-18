import openpyxl
import os

files = [
    "ARCHIVO LABORALES.xlsx",
    "Agenda de términos en materia Civil, Mercantil, Fiscal y Administrativo.xlsx"
]

base_path = "documentos_de_referencia"

def is_likely_header(row):
    # Heuristic: row has at least 3 strings and one of them looks like a header
    strings = [str(c).strip() for c in row if c is not None and str(c).strip()]
    if len(strings) < 2:
        return False
    keywords = ["EXPEDIENTE", "ACTOR", "JUICIO", "FECHA", "TÉRMINO", "AUDIENCIA", "JUZGADO", "MATERIA"]
    for s in strings:
        if any(k in s.upper() for k in keywords):
            return True
    return False

for f in files:
    path = os.path.join(base_path, f)
    print(f"--- FILE: {f} ---")
    try:
        wb = openpyxl.load_workbook(path, read_only=True, data_only=True)
        # Check only first 3 sheets to avoid huge output, or check specific ones
        sheets_to_check = wb.sheetnames[:5] 
        for sheet_name in sheets_to_check:
            print(f"  SHEET: {sheet_name}")
            ws = wb[sheet_name]
            found_header = False
            for i, row in enumerate(ws.iter_rows(min_row=1, max_row=15, values_only=True), start=1):
                clean_row = [str(c).strip() if c is not None else "" for c in row]
                # Print non-empty rows
                if any(clean_row):
                     print(f"    ROW {i}: {clean_row}")
                
                if is_likely_header(row):
                    print(f"    >>> LIKELY HEADER FOUND AT ROW {i} <<<")
                    found_header = True
            if not found_header:
                 print("    (No obvious header found in first 15 rows)")
            print("\n")
    except Exception as e:
        print(f"    ERROR: {e}")
    print("\n")
