document.addEventListener("DOMContentLoaded", () => {

    if (!window.dashboardData) {
        console.error("dashboardData no definido");
        return;
    }

    renderEstatus();
    renderCargaUsuarios();
    renderGerencias();
    renderTrabajoMensual();
});

// =========================
// ESTATUS EXPEDIENTES
// =========================
function renderEstatus() {
    const d = dashboardData.estatusExpedientes;
    if (!d) return;

    new Chart(document.getElementById("chartEstatusExpedientes"), {
        type: "doughnut",
        data: {
            labels: d.labels,
            datasets: [{
                data: d.values
            }]
        }
    });
}

// =========================
// CARGA POR USUARIO
// =========================
function renderCargaUsuarios() {
    const d = dashboardData.cargaTrabajoUsuarios;
    if (!d) return;

    new Chart(document.getElementById("chartCargaTrabajoUsuarios"), {
        type: "bar",
        data: {
            labels: d.labels,
            datasets: [
                { label: "Expedientes", data: d.expedientes },
                { label: "Audiencias", data: d.audiencias },
                { label: "Términos", data: d.terminos }
            ]
        }
    });
}

// =========================
// GERENCIAS
// =========================
function renderGerencias() {
    const d = dashboardData.distribucionGerencias;
    if (!d) return;

    new Chart(document.getElementById("chartDistribucionGerencias"), {
        type: "bar",
        data: {
            labels: d.labels,
            datasets: [{
                label: "Expedientes",
                data: d.values
            }]
        }
    });
}

// =========================
// TRABAJO MENSUAL
// =========================
function renderTrabajoMensual() {
    const d = dashboardData.trabajoMensual;
    if (!d) return;

    new Chart(document.getElementById("chartTrabajoCompletado"), {
        type: "line",
        data: {
            labels: d.labels,
            datasets: [{
                label: "Expedientes",
                data: d.values,
                tension: 0.3
            }]
        }
    });
}
