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

// --- SUBIDA DE ARCHIVOS (WORD) ---
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

// --- SUBIDA DE ACUSE (MODAL) ---
function activarSubidaAcuse(idTermino) {
    // 1. Asignar el ID al input oculto dentro del modal de acuse
    const inputId = document.getElementById('input-acuse-id');
    
    if(inputId) {
        inputId.value = idTermino;
        
        // 2. Mostrar el modal
        const modal = document.getElementById('modal-subir-acuse');
        if(modal) {
            modal.classList.remove('hidden');
            modal.classList.add('flex'); // Asegurar flex para centrado
        }
    } else {
        console.error("Error: No se encontró el input 'input-acuse-id' en el modal de acuse.");
    }
}

// ==========================================
// 🛠️ GESTIÓN DE MODALES (NUEVO / EDITAR)
// ==========================================

// 1. Abrir Modal NUEVO
function abrirModalNuevo() {
    // Referencias a los campos del nuevo HTML
    const expSelect = document.getElementById('select-expediente-termino');
    const asuntoInput = document.getElementById('input-asunto');
    const vencimientoInput = document.getElementById('input-vencimiento');
    const abogadoSelect = document.getElementById('select-abogado-termino');

    // Limpiar valores
    if(expSelect) expSelect.value = "";
    if(asuntoInput) asuntoInput.value = "";
    if(vencimientoInput) vencimientoInput.value = "";
    
    // Resetear el select de abogado (bloqueado por defecto)
    if(abogadoSelect) {
        abogadoSelect.innerHTML = '<option value="">Seleccione expediente primero...</option>';
        abogadoSelect.classList.add('cursor-not-allowed');
        abogadoSelect.classList.remove('bg-white');
        abogadoSelect.classList.add('bg-gray-50');
    }

    // Cambiar título
    const titulo = document.getElementById('modal-titulo');
    if(titulo) titulo.innerHTML = '<i class="fas fa-plus-circle"></i> Nuevo Término';
    
    // Mostrar modal
    const modal = document.getElementById('modal-nuevo-termino');
    if(modal) modal.classList.remove('hidden');
}

// 2. Abrir Modal EDITAR (Para uso futuro o si ya tienes botones de editar)
function editarTermino(id, actuacion, fecha, expedienteId, abogadoId) {
    // NOTA: Asegúrate de que los IDs del HTML coincidan con los que buscas aquí.
    // Con la actualización reciente, los IDs cambiaron a 'input-asunto', 'select-expediente-termino', etc.
    // Si vas a usar esta función, actualízala para usar los mismos IDs que abrirModalNuevo.
    
    const asuntoInput = document.getElementById('input-asunto');
    const vencimientoInput = document.getElementById('input-vencimiento');
    const expSelect = document.getElementById('select-expediente-termino');
    // ... setear valores ...
    
    const titulo = document.getElementById('modal-titulo');
    if(titulo) titulo.innerHTML = '<i class="fas fa-edit"></i> Editar Término';
    
    const modal = document.getElementById('modal-nuevo-termino');
    if(modal) modal.classList.remove('hidden');
}

// 3. Función de Vinculación Automática (CORREGIDA Y EN EL SCOPE GLOBAL)
function vincularAbogadoTermino() {
    const expSelect = document.getElementById('select-expediente-termino');
    const abogadoSelect = document.getElementById('select-abogado-termino');
    
    if(!expSelect || !abogadoSelect) return;
    
    const expId = expSelect.value;
    if(!expId) return;

    // Feedback visual de carga
    abogadoSelect.innerHTML = '<option>Cargando responsable...</option>';
    abogadoSelect.classList.add('animate-pulse');

    // Llamada a la API que creamos en el Controller
    fetch(`/terminos/api/responsable-expediente/${expId}`)
        .then(response => {
            if (!response.ok) throw new Error("Sin responsable");
            return response.json();
        })
        .then(data => {
            // Limpiamos y dejamos SOLO al responsable
            abogadoSelect.innerHTML = ''; 
            
            const option = document.createElement('option');
            option.value = data.id;
            option.text = data.nombre;
            option.selected = true;
            
            abogadoSelect.appendChild(option);
            
            // Quitar animación y estilos de bloqueo
            abogadoSelect.classList.remove('animate-pulse');
            abogadoSelect.classList.remove('cursor-not-allowed');
            abogadoSelect.classList.remove('bg-gray-50');
            abogadoSelect.classList.add('bg-white');
        })
        .catch(err => {
            console.error(err);
            abogadoSelect.innerHTML = '<option value="">Sin abogado asignado</option>';
        });
}