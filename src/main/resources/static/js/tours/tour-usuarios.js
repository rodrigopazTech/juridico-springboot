/**
 * SIAJ - Sistema Integral de Gestión Jurídica
 * Tour Guiado y Onboarding Contextual: Administración de Usuarios (/usuarios)
 */
(function () {
    'use strict';

    const STORAGE_KEY = 'siaj_tour_usuarios_visto';

    document.addEventListener('DOMContentLoaded', function () {
        initTourUsuarios();
        initFirstTimeBanner();
    });

    /**
     * Inicializa el botón disparador del tour
     */
    function initTourUsuarios() {
        const triggerBtn = document.getElementById('btn-iniciar-tour-usuarios');
        if (!triggerBtn) return;

        triggerBtn.addEventListener('click', function (e) {
            e.preventDefault();
            lanzarTourUsuarios();
        });
    }

    /**
     * Construye y ejecuta el tour con efecto Spotlight
     */
    function lanzarTourUsuarios() {
        if (!window.driver || !window.driver.js || typeof window.driver.js.driver !== 'function') {
            console.warn('[SIAJ Tour] Driver.js no está cargado.');
            return;
        }

        const driver = window.driver.js.driver;

        // Elementos interactivos
        const elLista = document.querySelector('.w-full.lg\\:w-1\\/3') || document.getElementById('lista-abogados')?.closest('div.bg-white');
        const elBtnAgregar = document.getElementById('btn-agregar-usuario') || document.querySelector('button[onclick*="iniciarNuevoUsuarioEnDetalle"]');
        const elSelectorEstado = document.querySelector('.flex.border.border-gray-200.bg-gray-100\\/80') || document.querySelector('a[href*="estado=activos"]')?.parentElement;
        const elBuscador = document.getElementById('input-search-usuarios')?.closest('form') || document.getElementById('input-search-usuarios');
        const elPrimeraTarjeta = document.querySelector('.tarjeta-abogado') || document.getElementById('lista-abogados');
        const elDetalle = document.querySelector('.w-full.lg\\:w-2\\/3');

        const steps = [];

        // Paso 1: Panel de Control de Usuarios
        if (elLista) {
            steps.push({
                element: elLista,
                popover: {
                    title: '<i class="fas fa-users-cog text-gob-guinda mr-1"></i> 1. Gestión Integral de Personal',
                    description: 'Aquí administras las cuentas de acceso, roles institucionales y adscripciones jurídicas de todo el equipo del SIAJ.',
                    side: 'right',
                    align: 'start'
                }
            });
        }

        // Paso 2: Botón Agregar Usuario
        if (elBtnAgregar) {
            steps.push({
                element: elBtnAgregar,
                popover: {
                    title: '<i class="fas fa-user-plus text-gob-oro mr-1"></i> 2. Alta de Colaboradores',
                    description: 'Pulsa para dar de alta un nuevo servidor público. Asigna su <strong>correo institucional, rol (Abogado, Gerente, Directivo)</strong> y contraseña de acceso.',
                    side: 'bottom',
                    align: 'center'
                }
            });
        }

        // Paso 3: Selector de Estado (Activos / Desactivados)
        if (elSelectorEstado) {
            steps.push({
                element: elSelectorEstado,
                popover: {
                    title: '<i class="fas fa-user-shield text-gob-guinda mr-1"></i> 3. Control de Estado',
                    description: 'Alterna entre personal con <strong>acceso vigente</strong> y cuentas <strong>desactivadas</strong>. Al suspender una cuenta se revoca el acceso pero se preserva la autoría histórica de sus actuaciones.',
                    side: 'bottom',
                    align: 'center'
                }
            });
        }

        // Paso 4: Búsqueda rápida
        if (elBuscador) {
            steps.push({
                element: elBuscador,
                popover: {
                    title: '<i class="fas fa-search text-gob-oro mr-1"></i> 4. Búsqueda y Filtrado',
                    description: 'Localiza cuentas al instante ingresando el nombre, correo o iniciales del usuario.',
                    side: 'bottom',
                    align: 'center'
                }
            });
        }

        // Paso 5: Tarjetas de Usuarios
        if (elPrimeraTarjeta) {
            steps.push({
                element: elPrimeraTarjeta,
                popover: {
                    title: '<i class="fas fa-id-badge text-gob-guinda mr-1"></i> 5. Selección y Competencias',
                    description: 'Haz clic sobre cualquier usuario para desplegar su ficha completa a la derecha: rol, gerencia, <strong>materias autorizadas</strong>, expedientes y términos bajo su responsabilidad.',
                    side: 'right',
                    align: 'center'
                }
            });
        }

        // Paso 6: Panel Detalle / Ficha Técnica
        if (elDetalle) {
            steps.push({
                element: elDetalle,
                popover: {
                    title: '<i class="fas fa-address-card text-gob-oro mr-1"></i> 6. Ficha de Perfil y Materias',
                    description: 'Desde este panel configuras las <strong>materias jurídicas</strong> asignadas (que determinan qué juicios puede ver el abogado), editas sus datos o reactivas/desactivas su cuenta.',
                    side: 'left',
                    align: 'start'
                }
            });
        }

        // Paso Final Modal
        steps.push({
            popover: {
                title: '<div class="siaj-tour-final-header"><div class="siaj-tour-final-icon"><i class="fas fa-check"></i></div><span>¡Gobernanza de Acceso Dominada!</span></div>',
                description: '<p class="text-gray-600 text-sm leading-relaxed mb-1">Cada rol y materia asignada define estrictamente los expedientes, términos y alertas que el colaborador podrá consultar.</p><p class="text-xs text-gray-500 mt-2">Puedes relanzar este recorrido cuando lo desees usando el botón <strong>"? Guía rápida"</strong>.</p>',
                popoverClass: 'siaj-tour-popover siaj-tour-popover-final',
                doneBtnText: '¡Entendido, administrar usuarios!',
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
            doneBtnText: '¡Entendido, administrar usuarios!',
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
            if (document.getElementById('siaj-ftue-usuarios-toast')) return;

            const toast = document.createElement('div');
            toast.id = 'siaj-ftue-usuarios-toast';
            toast.className = 'siaj-ftue-toast';
            toast.innerHTML = `
                <div class="flex items-start justify-between gap-3">
                    <div class="flex items-center gap-2 text-gob-guinda font-extrabold text-sm">
                        <i class="fas fa-users-cog text-base text-gob-oro"></i>
                        <span>¿Primera vez en Gestión de Usuarios?</span>
                    </div>
                    <button type="button" id="btn-cerrar-ftue-usuarios" class="text-gray-400 hover:text-gray-600 transition-colors text-base" aria-label="Cerrar">
                        <i class="fas fa-times"></i>
                    </button>
                </div>
                <p class="text-xs text-gray-600 leading-relaxed">
                    Aprende cómo dar de alta personal, asignar roles y parametrizar materias jurídicas en 40 segundos.
                </p>
                <div class="flex items-center justify-end gap-2 pt-1">
                    <button type="button" id="btn-omitir-ftue-usuarios" class="text-xs font-semibold text-gray-500 hover:text-gray-700 px-3 py-1.5 transition-colors">
                        Omitir
                    </button>
                    <button type="button" id="btn-comenzar-ftue-usuarios" class="inline-flex items-center gap-1.5 text-xs font-bold text-white bg-gob-guinda hover:bg-gob-guindaDark px-3.5 py-1.5 rounded-lg shadow-sm transition-all">
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

            document.getElementById('btn-cerrar-ftue-usuarios')?.addEventListener('click', cerrar);
            document.getElementById('btn-omitir-ftue-usuarios')?.addEventListener('click', cerrar);

            document.getElementById('btn-comenzar-ftue-usuarios')?.addEventListener('click', function () {
                cerrar();
                lanzarTourUsuarios();
            });
        }, 1200);
    }

    window.lanzarTourUsuarios = lanzarTourUsuarios;
})();
