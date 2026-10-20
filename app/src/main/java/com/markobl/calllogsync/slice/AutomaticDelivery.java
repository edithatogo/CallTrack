package com.markobl.calllogsync.slice;

/** Activation-bound one-page runner. Scheduler repeats incomplete work; old workers fail closed. */
public final class AutomaticDelivery {
    public interface Progress extends ScanDelivery.Progress {
        java.util.List<String> pending(
                DurableDelivery.Operation operation, long calendar, int limit);

        void rollover(
                DurableDelivery.Operation operation,
                PagedCalls.Checkpoint expected,
                PagedCalls.Checkpoint next);
    }

    private final ActivationControl activation;
    private final PagedCalls.Source source;
    private final Progress progress;
    private final ScanDelivery scan;
    private final DurableDelivery delivery;

    public AutomaticDelivery(
            ActivationControl activation,
            PagedCalls.Source source,
            Progress progress,
            DurableDelivery delivery) {
        this.delivery = delivery;
        this.activation = activation;
        this.source = source;
        this.progress = progress;
        scan = new ScanDelivery(source, progress, delivery);
    }

    public ScanDelivery.Batch run(ActivationControl.Settings worker, int limit) {
        return run(worker, limit, () -> true);
    }

    public ScanDelivery.Batch run(
            ActivationControl.Settings worker,
            int limit,
            java.util.function.BooleanSupplier running) {
        if (limit < 1 || limit > 500) throw new IllegalArgumentException("Invalid page limit");
        synchronized (DurableDelivery.DELIVERY_LOCK) {
            try {
                if (!permitted(worker, running)) return stopped(DurableDelivery.Result.PAUSED);
                java.util.List<String> pending =
                        progress.pending(worker.operation(), worker.calendar, limit);
                if (pending == null
                        || pending.size() > limit
                        || new java.util.HashSet<>(pending).size() != pending.size())
                    throw new IllegalStateException("Invalid pending batch");
                if (!pending.isEmpty()) {
                    int handled = 0, delivered = 0, review = 0;
                    for (String key : pending) {
                        if (key == null || key.isEmpty())
                            throw new IllegalStateException("Invalid pending key");
                        DurableDelivery.Result result =
                                delivery.resume(
                                        key,
                                        worker.calendar,
                                        worker.operation(),
                                        () -> permitted(worker, running));
                        switch (result) {
                            case SAVED:
                            case RECOVERED:
                            case ALREADY_LOGGED:
                                delivered++;
                                break;
                            case REVIEW_REQUIRED:
                            case SOURCE_CHANGED:
                                review++;
                                break;
                            default:
                                return new ScanDelivery.Batch(
                                        handled, delivered, 0, review, false, result);
                        }
                        handled++;
                    }
                    // A separate invocation continues recovery or scans; never touch source in this
                    // batch.
                    return new ScanDelivery.Batch(handled, delivered, 0, review, false, null);
                }
                PagedCalls.Checkpoint checkpoint = progress.get(worker.operation());
                if (checkpoint != null && checkpoint.lastId == checkpoint.upperId) {
                    PagedCalls.Checkpoint next = PagedCalls.extend(source, checkpoint);
                    if (!permitted(worker, running)) return stopped(DurableDelivery.Result.PAUSED);
                    progress.rollover(worker.operation(), checkpoint, next);
                }
                return scan.run(
                        worker.operation(),
                        worker.policy(),
                        worker.calendar,
                        worker.timezone,
                        limit,
                        () -> permitted(worker, running));
            } catch (PagedCalls.SourceChanged changed) {
                return stopped(DurableDelivery.Result.SOURCE_CHANGED);
            } catch (SecurityException denied) {
                return stopped(DurableDelivery.Result.PERMISSION_REQUIRED);
            } catch (RuntimeException failed) {
                return stopped(DurableDelivery.Result.FAILED);
            }
        }
    }

    private boolean permitted(
            ActivationControl.Settings worker, java.util.function.BooleanSupplier running) {
        return running.getAsBoolean() && activation.allowed(worker);
    }

    private static ScanDelivery.Batch stopped(DurableDelivery.Result result) {
        return new ScanDelivery.Batch(0, 0, 0, 0, false, result);
    }
}
