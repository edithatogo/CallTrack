CREATE TABLE receipts (source TEXT NOT NULL, calendar INTEGER NOT NULL, event INTEGER NOT NULL, fingerprint TEXT NOT NULL, PRIMARY KEY(source,calendar))
CREATE TABLE outbox (source TEXT NOT NULL, calendar INTEGER NOT NULL, state TEXT NOT NULL, event INTEGER NOT NULL, signature TEXT, marker TEXT, fingerprint TEXT NOT NULL, payload_title TEXT, payload_description TEXT, payload_start INTEGER, payload_end INTEGER, payload_timezone TEXT, PRIMARY KEY(source,calendar))
CREATE INDEX outbox_state ON outbox(calendar,state)
