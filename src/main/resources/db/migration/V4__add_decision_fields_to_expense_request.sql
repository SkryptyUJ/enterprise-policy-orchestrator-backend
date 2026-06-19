ALTER TABLE expense_request
    ADD COLUMN applied_policy_name VARCHAR(255),
    ADD COLUMN decision_rationale TEXT,
    ADD COLUMN decided_by VARCHAR(255),
    ADD COLUMN decided_at TIMESTAMP;
