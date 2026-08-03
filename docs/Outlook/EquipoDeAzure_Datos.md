Excelente enfoque. Si vas a solicitar apoyo al equipo que administra Azure, conviene enviarles un documento formal que explique el objetivo de negocio, los requisitos técnicos y exactamente qué deben entregarte. Esto evita idas y vueltas y facilita que el área de seguridad apruebe la integración.

Manual para el Equipo Administrador de Azure
Integración de Calendario Outlook con Sistema Web mediante Microsoft Graph
1. Objetivo de la integración

Se requiere habilitar la integración entre el sistema web corporativo y los calendarios de Outlook de los usuarios para permitir la sincronización bidireccional de eventos.

Esto significa que los usuarios podrán:

Visualizar sus eventos de Outlook dentro del sistema.
Crear eventos desde el sistema y que aparezcan en Outlook.
Modificar eventos desde cualquiera de las dos plataformas.
Eliminar eventos desde cualquiera de las dos plataformas.
Mantener la información sincronizada entre ambos entornos.
¿Es seguro?

Sí.

La integración utiliza los mecanismos oficiales de Microsoft:

Azure Entra ID (antes Azure Active Directory).
OAuth 2.0.
Microsoft Graph API.
Permisos delegados otorgados por cada usuario.

Características de seguridad:

✅ El sistema nunca solicita contraseñas de Outlook.

✅ Los usuarios se autentican directamente contra Microsoft.

✅ Los accesos se controlan mediante tokens temporales.

✅ El usuario puede revocar el acceso en cualquier momento.

✅ Solo se accede al calendario mediante permisos autorizados.

✅ Toda la comunicación se realiza mediante HTTPS.

2. Información que el equipo de desarrollo debe proporcionar previamente

Antes de comenzar la configuración, el equipo de desarrollo deberá entregar la siguiente información al administrador de Azure.

Datos obligatorios
Nombre de la aplicación

Ejemplo:

SIAPP Calendario Outlook

URL pública de la aplicación

Ejemplo:

https://siapp.midominio.com

Datos de nuestros ambientes:

Desarrollo:
Servidor DEV: 30.0.0.150
URL pública DEV: pendiente de confirmar si se publicará mediante dominio HTTPS.

Producción:
Servidor aplicativo PROD: 40.0.0.61
URL pública PROD: pendiente de confirmar el dominio HTTPS configurado en DNS.

Nota sobre Producción:
Para Azure, si producción ya cuenta con DNS apuntando a un dominio, se deberá registrar el dominio HTTPS público en las URLs de autenticación y webhook, no la IP del servidor.

URL de autenticación (Redirect URI)

Ejemplo:

https://siapp.midominio.com/auth/microsoft/callback

Datos de nuestros ambientes:

Desarrollo:
https://[dominio-dev-pendiente]/auth/microsoft/callback

Producción:
https://[dominio-produccion-pendiente]/auth/microsoft/callback

Esta URL será utilizada por Microsoft para devolver al usuario a la aplicación después de autenticarse.

URL de Webhook

Ejemplo:

https://siapp.midominio.com/api/outlook/webhook

Datos de nuestros ambientes:

Desarrollo:
https://[dominio-dev-pendiente]/api/outlook/webhook

Producción:
https://[dominio-produccion-pendiente]/api/outlook/webhook

Esta URL recibirá las notificaciones de cambios en el calendario.

Ambientes

Indicar si existirán:

Desarrollo
QA
Producción

Ejemplo:

Desarrollo:
https://dev.siapp.midominio.com

Producción:
https://siapp.midominio.com

Datos de nuestros ambientes:

Desarrollo:
Servidor DEV: 30.0.0.150

Producción:
Servidor aplicativo PROD: 40.0.0.61
Dominio público PROD: pendiente de confirmar.

3. Actividades que debe realizar el Administrador de Azure
Paso 1. Ingresar al Portal Azure

Entrar a:

Azure Portal


Menú:

Microsoft Entra ID

Paso 2. Registrar una nueva aplicación

Navegar a:

App registrations


Seleccionar:

New registration


Capturar:

Nombre
SIAPP Calendario Outlook

Supported account types

Recomendado:

Accounts in this organizational directory only


(si solo se utilizará dentro de la organización)

Redirect URI

Tipo:

Web


URL:

https://siapp.midominio.com/auth/microsoft/callback

Datos de nuestros ambientes:

Desarrollo:
https://[dominio-dev-pendiente]/auth/microsoft/callback

Producción:
https://[dominio-produccion-pendiente]/auth/microsoft/callback

Guardar.

Paso 3. Obtener Application ID y Tenant ID

Una vez creada la aplicación registrar los siguientes datos:

Application (client) ID

Ejemplo:

12345678-abcd-1234-abcd-123456789000

Directory (tenant) ID

Ejemplo:

87654321-abcd-4321-abcd-000987654321

Paso 4. Crear Client Secret

Ir a:

Certificates & Secrets


Seleccionar:

New Client Secret


Descripción:

SIAPP Producción


Definir vigencia según política corporativa.

Guardar inmediatamente el valor.

Importante:

El valor sólo puede visualizarse una vez.

Paso 5. Asignar permisos Microsoft Graph

Ir a:

API Permissions


Seleccionar:

Add Permission


Elegir:

Microsoft Graph


Luego:

Delegated Permissions


Agregar:

Requeridos
User.Read
Calendars.Read
Calendars.ReadWrite
offline_access

Paso 6. Conceder consentimiento administrativo

Seleccionar:

Grant admin consent


Confirmar.

Esto permitirá que los usuarios utilicen la integración sin problemas de consentimiento individual si así lo define la política de la organización.

Paso 7. Validar autenticación

Confirmar que la aplicación permita:

Authorization Code Flow


Con:

PKCE


(recomendado por Microsoft para aplicaciones modernas).

Paso 8. Configurar origenes permitidos (si aplica)

Si la aplicación utiliza frontend React, Angular o Vue, agregar los dominios autorizados.

Ejemplo:

https://siapp.midominio.com

Datos de nuestros ambientes:

Desarrollo:
https://[dominio-dev-pendiente]

Producción:
https://[dominio-produccion-pendiente]

4. Información que debe entregar el Administrador de Azure

Al concluir la configuración deberá proporcionar:

Identificadores
Tenant ID
Client ID

Credenciales
Client Secret


o preferentemente

Certificado de aplicación


si la política corporativa lo requiere.

URLs registradas
Redirect URI
Webhook URI

Permisos otorgados

Confirmación de que existen:

User.Read
Calendars.Read
Calendars.ReadWrite
offline_access

Tipo de cuentas habilitadas

Confirmar si la aplicación permite:

Solo cuentas corporativas


o

Cuentas externas

5. Actividades posteriores del equipo de desarrollo

Una vez recibida la información anterior, el equipo de desarrollo deberá:

Configurar OAuth

Utilizando:

Tenant ID
Client ID
Client Secret

Implementar autenticación Microsoft

Flujo:

Usuario
      ↓
Login Microsoft
      ↓
Consentimiento
      ↓
Obtención de Token
      ↓
Acceso Graph API

Implementar operaciones del calendario
Consultar eventos
GET /me/events

Crear eventos
POST /me/events

Actualizar eventos
PATCH /me/events/{id}

Eliminar eventos
DELETE /me/events/{id}

Implementar Webhooks

Registrar suscripciones de Microsoft Graph para detectar cambios en Outlook.

Eventos a monitorear:

created
updated
deleted

6. Pruebas que debe realizarse antes de liberar
Prueba 1

Crear evento en el sistema.

Resultado esperado:

Debe aparecer en Outlook.

Prueba 2

Crear evento en Outlook.

Resultado esperado:

Debe aparecer en el sistema.

Prueba 3

Modificar evento desde Outlook.

Resultado esperado:

El sistema refleja el cambio.

Prueba 4

Modificar evento desde el sistema.

Resultado esperado:

Outlook refleja el cambio.

Prueba 5

Eliminar evento desde cualquiera de los entornos.

Resultado esperado:

El evento desaparece en ambas plataformas.

Resumen de entregables
Lo que desarrollo entrega a Azure
Nombre de la aplicación.
URL pública del sistema.
Redirect URI.
URL de Webhook.
Ambientes (DEV/QA/PROD).

Datos de nuestros ambientes:
Desarrollo: 30.0.0.150.
Producción: 40.0.0.61.
Dominio público de Producción: pendiente de confirmar; para Azure se debe usar el dominio HTTPS público si el DNS ya apunta a Producción.
Dominio público de Desarrollo: pendiente de confirmar si aplica.

Lo que Azure entrega a desarrollo
Tenant ID.
Client ID.
Client Secret o Certificado.
Confirmación de permisos Graph.
Confirmación de consentimiento administrativo.
URLs registradas.

Con este documento, el administrador de Azure tendrá una guía completa para crear correctamente la aplicación y tú recibirás exactamente todos los datos necesarios para implementar la sincronización bidireccional de Outlook dentro de tu sistema.
