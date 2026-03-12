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
    if (!e.target.closest('td.relative')) {
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

                    // LÓGICA DE AUTO-SELECCIÓN (ROD-146):
                    // Si NO estamos editando (idPreseleccionado es null)
                    // Y el usuario que está creando es el propio Titular
                    if (!idPreseleccionado && String(a.id) === String(currentUserId)) {
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