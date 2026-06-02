# Backlog: Semaforos y notificaciones configurables

## Objetivo

Permitir que los recordatorios automaticos y los colores de proximidad de `Terminos` y
`Audiencias` dejen de depender de dias fijos en codigo. La configuracion debe soportar:

- Valores predeterminados del sistema.
- Politicas obligatorias o ajustables por gerencia.
- Preferencias personales.
- Excepciones para un termino o audiencia concretos.
- Separacion entre el color del semaforo y el envio de notificaciones.

## Alcances y herencia

| Nivel | Quien lo administra | Uso recomendado |
|---|---|---|
| `SISTEMA` | Administrador | Valores iniciales y avisos minimos obligatorios |
| `GERENCIA` | Gerente o administrador | Politica comun del area |
| `USUARIO` | Usuario autenticado | Avisos personales adicionales o reemplazo permitido |
| `ASUNTO` | Responsable autorizado | Excepcion para un termino o audiencia concretos |

Orden de precedencia para reglas ajustables:

`ASUNTO > USUARIO > GERENCIA > SISTEMA > valores historicos`

Las reglas marcadas como obligatorias se acumulan y no pueden ser eliminadas por un
nivel inferior.

## Comportamiento historico conservado

Mientras no existan configuraciones guardadas, el sistema mantiene estos avisos:

| Evento | Dias de anticipacion |
|---|---|
| Termino por vencer | `3`, `1`, `0` |
| Audiencia proxima | `3`, `1`, `0` |

## Backlog

| ID | Issue | Prioridad | Estado | Criterio principal |
|---|---|---|---|---|
| `CFG-001` | Documentar reglas actuales y niveles de configuracion | P0 | Completado | Backlog versionado y valores heredados identificados |
| `CFG-002` | Crear modelo persistente de reglas de alerta | P0 | Completado | Una fila representa un dia de anticipacion y su alcance |
| `CFG-003` | Implementar resolvedor de herencia de alertas | P0 | Completado | Aplica precedencia y conserva reglas obligatorias |
| `CFG-004` | Sustituir dias fijos del scheduler de terminos | P0 | Completado | El scheduler consulta reglas y conserva `3`, `1`, `0` por defecto |
| `CFG-005` | Sustituir dias fijos del scheduler de audiencias | P0 | Completado | El scheduler consulta reglas y conserva `3`, `1`, `0` por defecto |
| `CFG-006` | Crear endpoints administrativos para reglas de gerencia | P0 | Completado | CRUD restringido para reglas `SISTEMA` y `GERENCIA` |
| `CFG-007` | Crear endpoints de preferencias personales | P0 | Completado | Cada usuario administra sus reglas `USUARIO` |
| `CFG-008` | Crear endpoints de excepciones por asunto | P0 | Completado | Responsable autorizado administra reglas `ASUNTO` |
| `CFG-009` | Crear modelo independiente para rangos de semaforo | P0 | Completado | Los puntos de corte de colores no se mezclan con avisos |
| `CFG-010` | Aplicar semaforos configurables en terminos | P1 | Completado | La tabla de terminos deja de usar cortes fijos `3` y `7` |
| `CFG-011` | Aplicar semaforos configurables en audiencias | P1 | Pendiente | La tabla de audiencias deja de usar cortes fijos `1` y `3` |
| `CFG-012` | Aplicar semaforos configurables en dashboard | P1 | Pendiente | Las metricas usan los mismos rangos que las pantallas |
| `CFG-013` | Crear pantalla administrativa por gerencia | P1 | Pendiente | Gerencia configura avisos y colores desde UI |
| `CFG-014` | Extender pantalla de perfil | P1 | Pendiente | Usuario configura sus dias de aviso y visualiza obligatorios |
| `CFG-015` | Agregar seccion de alertas en detalle del asunto | P1 | Pendiente | Se visualiza origen y excepciones aplicables |
| `CFG-016` | Validar permisos y consistencia de rangos | P0 | Pendiente | No hay dias negativos, duplicados ni rangos de color invalidos |
| `CFG-017` | Registrar auditoria de cambios | P1 | Pendiente | Guarda actor, nivel, valor anterior y valor nuevo |
| `CFG-018` | Garantizar idempotencia diaria | P0 | Pendiente | Reejecutar scheduler no duplica una alerta ya emitida |
| `CFG-019` | Agregar migracion y datos semilla | P1 | Pendiente | Despliegue crea reglas iniciales explicitas |
| `CFG-020` | Agregar pruebas unitarias e integracion | P0 | Pendiente | Cubre herencia, obligatorios, schedulers y permisos |

## Primer corte implementado

El primer corte cubre `CFG-001` a `CFG-005`. Incluye:

- Enum de tipo de evento: `TERMINO`, `AUDIENCIA`.
- Enum de nivel: `SISTEMA`, `GERENCIA`, `USUARIO`, `ASUNTO`.
- Entidad `ConfiguracionAlerta`.
- Repositorio y servicio resolvedor.
- Integracion de los schedulers existentes con reglas configurables.

Este corte habilita configuracion mediante datos persistidos, pero todavia no expone
formularios ni endpoints para modificarlos desde la aplicacion. Esa capacidad comienza
en `CFG-006`.

## Segundo corte implementado

El segundo corte cubre `CFG-006` a `CFG-010`. Incluye:

- API administrativa para reglas `SISTEMA` y `GERENCIA`.
- API de preferencias personales `USUARIO`.
- API de excepciones para un termino o audiencia especificos.
- Entidad independiente `ConfiguracionSemaforo`.
- Rangos visuales de terminos configurables con herencia `GERENCIA > SISTEMA > historico`.

La API permite enviar una coleccion vacia de dias para eliminar una configuracion y
volver a heredar el nivel superior. Las pantallas para consumir estos endpoints se
implementan en `CFG-013`, `CFG-014` y `CFG-015`.

## Decisiones pendientes de producto

Antes de implementar las pantallas se debe confirmar:

- Si un usuario puede reemplazar la politica de su gerencia o solamente agregar avisos.
- Si el nivel `ASUNTO` se captura por termino/audiencia individual o tambien por expediente.
- Que roles pueden editar reglas obligatorias.
- Si el aviso del dia `0` debe ser siempre obligatorio.
- Si las notificaciones futuras incluiran correo o solamente el centro de alertas interno.
