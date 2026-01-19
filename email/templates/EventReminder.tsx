import {
  Button,
  Heading,
  Section,
  Text,
} from '@react-email/components'
import * as React from 'react'
import { BaseLayout } from './BaseLayout'

type EventReminderProps = {
  eventName: string
  eventDate?: string
  message?: string
  actionUrl: string
  mode?: 'light' | 'dark'
}

export function EventReminder({
  eventName,
  eventDate,
  message = 'Quick reminder before the event starts.',
  actionUrl,
  mode = 'light',
}: EventReminderProps) {
  const colors = palette(mode)

  return (
    <BaseLayout
      heading="Event reminder"
      previewText={`${eventName}${eventDate ? ` · ${eventDate}` : ''}`}
      mode={mode}
    >
      <Heading style={styles.title(colors)}>See you at {eventName}</Heading>
      {eventDate ? (
        <Text style={styles.meta(colors)}>Starts: {eventDate}</Text>
      ) : null}
      <Text style={styles.paragraph(colors)}>{message}</Text>

      <Section style={styles.actionRow}>
        <Button href={actionUrl} style={styles.primaryButton(colors)}>
          View details
        </Button>
      </Section>

      <Text style={styles.muted(colors)}>
        Add this to your calendar to stay on time. We’re excited to have you
        join.
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
  meta: (c: ReturnType<typeof palette>) =>
    ({
      fontSize: '14px',
      color: c.text,
      fontWeight: 700,
      margin: '0 0 10px',
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
