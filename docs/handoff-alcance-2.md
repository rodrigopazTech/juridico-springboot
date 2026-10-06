# Handoff — cierre técnico del Alcance 2

## Resultado

El Alcance 2 quedó implementado y cerrado técnicamente el 6 de octubre de 2026.

Incluye:

- cálculo en días naturales y hábiles sin contar la fecha origen;
- vencimiento calculado, capturado y sin vencimiento;
- ajuste opcional al siguiente día hábil;
- calendario institucional versionado;
- días inhábiles y suspensiones formales con motivo, estado y autor;
- snapshot inmutable de plazo, calendario y Regla de alerta;
- reproducción histórica sin consultar la configuración actual;
- vista previa y explicación visible del cálculo;
- excepción autorizada de fecha con valor anterior, nuevo valor, autor, fecha y motivo;
- días restantes calculados con el calendario congelado del registro.

## Evidencia ejecutada

- `mvn -q -DskipTests compile`: aprobado.
- `CalculadoraPlazoServiceTest`: aprobado.
- `CalendarioInstitucionalServiceTest`: aprobado.
- `Alcance1PantallasIntegrationTest`: aprobado con las nuevas pantallas del Alcance 2.
- Se verificaron visualmente acceso a Plazos y alertas, calendario versionado, alta de suspensión y vista previa explicada de un plazo calculado.

## Validación delegable pendiente

No requiere desarrollo adicional. Un agente de pruebas o el usuario puede repetir en navegador:

1. crear un Término calculado y abrir “Cómo se calculó”;
2. crear un Término capturado y confirmar que conserva la fecha indicada;
3. crear un Término sin vencimiento y confirmar que no genera fecha ni alerta;
4. autorizar una excepción con Jefatura y comprobar que Abogado no dispone de esa acción;
5. revisar consola del navegador y bitácora durante esos cuatro recorridos.

## Siguiente trabajo

Continuar con el Alcance 3. No rediseñar ni volver a implementar el motor de plazo salvo que una prueba demuestre una regresión concreta.

Cambios ajenos deliberadamente excluidos del commit del Alcance 2:

- eliminación de `config/siaj-microsoft.example.properties`;
- actualización de Bouncy Castle en `pom.xml`.
