import {
  Body,
  Head,
  Html,
  Img,
  Link,
  Preview,
  Text,
} from '@react-email/components'
import * as React from 'react'

type BaseLayoutProps = {
  previewText?: string
  heading?: string
  children: React.ReactNode
  footerText?: string
  mode?: 'light' | 'dark'
  logoSrc?: string
}

const fontFamily = "'Helvetica Neue', Arial, 'sans-serif'"

function getPalette(mode?: 'light' | 'dark') {
  const isDark = mode === 'dark'
  return {
    isDark,
    background: isDark ? '#0b0b0c' : '#ffffff',
    surface: isDark ? '#0b0b0c' : '#ffffff',
    text: isDark ? '#f5f5f5' : '#0c0c0c',
    muted: isDark ? '#c2c2c2' : '#444444',
    border: isDark ? '#2a2a2a' : '#e5e5e5',
    link: isDark ? '#f5f5f5' : '#0c0c0c',
  }
}

function resolveLogoSrc(preferred?: string) {
  const bucket = process.env.APP_ASSET_BUCKET
  const logo = preferred ?? process.env.LOGO_URL ?? ''

  if (logo.startsWith('http://') || logo.startsWith('https://')) {
    return logo
  }

  if (logo && bucket) {
    return `https://${bucket}.s3.us-east-2.amazonaws.com/${logo}`
  }

  if (logo) {
    return logo
  }

  return ''
}

export function BaseLayout({
  previewText,
  heading,
  children,
  footerText = '© SHDE. All rights reserved.',
  mode = 'light',
  logoSrc,
}: BaseLayoutProps) {
  const palette = getPalette(mode)
  const resolvedLogo = resolveLogoSrc(logoSrc)

  return (
    <Html>
      <Head />
      {previewText ? <Preview>{previewText}</Preview> : null}
      <Body style={styles.body(palette)}>
        <table
          role="presentation"
          width="100%"
          cellPadding="0"
          cellSpacing="0"
          style={styles.outerTable(palette)}
        >
          <tbody>
            <tr>
              <td align="center" style={styles.outerCell}>
                <table
                  role="presentation"
                  width="100%"
                  cellPadding="0"
                  cellSpacing="0"
                  style={styles.innerTable(palette)}
                >
                  <tbody>
                    {heading ? (
                      <tr>
                        <td style={styles.header(palette)}>
                          <table
                            role="presentation"
                            width="100%"
                            cellPadding="0"
                            cellSpacing="0"
                          >
                            <tbody>
                              <tr>
                                {resolvedLogo ? (
                                  <td
                                    width="44"
                                    valign="top"
                                    style={styles.logoCell}
                                  >
                                    <Img
                                      src={resolvedLogo}
                                      alt="Shade"
                                      width="36"
                                      height="36"
                                      style={styles.logo}
                                    />
                                  </td>
                                ) : (
                                  <td width="44" style={styles.logoCell} />
                                )}
                                <td valign="top">
                                  <Text style={styles.brand(palette)}>Shade</Text>
                                  <Text style={styles.heading(palette)}>
                                    {heading}
                                  </Text>
                                  <Text style={styles.subheading(palette)}>
                                    Purpose-built event ops for modern teams.
                                  </Text>
                                </td>
                              </tr>
                            </tbody>
                          </table>
                        </td>
                      </tr>
                    ) : null}

                    <tr>
                      <td style={styles.content(palette)}>{children}</td>
                    </tr>

                    <tr>
                      <td style={styles.footer(palette)}>
                        <Text style={styles.footerText(palette)}>{footerText}</Text>
                        <Text style={styles.footerSubtext(palette)}>
                          You’re receiving this email as part of SHDE notifications.{' '}
                          <Link
                            href="https://shde.com/account/notifications"
                            style={styles.link(palette)}
                          >
                            Manage preferences
                          </Link>
                        </Text>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </td>
            </tr>
          </tbody>
        </table>
      </Body>
    </Html>
  )
}

const styles = {
  body: (p: ReturnType<typeof getPalette>) =>
    ({
      margin: 0,
      padding: '32px 0',
      backgroundColor: p.background,
      color: p.text,
      fontFamily,
    }) as React.CSSProperties,
  outerTable: (p: ReturnType<typeof getPalette>) =>
    ({
      backgroundColor: p.background,
    }) as React.CSSProperties,
  outerCell: {
    padding: '0 12px',
  } as React.CSSProperties,
  innerTable: (p: ReturnType<typeof getPalette>) =>
    ({
      width: '100%',
      maxWidth: '640px',
      margin: '0 auto',
      backgroundColor: p.surface,
      padding: '0 32px 32px',
    }) as React.CSSProperties,
  header: (p: ReturnType<typeof getPalette>) =>
    ({
      padding: '8px 0 18px',
      borderBottom: `1px solid ${p.border}`,
    }) as React.CSSProperties,
  logo: {
    display: 'block',
  } as React.CSSProperties,
  logoCell: {
    paddingRight: '12px',
  } as React.CSSProperties,
  heading: (p: ReturnType<typeof getPalette>) =>
    ({
      fontSize: '22px',
      fontWeight: 800,
      margin: '2px 0 0',
      letterSpacing: '-0.3px',
      color: p.text,
    }) as React.CSSProperties,
  brand: (p: ReturnType<typeof getPalette>) =>
    ({
      margin: 0,
      fontSize: '12px',
      letterSpacing: '1px',
      textTransform: 'uppercase',
      color: p.muted,
    }) as React.CSSProperties,
  subheading: (p: ReturnType<typeof getPalette>) =>
    ({
      margin: '6px 0 0',
      color: p.muted,
      fontSize: '13px',
      lineHeight: '20px',
    }) as React.CSSProperties,
  content: (p: ReturnType<typeof getPalette>) =>
    ({
      padding: '22px 0 18px',
      color: p.text,
    }) as React.CSSProperties,
  footer: (p: ReturnType<typeof getPalette>) =>
    ({
      padding: '18px 0 12px',
      borderTop: `1px solid ${p.border}`,
    }) as React.CSSProperties,
  footerText: (p: ReturnType<typeof getPalette>) =>
    ({
      color: p.text,
      fontSize: '13px',
      margin: '0 0 6px',
    }) as React.CSSProperties,
  footerSubtext: (p: ReturnType<typeof getPalette>) =>
    ({
      color: p.muted,
      fontSize: '12px',
      lineHeight: '18px',
      margin: 0,
    }) as React.CSSProperties,
  link: (p: ReturnType<typeof getPalette>) =>
    ({
      color: p.link,
      textDecoration: 'underline',
      fontWeight: 600,
    }) as React.CSSProperties,
}
