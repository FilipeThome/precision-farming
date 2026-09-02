CREATE TABLE user_farms (
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    farm_id UUID NOT NULL,
    PRIMARY KEY (user_id, farm_id)
);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    jti VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
