// Booking calendar (browser side). Calls /citizen/appointments/biometric/availability (AJAX, JSON) to show
// only dates and times that still have places.
// Input validation (client side): the form cannot be submitted until a date and time are chosen. The server
// checks everything again, so this is only for a friendlier page.
// Exception handling: a failed request shows a message instead of breaking the calendar.
(() => {
  const selection = document.querySelector('[data-biometric-selection]');
  if (!selection) return;

  const form = selection.closest('form');
  const centre = selection.querySelector('[name="centreId"]');
  const slotId = selection.querySelector('[name="slotId"]');
  const calendar = selection.querySelector('[data-calendar-days]');
  const monthLabel = selection.querySelector('[data-calendar-month]');
  const previous = selection.querySelector('[data-calendar-previous]');
  const next = selection.querySelector('[data-calendar-next]');
  const slotHeading = selection.querySelector('[data-slot-heading]');
  const slotList = selection.querySelector('[data-slot-list]');
  const message = form.querySelector('[data-capacity-message]');
  const earliest = parseDate(selection.dataset.earliestDate);
  const latest = parseDate(selection.dataset.latestDate);
  let displayedMonth = new Date(earliest.getFullYear(), earliest.getMonth(), 1);
  let availableDates = new Map();
  let requestNumber = 0;

  function parseDate(value) {
    const [year, month, day] = value.split('-').map(Number);
    return new Date(year, month - 1, day);
  }

  function dateValue(date) {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  }

  function monthStart(date) { return new Date(date.getFullYear(), date.getMonth(), 1); }
  function monthEnd(date) { return new Date(date.getFullYear(), date.getMonth() + 1, 0); }
  function clampStart(date) { return date < earliest ? earliest : date; }
  function clampEnd(date) { return date > latest ? latest : date; }

  async function loadMonth() {
    slotId.value = '';
    slotList.replaceChildren();
    slotHeading.textContent = 'Select an available date';
    if (!centre.value) {
      message.textContent = 'Select a service centre to load its published appointment dates.';
      renderCalendar();
      return;
    }
    const request = ++requestNumber;
    message.textContent = 'Loading published appointment dates…';
    const start = clampStart(monthStart(displayedMonth));
    const end = clampEnd(monthEnd(displayedMonth));
    if (start > end) {
      availableDates = new Map();
      renderCalendar();
      return;
    }
    try {
      const query = new URLSearchParams({
        applicationId: selection.dataset.applicationId,
        centreId: centre.value,
        startDate: dateValue(start),
        endDate: dateValue(end)
      });
      const response = await fetch('/citizen/appointments/biometric/availability?' + query, {cache: 'no-store'});
      const result = await response.json();
      if (request !== requestNumber) return;
      if (!response.ok) throw new Error(result.error || 'Availability could not be loaded.');
      availableDates = new Map(result.dates.map(day => [day.date, day]));
      renderCalendar();
      message.textContent = result.dates.length
        ? 'Green dates have available appointments. Choose a date to view its times.'
        : 'No appointments are published for this centre in this month.';
    } catch (error) {
      if (request !== requestNumber) return;
      availableDates = new Map();
      renderCalendar();
      message.textContent = error.message || 'Availability could not be loaded. Please try again.';
    }
  }

  function renderCalendar() {
    calendar.replaceChildren();
    monthLabel.textContent = displayedMonth.toLocaleDateString(undefined, {month: 'long', year: 'numeric'});
    const first = monthStart(displayedMonth);
    const last = monthEnd(displayedMonth);
    for (let index = 0; index < first.getDay(); index++) {
      const empty = document.createElement('div');
      empty.className = 'appointment-day border-b border-r bg-slate-50';
      calendar.append(empty);
    }
    for (let day = 1; day <= last.getDate(); day++) {
      const current = new Date(first.getFullYear(), first.getMonth(), day);
      const value = dateValue(current);
      const availability = availableDates.get(value);
      const button = document.createElement('button');
      button.type = 'button';
      button.className = 'appointment-day border-b border-r p-2 text-left transition';
      button.disabled = !availability || current < earliest || current > latest;
      button.innerHTML = `<b class="text-sm">${day}</b>` + (availability
        ? `<span class="mt-2 block text-[10px] font-bold text-emerald-700">AVAILABLE</span><small class="block text-[10px] text-slate-500">${availability.remaining} place(s)</small>`
        : '<span class="mt-2 block text-[10px] font-bold text-slate-300">—</span>');
      button.classList.add(...(availability
        ? ['bg-emerald-50', 'hover:bg-emerald-100']
        : ['bg-slate-50', 'text-slate-400']));
      if (availability) button.addEventListener('click', () => selectDate(value, availability, button));
      calendar.append(button);
    }
    previous.disabled = monthStart(displayedMonth) <= monthStart(earliest);
    next.disabled = monthStart(displayedMonth) >= monthStart(latest);
    previous.classList.toggle('opacity-40', previous.disabled);
    next.classList.toggle('opacity-40', next.disabled);
  }

  function selectDate(date, availability, button) {
    slotId.value = '';
    calendar.querySelectorAll('.selected').forEach(item => item.classList.remove('selected'));
    button.classList.add('selected');
    slotHeading.textContent = new Date(date + 'T00:00:00').toLocaleDateString(undefined, {
      weekday: 'long', year: 'numeric', month: 'long', day: 'numeric'
    });
    slotList.replaceChildren();
    availability.slots.forEach(slot => {
      const choice = document.createElement('button');
      choice.type = 'button';
      choice.className = 'rounded-xl border bg-white p-4 text-left hover:border-sky-500 hover:bg-sky-50';
      const formatted = new Date(date + 'T' + slot.time).toLocaleTimeString(undefined, {hour: '2-digit', minute: '2-digit'});
      choice.innerHTML = `<b class="block">${formatted}</b><span class="mt-1 block text-xs text-emerald-700">${slot.remaining} place(s) available</span>`;
      choice.addEventListener('click', () => {
        slotList.querySelectorAll('button').forEach(item => item.classList.remove('border-sky-600', 'bg-sky-50', 'ring-2', 'ring-sky-200'));
        choice.classList.add('border-sky-600', 'bg-sky-50', 'ring-2', 'ring-sky-200');
        slotId.value = String(slot.slotId);
        message.textContent = `${formatted} selected. Availability will be checked again when you confirm.`;
      });
      slotList.append(choice);
    });
  }

  previous.addEventListener('click', () => {
    displayedMonth = new Date(displayedMonth.getFullYear(), displayedMonth.getMonth() - 1, 1);
    loadMonth();
  });
  next.addEventListener('click', () => {
    displayedMonth = new Date(displayedMonth.getFullYear(), displayedMonth.getMonth() + 1, 1);
    loadMonth();
  });
  centre.addEventListener('change', loadMonth);
  form.addEventListener('submit', event => {
    if (slotId.value) return;
    event.preventDefault();
    message.textContent = 'Select an available date and time before confirming.';
    message.scrollIntoView({behavior: 'smooth', block: 'center'});
  });
  window.addEventListener('pageshow', loadMonth);
})();
