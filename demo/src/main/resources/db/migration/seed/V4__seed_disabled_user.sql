-- V4: Seed disabled user for AC-7 integration test
-- This file is only applied when spring.flyway.locations includes classpath:db/migration/seed
-- (dev and test profiles only — never runs in staging/prod)
--
-- disabled_user is used by AuthIntegrationTest.login_withDisabledUser_returns401()
-- Hash matches "User@123" (BCrypt cost 12, computed offline).
-- The password is irrelevant because Spring Security throws DisabledException before checking it.
INSERT INTO users (username, password_hash, enabled) VALUES
    ('disabled_user', '$2b$12$dBk9aMKeOhzhrRtAy24/ROkWp.oszCvx2jXJG4aeDd.9hFGbVU3AK', FALSE)
ON CONFLICT (username) DO NOTHING;
