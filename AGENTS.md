## 🗺️ 5. Estado de Arquitectura de Base de Datos (Camino B Implementado)

El proyecto completó exitosamente la transición al **Camino B** (Migraciones formales Flyway + Hibernate `validate`):

1. **Única Fuente de Verdad:**
   - La estructura completa de la base de datos reside en migraciones versionadas bajo `src/main/resources/db/migration/`:
     - `V1__baseline_siaj.sql`: Baseline de esquema DDL completo con restricciones XOR, foreign keys, secuencias e índices.
     - `V2__catalogo_datos_iniciales.sql`: Carga idempotente de perfiles de alerta y recordatorios.
   - Tabla de control: `public.flyway_schema_history`.

2. **Validación de Hibernate:**
   - Configuración activa: `spring.jpa.hibernate.ddl-auto=validate`.
   - Hibernate ya no altera tablas ni crea restricciones en tiempo de ejecución. Si el esquema difiere de las entidades, la aplicación falla explícitamente en el arranque.

3. **Retiro de DDL en el Arranque Java:**
   - `DatabaseSchemaCompatibilityInitializer`: Deshabilitado por defecto mediante `@ConditionalOnProperty(name = "app.legacy-schema-compatibility.enabled", havingValue = "true")`.
   - `AtencionTemporalInitializer`: Deshabilitado por defecto mediante `@ConditionalOnProperty(name = "app.seed.temporal-atencion.enabled", havingValue = "true")`.

4. **Estrategia Centralizada de Normalización de Códigos (`VARCHAR(50)`):**
   - Implementado en `com.juridico.sistema_juridico.util.CodigoNormalizer`.
   - Aplica normalización Diacrítica (NFD), mayúsculas, formato seguro y límite estricto de $\le 50$ caracteres con hash SHA-256 corto ante strings extensos o sufijos ID (`base_id`).
   - Cobertura completa de pruebas unitarias en `CodigoNormalizerTest` (102 tests passing).
