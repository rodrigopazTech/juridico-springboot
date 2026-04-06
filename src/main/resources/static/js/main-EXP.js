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
            modalNuevo.classList.add('hidden');
        }
    }
};

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
    selectMateria.innerHTML = '<option value="">Cargando materias...</option>';
    selectMateria.disabled = true;
    selectMateria.classList.add('bg-gray-50');

    if (selectAbogado) {
        selectAbogado.innerHTML = '<option value="">Primero selecciona una materia...</option>';
        selectAbogado.disabled = true;
        selectAbogado.classList.add('bg-gray-50');
    }

    if (!gerenciaId) {
        selectMateria.innerHTML = '<option value="">Primero selecciona una gerencia...</option>';
        return Promise.resolve();
    }

    // 2. Traer materias de la API
    return fetch(`/usuarios/api/materias-por-gerencia/${gerenciaId}`)
        .then(res => res.json())
        .then(materias => {
            selectMateria.innerHTML = '<option value="">Selecciona materia...</option>';
            materias.forEach(m => {
                const selected = (materiaIdSeleccionado && m.id == materiaIdSeleccionado) ? 'selected' : '';
                selectMateria.innerHTML += `<option value="${m.id}" ${selected}>${m.nombre}</option>`;
            });

            // Desbloquear selector
            selectMateria.disabled = false;
            selectMateria.classList.remove('bg-gray-50');

            return materias;
        })
        .catch(error => {
            console.error("Error:", error);
            selectMateria.innerHTML = '<option value="">Error al cargar materias</option>';
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

    selectOrgano.innerHTML = '<option value="">Cargando órganos...</option>';
    selectOrgano.disabled = true;
    selectOrgano.classList.add('bg-gray-50');

    if (!gerenciaId) {
        selectOrgano.innerHTML = '<option value="">Primero selecciona una gerencia...</option>';
        return Promise.resolve();
    }

    return fetch(`/admin/importar/api/organos-por-gerencia/${gerenciaId}`)
        .then(res => res.json())
        .then(organos => {
            selectOrgano.innerHTML = '<option value="">Selecciona órgano...</option>';
            organos.forEach(o => {
                const selected = (organoIdSeleccionado && o.id == organoIdSeleccionado) ? 'selected' : '';
                selectOrgano.innerHTML += `<option value="${o.id}" ${selected}>${o.nombre}</option>`;
            });

            selectOrgano.disabled = false;
            selectOrgano.classList.remove('bg-gray-50');
            return organos;
        })
        .catch(error => {
            console.error("Error al cargar organos:", error);
            selectOrgano.innerHTML = '<option value="">Error al cargar órganos</option>';
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

    const gerenciaId = document.getElementById(gerId) ? document.getElementById(gerId).value : null;
    const materiaId = document.getElementById(matId) ? document.getElementById(matId).value : null;
    const selectAbogado = document.getElementById(abogId);

    if (!selectAbogado) return;

    // 1. Limpiar y bloquear
    selectAbogado.innerHTML = '<option value="">Buscando abogados...</option>';
    selectAbogado.disabled = true;
    selectAbogado.classList.add('bg-gray-50');

    if (!materiaId || !gerenciaId) {
        selectAbogado.innerHTML = '<option value="">Primero selecciona una materia...</option>';
        return;
    }

    // 2. Traer abogados filtrados (Backend ahora incluye Gerentes, Abogados y Jefes con lógica de materia)
    return fetch(`/expedientes/api/abogados?gerenciaId=${gerenciaId}&materiaId=${materiaId}`)
        .then(res => res.json())
        .then(abogados => {
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
            return abogados;
        })
        .catch(error => {
            console.error("Error:", error);
            selectAbogado.innerHTML = '<option value="">Error al cargar abogados</option>';
        });
}

// AUTO-INICIALIZACIÓN PARA GERENTES
document.addEventListener('DOMContentLoaded', function () {
    const gerenciaSelect = document.getElementById('select-gerencia-exp');

    // Si existe el select y ya tiene un valor (porque Thymeleaf lo pre-seleccionó)
    const materiaSelect = document.getElementById('select-materia-exp');
    if (gerenciaSelect && gerenciaSelect.value && materiaSelect && !materiaSelect.value) {
        console.log("Gerencia pre-detectada, cargando materias y órganos...");
        cargarMateriasExpediente();
        cargarOrganosExpediente();
    }

    // Inicializar selectores de Etiquetas (Tags)
    if (typeof Choices !== 'undefined') {
        const choicesConfig = {
            removeItemButton: true,
            allowHTML: true,
            placeholderValue: 'Seleccione o escriba...',
            searchPlaceholderValue: 'Buscar o añadir etiqueta',
            noResultsText: 'No se encontraron etiquetas con ese nombre',
            noChoicesText: 'No hay más etiquetas para escoger',
            itemSelectText: 'Presione para seleccionar',
            addItems: true,
            addChoices: true,
            duplicateItemsAllowed: false,
        };

        const elNuevo = document.getElementById('etiquetas-nuevo');
        if (elNuevo) {
            new Choices(elNuevo, choicesConfig);
        }

        const elEditar = document.getElementById('etiquetas-editar');
        if (elEditar) {
            new Choices(elEditar, choicesConfig);
        }
    } else {
        console.warn("La librería Choices.js no se cargó correctamente.");
    }
});