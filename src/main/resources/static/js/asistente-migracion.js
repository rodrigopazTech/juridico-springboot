function inicializarAsistente() {
    // ==========================================
    // LÓGICA DE PESTAÑAS (TABS) PARA LA ETAPA 1
    // ==========================================
    const tabBtns = document.querySelectorAll('.tab-btn');
    const tabPanels = document.querySelectorAll('.tab-panel');

    console.log("Cargando Asistente de Migración... Pestañas detectadas:", tabBtns.length);

    // Encender el primer panel disponible
    if (tabPanels.length > 0 && tabBtns.length > 0) {
        tabPanels[0].classList.remove('hidden');
        
        // Limpiamos estilos de todos y activamos el primero
        tabBtns.forEach(b => {
            b.classList.remove('border-gob-oro', 'text-gob-oro', 'font-bold');
            b.classList.add('border-transparent', 'text-gray-500');
        });
        tabBtns[0].classList.add('border-gob-oro', 'text-gob-oro', 'font-bold');
        tabBtns[0].classList.remove('border-transparent', 'text-gray-500');
    }

    tabBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            const targetId = btn.getAttribute('data-target');
            const targetPanel = document.getElementById(targetId);

            // Protección anticaídas: Si el HTML tiene mal el nombre, te avisa en consola pero no se rompe
            if(!targetPanel) {
                console.error("¡Cuidado! No se encontró el panel con el ID:", targetId);
                return; 
            }

            // 1. Resetear botones
            tabBtns.forEach(b => {
                b.classList.remove('border-gob-oro', 'text-gob-oro', 'font-bold');
                b.classList.add('border-transparent', 'text-gray-500');
            });
            // 2. Ocultar todos los paneles
            tabPanels.forEach(p => p.classList.add('hidden'));

            // 3. Activar el botón clickeado
            btn.classList.add('border-gob-oro', 'text-gob-oro', 'font-bold');
            btn.classList.remove('border-transparent', 'text-gray-500');
            
            // 4. Mostrar el panel correspondiente
            targetPanel.classList.remove('hidden');
        });
    });
}

// Asegurar ejecución del JS sin importar cómo cargue el navegador
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', inicializarAsistente);
} else {
    inicializarAsistente();
}

// ==========================================
// SWEET ALERT PARA LA ETAPA FINAL
// ==========================================
function confirmarInyeccion() {
    Swal.fire({
        title: '¿Estás completamente seguro?',
        text: "Se generarán los expedientes y se enviarán notificaciones a los abogados.",
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#1a3c34',
        cancelButtonColor: '#d33',
        confirmButtonText: 'Sí, ¡Inyectar ahora!',
        cancelButtonText: 'Revisar de nuevo'
    }).then((result) => {
        if (result.isConfirmed) {
            document.getElementById('formFinal').submit();
        }
    });
}