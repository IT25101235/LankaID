// Opt in only the forms that need inline validation feedback.
document.querySelectorAll('form[data-validate]').forEach(form => {
  function showError(field) {
    if (!field.willValidate) return;
    let error = field.parentElement.querySelector('.client-field-error');
    if (!error) {
      error = document.createElement('span');
      error.className = 'client-field-error';
      error.style.cssText = 'display:block;color:#dc2626;font-size:12px;margin-top:5px;font-weight:500';
      error.setAttribute('role', 'alert');
      error.id = 'field-error-' + (field.id || field.name);
      field.insertAdjacentElement('afterend', error);
      field.setAttribute('aria-describedby', error.id);
    }
    error.textContent = field.validity.valid ? '' : field.validationMessage;
    field.setAttribute('aria-invalid', String(!field.validity.valid));
  }
  form.addEventListener('invalid', event => showError(event.target), true);
  form.addEventListener('input', event => {
    if (event.target.hasAttribute('aria-invalid')) showError(event.target);
  });
  form.addEventListener('change', event => {
    if (event.target.hasAttribute('aria-invalid')) showError(event.target);
  });
});
