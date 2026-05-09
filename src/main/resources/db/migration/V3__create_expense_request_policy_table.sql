CREATE TABLE expense_request_policy (
    request_id BIGINT REFERENCES expense_request(id) ON DELETE CASCADE,
    policy_id BIGINT REFERENCES policy(id) ON DELETE CASCADE,
    PRIMARY KEY (request_id, policy_id)
);