// Función para abrir/cerrar modales por ID
function toggleModal(modalID) {
    const modal = document.getElementById(modalID);
    if (modal) {
        modal.classList.toggle('hidden');
        
        // Animación simple
        if (!modal.classList.contains('hidden')) {
            const content = modal.querySelector('div[class*="transform"]');
            if(content) {
                content.classList.add('scale-100');
                content.classList.remove('scale-95');
            }
        } else {
            const content = modal.querySelector('div[class*="transform"]');
            if(content) {
                content.classList.add('scale-95');
                content.classList.remove('scale-100');
            }
        }
    }
}

// Cerrar con tecla ESC
document.onkeydown = function(evt) {
    evt = evt || window.event;
    if (evt.keyCode == 27) {
        const modals = document.querySelectorAll('[role="dialog"]');
        modals.forEach(modal => {
            if (!modal.classList.contains('hidden')) {
                modal.classList.add('hidden');
            }
        });
        // Soporte específico para el modal de nuevo expediente si no usa role="dialog"
        const modalNuevo = document.getElementById('modal-nuevo-exp');
        if (modalNuevo && !modalNuevo.classList.contains('hidden')) {
            modalNuevo.classList.add('hidden');
        }
    }
};

// LÓGICA DE SELECTORES EN CASCADA (NUEVO EXPEDIENTE)
function cargarMateriasExpediente() {
    const gerenciaId = document.getElementById('select-gerencia-exp').value;
    const selectMateria = document.getElementById('select-materia-exp');
    const selectAbogado = document.getElementById('select-abogado-exp');

    // 1. Limpiar y bloquear selectores hijos
    selectMateria.innerHTML = '<option value="">Cargando materias...</option>';
    selectMateria.disabled = true;
    selectMateria.classList.add('bg-gray-50');
    
    selectAbogado.innerHTML = '<option value="">Primero selecciona una materia...</option>';
    selectAbogado.disabled = true;
    selectAbogado.classList.add('bg-gray-50');

    if (!gerenciaId) {
        selectMateria.innerHTML = '<option value="">Primero selecciona una gerencia...</option>';
        return;
    }

    // 2. Traer materias de la API
    fetch(`/usuarios/api/materias-por-gerencia/${gerenciaId}`)
        .then(res => res.json())
        .then(materias => {
            selectMateria.innerHTML = '<option value="">Selecciona materia...</option>';
            materias.forEach(m => {
                selectMateria.innerHTML += `<option value="${m.id}">${m.nombre}</option>`;
            });
            
            // Desbloquear selector
            selectMateria.disabled = false;
            selectMateria.classList.remove('bg-gray-50');
        })
        .catch(error => {
            console.error("Error:", error);
            selectMateria.innerHTML = '<option value="">Error al cargar materias</option>';
        });
}

function cargarAbogadosExpediente() {
    const gerenciaId = document.getElementById('select-gerencia-exp').value;
    const materiaId = document.getElementById('select-materia-exp').value;
    const selectAbogado = document.getElementById('select-abogado-exp');

    // 1. Limpiar y bloquear
    selectAbogado.innerHTML = '<option value="">Buscando abogados...</option>';
    selectAbogado.disabled = true;
    selectAbogado.classList.add('bg-gray-50');

    if (!materiaId || !gerenciaId) {
        selectAbogado.innerHTML = '<option value="">Primero selecciona una materia...</option>';
        return;
    }

    // 2. Traer abogados filtrados
    fetch(`/expedientes/api/abogados?gerenciaId=${gerenciaId}&materiaId=${materiaId}`)
        .then(res => res.json())
        .then(abogados => {
            selectAbogado.innerHTML = '<option value="">Asignar abogado...</option>';
            
            if(abogados.length === 0) {
                    selectAbogado.innerHTML = '<option value="">No hay abogados para esta materia</option>';
            } else {
                abogados.forEach(a => {
                    selectAbogado.innerHTML += `<option value="${a.id}">${a.nombreCompleto}</option>`;
                });
                // Solo desbloqueamos si sí encontró abogados
                selectAbogado.disabled = false;
                selectAbogado.classList.remove('bg-gray-50');
            }
        })
        .catch(error => {
            console.error("Error:", error);
            selectAbogado.innerHTML = '<option value="">Error al cargar abogados</option>';
        });
}