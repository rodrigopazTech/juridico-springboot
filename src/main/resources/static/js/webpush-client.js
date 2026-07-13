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

let serviceWorkerRegistration = null;

// Registra el Service Worker de forma silenciosa al cargar la página
async function registerServiceWorkerOnly() {
    if (!('serviceWorker' in navigator) || !('PushManager' in window)) {
        console.warn('[WebPush] Las notificaciones Push no son soportadas en este navegador.');
        return null;
    }
    try {
        const registration = await navigator.serviceWorker.register('/sw.js');
        console.log('[WebPush] Service Worker registrado con éxito en scope:', registration.scope);
        serviceWorkerRegistration = registration;
        return registration;
    } catch (error) {
        console.error('[WebPush] Error al registrar Service Worker:', error);
        return null;
    }
}

// Realiza la suscripción al servidor una vez concedido el permiso
async function subscribeUser() {
    try {
        const registration = serviceWorkerRegistration || await navigator.serviceWorker.ready;
        if (!registration) return;

        let subscription = await registration.pushManager.getSubscription();

        if (!subscription) {
            console.log('[WebPush] Creando nueva suscripción...');
            const response = await fetch('/api/webpush/public-key');
            if (!response.ok) {
                throw new Error('[WebPush] No se pudo obtener la clave pública VAPID del backend.');
            }
            const data = await response.json();
            const publicKey = urlBase64ToUint8Array(data.publicKey);

            subscription = await registration.pushManager.subscribe({
                userVisibleOnly: true,
                applicationServerKey: publicKey
            });

            console.log('[WebPush] Dispositivo registrado en el navegador.');
            await sendSubscriptionToBackend(subscription);
        } else {
            console.log('[WebPush] Suscripción existente detectada.');
            await sendSubscriptionToBackend(subscription);
        }
    } catch (error) {
        console.error('[WebPush] Error al suscribir al usuario:', error);
    }
}

// Muestra el popover flotante apuntando a la campana
function showPushPopover() {
    // Verificar si el permiso es default y no ha sido descartado previamente por el usuario
    if (Notification.permission !== 'default' || localStorage.getItem('webpush_popover_dismissed') === 'true') {
        return;
    }

    const wrapper = document.getElementById('notif-bell-wrapper');
    if (!wrapper) return;

    // Evitar duplicados
    if (document.getElementById('webpush-popover')) return;

    const popover = document.createElement('div');
    popover.id = 'webpush-popover';
    
    // Estilos premium: posicionado debajo de la campana, con sombra, transición y flecha indicadora
    popover.className = 'absolute top-[60px] right-0 z-[60] w-72 bg-white rounded-2xl shadow-2xl border border-gray-100 p-4 flex flex-col gap-2.5 transition-all duration-300 transform scale-95 opacity-0';
    popover.innerHTML = `
        <!-- Flechita apuntando a la campana -->
        <div class="absolute -top-1.5 right-4.5 w-3 h-3 rotate-45 bg-white border-t border-l border-gray-100"></div>
        <!-- Botón cerrar (X) -->
        <button id="webpush-popover-close" class="absolute top-2 right-2 text-gray-300 hover:text-gray-500 text-xs leading-none transition-colors" aria-label="Cerrar">
            <i class="fas fa-times"></i>
        </button>
        
        <div class="flex items-start gap-2.5">
            <div class="w-8 h-8 rounded-full bg-gob-oro/10 flex items-center justify-center text-gob-oro flex-shrink-0">
                <i class="fas fa-bell"></i>
            </div>
            <div class="flex-1 min-w-0">
                <p class="text-xs font-bold text-gray-800">Notificaciones en tiempo real</p>
                <p class="text-[11px] text-gray-500 mt-1 leading-normal">Recibe alertas al instante sobre tus audiencias, términos y expedientes.</p>
                <p class="text-[10px] text-gob-oro font-bold mt-1.5 flex items-center gap-1">
                    <i class="fas fa-info-circle text-[9px]"></i> Se recomienda activar esta opción
                </p>
            </div>
        </div>
        
        <div class="flex justify-end gap-2 mt-1.5 border-t border-gray-100 pt-2">
            <button id="webpush-popover-dismiss" class="px-2.5 py-1 text-gray-400 hover:text-gray-600 text-[11px] font-bold transition-colors">
                Ahora no
            </button>
            <button id="webpush-popover-activate" class="px-3.5 py-1 bg-gob-guinda text-white text-[11px] font-bold rounded-lg hover:bg-red-800 transition-colors shadow-sm">
                Activar
            </button>
        </div>
    `;

    wrapper.appendChild(popover);

    // Animación de entrada fluida
    requestAnimationFrame(() => {
        requestAnimationFrame(() => {
            popover.classList.remove('scale-95', 'opacity-0');
            popover.classList.add('scale-100', 'opacity-100');
        });
    });

    const btnActivate = popover.querySelector('#webpush-popover-activate');
    const btnDismiss = popover.querySelector('#webpush-popover-dismiss');

    btnActivate.addEventListener('click', async (e) => {
        e.stopPropagation();
        btnActivate.disabled = true;
        btnActivate.textContent = 'Activando...';
        
        try {
            const permission = await Notification.requestPermission();
            if (permission === 'granted') {
                console.log('[WebPush] Permiso concedido por el usuario.');
                dismissPopover();
                await subscribeUser();
            } else {
                console.warn('[WebPush] Permiso denegado por el usuario o navegador.');
                btnActivate.textContent = 'Denegado';
                setTimeout(dismissPopover, 1500);
            }
        } catch (err) {
            console.error('[WebPush] Error al solicitar permiso:', err);
            btnActivate.disabled = false;
            btnActivate.textContent = 'Activar';
        }
    });

    btnDismiss.addEventListener('click', (e) => {
        e.stopPropagation();
        localStorage.setItem('webpush_popover_dismissed', 'true');
        dismissPopover();
    });

    const btnClose = popover.querySelector('#webpush-popover-close');
    btnClose.addEventListener('click', (e) => {
        e.stopPropagation();
        dismissPopover();
    });

    // Función para cerrar con animación de salida
    function dismissPopover() {
        popover.classList.remove('scale-100', 'opacity-100');
        popover.classList.add('scale-95', 'opacity-0');
        popover.addEventListener('transitionend', () => popover.remove(), { once: true });
    }

    // Ocultar si el usuario abre el menú real de la campana
    const bellBtn = document.getElementById('notif-bell-btn');
    if (bellBtn) {
        bellBtn.addEventListener('click', dismissPopover, { once: true });
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

// Desuscribirse del Web Push antes de hacer logout
async function unsubscribeUserBeforeLogout(e, logoutLink) {
    if (!('serviceWorker' in navigator) || !('PushManager' in window)) {
        return;
    }
    
    // Prevenir la redirección inmediata del logout
    e.preventDefault();
    const targetLink = logoutLink || (e.target && e.target.closest('a[href*="/logout"]'));
    const logoutUrl = (targetLink && typeof targetLink.getAttribute === 'function')
        ? targetLink.getAttribute('href')
        : '/logout';

    try {
        // Obtenemos el registro activo de forma inmediata sin quedarnos esperando colgados si no existe
        const registration = await navigator.serviceWorker.getRegistration();
        if (registration) {
            const subscription = await registration.pushManager.getSubscription();
            if (subscription) {
                console.log('[WebPush] Desuscribiendo del servidor...');
                const csrfTokenEl = document.querySelector('meta[name="_csrf"]');
                const csrfHeaderEl = document.querySelector('meta[name="_csrf_header"]');
                
                const headers = {
                    'Content-Type': 'application/json'
                };
                if (csrfTokenEl && csrfHeaderEl) {
                    headers[csrfHeaderEl.content] = csrfTokenEl.content;
                }

                // Definimos un límite de tiempo de 3 segundos para la petición al servidor
                const controller = new AbortController();
                const timeoutId = setTimeout(() => controller.abort(), 3000);

                try {
                    await fetch('/api/webpush/unsubscribe', {
                        method: 'POST',
                        headers: headers,
                        body: JSON.stringify({ endpoint: subscription.endpoint }),
                        signal: controller.signal
                    });
                } catch (fetchErr) {
                    console.error('[WebPush] Error o tiempo de espera agotado al desuscribir del servidor:', fetchErr);
                } finally {
                    clearTimeout(timeoutId);
                }

                // Desuscribir en el navegador
                await subscription.unsubscribe();
                console.log('[WebPush] Desuscrito con éxito del navegador.');
            }
        }
    } catch (err) {
        console.error('[WebPush] Error durante la desuscripción de cierre de sesión:', err);
    } finally {
        // Proceder con la redirección al logout original
        window.location.href = logoutUrl;
    }
}

// Iniciar Web Push al cargar el DOM
document.addEventListener('DOMContentLoaded', () => {
    console.log('[WebPush] Inicializando registro de Service Worker...');
    registerServiceWorkerOnly().then(() => {
        if (Notification.permission === 'granted') {
            console.log('[WebPush] Permiso ya concedido previamente. Suscribiendo...');
            subscribeUser();
        } else if (Notification.permission === 'default') {
            // Mostrar el popover flotante después de 1.5 segundos para que la carga sea fluida
            setTimeout(showPushPopover, 1500);
        }
    });

    // Interceptar clics en enlaces de logout para desuscribir (funciona en móvil y PC)
    document.addEventListener('click', (e) => {
        const logoutLink = e.target.closest('a[href*="/logout"]');
        if (logoutLink) {
            unsubscribeUserBeforeLogout(e, logoutLink);
        }
    });
});
