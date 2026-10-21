package com.markobl.calllogsync.slice;

import java.util.concurrent.Future;
import java.util.function.Function;

/** Orders policy publication and scheduling requests against pause and newer activations. */
final class AutomaticScheduleControl {
    private final ActivationControl activation;
    private final Function<ActivationControl.Settings, Future<?>[]> schedule;
    private final Runnable cancel;

    AutomaticScheduleControl(
            ActivationControl activation,
            Function<ActivationControl.Settings, Future<?>[]> schedule,
            Runnable cancel) {
        this.activation = activation;
        this.schedule = schedule;
        this.cancel = cancel;
    }

    ActivationControl.Settings activate(
            long calendar,
            String timezone,
            long now,
            boolean incoming,
            boolean outgoing,
            boolean missed) {
        synchronized (DurableDelivery.DELIVERY_LOCK) {
            ActivationControl.Settings settings =
                    activation.activate(calendar, timezone, now, incoming, outgoing, missed);
            schedule.apply(settings);
            return settings;
        }
    }

    void pause() {
        synchronized (DurableDelivery.DELIVERY_LOCK) {
            activation.pause();
            cancel.run();
        }
    }

    Future<?>[] reconcile() {
        return reconcile(() -> true);
    }

    Future<?>[] reconcile(java.util.function.BooleanSupplier permitted) {
        if (!permitted.getAsBoolean()) return new Future<?>[0];
        synchronized (DurableDelivery.DELIVERY_LOCK) {
            if (!permitted.getAsBoolean()) return new Future<?>[0];
            ActivationControl.Settings settings = activation.current();
            return settings != null && settings.enabled && permitted.getAsBoolean()
                    ? schedule.apply(settings)
                    : new Future<?>[0];
        }
    }
}
