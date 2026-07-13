document.addEventListener('DOMContentLoaded', () => {
    console.log("Usuarios Admin cargados");
});

let materiasUsuarioActual = [];
let materiasUsuarioEdicion = [];
let usuarioSeleccionadoId = null;

// --- 1. LÓGICA DE MAESTRO - DETALLE ---

function seleccionarAbogado(cardElement) {
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
    const rol = cardElement.getAttribute('data-rol');
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
    document.getElementById('det-email').innerText = email;

    // Badges
    let badgesHtml = `<span class="bg-white/10 border border-white/20 text-white text-[10px] font-bold px-2 py-0.5 rounded shadow-sm">${rol}</span>`;
    badgesHtml += `<span class="bg-white/10 border border-white/20 text-white text-[10px] font-bold px-2 py-0.5 rounded shadow-sm"><i class="fas fa-building mr-1"></i>${gerenciaNombre}</span>`;

    if (activo) {
        badgesHtml += `<span class="bg-green-500/20 border border-green-400/50 text-green-300 text-[10px] font-bold px-2 py-0.5 rounded shadow-sm"><i class="fas fa-check-circle mr-1"></i>Activo</span>`;
    } else {
        badgesHtml += `<span class="bg-red-500/30 border border-red-400/50 text-red-300 text-[10px] font-bold px-2 py-0.5 rounded shadow-sm"><i class="fas fa-ban mr-1"></i>Inactivo</span>`;
    }

    // Materias
    materiasNombres.forEach(m => {
        if (m.trim() !== '') {
            badgesHtml += `<span class="bg-yellow-500/20 border border-yellow-500/50 text-yellow-300 text-[10px] font-bold px-2 py-0.5 rounded shadow-sm">${m}</span>`;
        }
    });

    document.getElementById('det-badges').innerHTML = badgesHtml;

    // 6. Pre-poblar los campos de la pestaña "Editar Perfil"
    const inputDetId = document.getElementById('det-input-id');
    if (inputDetId) {
        document.getElementById('det-input-id').value = id;
        document.getElementById('det-input-nombre').value = nombre;
        document.getElementById('det-input-email').value = email;
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

        materiasUsuarioEdicion = materiasIdsArray || [];
        document.querySelectorAll('.det-gerencia-checkbox').forEach(cb => {
            cb.checked = gerenciasIdsArray.includes(Number(cb.value));
        });

        actualizarFormularioEdicionPorRol();
        cargarMateriasDeGerenciaEdicion();
    }

    // 7. Configurar botón de estatus
    const btnEstatus = document.getElementById('btn-estatus');
    if (btnEstatus) {
        btnEstatus.innerHTML = activo ? '<i class="fas fa-ban mr-1"></i> Desactivar' : '<i class="fas fa-check-circle mr-1"></i> Activar';
        btnEstatus.className = activo ? 'px-3 py-1.5 bg-white/10 hover:bg-red-500/80 rounded-lg text-sm font-bold transition-colors backdrop-blur-sm border border-white/10 shadow-sm' : 'px-3 py-1.5 bg-white/10 hover:bg-green-500/80 rounded-lg text-sm font-bold transition-colors backdrop-blur-sm border border-white/10 shadow-sm';
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
    const divGerencia = document.getElementById('div-gerencia-asignada');
    const contenedorMaterias = document.getElementById('contenedor-materias');

    if (rol === 'ADMIN' || rol === 'DIRECTOR_PRINCIPAL' || rol === 'DIRECTOR' || rol === 'SUBDIRECTOR') {
        if (divGerencia) divGerencia.classList.add('hidden');
        if (contenedorMaterias) contenedorMaterias.classList.add('hidden');
    } else {
        const creadorRol = window.usuarioActual ? window.usuarioActual.rol : '';
        if (creadorRol !== 'GERENTE' && creadorRol !== 'JEFE_DEPTO') {
            if (divGerencia) divGerencia.classList.remove('hidden');
        }
        
        const selectGerencias = document.getElementById('select-usuario-gerencia');
        const checkedGerencias = selectGerencias ? Array.from(selectGerencias.selectedOptions).map(opt => opt.value) : [];
        
        if (checkedGerencias.length > 0 && creadorRol !== 'JEFE_DEPTO') {
            if (contenedorMaterias) contenedorMaterias.classList.remove('hidden');
        } else {
            if (contenedorMaterias) contenedorMaterias.classList.add('hidden');
        }
    }
}

function cargarMateriasDeGerencia() {
    const selectRol = document.getElementById('select-usuario-rol');
    const rol = selectRol ? selectRol.value : '';
    if (rol === 'ADMIN' || rol === 'DIRECTOR_PRINCIPAL' || rol === 'DIRECTOR' || rol === 'SUBDIRECTOR') {
        const contenedor = document.getElementById('contenedor-materias');
        if (contenedor) contenedor.classList.add('hidden');
        return;
    }
    const selectGerencias = document.getElementById('select-usuario-gerencia');
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
                const isChecked = materiasUsuarioActual.includes(m.id) ? 'checked' : '';
                lista.innerHTML += `
                    <div class="flex items-center space-x-2">
                        <input type="checkbox" name="materiasIds" value="${m.id}" id="mat-${m.id}" ${isChecked} class="rounded text-gob-guinda h-4 w-4 border-gray-300 focus:ring-gob-oro">
                        <label for="mat-${m.id}" class="text-xs text-gray-700 cursor-pointer select-none">${m.nombre}</label>
                    </div>`;
            });
        })
        .catch(err => {
            console.error("Error al cargar materias:", err);
            if (lista) lista.innerHTML = '<p class="text-xs text-red-400">Error al cargar materias.</p>';
        });
}

function editarUsuario(id, nombre, email, rol, gerenciaIdsArray, activo, materiasIdsArray) {
    abrirModalUsuario();
    
    const titulo = document.getElementById('titulo-modal-usuario');
    if (titulo) titulo.innerHTML = '<i class="fas fa-user-edit mr-2"></i> Editar Usuario';

    const inputId = document.getElementById('input-usuario-id');
    if (inputId) inputId.value = id;

    const inputNombre = document.getElementById('input-usuario-nombre');
    if (inputNombre) inputNombre.value = nombre;

    const inputEmail = document.getElementById('input-usuario-email');
    if (inputEmail) inputEmail.value = email;

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

// ── MODAL DE CONTRASEÑA TEMPORAL ──────────────────────────────────────────
function abrirModalPasswordTemporal() {
    const digitos = Math.floor(10000 + Math.random() * 90000);
    const passGenerada = `Temporal${digitos}`;

    Swal.fire({
        width: '36rem',
        title: '<i class="fas fa-key text-gob-guinda mr-2"></i>Contraseña Temporal',
        html: `
            <p class="text-sm text-gray-600 mb-6">Se asignará esta contraseña temporal al usuario.<br>Al iniciar sesión se le pedirá cambiarla.</p>
            <div class="flex items-center gap-6 bg-gray-50 border border-gray-200 rounded-lg px-4 py-6">
                <code id="swal-pass-code" class="flex-1 text-left text-lg font-mono font-bold text-gob-guinda tracking-widest select-all">${passGenerada}</code>
                <button type="button" onclick="copiarPasswordTemporal('${passGenerada}')"
                     class="flex-shrink-0 px-3 py-1.5 bg-gob-guinda text-white text-xs font-bold rounded-lg hover:opacity-90 transition-all">
                    <i class="fas fa-copy mr-1"></i>Copiar
                </button>
            </div>
            <p class="text-[10px] text-gray-400 mt-2">El usuario deberá cambiarla en su próximo inicio de sesión.</p>
        `,
        showCancelButton: true,
        confirmButtonText: '<i class="fas fa-save mr-1"></i> Actualizar contraseña',
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
            const hiddenPass = document.getElementById('input-password-temporal');
            if (hiddenPass) hiddenPass.value = passGenerada;

            const form = document.querySelector('#modal-usuario form');
            if (form) form.submit();
        }
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
            window.location.href = `/usuarios-admin/eliminar/${id}`;
        }
    });
}

function confirmarCambioEstatus(id) {
    abrirModalConfirmarGenerico({
        titulo: '¿Cambiar acceso al sistema?',
        texto: 'El usuario perderá/recuperará acceso a la plataforma.',
        advertencia: 'El cambio afectará la disponibilidad del registro en el sistema.',
        esPeligro: false,
        callback: () => {
            window.location.href = `/usuarios-admin/toggle/${id}`;
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
    if (!usuarioSeleccionadoId) return;

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

    fetch(`/usuarios-admin/api/${idUsuario}/casos`)
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

    fetch(`/usuarios-admin/api/${idUsuario}/agenda`)
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

function cargarHistorialAbogado(idUsuario) {
    const container = document.getElementById('container-historial');
    document.getElementById('container-historial').classList.add('hidden');
    document.getElementById('empty-historial').classList.add('hidden');
    document.getElementById('loading-historial').classList.remove('hidden');

    document.querySelectorAll('.btn-filtro-historial').forEach(btn => {
        btn.className = "btn-filtro-historial px-3 py-1.5 text-xs rounded-full bg-white text-slate-600 font-medium border border-slate-200 hover:bg-slate-50 transition-all";
    });
    const defaultBtn = document.querySelector('.btn-filtro-historial[onclick*="TODOS"]');
    if (defaultBtn) {
        defaultBtn.className = "btn-filtro-historial active px-3 py-1.5 text-xs rounded-full bg-slate-200 text-slate-700 font-bold border border-transparent shadow-sm transition-all";
    }

    fetch(`/usuarios-admin/api/${idUsuario}/historial`)
        .then(res => res.json())
        .then(data => {
            document.getElementById('loading-historial').classList.add('hidden');
            historialDatosCargados = data;
            renderizarHistorial(data);
        }).catch(err => {
            console.error(err);
            document.getElementById('loading-historial').classList.add('hidden');
        });
}

function renderizarHistorial(data) {
    const container = document.getElementById('container-historial');
    if (!data || data.length === 0) {
        container.innerHTML = '';
        container.classList.add('hidden');
        document.getElementById('empty-historial').classList.remove('hidden');
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
                        <p class="text-sm font-bold text-slate-800 truncate">${act.accion}</p>
                        <span class="text-[10px] text-slate-400 whitespace-nowrap ml-2">${act.fecha}</span>
                    </div>
                    <p class="text-xs text-slate-600 mt-1">${act.detalles}</p>
                    ${act.expediente !== 'General' ? `
                    <p class="text-xs font-medium text-gob-guinda flex items-center gap-1 mt-2">
                        <i class="fas fa-file-alt"></i> Exp: ${act.expediente}
                    </p>` : ''}
                </div>
            </div>
        `;
    });
    container.classList.remove('hidden');
}

function filtrarHistorial(categoria) {
    document.querySelectorAll('.btn-filtro-historial').forEach(btn => {
        if (btn.getAttribute('onclick').includes(`'${categoria}'`)) {
            btn.className = "btn-filtro-historial active px-3 py-1.5 text-xs rounded-full bg-slate-200 text-slate-700 font-bold border border-transparent shadow-sm transition-all";
        } else {
            btn.className = "btn-filtro-historial px-3 py-1.5 text-xs rounded-full bg-white text-slate-600 font-medium border border-slate-200 hover:bg-slate-50 transition-all";
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
    const contenedorMaterias = document.getElementById('det-contenedor-materias');

    if (rol === 'ADMIN' || rol === 'DIRECTOR_PRINCIPAL' || rol === 'DIRECTOR' || rol === 'SUBDIRECTOR') {
        if (divGerencia) divGerencia.classList.add('hidden');
        if (contenedorMaterias) contenedorMaterias.classList.add('hidden');
    } else {
        const creadorRol = window.usuarioActual ? window.usuarioActual.rol : '';
        if (creadorRol !== 'GERENTE' && creadorRol !== 'JEFE_DEPTO') {
            if (divGerencia) divGerencia.classList.remove('hidden');
        }
        const checkedGerencias = document.querySelectorAll('.det-gerencia-checkbox:checked');
        if (checkedGerencias.length > 0 && creadorRol !== 'JEFE_DEPTO') {
            if (contenedorMaterias) contenedorMaterias.classList.remove('hidden');
        } else {
            if (contenedorMaterias) contenedorMaterias.classList.add('hidden');
        }
    }
}

function cargarMateriasDeGerenciaEdicion() {
    const selectRol = document.getElementById('det-select-rol');
    const rol = selectRol ? selectRol.value : '';
    if (rol === 'ADMIN' || rol === 'DIRECTOR_PRINCIPAL' || rol === 'DIRECTOR' || rol === 'SUBDIRECTOR') {
        const contenedor = document.getElementById('det-contenedor-materias');
        if (contenedor) contenedor.classList.add('hidden');
        return;
    }
    const checkedGerencias = Array.from(document.querySelectorAll('.det-gerencia-checkbox:checked')).map(cb => cb.value);
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
                const isChecked = materiasUsuarioEdicion.includes(m.id) ? 'checked' : '';
                lista.innerHTML += `
                    <div class="flex items-center space-x-2">
                        <input type="checkbox" name="materiasIds" value="${m.id}" id="det-mat-${m.id}" ${isChecked} class="rounded text-gob-guinda h-4 w-4 border-gray-300 focus:ring-gob-oro">
                        <label for="det-mat-${m.id}" class="text-xs text-gray-700 cursor-pointer select-none">${m.nombre}</label>
                    </div>`;
            });
        })
        .catch(err => {
            console.error("Error al cargar materias en edición:", err);
            if (lista) lista.innerHTML = '<p class="text-xs text-red-400">Error al cargar materias.</p>';
        });
}

function abrirModalPasswordTemporalEdicion() {
    const digitos = Math.floor(10000 + Math.random() * 90000);
    const passGenerada = `Temporal${digitos}`;

    Swal.fire({
        width: '36rem',
        title: '<i class="fas fa-key text-gob-guinda mr-2"></i>Contraseña Temporal',
        html: `
            <p class="text-sm text-gray-600 mb-6">Se asignará esta contraseña temporal al usuario.<br>Al iniciar sesión se le pedirá cambiarla.</p>
            <div class="flex items-center gap-6 bg-gray-50 border border-gray-200 rounded-lg px-4 py-6">
                <code id="swal-pass-code-edicion" class="flex-1 text-left text-lg font-mono font-bold text-gob-guinda tracking-widest select-all">${passGenerada}</code>
                <button type="button" onclick="copiarPasswordTemporalEdicion('${passGenerada}')"
                    class="flex-shrink-0 px-3 py-1.5 bg-gob-guinda text-white text-xs font-bold rounded-lg hover:opacity-90 transition-all">
                    <i class="fas fa-copy mr-1"></i>Copiar
                </button>
            </div>
            <p class="text-[10px] text-gray-400 mt-2">El usuario deberá cambiarla en su próximo inicio de sesión.</p>
        `,
        showCancelButton: true,
        confirmButtonText: '<i class="fas fa-save mr-1"></i> Generar y Enviar',
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
                title: 'Guardando contraseña...',
                html: 'Guardando la contraseña temporal en la base de datos...',
                allowOutsideClick: false,
                didOpen: () => {
                    Swal.showLoading();
                }
            });

            const formData = new FormData();
            formData.append('id', userId);
            formData.append('password', passGenerada);

            fetch('/usuarios-admin/api/password-temporal', {
                method: 'POST',
                body: formData
            })
                .then(res => {
                    if (!res.ok) {
                        return res.json().then(data => {
                            throw new Error(data.mensaje || 'Error al guardar la contraseña.');
                        });
                    }
                    return res.json();
                })
                .then(data => {
                    const badge = document.getElementById('det-badge-pass-generado');
                    if (badge) {
                        badge.innerHTML = '<i class="fas fa-check-circle text-sm"></i> Contraseña Temporal Actualizada';
                        badge.classList.remove('hidden');
                    }
                    Swal.fire({
                        title: '¡Contraseña Temporal Actualizada!',
                        text: 'La contraseña temporal ha sido actualizada de manera correcta, ya no es necesario que guardes los cambios del perfil',
                        icon: 'success',
                        confirmButtonColor: '#691228'
                    });
                })
                .catch(err => {
                    Swal.fire({
                        title: 'Error',
                        text: err.message || 'No se pudo guardar la contraseña temporal.',
                        icon: 'error',
                        confirmButtonColor: '#691228'
                    });
                });
        }
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
            window.location.href = `/usuarios-admin/eliminar/${id}`;
        }
    });
}
