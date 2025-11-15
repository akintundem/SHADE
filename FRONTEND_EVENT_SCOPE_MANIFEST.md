# Event Scope & Feed Feature - Frontend Integration Guide

**Version:** 1.0.0  
**Last Updated:** November 15, 2025  
**Feature Branch:** `feature/event-scope-feed-pagination`

---

## 📋 Overview

The Event Scope feature introduces **role-based event views** that return different data structures based on the user's relationship to an event. This allows the frontend to render appropriate components for owners/organizers vs. guests.

### Key Concepts

- **FULL Scope**: Owners and high-responsibility users (ORGANIZER, COORDINATOR, STAFF) receive complete event details
- **FEED Scope**: Guests and low-responsibility users receive a social feed view with posts
- **Scope Field**: Both response types include a `scope` field indicating which view to render

---

## 🎯 What Changed

### New Endpoints

1. **GET `/api/v1/events/{id}/feed`** - Always returns feed view (regardless of user role)
2. **GET `/api/v1/events/{id}`** - Now returns scope-based response (FULL or FEED)

### Modified Behavior

- `GET /api/v1/events/{id}` now returns:
  - `EventResponse` with `scope: "FULL"` for owners/high-responsibility users
  - `EventFeedResponse` with `scope: "FEED"` for guests/low-responsibility users

---

## 📦 TypeScript Types

### Event Scope Enum

```typescript
type EventScope = 'FULL' | 'FEED';
```

### Event Response Types

```typescript
// Full Event Response (for owners/high-responsibility)
interface EventResponse {
  id: string; // UUID
  name: string;
  description?: string;
  eventType: EventType;
  eventStatus: EventStatus;
  startDateTime: string; // ISO datetime
  endDateTime?: string; // ISO datetime
  registrationDeadline?: string; // ISO datetime
  capacity?: number;
  currentAttendeeCount: number;
  isPublic: boolean;
  requiresApproval: boolean;
  qrCodeEnabled: boolean;
  qrCode?: string;
  coverImageUrl?: string;
  eventWebsiteUrl?: string;
  hashtag?: string;
  theme?: string;
  objectives?: string;
  targetAudience?: string;
  successMetrics?: string;
  brandingGuidelines?: string;
  venueRequirements?: string;
  technicalRequirements?: string;
  accessibilityFeatures?: string;
  emergencyPlan?: string;
  backupPlan?: string;
  postEventTasks?: string;
  metadata?: string;
  ownerId: string; // UUID
  venueId?: string; // UUID
  venue?: VenueDTO;
  createdAt: string; // ISO datetime
  updatedAt: string; // ISO datetime
  scope: 'FULL'; // Always FULL for this response type
}

// Feed Response (for guests)
interface EventFeedResponse {
  eventId: string; // UUID
  eventName: string;
  description?: string;
  coverImageUrl?: string;
  startDateTime: string; // ISO datetime
  endDateTime?: string; // ISO datetime
  hashtag?: string;
  eventWebsiteUrl?: string;
  posts: FeedPost[];
  
  // Pagination metadata
  currentPage: number;
  pageSize: number;
  totalPosts: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
  
  scope: 'FEED'; // Always FEED for this response type
}

// Feed Post
interface FeedPost {
  id: string; // UUID
  type: 'VIDEO' | 'IMAGE' | 'TEXT';
  content?: string;
  mediaUrl?: string;
  thumbnailUrl?: string;
  authorName?: string;
  authorAvatarUrl?: string;
  postedAt: string; // ISO datetime
  likes?: number;
  comments?: number;
}

// Feed Request (for pagination)
interface EventFeedRequest {
  page?: number; // Default: 0 (0-indexed)
  size?: number; // Default: 20, Max: 50
  postType?: 'VIDEO' | 'IMAGE' | 'TEXT' | 'ALL'; // Filter by post type
}
```

### Union Type for Event Data

```typescript
type EventData = EventResponse | EventFeedResponse;

// Type guard functions
function isFullEventResponse(data: EventData): data is EventResponse {
  return 'scope' in data && data.scope === 'FULL';
}

function isFeedResponse(data: EventData): data is EventFeedResponse {
  return 'scope' in data && data.scope === 'FEED';
}
```

---

## 🔌 API Endpoints

### 1. Get Event (Scope-Based)

**Endpoint:** `GET /api/v1/events/{id}`

**Query Parameters:**
- `page` (optional): Page number for feed pagination (only used if scope is FEED)
- `size` (optional): Page size for feed pagination (only used if scope is FEED)
- `postType` (optional): Filter posts by type (only used if scope is FEED)

**Response:**
- **200 OK**: Returns either `EventResponse` (scope: FULL) or `EventFeedResponse` (scope: FEED)
- **404 Not Found**: Event not found or access denied
- **401 Unauthorized**: Missing or invalid token

**Example Request:**
```typescript
// For owners (returns FULL scope)
const response = await fetch(`/api/v1/events/${eventId}`, {
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  }
});

const eventData: EventData = await response.json();

if (isFullEventResponse(eventData)) {
  // Render full event management dashboard
  console.log('Full event details:', eventData);
} else if (isFeedResponse(eventData)) {
  // Render feed view
  console.log('Feed view:', eventData);
}
```

**Example Request (Guest with Pagination):**
```typescript
// For guests (returns FEED scope)
const response = await fetch(
  `/api/v1/events/${eventId}?page=0&size=20&postType=VIDEO`,
  {
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    }
  }
);

const feedData: EventFeedResponse = await response.json();
// feedData.scope will be "FEED"
```

### 2. Get Event Feed (Always Feed)

**Endpoint:** `GET /api/v1/events/{id}/feed`

**Query Parameters:**
- `page` (optional): Page number (default: 0)
- `size` (optional): Page size (default: 20, max: 50)
- `postType` (optional): Filter by post type: `VIDEO`, `IMAGE`, `TEXT`, or `ALL`

**Response:**
- **200 OK**: Always returns `EventFeedResponse` with `scope: "FEED"`
- **404 Not Found**: Event not found or access denied
- **401 Unauthorized**: Missing or invalid token

**Example Request:**
```typescript
// Load first page
const loadFeed = async (eventId: string, page: number = 0) => {
  const response = await fetch(
    `/api/v1/events/${eventId}/feed?page=${page}&size=20`,
    {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    }
  );
  
  const feedData: EventFeedResponse = await response.json();
  
  return {
    posts: feedData.posts,
    hasNext: feedData.hasNext,
    hasPrevious: feedData.hasPrevious,
    currentPage: feedData.currentPage,
    totalPages: feedData.totalPages
  };
};

// Load more posts (pagination)
const loadMorePosts = async (eventId: string, nextPage: number) => {
  const feed = await loadFeed(eventId, nextPage);
  return feed.posts;
};
```

---

## 🎨 Frontend Implementation Guide

### 1. Component Structure

Create separate components for each scope:

```typescript
// components/events/EventDetailView.tsx
import React from 'react';

interface Props {
  eventData: EventData;
}

export const EventDetailView: React.FC<Props> = ({ eventData }) => {
  if (isFullEventResponse(eventData)) {
    return <FullEventDashboard event={eventData} />;
  } else if (isFeedResponse(eventData)) {
    return <EventFeedView feed={eventData} />;
  }
  
  return null;
};
```

### 2. Full Event Dashboard Component

```typescript
// components/events/FullEventDashboard.tsx
import React from 'react';
import { EventResponse } from '../types';

interface Props {
  event: EventResponse;
}

export const FullEventDashboard: React.FC<Props> = ({ event }) => {
  return (
    <div className="event-dashboard">
      <h1>{event.name}</h1>
      
      {/* Full event details */}
      <EventDetails event={event} />
      <EventAnalytics eventId={event.id} />
      <EventManagementTools event={event} />
      <EventSettings event={event} />
      
      {/* Edit buttons, admin controls, etc. */}
      <button onClick={() => editEvent(event.id)}>Edit Event</button>
      <button onClick={() => viewAnalytics(event.id)}>View Analytics</button>
    </div>
  );
};
```

### 3. Feed View Component

```typescript
// components/events/EventFeedView.tsx
import React, { useState, useEffect } from 'react';
import { EventFeedResponse, FeedPost } from '../types';

interface Props {
  feed: EventFeedResponse;
  eventId: string;
}

export const EventFeedView: React.FC<Props> = ({ feed, eventId }) => {
  const [posts, setPosts] = useState<FeedPost[]>(feed.posts);
  const [currentPage, setCurrentPage] = useState(feed.currentPage);
  const [hasNext, setHasNext] = useState(feed.hasNext);
  const [loading, setLoading] = useState(false);

  // Load more posts (infinite scroll)
  const loadMore = async () => {
    if (!hasNext || loading) return;
    
    setLoading(true);
    try {
      const response = await fetch(
        `/api/v1/events/${eventId}/feed?page=${currentPage + 1}&size=20`,
        {
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          }
        }
      );
      
      const newFeed: EventFeedResponse = await response.json();
      setPosts(prev => [...prev, ...newFeed.posts]);
      setCurrentPage(newFeed.currentPage);
      setHasNext(newFeed.hasNext);
    } catch (error) {
      console.error('Failed to load more posts:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="event-feed">
      {/* Event header */}
      <div className="feed-header">
        <h1>{feed.eventName}</h1>
        <p>{feed.description}</p>
        {feed.coverImageUrl && (
          <img src={feed.coverImageUrl} alt={feed.eventName} />
        )}
      </div>

      {/* Feed posts */}
      <div className="feed-posts">
        {posts.map(post => (
          <FeedPostCard key={post.id} post={post} />
        ))}
      </div>

      {/* Load more button */}
      {hasNext && (
        <button 
          onClick={loadMore} 
          disabled={loading}
          className="load-more-btn"
        >
          {loading ? 'Loading...' : 'Load More Posts'}
        </button>
      )}
    </div>
  );
};
```

### 4. Feed Post Card Component

```typescript
// components/events/FeedPostCard.tsx
import React from 'react';
import { FeedPost } from '../types';

interface Props {
  post: FeedPost;
}

export const FeedPostCard: React.FC<Props> = ({ post }) => {
  return (
    <div className="feed-post-card">
      {/* Author info */}
      {post.authorAvatarUrl && (
        <img 
          src={post.authorAvatarUrl} 
          alt={post.authorName || 'Author'} 
          className="author-avatar"
        />
      )}
      <div className="post-author">{post.authorName}</div>
      <div className="post-date">
        {new Date(post.postedAt).toLocaleDateString()}
      </div>

      {/* Post content */}
      {post.type === 'IMAGE' && post.mediaUrl && (
        <img src={post.mediaUrl} alt="Post image" className="post-image" />
      )}
      
      {post.type === 'VIDEO' && post.mediaUrl && (
        <video 
          src={post.mediaUrl} 
          controls 
          poster={post.thumbnailUrl}
          className="post-video"
        />
      )}
      
      {post.content && (
        <p className="post-content">{post.content}</p>
      )}

      {/* Engagement */}
      <div className="post-engagement">
        <span>❤️ {post.likes || 0}</span>
        <span>💬 {post.comments || 0}</span>
      </div>
    </div>
  );
};
```

### 5. Hook for Event Data

```typescript
// hooks/useEvent.ts
import { useState, useEffect } from 'react';
import { EventData, EventResponse, EventFeedResponse } from '../types';

export const useEvent = (eventId: string) => {
  const [eventData, setEventData] = useState<EventData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    const fetchEvent = async () => {
      try {
        setLoading(true);
        const response = await fetch(`/api/v1/events/${eventId}`, {
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          }
        });

        if (!response.ok) {
          throw new Error(`Failed to fetch event: ${response.statusText}`);
        }

        const data: EventData = await response.json();
        setEventData(data);
      } catch (err) {
        setError(err as Error);
      } finally {
        setLoading(false);
      }
    };

    fetchEvent();
  }, [eventId]);

  return {
    eventData,
    loading,
    error,
    isFullScope: eventData ? isFullEventResponse(eventData) : false,
    isFeedScope: eventData ? isFeedResponse(eventData) : false
  };
};
```

### 6. Hook for Feed Pagination

```typescript
// hooks/useEventFeed.ts
import { useState, useCallback } from 'react';
import { EventFeedResponse, FeedPost } from '../types';

export const useEventFeed = (eventId: string, initialPage: number = 0) => {
  const [posts, setPosts] = useState<FeedPost[]>([]);
  const [currentPage, setCurrentPage] = useState(initialPage);
  const [hasNext, setHasNext] = useState(false);
  const [hasPrevious, setHasPrevious] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const loadFeed = useCallback(async (page: number, postType?: string) => {
    try {
      setLoading(true);
      setError(null);
      
      const params = new URLSearchParams({
        page: page.toString(),
        size: '20'
      });
      
      if (postType && postType !== 'ALL') {
        params.append('postType', postType);
      }

      const response = await fetch(
        `/api/v1/events/${eventId}/feed?${params.toString()}`,
        {
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          }
        }
      );

      if (!response.ok) {
        throw new Error(`Failed to load feed: ${response.statusText}`);
      }

      const feedData: EventFeedResponse = await response.json();
      
      if (page === 0) {
        setPosts(feedData.posts);
      } else {
        setPosts(prev => [...prev, ...feedData.posts]);
      }
      
      setCurrentPage(feedData.currentPage);
      setHasNext(feedData.hasNext);
      setHasPrevious(feedData.hasPrevious);
    } catch (err) {
      setError(err as Error);
    } finally {
      setLoading(false);
    }
  }, [eventId]);

  const loadMore = useCallback(() => {
    if (hasNext && !loading) {
      loadFeed(currentPage + 1);
    }
  }, [hasNext, loading, currentPage, loadFeed]);

  const refresh = useCallback(() => {
    loadFeed(0);
  }, [loadFeed]);

  return {
    posts,
    currentPage,
    hasNext,
    hasPrevious,
    loading,
    error,
    loadMore,
    refresh,
    loadFeed
  };
};
```

---

## 🔄 Migration Guide

### Before (Old Implementation)

```typescript
// Old code - always expected EventResponse
const response = await fetch(`/api/v1/events/${eventId}`);
const event: EventResponse = await response.json();

// Always rendered full dashboard
<EventDashboard event={event} />
```

### After (New Implementation)

```typescript
// New code - handles both response types
const response = await fetch(`/api/v1/events/${eventId}`);
const eventData: EventData = await response.json();

// Check scope and render appropriate component
{eventData.scope === 'FULL' ? (
  <FullEventDashboard event={eventData as EventResponse} />
) : (
  <EventFeedView feed={eventData as EventFeedResponse} />
)}
```

---

## 📱 Mobile Implementation (Swipe to Load More)

### React Native Example

```typescript
import { FlatList } from 'react-native';
import { useEventFeed } from '../hooks/useEventFeed';

export const EventFeedScreen = ({ eventId }: { eventId: string }) => {
  const { posts, hasNext, loadMore, loading } = useEventFeed(eventId);

  const renderPost = ({ item }: { item: FeedPost }) => (
    <FeedPostCard post={item} />
  );

  const handleEndReached = () => {
    if (hasNext && !loading) {
      loadMore();
    }
  };

  return (
    <FlatList
      data={posts}
      renderItem={renderPost}
      onEndReached={handleEndReached}
      onEndReachedThreshold={0.5}
      keyExtractor={(item) => item.id}
      ListFooterComponent={
        loading ? <ActivityIndicator /> : null
      }
    />
  );
};
```

---

## 🎯 Best Practices

### 1. Always Check Scope

```typescript
// ✅ Good - Check scope before rendering
if (eventData.scope === 'FULL') {
  // Render full dashboard
} else {
  // Render feed view
}

// ❌ Bad - Assume response type
const event = eventData as EventResponse; // May fail for guests!
```

### 2. Use Type Guards

```typescript
// ✅ Good - Use type guards
if (isFullEventResponse(eventData)) {
  // TypeScript knows this is EventResponse
  console.log(eventData.ownerId); // ✅ Type-safe
}

// ❌ Bad - Direct casting
const fullEvent = eventData as EventResponse; // Unsafe!
```

### 3. Handle Pagination Properly

```typescript
// ✅ Good - Check hasNext before loading
if (feed.hasNext && !loading) {
  loadMore();
}

// ❌ Bad - Load without checking
loadMore(); // May cause unnecessary requests
```

### 4. Cache Feed Data

```typescript
// ✅ Good - Cache feed posts
const [allPosts, setAllPosts] = useState<FeedPost[]>([]);

useEffect(() => {
  if (feed.posts.length > 0) {
    setAllPosts(prev => {
      // Avoid duplicates
      const existingIds = new Set(prev.map(p => p.id));
      const newPosts = feed.posts.filter(p => !existingIds.has(p.id));
      return [...prev, ...newPosts];
    });
  }
}, [feed.posts]);
```

---

## 🐛 Error Handling

```typescript
try {
  const response = await fetch(`/api/v1/events/${eventId}`);
  
  if (response.status === 404) {
    // Event not found or access denied
    throw new Error('Event not found or you do not have access');
  }
  
  if (response.status === 401) {
    // Unauthorized - redirect to login
    redirectToLogin();
    return;
  }
  
  if (!response.ok) {
    throw new Error(`Failed to fetch event: ${response.statusText}`);
  }
  
  const eventData: EventData = await response.json();
  // Handle eventData...
  
} catch (error) {
  console.error('Error fetching event:', error);
  // Show error message to user
}
```

---

## 📊 Response Examples

### Full Event Response (Owner)

```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "name": "Tech Conference 2024",
  "description": "Annual technology conference",
  "eventType": "CONFERENCE",
  "eventStatus": "PUBLISHED",
  "startDateTime": "2024-06-15T09:00:00",
  "endDateTime": "2024-06-15T17:00:00",
  "capacity": 500,
  "currentAttendeeCount": 250,
  "isPublic": true,
  "ownerId": "user-uuid-here",
  "scope": "FULL",
  // ... all other event fields
}
```

### Feed Response (Guest)

```json
{
  "eventId": "123e4567-e89b-12d3-a456-426614174000",
  "eventName": "Tech Conference 2024",
  "description": "Annual technology conference",
  "coverImageUrl": "https://example.com/cover.jpg",
  "startDateTime": "2024-06-15T09:00:00",
  "endDateTime": "2024-06-15T17:00:00",
  "hashtag": "#TechConf2024",
  "posts": [
    {
      "id": "post-uuid-1",
      "type": "IMAGE",
      "content": "Great event so far!",
      "mediaUrl": "https://example.com/image1.jpg",
      "authorName": "John Doe",
      "authorAvatarUrl": "https://example.com/avatar.jpg",
      "postedAt": "2024-06-15T10:30:00",
      "likes": 42,
      "comments": 5
    },
    {
      "id": "post-uuid-2",
      "type": "VIDEO",
      "content": "Check out this amazing keynote!",
      "mediaUrl": "https://example.com/video1.mp4",
      "thumbnailUrl": "https://example.com/thumb1.jpg",
      "authorName": "Jane Smith",
      "postedAt": "2024-06-15T11:00:00",
      "likes": 89,
      "comments": 12
    }
  ],
  "currentPage": 0,
  "pageSize": 20,
  "totalPosts": 2,
  "totalPages": 1,
  "hasNext": false,
  "hasPrevious": false,
  "scope": "FEED"
}
```

---

## 🔍 Determining User Scope

The backend automatically determines the scope based on:

1. **FULL Scope** (Full Event Details):
   - Event owner
   - Users with ORGANIZER role
   - Users with COORDINATOR role
   - Users with STAFF role

2. **FEED Scope** (Feed View):
   - Guests
   - Attendees
   - Users with low-responsibility roles
   - Users without any role

**Note:** The frontend doesn't need to determine scope - it's included in the response!

---

## 🚀 Quick Start Checklist

- [ ] Add TypeScript types for `EventScope`, `EventResponse`, `EventFeedResponse`, `FeedPost`
- [ ] Create type guard functions (`isFullEventResponse`, `isFeedResponse`)
- [ ] Update event detail component to check `scope` field
- [ ] Create `FullEventDashboard` component for FULL scope
- [ ] Create `EventFeedView` component for FEED scope
- [ ] Implement pagination logic for feed (infinite scroll or load more button)
- [ ] Add error handling for 404/401 responses
- [ ] Test with both owner and guest user accounts
- [ ] Update routing to handle both response types

---

## 📝 Summary

### Key Changes for Frontend

1. **Check `scope` field** in event responses to determine which component to render
2. **Use `/feed` endpoint** for dedicated feed view (always returns FEED scope)
3. **Implement pagination** for feed posts using `hasNext` and `hasPrevious` fields
4. **Handle both response types** - `EventResponse` and `EventFeedResponse`
5. **Filter posts** by type using `postType` query parameter

### Benefits

- ✅ Clear separation between owner and guest views
- ✅ Better UX - guests see social feed, owners see management dashboard
- ✅ Pagination support for large feeds
- ✅ Type-safe implementation with TypeScript
- ✅ Backward compatible (existing FULL scope users unaffected)

---

## 🆘 Support

For questions or issues:
- Check the test report: `test/reports/event_test_report_*.md`
- Review API documentation: `EVENTS_MANIFEST.md`
- Contact backend team for scope determination logic

---

**Happy Coding! 🎉**

