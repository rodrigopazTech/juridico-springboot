# Diccionario de Datos - Sistema Jurídico SIAJ

## Descripción General del Sistema

El **Sistema Jurídico SIAJ** es una aplicación diseñada para la gestión integral de procedimientos legales, incluyendo la administración de usuarios, materias jurídicas, gerencias, audiencias, términos procesales, expedientes y documentación asociada.

---

## Tablas del Sistema

### 1. Tabla: `usuarios`

**Descripción:** Almacena la información de los usuarios del sistema jurídico. Cada usuario representa a un profesional involucrado en los procedimientos legales (abogados, administradores, secretarios, etc.).

| Campo | Tipo de Dato | Nullable | Descripción |
|-------|--------------|----------|-------------|
| `id` | INTEGER | NO | Identificador único y autoincremental del usuario. Es la clave primaria. |
| `nombre_completo` | VARCHAR(200) | NO | Nombre completo del usuario. |
| `email` | VARCHAR(100) | NO | Correo electrónico único del usuario. Se utiliza para el inicio de sesión. |
| `password_hash` | VARCHAR(255) | NO | Contraseña cifrada del usuario (hash). |
| `rol` | VARCHAR(50) | NO | Rol del usuario en el sistema (ej: ADMIN, ABOGADO, JEFE_DEPTO, SECRETARIO, etc.). |
| `gerencia_id` | INTEGER | SI | Identificador de la gerencia a la que pertenece el usuario. Relación muchos a uno con la tabla `gerencias`. |
| `activo` | BOOLEAN | NO | Indica si el usuario está activo en el sistema. Valor por defecto: true. |
| `ultimo_acceso` | DATETIME | SI | Fecha y hora del último acceso del usuario al sistema. |
| `created_at` | DATETIME | SI | Fecha y hora de creación del registro del usuario. |
| `updated_at` | DATETIME | SI | Fecha y hora de la última modificación del registro del usuario. |

**Índices:**
- `idx_usuarios_email` - Índice en la columna `email` para búsquedas rápidas.
- `idx_usuarios_rol` - Índice en la columna `rol` para filtrar por rol.

**Relaciones:**
- Relación muchos a uno con `gerencias` (un usuario pertenece a una gerencia).
- Relación muchos a muchos con `materias` a través de la tabla `usuario_materias`.

---

### 2. Tabla: `usuario_materias` (Tabla de Relación - Junction Table)

**Descripción:** Tabla de relación muchos a muchos que associa usuarios con materias jurídicas especializadas. Permite que un usuario tenga asignadas múltiples materias y que una materia sea atendida por múltiples usuarios. Esta relación es fundamental para el sistema de asignación de casos y filtrado de información.

| Campo | Tipo de Dato | Nullable | Descripción |
|-------|--------------|----------|-------------|
| `usuario_id` | INTEGER | NO | Identificador del usuario. Es una clave foránea que referencia `usuarios.id`. |
| `materia_id` | INTEGER | NO | Identificador de la materia jurídica. Es una clave foránea que referencia `materias.id`. |

**Clave Primaria:** Combinación de (`usuario_id`, `materia_id`)

**Descripción del Funcionalidad:**
- Esta tabla define qué materias jurídicas puede atender cada usuario.
- Es utilizada para:
  - Asignar materias específicas a abogados.
  - Filtrar audiencias, términos y expedientes por materia.
  - Determinar qué usuarios pueden ver o gestionar determinados casos.
  - Asignar automáticamente notificaciones a los usuarios correspondientes según la materia del caso.

**Relaciones:**
- Relación muchos a uno con `usuarios` (un usuario tiene muchas materias).
- Relación muchos a uno con `materias` (una materia puede ser atendida por muchos usuarios).

---

### 3. Tabla: `materias` (Referencia Adicional)

**Descripción:** Catálogo de especialidades o ramas del derecho que pueden ser asignadas a usuarios y asociadas a expedientes.

| Campo | Tipo de Dato | Nullable | Descripción |
|-------|--------------|----------|-------------|
| `id` | INTEGER | NO | Identificador único y autoincremental de la materia. |
| `nombre` | VARCHAR(100) | NO | Nombre de la materia jurídica (ej: Penal, Civil, Laboral, Familiar, etc.). |
| `gerencia_id` | INTEGER | NO | Identificador de la gerencia a la que pertenece la materia. |
| `descripcion` | TEXT | SI | Descripción detallada de la materia. |
| `activo` | BOOLEAN | NO | Indica si la materia está activa en el sistema. |
| `created_at` | DATETIME | SI | Fecha de creación del registro. |
| `updated_at` | DATETIME | SI | Fecha de última modificación. |

---

## Notas

- La tabla `usuario_materias` es una tabla de intersección (junction table) generada automáticamente por JPA/Hibernate mediante la anotación `@JoinTable` en la entidad `Usuario`.
- El sistema utiliza esta relación para filtrar contenidos por materia y usuario, permitiendo una gestión más granular de los procedimientos legales.
- Los roles de usuario determinan las operaciones que cada usuario puede realizar en el sistema.

