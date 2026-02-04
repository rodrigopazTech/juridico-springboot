/**
 * Gestor Documental - Lógica JavaScript
 */

let categoriaActual = null;
let vistaActual = 'list';
let documentosCache = [];

// --- FUNCIONES DE MODAL ---

function openGestorDocumental() {
    const modal = document.getElementById('modal-gestor-documental');
    if (modal) {
        modal.classList.remove('hidden');
        modal.classList.add('flex');
        document.body.style.overflow = 'hidden';

        // Cargar documentos y espacio ocupado
        cargarDocumentos();
        cargarEspacioOcupado();
    }
}

function closeGestorDocumental() {
    const modal = document.getElementById('modal-gestor-documental');
    if (modal) {
        modal.classList.add('hidden');
        modal.classList.remove('flex');
        document.body.style.overflow = 'auto';
    }
}

function abrirModalSubirDocumento() {
    const modal = document.getElementById('modal-subir-documento-gestor');
    if (modal) {
        modal.classList.remove('hidden');
        modal.classList.add('flex');

        // Pre-seleccionar categoría si hay una activa
        if (categoriaActual) {
            document.getElementById('doc-categoria-gestor').value = categoriaActual;
        }
    }
}

function cerrarModalSubirDocumento() {
    const modal = document.getElementById('modal-subir-documento-gestor');
    if (modal) {
        modal.classList.add('hidden');
        modal.classList.remove('flex');

        // Limpiar formulario
        document.getElementById('form-subir-documento-gestor').reset();
    }
}

// --- FUNCIONES DE NAVEGACIÓN ---

function cambiarCategoria(categoria) {
    categoriaActual = categoria;

    // Actualizar botones activos
    document.querySelectorAll('.categoria-btn').forEach(btn => {
        btn.classList.remove('bg-gray-200');
    });

    const btnId = categoria ? `btn-cat-${categoria.toLowerCase()}` : 'btn-cat-todos';
    const btn = document.getElementById(btnId);
    if (btn) {
        btn.classList.add('bg-gray-200');
    }

    // Recargar documentos
    cargarDocumentos();
}

function cambiarVista(vista) {
    vistaActual = vista;

    // Actualizar botones
    document.getElementById('btn-vista-grid').classList.remove('text-gob-guinda');
    document.getElementById('btn-vista-list').classList.remove('text-gob-guinda');
    document.getElementById('btn-vista-grid').classList.add('text-gray-600');
    document.getElementById('btn-vista-list').classList.add('text-gray-600');

    if (vista === 'grid') {
        document.getElementById('btn-vista-grid').classList.add('text-gob-guinda');
        document.getElementById('vista-lista').classList.add('hidden');
        document.getElementById('vista-grid').classList.remove('hidden');
    } else {
        document.getElementById('btn-vista-list').classList.add('text-gob-guinda');
        document.getElementById('vista-grid').classList.add('hidden');
        document.getElementById('vista-lista').classList.remove('hidden');
    }

    // Renderizar documentos en la vista seleccionada
    renderizarDocumentos(documentosCache);
}

// --- FUNCIONES DE CARGA DE DATOS ---

async function cargarDocumentos() {
    const expedienteId = EXPEDIENTE_ID;
    let url = `/api/documentos/expedientes/${expedienteId}`;

    if (categoriaActual) {
        url += `?categoria=${categoriaActual}`;
    }

    try {
        const response = await fetch(url);
        if (!response.ok) throw new Error('Error al cargar documentos');

        const documentos = await response.json();
        documentosCache = documentos;
        renderizarDocumentos(documentos);

    } catch (error) {
        console.error('Error:', error);
        mostrarMensajeVacio('Error al cargar documentos');
    }
}

async function cargarEspacioOcupado() {
    const expedienteId = EXPEDIENTE_ID;

    try {
        const response = await fetch(`/api/documentos/expedientes/${expedienteId}/storage-info`);
        if (!response.ok) throw new Error('Error al cargar espacio');

        const info = await response.json();
        const espacioMB = info.espacioMB.toFixed(2);

        document.getElementById('espacio-ocupado').innerHTML =
            `<i class="fas fa-hdd mr-1"></i> ${espacioMB} MB ocupados`;

    } catch (error) {
        console.error('Error:', error);
        document.getElementById('espacio-ocupado').innerHTML =
            `<i class="fas fa-hdd mr-1"></i> Error`;
    }
}

// --- FUNCIONES DE RENDERIZADO ---

function renderizarDocumentos(documentos) {
    if (documentos.length === 0) {
        mostrarMensajeVacio('No hay documentos en esta categoría');
        return;
    }

    if (vistaActual === 'list') {
        renderizarVistaLista(documentos);
    } else {
        renderizarVistaGrid(documentos);
    }
}

function renderizarVistaLista(documentos) {
    const container = document.getElementById('vista-lista');
    container.innerHTML = '';

    documentos.forEach(doc => {
        const icono = obtenerIconoArchivo(doc.tipoMime);
        const tamanio = formatearTamanio(doc.tamanioBytes);
        const fecha = new Date(doc.fechaSubida).toLocaleDateString('es-MX');

        const div = document.createElement('div');
        div.className = 'bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow flex items-center justify-between';
        div.innerHTML = `
            <div class="flex items-center gap-4 flex-1">
                <div class="text-3xl ${icono.color}">
                    <i class="${icono.clase}"></i>
                </div>
                <div class="flex-1">
                    <h4 class="font-semibold text-gray-800 text-sm">${doc.nombreOriginal}</h4>
                    <p class="text-xs text-gray-500">${doc.descripcion || 'Sin descripción'}</p>
                    <div class="flex items-center gap-3 mt-1 text-xs text-gray-400">
                        <span><i class="fas fa-calendar mr-1"></i>${fecha}</span>
                        <span><i class="fas fa-weight mr-1"></i>${tamanio}</span>
                        <span class="px-2 py-0.5 bg-gray-100 rounded text-gray-600">${doc.categoria}</span>
                    </div>
                </div>
            </div>
            <div class="flex items-center gap-2">
                ${puedePrevisualizar(doc.tipoMime) ?
                `<button onclick="previsualizarDocumento('${doc.id}')" class="p-2 text-blue-600 hover:bg-blue-50 rounded" title="Vista previa">
                        <i class="fas fa-eye"></i>
                    </button>` : ''}
                <button onclick="descargarDocumento('${doc.id}')" class="p-2 text-green-600 hover:bg-green-50 rounded" title="Descargar">
                    <i class="fas fa-download"></i>
                </button>
                <button onclick="eliminarDocumento('${doc.id}')" class="p-2 text-red-600 hover:bg-red-50 rounded" title="Eliminar">
                    <i class="fas fa-trash"></i>
                </button>
            </div>
        `;
        container.appendChild(div);
    });
}

function renderizarVistaGrid(documentos) {
    const container = document.getElementById('vista-grid');
    container.innerHTML = '';

    documentos.forEach(doc => {
        const icono = obtenerIconoArchivo(doc.tipoMime);
        const tamanio = formatearTamanio(doc.tamanioBytes);

        const div = document.createElement('div');
        div.className = 'bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow flex flex-col items-center text-center';
        div.innerHTML = `
            <div class="text-5xl ${icono.color} mb-3">
                <i class="${icono.clase}"></i>
            </div>
            <h4 class="font-semibold text-gray-800 text-xs mb-1 line-clamp-2">${doc.nombreOriginal}</h4>
            <p class="text-xs text-gray-400 mb-3">${tamanio}</p>
            <div class="flex items-center gap-2 w-full">
                ${puedePrevisualizar(doc.tipoMime) ?
                `<button onclick="previsualizarDocumento('${doc.id}')" class="flex-1 p-1.5 text-xs text-blue-600 hover:bg-blue-50 rounded">
                        <i class="fas fa-eye"></i>
                    </button>` : ''}
                <button onclick="descargarDocumento('${doc.id}')" class="flex-1 p-1.5 text-xs text-green-600 hover:bg-green-50 rounded">
                    <i class="fas fa-download"></i>
                </button>
                <button onclick="eliminarDocumento('${doc.id}')" class="flex-1 p-1.5 text-xs text-red-600 hover:bg-red-50 rounded">
                    <i class="fas fa-trash"></i>
                </button>
            </div>
        `;
        container.appendChild(div);
    });
}

function mostrarMensajeVacio(mensaje) {
    const vistaLista = document.getElementById('vista-lista');
    const vistaGrid = document.getElementById('vista-grid');

    const html = `
        <div class="text-center text-gray-400 py-12">
            <i class="far fa-folder-open text-5xl mb-3"></i>
            <p>${mensaje}</p>
        </div>
    `;

    vistaLista.innerHTML = html;
    vistaGrid.innerHTML = html;
}

// --- FUNCIONES DE ACCIONES ---

async function subirDocumento() {
    const fileInput = document.getElementById('doc-file-gestor');
    const categoriaSelect = document.getElementById('doc-categoria-gestor');
    const descripcionInput = document.getElementById('doc-descripcion-gestor');
    const expedienteId = document.getElementById('expediente-id-upload').value;
    const btn = document.getElementById('btn-subir-documento-gestor');

    if (!fileInput.files[0]) {
        alert('Por favor seleccione un archivo');
        return;
    }

    if (!categoriaSelect.value) {
        alert('Por favor seleccione una categoría');
        return;
    }

    // Deshabilitar botón
    btn.disabled = true;
    btn.innerHTML = '<i class="fas fa-spinner fa-spin mr-1"></i> Subiendo...';

    const formData = new FormData();
    formData.append('file', fileInput.files[0]);
    formData.append('categoria', categoriaSelect.value);
    formData.append('descripcion', descripcionInput.value);

    try {
        const response = await fetch(`/api/documentos/expedientes/${expedienteId}`, {
            method: 'POST',
            body: formData
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Error al subir archivo');
        }

        // Cerrar modal y recargar
        cerrarModalSubirDocumento();
        await cargarDocumentos();
        await cargarEspacioOcupado();

        if (typeof Swal !== 'undefined') {
            Swal.fire({
                icon: 'success',
                title: 'Documento subido',
                text: 'El archivo se ha guardado correctamente',
                timer: 2000,
                showConfirmButton: false
            });
        }

    } catch (error) {
        console.error('Error:', error);
        alert('Error al subir documento: ' + error.message);
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="fas fa-upload mr-1"></i> Subir Ahora';
    }
}

function descargarDocumento(documentoId) {
    window.open(`/api/documentos/${documentoId}/download`, '_blank');
}

function previsualizarDocumento(documentoId) {
    window.open(`/api/documentos/${documentoId}/preview`, '_blank');
}

async function eliminarDocumento(documentoId) {
    const confirmar = confirm('¿Está seguro de eliminar este documento? Esta acción no se puede deshacer.');

    if (!confirmar) return;

    try {
        const response = await fetch(`/api/documentos/${documentoId}`, {
            method: 'DELETE'
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Error al eliminar');
        }

        // Recargar documentos
        await cargarDocumentos();
        await cargarEspacioOcupado();

        if (typeof Swal !== 'undefined') {
            Swal.fire({
                icon: 'success',
                title: 'Documento eliminado',
                timer: 1500,
                showConfirmButton: false
            });
        }

    } catch (error) {
        console.error('Error:', error);
        alert('Error al eliminar documento: ' + error.message);
    }
}

// --- BÚSQUEDA ---

document.addEventListener('DOMContentLoaded', () => {
    const searchInput = document.getElementById('search-documentos-gestor');
    if (searchInput) {
        searchInput.addEventListener('input', (e) => {
            const busqueda = e.target.value.toLowerCase();

            if (!busqueda) {
                renderizarDocumentos(documentosCache);
                return;
            }

            const filtrados = documentosCache.filter(doc =>
                doc.nombreOriginal.toLowerCase().includes(busqueda) ||
                (doc.descripcion && doc.descripcion.toLowerCase().includes(busqueda))
            );

            renderizarDocumentos(filtrados);
        });
    }
});

// --- UTILIDADES ---

function obtenerIconoArchivo(tipoMime) {
    if (!tipoMime) return { clase: 'fas fa-file', color: 'text-gray-400' };

    if (tipoMime.includes('pdf')) {
        return { clase: 'fas fa-file-pdf', color: 'text-red-500' };
    } else if (tipoMime.includes('word') || tipoMime.includes('document')) {
        return { clase: 'fas fa-file-word', color: 'text-blue-500' };
    } else if (tipoMime.includes('excel') || tipoMime.includes('spreadsheet')) {
        return { clase: 'fas fa-file-excel', color: 'text-green-500' };
    } else if (tipoMime.includes('image')) {
        return { clase: 'fas fa-file-image', color: 'text-purple-500' };
    } else if (tipoMime.includes('video')) {
        return { clase: 'fas fa-file-video', color: 'text-pink-500' };
    } else if (tipoMime.includes('zip') || tipoMime.includes('rar') || tipoMime.includes('compressed')) {
        return { clase: 'fas fa-file-archive', color: 'text-yellow-500' };
    } else {
        return { clase: 'fas fa-file', color: 'text-gray-400' };
    }
}

function puedePrevisualizar(tipoMime) {
    if (!tipoMime) return false;
    return tipoMime.includes('pdf') || tipoMime.includes('image');
}

function formatearTamanio(bytes) {
    if (bytes === 0) return '0 Bytes';

    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));

    return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i];
}
