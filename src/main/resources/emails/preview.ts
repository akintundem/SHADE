import { writeFileSync } from 'fs'
import path from 'path'
import { EmailTemplateKey, renderEmailTemplate } from './templates'

const sampleData: Record<EmailTemplateKey, any> = {
  EMAIL_VERIFICATION: {
    userName: 'Jordan',
    confirmLink: 'https://shde.com/auth/verify?token=example',
    appName: 'SHDE',
  },
  PASSWORD_RESET: {
    userName: 'Jordan',
    resetLink: 'https://shde.com/auth/reset-password?token=example',
    expiresInMinutes: 60,
  },
  GENERAL_ANNOUNCEMENT: {
    eventName: 'Tech Meetup',
    subjectLine: 'A night of product demos',
    eventDate: 'Aug 12, 7:00 PM',
    venue: 'Pier 27, San Francisco',
    highlight: 'Live demos from three YC founders plus networking.',
    actionUrl: 'https://shde.com/events/tech-meetup',
  },
  EVENT_CANCELLATION: {
    eventName: 'Tech Meetup',
    reason: 'Weather conditions and travel delays.',
    actionUrl: 'https://shde.com/events/tech-meetup',
  },
  EVENT_REMINDER: {
    eventName: 'Tech Meetup',
    eventDate: 'Aug 12, 7:00 PM',
    message: 'Doors open at 6:30 PM. Parking is limited, rideshare recommended.',
    actionUrl: 'https://shde.com/events/tech-meetup',
  },
  ATTENDEE_WELCOME: {
    attendeeName: 'Jordan',
    eventName: 'Tech Meetup',
    eventDate: 'Aug 12, 7:00 PM',
    venue: 'Pier 27, San Francisco',
    actionUrl: 'https://shde.com/events/tech-meetup',
  },
  TICKET_CONFIRMATION: {
    attendeeName: 'Jordan',
    eventName: 'Tech Meetup',
    ticketCount: 2,
    ticketsUrl: 'https://shde.com/tickets/abc123',
  },
  COLLABORATOR_INVITE: {
    inviteeName: 'Jordan',
    inviterName: 'Alex',
    eventName: 'Tech Meetup',
    role: 'Coordinator',
    message: 'Join us to run the speaker schedule and help with AV.',
    acceptUrl: 'https://shde.com/collab/accept?token=example',
  },
  COLLABORATOR_WELCOME: {
    collaboratorName: 'Jordan',
    eventName: 'Tech Meetup',
    role: 'Coordinator',
    eventDate: 'Aug 12, 7:00 PM',
    actionUrl: 'https://shde.com/events/tech-meetup/workspace',
  },
}

const key = (process.argv[2] as EmailTemplateKey) || 'GENERAL_ANNOUNCEMENT'
const modeArg = process.argv[3] as 'light' | 'dark' | undefined

if (!sampleData[key]) {
  console.error(
    `Invalid template key. Choose one of: ${Object.keys(sampleData).join(', ')}`
  )
  process.exit(1)
}

async function run() {
  const props = modeArg ? { ...sampleData[key], mode: modeArg } : sampleData[key]
  const { html, subject, templateId } = await renderEmailTemplate(
    key,
    props
  )
  const outPath = path.join(__dirname, 'preview.html')
  writeFileSync(outPath, html)

  console.log(`Rendered ${key} (${templateId}) to ${outPath}`)
  console.log(`Subject: ${subject}`)
}

run().catch((err) => {
  console.error(err)
  process.exit(1)
})
