/**
 * Lógica para los modales y acciones de la vista de detalle del expediente.
 */

const EXPEDIENTE_ID = window.location.pathname.split('/').pop();

// --- FUNCIONES GENÉRICAS DE MODALES ---

function openModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.remove('hidden');
        modal.classList.add('flex');
        document.body.style.overflow = 'hidden';
        
        if (modalId === 'modal-editar') {
            inicializarSelectsEdicionExpediente();
        }
    }
}

function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.add('hidden');
        modal.classList.remove('flex');
        document.body.style.overflow = 'auto';
    }
}

// Cerrar modales al hacer clic fuera del contenido
window.onclick = function (event) {
    if (event.target.classList.contains('bg-gray-900/50')) {
        event.target.classList.add('hidden');
        event.target.classList.remove('flex');
        document.body.style.overflow = 'auto';
    }
}

// --- MÓDULO DE OBSERVACIONES (NOTAS) ---

async function openObservaciones() {
    openModal('modal-observaciones');
    await loadObservaciones(false);
}

async function loadObservaciones(smooth = false) {
    const container = document.getElementById('lista-observaciones');
    try {
        const response = await fetch(`/expedientes/${EXPEDIENTE_ID}/comentarios`);
        const notas = await response.json();

        container.innerHTML = '';

        if (notas.length === 0) {
            container.innerHTML = `
                <div class="flex flex-col items-center justify-center h-full text-gray-400 opacity-60">
                    <i class="far fa-comment-dots text-4xl mb-2"></i>
                    <p class="text-sm italic">No hay observaciones registradas.</p>
                </div>`;
            return;
        }

        notas.forEach(nota => {
            const date = new Date(nota.createdAt);
            const fechaStr = date.toLocaleDateString('es-MX', { day: '2-digit', month: 'short', year: 'numeric' });
            const horaStr = date.toLocaleTimeString('es-MX', { hour: '2-digit', minute: '2-digit' });

            // Mapeo de etapa a color de badge
            const etapaColores = {
                'TRAMITE': 'bg-blue-100 text-blue-700 border-blue-200',
                'LAUDO': 'bg-yellow-100 text-yellow-700 border-yellow-200',
                'FIRME': 'bg-green-100 text-green-700 border-green-200',
                'CONCLUIDO': 'bg-gray-100 text-gray-600 border-gray-200'
            };
            const etapaLabels = {
                'TRAMITE': 'Trámite',
                'LAUDO': 'Laudo',
                'FIRME': 'Firme',
                'CONCLUIDO': 'Concluido'
            };
            const etapa = nota.etapaNombre || nota.etapaProcesal;
            const etapaBadge = etapa
                ? `<span class="text-[9px] font-bold uppercase px-1.5 py-0.5 rounded border ${etapaColores[etapa] || 'bg-gray-100 text-gray-500 border-gray-200'}">${etapaLabels[etapa] || etapa}</span>`
                : '';

            const div = document.createElement('div');
            div.className = 'bg-white p-3 rounded-lg border border-gray-200 shadow-sm';
            div.innerHTML = `
                <div class="flex justify-between items-start mb-2 border-b border-gray-100 pb-1">
                    <span class="text-xs font-bold text-blue-600 uppercase">${nota.usuarioNombre || 'Usuario'}</span>
                    <div class="flex items-center gap-2">
                        ${etapaBadge}
                        <span class="text-[10px] text-gray-400">${fechaStr} ${horaStr}</span>
                    </div>
                </div>
                <p class="text-sm text-gray-700 whitespace-pre-wrap leading-relaxed break-words">${nota.comentario}</p>
            `;
            container.appendChild(div);
        });

        if (smooth) {
            container.scrollTo({
                top: container.scrollHeight,
                behavior: 'smooth'
            });
        } else {
            container.scrollTop = container.scrollHeight;
        }

    } catch (error) {
        console.error('Error al cargar notas:', error);
        container.innerHTML = '<p class="text-center text-red-500 py-4">Error al cargar las notas.</p>';
    }
}

async function guardarNota() {
    const textarea = document.getElementById('texto-nueva-nota');
    const texto = textarea.value.trim();
    const btn = document.getElementById('btn-guardar-nota');

    if (!texto) return;

    btn.disabled = true;
    btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';

    try {
        const response = await fetch(`/expedientes/${EXPEDIENTE_ID}/comentarios`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({comentario: texto })
        });

        if (response.ok) {
            textarea.value = '';
            await loadObservaciones(true);
            if (typeof Swal !== 'undefined') {
                const Toast = Swal.mixin({
                    toast: true,
                    position: 'bottom-end',
                    showConfirmButton: false,
                    timer: 2000,
                    //timerProgressBar: true
                });
                Toast.fire({ icon: 'success', title: 'Nota guardada' });
            }
        }
    } catch (error) {
        console.error('Error al guardar nota:', error);
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="fas fa-paper-plane"></i>';
    }
}

// --- MÓDULO DE CAMBIO DE ETAPA ---

async function updateEtapa(nuevaEtapa) {
    try {
        const response = await fetch(`/expedientes/${EXPEDIENTE_ID}/cambiar-etapa?etapa=${nuevaEtapa}`, {
            method: 'POST'
        });

        if (response.ok) {
            closeModal('modal-editar');
            if (typeof Swal !== 'undefined') {
                Swal.fire({
                    icon: 'success',
                    title: 'Etapa Actualizada',
                    text: `El expediente ha pasado a la etapa: ${nuevaEtapa}`,
                    timer: 1500,
                    showConfirmButton: false
                }).then(() => {
                    window.location.reload();
                });
            } else {
                window.location.reload();
            }
        }
    } catch (error) {
        console.error('Error al cambiar etapa:', error);
        if (typeof Swal !== 'undefined') {
            Swal.fire('Error', 'No se pudo actualizar la etapa', 'error');
        }
    }
}

// --- INICIALIZACIÓN ---

document.addEventListener('DOMContentLoaded', () => {
    const btnGuardarNota = document.getElementById('btn-guardar-nota');
    if (btnGuardarNota) {
        btnGuardarNota.addEventListener('click', guardarNota);
    }

    const textareaNota = document.getElementById('texto-nueva-nota');
    if (textareaNota) {
        textareaNota.addEventListener('keydown', (e) => {
            if (e.ctrlKey && e.key === 'Enter') {
                guardarNota();
            }
        });
    }

    cargarArchivosRecientes();
    iniciarWebSocketExpediente();

    // Abrir automáticamente el apartado de Observaciones si viene con el parámetro en la URL
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.get('openObservaciones') === 'true') {
        const newUrl = window.location.pathname;
        window.history.replaceState({}, document.title, newUrl);
        openObservaciones();
    }
});

// ========================================
// TIEMPO REAL (WEBSOCKET) PARA EXPEDIENTE
// ========================================
let stompClientExpediente = null;

function iniciarWebSocketExpediente() {
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
        if (stompClientExpediente) return;

        const socket = new SockJS('/ws-realtime');
        stompClientExpediente = Stomp.over(socket);
        stompClientExpediente.debug = null;

        stompClientExpediente.connect({}, function (frame) {
            //console.log('🔔 Detalle Expediente: Conexión WebSocket establecida.');
            
            const dest = '/topic/expedientes/' + EXPEDIENTE_ID;
            stompClientExpediente.subscribe(dest, function (message) {
                try {
                    const payload = JSON.parse(message.body);
                    //console.log("🔔 Cambio detectado:", payload.tabla, payload.operacion);
                    
                    if (payload.tabla === 'expedientes' || payload.tabla === 'actividad_expedientes' || payload.tabla === 'documentos') {
                        recargarDetalleExpediente();
                        if (payload.tabla === 'documentos') {
                            cargarArchivosRecientes();
                        }
                    } else if (payload.tabla === 'comentarios') {
                        const modalObs = document.getElementById('modal-observaciones');
                        if (modalObs && !modalObs.classList.contains('hidden')) {
                            loadObservaciones(true);
                        }
                        recargarDetalleExpediente();
                    }
                } catch (e) {
                    console.error("Error al procesar mensaje en detalle:", e);
                }
            });
        }, function (error) {
            stompClientExpediente = null;
            setTimeout(iniciarWebSocketExpediente, 5000);
        });
    });
}

function recargarDetalleExpediente() {
    fetch(window.location.pathname)
    .then(res => res.text())
    .then(html => {
        const parser = new DOMParser();
        const doc = parser.parseFromString(html, 'text/html');
        
        // 1. Encabezado
        const nuevoHeader = doc.getElementById('encabezado-detalle-container');
        const actualHeader = document.getElementById('encabezado-detalle-container');
        if (nuevoHeader && actualHeader) {
            actualHeader.innerHTML = nuevoHeader.innerHTML;
        }
        
        // 2. Información General
        const nuevoInfo = doc.getElementById('informacion-general-container');
        const actualInfo = document.getElementById('informacion-general-container');
        if (nuevoInfo && actualInfo) {
            actualInfo.innerHTML = nuevoInfo.innerHTML;
        }
        
        // 3. Historial Lateral
        const nuevoHistorial = doc.getElementById('historial-lateral-container');
        const actualHistorial = document.getElementById('historial-lateral-container');
        if (nuevoHistorial && actualHistorial) {
            actualHistorial.innerHTML = nuevoHistorial.innerHTML;
        }
    })
    .catch(err => console.error("Error al recargar detalle:", err));
}

// ========================================
// CARGA DINÁMICA DE ARCHIVOS RECIENTES
// ========================================

function formatearTamanioDocumento(bytes) {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i];
}

async function cargarArchivosRecientes() {
    const grid = document.getElementById('archivos-recientes-grid');
    if (!grid) return;

    try {
        const response = await fetch(`/api/documentos/expedientes/${EXPEDIENTE_ID}`);
        if (!response.ok) {
            throw new Error('Error al obtener documentos');
        }
        const documentos = await response.json();

        grid.innerHTML = '';

        if (!documentos || documentos.length === 0) {
            grid.innerHTML = `
                <div class="col-span-3 text-center py-6 text-gray-400 italic text-sm">
                    No hay archivos subidos en este expediente.
                </div>
            `;
            return;
        }

        // Tomar los últimos 3 documentos subidos
        const recientes = documentos.slice(0, 3);

        recientes.forEach(doc => {
            const nombre = doc.nombreOriginal || 'Sin nombre';
            const extension = nombre.split('.').pop().toLowerCase();
            const tamanio = formatearTamanioDocumento(doc.tamanioBytes);

            let bgClass = 'bg-gray-100 text-gray-600 border border-gray-200';
            let iconClass = 'fa-file';

            if (extension === 'pdf') {
                bgClass = 'bg-red-50 text-red-650 border border-red-100';
                iconClass = 'fa-file-pdf';
            } else if (['doc', 'docx'].includes(extension)) {
                bgClass = 'bg-blue-50 text-blue-650 border border-blue-100';
                iconClass = 'fa-file-word';
            } else if (['xls', 'xlsx'].includes(extension)) {
                bgClass = 'bg-green-50 text-green-650 border border-green-100';
                iconClass = 'fa-file-excel';
            } else if (['jpg', 'jpeg', 'png', 'gif'].includes(extension)) {
                bgClass = 'bg-purple-50 text-purple-650 border border-purple-100';
                iconClass = 'fa-file-image';
            } else if (['zip', 'rar', 'tar', 'gz'].includes(extension)) {
                bgClass = 'bg-amber-50 text-amber-600 border border-amber-100';
                iconClass = 'fa-file-archive';
            }

            const fechaSubida = new Date(doc.fechaSubida);
            const hoy = new Date();
            const diffMs = hoy - fechaSubida;
            const diffMins = Math.floor(diffMs / 60000);
            const diffHoras = Math.floor(diffMins / 60);
            const diffDias = Math.floor(diffHoras / 24);

            let tiempoStr = '';
            if (diffMins < 1) {
                tiempoStr = 'Hace un momento';
            } else if (diffMins < 60) {
                tiempoStr = `Hace ${diffMins} min`;
            } else if (diffHoras < 24) {
                tiempoStr = `Hace ${diffHoras} hr`;
            } else if (diffDias === 1) {
                tiempoStr = 'Ayer';
            } else if (diffDias < 7) {
                tiempoStr = `Hace ${diffDias} días`;
            } else {
                tiempoStr = fechaSubida.toLocaleDateString('es-MX', { day: '2-digit', month: 'short' });
            }

            const item = document.createElement('div');
            item.className = 'group relative flex items-center justify-between p-3 border border-gray-150 rounded-xl hover:bg-gray-50/85 hover:border-gob-oro/30 hover:shadow-sm transition-all duration-200 cursor-pointer';
            item.title = 'Haga clic para previsualizar';
            
            item.addEventListener('click', () => {
                window.open(`/api/documentos/${doc.id}/preview`, '_blank');
            });

            item.innerHTML = `
                <div class="flex items-center gap-3 min-w-0">
                    <div class="w-10 h-10 flex items-center justify-center rounded-lg text-lg ${bgClass} shrink-0">
                        <i class="fas ${iconClass}"></i>
                    </div>
                    <div class="min-w-0">
                        <p class="text-xs font-bold text-gray-700 truncate group-hover:text-gob-guinda transition-colors" title="${nombre}">${nombre}</p>
                        <p class="text-[10px] text-gray-400">${tamanio} • ${tiempoStr}</p>
                    </div>
                </div>
                <div class="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity ml-2 shrink-0">
                    <button onclick="event.stopPropagation(); window.open('/api/documentos/${doc.id}/download', '_blank');"
                            class="w-7 h-7 flex items-center justify-center rounded-md text-gray-500 hover:text-gob-guinda hover:bg-gray-100 transition-all"
                            title="Descargar">
                        <i class="fas fa-download text-xs"></i>
                    </button>
                </div>
            `;
            grid.appendChild(item);
        });
    } catch (error) {
        console.error('Error al cargar archivos recientes:', error);
        grid.innerHTML = `
            <div class="col-span-3 text-center py-6 text-red-500 italic text-sm">
                Error al cargar archivos recientes.
            </div>
        `;
    }
}

function inicializarSelectsEdicionExpediente() {
    const selectGerencia = document.getElementById('select-gerencia-exp-edit');
    const selectSede = document.getElementById('select-sede-exp-edit');
    const selectMateria = document.getElementById('select-materia-exp-edit');
    const selectAbogado = document.getElementById('select-abogado-exp-edit');
    const selectEtapa = document.getElementById('select-etapa-exp-edit');

    if (selectGerencia && !selectGerencia.tomselect) {
        const tsGer = initSearchableSelect(selectGerencia, { tipo: 'gerencia' });
        
        if (tsGer) {
            tsGer.on('change', function() {
                const gerId = tsGer.getValue();
                const tsMat = selectMateria ? selectMateria.tomselect : null;
                if (tsMat && gerId) {
                    tsMat.disable();
                    fetch(`/expedientes/api/materias-por-gerencia/${gerId}`)
                        .then(res => res.json())
                        .then(materias => {
                            updateTomSelectOptions(tsMat, materias.map(m => ({ value: m.id, text: m.nombre })), 'Selecciona materia...');
                            tsMat.enable();
                            if (selectEtapa?.tomselect) {
                                updateTomSelectOptions(selectEtapa.tomselect, [], 'Primero selecciona una materia...');
                                selectEtapa.tomselect.disable();
                            }
                        });
                }
            });
        }
    }

    if (selectSede && !selectSede.tomselect) {
        initSearchableSelect(selectSede, { tipo: 'sede', createInline: true, useTextAsValue: true });
    }

    if (selectMateria && !selectMateria.tomselect) {
        const tsMat = initSearchableSelect(selectMateria, { 
            tipo: 'materia', 
            createInline: true, 
            gerenciaSelector: '#select-gerencia-exp-edit' 
        });
        
        if (tsMat) {
            tsMat.on('change', function() {
                const tsGer = selectGerencia ? selectGerencia.tomselect : null;
                const tsAbg = selectAbogado ? selectAbogado.tomselect : null;
                const tsEtapa = selectEtapa ? selectEtapa.tomselect : null;
                if (tsGer && tsAbg) {
                    const gerId = tsGer.getValue();
                    const matId = tsMat.getValue();
                    if (gerId && matId) {
                        tsAbg.disable();
                        fetch(`/expedientes/api/abogados?gerenciaId=${gerId}&materiaId=${matId}`)
                            .then(res => res.json())
                            .then(abogados => {
                                updateTomSelectOptions(tsAbg, abogados.map(a => ({ value: a.id, text: a.nombreCompleto })), 'Asignar abogado...');
                                tsAbg.enable();
                            });
                    }
                }
                if (tsEtapa) {
                    const matId = tsMat.getValue();
                    tsEtapa.disable();
                    updateTomSelectOptions(tsEtapa, [], 'Cargando etapas...');
                    if (matId) {
                        fetch(`/expedientes/api/etapas-por-materia/${matId}`)
                            .then(res => res.json())
                            .then(etapas => {
                                if (etapas.length) {
                                    updateTomSelectOptions(tsEtapa, etapas.map(e => ({
                                        value: e.id,
                                        text: e.parentId ? `- ${e.nombreJerarquico || e.nombre}` : (e.nombreJerarquico || e.nombre)
                                    })), 'Selecciona etapa...');
                                    tsEtapa.enable();
                                    tsEtapa.setValue(etapas[0].id);
                                } else {
                                    updateTomSelectOptions(tsEtapa, [], 'Configura etapas para esta materia');
                                }
                            });
                    } else {
                        updateTomSelectOptions(tsEtapa, [], 'Primero selecciona una materia...');
                    }
                }
            });
        }
    }

    if (selectAbogado && !selectAbogado.tomselect) {
        initSearchableSelect(selectAbogado, { tipo: 'abogado' });
    }

    if (selectEtapa && !selectEtapa.tomselect) {
        initSearchableSelect(selectEtapa, { tipo: 'etapa' });
    }
}
