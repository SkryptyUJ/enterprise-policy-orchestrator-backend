ALTER TABLE expense_request
    ADD COLUMN applied_policy_id BIGINT REFERENCES policy(id),
    ADD COLUMN decision_rationale TEXT,
    ADD COLUMN decided_by VARCHAR(255),
    ADD COLUMN decided_at TIMESTAMP;

CREATE INDEX idx_expense_request_applied_policy_id ON expense_request(applied_policy_id);
