import { Button, Heading, Section, Text } from '@react-email/components'
import * as React from 'react'
import { BaseLayout } from './BaseLayout'

type AttendeeInviteResponseProps = {
  inviterName?: string
  inviteeName?: string
  eventName: string
  status: string
  eventUrl?: string
  mode?: 'light' | 'dark'
}

export function AttendeeInviteResponse({
  inviterName = 'Organizer',
  inviteeName = 'Guest',
  eventName,
  status,
  eventUrl,
  mode = 'light',
}: AttendeeInviteResponseProps) {
  const colors = palette(mode)
  const normalized = status ? status.toLowerCase() : 'updated'

  return (
    <BaseLayout
      heading="Invite update"
      previewText={`${inviteeName} ${normalized} the invite`}
      mode={mode}
    >
      <Heading style={styles.title(colors)}>Invite update</Heading>
      <Text style={styles.paragraph(colors)}>
        Hi {inviterName}, {inviteeName} {normalized} the invite for{' '}
        <strong>{eventName}</strong>.
      </Text>

      {eventUrl ? (
        <Section style={styles.actionRow}>
          <Button href={eventUrl} style={styles.primaryButton(colors)}>
            View event
          </Button>
        </Section>
      ) : null}

      <Text style={styles.muted(colors)}>
        You can update attendee details from the event dashboard.
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
  muted: (c: ReturnType<typeof palette>) =>
    ({
      fontSize: '13px',
      color: c.muted,
      margin: 0,
    }) as React.CSSProperties,
}
