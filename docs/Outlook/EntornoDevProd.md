Mi recomendación profesional es crear dos aplicaciones distintas, una para Desarrollo y otra para Producción.

Opción 1 (Recomendada): Dos aplicaciones
Aplicación Desarrollo
SIAPP Calendario Outlook DEV


URLs:

https://dev.siapp.midominio.com/auth/microsoft/callback
https://dev.siapp.midominio.com/api/outlook/webhook


Obtendrás:

Client ID DEV
Client Secret DEV

Aplicación Producción
SIAPP Calendario Outlook PROD


URLs:

https://siapp.midominio.com/auth/microsoft/callback
https://siapp.midominio.com/api/outlook/webhook


Obtendrás:

Client ID PROD
Client Secret PROD

Ventajas

✅ Aislamiento completo.

✅ Si modificas permisos en DEV no afectas Producción.

✅ Puedes probar cambios sin riesgo.

✅ Los administradores de seguridad suelen preferir esta práctica.

✅ Facilita auditorías.

✅ Permite rotar secretos de DEV sin tocar PROD.

✅ Si accidentalmente un desarrollador expone una credencial de DEV, Producción sigue protegida.

Opción 2: Una sola aplicación con múltiples URLs

Azure permite registrar varias Redirect URI:

https://dev.siapp.midominio.com/auth/microsoft/callback

https://siapp.midominio.com/auth/microsoft/callback


También podrías usar el mismo:

Client ID
Client Secret


para ambos entornos.

Desventajas

❌ DEV y PROD comparten credenciales.

❌ Un cambio de permisos impacta ambos entornos.

❌ Riesgo de configuración cruzada.

❌ Más difícil de auditar.

❌ Menor separación de responsabilidades.

Lo que yo pediría al administrador de Azure

Agregar una sección al documento:

Ambientes requeridos

Se solicita crear las siguientes aplicaciones:

Desarrollo
Nombre:
SIAPP Calendario Outlook DEV


Entregar:

Tenant ID
Client ID
Client Secret

Producción
Nombre:
SIAPP Calendario Outlook PROD


Entregar:

Tenant ID
Client ID
Client Secret

Permisos para ambas
User.Read
Calendars.Read
Calendars.ReadWrite
offline_access

Consentimiento administrativo

Solicitar que el consentimiento quede otorgado en ambas aplicaciones.

Un detalle muy importante para tu caso

Como quieres implementar webhooks de Microsoft Graph para sincronización bidireccional, también es recomendable que cada ambiente tenga su propio endpoint:

DEV
https://dev.siapp.midominio.com/api/outlook/webhook

PROD
https://siapp.midominio.com/api/outlook/webhook


Así evitarás que eventos de pruebas terminen llegando al ambiente productivo.

En resumen: técnicamente se puede hacer con una sola App Registration agregando varias URLs, pero para una aplicación empresarial que tendrá sincronización de calendarios, webhooks y acceso a datos de usuarios, la práctica recomendada es 2 App Registrations independientes: DEV y PROD. Esto te dará una operación mucho más segura, mantenible y alineada con buenas prácticas de Azure y Microsoft Graph.