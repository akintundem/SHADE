import * as React from 'react'
import { render } from '@react-email/render'
import { AttendeeWelcome } from './AttendeeWelcome'
import { AttendeeInvite } from './AttendeeInvite'
import { AttendeeInviteResponse } from './AttendeeInviteResponse'
import { CollaboratorInvite } from './CollaboratorInvite'
import { CollaboratorWelcome } from './CollaboratorWelcome'
import { EventAnnouncement } from './EventAnnouncement'
import { EventCancelled } from './EventCancelled'
import { EventReminder } from './EventReminder'
import { TicketConfirmation } from './TicketConfirmation'

type TemplateEntry<P> = {
  id: string
  subject: string | ((props: P) => string)
  component: (props: P) => React.ReactElement
}

export const EMAIL_TEMPLATES = {
  GENERAL_ANNOUNCEMENT: {
    id: 'general-announcement',
    subject: ({ eventName }: { eventName: string }) =>
      `You’re invited: ${eventName}`,
    component: EventAnnouncement,
  },
  EVENT_CANCELLATION: {
    id: 'event-cancellation-notice',
    subject: ({ eventName }: { eventName: string }) =>
      `Event cancelled: ${eventName}`,
    component: EventCancelled,
  },
  EVENT_REMINDER: {
    id: 'event-reminder',
    subject: ({ eventName }: { eventName: string }) =>
      `Reminder: ${eventName}`,
    component: EventReminder,
  },
  ATTENDEE_WELCOME: {
    id: 'attendee-welcome',
    subject: ({ eventName }: { eventName: string }) =>
      `You’re attending: ${eventName}`,
    component: AttendeeWelcome,
  },
  ATTENDEE_INVITE: {
    id: 'attendee-invite',
    subject: ({ eventName }: { eventName: string }) =>
      `You're invited: ${eventName}`,
    component: AttendeeInvite,
  },
  ATTENDEE_INVITE_RESPONSE: {
    id: 'attendee-invite-response',
    subject: ({
      eventName,
      inviteeName,
      status,
    }: {
      eventName: string
      inviteeName?: string
      status?: string
    }) => {
      const normalized = status ? status.toLowerCase() : 'updated'
      const name = inviteeName || 'An attendee'
      return `${name} ${normalized} the invite to ${eventName}`
    },
    component: AttendeeInviteResponse,
  },
  TICKET_CONFIRMATION: {
    id: 'ticket-confirmation',
    subject: ({ eventName }: { eventName: string }) =>
      `Your tickets for ${eventName}`,
    component: TicketConfirmation,
  },
  COLLABORATOR_INVITE: {
    id: 'event-invitation',
    subject: ({ eventName }: { eventName: string }) =>
      `Invitation to collaborate on ${eventName}`,
    component: CollaboratorInvite,
  },
  COLLABORATOR_WELCOME: {
    id: 'collaborator-welcome',
    subject: ({ eventName }: { eventName: string }) =>
      `Added to ${eventName}`,
    component: CollaboratorWelcome,
  },
} satisfies Record<string, TemplateEntry<any>>

export type EmailTemplateKey = keyof typeof EMAIL_TEMPLATES

/**
 * Render a template to HTML + subject so it can be passed to Resend
 */
export async function renderEmailTemplate<K extends EmailTemplateKey>(
  key: K,
  props: Parameters<(typeof EMAIL_TEMPLATES)[K]['component']>[0]
) {
  const template = EMAIL_TEMPLATES[key]
  const subject =
    typeof template.subject === 'function'
      ? template.subject(props as never)
      : template.subject
  const rendered = render(template.component(props as never))
  const html = rendered instanceof Promise ? await rendered : rendered

  return {
    subject,
    html,
    templateId: template.id,
  }
}

export {
  AttendeeInvite,
  AttendeeInviteResponse,
  AttendeeWelcome,
  CollaboratorInvite,
  CollaboratorWelcome,
  EventAnnouncement,
  EventCancelled,
  EventReminder,
  TicketConfirmation,
}
