document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('[data-email-loading-form]').forEach(function (form) {
        form.addEventListener('submit', function () {
            if (form.dataset.submitting === 'true') {
                return;
            }

            form.dataset.submitting = 'true';

            const button = form.querySelector('[data-email-loading-button]');
            const status = form.querySelector('[data-email-loading-status]');
            const loadingText = form.dataset.loadingText || 'Enviando...';
            const loadingStatus = form.dataset.loadingStatus || 'Estamos procesando la solicitud.';

            if (button) {
                button.disabled = true;
                button.setAttribute('aria-busy', 'true');
                button.classList.add('opacity-90', 'cursor-wait');
                button.innerHTML = '<i class="fas fa-circle-notch fa-spin"></i><span>' + loadingText + '</span>';
            }

            if (status) {
                status.textContent = loadingStatus;
                status.classList.remove('hidden');
            }
        });
    });
});
