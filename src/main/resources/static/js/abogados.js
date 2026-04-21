document.addEventListener('DOMContentLoaded', () => {
    console.log("Abogados cargados");
});

let materiasUsuarioActual = [];
let usuarioSeleccionadoId = null;

// --- 1. LÓGICA DE MAESTRO - DETALLE ---

function seleccionarAbogado(cardElement) {
    // 1. Quitar estado activo de todas las tarjetas (Tonos rojizos ligeros)
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
    const activo = cardElement.getAttribute('data-activo') === 'true';

    const materiasStr = cardElement.getAttribute('data-materias-nombres');
    const materiasNombres = materiasStr ? materiasStr.split('|') : [];

    usuarioSeleccionadoId = id;

    // 4. Ocultar estado vacío, mostrar detalle
    document.getElementById('panel-vacio').classList.add('hidden');
    const panelDetalle = document.getElementById('panel-detalle');
    panelDetalle.classList.remove('hidden');

    panelDetalle.classList.remove('animate-fade-in');
    void panelDetalle.offsetWidth; // Trigger reflow
    panelDetalle.classList.add('animate-fade-in');

    // 5. Inyectar datos en el panel derecho
    document.getElementById('det-avatar').innerText = nombre.charAt(0).toUpperCase();
    document.getElementById('det-nombre').innerText = nombre;
    document.getElementById('det-email').innerText = email;

    // Badges rediseñados para verse bien sobre el fondo GOB-VERDE
    let badgesHtml = `<span class="bg-white/10 border border-white/20 text-white text-[10px] font-bold px-2 py-0.5 rounded shadow-sm">${rol}</span>`;
    badgesHtml += `<span class="bg-white/10 border border-white/20 text-white text-[10px] font-bold px-2 py-0.5 rounded shadow-sm"><i class="fas fa-building mr-1"></i>${gerenciaNombre}</span>`;

    if (activo) {
        badgesHtml += `<span class="bg-green-500/20 border border-green-400/50 text-green-300 text-[10px] font-bold px-2 py-0.5 rounded shadow-sm"><i class="fas fa-check-circle mr-1"></i>Activo</span>`;
    } else {
        badgesHtml += `<span class="bg-red-500/30 border border-red-400/50 text-red-300 text-[10px] font-bold px-2 py-0.5 rounded shadow-sm"><i class="fas fa-ban mr-1"></i>Inactivo</span>`;
    }

    // Materias en tono Oro para resaltar sobre el fondo verde
    materiasNombres.forEach(m => {
        if (m.trim() !== '') {
            badgesHtml += `<span class="bg-yellow-500/20 border border-yellow-500/50 text-yellow-300 text-[10px] font-bold px-2 py-0.5 rounded shadow-sm">${m}</span>`;
        }
    });

    document.getElementById('det-badges').innerHTML = badgesHtml;

    // 6. Configurar botones de acción
    const btnEditar = document.getElementById('btn-editar');
    const btnEstatus = document.getElementById('btn-estatus');

    if (btnEditar) {
        btnEditar.onclick = () => {
            const gerenciaId = cardElement.getAttribute('data-gerencia-id');
            const materiasIdsStr = cardElement.getAttribute('data-materias-ids');
            const materiasIdsArray = materiasIdsStr ? materiasIdsStr.split(',').map(Number) : [];
            editarUsuario(id, nombre, email, rol, gerenciaId, activo, materiasIdsArray);
        };
    }

    if (btnEstatus) {
        btnEstatus.innerHTML = activo ? '<i class="fas fa-ban mr-1"></i> Desactivar' : '<i class="fas fa-check-circle mr-1"></i> Activar';
        btnEstatus.className = activo ? 'px-3 py-1.5 bg-white/10 hover:bg-red-500/80 rounded-lg text-sm font-bold transition-colors backdrop-blur-sm border border-white/10' : 'px-3 py-1.5 bg-white/10 hover:bg-green-500/80 rounded-lg text-sm font-bold transition-colors backdrop-blur-sm border border-white/10';
        btnEstatus.onclick = () => confirmarCambioEstatus(id);
    }
    cambiarTabAbogado('casos');
}

// --- 2. LÓGICA DE MODALES (MANTENIDA Y ADAPTADA) ---

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

    fetch(`/usuarios/api/materias-por-gerencia/${gerenciaId}`)
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
                        <input type="checkbox" name="materiasIds" value="${m.id}" id="mat-${m.id}" ${isChecked} class="rounded text-gob-guinda h-4 w-4 border-gray-300 focus:ring-gob-oro">
                        <label for="mat-${m.id}" class="text-xs text-gray-700 cursor-pointer select-none">${m.nombre}</label>
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
    inputPass.placeholder = "Dejar en blanco para mantener actual";

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
        title: '¿Eliminar abogado?', text: "Esta acción no se puede deshacer.", icon: 'error',
        showCancelButton: true, confirmButtonColor: '#d33', confirmButtonText: 'Sí, eliminar', cancelButtonText: 'Cancelar'
    }).then((result) => {
        if (result.isConfirmed) {
            window.location.href = `/abogados/eliminar/${id}`;
        }
    });
}

function confirmarCambioEstatus(id) {
    Swal.fire({
        title: '¿Cambiar acceso al sistema?',
        text: 'El abogado perderá/recuperará acceso a la plataforma.',
        icon: 'warning', showCancelButton: true,
        confirmButtonColor: '#9D2449', confirmButtonText: 'Sí, cambiar', cancelButtonText: 'Cancelar'
    }).then((result) => {
        if (result.isConfirmed) window.location.href = `/abogados/toggle/${id}`;
    });
}

// --- 3. BUSCADOR EN TIEMPO REAL PARA TARJETAS ---

document.addEventListener('DOMContentLoaded', () => {
    const search = document.getElementById('input-search-usuarios');
    if (search) {
        search.addEventListener('keyup', function () {
            const filter = this.value.toLowerCase();
            const cards = document.querySelectorAll('.abogado-card');

            cards.forEach(card => {
                const nombre = card.getAttribute('data-nombre').toLowerCase();
                const email = card.getAttribute('data-email').toLowerCase();
                const rol = card.getAttribute('data-rol').toLowerCase();

                if (nombre.includes(filter) || email.includes(filter) || rol.includes(filter)) {
                    card.style.display = 'flex';
                } else {
                    card.style.display = 'none';
                }
            });
        });
    }
});

// --- 4. NAVEGACIÓN DE PESTAÑAS (DETALLE) ---

function cambiarTabAbogado(tabName) {
    // 1. Ocultar todos los contenidos de las pestañas
    document.querySelectorAll('.tab-abogado-content').forEach(el => {
        el.classList.add('hidden');
        el.classList.remove('block');
    });

    // 2. Mostrar el contenido de la pestaña seleccionada
    const contenidoActivo = document.getElementById('tab-content-' + tabName);
    if (contenidoActivo) {
        contenidoActivo.classList.remove('hidden');
        contenidoActivo.classList.add('block');
    }

    // 3. Resetear el diseño de todos los botones de las pestañas
    document.querySelectorAll('.tab-abogado').forEach(btn => {
        btn.classList.remove('border-gob-guinda', 'text-gob-guinda', 'font-bold');
        btn.classList.add('border-transparent', 'text-gray-500', 'font-medium');
    });

    // 4. Activar el diseño del botón presionado
    const botonActivo = document.getElementById('tab-btn-' + tabName);
    if (botonActivo) {
        botonActivo.classList.remove('border-transparent', 'text-gray-500', 'font-medium');
        botonActivo.classList.add('border-gob-guinda', 'text-gob-guinda', 'font-bold');
    }

    // Aquí en el futuro agregaremos una llamada Fetch/AJAX para traer los datos:
    // si (tabName === 'casos') cargarCasosAbogado(usuarioSeleccionadoId);
}// --- 4. NAVEGACIÓN Y CARGA DE DATOS (PESTAÑAS) ---

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

    fetch(`/abogados/api/${idUsuario}/casos`)
        .then(res => res.json())
        .then(data => {
            document.getElementById('loading-casos').classList.add('hidden');
            if (data.length === 0) {
                document.getElementById('empty-casos').classList.remove('hidden');
            } else {
                tbody.innerHTML = '';
                data.forEach(caso => {
                    // Colores del estatus basados en EtapaProcesal enum
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

    fetch(`/abogados/api/${idUsuario}/agenda`)
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

function cargarHistorialAbogado(idUsuario) {
    const container = document.getElementById('container-historial');
    document.getElementById('container-historial').classList.add('hidden');
    document.getElementById('empty-historial').classList.add('hidden');
    document.getElementById('loading-historial').classList.remove('hidden');

    fetch(`/abogados/api/${idUsuario}/historial`)
        .then(res => res.json())
        .then(data => {
            document.getElementById('loading-historial').classList.add('hidden');
            if (data.length === 0) {
                document.getElementById('empty-historial').classList.remove('hidden');
            } else {
                container.innerHTML = '';
                data.forEach(act => {
                    container.innerHTML += `
                        <div class="flex gap-4 p-4 bg-white rounded-xl border border-gray-100 shadow-sm hover:shadow-md transition-shadow">
                            <div class="flex-shrink-0 w-10 h-10 bg-gray-50 rounded-full flex items-center justify-center text-gob-guinda border border-gray-100">
                                <i class="fas fa-edit text-sm"></i>
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
        }).catch(err => console.error(err));
}