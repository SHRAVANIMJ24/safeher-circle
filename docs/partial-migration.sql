-- Splitting a listing between several people, and system messages
--
-- A donor with five packs may want to give two to one person and three to
-- another. Quantity is free text, so the platform cannot do that arithmetic —
-- instead the donor says "this can be split", accepting no longer closes the
-- listing, and she marks it done when she runs out.

ALTER TABLE listings
    ADD COLUMN IF NOT EXISTS can_split BOOLEAN NOT NULL DEFAULT FALSE;

-- System messages are ordinary messages with no sender. Reusing the table
-- rather than adding a separate event log keeps the whole history of an
-- exchange in one place, in order, which is what both people actually want
-- to read.
ALTER TABLE claim_messages
    ADD COLUMN IF NOT EXISTS is_system BOOLEAN NOT NULL DEFAULT FALSE;

-- When each side last opened their exchanges page. Anything newer than this
-- counts as unread, which is enough for a dot without building an inbox.
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS exchanges_seen_at TIMESTAMPTZ;
