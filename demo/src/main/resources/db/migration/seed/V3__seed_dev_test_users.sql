-- V3: Seed default dev/test users (BCrypt cost 12, hashes computed offline)
-- This file is only applied when spring.flyway.locations includes classpath:db/migration/seed
-- (dev and test profiles only — never runs in staging/prod)

-- Seed roles
INSERT INTO roles (code, name) VALUES
    ('ADMIN', 'Administrator'),
    ('USER',  'Regular User')
ON CONFLICT (code) DO NOTHING;

-- Seed users
-- admin / Admin@123
INSERT INTO users (username, password_hash, enabled) VALUES
    ('admin',  '$2b$12$jp5g.Ef0SvZYApZhOD2AcOdK/Ds3xDO.vJJDtBs7fw5b8z4eXuN.6', TRUE)
ON CONFLICT (username) DO NOTHING;

-- user01 / User@123
INSERT INTO users (username, password_hash, enabled) VALUES
    ('user01', '$2b$12$dBk9aMKeOhzhrRtAy24/ROkWp.oszCvx2jXJG4aeDd.9hFGbVU3AK', TRUE)
ON CONFLICT (username) DO NOTHING;

-- Assign roles
INSERT INTO user_roles (user_id, role_id)
    SELECT u.id, r.id FROM users u, roles r
    WHERE u.username = 'admin' AND r.code = 'ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
    SELECT u.id, r.id FROM users u, roles r
    WHERE u.username = 'user01' AND r.code = 'USER'
ON CONFLICT DO NOTHING;
