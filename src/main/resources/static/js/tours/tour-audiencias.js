/**
 * SIAJ - Sistema Integral de Gestión Jurídica
 * Tour Guiado y Onboarding Contextual: Control y Programación de Audiencias
 */
(function () {
    'use strict';

    const STORAGE_KEY = 'siaj_tour_audiencias_visto';

    document.addEventListener('DOMContentLoaded', function () {
        initTourAudiencias();
        initFirstTimeBanner();
    });

    /**
     * Inicializa el botón disparador del tour
     */
    function initTourAudiencias() {
        const triggerBtn = document.getElementById('btn-iniciar-tour-audiencias');
        if (!triggerBtn) return;

        triggerBtn.addEventListener('click', function (e) {
            e.preventDefault();
            lanzarTourAudiencias();
        });
    }

    /**
     * Construye y ejecuta el tour con efecto Spotlight
     */
    function lanzarTourAudiencias() {
        if (!window.driver || !window.driver.js || typeof window.driver.js.driver !== 'function') {
            console.warn('[SIAJ Tour] Driver.js no está cargado.');
            return;
        }

        const driver = window.driver.js.driver;

        // Selección dinámica para la fila de la audiencia
        let targetFila = '#tabla-audiencias-body-desktop tr:first-child';
        let descFila = 'Cada audiencia especifica: expediente origen, fecha/hora, sala física o liga virtual y abogado compareciente. Los semáforos alertan proximidad en horas.';

        const filaReal = document.querySelector('#tabla-audiencias-body-desktop tr');
        if (!filaReal || filaReal.querySelector('td[colspan]')) {
            targetFila = '#tabla-audiencias-container';
            descFila = 'En este tablero se concentran todas las comparecencias judiciales programadas. Cuando agendes una cita, podrás consultar aquí la sala o acceder a la videollamada.';
        }

        const steps = [
            {
                element: 'aside.col-span-12',
                popover: {
                    title: '<i class="fas fa-calendar-alt text-gob-guinda mr-1"></i> 1. Calendario y Filtros',
                    description: 'Alterna entre el <strong>Mini-Calendario</strong> (para explorar audiencias por día específico) y la pestaña <strong>Filtros</strong> (para segmentar por juzgado, materia o abogado).',
                    side: 'right',
                    align: 'start'
                }
            },
            {
                element: '#contenedor-kpis-audiencias',
                popover: {
                    title: '<i class="fas fa-layer-group text-gob-oro mr-1"></i> 2. Indicadores y Filtro Rápido',
                    description: 'Supervisa el volumen de diligencias. <strong>Haz clic en cualquiera de las tarjetas</strong> (<em>Total, Pendientes, Resultado registrado, Concluidas</em>) para filtrar la lista al instante.',
                    side: 'bottom',
                    align: 'center'
                }
            },
            {
                element: '#form-search-audiencias',
                popover: {
                    title: '<i class="fas fa-search text-gob-guinda mr-1"></i> 3. Búsqueda y Ver Todos',
                    description: 'Localiza comparecencias por juzgado, tipo de diligencia o expediente. Usa el botón <strong>"Ver Todos"</strong> para reiniciar cualquier filtro rápidamente.',
                    side: 'bottom',
                    align: 'start'
                }
            },
            {
                element: targetFila,
                popover: {
                    title: '<i class="fas fa-video text-gob-oro mr-1"></i> 4. Modalidad, Enlaces y Semáforos',
                    description: descFila,
                    side: 'top',
                    align: 'center'
                }
            },
            {
                element: '#btn-nueva-audiencia-header',
                popover: {
                    title: '<i class="fas fa-plus-circle text-gob-guinda mr-1"></i> 5. Programación en 2 Pasos',
                    description: 'Agenda una audiencia mediante el asistente guiado: <strong>Paso 1:</strong> Selecciona el asunto (Expediente o Asunto Independiente) y <strong>Paso 2:</strong> Captura fecha, hora, modalidad y sala o enlace virtual.',
                    side: 'bottom',
                    align: 'end'
                }
            },
            {
                popover: {
                    title: '<div class="siaj-tour-final-header"><div class="siaj-tour-final-icon"><i class="fas fa-check"></i></div><span>¡Control de Audiencias al Día!</span></div>',
                    description: '<p class="text-gray-600 text-sm leading-relaxed mb-1">Mantén al equipo jurídico puntual y preparado en cada comparecencia procesal.</p><p class="text-xs text-gray-500 mt-2">Puedes relanzar este recorrido cuando lo necesites pulsando el botón <strong>"? Guía rápida"</strong>.</p>',
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
     * Muestra banner en primer uso de audiencias
     */
    function initFirstTimeBanner() {
        const yaVisto = localStorage.getItem(STORAGE_KEY);
        if (yaVisto === 'true') return;

        setTimeout(function () {
            if (document.getElementById('siaj-ftue-audiencias-toast')) return;

            const toast = document.createElement('div');
            toast.id = 'siaj-ftue-audiencias-toast';
            toast.className = 'siaj-ftue-toast';
            toast.innerHTML = `
                <div class="flex items-start justify-between gap-3">
                    <div class="flex items-center gap-2 text-gob-guinda font-extrabold text-sm">
                        <i class="fas fa-gavel text-base text-gob-oro"></i>
                        <span>¿Primera vez en Audiencias?</span>
                    </div>
                    <button type="button" id="btn-cerrar-ftue-audiencias" class="text-gray-400 hover:text-gray-600 transition-colors text-base" aria-label="Cerrar">
                        <i class="fas fa-times"></i>
                    </button>
                </div>
                <p class="text-xs text-gray-600 leading-relaxed">
                    Conoce cómo dar seguimiento a comparecencias, salas virtuales y carga de actas en 45 segundos.
                </p>
                <div class="flex items-center justify-end gap-2 pt-1">
                    <button type="button" id="btn-omitir-ftue-audiencias" class="text-xs font-semibold text-gray-500 hover:text-gray-700 px-3 py-1.5 transition-colors">
                        Omitir
                    </button>
                    <button type="button" id="btn-comenzar-ftue-audiencias" class="inline-flex items-center gap-1.5 text-xs font-bold text-white bg-gob-guinda hover:bg-gob-guindaDark px-3.5 py-1.5 rounded-lg shadow-sm transition-all">
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

            document.getElementById('btn-cerrar-ftue-audiencias')?.addEventListener('click', cerrar);
            document.getElementById('btn-omitir-ftue-audiencias')?.addEventListener('click', cerrar);

            document.getElementById('btn-comenzar-ftue-audiencias')?.addEventListener('click', function () {
                cerrar();
                lanzarTourAudiencias();
            });
        }, 1200);
    }

    window.lanzarTourAudiencias = lanzarTourAudiencias;
})();
