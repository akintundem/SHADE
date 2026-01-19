# Shade AI Service

AI-powered microservice for event planning features including intelligent image generation for event covers.

## Features

- **Event Cover Image Generation**: Generate stunning, style-appropriate cover images for events using OpenAI's latest GPT Image 1.5 model
- **Intelligent Prompt Engineering**: Uses LLM to create optimized prompts based on event details
- **Style-Aware Generation**: Automatically adapts to event types (party, conference, professional, trendy)
- **FastAPI**: Modern, fast Python web framework
- **Docker Ready**: Containerized for easy deployment

## API Endpoints

### Health Check
```
GET /health
```

### Generate Cover Image
```
POST /generate-cover-image
Content-Type: application/json
x-ai-secret: <secret> (or Authorization: Bearer <Cognito JWT>)

{
  "event_name": "Summer Music Festival",
  "description": "A vibrant outdoor music festival",
  "event_type": "PARTY",
  "location": "Central Park, NYC",
  "date": "July 15, 2024",
  "style": "party",  // optional: party, conference, professional, sick
  "width": 1024,    // optional, default: 1024
  "height": 1024    // optional, default: 1024
}
```

**Response:**
```json
{
  "success": true,
  "image_url": "https://...",
  "image_base64": null,  // or base64 if RETURN_BASE64=true
  "prompt_used": "A vibrant, energetic party scene..."
}
```

### Generate Cover Image (Base64)
```
POST /generate-cover-image-v2
```
Same request format, but always returns image as base64.

### Generate Event Cover Image (Mobile-Optimized)
```
POST /generate-event-cover-image
Content-Type: application/json
x-ai-secret: <secret> (or Authorization: Bearer <Cognito JWT>)

{
  "name": "Summer Music Festival",
  "description": "A vibrant outdoor music festival featuring top artists",
  "eventType": "FESTIVAL",
  "theme": "Summer vibes and music celebration",
  "objectives": "Bring music lovers together for an unforgettable experience",
  "targetAudience": "Music enthusiasts aged 18-35",
  "hashtag": "#SummerFest2024",
  "brandingGuidelines": "Vibrant colors, modern design, music-themed",
  "venue": {
    "city": "New York",
    "state": "NY",
    "country": "United States"
  },
  "startDateTime": "2024-07-15T18:00:00",
  "width": 1024,
  "height": 1024,
  "variationSeed": 12345  // optional: for consistent variation
}
```

**Response:**
```json
{
  "success": true,
  "image_url": "https://...",
  "image_base64": "iVBORw0KGgoAAAANS...",  // Always included for S3 upload
  "prompt_used": "Create a stunning event cover image..."
}
```

**Features:**
- Uses comprehensive event data for better image generation
- Adds randomness for visual variation (different styles, colors, compositions)
- Returns base64 by default for easy S3 upload from mobile
- Adapts to different event types automatically
- Optional `variationSeed` for consistent variation when regenerating

## Configuration

### Environment Variables

- `PORT`: Service port (default: 8000)
- `AI_SERVICE_SECRET`: Secret for service authentication
- `AI_COGNITO_ISSUER`: Cognito issuer (e.g., `https://cognito-idp.us-east-2.amazonaws.com/us-east-2_AbcdefGhi`)
- `AI_COGNITO_AUDIENCE`: Cognito app client ID to validate `aud`
- `AI_COGNITO_JWKS_URL`: Optional override for JWKS URL (defaults to `<issuer>/.well-known/jwks.json`)
- `OPENAI_API_KEY`: OpenAI API key (required)
- `OPENAI_CHAT_MODEL`: Model for prompt refinement (default: gpt-4-turbo-preview)
- `OPENAI_IMAGE_MODEL`: Model for image generation (default: gpt-image-1.5, falls back to dall-e-3)
- `RETURN_BASE64`: Return images as base64 instead of URL (default: false)

### Local Development

1. Create virtual environment:
```bash
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
```

2. Create `.env` file:
```bash
cp .env.example .env
# Edit .env and add your OPENAI_API_KEY
```

3. Install dependencies:
```bash
pip install -r requirements.txt
```

4. Run the service:
```bash
uvicorn main:app --reload --port 8000
```

5. Test it:
```bash
# Health check
curl http://localhost:8000/health

# Generate event cover image (returns base64 for S3 upload)
curl -X POST http://localhost:8000/generate-event-cover-image \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Summer Music Festival 2024",
    "description": "A vibrant outdoor music festival",
    "eventType": "FESTIVAL",
    "theme": "Summer vibes",
    "width": 1024,
    "height": 1024
  }' | jq .

# Full example with all fields
curl -X POST http://localhost:8000/generate-event-cover-image \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Tech Conference 2024",
    "description": "Annual technology conference featuring industry leaders",
    "eventType": "CONFERENCE",
    "theme": "Innovation and Technology",
    "objectives": "Bring tech professionals together",
    "targetAudience": "Developers, engineers, tech leaders",
    "hashtag": "#TechConf2024",
    "venue": {
      "city": "San Francisco",
      "state": "CA",
      "country": "United States"
    },
    "startDateTime": "2024-06-15T09:00:00",
    "width": 1024,
    "height": 1024
  }' | jq .
```

### Docker

```bash
docker build -t shade-ai-service .
docker run -p 8000:8000 -e OPENAI_API_KEY=sk-... shade-ai-service
```

## Integration with Java App

The Java app can call this service via HTTP:

```java
RestTemplate restTemplate = new RestTemplate();
HttpHeaders headers = new HttpHeaders();
headers.setContentType(MediaType.APPLICATION_JSON);
headers.add("x-ai-secret", aiServiceSecret);

ImageGenerationRequest request = new ImageGenerationRequest(
    eventName, description, eventType, location, date, style
);

HttpEntity<ImageGenerationRequest> entity = new HttpEntity<>(request, headers);
ImageGenerationResponse response = restTemplate.postForObject(
    aiServiceUrl + "/generate-cover-image",
    entity,
    ImageGenerationResponse.class
);
```

## Image Generation Styles

- **party**: Vibrant, energetic, colorful, celebration mood
- **conference**: Professional, modern, tech-forward, corporate elegance
- **professional**: Clean, educational, business-focused
- **sick** (default): Trendy, eye-catching, Instagram-worthy, bold colors

## Future Enhancements

- Support for multiple image providers (Midjourney, Stable Diffusion)
- Image editing and customization
- Batch generation
- Caching for similar events
- AI-powered event recommendations
- Natural language event planning assistance
