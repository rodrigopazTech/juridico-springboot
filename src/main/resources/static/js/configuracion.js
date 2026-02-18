/**
 * Lógica para la gestión de catálogos en el módulo de Configuración
 */

function verMaterias(gerenciaId, gerenciaNombre) {
    document.getElementById('modal-gerencia-id').value = gerenciaId;
    document.getElementById('subtitulo-gerencia').innerText = gerenciaNombre;
    document.getElementById('modal-materias').classList.remove('hidden');
    document.getElementById('modal-materias').classList.add('flex');
    cargarMaterias(gerenciaId);
}

function cerrarModalMaterias() {
    document.getElementById('modal-materias').classList.add('hidden');
    document.getElementById('modal-materias').classList.remove('flex');
    document.getElementById('nueva-materia-nombre').value = '';
    document.getElementById('materia-feedback').classList.add('hidden');
}

function cargarMaterias(gerenciaId) {
    const contenedor = document.getElementById('contenedor-lista-materias');
    contenedor.innerHTML = '<div class="text-center py-4"><i class="fas fa-spinner fa-spin mr-2"></i> Cargando...</div>';

    fetch(`/configuracion/gerencias/${gerenciaId}/materias`)
        .then(response => response.text())
        .then(html => {
            contenedor.innerHTML = html;
        })
        .catch(error => {
            console.error('Error:', error);
            contenedor.innerHTML = '<p class="text-red-500 text-sm p-4">Error al cargar materias.</p>';
        });
}

function agregarMateria() {
    const nombreInput = document.getElementById('nueva-materia-nombre');
    const gerenciaId = document.getElementById('modal-gerencia-id').value;
    const nombre = nombreInput.value.trim();

    if (!nombre) {
        showFeedback('Ingrese un nombre para la materia', 'error');
        return;
    }

    const data = {
        nombre: nombre,
        gerencia: { id: parseInt(gerenciaId) },
        activo: true
    };

    fetch('/configuracion/gerencias/materias/guardar', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(data)
    })
        .then(response => {
            if (response.ok) {
                nombreInput.value = '';
                showFeedback('Materia agregada correctamente', 'success');
                cargarMaterias(gerenciaId);
            } else {
                return response.text().then(text => { throw new Error(text) });
            }
        })
        .catch(error => {
            showFeedback(error.message, 'error');
        });
}

function confirmarEliminarMateria(materiaId) {
    if (!confirm('¿Está seguro de eliminar esta materia? Esta acción no se puede deshacer.')) return;

    fetch(`/configuracion/gerencias/materias/eliminar/${materiaId}`, {
        method: 'DELETE'
    })
        .then(response => {
            if (response.ok) {
                const gerenciaId = document.getElementById('modal-gerencia-id').value;
                cargarMaterias(gerenciaId);
                showFeedback('Materia eliminada', 'success');
            } else {
                return response.text().then(text => { throw new Error(text) });
            }
        })
        .catch(error => {
            alert(error.message);
        });
}

function showFeedback(msg, type) {
    const field = document.getElementById('materia-feedback');
    field.innerText = msg;
    field.className = `mt-2 text-xs p-2 rounded ${type === 'success' ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`;
    field.classList.remove('hidden');
    setTimeout(() => field.classList.add('hidden'), 3000);
}
