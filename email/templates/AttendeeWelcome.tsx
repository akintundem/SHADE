import {
  Button,
  Heading,
  Section,
  Text,
} from '@react-email/components'
import * as React from 'react'
import { BaseLayout } from './BaseLayout'

type AttendeeWelcomeProps = {
  attendeeName?: string
  eventName: string
  eventDate?: string
  venue?: string
  actionUrl: string
  mode?: 'light' | 'dark'
}

export function AttendeeWelcome({
  attendeeName = 'there',
  eventName,
  eventDate,
  venue,
  actionUrl,
  mode = 'light',
}: AttendeeWelcomeProps) {
  const colors = palette(mode)

  return (
    <BaseLayout
      heading="You’re on the list"
      previewText={`You’ve been added to ${eventName}`}
      mode={mode}
    >
      <Heading style={styles.title(colors)}>Welcome, {attendeeName}</Heading>
      <Text style={styles.paragraph(colors)}>
        You’ve been added as an attendee for <strong>{eventName}</strong>.
        We’ll keep you posted on updates.
      </Text>

      {eventDate ? (
        <Text style={styles.detail(colors)}>
          <strong>When:</strong> {eventDate}
        </Text>
      ) : null}
      {venue ? (
        <Text style={styles.detail(colors)}>
          <strong>Where:</strong> {venue}
        </Text>
      ) : null}

      <Section style={styles.actionRow}>
        <Button href={actionUrl} style={styles.primaryButton(colors)}>
          View event
        </Button>
        <Text style={styles.linkHint(colors)}>{actionUrl}</Text>
      </Section>

      <Text style={styles.muted(colors)}>
        Bring a valid ID to check in quickly. Let us know if you can’t make it.
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
  detail: (c: ReturnType<typeof palette>) =>
    ({
      margin: '0 0 8px',
      fontSize: '14px',
      color: c.text,
      lineHeight: '20px',
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
