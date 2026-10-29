/**
 * SIAJ - Sistema Integral de Gestión Jurídica
 * Tour Guiado y Onboarding Contextual: Plazos y Alertas (Administración Institucional)
 */
(function () {
    'use strict';

    const STORAGE_KEY = 'siaj_tour_plazos_visto';

    document.addEventListener('DOMContentLoaded', function () {
        initTourPlazos();
        initFirstTimeBanner();
    });

    /**
     * Inicializa el botón disparador del tour
     */
    function initTourPlazos() {
        const triggerBtn = document.getElementById('btn-iniciar-tour-plazos');
        if (!triggerBtn) return;

        triggerBtn.addEventListener('click', function (e) {
            e.preventDefault();
            lanzarTourPlazos();
        });
    }

    /**
     * Construye y ejecuta el tour con efecto Spotlight
     */
    function lanzarTourPlazos() {
        if (!window.driver || !window.driver.js || typeof window.driver.js.driver !== 'function') {
            console.warn('[SIAJ Tour] Driver.js no está cargado.');
            return;
        }

        const driver = window.driver.js.driver;

        // Asegurar que estamos en la pestaña de términos para que los selectores existan
        const tabTerminos = document.getElementById('tab-terminos');
        if (tabTerminos && !tabTerminos.classList.contains('is-active')) {
            tabTerminos.click();
        }

        const steps = [
            {
                element: 'nav.pa-tabs',
                popover: {
                    title: '<i class="fas fa-layer-group text-gob-guinda mr-1"></i> 1. Pestañas Maestras',
                    description: 'Navega entre <strong>Términos</strong>, <strong>Audiencias</strong> y <strong>Calendario Oficial</strong>. Cada sección define cómo responderá el sistema ante los plazos y actuaciones legales.',
                    side: 'bottom',
                    align: 'start'
                }
            },
            {
                element: '#panel-terminos aside.pa-master',
                popover: {
                    title: '<i class="fas fa-list-check text-gob-oro mr-1"></i> 2. Catálogo de Tipos Jurídicos',
                    description: 'Administra los tipos de término por materia. Puedes activar o desactivar tipos institucionales y pulsar sobre cualquiera para auditar o ajustar sus reglas.',
                    side: 'right',
                    align: 'start'
                }
            },
            {
                element: '#tt-modos',
                popover: {
                    title: '<i class="fas fa-calculator text-gob-guinda mr-1"></i> 3. Modo de Plazo',
                    description: 'Determina cómo se calcula la fecha límite: <strong>Calculado por el sistema</strong> (aplica automáticamente cómputo de días hábiles y suspensiones), <strong>Definido por caso</strong> (captura manual) o <strong>Sin vencimiento</strong>.',
                    side: 'bottom',
                    align: 'start'
                }
            },
            {
                element: '#tt-alertas',
                popover: {
                    title: '<i class="fas fa-bell text-gob-oro mr-1"></i> 4. Umbrales de Alerta y Presets',
                    description: 'Calibra cuántos días de anticipación detonan los semáforos: 🟡 <strong>Atención</strong> y 🔴 <strong>Crítico</strong>. Usa las plantillas sugeridas: <em>Conservadora</em>, <em>Estándar</em> o <em>Urgente</em>.',
                    side: 'top',
                    align: 'start'
                }
            },
            {
                element: '#tt-preview',
                popover: {
                    title: '<i class="fas fa-chart-line text-gob-guinda mr-1"></i> 5. Simulación en Tiempo Real',
                    description: 'Observa la línea de tiempo interactiva que refleja con exactitud cómo se distribuirán los rangos verde, amarillo y rojo según los valores calibrados antes de guardar.',
                    side: 'top',
                    align: 'center'
                }
            },
            {
                popover: {
                    title: '<div class="siaj-tour-final-header"><div class="siaj-tour-final-icon"><i class="fas fa-shield-alt"></i></div><span>¡Parámetros Institucionales Dominados!</span></div>',
                    description: '<p class="text-gray-600 text-sm leading-relaxed mb-1">Recuerda que los ajustes aplican a <strong>nuevos registros</strong> de la materia, protegiendo la certeza jurídica de los plazos ya iniciados.</p><p class="text-xs text-gray-500 mt-2">Puedes consultar este recorrido cuando lo desees usando el botón <strong>"? Guía rápida"</strong>.</p>',
                    popoverClass: 'siaj-tour-popover siaj-tour-popover-final',
                    doneBtnText: '¡Entendido, volver a la configuración!',
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
            doneBtnText: '¡Entendido!',
            progressText: 'Paso {{current}} de {{total}}',
            steps: steps,
            onDestroyed: function () {
                localStorage.setItem(STORAGE_KEY, 'true');
            }
        });

        driverObj.drive();
    }

    /**
     * Muestra banner en primer uso de plazos y alertas
     */
    function initFirstTimeBanner() {
        const yaVisto = localStorage.getItem(STORAGE_KEY);
        if (yaVisto === 'true') return;

        setTimeout(function () {
            if (document.getElementById('siaj-ftue-plazos-toast')) return;

            const toast = document.createElement('div');
            toast.id = 'siaj-ftue-plazos-toast';
            toast.className = 'siaj-ftue-toast';
            toast.innerHTML = `
                <div class="flex items-start justify-between gap-3">
                    <div class="flex items-center gap-2 text-gob-guinda font-extrabold text-sm">
                        <i class="fas fa-sliders-h text-base text-gob-oro"></i>
                        <span>¿Primera vez en Plazos y Alertas?</span>
                    </div>
                    <button type="button" id="btn-cerrar-ftue-plazos" class="text-gray-400 hover:text-gray-600 transition-colors text-base" aria-label="Cerrar">
                        <i class="fas fa-times"></i>
                    </button>
                </div>
                <p class="text-xs text-gray-600 leading-relaxed">
                    Aprende cómo calibrar tipos de término, audiencias, días hábiles y umbrales de semáforo en 45 segundos.
                </p>
                <div class="flex items-center justify-end gap-2 pt-1">
                    <button type="button" id="btn-omitir-ftue-plazos" class="text-xs font-semibold text-gray-500 hover:text-gray-700 px-3 py-1.5 transition-colors">
                        Omitir
                    </button>
                    <button type="button" id="btn-comenzar-ftue-plazos" class="inline-flex items-center gap-1.5 text-xs font-bold text-white bg-gob-guinda hover:bg-gob-guindaDark px-3.5 py-1.5 rounded-lg shadow-sm transition-all">
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

            document.getElementById('btn-cerrar-ftue-plazos')?.addEventListener('click', cerrar);
            document.getElementById('btn-omitir-ftue-plazos')?.addEventListener('click', cerrar);

            document.getElementById('btn-comenzar-ftue-plazos')?.addEventListener('click', function () {
                cerrar();
                lanzarTourPlazos();
            });
        }, 1200);
    }

    window.lanzarTourPlazos = lanzarTourPlazos;
})();
