// Shared by the NIC and passport forms, including renewals and corrections.
(() => {
  const form = document.querySelector('form[data-application-form]');
  if (!form) return;

  ['familyName', 'givenName', 'surname'].forEach(name => {
    const input = form.elements.namedItem(name);
    input.addEventListener('input', () => {
      const valid = !input.value || /^\p{L}[\p{L}\p{M} ]*$/u.test(input.value);
      input.setCustomValidity(valid ? '' : 'Enter letters and spaces only; remove numbers and symbols.');
    });
  });

  ['permanentPostalCode', 'postalPostalCode'].forEach(name => {
    const input = form.elements.namedItem(name);
    input.addEventListener('input', () => {
      input.setCustomValidity(!input.value || /^[0-9]{5}$/.test(input.value)
        ? '' : 'Enter exactly 5 digits for the postal code.');
    });
  });

  function setSection(id, active) {
    const section = document.getElementById(id);
    section.classList.toggle('hidden', !active);
    section.querySelectorAll('input').forEach(input => {
      input.disabled = !active;
      input.required = active;
    });
  }

  function updateSections() {
    setSection('postalAddress', !document.getElementById('samePostal').checked);
    setSection('dualDetails', document.getElementById('dualCitizen').checked);
  }
  document.getElementById('samePostal').addEventListener('change', updateSections);
  document.getElementById('dualCitizen').addEventListener('change', updateSections);
  updateSections();

  const today = new Date();
  const localDate = date => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  form.querySelectorAll('input[type="date"]').forEach(input => { input.max = localDate(today); });
  const yesterday = new Date(today);
  yesterday.setDate(today.getDate() - 1);
  form.elements.namedItem('dateOfBirth').max = localDate(yesterday);

  if (form.action.includes('/citizen/applications/nic/')) {
    const minimumNicAge = new Date(today);
    minimumNicAge.setFullYear(today.getFullYear() - 15);
    form.elements.namedItem('dateOfBirth').max = localDate(minimumNicAge);
  }
})();
