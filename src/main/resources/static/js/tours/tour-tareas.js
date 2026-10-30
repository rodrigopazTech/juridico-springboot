/**
 * SIAJ - Sistema Integral de Gestión Jurídica
 * Tour Guiado y Onboarding Contextual: Bandeja de Tareas (/tareas)
 */
(function () {
    'use strict';

    const STORAGE_KEY = 'siaj_tour_tareas_visto';

    document.addEventListener('DOMContentLoaded', function () {
        initTourTareas();
        initFirstTimeBanner();
    });

    /**
     * Inicializa el botón disparador del tour
     */
    function initTourTareas() {
        const triggerBtn = document.getElementById('btn-iniciar-tour-tareas');
        if (!triggerBtn) return;

        triggerBtn.addEventListener('click', function (e) {
            e.preventDefault();
            lanzarTourTareas();
        });
    }

    /**
     * Construye y ejecuta el tour con efecto Spotlight
     */
    function lanzarTourTareas() {
        if (!window.driver || !window.driver.js || typeof window.driver.js.driver !== 'function') {
            console.warn('[SIAJ Tour] Driver.js no está cargado.');
            return;
        }

        const driver = window.driver.js.driver;

        // Elementos interactivos
        const elTitulo = document.getElementById('titulo-mis-tareas') || document.querySelector('h1');
        const elBtnNueva = document.getElementById('btn-nueva-tarea') || document.querySelector('button[onclick*="abrirModalTarea"]');
        const elBannerAvisos = document.querySelector('section[aria-labelledby="como-funcionan-recordatorios"]');
        const elFiltros = document.querySelector('div.flex.gap-2.bg-gray-100') || document.querySelector('.filtro-btn')?.parentElement;
        const elPrimeraFila = document.querySelector('.fila-tarea') || document.getElementById('body-tareas');

        const steps = [];

        // Paso 1: Título / Concepto
        if (elTitulo) {
            steps.push({
                element: elTitulo,
                popover: {
                    title: '<i class="fas fa-tasks text-gob-guinda mr-1"></i> 1. Bandeja de Tareas Personales',
                    description: 'Tu espacio individual de trabajo para organizar <strong>redacciones, revisiones, solicitudes de copias o llamadas procesales</strong> sin saturar los plazos judiciales.',
                    side: 'bottom',
                    align: 'start'
                }
            });
        }

        // Paso 2: Botón Nueva Tarea
        if (elBtnNueva) {
            steps.push({
                element: elBtnNueva,
                popover: {
                    title: '<i class="fas fa-plus-circle text-gob-oro mr-1"></i> 2. Crear Nueva Tarea',
                    description: 'Haz clic aquí para programar un nuevo pendiente. Asigna fecha, hora límite, nivel de prioridad y, si lo requieres, <strong>vincúlalo a un expediente judicial</strong>.',
                    side: 'bottom',
                    align: 'end'
                }
            });
        }

        // Paso 3: Banner de Avisos y Notificaciones
        if (elBannerAvisos) {
            steps.push({
                element: elBannerAvisos,
                popover: {
                    title: '<i class="fas fa-bell text-gob-guinda mr-1"></i> 3. Alertas y Anticipación',
                    description: 'El sistema emite una <strong>alerta sonora y visual</strong> con el tiempo de anticipación configurado. Puedes personalizar los minutos de aviso en tus preferencias.',
                    side: 'bottom',
                    align: 'center'
                }
            });
        }

        // Paso 4: Segmentación por Estado
        if (elFiltros) {
            steps.push({
                element: elFiltros,
                popover: {
                    title: '<i class="fas fa-filter text-gob-oro mr-1"></i> 4. Control de Avance',
                    description: 'Alterna entre <strong>Todas</strong>, <strong>Pendientes</strong> y <strong>Completadas</strong> para auditar rápidamente tus actividades concluidas en la jornada.',
                    side: 'bottom',
                    align: 'end'
                }
            });
        }

        // Paso 5: Fila de Tarea y Checklist
        if (elPrimeraFila) {
            steps.push({
                element: elPrimeraFila,
                popover: {
                    title: '<i class="fas fa-check-circle text-gob-guinda mr-1"></i> 5. Checklist y Acciones',
                    description: 'Pulsa el círculo de verificación para <strong>marcar una tarea como terminada</strong> al instante. También puedes editar la hora o eliminarla según lo requieras.',
                    side: 'top',
                    align: 'start'
                }
            });
        }

        // Paso Final Modal
        steps.push({
            popover: {
                title: '<div class="siaj-tour-final-header"><div class="siaj-tour-final-icon"><i class="fas fa-check"></i></div><span>¡Productividad Jurídica Dominada!</span></div>',
                description: '<p class="text-gray-600 text-sm leading-relaxed mb-1">Tus tareas pendientes se reflejan en la tarjeta de resumen de tu <strong>Dashboard</strong> para mantener siempre la visibilidad de tu jornada.</p><p class="text-xs text-gray-500 mt-2">Puedes relanzar este recorrido cuando lo desees usando el botón <strong>"? Guía rápida"</strong>.</p>',
                popoverClass: 'siaj-tour-popover siaj-tour-popover-final',
                doneBtnText: '¡A comenzar mis tareas!',
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
            doneBtnText: '¡A comenzar mis tareas!',
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
            if (document.getElementById('siaj-ftue-tareas-toast')) return;

            const toast = document.createElement('div');
            toast.id = 'siaj-ftue-tareas-toast';
            toast.className = 'siaj-ftue-toast';
            toast.innerHTML = `
                <div class="flex items-start justify-between gap-3">
                    <div class="flex items-center gap-2 text-gob-guinda font-extrabold text-sm">
                        <i class="fas fa-tasks text-base text-gob-oro"></i>
                        <span>¿Primera vez en Mis Tareas?</span>
                    </div>
                    <button type="button" id="btn-cerrar-ftue-tareas" class="text-gray-400 hover:text-gray-600 transition-colors text-base" aria-label="Cerrar">
                        <i class="fas fa-times"></i>
                    </button>
                </div>
                <p class="text-xs text-gray-600 leading-relaxed">
                    Aprende cómo registrar recordatorios de trabajo, configurar avisos anticipados y dar seguimiento a tus pendientes en 40 segundos.
                </p>
                <div class="flex items-center justify-end gap-2 pt-1">
                    <button type="button" id="btn-omitir-ftue-tareas" class="text-xs font-semibold text-gray-500 hover:text-gray-700 px-3 py-1.5 transition-colors">
                        Omitir
                    </button>
                    <button type="button" id="btn-comenzar-ftue-tareas" class="inline-flex items-center gap-1.5 text-xs font-bold text-white bg-gob-guinda hover:bg-gob-guindaDark px-3.5 py-1.5 rounded-lg shadow-sm transition-all">
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

            document.getElementById('btn-cerrar-ftue-tareas')?.addEventListener('click', cerrar);
            document.getElementById('btn-omitir-ftue-tareas')?.addEventListener('click', cerrar);

            document.getElementById('btn-comenzar-ftue-tareas')?.addEventListener('click', function () {
                cerrar();
                lanzarTourTareas();
            });
        }, 1200);
    }

    window.lanzarTourTareas = lanzarTourTareas;
})();
