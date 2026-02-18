# Repositorios del Sistema Jurídico

Este documento describe los repositorios de datos utilizados en el sistema jurídico, organizados por módulos. Todos los repositorios extienden `JpaRepository` de Spring Data JPA, proporcionando operaciones CRUD básicas y métodos personalizados para consultas específicas.

## Estructura de los Repositorios

Los repositorios están organizados en las siguientes categorías:

- **Catalogo**: Repositorios para datos de catálogo (listas de valores fijos)
- **Expediente**: Repositorios relacionados con expedientes y sus componentes
- **General**: Repositorios para funcionalidades generales del sistema
- **Usuarios**: Repositorios para gestión de usuarios y funcionalidades relacionadas
- **procesal**: Repositorios para procesos judiciales (audiencias, términos, etc.)

## Catalogo

### GerenciaRepository
- **Entidad**: `Gerencia`
- **Propósito**: Gestiona las gerencias del sistema
- **Métodos clave**:
  - `findByActivoTrueOrderByNombreAsc()`: Obtiene gerencias activas ordenadas por nombre

### MateriaRepository
- **Entidad**: `Materia`
- **Propósito**: Gestiona las materias jurídicas

### OrganoJurisdiccionalRepository
- **Entidad**: `OrganoJurisdiccional`
- **Propósito**: Gestiona los órganos jurisdiccionales

### TipoAudienciaRepository
- **Entidad**: `TipoAudiencia`
- **Propósito**: Gestiona los tipos de audiencia

## Expediente

### ExpedienteRepository
- **Entidad**: `Expediente`
- **Propósito**: Gestiona los expedientes del sistema
- **Métodos clave**:
  - `findByNumeroIgnoreCase(String numero)`: Busca expediente por número (case insensitive)
  - `findByAbogadoResponsableId(Integer abogadoId, Pageable pageable)`: Filtra expedientes por abogado responsable
  - `buscarPorPalabraClaveYGerencia(String keyword, Integer gerenciaId, Pageable pageable)`: Búsqueda "Google-like" por palabra clave y gerencia

### ActividadExpedienteRepository
- **Entidad**: `ActividadExpediente`
- **Propósito**: Gestiona las actividades relacionadas con expedientes

### ColaboradorExpedienteRepository
- **Entidad**: `ColaboradorExpediente`
- **Propósito**: Gestiona los colaboradores asignados a expedientes


## General

### ComentarioRepository
- **Entidad**: `Comentario`
- **Propósito**: Gestiona comentarios en el sistema
- **Métodos clave**:
  - `findByEntidadTipoAndEntidadIdOrderByCreatedAtDesc(String tipo, String id)`: Obtiene comentarios por tipo de entidad e ID, ordenados por fecha de creación descendente

## Usuarios

### UsuarioRepository
- **Entidad**: `Usuario`
- **Propósito**: Gestiona los usuarios del sistema
- **Métodos clave**:
  - `findByEmail(String email)`: Busca usuario por email
  - `existsByEmail(String email)`: Verifica si existe un usuario con el email dado
  - `findByGerenciaIdAndActivoTrue(Integer gerenciaId)`: Obtiene usuarios activos de una gerencia específica

### EventoCalendarioRepository
- **Entidad**: `EventoCalendario`
- **Propósito**: Gestiona eventos del calendario de usuarios

### NotificacionRepository
- **Entidad**: `Notificacion`
- **Propósito**: Gestiona notificaciones para usuarios

### RecordatorioRepository
- **Entidad**: `Recordatorio`
- **Propósito**: Gestiona recordatorios para usuarios

## procesal

### AudienciaRepository
- **Entidad**: `Audiencia`
- **Propósito**: Gestiona audiencias judiciales
- **Métodos clave**:
  - `findByExpedienteId(UUID expedienteId)`: Obtiene audiencias por expediente
  - `findByFechaAudienciaAndEstatusAudienciaNot(LocalDate fecha, String estatusExcluido)`: Obtiene audiencias de una fecha específica excluyendo un estatus
  - `existsByExpedienteIdAndAbogadoCompareceIdAndEstatusAudienciaNot(UUID expedienteId, Integer abogadoId, String estatusExcluido)`: Verifica existencia de audiencia activa delegada

### AudienciaDesahogadaRepository
- **Entidad**: `AudienciaDesahogada`
- **Propósito**: Gestiona audiencias desahogadas (concluidas)

### TerminoRepository
- **Entidad**: `Termino`
- **Propósito**: Gestiona términos judiciales

### TerminoPresentadoRepository
- **Entidad**: `TerminoPresentado`
- **Propósito**: Gestiona términos presentados

## Notas Técnicas

- Todos los repositorios están anotados con `@Repository` para inyección de dependencias
- Utilizan Spring Data JPA para generación automática de consultas
- Los métodos personalizados siguen las convenciones de nomenclatura de Spring Data
- Algunos repositorios incluyen consultas JPQL personalizadas con `@Query`
- Se utilizan tipos de datos como `UUID` para identificadores únicos y `Pageable` para paginación

## Dependencias

Los repositorios dependen de las siguientes entidades JPA ubicadas en el paquete `com.juridico.sistema_juridico.Entity`:

- `catalogo.*`
- `expediente.*`
- `general.*`
- `procesal.*`
- `usuario.*`
