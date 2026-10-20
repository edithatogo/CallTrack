package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import org.junit.Test;

public class ManualDeliveryTest {
    @Test
    public void manualAndAutomaticUseOneReceiptWithoutDuplicate() {
        CallRecord call = PagedCallsTest.row(1, 1);
        DurableDeliveryTest.Store store = new DurableDeliveryTest.Store();
        SliceTest.Sink sink = new SliceTest.Sink();
        DurableDelivery delivery = new DurableDelivery(store, sink);
        assertEquals(
                SliceEngine.Result.SAVED, new ManualDelivery(() -> call, delivery, "UTC").run(7));
        assertEquals(
                DurableDelivery.Result.ALREADY_LOGGED,
                delivery.deliver(
                        call, 7, "UTC", new DurableDelivery.Operation("automatic", 1), () -> true));
        assertEquals(
                SliceEngine.Result.ALREADY_LOGGED,
                new ManualDelivery(() -> call, delivery, "UTC").run(7));
        assertEquals(1, sink.inserts);
    }

    @Test
    public void missingOrDeniedSourceHasNoCalendarEffect() {
        DurableDeliveryTest.Store store = new DurableDeliveryTest.Store();
        SliceTest.Sink sink = new SliceTest.Sink();
        DurableDelivery delivery = new DurableDelivery(store, sink);
        assertEquals(
                SliceEngine.Result.NO_CALL, new ManualDelivery(() -> null, delivery, "UTC").run(7));
        assertEquals(
                SliceEngine.Result.PERMISSION_REQUIRED,
                new ManualDelivery(
                                () -> {
                                    throw new SecurityException();
                                },
                                delivery,
                                "UTC")
                        .run(7));
        assertEquals(0, sink.inserts);
    }
}
