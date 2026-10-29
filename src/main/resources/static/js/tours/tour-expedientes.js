/**
 * SIAJ - Sistema Integral de Gestión Jurídica
 * Tour Guiado y Onboarding Contextual: Módulo de Expedientes
 */
(function () {
    'use strict';

    const STORAGE_KEY = 'siaj_tour_expedientes_visto';

    document.addEventListener('DOMContentLoaded', function () {
        initTourExpedientes();
        initFirstTimeBanner();
    });

    /**
     * Inicializa el tour interactivo con Driver.js
     */
    function initTourExpedientes() {
        const triggerBtn = document.getElementById('btn-iniciar-tour-expedientes');
        if (!triggerBtn) return;

        triggerBtn.addEventListener('click', function (e) {
            e.preventDefault();
            lanzarTour();
        });
    }

    /**
     * Construye y ejecuta el tour con efecto Spotlight
     */
    function lanzarTour() {
        if (!window.driver || !window.driver.js || typeof window.driver.js.driver !== 'function') {
            console.warn('[SIAJ Tour] Driver.js no está cargado.');
            return;
        }

        const driver = window.driver.js.driver;

        // Determinar selector dinámico para el paso 3 (fila de expediente o contenedor)
        let filaTarget = '#tabla-expedientes-body tr.expedientes-clickable:first-child';
        let descPaso3 = 'Cada registro resume el número oficial, partes, juzgado, abogado asignado y etapa actual. <strong>Haz clic en cualquier parte de la fila</strong> para entrar al expediente completo.';

        const filaExistente = document.querySelector(filaTarget) || document.querySelector('.expedientes-clickable');
        if (!filaExistente) {
            filaTarget = '#grid-resultados';
            descPaso3 = 'Aquí se listarán los expedientes de tu área. Cuando tengas casos asignados o crees uno nuevo, podrás hacer clic en su fila para ver todo su historial.';
        } else if (!document.querySelector(filaTarget) && document.querySelector('.expedientes-clickable')) {
            filaTarget = '.expedientes-clickable:first-child';
        }

        const steps = [
            {
                element: '#formFiltros',
                popover: {
                    title: '<i class="fas fa-filter text-gob-oro mr-1"></i> 1. Delimita tu consulta',
                    description: 'Usa estos selectores para filtrar por <strong>Gerencia</strong>, <strong>Materia</strong>, <strong>Abogado asignado</strong>, <strong>Prioridad</strong> o <strong>Etapa procesal</strong>. La lista se actualizará al instante.',
                    side: 'bottom',
                    align: 'start'
                }
            },
            {
                element: '#form-search-expedientes',
                popover: {
                    title: '<i class="fas fa-search text-gob-guinda mr-1"></i> 2. Localización inmediata',
                    description: 'Escribe cualquier término: número de expediente o nombre de las partes involucradas. Si aplicaste varios filtros y deseas ver todo de nuevo, pulsa el botón <strong>Limpiar filtro</strong>.',
                    side: 'bottom',
                    align: 'start'
                }
            },
            {
                element: filaTarget,
                popover: {
                    title: '<i class="fas fa-folder-open text-gob-guinda mr-1"></i> 3. Acceso al expediente',
                    description: descPaso3,
                    side: 'bottom',
                    align: 'center'
                }
            },
            {
                element: '#btn-exportar-expedientes',
                popover: {
                    title: '<i class="fas fa-file-excel text-green-600 mr-1"></i> 4. Reportes en un clic',
                    description: '¿Necesitas entregar un reporte a tu jefatura? Este botón genera y descarga un archivo Excel (.xlsx) con exactamente los casos filtrados en pantalla.',
                    side: 'bottom',
                    align: 'end'
                }
            },
            {
                element: '#btn-nuevo-expediente',
                popover: {
                    title: '<i class="fas fa-plus-circle text-gob-guinda mr-1"></i> 5. Alta de expediente',
                    description: 'Registra un nuevo caso ingresando su número oficial, materia y datos generales. Los campos marcados con asterisco (*) son obligatorios.',
                    side: 'bottom',
                    align: 'end'
                }
            },
            {
                popover: {
                    title: '<div class="siaj-tour-final-header"><div class="siaj-tour-final-icon"><i class="fas fa-check"></i></div><span>¡Todo listo para comenzar!</span></div>',
                    description: '<p class="text-gray-600 text-sm leading-relaxed mb-1">Ya conoces los elementos clave para consultar, filtrar y gestionar expedientes en el sistema.</p><p class="text-xs text-gray-500 mt-2">Puedes volver a activar este recorrido cuando lo desees desde el botón <strong>"? Guía rápida"</strong> en la barra superior.</p>',
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
     * Muestra un banner flotante discreto en el primer uso (FTUE)
     */
    function initFirstTimeBanner() {
        const yaVisto = localStorage.getItem(STORAGE_KEY);
        if (yaVisto === 'true') return;

        // Esperar 1.2 segundos tras la carga para no abrumar al usuario
        setTimeout(function () {
            if (document.getElementById('siaj-ftue-toast')) return;

            const toast = document.createElement('div');
            toast.id = 'siaj-ftue-toast';
            toast.className = 'siaj-ftue-toast';
            toast.innerHTML = `
                <div class="flex items-start justify-between gap-3">
                    <div class="flex items-center gap-2 text-gob-guinda font-extrabold text-sm">
                        <i class="fas fa-compass text-base text-gob-oro"></i>
                        <span>¿Primera vez en Expedientes?</span>
                    </div>
                    <button type="button" id="btn-cerrar-ftue" class="text-gray-400 hover:text-gray-600 transition-colors text-base" aria-label="Cerrar">
                        <i class="fas fa-times"></i>
                    </button>
                </div>
                <p class="text-xs text-gray-600 leading-relaxed">
                    Toma un recorrido interactivo de 45 segundos para conocer cómo buscar, filtrar y registrar asuntos jurídicos sin complicaciones.
                </p>
                <div class="flex items-center justify-end gap-2 pt-1">
                    <button type="button" id="btn-omitir-ftue" class="text-xs font-semibold text-gray-500 hover:text-gray-700 px-3 py-1.5 transition-colors">
                        Omitir
                    </button>
                    <button type="button" id="btn-comenzar-ftue" class="inline-flex items-center gap-1.5 text-xs font-bold text-white bg-gob-guinda hover:bg-gob-guindaDark px-3.5 py-1.5 rounded-lg shadow-sm transition-all">
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

            document.getElementById('btn-cerrar-ftue')?.addEventListener('click', cerrar);
            document.getElementById('btn-omitir-ftue')?.addEventListener('click', cerrar);

            document.getElementById('btn-comenzar-ftue')?.addEventListener('click', function () {
                cerrar();
                lanzarTour();
            });
        }, 1200);
    }

    // Exponer función de relanzamiento en el scope global
    window.lanzarTourExpedientes = lanzarTour;
})();
