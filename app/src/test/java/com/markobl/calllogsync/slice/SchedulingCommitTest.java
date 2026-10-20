package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.concurrent.*;

/** Broadcast completion must cover both durable scheduling acknowledgements within one budget. */
public class SchedulingCommitTest {
    @Test
    public void bothSchedulingOperationsMustAcknowledgeBeforeBroadcastCanFinish() throws Exception {
        CompletableFuture<Void> periodic = new CompletableFuture<>(),
                prompt = new CompletableFuture<>();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch entered = new CountDownLatch(1);
        try {
            Future<?> result =
                    executor.submit(
                            () -> {
                                entered.countDown();
                                try {
                                    AutomaticScheduling.awaitCommitted(
                                            new Future<?>[] {periodic, prompt},
                                            System.nanoTime() + TimeUnit.SECONDS.toNanos(3));
                                } catch (Exception error) {
                                    throw new AssertionError(error);
                                }
                            });
            assertTrue(entered.await(1, TimeUnit.SECONDS));
            periodic.complete(null);
            assertThrows(TimeoutException.class, () -> result.get(50, TimeUnit.MILLISECONDS));
            prompt.complete(null);
            result.get(1, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void failedEnqueueCannotBeAcknowledgedAsCommitted() {
        CompletableFuture<Void> failed = new CompletableFuture<>();
        failed.completeExceptionally(new IllegalStateException("fixture"));
        assertThrows(
                ExecutionException.class,
                () ->
                        AutomaticScheduling.awaitCommitted(
                                new Future<?>[] {CompletableFuture.completedFuture(null), failed},
                                System.nanoTime() + TimeUnit.SECONDS.toNanos(1)));
    }

    @Test
    public void neverAcknowledgedEnqueueHasABoundedBroadcastBudget() {
        assertThrows(
                TimeoutException.class,
                () ->
                        AutomaticScheduling.awaitCommitted(
                                new Future<?>[] {new CompletableFuture<>()},
                                System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(5)));
    }
}
