// ===============================================
// 1. CONFIGURACIÓN Y API
// ===============================================
const API_URL = '/api/v1/terminos'; // Conexión a tu Spring Boot
const USER_ROLE = 'Subdireccion';   // Tu rol actual

const FLUJO_ETAPAS = {
    'Proyectista': { siguiente: 'Revisión', accion: 'enviarRevision', label: 'Enviar a Revisión' },
    'Revisión':    { siguiente: 'Gerencia', anterior: 'Proyectista', accion: 'aprobar', label: 'Aprobar a Gerencia' },
    'Gerencia':    { siguiente: 'Dirección', anterior: 'Revisión', accion: 'aprobar', label: 'Aprobar a Dirección' },
    'Dirección':   { siguiente: 'Liberado', anterior: 'Gerencia', accion: 'aprobar', label: 'Liberar Término' },
    'Liberado':    { siguiente: 'Presentado', accion: 'subirAcuse', label: 'Subir Acuse' },
    'Presentado':  { siguiente: 'Concluido', accion: 'concluir', label: 'Concluir' }
};

const PERMISOS_ETAPAS = {
    'Proyectista': ['Abogado', 'Gerente','JefeDepto','Direccion'],
    'Revisión':    ['JefeDepto', 'Gerente', 'Direccion'],
    'Gerencia':    ['Gerente', 'Direccion'],
    'Dirección':   ['Direccion', 'Subdireccion'],
    'Liberado':    ['Abogado', 'JefeDepto', 'Gerente','Direccion','Subdireccion'],
    'Presentado':  ['Direccion','Subdireccion'],
    'Concluido':   []
};

let TERMINOS = [];

// ===============================================
// 2. INICIALIZACIÓN
// ===============================================
function initTerminos() {
    console.log("Iniciando módulo Términos (Conectado a Spring Boot)...");
    
    // Carga inicial desde el Backend
    loadTerminos(); 
    
    // Inicializar UI
    setupSearchAndFilters();
    initModalTerminosJS();      
    initModalPresentar();       
    initModalReasignar();       
    setupActionMenuListener(); 
    
    // Cargas auxiliares (estas podrían requerir sus propias APIs en el futuro)
    cargarAsuntosEnSelectorJS(); 
    cargarAbogadosSelector();

    // Configurar Botones
    const btnNuevo = document.getElementById('add-termino');
    if (btnNuevo) {
        const newBtn = btnNuevo.cloneNode(true);
        btnNuevo.parentNode.replaceChild(newBtn, btnNuevo);
        newBtn.addEventListener('click', () => openTerminoModalJS());
    }

    const btnExportar = document.getElementById('export-terminos');
    if (btnExportar) {
        const newBtnExp = btnExportar.cloneNode(true);
        btnExportar.parentNode.replaceChild(newBtnExp, btnExportar);
        newBtnExp.addEventListener('click', () => {
             if(typeof XLSX !== 'undefined') exportarTablaExcel(); 
             else alert('Librería de exportación no cargada.');
        });
    }
}

// ===============================================
// 3. RENDERIZADO Y CARGA (API)
// ===============================================
async function loadTerminos() {
    const tbody = document.getElementById('terminos-body');
    if(!tbody) return;
    
    // Indicador de carga
    tbody.innerHTML = '<tr><td colspan="9" class="text-center py-4 text-gray-500"><i class="fas fa-spinner fa-spin mr-2"></i>Cargando datos...</td></tr>';

    try {
        // --- PETICIÓN AL BACKEND ---
        const response = await fetch(API_URL);
        
        if (!response.ok) {
            throw new Error(`Error HTTP: ${response.status}`);
        }
        
        TERMINOS = await response.json(); // Guardamos los datos reales en memoria
        renderTabla(); // Dibujamos la tabla

    } catch (error) {
        console.error("Error cargando términos:", error);
        tbody.innerHTML = '<tr><td colspan="9" class="text-center py-4 text-red-500"><i class="fas fa-exclamation-triangle mr-1"></i>Error al cargar datos del servidor.</td></tr>';
        mostrarMensajeGlobal('Error de conexión con el servidor', 'danger');
    }
}

function renderTabla() {
    const tbody = document.getElementById('terminos-body');
    
    // Filtros
    const filtros = {
        tribunal: document.getElementById('filter-tribunal-termino')?.value.toLowerCase() || '',
        estado: document.getElementById('filter-estado-termino')?.value.toLowerCase() || '',
        estatus: document.getElementById('filter-estatus-termino')?.value || '',
        prioridad: document.getElementById('filter-prioridad-termino')?.value || '',
        materia: document.getElementById('filter-materia-termino')?.value || '',
        search: document.getElementById('search-terminos')?.value.toLowerCase() || ''
    };

    const listaFiltrada = TERMINOS.filter(t => {
        const textoCompleto = `${t.expediente || ''} ${t.actor || ''} ${t.asunto || ''} ${t.abogado || ''}`.toLowerCase();
        
        if (filtros.search && !textoCompleto.includes(filtros.search)) return false;
        if (filtros.estatus && !filtros.estatus.includes('Todos') && t.estatus !== filtros.estatus) return false;
        if (filtros.prioridad && !filtros.prioridad.includes('Todas') && t.prioridad !== filtros.prioridad) return false;
        
        return true;
    });

    let html = '';
    listaFiltrada.forEach(t => {
        const semaforoColor = getSemaforoColor(t.fechaVencimiento); 
        const badgeClass = getBadgeClass(t.estatus);
        const diasRestantes = calcularDiasRestantes(t.fechaVencimiento);
        
        let tooltipTexto = diasRestantes < 0 ? `Vencido hace ${Math.abs(diasRestantes)} días` : (diasRestantes === 0 ? "Vence HOY" : `Faltan ${diasRestantes} días`);

        const esBloqueado = t.estatus === 'Concluido' || t.estatus === 'Presentado';
        
        const botonEditar = esBloqueado
            ? `<button class="text-gray-300 cursor-not-allowed p-1" title="Bloqueado"><i class="fas fa-lock"></i></button>` 
            : `<button class="text-gray-400 hover:text-gob-oro action-edit p-1" title="Editar"><i class="fas fa-edit"></i></button>`;

        html += `
        <tr class="bg-white hover:bg-gray-50 border-b transition-colors group" data-id="${t.id}">
            <td class="px-4 py-3 whitespace-nowrap text-center">
                <div class="flex items-center">
                    <div class="w-2.5 h-2.5 rounded-full mr-2 ${semaforoColor}" title="${tooltipTexto}"></div>
                    <span class="text-sm font-medium text-gray-900">${formatDate(t.fechaIngreso)}</span>
                </div>
            </td>
            <td class="px-4 py-3 text-sm text-gray-500 font-bold">${formatDate(t.fechaVencimiento)}</td>
            <td class="px-4 py-3 text-sm font-bold text-gob-guinda">${t.expediente || 'S/N'}</td>
            <td class="px-4 py-3 text-sm text-gray-700 max-w-[150px] truncate" title="${t.actor}">${t.actor || ''}</td>
            <td class="px-4 py-3 text-sm text-gray-600 max-w-[200px] truncate" title="${t.asunto}">${t.asunto || ''}</td> 
            <td class="px-4 py-3 text-sm text-gray-500">${t.prestacion || 'N/A'}</td> 
            <td class="px-4 py-3 text-sm text-gray-500">${t.abogado || 'Sin asignar'}</td>
            <td class="px-4 py-3">
                <span class="inline-flex items-center px-2.5 py-0.5 rounded text-xs font-bold border ${badgeClass}">
                    ${t.estatus}
                </span>
            </td>
            
            <td class="px-4 py-3 text-right whitespace-nowrap relative">
                <div class="flex items-center justify-end gap-2">
                    ${botonEditar}
                    <button class="text-gray-400 hover:text-gob-guinda action-menu-toggle p-1 px-2 transition-colors" title="Más Acciones"><i class="fas fa-ellipsis-v"></i></button>
                    <div class="action-menu hidden bg-white rounded shadow-xl border border-gray-100 border-t-4 border-gob-oro w-56 font-headings z-50 absolute right-0 mt-8">
                        ${generarAccionesRapidas(t, USER_ROLE)}
                    </div>
                    <input type="file" class="input-acuse-hidden hidden" data-id="${t.id}">
                </div>
            </td>
        </tr>
        `;
    });

    tbody.innerHTML = html || '<tr><td colspan="9" class="text-center py-8 text-gray-500">No se encontraron resultados</td></tr>';
}

function setupSearchAndFilters() {
    const ids = ['search-terminos', 'filter-tribunal-termino', 'filter-estado-termino', 'filter-estatus-termino', 'filter-prioridad-termino', 'filter-materia-termino'];
    ids.forEach(id => {
        const el = document.getElementById(id);
        if(el) el.addEventListener(el.tagName === 'INPUT' ? 'input' : 'change', renderTabla);
    });
}

// ===============================================
// 4. GENERACIÓN DE MENÚ
// ===============================================
function generarAccionesRapidas(termino, rol) {
    let html = '';
    const etapa = termino.estatus;
    const rolesPermitidos = PERMISOS_ETAPAS[etapa] || [];
    const puedeActuar = rolesPermitidos.includes(rol);

    const itemClass = "w-full text-left px-4 py-3 text-sm text-gob-gris hover:bg-gray-50 hover:text-gob-guinda transition-colors flex items-center gap-3 border-b border-gray-50 last:border-0";

    // Opciones Comunes
    html += `<button class="${itemClass} action-view-asunto"><i class="fas fa-briefcase text-gray-400"></i> Ver Asunto</button>`;
    html += `<button class="${itemClass} action-history"><i class="fas fa-eye text-gray-400"></i> Ver Historial</button>`;

    // CASO FINAL: CONCLUIDO
    if (etapa === 'Concluido') {
        if (termino.acuseDocumento) html += `<button class="${itemClass} action-download-acuse text-blue-600"><i class="fas fa-file-download"></i> Descargar Acuse</button>`;
        if (rol === 'Direccion') html += `<button class="${itemClass} action-delete text-red-600 font-bold"><i class="fas fa-trash-alt"></i> Eliminar</button>`;
        return html; 
    }

    // CASO ESPECIAL: PRESENTADO
    if (etapa === 'Presentado') {
        html += `<button class="${itemClass} action-download-acuse text-blue-600"><i class="fas fa-file-download"></i> Descargar Acuse</button>`;
        if (puedeActuar) html += `<button class="${itemClass} action-conclude text-green-600 font-bold"><i class="fas fa-flag-checkered"></i> <strong>Concluir</strong></button>`;
        html += `<button class="${itemClass} action-remove-acuse text-red-500"><i class="fas fa-times-circle"></i> Quitar Acuse</button>`;
        return html;
    }

    // RESTO DE ETAPAS
    if (puedeActuar) {
        const config = FLUJO_ETAPAS[etapa];
        if (config) {
            if (config.accion === 'enviarRevision' || config.accion === 'aprobar') html += `<button class="${itemClass} action-advance text-green-700"><i class="fas fa-check"></i> <strong>${config.label}</strong></button>`;
            if (config.accion === 'subirAcuse') html += `<button class="${itemClass} action-upload-acuse text-blue-700"><i class="fas fa-file-upload"></i> <strong>${config.label}</strong></button>`;
            if (config.anterior) html += `<button class="${itemClass} action-reject text-red-600"><i class="fas fa-times"></i> Rechazar</button>`;
        }
    }
    
    // Opciones Admin/Gerente
    if (rol === 'Gerente' || rol === 'Direccion' || rol === 'Subdireccion') {
        html += `<div class="border-t border-gray-100 my-1"></div>`;
        // html += `<button class="${itemClass} action-reasignar"><i class="fas fa-user-friends text-gray-400"></i> Reasignar</button>`; // Movido a Audiencias?
        if (rol === 'Direccion') {
            html += `<button class="${itemClass} action-delete text-red-600 font-bold hover:bg-red-50"><i class="fas fa-trash-alt"></i> Eliminar</button>`;
        }
    }
    return html;
}

// ===============================================
// 5. LISTENER INTELIGENTE
// ===============================================
function setupActionMenuListener() {
    const tbody = document.getElementById('terminos-body');
    if(!tbody) return;

    // Clonar para limpiar eventos antiguos
    const newTbody = tbody.cloneNode(true);
    tbody.parentNode.replaceChild(newTbody, tbody);
    
    // Volver a referenciar
    const cleanTbody = document.getElementById('terminos-body');
    
    // Listener Delegado
    cleanTbody.addEventListener('click', function(e) {
        const target = e.target.closest('button');
        if (!target) return;
        const row = target.closest('tr');
        if(!row) return;
        
        const id = row.getAttribute('data-id');
        // Buscar por ID como String para seguridad
        const termino = TERMINOS.find(t => String(t.id) === String(id));
        
        // --- MENÚ FLOTANTE ---
        if (target.classList.contains('action-menu-toggle')) {
             e.preventDefault(); e.stopPropagation();
             const menu = target.nextElementSibling;
             
             // Cerrar otros
             document.querySelectorAll('.action-menu').forEach(m => { 
                if(m !== menu) { m.classList.add('hidden'); m.style.cssText = ''; }
             });

             if (!menu) return;
             if (!menu.classList.contains('hidden')) { menu.classList.add('hidden'); menu.style.cssText = ''; return; }

             menu.classList.remove('hidden');
             const rect = target.getBoundingClientRect();
             const menuWidth = 224; const menuHeight = menu.offsetHeight || 220; const spaceBelow = window.innerHeight - rect.bottom;
             menu.style.position = 'fixed'; menu.style.zIndex = '99999'; menu.style.width = menuWidth + 'px'; menu.style.left = (rect.right - menuWidth) + 'px';
             
             if (spaceBelow < menuHeight) {
                 menu.style.top = 'auto'; menu.style.bottom = (window.innerHeight - rect.top + 5) + 'px';
                 menu.classList.remove('border-t-4'); menu.classList.add('border-b-4');
             } else {
                 menu.style.bottom = 'auto'; menu.style.top = (rect.bottom + 5) + 'px';
                 menu.classList.add('border-t-4'); menu.classList.remove('border-b-4');
             }
             return;
        }

        // --- ACCIONES ---
        if(target.classList.contains('action-edit')) openTerminoModalJS(termino);
        else if(target.classList.contains('action-advance')) avanzarEtapa(id);
        else if(target.classList.contains('action-reject')) regresarEtapa(id);
        else if(target.classList.contains('action-upload-acuse')) row.querySelector('.input-acuse-hidden').click();
        
        else if(target.classList.contains('action-download-acuse')) {
            mostrarAlertaTermino(`Descargando documento: ${termino.acuseDocumento}`);
        }
        else if(target.classList.contains('action-remove-acuse')) {
            mostrarConfirmacion(
                'Quitar Acuse',
                '¿Deseas quitar el acuse actual? \n\nEl término regresará al estado "Liberado".',
                () => quitarAcuseAPI(id) // Llamada a función API
            );
        }
        else if(target.classList.contains('action-conclude')) abrirModalPresentar(id, 'Concluir Término', 'Se marcará como finalizado.');
        else if(target.classList.contains('action-delete')) eliminarTerminoAPI(id);
        
        // Cerrar menú
        document.querySelectorAll('.action-menu').forEach(m => m.classList.add('hidden'));
    });

    // Cierres Globales
    document.addEventListener('click', e => {
        if (!e.target.closest('.action-menu-toggle') && !e.target.closest('.action-menu')) {
            document.querySelectorAll('.action-menu').forEach(m => { m.classList.add('hidden'); m.style.cssText = ''; });
        }
    });
    window.addEventListener('scroll', () => {
        document.querySelectorAll('.action-menu:not(.hidden)').forEach(m => { m.classList.add('hidden'); m.style.cssText = ''; });
    }, true);
    
    // Listener Input Archivo (Subida)
    cleanTbody.addEventListener('change', function(e) {
        if (e.target.classList.contains('input-acuse-hidden') && e.target.files.length > 0) {
            const id = e.target.getAttribute('data-id');
            // Aquí deberías subir el archivo real al servidor
            // Por ahora simulamos guardando el nombre
            actualizarAcuseAPI(id, e.target.files[0].name);
        }
    });
}

// ===============================================
// 6. TRANSICIONES Y CRUD (API)
// ===============================================

async function avanzarEtapa(id) {
    const termino = TERMINOS.find(t => String(t.id) === String(id));
    if (!termino) return;
    const config = FLUJO_ETAPAS[termino.estatus];
    
    if(config && config.siguiente) {
        if (termino.estatus === 'Dirección') {
             abrirModalPresentar(id, 'Liberar Término', 'El término pasará a estado "Liberado".');
             return;
        }
        mostrarConfirmacion('Avanzar Etapa', `¿Avanzar de "${termino.estatus}" a "${config.siguiente}"?`, async () => {
            try {
                // PATCH al Backend
                const res = await fetch(`${API_URL}/${id}/estatus?estatus=${config.siguiente}`, { method: 'PATCH' });
                if(res.ok) {
                    loadTerminos(); // Recargar tabla
                    mostrarMensajeGlobal(`Avanzado a ${config.siguiente}`, 'success');
                } else { throw new Error(); }
            } catch { mostrarMensajeGlobal('Error al actualizar en servidor', 'danger'); }
        });
    }
}

async function regresarEtapa(id) {
    const termino = TERMINOS.find(t => String(t.id) === String(id));
    const config = FLUJO_ETAPAS[termino.estatus];
    
    if(config && config.anterior) {
        mostrarPrompt('Rechazar Término', `Motivo del rechazo:`, '...', async (motivo) => {
            try {
                const res = await fetch(`${API_URL}/${id}/estatus?estatus=${config.anterior}`, { method: 'PATCH' });
                if(res.ok) {
                    loadTerminos();
                    mostrarMensajeGlobal(`Regresado a ${config.anterior}`, 'warning');
                }
            } catch { mostrarMensajeGlobal('Error de red', 'danger'); }
        });
    }
}

async function eliminarTerminoAPI(id) {
    mostrarConfirmacion('Eliminar', '¿Borrar permanentemente?', async () => {
        try {
            const res = await fetch(`${API_URL}/${id}`, { method: 'DELETE' });
            if(res.ok) {
                loadTerminos();
                mostrarMensajeGlobal('Eliminado correctamente', 'success');
            } else { throw new Error(); }
        } catch { mostrarMensajeGlobal('Error al eliminar', 'danger'); }
    });
}

// Funciones nuevas para Acuse (Spring Boot)
async function quitarAcuseAPI(id) {
    // Para "quitar acuse" y regresar estado, podemos hacer dos llamadas o tener un endpoint específico.
    // Aquí hacemos un update manual del objeto simulando la lógica.
    // Idealmente tu Backend debería tener un endpoint: /api/v1/terminos/{id}/acuse (DELETE)
    
    const termino = TERMINOS.find(t => String(t.id) === String(id));
    if(!termino) return;
    
    // Objeto con los cambios
    const updates = { 
        ...termino,
        acuseDocumento: null, 
        estatus: 'Liberado' 
    };

    try {
        const res = await fetch(`${API_URL}/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(updates)
        });
        
        if(res.ok) {
            loadTerminos();
            mostrarMensajeGlobal('Acuse eliminado. Estado regresado a Liberado.', 'warning');
        }
    } catch(e) {
        mostrarMensajeGlobal('Error al actualizar acuse', 'danger');
    }
}

async function actualizarAcuseAPI(id, fileName) {
    // Simulación: Actualizamos el nombre del archivo y el estado a Presentado
    const termino = TERMINOS.find(t => String(t.id) === String(id));
    const updates = {
        ...termino,
        acuseDocumento: fileName,
        estatus: 'Presentado' // Automáticamente pasa a presentado al subir
    };

    try {
        const res = await fetch(`${API_URL}/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(updates)
        });
        if(res.ok) {
            loadTerminos();
            mostrarMensajeGlobal('Acuse subido exitosamente.', 'success');
        }
    } catch(e) { mostrarMensajeGlobal('Error al subir archivo', 'danger'); }
}

// ===============================================
// 7. MODALES (MANUALES) - API
// ===============================================
function initModalTerminosJS() {
   const modal = document.getElementById('modal-termino');
   const btnSave = document.getElementById('save-termino');
   
   if (!modal) return;

   document.querySelectorAll('#close-modal-termino, #cancel-termino').forEach(btn => {
       if(btn) btn.onclick = () => { modal.classList.remove('flex'); modal.classList.add('hidden'); };
   });

   if(btnSave) {
       const newBtn = btnSave.cloneNode(true);
       btnSave.parentNode.replaceChild(newBtn, btnSave);
       newBtn.onclick = (e) => { e.preventDefault(); guardarTermino(); };
   }
}

function openTerminoModalJS(termino = null) {
    const modal = document.getElementById('modal-termino');
    const form = document.getElementById('form-termino');
    const title = document.getElementById('modal-termino-title');
    
    if(form) form.reset();
    cargarAsuntosEnSelectorJS();
    
    if (termino) {
        title.textContent = 'Editar Datos del Término';
        document.getElementById('termino-id').value = termino.id;
        
        const selAsunto = document.getElementById('asunto-selector');
        // Nota: Si tu backend devuelve 'asuntoId', úsalo aquí.
        if(selAsunto) selAsunto.value = termino.asuntoId || '';
        
        if(document.getElementById('fecha-ingreso')) document.getElementById('fecha-ingreso').value = termino.fechaIngreso || '';
        if(document.getElementById('fecha-vencimiento')) document.getElementById('fecha-vencimiento').value = termino.fechaVencimiento || '';
        if(document.getElementById('actuacion')) document.getElementById('actuacion').value = termino.asunto || '';
        
        // Disparar carga de datos de expediente si hay ID
        if(termino.asuntoId) cargarDatosAsuntoEnModalJS(termino.asuntoId);

    } else {
        title.textContent = 'Nuevo Término';
        document.getElementById('termino-id').value = '';
    }
    
    modal.classList.remove('hidden'); modal.classList.add('flex');
}

async function guardarTermino() {
    const id = document.getElementById('termino-id').value;
    
    // Objeto JSON para el Backend
    const data = {
        asuntoId: document.getElementById('asunto-selector')?.value,
        fechaIngreso: document.getElementById('fecha-ingreso')?.value,
        fechaVencimiento: document.getElementById('fecha-vencimiento')?.value,
        asunto: document.getElementById('actuacion')?.value, // "Asunto" en DB = Actuación
        
        // Campos Readonly (se envían para consistencia, aunque el backend podría ignorarlos o re-buscarlos)
        expediente: document.getElementById('termino-expediente')?.value, 
        actor: document.getElementById('termino-partes')?.value,
        abogado: document.getElementById('termino-abogado')?.value,
        estatus: 'Proyectista' // Default si es nuevo
    };

    if(!data.fechaVencimiento) return mostrarMensajeGlobal('Faltan campos obligatorios', 'danger');

    try {
        let url = API_URL;
        let method = 'POST';

        if(id) {
            url = `${API_URL}/${id}`;
            method = 'PUT';
            data.id = id;
            // Preservar estatus si se edita
            const actual = TERMINOS.find(t => String(t.id) === String(id));
            if(actual) data.estatus = actual.estatus;
        }

        const res = await fetch(url, {
            method: method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });

        if(res.ok) {
            document.getElementById('modal-termino').classList.remove('flex');
            document.getElementById('modal-termino').classList.add('hidden');
            loadTerminos(); // Recargar de la BD
            mostrarMensajeGlobal('Guardado exitosamente', 'success');
        } else {
            throw new Error();
        }
    } catch (e) {
        mostrarMensajeGlobal('Error al guardar en el servidor', 'danger');
    }
}

// --- Helpers de Modales ---
function initModalReasignar() {
    // Si la funcionalidad no está en Términos, puedes dejar esta función vacía o eliminarla
    // En tu versión anterior estaba aquí, la dejo por compatibilidad si la requieres.
}
function abrirModalReasignar(id) {
    // Ídem
}

function initModalPresentar() {
    const modal = document.getElementById('modal-presentar-termino');
    if (!modal) return;
    const btnConfirm = document.getElementById('confirmar-presentar');
    document.querySelectorAll('#close-modal-presentar, #cancel-presentar').forEach(btn => btn.onclick = () => { modal.classList.remove('flex'); modal.classList.add('hidden'); });
    if(btnConfirm) {
        const newBtn = btnConfirm.cloneNode(true);
        btnConfirm.parentNode.replaceChild(newBtn, btnConfirm);
        newBtn.onclick = async () => {
            const id = document.getElementById('presentar-termino-id').value;
            const termino = TERMINOS.find(t => String(t.id) === String(id));
            if(termino) {
                // Lógica de Concluir (Presentado -> Concluido)
                // O lógica de avance normal si es otro estado
                let nuevoEstado = '';
                if (termino.estatus === 'Presentado') {
                    nuevoEstado = 'Concluido';
                } else {
                    nuevoEstado = FLUJO_ETAPAS[termino.estatus]?.siguiente;
                }

                if(nuevoEstado) {
                     try {
                        const res = await fetch(`${API_URL}/${id}/estatus?estatus=${nuevoEstado}`, { method: 'PATCH' });
                        if(res.ok) {
                            loadTerminos();
                            mostrarMensajeGlobal('Estatus actualizado', 'success');
                        }
                    } catch(e) { mostrarMensajeGlobal('Error al actualizar', 'danger'); }
                }
                modal.classList.remove('flex'); modal.classList.add('hidden');
            }
        };
    }
}

function abrirModalPresentar(id, titulo, mensaje) {
    const modal = document.getElementById('modal-presentar-termino');
    document.getElementById('presentar-termino-id').value = id;
    // Opcional: Actualizar textos del modal si tienes elementos para ello
    modal.classList.remove('hidden'); modal.classList.add('flex');
}

// ===============================================
// 8. HELPERS & MODALES GLOBALES
// ===============================================
function calcularDiasRestantes(fechaVencimiento) {
    if (!fechaVencimiento) return null;
    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);
    // Parseo seguro YYYY-MM-DD
    const parts = fechaVencimiento.split('-');
    if(parts.length !== 3) return null;
    const vencimiento = new Date(parts[0], parts[1] - 1, parts[2]); 
    const diferenciaMs = vencimiento - hoy;
    return Math.ceil(diferenciaMs / (1000 * 60 * 60 * 24));
}

function getSemaforoColor(fecha) {
    const dias = calcularDiasRestantes(fecha);
    if (dias === null) return 'bg-gray-300';
    if (dias < 0) return 'bg-red-700';      
    if (dias === 0) return 'bg-red-500 animate-pulse'; 
    if (dias <= 3) return 'bg-orange-500';  
    if (dias <= 7) return 'bg-yellow-400';  
    return 'bg-green-500';
}

function getBadgeClass(estatus) {
    switch(estatus) {
        case 'Proyectista': return 'bg-gray-100 text-gray-700 border-gray-200';
        case 'Liberado': return 'bg-green-100 text-green-800 border-green-200';
        case 'Concluido': return 'bg-gob-verde text-white border-gob-verdeDark';
        case 'Presentado': return 'bg-blue-100 text-blue-800 border-blue-200';
        default: return 'bg-yellow-100 text-yellow-800 border-yellow-200'; 
    }
}

// --- MODAL CONFIRMACION ---
let onConfirmAction = null;
function mostrarConfirmacion(titulo, mensaje, callback) {
    const modal = document.getElementById('modal-confirmacion-global');
    if (!modal) return;
    document.getElementById('confirm-titulo').textContent = titulo;
    document.getElementById('confirm-mensaje').textContent = mensaje;
    onConfirmAction = callback;
    document.getElementById('btn-confirm-accept').onclick = function() {
        if (onConfirmAction) onConfirmAction();
        cerrarConfirmacion();
    };
    document.getElementById('btn-confirm-cancel').onclick = cerrarConfirmacion;
    modal.classList.remove('hidden'); modal.classList.add('flex');
}
function cerrarConfirmacion() {
    const modal = document.getElementById('modal-confirmacion-global');
    if (modal) { modal.classList.remove('flex'); modal.classList.add('hidden'); }
    onConfirmAction = null;
}

// --- MODAL PROMPT ---
let onPromptAction = null; 
function mostrarPrompt(titulo, mensaje, placeholder, callback) {
    const modal = document.getElementById('modal-prompt-global');
    if (!modal) return;
    document.getElementById('prompt-titulo').textContent = titulo;
    document.getElementById('prompt-mensaje').textContent = mensaje;
    const input = document.getElementById('prompt-input');
    input.placeholder = placeholder || '...';
    input.value = '';
    onPromptAction = callback;
    document.getElementById('btn-prompt-accept').onclick = function() {
        if(input.value.trim()) { if(onPromptAction) onPromptAction(input.value.trim()); cerrarPrompt(); }
    };
    document.getElementById('btn-prompt-cancel').onclick = cerrarPrompt;
    modal.classList.remove('hidden'); modal.classList.add('flex');
}
function cerrarPrompt() {
    const modal = document.getElementById('modal-prompt-global');
    if (modal) { modal.classList.remove('flex'); modal.classList.add('hidden'); }
    onPromptAction = null;
}

// --- MODAL ALERTA ---
function mostrarAlertaTermino(mensaje) {
    const modal = document.getElementById('modal-alerta-termino');
    if(!modal) return;
    document.getElementById('alerta-mensaje-termino').textContent = mensaje;
    document.getElementById('btn-alerta-accept-termino').onclick = () => { modal.classList.remove('flex'); modal.classList.add('hidden'); };
    modal.classList.remove('hidden'); modal.classList.add('flex');
}

// --- Helpers ---
function mostrarMensajeGlobal(msg, type) {
    const div = document.createElement('div');
    const color = type === 'success' ? 'bg-green-500' : 'bg-red-500';
    div.className = `fixed top-5 right-5 px-6 py-3 text-white rounded shadow-lg z-[100] ${color} animate-fade-in-down`;
    div.innerText = msg;
    document.body.appendChild(div);
    setTimeout(() => div.remove(), 3000);
}

// Carga de selectores (Simulada o conectada a otra API)
function cargarAsuntosEnSelectorJS() {
    const sel = document.getElementById('asunto-selector');
    if(!sel) return;
    
    // Aquí podrías hacer fetch('/api/v1/expedientes') si tuvieras esa API
    // Por ahora leemos de localStorage para no romper la demo si no tienes esa API lista
    const expedientesData = JSON.parse(localStorage.getItem('expedientesData')) || [];
    
    sel.innerHTML = '<option value="">Seleccionar...</option>';
    expedientesData.forEach(e => {
        const opt = document.createElement('option');
        opt.value = e.id;
        opt.text = `${e.numero || e.expediente} - ${e.descripcion ? e.descripcion.substring(0,30)+'...' : ''}`;
        sel.appendChild(opt);
    });
    
    sel.onchange = () => {
        const e = expedientesData.find(x => String(x.id) === sel.value);
        if(e) {
            if(document.getElementById('termino-expediente')) document.getElementById('termino-expediente').value = e.numero || e.expediente || '';
            if(document.getElementById('termino-materia')) document.getElementById('termino-materia').value = e.materia || '';
            if(document.getElementById('termino-gerencia')) document.getElementById('termino-gerencia').value = e.gerencia || '';
            if(document.getElementById('termino-abogado')) document.getElementById('termino-abogado').value = e.abogado || e.abogadoResponsable || '';
            if(document.getElementById('termino-partes')) document.getElementById('termino-partes').value = e.partes || e.partesProcesales || 'Actor vs Demandado';
        }
    };
}

function cargarDatosAsuntoEnModalJS(asuntoId) {
    const selector = document.getElementById('asunto-selector');
    if(selector) { selector.value = asuntoId; selector.dispatchEvent(new Event('change')); }
}

function cargarAbogadosSelector() {
    // También podría ser fetch('/api/v1/usuarios?rol=ABOGADO')
    const sel = document.getElementById('select-nuevo-abogado');
    if(!sel) return;
    sel.innerHTML = '<option value="">Seleccionar...</option><option value="Lic. A">Lic. A</option>';
}

function formatDate(dateString) {
    if (!dateString) return 'N/A';
    const parts = dateString.split('-');
    if(parts.length === 3) {
        // Asumiendo formato YYYY-MM-DD
        const date = new Date(parts[0], parts[1] - 1, parts[2]); 
        return date.toLocaleDateString('es-ES', { day: '2-digit', month: '2-digit', year: 'numeric' });
    }
    return dateString;
}

function exportarTablaExcel() {
    const table = document.querySelector('table');
    const wb = XLSX.utils.table_to_book(table);
    XLSX.writeFile(wb, 'Terminos.xlsx');
}