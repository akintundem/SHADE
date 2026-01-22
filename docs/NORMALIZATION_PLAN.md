# Database Normalization Plan - Performance-Focused

## Objective
Normalize database to improve data integrity while **maintaining performance**. We will fix true normalization violations (JSON fields, repeating groups) while **keeping denormalized counts and aggregates** for speed.

---

## What We'll Normalize

### 1. User Preferences (1NF Violation → Fixed)

**Current Problem**: `auth_users.preferences` stores JSON
```sql
auth_users
├─ preferences: TEXT (JSON blob)
```

**Solution**: New `user_preferences` table
```sql
user_preferences
├─ id (PK)
├─ user_id (FK → auth_users)
├─ preference_key (VARCHAR) - e.g., "theme", "language", "notifications_email"
├─ preference_value (VARCHAR)
├─ UNIQUE(user_id, preference_key)
```

**Benefits**:
- ✅ Queryable individual preferences
- ✅ Indexable for fast lookups
- ✅ Type-safe validation per key
- ✅ No JSON parsing overhead

---

### 2. Notification Settings (1NF Violation → Fixed)

**Current Problem**: `user_settings.notification_preferences` as JSON

**Solution**: New `user_notification_preferences` table
```sql
user_notification_preferences
├─ id (PK)
├─ user_id (FK → auth_users)
├─ notification_type (VARCHAR) - e.g., "EVENT_REMINDER", "TICKET_SOLD", "NEW_POST"
├─ channel (VARCHAR) - EMAIL, PUSH, SMS
├─ enabled (BOOLEAN)
├─ UNIQUE(user_id, notification_type, channel)
```

---

### 3. Privacy Settings (1NF Violation → Fixed)

**Current Problem**: `user_settings.privacy_settings` as JSON

**Solution**: New `user_privacy_settings` table
```sql
user_privacy_settings
├─ id (PK)
├─ user_id (FK → auth_users)
├─ setting_key (VARCHAR) - e.g., "profile_visibility", "email_visible", "followers_visible"
├─ setting_value (VARCHAR)
├─ UNIQUE(user_id, setting_key)
```

---

### 4. Event Metadata (1NF Violation → Fixed)

**Current Problem**: `events.metadata` stores arbitrary JSON

**Solution**: New `event_metadata` table
```sql
event_metadata
├─ id (PK)
├─ event_id (FK → events)
├─ metadata_key (VARCHAR)
├─ metadata_value (TEXT)
├─ metadata_type (VARCHAR) - STRING, NUMBER, BOOLEAN, JSON
├─ UNIQUE(event_id, metadata_key)
```

---

### 5. Venues (3NF Violation → Fixed)

**Current Problem**: `events.venue` stores JSON with address, coordinates, etc.

**Solution**: Proper `venues` table
```sql
venues
├─ id (PK)
├─ name (VARCHAR)
├─ address_line1 (VARCHAR)
├─ address_line2 (VARCHAR)
├─ city (VARCHAR)
├─ state (VARCHAR)
├─ country (VARCHAR)
├─ postal_code (VARCHAR)
├─ latitude (DECIMAL)
├─ longitude (DECIMAL)
├─ capacity (INT)
├─ venue_type (VARCHAR)
├─ accessibility_features (TEXT)
├─ created_at, updated_at, deleted_at
```

**Events Reference**:
```sql
events
├─ venue_id (FK → venues) - nullable
```

**Benefits**:
- ✅ Reusable venues across events
- ✅ Searchable by location
- ✅ Indexable coordinates for geo queries
- ✅ No JSON parsing

---

### 6. Ticket Metadata (1NF Violation → Fixed)

**Current Problem**: `tickets.metadata`, `ticket_types.metadata` as TEXT/JSON

**Solution**: New `ticket_metadata` and `ticket_type_metadata` tables
```sql
ticket_metadata
├─ id (PK)
├─ ticket_id (FK → tickets)
├─ metadata_key (VARCHAR)
├─ metadata_value (TEXT)
├─ UNIQUE(ticket_id, metadata_key)

ticket_type_metadata
├─ id (PK)
├─ ticket_type_id (FK → ticket_types)
├─ metadata_key (VARCHAR)
├─ metadata_value (TEXT)
├─ UNIQUE(ticket_type_id, metadata_key)
```

---

### 7. Currency Normalization (2NF Violation → Fixed)

**Current Problem**: `ticket_types.currency` and `budgets.currency` repeat currency codes

**Solution**: New `currencies` reference table
```sql
currencies
├─ code (VARCHAR(3), PK) - e.g., "USD", "EUR", "GBP"
├─ name (VARCHAR) - "US Dollar", "Euro"
├─ symbol (VARCHAR) - "$", "€"
├─ decimal_places (INT) - 2 for most, 0 for JPY
├─ is_active (BOOLEAN)
```

**References**:
```sql
ticket_types.currency → currencies.code (FK)
budgets.currency → currencies.code (FK)
ticket_price_tiers.currency → currencies.code (FK)
```

**Benefits**:
- ✅ Centralized currency management
- ✅ Validation (only valid currency codes)
- ✅ Metadata (symbols, decimal places) in one place

---

## What We'll KEEP Denormalized (Performance)

### ✅ Keep As-Is (Critical for Performance)

1. **Denormalized Counts**:
   - `ticket_types.quantity_sold` - Avoid COUNT on millions of tickets
   - `ticket_types.quantity_reserved` - Real-time availability
   - `events.current_attendee_count` - Fast dashboard
   - `event_posts.repost_count` - Feed performance
   - `tasks.completed_subtasks_count` - Progress tracking
   - `budgets.total_revenue` - Financial dashboards
   - `budgets.projected_revenue` - Planning analytics
   - `budgets.total_actual` - Budget tracking
   - `budgets.total_estimated` - Planning

2. **Computed Fields**:
   - `budgets.variance` - Computed but cached
   - `budgets.net_position` - Computed but cached
   - `budget_categories.total_actual` - Category rollups

3. **Status/Temporal Fields**:
   - `events.is_archived` - Fast filtering
   - `events.timeline_published` - Authorization checks
   - `tickets.status` - State machine tracking

**Why Keep Them?**
- Avoid expensive aggregation queries (COUNT, SUM)
- Real-time dashboards need instant responses
- Updated transactionally when source data changes
- Indexed for fast filtering

---

## Implementation Strategy

### Phase 1: New Tables (Additive - No Breaking Changes)

**Step 1**: Create new normalized tables
- `user_preferences`
- `user_notification_preferences`
- `user_privacy_settings`
- `event_metadata`
- `venues`
- `ticket_metadata`
- `ticket_type_metadata`
- `currencies`

**Step 2**: Migrate existing data
- Parse JSON from old columns
- Insert into new tables
- Keep old columns temporarily for rollback

**Step 3**: Update application code
- Create new entities
- Add repositories
- Update services to use new tables
- Dual-write to both old and new (transition period)

### Phase 2: Deprecate Old Columns (After Verification)

**Step 4**: Switch reads to new tables
- Update all service methods
- Remove dual-write

**Step 5**: Drop old columns
- `ALTER TABLE auth_users DROP COLUMN preferences`
- `ALTER TABLE user_settings DROP COLUMN notification_preferences`
- `ALTER TABLE user_settings DROP COLUMN privacy_settings`
- `ALTER TABLE events DROP COLUMN metadata`
- `ALTER TABLE events DROP COLUMN venue` (keep venue_id)
- `ALTER TABLE tickets DROP COLUMN metadata`

---

## Migration Script Outline

### V12__normalize_user_preferences.sql
```sql
-- Create user_preferences table
CREATE TABLE IF NOT EXISTS user_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    preference_key VARCHAR(100) NOT NULL,
    preference_value VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_preferences_user FOREIGN KEY (user_id) REFERENCES auth_users(id) ON DELETE CASCADE,
    CONSTRAINT uk_user_preferences UNIQUE (user_id, preference_key)
);

CREATE INDEX idx_user_preferences_user ON user_preferences(user_id);
CREATE INDEX idx_user_preferences_key ON user_preferences(preference_key);

-- Migrate existing preferences from JSON
-- (Custom migration code will parse JSON and insert)
```

### V13__normalize_notification_settings.sql
```sql
-- Create user_notification_preferences
CREATE TABLE IF NOT EXISTS user_notification_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_notif_prefs_user FOREIGN KEY (user_id) REFERENCES auth_users(id) ON DELETE CASCADE,
    CONSTRAINT uk_user_notif_prefs UNIQUE (user_id, notification_type, channel)
);

CREATE INDEX idx_user_notif_prefs_user ON user_notification_preferences(user_id);
```

### V14__normalize_venues.sql
```sql
-- Create venues table
CREATE TABLE IF NOT EXISTS venues (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    country VARCHAR(100),
    postal_code VARCHAR(20),
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    capacity INT,
    venue_type VARCHAR(50),
    accessibility_features TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_venues_location ON venues(city, state, country);
CREATE INDEX idx_venues_coordinates ON venues(latitude, longitude);

-- Add venue_id to events
ALTER TABLE events ADD COLUMN IF NOT EXISTS venue_id UUID;
ALTER TABLE events ADD CONSTRAINT fk_events_venue FOREIGN KEY (venue_id) REFERENCES venues(id) ON DELETE SET NULL;

CREATE INDEX idx_events_venue ON events(venue_id);
```

### V15__normalize_currencies.sql
```sql
-- Create currencies reference table
CREATE TABLE IF NOT EXISTS currencies (
    code VARCHAR(3) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    symbol VARCHAR(10),
    decimal_places INT NOT NULL DEFAULT 2,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert common currencies
INSERT INTO currencies (code, name, symbol, decimal_places) VALUES
('USD', 'US Dollar', '$', 2),
('EUR', 'Euro', '€', 2),
('GBP', 'British Pound', '£', 2),
('JPY', 'Japanese Yen', '¥', 0),
('CAD', 'Canadian Dollar', 'CA$', 2),
('AUD', 'Australian Dollar', 'A$', 2),
('NGN', 'Nigerian Naira', '₦', 2)
ON CONFLICT (code) DO NOTHING;

-- Add foreign keys (keep existing currency columns for compatibility)
-- Migration will ensure only valid currency codes
```

### V16__normalize_metadata.sql
```sql
-- Event metadata
CREATE TABLE IF NOT EXISTS event_metadata (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL,
    metadata_key VARCHAR(100) NOT NULL,
    metadata_value TEXT,
    metadata_type VARCHAR(20) DEFAULT 'STRING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_event_metadata_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    CONSTRAINT uk_event_metadata UNIQUE (event_id, metadata_key)
);

CREATE INDEX idx_event_metadata_event ON event_metadata(event_id);
CREATE INDEX idx_event_metadata_key ON event_metadata(metadata_key);

-- Ticket metadata
CREATE TABLE IF NOT EXISTS ticket_metadata (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id UUID NOT NULL,
    metadata_key VARCHAR(100) NOT NULL,
    metadata_value TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ticket_metadata_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE,
    CONSTRAINT uk_ticket_metadata UNIQUE (ticket_id, metadata_key)
);

CREATE INDEX idx_ticket_metadata_ticket ON ticket_metadata(ticket_id);

-- Ticket type metadata
CREATE TABLE IF NOT EXISTS ticket_type_metadata (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_type_id UUID NOT NULL,
    metadata_key VARCHAR(100) NOT NULL,
    metadata_value TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ticket_type_metadata_type FOREIGN KEY (ticket_type_id) REFERENCES ticket_types(id) ON DELETE CASCADE,
    CONSTRAINT uk_ticket_type_metadata UNIQUE (ticket_type_id, metadata_key)
);

CREATE INDEX idx_ticket_type_metadata_type ON ticket_type_metadata(ticket_type_id);
```

---

## Entity Class Changes

### New Entities to Create

1. `UserPreference.java`
2. `UserNotificationPreference.java`
3. `UserPrivacySetting.java`
4. `EventMetadata.java`
5. `Venue.java`
6. `TicketMetadata.java`
7. `TicketTypeMetadata.java`
8. `Currency.java`

### Entities to Update

1. `UserAccount.java` - Remove `preferences`, add `@OneToMany preferences`
2. `UserSettings.java` - Remove JSON fields, add relationships
3. `Event.java` - Remove `metadata`, `venue` JSON, add `venue_id` FK
4. `Ticket.java` - Remove `metadata` TEXT, add `@OneToMany metadata`
5. `TicketType.java` - Remove `metadata` TEXT, add `@OneToMany metadata`

---

## Service Layer Changes

### UserService
- Add methods: `getUserPreference(userId, key)`, `setUserPreference(userId, key, value)`
- Add methods: `getNotificationPreferences(userId)`, `updateNotificationPreference(...)`
- Add methods: `getPrivacySetting(userId, key)`, `setPrivacySetting(...)`

### EventService
- Add methods: `getEventMetadata(eventId, key)`, `setEventMetadata(...)`
- Update venue handling to use Venue entity

### VenueService (New)
- `createVenue(VenueRequest)`
- `updateVenue(venueId, VenueRequest)`
- `searchVenues(city, state, country)`
- `getVenuesNearLocation(lat, lng, radius)`

### TicketService
- Add methods for ticket metadata management

---

## Performance Impact Assessment

### Positive Impacts ✅
- **Faster preference lookups**: Indexed queries vs JSON parsing
- **Better query optimization**: Database can optimize relational queries
- **Geo queries**: Indexed lat/lng for venue search
- **Reduced payload**: Fetch only needed preferences, not entire JSON blob

### Neutral (No Change) ⚡
- **Denormalized counts**: Still instant (kept as-is)
- **Dashboard queries**: Same speed (counts unchanged)
- **List operations**: Same pagination performance

### Minimal Overhead ⚠️
- **Preference reads**: 1 additional join (negligible with indexes)
- **Event with venue**: 1 join to venues table (indexed FK)
- **Metadata access**: Key-value lookup (indexed, very fast)

### Mitigation Strategies
- **Eager loading**: Use `@EntityGraph` for preferences when needed
- **Caching**: Cache frequently accessed preferences (theme, language)
- **Batch loading**: Load all user preferences in one query when profile loads

---

## Rollback Plan

If issues arise:
1. Old columns remain until Phase 2
2. Can switch back to reading from JSON columns
3. Dual-write ensures data consistency
4. Migration scripts are idempotent

---

## Success Metrics

After normalization:
- ✅ No JSON fields for structured data
- ✅ All tables in at least 3NF
- ✅ BCNF for non-performance-critical tables
- ✅ Query performance maintained or improved
- ✅ Code is cleaner and more maintainable
- ✅ Better data integrity constraints

---

## Summary

**Normalize**: User prefs, notifications, privacy, metadata, venues, currencies
**Keep Denormalized**: Counts, totals, computed aggregates (performance-critical)
**Result**: Best of both worlds - data integrity + speed
