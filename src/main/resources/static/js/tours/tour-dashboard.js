/**
 * SIAJ - Sistema Integral de Gestión Jurídica
 * Tour Guiado y Onboarding Contextual: Dashboard Principal por Rol
 */
(function () {
    'use strict';

    const STORAGE_KEY = 'siaj_tour_dashboard_visto';

    document.addEventListener('DOMContentLoaded', function () {
        initTourDashboard();
        initFirstTimeBanner();
    });

    /**
     * Inicializa el botón disparador del tour
     */
    function initTourDashboard() {
        const triggerBtn = document.getElementById('btn-iniciar-tour-dashboard');
        if (!triggerBtn) return;

        triggerBtn.addEventListener('click', function (e) {
            e.preventDefault();
            lanzarTourDashboard();
        });
    }

    /**
     * Construye y ejecuta el tour con efecto Spotlight adaptativo según el rol
     */
    function lanzarTourDashboard() {
        if (!window.driver || !window.driver.js || typeof window.driver.js.driver !== 'function') {
            console.warn('[SIAJ Tour] Driver.js no está cargado.');
            return;
        }

        const driver = window.driver.js.driver;

        // Detección adaptativa de elementos según la vista cargada (Abogado vs Directivo/Gerente)
        const isAbogado = !!document.querySelector('.kpi-danger') || !!document.querySelector('select[name="materiaId"]');

        // Paso 1: Filtro o encabezado
        let elFiltro = document.getElementById('filtroGerencia') || 
                       document.getElementById('gerenteDashboardFilters') || 
                       document.querySelector('form select[name="materiaId"]')?.closest('form') ||
                       document.querySelector('main > section:first-child') ||
                       document.querySelector('main > div:first-child');

        // Paso 2: Tarjetas Métricas / KPIs
        let elKpis = document.querySelector('.kpi-danger')?.closest('section') || 
                     document.getElementById('kpiTotalExp')?.closest('div.grid') ||
                     document.querySelector('main .grid');

        // Paso 3: Términos críticos o gráfica de estatus
        let elTerminos = Array.from(document.querySelectorAll('h2, h3')).find(h => /t[eé]rminos cr[ií]ticos/i.test(h.textContent))?.closest('div.metric-card, div.rounded-2xl, div.bg-white') ||
                         document.getElementById('chartEstatusExpedientes')?.closest('div.bg-white') ||
                         document.querySelector('.chart-container')?.closest('div.bg-white');

        // Paso 4: Próximas audiencias o carga de trabajo
        let elAudiencias = Array.from(document.querySelectorAll('h2, h3')).find(h => /pr[oó]ximas audiencias/i.test(h.textContent))?.closest('div.metric-card, div.rounded-2xl, div.bg-white') ||
                           document.getElementById('chartCargaTrabajoUsuarios')?.closest('div.bg-white');

        // Paso 5: Tareas o distribución institucional
        let elTareasOCharts = Array.from(document.querySelectorAll('h2, h3')).find(h => /tareas personales/i.test(h.textContent))?.closest('div.metric-card, div.rounded-2xl, div.bg-white') ||
                              document.getElementById('chartDistribucionGerencias')?.closest('div.bg-white') ||
                              document.querySelector('div.chart-panel');

        const steps = [];

        if (elFiltro) {
            steps.push({
                element: elFiltro,
                popover: {
                    title: '<i class="fas fa-sliders-h text-gob-guinda mr-1"></i> 1. Alcance y Filtros',
                    description: isAbogado 
                        ? 'Filtra tus indicadores por <strong>materia jurídica</strong> asignada para concentrarte en tus expedientes activos.' 
                        : 'Segmenta el análisis global por <strong>gerencia, materia, abogado o periodo</strong> para evaluar la carga de trabajo institucional.',
                    side: 'bottom',
                    align: 'start'
                }
            });
        }

        if (elKpis) {
            steps.push({
                element: elKpis,
                popover: {
                    title: '<i class="fas fa-traffic-light text-gob-oro mr-1"></i> 2. Semáforos y Vencimientos Clave',
                    description: isAbogado
                        ? 'Identifica de inmediato lo prioritario: 🔴 <strong>Términos vencidos</strong>, <strong>Términos de hoy</strong> y <strong>Audiencias próximas</strong>. <em>Haz clic en cualquier tarjeta para abrir directamente esos registros.</em>'
                        : 'Supervisa el volumen de <strong>Expedientes totales, en trámite, audiencias y términos activos</strong> calculados en tiempo real.',
                    side: 'bottom',
                    align: 'center'
                }
            });
        }

        if (elTerminos) {
            steps.push({
                element: elTerminos,
                popover: {
                    title: '<i class="fas fa-hourglass-half text-gob-guinda mr-1"></i> 3. Control de Plazos Críticos',
                    description: isAbogado
                        ? 'Lista priorizada de actuaciones próximas a vencer. Permite anticipar promociones y proyectos antes de alcanzar el semáforo crítico.'
                        : 'Monitorea el desglose y avance procesal por estado para prevenir rezagos en las distintas áreas.',
                    side: 'top',
                    align: 'start'
                }
            });
        }

        if (elAudiencias) {
            steps.push({
                element: elAudiencias,
                popover: {
                    title: '<i class="fas fa-gavel text-gob-oro mr-1"></i> 4. Agenda y Comparecencias',
                    description: isAbogado
                        ? 'Consulta tus audiencias programadas con fecha, hora, juzgado o enlace para comparecencia virtual.'
                        : 'Revisa la distribución y balance de asuntos entre los abogados responsables de la unidad.',
                    side: 'top',
                    align: 'end'
                }
            });
        }

        if (elTareasOCharts) {
            steps.push({
                element: elTareasOCharts,
                popover: {
                    title: '<i class="fas fa-chart-pie text-gob-guinda mr-1"></i> 5. Métricas de Rendimiento',
                    description: isAbogado
                        ? 'Gestiona tus pendientes individuales para mantener la trazabilidad de tus actividades diarias.'
                        : 'Visualiza la distribución porcentual de expedientes y rendimiento histórico de la dirección.',
                    side: 'top',
                    align: 'center'
                }
            });
        }

        // Paso Final Modal
        steps.push({
            popover: {
                title: '<div class="siaj-tour-final-header"><div class="siaj-tour-final-icon"><i class="fas fa-check"></i></div><span>¡Centro de Mando Dominado!</span></div>',
                description: '<p class="text-gray-600 text-sm leading-relaxed mb-1">Tu Dashboard se actualiza automáticamente con cada actuación registrada en el sistema.</p><p class="text-xs text-gray-500 mt-2">Puedes relanzar este recorrido cuando lo desees usando el botón <strong>"? Guía rápida"</strong>.</p>',
                popoverClass: 'siaj-tour-popover siaj-tour-popover-final',
                doneBtnText: '¡Comenzar mi jornada!',
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
            doneBtnText: '¡Comenzar mi jornada!',
            progressText: 'Paso {{current}} de {{total}}',
            steps: steps,
            onDestroyed: function () {
                localStorage.setItem(STORAGE_KEY, 'true');
            }
        });

        driverObj.drive();
    }

    /**
     * Muestra banner en primer uso del dashboard
     */
    function initFirstTimeBanner() {
        const yaVisto = localStorage.getItem(STORAGE_KEY);
        if (yaVisto === 'true') return;

        setTimeout(function () {
            if (document.getElementById('siaj-ftue-dashboard-toast')) return;

            const toast = document.createElement('div');
            toast.id = 'siaj-ftue-dashboard-toast';
            toast.className = 'siaj-ftue-toast';
            toast.innerHTML = `
                <div class="flex items-start justify-between gap-3">
                    <div class="flex items-center gap-2 text-gob-guinda font-extrabold text-sm">
                        <i class="fas fa-tachometer-alt text-base text-gob-oro"></i>
                        <span>¿Primera vez en el Dashboard?</span>
                    </div>
                    <button type="button" id="btn-cerrar-ftue-dashboard" class="text-gray-400 hover:text-gray-600 transition-colors text-base" aria-label="Cerrar">
                        <i class="fas fa-times"></i>
                    </button>
                </div>
                <p class="text-xs text-gray-600 leading-relaxed">
                    Aprende cómo consultar semáforos de vencimiento, audiencias y control de expedientes en 45 segundos.
                </p>
                <div class="flex items-center justify-end gap-2 pt-1">
                    <button type="button" id="btn-omitir-ftue-dashboard" class="text-xs font-semibold text-gray-500 hover:text-gray-700 px-3 py-1.5 transition-colors">
                        Omitir
                    </button>
                    <button type="button" id="btn-comenzar-ftue-dashboard" class="inline-flex items-center gap-1.5 text-xs font-bold text-white bg-gob-guinda hover:bg-gob-guindaDark px-3.5 py-1.5 rounded-lg shadow-sm transition-all">
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

            document.getElementById('btn-cerrar-ftue-dashboard')?.addEventListener('click', cerrar);
            document.getElementById('btn-omitir-ftue-dashboard')?.addEventListener('click', cerrar);

            document.getElementById('btn-comenzar-ftue-dashboard')?.addEventListener('click', function () {
                cerrar();
                lanzarTourDashboard();
            });
        }, 1200);
    }

    window.lanzarTourDashboard = lanzarTourDashboard;
})();
