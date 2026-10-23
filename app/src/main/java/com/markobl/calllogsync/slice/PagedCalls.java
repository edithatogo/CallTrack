package com.markobl.calllogsync.slice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Frozen ID snapshot with source/anchor validation; progress advances only after row handling. */
public final class PagedCalls {
    private PagedCalls() {}

    public interface Source {
        String namespace();

        CallRecord upper();

        CallRecord at(long id);

        List<CallRecord> page(long afterExclusive, long upperInclusive, int limit);
    }

    public static final class SourceChanged extends IllegalStateException {
        SourceChanged() {
            super("Source changed or paging contract unavailable; review required");
        }
    }

    public static final class Checkpoint {
        public final String namespace, upperBinding, lastBinding;
        public final long upperId, lastId;

        public Checkpoint(
                String namespace,
                long upperId,
                String upperBinding,
                long lastId,
                String lastBinding) {
            if (namespace == null
                    || namespace.isEmpty()
                    || upperId < -1
                    || lastId < -1
                    || lastId > upperId
                    || (upperId == -1) != (upperBinding == null)
                    || (lastId == -1) != (lastBinding == null))
                throw new IllegalArgumentException("Invalid source checkpoint");
            this.namespace = namespace;
            this.upperId = upperId;
            this.upperBinding = upperBinding;
            this.lastId = lastId;
            this.lastBinding = lastBinding;
        }

        public Checkpoint advance(CallRecord call) {
            long id = id(call);
            if (!namespace.equals(call.namespace) || id <= lastId || id > upperId)
                throw new SourceChanged();
            return new Checkpoint(namespace, upperId, upperBinding, id, binding(call));
        }
    }

    public static Checkpoint capture(Source source) {
        String namespace = source.namespace();
        CallRecord upper = source.upper();
        if (upper == null) return new Checkpoint(namespace, -1, null, -1, null);
        if (!Objects.equals(namespace, upper.namespace)) throw new SourceChanged();
        return new Checkpoint(namespace, id(upper), binding(upper), -1, null);
    }

    /** Extend only a completed snapshot, retaining its processed anchor. */
    public static Checkpoint extend(Source source, Checkpoint completed) {
        if (completed.lastId != completed.upperId)
            throw new IllegalArgumentException("Scan is incomplete");
        validate(source, completed);
        Checkpoint captured = capture(source);
        validate(source, completed);
        if (!completed.namespace.equals(captured.namespace) || captured.upperId < completed.upperId)
            throw new SourceChanged();
        if (captured.upperId == completed.upperId
                && !Objects.equals(captured.upperBinding, completed.upperBinding))
            throw new SourceChanged();
        return new Checkpoint(
                completed.namespace,
                captured.upperId,
                captured.upperBinding,
                completed.lastId,
                completed.lastBinding);
    }

    public static boolean equal(Checkpoint left, Checkpoint right) {
        return left != null
                && right != null
                && left.namespace.equals(right.namespace)
                && left.upperId == right.upperId
                && left.lastId == right.lastId
                && Objects.equals(left.upperBinding, right.upperBinding)
                && Objects.equals(left.lastBinding, right.lastBinding);
    }

    public static List<CallRecord> read(Source source, Checkpoint checkpoint, int limit) {
        if (limit < 1 || limit > 500) throw new IllegalArgumentException("Invalid page limit");
        validate(source, checkpoint);
        if (checkpoint.lastId == checkpoint.upperId) return Collections.emptyList();
        List<CallRecord> rows = source.page(checkpoint.lastId, checkpoint.upperId, limit);
        if (rows == null || rows.isEmpty() || rows.size() > limit) throw new SourceChanged();
        long previous = checkpoint.lastId;
        for (CallRecord call : rows) {
            long id = id(call);
            if (!checkpoint.namespace.equals(call.namespace)
                    || id <= previous
                    || id > checkpoint.upperId) throw new SourceChanged();
            previous = id;
        }
        // Detect an observable source reset/change that occurred during the provider query.
        validate(source, checkpoint);
        return Collections.unmodifiableList(new ArrayList<>(rows));
    }

    public static void validate(Source source, Checkpoint checkpoint) {
        if (!checkpoint.namespace.equals(source.namespace())) throw new SourceChanged();
        if (checkpoint.upperId >= 0
                && !same(
                        source.at(checkpoint.upperId),
                        checkpoint.upperId,
                        checkpoint.upperBinding,
                        checkpoint.namespace)) throw new SourceChanged();
        if (checkpoint.lastId >= 0
                && !same(
                        source.at(checkpoint.lastId),
                        checkpoint.lastId,
                        checkpoint.lastBinding,
                        checkpoint.namespace)) throw new SourceChanged();
    }

    private static boolean same(
            CallRecord call, long expectedId, String expectedBinding, String namespace) {
        return call != null
                && id(call) == expectedId
                && namespace.equals(call.namespace)
                && expectedBinding.equals(binding(call));
    }

    public static String binding(CallRecord call) {
        // Separate checkpoint binding preserves raw presentation without changing stored source-v1
        // IDs.
        return CallIdentity.signature(call)
                + "|presentation-v1|"
                + call.presentationSupplied
                + "|"
                + call.numberPresentation;
    }

    public static long id(CallRecord call) {
        if (call == null) throw new SourceChanged();
        try {
            long id = Long.parseLong(call.id);
            if (id < 0 || !Long.toString(id).equals(call.id)) throw new SourceChanged();
            return id;
        } catch (NumberFormatException e) {
            throw new SourceChanged();
        }
    }
}
