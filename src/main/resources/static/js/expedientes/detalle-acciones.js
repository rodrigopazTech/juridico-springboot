/**
 * Lógica simplificada y robusta para los modales de la vista de detalle.
 */

console.log("DETALLE-ACCIONES: Cargando script...");

// Extracción del ID de forma segura
const pathParts = window.location.pathname.split('/').filter(p => p);
const EXPEDIENTE_ID = pathParts.length > 0 ? pathParts[pathParts.length - 1] : null;

console.log("DETALLE-ACCIONES: ID Detectado =", EXPEDIENTE_ID);

function openModal(modalId) {
    console.log("DETALLE-ACCIONES: Intentando abrir modal =", modalId);
    const modal = document.getElementById(modalId);
    if (!modal) {
        console.error("DETALLE-ACCIONES: No se encontró el elemento con ID:", modalId);
        alert("Error técnico: El modal '" + modalId + "' no existe en esta página.");
        return;
    }

    // Forzar visibilidad ignorando clases si es necesario
    modal.style.display = 'flex';
    modal.classList.remove('hidden');
    modal.classList.add('flex');
    document.body.style.overflow = 'hidden';
}

function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.style.display = 'none';
        modal.classList.add('hidden');
        modal.classList.remove('flex');
        document.body.style.overflow = 'auto';
    }
}

// Globalizar funciones para asegurar que sean accesibles desde onclick
window.openObservaciones = async function () {
    console.log("DETALLE-ACCIONES: Función openObservaciones ejecutada");
    openModal('modal-observaciones');
    await loadObservaciones();
};

window.abrirModalEdit = function (gerenciaId, materiaId, abogadoId) {
    console.log("DETALLE-ACCIONES: Función abrirModalEdit ejecutada", { gerenciaId, materiaId, abogadoId });
    openModal('modal-editar');

    // Inicialización de selectores (reutiliza lógica de main-EXP.js)
    if (typeof cargarMateriasExpediente === 'function') {
        cargarMateriasExpediente('-edit', '', '', materiaId).then(() => {
            if (typeof cargarAbogadosExpediente === 'function') {
                cargarAbogadosExpediente('-edit', '', '', abogadoId);
            }
        }).catch(err => console.error("Error al inicializar selectores de edición:", err));
    } else {
        console.warn("DETALLE-ACCIONES: cargarMateriasExpediente no está disponible.");
    }
};

// --- MÓDULO DE OBSERVACIONES (NOTAS) ---

async function loadObservaciones() {
    const container = document.getElementById('lista-observaciones');
    if (!container) return;

    try {
        const response = await fetch(`/expedientes/${EXPEDIENTE_ID}/comentarios`);
        if (!response.ok) throw new Error("Error en respuesta de servidor");

        const notas = await response.json();
        container.innerHTML = '';

        if (notas.length === 0) {
            container.innerHTML = `
                <div class="flex flex-col items-center justify-center h-full text-gray-400 opacity-60">
                    <i class="far fa-comment-dots text-4xl mb-2"></i>
                    <p class="text-sm italic">No hay observaciones registradas.</p>
                </div>`;
            return;
        }

        notas.forEach(nota => {
            const date = new Date(nota.createdAt);
            const fechaStr = date.toLocaleDateString('es-MX', { day: '2-digit', month: 'short', year: 'numeric' });
            const horaStr = date.toLocaleTimeString('es-MX', { hour: '2-digit', minute: '2-digit' });

            let badgeEtapa = '';
            if (nota.etapaProcesal) {
                const colorMap = {
                    'TRAMITE': 'bg-blue-100 text-blue-700 border-blue-200',
                    'LAUDO': 'bg-emerald-100 text-emerald-700 border-emerald-200',
                    'FIRME': 'bg-amber-100 text-amber-700 border-amber-200',
                    'CONCLUIDO': 'bg-gray-100 text-gray-700 border-gray-200'
                };
                const colorClass = colorMap[nota.etapaProcesal] || 'bg-gray-100 text-gray-700 border-gray-200';
                badgeEtapa = `<span class="px-2 py-0.5 rounded border text-[9px] font-bold uppercase ${colorClass}">${nota.etapaProcesal}</span>`;
            }

            const div = document.createElement('div');
            div.className = 'bg-white p-3 rounded-lg border border-gray-200 shadow-sm mb-3';
            div.innerHTML = `
                <div class="flex justify-between items-center mb-2 border-b border-gray-100 pb-1">
                    <div class="flex items-center gap-2">
                         <span class="text-xs font-bold text-blue-600 uppercase">${nota.usuarioNombre || 'Usuario'}</span>
                         ${badgeEtapa}
                    </div>
                    <span class="text-[10px] text-gray-400">${fechaStr} ${horaStr}</span>
                </div>
                <p class="text-sm text-gray-700 whitespace-pre-wrap leading-relaxed">${nota.comentario}</p>
            `;
            container.appendChild(div);
        });

        container.scrollTop = container.scrollHeight;

    } catch (error) {
        console.error('Error al cargar notas:', error);
        container.innerHTML = '<p class="text-center text-red-500 py-4">Error al cargar las observaciones.</p>';
    }
}

async function guardarNota() {
    const textarea = document.getElementById('texto-nueva-nota');
    const btn = document.getElementById('btn-guardar-nota');
    if (!textarea || !btn) return;

    const texto = textarea.value.trim();
    if (!texto) return;

    btn.disabled = true;
    btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';

    try {
        const response = await fetch(`/expedientes/${EXPEDIENTE_ID}/comentarios`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ comentario: texto })
        });

        if (response.ok) {
            textarea.value = '';
            await loadObservaciones();
            if (typeof Swal !== 'undefined') {
                Swal.fire({
                    toast: true,
                    position: 'top-end',
                    icon: 'success',
                    title: 'Nota guardada',
                    timer: 2000,
                    showConfirmButton: false
                });
            }
        }
    } catch (error) {
        console.error('Error al guardar nota:', error);
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="fas fa-paper-plane"></i>';
    }
}

// --- ACTUALIZAR ETAPA ---
window.updateEtapa = async function (nuevaEtapa) {
    try {
        const response = await fetch(`/expedientes/${EXPEDIENTE_ID}/cambiar-etapa?etapa=${nuevaEtapa}`, {
            method: 'POST'
        });

        if (response.ok) {
            closeModal('modal-etapa');
            location.reload();
        }
    } catch (error) {
        console.error('Error al cambiar etapa:', error);
    }
};

// Cerrar al hacer clic fuera
window.addEventListener('click', function (event) {
    if (event.target.classList.contains('fixed') && event.target.id.startsWith('modal-')) {
        closeModal(event.target.id);
    }
});

// Inicialización de listeners
document.addEventListener('DOMContentLoaded', () => {
    const btnGuardarNota = document.getElementById('btn-guardar-nota');
    if (btnGuardarNota) btnGuardarNota.addEventListener('click', guardarNota);

    const textareaNota = document.getElementById('texto-nueva-nota');
    if (textareaNota) {
        textareaNota.addEventListener('keydown', (e) => {
            if (e.ctrlKey && e.key === 'Enter') guardarNota();
        });
    }
});
