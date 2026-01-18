// @ts-nocheck
import 'dotenv/config'
import amqp from 'amqplib'
import { render } from '@react-email/render'
import { Resend } from 'resend'
import { EMAIL_TEMPLATES } from './templates/index'

const DEFAULT_FROM = process.env.RESEND_FROM || ''
const ALLOWED = new Set(
  (process.env.ALLOWED_TEMPLATES || '')
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
)
const RABBITMQ_URL = process.env.RABBITMQ_URL || 'amqp://guest:guest@rabbitmq:5672'
const RABBITMQ_EXCHANGE = process.env.RABBITMQ_EXCHANGE || 'notifications'
const RABBITMQ_QUEUE = process.env.RABBITMQ_EMAIL_QUEUE || 'email.jobs'
const RABBITMQ_ROUTING_KEY = process.env.RABBITMQ_EMAIL_ROUTING_KEY || 'email.send'
const RABBITMQ_PREFETCH = Number(process.env.RABBITMQ_PREFETCH || '10')
const RABBITMQ_RECONNECT_MS = Number(process.env.RABBITMQ_RECONNECT_MS || '5000')
const RABBITMQ_REQUEUE_ON_ERROR = process.env.RABBITMQ_REQUEUE_ON_ERROR === 'true'

const resendApiKey = process.env.RESEND_API_KEY
if (!resendApiKey) {
  throw new Error('RESEND_API_KEY is required (set in .env or environment)')
}

const resend = new Resend(resendApiKey)

const templateLookup = new Map()
Object.entries(EMAIL_TEMPLATES).forEach(([key, entry]) => {
  templateLookup.set(key, { key, ...entry })
  templateLookup.set(entry.id, { key, ...entry })
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

const resolveEmailRequest = (payload: any) => {
  const { templateId, to, cc, bcc, replyTo, subject, from, variables = {} } = payload || {}
  if (!templateId) requestError('templateId is required')

  const toList = normalizeList(to)
  if (!toList.length) requestError('to is required')
  const fromAddress = from || DEFAULT_FROM
  if (!fromAddress) requestError('from is required')

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

  const ccList = normalizeList(cc)
  const bccList = normalizeList(bcc)

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

const sendEmail = async (payload: any) => {
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
    const connection = await amqp.connect(RABBITMQ_URL)
    connection.on('error', () => {})
    connection.on('close', () => {
      setTimeout(startRabbitConsumer, RABBITMQ_RECONNECT_MS)
    })

    const channel = await connection.createChannel()
    await channel.assertExchange(RABBITMQ_EXCHANGE, 'direct', { durable: true })
    await channel.assertQueue(RABBITMQ_QUEUE, { durable: true })
    await channel.bindQueue(RABBITMQ_QUEUE, RABBITMQ_EXCHANGE, RABBITMQ_ROUTING_KEY)
    channel.prefetch(RABBITMQ_PREFETCH)

    await channel.consume(RABBITMQ_QUEUE, async (msg) => {
      if (!msg) return
      const jobId = msg.properties.messageId || msg.properties.correlationId || 'unknown'

      let payload: any
      try {
        payload = JSON.parse(msg.content.toString())
      } catch (err) {
        channel.ack(msg)
        return
      }

      try {
        await sendEmail(payload)
        channel.ack(msg)
      } catch (err: any) {
        if (err?.statusCode === 400) {
          channel.ack(msg)
          return
        }
        channel.nack(msg, false, RABBITMQ_REQUEUE_ON_ERROR)
      }
    })

  } catch (err) {
    setTimeout(startRabbitConsumer, RABBITMQ_RECONNECT_MS)
  }
}

startRabbitConsumer()
