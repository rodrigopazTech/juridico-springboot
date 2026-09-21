# SIAJ — Modelo funcional definitivo de plazos, alertas y actuaciones sin expediente

**Fecha:** 10 de septiembre de 2026  
**Alcance:** Tipos de Término, Tipos de Audiencia, plazos, vencimientos, alertas, semáforos y registros sin expediente  
**Objetivo:** establecer un modelo simple, intuitivo y mantenible, alineado con la operación jurídica real de SIAJ

## Resumen de decisiones

1. **“Asunto Único” se sustituye por “Asunto independiente”.**
2. **Asunto independiente será una entidad compartida**, pero no tendrá un módulo principal propio.
3. Un Término o Audiencia tendrá exactamente uno de estos contextos: **Expediente** o **Asunto independiente**.
4. Desde Nuevo Término y Nueva Audiencia **nunca se crea un Expediente**. Sólo se selecciona uno existente o se registra un Asunto independiente.
5. El flujo visible de Términos se conserva: **Proyectista → Revisión → Gerencia → Dirección → Liberado → Presentado → Concluido**.
6. Internamente, el Término separará **etapa de trabajo**, **estado operativo** y **estado del acuse**.
7. El flujo de Audiencias cambia a **Pendiente → Resultado registrado → Concluida**.
8. **Con Acta deja de ser estado principal** y pasa a ser un estado documental.
9. En Tipos de Término se reemplaza “fijo/dinámico” por términos más precisos: **Calculado por el sistema** y **Capturado al registrar**.
10. **Semáforos Procesales deja de existir como catálogo.**
11. El semáforo será un valor derivado: verde, amarillo, rojo, vencido, cerrado o sin cálculo.
12. Los umbrales se administrarán mediante **Perfiles de alerta**, dentro de Administración → Plazos y alertas.
13. Para plazos calculados se usarán umbrales de **días restantes**.
14. Para plazos capturados se usará un modelo de **porcentaje restante con límites absolutos**.
15. Los usuarios operativos siempre verán **fecha, días restantes y explicación**; los porcentajes sólo serán visibles al administrador que configura la regla.
16. Al registrar la presentación de un Término se detiene el riesgo de vencimiento. La validación y conclusión continúan como control administrativo/documental.
17. Todo Término conservará una fotografía de la regla, calendario y umbrales usados, aunque la configuración cambie después.

---

# 1. Modelo funcional definitivo

## 1.1 Mapa conceptual

```text
                         CONTEXTO DE LA ACTUACIÓN
                    ┌──────────────────────────────┐
                    │ exactamente una vinculación │
                    └──────────────┬───────────────┘
                                   │
                     ┌─────────────┴─────────────┐
                     ▼                           ▼
             Expediente existente       Asunto independiente
                     │                           │
                     └─────────────┬─────────────┘
                                   ▼
                          Término o Audiencia

Término
  ├─ Tipo de Término ──► regla de cálculo del plazo
  ├─ Perfil de alerta ─► umbrales de atención
  ├─ Etapa de trabajo
  ├─ Estado operativo
  └─ Estado del acuse

Audiencia
  ├─ Tipo de Audiencia ─────► requisitos de captura/cierre
  ├─ Perfil de recordatorio ─► proximidad de la audiencia
  ├─ Estado operativo
  ├─ Resultado
  └─ Estado del acta
```

## 1.2 Tipo de Término

### Responsabilidad

Definir qué clase de obligación se está registrando, cómo se obtiene su fecha de vencimiento y qué controles documentales requiere.

### Modos permitidos de plazo

| Nombre funcional | Código sugerido | Comportamiento |
|---|---|---|
| Calculado por el sistema | `CALCULADO` | El tipo contiene cantidad y clase de días; el usuario captura la fecha origen y SIAJ calcula el vencimiento. |
| Capturado al registrar | `CAPTURADO` | El usuario captura fecha origen y fecha vencimiento; SIAJ calcula la duración total y aplica un perfil proporcional. |
| Sin vencimiento | `SIN_VENCIMIENTO` | Uso excepcional para controles sin fecha límite. No genera semáforo de plazo. |

En la interfaz administrativa puede conservarse la ayuda “plazo fijo” y “plazo variable”, pero esos términos no deben ser los nombres del modelo:

- **Calculado por el sistema** comunica mejor la acción de SIAJ.
- **Capturado al registrar** comunica mejor la responsabilidad del usuario.
- “Dinámico” puede confundirse con una fecha que cambia automáticamente.

### Datos definitivos del Tipo de Término

| Campo | Obligatorio | Regla |
|---|---:|---|
| Código | Sí | Estable y único; no cambia al renombrar. |
| Nombre | Sí | Etiqueta jurídica para captura y consulta. |
| Descripción/instrucción | Sí | Explica cuándo utilizar el tipo. |
| Materia/alcance | No | Global o limitado a materias autorizadas. |
| Modo de plazo | Sí | Calculado, Capturado o Sin vencimiento. |
| Cantidad de días | Sólo Calculado | Entero mayor a cero. |
| Tipo de días | Calculado/Capturado | Naturales o hábiles; en Capturado se usa para medir duración y días restantes. |
| Calendario | Si son hábiles | Calendario institucional aplicable. |
| Regla del día origen | Sí | En SIAJ 2.0 será fija: el día origen no cuenta. |
| Ajuste por día inhábil | Sí | En SIAJ 2.0 será fijo: mover al siguiente día hábil. |
| Perfil de alerta | Si tiene vencimiento | Perfil absoluto o proporcional, compatible con el modo. |
| Requiere documento liberado | Sí | Normalmente verdadero. |
| Requiere acuse para presentar | Sí | Normalmente verdadero. |
| Permite excepción sin acuse | Sí | Define si un autorizado puede justificarla. |
| Requiere validar acuse para concluir | Sí | Normalmente verdadero. |
| Estado | Sí | Borrador, Vigente o Inactivo. |
| Vigencia desde/hasta | Sí/No | Controla uso futuro, no modifica registros existentes. |

### Lo que no pertenece al Tipo de Término

- Fechas concretas de un Término.
- Color actual.
- Días restantes actuales.
- Responsable.
- Etapa actual del flujo.
- Observaciones de presentación.
- Umbrales copiados como columnas rojo/amarillo si ya existe un Perfil de alerta.
- Variantes de flujo por cada tipo durante la primera versión.

## 1.3 Flujo definitivo de Términos

### Decisión

El flujo institucional se conserva completo porque representa responsabilidades reales y puntos de control reconocibles:

```text
PROYECTISTA
    │
    ▼
REVISIÓN
    │
    ▼
GERENCIA
    │
    ▼
DIRECCIÓN
    │
    ▼
LIBERADO
    │ Registrar presentación + fecha + acuse/justificación
    ▼
PRESENTADO
    │ Validar acuse + observaciones finales
    ▼
CONCLUIDO
```

No se implementará como una sola columna que mezcla conceptos. Se conservará como una secuencia visible, pero se dividirá internamente:

| Dimensión | Valores | Propósito |
|---|---|---|
| Etapa de trabajo | Proyectista, Revisión, Gerencia, Dirección, Liberado | Indica dónde está la preparación y aprobación del documento. |
| Estado operativo | Activo, Presentado, Concluido, Cancelado, Anulado | Indica el ciclo de vida jurídico-administrativo. |
| Estado del acuse | No aplica, Pendiente, Adjunto, Validado, Excepción autorizada | Indica si existe y fue revisada la evidencia de presentación. |
| Condición de cumplimiento | No determinado, En tiempo, Fuera de plazo | Se fija al registrar la presentación y no cambia después. |
| Atención temporal | Sin cálculo, Verde, Amarillo, Rojo, Vencido, Cerrado | Valor derivado; nunca se captura. |

### Mapeo para la interfaz

| Paso visible | Estado operativo | Etapa de trabajo | Estado documental esperado |
|---|---|---|---|
| Proyectista | Activo | Proyectista | Documento en elaboración. |
| Revisión | Activo | Revisión | Documento en revisión. |
| Gerencia | Activo | Gerencia | Validación de Gerencia. |
| Dirección | Activo | Dirección | Validación de Dirección. |
| Liberado | Activo | Liberado | Documento final aprobado. |
| Presentado | Presentado | Última etapa conservada en historial | Acuse adjunto o excepción autorizada. |
| Concluido | Concluido | Última etapa conservada en historial | Acuse validado o excepción autorizada. |

### Reglas de transición

- Sólo se puede avanzar a Liberado si el documento requerido está disponible.
- “Registrar presentación” es una acción, no un simple cambio de select.
- Esta acción captura fecha/hora de presentación, medio, folio o referencia, observaciones y acuse.
- Si el tipo exige acuse, no se permite Presentado sin archivo, salvo permiso de excepción y motivo obligatorio.
- Al pasar a Presentado, se calcula si se cumplió en tiempo y se detienen alertas de vencimiento.
- Concluir requiere acuse validado o excepción autorizada, además de observaciones finales cuando sean necesarias.
- Volver de Presentado a Activo es una reapertura, exige permiso y motivo.
- Cancelado significa que la obligación dejó de proceder; Anulado significa que el registro fue inválido. Ninguno elimina datos.

### Separación de observaciones

| Campo | Momento | Contenido |
|---|---|---|
| Observaciones de trabajo | Etapas Proyectista–Liberado | Comentarios de elaboración o revisión. |
| Observaciones de presentación | Acción Registrar presentación | Incidencias del envío, recepción, folio o acuse. |
| Observaciones de conclusión | Acción Concluir | Resultado final, validación o aclaración de cierre. |

## 1.4 Tipo de Audiencia

### Responsabilidad

Definir la clase de evento, los datos requeridos para programarlo, la necesidad de acta y los recordatorios aplicables.

### Datos definitivos del Tipo de Audiencia

| Campo | Obligatorio | Regla |
|---|---:|---|
| Código | Sí | Único y estable. |
| Nombre | Sí | Etiqueta usada en agenda y reportes. |
| Descripción/instrucción | Sí | Explica cuándo utilizarlo. |
| Materia/alcance | No | Global o restringido por Materia. |
| Duración predeterminada | No | El usuario puede modificarla al programar. |
| Modalidades permitidas | Sí | Presencial, virtual y/o híbrida. |
| Modalidad predeterminada | No | Debe pertenecer a las permitidas. |
| Requiere acta | Sí | Control documental de cierre. |
| Permite excepción sin acta | Sí | Sólo para tipos jurídicamente aplicables. |
| Requiere validar acta para concluir | Sí | Normalmente verdadero cuando requiere acta. |
| Perfil de recordatorio | Sí | Perfil absoluto de proximidad; no usa porcentajes. |
| Estado | Sí | Borrador, Vigente o Inactivo. |
| Vigencia | Sí/No | Afecta captura futura. |

### Lo que no pertenece al Tipo de Audiencia

- Fecha y hora concretas.
- Sede o enlace concreto.
- Resultado de una audiencia.
- Estado del acta.
- Color actual.
- Umbrales rojo/amarillo duplicados si existe un Perfil de recordatorio.
- Días de plazo como si la audiencia fuera un Término.

## 1.5 Flujo definitivo de Audiencias

### Decisión

“Con Acta” deja de ser un estado operativo. El modelo definitivo separa el avance de la audiencia, su resultado y su documentación.

```text
PENDIENTE
    │ Registrar resultado
    ▼
RESULTADO REGISTRADO
    │ Adjuntar/validar acta cuando corresponda
    ▼
CONCLUIDA

Salidas alternativas desde Pendiente:
    CANCELADA
    ANULADA
```

### Dimensiones

| Dimensión | Valores | Propósito |
|---|---|---|
| Estado operativo | Pendiente, Resultado registrado, Concluida, Cancelada, Anulada | Ciclo de vida del registro. |
| Resultado | Celebrada, Diferida, No celebrada, Suspendida | Qué ocurrió en la fecha programada. |
| Estado del acta | No requerida, Pendiente, Adjunta, Validada, Excepción autorizada | Evidencia documental. |
| Atención temporal | Futura, Próxima, Hoy, Pasada sin resultado, Cerrada | Proximidad derivada. |

### Reglas

- La audiencia nace Pendiente.
- En “Registrar resultado” se captura el resultado, hora real, observaciones y próximos actos.
- Si el resultado es Diferida, la audiencia original conserva ese resultado y se ofrece crear una nueva programación relacionada. No se sobrescribe la fecha original.
- Si requiere acta, después del resultado queda Acta pendiente hasta adjuntarla.
- “Adjuntar acta” cambia sólo el estado documental, no concluye automáticamente.
- Concluir requiere resultado registrado y, cuando corresponda, acta validada o excepción autorizada.
- Una audiencia cuya fecha ya pasó y permanece Pendiente se muestra como **Pasada sin resultado** en rojo.
- Cancelada corresponde a un evento que no se realizará por decisión válida. Anulada corresponde a un registro incorrecto. Ninguna opción borra el registro.

## 1.6 Plazo y vencimiento

### Definiciones

| Concepto | Definición |
|---|---|
| Fecha origen | Fecha del hecho desde el cual inicia el cómputo. |
| Fecha vencimiento | Última fecha válida para cumplir la obligación. |
| Duración total | Número de días naturales o hábiles entre origen y vencimiento, excluyendo el día origen. |
| Días restantes | Días del mismo tipo entre la fecha de evaluación y el vencimiento. |
| Condición temporal | Nivel derivado de días restantes y perfil aplicado. |
| Fecha de presentación | Fecha real de cumplimiento; detiene el riesgo temporal. |

### Convención de SIAJ 2.0

- El día origen no cuenta.
- Para días naturales, `vencimiento = fechaOrigen + cantidad`.
- Para días hábiles, se cuentan únicamente días válidos del calendario asignado.
- Si una regla natural exige ajuste y el vencimiento cae en inhábil, se mueve al siguiente día hábil.
- El usuario siempre ve el texto “días naturales” o “días hábiles”.
- Una excepción manual conserva fecha calculada, fecha adoptada, autor y motivo.

### Snapshot obligatorio en cada Término

Cada Término guarda, además de sus fechas:

- modo de plazo;
- cantidad configurada o duración detectada;
- tipo de días;
- calendario y versión;
- regla de cómputo;
- perfil de alerta y versión;
- umbral amarillo calculado;
- umbral rojo calculado;
- explicación del cálculo.

Cambiar el Tipo de Término o el Perfil no modifica silenciosamente registros ya creados.

## 1.7 Alertas y semáforos

### Distinción definitiva

| Elemento | Qué es | Se guarda/configura |
|---|---|---|
| Perfil de alerta | Regla administrativa reusable. | Sí, versionada. |
| Nivel de atención | Resultado de evaluar el plazo hoy. | Se calcula; puede cachearse. |
| Semáforo | Representación visual del nivel. | No es entidad ni catálogo. |
| Alerta | Aviso funcional al cruzar un umbral. | Sí, como evento generado. |
| Notificación | Entrega de una alerta por bandeja, correo o push. | Sí, con estado de entrega/lectura. |

### Estados visuales de un Término

| Nivel | Condición | Presentación |
|---|---|---|
| Sin cálculo | No existe vencimiento aplicable. | Gris + “Sin vencimiento”. |
| Verde | Restantes mayores al umbral amarillo. | Verde + fecha + días restantes. |
| Amarillo | Restantes ≤ amarillo y > rojo. | Amarillo + “Atención próxima”. |
| Rojo | Restantes ≤ rojo y ≥ 0. | Rojo + “Vence hoy/en N días”. |
| Vencido | Restantes < 0 y sigue Activo. | Rojo oscuro + “Vencido hace N días”. |
| Cerrado | Presentado, Concluido, Cancelado o Anulado. | Neutral/azul/gris + resultado; sin alerta temporal activa. |

El color nunca aparece solo. Siempre se acompaña de icono, texto, fecha y días.

---

# 2. Diseño definitivo de Catálogos y configuración

## 2.1 Estructura administrativa

```text
ADMINISTRACIÓN
├── Catálogos jurídicos
│   ├── Tipos de Término
│   └── Tipos de Audiencia
└── Plazos y alertas
    ├── Perfiles de alerta de Términos
    ├── Perfiles de recordatorio de Audiencias
    └── Calendarios de días hábiles
```

### Decisión sobre Semáforos Procesales

**Se elimina como catálogo independiente.**

Razones funcionales:

- Un semáforo no es un dato maestro; es el resultado de una evaluación.
- El mismo color tiene semántica diferente en Términos y Audiencias.
- Configurar filas “verde/amarillo/rojo” favorece rangos solapados o incompletos.
- La regla debe expresarse mediante umbrales, y el sistema debe construir automáticamente los rangos.

Su reemplazo es **Perfiles de alerta**, ubicado en Administración, no en Catálogos.

## 2.2 Perfil de alerta de Término

### Tipos de perfil

| Tipo | Aplicación | Parámetros |
|---|---|---|
| Días restantes | Tipos de plazo Calculado | Inicio amarillo e inicio rojo en días restantes. |
| Proporcional con límites | Tipos de plazo Capturado | Porcentaje restante más mínimo y máximo de días para cada umbral. |

### Campos

- Código y nombre.
- Descripción en lenguaje natural.
- Modo compatible.
- Umbral amarillo.
- Umbral rojo.
- Límites mínimos/máximos cuando sea proporcional.
- Canales de alerta habilitados por umbral.
- Frecuencia de repetición para vencidos.
- Estado, versión y vigencia.
- Vista previa con casos ejemplo.

### Reglas de validación

- El umbral amarillo siempre debe ser mayor que el rojo.
- Los porcentajes están entre 0 y 100.
- El porcentaje amarillo debe ser mayor que el rojo.
- `mínimo ≤ máximo`.
- Los rangos no se capturan manualmente: se derivan de los dos umbrales.
- Publicar crea una versión; no modifica versiones usadas por registros existentes.

## 2.3 Perfil de recordatorio de Audiencia

Audiencias usa días/horas absolutos porque la fecha del evento es el dato relevante y no existe una “duración total del plazo” jurídicamente útil.

Ejemplo:

| Nivel | Regla |
|---|---|
| Verde | Más de 3 días para la audiencia. |
| Amarillo | Entre 3 días y más de 1 día. |
| Rojo | 1 día o menos, incluida hoy. |
| Pasada sin resultado | La fecha pasó y sigue Pendiente. |

Campos:

- código, nombre y descripción;
- anticipación amarilla;
- anticipación roja;
- recordatorios programados —por ejemplo 7 días, 3 días, 1 día y 2 horas—;
- canales;
- versión, vigencia y estado.

No comparte perfil con Términos, porque proximidad de evento y vencimiento de obligación son conceptos diferentes.

## 2.4 Vinculación de reglas

```text
Tipo de Término
  ├─ modo CALCULADO
  │    └─ Perfil de alerta “Días restantes”
  ├─ modo CAPTURADO
  │    └─ Perfil “Proporcional con límites”
  └─ modo SIN_VENCIMIENTO
       └─ sin perfil

Tipo de Audiencia
  └─ Perfil de recordatorio de Audiencia
```

Cada tipo selecciona exactamente un perfil compatible. En la primera versión no habrá jerarquía por Gerencia ni edición de umbrales por registro.

## 2.5 Configuración que se elimina

- Columnas `semaforoRojoDias` y `semaforoAmarilloDias` duplicadas en tipos cuando el perfil ya las define.
- Configuración paralela de Semáforos Procesales.
- Umbrales codificados directamente en la entidad Audiencia.
- Captura manual de color.
- Rangos completos verde/amarillo/rojo escritos por el administrador.
- Prioridad como parte del cálculo del color. La prioridad se muestra por separado.
- Reglas distintas por Gerencia durante la primera versión.

## 2.6 Valores iniciales recomendados

| Perfil | Modo | Amarillo | Rojo |
|---|---|---:|---:|
| Plazo corto | Días restantes | 2 días | 1 día |
| Plazo medio | Días restantes | 5 días | 2 días |
| Plazo variable estándar | Proporcional limitado | 50%, mín. 3, máx. 10 | 20%, mín. 1, máx. 4 |
| Audiencia estándar | Días absolutos | 3 días | 1 día |

Son valores iniciales, no normas jurídicas universales. Cada tipo debe vincular el perfil aprobado por el área responsable.

---

# 3. Diseño exacto de captura

## 3.1 Nuevo Término con plazo calculado

### Lo que captura el usuario

1. Contexto: Expediente existente o Asunto independiente.
2. Tipo de Término.
3. Actuación/asunto específico.
4. Fecha origen.
5. Responsable.
6. Prioridad administrativa, si procede.
7. Observaciones iniciales.

### Lo que calcula SIAJ

- tipo y cantidad de días;
- calendario aplicable;
- fecha vencimiento;
- inicio de amarillo;
- inicio de rojo;
- nivel actual;
- explicación completa.

### Ejemplo completo

```text
Contexto:                 Expediente 123/2027
Tipo de Término:          Contestación de demanda
Modo:                     Calculado por el sistema
Fecha origen:             01/01/2027
Regla del tipo:           5 días naturales; no cuenta el día origen
Fecha vencimiento:        06/01/2027  [calculada, sólo lectura]
Perfil de alerta:         Plazo corto
Amarillo desde:           04/01/2027  [2 días restantes]
Rojo desde:               05/01/2027  [1 día restante]
Responsable:              Lic. Andrea Pérez
Actuación:                Preparar contestación de demanda
```

Antes de guardar, SIAJ muestra:

> Vence el 06/01/2027. Se calcularon 5 días naturales a partir del 01/01/2027, sin contar el día origen.

### Validaciones

- Fecha origen obligatoria.
- Tipo vigente y compatible con la Materia.
- Responsable dentro del alcance permitido.
- No se permite editar directamente la fecha calculada.
- “Solicitar excepción de fecha” es una acción separada y autorizada.

## 3.2 Nuevo Término con plazo capturado

### Lo que captura el usuario

1. Contexto.
2. Tipo de Término.
3. Actuación/asunto.
4. Fecha origen.
5. Fecha vencimiento informada en el oficio o requerimiento.
6. Responsable.
7. Referencia/documento que sustenta la fecha.
8. Observaciones iniciales.

### Lo que calcula SIAJ

- duración total entre origen y vencimiento;
- umbrales proporcional-limitados;
- fechas de inicio amarillo y rojo;
- nivel actual.

### Ejemplo completo

```text
Contexto:                 Asunto independiente AI-2027-0041
Tipo de Término:          Requerimiento
Modo:                     Capturado al registrar
Fecha origen:             01/01/2027
Fecha vencimiento:        21/01/2027
Duración detectada:       20 días naturales
Perfil:                   Plazo variable estándar
Amarillo:                 50%, mín. 3, máx. 10 → 10 días
Rojo:                     20%, mín. 1, máx. 4 → 4 días
Amarillo desde:           11/01/2027
Rojo desde:               17/01/2027
Referencia:               Oficio DGJ/REQ/0041/2027
Responsable:              Lic. Andrea Pérez
```

Antes de guardar:

> El periodo contiene 20 días naturales. La atención amarilla inicia con 10 días restantes y la roja con 4 días restantes.

### Validaciones

- Vencimiento posterior a fecha origen.
- Referencia obligatoria para justificar la fecha capturada.
- El usuario no configura porcentajes ni colores.
- Si modifica una fecha antes de guardar, la vista previa se recalcula inmediatamente.
- Una modificación posterior genera trazabilidad y vuelve a calcular desde la misma versión del perfil, salvo una migración autorizada.

## 3.3 Nueva Audiencia

### Lo que captura el usuario

1. Contexto: Expediente existente o Asunto independiente.
2. Tipo de Audiencia.
3. Fecha y hora.
4. Zona horaria, preseleccionada por sede.
5. Duración estimada.
6. Modalidad.
7. Sede/sala o enlace.
8. Órgano.
9. Responsable y participantes.
10. Observaciones de preparación.

### Lo que deriva SIAJ

- requisito de acta;
- perfil de recordatorios;
- nivel de proximidad;
- posibles conflictos de agenda;
- fechas/horas de notificación.

### Ejemplo

```text
Contexto:                 Expediente 123/2027
Tipo:                     Audiencia inicial
Fecha y hora:             15/02/2027 10:00
Zona horaria:             America/Mexico_City
Duración:                 90 minutos
Modalidad:                Presencial
Sede / Sala:              Sede Centro / Sala 2
Responsable:              Lic. Andrea Pérez
Acta requerida:           Sí  [definido por el tipo]
Recordatorios:            3 días, 1 día y 2 horas antes
```

### Al atender la audiencia

```text
Acción:                   Registrar resultado
Resultado:                Celebrada
Hora real de finalización:11:22
Observaciones:            Se admitieron las pruebas ofrecidas...
Acta:                     Pendiente
Próxima acción:           Generar término para presentación de alegatos
```

Después se adjunta y valida el acta; sólo entonces se habilita Concluir, salvo excepción autorizada.

## 3.4 Registro sin Expediente

### Nombre definitivo

**Asunto independiente**.

Texto de ayuda:

> Utiliza esta opción cuando el Término o la Audiencia aún no pertenece a un Expediente de SIAJ. Podrás vincularlo posteriormente a un Expediente existente.

“Asunto Único” se elimina porque no explica la ausencia de expediente y puede interpretarse como un asunto irrepetible o una actuación aislada.

### Entidad propia

Sí. Se crea una entidad `AsuntoIndependiente` compartida por Términos y Audiencias.

```text
AsuntoIndependiente
├─ id / folio SIAJ generado
├─ referencia externa
├─ tipo de referencia
├─ fecha de referencia/recepción
├─ Materia
├─ descripción
├─ parte interesada / área / contraparte
├─ Gerencia
├─ responsable
├─ órgano, si se conoce
├─ origen/canal
├─ estado: Activo, Vinculado o Anulado
├─ expediente destino, cuando se vincule
└─ trazabilidad
```

### Campos obligatorios

- Materia.
- Referencia externa o justificación “sin referencia”.
- Tipo de referencia: oficio, folio, documento origen, solicitud, otro.
- Fecha de recepción/origen.
- Parte interesada, área o contraparte.
- Descripción del asunto.
- Gerencia.
- Responsable.

Órgano, sede y documento origen son opcionales, pero recomendados cuando existan.

### ¿Requiere módulo propio?

No en SIAJ 2.0. Se resuelve con:

- selector “Expediente / Asunto independiente” en la captura;
- búsqueda y reutilización de Asuntos independientes existentes;
- vista guardada “Sin expediente” en Términos y Audiencias;
- vista “Sin expediente” en Historial Procesal;
- búsqueda global por folio SIAJ, referencia, parte y descripción.

Un módulo propio sólo se justificará si en el futuro los Asuntos independientes adquieren documentos, responsables, actuaciones y ciclo de vida autónomos de gran volumen.

### Comportamiento en listados

```text
VINCULACIÓN
[EXPEDIENTE] 123/2027

o

[SIN EXPEDIENTE] AI-2027-0041
Oficio DGJ/REQ/0041/2027
```

Nunca se mostrará una celda vacía o sólo “Asunto Único”.

### Comportamiento en búsquedas

Se indexan:

- folio de Asunto independiente;
- referencia externa;
- descripción;
- parte/área/contraparte;
- Materia;
- responsable;
- IDs de Términos y Audiencias relacionados.

### Vinculación posterior

```text
Asunto independiente activo
        │
        ▼
Acción “Vincular a expediente existente”
        │
        ├─ Buscar y seleccionar expediente
        ├─ Comparar Materia, partes, órgano y Gerencia
        ├─ Resolver discrepancias explícitamente
        ├─ Mostrar Términos/Audiencias que se moverán
        └─ Confirmar con motivo
        │
        ▼
Todos los registros relacionados quedan vinculados al Expediente
El Asunto independiente queda en estado VINCULADO
Su folio y datos originales se conservan en trazabilidad
```

No aparece “Crear nuevo expediente” dentro de este flujo. Si el expediente no existe, el usuario debe salir y crearlo en Expedientes; al regresar, SIAJ conserva el borrador o permite reanudar la vinculación.

La vinculación es del Asunto independiente completo y mueve sus actuaciones relacionadas. Mover sólo una actuación será una corrección administrativa excepcional.

---

# 4. Diseño definitivo del semáforo

## 4.1 Recomendación de modelo

| Alternativa | Ventaja | Problema | Decisión |
|---|---|---|---|
| Días absolutos para todo | Muy fácil de explicar. | Se comporta mal con plazos variables muy cortos o largos. | Usar sólo en plazos Calculados y Audiencias. |
| Porcentaje puro | Se adapta a cualquier duración. | Puede alertar demasiado pronto en periodos largos y es poco natural para el usuario jurídico. | No usar solo. |
| Porcentaje con mínimo | Evita umbrales menores a un día. | Sigue pudiendo alertar demasiado pronto en periodos largos. | Insuficiente. |
| Porcentaje con mínimo y máximo | Se adapta, evita alertas demasiado tardías y demasiado tempranas. | Configuración más técnica. | **Modelo elegido para plazos Capturados.** |

La complejidad queda en Administración. El abogado sólo ve las fechas resultantes.

## 4.2 Fórmulas

### Plazo Calculado: días absolutos

```text
si díasRestantes < 0                         → VENCIDO
si díasRestantes <= umbralRojo               → ROJO
si díasRestantes <= umbralAmarillo           → AMARILLO
si díasRestantes > umbralAmarillo            → VERDE
```

Restricción: `umbralAmarillo > umbralRojo ≥ 0`.

No se configura el rango verde: es todo lo que queda por encima de amarillo.

### Plazo Capturado: porcentaje con límites

```text
umbralRojo = redondearArriba(duraciónTotal × porcentajeRojo)
umbralRojo = limitar(umbralRojo, mínimoRojo, máximoRojo)

umbralAmarillo = redondearArriba(duraciónTotal × porcentajeAmarillo)
umbralAmarillo = limitar(umbralAmarillo, mínimoAmarillo, máximoAmarillo)

limitar(valor, mínimo, máximo) =
    máximo(mínimo, mínimo(valor, máximo))
```

Después se aplican las mismas condiciones de días restantes.

### Ejemplo variable estándar

```text
Duración total:      20 días
Amarillo:            50%, mínimo 3, máximo 10
Rojo:                20%, mínimo 1, máximo 4

Amarillo calculado:  ceil(20 × .50) = 10 → limitado a 10
Rojo calculado:      ceil(20 × .20) = 4  → limitado a 4
```

## 4.3 Línea temporal del plazo fijo de cinco días

Fecha origen 01/01/2027; vencimiento 06/01/2027; amarillo 2; rojo 1.

| Fecha | Días restantes | Nivel | Texto mostrado |
|---|---:|---|---|
| 01/01/2027 | 5 | Verde | Vence en 5 días naturales. |
| 02/01/2027 | 4 | Verde | Vence en 4 días naturales. |
| 03/01/2027 | 3 | Verde | Vence en 3 días naturales. |
| 04/01/2027 | 2 | Amarillo | Atención: vence en 2 días naturales. |
| 05/01/2027 | 1 | Rojo | Urgente: vence mañana. |
| 06/01/2027 | 0 | Rojo | Vence hoy. |
| 07/01/2027 | -1 | Vencido | Vencido hace 1 día. |

## 4.4 Línea temporal del plazo capturado de veinte días

Fecha origen 01/01/2027; vencimiento 21/01/2027; amarillo 10; rojo 4.

| Periodo | Días restantes | Nivel |
|---|---:|---|
| 01/01 al 10/01 | 20 a 11 | Verde |
| 11/01 al 16/01 | 10 a 5 | Amarillo |
| 17/01 al 21/01 | 4 a 0 | Rojo |
| Desde 22/01 | Menor que 0 | Vencido |

## 4.5 Cuándo cambia el color

- Al cambiar el día según la zona horaria institucional.
- Al modificar fecha origen o vencimiento mediante una operación autorizada.
- Al publicarse una suspensión o excepción aplicada expresamente al Término.
- Al registrar Presentado, cuando pasa inmediatamente a Cerrado temporal.
- Al cancelar o anular.
- No cambia retroactivamente porque un administrador edite un perfil; el Término usa su snapshot.

Un job diario actualiza proyecciones y genera alertas. La pantalla también recalcula al consultar para evitar depender únicamente del job.

## 4.6 Qué ve el usuario

### En listado

```text
[ROJO · VENCE MAÑANA]
06 ene 2027 · 1 día natural
```

### En detalle

```text
Vencimiento
06 de enero de 2027

[ROJO] Vence mañana
1 día natural restante

Cómo se calculó
5 días naturales desde el 01/01/2027.
El día origen no se contó.

Alertas
Amarillo desde 04/01 · Rojo desde 05/01
```

### En calendario

- Punto de color más etiqueta accesible.
- Tooltip y nombre accesible con fecha, tipo y condición.
- Nunca depender sólo del color.

## 4.7 Qué ocurre al vencer

1. El Término continúa Activo.
2. La condición temporal cambia a Vencido.
3. Se genera una alerta idempotente `TERMINO_VENCIDO`.
4. Se notifica al responsable y a la cadena definida.
5. Aparece en las vistas Vencidos de Términos, Expedientes e Inicio.
6. No se cambia automáticamente a Concluido, Cancelado ni Anulado.
7. Registrar presentación posterior fija la condición de cumplimiento como Fuera de plazo.
8. Las observaciones de presentación permiten documentar la causa, sin borrar el atraso.

## 4.8 Alertas y notificaciones

| Evento | Alerta | Persistencia |
|---|---|---|
| Entra en amarillo | Próximo vencimiento | Una por cruce de umbral. |
| Entra en rojo | Vencimiento urgente | Una por cruce de umbral. |
| Vence hoy | Vence hoy | Una diaria o según perfil. |
| Pasa a vencido | Término vencido | Inmediata y escalable. |
| Sigue vencido | Recordatorio de vencido | Frecuencia configurable; evita saturación. |
| Se presenta | Cumplimiento registrado | Cierra futuras alertas temporales. |
| Cambia fecha | Vencimiento recalculado | Informa sólo a interesados relevantes. |

Marcar una notificación como leída no cambia el semáforo ni elimina el riesgo.

## 4.9 Audiencias

Las Audiencias no usan porcentaje.

```text
si estado != PENDIENTE                   → CERRADA/NEUTRAL
si fechaHora < ahora                     → PASADA SIN RESULTADO
si tiempoRestante <= umbralRojo          → ROJO
si tiempoRestante <= umbralAmarillo      → AMARILLO
si tiempoRestante > umbralAmarillo       → VERDE
```

El semáforo de Audiencia comunica proximidad, no vencimiento. El texto debe decir “Audiencia en 1 día” o “Pasada sin resultado”, nunca “plazo rojo”.

---

# 5. Mockups conceptuales en texto

## 5.1 Administración → Tipos de Término

```text
┌──────────────────────────────────────────────────────────────────────────┐
│ Tipos de Término                                      [+ Nuevo tipo]    │
│ Define la captura, cálculo y controles de cada obligación.              │
├──────────────────────────────────────────────────────────────────────────┤
│ [Buscar tipo________________] [Estado ▾] [Materia ▾] [Modo de plazo ▾]  │
├───────────────┬──────────────────────┬──────────────┬──────────┬─────────┤
│ Tipo          │ Modo                 │ Regla        │ Perfil   │ Estado  │
├───────────────┼──────────────────────┼──────────────┼──────────┼─────────┤
│ Contestación  │ Calculado            │ 5 naturales │ Corto    │ Vigente │
│ Requerimiento │ Capturado al registrar│ Usuario     │ Variable │ Vigente │
└───────────────┴──────────────────────┴──────────────┴──────────┴─────────┘

Panel de edición
┌──────────────────────────────────────────────────────────────────────────┐
│ Nombre: [Contestación de demanda____________________________]            │
│ Modo:   (●) Calculado por el sistema  ( ) Capturado al registrar        │
│ Plazo:  [5] [Días naturales ▾]                                          │
│ Perfil: [Plazo corto — amarillo 2, rojo 1 ▾] [Ver perfil]               │
│                                                                          │
│ Controles documentales                                                   │
│ [✓] Requiere documento liberado                                          │
│ [✓] Requiere acuse para presentar                                        │
│ [✓] Requiere validar acuse para concluir                                 │
│ [ ] Permite excepción sin acuse                                          │
│                                                                          │
│ Vista previa: Origen 01/01/2027 → Vence 06/01/2027                       │
│ Verde hasta 03/01 · Amarillo 04/01 · Rojo desde 05/01                   │
│                                              [Cancelar] [Guardar borrador]│
└──────────────────────────────────────────────────────────────────────────┘
```

## 5.2 Administración → Perfiles de alerta

```text
┌──────────────────────────────────────────────────────────────────────────┐
│ Plazos y alertas / Perfil “Variable estándar”                            │
├──────────────────────────────────────────────────────────────────────────┤
│ Aplicación: Plazos capturados al registrar                               │
│                                                                          │
│ Atención amarilla                                                        │
│ Porcentaje restante [50 %]  Mínimo [3 días]  Máximo [10 días]          │
│                                                                          │
│ Atención roja                                                            │
│ Porcentaje restante [20 %]  Mínimo [1 día ]  Máximo [4 días ]          │
│                                                                          │
│ Simular                                                                  │
│ Duración [20 días] → Amarillo con 10 días · Rojo con 4 días             │
│ Duración [ 5 días] → Amarillo con  3 días · Rojo con 1 día              │
│ Duración [90 días] → Amarillo con 10 días · Rojo con 4 días             │
│                                                                          │
│ Esta versión será usada sólo por nuevos registros.                       │
│                                    [Guardar borrador] [Publicar versión] │
└──────────────────────────────────────────────────────────────────────────┘
```

## 5.3 Nuevo Término

```text
┌──────────────────────────────────────────────────────────────────────────┐
│ Nuevo Término                                                            │
├──────────────────────────────────────────────────────────────────────────┤
│ 1. Vinculación                                                           │
│ (●) Expediente existente     ( ) Sin expediente / Asunto independiente  │
│ [Buscar número, partes o referencia___________________________]          │
│ Seleccionado: 123/2027 · Materia administrativa                          │
│                                                                          │
│ 2. Término                                                               │
│ Tipo              [Contestación de demanda ▾]                            │
│ Actuación         [Preparar contestación de demanda____________]         │
│ Fecha origen      [01/01/2027]                                           │
│ Responsable       [Andrea Pérez ▾]                                       │
│                                                                          │
│ 3. Cálculo automático                                                    │
│ ┌──────────────────────────────────────────────────────────────────────┐ │
│ │ Vence 06/01/2027 · 5 días naturales                                │ │
│ │ El día origen no cuenta.                                             │ │
│ │ [VERDE] Amarillo desde 04/01 · Rojo desde 05/01                     │ │
│ └──────────────────────────────────────────────────────────────────────┘ │
│                                                                          │
│                                      [Cancelar] [Guardar Término]        │
└──────────────────────────────────────────────────────────────────────────┘
```

### Variante sin expediente

```text
( ) Expediente existente     (●) Sin expediente / Asunto independiente

[Buscar Asunto independiente existente________________________]
o
[+ Registrar nuevo Asunto independiente]

Materia*                 [Administrativa ▾]
Tipo de referencia*      [Oficio ▾]
Referencia*              [DGJ/REQ/0041/2027________________]
Fecha de recepción*      [01/01/2027]
Parte/área/contraparte*  [Dirección de Recursos Humanos_____]
Descripción*             [Requerimiento de información______]
Gerencia*                [Contencioso ▾]
Responsable*             [Andrea Pérez ▾]

Se generará el folio AI-2027-0041.
No se creará un Expediente.
```

## 5.4 Nueva Audiencia

```text
┌──────────────────────────────────────────────────────────────────────────┐
│ Nueva Audiencia                                                          │
├──────────────────────────────────────────────────────────────────────────┤
│ Vinculación: (●) Expediente  ( ) Asunto independiente                   │
│ Expediente       [123/2027 · Buscar/cambiar]                             │
│ Tipo              [Audiencia inicial ▾]                                  │
│ Fecha y hora      [15/02/2027] [10:00]                                   │
│ Duración          [90 min]     Zona [America/Mexico_City ▾]             │
│ Modalidad         [Presencial ▾]                                         │
│ Sede / Sala       [Sede Centro ▾] [Sala 2 ▾]                            │
│ Órgano            [Juzgado ... ▾]                                        │
│ Responsable       [Andrea Pérez ▾]                                       │
│ Participantes     [+ Agregar]                                            │
│                                                                          │
│ Acta requerida: Sí                                                       │
│ Recordatorios: 3 días · 1 día · 2 horas                                  │
│ Conflictos: Ninguno                                                       │
│                                      [Cancelar] [Programar audiencia]     │
└──────────────────────────────────────────────────────────────────────────┘
```

## 5.5 Detalle de Término

```text
┌──────────────────────────────────────────────────────────────────────────┐
│ Término T-2027-0182        [ROJO · VENCE MAÑANA]      [Registrar presentación]│
│ Contestación de demanda · Expediente 123/2027                            │
├──────────────────────────────────────────────────────────────────────────┤
│ PROYECTISTA ─ REVISIÓN ─ GERENCIA ─ DIRECCIÓN ─ [LIBERADO] ─ PRESENTADO │
│                                                                          │
│ Vencimiento                       Contexto                               │
│ 06/01/2027                        Expediente 123/2027                    │
│ 1 día natural restante            Materia administrativa                │
│ Calculado: 5 días desde 01/01     Responsable: Andrea Pérez             │
│                                                                          │
│ Documento liberado [Ver]          Acuse: Pendiente                      │
│                                                                          │
│ [Resumen] [Documentos] [Comentarios] [Trazabilidad]                     │
└──────────────────────────────────────────────────────────────────────────┘
```

## 5.6 Detalle de Audiencia

```text
┌──────────────────────────────────────────────────────────────────────────┐
│ Audiencia A-2027-0091       [HOY · 10:00]          [Registrar resultado]│
│ Audiencia inicial · Expediente 123/2027                                 │
├──────────────────────────────────────────────────────────────────────────┤
│ Estado: PENDIENTE         Resultado: —          Acta: PENDIENTE          │
│                                                                          │
│ 15/02/2027 · 10:00–11:30 · America/Mexico_City                          │
│ Presencial · Sede Centro · Sala 2                                       │
│ Responsable: Andrea Pérez                                                │
│                                                                          │
│ [Resumen] [Participantes] [Documentos] [Trazabilidad]                    │
└──────────────────────────────────────────────────────────────────────────┘
```

## 5.7 Listado de Términos

```text
┌──────────────────────────────────────────────────────────────────────────┐
│ Términos                                             [+ Nuevo Término]  │
│ [Mi trabajo] [Vencidos 4] [Hoy 3] [Próximos 12] [Sin expediente 6]     │
│ [Buscar________________________________] [Filtros ▾] [Lista | Calendario]│
├────────────┬──────────────────┬──────────────┬────────────┬──────────────┤
│ Atención   │ Término/contexto │ Etapa/estado │ Vencimiento│ Responsable  │
├────────────┼──────────────────┼──────────────┼────────────┼──────────────┤
│ ROJO       │ Contestación     │ Liberado     │ 06/01/2027 │ A. Pérez     │
│ Mañana     │ EXP 123/2027     │ Activo       │ 1 natural  │              │
├────────────┼──────────────────┼──────────────┼────────────┼──────────────┤
│ AMARILLO   │ Requerimiento    │ Revisión     │ 21/01/2027 │ J. López     │
│ 8 días     │ SIN EXP AI-0041  │ Activo       │ 8 naturales│              │
├────────────┼──────────────────┼──────────────┼────────────┼──────────────┤
│ CERRADO    │ Informe          │ Presentado   │ 04/01/2027 │ A. Pérez     │
│ En tiempo  │ EXP 098/2027     │ Acuse adjunto│ Presentado │              │
└────────────┴──────────────────┴──────────────┴────────────┴──────────────┘
```

## 5.8 Listado de Audiencias

```text
┌──────────────────────────────────────────────────────────────────────────┐
│ Audiencias                                         [+ Nueva Audiencia]  │
│ [Hoy 4] [Próximas 10] [Pasadas sin resultado 2] [Acta pendiente 5]    │
│ [Buscar________________________] [Filtros ▾] [Día | Semana | Mes | Lista]│
├──────────────┬──────────────────────┬───────────┬────────────┬───────────┤
│ Atención     │ Audiencia/contexto   │ Fecha     │ Estado     │ Acta      │
├──────────────┼──────────────────────┼───────────┼────────────┼───────────┤
│ HOY          │ Audiencia inicial    │ 10:00     │ Pendiente  │ Requerida │
│              │ EXP 123/2027         │ Sala 2    │            │           │
│ PASADA       │ Comparecencia        │ Ayer 13:00│ Pendiente  │ Pendiente │
│ SIN RESULTADO│ SIN EXP AI-0038      │ Virtual   │            │           │
│ CERRADA      │ Audiencia incidental │ 08/02     │ Concluida  │ Validada  │
│              │ EXP 077/2027         │           │ Celebrada  │           │
└──────────────┴──────────────────────┴───────────┴────────────┴───────────┘
```

---

# 6. Análisis crítico de la Arquitectura Objetivo original

## 6.1 Elementos que se conservan

| Decisión original | Aplicación definitiva |
|---|---|
| Expedientes se crean únicamente en Expedientes | Se conserva y se hace regla explícita de navegación y permisos. |
| Términos y Audiencias son fuentes operativas | Se conserva. |
| Historial Procesal es una proyección de lectura | Se conserva. |
| Trazabilidad es inmutable | Se conserva como bitácora append-only, sin convertir todo el negocio en event sourcing. |
| Catálogos y Administración están separados | Se conserva. Tipos quedan en Catálogos; perfiles y calendarios en Administración. |
| Estado, etapa y atención son dimensiones distintas | Se conserva y se concreta para ambos flujos. |
| Los asuntos sin expediente deben tener contexto válido | Se conserva mediante Asunto independiente. |
| No existe borrado físico de actuaciones jurídicas | Se conserva. Cancelación y anulación son transiciones auditables. |
| Las reglas aplicadas quedan versionadas/snapshot | Se conserva para reproducir vencimientos. |

## 6.2 Elementos que se simplifican

### Contexto procesal

No se necesita exponer una jerarquía abstracta compleja al usuario. La experiencia tendrá sólo dos opciones:

```text
Expediente existente | Asunto independiente
```

En software puede implementarse con dos referencias excluyentes y validación XOR.

### Políticas de plazo

No se requiere un motor genérico de reglas en la primera versión. El modelo queda reducido a:

- Tipo de Término con modo Calculado, Capturado o Sin vencimiento.
- Cantidad/tipo de días cuando es Calculado.
- Calendario institucional.
- Perfil de alerta compatible.
- Snapshot de parámetros en cada Término.

### Jerarquía de configuración

Se elimina inicialmente la precedencia global → Gerencia → tipo → registro. La versión definitiva inicial utiliza:

```text
Perfil predeterminado del sistema
        ↓
Perfil seleccionado por Tipo de Término/Audiencia
```

No habrá override por Gerencia ni ajuste libre por registro. Las excepciones de fecha se autorizan y auditan, pero no crean semáforos personalizados.

### Flujo de Términos

No se crea un workflow engine configurable. El flujo institucional es fijo. Se representa visualmente como una secuencia, pero los datos se separan en etapa, estado y acuse.

### Flujo de Audiencias

Se reduce a tres pasos operativos y una dimensión documental. No se crean estados combinados para cada posible resultado y condición del acta.

### CQRS

Se separan comandos y consultas mediante servicios/DTOs, sin adoptar infraestructura CQRS distribuida. Historial y listados pueden ser consultas optimizadas dentro del mismo monolito.

## 6.3 Elementos que se eliminan por sobreingeniería

- Crear un módulo completo para Asuntos independientes desde el inicio.
- Permitir crear Expediente desde Términos/Audiencias.
- Motor universal de políticas con expresiones arbitrarias.
- Semáforos configurables por registro.
- Jerarquías de override por área, usuario y asunto.
- Flujos de aprobación diferentes por cada Tipo de Término.
- Event sourcing como modelo principal de persistencia.
- Microservicios para Expedientes, Términos o Audiencias.
- Una entidad editable de Historial Procesal.
- Porcentajes para la proximidad de Audiencias.
- Configurar manualmente los tres rangos de color.
- Recalcular automáticamente todos los históricos al cambiar un perfil.

## 6.4 Elementos que se posponen

| Capacidad | Cuándo reconsiderarla |
|---|---|
| Perfiles distintos por Gerencia | Cuando exista evidencia normativa u operativa de reglas realmente diferentes. |
| Módulo propio de Asuntos independientes | Cuando concentren múltiples actuaciones/documentos y volumen que justifique gestión autónoma. |
| Varios calendarios por jurisdicción | Cuando SIAJ opere con competencias que tengan días inhábiles diferentes. La primera versión usa el calendario institucional necesario. |
| Escalamiento multicanal avanzado | Después de estabilizar bandeja interna y correo, midiendo fatiga de alertas. |
| Reglas compuestas por horas/minutos | Cuando existan plazos intradía reales. |
| Firma o sello criptográfico de exportaciones | Cuando la política de auditoría o evidencia lo requiera formalmente. |
| Motor configurable de workflows | Sólo si aparecen varios flujos institucionales estables y aprobados. |
| Índice de búsqueda externo | Cuando volumen y latencia no puedan resolverse con PostgreSQL e índices adecuados. |
| Analítica predictiva de riesgo | Después de contar con datos históricos completos y confiables. |

---

# 7. Modelo de datos funcional mínimo

```text
TipoTermino
├─ id, codigo, nombre, descripcion
├─ modoPlazo
├─ cantidadDias?
├─ tipoDias?
├─ calendarioId?
├─ perfilAlertaId?
├─ requiereDocumentoLiberado
├─ requiereAcuse
├─ permiteExcepcionSinAcuse
├─ requiereValidacionAcuse
└─ estado, vigencia

PerfilAlertaTermino
├─ id, codigo, nombre, version
├─ modo: DIAS | PROPORCIONAL_LIMITADO
├─ amarilloDias? / rojoDias?
├─ amarilloPct? / minAmarillo? / maxAmarillo?
├─ rojoPct? / minRojo? / maxRojo?
└─ estado, vigencia

Termino
├─ expedienteId? XOR asuntoIndependienteId?
├─ tipoTerminoId
├─ fechaOrigen, fechaVencimiento
├─ snapshotCalculo
├─ etapaTrabajo
├─ estadoOperativo
├─ estadoAcuse
├─ fechaPresentacion?, presentadoPor?
├─ condicionCumplimiento?
├─ fechaConclusion?, concluidoPor?
└─ observaciones separadas

TipoAudiencia
├─ id, codigo, nombre, descripcion
├─ duracionPredeterminada?
├─ modalidadesPermitidas
├─ requiereActa
├─ permiteExcepcionSinActa
├─ requiereValidacionActa
├─ perfilRecordatorioId
└─ estado, vigencia

Audiencia
├─ expedienteId? XOR asuntoIndependienteId?
├─ tipoAudienciaId
├─ fechaHora, zonaHoraria, duracion
├─ modalidad, sede/sala/enlace
├─ estadoOperativo
├─ resultado?
├─ estadoActa
├─ observacionesResultado?
├─ fechaConclusion?, concluidoPor?
└─ audienciaOrigenId? para diferimientos

AsuntoIndependiente
├─ id, folioSiaj
├─ tipoReferencia, referencia, fechaReferencia
├─ materiaId, descripcion
├─ parteInteresada
├─ gerenciaId, responsableId
├─ organoId?, documentoOrigenId?
├─ estado
└─ expedienteDestinoId?, fechaVinculacion?
```

## Invariantes obligatorias

1. Término y Audiencia tienen Expediente o Asunto independiente, nunca ambos y nunca ninguno.
2. Desde Términos/Audiencias sólo puede seleccionarse un Expediente existente.
3. El perfil elegido debe ser compatible con el modo del Tipo.
4. Amarillo siempre comienza antes que Rojo.
5. Vencimiento es posterior a origen.
6. Presentado detiene alertas de vencimiento y fija cumplimiento en tiempo/fuera de plazo.
7. Concluir Término valida requisitos de acuse.
8. Concluir Audiencia valida resultado y acta aplicable.
9. Un cambio de configuración no reescribe snapshots anteriores.
10. Vincular un Asunto independiente conserva folio, datos originales y trazabilidad.

---

# 8. Flujo integral resultante

```text
EXPEDIENTE EXISTENTE                      SIN EXPEDIENTE
        │                                      │
        │                              Asunto independiente
        │                                      │
        └──────────────────┬───────────────────┘
                           ▼
                 TÉRMINO / AUDIENCIA
                           │
             ┌─────────────┴─────────────┐
             ▼                           ▼
      Tipo de actuación           Datos particulares
             │                           │
             └─────────────┬─────────────┘
                           ▼
               Regla y perfil aplicables
                           │
             ┌─────────────┴─────────────┐
             ▼                           ▼
     Cálculo/proximidad             Flujo operativo
             │                           │
             ├─ semáforo calculado       ├─ documentos
             ├─ alertas                  ├─ presentación/resultado
             └─ notificaciones           └─ conclusión
                           │
                           ▼
              Historial + Trazabilidad
```

## Decisión final

SIAJ debe resolver este dominio con cinco piezas claramente separadas:

1. **Tipo de Término/Audiencia:** define comportamiento y requisitos.
2. **Registro operativo:** contiene fechas, responsables, estado y documentos reales.
3. **Perfil de alerta/recordatorio:** define cuándo aumentar atención.
4. **Semáforo:** muestra el resultado calculado; no se administra.
5. **Asunto independiente:** aporta contexto válido cuando aún no existe Expediente.

El modelo híbrido limitado se reserva para plazos capturados porque se adapta a duraciones variables sin generar alertas absurdamente tempranas o tardías. El usuario jurídico nunca tendrá que interpretar la fórmula: verá “vence el 21 de enero”, “quedan 4 días” y “atención roja”.

Esta solución mantiene el flujo institucional, elimina duplicidad funcional y evita introducir un motor de reglas o módulos adicionales antes de que el negocio los necesite.
