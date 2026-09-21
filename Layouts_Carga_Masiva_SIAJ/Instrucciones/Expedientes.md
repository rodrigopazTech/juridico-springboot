# Expedientes

Complete una fila por expediente en `Layout_Expedientes.xlsx`. Todos los registros de un archivo deben compartir el mismo `gerencia_codigo`.

`clave_origen` identifica al expediente en los archivos relacionados. Es obligatoria, única y debe conservarse exactamente igual en las columnas `expediente_clave_origen` de Audiencias y Términos. `numero_expediente` es el número oficial mostrado en SIAJ y debe registrarse como texto.

Los campos `materia_codigo`, `tipo_expediente_codigo`, `prioridad_codigo`, `organo_codigo` y `sede_codigo` deben usar los códigos preparados en el layout de Catálogos. El abogado responsable se identifica por el correo de un usuario activo de la misma Gerencia y con acceso a la materia.

`partes` se captura como texto libre, por ejemplo: `Actor: María López; Demandado: Institución X`.
