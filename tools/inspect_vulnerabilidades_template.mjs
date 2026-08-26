import fs from "node:fs/promises";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const templatePath = "C:/Users/Rodrigo.DESKTOP-2MLHEH3/Desktop/Entregables/VULNERABILIDADES.xlsx";
const outputDir = "tmp/spreadsheets";
await fs.mkdir(outputDir, { recursive: true });

const input = await FileBlob.load(templatePath);
const workbook = await SpreadsheetFile.importXlsx(input);

const summary = await workbook.inspect({
  kind: "workbook,sheet,table,region,computedStyle",
  maxChars: 12000,
  tableMaxRows: 20,
  tableMaxCols: 12,
  tableMaxCellChars: 120,
});
console.log(summary.ndjson);

for (const sheet of workbook.worksheets.items) {
  const preview = await workbook.render({
    sheetName: sheet.name,
    autoCrop: "all",
    scale: 2,
    format: "png",
  });
  await fs.writeFile(
    `${outputDir}/template_${sheet.name.replace(/[\\/:*?"<>|]/g, "_")}.png`,
    new Uint8Array(await preview.arrayBuffer()),
  );
}
