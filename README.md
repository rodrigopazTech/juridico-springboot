# 🏛️ Sistema Jurídico - Backend Spring Boot

API REST Backend para el Sistema de Agenda Jurídica Gob.MX V3.

---

## 📋 Información del Proyecto

**Stack Tecnológico:**
- ☕ Java 17
- 🍃 Spring Boot 3.4.12
- 🐳 Docker & Docker Compose
- 🐘 PostgreSQL 15
- 🔐 Spring Security + JWT
- 📚 Spring Data JPA
- 🎨 Thymeleaf (vistas admin)
- 📖 Swagger/OpenAPI 3.0
- 🔄 Lombok

**Repositorio:**
- **GitLab:** git@30.0.0.56:Rodrigo/juridico-siaj.git

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

- **Java JDK 17+**
- **Docker & Docker Desktop**
- **Maven 3.8+** (opcional, se incluye Maven Wrapper)
- **Git**

## 🚀 Instalación y Configuración

### 1. Clonar el Repositorio

```bash
git clone git@30.0.0.56:Rodrigo/juridico-siaj.git
cd juridico-siaj
```

### 2. Levantar la Infraestructura con Docker

El proyecto utiliza Docker para gestionar la base de datos y herramientas de administración.

```bash
# Iniciar base de datos (PostgreSQL 15) y pgAdmin
docker-compose up -d
```

**Servicios incluidos:**
- ✅ **Base de Datos:** PostgreSQL en `localhost:5433`
- ✅ **Gestor BD:** pgAdmin en `http://localhost:5050` (Email: `ricardo@juridico.com`, Pass: `admin`)

**Credenciales de Base de Datos:**
- **DB Name:** `sistema_juridico`
- **User:** `villalobos_admin`
- **Password:** `password123`

### 3. Configurar application.properties

Asegúrate de que `src/main/resources/application.properties` apunte al contenedor de Docker:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5433/sistema_juridico
spring.datasource.username=villalobos_admin
spring.datasource.password=password123
```

### 4. Compilar y Ejecutar la Aplicación

```bash
# Limpiar y compilar
./mvnw clean install

# Ejecutar la aplicación
./mvnw spring-boot:run
```

## 🌐 URLs Importantes

| Recurso | URL | Descripción |
|---------|-----|-------------|
| **Frontend Principal** | http://localhost:8080/ | Dashboard de módulos |
| **API Base** | http://localhost:8080/api | Base path de la API REST |
| **Swagger UI** | http://localhost:8080/swagger-ui.html | Documentación interactiva |
| **pgAdmin** | http://localhost:5050 | Administración de base de datos |

### Módulos Frontend

El frontend está integrado en el servidor de Spring Boot bajo el directorio `templates/views/`.

| Módulo | URL Interna |
|--------|-------------|
| Dashboard | `/views/dashboard` |
| Expedientes | `/views/expedientes` |
| Audiencias | `/views/audiencias` |
| Usuarios | `/views/usuarios` |
| Agenda | `/views/agenda` |

## 📁 Estructura del Proyecto

```
src/main/java/com/juridico/sistema_juridico/
├── controller/          # Controladores REST y MVC
├── entity/             # Entidades JPA
├── repository/         # Repositorios Spring Data
├── service/            # Lógica de negocio
├── dto/                # Objetos de Transferencia de Datos
├── security/           # Configuración de JWT y Security
└── exception/          # Manejo global de errores

src/main/resources/
├── application.properties  # Configuración principal
├── static/                 # Recursos estáticos (CSS, JS, Libs)
│   ├── css/
│   ├── js/
│   └── lib/
└── templates/              # Vistas Thymeleaf (HTML)
    ├── fragments/          # Componentes reutilizables
    └── views/              # Vistas de los módulos
```

## 🔐 Seguridad

La configuración actual de desarrollo permite el acceso a las vistas principales, pero requiere autenticación JWT para los endpoints de la API (`/api/**`).

**Antes de Producción:**
1. Cambiar `app.jwt.secret` en `application.properties`.
2. Habilitar protección CSRF.
3. Configurar HTTPS.

## 📞 Contacto y Soporte

- **Líder de Proyecto:** Rodrigo Paz
- **Email:** rodrigo.paz@finabien.gob.mx

## 📄 Licencia

Copyright © 2026 Gobierno de México - FinaBien
Este es un proyecto privado de uso interno institucional.
