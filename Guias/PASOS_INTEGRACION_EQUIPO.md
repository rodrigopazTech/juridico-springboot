# Procedimiento de Integración para el Equipo

Este documento detalla los pasos que deben seguir los miembros del equipo para sincronizar sus entornos locales de base de datos y backend con las últimas modificaciones (eliminación de la entidad `Etiqueta` y el soporte del sistema de notificaciones Push y planificador).

---

## 📋 Pasos para la Sincronización

### Paso 1: Obtener el Código Actualizado

Ejecuta el comando para traer los últimos cambios del repositorio y actualizar tu rama de trabajo local:

```bash
git pull
```

_(Esto eliminará automáticamente del código fuente local las clases de `Etiqueta.java` y `EtiquetaRepository.java`, e integrará las nuevas dependencias en `pom.xml`, configuraciones de seguridad, controladores, servicios y scripts SQL actualizados)._

---

### Paso 2: Actualizar la Base de Datos Local (PostgreSQL)

Conéctate a tu base de datos local (o dentro de tu contenedor Docker de PostgreSQL) y ejecuta las siguientes acciones:

1. **Eliminar las tablas huérfanas de etiquetas:**

   ```sql
   DROP TABLE IF EXISTS expediente_etiquetas CASCADE;
   DROP TABLE IF EXISTS etiquetas CASCADE;
   ```

2. **Crear la tabla para la gestión de suscripciones Push:**
   _(Si tu entorno no genera las tablas automáticamente por JPA, ejecuta el DDL de la sección 2.A del archivo `INSTRUCCIONES_INTEGRACION.md`)._

3. **Actualizar Triggers de Base de Datos:**
   Ejecuta el script completo de triggers para sincronizar el envío de notificaciones en tiempo real en la BD:
   `deploy/database/02-realtime-notifications.sql`

---

### Paso 3: Configurar Propiedades locales (`application.properties`)

Añade las siguientes líneas al final de tu archivo `src/main/resources/application.properties` para dar soporte a las credenciales de notificaciones Web Push VAPID:

```properties
# Configuración Web Push VAPID
app.webpush.public-key=BI64zp-EABSMNx2692_GNdP70TZVVRe8Q3iL2WJYGUQvezDBJwEDjLr310TWyNcktaCqTxNYXwXbsAMJ2PGH-KU
app.webpush.private-key=iDttC07TJ2wq677fomN3FIGvpqwDorDM1hLafCxrdxM
app.webpush.subject=mailto:siaj@finabien.gob.mx
```

---

### Paso 4: Limpiar y Compilar el Proyecto

Para asegurar que los archivos binarios compilados previamente no tengan rastro de las entidades removidas, limpia el directorio `target` e inicia la compilación de Maven:

```bash
mvn clean compile
```

---

### Paso 5: Iniciar la Aplicación

Reinicia o arranca de nuevo el servidor Spring Boot:

```bash
mvn spring-boot:run
```

_(O ejecútalo directamente desde la interfaz de tu IDE)._
