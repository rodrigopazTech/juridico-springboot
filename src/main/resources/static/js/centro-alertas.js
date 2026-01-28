/**
 * Lógica para Centro de Alertas (Notificaciones + Recordatorios)
 */

console.log("Cargando Centro de Alertas JS...");

document.addEventListener('DOMContentLoaded', function() {
    console.log("DOM Cargado - Inicializando Listeners");

    // 1. Detectar tab en URL para abrirlo directamente
    const params = new URLSearchParams(window.location.search);
    const tabParam = params.get('tab');
    if(tabParam) {
        window.cambiarTab(tabParam);
    }

    // 2. Activar Buscador de Notificaciones
    const inputNotif = document.getElementById('search-notifications');
    if(inputNotif) {
        inputNotif.addEventListener('keyup', function() {
            filtrarTabla('search-notifications', 'tabla-notificaciones-body');
        });
    }

    // 3. Activar Buscador de Recordatorios
    const inputRec = document.getElementById('search-recordatorios');
    if(inputRec) {
        inputRec.addEventListener('keyup', function() {
            filtrarTarjetas('search-recordatorios', 'grid-recordatorios-container');
        });
    }
});

// --- FUNCIONES GLOBALES (Window) PARA EVITAR REFERENCE ERROR ---

window.cambiarTab = function(tabName) {
    console.log("Cambiando a tab:", tabName);
    
    // A. Ocultar TODOS los contenidos
    document.querySelectorAll('.tab-content').forEach(el => {
        el.classList.add('hidden');
        el.classList.remove('block');
        el.style.display = 'none'; // Forzar CSS
    });

    // B. Mostrar el contenido seleccionado
    const content = document.getElementById('tab-' + tabName);
    if(content) {
        content.classList.remove('hidden');
        content.classList.add('block');
        content.style.display = 'block';
    } else {
        console.error("No se encontró el contenido del tab: tab-" + tabName);
    }

    // C. Actualizar estilos de Botones
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.classList.remove('border-gob-guinda', 'text-gob-guinda', 'font-bold', 'active');
        btn.classList.add('border-transparent', 'text-gray-500');
    });

    // Activar botón actual
    const btn = document.getElementById('btn-tab-' + tabName);
    if(btn) {
        btn.classList.remove('border-transparent', 'text-gray-500');
        btn.classList.add('border-gob-guinda', 'text-gob-guinda', 'font-bold', 'active');
    }
};

window.abrirModalRecordatorio = function() {
    console.log("Abriendo modal recordatorio");
    const modal = document.getElementById('modal-recordatorio');
    
    if (modal) {
        modal.classList.remove('hidden');
        modal.classList.add('flex');
        
        // Poner fecha de hoy si el campo está vacío
        const dateInput = modal.querySelector('input[name="fechaRecordatorio"]');
        if(dateInput && !dateInput.value) {
            dateInput.valueAsDate = new Date();
        }
    } else {
        console.error("Error: No se encuentra el elemento con ID 'modal-recordatorio'");
    }
};

window.cerrarModalRecordatorio = function() {
    const modal = document.getElementById('modal-recordatorio');
    if(modal) {
        modal.classList.add('hidden');
        modal.classList.remove('flex');
    }
};

// --- FUNCIONES AUXILIARES (No necesitan ser globales si no se llaman desde HTML onclick) ---

function filtrarTabla(inputId, tbodyId) {
    const input = document.getElementById(inputId);
    const filter = input.value.toLowerCase();
    const tbody = document.getElementById(tbodyId);
    if(!tbody) return;

    const rows = tbody.getElementsByTagName('tr');
    for (let i = 0; i < rows.length; i++) {
        const texto = rows[i].textContent || rows[i].innerText;
        rows[i].style.display = texto.toLowerCase().indexOf(filter) > -1 ? "" : "none";
    }
}

function filtrarTarjetas(inputId, containerId) {
    const input = document.getElementById(inputId);
    const filter = input.value.toLowerCase();
    const container = document.getElementById(containerId);
    if(!container) return;

    const cards = container.children; 
    for (let i = 0; i < cards.length; i++) {
        const card = cards[i];
        const texto = card.textContent || card.innerText;
        if(!card.classList.contains('empty-state')) {
             card.style.display = texto.toLowerCase().indexOf(filter) > -1 ? "" : "none";
        }
    }
}