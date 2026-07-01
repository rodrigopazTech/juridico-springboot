# Guía Breve de NGINX para el Equipo

## 1. Qué es NGINX

NGINX es un servidor web que en nuestro proyecto trabaja principalmente como `reverse proxy`.

Eso significa que:

- el navegador no entra directo a Spring Boot
- el navegador entra a NGINX
- NGINX reenvía la petición a nuestra app Java en `localhost:8080`

Flujo real:

```text
Navegador -> NGINX -> Spring Boot
```

## 2. Por qué lo usamos con nuestra app Java

NGINX nos ayuda a:

- exponer la app en puerto `80` o `443`
- centralizar la entrada al sistema
- controlar tamaños máximos de subida
- manejar timeouts largos
- servir rutas especiales como `/uploads/`
- permitir correctamente WebSockets y SockJS

## 3. Qué hace en nuestro sistema

En nuestra app, NGINX hace tres cosas importantes:

1. Envía la aplicación web al backend Java.
2. Sirve archivos cargados desde `/uploads/`.
3. Reenvía correctamente la ruta `/ws-realtime/` para WebSockets.

## 4. Conceptos que deben conocer

### `server`

Define un sitio o punto de entrada.

Ejemplo:

```nginx
server {
    listen 80;
    server_name _;
}
```

### `location`

Define qué hacer según la ruta pedida.

Ejemplo:

```nginx
location / {
    proxy_pass http://localhost:8080;
}
```

### `proxy_pass`

Reenvía la petición a otro servicio, en nuestro caso Spring Boot.

### Headers importantes

NGINX manda al backend información útil:

- `Host`
- `X-Real-IP`
- `X-Forwarded-For`
- `X-Forwarded-Proto`

Esto ayuda a Spring a saber desde dónde llegó la petición.

## 5. Reverse proxy explicado fácil

Sin NGINX:

```text
Navegador -> Spring Boot
```

Con NGINX:

```text
Navegador -> NGINX -> Spring Boot
```

Ventajas:

- más control
- mejor seguridad operacional
- mejor manejo de tráfico
- configuración separada del código Java

## 6. WebSockets y por qué requieren configuración especial

Nuestra app usa:

- SockJS
- STOMP
- endpoint `/ws-realtime`

Un WebSocket no es una petición HTTP normal. Empieza como HTTP, pero luego intenta cambiar de protocolo usando:

- `Upgrade: websocket`
- `Connection: upgrade`

Si NGINX no reenvía eso correctamente, el navegador ve errores como:

```text
WebSocket handshake failed
Unexpected response code: 400
```

Por eso para `/ws-realtime/` necesitamos:

```nginx
proxy_http_version 1.1;
proxy_set_header Upgrade $http_upgrade;
proxy_set_header Connection "upgrade";
proxy_buffering off;
```

## 7. Cómo está pensada nuestra configuración

Nuestra configuración tiene esta lógica:

### `location /ws-realtime/`

Se usa para WebSockets y SockJS.

### `location /`

Se usa para la aplicación normal de Spring Boot.

### `location /uploads/`

Sirve documentos y archivos cargados por usuarios.

## 8. Archivo base que usamos

La configuración de referencia del equipo está en:

[siaj.conf.local](C:/Users/Rodrigo.DESKTOP-2MLHEH3/Desktop/Proyectos_Desarrollo/juridico-springboot/nginx/siaj.conf.local)

Ese archivo replica la estructura que usamos en el servidor.

## 9. Cómo validar un cambio

Después de modificar NGINX:

```bash
sudo nginx -t
sudo systemctl reload nginx
```

Qué revisar:

- que `nginx -t` diga que la sintaxis está bien
- que la app siga respondiendo
- que `/ws-realtime/info` regrese `200`
- que el navegador ya no marque errores de handshake

## 10. Pruebas útiles

Prueba de SockJS:

```bash
curl -i http://TU_SERVIDOR/ws-realtime/info
```

Respuesta esperada:

- código `200`
- JSON con algo como `websocket:true`

Prueba funcional:

- abrir dos sesiones del sistema
- hacer un cambio en audiencias, términos o expedientes
- verificar que la otra sesión reciba actualización en tiempo real

## 11. Errores comunes

- olvidar `proxy_http_version 1.1`
- no enviar `Upgrade` y `Connection`
- no recargar NGINX después del cambio
- usar un `proxy_pass` incorrecto
- no ajustar `client_max_body_size`
- pensar que el problema está en Java cuando en realidad lo rompe el proxy

## 12. Qué debe saber cualquier miembro del equipo

Al terminar esta guía, cualquier integrante debe poder:

- explicar qué es NGINX
- identificar qué hace como reverse proxy
- ubicar el bloque de WebSockets
- saber por qué `/ws-realtime/` necesita configuración especial
- validar cambios básicos sin tocar el código Java

