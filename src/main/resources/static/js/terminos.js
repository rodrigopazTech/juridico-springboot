/**
 * Lógica para el módulo de Términos
 * Autor: Ricardo Villalobos
 */

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
window.addEventListener('click', function(e) {
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
    
    if(inputId && inputFile) {
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

function activarSubidaAcuse(idTermino) {
    const inputId = document.getElementById('input-acuse-id');
    if(inputId) {
        inputId.value = idTermino;
        const modal = document.getElementById('modal-subir-acuse');
        if(modal) {
            modal.classList.remove('hidden');
            modal.classList.add('flex');
        }
    }
}

// --- GESTIÓN DE MODALES ---

// 1. ABRIR MODAL NUEVO (Limpio)
function abrirModalNuevo() {
    const idInput = document.getElementById('input-termino-id');
    const expSelect = document.getElementById('select-expediente-termino');
    const asuntoInput = document.getElementById('input-asunto');
    const vencimientoInput = document.getElementById('input-vencimiento');
    const abogadoSelect = document.getElementById('select-abogado-termino');
    const titulo = document.getElementById('modal-titulo');

    // Limpiar campos
    if(idInput) idInput.value = ""; 
    if(expSelect) expSelect.value = "";
    if(asuntoInput) asuntoInput.value = "";
    if(vencimientoInput) vencimientoInput.value = "";
    
    // Resetear abogado
    if(abogadoSelect) {
        abogadoSelect.innerHTML = '<option value="">Seleccione expediente primero...</option>';
        abogadoSelect.classList.add('cursor-not-allowed');
        abogadoSelect.classList.add('bg-gray-50');
    }

    if(titulo) titulo.innerHTML = '<i class="fas fa-plus-circle"></i> Nuevo Término';
    
    const modal = document.getElementById('modal-nuevo-termino');
    if(modal) modal.classList.remove('hidden');
}

// 2. EDITAR TÉRMINO (Con el botón amarillo)
function editarTermino(id, actuacion, fecha, expedienteId, abogadoId) {
    
    // Llenar datos en el formulario
    const idInput = document.getElementById('input-termino-id');
    const asuntoInput = document.getElementById('input-asunto');
    const vencimientoInput = document.getElementById('input-vencimiento');
    const expSelect = document.getElementById('select-expediente-termino');
    const titulo = document.getElementById('modal-titulo');

    if (idInput) idInput.value = id;
    if (asuntoInput) asuntoInput.value = actuacion;
    if (vencimientoInput) vencimientoInput.value = fecha;

    if (expSelect && expedienteId) {
        expSelect.value = expedienteId;
        vincularAbogadoTermino(); // Cargar abogado automáticamente
    }

    if(titulo) titulo.innerHTML = '<i class="fas fa-edit"></i> Editar Término';
    
    const modal = document.getElementById('modal-nuevo-termino');
    if(modal) modal.classList.remove('hidden');
}

// 3. VINCULACIÓN AUTOMÁTICA
function vincularAbogadoTermino() {
    const expSelect = document.getElementById('select-expediente-termino');
    const abogadoSelect = document.getElementById('select-abogado-termino');
    
    if(!expSelect || !abogadoSelect) return;
    
    const expId = expSelect.value;
    if(!expId) return;

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