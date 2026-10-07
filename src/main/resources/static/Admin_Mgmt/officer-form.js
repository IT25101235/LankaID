document.querySelectorAll('form[data-officer-form]').forEach(form => {
  const password = form.querySelector('input[type="password"]');
  const phone = form.querySelector('input[name="phone"]');
  password.addEventListener('input', () => {
    password.setCustomValidity(!password.required && !password.value ? '' : passwordError(password.value));
  });
  phone.addEventListener('input', () => {
    phone.setCustomValidity(!phone.value || /^(?:\+94|0)?7[0-9]{8}$/.test(phone.value)
      ? '' : 'Enter a Sri Lankan mobile number: 0771234567 or +94771234567.');
  });
});
