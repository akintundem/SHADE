const express = require("express");
const { v4: uuid } = require("uuid");

const app = express();
app.use(express.json());

const PORT = process.env.PORT || 3100;
const SHARED_SECRET = process.env.PUSH_SERVICE_SECRET || "";

function unauthorized(res, message) {
  return res.status(401).json({ success: false, error: message || "Unauthorized" });
}

app.get("/health", (_req, res) => {
  res.json({ status: "ok", service: "push-service" });
});

app.post("/send-push", (req, res) => {
  if (SHARED_SECRET) {
    const headerSecret = req.header("x-push-secret");
    if (headerSecret !== SHARED_SECRET) {
      return unauthorized(res, "Invalid push secret");
    }
  }

  const { to, title, body, data } = req.body || {};
  const recipients = Array.isArray(to) ? to : to ? [to] : [];
  if (!recipients.length) {
    return res.status(400).json({ success: false, error: "`to` (device token or list) is required" });
  }

  // Stubbed response; in real deployments wire up Firebase/Admin SDK here.
  const messageId = uuid();
  console.log(`[push] sending to=${recipients.length} title="${title || ""}" body="${body || ""}" data=${JSON.stringify(data || {})}`);

  return res.json({
    success: true,
    messageId,
    delivered: recipients.length,
  });
});

app.use((err, _req, res, _next) => {
  console.error("[push] Unhandled error", err);
  res.status(500).json({ success: false, error: "Internal server error" });
});

app.listen(PORT, () => {
  console.log(`[push] service listening on :${PORT}`);
});
