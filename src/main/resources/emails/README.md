# React Email templates for SHDE

Lightweight React email components that mirror the backend templates. Built with `@react-email/components`, rendered via `@react-email/render`, and ready to send through `resend`.

## Structure

- `layouts/BaseLayout.tsx` — shared chrome, typography, footer.
- `templates/` — one component per email:
  - `EmailVerification`, `PasswordReset`
  - `EventAnnouncement`, `EventCancelled`, `EventReminder`
  - `AttendeeWelcome`, `TicketConfirmation`
  - `CollaboratorInvite`, `CollaboratorWelcome`
- `templates/index.ts` — exports `EMAIL_TEMPLATES` map, `renderEmailTemplate` helper.
- `sendWithResend.ts` — small helper to send by key with Resend.

## Quick start

```bash
npm install @react-email/components @react-email/render resend
```

Render locally:

```ts
import { renderEmailTemplate } from './templates'

const { subject, html } = renderEmailTemplate('EVENT_CANCELLATION', {
  eventName: 'Tech Meetup',
  reason: 'Weather conditions',
  actionUrl: 'https://yourapp.com/events/123',
})
console.log(subject)
console.log(html)
```

Send via Resend:

```ts
import { sendEmail } from './sendWithResend'

await sendEmail(
  'TICKET_CONFIRMATION',
  { eventName: 'Tech Meetup', ticketCount: 2, ticketsUrl: 'https://...' },
  { to: ['user@email.com'], from: 'Events <noreply@shde.com>' }
)
```

By default, the helper uses the rendered HTML. Pass `useReact: true` to send the raw React tree instead.

## Assets

- The logo is loaded from `${EMAIL_ASSET_BASE_URL || 'http://localhost:8080'}/images/shade_app_icon.png`, served by the Java app’s static resources (`src/main/resources/static/images`).
- For previews, either start the Java server or set `EMAIL_ASSET_BASE_URL` to the running host, e.g. `EMAIL_ASSET_BASE_URL=https://shade.example.com npx tsx preview.ts GENERAL_ANNOUNCEMENT`.
- If needed, pass a `logoSrc` prop to `BaseLayout` to override the logo source.
