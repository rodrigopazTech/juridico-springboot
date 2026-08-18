// Función para abrir/cerrar modales por ID
function toggleModal(modalID) {
    const modal = document.getElementById(modalID);
    if (modal) {
        modal.classList.toggle('hidden');

        // Animación simple
        if (!modal.classList.contains('hidden')) {
            const content = modal.querySelector('div[class*="transform"]');
            if (content) {
                content.classList.add('scale-100');
                content.classList.remove('scale-95');
            }
        } else {
            const content = modal.querySelector('div[class*="transform"]');
            if (content) {
                content.classList.add('scale-95');
                content.classList.remove('scale-100');
            }
        }
    }
}

// Cerrar con tecla ESC
document.onkeydown = function (evt) {
    evt = evt || window.event;
    if (evt.keyCode == 27) {
        const modals = document.querySelectorAll('[role="dialog"]');
        modals.forEach(modal => {
            if (!modal.classList.contains('hidden')) {
                modal.classList.add('hidden');
            }
        });
        const modalNuevo = document.getElementById('modal-nuevo-exp');
        if (modalNuevo && !modalNuevo.classList.contains('hidden')) {
            cerrarModalNuevoExpediente();
        }
    }
};

function limpiarModalNuevoExpediente() {
    const modal = document.getElementById('modal-nuevo-exp');
    if (!modal) return;

    // 1. Limpiar inputs simples
    const inputNumero = modal.querySelector('input[name="numero"]');
    if (inputNumero) inputNumero.value = '';

    const inputPartes = modal.querySelector('input[name="partes"]');
    if (inputPartes) inputPartes.value = '';

    const textareaDesc = modal.querySelector('textarea[name="descripcion"]');
    if (textareaDesc) textareaDesc.value = '';

    // 2. Limpiar selects simples (estáticos / no cascada)
    const selectPrioridad = modal.querySelector('select[name="prioridad.id"]');
    if (selectPrioridad) {
        if (selectPrioridad.tomselect) selectPrioridad.tomselect.setValue(selectPrioridad.options[0]?.value || '');
        else selectPrioridad.selectedIndex = 0;
    }

    const selectEtapa = document.getElementById('select-etapa-exp');
    if (selectEtapa) {
        if (selectEtapa.tomselect) {
            updateTomSelectOptions(selectEtapa.tomselect, [], 'Primero selecciona una materia...');
            selectEtapa.tomselect.disable();
        } else {
            selectEtapa.innerHTML = '<option value="">Primero selecciona una materia...</option>';
            selectEtapa.disabled = true;
        }
    }

    const selectSede = modal.querySelector('select[name="sede"]');
    if (selectSede) {
        if (selectSede.tomselect) selectSede.tomselect.setValue('');
        else selectSede.value = '';
    }

    const selectTipo = modal.querySelector('select[name="tipoExpediente.id"]');
    if (selectTipo) {
        if (selectTipo.tomselect) selectTipo.tomselect.setValue('');
        else selectTipo.value = '';
    }

    // 3. Limpiar selectores en cascada (Gerencia, Materia, Órgano, Abogado)
    const selectGerencia = document.getElementById('select-gerencia-exp');
    if (selectGerencia && !selectGerencia.disabled) {
        if (selectGerencia.tomselect) {
            selectGerencia.tomselect.setValue('');
        } else {
            selectGerencia.value = '';
        }
    }

    const selectMateria = document.getElementById('select-materia-exp');
    if (selectMateria) {
        if (selectMateria.tomselect) {
            selectMateria.tomselect.setValue('');
        } else {
            selectMateria.value = '';
        }
    }

    const selectOrgano = document.getElementById('select-organo-exp');
    if (selectOrgano) {
        if (selectOrgano.tomselect) {
            selectOrgano.tomselect.setValue('');
        } else {
            selectOrgano.value = '';
        }
    }

    const selectAbogado = document.getElementById('select-abogado-exp');
    if (selectAbogado && selectAbogado.dataset.locked !== 'true') {
        if (selectAbogado.tomselect) {
            selectAbogado.tomselect.setValue('');
        } else {
            selectAbogado.value = '';
        }
    }

    // Recargar cascadas para aplicar los placeholders/deshabilitar según el estado de gerencia
    cargarMateriasExpediente().then(() => {
        cargarAbogadosExpediente();
    });
    cargarOrganosExpediente();
}

function cerrarModalNuevoExpediente() {
    const modal = document.getElementById('modal-nuevo-exp');
    if (modal) {
        modal.classList.add('hidden');
        limpiarModalNuevoExpediente();
    }
}

// LÓGICA DE SELECTORES EN CASCADA (REUTILIZABLE)
function cargarMateriasExpediente(arg1 = '', arg2 = '', arg3 = '', materiaIdSeleccionado = null) {
    let gerId, matId, abogId;

    if (arg2 === '' && arg3 === '') {
        // arg1 es un sufijo (ej: '', '-edit')
        const suffix = arg1;
        gerId = 'select-gerencia-exp' + suffix;
        matId = 'select-materia-exp' + suffix;
        abogId = 'select-abogado-exp' + suffix;
    } else {
        // IDs proporcionados explícitamente
        gerId = arg1;
        matId = arg2;
        abogId = arg3;
    }

    const gerenciaEl = document.getElementById(gerId);
    const selectMateria = document.getElementById(matId);
    const selectAbogado = document.getElementById(abogId);

    if (!gerenciaEl || !selectMateria) return Promise.resolve();

    const gerenciaId = gerenciaEl.value;

    // 1. Limpiar y bloquear selectores hijos
    if (selectMateria.tomselect) {
        updateTomSelectOptions(selectMateria.tomselect, [], 'Cargando materias...');
        selectMateria.tomselect.disable();
    } else {
        selectMateria.innerHTML = '<option value="">Cargando materias...</option>';
        selectMateria.disabled = true;
        selectMateria.classList.add('bg-gray-50');
    }

    if (selectAbogado) {
        if (selectAbogado.tomselect) {
            updateTomSelectOptions(selectAbogado.tomselect, [], 'Primero selecciona una materia...');
            selectAbogado.tomselect.disable();
        } else {
            selectAbogado.innerHTML = '<option value="">Primero selecciona una materia...</option>';
            selectAbogado.disabled = true;
            selectAbogado.classList.add('bg-gray-50');
        }
    }

    if (!gerenciaId) {
        if (selectMateria.tomselect) {
            updateTomSelectOptions(selectMateria.tomselect, [], 'Primero selecciona una gerencia...');
        } else {
            selectMateria.innerHTML = '<option value="">Primero selecciona una gerencia...</option>';
        }
        return Promise.resolve();
    }

    // 2. Traer materias de la API
    return fetch(`/expedientes/api/materias-por-gerencia/${gerenciaId}`)
        .then(res => res.json())
        .then(materias => {
            if (selectMateria.tomselect) {
                updateTomSelectOptions(selectMateria.tomselect, materias.map(m => ({ value: m.id, text: m.nombre })), 'Selecciona materia...');
                selectMateria.tomselect.enable();
                if (materiaIdSeleccionado) {
                    selectMateria.tomselect.setValue(materiaIdSeleccionado);
                }
            } else {
                selectMateria.innerHTML = '<option value="">Selecciona materia...</option>';
                materias.forEach(m => {
                    const selected = (materiaIdSeleccionado && m.id == materiaIdSeleccionado) ? 'selected' : '';
                    selectMateria.innerHTML += `<option value="${m.id}" ${selected}>${m.nombre}</option>`;
                });
                selectMateria.disabled = false;
                selectMateria.classList.remove('bg-gray-50');
            }

            return materias;
        })
        .catch(error => {
            console.error("Error:", error);
            if (selectMateria.tomselect) {
                updateTomSelectOptions(selectMateria.tomselect, [], 'Error al cargar materias');
            } else {
                selectMateria.innerHTML = '<option value="">Error al cargar materias</option>';
            }
        });
}

function cargarOrganosExpediente(arg1 = '', arg2 = '', organoIdSeleccionado = null) {
    let gerId, orgId;
    if (arg2 === '') {
        const suffix = arg1;
        gerId = 'select-gerencia-exp' + suffix;
        orgId = 'select-organo-exp' + suffix;
    } else {
        gerId = arg1;
        orgId = arg2;
    }

    const gerenciaEl = document.getElementById(gerId);
    const selectOrgano = document.getElementById(orgId);

    if (!gerenciaEl || !selectOrgano) return Promise.resolve();

    const gerenciaId = gerenciaEl.value;

    if (selectOrgano.tomselect) {
        updateTomSelectOptions(selectOrgano.tomselect, [], 'Cargando órganos...');
        selectOrgano.tomselect.disable();
    } else {
        selectOrgano.innerHTML = '<option value="">Cargando órganos...</option>';
        selectOrgano.disabled = true;
        selectOrgano.classList.add('bg-gray-50');
    }

    if (!gerenciaId) {
        if (selectOrgano.tomselect) {
            updateTomSelectOptions(selectOrgano.tomselect, [], 'Primero selecciona una gerencia...');
        } else {
            selectOrgano.innerHTML = '<option value="">Primero selecciona una gerencia...</option>';
        }
        return Promise.resolve();
    }

    return fetch(`/expedientes/api/organos-por-gerencia/${gerenciaId}`)
        .then(res => res.json())
        .then(organos => {
            if (selectOrgano.tomselect) {
                if (organos.length === 0) {
                    updateTomSelectOptions(selectOrgano.tomselect, [], '-- Sin órgano jurisdiccional --');
                } else {
                    updateTomSelectOptions(selectOrgano.tomselect, organos.map(o => ({ value: o.id, text: o.nombre })), 'Selecciona órgano...');
                    if (organoIdSeleccionado) {
                        selectOrgano.tomselect.setValue(organoIdSeleccionado);
                    }
                }
                selectOrgano.tomselect.enable();
            } else {
                if (organos.length === 0) {
                    selectOrgano.innerHTML = '<option value="">-- Sin órgano jurisdiccional --</option>';
                    selectOrgano.disabled = false;
                    selectOrgano.classList.remove('bg-gray-50');
                } else {
                    selectOrgano.innerHTML = '<option value="">Selecciona órgano...</option>';
                    organos.forEach(o => {
                        const selected = (organoIdSeleccionado && o.id == organoIdSeleccionado) ? 'selected' : '';
                        selectOrgano.innerHTML += `<option value="${o.id}" ${selected}>${o.nombre}</option>`;
                    });
                    selectOrgano.disabled = false;
                    selectOrgano.classList.remove('bg-gray-50');
                }
            }
            return organos;
        })
        .catch(error => {
            console.error("Error al cargar organos:", error);
            if (selectOrgano.tomselect) {
                updateTomSelectOptions(selectOrgano.tomselect, [], 'Error al cargar órganos');
            } else {
                selectOrgano.innerHTML = '<option value="">Error al cargar órganos</option>';
            }
        });
}

function cargarAbogadosExpediente(arg1 = '', arg2 = '', arg3 = '', abogadoIdSeleccionado = null) {
    let gerId, matId, abogId;

    if (arg2 === '' && arg3 === '') {
        const suffix = arg1;
        gerId = 'select-gerencia-exp' + suffix;
        matId = 'select-materia-exp' + suffix;
        abogId = 'select-abogado-exp' + suffix;
    } else {
        gerId = arg1;
        matId = arg2;
        abogId = arg3;
    }

    const gerenciaEl = document.getElementById(gerId);
    const materiaEl = document.getElementById(matId);
    const gerenciaId = gerenciaEl ? gerenciaEl.value : null;
    const materiaId = materiaEl ? materiaEl.value : null;
    const selectAbogado = document.getElementById(abogId);

    if (!selectAbogado) return;

    if (selectAbogado.dataset.locked === 'true') {
        return;
    }

    if (selectAbogado.tomselect) {
        updateTomSelectOptions(selectAbogado.tomselect, [], 'Buscando abogados...');
        selectAbogado.tomselect.disable();
    } else {
        selectAbogado.innerHTML = '<option value="">Buscando abogados...</option>';
        selectAbogado.disabled = true;
        selectAbogado.classList.add('bg-gray-50');
    }

    if (!materiaId || !gerenciaId) {
        if (selectAbogado.tomselect) {
            updateTomSelectOptions(selectAbogado.tomselect, [], 'Primero selecciona una materia...');
        } else {
            selectAbogado.innerHTML = '<option value="">Primero selecciona una materia...</option>';
        }
        return;
    }

    return fetch(`/expedientes/api/abogados?gerenciaId=${gerenciaId}&materiaId=${materiaId}`)
        .then(res => res.json())
        .then(abogados => {
            if (selectAbogado.tomselect) {
                if (abogados.length === 0) {
                    updateTomSelectOptions(selectAbogado.tomselect, [], 'No hay abogados para esta materia');
                } else {
                    updateTomSelectOptions(selectAbogado.tomselect, abogados.map(a => ({ value: a.id, text: a.nombreCompleto })), 'Asignar abogado...');
                    if (abogadoIdSeleccionado) {
                        selectAbogado.tomselect.setValue(abogadoIdSeleccionado);
                    }
                }
                selectAbogado.tomselect.enable();
            } else {
                selectAbogado.innerHTML = '<option value="">Asignar abogado...</option>';
                if (abogados.length === 0) {
                    selectAbogado.innerHTML = '<option value="">No hay abogados para esta materia</option>';
                } else {
                    abogados.forEach(a => {
                        const selected = (abogadoIdSeleccionado && a.id == abogadoIdSeleccionado) ? 'selected' : '';
                        selectAbogado.innerHTML += `<option value="${a.id}" ${selected}>${a.nombreCompleto}</option>`;
                    });
                    selectAbogado.disabled = false;
                    selectAbogado.classList.remove('bg-gray-50');
                }
            }
            return abogados;
        })
        .catch(error => {
            console.error("Error:", error);
            if (selectAbogado.tomselect) {
                updateTomSelectOptions(selectAbogado.tomselect, [], 'Error al cargar abogados');
            } else {
                selectAbogado.innerHTML = '<option value="">Error al cargar abogados</option>';
            }
        });
}

function cargarEtapasExpediente(arg1 = '', arg2 = '', etapaIdSeleccionada = null) {
    let matId, etapaId;

    if (arg2 === '') {
        const suffix = arg1;
        matId = 'select-materia-exp' + suffix;
        etapaId = 'select-etapa-exp' + suffix;
    } else {
        matId = arg1;
        etapaId = arg2;
    }

    const materiaEl = document.getElementById(matId);
    const selectEtapa = document.getElementById(etapaId);
    if (!materiaEl || !selectEtapa) return Promise.resolve();

    const materiaId = materiaEl.value;

    if (selectEtapa.tomselect) {
        updateTomSelectOptions(selectEtapa.tomselect, [], 'Cargando etapas...');
        selectEtapa.tomselect.disable();
    } else {
        selectEtapa.innerHTML = '<option value="">Cargando etapas...</option>';
        selectEtapa.disabled = true;
        selectEtapa.classList.add('bg-gray-50');
    }

    if (!materiaId) {
        if (selectEtapa.tomselect) {
            updateTomSelectOptions(selectEtapa.tomselect, [], 'Primero selecciona una materia...');
        } else {
            selectEtapa.innerHTML = '<option value="">Primero selecciona una materia...</option>';
        }
        return Promise.resolve();
    }

    return fetch(`/expedientes/api/etapas-por-materia/${materiaId}`)
        .then(res => res.json())
        .then(etapas => {
            if (selectEtapa.tomselect) {
                if (etapas.length === 0) {
                    updateTomSelectOptions(selectEtapa.tomselect, [], 'Configura etapas para esta materia');
                } else {
                    updateTomSelectOptions(selectEtapa.tomselect, etapas.map(e => ({
                        value: e.id,
                        text: e.parentId ? `- ${e.nombreJerarquico || e.nombre}` : (e.nombreJerarquico || e.nombre)
                    })), 'Selecciona etapa...');
                    selectEtapa.tomselect.enable();
                    selectEtapa.tomselect.setValue(etapaIdSeleccionada || etapas[0].id);
                }
            } else {
                if (etapas.length === 0) {
                    selectEtapa.innerHTML = '<option value="">Configura etapas para esta materia</option>';
                    selectEtapa.disabled = true;
                } else {
                    selectEtapa.innerHTML = '<option value="">Selecciona etapa...</option>';
                    etapas.forEach((e, index) => {
                        const selected = ((etapaIdSeleccionada && e.id == etapaIdSeleccionada) || (!etapaIdSeleccionada && index === 0)) ? 'selected' : '';
                        const label = e.parentId ? `- ${e.nombreJerarquico || e.nombre}` : (e.nombreJerarquico || e.nombre);
                        selectEtapa.innerHTML += `<option value="${e.id}" ${selected}>${label}</option>`;
                    });
                    selectEtapa.disabled = false;
                    selectEtapa.classList.remove('bg-gray-50');
                }
            }
            return etapas;
        })
        .catch(error => {
            console.error("Error al cargar etapas:", error);
            if (selectEtapa.tomselect) {
                updateTomSelectOptions(selectEtapa.tomselect, [], 'Error al cargar etapas');
            } else {
                selectEtapa.innerHTML = '<option value="">Error al cargar etapas</option>';
            }
        });
}

function inicializarSelectsNuevoExpediente() {
    const selectGerencia = document.getElementById('select-gerencia-exp');
    const selectSede = document.getElementById('select-sede-exp');
    const selectMateria = document.getElementById('select-materia-exp');
    const selectOrgano = document.getElementById('select-organo-exp');
    const selectAbogado = document.getElementById('select-abogado-exp');
    const selectEtapa = document.getElementById('select-etapa-exp');

    if (selectGerencia && !selectGerencia.tomselect) {
        initSearchableSelect(selectGerencia, { 
            tipo: 'gerencia',
            onChange: function() {
                cargarMateriasExpediente();
                cargarOrganosExpediente();
                cargarEtapasExpediente();
            }
        });
    }

    if (selectSede && !selectSede.tomselect) {
        initSearchableSelect(selectSede, { tipo: 'sede', createInline: true, useTextAsValue: true });
    }

    if (selectMateria && !selectMateria.tomselect) {
        initSearchableSelect(selectMateria, { 
            tipo: 'materia', 
            createInline: true, 
            gerenciaSelector: '#select-gerencia-exp',
            onChange: function() {
                cargarAbogadosExpediente();
                cargarEtapasExpediente();
            }
        });
    }

    if (selectEtapa && !selectEtapa.tomselect) {
        initSearchableSelect(selectEtapa, { tipo: 'etapa' });
    }

    if (selectOrgano && !selectOrgano.tomselect) {
        initSearchableSelect(selectOrgano, { 
            tipo: 'organo', 
            createInline: true, 
            gerenciaSelector: '#select-gerencia-exp' 
        });
    }

    if (selectAbogado && !selectAbogado.tomselect && selectAbogado.dataset.locked !== 'true') {
        initSearchableSelect(selectAbogado, { tipo: 'abogado' });
    }
}

// AUTO-INICIALIZACIÓN PARA GERENTES
let expedientesFetchController = null;
let expedientesDebounceTimer = null;

function obtenerParametrosListadoExpedientes({ resetPage = false } = {}) {
    const params = new URLSearchParams();
    const formSearch = document.getElementById('form-search-expedientes');
    const formFiltros = document.getElementById('formFiltros');
    const keywordInput = document.getElementById('input-search-expedientes');

    if (formSearch) {
        for (const [key, value] of new FormData(formSearch).entries()) {
            if (value) params.set(key, value);
        }
    }

    if (formFiltros) {
        for (const [key, value] of new FormData(formFiltros).entries()) {
            if (value) params.set(key, value);
            else params.delete(key);
        }
    }

    if (keywordInput && keywordInput.value.trim()) {
        params.set('keyword', keywordInput.value.trim());
    } else {
        params.delete('keyword');
    }

    if (resetPage) params.set('page', '0');
    return params;
}

function actualizarUrlListadoExpedientes(params) {
    const cleanParams = new URLSearchParams(params);
    for (const [key, value] of Array.from(cleanParams.entries())) {
        if (!value || (key === 'page' && value === '0')) cleanParams.delete(key);
    }

    const query = cleanParams.toString();
    window.history.replaceState({}, '', query ? `/expedientes?${query}` : '/expedientes');
}

function reemplazarGridExpedientes(html) {
    const doc = new DOMParser().parseFromString(html, 'text/html');
    const nuevoGrid = doc.getElementById('grid-resultados');
    const gridActual = document.getElementById('grid-resultados');
    if (!nuevoGrid || !gridActual) return false;

    gridActual.innerHTML = nuevoGrid.innerHTML;
    return true;
}

function cargarListadoExpedientes({ resetPage = false, debounce = 0 } = {}) {
    clearTimeout(expedientesDebounceTimer);

    expedientesDebounceTimer = setTimeout(() => {
        const gridActual = document.getElementById('grid-resultados');
        const params = obtenerParametrosListadoExpedientes({ resetPage });

        if (expedientesFetchController) expedientesFetchController.abort();
        expedientesFetchController = new AbortController();
        if (gridActual) gridActual.classList.add('is-refreshing');

        fetch('/expedientes?' + params.toString(), {
            headers: { 'X-Requested-With': 'XMLHttpRequest' },
            signal: expedientesFetchController.signal
        })
            .then(res => {
                if (!res.ok) throw new Error(`Respuesta HTTP ${res.status}`);
                return res.text();
            })
            .then(html => {
                if (reemplazarGridExpedientes(html)) actualizarUrlListadoExpedientes(params);
            })
            .catch(err => {
                if (err.name !== 'AbortError') console.error('Error al actualizar expedientes:', err);
            })
            .finally(() => {
                if (gridActual) gridActual.classList.remove('is-refreshing');
                expedientesFetchController = null;
            });
    }, debounce);
}

function cargarListadoExpedientesConParametros(params) {
    clearTimeout(expedientesDebounceTimer);

    const gridActual = document.getElementById('grid-resultados');
    if (expedientesFetchController) expedientesFetchController.abort();
    expedientesFetchController = new AbortController();
    if (gridActual) gridActual.classList.add('is-refreshing');

    fetch('/expedientes?' + params.toString(), {
        headers: { 'X-Requested-With': 'XMLHttpRequest' },
        signal: expedientesFetchController.signal
    })
        .then(res => {
            if (!res.ok) throw new Error(`Respuesta HTTP ${res.status}`);
            return res.text();
        })
        .then(html => {
            if (reemplazarGridExpedientes(html)) actualizarUrlListadoExpedientes(params);
        })
        .catch(err => {
            if (err.name !== 'AbortError') console.error('Error al actualizar expedientes:', err);
        })
        .finally(() => {
            if (gridActual) gridActual.classList.remove('is-refreshing');
            expedientesFetchController = null;
        });
}

document.addEventListener('DOMContentLoaded', function () {
    inicializarSelectsNuevoExpediente();

    const gerenciaSelect = document.getElementById('select-gerencia-exp');

    // Si existe el select y ya tiene un valor (porque Thymeleaf lo pre-seleccionó)
    const materiaSelect = document.getElementById('select-materia-exp');
    if (gerenciaSelect && gerenciaSelect.value && materiaSelect && !materiaSelect.value) {
        console.log("Gerencia pre-detectada, cargando materias y órganos...");
        cargarMateriasExpediente();
        cargarOrganosExpediente();
    }



    // Inicializar barra de búsqueda (local & server)
    const inputExp = document.getElementById('input-search-expedientes');
    const btnClearExp = document.getElementById('btn-clear-search-expedientes');
    const formSearchExpedientes = document.getElementById('form-search-expedientes');
    const formFiltrosExpedientes = document.getElementById('formFiltros');
    const btnLimpiarFiltrosExpedientes = document.getElementById('btn-limpiar-filtros-expedientes');

    if (inputExp) {
        inputExp.addEventListener('input', function (e) {
            e.stopImmediatePropagation();

            if (btnClearExp) {
                btnClearExp.classList.toggle('hidden', this.value.trim() === '');
            }

            cargarListadoExpedientes({ resetPage: true, debounce: 250 });
        }, true);

        inputExp.addEventListener('keydown', function (e) {
            if (e.key === 'Enter') e.preventDefault();
        });
    }

    if (btnClearExp && inputExp) {
        btnClearExp.addEventListener('click', function (e) {
            e.preventDefault();
            e.stopImmediatePropagation();
            inputExp.value = '';
            btnClearExp.classList.add('hidden');
            cargarListadoExpedientes({ resetPage: true });
        }, true);
    }

    if (formSearchExpedientes) {
        formSearchExpedientes.addEventListener('submit', function (e) {
            e.preventDefault();
            e.stopImmediatePropagation();
            cargarListadoExpedientes({ resetPage: true });
        }, true);
    }

    if (formFiltrosExpedientes) {
        formFiltrosExpedientes.addEventListener('submit', function (e) {
            e.preventDefault();
            cargarListadoExpedientes({ resetPage: true });
        });

        formFiltrosExpedientes.addEventListener('change', function (e) {
            if (e.target.matches('select')) {
                cargarListadoExpedientes({ resetPage: true });
            }
        });
    }

    if (btnLimpiarFiltrosExpedientes) {
        btnLimpiarFiltrosExpedientes.addEventListener('click', function () {
            if (formFiltrosExpedientes) {
                formFiltrosExpedientes.querySelectorAll('select').forEach(select => {
                    select.value = '';
                });
            }

            if (inputExp) {
                inputExp.value = '';
            }

            if (btnClearExp) {
                btnClearExp.classList.add('hidden');
            }

            cargarListadoExpedientes({ resetPage: true });
        });
    }

    document.addEventListener('click', function (e) {
        const link = e.target.closest('.pagina-link');
        if (!link) return;

        e.preventDefault();
        const params = obtenerParametrosListadoExpedientes();
        const page = new URL(link.href).searchParams.get('page');
        if (page !== null) params.set('page', page);
        cargarListadoExpedientesConParametros(params);
    });
    
    let debounceTimer;

    if (inputExp) {
        // A. Mostrar/Ocultar botón X y lanzar Fetch de Búsqueda
        inputExp.addEventListener('input', function (e) {
            clearTimeout(debounceTimer);

            if (btnClearExp) {
                if (this.value.trim() !== "") {
                    btnClearExp.classList.remove('hidden');
                } else {
                    btnClearExp.classList.add('hidden');
                }
            }

            // --- Búsqueda Asíncrona (Fetch y reemplazo de tabla) ---
            debounceTimer = setTimeout(() => {
                const form = document.getElementById('form-search-expedientes');
                if (!form) return;

                const urlParams = new URLSearchParams(new FormData(form));
                urlParams.set('page', 0); // Al tipear, reseteamos a página 0
                
                // Recoger filtros adicionales del select superior
                const formFiltros = document.getElementById('formFiltros');
                if (formFiltros) {
                    const fd = new FormData(formFiltros);
                    for (const [key, value] of fd.entries()) {
                        if (value) urlParams.set(key, value);
                    }
                }

                // Llamar al endpoint
                fetch('/expedientes?' + urlParams.toString(), {
                    headers: { 'X-Requested-With': 'XMLHttpRequest' }
                })
                .then(res => res.text())
                .then(html => {
                    const parser = new DOMParser();
                    const doc = parser.parseFromString(html, 'text/html');
                    const nuevoGrid = doc.getElementById('grid-resultados');
                    const gridActual = document.getElementById('grid-resultados');
                    if (nuevoGrid && gridActual) {
                        gridActual.innerHTML = nuevoGrid.innerHTML;
                    }
                })
                .catch(err => console.error("Error en búsqueda en vivo:", err));

            }, 400); // 400ms debounce
        });

        // B. Manejar Botón Limpiar (X)
        if (btnClearExp) {
            btnClearExp.onclick = () => {
                inputExp.value = '';
                // Disparamos un evento 'input' manual para que se ejecute la búsqueda vacía asíncrona
                inputExp.dispatchEvent(new Event('input', { bubbles: true }));
                
                // O si preferimos forzar recarga (lo comento porque es mejor asíncrono)
                // window.location.href = '/expedientes';
            };
        }

        // C. Detección de "Borrado Total" por teclado 
        // Ya está cubierto por el evento 'input' y el Fetch asíncrono superior.
        // Pero para compatibilidad si hay URLParams:
        inputExp.addEventListener('keyup', function (e) {
            if (this.value.trim() === "" && (e.key === 'Backspace' || e.key === 'Delete')) {
                const urlParams = new URLSearchParams(window.location.search);
                if (urlParams.has('keyword')) {
                    // Mantenemos esto si queremos purgar la URL de los params "keyword" limpios
                    window.history.pushState({}, '', '/expedientes');
                }
            }
        });

        // D. Evitar que la tecla ENTER cause un submit tradicional de página (recarga completa)
        // Ya que el debounce buscará de forma asíncrona.
        const formSearch = document.getElementById('form-search-expedientes');
        if (formSearch) {
            formSearch.addEventListener('submit', function(e) {
                e.preventDefault();
            });
        }
    }

    iniciarWebSocketExpedientes();
});

// ========================================
// TIEMPO REAL (WEBSOCKET) PARA LISTADO
// ========================================
let stompClientExpedientes = null;

function iniciarWebSocketExpedientes() {
    function loadScripts(urls, callback) {
        let loadedCount = 0;
        let scriptsToLoad = [];
        
        urls.forEach(url => {
            if (url.includes('sockjs') && typeof SockJS !== 'undefined') {
                loadedCount++;
            } else if (url.includes('stomp') && typeof Stomp !== 'undefined') {
                loadedCount++;
            } else {
                scriptsToLoad.push(url);
            }
        });

        if (loadedCount === urls.length) {
            callback();
            return;
        }

        let loadedScriptsCount = 0;
        scriptsToLoad.forEach(url => {
            const script = document.createElement('script');
            script.type = 'text/javascript';
            script.src = url;
            script.onload = () => {
                loadedScriptsCount++;
                if (loadedScriptsCount === scriptsToLoad.length) {
                    callback();
                }
            };
            document.head.appendChild(script);
        });
    }

    loadScripts([
        'https://cdnjs.cloudflare.com/ajax/libs/sockjs-client/1.5.1/sockjs.min.js',
        'https://cdnjs.cloudflare.com/ajax/libs/stomp.js/2.3.3/stomp.min.js'
    ], function() {
        if (stompClientExpedientes) return;

        const socket = new SockJS('/ws-realtime');
        stompClientExpedientes = Stomp.over(socket);
        stompClientExpedientes.debug = null;

        stompClientExpedientes.connect({}, function (frame) {
            console.log('🔔 Listado Expedientes: Conexión WebSocket establecida.');
            
            stompClientExpedientes.subscribe('/topic/expedientes', function (message) {
                try {
                    const payload = JSON.parse(message.body);
                    if (payload.tabla === 'expedientes') {
                        console.log("🔔 Expediente modificado o creado. Actualizando listado...");
                        recargarListadoExpedientes();
                    }
                } catch (e) {
                    console.error("Error al procesar mensaje en listado:", e);
                }
            });
        }, function (error) {
            stompClientExpedientes = null;
            setTimeout(iniciarWebSocketExpedientes, 5000);
        });
    });
}

function recargarListadoExpedientes() {
    cargarListadoExpedientes();
}
