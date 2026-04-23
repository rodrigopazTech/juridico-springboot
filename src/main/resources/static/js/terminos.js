/**
 * Lógica para el módulo de Términos
 * Autor: Ricardo Villalobos
 */

document.addEventListener('DOMContentLoaded', function () {
    console.log("Sistema de Terminos Cargado");
    initMiniCalendar();
    initSearchTerminos();
});

// --- BÚSQUEDA ASÍNCRONA GLOBAL ---
function initSearchTerminos() {
    const inputSearch = document.getElementById('input-search-terminos');
    const btnClear = document.getElementById('btn-clear-search-terminos');
    const formSearch = document.getElementById('form-search-terminos');
    let debounceTimer;

    if (!inputSearch) return;

    inputSearch.addEventListener('input', function() {
        clearTimeout(debounceTimer);
        
        // Mostrar/ocultar botón X
        if (btnClear) {
            if (this.value.trim() !== "") {
                btnClear.classList.remove('hidden');
            } else {
                btnClear.classList.add('hidden');
            }
        }

        debounceTimer = setTimeout(() => {
            if (!formSearch) return;

            // Preparamos los parámetros
            const urlParams = new URLSearchParams(new FormData(formSearch));
            urlParams.set('page', 0); // Reset a primera página

            // LLAMADA AJAX
            fetch('/terminos?' + urlParams.toString(), {
                headers: { 'X-Requested-With': 'XMLHttpRequest' }
            })
            .then(res => res.text())
            .then(html => {
                const parser = new DOMParser();
                const doc = parser.parseFromString(html, 'text/html');
                const nuevoContenedor = doc.getElementById('tabla-resultados-container');
                const contenedorActual = document.getElementById('tabla-resultados-container');
                
                if (nuevoContenedor && contenedorActual) {
                    contenedorActual.innerHTML = nuevoContenedor.innerHTML;
                }
            })
            .catch(err => console.error("Error en búsqueda asíncrona:", err));

        }, 400); // 400ms de espera para no saturar el servidor
    });

    if (btnClear) {
        btnClear.onclick = () => {
            inputSearch.value = '';
            inputSearch.dispatchEvent(new Event('input', { bubbles: true }));
        };
    }

    // Evitar recarga completa con ENTER
    if (formSearch) {
        formSearch.addEventListener('submit', (e) => e.preventDefault());
    }
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
            window.location.href = '/terminos/eliminar/' + id;
        }
    })
}

// --- MODALES (NUEVO / EDITAR) ---

function abrirModalNuevo() {
    const form = document.getElementById('form-termino');
    if (form) form.reset();

    // Limpiamos el ID oculto para que el Controller sepa que es CREAR
    document.getElementById('input-termino-id').value = '';

    // Cambiamos título
    document.getElementById('modal-titulo').innerHTML = '<i class="fas fa-plus-circle"></i> Nuevo Término';

    // Mostramos modal
    document.getElementById('modal-nuevo-termino').classList.remove('hidden');
}

function editarTermino(id, actuacion, fecha, expedienteId, abogadoId) {
    // 1. Rellenar campos del formulario
    document.getElementById('input-termino-id').value = id;
    document.getElementById('input-actuacion').value = actuacion;
    document.getElementById('input-fecha').value = fecha;
    document.getElementById('input-expediente').value = expedienteId;

    // Manejo seguro del abogado (puede ser null)
    if (abogadoId) {
        document.getElementById('input-abogado').value = abogadoId;
    } else {
        document.getElementById('input-abogado').value = "";
    }

    // 2. Cambiar título a "Editar"
    document.getElementById('modal-titulo').innerHTML = '<i class="fas fa-edit"></i> Editar Término';

    // 3. Mostrar modal
    document.getElementById('modal-nuevo-termino').classList.remove('hidden');
}


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
    if (modal) modal.classList.remove('hidden');
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
    if (modal) modal.classList.remove('hidden');
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
    }
}

// ==========================================
// 📅 LÓGICA DEL MINI-CALENDARIO (TÉRMINOS)
// ==========================================

const miniCalendarState = {
    currentDate: new Date(),
    selectedDate: new Date(),
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

    // Si hay un keyword que es fecha, marcarla como seleccionada
    const urlParams = new URLSearchParams(window.location.search);
    const keyword = urlParams.get('keyword');
    if (keyword && keyword.match(/^\d{4}-\d{2}-\d{2}$/)) {
        const parts = keyword.split('-');
        miniCalendarState.selectedDate = new Date(parseInt(parts[0]), parseInt(parts[1]) - 1, parseInt(parts[2]));
    }

    if (prevBtn && nextBtn) {
        prevBtn.onclick = () => {
            const date = miniCalendarState.currentDate;
            date.setMonth(date.getMonth() - 1);
            window.location.href = `/terminos?mes=${date.getMonth() + 1}&anio=${date.getFullYear()}`;
        };
        nextBtn.onclick = () => {
            const date = miniCalendarState.currentDate;
            date.setMonth(date.getMonth() + 1);
            window.location.href = `/terminos?mes=${date.getMonth() + 1}&anio=${date.getFullYear()}`;
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
            const url = new URL(window.location.href);
            url.searchParams.set('keyword', dateStr);
            url.searchParams.set('mes', month + 1);
            url.searchParams.set('anio', year);
            window.location.href = url.toString();
        };

        grid.appendChild(cell);
    }
}