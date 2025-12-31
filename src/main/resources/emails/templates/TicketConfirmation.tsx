import {
  Button,
  Heading,
  Section,
  Text,
} from '@react-email/components'
import * as React from 'react'
import { BaseLayout } from '../layouts/BaseLayout'

type TicketConfirmationProps = {
  attendeeName?: string
  eventName: string
  ticketCount?: number
  ticketsUrl: string
  mode?: 'light' | 'dark'
}

export function TicketConfirmation({
  attendeeName = 'there',
  eventName,
  ticketCount = 1,
  ticketsUrl,
  mode = 'light',
}: TicketConfirmationProps) {
  const colors = palette(mode)

  return (
    <BaseLayout
      heading="Your tickets are ready"
      previewText={`${ticketCount} ticket${ticketCount === 1 ? '' : 's'} for ${eventName}`}
      mode={mode}
    >
      <Heading style={styles.title(colors)}>You’re all set</Heading>
      <Text style={styles.paragraph(colors)}>
        Hi {attendeeName}, your ticket{ticketCount === 1 ? '' : 's'} for{' '}
        <strong>{eventName}</strong> are confirmed.
      </Text>

      <Text style={styles.detail(colors)}>
        <strong>Ticket count:</strong> {ticketCount}
      </Text>

      <Section style={styles.actionRow}>
        <Button href={ticketsUrl} style={styles.primaryButton(colors)}>
          View tickets
        </Button>
        <Text style={styles.linkHint(colors)}>{ticketsUrl}</Text>
      </Section>

      <Text style={styles.muted(colors)}>
        Save the QR codes to your wallet and bring a valid ID to check in
        smoothly.
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
      margin: '0 0 12px',
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
