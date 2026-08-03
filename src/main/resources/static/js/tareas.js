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
