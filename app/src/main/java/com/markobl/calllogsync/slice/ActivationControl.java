package com.markobl.calllogsync.slice;

import java.util.Objects;
import java.util.TimeZone;

/** Committed activation publication shares the delivery lock with final provider-effect checks. */
public final class ActivationControl {
    public interface Store {
        Settings get();

        /** Must commit atomically before returning; a failed commit must throw. */
        void put(Settings settings);
    }

    public static final class Settings {
        public final long generation, calendar, start;
        public final String timezone;
        public final boolean enabled, incoming, outgoing, missed;

        public Settings(
                long generation,
                boolean enabled,
                long calendar,
                String timezone,
                long start,
                boolean incoming,
                boolean outgoing,
                boolean missed) {
            if (generation < 1
                    || calendar < 0
                    || start < 0
                    || timezone == null
                    || !java.util.Arrays.asList(TimeZone.getAvailableIDs()).contains(timezone))
                throw new IllegalArgumentException("Invalid activation settings");
            this.generation = generation;
            this.enabled = enabled;
            this.calendar = calendar;
            this.timezone = timezone;
            this.start = start;
            this.incoming = incoming;
            this.outgoing = outgoing;
            this.missed = missed;
        }

        public DurableDelivery.Operation operation() {
            return new DurableDelivery.Operation("automatic", generation);
        }

        public ScanDelivery.Policy policy() {
            return new ScanDelivery.Policy(start, incoming, outgoing, missed);
        }

        Settings paused() {
            return new Settings(
                    generation, false, calendar, timezone, start, incoming, outgoing, missed);
        }

        boolean same(Settings other) {
            return other != null
                    && generation == other.generation
                    && enabled == other.enabled
                    && calendar == other.calendar
                    && start == other.start
                    && Objects.equals(timezone, other.timezone)
                    && incoming == other.incoming
                    && outgoing == other.outgoing
                    && missed == other.missed;
        }
    }

    private final Store store;

    public ActivationControl(Store store) {
        this.store = Objects.requireNonNull(store);
    }

    public Settings current() {
        synchronized (DurableDelivery.DELIVERY_LOCK) {
            return store.get();
        }
    }

    /** Every explicit activation starts a fresh future boundary; historical work needs import. */
    public Settings activate(
            long calendar,
            String timezone,
            long now,
            boolean incoming,
            boolean outgoing,
            boolean missed) {
        synchronized (DurableDelivery.DELIVERY_LOCK) {
            Settings old = store.get();
            Settings next =
                    new Settings(
                            old == null ? 1 : Math.addExact(old.generation, 1),
                            true,
                            calendar,
                            timezone,
                            now,
                            incoming,
                            outgoing,
                            missed);
            store.put(next);
            return next;
        }
    }

    public void pause() {
        synchronized (DurableDelivery.DELIVERY_LOCK) {
            Settings old = store.get();
            if (old != null) store.put(old.paused());
        }
    }

    public boolean allowed(Settings worker) {
        synchronized (DurableDelivery.DELIVERY_LOCK) {
            return worker != null && worker.enabled && worker.same(store.get());
        }
    }
}
