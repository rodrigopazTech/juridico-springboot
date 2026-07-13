/**
 * Lógica para Gestión de Usuarios, Gerencias y Materias
 */
let materiasUsuarioActual = [];

function cambiarTab(tabName) {
    // A. Ocultar contenidos
    document.querySelectorAll('.tab-content').forEach(el => {
        el.classList.add('hidden');
        el.classList.remove('block');
        el.style.display = 'none';
    });

    // Mostrar contenido seleccionado
    const content = document.getElementById('tab-' + tabName);
    if (content) {
        content.classList.remove('hidden');
        content.classList.add('block');
        content.style.display = 'block';
    }

    // B. Actualizar Botones
    document.querySelectorAll('.tab-btn').forEach(btn => {
        // Quitamos estilos de Tailwind
        btn.classList.remove('border-gob-guinda', 'text-gob-guinda', 'font-bold');
        btn.classList.add('border-transparent', 'text-gray-500');

        // --- CORRECCIÓN CRÍTICA: Quitamos también la clase CSS 'active' ---
        btn.classList.remove('active');
    });

    // Activar botón actual
    const btn = document.getElementById('btn-tab-' + tabName);
    if (btn) {
        // Ponemos estilos Tailwind
        btn.classList.remove('border-transparent', 'text-gray-500');
        btn.classList.add('border-gob-guinda', 'text-gob-guinda', 'font-bold');

        // --- CORRECCIÓN CRÍTICA: Agregamos la clase CSS 'active' ---
        btn.classList.add('active');
    }
}

function abrirModalUsuario() {
    document.getElementById('titulo-modal-usuario').innerHTML = '<i class="fas fa-user-plus mr-2"></i> Nuevo Usuario';

    document.getElementById('input-usuario-id').value = '';
    document.getElementById('input-usuario-nombre').value = '';
    document.getElementById('input-usuario-email').value = '';
    document.getElementById('input-usuario-password').value = '';
    document.getElementById('input-usuario-password').required = true;
    document.getElementById('input-usuario-password').placeholder = "";

    document.getElementById('select-usuario-rol').value = "";
    
    const gerSelect = document.getElementById('select-usuario-gerencia');
    if (gerSelect) {
        if (gerSelect.tomselect) gerSelect.tomselect.setValue('');
        else gerSelect.value = "";
    }

    document.getElementById('check-usuario-activo').checked = true;

    materiasUsuarioActual = [];

    document.getElementById('contenedor-materias').classList.add('hidden');
    document.getElementById('lista-materias-checkboxes').innerHTML = '';

    document.getElementById('seccion-asignacion').classList.remove('hidden');

    document.getElementById('btn-eliminar-usuario').classList.add('hidden');

    const modal = document.getElementById('modal-usuario');
    modal.classList.remove('hidden');
    modal.classList.add('flex');
}

function actualizarFormularioPorRol() {
    const seccionAsignacion = document.getElementById('seccion-asignacion');
    seccionAsignacion.classList.remove('hidden');
}

function cargarMateriasDeGerencia() {
    const selectGer = document.getElementById('select-usuario-gerencia');
    const gerenciaId = selectGer ? (selectGer.tomselect ? selectGer.tomselect.getValue() : selectGer.value) : '';
    const contenedor = document.getElementById('contenedor-materias');
    const listaCheckboxes = document.getElementById('lista-materias-checkboxes');

    if (!gerenciaId) {
        contenedor.classList.add('hidden');
        return;
    }

    listaCheckboxes.innerHTML = '<p class="text-xs text-gray-400">Cargando...</p>';
    contenedor.classList.remove('hidden');

    fetch(`/usuarios/api/materias-por-gerencia/${gerenciaId}`)
        .then(res => res.json())
        .then(materias => {
            listaCheckboxes.innerHTML = '';
            if (materias.length === 0) {
                listaCheckboxes.innerHTML = '<p class="text-xs text-red-400">Sin materias registradas.</p>';
                return;
            }
            materias.forEach(m => {
                const isChecked = materiasUsuarioActual.includes(m.id) ? 'checked' : '';
                const html = `
                    <div class="flex items-center space-x-2">
                        <input type="checkbox" name="materiasIds" value="${m.id}" id="mat-${m.id}" ${isChecked}
                               class="rounded text-gob-guinda focus:ring-gob-oro h-4 w-4 border-gray-300">
                        <label for="mat-${m.id}" class="text-xs text-gray-700 cursor-pointer select-none">${m.nombre}</label>
                    </div>`;
                listaCheckboxes.innerHTML += html;
            });
        });
}

function editarUsuario(id, nombre, email, rol, gerenciaId, activo, materiasIdsArray) {
    document.getElementById('titulo-modal-usuario').innerHTML = '<i class="fas fa-user-edit mr-2"></i> Editar Usuario';
    document.getElementById('input-usuario-id').value = id;
    document.getElementById('input-usuario-nombre').value = nombre;
    document.getElementById('input-usuario-email').value = email;
    document.getElementById('select-usuario-rol').value = rol;
    document.getElementById('check-usuario-activo').checked = activo;

    const inputPass = document.getElementById('input-usuario-password');
    inputPass.value = ''; inputPass.required = false; inputPass.placeholder = "Dejar en blanco para mantener";

    materiasUsuarioActual = materiasIdsArray || [];
    const selectGerencia = document.getElementById('select-usuario-gerencia');

    actualizarFormularioPorRol();

    if (gerenciaId) {
        if (selectGerencia.tomselect) {
            selectGerencia.tomselect.setValue(gerenciaId);
        } else {
            selectGerencia.value = gerenciaId;
        }
        cargarMateriasDeGerencia();
    } else {
        if (selectGerencia.tomselect) {
            selectGerencia.tomselect.setValue('');
        } else {
            selectGerencia.value = "";
        }
        document.getElementById('contenedor-materias').classList.add('hidden');
    }

    const btnEliminar = document.getElementById('btn-eliminar-usuario');
    btnEliminar.classList.remove('hidden');
    document.getElementById('input-usuario-id').value = id;

    const modal = document.getElementById('modal-usuario');
    modal.classList.remove('hidden'); modal.classList.add('flex');
}

function abrirModalGerencia() {
    document.getElementById('titulo-modal-gerencia').innerHTML = '<i class="fas fa-building mr-2"></i> Nueva Gerencia';
    document.getElementById('input-gerencia-id').value = '';
    document.getElementById('input-gerencia-nombre').value = '';
    document.getElementById('input-gerencia-desc').value = '';
    document.getElementById('input-gerencia-activo').checked = true;

    const modal = document.getElementById('modal-gerencia');
    modal.classList.remove('hidden');
    modal.classList.add('flex');
}

function editarGerencia(id, nombre, descripcion, activo) {
    document.getElementById('titulo-modal-gerencia').innerHTML = '<i class="fas fa-edit mr-2"></i> Editar Gerencia';
    document.getElementById('input-gerencia-id').value = id;
    document.getElementById('input-gerencia-nombre').value = nombre;
    document.getElementById('input-gerencia-desc').value = descripcion;
    document.getElementById('input-gerencia-activo').checked = activo;

    const modal = document.getElementById('modal-gerencia');
    modal.classList.remove('hidden');
    modal.classList.add('flex');
}

// --- ÓRGANOS JURISDICCIONALES ---

function abrirModalOrgano() {
    document.getElementById('titulo-modal-organo').innerHTML = '<i class="fas fa-university mr-2"></i> Nuevo Órgano Jurisdiccional';
    document.getElementById('input-organo-id').value = '';
    document.getElementById('input-organo-nombre').value = '';
    document.getElementById('select-organo-tipo').value = 'JUZGADO';
    document.getElementById('input-organo-sede').value = '';
    document.getElementById('check-organo-activo').checked = true;

    const modal = document.getElementById('modal-organo');
    modal.classList.remove('hidden');
    modal.classList.add('flex');
}

function editarOrgano(id, nombre, tipo, sede, activo) {
    document.getElementById('titulo-modal-organo').innerHTML = '<i class="fas fa-edit mr-2"></i> Editar Órgano Jurisdiccional';
    document.getElementById('input-organo-id').value = id;
    document.getElementById('input-organo-nombre').value = nombre;
    document.getElementById('select-organo-tipo').value = tipo;
    document.getElementById('input-organo-sede').value = sede;
    document.getElementById('check-organo-activo').checked = (activo === 'true' || activo === true);

    const modal = document.getElementById('modal-organo');
    modal.classList.remove('hidden');
    modal.classList.add('flex');
}

// --- TIPOS DE AUDIENCIA ---

function abrirModalTipoAudiencia() {
    document.getElementById('titulo-modal-tipo-audiencia').innerHTML = '<i class="fas fa-calendar-alt mr-2"></i> Nuevo Tipo de Audiencia';
    document.getElementById('input-tipo-audiencia-id').value = '';
    document.getElementById('input-tipo-audiencia-nombre').value = '';
    document.getElementById('check-tipo-audiencia-activo').checked = true;

    const modal = document.getElementById('modal-tipo-audiencia');
    modal.classList.remove('hidden');
    modal.classList.add('flex');
}

function editarTipoAudiencia(id, nombre, activo) {
    document.getElementById('titulo-modal-tipo-audiencia').innerHTML = '<i class="fas fa-edit mr-2"></i> Editar Tipo de Audiencia';
    document.getElementById('input-tipo-audiencia-id').value = id;
    document.getElementById('input-tipo-audiencia-nombre').value = nombre;
    document.getElementById('check-tipo-audiencia-activo').checked = (activo === 'true' || activo === true);

    const modal = document.getElementById('modal-tipo-audiencia');
    modal.classList.remove('hidden');
    modal.classList.add('flex');
}

// --- 3. LÓGICA "PRO" MATERIAS (AJAX) ---

// Abrir el modal y cargar lista
function verMaterias(gerenciaId, nombreGerencia) {
    // 1. Configurar Modal
    document.getElementById('hidden-gerencia-id').value = gerenciaId;
    document.getElementById('subtitulo-gerencia').textContent = 'Gerencia: ' + nombreGerencia;

    // 2. Mostrar Modal
    const modal = document.getElementById('modal-materias');
    modal.classList.remove('hidden');
    modal.classList.add('flex');

    // 3. Cargar Lista via FETCH
    cargarListaMaterias(gerenciaId);
}

function cargarListaMaterias(gerenciaId) {
    const contenedor = document.getElementById('contenedor-lista-materias');
    contenedor.innerHTML = '<div class="text-center text-gray-400 py-4"><i class="fas fa-spinner fa-spin mr-2"></i> Cargando...</div>';

    fetch(`/usuarios/gerencias/${gerenciaId}/materias`)
        .then(response => response.text())
        .then(htmlFragment => {
            // Inyectar el HTML que vino del servidor
            contenedor.innerHTML = htmlFragment;
        })
        .catch(error => {
            console.error('Error:', error);
            contenedor.innerHTML = '<p class="text-red-500 text-center text-sm">Error al cargar materias</p>';
        });
}

function guardarMateriaJS() {
    const gerenciaId = document.getElementById('hidden-gerencia-id').value;
    const nombreMateria = document.getElementById('input-nueva-materia').value;

    if (!nombreMateria.trim()) {
        alert("Escribe un nombre para la materia");
        return;
    }

    // Datos a enviar
    const data = {
        nombre: nombreMateria,
        gerencia: { id: gerenciaId }, // Solo mandamos el ID de la relación
        activo: true
    };

    fetch('/usuarios/gerencias/materias/guardar', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            // Si usas CSRF, aquí iría el token
        },
        body: JSON.stringify(data)
    })
        .then(response => {
            if (response.ok) {
                // Limpiar input y recargar lista
                document.getElementById('input-nueva-materia').value = '';
                cargarListaMaterias(gerenciaId);
            } else {
                alert("Error al guardar la materia");
            }
        })
        .catch(error => console.error("Error:", error));
}

function cerrarModalMaterias() {
    const modal = document.getElementById('modal-materias');
    modal.classList.add('hidden');
    modal.classList.remove('flex');
}

// --- 4. BUSCADORES EN TIEMPO REAL ---

function filtrarTabla(inputId, tbodyId) {
    const input = document.getElementById(inputId);
    const filter = input.value.toLowerCase();
    const tbody = document.getElementById(tbodyId);
    if (!tbody) return;

    const rows = tbody.getElementsByTagName('tr');

    for (let i = 0; i < rows.length; i++) {
        const textoFila = rows[i].textContent || rows[i].innerText;
        if (textoFila.toLowerCase().indexOf(filter) > -1) {
            rows[i].style.display = "";
        } else {
            rows[i].style.display = "none";
        }
    }
}

// Inicializar Listeners
document.addEventListener('DOMContentLoaded', function () {
    inicializarSelectsUsuarios();

    // Buscador Gerencias
    const searchGerencia = document.getElementById('input-search-gerencias');
    if (searchGerencia) {
        searchGerencia.addEventListener('keyup', () => filtrarTabla('input-search-gerencias', 'tabla-gerencias-body'));
    }

    // Buscador Órganos
    const searchOrgano = document.getElementById('input-search-organos');
    if (searchOrgano) {
        searchOrgano.addEventListener('keyup', () => filtrarTabla('input-search-organos', 'tabla-organos-body'));
    }

    // Buscador Tipos Audiencia
    const searchTipoAudiencia = document.getElementById('input-search-tipos-audiencia');
    if (searchTipoAudiencia) {
        searchTipoAudiencia.addEventListener('keyup', () => filtrarTabla('input-search-tipos-audiencia', 'tabla-tipos-audiencia-body'));
    }
});

// --- 5. CAMBIO DE ESTATUS (Eliminado Lógico) ---

let accionConfirmadaCallback = null;

function abrirModalConfirmarGenerico(opciones) {
    const { titulo, texto, advertencia, esPeligro, callback } = opciones;

    document.getElementById('modal-confirmar-titulo').textContent = titulo;
    document.getElementById('modal-confirmar-texto').textContent = texto;

    const advBox = document.getElementById('modal-confirmar-advertencia-box');
    if (advertencia) {
        document.getElementById('modal-confirmar-advertencia').textContent = advertencia;
        advBox.classList.remove('hidden');
    } else {
        advBox.classList.add('hidden');
    }

    const borderEl = document.getElementById('modal-confirmar-border');
    const iconBgEl = document.getElementById('modal-confirmar-icon-bg');
    const iconEl = document.getElementById('modal-confirmar-icon');
    const btnAccion = document.getElementById('btn-modal-confirmar-accion');

    if (esPeligro) {
        borderEl.className = "relative w-full max-w-sm bg-white rounded-lg shadow-2xl border-t-4 border-red-600";
        iconBgEl.className = "w-16 h-16 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-4";
        iconEl.className = "fas fa-exclamation-triangle text-red-600 text-2xl";
        btnAccion.className = "flex-1 px-4 py-2 text-sm font-bold text-white bg-red-600 rounded hover:bg-red-700 shadow-md";
        btnAccion.textContent = "Eliminar";
    } else {
        borderEl.className = "relative w-full max-w-sm bg-white rounded-lg shadow-2xl border-t-4 border-amber-500";
        iconBgEl.className = "w-16 h-16 bg-amber-100 rounded-full flex items-center justify-center mx-auto mb-4";
        iconEl.className = "fas fa-exclamation-triangle text-amber-600 text-2xl";
        btnAccion.className = "flex-1 px-4 py-2 text-sm font-bold text-white bg-gob-guinda rounded hover:bg-opacity-95 shadow-md";
        btnAccion.textContent = "Aceptar";
    }

    accionConfirmadaCallback = callback;

    const modal = document.getElementById('modal-confirmar-generico');
    if (modal) {
        modal.classList.remove('hidden');
        modal.classList.add('flex');
    }
}

function cerrarModalConfirmarGenerico() {
    accionConfirmadaCallback = null;
    const modal = document.getElementById('modal-confirmar-generico');
    if (modal) {
        modal.classList.add('hidden');
        modal.classList.remove('flex');
    }
}

function ejecutarAccionConfirmada() {
    if (accionConfirmadaCallback) {
        accionConfirmadaCallback();
    }
}

function confirmarCambioEstatus(id, tipo) {
    let titulo = tipo === 'usuario' ? '¿Cambiar acceso del usuario?' : '¿Cambiar estatus de gerencia?';
    let texto = "El registro cambiará de Activo a Inactivo (o viceversa).";
    let url = tipo === 'usuario' ? `/usuarios/toggle/${id}` : `/usuarios/gerencias/toggle/${id}`;

    abrirModalConfirmarGenerico({
        titulo: titulo,
        texto: texto,
        advertencia: "El cambio afectará la disponibilidad del registro en el sistema.",
        esPeligro: false,
        callback: () => {
            window.location.href = url;
        }
    });
}

function confirmarCambioEstatusCatalogo(id, tipo) {
    let titulo = '';
    let url = '';

    if (tipo === 'organo') {
        titulo = '¿Cambiar estatus del Órgano?';
        url = `/usuarios/organos/toggle/${id}`;
    } else if (tipo === 'tipo-audiencia') {
        titulo = '¿Cambiar estatus del Tipo de Audiencia?';
        url = `/usuarios/tipos-audiencia/toggle/${id}`;
    }

    abrirModalConfirmarGenerico({
        titulo: titulo,
        texto: "El registro cambiará su disponibilidad en el sistema.",
        advertencia: "Puedes revertir este cambio en cualquier momento.",
        esPeligro: false,
        callback: () => {
            window.location.href = url;
        }
    });
}

function eliminarMateriaJS(materiaId, gerenciaId) {
    abrirModalConfirmarGenerico({
        titulo: "¿Eliminar materia?",
        texto: "¿Seguro que deseas eliminar esta materia?",
        advertencia: "Esta acción no se puede deshacer.",
        esPeligro: true,
        callback: () => {
            cerrarModalConfirmarGenerico();
            fetch(`/usuarios/gerencias/materias/eliminar/${materiaId}`, {
                method: 'DELETE'
            })
            .then(response => {
                if (response.ok) {
                    cargarListaMaterias(gerenciaId);
                } else {
                    alert("No se pudo eliminar. Verifica que no tenga expedientes vinculados.");
                }
            })
            .catch(error => console.error("Error:", error));
        }
    });
}

function eliminarUsuarioDesdeModal() {
    const id = document.getElementById('input-usuario-id').value;
    if (!id) return;

    abrirModalConfirmarGenerico({
        titulo: "¿Eliminar usuario definitivamente?",
        texto: "Esta acción no se puede deshacer. Si el usuario tiene historial, no se podrá borrar.",
        advertencia: "Se borrarán los accesos y registros asociados permanentemente.",
        esPeligro: true,
        callback: () => {
            cerrarModalConfirmarGenerico();
            fetch(`/usuarios/eliminar/${id}`, {
                method: 'DELETE'
            })
            .then(async response => {
                if (response.ok) {
                    const Toast = Swal.mixin({
                        toast: true,
                        position: 'bottom-end',
                        showConfirmButton: false,
                        timer: 2000,
                    });

                    Toast.fire({
                        icon: 'success',
                        title: 'Eliminado correctamente',
                        iconColor: '#22c55e',
                        background: '#f0fdf4',
                        customClass: {
                            title: 'text-green-800 text-sm font-bold'
                        }
                    }).then(() => {
                        location.reload();
                    });
                } else {
                    const msg = await response.text();
                    Swal.fire('Error', msg || 'No se pudo eliminar el usuario.', 'error');
                }
            })
            .catch(error => {
                console.error(error);
                Swal.fire('Error', 'Ocurrió un error inesperado.', 'error');
            });
        }
    });
}
// Detectar tab en URL para abrirlo automáticamente
const params = new URLSearchParams(window.location.search);
const tabParam = params.get('tab');
if (tabParam) {
    cambiarTab(tabParam);
}

function inicializarSelectsUsuarios() {
    const selectGerencia = document.getElementById('select-usuario-gerencia');
    if (selectGerencia && !selectGerencia.tomselect) {
        initSearchableSelect(selectGerencia, {
            tipo: 'gerencia',
            onChange: function() {
                cargarMateriasDeGerencia();
            }
        });
    }
}