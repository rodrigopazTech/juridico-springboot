/**
 * Lógica para el módulo de Términos
 * Autor: Ricardo Villalobos
 */

document.addEventListener('DOMContentLoaded', function() {
    // Aquí puedes poner inicializaciones si las necesitas
});

// --- MENÚS DESPLEGABLES ---
function toggleMenu(id) {
    // 1. Cierra todos los otros menús abiertos
    document.querySelectorAll('[id^="menu-"]').forEach(menu => {
        if (menu.id !== 'menu-' + id) {
            menu.classList.add('hidden');
        }
    });

    // 2. Alterna el menú actual
    const menu = document.getElementById('menu-' + id);
    if (menu) {
        menu.classList.toggle('hidden');
    }
}

// Cierra el menú si haces clic fuera de él
window.addEventListener('click', function(e) {
    if (!e.target.closest('td.relative')) {
        document.querySelectorAll('[id^="menu-"]').forEach(menu => {
            menu.classList.add('hidden');
        });
    }
});

// --- SUBIDA DE ARCHIVOS ---
function activarSubida(idTermino) {
    // Vincula el ID al input oculto y abre el selector de archivos
    const inputId = document.getElementById('upload-termino-id');
    const inputFile = document.getElementById('upload-file-input');
    
    if(inputId && inputFile) {
        inputId.value = idTermino;
        inputFile.click();
    } else {
        console.error("No se encontraron los inputs de subida (upload-termino-id o upload-file-input)");
    }
}

// --- ELIMINAR ---
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
    if(form) form.reset();
    
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
    if(abogadoId) {
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
    // 1. Asignar ID al input oculto
    const inputId = document.getElementById('acuse-termino-id');
    const inputFile = document.getElementById('acuse-file-input');
    
    if(inputId && inputFile) {
        inputId.value = idTermino;
        // 2. Abrir selector de archivos
        inputFile.click();
    } else {
        console.error("No se encontraron los inputs de acuse");
    }
}

function activarSubidaAcuse(idTermino) {
    // 1. Asignar el ID al input oculto dentro del modal nuevo
    const inputId = document.getElementById('input-acuse-id');
    
    if(inputId) {
        inputId.value = idTermino;
        
        // 2. Mostrar el modal
        const modal = document.getElementById('modal-subir-acuse');
        if(modal) {
            modal.classList.remove('hidden');
            modal.style.display = 'flex'; // Asegurar display flex para centrado
            modal.classList.add('flex');
            modal.classList.add('items-center');
            modal.classList.add('justify-center');
        }
    } else {
        console.error("Error: No se encontró el input 'input-acuse-id' en el modal de acuse.");
    }
}