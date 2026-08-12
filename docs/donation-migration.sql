-- Donation board — schema changes
--
-- Run against an existing database. These columns did not exist in the
-- original schema.sql; they came out of design decisions made while building
-- the module.

-- A second, separate pseudonym used only on the donation board.
--
-- Why: "I cannot afford pads this month" is a disclosure of poverty. If it
-- carried the same handle as someone's posts about a domestic situation, the
-- two become linkable. A separate handle keeps the donation board from
-- becoming a means of profiling the people using the rest of the site.
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS donation_handle VARCHAR(50) UNIQUE;

-- Requests can hide their description until someone makes contact.
--
-- The listing still appears in the city and the item counts, so the board can
-- show "3 people need pads in Andheri" — which is what prompts a donation —
-- without the detail of who is struggling being public.
ALTER TABLE listings
    ADD COLUMN IF NOT EXISTS detail_hidden BOOLEAN NOT NULL DEFAULT FALSE;

-- Whether this listing is handled by an individual or an organisation.
-- Organisation drop points are safer; person-to-person is more useful for
-- immediate neighbour help. Both are offered.
ALTER TABLE listings
    ADD COLUMN IF NOT EXISTS handled_by VARCHAR(20) NOT NULL DEFAULT 'INDIVIDUAL';

ALTER TABLE listings
    ADD COLUMN IF NOT EXISTS organisation_id UUID REFERENCES organisations(id);

CREATE INDEX IF NOT EXISTS idx_listings_org ON listings(organisation_id);

-- Messages exchanged after a claim, so arranging a handover does not require
-- either side to publish a phone number.
CREATE TABLE IF NOT EXISTS claim_messages (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    claim_id        UUID NOT NULL REFERENCES listing_claims(id) ON DELETE CASCADE,
    sender_id       UUID REFERENCES users(id) ON DELETE SET NULL,
    sender_handle   VARCHAR(50) NOT NULL,
    body            TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_claim_messages_claim
    ON claim_messages(claim_id, created_at);

-- Backfill donation handles for existing accounts. New accounts get one at
-- registration; this covers anyone who signed up before the column existed.
UPDATE users
SET donation_handle = 'give-' || substr(md5(id::text), 1, 8)
WHERE donation_handle IS NULL;
