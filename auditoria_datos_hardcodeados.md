# Auditoría de Datos Hardcodeados - Sistema Jurídico

Este documento detalla los hallazgos de la auditoría realizada el 16 de febrero de 2026, con el fin de identificar datos estáticos, filtros y lógicas que deberían ser extraídos a la base de datos o centralizados en Enums/Catálogos.

## 🔴 Prioridad Alta: Riesgo de Inconsistencia y Errores de Flujo

### 1. Flujo de Estados de Términos (Strings Sueltos)
**Ubicación:** `TerminosController.java` y asociados.
**Descripción:** El ciclo de vida de un Término se maneja mediante cadenas de texto literales en múltiples lugares. Un error tipográfico en cualquier lugar rompería la lógica de negocio.
- **Hallazgos:**
    - L100: Lista de estatus para filtros.
    - L350-360: Lógica de transición de estados (`calcularSiguienteEstado`).
    - L55-61: Matriz de permisos por etapa.
    - L226-235: Identificación de destinatarios para notificaciones.
- **Sugerencia:** Crear `enum EstatusTermino` y refactorizar todas las comparaciones y asignaciones.

### 2. Estados de Audiencia y Agenda
**Ubicación:** `AudienciasController.java` (L195), `AgendaService.java` (L33).
**Descripción:** Se usan strings como `"PENDIENTE"`, `"Presentado"` y `"Concluido"` para filtrar y asignar estados.
- **Sugerencia:** Centralizar en `enum EstatusAudiencia`.

---

## 🟡 Prioridad Media: Deuda Técnica y Mantenibilidad

### 3. Filtros Inteligentes de Periodos
**Ubicación:** `barra-filtros.html` (L25-29), `AudienciasController.java` (L112-117, L382-399).
**Descripción:** Las opciones de tiempo (`HOY`, `MANANA`, `SEMANA`, `MES`) están duplicadas tanto en el frontend (select options) como en el backend (switch cases).
- **Sugerencia:** Centralizar estas constantes en un objeto de configuración o Enum compartido para evitar discrepancias entre UI y lógica.

### 4. Niveles de Permiso de Colaboradores
**Ubicación:** `AudienciasController.java` (L220).
**Descripción:** El nivel `"LECTURA_TOTAL"` se asigna como un string estático al crear colaboradores temporales.
- **Sugerencia:** Integrar con un Enum de niveles de acceso/permisos.

### 5. Configuración de Gráficos del Dashboard
**Ubicación:** `DashboardService.java` (L53).
**Descripción:** El gráfico de estatus de expedientes excluye explícitamente el estado `CONCLUIDO` mediante una lista hardcodeada: `Arrays.asList(EtapaProcesal.TRAMITE, EtapaProcesal.LAUDO, EtapaProcesal.FIRME)`.
- **Sugerencia:** Permitir que los estados a graficar sean configurables o que incluyan todos los valores del Enum `EtapaProcesal`.

---

## 🟢 Prioridad Baja: Datos Semilla (Seed Data)

### 6. Inicialización de Catálogos
**Ubicación:** `DataInitializer.java`.
**Descripción:** Contiene la creación inicial de Gerencias, Materias, Tipos de Expediente y Estados. 
- **Nota:** Esto es aceptable para inicializar el sistema, pero cualquier cambio en estos catálogos requiere intervención del desarrollador si no se expone una interfaz de administración.

---

## 📊 Resumen para Linear AI

| Tarea | Impacto | Componente |
|---|---|---|
| Implementar Enum para Estatus de Términos | Crítico | Backend / Seguridad |
| Implementar Enum para Estatus de Audiencias | Crítico | Backend / Agenda |
| Centralizar Definición de Periodos de Filtro | Medio | UI / Backend |
| Migrar Permisos de Colaboradores a Enums | Medio | Seguridad |
| Refactorizar Metríficas de Dashboard | Bajo | Dashboard |
