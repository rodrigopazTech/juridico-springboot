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
    document.querySelectorAll('[id^="menu-"]').forEach(m => {
        if (m !== menu) m.classList.add('hidden');
    });

    // Alternamos el actual
    menu.classList.toggle('hidden');
}

// Cerrar menús al hacer clic fuera
window.addEventListener('click', function (e) {
    if (!e.target.closest('[id^="menu-"]') && !e.target.closest('[onclick*="toggleMenu"]') && !e.target.closest('[data-audiencia-id]')) {
        document.querySelectorAll('[id^="menu-"]').forEach(m => m.classList.add('hidden'));
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
        if (inputSala) inputSala.removeAttribute('required');
    } else {
        if (campoSala) campoSala.classList.remove('hidden');
        if (campoUrl) campoUrl.classList.add('hidden');
        if (inputSala) inputSala.setAttribute('required', 'required');
        if (inputUrl) inputUrl.removeAttribute('required');
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
                            position: 'top-end',
                            showConfirmButton: false,
                            timer: 2000,
                            timerProgressBar: true
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
                            location.reload();
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
            window.location.href = `/audiencias?mes=${date.getMonth() + 1}&anio=${date.getFullYear()}`;
        };
        nextBtn.onclick = () => {
            const date = miniCalendarState.currentDate;
            date.setMonth(date.getMonth() + 1);
            window.location.href = `/audiencias?mes=${date.getMonth() + 1}&anio=${date.getFullYear()}`;
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
            // También mantenemos el mes/anio para que el calendario no brinque
            url.searchParams.set('mes', month + 1);
            url.searchParams.set('anio', year);
            window.location.href = url.toString();
        };

        grid.appendChild(cell);
    }
}

// Iniciar componentes al cargar
document.addEventListener('DOMContentLoaded', () => {
    initMiniCalendar();
    initSearchBar();
});

function initSearchBar() {
    const input = document.getElementById('input-search-audiencias');
    const btnClear = document.getElementById('btn-clear-search');
    const form = document.getElementById('form-search-audiencias');

    if (!input || !form) return;

    // A. Mostrar/Ocultar botón X según el contenido
    input.addEventListener('input', function () {
        if (btnClear) {
            if (this.value.trim() !== "") {
                btnClear.classList.remove('hidden');
            } else {
                btnClear.classList.add('hidden');
            }
        }
    });

    // B. Manejar tecla ENTER (Búsqueda Servidor)
    input.addEventListener('keypress', function (e) {
        if (e.key === 'Enter') {
            e.preventDefault();
            form.submit();
        }
    });

    // C. Manejar Botón Limpiar (X)
    if (btnClear) {
        btnClear.onclick = () => {
            input.value = '';
            // Al limpiar, quitamos también los parámetros de mes/anio si queremos volver a "Hoy"
            // o simplemente enviamos el form para quitar el keyword.
            // Para volver al "Estado Original", lo mejor es redirigir a la URL base de audiencias.
            window.location.href = '/audiencias';
        };
    }

    // D. Detección de "Borrado Total" por teclado
    input.addEventListener('keyup', function (e) {
        if (this.value.trim() === "" && (e.key === 'Backspace' || e.key === 'Delete')) {
            // Si el usuario borra todo y ya había una búsqueda (keyword en URL), recargamos
            const urlParams = new URLSearchParams(window.location.search);
            if (urlParams.has('keyword')) {
                window.location.href = '/audiencias';
            }
        }
    });
}
