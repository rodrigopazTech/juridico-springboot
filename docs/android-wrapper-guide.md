# Guía de Configuración e Inicio para el Wrapper de Android (Capacitor)

Esta guía detalla los pasos y configuraciones necesarios para levantar, probar y sincronizar el wrap de Android del **Sistema Jurídico (SIAJ)** utilizando Capacitor.

---

## 📋 Requisitos Previos

Antes de comenzar, asegúrate de tener instalado en tu equipo de desarrollo:
1. **Node.js** (Versión 18 o superior recomendada) junto con `npm`.
2. **Java Development Kit (JDK)** (Versión 17 es la requerida para el backend y compatible con Android Gradle).
3. **Android Studio** instalado con:
   - Android SDK (versión API 34 o superior recomendada).
   - Android SDK Command-line Tools.
   - Un emulador configurado o un dispositivo físico Android con la *Depuración por USB* activada.

---

## 🚀 Pasos de Instalación y Sincronización

Sigue estos pasos en tu terminal desde la raíz del proyecto para inicializar el entorno de desarrollo móvil:

### 1. Instalar las dependencias de Node
Instala Capacitor y los plugins móviles necesarios definidos en el `package.json`:
```bash
npm install
```

### 2. Sincronizar Capacitor con el proyecto Android
Este comando compila/copia los archivos de configuración y sincroniza todos los plugins instalados (como el sistema de notificaciones locales) con el código nativo de la carpeta `android/`:
```bash
npx cap sync
```

### 3. Abrir el proyecto en Android Studio
Para compilar y correr la aplicación en un emulador o dispositivo real:
```bash
npx cap open android
```
*Este comando abrirá automáticamente Android Studio importando el directorio nativo `android/`.*

---

## ⚙️ Configuración del Servidor y Entorno de Red

Capacitor está configurado mediante el archivo `capacitor.config.json` en la raíz. Este archivo le indica a la aplicación móvil dónde cargar el frontend web:

```json
{
  "appId": "com.finabien.siaj",
  "appName": "Sistema Jurídico",
  "webDir": "src/main/resources/static",
  "server": {
    "url": "http://[TU_IP_LOCAL]:[PUERTO]",
    "cleartext": true,
    "allowNavigation": [
      "https://url_de_ngrok_si_tienes_una",
      "http://[TU_IP_LOCAL]:[PUERTO]"
    ]
  }
}
```

### Explicación de campos clave:
*   **`webDir`**: Es el directorio local donde Capacitor busca recursos HTML estáticos iniciales.
*   **`server.url`**: Dirección del backend en desarrollo al que apunta el WebView. Debe configurarse con la IP y el puerto en el que corre el backend en tu entorno local.
*   **`server.cleartext`**: Habilitado en `true` para permitir conexiones HTTP no seguras de desarrollo.
*   **`server.allowNavigation`**: Lista de URLs externas e internas permitidas a las que la aplicación web móvil tiene autorización para navegar o interactuar (por ejemplo, para integraciones de túneles como ngrok).

---

## 🌐 Cómo Cambiar la IP Local de Backend

Si estás corriendo el servidor backend localmente en tu propia máquina y probando en un emulador o dispositivo físico, deberás reemplazar los marcadores genéricos por tu dirección IP local.

Sigue estas recomendaciones para ajustarlo:

### Opción A: Modificar la IP local en `capacitor.config.json`
1. Abre `capacitor.config.json` en tu editor.
2. Cambia el valor de `"url"` y de `"allowNavigation"` por la dirección IP local de tu máquina de desarrollo.
   * *Ejemplo (si tu IP es `192.168.1.15`):*
     ```json
     "url": "http://192.168.1.15:8080",
     "allowNavigation": [
       "http://192.168.1.15:8080"
     ]
     ```
3. Ejecuta `npx cap sync` para aplicar el cambio en el wrapper.
4. **Nota importante:** Intenta no subir este cambio de IP personal al repositorio Git para no afectar el entorno común.

### Opción B: Uso de Túneles (ngrok)
Si usas un túnel público para pruebas remotas o para recibir Web Push Notifications:
1. Inicia ngrok apuntando a tu puerto local:
   ```bash
   ngrok http 8080
   ```
2. Copia la URL provista por ngrok (por ejemplo, `https://mi-tunel.ngrok-free.dev`).
3. Modifica la propiedad `"url"` en `capacitor.config.json` con tu URL de ngrok.
4. Sincroniza los cambios: `npx cap sync`.

---

## 🛠️ Resolución de Problemas Comunes (FAQ)

### 1. La aplicación muestra una pantalla en blanco o error de conexión
*   **Causa:** La IP configurada en `server.url` no es accesible desde el emulador o dispositivo móvil.
*   **Solución:** Asegúrate de que el dispositivo móvil y tu equipo de desarrollo estén en la misma red Wi-Fi. Alternativamente, puedes usar `10.0.2.2:8080` si estás probando con el emulador oficial de Android Studio para hacer referencia a `localhost` del host de desarrollo.

### 2. Error: "Cleartext HTTP traffic not permitted"
*   **Causa:** Android bloquea las conexiones `http://` no seguras por defecto.
*   **Solución:** Capacitor gestiona esto automáticamente si `"cleartext": true` está habilitado en `capacitor.config.json`. Sin embargo, si surge el error, verifica que en `android/app/src/main/AndroidManifest.xml` la etiqueta `<application>` contenga la propiedad:
    ```xml
    android:usesCleartextTraffic="true"
    ```

### 3. Los cambios que realicé en `capacitor.config.json` no se reflejan en el celular
*   **Solución:** Cada vez que modifiques archivos de Capacitor o assets del frontend, debes ejecutar:
    ```bash
    npx cap sync
    ```
    Y luego volver a reconstruir y ejecutar la app en tu dispositivo desde Android Studio.
