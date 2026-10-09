/*
 * Citizen navigation: notifications dropdown.
 * The bell opens a small box with the five newest notifications and a "See all" link to the
 * full notifications page. Messages are inserted with textContent, never as HTML.
 */
(function () {
  'use strict';

  if (window.__lankaidNotify) return;
  window.__lankaidNotify = true;

  var root = document.querySelector('[data-notify]');
  if (!root) return;

  var button = root.querySelector('[data-notify-button]');
  var panel = root.querySelector('[data-notify-panel]');
  var list = root.querySelector('[data-notify-list]');
  var unreadLabel = root.querySelector('[data-notify-unread]');
  if (!button || !panel || !list) return;

  var loading = false;

  function open() {
    panel.hidden = false;
    button.setAttribute('aria-expanded', 'true');
    load(false);
  }

  function close(returnFocus) {
    panel.hidden = true;
    button.setAttribute('aria-expanded', 'false');
    if (returnFocus) button.focus();
  }

  button.addEventListener('click', function (event) {
    event.stopPropagation();
    if (panel.hidden) open(); else close(false);
  });

  document.addEventListener('click', function (event) {
    if (!panel.hidden && !root.contains(event.target)) close(false);
  });

  document.addEventListener('keydown', function (event) {
    if (event.key === 'Escape' && !panel.hidden) close(true);
  });

  // Keyboard users who tab out of the box close it.
  document.addEventListener('focusin', function (event) {
    if (!panel.hidden && !root.contains(event.target)) close(false);
  });

  function load(quiet) {
    if (loading) return;
    loading = true;
    if (!quiet) showMessage('Loading…');
    fetch('/citizen/notifications/recent', {
      credentials: 'same-origin',
      headers: { 'Accept': 'application/json' }
    }).then(function (response) {
      if (response.status === 401) { window.location.href = '/login'; throw new Error('signed out'); }
      if (!response.ok) throw new Error('unavailable');
      return response.json();
    }).then(function (data) {
      updateBadge(data.unreadCount);
      if (!quiet || !panel.hidden) render(data.notifications || []);
    }).catch(function () {
      if (!quiet) showMessage('Notifications could not be loaded. Please try again.');
    }).then(function () { loading = false; });
  }

  function showMessage(text) {
    list.textContent = '';
    var item = document.createElement('li');
    item.className = 'citizen-notify-empty';
    item.textContent = text;
    list.appendChild(item);
  }

  function render(items) {
    if (!items.length) { showMessage('No notifications yet'); return; }
    list.textContent = '';
    items.forEach(function (entry) {
      var li = document.createElement('li');
      var link = document.createElement('a');
      link.className = 'citizen-notify-item' + (entry.read ? '' : ' is-unread');
      link.href = '/citizen/notifications';

      var dot = document.createElement('span');
      dot.className = 'citizen-notify-dot';
      dot.setAttribute('aria-hidden', 'true');

      var body = document.createElement('span');
      body.className = 'citizen-notify-body';

      var title = document.createElement('strong');
      title.textContent = label(entry.type) + (entry.read ? '' : ' (unread)');

      var message = document.createElement('span');
      message.className = 'citizen-notify-message';
      message.textContent = entry.message || '';

      var time = document.createElement('small');
      time.textContent = timeAgo(entry.createdAt);

      body.appendChild(title);
      body.appendChild(message);
      body.appendChild(time);
      link.appendChild(dot);
      link.appendChild(body);
      li.appendChild(link);
      list.appendChild(li);
    });
  }

  function updateBadge(count) {
    var unread = Number(count) || 0;
    if (unreadLabel) unreadLabel.textContent = unread > 0 ? unread + ' unread' : 'All read';
    var badge = button.querySelector('[data-notify-badge]');
    if (unread > 0) {
      if (!badge) {
        badge = document.createElement('span');
        badge.className = 'citizen-notification-tag';
        badge.setAttribute('data-notify-badge', '');
        button.appendChild(badge);
      }
      badge.textContent = unread > 99 ? '99+' : String(unread);
      button.setAttribute('aria-label', 'Notifications, ' + unread + ' unread');
    } else {
      if (badge) badge.remove();
      button.setAttribute('aria-label', 'Notifications');
    }
  }

  function label(type) {
    var text = String(type || 'Notification').replace(/_/g, ' ').toLowerCase();
    return text.charAt(0).toUpperCase() + text.slice(1);
  }

  function timeAgo(value) {
    var then = value ? new Date(value) : null;
    if (!then || isNaN(then.getTime())) return '';
    var seconds = Math.max(0, Math.round((Date.now() - then.getTime()) / 1000));
    if (seconds < 60) return 'Just now';
    var minutes = Math.round(seconds / 60);
    if (minutes < 60) return minutes + ' min ago';
    var hours = Math.round(minutes / 60);
    if (hours < 24) return hours + (hours === 1 ? ' hour ago' : ' hours ago');
    var days = Math.round(hours / 24);
    if (days < 7) return days + (days === 1 ? ' day ago' : ' days ago');
    return then.toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' });
  }

  // Pages whose bell was built in the browser have no server-side count yet.
  if (root.hasAttribute('data-notify-prefetch')) load(true);
})();
