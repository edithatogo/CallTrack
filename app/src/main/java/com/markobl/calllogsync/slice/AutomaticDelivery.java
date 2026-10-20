package com.markobl.calllogsync.slice;

/** Activation-bound one-page runner. Scheduler repeats incomplete work; old workers fail closed. */
public final class AutomaticDelivery {
    public interface Progress extends ScanDelivery.Progress {
        void rollover(
                DurableDelivery.Operation operation,
                PagedCalls.Checkpoint expected,
                PagedCalls.Checkpoint next);
    }

    private final ActivationControl activation;
    private final PagedCalls.Source source;
    private final Progress progress;
    private final ScanDelivery scan;

    public AutomaticDelivery(
            ActivationControl activation,
            PagedCalls.Source source,
            Progress progress,
            DurableDelivery delivery) {
        this.activation = activation;
        this.source = source;
        this.progress = progress;
        scan = new ScanDelivery(source, progress, delivery);
    }

    public ScanDelivery.Batch run(ActivationControl.Settings worker, int limit) {
        if (limit < 1 || limit > 500) throw new IllegalArgumentException("Invalid page limit");
        synchronized (DurableDelivery.DELIVERY_LOCK) {
            try {
                if (!activation.allowed(worker)) return stopped(DurableDelivery.Result.PAUSED);
                PagedCalls.Checkpoint checkpoint = progress.get(worker.operation());
                if (checkpoint != null && checkpoint.lastId == checkpoint.upperId) {
                    PagedCalls.Checkpoint next = PagedCalls.extend(source, checkpoint);
                    if (!activation.allowed(worker)) return stopped(DurableDelivery.Result.PAUSED);
                    progress.rollover(worker.operation(), checkpoint, next);
                }
                return scan.run(
                        worker.operation(),
                        worker.policy(),
                        worker.calendar,
                        worker.timezone,
                        limit,
                        () -> activation.allowed(worker));
            } catch (PagedCalls.SourceChanged changed) {
                return stopped(DurableDelivery.Result.SOURCE_CHANGED);
            } catch (SecurityException denied) {
                return stopped(DurableDelivery.Result.PERMISSION_REQUIRED);
            } catch (RuntimeException failed) {
                return stopped(DurableDelivery.Result.FAILED);
            }
        }
    }

    private static ScanDelivery.Batch stopped(DurableDelivery.Result result) {
        return new ScanDelivery.Batch(0, 0, 0, 0, false, result);
    }
}
