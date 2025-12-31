# Shade Email Service

Standalone Node container that renders our React Email templates and sends via Resend.

## Run locally
```sh
cd email
npm install
npm run dev   # or: npm start (production)
```

Env vars:
- `RESEND_API_KEY` (required)
- `RESEND_FROM` (default: `Shade <noreply@shade.com>`)
- `EMAIL_SHARED_SECRET` (optional, if set requests must include `x-email-secret`)
- `ALLOWED_TEMPLATES` (optional comma-list of allowed ids/keys)

## Endpoint
`POST /send-email`
```json
{
  "templateId": "attendee-welcome",
  "to": ["user@example.com"],
  "subject": "optional override",
  "variables": { "...template props..." },
  "cc": [],
  "bcc": [],
  "replyTo": "team@shade.com"
}
```
`templateId` can be a key (`ATTENDEE_WELCOME`) or id (`attendee-welcome`). Subjects default from the template unless overridden.

Health: `GET /health`
