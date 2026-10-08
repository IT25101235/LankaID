(() => {
  const statusStages = {
    DRAFT: { step: 0, icon: 'edit_document', title: 'Application in progress', message: 'Complete the form and upload the required documents when you are ready.' },
    SUBMITTED: { step: 1, icon: 'mark_email_read', title: 'Application received', message: 'Your application is safely submitted and waiting for an officer review.' },
    RESUBMITTED: { step: 1, icon: 'published_with_changes', title: 'Corrections resubmitted', message: 'Your updated information has been returned to the verification team.' },
    UNDER_REVIEW: { step: 1, icon: 'manage_search', title: 'Verification in progress', message: 'An officer is checking your details and supporting documents.' },
    CORRECTION_REQUIRED: { step: 1, icon: 'rate_review', title: 'Correction required', message: 'Update the requested information so your application can continue.' },
    REJECTED: { step: 1, icon: 'cancel', title: 'Application not approved', message: 'Review the decision notice or contact support if you need assistance.' },
    CANCELLED: { step: 0, icon: 'cancel', title: 'Application cancelled', message: 'This application is no longer being processed.' },
    APPROVED: { step: 3, icon: 'payments', title: 'Payment and booking required', message: 'Your application is approved. Complete payment and book your biometric appointment.' },
    PAYMENT_PENDING: { step: 3, icon: 'payments', title: 'Payment and booking required', message: 'Complete payment to reserve your biometric appointment.' },
    APPOINTMENT_BOOKED: { step: 4, icon: 'fingerprint', title: 'Biometric appointment confirmed', message: 'Your appointment is booked. Attend the visit with your appointment pass.' },
    BIOMETRIC_COLLECTED: { step: 5, icon: 'print', title: 'Ready for printing', message: 'Your biometrics are complete and your document is waiting to be produced.' },
    PROCESSING: { step: 5, icon: 'print', title: 'Document in production', message: 'Your document is being prepared by the printing team.' },
    ISSUED: { step: 6, icon: 'local_shipping', title: 'Document printed', message: 'Your document has been produced and is being prepared for delivery or collection.' }
  };

  const deliveryStages = {
    DISPATCHED: { step: 6, icon: 'local_shipping', title: 'Document dispatched', message: 'Your document is on its way. Follow the delivery reference for updates.' },
    READY_FOR_COLLECTION: { step: 6, icon: 'storefront', title: 'Ready for collection', message: 'Your document is ready at the selected service centre.' },
    DELIVERED: { step: 6, icon: 'task_alt', title: 'Delivered successfully', message: 'Your application journey is complete and the document has been delivered.', complete: true },
    COLLECTED: { step: 6, icon: 'task_alt', title: 'Collected successfully', message: 'Your application journey is complete and the document has been handed over.', complete: true }
  };

  document.querySelectorAll('.application-journey').forEach((journey) => {
    const status = journey.dataset.status || 'DRAFT';
    const deliveryStatus = journey.dataset.deliveryStatus || '';
    const state = deliveryStages[deliveryStatus] || statusStages[status] || statusStages.DRAFT;
    const steps = Array.from(journey.querySelectorAll('.journey-step'));
    const currentStep = Math.min(state.step, steps.length - 1);
    const progress = state.complete ? 100 : (currentStep * 100 / Math.max(steps.length - 1, 1));

    steps.forEach((step, index) => {
      step.classList.toggle('is-complete', Boolean(state.complete) || index < currentStep);
      step.classList.toggle('is-current', !state.complete && index === currentStep);
    });

    journey.classList.toggle('is-correction', status === 'CORRECTION_REQUIRED');
    journey.classList.toggle('is-rejected', status === 'REJECTED' || status === 'CANCELLED');
    journey.classList.toggle('is-complete', Boolean(state.complete));
    journey.style.setProperty('--journey-progress', `${progress}%`);
    journey.setAttribute('aria-valuenow', String(state.complete ? steps.length : currentStep + 1));
    journey.setAttribute('aria-valuetext', `${state.title}. ${state.message}`);

    const icon = journey.querySelector('.journey-status-icon');
    const title = journey.querySelector('.journey-status-title');
    const message = journey.querySelector('.journey-status-message');
    const count = journey.querySelector('.journey-step-current');
    if (icon) icon.textContent = state.icon;
    if (title) title.textContent = state.title;
    if (message) message.textContent = state.message;
    if (count) count.textContent = String(state.complete ? steps.length : currentStep + 1);

    requestAnimationFrame(() => journey.classList.add('is-ready'));
  });
})();
