-- Handover arrangement
--
-- Either party can propose a place and time; the other confirms or counters.
-- Deliberately not "the donor sets the location": the person receiving may not
-- have the fare to reach a spot chosen for her, and has no say in whether it
-- feels safe. Both propose, both agree.

ALTER TABLE listing_claims
    ADD COLUMN IF NOT EXISTS proposed_place    VARCHAR(300),
    ADD COLUMN IF NOT EXISTS proposed_time     VARCHAR(120),
    ADD COLUMN IF NOT EXISTS proposed_by_id    UUID REFERENCES users(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS handover_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS proposed_at       TIMESTAMPTZ;

-- A claim can be withdrawn by the claimant, which puts the listing back on
-- the board.
ALTER TABLE listing_claims
    ADD COLUMN IF NOT EXISTS withdrawn_at TIMESTAMPTZ;
