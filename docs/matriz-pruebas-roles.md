# 🧪 Matriz de pruebas por rol y pantalla

Fecha de inicio: 2026-07-21  
Última actualización: 2026-07-21  
Ambiente probado: `http://localhost:8080`

## 🧭 Cómo revisar este Markdown

Lee el archivo en este orden:

1. **🚦 Estado ejecutivo**: confirma si la versión tiene bloqueadores.
2. **🟠 Decisiones pendientes**: puntos donde necesito criterio de negocio.
3. **🟡 Pendiente por ejecutar**: pruebas que todavía faltan completar.
4. **✅ Validado**: lo que ya fue probado y pasó.
5. **🧯 Discrepancias corregidas**: errores encontrados y cómo quedaron.

## 🎨 Leyenda visual

| Marca | Estado | Qué significa |
| --- | --- | --- |
| ✅ | OK | Probado y funciona como se esperaba |
| 🟢 | Corregido | Falló antes, se corrigió y se volvió a validar |
| 🟡 | Pendiente | Falta ejecutar la prueba o completar evidencia |
| 🟠 | Decisión | Requiere confirmación de negocio |
| 🔴 | Crítico | No subir a usuarios reales hasta corregir |
| ⚫ | N/A | No aplica para el rol o pantalla |

## 🚦 Estado ejecutivo

| Estado | Lectura rápida |
| --- | --- |
| 🟢 Sin bloqueadores críticos abiertos | Los 4 hallazgos críticos/altos detectados ya fueron corregidos y revalidados |
| 🟡 Aún faltan pruebas funcionales | Faltan flujos completos de crear, concluir, descargar y validar notificaciones |
| 🟡 Queda una aclaración pendiente | Solo falta que me compartas o autorices generar el archivo controlado para probar Migración |
| ✅ App compila | `mvnw.cmd compile` pasó correctamente después de los fixes |

## 🟠 Decisiones y criterios acordados

| ID | Tema | Decisión pendiente | Recomendación |
| --- | --- | --- | --- |
| DEC-001 | Expedientes - ABOGADO | ✅ Decidido: el ABOGADO sí puede crear expedientes | Probar creación real como ABOGADO y confirmar que queda como responsable |
| DEC-002 | Catálogos | ✅ Decidido: seguir recomendación | ABOGADO solo lectura; GERENTE y JEFE_DEPTO solo modifican catálogos dentro de su alcance |
| DEC-003 | Migración | 🟠 Aclarado: el archivo es un Excel/CSV de muestra para importación | Falta definir si usaré uno real anonimizado o genero uno pequeño con datos ficticios controlados |
| DEC-004 | Perfil - contraseña | ✅ Decidido: seguir recomendación | Cambiar contraseñas solo de usuarios temporales QA |
| DEC-005 | Notificaciones | ✅ Decidido: seguir recomendación | Validar en BD y visualmente en navegador por rol |

## 🟡 Pendiente por ejecutar

No son errores abiertos; son pruebas funcionales que todavía faltan completar como usuario real.

| Prioridad | ID | Pantalla | Prueba pendiente | Motivo |
| --- | --- | --- | --- | --- |
| 🟡 Alta | EXP-ALL-002 | Expedientes | Crear expediente dentro del alcance por rol permitido | Flujo principal de negocio |
| 🟡 Alta | EXP-ABOG-001 | Expedientes | Crear expediente como ABOGADO | Decidido: debe estar permitido |
| 🟡 Alta | EXP-ALL-003 | Expedientes | Abrir detalle, comentarios y gestor documental | Flujo diario |
| 🟡 Alta | TER-ALL-002 | Términos | Crear término para expediente permitido | Flujo principal |
| 🟡 Alta | TER-ALL-004 | Términos | Avanzar término permitido | Flujo principal |
| 🟡 Alta | TER-ALL-005 | Términos | Concluir término permitido | Flujo principal |
| 🟡 Alta | AUD-ALL-002 | Audiencias | Crear audiencia para expediente permitido | Flujo principal |
| 🟡 Alta | AUD-ALL-004 | Audiencias | Concluir audiencia permitida | Flujo principal |
| 🟡 Media | CAT-ADMIN-001 | Catálogos | Crear, editar y activar/desactivar gerencia como ADMIN | Validar escritura autorizada |
| 🟡 Media | CAT-GER-001 | Catálogos | Crear, editar y activar/desactivar dentro de alcance como GERENTE | Validar alcance |
| 🟡 Media | CAT-JEFE-001 | Catálogos | Crear, editar y activar/desactivar dentro de alcance como JEFE_DEPTO | Validar alcance |
| 🟡 Media | MIG-ADMIN-001 | Migración | Abrir pantalla como ADMIN | Confirmar acceso exclusivo |
| 🟡 Media | MIG-ADMIN-002 | Migración | Descargar plantillas | Validar archivos |
| 🟡 Media | MIG-NOADMIN-* | Migración | Confirmar bloqueo por URL para no ADMIN | Regresión de permisos |
| 🟡 Media | PERF-ALL-002 | Perfil | Guardar preferencias | Flujo secundario |
| 🟡 Media | NOTIF-ALL-002 | Notificaciones | Leer, archivar y restaurar notificación propia | Flujo secundario |
| 🟡 Baja | DASH-ALL-002 | Dashboard | Cambiar filtro de gerencia | Validar filtros visuales |
| 🟡 Baja | CAL-ALL-002 | Calendario | Cambiar rango o vista | Validar UX |

## ✅ Validado por rol y acceso

| Bloque | Resultado |
| --- | --- |
| ✅ Login por rol | `ADMIN`, `DIRECTOR_PRINCIPAL`, `DIRECTOR`, `SUBDIRECTOR`, `GERENTE`, `JEFE_DEPTO`, `ABOGADO` entran con credenciales de prueba |
| ✅ Navbar/sidebar | El menú visible coincide con permisos generales por rol |
| ✅ `/usuarios` | Disponible para todos menos `ABOGADO`; `ABOGADO` recibe `403` |
| ✅ `/usuarios-admin` | Eliminado; responde `404` |
| ✅ `/abogados` | Eliminado; responde `404` |
| ✅ `/admin/importar` | Solo `ADMIN`; no admin recibe `403` |
| ✅ Agenda | Todos pueden abrir `/agenda` |
| ✅ Catálogos lectura | Todos pueden abrir `/catalogos` |
| ✅ Expedientes listado | Todos cargan `/expedientes`; corregido error JS en ABOGADO |
| ✅ Términos listado | Todos cargan `/terminos` |
| ✅ Audiencias listado | Todos cargan `/audiencias` |
| ✅ Perfil | `/perfil` carga datos del usuario autenticado |
| ✅ Notificaciones | `/notificaciones` carga sin error principal |

## ✅ Flujos ya probados

| ID | Flujo | Resultado |
| --- | --- | --- |
| TAR-ALL-002 | Crear tarea propia por todos los roles | ✅ OK |
| TAR-ALL-003 | Completar tarea propia por todos los roles | ✅ OK |
| TAR-ALL-004 | Eliminar tarea propia por todos los roles | ✅ OK |
| TER-ALL-006 | Eliminar término permitido por ADMIN, directivos, GERENTE y JEFE_DEPTO | ✅ OK |
| TER-ABOG-001 | ABOGADO responsable elimina término de su expediente | ✅ OK |
| TER-ABOG-002 | ABOGADO no responsable intenta eliminar término ajeno | ✅ Bloqueado correctamente |
| AUD-ALL-005 | Eliminar audiencia permitida por ADMIN, directivos, GERENTE y JEFE_DEPTO | ✅ OK |
| AUD-ABOG-001 | ABOGADO responsable elimina audiencia de su expediente | ✅ OK |
| AUD-ABOG-002 | ABOGADO no responsable intenta eliminar audiencia ajena | ✅ Bloqueado correctamente |
| USR-GER-002 | GERENTE crea usuario sin poder conservar materias fuera de su gerencia | 🟢 Corregido y OK |
| CAT-ABOG-001 | ABOGADO intenta escribir en Catálogos por POST directo | 🟢 Corregido; responde `403` |
| EXP-ABOG-001 | ABOGADO crea expediente | ✅ OK; queda en su gerencia y como responsable |
| CAT-GER-001 | GERENTE crea materia manipulando `gerenciaId` fuera de alcance | ✅ OK; se fuerza a su gerencia real |
| CAT-JEFE-001 | JEFE_DEPTO crea materia manipulando `gerenciaId` fuera de alcance | ✅ OK; se fuerza a su gerencia real |
| CAT-ADMIN-001 | ADMIN crea materia en gerencia seleccionada | ✅ OK; respeta la gerencia indicada |
| CAT-URL-001 | ABOGADO intenta `POST`/`toggle` directo en Catálogos | ✅ OK; responde `403` |
| CAT-URL-002 | GERENTE intenta modificar gerencia o toggle global fuera de alcance | ✅ OK; responde `403` |

## 🧯 Discrepancias corregidas

Todas las discrepancias críticas o altas detectadas hasta ahora están corregidas y revalidadas.

| ID | Severidad | Pantalla | Problema detectado | Estado |
| --- | --- | --- | --- | --- |
| DISC-001 | Alta | Expedientes | ABOGADO recibía error JS porque Expedientes llamaba un endpoint de Usuarios bloqueado para ABOGADO | 🟢 Corregido y validado |
| DISC-002 | Crítica | Mis Tareas | Un ABOGADO podía eliminar por URL directa una tarea de ADMIN | 🟢 Corregido y validado |
| DISC-003 | Alta | Usuarios | GERENTE podía crear usuario con materia fuera de su gerencia manipulando el POST | 🟢 Corregido y validado |
| DISC-004 | Crítica | Catálogos | ABOGADO podía crear gerencia por POST directo | 🟢 Corregido y validado |

## 👤 Roles usados en pruebas

| Rol | Uso en matriz |
| --- | --- |
| `ADMIN` | Acceso total, migración y administración general |
| `DIRECTOR_PRINCIPAL` | Acceso directivo amplio sin migración |
| `DIRECTOR` | Acceso directivo |
| `SUBDIRECTOR` | Acceso directivo operativo |
| `GERENTE` | Acceso limitado por gerencia |
| `JEFE_DEPTO` | Acceso limitado por materia/departamento |
| `ABOGADO` | Acceso operativo a sus expedientes, términos y audiencias |

## 📌 Datos prácticos disponibles

| Archivo | Para qué sirve |
| --- | --- |
| `qa-page-smoke-results-after-fix.json` | Evidencia de carga de pantallas por rol después de correcciones |
| `qa-tareas-crud-results-after-fix.json` | Evidencia de CRUD de tareas propias |
| `qa-tareas-cross-user-after-fix.json` | Evidencia de bloqueo de tarea ajena |
| `qa-catalogos-write-results-after-fix.json` | Evidencia de bloqueo de escritura en Catálogos para ABOGADO |
| `qa-usuarios-create-results-after-fix.json` | Evidencia de alcance corregido al crear usuarios como GERENTE |
| `qa-catalogos-scope-results-after-decisions.json` | Evidencia de alcance de Catálogos por GERENTE, JEFE_DEPTO y ADMIN |
| `qa-role-smoke-results-after-decisions.json` | Evidencia de navbar/rutas por rol después de los cambios |
| `qa-page-smoke-results-after-decisions.json` | Evidencia de carga de pantallas por rol después de los cambios |

## 📋 Siguiente ronda sugerida

1. Ejecutar primero los pendientes de prioridad alta.
2. Ejecutar Catálogos con alcance por URL directa y por pantalla.
3. Ejecutar Migración cuando exista archivo controlado de prueba.
4. Repetir una pasada de navbar + URL directa por rol.
5. Marcar cada fila con ✅, 🟢, 🟡, 🟠 o 🔴 según resultado.

## 🚀 Criterio para subir a pruebas con usuarios reales

La versión ya no tiene bloqueadores críticos conocidos después de los fixes aplicados. Para una prueba controlada interna puede usarse, pero antes de abrirla a usuarios reales conviene cerrar los pendientes de prioridad alta y definir el archivo controlado de Migración.
