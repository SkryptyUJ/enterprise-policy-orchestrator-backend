CREATE TABLE expense_request (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    category VARCHAR(100) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'WAITING_FOR_APPROVAL',
    expense_date DATE NOT NULL,
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_expense_request_user_id ON expense_request(user_id);
CREATE INDEX idx_expense_request_submitted_at ON expense_request(submitted_at);
