document.addEventListener("DOMContentLoaded", () => {

    if (!window.dashboardData) {
        console.error("dashboardData NO está definido");
        return;
    }

    initCharts();
});

function initCharts() {
    estatusExpedientesChart();
    cargaTrabajoUsuariosChart();
    distribucionGerenciasChart();
    trabajoCompletadoMensualChart(); // ✅ NUEVO
}

function estatusExpedientesChart() {
    const canvas = document.getElementById("chartEstatusExpedientes");
    if (!canvas) return;

    new Chart(canvas, {
        type: "pie",
        data: {
            labels: window.dashboardData.estatusExpedientes.labels,
            datasets: [{ data: window.dashboardData.estatusExpedientes.values }]
        }
    });
}

function cargaTrabajoUsuariosChart() {
    const canvas = document.getElementById("chartCargaTrabajoUsuarios");
    if (!canvas) return;

    new Chart(canvas, {
        type: "bar",
        data: {
            labels: window.dashboardData.cargaTrabajoUsuarios.labels,
            datasets: [{
                label: "Expedientes",
                data: window.dashboardData.cargaTrabajoUsuarios.values
            }]
        }
    });
}

function distribucionGerenciasChart() {
    const canvas = document.getElementById("chartDistribucionGerencias");
    if (!canvas) return;

    new Chart(canvas, {
        type: "doughnut",
        data: {
            labels: window.dashboardData.distribucionGerencias.labels,
            datasets: [{
                data: window.dashboardData.distribucionGerencias.values
            }]
        }
    });
}

// ✅ NUEVO
function trabajoCompletadoMensualChart() {
    const canvas = document.getElementById("chartTrabajoCompletado");
    if (!canvas) return;

    new Chart(canvas, {
        type: "line",
        data: {
            labels: window.dashboardData.trabajoCompletadoMensual.labels,
            datasets: [{
                label: "Expedientes Completados",
                data: window.dashboardData.trabajoCompletadoMensual.values,
                fill: false,
                tension: 0.3
            }]
        }
    });
}
