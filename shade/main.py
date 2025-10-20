"""Main web application for Event Planner AI Tools."""

import os
import sys
from pathlib import Path
from typing import Optional
from fastapi import FastAPI, HTTPException
from fastapi.responses import HTMLResponse
from pydantic import BaseModel
from dotenv import load_dotenv

# Add current directory to path
sys.path.insert(0, str(Path(__file__).parent))

from event_tool import EventTool
from weather_tool import WeatherTool
from time_tool import TimeTool

# Load environment variables
load_dotenv()

app = FastAPI(title="Event Planner AI Assistant", description="AI-powered event planning and weather checking tools")

# Initialize tools
tools = [
    EventTool(),
    WeatherTool()
]

# Simple conversation state management
conversation_state = {
    "active_tool": None,
    "tool_instance": None
}

def reset_conversation_state():
    """Reset the conversation state."""
    global conversation_state
    conversation_state["active_tool"] = None
    conversation_state["tool_instance"] = None


class ChatRequest(BaseModel):
    message: str


class ChatResponse(BaseModel):
    reply: str
    tool_used: str
    data: Optional[dict] = None
    show_chips: Optional[bool] = False


def process_message(message: str) -> ChatResponse:
    """Process message using available tools with conversation state management."""
    global conversation_state
    
    # If there's an active tool, try to continue the conversation with it
    if conversation_state["active_tool"] and conversation_state["tool_instance"]:
        try:
            result = conversation_state["tool_instance"].process(message)
            
            # Check if the conversation is complete
            if result.get("conversation_state") == "complete":
                # Reset conversation state
                conversation_state["active_tool"] = None
                conversation_state["tool_instance"] = None
            elif result.get("conversation_state") in ["collecting_required", "collecting_optional", "collecting_additional"]:
                # Keep the tool active for next message
                pass
            else:
                # Tool finished or error, reset state
                conversation_state["active_tool"] = None
                conversation_state["tool_instance"] = None
            
            return ChatResponse(
                reply=result["message"],
                tool_used=conversation_state["active_tool"] or "None",
                data=result.get("data"),
                show_chips=result.get("show_chips", False)
            )
        except Exception as e:
            # Reset on error
            conversation_state["active_tool"] = None
            conversation_state["tool_instance"] = None
            return ChatResponse(
                reply=f"❌ Error with {conversation_state['active_tool']}: {str(e)}",
                tool_used=conversation_state["active_tool"] or "None",
                data=None
            )
    
    # No active tool, try to find a new tool to handle the message
    for tool_class in [EventTool, WeatherTool, TimeTool]:
        tool = tool_class()
        if tool.can_handle(message):
            try:
                result = tool.process(message)
                
                # If the tool starts a conversation, make it active
                if result.get("conversation_state") in ["collecting_required", "collecting_optional", "collecting_additional"]:
                    conversation_state["active_tool"] = tool.__class__.__name__
                    conversation_state["tool_instance"] = tool
                
                return ChatResponse(
                    reply=result["message"],
                    tool_used=tool.__class__.__name__,
                    data=result.get("data"),
                    show_chips=result.get("show_chips", False)
                )
            except Exception as e:
                return ChatResponse(
                    reply=f"Oops! I had a little hiccup there. Let me try that again - could you repeat what you just said? 😊",
                    tool_used=tool.__class__.__name__,
                    data=None,
                    show_chips=False
                )
    
    return ChatResponse(
        reply="Hey there, gorgeous! I'm Shade, and I'm absolutely THRILLED to be your personal event planning superstar! 🎉✨ I'm here to help you create ONE absolutely INCREDIBLE event that will have everyone talking for years! I can help with every single detail, check the weather, verify timing, and make sure everything is absolutely PERFECT! Just say 'Create an event' and let's make some magic happen! 🎊",
        tool_used="None",
        data=None,
        show_chips=False
    )


@app.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest):
    """Chat endpoint."""
    try:
        return process_message(request.message)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/reset")
async def reset_conversation():
    """Reset conversation state."""
    reset_conversation_state()
    return {"message": "Conversation state reset successfully"}


@app.get("/", response_class=HTMLResponse)
async def home():
    """Simple HTML interface for testing."""
    return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Shade AI Tools</title>
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            max-width: 800px;
            margin: 0 auto;
            padding: 20px;
            background: #f5f5f5;
        }
        .container {
            background: white;
            border-radius: 10px;
            padding: 30px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        }
        h1 {
            color: #333;
            text-align: center;
            margin-bottom: 30px;
        }
        .chat-container {
            border: 1px solid #ddd;
            border-radius: 8px;
            height: 400px;
            overflow-y: auto;
            padding: 15px;
            margin-bottom: 20px;
            background: #fafafa;
        }
        .message {
            margin: 10px 0;
            padding: 10px;
            border-radius: 8px;
            max-width: 80%;
        }
        .user {
            background: #007bff;
            color: white;
            margin-left: auto;
            text-align: right;
        }
        .assistant {
            background: #e9ecef;
            color: #333;
        }
        .tool-info {
            font-size: 0.8em;
            color: #666;
            margin-top: 5px;
        }
        .input-container {
            display: flex;
            gap: 10px;
        }
        input[type="text"] {
            flex: 1;
            padding: 12px;
            border: 1px solid #ddd;
            border-radius: 6px;
            font-size: 16px;
        }
        button {
            padding: 12px 24px;
            background: #007bff;
            color: white;
            border: none;
            border-radius: 6px;
            cursor: pointer;
            font-size: 16px;
        }
        button:hover {
            background: #0056b3;
        }
        button:disabled {
            background: #ccc;
            cursor: not-allowed;
        }
        .examples {
            margin-top: 20px;
            padding: 15px;
            background: #f8f9fa;
            border-radius: 6px;
        }
        .examples h3 {
            margin-top: 0;
            color: #495057;
        }
        .example {
            margin: 5px 0;
            padding: 5px 10px;
            background: white;
            border-radius: 4px;
            cursor: pointer;
            border: 1px solid #dee2e6;
        }
        .example:hover {
            background: #e9ecef;
        }
        
        .chips-container {
            margin-top: 10px;
            display: flex;
            flex-wrap: wrap;
            gap: 8px;
        }
        
        .chip {
            background: #007bff;
            color: white;
            padding: 6px 12px;
            border-radius: 20px;
            cursor: pointer;
            font-size: 14px;
            border: none;
            transition: all 0.2s ease;
        }
        
        .chip:hover {
            background: #0056b3;
            transform: translateY(-1px);
        }
        
        .chip:active {
            transform: translateY(0);
        }
        
        .chip.yes {
            background: #28a745;
        }
        
        .chip.yes:hover {
            background: #1e7e34;
        }
        
        .chip.no {
            background: #6c757d;
        }
        
        .chip.no:hover {
            background: #545b62;
        }
        
        .chip.view {
            background: #17a2b8;
        }
        
        .chip.view:hover {
            background: #138496;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>✨ Meet Shade - Your Event Planning Superstar! ✨</h1>
        
        <div class="chat-container" id="chatContainer">
            <div class="message assistant">
                <div>Hey there, gorgeous! I am Shade, and I am absolutely THRILLED to be your personal event planning superstar! 🎉✨</div>
                <div>I can help you with:</div>
                <div>• Creating ONE absolutely INCREDIBLE event that will have everyone talking for years!</div>
                <div>• Planning every single detail to absolute perfection</div>
                <div>• Checking the weather and timing to ensure your special day is flawless</div>
                <div>• Making recommendations that will make your event absolutely SPECTACULAR</div>
                <div>• Focusing on ONE event at a time for the most amazing results</div>
                <div>Just say "Create an event" and let's make some magic happen! 🎊</div>
            </div>
        </div>
        
        <div class="input-container">
            <input type="text" id="messageInput" placeholder="Ask me about events or weather..." />
            <button id="sendButton">Send</button>
            <button id="resetButton" style="background: #dc3545;">Reset</button>
        </div>
        
        <div class="examples">
            <h3>Let's get started! Try these:</h3>
            <div class="example" onclick="sendExample('Create an event')">
                🎉 Create an event (let's plan something amazing!)
            </div>
            <div class="example" onclick="sendExample('Create a conference called Tech Summit 2024')">
                🏢 Create a conference called Tech Summit 2024
            </div>
            <div class="example" onclick="sendExample('What is the weather like for outdoor events?')">
                🌤️ What's the weather like for outdoor events?
            </div>
            <div class="example" onclick="sendExample('Create a birthday party for my daughter')">
                🎂 Create a birthday party for my daughter
            </div>
            <div class="example" onclick="sendExample('done')">
                ✅ Finish current event (when you're ready!)
            </div>
        </div>
    </div>

    <script>
        // Global variables
        let chatContainer, messageInput, sendButton, resetButton;
        let isInitialized = false;

        // Initialize when DOM is ready
        function initializeApp() {
            if (isInitialized) return;
            
            console.log('Initializing Shade Chat App...');
            
            // Get elements
            chatContainer = document.getElementById('chatContainer');
            messageInput = document.getElementById('messageInput');
            sendButton = document.getElementById('sendButton');
            resetButton = document.getElementById('resetButton');

            // Verify all elements exist
            if (!chatContainer || !messageInput || !sendButton || !resetButton) {
                console.error('Missing required elements:', {
                    chatContainer: !!chatContainer,
                    messageInput: !!messageInput,
                    sendButton: !!sendButton,
                    resetButton: !!resetButton
                });
                return;
            }

            console.log('All elements found, setting up event listeners...');

            // Add message to chat
            function addMessage(content, isUser = false, showChips = false) {
                const messageDiv = document.createElement('div');
                messageDiv.className = `message ${isUser ? 'user' : 'assistant'}`;
                messageDiv.innerHTML = content;
                
                // Add chips if requested
                if (showChips && !isUser) {
                    const chipsContainer = document.createElement('div');
                    chipsContainer.className = 'chips-container';
                    
                    // Add common response chips
                    const chips = [
                        { text: 'Yes!', class: 'yes', value: 'yes' },
                        { text: 'No thanks', class: 'no', value: 'no' },
                        { text: 'Add description', class: '', value: 'Add a description' },
                        { text: 'Set capacity', class: '', value: 'Set capacity' },
                        { text: 'Add venue info', class: '', value: 'Add venue requirements' },
                        { text: 'View Events', class: 'view', value: 'Show my events' }
                    ];
                    
                    chips.forEach(chip => {
                        const chipButton = document.createElement('button');
                        chipButton.className = `chip ${chip.class}`;
                        chipButton.textContent = chip.text;
                        chipButton.onclick = () => {
                            messageInput.value = chip.value;
                            sendMessage();
                        };
                        chipsContainer.appendChild(chipButton);
                    });
                    
                    messageDiv.appendChild(chipsContainer);
                }
                
                chatContainer.appendChild(messageDiv);
                chatContainer.scrollTop = chatContainer.scrollHeight;
            }

            // Send message function
            async function sendMessage() {
                const message = messageInput.value.trim();
                if (!message) {
                    console.log('No message to send');
                    return;
                }

                console.log('Sending message:', message);

                // Add user message to chat
                addMessage(message, true);
                
                // Clear input immediately and show visual feedback
                messageInput.value = '';
                messageInput.placeholder = 'Sending message...';
                sendButton.disabled = true;
                sendButton.textContent = 'Sending...';

                try {
                    const response = await fetch('/chat', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                        },
                        body: JSON.stringify({ message: message })
                    });

                    if (!response.ok) {
                        throw new Error(`HTTP error! status: ${response.status}`);
                    }

                    const data = await response.json();
                    console.log('Received response:', data);
                    addMessage(data.reply, false, data.show_chips || false);
                } catch (error) {
                    console.error('Error sending message:', error);
                    addMessage(`Oops! I had trouble with that. Could you try again? 😊`, false);
                } finally {
                    sendButton.disabled = false;
                    sendButton.textContent = 'Send';
                    messageInput.placeholder = 'Ask me about events or weather...';
                    messageInput.focus();
                }
            }

            // Reset conversation
            async function resetConversation() {
                console.log('Resetting conversation...');
                sendButton.disabled = true;
                resetButton.disabled = true;
                resetButton.textContent = 'Resetting...';

                try {
                    const response = await fetch('/reset', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                        }
                    });
                    
                    if (response.ok) {
                        addMessage('✨ All set! I am ready to help you plan something new and amazing! What would you like to create? 🎉', false);
                    } else {
                        addMessage('❌ Oops! Something went wrong. Let me try again!', false);
                    }
                } catch (error) {
                    console.error('Reset error:', error);
                    addMessage(`❌ Error: ${error.message}`, false);
                } finally {
                    sendButton.disabled = false;
                    resetButton.disabled = false;
                    resetButton.textContent = 'Reset';
                }
            }

            // Send example message
            function sendExample(text) {
                console.log('Sending example:', text);
                messageInput.value = text;
                sendMessage();
            }

            // Event listeners
            sendButton.onclick = function(e) {
                e.preventDefault();
                console.log('Send button clicked');
                sendMessage();
            };

            resetButton.onclick = function(e) {
                e.preventDefault();
                console.log('Reset button clicked');
                resetConversation();
            };

            messageInput.onkeypress = function(e) {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    console.log('Enter key pressed');
                    sendMessage();
                }
            };

            // Make sendExample globally available
            window.sendExample = sendExample;

            isInitialized = true;
            console.log('Shade Chat App initialized successfully!');
        }

        // Initialize when DOM is ready
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', initializeApp);
        } else {
            initializeApp();
        }

        // Fallback initialization
        setTimeout(function() {
            if (!isInitialized) {
                console.log('Fallback initialization...');
                initializeApp();
            }
        }, 1000);
    </script>
</body>
</html>
    """


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
