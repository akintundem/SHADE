// @ts-nocheck
import amqp from 'amqplib'
import { render } from '@react-email/render'
import { Resend } from 'resend'
import { config } from './config'
import { EMAIL_TEMPLATES } from './templates/index'

if (!config.resendApiKey) throw new Error('RESEND_API_KEY is required')
if (!config.resendFrom) throw new Error('RESEND_FROM is required')
if (!config.rabbitmqUrl) throw new Error('RABBITMQ_URL is required')
if (!config.rabbitmqExchange) throw new Error('RABBITMQ_EXCHANGE is required')
if (!config.rabbitmqQueue) throw new Error('RABBITMQ_EMAIL_QUEUE is required')
if (!config.rabbitmqRoutingKey) throw new Error('RABBITMQ_EMAIL_ROUTING_KEY is required')
if (!config.rabbitmqPrefetch) throw new Error('RABBITMQ_PREFETCH is required')
if (!config.rabbitmqReconnectMs) throw new Error('RABBITMQ_RECONNECT_MS is required')
if (!config.rabbitmqRequeueOnError) throw new Error('RABBITMQ_REQUEUE_ON_ERROR is required')

const rabbitmqPrefetch = Number(config.rabbitmqPrefetch)
const rabbitmqReconnectMs = Number(config.rabbitmqReconnectMs)
const rabbitmqRequeueOnError = config.rabbitmqRequeueOnError === 'true'
const DEFAULT_FROM = config.resendFrom as string
const ALLOWED = new Set(
  (config.allowedTemplates || '')
    .split(',')
    .map((entry) => entry.trim())
    .filter(Boolean)
)
const resend = new Resend(config.resendApiKey as string)

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
    const connection = await amqp.connect(config.rabbitmqUrl as string)
    connection.on('error', () => {})
    connection.on('close', () => {
      setTimeout(startRabbitConsumer, rabbitmqReconnectMs)
    })

    const channel = await connection.createChannel()
    await channel.assertExchange(config.rabbitmqExchange as string, 'direct', { durable: true })
    await channel.assertQueue(config.rabbitmqQueue as string, { durable: true })
    await channel.bindQueue(
      config.rabbitmqQueue as string,
      config.rabbitmqExchange as string,
      config.rabbitmqRoutingKey as string
    )
    channel.prefetch(rabbitmqPrefetch)

    await channel.consume(config.rabbitmqQueue as string, async (msg) => {
      if (!msg) return
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
        channel.nack(msg, false, rabbitmqRequeueOnError)
      }
    })

  } catch (err) {
    setTimeout(startRabbitConsumer, rabbitmqReconnectMs)
  }
}

startRabbitConsumer()
