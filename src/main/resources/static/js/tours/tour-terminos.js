/**
 * SIAJ - Sistema Integral de Gestión Jurídica
 * Tour Guiado y Onboarding Contextual: Gestión de Términos
 */
(function () {
    'use strict';

    const STORAGE_KEY = 'siaj_tour_terminos_visto';

    document.addEventListener('DOMContentLoaded', function () {
        initTourTerminos();
        initFirstTimeBanner();
    });

    /**
     * Inicializa el botón disparador del tour
     */
    function initTourTerminos() {
        const triggerBtn = document.getElementById('btn-iniciar-tour-terminos');
        if (!triggerBtn) return;

        triggerBtn.addEventListener('click', function (e) {
            e.preventDefault();
            lanzarTourTerminos();
        });
    }

    /**
     * Construye y ejecuta el tour con efecto Spotlight
     */
    function lanzarTourTerminos() {
        if (!window.driver || !window.driver.js || typeof window.driver.js.driver !== 'function') {
            console.warn('[SIAJ Tour] Driver.js no está cargado.');
            return;
        }

        const driver = window.driver.js.driver;

        // Selección dinámica para la fila del término
        let targetFila = '#tabla-terminos-body tr:first-child';
        let descFila = 'Cada registro muestra: expediente de origen, fecha límite, prioridad y abogado asignado. 🟢 <strong>A tiempo</strong>, 🟡 <strong>Atención</strong>, 🔴 <strong>Crítico</strong>. Pulsa en <em>"Cómo se calculó"</em> para auditar los días hábiles y suspensiones.';

        const filaReal = document.querySelector('#tabla-terminos-body tr');
        if (!filaReal || filaReal.querySelector('td[colspan]')) {
            targetFila = '#tabla-resultados-container';
            descFila = 'En esta tabla se concentran todos los términos procesales con sus semáforos de proximidad en días hábiles. Cuando registres o te asignen un plazo, lo supervisarás aquí.';
        }

        const steps = [
            {
                element: 'aside.col-span-12',
                popover: {
                    title: '<i class="fas fa-calendar-alt text-gob-guinda mr-1"></i> 1. Agenda y Filtros Laterales',
                    description: 'Alterna entre el <strong>Mini-Calendario</strong> (para explorar términos por día del mes) y la pestaña <strong>Filtros</strong> (para segmentar por abogado, materia o prioridad).',
                    side: 'right',
                    align: 'start'
                }
            },
            {
                element: '#contenedor-kpis-terminos',
                popover: {
                    title: '<i class="fas fa-network-wired text-gob-oro mr-1"></i> 2. Flujo Procesal del Plazo',
                    description: 'Supervisa el ciclo de vida de los términos: <em>Proyectista → Revisión Jefatura → Corrección → Aprobación → Liberado → Concluido</em>. <strong>Haz clic en cualquier esfera</strong> para filtrar la lista a esa etapa.',
                    side: 'bottom',
                    align: 'center'
                }
            },
            {
                element: '#form-search-terminos',
                popover: {
                    title: '<i class="fas fa-search text-gob-guinda mr-1"></i> 3. Búsqueda y Ver Todos',
                    description: 'Busca en vivo por número de expediente, partes o actuación. Usa el botón <strong>"VerTodos"</strong> para reiniciar cualquier filtro aplicado de forma inmediata.',
                    side: 'bottom',
                    align: 'start'
                }
            },
            {
                element: targetFila,
                popover: {
                    title: '<i class="fas fa-hourglass-half text-gob-oro mr-1"></i> 4. Semáforo y Vencimiento',
                    description: descFila,
                    side: 'top',
                    align: 'center'
                }
            },
            {
                element: '#btn-nuevo-termino-header',
                popover: {
                    title: '<i class="fas fa-plus-circle text-gob-guinda mr-1"></i> 5. Asistente de Registro',
                    description: 'Registra un término en 2 pasos guiados: <strong>Paso 1:</strong> Selecciona el asunto (Expediente o Asunto Independiente) y <strong>Paso 2:</strong> Define la actuación, tipo de término y fecha de vencimiento.',
                    side: 'bottom',
                    align: 'end'
                }
            },
            {
                popover: {
                    title: '<div class="siaj-tour-final-header"><div class="siaj-tour-final-icon"><i class="fas fa-check"></i></div><span>¡Dominas la Gestión de Términos!</span></div>',
                    description: '<p class="text-gray-600 text-sm leading-relaxed mb-1">Ahora tienes el control total de los plazos judiciales y sus semáforos de advertencia.</p><p class="text-xs text-gray-500 mt-2">Puedes relanzar este recorrido cuando lo necesites pulsando el botón <strong>"? Guía rápida"</strong>.</p>',
                    popoverClass: 'siaj-tour-popover siaj-tour-popover-final',
                    doneBtnText: '¡Comenzar a trabajar!',
                    prevBtnText: '← Repasar pasos anteriores',
                    side: 'over',
                    align: 'center'
                }
            }
        ];

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
            doneBtnText: '¡Comenzar a trabajar!',
            progressText: 'Paso {{current}} de {{total}}',
            steps: steps,
            onDestroyed: function () {
                localStorage.setItem(STORAGE_KEY, 'true');
            }
        });

        driverObj.drive();
    }

    /**
     * Muestra banner en primer uso de términos
     */
    function initFirstTimeBanner() {
        const yaVisto = localStorage.getItem(STORAGE_KEY);
        if (yaVisto === 'true') return;

        setTimeout(function () {
            if (document.getElementById('siaj-ftue-terminos-toast')) return;

            const toast = document.createElement('div');
            toast.id = 'siaj-ftue-terminos-toast';
            toast.className = 'siaj-ftue-toast';
            toast.innerHTML = `
                <div class="flex items-start justify-between gap-3">
                    <div class="flex items-center gap-2 text-gob-guinda font-extrabold text-sm">
                        <i class="fas fa-compass text-base text-gob-oro"></i>
                        <span>¿Primera vez en Términos?</span>
                    </div>
                    <button type="button" id="btn-cerrar-ftue-terminos" class="text-gray-400 hover:text-gray-600 transition-colors text-base" aria-label="Cerrar">
                        <i class="fas fa-times"></i>
                    </button>
                </div>
                <p class="text-xs text-gray-600 leading-relaxed">
                    Conoce cómo dar seguimiento a vencimientos judiciales, semáforos de urgencia y flujo de aprobación en 50 segundos.
                </p>
                <div class="flex items-center justify-end gap-2 pt-1">
                    <button type="button" id="btn-omitir-ftue-terminos" class="text-xs font-semibold text-gray-500 hover:text-gray-700 px-3 py-1.5 transition-colors">
                        Omitir
                    </button>
                    <button type="button" id="btn-comenzar-ftue-terminos" class="inline-flex items-center gap-1.5 text-xs font-bold text-white bg-gob-guinda hover:bg-gob-guindaDark px-3.5 py-1.5 rounded-lg shadow-sm transition-all">
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

            document.getElementById('btn-cerrar-ftue-terminos')?.addEventListener('click', cerrar);
            document.getElementById('btn-omitir-ftue-terminos')?.addEventListener('click', cerrar);

            document.getElementById('btn-comenzar-ftue-terminos')?.addEventListener('click', function () {
                cerrar();
                lanzarTourTerminos();
            });
        }, 1200);
    }

    window.lanzarTourTerminos = lanzarTourTerminos;
})();
