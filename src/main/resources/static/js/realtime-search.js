/**
 * realtime-search.js
 * Utilidades para búsqueda en tiempo real (CLIENT-SIDE ONLY).
 * Filtra las filas visibles de una tabla sin recargar la página.
 */

/**
 * Filtra las filas de un contenedor (tbody o div) según el texto del query.
 * @param {string} containerId  ID del elemento padre (tbody o div)
 * @param {string} query    Texto a buscar
 */
function filtrarTablaRealTime(containerId, query) {
    const container = document.getElementById(containerId);
    if (!container) return;

    // Obtener filas (pueden ser <tr> o <div> directos)
    const rows = container.children;
    const q = query.toLowerCase().trim();

    let visibles = 0;
    for (let i = 0; i < rows.length; i++) {
        const row = rows[i];
        // Si es un mensaje de "no hay resultados", lo ignoramos inicialmente
        if (row.id && row.id.includes('-empty')) continue;

        const texto = row.textContent.toLowerCase();
        const match = q === '' || texto.includes(q);
        row.style.display = match ? '' : 'none';
        if (match) visibles++;
    }

    // Opcional: Manejar mensaje de "Sin resultados" si existe
    const emptyMsg = document.getElementById(containerId + '-empty');
    if (emptyMsg) {
        emptyMsg.style.display = visibles === 0 && q !== '' ? '' : 'none';
    }
}

// Inicializar al cargar el DOM
document.addEventListener('DOMContentLoaded', () => {

    // === MÓDULO: Usuarios / Abogados ===
    const searchUsuarios = document.getElementById('input-search-usuarios');
    if (searchUsuarios) {
        searchUsuarios.addEventListener('input', function () {
            filtrarTablaRealTime('tabla-usuarios-body', this.value);
        });
    }

    // === MÓDULOS GLOBALES (Búsqueda en SERVIDOR) ===
    // (Búsqueda local CLIENT-SIDE deshabilitada intencionalmente. 
    // Ahora es asíncrona real desde main-EXP.js para paginación)

    // === MÓDULO: Términos (Listado Principal) ===
    // Deshabilitado localmente para usar búsqueda asíncrona global en terminos.js
    /*
    const searchTerminos = document.getElementById('input-search-terminos');
    if (searchTerminos) {
        searchTerminos.addEventListener('input', function () {
            filtrarTablaRealTime('tabla-terminos-body', this.value);
        });
    }
    */

    // === MÓDULO: Agenda ===
    const searchAgendaAud = document.getElementById('search-audiencias');
    if (searchAgendaAud) {
        searchAgendaAud.addEventListener('input', function () {
            filtrarTablaRealTime('audiencias-desahogadas-body', this.value);
        });
    }

    const searchAgendaTerm = document.getElementById('search-terminos');
    if (searchAgendaTerm) {
        searchAgendaTerm.addEventListener('input', function () {
            filtrarTablaRealTime('terminos-presentados-body', this.value);
        });
    }

    // === CATÁLOGOS ===
    const searchGerencias = document.getElementById('input-search-gerencias');
    if (searchGerencias) {
        searchGerencias.addEventListener('input', function () {
            filtrarTablaRealTime('tabla-gerencias-body', this.value);
        });
    }

    const searchOrganos = document.getElementById('input-search-organos');
    if (searchOrganos) {
        searchOrganos.addEventListener('input', function () {
            filtrarTablaRealTime('tabla-organos-body', this.value);
        });
    }

    const searchTiposAud = document.getElementById('input-search-tipos-audiencia');
    if (searchTiposAud) {
        searchTiposAud.addEventListener('input', function () {
            filtrarTablaRealTime('tabla-tipos-audiencia-body', this.value);
        });
    }
});
