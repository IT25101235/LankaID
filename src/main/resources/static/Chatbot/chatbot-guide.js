/* Public guide API adapter. No credentials, identity, or conversation history are sent. */
(function () {
  'use strict';

  async function reply(message) {
    const controller = new AbortController();
    const timeout = window.setTimeout(() => controller.abort(), 18000);
    try {
      const response = await fetch('/api/chatbot/messages', {
        method: 'POST',
        credentials: 'omit',
        cache: 'no-store',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message }),
        signal: controller.signal
      });
      const answer = await response.json();
      if (typeof answer.text !== 'string' || !Array.isArray(answer.links)) {
        throw new Error('Invalid guide response');
      }
      // Validation, scope refusals, and rate limits are safe backend messages.
      if (!response.ok && ![400, 403, 429].includes(response.status)) {
        throw new Error('Guide unavailable');
      }
      return answer;
    } catch (error) {
      return {
        text: 'Guidance is temporarily unavailable. Please try again shortly, explore the services on this page, or use citizen support. This assistant cannot access personal records or perform actions.',
        links: [{ label: 'Explore services', href: '#services' }, { label: 'Citizen support', href: '/citizen/support' }],
        source: 'GUIDE'
      };
    } finally {
      window.clearTimeout(timeout);
    }
  }

  window.LankaIDChatbotGuide = Object.freeze({ reply });
})();
