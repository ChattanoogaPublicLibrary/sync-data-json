CREATE TABLE entries (
  id serial primary key,
  source_id TEXT NULL,
  destination_id TEXT NULL,
  changed BOOLEAN NOT NULL DEFAULT 't');
