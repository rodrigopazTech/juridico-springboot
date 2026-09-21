# Manual de preparacion offline de SIAJ desde macOS

## Objetivo

Preparar desde una Mac todos los archivos necesarios para instalar SIAJ en el
servidor aplicativo de produccion `40.0.0.61`, que actualmente no tiene salida
a Internet.

Este procedimiento no instala Docker en el servidor aplicativo: no es
necesario. SIAJ se ejecutara como una aplicacion Java, con NGINX como proxy
inverso. La base de datos ya opera por separado en `40.0.0.60` mediante Docker
y PostgreSQL.

## Resultado esperado

Al terminar se tendran estos artefactos para copiar por SCP al servidor:

1. Un paquete offline de RPM con Java 17, NGINX y todas sus dependencias.
2. El JAR de SIAJ compilado desde la rama aprobada.
3. Los archivos de configuracion de produccion, sin secretos dentro del
   repositorio.
4. La configuracion de NGINX y el servicio systemd de SIAJ.

## 1. Identificar la arquitectura de la Mac

En Terminal ejecutar:

```bash
uname -m
```

| Resultado | Tipo de Mac | Entorno recomendado |
| --- | --- | --- |
| `x86_64` | Intel | VMware Fusion Pro o UTM con Rocky Linux 9.1 x86_64. |
| `arm64` | Apple Silicon (M1, M2, M3, M4) | UTM con emulacion x86_64. No usar una VM ARM para preparar RPM. |

Los servidores de produccion usan Rocky Linux 9.1 de arquitectura `x86_64`.
Los RPM ARM no se pueden instalar en ellos.

## 2. Descargas requeridas en la Mac

### Obligatorio

1. Una aplicacion de virtualizacion:
   - Mac Intel: VMware Fusion Pro o UTM.
   - Mac Apple Silicon: UTM, creando una maquina emulada `x86_64`.
2. Imagen ISO de Rocky Linux 9.1 para `x86_64`.
   - Preferir `Minimal ISO`.
   - No descargar ISO para ARM/aarch64.
3. El codigo fuente actual de SIAJ o el JAR aprobado ya compilado.
4. Acceso SSH/SCP al usuario de despliegue del servidor `40.0.0.61`.

### Recomendado para compilar el JAR en la Mac

1. JDK 17 para macOS.
2. Git.

Maven no es obligatorio si se usa el Maven Wrapper incluido en el proyecto
(`./mvnw`).

### No descargar

No se requiere descargar para el servidor aplicativo:

- Docker.
- PostgreSQL.
- pgAdmin.
- Paquetes de Microsoft, Outlook o Graph.

## 3. Crear la VM de preparacion

Crear una maquina virtual con estos valores:

| Recurso | Valor minimo | Valor recomendado |
| --- | ---: | ---: |
| Sistema operativo | Rocky Linux 9.1 x86_64 | Rocky Linux 9.1 x86_64 |
| CPU | 2 nucleos | 2 o 4 nucleos |
| RAM | 4 GB | 6 GB |
| Disco | 30 GB | 40 GB |
| Red | NAT | NAT |
| Instalacion | Minimal Install | Minimal Install |

La red NAT permite que la VM descargue paquetes usando la conexion de la Mac;
no modifica ni da Internet al servidor de produccion.

Durante la instalacion:

1. Activar la interfaz de red.
2. Crear un usuario administrador.
3. Definir una contrasena de root segura.
4. Reiniciar y retirar la ISO al terminar.

## 4. Validar la VM antes de descargar

Ejecutar en la VM:

```bash
cat /etc/os-release
uname -m
ping -c 2 1.1.1.1
curl -I https://mirrors.rockylinux.org
```

La validacion correcta debe mostrar Rocky Linux 9.1, arquitectura `x86_64` y
una respuesta HTTP desde los repositorios Rocky. Si la VM no tiene red, revisar
que el adaptador virtual este conectado y configurado en modo NAT.

## 5. Descargar el paquete offline de Java y NGINX

En la VM ejecutar:

```bash
sudo dnf install -y dnf-plugins-core

mkdir -p ~/siaj-rpms

dnf download --resolve --alldeps \
  --destdir ~/siaj-rpms \
  java-17-openjdk-headless \
  java-17-openjdk \
  nginx
```

El comando descarga:

- Java 17 necesario para ejecutar el JAR de SIAJ.
- NGINX para publicar SIAJ por HTTP y reenviar al puerto local `8080`.
- Todas las dependencias RPM requeridas para instalarlos sin Internet.

Validar el contenido:

```bash
find ~/siaj-rpms -maxdepth 1 -name '*.rpm' -type f | sort
```

Crear un archivo unico para transferir:

```bash
tar -C ~ -czf ~/siaj-rpms-rocky9-x86_64.tar.gz siaj-rpms
sha256sum ~/siaj-rpms-rocky9-x86_64.tar.gz
```

Guardar el valor SHA-256. Se utilizara para confirmar que la copia al servidor
no se corrompio.

## 6. Compilar el JAR de SIAJ

En la Mac, dentro del repositorio y sobre la rama aprobada:

```bash
git branch --show-current
chmod +x mvnw
./mvnw -DskipTests package
shasum -a 256 target/sistema-juridico-0.0.1-SNAPSHOT.jar
```

El archivo a transferir es:

```text
target/sistema-juridico-0.0.1-SNAPSHOT.jar
```

No transferir un archivo `application.properties` con contrasenas o secretos.
La configuracion de produccion se preparara en el servidor con permisos
restrictivos.

## 7. Transferir los archivos al servidor aplicativo

Desde la Mac, ajustar el usuario y ejecutar:

```bash
scp ~/siaj-rpms-rocky9-x86_64.tar.gz \
  USUARIO_DESPLIEGUE@40.0.0.61:/tmp/

scp target/sistema-juridico-0.0.1-SNAPSHOT.jar \
  USUARIO_DESPLIEGUE@40.0.0.61:/tmp/sistema-juridico.jar
```

En el servidor, validar las sumas antes de instalar:

```bash
sha256sum /tmp/siaj-rpms-rocky9-x86_64.tar.gz
sha256sum /tmp/sistema-juridico.jar
```

## 8. Instalacion offline en produccion

Este paso necesita privilegios `sudo` en `40.0.0.61`.

```bash
mkdir -p /tmp/siaj-rpms
tar -xzf /tmp/siaj-rpms-rocky9-x86_64.tar.gz -C /tmp

sudo dnf install -y --disablerepo='*' /tmp/siaj-rpms/*.rpm

java -version
nginx -v
```

La opcion `--disablerepo='*'` garantiza que la instalacion no intente acceder a
Internet. Si DNF reporta una dependencia faltante, se debe descargar ese RPM y
agregarlo al paquete offline; no se debe forzar la instalacion ignorando
dependencias.

## 9. Preparacion pendiente antes de activar SIAJ

Antes de arrancar el aplicativo deben estar definidos y aprobados:

1. La base de datos `sistema_juridico` en `40.0.0.60`.
2. El usuario de base de datos y su contrasena de produccion.
3. Si la base iniciara vacia con catalogos iniciales o se restaurara una copia
   autorizada.
4. La cuenta local administradora inicial de SIAJ.
5. La configuracion de correo y Microsoft, si se desea habilitarlas despues.

SIAJ puede iniciar con autenticacion local aunque SMTP, Microsoft Entra y
Microsoft Graph aun no esten disponibles.

## 10. Archivos que se instalaran en el despliegue

En el siguiente paso de despliegue se crearan estos archivos en produccion:

```text
/opt/siaj/app/sistema-juridico.jar
/etc/siaj/siaj.env
/etc/systemd/system/siaj.service
/etc/nginx/conf.d/siaj.conf
/home/ajappprod/uploads/
```

El archivo `/etc/siaj/siaj.env` tendra permisos restringidos porque contiene
credenciales de base de datos y, si se habilita, configuracion SMTP/Microsoft.

## 11. Verificacion final esperada

Al concluir el despliegue se verificara:

```bash
systemctl status siaj
systemctl status nginx
curl -I http://localhost:8080/
curl -I http://localhost/
```

El resultado esperado es que SIAJ responda por el puerto local `8080` y NGINX
lo publique por el puerto `80`.

## Seguridad

- No guardar contrasenas en el repositorio, scripts o archivos compartidos.
- No copiar la base de pruebas a produccion sin autorizacion explicita.
- Conservar los SHA-256 de los archivos transferidos.
- Retirar permisos sudo temporales de despliegue cuando se complete la
  instalacion.
