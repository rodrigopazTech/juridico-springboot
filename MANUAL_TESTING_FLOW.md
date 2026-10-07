## Flujo de Pruebas Manuales para el Sistema Jurídico SIAJ

Este documento describe un flujo de pruebas manuales para identificar errores críticos y verificar funcionalidades clave del sistema SIAJ (Sistema de Información para la Administración de Justicia).

---

### **Objetivo:**

Asegurar la estabilidad, funcionalidad y usabilidad de los módulos principales del sistema SIAJ, identificando posibles errores o comportamientos inesperados antes de su implementación en producción.

---

### **1. Prerrequisitos y Configuración del Entorno de Pruebas**

Antes de iniciar las pruebas, asegúrese de que el entorno esté correctamente configurado:

1.  **Clonar el Repositorio:**
    ```bash
    git clone git@30.0.0.56:Rodrigo/juridico-siaj.git
    cd juridico-siaj
    ```
2.  **Levantar Infraestructura con Docker:**
    *   Asegúrese de tener Docker Desktop o Docker Engine en ejecución.
    *   Ejecute el siguiente comando para iniciar la base de datos PostgreSQL y pgAdmin:
        ```bash
        docker-compose up -d
        ```
    *   Verifique que ambos servicios estén levantados y accesibles.
        *   **PostgreSQL:** `localhost:5433`
        *   **pgAdmin:** `http://localhost:5050` (Credenciales: Email: `admin@juridico.com`, Pass: `admin123`)
3.  **Configurar `application.properties`:**
    *   Confirme que `src/main/resources/application.properties` tenga las credenciales de la base de datos correctas:
        ```properties
        spring.datasource.url=jdbc:postgresql://localhost:5433/sistema_juridico
        spring.datasource.username=agendajuridicodbdev
        spring.datasource.password=DBag3$#38
        ```
4.  **Compilar y Ejecutar la Aplicación Spring Boot:**
    *   Limpiar y compilar el proyecto:
        ```bash
        ./mvnw clean install
        ```
    *   Ejecutar la aplicación:
        ```bash
        ./mvnw spring-boot:run
        ```
    *   Verifique que la aplicación se inicie correctamente y sea accesible en `http://localhost:8080/`.

---

### **2. Directrices Generales de Prueba**

Aplique estas directrices en todos los módulos:

*   **Interfaz de Usuario (UI) y Experiencia de Usuario (UX):**
    *   Verifique la consistencia del diseño, fuentes, colores y espaciado.
    *   Asegure que todos los elementos interactivos (botones, enlaces, campos de formulario) sean clicables y respondan como se espera.
    *   Pruebe la aplicación en diferentes tamaños de ventana del navegador para verificar la adaptabilidad (responsiveness).
    *   Confirme que los mensajes de validación y de éxito/error sean claros y se muestren en el lugar adecuado.
*   **Integridad de Datos:**
    *   Después de crear, editar o eliminar datos, verifique que los cambios se reflejen correctamente en la interfaz y, si es posible, en la base de datos (usando pgAdmin).
    *   Asegúrese de que los datos ingresados no se corrompan o trunquen.
*   **Manejo de Errores:**
    *   Pruebe escenarios con datos inválidos o incompletos para asegurar que el sistema maneje los errores de forma elegante y muestre mensajes útiles al usuario.
    *   Observe el comportamiento del sistema cuando se producen errores internos (ver sección de verificación de errores).
*   **Permisos y Roles (si aplica):**
    *   Si existen diferentes tipos de usuarios (ej. Administrador, Usuario Estándar), inicie sesión con cada rol y verifique que solo puedan acceder y realizar acciones permitidas para su rol.

---

### **3. Flujos de Pruebas por Módulo Principal**

A continuación, se detallan los flujos de prueba críticos para cada módulo del sistema:

#### **3.1. Autenticación y Autorización**

*   **Objetivo:** Verificar el acceso seguro al sistema.
*   **Flujos:**
    1.  **Inicio de Sesión Exitoso:**
        *   Acceda a la página de inicio de sesión (`http://localhost:8080/auth/login` o similar si hay redirección).
        *   Ingrese credenciales válidas y verifique el acceso al dashboard o página de inicio.
        *   Revise que el token JWT se genere correctamente (inspeccionando la red en el navegador, si es visible).
    2.  **Inicio de Sesión Fallido:**
        *   Ingrese credenciales inválidas (usuario incorrecto, contraseña incorrecta).
        *   Verifique que se muestre un mensaje de error apropiado y no se conceda el acceso.
    3.  **Cierre de Sesión:**
        *   Inicie sesión y luego use la opción de "Cerrar Sesión".
        *   Verifique que se redirige a la página de inicio de sesión y que no se puede acceder a páginas protegidas sin volver a iniciar sesión.
    4.  **Acceso No Autorizado:**
        *   Intente acceder directamente a una URL de un módulo (`/views/dashboard`, `/views/expedientes`) sin haber iniciado sesión.
        *   Verifique que el sistema redirija a la página de inicio de sesión o muestre un mensaje de acceso denegado.

#### **3.2. Dashboard (`http://localhost:8080/views/dashboard`)**

*   **Objetivo:** Verificar la correcta visualización de información clave y acceso a módulos.
*   **Flujos:**
    1.  **Carga Inicial:**
        *   Verifique que el dashboard carga sin errores visuales o de consola.
        *   Asegure que los widgets/gráficos muestren datos (incluso si son de ejemplo) y se actualicen correctamente.
    2.  **Navegación:**
        *   Verifique que los enlaces o botones a otros módulos (Expedientes, Audiencias, etc.) funcionen y redirijan a las páginas correctas.
    3.  **Visualización de Métricas:**
        *   Confirme que las métricas (ej. número de expedientes activos, próximas audiencias, tareas pendientes) se presenten de manera legible y coherente con los datos esperados.

#### **3.3. Gestión de Expedientes (`http://localhost:8080/views/expedientes`)**

*   **Objetivo:** Probar el ciclo de vida completo de un expediente.
*   **Flujos:**
    1.  **Listado de Expedientes:**
        *   Acceda a la vista de expedientes.
        *   Verifique que la tabla o lista cargue correctamente y muestre los expedientes existentes.
        *   Pruebe la paginación, ordenación y filtros (si están implementados).
    2.  **Creación de Expediente:**
        *   Haga clic en "Nuevo Expediente" (o similar).
        *   Rellene el formulario con datos válidos.
        *   Verifique que el expediente se guarde correctamente y aparezca en el listado.
        *   Intente crear un expediente con datos inválidos/vacíos y verifique los mensajes de validación.
    3.  **Visualización de Detalles de Expediente:**
        *   Seleccione un expediente del listado.
        *   Verifique que toda la información asociada (partes, fechas, estado, documentos adjuntos) se muestre correctamente.
    4.  **Edición de Expediente:**
        *   Modifique los datos de un expediente existente.
        *   Verifique que los cambios se guarden y reflejen en la vista de detalles y en el listado.
        *   Intente editar con datos inválidos/vacíos y verifique los mensajes de validación.
    5.  **Eliminación de Expediente:**
        *   Seleccione un expediente y use la opción de "Eliminar".
        *   Confirme el proceso de eliminación (si hay una confirmación).
        *   Verifique que el expediente ya no aparezca en el listado.
    6.  **Búsqueda:**
        *   Utilice el campo de búsqueda para buscar expedientes por diferentes criterios (ej. nombre, número, estado).
        *   Verifique que los resultados sean precisos.

#### **3.4. Gestión de Audiencias (`http://localhost:8080/views/audiencias`)**

*   **Objetivo:** Probar la creación y gestión de audiencias.
*   **Flujos:**
    1.  **Listado de Audiencias:**
        *   Acceda a la vista de audiencias.
        *   Verifique que la lista/calendario cargue correctamente.
        *   Pruebe filtros por fecha, tipo o expediente asociado.
    2.  **Creación de Audiencia:**
        *   Cree una nueva audiencia asociada a un expediente.
        *   Rellene todos los campos requeridos (fecha, hora, descripción, etc.).
        *   Verifique que se guarde correctamente y aparezca en el listado/calendario.
        *   Pruebe con datos inválidos.
    3.  **Visualización/Edición/Eliminación:**
        *   Similar a los expedientes, pruebe la visualización de detalles, edición y eliminación de audiencias, verificando la persistencia de los cambios.

#### **3.5. Gestión de Usuarios (`http://localhost:8080/views/usuarios`)**

*   **Objetivo:** Verificar la administración de usuarios del sistema.
*   **Flujos:**
    1.  **Listado de Usuarios:**
        *   Acceda a la vista de usuarios.
        *   Verifique que se muestren los usuarios existentes con su información básica.
        *   Pruebe filtros y búsqueda.
    2.  **Creación de Usuario:**
        *   Cree un nuevo usuario con diferentes roles (si aplica).
        *   Verifique la asignación de roles y que el usuario pueda iniciar sesión con las credenciales dadas.
        *   Pruebe con datos inválidos o usuarios duplicados.
    3.  **Edición de Usuario:**
        *   Modifique información de un usuario existente (ej. nombre, rol, contraseña).
        *   Verifique que los cambios se apliquen y que el usuario pueda seguir accediendo con la nueva información si la contraseña fue cambiada.
    4.  **Eliminación de Usuario:**
        *   Elimine un usuario.
        *   Verifique que el usuario ya no pueda iniciar sesión.

#### **3.6. Agenda / Calendario (`http://localhost:8080/views/agenda` y `/views/calendario`)**

*   **Objetivo:** Verificar la correcta visualización y gestión de eventos/términos.
*   **Flujos:**
    1.  **Visualización (Día, Semana, Mes):**
        *   Navegue entre las diferentes vistas del calendario (día, semana, mes).
        *   Verifique que los eventos (audiencias, términos, tareas) se muestren correctamente en cada vista.
    2.  **Creación de Evento/Término:**
        *   Cree un nuevo evento o término directamente desde el calendario.
        *   Asocie el evento a un expediente o usuario (si aplica).
        *   Verifique que se guarde y aparezca en la vista correspondiente.
    3.  **Edición/Eliminación de Evento/Término:**
        *   Modifique la fecha, hora o detalles de un evento.
        *   Elimine un evento.
        *   Verifique la persistencia de los cambios.

#### **3.7. Notificaciones y Recordatorios**

*   **Objetivo:** Confirmar que el sistema alerta a los usuarios sobre eventos importantes.
*   **Flujos:**
    1.  **Generación de Notificaciones:**
        *   Cree un expediente con una fecha de término cercana, o una audiencia próxima.
        *   Verifique si se genera alguna notificación en la UI (ej. icono de campana, popup) o si se envía un recordatorio por otro medio (ej. email si está configurado).
    2.  **Visualización de Notificaciones:**
        *   Acceda al centro de notificaciones (si existe un módulo dedicado).
        *   Verifique que las notificaciones se listen correctamente y que al hacer clic redirijan al elemento correspondiente (expediente, audiencia).
    3.  **Marcado como Leído/Borrado:**
        *   Pruebe marcar notificaciones como leídas o eliminarlas.

---

### **4. Verificación de Errores y Reporte**

Durante todas las pruebas, preste especial atención a:

1.  **Consola del Navegador (Developer Tools F12):**
    *   Busque errores de JavaScript, errores de red (códigos de estado HTTP 4xx, 5xx) o advertencias inusuales.
2.  **Pestaña de Red (Developer Tools F12):**
    *   Monitoree las solicitudes y respuestas de la API.
    *   Verifique que los datos enviados y recibidos sean correctos y consistentes.
    *   Asegúrese de que los endpoints de la API respondan con los códigos de estado HTTP esperados (ej., 200 OK para éxito, 201 Created para creación, 400 Bad Request para validación fallida, 401 Unauthorized, 403 Forbidden, 404 Not Found, 500 Internal Server Error).
3.  **Logs del Servidor (Backend):**
    *   Mantenga una ventana de terminal abierta donde se ejecuta la aplicación Spring Boot.
    *   Observe los logs (`./mvnw spring-boot:run` o desde `server_log.txt` si está redirigido) en busca de excepciones, errores graves (FATAL, ERROR) o trazas de pila (stack traces).
    *   Si hay errores, intente reproducirlos y capture los mensajes completos del log.
4.  **Base de Datos (pgAdmin):**
    *   Para pruebas de integridad de datos, acceda a pgAdmin (`http://localhost:5050`) y realice consultas directas a las tablas para verificar que los datos se inserten, actualicen o eliminen correctamente a nivel de base de datos.

### **Reporte de Errores:**

Todo error encontrado debe ser documentado con la siguiente información:

*   **Título:** Breve descripción del problema.
*   **Pasos para Reproducir:** Descripción clara y concisa de los pasos para que otro pueda reproducir el error.
*   **Comportamiento Esperado:** Qué debería haber hecho el sistema.
*   **Comportamiento Actual:** Qué hizo el sistema en realidad.
*   **Capturas de Pantalla/Videos:** Evidencia visual del error.
*   **Mensajes de Error:** Cualquier mensaje mostrado en la UI, consola del navegador o logs del servidor.
*   **Severidad:** Crítico, Mayor, Menor, Estético.
*   **Módulo Afectado:** Dashboard, Expedientes, Audiencias, etc.
*   **Rol de Usuario:** Si el error solo ocurre con un tipo específico de usuario.

---

Este flujo proporciona una guía robusta para la validación manual del sistema SIAJ.
