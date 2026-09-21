// ========================================
// GESTOR DOCUMENTAL - SISTEMA DE CARPETAS JERÁRQUICO
// ========================================

let expedienteIdGlobal = null;
let numeroExpedienteGlobal = null;
let carpetaActualId = null;
let arbolCarpetasGlobal = [];
let vistaActual = 'list';
let mostrandoPapelera = false;
let documentosActualesGlobal = [];

// Inyectar estilos para la animación de deslizamiento tipo toast en la esquina inferior derecha
(function() {
    if (typeof document !== 'undefined' && !document.getElementById('custom-upload-progress-style')) {
        const style = document.createElement('style');
        style.id = 'custom-upload-progress-style';
        style.innerHTML = `
            @keyframes slideInRight {
                from {
                    transform: translate3d(100%, 0, 0);
                    opacity: 0;
                }
                to {
                    transform: translate3d(0, 0, 0);
                    opacity: 1;
                }
            }
            @keyframes slideOutRight {
                from {
                    transform: translate3d(0, 0, 0);
                    opacity: 1;
                }
                to {
                    transform: translate3d(100%, 0, 0);
                    opacity: 0;
                }
            }
            .swal-slide-in-right {
                animation: slideInRight 0.35s cubic-bezier(0.16, 1, 0.3, 1) forwards !important;
            }
            .swal-slide-out-right {
                animation: slideOutRight 0.35s cubic-bezier(0.16, 1, 0.3, 1) forwards !important;
            }
            /* Estilización premium para el contenedor flotante */
            .swal2-container.swal2-bottom-end {
                padding: 1.5rem !important;
            }
            .swal2-container.swal2-bottom-end .swal2-popup {
                box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.15), 0 10px 10px -5px rgba(0, 0, 0, 0.04) !important;
                border: 1px solid rgba(229, 231, 235, 0.8) !important;
                border-radius: 1rem !important;
            }
        `;
        document.head.appendChild(style);
    }
})();

// ========================================
// MANEJO DE ERRORES DE API
// ========================================

async function verificarRespuestaApi(response) {
    if (response.redirected && response.url.includes('/login')) {
        mostrarNotificacion('Tu sesión terminó. Te llevaremos al inicio de sesión…', 'warning');
        setTimeout(() => window.location.href = '/', 2500);
        throw new Error('SESION_EXPIRADA');
    }
    if (response.status === 401) {
        mostrarNotificacion('Tu sesión terminó. Te llevaremos al inicio de sesión…', 'warning');
        setTimeout(() => window.location.href = '/', 2500);
        throw new Error('SESION_EXPIRADA');
    }
    if (response.status === 403) {
        throw new Error('SIN_PERMISO');
    }
    if (!response.ok) {
        throw new Error('ERROR_SERVIDOR_' + response.status);
    }
    return response;
}

function obtenerMensajeError(error) {
    if (error.message === 'SESION_EXPIRADA') return null;
    if (error.message === 'SIN_PERMISO') {
        return {
            icono: 'fa-lock',
            color: 'text-amber-500',
            texto: 'No tienes permiso para ver los documentos de este expediente.'
        };
    }
    return {
        icono: 'fa-exclamation-triangle',
        color: 'text-red-500',
        texto: 'Error al cargar contenido. Intenta cerrar y volver a abrir el gestor.'
    };
}


// ========================================
// INICIALIZACIÓN
// ========================================

function openGestorDocumental(expedienteId, numeroExpediente) {
    // Validar que el expedienteId sea válido antes de continuar
    if (!expedienteId || expedienteId === 'null' || String(expedienteId).trim() === '') {
        mostrarNotificacion('No se pudo identificar el expediente. Intenta recargar la página.', 'error');
        return;
    }

    expedienteIdGlobal = expedienteId;
    numeroExpedienteGlobal = numeroExpediente || 'Expediente';
    carpetaActualId = null;
    mostrandoPapelera = false;

    const modal = document.getElementById('modal-gestor-documental');
    modal.classList.remove('hidden');
    modal.classList.add('flex');
    actualizarEstadoPapelera();

    // Cargar datos iniciales
    cargarArbolCarpetas();
    cargarEspacioOcupado();
    cargarDocumentos();
    actualizarBadgePapelera();
}

function closeGestorDocumental() {
    const modal = document.getElementById('modal-gestor-documental');
    modal.classList.add('hidden');
    modal.classList.remove('flex');

    // Limpiar estado
    expedienteIdGlobal = null;
    numeroExpedienteGlobal = null;
    carpetaActualId = null;
    arbolCarpetasGlobal = [];
    mostrandoPapelera = false;
}

// ========================================
// ÁRBOL DE CARPETAS
// ========================================

async function cargarArbolCarpetas() {
    try {
        const response = await fetch(`/api/carpetas/expedientes/${expedienteIdGlobal}/arbol`);
        await verificarRespuestaApi(response);

        arbolCarpetasGlobal = await response.json();
        renderizarArbolCarpetas();
        actualizarSelectoresCarpetas();

    } catch (error) {
        if (error.message === 'SESION_EXPIRADA') return;
        console.error('Error al cargar árbol de carpetas:', error);
        document.getElementById('arbol-carpetas').innerHTML = `
            <div class="text-center text-red-500 py-8">
                <i class="fas fa-exclamation-triangle text-2xl mb-2"></i>
                <p class="text-xs">Error al cargar carpetas</p>
            </div>
        `;
    }
}

function renderizarArbolCarpetas() {
    const container = document.getElementById('arbol-carpetas');

    if (arbolCarpetasGlobal.length === 0) {
        container.innerHTML = `
            <div class="text-center text-gray-400 py-8">
                <i class="fas fa-folder-open text-2xl mb-2"></i>
                <p class="text-xs">Sin carpetas</p>
            </div>
        `;
        return;
    }

    // Crear nodo raíz del expediente con flecha funcional
    let html = `
        <div class="explorer-tree">
            <div class="explorer-item group ${carpetaActualId === null ? 'selected' : ''}" 
                 data-carpeta-id="raiz"
                 ondragover="handleFolderDragOver(event)"
                 ondragenter="handleFolderDragEnter(event)"
                 ondragleave="handleFolderDragLeave(event)"
                 ondrop="handleFolderDrop(event, null)"
                 onclick="seleccionarCarpeta(null)">
                <div class="explorer-chevron-box">
                    <i class="fas fa-chevron-down text-[10px] text-gray-400 explorer-toggle cursor-pointer"
                       onclick="event.stopPropagation(); toggleCarpeta(event, 'raiz')"></i>
                </div>
                <i class="fas fa-folder-open text-gob-guinda text-sm mr-1.5"></i>
                <span class="explorer-label text-gray-800 font-bold">${numeroExpedienteGlobal}</span>
            </div>
            <div class="explorer-children" id="children-raiz">
    `;

    // Renderizar carpetas recursivamente
    arbolCarpetasGlobal.forEach(carpeta => {
        html += renderizarNodoCarpeta(carpeta, 1);
    });

    html += `    </div>
        </div>`;

    container.innerHTML = html;
}

function renderizarNodoCarpeta(carpeta, nivel) {
    const tieneSubcarpetas = carpeta.subcarpetas && carpeta.subcarpetas.length > 0;
    const estaSeleccionada = carpetaActualId === carpeta.id;
    const expandido = true;

    let html = `
        <div class="explorer-group">
            <div class="explorer-item group ${estaSeleccionada ? 'selected' : ''}" 
                 data-carpeta-id="${carpeta.id}"
                 draggable="true"
                 ondragstart="handleFolderDragStart(event, '${carpeta.id}')"
                 ondragover="handleFolderDragOver(event)"
                 ondragenter="handleFolderDragEnter(event)"
                 ondragleave="handleFolderDragLeave(event)"
                 ondrop="handleFolderDrop(event, '${carpeta.id}')"
                 onclick="seleccionarCarpeta('${carpeta.id}')">
                <div class="explorer-chevron-box">
                    ${tieneSubcarpetas ? `
                        <i class="fas fa-chevron-${expandido ? 'down' : 'right'} text-[10px] text-gray-400 explorer-toggle cursor-pointer" 
                           onclick="event.stopPropagation(); toggleCarpeta(event, '${carpeta.id}')"></i>
                    ` : ''}
                </div>
                <i class="fas fa-folder${tieneSubcarpetas && expandido ? '-open' : ''} text-amber-500 text-sm mr-1.5"></i>
                <span class="explorer-label text-gray-600 flex-1 truncate">${carpeta.nombre}</span>
                ${carpeta.cantidadDocumentos > 0 ? `
                    <span class="text-[10px] text-gob-guinda bg-gob-guinda/10 px-1.5 rounded ml-1">${carpeta.cantidadDocumentos}</span>
                ` : ''}
                <div class="explorer-actions opacity-0 group-hover:opacity-100">
                    ${!carpeta.esSistema ? `
                        <button onclick="event.stopPropagation(); renombrarCarpeta('${carpeta.id}', '${carpeta.nombre}')" 
                                class="explorer-action" title="Renombrar">
                            <i class="fas fa-edit"></i>
                        </button>
                        <button onclick="event.stopPropagation(); eliminarCarpeta('${carpeta.id}')" 
                                class="explorer-action text-red-500 hover:text-red-600" title="Eliminar">
                            <i class="fas fa-trash"></i>
                        </button>
                    ` : ''}
                </div>
            </div>
    `;

    if (tieneSubcarpetas) {
        html += `<div class="explorer-children ${expandido ? '' : 'hidden'}" id="children-${carpeta.id}">`;
        carpeta.subcarpetas.forEach(sub => {
            html += renderizarNodoCarpeta(sub, nivel + 1);
        });
        html += `</div>`;
    }

    html += `</div>`;

    return html;
}

function toggleCarpeta(event, carpetaId) {
    const childrenDiv = document.getElementById(`children-${carpetaId}`);
    if (!childrenDiv) return;

    let toggleIcon = null;
    if (event && event.target) {
        toggleIcon = event.target.classList.contains('explorer-toggle') 
            ? event.target 
            : event.target.closest('.explorer-toggle');
    }

    if (!toggleIcon) {
        const itemDiv = document.querySelector(`[data-carpeta-id="${carpetaId}"]`);
        if (itemDiv) {
            toggleIcon = itemDiv.querySelector('.explorer-toggle');
        }
    }

    if (!toggleIcon) return;
    
    const folderIcon = toggleIcon.closest('.explorer-item').querySelector('.fa-folder, .fa-folder-open');

    if (childrenDiv.classList.contains('hidden')) {
        childrenDiv.classList.remove('hidden');
        toggleIcon.classList.remove('fa-chevron-right');
        toggleIcon.classList.add('fa-chevron-down');
        if (folderIcon) {
            folderIcon.classList.remove('fa-folder');
            folderIcon.classList.add('fa-folder-open');
        }
    } else {
        childrenDiv.classList.add('hidden');
        toggleIcon.classList.remove('fa-chevron-down');
        toggleIcon.classList.add('fa-chevron-right');
        if (folderIcon) {
            folderIcon.classList.remove('fa-folder-open');
            folderIcon.classList.add('fa-folder');
        }
    }
}

function seleccionarCarpeta(carpetaId) {
    mostrandoPapelera = false;
    carpetaActualId = carpetaId;
    actualizarEstadoPapelera();

    // Actualizar UI
    document.querySelectorAll('.explorer-item').forEach(node => {
        node.classList.remove('selected');
    });

    if (carpetaId) {
        const nodo = document.querySelector(`[data-carpeta-id="${carpetaId}"]`);
        if (nodo) nodo.classList.add('selected');
    } else {
        // Seleccionar raíz
        const root = document.querySelector('.explorer-tree > .explorer-item');
        if (root) root.classList.add('selected');
    }

    // Actualizar breadcrumb
    actualizarBreadcrumb();

    // Cargar documentos de esta carpeta
    cargarDocumentos();
}

function actualizarBreadcrumb() {
    const breadcrumb = document.getElementById('breadcrumb-carpetas');

    if (mostrandoPapelera) {
        breadcrumb.innerHTML = `
            <i class="fas fa-trash-restore text-gob-guinda mr-1"></i>
            <span class="font-semibold">Papelera</span>
            <span class="ml-2 text-gray-400">Los documentos se eliminan definitivamente despues de 90 dias.</span>
        `;
        return;
    }

    if (!carpetaActualId) {
        breadcrumb.innerHTML = `
            <i class="fas fa-folder text-gob-oro mr-1"></i>
            <span class="font-semibold">${numeroExpedienteGlobal}</span>
        `;
        return;
    }

    // Obtener ruta de carpeta
    const ruta = obtenerRutaCarpeta(carpetaActualId);

    let html = `
        <button onclick="seleccionarCarpeta(null)" class="hover:text-gob-guinda transition-colors">
            <i class="fas fa-folder text-gob-oro mr-1"></i> ${numeroExpedienteGlobal}
        </button>
    `;

    ruta.forEach((carpeta, index) => {
        html += `
            <i class="fas fa-chevron-right mx-2 text-gray-300"></i>
            <button onclick="seleccionarCarpeta('${carpeta.id}')" 
                    class="hover:text-gob-guinda transition-colors ${index === ruta.length - 1 ? 'font-semibold text-gob-guinda' : ''}">
                ${carpeta.nombre}
            </button>
        `;
    });

    breadcrumb.innerHTML = html;
}

function mostrarPapeleraDocumentos() {
    mostrandoPapelera = true;
    carpetaActualId = null;
    actualizarEstadoPapelera();
    actualizarBreadcrumb();
    cargarDocumentos();
}

function actualizarEstadoPapelera() {
    const btnPapelera = document.getElementById('btn-papelera-documentos');
    const btnNuevaCarpeta = document.getElementById('btn-nueva-carpeta');
    const btnSubir = document.getElementById('btn-subir-documento-toolbar');
    const searchInput = document.getElementById('search-documentos-gestor');

    if (btnPapelera) {
        btnPapelera.classList.toggle('text-gob-guinda', mostrandoPapelera);
        btnPapelera.classList.toggle('text-gray-600', !mostrandoPapelera);
    }
    if (btnNuevaCarpeta) {
        btnNuevaCarpeta.disabled = mostrandoPapelera;
        btnNuevaCarpeta.classList.toggle('opacity-40', mostrandoPapelera);
    }
    if (btnSubir) {
        btnSubir.disabled = mostrandoPapelera;
        btnSubir.classList.toggle('opacity-40', mostrandoPapelera);
    }
    if (searchInput) {
        searchInput.disabled = mostrandoPapelera;
        searchInput.value = '';
        searchInput.placeholder = mostrandoPapelera ? 'La busqueda no aplica en papelera' : 'Buscar archivo en esta carpeta...';
    }
}

function obtenerRutaCarpeta(carpetaId, arbol = arbolCarpetasGlobal, ruta = []) {
    for (const carpeta of arbol) {
        if (carpeta.id === carpetaId) {
            return [...ruta, carpeta];
        }

        if (carpeta.subcarpetas && carpeta.subcarpetas.length > 0) {
            const resultado = obtenerRutaCarpeta(carpetaId, carpeta.subcarpetas, [...ruta, carpeta]);
            if (resultado) return resultado;
        }
    }
    return null;
}

// ========================================
// GESTIÓN DE CARPETAS
// ========================================

function abrirModalNuevaCarpeta() {
    if (mostrandoPapelera) {
        return;
    }

    document.getElementById('carpeta-actual-id').value = carpetaActualId || '';
    document.getElementById('nombre-nueva-carpeta').value = '';

    // Actualizar indicador visual de ubicación (usa la carpeta actualmente seleccionada)
    const indicador = document.getElementById('nombre-carpeta-destino');
    if (!carpetaActualId) {
        indicador.textContent = numeroExpedienteGlobal || 'Raíz';
    } else {
        const nombreCarpeta = buscarNombreCarpeta(carpetaActualId, arbolCarpetasGlobal);
        indicador.textContent = nombreCarpeta || 'Carpeta seleccionada';
    }

    const modal = document.getElementById('modal-nueva-carpeta');
    modal.classList.remove('hidden');
    modal.classList.add('flex');

    // Focus en el input
    setTimeout(() => {
        document.getElementById('nombre-nueva-carpeta').focus();
    }, 100);
}

function buscarNombreCarpeta(carpetaId, carpetas) {
    for (const carpeta of carpetas) {
        if (carpeta.id === carpetaId) {
            return carpeta.nombre;
        }
        if (carpeta.subcarpetas && carpeta.subcarpetas.length > 0) {
            const resultado = buscarNombreCarpeta(carpetaId, carpeta.subcarpetas);
            if (resultado) return resultado;
        }
    }
    return null;
}

function cerrarModalNuevaCarpeta() {
    const modal = document.getElementById('modal-nueva-carpeta');
    modal.classList.add('hidden');
    modal.classList.remove('flex');
}

async function crearCarpeta() {
    const nombre = document.getElementById('nombre-nueva-carpeta').value.trim();
    // Usar la carpeta actualmente seleccionada como padre
    const carpetaPadreId = carpetaActualId || null;

    if (!nombre) {
        alert('Por favor ingrese un nombre para la carpeta');
        return;
    }

    const btn = document.getElementById('btn-crear-carpeta');
    btn.disabled = true;
    btn.innerHTML = '<i class="fas fa-spinner fa-spin mr-1"></i> Creando...';

    try {
        const formData = new FormData();
        formData.append('nombre', nombre);
        if (carpetaPadreId) {
            formData.append('carpetaPadreId', carpetaPadreId);
        }

        const response = await fetch(`/api/carpetas/expedientes/${expedienteIdGlobal}`, {
            method: 'POST',
            body: formData
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Error al crear carpeta');
        }

        // Cerrar modal
        cerrarModalNuevaCarpeta();

        // Recargar árbol
        await cargarArbolCarpetas();

        // Mostrar mensaje de éxito
        mostrarNotificacion('Carpeta creada correctamente', 'success');

    } catch (error) {
        console.error('Error:', error);
        alert(error.message);
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="fas fa-plus mr-1"></i> Crear Carpeta';
    }
}

async function renombrarCarpeta(carpetaId, nombreActual) {
    const nuevoNombre = prompt('Nuevo nombre de la carpeta:', nombreActual);

    if (!nuevoNombre || nuevoNombre.trim() === '' || nuevoNombre === nombreActual) {
        return;
    }

    try {
        const formData = new FormData();
        formData.append('nombre', nuevoNombre.trim());

        const response = await fetch(`/api/carpetas/${carpetaId}`, {
            method: 'PUT',
            body: formData
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Error al renombrar carpeta');
        }

        // Recargar árbol
        await cargarArbolCarpetas();
        mostrarNotificacion('Carpeta renombrada correctamente', 'success');

    } catch (error) {
        console.error('Error:', error);
        alert(error.message);
    }
}

let carpetaIdAEliminar = null;

function eliminarCarpeta(carpetaId) {
    carpetaIdAEliminar = carpetaId;
    const modal = document.getElementById('modal-confirmar-eliminar-carpeta');
    if (modal) {
        modal.classList.remove('hidden');
        modal.classList.add('flex');
    }
}

function cerrarConfirmarEliminarCarpeta() {
    carpetaIdAEliminar = null;
    const modal = document.getElementById('modal-confirmar-eliminar-carpeta');
    if (modal) {
        modal.classList.add('hidden');
        modal.classList.remove('flex');
    }
}

async function confirmarEliminarCarpeta() {
    if (!carpetaIdAEliminar) return;
    const btn = document.getElementById('btn-confirmar-eliminar-carpeta');
    if (btn) {
        btn.disabled = true;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin mr-1"></i> Eliminando...';
    }

    try {
        const response = await fetch(`/api/carpetas/${carpetaIdAEliminar}`, {
            method: 'DELETE'
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Error al eliminar carpeta');
        }

        cerrarConfirmarEliminarCarpeta();
        
        // Si estábamos en esta carpeta, volver a raíz
        if (carpetaActualId === carpetaIdAEliminar) {
            carpetaActualId = null;
        }

        // Recargar árbol
        await cargarArbolCarpetas();
        cargarDocumentos();
        mostrarNotificacion('Carpeta eliminada correctamente', 'success');

    } catch (error) {
        console.error('Error:', error);
        alert(error.message);
        cerrarConfirmarEliminarCarpeta();
    } finally {
        if (btn) {
            btn.disabled = false;
            btn.innerHTML = '<i class="fas fa-trash mr-1"></i> Eliminar';
        }
    }
}

function actualizarSelectoresCarpetas() {
    // Actualizar select de carpeta destino (modal subir documento) - mostrar todas
    const selectDestino = document.getElementById('doc-carpeta-gestor');
    if (selectDestino) {
        selectDestino.innerHTML = `<option value="">${numeroExpedienteGlobal} (raíz)</option>`;
        agregarOpcionesCarpetas(selectDestino, arbolCarpetasGlobal, 0);
        
        // Pre-seleccionar carpeta actual si hay una
        if (carpetaActualId) {
            selectDestino.value = carpetaActualId;
        }
    }
    
    // Actualizar select de mover documento
    const selectMover = document.getElementById('carpeta-destino-mover');
    if (selectMover) {
        selectMover.innerHTML = `<option value="">${numeroExpedienteGlobal} (raíz)</option>`;
        agregarOpcionesCarpetas(selectMover, arbolCarpetasGlobal, 0);
    }
}

function encontrarCarpeta(carpetas, id) {
    for (const carpeta of carpetas) {
        if (carpeta.id === id || carpeta.id === parseInt(id)) {
            return carpeta;
        }
        if (carpeta.subcarpetas && carpeta.subcarpetas.length > 0) {
            const encontrada = encontrarCarpeta(carpeta.subcarpetas, id);
            if (encontrada) return encontrada;
        }
    }
    return null;
}

function agregarOpcionesCarpetas(select, carpetas, nivel) {
    carpetas.forEach(carpeta => {
        const option = document.createElement('option');
        option.value = carpeta.id;
        // Usar padding visual simple sin caracteres feos
        option.textContent = '\u00A0'.repeat(nivel * 4) + carpeta.nombre;
        select.appendChild(option);

        if (carpeta.subcarpetas && carpeta.subcarpetas.length > 0) {
            agregarOpcionesCarpetas(select, carpeta.subcarpetas, nivel + 1);
        }
    });
}

// ========================================
// DOCUMENTOS
// ========================================

async function cargarDocumentos() {
    const vistaLista = document.getElementById('vista-lista');
    const vistaGrid = document.getElementById('vista-grid');

    // Mostrar loading
    vistaLista.innerHTML = `
        <div class="text-center text-gray-400 py-12">
            <i class="fas fa-spinner fa-spin text-3xl mb-2"></i>
            <p>Cargando contenido...</p>
        </div>
    `;

    try {
        if (mostrandoPapelera) {
            const response = await fetch(`/api/documentos/expedientes/${expedienteIdGlobal}/papelera`);

            if (!response.ok) {
                throw new Error('Error al cargar la papelera');
            }

            const documentos = await response.json();
            renderizarPapeleraExplorador(documentos);
            return;
        }

        // Obtener subcarpetas de la carpeta actual
        let subcarpetas = [];
        if (carpetaActualId) {
            const carpetaActual = encontrarCarpeta(arbolCarpetasGlobal, carpetaActualId);
            if (carpetaActual && carpetaActual.subcarpetas) {
                subcarpetas = carpetaActual.subcarpetas;
            }
        } else {
            // Estamos en raíz, mostrar carpetas de primer nivel
            subcarpetas = arbolCarpetasGlobal;
        }

        // Obtener documentos de la carpeta actual
        let url;
        if (carpetaActualId) {
            url = `/api/documentos/carpetas/${carpetaActualId}`;
        } else {
            // En raíz, obtener solo documentos sin carpeta
            url = `/api/documentos/expedientes/${expedienteIdGlobal}/raiz`;
        }

        const response = await fetch(url);

        if (!response.ok) {
            throw new Error('Error al cargar documentos');
        }

        const documentos = await response.json();
        documentosActualesGlobal = documentos || [];
        renderizarContenido(subcarpetas, documentos);

    } catch (error) {
        if (error.message === 'SESION_EXPIRADA') return;
        console.error('Error al cargar documentos:', error);
        const info = obtenerMensajeError(error);
        if (info) {
            vistaLista.innerHTML = `
                <div class="text-center ${info.color} py-12">
                    <i class="fas ${info.icono} text-3xl mb-2"></i>
                    <p>${info.texto}</p>
                </div>
            `;
        }
    }
}

function renderizarContenido(subcarpetas, documentos) {
    const vistaLista = document.getElementById('vista-lista');
    const vistaGrid = document.getElementById('vista-grid');

    const tieneContenido = subcarpetas.length > 0 || documentos.length > 0;

    if (!tieneContenido && mostrandoPapelera) {
        const mensajePapeleraVacia = `
            <div class="text-center text-gray-400 py-12">
                <i class="fas fa-trash text-5xl mb-3"></i>
                <p class="text-lg font-semibold">Papelera vacia</p>
                <p class="text-sm mt-1">No hay documentos pendientes de recuperacion</p>
            </div>
        `;
        vistaLista.innerHTML = mensajePapeleraVacia;
        vistaGrid.innerHTML = mensajePapeleraVacia;
        return;
    }

    if (!tieneContenido) {
        const mensajeVacio = `
            <div class="text-center text-gray-400 py-12">
                <i class="fas fa-folder-open text-5xl mb-3"></i>
                <p class="text-lg font-semibold">Carpeta vacía</p>
                <p class="text-sm mt-1">Crea una subcarpeta o sube un documento</p>
            </div>
        `;
        vistaLista.innerHTML = mensajeVacio;
        vistaGrid.innerHTML = mensajeVacio;
        return;
    }

    // Vista de lista
    let htmlLista = '';
    
    // Primero las subcarpetas
    subcarpetas.forEach(carpeta => {
        const cantidadItems = (carpeta.subcarpetas?.length || 0) + (carpeta.cantidadDocumentos || 0);
        htmlLista += `
            <div class="flex items-center justify-between p-3 bg-amber-50 border border-amber-200 rounded-lg hover:shadow-md hover:bg-amber-100 transition-all cursor-pointer folder-drop-target"
                 data-carpeta-id="${carpeta.id}"
                 draggable="true"
                 ondragstart="handleFolderDragStart(event, '${carpeta.id}')"
                 ondragover="handleFolderDragOver(event)"
                 ondragenter="handleFolderDragEnter(event)"
                 ondragleave="handleFolderDragLeave(event)"
                 ondrop="handleFolderDrop(event, '${carpeta.id}')"
                 onclick="seleccionarCarpeta('${carpeta.id}')">
                <div class="flex items-center gap-3 flex-1">
                    <div class="text-3xl">📁</div>
                    <div class="flex-1 min-w-0">
                        <p class="font-semibold text-sm text-gray-900">${carpeta.nombre}</p>
                        <p class="text-xs text-gray-500">${cantidadItems} elemento${cantidadItems !== 1 ? 's' : ''}</p>
                    </div>
                </div>
                <div class="flex items-center gap-2">
                    <i class="fas fa-chevron-right text-gray-400"></i>
                </div>
            </div>
        `;
    });
    
    // Luego los documentos
    documentos.forEach(doc => {
        const icono = obtenerIconoArchivo(doc.tipoMime);
        const tamanio = formatearTamanio(doc.tamanioBytes);
        const fecha = new Date(doc.fechaSubida).toLocaleDateString('es-MX');

        htmlLista += `
            <div class="flex items-center justify-between p-3 bg-white border border-gray-200 rounded-lg hover:shadow-md transition-shadow"
                 draggable="true"
                 ondragstart="handleDocumentDragStart(event, '${doc.id}')">
                <div class="flex items-center gap-3 flex-1">
                    <div class="text-3xl">${icono}</div>
                    <div class="flex-1 min-w-0">
                        <p class="font-semibold text-sm text-gray-900 truncate">${doc.nombreOriginal}</p>
                        <p class="text-xs text-gray-500">${tamanio} • ${fecha}</p>
                    </div>
                </div>
                <div class="flex items-center gap-2">
                    <button onclick="previsualizarDocumento('${doc.id}')" 
                            class="p-2 text-blue-600 hover:bg-blue-50 rounded transition-colors" title="Vista previa">
                        <i class="fas fa-eye"></i>
                    </button>
                    <button onclick="descargarDocumento('${doc.id}')" 
                            class="p-2 text-green-600 hover:bg-green-50 rounded transition-colors" title="Descargar">
                        <i class="fas fa-download"></i>
                    </button>
                    <button onclick="abrirModalMoverDocumento('${doc.id}', '${doc.nombreOriginal.replace(/'/g, "\\'")}')"
                            class="p-2 text-amber-600 hover:bg-amber-50 rounded transition-colors" title="Mover">
                        <i class="fas fa-folder-tree"></i>
                    </button>
                    <button onclick="eliminarDocumento('${doc.id}')" 
                            class="p-2 text-red-600 hover:bg-red-50 rounded transition-colors" title="Eliminar">
                        <i class="fas fa-trash"></i>
                    </button>
                </div>
            </div>
        `;
    });

    vistaLista.innerHTML = htmlLista;

    // Vista de cuadrícula
    let htmlGrid = '';
    
    // Primero las subcarpetas
    subcarpetas.forEach(carpeta => {
        const cantidadItems = (carpeta.subcarpetas?.length || 0) + (carpeta.cantidadDocumentos || 0);
        htmlGrid += `
            <div class="bg-amber-50 border border-amber-200 rounded-lg p-4 hover:shadow-lg hover:bg-amber-100 transition-all text-center cursor-pointer folder-drop-target"
                 data-carpeta-id="${carpeta.id}"
                 draggable="true"
                 ondragstart="handleFolderDragStart(event, '${carpeta.id}')"
                 ondragover="handleFolderDragOver(event)"
                 ondragenter="handleFolderDragEnter(event)"
                 ondragleave="handleFolderDragLeave(event)"
                 ondrop="handleFolderDrop(event, '${carpeta.id}')"
                 onclick="seleccionarCarpeta('${carpeta.id}')">
                <div class="text-5xl mb-3">📁</div>
                <p class="font-semibold text-sm text-gray-900 truncate mb-2">${carpeta.nombre}</p>
                <p class="text-xs text-gray-500">${cantidadItems} elemento${cantidadItems !== 1 ? 's' : ''}</p>
            </div>
        `;
    });
    
    // Luego los documentos
    documentos.forEach(doc => {
        const icono = obtenerIconoArchivo(doc.tipoMime);
        const tamanio = formatearTamanio(doc.tamanioBytes);

        htmlGrid += `
            <div class="bg-white border border-gray-200 rounded-lg p-4 hover:shadow-lg transition-shadow text-center"
                 draggable="true"
                 ondragstart="handleDocumentDragStart(event, '${doc.id}')">
                <div class="text-5xl mb-3">${icono}</div>
                <p class="font-semibold text-sm text-gray-900 truncate mb-2">${doc.nombreOriginal}</p>
                <p class="text-xs text-gray-500 mb-3">${tamanio}</p>
                <div class="flex justify-center gap-2">
                    <button onclick="previsualizarDocumento('${doc.id}')" 
                            class="p-2 text-blue-600 hover:bg-blue-50 rounded transition-colors" title="Vista previa">
                        <i class="fas fa-eye"></i>
                    </button>
                    <button onclick="descargarDocumento('${doc.id}')" 
                            class="p-2 text-green-600 hover:bg-green-50 rounded transition-colors" title="Descargar">
                        <i class="fas fa-download"></i>
                    </button>
                    <button onclick="abrirModalMoverDocumento('${doc.id}', '${doc.nombreOriginal.replace(/'/g, "\\'")}')"
                            class="p-2 text-amber-600 hover:bg-amber-50 rounded transition-colors" title="Mover">
                        <i class="fas fa-folder-tree"></i>
                    </button>
                    <button onclick="eliminarDocumento('${doc.id}')" 
                            class="p-2 text-red-600 hover:bg-red-50 rounded transition-colors" title="Eliminar">
                        <i class="fas fa-trash"></i>
                    </button>
                </div>
            </div>
        `;
    });

    vistaGrid.innerHTML = htmlGrid;
}

// Mantener compatibilidad con código existente que llame a renderizarDocumentos
function renderizarDocumentos(documentos) {
    renderizarContenido([], documentos);
}

function renderizarPapeleraExplorador(documentos) {
    const vistaLista = document.getElementById('vista-lista');
    const vistaGrid = document.getElementById('vista-grid');

    if (documentos.length === 0) {
        const mensajeVacio = `
            <div class="text-center text-gray-400 py-12">
                <i class="fas fa-trash text-5xl mb-3"></i>
                <p class="text-lg font-semibold">Papelera vacia</p>
                <p class="text-sm mt-1">No hay documentos pendientes de recuperacion</p>
            </div>
        `;
        vistaLista.innerHTML = mensajeVacio;
        vistaGrid.innerHTML = mensajeVacio;
        return;
    }

    let htmlLista = '';
    let htmlGrid = '';

    documentos.forEach(doc => {
        const icono = obtenerIconoArchivo(doc.tipoMime);
        const tamanio = formatearTamanio(doc.tamanioBytes);
        const fechaEliminacion = doc.fechaEliminacion ? new Date(doc.fechaEliminacion).toLocaleDateString('es-MX') : 'Sin fecha';
        const fechaDefinitiva = doc.fechaEliminacionDefinitiva ? new Date(doc.fechaEliminacionDefinitiva).toLocaleDateString('es-MX') : '90 dias';

        htmlLista += `
            <div class="flex items-center justify-between p-3 bg-white border border-red-100 rounded-lg hover:shadow-md transition-shadow">
                <div class="flex items-center gap-3 flex-1">
                    <div class="text-3xl opacity-70">${icono}</div>
                    <div class="flex-1 min-w-0">
                        <p class="font-semibold text-sm text-gray-900 truncate">${doc.nombreOriginal}</p>
                        <p class="text-xs text-gray-500">${tamanio} - eliminado: ${fechaEliminacion} - definitivo: ${fechaDefinitiva}</p>
                    </div>
                </div>
                <div class="flex items-center gap-2">
                    <button onclick="recuperarDocumento('${doc.id}')"
                            class="p-2 text-green-700 hover:bg-green-50 rounded transition-colors" title="Recuperar">
                        <i class="fas fa-trash-restore"></i>
                    </button>
                </div>
            </div>
        `;

        htmlGrid += `
            <div class="bg-white border border-red-100 rounded-lg p-4 hover:shadow-lg transition-shadow text-center">
                <div class="text-5xl mb-3 opacity-70">${icono}</div>
                <p class="font-semibold text-sm text-gray-900 truncate mb-2">${doc.nombreOriginal}</p>
                <p class="text-xs text-gray-500 mb-1">${tamanio}</p>
                <p class="text-[11px] text-gray-500 mb-3">Definitivo: ${fechaDefinitiva}</p>
                <button onclick="recuperarDocumento('${doc.id}')"
                        class="px-3 py-2 text-sm text-green-700 hover:bg-green-50 rounded transition-colors" title="Recuperar">
                    <i class="fas fa-trash-restore mr-1"></i> Recuperar
                </button>
            </div>
        `;
    });

    vistaLista.innerHTML = htmlLista;
    vistaGrid.innerHTML = htmlGrid;
}

// ========================================
// SUBIR DOCUMENTO
// ========================================

function abrirModalSubirDocumento() {
    if (mostrandoPapelera) {
        return;
    }

    document.getElementById('carpeta-actual-id').value = carpetaActualId || '';
    document.getElementById('doc-file-gestor').value = '';
    document.getElementById('doc-descripcion-gestor').value = '';
    document.getElementById('archivos-seleccionados').classList.add('hidden');

    // Preseleccionar carpeta actual
    const select = document.getElementById('doc-carpeta-gestor');
    select.value = carpetaActualId || '';
    
    // Actualizar indicador visual
    actualizarIndicadorUbicacionDoc();
    select.onchange = actualizarIndicadorUbicacionDoc;
    
    // Listener para mostrar cantidad de archivos seleccionados
    document.getElementById('doc-file-gestor').onchange = function() {
        const container = document.getElementById('archivos-seleccionados');
        const contador = document.getElementById('contador-archivos');
        if (this.files.length > 0) {
            contador.textContent = this.files.length;
            container.classList.remove('hidden');
        } else {
            container.classList.add('hidden');
        }
    };

    const modal = document.getElementById('modal-subir-documento-gestor');
    modal.classList.remove('hidden');
    modal.classList.add('flex');
}

function actualizarIndicadorUbicacionDoc() {
    const select = document.getElementById('doc-carpeta-gestor');
    const indicador = document.getElementById('nombre-carpeta-destino-doc');
    
    if (!select.value) {
        indicador.textContent = numeroExpedienteGlobal || 'Raíz';
    } else {
        const nombreCarpeta = buscarNombreCarpeta(select.value, arbolCarpetasGlobal);
        indicador.textContent = nombreCarpeta || 'Carpeta seleccionada';
    }
}

function cerrarModalSubirDocumento() {
    const modal = document.getElementById('modal-subir-documento-gestor');
    modal.classList.add('hidden');
    modal.classList.remove('flex');
}

function mostrarModalDuplicado(nombreArchivo) {
    return new Promise((resolve) => {
        if (typeof Swal === 'undefined') {
            const confirmacion = confirm(`Ya existe un archivo con el nombre "${nombreArchivo}". ¿Deseas reemplazarlo?\n(Aceptar = Reemplazar, Cancelar = Crear una copia)`);
            resolve(confirmacion ? 'replace' : 'copy');
            return;
        }

        Swal.fire({
            title: 'Archivo duplicado',
            html: `
                <div class="text-left font-sans text-sm text-gray-700">
                    <p class="mb-2">Ya existe un archivo activo con el nombre <strong class="text-gob-guinda">${nombreArchivo}</strong> en esta ubicación.</p>
                    <p class="text-xs text-gray-500">Elige la acción que deseas realizar:</p>
                </div>
            `,
            icon: 'warning',
            showCancelButton: true,
            showDenyButton: true,
            confirmButtonText: '<i class="fas fa-exchange-alt mr-1"></i> Reemplazar',
            denyButtonText: '<i class="fas fa-copy mr-1"></i> Subir como copia',
            cancelButtonText: '<i class="fas fa-times mr-1"></i> Cancelar',
            confirmButtonColor: '#691228',
            denyButtonColor: '#B38E5D',
            cancelButtonColor: '#9ca3af',
            allowOutsideClick: false,
            allowEscapeKey: false,
            customClass: {
                popup: 'rounded-xl shadow-2xl'
            }
        }).then((result) => {
            if (result.isConfirmed) {
                resolve('replace');
            } else if (result.isDenied) {
                resolve('copy');
            } else {
                resolve('cancel');
            }
        });
    });
}

function generarNombreUnicoCopia(nombreOriginal, listaDocumentos) {
    const puntoIdx = nombreOriginal.lastIndexOf('.');
    let base = puntoIdx !== -1 ? nombreOriginal.substring(0, puntoIdx) : nombreOriginal;
    const ext = puntoIdx !== -1 ? nombreOriginal.substring(puntoIdx) : '';
    
    let contador = 1;
    let nuevoNombre = `${base} (${contador})${ext}`;
    
    while (listaDocumentos.some(doc => doc.nombreOriginal === nuevoNombre)) {
        contador++;
        nuevoNombre = `${base} (${contador})${ext}`;
    }
    return nuevoNombre;
}

function subirArchivoConProgreso(archivo, index, total, carpetaId, descripcion) {
    return new Promise((resolve, reject) => {
        const xhr = new XMLHttpRequest();
        const formData = new FormData();
        formData.append('file', archivo);
        if (carpetaId) {
            formData.append('carpetaId', carpetaId);
        }
        if (descripcion && total === 1) {
            formData.append('descripcion', descripcion);
        }

        xhr.upload.addEventListener('progress', (e) => {
            if (e.lengthComputable) {
                let percent = Math.round((e.loaded / e.total) * 100);
                if (percent >= 98) {
                    percent = 98; // Limitar visualmente a 98% mientras se procesa en backend
                }
                const progressBar = document.getElementById('upload-progress-bar');
                const progressText = document.getElementById('upload-progress-text');
                const progressTitle = document.getElementById('upload-progress-title');
                
                if (progressBar) progressBar.style.width = percent + '%';
                if (progressText) {
                    if (percent >= 98) {
                        progressText.innerText = "98% - Procesando y guardando en el servidor...";
                    } else {
                        progressText.innerText = `${percent}%`;
                    }
                }
                if (progressTitle) {
                    if (total > 1) {
                        progressTitle.innerText = `Subiendo archivo ${index + 1} de ${total}: ${archivo.name}`;
                    } else {
                        progressTitle.innerText = `Subiendo: ${archivo.name}`;
                    }
                }
            }
        });

        xhr.onload = () => {
            if (xhr.status >= 200 && xhr.status < 300) {
                const progressBar = document.getElementById('upload-progress-bar');
                const progressText = document.getElementById('upload-progress-text');
                if (progressBar) progressBar.style.width = '100%';
                if (progressText) progressText.innerText = '100% - ¡Completado!';

                // Esperar 400ms antes de cerrar y resolver para que el usuario pueda ver el "¡Completado!"
                setTimeout(() => {
                    try {
                        const res = JSON.parse(xhr.responseText);
                        resolve(res);
                    } catch (e) {
                        resolve({ success: true });
                    }
                }, 400);
            } else {
                try {
                    const err = JSON.parse(xhr.responseText);
                    reject(new Error(err.error || `Error del servidor (${xhr.status})`));
                } catch (e) {
                    reject(new Error(`Error del servidor (${xhr.status})`));
                }
            }
        };

        xhr.onerror = () => {
            console.error("Error en petición XHR de subida:", xhr);
            reject(new Error("Error de conexión a la red o el servidor rechazó el archivo."));
        };

        xhr.open('POST', `/api/documentos/expedientes/${expedienteIdGlobal}`);
        xhr.withCredentials = true; // Asegurar el envío de cookies en entornos híbridos o de desarrollo
        xhr.send(formData);
    });
}

async function subirDocumento() {
    const fileInput = document.getElementById('doc-file-gestor');
    const carpetaId = document.getElementById('doc-carpeta-gestor').value || null;
    const descripcion = document.getElementById('doc-descripcion-gestor').value;

    if (!fileInput.files || fileInput.files.length === 0) {
        if (typeof Swal !== 'undefined') {
            Swal.fire({
                icon: 'warning',
                title: 'Seleccione un archivo',
                text: 'Por favor seleccione al menos un archivo para subir.',
                confirmButtonColor: '#691228'
            });
        } else {
            alert('Por favor seleccione al menos un archivo');
        }
        return;
    }

    // Validar archivos del lado del cliente
    for (let i = 0; i < fileInput.files.length; i++) {
        if (window.validarArchivoCliente && !window.validarArchivoCliente(fileInput.files[i])) {
            return;
        }
    }

    const btn = document.getElementById('btn-subir-documento-gestor');
    btn.disabled = true;

    // Verificar duplicados y preguntar qué hacer (Opción A)
    const archivosASubir = [];
    let docsTemporales = [...documentosActualesGlobal];

    for (let i = 0; i < fileInput.files.length; i++) {
        const archivo = fileInput.files[i];
        const existingDoc = docsTemporales.find(doc => doc.nombreOriginal === archivo.name);

        if (existingDoc) {
            const accion = await mostrarModalDuplicado(archivo.name);
            if (accion === 'replace') {
                archivosASubir.push({ file: archivo, replaceId: existingDoc.id });
            } else if (accion === 'copy') {
                const nuevoNombre = generarNombreUnicoCopia(archivo.name, docsTemporales);
                const archivoCopia = new File([archivo], nuevoNombre, { type: archivo.type });
                docsTemporales.push({ nombreOriginal: nuevoNombre });
                archivosASubir.push({ file: archivoCopia, replaceId: null });
            } else {
                // Cancelar/omitir este archivo
                continue;
            }
        } else {
            archivosASubir.push({ file: archivo, replaceId: null });
        }
    }

    if (archivosASubir.length === 0) {
        btn.disabled = false;
        return;
    }
    
    let archivosSubidos = 0;
    let errores = [];

    // Mostrar modal de progreso
    if (typeof Swal !== 'undefined') {
        Swal.fire({
            title: 'Subiendo archivos',
            html: `
                <div class="font-sans text-sm mb-3 text-gray-700 text-left" id="upload-progress-title">Preparando subida...</div>
                <div class="w-full bg-gray-200 rounded-full h-4 mb-2 overflow-hidden">
                    <div id="upload-progress-bar" class="h-4 rounded-full transition-all duration-300" style="width: 0%; background-color: #691228;"></div>
                </div>
                <div id="upload-progress-text" class="text-xs text-gray-500 font-bold text-center">0%</div>
            `,
            position: 'bottom-end',
            backdrop: false,
            showConfirmButton: false,
            allowOutsideClick: false,
            allowEscapeKey: false,
            allowEnterKey: false,
            showClass: {
                popup: 'swal-slide-in-right'
            },
            hideClass: {
                popup: 'swal-slide-out-right'
            }
        });
    }

    try {
        for (let i = 0; i < archivosASubir.length; i++) {
            const item = archivosASubir[i];
            btn.innerHTML = `<i class="fas fa-spinner fa-spin mr-1"></i> Subiendo ${i + 1} de ${archivosASubir.length}...`;
            
            // Si el usuario eligió reemplazar, eliminar lógicamente el archivo previo primero
            if (item.replaceId) {
                try {
                    const deleteResponse = await fetch(`/api/documentos/${item.replaceId}`, {
                        method: 'DELETE'
                    });
                    if (!deleteResponse.ok) {
                        console.warn("No se pudo mover a la papelera el archivo a reemplazar:", item.file.name);
                    }
                } catch (e) {
                    console.error("Error al reemplazar archivo duplicado:", e);
                }
            }

            try {
                await subirArchivoConProgreso(item.file, i, archivosASubir.length, carpetaId, descripcion);
                archivosSubidos++;
            } catch (err) {
                errores.push(`${item.file.name}: ${err.message}`);
            }
        }

        // Cerrar modal de progreso
        if (typeof Swal !== 'undefined') {
            Swal.close();
        }

        // Cerrar modal de formulario
        cerrarModalSubirDocumento();

        // Recargar documentos y espacio
        await cargarDocumentos();
        await cargarEspacioOcupado();
        await cargarArbolCarpetas(); // Actualizar contadores
        if (typeof actualizarBadgePapelera === 'function') {
            actualizarBadgePapelera();
        }

        // Mostrar resultado
        if (errores.length === 0) {
            mostrarNotificacion(`${archivosSubidos} documento(s) subido(s) correctamente`, 'success');
        } else if (archivosSubidos > 0) {
            mostrarNotificacion(`${archivosSubidos} subido(s), ${errores.length} error(es)`, 'warning');
            console.error('Errores al subir:', errores);
        } else {
            if (typeof Swal !== 'undefined') {
                Swal.fire({
                    icon: 'error',
                    title: 'Error al subir los documentos',
                    html: `<div class="text-left font-sans text-sm"><p class="mb-2">No se pudieron subir los siguientes archivos:</p><ul class="list-disc pl-5 space-y-1 text-red-600">${errores.map(e => `<li>${e}</li>`).join('')}</ul></div>`,
                    confirmButtonColor: '#691228'
                });
            } else {
                alert('Error al subir los documentos:\n' + errores.join('\n'));
            }
        }

    } catch (error) {
        console.error('Error:', error);
        if (typeof Swal !== 'undefined') {
            Swal.close();
            Swal.fire({
                icon: 'error',
                title: 'Error inesperado',
                text: error.message,
                confirmButtonColor: '#691228'
            });
        } else {
            alert(error.message);
        }
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="fas fa-upload mr-1"></i> Subir Ahora';
    }
}

let documentoIdAEliminar = null;

function eliminarDocumento(documentoId) {
    documentoIdAEliminar = documentoId;
    const modal = document.getElementById('modal-confirmar-eliminar-documento');
    if (modal) {
        modal.classList.remove('hidden');
        modal.classList.add('flex');
    }
}

function cerrarConfirmarEliminarDoc() {
    documentoIdAEliminar = null;
    const modal = document.getElementById('modal-confirmar-eliminar-documento');
    if (modal) {
        modal.classList.add('hidden');
        modal.classList.remove('flex');
    }
}

async function confirmarEliminarDoc() {
    if (!documentoIdAEliminar) return;
    const btn = document.getElementById('btn-confirmar-eliminar-doc');
    if (btn) {
        btn.disabled = true;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin mr-1"></i> Moviendo...';
    }

    try {
        const response = await fetch(`/api/documentos/${documentoIdAEliminar}`, {
            method: 'DELETE'
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Error al eliminar documento');
        }

        cerrarConfirmarEliminarDoc();

        // Recargar documentos y espacio
        await cargarDocumentos();
        await cargarEspacioOcupado();
        await cargarArbolCarpetas(); // Actualizar contadores

        mostrarNotificacion('Documento enviado a la papelera. Se eliminará definitivamente en 90 días.', 'warning');
        actualizarBadgePapelera();

    } catch (error) {
        console.error('Error:', error);
        alert(error.message);
        cerrarConfirmarEliminarDoc();
    } finally {
        if (btn) {
            btn.disabled = false;
            btn.innerHTML = '<i class="fas fa-trash mr-1"></i> Mover';
        }
    }
}

async function recuperarDocumento(documentoId) {
    try {
        const response = await fetch(`/api/documentos/${documentoId}/recuperar`, {
            method: 'PUT'
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Error al recuperar documento');
        }

        await cargarDocumentos();
        await cargarEspacioOcupado();
        await cargarArbolCarpetas();

        mostrarNotificacion('Documento recuperado correctamente', 'success');

    } catch (error) {
        console.error('Error:', error);
        alert(error.message);
    }
}

function descargarDocumento(documentoId) {
    window.open(`/api/documentos/${documentoId}/download`, '_blank');
}

function previsualizarDocumento(documentoId) {
    window.open(`/api/documentos/${documentoId}/preview`, '_blank');
}

// ========================================
// DRAG & DROP (CONTADOR DE ARRASTRE)
// ========================================

let dragCounter = 0;

function isLocalFileDrag(event) {
    if (event.dataTransfer && event.dataTransfer.types) {
        for (let i = 0; i < event.dataTransfer.types.length; i++) {
            const type = event.dataTransfer.types[i].toLowerCase();
            if (type === 'files' || type === 'application/x-moz-file') {
                return true;
            }
        }
    }
    return false;
}

function handleDragEnter(event) {
    if (mostrandoPapelera || !isLocalFileDrag(event)) {
        return;
    }

    event.preventDefault();
    event.stopPropagation();
    
    dragCounter++;
    if (dragCounter === 1) {
        const overlay = document.getElementById('drop-zone-overlay');
        const carpetaLabel = document.getElementById('drop-zone-carpeta');
        
        // Mostrar overlay
        if (overlay) {
            overlay.classList.remove('hidden');
            overlay.classList.add('flex');
        }
        
        // Actualizar texto con carpeta actual
        if (carpetaLabel) {
            if (carpetaActualId) {
                const nombreCarpeta = buscarNombreCarpeta(carpetaActualId, arbolCarpetasGlobal);
                carpetaLabel.textContent = `Se subirán a: ${nombreCarpeta}`;
            } else {
                carpetaLabel.textContent = `Se subirán a: ${numeroExpedienteGlobal || 'Raíz'}`;
            }
        }
    }
}

function handleDragOver(event) {
    if (mostrandoPapelera || !isLocalFileDrag(event)) {
        return;
    }
    event.preventDefault();
    event.stopPropagation();
}

function handleDragLeave(event) {
    if (mostrandoPapelera || !isLocalFileDrag(event)) {
        return;
    }
    event.preventDefault();
    event.stopPropagation();
    
    dragCounter--;
    if (dragCounter <= 0) {
        dragCounter = 0;
        const overlay = document.getElementById('drop-zone-overlay');
        if (overlay) {
            overlay.classList.add('hidden');
            overlay.classList.remove('flex');
        }
    }
}

async function handleDrop(event) {
    if (mostrandoPapelera || !isLocalFileDrag(event)) {
        return;
    }

    event.preventDefault();
    event.stopPropagation();
    
    dragCounter = 0;
    const overlay = document.getElementById('drop-zone-overlay');
    if (overlay) {
        overlay.classList.add('hidden');
        overlay.classList.remove('flex');
    }
    
    const files = event.dataTransfer.files;
    if (!files || files.length === 0) return;
    
    // Subir archivos directamente a la carpeta actual
    await subirArchivosDirecto(files, carpetaActualId);
}


async function subirArchivosDirecto(files, carpetaId) {
    const totalArchivos = files.length;
    let archivosSubidos = 0;
    let errores = [];
    
    // Validar archivos antes de subirlos
    const archivosValidos = [];
    for (let i = 0; i < totalArchivos; i++) {
        const archivo = files[i];
        if (window.validarArchivoCliente && !window.validarArchivoCliente(archivo)) {
            continue;
        }
        archivosValidos.push(archivo);
    }
    
    if (archivosValidos.length === 0) {
        return;
    }
    
    // Verificar duplicados y preguntar qué hacer (Opción A)
    const archivosASubir = [];
    let docsTemporales = [...documentosActualesGlobal];

    for (let i = 0; i < archivosValidos.length; i++) {
        const archivo = archivosValidos[i];
        const existingDoc = docsTemporales.find(doc => doc.nombreOriginal === archivo.name);

        if (existingDoc) {
            const accion = await mostrarModalDuplicado(archivo.name);
            if (accion === 'replace') {
                archivosASubir.push({ file: archivo, replaceId: existingDoc.id });
            } else if (accion === 'copy') {
                const nuevoNombre = generarNombreUnicoCopia(archivo.name, docsTemporales);
                const archivoCopia = new File([archivo], nuevoNombre, { type: archivo.type });
                docsTemporales.push({ nombreOriginal: nuevoNombre });
                archivosASubir.push({ file: archivoCopia, replaceId: null });
            } else {
                // Cancelar/omitir este archivo
                continue;
            }
        } else {
            archivosASubir.push({ file: archivo, replaceId: null });
        }
    }

    if (archivosASubir.length === 0) {
        return;
    }

    // Mostrar modal de progreso
    if (typeof Swal !== 'undefined') {
        Swal.fire({
            title: 'Subiendo archivos',
            html: `
                <div class="font-sans text-sm mb-3 text-gray-700 text-left" id="upload-progress-title">Preparando subida...</div>
                <div class="w-full bg-gray-200 rounded-full h-4 mb-2 overflow-hidden">
                    <div id="upload-progress-bar" class="h-4 rounded-full transition-all duration-300" style="width: 0%; background-color: #691228;"></div>
                </div>
                <div id="upload-progress-text" class="text-xs text-gray-500 font-bold text-center">0%</div>
            `,
            position: 'bottom-end',
            backdrop: false,
            showConfirmButton: false,
            allowOutsideClick: false,
            allowEscapeKey: false,
            allowEnterKey: false,
            showClass: {
                popup: 'swal-slide-in-right'
            },
            hideClass: {
                popup: 'swal-slide-out-right'
            }
        });
    } else {
        mostrarNotificacion(`Subiendo ${archivosASubir.length} archivo(s)...`, 'info');
    }
    
    for (let i = 0; i < archivosASubir.length; i++) {
        const item = archivosASubir[i];
        
        // Si el usuario eligió reemplazar, eliminar lógicamente el archivo previo primero
        if (item.replaceId) {
            try {
                const deleteResponse = await fetch(`/api/documentos/${item.replaceId}`, {
                    method: 'DELETE'
                });
                if (!deleteResponse.ok) {
                    console.warn("No se pudo mover a la papelera el archivo a reemplazar:", item.file.name);
                }
            } catch (e) {
                console.error("Error al reemplazar archivo duplicado:", e);
            }
        }

        try {
            await subirArchivoConProgreso(item.file, i, archivosASubir.length, carpetaId, null);
            archivosSubidos++;
        } catch (err) {
            errores.push(`${item.file.name}: ${err.message}`);
        }
    }
    
    // Cerrar modal de progreso
    if (typeof Swal !== 'undefined') {
        Swal.close();
    }

    // Recargar datos
    await cargarDocumentos();
    await cargarEspacioOcupado();
    await cargarArbolCarpetas();
    if (typeof actualizarBadgePapelera === 'function') {
        actualizarBadgePapelera();
    }
    
    // Mostrar resultado
    if (errores.length === 0) {
        mostrarNotificacion(`${archivosSubidos} documento(s) subido(s) correctamente`, 'success');
    } else if (archivosSubidos > 0) {
        mostrarNotificacion(`${archivosSubidos} subido(s), ${errores.length} error(es)`, 'warning');
    } else {
        mostrarNotificacion('Error al subir los documentos', 'error');
    }
}

// ========================================
// MOVER DOCUMENTO
// ========================================

function abrirModalMoverDocumento(documentoId, nombreDocumento) {
    document.getElementById('documento-id-mover').value = documentoId;
    document.getElementById('nombre-documento-mover').textContent = nombreDocumento;
    
    // Llenar select de carpetas destino
    const select = document.getElementById('carpeta-destino-mover');
    select.innerHTML = `<option value="">${numeroExpedienteGlobal} (raíz)</option>`;
    agregarOpcionesCarpetas(select, arbolCarpetasGlobal, 0);
    
    // Actualizar indicador
    actualizarIndicadorMover();
    select.onchange = actualizarIndicadorMover;
    
    const modal = document.getElementById('modal-mover-documento');
    modal.classList.remove('hidden');
    modal.classList.add('flex');
}

function actualizarIndicadorMover() {
    const select = document.getElementById('carpeta-destino-mover');
    const indicador = document.getElementById('nombre-carpeta-destino-mover');
    
    if (!select.value) {
        indicador.textContent = numeroExpedienteGlobal || 'Raíz';
    } else {
        const nombreCarpeta = buscarNombreCarpeta(select.value, arbolCarpetasGlobal);
        indicador.textContent = nombreCarpeta || 'Carpeta seleccionada';
    }
}

function cerrarModalMoverDocumento() {
    const modal = document.getElementById('modal-mover-documento');
    modal.classList.add('hidden');
    modal.classList.remove('flex');
}

async function moverDocumento() {
    const documentoId = document.getElementById('documento-id-mover').value;
    const carpetaDestinoId = document.getElementById('carpeta-destino-mover').value || null;
    
    const btn = document.getElementById('btn-mover-documento');
    btn.disabled = true;
    btn.innerHTML = '<i class="fas fa-spinner fa-spin mr-1"></i> Moviendo...';
    
    try {
        let url = `/api/documentos/${documentoId}/mover`;
        if (carpetaDestinoId) {
            url += `?carpetaId=${carpetaDestinoId}`;
        }
        
        const response = await fetch(url, {
            method: 'PUT'
        });
        
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Error al mover documento');
        }
        
        // Cerrar modal
        cerrarModalMoverDocumento();
        
        // Recargar documentos
        await cargarDocumentos();
        await cargarArbolCarpetas();
        
        mostrarNotificacion('Documento movido correctamente', 'success');
        
    } catch (error) {
        console.error('Error:', error);
        alert(error.message);
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="fas fa-folder-tree mr-1"></i> Mover';
    }
}

// ========================================
// ESPACIO OCUPADO
// ========================================

async function cargarEspacioOcupado() {
    try {
        const response = await fetch(`/api/documentos/expedientes/${expedienteIdGlobal}/storage-info`);

        if (!response.ok) {
            throw new Error('Error al cargar espacio');
        }

        const data = await response.json();
        
        // Formatear el tamaño de manera legible
        const bytes = data.espacioBytes || 0;
        let espacioTexto;
        
        if (bytes >= 1024 * 1024 * 1024) {
            espacioTexto = (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB';
        } else if (bytes >= 1024 * 1024) {
            espacioTexto = (bytes / (1024 * 1024)).toFixed(2) + ' MB';
        } else if (bytes >= 1024) {
            espacioTexto = (bytes / 1024).toFixed(2) + ' KB';
        } else {
            espacioTexto = bytes + ' Bytes';
        }

        // Actualizar texto
        const textoEspacio = document.getElementById('espacio-texto');
        if (textoEspacio) {
            textoEspacio.textContent = espacioTexto;
        }

    } catch (error) {
        console.error('Error:', error);
        const textoEspacio = document.getElementById('espacio-texto');
        if (textoEspacio) {
            textoEspacio.textContent = '0 Bytes';
        }
    }
}

// ========================================
// VISTAS
// ========================================

function cambiarVista(vista) {
    vistaActual = vista;

    const vistaLista = document.getElementById('vista-lista');
    const vistaGrid = document.getElementById('vista-grid');
    const btnList = document.getElementById('btn-vista-list');
    const btnGrid = document.getElementById('btn-vista-grid');

    if (vista === 'list') {
        vistaLista.classList.remove('hidden');
        vistaGrid.classList.add('hidden');
        btnList.classList.add('text-gob-guinda');
        btnList.classList.remove('text-gray-600');
        btnGrid.classList.remove('text-gob-guinda');
        btnGrid.classList.add('text-gray-600');
    } else {
        vistaLista.classList.add('hidden');
        vistaGrid.classList.remove('hidden');
        vistaGrid.classList.add('grid');
        btnGrid.classList.add('text-gob-guinda');
        btnGrid.classList.remove('text-gray-600');
        btnList.classList.remove('text-gob-guinda');
        btnList.classList.add('text-gray-600');
    }
}

// ========================================
// TIEMPO REAL (WEBSOCKET)
// ========================================

let stompClientDocumentos = null;

function iniciarWebSocketDocumentos() {
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
        if (stompClientDocumentos) return;

        const socket = new SockJS('/ws-realtime');
        stompClientDocumentos = Stomp.over(socket);
        stompClientDocumentos.debug = null; // Desactivar logs ruidosos en la consola

        stompClientDocumentos.connect({}, function (frame) {
            //console.log('🔔 Gestor Documental: Conexión WebSocket establecida de forma permanente.');
            
            const dest = '/topic/expedientes/' + expedienteIdGlobal;
            stompClientDocumentos.subscribe(dest, function (message) {
                try {
                    const payload = JSON.parse(message.body);
                    
                    if (payload.tabla === 'documentos' || payload.tabla === 'carpetas') {
                       // console.log(`🔔 Cambio detectado en la tabla '${payload.tabla}' (${payload.operacion}). Actualizando gestor documental...`);
                        cargarArbolCarpetas();
                        cargarEspacioOcupado();
                        cargarDocumentos();
                        if (typeof cargarArchivosRecientes === 'function') {
                            cargarArchivosRecientes();
                        }
                    }
                } catch (e) {
                    console.error("Error al procesar mensaje de WebSocket en el Gestor Documental:", e);
                }
            });
        }, function (error) {
            //console.warn('⚠️ Gestor Documental: Conexión WebSocket perdida. Reintentando en 5 segundos...', error);
            stompClientDocumentos = null;
            setTimeout(iniciarWebSocketDocumentos, 5000);
        });
    });
}

// ========================================
// BÚSQUEDA E INICIALIZACIÓN
// ========================================

document.addEventListener('DOMContentLoaded', function () {
    const searchInput = document.getElementById('search-documentos-gestor');
    if (searchInput) {
        searchInput.addEventListener('input', debounce(buscarDocumentos, 300));
    }

    // Detectar si estamos en la vista de detalle y obtener el expediente actual
    const btnGestor = document.querySelector('[data-expediente-id]');
    if (btnGestor) {
        expedienteIdGlobal = btnGestor.dataset.expedienteId;
        numeroExpedienteGlobal = btnGestor.dataset.expedienteNumero;
        
        // Conectar el canal de tiempo real para documentos/carpetas
        iniciarWebSocketDocumentos();
    }

    // Prevenir comportamiento por defecto de arrastrar y soltar en la ventana
    window.addEventListener("dragover", function (e) {
        e.preventDefault();
    }, false);
    window.addEventListener("drop", function (e) {
        e.preventDefault();
    }, false);
});

async function buscarDocumentos() {
    if (mostrandoPapelera) {
        return;
    }

    const busqueda = document.getElementById('search-documentos-gestor').value.trim();

    if (busqueda.length < 2) {
        cargarDocumentos();
        return;
    }

    try {
        let url;
        if (carpetaActualId) {
            // Buscar en carpeta específica (no implementado en backend aún)
            url = `/api/documentos/expedientes/${expedienteIdGlobal}/buscar?q=${encodeURIComponent(busqueda)}`;
        } else {
            url = `/api/documentos/expedientes/${expedienteIdGlobal}/buscar?q=${encodeURIComponent(busqueda)}`;
        }

        const response = await fetch(url);

        if (!response.ok) {
            throw new Error('Error en búsqueda');
        }

        const documentos = await response.json();
        renderizarDocumentos(documentos);

    } catch (error) {
        console.error('Error:', error);
    }
}

// ========================================
// UTILIDADES
// ========================================

function obtenerIconoArchivo(tipoMime) {
    if (!tipoMime) return '📄';

    if (tipoMime.includes('pdf')) return '📕';
    if (tipoMime.includes('word') || tipoMime.includes('document')) return '📘';
    if (tipoMime.includes('excel') || tipoMime.includes('spreadsheet')) return '📗';
    if (tipoMime.includes('powerpoint') || tipoMime.includes('presentation')) return '📙';
    if (tipoMime.includes('image')) return '🖼️';
    if (tipoMime.includes('video')) return '🎬';
    if (tipoMime.includes('audio')) return '🎵';
    if (tipoMime.includes('zip') || tipoMime.includes('rar') || tipoMime.includes('compressed')) return '📦';

    return '📄';
}

function formatearTamanio(bytes) {
    if (bytes === 0) return '0 Bytes';

    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));

    return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i];
}

function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

function mostrarNotificacion(mensaje, tipo = 'success') {
    if (typeof Swal !== 'undefined') {
        const Toast = Swal.mixin({
            toast: true,
            position: 'top-end',
            showConfirmButton: false,
            timer: 3000,
            timerProgressBar: true,
            didOpen: (toast) => {
                toast.addEventListener('mouseenter', Swal.stopTimer)
                toast.addEventListener('mouseleave', Swal.resumeTimer)
            }
        });
        Toast.fire({
            icon: tipo,
            title: mensaje
        });
    } else {
        console.log(`[${tipo.toUpperCase()}] ${mensaje}`);
    }
}

// ========================================
// PAPELERA TEMPORAL
// ========================================

let documentoIdEliminarPerm = null;

/**
 * Abre el modal de papelera y carga los documentos eliminados.
 */
async function abrirPapelera() {
    const modal = document.getElementById('modal-papelera');
    modal.classList.remove('hidden');
    modal.classList.add('flex');
    await cargarPapelera();
}

function cerrarPapelera() {
    const modal = document.getElementById('modal-papelera');
    modal.classList.add('hidden');
    modal.classList.remove('flex');
}

/**
 * Carga y renderiza los documentos en la papelera del expediente actual.
 */
async function cargarPapelera() {
    const contenido = document.getElementById('papelera-contenido');
    contenido.innerHTML = `
        <div class="text-center text-gray-400 py-12">
            <i class="fas fa-spinner fa-spin text-3xl mb-2"></i>
            <p>Cargando papelera...</p>
        </div>
    `;

    try {
        const response = await fetch(`/api/documentos/expedientes/${expedienteIdGlobal}/papelera`);

        if (!response.ok) {
            throw new Error('Error al cargar la papelera');
        }

        const documentos = await response.json();
        renderizarPapeleraModal(documentos);
        actualizarBadgePapelera(documentos.length);

    } catch (error) {
        console.error('Error:', error);
        contenido.innerHTML = `
            <div class="text-center text-red-500 py-12">
                <i class="fas fa-exclamation-triangle text-3xl mb-2"></i>
                <p>Error al cargar la papelera</p>
            </div>
        `;
    }
}

/**
 * Renderiza la lista de documentos en la papelera.
 */
function renderizarPapeleraModal(documentos) {
    const contenido = document.getElementById('papelera-contenido');
    const btnVaciar = document.getElementById('btn-vaciar-papelera');

    if (!documentos || documentos.length === 0) {
        if (btnVaciar) {
            btnVaciar.disabled = true;
            btnVaciar.classList.add('opacity-40', 'cursor-not-allowed');
        }
        contenido.innerHTML = `
            <div class="text-center text-gray-400 py-12">
                <i class="fas fa-trash-alt text-5xl mb-3 opacity-30"></i>
                <p class="text-lg font-semibold">La papelera está vacía</p>
                <p class="text-sm mt-1">Los documentos eliminados aparecerán aquí por 90 días.</p>
            </div>
        `;
        return;
    }

    if (btnVaciar) {
        btnVaciar.disabled = false;
        btnVaciar.classList.remove('opacity-40', 'cursor-not-allowed');
    }

    let html = `<div class="space-y-2">`;

    documentos.forEach(doc => {
        const icono = obtenerIconoArchivo(doc.tipoMime);
        const tamanio = formatearTamanio(doc.tamanioBytes);
        const fechaEliminado = doc.fechaEliminacion
            ? new Date(doc.fechaEliminacion).toLocaleDateString('es-MX', { year: 'numeric', month: 'short', day: 'numeric' })
            : '---';

        // Calcular días restantes antes de eliminación permanente
        let diasRestantes = '---';
        let colorDias = 'text-gray-500';
        if (doc.fechaEliminacionDefinitiva) {
            const fechaExpiracion = new Date(doc.fechaEliminacionDefinitiva);
            const hoy = new Date();
            const diff = Math.ceil((fechaExpiracion - hoy) / (1000 * 60 * 60 * 24));
            diasRestantes = diff > 0 ? diff + ' días' : 'Vencido';
            if (diff <= 7) colorDias = 'text-red-600 font-semibold';
            else if (diff <= 30) colorDias = 'text-amber-600';
            else colorDias = 'text-gray-500';
        }

        // Determinar info de carpeta original
        let carpetaInfo = 'Raíz del expediente';
        if (doc.carpetaOriginalId) {
            const carpeta = encontrarCarpeta(arbolCarpetasGlobal, doc.carpetaOriginalId);
            if (carpeta) {
                carpetaInfo = '📁 ' + carpeta.nombre;
            } else {
                carpetaInfo = '⚠️ Carpeta original eliminada (se restaurará en raíz)';
            }
        }

        const nombreEscapado = (doc.nombreOriginal || '').replace(/'/g, "\\'");

        html += `
            <div class="flex items-center justify-between p-3 bg-red-50 border border-red-200 rounded-lg hover:bg-red-100 transition-colors">
                <div class="flex items-center gap-3 flex-1 min-w-0">
                    <div class="text-3xl opacity-70">${icono}</div>
                    <div class="flex-1 min-w-0">
                        <p class="font-semibold text-sm text-gray-800 truncate">${doc.nombreOriginal}</p>
                        <p class="text-xs text-gray-500">${tamanio} • Eliminado el ${fechaEliminado}</p>
                        <p class="text-xs text-gray-400 truncate">${carpetaInfo}</p>
                    </div>
                </div>
                <div class="flex items-center gap-2 ml-3 shrink-0">
                    <span class="text-xs ${colorDias} whitespace-nowrap">
                        <i class="fas fa-clock mr-1"></i>${diasRestantes}
                    </span>
                    <button onclick="restaurarDocumentoDesde('${doc.id}')"
                            class="p-2 text-green-600 hover:bg-green-100 rounded transition-colors"
                            title="Restaurar documento">
                        <i class="fas fa-undo"></i>
                    </button>
                    <button onclick="solicitarEliminarPermanente('${doc.id}', '${nombreEscapado}')"
                            class="p-2 text-red-600 hover:bg-red-200 rounded transition-colors"
                            title="Eliminar permanentemente">
                        <i class="fas fa-times-circle"></i>
                    </button>
                </div>
            </div>
        `;
    });

    html += `</div>`;
    contenido.innerHTML = html;
}

/**
 * Actualiza el badge numérico del botón de papelera.
 * Si no se pasa cantidad, consulta al servidor.
 */
async function actualizarBadgePapelera(cantidad) {
    const badge = document.getElementById('papelera-badge');
    if (!badge || !expedienteIdGlobal) return;

    let total = cantidad;
    if (total === undefined) {
        try {
            const response = await fetch(`/api/documentos/expedientes/${expedienteIdGlobal}/papelera`);
            if (response.ok) {
                const docs = await response.json();
                total = docs.length;
            } else {
                return;
            }
        } catch {
            return;
        }
    }

    if (total > 0) {
        badge.textContent = total > 99 ? '99+' : total;
        badge.classList.remove('hidden');
        badge.classList.add('flex');
    } else {
        badge.classList.add('hidden');
        badge.classList.remove('flex');
    }
}

/**
 * Restaura un documento desde la papelera a su ubicación original.
 */
async function restaurarDocumentoDesde(documentoId) {
    try {
        const response = await fetch(`/api/documentos/${documentoId}/restaurar`, {
            method: 'PUT'
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Error al restaurar el documento');
        }

        // Recargar papelera y vista principal
        await cargarPapelera();
        await cargarDocumentos();
        await cargarArbolCarpetas();
        await cargarEspacioOcupado();

        mostrarNotificacion('Documento restaurado correctamente', 'success');

    } catch (error) {
        console.error('Error:', error);
        alert(error.message);
    }
}

/**
 * Muestra el diálogo de confirmación para eliminar permanentemente.
 */
function solicitarEliminarPermanente(documentoId, nombreDocumento) {
    documentoIdEliminarPerm = documentoId;
    document.getElementById('nombre-doc-eliminar-perm').textContent = nombreDocumento;

    const modal = document.getElementById('modal-confirmar-eliminar-permanente');
    modal.classList.remove('hidden');
    modal.classList.add('flex');
}

function cerrarConfirmarEliminarPerm() {
    documentoIdEliminarPerm = null;
    const modal = document.getElementById('modal-confirmar-eliminar-permanente');
    modal.classList.add('hidden');
    modal.classList.remove('flex');
}

/**
 * Ejecuta la eliminación permanente tras confirmación del usuario.
 */
async function confirmarEliminarPermanente() {
    if (!documentoIdEliminarPerm) return;

    const btn = document.getElementById('btn-confirmar-eliminar-perm');
    btn.disabled = true;
    btn.innerHTML = '<i class="fas fa-spinner fa-spin mr-1"></i> Eliminando...';

    try {
        const response = await fetch(`/api/documentos/${documentoIdEliminarPerm}/permanente`, {
            method: 'DELETE'
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Error al eliminar permanentemente');
        }

        cerrarConfirmarEliminarPerm();
        await cargarPapelera();
        await cargarEspacioOcupado();

        mostrarNotificacion('Documento eliminado permanentemente', 'success');

    } catch (error) {
        console.error('Error:', error);
        alert(error.message);
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="fas fa-trash mr-1"></i> Eliminar';
    }
}

/**
 * Solicita confirmación para vaciar la papelera de este expediente.
 */
function vaciarPapeleraConfirmacion() {
    if (typeof Swal !== 'undefined') {
        Swal.fire({
            title: '¿Vaciar la papelera?',
            text: 'Todos los documentos en la papelera serán eliminados permanentemente y de manera física. Esta acción no se puede deshacer.',
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#d33',
            cancelButtonColor: '#3085d6',
            confirmButtonText: 'Sí, vaciar papelera',
            cancelButtonText: 'Cancelar'
        }).then(async (result) => {
            if (result.isConfirmed) {
                await ejecutarVaciarPapelera();
            }
        });
    } else {
        if (confirm('¿Estás seguro de que deseas vaciar permanentemente todos los documentos de la papelera?')) {
            ejecutarVaciarPapelera();
        }
    }
}

/**
 * Ejecuta el borrado masivo de la papelera mediante petición DELETE.
 */
async function ejecutarVaciarPapelera() {
    const btnVaciar = document.getElementById('btn-vaciar-papelera');
    if (btnVaciar) {
        btnVaciar.disabled = true;
        btnVaciar.innerHTML = '<i class="fas fa-spinner fa-spin mr-1.5"></i> Vaciando...';
    }

    try {
        const response = await fetch(`/api/documentos/expedientes/${expedienteIdGlobal}/papelera`, {
            method: 'DELETE'
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Error al vaciar la papelera');
        }

        await cargarPapelera();
        await cargarEspacioOcupado();
        
        mostrarNotificacion('La papelera ha sido vaciada correctamente', 'success');

    } catch (error) {
        console.error('Error:', error);
        if (typeof Swal !== 'undefined') {
            Swal.fire({
                icon: 'error',
                title: 'Error',
                text: error.message,
                confirmButtonColor: '#691228'
            });
        } else {
            alert(error.message);
        }
    } finally {
        if (btnVaciar) {
            btnVaciar.disabled = false;
            btnVaciar.innerHTML = '<i class="fas fa-trash-alt mr-1.5"></i> Vaciar papelera';
        }
    }
}

// ========================================
// FUNCIONES AUXILIARES DE CARGA (SWEETALERT)
// ========================================

function mostrarCargando(mensaje = "Cargando...") {
    if (typeof Swal !== 'undefined') {
        Swal.fire({
            title: mensaje,
            allowOutsideClick: false,
            allowEscapeKey: false,
            allowEnterKey: false,
            didOpen: () => {
                Swal.showLoading();
            }
        });
    }
}

function ocultarCargando() {
    if (typeof Swal !== 'undefined') {
        Swal.close();
    }
}

// ========================================
// FUNCIONES DE DRAG & DROP INTERNO
// ========================================

function handleFolderDragStart(event, folderId) {
    if (mostrandoPapelera) return;
    event.dataTransfer.setData("text/plain", JSON.stringify({ type: 'folder', id: folderId }));
    event.dataTransfer.effectAllowed = "move";
    
    // Crear imagen de arrastre mini personalizada
    const target = event.currentTarget;
    let folderName = "Carpeta";
    const labelEl = target.querySelector('.explorer-label') || target.querySelector('.font-semibold');
    if (labelEl) folderName = labelEl.textContent.trim();
    
    const dragImage = document.createElement('div');
    dragImage.className = 'bg-white border border-amber-200 rounded-lg shadow-lg p-2 flex items-center gap-2 pointer-events-none text-xs font-semibold text-gray-800';
    dragImage.style.position = 'absolute';
    dragImage.style.top = '-1000px';
    dragImage.style.left = '-1000px';
    dragImage.style.borderLeft = '4px solid #f59e0b';
    dragImage.innerHTML = `<span>📁</span> <span style="max-width: 120px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">${folderName}</span>`;
    document.body.appendChild(dragImage);
    event.dataTransfer.setDragImage(dragImage, 15, 15);
    setTimeout(() => dragImage.remove(), 0);
}

function handleDocumentDragStart(event, docId) {
    if (mostrandoPapelera) return;
    event.dataTransfer.setData("text/plain", JSON.stringify({ type: 'document', id: docId }));
    event.dataTransfer.effectAllowed = "move";
    
    // Crear imagen de arrastre mini personalizada
    const target = event.currentTarget;
    let docName = "Documento";
    let icon = "📄";
    const titleEl = target.querySelector('.font-semibold');
    if (titleEl) docName = titleEl.textContent.trim();
    
    const iconEl = target.querySelector('.text-3xl') || target.querySelector('.text-5xl');
    if (iconEl) icon = iconEl.textContent.trim() || "📄";
    
    const dragImage = document.createElement('div');
    dragImage.className = 'bg-white border border-gray-200 rounded-lg shadow-lg p-2 flex items-center gap-2 pointer-events-none text-xs font-semibold text-gray-800';
    dragImage.style.position = 'absolute';
    dragImage.style.top = '-1000px';
    dragImage.style.left = '-1000px';
    dragImage.style.borderLeft = '4px solid #691228';
    dragImage.innerHTML = `<span style="font-size: 1.125rem;">${icon}</span> <span style="max-width: 120px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">${docName}</span>`;
    document.body.appendChild(dragImage);
    event.dataTransfer.setDragImage(dragImage, 15, 15);
    setTimeout(() => dragImage.remove(), 0);
}

function handleFolderDragOver(event) {
    event.preventDefault();
    event.dataTransfer.dropEffect = "move";
}

function handleFolderDragEnter(event) {
    event.preventDefault();
    const target = event.currentTarget;
    if (target.classList.contains('explorer-item')) {
        target.classList.add('explorer-item-drag-hover');
    } else {
        target.classList.add('folder-card-drag-hover');
    }
}

function handleFolderDragLeave(event) {
    const target = event.currentTarget;
    target.classList.remove('explorer-item-drag-hover', 'folder-card-drag-hover');
}

async function handleFolderDrop(event, targetFolderId) {
    event.preventDefault();
    event.stopPropagation();
    
    const target = event.currentTarget;
    target.classList.remove('explorer-item-drag-hover', 'folder-card-drag-hover');
    
    // Si es un arrastre de archivos locales desde el sistema operativo
    if (isLocalFileDrag(event)) {
        const files = event.dataTransfer.files;
        if (files && files.length > 0) {
            await subirArchivosDirecto(files, targetFolderId);
        }
        return;
    }
    
    try {
        const dataStr = event.dataTransfer.getData("text/plain");
        if (!dataStr) return;
        const data = JSON.parse(dataStr);
        if (!data || !data.id || !data.type) return;
        
        const sourceId = data.id;
        const sourceType = data.type;
        
        if (sourceType === 'folder') {
            if (sourceId === targetFolderId) {
                mostrarNotificacion("No puedes mover una carpeta dentro de sí misma", "error");
                return;
            }
            if (targetFolderId && esDescendiente(sourceId, targetFolderId)) {
                mostrarNotificacion("No puedes mover una carpeta dentro de una de sus subcarpetas", "error");
                return;
            }
            
            await ejecutarMoverCarpeta(sourceId, targetFolderId);
        } else if (sourceType === 'document') {
            if (carpetaActualId === targetFolderId || (carpetaActualId === null && targetFolderId === null)) {
                return;
            }
            await ejecutarMoverDocumento(sourceId, targetFolderId);
        }
    } catch (e) {
        console.error("Error al procesar el drop:", e);
    }
}

function esDescendiente(parentFolderId, targetFolderId) {
    if (!arbolCarpetasGlobal) return false;
    const parentFolder = encontrarCarpeta(arbolCarpetasGlobal, parentFolderId);
    if (!parentFolder || !parentFolder.subcarpetas) return false;
    
    return checkSubfolderContains(parentFolder.subcarpetas, targetFolderId);
}

function encontrarCarpeta(carpetas, id) {
    for (const folder of carpetas) {
        if (folder.id === id || String(folder.id) === String(id)) {
            return folder;
        }
        if (folder.subcarpetas && folder.subcarpetas.length > 0) {
            const found = encontrarCarpeta(folder.subcarpetas, id);
            if (found) return found;
        }
    }
    return null;
}

function checkSubfolderContains(subcarpetas, targetFolderId) {
    for (const sub of subcarpetas) {
        if (sub.id === targetFolderId || String(sub.id) === String(targetFolderId)) {
            return true;
        }
        if (sub.subcarpetas && sub.subcarpetas.length > 0) {
            if (checkSubfolderContains(sub.subcarpetas, targetFolderId)) {
                return true;
            }
        }
    }
    return false;
}

async function ejecutarMoverCarpeta(carpetaId, nuevaCarpetaPadreId) {
    mostrarCargando("Moviendo carpeta...");
    try {
        let url = `/api/carpetas/${carpetaId}/mover`;
        if (nuevaCarpetaPadreId) {
            url += `?nuevaCarpetaPadreId=${nuevaCarpetaPadreId}`;
        }
        
        const response = await fetch(url, {
            method: 'PUT'
        });
        
        if (!response.ok) {
            const err = await response.json();
            throw new Error(err.error || "No se pudo mover la carpeta");
        }
        
        mostrarNotificacion("Carpeta movida correctamente", "success");
        await cargarArbolCarpetas();
        await cargarDocumentos();
    } catch (error) {
        console.error("Error al mover carpeta:", error);
        mostrarNotificacion(error.message || "Error al mover la carpeta. Verifique el límite de profundidad.", "error");
    } finally {
        ocultarCargando();
    }
}

async function ejecutarMoverDocumento(documentoId, carpetaDestinoId) {
    mostrarCargando("Moviendo documento...");
    try {
        let url = `/api/documentos/${documentoId}/mover`;
        if (carpetaDestinoId) {
            url += `?carpetaId=${carpetaDestinoId}`;
        }
        
        const response = await fetch(url, {
            method: 'PUT'
        });
        
        if (!response.ok) {
            const err = await response.json();
            throw new Error(err.error || "No se pudo mover el documento");
        }
        
        mostrarNotificacion("Documento movido correctamente", "success");
        await cargarArbolCarpetas();
        await cargarDocumentos();
    } catch (error) {
        console.error("Error al mover documento:", error);
        mostrarNotificacion(error.message || "Error al mover el documento.", "error");
    } finally {
        ocultarCargando();
    }
}

// ========================================
// ESTILOS CSS PARA EL ÁRBOL
// ========================================

// Añadir estilos dinámicamente
const style = document.createElement('style');
style.textContent = `
    /* Explorer Tree - Light Theme */
    .explorer-tree {
        font-size: 13px;
        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
        user-select: none;
    }
    
    .explorer-item {
        padding: 4px 10px;
        cursor: pointer;
        display: flex;
        align-items: center;
        transition: background 0.15s;
        min-height: 26px;
        position: relative;
        border-radius: 4px;
        margin: 1px 4px;
    }
    
    .explorer-item:hover {
        background: rgba(0, 0, 0, 0.05);
    }
    
    .explorer-item.selected {
        background: linear-gradient(135deg, rgba(128, 21, 21, 0.1) 0%, rgba(218, 165, 32, 0.1) 100%);
        border-left: 3px solid #801515;
    }
    
    .explorer-item.selected:hover {
        background: linear-gradient(135deg, rgba(128, 21, 21, 0.15) 0%, rgba(218, 165, 32, 0.15) 100%);
    }
    
    .explorer-label {
        font-size: 13px;
        line-height: 1.4;
    }
    
    .explorer-toggle {
        cursor: pointer;
        opacity: 0.6;
    }
    
    .explorer-toggle:hover {
        opacity: 1;
    }
    
    .explorer-group {
        position: relative;
    }
    
    .explorer-children {
        border-left: 1px solid rgba(0, 0, 0, 0.1);
        margin-left: 11px;
    }
    
    .explorer-children.hidden {
        display: none;
    }
    
    .explorer-actions {
        display: flex;
        gap: 2px;
        margin-left: auto;
    }
    
    .explorer-item:hover .explorer-actions {
        opacity: 1 !important;
    }
    
    .explorer-action {
        padding: 2px 6px;
        border-radius: 4px;
        color: #6b7280;
        font-size: 11px;
        transition: all 0.1s;
    }
    
    .explorer-action:hover {
        background: rgba(128, 21, 21, 0.1);
        color: #801515;
    }
    
    /* Scrollbar styling for light panel */
    #arbol-carpetas::-webkit-scrollbar {
        width: 8px;
    }
    
    #arbol-carpetas::-webkit-scrollbar-track {
        background: transparent;
    }
    
    #arbol-carpetas::-webkit-scrollbar-thumb {
        background: rgba(0, 0, 0, 0.15);
        border-radius: 4px;
    }
    
    #arbol-carpetas::-webkit-scrollbar-thumb:hover {
        background: rgba(0, 0, 0, 0.25);
    }
`;
document.head.appendChild(style);
