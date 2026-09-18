(() => {
  const phone = document.getElementById('phone');
  if (phone) {
    phone.value = phone.value.trim().replace(/[^0-9+]/g, '').replace(/^\+94/, '').replace(/^0/, '');
    phone.addEventListener('input', () => { phone.value = phone.value.replace(/[^0-9]/g, '').slice(0, 9); });
  }

  const form = document.getElementById('registrationForm');
  if (!form) return;
  const password = document.getElementById('password');
  const confirmation = document.getElementById('confirmPassword');

  function show(input, id, message) {
    const output = document.getElementById(id);
    output.textContent = message;
    output.classList.toggle('hidden', !message);
    input.classList.toggle('border-red-500', Boolean(message));
    input.setAttribute('aria-invalid', message ? 'true' : 'false');
    return !message;
  }
  function validatePassword() {
    return show(password, 'passwordClientError', password.value ? passwordError(password.value) : 'Password is required.');
  }
  function validateConfirmation() {
    const message = !confirmation.value ? 'Please repeat your password.'
      : confirmation.value !== password.value ? 'Passwords do not match.' : '';
    return show(confirmation, 'confirmationClientError', message);
  }
  [password, confirmation].forEach(input => input.addEventListener('blur', () => {
    input.dataset.touched = 'true';
    input === password ? validatePassword() : validateConfirmation();
  }));
  password.addEventListener('input', () => {
    if (password.dataset.touched) validatePassword();
    if (confirmation.dataset.touched) validateConfirmation();
  });
  confirmation.addEventListener('input', () => { if (confirmation.dataset.touched) validateConfirmation(); });
  form.addEventListener('submit', event => {
    password.dataset.touched = confirmation.dataset.touched = 'true';
    if (![validatePassword(), validateConfirmation()].every(Boolean)) {
      event.preventDefault();
      form.querySelector('[aria-invalid="true"]')?.focus();
    }
  });
})();
