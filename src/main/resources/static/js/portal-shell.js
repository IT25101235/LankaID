(function () {
  'use strict';

  const path = window.location.pathname.replace(/;[^/]+/g, '').replace(/\/$/, '') || '/';
  const isOfficer = path.startsWith('/officer/');
  const isCitizen = path.startsWith('/citizen/') || path === '/walk-in' || path.startsWith('/walk-in/');
  if (!isOfficer && !isCitizen) return;

  if (!document.querySelector('link[href^="/css/government-theme.css"]')) {
    const stylesheet = document.createElement('link');
    stylesheet.rel = 'stylesheet';
    stylesheet.href = '/css/government-theme.css?v=20260917';
    document.head.appendChild(stylesheet);
  }

  if (!document.querySelector('link[href*="Material+Symbols"]')) {
    const icons = document.createElement('link');
    icons.rel = 'stylesheet';
    icons.href = 'https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined';
    document.head.appendChild(icons);
  }

  const publicWalkIn = !isOfficer && (path === '/walk-in' || path.startsWith('/walk-in/'));
  const role = detectRole(path, isOfficer);
  const config = isOfficer ? officerConfig(role) : (publicWalkIn ? publicWalkInConfig() : citizenConfig());
  const officerRank = isOfficer ? (document.body.dataset.officerRank || config.roleName) : '';
  const officerEmail = isOfficer ? (document.body.dataset.officerEmail || '') : '';
  const officerLoginTime = isOfficer ? (document.body.dataset.officerLoginTime || '') : '';
  document.body.classList.add('portal-shell', isOfficer ? 'portal-officer' : 'portal-citizen');

  if (isOfficer) {
    mountOfficerSidebar();
    if (role !== 'SYSTEM_ADMIN') refreshOfficerNotificationBadge();
  }
  else mountCitizenTopbar(publicWalkIn);

  function mountOfficerSidebar() {
    const sidebar = document.createElement('aside');
    sidebar.className = 'portal-sidebar';
    sidebar.setAttribute('aria-label', 'Primary navigation');
    sidebar.innerHTML =
      '<a class="portal-brand" href="' + config.home + '" aria-label="LankaID home">' +
        brandMarkup() +
      '</a>' +
      '<div class="portal-role"><p class="portal-role-label">' + config.eyebrow + '</p>' +
        '<p class="portal-role-name">' + escapeHtml(officerRank || config.roleName) + '</p>' +
        (officerEmail ? '<p class="portal-officer-email" title="' + escapeHtml(officerEmail) + '">' +
          '<span class="material-symbols-outlined">mail</span><span>' + escapeHtml(officerEmail) + '</span></p>' : '') +
        (officerLoginTime ? '<p class="portal-login-time"><span class="material-symbols-outlined">schedule</span>' +
          '<span>Signed in ' + escapeHtml(officerLoginTime) + '</span></p>' : '') + '</div>' +
      '<nav class="portal-nav">' + config.items.map(navItem).join('') + '</nav>' +
      '<div class="portal-sidebar-footer"><form action="/logout" method="post">' +
        '<button class="portal-logout" type="submit"><span class="material-symbols-outlined">logout</span><span class="portal-logout-label">Sign out</span></button>' +
      '</form></div>';

    const menuButton = document.createElement('button');
    menuButton.className = 'portal-menu-button';
    menuButton.type = 'button';
    menuButton.setAttribute('aria-label', 'Open navigation');
    menuButton.setAttribute('aria-expanded', 'false');
    menuButton.innerHTML = '<span class="material-symbols-outlined">menu</span>';

    const overlay = document.createElement('div');
    overlay.className = 'portal-overlay';
    overlay.setAttribute('aria-hidden', 'true');

    document.body.prepend(overlay);
    document.body.prepend(sidebar);
    document.body.prepend(menuButton);

    function closeMenu() {
      document.body.classList.remove('portal-menu-open');
      menuButton.setAttribute('aria-expanded', 'false');
      menuButton.setAttribute('aria-label', 'Open navigation');
      menuButton.querySelector('span').textContent = 'menu';
    }

    menuButton.addEventListener('click', function () {
      const open = document.body.classList.toggle('portal-menu-open');
      menuButton.setAttribute('aria-expanded', String(open));
      menuButton.setAttribute('aria-label', open ? 'Close navigation' : 'Open navigation');
      menuButton.querySelector('span').textContent = open ? 'close' : 'menu';
    });
    overlay.addEventListener('click', closeMenu);
    sidebar.addEventListener('click', function (event) {
      if (event.target.closest('a') && window.innerWidth <= 900) closeMenu();
    });
    window.addEventListener('resize', function () {
      if (window.innerWidth > 900) closeMenu();
    });
  }

  function refreshOfficerNotificationBadge() {
    fetch('/officer/notifications/unread-count', { credentials: 'same-origin' })
      .then(function (response) { return response.ok ? response.json() : null; })
      .then(function (result) {
        const count = result ? Number(result.unreadCount || 0) : 0;
        if (count < 1) return;
        const link = document.querySelector('.portal-nav-link[href="/officer/notifications"]');
        if (!link) return;
        const badge = document.createElement('span');
        badge.className = 'portal-notification-tag';
        badge.textContent = count > 99 ? '99+' : String(count);
        link.appendChild(badge);
        link.setAttribute('aria-label', 'Notifications, ' + count + ' unread');
      })
      .catch(function () { /* Navigation remains usable if the count cannot be loaded. */ });
  }

  function mountCitizenTopbar(publicAccess) {
    let header = document.querySelector('body > .citizen-topbar');
    if (!header) {
      header = document.createElement('header');
      header.className = 'citizen-topbar';
      header.innerHTML =
        '<div class="citizen-topbar-inner">' +
          '<a class="citizen-brand" href="' + config.home + '" aria-label="LankaID home">' + brandMarkup() + '</a>' +
          '<button class="citizen-menu-button" type="button" aria-label="Open navigation" aria-expanded="false" aria-controls="citizen-primary-nav">' +
            '<span class="material-symbols-outlined">menu</span><span class="citizen-menu-label">Menu</span>' +
          '</button>' +
          '<nav class="citizen-primary-nav" id="citizen-primary-nav" aria-label="Citizen navigation">' +
            '<div class="citizen-nav-main">' + config.items.filter(function (entry) { return !isToolItem(entry); }).map(citizenNavItem).join('') + '</div>' +
            (publicAccess
              ? ''
              : '<div class="citizen-nav-tools">' +
                  config.items.filter(isToolItem).map(citizenToolItem).join('') +
                  '<form class="citizen-signout-form" action="/logout" method="post"><button class="citizen-session-link" type="submit">Sign out</button></form>' +
                '</div>') +
          '</nav>' +
        '</div>';
      document.body.prepend(header);

      // The bell built here needs the dropdown script (pages using the shared fragment load it themselves).
      if (header.querySelector('[data-notify]') && !window.__lankaidNotify) {
        const notifyScript = document.createElement('script');
        notifyScript.src = '/js/citizen-notify.js';
        notifyScript.defer = true;
        document.body.appendChild(notifyScript);
      }
    }

    const menuButton = header.querySelector('.citizen-menu-button');
    const nav = header.querySelector('.citizen-primary-nav');

    function closeMenu() {
      document.body.classList.remove('citizen-menu-open');
      menuButton.setAttribute('aria-expanded', 'false');
      menuButton.setAttribute('aria-label', 'Open navigation');
      menuButton.querySelector('.material-symbols-outlined').textContent = 'menu';
    }

    menuButton.addEventListener('click', function () {
      const open = document.body.classList.toggle('citizen-menu-open');
      menuButton.setAttribute('aria-expanded', String(open));
      menuButton.setAttribute('aria-label', open ? 'Close navigation' : 'Open navigation');
      menuButton.querySelector('.material-symbols-outlined').textContent = open ? 'close' : 'menu';
    });
    nav.addEventListener('click', function (event) {
      if (event.target.closest('a') && window.innerWidth <= 1050) closeMenu();
    });
    window.addEventListener('resize', function () {
      if (window.innerWidth > 1050) closeMenu();
    });
  }

  function brandMarkup() {
    return '<span class="portal-brand-mark"><img src="/images/sri-lanka-government-emblem.png" alt="Sri Lanka Government emblem"></span>' +
      '<span class="portal-brand-copy"><span class="portal-brand-name">Lanka<span>ID</span></span>' +
      '<span class="portal-brand-subtitle">ශ්‍රී ලංකා රජය · இலங்கை அரசு · Government of Sri Lanka</span></span>';
  }

  function navItem(item) {
    const active = isActive(item);
    return '<a class="portal-nav-link' + (active ? ' active' : '') + '" href="' + item.href + '"' +
      (active ? ' aria-current="page"' : '') + '><span class="material-symbols-outlined">' +
      item.icon + '</span><span class="portal-nav-label">' + item.label + '</span></a>';
  }

  function citizenNavItem(item) {
    const active = isActive(item);
    return '<a class="citizen-nav-link' + (active ? ' active' : '') + '" href="' + item.href + '"' +
      (active ? ' aria-current="page"' : '') + '><span class="material-symbols-outlined">' +
      item.icon + '</span><span>' + item.label + '</span></a>';
  }

  // Notifications and support sit in the tools group on the right of the citizen bar.
  function isToolItem(item) {
    return item.href === '/citizen/notifications' || item.href === '/citizen/support';
  }

  function citizenToolItem(item) {
    if (item.href !== '/citizen/notifications') {
      const active = isActive(item);
      return '<a class="citizen-nav-link' + (active ? ' active' : '') + '" href="' + item.href + '" title="' + item.label + '"' +
        (active ? ' aria-current="page"' : '') + '><span class="material-symbols-outlined">' +
        item.icon + '</span><span class="citizen-nav-text">' + item.label + '</span></a>';
    }
    return '<div class="citizen-notify" data-notify data-notify-prefetch>' +
      '<button class="citizen-nav-link citizen-notify-button' + (isActive(item) ? ' active' : '') + '" type="button" title="Notifications" aria-label="Notifications" ' +
        'aria-haspopup="true" aria-expanded="false" aria-controls="citizen-notify-panel" data-notify-button>' +
        '<span class="material-symbols-outlined">notifications</span><span class="citizen-nav-text">Notifications</span></button>' +
      '<div class="citizen-notify-panel" id="citizen-notify-panel" role="region" aria-label="Recent notifications" data-notify-panel hidden>' +
        '<div class="citizen-notify-head"><strong>Notifications</strong><span class="citizen-notify-unread" data-notify-unread></span></div>' +
        '<ul class="citizen-notify-list" data-notify-list><li class="citizen-notify-empty">Loading\u2026</li></ul>' +
        '<a class="citizen-notify-all" href="/citizen/notifications">See all</a>' +
      '</div></div>';
  }

  function isActive(item) {
    const currentLocation = path + window.location.search;
    return item.matches.some(function (matcher) {
      return matcher.endsWith('*') ? currentLocation.startsWith(matcher.slice(0, -1)) : currentLocation === matcher;
    });
  }

  function detectRole(currentPath, officer) {
    if (!officer) return 'CITIZEN';
    if (document.body.dataset.officerRole) return document.body.dataset.officerRole;
    if (currentPath.startsWith('/officer/admin')) return 'SYSTEM_ADMIN';
    if (currentPath.startsWith('/officer/director')) return 'DIRECTOR';
    if (currentPath.startsWith('/officer/csr')) return 'CSR';
    if (currentPath.startsWith('/officer/passport')) return 'PASSPORT_OFFICER';
    if (currentPath.startsWith('/officer/nic')) return 'NIC_OFFICER';
    return window.sessionStorage.getItem('lankaIdOfficerRole') || 'OFFICER';
  }

  function rememberRole(value) {
    if (value !== 'OFFICER') window.sessionStorage.setItem('lankaIdOfficerRole', value);
    return value;
  }

  function citizenConfig() {
    return {
      home: '/citizen/dashboard',
      eyebrow: 'Citizen portal',
      roleName: 'Citizen services',
      items: [
        item('/citizen/dashboard', 'home', 'Home', ['/citizen/dashboard']),
        item('/citizen/applications', 'description', 'My applications', ['/citizen/applications*']),
        item('/citizen/appointments/biometric', 'fingerprint', 'Biometrics', ['/citizen/appointments*']),
        item('/citizen/notifications', 'notifications', 'Notifications', ['/citizen/notifications*']),
        item('/citizen/support', 'support_agent', 'Contact / Complaint', ['/citizen/support*'])
      ]
    };
  }

  function publicWalkInConfig() {
    return {
      home: '/',
      items: [
        item('/', 'home', 'Home', ['/']),
        item('/walk-in', 'event_available', 'Book a walk-in', ['/walk-in']),
        item('/walk-in/recover', 'qr_code_2', 'Recover booking', ['/walk-in/recover*'])
      ]
    };
  }

  function officerConfig(rawRole) {
    const currentRole = rememberRole(rawRole);
    if (currentRole === 'SYSTEM_ADMIN') {
      return {
        home: '/officer/admin', eyebrow: 'Officer portal', roleName: 'System Administrator',
        items: [
          item('/officer/admin', 'dashboard', 'Overview', ['/officer/admin']),
          item('/officer/admin?section=users', 'manage_accounts', 'Officer accounts', ['/officer/admin?section=users']),
          item('/officer/admin?section=settings', 'settings', 'System settings', ['/officer/admin?section=settings']),
          item('/officer/admin?section=centres', 'apartment', 'Service centres', ['/officer/admin?section=centres']),
          item('/officer/admin?section=maintenance', 'engineering', 'Maintenance mode', ['/officer/admin?section=maintenance']),
          item('/officer/admin?section=citizens', 'groups', 'Citizen accounts', ['/officer/admin?section=citizens']),
          item('/officer/admin?section=notifications', 'campaign', 'Notifications', ['/officer/admin?section=notifications'])
        ]
      };
    }
    if (currentRole === 'DIRECTOR') {
      return {
        home: '/officer/director', eyebrow: 'Officer portal', roleName: 'Identity Service Director',
        items: [
          item('/officer/director', 'space_dashboard', 'Executive overview', ['/officer/director']),
          item('/officer/director/referrals', 'policy', 'Officer referrals', ['/officer/director/referrals*']),
          item('/officer/director/printing', 'print', 'Printing and export', ['/officer/director/printing*']),
          item('/officer/application-tracking', 'timeline', 'Application tracking', ['/officer/application-tracking*']),
          item('/officer/notifications', 'notifications', 'Notifications', ['/officer/notifications*'])
        ]
      };
    }
    if (currentRole === 'CSR') {
      return {
        home: '/officer/csr', eyebrow: 'Officer portal', roleName: 'Citizen Service Representative',
        items: [
          item('/officer/csr', 'space_dashboard', 'Dashboard', ['/officer/csr']),
          item('/officer/csr/support', 'support_agent', 'Support queue', ['/officer/csr/support*', '/officer/csr/complaints*']),
          item('/officer/csr/walk-ins', 'event_available', 'Walk-in management', ['/officer/csr/walk-ins*']),
          item('/officer/csr/deliveries', 'inventory_2', 'Document collection', ['/officer/csr/deliveries*']),
          item('/officer/notifications', 'notifications', 'Notifications', ['/officer/notifications*'])
        ]
      };
    }

    const passport = currentRole === 'PASSPORT_OFFICER';
    const service = passport ? 'passport' : 'nic';
    const label = passport ? 'Passport Application Officer' : (currentRole === 'NIC_OFFICER' ? 'NIC Application Officer' : 'Application Officer');
    return {
      home: '/officer/' + service + '/applications', eyebrow: 'Officer portal', roleName: label,
      items: [
        item('/officer/' + service + '/applications', 'fact_check', 'Verification queue', ['/officer/' + service + '/applications']),
        item('/officer/' + service + '/applications/processed', 'task_alt', 'Reviewed applications', ['/officer/' + service + '/applications/processed']),
        item('/officer/application-tracking', 'timeline', 'Application tracking', ['/officer/application-tracking*']),
        item('/officer/' + service + '/appointments', 'event_available', 'Appointments', ['/officer/' + service + '/appointments*']),
        item('/officer/' + service + '/biometrics', 'fingerprint', 'Biometric collection', ['/officer/' + service + '/biometrics*']),
        item('/officer/notifications', 'notifications', 'Notifications', ['/officer/notifications*'])
      ]
    };
  }

  function item(href, icon, label, matches) {
    return { href: href, icon: icon, label: label, matches: matches };
  }

  function escapeHtml(value) {
    return String(value)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }
}());
