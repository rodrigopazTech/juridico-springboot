/**
 * SIAJ - Sistema Integral de Gestión Jurídica
 * Tour Guiado y Onboarding Contextual: Agenda y Calendario Jurídico (/calendario)
 */
(function () {
    'use strict';

    const STORAGE_KEY = 'siaj_tour_calendario_visto';

    document.addEventListener('DOMContentLoaded', function () {
        initTourCalendario();
        initFirstTimeBanner();
    });

    /**
     * Inicializa el botón disparador del tour
     */
    function initTourCalendario() {
        const triggerBtn = document.getElementById('btn-iniciar-tour-calendario');
        if (!triggerBtn) return;

        triggerBtn.addEventListener('click', function (e) {
            e.preventDefault();
            lanzarTourCalendario();
        });
    }

    /**
     * Construye y ejecuta el tour con efecto Spotlight
     */
    function lanzarTourCalendario() {
        if (!window.driver || !window.driver.js || typeof window.driver.js.driver !== 'function') {
            console.warn('[SIAJ Tour] Driver.js no está cargado.');
            return;
        }

        const driver = window.driver.js.driver;

        // Elementos interactivos del calendario
        const elToolbar = document.getElementById('calendarToolbar');
        const elPeriodo = document.querySelector('.calendar-period-control');
        const elViewSwitch = document.querySelector('.calendar-view-switch');
        const elMisAsuntos = document.querySelector('.calendar-own-toggle') || document.getElementById('chkMisAsuntos')?.parentElement;
        const elFiltros = document.querySelector('.calendar-inline-filters');
        const elGrid = document.getElementById('calendarContainer') || document.getElementById('calendarMonthView');

        const steps = [];

        // Paso 1: Centro Cronológico
        if (elToolbar) {
            steps.push({
                element: elToolbar,
                popover: {
                    title: '<i class="fas fa-calendar-alt text-gob-guinda mr-1"></i> 1. Agenda Jurídica Integral',
                    description: 'Aquí visualizas de forma unificada todas las <strong>audiencias judiciales</strong> y los <strong>vencimientos de términos</strong> de la institución.',
                    side: 'bottom',
                    align: 'start'
                }
            });
        }

        // Paso 2: Navegación de periodo y vistas
        if (elPeriodo || elViewSwitch) {
            steps.push({
                element: elViewSwitch || elPeriodo,
                popover: {
                    title: '<i class="fas fa-calendar-week text-gob-oro mr-1"></i> 2. Navegación y Vistas',
                    description: 'Cambia entre vista de <strong>Mes, Semana o Día</strong> para organizar tu agenda de trabajo o planificar comparecencias y traslados a juzgados.',
                    side: 'bottom',
                    align: 'center'
                }
            });
        }

        // Paso 3: Toggle Mis Asuntos
        if (elMisAsuntos) {
            steps.push({
                element: elMisAsuntos,
                popover: {
                    title: '<i class="fas fa-user-check text-gob-guinda mr-1"></i> 3. Filtro Rápido: Mis Asuntos',
                    description: 'Activa esta casilla para filtrar de inmediato y ver <strong>únicamente tus propias audiencias y términos</strong> asignados, ocultando la carga del resto del equipo.',
                    side: 'bottom',
                    align: 'end'
                }
            });
        }

        // Paso 4: Segmentación por Tipo y Materia
        if (elFiltros) {
            steps.push({
                element: elFiltros,
                popover: {
                    title: '<i class="fas fa-filter text-gob-oro mr-1"></i> 4. Segmentación Avanzada',
                    description: 'Filtra por <strong>Tipo</strong> (solo audiencias o solo términos), o segmenta por <strong>Gerencia, Usuario o Materia</strong> para auditoría o planeación de sala.',
                    side: 'bottom',
                    align: 'start'
                }
            });
        }

        // Paso 5: Malla de Eventos y Modal de Detalle
        if (elGrid) {
            steps.push({
                element: elGrid,
                popover: {
                    title: '<i class="fas fa-mouse-pointer text-gob-guinda mr-1"></i> 5. Malla de Eventos y Fichas',
                    description: 'Los eventos aparecen con distintivos de color según su tipo. <strong>Haz clic en cualquier evento</strong> para abrir su modal con juzgado, sala, liga virtual y acceso directo al expediente.',
                    side: 'top',
                    align: 'center'
                }
            });
        }

        // Paso Final Modal
        steps.push({
            popover: {
                title: '<div class="siaj-tour-final-header"><div class="siaj-tour-final-icon"><i class="fas fa-check"></i></div><span>¡Agenda Procesal Sincronizada!</span></div>',
                description: '<p class="text-gray-600 text-sm leading-relaxed mb-1">Cualquier actuación judicial registrada o reprogramada en el sistema se actualiza automáticamente en este calendario.</p><p class="text-xs text-gray-500 mt-2">Puedes relanzar este recorrido cuando lo desees usando el botón <strong>"? Guía rápida"</strong>.</p>',
                popoverClass: 'siaj-tour-popover siaj-tour-popover-final',
                doneBtnText: '¡Comenzar a planificar!',
                prevBtnText: '← Repasar pasos anteriores',
                side: 'over',
                align: 'center'
            }
        });

        const driverObj = driver({
            showProgress: true,
            animate: true,
            allowClose: true,
            overlayColor: '#0f172a',
            overlayOpacity: 0.75,
            stagePadding: 8,
            stageRadius: 10,
            popoverClass: 'siaj-tour-popover',
            nextBtnText: 'Siguiente →',
            prevBtnText: '← Anterior',
            doneBtnText: '¡Comenzar a planificar!',
            progressText: 'Paso {{current}} de {{total}}',
            steps: steps,
            onDestroyed: function () {
                localStorage.setItem(STORAGE_KEY, 'true');
            }
        });

        driverObj.drive();
    }

    /**
     * Muestra banner FTUE en primer uso
     */
    function initFirstTimeBanner() {
        const yaVisto = localStorage.getItem(STORAGE_KEY);
        if (yaVisto === 'true') return;

        setTimeout(function () {
            if (document.getElementById('siaj-ftue-calendario-toast')) return;

            const toast = document.createElement('div');
            toast.id = 'siaj-ftue-calendario-toast';
            toast.className = 'siaj-ftue-toast';
            toast.innerHTML = `
                <div class="flex items-start justify-between gap-3">
                    <div class="flex items-center gap-2 text-gob-guinda font-extrabold text-sm">
                        <i class="fas fa-calendar-check text-base text-gob-oro"></i>
                        <span>¿Primera vez en la Agenda Legal?</span>
                    </div>
                    <button type="button" id="btn-cerrar-ftue-calendario" class="text-gray-400 hover:text-gray-600 transition-colors text-base" aria-label="Cerrar">
                        <i class="fas fa-times"></i>
                    </button>
                </div>
                <p class="text-xs text-gray-600 leading-relaxed">
                    Aprende cómo alternar entre audiencias y términos, usar la vista de "Mis asuntos" y consultar detalles de comparecencias en 40 segundos.
                </p>
                <div class="flex items-center justify-end gap-2 pt-1">
                    <button type="button" id="btn-omitir-ftue-calendario" class="text-xs font-semibold text-gray-500 hover:text-gray-700 px-3 py-1.5 transition-colors">
                        Omitir
                    </button>
                    <button type="button" id="btn-comenzar-ftue-calendario" class="inline-flex items-center gap-1.5 text-xs font-bold text-white bg-gob-guinda hover:bg-gob-guindaDark px-3.5 py-1.5 rounded-lg shadow-sm transition-all">
                        <i class="fas fa-play text-[10px]"></i> Iniciar guía
                    </button>
                </div>
            `;

            document.body.appendChild(toast);

            const cerrar = function () {
                toast.classList.add('ftue-hiding');
                setTimeout(() => toast.remove(), 260);
                localStorage.setItem(STORAGE_KEY, 'true');
            };

            document.getElementById('btn-cerrar-ftue-calendario')?.addEventListener('click', cerrar);
            document.getElementById('btn-omitir-ftue-calendario')?.addEventListener('click', cerrar);

            document.getElementById('btn-comenzar-ftue-calendario')?.addEventListener('click', function () {
                cerrar();
                lanzarTourCalendario();
            });
        }, 1200);
    }

    window.lanzarTourCalendario = lanzarTourCalendario;
})();
