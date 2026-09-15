(() => {
  const selection = document.querySelector('[data-biometric-selection]');
  if (!selection) return;
  const centre = selection.querySelector('[name="centreId"]');
  const date = selection.querySelector('[name="appointmentDate"]');
  const time = selection.querySelector('[name="appointmentTime"]');
  const message = selection.closest('form').querySelector('[data-capacity-message]');
  let requestNumber = 0;

  async function updateAvailability() {
    const request = ++requestNumber;
    if (!centre.value || !date.value) return;
    const selectedTime = time.value;
    message.textContent = 'Checking availability…';
    try {
      const query = new URLSearchParams({applicationId: selection.dataset.applicationId, centreId: centre.value, date: date.value});
      const response = await fetch('/citizen/appointments/biometric/availability?' + query, {cache: 'no-store'});
      const result = await response.json();
      if (request !== requestNumber) return;
      if (!response.ok) throw new Error(result.error || 'Please sign in again to check availability.');
      time.replaceChildren(new Option('Select a time', ''));
      result.times.forEach(value => time.add(new Option(value, value)));
      time.value = result.times.includes(selectedTime) ? selectedTime : '';
      message.textContent = result.remaining > 0
        ? result.remaining + ' biometric places available at this centre on this date. Availability is checked again when confirming.'
        : 'This centre is full on this date. Choose another date or centre.';
    } catch (error) {
      if (request === requestNumber) message.textContent = error.message || 'Availability could not be loaded. Please try again.';
    }
  }
  centre.addEventListener('change', updateAvailability);
  date.addEventListener('change', updateAvailability);
  window.addEventListener('pageshow', updateAvailability);
})();
