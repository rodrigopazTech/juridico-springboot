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
    document.addEventListener('DOMContentLoaded', inicializarMapeo);
} else {
    inicializarAsistente();
    inicializarMapeo();
}

// ==========================================
// LÓGICA DE MAPEO (ETAPA 1)
// ==========================================
function inicializarMapeo() {
    // Inicializar TomSelect en todos los selects
    const selects = document.querySelectorAll('.searchable-select');
    selects.forEach(select => {
        if (!select.tomselect) {
            const ts = new TomSelect(select, {
                create: false,
                sortField: { field: "text", direction: "asc" },
                placeholder: '-- Selecciona un valor oficial --',
                dropdownParent: 'body',
                onDropdownOpen: function(dropdown) {
                    // Posicionar el dropdown justo debajo del campo de búsqueda
                    const rect = this.control.getBoundingClientRect();
                    dropdown.style.position = 'fixed';
                    dropdown.style.zIndex   = '99999';
                    dropdown.style.top      = rect.bottom + 'px';
                    dropdown.style.left     = rect.left + 'px';
                    dropdown.style.width    = rect.width + 'px';
                }
            });
        }
    });

    // Lógica para botones de "Crear como Nuevo"
    document.querySelectorAll('.btn-marcar-nuevo').forEach(btn => {
        btn.addEventListener('click', function() {
            const catId = this.getAttribute('data-cat-id');
            const originalVal = this.getAttribute('data-original-val');
            
            // Ocultar select original y botón +
            document.getElementById('mapeo-container-' + catId).classList.add('hidden');
            document.getElementById('mapeo-container-' + catId).classList.remove('flex');
            
            // Mostrar badge
            const badge = document.getElementById('nuevo-badge-' + catId);
            badge.classList.remove('hidden');
            badge.classList.add('flex');
            
            // Ocultamos y deshabilitamos el select
            const selectEl = document.getElementById('select-' + catId);
            selectEl.disabled = true; // Deshabilitamos para que no se envíe en el submit
            
            // Inyectar el campo hidden para el envío form
            if (!document.getElementById('hidden-nuevo-' + catId)) {
                const hiddenInput = document.createElement('input');
                hiddenInput.type = 'hidden';
                hiddenInput.id = 'hidden-nuevo-' + catId;
                hiddenInput.name = 'cat_' + catId;
                hiddenInput.value = 'NEW_' + originalVal;
                badge.appendChild(hiddenInput);
            }
        });
    });

    // Lógica para deshacer "Crear como Nuevo"
    document.querySelectorAll('.btn-deshacer-nuevo').forEach(btn => {
        btn.addEventListener('click', function() {
            const catId = this.getAttribute('data-cat-id');
            
            // Eliminar hidden input si existe
            const hiddenInput = document.getElementById('hidden-nuevo-' + catId);
            if (hiddenInput) {
                hiddenInput.remove();
            }
            
            // Volver a mostrar select y botón +
            const container = document.getElementById('mapeo-container-' + catId);
            container.classList.remove('hidden');
            container.classList.add('flex');
            
            // Ocultar badge
            const badge = document.getElementById('nuevo-badge-' + catId);
            badge.classList.add('hidden');
            badge.classList.remove('flex');
            
            // Habilitar y limpiar el select
            const selectEl = document.getElementById('select-' + catId);
            selectEl.disabled = false;
            
            // Restablecer TomSelect
            if (selectEl.tomselect) {
                selectEl.tomselect.clear();
            } else {
                selectEl.value = '';
            }
        });
    });

    // Botón Global "Crear TODOS como Nuevos"
    const btnCrearTodos = document.getElementById('btn-crear-todos-nuevos');
    if (btnCrearTodos) {
        btnCrearTodos.addEventListener('click', function() {
            // Aplicar a todos los botones que esten visibles (o ocultos) sin excepcion
            document.querySelectorAll('.btn-marcar-nuevo').forEach(b => {
                // Solo clickear si el select contenedor todavia esta visible (para no duplicar)
                const catId = b.getAttribute('data-cat-id');
                const container = document.getElementById('mapeo-container-' + catId);
                if (container && !container.classList.contains('hidden')) {
                    b.click();
                }
            });
        });
    }
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