(() => {
  function addToggle(input, index) {
    if (input.dataset.visibilityReady === 'true') return;
    if (input.parentElement?.querySelector('button[onclick*="togglePassword"]')) return;
    input.dataset.visibilityReady = 'true';

    if (!input.id) input.id = `password-field-${index + 1}`;
    const shell = document.createElement('div');
    shell.className = 'password-field-shell';
    input.parentNode.insertBefore(shell, input);
    shell.appendChild(input);

    const toggle = document.createElement('button');
    toggle.type = 'button';
    toggle.className = 'password-visibility-toggle';
    toggle.textContent = 'Show';
    toggle.setAttribute('aria-controls', input.id);
    toggle.setAttribute('aria-label', 'Show password');
    toggle.addEventListener('click', () => {
      const showing = input.type === 'text';
      input.type = showing ? 'password' : 'text';
      toggle.textContent = showing ? 'Show' : 'Hide';
      toggle.setAttribute('aria-label', showing ? 'Show password' : 'Hide password');
    });
    shell.appendChild(toggle);
  }

  document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('input[type="password"]')
      .forEach(addToggle);
  });
})();
