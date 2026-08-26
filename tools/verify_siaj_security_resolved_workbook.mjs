import fs from "node:fs/promises";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const workbookPath = "outputs/vulnerabilidades_siaj/SIAJ_Vulnerabilidades_Atendidas_Formato_Seguridad_2026-08-18.xlsx";
const outputDir = "outputs/vulnerabilidades_siaj/previews_atendidas";

await fs.mkdir(outputDir, { recursive: true });

const input = await FileBlob.load(workbookPath);
const workbook = await SpreadsheetFile.importXlsx(input);

const summary = await workbook.inspect({
  kind: "workbook,sheet,table,region",
  maxChars: 12000,
  tableMaxRows: 12,
  tableMaxCols: 8,
  tableMaxCellChars: 100,
});
console.log(summary.ndjson);

const errors = await workbook.inspect({
  kind: "match",
  searchTerm: "#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A",
  options: { useRegex: true, maxResults: 100 },
  summary: "status and formula error scan",
});
console.log(errors.ndjson);

for (const sheet of workbook.worksheets.items) {
  const preview = await workbook.render({
    sheetName: sheet.name,
    range: "A1:G18",
    scale: 1,
    format: "png",
  });
  await fs.writeFile(
    `${outputDir}/${sheet.name.replace(/[\\/:*?"<>|]/g, "_")}.png`,
    new Uint8Array(await preview.arrayBuffer()),
  );
}
