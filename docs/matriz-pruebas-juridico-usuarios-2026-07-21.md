# Matriz de pruebas SIAJ por rol

Fecha de elaboración: 21 de julio de 2026

## Indicaciones generales

- Cada usuario debe ingresar únicamente con su cuenta y probar exclusivamente las funciones de su rol.
- Si una opción no aparece en menú, botones o formularios, debe considerarse parte de la validación del permiso.
- En cada prueba se debe registrar: módulo, acción realizada, resultado esperado y observaciones.
- Si se detecta una incidencia, se recomienda adjuntar captura y describir los pasos exactos.

## Funciones disponibles identificadas en la revisión

- Expedientes con creación, edición, detalle, comentarios, cambio de etapa y exportación.
- Términos con creación, edición, avance, conclusión, carga y descarga de archivos.
- Términos por expediente y por asunto único.
- Audiencias con creación, edición, conclusión, carga y descarga de actas.
- Audiencias por expediente y por asunto único.
- Agenda, calendario, dashboard, tareas y notificaciones.
- Migración disponible solo para perfil `ADMIN`.
- Catálogos visibles para consulta general, con escritura restringida según rol.

## Matriz de pruebas

| Rol | Módulo | Prueba a ejecutar | Resultado esperado |
| --- | --- | --- | --- |
| ABOGADO | Acceso general | Ingresar al sistema con su usuario | Puede entrar y visualizar solo las opciones permitidas para su perfil |
| ABOGADO | Expedientes | Crear un expediente propio | El sistema permite guardarlo y lo asigna al mismo abogado como responsable |
| ABOGADO | Expedientes | Abrir expediente propio y revisar detalle | Puede consultar información, comentarios, documentos y trazabilidad del expediente |
| ABOGADO | Expedientes | Intentar consultar expediente ajeno no asignado | El sistema debe restringir el acceso si no existe asignación o colaboración válida |
| ABOGADO | Términos | Crear un término ligado a expediente permitido | El término se guarda correctamente y queda visible en su listado |
| ABOGADO | Términos | Crear un término como asunto único | El sistema permite capturarlo sin expediente formal, solicitando el contexto requerido |
| ABOGADO | Términos | Avanzar etapa de un término propio | El cambio de etapa se realiza correctamente dentro del flujo permitido |
| ABOGADO | Términos | Concluir término propio | El término puede concluirse y conservar su trazabilidad |
| ABOGADO | Audiencias | Crear audiencia ligada a expediente permitido | La audiencia se guarda correctamente |
| ABOGADO | Audiencias | Crear audiencia como asunto único | El sistema permite capturarla con datos de contexto sin expediente previo |
| ABOGADO | Audiencias | Concluir audiencia propia | La audiencia cambia a concluida correctamente |
| ABOGADO | Catálogos | Intentar modificar catálogos | No debe poder crear ni editar catálogos |
| ABOGADO | Agenda / Calendario | Revisar agenda y calendario personal | Debe ver sus asuntos y eventos dentro de su alcance |
| GERENTE | Acceso general | Ingresar al sistema con su usuario | Puede entrar y visualizar módulos operativos y administrativos autorizados |
| GERENTE | Expedientes | Consultar expedientes de su gerencia | Debe visualizar únicamente expedientes dentro de su ámbito |
| GERENTE | Términos | Consultar, revisar y dar seguimiento a términos de su gerencia | Puede revisar información dentro del alcance de su gerencia |
| GERENTE | Audiencias | Consultar, revisar y concluir audiencias dentro de su ámbito | Puede operar audiencias permitidas por su rol |
| GERENTE | Usuarios | Revisar usuarios del área | Puede acceder a la administración correspondiente a su alcance |
| GERENTE | Catálogos | Validar catálogos dentro de su alcance | Puede consultar y, en su caso, operar según la política vigente del sistema |
| JEFE_DEPTO | Acceso general | Ingresar al sistema con su usuario | Puede entrar y visualizar opciones correspondientes a su materia o departamento |
| JEFE_DEPTO | Expedientes | Consultar expedientes de sus materias | Solo debe ver expedientes dentro de su alcance funcional |
| JEFE_DEPTO | Términos | Revisar términos de sus materias | Debe operar dentro de su ámbito autorizado |
| JEFE_DEPTO | Audiencias | Revisar audiencias de sus materias | Debe operar dentro de su ámbito autorizado |
| JEFE_DEPTO | Usuarios | Validar alcance de usuarios bajo su supervisión | Debe ver únicamente el alcance permitido |
| ADMIN / Soporte | Migración | Ingresar al módulo de migración | El acceso debe estar habilitado solo para este perfil |
| ADMIN / Soporte | Migración | Descargar plantillas y revisar carga de archivos | Las plantillas deben descargarse correctamente y el módulo debe estar disponible |
| TODOS | Notificaciones | Revisar notificaciones propias | Cada usuario debe ver únicamente sus notificaciones |
| TODOS | Perfil | Abrir perfil y revisar datos del usuario | El perfil debe mostrar la información del usuario autenticado |
| TODOS | Responsive | Probar acceso desde pantalla móvil o ventana reducida | La navegación lateral y las pantallas principales deben mantenerse utilizables |

## Sugerencia de ejecución

1. Iniciar con pruebas del rol `ABOGADO`, por ser el flujo operativo principal.
2. Continuar con `GERENTE` y `JEFE_DEPTO` para validar supervisión y alcance.
3. Cerrar con `ADMIN / Soporte` para revisar migración y administración.
4. Registrar cualquier observación agrupándola por módulo: Expedientes, Términos, Audiencias, Catálogos, Agenda, Calendario, Notificaciones o Migración.

## Criterio esperado de retroalimentación

Se solicita que cada usuario reporte:

- Qué módulo probó.
- Qué acción realizó.
- Si el resultado fue correcto o no.
- Qué mensaje mostró el sistema, en caso de error.
- Si el comportamiento observado coincide con su forma real de trabajo.
