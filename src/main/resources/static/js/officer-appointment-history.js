(() => {
  document.querySelectorAll('[data-appointment-history]').forEach((history) => {
    const form = history.querySelector('[data-appointment-filters]');
    const rows = Array.from(history.querySelectorAll('[data-appointment-row]'));
    if (!form || rows.length === 0) return;

    const search = form.querySelector('[data-appointment-search]');
    const status = form.querySelector('[data-appointment-status]');
    const from = form.querySelector('[data-appointment-from]');
    const to = form.querySelector('[data-appointment-to]');
    const clear = form.querySelector('[data-appointment-clear]');
    const count = history.querySelector('[data-appointment-count]');
    const empty = history.querySelector('[data-appointment-empty]');

    const applyFilters = () => {
      const query = search.value.trim().toLowerCase();
      let visible = 0;

      rows.forEach((row) => {
        const matchesSearch = !query || row.dataset.search.toLowerCase().includes(query);
        const matchesStatus = !status.value || row.dataset.status === status.value;
        const matchesFrom = !from.value || row.dataset.date >= from.value;
        const matchesTo = !to.value || row.dataset.date <= to.value;
        const show = matchesSearch && matchesStatus && matchesFrom && matchesTo;
        row.hidden = !show;
        if (show) visible += 1;
      });

      if (count) count.textContent = `${visible} appointment(s) shown`;
      if (empty) empty.hidden = visible !== 0;
    };

    form.addEventListener('submit', (event) => event.preventDefault());
    search.addEventListener('input', applyFilters);
    [status, from, to].forEach((control) => control.addEventListener('change', applyFilters));
    clear.addEventListener('click', () => {
      form.reset();
      applyFilters();
      search.focus();
    });
  });
})();
