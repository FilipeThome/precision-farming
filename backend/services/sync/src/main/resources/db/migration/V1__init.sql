CREATE TABLE sync_commands (
    id UUID PRIMARY KEY,
    device_id VARCHAR(80) NOT NULL,
    client_operation_id VARCHAR(80) NOT NULL UNIQUE,
    command_type VARCHAR(80) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
