# Configuración dinámica de la contraseña SMTP

## Objetivo

El administrador puede registrar, probar y activar una contraseña SMTP sin reiniciar SIAJ. La contraseña se guarda cifrada; la pantalla sólo conserva y muestra metadatos de la rotación.

## Preparación única de producción

1. Genere una llave aleatoria de 32 bytes codificada en Base64. No la guarde en el repositorio ni en la base de datos:

   ```bash
   openssl rand -base64 32
   ```

2. Guárdela en `/etc/siaj/siaj.env`:

   ```text
   MAIL_CREDENTIAL_ENCRYPTION_KEY=<valor-generado>
   MAIL_DYNAMIC_REFRESH_MS=10000
   ```

3. Restrinja los permisos del archivo al usuario del servicio.
4. Ejecute `deploy/database/07-dynamic-smtp-credentials.sql` si el despliegue no permite que Hibernate cree la tabla.
5. Reinicie SIAJ esta única vez para cargar la llave maestra. Las rotaciones posteriores de la contraseña SMTP no requieren reinicio.

La llave maestra debe incluirse en el respaldo seguro de secretos. Perderla impide descifrar las contraseñas SMTP almacenadas. Cambiarla requiere un proceso explícito de recifrado; no se debe reemplazar directamente mientras existan registros activos.

## Operación mensual

1. Ingrese como usuario con autoridad `ADMIN`.
2. Abra **Administración > Configuración de correo**.
3. Capture la contraseña nueva, un destinatario de prueba y la próxima fecha de cambio.
4. Presione **Guardar cifrada**.
5. En el historial, presione **Probar**.
6. Confirme que llegó el mensaje y que el registro aparece como `PROBADA`.
7. Presione **Activar**.

La configuración se invalida inmediatamente en el nodo donde se activa. Los demás nodos la detectan al siguiente uso, con un intervalo máximo predeterminado de 10 segundos.

Si la nueva contraseña presenta un problema, use **Restaurar anterior**. La restauración conserva como anterior la credencial que estaba activa para mantener la trazabilidad.

## Seguridad

- La base contiene únicamente AES-256-GCM, IV y etiqueta de autenticación; nunca la contraseña en claro.
- La llave maestra permanece fuera de la base.
- Sólo `/admin/**` puede ejecutar el flujo y esa ruta exige autoridad `ADMIN` en `SecurityConfig`.
- Cada registro conserva quién lo creó, probó y activó, junto con sus fechas y resultados.
- Los errores de prueba se limitan y se filtran antes de persistirse.
- Mientras no exista una credencial dinámica activa, SIAJ continúa utilizando `spring.mail.password` como respaldo de transición.
