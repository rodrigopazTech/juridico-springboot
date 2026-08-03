# Funciones pendientes de permisos Microsoft Graph

Este documento concentra las integraciones que SIAJ podria habilitar con Microsoft Graph cuando el area administradora de Azure autorice los permisos correspondientes.

## Solicitud inmediata para vinculacion de calendario Outlook

Aunque estos permisos aparecen en Azure como tipo `Delegada` y con `Se necesita consentimiento de administrador: No`, el tenant institucional esta bloqueando el consentimiento directo del usuario. Por eso se solicita que un administrador conceda consentimiento para la aplicacion usada por SIAJ.

| API / permiso solicitado | Tipo | Consentimiento admin segun Azure | Uso exacto en SIAJ |
| --- | --- | --- | --- |
| `offline_access` | Delegada | No | Mantener la vinculacion del calendario Outlook del usuario mediante refresh token, sin pedir inicio de sesion cada vez que confirme crear, actualizar o eliminar un evento. |
| `Calendars.ReadWrite` | Delegada | No | Crear, actualizar y eliminar eventos en el calendario Outlook del usuario solo cuando el usuario lo confirme desde SIAJ. |

Redirect URI requerido para esta vinculacion:

```text
http://localhost:8080/perfil/outlook/callback
```

Si se prueba desde la IP local del equipo:

```text
http://172.16.143.17:8080/perfil/outlook/callback
```

Scopes que SIAJ solicita actualmente para calendario:

```text
offline_access https://graph.microsoft.com/Calendars.ReadWrite
```

| API / permiso solicitado | Descripcion clara de uso en SIAJ |
| --- | --- |
| `User.ReadBasic.All` | Buscar usuarios del directorio institucional desde el alta/edicion de usuarios para validar correos y autollenar datos basicos antes de guardar en SIAJ. |
| `GET /users/{id \| userPrincipalName}?$select=businessPhones,displayName,jobTitle,mail,mobilePhone,officeLocation,userPrincipalName,id` | Consultar el perfil institucional de una persona especifica por correo o UPN para autollenar nombre, puesto, telefonos, oficina y correo en el formulario de registro. |
| `Calendars.ReadBasic` | Consultar informacion basica del calendario del usuario vinculado para mostrar disponibilidad o eventos generales relacionados con agenda juridica. |
| `Calendars.ReadWrite` | Crear, actualizar y cancelar eventos de Outlook desde audiencias, terminos y recordatorios registrados en SIAJ. |
| `Mail.Send` | Enviar correos institucionales desde SIAJ para activacion de cuentas, recuperacion de contrasena, avisos de terminos, audiencias, asignaciones y cambios relevantes. |
| `Mail.ReadBasic` | Leer metadatos basicos de correos de un buzon vinculado o compartido para identificar mensajes relacionados con expedientes sin descargar contenido sensible completo. |
| `Mail.Read` | Leer mensajes y adjuntos cuando se autorice un flujo de recepcion documental para vincular oficios, promociones o comunicaciones a expedientes. |
| `Files.ReadWrite.All` o permiso SharePoint equivalente aprobado | Guardar, consultar y versionar documentos de expedientes en OneDrive/SharePoint mediante Graph, manteniendo trazabilidad documental institucional. |
| `Sites.ReadWrite.All` o permiso SharePoint equivalente aprobado | Integrar bibliotecas documentales de SharePoint con el gestor documental de SIAJ para carpetas por expediente, minutas, matrices y evidencias. |
| `Group.Read.All` o permiso de grupos equivalente aprobado | Consultar grupos institucionales autorizados para apoyar asignaciones por area, gerencia o equipo cuando el directorio lo permita. |
| Microsoft Graph change notifications para `messages`, `events` y `driveItem` | Recibir webhooks de cambios en correos, eventos o documentos para sincronizar SIAJ sin consultas constantes. |

## Funciones posibles con permisos actuales

Mientras no se autoricen permisos de lectura amplia del directorio, la integracion debe limitarse a datos del usuario que inicia sesion con Microsoft.

| Permiso disponible | Que permite hacer ahora |
| --- | --- |
| `openid`, `profile`, `email` | Autenticar al usuario con Microsoft Entra ID y obtener identidad basica para iniciar sesion de forma secundaria. |
| `User.Read` | Consultar el perfil del usuario autenticado mediante `/me`, incluyendo datos como nombre, correo, puesto, telefonos y ubicacion cuando esten disponibles. |
