CREATE TABLE expense_request_conflicting_policy (
    request_id BIGINT NOT NULL,
    policy_name VARCHAR(255) NOT NULL,
    PRIMARY KEY (request_id, policy_name),
    CONSTRAINT fk_expense_request_conflicting_policy_request
        FOREIGN KEY (request_id)
        REFERENCES expense_request(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_expense_request_conflicting_policy_request_id
    ON expense_request_conflicting_policy(request_id);
