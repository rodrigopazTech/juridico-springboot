let charts = {}; // Objeto para guardar instancias de Chart.js

document.addEventListener("DOMContentLoaded", () => {
    if (!window.dashboardData) {
        console.error("dashboardData no definido");
        return;
    }

    // Renderizado inicial
    renderAllCharts(window.dashboardData);

    // Evento para el filtro
    const filtro = document.getElementById("filtroGerencia");
    filtro.addEventListener("change", async (e) => {
        const gerenciaId = e.target.value;
        
        try {
            const response = await fetch(`/dashboard/data?gerenciaId=${gerenciaId}`);
            const data = await response.json();
            
            // Actualizar KPIs manualmente
            document.getElementById("kpiTotalExp").innerText = data.kpis.totalExpedientes;
            document.getElementById("kpiActivos").innerText = data.kpis.expedientesActivos;
            document.getElementById("kpiAudiencias").innerText = data.kpis.audienciasProgramadas;
            document.getElementById("kpiTerminos").innerText = data.kpis.terminosActivos;

            // Actualizar Gráficas
            renderAllCharts(data.dashboardData);
        } catch (error) {
            console.error("Error al filtrar:", error);
        }
    });
});

function renderAllCharts(data) {
    renderEstatus(data.estatusExpedientes);
    renderCargaUsuarios(data.cargaTrabajoUsuarios);
    renderGerencias(data.distribucionGerencias);
    renderTrabajoMensual(data.trabajoMensual);
}

// Función auxiliar para destruir chart si ya existe
function prepareCanvas(id) {
    if (charts[id]) {
        charts[id].destroy();
    }
}

function renderEstatus(d) {
    if (!d) return;
    prepareCanvas("chartEstatusExpedientes");
    charts["chartEstatusExpedientes"] = new Chart(document.getElementById("chartEstatusExpedientes"), {
        type: "doughnut",
        data: {
            labels: d.labels,
            datasets: [{ data: d.values }]
        },
        options: { responsive: true, maintainAspectRatio: false }
    });
}

function renderCargaUsuarios(d) {
    if (!d) return;
    prepareCanvas("chartCargaTrabajoUsuarios");
    charts["chartCargaTrabajoUsuarios"] = new Chart(document.getElementById("chartCargaTrabajoUsuarios"), {
        type: "bar",
        data: {
            labels: d.labels,
            datasets: [
                { label: "Expedientes", data: d.expedientes, backgroundColor: '#8B1E3F' },
                { label: "Audiencias", data: d.audiencias, backgroundColor: '#D4AF37' },
                { label: "Términos", data: d.terminos, backgroundColor: '#166534' }
            ]
        },
        options: { responsive: true, maintainAspectRatio: false }
    });
}

function renderGerencias(d) {
    if (!d) return;
    prepareCanvas("chartDistribucionGerencias");
    charts["chartDistribucionGerencias"] = new Chart(document.getElementById("chartDistribucionGerencias"), {
        type: "doughnut",
        data: {
            labels: d.labels,
            datasets: [{ data: d.values }]
        },
        options: { responsive: true, maintainAspectRatio: false }
    });
}

function renderTrabajoMensual(d) {
    if (!d) return;
    prepareCanvas("chartTrabajoCompletado");
    charts["chartTrabajoCompletado"] = new Chart(document.getElementById("chartTrabajoCompletado"), {
        type: "line",
        data: {
            labels: d.labels,
            datasets: [{
                label: "Expedientes",
                data: d.values,
                borderColor: '#8B1E3F',
                tension: 0.3,
                fill: true,
                backgroundColor: 'rgba(139, 30, 63, 0.1)'
            }]
        },
        options: { responsive: true, maintainAspectRatio: false }
    });
}