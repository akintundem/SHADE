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
- `RESEND_FROM` (required)
- `ALLOWED_TEMPLATES` (optional comma-list of allowed ids/keys)
- `RABBITMQ_URL` (required)
- `RABBITMQ_EXCHANGE` (required)
- `RABBITMQ_EMAIL_QUEUE` (required)
- `RABBITMQ_EMAIL_ROUTING_KEY` (required)
- `RABBITMQ_PREFETCH` (required)
- `RABBITMQ_RECONNECT_MS` (required)
- `RABBITMQ_REQUEUE_ON_ERROR` (required)

## RabbitMQ
The service consumes email jobs from RabbitMQ. Payloads use this shape:
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
