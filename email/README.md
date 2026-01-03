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




curl -X POST http://localhost:3000/send-email \
  -H "Content-Type: application/json" \
  -H "x-email-secret: ***REMOVED***" \
  -d '{
    "templateId": "attendee-welcome",
    "from": "events@noreply.mayokun.dev",
    "to": ["user@example.com"],
    "subject": "You’re on the list",
    "variables": {
      "attendeeName": "Jordan",
      "eventName": "Tech Meetup",
      "eventDate": "Aug 12, 7:00 PM",
      "venue": "Pier 27, San Francisco",
      "actionUrl": "https://shade.com/events/tech-meetup"
    }
  }'