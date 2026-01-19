export const config = {
  resendApiKey: process.env.RESEND_API_KEY,
  resendFrom: process.env.RESEND_FROM,
  allowedTemplates: process.env.ALLOWED_TEMPLATES,
  rabbitmqUrl: process.env.RABBITMQ_URL,
  rabbitmqExchange: process.env.RABBITMQ_EXCHANGE,
  rabbitmqQueue: process.env.RABBITMQ_EMAIL_QUEUE,
  rabbitmqRoutingKey: process.env.RABBITMQ_EMAIL_ROUTING_KEY,
  rabbitmqPrefetch: process.env.RABBITMQ_PREFETCH,
  rabbitmqReconnectMs: process.env.RABBITMQ_RECONNECT_MS,
  rabbitmqRequeueOnError: process.env.RABBITMQ_REQUEUE_ON_ERROR,
}
