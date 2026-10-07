# Public landing-page chatbot

`POST /api/chatbot/messages` accepts `{"message":"How do I apply for a NIC?"}`
and returns `{"text":"...","links":[...],"source":"GUIDE|MODEL|BOUNDARY"}`.
Only index.html loads the frontend. No login is required.

## Privacy and scope

- No repository, account service, uploaded document, action endpoint, or SQL access.
- No conversation persistence, IP storage, or application/booking/account lookup.
- Frontend requests omit cookies; both global page-model advices skip this endpoint
  before retrieving identity or account counters, even when a client sends a session.
- Raw visitor messages are used transiently to select a general topic. They are never
  sent to the LLM, logged by this module, or stored. Detected identifiers, credentials,
  personal lookups, staff/admin requests, and instruction overrides get local replies.
- The LLM receives only the topic's canonical question and approved public guide.
  Consequently, fine-grained questions currently get an explanation of the selected
  topic, rather than unrestricted free-form answers. Expand reviewed topics if needed.
- Models have no tools. Links come from the approved guide, never model output.
- Scope checks and prompts cannot guarantee perfect model wording. The hard privacy
  boundary is the absence of personal data and operation capabilities. Review model
  answers for accuracy before enabling a provider for public use.
- Replies use no-store caching. JSON-only requests and same-origin browser checks
  prevent ordinary cross-site browser use. Global limits default to 30 requests per
  minute and four concurrent requests; these are per application process, not per user.

## Reviewed content

Edit `src/main/resources/Chatbot/citizen-guide.json` and restart the application.
Keep this file restricted to public citizen-service information. Update it when
workflow requirements or policies change. Do not paste private records into it.

## Optional LLM configuration

By default LLM calls are disabled, and the backend returns the approved guides.
The included adapter supports a bearer-authenticated chat-completions JSON API:
request fields `model`, `messages`, `max_tokens`, `stream`; response text at
`choices[0].message.content`. Providers with a different API need a new LlmClient
adapter. GitHub Models is retired; no GitHub endpoint is configured.

Set these environment variables in the terminal or IDE that starts Spring Boot:

```powershell
$env:LANKAID_CHATBOT_LLM_ENABLED="true"
$env:LANKAID_CHATBOT_API_URL="https://YOUR-PROVIDER/FULL-CHAT-COMPLETIONS-ENDPOINT"
$env:LANKAID_CHATBOT_MODEL="YOUR-MODEL-OR-DEPLOYMENT-ID"
$env:LANKAID_CHATBOT_API_KEY="YOUR-KEY"
```

Store the actual key locally, never in source control or frontend JavaScript. Use
an HTTPS endpoint. HTTP is accepted only for localhost adapters/tests. Missing or
invalid configuration, provider errors, oversized/empty replies, and timeouts fall
back to reviewed guidance. Redirects are not followed and provider errors are not
exposed. Native Gemini APIs, Azure endpoints requiring `api-key` authentication,
or model-specific request fields require an adapter adjustment after provider selection.

## Verification

```powershell
.\mvnw.cmd -o "-Dtest=Chatbot*Tests,CitizenNavigationAdviceTests" test
```

Tests use mocks and a local HTTP fixture; they do not boot the application, connect
to SQL Server, send real LLM requests, or change citizen records. Restart Spring Boot
and test the landing widget to use the newly compiled endpoint.
