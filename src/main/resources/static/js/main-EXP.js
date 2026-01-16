// Función para abrir/cerrar modales por ID
function toggleModal(modalID) {
    const modal = document.getElementById(modalID);
    if (modal) {
        // Toggle de clases para mostrar/ocultar
        modal.classList.toggle('hidden');
        
        // Animación simple (opcional)
        if (!modal.classList.contains('hidden')) {
            modal.querySelector('div[class*="transform"]').classList.add('scale-100');
            modal.querySelector('div[class*="transform"]').classList.remove('scale-95');
        } else {
            modal.querySelector('div[class*="transform"]').classList.add('scale-95');
            modal.querySelector('div[class*="transform"]').classList.remove('scale-100');
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
    }
};