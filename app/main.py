import logging
from typing import Literal

from fastapi import FastAPI, HTTPException
from fastapi.responses import HTMLResponse
from pydantic import BaseModel, Field
from dotenv import load_dotenv
from pathlib import Path

# Ensure environment variables from assistant/.env are loaded before using the assistant
ROOT_DIR = Path(__file__).resolve().parent.parent
load_dotenv(ROOT_DIR / "assistant" / ".env")

from assistant import EventAssistant, EventDetails

logger = logging.getLogger("event_assistant.api")

app = FastAPI(
    title="Event Planner Assistant",
    description="Chat and event endpoints backed by LangChain + OpenAI.",
)
assistant = EventAssistant()


class ChatRequest(BaseModel):
    message: str = Field(..., description="User message for the assistant.")


class ChatResponse(BaseModel):
    reply: str = Field(..., description="Assistant response.")


@app.post("/chat", response_model=ChatResponse, tags=["chat"])
async def chat_endpoint(request: ChatRequest) -> ChatResponse:
    try:
        details = assistant.handle_event_if_present(request.message)
        reply = (
            assistant.confirmation_message(details)
            if details
            else assistant.prompt_for_event_details()
        )
    except Exception as exc:
        logger.exception("Chat invocation failed")
        raise HTTPException(status_code=500, detail=str(exc)) from exc
    return ChatResponse(reply=reply)


@app.get("/", response_class=HTMLResponse, tags=["ui"])
async def chat_ui() -> str:
    # Minimal inlined HTML/JS chat client posting to /chat
    return """
<!doctype html>
<html lang=\"en\">
  <head>
    <meta charset=\"utf-8\" />
    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />
    <title>Event Assistant Chat</title>
    <style>
      body { font-family: system-ui, -apple-system, Segoe UI, Roboto, sans-serif; margin: 0; background: #0b1020; color: #e8ecf1; }
      header { padding: 16px; background: #111833; box-shadow: 0 1px 0 #1f2947; position: sticky; top: 0; }
      main { max-width: 840px; margin: 0 auto; padding: 16px; }
      #log { border: 1px solid #1f2947; border-radius: 8px; padding: 12px; min-height: 320px; background:#0e1530; }
      .msg { margin: 8px 0; padding: 8px 10px; border-radius: 8px; max-width: 80%; white-space: pre-wrap; }
      .user { background: #1a2458; margin-left: auto; }
      .assistant { background: #0a1d3a; border: 1px solid #1b2b55; }
      form { display: flex; gap: 8px; margin-top: 12px; }
      input, button { font: inherit; }
      #message { flex: 1; padding: 10px; border-radius: 8px; border: 1px solid #1f2947; background: #0e1530; color: #e8ecf1; }
      button { padding: 10px 14px; border-radius: 8px; border: 1px solid #2a3a6a; background: #213264; color: #e8ecf1; cursor: pointer; }
      button:disabled { opacity: .6; cursor: not-allowed; }
    </style>
  </head>
  <body>
    <header>
      <strong>Event Planner Assistant</strong>
    </header>
    <main>
      <div id=\"log\"></div>
      <form id=\"chat\">
        <input id=\"message\" name=\"message\" placeholder=\"e.g. Create a product launch on 2025-04-01\" autocomplete=\"off\" required />
        <button id=\"send\" type=\"submit\">Send</button>
      </form>
    </main>
    <script>
      const log = document.getElementById('log');
      const form = document.getElementById('chat');
      const input = document.getElementById('message');
      const sendBtn = document.getElementById('send');

      const INTRO = 'Create an event. I need the event name, type, and date.';

      function add(role, text){
        const div = document.createElement('div');
        div.className = 'msg ' + (role === 'user' ? 'user' : 'assistant');
        div.textContent = text;
        log.appendChild(div);
        log.scrollTop = log.scrollHeight;
      }

      add('assistant', INTRO);

      form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const text = input.value.trim();
        if(!text) return;
        input.value = '';
        add('user', text);
        sendBtn.disabled = true;
        try {
          const res = await fetch('/chat', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ message: text })
          });
          if(!res.ok){
            const err = await res.text();
            add('assistant', 'Error: ' + err);
          } else {
            const data = await res.json();
            add('assistant', data.reply || INTRO);
          }
        } catch (err) {
          add('assistant', 'Network error: ' + err);
        } finally {
          sendBtn.disabled = false;
          input.focus();
        }
      });
    </script>
  </body>
  </html>
    """


class EventRequest(BaseModel):
    name: str = Field(..., description="Event display name.")
    type: str = Field(..., description="Kind of event, e.g., conference, meetup.")
    date: str = Field(..., description="Event date, ISO 8601 preferred.")


EventResponseStatus = Literal["captured"]


class EventResponse(BaseModel):
    status: EventResponseStatus = Field(
        default="captured",
        description="Current handling status for the event submission.",
    )
    event: EventRequest


@app.post("/events", response_model=EventResponse, tags=["events"])
async def create_event_endpoint(request: EventRequest) -> EventResponse:
    event_details = EventDetails(
        name=request.name,
        type=request.type,
        date=request.date,
    )
    logger.info(
        "Captured event submission name='%s' type='%s' date='%s'",
        event_details.name,
        event_details.type,
        event_details.date,
    )
    return EventResponse(event=request)
