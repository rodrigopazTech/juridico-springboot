# Guía de Integración: Backend, Seguridad y Base de Datos

Este documento contiene las dependencias, configuraciones y scripts necesarios para replicar los cambios del backend y base de datos en los entornos locales de desarrollo.

---

## 1. Dependencias de Maven (`pom.xml`)

Es necesario agregar las siguientes dependencias al archivo `pom.xml` para habilitar el cifrado criptográfico y el protocolo Web Push:

```xml
<dependency>
    <groupId>nl.martijndwars</groupId>
    <artifactId>web-push</artifactId>
    <version>5.1.1</version>
</dependency>
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcprov-jdk18on</artifactId>
    <version>1.78</version>
</dependency>
```

*(Nota: Asegúrate de recargar el proyecto de Maven/recargar dependencias en el IDE para que se descarguen).*

---

## 2. Cambios en la Base de Datos (BD)

### A. Estructura de la Tabla `push_subscriptions`
Si no utilizas la generación automática de Hibernate, ejecuta el siguiente DDL en tu base de datos PostgreSQL local para crear la tabla de suscripciones Web Push multidispositivo:

```sql
CREATE TABLE push_subscriptions (
    id SERIAL PRIMARY KEY,
    usuario_id INTEGER NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    endpoint TEXT NOT NULL,
    p256dh VARCHAR(255) NOT NULL,
    auth VARCHAR(255) NOT NULL,
    device_id VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_push_sub_usuario ON push_subscriptions(usuario_id);
CREATE INDEX idx_push_sub_endpoint ON push_subscriptions(endpoint);
```

### B. Ejecución de Triggers Actualizados
Para que las notificaciones en tiempo real incluyan la severidad/prioridad correcta y para dar soporte a la tabla renombrada `cat_tipo_expediente`, debes ejecutar en la base de datos el script ubicado en:
`deploy/database/02-realtime-notifications.sql`

### C. Limpieza de Datos Operativos (Opcional)
Si necesitas limpiar la base de datos para pruebas operativas manteniendo únicamente los usuarios, gerencias y materias configuradas, ejecuta:
`src/main/resources/limpiar_datos.sql`

---

## 3. Configuración del Entorno (`application.properties`)

Añade los parámetros VAPID para el protocolo Web Push al final de tu archivo de propiedades:

```properties
# Configuración Web Push VAPID
app.webpush.public-key=BI64zp-EABSMNx2692_GNdP70TZVVRe8Q3iL2WJYGUQvezDBJwEDjLr310TWyNcktaCqTxNYXwXbsAMJ2PGH-KU
app.webpush.private-key=iDttC07TJ2wq677fomN3FIGvpqwDorDM1hLafCxrdxM
app.webpush.subject=mailto:soporte@juridico.com
```

---

## 4. Estructura Crítica del Backend

Asegúrate de importar e integrar la lógica en las siguientes clases:

### A. Configuración de Seguridad (`SecurityConfig.java`)
* Se expone públicamente el Service Worker `/sw.js` para permitir la recepción de notificaciones Push en segundo plano.
* Se expone el endpoint `/usuarios/api/materias-por-gerencia/**` para usuarios autenticados.
* Al iniciar sesión de manera exitosa, el sistema ahora genera un Token JWT y lo adjunta en una cookie HttpOnly (`JWT_TOKEN`) para sincronizar solicitudes AJAX/REST.
* Se aumentan las sesiones concurrentes permitidas (`maximumSessions`) de 1 a **3** para permitir el uso simultáneo en Navegador Web, App de Escritorio y Dispositivo Móvil.

### B. Control de Acceso Restrictivo e IDOR (`DocumentoService.java` y Controladores)
Se modificó la lógica de seguridad para validar dinámicamente los recursos del expediente:
1. **Roles con acceso total:** `ADMIN`, `DIRECCION` y `SUBDIRECCION`.
2. **Barrera de Gerencia:** Ningún usuario (no directivo) puede ver o interactuar con expedientes de otra gerencia.
3. **Rol `JEFE_DEPTO`:** Solo accede a expedientes cuya materia coincida con las materias que tiene asignadas.
4. **Rol `ABOGADO`:** Solo accede si es el responsable directo del expediente o si posee una colaboración temporal vigente en la tabla `colaboradores_expediente`.

### C. Planificación de Tareas (`RecordatorioNotificacionScheduler.java`)
* **Diario (7:30 AM):** Envía un resumen matutino con las tareas programadas para el día de cada usuario.
* **Intradía (Cada minuto):** Busca tareas próximas a cumplirse en el día y dispara una alerta 15 minutos antes de su vencimiento (evitando envíos duplicados con una validación de base de datos).

---

## 5. Integración Frontend
Asegúrate de incorporar en tu rama los siguientes archivos estáticos:
* `src/main/resources/static/sw.js` (Service Worker para registrar eventos 'push' y 'notificationclick').
* `src/main/resources/static/manifest.json` (Parámetros del Service Worker y PWA).
* `src/main/resources/static/js/webpush-client.js` (Controlador en JS para suscribir el navegador/dispositivo).
