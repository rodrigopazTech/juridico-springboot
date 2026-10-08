// Avisos del navegador. La preferencia de la cuenta y el permiso del navegador
// son estados distintos: la primera se guarda en el servidor y el segundo solo
// puede consultarse o cambiarse desde el navegador de cada dispositivo.
(function () {
    const RETRY_AFTER_MS = 7 * 24 * 60 * 60 * 1000;
    let serviceWorkerRegistration = null;
    let vapidPublicKey = null;
    let appEnabled = true;

    function browserSupport() {
        return typeof window !== 'undefined' && 'Notification' in window
            && 'serviceWorker' in navigator && 'PushManager' in window;
    }

    function browserPermission() {
        return browserSupport() ? Notification.permission : 'unsupported';
    }

    function resolveNotificationState(preferenceEnabled, permission, subscribed) {
        if (permission === 'unsupported') return { key: 'unsupported', message: 'Tu navegador no admite avisos en tiempo real.', help: 'Puedes consultar tus avisos desde la campana de la aplicación.' };
        if (!preferenceEnabled) return { key: 'disabled-in-app', message: 'Los avisos están desactivados en tu cuenta.', help: 'Activa esta opción para recibirlos en este navegador.' };
        if (permission === 'denied') return { key: 'blocked-in-browser', message: 'Los avisos están bloqueados en tu navegador.', help: 'Actívalos desde la configuración del sitio y vuelve a esta página.' };
        if (permission === 'default') return { key: 'permission-not-requested', message: 'Aún no has activado los avisos en este navegador.', help: 'Activa esta opción y el navegador te pedirá autorización.' };
        if (permission === 'granted' && subscribed) return { key: 'enabled', message: 'Los avisos están activados en este navegador.', help: 'Te avisaremos sobre cambios importantes mientras esta cuenta esté activa.' };
        return { key: 'ready-to-connect', message: 'El navegador permite los avisos. Estamos terminando de activarlos.', help: 'Si este mensaje no cambia, intenta desactivar y activar la opción otra vez.' };
    }

    function getCsrfHeaders() {
        const csrfToken = document.querySelector('meta[name="_csrf"]');
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]');
        const headers = { 'Content-Type': 'application/json' };
        if (csrfToken && csrfHeader) headers[csrfHeader.content] = csrfToken.content;
        return headers;
    }

    async function loadAppPreference() {
        try {
            const response = await fetch('/api/webpush/preference');
            if (!response.ok) return true;
            const data = await response.json();
            return data.appEnabled !== false;
        } catch (_) { return true; }
    }

    async function saveAppPreference(enabled) {
        const response = await fetch('/api/webpush/preference', {
            method: 'POST', headers: getCsrfHeaders(), body: JSON.stringify({ appEnabled: enabled })
        });
        if (!response.ok) throw new Error('No fue posible guardar la preferencia.');
        const data = await response.json();
        appEnabled = data.appEnabled === true;
        return appEnabled;
    }

    async function preloadVapidPublicKey() {
        try {
            const response = await fetch('/api/webpush/public-key');
            if (!response.ok) return null;
            vapidPublicKey = (await response.json()).publicKey;
            return vapidPublicKey;
        } catch (_) { return null; }
    }

    async function registerServiceWorkerOnly() {
        if (!browserSupport()) return null;
        try {
            serviceWorkerRegistration = await navigator.serviceWorker.register('/sw.js');
            return serviceWorkerRegistration;
        } catch (_) { return null; }
    }

    function urlBase64ToUint8Array(base64String) {
        const padding = '='.repeat((4 - base64String.length % 4) % 4);
        const rawData = window.atob((base64String + padding).replace(/-/g, '+').replace(/_/g, '/'));
        return Uint8Array.from(rawData, (character) => character.charCodeAt(0));
    }

    function getOrCreateDeviceId() {
        let deviceId = localStorage.getItem('webpush_device_id');
        if (!deviceId) {
            deviceId = `device-${crypto.getRandomValues(new Uint32Array(2)).join('-')}`;
            localStorage.setItem('webpush_device_id', deviceId);
        }
        return deviceId;
    }

    async function sendSubscriptionToBackend(subscription) {
        const keys = subscription.toJSON().keys;
        const response = await fetch('/api/webpush/subscribe', {
            method: 'POST', headers: getCsrfHeaders(),
            body: JSON.stringify({ endpoint: subscription.endpoint, deviceId: getOrCreateDeviceId(), keys: { p256dh: keys.p256dh, auth: keys.auth } })
        });
        return response.ok;
    }

    async function subscribeUser() {
        if (!browserSupport() || browserPermission() !== 'granted') return false;
        try {
            const registration = serviceWorkerRegistration || await navigator.serviceWorker.ready;
            if (!registration) return false;
            let subscription = await registration.pushManager.getSubscription();
            if (!subscription) {
                const key = vapidPublicKey || await preloadVapidPublicKey();
                if (!key) return false;
                subscription = await registration.pushManager.subscribe({ userVisibleOnly: true, applicationServerKey: urlBase64ToUint8Array(key) });
            }
            return sendSubscriptionToBackend(subscription);
        } catch (_) { return false; }
    }

    async function unsubscribeCurrentDevice() {
        if (!browserSupport()) return;
        try {
            const registration = serviceWorkerRegistration || await navigator.serviceWorker.getRegistration();
            const subscription = registration && await registration.pushManager.getSubscription();
            if (!subscription) return;
            await fetch('/api/webpush/unsubscribe', { method: 'POST', headers: getCsrfHeaders(), body: JSON.stringify({ endpoint: subscription.endpoint }) });
            await subscription.unsubscribe();
        } catch (_) { /* La cuenta ya quedó desactivada aunque no se limpie este dispositivo. */ }
    }

    async function hasSubscription() {
        if (!browserSupport() || browserPermission() !== 'granted') return false;
        try {
            const registration = serviceWorkerRegistration || await navigator.serviceWorker.ready;
            return Boolean(await registration.pushManager.getSubscription());
        } catch (_) { return false; }
    }

    async function refreshSettingsUi() {
        const container = document.getElementById('webpush-settings');
        if (!container) return;
        const toggle = document.getElementById('webpush-enabled');
        const status = document.getElementById('webpush-status');
        const help = document.getElementById('webpush-help');
        const state = resolveNotificationState(appEnabled, browserPermission(), await hasSubscription());
        toggle.checked = appEnabled;
        toggle.disabled = state.key === 'unsupported';
        status.textContent = state.message;
        help.textContent = state.help;
        help.classList.toggle('hidden', !state.help);
    }

    function promptStorageKey() {
        const wrapper = document.getElementById('notif-bell-wrapper');
        const userId = wrapper && wrapper.dataset.webpushUserId;
        return `webpush_prompt_snooze_until_${userId || 'browser'}`;
    }

    function shouldShowPrompt(preferenceEnabled, permission, now, snoozeUntil) {
        return preferenceEnabled && permission === 'default' && Number(snoozeUntil || 0) <= now;
    }

    function postponePrompt() { localStorage.setItem(promptStorageKey(), String(Date.now() + RETRY_AFTER_MS)); }

    function dismissPopover(popover) {
        popover.classList.remove('opacity-100', 'translate-y-0');
        popover.classList.add('opacity-0', '-translate-y-1');
        popover.addEventListener('transitionend', () => popover.remove(), { once: true });
    }

    function showPushPopover() {
        if (!shouldShowPrompt(appEnabled, browserPermission(), Date.now(), localStorage.getItem(promptStorageKey()))) return;
        const wrapper = document.getElementById('notif-bell-wrapper');
        if (!wrapper || document.getElementById('webpush-popover')) return;
        const popover = document.createElement('section');
        popover.id = 'webpush-popover';
        popover.setAttribute('role', 'dialog');
        popover.setAttribute('aria-labelledby', 'webpush-popover-title');
        popover.className = 'absolute top-[60px] right-0 z-[60] w-72 rounded-2xl border border-gray-100 bg-white p-4 shadow-2xl transition duration-200 -translate-y-1 opacity-0';
        popover.innerHTML = `
            <div class="absolute -top-1.5 right-4 w-3 h-3 rotate-45 bg-white border-t border-l border-gray-100" aria-hidden="true"></div>
            <button id="webpush-popover-close" type="button" class="absolute right-2 top-2 rounded p-1 text-gray-400 hover:text-gray-700 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gob-guinda" aria-label="Posponer avisos durante siete días"><i class="fas fa-times" aria-hidden="true"></i></button>
            <div class="flex items-start gap-2.5 pr-4"><div class="flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-full bg-gob-oro/10 text-gob-oro"><i class="fas fa-bell" aria-hidden="true"></i></div><div><h2 id="webpush-popover-title" class="text-sm font-bold text-gray-800">Recibe avisos importantes</h2><p class="mt-1 text-xs leading-normal text-gray-600">Te avisaremos sobre audiencias, términos y cambios en tus expedientes.</p></div></div>
            <div class="mt-3 flex justify-end gap-2 border-t border-gray-100 pt-2"><button id="webpush-popover-dismiss" type="button" class="rounded px-2.5 py-1 text-xs font-bold text-gray-500 hover:text-gray-800 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gob-guinda">Ahora no</button><button id="webpush-popover-activate" type="button" class="rounded-lg bg-gob-guinda px-3.5 py-1 text-xs font-bold text-white shadow-sm hover:bg-red-800 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gob-guinda">Activar avisos</button></div>`;
        wrapper.appendChild(popover);
        requestAnimationFrame(() => { popover.classList.replace('opacity-0', 'opacity-100'); popover.classList.replace('-translate-y-1', 'translate-y-0'); });
        const postpone = () => { postponePrompt(); dismissPopover(popover); };
        popover.querySelector('#webpush-popover-dismiss').addEventListener('click', postpone);
        popover.querySelector('#webpush-popover-close').addEventListener('click', postpone);
        popover.querySelector('#webpush-popover-activate').addEventListener('click', async () => {
            const button = popover.querySelector('#webpush-popover-activate');
            button.disabled = true; button.textContent = 'Activando…';
            await enableNotifications(); dismissPopover(popover);
        });
        const bell = document.getElementById('notif-bell-btn');
        if (bell) bell.addEventListener('click', () => dismissPopover(popover), { once: true });
    }

    async function enableNotifications() {
        try {
            await saveAppPreference(true);
            if (browserPermission() === 'default') await Notification.requestPermission();
            if (browserPermission() === 'granted') await subscribeUser();
        } finally { await refreshSettingsUi(); }
    }

    function bindSettings() {
        const toggle = document.getElementById('webpush-enabled');
        if (!toggle) return;
        toggle.addEventListener('change', async () => {
            const intendedValue = toggle.checked;
            toggle.disabled = true;
            try {
                if (intendedValue) await enableNotifications();
                else { await saveAppPreference(false); await unsubscribeCurrentDevice(); await refreshSettingsUi(); }
            } catch (_) { await refreshSettingsUi(); }
            finally { toggle.disabled = browserPermission() === 'unsupported'; }
        });
        refreshSettingsUi();
    }

    async function unsubscribeUserBeforeLogout(event, logoutLink) {
        if (!browserSupport()) return;
        event.preventDefault();
        await Promise.race([unsubscribeCurrentDevice(), new Promise((resolve) => setTimeout(resolve, 3000))]);
        window.location.href = logoutLink.getAttribute('href') || '/logout';
    }

    const api = { resolveNotificationState, shouldShowPrompt, RETRY_AFTER_MS };
    if (typeof window !== 'undefined') window.WebPushClient = api;
    if (typeof module !== 'undefined' && module.exports) module.exports = api;

    if (typeof document !== 'undefined') document.addEventListener('DOMContentLoaded', async () => {
        appEnabled = await loadAppPreference();
        bindSettings();
        if (!browserSupport()) return;
        await Promise.all([preloadVapidPublicKey(), registerServiceWorkerOnly()]);
        if (appEnabled && browserPermission() === 'granted') { await subscribeUser(); await refreshSettingsUi(); }
        else if (shouldShowPrompt(appEnabled, browserPermission(), Date.now(), localStorage.getItem(promptStorageKey()))) setTimeout(showPushPopover, 1500);
        document.addEventListener('click', (event) => {
            const logoutLink = event.target.closest('a[href*="/logout"]');
            if (logoutLink) unsubscribeUserBeforeLogout(event, logoutLink);
        });
    });
})();
