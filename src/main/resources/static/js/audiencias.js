/**
 * Lógica del Módulo de Audiencias
 */

document.addEventListener('DOMContentLoaded', function() {
    // Inicializaciones si fueran necesarias
});

// --- MENÚS Y NAVEGACIÓN ---
function toggleMenu(id) {
    const menu = document.getElementById('menu-' + id);
    // Cierra otros menús abiertos
    document.querySelectorAll('[id^="menu-"]').forEach(m => {
        if(m !== menu) m.classList.add('hidden');
    });
    // Alterna el actual
    if(menu) menu.classList.toggle('hidden');
}

// Cerrar menús al hacer clic fuera
window.addEventListener('click', function(e) {
    if (!e.target.closest('td.relative')) {
        document.querySelectorAll('[id^="menu-"]').forEach(m => m.classList.add('hidden'));
    }
});

// --- FUNCIONES DEL MODAL (CRUD) ---

// 1. ABRIR PARA CREAR
function abrirModalAudiencia() {
    const form = document.getElementById('form-audiencia');
    if(form) form.reset();
    
    document.getElementById('input-audiencia-id').value = '';
    document.getElementById('modal-titulo-audiencia').innerHTML = '<i class="fas fa-gavel"></i> Nueva Audiencia';
    
    // Valores por defecto
    toggleUbicacion(false); // Presencial por defecto
    const radioPresencial = document.querySelector('input[name="esVirtual"][value="false"]');
    if(radioPresencial) radioPresencial.checked = true;
    
    document.getElementById('modal-audiencia').classList.remove('hidden');
}

// 2. ABRIR PARA EDITAR
function abrirModalEditar(id, expedienteId, tipoId, fecha, hora, esVirtual, sala, url, abogado) {
    // Rellenar IDs y Títulos
    document.getElementById('input-audiencia-id').value = id;
    document.getElementById('modal-titulo-audiencia').innerHTML = '<i class="fas fa-edit"></i> Editar Audiencia';

    // Seleccionar en desplegables
    document.getElementById('input-expediente-audiencia').value = expedienteId;
    document.getElementById('input-tipo-audiencia').value = tipoId;
    document.getElementById('input-abogado-audiencia').value = abogado || '';

    // Fechas
    document.getElementById('input-fecha-audiencia').value = fecha;
    document.getElementById('input-hora-audiencia').value = hora;

    // Lógica Virtual/Presencial
    if (esVirtual) {
        const radioVirtual = document.querySelector('input[name="esVirtual"][value="true"]');
        if(radioVirtual) radioVirtual.checked = true;
        document.getElementById('input-url').value = url || '';
        toggleUbicacion(true);
    } else {
        const radioPresencial = document.querySelector('input[name="esVirtual"][value="false"]');
        if(radioPresencial) radioPresencial.checked = true;
        document.getElementById('input-sala').value = sala || '';
        toggleUbicacion(false);
    }

    document.getElementById('modal-audiencia').classList.remove('hidden');
}

// --- FUNCIONES DE ESTADO (ACTAS Y CIERRE) ---

function activarSubidaActa(id) {
    document.getElementById('input-acta-id').value = id;
    document.getElementById('input-acta-file').click();
}

function abrirModalConcluir(id) {
    document.getElementById('concluir-audiencia-id').value = id;
    document.getElementById('modal-concluir').classList.remove('hidden');
}

// --- LÓGICA VISUAL (FORMULARIOS) ---
function toggleUbicacion(isVirtual) {
    const campoSala = document.getElementById('campo-sala');
    const campoUrl = document.getElementById('campo-url');
    
    if(isVirtual) {
        campoSala.classList.add('hidden');
        campoUrl.classList.remove('hidden');
        // Opcional: Limpiar el otro campo para no enviar basura
        // document.getElementById('input-sala').value = ''; 
    } else {
        campoSala.classList.remove('hidden');
        campoUrl.classList.add('hidden');
        // document.getElementById('input-url').value = '';
    }
}