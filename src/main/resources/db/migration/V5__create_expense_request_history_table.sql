CREATE TABLE expense_request_history (
    id BIGSERIAL PRIMARY KEY,
    request_id BIGINT NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    previous_status VARCHAR(50),
    new_status VARCHAR(50) NOT NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    change_reason VARCHAR(255) NOT NULL,
    FOREIGN KEY (request_id) REFERENCES expense_request(id) ON DELETE CASCADE
);

CREATE INDEX idx_expense_request_history_request_id ON expense_request_history(request_id);
CREATE INDEX idx_expense_request_history_user_id ON expense_request_history(user_id);
CREATE INDEX idx_expense_request_history_changed_at ON expense_request_history(changed_at);
