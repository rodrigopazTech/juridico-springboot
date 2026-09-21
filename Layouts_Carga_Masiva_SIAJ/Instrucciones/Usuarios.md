# Usuarios

Complete una fila por usuario en la hoja `CAPTURA` de `Layout_Usuarios.xlsx`.

`nombre_completo`, `rol` y `activo` son obligatorios. `gerencia_codigo` es obligatorio para los roles GERENTE, JEFE_DEPTO y ABOGADO. Para ADMIN, DIRECTOR_PRINCIPAL, DIRECTOR y SUBDIRECTOR puede quedar vacío cuando no exista una Gerencia operativa asignada.

Use el código de Gerencia definido en `Layout_Catalogos.xlsx`. En `materias_codigos` y `permisos_adicionales` se pueden capturar varios valores separados por punto y coma, sin espacios antes o después del separador. Los usuarios de Dirección y Subdirección se incluyen aquí con su rol y permisos; no deben capturarse en layouts de expedientes, audiencias ni términos salvo que tengan una asignación operativa autorizada.

Email, username y número de empleado deben ser únicos cuando se capturen. No incluya contraseñas, hashes, tokens ni datos de autenticación de Microsoft.
