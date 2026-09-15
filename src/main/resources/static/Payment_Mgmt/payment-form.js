(() => {
  const form = document.querySelector('form[data-payment-form]');
  if (!form) return;
  const number = form.elements.namedItem('cardNumber');
  const expiry = form.elements.namedItem('expiry');
  const cvv = form.elements.namedItem('cvv');

  number.addEventListener('input', () => {
    const digits = number.value.replace(/[^0-9]/g, '').slice(0, 16);
    number.value = digits.match(/.{1,4}/g)?.join(' ') || '';
    number.setCustomValidity(digits.length === 16 ? '' : 'Enter exactly 16 card digits.');
  });
  expiry.addEventListener('input', () => {
    const digits = expiry.value.replace(/[^0-9]/g, '').slice(0, 4);
    expiry.value = digits.length > 2 ? digits.slice(0, 2) + '/' + digits.slice(2) : digits;
    let message = '';
    if (!/^(0[1-9]|1[0-2])\/[0-9]{2}$/.test(expiry.value)) {
      message = 'Enter a valid month and year as MM/YY, for example 09/29.';
    } else {
      const now = new Date();
      const month = Number(digits.slice(0, 2));
      const year = 2000 + Number(digits.slice(2));
      if (year < now.getFullYear() || (year === now.getFullYear() && month < now.getMonth() + 1)) {
        message = 'This card has expired. Enter the expiry of a current card.';
      }
    }
    expiry.setCustomValidity(message);
  });
  cvv.addEventListener('input', () => {
    cvv.value = cvv.value.replace(/[^0-9]/g, '').slice(0, 3);
    cvv.setCustomValidity(cvv.value.length === 3 ? '' : 'Enter exactly 3 digits for the CVV.');
  });
  // Revalidate restored values, including an expired date returned by the server.
  [number, expiry, cvv].forEach(input => {
    if (input.value) input.dispatchEvent(new Event('input'));
  });
})();
