"""
Utilities for interacting with OpenAI through LangChain for the event planner.

This module currently exposes a thin wrapper around a LangChain `ChatOpenAI`
client so the rest of the application has a single place that owns the
configuration. It also provides a placeholder `create_event` flow that
collects details from the user and prints them, ready to be wired into a
front-end chat experience later on.
"""

from dataclasses import dataclass
from typing import Callable, Optional
import os

from langchain_core.messages import HumanMessage
from langchain_openai import ChatOpenAI
import json


@dataclass
class EventDetails:
    """Plain container for the minimal event metadata we capture."""

    name: str
    type: str
    date: str


class EventAssistant:
    """
    Thin wrapper around LangChain's `ChatOpenAI`.

    The assistant can be used as a chatting surface and, for now, supports a
    simple `create_event` flow that just echoes the collected values. When the
    richer create-event implementation is ready, this class should be extended
    to persist the data or forward it to the Java backend.
    """

    def __init__(
        self,
        *,
        model_name: str = "gpt-4o-mini",
        temperature: float = 0.1,
        openai_api_key: Optional[str] = None,
    ) -> None:
        api_key = openai_api_key or os.getenv("OPENAI_API_KEY")
        if not api_key:
            raise ValueError(
                "OPENAI_API_KEY is not set. Update the environment before using the assistant."
            )

        # LangChain reads the key from the environment, so we ensure it is set.
        os.environ["OPENAI_API_KEY"] = api_key

        self._chat = ChatOpenAI(model=model_name, temperature=temperature)

    def chat(self, user_message: str) -> str:
        """Fallback free-form chat using OpenAI. Not used in the minimal flow."""
        response = self._chat.invoke([HumanMessage(content=user_message)])
        return response.content

    @staticmethod
    def prompt_for_event_details() -> str:
        return "Create an event. I need the event name, type, and date."

    @staticmethod
    def confirmation_message(details: EventDetails) -> str:
        return (
            "Tool: EventPlanRequest\n"
            "Event created!\n"
            f"  Name: {details.name}\n"
            f"  Type: {details.type}\n"
            f"  Date: {details.date}"
        )

    def extract_event_details(self, text: str) -> Optional[EventDetails]:
        """
        Ask the model to extract event fields from freeform text.

        Returns EventDetails if name+type+date are present, otherwise None.
        """
        system_prompt = (
            "You are an information extractor. Given a user's message about an event, "
            "return ONLY a compact JSON object with keys: name, type, date. "
            "If a field is unknown, set it to an empty string. Do not add commentary."
        )
        user = (
            "Message: " + text +
            "\nReturn JSON like: {\"name\":\"...\",\"type\":\"...\",\"date\":\"YYYY-MM-DD\"}"
        )
        resp = self._chat.invoke([
            HumanMessage(content=system_prompt),
            HumanMessage(content=user),
        ])
        raw = resp.content.strip()
        try:
            data = json.loads(raw)
        except json.JSONDecodeError:
            return None

        name = str(data.get("name", "")).strip()
        etype = str(data.get("type", "")).strip()
        date = str(data.get("date", "")).strip()
        if name and etype and date:
            return EventDetails(name=name, type=etype, date=date)
        return None

    def handle_event_if_present(self, text: str) -> Optional[EventDetails]:
        """
        If the text contains event info, extract and print it. Returns details if captured.
        """
        details = self.extract_event_details(text)
        if details:
            self.create_event(details)
        return details

    def create_event(
        self,
        event: EventDetails,
        output_callback: Callable[[str], None] = print,
    ) -> EventDetails:
        """Placeholder create-event hook that simply prints to the terminal."""
        output_callback(
            (
                "[assistant] Tool: EventPlanRequest\n"
                "[assistant] Captured event details:\n"
                f"  Name: {event.name}\n"
                f"  Type: {event.type}\n"
                f"  Date: {event.date}"
            )
        )
        return event

    def prompt_and_create_event(
        self,
        input_provider: Callable[[str], str] = input,
        output_callback: Callable[[str], None] = print,
    ) -> EventDetails:
        """Interactive CLI helper useful for quick manual testing."""
        name = input_provider("Event Name: ").strip()
        event_type = input_provider("Event Type: ").strip()
        date = input_provider("Event Date: ").strip()

        event = EventDetails(name=name, type=event_type, date=date)
        return self.create_event(event, output_callback=output_callback)


def chat_loop() -> None:
    """
    Simple CLI loop so the assistant can be exercised manually for quick tests.
    """
    assistant = EventAssistant()
    print("Event Assistant ready. Type 'quit' to exit.")
    print(EventAssistant.prompt_for_event_details())
    while True:
        message = input("You: ").strip()
        if message.lower() in {"quit", "exit"}:
            print("Goodbye!")
            break
        details = assistant.handle_event_if_present(message)
        reply = (
            assistant.confirmation_message(details)
            if details
            else EventAssistant.prompt_for_event_details()
        )
        print(f"Assistant: {reply}")


if __name__ == "__main__":
    chat_loop()
