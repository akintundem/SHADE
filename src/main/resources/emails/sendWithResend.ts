import { Resend } from 'resend'
import { EMAIL_TEMPLATES, EmailTemplateKey, renderEmailTemplate } from './templates'

type SendOptions = {
  to: string | string[]
  from?: string
  bcc?: string[]
  useReact?: boolean
}

const defaultFrom = process.env.RESEND_FROM_EMAIL || 'SHDE <noreply@shde.com>'
const resend = new Resend(process.env.RESEND_API_KEY)

/**
 * Send any of the templates via Resend using either the rendered HTML
 * or the raw React tree.
 */
export async function sendEmail<K extends EmailTemplateKey>(
  key: K,
  props: Parameters<(typeof EMAIL_TEMPLATES)[K]['component']>[0],
  options: SendOptions
) {
  const { html, subject } = await renderEmailTemplate(key, props)
  const payload = {
    from: options.from || defaultFrom,
    to: Array.isArray(options.to) ? options.to : [options.to],
    bcc: options.bcc,
    subject,
    ...(options.useReact
      ? { react: EMAIL_TEMPLATES[key].component(props as never) }
      : { html }),
  }

  return resend.emails.send(payload)
}
