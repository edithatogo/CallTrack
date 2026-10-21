CREATE TABLE sessions (id TEXT PRIMARY KEY NOT NULL, revision INTEGER NOT NULL, body TEXT NOT NULL)
CREATE TABLE candidates (session TEXT NOT NULL REFERENCES sessions(id), source_id INTEGER NOT NULL, source_key TEXT NOT NULL, binding TEXT NOT NULL, PRIMARY KEY(session,source_id))
