(() => {
    'use strict';

    const byId = (id) => document.getElementById(id);
    const value = (id, next) => { const input = byId(id); if (input) input.value = next ?? ''; };
    const number = (id, fallback = 0) => Number.parseInt(byId(id)?.value, 10) || fallback;
    const toggle = (id, visible) => byId(id)?.classList.toggle('hidden', !visible);
    const plural = (amount, singular, pluralForm) => `${amount} ${amount === 1 ? singular : pluralForm}`;
    const normalizeSearch = (text) => (text || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
    const slug = (text) => (text || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toUpperCase().replace(/[^A-Z0-9]+/g, '_').replace(/^_+|_+$/g, '').slice(0, 50);

    function selectTab(name, updateHash = true) {
        const valid = ['terminos', 'audiencias', 'calendario'].includes(name) ? name : 'terminos';
        document.querySelectorAll('[data-tab]').forEach((tab) => {
            const active = tab.dataset.tab === valid;
            tab.classList.toggle('is-active', active);
            tab.setAttribute('aria-selected', String(active));
        });
        document.querySelectorAll('[data-panel]').forEach((panel) => panel.classList.toggle('hidden', panel.dataset.panel !== valid));
        if (updateHash && window.location.hash !== `#${valid}`) history.replaceState(null, '', `#${valid}`);
    }

    document.querySelectorAll('[data-tab]').forEach((tab) => tab.addEventListener('click', () => selectTab(tab.dataset.tab)));
    selectTab(window.location.hash.replace('#', ''), false);

    function bindSearch(inputId, listId, emptyId) {
        const input = byId(inputId);
        if (!input) return;
        input.addEventListener('input', () => {
            const query = normalizeSearch(input.value.trim());
            let visible = 0;
            document.querySelectorAll(`#${listId} .pa-record`).forEach((record) => {
                const match = normalizeSearch(record.dataset.search).includes(query);
                record.classList.toggle('hidden', !match);
                if (match) visible += 1;
            });
            toggle(emptyId, visible === 0);
        });
    }

    bindSearch('buscar-tipo-termino', 'lista-tipos-termino', 'sin-resultados-termino');
    bindSearch('buscar-tipo-audiencia', 'lista-tipos-audiencia', 'sin-resultados-audiencia');

    const calendar = (() => {
        const holidays = new Set([...document.querySelectorAll('[data-inhabil]')].map((item) => item.dataset.date));
        const suspensions = [...document.querySelectorAll('[data-suspension]')].map((item) => [item.dataset.start, item.dataset.end]);
        const iso = (date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
        const isBusiness = (date) => {
            const day = date.getDay();
            const key = iso(date);
            return day !== 0 && day !== 6 && !holidays.has(key) && !suspensions.some(([start, end]) => key >= start && key <= end);
        };
        const shift = (date, amount, business) => {
            const result = new Date(date.getFullYear(), date.getMonth(), date.getDate());
            let pending = Math.abs(amount);
            const direction = amount >= 0 ? 1 : -1;
            while (pending > 0) {
                result.setDate(result.getDate() + direction);
                if (!business || isBusiness(result)) pending -= 1;
            }
            return result;
        };
        const nextBusiness = (date) => {
            const result = new Date(date);
            while (!isBusiness(result)) result.setDate(result.getDate() + 1);
            return result;
        };
        return { shift, nextBusiness };
    })();

    const formatDate = (date) => new Intl.DateTimeFormat('es-MX', { day: '2-digit', month: '2-digit', year: 'numeric' }).format(date);

    function setTimeline(prefix, total, attention, critical) {
        const safeTotal = Math.max(total, attention + 1, 2);
        const green = Math.max(safeTotal - attention, 1);
        const yellow = Math.max(attention - critical, 1);
        const red = Math.max(critical, .5);
        const sum = green + yellow + red;
        const widths = [green, yellow, red].map((part) => `${Math.max(3, part * 100 / sum)}%`);
        const normalized = widths.map(Number.parseFloat);
        const normalizedSum = normalized.reduce((a, b) => a + b, 0);
        ['green', 'yellow', 'red'].forEach((color, index) => {
            const segment = byId(`${prefix}-segment-${color}`);
            if (segment) segment.style.width = `${normalized[index] * 100 / normalizedSum}%`;
        });
    }

    const termForm = byId('form-tipo-termino');
    const termCode = byId('tt-codigo');

    function termMode() { return document.querySelector('input[name="modoPlazo"]:checked')?.value || 'CALCULADO'; }

    function updateTermPreview() {
        if (!termForm) return;
        const mode = termMode();
        const calculated = mode === 'CALCULADO';
        const captured = mode === 'CAPTURADO';
        const noDeadline = mode === 'SIN_VENCIMIENTO';
        const unit = byId('tt-tipodias')?.value || 'HABILES';
        const business = unit === 'HABILES';
        const duration = Math.max(1, number('tt-dias', 15));
        const attention = Math.max(1, number('tt-atencion', 5));
        const critical = Math.max(0, number('tt-critico', 2));

        toggle('tt-config-plazo', !noDeadline);
        toggle('tt-duracion-wrap', calculated);
        toggle('tt-ajuste-wrap', calculated && !business);
        toggle('tt-capturado-note', captured);
        toggle('tt-sin-vencimiento-note', noDeadline);
        toggle('tt-alertas', !noDeadline);
        toggle('tt-preview', !noDeadline);

        byId('tt-dias').disabled = !calculated;
        byId('tt-dias').required = calculated;
        byId('tt-tipodias').disabled = noDeadline;
        byId('tt-ajusta').disabled = !calculated || business;
        byId('tt-atencion').disabled = noDeadline;
        byId('tt-critico').disabled = noDeadline;
        byId('tt-atencion').required = !noDeadline;
        byId('tt-critico').required = !noDeadline;
        if (business) byId('tt-ajusta').checked = false;
        if (noDeadline) return;

        const horizon = calculated ? duration : Math.max(attention * 2, attention + 3);
        setTimeline('tt', horizon, attention, critical);
        byId('tt-preview-total').textContent = calculated
            ? `Plazo total: ${plural(duration, 'día', 'días')} ${business ? 'hábiles' : 'naturales'}`
            : `Seguimiento en días ${business ? 'hábiles' : 'naturales'}`;
        byId('tt-label-green').textContent = `Más de ${plural(attention, 'día', 'días')}`;
        byId('tt-label-yellow').textContent = critical + 1 <= attention
            ? `De ${attention} a ${plural(critical + 1, 'día', 'días')}` : `${attention} días`;
        byId('tt-label-red').textContent = critical === 0 ? 'Al vencer' : `${plural(critical, 'día', 'días')} o menos`;
        byId('tt-preview-copy').textContent = `SIAJ mostrará Atención ${plural(attention, 'día', 'días')} antes y Crítico ${critical === 0 ? 'al vencer' : `${plural(critical, 'día', 'días')} antes`} del vencimiento.`;

        const dateCopy = byId('tt-preview-dates');
        if (!calculated) {
            dateCopy.textContent = 'Las fechas exactas se mostrarán cuando captures el vencimiento del caso.';
            return;
        }
        const today = new Date();
        let due = calendar.shift(today, duration, business);
        if (!business && byId('tt-ajusta').checked) due = calendar.nextBusiness(due);
        const warningDate = calendar.shift(due, -attention, business);
        const criticalDate = calendar.shift(due, -critical, business);
        dateCopy.textContent = `Si inicia hoy: Atención ${formatDate(warningDate)} · Crítico ${formatDate(criticalDate)} · Vencimiento ${formatDate(due)}.`;
    }

    function markSelected(button) {
        const list = button.closest('.pa-record-list');
        list?.querySelectorAll('.pa-record').forEach((record) => record.classList.remove('is-selected'));
        button.closest('.pa-record')?.classList.add('is-selected');
    }

    function editTerm(button) {
        if (!termForm) return;
        const data = button.dataset;
        value('tt-id', data.id); value('tt-materia', data.materia); value('tt-nombre', data.nombre);
        value('tt-codigo', data.codigo); value('tt-descripcion', data.descripcion); value('tt-dias', data.dias || 15);
        value('tt-tipodias', data.tipodias || 'HABILES'); value('tt-activo', data.activo || 'true');
        value('tt-atencion', data.atencion || 5); value('tt-critico', data.critico || 2);
        const mode = document.querySelector(`input[name="modoPlazo"][value="${data.modo}"]`);
        if (mode) mode.checked = true;
        byId('tt-ajusta').checked = data.ajusta === 'true';
        termCode.dataset.manual = 'true';
        byId('titulo-tipo-termino').textContent = `Editar: ${data.nombre}`;
        byId('cancelar-tipo-termino')?.classList.remove('hidden');
        markSelected(button);
        updateTermPreview();
    }

    function newTerm() {
        if (!termForm) return;
        termForm.reset();
        value('tt-id', ''); value('tt-activo', 'true');
        termCode.dataset.manual = '';
        byId('titulo-tipo-termino').textContent = 'Nuevo tipo de término';
        byId('cancelar-tipo-termino')?.classList.add('hidden');
        document.querySelectorAll('#lista-tipos-termino .pa-record').forEach((record) => record.classList.remove('is-selected'));
        updateTermPreview();
        byId('tt-nombre')?.focus();
    }

    document.querySelectorAll('[data-edit-term]').forEach((button) => button.addEventListener('click', () => editTerm(button)));
    byId('nuevo-tipo-termino')?.addEventListener('click', newTerm);
    byId('cancelar-tipo-termino')?.addEventListener('click', newTerm);
    byId('limpiar-tipo-termino')?.addEventListener('click', () => setTimeout(newTerm));
    document.querySelectorAll('input[name="modoPlazo"], #tt-dias, #tt-tipodias, #tt-ajusta, #tt-atencion, #tt-critico').forEach((input) => input.addEventListener('change', updateTermPreview));
    ['tt-dias', 'tt-atencion', 'tt-critico'].forEach((id) => byId(id)?.addEventListener('input', updateTermPreview));
    termCode?.addEventListener('input', () => { termCode.dataset.manual = termCode.value ? 'true' : ''; });
    byId('tt-nombre')?.addEventListener('input', (event) => { if (termCode && !termCode.dataset.manual) termCode.value = slug(event.target.value); });
    document.querySelectorAll('[data-term-preset]').forEach((button) => button.addEventListener('click', () => {
        const [attention, critical] = button.dataset.termPreset.split(',');
        value('tt-atencion', attention); value('tt-critico', critical);
        document.querySelectorAll('[data-term-preset]').forEach((item) => item.classList.toggle('is-selected', item === button));
        updateTermPreview();
    }));
    termForm?.addEventListener('submit', (event) => {
        if (termForm.dataset.readonly === 'true') { event.preventDefault(); return; }
        if (termMode() === 'SIN_VENCIMIENTO') return;
        const attention = number('tt-atencion'); const critical = number('tt-critico');
        byId('tt-atencion').setCustomValidity(attention <= critical ? 'Atención debe comenzar antes que Crítico.' : '');
        if (attention <= critical) { event.preventDefault(); byId('tt-atencion').reportValidity(); }
    });

    const audienceForm = byId('form-tipo-audiencia');
    const audienceCode = byId('ta-codigo');

    function updateAudiencePreview() {
        if (!audienceForm) return;
        const attention = Math.max(1, number('ta-atencion', 3));
        const critical = Math.max(0, number('ta-critico', 1));
        setTimeline('ta', Math.max(attention * 2, attention + 3), attention, critical);
        byId('ta-label-green').textContent = `Más de ${plural(attention, 'día', 'días')}`;
        byId('ta-label-yellow').textContent = critical + 1 <= attention ? `De ${attention} a ${plural(critical + 1, 'día', 'días')}` : `${attention} días`;
        byId('ta-label-red').textContent = critical === 0 ? 'El mismo día' : `${plural(critical, 'día', 'días')} o menos`;
        byId('ta-preview-copy').textContent = `SIAJ mostrará Atención ${plural(attention, 'día', 'días')} antes y Crítico ${critical === 0 ? 'el mismo día' : `${plural(critical, 'día', 'días')} antes`} de la audiencia.`;
    }

    function editAudience(button) {
        if (!audienceForm) return;
        const data = button.dataset;
        value('ta-id', data.id); value('ta-materia', data.materia); value('ta-nombre', data.nombre);
        value('ta-codigo', data.codigo); value('ta-descripcion', data.descripcion); value('ta-duracion', data.duracion || 60);
        value('ta-modalidad', data.modalidad || 'PRESENCIAL'); value('ta-modalidades', data.modalidades || 'PRESENCIAL,VIRTUAL,HIBRIDA');
        value('ta-activo', data.activo || 'true');
        value('ta-atencion', Math.max(1, Math.ceil(Number(data.atencionHoras || 72) / 24)));
        value('ta-critico', Math.max(1, Math.ceil(Number(data.criticoHoras || 24) / 24)));
        audienceCode.dataset.manual = 'true';
        byId('titulo-tipo-audiencia').textContent = `Editar: ${data.nombre}`;
        byId('cancelar-tipo-audiencia')?.classList.remove('hidden');
        markSelected(button);
        updateAudiencePreview();
    }

    function newAudience() {
        if (!audienceForm) return;
        audienceForm.reset(); value('ta-id', ''); value('ta-activo', 'true');
        audienceCode.dataset.manual = '';
        byId('titulo-tipo-audiencia').textContent = 'Nuevo tipo de audiencia';
        byId('cancelar-tipo-audiencia')?.classList.add('hidden');
        document.querySelectorAll('#lista-tipos-audiencia .pa-record').forEach((record) => record.classList.remove('is-selected'));
        updateAudiencePreview();
        byId('ta-nombre')?.focus();
    }

    document.querySelectorAll('[data-edit-audience]').forEach((button) => button.addEventListener('click', () => editAudience(button)));
    byId('nuevo-tipo-audiencia')?.addEventListener('click', newAudience);
    byId('cancelar-tipo-audiencia')?.addEventListener('click', newAudience);
    byId('limpiar-tipo-audiencia')?.addEventListener('click', () => setTimeout(newAudience));
    ['ta-atencion', 'ta-critico'].forEach((id) => { byId(id)?.addEventListener('input', updateAudiencePreview); byId(id)?.addEventListener('change', updateAudiencePreview); });
    audienceCode?.addEventListener('input', () => { audienceCode.dataset.manual = audienceCode.value ? 'true' : ''; });
    byId('ta-nombre')?.addEventListener('input', (event) => { if (audienceCode && !audienceCode.dataset.manual) audienceCode.value = slug(event.target.value); });
    document.querySelectorAll('[data-audience-preset]').forEach((button) => button.addEventListener('click', () => {
        const [attention, critical] = button.dataset.audiencePreset.split(',');
        value('ta-atencion', attention); value('ta-critico', critical);
        document.querySelectorAll('[data-audience-preset]').forEach((item) => item.classList.toggle('is-selected', item === button));
        updateAudiencePreview();
    }));
    audienceForm?.addEventListener('submit', (event) => {
        if (audienceForm.dataset.readonly === 'true') { event.preventDefault(); return; }
        const attention = number('ta-atencion'); const critical = number('ta-critico');
        byId('ta-atencion').setCustomValidity(attention <= critical ? 'Atención debe comenzar antes que Crítico.' : '');
        if (attention <= critical) { event.preventDefault(); byId('ta-atencion').reportValidity(); }
    });

    [termForm, audienceForm].forEach((form) => {
        if (form?.dataset.readonly === 'true') {
            form.querySelectorAll('input, select, textarea, button').forEach((control) => { control.disabled = true; });
        }
    });

    updateTermPreview();
    updateAudiencePreview();
    if (termForm) document.querySelector('[data-edit-term]')?.click();
    if (audienceForm) document.querySelector('[data-edit-audience]')?.click();
})();
