/**
 * Lógica para los modales y acciones de la vista de detalle del expediente.
 */

const EXPEDIENTE_ID = window.location.pathname.split('/').pop();

// --- FUNCIONES GENÉRICAS DE MODALES ---

function openModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.remove('hidden');
        modal.classList.add('flex');
        document.body.style.overflow = 'hidden';
    }
}

function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.add('hidden');
        modal.classList.remove('flex');
        document.body.style.overflow = 'auto';
    }
}

// Cerrar modales al hacer clic fuera del contenido
window.onclick = function (event) {
    if (event.target.classList.contains('bg-gray-900/50')) {
        event.target.classList.add('hidden');
        event.target.classList.remove('flex');
        document.body.style.overflow = 'auto';
    }
}

// --- MÓDULO DE OBSERVACIONES (NOTAS) ---

async function openObservaciones() {
    openModal('modal-observaciones');
    await loadObservaciones();
}

async function loadObservaciones() {
    const container = document.getElementById('lista-observaciones');
    try {
        const response = await fetch(`/expedientes/${EXPEDIENTE_ID}/comentarios`);
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

            const div = document.createElement('div');
            div.className = 'bg-white p-3 rounded-lg border border-gray-200 shadow-sm';
            div.innerHTML = `
                <div class="flex justify-between items-start mb-2 border-b border-gray-100 pb-1">
                    <span class="text-xs font-bold text-blue-600 uppercase">${nota.usuarioNombre || 'Usuario'}</span>
                    <span class="text-[10px] text-gray-400">${fechaStr} ${horaStr}</span>
                </div>
                <p class="text-sm text-gray-700 whitespace-pre-wrap leading-relaxed">${nota.comentario}</p>
            `;
            container.appendChild(div);
        });

        container.scrollTop = container.scrollHeight;

    } catch (error) {
        console.error('Error al cargar notas:', error);
        container.innerHTML = '<p class="text-center text-red-500 py-4">Error al cargar las notas.</p>';
    }
}

async function guardarNota() {
    const textarea = document.getElementById('texto-nueva-nota');
    const texto = textarea.value.trim();
    const btn = document.getElementById('btn-guardar-nota');

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
                const Toast = Swal.mixin({
                    toast: true,
                    position: 'top-end',
                    showConfirmButton: false,
                    timer: 2000,
                    timerProgressBar: true
                });
                Toast.fire({ icon: 'success', title: 'Nota guardada' });
            }
        }
    } catch (error) {
        console.error('Error al guardar nota:', error);
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="fas fa-paper-plane"></i>';
    }
}

// --- MÓDULO DE CAMBIO DE ETAPA ---

async function updateEtapa(nuevaEtapa) {
    try {
        const response = await fetch(`/expedientes/${EXPEDIENTE_ID}/cambiar-etapa?etapa=${nuevaEtapa}`, {
            method: 'POST'
        });

        if (response.ok) {
            closeModal('modal-etapa');
            if (typeof Swal !== 'undefined') {
                Swal.fire({
                    icon: 'success',
                    title: 'Etapa Actualizada',
                    text: `El expediente ha pasado a la etapa: ${nuevaEtapa}`,
                    timer: 1500,
                    showConfirmButton: false
                }).then(() => {
                    window.location.reload();
                });
            } else {
                window.location.reload();
            }
        }
    } catch (error) {
        console.error('Error al cambiar etapa:', error);
        if (typeof Swal !== 'undefined') {
            Swal.fire('Error', 'No se pudo actualizar la etapa', 'error');
        }
    }
}

// --- INICIALIZACIÓN ---

document.addEventListener('DOMContentLoaded', () => {
    const btnGuardarNota = document.getElementById('btn-guardar-nota');
    if (btnGuardarNota) {
        btnGuardarNota.addEventListener('click', guardarNota);
    }

    const textareaNota = document.getElementById('texto-nueva-nota');
    if (textareaNota) {
        textareaNota.addEventListener('keydown', (e) => {
            if (e.ctrlKey && e.key === 'Enter') {
                guardarNota();
            }
        });
    }

    // Cargar etapas dinámicamente
    const listaEtapas = document.getElementById('lista-etapas');
    if (listaEtapas) {
        const etapas = ['TRAMITE', 'LAUDO', 'FIRME', 'CONCLUIDO'];
        etapas.forEach(etapa => {
            const btn = document.createElement('button');
            btn.onclick = () => updateEtapa(etapa);
            btn.className = 'w-full text-left p-3 rounded-lg border border-gray-200 hover:bg-green-50 hover:border-green-300 transition-all flex justify-between items-center group';
            btn.innerHTML = `
                <span class="text-sm font-medium text-gray-700">${etapa}</span>
                <i class="fas fa-check text-green-500 opacity-0 group-hover:opacity-100"></i>
            `;
            listaEtapas.appendChild(btn);
        });
    }
});
