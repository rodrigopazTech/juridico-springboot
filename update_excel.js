const ExcelJS = require('exceljs');

async function updateExcel() {
  const workbook = new ExcelJS.Workbook();
  const filePath = 'archivos/Correo/R9 (DAF-DSFOS-DAJ).xlsx';
  
  await workbook.xlsx.readFile(filePath);
  const sheet = workbook.worksheets[0];
  
  // Row 14 is the row for 9.1
  const row14 = sheet.getRow(14);
  
  // Col 7: DESCRIPCIÓN CUALITATIVA
  row14.getCell(7).value = 'A la fecha el porcentaje de avance general se estima en 85%. Se completaron las vistas y la lógica de negocio para: 1) Registro de Expedientes y etiquetas, 2) Tablero de Términos Procesales con semaforización, 3) Control de Audiencias, y 4) Módulo de Inteligencia de Negocio. Asimismo se implementaron con éxito los controles de seguridad, roles (RBAC) y bitácora de auditoría.';
  
  // Col 11: % DE AVANCE 4TO TRIMESTRE
  row14.getCell(11).value = 0.85; 
  // Col 12: % AVANCE TOTAL ACUMULADO
  row14.getCell(12).value = 0.85;

  row14.commit();
  
  await workbook.xlsx.writeFile(filePath);
  console.log('Archivo Excel actualizado correctamente.');
}

updateExcel().catch(console.error);
