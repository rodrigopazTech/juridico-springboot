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
| 🟠 Hay decisiones de negocio pendientes | Principalmente permisos finos en Expedientes, Catálogos, Migración y Notificaciones |
| ✅ App compila | `mvnw.cmd compile` pasó correctamente después de los fixes |

## 🟠 Decisiones pendientes que debes leer

| ID | Tema | Decisión pendiente | Recomendación |
| --- | --- | --- | --- |
| DEC-001 | Expedientes - ABOGADO | Confirmar si el ABOGADO puede crear expedientes o solo trabajar expedientes asignados | Si no hay regla explícita, dejar creación solo para JEFE_DEPTO hacia arriba |
| DEC-002 | Catálogos | Confirmar quién puede crear, editar o activar/desactivar catálogos | Mantener ABOGADO solo lectura; GERENTE y JEFE_DEPTO solo dentro de su alcance |
| DEC-003 | Migración | Confirmar si la prueba debe usar archivo real de ejemplo o solo pantalla/plantillas | Para QA real conviene usar un archivo pequeño y controlado |
| DEC-004 | Perfil - contraseña | Confirmar si puedo cambiar contraseñas durante pruebas | Usar solo usuarios temporales QA para no romper credenciales base |
| DEC-005 | Notificaciones | Confirmar si basta validar en BD o también visualmente por navegador | Recomiendo ambas: BD + navegador por rol |

## 🟡 Pendiente por ejecutar

No son errores abiertos; son pruebas funcionales que todavía faltan completar como usuario real.

| Prioridad | ID | Pantalla | Prueba pendiente | Motivo |
| --- | --- | --- | --- | --- |
| 🟡 Alta | EXP-ALL-002 | Expedientes | Crear expediente dentro del alcance por rol permitido | Flujo principal de negocio |
| 🟡 Alta | EXP-ABOG-001 | Expedientes | Intentar creación como ABOGADO | Depende de DEC-001 |
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

## 📋 Siguiente ronda sugerida

1. Cerrar las decisiones `DEC-001` a `DEC-005`.
2. Ejecutar primero los pendientes de prioridad alta.
3. Ejecutar después Catálogos y Migración.
4. Repetir una pasada de navbar + URL directa por rol.
5. Marcar cada fila con ✅, 🟢, 🟡, 🟠 o 🔴 según resultado.

## 🚀 Criterio para subir a pruebas con usuarios reales

La versión ya no tiene bloqueadores críticos conocidos después de los fixes aplicados. Para una prueba controlada interna puede usarse, pero antes de abrirla a usuarios reales conviene cerrar los pendientes de prioridad alta y tomar las decisiones marcadas con 🟠.
