ALTER TABLE refresh_tokens
    ADD COLUMN family_id UUID,
    ADD COLUMN absolute_expires_at TIMESTAMPTZ,
    ADD COLUMN replaced_by_id BIGINT REFERENCES refresh_tokens(id);

UPDATE refresh_tokens
SET family_id = gen_random_uuid(),
    absolute_expires_at = expires_at;

ALTER TABLE refresh_tokens
    ALTER COLUMN family_id SET NOT NULL,
    ALTER COLUMN absolute_expires_at SET NOT NULL;

CREATE INDEX idx_refresh_tokens_family_id ON refresh_tokens(family_id);
