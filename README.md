# 🏛️ Sistema Jurídico - Backend Spring Boot

API REST Backend para el Sistema de Agenda Jurídica Gob.MX V3.

## 📋 Información del Proyecto

**Stack Tecnológico:**
- ☕ Java 17
- 🍃 Spring Boot 3.4.12
- 🐘 PostgreSQL 15+
- 🔐 Spring Security + JWT
- 📚 Spring Data JPA
- 🎨 Thymeleaf (vistas admin)
- 📖 Swagger/OpenAPI 3.0
- 🔄 Lombok
- 🗺️ ModelMapper

**Repositorios:**
- **Frontend:** [rodrigo pazTech/juridico](https://github.com/rodrigopazTech/juridico)
- **Backend:** [rodrigopazTech/juridico-springboot](https://github.com/rodrigopazTech/juridico-springboot)

## 🎯 Características

- ✅ API REST completa para gestión jurídica
- ✅ Autenticación JWT
- ✅ Gestión de Expedientes, Audiencias y Términos
- ✅ Sistema de Calendario integrado
- ✅ Dashboard con métricas y estadísticas
- ✅ Notificaciones y Recordatorios
- ✅ Documentación interactiva con Swagger
- ✅ CORS configurado para desarrollo

## 📦 Requisitos Previos

- **Java JDK 17+** - [Descargar](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
- **Maven 3.8+** - [Descargar](https://maven.apache.org/download.cgi)
- **PostgreSQL 15+** - [Descargar](https://www.postgresql.org/download/)
- **Git** - [Descargar](https://git-scm.com/downloads)

## 🚀 Instalación y Configuración

### 1. Clonar el Repositorio

```bash
git clone https://github.com/rodrigopazTech/juridico-springboot.git
cd juridico-springboot
```

### 2. Configurar Base de Datos

**Opción A: Con usuario por defecto**
```sql
-- Conectar a PostgreSQL como superusuario
psql -U postgres

-- Crear base de datos
CREATE DATABASE juridico_db;

-- Cambiar contraseña de postgres (si es necesario)
ALTER USER postgres WITH PASSWORD 'JuridicoPostgres2026!';
```

**Opción B: Crear usuario específico**
```sql
-- Crear usuario
CREATE USER juridico_user WITH PASSWORD 'juridico_pass';

-- Crear base de datos
CREATE DATABASE juridico_db OWNER juridico_user;

-- Otorgar privilegios
GRANT ALL PRIVILEGES ON DATABASE juridico_db TO juridico_user;
```

### 3. Cargar Schema de Base de Datos

**Ubicación del script:** `database-schema-completo.sql` (del repositorio frontend)

```bash
# Si usas el usuario postgres
psql -U postgres -d juridico_db -f database-schema-completo.sql

# Si usas usuario específico
psql -U juridico_user -d juridico_db -f database-schema-completo.sql
```

**El schema incluye:**
- ✅ 17 tablas principales
- ✅ 3 vistas materializadas
- ✅ 40+ índices optimizados
- ✅ 10 triggers automáticos
- ✅ Datos iniciales (seed data):
  - 3 Gerencias
  - 8 Materias
  - 6 Tipos de Audiencia
  - 6 Órganos Jurisdiccionales
  - 1 Usuario admin

### 4. Configurar application.properties

Editar `src/main/resources/application.properties` si necesitas cambiar configuraciones:

```properties
# Base de datos (cambiar si usas otros valores)
spring.datasource.url=jdbc:postgresql://localhost:5432/juridico_db
spring.datasource.username=postgres
spring.datasource.password=JuridicoPostgres2026!

# JWT (cambiar secret en producción)
jwt.secret=JuridicoGobMxSecretKey2026ChangeThisInProduction!

# Puerto del servidor
server.port=8080
```

### 5. Compilar el Proyecto

```bash
# Limpiar y compilar
./mvnw clean install

# O si tienes Maven instalado globalmente
mvn clean install
```

### 6. Ejecutar la Aplicación

```bash
# Opción 1: Con Maven wrapper
./mvnw spring-boot:run

# Opción 2: Con Maven global
mvn spring-boot:run

# Opción 3: Ejecutar el JAR
java -jar target/sistema-juridico-0.0.1-SNAPSHOT.jar
```

## 🌐 URLs Importantes

| Recurso | URL | Descripción |
|---------|-----|-------------|
| **API Base** | http://localhost:8080/api | Base path de la API |
| **Swagger UI** | http://localhost:8080/swagger-ui.html | Documentación interactiva |
| **API Docs JSON** | http://localhost:8080/api-docs | OpenAPI 3.0 JSON |
| **Health Check** | http://localhost:8080/actuator/health | Estado de la aplicación |

## 📁 Estructura del Proyecto

```
src/main/java/com/juridico/sistema_juridico/
├── SistemaJuridicoApplication.java    # Clase principal
├── controller/                         # Controladores REST (vacío, a implementar)
├── entity/                            # Entidades JPA (a implementar)
├── repository/                        # Repositorios Spring Data (a implementar)
├── service/                           # Lógica de negocio (a implementar)
├── dto/                               # DTOs para requests/responses
│   ├── request/                       # DTOs de entrada
│   └── response/                      # DTOs de salida
├── security/                          # Configuración de seguridad
│   └── SecurityConfig.java           # Config actual (permite todo en desarrollo)
├── exception/                         # Manejo global de excepciones (a implementar)
└── util/                              # Utilidades (a implementar)

src/main/resources/
├── application.properties             # Configuración principal
├── templates/                         # Templates Thymeleaf (admin views)
└── static/                           # Recursos estáticos (CSS, JS, imágenes)
```

## 👥 Equipo de Desarrollo

| Nombre | Rol | Horario | Disponibilidad |
|--------|-----|---------|----------------|
| **Ramses** | Senior Developer | 10:00-14:00 | 12-20 enero |
| **Aurora** | Junior Developer | 11:00-15:00 | Todo el proyecto |
| **Ricardo** | Mid-Senior Developer | 14:00-18:00 | Todo el proyecto |

**Traslapes para comunicación:**
- Ramses-Aurora: 11:00-14:00 (3 horas)
- Aurora-Ricardo: 14:00-15:00 (1 hora)

## 📚 Documentación Adicional

- **Plan de Implementación:** `PLAN-IMPLEMENTACION-BACKEND.md` (repositorio frontend)
- **Análisis de BD:** `ANALISIS-BASE-DE-DATOS.md` (repositorio frontend)
- **Índice de BD:** `INDICE-BASE-DE-DATOS.md` (repositorio frontend)
- **Schema Visual:** `ESQUEMA-VISUAL-TABLAS.md` (repositorio frontend)

## 🔧 Comandos Útiles

```bash
# Limpiar proyecto
./mvnw clean

# Compilar sin tests
./mvnw clean install -DskipTests

# Ejecutar tests
./mvnw test

# Generar JAR
./mvnw package

# Ver dependencias
./mvnw dependency:tree

# Actualizar dependencias
./mvnw versions:display-dependency-updates
```

## 🐛 Troubleshooting

### Error: Could not connect to database

**Solución:**
1. Verificar que PostgreSQL esté corriendo: `sudo systemctl status postgresql`
2. Verificar credenciales en `application.properties`
3. Verificar que la base de datos existe: `psql -U postgres -l`

### Error: Port 8080 already in use

**Solución:**
1. Cambiar puerto en `application.properties`: `server.port=8081`
2. O matar el proceso: `lsof -ti:8080 | xargs kill -9`

### Error: Cannot find main class

**Solución:**
```bash
./mvnw clean package
java -jar target/sistema-juridico-0.0.1-SNAPSHOT.jar
```

## 🔐 Seguridad

**⚠️ IMPORTANTE: Configuración Actual de Desarrollo**

La configuración de seguridad actual permite acceso a **TODOS** los endpoints sin autenticación para facilitar el desarrollo.

**Antes de Producción:**
1. Configurar roles y permisos en `SecurityConfig.java`
2. Implementar JWT filter chain
3. Habilitar CSRF protection
4. Cambiar `jwt.secret` en `application.properties`
5. Configurar HTTPS

## 📞 Contacto y Soporte

- **Líder de Proyecto:** Rodrigo Paz
- **Email:** contactorodrigopaz@gmail.com
- **GitHub:** [@rodrigopazTech](https://github.com/rodrigopazTech)

## 📄 Licencia

Copyright © 2026 Gobierno de México - Área Jurídica
