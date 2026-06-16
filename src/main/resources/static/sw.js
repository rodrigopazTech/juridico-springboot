self.addEventListener('push', function(event) {
    console.log('[Service Worker] Notificación Push recibida.');
    if (!event.data) {
        console.log('[Service Worker] Evento de push sin datos.');
        return;
    }

    let data = {};
    try {
        data = event.data.json();
    } catch (e) {
        console.error('[Service Worker] Error al parsear JSON del push:', e);
        data = {
            title: 'Sistema Jurídico',
            body: event.data.text(),
            url: '/notificaciones'
        };
    }

    const title = data.title || 'Sistema Jurídico';
    const options = {
        body: data.body || 'Tienes una nueva actualización en el sistema.',
        icon: '/img/logo.png',
        badge: '/img/badge-72.png',
        vibrate: [100, 50, 100],
        tag: 'juridico-notif-' + (data.url || 'general'),
        renotify: true,
        actions: [
            {
                action: 'ver',
                title: 'Ver detalles'
            },
            {
                action: 'ignorar',
                title: 'Ignorar'
            }
        ],
        data: {
            url: data.url || '/notificaciones'
        }
    };

    event.waitUntil(
        self.registration.showNotification(title, options)
    );
});

self.addEventListener('notificationclick', function(event) {
    console.log('[Service Worker] Clic en notificación. Acción:', event.action);
    event.notification.close();

    // Si el usuario tocó "Ignorar", solo cerrar la notificación
    if (event.action === 'ignorar') {
        return;
    }

    // Para "ver" o clic directo en el cuerpo, navegar a la URL
    const urlRelativa = event.notification.data.url;

    event.waitUntil(
        clients.matchAll({ type: 'window', includeUncontrolled: true }).then(function(windowClients) {
            // Usar el origen del cliente activo para evitar que quede hardcodeado
            // el origen de ngrok u otro túnel que se haya usado previamente
            const origenReal = windowClients.length > 0
                ? new URL(windowClients[0].url).origin
                : self.location.origin;

            let clickUrl = urlRelativa;
            if (clickUrl && !clickUrl.startsWith('http')) {
                clickUrl = new URL(clickUrl, origenReal).href;
            }

            // Si ya hay una ventana abierta, redirigirla y enfocarla
            for (var i = 0; i < windowClients.length; i++) {
                var client = windowClients[i];
                if ('focus' in client) {
                    return client.focus().then(function() {
                        return client.navigate(clickUrl);
                    });
                }
            }
            // Si no hay ventanas abiertas, abrir una nueva
            if (clients.openWindow) {
                return clients.openWindow(clickUrl);
            }
        })
    );
});
