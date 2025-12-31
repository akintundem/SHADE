import {
  Button,
  Heading,
  Section,
  Text,
} from '@react-email/components'
import * as React from 'react'
import { BaseLayout } from '../layouts/BaseLayout'

type CollaboratorInviteProps = {
  inviteeName?: string
  inviterName?: string
  eventName: string
  role?: string
  message?: string
  acceptUrl: string
  mode?: 'light' | 'dark'
}

export function CollaboratorInvite({
  inviteeName = 'there',
  inviterName = 'Someone',
  eventName,
  role = 'Collaborator',
  message,
  acceptUrl,
  mode = 'light',
}: CollaboratorInviteProps) {
  const colors = palette(mode)

  return (
    <BaseLayout
      heading="Collaboration invite"
      previewText={`${inviterName} invited you to ${eventName}`}
      mode={mode}
    >
      <Heading style={styles.title(colors)}>Join {eventName}</Heading>
      <Text style={styles.paragraph(colors)}>
        Hi {inviteeName}, {inviterName} invited you to collaborate on{' '}
        <strong>{eventName}</strong> as a {role.toLowerCase()}.
      </Text>

      {message ? (
        <Section>
          <Text style={styles.meta(colors)}>Note from {inviterName}</Text>
          <Text style={styles.calloutCopy(colors)}>{message}</Text>
        </Section>
      ) : null}

      <Section style={styles.actionRow}>
        <Button href={acceptUrl} style={styles.primaryButton(colors)}>
          Accept invite
        </Button>
        <Text style={styles.linkHint(colors)}>{acceptUrl}</Text>
      </Section>

      <Text style={styles.muted(colors)}>
        You’ll get access to schedules, attendees, and notifications once you
        accept.
      </Text>
    </BaseLayout>
  )
}

function palette(mode?: 'light' | 'dark') {
  const isDark = mode === 'dark'
  return {
    text: isDark ? '#f5f5f5' : '#0c0c0c',
    muted: isDark ? '#c2c2c2' : '#444444',
    buttonBg: isDark ? '#f5f5f5' : '#0c0c0c',
    buttonText: isDark ? '#0c0c0c' : '#f5f5f5',
  }
}

const styles = {
  title: (c: ReturnType<typeof palette>) =>
    ({
      fontSize: '22px',
      fontWeight: 800,
      margin: '0 0 10px',
      color: c.text,
      letterSpacing: '-0.2px',
    }) as React.CSSProperties,
  paragraph: (c: ReturnType<typeof palette>) =>
    ({
      color: c.text,
      lineHeight: '24px',
      fontSize: '15px',
      margin: '0 0 18px',
    }) as React.CSSProperties,
  meta: (c: ReturnType<typeof palette>) =>
    ({
      fontSize: '12px',
      textTransform: 'uppercase',
      letterSpacing: '0.3px',
      color: c.muted,
      margin: '0 0 6px',
    }) as React.CSSProperties,
  calloutCopy: (c: ReturnType<typeof palette>) =>
    ({
      margin: '0 0 12px',
      lineHeight: '22px',
      fontSize: '14px',
      color: c.text,
    }) as React.CSSProperties,
  actionRow: {
    margin: '14px 0 10px',
  } as React.CSSProperties,
  primaryButton: (c: ReturnType<typeof palette>) =>
    ({
      backgroundColor: c.buttonBg,
      color: c.buttonText,
      padding: '13px 18px',
      fontWeight: 700,
      textDecoration: 'none',
      display: 'inline-block',
      borderRadius: '8px',
      border: `1px solid ${c.buttonBg}`,
    }) as React.CSSProperties,
  linkHint: (c: ReturnType<typeof palette>) =>
    ({
      marginTop: '12px',
      fontSize: '12px',
      color: c.muted,
      wordBreak: 'break-all',
    }) as React.CSSProperties,
  muted: (c: ReturnType<typeof palette>) =>
    ({
      fontSize: '13px',
      color: c.muted,
      margin: 0,
    }) as React.CSSProperties,
}
