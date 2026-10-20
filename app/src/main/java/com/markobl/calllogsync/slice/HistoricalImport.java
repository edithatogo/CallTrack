package com.markobl.calllogsync.slice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Frozen historical candidate protocol. Store mutations must commit atomically before returning.
 */
public final class HistoricalImport {
    public enum Phase {
        BUILDING,
        PREVIEW,
        CONFIRMED,
        COMPLETE,
        CANCELLED,
        REVIEW
    }

    public interface Store {
        void create(Session session);

        Session get(String id);

        /**
         * Compare revision, append candidates and replace metadata in one committed transaction.
         */
        void save(Session expected, Session next, List<Candidate> added);

        List<Candidate> candidates(String id, long afterExclusive, int limit);

        /**
         * Commit candidate outcome and session progress atomically; failed attempts stay pending.
         */
        void finish(
                Session expected,
                Session next,
                Candidate candidate,
                DurableDelivery.Result outcome);
    }

    public static final class Candidate {
        public final long sourceId;
        public final String key, binding;
        public final DurableDelivery.Result outcome;

        public Candidate(long sourceId, String key, String binding) {
            this(sourceId, key, binding, null);
        }

        public Candidate(
                long sourceId, String key, String binding, DurableDelivery.Result outcome) {
            this.outcome = outcome;
            if (sourceId < 0
                    || key == null
                    || key.isEmpty()
                    || binding == null
                    || binding.isEmpty())
                throw new IllegalArgumentException("Invalid import candidate");
            this.sourceId = sourceId;
            this.key = key;
            this.binding = binding;
        }

        static Candidate from(CallRecord call) {
            return new Candidate(PagedCalls.id(call), call.key(), PagedCalls.binding(call));
        }
    }

    public static final class Session {
        public final String id, timezone;
        public final long calendar, from, to, revision, count, processedId, saved, review;
        public final boolean incoming, outgoing, missed;
        public final Phase phase;
        public final PagedCalls.Checkpoint checkpoint;
        public final DurableDelivery.Result stopped;

        public Session(
                String id,
                long calendar,
                String timezone,
                long from,
                long to,
                boolean incoming,
                boolean outgoing,
                boolean missed,
                Phase phase,
                PagedCalls.Checkpoint checkpoint,
                long revision,
                long count,
                long processedId,
                long saved,
                long review,
                DurableDelivery.Result stopped) {
            // Reuse activation route/timezone validation; import interval is [from,to).
            new ActivationControl.Settings(
                    1, false, calendar, timezone, from, incoming, outgoing, missed);
            if (id == null
                    || !id.matches("[a-f0-9-]{36}")
                    || to <= from
                    || revision < 0
                    || count < 0
                    || processedId < -1
                    || saved < 0
                    || review < 0
                    || saved > count
                    || review > count - saved
                    || phase == null
                    || checkpoint == null)
                throw new IllegalArgumentException("Invalid import session");
            this.id = id;
            this.calendar = calendar;
            this.timezone = timezone;
            this.from = from;
            this.to = to;
            this.incoming = incoming;
            this.outgoing = outgoing;
            this.missed = missed;
            this.phase = phase;
            this.checkpoint = checkpoint;
            this.revision = revision;
            this.count = count;
            this.processedId = processedId;
            this.saved = saved;
            this.review = review;
            this.stopped = stopped;
        }

        public DurableDelivery.Operation operation() {
            return new DurableDelivery.Operation("import/" + id, 1);
        }

        Session next(
                Phase phase,
                PagedCalls.Checkpoint checkpoint,
                long count,
                long processedId,
                long saved,
                long review,
                DurableDelivery.Result stopped) {
            return new Session(
                    id,
                    calendar,
                    timezone,
                    from,
                    to,
                    incoming,
                    outgoing,
                    missed,
                    phase,
                    checkpoint,
                    Math.addExact(revision, 1),
                    count,
                    processedId,
                    saved,
                    review,
                    stopped);
        }

        boolean includes(CallRecord call) {
            return call.start < to
                    && new ScanDelivery.Policy(from, incoming, outgoing, missed).includes(call);
        }
    }

    private final Store store;
    private final PagedCalls.Source source;
    private final DurableDelivery.Store receipts;
    private final DurableDelivery delivery;

    public HistoricalImport(
            Store store,
            PagedCalls.Source source,
            DurableDelivery.Store receipts,
            DurableDelivery delivery) {
        this.store = store;
        this.source = source;
        this.receipts = receipts;
        this.delivery = delivery;
    }

    public Session start(
            long calendar,
            String timezone,
            long from,
            long to,
            boolean incoming,
            boolean outgoing,
            boolean missed) {
        synchronized (DurableDelivery.DELIVERY_LOCK) {
            Session session =
                    new Session(
                            UUID.randomUUID().toString(),
                            calendar,
                            timezone,
                            from,
                            to,
                            incoming,
                            outgoing,
                            missed,
                            Phase.BUILDING,
                            PagedCalls.capture(source),
                            0,
                            0,
                            -1,
                            0,
                            0,
                            null);
            store.create(session);
            return session;
        }
    }

    public Session preview(String id, int limit) {
        bounds(limit);
        synchronized (DurableDelivery.DELIVERY_LOCK) {
            Session session = required(id);
            if (session.phase != Phase.BUILDING) return session;
            try {
                List<CallRecord> page = PagedCalls.read(source, session.checkpoint, limit);
                List<Candidate> candidates = new ArrayList<>();
                PagedCalls.Checkpoint checkpoint = session.checkpoint;
                for (CallRecord call : page) {
                    if (session.includes(call)) candidates.add(Candidate.from(call));
                    checkpoint = checkpoint.advance(call);
                }
                Session next =
                        session.next(
                                checkpoint.lastId == checkpoint.upperId
                                        ? Phase.PREVIEW
                                        : Phase.BUILDING,
                                checkpoint,
                                Math.addExact(session.count, candidates.size()),
                                -1,
                                0,
                                0,
                                null);
                store.save(session, next, candidates);
                return next;
            } catch (PagedCalls.SourceChanged changed) {
                return save(
                        session,
                        session.next(
                                Phase.REVIEW,
                                session.checkpoint,
                                session.count,
                                -1,
                                0,
                                0,
                                DurableDelivery.Result.SOURCE_CHANGED));
            }
        }
    }

    public Session confirm(String id) {
        synchronized (DurableDelivery.DELIVERY_LOCK) {
            Session session = required(id);
            if (session.phase != Phase.PREVIEW)
                throw new IllegalStateException("Preview must finish before confirmation");
            return save(
                    session,
                    session.next(
                            session.count == 0 ? Phase.COMPLETE : Phase.CONFIRMED,
                            session.checkpoint,
                            session.count,
                            -1,
                            0,
                            0,
                            null));
        }
    }

    public Session cancel(String id) {
        synchronized (DurableDelivery.DELIVERY_LOCK) {
            Session session = required(id);
            if (session.phase == Phase.COMPLETE || session.phase == Phase.CANCELLED) return session;
            return save(
                    session,
                    session.next(
                            Phase.CANCELLED,
                            session.checkpoint,
                            session.count,
                            session.processedId,
                            session.saved,
                            session.review,
                            null));
        }
    }

    public Session deliver(String id, int limit) {
        return deliver(id, limit, () -> true);
    }

    public Session deliver(String id, int limit, java.util.function.BooleanSupplier running) {
        bounds(limit);
        synchronized (DurableDelivery.DELIVERY_LOCK) {
            Session session = required(id);
            if (session.phase != Phase.CONFIRMED) return session;
            List<Candidate> page = store.candidates(id, session.processedId, limit);
            if (page == null || page.isEmpty() || page.size() > limit)
                throw new IllegalStateException("Import candidates unavailable");
            for (Candidate candidate : page) {
                if (!running.getAsBoolean()) return session;
                if (candidate.sourceId <= session.processedId
                        || candidate.sourceId > session.checkpoint.upperId)
                    throw new IllegalStateException("Invalid candidate order");
                DurableDelivery.Result result = attempt(session, candidate, running);
                long saved = session.saved, review = session.review;
                switch (result) {
                    case SAVED:
                    case RECOVERED:
                    case ALREADY_LOGGED:
                        saved = Math.addExact(saved, 1);
                        break;
                    case REVIEW_REQUIRED:
                    case SOURCE_CHANGED:
                        review = Math.addExact(review, 1);
                        break;
                    default:
                        return finish(
                                session,
                                session.next(
                                        session.phase,
                                        session.checkpoint,
                                        session.count,
                                        session.processedId,
                                        saved,
                                        review,
                                        result),
                                candidate,
                                result);
                }
                Session next =
                        session.next(
                                saved + review == session.count ? Phase.COMPLETE : Phase.CONFIRMED,
                                session.checkpoint,
                                session.count,
                                candidate.sourceId,
                                saved,
                                review,
                                null);
                session = finish(session, next, candidate, result);
                if (session.phase == Phase.COMPLETE) break;
            }
            return session;
        }
    }

    private DurableDelivery.Result attempt(
            Session session, Candidate candidate, java.util.function.BooleanSupplier running) {
        try {
            final Session active = session;
            DurableDelivery.Result result;
            DurableDelivery.Intent intent = receipts.get(candidate.key, session.calendar);
            if (owned(intent, session) && bindingMatches(candidate, intent)) {
                result =
                        delivery.resume(
                                candidate.key,
                                session.calendar,
                                session.operation(),
                                () -> running.getAsBoolean() && allowed(active));
            } else {
                CallRecord call = source.at(candidate.sourceId);
                if (call == null
                        || !candidate.key.equals(call.key())
                        || !candidate.binding.equals(PagedCalls.binding(call)))
                    result = DurableDelivery.Result.SOURCE_CHANGED;
                else
                    result =
                            delivery.deliver(
                                    call,
                                    session.calendar,
                                    session.timezone,
                                    session.operation(),
                                    () -> running.getAsBoolean() && allowed(active));
            }
            return result;
        } catch (SecurityException denied) {
            return DurableDelivery.Result.PERMISSION_REQUIRED;
        } catch (RuntimeException failed) {
            return DurableDelivery.Result.FAILED;
        }
    }

    private Session finish(
            Session expected, Session next, Candidate candidate, DurableDelivery.Result outcome) {
        store.finish(expected, next, candidate, outcome);
        return next;
    }

    private boolean allowed(Session worker) {
        Session current = store.get(worker.id);
        return current != null
                && current.phase == Phase.CONFIRMED
                && current.revision == worker.revision;
    }

    private static boolean owned(DurableDelivery.Intent intent, Session session) {
        return intent != null
                && intent.operation != null
                && intent.operation.owner.equals(session.operation().owner)
                && intent.operation.generation == 1;
    }

    private static boolean bindingMatches(Candidate candidate, DurableDelivery.Intent intent) {
        return intent.facts != null
                && candidate.binding.equals(
                        intent.sourceSignature
                                + "|presentation-v1|"
                                + intent.facts.presentationSupplied
                                + "|"
                                + intent.facts.presentation);
    }

    private Session save(Session expected, Session next) {
        store.save(expected, next, Collections.emptyList());
        return next;
    }

    private Session required(String id) {
        Session session = store.get(id);
        if (session == null) throw new IllegalArgumentException("Unknown import");
        return session;
    }

    private static void bounds(int limit) {
        if (limit < 1 || limit > 500)
            throw new IllegalArgumentException("Invalid import batch limit");
    }
}
