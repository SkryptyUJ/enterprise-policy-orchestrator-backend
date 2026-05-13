ALTER TABLE expense_request
ADD COLUMN resolution_policy_id BIGINT;

ALTER TABLE expense_request
ADD CONSTRAINT fk_expense_request_resolution_policy
FOREIGN KEY (resolution_policy_id) REFERENCES policy(id);

CREATE INDEX idx_expense_request_resolution_policy_id
ON expense_request(resolution_policy_id);
