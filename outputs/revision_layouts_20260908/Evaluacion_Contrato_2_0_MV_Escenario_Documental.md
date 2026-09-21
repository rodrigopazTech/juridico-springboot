# Evaluación del contrato 2.0-MV frente al escenario documental real

Fecha: 9 de septiembre de 2026  
Contrato evaluado: `2.0-MV`

## Dictamen

El modelo actual del sistema sí soporta el escenario operativo descrito, pero el contrato de migración `2.0-MV` no lo cubre por completo.

Los cinco layouts actuales son suficientes para usuarios con una sola gerencia, una sola Acta por audiencia y un solo Acuse por término. No son suficientes para migrar los múltiples documentos adicionales de un expediente que se administran en el Gestor Documental y que no pertenecen a una audiencia o término.

Si esos documentos históricos forman parte del alcance de la migración, se requiere un layout adicional de documentos de expediente. Este layout no representa un cambio arquitectónico: alimentaría el modelo ya existente de `documentos`, asociado directamente al expediente mediante `expediente_id` y opcionalmente a una carpeta.

## Evaluación por condición

| Condición operativa | Cobertura del contrato 2.0-MV | Soporte actual del sistema | Resultado |
|---|---|---|---|
| Ningún usuario pertenece a múltiples gerencias | `Layout_Usuarios_V2_MV` contiene un solo `gerencia_codigo`. | Aunque la entidad permite un conjunto de gerencias, el flujo de compatibilidad puede asignar una sola. | **Suficiente para el escenario declarado.** No se requiere la relación R02 ni otra hoja de usuario-gerencia. |
| Una audiencia tiene una sola Acta | El layout de Audiencias contiene un único `acta_archivo`, acompañado por estado y motivo de faltante. | `Audiencia` contiene un único valor `acta_documento`. | **Suficiente.** La restricción operativa coincide con la cardinalidad del contrato. |
| Un término tiene un solo Acuse | El layout de Términos contiene un único `acuse_archivo`, acompañado por estado y motivo de faltante. | `Termino` contiene un único valor `acuse` y su nombre. | **Suficiente.** No se necesita un manifiesto multianexo para el Acuse. |
| Un expediente puede tener múltiples documentos adicionales | El layout de Expedientes no contiene filas ni una sección repetible para documentos. | La tabla/entidad `documentos` admite múltiples registros con el mismo `expediente_id`. | **Brecha de migración.** El sistema puede operarlos, pero el contrato no permite declararlos ni conciliarlos. |
| Los documentos adicionales no están ligados a término o audiencia | El contrato sólo contempla Acta y Acuse dentro de sus layouts procesales. | `Documento` se relaciona directamente con el expediente y opcionalmente con una carpeta; no exige término ni audiencia. | **El modelo es compatible, pero falta la entrada de migración correspondiente.** |

## Decisión sobre el layout adicional

Para el escenario completo planteado, **sí se requiere un layout adicional de documentos de expediente** cuando los documentos históricos deban quedar disponibles en el Gestor Documental al terminar la migración.

El layout adicional debe representar una fila por archivo y referenciar al expediente ya migrado mediante `expediente_clave_origen`. Su carga debe ejecutarse después de Expedientes. Sólo necesita mapear información que el Gestor Documental ya maneja: archivo o ruta de entrega, nombre, descripción y carpeta opcional, además de los datos de control de migración necesarios para validar y conciliar la carga.

No se requieren relaciones nuevas con Audiencias o Términos, porque esa ausencia de vínculo coincide con el escenario real y con el modelo actual.

Si los documentos adicionales se excluyen expresamente del alcance y serán cargados manualmente después, los cinco layouts actuales pueden considerarse suficientes, pero la migración no dejará completo el expediente documental. El Gestor Documental aparecerá vacío o parcial hasta que se realice esa carga posterior.

## Conclusión de suficiencia

- **Contrato 2.0-MV para datos procesales y documentos principales:** suficiente bajo las cardinalidades declaradas.
- **Contrato 2.0-MV para el expediente documental histórico completo:** insuficiente.
- **Cambio requerido:** agregar un layout de migración para documentos de expediente, usando las estructuras existentes del Gestor Documental.
- **Cambios arquitectónicos:** ninguno.
