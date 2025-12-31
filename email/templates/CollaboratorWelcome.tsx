import {
  Button,
  Heading,
  Section,
  Text,
} from '@react-email/components'
import * as React from 'react'
import { BaseLayout } from './BaseLayout'

type CollaboratorWelcomeProps = {
  collaboratorName?: string
  eventName: string
  role?: string
  eventDate?: string
  actionUrl: string
  mode?: 'light' | 'dark'
}

export function CollaboratorWelcome({
  collaboratorName = 'there',
  eventName,
  role = 'Collaborator',
  eventDate,
  actionUrl,
  mode = 'light',
}: CollaboratorWelcomeProps) {
  const colors = palette(mode)

  return (
    <BaseLayout
      heading="You’re part of the crew"
      previewText={`Added as ${role.toLowerCase()} on ${eventName}`}
      mode={mode}
    >
      <Heading style={styles.title(colors)}>
        Welcome aboard, {collaboratorName}
      </Heading>
      <Text style={styles.paragraph(colors)}>
        You’ve been added to <strong>{eventName}</strong> as a{' '}
        {role.toLowerCase()}. We rely on you to keep this event running smoothly.
      </Text>

      {eventDate ? (
        <Section>
          <Text style={styles.detail(colors)}>
            <strong>Kickoff:</strong> {eventDate}
          </Text>
        </Section>
      ) : null}

      <Section style={styles.actionRow}>
        <Button href={actionUrl} style={styles.primaryButton(colors)}>
          Open event workspace
        </Button>
      </Section>

      <Text style={styles.muted(colors)}>
        Update your notification preferences anytime in the workspace.
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
  muted: (c: ReturnType<typeof palette>) =>
    ({
      fontSize: '13px',
      color: c.muted,
      margin: 0,
    }) as React.CSSProperties,
}
