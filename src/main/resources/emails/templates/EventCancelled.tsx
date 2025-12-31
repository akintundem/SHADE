import {
  Button,
  Heading,
  Section,
  Text,
} from '@react-email/components'
import * as React from 'react'
import { BaseLayout } from '../layouts/BaseLayout'

type EventCancelledProps = {
  eventName: string
  reason?: string
  actionUrl: string
  mode?: 'light' | 'dark'
}

export function EventCancelled({
  eventName,
  reason = 'The organizing team had to cancel this event.',
  actionUrl,
  mode = 'light',
}: EventCancelledProps) {
  const colors = palette(mode)

  return (
    <BaseLayout
      heading="Event cancelled"
      previewText={`${eventName} has been cancelled`}
      mode={mode}
    >
      <Heading style={styles.title(colors)}>Event cancelled</Heading>
      <Text style={styles.paragraph(colors)}>
        The event <strong>{eventName}</strong> will no longer take place.
      </Text>
      <Text style={styles.paragraph(colors)}>{reason}</Text>

      <Section style={styles.actionRow}>
        <Button href={actionUrl} style={styles.primaryButton(colors)}>
          View details
        </Button>
        <Text style={styles.linkHint(colors)}>{actionUrl}</Text>
      </Section>

      <Text style={styles.muted(colors)}>
        We’re sorry for the change of plans. Contact the organizer if you have
        questions.
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
      margin: '0 0 12px',
      color: c.text,
      letterSpacing: '-0.2px',
    }) as React.CSSProperties,
  paragraph: (c: ReturnType<typeof palette>) =>
    ({
      color: c.text,
      lineHeight: '24px',
      fontSize: '15px',
      margin: '0 0 12px',
    }) as React.CSSProperties,
  actionRow: {
    margin: '18px 0 10px',
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
