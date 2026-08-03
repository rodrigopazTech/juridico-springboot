(function () {
    const DEFAULT_RULES = [
        { id: 'length', label: 'Minimo 8 caracteres', test: value => value.length >= 8 },
        { id: 'upper', label: 'Una mayuscula', test: value => /[A-Z]/.test(value) },
        { id: 'digit', label: 'Un numero', test: value => /\d/.test(value) },
        { id: 'special', label: 'Un simbolo', test: value => /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(value) },
        { id: 'spaces', label: 'Sin espacios', test: value => value.length > 0 && !/\s/.test(value) },
    ];

    function buildRuleItem(rule) {
        return `
            <li data-password-rule="${rule.id}" class="flex items-center gap-2 text-gray-500">
                <i class="fas fa-circle text-[8px] w-4 text-center"></i>
                <span>${rule.label}</span>
            </li>`;
    }

    function setButtonState(button, disabled) {
        if (!button) return;
        button.disabled = disabled;
        button.classList.toggle('opacity-50', disabled);
        button.classList.toggle('cursor-not-allowed', disabled);
    }

    function renderStatus(container, results, matches, confirmTouched) {
        const missing = results.filter(result => !result.ok).map(result => result.label);
        const message = container.querySelector('[data-password-message]');

        results.forEach(result => {
            const item = container.querySelector(`[data-password-rule="${result.id}"]`);
            if (!item) return;
            const icon = item.querySelector('i');
            item.classList.toggle('text-green-700', result.ok);
            item.classList.toggle('text-gray-500', !result.ok);
            icon.className = result.ok
                ? 'fas fa-check-circle w-4 text-center text-green-600'
                : 'fas fa-circle text-[8px] w-4 text-center text-gray-400';
        });

        const matchItem = container.querySelector('[data-password-match]');
        if (matchItem) {
            const icon = matchItem.querySelector('i');
            const ok = matches && confirmTouched;
            matchItem.classList.toggle('text-green-700', ok);
            matchItem.classList.toggle('text-gray-500', !ok);
            icon.className = ok
                ? 'fas fa-check-circle w-4 text-center text-green-600'
                : 'fas fa-circle text-[8px] w-4 text-center text-gray-400';
        }

        if (!message) return;
        if (missing.length > 0) {
            message.textContent = 'Falta: ' + missing.join(', ') + '.';
            message.className = 'mt-3 text-xs font-bold text-red-700';
        } else if (!confirmTouched) {
            message.textContent = 'Confirma la contrasena para continuar.';
            message.className = 'mt-3 text-xs font-bold text-gray-600';
        } else if (!matches) {
            message.textContent = 'Las contrasenas no coinciden.';
            message.className = 'mt-3 text-xs font-bold text-red-700';
        } else {
            message.textContent = 'La contrasena cumple con la politica de seguridad.';
            message.className = 'mt-3 text-xs font-bold text-green-700';
        }
    }

    window.attachPasswordPolicyValidator = function (options) {
        const passwordInput = document.getElementById(options.passwordInputId);
        const confirmInput = document.getElementById(options.confirmInputId);
        const submitButton = document.getElementById(options.submitButtonId);
        const container = document.getElementById(options.containerId);
        if (!passwordInput || !confirmInput || !submitButton || !container) return;

        container.innerHTML = `
            <div class="bg-gray-50 border border-gray-200 rounded-lg p-4 text-xs space-y-1">
                <p class="font-bold text-gray-700 mb-2">
                    <i class="fas fa-shield-alt text-gob-guinda mr-1"></i> Requisitos de la contrasena
                </p>
                <ul class="space-y-1">
                    ${DEFAULT_RULES.map(buildRuleItem).join('')}
                    <li data-password-match class="flex items-center gap-2 text-gray-500">
                        <i class="fas fa-circle text-[8px] w-4 text-center"></i>
                        <span>Confirmacion coincide</span>
                    </li>
                </ul>
                <p data-password-message class="mt-3 text-xs font-bold text-gray-600">Escribe una contrasena nueva.</p>
                <p class="mt-2 text-[11px] text-gray-500"><i class="fas fa-info-circle text-blue-500 mr-1"></i> Vigencia institucional: 90 dias.</p>
            </div>`;

        function validate() {
            const password = passwordInput.value;
            const confirmation = confirmInput.value;
            const results = DEFAULT_RULES.map(rule => ({ ...rule, ok: rule.test(password) }));
            const policyOk = results.every(result => result.ok);
            const confirmTouched = confirmation.length > 0;
            const matches = password === confirmation;

            renderStatus(container, results, matches, confirmTouched);
            setButtonState(submitButton, !(policyOk && confirmTouched && matches));
        }

        passwordInput.addEventListener('input', validate);
        confirmInput.addEventListener('input', validate);
        validate();
    };
})();
