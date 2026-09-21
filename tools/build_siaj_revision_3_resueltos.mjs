import fs from "node:fs/promises";
import path from "node:path";
import { SpreadsheetFile, Workbook } from "@oai/artifact-tool";

const outputDir = "outputs/vulnerabilidades_siaj";
const outputPath = path.join(outputDir, "SIAJ_Vulnerabilidades_Atendidas_Acumulado_Revision_3_2026-09-15.xlsx");
const previewDir = path.join(outputDir, "previews_acumulado_revision_3_2026-09-15");

const rev1Date = new Date("2026-08-20T00:00:00");
const rev2Date = new Date("2026-09-03T00:00:00");
const rev3Date = new Date("2026-09-15T00:00:00");
const headers = ["Vulnerabilidad", "Package Name", "Categoría", "PáginaDF", "ESTADO", "Fecha de entrega", "Avance", "Observaciones"];
const severities = ["CRITICAL", "HIGH", "MEDIUM", "LOW"];

const rows = [
  ["HIGH", "Absolute_Path_Traversal 1/4", "Código fuente Java", "CWE-36", 14, "RESUELTO", rev1Date, "Revisión 1", "Entregado en el primer avance."],
  ["HIGH", "Absolute_Path_Traversal 2/4", "Código fuente Java", "CWE-36", 14, "RESUELTO", rev1Date, "Revisión 1", "Entregado en el primer avance."],
  ["HIGH", "Absolute_Path_Traversal 3/4", "Código fuente Java", "CWE-36", 14, "RESUELTO", rev1Date, "Revisión 1", "Entregado en el primer avance."],
  ["HIGH", "Absolute_Path_Traversal 4/4", "Código fuente Java", "CWE-36", 14, "RESUELTO", rev1Date, "Revisión 1", "Entregado en el primer avance."],
  ["HIGH", "Dependencia xlsx-0.18.5", "xlsx", "CWE-1333", 151, "RESUELTO", rev1Date, "Revisión 1", "Entregado en el primer avance."],
  ["HIGH", "Dependencia xlsx-0.18.5", "xlsx", "CWE-1321", 151, "RESUELTO", rev1Date, "Revisión 1", "Entregado en el primer avance."],
  ["HIGH", "Secret Detection 1/3", "Configuración / secretos", "Secret Detection", 222, "RESUELTO", rev1Date, "Revisión 1", "Entregado en el primer avance."],
  ["HIGH", "Secret Detection 2/3", "Configuración / secretos", "Secret Detection", 222, "RESUELTO", rev1Date, "Revisión 1", "Entregado en el primer avance."],
  ["HIGH", "Secret Detection 3/3", "Configuración / secretos", "Secret Detection", 222, "RESUELTO", rev1Date, "Revisión 1", "Entregado en el primer avance."],
  ["MEDIUM", "Missing_HSTS_Header 1/1", "Código fuente Java", "CWE-346", 143, "RESUELTO", rev1Date, "Revisión 1", "Entregado en el primer avance."],
  ["MEDIUM", "Open_Redirect 1/3", "Código fuente Java", "CWE-601", 141, "RESUELTO", rev1Date, "Revisión 1", "Entregado en el primer avance, actualizado al conteo del reporte de agosto."],
  ["MEDIUM", "Open_Redirect 2/3", "Código fuente Java", "CWE-601", 141, "RESUELTO", rev1Date, "Revisión 1", "Entregado en el primer avance, actualizado al conteo del reporte de agosto."],
  ["CRITICAL", "Spring_View_SPEL_Injection 1/4", "Código fuente Java", "CWE-917", 11, "RESUELTO", rev2Date, "Revisión 2", "Entregado en la segunda revisión."],
  ["CRITICAL", "Spring_View_SPEL_Injection 2/4", "Código fuente Java", "CWE-917", 11, "RESUELTO", rev2Date, "Revisión 2", "Entregado en la segunda revisión."],
  ["CRITICAL", "Spring_View_SPEL_Injection 3/4", "Código fuente Java", "CWE-917", 11, "RESUELTO", rev2Date, "Revisión 2", "Entregado en la segunda revisión."],
  ["CRITICAL", "Spring_View_SPEL_Injection 4/4", "Código fuente Java", "CWE-917", 11, "RESUELTO", rev2Date, "Revisión 2", "Entregado en la segunda revisión."],
  ["MEDIUM", "Open_Redirect 3/3", "Código fuente Java", "CWE-601", 141, "RESUELTO", rev2Date, "Revisión 2", "Entregado en la segunda revisión."],
];

for (let i = 1; i <= 60; i += 1) {
  rows.push([
    "MEDIUM",
    `Parameter_Tampering ${i}/235`,
    "Código fuente Java",
    "CWE-472",
    25,
    "RESUELTO",
    rev3Date,
    "Revisión 3",
    "Atendido en el avance del 15/09/2026 mediante validaciones de alcance, permisos y consistencia de parámetros en flujos de autenticación, usuarios y operaciones relacionadas.",
  ]);
}

for (let i = 1; i <= 9; i += 1) {
  rows.push([
    "MEDIUM",
    `Unsafe_Object_Binding ${i}/9`,
    "Código fuente Java",
    "CWE-915",
    17,
    "RESUELTO",
    rev3Date,
    "Revisión 3",
    "Atendido en el avance del 15/09/2026 mediante control explícito de campos administrables y validaciones del servicio antes de persistir cambios.",
  ]);
}

for (let i = 1; i <= 4; i += 1) {
  rows.push([
    "MEDIUM",
    `Session_Fixation ${i}/4`,
    "Código fuente Java",
    "CWE-384",
    22,
    "RESUELTO",
    rev3Date,
    "Revisión 3",
    "Atendido en el avance del 15/09/2026 con control de sesiones activas, versión de seguridad de usuario y revocación de sesiones/tokens tras cambios sensibles.",
  ]);
}

rows.push([
  "MEDIUM",
  "Insufficiently_Protected_Credentials 1/1",
  "Código fuente Java",
  "CWE-522",
  144,
  "RESUELTO",
  rev3Date,
  "Revisión 3",
  "Atendido en el avance del 15/09/2026 sustituyendo contraseña temporal por código manual de un uso, con expiración, hash, auditoría e invalidación posterior.",
]);

const colors = {
  CRITICAL: { title: "#C00000", light: "#FCE4D6", tab: "#C00000" },
  HIGH: { title: "#70AD47", light: "#E2F0D9", tab: "#70AD47" },
  MEDIUM: { title: "#FFC000", light: "#FFF2CC", tab: "#FFC000" },
  LOW: { title: "#9EADCC", light: "#D9E2F3", tab: "#9EADCC" },
};

function styleSheet(sheet, severity, rowCount) {
  const lastRow = Math.max(5, rowCount + 4);
  const palette = colors[severity] ?? colors.HIGH;
  sheet.showGridLines = false;
  sheet.tabColor = palette.tab;
  sheet.getRange("A1:H1").merge();
  sheet.getRange("A1").values = [[`VULNERABILIDADES ATENDIDAS - ${severity}`]];
  sheet.getRange("A1").format = {
    fill: palette.title,
    font: { bold: true, color: "#FFFFFF", fontSize: 13 },
    horizontalAlignment: "center",
    verticalAlignment: "center",
  };
  sheet.getRange("A2:H2").merge();
  sheet.getRange("A2").values = [["Sistema Integral de Agenda Jurídica (SIAJ) - Tercera revisión"]];
  sheet.getRange("A2").format = {
    fill: palette.light,
    font: { bold: true, color: "#000000" },
    horizontalAlignment: "center",
  };
  sheet.getRange("A3:H3").merge();
  sheet.getRange("A3").values = [["Fecha de entrega del avance: 2026-09-15"]];
  sheet.getRange("A3").format = {
    fill: "#F2F2F2",
    font: { italic: true, color: "#404040" },
    horizontalAlignment: "center",
  };
  sheet.getRange("A4:H4").values = [headers];
  sheet.getRange("A4:H4").format = {
    fill: "#1F4E78",
    font: { bold: true, color: "#FFFFFF" },
    horizontalAlignment: "center",
    verticalAlignment: "center",
  };
  sheet.getRange(`A4:H${lastRow}`).format.borders = {
    preset: "all",
    style: "thin",
    color: "#D9E2F3",
  };
  sheet.getRange(`A5:B${lastRow}`).format.horizontalAlignment = "left";
  sheet.getRange(`C5:G${lastRow}`).format.horizontalAlignment = "center";
  sheet.getRange(`A5:A${lastRow}`).format.wrapText = true;
  sheet.getRange(`B5:B${lastRow}`).format.wrapText = true;
  sheet.getRange(`F5:F${lastRow}`).setNumberFormat("yyyy-mm-dd");
  sheet.getRange(`E5:E${lastRow}`).format = {
    fill: "#A9D18E",
    font: { bold: true, color: "#000000" },
    horizontalAlignment: "center",
  };
  sheet.getRange(`H5:H${lastRow}`).format.wrapText = true;
  [42, 32, 18, 12, 15, 18, 14, 64].forEach((width, index) => {
    sheet.getRangeByIndexes(0, index, 1, 1).format.columnWidth = width;
  });
  sheet.freezePanes.freezeRows(4);
}

function addSeveritySheet(workbook, severity) {
  const sheet = workbook.worksheets.add(severity);
  const filtered = rows.filter((row) => row[0] === severity);
  const values = filtered.length
    ? filtered.map((row) => row.slice(1))
    : [["Sin vulnerabilidades atendidas en esta severidad para este avance", "", "", "", "", "", "", ""]];
  sheet.getRange(`A5:H${values.length + 4}`).values = values;
  styleSheet(sheet, severity, values.length);
}

function addSummary(workbook) {
  const sheet = workbook.worksheets.add("RESUMEN");
  sheet.showGridLines = false;
  sheet.tabColor = "#1F4E78";
  sheet.getRange("A1:H1").merge();
  sheet.getRange("A1").values = [["SIAJ - AVANCE DE VULNERABILIDADES ATENDIDAS"]];
  sheet.getRange("A1").format = {
    fill: "#1F4E78",
    font: { bold: true, color: "#FFFFFF", fontSize: 13 },
    horizontalAlignment: "center",
  };
  sheet.getRange("A2:H2").merge();
  sheet.getRange("A2").values = [["Tercera revisión - Fecha de entrega: 2026-09-15"]];
  sheet.getRange("A2").format = {
    fill: "#D9EAF7",
    font: { bold: true, color: "#000000" },
    horizontalAlignment: "center",
  };

  const counts = severities.map((severity) => [severity, rows.filter((row) => row[0] === severity).length]);
  const total = rows.length;
  const revision1Total = rows.filter((row) => row[7] === "Revisión 1").length;
  const revision2Total = rows.filter((row) => row[7] === "Revisión 2").length;
  const revision3Total = rows.filter((row) => row[7] === "Revisión 3").length;
  sheet.getRange("A4:B8").values = [["Gravedad", "Resueltas"], ...counts];
  sheet.getRange("D4:G8").values = [
    ["Avance", "Fecha", "Estatus", "Total"],
    ["Revisión 1", rev1Date, "Entregado", revision1Total],
    ["Revisión 2", rev2Date, "Entregado", revision2Total],
    ["Revisión 3", rev3Date, "Para entrega", revision3Total],
    ["Acumulado", rev3Date, "En seguimiento", total],
  ];
  sheet.getRange("A4:B4").format = {
    fill: "#70AD47",
    font: { bold: true, color: "#000000" },
    horizontalAlignment: "center",
  };
  sheet.getRange("D4:G4").format = {
    fill: "#70AD47",
    font: { bold: true, color: "#000000" },
    horizontalAlignment: "center",
  };
  sheet.getRange("A4:B8").format.borders = { preset: "all", style: "thin", color: "#70AD47" };
  sheet.getRange("D4:G8").format.borders = { preset: "all", style: "thin", color: "#70AD47" };
  sheet.getRange("E5:E8").setNumberFormat("yyyy-mm-dd");
  sheet.getRange("B5:B8").format.numberFormat = "#,##0";
  sheet.getRange("G5:G8").format.numberFormat = "#,##0";
  [18, 14, 4, 16, 16, 18, 12, 4].forEach((width, index) => {
    sheet.getRangeByIndexes(0, index, 1, 1).format.columnWidth = width;
  });
}

await fs.mkdir(outputDir, { recursive: true });
await fs.mkdir(previewDir, { recursive: true });

const workbook = Workbook.create();
addSummary(workbook);
for (const severity of severities) {
  addSeveritySheet(workbook, severity);
}

for (const sheet of workbook.worksheets.items) {
  const used = sheet.getUsedRange();
  used.format.font = { typeface: "Calibri", fontSize: 11 };
  used.format.autofitRows();
}

workbook.recalculate();

const summaryCheck = await workbook.inspect({
  kind: "table",
  sheetId: "RESUMEN",
  range: "A1:H9",
  include: "values,formulas",
  tableMaxRows: 10,
  tableMaxCols: 8,
  maxChars: 5000,
});
console.log(summaryCheck.ndjson);

const mediumCheck = await workbook.inspect({
  kind: "table",
  sheetId: "MEDIUM",
  range: "A1:H18",
  include: "values,formulas",
  tableMaxRows: 18,
  tableMaxCols: 8,
  maxChars: 10000,
});
console.log(mediumCheck.ndjson);

const errors = await workbook.inspect({
  kind: "match",
  searchTerm: "#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A|#NUM!|#NULL!|#SPILL!|#CALC!",
  options: { useRegex: true, maxResults: 300 },
  summary: "final formula error scan",
});
console.log(errors.ndjson);

for (const sheet of workbook.worksheets.items) {
  const preview = await workbook.render({
    sheetName: sheet.name,
    autoCrop: "all",
    scale: 1,
    format: "png",
  });
  await fs.writeFile(
    path.join(previewDir, `${sheet.name.replace(/[\\/:*?"<>|]/g, "_")}.png`),
    new Uint8Array(await preview.arrayBuffer()),
  );
}

const output = await SpreadsheetFile.exportXlsx(workbook);
await output.save(outputPath);
console.log(JSON.stringify({
  outputPath,
  totalRows: rows.length,
  revision1Total: rows.filter((row) => row[7] === "Revisión 1").length,
  revision2Total: rows.filter((row) => row[7] === "Revisión 2").length,
  revision3Total: rows.filter((row) => row[7] === "Revisión 3").length,
}, null, 2));
