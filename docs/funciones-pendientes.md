# Funciones pendientes de permisos Microsoft Graph

Este documento concentra las integraciones que SIAJ podria habilitar con Microsoft Graph cuando el area administradora de Azure autorice los permisos correspondientes.

## Alcance actual

La integracion activa de SIAJ se limita al inicio de sesion con Microsoft Entra ID y a consultar el perfil del usuario autenticado. No se solicitan ni se usan permisos de calendario, correo, archivos o directorio institucional.

| API / permiso solicitado | Descripcion clara de uso en SIAJ |
| --- | --- |
| `User.ReadBasic.All` | Buscar usuarios del directorio institucional desde el alta/edicion de usuarios para validar correos y autollenar datos basicos antes de guardar en SIAJ. |
| `GET /users/{id \| userPrincipalName}?$select=businessPhones,displayName,jobTitle,mail,mobilePhone,officeLocation,userPrincipalName,id` | Consultar el perfil institucional de una persona especifica por correo o UPN para autollenar nombre, puesto, telefonos, oficina y correo en el formulario de registro. |
| `Calendars.ReadWrite` | Futura integracion, no incluida en el alcance actual: crear, actualizar y cancelar eventos de Outlook desde SIAJ. |
| `Mail.Send` | Futura integracion, no incluida en el alcance actual: enviar correos institucionales como el usuario autenticado. |
| `Mail.Read` | Futura integracion, no incluida en el alcance actual: leer mensajes y adjuntos para recepcion documental. |
| `Files.ReadWrite.All` o permiso SharePoint equivalente aprobado | Futura integracion documental con OneDrive o SharePoint para expedientes. |
| `Sites.ReadWrite.All` o permiso SharePoint equivalente aprobado | Futura integracion con bibliotecas documentales de SharePoint. |
| `Group.Read.All` o permiso de grupos equivalente aprobado | Futura consulta de grupos institucionales para apoyar asignaciones por area. |

## Funciones posibles con permisos actuales

Mientras no se autoricen permisos de lectura amplia del directorio, la integracion debe limitarse a datos del usuario que inicia sesion con Microsoft.

| Permiso disponible | Que permite hacer ahora |
| --- | --- |
| `openid`, `profile`, `email` | Autenticar al usuario con Microsoft Entra ID y obtener identidad basica para iniciar sesion de forma secundaria. |
| `User.Read` | Consultar el perfil del usuario autenticado mediante `/me`, incluyendo datos como nombre, correo, puesto, telefonos y ubicacion cuando esten disponibles. |
