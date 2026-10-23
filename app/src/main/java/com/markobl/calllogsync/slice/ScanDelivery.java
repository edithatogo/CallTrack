package com.markobl.calllogsync.slice;

import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * One bounded scan step; delivery receipts precede progress, so replay cannot duplicate inserts.
 */
public final class ScanDelivery {
    public interface Progress {
        PagedCalls.Checkpoint get(DurableDelivery.Operation operation);

        void put(DurableDelivery.Operation operation, PagedCalls.Checkpoint checkpoint);
    }

    /** Immutable policy for an activation. Changes require a new persisted operation generation. */
    public static final class Policy {
        public final long activationStart;
        public final boolean incoming, outgoing, missed;

        public Policy(long activationStart, boolean incoming, boolean outgoing, boolean missed) {
            if (activationStart < 0)
                throw new IllegalArgumentException("Invalid activation boundary");
            this.activationStart = activationStart;
            this.incoming = incoming;
            this.outgoing = outgoing;
            this.missed = missed;
        }

        public boolean includes(CallRecord call) {
            return call.start >= activationStart
                    && (call.type == 1 && incoming
                            || call.type == 2 && outgoing
                            || call.type == 3 && missed);
        }
    }

    public static final class Batch {
        // Delivered counts newly confirmed receipts, including recovered effects, not rechecks
        // of an already confirmed receipt and not necessarily newly inserted calendar events.
        public final int handled, delivered, excluded, review;
        public final boolean complete;
        public final DurableDelivery.Result stopped;

        Batch(
                int handled,
                int delivered,
                int excluded,
                int review,
                boolean complete,
                DurableDelivery.Result stopped) {
            this.handled = handled;
            this.delivered = delivered;
            this.excluded = excluded;
            this.review = review;
            this.complete = complete;
            this.stopped = stopped;
        }
    }

    private final PagedCalls.Source source;
    private final Progress progress;
    private final DurableDelivery delivery;

    public ScanDelivery(PagedCalls.Source source, Progress progress, DurableDelivery delivery) {
        this.source = source;
        this.progress = progress;
        this.delivery = delivery;
    }

    public Batch run(
            DurableDelivery.Operation operation,
            Policy policy,
            long calendar,
            String timezone,
            int limit,
            BooleanSupplier allowed) {
        if (operation == null || policy == null || allowed == null || limit < 1 || limit > 500)
            throw new IllegalArgumentException("Invalid scan request");
        synchronized (DurableDelivery.DELIVERY_LOCK) {
            int handled = 0, delivered = 0, excluded = 0, review = 0;
            try {
                if (!allowed.getAsBoolean())
                    return new Batch(0, 0, 0, 0, false, DurableDelivery.Result.PAUSED);
                PagedCalls.Checkpoint checkpoint = progress.get(operation);
                if (checkpoint == null) {
                    checkpoint = PagedCalls.capture(source);
                    progress.put(operation, checkpoint);
                }
                List<CallRecord> rows = PagedCalls.read(source, checkpoint, limit);
                for (CallRecord call : rows) {
                    if (!allowed.getAsBoolean())
                        return new Batch(
                                handled,
                                delivered,
                                excluded,
                                review,
                                false,
                                DurableDelivery.Result.PAUSED);
                    // Recheck each returned candidate before any effect. ID bounds alone do not
                    // freeze content.
                    CallRecord current = source.at(PagedCalls.id(call));
                    if (current == null
                            || !call.id.equals(current.id)
                            || !call.namespace.equals(current.namespace)
                            || !PagedCalls.binding(call).equals(PagedCalls.binding(current)))
                        throw new PagedCalls.SourceChanged();
                    if (!policy.includes(call)) excluded++;
                    else {
                        boolean wasConfirmed = delivery.previouslyConfirmed(call.key(), calendar);
                        DurableDelivery.Result result =
                                delivery.deliver(call, calendar, timezone, operation, allowed);
                        switch (result) {
                            case SAVED:
                            case RECOVERED:
                            case ALREADY_LOGGED:
                                if (!wasConfirmed) delivered++;
                                break;
                            case REVIEW_REQUIRED:
                            case SOURCE_CHANGED:
                                review++;
                                break;
                            default:
                                return new Batch(
                                        handled, delivered, excluded, review, false, result);
                        }
                    }
                    checkpoint = checkpoint.advance(call);
                    progress.put(operation, checkpoint);
                    handled++;
                }
                return new Batch(
                        handled,
                        delivered,
                        excluded,
                        review,
                        checkpoint.lastId == checkpoint.upperId,
                        null);
            } catch (PagedCalls.SourceChanged changed) {
                return new Batch(
                        handled,
                        delivered,
                        excluded,
                        review,
                        false,
                        DurableDelivery.Result.SOURCE_CHANGED);
            } catch (SecurityException denied) {
                return new Batch(
                        handled,
                        delivered,
                        excluded,
                        review,
                        false,
                        DurableDelivery.Result.PERMISSION_REQUIRED);
            } catch (RuntimeException failed) {
                return new Batch(
                        handled, delivered, excluded, review, false, DurableDelivery.Result.FAILED);
            }
        }
    }
}
