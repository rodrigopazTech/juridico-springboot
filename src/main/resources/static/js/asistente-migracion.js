function inicializarAsistente() {
    // ==========================================
    // LÓGICA DE PESTAÑAS (TABS) PARA LA ETAPA 1
    // ==========================================
    const tabBtns = document.querySelectorAll(".tab-btn");
    const tabPanels = document.querySelectorAll(".tab-panel");

    console.log("Cargando Asistente de Migración... Pestañas detectadas:", tabBtns.length);

    if (tabBtns.length === 0 || tabPanels.length === 0) return;

    // 1. Esconder todos los paneles por seguridad
    tabPanels.forEach(p => p.classList.add("hidden"));
    
    // 2. Encender dinámicamente el panel que corresponda al PRIMER botón que exista
    const firstBtn = tabBtns[0];
    const targetId = firstBtn.getAttribute("data-target");
    const firstPanel = document.getElementById(targetId);

    if(firstPanel) {
        firstPanel.classList.remove("hidden");
        firstBtn.classList.add("border-gob-oro", "text-gob-oro", "font-bold");
        firstBtn.classList.remove("border-transparent", "text-gray-500");
    }

    // 3. Asignar los eventos de clic
    tabBtns.forEach(btn => {
        btn.addEventListener("click", function() {
            const tId = this.getAttribute("data-target");
            const tPanel = document.getElementById(tId);

            if(!tPanel) return; // Si no existe el panel, no hace nada y no se rompe

            // Resetear botones y paneles
            tabBtns.forEach(b => {
                b.classList.remove("border-gob-oro", "text-gob-oro", "font-bold");
                b.classList.add("border-transparent", "text-gray-500");
            });
            tabPanels.forEach(p => p.classList.add("hidden"));

            // Activar el seleccionado
            this.classList.add("border-gob-oro", "text-gob-oro", "font-bold");
            this.classList.remove("border-transparent", "text-gray-500");
            tPanel.classList.remove("hidden");
        });
    });
}

// Forzar ejecución inmediata de las pestañas al cargar la página
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', inicializarAsistente);
} else {
    inicializarAsistente();
}

// ==========================================
// SWEET ALERT PARA LA ETAPA FINAL (Inyección)
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