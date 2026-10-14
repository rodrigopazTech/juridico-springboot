function esRolDirectivo(rol) {
    return ['ADMIN', 'DIRECTOR_PRINCIPAL', 'DIRECTOR', 'SUBDIRECTOR'].includes(rol);
}

document.addEventListener('DOMContentLoaded', () => {
    console.log("Usuarios cargados");
    const formulario = document.getElementById('form-editar-abogado');
    if (formulario) formulario.addEventListener('submit', validarAsignacionFormulario);
    ['det-input-nombre', 'det-input-numero-empleado'].forEach(id => {
        document.getElementById(id)?.addEventListener('input', actualizarNombreUsuarioGenerado);
    });
    [
        'det-input-email',
        'det-input-puesto',
        'det-input-telefono-institucional',
        'det-input-telefono-movil',
        'det-input-ubicacion-oficina'
    ].forEach(id => {
        document.getElementById(id)?.addEventListener('input', actualizarLeyendaAutocompletado);
    });
});

let materiasUsuarioActual = [];
let materiasUsuarioEdicion = [];
let usuarioSeleccionadoId = null;
let creandoUsuario = false;

function validarAsignacionFormulario(event) {
    const rol = document.getElementById('det-select-rol')?.value || '';
    const error = document.getElementById('det-asignacion-error');
    if (error) error.classList.add('hidden');

    if (esRolDirectivo(rol)) {
        return; // Directivos no requieren área ni materias
    }

    const gerencias = Array.from(document.getElementById('det-select-usuario-gerencia')?.selectedOptions || []);
    const materias = document.querySelectorAll('#det-lista-materias-checkboxes input[name="materiasIds"]:checked');

    let mensaje = '';
    if (gerencias.length !== 1 || !gerencias[0].value) {
        mensaje = 'Selecciona el área del usuario.';
    } else if ((rol === 'JEFE_DEPTO' || rol === 'ABOGADO') && materias.length === 0) {
        mensaje = 'Selecciona al menos una materia para el usuario.';
    }

    if (mensaje) {
        event.preventDefault();
        if (error) {
            error.innerText = mensaje;
            error.classList.remove('hidden');
            error.focus({ preventScroll: true });
        }
        document.getElementById('det-seccion-asignacion')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
}

function actualizarNombreUsuarioGenerado() {
    if (!creandoUsuario) return;

    const nombre = document.getElementById('det-input-nombre')?.value?.trim() || '';
    const numeroEmpleado = document.getElementById('det-input-numero-empleado')?.value?.trim() || '';
    const rol = document.getElementById('det-select-rol')?.value || '';
    const area = document.getElementById('det-select-usuario-gerencia')?.value || '';
    const inputUsername = document.getElementById('det-input-username');
    if (!inputUsername) return;

    if (!nombre || !numeroEmpleado || !rol || (!esRolDirectivo(rol) && !area)) {
        inputUsername.value = '';
        return;
    }

    inputUsername.value = normalizarNombreUsuario(nombre);
}

function normalizarNombreUsuario(nombre) {
    return nombre
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .toLowerCase()
        .trim()
        .replace(/[^a-z0-9._-]+/g, '.')
        .replace(/\.{2,}/g, '.')
        .replace(/^\.|\.$/g, '')
        .substring(0, 80);
}

function actualizarLeyendaAutocompletado() {
    const email = document.getElementById('det-input-email')?.value?.trim() || '';
    const datosInstitucionales = [
        'det-input-puesto',
        'det-input-telefono-institucional',
        'det-input-telefono-movil',
        'det-input-ubicacion-oficina'
    ];
    const todosVacios = datosInstitucionales.every(id =>
        !(document.getElementById(id)?.value?.trim())
    );
    const leyenda = document.getElementById('det-email-autocomplete-note');
    if (leyenda) leyenda.classList.toggle('hidden', !email || !todosVacios);
}

function mostrarRecuperacionManual(visible) {
    const button = document.getElementById('det-btn-generar-password');
    if (button) button.classList.toggle('hidden', !visible);
}

// --- 1. LÓGICA DE MAESTRO - DETALLE ---

function seleccionarAbogado(cardElement) {
    creandoUsuario = false;
    setModoCreacionUsuario(false);

    // 1. Quitar estado activo de todas las tarjetas
    document.querySelectorAll('.abogado-card').forEach(card => {
        card.classList.remove('bg-red-50', 'border-red-100');
        card.classList.add('border-transparent');
    });

    // 2. Poner estado activo a la tarjeta clickeada
    cardElement.classList.add('bg-red-50', 'border-red-100');
    cardElement.classList.remove('border-transparent');

    // 3. Extraer datos de los atributos data-*
    const id = cardElement.getAttribute('data-id');
    const nombre = cardElement.getAttribute('data-nombre');
    const email = cardElement.getAttribute('data-email');
    const username = cardElement.getAttribute('data-username') || '';
    const numeroEmpleado = cardElement.getAttribute('data-numero-empleado') || '';
    const puesto = cardElement.getAttribute('data-puesto') || '';
    const telefonoInstitucional = cardElement.getAttribute('data-telefono-institucional') || '';
    const telefonoMovil = cardElement.getAttribute('data-telefono-movil') || '';
    const ubicacionOficina = cardElement.getAttribute('data-ubicacion-oficina') || '';
    const rol = cardElement.getAttribute('data-rol');
    const microsoftLoginEnabled = cardElement.getAttribute('data-microsoft-login-enabled') === 'true';
    const gerenciaNombre = cardElement.getAttribute('data-gerencia-nombre');
    const gerenciasIdsStr = cardElement.getAttribute('data-gerencias-ids');
    const gerenciasIdsArray = gerenciasIdsStr ? gerenciasIdsStr.split(',').map(Number) : [];
    const activo = cardElement.getAttribute('data-activo') === 'true';

    const materiasStr = cardElement.getAttribute('data-materias-nombres');
    const materiasNombres = materiasStr ? materiasStr.split('|') : [];

    const materiasIdsStr = cardElement.getAttribute('data-materias-ids');
    const materiasIdsArray = materiasIdsStr ? materiasIdsStr.split(',').map(Number) : [];

    usuarioSeleccionadoId = id;

    // 4. Ocultar estado vacío, mostrar detalle
    document.getElementById('panel-vacio').classList.add('hidden');
    const panelDetalle = document.getElementById('panel-detalle');
    panelDetalle.classList.remove('hidden');

    panelDetalle.classList.remove('animate-fade-in');
    void panelDetalle.offsetWidth; // Trigger reflow
    panelDetalle.classList.add('animate-fade-in');

    // 5. Inyectar datos en el panel derecho (Vista de consulta)
    document.getElementById('det-avatar').innerText = nombre.charAt(0).toUpperCase();
    document.getElementById('det-nombre').innerText = nombre;
    document.getElementById('det-email').innerText = email || 'Sin correo institucional';
    const detIdentity = document.getElementById('det-identity');
    if (detIdentity) {
        detIdentity.innerText = numeroEmpleado ? `${username} · ${numeroEmpleado}` : username;
    }

    // Badges
    let badgesHtml = `<span class="bg-white/10 border border-white/20 text-white text-[10px] font-bold px-2 py-0.5 rounded shadow-sm">${rol}</span>`;
    if (esRolDirectivo(rol)) {
        badgesHtml += `<span class="bg-blue-500/30 border border-blue-400/50 text-blue-100 text-[10px] font-bold px-2 py-0.5 rounded shadow-sm"><i class="fas fa-landmark mr-1"></i>Direccion de Juridico</span>`;
    } else if (gerenciaNombre && gerenciaNombre !== 'Sin Gerencia') {
        badgesHtml += `<span class="bg-white/10 border border-white/20 text-white text-[10px] font-bold px-2 py-0.5 rounded shadow-sm"><i class="fas fa-building mr-1"></i>${gerenciaNombre}</span>`;
    } else {
        badgesHtml += `<span class="bg-white/10 border border-white/20 text-white text-[10px] font-bold px-2 py-0.5 rounded shadow-sm"><i class="fas fa-building mr-1"></i>Sin Gerencia</span>`;
    }

    if (activo) {
        badgesHtml += `<span class="bg-green-500/20 border border-green-400/50 text-green-300 text-[10px] font-bold px-2 py-0.5 rounded shadow-sm"><i class="fas fa-check-circle mr-1"></i>Activo</span>`;
    } else {
        badgesHtml += `<span class="bg-red-500/30 border border-red-400/50 text-red-300 text-[10px] font-bold px-2 py-0.5 rounded shadow-sm"><i class="fas fa-ban mr-1"></i>Inactivo</span>`;
    }
    if (!email) {
        badgesHtml += '<span class="bg-amber-500/20 border border-amber-400/50 text-amber-100 text-[10px] font-bold px-2 py-0.5 rounded shadow-sm"><i class="fas fa-key mr-1"></i>Acceso local</span>';
    } else if (microsoftLoginEnabled) {
        badgesHtml += '<span class="bg-blue-500/20 border border-blue-300/50 text-blue-100 text-[10px] font-bold px-2 py-0.5 rounded shadow-sm"><i class="fab fa-microsoft mr-1"></i>Microsoft vinculado</span>';
    } else {
        badgesHtml += '<span class="bg-amber-500/20 border border-amber-400/50 text-amber-100 text-[10px] font-bold px-2 py-0.5 rounded shadow-sm"><i class="fas fa-envelope mr-1"></i>Activacion pendiente</span>';
    }

    // Materias (solo para operativos)
    if (!esRolDirectivo(rol)) {
        materiasNombres.forEach(m => {
            if (m.trim() !== '') {
                badgesHtml += `<span class="bg-yellow-500/20 border border-yellow-500/50 text-yellow-300 text-[10px] font-bold px-2 py-0.5 rounded shadow-sm">${m}</span>`;
            }
        });
    }

    document.getElementById('det-badges').innerHTML = badgesHtml;

    // 6. Pre-poblar los campos de la pestaña "Editar Perfil"
    const inputDetId = document.getElementById('det-input-id');
    if (inputDetId) {
        document.getElementById('det-input-id').value = id;
        document.getElementById('det-input-nombre').value = nombre;
        document.getElementById('det-input-email').value = email;
        actualizarLeyendaAutocompletado();
        document.getElementById('det-input-username').value = username;
        document.getElementById('det-input-numero-empleado').value = numeroEmpleado;
        const detPuesto = document.getElementById('det-input-puesto');
        if (detPuesto) detPuesto.value = puesto;
        const detTelefonoInstitucional = document.getElementById('det-input-telefono-institucional');
        if (detTelefonoInstitucional) detTelefonoInstitucional.value = telefonoInstitucional;
        const detTelefonoMovil = document.getElementById('det-input-telefono-movil');
        if (detTelefonoMovil) detTelefonoMovil.value = telefonoMovil;
        const detUbicacionOficina = document.getElementById('det-input-ubicacion-oficina');
        if (detUbicacionOficina) detUbicacionOficina.value = ubicacionOficina;
        actualizarLeyendaAutocompletado();
        document.getElementById('det-select-rol').value = rol;
        document.getElementById('det-check-activo').checked = activo;

        // Reset password fields in edit tab
        const detPass = document.getElementById('det-input-password-temporal');
        if (detPass) {
            detPass.value = '';
            detPass.disabled = true;
        }
        const detBadge = document.getElementById('det-badge-pass-generado');
        if (detBadge) detBadge.classList.add('hidden');
        const detSeguridad = document.getElementById('det-seccion-seguridad');
        if (detSeguridad) detSeguridad.classList.remove('hidden');
        const detFormTitulo = document.getElementById('det-form-titulo');
        if (detFormTitulo) detFormTitulo.innerHTML = '<i class="fas fa-id-card mr-2 text-gob-guinda"></i> Datos principales';
        const detSubmitLabel = document.getElementById('det-submit-label');
        if (detSubmitLabel) detSubmitLabel.innerText = 'Guardar Cambios';

        materiasUsuarioEdicion = materiasIdsArray || [];
        const detSelectGerencia = document.getElementById('det-select-usuario-gerencia');
        if (detSelectGerencia) {
            Array.from(detSelectGerencia.options).forEach(opt => {
                opt.selected = gerenciasIdsArray.includes(Number(opt.value));
            });
        }

        actualizarFormularioEdicionPorRol();
        cargarMateriasDeGerenciaEdicion();
    }

    // 7. Configurar botón de estatus
    const btnEstatus = document.getElementById('btn-estatus');
    if (btnEstatus) {
        btnEstatus.innerHTML = activo ? '<i class="fas fa-ban mr-1"></i> Desactivar' : '<i class="fas fa-check-circle mr-1"></i> Activar';
        btnEstatus.className = activo
            ? 'px-4 py-2 bg-white text-red-800 hover:bg-red-50 rounded-lg text-sm font-bold transition-colors border border-red-200 shadow-sm focus-visible:ring-2 focus-visible:ring-white'
            : 'px-4 py-2 bg-white text-emerald-800 hover:bg-emerald-50 rounded-lg text-sm font-bold transition-colors border border-emerald-200 shadow-sm focus-visible:ring-2 focus-visible:ring-white';
        btnEstatus.onclick = () => confirmarCambioEstatus(id);
    }
    if (document.getElementById('tab-btn-editar')) {
        cambiarTabAbogado('editar');
    } else {
        cambiarTabAbogado('casos');
    }

    // Ocultar listado y mostrar detalle en móvil
    const panelLista = document.getElementById('panel-lista');
    const panelDetalleContainer = document.getElementById('panel-detalle-container');
    if (window.innerWidth < 1024) {
        if (panelLista) panelLista.classList.add('hidden');
        if (panelDetalleContainer) panelDetalleContainer.classList.remove('hidden');
    }
}

function iniciarNuevoUsuarioEnDetalle() {
    creandoUsuario = true;
    usuarioSeleccionadoId = null;
    setModoCreacionUsuario(true);
    asegurarTarjetaNuevoUsuario();

    document.querySelectorAll('.abogado-card').forEach(card => {
        card.classList.remove('bg-red-50', 'border-red-100');
        card.classList.add('border-transparent');
    });
    const tempCard = document.getElementById('usuario-card-nuevo');
    if (tempCard) {
        tempCard.classList.add('bg-red-50', 'border-red-100');
        tempCard.classList.remove('border-transparent');
    }

    document.getElementById('panel-vacio').classList.add('hidden');
    const panelDetalle = document.getElementById('panel-detalle');
    panelDetalle.classList.remove('hidden');

    document.getElementById('det-avatar').innerText = '+';
    document.getElementById('det-nombre').innerText = 'Nuevo Usuario';
    document.getElementById('det-identity').innerText = 'Captura los datos para crear el acceso';
    document.getElementById('det-email').innerText = 'Correo institucional opcional';
    document.getElementById('det-badges').innerHTML =
        '<span class="bg-white/10 border border-white/20 text-white text-[10px] font-bold px-2 py-0.5 rounded shadow-sm">ALTA</span>';

    limpiarFormularioDetalleUsuario();
    const selectRol = document.getElementById('det-select-rol');
    if (selectRol && selectRol.options.length > 0) {
        selectRol.selectedIndex = 0;
    }
    const checkActivo = document.getElementById('det-check-activo');
    if (checkActivo) checkActivo.checked = true;
    preseleccionarGerenciaActorEnDetalle();
    materiasUsuarioEdicion = [];

    const detFormTitulo = document.getElementById('det-form-titulo');
    if (detFormTitulo) detFormTitulo.innerHTML = '<i class="fas fa-user-plus mr-2 text-gob-guinda"></i> Datos del Nuevo Usuario';
    const detSubmitLabel = document.getElementById('det-submit-label');
    if (detSubmitLabel) detSubmitLabel.innerText = 'Crear Usuario';

    actualizarFormularioEdicionPorRol();
    actualizarNombreUsuarioGenerado();
    cambiarTabAbogado('editar');

    const panelLista = document.getElementById('panel-lista');
    const panelDetalleContainer = document.getElementById('panel-detalle-container');
    if (window.innerWidth < 1024) {
        if (panelLista) panelLista.classList.add('hidden');
        if (panelDetalleContainer) panelDetalleContainer.classList.remove('hidden');
    }
}

function asegurarTarjetaNuevoUsuario() {
    if (document.getElementById('usuario-card-nuevo')) return;
    const lista = document.getElementById('lista-abogados');
    if (!lista) return;
    const card = document.createElement('button');
    card.id = 'usuario-card-nuevo';
    card.type = 'button';
    card.onclick = iniciarNuevoUsuarioEnDetalle;
    card.className = 'abogado-card w-full text-left p-3 rounded-xl border border-transparent transition-all flex gap-3 group focus:outline-none';
    card.innerHTML = `
        <div class="w-12 h-12 rounded-full bg-gob-guinda flex-shrink-0 flex items-center justify-center text-white font-bold text-lg shadow-sm border-2 border-white">
            <i class="fas fa-plus text-sm"></i>
        </div>
        <div class="flex-1 min-w-0">
            <p class="text-sm font-bold text-gray-800 truncate group-hover:text-gob-guinda transition-colors">Nuevo usuario</p>
            <p class="text-xs text-gray-500 truncate mb-1">Formulario de alta</p>
            <span class="bg-red-50 text-gob-guinda text-[9px] font-extrabold px-1.5 py-0.5 rounded border border-red-100">SIN GUARDAR</span>
        </div>`;
    lista.prepend(card);
}

function limpiarFormularioDetalleUsuario() {
    const ids = [
        'det-input-id',
        'det-input-nombre',
        'det-input-email',
        'det-input-username',
        'det-input-numero-empleado',
        'det-input-puesto',
        'det-input-telefono-institucional',
        'det-input-telefono-movil',
        'det-input-ubicacion-oficina'
    ];
    ids.forEach(id => {
        const input = document.getElementById(id);
        if (input) input.value = '';
    });
    actualizarLeyendaAutocompletado();
    const pass = document.getElementById('det-input-password-temporal');
    if (pass) {
        pass.value = '';
        pass.disabled = true;
    }
    const badge = document.getElementById('det-badge-pass-generado');
    if (badge) badge.classList.add('hidden');
    const selectGerencia = document.getElementById('det-select-usuario-gerencia');
    if (selectGerencia) {
        Array.from(selectGerencia.options).forEach(opt => opt.selected = false);
    }
    const listaMaterias = document.getElementById('det-lista-materias-checkboxes');
    if (listaMaterias) listaMaterias.innerHTML = '';
}

function preseleccionarGerenciaActorEnDetalle() {
    if (!window.usuarioActual || (window.usuarioActual.rol !== 'GERENTE' && window.usuarioActual.rol !== 'JEFE_DEPTO')) {
        return;
    }
    const selectGerencia = document.getElementById('det-select-usuario-gerencia');
    if (!selectGerencia) return;
    Array.from(selectGerencia.options).forEach(opt => {
        opt.selected = Number(opt.value) === Number(window.usuarioActual.gerenciaId);
    });
}

function setModoCreacionUsuario(activo) {
    ['casos', 'terminos', 'historial', 'permisos'].forEach(tab => {
        const btn = document.getElementById('tab-btn-' + tab);
        if (btn) btn.classList.toggle('hidden', activo);
    });
    const btnEstatus = document.getElementById('btn-estatus');
    if (btnEstatus) btnEstatus.classList.toggle('hidden', activo);
    const btnEliminar = document.getElementById('det-btn-eliminar-usuario');
    if (btnEliminar) btnEliminar.classList.toggle('hidden', activo);
    const btnCancelar = document.getElementById('det-btn-cancelar-creacion');
    if (btnCancelar) btnCancelar.classList.toggle('hidden', !activo);
    const seccionSeguridad = document.getElementById('det-seccion-seguridad');
    if (seccionSeguridad) seccionSeguridad.classList.toggle('hidden', activo);
}

function cancelarNuevoUsuarioEnDetalle() {
    creandoUsuario = false;
    usuarioSeleccionadoId = null;
    setModoCreacionUsuario(false);
    const tempCard = document.getElementById('usuario-card-nuevo');
    if (tempCard) tempCard.remove();
    document.querySelectorAll('.abogado-card').forEach(card => {
        card.classList.remove('bg-red-50', 'border-red-100');
        card.classList.add('border-transparent');
    });
    const panelDetalle = document.getElementById('panel-detalle');
    const panelVacio = document.getElementById('panel-vacio');
    if (panelDetalle) panelDetalle.classList.add('hidden');
    if (panelVacio) panelVacio.classList.remove('hidden');
}

function regresarALista() {
    const panelLista = document.getElementById('panel-lista');
    const panelDetalleContainer = document.getElementById('panel-detalle-container');

    if (panelLista) panelLista.classList.remove('hidden');
    if (panelDetalleContainer) panelDetalleContainer.classList.add('hidden');

    // Limpiar el estado activo de las tarjetas
    document.querySelectorAll('.abogado-card').forEach(card => {
        card.classList.remove('bg-red-50', 'border-red-100');
        card.classList.add('border-transparent');
    });
}

// --- 2. LÓGICA DE MODALES ---

function abrirModalUsuario() {
    const titulo = document.getElementById('titulo-modal-usuario');
    if (titulo) titulo.innerHTML = '<i class="fas fa-user-plus mr-2"></i> Nuevo Usuario';

    const inputId = document.getElementById('input-usuario-id');
    if (inputId) inputId.value = '';

    const inputNombre = document.getElementById('input-usuario-nombre');
    if (inputNombre) inputNombre.value = '';

    const inputEmail = document.getElementById('input-usuario-email');
    if (inputEmail) inputEmail.value = '';

    const inputUsername = document.getElementById('input-usuario-username');
    if (inputUsername) inputUsername.value = '';

    const inputNumeroEmpleado = document.getElementById('input-usuario-numero-empleado');
    if (inputNumeroEmpleado) inputNumeroEmpleado.value = '';

    const inputPuesto = document.getElementById('input-usuario-puesto');
    if (inputPuesto) inputPuesto.value = '';

    const inputTelefonoInstitucional = document.getElementById('input-usuario-telefono-institucional');
    if (inputTelefonoInstitucional) inputTelefonoInstitucional.value = '';

    const inputTelefonoMovil = document.getElementById('input-usuario-telefono-movil');
    if (inputTelefonoMovil) inputTelefonoMovil.value = '';

    const inputUbicacionOficina = document.getElementById('input-usuario-ubicacion-oficina');
    if (inputUbicacionOficina) inputUbicacionOficina.value = '';

    const selectRol = document.getElementById('select-usuario-rol');
    if (selectRol) selectRol.value = '';

    // Reset multiple select
    const selectGerencia = document.getElementById('select-usuario-gerencia');
    if (selectGerencia) {
        Array.from(selectGerencia.options).forEach(opt => opt.selected = false);
    }
    if (window.usuarioActual && (window.usuarioActual.rol === 'GERENTE' || window.usuarioActual.rol === 'JEFE_DEPTO')) {
        const creatorGerId = window.usuarioActual.gerenciaId;
        if (selectGerencia) {
            Array.from(selectGerencia.options).forEach(opt => {
                if (Number(opt.value) === Number(creatorGerId)) {
                    opt.selected = true;
                }
            });
        }
    }

    const checkActivo = document.getElementById('check-usuario-activo');
    if (checkActivo) checkActivo.checked = true;

    // CREACIÓN
    const bloqueNuevo = document.getElementById('bloque-password-nuevo');
    const bloqueTemporal = document.getElementById('bloque-password-temporal');
    const inputPass = document.getElementById('input-usuario-password');
    if (bloqueNuevo) { bloqueNuevo.classList.remove('hidden'); }
    if (bloqueTemporal) { bloqueTemporal.classList.add('hidden'); }
    if (inputPass) { inputPass.value = ''; inputPass.required = true; inputPass.placeholder = 'Contraseña inicial'; inputPass.disabled = false; }

    const hiddenPass = document.getElementById('input-password-temporal');
    const badge = document.getElementById('badge-pass-generado');
    if (hiddenPass) {
        hiddenPass.value = '';
        hiddenPass.disabled = true;
    }
    if (badge) badge.classList.add('hidden');

    const fpcOld = document.getElementById('input-force-password-change');
    if (fpcOld) fpcOld.remove();

    materiasUsuarioActual = [];

    const contMaterias = document.getElementById('contenedor-materias');
    if (contMaterias) contMaterias.classList.add('hidden');

    const listaMaterias = document.getElementById('lista-materias-checkboxes');
    if (listaMaterias) listaMaterias.innerHTML = '';

    const seccionAsignacion = document.getElementById('seccion-asignacion');
    if (seccionAsignacion) seccionAsignacion.classList.remove('hidden');

    const btnEliminar = document.getElementById('btn-eliminar-usuario');
    if (btnEliminar) btnEliminar.classList.add('hidden');

    actualizarFormularioPorRol();
    cargarMateriasDeGerencia();

    const modal = document.getElementById('modal-usuario');
    if (modal) {
        modal.classList.remove('hidden');
        modal.classList.add('flex');
    }
}

function actualizarFormularioPorRol() {
    const selectRol = document.getElementById('select-usuario-rol');
    if (!selectRol) return;
    const rol = selectRol.value;
    const seccionAsignacion = document.getElementById('seccion-asignacion');
    const divGerencia = document.getElementById('div-gerencia-asignada');
    const contenedorMaterias = document.getElementById('contenedor-materias');

    if (esRolDirectivo(rol)) {
        if (seccionAsignacion) seccionAsignacion.classList.add('hidden');
        if (divGerencia) divGerencia.classList.add('hidden');
        if (contenedorMaterias) contenedorMaterias.classList.add('hidden');
    } else {
        if (seccionAsignacion) seccionAsignacion.classList.remove('hidden');
        const creadorRol = window.usuarioActual ? window.usuarioActual.rol : '';
        if (creadorRol !== 'GERENTE' && creadorRol !== 'JEFE_DEPTO') {
            if (divGerencia) divGerencia.classList.remove('hidden');
        }

        const selectGerencias = document.getElementById('select-usuario-gerencia');
        const checkedGerencias = selectGerencias ? Array.from(selectGerencias.selectedOptions).map(opt => opt.value) : [];

        if (checkedGerencias.length > 0) {
            if (contenedorMaterias) contenedorMaterias.classList.remove('hidden');
        } else {
            if (contenedorMaterias) contenedorMaterias.classList.add('hidden');
        }
    }
    cargarMateriasDeGerencia();
}

function cargarMateriasDeGerencia(event) {
    const selectRol = document.getElementById('select-usuario-rol');
    const rol = selectRol ? selectRol.value : '';
    if (rol === 'ADMIN' || rol === 'DIRECTOR_PRINCIPAL' || rol === 'DIRECTOR' || rol === 'SUBDIRECTOR') {
        const contenedor = document.getElementById('contenedor-materias');
        if (contenedor) contenedor.classList.add('hidden');
        return;
    }
    const selectGerencias = document.getElementById('select-usuario-gerencia');
    if (selectGerencias && (rol === 'GERENTE' || rol === 'JEFE_DEPTO' || rol === 'ABOGADO')) {
        const selectedOptions = Array.from(selectGerencias.selectedOptions);
        if (selectedOptions.length > 1) {
            const lastSelected = selectedOptions[selectedOptions.length - 1];
            Array.from(selectGerencias.options).forEach(opt => {
                opt.selected = (opt === lastSelected);
            });
            materiasUsuarioActual = [];
        }
    }
    const checkedGerencias = selectGerencias ? Array.from(selectGerencias.selectedOptions).map(opt => opt.value) : [];
    const contenedor = document.getElementById('contenedor-materias');
    const lista = document.getElementById('lista-materias-checkboxes');

    if (checkedGerencias.length === 0) {
        if (contenedor) contenedor.classList.add('hidden');
        if (lista) lista.innerHTML = '';
        return;
    }

    if (lista) lista.innerHTML = '<p class="text-xs text-gray-400">Cargando...</p>';
    if (contenedor) contenedor.classList.remove('hidden');

    const fetchPromises = checkedGerencias.map(gerenciaId =>
        fetch(`/usuarios/api/materias-por-gerencia/${gerenciaId}`).then(res => res.json())
    );

    Promise.all(fetchPromises)
        .then(results => {
            const todasMaterias = [];
            const idsVistos = new Set();
            results.forEach(materias => {
                materias.forEach(m => {
                    if (!idsVistos.has(m.id)) {
                        idsVistos.add(m.id);
                        todasMaterias.push(m);
                    }
                });
            });

            if (!lista) return;
            lista.innerHTML = '';
            if (todasMaterias.length === 0) {
                lista.innerHTML = '<p class="text-xs text-red-400">Sin materias.</p>';
                return;
            }
            todasMaterias.forEach(m => {
                const isGerente = (rol === 'GERENTE');
                const isChecked = (isGerente || materiasUsuarioActual.includes(m.id)) ? 'checked' : '';
                const clickAttr = isGerente ? 'onclick="return false;" style="pointer-events: none; opacity: 0.7;"' : '';
                lista.innerHTML += `
                    <div class="flex items-center space-x-2">
                        <input type="checkbox" name="materiasIds" value="${m.id}" id="mat-${m.id}" ${isChecked} ${clickAttr} class="rounded text-gob-guinda h-4 w-4 border-gray-300 focus:ring-gob-oro">
                        <label for="mat-${m.id}" class="text-xs text-gray-700 cursor-pointer select-none">${m.nombre}</label>
                    </div>`;
            });
        })
        .catch(err => {
            console.error("Error al cargar materias:", err);
            if (lista) lista.innerHTML = '<p class="text-xs text-red-400">Error al cargar materias.</p>';
        });
}

function editarUsuario(id, nombre, email, rol, gerenciaIdsArray, activo, materiasIdsArray, username = '', numeroEmpleado = '', puesto = '', telefonoInstitucional = '', telefonoMovil = '', ubicacionOficina = '') {
    abrirModalUsuario();

    const titulo = document.getElementById('titulo-modal-usuario');
    if (titulo) titulo.innerHTML = '<i class="fas fa-user-edit mr-2"></i> Editar Usuario';

    const inputId = document.getElementById('input-usuario-id');
    if (inputId) inputId.value = id;

    const inputNombre = document.getElementById('input-usuario-nombre');
    if (inputNombre) inputNombre.value = nombre;

    const inputEmail = document.getElementById('input-usuario-email');
    if (inputEmail) inputEmail.value = email;

    const inputUsername = document.getElementById('input-usuario-username');
    if (inputUsername) inputUsername.value = username;

    const inputNumeroEmpleado = document.getElementById('input-usuario-numero-empleado');
    if (inputNumeroEmpleado) inputNumeroEmpleado.value = numeroEmpleado;

    const inputPuesto = document.getElementById('input-usuario-puesto');
    if (inputPuesto) inputPuesto.value = puesto;

    const inputTelefonoInstitucional = document.getElementById('input-usuario-telefono-institucional');
    if (inputTelefonoInstitucional) inputTelefonoInstitucional.value = telefonoInstitucional;

    const inputTelefonoMovil = document.getElementById('input-usuario-telefono-movil');
    if (inputTelefonoMovil) inputTelefonoMovil.value = telefonoMovil;

    const inputUbicacionOficina = document.getElementById('input-usuario-ubicacion-oficina');
    if (inputUbicacionOficina) inputUbicacionOficina.value = ubicacionOficina;

    const selectRol = document.getElementById('select-usuario-rol');
    if (selectRol) selectRol.value = rol;

    const checkActivo = document.getElementById('check-usuario-activo');
    if (checkActivo) checkActivo.checked = activo;

    // EDICIÓN
    const bloqueNuevo = document.getElementById('bloque-password-nuevo');
    const bloqueTemporal = document.getElementById('bloque-password-temporal');
    const inputPass = document.getElementById('input-usuario-password');
    if (bloqueNuevo) { bloqueNuevo.classList.add('hidden'); }
    if (bloqueTemporal) { bloqueTemporal.classList.remove('hidden'); }
    if (inputPass) { inputPass.required = false; inputPass.value = ''; inputPass.disabled = true; }

    const hiddenPass = document.getElementById('input-password-temporal');
    const badge = document.getElementById('badge-pass-generado');
    if (hiddenPass) {
        hiddenPass.value = '';
        hiddenPass.disabled = false;
    }
    if (badge) badge.classList.add('hidden');

    materiasUsuarioActual = materiasIdsArray || [];

    const selectGerencia = document.getElementById('select-usuario-gerencia');
    if (selectGerencia) {
        Array.from(selectGerencia.options).forEach(opt => {
            opt.selected = (gerenciaIdsArray || []).includes(Number(opt.value));
        });
    }

    actualizarFormularioPorRol();
    cargarMateriasDeGerencia();

    const btnEliminar = document.getElementById('btn-eliminar-usuario');
    if (btnEliminar) btnEliminar.classList.remove('hidden');
}

// -- MODAL DE CONTRASEÑA TEMPORAL ------------------------------------------
function abrirModalPasswordTemporal() {
    Swal.fire({
        width: '36rem',
        title: '<i class="fas fa-paper-plane text-gob-guinda mr-2"></i>Usa el enlace de activacion',
        text: 'La creacion de usuarios ahora envia un enlace al correo institucional registrado. No se generan contrasenas temporales.',
        icon: 'info',
        confirmButtonText: 'Entendido',
        confirmButtonColor: '#691228',
        customClass: { title: 'text-3xl font-bold pt-6', confirmButton: 'px-8 py-3 text-lg rounded-lg' }
    });
}

function ejecutarCopiado(text) {
    if (navigator.clipboard && navigator.clipboard.writeText) {
        return navigator.clipboard.writeText(text);
    } else {
        return new Promise((resolve, reject) => {
            try {
                const textArea = document.createElement("textarea");
                textArea.value = text;
                textArea.style.position = "fixed";
                textArea.style.top = "0";
                textArea.style.left = "0";
                textArea.style.opacity = "0";
                document.body.appendChild(textArea);
                textArea.focus();
                textArea.select();
                const successful = document.execCommand('copy');
                document.body.removeChild(textArea);
                if (successful) {
                    resolve();
                } else {
                    reject(new Error("Copiado fallido"));
                }
            } catch (err) {
                reject(err);
            }
        });
    }
}

function copiarPasswordTemporal(pass) {
    ejecutarCopiado(pass).then(() => {
        const btn = document.querySelector('#swal-pass-code + button') ||
            document.querySelector('[onclick*="copiarPasswordTemporal"]');
        if (btn) {
            const original = btn.innerHTML;
            btn.innerHTML = '<i class="fas fa-check mr-1"></i>¡Copiado!';
            btn.classList.add('bg-green-600');
            btn.classList.remove('bg-gob-guinda');
            setTimeout(() => { btn.innerHTML = original; btn.classList.remove('bg-green-600'); btn.classList.add('bg-gob-guinda'); }, 1500);
        }
    }).catch((err) => { console.error("Error al copiar:", err); });
}

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

function eliminarUsuarioDesdeModal() {
    const id = document.getElementById('input-usuario-id').value;
    if (!id) return;
    abrirModalConfirmarGenerico({
        titulo: '¿Eliminar usuario?',
        texto: 'Esta acción no se puede deshacer.',
        advertencia: 'Se borrarán los accesos y registros asociados permanentemente.',
        esPeligro: true,
        callback: () => {
            window.location.href = `/usuarios/eliminar/${id}`;
        }
    });
}

function confirmarCambioEstatus(id) {
    const urlParams = new URLSearchParams(window.location.search);
    const estadoActual = urlParams.get('estado') || 'activos';
    abrirModalConfirmarGenerico({
        titulo: '¿Cambiar acceso al sistema?',
        texto: 'El usuario perderá/recuperará acceso a la plataforma.',
        advertencia: 'El cambio afectará la disponibilidad del registro en el sistema.',
        esPeligro: false,
        callback: () => {
            window.location.href = `/usuarios/toggle/${id}?estado=${encodeURIComponent(estadoActual)}`;
        }
    });
}

// --- 3. BÚSQUEDA SERVER-SIDE ---

// --- 4. NAVEGACIÓN Y CARGA DE DATOS (PESTAÑAS) ---

function cambiarTabAbogado(tabName) {
    // 1. Ocultar todos y mostrar el activo
    document.querySelectorAll('.tab-abogado-content').forEach(el => { el.classList.add('hidden'); el.classList.remove('block'); });
    const contenidoActivo = document.getElementById('tab-content-' + tabName);
    if (contenidoActivo) { contenidoActivo.classList.remove('hidden'); contenidoActivo.classList.add('block'); }

    // 2. Estilos de los botones
    document.querySelectorAll('.tab-abogado').forEach(btn => {
        btn.classList.remove('border-gob-guinda', 'text-gob-guinda', 'font-bold');
        btn.classList.add('border-transparent', 'text-gray-500', 'font-medium');
    });
    const botonActivo = document.getElementById('tab-btn-' + tabName);
    if (botonActivo) {
        botonActivo.classList.remove('border-transparent', 'text-gray-500', 'font-medium');
        botonActivo.classList.add('border-gob-guinda', 'text-gob-guinda', 'font-bold');
    }

    // 3. CARGA DINÁMICA DE DATOS SEGÚN LA PESTAÑA
    if (!usuarioSeleccionadoId || creandoUsuario) return;

    if (tabName === 'casos') {
        cargarCasosAbogado(usuarioSeleccionadoId);
    } else if (tabName === 'terminos') {
        cargarAgendaAbogado(usuarioSeleccionadoId);
    } else if (tabName === 'historial') {
        cargarHistorialAbogado(usuarioSeleccionadoId);
    }
}

function cargarCasosAbogado(idUsuario) {
    const tbody = document.getElementById('tbody-casos');
    document.getElementById('container-tabla-casos').classList.add('hidden');
    document.getElementById('empty-casos').classList.add('hidden');
    document.getElementById('loading-casos').classList.remove('hidden');

    fetch(`/usuarios/api/${idUsuario}/casos`)
        .then(res => res.json())
        .then(data => {
            document.getElementById('loading-casos').classList.add('hidden');
            if (data.length === 0) {
                document.getElementById('empty-casos').classList.remove('hidden');
            } else {
                tbody.innerHTML = '';
                data.forEach(caso => {
                    let estatusClass = 'bg-gray-100 text-gray-800';
                    if (caso.estatus === 'TRAMITE') estatusClass = 'bg-blue-100 text-blue-800';
                    else if (caso.estatus === 'LAUDO') estatusClass = 'bg-yellow-100 text-yellow-800';
                    else if (caso.estatus === 'FIRME') estatusClass = 'bg-green-100 text-green-800';
                    else if (caso.estatus === 'CONCLUIDO') estatusClass = 'bg-gray-200 text-gray-700';

                    tbody.innerHTML += `
                        <tr class="hover:bg-gray-50 transition-colors border-b border-gray-100">
                            <td class="px-6 py-4">
                                <p class="font-bold text-slate-800">${caso.numero}</p>
                                <p class="text-xs text-slate-500 truncate max-w-xs" title="${caso.asunto}">${caso.asunto}</p>
                            </td>
                            <td class="px-6 py-4 text-slate-700 font-medium">${caso.cliente}</td>
                            <td class="px-6 py-4"><span class="${estatusClass} text-[10px] font-bold px-2 py-0.5 rounded border border-black/5">${caso.estatus}</span></td>
                            <td class="px-6 py-4 text-right text-slate-600 font-medium">${caso.fecha}</td>
                        </tr>
                    `;
                });
                document.getElementById('container-tabla-casos').classList.remove('hidden');
            }
        }).catch(err => console.error(err));
}

function cargarAgendaAbogado(idUsuario) {
    const tbody = document.getElementById('tbody-terminos');
    document.getElementById('container-tabla-terminos').classList.add('hidden');
    document.getElementById('empty-terminos').classList.add('hidden');
    document.getElementById('loading-terminos').classList.remove('hidden');

    fetch(`/usuarios/api/${idUsuario}/agenda`)
        .then(res => res.json())
        .then(data => {
            document.getElementById('loading-terminos').classList.add('hidden');
            if (data.length === 0) {
                document.getElementById('empty-terminos').classList.remove('hidden');
            } else {
                tbody.innerHTML = '';
                data.forEach(item => {
                    let icon = item.tipo === 'Audiencia' ? '<i class="fas fa-gavel text-gob-oro mr-2"></i>' : '<i class="fas fa-hourglass-half text-gob-guinda mr-2"></i>';
                    let typeBadge = item.tipo === 'Audiencia' ? 'bg-amber-100 text-amber-800' : 'bg-rose-100 text-rose-800';

                    tbody.innerHTML += `
                        <tr class="hover:bg-gray-50 transition-colors border-b border-gray-100">
                            <td class="px-6 py-4 font-bold text-slate-800">
                                <span class="${typeBadge} px-2 py-1 rounded text-[10px] uppercase tracking-wider border border-current/10">${icon} ${item.tipo}</span>
                            </td>
                            <td class="px-6 py-4 text-slate-600 text-sm">${item.descripcion}</td>
                            <td class="px-6 py-4 font-bold text-gob-guinda text-sm">${item.fecha}</td>
                        </tr>
                    `;
                });
                document.getElementById('container-tabla-terminos').classList.remove('hidden');
            }
        }).catch(err => console.error(err));
}

let historialDatosCargados = [];
let historialFiltroActual = 'TODOS';

function cargarHistorialAbogado(idUsuario) {
    const container = document.getElementById('container-historial');
    document.getElementById('container-historial').classList.add('hidden');
    document.getElementById('empty-historial').classList.add('hidden');
    document.getElementById('loading-historial').classList.remove('hidden');

    document.querySelectorAll('.btn-filtro-historial').forEach(btn => {
        btn.className = "btn-filtro-historial px-3 py-1.5 text-xs rounded-full bg-white text-slate-600 font-medium border border-slate-200 hover:bg-slate-50 transition-colors";
    });
    const defaultBtn = document.querySelector('.btn-filtro-historial[onclick*="TODOS"]');
    if (defaultBtn) {
        defaultBtn.className = "btn-filtro-historial active px-3 py-1.5 text-xs rounded-full bg-slate-200 text-slate-700 font-bold border border-transparent shadow-sm transition-colors";
    }
    historialFiltroActual = 'TODOS';

    fetch(`/usuarios/api/${idUsuario}/historial`)
        .then(res => {
            if (!res.ok) throw new Error(`No fue posible cargar el historial (${res.status}).`);
            return res.json();
        })
        .then(data => {
            document.getElementById('loading-historial').classList.add('hidden');
            historialDatosCargados = data;
            renderizarHistorial(data);
        }).catch(err => {
            console.error(err);
            document.getElementById('loading-historial').classList.add('hidden');
            mostrarHistorialVacio('No se pudo cargar el historial', 'Actualiza la página o intenta nuevamente.');
        });
}

function mostrarHistorialVacio(titulo, mensaje) {
    const container = document.getElementById('container-historial');
    const empty = document.getElementById('empty-historial');
    container.innerHTML = '';
    container.classList.add('hidden');
    document.getElementById('empty-historial-title').innerText = titulo;
    document.getElementById('empty-historial-message').innerText = mensaje;
    empty.classList.remove('hidden');
}

function escaparHtml(valor) {
    const elemento = document.createElement('span');
    elemento.textContent = valor == null ? '' : String(valor);
    return elemento.innerHTML;
}

function renderizarHistorial(data) {
    const container = document.getElementById('container-historial');
    if (!data || data.length === 0) {
        const etiqueta = historialFiltroActual === 'ASIGNACION' ? 'asignaciones'
            : historialFiltroActual === 'DOCUMENTAL' ? 'documentos'
            : historialFiltroActual === 'SEGURIDAD' ? 'eventos de seguridad'
            : historialFiltroActual === 'ACCIONES' ? 'acciones'
            : 'actividad';
        const mensaje = categoria === 'asignacion'
            ? 'No hay asignaciones registradas para este usuario.'
            : `No hay ${etiqueta} registrados para este usuario.`;
        mostrarHistorialVacio(`Sin ${etiqueta}`, mensaje);
        return;
    }

    document.getElementById('empty-historial').classList.add('hidden');
    container.innerHTML = '';

    data.forEach(act => {
        let iconClass = 'fa-edit';
        let bgClass = 'bg-gray-50 text-gray-600 border-gray-100';

        const cat = act.categoria || 'ACCIONES';
        if (cat === 'SEGURIDAD') {
            iconClass = 'fa-shield-alt';
            bgClass = 'bg-blue-50 text-blue-600 border-blue-100';
        } else if (cat === 'ASIGNACION') {
            iconClass = 'fa-user-tag';
            bgClass = 'bg-purple-50 text-purple-600 border-purple-100';
        } else if (cat === 'DOCUMENTAL') {
            iconClass = 'fa-file-signature';
            bgClass = 'bg-emerald-50 text-emerald-600 border-emerald-100';
        } else if (cat === 'ACCIONES') {
            iconClass = 'fa-gavel';
            bgClass = 'bg-amber-50 text-amber-600 border-amber-100';
        }

        container.innerHTML += `
            <div class="flex gap-4 p-4 bg-white rounded-xl border border-gray-100 shadow-sm hover:shadow-md transition-shadow">
                <div class="flex-shrink-0 w-10 h-10 ${bgClass} rounded-full flex items-center justify-center border">
                    <i class="fas ${iconClass} text-sm"></i>
                </div>
                <div class="flex-1 min-w-0">
                    <div class="flex justify-between items-start">
                        <p class="text-sm font-bold text-slate-800 truncate">${escaparHtml(act.accion)}</p>
                        <span class="text-[10px] text-slate-400 whitespace-nowrap ml-2">${escaparHtml(act.fecha)}</span>
                    </div>
                    <p class="text-xs text-slate-600 mt-1">${escaparHtml(act.detalles)}</p>
                    ${act.expediente !== 'General' ? `
                    <p class="text-xs font-medium text-gob-guinda flex items-center gap-1 mt-2">
                        <i class="fas fa-file-alt"></i> Exp: ${escaparHtml(act.expediente)}
                    </p>` : ''}
                </div>
            </div>
        `;
    });
    container.classList.remove('hidden');
}

function filtrarHistorial(categoria) {
    historialFiltroActual = categoria;
    document.querySelectorAll('.btn-filtro-historial').forEach(btn => {
        if (btn.getAttribute('onclick').includes(`'${categoria}'`)) {
            btn.className = "btn-filtro-historial active px-3 py-1.5 text-xs rounded-full bg-slate-200 text-slate-700 font-bold border border-transparent shadow-sm transition-colors";
        } else {
            btn.className = "btn-filtro-historial px-3 py-1.5 text-xs rounded-full bg-white text-slate-600 font-medium border border-slate-200 hover:bg-slate-50 transition-colors";
        }
    });

    if (categoria === 'TODOS') {
        renderizarHistorial(historialDatosCargados);
    } else {
        const filtrados = historialDatosCargados.filter(act => (act.categoria || 'ACCIONES') === categoria);
        renderizarHistorial(filtrados);
    }
}

// --- 5. LÓGICA DE EDICIÓN EN LA PESTAÑA "EDITAR PERFIL" ---

function actualizarFormularioEdicionPorRol() {
    const selectRol = document.getElementById('det-select-rol');
    if (!selectRol) return;
    const rol = selectRol.value;
    const divGerencia = document.getElementById('det-div-gerencia-asignada');
    const selectGerencias = document.getElementById('det-select-usuario-gerencia');
    const contenedorMaterias = document.getElementById('det-contenedor-materias');
    const seccionAsignacion = document.getElementById('det-seccion-asignacion');

    if (esRolDirectivo(rol)) {
        if (divGerencia) divGerencia.classList.add('hidden');
        if (contenedorMaterias) contenedorMaterias.classList.add('hidden');
        if (seccionAsignacion) seccionAsignacion.classList.add('hidden');
        if (selectGerencias) {
            selectGerencias.required = false;
            selectGerencias.value = '';
        }
    } else {
        if (divGerencia) divGerencia.classList.remove('hidden');
        if (seccionAsignacion) seccionAsignacion.classList.remove('hidden');
        if (selectGerencias) selectGerencias.required = true;
        const selectedGerencias = selectGerencias
            ? Array.from(selectGerencias.selectedOptions).filter(opt => opt.value)
            : [];
        if (selectedGerencias.length > 0) {
            if (contenedorMaterias) contenedorMaterias.classList.remove('hidden');
        } else {
            if (contenedorMaterias) contenedorMaterias.classList.add('hidden');
        }
    }
    cargarMateriasDeGerenciaEdicion();
}

function cargarMateriasDeGerenciaEdicion(event) {
    const selectRol = document.getElementById('det-select-rol');
    const rol = selectRol ? selectRol.value : '';
    if (rol === 'ADMIN' || rol === 'DIRECTOR_PRINCIPAL' || rol === 'DIRECTOR' || rol === 'SUBDIRECTOR') {
        const contenedor = document.getElementById('det-contenedor-materias');
        if (contenedor) contenedor.classList.add('hidden');
        return;
    }

    const selectGerencias = document.getElementById('det-select-usuario-gerencia');
    if (selectGerencias && (rol === 'GERENTE' || rol === 'JEFE_DEPTO' || rol === 'ABOGADO')) {
        const selectedOptions = Array.from(selectGerencias.selectedOptions);
        if (selectedOptions.length > 1) {
            const lastSelected = selectedOptions[selectedOptions.length - 1];
            Array.from(selectGerencias.options).forEach(opt => {
                opt.selected = (opt === lastSelected);
            });
            materiasUsuarioEdicion = [];
        }
    }
    if (event && event.target === selectGerencias) {
        materiasUsuarioEdicion = [];
    }

    const checkedGerencias = selectGerencias
        ? Array.from(selectGerencias.selectedOptions).map(opt => opt.value).filter(Boolean)
        : [];
    const contenedor = document.getElementById('det-contenedor-materias');
    const lista = document.getElementById('det-lista-materias-checkboxes');

    if (checkedGerencias.length === 0) {
        if (contenedor) contenedor.classList.add('hidden');
        if (lista) lista.innerHTML = '';
        return;
    }

    if (lista) lista.innerHTML = '<p class="text-xs text-gray-400">Cargando...</p>';
    if (contenedor) contenedor.classList.remove('hidden');

    const fetchPromises = checkedGerencias.map(gerenciaId =>
        fetch(`/usuarios/api/materias-por-gerencia/${gerenciaId}`).then(res => res.json())
    );

    Promise.all(fetchPromises)
        .then(results => {
            const todasMaterias = [];
            const idsVistos = new Set();
            results.forEach(materias => {
                materias.forEach(m => {
                    if (!idsVistos.has(m.id)) {
                        idsVistos.add(m.id);
                        todasMaterias.push(m);
                    }
                });
            });

            if (!lista) return;
            lista.innerHTML = '';
            if (todasMaterias.length === 0) {
                lista.innerHTML = '<p class="text-xs text-red-400">Sin materias disponibles.</p>';
                return;
            }
            todasMaterias.forEach(m => {
                const isGerente = (rol === 'GERENTE');
                const isChecked = (isGerente || materiasUsuarioEdicion.includes(m.id)) ? 'checked' : '';
                const clickAttr = isGerente ? 'onclick="return false;" style="pointer-events: none; opacity: 0.7;"' : '';
                lista.innerHTML += `
                    <div class="flex items-center space-x-2">
                        <input type="checkbox" name="materiasIds" value="${m.id}" id="det-mat-${m.id}" ${isChecked} ${clickAttr} class="rounded text-gob-guinda h-4 w-4 border-gray-300 focus:ring-gob-oro">
                        <label for="det-mat-${m.id}" class="text-xs text-gray-700 cursor-pointer select-none">${m.nombre}</label>
                    </div>`;
            });
        })
        .catch(err => {
            console.error("Error al cargar materias en edición:", err);
            if (lista) lista.innerHTML = '<p class="text-xs text-red-400">Error al cargar materias.</p>';
        });
}

function abrirModalPasswordTemporalEdicionLegacy() {
    if (window.emailMaintenance) {
        Swal.fire({
            title: 'Correo en mantenimiento',
            text: window.emailMaintenanceMessage || 'Por ahora no es posible enviar enlaces por correo.',
            icon: 'warning',
            confirmButtonColor: '#691228'
        });
        return;
    }
    Swal.fire({
        width: '36rem',
        title: '<i class="fas fa-paper-plane text-gob-guinda mr-2"></i>Enviar enlace de restablecimiento',
        html: `
            <p class="text-sm text-gray-600 mb-6">Se enviará al correo institucional registrado el mismo enlace que recibiría el usuario desde Recuperar contraseña.</p>
            <div class="flex items-center gap-6 bg-gray-50 border border-gray-200 rounded-lg px-4 py-6">
                <i class="fas fa-envelope text-gob-guinda text-lg"></i>
                <span class="text-sm font-bold text-gray-700">El usuario podrá crear una nueva contraseña segura desde ese enlace.</span>
            </div>
            <p class="text-[10px] text-gray-400 mt-2">No se genera ni se muestra ninguna contraseña temporal.</p>
        `,
        showCancelButton: true,
        confirmButtonText: '<i class="fas fa-paper-plane mr-1"></i> Enviar enlace',
        cancelButtonText: 'Cancelar',
        confirmButtonColor: '#691228',
        cancelButtonColor: '#808590ff',
        focusConfirm: false,
        customClass: {
            title: 'text-3xl font-bold pt-6',
            confirmButton: 'px-8 py-3 text-lg rounded-lg',
            cancelButton: 'px-8 py-3 text-lg rounded-lg',
        }
    }).then(result => {
        if (result.isConfirmed) {
            const userId = document.getElementById('det-input-id').value;
            if (!userId) {
                Swal.fire('Error', 'No se ha podido identificar al usuario seleccionado.', 'error');
                return;
            }

            Swal.fire({
                title: 'Enviando enlace seguro...',
                html: 'Estamos preparando el enlace y enviandolo al correo registrado.',
                allowOutsideClick: false,
                allowEscapeKey: false,
                didOpen: () => {
                    Swal.showLoading();
                }
            });

            const formData = new FormData();
            formData.append('id', userId);

            fetch('/usuarios/api/password-temporal', {
                method: 'POST',
                body: formData
            })
                .then(res => {
                    if (!res.ok) {
                        return res.json().then(data => {
                            throw new Error(data.mensaje || 'Error al enviar el enlace.');
                        });
                    }
                    return res.json();
                })
                .then(data => {
                    const badge = document.getElementById('det-badge-pass-generado');
                    if (badge) {
                        badge.innerHTML = '<i class="fas fa-check-circle text-sm"></i> Enlace enviado al correo registrado';
                        badge.classList.remove('hidden');
                    }
                    Swal.fire({
                        title: 'Enlace enviado',
                        text: data.mensaje || 'Se envio el enlace de restablecimiento al correo registrado.',
                        icon: 'success',
                        confirmButtonColor: '#691228'
                    });
                })
                .catch(err => {
                    Swal.fire({
                        title: 'Error',
                        text: err.message || 'No se pudo enviar el enlace de restablecimiento.',
                        icon: 'error',
                        confirmButtonColor: '#691228'
                    });
                });
        }
    });
}

function abrirModalRecuperacionManualEdicion() {
    Swal.fire({
        width: '36rem',
        title: '<i class="fas fa-shield-alt text-gob-guinda mr-2"></i>Codigo de recuperacion manual',
        html: `
            <p class="text-sm text-gray-600 mb-4">El usuario recibira un codigo de un uso y establecera personalmente su nueva contrasena.</p>
            <div class="bg-gray-50 border border-gray-200 rounded-lg px-4 py-4 text-left">
                <label for="swal-motivo-recuperacion" class="block text-xs font-bold text-gray-500 uppercase mb-1">Motivo</label>
                <textarea id="swal-motivo-recuperacion" minlength="10" required rows="3"
                    class="w-full border border-gray-300 rounded-lg p-2.5 text-sm focus:ring-gob-oro focus:border-gob-oro outline-none"
                    placeholder="Describe por que no puede usarse el enlace por correo."></textarea>
                <p class="text-[10px] text-gray-500 mt-2">El codigo vence en 15 minutos, se muestra una sola vez y queda auditado.</p>
            </div>
        `,
        showCancelButton: true,
        confirmButtonText: '<i class="fas fa-shield-alt mr-1"></i> Generar codigo',
        cancelButtonText: 'Cancelar',
        confirmButtonColor: '#691228',
        cancelButtonColor: '#808590ff',
        focusConfirm: false
    }).then(result => {
        if (!result.isConfirmed) return;
        const userId = document.getElementById('det-input-id').value;
        if (!userId) {
            Swal.fire('Error', 'No se ha podido identificar al usuario seleccionado.', 'error');
            return;
        }

        const motivo = document.getElementById('swal-motivo-recuperacion')?.value?.trim() || '';
        if (motivo.length < 10) {
            Swal.fire('Motivo requerido', 'Describe el motivo con al menos 10 caracteres.', 'warning');
            return;
        }
        const formData = new FormData();
        formData.append('motivo', motivo);

        Swal.fire({
            title: 'Generando codigo...',
            html: 'El usuario establecera su propia contrasena.',
            allowOutsideClick: false,
            allowEscapeKey: false,
            didOpen: () => Swal.showLoading()
        });

        fetch(`/usuarios/api/${userId}/recuperacion-manual`, {
            method: 'POST',
            body: formData
        })
            .then(res => {
                if (!res.ok) {
                    return res.json().then(data => {
                        throw new Error(data.mensaje || 'Error al generar el codigo de recuperacion.');
                    });
                }
                return res.json();
            })
            .then(data => {
                const codigoEscapado = encodeURIComponent(data.codigo || '');
                const recoveryUrl = data.recoveryUrl || '/auth/recuperacion-manual';
                const instrucciones = `Este codigo no es tu contrasena. Entra a ${recoveryUrl}, captura tu usuario y el codigo ${data.codigo || ''}, y crea una nueva contrasena. Vence: ${data.venceEn}.`;
                const instruccionesEscapadas = encodeURIComponent(instrucciones);
                const badge = document.getElementById('det-badge-pass-generado');
                if (badge) {
                    badge.innerHTML = '<i class="fas fa-check-circle text-sm"></i> Codigo manual emitido';
                    badge.classList.remove('hidden');
                }
                Swal.fire({
                    title: 'Codigo de recuperacion listo',
                    html: `
                        <p class="text-sm text-gray-600 mb-3">${data.mensaje || 'El usuario establecera su propia contrasena.'}</p>
                        <p class="text-xs text-amber-700 bg-amber-50 border border-amber-100 rounded-lg px-3 py-2 mb-3">Comparte este codigo por un canal autorizado. Vence: ${data.venceEn}. Se mostrara una sola vez.</p>
                        <p class="text-xs text-blue-800 bg-blue-50 border border-blue-100 rounded-lg px-3 py-2 mb-3">
                            El codigo no sustituye la contrasena. El usuario debe capturarlo en
                            <a class="font-bold underline" href="${recoveryUrl}" target="_blank" rel="noopener">Recuperacion manual</a>
                            y crear una nueva contrasena.
                        </p>
                        <div class="flex items-center gap-2 bg-gray-50 border border-gray-200 rounded-lg px-3 py-3">
                            <code id="swal-pass-code-edicion" class="flex-1 text-left text-lg font-bold text-gob-guinda break-all">${data.codigo || ''}</code>
                            <button type="button" onclick="copiarPasswordTemporalEdicion(decodeURIComponent('${codigoEscapado}'))"
                                class="bg-gob-guinda text-white rounded px-3 py-2 text-xs font-bold">
                                <i class="fas fa-copy mr-1"></i>Copiar
                            </button>
                        </div>
                        <button type="button" onclick="copiarPasswordTemporalEdicion(decodeURIComponent('${instruccionesEscapadas}'))"
                            class="mt-3 w-full border border-gob-guinda text-gob-guinda rounded px-3 py-2 text-xs font-bold">
                            <i class="fas fa-copy mr-1"></i>Copiar codigo e instrucciones
                        </button>
                    `,
                    icon: 'success',
                    confirmButtonColor: '#691228'
                });
            })
            .catch(err => {
                Swal.fire({
                    title: 'Error',
                    text: err.message || 'No se pudo generar el codigo de recuperacion.',
                    icon: 'error',
                    confirmButtonColor: '#691228'
                });
            });
    });
}

function enviarEnlaceRestablecimientoEdicion() {
    if (window.emailMaintenance) {
        Swal.fire({
            title: 'Correo en mantenimiento',
            text: window.emailMaintenanceMessage || 'Por ahora no es posible enviar enlaces por correo.',
            icon: 'warning',
            confirmButtonColor: '#691228'
        });
        return;
    }
    const userId = document.getElementById('det-input-id').value;
    if (!userId) {
        Swal.fire('Error', 'No se ha podido identificar al usuario seleccionado.', 'error');
        return;
    }

    Swal.fire({
        width: '34rem',
        title: '<i class="fas fa-paper-plane text-gob-guinda mr-2"></i>Enviar enlace de restablecimiento',
        html: '<p class="text-sm text-gray-600">Se enviara al correo institucional registrado un enlace seguro para crear una nueva contrasena.</p>',
        showCancelButton: true,
        confirmButtonText: '<i class="fas fa-paper-plane mr-1"></i> Enviar enlace',
        cancelButtonText: 'Cancelar',
        confirmButtonColor: '#691228',
        cancelButtonColor: '#808590ff'
    }).then(result => {
        if (!result.isConfirmed) return;

        Swal.fire({
            title: 'Enviando enlace seguro...',
            html: 'Estamos preparando el enlace y enviandolo al correo registrado.',
            allowOutsideClick: false,
            allowEscapeKey: false,
            didOpen: () => Swal.showLoading()
        });

        fetch(`/usuarios/api/${userId}/enviar-restablecimiento`, { method: 'POST' })
            .then(async res => {
                if (!res.ok) {
                    const data = await res.json();
                    if (data.manualRecoveryAvailable) mostrarRecuperacionManual(true);
                    throw new Error(data.mensaje || 'Error al enviar el enlace.');
                }
                return res.json();
            })
            .then(data => {
                const badge = document.getElementById('det-badge-pass-generado');
                if (badge) {
                    badge.innerHTML = '<i class="fas fa-check-circle text-sm"></i> Enlace enviado al correo registrado';
                    badge.classList.remove('hidden');
                }
                Swal.fire({
                    title: 'Enlace enviado',
                    text: data.mensaje || 'Se envio el enlace de restablecimiento al correo registrado.',
                    icon: 'success',
                    confirmButtonColor: '#691228'
                });
            })
            .catch(err => {
                Swal.fire({
                    title: 'Error',
                    text: err.message || 'No se pudo enviar el enlace de restablecimiento.',
                    icon: 'error',
                    confirmButtonColor: '#691228'
                });
            });
    });
}

function copiarPasswordTemporalEdicion(pass) {
    ejecutarCopiado(pass).then(() => {
        const btn = document.getElementById('swal-pass-code-edicion').nextElementSibling;
        if (btn) {
            const original = btn.innerHTML;
            btn.innerHTML = '<i class="fas fa-check mr-1"></i>¡Copiado!';
            btn.classList.add('bg-green-600');
            btn.classList.remove('bg-gob-guinda');
            setTimeout(() => {
                btn.innerHTML = original;
                btn.classList.remove('bg-green-600');
                btn.classList.add('bg-gob-guinda');
            }, 1500);
        }
    }).catch((err) => { console.error("Error al copiar:", err); });
}

function eliminarUsuarioDesdeDetalle() {
    const id = document.getElementById('det-input-id').value;
    if (!id) return;
    abrirModalConfirmarGenerico({
        titulo: '¿Eliminar usuario?',
        texto: 'Esta acción no se puede deshacer.',
        advertencia: 'Se borrarán los accesos y registros asociados permanentemente.',
        esPeligro: true,
        callback: () => {
            window.location.href = `/usuarios/eliminar/${id}`;
        }
    });
}
