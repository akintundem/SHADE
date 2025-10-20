# Shade AI Event Planner

This is the AI-powered event planning assistant with conversational tools.

## Setup

1. **Activate the virtual environment:**
   ```bash
   source activate.sh
   ```
   Or manually:
   ```bash
   source venv/bin/activate
   ```

2. **Install dependencies (if not already installed):**
   ```bash
   pip install -r requirements.txt
   ```

3. **Set up environment variables:**
   Run the setup script to create a `.env` file:
   ```bash
   python setup.py
   ```
   Then edit the `.env` file with your actual API key:
   ```
   OPENAI_API_KEY=your_openai_api_key_here
   ```
   Note: Weather tool uses mock data, so OpenWeather API key is optional.

## Running the Application

```bash
python main.py
```

Then visit `http://localhost:8000` in your browser.

## Features

- **Event Tool**: Conversational event creation with data extraction using OpenAI
- **Weather Tool**: Mock weather data for outdoor event planning
- **Web Interface**: Clean, modern web app for interaction

## Tools

- `event_tool.py`: Handles event creation and data extraction
- `weather_tool.py`: Handles weather-related queries
- `main.py`: FastAPI web application

## Usage

The system will:
1. Extract event data from conversations
2. When all required information is collected, it will print "Tool being called: CreateEvent" with the extracted data
3. Provide weather information for outdoor events
