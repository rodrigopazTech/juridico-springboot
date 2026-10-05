function abrirModalVincularExpediente(asuntoId, folio, gerenciaId) {
    const modal = document.getElementById('modal-vincular-asunto');
    if (!modal) return;
    document.getElementById('asunto-id-vincular').value = asuntoId;
    document.getElementById('folio-asunto-vincular').textContent = folio || '';
    const select = document.getElementById('expediente-destino-asunto');
    if (select) {
        select.value = '';
        Array.from(select.options).forEach(option => {
            if (!option.value) return;
            const coincide = !gerenciaId || String(option.dataset.gerenciaId) === String(gerenciaId);
            option.disabled = !coincide;
            option.hidden = !coincide;
        });
    }
    modal.classList.remove('hidden');
}

function cerrarModalVincularAsunto() {
    document.getElementById('modal-vincular-asunto')?.classList.add('hidden');
}

function confirmarVinculacionAsunto(form) {
    if (!form.reportValidity()) return;
    Swal.fire({
        icon: 'question',
        title: '¿Vincular al expediente?',
        text: 'Los términos y audiencias del asunto pasarán al expediente seleccionado. La trazabilidad se conservará.',
        showCancelButton: true,
        confirmButtonColor: '#9D2449',
        confirmButtonText: 'Sí, vincular',
        cancelButtonText: 'Cancelar'
    }).then(result => {
        if (result.isConfirmed) form.submit();
    });
}
