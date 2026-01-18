require("dotenv").config();
const amqp = require("amqplib");
const admin = require("firebase-admin");
const fs = require("fs");
const path = require("path");
const RABBITMQ_URL = process.env.RABBITMQ_URL || "amqp://guest:guest@rabbitmq:5672";
const RABBITMQ_EXCHANGE = process.env.RABBITMQ_EXCHANGE || "notifications";
const RABBITMQ_QUEUE = process.env.RABBITMQ_PUSH_QUEUE || "push.jobs";
const RABBITMQ_ROUTING_KEY = process.env.RABBITMQ_PUSH_ROUTING_KEY || "push.send";
const RABBITMQ_PREFETCH = parseInt(process.env.RABBITMQ_PREFETCH || "10", 10);
const RABBITMQ_RECONNECT_MS = parseInt(process.env.RABBITMQ_RECONNECT_MS || "5000", 10);
const RABBITMQ_REQUEUE_ON_ERROR = process.env.RABBITMQ_REQUEUE_ON_ERROR === "true";
const FIREBASE_KEY_PATH =
  process.env.FIREBASE_SERVICE_ACCOUNT_KEY_PATH || process.env.GOOGLE_APPLICATION_CREDENTIALS;
const FIREBASE_PROJECT_ID = process.env.FIREBASE_PROJECT_ID || "";

let firebaseApp;

function getFirebaseApp() {
  if (firebaseApp) return firebaseApp;
  if (!FIREBASE_KEY_PATH) {
    throw new Error("FIREBASE_SERVICE_ACCOUNT_KEY_PATH is required");
  }

  const resolvedPath = path.resolve(FIREBASE_KEY_PATH);
  if (!fs.existsSync(resolvedPath)) {
    throw new Error(`Firebase service account key not found at ${resolvedPath}`);
  }

  const serviceAccount = require(resolvedPath);
  firebaseApp = admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
    projectId: FIREBASE_PROJECT_ID || serviceAccount.project_id,
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
    const connection = await amqp.connect(RABBITMQ_URL);
    connection.on("error", () => {});
    connection.on("close", () => {
      setTimeout(startRabbitConsumer, RABBITMQ_RECONNECT_MS);
    });

    const channel = await connection.createChannel();
    await channel.assertExchange(RABBITMQ_EXCHANGE, "direct", { durable: true });
    await channel.assertQueue(RABBITMQ_QUEUE, { durable: true });
    await channel.bindQueue(RABBITMQ_QUEUE, RABBITMQ_EXCHANGE, RABBITMQ_ROUTING_KEY);
    channel.prefetch(RABBITMQ_PREFETCH);

    await channel.consume(RABBITMQ_QUEUE, async (msg) => {
      if (!msg) return;
      const jobId = msg.properties.messageId || msg.properties.correlationId || "unknown";

      let payload;
      try {
        payload = JSON.parse(msg.content.toString());
      } catch (err) {
        channel.ack(msg);
        return;
      }

      try {
        await sendPush(payload);
        channel.ack(msg);
      } catch (err) {
        if (err?.statusCode === 400) {
          channel.ack(msg);
          return;
        }
        channel.nack(msg, false, RABBITMQ_REQUEUE_ON_ERROR);
      }
    });

  } catch (err) {
    setTimeout(startRabbitConsumer, RABBITMQ_RECONNECT_MS);
  }
}

startRabbitConsumer();
