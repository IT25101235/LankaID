(() => {
  const form = document.querySelector('[data-walkin-calendar]');
  if (!form) return;

  const centre = form.querySelector('[data-walkin-centre]');
  const slotSelect = form.querySelector('[data-walkin-slot]');
  const fallback = form.querySelector('[data-slot-fallback]');
  const calendarUi = form.querySelector('[data-calendar-ui]');
  const days = form.querySelector('[data-calendar-days]');
  const monthLabel = form.querySelector('[data-calendar-month]');
  const previous = form.querySelector('[data-calendar-previous]');
  const next = form.querySelector('[data-calendar-next]');
  const timePanel = form.querySelector('[data-time-panel]');
  const timeHeading = form.querySelector('[data-time-heading]');
  const timeSlots = form.querySelector('[data-time-slots]');
  const message = form.querySelector('[data-calendar-message]');
  const slots = [...slotSelect.options].slice(1).map(option => ({
    id: option.value,
    centreId: option.dataset.centreId,
    centreName: option.dataset.centreName,
    date: option.dataset.date,
    time: option.dataset.time,
    remaining: Number(option.dataset.remaining)
  }));
  if (!slots.length) return;

  const uniqueCentres = new Map(slots.map(slot => [slot.centreId, slot.centreName]));
  uniqueCentres.forEach((name, id) => centre.add(new Option(name, id)));
  fallback.hidden = true;
  calendarUi.hidden = false;
  let displayedMonth = monthStart(parseDate(slots[0].date));
  let selectedDate = '';

  function parseDate(value) {
    const [year, month, day] = value.split('-').map(Number);
    return new Date(year, month - 1, day);
  }
  function monthStart(date) { return new Date(date.getFullYear(), date.getMonth(), 1); }
  function dateValue(date) { return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`; }
  function centreSlots() { return slots.filter(slot => slot.centreId === centre.value); }

  function renderCalendar() {
    days.replaceChildren();
    slotSelect.value = '';
    timePanel.hidden = true;
    monthLabel.textContent = displayedMonth.toLocaleDateString(undefined, {month: 'long', year: 'numeric'});
    const available = new Map();
    centreSlots().forEach(slot => {
      const entries = available.get(slot.date) || [];
      entries.push(slot);
      available.set(slot.date, entries);
    });
    const first = monthStart(displayedMonth);
    const last = new Date(first.getFullYear(), first.getMonth() + 1, 0);
    for (let index = 0; index < first.getDay(); index++) {
      const empty = document.createElement('span');
      empty.className = 'min-h-16 border-b border-r bg-slate-50';
      days.append(empty);
    }
    for (let day = 1; day <= last.getDate(); day++) {
      const current = new Date(first.getFullYear(), first.getMonth(), day);
      const value = dateValue(current);
      const availableSlots = available.get(value);
      const button = document.createElement('button');
      button.type = 'button';
      button.className = 'min-h-16 border-b border-r p-2 text-left text-sm transition';
      button.disabled = !availableSlots;
      button.innerHTML = `<b>${day}</b>${availableSlots ? `<small class="mt-1 block font-bold text-emerald-700">${availableSlots.length} time${availableSlots.length === 1 ? '' : 's'}</small>` : ''}`;
      button.classList.add(...(availableSlots ? ['bg-emerald-50', 'hover:bg-emerald-100'] : ['bg-slate-50', 'text-slate-300']));
      if (availableSlots) button.addEventListener('click', () => selectDate(value, availableSlots, button));
      days.append(button);
    }
    const months = centreSlots().map(slot => monthStart(parseDate(slot.date)).getTime());
    previous.disabled = !months.some(month => month < displayedMonth.getTime());
    next.disabled = !months.some(month => month > displayedMonth.getTime());
    previous.classList.toggle('opacity-40', previous.disabled);
    next.classList.toggle('opacity-40', next.disabled);
  }

  function selectDate(value, availableSlots, button) {
    selectedDate = value;
    days.querySelectorAll('.ring-2').forEach(item => item.classList.remove('ring-2', 'ring-inset', 'ring-cyan-600'));
    button.classList.add('ring-2', 'ring-inset', 'ring-cyan-600');
    slotSelect.value = '';
    timePanel.hidden = false;
    timeHeading.textContent = parseDate(value).toLocaleDateString(undefined, {weekday: 'long', year: 'numeric', month: 'long', day: 'numeric'});
    timeSlots.replaceChildren();
    availableSlots.forEach(slot => {
      const choice = document.createElement('button');
      choice.type = 'button';
      choice.className = 'rounded-xl border bg-white p-4 text-left hover:border-cyan-600 hover:bg-cyan-50';
      const time = new Date(`${slot.date}T${slot.time}`).toLocaleTimeString(undefined, {hour: '2-digit', minute: '2-digit'});
      choice.innerHTML = `<b class="block">${time}</b><span class="mt-1 block text-xs text-emerald-700">${slot.remaining} place(s) available</span>`;
      choice.addEventListener('click', () => {
        timeSlots.querySelectorAll('button').forEach(item => item.classList.remove('border-cyan-600', 'bg-cyan-50', 'ring-2', 'ring-cyan-200'));
        choice.classList.add('border-cyan-600', 'bg-cyan-50', 'ring-2', 'ring-cyan-200');
        slotSelect.value = slot.id;
        message.textContent = `${time} selected. Availability will be checked again when you continue.`;
      });
      timeSlots.append(choice);
    });
  }

  centre.addEventListener('change', () => {
    selectedDate = '';
    const firstSlot = centreSlots()[0];
    if (firstSlot) displayedMonth = monthStart(parseDate(firstSlot.date));
    message.textContent = firstSlot ? 'Green dates have available appointments. Choose a date, then a time.' : 'No slots are available for this centre.';
    renderCalendar();
  });
  previous.addEventListener('click', () => { displayedMonth = new Date(displayedMonth.getFullYear(), displayedMonth.getMonth() - 1, 1); renderCalendar(); });
  next.addEventListener('click', () => { displayedMonth = new Date(displayedMonth.getFullYear(), displayedMonth.getMonth() + 1, 1); renderCalendar(); });
  form.addEventListener('submit', event => {
    if (slotSelect.value) return;
    event.preventDefault();
    message.textContent = selectedDate ? 'Choose an appointment time.' : 'Choose a service centre and an available date.';
    message.scrollIntoView({behavior: 'smooth', block: 'center'});
  });
  renderCalendar();
})();
