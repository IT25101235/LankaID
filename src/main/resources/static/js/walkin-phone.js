// The server validates again; these checks simply make the form easier to use.
const walkInPhone = document.getElementById('visitorPhone');
if (walkInPhone) {
  // Restore submitted local/international numbers in the nine-digit input after an error.
  const restored = walkInPhone.value.trim().replace(/[ -]/g, '');
  if (/^\+947[0-9]{8}$/.test(restored)) walkInPhone.value = restored.slice(3);
  else if (/^07[0-9]{8}$/.test(restored)) walkInPhone.value = restored.slice(1);
  walkInPhone.addEventListener('input', () => {
    walkInPhone.value = walkInPhone.value.replace(/[^0-9]/g, '');
    walkInPhone.setCustomValidity(
      walkInPhone.value && !/^7[0-9]{8}$/.test(walkInPhone.value)
        ? 'Enter exactly 9 digits starting with 7, without the leading zero.' : ''
    );
  });
  walkInPhone.addEventListener('paste', (event) => {
    const pasted = event.clipboardData.getData('text').trim().replace(/[ -]/g, '');
    let number = pasted;
    if (/^\+947[0-9]{8}$/.test(number)) number = number.slice(3);
    else if (/^07[0-9]{8}$/.test(number)) number = number.slice(1);
    if (/^7[0-9]{8}$/.test(number)) {
      event.preventDefault();
      walkInPhone.value = number;
      walkInPhone.dispatchEvent(new Event('input'));
    }
  });
}
