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
    'materia':          'materias',
    'etapa':            'cat_etapas_procesales'
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

function filtrarParentsEtapa() {
    const materiaId = document.getElementById('etapa-materia-id')?.value || '';
    const etapaId = document.getElementById('etapa-id')?.value || '';
    const parentSelect = document.getElementById('etapa-parent-id');
    if (!parentSelect) return;
    Array.from(parentSelect.options).forEach(option => {
        if (!option.value) {
            option.hidden = false;
            return;
        }
        option.hidden = option.dataset.materiaId !== materiaId || option.value === etapaId;
    });
    if (parentSelect.selectedOptions[0]?.hidden) {
        parentSelect.value = '';
    }
    sugerirOrdenEtapa();
    actualizarPreviewOrdenEtapa();
}

function getEtapaRows() {
    return Array.from(document.querySelectorAll('#tbody-etapas tr[data-id]'));
}

function aplicarFiltrosEtapas() {
    const materiaId = document.getElementById('filtro-etapas-materia')?.value || '';
    const texto = (document.getElementById('search-etapas')?.value || '').toLowerCase();
    let visibles = 0;

    getEtapaRows().forEach(row => {
        const coincideMateria = !materiaId || row.dataset.materiaId === materiaId;
        const coincideTexto = !texto || row.textContent.toLowerCase().includes(texto);
        const visible = coincideMateria && coincideTexto;
        row.style.display = visible ? '' : 'none';
        if (visible) visibles++;
    });

    const emptyRow = document.getElementById('empty-etapas-filter');
    if (emptyRow) {
        const hayDatos = getEtapaRows().length > 0;
        const mostrarEstadoVacio = hayDatos && visibles === 0;
        emptyRow.classList.toggle('hidden', !mostrarEstadoVacio);
    }
}

function limpiarFiltrosEtapas() {
    const materia = document.getElementById('filtro-etapas-materia');
    const busqueda = document.getElementById('search-etapas');
    if (materia) materia.value = '';
    if (busqueda) busqueda.value = '';
    aplicarFiltrosEtapas();
}

function etapasDelNivelActual() {
    const materiaId = document.getElementById('etapa-materia-id')?.value || '';
    const parentId = document.getElementById('etapa-parent-id')?.value || '';
    const etapaId = document.getElementById('etapa-id')?.value || '';
    if (!materiaId) return [];

    return getEtapaRows()
        .filter(row => row.dataset.id !== etapaId)
        .filter(row => row.dataset.materiaId === materiaId)
        .filter(row => (row.dataset.parentId || '') === parentId)
        .map(row => ({
            nombre: row.dataset.nombre || row.textContent.trim(),
            orden: parseInt(row.dataset.orden || '1', 10)
        }))
        .sort((a, b) => a.orden - b.orden || a.nombre.localeCompare(b.nombre));
}

function escapeHtml(value) {
    return String(value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

function sugerirOrdenEtapa() {
    const ordenInput = document.getElementById('etapa-orden');
    if (!ordenInput || ordenInput.dataset.manual === 'true') return;
    const etapas = etapasDelNivelActual();
    const siguiente = etapas.length ? Math.max(...etapas.map(e => e.orden || 1)) + 1 : 1;
    ordenInput.value = String(siguiente);
}

function actualizarPreviewOrdenEtapa() {
    const preview = document.getElementById('etapa-preview-orden');
    if (!preview) return;

    const materiaSelect = document.getElementById('etapa-materia-id');
    const parentSelect = document.getElementById('etapa-parent-id');
    const nombre = document.getElementById('etapa-nombre')?.value.trim() || 'Nueva etapa';
    const orden = Math.max(parseInt(document.getElementById('etapa-orden')?.value || '1', 10), 1);
    const materia = materiaSelect?.selectedOptions[0]?.textContent?.trim() || '';
    const parent = parentSelect?.value ? parentSelect.selectedOptions[0]?.textContent?.trim() : '';

    if (!materiaSelect?.value) {
        preview.innerHTML = '<span class="text-gray-400 italic">Selecciona materia y captura el nombre para ver la posicion.</span>';
        return;
    }

    const nivel = parent ? `Subetapas de ${parent.replace(`${materia} / `, '')}` : `Etapas principales de ${materia}`;
    const items = [...etapasDelNivelActual(), { nombre, orden, nueva: true }]
        .sort((a, b) => a.orden - b.orden || (b.nueva ? 1 : 0));

    preview.innerHTML = `
        <div class="text-[10px] font-bold text-gray-500 uppercase tracking-wide mb-2">${escapeHtml(nivel)}</div>
        <ol class="space-y-1">
            ${items.map(item => `
                <li class="flex items-center gap-2 ${item.nueva ? 'font-bold text-gob-guinda' : 'text-gray-600'}">
                    <i class="fas fa-code-branch text-[10px] text-gray-400"></i>
                    <span class="inline-flex items-center justify-center min-w-7 px-1.5 h-5 rounded border border-gray-200 bg-gray-50 text-[11px]">${item.orden}</span>
                    <span>${escapeHtml(item.nombre)}</span>
                    ${item.nueva ? '<span class="text-[10px] bg-gob-guinda text-white px-1.5 py-0.5 rounded">nuevo</span>' : ''}
                </li>
            `).join('')}
        </ol>
    `;
}

function abrirModalEtapa() {
    document.getElementById('modal-etapa-titulo').textContent = 'Nueva etapa procesal';
    document.getElementById('etapa-id').value = '';
    document.getElementById('etapa-materia-id').value = document.getElementById('filtro-etapas-materia')?.value || '';
    document.getElementById('etapa-parent-id').value = '';
    document.getElementById('etapa-nombre').value = '';
    document.getElementById('etapa-clave').value = '';
    document.getElementById('etapa-orden').dataset.manual = 'false';
    document.getElementById('etapa-cierra').checked = false;
    document.getElementById('etapa-activo').checked = true;
    filtrarParentsEtapa();
    const modal = document.getElementById('modal-etapa');
    modal.classList.remove('hidden');
    modal.classList.add('flex');
    setTimeout(() => document.getElementById('etapa-materia-id')?.focus(), 150);
}

function editarEtapa(btn) {
    document.getElementById('modal-etapa-titulo').textContent = 'Editar etapa procesal';
    document.getElementById('etapa-id').value = btn.dataset.id || '';
    document.getElementById('etapa-materia-id').value = btn.dataset.materiaId || '';
    filtrarParentsEtapa();
    document.getElementById('etapa-parent-id').value = btn.dataset.parentId || '';
    document.getElementById('etapa-nombre').value = btn.dataset.nombre || '';
    document.getElementById('etapa-clave').value = btn.dataset.clave || '';
    document.getElementById('etapa-orden').value = btn.dataset.orden || '1';
    document.getElementById('etapa-orden').dataset.manual = 'true';
    document.getElementById('etapa-cierra').checked = btn.dataset.cierra === 'true';
    document.getElementById('etapa-activo').checked = btn.dataset.activo !== 'false';
    actualizarPreviewOrdenEtapa();
    const modal = document.getElementById('modal-etapa');
    modal.classList.remove('hidden');
    modal.classList.add('flex');
    setTimeout(() => document.getElementById('etapa-nombre')?.focus(), 150);
}

function cerrarModalEtapa() {
    const modal = document.getElementById('modal-etapa');
    if (!modal) return;
    modal.classList.add('hidden');
    modal.classList.remove('flex');
}

function guardarEtapa() {
    const nombre = document.getElementById('etapa-nombre').value.trim();
    const materiaId = document.getElementById('etapa-materia-id').value;
    if (!materiaId || !nombre) {
        Swal.fire({ icon: 'warning', title: 'Datos requeridos', text: 'Selecciona materia y escribe el nombre de la etapa.', confirmButtonColor: '#9D2449' });
        return;
    }

    const body = {
        id: document.getElementById('etapa-id').value,
        materiaId,
        parentId: document.getElementById('etapa-parent-id').value,
        nombre,
        clave: '',
        orden: document.getElementById('etapa-orden').value || '1',
        cierraExpediente: String(document.getElementById('etapa-cierra').checked),
        activo: String(document.getElementById('etapa-activo').checked)
    };

    fetch('/catalogos/etapas/guardar', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
    })
        .then(res => res.json())
        .then(data => {
            if (data.status === 'ok') {
                cerrarModalEtapa();
                Swal.fire({ icon: 'success', title: 'Guardado', text: data.mensaje, timer: 1400, showConfirmButton: false });
                const url = new URL(window.location);
                url.searchParams.set('tab', 'etapas');
                setTimeout(() => window.location.href = url.toString(), 350);
            } else {
                Swal.fire({ icon: 'error', title: 'Error', text: data.error || 'No se pudo guardar.', confirmButtonColor: '#9D2449' });
            }
        })
        .catch(err => Swal.fire({ icon: 'error', title: 'Error de conexion', text: err.message, confirmButtonColor: '#9D2449' }));
}

function eliminarEtapa(btn) {
    const nombre = btn.dataset.nombre || 'esta etapa';
    Swal.fire({
        icon: 'warning',
        title: 'Desactivar etapa',
        text: `La etapa "${nombre}" se ocultara de nuevos expedientes, pero se conserva para historial.`,
        showCancelButton: true,
        confirmButtonText: 'Desactivar',
        cancelButtonText: 'Cancelar',
        confirmButtonColor: '#9D2449'
    }).then(result => {
        if (!result.isConfirmed) return;
        fetch('/catalogos/etapas/eliminar/' + btn.dataset.id, { method: 'DELETE' })
            .then(res => res.json())
            .then(data => {
                if (data.status === 'ok') {
                    Swal.fire({ icon: 'success', title: 'Desactivada', text: data.mensaje, timer: 1400, showConfirmButton: false });
                    const url = new URL(window.location);
                    url.searchParams.set('tab', 'etapas');
                    setTimeout(() => window.location.href = url.toString(), 350);
                } else {
                    Swal.fire({ icon: 'error', title: 'Error', text: data.error || 'No se pudo desactivar.', confirmButtonColor: '#9D2449' });
                }
            });
    });
}

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
let itemAEliminar = null;

function eliminarItem(tipo, btn) {
    const cfg = TIPOS_CONFIG[tipo];
    if (!cfg) return;

    const id = btn.dataset.id;
    const nombre = btn.dataset.nombre;

    itemAEliminar = { tipo, id, nombre, btn };
    document.getElementById('nombre-registro-eliminar').textContent = nombre;

    const modal = document.getElementById('modal-confirmar-eliminar-registro');
    if (modal) {
        modal.classList.remove('hidden');
        modal.classList.add('flex');
    }
}

function cerrarConfirmarEliminarRegistro() {
    itemAEliminar = null;
    const modal = document.getElementById('modal-confirmar-eliminar-registro');
    if (modal) {
        modal.classList.add('hidden');
        modal.classList.remove('flex');
    }
}

function confirmarEliminarRegistro() {
    if (!itemAEliminar) return;
    const { tipo, id, btn } = itemAEliminar;
    const cfg = TIPOS_CONFIG[tipo];

    const btnConfirmar = document.getElementById('btn-confirmar-eliminar-registro');
    if (btnConfirmar) {
        btnConfirmar.disabled = true;
        btnConfirmar.innerHTML = '<i class="fas fa-spinner fa-spin mr-1"></i> Eliminando...';
    }

    fetch(cfg.urlEliminar + id, { method: 'DELETE' })
        .then(res => res.json())
        .then(data => {
            if (data.status === 'ok') {
                cerrarConfirmarEliminarRegistro();
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
                cerrarConfirmarEliminarRegistro();
                Swal.fire({ icon: 'error', title: 'Error', text: data.error || 'No se pudo eliminar.', confirmButtonColor: '#9D2449' });
            }
        })
        .catch(err => {
            cerrarConfirmarEliminarRegistro();
            Swal.fire({ icon: 'error', title: 'Error', text: 'No se pudo conectar con el servidor.', confirmButtonColor: '#9D2449' });
        })
        .finally(() => {
            if (btnConfirmar) {
                btnConfirmar.disabled = false;
                btnConfirmar.innerHTML = '<i class="fas fa-trash mr-1"></i> Eliminar';
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
    document.getElementById('search-etapas')?.addEventListener('input', aplicarFiltrosEtapas);
    document.getElementById('filtro-etapas-materia')?.addEventListener('change', aplicarFiltrosEtapas);
    aplicarFiltrosEtapas();

    document.getElementById('etapa-materia-id')?.addEventListener('change', filtrarParentsEtapa);
    document.getElementById('etapa-parent-id')?.addEventListener('change', () => {
        document.getElementById('etapa-orden')?.setAttribute('data-manual', 'false');
        sugerirOrdenEtapa();
        actualizarPreviewOrdenEtapa();
    });
    document.getElementById('etapa-nombre')?.addEventListener('input', actualizarPreviewOrdenEtapa);
    document.getElementById('etapa-orden')?.addEventListener('input', e => {
        e.currentTarget.dataset.manual = 'true';
        actualizarPreviewOrdenEtapa();
    });

    // Cerrar modal con Escape
    document.addEventListener('keydown', e => {
        if (e.key === 'Escape') cerrarModal();
    });

    // Cerrar modal clic fuera
    document.getElementById('modal-catalogo')?.addEventListener('click', e => {
        if (e.target === e.currentTarget) cerrarModal();
    });
    document.getElementById('modal-etapa')?.addEventListener('click', e => {
        if (e.target === e.currentTarget) cerrarModalEtapa();
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
    'cat_estados':              'tbody-sedes',
    'cat_etapas_procesales':    'tbody-etapas'
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
    const badge = tabBtn.querySelector('span.rounded-full');
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
            if (tbodyId === 'tbody-etapas') {
                aplicarFiltrosEtapas();
            } else {
                setupSearch(tbodyId.replace('tbody-', 'search-'), tbodyId);
            }
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
    if (window.WebSocketManager) {
        window.WebSocketManager.subscribe('/topic/catalogos', function (message) {
            try {
                const payload = JSON.parse(message.body);
                console.log(`[Catálogos] Cambio detectado en '${payload.tabla}' (${payload.operacion})`);
                refrescarTablaCatalogo(payload.tabla);
            } catch (e) {
                console.error('[Catálogos] Error parseando mensaje WS:', e);
            }
        });
    } else {
        setTimeout(initWebSocketCatalogos, 200);
    }
}

function actualizarTipoTermino() {
    const modo = document.getElementById('tt-modo')?.value;
    const esCalculado = modo === 'CALCULADO';
    const sinVencimiento = modo === 'SIN_VENCIMIENTO';
    const regla = document.getElementById('tt-regla');
    const perfilLabel = document.getElementById('tt-perfil-label');
    const dias = document.getElementById('tt-dias');
    const perfil = document.getElementById('tt-perfil');

    regla?.classList.toggle('hidden', sinVencimiento);
    perfilLabel?.classList.toggle('hidden', sinVencimiento);
    if (dias) {
        dias.closest('label')?.classList.toggle('hidden', !esCalculado);
        dias.required = esCalculado;
    }
    if (perfil) {
        perfil.required = !sinVencimiento;
        const compatible = esCalculado ? 'DIAS_RESTANTES' : 'PROPORCIONAL_LIMITADO';
        Array.from(perfil.options).forEach(option => {
            if (!option.value) return;
            option.hidden = option.dataset.modo !== compatible;
        });
        if (perfil.selectedOptions[0]?.hidden || sinVencimiento) perfil.value = '';
    }
}

function editarTipoTermino(button) {
    const valor = id => document.getElementById(id);
    valor('tt-id').value = button.dataset.id || '';
    valor('tt-codigo').value = button.dataset.codigo || '';
    valor('tt-nombre').value = button.dataset.nombre || '';
    valor('tt-descripcion').value = button.dataset.descripcion || '';
    valor('tt-modo').value = button.dataset.modo || 'CALCULADO';
    valor('tt-dias').value = button.dataset.dias || '';
    valor('tt-tipo-dias').value = button.dataset.tipoDias || 'HABILES';
    valor('tt-doc').checked = button.dataset.doc === 'true';
    valor('tt-acuse').checked = button.dataset.acuse === 'true';
    valor('tt-excepcion').checked = button.dataset.excepcion === 'true';
    valor('tt-validacion').checked = button.dataset.validacion === 'true';
    actualizarTipoTermino();
    valor('tt-perfil').value = button.dataset.perfil || '';
    valor('form-tipo-termino').scrollIntoView({ behavior: 'smooth', block: 'start' });
}

document.addEventListener('DOMContentLoaded', actualizarTipoTermino);

function prepararTipoAudiencia() {
    const seleccionadas = Array.from(document.querySelectorAll('.ta-modalidad:checked')).map(input => input.value);
    document.getElementById('ta-modalidades').value = seleccionadas.join(',');
}

function nuevoTipoAudienciaCompleto() {
    const form = document.getElementById('form-tipo-audiencia-completo');
    if (!form) return;
    form.reset();
    document.getElementById('ta-id').value = '';
    document.getElementById('ta-duracion').value = '60';
    document.querySelectorAll('.ta-modalidad').forEach(input => input.checked = true);
    prepararTipoAudiencia();
    form.scrollIntoView({ behavior: 'smooth', block: 'center' });
    document.getElementById('ta-nombre')?.focus();
}

function editarTipoAudienciaCompleto(button) {
    const valor = id => document.getElementById(id);
    valor('ta-id').value = button.dataset.id || '';
    valor('ta-codigo').value = button.dataset.codigo || '';
    valor('ta-nombre').value = button.dataset.nombre || '';
    valor('ta-descripcion').value = button.dataset.descripcion || '';
    valor('ta-perfil').value = button.dataset.perfil || '';
    valor('ta-duracion').value = button.dataset.duracion || '60';
    valor('ta-modalidad').value = button.dataset.modalidad || 'PRESENCIAL';
    const permitidas = (button.dataset.modalidades || '').split(',');
    document.querySelectorAll('.ta-modalidad').forEach(input => input.checked = permitidas.includes(input.value));
    valor('ta-acta').checked = button.dataset.acta === 'true';
    valor('ta-excepcion').checked = button.dataset.excepcion === 'true';
    valor('ta-validacion').checked = button.dataset.validacion === 'true';
    prepararTipoAudiencia();
    valor('form-tipo-audiencia-completo').scrollIntoView({ behavior: 'smooth', block: 'center' });
}

document.getElementById('form-tipo-audiencia-completo')?.addEventListener('submit', prepararTipoAudiencia);
