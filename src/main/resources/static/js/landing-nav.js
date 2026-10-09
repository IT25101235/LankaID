/*
 * Landing page navigation: a phone menu and a highlight for the section being read.
 */
(function () {
  'use strict';

  var header = document.querySelector('.gov-site-header');
  if (!header) return;

  // ---- Phone menu ----
  var button = header.querySelector('[data-landing-menu-button]');
  var menu = document.getElementById('landingMenu');
  if (button && menu) {
    var icon = button.querySelector('.material-symbols-outlined');

    function setMenu(open) {
      menu.hidden = !open;
      button.setAttribute('aria-expanded', String(open));
      button.setAttribute('aria-label', open ? 'Close menu' : 'Open menu');
      if (icon) icon.textContent = open ? 'close' : 'menu';
    }

    button.addEventListener('click', function () { setMenu(menu.hidden); });
    menu.addEventListener('click', function (event) {
      if (event.target.closest('a')) setMenu(false);
    });
    document.addEventListener('keydown', function (event) {
      if (event.key === 'Escape' && !menu.hidden) { setMenu(false); button.focus(); }
    });
    window.addEventListener('resize', function () {
      if (window.innerWidth >= 768) setMenu(false);
    });
  }

  // ---- Highlight the section in view ----
  var links = Array.prototype.slice.call(header.querySelectorAll('a[data-nav-section]'));
  if (!links.length) return;

  var sections = [];
  links.forEach(function (link) {
    var id = link.getAttribute('data-nav-section');
    var target = id === 'top' ? document.body : document.getElementById(id);
    if (target && !sections.some(function (entry) { return entry.id === id; })) {
      sections.push({ id: id, element: target });
    }
  });

  // The identity card illustration has no link of its own; it belongs under Services.
  var aliases = { 'nic-card': 'services' };
  Object.keys(aliases).forEach(function (id) {
    var element = document.getElementById(id);
    if (element) sections.push({ id: id, element: element });
  });

  function highlight() {
    var offset = header.offsetHeight + 40;
    // The section whose top has most recently passed the header is the one being read,
    // whatever order the sections are in on the page.
    var current = 'top';
    var closest = -Infinity;
    sections.forEach(function (entry) {
      if (entry.id === 'top') return;
      var top = entry.element.getBoundingClientRect().top;
      if (top <= offset && top > closest) { closest = top; current = entry.id; }
    });
    if (aliases[current]) current = aliases[current];
    links.forEach(function (link) {
      var active = link.getAttribute('data-nav-section') === current;
      link.classList.toggle('is-current', active);
      if (active) link.setAttribute('aria-current', 'location'); else link.removeAttribute('aria-current');
    });
  }

  var timer = null;
  window.addEventListener('scroll', function () {
    if (timer) return;
    timer = window.setTimeout(function () { timer = null; highlight(); }, 80);
  }, { passive: true });
  window.addEventListener('resize', highlight);
  highlight();
})();
