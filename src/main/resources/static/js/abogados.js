document.addEventListener('DOMContentLoaded', () => {
    console.log("Abogados cargados");
});


let materiasUsuarioActual = [];

function abrirModalUsuario() {
    document.getElementById('titulo-modal-usuario').innerHTML = '<i class="fas fa-user-plus mr-2"></i> Nuevo Abogado';
    document.getElementById('input-usuario-id').value = '';
    document.getElementById('input-usuario-nombre').value = '';
    document.getElementById('input-usuario-email').value = '';
    document.getElementById('input-usuario-password').value = '';
    document.getElementById('input-usuario-password').required = true;
    document.getElementById('input-usuario-password').placeholder = "";
    document.getElementById('select-usuario-rol').value = "";
    document.getElementById('select-usuario-gerencia').value = "";
    document.getElementById('check-usuario-activo').checked = true;

    materiasUsuarioActual = [];
    document.getElementById('contenedor-materias').classList.add('hidden');
    document.getElementById('lista-materias-checkboxes').innerHTML = '';
    document.getElementById('seccion-asignacion').classList.remove('hidden');
    document.getElementById('btn-eliminar-usuario').classList.add('hidden');

    const modal = document.getElementById('modal-usuario');
    modal.classList.remove('hidden'); modal.classList.add('flex');
}

function actualizarFormularioPorRol() {
    const rol = document.getElementById('select-usuario-rol').value;
    const seccion = document.getElementById('seccion-asignacion');
    const selectGerencia = document.getElementById('select-usuario-gerencia');
    if (rol === 'DIRECCION' || rol === 'SUBDIRECCION') {
        seccion.classList.add('hidden');
        selectGerencia.value = "";
    } else {
        seccion.classList.remove('hidden');
    }
}

function cargarMateriasDeGerencia() {
    const gerenciaId = document.getElementById('select-usuario-gerencia').value;
    const contenedor = document.getElementById('contenedor-materias');
    const lista = document.getElementById('lista-materias-checkboxes');

    if (!gerenciaId) { contenedor.classList.add('hidden'); return; }

    lista.innerHTML = '<p class="text-xs text-gray-400">Cargando...</p>';
    contenedor.classList.remove('hidden');

    fetch(`/catalogos/api/materias-por-gerencia/${gerenciaId}`)
        .then(res => res.json())
        .then(materias => {
            lista.innerHTML = '';
            if (materias.length === 0) {
                lista.innerHTML = '<p class="text-xs text-red-400">Sin materias.</p>';
                return;
            }
            materias.forEach(m => {
                const isChecked = materiasUsuarioActual.includes(m.id) ? 'checked' : '';
                lista.innerHTML += `
                    <div class="flex items-center space-x-2">
                        <input type="checkbox" name="materiasIds" value="${m.id}" id="mat-${m.id}" ${isChecked} class="rounded text-gob-guinda h-4 w-4 border-gray-300">
                        <label for="mat-${m.id}" class="text-xs text-gray-700 cursor-pointer">${m.nombre}</label>
                    </div>`;
            });
        });
}

function editarUsuario(id, nombre, email, rol, gerenciaId, activo, materiasIdsArray) {
    abrirModalUsuario();
    document.getElementById('titulo-modal-usuario').innerHTML = '<i class="fas fa-user-edit mr-2"></i> Editar Abogado';
    document.getElementById('input-usuario-id').value = id;
    document.getElementById('input-usuario-nombre').value = nombre;
    document.getElementById('input-usuario-email').value = email;
    document.getElementById('select-usuario-rol').value = rol;
    document.getElementById('check-usuario-activo').checked = activo;

    const inputPass = document.getElementById('input-usuario-password');
    inputPass.required = false;
    inputPass.placeholder = "Dejar en blanco para mantener";

    materiasUsuarioActual = materiasIdsArray || [];
    actualizarFormularioPorRol();

    if (gerenciaId) {
        document.getElementById('select-usuario-gerencia').value = gerenciaId;
        cargarMateriasDeGerencia();
    }
    document.getElementById('btn-eliminar-usuario').classList.remove('hidden');
}

function eliminarUsuarioDesdeModal() {
    const id = document.getElementById('input-usuario-id').value;
    if (!id) return;
    Swal.fire({
        title: '¿Eliminar abogado?', text: "Acción irreversible.", icon: 'error',
        showCancelButton: true, confirmButtonColor: '#d33', confirmButtonText: 'Eliminar'
    }).then((result) => {
        if (result.isConfirmed) {
            window.location.href = `/abogados/eliminar/${id}`;
        }
    });
}

function confirmarCambioEstatus(id) {
    Swal.fire({
        title: '¿Cambiar acceso?', icon: 'warning', showCancelButton: true,
        confirmButtonColor: '#9D2449', confirmButtonText: 'Sí, cambiar'
    }).then((result) => {
        if (result.isConfirmed) window.location.href = `/abogados/toggle/${id}`;
    });
}

document.addEventListener('DOMContentLoaded', () => {
    const search = document.getElementById('input-search-usuarios');
    if (search) {
        search.addEventListener('keyup', function () {
            const filter = this.value.toLowerCase();
            const rows = document.getElementById('tabla-usuarios-body').getElementsByTagName('tr');
            for (let row of rows) row.style.display = row.textContent.toLowerCase().indexOf(filter) > -1 ? "" : "none";
        });
    }
});