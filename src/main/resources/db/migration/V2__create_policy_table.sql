-- Create policy table
CREATE TABLE policy (
    id BIGSERIAL PRIMARY KEY,
    policy_id VARCHAR(255) NOT NULL,
    author_user_id VARCHAR(255) NOT NULL,
    category_id INT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    starts_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP,
    min_price NUMERIC(38,0),
    max_price NUMERIC(38,0),
    category INT NOT NULL,
    authorized_role INT
);

-- Create indexes for common queries
CREATE INDEX idx_policy_name ON policy(name);
CREATE INDEX idx_policy_author_user_id ON policy(author_user_id);
CREATE INDEX idx_policy_created_at ON policy(created_at);
CREATE INDEX idx_policy_expires_at ON policy(expires_at);

