(() => {
  "use strict";

  const form = document.getElementById("chatForm");
  const input = document.getElementById("messageInput");
  const sendButton = document.getElementById("sendButton");
  const messages = document.getElementById("messages");
  const welcomeBlock = document.getElementById("welcomeBlock");
  const conversation = document.getElementById("conversation");
  const template = document.getElementById("messageTemplate");
  const characterCount = document.getElementById("characterCount");
  const sidebar = document.getElementById("sidebar");
  const sidebarOverlay = document.getElementById("sidebarOverlay");
  let replyTimer;

  const replies = [
    {
      keywords: ["nic", "identity card", "national identity", "id card"],
      html: `<p>To apply for a new National Identity Card, you will generally need to:</p>
        <ol>
          <li>Complete the online NIC application.</li>
          <li>Upload the requested identity and citizenship documents.</li>
          <li>Book an appointment if biometric verification is required.</li>
          <li>Keep your application reference to track progress.</li>
        </ol>
        <p>You can begin from <a href="/citizen/applications/nic/new">New NIC Application</a>. Requirements may vary, so confirm the checklist shown on the application page.</p>`
    },
    {
      keywords: ["passport", "travel document"],
      html: `<p>For a passport application, prepare your NIC or accepted identity proof, birth certificate details, a compliant photograph, and any supporting documents requested for your application type.</p>
        <p>Use <a href="/citizen/applications/passport/new">New Passport Application</a> for a first passport. Renewal and replacement options are also available in the citizen portal.</p>`
    },
    {
      keywords: ["track", "status", "progress", "reference"],
      html: `<p>Open <a href="/citizen/applications">My Applications</a> to view the latest demonstrated status for your NIC or passport request. Your application reference helps identify the correct record.</p>
        <p>This demo assistant cannot access personal application records or verify a live status.</p>`
    },
    {
      keywords: ["appointment", "biometric", "fingerprint", "book"],
      html: `<p>You can choose an available date and service centre from the <a href="/citizen/appointments/biometric">Biometric Appointment</a> page after your application reaches the required stage.</p>
        <p>For in-person application help, use the walk-in service booking option in the citizen portal.</p>`
    },
    {
      keywords: ["lost", "damaged", "replace", "replacement"],
      html: `<p>If an NIC or passport is lost or damaged, select the relevant replacement service in the citizen portal. You may be asked for a police report or a declaration, along with proof of identity.</p>
        <p>Do not share document numbers here. An authorised officer can confirm the exact evidence required for your case.</p>`
    },
    {
      keywords: ["complaint", "support", "help", "contact", "officer"],
      html: `<p>For help from a citizen service officer, open <a href="/citizen/support/new">Citizen Support</a> and submit an inquiry. You may also call the national government information centre on <strong>1919</strong>.</p>
        <p>Please avoid including passwords, PINs, payment details, or full document numbers.</p>`
    },
    {
      keywords: ["hello", "hi", "hey", "ayubowan"],
      html: `<p>Ayubowan! I can guide you through NIC and passport applications, appointments, application tracking, replacements, and citizen support.</p>
        <p>What would you like help with today?</p>`
    }
  ];

  const fallbackReply = `<p>I do not have a sample answer for that question yet. This interface is currently a demonstration and is not connected to an AI service.</p>
    <p>Try asking about <strong>NIC applications</strong>, <strong>passports</strong>, <strong>application tracking</strong>, or <strong>appointments</strong>. For official assistance, call <strong>1919</strong>.</p>`;

  function setSidebar(open) {
    sidebar.classList.toggle("is-open", open);
    sidebarOverlay.classList.toggle("is-open", open);
    sidebarOverlay.setAttribute("aria-hidden", String(!open));
    document.body.style.overflow = open ? "hidden" : "";
  }

  function updateComposer() {
    const length = input.value.length;
    sendButton.disabled = input.value.trim().length === 0;
    characterCount.textContent = `${length} / 500`;
    input.style.height = "auto";
    input.style.height = `${Math.min(input.scrollHeight, 130)}px`;
  }

  function scrollToLatest() {
    requestAnimationFrame(() => {
      conversation.scrollTo({ top: conversation.scrollHeight, behavior: "smooth" });
    });
  }

  function createMessage(role, content, isHtml = false) {
    const fragment = template.content.cloneNode(true);
    const row = fragment.querySelector(".message-row");
    const avatar = fragment.querySelector(".message-avatar");
    const label = fragment.querySelector(".message-label");
    const bubble = fragment.querySelector(".message-bubble");
    const actions = fragment.querySelector(".message-actions");

    row.classList.add(role);
    label.textContent = role === "assistant" ? "LankaID Assistant" : "You";
    avatar.innerHTML = `<span class="material-symbols-outlined">${role === "assistant" ? "support_agent" : "person"}</span>`;

    if (isHtml) {
      bubble.innerHTML = content;
    } else {
      const paragraph = document.createElement("p");
      paragraph.textContent = content;
      bubble.appendChild(paragraph);
    }

    if (role === "assistant") {
      const copyButton = document.createElement("button");
      copyButton.type = "button";
      copyButton.className = "copy-button";
      copyButton.innerHTML = `<span class="material-symbols-outlined">content_copy</span><span>Copy answer</span>`;
      copyButton.addEventListener("click", async () => {
        try {
          await navigator.clipboard.writeText(bubble.innerText);
          copyButton.querySelector("span:last-child").textContent = "Copied";
          setTimeout(() => { copyButton.querySelector("span:last-child").textContent = "Copy answer"; }, 1600);
        } catch (_error) {
          copyButton.querySelector("span:last-child").textContent = "Copy unavailable";
        }
      });
      actions.appendChild(copyButton);
    }

    messages.appendChild(fragment);
    scrollToLatest();
  }

  function showTyping() {
    const fragment = template.content.cloneNode(true);
    const row = fragment.querySelector(".message-row");
    row.id = "typingIndicator";
    row.classList.add("assistant");
    fragment.querySelector(".message-avatar").innerHTML = `<span class="material-symbols-outlined">support_agent</span>`;
    fragment.querySelector(".message-label").textContent = "LankaID Assistant";
    fragment.querySelector(".message-bubble").classList.add("typing-bubble");
    fragment.querySelector(".message-bubble").innerHTML = "<span></span><span></span><span></span>";
    messages.appendChild(fragment);
    scrollToLatest();
  }

  function chooseReply(text) {
    const normalised = text.toLowerCase();
    const match = replies.find((reply) => reply.keywords.some((keyword) => normalised.includes(keyword)));
    return match ? match.html : fallbackReply;
  }

  function sendMessage(text) {
    const cleanText = text.trim();
    if (!cleanText) return;

    clearTimeout(replyTimer);
    document.getElementById("typingIndicator")?.remove();
    welcomeBlock.classList.add("is-hidden");
    createMessage("user", cleanText);
    input.value = "";
    updateComposer();
    showTyping();

    replyTimer = setTimeout(() => {
      document.getElementById("typingIndicator")?.remove();
      createMessage("assistant", chooseReply(cleanText), true);
    }, 720);
  }

  function resetChat() {
    clearTimeout(replyTimer);
    messages.replaceChildren();
    welcomeBlock.classList.remove("is-hidden");
    input.value = "";
    updateComposer();
    conversation.scrollTo({ top: 0, behavior: "smooth" });
    input.focus();
    setSidebar(false);
  }

  form.addEventListener("submit", (event) => {
    event.preventDefault();
    sendMessage(input.value);
  });

  input.addEventListener("input", updateComposer);
  input.addEventListener("keydown", (event) => {
    if (event.key === "Enter" && !event.shiftKey && !event.isComposing) {
      event.preventDefault();
      form.requestSubmit();
    }
  });

  document.querySelectorAll("[data-prompt]").forEach((button) => {
    button.addEventListener("click", () => {
      sendMessage(button.dataset.prompt);
      setSidebar(false);
    });
  });

  document.getElementById("newChatButton").addEventListener("click", resetChat);
  document.getElementById("clearChatButton").addEventListener("click", resetChat);
  document.getElementById("openSidebar").addEventListener("click", () => setSidebar(true));
  document.getElementById("closeSidebar").addEventListener("click", () => setSidebar(false));
  sidebarOverlay.addEventListener("click", () => setSidebar(false));
  document.getElementById("dismissNotice").addEventListener("click", (event) => {
    event.currentTarget.closest(".service-notice").classList.add("is-hidden");
  });

  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape") setSidebar(false);
  });

  updateComposer();
})();
