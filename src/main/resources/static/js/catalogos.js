document.addEventListener('DOMContentLoaded', () => {
    console.log("Catálogos cargados");
});

function cambiarTab(tabName) {
    document.querySelectorAll('.tab-content').forEach(el => {
        el.classList.add('hidden'); el.classList.remove('block');
    });
    document.getElementById('tab-' + tabName).classList.remove('hidden');
    document.getElementById('tab-' + tabName).classList.add('block');

    document.querySelectorAll('[id^="card-"]').forEach(card => {
        card.classList.remove('border-gob-oro', 'bg-gray-50');
        card.classList.add('border-transparent', 'bg-white');
    });
    const activeCard = document.getElementById('card-' + tabName);
    if (activeCard) {
        activeCard.classList.remove('border-transparent', 'bg-white');
        activeCard.classList.add('border-gob-oro', 'bg-gray-50');
    }
}

// === GERENCIAS ===
function abrirModalGerencia() {
    document.getElementById('titulo-modal-gerencia').innerHTML = '<i class="fas fa-building mr-2"></i> Nueva Gerencia';
    document.getElementById('input-gerencia-id').value = '';
    document.getElementById('input-gerencia-nombre').value = '';
    document.getElementById('input-gerencia-desc').value = '';
    document.getElementById('input-gerencia-activo').checked = true;
    document.getElementById('modal-gerencia').classList.remove('hidden'); document.getElementById('modal-gerencia').classList.add('flex');
}

function editarGerencia(id, n, d, a) {
    abrirModalGerencia();
    document.getElementById('titulo-modal-gerencia').innerHTML = '<i class="fas fa-edit mr-2"></i> Editar Gerencia';
    document.getElementById('input-gerencia-id').value = id;
    document.getElementById('input-gerencia-nombre').value = n;
    document.getElementById('input-gerencia-desc').value = d;
    document.getElementById('input-gerencia-activo').checked = (a === 'true' || a === true);
}

// === ÓRGANOS ===
function abrirModalOrgano() {
    document.getElementById('titulo-modal-organo').innerHTML = '<i class="fas fa-university mr-2"></i> Nuevo Órgano';
    document.getElementById('input-organo-id').value = '';
    document.getElementById('input-organo-nombre').value = '';
    document.getElementById('input-organo-sede').value = '';
    document.getElementById('modal-organo').classList.remove('hidden'); document.getElementById('modal-organo').classList.add('flex');
}

function editarOrgano(id, n, t, s, a) {
    abrirModalOrgano();
    document.getElementById('titulo-modal-organo').innerHTML = '<i class="fas fa-edit mr-2"></i> Editar Órgano';
    document.getElementById('input-organo-id').value = id;
    document.getElementById('input-organo-nombre').value = n;
    document.getElementById('select-organo-tipo').value = t;
    document.getElementById('input-organo-sede').value = s;
    document.getElementById('check-organo-activo').checked = (a === 'true' || a === true);
}

// === TIPOS AUDIENCIA ===
function abrirModalTipoAudiencia() {
    document.getElementById('titulo-modal-tipo-audiencia').innerHTML = '<i class="fas fa-calendar-alt mr-2"></i> Nuevo Tipo';
    document.getElementById('input-tipo-audiencia-id').value = '';
    document.getElementById('input-tipo-audiencia-nombre').value = '';
    document.getElementById('modal-tipo-audiencia').classList.remove('hidden'); document.getElementById('modal-tipo-audiencia').classList.add('flex');
}

function editarTipoAudiencia(id, n, a) {
    abrirModalTipoAudiencia();
    document.getElementById('titulo-modal-tipo-audiencia').innerHTML = '<i class="fas fa-edit mr-2"></i> Editar Tipo';
    document.getElementById('input-tipo-audiencia-id').value = id;
    document.getElementById('input-tipo-audiencia-nombre').value = n;
    document.getElementById('check-tipo-audiencia-activo').checked = (a === 'true' || a === true);
}

// === MATERIAS ===
function verMaterias(gerenciaId, nombre) {
    document.getElementById('hidden-gerencia-id').value = gerenciaId;
    document.getElementById('subtitulo-gerencia').textContent = 'Gerencia: ' + nombre;
    document.getElementById('modal-materias').classList.remove('hidden'); document.getElementById('modal-materias').classList.add('flex');
    cargarListaMaterias(gerenciaId);
}

function cargarListaMaterias(gerenciaId) {
    const cont = document.getElementById('contenedor-lista-materias');
    cont.innerHTML = '<div class="text-center py-4"><i class="fas fa-spinner fa-spin"></i></div>';
    fetch(`/catalogos/gerencias/${gerenciaId}/materias`)
        .then(res => res.text()).then(html => cont.innerHTML = html);
}

function guardarMateriaJS() {
    const gerenciaId = document.getElementById('hidden-gerencia-id').value;
    const nombre = document.getElementById('input-nueva-materia').value;
    if (!nombre) return;
    fetch('/catalogos/gerencias/materias/guardar', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ nombre: nombre, gerencia: { id: gerenciaId }, activo: true })
    }).then(res => {
        if (res.ok) { document.getElementById('input-nueva-materia').value = ''; cargarListaMaterias(gerenciaId); }
    });
}

function eliminarMateriaJS(id, gId) {
    if (!confirm("¿Eliminar materia?")) return;
    fetch(`/catalogos/gerencias/materias/eliminar/${id}`, { method: 'DELETE' })
        .then(res => { if (res.ok) cargarListaMaterias(gId); else alert("Error"); });
}

function cerrarModalMaterias() {
    document.getElementById('modal-materias').classList.add('hidden'); document.getElementById('modal-materias').classList.remove('flex');
}

// === TOGGLES ===
function confirmarCambioEstatusCatalogo(id, tipo) {
    let url = `/catalogos/${tipo}s/toggle/${id}`;
    if (tipo === 'gerencia') url = `/catalogos/gerencias/toggle/${id}`;
    if (tipo === 'tipo-audiencia') url = `/catalogos/tipos-audiencia/toggle/${id}`;

    Swal.fire({ title: '¿Cambiar estatus?', icon: 'warning', showCancelButton: true, confirmButtonColor: '#9D2449', confirmButtonText: 'Sí' })
        .then((result) => { if (result.isConfirmed) window.location.href = url; });
}

// === INICIO ===
document.addEventListener('DOMContentLoaded', () => {
    const params = new URLSearchParams(window.location.search);
    cambiarTab(params.get('tab') || 'gerencias');
    ['gerencias', 'organos', 'tipos-audiencia'].forEach(tipo => {
        const input = document.getElementById(`input-search-${tipo}`);
        if (input) {
            input.addEventListener('keyup', function () {
                const filter = this.value.toLowerCase();
                const rows = document.getElementById(`tabla-${tipo}-body`).getElementsByTagName('tr');
                for (let r of rows) r.style.display = r.textContent.toLowerCase().indexOf(filter) > -1 ? "" : "none";
            });
        }
    });
});