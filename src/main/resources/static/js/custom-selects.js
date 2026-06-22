/**
 * custom-selects.js - Utilidades globales para inicializar Tom Select
 * con autocompletado, creación inline y control de duplicados.
 */

// Inyección de estilos premium para Tom Select en Expedientes, Términos y Audiencias
(function() {
    const path = window.location.pathname;
    if (path.includes('/expedientes') || path.includes('/terminos') || path.includes('/audiencias')) {
        const styleId = 'ts-premium-styles';
        if (document.getElementById(styleId)) return;
        const styleEl = document.createElement('style');
        styleEl.id = styleId;
        styleEl.innerHTML = `
            .ts-dropdown {
              border-radius: 0.75rem !important;
              box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05) !important;
              border: 1px solid rgba(229, 231, 235, 0.8) !important;
              z-index: 9999 !important;
              margin-top: 4px !important;
              background-color: #ffffff !important;
            }
            .ts-dropdown-content {
              padding: 4px !important;
              max-height: 250px !important;
            }
            .ts-dropdown .option, .ts-dropdown .create {
              padding: 8px 12px !important;
              border-radius: 0.375rem !important;
              font-size: 0.875rem !important;
              color: #374151 !important;
              transition: all 0.2s ease !important;
            }
            .ts-dropdown .active, .ts-dropdown .create:hover {
              background-color: #9D2449 !important;
              color: #ffffff !important;
            }
            .ts-control {
              border-radius: 0.5rem !important;
              border: 1px solid #d1d5db !important;
              padding: 8px 12px !important;
              transition: all 0.2s ease !important;
            }
            .ts-wrapper.focus .ts-control {
              border-color: #9D2449 !important;
              box-shadow: 0 0 0 3px rgba(157, 36, 73, 0.15) !important;
            }
        `;
        document.head.appendChild(styleEl);
    }
})();

const SELECT_URLS = {
    'organo': '/catalogos/organos/guardar',
    'tipo-audiencia': '/catalogos/tipos-audiencia/guardar',
    'sede': '/catalogos/sedes/guardar',
    'materia': '/catalogos/materias/guardar'
};

const SELECT_PLACEHOLDERS = {
    'organo': 'Buscar u órgano...',
    'tipo-audiencia': 'Buscar o tipo audiencia...',
    'sede': 'Buscar o sede/estado...',
    'materia': 'Buscar o materia...',
    'expediente': 'Buscar expediente por número o partes...',
    'abogado': 'Buscar abogado...',
    'gerencia': 'Buscar gerencia...'
};

/**
 * Inicializa un select normal convirtiéndolo en un buscador de Tom Select.
 * 
 * @param {string|HTMLElement} elementSelector ID o elemento select.
 * @param {Object} options Opciones de configuración.
 * @param {string} options.tipo Tipo de catálogo ('organo', 'materia', etc.)
 * @param {boolean} options.createInline Si se permite la creación inline (por defecto: false).
 * @param {string} options.gerenciaSelector Selector del campo Gerencia para dependencias de catálogo.
 * @param {string} options.placeholder Placeholder personalizado.
 * @param {boolean} options.multiple Si es un selector múltiple.
 * @param {Function} options.onChange Callback cuando cambia el valor.
 */
function initSearchableSelect(elementSelector, options = {}) {
    const selectEl = typeof elementSelector === 'string' ? document.querySelector(elementSelector) : elementSelector;
    if (!selectEl) {
        console.warn(`[custom-selects] No se encontró el elemento: ${elementSelector}`);
        return null;
    }

    // Si ya existe una instancia de Tom Select, destruirla antes de re-inicializar
    if (selectEl.tomselect) {
        selectEl.tomselect.destroy();
    }

    const tipo = options.tipo || '';
    const createInline = options.createInline || false;
    const gerenciaSelector = options.gerenciaSelector || null;
    const placeholder = options.placeholder || SELECT_PLACEHOLDERS[tipo] || 'Seleccione una opción...';
    const multiple = options.multiple || selectEl.hasAttribute('multiple') || false;
    const useTextAsValue = options.useTextAsValue || false;

    const config = {
        plugins: multiple ? ['remove_button'] : [],
        create: false,
        placeholder: placeholder,
        maxItems: multiple ? null : 1,
        allowEmptyOption: true,
        sortField: { field: "text", direction: "asc" }
    };

    // Configurar creación inline si está habilitada y es un catálogo válido
    if (createInline && SELECT_URLS[tipo]) {
        config.create = function(input, callback) {
            const trimmedInput = input.trim();
            if (!trimmedInput) return callback(false);

            // 1. Validar duplicados en el cliente (case-insensitive)
            const self = this;
            const alreadyExists = Object.values(self.options).some(opt => 
                opt.text.toLowerCase().trim() === trimmedInput.toLowerCase()
            );

            if (alreadyExists) {
                Swal.fire({
                    icon: 'warning',
                    title: 'Elemento existente',
                    text: `El elemento "${trimmedInput}" ya existe en el listado. Selecciónalo en el buscador.`,
                    confirmButtonColor: '#9D2449'
                });
                return callback(false);
            }

            // Obtener el valor de la gerencia si es dependiente (ej: materias requiere gerencia)
            let gerenciaIdVal = null;
            if (gerenciaSelector) {
                const gerEl = document.querySelector(gerenciaSelector);
                if (gerEl) {
                    // Si el selector tiene Tom Select, leer el valor desde su instancia
                    const selectedVal = gerEl.tomselect ? gerEl.tomselect.getValue() : gerEl.value;

                    // Primero intentar leer data-gerencia-id del DOM (select normal)
                    const optElDOM = gerEl.querySelector(`option[value="${selectedVal}"]`);
                    if (optElDOM && optElDOM.hasAttribute('data-gerencia-id')) {
                        gerenciaIdVal = optElDOM.getAttribute('data-gerencia-id');
                    } else if (gerEl.tomselect && gerEl.tomselect.options[selectedVal]) {
                        // Si es Tom Select, buscar en el mapa de opciones interno
                        const tsOpt = gerEl.tomselect.options[selectedVal];
                        gerenciaIdVal = tsOpt.gerenciaId || tsOpt['data-gerencia-id'] || selectedVal || null;
                    } else {
                        // Fallback: usar el valor directamente
                        gerenciaIdVal = selectedVal || null;
                    }
                }
                // Solo bloquear si el tipo realmente necesita gerencia y no la hay
                if (!gerenciaIdVal && (tipo === 'organo' || tipo === 'tipo-audiencia')) {
                    Swal.fire({
                        icon: 'info',
                        title: 'Gerencia / Expediente requerido',
                        text: 'Primero debes seleccionar una gerencia o expediente para poder registrar este elemento.',
                        confirmButtonColor: '#9D2449'
                    });
                    return callback(false);
                }
            }

            // 2. Diálogo de confirmación SweetAlert2 antes de guardar
            Swal.fire({
                title: '¿Crear nuevo elemento?',
                text: `¿Deseas agregar "${trimmedInput}" al catálogo de ${tipo === 'tipo-audiencia' ? 'tipos de audiencia' : tipo === 'organo' ? 'órganos jurisdiccionales' : tipo}?`,
                icon: 'question',
                showCancelButton: true,
                confirmButtonColor: '#9D2449',
                cancelButtonColor: '#6B7280',
                confirmButtonText: 'Sí, crear',
                cancelButtonText: 'Cancelar'
            }).then((result) => {
                if (result.isConfirmed) {
                    // Mostrar loading spinner
                    Swal.fire({
                        title: 'Guardando...',
                        text: 'Registrando elemento en el sistema',
                        allowOutsideClick: false,
                        didOpen: () => {
                            Swal.showLoading();
                        }
                    });

                    const bodyData = { nombre: trimmedInput };
                    if (gerenciaIdVal) {
                        bodyData.gerenciaId = gerenciaIdVal;
                    }

                    fetch(SELECT_URLS[tipo], {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify(bodyData)
                    })
                    .then(res => res.json())
                    .then(data => {
                        Swal.close();
                        if (data.status === 'ok' || data.status === 'exists') {
                            Swal.fire({
                                icon: 'success',
                                title: data.status === 'exists' ? 'Elemento existente' : 'Guardado',
                                text: data.mensaje || 'Se guardó correctamente.',
                                timer: 1500,
                                showConfirmButton: false
                            });
                            // Retornar al callback para insertarlo y seleccionarlo en Tom Select
                            const optVal = useTextAsValue ? trimmedInput : data.id;
                            callback({ value: optVal, text: trimmedInput });
                        } else {
                            Swal.fire({
                                icon: 'error',
                                title: 'Error',
                                text: data.error || 'No se pudo guardar el registro.',
                                confirmButtonColor: '#9D2449'
                            });
                            callback(false);
                        }
                    })
                    .catch(err => {
                        Swal.close();
                        Swal.fire({
                            icon: 'error',
                            title: 'Error de red',
                            text: 'No se pudo conectar con el servidor para guardar.',
                            confirmButtonColor: '#9D2449'
                        });
                        callback(false);
                    });
                } else {
                    callback(false);
                }
            });
        };
        config.createOptionHeader = function(data, escape) {
            return `<div class="create">Crear nuevo catálogo: <strong>${escape(data.input)}</strong>&hellip;</div>`;
        };
    }

    const ts = new TomSelect(selectEl, config);

    if (options.onChange) {
        ts.on('change', options.onChange);
    }

    return ts;
}

/**
 * Helper para actualizar de forma segura las opciones de un Tom Select
 * (útil en selectores en cascada).
 * 
 * @param {TomSelect} tsInstance Instancia de Tom Select.
 * @param {Array} items Lista de objetos con {value, text}.
 * @param {string} emptyPlaceholder Texto cuando no hay elementos.
 */
function updateTomSelectOptions(tsInstance, items, emptyPlaceholder = 'Seleccione una opción...') {
    if (!tsInstance) return;
    
    // Guardar el valor anteriormente seleccionado por si coincide en la nueva lista
    const prevValue = tsInstance.getValue();
    
    tsInstance.clear();
    tsInstance.clearOptions();
    
    // Helper nulo-seguro para actualizar el placeholder del input interno
    const setInputPlaceholder = (placeholder) => {
        const inputEl = tsInstance.control ? tsInstance.control.querySelector('input') : null;
        if (inputEl) inputEl.placeholder = placeholder;
    };

    if (!items || items.length === 0) {
        tsInstance.settings.placeholder = emptyPlaceholder;
        setInputPlaceholder(emptyPlaceholder);
        tsInstance.refreshState();
        return;
    }

    tsInstance.settings.placeholder = emptyPlaceholder;
    setInputPlaceholder(emptyPlaceholder);
    
    items.forEach(item => {
        tsInstance.addOption({ value: item.value, text: item.text });
    });
    
    // Reactivar el valor anterior si está en las nuevas opciones
    if (prevValue && items.some(i => String(i.value) === String(prevValue))) {
        tsInstance.setValue(prevValue);
    }
    
    tsInstance.refreshState();
}
