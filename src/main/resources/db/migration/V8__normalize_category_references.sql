INSERT INTO category (id, value, label)
SELECT DISTINCT p.category_id, p.category_id::TEXT, 'Category ' || p.category_id
FROM policy p
WHERE NOT EXISTS (
    SELECT 1 FROM category c WHERE c.id = p.category_id
);

INSERT INTO category (id, value, label)
SELECT DISTINCT er.category::INT, er.category, 'Category ' || er.category
FROM expense_request er
WHERE er.category ~ '^[0-9]+$'
  AND NOT EXISTS (
      SELECT 1 FROM category c WHERE c.id = er.category::INT
  );

INSERT INTO category (value, label)
SELECT DISTINCT lower(trim(er.category)), trim(er.category)
FROM expense_request er
WHERE er.category IS NOT NULL
  AND trim(er.category) <> ''
  AND er.category !~ '^[0-9]+$'
  AND lower(trim(er.category)) NOT IN (
      'sprzet biurowy',
      'travel',
      'business travel',
      'office',
      'office supplies',
      'hardware',
      'podroze sluzbowe',
      'szkolenia',
      'training',
      'posilki',
      'meals'
  )
  AND NOT EXISTS (
      SELECT 1 FROM category c WHERE lower(c.label) = lower(trim(er.category))
  )
  AND NOT EXISTS (
      SELECT 1 FROM category c WHERE lower(c.value) = lower(trim(er.category))
  );

ALTER TABLE expense_request ADD COLUMN category_id INT;

UPDATE expense_request
SET category_id = CASE
    WHEN lower(category) IN ('1', 'sprzet biurowy', 'travel', 'business travel', 'office', 'office supplies', 'hardware') THEN 1
    WHEN lower(category) IN ('2', 'podroze sluzbowe') THEN 2
    WHEN lower(category) IN ('3', 'szkolenia', 'training') THEN 3
    WHEN lower(category) IN ('4', 'posilki', 'meals') THEN 4
    WHEN category ~ '^[0-9]+$' THEN category::INT
    ELSE NULL
END;

UPDATE expense_request er
SET category_id = c.id
FROM category c
WHERE er.category_id IS NULL
  AND lower(c.label) = lower(trim(er.category));

ALTER TABLE expense_request ALTER COLUMN category_id SET NOT NULL;

ALTER TABLE policy DROP COLUMN category;
ALTER TABLE expense_request DROP COLUMN category;
ALTER TABLE category DROP COLUMN value;

ALTER TABLE policy
    ADD CONSTRAINT fk_policy_category
    FOREIGN KEY (category_id) REFERENCES category(id);

ALTER TABLE expense_request
    ADD CONSTRAINT fk_expense_request_category
    FOREIGN KEY (category_id) REFERENCES category(id);

SELECT setval(pg_get_serial_sequence('category', 'id'), (SELECT MAX(id) FROM category));
