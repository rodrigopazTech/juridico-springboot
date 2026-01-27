document.addEventListener("DOMContentLoaded", () => {

    if (!window.dashboardData) {
        console.error("dashboardData NO está definido");
        return;
    }

    initCharts();
});

function initCharts() {
    estatusExpedientesChart();
    trabajoCompletadoChart();
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

function trabajoCompletadoChart() {
    const canvas = document.getElementById("chartTrabajoCompletado");
    if (!canvas) return;

    new Chart(canvas.getContext("2d"), {
        type: "line",
        data: {
            labels: window.dashboardData.trabajoCompletado.meses,
            datasets: [{
                label: "Casos cerrados",
                data: window.dashboardData.trabajoCompletado.valores,
                borderWidth: 2,
                tension: 0.3,
                fill: false
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
