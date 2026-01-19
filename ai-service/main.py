"""
AI Service for Event Planner
Handles AI-powered features including image generation for event covers
"""
import os
import base64
import io
import random
from typing import Optional, Dict, Any, List
from datetime import datetime, timedelta
from fastapi import FastAPI, HTTPException, Header, Request
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field
from openai import OpenAI
from dotenv import load_dotenv
import httpx
from jose import jwt, JWTError

# Load environment variables
load_dotenv()

app = FastAPI(
    title="Shade AI Service",
    description="AI-powered services for event planning including image generation",
    version="1.0.0"
)

# Configuration
PORT = int(os.getenv("PORT", "8000"))
# Kong → AI service shared secret (injected by Kong via x-ai-secret header)
SHARED_SECRET = os.getenv("AI_SERVICE_SECRET", "")
# Whether to require the x-ai-secret header for all requests (except /health)
REQUIRE_SECRET = os.getenv("AI_SERVICE_REQUIRE_SECRET", "true").lower() == "true"
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
COGNITO_ISSUER = os.getenv("AI_COGNITO_ISSUER") or os.getenv("COGNITO_ISSUER_URI") or ""
COGNITO_AUDIENCE = os.getenv("AI_COGNITO_AUDIENCE") or os.getenv("COGNITO_AUDIENCE") or ""
JWKS_URL = os.getenv("AI_COGNITO_JWKS_URL") or (f"{COGNITO_ISSUER}/.well-known/jwks.json" if COGNITO_ISSUER else "")
JWKS_CACHE_SECONDS = int(os.getenv("AI_JWKS_CACHE_SECONDS", "3600"))

# Validate required configuration on startup
if REQUIRE_SECRET and not SHARED_SECRET:
    print("[ai] WARNING: AI_SERVICE_SECRET is not set but REQUIRE_SECRET is true. "
          "All requests will require valid Cognito JWT instead.")

jwks_cache: Dict[str, Any] = {"keys": None, "expires_at": datetime.min}

# Initialize OpenAI client
openai_client = None
if OPENAI_API_KEY:
    openai_client = OpenAI(api_key=OPENAI_API_KEY)
else:
    print("[ai] WARNING: OPENAI_API_KEY not configured. Image generation will not work.")


async def fetch_jwks():
    """Fetch and cache JWKS from Cognito."""
    global jwks_cache
    now = datetime.utcnow()
    if jwks_cache.get("keys") and jwks_cache.get("expires_at") and now < jwks_cache["expires_at"]:
        return jwks_cache["keys"]

    if not JWKS_URL:
        raise HTTPException(status_code=500, detail="Cognito JWKS URL not configured")

    async with httpx.AsyncClient() as client:
        resp = await client.get(JWKS_URL, timeout=5)
        resp.raise_for_status()
        data = resp.json()
        jwks_cache = {
            "keys": data.get("keys", []),
            "expires_at": now + timedelta(seconds=JWKS_CACHE_SECONDS)
        }
        return jwks_cache["keys"]


async def verify_jwt_token(token: str) -> Dict[str, Any]:
    """Validate Cognito JWT using JWKS."""
    if not token:
        raise HTTPException(status_code=401, detail="Missing bearer token")
    keys = await fetch_jwks()
    try:
        header = jwt.get_unverified_header(token)
        kid = header.get("kid")
        key = next((k for k in keys if k.get("kid") == kid), None)
        if not key:
            raise HTTPException(status_code=401, detail="Unable to match signing key")

        options = {"verify_aud": bool(COGNITO_AUDIENCE)}
        claims = jwt.decode(
            token,
            key,
            algorithms=[key.get("alg", "RS256")],
            audience=COGNITO_AUDIENCE or None,
            issuer=COGNITO_ISSUER or None,
            options=options
        )
        return claims
    except JWTError as exc:
        raise HTTPException(status_code=401, detail=f"Invalid token: {exc}") from exc


# Request/Response Models
class ImageGenerationRequest(BaseModel):
    """Request model for image generation"""
    event_name: str = Field(..., description="Name of the event")
    description: Optional[str] = Field(None, description="Event description")
    event_type: Optional[str] = Field(None, description="Type of event (e.g., PARTY, CONFERENCE, WORKSHOP)")
    location: Optional[str] = Field(None, description="Event location")
    date: Optional[str] = Field(None, description="Event date")
    style: Optional[str] = Field(None, description="Desired style (sick, party, conference, professional)")
    width: Optional[int] = Field(1024, description="Image width in pixels")
    height: Optional[int] = Field(1024, description="Image height in pixels")


class ImageGenerationResponse(BaseModel):
    """Response model for image generation"""
    success: bool
    image_url: Optional[str] = None
    image_base64: Optional[str] = None
    error: Optional[str] = None
    prompt_used: Optional[str] = None


class VenueDTO(BaseModel):
    """Venue information with location details"""
    address: Optional[str] = None
    city: Optional[str] = None
    state: Optional[str] = None
    country: Optional[str] = None
    zipCode: Optional[str] = None
    latitude: Optional[float] = None
    longitude: Optional[float] = None
    googlePlaceId: Optional[str] = None
    googlePlaceData: Optional[str] = None


class EventCoverImageRequest(BaseModel):
    """Comprehensive request model for event cover image generation based on CreateEventRequest"""
    name: str = Field(..., description="Name of the event")
    description: Optional[str] = Field(None, description="Detailed description of the event")
    eventType: Optional[str] = Field(None, description="Type of event (CONFERENCE, PARTY, WORKSHOP, etc.)")
    theme: Optional[str] = Field(None, description="Event theme details")
    objectives: Optional[str] = Field(None, description="Event objectives")
    targetAudience: Optional[str] = Field(None, description="Target audience description")
    hashtag: Optional[str] = Field(None, description="Event hashtag")
    brandingGuidelines: Optional[str] = Field(None, description="Brand guidelines to follow")
    venue: Optional[VenueDTO] = Field(None, description="Venue information with location details")
    startDateTime: Optional[str] = Field(None, description="Start date and time of the event")
    width: Optional[int] = Field(1024, description="Image width in pixels")
    height: Optional[int] = Field(1024, description="Image height in pixels")
    variationSeed: Optional[int] = Field(None, description="Optional seed for consistent variation")


# Middleware for secret authentication
# Kong injects x-ai-secret header for all requests routed through /ai-service
@app.middleware("http")
async def verify_auth(request: Request, call_next):
    # Skip authentication for health endpoint
    if request.url.path == "/health":
        return await call_next(request)

    # Check for Kong-injected service secret
    secret_header = request.headers.get("x-ai-secret")
    
    # If shared secret is configured and matches, allow the request (Kong gateway auth)
    if SHARED_SECRET and secret_header == SHARED_SECRET:
        return await call_next(request)
    
    # If secret is required but missing/invalid, check for valid JWT as fallback
    auth_header = request.headers.get("Authorization") or request.headers.get("authorization")
    token = auth_header[len("Bearer "):] if auth_header and auth_header.startswith("Bearer ") else None

    # If no secret provided and we require it, reject unless JWT is valid
    if REQUIRE_SECRET and not secret_header:
        if not token:
            return JSONResponse(
                status_code=403,
                content={"success": False, "error": "Missing required x-ai-secret header"}
            )

    try:
        await verify_jwt_token(token)
    except HTTPException as exc:
        # If secret was provided but invalid, give specific error
        if secret_header and SHARED_SECRET and secret_header != SHARED_SECRET:
            return JSONResponse(
                status_code=403,
                content={"success": False, "error": "Invalid x-ai-secret header"}
            )
        return JSONResponse(
            status_code=exc.status_code,
            content={"success": False, "error": exc.detail}
        )

    return await call_next(request)


def generate_image_prompt(
    event_name: str,
    description: Optional[str] = None,
    event_type: Optional[str] = None,
    location: Optional[str] = None,
    date: Optional[str] = None,
    style: Optional[str] = None
) -> str:
    """
    Generate a creative prompt for image generation based on event details.
    Uses LLM to create a compelling, style-appropriate prompt.
    """
    # Determine style if not provided
    if not style:
        if event_type:
            event_type_lower = event_type.lower()
            if "party" in event_type_lower or "celebration" in event_type_lower:
                style = "party"
            elif "conference" in event_type_lower or "summit" in event_type_lower:
                style = "conference"
            elif "workshop" in event_type_lower or "training" in event_type_lower:
                style = "professional"
            else:
                style = "sick"
        else:
            style = "sick"
    
    # Build base prompt
    prompt_parts = []
    
    if style == "party":
        prompt_parts.append("A vibrant, energetic party scene with")
    elif style == "conference":
        prompt_parts.append("A professional, modern conference setting with")
    elif style == "professional":
        prompt_parts.append("A clean, professional workshop or business event scene with")
    else:  # sick
        prompt_parts.append("An eye-catching, trendy, and visually stunning event scene with")
    
    prompt_parts.append(f"'{event_name}' prominently featured")
    
    if description:
        # Extract key visual elements from description
        prompt_parts.append(f"inspired by: {description[:100]}")
    
    if location:
        prompt_parts.append(f"set in {location}")
    
    if date:
        prompt_parts.append(f"for {date}")
    
    # Add style-specific enhancements
    if style == "party":
        prompt_parts.append("with dynamic lighting, confetti, music vibes, colorful atmosphere, celebration mood")
    elif style == "conference":
        prompt_parts.append("with modern architecture, professional lighting, tech-forward design, corporate elegance")
    elif style == "professional":
        prompt_parts.append("with clean lines, modern design, educational atmosphere, professional setting")
    else:  # sick
        prompt_parts.append("with bold colors, modern aesthetics, Instagram-worthy design, trendy vibes")
    
    prompt_parts.append("high quality, detailed, professional photography style, 4k")
    
    base_prompt = ", ".join(prompt_parts)
    
    # Use OpenAI to refine the prompt if available
    if openai_client:
        try:
            system_prompt = """You are a creative prompt engineer for event cover images. 
            Create a detailed, visually descriptive prompt for an AI image generator that will create 
            an event cover image. The prompt should be vivid, specific, and optimized for generating 
            high-quality event cover images. Focus on visual elements, mood, and style."""
            
            response = openai_client.chat.completions.create(
                model=os.getenv("OPENAI_CHAT_MODEL", "gpt-4-turbo-preview"),
                messages=[
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": f"Create an optimized image generation prompt for: {base_prompt}"}
                ],
                temperature=0.8,
                max_tokens=200
            )
            
            refined_prompt = response.choices[0].message.content.strip()
            # Remove quotes if the model wrapped it
            if refined_prompt.startswith('"') and refined_prompt.endswith('"'):
                refined_prompt = refined_prompt[1:-1]
            return refined_prompt
        except Exception as e:
            print(f"[ai] Error refining prompt with LLM: {e}")
            # Fall back to base prompt
    
    return base_prompt


def generate_enhanced_event_prompt(
    name: str,
    description: Optional[str] = None,
    event_type: Optional[str] = None,
    theme: Optional[str] = None,
    objectives: Optional[str] = None,
    target_audience: Optional[str] = None,
    hashtag: Optional[str] = None,
    branding_guidelines: Optional[str] = None,
    venue: Optional[VenueDTO] = None,
    start_date_time: Optional[str] = None,
    variation_seed: Optional[int] = None
) -> str:
    """
    Generate a creative, varied prompt for event cover image generation.
    Uses all available event data and adds randomness for visual variation.
    """
    # Set random seed if provided for consistent variation
    if variation_seed is not None:
        random.seed(variation_seed)
    
    # Define variation pools for randomness
    artistic_styles = [
        "modern minimalist design", "vibrant contemporary art", "elegant sophisticated style",
        "bold graphic design", "photorealistic composition", "illustrative artistic style",
        "geometric abstract design", "fluid organic shapes", "retro modern aesthetic",
        "futuristic tech-inspired", "classic elegant design", "playful creative style"
    ]
    
    color_palettes = [
        "vibrant and energetic colors", "sophisticated muted tones", "bold contrasting colors",
        "warm inviting palette", "cool professional tones", "monochromatic with accent colors",
        "pastel soft colors", "rich deep colors", "neon bright colors", "earth natural tones"
    ]
    
    compositions = [
        "centered composition with dynamic elements", "asymmetric balanced layout",
        "rule of thirds composition", "diagonal dynamic composition", "symmetrical elegant layout",
        "overlapping layered elements", "minimalist spacious composition", "maximalist rich details"
    ]
    
    perspectives = [
        "eye-level view", "aerial perspective", "low angle dramatic view",
        "wide panoramic view", "close-up detailed view", "bird's eye view",
        "cinematic wide shot", "intimate focused view"
    ]
    
    lighting_styles = [
        "natural daylight", "dramatic golden hour lighting", "vibrant neon lighting",
        "soft ambient lighting", "bold high contrast lighting", "warm sunset lighting",
        "cool blue hour lighting", "dynamic stage lighting", "elegant soft glow"
    ]
    
    # Select random variations
    selected_style = random.choice(artistic_styles)
    selected_colors = random.choice(color_palettes)
    selected_composition = random.choice(compositions)
    selected_perspective = random.choice(perspectives)
    selected_lighting = random.choice(lighting_styles)
    
    # Build context from event data
    context_parts = []
    
    # Event name (always included)
    context_parts.append(f"Event name: '{name}'")
    
    # Event type influences base style
    event_style_context = ""
    if event_type:
        event_type_lower = event_type.lower()
        if event_type_lower in ["conference", "seminar", "meeting", "corporate_event"]:
            event_style_context = "professional corporate event"
        elif event_type_lower in ["party", "celebration", "festival", "concert"]:
            event_style_context = "vibrant celebration event"
        elif event_type_lower in ["workshop", "training", "retreat"]:
            event_style_context = "educational interactive event"
        elif event_type_lower in ["wedding", "birthday"]:
            event_style_context = "elegant personal celebration"
        elif event_type_lower in ["sports_event", "charity_event"]:
            event_style_context = "dynamic community event"
        else:
            event_style_context = "engaging social event"
    
    # Description
    if description:
        # Extract key visual keywords (first 150 chars)
        desc_snippet = description[:150].strip()
        context_parts.append(f"Description context: {desc_snippet}")
    
    # Theme
    if theme:
        context_parts.append(f"Theme: {theme}")
    
    # Objectives (can inform visual direction)
    if objectives:
        # Extract key concepts
        obj_keywords = objectives[:100].strip()
        context_parts.append(f"Event focus: {obj_keywords}")
    
    # Target audience (influences style)
    if target_audience:
        context_parts.append(f"Target audience: {target_audience[:80]}")
    
    # Hashtag (can add personality)
    if hashtag:
        context_parts.append(f"Event identity: {hashtag}")
    
    # Branding guidelines (important for visual consistency)
    if branding_guidelines:
        context_parts.append(f"Brand guidelines: {branding_guidelines[:100]}")
    
    # Venue/location context
    location_context = ""
    if venue:
        location_parts = []
        if venue.city:
            location_parts.append(venue.city)
        if venue.state:
            location_parts.append(venue.state)
        if venue.country:
            location_parts.append(venue.country)
        if location_parts:
            location_context = ", ".join(location_parts)
        elif venue.address:
            location_context = venue.address[:50]
    
    # Date/time context
    time_context = ""
    if start_date_time:
        try:
            # Try to parse different datetime formats
            dt_str = start_date_time.replace('Z', '+00:00')
            # Handle various formats
            formats = [
                "%Y-%m-%dT%H:%M:%S%z",
                "%Y-%m-%dT%H:%M:%S.%f%z",
                "%Y-%m-%dT%H:%M:%S",
                "%Y-%m-%dT%H:%M:%S.%f"
            ]
            dt = None
            for fmt in formats:
                try:
                    dt = datetime.strptime(dt_str, fmt)
                    break
                except ValueError:
                    continue
            
            if dt:
                month = dt.month
                hour = dt.hour
                
                # Season
                if month in [12, 1, 2]:
                    time_context = "winter season"
                elif month in [3, 4, 5]:
                    time_context = "spring season"
                elif month in [6, 7, 8]:
                    time_context = "summer season"
                else:
                    time_context = "autumn season"
                
                # Time of day
                if 6 <= hour < 12:
                    time_context += ", morning time"
                elif 12 <= hour < 17:
                    time_context += ", afternoon time"
                elif 17 <= hour < 21:
                    time_context += ", evening time"
                else:
                    time_context += ", night time"
        except Exception as e:
            print(f"[ai] Error parsing datetime: {e}")
            pass
    
    # Build the comprehensive prompt
    prompt_sections = []
    
    # Main visual description
    prompt_sections.append(f"Create a stunning event cover image for '{name}'")
    
    if event_style_context:
        prompt_sections.append(f"representing a {event_style_context}")
    
    # Add context elements
    if context_parts:
        # Combine context intelligently
        context_summary = ". ".join(context_parts[:4])  # Limit to avoid too long prompts
        prompt_sections.append(f"Context: {context_summary}")
    
    # Add location if available
    if location_context:
        prompt_sections.append(f"Location setting: {location_context}")
    
    # Add time context
    if time_context:
        prompt_sections.append(f"Time context: {time_context}")
    
    # Add random variations for uniqueness
    prompt_sections.append(f"Artistic style: {selected_style}")
    prompt_sections.append(f"Color palette: {selected_colors}")
    prompt_sections.append(f"Composition: {selected_composition}")
    prompt_sections.append(f"Perspective: {selected_perspective}")
    prompt_sections.append(f"Lighting: {selected_lighting}")
    
    # Quality and technical specs
    prompt_sections.append("high quality, professional, detailed, 4k resolution, suitable for event cover image")
    
    base_prompt = ". ".join(prompt_sections)
    
    # Use LLM to refine and optimize the prompt if available
    if openai_client:
        try:
            system_prompt = """You are an expert prompt engineer for event cover images. 
            Create a vivid, detailed, and visually compelling prompt for an AI image generator.
            The prompt should be specific, include visual details, mood, and style elements.
            Make it engaging and optimized for generating high-quality event cover images.
            Keep it concise but descriptive (under 300 words)."""
            
            user_prompt = f"""Create an optimized image generation prompt based on this event information:
            
{base_prompt}

Make it visually rich, specific, and compelling. Focus on creating a unique and attractive event cover image."""
            
            response = openai_client.chat.completions.create(
                model=os.getenv("OPENAI_CHAT_MODEL", "gpt-4o"),
                messages=[
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": user_prompt}
                ],
                temperature=0.9,  # Higher temperature for more variation
                max_tokens=300
            )
            
            refined_prompt = response.choices[0].message.content.strip()
            # Remove quotes if wrapped
            if refined_prompt.startswith('"') and refined_prompt.endswith('"'):
                refined_prompt = refined_prompt[1:-1]
            return refined_prompt
        except Exception as e:
            print(f"[ai] Error refining prompt with LLM: {e}")
            # Fall back to base prompt
    
    return base_prompt


@app.get("/health")
async def health():
    """Health check endpoint"""
    return {
        "status": "ok",
        "service": "ai",
        "openai_configured": openai_client is not None
    }


@app.post("/generate-cover-image", response_model=ImageGenerationResponse)
async def generate_cover_image(
    request: ImageGenerationRequest,
    x_ai_secret: Optional[str] = Header(None, alias="x-ai-secret")
):
    """
    Generate a cover image for an event based on its details.
    
    Uses OpenAI's latest image generation model (via ChatGPT chat completions) 
    to create custom event cover images that match the event's style, theme, 
    and vibe (party, conference, professional, or sick/trendy).
    """
    if not openai_client:
        raise HTTPException(
            status_code=503,
            detail="OpenAI API not configured. Please set OPENAI_API_KEY environment variable."
        )
    
    try:
        # Generate optimized prompt
        prompt = generate_image_prompt(
            event_name=request.event_name,
            description=request.description,
            event_type=request.event_type,
            location=request.location,
            date=request.date,
            style=request.style
        )
        
        print(f"[ai] Generating image with prompt: {prompt[:100]}...")
        
        # Generate image using OpenAI's latest image generation model
        # GPT Image 1.5 is the latest model (replacing legacy DALL-E)
        # Use images.generate() API with the newer model name
        image_model = os.getenv("OPENAI_IMAGE_MODEL", "gpt-image-1.5")
        
        try:
            # Try using the latest GPT Image 1.5 model
            response = openai_client.images.generate(
                model=image_model,
                prompt=prompt,
                size=f"{request.width}x{request.height}",
                quality="hd",
                n=1
            )
            image_url = response.data[0].url
        except Exception as e:
            # Fallback to DALL-E 3 if GPT Image 1.5 is not available
            print(f"[ai] GPT Image 1.5 not available, falling back to DALL-E 3: {e}")
            response = openai_client.images.generate(
                model="dall-e-3",
                prompt=prompt,
                size=f"{request.width}x{request.height}",
                quality="hd",
                n=1
            )
            image_url = response.data[0].url
        
        # Optionally fetch and return as base64
        image_base64 = None
        if os.getenv("RETURN_BASE64", "false").lower() == "true":
            async with httpx.AsyncClient() as client:
                img_response = await client.get(image_url)
                image_base64 = base64.b64encode(img_response.content).decode('utf-8')
        
        return ImageGenerationResponse(
            success=True,
            image_url=image_url,
            image_base64=image_base64,
            prompt_used=prompt
        )
        
    except Exception as e:
        print(f"[ai] Error generating image: {e}")
        raise HTTPException(
            status_code=500,
            detail=f"Failed to generate image: {str(e)}"
        )


@app.post("/generate-cover-image-v2")
async def generate_cover_image_v2(
    request: ImageGenerationRequest,
    x_ai_secret: Optional[str] = Header(None, alias="x-ai-secret")
):
    """
    Alternative endpoint that returns image as base64 by default.
    Useful for direct embedding without external URL dependencies.
    """
    if not openai_client:
        raise HTTPException(
            status_code=503,
            detail="OpenAI API not configured"
        )
    
    try:
        prompt = generate_image_prompt(
            event_name=request.event_name,
            description=request.description,
            event_type=request.event_type,
            location=request.location,
            date=request.date,
            style=request.style
        )
        
        # Generate image using OpenAI's latest image generation model
        # GPT Image 1.5 is the latest model (replacing legacy DALL-E)
        image_model = os.getenv("OPENAI_IMAGE_MODEL", "gpt-image-1.5")
        
        try:
            # Try using the latest GPT Image 1.5 model
            response = openai_client.images.generate(
                model=image_model,
                prompt=prompt,
                size=f"{request.width}x{request.height}",
                quality="hd",
                n=1
            )
            image_url = response.data[0].url
        except Exception as e:
            # Fallback to DALL-E 3 if GPT Image 1.5 is not available
            print(f"[ai] GPT Image 1.5 not available, falling back to DALL-E 3: {e}")
            response = openai_client.images.generate(
                model="dall-e-3",
                prompt=prompt,
                size=f"{request.width}x{request.height}",
                quality="hd",
                n=1
            )
            image_url = response.data[0].url
        
        # Always fetch and return as base64
        async with httpx.AsyncClient() as client:
            img_response = await client.get(image_url)
            image_base64 = base64.b64encode(img_response.content).decode('utf-8')
        
        return ImageGenerationResponse(
            success=True,
            image_url=image_url,
            image_base64=image_base64,
            prompt_used=prompt
        )
        
    except Exception as e:
        print(f"[ai] Error generating image: {e}")
        return JSONResponse(
            status_code=500,
            content={
                "success": False,
                "error": str(e),
                "prompt_used": prompt if 'prompt' in locals() else None
            }
        )


@app.post("/generate-event-cover-image", response_model=ImageGenerationResponse)
async def generate_event_cover_image(
    request: EventCoverImageRequest,
    x_ai_secret: Optional[str] = Header(None, alias="x-ai-secret")
):
    """
    Generate a cover image for an event based on comprehensive event data.
    
    This endpoint is designed for mobile apps where users fill out event details
    and want to preview/generate a cover image on the review page. The image is
    returned as base64 so the mobile app can save it to S3 if the user likes it.
    
    Features:
    - Uses all available event data (name, description, theme, objectives, venue, etc.)
    - Adds randomness for visual variation (different styles, colors, compositions)
    - Returns base64 image by default for easy S3 upload
    - Adapts to different event types (conference, party, workshop, etc.)
    """
    if not openai_client:
        raise HTTPException(
            status_code=503,
            detail="OpenAI API not configured. Please set OPENAI_API_KEY environment variable."
        )
    
    try:
        # Generate enhanced prompt with all event data and randomness
        prompt = generate_enhanced_event_prompt(
            name=request.name,
            description=request.description,
            event_type=request.eventType,
            theme=request.theme,
            objectives=request.objectives,
            target_audience=request.targetAudience,
            hashtag=request.hashtag,
            branding_guidelines=request.brandingGuidelines,
            venue=request.venue,
            start_date_time=request.startDateTime,
            variation_seed=request.variationSeed
        )
        
        print(f"[ai] Generating event cover image for '{request.name}' with prompt: {prompt[:150]}...")
        
        # Generate image using OpenAI's latest image generation model
        image_model = os.getenv("OPENAI_IMAGE_MODEL", "gpt-image-1.5")
        
        try:
            # Try using the latest GPT Image 1.5 model
            response = openai_client.images.generate(
                model=image_model,
                prompt=prompt,
                size=f"{request.width}x{request.height}",
                quality="hd",
                n=1
            )
            image_url = response.data[0].url
        except Exception as e:
            # Fallback to DALL-E 3 if GPT Image 1.5 is not available
            print(f"[ai] GPT Image 1.5 not available, falling back to DALL-E 3: {e}")
            response = openai_client.images.generate(
                model="dall-e-3",
                prompt=prompt,
                size=f"{request.width}x{request.height}",
                quality="hd",
                n=1
            )
            image_url = response.data[0].url
        
        # Always fetch and return as base64 for mobile to save to S3
        async with httpx.AsyncClient() as client:
            img_response = await client.get(image_url)
            img_response.raise_for_status()
            image_base64 = base64.b64encode(img_response.content).decode('utf-8')
        
        return ImageGenerationResponse(
            success=True,
            image_url=image_url,  # Also return URL for reference
            image_base64=image_base64,  # Base64 for mobile to save to S3
            prompt_used=prompt
        )
        
    except httpx.HTTPError as e:
        print(f"[ai] Error fetching image from URL: {e}")
        raise HTTPException(
            status_code=500,
            detail=f"Failed to fetch generated image: {str(e)}"
        )
    except Exception as e:
        print(f"[ai] Error generating event cover image: {e}")
        raise HTTPException(
            status_code=500,
            detail=f"Failed to generate image: {str(e)}"
        )


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=PORT)
