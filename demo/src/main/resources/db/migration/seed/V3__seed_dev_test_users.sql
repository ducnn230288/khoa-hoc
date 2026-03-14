-- Seed data for dev/test environments ONLY.
-- This migration runs only when spring.flyway.locations includes classpath:db/migration/seed.
-- Passwords hashed offline with BCrypt cost factor 12.

INSERT INTO roles (code, name) VALUES ('ADMIN', 'Administrator');
INSERT INTO roles (code, name) VALUES ('USER', 'Standard User');

INSERT INTO users (username, password_hash, enabled)
VALUES ('admin', '$2b$12$WaYbybitNxHQ/JkbdgHB9.A7b8Tgs0J60RQcbTwozRcZk/9a5C4Ci', true);

INSERT INTO users (username, password_hash, enabled)
VALUES ('user01', '$2b$12$kwPQp.7e/WKUEcbiC0USpO9WNrt72c7PqCIiwgAF1b6DPJp5zG9pG', true);

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'admin' AND r.code = 'ADMIN';

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'admin' AND r.code = 'USER';

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'user01' AND r.code = 'USER';
