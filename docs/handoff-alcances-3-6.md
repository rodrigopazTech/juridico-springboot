# Handoff — Alcances 3 a 6 de SIAJ 2.0

## Resultado

Los Alcances 3, 4 y 5 quedaron cerrados técnicamente el 6 de octubre de 2026. El Alcance 6 quedó implementado y en validación funcional; únicamente requiere la aceptación final con un abogado real.

## Alcance 3 — Términos

- Flujo secuencial Proyectista → Revisión → Gerencia → Dirección → Liberado.
- Presentación exclusivamente desde Liberado mediante Acuse o Nota Informativa.
- Conclusión exclusivamente desde Presentado y con evidencia conservada.
- Bloqueo en servidor de saltos y transiciones genéricas indebidas.
- Roles operativos flexibles; la etapa determina destinatarios de notificación, no permisos rígidos.

Commit: `fa301cd`.

## Alcance 4 — Audiencias

- Resultado, Acta y conclusión separados.
- Flujo Pendiente → Resultado registrado → Concluida.
- Flujo excepcional Pendiente → Suspendida con Nota Informativa → Concluida.
- El Acta no altera el estado por sí sola.
- Vista previa de PDF y validaciones de acceso para Expediente o Asunto Independiente.

Commit: `59be72f`.

## Alcance 5 — Asuntos Independientes

- Búsqueda por folio, referencia, parte, Materia y responsable.
- Alcance por Gerencia, Materia y responsable.
- XOR Expediente/Asunto Independiente protegido en base de datos.
- Vinculación transaccional que mueve Términos y Audiencias y conserva trazabilidad.

Commit: `76c9083`.

## Alcance 6 — Estabilización

- Motor único diario de notificaciones conectado al evaluador temporal usado por las pantallas.
- Programadores heredados desactivados para evitar cálculos y entregas paralelas.
- Deduplicación por usuario, evento, entidad y título.
- Matriz de destinatarios por etapa, Gerencia y Materia.
- Jefatura administra Reglas y Tipos dentro de sus Materias; Gerencia dentro de su Gerencia.
- Calendario institucional reservado a Dirección y Administración.
- Consulta de Plazos y alertas disponible para todos los usuarios autenticados.

## Evidencia ejecutada

- `mvn -q test`: 96 pruebas, 0 fallos, 0 errores.
- `node --check src/main/resources/static/js/audiencias.js`: aprobado.
- Playwright en Administrador, Jefe de Departamento y Abogado QA: 3 recorridos aprobados.
- Consola del navegador: sin errores en Términos, Audiencias, Asuntos Independientes y Plazos y alertas.
- Bitácora del servidor estable: sin excepciones durante los recorridos.
- Arranque aislado sobre una base PostgreSQL limpia: aprobado; la base temporal se eliminó al terminar.

El reporte navegable está en `reports/report.html` y las capturas se conservan en `tests_e2e/reports/screenshots`.

## Pendiente externo

Ejecutar una sesión de aceptación con un abogado real. Debe cubrir:

1. Término completo con Acuse.
2. Término completo con Nota Informativa.
3. Audiencia celebrada con resultado y Acta.
4. Audiencia suspendida con Nota Informativa.
5. Término y Audiencia mediante Asunto Independiente.

Microsoft Graph y el rediseño futuro de Reglas de alerta continúan fuera de estos alcances.
