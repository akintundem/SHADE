# Fix for "Maximum Update Depth Exceeded" Error

## Problem

The "Maximum update depth exceeded" error occurs when React detects an infinite loop in state updates, typically caused by:

1. **Missing dependencies** in `useEffect` or `useCallback` hooks
2. **State updates triggering re-renders** that trigger more state updates
3. **Functions being recreated** on every render, causing dependency arrays to change
4. **Props changing** on every render (new object/array references)

## Common Issues in Event Scope Feature

### Issue 1: Missing `token` in Dependencies

**❌ Problem:**
```typescript
useEffect(() => {
  fetch(`/api/v1/events/${eventId}`, {
    headers: {
      'Authorization': `Bearer ${token}`, // token used but not in deps
    }
  });
}, [eventId]); // Missing token!
```

**✅ Fix:**
```typescript
useEffect(() => {
  if (!token) return; // Guard clause
  
  fetch(`/api/v1/events/${eventId}`, {
    headers: {
      'Authorization': `Bearer ${token}`,
    }
  });
}, [eventId, token]); // Include token
```

### Issue 2: `loadFeed` Function Recreation

**❌ Problem:**
```typescript
const loadFeed = useCallback(async (page: number) => {
  // ... fetch logic
}, [eventId]); // Missing token, causing recreation

const loadMore = useCallback(() => {
  loadFeed(currentPage + 1);
}, [loadFeed, currentPage]); // loadFeed changes every render!
```

**✅ Fix:**
```typescript
const loadFeed = useCallback(async (page: number) => {
  if (loading) return; // Prevent concurrent calls
  
  // ... fetch logic
}, [eventId, token, loading]); // Include all dependencies

const loadMore = useCallback(() => {
  if (hasNext && !loading) {
    loadFeed(currentPage + 1);
  }
}, [hasNext, loading, currentPage, loadFeed]); // Stable dependencies
```

### Issue 3: Initializing State from Props

**❌ Problem:**
```typescript
const [posts, setPosts] = useState<FeedPost[]>(feed.posts);
// If feed.posts is a new array on every render, this causes issues
```

**✅ Fix:**
```typescript
// Option 1: Use useEffect to sync
const [posts, setPosts] = useState<FeedPost[]>([]);

useEffect(() => {
  setPosts(feed.posts);
}, [feed.posts.length]); // Only depend on length

// Option 2: Use a ref to track if initialized
const initialized = useRef(false);
useEffect(() => {
  if (!initialized.current) {
    setPosts(feed.posts);
    initialized.current = true;
  }
}, []);
```

### Issue 4: Array Dependencies Causing Loops

**❌ Problem:**
```typescript
useEffect(() => {
  setAllPosts(prev => [...prev, ...feed.posts]);
}, [feed.posts]); // feed.posts is a new array every time!
```

**✅ Fix:**
```typescript
// Option 1: Depend on length instead
useEffect(() => {
  setAllPosts(prev => {
    const existingIds = new Set(prev.map(p => p.id));
    const newPosts = feed.posts.filter(p => !existingIds.has(p.id));
    return newPosts.length > 0 ? [...prev, ...newPosts] : prev;
  });
}, [feed.posts.length]); // Only length, not the array

// Option 2: Use a ref to track previous IDs
const previousIds = useRef<Set<string>>(new Set());
useEffect(() => {
  const newPosts = feed.posts.filter(p => !previousIds.current.has(p.id));
  if (newPosts.length > 0) {
    newPosts.forEach(p => previousIds.current.add(p.id));
    setAllPosts(prev => [...prev, ...newPosts]);
  }
}, [feed.posts.length]);
```

## Complete Fixed Hook Examples

### Fixed `useEvent` Hook

```typescript
import { useState, useEffect, useMemo } from 'react';

export const useEvent = (eventId: string, token: string) => {
  const [eventData, setEventData] = useState<EventData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    // Guard clauses
    if (!eventId || !token) {
      setLoading(false);
      return;
    }

    let cancelled = false; // Cleanup flag

    const fetchEvent = async () => {
      try {
        setLoading(true);
        setError(null);
        
        const response = await fetch(`/api/v1/events/${eventId}`, {
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          }
        });

        if (cancelled) return; // Don't update if cancelled

        if (!response.ok) {
          throw new Error(`Failed to fetch: ${response.statusText}`);
        }

        const data = await response.json();
        
        if (!cancelled) {
          setEventData(data);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err as Error);
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    fetchEvent();

    // Cleanup function
    return () => {
      cancelled = true;
    };
  }, [eventId, token]); // All dependencies included

  // Memoize computed values
  const isFullScope = useMemo(() => 
    eventData ? isFullEventResponse(eventData) : false,
    [eventData]
  );
  
  const isFeedScope = useMemo(() => 
    eventData ? isFeedResponse(eventData) : false,
    [eventData]
  );

  return { eventData, loading, error, isFullScope, isFeedScope };
};
```

### Fixed `useEventFeed` Hook

```typescript
import { useState, useCallback, useEffect, useRef } from 'react';

export const useEventFeed = (eventId: string, token: string, initialPage = 0) => {
  const [posts, setPosts] = useState<FeedPost[]>([]);
  const [currentPage, setCurrentPage] = useState(initialPage);
  const [hasNext, setHasNext] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  
  const hasInitialLoad = useRef(false);
  const loadingRef = useRef(false); // Prevent concurrent loads

  const loadFeed = useCallback(async (page: number, postType?: string) => {
    // Prevent concurrent loads
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
      if (page === 0) {
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
    } catch (err) {
      setError(err as Error);
    } finally {
      setLoading(false);
      loadingRef.current = false;
    }
  }, [eventId, token]); // Stable dependencies

  // Initial load
  useEffect(() => {
    if (!hasInitialLoad.current && eventId && token) {
      hasInitialLoad.current = true;
      loadFeed(initialPage);
    }
  }, [eventId, token, initialPage, loadFeed]);

  const loadMore = useCallback(() => {
    if (hasNext && !loading && currentPage !== undefined) {
      loadFeed(currentPage + 1);
    }
  }, [hasNext, loading, currentPage, loadFeed]);

  const refresh = useCallback(() => {
    hasInitialLoad.current = false;
    setPosts([]);
    setCurrentPage(initialPage);
    loadFeed(initialPage);
  }, [initialPage, loadFeed]);

  return { posts, currentPage, hasNext, loading, error, loadMore, refresh, loadFeed };
};
```

## Quick Checklist

When implementing hooks, ensure:

- [ ] All variables used inside `useEffect`/`useCallback` are in the dependency array
- [ ] Guard clauses prevent unnecessary executions
- [ ] Cleanup functions cancel async operations
- [ ] State updates are conditional (don't update if value hasn't changed)
- [ ] Refs are used for values that shouldn't trigger re-renders
- [ ] Memoization is used for expensive computations
- [ ] Array/object dependencies use stable references or length/keys

## Testing for Infinite Loops

1. **Check React DevTools Profiler** - Look for components re-rendering excessively
2. **Add console.logs** - See if functions are being called repeatedly
3. **Use React StrictMode** - It will help detect issues in development
4. **Check Network Tab** - See if API calls are being made repeatedly

## Summary

The main fixes are:
1. ✅ Include `token` in all dependency arrays
2. ✅ Use cleanup functions in `useEffect`
3. ✅ Prevent concurrent API calls with refs
4. ✅ Memoize computed values
5. ✅ Use stable dependencies (length instead of arrays when possible)
6. ✅ Guard clauses to prevent unnecessary executions

