/**
 * Lógica del Módulo de Agenda General
 * Gestiona pestañas, filtros y modales de observación.
 */

// 1. CAMBIAR PESTAÑA (Global) - Conservado para compatibilidad
window.cambiarTab = function(tabName) {
    document.querySelectorAll('.tab-content').forEach(el => el.classList.remove('active'));
    document.querySelectorAll('.tab-btn').forEach(el => el.classList.remove('active'));
    const content = document.getElementById('tab-' + tabName);
    if(content) content.classList.add('active');
    const btn = document.getElementById('btn-tab-' + tabName);
    if(btn) btn.classList.add('active');
    try {
        const url = new URL(window.location);
        url.searchParams.set('tab', tabName);
        window.history.pushState({}, '', url);
    } catch(e) { console.error("Error actualizando URL", e); }
};

// 1.1 CAMBIAR PESTAÑA (PILL SWITCHER)
window.cambiarTabPill = function(tabName) {
    // Hide all contents
    document.querySelectorAll('.tab-content').forEach(el => {
        el.classList.add('hidden');
        el.classList.remove('block');
    });

    // Reset all buttons
    document.querySelectorAll('.tab-btn-pill').forEach(el => {
        el.classList.remove('bg-gob-guinda', 'text-white', 'shadow-md', 'shadow-gob-guinda/20');
        el.classList.add('text-gray-400', 'hover:bg-gray-50', 'hover:text-gray-600');
    });

    // Show active content
    const content = document.getElementById('tab-' + tabName);
    if(content) {
        content.classList.remove('hidden');
        content.classList.add('block');
    }

    // Highlight active button
    const btn = document.getElementById('btn-tab-' + tabName);
    if(btn) {
        btn.classList.remove('text-gray-400', 'hover:bg-gray-50', 'hover:text-gray-600');
        btn.classList.add('bg-gob-guinda', 'text-white', 'shadow-md', 'shadow-gob-guinda/20');
    }

    // Actualizar URL sin recargar
    try {
        const url = new URL(window.location);
        url.searchParams.set('tab', tabName);
        window.history.pushState({}, '', url);
    } catch(e) { console.error("Error actualizando URL", e); }
};


// 2. FILTRO RÁPIDO EN CLIENTE (Buscador)
window.filtrarTabla = function(tablaId, texto) {
    const filtro = texto.toLowerCase();
    const tabla = document.getElementById(tablaId); 
    if (!tabla) return;
    
    const filas = tabla.getElementsByTagName('tr');
    for (let i = 0; i < filas.length; i++) { 
        // Ignorar encabezados (dentro de thead)
        if (filas[i].parentNode.tagName === 'THEAD') continue;

        let textoFila = filas[i].textContent || filas[i].innerText;
        if (textoFila.toLowerCase().indexOf(filtro) > -1) {
            filas[i].style.display = "";
        } else {
            filas[i].style.display = "none";
        }
    }
};

// 3. VER OBSERVACIONES (Modal)
window.verObservaciones = function(id, tipo, expediente, observaciones) {
    const titleEl = document.getElementById('obs-modal-title');
    const expEl = document.getElementById('obs-modal-expediente');
    const contentEl = document.getElementById('obs-modal-content');
    const modal = document.getElementById('modal-observaciones');

    if(titleEl) {
        titleEl.innerHTML = tipo === 'audiencia' ? 
            '<i class="fas fa-gavel mr-2"></i>Observaciones Audiencia' : 
            '<i class="fas fa-clock mr-2"></i>Observaciones Término';
    }
    
    if(expEl) {
        expEl.innerHTML = `<span class="font-bold text-gob-guinda text-lg"><i class="fas fa-folder-open mr-2"></i>${expediente}</span>`;
    }
    
    if(contentEl) {
        // Limpiamos observaciones que vengan como "null" string
        let texto = (observaciones && observaciones !== 'null' && observaciones !== '') ? observaciones : 'Sin observaciones registradas.';
        contentEl.innerHTML = `<p class="text-gray-700 bg-gray-50 p-4 rounded border border-gray-200 text-sm leading-relaxed">${texto}</p>`;
    }

    if(modal) {
        modal.classList.remove('hidden');
        // Aseguramos visualización forzando flex si usas tailwind o style directo
        modal.style.display = 'flex'; 
        modal.classList.add('flex'); // Clase de utilidad si la tienes
    }
};

// 4. CERRAR MODAL
window.cerrarModal = function() {
    const modal = document.getElementById('modal-observaciones');
    if(modal) {
        modal.classList.add('hidden');
        modal.classList.remove('flex');
        modal.style.display = 'none';
    }
};

// 5. INICIALIZACIÓN AL CARGAR EL DOM
document.addEventListener("DOMContentLoaded", function() {
    // Asignar eventos de cierre al modal
    const btnClose = document.getElementById('close-modal-observaciones');
    if(btnClose) btnClose.onclick = window.cerrarModal;
    
    const btnCerrar = document.getElementById('btn-cerrar-obs');
    if(btnCerrar) btnCerrar.onclick = window.cerrarModal;
    
    // Cerrar al hacer clic fuera del contenido del modal
    window.onclick = function(event) {
        const modal = document.getElementById('modal-observaciones');
        if (event.target == modal) window.cerrarModal();
    };

    // Búsqueda asíncrona (Debounce)
    document.addEventListener('input', function(e) {
        if (e.target.id === 'search-agenda-audiencias' || e.target.id === 'search-agenda-terminos') {
            // Sincronizar el input oculto del formulario
            const inputHidden = document.getElementById('historial-keyword');
            if (inputHidden) {
                inputHidden.value = e.target.value;
            }

            clearTimeout(debounceTimerAgenda);
            debounceTimerAgenda = setTimeout(() => {
                actualizarAgendaAsincrono();
            }, 650);
        }
    });

    // Delegación para paginación asíncrona
    document.addEventListener('click', function(e) {
        const linkPag = e.target.closest('.px-3.py-1.border'); // The pagination links
        if (linkPag && linkPag.href && linkPag.href.includes('/agenda?')) {
            e.preventDefault();
            const url = linkPag.getAttribute('href');
            actualizarAgendaDesdeUrl(url);
        }
    });
});

// --- BÚSQUEDA ASÍNCRONA CENTRALIZADA ---
let debounceTimerAgenda;
let abortControllerAgenda;

function actualizarAgendaAsincrono() {
    // Collect all inputs
    const formFiltros = document.getElementById('form-filtros');
    const urlParams = new URLSearchParams();

    // Determinar qué pestaña está activa
    let tabActiva = 'audiencias';
    const tabTerminos = document.getElementById('tab-terminos');
    if (tabTerminos && tabTerminos.classList.contains('block')) {
        tabActiva = 'terminos';
    }
    urlParams.set('tab', tabActiva);

    // Obtener keyword del input visible
    let keyword = '';
    const searchAud = document.getElementById('search-agenda-audiencias');
    const searchTer = document.getElementById('search-agenda-terminos');
    
    if (tabActiva === 'audiencias' && searchAud) keyword = searchAud.value;
    else if (tabActiva === 'terminos' && searchTer) keyword = searchTer.value;

    if (keyword) {
        urlParams.set('keyword', keyword);
    }

    // Filtros
    if (formFiltros) {
        const fd = new FormData(formFiltros);
        for (const [key, value] of fd.entries()) {
            if (key === 'keyword' || key === 'tab' || key === '_csrf') continue;
            if (value && value !== 'null' && value !== '') {
                urlParams.set(key, value);
            }
        }
    }

    urlParams.set('page', 0); // Al buscar, reiniciar paginación

    const newUrl = window.location.pathname + '?' + urlParams.toString();
    window.history.replaceState({ path: newUrl }, '', newUrl);

    ejecutarFetchAgenda('/agenda?' + urlParams.toString());
}

function actualizarAgendaDesdeUrl(url) {
    window.history.replaceState({ path: url }, '', url);
    ejecutarFetchAgenda(url);
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

        // Reemplazar tabla audiencias
        const tablaAudNueva = doc.getElementById('contenedor-tabla-audiencias');
        const tablaAudActual = document.getElementById('contenedor-tabla-audiencias');
        if (tablaAudNueva && tablaAudActual) {
            tablaAudActual.innerHTML = tablaAudNueva.innerHTML;
        }

        // Reemplazar tabla terminos
        const tablaTerNueva = doc.getElementById('contenedor-tabla-terminos');
        const tablaTerActual = document.getElementById('contenedor-tabla-terminos');
        if (tablaTerNueva && tablaTerActual) {
            tablaTerActual.innerHTML = tablaTerNueva.innerHTML;
        }
    })
    .catch(err => {
        if (err.name === 'AbortError') return;
        console.error("Error en actualización asíncrona de agenda:", err);
    });
}