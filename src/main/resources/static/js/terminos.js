/**
 * Lógica para el módulo de Términos
 * Autor: Ricardo Villalobos
 */

document.addEventListener('DOMContentLoaded', function () {
    console.log("Sistema de Terminos Cargado");
    initMiniCalendar();
    initSearchTerminos();

    // D. Paginación Asíncrona (Delegación Global)
    document.addEventListener('click', function (e) {
        // 1. Paginación
        const linkPag = e.target.closest('.pagina-link-ter');
        if (linkPag) {
            e.preventDefault();
            const url = linkPag.getAttribute('href');
            fetch(url, { headers: { 'X-Requested-With': 'XMLHttpRequest' } })
                .then(res => res.text())
                .then(html => {
                    const parser = new DOMParser();
                    const doc = parser.parseFromString(html, 'text/html');
                    const nuevoMaestro = doc.getElementById('grid-maestro-terminos');
                    const maestroDestino = document.getElementById('grid-maestro-terminos');
                    if (nuevoMaestro && maestroDestino) {
                        maestroDestino.innerHTML = nuevoMaestro.innerHTML;
                    }
                    window.history.replaceState({ path: url }, '', url);
                })
                .catch(err => console.error("Error en paginación asíncrona:", err));
            return;
        }

        // 2. Botón Limpiar Búsqueda (Barra Superior)
        const btnClear = e.target.closest('#btn-clear-search-terminos');
        if (btnClear) {
            const input = document.getElementById('input-search-terminos');
            if (input) input.value = '';
            actualizarTablaAsincrono();
            return;
        }

        // 3. Botón Limpiar Filtros (Sidebar)
        const btnLimpiar = e.target.closest('#btn-limpiar-filtros-ter');
        if (btnLimpiar) {
            const form = document.getElementById('form-filtros');
            if (form) {
                form.querySelectorAll('select').forEach(s => s.value = '');
            }
            const input = document.getElementById('input-search-terminos');
            if (input) input.value = '';

            miniCalendarState.isDateFilterActive = false;
            miniCalendarState.selectedDate = new Date(0);
            renderMiniCalendar();
            actualizarTablaAsincrono();
            return;
        }
    });

    // E. Búsqueda en Tiempo Real (Delegación Global con Debounce)
    document.addEventListener('input', function (e) {
        if (e.target.id === 'input-search-terminos') {
            const input = e.target;
            const btnClear = document.getElementById('btn-clear-search-terminos');
            if (input.value) {
                btnClear?.classList.remove('hidden');
            } else {
                btnClear?.classList.add('hidden');
            }

            clearTimeout(debounceTimerTerminos);
            debounceTimerTerminos = setTimeout(() => {
                actualizarTablaAsincrono();
            }, 650);
        }
    });

    // F. Submit del formulario de búsqueda (Evitar recarga)
    document.addEventListener('submit', function (e) {
        if (e.target.id === 'form-search-terminos') {
            e.preventDefault();
            actualizarTablaAsincrono();
        }
    });

    // G. Filtros del sidebar (Delegación para que no sea necesario rebindear)
    document.addEventListener('change', function (e) {
        if (e.target.closest('#form-filtros')) {
            actualizarTablaAsincrono();
        }
    });
});

// --- BÚSQUEDA ASÍNCRONA CENTRALIZADA ---
let debounceTimerTerminos;
let abortControllerTerminos;

function actualizarTablaAsincrono(keywordForzado = null) {
    const input = document.getElementById('input-search-terminos');
    const container = document.getElementById('tabla-resultados-container');

    if (!container) return;

    // Abortar petición previa si existe
    if (abortControllerTerminos) {
        abortControllerTerminos.abort();
    }
    abortControllerTerminos = new AbortController();

    const urlParams = new URLSearchParams();

    // 1. Keyword (Buscador o Fecha del Calendario)
    const valKeyword = keywordForzado !== null ? keywordForzado : (input ? input.value : '');
    if (valKeyword) urlParams.set('keyword', valKeyword);

    // 2. Mes y Año del Calendario (Solo si el filtro de fecha está activo)
    if (miniCalendarState.isDateFilterActive) {
        const date = miniCalendarState.currentDate;
        urlParams.set('mes', date.getMonth() + 1);
        urlParams.set('anio', date.getFullYear());
    }

    // 3. Filtros del Lateral
    const formFiltros = document.getElementById('form-filtros');
    let filterCount = 0;
    if (formFiltros) {
        const fd = new FormData(formFiltros);
        for (const [key, value] of fd.entries()) {
            if (key === 'mes' || key === 'anio' || key === 'keyword' || key === '_csrf') continue;
            if (value && value !== 'null' && value !== '') {
                urlParams.set(key, value);
                filterCount++;
            }
        }
    }

    // 4. Actualizar Indicador Visual (Badge con Conteo)
    const badge = document.getElementById('badge-filtros-ter');
    if (badge) {
        if (filterCount > 0) {
            badge.innerText = filterCount;
            badge.classList.remove('hidden');
        } else {
            badge.classList.add('hidden');
        }
    }

    urlParams.set('page', 0);

    const newUrl = window.location.pathname + '?' + urlParams.toString();
    window.history.replaceState({ path: newUrl }, '', newUrl);

    fetch('/terminos?' + urlParams.toString(), {
        headers: { 'X-Requested-With': 'XMLHttpRequest' },
        signal: abortControllerTerminos.signal
    })
        .then(res => res.text())
        .then(html => {
            const parser = new DOMParser();
            const doc = parser.parseFromString(html, 'text/html');

            // 1. ACTUALIZAR KPIs (Sin afectar al resto)
            const kpisNuevos = doc.getElementById('contenedor-kpis-terminos');
            const kpisActuales = document.getElementById('contenedor-kpis-terminos');
            if (kpisNuevos && kpisActuales) {
                kpisActuales.innerHTML = kpisNuevos.innerHTML;
            }

            // 2. ACTUALIZAR TABLA DE RESULTADOS (Lo más importante)
            const tablaNueva = doc.getElementById('tabla-resultados-container');
            const tablaActual = document.getElementById('tabla-resultados-container');
            if (tablaNueva && tablaActual) {
                tablaActual.innerHTML = tablaNueva.innerHTML;
            }

            // 3. ACTUALIZAR ACCIONES (VerTodos y Exportar que dependen de filtros)
            const accionesNuevas = doc.getElementById('acciones-barra-terminos');
            const accionesActuales = document.getElementById('acciones-barra-terminos');
            if (accionesNuevas && accionesActuales) {
                accionesActuales.innerHTML = accionesNuevas.innerHTML;
            }

            // 4. SINCRONIZAR INPUT (Solo si se forzó desde fuera, ej: KPIs)
            const searchInput = document.getElementById('input-search-terminos');
            if (keywordForzado !== null && searchInput) {
                searchInput.value = keywordForzado;
                const btnClear = document.getElementById('btn-clear-search-terminos');
                if (keywordForzado) btnClear?.classList.remove('hidden');
                else btnClear?.classList.add('hidden');
            }
        })
        .catch(err => {
            if (err.name === 'AbortError') return; // Ignorar si fue abortada
            console.error("Error en actualización asíncrona:", err);
        });
}

function initSearchTerminos() {
    // Implementación asíncrona vía delegación global
}

// --- MENÚS DESPLEGABLES ---
function toggleMenu(id) {
    document.querySelectorAll('[id^="menu-"]').forEach(menu => {
        if (menu.id !== 'menu-' + id) {
            menu.classList.add('hidden');
        }
    });

    const menu = document.getElementById('menu-' + id);
    if (menu) {
        menu.classList.toggle('hidden');
    }
}

// Cierra el menú si haces clic fuera de él
window.addEventListener('click', function (e) {
    if (!e.target.closest('td.relative')) {
        document.querySelectorAll('[id^="menu-"]').forEach(menu => {
            menu.classList.add('hidden');
        });
    }
});

// --- LÍNEA DE TIEMPO (NUEVO) ---
function toggleTimeline(id) {
    const row = document.getElementById('timeline-' + id);
    const btn = document.getElementById('btn-expand-' + id);

    if (row) {
        row.classList.toggle('hidden');
        if (btn) {
            // Rotar flecha
            if (!row.classList.contains('hidden')) {
                btn.classList.add('rotate-180', 'text-gob-guinda');
                btn.classList.remove('text-gray-400');
            } else {
                btn.classList.remove('rotate-180', 'text-gob-guinda');
                btn.classList.add('text-gray-400');
            }
        }
    }
}

// --- SUBIDA DE ARCHIVOS ---
function activarSubida(idTermino) {
    const inputId = document.getElementById('upload-termino-id');
    const inputFile = document.getElementById('upload-file-input');

    if (inputId && inputFile) {
        inputId.value = idTermino;
        inputFile.click();
    }
}

// ROD-186: Configuración de Notificaciones Estilo Proyecto
const Toast = Swal.mixin({
    toast: true,
    position: 'top-end',
    showConfirmButton: false,
    timer: 3000,
    timerProgressBar: true,
    didOpen: (toast) => {
        toast.addEventListener('mouseenter', Swal.stopTimer)
        toast.addEventListener('mouseleave', Swal.resumeTimer)
    }
});

function showSuccessToast(mensaje) {
    Toast.fire({
        icon: 'success',
        title: mensaje,
        iconColor: '#22c55e',
        background: '#f0fdf4',
        padding: '1em',
        customClass: { title: 'text-green-800 text-sm font-bold' }
    });
}

function showErrorModal(mensaje) {
    Swal.fire({
        title: 'Atención',
        text: mensaje,
        icon: 'error',
        confirmButtonText: 'Entendido',
        confirmButtonColor: '#bc955c',
        background: '#f9fafb',
        customClass: { confirmButton: 'font-bold' }
    });
}

function confirmarEliminar(id) {
    Swal.fire({
        title: '¿Eliminar término?',
        text: "Esta acción no se puede deshacer",
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#d33',
        cancelButtonColor: '#3085d6',
        confirmButtonText: 'Sí, eliminar',
        cancelButtonText: 'Cancelar'
    }).then((result) => {
        if (result.isConfirmed) {
            // ROD-186: Usar AJAX para eliminar y preservar filtros
            fetch('/terminos/eliminar/' + id, {
                method: 'POST',
                headers: { 'X-Requested-With': 'XMLHttpRequest' }
            })
                .then(res => {
                    if (res.ok) {
                        showSuccessToast('¡Término eliminado correctamente!');
                        actualizarTablaAsincrono();
                    } else {
                        throw new Error('Error al eliminar');
                    }
                })
                .catch(err => {
                    showErrorModal('No se pudo eliminar el registro.');
                });
        }
    })
}

// ROD-186: Listeners para subida de archivos y conclusión vía AJAX (Preservar contexto)

// A. Subida de Archivo General (Form oculto en index)
document.addEventListener('change', function (e) {
    if (e.target && e.target.id === 'upload-file-input') {
        const form = document.getElementById('form-upload');
        if (!form) return;

        const fd = new FormData(form);
        Swal.fire({ title: 'Subiendo archivo...', allowOutsideClick: false, didOpen: () => Swal.showLoading() });

        fetch(form.action, {
            method: 'POST',
            body: fd,
            headers: { 'X-Requested-With': 'XMLHttpRequest' }
        })
            .then(res => {
                if (res.ok) {
                    showSuccessToast('¡Archivo subido correctamente!');
                    actualizarTablaAsincrono();
                } else throw new Error();
            })
            .catch(() => showErrorModal('No se pudo subir el archivo.'));
    }
});

// B. Concluir Término (Form en modal)
document.addEventListener('submit', function (e) {
    // Buscar si el form es el de concluir (está dentro del modal-concluir-final)
    const formConcluir = e.target.closest('#modal-concluir-final form');
    if (formConcluir) {
        e.preventDefault();
        const fd = new FormData(formConcluir);

        fetch(formConcluir.action, {
            method: 'POST',
            body: fd,
            headers: { 'X-Requested-With': 'XMLHttpRequest' }
        })
            .then(res => {
                if (res.ok) {
                    cerrarModal('modal-concluir-final');
                    showSuccessToast('¡Término Concluido con éxito!');
                    actualizarTablaAsincrono();
                } else throw new Error();
            })
            .catch(() => showErrorModal('No se pudo concluir el término.'));
    }

    // C. Subir Acuse (Form en modal acuse)
    const formAcuse = e.target.closest('#modal-subir-acuse form');
    if (formAcuse) {
        e.preventDefault();
        const fd = new FormData(formAcuse);
        Swal.fire({ title: 'Subiendo acuse...', allowOutsideClick: false, didOpen: () => Swal.showLoading() });

        fetch(formAcuse.action, {
            method: 'POST',
            body: fd,
            headers: { 'X-Requested-With': 'XMLHttpRequest' }
        })
            .then(res => {
                if (res.ok) {
                    cerrarModal('modal-subir-acuse');
                    showSuccessToast('¡Acuse registrado correctamente!');
                    actualizarTablaAsincrono();
                } else throw new Error();
            })
            .catch(() => showErrorModal('No se pudo registrar el acuse.'));
    }

    // D. Guardar/Editar Término
    const formTermino = e.target.closest('#modal-nuevo-termino form');
    if (formTermino) {
        e.preventDefault();
        const fd = new FormData(formTermino);
        Swal.fire({ title: 'Guardando...', allowOutsideClick: false, didOpen: () => Swal.showLoading() });

        fetch(formTermino.action, {
            method: 'POST',
            body: fd,
            headers: { 'X-Requested-With': 'XMLHttpRequest' }
        })
            .then(res => {
                if (res.ok) {
                    cerrarModal('modal-nuevo-termino');
                    showSuccessToast('¡Datos guardados correctamente!');
                    actualizarTablaAsincrono();
                } else throw new Error();
            })
            .catch(() => showErrorModal('Error al guardar el término.'));
    }
});


// --- MODALES (NUEVO / EDITAR) ---



function activarSubidaAcuse(idTermino) {
    const inputId = document.getElementById('input-acuse-id');
    if (inputId) {
        inputId.value = idTermino;
        const modal = document.getElementById('modal-subir-acuse');
        if (modal) {
            modal.classList.remove('hidden');
            modal.classList.add('flex');
        }
    }
}


// 1. ABRIR MODAL NUEVO (Limpio)
function abrirModalNuevo() {
    const idInput = document.getElementById('input-termino-id');
    const expSelect = document.getElementById('select-expediente-termino');
    const asuntoInput = document.getElementById('input-asunto');
    const vencimientoInput = document.getElementById('input-vencimiento');
    const abogadoSelect = document.getElementById('select-abogado-termino');
    const observacionesInput = document.getElementById('input-observaciones-termino');
    const titulo = document.getElementById('modal-titulo');

    // Limpiar campos
    if (idInput) idInput.value = "";
    if (expSelect) expSelect.value = "";
    if (asuntoInput) asuntoInput.value = "";
    if (vencimientoInput) vencimientoInput.value = "";
    if (observacionesInput) observacionesInput.value = "";

    // Resetear abogado
    if (abogadoSelect) {
        abogadoSelect.innerHTML = '<option value="">Seleccione expediente primero...</option>';
        abogadoSelect.classList.add('cursor-not-allowed');
        abogadoSelect.classList.add('bg-gray-50');
    }

    // Resetear prioridad
    const prioridadSelect = document.getElementById('select-prioridad');
    if (prioridadSelect) prioridadSelect.value = "MEDIA";

    if (titulo) titulo.innerHTML = '<i class="fas fa-plus-circle"></i> Nuevo Término';

    const modal = document.getElementById('modal-nuevo-termino');
    if (modal) {
        modal.classList.remove('hidden');
        modal.classList.add('flex');
    }
}

// 2. EDITAR TÉRMINO (Con el botón amarillo)
function editarTermino(id, actuacion, fecha, expedienteId, expedienteNum, abogadoId, prioridad, observaciones = "") {

    // Llenar datos en el formulario
    const idInput = document.getElementById('input-termino-id');
    const asuntoInput = document.getElementById('input-asunto');
    const vencimientoInput = document.getElementById('input-vencimiento');
    const expSelect = document.getElementById('select-expediente-termino');
    const prioridadSelect = document.getElementById('select-prioridad');
    const observacionesInput = document.getElementById('input-observaciones-termino');
    const titulo = document.getElementById('modal-titulo');

    if (idInput) idInput.value = id;
    if (asuntoInput) asuntoInput.value = actuacion;
    if (vencimientoInput) vencimientoInput.value = fecha;
    if (prioridadSelect && prioridad) prioridadSelect.value = prioridad;
    if (observacionesInput) observacionesInput.value = observaciones;

    if (expSelect && expedienteId) {
        // Verificar si la opción ya existe en la lista del select actual (por filtro de roles)
        let optionExists = Array.from(expSelect.options).some(opt => opt.value == expedienteId);

        // Si no existe, agregarla dinámicamente para que el 'required' del select no bloquee el guardado
        if (!optionExists) {
            const opt = document.createElement('option');
            opt.value = expedienteId;
            opt.text = expedienteNum + " (Autoseleccionado)";
            expSelect.add(opt);
        }

        expSelect.value = expedienteId;
        vincularAbogadoTermino(); // Cargar abogado automáticamente
    }

    if (titulo) titulo.innerHTML = '<i class="fas fa-edit"></i> Editar Término';

    const modal = document.getElementById('modal-nuevo-termino');
    if (modal) {
        modal.classList.remove('hidden');
        modal.classList.add('flex');
    }
}

// 3. VINCULACIÓN AUTOMÁTICA
function vincularAbogadoTermino() {
    const expSelect = document.getElementById('select-expediente-termino');
    const abogadoSelect = document.getElementById('select-abogado-termino');

    if (!expSelect || !abogadoSelect) return;

    const expId = expSelect.value;
    if (!expId) return;

    abogadoSelect.innerHTML = '<option>Cargando...</option>';

    fetch(`/terminos/api/responsable-expediente/${expId}`)
        .then(response => {
            if (!response.ok) throw new Error("Sin responsable");
            return response.json();
        })
        .then(data => {
            abogadoSelect.innerHTML = '';
            const option = document.createElement('option');
            option.value = data.id;
            option.text = data.nombre;
            option.selected = true;
            abogadoSelect.appendChild(option);

            abogadoSelect.classList.remove('cursor-not-allowed', 'bg-gray-50');
            abogadoSelect.classList.add('bg-white');
        })
        .catch(err => {
            console.error(err);
            abogadoSelect.innerHTML = '<option value="">Sin abogado asignado</option>';
        });
}
// ABRIR MODAL PARA CONCLUIR TÉRMINO
function abrirModalConcluir(idTermino) {
    const inputId = document.getElementById('input-concluir-id');
    const modal = document.getElementById('modal-concluir-final');

    if (inputId && modal) {
        inputId.value = idTermino;
        modal.classList.remove('hidden');
        modal.classList.add('flex');
    }
}

/**
 * Función estandarizada para cerrar modales eliminando conflictos de display
 */
function cerrarModal(id) {
    const modal = document.getElementById(id);
    if (modal) {
        modal.classList.add('hidden');
        modal.classList.remove('flex');
    }
}

// ==========================================
// 📅 LÓGICA DEL MINI-CALENDARIO (TÉRMINOS)
// ==========================================

const miniCalendarState = {
    currentDate: new Date(),
    selectedDate: new Date(),
    isDateFilterActive: false, // ROD-185: Bandera para saber si filtramos por fecha o vemos todo
    names: ["Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"],
    events: []
};

function initMiniCalendar() {
    const prevBtn = document.getElementById('miniPrev');
    const nextBtn = document.getElementById('miniNext');

    // Sincronizar con el estado del servidor
    const hdnMes = document.getElementById('hdnMesActual');
    const hdnAnio = document.getElementById('hdnAnioActual');
    if (hdnMes && hdnAnio) {
        miniCalendarState.currentDate = new Date(parseInt(hdnAnio.value), parseInt(hdnMes.value) - 1, 1);
    }

    // Determinar si hay un filtro de fecha activo en la URL inicial
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has('mes') || urlParams.has('anio') || urlParams.get('keyword')?.match(/^\d{4}-\d{2}-\d{2}$/)) {
        miniCalendarState.isDateFilterActive = true;
    }

    // Si hay un keyword que es fecha, marcarla como seleccionada
    const keyword = urlParams.get('keyword');
    if (keyword && keyword.match(/^\d{4}-\d{2}-\d{2}$/)) {
        const parts = keyword.split('-');
        miniCalendarState.selectedDate = new Date(parseInt(parts[0]), parseInt(parts[1]) - 1, parseInt(parts[2]));
    }

    if (prevBtn && nextBtn) {
        prevBtn.onclick = () => {
            const date = miniCalendarState.currentDate;
            date.setMonth(date.getMonth() - 1);
            const searchInput = document.getElementById('input-search-terminos');
            if (searchInput) {
                searchInput.value = '';
                const btnClear = document.getElementById('btn-clear-search-terminos');
                if (btnClear) btnClear.classList.add('hidden');
            }
            renderMiniCalendar();
            miniCalendarState.isDateFilterActive = true; // Activar al navegar meses
            actualizarTablaAsincrono();
        };
        nextBtn.onclick = () => {
            const date = miniCalendarState.currentDate;
            date.setMonth(date.getMonth() + 1);
            const searchInput = document.getElementById('input-search-terminos');
            if (searchInput) {
                searchInput.value = '';
                const btnClear = document.getElementById('btn-clear-search-terminos');
                if (btnClear) btnClear.classList.add('hidden');
            }
            renderMiniCalendar();
            miniCalendarState.isDateFilterActive = true; // Activar al navegar meses
            actualizarTablaAsincrono();
        };
    }

    renderMiniCalendar();
}

function renderMiniCalendar() {
    const grid = document.getElementById('miniCalendarGrid');
    const monthLabel = document.getElementById('miniCalendarMonth');
    if (!grid || !monthLabel) return;

    grid.innerHTML = '';
    const year = miniCalendarState.currentDate.getFullYear();
    const month = miniCalendarState.currentDate.getMonth();

    monthLabel.innerText = `${miniCalendarState.names[month]} ${year}`;
    monthLabel.className = "text-base font-bold text-white font-headings cursor-pointer px-2 py-1 rounded-lg transition-all flex items-center gap-2 border border-gob-guinda bg-gob-guinda hover:bg-gob-guinda/80";
    monthLabel.title = "Ver todo el mes";
    monthLabel.onclick = () => {
        // Al hacer clic en el mes, queremos ver TODO el mes
        miniCalendarState.selectedDate = new Date(0);
        const searchInput = document.getElementById('input-search-terminos');
        if (searchInput) {
            searchInput.value = '';
            const btnClear = document.getElementById('btn-clear-search-terminos');
            if (btnClear) btnClear.classList.add('hidden');
        }
        miniCalendarState.isDateFilterActive = true; // ACTIVAR filtro de fecha
        renderMiniCalendar();
        actualizarTablaAsincrono();
    };

    const firstDay = new Date(year, month, 1).getDay();
    const daysInMonth = new Date(year, month + 1, 0).getDate();

    // Celdas vacías
    for (let i = 0; i < firstDay; i++) {
        const emptyCell = document.createElement('div');
        emptyCell.className = 'mini-day-cell mini-empty';
        grid.appendChild(emptyCell);
    }

    // Días del mes
    const today = new Date();
    for (let day = 1; day <= daysInMonth; day++) {
        const cell = document.createElement('div');
        cell.className = 'mini-day-cell';
        cell.innerText = day;

        const isToday = day === today.getDate() && month === today.getMonth() && year === today.getFullYear();
        if (isToday) cell.classList.add('mini-day-today');

        // Lógica de Selección (Fondo Guinda Sólido)
        const isSelected = day === miniCalendarState.selectedDate.getDate() &&
            month === miniCalendarState.selectedDate.getMonth() &&
            year === miniCalendarState.selectedDate.getFullYear();
        if (isSelected) {
            cell.classList.add('mini-day-selected');
        }

        cell.onclick = () => {
            const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
            miniCalendarState.selectedDate = new Date(year, month, day);
            miniCalendarState.isDateFilterActive = true; // ACTIVAR filtro de fecha
            renderMiniCalendar();
            actualizarTablaAsincrono(dateStr);
        };

        grid.appendChild(cell);
    }
}

/**
 * Cambia entre la vista de Calendario y Filtros en el sidebar (ROD-183)
 */
function toggleSidebarView(view) {
    const btnCal = document.getElementById('btn-ver-calendario');
    const btnFil = document.getElementById('btn-ver-filtros');
    const contCal = document.getElementById('cont-calendario');
    const contFil = document.getElementById('cont-filtros');

    if (view === 'calendario') {
        // Activar Botón Calendario
        btnCal.classList.add('bg-gob-guinda', 'text-white', 'shadow-md', 'shadow-gob-guinda/20');
        btnCal.classList.remove('text-gray-400', 'hover:bg-gray-50');

        // Desactivar Botón Filtros
        btnFil.classList.remove('bg-gob-guinda', 'text-white', 'shadow-md', 'shadow-gob-guinda/20');
        btnFil.classList.add('text-gray-400', 'hover:bg-gray-50');

        // Mostrar Contenedor Calendario
        contCal.classList.remove('hidden');
        contFil.classList.add('hidden');
    } else {
        // Activar Botón Filtros
        btnFil.classList.add('bg-gob-guinda', 'text-white', 'shadow-md', 'shadow-gob-guinda/20');
        btnFil.classList.remove('text-gray-400', 'hover:bg-gray-50');

        // Desactivar Botón Calendario
        btnCal.classList.remove('bg-gob-guinda', 'text-white', 'shadow-md', 'shadow-gob-guinda/20');
        btnCal.classList.add('text-gray-400', 'hover:bg-gray-50');

        // Mostrar Contenedor Filtros
        contFil.classList.remove('hidden');
        contCal.classList.add('hidden');
    }
}

// Inicialización: Si hay filtros activos con VALOR en la URL, mostrar la pestaña de filtros por defecto
document.addEventListener('DOMContentLoaded', () => {
    const params = new URLSearchParams(window.location.search);
    const filterKeys = ['gerenciaId', 'materiaId', 'estatus', 'abogadoId', 'periodo', 'prioridadId'];

    let count = 0;
    filterKeys.forEach(key => {
        const val = params.get(key);
        if (val !== null && val !== '') count++;
    });

    if (count > 0) {
        toggleSidebarView('filtros');
        const badge = document.getElementById('badge-filtros-ter');
        if (badge) {
            badge.innerText = count;
            badge.classList.remove('hidden');
        }
    }
});