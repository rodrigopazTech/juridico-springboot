document.addEventListener('DOMContentLoaded', function() {
    console.log("Sistema de Audiencias v2.0 - Cargado");
});

// --- MENÚS (ACCIONES RÁPIDAS) ---
function toggleMenu(id) {
    const menu = document.getElementById('menu-' + id);
    if (!menu) return;

    // Cerramos todos los demás
    document.querySelectorAll('[id^="menu-"]').forEach(m => {
        if (m !== menu) m.classList.add('hidden');
    });

    // Alternamos el actual
    menu.classList.toggle('hidden');
}

// Cerrar menús al hacer clic fuera
window.addEventListener('click', function(e) {
    if (!e.target.closest('td.relative')) {
        document.querySelectorAll('[id^="menu-"]').forEach(m => m.classList.add('hidden'));
    }
});

// --- FUNCIONES DEL MODAL ---

// 1. NUEVA AUDIENCIA
function abrirModalAudiencia() {
    const form = document.getElementById('form-audiencia');
    if(form) form.reset();
    
    document.getElementById('input-audiencia-id').value = '';
    document.getElementById('modal-titulo-audiencia').innerHTML = '<i class="fas fa-gavel"></i> Nueva Audiencia';
    
    toggleUbicacion(false); 
    const radioPresencial = document.querySelector('input[name="esVirtual"][value="false"]');
    if(radioPresencial) radioPresencial.checked = true;
    
    document.getElementById('modal-audiencia').classList.remove('hidden');
}

// 2. PREPARAR EDICIÓN (Lee los datos seguros del botón)
function prepararEdicion(btn) {
    // Leemos los datos de los atributos data-*
    const id = btn.getAttribute('data-id');
    const expedienteId = btn.getAttribute('data-expediente-id');
    const tipoId = btn.getAttribute('data-tipo-id');
    const fecha = btn.getAttribute('data-fecha');
    const hora = btn.getAttribute('data-hora');
    const esVirtual = btn.getAttribute('data-virtual') === 'true';
    const sala = btn.getAttribute('data-sala');
    const url = btn.getAttribute('data-url');
    const abogadoId = btn.getAttribute('data-abogado-id');

    console.log("Editando ID:", id);

    // Llenamos el formulario
    document.getElementById('input-audiencia-id').value = id;
    document.getElementById('modal-titulo-audiencia').innerHTML = '<i class="fas fa-edit"></i> Editar Audiencia';

    setValueIfExists('input-expediente-audiencia', expedienteId);
    setValueIfExists('input-tipo-audiencia', tipoId);
    setValueIfExists('input-abogado-audiencia', abogadoId);

    document.getElementById('input-fecha-audiencia').value = fecha;
    document.getElementById('input-hora-audiencia').value = hora;

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

function setValueIfExists(elementId, value) {
    const el = document.getElementById(elementId);
    if(el) el.value = value || ''; 
}

// --- UTILIDADES ---
function toggleUbicacion(isVirtual) {
    const campoSala = document.getElementById('campo-sala');
    const campoUrl = document.getElementById('campo-url');
    const inputSala = document.getElementById('input-sala');
    const inputUrl = document.getElementById('input-url');
    
    if(isVirtual) {
        campoSala.classList.add('hidden');
        campoUrl.classList.remove('hidden');
        if(inputUrl) inputUrl.setAttribute('required', 'required');
        if(inputSala) inputSala.removeAttribute('required');
    } else {
        campoSala.classList.remove('hidden');
        campoUrl.classList.add('hidden');
        if(inputSala) inputSala.setAttribute('required', 'required');
        if(inputUrl) inputUrl.removeAttribute('required');
    }
}

function activarSubidaActa(id) {
    document.getElementById('input-acta-id').value = id;
    document.getElementById('input-acta-file').click();
}

function abrirModalConcluir(id) {
    document.getElementById('concluir-audiencia-id').value = id;
    document.getElementById('modal-concluir').classList.remove('hidden');
}