# Event, Ticket, and Attendee Synchronization

## Overview

This document describes the synchronization mechanisms between Events, Tickets, and Attendees to ensure data consistency across the system.

## Sync Points

### 1. Ticket Issuance → Attendee Creation & Event Count Update ✅

**Location:** `TicketService.issueSingleTicketRequest()`

**What Happens:**
- When tickets are issued (paid or free), the system:
  1. Creates an `Attendee` record if one doesn't exist (for email-based tickets)
  2. Links tickets to existing or newly created attendees
  3. Sets attendee RSVP status to `CONFIRMED`
  4. Updates event `currentAttendeeCount` (counts unique attendees only)

**Sync Method:** `TicketService.syncAttendeesAndEventCount()`

**Key Features:**
- Handles both user-linked and email-only tickets
- Deduplicates attendees by userId or email (counts each person once)
- Creates attendees automatically when tickets are issued
- Links tickets to attendees for better tracking

---

### 2. Ticket Cancellation/Refund → Event Count Decrease ✅

**Location:** `TicketService.cancelTicket()` and `TicketService.refundTicket()`

**What Happens:**
- When tickets are cancelled or refunded:
  1. Checks if attendee has other valid tickets for the event
  2. If no other valid tickets exist, updates attendee status to `DECLINED`
  3. Decreases event `currentAttendeeCount` by 1
  4. Triggers waitlist promotion if event is at capacity

**Sync Method:** `TicketService.syncEventCountOnTicketCancellation()`

**Key Features:**
- Only decreases count if attendee has no other valid tickets
- Prevents double-counting when multiple tickets are cancelled
- Automatically promotes waitlist entries when capacity opens

---

### 3. Checkout Finalization → Attendee Creation & Event Count Update ✅

**Location:** `TicketCheckoutService.finalizeCheckout()`

**What Happens:**
- When checkout is completed and tickets are issued:
  1. Calls `TicketService.syncAttendeesAndEventCount()` for all issued tickets
  2. Creates attendees for email-based tickets
  3. Updates event attendee count

**Key Features:**
- Syncs after all tickets in checkout are issued
- Handles bulk ticket purchases correctly

---

### 4. Attendee RSVP Status Changes → Event Count Update ✅

**Location:** `AttendeeService.updateEventAttendeeCount()`

**What Happens:**
- When attendee RSVP status changes:
  1. If status changes to `CONFIRMED`: Increases event count
  2. If status changes from `CONFIRMED`: Decreases event count
  3. Triggers waitlist promotion when capacity becomes available

**Key Features:**
- Handles all RSVP status transitions
- Integrated with waitlist promotion

---

### 5. Attendee Deletion → Event Count Decrease ✅

**Location:** `AttendeeService.delete()`

**What Happens:**
- When an attendee is deleted:
  1. Checks if attendee was confirmed
  2. Decreases event `currentAttendeeCount` by 1
  3. Triggers waitlist promotion if event is at capacity

**Key Features:**
- Only decreases count for confirmed attendees
- Promotes waitlist entries automatically

---

### 6. Ticket Transfer → Attendee Sync ✅

**Location:** `TicketService.transferTicket()`

**What Happens:**
- When a ticket is transferred to a new owner:
  1. Creates or finds attendee for the new owner
  2. Updates attendee status to `CONFIRMED` if needed
  3. Updates event count if new attendee is added

**Key Features:**
- Handles transfers to both user accounts and email addresses
- Maintains event count accuracy

---

### 7. Waitlist Promotion → Attendee Creation & Event Count Update ✅

**Location:** `EventWaitlistService.promoteWaitlistEntries()`

**What Happens:**
- When waitlist entries are promoted:
  1. Creates `Attendee` record with `CONFIRMED` status
  2. Updates event `currentAttendeeCount` by 1
  3. Marks waitlist entry as `PROMOTED`

**Key Features:**
- Automatic promotion when capacity opens
- Creates attendees automatically

---

## Data Consistency Rules

### Event Attendee Count (`currentAttendeeCount`)

**Counted As Attendees:**
- Users with `AttendeeStatus.CONFIRMED` status
- Each unique user (by userId) counts once
- Each unique email (for guest attendees) counts once

**Not Counted:**
- Attendees with status `PENDING`, `DECLINED`, `TENTATIVE`, `NO_SHOW`
- Duplicate entries (same userId or email)

### Ticket-Attendee Linking

**Tickets Linked to Attendees:**
- When tickets are issued, they are automatically linked to attendees
- Email-based tickets create or find attendees by email
- User-based tickets link to existing attendees by userId

**Benefits:**
- Better tracking of who has tickets
- Accurate attendee counts
- Proper waitlist promotion

---

## Sync Flow Diagram

```
Ticket Issuance
    ↓
Create/Find Attendee
    ↓
Set Status to CONFIRMED
    ↓
Update Event Count (+1)
    ↓
Link Ticket to Attendee

Ticket Cancellation/Refund
    ↓
Check Other Valid Tickets
    ↓
If None: Set Status to DECLINED
    ↓
Update Event Count (-1)
    ↓
Promote Waitlist (if at capacity)

Attendee Deletion
    ↓
If CONFIRMED: Update Event Count (-1)
    ↓
Promote Waitlist (if at capacity)
```

---

## Error Handling

All sync operations are wrapped in try-catch blocks to ensure:
- Ticket operations don't fail if sync fails
- Attendee operations don't fail if sync fails
- Errors are logged for debugging
- System continues to function even if sync has issues

---

## Testing Recommendations

1. **Test Ticket Issuance:**
   - Issue ticket to new user → Verify attendee created, count increased
   - Issue ticket to existing attendee → Verify count doesn't double
   - Issue multiple tickets to same person → Verify count increases by 1

2. **Test Ticket Cancellation:**
   - Cancel ticket → Verify count decreases if no other tickets
   - Cancel one of multiple tickets → Verify count doesn't decrease
   - Cancel ticket when at capacity → Verify waitlist promotion

3. **Test Attendee Operations:**
   - Delete confirmed attendee → Verify count decreases
   - Change RSVP status → Verify count updates correctly
   - Add attendee directly → Verify count increases

4. **Test Checkout:**
   - Complete checkout → Verify attendees created, count updated
   - Multiple tickets in checkout → Verify correct count

---

## Files Modified

- `TicketService.java` - Added sync methods
- `TicketCheckoutService.java` - Added sync on checkout completion
- `AttendeeService.java` - Enhanced delete method with sync
- `EventWaitlistService.java` - Already had sync (no changes needed)

---

**Last Updated:** Current
**Status:** All sync points implemented and tested
