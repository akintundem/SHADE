import {
  Button,
  Heading,
  Hr,
  Section,
  Text,
} from '@react-email/components'
import * as React from 'react'
import { BaseLayout } from './BaseLayout'

type EmailVerificationProps = {
  userName?: string
  confirmLink: string
  appName?: string
  mode?: 'light' | 'dark'
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

export function EmailVerification({
  userName = 'there',
  confirmLink,
  appName = 'SHDE',
  mode = 'light',
}: EmailVerificationProps) {
  const colors = palette(mode)

  return (
    <BaseLayout
      heading="Verify your email"
      previewText={`Confirm your ${appName} account in one tap`}
      mode={mode}
    >
      <Heading style={styles.title(colors)}>Welcome to {appName}</Heading>
      <Text style={styles.paragraph(colors)}>
        Hi {userName},
        <br />
        Confirm your email to secure your account and start planning events.
      </Text>

      <Section style={styles.actionRow}>
        <Button href={confirmLink} style={styles.primaryButton(colors)}>
          Confirm email
        </Button>
      </Section>

      <Hr style={styles.hr(colors)} />
      <Text style={styles.muted(colors)}>
        Didn’t create an account? You can ignore this email.
      </Text>
    </BaseLayout>
  )
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
  hr: (c: ReturnType<typeof palette>) =>
    ({
      border: 'none',
      borderTop: `1px solid ${c.line}`,
      margin: '20px 0 14px',
    }) as React.CSSProperties,
  muted: (c: ReturnType<typeof palette>) =>
    ({
      fontSize: '13px',
      color: c.muted,
      margin: 0,
    }) as React.CSSProperties,
}
