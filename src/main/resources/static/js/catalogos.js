/**
 * catalogos.js - Catálogos Generales (CRUD completo con AJAX)
 */

// ====================================================================
// TAB SYSTEM
// ====================================================================
function cambiarTab(tabName) {
    // Ocultar todos los paneles
    document.querySelectorAll('.tab-panel').forEach(p => {
        p.classList.add('hidden');
        p.classList.remove('animate-fade-slide');
    });

    // Desactivar todos los botones
    document.querySelectorAll('.tab-btn').forEach(b => {
        b.classList.remove('tab-active', 'border-gob-guinda', 'text-gob-guinda');
        b.classList.add('border-transparent');
    });

    // Mostrar panel activo
    const panel = document.getElementById('panel-' + tabName);
    if (panel) {
        panel.classList.remove('hidden');
        void panel.offsetWidth;
        panel.classList.add('animate-fade-slide');
    }

    // Activar botón
    const btn = document.querySelector(`.tab-btn[data-tab="${tabName}"]`);
    if (btn) {
        btn.classList.add('tab-active', 'border-gob-guinda', 'text-gob-guinda');
        btn.classList.remove('border-transparent');
    }

    // Guardar en URL
    const url = new URL(window.location);
    url.searchParams.set('tab', tabName);
    window.history.replaceState({}, '', url);

    // Si el tab estaba marcado como sucio (cambio mientras estaba oculto), refrescarlo ahora
    const tbodyId = `tbody-${tabName}`;
    if (dirtyTabs.has(tbodyId)) {
        dirtyTabs.delete(tbodyId);
        const tabla = Object.keys(TABLA_A_TBODY).find(k => TABLA_A_TBODY[k] === tbodyId);
        if (tabla) refrescarTablaCatalogo(tabla, true);
    }
}

// ====================================================================
// MODAL UNIVERSAL
// ====================================================================

/** Mapa tipo JS → tabla de BD (para saber qué tbody refrescar al guardar) */
const TIPO_A_TABLA = {
    'organo':           'organos_jurisdiccionales',
    'tipo-audiencia':   'tipos_audiencia',
    'tipo-expediente':  'cat_tipo_expediente',
    'sede':             'cat_estados',
    'materia':          'materias'
};

/** Tabs que recibieron un evento WS mientras estaban ocultos */
const dirtyTabs = new Set();

const TIPOS_CONFIG = {
    'organo': {
        titulo: 'Órgano Jurisdiccional',
        icono: 'fa-landmark',
        urlGuardar: '/catalogos/organos/guardar',
        urlEliminar: '/catalogos/organos/eliminar/',
        placeholder: 'Ej. Juzgado 1° de Distrito en Materia Civil'
    },
    'tipo-audiencia': {
        titulo: 'Tipo de Audiencia',
        icono: 'fa-gavel',
        urlGuardar: '/catalogos/tipos-audiencia/guardar',
        urlEliminar: '/catalogos/tipos-audiencia/eliminar/',
        placeholder: 'Ej. Conciliación y Arbitraje'
    },
    'tipo-expediente': {
        titulo: 'Tipo de Expediente',
        icono: 'fa-folder',
        urlGuardar: '/catalogos/tipos-expediente/guardar',
        urlEliminar: '/catalogos/tipos-expediente/eliminar/',
        placeholder: 'Ej. Ordinario Civil'
    },
    'sede': {
        titulo: 'Sede / Estado',
        icono: 'fa-map-marker-alt',
        urlGuardar: '/catalogos/sedes/guardar',
        urlEliminar: '/catalogos/sedes/eliminar/',
        placeholder: 'Ej. Ciudad de México'
    },
    'materia': {
        titulo: 'Materia',
        icono: 'fa-balance-scale',
        urlGuardar: '/catalogos/materias/guardar',
        urlEliminar: '/catalogos/materias/eliminar/',
        placeholder: 'Ej. Civil, Penal'
    }
};

function abrirModalNuevo(tipo) {
    const cfg = TIPOS_CONFIG[tipo];
    if (!cfg) return;

    document.getElementById('modal-titulo').innerHTML = `<i class="fas fa-plus-circle mr-2 text-gob-oro"></i> Nuevo ${cfg.titulo}`;
    document.getElementById('modal-icono').className = 'fas ' + cfg.icono;
    document.getElementById('modal-nombre').placeholder = cfg.placeholder;
    document.getElementById('modal-nombre').value = '';
    document.getElementById('modal-item-id').value = '';
    document.getElementById('modal-tipo').value = tipo;

    const modal = document.getElementById('modal-catalogo');
    modal.classList.remove('hidden');
    modal.classList.add('flex');

    setTimeout(() => document.getElementById('modal-nombre').focus(), 200);
}

function editarItem(tipo, btn) {
    const cfg = TIPOS_CONFIG[tipo];
    if (!cfg) return;

    const id = btn.dataset.id;
    const nombre = btn.dataset.nombre;

    document.getElementById('modal-titulo').innerHTML = `<i class="fas fa-edit mr-2 text-gob-oro"></i> Editar ${cfg.titulo}`;
    document.getElementById('modal-icono').className = 'fas ' + cfg.icono;
    document.getElementById('modal-nombre').placeholder = cfg.placeholder;
    document.getElementById('modal-nombre').value = nombre;
    document.getElementById('modal-item-id').value = id;
    document.getElementById('modal-tipo').value = tipo;

    const modal = document.getElementById('modal-catalogo');
    modal.classList.remove('hidden');
    modal.classList.add('flex');

    setTimeout(() => document.getElementById('modal-nombre').focus(), 200);
}

function cerrarModal() {
    const modal = document.getElementById('modal-catalogo');
    modal.classList.add('hidden');
    modal.classList.remove('flex');
}

function guardarItem() {
    const tipo = document.getElementById('modal-tipo').value;
    const cfg = TIPOS_CONFIG[tipo];
    if (!cfg) return;

    const nombre = document.getElementById('modal-nombre').value.trim();
    const id = document.getElementById('modal-item-id').value;
    const gerenciaId = document.getElementById('gerencia-id-actual')?.value || '';

    if (!nombre) {
        Swal.fire({ icon: 'warning', title: 'Campo vacío', text: 'El nombre es obligatorio.', confirmButtonColor: '#9D2449' });
        return;
    }

    const body = { nombre: nombre, gerenciaId: gerenciaId };
    if (id) body.id = id;

    fetch(cfg.urlGuardar, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
    })
    .then(res => res.json())
    .then(data => {
        if (data.status === 'ok') {
            Swal.fire({
                icon: 'success',
                title: id ? 'Actualizado' : 'Creado',
                text: data.mensaje,
                confirmButtonColor: '#9D2449',
                timer: 1500,
                showConfirmButton: false
            });
            cerrarModal();
            // Refrescar la tabla vía AJAX en lugar de recargar toda la página
            const tablaAfectada = TIPO_A_TABLA[tipo];
            if (tablaAfectada) {
                setTimeout(() => refrescarTablaCatalogo(tablaAfectada, true), 400);
            } else {
                setTimeout(() => location.reload(), 800);
            }
        } else {
            Swal.fire({ icon: 'error', title: 'Error', text: data.error || 'Error desconocido.', confirmButtonColor: '#9D2449' });
        }
    })
    .catch(err => {
        Swal.fire({ icon: 'error', title: 'Error de conexión', text: err.message, confirmButtonColor: '#9D2449' });
    });
}

// ====================================================================
// ELIMINAR
// ====================================================================
function eliminarItem(tipo, btn) {
    const cfg = TIPOS_CONFIG[tipo];
    if (!cfg) return;

    const id = btn.dataset.id;
    const nombre = btn.dataset.nombre;

    Swal.fire({
        title: '¿Eliminar este registro?',
        html: `<p class="text-sm text-gray-600">Se eliminará <strong>"${nombre}"</strong> de todo el sistema.<br>Esta acción no se puede deshacer.</p>`,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#DC2626',
        cancelButtonColor: '#6B7280',
        confirmButtonText: '<i class="fas fa-trash-alt mr-1"></i> Sí, eliminar',
        cancelButtonText: 'Cancelar'
    }).then(result => {
        if (result.isConfirmed) {
            fetch(cfg.urlEliminar + id, { method: 'DELETE' })
                .then(res => res.json())
                .then(data => {
                    if (data.status === 'ok') {
                        Swal.fire({
                            icon: 'success',
                            title: 'Eliminado',
                            text: data.mensaje,
                            confirmButtonColor: '#9D2449',
                            timer: 1500,
                            showConfirmButton: false
                        });
                        // Remover la fila visualmente y actualizar badge
                        const row = btn.closest('tr');
                        const tbodyEl = btn.closest('tbody');
                        if (row) {
                            row.style.transition = 'all 0.3s ease';
                            row.style.opacity = '0';
                            row.style.transform = 'translateX(20px)';
                            setTimeout(() => {
                                row.remove();
                                if (tbodyEl) actualizarBadgeTab(tbodyEl.id);
                            }, 300);
                        }
                    } else {
                        Swal.fire({ icon: 'error', title: 'Error', text: data.error || 'No se pudo eliminar.', confirmButtonColor: '#9D2449' });
                    }
                })
                .catch(err => {
                    Swal.fire({ icon: 'error', title: 'Error', text: 'No se pudo conectar con el servidor.', confirmButtonColor: '#9D2449' });
                });
        }
    });
}

// ====================================================================
// BUSCADORES
// ====================================================================
function setupSearch(inputId, tbodyId) {
    const input = document.getElementById(inputId);
    if (!input) return;
    input.addEventListener('keyup', function () {
        const filter = this.value.toLowerCase();
        const rows = document.getElementById(tbodyId)?.getElementsByTagName('tr') || [];
        for (let r of rows) {
            r.style.display = r.textContent.toLowerCase().indexOf(filter) > -1 ? '' : 'none';
        }
    });
}

// ====================================================================
// INICIALIZACIÓN
// ====================================================================
document.addEventListener('DOMContentLoaded', () => {
    // Inicializar tabs
    const params = new URLSearchParams(window.location.search);
    cambiarTab(params.get('tab') || 'organos');

    // Listeners para tabs
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.addEventListener('click', () => cambiarTab(btn.dataset.tab));
    });

    // Buscadores
    setupSearch('search-organos', 'tbody-organos');
    setupSearch('search-tipos-audiencia', 'tbody-tipos-audiencia');
    setupSearch('search-tipos-expediente', 'tbody-tipos-expediente');
    setupSearch('search-abogados', 'tbody-abogados');
    setupSearch('search-sedes', 'tbody-sedes');
    setupSearch('search-materias', 'tbody-materias');

    // Cerrar modal con Escape
    document.addEventListener('keydown', e => {
        if (e.key === 'Escape') cerrarModal();
    });

    // Cerrar modal clic fuera
    document.getElementById('modal-catalogo')?.addEventListener('click', e => {
        if (e.target === e.currentTarget) cerrarModal();
    });

    // Enter en modal guarda
    document.getElementById('modal-nombre')?.addEventListener('keydown', e => {
        if (e.key === 'Enter') {
            e.preventDefault();
            guardarItem();
        }
    });

    // Iniciar WebSocket en tiempo real
    initWebSocketCatalogos();
});

// ====================================================================
// WEBSOCKET: ACTUALIZACIÓN EN TIEMPO REAL PARA CATÁLOGOS
// ====================================================================

/** Mapa: tabla de BD → id del tbody en el HTML */
const TABLA_A_TBODY = {
    'organos_jurisdiccionales': 'tbody-organos',
    'tipos_audiencia':          'tbody-tipos-audiencia',
    'cat_tipo_expediente':      'tbody-tipos-expediente',
    'materias':                 'tbody-materias',
    'cat_estados':              'tbody-sedes'
};

/**
 * Actualiza el badge de conteo en el botón del tab.
 * Cuenta las filas reales (sin colspan = sin la fila de estado vacío).
 */
function actualizarBadgeTab(tbodyId) {
    const tbody = document.getElementById(tbodyId);
    if (!tbody) return;
    const dataRows = Array.from(tbody.querySelectorAll('tr')).filter(r => !r.querySelector('td[colspan]'));
    const tabName = tbodyId.replace('tbody-', '');
    const tabBtn = document.querySelector(`.tab-btn[data-tab="${tabName}"]`);
    if (!tabBtn) return;
    const badge = tabBtn.querySelector('span');
    if (badge) badge.textContent = dataRows.length;
}

/**
 * Hace fetch de la página actual y reemplaza el tbody del catálogo afectado.
 * Siempre actualiza el tbody y el badge, incluso si el panel está oculto.
 * @param {string} tabla   - Nombre de la tabla de BD.
 * @param {boolean} force  - Parámetro mantenido por compatibilidad (ya no limita la ejecución).
 */
async function refrescarTablaCatalogo(tabla, force = false) {
    const tbodyId = TABLA_A_TBODY[tabla];
    if (!tbodyId) return;

    const tbodyActual = document.getElementById(tbodyId);
    if (!tbodyActual) return;

    try {
        const res = await fetch(window.location.href, {
            headers: { 'X-Requested-With': 'XMLHttpRequest' }
        });
        if (!res.ok) return;
        const html = await res.text();
        const doc = new DOMParser().parseFromString(html, 'text/html');
        const tbodyNuevo = doc.getElementById(tbodyId);
        if (tbodyNuevo) {
            tbodyActual.innerHTML = tbodyNuevo.innerHTML;
            // Actualizar buscador y badge de conteo (siempre, incluso si el panel está oculto)
            setupSearch(tbodyId.replace('tbody-', 'search-'), tbodyId);
            actualizarBadgeTab(tbodyId);
            // Limpiar el flag dirty: ya está actualizado, no hace falta un segundo fetch al abrir el tab
            dirtyTabs.delete(tbodyId);
            console.log(`[Catálogos] Tabla '${tbodyId}' actualizada en tiempo real.`);
        }
    } catch (e) {
        console.error('[Catálogos] Error refrescando tabla:', e);
        // Si falla el fetch, marcar como sucio para reintentar al cambiar de tab
        dirtyTabs.add(tbodyId);
    }
}

function initWebSocketCatalogos() {
    const intentarConectar = () => {
        if (typeof SockJS === 'undefined' || typeof Stomp === 'undefined') {
            setTimeout(intentarConectar, 1000);
            return;
        }

        const socket = new SockJS('/ws-realtime');
        const stompClient = Stomp.over(socket);
        stompClient.debug = null;

        stompClient.connect({}, (frame) => {
            console.log('[Catálogos] Conectado a WebSockets en tiempo real.');

            stompClient.subscribe('/topic/catalogos', (message) => {
                try {
                    const payload = JSON.parse(message.body);
                    console.log(`[Catálogos] Cambio detectado en '${payload.tabla}' (${payload.operacion})`);
                    refrescarTablaCatalogo(payload.tabla);
                } catch (e) {
                    console.error('[Catálogos] Error parseando mensaje WS:', e);
                }
            });
        }, (error) => {
            console.warn('[Catálogos] Conexión WS perdida. Reconectando en 5s...');
            setTimeout(intentarConectar, 5000);
        });
    };

    // Retrasar ligeramente para no competir con el WS del Navbar
    setTimeout(intentarConectar, 2500);
}