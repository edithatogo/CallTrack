package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import org.junit.Test;

public class ActivationControlTest {
    static class Store implements ActivationControl.Store {
        ActivationControl.Settings value;
        boolean fail;

        public ActivationControl.Settings get() {
            return value;
        }

        public void put(ActivationControl.Settings v) {
            if (fail) throw new IllegalStateException();
            value = v;
        }
    }

    @Test
    public void disabledUntilExplicitActivationAndPauseSurvivesControllerRestart() {
        Store s = new Store();
        ActivationControl c = new ActivationControl(s);
        assertNull(c.current());
        ActivationControl.Settings a = c.activate(7, "UTC", 1000, true, false, true);
        assertEquals(1, a.generation);
        assertEquals(1000, a.start);
        assertTrue(c.allowed(a));
        c.pause();
        assertFalse(new ActivationControl(s).allowed(a));
        assertEquals(1, s.value.generation);
    }

    @Test
    public void reactivationCreatesNewBoundaryAndInvalidatesOldWorker() {
        Store s = new Store();
        ActivationControl c = new ActivationControl(s);
        ActivationControl.Settings a = c.activate(7, "UTC", 1000, true, true, true);
        c.pause();
        ActivationControl.Settings b = c.activate(8, "UTC", 2000, true, false, true);
        assertEquals(2, b.generation);
        assertEquals(2000, b.start);
        assertFalse(c.allowed(a));
        assertTrue(c.allowed(b));
    }

    @Test
    public void failedPublicationCannotEnableAndGenerationCannotWrap() {
        Store s = new Store();
        ActivationControl c = new ActivationControl(s);
        s.fail = true;
        assertThrows(
                IllegalStateException.class, () -> c.activate(7, "UTC", 1000, true, true, true));
        assertNull(c.current());
        s.fail = false;
        s.value =
                new ActivationControl.Settings(
                        Long.MAX_VALUE, false, 7, "UTC", 1000, true, true, true);
        assertThrows(ArithmeticException.class, () -> c.activate(7, "UTC", 2000, true, true, true));
        assertFalse(s.value.enabled);
    }

    @Test
    public void workerMustMatchEntirePersistedPolicyNotJustGeneration() {
        Store s = new Store();
        ActivationControl c = new ActivationControl(s);
        ActivationControl.Settings a = c.activate(7, "UTC", 1000, true, true, true);
        assertFalse(
                c.allowed(
                        new ActivationControl.Settings(
                                a.generation, true, 8, "UTC", 1000, true, true, true)));
    }
}
