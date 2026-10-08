(function () {
  const banner = document.getElementById('publicAnnouncement');
  if (!banner) return;

  const storageKey = 'lankaid-dismissed-announcement-' + banner.dataset.announcementId;
  try {
    if (sessionStorage.getItem(storageKey) === 'true') {
      banner.remove();
      return;
    }
  } catch (_) {
    // The notice still works when browser storage is unavailable.
  }

  const dismissButton = document.getElementById('dismissPublicAnnouncement');
  if (!dismissButton) return;

  dismissButton.addEventListener('click', function () {
    try { sessionStorage.setItem(storageKey, 'true'); } catch (_) {}
    banner.classList.add('is-dismissing');
    banner.addEventListener('animationend', function () { banner.remove(); }, { once: true });
  });
})();
