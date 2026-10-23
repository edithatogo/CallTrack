package com.markobl.calllogsync.slice;

/** A source row, not a contacts lookup. Restricted presentation masks all cached identity. */
public final class CallRecord {
    public final String namespace, id, direction, name, number;
    public final long start, seconds;
    public final boolean identityAllowed;

    public final int type;
    public final String phoneAccountId;

    public CallRecord(
            String namespace,
            String id,
            long start,
            long seconds,
            String direction,
            String name,
            String number,
            boolean identityAllowed) {
        this(
                namespace,
                id,
                start,
                seconds,
                direction,
                name,
                number,
                identityAllowed,
                typeOf(direction),
                null);
    }

    public CallRecord(
            String namespace,
            String id,
            long start,
            long seconds,
            int type,
            String name,
            String number,
            boolean identityAllowed,
            String phoneAccountId) {
        this(
                namespace,
                id,
                start,
                seconds,
                directionOf(type),
                name,
                number,
                identityAllowed,
                type,
                phoneAccountId);
    }

    private CallRecord(
            String namespace,
            String id,
            long start,
            long seconds,
            String direction,
            String name,
            String number,
            boolean identityAllowed,
            int type,
            String phoneAccountId) {
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
        this.type = type;
        this.phoneAccountId = phoneAccountId;
    }

    private static int typeOf(String direction) {
        return "Incoming".equals(direction)
                ? 1
                : "Outgoing".equals(direction) ? 2 : "Missed".equals(direction) ? 3 : 0;
    }

    private static String directionOf(int type) {
        return type == 1 ? "Incoming" : type == 2 ? "Outgoing" : type == 3 ? "Missed" : "Unknown";
    }

    public String key() {
        return namespace.length() + ":" + namespace + id.length() + ":" + id;
    }
}
