/**
 * Lógica para el módulo de Tareas
 */
document.addEventListener('DOMContentLoaded', function () {
    console.log("Sistema de Tareas Cargado");

    const estadoInicial = new URLSearchParams(window.location.search).get('estado');
    if (estadoInicial) {
        const botonInicial = Array.from(document.querySelectorAll('.filtro-btn'))
            .find(btn => btn.getAttribute('onclick')?.includes(`'${estadoInicial}'`));
        if (botonInicial) {
            filtrarTareas(estadoInicial, botonInicial);
        }
    }

    const formTarea = document.getElementById('form-tarea');
    if (formTarea) {
        formTarea.addEventListener('submit', function (e) {
            if (formTarea.dataset.outlookConfirmed) {
                delete formTarea.dataset.outlookConfirmed;
                return;
            }

            const calendarioVinculado = String(document.getElementById('hdnOutlookCalendarLinkedTareas')?.value) === 'true';
            const syncInput = document.getElementById('input-sync-outlook-tarea');

            if (!calendarioVinculado) {
                if (syncInput) syncInput.value = 'false';
                return;
            }

            if (typeof formTarea.checkValidity === 'function' && !formTarea.checkValidity()) {
                return;
            }

            e.preventDefault();
            Swal.fire({
                title: 'Crear evento en Outlook',
                text: 'Elige si quieres guardar la tarea solo en SIAJ o tambien crearla en tu calendario de Outlook.',
                icon: 'question',
                showDenyButton: true,
                showCancelButton: true,
                confirmButtonText: 'Crear en Outlook',
                denyButtonText: 'Solo SIAJ',
                cancelButtonText: 'Cancelar',
                confirmButtonColor: '#2563eb',
                denyButtonColor: '#6b7280',
                cancelButtonColor: '#9ca3af'
            }).then((result) => {
                if (result.isConfirmed || result.isDenied) {
                    if (syncInput) syncInput.value = result.isConfirmed ? 'true' : 'false';
                    formTarea.dataset.outlookConfirmed = 'true';
                    if (typeof formTarea.requestSubmit === 'function') {
                        formTarea.requestSubmit();
                    } else {
                        formTarea.submit();
                    }
                }
            });
        });
    }
});

// Función para completar tarea
function toggleTarea(id) {
    window.location.href = `/tareas/completar/${id}`;
}

// Confirmación para eliminar
function confirmarEliminarTarea(id) {
    Swal.fire({
        title: '¿Eliminar tarea?',
        text: "Esta acción no se puede deshacer.",
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#bc955c', // Oro Gob
        cancelButtonColor: '#691c32',  // Guinda Gob
        confirmButtonText: 'Sí, eliminar',
        cancelButtonText: 'Cancelar'
    }).then((result) => {
        if (result.isConfirmed) {
            window.location.href = `/tareas/eliminar/${id}`;
        }
    });
} // <-- Faltaba cerrar esta llave correctamente antes de iniciar la otra función

// Filtro interactivo para la tabla de tareas (AHORA SÍ ESTÁ GLOBAL)
function filtrarTareas(estado, btnElement) {
    // 1. Quitar los estilos activos de todos los botones
    const botones = document.querySelectorAll('.filtro-btn');
    botones.forEach(btn => {
        btn.classList.remove('bg-white', 'shadow-sm', 'text-gob-guinda');
        btn.classList.add('hover:text-gob-guinda');
    });

    // 2. Aplicar estilo activo al botón clickeado
    btnElement.classList.add('bg-white', 'shadow-sm', 'text-gob-guinda');
    btnElement.classList.remove('hover:text-gob-guinda');

    // 3. Mostrar/Ocultar las filas según su data-estado
    const filas = document.querySelectorAll('.fila-tarea');
    filas.forEach(fila => {
        if (estado === 'todas') {
            fila.style.display = ''; // Muestra todas
        } else {
            // Compara el estado requerido con el atributo de la fila
            if (fila.getAttribute('data-estado') === estado) {
                fila.style.display = '';
            } else {
                fila.style.display = 'none';
            }
        }
    });
}

function confirmarEliminarTarea(id, outlookLinked = false) {
    if (outlookLinked === true || String(outlookLinked) === 'true') {
        Swal.fire({
            title: 'Eliminar tarea vinculada',
            text: 'Esta tarea tiene un evento en Outlook. Elige que deseas eliminar.',
            icon: 'warning',
            showDenyButton: true,
            showCancelButton: true,
            confirmButtonColor: '#bc955c',
            denyButtonColor: '#6b7280',
            cancelButtonColor: '#691c32',
            confirmButtonText: 'Eliminar SIAJ y Outlook',
            denyButtonText: 'Eliminar solo SIAJ',
            cancelButtonText: 'Cancelar'
        }).then((result) => {
            if (result.isConfirmed || result.isDenied) {
                window.location.href = `/tareas/eliminar/${id}?deleteOutlook=${result.isConfirmed}`;
            }
        });
        return;
    }

    Swal.fire({
        title: 'Eliminar tarea',
        text: 'Esta accion no se puede deshacer.',
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#bc955c',
        cancelButtonColor: '#691c32',
        confirmButtonText: 'Si, eliminar',
        cancelButtonText: 'Cancelar'
    }).then((result) => {
        if (result.isConfirmed) {
            window.location.href = `/tareas/eliminar/${id}`;
        }
    });
}
