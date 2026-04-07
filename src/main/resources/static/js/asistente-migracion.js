function inicializarAsistente() {
    // ==========================================
    // LÓGICA DE PESTAÑAS (TABS) PARA LA ETAPA 1
    // ==========================================
    const tabBtns = document.querySelectorAll(".tab-btn");
    const tabPanels = document.querySelectorAll(".tab-panel");

    console.log("Cargando Asistente de Migración... Pestañas detectadas:", tabBtns.length);

    if (tabBtns.length === 0 || tabPanels.length === 0) return;

    // 1. Esconder todos los paneles por seguridad
    tabPanels.forEach(p => p.classList.add("hidden"));

    // 2. Encender dinámicamente el panel que corresponda al PRIMER botón que exista
    const firstBtn = tabBtns[0];
    const targetId = firstBtn.getAttribute("data-target");
    const firstPanel = document.getElementById(targetId);

    if (firstPanel) {
        firstPanel.classList.remove("hidden");
        firstBtn.classList.add("border-gob-oro", "text-gob-oro", "font-bold");
        firstBtn.classList.remove("border-transparent", "text-gray-500");
    }

    // 3. Asignar los eventos de clic
    tabBtns.forEach(btn => {
        btn.addEventListener("click", function () {
            const tId = this.getAttribute("data-target");
            const tPanel = document.getElementById(tId);

            if (!tPanel) return;

            tabBtns.forEach(b => {
                b.classList.remove("border-gob-oro", "text-gob-oro", "font-bold");
                b.classList.add("border-transparent", "text-gray-500");
            });
            tabPanels.forEach(p => p.classList.add("hidden"));

            this.classList.add("border-gob-oro", "text-gob-oro", "font-bold");
            this.classList.remove("border-transparent", "text-gray-500");
            tPanel.classList.remove("hidden");
        });
    });
}

// Forzar ejecución inmediata de las pestañas al cargar la página
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', inicializarAsistente);
    document.addEventListener('DOMContentLoaded', inicializarMapeo);
} else {
    inicializarAsistente();
    inicializarMapeo();
}

// ==========================================
// LÓGICA DE MAPEO (ETAPA 1)
// ==========================================
function inicializarMapeo() {
    // Inicializar TomSelect en todos los selects
    const selects = document.querySelectorAll('.searchable-select');
    selects.forEach(select => {
        if (!select.tomselect) {
            const ts = new TomSelect(select, {
                create: false,
                sortField: { field: "text", direction: "asc" },
                placeholder: '-- Selecciona un valor oficial --',
                dropdownParent: 'body',
                onDropdownOpen: function (dropdown) {
                    const rect = this.control.getBoundingClientRect();
                    dropdown.style.position = 'fixed';
                    dropdown.style.zIndex = '99999';
                    dropdown.style.top = rect.bottom + 'px';
                    dropdown.style.left = rect.left + 'px';
                    dropdown.style.width = rect.width + 'px';
                }
            });
        }
    });

    window.addEventListener('scroll', function (e) {
        // 1. Verificamos si el scroll viene desde adentro del menú desplegable
        if (e.target && e.target.closest && e.target.closest('.ts-dropdown')) {
            return; // Si es así, detenemos la función aquí y dejamos que el usuario baje por la lista
        }

        // 2. Si el scroll viene de la tabla o la página, cerramos los selects abiertos
        selects.forEach(select => {
            if (select.tomselect && select.tomselect.isOpen) {
                select.tomselect.close();
            }
        });
    }, true);

    // ==========================================
    // FILTRO DE GERENCIA PARA MATERIAS (ETAPA 1)
    // ==========================================
    document.querySelectorAll('.materia-gerencia-filter').forEach(filter => {
        filter.addEventListener('change', function () {
            const gerenciaId = this.value;
            const targetId = this.getAttribute('data-target');
            const selectEl = document.getElementById(targetId);

            if (selectEl && selectEl.tomselect) {
                const ts = selectEl.tomselect;
                ts.clear();

                const originalOptions = Array.from(selectEl.querySelectorAll('option'));
                ts.clearOptions();

                originalOptions.forEach(opt => {
                    const parentGroup = opt.closest('optgroup');
                    const gId = parentGroup ? parentGroup.getAttribute('data-gerencia-id') : null;

                    if (!gerenciaId || gId === gerenciaId || opt.value === "") {
                        ts.addOption({
                            value: opt.value,
                            text: opt.text
                        });
                    }
                });
                ts.refreshOptions(false);
                console.log("Filtrado completado para:", targetId);
            }
        });
    });

    // ==========================================
    // BOTÓN: EDITAR TEXTO DETECTADO (lápiz)
    // ==========================================
    document.querySelectorAll('.btn-editar-texto').forEach(btn => {
        btn.addEventListener('click', function () {
            const catId = this.getAttribute('data-cat-id');
            // Ocultar display, mostrar input
            document.getElementById('texto-display-' + catId).classList.add('hidden');
            document.getElementById('texto-display-' + catId).classList.remove('flex');
            const editDiv = document.getElementById('texto-edit-' + catId);
            editDiv.classList.remove('hidden');
            editDiv.classList.add('flex');
            // Focus en el input
            document.getElementById('texto-input-' + catId).focus();
        });
    });

    // Confirmar edición del texto
    document.querySelectorAll('.btn-confirmar-edit').forEach(btn => {
        btn.addEventListener('click', function () {
            const catId = this.getAttribute('data-cat-id');
            const nuevoTexto = document.getElementById('texto-input-' + catId).value.trim();
            if (!nuevoTexto) return;
            // Actualizar el texto visible
            document.getElementById('texto-valor-' + catId).textContent = nuevoTexto;
            // Actualizar el data-original-val en los botones que lo usan
            const btnNuevo = document.querySelector('.btn-marcar-nuevo[data-cat-id="' + catId + '"]');
            if (btnNuevo) btnNuevo.setAttribute('data-original-val', nuevoTexto);
            // Ocultar input, mostrar display
            document.getElementById('texto-edit-' + catId).classList.add('hidden');
            document.getElementById('texto-edit-' + catId).classList.remove('flex');
            document.getElementById('texto-display-' + catId).classList.remove('hidden');
            document.getElementById('texto-display-' + catId).classList.add('flex');
        });
    });

    // Cancelar edición del texto
    document.querySelectorAll('.btn-cancelar-edit').forEach(btn => {
        btn.addEventListener('click', function () {
            const catId = this.getAttribute('data-cat-id');
            // Restaurar input al valor actual del span
            const textoActual = document.getElementById('texto-valor-' + catId).textContent;
            document.getElementById('texto-input-' + catId).value = textoActual;
            // Ocultar input, mostrar display
            document.getElementById('texto-edit-' + catId).classList.add('hidden');
            document.getElementById('texto-edit-' + catId).classList.remove('flex');
            document.getElementById('texto-display-' + catId).classList.remove('hidden');
            document.getElementById('texto-display-' + catId).classList.add('flex');
        });
    });

    // ==========================================
    // BOTÓN: CREAR COMO NUEVO (+)
    // ==========================================
    document.querySelectorAll('.btn-marcar-nuevo').forEach(btn => {
        btn.addEventListener('click', function () {
            const catId = this.getAttribute('data-cat-id');
            const originalVal = this.getAttribute('data-original-val');

            // Ocultar select original y botones
            document.getElementById('mapeo-container-' + catId).classList.add('hidden');
            document.getElementById('mapeo-container-' + catId).classList.remove('flex');

            // Mostrar badge de "nuevo"
            const badge = document.getElementById('nuevo-badge-' + catId);
            badge.classList.remove('hidden');
            badge.classList.add('flex');

            // Deshabilitar el select
            const selectEl = document.getElementById('select-' + catId);
            selectEl.disabled = true;

            // Inyectar hidden input
            if (!document.getElementById('hidden-nuevo-' + catId)) {
                const hiddenInput = document.createElement('input');
                hiddenInput.type = 'hidden';
                hiddenInput.id = 'hidden-nuevo-' + catId;
                hiddenInput.name = 'cat_' + catId;
                hiddenInput.value = 'NEW_' + originalVal;
                badge.appendChild(hiddenInput);
            }
        });
    });

    // Deshacer "Crear como Nuevo"
    document.querySelectorAll('.btn-deshacer-nuevo').forEach(btn => {
        btn.addEventListener('click', function () {
            const catId = this.getAttribute('data-cat-id');

            const hiddenInput = document.getElementById('hidden-nuevo-' + catId);
            if (hiddenInput) hiddenInput.remove();

            const container = document.getElementById('mapeo-container-' + catId);
            container.classList.remove('hidden');
            container.classList.add('flex');

            const badge = document.getElementById('nuevo-badge-' + catId);
            badge.classList.add('hidden');
            badge.classList.remove('flex');

            const selectEl = document.getElementById('select-' + catId);
            selectEl.disabled = false;
            if (selectEl.tomselect) {
                selectEl.tomselect.clear();
            } else {
                selectEl.value = '';
            }
        });
    });

    // ==========================================
    // BOTÓN: DESCARTAR (-)
    // ==========================================
    document.querySelectorAll('.btn-descartar').forEach(btn => {
        btn.addEventListener('click', function () {
            const catId = this.getAttribute('data-cat-id');

            // Ocultar select y botones
            document.getElementById('mapeo-container-' + catId).classList.add('hidden');
            document.getElementById('mapeo-container-' + catId).classList.remove('flex');

            // Mostrar badge "descartado"
            const badge = document.getElementById('descartado-badge-' + catId);
            badge.classList.remove('hidden');
            badge.classList.add('flex');

            // Deshabilitar select para que no se envíe
            const selectEl = document.getElementById('select-' + catId);
            selectEl.disabled = true;

            // Inyectar hidden con valor SKIP para que el backend lo ignore
            if (!document.getElementById('hidden-skip-' + catId)) {
                const hiddenInput = document.createElement('input');
                hiddenInput.type = 'hidden';
                hiddenInput.id = 'hidden-skip-' + catId;
                hiddenInput.name = 'cat_' + catId;
                hiddenInput.value = 'SKIP';
                badge.appendChild(hiddenInput);
            }

            // Atenuar la fila
            const fila = document.getElementById('fila-' + catId);
            if (fila) fila.classList.add('opacity-40');
        });
    });

    // Deshacer descarte
    document.querySelectorAll('.btn-deshacer-descartado').forEach(btn => {
        btn.addEventListener('click', function () {
            const catId = this.getAttribute('data-cat-id');

            const hiddenInput = document.getElementById('hidden-skip-' + catId);
            if (hiddenInput) hiddenInput.remove();

            const container = document.getElementById('mapeo-container-' + catId);
            container.classList.remove('hidden');
            container.classList.add('flex');

            const badge = document.getElementById('descartado-badge-' + catId);
            badge.classList.add('hidden');
            badge.classList.remove('flex');

            const selectEl = document.getElementById('select-' + catId);
            selectEl.disabled = false;

            const fila = document.getElementById('fila-' + catId);
            if (fila) fila.classList.remove('opacity-40');
        });
    });

    // ==========================================
    // BOTÓN GLOBAL: "Crear TODOS como Nuevos" (toggle)
    // ==========================================
    const btnCrearTodos = document.getElementById('btn-crear-todos-nuevos');
    if (btnCrearTodos) {
        let todosNuevos = false; // Estado del toggle

        btnCrearTodos.addEventListener('click', function () {
            todosNuevos = !todosNuevos;

            if (todosNuevos) {
                // ACTIVAR: Solo marcar como nuevo los que NO tienen un valor asignado
                document.querySelectorAll('.btn-marcar-nuevo').forEach(b => {
                    const catId = b.getAttribute('data-cat-id');
                    const container = document.getElementById('mapeo-container-' + catId);
                    if (!container || container.classList.contains('hidden')) return; // Ya tiene acción

                    const selectEl = document.getElementById('select-' + catId);
                    const tieneValor = selectEl && selectEl.tomselect
                        ? selectEl.tomselect.getValue() !== ''
                        : (selectEl && selectEl.value !== '');

                    if (!tieneValor) {
                        b.click(); // Solo aplicar a los vacíos
                    }
                });

                this.innerHTML = '<i class="fas fa-undo mr-2"></i> Restaurar Vacíos';
                this.classList.add('bg-amber-100', 'border-amber-400', 'text-amber-800');
                this.classList.remove('bg-gray-100', 'border-gray-300', 'text-gray-700');
            } else {
                // DESACTIVAR: Deshacer todos los que se marcaron como nuevo
                document.querySelectorAll('.btn-deshacer-nuevo').forEach(b => {
                    const catId = b.getAttribute('data-cat-id');
                    const badge = document.getElementById('nuevo-badge-' + catId);
                    if (badge && !badge.classList.contains('hidden')) {
                        b.click();
                    }
                });

                this.innerHTML = '<i class="fas fa-plus-square mr-2"></i> Crear TODOS como Nuevos';
                this.classList.remove('bg-amber-100', 'border-amber-400', 'text-amber-800');
                this.classList.add('bg-gray-100', 'border-gray-300', 'text-gray-700');
            }
        });
    }
}

// ==========================================
// SWEET ALERT PARA LA ETAPA FINAL (Inyección)
// ==========================================
function confirmarInyeccion() {
    Swal.fire({
        title: '¿Estás completamente seguro?',
        text: "Se generarán los expedientes y se enviarán notificaciones a los abogados.",
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#1a3c34',
        cancelButtonColor: '#d33',
        confirmButtonText: 'Sí, ¡Inyectar ahora!',
        cancelButtonText: 'Revisar de nuevo'
    }).then((result) => {
        if (result.isConfirmed) {
            document.getElementById('formFinal').submit();
        }
    });
}

// ==========================================
// AJAX HELPER: enviar formulario al backend
// ==========================================
async function guardarEdicionRegistro(formEl) {
    const formData = new FormData(formEl);
    const data = new URLSearchParams(formData);
    const registroId = formData.get('registroId');

    // Detectar a qué etapa pertenece el formulario
    const esAudiencia = formData.has('fechaAudiencia');
    const esTermino   = formData.has('fechaVencimiento') && !formData.has('fechaAudiencia');
    const esExpediente = !esAudiencia && !esTermino;
    
    try {
        const resp = await fetch('/admin/importar/editar-registro', { method: 'POST', body: data });
        const json = await resp.json();
        
        if (resp.ok && json.status === 'ok') {

            // 1. Cerrar el modal INMEDIATAMENTE
            const openModal = document.querySelector('.fixed.flex:not(.hidden)');
            if (openModal) { openModal.classList.add('hidden'); openModal.classList.remove('flex'); }

            // 2. Mostrar toast de éxito
            Swal.fire({ icon: 'success', title: '¡Guardado!', text: json.mensaje, timer: 1200, showConfirmButton: false });

            // 3. Actualizar fila en tiempo real
            const row = document.getElementById('row-' + registroId);
            if (!row) return;

            // Actualizar data-attributes del botón de editar
            const btnEditar = row.querySelector('[class*="btn-editar"]');
            if (btnEditar) {
                formData.forEach((value, key) => {
                    const attrName = 'data-' + key.replace(/([a-z0-9]|(?=[A-Z]))([A-Z])/g, '$1-$2').toLowerCase();
                    btnEditar.setAttribute(attrName, value);
                });
                if (json.gerenciaId) btnEditar.setAttribute('data-gerencia-id', json.gerenciaId);
                if (json.materiaId)  btnEditar.setAttribute('data-materia-id',  json.materiaId);
            }

            if (esExpediente) {
                // ============================================
                // ETAPA 2: Expedientes
                // ============================================
                const expNum = row.querySelector('td:first-child .font-bold');
                if (expNum && formData.has('numeroExpediente'))
                    expNum.innerText = formData.get('numeroExpediente');

                const partesSpan = row.querySelector('td:nth-child(2) span');
                if (partesSpan && formData.has('partes'))
                    partesSpan.innerText = formData.get('partes');

                const cellMG = row.querySelector('td:nth-child(3) .flex-col');
                if (cellMG) {
                    const spMs = cellMG.querySelectorAll('span');
                    if (spMs[0] && formData.has('materia'))  spMs[0].innerText = formData.get('materia');
                    if (spMs[1] && formData.has('gerencia')) spMs[1].innerText = formData.get('gerencia');
                }

                // Recalcular badges de validación
                const numVal = formData.get('numeroExpediente') || '';
                const matVal = formData.get('materia') || '';
                const gerVal = formData.get('gerencia') || '';
                const faltantes = [];
                if (!numVal || numVal.startsWith('SIN_NUMERO')) faltantes.push('Número de Expediente');
                if (!matVal || matVal === 'Por asignar')        faltantes.push('Materia');
                if (!gerVal)                                    faltantes.push('Gerencia');

                const validacionTd = row.querySelector('td:nth-child(5)');
                const estadoTd     = row.querySelector('td:nth-child(6)');
                if (validacionTd) {
                    validacionTd.innerHTML = faltantes.length === 0
                        ? '<span class="text-[10px] text-green-600"><i class="fas fa-check mr-1"></i>Completo</span>'
                        : '<div class="flex flex-wrap gap-1">' + faltantes.map(f => {
                            const esCrit = ['Número de Expediente','Materia','Gerencia'].includes(f);
                            const cls = esCrit ? 'bg-red-100 text-red-700' : 'bg-yellow-100 text-yellow-700';
                            return `<span class="px-2 py-0.5 rounded-full text-[9px] font-bold uppercase tracking-tighter ${cls}">FALTA ${f}</span>`;
                          }).join('') + '</div>';
                }
                if (estadoTd) {
                    if (faltantes.length === 0) {
                        estadoTd.innerHTML = '<span class="bg-green-100 text-green-800 px-2 py-0.5 rounded-full text-[10px] font-bold uppercase"><i class="fas fa-heartbeat mr-1"></i>Óptimo</span>';
                    } else {
                        const esCrit = faltantes.some(f => ['Número de Expediente','Materia','Gerencia'].includes(f));
                        estadoTd.innerHTML = esCrit
                            ? '<span class="bg-red-100 text-red-800 px-2 py-0.5 rounded-full text-[10px] font-bold uppercase"><i class="fas fa-skull-crossbones mr-1"></i>Crítico</span>'
                            : '<span class="bg-yellow-100 text-yellow-800 px-2 py-0.5 rounded-full text-[10px] font-bold uppercase"><i class="fas fa-exclamation-triangle mr-1"></i>Incompleto</span>';
                    }
                }

            } else if (esAudiencia) {
                // ============================================
                // ETAPA 3: Audiencias
                // ============================================
                const fhDiv = row.querySelector('td:nth-child(2) .flex');
                if (fhDiv) {
                    const sp = fhDiv.querySelectorAll('span');
                    if (sp[0] && formData.has('fechaAudiencia')) sp[0].innerText = formData.get('fechaAudiencia');
                    if (sp[2] && formData.has('horaAudiencia'))  sp[2].innerText = formData.get('horaAudiencia');
                }
                const audTipo = row.querySelector('td:nth-child(3) span');
                if (audTipo && formData.has('tipoAudiencia'))        audTipo.innerText = formData.get('tipoAudiencia');
                const audComp = row.querySelector('td:nth-child(4) span');
                if (audComp && formData.has('abogadoCompareciente')) audComp.innerText = formData.get('abogadoCompareciente');
                const audResp = row.querySelector('td:nth-child(5) span');
                if (audResp && formData.has('abogadoResponsable'))   audResp.innerText = formData.get('abogadoResponsable');

            } else if (esTermino) {
                // ============================================
                // ETAPA 4: Términos
                // ============================================
                const termAct = row.querySelector('td:nth-child(2) span');
                if (termAct && formData.has('actuacion'))            termAct.innerText = formData.get('actuacion');
                const termFec = row.querySelector('td:nth-child(3) span.truncate');
                if (termFec && formData.has('fechaVencimiento'))     termFec.innerText = formData.get('fechaVencimiento');
                const termAbog = row.querySelector('td:nth-child(4) span');
                if (termAbog && formData.has('abogadoResponsable'))  termAbog.innerText = formData.get('abogadoResponsable');
            }

            // ANIMACIÓN HIGHLIGHT — cada TD por compatibilidad con sticky
            row.querySelectorAll('td').forEach(td => {
                td.style.transition = 'background-color 0.35s ease';
                td.style.backgroundColor = '#f0fdf4';
            });
            setTimeout(() => {
                row.querySelectorAll('td').forEach(td => {
                    td.style.transition = 'background-color 1.4s ease';
                    td.style.backgroundColor = '';
                });
            }, 2000);

        } else {
            Swal.fire({ icon: 'error', title: 'Error', text: json.error || 'No se pudo guardar.' });
        }
    } catch (e) {
        Swal.fire({ icon: 'error', title: 'Error de conexión', text: e.message });
    }
}

// ==========================================
// LÓGICA DE SELECTORES JERÁRQUICOS
// ==========================================

// CACHÉ EN MEMORIA para evitar peticiones repetidas al servidor
const _apiCache = new Map();
async function fetchCached(url) {
    if (_apiCache.has(url)) return _apiCache.get(url);
    const resp = await fetch(url);
    const data = await resp.json();
    _apiCache.set(url, data);
    return data;
}

async function cargarMaterias(gerenciaId, targetSelect, selectedValue = '') {
    if (!gerenciaId) {
        targetSelect.innerHTML = '<option value="">-- Primero selecciona una gerencia --</option>';
        targetSelect.disabled = true;
        return;
    }
    try {
        const loteIdInput = document.querySelector('input[name="loteId"]');
        const loteParam = loteIdInput ? `?loteId=${loteIdInput.value}` : '';
        const materias = await fetchCached(`/admin/importar/api/materias-por-gerencia/${gerenciaId}${loteParam}`);
        let html = '<option value="">-- Seleccione Materia --</option>';
        materias.forEach(m => {
            const isSelected = (m.nombre === selectedValue || m.id == selectedValue);
            const selected = isSelected ? 'selected' : '';
            html += `<option value="${m.nombre}" data-id="${m.id}" ${selected}>${m.nombre}</option>`;
        });
        targetSelect.innerHTML = html;
        targetSelect.disabled = false;
        if (selectedValue) {
            targetSelect.dispatchEvent(new Event('change'));
        }
    } catch (e) {
        console.error("Error cargando materias:", e);
    }
}

async function cargarAbogados(materiaId, targetSelect, selectedValue = '') {
    if (!materiaId) {
        targetSelect.innerHTML = '<option value="">-- Primero selecciona una materia --</option>';
        targetSelect.disabled = true;
        return;
    }
    try {
        const loteIdInput = document.querySelector('input[name="loteId"]');
        const loteParam = loteIdInput ? `?loteId=${loteIdInput.value}` : '';
        const usuarios = await fetchCached(`/admin/importar/api/usuarios-por-materia/${materiaId}${loteParam}`);
        let html = '<option value="">-- Seleccione Abogado --</option>';
        usuarios.forEach(u => {
            const isSelected = (u.nombre === selectedValue || u.id == selectedValue);
            const selected = isSelected ? 'selected' : '';
            html += `<option value="${u.nombre}" data-id="${u.id}" ${selected}>${u.nombre}</option>`;
        });
        targetSelect.innerHTML = html;
        targetSelect.disabled = false;
    } catch (e) {
        console.error("Error cargando abogados:", e);
    }
}

async function cargarOrganos(gerenciaId, targetSelect, selectedValue = '') {
    if (!gerenciaId) {
        targetSelect.innerHTML = '<option value="">-- Primero selecciona una gerencia --</option>';
        targetSelect.disabled = true;
        return;
    }
    try {
        const loteIdInput = document.querySelector('input[name="loteId"]');
        const loteParam = loteIdInput ? `?loteId=${loteIdInput.value}` : '';
        const organos = await fetchCached(`/admin/importar/api/organos-por-gerencia/${gerenciaId}${loteParam}`);
        let html = '<option value="">-- Seleccione Órgano Jurisdiccional --</option>';
        organos.forEach(o => {
            const isSelected = (o.nombre === selectedValue || o.id == selectedValue);
            const selected = isSelected ? 'selected' : '';
            html += `<option value="${o.nombre}" data-id="${o.id}" ${selected}>${o.nombre}</option>`;
        });
        targetSelect.innerHTML = html;
        targetSelect.disabled = false;
    } catch (e) {
        console.error("Error cargando órganos:", e);
    }
}

async function cargarTiposAudiencia(gerenciaId, targetSelect, selectedValue = '') {
    if (!gerenciaId) {
        targetSelect.innerHTML = '<option value="">-- Primero selecciona una gerencia --</option>';
        targetSelect.disabled = true;
        return;
    }
    try {
        const loteIdInput = document.querySelector('input[name="loteId"]');
        const loteParam = loteIdInput ? `?loteId=${loteIdInput.value}` : '';
        const tipos = await fetchCached(`/admin/importar/api/tipos-audiencia-por-gerencia/${gerenciaId}${loteParam}`);
        let html = '<option value="">-- Seleccione Tipo de Audiencia --</option>';
        tipos.forEach(t => {
            const isSelected = (t.nombre === selectedValue || t.id == selectedValue);
            const selected = isSelected ? 'selected' : '';
            html += `<option value="${t.nombre}" data-id="${t.id}" ${selected}>${t.nombre}</option>`;
        });
        targetSelect.innerHTML = html;
        targetSelect.disabled = false;
    } catch (e) {
        console.error("Error cargando tipos de audiencia:", e);
    }
}

// ==========================================
// LÓGICA DE SELECTORES JERÁRQUICOS Y MATCHING INTELIGENTE
// ==========================================
function seleccionarOpcionPorIdONombre(selectEl, id, nombre, triggerChange = true) {
    if (!selectEl) return false;
    let found = false;

    // 1. Limpieza de datos (Evitamos strings "null" o "undefined")
    const cleanId = (id && id !== 'null' && id !== 'undefined' && id !== '') ? id.toString() : null;
    const cleanNombre = (nombre && nombre !== 'null' && nombre !== 'undefined' && nombre !== '') ? nombre.toString().trim().toLowerCase() : null;

    // 2. Intentar buscar por ID
    if (cleanId) {
        for (let i = 0; i < selectEl.options.length; i++) {
            if (selectEl.options[i].getAttribute('data-id') == cleanId) {
                selectEl.selectedIndex = i;
                found = true;
                break;
            }
        }
    }

    // 3. Intentar buscar por Texto (Ignorando mayúsculas y espacios extra)
    if (!found && cleanNombre) {
        for (let i = 0; i < selectEl.options.length; i++) {
            const optVal = selectEl.options[i].value.trim().toLowerCase();
            const optText = selectEl.options[i].text.trim().toLowerCase();

            if (optVal === cleanNombre || optText === cleanNombre) {
                selectEl.selectedIndex = i;
                found = true;
                break;
            }
        }
    }

    // 4. Disparar el evento solo si se requiere (Evita Race Conditions)
    if (found && triggerChange) {
        selectEl.dispatchEvent(new Event('change'));
    }

    return found; // Retornamos si tuvimos éxito
}

function setupHierarchicalSelects(prefix) {
    const gerenciaSel = document.getElementById(`${prefix}-gerencia`);
    const materiaSel = document.getElementById(`${prefix}-materia`);
    const abogadoSel = document.getElementById(`${prefix}-abogado`) || document.getElementById(`${prefix}-responsable`) || document.getElementById(`${prefix}-compareciente`);

    if (gerenciaSel) {
        gerenciaSel.addEventListener('change', function () {
            const selectedOption = this.options[this.selectedIndex];
            const gerenciaId = selectedOption ? selectedOption.getAttribute('data-id') : null;
            
            if (materiaSel) {
                cargarMaterias(gerenciaId, materiaSel);
            }
            
            const organos = document.querySelectorAll(`[id^="${prefix}-"][id$="organo"]`);
            organos.forEach(sel => cargarOrganos(gerenciaId, sel));
            
            const tiposAud = document.querySelectorAll(`[id^="${prefix}-"][id$="tipo"]`);
            tiposAud.forEach(sel => cargarTiposAudiencia(gerenciaId, sel));
            
            if (abogadoSel) {
                abogadoSel.innerHTML = '<option value="">-- Primero selecciona una materia --</option>';
                abogadoSel.disabled = true;
            }
        });
    }

    if (materiaSel) {
        materiaSel.addEventListener('change', function () {
            const selectedOption = this.options[this.selectedIndex];
            const materiaId = selectedOption ? selectedOption.getAttribute('data-id') : null;
            const abogados = document.querySelectorAll(`[id^="${prefix}-"][id$="abogado"], [id^="${prefix}-"][id$="responsable"], [id^="${prefix}-"][id$="compareciente"]`);
            abogados.forEach(sel => {
                if (sel.id !== `${prefix}-materia` && sel.id !== `${prefix}-gerencia`) {
                    cargarAbogados(materiaId, sel);
                }
            });
        });
    }
}

document.addEventListener('DOMContentLoaded', () => {
    setupHierarchicalSelects('exp');
    // aud y term ya no tienen selects jerárquicos en sus modales
});

// ==========================================
// MODALES: ETAPA 2 - EXPEDIENTES
// ==========================================
document.querySelectorAll('.btn-editar-registro').forEach(btn => {
    btn.addEventListener('click', async function () {
        // RESET FORM PRE-LOAD
        const formExp = document.getElementById('form-editar-expediente');
        if (formExp) formExp.reset();

        document.getElementById('exp-materia').innerHTML = '<option value="">-- Cargando... --</option>';
        document.getElementById('exp-materia').disabled = true;
        const expOrgReset = document.getElementById('exp-organo');
        if (expOrgReset) {
            expOrgReset.innerHTML = '<option value="">-- Cargando... --</option>';
            expOrgReset.disabled = true;
        }

        const d = this.dataset;
        document.getElementById('exp-registro-id').value = d.registroId;
        document.getElementById('exp-numero').value = d.numeroExpediente || '';
        document.getElementById('exp-partes').value = d.partes || '';
        document.getElementById('exp-descripcion').value = d.descripcion || '';

        const sedeSel = document.getElementById('exp-sede');
        seleccionarOpcionPorIdONombre(sedeSel, d.sedeId, d.sede, false);

        // ---- GERENCIA BLOQUEADA ----
        // La gerencia se toma del dataset y se muestra en el campo visual, no en un select.
        const gerenciaNombre = (d.gerencia && d.gerencia !== 'null' && d.gerencia.trim() !== '') ? d.gerencia.trim() : 'Sin gerencia definida';
        const gerenciaId     = (d.gerenciaId && d.gerenciaId !== 'null' && d.gerenciaId.trim() !== '') ? d.gerenciaId.trim() : d.gerencia?.trim();

        // Mostrar en el campo visual
        const labelEl = document.getElementById('exp-gerencia-label');
        if (labelEl) labelEl.textContent = gerenciaNombre;

        // Guardar en los inputs ocultos para que el formulario los envíe / el JS los lea
        const hiddenGer = document.getElementById('exp-gerencia');
        if (hiddenGer) hiddenGer.value = gerenciaNombre;
        const hiddenGerId = document.getElementById('exp-gerencia-id');
        if (hiddenGerId) hiddenGerId.value = gerenciaId || '';

        const materiaSel = document.getElementById('exp-materia');

        // Cargar materias y organos usando la gerencia anclada
        if (gerenciaId) {
            await Promise.all([
                cargarMaterias(gerenciaId, materiaSel, d.materiaId || d.materia),
                cargarOrganos(gerenciaId, expOrgReset, d.organoId || d.organo)
            ]);
        } else {
            materiaSel.innerHTML = '<option value="">-- Sin gerencia detectada. Ajusta los catálogos --</option>';
        }

        const modal = document.getElementById('modal-editar-expediente');
        modal.classList.remove('hidden'); modal.classList.add('flex');
    });
});
['cerrar-modal-exp', 'cancelar-modal-exp'].forEach(id => {
    const el = document.getElementById(id);
    if (el) el.addEventListener('click', () => {
        const modal = document.getElementById('modal-editar-expediente');
        modal.classList.add('hidden'); modal.classList.remove('flex');
    });
});
const formExp = document.getElementById('form-editar-expediente');
if (formExp) formExp.addEventListener('submit', function (e) { e.preventDefault(); guardarEdicionRegistro(this); });

// ==========================================
// MODALES: ETAPA 3 - AUDIENCIAS
// ==========================================
document.querySelectorAll('.btn-editar-audiencia').forEach(btn => {
    btn.addEventListener('click', async function () {
        // RESET FORM PRE-LOAD
        const formAud = document.getElementById('form-editar-audiencia');
        if (formAud) formAud.reset();
        ['aud-tipo', 'aud-responsable', 'aud-compareciente'].forEach(id => {
            const el = document.getElementById(id);
            if (el) {
                el.innerHTML = '<option value="">-- Cargando opciones... --</option>';
                el.disabled = true;
            }
        });

        const d = this.dataset;
        document.getElementById('aud-registro-id').value = d.registroId;
        document.getElementById('aud-numero').value = d.numeroExpediente || '';
        document.getElementById('aud-fecha').value = d.fechaAudiencia || '';
        document.getElementById('aud-hora').value = d.horaAudiencia || '';
        document.getElementById('aud-observaciones').value = d.observaciones || '';

        const sedeSel = document.getElementById('aud-sede');
        seleccionarOpcionPorIdONombre(sedeSel, d.sedeId, d.sede, false);

        // Optimizado: usar variables de dataset en lugar de <select>s para Etapa 3
        const activeGerenciaId = d.gerenciaId || d.gerencia;
        const activeMateriaId = d.materiaId || d.materia;

        if (activeGerenciaId) {
            const audTipo = document.getElementById('aud-tipo');
            if (audTipo) await cargarTiposAudiencia(activeGerenciaId, audTipo, d.tipoAudienciaId || d.tipoAudiencia);
        }

        if (activeMateriaId) {
            await cargarAbogados(activeMateriaId, document.getElementById('aud-responsable'), d.abogadoResponsableId || d.abogadoResponsable || d.abogadoId);
            await cargarAbogados(activeMateriaId, document.getElementById('aud-compareciente'), d.abogadoComparecienteId || d.abogadoCompareciente || d.abogadoId);
        }

        const audSala = document.getElementById('aud-sala');
        if (audSala) audSala.value = d.sala || '';
        const esVirtual = d.esEnLinea === 'true' || d.esVirtual === 'true';
        const rdPresencial = document.getElementById('aud-presencial');
        const rdVirtual = document.getElementById('aud-virtual');
        const urlGroup = document.getElementById('aud-url-group');
        if (rdPresencial && rdVirtual) {
            rdPresencial.checked = !esVirtual;
            rdVirtual.checked = esVirtual;
        }
        if (urlGroup) urlGroup.classList.toggle('hidden', !esVirtual);
        const audUrl = document.getElementById('aud-url');
        if (audUrl) audUrl.value = d.enlaceReunion || '';

        const modal = document.getElementById('modal-editar-audiencia');
        modal.classList.remove('hidden'); modal.classList.add('flex');
    });
});
['cerrar-modal-aud', 'cancelar-modal-aud'].forEach(id => {
    const el = document.getElementById(id);
    if (el) el.addEventListener('click', () => {
        const modal = document.getElementById('modal-editar-audiencia');
        modal.classList.add('hidden'); modal.classList.remove('flex');
    });
});
const formAud = document.getElementById('form-editar-audiencia');
if (formAud) formAud.addEventListener('submit', function (e) { e.preventDefault(); guardarEdicionRegistro(this); });

// ==========================================
// MODALES: ETAPA 4 - TÉRMINOS
// ==========================================
document.querySelectorAll('.btn-editar-termino').forEach(btn => {
    btn.addEventListener('click', async function () {
        // RESET FORM PRE-LOAD
        const formTerm = document.getElementById('form-editar-termino');
        if (formTerm) formTerm.reset();

        document.getElementById('term-abogado').innerHTML = '<option value="">-- Cargando opciones... --</option>';
        document.getElementById('term-abogado').disabled = true;

        const d = this.dataset;
        document.getElementById('term-registro-id').value = d.registroId;
        document.getElementById('term-numero').value = d.numeroExpediente || '';
        document.getElementById('term-actuacion').value = d.actuacion || '';
        document.getElementById('term-fecha').value = d.fechaVencimiento || '';
        document.getElementById('term-fecha-ingreso').value = d.fechaIngreso || '';
        document.getElementById('term-prioridad').value = d.prioridad || 'Media';
        document.getElementById('term-observaciones').value = d.observaciones || '';

        const sedeSel = document.getElementById('term-sede');
        if (sedeSel) seleccionarOpcionPorIdONombre(sedeSel, d.sedeId, d.sede, false);

        // Optimizado: usar variables de dataset en lugar de <select>s para Etapa 4
        const activeMateriaId = d.materiaId || d.materia;

        if (activeMateriaId) {
            await cargarAbogados(activeMateriaId, document.getElementById('term-abogado'), d.abogadoId || d.abogadoResponsable);
        }

        const modal = document.getElementById('modal-editar-termino');
        modal.classList.remove('hidden'); modal.classList.add('flex');
    });
});
['cerrar-modal-term', 'cancelar-modal-term'].forEach(id => {
    const el = document.getElementById(id);
    if (el) el.addEventListener('click', () => {
        const modal = document.getElementById('modal-editar-termino');
        modal.classList.add('hidden'); modal.classList.remove('flex');
    });
});
const formTerm = document.getElementById('form-editar-termino');
if (formTerm) formTerm.addEventListener('submit', function (e) { e.preventDefault(); guardarEdicionRegistro(this); });