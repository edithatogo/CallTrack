package com.markobl.calllogsync.slice;

import java.util.Objects;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/** One explicit human reapproval of an unchanged, never-attempted frozen intent. */
public final class ReadyReapproval {
    private final DurableDelivery.Store store;
    private final SliceEngine.CalendarSink sink;

    public ReadyReapproval(DurableDelivery.Store store, SliceEngine.CalendarSink sink) {
        this.store = store;
        this.sink = sink;
    }

    public static boolean eligible(DurableDelivery.Intent intent) {
        return intent != null
                && intent.state == DurableDelivery.State.READY
                && intent.eventId < 0
                && intent.payload != null
                && intent.sourceSignature != null
                && intent.marker != null
                && intent.marker.equals(intent.payload.marker)
                && intent.fingerprint.equals(intent.payload.fingerprint());
    }

    public DurableDelivery.Result approve(
            DurableDelivery.Intent expected, BooleanSupplier confirmed) {
        synchronized (DurableDelivery.DELIVERY_LOCK) {
            try {
                if (!confirmed.getAsBoolean()) return DurableDelivery.Result.PAUSED;
                if (!eligible(expected)) return DurableDelivery.Result.REVIEW_REQUIRED;
                DurableDelivery.Intent current = store.get(expected.key, expected.calendar);
                if (!eligible(current)) return DurableDelivery.Result.REVIEW_REQUIRED;
                if (!unchanged(current, expected)) return DurableDelivery.Result.SOURCE_CHANGED;
                if (!sink.writable(current.calendar))
                    return DurableDelivery.Result.DESTINATION_UNAVAILABLE;
                if (!confirmed.getAsBoolean()) return DurableDelivery.Result.PAUSED;
                DurableDelivery.Operation approval =
                        new DurableDelivery.Operation("manual-reapproval/" + UUID.randomUUID(), 1);
                store.put(
                        new DurableDelivery.Intent(
                                DurableDelivery.State.READY,
                                current.key,
                                current.calendar,
                                current.sourceSignature,
                                current.marker,
                                current.fingerprint,
                                -1,
                                current.payload,
                                approval,
                                current.facts));
                return new DurableDelivery(store, sink)
                        .resume(current.key, current.calendar, approval, confirmed);
            } catch (SecurityException denied) {
                return DurableDelivery.Result.PERMISSION_REQUIRED;
            } catch (RuntimeException failed) {
                return DurableDelivery.Result.FAILED;
            }
        }
    }

    private static boolean unchanged(DurableDelivery.Intent a, DurableDelivery.Intent b) {
        return a.key.equals(b.key)
                && a.calendar == b.calendar
                && a.fingerprint.equals(b.fingerprint)
                && Objects.equals(a.sourceSignature, b.sourceSignature)
                && Objects.equals(a.marker, b.marker)
                && sameOperation(a.operation, b.operation)
                && sameFacts(a.facts, b.facts);
    }

    private static boolean sameOperation(DurableDelivery.Operation a, DurableDelivery.Operation b) {
        return a == null
                ? b == null
                : b != null && a.owner.equals(b.owner) && a.generation == b.generation;
    }

    private static boolean sameFacts(DurableDelivery.SourceFacts a, DurableDelivery.SourceFacts b) {
        return a == null
                ? b == null
                : b != null
                        && a.start == b.start
                        && a.seconds == b.seconds
                        && a.type == b.type
                        && a.presentationSupplied == b.presentationSupplied
                        && Objects.equals(a.presentation, b.presentation);
    }
}
