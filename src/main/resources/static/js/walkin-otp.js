// Use elapsed time so background tabs do not leave the countdown running too long.
document.querySelectorAll('[data-otp-retry]').forEach((button) => {
  const deadline = Date.now() + Number(button.dataset.retrySeconds || 0) * 1000;
  const feedback = document.getElementById('otp-feedback');
  const originalMessage = feedback?.textContent || '';
  const update = () => {
    const seconds = Math.max(0, Math.ceil((deadline - Date.now()) / 1000));
    button.disabled = seconds > 0;
    button.textContent = seconds > 0
      ? `Try again in ${seconds} ${seconds === 1 ? 'second' : 'seconds'}`
      : button.dataset.readyLabel;
    if (feedback) {
      feedback.textContent = originalMessage
        .replace(/Please wait \d+ seconds before requesting another code\./,
          seconds > 0 ? `Please wait ${seconds} ${seconds === 1 ? 'second' : 'seconds'} before requesting another code.`
            : 'You can request another code now.')
        .replace(/Please try again in \d+ seconds\./,
          seconds > 0 ? `Please try again in ${seconds} ${seconds === 1 ? 'second' : 'seconds'}.`
            : 'You can try again now.');
    }
    return seconds;
  };
  update();
  const timer = setInterval(() => {
    if (update() === 0) clearInterval(timer);
  }, 250);
  button.form.addEventListener('submit', (event) => {
    if (Date.now() < deadline) {
      event.preventDefault();
      return;
    }
    button.disabled = true;
    button.textContent = 'Requesting code…';
  });
  window.addEventListener('pageshow', update);
});
