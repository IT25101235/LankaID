/*
 * Landing page: example National Identity Card.
 * Flips the card (click, Enter, Space or the button), keeps the aria-label in step with
 * the side shown, and draws the data-matrix style barcode on the back. Illustration only.
 */
(function () {
  'use strict';

  var card = document.querySelector('[data-nic-card]');
  if (!card) return;

  var button = document.querySelector('[data-nic-flip-button]');
  var buttonLabel = document.querySelector('[data-nic-flip-label]');

  function setFlipped(flipped) {
    card.classList.toggle('is-flipped', flipped);
    card.setAttribute('aria-pressed', flipped ? 'true' : 'false');
    card.setAttribute('aria-label', flipped
      ? 'National Identity Card illustration with masked details. Back of card shown. Press to show the front.'
      : 'National Identity Card illustration with masked details. Front of card shown. Press to show the back.');
    if (buttonLabel) buttonLabel.textContent = flipped ? 'Show front of card' : 'Show back of card';
  }

  function toggle() { setFlipped(!card.classList.contains('is-flipped')); }

  card.addEventListener('click', toggle);
  card.addEventListener('keydown', function (event) {
    if (event.key === 'Enter' || event.key === ' ' || event.key === 'Spacebar') {
      event.preventDefault();
      toggle();
    }
  });
  if (button) button.addEventListener('click', toggle);

  // Small seeded generator so the barcode looks the same on every visit.
  function seeded(seed) {
    var s = seed >>> 0;
    return function () {
      s = (s + 0x6D2B79F5) >>> 0;
      var t = s;
      t = Math.imul(t ^ (t >>> 15), t | 1);
      t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
      return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
    };
  }

  var SVG_NS = 'http://www.w3.org/2000/svg';

  function svgFrom(viewW, viewH, pathData, className) {
    var svg = document.createElementNS(SVG_NS, 'svg');
    svg.setAttribute('viewBox', '0 0 ' + viewW + ' ' + viewH);
    svg.setAttribute('preserveAspectRatio', 'none');
    svg.setAttribute('shape-rendering', 'crispEdges');
    svg.setAttribute('focusable', 'false');
    svg.setAttribute('class', className);
    var path = document.createElementNS(SVG_NS, 'path');
    path.setAttribute('d', pathData);
    path.setAttribute('fill', '#111');
    svg.appendChild(path);
    return svg;
  }

  // Linear barcode: a few thick and thin bars with small gaps.
  function linearBars(rand) {
    var widths = [5, 1, 1, 2, 1, 1, 3, 1, 2, 1, 4, 1, 1, 2, 1, 3];
    var x = 0, d = '';
    widths.forEach(function (w) {
      d += 'M' + x + ' 0h' + w + 'v100h-' + w + 'z';
      x += w + 1 + Math.floor(rand() * 2);
    });
    return svgFrom(x, 100, d, 'nic-bars');
  }

  // Data-matrix style block: solid left and bottom edges, alternating top and right, random inside.
  function matrix(rand, cols, rows) {
    var d = '';
    for (var y = 0; y < rows; y++) {
      for (var x = 0; x < cols; x++) {
        var on;
        if (x === 0 || y === rows - 1) on = true;
        else if (y === 0) on = x % 2 === 0;
        else if (x === cols - 1) on = y % 2 === 1;
        else on = rand() > 0.46;
        if (on) d += 'M' + x + ' ' + y + 'h1v1h-1z';
      }
    }
    return svgFrom(cols, rows, d, 'nic-matrix');
  }

  var holder = document.querySelector('[data-nic-barcode]');
  if (holder) {
    var rand = seeded(1968);
    holder.appendChild(linearBars(rand));
    for (var i = 0; i < 6; i++) holder.appendChild(matrix(rand, 16, 30));
  }
})();
