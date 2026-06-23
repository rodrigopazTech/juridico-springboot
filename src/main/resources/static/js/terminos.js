/**
 * Lógica para el módulo de Términos
 * Autor: Ricardo Villalobos
 */

document.addEventListener('DOMContentLoaded', function () {
    inicializarSelectsTerminos();
    //console.log("Sistema de Terminos Cargado");
    initMiniCalendar();
    initSearchTerminos();

    // Si la navegación viene de notificaciones con un estatus objetivo, hacemos scroll y parpadeo
    const urlParams = new URLSearchParams(window.location.search);
    const estatusObj = urlParams.get('estatus');
    if (estatusObj) {
        setTimeout(() => {
            animarEtapaPorEstatus(estatusObj);
        }, 150);
    }


    // D. Paginación Asíncrona (Delegación Global)
    document.addEventListener('click', function (e) {
        // 1. Paginación
        const linkPag = e.target.closest('.pagina-link-ter');
        if (linkPag) {
            e.preventDefault();
            const url = linkPag.getAttribute('href');
            fetch(url, { headers: { 'X-Requested-With': 'XMLHttpRequest' } })
                .then(res => res.text())
                .then(html => {
                    const parser = new DOMParser();
                    const doc = parser.parseFromString(html, 'text/html');
                    const nuevoMaestro = doc.getElementById('grid-maestro-terminos');
                    const maestroDestino = document.getElementById('grid-maestro-terminos');
                    if (nuevoMaestro && maestroDestino) {
                        maestroDestino.innerHTML = nuevoMaestro.innerHTML;
                    }
                    window.history.replaceState({ path: url }, '', url);
                })
                .catch(err => console.error("Error en paginación asíncrona:", err));
            return;
        }

        // 1.5 Avanzar Etapa Asíncrono
        const btnAvanzar = e.target.closest('.btn-avanzar-etapa');
        if (btnAvanzar) {
            e.preventDefault();
            const url = btnAvanzar.getAttribute('href');
            Swal.fire({
                title: 'Avanzando etapa...',
                allowOutsideClick: false,
                didOpen: () => {
                    Swal.showLoading();
                }
            });
            fetch(url, {
                headers: { 'X-Requested-With': 'XMLHttpRequest' }
            })
                .then(res => {
                    Swal.close();
                    if (res.ok) {
                        showSuccessToast('¡Etapa avanzada correctamente!');
                    } else {
                        showErrorModal('No se pudo avanzar la etapa del término.');
                    }
                })
                .catch(err => {
                    Swal.close();
                    console.error("Error al avanzar etapa:", err);
                    showErrorModal('Error de red al intentar avanzar la etapa.');
                });
            return;
        }

        // 2. Botón Limpiar Búsqueda (Barra Superior)
        const btnClear = e.target.closest('#btn-clear-search-terminos');
        if (btnClear) {
            const input = document.getElementById('input-search-terminos');
            if (input) input.value = '';
            actualizarTablaAsincrono();
            return;
        }

        // 3. Botón Limpiar Filtros (Sidebar)
        const btnLimpiar = e.target.closest('#btn-limpiar-filtros-ter');
        if (btnLimpiar) {
            const form = document.getElementById('form-filtros');
            if (form) {
                form.querySelectorAll('select').forEach(s => s.value = '');
            }
            const input = document.getElementById('input-search-terminos');
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
        if (e.target.id === 'input-search-terminos') {
            const input = e.target;
            const btnClear = document.getElementById('btn-clear-search-terminos');
            if (input.value) {
                btnClear?.classList.remove('hidden');
            } else {
                btnClear?.classList.add('hidden');
            }

            clearTimeout(debounceTimerTerminos);
            debounceTimerTerminos = setTimeout(() => {
                actualizarTablaAsincrono();
            }, 650);
        }
    });

    // F. Submit del formulario de búsqueda (Evitar recarga)
    document.addEventListener('submit', function (e) {
        if (e.target.id === 'form-search-terminos') {
            e.preventDefault();
            actualizarTablaAsincrono();
        }
    });

    // G. Filtros del sidebar (Delegación para que no sea necesario rebindear)
    document.addEventListener('change', function (e) {
        if (e.target.closest('#form-filtros')) {
            actualizarTablaAsincrono();
        }
    });
    // H. Iniciar conexión WebSocket
    iniciarTerminosWebSocket();

    // I. Event listener para recarga de materias y abogados según gerencia
    document.addEventListener('change', function (e) {
        if (e.target.id === 'select-contexto-gerencia') {
            const origen = document.querySelector('input[name="origenTermino"]:checked')?.value || 'EXPEDIENTE';
            if (origen === 'ASUNTO_UNICO') {
                cargarMateriasYAbogadosPorGerencia(e.target.value);
            }
        }
    });
});

// --- BÚSQUEDA ASÍNCRONA CENTRALIZADA ---
let debounceTimerTerminos;
let abortControllerTerminos;

function actualizarTablaAsincrono(keywordForzado = null) {
    const input = document.getElementById('input-search-terminos');
    const container = document.getElementById('tabla-resultados-container');

    if (!container) return;

    // Abortar petición previa si existe
    if (abortControllerTerminos) {
        abortControllerTerminos.abort();
    }
    abortControllerTerminos = new AbortController();

    const urlParams = new URLSearchParams();

    // Preservar ordenamiento actual de la barra de direcciones
    const currentParams = new URLSearchParams(window.location.search);
    const sortBy = currentParams.get('sortBy');
    const sortDir = currentParams.get('sortDir');
    if (sortBy) urlParams.set('sortBy', sortBy);
    if (sortDir) urlParams.set('sortDir', sortDir);

    // 1. Keyword (Buscador o Fecha del Calendario)
    const valKeyword = keywordForzado !== null ? keywordForzado : (input ? input.value : '');
    if (valKeyword) urlParams.set('keyword', valKeyword);

    // 2. Mes y Año del Calendario (Solo si el filtro de fecha está activo)
    if (miniCalendarState.isDateFilterActive) {
        const date = miniCalendarState.currentDate;
        urlParams.set('mes', date.getMonth() + 1);
        urlParams.set('anio', date.getFullYear());
    }

    // 3. Filtros del Lateral
    const formFiltros = document.getElementById('form-filtros');
    let filterCount = 0;
    if (formFiltros) {
        const fd = new FormData(formFiltros);
        for (const [key, value] of fd.entries()) {
            if (key === 'mes' || key === 'anio' || key === 'keyword' || key === '_csrf') continue;
            if (value && value !== 'null' && value !== '') {
                urlParams.set(key, value);
                filterCount++;
            }
        }
    }

    // 4. Actualizar Indicador Visual (Badge con Conteo)
    const badge = document.getElementById('badge-filtros-ter');
    if (badge) {
        if (filterCount > 0) {
            badge.innerText = filterCount;
            badge.classList.remove('hidden');
        } else {
            badge.classList.add('hidden');
        }
    }

    urlParams.set('page', 0);

    const newUrl = window.location.pathname + '?' + urlParams.toString();
    window.history.replaceState({ path: newUrl }, '', newUrl);

    fetch('/terminos?' + urlParams.toString(), {
        headers: { 'X-Requested-With': 'XMLHttpRequest' },
        signal: abortControllerTerminos.signal
    })
        .then(res => res.text())
        .then(html => {
            const parser = new DOMParser();
            const doc = parser.parseFromString(html, 'text/html');

            // 1. ACTUALIZAR KPIs (Sin afectar al resto)
            const kpisNuevos = doc.getElementById('contenedor-kpis-terminos');
            const kpisActuales = document.getElementById('contenedor-kpis-terminos');
            if (kpisNuevos && kpisActuales) {
                kpisActuales.innerHTML = kpisNuevos.innerHTML;
            }

            // 2. ACTUALIZAR TABLA DE RESULTADOS ESCRITORIO
            const tablaNueva = doc.getElementById('tabla-resultados-container');
            const tablaActual = document.getElementById('tabla-resultados-container');
            if (tablaNueva && tablaActual) {
                tablaActual.innerHTML = tablaNueva.innerHTML;
            }

            // 2b. ACTUALIZAR VISTA MÓVIL
            const movilNuevo = doc.getElementById('tabla-resultados-movil-container');
            const movilActual = document.getElementById('tabla-resultados-movil-container');
            if (movilNuevo && movilActual) {
                movilActual.innerHTML = movilNuevo.innerHTML;
            }

            // 2c. ACTUALIZAR PAGINACIÓN
            const paginacionNueva = doc.getElementById('paginacion-container');
            const paginacionActual = document.getElementById('paginacion-container');
            if (paginacionNueva && paginacionActual) {
                paginacionActual.innerHTML = paginacionNueva.innerHTML;
            }

            // 3. ACTUALIZAR ACCIONES (VerTodos y Exportar que dependen de filtros)
            const accionesNuevas = doc.getElementById('acciones-barra-terminos');
            const accionesActuales = document.getElementById('acciones-barra-terminos');
            if (accionesNuevas && accionesActuales) {
                accionesActuales.innerHTML = accionesNuevas.innerHTML;
            }

            // 4. SINCRONIZAR INPUT (Solo si se forzó desde fuera, ej: KPIs)
            const searchInput = document.getElementById('input-search-terminos');
            if (keywordForzado !== null && searchInput) {
                searchInput.value = keywordForzado;
                const btnClear = document.getElementById('btn-clear-search-terminos');
                if (keywordForzado) btnClear?.classList.remove('hidden');
                else btnClear?.classList.add('hidden');
            }

            // Actualizar también los eventos e indicadores del mini calendario
            fetchMiniCalendarEvents().then(() => {
                renderMiniCalendar();
            });
        })
        .catch(err => {
            if (err.name === 'AbortError') return; // Ignorar si fue abortada
            console.error("Error en actualización asíncrona:", err);
        });
}

function initSearchTerminos() {
    // Implementación asíncrona vía delegación global
}

// --- MENÚS DESPLEGABLES ---
function toggleMenu(id) {
    document.querySelectorAll('[id^="menu-"]:not([id^="menu-mob-"])').forEach(menu => {
        if (menu.id !== 'menu-' + id) {
            menu.classList.add('hidden');
            menu.removeAttribute('style');
        }
    });

    const menu = document.getElementById('menu-' + id);
    if (!menu) return;

    const isHidden = menu.classList.contains('hidden');
    if (isHidden) {
        menu.classList.remove('hidden');
        const btn = menu.parentElement.querySelector('button[onclick*="toggleMenu"]');
        if (btn) {
            const rect = btn.getBoundingClientRect();
            menu.style.position = 'fixed';
            menu.style.zIndex = '9999';
            menu.style.top = `${rect.bottom + 4}px`;
            const menuWidth = 256; // Ancho del menú (w-64)
            menu.style.left = `${rect.right - menuWidth}px`;
        }
    } else {
        menu.classList.add('hidden');
        menu.removeAttribute('style');
    }
}

// Cierra el menú si haces clic fuera de él
window.addEventListener('click', function (e) {
    if (!e.target.closest('td.relative')) {
        document.querySelectorAll('[id^="menu-"]:not([id^="menu-mob-"])').forEach(menu => {
            menu.classList.add('hidden');
            menu.removeAttribute('style');
        });
    }
});

// Cerrar menús al hacer scroll o cambiar tamaño de pantalla para evitar desalineación
window.addEventListener('scroll', function () {
    document.querySelectorAll('[id^="menu-"]:not([id^="menu-mob-"])').forEach(menu => {
        menu.classList.add('hidden');
        menu.removeAttribute('style');
    });
}, { passive: true });

window.addEventListener('resize', function () {
    document.querySelectorAll('[id^="menu-"]:not([id^="menu-mob-"])').forEach(menu => {
        menu.classList.add('hidden');
        menu.removeAttribute('style');
    });
});

// --- LÍNEA DE TIEMPO (NUEVO) ---
function toggleTimeline(id) {
    const row = document.getElementById('timeline-' + id);
    const btn = document.getElementById('btn-expand-' + id);

    if (row) {
        row.classList.toggle('hidden');
        if (btn) {
            // Rotar flecha
            if (!row.classList.contains('hidden')) {
                btn.classList.add('rotate-180', 'text-gob-guinda');
                btn.classList.remove('text-gray-400');
            } else {
                btn.classList.remove('rotate-180', 'text-gob-guinda');
                btn.classList.add('text-gray-400');
            }
        }
    }
}

// Parpadeo local de la etapa cuando se navega desde una notificación
function animarEtapaPorEstatus(estatusObj) {
    try {
        const targets = [];
        const targetDesk = document.getElementById('etapa-' + estatusObj);
        const targetMob = document.getElementById('etapa-mob-' + estatusObj);

        if (targetDesk) targets.push(targetDesk);
        if (targetMob) targets.push(targetMob);

        if (targets.length === 0) {
            const fallbacks = document.querySelectorAll('[data-etapa-hidden="true"][value="' + estatusObj + '"]');
            fallbacks.forEach(fb => {
                if (fb.parentElement) {
                    targets.push(fb.parentElement);
                }
            });
        }

        if (targets.length === 0) return;

        // Scroll al primer elemento que esté visible en la pantalla
        const firstVisible = targets.find(t => t.offsetWidth > 0 || t.offsetHeight > 0) || targets[0];
        firstVisible.scrollIntoView({ behavior: 'smooth', block: 'center' });

        targets.forEach(target => {
            target.classList.remove('ring-4', 'ring-gob-guinda/60');
            target.classList.add('ring-2', 'ring-cyan');
            target.classList.add('shadow-[0_0_30px_cyan]');
        });

        let toggles = 0;
        const interval = setInterval(() => {
            const connectedTargets = targets.filter(t => t.isConnected);
            if (connectedTargets.length === 0) {
                clearInterval(interval);
                return;
            }

            if (toggles >= 12) {
                clearInterval(interval);
                connectedTargets.forEach(target => {
                    target.classList.remove('ring-2', 'ring-cyan', 'shadow-[0_0_30px_cyan]');
                    target.classList.remove('ring-4');
                });
                return;
            }

            connectedTargets.forEach(target => {
                target.classList.toggle('ring-4');
                target.classList.toggle('ring-2');
            });
            toggles++;

        }, 250);
    } catch (e) {
        // silencioso
    }
}

// --- CONEXIÓN WEBlet activeLocks = {};

function iniciarTerminosWebSocket() {
    if (typeof window.WebSocketManager === 'undefined') {
        console.warn("WebSocketManager global no encontrado. Reintentando en 500ms...");
        setTimeout(iniciarTerminosWebSocket, 500);
        return;
    }

    // Suscribirse a alertas globales de términos
    window.WebSocketManager.subscribe('/topic/alertas', function (message) {
        try {
            const payload = JSON.parse(message.body);
            if (payload.tabla === 'terminos') {
                actualizarTablaAsincrono();

                // Notificación de eliminación en tiempo real para otros usuarios
                if (payload.operacion === 'DELETE') {
                    if (window.justDeletedTermId !== payload.id) {
                        Toast.fire({
                            icon: 'info',
                            title: `El término "${payload.titulo}" ha sido eliminado.`
                        });
                    } else {
                        delete window.justDeletedTermId;
                    }
                }

                // Advertencia de modificación en tiempo real para el usuario que tiene abierto el modal
                if (payload.operacion === 'UPDATE') {
                    const idInput = document.getElementById('input-termino-id');
                    if (idInput && idInput.value && parseInt(idInput.value) === parseInt(payload.id)) {
                        const banner = document.getElementById('banner-concurrencia');
                        const mensaje = document.getElementById('mensaje-concurrencia');
                        
                        // Bloquear botón de guardar
                        const btnGuardar = document.getElementById('btn-guardar-termino');
                        if (btnGuardar) {
                            btnGuardar.disabled = true;
                            btnGuardar.classList.add('opacity-50', 'cursor-not-allowed');
                        }

                        if (banner && mensaje) {
                            mensaje.innerHTML = `⚠️ Atención: Este término ha sido modificado recientemente por otro usuario. El botón de guardar se ha desactivado. <button type="button" onclick="recargarDatosTerminoActual()" class="underline font-bold text-red-900 hover:text-red-950 ml-2">Recargar Datos</button>`;
                            banner.classList.remove('hidden');
                            banner.classList.remove('bg-yellow-50', 'border-yellow-400', 'text-yellow-700');
                            banner.classList.add('bg-red-50', 'border-red-500', 'text-red-700');
                        }
                        
                        // Mostrar Toast de advertencia crítica con opción de recargar de inmediato
                        Swal.fire({
                            title: 'Término Modificado',
                            text: 'Este término ha sido modificado en la base de datos por otro usuario. El botón de guardar se ha desactivado temporalmente para evitar sobrescribir los datos.',
                            icon: 'warning',
                            showCancelButton: true,
                            confirmButtonText: 'Recargar datos',
                            cancelButtonText: 'Entendido',
                            confirmButtonColor: '#bc955c',
                            cancelButtonColor: '#6b7280'
                        }).then((result) => {
                            if (result.isConfirmed) {
                                recargarDatosTerminoActual();
                            }
                        });
                    }
                }

                if (payload.etapa_procesal) {
                    setTimeout(() => {
                        animarEtapaPorEstatus(payload.etapa_procesal);
                    }, 1000);
                }
            }
        } catch (e) {
            console.error("Error al procesar mensaje de WebSocket:", e);
        }
    });

    // Suscribirse a bloqueos de edición de términos
    window.WebSocketManager.subscribe('/topic/terminos/locks', function (message) {
        try {
            const locks = JSON.parse(message.body);
            onLocksReceived(locks);
        } catch (e) {
            console.error("Error al procesar mensaje de locks:", e);
        }
    });
}

function sendTerminoLock(terminoId) {
    const stomp = window.WebSocketManager && window.WebSocketManager.stompClient;
    if (stomp && stomp.connected) {
        const nombreInput = document.getElementById('hdnUsuarioNombre');
        const nombre = nombreInput ? nombreInput.value : '';
        stomp.send('/app/terminos/lock', {}, JSON.stringify({
            terminoId: terminoId,
            nombreCompleto: nombre
        }));
    }
}

function sendTerminoUnlock(terminoId) {
    const stomp = window.WebSocketManager && window.WebSocketManager.stompClient;
    if (stomp && stomp.connected) {
        stomp.send('/app/terminos/unlock', {}, JSON.stringify({
            terminoId: terminoId
        }));
    }
}

function onLocksReceived(locks) {
    activeLocks = locks || {};
    
    const idInput = document.getElementById('input-termino-id');
    const myEmailInput = document.getElementById('hdnUsuarioEmail');
    
    if (idInput && idInput.value && myEmailInput) {
        const currentTermId = parseInt(idInput.value);
        const myEmail = myEmailInput.value;
        const lockUsers = activeLocks[currentTermId];
        
        const banner = document.getElementById('banner-concurrencia');
        const mensaje = document.getElementById('mensaje-concurrencia');
        
        // Filtrar a mí mismo para ver si hay otros editando
        const otherEditors = Object.keys(lockUsers || {})
            .filter(email => email !== myEmail)
            .map(email => lockUsers[email]);
            
        if (otherEditors.length > 0) {
            if (banner && mensaje) {
                const nombres = otherEditors.join(', ');
                mensaje.innerHTML = `⚠️ Atención: El/La Lic. ${nombres} también está editando este término en este momento.`;
                banner.classList.remove('hidden');
                
                // Mostrar alerta estilo Toast
                Toast.fire({
                    icon: 'warning',
                    title: `Edición concurrente: ${nombres} está editando este término.`
                });
            }
        } else {
            if (banner) banner.classList.add('hidden');
        }
    }
}


// --- SUBIDA DE ARCHIVOS ---
function activarSubida(idTermino) {
    const inputId = document.getElementById('upload-termino-id');
    const inputFile = document.getElementById('upload-file-input');

    if (inputId && inputFile) {
        inputId.value = idTermino;
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
        customClass: { title: 'text-green-800 text-sm font-bold' }
    });
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

function confirmarEliminar(id) {
    Swal.fire({
        title: '¿Eliminar término?',
        text: "Esta acción no se puede deshacer",
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#d33',
        cancelButtonColor: '#3085d6',
        confirmButtonText: 'Sí, eliminar',
        cancelButtonText: 'Cancelar'
    }).then((result) => {
        if (result.isConfirmed) {
            window.justDeletedTermId = id;
            // ROD-186: Usar AJAX para eliminar y preservar filtros
            fetch('/terminos/eliminar/' + id, {
                method: 'POST',
                headers: { 'X-Requested-With': 'XMLHttpRequest' }
            })
                .then(res => {
                    if (res.ok) {
                        showSuccessToast('¡Término eliminado correctamente!');
                        actualizarTablaAsincrono();
                    } else {
                        throw new Error('Error al eliminar');
                    }
                })
                .catch(err => {
                    showErrorModal('No se pudo eliminar el registro.');
                });
        }
    })
}

// ROD-186: Listeners para subida de archivos y conclusión vía AJAX (Preservar contexto)

// A. Subida de Archivo General (Form oculto en index)
document.addEventListener('change', function (e) {
    if (e.target && e.target.id === 'upload-file-input') {
        const form = document.getElementById('form-upload');
        if (!form) return;

        const fd = new FormData(form);
        Swal.fire({ title: 'Subiendo archivo...', allowOutsideClick: false, didOpen: () => Swal.showLoading() });

        fetch(form.action, {
            method: 'POST',
            body: fd,
            headers: { 'X-Requested-With': 'XMLHttpRequest' }
        })
            .then(res => {
                if (res.ok) {
                    showSuccessToast('¡Archivo subido correctamente!');
                    actualizarTablaAsincrono();
                } else throw new Error();
            })
            .catch(() => showErrorModal('No se pudo subir el archivo.'));
    }
});

// B. Concluir Término (Form en modal)
document.addEventListener('submit', function (e) {
    // Buscar si el form es el de concluir (está dentro del modal-concluir-final)
    const formConcluir = e.target.closest('#modal-concluir-final form');
    if (formConcluir) {
        e.preventDefault();
        const fd = new FormData(formConcluir);

        fetch(formConcluir.action, {
            method: 'POST',
            body: fd,
            headers: { 'X-Requested-With': 'XMLHttpRequest' }
        })
            .then(res => {
                if (res.ok) {
                    cerrarModal('modal-concluir-final');
                    showSuccessToast('¡Término Concluido con éxito!');
                    actualizarTablaAsincrono();
                } else throw new Error();
            })
            .catch(() => showErrorModal('No se pudo concluir el término.'));
    }

    // C. Subir Acuse (Form en modal acuse)
    const formAcuse = e.target.closest('#modal-subir-acuse form');
    if (formAcuse) {
        e.preventDefault();
        const fd = new FormData(formAcuse);
        Swal.fire({ title: 'Subiendo acuse...', allowOutsideClick: false, didOpen: () => Swal.showLoading() });

        fetch(formAcuse.action, {
            method: 'POST',
            body: fd,
            headers: { 'X-Requested-With': 'XMLHttpRequest' }
        })
            .then(res => {
                if (res.ok) {
                    cerrarModal('modal-subir-acuse');
                    showSuccessToast('¡Acuse registrado correctamente!');
                    actualizarTablaAsincrono();
                } else throw new Error();
            })
            .catch(() => showErrorModal('No se pudo registrar el acuse.'));
    }

    // D. Guardar/Editar Término
    const formTermino = e.target.closest('#modal-nuevo-termino form');
    if (formTermino) {
        e.preventDefault();
        const fd = new FormData(formTermino);
        Swal.fire({ title: 'Guardando...', allowOutsideClick: false, didOpen: () => Swal.showLoading() });

        fetch(formTermino.action, {
            method: 'POST',
            body: fd,
            headers: { 'X-Requested-With': 'XMLHttpRequest' }
        })
            .then(res => {
                if (res.ok) {
                    cerrarModal('modal-nuevo-termino');
                    showSuccessToast('¡Datos guardados correctamente!');
                    actualizarTablaAsincrono();
                } else throw new Error();
            })
            .catch(() => showErrorModal('Error al guardar el término.'));
    }
});


// --- MODALES (NUEVO / EDITAR) ---



function activarSubidaAcuse(idTermino) {
    const inputId = document.getElementById('input-acuse-id');
    const inputFile = document.getElementById('input-acuse-file');
    if (inputId) {
        inputId.value = idTermino;
        if (inputFile) inputFile.value = ''; // Limpiar archivo previo
        const modal = document.getElementById('modal-subir-acuse');
        if (modal) {
            modal.classList.remove('hidden');
            modal.classList.add('flex');
        }
    }
}


// 1. ABRIR MODAL NUEVO (Limpio)
function abrirModalNuevo() {
    const idInput = document.getElementById('input-termino-id');
    const expSelect = document.getElementById('select-expediente-termino');
    const asuntoInput = document.getElementById('input-asunto');
    const vencimientoInput = document.getElementById('input-vencimiento');
    const abogadoSelect = document.getElementById('select-abogado-termino');
    const observacionesInput = document.getElementById('input-observaciones-termino');
    const titulo = document.getElementById('modal-titulo');

    // Limpiar campos
    if (idInput) idInput.value = "";
    if (expSelect) {
        if (expSelect.tomselect) expSelect.tomselect.setValue('');
        else expSelect.value = "";
    }
    if (asuntoInput) asuntoInput.value = "";
    if (vencimientoInput) vencimientoInput.value = "";
    if (observacionesInput) observacionesInput.value = "";
    ['select-contexto-materia', 'input-contexto-descripcion', 'input-contexto-partes', 'input-contexto-referencia']
        .forEach(id => {
            const el = document.getElementById(id);
            if (el) {
                if (el.tomselect) el.tomselect.setValue('');
                else el.value = "";
            }
        });
    const gerenciaSelect = document.getElementById('select-contexto-gerencia');
    if (gerenciaSelect) {
        if (gerenciaSelect.tomselect) {
            gerenciaSelect.tomselect.setValue('');
        } else {
            if (gerenciaSelect.options.length === 2) {
                gerenciaSelect.selectedIndex = 1;
            } else {
                gerenciaSelect.value = "";
            }
        }
    }

    document.querySelectorAll('input[name="origenTermino"]').forEach(radio => {
        radio.checked = radio.value === 'EXPEDIENTE';
    });
    toggleOrigenTermino();

    // Resetear abogado
    if (abogadoSelect) {
        if (abogadoSelect.tomselect) {
            updateTomSelectOptions(abogadoSelect.tomselect, [], 'Seleccione expediente primero...');
            abogadoSelect.tomselect.disable();
        } else {
            abogadoSelect.innerHTML = '<option value="">Seleccione expediente primero...</option>';
            abogadoSelect.classList.add('cursor-not-allowed');
            abogadoSelect.classList.add('bg-gray-50');
        }
    }

    // Resetear prioridad
    const prioridadSelect = document.getElementById('select-prioridad');
    if (prioridadSelect) prioridadSelect.value = "MEDIA";

    if (titulo) titulo.innerHTML = '<i class="fas fa-plus-circle"></i> Nuevo Término';

    const btnGuardar = document.getElementById('btn-guardar-termino');
    if (btnGuardar) {
        btnGuardar.disabled = false;
        btnGuardar.classList.remove('opacity-50', 'cursor-not-allowed');
    }

    const modal = document.getElementById('modal-nuevo-termino');
    if (modal) {
        modal.classList.remove('hidden');
        modal.classList.add('flex');
    }
    
    ajustarVisibilidadCamposPorRolYOrigen();
}

// 2. EDITAR TÉRMINO (Con el botón amarillo)
function toggleOrigenTermino() {
    const origen = document.querySelector('input[name="origenTermino"]:checked')?.value || 'EXPEDIENTE';
    const esAsuntoUnico = origen === 'ASUNTO_UNICO';
    const grupoExpediente = document.getElementById('grupo-expediente-termino');
    const grupoContexto = document.getElementById('grupo-contexto-termino');
    const expSelect = document.getElementById('select-expediente-termino');
    const abogadoSelect = document.getElementById('select-abogado-termino');
    const userRol = document.getElementById('hdnUsuarioRol')?.value;

    grupoExpediente?.classList.toggle('hidden', esAsuntoUnico);
    grupoContexto?.classList.toggle('hidden', !esAsuntoUnico);

    if (expSelect) expSelect.required = !esAsuntoUnico;
    ['select-contexto-materia', 'select-contexto-gerencia', 'input-contexto-descripcion'].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.required = esAsuntoUnico;
    });

    if (esAsuntoUnico && abogadoSelect) {
        if (abogadoSelect.tomselect) {
            abogadoSelect.tomselect.enable();
        } else {
            abogadoSelect.removeAttribute('readonly');
            abogadoSelect.classList.remove('cursor-not-allowed', 'bg-gray-50');
            abogadoSelect.classList.add('bg-white');
        }
        
        const gerenciaSelect = document.getElementById('select-contexto-gerencia');
        if (gerenciaSelect) {
            const currentGerenciaVal = gerenciaSelect.tomselect ? gerenciaSelect.tomselect.getValue() : gerenciaSelect.value;
            // Auto-seleccionar si solo hay 1 opción
            if (!gerenciaSelect.tomselect && gerenciaSelect.options.length === 2 && !gerenciaSelect.value) {
                gerenciaSelect.selectedIndex = 1;
            }
            
            if (userRol === 'ABOGADO') {
                const myId = document.getElementById('hdnUsuarioId')?.value;
                const myName = document.getElementById('hdnUsuarioNombre')?.value;
                if (abogadoSelect.tomselect) {
                    abogadoSelect.tomselect.addOption({ value: myId, text: myName });
                    abogadoSelect.tomselect.setValue(myId);
                } else {
                    abogadoSelect.innerHTML = `<option value="${myId}" selected>${myName}</option>`;
                    abogadoSelect.value = myId;
                }
            }
            
            // Cargar materias y abogados dinámicamente si hay una gerencia seleccionada
            if (currentGerenciaVal) {
                cargarMateriasYAbogadosPorGerencia(currentGerenciaVal);
            } else {
                cargarMateriasYAbogadosPorGerencia('');
            }
        }
    } else if (abogadoSelect) {
        const currentExpVal = expSelect ? (expSelect.tomselect ? expSelect.tomselect.getValue() : expSelect.value) : '';
        if (!currentExpVal) {
            if (abogadoSelect.tomselect) {
                updateTomSelectOptions(abogadoSelect.tomselect, [], 'Seleccione expediente primero...');
                abogadoSelect.tomselect.disable();
            } else {
                abogadoSelect.innerHTML = '<option value="">Seleccione expediente primero...</option>';
                abogadoSelect.classList.add('cursor-not-allowed', 'bg-gray-50');
                abogadoSelect.classList.remove('bg-white');
                abogadoSelect.setAttribute('readonly', 'true');
            }
        }
    }

    ajustarVisibilidadCamposPorRolYOrigen();
}

function ajustarVisibilidadCamposPorRolYOrigen() {
    const userRol = document.getElementById('hdnUsuarioRol')?.value;
    const origen = document.querySelector('input[name="origenTermino"]:checked')?.value || 'EXPEDIENTE';
    
    const divGerencia = document.getElementById('div-gerencia-termino');
    const divAbogado = document.getElementById('div-abogado-termino');
    const gerenciaSelect = document.getElementById('select-contexto-gerencia');
    const abogadoSelect = document.getElementById('select-abogado-termino');

    // 1. Gerencia (solo relevante para ASUNTO_UNICO)
    if (divGerencia) {
        if (origen === 'ASUNTO_UNICO') {
            if (['ABOGADO', 'JEFE_DEPTO', 'GERENTE'].includes(userRol)) {
                divGerencia.classList.add('hidden');
                if (gerenciaSelect) gerenciaSelect.required = false;
            } else {
                divGerencia.classList.remove('hidden');
                if (gerenciaSelect) gerenciaSelect.required = true;
            }
        } else {
            divGerencia.classList.add('hidden');
            if (gerenciaSelect) gerenciaSelect.required = false;
        }
    }

    // 2. Abogado Responsable
    if (divAbogado) {
        if (origen === 'EXPEDIENTE') {
            // Vinculado al expediente, por lo tanto es readonly → lo ocultamos
            divAbogado.classList.add('hidden');
            if (abogadoSelect) abogadoSelect.required = false;
        } else {
            // ASUNTO_UNICO
            if (userRol === 'ABOGADO') {
                // El abogado no puede elegir (siempre es él mismo) → lo ocultamos
                divAbogado.classList.add('hidden');
                if (abogadoSelect) abogadoSelect.required = false;
            } else {
                // Jefes, Gerentes, Admin, Dirección pueden asignar → lo mostramos
                divAbogado.classList.remove('hidden');
                if (abogadoSelect) abogadoSelect.required = true;
            }
        }
    }
}

function cargarMateriasYAbogadosPorGerencia(gerenciaId, callbackMateria = null, callbackAbogado = null) {
    const materiaSelect = document.getElementById('select-contexto-materia');
    const abogadoSelect = document.getElementById('select-abogado-termino');

    if (!gerenciaId) {
        if (materiaSelect) {
            if (materiaSelect.tomselect) {
                updateTomSelectOptions(materiaSelect.tomselect, [], 'Seleccione gerencia primero...');
                materiaSelect.tomselect.disable();
            } else {
                materiaSelect.innerHTML = '<option value="">Seleccione gerencia primero...</option>';
                materiaSelect.disabled = true;
            }
        }
        if (abogadoSelect) {
            if (abogadoSelect.tomselect) {
                updateTomSelectOptions(abogadoSelect.tomselect, [], 'Seleccione gerencia primero...');
                abogadoSelect.tomselect.disable();
            } else {
                abogadoSelect.innerHTML = '<option value="">Seleccione gerencia primero...</option>';
                abogadoSelect.disabled = true;
            }
        }
        return;
    }

    if (materiaSelect) {
        if (materiaSelect.tomselect) {
            updateTomSelectOptions(materiaSelect.tomselect, [], 'Cargando materias...');
            materiaSelect.tomselect.disable();
        } else {
            materiaSelect.disabled = false;
            materiaSelect.innerHTML = '<option value="">Cargando materias...</option>';
        }
        fetch(`/terminos/api/materias-por-gerencia/${gerenciaId}`)
            .then(res => res.json())
            .then(data => {
                if (materiaSelect.tomselect) {
                    updateTomSelectOptions(materiaSelect.tomselect, data.map(m => ({ value: m.id, text: m.nombre })), 'Seleccione materia...');
                    materiaSelect.tomselect.enable();
                } else {
                    materiaSelect.innerHTML = '<option value="">Seleccione materia...</option>';
                    data.forEach(m => {
                        const opt = document.createElement('option');
                        opt.value = m.id;
                        opt.text = m.nombre;
                        materiaSelect.appendChild(opt);
                    });
                }
                if (callbackMateria) callbackMateria();
            })
            .catch(err => {
                console.error("Error al cargar materias:", err);
                if (materiaSelect.tomselect) {
                    updateTomSelectOptions(materiaSelect.tomselect, [], 'Error al cargar materias');
                } else {
                    materiaSelect.innerHTML = '<option value="">Error al cargar materias</option>';
                }
            });
    }

    if (abogadoSelect) {
        if (abogadoSelect.tomselect) {
            updateTomSelectOptions(abogadoSelect.tomselect, [], 'Cargando abogados...');
            abogadoSelect.tomselect.disable();
        } else {
            abogadoSelect.disabled = false;
            abogadoSelect.innerHTML = '<option value="">Cargando abogados...</option>';
        }
        fetch(`/terminos/api/abogados-por-gerencia/${gerenciaId}`)
            .then(res => res.json())
            .then(data => {
                if (abogadoSelect.tomselect) {
                    updateTomSelectOptions(abogadoSelect.tomselect, data.map(a => ({ value: a.id, text: a.nombre })), 'Seleccione abogado...');
                    abogadoSelect.tomselect.enable();
                    if (data.length === 1) {
                        abogadoSelect.tomselect.setValue(data[0].id);
                    }
                } else {
                    abogadoSelect.innerHTML = '<option value="">Seleccione abogado...</option>';
                    data.forEach(a => {
                        const opt = document.createElement('option');
                        opt.value = a.id;
                        opt.text = a.nombre;
                        abogadoSelect.appendChild(opt);
                    });
                    // Auto-seleccionar si solo hay 1 abogado disponible (ej. rol Abogado)
                    if (abogadoSelect.options.length === 2) {
                        abogadoSelect.selectedIndex = 1;
                    }
                }
                if (callbackAbogado) callbackAbogado();
            })
            .catch(err => {
                console.error("Error al cargar abogados:", err);
                if (abogadoSelect.tomselect) {
                    updateTomSelectOptions(abogadoSelect.tomselect, [], 'Error al cargar abogados');
                } else {
                    abogadoSelect.innerHTML = '<option value="">Error al cargar abogados</option>';
                }
            });
    }
}

function editarTermino(id, actuacion, fecha, expedienteId, expedienteNum, abogadoId, prioridad, observaciones = "", origenTermino = 'EXPEDIENTE', materiaId = '', gerenciaId = '', descripcion = '', partes = '', referencia = '') {

    // Llenar datos en el formulario
    const idInput = document.getElementById('input-termino-id');
    const asuntoInput = document.getElementById('input-asunto');
    const vencimientoInput = document.getElementById('input-vencimiento');
    const expSelect = document.getElementById('select-expediente-termino');
    const prioridadSelect = document.getElementById('select-prioridad');
    const observacionesInput = document.getElementById('input-observaciones-termino');
    const titulo = document.getElementById('modal-titulo');
    
    const materiaSelect = document.getElementById('select-contexto-materia');
    const gerenciaSelect = document.getElementById('select-contexto-gerencia');
    const descTextArea = document.getElementById('input-contexto-descripcion');
    const partesInput = document.getElementById('input-contexto-partes');
    const refInput = document.getElementById('input-contexto-referencia');
    const abogadoSelect = document.getElementById('select-abogado-termino');

    if (idInput) idInput.value = id;
    if (asuntoInput) asuntoInput.value = actuacion;
    if (vencimientoInput) vencimientoInput.value = fecha;
    if (prioridadSelect && prioridad) prioridadSelect.value = prioridad;
    if (observacionesInput) observacionesInput.value = observaciones;

    if (descTextArea) descTextArea.value = descripcion;
    if (partesInput) partesInput.value = partes;
    if (refInput) refInput.value = referencia;

    document.querySelectorAll('input[name="origenTermino"]').forEach(radio => {
        radio.checked = radio.value === origenTermino;
    });

    if (origenTermino === 'ASUNTO_UNICO') {
        if (gerenciaSelect) {
            if (gerenciaSelect.tomselect) {
                if (gerenciaId) {
                    gerenciaSelect.tomselect.setValue(gerenciaId);
                } else {
                    gerenciaSelect.tomselect.setValue('');
                }
            } else {
                if (gerenciaId) {
                    gerenciaSelect.value = gerenciaId;
                } else if (gerenciaSelect.options.length === 2) {
                    gerenciaSelect.selectedIndex = 1;
                } else {
                    gerenciaSelect.value = "";
                }
            }
        }
        toggleOrigenTermino();
        
        const currentGerenciaId = gerenciaSelect ? (gerenciaSelect.tomselect ? gerenciaSelect.tomselect.getValue() : gerenciaSelect.value) : '';
        cargarMateriasYAbogadosPorGerencia(currentGerenciaId, () => {
            if (materiaSelect) {
                if (materiaSelect.tomselect && materiaId) materiaSelect.tomselect.setValue(materiaId);
                else if (materiaId) materiaSelect.value = materiaId;
            }
        }, () => {
            if (abogadoSelect) {
                if (abogadoSelect.tomselect && abogadoId) abogadoSelect.tomselect.setValue(abogadoId);
                else if (abogadoId) abogadoSelect.value = abogadoId;
            }
        });
    } else {
        toggleOrigenTermino();
        if (expSelect && expedienteId) {
            if (expSelect.tomselect) {
                const optionExists = Object.keys(expSelect.tomselect.options).includes(String(expedienteId));
                if (!optionExists) {
                    expSelect.tomselect.addOption({ value: expedienteId, text: expedienteNum + " (Autoseleccionado)" });
                }
                expSelect.tomselect.setValue(expedienteId);
                vincularAbogadoTermino();
            } else {
                let optionExists = Array.from(expSelect.options).some(opt => opt.value == expedienteId);
                if (!optionExists) {
                    const opt = document.createElement('option');
                    opt.value = expedienteId;
                    opt.text = expedienteNum + " (Autoseleccionado)";
                    expSelect.add(opt);
                }
                expSelect.value = expedienteId;
                vincularAbogadoTermino();
            }
        }
        if (abogadoSelect && abogadoId) {
            if (abogadoSelect.tomselect) {
                const optionExists = Object.keys(abogadoSelect.tomselect.options).includes(String(abogadoId));
                if (!optionExists) {
                    abogadoSelect.tomselect.addOption({ value: abogadoId, text: "Abogado Autoseleccionado" });
                }
                abogadoSelect.tomselect.setValue(abogadoId);
            } else {
                abogadoSelect.value = abogadoId;
            }
        }
    }

    if (titulo) titulo.innerHTML = '<i class="fas fa-edit"></i> Editar Término';

    const btnGuardar = document.getElementById('btn-guardar-termino');
    if (btnGuardar) {
        btnGuardar.disabled = false;
        btnGuardar.classList.remove('opacity-50', 'cursor-not-allowed');
    }

    const modal = document.getElementById('modal-nuevo-termino');
    if (modal) {
        modal.classList.remove('hidden');
        modal.classList.add('flex');
    }

    if (id) {
        sendTerminoLock(id);
    }
    
    ajustarVisibilidadCamposPorRolYOrigen();
}

function recargarDatosTerminoActual() {
    const idInput = document.getElementById('input-termino-id');
    if (!idInput || !idInput.value) return;

    const termId = idInput.value;
    
    Swal.fire({
        title: 'Recargando datos...',
        allowOutsideClick: false,
        didOpen: () => Swal.showLoading()
    });

    fetch(`/terminos/api/${termId}`)
        .then(res => {
            if (!res.ok) throw new Error('Error al recargar');
            return res.json();
        })
        .then(data => {
            editarTermino(
                data.id,
                data.asunto,
                data.fechaVencimiento,
                data.expedienteId,
                data.expedienteNum,
                data.abogadoId,
                data.prioridad,
                data.observaciones,
                data.origenTermino,
                data.materiaId,
                data.gerenciaId,
                data.descripcion,
                data.partes,
                data.referencia
            );

            const btnGuardar = document.getElementById('btn-guardar-termino');
            if (btnGuardar) {
                btnGuardar.disabled = false;
                btnGuardar.classList.remove('opacity-50', 'cursor-not-allowed');
            }

            const banner = document.getElementById('banner-concurrencia');
            if (banner) {
                banner.classList.add('hidden');
            }

            Swal.fire({
                title: 'Datos Recargados',
                text: 'Se han cargado los datos más recientes del término desde la base de datos.',
                icon: 'success',
                confirmButtonText: 'Entendido',
                confirmButtonColor: '#bc955c'
            });
        })
        .catch(err => {
            console.error("Error al recargar término:", err);
            Swal.fire({
                title: 'Error',
                text: 'No se pudieron recargar los datos del término.',
                icon: 'error',
                confirmButtonText: 'Entendido',
                confirmButtonColor: '#bc955c'
            });
        });
}

function vincularAbogadoTermino() {
    const expSelect = document.getElementById('select-expediente-termino');
    const abogadoSelect = document.getElementById('select-abogado-termino');

    if (!expSelect || !abogadoSelect) return;

    const expId = expSelect.tomselect ? expSelect.tomselect.getValue() : expSelect.value;
    if (!expId) return;

    if (abogadoSelect.tomselect) {
        updateTomSelectOptions(abogadoSelect.tomselect, [], 'Cargando...');
    } else {
        abogadoSelect.innerHTML = '<option>Cargando...</option>';
    }

    fetch(`/terminos/api/responsable-expediente/${expId}`)
        .then(response => {
            if (!response.ok) throw new Error("Sin responsable");
            return response.json();
        })
        .then(data => {
            if (abogadoSelect.tomselect) {
                updateTomSelectOptions(abogadoSelect.tomselect, [{ value: data.id, text: data.nombre }], 'Asignar abogado...');
                abogadoSelect.tomselect.setValue(data.id);
                abogadoSelect.tomselect.enable();
            } else {
                abogadoSelect.innerHTML = '';
                const option = document.createElement('option');
                option.value = data.id;
                option.text = data.nombre;
                option.selected = true;
                abogadoSelect.appendChild(option);

                abogadoSelect.classList.remove('cursor-not-allowed', 'bg-gray-50');
                abogadoSelect.classList.add('bg-white');
            }
        })
        .catch(err => {
            console.error(err);
            if (abogadoSelect.tomselect) {
                updateTomSelectOptions(abogadoSelect.tomselect, [], 'Sin abogado asignado');
            } else {
                abogadoSelect.innerHTML = '<option value="">Sin abogado asignado</option>';
            }
        });
}

function inicializarSelectsTerminos() {
    const selectExpediente = document.getElementById('select-expediente-termino');
    const selectGerencia = document.getElementById('select-contexto-gerencia');
    const selectMateria = document.getElementById('select-contexto-materia');
    const selectAbogado = document.getElementById('select-abogado-termino');

    if (selectExpediente && !selectExpediente.tomselect) {
        initSearchableSelect(selectExpediente, { 
            tipo: 'expediente',
            onChange: function() {
                vincularAbogadoTermino();
            }
        });
    }

    if (selectGerencia && !selectGerencia.tomselect) {
        initSearchableSelect(selectGerencia, { 
            tipo: 'gerencia',
            onChange: function() {
                const origen = document.querySelector('input[name="origenTermino"]:checked')?.value || 'EXPEDIENTE';
                if (origen === 'ASUNTO_UNICO') {
                    const val = selectGerencia.tomselect.getValue();
                    cargarMateriasYAbogadosPorGerencia(val);
                }
            }
        });
    }

    if (selectMateria && !selectMateria.tomselect) {
        initSearchableSelect(selectMateria, { 
            tipo: 'materia', 
            createInline: true, 
            gerenciaSelector: '#select-contexto-gerencia' 
        });
    }

    if (selectAbogado && !selectAbogado.tomselect) {
        initSearchableSelect(selectAbogado, { tipo: 'abogado' });
    }
}
// ABRIR MODAL PARA CONCLUIR TÉRMINO
function abrirModalConcluir(idTermino) {
    const inputId = document.getElementById('input-concluir-id');
    const inputObs = document.getElementById('input-concluir-obs');
    const modal = document.getElementById('modal-concluir-final');

    if (inputId && modal) {
        inputId.value = idTermino;
        if (inputObs) inputObs.value = ''; // Limpiar comentarios previos
        modal.classList.remove('hidden');
        modal.classList.add('flex');
    }
}

/**
 * Función estandarizada para cerrar modales eliminando conflictos de display
 */
function cerrarModal(id) {
    const modal = document.getElementById(id);
    if (modal) {
        modal.classList.add('hidden');
        modal.classList.remove('flex');
        
        if (id === 'modal-nuevo-termino') {
            const banner = document.getElementById('banner-concurrencia');
            if (banner) banner.classList.add('hidden');
            
            const idInput = document.getElementById('input-termino-id');
            if (idInput && idInput.value) {
                sendTerminoUnlock(idInput.value);
                idInput.value = "";
            }
        }
    }
}

// ==========================================
// 📅 LÓGICA DEL MINI-CALENDARIO (TÉRMINOS)
// ==========================================

const miniCalendarState = {
    currentDate: new Date(),
    selectedDate: new Date(),
    isDateFilterActive: false, // ROD-185: Bandera para saber si filtramos por fecha o vemos todo
    names: ["Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"],
    events: []
};

function fetchMiniCalendarEvents() {
    const urlParams = new URLSearchParams();
    urlParams.set('tipo', 'termino');

    // Obtener filtros del Sidebar
    const formFiltros = document.getElementById('form-filtros');
    if (formFiltros) {
        const fd = new FormData(formFiltros);
        const mapping = {
            'gerenciaId': 'gerencia',
            'abogadoId': 'usuario',
            'materiaId': 'materia',
            'estatus': 'estatus',
            'prioridadId': 'prioridadId'
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
    if (urlParams.has('mes') || urlParams.has('anio') || urlParams.get('keyword')?.match(/^\d{4}-\d{2}-\d{2}$/)) {
        miniCalendarState.isDateFilterActive = true;
    }

    // Si hay un keyword que es fecha, marcarla como seleccionada
    const keyword = urlParams.get('keyword');
    if (keyword && keyword.match(/^\d{4}-\d{2}-\d{2}$/)) {
        const parts = keyword.split('-');
        miniCalendarState.selectedDate = new Date(parseInt(parts[0]), parseInt(parts[1]) - 1, parseInt(parts[2]));
    }

    if (prevBtn && nextBtn) {
        prevBtn.onclick = () => {
            const date = miniCalendarState.currentDate;
            date.setMonth(date.getMonth() - 1);
            const searchInput = document.getElementById('input-search-terminos');
            if (searchInput) {
                searchInput.value = '';
                const btnClear = document.getElementById('btn-clear-search-terminos');
                if (btnClear) btnClear.classList.add('hidden');
            }
            miniCalendarState.isDateFilterActive = true; // Activar al navegar meses
            actualizarTablaAsincrono();
        };
        nextBtn.onclick = () => {
            const date = miniCalendarState.currentDate;
            date.setMonth(date.getMonth() + 1);
            const searchInput = document.getElementById('input-search-terminos');
            if (searchInput) {
                searchInput.value = '';
                const btnClear = document.getElementById('btn-clear-search-terminos');
                if (btnClear) btnClear.classList.add('hidden');
            }
            miniCalendarState.isDateFilterActive = true; // Activar al navegar meses
            actualizarTablaAsincrono();
        };
    }

    fetchMiniCalendarEvents().then(() => {
        renderMiniCalendar();
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
        const searchInput = document.getElementById('input-search-terminos');
        if (searchInput) {
            searchInput.value = '';
            const btnClear = document.getElementById('btn-clear-search-terminos');
            if (btnClear) btnClear.classList.add('hidden');
        }
        miniCalendarState.isDateFilterActive = true; // ACTIVAR filtro de fecha
        renderMiniCalendar();
        actualizarTablaAsincrono();
    };

    const firstDay = new Date(year, month, 1).getDay();
    const daysInMonth = new Date(year, month + 1, 0).getDate();

    // Celdas vacías
    for (let i = 0; i < firstDay; i++) {
        const emptyCell = document.createElement('div');
        emptyCell.className = 'mini-day-cell mini-empty';
        grid.appendChild(emptyCell);
    }

    // Días del mes
    const today = new Date();
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
            miniCalendarState.selectedDate = new Date(year, month, day);
            miniCalendarState.isDateFilterActive = true; // ACTIVAR filtro de fecha
            renderMiniCalendar();
            actualizarTablaAsincrono(dateStr);
        };

        grid.appendChild(cell);
    }
}

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
    const filterKeys = ['gerenciaId', 'materiaId', 'estatus', 'abogadoId', 'periodo', 'prioridadId'];

    let count = 0;
    filterKeys.forEach(key => {
        const val = params.get(key);
        if (val !== null && val !== '') count++;
    });

    if (count > 0) {
        toggleSidebarView('filtros');
        const badge = document.getElementById('badge-filtros-ter');
        if (badge) {
            badge.innerText = count;
            badge.classList.remove('hidden');
        }
    }
});
