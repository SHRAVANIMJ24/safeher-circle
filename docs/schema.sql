-- SafeHer Circle — database schema
-- PostgreSQL 14+
--
-- Design note on anonymity: a user's real identity (phone, email) lives only in
-- `users`. Everything public references `anon_handle`. Posts store an area name
-- and a coarse centroid, never the device's exact coordinates.
--
-- Coordinates are plain NUMERIC(9,6) columns, not PostGIS geometry. Distance
-- filtering is done in the service layer with the haversine formula. If the
-- volume ever justifies it, these columns migrate cleanly to PostGIS later.

-- PostGIS not required: coordinates are stored as plain numeric columns.
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";


-- ============================================================
-- 1. IDENTITY
-- ============================================================

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    phone           VARCHAR(20)  UNIQUE,          -- for SOS verification
    email           VARCHAR(255) UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    anon_handle     VARCHAR(50)  UNIQUE NOT NULL, -- e.g. "quiet-lark-4471"
    display_city    VARCHAR(100),
    is_verified     BOOLEAN      NOT NULL DEFAULT FALSE,
    is_banned       BOOLEAN      NOT NULL DEFAULT FALSE,
    role            VARCHAR(20)  NOT NULL DEFAULT 'USER',  -- USER | MODERATOR | ADMIN | NGO
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_seen_at    TIMESTAMPTZ,
    CONSTRAINT contact_required CHECK (phone IS NOT NULL OR email IS NOT NULL)
);

CREATE INDEX idx_users_handle ON users(anon_handle);


CREATE TABLE trusted_contacts (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    phone           VARCHAR(20)  NOT NULL,
    relationship    VARCHAR(50),
    priority        SMALLINT     NOT NULL DEFAULT 1,   -- who gets contacted first
    notify_by_sms   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_contacts_user ON trusted_contacts(user_id);
-- Cap of five contacts is enforced in the service layer, not here,
-- so the error message can be readable.


-- ============================================================
-- 2. COMMUNITY POSTS
-- ============================================================

CREATE TABLE categories (
    id              SERIAL PRIMARY KEY,
    slug            VARCHAR(50)  UNIQUE NOT NULL,
    label           VARCHAR(100) NOT NULL,
    description     TEXT,
    icon            VARCHAR(50),
    sort_order      SMALLINT NOT NULL DEFAULT 0
);

INSERT INTO categories (slug, label, sort_order) VALUES
    ('harassment',   'Harassment & stalking',      1),
    ('domestic',     'Domestic situations',        2),
    ('workplace',    'Workplace issues',           3),
    ('legal',        'Legal help',                 4),
    ('health',       'Health & menstrual health',  5),
    ('mental-health','Mental health',              6),
    ('financial',    'Financial hardship',         7),
    ('general',      'General support',            8);


CREATE TABLE posts (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    author_id       UUID REFERENCES users(id) ON DELETE SET NULL,
    author_handle   VARCHAR(50) NOT NULL,      -- denormalised so deleting a user keeps the post readable
    category_id     INT  NOT NULL REFERENCES categories(id),
    title           VARCHAR(200) NOT NULL,
    body            TEXT NOT NULL,

    -- Location: coarse only
    area_name       VARCHAR(150),              -- "Andheri West, Mumbai"
    city            VARCHAR(100),
    state           VARCHAR(100),
    approx_lat      NUMERIC(9,6),              -- snapped to ~1km grid before storing
    approx_lng      NUMERIC(9,6),

    urgency_score   SMALLINT DEFAULT 0,        -- 0-100, set by the ML service
    status          VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED', -- PENDING | PUBLISHED | FLAGGED | REMOVED
    upvote_count    INT NOT NULL DEFAULT 0,
    comment_count   INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_posts_category   ON posts(category_id);
CREATE INDEX idx_posts_city       ON posts(city);
CREATE INDEX idx_posts_created    ON posts(created_at DESC);
CREATE INDEX idx_posts_status     ON posts(status);
CREATE INDEX idx_posts_geo        ON posts(approx_lat, approx_lng);


CREATE TABLE comments (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    post_id         UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    author_id       UUID REFERENCES users(id) ON DELETE SET NULL,
    author_handle   VARCHAR(50) NOT NULL,
    body            TEXT NOT NULL,
    parent_id       UUID REFERENCES comments(id) ON DELETE CASCADE,  -- threaded replies
    status          VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_comments_post ON comments(post_id);


CREATE TABLE post_votes (
    post_id         UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (post_id, user_id)
);


-- ============================================================
-- 3. EMERGENCY ALERTS
-- ============================================================

CREATE TABLE sos_alerts (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    triggered_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved_at     TIMESTAMPTZ,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE | SAFE | CANCELLED | EXPIRED
    trigger_method  VARCHAR(30),               -- BUTTON | VOICE | SHAKE | TIMER
    alarm_type      VARCHAR(30),               -- SIREN | SCREAM | MALE_VOICE | SILENT
    exact_lat       NUMERIC(9,6),              -- exact location IS kept here, unlike posts
    exact_lng       NUMERIC(9,6),
    accuracy_meters REAL,
    note            TEXT
);

CREATE INDEX idx_sos_user   ON sos_alerts(user_id);
CREATE INDEX idx_sos_status ON sos_alerts(status);


-- Location breadcrumbs while an alert is live
CREATE TABLE sos_location_pings (
    id              BIGSERIAL PRIMARY KEY,
    alert_id        UUID NOT NULL REFERENCES sos_alerts(id) ON DELETE CASCADE,
    lat             NUMERIC(9,6) NOT NULL,
    lng             NUMERIC(9,6) NOT NULL,
    recorded_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pings_alert ON sos_location_pings(alert_id, recorded_at);


CREATE TABLE sos_notifications (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    alert_id        UUID NOT NULL REFERENCES sos_alerts(id) ON DELETE CASCADE,
    contact_id      UUID REFERENCES trusted_contacts(id) ON DELETE SET NULL,
    channel         VARCHAR(20) NOT NULL,      -- SMS | PUSH | EMAIL
    delivery_status VARCHAR(20) NOT NULL DEFAULT 'QUEUED', -- QUEUED | SENT | FAILED | ACKNOWLEDGED
    provider_ref    VARCHAR(100),              -- Twilio message SID
    sent_at         TIMESTAMPTZ,
    acknowledged_at TIMESTAMPTZ,
    error_message   TEXT
);

CREATE INDEX idx_notif_alert ON sos_notifications(alert_id);


-- ============================================================
-- 4. DONATION EXCHANGE
-- ============================================================

CREATE TABLE item_types (
    id              SERIAL PRIMARY KEY,
    slug            VARCHAR(50) UNIQUE NOT NULL,
    label           VARCHAR(100) NOT NULL,
    is_hygiene      BOOLEAN NOT NULL DEFAULT FALSE
);

INSERT INTO item_types (slug, label, is_hygiene) VALUES
    ('sanitary-pads',    'Sanitary pads',        TRUE),
    ('menstrual-cups',   'Menstrual cups',       TRUE),
    ('reusable-pads',    'Reusable cloth pads',  TRUE),
    ('toiletries',       'Soap & toiletries',    TRUE),
    ('clothing',         'Clothing',             FALSE),
    ('baby-items',       'Baby items',           FALSE),
    ('school-supplies',  'School supplies',      FALSE),
    ('medicine',         'Medicine',             FALSE);


CREATE TABLE listings (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID REFERENCES users(id) ON DELETE SET NULL,
    user_handle     VARCHAR(50) NOT NULL,
    listing_type    VARCHAR(10) NOT NULL,      -- OFFER | REQUEST
    item_type_id    INT NOT NULL REFERENCES item_types(id),
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    quantity        VARCHAR(100),              -- free text: "2 packs", "approx 20 pads"
    area_name       VARCHAR(150),
    city            VARCHAR(100),
    approx_lat      NUMERIC(9,6),
    approx_lng      NUMERIC(9,6),
    status          VARCHAR(20) NOT NULL DEFAULT 'OPEN',  -- OPEN | MATCHED | FULFILLED | EXPIRED
    expires_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_listings_type ON listings(listing_type, status);
CREATE INDEX idx_listings_city ON listings(city);
CREATE INDEX idx_listings_geo  ON listings(approx_lat, approx_lng);


CREATE TABLE listing_claims (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    listing_id      UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    claimant_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message         TEXT,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING | ACCEPTED | DECLINED | COMPLETED
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (listing_id, claimant_id)
);


-- ============================================================
-- 5. NGO & HELPLINE DIRECTORY
-- ============================================================

CREATE TABLE organisations (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name            VARCHAR(200) NOT NULL,
    org_type        VARCHAR(30) NOT NULL,      -- NGO | HELPLINE | SHELTER | LEGAL_AID | POLICE
    description     TEXT,
    phone           VARCHAR(50),
    alt_phone       VARCHAR(50),
    email           VARCHAR(255),
    website         VARCHAR(255),
    address         TEXT,
    city            VARCHAR(100),
    state           VARCHAR(100),
    lat             NUMERIC(9,6),
    lng             NUMERIC(9,6),
    services        TEXT[],                    -- {'counselling','shelter','legal'}
    is_24x7         BOOLEAN NOT NULL DEFAULT FALSE,
    is_verified     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orgs_city ON organisations(city);
CREATE INDEX idx_orgs_geo  ON organisations(lat, lng);


-- ============================================================
-- 6. MODERATION
-- ============================================================

CREATE TABLE reports (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    reporter_id     UUID REFERENCES users(id) ON DELETE SET NULL,
    target_type     VARCHAR(20) NOT NULL,      -- POST | COMMENT | LISTING | USER
    target_id       UUID NOT NULL,
    reason          VARCHAR(50) NOT NULL,      -- SPAM | ABUSE | FAKE | DOXXING | OTHER
    detail          TEXT,
    status          VARCHAR(20) NOT NULL DEFAULT 'OPEN',  -- OPEN | REVIEWING | ACTIONED | DISMISSED
    reviewed_by     UUID REFERENCES users(id) ON DELETE SET NULL,
    reviewed_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_reports_status ON reports(status);
CREATE INDEX idx_reports_target ON reports(target_type, target_id);


-- Output of the automated moderation service
CREATE TABLE moderation_scores (
    id              BIGSERIAL PRIMARY KEY,
    target_type     VARCHAR(20) NOT NULL,
    target_id       UUID NOT NULL,
    toxicity        REAL,                      -- 0.0 - 1.0
    urgency         REAL,
    predicted_category VARCHAR(50),
    model_version   VARCHAR(50),
    auto_action     VARCHAR(20),               -- NONE | FLAG | HOLD | BLOCK
    scored_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_modscore_target ON moderation_scores(target_type, target_id);


-- ============================================================
-- 7. AUDIT
-- ============================================================

CREATE TABLE audit_log (
    id              BIGSERIAL PRIMARY KEY,
    actor_id        UUID REFERENCES users(id) ON DELETE SET NULL,
    action          VARCHAR(100) NOT NULL,
    target_type     VARCHAR(50),
    target_id       UUID,
    metadata        JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_actor   ON audit_log(actor_id);
CREATE INDEX idx_audit_created ON audit_log(created_at DESC);
