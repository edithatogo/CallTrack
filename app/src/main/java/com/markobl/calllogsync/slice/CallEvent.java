package com.markobl.calllogsync.slice;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/** Immutable public event fields. No network, attendees, reminders or sync-only fields. */
public final class CallEvent {
    public final String title, description, timezone, marker;
    public final long start, end;

    public CallEvent(
            String title,
            String description,
            long start,
            long end,
            String timezone,
            String marker) {
        this.title = title;
        this.description = description;
        this.start = start;
        this.end = end;
        this.timezone = timezone;
        this.marker = marker;
    }

    public static CallEvent from(CallRecord call, String timezone) {
        String person =
                !call.identityAllowed
                        ? "Private number"
                        : present(call.name)
                                ? call.name
                                : present(call.number) ? call.number : "Unknown number";
        String description =
                "Number: "
                        + (call.identityAllowed && present(call.number)
                                ? call.number
                                : "Unavailable")
                        + "\nActual duration: "
                        + call.seconds
                        + " seconds";
        if (call.seconds == 0)
            description += "\nOne-minute visual marker; actual call duration is zero.";
        long end =
                Math.addExact(
                        call.start,
                        call.seconds == 0 ? 60000 : Math.multiplyExact(call.seconds, 1000));
        return new CallEvent(
                call.direction + ": " + person,
                description,
                call.start,
                end,
                Objects.requireNonNull(timezone),
                "calltrack-dev://source/" + hash(call.key()));
    }

    private static boolean present(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String field(String s) {
        return s == null ? "-1:" : s.length() + ":" + s;
    }

    public String fingerprint() {
        return hash(
                field(title)
                        + field(description)
                        + start
                        + ":"
                        + end
                        + ":"
                        + field(timezone)
                        + field(marker));
    }

    private static String hash(String text) {
        try {
            byte[] bytes =
                    MessageDigest.getInstance("SHA-256")
                            .digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte b : bytes)
                result.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }
}
