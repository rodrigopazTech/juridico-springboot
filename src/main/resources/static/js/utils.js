/**
 * Utilidades globales del sistema.
 * Autor: Ricardo Villalobos
 */

/**
 * Elimina los parámetros vacíos de la URL al cargar la página,
 * sin provocar una recarga. Ej: /terminos?keyword=&estatus=PROYECTISTA
 * queda como: /terminos?estatus=PROYECTISTA
 */
(function limpiarParamsVaciosUrl() {
    const url = new URL(window.location.href);
    let cambio = false;
    for (const [key, value] of [...url.searchParams.entries()]) {
        if (!value) {
            url.searchParams.delete(key);
            cambio = true;
        }
    }
    if (cambio) {
        window.history.replaceState({}, '', url.toString());
    }
})();

/**
 * Configuración visual por tipo de notificación.
 * Define canal, nombre, importancia, color de LED y emoji.
 */
const NOTIF_CONFIG = {
    TERMINO:     { canal: 'juridico-terminos',    nombre: 'Términos Procesales', importancia: 5, color: '#ef4444', emoji: '⏳' },
    AUDIENCIA:   { canal: 'juridico-audiencias',  nombre: 'Audiencias',          importancia: 5, color: '#3b82f6', emoji: '⚖️' },
    EXPEDIENTE:  { canal: 'juridico-expedientes', nombre: 'Expedientes',         importancia: 3, color: '#22c55e', emoji: '📁' },
    ASIGNACION:  { canal: 'juridico-asignacion',  nombre: 'Asignaciones',        importancia: 5, color: '#a855f7', emoji: '👤' },
    OBSERVACION: { canal: 'juridico-obs',         nombre: 'Observaciones',       importancia: 3, color: '#f59e0b', emoji: '💬' },
    SISTEMA:     { canal: 'juridico-sistema',     nombre: 'Sistema Jurídico',    importancia: 3, color: '#691228', emoji: '🔔' }
};

/**
 * Inicializa los canales de notificación y los tipos de acción en Android.
 * Se ejecuta una vez al cargar la página (DOMContentLoaded).
 */
async function inicializarCapacitorNotificaciones() {
    if (!window.Capacitor || !window.Capacitor.isNativePlatform()) return;

    try {
        const { LocalNotifications } = window.Capacitor.Plugins;

        // Registrar botones de acción
        await LocalNotifications.registerActionTypes({
            types: [{
                id: 'JURIDICO_ACTIONS',
                actions: [
                    { id: 'ver',     title: '👁️ Ver detalles' },
                    { id: 'ignorar', title: '✕ Ignorar', destructive: true }
                ]
            }]
        });

        // Crear un canal por cada tipo de notificación
        for (const [, cfg] of Object.entries(NOTIF_CONFIG)) {
            await LocalNotifications.createChannel({
                id:          cfg.canal,
                name:        cfg.emoji + ' ' + cfg.nombre,
                description: 'Notificaciones de ' + cfg.nombre,
                importance:  cfg.importancia,
                visibility:  1,       // PUBLIC
                vibration:   true,
                lights:      true,
                lightColor:  cfg.color
            });
        }

        // Listener: al tocar "Ver detalles" navega a la notificación
        LocalNotifications.addListener('localNotificationActionPerformed', function (event) {
            if (event.actionId === 'ver') {
                const url = (event.notification.extra && event.notification.extra.notifUrl)
                    ? event.notification.extra.notifUrl
                    : '/notificaciones';
                if (window.location) window.location.href = url;
            }
            // 'ignorar' solo cierra la notificación, no hace nada más
        });

    } catch (e) {
        console.warn('[Capacitor] Error al inicializar canales:', e);
    }
}

// Inicializar cuando el DOM esté listo
document.addEventListener('DOMContentLoaded', inicializarCapacitorNotificaciones);

/**
 * Lanza una notificación nativa Android enriquecida si la app corre en Capacitor.
 * En el navegador no hace nada (los toasts del navbar se encargan).
 * @param {Object} notif - Objeto de notificación con { id, titulo, mensaje, tipo, prioridad }
 */
window.mostrarNotificacionNativa = async function (notif) {
    if (!window.Capacitor || !window.Capacitor.isNativePlatform()) return;

    try {
        const { LocalNotifications } = window.Capacitor.Plugins;

        // Solicitar permiso si aún no se ha concedido
        const permiso = await LocalNotifications.requestPermissions();
        if (permiso.display !== 'granted') return;

        const tipo   = (notif.tipo || 'SISTEMA').toUpperCase();
        const cfg    = NOTIF_CONFIG[tipo] || NOTIF_CONFIG.SISTEMA;
        const notifId = parseInt(notif.id) || Math.floor(Math.random() * 9999999);

        // Sufijo visual de prioridad en el título
        const prioEmoji = notif.prioridad === 'ALTA'  ? ' 🔴'
                        : notif.prioridad === 'MEDIA' ? ' 🟡'
                        : '';

        await LocalNotifications.schedule({
            notifications: [{
                title:        cfg.emoji + ' ' + (notif.titulo || 'Sistema Jurídico') + prioEmoji,
                body:         notif.mensaje || '',
                id:           notifId,
                channelId:    cfg.canal,
                smallIcon:    'ic_notification',
                iconColor:    cfg.color,
                actionTypeId: 'JURIDICO_ACTIONS',
                autoCancel:   true,
                ongoing:      false,
                extra: {
                    notifUrl: '/notificaciones/leer/' + notif.id
                }
            }]
        });

    } catch (e) {
        console.warn('[Capacitor] Error al mostrar notificación nativa:', e);
    }
};

/**
 * Valida un archivo seleccionado por el usuario del lado del cliente.
 * Muestra una alerta SweetAlert2 si el formato o tamaño de 1GB no son válidos.
 * 
 * @param {File} file - El archivo a validar.
 * @returns {boolean} - true si es válido, false en caso contrario.
 */
window.validarArchivoCliente = function(file) {
    if (!file) return true;

    // Límite de 1GB en bytes (1024 * 1024 * 1024)
    const maxSizeBytes = 1024 * 1024 * 1024;
    if (file.size > maxSizeBytes) {
        if (typeof Swal !== 'undefined') {
            Swal.fire({
                icon: 'error',
                title: 'Archivo demasiado grande',
                text: `El archivo "${file.name}" excede el límite máximo permitido de 1GB.`,
                confirmButtonColor: '#691228'
            });
        } else {
            alert(`El archivo "${file.name}" excede el límite máximo permitido de 1GB.`);
        }
        return false;
    }

    const nombreOriginal = file.name;
    if (!nombreOriginal || !nombreOriginal.includes('.')) {
        if (typeof Swal !== 'undefined') {
            Swal.fire({
                icon: 'error',
                title: 'Archivo inválido',
                text: `El archivo "${file.name}" no tiene una extensión válida.`,
                confirmButtonColor: '#691228'
            });
        } else {
            alert(`El archivo "${file.name}" no tiene una extensión válida.`);
        }
        return false;
    }

    const extension = nombreOriginal.split('.').pop().toLowerCase();
    const extensionesPermitidas = [
        // PDF
        "pdf",
        // Word
        "doc", "docx", "docm",
        // Excel
        "xls", "xlsx", "xlsm", "xlsb",
        // PowerPoint
        "ppt", "pptx",
        // Imágenes
        "jpg", "jpeg", "png", "gif", "bmp", "webp", "tiff", "svg",
        // Videos
        "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "mpeg", "3gp",
        // Audio
        "mp3", "wav", "ogg", "m4a", "aac", "flac", "wma"
    ];

    if (!extensionesPermitidas.includes(extension)) {
        if (typeof Swal !== 'undefined') {
            Swal.fire({
                icon: 'error',
                title: 'Formato no permitido',
                html: `El formato del archivo <b>.${extension}</b> no está permitido.<br><br>Solo se admiten archivos PDF, Office (Word, Excel, PowerPoint), imágenes, videos y audio.`,
                confirmButtonColor: '#691228'
            });
        } else {
            alert(`El formato del archivo .${extension} no está permitido. Solo se admiten archivos PDF, Office, imágenes, videos y audio.`);
        }
        return false;
    }

    return true;
};

// Escuchador global para validar cualquier cambio de input tipo archivo en el DOM
document.addEventListener('change', function(event) {
    if (event.target && event.target.type === 'file') {
        const files = event.target.files;
        if (files && files.length > 0) {
            for (let i = 0; i < files.length; i++) {
                if (!window.validarArchivoCliente(files[i])) {
                    event.target.value = ''; // Resetear el input para impedir subida/envío
                    break;
                }
            }
        }
    }
});
