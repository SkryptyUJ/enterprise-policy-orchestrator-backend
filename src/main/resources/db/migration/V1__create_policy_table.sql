-- Create policy table
CREATE TABLE policy (
    id BIGSERIAL PRIMARY KEY,
    policy_id VARCHAR(255) NOT NULL,
    author_user_id VARCHAR(255) NOT NULL,
    category_id INT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    version INT NOT NULL DEFAULT 1,
    created_at TIME NOT NULL DEFAULT CURRENT_TIME,
    updated_at TIME NOT NULL DEFAULT CURRENT_TIME,
    starts_at DATE NOT NULL,
    expires_at DATE,
    min_price INT,
    max_price INT,
    category VARCHAR(255) NOT NULL,
    authorized_role INT NOT NULL,
    is_valid BOOLEAN NOT NULL DEFAULT TRUE
);

-- Create indexes for common queries
CREATE INDEX idx_policy_name ON policy(name);
CREATE INDEX idx_policy_author_user_id ON policy(author_user_id);
CREATE INDEX idx_policy_created_at ON policy(created_at);
CREATE INDEX idx_policy_expires_at ON policy(expires_at);
