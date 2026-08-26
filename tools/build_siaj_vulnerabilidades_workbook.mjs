import fs from "node:fs/promises";
import { SpreadsheetFile, Workbook } from "@oai/artifact-tool";

const sourceTextPath = "tmp/pdfs/SIAJCx_ScanReport_extracted.txt";
const outputDir = "outputs/vulnerabilidades_siaj";
const outputPath = `${outputDir}/SIAJ_Plan_Vulnerabilidades_2026_2028.xlsx`;

const text = await fs.readFile(sourceTextPath, "utf8");

function pageForIndex(index) {
  const before = text.slice(0, index);
  const matches = [...before.matchAll(/--- PAGE (\d+) ---/g)];
  return matches.length ? Number(matches[matches.length - 1][1]) : null;
}

function clean(value) {
  return String(value ?? "")
    .replace(/\s+/g, " ")
    .replace(/\s+([:.,;])/g, "$1")
    .trim();
}

function monthDeliveries() {
  const dates = [new Date("2026-08-20T00:00:00")];
  for (let y = 2026; y <= 2028; y += 1) {
    const startMonth = y === 2026 ? 8 : 0;
    for (let m = startMonth; m < 12; m += 1) {
      dates.push(new Date(Date.UTC(y, m, 15)));
    }
  }
  return dates;
}

const deliveries = monthDeliveries();
const regularDeliveries = deliveries.slice(1);

function severityRank(severity) {
  return { CRITICAL: 0, HIGH: 1, MEDIUM: 2, LOW: 3 }[severity] ?? 9;
}

function nextRegularDelivery(index, total) {
  if (total <= 1) return regularDeliveries[0];
  const dateIndex = Math.floor((index / total) * regularDeliveries.length);
  return regularDeliveries[Math.min(dateIndex, regularDeliveries.length - 1)];
}

const rows = [];
let seq = 1;

function addRow({
  scanner,
  severity,
  vulnerability,
  packageName = "",
  version = "",
  category = "",
  cve = "",
  page = "",
  status = "PENDIENTE",
  notes = "",
}) {
  rows.push({
    id: `SIAJ-${String(seq).padStart(4, "0")}`,
    scanner,
    severity,
    vulnerability: clean(vulnerability),
    packageName: clean(packageName),
    version: clean(version),
    category: clean(category),
    cve: clean(cve),
    page,
    status,
    delivery: null,
    notes: clean(notes),
  });
  seq += 1;
}

const sastTypes = [
  { vulnerability: "Absolute_Path_Traversal", severity: "HIGH", count: 4, category: "CWE-36", page: 11 },
  { vulnerability: "Unsafe_Object_Binding", severity: "MEDIUM", count: 11, category: "CWE-915", page: 14 },
  { vulnerability: "Parameter_Tampering", severity: "MEDIUM", count: 196, category: "CWE-472", page: 20 },
  { vulnerability: "Missing_HSTS_Header", severity: "MEDIUM", count: 1, category: "CWE-346", page: 113 },
  { vulnerability: "Open_Redirect", severity: "MEDIUM", count: 2, category: "CWE-601", page: 114 },
];

for (const item of sastTypes) {
  for (let i = 1; i <= item.count; i += 1) {
    const fastTrack =
      item.vulnerability === "Absolute_Path_Traversal" ||
      item.vulnerability === "Missing_HSTS_Header" ||
      item.vulnerability === "Open_Redirect";
    addRow({
      scanner: "SAST",
      severity: item.severity,
      vulnerability: `${item.vulnerability} ${i}/${item.count}`,
      packageName: "Código fuente Java",
      category: item.category,
      page: item.page,
      status: fastTrack ? "EN PROCESO" : "PENDIENTE",
      notes: fastTrack
        ? "Candidato para primer avance por impacto y alcance acotado."
        : "Validar aplicabilidad antes de remediar.",
    });
  }
}

function parseScaSection() {
  const sectionStart = "SCA Scan Results (163 Results)";
  const sectionEnd = "Container Security Vulnerabilities";
  const start = text.indexOf(sectionStart);
  const end = text.indexOf(sectionEnd, start + sectionStart.length);
  if (start < 0 || end < 0) return 0;
  const section = text.slice(start, end);
  const lines = section.split(/\r?\n/);
  let count = 0;
  let packageName = "";
  let version = "";
  let category = "";
  let offset = start;
  for (let i = 0; i < lines.length; i += 1) {
    const line = lines[i];
    if (line.startsWith("Package Name:")) packageName = clean(line.replace("Package Name:", ""));
    if (line.startsWith("Version:")) version = clean(line.replace("Version:", ""));
    if (line.startsWith("Category:")) category = clean(line.replace("Category:", ""));
    if (!/^Result\s+\d+\s+of\s+\d+\s+-\s+Vulnerability/.test(line)) {
      offset += line.length + 1;
      continue;
    }
    const severityLine = clean(lines[i + 1] ?? "");
    const severity = clean(severityLine.split(" ")[0]).toUpperCase();
    let cve = "";
    const descParts = [];
    let inDescription = false;
    for (let j = i + 2; j < lines.length; j += 1) {
      const next = lines[j];
      if (/^Result\s+\d+\s+of\s+\d+\s+-\s+Vulnerability/.test(next) || next.startsWith("Package Name:")) break;
      if (next.startsWith("CVE:")) {
        cve = clean(next.replace("CVE:", ""));
        inDescription = false;
      } else if (next.startsWith("Description:")) {
        inDescription = true;
        descParts.push(clean(next.replace("Description:", "")));
      } else if (next.startsWith("References:")) {
        inDescription = false;
      } else if (inDescription) {
        descParts.push(clean(next));
      }
    }
    const desc = clean(descParts.join(" "));
    const isFastTrack = packageName === "xlsx";
    addRow({
      scanner: "SCA",
      severity,
      vulnerability: `Dependencia ${packageName}-${version}`,
      packageName,
      version,
      category,
      cve,
      page: pageForIndex(offset),
      status: isFastTrack ? "EN PROCESO" : "PENDIENTE",
      notes: isFastTrack
        ? "Candidato para primer avance: actualización de dependencia acotada."
        : desc.slice(0, 180),
    });
    count += 1;
    offset += line.length + 1;
  }
  return count;
}

function parseContainerSection() {
  const sectionStart = "Container Security Scan Results (132)";
  const sectionEnd = "SAST Resolved Vulnerabilities";
  const start = text.indexOf(sectionStart);
  const end = text.indexOf(sectionEnd, start + sectionStart.length);
  if (start < 0 || end < 0) return 0;
  const section = text.slice(start, end);
  const lines = section.split(/\r?\n/);
  let count = 0;
  let current = null;
  let offset = start;
  for (const line of lines) {
    if (line.startsWith("Package Name:")) {
      current = {
        packageName: clean(line.replace("Package Name:", "")),
        severity: "",
        version: "",
        category: "",
        cve: "",
        offset,
      };
    } else if (current && line.startsWith("Severity:")) {
      current.severity = clean(line.replace("Severity:", "")).toUpperCase();
    } else if (current && line.startsWith("Version:")) {
      current.version = clean(line.replace("Version:", ""));
    } else if (current && line.startsWith("CWE:")) {
      current.category = clean(line.replace("CWE:", ""));
    } else if (current && line.startsWith("CVE:")) {
      current.cve = clean(line.replace("CVE:", ""));
      addRow({
        scanner: "CONTAINER",
        severity: current.severity,
        vulnerability: `Contenedor ${current.packageName}-${current.version}`,
        packageName: current.packageName,
        version: current.version,
        category: current.category,
        cve: current.cve,
        page: pageForIndex(current.offset),
        status: "PENDIENTE",
        notes: "Actualizar imagen/base y validar compatibilidad en despliegue.",
      });
      count += 1;
      current = null;
    }
    offset += line.length + 1;
  }
  return count;
}

const scaCount = parseScaSection();
const containerCount = parseContainerSection();

for (let i = 1; i <= 3; i += 1) {
  addRow({
    scanner: "SCS",
    severity: "HIGH",
    vulnerability: `Secret Detection ${i}/3`,
    packageName: "Configuración / secretos",
    category: "Secret Detection",
    page: 222,
    status: "EN PROCESO",
    notes: "Validar si corresponde a secreto real, rotar si aplica y retirar del repositorio/configuración.",
  });
}

rows.sort((a, b) => {
  const statusRank = { "EN PROCESO": 0, PENDIENTE: 1, RESUELTO: 2 };
  return (
    (statusRank[a.status] ?? 9) - (statusRank[b.status] ?? 9) ||
    severityRank(a.severity) - severityRank(b.severity) ||
    a.scanner.localeCompare(b.scanner) ||
    a.vulnerability.localeCompare(b.vulnerability)
  );
});

const pendingRows = rows.filter((row) => row.status !== "EN PROCESO");
let pendingIndex = 0;
rows.forEach((row) => {
  if (row.status === "EN PROCESO") {
    row.delivery = deliveries[0];
  } else {
    row.delivery = nextRegularDelivery(pendingIndex, pendingRows.length);
    pendingIndex += 1;
  }
});

const workbook = Workbook.create();
const comp = workbook.worksheets.add("COMPENDIO");
const critical = workbook.worksheets.add("CRITICAL");
const high = workbook.worksheets.add("HIGH");
const medium = workbook.worksheets.add("MEDIUM");
const entregas = workbook.worksheets.add("ENTREGAS");

const headers = [
  "ID",
  "Scanner",
  "Gravedad",
  "Vulnerabilidad",
  "Package Name",
  "Version",
  "Categoria",
  "CVE",
  "PaginaDF",
  "ESTADO",
  "Fecha tentativa",
  "Entrega",
  "Observaciones",
];

function styleTrackerSheet(sheet, title, rowsCount) {
  sheet.showGridLines = false;
  sheet.getRange("A2:M2").merge();
  sheet.getRange("A2").values = [[title]];
  sheet.getRange("A2").format = {
    font: { bold: true, fontSize: 12, color: "#000000" },
    horizontalAlignment: "center",
  };
  sheet.getRange("A4:M4").format = {
    fill: "#70AD47",
    font: { bold: true, color: "#000000" },
    horizontalAlignment: "center",
  };
  sheet.getRange(`A4:M${Math.max(5, rowsCount + 4)}`).format.borders = {
    preset: "all",
    style: "thin",
    color: "#70AD47",
  };
  sheet.getRange(`D5:D${Math.max(5, rowsCount + 4)}`).format.wrapText = true;
  sheet.getRange(`M5:M${Math.max(5, rowsCount + 4)}`).format.wrapText = true;
  sheet.getRange(`J5:J${Math.max(5, rowsCount + 4)}`).format.horizontalAlignment = "center";
  sheet.getRange(`K5:K${Math.max(5, rowsCount + 4)}`).setNumberFormat("yyyy-mm-dd");
  sheet.freezePanes.freezeRows(4);
  const widths = [13, 12, 12, 34, 38, 14, 18, 18, 10, 14, 16, 20, 52];
  widths.forEach((width, idx) => {
    sheet.getRangeByIndexes(0, idx, 1, 1).format.columnWidth = width;
  });
  const legendCol = "O";
  sheet.getRange(`${legendCol}3`).values = [["ESTATUS"]];
  sheet.getRange(`${legendCol}4:${legendCol}6`).values = [["RESUELTO"], ["EN PROCESO"], ["PENDIENTE"]];
  sheet.getRange(`${legendCol}3:${legendCol}6`).format = {
    font: { bold: true, color: "#000000" },
    horizontalAlignment: "center",
  };
  sheet.getRange(`${legendCol}4`).format.fill = "#A9D18E";
  sheet.getRange(`${legendCol}5`).format.fill = "#FFD966";
  sheet.getRange(`${legendCol}6`).format.fill = "#FF0000";
  sheet.getRange(`${legendCol}:${legendCol}`).format.columnWidth = 16;
}

function addStatusFormatting(sheet, lastRow) {
  const statusRange = sheet.getRange(`J5:J${lastRow}`);
  statusRange.conditionalFormats.add("containsText", {
    text: "RESUELTO",
    format: { fill: "#A9D18E", font: { color: "#000000" } },
  });
  statusRange.conditionalFormats.add("containsText", {
    text: "EN PROCESO",
    format: { fill: "#FFD966", font: { color: "#000000" } },
  });
  statusRange.conditionalFormats.add("containsText", {
    text: "PENDIENTE",
    format: { fill: "#FF0000", font: { color: "#000000" } },
  });
}

const matrix = [
  headers,
  ...rows.map((row) => [
    row.id,
    row.scanner,
    row.severity,
    row.vulnerability,
    row.packageName,
    row.version,
    row.category,
    row.cve,
    row.page,
    row.status,
    row.delivery,
    `Avance ${row.delivery.toISOString().slice(0, 10)}`,
    row.notes,
  ]),
];

comp.getRange(`A4:M${rows.length + 4}`).values = matrix;
styleTrackerSheet(comp, "COMPENDIO DE VULNERABILIDADES SIAJ", rows.length + 1);
addStatusFormatting(comp, rows.length + 4);
comp.getRange(`J5:J${rows.length + 4}`).dataValidation = {
  rule: { type: "list", values: ["RESUELTO", "EN PROCESO", "PENDIENTE"] },
};

function makeSeverityView(sheet, severity) {
  styleTrackerSheet(sheet, `VULNERABILIDAD ${severity}`, rows.length + 1);
  addStatusFormatting(sheet, rows.length + 4);
  sheet.getRange("A4:M4").values = [headers];
  sheet.getRange("A5").formulas = [
    [
      `=FILTER('COMPENDIO'!A5:M${rows.length + 4},'COMPENDIO'!C5:C${rows.length + 4}="${severity}","")`,
    ],
  ];
}

makeSeverityView(critical, "CRITICAL");
makeSeverityView(high, "HIGH");
makeSeverityView(medium, "MEDIUM");

const deliveryHeaders = ["Fecha", "Total", "Critical", "High", "Medium", "Resuelto", "En proceso", "Pendiente", "Objetivo"];
const deliveryRows = deliveries.map((date) => [
  date,
  `=COUNTIF('COMPENDIO'!K5:K${rows.length + 4},A${deliveries.indexOf(date) + 5})`,
  `=COUNTIFS('COMPENDIO'!K5:K${rows.length + 4},A${deliveries.indexOf(date) + 5},'COMPENDIO'!C5:C${rows.length + 4},"CRITICAL")`,
  `=COUNTIFS('COMPENDIO'!K5:K${rows.length + 4},A${deliveries.indexOf(date) + 5},'COMPENDIO'!C5:C${rows.length + 4},"HIGH")`,
  `=COUNTIFS('COMPENDIO'!K5:K${rows.length + 4},A${deliveries.indexOf(date) + 5},'COMPENDIO'!C5:C${rows.length + 4},"MEDIUM")`,
  `=COUNTIFS('COMPENDIO'!K5:K${rows.length + 4},A${deliveries.indexOf(date) + 5},'COMPENDIO'!J5:J${rows.length + 4},"RESUELTO")`,
  `=COUNTIFS('COMPENDIO'!K5:K${rows.length + 4},A${deliveries.indexOf(date) + 5},'COMPENDIO'!J5:J${rows.length + 4},"EN PROCESO")`,
  `=COUNTIFS('COMPENDIO'!K5:K${rows.length + 4},A${deliveries.indexOf(date) + 5},'COMPENDIO'!J5:J${rows.length + 4},"PENDIENTE")`,
  date.toISOString().slice(0, 10) === "2026-08-20"
    ? "Primer avance: secretos/configuración, xlsx, HSTS, Open Redirect y rutas de archivo."
    : "Avance mensual conforme a priorización.",
]);

entregas.showGridLines = false;
entregas.getRange("A2:I2").merge();
entregas.getRange("A2").values = [["CALENDARIO DE ENTREGAS - AVANCES CADA DIA 15"]];
entregas.getRange("A2").format = {
  font: { bold: true, fontSize: 12, color: "#000000" },
  horizontalAlignment: "center",
};
entregas.getRange(`A4:I${deliveries.length + 4}`).values = [deliveryHeaders, ...deliveryRows];
entregas.getRange("A4:I4").format = {
  fill: "#70AD47",
  font: { bold: true, color: "#000000" },
  horizontalAlignment: "center",
};
entregas.getRange(`A5:A${deliveries.length + 4}`).setNumberFormat("yyyy-mm-dd");
entregas.getRange(`B5:H${deliveries.length + 4}`).format.numberFormat = "#,##0";
entregas.getRange(`A4:I${deliveries.length + 4}`).format.borders = {
  preset: "all",
  style: "thin",
  color: "#70AD47",
};
entregas.getRange(`I5:I${deliveries.length + 4}`).format.wrapText = true;
[14, 10, 10, 10, 10, 12, 14, 12, 70].forEach((width, idx) => {
  entregas.getRangeByIndexes(0, idx, 1, 1).format.columnWidth = width;
});
entregas.freezePanes.freezeRows(4);

for (const sheet of [comp, critical, high, medium, entregas]) {
  const used = sheet.getUsedRange();
  used.format.font = { typeface: "Calibri", fontSize: 11 };
  used.format.autofitRows();
}

await fs.mkdir(outputDir, { recursive: true });

for (const sheetName of ["COMPENDIO", "HIGH", "MEDIUM", "CRITICAL", "ENTREGAS"]) {
  const preview = await workbook.render({ sheetName, autoCrop: "all", scale: 1, format: "png" });
  await fs.writeFile(`${outputDir}/preview_${sheetName}.png`, new Uint8Array(await preview.arrayBuffer()));
}

const check = await workbook.inspect({
  kind: "table",
  sheetId: "COMPENDIO",
  range: "A4:M18",
  include: "values,formulas",
  tableMaxRows: 15,
  tableMaxCols: 13,
  maxChars: 10000,
});
console.log(check.ndjson);

const errors = await workbook.inspect({
  kind: "match",
  searchTerm: "#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A",
  options: { useRegex: true, maxResults: 300 },
  summary: "final formula error scan",
});
console.log(errors.ndjson);

const output = await SpreadsheetFile.exportXlsx(workbook);
await output.save(outputPath);
console.log(JSON.stringify({ outputPath, rows: rows.length, scaCount, containerCount }, null, 2));
