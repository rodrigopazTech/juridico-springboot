/**
 * Lógica para el módulo de Términos
 * Autor: Ricardo Villalobos
 */

document.addEventListener('DOMContentLoaded', function () {
    console.log("Sistema de Terminos Cargado");
});

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
            fetch('/terminos/eliminar/' + id, {
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
                            title: 'Término eliminado',
                            iconColor: '#22c55e',
                            background: '#f0fdf4',
                            customClass: {
                                title: 'text-green-800 text-sm font-bold'
                            }
                        }).then(() => {
                            location.reload();
                        });
                    } else {
                        Swal.fire('Error', data.message || 'Error al eliminar', 'error');
                    }
                })
                .catch(error => {
                    Swal.fire('Error', 'Hubo un problema de conexión', 'error');
                });
        }
    })
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