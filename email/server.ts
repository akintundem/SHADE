import amqp from 'amqplib'
import { render } from '@react-email/render'
import { Resend } from 'resend'
import { config } from './config'
import { EMAIL_TEMPLATES } from './templates/index'

interface EmailJobPayload {
  templateId: string
  to: string | string[]
  cc?: string | string[]
  bcc?: string | string[]
  replyTo?: string
  from?: string
  subject?: string
  variables?: Record<string, unknown>
}

if (!config.resendApiKey) throw new Error('RESEND_API_KEY is required')
if (!config.resendFrom) throw new Error('RESEND_FROM is required')
if (!config.rabbitmqUrl) throw new Error('RABBITMQ_URL is required')
if (!config.rabbitmqExchange) throw new Error('RABBITMQ_EXCHANGE is required')
if (!config.rabbitmqQueue) throw new Error('RABBITMQ_EMAIL_QUEUE is required')
if (!config.rabbitmqRoutingKey) throw new Error('RABBITMQ_EMAIL_ROUTING_KEY is required')
if (!config.rabbitmqPrefetch) throw new Error('RABBITMQ_PREFETCH is required')
if (!config.rabbitmqReconnectMs) throw new Error('RABBITMQ_RECONNECT_MS is required')
// RABBITMQ_REQUEUE_ON_ERROR is no longer used — retry logic is handled by DLX + x-retry-count header

const rabbitmqPrefetch = Number(config.rabbitmqPrefetch)
const rabbitmqReconnectMs = Number(config.rabbitmqReconnectMs)
const MAX_RETRIES = 3
const DLX_EXCHANGE = 'dlx.notifications'
const DLX_EMAIL_QUEUE = 'dlq.email'
const DEFAULT_FROM = config.resendFrom as string
const ALLOWED = new Set(
  (config.allowedTemplates || '')
    .split(',')
    .map((entry) => entry.trim())
    .filter(Boolean)
)
const resend = new Resend(config.resendApiKey as string)

const MAX_RECIPIENTS_TOTAL = 50
const MAX_SUBJECT_LENGTH = 500
const MAX_FROM_LENGTH = 200
const BASIC_EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function isValidEmail(s: string): boolean {
  return typeof s === 'string' && s.length <= 254 && BASIC_EMAIL_REGEX.test(s.trim())
}

type TemplateLookupEntry = { key: string; id: string; subject: unknown; component: (props: never) => unknown }
const templateLookup = new Map<string, TemplateLookupEntry>()
Object.entries(EMAIL_TEMPLATES).forEach(([key, entry]) => {
  templateLookup.set(key, { key, ...entry } as TemplateLookupEntry)
  templateLookup.set(entry.id, { key, ...entry } as TemplateLookupEntry)
})

const normalizeList = (value?: string | string[]) => {
  if (!value) return []
  return Array.isArray(value) ? value : [value]
}

const requestError = (message: string) => {
  const err = new Error(message) as Error & { statusCode?: number }
  err.statusCode = 400
  throw err
}

const resolveEmailRequest = (payload: EmailJobPayload) => {
  const { templateId, to, cc, bcc, replyTo, subject, from, variables = {} } = payload || {}
  if (!templateId) requestError('templateId is required')

  const toList = normalizeList(to)
  if (!toList.length) requestError('to is required')
  const ccList = normalizeList(cc)
  const bccList = normalizeList(bcc)
  const totalRecipients = toList.length + ccList.length + bccList.length
  if (totalRecipients > MAX_RECIPIENTS_TOTAL) requestError(`total recipients must be at most ${MAX_RECIPIENTS_TOTAL}`)

  for (const addr of [...toList, ...ccList, ...bccList]) {
    if (!isValidEmail(addr)) requestError(`invalid email address: ${String(addr).slice(0, 50)}`)
  }

  const fromAddress = from || DEFAULT_FROM
  if (!fromAddress) requestError('from is required')
  if (fromAddress.length > MAX_FROM_LENGTH) requestError('from too long')

  const template = templateLookup.get(templateId)
  if (!template) requestError('unknown templateId')

  if (ALLOWED.size && !ALLOWED.has(template.id) && !ALLOWED.has(template.key)) {
    requestError('template not allowed')
  }

  const props = { ...variables }
  const resolvedSubject =
    subject ||
    (typeof template.subject === 'function'
      ? template.subject(props as never)
      : template.subject)
  if (resolvedSubject && resolvedSubject.length > MAX_SUBJECT_LENGTH) requestError('subject too long')
  if (replyTo && !isValidEmail(replyTo)) requestError('invalid replyTo')

  return {
    toList,
    ccList,
    bccList,
    replyTo,
    resolvedSubject,
    fromAddress,
    props,
    template,
  }
}

const sendEmail = async (payload: EmailJobPayload) => {
  const { toList, ccList, bccList, replyTo, resolvedSubject, fromAddress, props, template } =
    resolveEmailRequest(payload)

  // Render React component to HTML
  const reactElement = template.component(props as never)
  const html = await render(reactElement)

  return resend.emails.send({
    from: fromAddress,
    to: toList,
    cc: ccList.length ? ccList : undefined,
    bcc: bccList.length ? bccList : undefined,
    reply_to: replyTo,
    subject: resolvedSubject,
    html: html,
  })
}

const startRabbitConsumer = async () => {
  try {
    const connection = await amqp.connect(config.rabbitmqUrl as string)
    connection.on('error', (err: Error) => {
      console.error('[rabbitmq] connection error:', err)
      process.exit(1)
    })
    connection.on('close', () => {
      setTimeout(startRabbitConsumer, rabbitmqReconnectMs)
    })

    const channel = await connection.createChannel()

    // Assert dead-letter exchange and queue first
    await channel.assertExchange(DLX_EXCHANGE, 'direct', { durable: true })
    await channel.assertQueue(DLX_EMAIL_QUEUE, { durable: true })
    await channel.bindQueue(DLX_EMAIL_QUEUE, DLX_EXCHANGE, 'dead.email')

    await channel.assertExchange(config.rabbitmqExchange as string, 'direct', { durable: true })
    await channel.assertQueue(config.rabbitmqQueue as string, {
      durable: true,
      arguments: {
        'x-dead-letter-exchange': DLX_EXCHANGE,
        'x-dead-letter-routing-key': 'dead.email',
      },
    })
    await channel.bindQueue(
      config.rabbitmqQueue as string,
      config.rabbitmqExchange as string,
      config.rabbitmqRoutingKey as string
    )
    channel.prefetch(rabbitmqPrefetch)

    await channel.consume(config.rabbitmqQueue as string, async (msg) => {
      if (!msg) return
      let payload: EmailJobPayload
      try {
        payload = JSON.parse(msg.content.toString()) as EmailJobPayload
      } catch (err) {
        // Unparseable message — dead-letter immediately, do not requeue
        channel.nack(msg, false, false)
        return
      }

      try {
        await sendEmail(payload)
        channel.ack(msg)
      } catch (err: unknown) {
        if ((err as { statusCode?: number })?.statusCode === 400) {
          // Validation error — message is permanently bad, dead-letter it
          channel.nack(msg, false, false)
          return
        }
        // Transient error — track retry count
        const headers = (msg.properties.headers as Record<string, unknown>) || {}
        const retryCount = (Number(headers['x-retry-count']) || 0) + 1
        if (retryCount >= MAX_RETRIES) {
          console.error(`[email] message failed after ${MAX_RETRIES} attempts, dead-lettering`)
          channel.nack(msg, false, false)
        } else {
          // Republish with incremented retry count rather than requeue to head
          channel.ack(msg)
          channel.publish(
            config.rabbitmqExchange as string,
            config.rabbitmqRoutingKey as string,
            msg.content,
            { headers: { ...headers, 'x-retry-count': retryCount }, persistent: true }
          )
        }
      }
    })

  } catch (err) {
    setTimeout(startRabbitConsumer, rabbitmqReconnectMs)
  }
}

startRabbitConsumer()
