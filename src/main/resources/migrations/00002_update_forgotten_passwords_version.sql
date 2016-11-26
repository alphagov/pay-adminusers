ALTER TABLE forgotten_passwords
  ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
