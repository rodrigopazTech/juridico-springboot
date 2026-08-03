Este documento describe el objetivo de negocio, los requisitos técnicos y los entregables necesarios para configurar en Microsoft Azure la integración del sistema con Microsoft 365.

Manual para el Equipo Administrador de Azure
Integración de Inicio de Sesión Microsoft y Calendario Outlook mediante Microsoft Graph
1. Objetivo de la integración

Se requiere habilitar la integración entre el sistema web corporativo y Microsoft 365 para cubrir dos funcionalidades independientes dentro del mismo sistema:

1. Inicio de sesión opcional con cuenta Microsoft corporativa.
2. Vinculación del calendario de Outlook de cada usuario para sincronización bidireccional de eventos mediante Microsoft Graph.

El inicio de sesión con Microsoft será opcional. Los usuarios podrán autenticarse con su correo corporativo de Microsoft si así lo desean, sin que esto elimine necesariamente otros métodos de acceso existentes en el sistema.

La vinculación del calendario también será autorizada por cada usuario. Una vez vinculada su cuenta, el sistema podrá consultar y sincronizar eventos de Outlook conforme a los permisos autorizados.

Esto significa que los usuarios podrán:

Iniciar sesión opcionalmente con su cuenta Microsoft corporativa.
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
Nombre de las aplicaciones

Se solicita crear dos App Registrations independientes en Azure:

SIAPP Outlook DEV
SIAPP Outlook PROD

Cada aplicación debe tener sus propios identificadores, secretos, Redirect URI y Webhook URI.

URL pública de la aplicación

Desarrollo:

Servidor de desarrollo: servidor 30

URL pública de desarrollo:

Pendiente de confirmar por infraestructura/Azure según el dominio o publicación disponible para el servidor 30.

Producción:

Servidor aplicativo: 40.0.0.61

Servidor de base de datos: 40.0.0.60

Importante:

El servidor 40.0.0.60 corresponde a base de datos y no debe registrarse como URL pública, Redirect URI ni Webhook URI.

La integración OAuth y los webhooks deben apuntar al servidor aplicativo de producción 40.0.0.61 mediante la URL pública HTTPS correspondiente.

Ejemplo de URL pública productiva:

https://siapp.midominio.com

URL de autenticación (Redirect URI)

Ejemplos:

Desarrollo:

https://dev.siapp.midominio.com/auth/microsoft/callback

Producción:

https://siapp.midominio.com/auth/microsoft/callback


Esta URL será utilizada por Microsoft para devolver al usuario a la aplicación después de autenticarse.

URL de Webhook

Ejemplos:

Desarrollo:

https://dev.siapp.midominio.com/api/outlook/webhook

Producción:

https://siapp.midominio.com/api/outlook/webhook


Esta URL recibirá las notificaciones de cambios en el calendario.

Ambientes requeridos

Para evitar mezcla de credenciales, permisos, pruebas y notificaciones de calendario, se solicita manejar Desarrollo y Producción como dos aplicativos separados en Azure.

Desarrollo

Aplicación solicitada:

SIAPP Outlook DEV

Ambiente:

Servidor 30

Debe contar con:

Tenant ID
Client ID DEV
Client Secret DEV o certificado DEV
Redirect URI DEV
Webhook URI DEV
Permisos Microsoft Graph para DEV
Consentimiento administrativo para DEV

Producción

Aplicación solicitada:

SIAPP Outlook PROD

Ambiente:

Servidor aplicativo 40.0.0.61

Base de datos:

40.0.0.60

Debe contar con:

Tenant ID
Client ID PROD
Client Secret PROD o certificado PROD
Redirect URI PROD
Webhook URI PROD
Permisos Microsoft Graph para PROD
Consentimiento administrativo para PROD

Nota importante:

Aunque técnicamente Azure permite registrar varias URLs en una sola App Registration, para esta integración se solicita expresamente crear dos aplicaciones independientes: una para Desarrollo y otra para Producción. Esto mantiene separados los accesos, las credenciales y los webhooks de cada ambiente.

3. Actividades que debe realizar el Administrador de Azure
Paso 1. Ingresar al Portal Azure

Entrar a:

Azure Portal


Menú:

Microsoft Entra ID

Paso 2. Registrar las aplicaciones requeridas

Navegar a:

App registrations


Seleccionar:

New registration


Capturar una aplicación para Desarrollo y otra para Producción.

Aplicación 1:

Nombre
SIAPP Outlook DEV

Aplicación 2:

Nombre
SIAPP Outlook PROD

Supported account types

Recomendado:

Accounts in this organizational directory only


(si solo se utilizará dentro de la organización)

Redirect URI

Tipo:

Web


URLs:

Desarrollo:

https://dev.siapp.midominio.com/auth/microsoft/callback

Producción:

https://siapp.midominio.com/auth/microsoft/callback


Guardar cada aplicación por separado.

Paso 3. Obtener Application ID y Tenant ID

Una vez creadas las aplicaciones, registrar los siguientes datos para cada una:

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

SIAPP Desarrollo

y, por separado:

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

Estos permisos deben quedar configurados tanto en SIAPP Outlook DEV como en SIAPP Outlook PROD.

Paso 6. Conceder consentimiento administrativo

Seleccionar:

Grant admin consent


Confirmar.

Esto debe realizarse en ambas aplicaciones, Desarrollo y Producción. Permitirá que los usuarios utilicen la integración sin problemas de consentimiento individual si así lo define la política de la organización.

Paso 7. Validar autenticación

Confirmar que la aplicación permita:

Authorization Code Flow


Con:

PKCE


(recomendado por Microsoft para aplicaciones modernas).

Paso 8. Configurar orígenes permitidos (si aplica)

Si la aplicación utiliza frontend React, Angular o Vue, agregar los dominios autorizados.

Ejemplo:

https://dev.siapp.midominio.com
https://siapp.midominio.com

4. Información que debe entregar el Administrador de Azure

Al concluir la configuración deberá proporcionar la información separada por ambiente.

Identificadores
Tenant ID
Client ID DEV
Client ID PROD

Credenciales
Client Secret DEV
Client Secret PROD


o preferentemente

Certificado de aplicación DEV
Certificado de aplicación PROD


si la política corporativa lo requiere.

URLs registradas
Redirect URI DEV
Redirect URI PROD
Webhook URI DEV
Webhook URI PROD

Permisos otorgados

Confirmación de que existen en ambas aplicaciones:

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
Client ID DEV / Client ID PROD
Client Secret DEV / Client Secret PROD

Cada ambiente debe utilizar únicamente las credenciales que le corresponden. Desarrollo no debe utilizar credenciales productivas y Producción no debe utilizar credenciales de Desarrollo.

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
Nombre de las aplicaciones solicitadas: SIAPP Outlook DEV y SIAPP Outlook PROD.
URL pública del sistema para Desarrollo, asociada al servidor 30.
URL pública del sistema para Producción, asociada al servidor aplicativo 40.0.0.61.
Confirmación de que el servidor 40.0.0.60 es base de datos y no debe registrarse como endpoint público.
Redirect URI DEV.
Redirect URI PROD.
URL de Webhook DEV.
URL de Webhook PROD.
Ambientes requeridos: DEV y PROD.
Lo que Azure entrega a desarrollo
Tenant ID.
Client ID DEV.
Client ID PROD.
Client Secret DEV o Certificado DEV.
Client Secret PROD o Certificado PROD.
Confirmación de permisos Graph en ambas aplicaciones.
Confirmación de consentimiento administrativo en ambas aplicaciones.
URLs registradas para DEV.
URLs registradas para PROD.

Con este documento, el administrador de Azure tendrá una guía completa para crear correctamente las dos aplicaciones requeridas y el equipo de desarrollo recibirá exactamente los datos necesarios para implementar el inicio de sesión opcional con Microsoft y la sincronización bidireccional de Outlook dentro del sistema.
