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
    let clickUrl = event.notification.data.url;

    // Garantizar que la URL sea absoluta dentro de la aplicación
    if (clickUrl && !clickUrl.startsWith('http')) {
        clickUrl = new URL(clickUrl, self.location.origin).href;
    }

    event.waitUntil(
        clients.matchAll({ type: 'window', includeUncontrolled: true }).then(function(windowClients) {
            // Si ya hay una pestaña abierta de nuestro sitio, redirigirla y enfocarla
            for (var i = 0; i < windowClients.length; i++) {
                var client = windowClients[i];
                if (client.url.startsWith(self.location.origin) && 'focus' in client) {
                    return client.focus().then(function() {
                        return client.navigate(clickUrl);
                    });
                }
            }
            // Si no hay pestañas abiertas, abrir una nueva
            if (clients.openWindow) {
                return clients.openWindow(clickUrl);
            }
        })
    );
});
