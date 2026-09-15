const registrationForm = document.getElementById('registrationForm');
const email = document.getElementById('email');
const phone = document.getElementById('phone');
const password = document.getElementById('password');
const confirmation = document.getElementById('confirmPassword');

function showClientError(input, errorId, message) {
  const error = document.getElementById(errorId);
  error.textContent = message;
  error.classList.toggle('hidden', !message);
  input.classList.toggle('border-red-500', Boolean(message));
  input.setAttribute('aria-invalid', message ? 'true' : 'false');
  return !message;
}

function validateEmail() {
  const value = email.value.trim();
  let message = '';
  if (!value) message = 'Email address is required.';
  else if (value.length > 100) message = 'Email cannot exceed 100 characters.';
  else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) message = 'Enter a valid email address, such as name@example.com.';
  return showClientError(email, 'emailClientError', message);
}

function validatePhone() {
  const value = phone.value.trim();
  let message = '';
  if (!value) message = 'Mobile number is required.';
  else if (!/^7[0-9]{8}$/.test(value)) message = 'Enter exactly 9 digits starting with 7, without the leading zero.';
  return showClientError(phone, 'phoneClientError', message);
}

function validatePassword() {
  let message = '';
  if (!password.value) message = 'Password is required.';
  else message = passwordError(password.value);
  return showClientError(password, 'passwordClientError', message);
}

function validateConfirmation() {
  let message = '';
  if (!confirmation.value) message = 'Please repeat your password.';
  else if (confirmation.value !== password.value) message = 'Passwords do not match.';
  return showClientError(confirmation, 'confirmationClientError', message);
}

const fields = [
  [email, validateEmail],
  [phone, validatePhone],
  [password, validatePassword],
  [confirmation, validateConfirmation]
];

fields.forEach(([input, validate]) => {
  input.addEventListener('blur', () => {
    input.dataset.touched = 'true';
    validate();
  });
  input.addEventListener('input', () => {
    if (input.dataset.touched) validate();
    if (input === password && confirmation.dataset.touched) validateConfirmation();
  });
});

registrationForm.addEventListener('submit', event => {
  fields.forEach(([input]) => { input.dataset.touched = 'true'; });
  const valid = [validateEmail(), validatePhone(), validatePassword(), validateConfirmation()].every(Boolean);
  if (!valid) {
    event.preventDefault();
    registrationForm.querySelector('[aria-invalid="true"]')?.focus();
  }
});

document.querySelectorAll('[data-toggle-password]').forEach(button => {
  button.addEventListener('click', () => {
    const input = document.getElementById(button.dataset.togglePassword);
    const showing = input.type === 'text';
    input.type = showing ? 'password' : 'text';
    button.querySelector('span').textContent = showing ? 'visibility' : 'visibility_off';
  });
});

// Display the same +94 / nine-digit entry used by walk-in booking.
function localMobile(value) {
  const number = value.trim().replace(/[ -]/g, '');
  if (/^\+947[0-9]{8}$/.test(number)) return number.slice(3);
  if (/^07[0-9]{8}$/.test(number)) return number.slice(1);
  return number;
}
phone.value = localMobile(phone.value);
phone.addEventListener('input', () => { phone.value = phone.value.replace(/[^0-9]/g, ''); });
phone.addEventListener('paste', event => {
  const number = localMobile(event.clipboardData.getData('text'));
  if (/^7[0-9]{8}$/.test(number)) {
    event.preventDefault();
    phone.value = number;
    phone.dispatchEvent(new Event('input'));
  }
});
