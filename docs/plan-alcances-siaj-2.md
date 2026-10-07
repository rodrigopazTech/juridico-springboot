# SIAJ 2.0 — Plan definitivo de alcances y criterios de cierre

**Estado del documento:** Vigente  
**Versión:** 1.0  
**Fecha de consolidación:** 5 de octubre de 2026  
**Propósito:** Establecer qué debe entregar cada alcance de SIAJ 2.0 y qué evidencias son obligatorias para declararlo completamente cerrado.

---

## 1. Función de este documento

Este documento es la fuente de verdad para:

- el orden de implementación de SIAJ 2.0;
- el contenido mínimo de cada alcance;
- las validaciones funcionales, técnicas y visuales requeridas;
- la evidencia necesaria para aceptar un alcance;
- el estado de avance del proyecto.

No sustituye el diseño funcional ni la arquitectura objetivo. Los utiliza como base y los convierte en un plan ejecutable y verificable.

### 1.1 Jerarquía documental

Cuando sea necesario interpretar una decisión se utilizará el siguiente orden:

1. **Este plan de alcances:** determina orden, entregables y criterios de cierre.
2. **`modelo-funcional-plazos-alertas-siaj.md`:** determina el comportamiento funcional exacto de plazos, alertas, Términos, Audiencias y Asuntos Independientes.
3. **`arquitectura-objetivo-siaj.md`:** determina la dirección arquitectónica general y los principios de largo plazo.
4. **Decisiones de negocio posteriores documentadas y aprobadas:** complementan o sustituyen una definición anterior cuando se registran explícitamente.

Si la arquitectura objetivo propone una solución más amplia que el modelo funcional definitivo, se implementará primero la versión simplificada definida por el modelo funcional, salvo aprobación expresa de un cambio de alcance.

---

## 2. Decisiones vigentes

Las siguientes decisiones forman parte de la línea base del proyecto:

1. El **Alcance 1 se considera cerrado**.
2. En la interfaz se utiliza el término **Regla de alerta**. El modelo interno puede conservar entidades técnicas de perfil o versión.
3. El rediseño para configurar las alertas directamente dentro de un Tipo de Término o Tipo de Audiencia queda **pospuesto**. No bloquea los siguientes alcances.
4. Los Tipos de Término y Tipos de Audiencia se administran en **Plazos y alertas**.
5. Semáforo es únicamente una representación visual de un cálculo. No es un catálogo capturable.
6. Todos los Tipos de Término requieren proyecto final aprobado.
7. Para registrar un Término como Presentado se adjunta un **Acuse** o, cuando no fue posible presentar o existe una situación justificable, una **Nota Informativa** que lo sustituye.
8. La Nota Informativa cambia el Término a Presentado y debe poder localizarse posteriormente mediante filtros.
9. Una Audiencia puede quedar **Suspendida** únicamente si se adjunta una Nota Informativa.
10. La prueba integral con un abogado real se realizará al terminar todos los alcances.
11. Microsoft Graph es una línea de trabajo independiente y no bloquea SIAJ 2.0.
12. El sistema se encuentra en desarrollo. No se exige migrar datos históricos para cerrar estos alcances; sí se exige que el esquema pueda generarse de forma reproducible en una base limpia y que las restricciones sean verificables.
13. Las etapas operativas de Términos y Audiencias son flexibles: cualquier usuario con acceso al registro puede participar. La matriz de roles dirige notificaciones, no bloquea la operación.
14. Jefatura de Departamento tiene la misma autoridad funcional que Gerencia únicamente dentro de sus Materias asignadas.
15. Plazos y alertas es visible para todos los usuarios autenticados. Jefatura y Gerencia administran Reglas y Tipos dentro de su alcance; el calendario institucional, por su impacto global, queda reservado a Dirección y Administración.

---

## 3. Estados permitidos para un alcance

| Estado | Significado |
|---|---|
| No iniciado | No existe implementación verificable. |
| En desarrollo | Hay cambios en construcción sin validación integral. |
| En validación | La implementación está completa y se están ejecutando pruebas. |
| Cerrado con observaciones | Funciona, pero existen pendientes aceptados que no afectan integridad, seguridad ni operación principal. |
| Cerrado | Cumple todos los criterios obligatorios y cuenta con aceptación funcional. |
| Reabierto | Una regresión o nueva evidencia invalida el cierre anterior. |

Un alcance no puede considerarse cerrado únicamente porque el código compile o porque la pantalla se muestre correctamente.

---

## 4. Criterios globales de cierre

Cada alcance debe cumplir todos los criterios aplicables de esta sección.

### 4.1 Funcionalidad

- Todos los casos de uso principales funcionan de punta a punta.
- Los casos excepcionales definidos generan una respuesta controlada y comprensible.
- No existen dos flujos activos que resuelvan de manera contradictoria el mismo proceso.
- Las reglas vigentes se aplican también desde peticiones manipuladas, no sólo mediante controles visuales.
- Los mensajes de error están en español, explican el problema y orientan al usuario.

### 4.2 Integridad de datos

- Las invariantes críticas se protegen en el servicio y, cuando corresponde, en base de datos.
- No se generan registros huérfanos, duplicados involuntarios o combinaciones inválidas.
- Los cambios de configuración no alteran silenciosamente registros históricos.
- Las acciones sensibles conservan usuario, fecha, resultado y motivo cuando aplique.
- Una instalación limpia puede construir el esquema requerido sin pasos manuales ocultos.

### 4.3 Seguridad y alcance

- Los listados, selectores, búsquedas, exportaciones y endpoints respetan el mismo alcance de permisos.
- Un usuario no puede consultar, modificar o vincular expedientes, asuntos o documentos fuera de su ámbito.
- Las validaciones de permisos se realizan en servidor.
- Los archivos sólo son accesibles para usuarios autorizados.

### 4.4 Experiencia de usuario

- El flujo utiliza términos comprensibles para usuarios jurídicos no técnicos.
- El estado de un registro se comprende sin depender exclusivamente del color.
- Las acciones disponibles corresponden al estado actual del registro.
- Los formularios muestran únicamente campos aplicables al caso seleccionado.
- Los menús, modales y mensajes son completamente visibles en resoluciones de escritorio soportadas.
- Los PDF se visualizan en el navegador; formatos no visualizables se descargan de forma controlada.
- No hay acciones duplicadas ni nombres de archivo usados como etiqueta principal de navegación.

### 4.5 Calidad técnica

- El proyecto compila desde una copia limpia.
- Las pruebas unitarias y de integración terminan sin fallos.
- No existen errores JavaScript en la consola durante los casos probados.
- No existen errores no controlados en la consola o bitácora del servidor.
- Las tareas programadas y procesos en segundo plano son idempotentes cuando puedan repetirse.
- No se incorporan secretos, credenciales ni archivos locales al repositorio.

### 4.6 Validación en navegador

Para cada caso principal se debe conservar evidencia de:

1. inicio de sesión con un rol representativo;
2. navegación hasta la pantalla;
3. captura de datos;
4. guardado o transición;
5. resultado visible;
6. actualización o reapertura del registro;
7. revisión de consola del navegador;
8. revisión de bitácora del servidor.

Las pruebas automatizadas con Playwright deben cubrir el camino principal y los bloqueos críticos. La validación manual del responsable funcional complementa las pruebas automatizadas.

### 4.7 Evidencia de cierre

Cada alcance debe entregar:

- lista de casos de uso probados;
- resultados de pruebas automatizadas;
- capturas de las pantallas principales;
- lista de observaciones conocidas;
- confirmación de que no quedan defectos críticos o altos;
- commit independiente o conjunto de commits claramente identificados;
- aceptación funcional del responsable del producto.

---

# 5. Alcances de implementación

## Alcance 1 — Reglas de alerta y motor único de atención temporal

### Objetivo

Eliminar el modelo de Semáforos Procesales configurables y establecer una sola fuente de verdad para calcular y explicar el nivel de atención temporal de Términos y Audiencias.

### Debe incluir

- Eliminación de Semáforos Procesales como catálogo funcional.
- Retiro de cálculos heredados o duplicados.
- Reglas de alerta para Términos calculados.
- Reglas proporcionales con límites para Términos capturados.
- Reglas de recordatorio en horas para Audiencias.
- Soporte para Términos sin vencimiento.
- Ciclo de vida de una Regla de alerta:
  - borrador;
  - guardar y publicar con confirmación;
  - editar;
  - inactivar.
- Snapshot de los umbrales aplicados al registro.
- Motor único para determinar:
  - sin cálculo;
  - a tiempo;
  - atención;
  - crítico;
  - vencido;
  - cerrado.
- Explicación contextual del resultado: estado, regla, vencimiento, tiempo restante y causa.
- Acceso de consulta a Plazos y alertas para todos los usuarios.
- Modificación compartida sin solicitud de permiso adicional, mostrando claramente que el cambio afecta la configuración de la Gerencia.

### No incluye

- Rediseñar la persistencia para eliminar la entidad interna de reglas.
- Configurar alertas directamente dentro del formulario del Tipo.
- Integraciones Microsoft Graph.

### Validaciones obligatorias

1. No existe opción visible para crear Semáforos Procesales.
2. Ningún controlador, entidad, plantilla o tarea calcula colores mediante una regla paralela.
3. El color siempre aparece acompañado de texto e icono.
4. Una regla incompatible no puede asociarse a un Tipo.
5. Amarillo siempre inicia antes que rojo.
6. Un Término sin vencimiento no genera una fecha artificial ni una alerta de atraso.
7. Presentar, concluir, cancelar o anular detiene el riesgo temporal.
8. Editar una Regla no modifica los umbrales guardados en registros anteriores.
9. El comportamiento es consistente en listado, detalle, calendario y notificaciones.
10. Las pruebas del evaluador cubren todos los niveles y fronteras de umbral.

### Criterio definitivo de cierre

Existe un único motor de atención temporal y ninguna ruta funcional conserva el modelo anterior de semáforos capturados.

### Estado actual

**Cerrado.** Confirmado funcionalmente el 5 de octubre de 2026. El rediseño adicional de UX para reglas queda diferido.

---

## Alcance 2 — Cálculo reproducible de plazos y calendarios

### Objetivo

Garantizar que todo vencimiento pueda calcularse, explicarse y reproducirse con los datos y reglas vigentes al momento de crear el Término.

### Debe incluir

- Fecha origen obligatoria.
- Convención de que el día origen no cuenta.
- Cálculo en días naturales.
- Cálculo en días hábiles.
- Exclusión de fines de semana cuando corresponda.
- Calendario institucional de días inhábiles.
- Suspensiones institucionales con periodo, motivo, estado y trazabilidad.
- Tres modos de plazo:
  - calculado por el sistema;
  - capturado por el usuario;
  - sin vencimiento.
- Ajuste al siguiente día hábil cuando la regla aplicable lo requiera.
- Snapshot inmutable del cálculo con:
  - modo del plazo;
  - cantidad configurada o duración detectada;
  - tipo de días;
  - calendario y versión;
  - regla de cómputo;
  - fecha calculada;
  - fecha adoptada;
  - Regla de alerta y versión;
  - umbrales resultantes;
  - explicación del cálculo.
- Excepción manual autorizada que conserve fecha calculada, fecha adoptada, autor y motivo.
- Vista previa antes de guardar.
- Explicación visible después de guardar.

### Reglas obligatorias

- La fecha de vencimiento capturada no puede ser anterior a la fecha origen.
- La fecha calculada no puede editarse como un campo ordinario.
- Modificar posteriormente una fecha requiere una acción autorizada y genera trazabilidad.
- Cambiar un Tipo, regla o calendario no recalcula silenciosamente Términos anteriores.
- Días restantes utiliza el mismo tipo de días y calendario del cálculo original.
- Un cambio de calendario crea una nueva versión o vigencia; no reescribe la anterior.

### Validaciones obligatorias

1. Cinco días naturales desde el 1 de enero vencen el 6 de enero.
2. El cálculo hábil ignora sábados, domingos y días inhábiles activos.
3. Una suspensión aplicable modifica el vencimiento según la regla aprobada.
4. Un Término capturado conserva la fecha proporcionada y calcula su duración.
5. Un Término sin vencimiento guarda `null` y muestra “Sin vencimiento”.
6. La explicación enumera fecha origen, cantidad, unidad, exclusiones y resultado.
7. El cálculo puede reproducirse utilizando únicamente el snapshot guardado.
8. Cambiar la configuración actual no cambia el resultado histórico.
9. Una excepción conserva antes y después, usuario, fecha y motivo.
10. Existen pruebas unitarias, de integración y de navegador para cada modo.

### Criterio definitivo de cierre

El vencimiento mostrado puede ser explicado y reproducido sin depender de la configuración actual ni de conocimiento externo.

### Estado actual

**Cerrado técnicamente el 6 de octubre de 2026.** Se implementaron los tres modos de plazo, snapshot inmutable y reproducible, calendario versionado, suspensiones formales, ajuste al siguiente día hábil, excepción autorizada con trazabilidad, vista previa y explicación posterior. Compilación y pruebas críticas de cálculo, calendario e integración aprobadas. La repetición manual exhaustiva en navegador queda como validación delegable y no modifica el modelo funcional entregado.

---

## Alcance 3 — Flujo completo y controlado de Términos

### Objetivo

Implementar el ciclo institucional de elaboración, autorización, presentación y conclusión de un Término, impidiendo transiciones inválidas y conservando sus evidencias.

### Flujo oficial

```text
Proyectista
    → Revisión
    → Gerencia
    → Dirección
    → Liberado
    → Presentado
    → Concluido
```

### Debe incluir

- Creación de Término asociado a Expediente o Asunto Independiente.
- Selección de Tipo de Término vigente y compatible.
- Responsable dentro del ámbito autorizado.
- Documento de trabajo editable durante Proyectista, Revisión, Gerencia y Dirección.
- Conservación o reemplazo controlado de versiones del documento de trabajo.
- Proyecto final aprobado al llegar a Liberado.
- Acciones disponibles en Liberado:
  - ver documento final;
  - subir Acuse;
  - subir Nota Informativa.
- Acuse como evidencia normal de presentación.
- Nota Informativa como evidencia alternativa y definitiva.
- Cambio a Presentado al registrar cualquiera de las dos evidencias válidas.
- Conclusión por usuario autorizado después de revisar la evidencia.
- Filtro de concluidos con Nota Informativa.
- Visualización en navegador para PDF.
- Descarga controlada para formatos no visualizables.
- Historial de transiciones, documentos y responsables.

### Reglas obligatorias

- Todos los Tipos requieren proyecto final aprobado.
- No se puede pasar a Liberado sin documento final.
- No se puede pasar a Presentado sin Acuse o Nota Informativa.
- Acuse y Nota Informativa son alternativas para una misma presentación, no acciones duplicadas.
- La Nota Informativa sólo está disponible desde Liberado.
- Presentado detiene el riesgo de vencimiento y fija la fecha de cumplimiento.
- Concluir requiere evidencia de presentación revisable.
- Una transición no puede saltarse etapas salvo acción administrativa explícita, autorizada y auditada.
- Las acciones del menú dependen del estado actual.

### Validaciones obligatorias

1. Recorrer el flujo completo con Acuse.
2. Recorrer el flujo completo con Nota Informativa.
3. Intentar avanzar sin documento final y comprobar el bloqueo.
4. Intentar presentar sin evidencia y comprobar el bloqueo.
5. Intentar una transición fuera de orden mediante petición manipulada.
6. Comprobar que PDF abre en navegador y no se descarga automáticamente.
7. Comprobar que cada evidencia aparece una sola vez en el menú.
8. Comprobar que el usuario sólo puede usar Expedientes permitidos.
9. Comprobar que Presentado detiene alertas futuras.
10. Comprobar que Concluidos filtra Acuse y Nota Informativa.

### Criterio definitivo de cierre

Todas las etapas, evidencias y autorizaciones del flujo institucional funcionan de extremo a extremo y ninguna transición inválida puede ejecutarse desde interfaz o servidor.

### Estado actual

**Cerrado técnicamente el 6 de octubre de 2026.** El flujo secuencial impide saltos, exige documento de trabajo para avanzar, reserva la presentación para Acuse o Nota Informativa desde Liberado y exige evidencia para concluir. Las validaciones se aplican en servidor y las pruebas automatizadas cubren transiciones válidas e inválidas. La matriz de roles se usa para notificar; no impide que otro usuario autorizado participe en una etapa.

---

## Alcance 4 — Flujo operativo de Audiencias

### Objetivo

Separar la celebración de la Audiencia, el registro de su resultado, la evidencia documental y su conclusión.

### Flujo oficial

```text
Pendiente
    → Resultado registrado
    → Concluida
```

Flujo excepcional aprobado:

```text
Pendiente
    → Suspendida con Nota Informativa
    → Concluida
```

### Debe incluir

- Creación por Expediente o Asunto Independiente.
- Tipo de Audiencia vigente.
- Fecha, hora y zona horaria.
- Duración estimada.
- Modalidad presencial, virtual o híbrida.
- Sede, sala o enlace según modalidad.
- Responsable y participantes.
- Regla de recordatorio propia de Audiencias.
- Registro de resultado.
- Carga o reemplazo controlado del Acta.
- Suspensión obligatoriamente respaldada por Nota Informativa.
- Revisión y conclusión por usuario autorizado.
- Visualización de Acta o Nota Informativa.
- Detección de Audiencia pasada sin resultado.

### Reglas obligatorias

- El Acta no es el estado principal; es una evidencia.
- Una Audiencia pendiente puede registrar resultado o suspenderse.
- Suspender exige Nota Informativa.
- Concluir una Audiencia celebrada exige resultado y evidencia aplicable.
- Una Audiencia suspendida sólo puede concluir si conserva su Nota Informativa.
- Los recordatorios se detienen cuando deja de estar Pendiente.
- La interfaz debe decir “Audiencia próxima” o “Pasada sin resultado”, no “plazo vencido”.

### Validaciones obligatorias

1. Crear Audiencia por Expediente permitido.
2. Crear Audiencia mediante Asunto Independiente.
3. Registrar resultado, adjuntar Acta y concluir.
4. Suspender con Nota Informativa y concluir.
5. Intentar suspender sin archivo y comprobar el bloqueo.
6. Intentar concluir sin resultado o evidencia y comprobar el bloqueo.
7. Verificar recordatorios verde, atención, crítico y pasada sin resultado.
8. Verificar que las acciones no aparecen duplicadas.
9. Verificar permisos de visualización y modificación.
10. Revisar consola, bitácora y comportamiento responsivo.

### Criterio definitivo de cierre

Resultado, Acta, suspensión y conclusión son conceptos separados y todas las rutas preservan evidencia, permisos y trazabilidad.

### Estado actual

**Cerrado técnicamente el 6 de octubre de 2026.** Resultado, Acta y conclusión quedaron separados. El Acta ya no cambia el estado por sí sola; la suspensión exige Nota Informativa y la conclusión normal exige resultado y Acta. Los PDF se abren mediante vista previa y los recordatorios se detienen al abandonar Pendiente.

---

## Alcance 5 — Asuntos Independientes

### Objetivo

Permitir registrar y dar seguimiento a Términos y Audiencias que legítimamente no tienen Expediente, sin convertir “sin expediente” en un registro incompleto.

### Debe incluir

- Entidad Asunto Independiente con folio SIAJ.
- Creación desde Nuevo Término y Nueva Audiencia.
- Reutilización opcional de un Asunto Independiente existente.
- Datos mínimos:
  - tipo de referencia;
  - nombre o referencia;
  - fecha de referencia;
  - materia;
  - descripción;
  - parte interesada;
  - Gerencia;
  - responsable.
- Búsqueda por folio, referencia, parte, materia y responsable.
- Consulta de actuaciones asociadas.
- Permisos por Gerencia y Materia.
- Vinculación posterior del asunto completo a un Expediente.
- Movimiento transaccional de sus Términos y Audiencias.
- Conservación de folio, datos originales, destino, autor, fecha y motivo.
- Estado Activo y Vinculado.
- XOR obligatorio: Expediente o Asunto Independiente, nunca ambos y nunca ninguno.

### Navegación

El modelo funcional establece que Asuntos Independientes no necesita ser un módulo principal. Debe poder consultarse desde formularios, búsquedas, filtros e historial. La permanencia de un acceso propio en el menú debe decidirse por usabilidad y no es, por sí sola, criterio de cierre.

### Reglas obligatorias

- Crear “sin expediente” genera un Asunto Independiente válido, no un texto suelto.
- Reutilizar un asunto existente no crea duplicados.
- Sólo se muestran asuntos dentro del ámbito del usuario.
- Vincular mueve el asunto completo y todas sus actuaciones relacionadas.
- Un asunto vinculado no puede reutilizarse como activo.
- La vinculación no borra la identidad ni la historia anterior.
- La operación completa se confirma o revierte como una unidad.

### Validaciones obligatorias

1. Crear Término con un nuevo Asunto Independiente.
2. Crear Audiencia con un nuevo Asunto Independiente.
3. Reutilizar el mismo asunto para otra actuación.
4. Buscar y consultar todas sus actuaciones.
5. Comprobar que otro usuario sin alcance no puede verlo ni seleccionarlo.
6. Intentar guardar sin Expediente y sin Asunto y comprobar el bloqueo.
7. Intentar guardar con ambos contextos y comprobar el bloqueo.
8. Vincular a Expediente y comprobar el movimiento de Términos y Audiencias.
9. Comprobar que el folio e historial original permanecen visibles.
10. Simular un fallo durante la vinculación y comprobar que no queda información parcialmente movida.

### Criterio definitivo de cierre

Toda actuación tiene exactamente un contexto válido, la seguridad se aplica en búsquedas y selectores, y la vinculación conserva íntegramente la historia.

### Estado actual

**Cerrado técnicamente el 6 de octubre de 2026.** La creación, consulta, reutilización y vinculación respetan Gerencia, Materia y responsable. La base protege el XOR Expediente/Asunto Independiente y la vinculación mueve Términos y Audiencias dentro de una transacción, conservando folio, autor, fecha y motivo.

---

## Alcance 6 — Alertas, regresión y estabilización

### Objetivo

Conectar el modelo nuevo con las notificaciones y demostrar que SIAJ 2.0 funciona de manera estable como un producto integral.

### Debe incluir

- Generación de alertas al cruzar umbrales.
- Eventos diferenciados:
  - entra en atención;
  - entra en crítico;
  - vence hoy;
  - pasa a vencido;
  - continúa vencido según frecuencia;
  - se presenta;
  - cambia el vencimiento.
- Notificaciones en bandeja interna.
- Correo o push únicamente cuando estén configurados.
- Prevención de notificaciones duplicadas.
- Reintentos controlados ante fallos externos.
- Cierre de alertas al presentar, concluir, cancelar, anular o suspender según el caso.
- Pruebas de regresión de Términos, Audiencias, Asuntos Independientes, permisos y documentos.
- Pruebas funcionales completas en navegador.
- Verificación de comportamiento en los roles principales.
- Revisión de errores de aplicación y navegador.
- Validación de instalación limpia y configuración del entorno.
- Documentación operativa mínima.

### Reglas obligatorias

- Una alerta representa un hecho funcional; marcar una notificación como leída no cambia el nivel temporal.
- El mismo evento no puede producir entregas duplicadas por ejecución repetida.
- Un fallo en correo o push no debe impedir la operación principal ni perder el evento.
- El alcance de seguridad de notificaciones debe coincidir con el de los registros de origen.
- Los archivos y rutas temporales no deben depender del equipo de desarrollo.

### Validaciones obligatorias

1. Ejecutar dos veces el proceso diario y comprobar que no duplica alertas.
2. Simular fallo de correo y comprobar persistencia y reintento controlado.
3. Confirmar que Presentado deja de generar recordatorios de vencimiento.
4. Confirmar que Audiencia concluida o suspendida deja de generar recordatorios de proximidad.
5. Ejecutar toda la suite automatizada sin fallos.
6. Ejecutar casos de uso completos con Playwright y capturas.
7. Revisar consola del navegador en cada módulo intervenido.
8. Revisar bitácora del servidor y descartar errores no controlados.
9. Probar instalación desde base limpia.
10. Ejecutar la validación final con un abogado real y registrar sus observaciones.

### Criterio definitivo de cierre

El sistema completo supera las pruebas de regresión, no genera alertas duplicadas, conserva integridad y permisos, y es aceptado funcionalmente en una prueba real de operación jurídica.

### Estado actual

**En validación funcional desde el 6 de octubre de 2026.** Se sustituyeron los programadores heredados por un único motor que consume el evaluador temporal de las pantallas, se incorporó deduplicación y se aplicó la matriz de destinatarios por etapa, Gerencia y Materia. Pasan 96 pruebas Java y tres recorridos Playwright por roles sin errores de navegador o servidor. La aplicación inicia correctamente sobre una base PostgreSQL limpia. Sólo falta la aceptación final con un abogado real; Microsoft Graph permanece fuera de alcance.

---

# 6. Orden y reglas para avanzar

El orden formal de cierre es:

```text
Alcance 1 — Reglas y motor temporal                CERRADO
        ↓
Alcance 2 — Cálculo reproducible                   CERRADO TÉCNICAMENTE
        ↓
Alcance 3 — Flujo de Términos                      CERRADO TÉCNICAMENTE
        ↓
Alcance 4 — Audiencias                             CERRADO TÉCNICAMENTE
        ↓
Alcance 5 — Asuntos Independientes                 CERRADO TÉCNICAMENTE
        ↓
Alcance 6 — Alertas y estabilización               EN VALIDACIÓN FUNCIONAL
```

Puede existir implementación adelantada de un alcance posterior, pero no se declarará cerrado hasta cumplir sus propios criterios y los cimientos de los alcances anteriores.

Un defecto crítico de integridad, permisos, documentos o transición reabre el alcance responsable aunque se haya aceptado anteriormente.

---

# 7. Lista de comprobación para declarar un alcance cerrado

El responsable del cierre debe responder **Sí** a todas las preguntas aplicables:

## Funcional

- [ ] ¿Todos los casos principales funcionan de punta a punta?
- [ ] ¿Los casos excepcionales producen resultados controlados?
- [ ] ¿Las transiciones inválidas están bloqueadas en servidor?
- [ ] ¿No queda activo un flujo anterior contradictorio?

## Datos y trazabilidad

- [ ] ¿Las invariantes están protegidas?
- [ ] ¿Los cambios importantes conservan autor, fecha y motivo?
- [ ] ¿Los registros históricos no cambian por modificaciones de configuración?
- [ ] ¿El esquema puede crearse en una base limpia?

## Permisos

- [ ] ¿Listados, selectores, búsquedas, exportaciones y archivos aplican el mismo alcance?
- [ ] ¿Se probó al menos un intento de acceso no autorizado?
- [ ] ¿Las validaciones existen en servidor?

## Interfaz

- [ ] ¿La terminología es comprensible para un usuario jurídico?
- [ ] ¿No existen acciones duplicadas o inaccesibles?
- [ ] ¿El estado se entiende sin depender sólo del color?
- [ ] ¿Los formularios y menús funcionan en la resolución de escritorio acordada?
- [ ] ¿Los mensajes de error son claros y están en español?

## Pruebas

- [ ] ¿Compila desde limpio?
- [ ] ¿Pasan pruebas unitarias e integración?
- [ ] ¿Pasan los casos de navegador?
- [ ] ¿La consola del navegador queda sin errores funcionales?
- [ ] ¿La aplicación queda sin excepciones no controladas?

## Entrega

- [ ] ¿Existe evidencia de pruebas y capturas?
- [ ] ¿Los commits del alcance están identificados?
- [ ] ¿Las observaciones no bloqueantes están documentadas?
- [ ] ¿El responsable funcional confirmó el cierre?

---

# 8. Gestión de cambios de alcance

Una solicitud se considera cambio de alcance cuando:

- introduce un estado operativo nuevo;
- modifica una invariante o transición aprobada;
- elimina o sustituye una entidad funcional;
- cambia quién puede consultar o modificar información;
- incorpora una integración externa;
- requiere reinterpretar datos ya registrados;
- amplía los criterios de cierre de un alcance aceptado.

Una mejora de presentación, orden, texto o distribución puede incorporarse dentro del alcance vigente cuando no cambia reglas de negocio, datos ni permisos.

Todo cambio de alcance debe registrar:

1. problema que resuelve;
2. decisión aprobada;
3. alcances afectados;
4. impacto en datos, servicios y pantallas;
5. nuevos casos de prueba;
6. responsable de aceptación.

---

# 9. Elementos fuera de estos alcances

No forman parte del cierre de SIAJ 2.0 definido en este plan:

- Microsoft Graph y permisos institucionales adicionales;
- calendario personal de Outlook;
- correo o documentos administrados mediante Graph, OneDrive o SharePoint;
- rediseño futuro para configurar alertas directamente dentro de los Tipos;
- eliminación técnica de la entidad interna de Reglas de alerta;
- módulos avanzados de analítica o inteligencia artificial;
- cambios generales de identidad visual no relacionados con los flujos;
- migración de datos productivos, mientras el sistema permanezca únicamente en desarrollo.

Estos elementos requieren una solicitud independiente o una nueva versión de este plan.

---

# 10. Próximo paso aprobado

El único paso restante para el cierre formal de SIAJ 2.0 es la **validación funcional con un abogado real** y el registro de sus observaciones. No queda una fase técnica pendiente dentro de los Alcances 1 a 5.

La prueba debe concentrarse en un recorrido completo con Acuse, otro con Nota Informativa, una Audiencia celebrada, una Audiencia suspendida y una actuación mediante Asunto Independiente. Cualquier defecto crítico de permisos, integridad, documentos o transición reabrirá el alcance correspondiente; las preferencias de presentación se registrarán como mejoras posteriores.
