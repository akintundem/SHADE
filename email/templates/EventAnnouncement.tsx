import {
  Button,
  Column,
  Heading,
  Row,
  Section,
  Text,
} from '@react-email/components'
import * as React from 'react'
import { BaseLayout } from './BaseLayout'

type EventAnnouncementProps = {
  eventName: string
  subjectLine?: string
  eventDate?: string
  venue?: string
  highlight?: string
  actionUrl: string
  mode?: 'light' | 'dark'
}

export function EventAnnouncement({
  eventName,
  subjectLine = 'You are invited',
  eventDate,
  venue,
  highlight,
  actionUrl,
  mode = 'light',
}: EventAnnouncementProps) {
  const colors = palette(mode)

  return (
    <BaseLayout
      heading="Event announcement"
      previewText={`${eventName}${eventDate ? ` · ${eventDate}` : ''}`}
      mode={mode}
    >
      <Heading style={styles.title(colors)}>{eventName}</Heading>
      <Text style={styles.lead(colors)}>{subjectLine}</Text>

      <Row style={styles.metaRow}>
        {eventDate ? (
          <Column>
            <Text style={styles.metaLabel(colors)}>Date & time</Text>
            <Text style={styles.metaValue(colors)}>{eventDate}</Text>
          </Column>
        ) : null}
        {venue ? (
          <Column>
            <Text style={styles.metaLabel(colors)}>Location</Text>
            <Text style={styles.metaValue(colors)}>{venue}</Text>
          </Column>
        ) : null}
      </Row>

      {highlight ? (
        <Section>
          <Text style={styles.metaLabel(colors)}>Why attend</Text>
          <Text style={styles.calloutCopy(colors)}>{highlight}</Text>
        </Section>
      ) : null}

      <Section style={styles.actionRow}>
        <Button href={actionUrl} style={styles.primaryButton(colors)}>
          View event
        </Button>
        <Text style={styles.linkHint(colors)}>{actionUrl}</Text>
      </Section>

      <Text style={styles.muted(colors)}>
        Share this invite with teammates who should join.
      </Text>
    </BaseLayout>
  )
}

function palette(mode?: 'light' | 'dark') {
  const isDark = mode === 'dark'
  return {
    text: isDark ? '#f5f5f5' : '#0c0c0c',
    muted: isDark ? '#c2c2c2' : '#444444',
    line: isDark ? '#2a2a2a' : '#e5e5e5',
    buttonBg: isDark ? '#f5f5f5' : '#0c0c0c',
    buttonText: isDark ? '#0c0c0c' : '#f5f5f5',
  }
}

const styles = {
  title: (c: ReturnType<typeof palette>) =>
    ({
      fontSize: '24px',
      fontWeight: 800,
      margin: '0 0 8px',
      color: c.text,
      letterSpacing: '-0.2px',
    }) as React.CSSProperties,
  lead: (c: ReturnType<typeof palette>) =>
    ({
      fontSize: '15px',
      color: c.text,
      margin: '0 0 16px',
      lineHeight: '22px',
    }) as React.CSSProperties,
  metaRow: {
    display: 'flex',
    gap: '24px',
    margin: '0 0 16px',
  } as React.CSSProperties,
  metaLabel: (c: ReturnType<typeof palette>) =>
    ({
      fontSize: '12px',
      textTransform: 'uppercase',
      letterSpacing: '0.3px',
      color: c.muted,
      margin: '0 0 4px',
    }) as React.CSSProperties,
  metaValue: (c: ReturnType<typeof palette>) =>
    ({
      fontSize: '14px',
      color: c.text,
      margin: '0 0 8px',
      fontWeight: 700,
    }) as React.CSSProperties,
  calloutCopy: (c: ReturnType<typeof palette>) =>
    ({
      margin: '0 0 12px',
      lineHeight: '22px',
      fontSize: '14px',
      color: c.text,
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
