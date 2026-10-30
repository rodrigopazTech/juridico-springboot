/**
 * SIAJ - Sistema Integral de Gestión Jurídica
 * Tour Guiado y Onboarding Contextual: Administración de Catálogos (/catalogos)
 */
(function () {
    'use strict';

    const STORAGE_KEY = 'siaj_tour_catalogos_visto';

    document.addEventListener('DOMContentLoaded', function () {
        initTourCatalogos();
        initFirstTimeBanner();
    });

    /**
     * Inicializa el botón disparador del tour
     */
    function initTourCatalogos() {
        const triggerBtn = document.getElementById('btn-iniciar-tour-catalogos');
        if (!triggerBtn) return;

        triggerBtn.addEventListener('click', function (e) {
            e.preventDefault();
            lanzarTourCatalogos();
        });
    }

    /**
     * Construye y ejecuta el tour con efecto Spotlight
     */
    function lanzarTourCatalogos() {
        if (!window.driver || !window.driver.js || typeof window.driver.js.driver !== 'function') {
            console.warn('[SIAJ Tour] Driver.js no está cargado.');
            return;
        }

        const driver = window.driver.js.driver;

        // Elementos interactivos
        const elHero = document.querySelector('.catalogos-hero') || document.querySelector('h1');
        const elImpacto = document.querySelector('.catalogos-hero .bg-amber-50') || document.querySelector('.catalogos-hero');
        const elGerencias = document.querySelector('div.overflow-x-auto') || document.querySelector('a.card-gerencia-active')?.parentElement;
        const elNavTabs = document.getElementById('tab-nav') || document.querySelector('.catalogos-nav');
        const elPanels = document.querySelector('.catalogos-panels') || document.querySelector('.tab-panel:not(.hidden)');

        const steps = [];

        // Paso 1: Hero y Gobierno de Datos
        if (elHero) {
            steps.push({
                element: elHero,
                popover: {
                    title: '<i class="fas fa-cogs text-gob-guinda mr-1"></i> 1. Gobierno de Datos del SIAJ',
                    description: 'Aquí se administran las <strong>tablas maestras</strong> que alimentan todo el sistema: materias, juzgados, etapas procesales y tipos de juicio.',
                    side: 'bottom',
                    align: 'start'
                }
            });
        }

        // Paso 2: Impacto Compartido
        if (elImpacto) {
            steps.push({
                element: elImpacto,
                popover: {
                    title: '<i class="fas fa-exclamation-triangle text-amber-600 mr-1"></i> 2. Impacto Compartido',
                    description: 'Cualquier alta, edición o desactivación de un registro tiene <strong>efecto inmediato para todos los abogados</strong> adscritos a la gerencia activa.',
                    side: 'bottom',
                    align: 'center'
                }
            });
        }

        // Paso 3: Selector de Gerencias (si está disponible para el rol)
        if (elGerencias) {
            steps.push({
                element: elGerencias,
                popover: {
                    title: '<i class="fas fa-building text-gob-oro mr-1"></i> 3. Ámbito por Gerencia',
                    description: 'Los catálogos están organizados por área jurídica. Si tienes facultades directivas, puedes alternar entre gerencias para parametrizar sus listas maestras.',
                    side: 'bottom',
                    align: 'start'
                }
            });
        }

        // Paso 4: Menú de Familias de Catálogos
        if (elNavTabs) {
            steps.push({
                element: elNavTabs,
                popover: {
                    title: '<i class="fas fa-sitemap text-gob-guinda mr-1"></i> 4. Familias de Catálogos',
                    description: 'Navega entre <strong>Materias, Etapas Procesales, Órganos Jurisdiccionales, Tipos de Expediente, Sedes y Abogados</strong>. Los contadores muestran los registros activos en tiempo real.',
                    side: 'right',
                    align: 'start'
                }
            });
        }

        // Paso 5: Panel de Edición e Integridad
        if (elPanels) {
            steps.push({
                element: elPanels,
                popover: {
                    title: '<i class="fas fa-toggle-on text-gob-oro mr-1"></i> 5. Integridad y Altas',
                    description: 'Agrega nuevos registros con el botón superior. Recuerda que los elementos <strong>no se eliminan para proteger la historia de los juicios</strong>; se activan o desactivan con el switch.',
                    side: 'left',
                    align: 'start'
                }
            });
        }

        // Paso Final Modal
        steps.push({
            popover: {
                title: '<div class="siaj-tour-final-header"><div class="siaj-tour-final-icon"><i class="fas fa-check"></i></div><span>¡Catálogos Maestros Protegidos!</span></div>',
                description: '<p class="text-gray-600 text-sm leading-relaxed mb-1">Mantener las listas maestras limpias y actualizadas asegura que los semáforos procesales y reportes ejecutivos operen sin errores.</p><p class="text-xs text-gray-500 mt-2">Puedes relanzar este recorrido cuando lo desees usando el botón <strong>"? Guía rápida"</strong>.</p>',
                popoverClass: 'siaj-tour-popover siaj-tour-popover-final',
                doneBtnText: '¡Entendido, administrar catálogos!',
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
            doneBtnText: '¡Entendido, administrar catálogos!',
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
            if (document.getElementById('siaj-ftue-catalogos-toast')) return;

            const toast = document.createElement('div');
            toast.id = 'siaj-ftue-catalogos-toast';
            toast.className = 'siaj-ftue-toast';
            toast.innerHTML = `
                <div class="flex items-start justify-between gap-3">
                    <div class="flex items-center gap-2 text-gob-guinda font-extrabold text-sm">
                        <i class="fas fa-cogs text-base text-gob-oro"></i>
                        <span>¿Primera vez en Catálogos Maestros?</span>
                    </div>
                    <button type="button" id="btn-cerrar-ftue-catalogos" class="text-gray-400 hover:text-gray-600 transition-colors text-base" aria-label="Cerrar">
                        <i class="fas fa-times"></i>
                    </button>
                </div>
                <p class="text-xs text-gray-600 leading-relaxed">
                    Aprende cómo organizar tablas maestras por gerencia, dar de alta registros y mantener la integridad procesal en 45 segundos.
                </p>
                <div class="flex items-center justify-end gap-2 pt-1">
                    <button type="button" id="btn-omitir-ftue-catalogos" class="text-xs font-semibold text-gray-500 hover:text-gray-700 px-3 py-1.5 transition-colors">
                        Omitir
                    </button>
                    <button type="button" id="btn-comenzar-ftue-catalogos" class="inline-flex items-center gap-1.5 text-xs font-bold text-white bg-gob-guinda hover:bg-gob-guindaDark px-3.5 py-1.5 rounded-lg shadow-sm transition-all">
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

            document.getElementById('btn-cerrar-ftue-catalogos')?.addEventListener('click', cerrar);
            document.getElementById('btn-omitir-ftue-catalogos')?.addEventListener('click', cerrar);

            document.getElementById('btn-comenzar-ftue-catalogos')?.addEventListener('click', function () {
                cerrar();
                lanzarTourCatalogos();
            });
        }, 1200);
    }

    window.lanzarTourCatalogos = lanzarTourCatalogos;
})();
