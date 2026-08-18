window.cambiarTab = function(tabName) {
    window.cambiarTabPill(tabName);
};

window.cambiarTabPill = function(tabName) {
    document.querySelectorAll('.tab-content').forEach(el => {
        el.classList.add('hidden');
        el.classList.remove('block');
    });

    document.querySelectorAll('.tab-btn-pill').forEach(el => {
        el.classList.remove('bg-gob-guinda', 'text-white', 'shadow-sm', 'shadow-md', 'shadow-gob-guinda/20');
        el.classList.add('text-gray-500', 'hover:bg-white', 'hover:text-gray-700');
    });

    const content = document.getElementById('tab-' + tabName);
    if (content) {
        content.classList.remove('hidden');
        content.classList.add('block');
    }

    const btn = document.getElementById('btn-tab-' + tabName);
    if (btn) {
        btn.classList.remove('text-gray-500', 'hover:bg-white', 'hover:text-gray-700');
        btn.classList.add('bg-gob-guinda', 'text-white', 'shadow-sm');
    }

    const tabInput = document.getElementById('agenda-active-tab');
    if (tabInput) tabInput.value = tabName;
    actualizarVisibilidadFiltrosAgenda(tabName);

    try {
        const url = new URL(window.location);
        url.searchParams.set('tab', tabName);
        window.history.pushState({}, '', url);
    } catch (e) {
        console.error('Error actualizando URL', e);
    }
};

function actualizarVisibilidadFiltrosAgenda(tabName) {
    document.querySelectorAll('.agenda-filter-audiencias').forEach(el => {
        el.classList.toggle('hidden', tabName !== 'audiencias');
    });
    document.querySelectorAll('.agenda-filter-terminos').forEach(el => {
        el.classList.toggle('hidden', tabName !== 'terminos');
    });
}

window.manejarRangoFechaChange = function() {
    const select = document.getElementById('agenda-rango-fecha');
    const form = document.getElementById('form-filtros');
    if (!select || !form) return;

    const inicio = form.querySelector('input[name="fechaInicio"]');
    const fin = form.querySelector('input[name="fechaFin"]');
    const hoy = new Date();
    let desde = null;
    let hasta = null;

    if (select.value === 'hoy') {
        desde = new Date(hoy);
        hasta = new Date(hoy);
    } else if (select.value === 'semana') {
        const dia = hoy.getDay() || 7;
        desde = new Date(hoy);
        desde.setDate(hoy.getDate() - dia + 1);
        hasta = new Date(desde);
        hasta.setDate(desde.getDate() + 6);
    } else if (select.value === 'mes') {
        desde = new Date(hoy.getFullYear(), hoy.getMonth(), 1);
        hasta = new Date(hoy.getFullYear(), hoy.getMonth() + 1, 0);
    } else if (select.value === 'mes_anterior') {
        desde = new Date(hoy.getFullYear(), hoy.getMonth() - 1, 1);
        hasta = new Date(hoy.getFullYear(), hoy.getMonth(), 0);
    }

    if (inicio && fin) {
        inicio.value = desde ? formatDateInput(desde) : '';
        fin.value = hasta ? formatDateInput(hasta) : '';
    }

    actualizarAgendaAsincrono();
};

window.manejarFechaManualChange = function() {
    const select = document.getElementById('agenda-rango-fecha');
    if (select) select.value = '';
    actualizarAgendaAsincrono();
};

function formatDateInput(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

window.filtrarTabla = function(tablaId, texto) {
    const filtro = texto.toLowerCase();
    const tabla = document.getElementById(tablaId);
    if (!tabla) return;

    const filas = tabla.getElementsByTagName('tr');
    for (let i = 0; i < filas.length; i++) {
        if (filas[i].parentNode.tagName === 'THEAD') continue;
        const textoFila = filas[i].textContent || filas[i].innerText;
        filas[i].style.display = textoFila.toLowerCase().indexOf(filtro) > -1 ? '' : 'none';
    }
};

window.verObservaciones = function(id, tipo, expediente, observaciones) {
    const titleEl = document.getElementById('obs-modal-title');
    const expEl = document.getElementById('obs-modal-expediente');
    const contentEl = document.getElementById('obs-modal-content');
    const modal = document.getElementById('modal-observaciones');

    if (titleEl) {
        titleEl.innerHTML = tipo === 'audiencia'
            ? '<i class="fas fa-gavel mr-2"></i>Observaciones Audiencia'
            : '<i class="fas fa-clock mr-2"></i>Observaciones Termino';
    }

    if (expEl) {
        expEl.innerHTML = `<span class="font-bold text-gob-guinda text-lg"><i class="fas fa-folder-open mr-2"></i>${expediente}</span>`;
    }

    if (contentEl) {
        const texto = (observaciones && observaciones !== 'null' && observaciones !== '') ? observaciones : 'Sin observaciones registradas.';
        contentEl.innerHTML = `<p class="text-gray-700 bg-gray-50 p-4 rounded border border-gray-200 text-sm leading-relaxed">${texto}</p>`;
    }

    if (modal) {
        modal.classList.remove('hidden');
        modal.style.display = 'flex';
        modal.classList.add('flex');
    }
};

window.cerrarModal = function() {
    const modal = document.getElementById('modal-observaciones');
    if (modal) {
        modal.classList.add('hidden');
        modal.classList.remove('flex');
        modal.style.display = 'none';
    }
};

let debounceTimerAgenda;
let abortControllerAgenda;

document.addEventListener('DOMContentLoaded', function() {
    try {
        const url = new URL(window.location);
        actualizarVisibilidadFiltrosAgenda(url.searchParams.get('tab') || 'audiencias');
    } catch (e) {
        actualizarVisibilidadFiltrosAgenda('audiencias');
    }

    const btnClose = document.getElementById('close-modal-observaciones');
    if (btnClose) btnClose.onclick = window.cerrarModal;

    const btnCerrar = document.getElementById('btn-cerrar-obs');
    if (btnCerrar) btnCerrar.onclick = window.cerrarModal;

    window.onclick = function(event) {
        const modal = document.getElementById('modal-observaciones');
        if (event.target === modal) window.cerrarModal();
    };

    document.addEventListener('input', function(e) {
        if (!e.target.classList || !e.target.classList.contains('agenda-search')) return;
        const inputHidden = document.getElementById('historial-keyword');
        if (inputHidden) inputHidden.value = e.target.value;

        clearTimeout(debounceTimerAgenda);
        debounceTimerAgenda = setTimeout(() => {
            actualizarAgendaAsincrono();
        }, 650);
    });

    document.addEventListener('change', function(e) {
        const filterType = e.target.dataset ? e.target.dataset.agendaFilter : null;
        if (!filterType) return;

        if (filterType === 'gerencia') {
            cargarCatalogosAgenda({ resetMateria: true, resetAbogado: true, resetTipoAudiencia: true })
                .then(() => actualizarAgendaAsincrono());
            return;
        }

        if (filterType === 'materia') {
            cargarCatalogosAgenda({ resetAbogado: true })
                .then(() => actualizarAgendaAsincrono());
            return;
        }

        actualizarAgendaAsincrono();
    });

    document.addEventListener('click', function(e) {
        const linkPag = e.target.closest('.px-3.py-1.border');
        if (linkPag && linkPag.href && linkPag.href.includes('/agenda?')) {
            e.preventDefault();
            actualizarAgendaDesdeUrl(linkPag.getAttribute('href'));
        }
    });
});

function actualizarAgendaAsincrono() {
    const formFiltros = document.getElementById('form-filtros');
    const urlParams = new URLSearchParams();

    let tabActiva = 'audiencias';
    const tabTerminos = document.getElementById('tab-terminos');
    if (tabTerminos && tabTerminos.classList.contains('block')) {
        tabActiva = 'terminos';
    }
    urlParams.set('tab', tabActiva);

    const searchGlobal = document.getElementById('search-agenda-global');
    const keyword = searchGlobal ? searchGlobal.value : '';
    if (keyword) urlParams.set('keyword', keyword);

    if (formFiltros) {
        const fd = new FormData(formFiltros);
        for (const [key, value] of fd.entries()) {
            if (key === 'keyword' || key === 'tab' || key === '_csrf') continue;
            if (value && value !== 'null' && value !== '') {
                urlParams.set(key, value);
            }
        }
    }

    urlParams.set('page', 0);
    const newUrl = window.location.pathname + '?' + urlParams.toString();
    window.history.replaceState({ path: newUrl }, '', newUrl);
    ejecutarFetchAgenda('/agenda?' + urlParams.toString());
}

function actualizarAgendaDesdeUrl(url) {
    window.history.replaceState({ path: url }, '', url);
    ejecutarFetchAgenda(url);
}

function cargarCatalogosAgenda(options = {}) {
    const formFiltros = document.getElementById('form-filtros');
    if (!formFiltros) return Promise.resolve();

    const gerenciaSelect = formFiltros.querySelector('select[name="gerenciaId"]');
    const materiaSelect = formFiltros.querySelector('select[name="materiaId"]');
    const abogadoSelect = formFiltros.querySelector('select[name="abogadoId"]');
    const tipoAudienciaSelect = formFiltros.querySelector('select[name="tipoAudienciaId"]');

    const params = new URLSearchParams();
    const gerenciaId = gerenciaSelect ? gerenciaSelect.value : '';
    const materiaId = materiaSelect ? materiaSelect.value : '';

    if (gerenciaId) params.set('gerenciaId', gerenciaId);
    if (materiaId && !options.resetMateria) params.set('materiaId', materiaId);

    bloquearSelect(materiaSelect, true);
    bloquearSelect(abogadoSelect, true);
    bloquearSelect(tipoAudienciaSelect, true);

    return fetch('/agenda/api/filtros?' + params.toString(), {
        headers: { 'X-Requested-With': 'XMLHttpRequest' }
    })
    .then(res => {
        if (!res.ok) throw new Error('No se pudieron cargar los filtros dependientes');
        return res.json();
    })
    .then(data => {
        if (materiaSelect) {
            actualizarOpcionesSelect(materiaSelect, data.materias || [], 'Todas', options.resetMateria ? '' : materiaSelect.value);
        }
        if (abogadoSelect) {
            actualizarOpcionesSelect(abogadoSelect, data.abogados || [], 'Todos', options.resetAbogado ? '' : abogadoSelect.value);
        }
        if (tipoAudienciaSelect) {
            actualizarOpcionesSelect(tipoAudienciaSelect, data.tiposAudiencia || [], 'Todos', options.resetTipoAudiencia ? '' : tipoAudienciaSelect.value);
        }
    })
    .catch(err => {
        console.error('Error cargando catalogos de agenda:', err);
    })
    .finally(() => {
        bloquearSelect(materiaSelect, false);
        bloquearSelect(abogadoSelect, false);
        bloquearSelect(tipoAudienciaSelect, false);
    });
}

function bloquearSelect(select, bloqueado) {
    if (!select) return;
    select.disabled = bloqueado;
    select.classList.toggle('opacity-60', bloqueado);
}

function actualizarOpcionesSelect(select, items, placeholder, selectedValue) {
    if (!select) return;

    const selected = String(selectedValue || '');
    select.innerHTML = '';
    select.appendChild(new Option(placeholder, ''));

    let conservaSeleccion = false;
    items.forEach(item => {
        const option = new Option(item.nombre, item.id);
        if (String(item.id) === selected) {
            option.selected = true;
            conservaSeleccion = true;
        }
        select.appendChild(option);
    });

    if (!conservaSeleccion) {
        select.value = '';
    }
}

function ejecutarFetchAgenda(url) {
    if (abortControllerAgenda) {
        abortControllerAgenda.abort();
    }
    abortControllerAgenda = new AbortController();

    fetch(url, {
        headers: { 'X-Requested-With': 'XMLHttpRequest' },
        signal: abortControllerAgenda.signal
    })
    .then(res => res.text())
    .then(html => {
        const parser = new DOMParser();
        const doc = parser.parseFromString(html, 'text/html');

        const tablaAudNueva = doc.getElementById('contenedor-tabla-audiencias');
        const tablaAudActual = document.getElementById('contenedor-tabla-audiencias');
        if (tablaAudNueva && tablaAudActual) {
            tablaAudActual.innerHTML = tablaAudNueva.innerHTML;
        }

        const tablaTerNueva = doc.getElementById('contenedor-tabla-terminos');
        const tablaTerActual = document.getElementById('contenedor-tabla-terminos');
        if (tablaTerNueva && tablaTerActual) {
            tablaTerActual.innerHTML = tablaTerNueva.innerHTML;
        }
    })
    .catch(err => {
        if (err.name === 'AbortError') return;
        console.error('Error en actualizacion asincrona de agenda:', err);
    });
}
