// ============================================================
// SPINNER GLOBAL DE CARGA — Migración Inteligente
// Inyecta CSS + DOM dinámicamente, sin archivos externos extra.
// ============================================================
(function _initSpinner() {
    // 1. Inyectar estilos CSS (solo una vez)
    if (document.getElementById('mig-spinner-style')) return;
    const style = document.createElement('style');
    style.id = 'mig-spinner-style';
    style.textContent = `
        #mig-spinner-overlay {
            position: fixed; inset: 0; z-index: 99999;
            display: flex; flex-direction: column;
            align-items: center; justify-content: center;
            background: rgba(15, 23, 42, 0.45);
            backdrop-filter: blur(3px);
            opacity: 0; pointer-events: none;
            transition: opacity 0.18s ease;
        }
        #mig-spinner-overlay.visible {
            opacity: 1; pointer-events: all;
        }
        .mig-spinner-ring {
            width: 52px; height: 52px;
            border: 5px solid rgba(255,255,255,0.25);
            border-top-color: #c9a227;
            border-radius: 50%;
            animation: mig-spin 0.72s linear infinite;
        }
        @keyframes mig-spin { to { transform: rotate(360deg); } }
        .mig-spinner-label {
            margin-top: 14px;
            color: #fff;
            font-size: 13px;
            font-weight: 600;
            letter-spacing: 0.03em;
            text-shadow: 0 1px 4px rgba(0,0,0,0.4);
        }
    `;
    document.head.appendChild(style);

    // 2. Crear el elemento overlay
    const overlay = document.createElement('div');
    overlay.id = 'mig-spinner-overlay';
    overlay.innerHTML = `
        <div class="mig-spinner-ring"></div>
        <span class="mig-spinner-label" id="mig-spinner-label">Procesando...</span>
    `;
    document.body.appendChild(overlay);
})();

/**
 * Muestra el spinner de carga global con un texto opcional.
 * @param {string} [label='Procesando...'] Texto debajo del spinner
 */
function showSpinner(label = 'Procesando...') {
    const overlay = document.getElementById('mig-spinner-overlay');
    const labelEl = document.getElementById('mig-spinner-label');
    if (!overlay) return;
    if (labelEl) labelEl.textContent = label;
    overlay.classList.add('visible');
}

/** Oculta el spinner de carga global. */
function hideSpinner() {
    const overlay = document.getElementById('mig-spinner-overlay');
    if (overlay) overlay.classList.remove('visible');
}

// ============================================================
// HOOK EN FORMULARIOS DE NAVEGACIÓN DE ETAPA
// Al hacer submit en formularios de avanzar/retroceder/mapear/
// inyectar, mostramos el spinner con texto de contexto.
// Usamos delegación sobre el documento para capturar cualquier form
// dinámico (Thymeleaf fragments).
// ============================================================
document.addEventListener('submit', function (e) {
    const form = e.target;
    if (!form || form.tagName !== 'FORM') return;
    const action = form.getAttribute('action') || '';

    // Solo interceptamos los forms de navegación de migración (no los modales AJAX)
    if (action.includes('/admin/importar/')) {
        // Los forms de edición AJAX son manejados por guardarEdicionRegistro → no tienen action
        // Aquí sólo capturamos los forms con action explícito (avanzar, retroceder, mapear, inyectar, etc.)
        if (action.includes('mapear-catalogos')) {
            showSpinner('Guardando mapeo de catálogos...');
        } else if (action.includes('validar-expedientes')) {
            showSpinner('Validando expedientes...');
        } else if (action.includes('validar-audiencias')) {
            showSpinner('Validando audiencias...');
        } else if (action.includes('avanzar')) {
            showSpinner('Avanzando a la siguiente etapa...');
        } else if (action.includes('retroceder')) {
            showSpinner('Regresando a la etapa anterior...');
        } else if (action.includes('inyectar')) {
            showSpinner('Inyectando datos a la base oficial...');
        } else if (action.includes('guardar-gerencia-global')) {
            showSpinner('Guardando gerencia del lote...');
        } else if (action.includes('cancelar')) {
            showSpinner('Cancelando lote de migración...');
        } else {
            showSpinner('Procesando...');
        }
    }
}, true); // capture=true para atrapar antes de cualquier otro handler

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
                let count = 0;
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
                        count++;
                    }
                });

                Swal.fire({
                    icon: count > 0 ? 'success' : 'info',
                    title: count > 0 ? '¡Listo!' : 'Sin cambios',
                    text: count > 0 ? `Se han marcado ${count} elementos vacíos como nuevos en todas las pestañas.` : 'No se encontraron elementos vacíos en ninguna pestaña.',
                    timer: 2500,
                    showConfirmButton: false,
                    toast: true,
                    position: 'top-end'
                });

                this.innerHTML = '<i class="fas fa-undo mr-2"></i> Restaurar Vacíos';
                this.classList.add('bg-amber-100', 'border-amber-400', 'text-amber-800');
                this.classList.remove('bg-gray-100', 'border-gray-300', 'text-gray-700');
            } else {
                // DESACTIVAR: Deshacer todos los que se marcaron como nuevo
                let countUndo = 0;
                document.querySelectorAll('.btn-deshacer-nuevo').forEach(b => {
                    const catId = b.getAttribute('data-cat-id');
                    const badge = document.getElementById('nuevo-badge-' + catId);
                    if (badge && !badge.classList.contains('hidden')) {
                        b.click();
                        countUndo++;
                    }
                });

                if (countUndo > 0) {
                    Swal.fire({
                        icon: 'info',
                        title: 'Restaurado',
                        text: `Se han restaurado ${countUndo} elementos en el lote.`,
                        timer: 2000,
                        showConfirmButton: false,
                        toast: true,
                        position: 'top-end'
                    });
                }

                this.innerHTML = '<i class="fas fa-plus-square mr-2"></i> Crear TODOS como Nuevos';
                this.classList.remove('bg-amber-100', 'border-amber-400', 'text-amber-800');
                this.classList.add('bg-gray-100', 'border-gray-300', 'text-gray-700');
            }
        });
    }

    // ==========================================
    // VALIDACIÓN GLOBAL AL ENVIAR FORMULARIO
    // ==========================================
    const formMapeo = document.querySelector('form[action*="mapear-catalogos"]');
    if (formMapeo) {
        formMapeo.addEventListener('submit', function (e) {
            // Verificar si hay campos requeridos vacíos que NO estén ocultos (es decir, que no tengan acción de nuevo/descartar)
            const containersPendientes = Array.from(document.querySelectorAll('[id^="mapeo-container-"]:not(.hidden)'));

            let erroresCount = 0;
            let categoriasConErrores = new Set();

            containersPendientes.forEach(container => {
                const selectEl = container.querySelector('select');
                if (selectEl) {
                    const val = selectEl.tomselect ? selectEl.tomselect.getValue() : selectEl.value;
                    if (!val) {
                        erroresCount++;
                        // Intentar hallar el nombre del panel (pestaña) para avisar dónde buscar
                        const panel = container.closest('.tab-panel');
                        if (panel) {
                            const tabBtn = document.querySelector(`button[data-target="${panel.id}"]`);
                            if (tabBtn) {
                                // Limpiamos el texto (ej. "Gerencias (2)" -> "Gerencias")
                                let labelText = tabBtn.innerText.replace(/\(\d+\)/, '').trim();
                                categoriasConErrores.add(labelText);
                            }
                        }
                    }
                }
            });

            if (erroresCount > 0) {
                e.preventDefault();
                const listaCategorias = Array.from(categoriasConErrores).join(', ');
                Swal.fire({
                    icon: 'warning',
                    title: 'Faltan datos por homologar',
                    html: `Aún tienes <b>${erroresCount}</b> elemento(s) pendiente(s) de mapear.<br><br>` +
                        `Revisa las pestañas: <span class="text-gob-guinda font-bold">${listaCategorias}</span>.<br><br>` +
                        `Debes seleccionar un valor oficial o marcarlos como nuevos con el botón <i class="fas fa-plus"></i>.`,
                    confirmButtonColor: '#1a3c34'
                });
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
        confirmButtonColor: '#16332cff',
        cancelButtonColor: '#7b1c2e',
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
    const esTermino = formData.has('fechaVencimiento') && !formData.has('fechaAudiencia');
    const esExpediente = !esAudiencia && !esTermino;

    showSpinner('Guardando cambios...');
    try {
        const resp = await fetch('/admin/importar/editar-registro', { method: 'POST', body: data });
        const json = await resp.json();
        hideSpinner();

        if (resp.ok && json.status === 'ok') {

            // 1. Cerrar el modal INMEDIATAMENTE — buscar por relación directa con el formulario
            const formParent = formEl.closest('[id^="modal-"]');
            if (formParent) { formParent.classList.add('hidden'); formParent.classList.remove('flex'); } else {
                // Fallback por selector
                const openModal = document.querySelector('[id^="modal-"].flex:not(.hidden)');
                if (openModal) { openModal.classList.add('hidden'); openModal.classList.remove('flex'); }
            }

            // 2. Mostrar toast de éxito
            Swal.fire({ icon: 'success', title: '¡Guardado!', text: json.mensaje, timer: 1200, showConfirmButton: false });

            // 3. Actualizar fila en tiempo real
            let row = document.getElementById('row-' + registroId);
            if (!row) row = document.getElementById('row-nuevo-' + registroId);
            if (!row) row = document.getElementById('row-existente-' + registroId);
            if (!row) return;

            // Actualizar data-attributes del botón de editar
            const btnEditar = row.querySelector('[class*="btn-editar"]');
            if (btnEditar) {
                formData.forEach((value, key) => {
                    const attrName = 'data-' + key.replace(/([a-z0-9]|(?=[A-Z]))([A-Z])/g, '$1-$2').toLowerCase();
                    btnEditar.setAttribute(attrName, value);
                });
                if (json.gerenciaId) btnEditar.setAttribute('data-gerencia-id', json.gerenciaId);
                if (json.materiaId) btnEditar.setAttribute('data-materia-id', json.materiaId);
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
                    if (spMs[0] && formData.has('materia')) spMs[0].innerText = formData.get('materia');
                    if (spMs[1] && formData.has('gerencia')) spMs[1].innerText = formData.get('gerencia');
                }

                // Recalcular badges de validación
                const numVal = formData.get('numeroExpediente') || '';
                const matVal = formData.get('materia') || '';
                const gerVal = formData.get('gerencia') || '';
                const faltantes = [];
                if (!numVal || numVal.startsWith('SIN_NUMERO')) faltantes.push('Número de Expediente');
                if (!matVal || matVal === 'Por asignar') faltantes.push('Materia');
                if (!gerVal) faltantes.push('Gerencia');

                const validacionTd = row.querySelector('td:nth-child(5)');
                const estadoTd = row.querySelector('td:nth-child(6)');
                if (validacionTd) {
                    validacionTd.innerHTML = faltantes.length === 0
                        ? '<span class="text-[10px] text-green-600"><i class="fas fa-check mr-1"></i>Completo</span>'
                        : '<div class="flex flex-wrap gap-1">' + faltantes.map(f => {
                            const esCrit = ['Número de Expediente', 'Materia', 'Gerencia'].includes(f);
                            const cls = esCrit ? 'bg-red-100 text-red-700' : 'bg-yellow-100 text-yellow-700';
                            return `<span class="px-2 py-0.5 rounded-full text-[9px] font-bold uppercase tracking-tighter ${cls}">FALTA ${f}</span>`;
                        }).join('') + '</div>';
                }
                if (estadoTd) {
                    if (faltantes.length === 0) {
                        estadoTd.innerHTML = '<span class="bg-green-100 text-green-800 px-2 py-0.5 rounded-full text-[10px] font-bold uppercase"><i class="fas fa-heartbeat mr-1"></i>Óptimo</span>';
                    } else {
                        const esCrit = faltantes.some(f => ['Número de Expediente', 'Materia', 'Gerencia'].includes(f));
                        estadoTd.innerHTML = esCrit
                            ? '<span class="bg-red-100 text-red-800 px-2 py-0.5 rounded-full text-[10px] font-bold uppercase"><i class="fas fa-skull-crossbones mr-1"></i>Crítico</span>'
                            : '<span class="bg-yellow-100 text-yellow-800 px-2 py-0.5 rounded-full text-[10px] font-bold uppercase"><i class="fas fa-exclamation-triangle mr-1"></i>Incompleto</span>';
                    }
                }

            } else if (esAudiencia) {
                // ============================================
                // ETAPA 3: Audiencias
                // Columnas: 1=Exp, 2=Fecha/Hora, 3=Tipo, 4=Compareciente, 5=Responsable, 6=Sede, 7=Acción
                // ============================================
                const fhDiv = row.querySelector('td:nth-child(2) .flex');
                if (fhDiv) {
                    const sp = fhDiv.querySelectorAll('span');
                    if (sp[0] && formData.has('fechaAudiencia')) sp[0].innerText = formData.get('fechaAudiencia');
                    if (sp[2] && formData.has('horaAudiencia')) sp[2].innerText = formData.get('horaAudiencia');
                }
                const audTipo = row.querySelector('td:nth-child(3) span');
                if (audTipo && formData.has('tipoAudiencia')) audTipo.innerText = formData.get('tipoAudiencia');
                const audComp = row.querySelector('td:nth-child(4) span');
                if (audComp && formData.has('abogadoCompareciente')) audComp.innerText = formData.get('abogadoCompareciente');
                const audResp = row.querySelector('td:nth-child(5) span');
                if (audResp && formData.has('abogadoResponsable')) audResp.innerText = formData.get('abogadoResponsable');
                // Sede está en la 6ª columna
                const audSede = row.querySelector('td:nth-child(6) span');
                if (audSede && formData.has('sede')) audSede.innerText = formData.get('sede');

            } else if (esTermino) {
                // ============================================
                // ETAPA 4: Términos
                // Columnas: 1=Exp, 2=Actuación, 3=Vencimiento, 4=Abogado, 5=Prioridad, 6=Acción
                // OJO: el campo del form se llama 'asunto' (no 'actuacion')
                // ============================================
                const termAct = row.querySelector('td:nth-child(2) span');
                // El form envía 'asunto' según el HTML del modal
                const actuacionVal = formData.get('asunto') || formData.get('actuacion') || '';
                if (termAct && actuacionVal) termAct.innerText = actuacionVal;

                const termFec = row.querySelector('td:nth-child(3) span.truncate');
                if (termFec && formData.has('fechaVencimiento')) termFec.innerText = formData.get('fechaVencimiento');

                const termAbog = row.querySelector('td:nth-child(4) span');
                if (termAbog && formData.has('abogadoResponsable')) termAbog.innerText = formData.get('abogadoResponsable');

                // Prioridad — actualizar texto Y clases de color
                const termPrioSpan = row.querySelector('td:nth-child(5) span');
                const prioVal = formData.get('prioridad') || '';
                if (termPrioSpan && prioVal) {
                    termPrioSpan.innerText = prioVal;
                    termPrioSpan.className = 'text-xs font-bold px-2 py-0.5 rounded-full ';
                    if (prioVal === 'Alta') termPrioSpan.className += 'bg-red-100 text-red-700';
                    else if (prioVal === 'Baja') termPrioSpan.className += 'bg-gray-100 text-gray-600';
                    else termPrioSpan.className += 'bg-yellow-100 text-yellow-700';
                }

            } // fin if/else etapas

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
        hideSpinner();
        Swal.fire({ icon: 'error', title: 'Error de conexión', text: e.message });
    }
}

// ==========================================
// LÓGICA DE SELECTORES JERÁRQUICOS
// ==========================================

// ==========================================
// CACHÉ GLOBAL + BATCH PRE-LOAD DE CATÁLOGOS
// ==========================================
// _apiCache: caché de respuestas HTTP individuales (fallback)
const _apiCache = new Map();
async function fetchCached(url) {
    if (_apiCache.has(url)) return _apiCache.get(url);
    const resp = await fetch(url);
    const data = await resp.json();
    _apiCache.set(url, data);
    return data;
}

// _catalogosBatch: catálogos completos pre-cargados en paralelo al entrar a la página.
// Estructura:
//   _catalogosBatch.abogados       → Array de todos los abogados
//   _catalogosBatch.materias       → Map<gerenciaId, Array>
//   _catalogosBatch.organos        → Map<gerenciaId, Array>
//   _catalogosBatch.tiposAudiencia → Map<gerenciaId, Array>
window._catalogosBatch = null;
window._catalogosBatchPromise = null;

/**
 * Inicia la pre-carga en paralelo de TODOS los catálogos necesarios.
 * Se llama UNA SOLA VEZ al cargar la página (DOMContentLoaded).
 * Los modales esperan a que esta promesa se resuelva → primer modal ~igual de rápido que los demás.
 */
function preCatalogosBatch() {
    const loteIdInput = document.querySelector('input[name="loteId"]');
    if (!loteIdInput) return; // No hay lote activo (p.ej. página de INICIO)
    const loteId = loteIdInput.value;
    const loteParam = `?loteId=${loteId}`;

    // Recolectar todas las gerencias únicas que están en la página
    const gerenciaIds = new Set();
    document.querySelectorAll('[data-gerencia-id]').forEach(el => {
        const gid = el.getAttribute('data-gerencia-id');
        if (gid && gid !== 'null' && gid !== '' && gid !== 'undefined') gerenciaIds.add(gid);
    });

    const peticiones = [];

    // 1. Abogados (global, sin filtro)
    peticiones.push(
        fetchCached(`/admin/importar/api/todos-abogados${loteParam}`)
            .then(data => { if (!window._catalogosBatch) window._catalogosBatch = {}; window._catalogosBatch.abogados = data; })
    );

    // 2. Materias, Órganos y Tipos de Audiencia agrupados por gerencia
    gerenciaIds.forEach(gid => {
        peticiones.push(
            fetchCached(`/admin/importar/api/materias-por-gerencia/${gid}${loteParam}`)
                .then(data => {
                    if (!window._catalogosBatch) window._catalogosBatch = {};
                    if (!window._catalogosBatch.materias) window._catalogosBatch.materias = {};
                    window._catalogosBatch.materias[gid] = data;
                })
        );
        peticiones.push(
            fetchCached(`/admin/importar/api/organos-por-gerencia/${gid}${loteParam}`)
                .then(data => {
                    if (!window._catalogosBatch) window._catalogosBatch = {};
                    if (!window._catalogosBatch.organos) window._catalogosBatch.organos = {};
                    window._catalogosBatch.organos[gid] = data;
                })
        );
        peticiones.push(
            fetchCached(`/admin/importar/api/tipos-audiencia-por-gerencia/${gid}${loteParam}`)
                .then(data => {
                    if (!window._catalogosBatch) window._catalogosBatch = {};
                    if (!window._catalogosBatch.tiposAudiencia) window._catalogosBatch.tiposAudiencia = {};
                    window._catalogosBatch.tiposAudiencia[gid] = data;
                })
        );
    });

    window._catalogosBatchPromise = Promise.all(peticiones).then(() => {
        console.log('[MigracionBatch] Catálogos pre-cargados:', {
            abogados: (window._catalogosBatch?.abogados || []).length,
            gerencias: gerenciaIds.size
        });
    }).catch(e => console.error('[MigracionBatch] Error pre-cargando catálogos:', e));
}

/** Esperar a que el batch esté listo antes de abrir un modal. */
async function esperarBatch() {
    if (window._catalogosBatchPromise) await window._catalogosBatchPromise;
}

// ==========================================
// FUNCIONES DE CARGA DE SELECTS
// Ahora usan primero _catalogosBatch (memoria local)
// y solo hacen fetch si el dato no está pre-cargado.
// ==========================================

async function cargarMaterias(gerenciaId, targetSelect, selectedValue = '') {
    if (!gerenciaId) {
        targetSelect.innerHTML = '<option value="">-- Primero selecciona una gerencia --</option>';
        targetSelect.disabled = true;
        return;
    }
    try {
        let materias;
        // Intentar leer del batch pre-cargado primero
        if (window._catalogosBatch?.materias?.[gerenciaId]) {
            materias = window._catalogosBatch.materias[gerenciaId];
        } else {
            // Fallback: petición individual (p.ej. si el usuario cambió de gerencia)
            const loteIdInput = document.querySelector('input[name="loteId"]');
            const loteParam = loteIdInput ? `?loteId=${loteIdInput.value}` : '';
            materias = await fetchCached(`/admin/importar/api/materias-por-gerencia/${gerenciaId}${loteParam}`);
            // Guardar en batch para futuras llamadas
            if (!window._catalogosBatch) window._catalogosBatch = {};
            if (!window._catalogosBatch.materias) window._catalogosBatch.materias = {};
            window._catalogosBatch.materias[gerenciaId] = materias;
        }
        let html = '<option value="">-- Seleccione Materia --</option>';
        materias.forEach(m => {
            const isSelected = (m.nombre === selectedValue || m.id == selectedValue);
            html += `<option value="${m.nombre}" data-id="${m.id}" ${isSelected ? 'selected' : ''}>${m.nombre}</option>`;
        });
        targetSelect.innerHTML = html;
        targetSelect.disabled = false;
        if (selectedValue) targetSelect.dispatchEvent(new Event('change'));
    } catch (e) {
        console.error('Error cargando materias:', e);
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
            html += `<option value="${u.nombre}" data-id="${u.id}" ${isSelected ? 'selected' : ''}>${u.nombre}</option>`;
        });
        targetSelect.innerHTML = html;
        targetSelect.disabled = false;
    } catch (e) {
        console.error('Error cargando abogados:', e);
    }
}

/**
 * Carga TODOS los abogados activos en el select (sin filtrar por materia).
 * Lee primero del batch pre-cargado → instantáneo desde el segundo modal.
 */
function cargarTodosAbogados(targetSelect, selectedValue = '') {
    // Leer del batch si ya está disponible (síncrono)
    if (window._catalogosBatch?.abogados) {
        _rellenarSelectAbogados(targetSelect, window._catalogosBatch.abogados, selectedValue);
        return Promise.resolve();
    }
    // Fallback asíncrono si el batch aún no llegó
    const loteIdInput = document.querySelector('input[name="loteId"]');
    const loteParam = loteIdInput ? `?loteId=${loteIdInput.value}` : '';
    return fetchCached(`/admin/importar/api/todos-abogados${loteParam}`).then(todos => {
        if (!window._catalogosBatch) window._catalogosBatch = {};
        window._catalogosBatch.abogados = todos;
        _rellenarSelectAbogados(targetSelect, todos, selectedValue);
    }).catch(e => {
        console.error('Error cargando todos los abogados:', e);
        targetSelect.innerHTML = '<option value="">-- Error al cargar abogados --</option>';
    });
}

function _rellenarSelectAbogados(targetSelect, todos, selectedValue) {
    let html = '<option value="">-- Seleccione Abogado --</option>';
    todos.forEach(u => {
        const isSelected = (u.nombre === selectedValue || u.id == selectedValue);
        const materiaIds = (u.materiaIds || []).join(',');
        html += `<option value="${u.nombre}" data-id="${u.id}" data-materia-ids="${materiaIds}" data-gerencia-id="${u.gerenciaId || ''}" ${isSelected ? 'selected' : ''}>${u.nombre}</option>`;
    });
    targetSelect.innerHTML = html;
    targetSelect.disabled = false;
}

async function cargarOrganos(gerenciaId, targetSelect, selectedValue = '') {
    if (!gerenciaId) {
        targetSelect.innerHTML = '<option value="">-- Primero selecciona una gerencia --</option>';
        targetSelect.disabled = true;
        return;
    }
    try {
        let organos;
        if (window._catalogosBatch?.organos?.[gerenciaId]) {
            organos = window._catalogosBatch.organos[gerenciaId];
        } else {
            const loteIdInput = document.querySelector('input[name="loteId"]');
            const loteParam = loteIdInput ? `?loteId=${loteIdInput.value}` : '';
            organos = await fetchCached(`/admin/importar/api/organos-por-gerencia/${gerenciaId}${loteParam}`);
            if (!window._catalogosBatch) window._catalogosBatch = {};
            if (!window._catalogosBatch.organos) window._catalogosBatch.organos = {};
            window._catalogosBatch.organos[gerenciaId] = organos;
        }
        let html = '<option value="">-- Seleccione Órgano Jurisdiccional --</option>';
        organos.forEach(o => {
            const isSelected = (o.nombre === selectedValue || o.id == selectedValue);
            html += `<option value="${o.nombre}" data-id="${o.id}" ${isSelected ? 'selected' : ''}>${o.nombre}</option>`;
        });
        targetSelect.innerHTML = html;
        targetSelect.disabled = false;
    } catch (e) {
        console.error('Error cargando órganos:', e);
    }
}

async function cargarTiposAudiencia(gerenciaId, targetSelect, selectedValue = '') {
    if (!gerenciaId) {
        targetSelect.innerHTML = '<option value="">-- Primero selecciona una gerencia --</option>';
        targetSelect.disabled = true;
        return;
    }
    try {
        let tipos;
        if (window._catalogosBatch?.tiposAudiencia?.[gerenciaId]) {
            tipos = window._catalogosBatch.tiposAudiencia[gerenciaId];
        } else {
            const loteIdInput = document.querySelector('input[name="loteId"]');
            const loteParam = loteIdInput ? `?loteId=${loteIdInput.value}` : '';
            tipos = await fetchCached(`/admin/importar/api/tipos-audiencia-por-gerencia/${gerenciaId}${loteParam}`);
            if (!window._catalogosBatch) window._catalogosBatch = {};
            if (!window._catalogosBatch.tiposAudiencia) window._catalogosBatch.tiposAudiencia = {};
            window._catalogosBatch.tiposAudiencia[gerenciaId] = tipos;
        }
        let html = '<option value="">-- Seleccione Tipo de Audiencia --</option>';
        tipos.forEach(t => {
            const isSelected = (t.nombre === selectedValue || t.id == selectedValue);
            html += `<option value="${t.nombre}" data-id="${t.id}" ${isSelected ? 'selected' : ''}>${t.nombre}</option>`;
        });
        targetSelect.innerHTML = html;
        targetSelect.disabled = false;
    } catch (e) {
        console.error('Error cargando tipos de audiencia:', e);
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
                // Excluimos 'exp-abogado' porque ahora muestra TODOS los abogados
                if (sel.id !== `${prefix}-materia` && sel.id !== `${prefix}-gerencia` && sel.id !== 'exp-abogado') {
                    cargarAbogados(materiaId, sel);
                }
            });
        });
    }
}

document.addEventListener('DOMContentLoaded', () => {
    setupHierarchicalSelects('exp');
    // Iniciar pre-carga de catálogos en paralelo en background
    // así el primer modal ya tiene los datos listos (o casi).
    preCatalogosBatch();
    // aud y term ya no tienen selects jerárquicos en sus modales

    // Inicializar buscadores predictivos de expedientes
    inicializarAutocompleteExpediente('exp-numero');
    inicializarAutocompleteExpediente('aud-numero');
    inicializarAutocompleteExpediente('term-numero');
});

/**
 * Inicializa la búsqueda predictiva (autocomplete) en un campo de Número de Expediente.
 * @param {string} inputId ID del input de texto
 */
function inicializarAutocompleteExpediente(inputId) {
    const input = document.getElementById(inputId);
    if (!input) return;

    // Deshabilitar el autocompletado nativo del navegador para que no se encime
    input.setAttribute('autocomplete', 'off');

    // Crear dinámicamente el contenedor de resultados si no existe
    let resultsDiv = document.getElementById(inputId + '-results');
    if (!resultsDiv) {
        resultsDiv = document.createElement('div');
        resultsDiv.id = inputId + '-results';
        resultsDiv.className = 'absolute z-[100000] w-full bg-white border border-gray-200 rounded-lg shadow-xl mt-1 hidden overflow-hidden max-h-48 overflow-y-auto';

        // Asegurar que el contenedor padre tenga posicionamiento relativo para el absolute
        if (input.parentNode) {
            input.parentNode.style.position = 'relative';
            input.parentNode.appendChild(resultsDiv);
        }
    }

    let timeout = null;

    input.addEventListener('input', function () {
        const term = this.value.trim();
        clearTimeout(timeout);

        if (term.length < 2) {
            resultsDiv.classList.add('hidden');
            return;
        }

        timeout = setTimeout(async () => {
            const loteIdInput = document.querySelector('input[name="loteId"]');
            const loteId = loteIdInput ? loteIdInput.value : null;
            if (!loteId) return;

            try {
                const resp = await fetch(`/admin/importar/api/buscar-expedientes-similares?term=${encodeURIComponent(term)}&loteId=${loteId}`);
                if (!resp.ok) return;
                const data = await resp.json();

                if (data && data.length > 0) {
                    resultsDiv.innerHTML = data.map(num => `
                        <div class="px-4 py-2 hover:bg-gob-oro/10 cursor-pointer text-sm border-b border-gray-50 last:border-0" data-val="${num}">
                            <i class="fas fa-file-alt mr-2 text-gob-oro/60"></i><span class="font-medium">${num}</span>
                        </div>
                    `).join('');
                    resultsDiv.classList.remove('hidden');

                    // Evento click en cada resultado
                    resultsDiv.querySelectorAll('[data-val]').forEach(item => {
                        item.addEventListener('click', function () {
                            input.value = this.getAttribute('data-val');
                            resultsDiv.classList.add('hidden');
                        });
                    });
                } else {
                    resultsDiv.classList.add('hidden');
                }
            } catch (e) {
                console.error('Error buscando expedientes:', e);
            }
        }, 300);
    });

    // Cerrar si se hace clic fuera
    document.addEventListener('click', function (e) {
        if (!input.contains(e.target) && !resultsDiv.contains(e.target)) {
            resultsDiv.classList.add('hidden');
        }
    });

    // Cerrar con Escape
    input.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') {
            resultsDiv.classList.add('hidden');
        }
    });
}


// ==========================================
// MODALES: ETAPA 2 - EXPEDIENTES
// ==========================================
document.querySelectorAll('.btn-editar-registro').forEach(btn => {
    btn.addEventListener('click', async function () {
        // Mostrar spinner mientras se espera el batch de catálogos
        showSpinner('Abriendo editor...');
        await esperarBatch();

        // RESET FORM PRE-LOAD
        const formExp = document.getElementById('form-editar-expediente');
        if (formExp) formExp.reset();

        const d = this.dataset;
        document.getElementById('exp-registro-id').value = d.registroId;
        document.getElementById('exp-numero').value = d.numeroExpediente || '';
        document.getElementById('exp-partes').value = d.partes || '';
        document.getElementById('exp-descripcion').value = d.descripcion || '';

        const sedeSel = document.getElementById('exp-sede');
        seleccionarOpcionPorIdONombre(sedeSel, d.sedeId, d.sede, false);

        // ---- GERENCIA BLOQUEADA ----
        const gerenciaNombre = (d.gerencia && d.gerencia !== 'null' && d.gerencia.trim() !== '') ? d.gerencia.trim() : 'Sin gerencia definida';
        const gerenciaId = (d.gerenciaId && d.gerenciaId !== 'null' && d.gerenciaId.trim() !== '') ? d.gerenciaId.trim() : d.gerencia?.trim();

        const labelEl = document.getElementById('exp-gerencia-label');
        if (labelEl) labelEl.textContent = gerenciaNombre;

        const hiddenGer = document.getElementById('exp-gerencia');
        if (hiddenGer) hiddenGer.value = gerenciaNombre;
        const hiddenGerId = document.getElementById('exp-gerencia-id');
        if (hiddenGerId) hiddenGerId.value = gerenciaId || '';

        const materiaSel = document.getElementById('exp-materia');
        const expOrgReset = document.getElementById('exp-organo');
        const abogadoSel = document.getElementById('exp-abogado');

        // Cargar materias y organos (desde batch → instantáneo)
        if (gerenciaId) {
            await Promise.all([
                cargarMaterias(gerenciaId, materiaSel, d.materiaId || d.materia),
                cargarOrganos(gerenciaId, expOrgReset, d.organoId || d.organo)
            ]);
        } else {
            materiaSel.innerHTML = '<option value="">-- Sin gerencia detectada. Ajusta los catálogos --</option>';
        }

        // Cargar TODOS los abogados (desde batch → instantáneo)
        if (abogadoSel) {
            await cargarTodosAbogados(abogadoSel, d.abogadoId || d.abogado);
        }

        const modal = document.getElementById('modal-editar-expediente');
        hideSpinner();
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
if (formExp) {
    formExp.addEventListener('submit', async function (e) {
        e.preventDefault();

        const abogadoSel = document.getElementById('exp-abogado');
        const materiaSel = document.getElementById('exp-materia');

        const abogadoOpt = abogadoSel ? abogadoSel.options[abogadoSel.selectedIndex] : null;
        const materiaOpt = materiaSel ? materiaSel.options[materiaSel.selectedIndex] : null;

        const abogadoId = abogadoOpt ? abogadoOpt.getAttribute('data-id') : null;
        const abogadoNom = abogadoOpt ? abogadoOpt.text : '';
        const materiaId = materiaOpt ? materiaOpt.getAttribute('data-id') : null;
        const materiaNom = materiaOpt ? materiaOpt.text : '';

        // Dejar que abogadoId sea String por defecto, pero parsear materiaId
        const materiaIdNum = parseInt(materiaId);

        if (abogadoOpt && abogadoOpt.value && abogadoId && !isNaN(materiaIdNum)) {
            // Obtener las materias del abogado desde el data-attribute (ya cargadas en el option)
            const materiaIdsStr = abogadoOpt.getAttribute('data-materia-ids') || '';
            const materiaIds = materiaIdsStr ? materiaIdsStr.split(',').map(id => parseInt(id)) : [];
            const perteneceAMateria = materiaIds.includes(materiaIdNum);

            if (!perteneceAMateria) {
                // El abogado NO pertenece a esta materia → mostrar confirmación
                const result = await Swal.fire({
                    icon: 'warning',
                    title: 'Abogado fuera de materia',
                    html: `<div class="text-center text-sm text-gray-700 space-y-2">
                        <p><strong>${abogadoNom}</strong> no pertenece actualmente a la materia <strong>${materiaNom}</strong>.</p>
                        <p>¿Desea asignarle esta materia al abogado para continuar?</p>
                        <p class="text-xs text-blue-700 mt-2"><i class="fas fa-info-circle mr-1"></i>El abogado conservará sus materias actuales; solo se le añadirá esta nueva.</p>
                    </div>`,
                    showCancelButton: true,
                    confirmButtonColor: '#7b1c2e',
                    cancelButtonColor: '#6b7280',
                    confirmButtonText: '<i class="fas fa-user-plus mr-1"></i> Sí, asignar materia y guardar',
                    cancelButtonText: 'Cancelar'
                });

                if (!result.isConfirmed) return; // Usuario canceló → no guardar

                // Asignar la materia al abogado antes de guardar
                try {
                    const params = new URLSearchParams({ abogadoId: abogadoId, materiaId: materiaIdNum });
                    const resp = await fetch('/admin/importar/api/asignar-materia-abogado', { method: 'POST', body: params });
                    const json = await resp.json();
                    if (!resp.ok) throw new Error(json.error || 'Error al asignar materia');
                    // Limpiar caché para que la próxima carga refleje el cambio
                    _apiCache.clear();
                } catch (err) {
                    Swal.fire({ icon: 'error', title: 'Error', text: 'No se pudo asignar la materia: ' + err.message });
                    return;
                }
            }
        }

        // Todo validado → guardar el expediente normalmente
        guardarEdicionRegistro(this);
    });
}


// ==========================================
// MODALES: ETAPA 3 - AUDIENCIAS
// ==========================================
document.querySelectorAll('.btn-editar-audiencia').forEach(btn => {
    btn.addEventListener('click', async function () {
        // Mostrar spinner mientras se espera el batch de catálogos
        showSpinner('Abriendo editor...');
        await esperarBatch();

        // RESET FORM PRE-LOAD
        const formAud = document.getElementById('form-editar-audiencia');
        if (formAud) formAud.reset();

        const d = this.dataset;
        document.getElementById('aud-registro-id').value = d.registroId;
        document.getElementById('aud-numero').value = d.numeroExpediente || '';
        document.getElementById('aud-fecha').value = d.fechaAudiencia || '';
        document.getElementById('aud-hora').value = d.horaAudiencia || '';
        document.getElementById('aud-observaciones').value = d.observaciones || '';

        const sedeSel = document.getElementById('aud-sede');
        seleccionarOpcionPorIdONombre(sedeSel, d.sedeId, d.sede, false);

        const activeGerenciaId = d.gerenciaId || d.gerencia;

        if (activeGerenciaId) {
            const audTipo = document.getElementById('aud-tipo');
            if (audTipo) await cargarTiposAudiencia(activeGerenciaId, audTipo, d.tipoAudienciaId || d.tipoAudiencia);
        }

        // Cargar todos los abogados (desde batch → instantáneo)
        await cargarTodosAbogados(document.getElementById('aud-responsable'), d.abogadoResponsableId || d.abogadoResponsable || d.abogadoId);
        await cargarTodosAbogados(document.getElementById('aud-compareciente'), d.abogadoComparecienteId || d.abogadoCompareciente || d.abogadoId);

        // Bloquear solo el responsable
        document.getElementById('aud-responsable').disabled = true;

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
        hideSpinner();
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
        // Mostrar spinner mientras se espera el batch de catálogos
        showSpinner('Abriendo editor...');
        await esperarBatch();

        // RESET FORM PRE-LOAD
        const formTerm = document.getElementById('form-editar-termino');
        if (formTerm) formTerm.reset();

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

        // Cargar todos los abogados (desde batch → instantáneo)
        await cargarTodosAbogados(document.getElementById('term-abogado'), d.abogadoId || d.abogadoResponsable);
        // Bloquear abogado responsable según solicitud de consistencia de datos
        document.getElementById('term-abogado').disabled = true;

        const modal = document.getElementById('modal-editar-termino');
        hideSpinner();
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