# Event Feed API - Frontend Integration Guide

**Version:** 1.0.0  
**Last Updated:** November 15, 2025  
**Feature:** Event Feed with Pagination

---

## 📋 Overview

The Event Feed feature provides a social media-style feed view for events, displaying posts (videos, images, and text) in a paginated format. This is designed for guest users and attendees who want to see event content in a feed format rather than full event management details.

### Key Concepts

- **Feed View**: Social media-style feed showing event posts
- **Pagination**: Efficient loading of posts with page-based navigation
- **Post Types**: VIDEO, IMAGE, and TEXT posts
- **Scope-Based**: Automatically returns feed view for guests, full details for owners
- **Internal Posts Only**: Only posts generated within the application (no external social media)

---

## 🎯 What This Feature Provides

### Capabilities

1. **View Event Feed**: Get paginated feed of event posts
2. **Filter by Post Type**: Filter posts by VIDEO, IMAGE, TEXT, or ALL
3. **Pagination**: Load posts page by page with metadata
4. **Scope Detection**: Automatically returns feed for guests, full details for owners

### Access Control

- **Feed View**: Available to all users (guests, attendees, etc.)
- **Full Details**: Only event owners and high-responsibility roles (ORGANIZER, COORDINATOR, STAFF)

---

## 📦 TypeScript Types

### Event Scope Enum

```typescript
type EventScope = 'FULL' | 'FEED';
```

### Post Type Enum

```typescript
type PostType = 'VIDEO' | 'IMAGE' | 'TEXT';
```

### Request DTOs

```typescript
// Feed Request (Query Parameters)
interface EventFeedRequest {
  page?: number;        // Page number (0-indexed), default: 0
  size?: number;        // Page size (1-50), default: 20
  postType?: string;    // Filter: 'VIDEO', 'IMAGE', 'TEXT', or 'ALL'
}
```

### Response DTOs

```typescript
// Feed Response
interface EventFeedResponse {
  eventId: string;                    // UUID
  eventName: string;
  description?: string;
  coverImageUrl?: string;
  startDateTime: string;               // ISO datetime
  endDateTime?: string;                // ISO datetime
  hashtag?: string;
  eventWebsiteUrl?: string;
  posts: FeedPost[];                   // Array of feed posts
  currentPage: number;                 // Current page (0-indexed)
  pageSize: number;                    // Number of items per page
  totalPosts: number;                  // Total number of posts
  totalPages: number;                  // Total number of pages
  hasNext: boolean;                    // Whether there is a next page
  hasPrevious: boolean;                // Whether there is a previous page
  scope: 'FEED';                       // Always 'FEED' for this response
}

// Feed Post
interface FeedPost {
  id: string;                          // UUID
  type: 'VIDEO' | 'IMAGE' | 'TEXT';
  content?: string;                    // Post text content
  mediaUrl?: string;                   // Media URL (for videos/images)
  thumbnailUrl?: string;               // Thumbnail URL (for videos)
  authorName?: string;
  authorAvatarUrl?: string;
  postedAt: string;                    // ISO datetime
  likes?: number;                      // Number of likes
  comments?: number;                 // Number of comments
}
```

---

## 🔌 API Endpoints

### Base URL
```
/api/v1/events
```

### Required Headers
```typescript
{
  'Content-Type': 'application/json',
  'Authorization': 'Bearer {accessToken}',
  'X-Device-Id': '{deviceId}' // Optional but recommended
}
```

---

### 1. Get Event Feed (Dedicated Endpoint)

**Endpoint:** `GET /api/v1/events/{eventId}/feed`

**Query Parameters:**
- `page` (optional): Page number (default: 0, 0-indexed)
- `size` (optional): Page size (default: 20, max: 50)
- `postType` (optional): Filter by type - `VIDEO`, `IMAGE`, `TEXT`, or `ALL` (default: ALL)

**Response:**
- **200 OK**: Returns `EventFeedResponse` with `scope: "FEED"`
- **401 Unauthorized**: Missing or invalid token
- **404 Not Found**: Event not found or access denied

**Example Request:**
```typescript
const getEventFeed = async (
  eventId: string,
  page: number = 0,
  size: number = 20,
  postType?: 'VIDEO' | 'IMAGE' | 'TEXT' | 'ALL'
) => {
  const params = new URLSearchParams({
    page: page.toString(),
    size: size.toString()
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
    throw new Error(`Failed to fetch feed: ${response.statusText}`);
  }

  const feed: EventFeedResponse = await response.json();
  return feed;
};

// Usage examples
const feed = await getEventFeed(eventId, 0, 20); // First page, all posts
const videoPosts = await getEventFeed(eventId, 0, 20, 'VIDEO'); // Only videos
const nextPage = await getEventFeed(eventId, 1, 20); // Second page
```

---

### 2. Get Event (Scope-Based)

**Endpoint:** `GET /api/v1/events/{eventId}`

**Query Parameters:**
- `page` (optional): Page number for feed pagination (only used if scope is FEED)
- `size` (optional): Page size for feed pagination (only used if scope is FEED)
- `postType` (optional): Filter posts by type (only used if scope is FEED)

**Response:**
- **200 OK**: Returns either:
  - `EventResponse` with `scope: "FULL"` (for owners/high-responsibility users)
  - `EventFeedResponse` with `scope: "FEED"` (for guests/attendees)
- **401 Unauthorized**: Missing or invalid token
- **404 Not Found**: Event not found or access denied

**Example Request:**
```typescript
const getEvent = async (eventId: string) => {
  const response = await fetch(`/api/v1/events/${eventId}`, {
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    }
  });

  if (!response.ok) {
    throw new Error(`Failed to fetch event: ${response.statusText}`);
  }

  const data: EventResponse | EventFeedResponse = await response.json();
  
  // Check scope to determine response type
  if (data.scope === 'FULL') {
    // Handle full event details
    const fullEvent = data as EventResponse;
    console.log('Full event:', fullEvent);
  } else {
    // Handle feed view
    const feed = data as EventFeedResponse;
    console.log('Feed view:', feed);
  }
  
  return data;
};
```

---

## 🎨 Frontend Implementation Guide

### 1. React Hook for Event Feed

```typescript
// hooks/useEventFeed.ts
import { useState, useCallback, useEffect, useRef } from 'react';
import { EventFeedResponse, FeedPost } from '../types';

export const useEventFeed = (
  eventId: string,
  token: string,
  initialPage: number = 0
) => {
  const [posts, setPosts] = useState<FeedPost[]>([]);
  const [currentPage, setCurrentPage] = useState(initialPage);
  const [hasNext, setHasNext] = useState(false);
  const [hasPrevious, setHasPrevious] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const [feedInfo, setFeedInfo] = useState<Partial<EventFeedResponse>>({});
  
  const hasInitialLoad = useRef(false);
  const loadingRef = useRef(false);

  const loadFeed = useCallback(async (
    page: number,
    postType?: string,
    append: boolean = false
  ) => {
    if (loadingRef.current || !eventId || !token) {
      return;
    }

    loadingRef.current = true;

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
      
      // Prevent duplicates
      if (page === 0 || !append) {
        setPosts(feedData.posts);
      } else {
        setPosts(prev => {
          const existingIds = new Set(prev.map(p => p.id));
          const newPosts = feedData.posts.filter(p => !existingIds.has(p.id));
          return newPosts.length > 0 ? [...prev, ...newPosts] : prev;
        });
      }
      
      setCurrentPage(feedData.currentPage);
      setHasNext(feedData.hasNext);
      setHasPrevious(feedData.hasPrevious);
      setFeedInfo({
        eventId: feedData.eventId,
        eventName: feedData.eventName,
        description: feedData.description,
        coverImageUrl: feedData.coverImageUrl,
        startDateTime: feedData.startDateTime,
        endDateTime: feedData.endDateTime,
        hashtag: feedData.hashtag,
        eventWebsiteUrl: feedData.eventWebsiteUrl
      });
    } catch (err) {
      setError(err as Error);
    } finally {
      setLoading(false);
      loadingRef.current = false;
    }
  }, [eventId, token]);

  // Initial load
  useEffect(() => {
    if (!hasInitialLoad.current && eventId && token) {
      hasInitialLoad.current = true;
      loadFeed(initialPage);
    }
  }, [eventId, token, initialPage, loadFeed]);

  const loadMore = useCallback(() => {
    if (hasNext && !loading && currentPage !== undefined) {
      loadFeed(currentPage + 1, undefined, true);
    }
  }, [hasNext, loading, currentPage, loadFeed]);

  const refresh = useCallback(() => {
    hasInitialLoad.current = false;
    setPosts([]);
    setCurrentPage(initialPage);
    loadFeed(initialPage);
  }, [initialPage, loadFeed]);

  const filterByType = useCallback((postType: string) => {
    hasInitialLoad.current = false;
    setPosts([]);
    setCurrentPage(0);
    loadFeed(0, postType);
  }, [loadFeed]);

  return {
    posts,
    feedInfo,
    currentPage,
    hasNext,
    hasPrevious,
    loading,
    error,
    loadMore,
    refresh,
    filterByType,
    loadFeed
  };
};
```

---

### 2. Feed View Component

```typescript
// components/events/EventFeedView.tsx
import React from 'react';
import { useEventFeed } from '../../hooks/useEventFeed';
import { FeedPostCard } from './FeedPostCard';

interface Props {
  eventId: string;
  token: string;
}

export const EventFeedView: React.FC<Props> = ({ eventId, token }) => {
  const {
    posts,
    feedInfo,
    hasNext,
    loadMore,
    loading,
    error,
    filterByType
  } = useEventFeed(eventId, token);

  if (error) {
    return (
      <div className="feed-error">
        <p>Error loading feed: {error.message}</p>
        <button onClick={() => window.location.reload()}>Retry</button>
      </div>
    );
  }

  return (
    <div className="event-feed">
      {/* Event Header */}
      {feedInfo.coverImageUrl && (
        <div className="feed-header">
          <img 
            src={feedInfo.coverImageUrl} 
            alt={feedInfo.eventName} 
            className="cover-image"
          />
          <div className="feed-header-content">
            <h1>{feedInfo.eventName}</h1>
            {feedInfo.description && <p>{feedInfo.description}</p>}
            {feedInfo.hashtag && (
              <span className="hashtag">{feedInfo.hashtag}</span>
            )}
          </div>
        </div>
      )}

      {/* Filter Buttons */}
      <div className="feed-filters">
        <button onClick={() => filterByType('ALL')}>All</button>
        <button onClick={() => filterByType('IMAGE')}>Images</button>
        <button onClick={() => filterByType('VIDEO')}>Videos</button>
        <button onClick={() => filterByType('TEXT')}>Text</button>
      </div>

      {/* Feed Posts */}
      <div className="feed-posts">
        {posts.length === 0 && !loading ? (
          <div className="no-posts">
            <p>No posts yet. Check back later!</p>
          </div>
        ) : (
          posts.map(post => (
            <FeedPostCard key={post.id} post={post} />
          ))
        )}
      </div>

      {/* Load More Button */}
      {hasNext && (
        <div className="load-more-container">
          <button
            onClick={loadMore}
            disabled={loading}
            className="load-more-btn"
          >
            {loading ? 'Loading...' : 'Load More Posts'}
          </button>
        </div>
      )}

      {loading && posts.length === 0 && (
        <div className="loading">Loading feed...</div>
      )}
    </div>
  );
};
```

---

### 3. Feed Post Card Component

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
      {/* Author Info */}
      <div className="post-header">
        {post.authorAvatarUrl && (
          <img
            src={post.authorAvatarUrl}
            alt={post.authorName || 'Author'}
            className="author-avatar"
          />
        )}
        <div className="author-info">
          <div className="author-name">{post.authorName || 'Anonymous'}</div>
          <div className="post-date">
            {new Date(post.postedAt).toLocaleDateString('en-US', {
              year: 'numeric',
              month: 'short',
              day: 'numeric',
              hour: '2-digit',
              minute: '2-digit'
            })}
          </div>
        </div>
      </div>

      {/* Post Content */}
      {post.type === 'IMAGE' && post.mediaUrl && (
        <div className="post-image-container">
          <img
            src={post.mediaUrl}
            alt={post.content || 'Post image'}
            className="post-image"
            loading="lazy"
          />
        </div>
      )}

      {post.type === 'VIDEO' && post.mediaUrl && (
        <div className="post-video-container">
          <video
            src={post.mediaUrl}
            controls
            poster={post.thumbnailUrl}
            className="post-video"
          >
            Your browser does not support the video tag.
          </video>
        </div>
      )}

      {post.content && (
        <div className="post-content">
          <p>{post.content}</p>
        </div>
      )}

      {/* Engagement Metrics */}
      <div className="post-engagement">
        <div className="engagement-item">
          <span className="icon">❤️</span>
          <span>{post.likes || 0}</span>
        </div>
        <div className="engagement-item">
          <span className="icon">💬</span>
          <span>{post.comments || 0}</span>
        </div>
      </div>
    </div>
  );
};
```

---

### 4. Infinite Scroll Implementation

```typescript
// components/events/InfiniteFeedView.tsx
import React, { useEffect, useRef } from 'react';
import { useEventFeed } from '../../hooks/useEventFeed';
import { FeedPostCard } from './FeedPostCard';

interface Props {
  eventId: string;
  token: string;
}

export const InfiniteFeedView: React.FC<Props> = ({ eventId, token }) => {
  const { posts, hasNext, loadMore, loading } = useEventFeed(eventId, token);
  const observerRef = useRef<IntersectionObserver | null>(null);
  const loadMoreRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!hasNext || loading) return;

    // Create intersection observer
    observerRef.current = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting) {
          loadMore();
        }
      },
      { threshold: 0.1 }
    );

    if (loadMoreRef.current) {
      observerRef.current.observe(loadMoreRef.current);
    }

    return () => {
      if (observerRef.current && loadMoreRef.current) {
        observerRef.current.unobserve(loadMoreRef.current);
      }
    };
  }, [hasNext, loading, loadMore]);

  return (
    <div className="infinite-feed">
      {posts.map(post => (
        <FeedPostCard key={post.id} post={post} />
      ))}
      
      {/* Intersection observer target */}
      {hasNext && (
        <div ref={loadMoreRef} className="load-more-trigger">
          {loading && <div>Loading more posts...</div>}
        </div>
      )}
    </div>
  );
};
```

---

### 5. Mobile Swipe Implementation (React Native)

```typescript
// components/events/MobileFeedView.tsx
import React from 'react';
import { FlatList, ActivityIndicator, View } from 'react-native';
import { useEventFeed } from '../../hooks/useEventFeed';
import { FeedPostCard } from './FeedPostCard';

interface Props {
  eventId: string;
  token: string;
}

export const MobileFeedView: React.FC<Props> = ({ eventId, token }) => {
  const { posts, hasNext, loadMore, loading } = useEventFeed(eventId, token);

  const handleEndReached = () => {
    if (hasNext && !loading) {
      loadMore();
    }
  };

  return (
    <FlatList
      data={posts}
      renderItem={({ item }) => <FeedPostCard post={item} />}
      keyExtractor={(item) => item.id}
      onEndReached={handleEndReached}
      onEndReachedThreshold={0.5}
      ListFooterComponent={
        loading ? <ActivityIndicator /> : null
      }
      refreshing={false}
      onRefresh={() => {}}
    />
  );
};
```

---

## 🐛 Error Handling

```typescript
try {
  const feed = await getEventFeed(eventId, 0, 20);
} catch (error) {
  if (error.message.includes('401')) {
    // Unauthorized - redirect to login
    redirectToLogin();
  } else if (error.message.includes('404')) {
    // Event not found or access denied
    showError('Event not found or you do not have access');
  } else {
    // Other errors
    showError('Failed to load feed. Please try again.');
  }
}
```

---

## 📊 Response Examples

### Feed Response Example

```json
{
  "eventId": "123e4567-e89b-12d3-a456-426614174000",
  "eventName": "Tech Conference 2024",
  "description": "Annual technology conference",
  "coverImageUrl": "https://images.unsplash.com/photo-1540575467063-178a50c2df87",
  "startDateTime": "2024-06-15T09:00:00",
  "endDateTime": "2024-06-15T17:00:00",
  "hashtag": "#TechConf2024",
  "eventWebsiteUrl": "https://example.com/event",
  "posts": [
    {
      "id": "post-uuid-1",
      "type": "IMAGE",
      "content": "Great event so far!",
      "mediaUrl": "https://images.unsplash.com/photo-1505373877841-8d25f7d46678",
      "authorName": "John Doe",
      "authorAvatarUrl": "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e",
      "postedAt": "2024-06-15T10:30:00",
      "likes": 42,
      "comments": 5
    },
    {
      "id": "post-uuid-2",
      "type": "VIDEO",
      "content": "Check out this amazing keynote!",
      "mediaUrl": "https://example.com/video1.mp4",
      "thumbnailUrl": "https://images.unsplash.com/photo-1478737270239-2f02b77fc618",
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

## 🎯 Best Practices

### 1. Pagination

```typescript
// ✅ Good - Check hasNext before loading
if (feed.hasNext && !loading) {
  loadMore();
}

// ❌ Bad - Load without checking
loadMore(); // May cause unnecessary requests
```

### 2. Image Optimization

```typescript
// ✅ Good - Use lazy loading for images
<img
  src={post.mediaUrl}
  alt={post.content}
  loading="lazy"
  className="post-image"
/>

// ✅ Good - Use thumbnail for videos
<video
  src={post.mediaUrl}
  poster={post.thumbnailUrl}
  controls
/>
```

### 3. Duplicate Prevention

```typescript
// ✅ Good - Prevent duplicate posts
setPosts(prev => {
  const existingIds = new Set(prev.map(p => p.id));
  const newPosts = feedData.posts.filter(p => !existingIds.has(p.id));
  return [...prev, ...newPosts];
});
```

### 4. Error Recovery

```typescript
// ✅ Good - Retry on error
const loadFeedWithRetry = async (retries = 3) => {
  for (let i = 0; i < retries; i++) {
    try {
      return await loadFeed(eventId, page);
    } catch (error) {
      if (i === retries - 1) throw error;
      await new Promise(resolve => setTimeout(resolve, 1000 * (i + 1)));
    }
  }
};
```

---

## 🚀 Quick Start Checklist

- [ ] Add TypeScript types for `EventFeedResponse`, `FeedPost`, `EventFeedRequest`
- [ ] Create `useEventFeed` hook with pagination
- [ ] Create `EventFeedView` component
- [ ] Create `FeedPostCard` component for individual posts
- [ ] Implement infinite scroll or "Load More" button
- [ ] Add filter buttons for post types (VIDEO, IMAGE, TEXT)
- [ ] Add error handling for 404/401 responses
- [ ] Implement image lazy loading
- [ ] Test with different post types
- [ ] Test pagination with large feeds

---

## 📝 Summary

### Key Features

1. **Feed View**: Social media-style feed for event posts
2. **Pagination**: Efficient page-based loading
3. **Filtering**: Filter by post type (VIDEO, IMAGE, TEXT)
4. **Scope-Based**: Automatic feed view for guests
5. **Internal Posts**: Only posts from within the application

### Endpoints

1. **GET `/api/v1/events/{id}/feed`** - Always returns feed view
2. **GET `/api/v1/events/{id}`** - Returns feed or full details based on user role

### Benefits

- ✅ Social media-style experience for guests
- ✅ Efficient pagination for large feeds
- ✅ Filter by post type
- ✅ Automatic scope detection
- ✅ Mobile-friendly with infinite scroll support

---

**Happy Coding! 🎉**

