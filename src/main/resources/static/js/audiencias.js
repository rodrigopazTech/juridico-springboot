/**
 * Lógica del Módulo de Audiencias
 * Versión v4.0 - Con Asignación Dinámica de Abogados
 */

document.addEventListener('DOMContentLoaded', function () {
    console.log("Sistema de Audiencias Cargado");
    inicializarSelectsAudiencias();
});

// --- MENÚS Y NAVEGACIÓN ---
function toggleMenu(id) {
    const menu = document.getElementById('menu-' + id);
    if (!menu) return;

    // Cerramos todos los demás
    document.querySelectorAll('[id^="menu-"]:not([id^="menu-mob-"])').forEach(m => {
        if (m !== menu) {
            m.classList.add('hidden');
            m.removeAttribute('style');
        }
    });

    // Alternamos el actual
    const isHidden = menu.classList.contains('hidden');
    if (isHidden) {
        menu.classList.remove('hidden');
        const btn = menu.parentElement.querySelector('button[onclick*="toggleMenu"]') || 
                    menu.parentElement.querySelector('[data-audiencia-id]');
        if (btn) {
            const rect = btn.getBoundingClientRect();
            const menuWidth = 256;
            const spacing = 8;
            const viewportPadding = 12;
            const menuHeight = menu.offsetHeight;
            const resultsPanel = menu.closest('#grid-resultados-audiencias') || menu.closest('#tabla-audiencias-container');
            const visibleBottom = Math.min(
                window.innerHeight - viewportPadding,
                resultsPanel ? resultsPanel.getBoundingClientRect().bottom - viewportPadding : window.innerHeight - viewportPadding
            );
            const spaceBelow = visibleBottom - rect.bottom;
            const openAbove = spaceBelow < menuHeight + spacing && rect.top > menuHeight + spacing + viewportPadding;
            const top = openAbove
                ? rect.top - menuHeight - spacing
                : Math.min(rect.bottom + spacing, visibleBottom - menuHeight);
            const left = Math.min(
                Math.max(viewportPadding, rect.right - menuWidth),
                window.innerWidth - menuWidth - viewportPadding
            );

            menu.style.position = 'fixed';
            menu.style.zIndex = '9999';
            menu.style.margin = '0';
            menu.style.top = `${Math.max(viewportPadding, top)}px`;
            menu.style.left = `${left}px`;
        }
    } else {
        menu.classList.add('hidden');
        menu.removeAttribute('style');
    }
}

// Cerrar menús al hacer clic fuera
function toggleMenuMob(id) {
    const menu = document.getElementById('menu-mob-' + id);
    if (!menu) return;

    const isHidden = menu.classList.contains('hidden');
    document.querySelectorAll('[id^="menu-mob-"]').forEach(m => m.classList.add('hidden'));
    if (isHidden) {
        menu.classList.remove('hidden');
    }
}

window.addEventListener('click', function (e) {
    if (!e.target.closest('[id^="menu-"]') && !e.target.closest('[onclick*="toggleMenu"]') && !e.target.closest('[data-audiencia-id]')) {
        document.querySelectorAll('[id^="menu-"]:not([id^="menu-mob-"])').forEach(m => {
            m.classList.add('hidden');
            m.removeAttribute('style');
        });
    }

    if (!e.target.closest('[id^="menu-mob-"]') && !e.target.closest('[onclick*="toggleMenuMob"]')) {
        document.querySelectorAll('[id^="menu-mob-"]').forEach(m => m.classList.add('hidden'));
    }
});

// Cerrar menús al hacer scroll o cambiar tamaño de pantalla para evitar desalineación
window.addEventListener('scroll', function () {
    document.querySelectorAll('[id^="menu-"]:not([id^="menu-mob-"])').forEach(m => {
        m.classList.add('hidden');
        m.removeAttribute('style');
    });
}, { passive: true });

window.addEventListener('resize', function () {
    document.querySelectorAll('[id^="menu-"]:not([id^="menu-mob-"])').forEach(m => {
        m.classList.add('hidden');
        m.removeAttribute('style');
    });
});

// --- FUNCIONES DEL MODAL (CRUD) ---

// 1. ABRIR PARA CREAR
function abrirModalAudiencia() {
    const form = document.getElementById('form-audiencia');
    if (form) form.reset();

    const inputId = document.getElementById('input-audiencia-id');
    if (inputId) inputId.value = '';

    const observacionesInput = document.getElementById('input-observaciones-aud');
    if (observacionesInput) observacionesInput.value = '';

    const titulo = document.getElementById('modal-titulo-audiencia');
    if (titulo) titulo.innerHTML = '<i class="fas fa-gavel"></i> Nueva Audiencia';

    // Mostrar el contenedor de pestañas por si acaso estaba oculto al editar
    const contenedorTabs = document.getElementById('contenedor-tabs-audiencia');
    if (contenedorTabs) contenedorTabs.classList.remove('hidden');

    // Resetear Tom Selects
    const expSelect = document.getElementById('input-expediente');
    if (expSelect && expSelect.tomselect) {
        expSelect.tomselect.setValue('');
    }
    const tipoSelect = document.getElementById('input-tipo');
    if (tipoSelect && tipoSelect.tomselect) {
        tipoSelect.tomselect.setValue('');
    }
    const salaSelect = document.getElementById('input-sala');
    if (salaSelect && salaSelect.tomselect) {
        salaSelect.tomselect.setValue('');
    }

    const gerenciaSelect = document.getElementById('aud-contexto-gerencia');
    if (gerenciaSelect && gerenciaSelect.tomselect) {
        gerenciaSelect.tomselect.setValue('');
    }
    const materiaSelect = document.getElementById('aud-contexto-materia');
    if (materiaSelect) {
        if (materiaSelect.tomselect) {
            updateTomSelectOptions(materiaSelect.tomselect, [], 'Seleccione gerencia primero...');
            materiaSelect.tomselect.setValue('');
        } else {
            materiaSelect.innerHTML = '<option value="">Seleccione gerencia primero...</option>';
        }
    }

    // Resetear selector de abogados
    const abogadoSelect = document.getElementById('input-abogado');
    if (abogadoSelect) {
        if (abogadoSelect.tomselect) {
            updateTomSelectOptions(abogadoSelect.tomselect, [], 'Seleccione expediente primero...');
            abogadoSelect.tomselect.setValue('');
        } else {
            abogadoSelect.innerHTML = '<option value="">Seleccione expediente primero...</option>';
            abogadoSelect.disabled = false;
        }
    }

    toggleUbicacion(false);

    // Resetear radio buttons
    const radioPresencial = document.querySelector('input[name="esVirtual"][value="false"]');
    if (radioPresencial) radioPresencial.checked = true;

    // Inicializar en modo EXPEDIENTE por defecto
    switchModoRegistro('EXPEDIENTE');

    document.getElementById('modal-audiencia').classList.remove('hidden');
}

function abrirModalEditar(id, expedienteId, tipoId, fecha, hora, isVirtual, sala, link, abogadoId, observaciones = "", origenAudiencia = "EXPEDIENTE", contextoGerenciaId = "", contextoMateriaId = "", contextoNumeroReferencia = "", contextoSede = "", contextoPartes = "", contextoDescripcion = "") {

    if (origenAudiencia === 'EXPEDIENTE') {
        window.editandoAudienciaAbogadoId = abogadoId;
        window.editandoAudienciaMateriaId = null;
    } else {
        window.editandoAudienciaAbogadoId = abogadoId;
        window.editandoAudienciaMateriaId = contextoMateriaId;
    }

    // A. Llenar campos básicos
    setValueIfExists('input-audiencia-id', id);
    
    // Configurar origen (pestaña y modo)
    switchModoRegistro(origenAudiencia);

    // Ocultar pestañas al editar para evitar que se cambie el origen de una audiencia existente
    const contenedorTabs = document.getElementById('contenedor-tabs-audiencia');
    if (contenedorTabs) {
        contenedorTabs.classList.add('hidden');
    }

    if (origenAudiencia === 'EXPEDIENTE') {
        // En expediente, el seteo del valor disparará el onChange que cargará los abogados
    } else {
        // Llenar campos de Asunto Único
        const gerenciaSelect = document.getElementById('aud-contexto-gerencia');
        if (gerenciaSelect) {
            if (gerenciaSelect.tomselect) {
                const valorActual = gerenciaSelect.tomselect.getValue();
                if (String(valorActual) === String(contextoGerenciaId)) {
                    // Si ya tiene el mismo valor, onChange no se disparará. Llamamos directo.
                    cargarAbogadosAsuntoUnico(abogadoId);
                    cargarMateriasAsuntoUnico(contextoMateriaId);
                    window.editandoAudienciaAbogadoId = null;
                    window.editandoAudienciaMateriaId = null;
                } else {
                    gerenciaSelect.tomselect.setValue(contextoGerenciaId);
                }
            } else {
                gerenciaSelect.value = contextoGerenciaId;
            }
        }

        const materiaSelect = document.getElementById('aud-contexto-materia');
        if (materiaSelect) {
            if (materiaSelect.tomselect) {
                materiaSelect.tomselect.setValue(contextoMateriaId);
            } else {
                materiaSelect.value = contextoMateriaId;
            }
        }

        setValueIfExists('aud-contexto-referencia', contextoNumeroReferencia);


        setValueIfExists('aud-contexto-partes', contextoPartes);
        setValueIfExists('aud-contexto-descripcion', contextoDescripcion);
    }

    const tipoSelect = document.getElementById('input-tipo');
    if (tipoSelect && tipoSelect.tomselect) {
        tipoSelect.tomselect.setValue(tipoId);
    } else {
        setValueIfExists('input-tipo', tipoId);
    }

    setValueIfExists('input-fecha', fecha);
    setValueIfExists('input-hora', hora);
    setValueIfExists('input-observaciones-aud', observaciones);

    // B. Configurar Modalidad (Virtual/Presencial)
    const esVirtualBool = (String(isVirtual) === 'true');

    const radio = document.querySelector(`input[name="esVirtual"][value="${esVirtualBool}"]`);
    if (radio) radio.checked = true;

    toggleUbicacion(esVirtualBool);

    if (esVirtualBool) {
        setValueIfExists('input-url', link);
    } else {
        const salaSelect = document.getElementById('input-sala');
        if (salaSelect && salaSelect.tomselect) {
            salaSelect.tomselect.setValue(sala);
        } else {
            setValueIfExists('input-sala', sala);
        }
    }

    // C. CARGAR Y SELECCIONAR ABOGADO
    if (origenAudiencia === 'EXPEDIENTE') {
        if (expedienteId) {
            window.editandoAudienciaAbogadoId = abogadoId;
            const expSelect = document.getElementById('input-expediente');
            if (expSelect && expSelect.tomselect) {
                const valorActual = expSelect.tomselect.getValue();
                if (String(valorActual) === String(expedienteId)) {
                    // Si ya es el mismo valor, onChange no se disparará. Llamamos directo.
                    cargarAbogadosPosibles(abogadoId);
                    window.editandoAudienciaAbogadoId = null;
                } else {
                    expSelect.tomselect.setValue(expedienteId);
                }
            } else {
                setValueIfExists('input-expediente', expedienteId);
                cargarAbogadosPosibles(abogadoId);
            }
        }
    } else {
        // En Asunto Único, cargar abogados y materias si aún no se han cargado (por ejemplo, si la gerencia es fija/input hidden)
        if (window.editandoAudienciaAbogadoId !== null) {
            cargarAbogadosAsuntoUnico(window.editandoAudienciaAbogadoId);
            window.editandoAudienciaAbogadoId = null;
        }
        if (window.editandoAudienciaMateriaId !== null) {
            cargarMateriasAsuntoUnico(window.editandoAudienciaMateriaId);
            window.editandoAudienciaMateriaId = null;
        }
    }

    // D. Cambiar Título y Mostrar
    const titulo = document.getElementById('modal-titulo-audiencia');
    if (titulo) titulo.innerHTML = '<i class="fas fa-edit"></i> Editar Audiencia';

    document.getElementById('modal-audiencia').classList.remove('hidden');
}

// Helper para evitar errores si el input no existe
function setValueIfExists(elementId, value) {
    const el = document.getElementById(elementId);
    if (el) el.value = value || '';
}

// --- UTILIDADES ---
function toggleUbicacion(isVirtual) {
    const campoSala = document.getElementById('campo-sala');
    const campoUrl = document.getElementById('campo-url');
    const inputSala = document.getElementById('input-sala');
    const inputUrl = document.getElementById('input-url');

    // Convertir a booleano por seguridad
    const showVirtual = (String(isVirtual) === 'true');

    if (showVirtual) {
        if (campoSala) campoSala.classList.add('hidden');
        if (campoUrl) campoUrl.classList.remove('hidden');
        if (inputUrl) inputUrl.setAttribute('required', 'required');
        if (inputSala) {
            inputSala.removeAttribute('required');
            inputSala.value = ''; // Limpiar Sala si es Virtual
        }
    } else {
        if (campoSala) campoSala.classList.remove('hidden');
        if (campoUrl) campoUrl.classList.add('hidden');
        if (inputSala) inputSala.setAttribute('required', 'required');
        if (inputUrl) {
            inputUrl.removeAttribute('required');
            inputUrl.value = ''; // Limpiar URL si es Presencial
        }
    }
}

// Alternar entre modo Expediente y modo Asunto Único en el formulario de Registro/Edición
function switchModoRegistro(modo) {
    const tabExpediente = document.getElementById('tab-expediente');
    const tabAsuntoUnico = document.getElementById('tab-asunto-unico');
    const seccionExpediente = document.getElementById('seccion-expediente');
    const seccionAsuntoUnico = document.getElementById('seccion-asunto-unico');
    const inputOrigen = document.getElementById('input-origen-audiencia');
    const formAudiencia = document.getElementById('form-audiencia');

    const expSelect = document.getElementById('input-expediente');
    const contextoGerencia = document.getElementById('aud-contexto-gerencia');
    const contextoMateria = document.getElementById('aud-contexto-materia');
    const contextoReferencia = document.getElementById('aud-contexto-referencia');
    
    const abogadoSelect = document.getElementById('input-abogado');

    if (!tabExpediente || !tabAsuntoUnico || !seccionExpediente || !seccionAsuntoUnico) return;

    if (modo === 'EXPEDIENTE') {
        // Estilo de pestañas
        tabExpediente.className = "flex-1 py-3 px-4 text-center font-bold border-b-2 transition-all border-gob-guinda text-gob-guinda bg-gob-guinda/5";
        tabAsuntoUnico.className = "flex-1 py-3 px-4 text-center font-bold border-b-2 transition-all border-transparent text-gray-400 hover:text-gray-600 hover:bg-gray-50/50";

        // Secciones visibles
        seccionExpediente.classList.remove('hidden');
        seccionAsuntoUnico.classList.add('hidden');

        // Origen y Action
        if (inputOrigen) inputOrigen.value = 'EXPEDIENTE';
        if (formAudiencia) formAudiencia.action = '/audiencias/guardar';

        // Requeridos
        if (expSelect) {
            expSelect.setAttribute('required', 'required');
        }
        if (contextoGerencia) {
            contextoGerencia.removeAttribute('required');
            if (contextoGerencia.type !== 'hidden') {
                if (contextoGerencia.tomselect) {
                    contextoGerencia.tomselect.setValue('', true);
                } else {
                    contextoGerencia.value = '';
                }
            }
        }
        if (contextoMateria) {
            contextoMateria.removeAttribute('required');
            if (contextoMateria.tomselect) {
                contextoMateria.tomselect.setValue('');
            } else {
                contextoMateria.value = '';
            }
        }
        if (contextoReferencia) {
            contextoReferencia.removeAttribute('required');
            contextoReferencia.value = '';
        }


        setValueIfExists('aud-contexto-partes', '');
        setValueIfExists('aud-contexto-descripcion', '');

        // Ocultar abogados de Asunto Único y cargar los del expediente
        if (abogadoSelect) {
            if (abogadoSelect.tomselect) {
                abogadoSelect.tomselect.setValue('');
            } else {
                abogadoSelect.querySelectorAll('.abogado-option-all').forEach(opt => opt.classList.add('hidden'));
            }
        }
        cargarAbogadosPosibles();

    } else if (modo === 'ASUNTO_UNICO') {
        // Estilo de pestañas
        tabAsuntoUnico.className = "flex-1 py-3 px-4 text-center font-bold border-b-2 transition-all border-gob-guinda text-gob-guinda bg-gob-guinda/5";
        tabExpediente.className = "flex-1 py-3 px-4 text-center font-bold border-b-2 transition-all border-transparent text-gray-400 hover:text-gray-600 hover:bg-gray-50/50";

        // Secciones visibles
        seccionAsuntoUnico.classList.remove('hidden');
        seccionExpediente.classList.add('hidden');

        // Origen y Action
        if (inputOrigen) inputOrigen.value = 'ASUNTO_UNICO';
        if (formAudiencia) formAudiencia.action = '/audiencias/guardar-asunto-unico';

        // Requeridos
        if (expSelect) {
            expSelect.removeAttribute('required');
            if (expSelect.tomselect) {
                expSelect.tomselect.setValue('');
            } else {
                expSelect.value = '';
            }
        }
        if (contextoGerencia) contextoGerencia.setAttribute('required', 'required');
        if (contextoMateria) contextoMateria.setAttribute('required', 'required');
        if (contextoReferencia) contextoReferencia.setAttribute('required', 'required');

        // Limpiar selector de sala (Sede de Expediente)
        const salaSelect = document.getElementById('input-sala');
        if (salaSelect && salaSelect.tomselect) {
            salaSelect.tomselect.setValue('');
        } else {
            setValueIfExists('input-sala', '');
        }

        // Cargar los abogados y materias de la gerencia seleccionada en Asunto Único
        if (!window.editandoAudienciaAbogadoId) {
            cargarAbogadosAsuntoUnico();
        }
        if (!window.editandoAudienciaMateriaId) {
            cargarMateriasAsuntoUnico();
        }
    }
}

function activarSubidaActa(id) {
    const inputId = document.getElementById('input-acta-id');
    const inputFile = document.getElementById('input-acta-file');
    if (inputId && inputFile) {
        inputId.value = id;
        inputFile.click();
    }
}

// ROD-186: Configuración de Notificaciones Estilo Proyecto
const Toast = Swal.mixin({
    toast: true,
    position: 'bottom-end',
    showConfirmButton: false,
    timer: 3000,
    //timerProgressBar: true,
    didOpen: (toast) => {
        //toast.addEventListener('mouseenter', Swal.stopTimer)
        toast.addEventListener('mouseleave', Swal.resumeTimer)
    }
});

function showSuccessToast(mensaje) {
    Toast.fire({
        icon: 'success',
        title: mensaje,
        iconColor: '#22c55e',
        background: '#f0fdf4',
        padding: '1em',
        customClass: {
            title: 'text-green-800 text-sm font-bold'
        }
    });
}

// ROD-186: Filtrar por estatus desde las tarjetas KPI
function filtrarPorEstatus(estatus) {
    const formFiltros = document.getElementById('form-filtros-aud');
    if (!formFiltros) return;

    const selectEstatus = formFiltros.querySelector('select[name="estatus"]');
    if (selectEstatus) {
        selectEstatus.value = estatus;
        actualizarTablaAsincrono();
    }
}

function showErrorModal(mensaje) {
    Swal.fire({
        title: 'Atención',
        text: mensaje,
        icon: 'error',
        confirmButtonText: 'Entendido',
        confirmButtonColor: '#bc955c',
        background: '#f9fafb',
        customClass: { confirmButton: 'font-bold' }
    });
}

// ROD-186: Listener para subida de acta y conclusión vía AJAX (Preserva filtros)
document.addEventListener('submit', function (e) {
    const formAudiencia = e.target.closest('#form-audiencia');
    // A. Concluir Audiencia
    const formConcluir = e.target.closest('#form-concluir');
    if (formConcluir) {
        e.preventDefault();
        const formData = new FormData(formConcluir);

        Swal.fire({
            title: 'Procesando...',
            allowOutsideClick: false,
            didOpen: () => { Swal.showLoading(); }
        });

        fetch(formConcluir.action, {
            method: 'POST',
            body: formData,
            headers: { 'X-Requested-With': 'XMLHttpRequest' }
        })
            .then(response => {
                if (response.ok) {
                    document.getElementById('modal-concluir').classList.add('hidden');
                    showSuccessToast('¡Audiencia Concluida correctamente!');
                    actualizarTablaAsincrono();
                } else {
                    throw new Error('Error al concluir');
                }
            })
            .catch(error => {
                showErrorModal('No se pudo completar la acción.');
            });
    }

    // B. Guardar/Editar Audiencia
    if (formAudiencia) {
        e.preventDefault();
        const formData = new FormData(formAudiencia);

        Swal.fire({
            title: 'Guardando...',
            allowOutsideClick: false,
            didOpen: () => { Swal.showLoading(); }
        });

        fetch(formAudiencia.action, {
            method: 'POST',
            body: formData,
            headers: { 'X-Requested-With': 'XMLHttpRequest' }
        })
            .then(response => {
                if (response.ok) {
                    document.getElementById('modal-audiencia').classList.add('hidden');
                    showSuccessToast('¡Datos guardados correctamente!');
                    actualizarTablaAsincrono();
                } else {
                    throw new Error('Error al guardar');
                }
            })
            .catch(error => {
                showErrorModal('No se pudo guardar la audiencia.');
            });
    }
});

document.addEventListener('change', function (e) {
    if (e.target && e.target.id === 'input-acta-file') {
        const form = document.getElementById('form-subir-acta');
        if (!form) return;

        const formData = new FormData(form);

        // Mostrar indicador de carga
        Swal.fire({
            title: 'Subiendo acta...',
            allowOutsideClick: false,
            didOpen: () => { Swal.showLoading(); }
        });

        fetch(form.action, {
            method: 'POST',
            body: formData,
            headers: { 'X-Requested-With': 'XMLHttpRequest' }
        })
            .then(response => {
                if (response.ok) {
                    showSuccessToast('¡Acta subida correctamente!');
                    actualizarTablaAsincrono();
                } else {
                    throw new Error('Error en la subida');
                }
            })
            .catch(error => {
                showErrorModal('No se pudo subir el archivo. Intente de nuevo.');
            });
    }
});

function abrirModalConcluir(id) {
    const inputId = document.getElementById('concluir-audiencia-id');
    if (inputId) inputId.value = id;

    document.getElementById('modal-concluir').classList.remove('hidden');
}

// ==========================================
// ⚖️ LÓGICA DE ASIGNACIÓN DE ABOGADOS
// ==========================================

function cargarAbogadosPosibles(idPreseleccionado = null) {
    const expSelect = document.getElementById('input-expediente');
    const abogadoSelect = document.getElementById('input-abogado');

    if (!expSelect || !abogadoSelect) return;

    const expId = expSelect.tomselect ? expSelect.tomselect.getValue() : expSelect.value;

    // Obtener gerenciaId: primero del DOM, si no del mapa de Tom Select (opciones virtualizadas)
    let gerenciaId = null;
    const optElDOM = expSelect.querySelector(`option[value="${expId}"]`);
    if (optElDOM) {
        gerenciaId = optElDOM.getAttribute('data-gerencia-id');
    } else if (expSelect.tomselect && expId && expSelect.tomselect.options[expId]) {
        gerenciaId = expSelect.tomselect.options[expId].gerenciaId || null;
    }

    // Filtrar Tipos de Audiencia por Gerencia (preservar opciones creadas inline)
    const tipoSelect = document.getElementById('input-tipo');
    if (tipoSelect) {
        const baseFiltered = (window.allTiposAudiencia || []).filter(t => !gerenciaId || !t.gerenciaId || t.gerenciaId === gerenciaId);
        if (tipoSelect.tomselect) {
            // Preservar opciones inline (creadas por el usuario, no están en allTiposAudiencia)
            const originalValues = new Set((window.allTiposAudiencia || []).map(t => String(t.value)));
            const inlineOpts = Object.values(tipoSelect.tomselect.options)
                .filter(o => o.value && !originalValues.has(String(o.value)))
                .map(o => ({ value: o.value, text: o.text, gerenciaId: '' }));
            updateTomSelectOptions(tipoSelect.tomselect, [...baseFiltered, ...inlineOpts], 'Seleccione tipo de audiencia...');
        } else {
            Array.from(tipoSelect.options).forEach(opt => {
                if (opt.value === "") return;
                const optGerenciaId = opt.getAttribute('data-gerencia-id');
                if (!gerenciaId) {
                    opt.style.display = 'none';
                    opt.hidden = true;
                } else if (!optGerenciaId || optGerenciaId === gerenciaId) {
                    opt.style.display = '';
                    opt.hidden = false;
                } else {
                    opt.style.display = 'none';
                    opt.hidden = true;
                }
            });
            const currentTipo = tipoSelect.options[tipoSelect.selectedIndex];
            if (currentTipo && currentTipo.hidden) {
                tipoSelect.value = '';
            }
        }
    }

    // input-sala es ahora un <input> con datalist nativo filtrado desde el servidor; no se filtra en cliente.


    if (!expId) {
        if (abogadoSelect.tomselect) {
            updateTomSelectOptions(abogadoSelect.tomselect, [], 'Seleccione expediente primero...');
        } else {
            abogadoSelect.innerHTML = '<option value="">Seleccione expediente primero...</option>';
        }
        // Limpiar delegados también
        const delegadoSelect = document.getElementById('input-delegado');
        const delegadoHidden = document.getElementById('input-delegado-hidden');
        if (delegadoSelect) delegadoSelect.innerHTML = '<option value="">Sin delegación</option>';
        if (delegadoHidden) delegadoHidden.value = '';
        return;
    }

    if (abogadoSelect.tomselect) {
        updateTomSelectOptions(abogadoSelect.tomselect, [], 'Buscando abogados disponibles...');
    } else {
        abogadoSelect.innerHTML = '<option>Buscando abogados disponibles...</option>';
    }

    fetch(`/audiencias/api/abogados-disponibles/${expId}`)
        .then(response => response.json())
        .then(abogados => {
            if (abogadoSelect.tomselect) {
                const options = abogados.map(a => {
                    return {
                        value: a.id,
                        text: a.esTitular ? `⭐ ${a.nombre} (Titular)` : a.nombre
                    };
                });
                updateTomSelectOptions(abogadoSelect.tomselect, options, '-- Seleccione quien asistirá (Opcional) --');
                
                const titular = abogados.find(a => a.esTitular);
                if (idPreseleccionado) {
                    abogadoSelect.tomselect.setValue(idPreseleccionado);
                } else if (titular) {
                    abogadoSelect.tomselect.setValue(titular.id);
                } else {
                    abogadoSelect.tomselect.setValue('');
                }
            } else {
                abogadoSelect.innerHTML = '';

                if (abogados.length === 0) {
                    abogadoSelect.innerHTML = '<option value="">No hay abogados en esta gerencia</option>';
                    return;
                }

                const defaultOption = document.createElement('option');
                defaultOption.value = "";
                defaultOption.text = "-- Seleccione quien asistirá (Opcional) --";
                abogadoSelect.appendChild(defaultOption);

                abogados.forEach(a => {
                    const option = document.createElement('option');
                    option.value = a.id;

                    if (a.esTitular) {
                        option.text = `⭐ ${a.nombre} (Titular)`;
                        option.style.fontWeight = 'bold';
                        if (!idPreseleccionado) {
                            option.selected = true;
                        }
                    } else {
                        option.text = a.nombre;
                    }

                    if (idPreseleccionado && String(a.id) === String(idPreseleccionado)) {
                        option.selected = true;
                    }

                    abogadoSelect.appendChild(option);
                });
            }

            // Llenar selector de delegados (misma lista, sin el titular)
            const delegadoSelect = document.getElementById('input-delegado');
            const delegadoHidden = document.getElementById('input-delegado-hidden');
            if (delegadoSelect) {
                delegadoSelect.innerHTML = '<option value="">Sin delegación</option>';
                abogados
                    .filter(a => !a.esTitular)
                    .forEach(a => {
                        const opt = document.createElement('option');
                        opt.value = a.id;
                        opt.text = a.nombre;
                        delegadoSelect.appendChild(opt);
                    });
                if (delegadoHidden) delegadoHidden.value = '';
            }
        })
        .catch(err => {
            console.error(err);
            if (abogadoSelect.tomselect) {
                updateTomSelectOptions(abogadoSelect.tomselect, [], 'Error al cargar abogados');
            } else {
                abogadoSelect.innerHTML = '<option value="">Error al cargar abogados</option>';
            }
        });
}

function cargarAbogadosAsuntoUnico(idPreseleccionado = null) {
    const gerenciaEl = document.getElementById('aud-contexto-gerencia');
    const abogadoSelect = document.getElementById('input-abogado');
    if (!gerenciaEl || !abogadoSelect) return;

    const gerenciaId = gerenciaEl.tomselect ? gerenciaEl.tomselect.getValue() : gerenciaEl.value;

    if (!gerenciaId) {
        if (abogadoSelect.tomselect) {
            updateTomSelectOptions(abogadoSelect.tomselect, [], 'Seleccione gerencia primero...');
        } else {
            abogadoSelect.innerHTML = '<option value="">Seleccione gerencia primero...</option>';
        }
        return;
    }

    if (abogadoSelect.tomselect) {
        updateTomSelectOptions(abogadoSelect.tomselect, [], 'Buscando abogados disponibles...');
    } else {
        abogadoSelect.innerHTML = '<option>Buscando abogados disponibles...</option>';
    }

    fetch(`/audiencias/api/abogados-por-gerencia/${gerenciaId}`)
        .then(response => response.json())
        .then(abogados => {
            const options = abogados.map(a => ({
                value: a.id,
                text: a.nombre
            }));
            
            if (abogadoSelect.tomselect) {
                updateTomSelectOptions(abogadoSelect.tomselect, options, '-- Seleccione quien asistirá (Opcional) --');
                if (idPreseleccionado) {
                    abogadoSelect.tomselect.setValue(idPreseleccionado);
                } else {
                    abogadoSelect.tomselect.setValue('');
                }
            } else {
                abogadoSelect.innerHTML = '<option value="">-- Seleccione quien asistirá (Opcional) --</option>';
                options.forEach(opt => {
                    const o = document.createElement('option');
                    o.value = opt.value;
                    o.text = opt.text;
                    if (idPreseleccionado && String(opt.value) === String(idPreseleccionado)) {
                        o.selected = true;
                    }
                    abogadoSelect.appendChild(o);
                });
            }
        })
        .catch(err => {
            console.error(err);
            if (abogadoSelect.tomselect) {
                updateTomSelectOptions(abogadoSelect.tomselect, [], 'Error al cargar abogados');
            } else {
                abogadoSelect.innerHTML = '<option value="">Error al cargar abogados</option>';
            }
        });
}

function cargarMateriasAsuntoUnico(idPreseleccionado = null) {
    const gerenciaEl = document.getElementById('aud-contexto-gerencia');
    const materiaSelect = document.getElementById('aud-contexto-materia');
    if (!gerenciaEl || !materiaSelect) return;

    const gerenciaId = gerenciaEl.tomselect ? gerenciaEl.tomselect.getValue() : gerenciaEl.value;

    if (!gerenciaId) {
        if (materiaSelect.tomselect) {
            updateTomSelectOptions(materiaSelect.tomselect, [], 'Seleccione gerencia primero...');
            materiaSelect.tomselect.setValue('');
        } else {
            materiaSelect.innerHTML = '<option value="">Seleccione gerencia primero...</option>';
        }
        return;
    }

    if (materiaSelect.tomselect) {
        updateTomSelectOptions(materiaSelect.tomselect, [], 'Buscando materias disponibles...');
    } else {
        materiaSelect.innerHTML = '<option>Buscando materias disponibles...</option>';
    }

    fetch(`/audiencias/api/materias-por-gerencia/${gerenciaId}`)
        .then(response => response.json())
        .then(materias => {
            const options = materias.map(m => ({
                value: m.id,
                text: m.nombre
            }));
            
            if (materiaSelect.tomselect) {
                updateTomSelectOptions(materiaSelect.tomselect, options, '-- Seleccione Materia --');
                if (idPreseleccionado) {
                    materiaSelect.tomselect.setValue(idPreseleccionado);
                } else {
                    materiaSelect.tomselect.setValue('');
                }
            } else {
                materiaSelect.innerHTML = '<option value="">-- Seleccione Materia --</option>';
                options.forEach(opt => {
                    const o = document.createElement('option');
                    o.value = opt.value;
                    o.text = opt.text;
                    if (idPreseleccionado && String(opt.value) === String(idPreseleccionado)) {
                        o.selected = true;
                    }
                    materiaSelect.appendChild(o);
                });
            }
        })
        .catch(err => {
            console.error(err);
            if (materiaSelect.tomselect) {
                updateTomSelectOptions(materiaSelect.tomselect, [], 'Error al cargar materias');
            } else {
                materiaSelect.innerHTML = '<option value="">Error al cargar materias</option>';
            }
        });
}

function inicializarSelectsAudiencias() {
    const tipoSelect = document.getElementById('input-tipo');
    if (tipoSelect) {
        window.allTiposAudiencia = Array.from(tipoSelect.options)
            .filter(o => o.value !== "")
            .map(o => ({ value: o.value, text: o.text, gerenciaId: o.getAttribute('data-gerencia-id') }));
    }
    window.allSalas = [];

    // Capturar todos los abogados ANTES de que Tom Select los reescriba
    const selectAbogado = document.getElementById('input-abogado');
    if (selectAbogado) {
        window.allAbogados = Array.from(selectAbogado.options)
            .filter(o => o.value !== "")
            .map(o => ({ value: o.value, text: o.text }));
    }

    const selectExpediente = document.getElementById('input-expediente');
    const selectTipo = document.getElementById('input-tipo');
    const selectSala = document.getElementById('input-sala');

    if (selectExpediente && !selectExpediente.tomselect) {
        // Leer opciones con data-gerencia-id ANTES de que Tom Select las virtualice
        const expedienteOptions = Array.from(selectExpediente.options).map(o => ({
            value: o.value,
            text: o.text,
            gerenciaId: o.getAttribute('data-gerencia-id') || ''
        }));
        initSearchableSelect(selectExpediente, { 
            tipo: 'expediente',
            onChange: function() {
                if (window.editandoAudienciaAbogadoId) {
                    cargarAbogadosPosibles(window.editandoAudienciaAbogadoId);
                    window.editandoAudienciaAbogadoId = null;
                } else {
                    cargarAbogadosPosibles();
                }
            }
        });
        // Inyectar gerenciaId en el mapa de opciones de Tom Select para que custom-selects pueda leerlo
        if (selectExpediente.tomselect) {
            expedienteOptions.forEach(opt => {
                const tsOpt = selectExpediente.tomselect.options[opt.value];
                if (tsOpt && opt.gerenciaId) {
                    tsOpt.gerenciaId = opt.gerenciaId;
                }
            });
        }
    }

    if (selectTipo && !selectTipo.tomselect) {
        initSearchableSelect(selectTipo, { 
            tipo: 'tipo-audiencia',
            createInline: true,
            gerenciaSelector: '#input-expediente'
        });
    }

    // Inicializar Tom Select para Sedes (sala y ubicación)
    if (selectSala && !selectSala.tomselect) {
        initSearchableSelect(selectSala, { tipo: 'sede', createInline: true, useTextAsValue: true });
    }


    // Inicializar selector de gerencia en Asunto Único
    const selectGerencia = document.getElementById('aud-contexto-gerencia');
    if (selectGerencia) {
        if (!selectGerencia.tomselect && selectGerencia.tagName === 'SELECT') {
            initSearchableSelect(selectGerencia, { 
                tipo: 'gerencia',
                onChange: function() {
                    if (document.getElementById('input-origen-audiencia').value === 'ASUNTO_UNICO') {
                        if (window.editandoAudienciaAbogadoId) {
                            cargarAbogadosAsuntoUnico(window.editandoAudienciaAbogadoId);
                            window.editandoAudienciaAbogadoId = null;
                        } else {
                            cargarAbogadosAsuntoUnico();
                        }
                        if (window.editandoAudienciaMateriaId) {
                            cargarMateriasAsuntoUnico(window.editandoAudienciaMateriaId);
                            window.editandoAudienciaMateriaId = null;
                        } else {
                            cargarMateriasAsuntoUnico();
                        }
                    }
                }
            });
        } else {
            selectGerencia.addEventListener('change', function() {
                if (document.getElementById('input-origen-audiencia').value === 'ASUNTO_UNICO') {
                    if (window.editandoAudienciaAbogadoId) {
                        cargarAbogadosAsuntoUnico(window.editandoAudienciaAbogadoId);
                        window.editandoAudienciaAbogadoId = null;
                    } else {
                        cargarAbogadosAsuntoUnico();
                    }
                    if (window.editandoAudienciaMateriaId) {
                        cargarMateriasAsuntoUnico(window.editandoAudienciaMateriaId);
                        window.editandoAudienciaMateriaId = null;
                    } else {
                        cargarMateriasAsuntoUnico();
                    }
                }
            });
        }
    }

    // Inicializar selector de materia en Asunto Único
    const selectMateria = document.getElementById('aud-contexto-materia');
    if (selectMateria && !selectMateria.tomselect && selectMateria.tagName === 'SELECT') {
        initSearchableSelect(selectMateria, { tipo: 'materia' });
    }

    if (selectAbogado && !selectAbogado.tomselect) {
        initSearchableSelect(selectAbogado, { tipo: 'abogado' });
    }
}

// 4. CONFIRMAR ELIMINAR AUDIENCIA
function confirmarEliminarAudiencia(id) {
    Swal.fire({
        title: '¿Eliminar audiencia?',
        text: "Esta acción no se puede deshacer. Se notificará a tus superiores.",
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#d33',
        cancelButtonColor: '#3085d6',
        confirmButtonText: 'Sí, eliminar',
        cancelButtonText: 'Cancelar'
    }).then((result) => {
        if (result.isConfirmed) {
            ejecutarEliminacionAudiencia(id);
        }
    });
}

function ejecutarEliminacionAudiencia(id) {
            fetch('/audiencias/eliminar/' + id, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                }
            })
                .then(response => response.json())
                .then(data => {
                    if (data.success) {
                        // Éxito: Usamos un toast
                        const Toast = Swal.mixin({
                            toast: true,
                            position: 'bottom-end',
                            showConfirmButton: false,
                            timer: 2000,
                            //timerProgressBar: true
                        });

                        Toast.fire({
                            icon: 'success',
                            title: 'Audiencia eliminada',
                            iconColor: '#22c55e',
                            background: '#f0fdf4',
                            customClass: {
                                title: 'text-green-800 text-sm font-bold'
                            }
                        }).then(() => {
                            // ROD-186: En lugar de reload, usamos actualización asíncrona para preservar filtros
                            actualizarTablaAsincrono();
                        });
                    } else {
                        Swal.fire('Error', data.message || 'Error al eliminar la audiencia.', 'error');
                    }
                })
                .catch(error => {
                    Swal.fire('Error', 'Hubo un problema de conexión.', 'error');
                });
}

// ==========================================
// 📅 LÓGICA DEL MINI-CALENDARIO
// ==========================================

const miniCalendarState = {
    currentDate: new Date(),
    selectedDate: new Date(),
    isDateFilterActive: false, // Nueva bandera para saber si filtrar por fecha o ver todo (ROD-185)
    names: ["Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"],
    events: [],
    view: 'month'
};

function fetchMiniCalendarEvents() {
    const urlParams = new URLSearchParams();
    urlParams.set('tipo', 'audiencia'); // Filtrar solo por eventos tipo "audiencia"

    // Obtener filtros del Sidebar de Audiencias
    const formFiltros = document.getElementById('form-filtros-aud');
    if (formFiltros) {
        const fd = new FormData(formFiltros);
        const mapping = {
            'tipo': 'tipoAudiencia', // Mapear el filtro de subtipo 'tipo' a 'tipoAudiencia' para evitar conflicto con el tipo de evento
            'estatus': 'estatus',
            'abogadoId': 'usuario',
            'gerencia': 'gerencia',
            'materia': 'materia'
        };
        for (const [formKey, apiKey] of Object.entries(mapping)) {
            const val = fd.get(formKey);
            if (val && val !== 'null' && val !== '') {
                urlParams.set(apiKey, val);
            }
        }
    }

    return fetch('/api/calendario/eventos?' + urlParams.toString(), {
        headers: { 'X-Requested-With': 'XMLHttpRequest' }
    })
        .then(res => res.json())
        .then(events => {
            miniCalendarState.events = events || [];
        })
        .catch(err => {
            console.error("Error al cargar eventos del mini calendario:", err);
            miniCalendarState.events = [];
        });
}

function initMiniCalendar() {
    const prevBtn = document.getElementById('miniPrev');
    const nextBtn = document.getElementById('miniNext');

    // Sincronizar con el estado del servidor
    const hdnMes = document.getElementById('hdnMesActual');
    const hdnAnio = document.getElementById('hdnAnioActual');
    if (hdnMes && hdnAnio) {
        miniCalendarState.currentDate = new Date(parseInt(hdnAnio.value), parseInt(hdnMes.value) - 1, 1);
    }

    // Determinar si hay un filtro de fecha activo en la URL inicial
    const urlParams = new URLSearchParams(window.location.search);
    const fechaExacta = urlParams.get('fecha');
    if (urlParams.has('mes') || urlParams.has('anio') || fechaExacta || urlParams.get('keyword')?.match(/^\d{4}-\d{2}-\d{2}$/)) {
        miniCalendarState.isDateFilterActive = true;
    }

    // Si hay fecha exacta o keyword de fecha, marcarla como seleccionada
    const keyword = urlParams.get('keyword');
    const fechaSeleccionada = fechaExacta || (keyword && keyword.match(/^\d{4}-\d{2}-\d{2}$/) ? keyword : null);
    if (fechaSeleccionada) {
        const parts = fechaSeleccionada.split('-');
        miniCalendarState.selectedDate = new Date(parseInt(parts[0]), parseInt(parts[1]) - 1, parseInt(parts[2]));
        miniCalendarState.currentDate = new Date(parseInt(parts[0]), parseInt(parts[1]) - 1, 1);
    }

    const viewMonthBtn = document.getElementById('miniViewMonth');
    const viewWeekBtn = document.getElementById('miniViewWeek');
    if (viewMonthBtn && viewWeekBtn) {
        viewMonthBtn.onclick = () => {
            miniCalendarState.view = 'month';
            renderMiniCalendar();
        };
        viewWeekBtn.onclick = () => {
            miniCalendarState.view = 'week';
            renderMiniCalendar();
        };
    }

    if (prevBtn && nextBtn) {
        prevBtn.onclick = () => {
            if (miniCalendarState.view === 'week') {
                const date = (miniCalendarState.selectedDate && miniCalendarState.selectedDate.getTime() > 0) ? miniCalendarState.selectedDate : new Date(miniCalendarState.currentDate);
                date.setDate(date.getDate() - 7);
                miniCalendarState.selectedDate = new Date(date);
                miniCalendarState.currentDate = new Date(date);
                const dateStr = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
                
                const searchInput = document.getElementById('input-search-audiencias');
                if (searchInput) searchInput.value = '';
                miniCalendarState.isDateFilterActive = true;
                actualizarTablaAsincrono(dateStr);
            } else {
                const date = miniCalendarState.currentDate;
                date.setMonth(date.getMonth() - 1);
                const searchInput = document.getElementById('input-search-audiencias');
                if (searchInput) searchInput.value = '';
                miniCalendarState.isDateFilterActive = true; // Activar filtro al navegar meses
                actualizarTablaAsincrono();
            }
        };
        nextBtn.onclick = () => {
            if (miniCalendarState.view === 'week') {
                const date = (miniCalendarState.selectedDate && miniCalendarState.selectedDate.getTime() > 0) ? miniCalendarState.selectedDate : new Date(miniCalendarState.currentDate);
                date.setDate(date.getDate() + 7);
                miniCalendarState.selectedDate = new Date(date);
                miniCalendarState.currentDate = new Date(date);
                const dateStr = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
                
                const searchInput = document.getElementById('input-search-audiencias');
                if (searchInput) searchInput.value = '';
                miniCalendarState.isDateFilterActive = true;
                actualizarTablaAsincrono(dateStr);
            } else {
                const date = miniCalendarState.currentDate;
                date.setMonth(date.getMonth() + 1);
                const searchInput = document.getElementById('input-search-audiencias');
                if (searchInput) searchInput.value = '';
                miniCalendarState.isDateFilterActive = true; // Activar filtro al navegar meses
                actualizarTablaAsincrono();
            }
        };
    }

    fetchMiniCalendarEvents().then(() => {
        renderMiniCalendar();
    });
}

/**
 * Función centralizada para disparar la actualización de la tabla 
 * capturando TODOS los parámetros (Buscador, Mini-Calendario, Filtros Side)
 */
// --- BÚSQUEDA ASÍNCRONA CENTRALIZADA ---
let debounceTimerAudiencias;
let abortControllerAudiencias;

function actualizarTablaAsincrono(keywordForzado = null) {
    const input = document.getElementById('input-search-audiencias');
    const formSearch = document.getElementById('form-search-audiencias');
    const gridContext = document.getElementById('grid-master-audiencias') || document.getElementById('grid-maestro-audiencias');

    if (!gridContext) return;

    // Abortar petición previa si existe
    if (abortControllerAudiencias) {
        abortControllerAudiencias.abort();
    }
    abortControllerAudiencias = new AbortController();

    const urlParams = new URLSearchParams();

    // Preservar ordenamiento actual de la barra de direcciones
    const currentParams = new URLSearchParams(window.location.search);
    const sortBy = currentParams.get('sortBy');
    const sortDir = currentParams.get('sortDir');
    if (sortBy) urlParams.set('sortBy', sortBy);
    if (sortDir) urlParams.set('sortDir', sortDir);

    // 1. Keyword (Prioridad)
    const valKeyword = keywordForzado !== null ? keywordForzado : (input ? input.value : '');
    if (valKeyword) urlParams.set('keyword', valKeyword);

    // 2. Mes y Año del Calendario (Solo si el filtro de fecha está activo)
    if (miniCalendarState.isDateFilterActive) {
        const date = miniCalendarState.currentDate;
        urlParams.set('mes', date.getMonth() + 1);
        urlParams.set('anio', date.getFullYear());
    }

    // 3. Filtros del Lateral
    const formFiltros = document.getElementById('form-filtros-aud');
    let filterCount = 0;
    if (formFiltros) {
        const fd = new FormData(formFiltros);
        for (const [key, value] of fd.entries()) {
            if (key === 'keyword' || key === 'mes' || key === 'anio') continue;
            if (value && value !== 'null' && value !== '') {
                urlParams.set(key, value);
                filterCount++;
            }
        }
    }

    // 4. Actualizar Indicador Visual (Badge con Conteo)
    const badge = document.getElementById('badge-filtros-aud');
    if (badge) {
        if (filterCount > 0) {
            badge.innerText = filterCount;
            badge.classList.remove('hidden');
        } else {
            badge.classList.add('hidden');
        }
    }

    // 5. Resetear página
    urlParams.set('page', 0);

    // 6. Sincronizar URL en la barra de direcciones
    const newUrl = window.location.pathname + '?' + urlParams.toString();
    window.history.replaceState({ path: newUrl }, '', newUrl);

    // 7. Fetch con AbortController
    return fetch('/audiencias?' + urlParams.toString(), {
        headers: { 'X-Requested-With': 'XMLHttpRequest' },
        signal: abortControllerAudiencias.signal
    })
        .then(res => res.text())
        .then(html => {
            const parser = new DOMParser();
            const doc = parser.parseFromString(html, 'text/html');

            // 1. ACTUALIZAR KPIs
            const kpisNuevos = doc.getElementById('contenedor-kpis-audiencias');
            const kpisActuales = document.getElementById('contenedor-kpis-audiencias');
            if (kpisNuevos && kpisActuales) {
                kpisActuales.innerHTML = kpisNuevos.innerHTML;
            }

            // 2. ACTUALIZAR TABLA DE RESULTADOS
            const tablaNueva = doc.getElementById('grid-resultados-audiencias');
            const tablaActual = document.getElementById('grid-resultados-audiencias');
            if (tablaNueva && tablaActual) {
                tablaActual.innerHTML = tablaNueva.innerHTML;
            }

            // 3. ACTUALIZAR ACCIONES (VerTodos y Exportar)
            const accionesNuevas = doc.getElementById('acciones-barra-audiencias');
            const accionesActuales = document.getElementById('acciones-barra-audiencias');
            if (accionesNuevas && accionesActuales) {
                accionesActuales.innerHTML = accionesNuevas.innerHTML;
            }

            // 4. SINCRONIZAR BARRA DE BÚSQUEDA (Solo si es keyword forzado de calendario/KPIs)
            const searchInput = document.getElementById('input-search-audiencias');
            if (keywordForzado !== null && searchInput) {
                searchInput.value = keywordForzado;
                const btnClear = document.getElementById('btn-clear-search');
                if (keywordForzado) btnClear?.classList.remove('hidden');
                else btnClear?.classList.add('hidden');
            }

            // Actualizar también los eventos e indicadores del mini calendario de audiencias
            fetchMiniCalendarEvents().then(() => {
                renderMiniCalendar();
            });
        })
        .catch(err => {
            if (err.name === 'AbortError') return; // Ignorar si fue abortada
            console.error("Error en actualización asíncrona:", err);
        });
}

function renderMiniCalendar() {
    const grid = document.getElementById('miniCalendarGrid');
    const monthLabel = document.getElementById('miniCalendarMonth');
    if (!grid || !monthLabel) return;

    grid.innerHTML = '';
    const year = miniCalendarState.currentDate.getFullYear();
    const month = miniCalendarState.currentDate.getMonth();

    monthLabel.innerText = `${miniCalendarState.names[month]} ${year}`;
    monthLabel.className = "text-base font-bold text-white font-headings cursor-pointer px-2 py-1 rounded-lg transition-all flex items-center gap-2 border border-gob-guinda bg-gob-guinda hover:bg-gob-guinda/80";
    monthLabel.title = "Ver todo el mes";
    monthLabel.onclick = () => {
        // Al hacer clic en el mes, queremos ver TODO el mes
        miniCalendarState.selectedDate = new Date(0);
        if (miniCalendarState.view === 'week') {
            miniCalendarState.view = 'month';
        }
        const searchInput = document.getElementById('input-search-audiencias');
        if (searchInput) {
            searchInput.value = '';
            const btnClear = document.getElementById('btn-clear-search');
            if (btnClear) btnClear.classList.add('hidden');
        }
        miniCalendarState.isDateFilterActive = true; // ACTIVAR filtro de fecha
        renderMiniCalendar();
        actualizarTablaAsincrono();
    };

    // Update active/inactive switcher buttons class
    const viewMonthBtn = document.getElementById('miniViewMonth');
    const viewWeekBtn = document.getElementById('miniViewWeek');
    if (viewMonthBtn && viewWeekBtn) {
        if (miniCalendarState.view === 'month') {
            viewMonthBtn.className = "flex-1 text-center py-1 font-semibold rounded-md transition-all bg-white shadow-sm text-gob-guinda";
            viewWeekBtn.className = "flex-1 text-center py-1 font-semibold rounded-md transition-all text-gray-500 hover:text-gob-guinda";
        } else {
            viewWeekBtn.className = "flex-1 text-center py-1 font-semibold rounded-md transition-all bg-white shadow-sm text-gob-guinda";
            viewMonthBtn.className = "flex-1 text-center py-1 font-semibold rounded-md transition-all text-gray-500 hover:text-gob-guinda";
        }
    }

    const today = new Date();

    if (miniCalendarState.view === 'week') {
        const referenceDate = (miniCalendarState.selectedDate && miniCalendarState.selectedDate.getTime() > 0) ? miniCalendarState.selectedDate : miniCalendarState.currentDate;
        const startOfWeek = new Date(referenceDate);
        const dayOfWeek = startOfWeek.getDay();
        startOfWeek.setDate(startOfWeek.getDate() - dayOfWeek);

        for (let i = 0; i < 7; i++) {
            const loopDate = new Date(startOfWeek);
            loopDate.setDate(startOfWeek.getDate() + i);

            const cell = document.createElement('div');
            cell.className = 'mini-day-cell';
            cell.innerText = loopDate.getDate();

            const isToday = loopDate.getDate() === today.getDate() &&
                           loopDate.getMonth() === today.getMonth() &&
                           loopDate.getFullYear() === today.getFullYear();
            if (isToday) cell.classList.add('mini-day-today');

            const isSelected = loopDate.getDate() === miniCalendarState.selectedDate.getDate() &&
                               loopDate.getMonth() === miniCalendarState.selectedDate.getMonth() &&
                               loopDate.getFullYear() === miniCalendarState.selectedDate.getFullYear();
            if (isSelected) {
                cell.classList.add('mini-day-selected');
            }

            const dateStr = `${loopDate.getFullYear()}-${String(loopDate.getMonth() + 1).padStart(2, '0')}-${String(loopDate.getDate()).padStart(2, '0')}`;
            const hasEvents = miniCalendarState.events.some(e => e.fecha === dateStr);
            if (hasEvents) {
                cell.classList.add('mini-has-events');
            }

            cell.onclick = () => {
                miniCalendarState.selectedDate = new Date(loopDate);
                miniCalendarState.isDateFilterActive = true;
                renderMiniCalendar();
                actualizarTablaAsincrono(dateStr);
            };

            grid.appendChild(cell);
        }
    } else {
        const firstDay = new Date(year, month, 1).getDay();
        const daysInMonth = new Date(year, month + 1, 0).getDate();

        // Celdas vacías
        for (let i = 0; i < firstDay; i++) {
            const emptyCell = document.createElement('div');
            emptyCell.className = 'mini-day-cell mini-empty';
            grid.appendChild(emptyCell);
        }

        // Días del mes
        for (let day = 1; day <= daysInMonth; day++) {
            const cell = document.createElement('div');
            cell.className = 'mini-day-cell';
            cell.innerText = day;

            const isToday = day === today.getDate() && month === today.getMonth() && year === today.getFullYear();
            if (isToday) cell.classList.add('mini-day-today');

            // Lógica de Selección (Fondo Guinda Sólido)
            const isSelected = day === miniCalendarState.selectedDate.getDate() &&
                month === miniCalendarState.selectedDate.getMonth() &&
                year === miniCalendarState.selectedDate.getFullYear();
            if (isSelected) {
                cell.classList.add('mini-day-selected');
            }

            // Lógica de eventos (Puntos indicadores bajo el día)
            const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
            const hasEvents = miniCalendarState.events.some(e => e.fecha === dateStr);
            if (hasEvents) {
                cell.classList.add('mini-has-events');
            }

            cell.onclick = () => {
                // Actualizar estado visual
                miniCalendarState.selectedDate = new Date(year, month, day);
                miniCalendarState.isDateFilterActive = true; // ACTIVAR filtro de fecha
                renderMiniCalendar();

                // Actualizar tabla con el keyword de fecha
                actualizarTablaAsincrono(dateStr);
            };

            grid.appendChild(cell);
        }
    }
}

function initSearchBar() {
    // Implementación asíncrona vía delegación global en document
}

// Iniciar componentes al cargar
document.addEventListener('DOMContentLoaded', () => {
    initMiniCalendar();
    initSearchBar();

    // D. Delegación Global para Interacción (Paginación, Búsqueda, Limpieza)
    document.addEventListener('click', function (e) {
        // 1. Paginación
        const linkPag = e.target.closest('.pagina-link-aud');
        if (linkPag) {
            e.preventDefault();
            const url = linkPag.getAttribute('href');
            fetch(url, { headers: { 'X-Requested-With': 'XMLHttpRequest' } })
                .then(res => res.text())
                .then(html => {
                    const parser = new DOMParser();
                    const doc = parser.parseFromString(html, 'text/html');
                    const nuevoMaestro = doc.getElementById('grid-maestro-audiencias');
                    const maestroDestino = document.getElementById('grid-maestro-audiencias');
                    if (nuevoMaestro && maestroDestino) {
                        maestroDestino.innerHTML = nuevoMaestro.innerHTML;
                    }
                    window.history.replaceState({ path: url }, '', url);
                })
                .catch(err => console.error("Error en paginación asíncrona:", err));
            return;
        }

        // 2. Botón Limpiar Búsqueda (Barra Superior)
        const btnClear = e.target.closest('#btn-clear-search');
        if (btnClear) {
            const input = document.getElementById('input-search-audiencias');
            if (input) input.value = '';
            actualizarTablaAsincrono();
            return;
        }

        // 3. Botón Limpiar Filtros (Sidebar)
        const btnLimpiar = e.target.closest('#btn-limpiar-filtros-aud');
        if (btnLimpiar) {
            const form = document.getElementById('form-filtros-aud');
            if (form) {
                form.querySelectorAll('select').forEach(s => s.value = '');
            }
            const input = document.getElementById('input-search-audiencias');
            if (input) input.value = '';
            miniCalendarState.isDateFilterActive = false;
            miniCalendarState.selectedDate = new Date(0);
            renderMiniCalendar();
            actualizarTablaAsincrono();
            return;
        }
    });

    // E. Búsqueda en Tiempo Real (Delegación Global con Debounce)
    document.addEventListener('input', function (e) {
        if (e.target.id === 'input-search-audiencias') {
            const input = e.target;
            const btnClear = document.getElementById('btn-clear-search');
            if (input.value) {
                btnClear?.classList.remove('hidden');
            } else {
                btnClear?.classList.add('hidden');
            }

            clearTimeout(debounceTimerAudiencias);
            debounceTimerAudiencias = setTimeout(() => {
                actualizarTablaAsincrono();
            }, 650);
        }
    });

    // F. Submit Form (Delegación Global)
    document.addEventListener('submit', function (e) {
        if (e.target.id === 'form-search-audiencias') {
            e.preventDefault();
            actualizarTablaAsincrono();
        }
    });

    // G. Filtros del sidebar (Delegación Global)
    document.addEventListener('change', function (e) {
        if (e.target.closest('#form-filtros-aud')) {
            actualizarTablaAsincrono();
        }
    });
});

/**
 * Cambia entre la vista de Calendario y Filtros en el sidebar (ROD-183)
 */
function toggleSidebarView(view) {
    const btnCal = document.getElementById('btn-ver-calendario');
    const btnFil = document.getElementById('btn-ver-filtros');
    const contCal = document.getElementById('cont-calendario');
    const contFil = document.getElementById('cont-filtros');

    if (view === 'calendario') {
        // Activar Botón Calendario
        btnCal.classList.add('bg-gob-guinda', 'text-white', 'shadow-md', 'shadow-gob-guinda/20');
        btnCal.classList.remove('text-gray-400', 'hover:bg-gray-50');

        // Desactivar Botón Filtros
        btnFil.classList.remove('bg-gob-guinda', 'text-white', 'shadow-md', 'shadow-gob-guinda/20');
        btnFil.classList.add('text-gray-400', 'hover:bg-gray-50');

        // Mostrar Contenedor Calendario
        contCal.classList.remove('hidden');
        contFil.classList.add('hidden');
    } else {
        // Activar Botón Filtros
        btnFil.classList.add('bg-gob-guinda', 'text-white', 'shadow-md', 'shadow-gob-guinda/20');
        btnFil.classList.remove('text-gray-400', 'hover:bg-gray-50');

        // Desactivar Botón Calendario
        btnCal.classList.remove('bg-gob-guinda', 'text-white', 'shadow-md', 'shadow-gob-guinda/20');
        btnCal.classList.add('text-gray-400', 'hover:bg-gray-50');

        // Mostrar Contenedor Filtros
        contFil.classList.remove('hidden');
        contCal.classList.add('hidden');
    }
}

// Inicialización: Si hay filtros activos con VALOR en la URL, mostrar la pestaña de filtros por defecto
document.addEventListener('DOMContentLoaded', () => {
    const params = new URLSearchParams(window.location.search);
    const filterKeys = ['tipo', 'gerencia', 'materia', 'estatus', 'abogadoId', 'periodo'];

    let count = 0;
    filterKeys.forEach(key => {
        const val = params.get(key);
        if (val !== null && val !== '') count++;
    });

    if (count > 0) {
        toggleSidebarView('filtros');
        const badge = document.getElementById('badge-filtros-aud');
        if (badge) {
            badge.innerText = count;
            badge.classList.remove('hidden');
        }
    }
});


// ==========================================
// 🔔 WEBSOCKET: ACTUALIZACIÓN EN TIEMPO REAL
// ==========================================
function iniciarWebSocketAudiencias() {
    if (typeof SockJS === 'undefined' || typeof Stomp === 'undefined') {
        setTimeout(iniciarWebSocketAudiencias, 1000);
        return;
    }
    const socket = new SockJS('/ws-realtime');
    const stompClient = Stomp.over(socket);
    stompClient.debug = null; // Silenciar logs de debug en consola

    stompClient.connect({}, function (frame) {
        console.log('Conectado a WebSockets para Audiencias en tiempo real.');
        stompClient.subscribe('/topic/alertas', function (message) {
            try {
                const payload = JSON.parse(message.body);
                // Escuchar eventos específicamente para la tabla 'audiencias'
                if (payload.tabla === 'audiencias') {
                    console.log('Cambio detectado en audiencias vía WS:', payload);


                    // Refrescar la tabla y KPIs de forma asíncrona, y LUEGO alumbrar la tarjeta
                    const fetchPromise = actualizarTablaAsincrono();
                    if (fetchPromise && fetchPromise.then) {
                        fetchPromise.then(() => {
                            animarTarjetaKPI(payload.estatus);
                        });
                    } else {
                        // Por si falló o retornó algo nulo
                        setTimeout(() => animarTarjetaKPI(payload.estatus), 800);
                    }
                }
            } catch (e) {
                console.error("Error parseando notificación de websocket", e);
            }
        });
    }, function (error) {
        console.warn('Conexión WS perdida. Reconectando en 5s...');
        setTimeout(iniciarWebSocketAudiencias, 5000);
    });
}

function animarTarjetaKPI(estatus) {
    let cardId = null;
    let colorClase = 'ring-gob-oro';
    let bgClase = 'bg-yellow-50';

    if (estatus === 'PENDIENTE') {
        cardId = 'kpi-card-pendiente';
        colorClase = 'ring-orange-400';
        bgClase = 'bg-orange-50';
    } else if (estatus === 'CON_ACTA') {
        cardId = 'kpi-card-con-acta';
        colorClase = 'ring-blue-400';
        bgClase = 'bg-blue-50';
    } else if (estatus === 'CONCLUIDA') {
        cardId = 'kpi-card-concluida';
        colorClase = 'ring-green-400';
        bgClase = 'bg-green-50';
    } else {
        // Si no hay estatus (ej. un borrado genérico o creación sin estatus explícito), alumbrar el Total
        cardId = 'kpi-card-total';
        colorClase = 'ring-red-400';
        bgClase = 'bg-red-50';
    }

    // Siempre alumbrar la tarjeta Total sutilmente
    const totalCard = document.getElementById('kpi-card-total');
    if (totalCard && cardId !== 'kpi-card-total') {
        totalCard.classList.add('ring-4', 'ring-red-200', 'scale-105', 'shadow-xl');
        setTimeout(() => totalCard.classList.remove('ring-4', 'ring-red-200', 'scale-105', 'shadow-xl'), 1500);
    }

    if (cardId) {
        const card = document.getElementById(cardId);
        if (card) {
            // Aplicar clases de alumbrado (Glow animation)
            card.classList.add('ring-4', colorClase, bgClase, 'scale-110', 'shadow-2xl', 'z-50', 'transition-all', 'duration-300');

            // Quitar el alumbrado después de 1.5s
            setTimeout(() => {
                card.classList.remove('ring-4', colorClase, bgClase, 'scale-110', 'shadow-2xl', 'z-50');
            }, 1500);
        }
    }
}

// Inicializar la conexión WS después de que el DOM esté listo
document.addEventListener('DOMContentLoaded', () => {
    // Retrasar ligeramente la conexión para no competir con el WS del Navbar y asegurar librerías
    setTimeout(iniciarWebSocketAudiencias, 2000);
});
