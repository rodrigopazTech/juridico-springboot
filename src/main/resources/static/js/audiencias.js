/**
 * Lógica del Módulo de Audiencias
 * Versión v4.0 - Con Asignación Dinámica de Abogados
 */

document.addEventListener('DOMContentLoaded', function () {
    console.log("Sistema de Audiencias Cargado");
});

// --- MENÚS Y NAVEGACIÓN ---
function toggleMenu(id) {
    const menu = document.getElementById('menu-' + id);
    if (!menu) return;

    // Cerramos todos los demás
    document.querySelectorAll('[id^="menu-"]:not([id^="menu-mob-"])').forEach(m => {
        if (m !== menu) m.classList.add('hidden');
    });

    // Alternamos el actual
    menu.classList.toggle('hidden');
}

// Cerrar menús al hacer clic fuera
window.addEventListener('click', function (e) {
    if (!e.target.closest('[id^="menu-"]') && !e.target.closest('[onclick*="toggleMenu"]') && !e.target.closest('[data-audiencia-id]')) {
        document.querySelectorAll('[id^="menu-"]:not([id^="menu-mob-"])').forEach(m => m.classList.add('hidden'));
    }
});

// --- FUNCIONES DEL MODAL (CRUD) ---

// 1. ABRIR PARA CREAR
function abrirModalAudiencia() {
    const form = document.getElementById('form-audiencia');
    if (form) form.reset();

    const inputId = document.getElementById('input-audiencia-id');
    if (inputId) inputId.value = '';

    const observacionesInput = document.getElementById('input-observaciones-aud');
    if (observacionesInput) observacionesInput.value = '';

    const titulo = document.getElementById('modal-titulo-audiencia');
    if (titulo) titulo.innerHTML = '<i class="fas fa-gavel"></i> Nueva Audiencia';

    // Resetear selector de abogados
    const abogadoSelect = document.getElementById('input-abogado');
    if (abogadoSelect) {
        abogadoSelect.innerHTML = '<option value="">Seleccione expediente primero...</option>';
        abogadoSelect.disabled = false;
    }

    toggleUbicacion(false);

    // Resetear radio buttons
    const radioPresencial = document.querySelector('input[name="esVirtual"][value="false"]');
    if (radioPresencial) radioPresencial.checked = true;

    // Ejecutar filtrado de tipos de audiencia vacío
    cargarAbogadosPosibles();

    document.getElementById('modal-audiencia').classList.remove('hidden');
}

// 2. ABRIR PARA EDITAR (¡CORREGIDO!)
function abrirModalEditar(id, expedienteId, tipoId, fecha, hora, isVirtual, sala, link, abogadoId, observaciones = "") {

    // A. Llenar campos básicos
    setValueIfExists('input-audiencia-id', id);
    setValueIfExists('input-expediente', expedienteId);
    setValueIfExists('input-tipo', tipoId);
    setValueIfExists('input-fecha', fecha);
    setValueIfExists('input-hora', hora);
    setValueIfExists('input-observaciones-aud', observaciones);

    // B. Configurar Modalidad (Virtual/Presencial)
    // Convertimos a booleano real por si viene como string "true"
    const esVirtualBool = (String(isVirtual) === 'true');

    const radio = document.querySelector(`input[name="esVirtual"][value="${esVirtualBool}"]`);
    if (radio) radio.checked = true;

    toggleUbicacion(esVirtualBool);

    if (esVirtualBool) {
        setValueIfExists('input-url', link);
    } else {
        setValueIfExists('input-sala', sala);
    }

    // C. CARGAR Y SELECCIONAR ABOGADO (La parte crítica)
    // Llamamos a la API usando el expediente seleccionado y le pasamos el abogado actual para que lo pre-seleccione
    if (expedienteId) {
        cargarAbogadosPosibles(abogadoId);
    }

    // D. Cambiar Título y Mostrar
    const titulo = document.getElementById('modal-titulo-audiencia');
    if (titulo) titulo.innerHTML = '<i class="fas fa-edit"></i> Editar Audiencia';

    document.getElementById('modal-audiencia').classList.remove('hidden');
}

// Helper para evitar errores si el input no existe
function setValueIfExists(elementId, value) {
    const el = document.getElementById(elementId);
    if (el) el.value = value || '';
}

// --- UTILIDADES ---
function toggleUbicacion(isVirtual) {
    const campoSala = document.getElementById('campo-sala');
    const campoUrl = document.getElementById('campo-url');
    const inputSala = document.getElementById('input-sala');
    const inputUrl = document.getElementById('input-url');

    // Convertir a booleano por seguridad
    const showVirtual = (String(isVirtual) === 'true');

    if (showVirtual) {
        if (campoSala) campoSala.classList.add('hidden');
        if (campoUrl) campoUrl.classList.remove('hidden');
        if (inputUrl) inputUrl.setAttribute('required', 'required');
        if (inputSala) {
            inputSala.removeAttribute('required');
            inputSala.value = ''; // Limpiar Sala si es Virtual
        }
    } else {
        if (campoSala) campoSala.classList.remove('hidden');
        if (campoUrl) campoUrl.classList.add('hidden');
        if (inputSala) inputSala.setAttribute('required', 'required');
        if (inputUrl) {
            inputUrl.removeAttribute('required');
            inputUrl.value = ''; // Limpiar URL si es Presencial
        }
    }
}

function activarSubidaActa(id) {
    const inputId = document.getElementById('input-acta-id');
    const inputFile = document.getElementById('input-acta-file');
    if (inputId && inputFile) {
        inputId.value = id;
        inputFile.click();
    }
}

// ROD-186: Configuración de Notificaciones Estilo Proyecto
const Toast = Swal.mixin({
    toast: true,
    position: 'bottom-end',
    showConfirmButton: false,
    timer: 3000,
    //timerProgressBar: true,
    didOpen: (toast) => {
        //toast.addEventListener('mouseenter', Swal.stopTimer)
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
        customClass: {
            title: 'text-green-800 text-sm font-bold'
        }
    });
}

// ROD-186: Filtrar por estatus desde las tarjetas KPI
function filtrarPorEstatus(estatus) {
    const formFiltros = document.getElementById('form-filtros-aud');
    if (!formFiltros) return;

    const selectEstatus = formFiltros.querySelector('select[name="estatus"]');
    if (selectEstatus) {
        selectEstatus.value = estatus;
        actualizarTablaAsincrono();
    }
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

// ROD-186: Listener para subida de acta y conclusión vía AJAX (Preserva filtros)
document.addEventListener('submit', function (e) {
    // A. Concluir Audiencia
    const formConcluir = e.target.closest('#form-concluir');
    if (formConcluir) {
        e.preventDefault();
        const formData = new FormData(formConcluir);

        Swal.fire({
            title: 'Procesando...',
            allowOutsideClick: false,
            didOpen: () => { Swal.showLoading(); }
        });

        fetch(formConcluir.action, {
            method: 'POST',
            body: formData,
            headers: { 'X-Requested-With': 'XMLHttpRequest' }
        })
            .then(response => {
                if (response.ok) {
                    document.getElementById('modal-concluir').classList.add('hidden');
                    showSuccessToast('¡Audiencia Concluida correctamente!');
                    actualizarTablaAsincrono();
                } else {
                    throw new Error('Error al concluir');
                }
            })
            .catch(error => {
                showErrorModal('No se pudo completar la acción.');
            });
    }

    // B. Guardar/Editar Audiencia
    const formAudiencia = e.target.closest('#form-audiencia');
    if (formAudiencia) {
        e.preventDefault();
        const formData = new FormData(formAudiencia);

        Swal.fire({
            title: 'Guardando...',
            allowOutsideClick: false,
            didOpen: () => { Swal.showLoading(); }
        });

        fetch(formAudiencia.action, {
            method: 'POST',
            body: formData,
            headers: { 'X-Requested-With': 'XMLHttpRequest' }
        })
            .then(response => {
                if (response.ok) {
                    document.getElementById('modal-audiencia').classList.add('hidden');
                    showSuccessToast('¡Datos guardados correctamente!');
                    actualizarTablaAsincrono();
                } else {
                    throw new Error('Error al guardar');
                }
            })
            .catch(error => {
                showErrorModal('No se pudo guardar la audiencia.');
            });
    }
});

document.addEventListener('change', function (e) {
    if (e.target && e.target.id === 'input-acta-file') {
        const form = document.getElementById('form-subir-acta');
        if (!form) return;

        const formData = new FormData(form);

        // Mostrar indicador de carga
        Swal.fire({
            title: 'Subiendo acta...',
            allowOutsideClick: false,
            didOpen: () => { Swal.showLoading(); }
        });

        fetch(form.action, {
            method: 'POST',
            body: formData,
            headers: { 'X-Requested-With': 'XMLHttpRequest' }
        })
            .then(response => {
                if (response.ok) {
                    showSuccessToast('¡Acta subida correctamente!');
                    actualizarTablaAsincrono();
                } else {
                    throw new Error('Error en la subida');
                }
            })
            .catch(error => {
                showErrorModal('No se pudo subir el archivo. Intente de nuevo.');
            });
    }
});

function abrirModalConcluir(id) {
    const inputId = document.getElementById('concluir-audiencia-id');
    if (inputId) inputId.value = id;

    document.getElementById('modal-concluir').classList.remove('hidden');
}

// ==========================================
// ⚖️ LÓGICA DE ASIGNACIÓN DE ABOGADOS
// ==========================================

function cargarAbogadosPosibles(idPreseleccionado = null) {
    const expSelect = document.getElementById('input-expediente');
    const abogadoSelect = document.getElementById('input-abogado');

    if (!expSelect || !abogadoSelect) return;

    const expId = expSelect.value;

    // Filtrar Tipos de Audiencia por Gerencia
    const tipoSelect = document.getElementById('input-tipo');
    if (tipoSelect) {
        const selectedOption = expSelect.options[expSelect.selectedIndex];
        const gerenciaId = selectedOption ? selectedOption.getAttribute('data-gerencia-id') : null;

        Array.from(tipoSelect.options).forEach(opt => {
            if (opt.value === "") return; // Option disabled selected
            const optGerenciaId = opt.getAttribute('data-gerencia-id');

            // Si no hay gerencia seleccionada (no hay expediente), ocultar todos
            if (!gerenciaId) {
                opt.style.display = 'none';
                opt.hidden = true;
            }
            // Si hay gerenciaId, mostrar solo los que tengan la misma
            else if (!optGerenciaId || optGerenciaId === gerenciaId) {
                opt.style.display = '';
                opt.hidden = false;
            } else {
                opt.style.display = 'none';
                opt.hidden = true;
            }
        });

        // Si el tipo actual está oculto, resetear
        const currentTipo = tipoSelect.options[tipoSelect.selectedIndex];
        if (currentTipo && currentTipo.hidden) {
            tipoSelect.value = '';
        }
    }

    // Filtrar Organos Jurisdiccionales (input-sala) por Gerencia
    const salaSelect = document.getElementById('input-sala');
    if (salaSelect && salaSelect.tagName === 'SELECT') {
        const selectedOption = expSelect.options[expSelect.selectedIndex];
        const gerenciaId = selectedOption ? selectedOption.getAttribute('data-gerencia-id') : null;

        Array.from(salaSelect.options).forEach(opt => {
            if (opt.value === "") return; // Option disabled selected
            const optGerenciaId = opt.getAttribute('data-gerencia-id');

            // Si no hay gerencia seleccionada, ocultar
            if (!gerenciaId) {
                opt.style.display = 'none';
                opt.hidden = true;
            }
            // Si hay gerenciaId, mostrar solo los que tengan la misma
            else if (!optGerenciaId || optGerenciaId === gerenciaId) {
                opt.style.display = '';
                opt.hidden = false;
            } else {
                opt.style.display = 'none';
                opt.hidden = true;
            }
        });

        // Resetear si la opción seleccionada quedó oculta
        const currentSala = salaSelect.options[salaSelect.selectedIndex];
        if (currentSala && currentSala.hidden) {
            salaSelect.value = '';
        }
    }

    if (!expId) {
        abogadoSelect.innerHTML = '<option value="">Seleccione expediente primero...</option>';
        return;
    }

    // Feedback de carga
    abogadoSelect.innerHTML = '<option>Buscando abogados disponibles...</option>';
    // No lo deshabilitamos totalmente en edición para no bloquear la UI, pero visualmente ayuda
    // abogadoSelect.disabled = true; 

    // Obtener ID del usuario actual de un campo oculto en el modal
    const currentUserIdEl = document.getElementById('current-user-id');
    const currentUserId = currentUserIdEl ? currentUserIdEl.value : null;

    fetch(`/audiencias/api/abogados-disponibles/${expId}`)
        .then(response => response.json())
        .then(abogados => {
            abogadoSelect.innerHTML = '';

            if (abogados.length === 0) {
                abogadoSelect.innerHTML = '<option value="">No hay abogados en esta gerencia</option>';
                return;
            }

            // Agregar opción por defecto (Opcional)
            const defaultOption = document.createElement('option');
            defaultOption.value = "";
            defaultOption.text = "-- Seleccione quien asistirá (Opcional) --";
            abogadoSelect.appendChild(defaultOption);

            let titularEncontrado = false;

            abogados.forEach(a => {
                const option = document.createElement('option');
                option.value = a.id;

                // Distinguir visualmente al titular
                if (a.esTitular) {
                    option.text = `⭐ ${a.nombre} (Titular)`;
                    option.style.fontWeight = 'bold';

                    // LÓGICA DE AUTO-SELECCIÓN (ROD-146 Mejorado):
                    // Si estamos creando (idPreseleccionado es null), sugerimos siempre al Titular
                    if (!idPreseleccionado && a.esTitular) {
                        option.selected = true;
                    }
                    titularEncontrado = true;
                } else {
                    option.text = a.nombre;
                }

                // LÓGICA DE EDICIÓN: Si el ID coincide con el que ya tenía la audiencia
                if (idPreseleccionado && String(a.id) === String(idPreseleccionado)) {
                    option.selected = true;
                }

                abogadoSelect.appendChild(option);
            });

            // Si no se auto-seleccionó nada (porque no es el titular o estamos editando otro), 
            // y no teníamos un idPreseleccionado, queda en "Seleccione..."
            if (!idPreseleccionado && !abogadoSelect.value) {
                abogadoSelect.value = "";
            }
        })
        .catch(err => {
            console.error(err);
            abogadoSelect.innerHTML = '<option value="">Error al cargar abogados</option>';
        });
}

// 4. CONFIRMAR ELIMINAR AUDIENCIA
function confirmarEliminarAudiencia(id) {
    Swal.fire({
        title: '¿Eliminar audiencia?',
        text: "Esta acción no se puede deshacer",
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#d33',
        cancelButtonColor: '#3085d6',
        confirmButtonText: 'Sí, eliminar',
        cancelButtonText: 'Cancelar'
    }).then((result) => {
        if (result.isConfirmed) {
            fetch('/audiencias/eliminar/' + id, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                }
            })
                .then(response => response.json())
                .then(data => {
                    if (data.success) {
                        // Éxito: Usamos un toast
                        const Toast = Swal.mixin({
                            toast: true,
                            position: 'bottom-end',
                            showConfirmButton: false,
                            timer: 2000,
                            //timerProgressBar: true
                        });

                        Toast.fire({
                            icon: 'success',
                            title: 'Audiencia eliminada',
                            iconColor: '#22c55e',
                            background: '#f0fdf4',
                            customClass: {
                                title: 'text-green-800 text-sm font-bold'
                            }
                        }).then(() => {
                            // ROD-186: En lugar de reload, usamos actualización asíncrona para preservar filtros
                            actualizarTablaAsincrono();
                        });
                    } else {
                        Swal.fire('Error', data.message || 'Error al eliminar la audiencia.', 'error');
                    }
                })
                .catch(error => {
                    Swal.fire('Error', 'Hubo un problema de conexión.', 'error');
                });
        }
    });
}

// ==========================================
// 📅 LÓGICA DEL MINI-CALENDARIO
// ==========================================

const miniCalendarState = {
    currentDate: new Date(),
    selectedDate: new Date(),
    isDateFilterActive: false, // Nueva bandera para saber si filtrar por fecha o ver todo (ROD-185)
    names: ["Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"],
    events: [] // Para los puntos indicadores (opcional por ahora)
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
            const searchInput = document.getElementById('input-search-audiencias');
            if (searchInput) searchInput.value = '';
            renderMiniCalendar();
            miniCalendarState.isDateFilterActive = true; // Activar filtro al navegar meses
            actualizarTablaAsincrono();
        };
        nextBtn.onclick = () => {
            const date = miniCalendarState.currentDate;
            date.setMonth(date.getMonth() + 1);
            const searchInput = document.getElementById('input-search-audiencias');
            if (searchInput) searchInput.value = '';
            renderMiniCalendar();
            miniCalendarState.isDateFilterActive = true; // Activar filtro al navegar meses
            actualizarTablaAsincrono();
        };
    }

    renderMiniCalendar();
}

/**
 * Función centralizada para disparar la actualización de la tabla 
 * capturando TODOS los parámetros (Buscador, Mini-Calendario, Filtros Side)
 */
// --- BÚSQUEDA ASÍNCRONA CENTRALIZADA ---
let debounceTimerAudiencias;
let abortControllerAudiencias;

function actualizarTablaAsincrono(keywordForzado = null) {
    const input = document.getElementById('input-search-audiencias');
    const formSearch = document.getElementById('form-search-audiencias');
    const gridContext = document.getElementById('grid-master-audiencias') || document.getElementById('grid-maestro-audiencias');

    if (!gridContext) return;

    // Abortar petición previa si existe
    if (abortControllerAudiencias) {
        abortControllerAudiencias.abort();
    }
    abortControllerAudiencias = new AbortController();

    const urlParams = new URLSearchParams();

    // Preservar ordenamiento actual de la barra de direcciones
    const currentParams = new URLSearchParams(window.location.search);
    const sortBy = currentParams.get('sortBy');
    const sortDir = currentParams.get('sortDir');
    if (sortBy) urlParams.set('sortBy', sortBy);
    if (sortDir) urlParams.set('sortDir', sortDir);

    // 1. Keyword (Prioridad)
    const valKeyword = keywordForzado !== null ? keywordForzado : (input ? input.value : '');
    if (valKeyword) urlParams.set('keyword', valKeyword);

    // 2. Mes y Año del Calendario (Solo si el filtro de fecha está activo)
    if (miniCalendarState.isDateFilterActive) {
        const date = miniCalendarState.currentDate;
        urlParams.set('mes', date.getMonth() + 1);
        urlParams.set('anio', date.getFullYear());
    }

    // 3. Filtros del Lateral
    const formFiltros = document.getElementById('form-filtros-aud');
    let filterCount = 0;
    if (formFiltros) {
        const fd = new FormData(formFiltros);
        for (const [key, value] of fd.entries()) {
            if (key === 'keyword' || key === 'mes' || key === 'anio') continue;
            if (value && value !== 'null' && value !== '') {
                urlParams.set(key, value);
                filterCount++;
            }
        }
    }

    // 4. Actualizar Indicador Visual (Badge con Conteo)
    const badge = document.getElementById('badge-filtros-aud');
    if (badge) {
        if (filterCount > 0) {
            badge.innerText = filterCount;
            badge.classList.remove('hidden');
        } else {
            badge.classList.add('hidden');
        }
    }

    // 5. Resetear página
    urlParams.set('page', 0);

    // 6. Sincronizar URL en la barra de direcciones
    const newUrl = window.location.pathname + '?' + urlParams.toString();
    window.history.replaceState({ path: newUrl }, '', newUrl);

    // 7. Fetch con AbortController
    return fetch('/audiencias?' + urlParams.toString(), {
        headers: { 'X-Requested-With': 'XMLHttpRequest' },
        signal: abortControllerAudiencias.signal
    })
        .then(res => res.text())
        .then(html => {
            const parser = new DOMParser();
            const doc = parser.parseFromString(html, 'text/html');

            // 1. ACTUALIZAR KPIs
            const kpisNuevos = doc.getElementById('contenedor-kpis-audiencias');
            const kpisActuales = document.getElementById('contenedor-kpis-audiencias');
            if (kpisNuevos && kpisActuales) {
                kpisActuales.innerHTML = kpisNuevos.innerHTML;
            }

            // 2. ACTUALIZAR TABLA DE RESULTADOS
            const tablaNueva = doc.getElementById('grid-resultados-audiencias');
            const tablaActual = document.getElementById('grid-resultados-audiencias');
            if (tablaNueva && tablaActual) {
                tablaActual.innerHTML = tablaNueva.innerHTML;
            }

            // 3. ACTUALIZAR ACCIONES (VerTodos y Exportar)
            const accionesNuevas = doc.getElementById('acciones-barra-audiencias');
            const accionesActuales = document.getElementById('acciones-barra-audiencias');
            if (accionesNuevas && accionesActuales) {
                accionesActuales.innerHTML = accionesNuevas.innerHTML;
            }

            // 4. SINCRONIZAR BARRA DE BÚSQUEDA (Solo si es keyword forzado de calendario/KPIs)
            const searchInput = document.getElementById('input-search-audiencias');
            if (keywordForzado !== null && searchInput) {
                searchInput.value = keywordForzado;
                const btnClear = document.getElementById('btn-clear-search');
                if (keywordForzado) btnClear?.classList.remove('hidden');
                else btnClear?.classList.add('hidden');
            }
        })
        .catch(err => {
            if (err.name === 'AbortError') return; // Ignorar si fue abortada
            console.error("Error en actualización asíncrona:", err);
        });
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
        // 1. Limpiar fecha seleccionada en el estado visual
        miniCalendarState.selectedDate = new Date(0);
        // 2. Limpiar el buscador si tiene una fecha
        const searchInput = document.getElementById('input-search-audiencias');
        if (searchInput) {
            searchInput.value = '';
            const btnClear = document.getElementById('btn-clear-search');
            if (btnClear) btnClear.classList.add('hidden');
        }
        miniCalendarState.isDateFilterActive = true; // ACTIVAR filtro de fecha
        renderMiniCalendar();
        actualizarTablaAsincrono();
    };


    const firstDay = new Date(year, month, 1).getDay();
    const daysInMonth = new Date(year, month + 1, 0).getDate();

    const today = new Date();
    // Celdas vacías
    for (let i = 0; i < firstDay; i++) {
        const emptyCell = document.createElement('div');
        emptyCell.className = 'mini-day-cell mini-empty';
        grid.appendChild(emptyCell);
    }

    // Días del mes
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
            // Al hacer clic en un día, forzamos el keyword con la fecha YYYY-MM-DD
            const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;

            // Actualizar estado visual
            miniCalendarState.selectedDate = new Date(year, month, day);
            miniCalendarState.isDateFilterActive = true; // ACTIVAR filtro de fecha
            renderMiniCalendar();

            // Actualizar tabla con el keyword de fecha
            actualizarTablaAsincrono(dateStr);
        };

        grid.appendChild(cell);
    }
}

function initSearchBar() {
    // Implementación asíncrona vía delegación global en document
}

// Iniciar componentes al cargar
document.addEventListener('DOMContentLoaded', () => {
    initMiniCalendar();
    initSearchBar();

    // D. Delegación Global para Interacción (Paginación, Búsqueda, Limpieza)
    document.addEventListener('click', function (e) {
        // 1. Paginación
        const linkPag = e.target.closest('.pagina-link-aud');
        if (linkPag) {
            e.preventDefault();
            const url = linkPag.getAttribute('href');
            fetch(url, { headers: { 'X-Requested-With': 'XMLHttpRequest' } })
                .then(res => res.text())
                .then(html => {
                    const parser = new DOMParser();
                    const doc = parser.parseFromString(html, 'text/html');
                    const nuevoMaestro = doc.getElementById('grid-maestro-audiencias');
                    const maestroDestino = document.getElementById('grid-maestro-audiencias');
                    if (nuevoMaestro && maestroDestino) {
                        maestroDestino.innerHTML = nuevoMaestro.innerHTML;
                    }
                    window.history.replaceState({ path: url }, '', url);
                })
                .catch(err => console.error("Error en paginación asíncrona:", err));
            return;
        }

        // 2. Botón Limpiar Búsqueda (Barra Superior)
        const btnClear = e.target.closest('#btn-clear-search');
        if (btnClear) {
            const input = document.getElementById('input-search-audiencias');
            if (input) input.value = '';
            actualizarTablaAsincrono();
            return;
        }

        // 3. Botón Limpiar Filtros (Sidebar)
        const btnLimpiar = e.target.closest('#btn-limpiar-filtros-aud');
        if (btnLimpiar) {
            const form = document.getElementById('form-filtros-aud');
            if (form) {
                form.querySelectorAll('select').forEach(s => s.value = '');
            }
            const input = document.getElementById('input-search-audiencias');
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
        if (e.target.id === 'input-search-audiencias') {
            const input = e.target;
            const btnClear = document.getElementById('btn-clear-search');
            if (input.value) {
                btnClear?.classList.remove('hidden');
            } else {
                btnClear?.classList.add('hidden');
            }

            clearTimeout(debounceTimerAudiencias);
            debounceTimerAudiencias = setTimeout(() => {
                actualizarTablaAsincrono();
            }, 650);
        }
    });

    // F. Submit Form (Delegación Global)
    document.addEventListener('submit', function (e) {
        if (e.target.id === 'form-search-audiencias') {
            e.preventDefault();
            actualizarTablaAsincrono();
        }
    });

    // G. Filtros del sidebar (Delegación Global)
    document.addEventListener('change', function (e) {
        if (e.target.closest('#form-filtros-aud')) {
            actualizarTablaAsincrono();
        }
    });
});

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
    const filterKeys = ['tipo', 'gerencia', 'materia', 'estatus', 'abogadoId', 'periodo'];

    let count = 0;
    filterKeys.forEach(key => {
        const val = params.get(key);
        if (val !== null && val !== '') count++;
    });

    if (count > 0) {
        toggleSidebarView('filtros');
        const badge = document.getElementById('badge-filtros-aud');
        if (badge) {
            badge.innerText = count;
            badge.classList.remove('hidden');
        }
    }
});


// ==========================================
// 🔔 WEBSOCKET: ACTUALIZACIÓN EN TIEMPO REAL
// ==========================================
function iniciarWebSocketAudiencias() {
    if (typeof SockJS === 'undefined' || typeof Stomp === 'undefined') {
        setTimeout(iniciarWebSocketAudiencias, 1000);
        return;
    }
    const socket = new SockJS('/ws-realtime');
    const stompClient = Stomp.over(socket);
    stompClient.debug = null; // Silenciar logs de debug en consola

    stompClient.connect({}, function (frame) {
        console.log('Conectado a WebSockets para Audiencias en tiempo real.');
        stompClient.subscribe('/topic/alertas', function (message) {
            try {
                const payload = JSON.parse(message.body);
                // Escuchar eventos específicamente para la tabla 'audiencias'
                if (payload.tabla === 'audiencias') {
                    console.log('Cambio detectado en audiencias vía WS:', payload);


                    // Refrescar la tabla y KPIs de forma asíncrona, y LUEGO alumbrar la tarjeta
                    const fetchPromise = actualizarTablaAsincrono();
                    if (fetchPromise && fetchPromise.then) {
                        fetchPromise.then(() => {
                            animarTarjetaKPI(payload.estatus);
                        });
                    } else {
                        // Por si falló o retornó algo nulo
                        setTimeout(() => animarTarjetaKPI(payload.estatus), 800);
                    }
                }
            } catch (e) {
                console.error("Error parseando notificación de websocket", e);
            }
        });
    }, function (error) {
        console.warn('Conexión WS perdida. Reconectando en 5s...');
        setTimeout(iniciarWebSocketAudiencias, 5000);
    });
}

function animarTarjetaKPI(estatus) {
    let cardId = null;
    let colorClase = 'ring-gob-oro';
    let bgClase = 'bg-yellow-50';

    if (estatus === 'PENDIENTE') {
        cardId = 'kpi-card-pendiente';
        colorClase = 'ring-orange-400';
        bgClase = 'bg-orange-50';
    } else if (estatus === 'CON_ACTA') {
        cardId = 'kpi-card-con-acta';
        colorClase = 'ring-blue-400';
        bgClase = 'bg-blue-50';
    } else if (estatus === 'CONCLUIDA') {
        cardId = 'kpi-card-concluida';
        colorClase = 'ring-green-400';
        bgClase = 'bg-green-50';
    } else {
        // Si no hay estatus (ej. un borrado genérico o creación sin estatus explícito), alumbrar el Total
        cardId = 'kpi-card-total';
        colorClase = 'ring-red-400';
        bgClase = 'bg-red-50';
    }

    // Siempre alumbrar la tarjeta Total sutilmente
    const totalCard = document.getElementById('kpi-card-total');
    if (totalCard && cardId !== 'kpi-card-total') {
        totalCard.classList.add('ring-4', 'ring-red-200', 'scale-105', 'shadow-xl');
        setTimeout(() => totalCard.classList.remove('ring-4', 'ring-red-200', 'scale-105', 'shadow-xl'), 1500);
    }

    if (cardId) {
        const card = document.getElementById(cardId);
        if (card) {
            // Aplicar clases de alumbrado (Glow animation)
            card.classList.add('ring-4', colorClase, bgClase, 'scale-110', 'shadow-2xl', 'z-50', 'transition-all', 'duration-300');

            // Quitar el alumbrado después de 1.5s
            setTimeout(() => {
                card.classList.remove('ring-4', colorClase, bgClase, 'scale-110', 'shadow-2xl', 'z-50');
            }, 1500);
        }
    }
}

// Inicializar la conexión WS después de que el DOM esté listo
document.addEventListener('DOMContentLoaded', () => {
    // Retrasar ligeramente la conexión para no competir con el WS del Navbar y asegurar librerías
    setTimeout(iniciarWebSocketAudiencias, 2000);
});
