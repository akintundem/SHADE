# Event Collaboration API - Frontend Integration Guide

**Version:** 1.0.0  
**Last Updated:** November 15, 2025  
**Feature:** Event Collaboration Management

---

## 📋 Overview

The Event Collaboration feature allows event owners and administrators to manage collaborators for their events. Collaborators can be assigned different roles and permissions to help organize and manage events.

### Key Concepts

- **Collaborators**: Users who are assigned to help manage an event
- **Roles**: Different levels of access (ORGANIZER, COORDINATOR, STAFF, ATTENDEE)
- **Permissions**: Custom permissions that can be assigned to collaborators
- **Invitations**: Optional email invitations sent when adding collaborators

---

## 🎯 What This Feature Provides

### Capabilities

1. **View Collaborators**: Get a paginated list of all collaborators for an event
2. **Add Collaborators**: Add new collaborators with specific roles and permissions
3. **Update Collaborators**: Modify collaborator information, roles, and permissions
4. **Remove Collaborators**: Remove collaborators from an event

### Access Control

- **View Collaborators**: Users with event access can view collaborators
- **Add/Update/Remove**: Only event owners and system admins can manage collaborators

---

## 📦 TypeScript Types

### Event User Type Enum

```typescript
type EventUserType = 
  | 'ORGANIZER'    // Full event management access
  | 'COORDINATOR'    // Event coordination and planning
  | 'STAFF'        // Event staff with limited permissions
  | 'ATTENDEE'     // Regular attendee
  | 'GUEST';       // Guest access
```

### Request DTOs

```typescript
// Add/Update Collaborator Request
interface EventCollaboratorRequest {
  userId: string;                    // UUID - Required
  email: string;                     // Required, must be valid email
  role: EventUserType;               // Required
  permissions?: string[];            // Optional custom permissions
  notes?: string;                    // Optional notes about collaborator
  sendInvitation?: boolean;          // Default: true
  invitationMessage?: string;        // Optional custom invitation message
}
```

### Response DTOs

```typescript
// Collaborator Response
interface EventCollaboratorResponse {
  collaboratorId: string;           // UUID
  eventId: string;                   // UUID
  userId: string;                     // UUID
  email: string;
  userName?: string;
  role: EventUserType;
  permissions: string[];             // List of custom permissions
  registrationStatus?: string;        // e.g., "CONFIRMED", "PENDING"
  notes?: string;
  invitationSent: boolean;
  invitationSentAt?: string;         // ISO datetime
  addedAt: string;                    // ISO datetime
  updatedAt: string;                  // ISO datetime
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

### 1. Get Event Collaborators

**Endpoint:** `GET /api/v1/events/{eventId}/collaborators`

**Query Parameters:**
- `page` (optional): Page number (default: 0)
- `size` (optional): Page size (default: 20, max: 100)

**Response:**
- **200 OK**: Returns `EventCollaboratorResponse[]`
- **401 Unauthorized**: Missing or invalid token
- **403 Forbidden**: User doesn't have access to the event

**Example Request:**
```typescript
const getCollaborators = async (
  eventId: string, 
  page: number = 0, 
  size: number = 20
) => {
  const response = await fetch(
    `/api/v1/events/${eventId}/collaborators?page=${page}&size=${size}`,
    {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    }
  );

  if (!response.ok) {
    throw new Error(`Failed to fetch collaborators: ${response.statusText}`);
  }

  const collaborators: EventCollaboratorResponse[] = await response.json();
  return collaborators;
};
```

---

### 2. Add Event Collaborator

**Endpoint:** `POST /api/v1/events/{eventId}/collaborators`

**Request Body:** `EventCollaboratorRequest`

**Response:**
- **200 OK**: Returns `EventCollaboratorResponse`
- **400 Bad Request**: Invalid request data
- **401 Unauthorized**: Missing or invalid token
- **403 Forbidden**: User is not event owner or admin

**Example Request:**
```typescript
const addCollaborator = async (
  eventId: string,
  collaborator: EventCollaboratorRequest
) => {
  const response = await fetch(
    `/api/v1/events/${eventId}/collaborators`,
    {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(collaborator)
    }
  );

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Failed to add collaborator');
  }

  const newCollaborator: EventCollaboratorResponse = await response.json();
  return newCollaborator;
};

// Usage
const newCollaborator = await addCollaborator(eventId, {
  userId: 'user-uuid-here',
  email: 'collaborator@example.com',
  role: 'COORDINATOR',
  permissions: ['read', 'write'],
  notes: 'Helping with event logistics',
  sendInvitation: true,
  invitationMessage: 'You have been invited to collaborate on this event!'
});
```

---

### 3. Update Event Collaborator

**Endpoint:** `PUT /api/v1/events/{eventId}/collaborators/{collaboratorId}`

**Request Body:** `EventCollaboratorRequest` (all fields optional except those you want to update)

**Response:**
- **200 OK**: Returns updated `EventCollaboratorResponse`
- **400 Bad Request**: Invalid request data
- **401 Unauthorized**: Missing or invalid token
- **403 Forbidden**: User is not event owner or admin
- **404 Not Found**: Collaborator not found

**Example Request:**
```typescript
const updateCollaborator = async (
  eventId: string,
  collaboratorId: string,
  updates: Partial<EventCollaboratorRequest>
) => {
  const response = await fetch(
    `/api/v1/events/${eventId}/collaborators/${collaboratorId}`,
    {
      method: 'PUT',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(updates)
    }
  );

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Failed to update collaborator');
  }

  const updatedCollaborator: EventCollaboratorResponse = await response.json();
  return updatedCollaborator;
};

// Usage - Update role and permissions
const updated = await updateCollaborator(eventId, collaboratorId, {
  role: 'ORGANIZER',
  permissions: ['read', 'write', 'delete'],
  notes: 'Promoted to organizer'
});
```

---

### 4. Remove Event Collaborator

**Endpoint:** `DELETE /api/v1/events/{eventId}/collaborators/{collaboratorId}`

**Response:**
- **204 No Content**: Collaborator removed successfully
- **401 Unauthorized**: Missing or invalid token
- **403 Forbidden**: User is not event owner or admin
- **404 Not Found**: Collaborator not found

**Example Request:**
```typescript
const removeCollaborator = async (
  eventId: string,
  collaboratorId: string
) => {
  const response = await fetch(
    `/api/v1/events/${eventId}/collaborators/${collaboratorId}`,
    {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    }
  );

  if (response.status === 204) {
    return true; // Successfully removed
  }

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Failed to remove collaborator');
  }
};

// Usage
await removeCollaborator(eventId, collaboratorId);
```

---

## 🎨 Frontend Implementation Guide

### 1. React Hook for Collaborators

```typescript
// hooks/useEventCollaborators.ts
import { useState, useEffect, useCallback } from 'react';
import { EventCollaboratorResponse, EventCollaboratorRequest } from '../types';

export const useEventCollaborators = (eventId: string, token: string) => {
  const [collaborators, setCollaborators] = useState<EventCollaboratorResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);

  const fetchCollaborators = useCallback(async (page: number = 0, size: number = 20) => {
    if (!eventId || !token) return;

    try {
      setLoading(true);
      setError(null);

      const response = await fetch(
        `/api/v1/events/${eventId}/collaborators?page=${page}&size=${size}`,
        {
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          }
        }
      );

      if (!response.ok) {
        throw new Error(`Failed to fetch collaborators: ${response.statusText}`);
      }

      const data: EventCollaboratorResponse[] = await response.json();
      setCollaborators(page === 0 ? data : prev => [...prev, ...data]);
      setCurrentPage(page);
      setHasMore(data.length === size);
    } catch (err) {
      setError(err as Error);
    } finally {
      setLoading(false);
    }
  }, [eventId, token]);

  const addCollaborator = useCallback(async (request: EventCollaboratorRequest) => {
    try {
      setLoading(true);
      setError(null);

      const response = await fetch(
        `/api/v1/events/${eventId}/collaborators`,
        {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(request)
        }
      );

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || 'Failed to add collaborator');
      }

      const newCollaborator: EventCollaboratorResponse = await response.json();
      setCollaborators(prev => [newCollaborator, ...prev]);
      return newCollaborator;
    } catch (err) {
      setError(err as Error);
      throw err;
    } finally {
      setLoading(false);
    }
  }, [eventId, token]);

  const updateCollaborator = useCallback(async (
    collaboratorId: string,
    updates: Partial<EventCollaboratorRequest>
  ) => {
    try {
      setLoading(true);
      setError(null);

      const response = await fetch(
        `/api/v1/events/${eventId}/collaborators/${collaboratorId}`,
        {
          method: 'PUT',
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(updates)
        }
      );

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || 'Failed to update collaborator');
      }

      const updated: EventCollaboratorResponse = await response.json();
      setCollaborators(prev =>
        prev.map(c => c.collaboratorId === collaboratorId ? updated : c)
      );
      return updated;
    } catch (err) {
      setError(err as Error);
      throw err;
    } finally {
      setLoading(false);
    }
  }, [eventId, token]);

  const removeCollaborator = useCallback(async (collaboratorId: string) => {
    try {
      setLoading(true);
      setError(null);

      const response = await fetch(
        `/api/v1/events/${eventId}/collaborators/${collaboratorId}`,
        {
          method: 'DELETE',
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          }
        }
      );

      if (response.status !== 204 && !response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || 'Failed to remove collaborator');
      }

      setCollaborators(prev => prev.filter(c => c.collaboratorId !== collaboratorId));
    } catch (err) {
      setError(err as Error);
      throw err;
    } finally {
      setLoading(false);
    }
  }, [eventId, token]);

  useEffect(() => {
    fetchCollaborators(0);
  }, [fetchCollaborators]);

  return {
    collaborators,
    loading,
    error,
    currentPage,
    hasMore,
    fetchCollaborators,
    addCollaborator,
    updateCollaborator,
    removeCollaborator,
    refresh: () => fetchCollaborators(0)
  };
};
```

---

### 2. Collaborators List Component

```typescript
// components/events/CollaboratorsList.tsx
import React from 'react';
import { useEventCollaborators } from '../../hooks/useEventCollaborators';
import { EventCollaboratorResponse } from '../../types';

interface Props {
  eventId: string;
  token: string;
}

export const CollaboratorsList: React.FC<Props> = ({ eventId, token }) => {
  const {
    collaborators,
    loading,
    error,
    removeCollaborator,
    updateCollaborator
  } = useEventCollaborators(eventId, token);

  const handleRemove = async (collaboratorId: string) => {
    if (window.confirm('Are you sure you want to remove this collaborator?')) {
      try {
        await removeCollaborator(collaboratorId);
      } catch (err) {
        alert('Failed to remove collaborator');
      }
    }
  };

  const handleRoleChange = async (
    collaboratorId: string,
    newRole: EventUserType
  ) => {
    try {
      await updateCollaborator(collaboratorId, { role: newRole });
    } catch (err) {
      alert('Failed to update collaborator role');
    }
  };

  if (loading && collaborators.length === 0) {
    return <div>Loading collaborators...</div>;
  }

  if (error) {
    return <div>Error: {error.message}</div>;
  }

  return (
    <div className="collaborators-list">
      <h2>Event Collaborators</h2>
      
      {collaborators.length === 0 ? (
        <p>No collaborators yet. Add one to get started!</p>
      ) : (
        <ul>
          {collaborators.map(collaborator => (
            <li key={collaborator.collaboratorId} className="collaborator-item">
              <div className="collaborator-info">
                <strong>{collaborator.userName || collaborator.email}</strong>
                <span className="role-badge">{collaborator.role}</span>
              </div>
              
              <div className="collaborator-actions">
                <select
                  value={collaborator.role}
                  onChange={(e) => handleRoleChange(
                    collaborator.collaboratorId,
                    e.target.value as EventUserType
                  )}
                >
                  <option value="ORGANIZER">Organizer</option>
                  <option value="COORDINATOR">Coordinator</option>
                  <option value="STAFF">Staff</option>
                  <option value="ATTENDEE">Attendee</option>
                </select>
                
                <button
                  onClick={() => handleRemove(collaborator.collaboratorId)}
                  className="btn-danger"
                >
                  Remove
                </button>
              </div>
              
              {collaborator.notes && (
                <p className="notes">{collaborator.notes}</p>
              )}
              
              {collaborator.invitationSent && (
                <span className="invitation-sent">
                  ✓ Invitation sent {collaborator.invitationSentAt && 
                    new Date(collaborator.invitationSentAt).toLocaleDateString()}
                </span>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
};
```

---

### 3. Add Collaborator Form Component

```typescript
// components/events/AddCollaboratorForm.tsx
import React, { useState } from 'react';
import { useEventCollaborators } from '../../hooks/useEventCollaborators';
import { EventUserType } from '../../types';

interface Props {
  eventId: string;
  token: string;
  onSuccess?: () => void;
}

export const AddCollaboratorForm: React.FC<Props> = ({ eventId, token, onSuccess }) => {
  const { addCollaborator, loading, error } = useEventCollaborators(eventId, token);
  const [formData, setFormData] = useState({
    userId: '',
    email: '',
    role: 'COORDINATOR' as EventUserType,
    permissions: [] as string[],
    notes: '',
    sendInvitation: true,
    invitationMessage: ''
  });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    try {
      await addCollaborator(formData);
      // Reset form
      setFormData({
        userId: '',
        email: '',
        role: 'COORDINATOR',
        permissions: [],
        notes: '',
        sendInvitation: true,
        invitationMessage: ''
      });
      onSuccess?.();
    } catch (err) {
      // Error is handled by the hook
      console.error('Failed to add collaborator:', err);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="add-collaborator-form">
      <h3>Add Collaborator</h3>
      
      {error && (
        <div className="error-message">
          {error.message}
        </div>
      )}

      <div className="form-group">
        <label>
          User ID (UUID):
          <input
            type="text"
            value={formData.userId}
            onChange={(e) => setFormData({ ...formData, userId: e.target.value })}
            required
            placeholder="user-uuid-here"
          />
        </label>
      </div>

      <div className="form-group">
        <label>
          Email:
          <input
            type="email"
            value={formData.email}
            onChange={(e) => setFormData({ ...formData, email: e.target.value })}
            required
            placeholder="collaborator@example.com"
          />
        </label>
      </div>

      <div className="form-group">
        <label>
          Role:
          <select
            value={formData.role}
            onChange={(e) => setFormData({ ...formData, role: e.target.value as EventUserType })}
            required
          >
            <option value="ORGANIZER">Organizer</option>
            <option value="COORDINATOR">Coordinator</option>
            <option value="STAFF">Staff</option>
            <option value="ATTENDEE">Attendee</option>
          </select>
        </label>
      </div>

      <div className="form-group">
        <label>
          Notes (optional):
          <textarea
            value={formData.notes}
            onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
            placeholder="Additional notes about this collaborator"
          />
        </label>
      </div>

      <div className="form-group">
        <label>
          <input
            type="checkbox"
            checked={formData.sendInvitation}
            onChange={(e) => setFormData({ ...formData, sendInvitation: e.target.checked })}
          />
          Send invitation email
        </label>
      </div>

      {formData.sendInvitation && (
        <div className="form-group">
          <label>
            Invitation Message (optional):
            <textarea
              value={formData.invitationMessage}
              onChange={(e) => setFormData({ ...formData, invitationMessage: e.target.value })}
              placeholder="Custom invitation message"
            />
        </label>
        </div>
      )}

      <button type="submit" disabled={loading}>
        {loading ? 'Adding...' : 'Add Collaborator'}
      </button>
    </form>
  );
};
```

---

### 4. Complete Collaborators Management Component

```typescript
// components/events/EventCollaborators.tsx
import React, { useState } from 'react';
import { CollaboratorsList } from './CollaboratorsList';
import { AddCollaboratorForm } from './AddCollaboratorForm';

interface Props {
  eventId: string;
  token: string;
}

export const EventCollaborators: React.FC<Props> = ({ eventId, token }) => {
  const [showAddForm, setShowAddForm] = useState(false);

  return (
    <div className="event-collaborators">
      <div className="header">
        <h2>Collaborators</h2>
        <button onClick={() => setShowAddForm(!showAddForm)}>
          {showAddForm ? 'Cancel' : 'Add Collaborator'}
        </button>
      </div>

      {showAddForm && (
        <AddCollaboratorForm
          eventId={eventId}
          token={token}
          onSuccess={() => setShowAddForm(false)}
        />
      )}

      <CollaboratorsList eventId={eventId} token={token} />
    </div>
  );
};
```

---

## 🐛 Error Handling

```typescript
try {
  const collaborator = await addCollaborator(eventId, {
    userId: 'user-id',
    email: 'collaborator@example.com',
    role: 'COORDINATOR'
  });
} catch (error) {
  if (error.message.includes('403')) {
    // User doesn't have permission (not owner/admin)
    showError('Only event owners and admins can add collaborators');
  } else if (error.message.includes('400')) {
    // Invalid request data
    showError('Please check your input and try again');
  } else if (error.message.includes('401')) {
    // Unauthorized - redirect to login
    redirectToLogin();
  } else {
    // Other errors
    showError('Failed to add collaborator. Please try again.');
  }
}
```

---

## 📊 Response Examples

### Get Collaborators Response

```json
[
  {
    "collaboratorId": "123e4567-e89b-12d3-a456-426614174000",
    "eventId": "event-uuid-here",
    "userId": "user-uuid-here",
    "email": "collaborator@example.com",
    "userName": "John Doe",
    "role": "COORDINATOR",
    "permissions": ["read", "write"],
    "registrationStatus": "CONFIRMED",
    "notes": "Helping with event logistics",
    "invitationSent": true,
    "invitationSentAt": "2024-11-15T10:30:00",
    "addedAt": "2024-11-15T10:00:00",
    "updatedAt": "2024-11-15T10:30:00"
  }
]
```

---

## 🎯 Best Practices

### 1. Permission Checks

```typescript
// ✅ Good - Check permissions before showing UI
const { isEventOwner, isAdmin } = useEventPermissions(eventId, token);

{isEventOwner || isAdmin ? (
  <AddCollaboratorForm eventId={eventId} token={token} />
) : (
  <p>Only event owners can add collaborators</p>
)}
```

### 2. Optimistic Updates

```typescript
// ✅ Good - Update UI immediately, rollback on error
const addCollaboratorOptimistic = async (request: EventCollaboratorRequest) => {
  const tempId = `temp-${Date.now()}`;
  const tempCollaborator: EventCollaboratorResponse = {
    collaboratorId: tempId,
    ...request,
    addedAt: new Date().toISOString()
  };

  // Add optimistically
  setCollaborators(prev => [tempCollaborator, ...prev]);

  try {
    const realCollaborator = await addCollaborator(eventId, request);
    // Replace temp with real
    setCollaborators(prev =>
      prev.map(c => c.collaboratorId === tempId ? realCollaborator : c)
    );
  } catch (err) {
    // Rollback on error
    setCollaborators(prev => prev.filter(c => c.collaboratorId !== tempId));
    throw err;
  }
};
```

### 3. Validation

```typescript
// ✅ Good - Validate before sending
const validateCollaboratorRequest = (request: EventCollaboratorRequest): string[] => {
  const errors: string[] = [];

  if (!request.userId || !isValidUUID(request.userId)) {
    errors.push('Valid user ID is required');
  }

  if (!request.email || !isValidEmail(request.email)) {
    errors.push('Valid email is required');
  }

  if (!request.role || !['ORGANIZER', 'COORDINATOR', 'STAFF', 'ATTENDEE'].includes(request.role)) {
    errors.push('Valid role is required');
  }

  return errors;
};
```

---

## 🚀 Quick Start Checklist

- [ ] Add TypeScript types for `EventUserType`, `EventCollaboratorRequest`, `EventCollaboratorResponse`
- [ ] Create `useEventCollaborators` hook
- [ ] Create `CollaboratorsList` component
- [ ] Create `AddCollaboratorForm` component
- [ ] Add permission checks (only owners/admins can manage)
- [ ] Implement error handling for 403/401 responses
- [ ] Add loading states and optimistic updates
- [ ] Test with different user roles

---

## 📝 Summary

### Key Features

1. **View Collaborators**: Paginated list of all event collaborators
2. **Add Collaborators**: Add new collaborators with roles and permissions
3. **Update Collaborators**: Modify collaborator information
4. **Remove Collaborators**: Remove collaborators from events

### Access Control

- **View**: Users with event access
- **Manage**: Only event owners and system admins

### Benefits

- ✅ Role-based access control for event management
- ✅ Flexible permission system
- ✅ Optional email invitations
- ✅ Full CRUD operations for collaborators

---

**Happy Coding! 🎉**

