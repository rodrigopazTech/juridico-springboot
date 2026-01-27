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
}

function estatusExpedientesChart() {
    const canvas = document.getElementById("chartEstatusExpedientes");
    if (!canvas) return;

    new Chart(canvas.getContext("2d"), {
        type: "pie",
        data: {
            labels: window.dashboardData.estatusExpedientes.labels,
            datasets: [{
                data: window.dashboardData.estatusExpedientes.values
            }]
        },
        options: {
            responsive: true,
            plugins: {
                legend: { position: "bottom" }
            }
        }
    });
}

function cargaTrabajoUsuariosChart() {
    const canvas = document.getElementById("chartCargaTrabajoUsuarios");
    if (!canvas) return;

    new Chart(canvas.getContext("2d"), {
        type: "bar",
        data: {
            labels: window.dashboardData.cargaTrabajoUsuarios.labels,
            datasets: [{
                label: "Expedientes asignados",
                data: window.dashboardData.cargaTrabajoUsuarios.values
            }]
        },
        options: {
            responsive: true,
            scales: {
                y: { beginAtZero: true }
            }
        }
    });
}
