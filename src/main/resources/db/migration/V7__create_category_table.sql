CREATE TABLE category (
    id SERIAL PRIMARY KEY,
    value VARCHAR(100) NOT NULL UNIQUE,
    label VARCHAR(255) NOT NULL UNIQUE
);

INSERT INTO category (id, value, label) VALUES
    (1, '1', 'Sprzet biurowy'),
    (2, '2', 'Podroze sluzbowe'),
    (3, '3', 'Szkolenia'),
    (4, '4', 'Posilki');

SELECT setval(pg_get_serial_sequence('category', 'id'), (SELECT MAX(id) FROM category));
