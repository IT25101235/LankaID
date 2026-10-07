(function () {
  'use strict';

  const root = document.querySelector('[data-chatbot]');
  if (!root || !document.body.classList.contains('gov-landing-page')) return;

  const launcher = root.querySelector('[data-chatbot-launcher]');
  const panel = root.querySelector('[role="dialog"]');
  const log = root.querySelector('[data-chatbot-log]');
  const welcome = root.querySelector('[data-chatbot-welcome]').cloneNode(true);
  const form = root.querySelector('[data-chatbot-form]');
  const input = root.querySelector('[data-chatbot-input]');
  const send = root.querySelector('[data-chatbot-send]');
  const count = root.querySelector('[data-chatbot-count]');
  const prompts = root.querySelectorAll('[data-chatbot-prompt]');
  let busy = false;
  let conversationVersion = 0;
  let returnFocus = launcher;

  function openChat(event) {
    returnFocus = event.currentTarget;
    panel.hidden = false;
    launcher.setAttribute('aria-expanded', 'true');
    updateInput();
    input.focus();
  }

  function closeChat() {
    panel.hidden = true;
    launcher.setAttribute('aria-expanded', 'false');
    returnFocus.focus();
  }

  function updateInput() {
    send.disabled = busy || input.value.trim().length === 0;
    count.textContent = input.value.length + '/500';
    input.style.height = 'auto';
    input.style.height = Math.min(input.scrollHeight, 90) + 'px';
  }

  function setBusy(value) {
    busy = value;
    prompts.forEach((button) => { button.disabled = value; });
    updateInput();
  }

  function scrollConversation() {
    log.scrollTop = log.scrollHeight;
  }

  function addMessage(role, text, links) {
    const message = document.createElement('div');
    message.className = 'lankaid-chatbot-message lankaid-chatbot-message-' + role;
    const label = document.createElement('span');
    label.className = 'lankaid-chatbot-message-label';
    label.textContent = role === 'user' ? 'You' : 'LankaID · Quick guide';
    const bubble = document.createElement('div');
    bubble.className = 'lankaid-chatbot-message-bubble';
    const copy = document.createElement('p');
    // Message text is always treated as text, including future provider replies.
    copy.textContent = text;
    bubble.appendChild(copy);
    const safeLinks = (links || []).filter((link) => {
      if (!link || typeof link.href !== 'string' || typeof link.label !== 'string') return false;
      if (/^#[a-z][a-z0-9-]*$/i.test(link.href)) return true;
      if (!/^\/(?!\/)/.test(link.href)) return false;
      try {
        return new URL(link.href, window.location.href).origin === window.location.origin;
      } catch (error) {
        return false;
      }
    });
    if (safeLinks.length) {
      const actions = document.createElement('div');
      actions.className = 'lankaid-chatbot-message-links';
      safeLinks.forEach((link) => {
        const action = document.createElement('a');
        action.href = link.href;
        action.textContent = link.label + ' ↗';
        action.addEventListener('click', () => {
          if (link.href.startsWith('#')) closeChat();
        });
        actions.appendChild(action);
      });
      bubble.appendChild(actions);
    }
    message.append(label, bubble);
    log.appendChild(message);
    scrollConversation();
  }

  function addTyping() {
    const typing = document.createElement('div');
    typing.className = 'lankaid-chatbot-typing';
    for (let dot = 0; dot < 3; dot++) {
      const indicator = document.createElement('span');
      indicator.setAttribute('aria-hidden', 'true');
      typing.appendChild(indicator);
    }
    const status = document.createElement('span');
    status.className = 'lankaid-chatbot-sr-only';
    status.textContent = 'Preparing guidance…';
    typing.appendChild(status);
    log.appendChild(typing);
    scrollConversation();
    return typing;
  }

  async function sendMessage(message) {
    const text = message.trim();
    if (!text || busy || text.length > 500) return;
    const version = conversationVersion;
    addMessage('user', text);
    input.value = '';
    setBusy(true);
    const typing = addTyping();
    try {
      const [answer] = await Promise.all([
        window.LankaIDChatbotGuide.reply(text),
        new Promise((resolve) => window.setTimeout(resolve, 400))
      ]);
      if (version !== conversationVersion) return;
      typing.remove();
      addMessage('assistant', answer.text, answer.links);
    } catch (error) {
      if (version !== conversationVersion) return;
      typing.remove();
      addMessage('assistant', 'Guidance could not be loaded. Please try again or open citizen support.',
        [{ label: 'Citizen support', href: '/citizen/support' }]);
    } finally {
      if (version === conversationVersion) setBusy(false);
    }
  }

  launcher.addEventListener('click', openChat);
  document.querySelectorAll('[data-chatbot-open]').forEach((button) => button.addEventListener('click', openChat));
  root.querySelector('[data-chatbot-close]').addEventListener('click', closeChat);
  root.querySelector('[data-chatbot-reset]').addEventListener('click', () => {
    conversationVersion++;
    log.replaceChildren(welcome.cloneNode(true));
    input.value = '';
    setBusy(false);
    log.scrollTop = 0;
    input.focus();
  });
  panel.addEventListener('keydown', (event) => {
    if (event.key === 'Escape') {
      event.preventDefault();
      closeChat();
    }
  });
  input.addEventListener('input', updateInput);
  input.addEventListener('keydown', (event) => {
    if (event.key === 'Enter' && !event.shiftKey && !event.isComposing) {
      event.preventDefault();
      sendMessage(input.value);
    }
  });
  form.addEventListener('submit', (event) => {
    event.preventDefault();
    sendMessage(input.value);
  });
  prompts.forEach((button) => button.addEventListener('click', () => {
    sendMessage(button.dataset.chatbotPrompt);
  }));
  root.hidden = false;
  updateInput();
})();
