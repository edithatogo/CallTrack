package com.markobl.calllogsync.slice;

/** A source row, not a contacts lookup. Restricted presentation masks all cached identity. */
public final class CallRecord {
    public final String namespace, id, direction, name, number;
    public final long start, seconds;
    public final boolean identityAllowed;

    public CallRecord(
            String namespace,
            String id,
            long start,
            long seconds,
            String direction,
            String name,
            String number,
            boolean identityAllowed) {
        if (namespace == null
                || namespace.isEmpty()
                || id == null
                || id.isEmpty()
                || start < 0
                || seconds < 0) throw new IllegalArgumentException("Invalid call identity/time");
        this.namespace = namespace;
        this.id = id;
        this.start = start;
        this.seconds = seconds;
        this.direction = direction == null ? "Unknown" : direction;
        this.name = name;
        this.number = number;
        this.identityAllowed = identityAllowed;
    }

    public String key() {
        return namespace.length() + ":" + namespace + id.length() + ":" + id;
    }
}
