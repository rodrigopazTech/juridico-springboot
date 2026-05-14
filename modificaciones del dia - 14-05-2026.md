# Reporte de Modificaciones - 14 de Mayo de 2026

## Objetivo Principal
Optimización del sistema de notificaciones para eliminar la duplicidad de mensajes y mejorar la claridad de la información enviada a los niveles jerárquicos (Jefes, Gerentes y Directores).

## Cambios Realizados

### 1. Refactorización de `NotificacionService.java`
- **Modo Silencioso:** Se añadió un parámetro booleano `silenciarSuperiores` al método `crearNotificacion`. Esto permite que procesos que ya manejan sus propios destinatarios no disparen la jerarquía automática, evitando duplicados.
- **Contexto de Autor (Actor):** El servicio ahora recibe quién realiza la acción.
- **Mensajes Inteligentes para Superiores:** Los jefes ahora reciben mensajes detallados como: *"El [Rol] [Nombre] ha asignado un término al [Rol] [Nombre] en el expediente: [Número]"*.
- **Filtro de Eco:** Se evita que el autor de una acción reciba una notificación automática de su propio movimiento a través de la jerarquía.

### 2. Eliminación de Duplicados en Flujos Críticos
- **`TerminosController.java`**: Se actualizó el método `notificarCambioDeEtapa` para usar el modo silencioso, ya que la lógica de transición ya define a los interesados.
- **`NotificacionAutomaticaService.java`**: Los recordatorios diarios de vencimientos ahora se envían de forma directa sin duplicar avisos a superiores.
- **`AudienciaService.java`**: Se ajustó la notificación de actas disponibles y creación de audiencias para incluir al autor y evitar redundancias.

### 3. Mantenimiento y Estabilidad
- **`AudienciaNotificacionScheduler.java`**: Se reescribió el método de recordatorios de audiencias para corregir errores de sintaxis y asegurar la compatibilidad con la nueva firma del servicio de notificaciones.
- **Validación de Datos:** Se mejoró el manejo de campos como `es_virtual` y `id` para evitar errores de tipo o nulos en las notificaciones.

---
**Estado:** Finalizado y Confirmado.
**Rama:** `notificaciones-etapa6`
