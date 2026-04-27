/**
 * Lógica del Módulo de Agenda General
 * Gestiona pestañas, filtros y modales de observación.
 */

// 1. CAMBIAR PESTAÑA (Global) - Conservado para compatibilidad
window.cambiarTab = function(tabName) {
    document.querySelectorAll('.tab-content').forEach(el => el.classList.remove('active'));
    document.querySelectorAll('.tab-btn').forEach(el => el.classList.remove('active'));
    const content = document.getElementById('tab-' + tabName);
    if(content) content.classList.add('active');
    const btn = document.getElementById('btn-tab-' + tabName);
    if(btn) btn.classList.add('active');
    try {
        const url = new URL(window.location);
        url.searchParams.set('tab', tabName);
        window.history.pushState({}, '', url);
    } catch(e) { console.error("Error actualizando URL", e); }
};

// 1.1 CAMBIAR PESTAÑA (PILL SWITCHER)
window.cambiarTabPill = function(tabName) {
    // Hide all contents
    document.querySelectorAll('.tab-content').forEach(el => {
        el.classList.add('hidden');
        el.classList.remove('block');
    });

    // Reset all buttons
    document.querySelectorAll('.tab-btn-pill').forEach(el => {
        el.classList.remove('bg-gob-guinda', 'text-white', 'shadow-md', 'shadow-gob-guinda/20');
        el.classList.add('text-gray-400', 'hover:bg-gray-50', 'hover:text-gray-600');
    });

    // Show active content
    const content = document.getElementById('tab-' + tabName);
    if(content) {
        content.classList.remove('hidden');
        content.classList.add('block');
    }

    // Highlight active button
    const btn = document.getElementById('btn-tab-' + tabName);
    if(btn) {
        btn.classList.remove('text-gray-400', 'hover:bg-gray-50', 'hover:text-gray-600');
        btn.classList.add('bg-gob-guinda', 'text-white', 'shadow-md', 'shadow-gob-guinda/20');
    }

    // Actualizar URL sin recargar
    try {
        const url = new URL(window.location);
        url.searchParams.set('tab', tabName);
        window.history.pushState({}, '', url);
    } catch(e) { console.error("Error actualizando URL", e); }
};


// 2. FILTRO RÁPIDO EN CLIENTE (Buscador)
window.filtrarTabla = function(tablaId, texto) {
    const filtro = texto.toLowerCase();
    const tabla = document.getElementById(tablaId); 
    if (!tabla) return;
    
    const filas = tabla.getElementsByTagName('tr');
    for (let i = 0; i < filas.length; i++) { 
        // Ignorar encabezados (dentro de thead)
        if (filas[i].parentNode.tagName === 'THEAD') continue;

        let textoFila = filas[i].textContent || filas[i].innerText;
        if (textoFila.toLowerCase().indexOf(filtro) > -1) {
            filas[i].style.display = "";
        } else {
            filas[i].style.display = "none";
        }
    }
};

// 3. VER OBSERVACIONES (Modal)
window.verObservaciones = function(id, tipo, expediente, observaciones) {
    const titleEl = document.getElementById('obs-modal-title');
    const expEl = document.getElementById('obs-modal-expediente');
    const contentEl = document.getElementById('obs-modal-content');
    const modal = document.getElementById('modal-observaciones');

    if(titleEl) {
        titleEl.innerHTML = tipo === 'audiencia' ? 
            '<i class="fas fa-gavel mr-2"></i>Observaciones Audiencia' : 
            '<i class="fas fa-clock mr-2"></i>Observaciones Término';
    }
    
    if(expEl) {
        expEl.innerHTML = `<span class="font-bold text-gob-guinda text-lg"><i class="fas fa-folder-open mr-2"></i>${expediente}</span>`;
    }
    
    if(contentEl) {
        // Limpiamos observaciones que vengan como "null" string
        let texto = (observaciones && observaciones !== 'null' && observaciones !== '') ? observaciones : 'Sin observaciones registradas.';
        contentEl.innerHTML = `<p class="text-gray-700 bg-gray-50 p-4 rounded border border-gray-200 text-sm leading-relaxed">${texto}</p>`;
    }

    if(modal) {
        modal.classList.remove('hidden');
        // Aseguramos visualización forzando flex si usas tailwind o style directo
        modal.style.display = 'flex'; 
        modal.classList.add('flex'); // Clase de utilidad si la tienes
    }
};

// 4. CERRAR MODAL
window.cerrarModal = function() {
    const modal = document.getElementById('modal-observaciones');
    if(modal) {
        modal.classList.add('hidden');
        modal.classList.remove('flex');
        modal.style.display = 'none';
    }
};

// 5. INICIALIZACIÓN AL CARGAR EL DOM
document.addEventListener("DOMContentLoaded", function() {
    // Asignar eventos de cierre al modal
    const btnClose = document.getElementById('close-modal-observaciones');
    if(btnClose) btnClose.onclick = window.cerrarModal;
    
    const btnCerrar = document.getElementById('btn-cerrar-obs');
    if(btnCerrar) btnCerrar.onclick = window.cerrarModal;
    
    // Cerrar al hacer clic fuera del contenido del modal
    window.onclick = function(event) {
        const modal = document.getElementById('modal-observaciones');
        if (event.target == modal) window.cerrarModal();
    };
});