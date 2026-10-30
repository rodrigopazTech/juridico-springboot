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

    // Filtro combinado de búsqueda y materia para paneles maestros
    function bindMateriaAndSearchFilter(selectId, searchId, listId, emptyId, countId) {
        const select = byId(selectId);
        const searchInput = byId(searchId);
        const emptyElem = byId(emptyId);
        const countElem = byId(countId);

        function filterRecords() {
            const selectedMateria = select?.value || '';
            const query = normalizeSearch(searchInput?.value.trim());
            let visible = 0;
            document.querySelectorAll(`#${listId} .pa-record`).forEach((record) => {
                const matchSearch = normalizeSearch(record.dataset.search).includes(query);
                const matchMateria = !selectedMateria || record.dataset.materia === selectedMateria;
                const isMatch = matchSearch && matchMateria;
                record.classList.toggle('hidden', !isMatch);
                if (isMatch) visible += 1;
            });
            toggle(emptyId, visible === 0);
            if (countElem) {
                countElem.textContent = `${visible} ${visible === 1 ? 'disponible' : 'configurados'}`;
            }
        }

        select?.addEventListener('change', filterRecords);
        searchInput?.addEventListener('input', filterRecords);
    }

    bindMateriaAndSearchFilter('filtro-materia-termino', 'buscar-tipo-termino', 'lista-tipos-termino', 'sin-resultados-termino', 'contador-terminos');
    bindMateriaAndSearchFilter('filtro-materia-audiencia', 'buscar-tipo-audiencia', 'lista-tipos-audiencia', 'sin-resultados-audiencia', 'contador-audiencias');

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

    // Toggle de códigos técnicos colapsables
    function setupCodeDrawer(btnId, drawerId) {
        const btn = byId(btnId);
        const drawer = byId(drawerId);
        if (!btn || !drawer) return;
        btn.addEventListener('click', (e) => {
            e.preventDefault();
            const isHidden = drawer.classList.contains('hidden');
            drawer.classList.toggle('hidden', !isHidden);
            btn.setAttribute('aria-expanded', String(isHidden));
            if (isHidden) {
                drawer.querySelector('input')?.focus();
            }
        });
    }

    setupCodeDrawer('btn-toggle-codigo-termino', 'wrap-codigo-termino');
    setupCodeDrawer('btn-toggle-codigo-audiencia', 'wrap-codigo-audiencia');

    const termForm = byId('form-tipo-termino');
    const termCode = byId('tt-codigo');
    const previewCodigoTermino = byId('preview-codigo-termino');

    function syncTermCodePreview() {
        if (!previewCodigoTermino) return;
        const codeVal = termCode?.value.trim();
        previewCodigoTermino.textContent = codeVal || 'AUTOMÁTICO';
    }

    function termMode() { return document.querySelector('input[name="modoPlazo"]:checked')?.value || 'CALCULADO'; }

    function clearTermValidity() {
        byId('tt-atencion')?.setCustomValidity('');
        byId('tt-critico')?.setCustomValidity('');
    }

    function validateTermAlerts() {
        const attentionInput = byId('tt-atencion');
        if (!attentionInput) return true;
        if (termMode() === 'SIN_VENCIMIENTO') {
            attentionInput.setCustomValidity('');
            return true;
        }
        const attention = number('tt-atencion');
        const critical = number('tt-critico');
        if (attention <= critical) {
            attentionInput.setCustomValidity('Los días de anticipación de Atención deben ser mayores que los de Crítico (ej. 5 días de atención y 2 días de crítico).');
            return false;
        }
        attentionInput.setCustomValidity('');
        return true;
    }

    function updateTermPreview() {
        if (!termForm) return;
        clearTermValidity();
        syncTermCodePreview();

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

        // Simulador de Fechas Procesales en Vivo (Cálculo Judicial Real)
        const today = new Date();
        const simInicio = byId('tt-sim-inicio');
        const simAtencion = byId('tt-sim-atencion');
        const simAtencionSub = byId('tt-sim-atencion-sub');
        const simCritico = byId('tt-sim-critico');
        const simCriticoSub = byId('tt-sim-critico-sub');
        const simVence = byId('tt-sim-vence');
        const simVenceSub = byId('tt-sim-vence-sub');

        if (calculated) {
            let due = calendar.shift(today, duration, business);
            if (!business && byId('tt-ajusta').checked) due = calendar.nextBusiness(due);
            const warningDate = calendar.shift(due, -attention, business);
            const criticalDate = calendar.shift(due, -critical, business);

            if (simInicio) simInicio.textContent = formatDate(today);
            if (simAtencion) simAtencion.textContent = formatDate(warningDate);
            if (simAtencionSub) simAtencionSub.textContent = `${plural(attention, 'día', 'días')} ${business ? 'hábiles' : 'naturales'} antes`;
            if (simCritico) simCritico.textContent = formatDate(criticalDate);
            if (simCriticoSub) simCriticoSub.textContent = `${critical === 0 ? 'Mismo día' : `${plural(critical, 'día', 'días')} ${business ? 'hábiles' : 'naturales'} antes`}`;
            if (simVence) simVence.textContent = formatDate(due);
            if (simVenceSub) {
                const weekday = due.toLocaleDateString('es-MX', { weekday: 'long' });
                simVenceSub.textContent = `Vence en ${weekday}`;
            }
        } else if (captured) {
            if (simInicio) simInicio.textContent = 'Al radicar';
            if (simAtencion) simAtencion.textContent = `-${attention} días`;
            if (simAtencionSub) simAtencionSub.textContent = 'Anticipación amarilla';
            if (simCritico) simCritico.textContent = `-${critical} días`;
            if (simCriticoSub) simCriticoSub.textContent = 'Anticipación roja';
            if (simVence) simVence.textContent = 'Por acuerdo';
            if (simVenceSub) simVenceSub.textContent = 'Fecha fijada por juzgado';
        }
    }

    function markSelected(button) {
        const list = button.closest('.pa-record-list');
        list?.querySelectorAll('.pa-record').forEach((record) => record.classList.remove('is-selected'));
        button.closest('.pa-record')?.classList.add('is-selected');
    }

    function editTerm(button) {
        if (!termForm) return;
        clearTermValidity();
        const data = button.dataset;
        value('tt-id', data.id); value('tt-materia', data.materia); value('tt-nombre', data.nombre);
        value('tt-codigo', data.codigo); value('tt-descripcion', data.descripcion); value('tt-dias', data.dias || 15);
        value('tt-tipodias', data.tipodias || 'HABILES'); value('tt-activo', data.activo || 'true');
        value('tt-atencion', data.atencion || 5); value('tt-critico', data.critico || 2);
        const mode = document.querySelector(`input[name="modoPlazo"][value="${data.modo}"]`);
        if (mode) mode.checked = true;
        byId('tt-ajusta').checked = data.ajusta === 'true';
        termCode.dataset.manual = 'true';
        syncTermCodePreview();
        byId('titulo-tipo-termino').textContent = `Editar: ${data.nombre}`;
        byId('cancelar-tipo-termino')?.classList.remove('hidden');
        markSelected(button);
        updateTermPreview();
    }

    function newTerm() {
        if (!termForm) return;
        clearTermValidity();
        termForm.reset();
        value('tt-id', ''); value('tt-activo', 'true');
        termCode.dataset.manual = '';
        syncTermCodePreview();
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
    
    termCode?.addEventListener('input', () => { 
        termCode.dataset.manual = termCode.value ? 'true' : ''; 
        syncTermCodePreview();
    });
    byId('tt-nombre')?.addEventListener('input', (event) => { 
        if (termCode && !termCode.dataset.manual) {
            termCode.value = slug(event.target.value);
            syncTermCodePreview();
        }
    });

    document.querySelectorAll('[data-term-preset]').forEach((button) => button.addEventListener('click', () => {
        clearTermValidity();
        const [attention, critical] = button.dataset.termPreset.split(',');
        value('tt-atencion', attention); value('tt-critico', critical);
        document.querySelectorAll('[data-term-preset]').forEach((item) => item.classList.toggle('is-selected', item === button));
        updateTermPreview();
    }));

    termForm?.addEventListener('submit', (event) => {
        if (termForm.dataset.readonly === 'true') { event.preventDefault(); return; }
        if (termMode() === 'SIN_VENCIMIENTO') return;
        if (!validateTermAlerts()) {
            event.preventDefault();
            byId('tt-atencion')?.reportValidity();
        }
    });

    // Panel de Audiencias
    const audienceForm = byId('form-tipo-audiencia');
    const audienceCode = byId('ta-codigo');
    const previewCodigoAudience = byId('preview-codigo-audiencia');

    function syncAudienceCodePreview() {
        if (!previewCodigoAudience) return;
        const codeVal = audienceCode?.value.trim();
        previewCodigoAudience.textContent = codeVal || 'AUTOMÁTICO';
    }

    function clearAudienceValidity() {
        byId('ta-atencion')?.setCustomValidity('');
        byId('ta-critico')?.setCustomValidity('');
    }

    function validateAudienceAlerts() {
        const attentionInput = byId('ta-atencion');
        if (!attentionInput) return true;
        const attention = number('ta-atencion');
        const critical = number('ta-critico');
        if (attention <= critical) {
            attentionInput.setCustomValidity('La anticipación de Atención debe ser mayor que la de Crítico (ej. 3 días de atención y 1 día de crítico).');
            return false;
        }
        attentionInput.setCustomValidity('');
        return true;
    }

    function updateAudiencePreview() {
        if (!audienceForm) return;
        clearAudienceValidity();
        syncAudienceCodePreview();

        const attention = Math.max(1, number('ta-atencion', 3));
        const critical = Math.max(0, number('ta-critico', 1));
        setTimeline('ta', Math.max(attention * 2, attention + 3), attention, critical);
        byId('ta-label-green').textContent = `Más de ${plural(attention, 'día', 'días')}`;
        byId('ta-label-yellow').textContent = critical + 1 <= attention ? `De ${attention} a ${plural(critical + 1, 'día', 'días')}` : `${attention} días`;
        byId('ta-label-red').textContent = critical === 0 ? 'El mismo día' : `${plural(critical, 'día', 'días')} o menos`;
        byId('ta-preview-copy').textContent = `SIAJ alertará con Atención ${plural(attention, 'día', 'días')} antes e Inminente ${critical === 0 ? 'el mismo día' : `${plural(critical, 'día', 'días')} antes`} de la celebración de la audiencia.`;
    }

    function editAudience(button) {
        if (!audienceForm) return;
        clearAudienceValidity();
        const data = button.dataset;
        value('ta-id', data.id); value('ta-materia', data.materia); value('ta-nombre', data.nombre);
        value('ta-codigo', data.codigo); value('ta-descripcion', data.descripcion); value('ta-duracion', data.duracion || 60);
        value('ta-modalidad', data.modalidad || 'PRESENCIAL'); value('ta-modalidades', data.modalidades || 'PRESENCIAL,VIRTUAL,HIBRIDA');
        value('ta-activo', data.activo || 'true');
        value('ta-atencion', Math.max(1, Math.ceil(Number(data.atencionHoras || 72) / 24)));
        value('ta-critico', Math.max(1, Math.ceil(Number(data.criticoHoras || 24) / 24)));
        audienceCode.dataset.manual = 'true';
        syncAudienceCodePreview();
        byId('titulo-tipo-audiencia').textContent = `Editar: ${data.nombre}`;
        byId('cancelar-tipo-audiencia')?.classList.remove('hidden');
        markSelected(button);
        updateAudiencePreview();
    }

    function newAudience() {
        if (!audienceForm) return;
        clearAudienceValidity();
        audienceForm.reset(); value('ta-id', ''); value('ta-activo', 'true');
        audienceCode.dataset.manual = '';
        syncAudienceCodePreview();
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
    ['ta-atencion', 'ta-critico'].forEach((id) => {
        byId(id)?.addEventListener('input', updateAudiencePreview);
        byId(id)?.addEventListener('change', updateAudiencePreview);
    });
    
    audienceCode?.addEventListener('input', () => { 
        audienceCode.dataset.manual = audienceCode.value ? 'true' : ''; 
        syncAudienceCodePreview();
    });
    byId('ta-nombre')?.addEventListener('input', (event) => { 
        if (audienceCode && !audienceCode.dataset.manual) {
            audienceCode.value = slug(event.target.value);
            syncAudienceCodePreview();
        }
    });

    document.querySelectorAll('[data-audience-preset]').forEach((button) => button.addEventListener('click', () => {
        clearAudienceValidity();
        const [attention, critical] = button.dataset.audiencePreset.split(',');
        value('ta-atencion', attention); value('ta-critico', critical);
        document.querySelectorAll('[data-audience-preset]').forEach((item) => item.classList.toggle('is-selected', item === button));
        updateAudiencePreview();
    }));

    audienceForm?.addEventListener('submit', (event) => {
        if (audienceForm.dataset.readonly === 'true') { event.preventDefault(); return; }
        if (!validateAudienceAlerts()) {
            event.preventDefault();
            byId('ta-atencion')?.reportValidity();
        }
    });

    // Gestión asíncrona de los botones de toggle en la lista
    document.querySelectorAll('.pa-record-state').forEach((form) => {
        form.addEventListener('submit', async (event) => {
            event.preventDefault();
            const submitBtn = form.querySelector('button[type="submit"]');
            const icon = form.querySelector('i.fa-solid');
            const record = form.closest('.pa-record');
            const recordMain = record?.querySelector('.pa-record-main');
            const statusBadge = record?.querySelector('.pa-status');
            const hiddenActivo = form.querySelector('input[name="activo"]');
            const targetActivo = hiddenActivo?.value === 'true';

            if (submitBtn) submitBtn.disabled = true;

            try {
                const response = await fetch(form.action, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: new URLSearchParams(new FormData(form)).toString()
                });

                if (response.ok || response.redirected) {
                    const nextActivo = !targetActivo;
                    if (hiddenActivo) hiddenActivo.value = String(nextActivo);
                    if (submitBtn) submitBtn.title = targetActivo ? 'Inactivar tipo' : 'Activar tipo';

                    if (icon) {
                        icon.classList.toggle('fa-toggle-on', targetActivo);
                        icon.classList.toggle('fa-toggle-off', !targetActivo);
                        icon.classList.toggle('text-emerald-600', targetActivo);
                        icon.classList.toggle('text-stone-400', !targetActivo);
                    }
                    if (statusBadge) {
                        statusBadge.textContent = targetActivo ? 'Activo' : 'Inactivo';
                        statusBadge.classList.toggle('is-active', targetActivo);
                        statusBadge.classList.toggle('is-inactive', !targetActivo);
                    }
                    if (recordMain) {
                        recordMain.dataset.activo = String(targetActivo);
                        const termId = byId('tt-id')?.value;
                        if (termId && termId === recordMain.dataset.id) {
                            value('tt-activo', String(targetActivo));
                        }
                        const audId = byId('ta-id')?.value;
                        if (audId && audId === recordMain.dataset.id) {
                            value('ta-activo', String(targetActivo));
                        }
                    }

                    if (typeof Swal !== 'undefined') {
                        Swal.fire({
                            toast: true,
                            position: 'top-end',
                            icon: 'success',
                            title: targetActivo ? 'Tipo activado institucionalmente' : 'Tipo inactivado',
                            showConfirmButton: false,
                            timer: 2000
                        });
                    }
                } else {
                    form.submit();
                }
            } catch (err) {
                form.submit();
            } finally {
                if (submitBtn) submitBtn.disabled = false;
            }
        });
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
