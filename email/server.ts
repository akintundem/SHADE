// @ts-nocheck
import 'dotenv/config'
import express from 'express'
import React from 'react'
import { render } from '@react-email/render'
import { Resend } from 'resend'
import { EMAIL_TEMPLATES } from './templates/index'

const app = express()
app.use(express.json())

const PORT = process.env.PORT || 3000
const DEFAULT_FROM = process.env.RESEND_FROM || ''
const SECRET = process.env.RESEND_SHARED_SECRET || process.env.EMAIL_SHARED_SECRET || ''
const ALLOWED = new Set(
  (process.env.ALLOWED_TEMPLATES || '')
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
)

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

app.get('/health', (_req, res) => {
  res.json({ ok: true })
})

app.post('/send-email', async (req, res) => {
  try {
    if (SECRET && req.get('x-email-secret') !== SECRET) {
      return res.status(401).json({ error: 'unauthorized' })
    }

    const { templateId, to, cc, bcc, replyTo, subject, from, variables = {} } = req.body || {}
    if (!templateId) return res.status(400).json({ error: 'templateId is required' })

    const toList = normalizeList(to)
    if (!toList.length) return res.status(400).json({ error: 'to is required' })
    const fromAddress = from || DEFAULT_FROM
    if (!fromAddress) return res.status(400).json({ error: 'from is required' })

    const template = templateLookup.get(templateId)
    if (!template) return res.status(400).json({ error: 'unknown templateId' })

    if (ALLOWED.size && !ALLOWED.has(template.id) && !ALLOWED.has(template.key)) {
      return res.status(400).json({ error: 'template not allowed' })
    }

    const props = { ...variables }
    const resolvedSubject =
      subject ||
      (typeof template.subject === 'function'
        ? template.subject(props as never)
        : template.subject)

    const ccList = normalizeList(cc)
    const bccList = normalizeList(bcc)

    // Render React component to HTML
    const reactElement = template.component(props as never)
    const html = await render(reactElement)

    const sendResult = await resend.emails.send({
      from: fromAddress,
      to: toList,
      cc: ccList.length ? ccList : undefined,
      bcc: bccList.length ? bccList : undefined,
      reply_to: replyTo,
      subject: resolvedSubject,
      html: html,
    })

    return res.status(202).json(sendResult)
  } catch (err: any) {
    console.error(err)
    return res.status(500).json({ error: err?.message || 'send failed' })
  }
})

app.listen(PORT, () => {
  console.log(`[email-service] listening on ${PORT}`)
})
