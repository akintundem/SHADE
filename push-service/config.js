module.exports = {
  rabbitmqUrl: process.env.RABBITMQ_URL,
  rabbitmqExchange: process.env.RABBITMQ_EXCHANGE,
  rabbitmqQueue: process.env.RABBITMQ_PUSH_QUEUE,
  rabbitmqRoutingKey: process.env.RABBITMQ_PUSH_ROUTING_KEY,
  rabbitmqPrefetch: process.env.RABBITMQ_PREFETCH,
  rabbitmqReconnectMs: process.env.RABBITMQ_RECONNECT_MS,
  rabbitmqRequeueOnError: process.env.RABBITMQ_REQUEUE_ON_ERROR,
  firebaseKeyPath: process.env.FIREBASE_SERVICE_ACCOUNT_KEY_PATH,
  firebaseProjectId: process.env.FIREBASE_PROJECT_ID,
};
