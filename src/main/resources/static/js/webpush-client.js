// webpush-client.js

// Función para convertir la llave pública VAPID (base64url) a un UInt8Array para el navegador
function urlBase64ToUint8Array(base64String) {
    const padding = '='.repeat((4 - base64String.length % 4) % 4);
    const base64 = (base64String + padding)
        .replace(/\-/g, '+')
        .replace(/_/g, '/');

    const rawData = window.atob(base64);
    const outputArray = new Uint8Array(rawData.length);

    for (let i = 0; i < rawData.length; ++i) {
        outputArray[i] = rawData.charCodeAt(i);
    }
    return outputArray;
}

async function initWebPush() {
    // Comprobar soporte
    if (!('serviceWorker' in navigator) || !('PushManager' in window)) {
        console.warn('[WebPush] Las notificaciones Push no son soportadas en este navegador.');
        return;
    }

    try {
        // Registrar Service Worker (sw.js en la raíz del contexto de Spring Boot)
        const registration = await navigator.serviceWorker.register('/sw.js');
        console.log('[WebPush] Service Worker registrado con éxito en scope:', registration.scope);

        // Esperar a que el service worker esté listo
        await navigator.serviceWorker.ready;

        // Comprobar estado de permisos de notificación
        if (Notification.permission === 'denied') {
            console.warn('[WebPush] El permiso para recibir notificaciones está denegado.');
            return;
        }

        if (Notification.permission === 'default') {
            // Solicitar permisos de manera nativa al usuario
            const permission = await Notification.requestPermission();
            if (permission !== 'granted') {
                console.warn('[WebPush] Permiso para notificaciones denegado.');
                return;
            }
        }

        // Obtener suscripción actual si existe
        let subscription = await registration.pushManager.getSubscription();

        if (!subscription) {
            console.log('[WebPush] Creando nueva suscripción...');
            // Obtener la clave pública VAPID desde el backend
            const response = await fetch('/api/webpush/public-key');
            if (!response.ok) {
                throw new Error('[WebPush] No se pudo obtener la clave pública VAPID del backend.');
            }
            const data = await response.json();
            const publicKey = urlBase64ToUint8Array(data.publicKey);

            // Registrar suscripción en el navegador
            subscription = await registration.pushManager.subscribe({
                userVisibleOnly: true,
                applicationServerKey: publicKey
            });

            console.log('[WebPush] Dispositivo registrado en el navegador.');
            await sendSubscriptionToBackend(subscription);
        } else {
            console.log('[WebPush] Suscripción existente detectada.');
            // Enviar de todas formas para verificar coherencia del lado del servidor
            await sendSubscriptionToBackend(subscription);
        }

    } catch (error) {
        console.error('[WebPush] Error al inicializar notificaciones push:', error);
    }
}

function getOrCreateDeviceId() {
    let deviceId = localStorage.getItem('webpush_device_id');
    if (!deviceId) {
        deviceId = 'device-' + Math.random().toString(36).substring(2, 15) + '-' + Math.random().toString(36).substring(2, 15);
        localStorage.setItem('webpush_device_id', deviceId);
    }
    return deviceId;
}

async function sendSubscriptionToBackend(subscription) {
    const rawKeys = subscription.toJSON().keys;
    
    // Obtener CSRF token si existe en la página (Spring Security por defecto)
    const csrfTokenEl = document.querySelector('meta[name="_csrf"]');
    const csrfHeaderEl = document.querySelector('meta[name="_csrf_header"]');
    
    const headers = {
        'Content-Type': 'application/json'
    };
    
    if (csrfTokenEl && csrfHeaderEl) {
        headers[csrfHeaderEl.content] = csrfTokenEl.content;
    }

    const payload = {
        endpoint: subscription.endpoint,
        deviceId: getOrCreateDeviceId(),
        keys: {
            p256dh: rawKeys.p256dh,
            auth: rawKeys.auth
        }
    };

    const response = await fetch('/api/webpush/subscribe', {
        method: 'POST',
        headers: headers,
        body: JSON.stringify(payload)
    });

    if (!response.ok) {
        console.error('[WebPush] Error al registrar la suscripción en el backend:', response.status);
    } else {
        console.log('[WebPush] Suscripción sincronizada con éxito en el servidor.');
    }
}

// Función para detectar si el cliente es un dispositivo móvil o tablet
function isMobileDevice() {
    const userAgent = navigator.userAgent || navigator.vendor || window.opera;
    // Detección por expresión regular del User Agent
    const isMobileUA = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(userAgent);
    // Detección por tamaño de pantalla (móviles y tablets suelen tener anchos <= 1024px)
    const isSmallScreen = window.matchMedia && window.matchMedia('(max-width: 1024px)').matches;
    return isMobileUA || isSmallScreen;
}

// Iniciar Web Push al cargar el DOM solo si es un dispositivo móvil
document.addEventListener('DOMContentLoaded', () => {
    if (isMobileDevice()) {
        console.log('[WebPush] Dispositivo móvil detectado. Inicializando registro de Service Worker y suscripción push...');
        initWebPush();
    } else {
        console.log('[WebPush] Entorno de escritorio detectado. Notificaciones en segundo plano Web Push omitidas de manera silenciosa.');
    }
});
