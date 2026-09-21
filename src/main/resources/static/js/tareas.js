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

    const fechaInput = document.getElementById('fechaRecordatorio');
    const horaInput = document.getElementById('horaRecordatorio');
    if (fechaInput) {
        fechaInput.min = formatearFechaInput(new Date());
        fechaInput.addEventListener('change', actualizarResumenRecordatorio);
    }
    if (horaInput) {
        horaInput.addEventListener('input', actualizarResumenRecordatorio);
    }

});

function abrirModalTarea() {
    const form = document.getElementById('form-tarea');
    if (!form) return;

    form.reset();
    form.action = '/tareas/guardar';
    delete form.dataset.notificadoEn;
    delete form.dataset.fechaOriginal;
    delete form.dataset.horaOriginal;
    document.getElementById('modal-tarea-titulo').textContent = 'Nueva Tarea';
    document.getElementById('btn-guardar-tarea').textContent = 'Crear Tarea';
    document.getElementById('resumen-recordatorio').textContent = 'Selecciona fecha y hora para calcular el recordatorio.';
    document.getElementById('modal-recordatorio').classList.remove('hidden');
    setTimeout(() => document.getElementById('titulo')?.focus(), 50);
}

function abrirModalEditarTarea(button) {
    const form = document.getElementById('form-tarea');
    if (!form || !button) return;

    form.reset();
    form.action = `/tareas/editar/${encodeURIComponent(button.dataset.id)}`;
    document.getElementById('modal-tarea-titulo').textContent = 'Editar Tarea';
    document.getElementById('btn-guardar-tarea').textContent = 'Guardar Cambios';
    document.getElementById('titulo').value = button.dataset.titulo || '';
    document.getElementById('detalles').value = button.dataset.detalles || '';
    document.getElementById('fechaRecordatorio').value = button.dataset.fecha || '';
    document.getElementById('horaRecordatorio').value = normalizarHoraInput(button.dataset.hora);
    document.getElementById('prioridadId').value = button.dataset.prioridadId || '';
    form.dataset.notificadoEn = button.dataset.notificadoEn || '';
    form.dataset.fechaOriginal = button.dataset.fechaOriginal || '';
    form.dataset.horaOriginal = normalizarHoraInput(button.dataset.horaOriginal || '');
    actualizarResumenRecordatorio();
    document.getElementById('modal-recordatorio').classList.remove('hidden');
    setTimeout(() => document.getElementById('titulo')?.focus(), 50);
}

function cerrarModalTarea() {
    document.getElementById('modal-recordatorio')?.classList.add('hidden');
}

function abrirModalRecordatorio() {
    abrirModalTarea();
}

function cerrarModalRecordatorio() {
    cerrarModalTarea();
}

function actualizarResumenRecordatorio() {
    const form = document.getElementById('form-tarea');
    const fecha = document.getElementById('fechaRecordatorio')?.value;
    const hora = document.getElementById('horaRecordatorio')?.value;
    const resumen = document.getElementById('resumen-recordatorio');
    if (!resumen) return;

    if (!fecha || !hora) {
        resumen.textContent = 'Selecciona fecha y hora para calcular el recordatorio.';
        return;
    }

    const [anio, mes, dia] = fecha.split('-').map(Number);
    const [horas, minutos] = hora.split(':').map(Number);
    const programada = new Date(anio, mes - 1, dia, horas, minutos);
    const aviso = new Date(anio, mes - 1, dia, horas, minutos);
    aviso.setMinutes(aviso.getMinutes() - 15);

    const ahora = new Date();
    const programacionSinCambios = form?.dataset.notificadoEn
        && form.dataset.fechaOriginal === fecha
        && form.dataset.horaOriginal === hora.substring(0, 5);
    if (programacionSinCambios) {
        const notificadoEn = new Date(form.dataset.notificadoEn);
        resumen.textContent = `Esta tarea ya fue notificada el ${notificadoEn.toLocaleDateString('es-MX')} a las ${notificadoEn.toLocaleTimeString('es-MX', {
            hour: '2-digit',
            minute: '2-digit'
        })}.`;
        return;
    }
    if (programada <= ahora) {
        resumen.textContent = 'Selecciona una fecha y hora futuras para programar el aviso.';
        return;
    }
    if (aviso <= ahora && programada > ahora) {
        resumen.textContent = 'Como faltan menos de 15 minutos, te avisaremos en breve.';
        return;
    }

    resumen.textContent = `Te avisaremos el ${aviso.toLocaleDateString('es-MX')} a las ${aviso.toLocaleTimeString('es-MX', {
        hour: '2-digit',
        minute: '2-digit'
    })}.`;
}

function normalizarHoraInput(hora) {
    if (!hora) return '';
    return hora.substring(0, 5);
}

function formatearFechaInput(fecha) {
    const anio = fecha.getFullYear();
    const mes = String(fecha.getMonth() + 1).padStart(2, '0');
    const dia = String(fecha.getDate()).padStart(2, '0');
    return `${anio}-${mes}-${dia}`;
}

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
