/**
 * dashboard.js
 * Manejo de gráficas dinámicas y filtros por gerencia y grupos de gerencias
 */

// Paleta de colores basada en tailwind.config.js
const colorPalette = [
    '#691228', // gob-guinda
    '#4e0c1d', // gob-guindaDark
    '#D4C19C', // gob-oro
    '#545454', // gob-gris
    '#98989A', // gob-plata
    '#13322B', // gob-verde
    '#0C231E', // gob-verdeDark
    '#F5F5F5'  // gob-fondo
];

// Objeto global para almacenar las instancias de las gráficas
let chartInstances = {};

document.addEventListener("DOMContentLoaded", () => {

    if (!window.dashboardData) {
        console.error("dashboardData no está definido desde el servidor.");
        return;
    }

    // 1. Renderizar gráficas con los datos cargados inicialmente por Thymeleaf
    renderAllCharts(window.dashboardData);

    // 2. Escuchar cambios en el filtro de gerencia
    const filtroGerencia = document.getElementById("filtroGerencia");
    
    // Verificamos si es un SELECT (Directivos) para añadir el evento de cambio
    if (filtroGerencia && filtroGerencia.tagName === 'SELECT') {
        filtroGerencia.addEventListener("change", (e) => {
            const value = e.target.value;
            actualizarDashboard(value);
        });
    }
});

/**
 * Solicita nuevos datos al servidor y actualiza la vista
 * @param {string} value - ID de la gerencia individual (o vacío para todas)
 */
async function actualizarDashboard(value) {
    try {
        let url;
        if (value) {
            url = `/dashboard/data?gerenciaId=${value}`;
        } else {
            url = '/dashboard/data';
        }
        
        const response = await fetch(url);
        
        if (!response.ok) throw new Error("Error en la respuesta del servidor");
        
        const data = await response.json();

        // 3. Actualizar los números (KPIs) en la parte superior
        updateKPIs(data.kpis);

        // 4. Re-renderizar todas las gráficas con los nuevos datos
        renderAllCharts(data.dashboardData);

    } catch (error) {
        console.error("Error al actualizar el dashboard:", error);
        Swal.fire("Error", "No se pudieron obtener los datos filtrados", "error");
    }
}

/**
 * Actualiza los elementos de texto de los KPIs
 */
function updateKPIs(kpis) {
    document.getElementById("kpiTotalExp").innerText = kpis.totalExpedientes || 0;
    document.getElementById("kpiActivos").innerText = kpis.expedientesActivos || 0;
    document.getElementById("kpiAudiencias").innerText = kpis.audienciasProgramadas || 0;
    document.getElementById("kpiTerminos").innerText = kpis.terminosActivos || 0;
}

/**
 * Roles para los cuales se oculta la gráfica de distribución por gerencia
 */
const ROLES_SIN_GRAFICA_GERENCIA = ['JEFE_DEPTO', 'GERENTE'];

/**
 * Llama a las funciones de renderizado de cada gráfica
 */
function renderAllCharts(data) {
    renderEstatus(data.estatusExpedientes);
    renderCargaUsuarios(data.cargaTrabajoUsuarios);
    // Solo renderizar gráfica de gerencias si el usuario no es JEFE_DEPTO o GERENTE
    if (!ROLES_SIN_GRAFICA_GERENCIA.includes(window.userRole)) {
        renderGerencias(data.distribucionGerencias);
    }
    renderTrabajoMensual(data.trabajoMensual);
}

/**
 * Función auxiliar para destruir una gráfica si ya existe en el canvas
 */
function destroyExistingChart(id) {
    if (chartInstances[id]) {
        chartInstances[id].destroy();
    }
}

// ==========================================
// RENDERS DE GRÁFICAS INDIVIDUALEES
// ==========================================

function renderEstatus(d) {
    if (!d) return;
    const canvasId = "chartEstatusExpedientes";
    destroyExistingChart(canvasId);

    chartInstances[canvasId] = new Chart(document.getElementById(canvasId), {
        type: "doughnut",
        data: {
            labels: d.labels,
            datasets: [{
                data: d.values,
                backgroundColor: colorPalette.slice(0, 4),
                borderWidth: 1
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { position: 'bottom' } }
        }
    });
}

function renderCargaUsuarios(d) {
    if (!d) return;
    const canvasId = "chartCargaTrabajoUsuarios";
    destroyExistingChart(canvasId);

    chartInstances[canvasId] = new Chart(document.getElementById(canvasId), {
        type: "bar",
        data: {
            labels: d.labels,
            datasets: [
                { label: "Expedientes", data: d.expedientes, backgroundColor: colorPalette[0] },
                { label: "Audiencias", data: d.audiencias, backgroundColor: colorPalette[1] },
                { label: "Términos", data: d.terminos, backgroundColor: colorPalette[2] }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: { y: { beginAtZero: true } }
        }
    });
}

function renderGerencias(d) {
    if (!d) return;
    const canvasId = "chartDistribucionGerencias";
    destroyExistingChart(canvasId);

    chartInstances[canvasId] = new Chart(document.getElementById(canvasId), {
        type: "pie",
        data: {
            labels: d.labels,
            datasets: [{
                data: d.values,
                backgroundColor: colorPalette.slice(0, 5)
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { position: 'right' } }
        }
    });
}

function renderTrabajoMensual(d) {
    if (!d) return;
    const canvasId = "chartTrabajoCompletado";
    destroyExistingChart(canvasId);

    const baseColor = colorPalette[0];
    const rgbaColor = baseColor.replace('#', '').match(/.{2}/g).map(x => parseInt(x, 16)).join(', ');
    const backgroundColor = `rgba(${rgbaColor}, 0.1)`;

    chartInstances[canvasId] = new Chart(document.getElementById(canvasId), {
        type: "line",
        data: {
            labels: d.labels,
            datasets: [{
                label: "Expedientes Completados",
                data: d.values,
                borderColor: baseColor,
                backgroundColor: backgroundColor,
                fill: true,
                tension: 0.4
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: { y: { beginAtZero: true } }
        }
    });
}
