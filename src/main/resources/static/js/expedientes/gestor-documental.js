// ========================================
// GESTOR DOCUMENTAL - SISTEMA DE CARPETAS JERÁRQUICO
// ========================================

let expedienteIdGlobal = null;
let numeroExpedienteGlobal = null;
let carpetaActualId = null;
let arbolCarpetasGlobal = [];
let vistaActual = 'list';

// ========================================
// INICIALIZACIÓN
// ========================================

function openGestorDocumental(expedienteId, numeroExpediente) {
    expedienteIdGlobal = expedienteId;
    numeroExpedienteGlobal = numeroExpediente || 'Expediente';
    carpetaActualId = null;

    const modal = document.getElementById('modal-gestor-documental');
    modal.classList.remove('hidden');
    modal.classList.add('flex');

    // Cargar datos iniciales
    cargarArbolCarpetas();
    cargarEspacioOcupado();
    cargarDocumentos();
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
}

// ========================================
// ÁRBOL DE CARPETAS
// ========================================

async function cargarArbolCarpetas() {
    try {
        const response = await fetch(`/api/carpetas/expedientes/${expedienteIdGlobal}/arbol`);

        if (!response.ok) {
            throw new Error('Error al cargar carpetas');
        }

        arbolCarpetasGlobal = await response.json();
        renderizarArbolCarpetas();
        actualizarSelectoresCarpetas();

    } catch (error) {
        console.error('Error:', error);
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

    // Crear nodo raíz del expediente
    let html = `
        <div class="explorer-tree">
            <div class="explorer-item ${carpetaActualId === null ? 'selected' : ''}" 
                 onclick="seleccionarCarpeta(null)">
                <span class="explorer-indent"></span>
                <i class="fas fa-chevron-down text-[10px] text-gray-400 mr-1"></i>
                <i class="fas fa-folder text-amber-500 text-sm mr-1.5"></i>
                <span class="explorer-label text-gray-700 font-medium">${numeroExpedienteGlobal}</span>
            </div>
    `;

    // Renderizar carpetas recursivamente
    arbolCarpetasGlobal.forEach(carpeta => {
        html += renderizarNodoCarpeta(carpeta, 1);
    });

    html += `</div>`;

    container.innerHTML = html;
}

function renderizarNodoCarpeta(carpeta, nivel) {
    const tieneSubcarpetas = carpeta.subcarpetas && carpeta.subcarpetas.length > 0;
    const estaSeleccionada = carpetaActualId === carpeta.id;
    const expandido = true;
    const indent = nivel * 12;

    let html = `
        <div class="explorer-group">
            <div class="explorer-item ${estaSeleccionada ? 'selected' : ''}" 
                 data-carpeta-id="${carpeta.id}"
                 onclick="seleccionarCarpeta('${carpeta.id}')"
                 style="padding-left: ${indent}px">
                ${tieneSubcarpetas ? `
                    <i class="fas fa-chevron-${expandido ? 'down' : 'right'} text-[10px] text-gray-400 mr-1 explorer-toggle cursor-pointer" 
                       onclick="event.stopPropagation(); toggleCarpeta('${carpeta.id}')"></i>
                ` : '<span class="w-3 mr-1"></span>'}
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
                                class="explorer-action" title="Eliminar">
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

function toggleCarpeta(carpetaId) {
    const childrenDiv = document.getElementById(`children-${carpetaId}`);
    const toggleIcon = event.target.classList.contains('explorer-toggle') 
        ? event.target 
        : event.target.closest('.explorer-toggle');
    
    const folderIcon = toggleIcon.parentElement.querySelector('.fa-folder, .fa-folder-open');

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
    carpetaActualId = carpetaId;

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

async function eliminarCarpeta(carpetaId) {
    if (!confirm('¿Está seguro de eliminar esta carpeta? Solo se pueden eliminar carpetas vacías.')) {
        return;
    }

    try {
        const response = await fetch(`/api/carpetas/${carpetaId}`, {
            method: 'DELETE'
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Error al eliminar carpeta');
        }

        // Si estábamos en esta carpeta, volver a raíz
        if (carpetaActualId === carpetaId) {
            carpetaActualId = null;
        }

        // Recargar árbol
        await cargarArbolCarpetas();
        cargarDocumentos();
        mostrarNotificacion('Carpeta eliminada correctamente', 'success');

    } catch (error) {
        console.error('Error:', error);
        alert(error.message);
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
        renderizarContenido(subcarpetas, documentos);

    } catch (error) {
        console.error('Error:', error);
        vistaLista.innerHTML = `
            <div class="text-center text-red-500 py-12">
                <i class="fas fa-exclamation-triangle text-3xl mb-2"></i>
                <p>Error al cargar contenido</p>
            </div>
        `;
    }
}

function renderizarContenido(subcarpetas, documentos) {
    const vistaLista = document.getElementById('vista-lista');
    const vistaGrid = document.getElementById('vista-grid');

    const tieneContenido = subcarpetas.length > 0 || documentos.length > 0;

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
            <div class="flex items-center justify-between p-3 bg-amber-50 border border-amber-200 rounded-lg hover:shadow-md hover:bg-amber-100 transition-all cursor-pointer"
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
            <div class="flex items-center justify-between p-3 bg-white border border-gray-200 rounded-lg hover:shadow-md transition-shadow">
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
            <div class="bg-amber-50 border border-amber-200 rounded-lg p-4 hover:shadow-lg hover:bg-amber-100 transition-all text-center cursor-pointer"
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
            <div class="bg-white border border-gray-200 rounded-lg p-4 hover:shadow-lg transition-shadow text-center">
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

// ========================================
// SUBIR DOCUMENTO
// ========================================

function abrirModalSubirDocumento() {
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

async function subirDocumento() {
    const fileInput = document.getElementById('doc-file-gestor');
    const carpetaId = document.getElementById('doc-carpeta-gestor').value || null;
    const descripcion = document.getElementById('doc-descripcion-gestor').value;

    if (!fileInput.files || fileInput.files.length === 0) {
        alert('Por favor seleccione al menos un archivo');
        return;
    }

    const btn = document.getElementById('btn-subir-documento-gestor');
    btn.disabled = true;
    
    const totalArchivos = fileInput.files.length;
    let archivosSubidos = 0;
    let errores = [];

    try {
        for (let i = 0; i < totalArchivos; i++) {
            const archivo = fileInput.files[i];
            btn.innerHTML = `<i class="fas fa-spinner fa-spin mr-1"></i> Subiendo ${i + 1} de ${totalArchivos}...`;
            
            const formData = new FormData();
            formData.append('file', archivo);
            if (carpetaId) {
                formData.append('carpetaId', carpetaId);
            }
            if (descripcion && totalArchivos === 1) {
                // Solo agregar descripción si es un solo archivo
                formData.append('descripcion', descripcion);
            }

            try {
                const response = await fetch(`/api/documentos/expedientes/${expedienteIdGlobal}`, {
                    method: 'POST',
                    body: formData
                });

                if (!response.ok) {
                    const error = await response.json();
                    errores.push(`${archivo.name}: ${error.error || 'Error desconocido'}`);
                } else {
                    archivosSubidos++;
                }
            } catch (err) {
                errores.push(`${archivo.name}: ${err.message}`);
            }
        }

        // Cerrar modal
        cerrarModalSubirDocumento();

        // Recargar documentos y espacio
        await cargarDocumentos();
        await cargarEspacioOcupado();
        await cargarArbolCarpetas(); // Actualizar contadores

        // Mostrar resultado
        if (errores.length === 0) {
            mostrarNotificacion(`${archivosSubidos} documento(s) subido(s) correctamente`, 'success');
        } else if (archivosSubidos > 0) {
            mostrarNotificacion(`${archivosSubidos} subido(s), ${errores.length} error(es)`, 'warning');
            console.error('Errores al subir:', errores);
        } else {
            alert('Error al subir los documentos:\\n' + errores.join('\\n'));
        }

    } catch (error) {
        console.error('Error:', error);
        alert(error.message);
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="fas fa-upload mr-1"></i> Subir Ahora';
    }
}

async function eliminarDocumento(documentoId) {
    if (!confirm('¿Está seguro de eliminar este documento? Esta acción no se puede deshacer.')) {
        return;
    }

    try {
        const response = await fetch(`/api/documentos/${documentoId}`, {
            method: 'DELETE'
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Error al eliminar documento');
        }

        // Recargar documentos y espacio
        await cargarDocumentos();
        await cargarEspacioOcupado();
        await cargarArbolCarpetas(); // Actualizar contadores

        mostrarNotificacion('Documento eliminado correctamente', 'success');

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
// DRAG & DROP
// ========================================

function handleDragOver(event) {
    event.preventDefault();
    event.stopPropagation();
    
    const overlay = document.getElementById('drop-zone-overlay');
    const carpetaLabel = document.getElementById('drop-zone-carpeta');
    
    // Mostrar overlay
    overlay.classList.remove('hidden');
    overlay.classList.add('flex');
    
    // Actualizar texto con carpeta actual
    if (carpetaActualId) {
        const nombreCarpeta = buscarNombreCarpeta(carpetaActualId, arbolCarpetasGlobal);
        carpetaLabel.textContent = `Se subirán a: ${nombreCarpeta}`;
    } else {
        carpetaLabel.textContent = `Se subirán a: ${numeroExpedienteGlobal || 'Raíz'}`;
    }
}

function handleDragLeave(event) {
    event.preventDefault();
    event.stopPropagation();
    
    // Solo ocultar si salimos del área principal
    const rect = document.getElementById('area-documentos').getBoundingClientRect();
    if (event.clientX < rect.left || event.clientX > rect.right || 
        event.clientY < rect.top || event.clientY > rect.bottom) {
        const overlay = document.getElementById('drop-zone-overlay');
        overlay.classList.add('hidden');
        overlay.classList.remove('flex');
    }
}

async function handleDrop(event) {
    event.preventDefault();
    event.stopPropagation();
    
    const overlay = document.getElementById('drop-zone-overlay');
    overlay.classList.add('hidden');
    overlay.classList.remove('flex');
    
    const files = event.dataTransfer.files;
    if (!files || files.length === 0) return;
    
    // Subir archivos directamente a la carpeta actual
    await subirArchivosDirecto(files, carpetaActualId);
}

async function subirArchivosDirecto(files, carpetaId) {
    const totalArchivos = files.length;
    let archivosSubidos = 0;
    let errores = [];
    
    // Mostrar notificación de inicio
    mostrarNotificacion(`Subiendo ${totalArchivos} archivo(s)...`, 'info');
    
    for (let i = 0; i < totalArchivos; i++) {
        const archivo = files[i];
        
        const formData = new FormData();
        formData.append('file', archivo);
        if (carpetaId) {
            formData.append('carpetaId', carpetaId);
        }
        
        try {
            const response = await fetch(`/api/documentos/expedientes/${expedienteIdGlobal}`, {
                method: 'POST',
                body: formData
            });
            
            if (!response.ok) {
                const error = await response.json();
                errores.push(`${archivo.name}: ${error.error || 'Error'}`);
            } else {
                archivosSubidos++;
            }
        } catch (err) {
            errores.push(`${archivo.name}: ${err.message}`);
        }
    }
    
    // Recargar datos
    await cargarDocumentos();
    await cargarEspacioOcupado();
    await cargarArbolCarpetas();
    
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
// BÚSQUEDA
// ========================================

document.addEventListener('DOMContentLoaded', function () {
    const searchInput = document.getElementById('search-documentos-gestor');
    if (searchInput) {
        searchInput.addEventListener('input', debounce(buscarDocumentos, 300));
    }
});

async function buscarDocumentos() {
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
    // Implementación simple con alert
    // Puedes mejorar esto con una librería de notificaciones
    console.log(`[${tipo.toUpperCase()}] ${mensaje}`);
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
