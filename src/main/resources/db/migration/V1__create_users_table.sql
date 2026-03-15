-- Create users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE
);

-- Create index on username for faster queries
CREATE INDEX idx_users_username ON users(username);

