/**
 * SIAJ - Sistema Integral de Gestión Jurídica
 * Tour Guiado y Onboarding Contextual: Asuntos Independientes (/asuntos-independientes)
 */
(function () {
    'use strict';

    const STORAGE_KEY = 'siaj_tour_asuntos_independientes_visto';

    document.addEventListener('DOMContentLoaded', function () {
        initTourAsuntosIndependientes();
        initFirstTimeBanner();
    });

    /**
     * Inicializa el botón disparador del tour
     */
    function initTourAsuntosIndependientes() {
        const triggerBtn = document.getElementById('btn-iniciar-tour-asuntos');
        if (!triggerBtn) return;

        triggerBtn.addEventListener('click', function (e) {
            e.preventDefault();
            lanzarTourAsuntos();
        });
    }

    /**
     * Construye y ejecuta el tour con efecto Spotlight
     */
    function lanzarTourAsuntos() {
        if (!window.driver || !window.driver.js || typeof window.driver.js.driver !== 'function') {
            console.warn('[SIAJ Tour] Driver.js no está cargado.');
            return;
        }

        const driver = window.driver.js.driver;

        // Elementos interactivos
        const elBanner = document.getElementById('asuntos-banner-intro') || document.querySelector('section > div:first-child');
        const elAcciones = document.getElementById('asuntos-acciones-rapidas') || document.querySelector('.flex.flex-wrap.gap-2');
        const elBuscador = document.querySelector('form[action*="asuntos-independientes"]');
        const elArticulo = document.querySelector('article');

        const steps = [];

        // Paso 1: Banner explicativo
        if (elBanner) {
            steps.push({
                element: elBanner,
                popover: {
                    title: '<i class="fas fa-folder-tree text-gob-guinda mr-1"></i> 1. ¿Qué es un Asunto Independiente?',
                    description: 'Concentra trámites, quejas, convenios o actuaciones que <strong>aún no tienen número de expediente judicial formal</strong>. Permite controlar plazos y audiencias sin dejarlos en el limbo.',
                    side: 'bottom',
                    align: 'start'
                }
            });
        }

        // Paso 2: Botones rápidos a Términos y Audiencias
        if (elAcciones) {
            steps.push({
                element: elAcciones,
                popover: {
                    title: '<i class="fas fa-plus-circle text-gob-oro mr-1"></i> 2. Creación de Nuevas Actuaciones',
                    description: 'Para crear un nuevo asunto independiente, ve al formulario de <strong>Términos</strong> o <strong>Audiencias</strong> y selecciona la opción <em>«Sin expediente»</em>. El sistema generará su folio <code>AI-AAAA-NNNN</code> automáticamente.',
                    side: 'bottom',
                    align: 'end'
                }
            });
        }

        // Paso 3: Barra de búsqueda
        if (elBuscador) {
            steps.push({
                element: elBuscador,
                popover: {
                    title: '<i class="fas fa-search text-gob-guinda mr-1"></i> 3. Búsqueda y Localización',
                    description: 'Filtra y localiza registros rápidamente ingresando el <strong>Folio SIAJ</strong>, nombre de la <strong>parte interesada</strong>, materia o abogado responsable.',
                    side: 'bottom',
                    align: 'center'
                }
            });
        }

        // Paso 4: Tarjeta de Asunto
        if (elArticulo) {
            steps.push({
                element: elArticulo,
                popover: {
                    title: '<i class="fas fa-id-card text-gob-guinda mr-1"></i> 4. Ficha y Trazabilidad',
                    description: 'Cada tarjeta resume el estado (Activo/Inactivo), fecha de referencia, parte promovente y abogado responsable.',
                    side: 'top',
                    align: 'start'
                }
            });

            const elFooterCard = elArticulo.querySelector('div.mt-4.flex.flex-wrap.gap-2') || elArticulo;
            steps.push({
                element: elFooterCard,
                popover: {
                    title: '<i class="fas fa-link text-gob-oro mr-1"></i> 5. Exploración y Vinculación Procesal',
                    description: 'Pulsa para inspeccionar sus <strong>términos fatales</strong> o <strong>audiencias</strong>. Cuando el juzgado radique la demanda, podrás <strong>vincular este asunto a su expediente definitivo</strong> para unificar la historia procesal.',
                    side: 'top',
                    align: 'center'
                }
            });
        }

        // Paso Final Modal
        steps.push({
            popover: {
                title: '<div class="siaj-tour-final-header"><div class="siaj-tour-final-icon"><i class="fas fa-check"></i></div><span>¡Control Prejudicial Dominado!</span></div>',
                description: '<p class="text-gray-600 text-sm leading-relaxed mb-1">Las actuaciones sin expediente tienen exactamente las mismas alertas y semáforos de riesgo que un expediente formal.</p><p class="text-xs text-gray-500 mt-2">Puedes relanzar este recorrido cuando lo desees usando el botón <strong>"? Guía rápida"</strong>.</p>',
                popoverClass: 'siaj-tour-popover siaj-tour-popover-final',
                doneBtnText: '¡Entendido, gracias!',
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
            doneBtnText: '¡Entendido, gracias!',
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
            if (document.getElementById('siaj-ftue-asuntos-toast')) return;

            const toast = document.createElement('div');
            toast.id = 'siaj-ftue-asuntos-toast';
            toast.className = 'siaj-ftue-toast';
            toast.innerHTML = `
                <div class="flex items-start justify-between gap-3">
                    <div class="flex items-center gap-2 text-gob-guinda font-extrabold text-sm">
                        <i class="fas fa-folder-tree text-base text-gob-oro"></i>
                        <span>¿Primera vez en Asuntos Independientes?</span>
                    </div>
                    <button type="button" id="btn-cerrar-ftue-asuntos" class="text-gray-400 hover:text-gray-600 transition-colors text-base" aria-label="Cerrar">
                        <i class="fas fa-times"></i>
                    </button>
                </div>
                <p class="text-xs text-gray-600 leading-relaxed">
                    Aprende cómo gestionar actuaciones preliminares sin expediente judicial y cómo vincularlas posteriormente en 40 segundos.
                </p>
                <div class="flex items-center justify-end gap-2 pt-1">
                    <button type="button" id="btn-omitir-ftue-asuntos" class="text-xs font-semibold text-gray-500 hover:text-gray-700 px-3 py-1.5 transition-colors">
                        Omitir
                    </button>
                    <button type="button" id="btn-comenzar-ftue-asuntos" class="inline-flex items-center gap-1.5 text-xs font-bold text-white bg-gob-guinda hover:bg-gob-guindaDark px-3.5 py-1.5 rounded-lg shadow-sm transition-all">
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

            document.getElementById('btn-cerrar-ftue-asuntos')?.addEventListener('click', cerrar);
            document.getElementById('btn-omitir-ftue-asuntos')?.addEventListener('click', cerrar);

            document.getElementById('btn-comenzar-ftue-asuntos')?.addEventListener('click', function () {
                cerrar();
                lanzarTourAsuntos();
            });
        }, 1200);
    }

    window.lanzarTourAsuntos = lanzarTourAsuntos;
})();
