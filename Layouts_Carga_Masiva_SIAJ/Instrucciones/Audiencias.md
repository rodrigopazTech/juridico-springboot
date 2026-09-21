# Audiencias

Complete una fila por audiencia en `Layout_Audiencias.xlsx`. `clave_origen`, `gerencia_codigo`, `expediente_clave_origen`, `tipo_audiencia_codigo`, `fecha_audiencia`, `hora_audiencia`, `estatus_audiencia` y `es_virtual` son obligatorios.

La Gerencia y el expediente deben coincidir con el bloque de Expedientes. Si `es_virtual` es `true`, capture `url_reunion`. Si el estado es `CONCLUIDA`, capture `fecha_desahogo`. Si el estado es `CON_ACTA`, capture `acta_archivo` y entregue el archivo en el paquete de adjuntos con ese nombre relativo.

Use `HH:mm` en `hora_audiencia` y no capture rutas locales en campos de archivo.
