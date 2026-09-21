# Layouts de Carga Masiva SIAJ

Esta carpeta contiene los layouts oficiales para la carga histórica controlada por Gerencia Jurídica.

Cada Gerencia prepara sus propios archivos. Los layouts operativos de Expedientes, Audiencias y Términos deben contener registros de una sola Gerencia. Dirección Jurídica y Subdirección Jurídica se cargan únicamente mediante el layout de Usuarios, con su rol y permisos; no cuentan con layouts operativos propios.

## Archivos de captura

| Archivo | Uso |
| --- | --- |
| `Layout_Catalogos.xlsx` | Códigos y catálogos requeridos para referencias posteriores. |
| `Layout_Usuarios.xlsx` | Usuarios, alcance por Gerencia, materias y permisos adicionales. |
| `Layout_Expedientes.xlsx` | Expedientes históricos de una Gerencia. |
| `Layout_Audiencias.xlsx` | Audiencias vinculadas a los expedientes del mismo bloque. |
| `Layout_Terminos.xlsx` | Términos vinculados a los expedientes del mismo bloque. |

Use primero los archivos de `Ejemplos/` como referencia visual. Para capturar, use los archivos `Layout_*.xlsx`, que están vacíos y contienen las hojas `CAPTURA`, `INSTRUCCIONES`, `DICCIONARIO` y `CATALOGOS`. En `Layout_Catalogos.xlsx` las hojas de captura se llaman `GERENCIAS`, `MATERIAS`, `PRIORIDADES`, `TIPOS_EXPEDIENTE`, `ORGANOS`, `TIPOS_AUDIENCIA` y `SEDES`.

## Orden de preparación

1. Catálogos.
2. Usuarios.
3. Expedientes.
4. Audiencias y términos.

No modifique encabezados. Fechas: `AAAA-MM-DD`; horas: `HH:mm`; booleanos: `true` o `false`. Los códigos y correos deben coincidir con los catálogos y usuarios definidos para la misma Gerencia.

Los nombres de archivo incluidos en campos `*_archivo` son referencias relativas para un paquete de adjuntos; no son rutas absolutas ni sustituyen la entrega del archivo binario correspondiente.
