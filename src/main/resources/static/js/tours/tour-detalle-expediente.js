/**
 * SIAJ - Sistema Integral de Gestión Jurídica
 * Tour Guiado y Onboarding Contextual: Detalle del Expediente
 */
(function () {
    'use strict';

    const STORAGE_KEY = 'siaj_tour_detalle_expediente_visto';

    document.addEventListener('DOMContentLoaded', function () {
        initTourDetalle();
        initFirstTimeBanner();
    });

    /**
     * Inicializa el botón disparador del tour
     */
    function initTourDetalle() {
        const triggerBtn = document.getElementById('btn-iniciar-tour-detalle');
        if (!triggerBtn) return;

        triggerBtn.addEventListener('click', function (e) {
            e.preventDefault();
            lanzarTourDetalle();
        });
    }

    /**
     * Construye y ejecuta el tour con efecto Spotlight
     */
    function lanzarTourDetalle() {
        if (!window.driver || !window.driver.js || typeof window.driver.js.driver !== 'function') {
            console.warn('[SIAJ Tour] Driver.js no está cargado.');
            return;
        }

        const driver = window.driver.js.driver;

        // Determinar dinámicamente si existe la tarjeta de próxima audiencia
        const cardAudiencia = document.querySelector('#informacion-general-container .bg-orange-50');
        const targetPaso3 = cardAudiencia ? cardAudiencia : '#informacion-general-container';
        const descPaso3 = cardAudiencia
            ? 'Esta tarjeta te alerta de la comparecencia judicial más próxima: tipo de audiencia, fecha, hora y si comparece el abogado titular o un sustituto.'
            : 'En esta sección se mostrará automáticamente una tarjeta de alerta destacada cuando el juzgado fije una fecha para tu próxima audiencia.';

        const steps = [
            {
                element: '#encabezado-detalle-container',
                popover: {
                    title: '<i class="fas fa-landmark text-gob-guinda mr-1"></i> 1. Ficha y Estado Procesal',
                    description: 'Aquí encuentras el número oficial del caso, juzgado de radicación y su <strong>etapa procesal actual</strong> (Trámite, Sentencia, Concluido, etc.).',
                    side: 'bottom',
                    align: 'start'
                }
            },
            {
                element: '#contenedor-acciones-detalle',
                popover: {
                    title: '<i class="fas fa-toolbox text-gob-oro mr-1"></i> 2. Acciones del Expediente',
                    description: 'Herramientas rápidas: <strong>Volver</strong> al inventario, registrar <strong>Observaciones</strong> internas de trabajo, consultar la <strong>Trazabilidad</strong> completa o <strong>Editar</strong> la ficha técnica.',
                    side: 'bottom',
                    align: 'end'
                }
            },
            {
                element: targetPaso3,
                popover: {
                    title: '<i class="fas fa-gavel text-gob-guinda mr-1"></i> 3. Comparecencias Inmediatas',
                    description: descPaso3,
                    side: 'bottom',
                    align: 'center'
                }
            },
            {
                element: '#cruzado-datos-container',
                popover: {
                    title: '<i class="fas fa-hourglass-half text-gob-oro mr-1"></i> 4. Cruzado de Datos y Semáforos',
                    description: 'Supervisa tus <strong>Términos</strong> y <strong>Audiencias</strong> en un solo lugar. Cada plazo incluye un <strong>semáforo de color</strong> (🟢 A tiempo, 🟡 Por vencer, 🔴 Crítico) calculado con el calendario oficial de días hábiles.',
                    side: 'top',
                    align: 'center'
                }
            },
            {
                element: '#historial-lateral-container',
                popover: {
                    title: '<i class="fas fa-history text-gob-guinda mr-1"></i> 5. Historial y Bitácora',
                    description: 'Registro cronológico auditable: consulta cada movimiento realizado en este expediente con fecha, hora y abogado responsable. También puedes descargarlo a Excel.',
                    side: 'left',
                    align: 'start'
                }
            },
            {
                element: '#btn-gestor-documental-header',
                popover: {
                    title: '<i class="fas fa-folder-open text-gob-guinda mr-1"></i> 6. Gestor Documental',
                    description: 'Abre el repositorio digital del expediente para subir, consultar y organizar demandas, acuerdos, promociones y anexos oficiales en carpetas seguras.',
                    side: 'bottom',
                    align: 'end'
                }
            },
            {
                popover: {
                    title: '<div class="siaj-tour-final-header"><div class="siaj-tour-final-icon"><i class="fas fa-check"></i></div><span>¡Dominas el Detalle del Expediente!</span></div>',
                    description: '<p class="text-gray-600 text-sm leading-relaxed mb-1">Ya conoces cómo dar seguimiento puntual a los términos, comparecencias y documentos de este asunto.</p><p class="text-xs text-gray-500 mt-2">Recuerda que puedes relanzar esta guía en cualquier momento haciendo clic en el botón <strong>"? Guía rápida"</strong>.</p>',
                    popoverClass: 'siaj-tour-popover siaj-tour-popover-final',
                    doneBtnText: '¡Continuar con el expediente!',
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
            doneBtnText: '¡Continuar con el expediente!',
            progressText: 'Paso {{current}} de {{total}}',
            steps: steps,
            onDestroyed: function () {
                localStorage.setItem(STORAGE_KEY, 'true');
            }
        });

        driverObj.drive();
    }

    /**
     * Muestra un toast en el primer uso del detalle
     */
    function initFirstTimeBanner() {
        const yaVisto = localStorage.getItem(STORAGE_KEY);
        if (yaVisto === 'true') return;

        setTimeout(function () {
            if (document.getElementById('siaj-ftue-detalle-toast')) return;

            const toast = document.createElement('div');
            toast.id = 'siaj-ftue-detalle-toast';
            toast.className = 'siaj-ftue-toast';
            toast.innerHTML = `
                <div class="flex items-start justify-between gap-3">
                    <div class="flex items-center gap-2 text-gob-guinda font-extrabold text-sm">
                        <i class="fas fa-compass text-base text-gob-oro"></i>
                        <span>¿Primera vez en el expediente?</span>
                    </div>
                    <button type="button" id="btn-cerrar-ftue-detalle" class="text-gray-400 hover:text-gray-600 transition-colors text-base" aria-label="Cerrar">
                        <i class="fas fa-times"></i>
                    </button>
                </div>
                <p class="text-xs text-gray-600 leading-relaxed">
                    Descubre cómo gestionar términos procesales, audiencias, documentos oficiales y bitácora en un recorrido de 50 segundos.
                </p>
                <div class="flex items-center justify-end gap-2 pt-1">
                    <button type="button" id="btn-omitir-ftue-detalle" class="text-xs font-semibold text-gray-500 hover:text-gray-700 px-3 py-1.5 transition-colors">
                        Omitir
                    </button>
                    <button type="button" id="btn-comenzar-ftue-detalle" class="inline-flex items-center gap-1.5 text-xs font-bold text-white bg-gob-guinda hover:bg-gob-guindaDark px-3.5 py-1.5 rounded-lg shadow-sm transition-all">
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

            document.getElementById('btn-cerrar-ftue-detalle')?.addEventListener('click', cerrar);
            document.getElementById('btn-omitir-ftue-detalle')?.addEventListener('click', cerrar);

            document.getElementById('btn-comenzar-ftue-detalle')?.addEventListener('click', function () {
                cerrar();
                lanzarTourDetalle();
            });
        }, 1200);
    }

    window.lanzarTourDetalleExpediente = lanzarTourDetalle;
})();
