const amqp = require("amqplib");
const admin = require("firebase-admin");
const config = require("./config");
const fs = require("fs");
const path = require("path");

if (!config.rabbitmqUrl) throw new Error("RABBITMQ_URL is required");
if (!config.rabbitmqExchange) throw new Error("RABBITMQ_EXCHANGE is required");
if (!config.rabbitmqQueue) throw new Error("RABBITMQ_PUSH_QUEUE is required");
if (!config.rabbitmqRoutingKey) throw new Error("RABBITMQ_PUSH_ROUTING_KEY is required");
if (!config.rabbitmqPrefetch) throw new Error("RABBITMQ_PREFETCH is required");
if (!config.rabbitmqReconnectMs) throw new Error("RABBITMQ_RECONNECT_MS is required");
if (!config.firebaseKeyPath) throw new Error("FIREBASE_SERVICE_ACCOUNT_KEY_PATH is required");
if (!config.firebaseProjectId) throw new Error("FIREBASE_PROJECT_ID is required");

const rabbitmqPrefetch = Number(config.rabbitmqPrefetch);
const rabbitmqReconnectMs = Number(config.rabbitmqReconnectMs);
/** Max tokens per push message to align with Java producer and provider limits. */
const MAX_PUSH_TOKENS_PER_MESSAGE = 500;
/** Maximum delivery attempts before a message is dead-lettered. */
const MAX_RETRIES = 3;
const DLX_EXCHANGE = "dlx.notifications";
const DLX_PUSH_QUEUE = "dlq.push";

let firebaseApp;

function getFirebaseApp() {
  if (firebaseApp) return firebaseApp;

  const resolvedPath = path.resolve(config.firebaseKeyPath);
  if (!fs.existsSync(resolvedPath)) {
    throw new Error(`Firebase service account key not found at ${resolvedPath}`);
  }

  const serviceAccount = require(resolvedPath);
  firebaseApp = admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
    projectId: config.firebaseProjectId,
  });

  return firebaseApp;
}

function requestError(message) {
  const err = new Error(message);
  err.statusCode = 400;
  throw err;
}

function resolvePushRequest(payload) {
  const { to, title, body, data } = payload || {};
  const recipients = Array.isArray(to) ? to : to ? [to] : [];
  if (!recipients.length) {
    requestError("`to` (device token or list) is required");
  }
  if (recipients.length > MAX_PUSH_TOKENS_PER_MESSAGE) {
    requestError(`at most ${MAX_PUSH_TOKENS_PER_MESSAGE} tokens per message`);
  }
  if (title && title.length > 500) requestError("title too long");
  if (body && body.length > 2000) requestError("body too long");
  if (data && typeof data === "object") {
    const keys = Object.keys(data);
    if (keys.length > 20) requestError("data has too many keys");
    for (const k of keys) {
      if (String(data[k]).length > 500) requestError("data value too long");
    }
  }
  return { recipients, title, body, data };
}

function normalizeData(data) {
  if (!data || typeof data !== "object") return undefined;
  const entries = Object.entries(data).map(([key, value]) => {
    if (value === null || value === undefined) return [key, ""];
    if (typeof value === "string") return [key, value];
    return [key, JSON.stringify(value)];
  });

  return entries.length ? Object.fromEntries(entries) : undefined;
}

async function sendPush(payload) {
  const { recipients, title, body, data } = resolvePushRequest(payload);
  if (!title && !body && (!data || Object.keys(data).length === 0)) {
    requestError("payload requires title, body, or data");
  }

  const app = getFirebaseApp();
  const notification =
    title || body
      ? {
          ...(title ? { title } : {}),
          ...(body ? { body } : {}),
        }
      : undefined;
  const normalizedData = normalizeData(data);

  if (recipients.length === 1) {
    const messageId = await admin.messaging(app).send({
      token: recipients[0],
      notification,
      data: normalizedData,
    });

    return {
      success: true,
      messageId,
      delivered: 1,
    };
  }

  const response = await admin.messaging(app).sendEachForMulticast({
    tokens: recipients,
    notification,
    data: normalizedData,
  });

  return {
    success: response.failureCount === 0,
    delivered: response.successCount,
    failed: response.failureCount,
  };
}

async function startRabbitConsumer() {
  try {
    const connection = await amqp.connect(config.rabbitmqUrl);
    connection.on("error", (err) => {
      console.error("[rabbitmq] connection error:", err);
      process.exit(1);
    });
    connection.on("close", () => {
      setTimeout(startRabbitConsumer, rabbitmqReconnectMs);
    });

    const channel = await connection.createChannel();

    // Assert dead-letter exchange and queue first
    await channel.assertExchange(DLX_EXCHANGE, "direct", { durable: true });
    await channel.assertQueue(DLX_PUSH_QUEUE, { durable: true });
    await channel.bindQueue(DLX_PUSH_QUEUE, DLX_EXCHANGE, "dead.push");

    await channel.assertExchange(config.rabbitmqExchange, "direct", { durable: true });
    await channel.assertQueue(config.rabbitmqQueue, {
      durable: true,
      arguments: {
        "x-dead-letter-exchange": DLX_EXCHANGE,
        "x-dead-letter-routing-key": "dead.push",
      },
    });
    await channel.bindQueue(
      config.rabbitmqQueue,
      config.rabbitmqExchange,
      config.rabbitmqRoutingKey
    );
    channel.prefetch(rabbitmqPrefetch);

    await channel.consume(config.rabbitmqQueue, async (msg) => {
      if (!msg) return;

      let payload;
      try {
        payload = JSON.parse(msg.content.toString());
      } catch (err) {
        // Unparseable message — dead-letter immediately, do not requeue
        channel.nack(msg, false, false);
        return;
      }

      try {
        await sendPush(payload);
        channel.ack(msg);
      } catch (err) {
        if (err?.statusCode === 400) {
          // Validation error — message is permanently bad, dead-letter it
          channel.nack(msg, false, false);
          return;
        }
        // Transient error — track retry count
        const headers = msg.properties.headers || {};
        const retryCount = (headers["x-retry-count"] || 0) + 1;
        if (retryCount >= MAX_RETRIES) {
          console.error(`[push] message failed after ${MAX_RETRIES} attempts, dead-lettering`);
          channel.nack(msg, false, false);
        } else {
          // Republish with incremented retry count rather than requeue to head
          channel.ack(msg);
          channel.publish(
            config.rabbitmqExchange,
            config.rabbitmqRoutingKey,
            msg.content,
            { headers: { ...headers, "x-retry-count": retryCount }, persistent: true }
          );
        }
      }
    });

  } catch (err) {
    setTimeout(startRabbitConsumer, rabbitmqReconnectMs);
  }
}

startRabbitConsumer();
