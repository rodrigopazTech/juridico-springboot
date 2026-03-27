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

                    // Revisar si el select tiene un valor seleccionado
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
    const data = new URLSearchParams(new FormData(formEl));
    try {
        const resp = await fetch('/admin/importar/editar-registro', { method: 'POST', body: data });
        const json = await resp.json();
        if (resp.ok && json.status === 'ok') {
            Swal.fire({ icon: 'success', title: '¡Guardado!', text: json.mensaje, timer: 1500, showConfirmButton: false })
                .then(() => location.reload());
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

async function cargarMaterias(gerenciaId, targetSelect, selectedValue = '') {
    if (!gerenciaId) {
        targetSelect.innerHTML = '<option value="">-- Primero selecciona una gerencia --</option>';
        targetSelect.disabled = true;
        return;
    }
    try {
        const resp = await fetch(`/admin/importar/api/materias-por-gerencia/${gerenciaId}`);
        const materias = await resp.json();
        let html = '<option value="">-- Seleccione Materia --</option>';
        materias.forEach(m => {
            const selected = (m.nombre === selectedValue) ? 'selected' : '';
            html += `<option value="${m.nombre}" data-id="${m.id}" ${selected}>${m.nombre}</option>`;
        });
        targetSelect.innerHTML = html;
        targetSelect.disabled = false;

        // Si hay un valor seleccionado, disparar cambio para cargar abogados
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
        const resp = await fetch(`/admin/importar/api/usuarios-por-materia/${materiaId}`);
        const usuarios = await resp.json();
        let html = '<option value="">-- Seleccione Abogado --</option>';
        usuarios.forEach(u => {
            const selected = (u.nombre === selectedValue) ? 'selected' : '';
            html += `<option value="${u.nombre}" ${selected}>${u.nombre}</option>`;
        });
        targetSelect.innerHTML = html;
        targetSelect.disabled = false;
    } catch (e) {
        console.error("Error cargando abogados:", e);
    }
}

function setupHierarchicalSelects(prefix) {
    const gerenciaSel = document.getElementById(`${prefix}-gerencia`);
    const materiaSel = document.getElementById(`${prefix}-materia`);
    const abogadoSel = document.getElementById(`${prefix}-abogado`) || document.getElementById(`${prefix}-responsable`) || document.getElementById(`${prefix}-compareciente`);

    if (gerenciaSel && materiaSel) {
        gerenciaSel.addEventListener('change', function () {
            const selectedOption = this.options[this.selectedIndex];
            const gerenciaId = selectedOption ? selectedOption.getAttribute('data-id') : null;
            cargarMaterias(gerenciaId, materiaSel);
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

            // Si hay dos abogados (Compareciente y Responsable en Audiencias)
            const abogados = document.querySelectorAll(`[id^="${prefix}-"][id$="abogado"], [id^="${prefix}-"][id$="responsable"], [id^="${prefix}-"][id$="compareciente"]`);
            abogados.forEach(sel => {
                if (sel.id !== `${prefix}-materia` && sel.id !== `${prefix}-gerencia`) {
                    cargarAbogados(materiaId, sel);
                }
            });
        });
    }
}

// Inicializar jerarquías
document.addEventListener('DOMContentLoaded', () => {
    setupHierarchicalSelects('exp');
    setupHierarchicalSelects('aud');
    setupHierarchicalSelects('term');
});

// ==========================================
// MODALES: ETAPA 2 - EXPEDIENTES
// ==========================================
document.querySelectorAll('.btn-editar-registro').forEach(btn => {
    btn.addEventListener('click', async function () {
        const d = this.dataset;
        document.getElementById('exp-registro-id').value = d.registroId;
        document.getElementById('exp-numero').value = d.numeroExpediente || '';
        document.getElementById('exp-partes').value = d.partes || '';
        document.getElementById('exp-organo').value = d.organo || '';
        document.getElementById('exp-sede').value = d.sede || '';
        document.getElementById('exp-descripcion').value = d.descripcion || '';
        document.getElementById('exp-prioridad').value = d.prioridad || 'Media';
        document.getElementById('exp-etapa').value = d.etapaProcesal || 'TRAMITE';

        // Cargar jerarquía
        const gerenciaSel = document.getElementById('exp-gerencia');
        gerenciaSel.value = d.gerencia || '';

        if (gerenciaSel.value && gerenciaSel.selectedIndex >= 0) {
            const gerenciaId = gerenciaSel.options[gerenciaSel.selectedIndex].getAttribute('data-id');
            await cargarMaterias(gerenciaId, document.getElementById('exp-materia'), d.materia);

            const materiaSel = document.getElementById('exp-materia');
            if (materiaSel.value && materiaSel.selectedIndex >= 0) {
                const materiaId = materiaSel.options[materiaSel.selectedIndex].getAttribute('data-id');
                await cargarAbogados(materiaId, document.getElementById('exp-abogado'), d.abogado);
            }
        } else {
            document.getElementById('exp-materia').disabled = true;
            document.getElementById('exp-abogado').disabled = true;
        }

        const modal = document.getElementById('modal-editar-expediente');
        modal.classList.remove('hidden');
        modal.classList.add('flex');
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
        const d = this.dataset;
        document.getElementById('aud-registro-id').value = d.registroId;
        document.getElementById('aud-numero').value = d.numeroExpediente || '';
        document.getElementById('aud-fecha').value = d.fechaAudiencia || '';
        document.getElementById('aud-hora').value = d.horaAudiencia || '';
        document.getElementById('aud-tipo').value = d.tipoAudiencia || '';
        document.getElementById('aud-sede').value = d.sede || '';
        document.getElementById('aud-organo').value = d.organoJurisdiccional || '';

        // Jerarquía
        const gerenciaSel = document.getElementById('aud-gerencia');
        gerenciaSel.value = d.gerencia || '';
        if (gerenciaSel.value && gerenciaSel.selectedIndex >= 0) {
            const gerenciaId = gerenciaSel.options[gerenciaSel.selectedIndex].getAttribute('data-id');
            await cargarMaterias(gerenciaId, document.getElementById('aud-materia'), d.materia);

            const materiaSel = document.getElementById('aud-materia');
            if (materiaSel.value && materiaSel.selectedIndex >= 0) {
                const materiaId = materiaSel.options[materiaSel.selectedIndex].getAttribute('data-id');
                await cargarAbogados(materiaId, document.getElementById('aud-responsable'), d.abogadoResponsable);
                await cargarAbogados(materiaId, document.getElementById('aud-compareciente'), d.abogadoCompareciente);
            }
        } else {
            document.getElementById('aud-materia').disabled = true;
            document.getElementById('aud-responsable').disabled = true;
            document.getElementById('aud-compareciente').disabled = true;
        }

        // Sala / Lugar
        const audSala = document.getElementById('aud-sala');
        if (audSala) audSala.value = d.sala || '';
        // Modalidad: esEnLinea/esVirtual
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
        const d = this.dataset;
        document.getElementById('term-registro-id').value = d.registroId;
        document.getElementById('term-numero').value = d.numeroExpediente || '';
        document.getElementById('term-actuacion').value = d.actuacion || '';
        document.getElementById('term-fecha').value = d.fechaVencimiento || '';
        document.getElementById('term-fecha-ingreso').value = d.fechaIngreso || '';
        document.getElementById('term-prioridad').value = d.prioridad || 'Media';
        document.getElementById('term-sede').value = d.sede || '';
        document.getElementById('term-organo').value = d.organo || '';
        document.getElementById('term-observaciones').value = d.observaciones || '';

        // Jerarquía
        const gerenciaSel = document.getElementById('term-gerencia');
        gerenciaSel.value = d.gerencia || '';
        if (gerenciaSel.value && gerenciaSel.selectedIndex >= 0) {
            const gerenciaId = gerenciaSel.options[gerenciaSel.selectedIndex].getAttribute('data-id');
            await cargarMaterias(gerenciaId, document.getElementById('term-materia'), d.materia);

            const materiaSel = document.getElementById('term-materia');
            if (materiaSel.value && materiaSel.selectedIndex >= 0) {
                const materiaId = materiaSel.options[materiaSel.selectedIndex].getAttribute('data-id');
                await cargarAbogados(materiaId, document.getElementById('term-abogado'), d.abogadoResponsable);
            }
        } else {
            document.getElementById('term-materia').disabled = true;
            document.getElementById('term-abogado').disabled = true;
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