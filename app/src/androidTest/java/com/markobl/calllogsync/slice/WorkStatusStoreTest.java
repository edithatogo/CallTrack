package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;

/** Private nonpersonal settings fixture only; no work scheduling or provider permissions. */
public class WorkStatusStoreTest {
    private Context context() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    private String fixture() {
        String token = InstrumentationRegistry.getArguments().getString("fixtureToken", "");
        if (!token.matches("[a-f0-9]{32}")) throw new IllegalArgumentException("Fixture required");
        return "status-" + token;
    }

    @Test
    public void publicationPersistsFailureCountAndRejectsOldActivationAfterReopen() {
        String name = fixture();
        SharedPreferences preferences = context().getSharedPreferences(name, Context.MODE_PRIVATE);
        try {
            ActivationControl control =
                    new ActivationControl(new AndroidActivationStore(preferences));
            control.activate(7, "UTC", 1000, true, true, true);
            AndroidWorkStatusStore store = new AndroidWorkStatusStore(preferences);
            assertTrue(store.publish(1, 1001, "a", WorkStatus.Code.FAILED, 0, 0));
            assertTrue(store.publish(1, 1002, "a", WorkStatus.Code.FAILED, 0, 0));
            assertEquals(1, new AndroidWorkStatusStore(preferences).read(1).failures);
            assertTrue(store.publish(1, 1003, "b", WorkStatus.Code.COMPLETE, 2, 0));
            assertEquals(1, store.read(1).failures);
            control.activate(8, "UTC", 2000, true, false, true);
            assertTrue(store.publish(1, 2001, "old", WorkStatus.Code.FAILED, 0, 0));
            assertNull(store.read(2));
            assertEquals(1, store.read(1).failures);
            assertTrue(store.publish(2, 2002, "new", WorkStatus.Code.COMPLETE, 3, 0));
            assertTrue(store.publish(1, 3000, "late", WorkStatus.Code.FAILED, 0, 0));
            WorkStatus current = new AndroidWorkStatusStore(preferences).read(2);
            assertEquals(0, current.failures);
            assertEquals("new", current.token);
            assertEquals(3, current.delivered);
            assertEquals(WorkStatus.Code.COMPLETE, current.code);
        } finally {
            assertTrue(context().deleteSharedPreferences(name));
        }
    }

    @Test
    public void legacyStatusWithoutCounterRemainsReadableAndUnknownCodesAreSanitised() {
        String name = fixture();
        SharedPreferences preferences = context().getSharedPreferences(name, Context.MODE_PRIVATE);
        try {
            assertTrue(
                    preferences
                            .edit()
                            .putLong("automaticStatusGeneration", 1)
                            .putLong("automaticStatusAt", 1000)
                            .putString("automaticStatusCode", "unsupported-private-text")
                            .commit());
            WorkStatus status = new AndroidWorkStatusStore(preferences).read(1);
            assertEquals(WorkStatus.Code.UNKNOWN, status.code);
            assertEquals(0, status.failures);
            assertEquals("legacy", status.token);
        } finally {
            assertTrue(context().deleteSharedPreferences(name));
        }
    }

    private static void expectUnavailable(Runnable read) {
        try {
            read.run();
            fail("An uncertain status must not be presented as saved");
        } catch (IllegalStateException expected) {
        }
    }

    @Test
    public void uncertainCommitIsHiddenAcrossStoreInstancesUntilSuccessfulPublication() {
        String name = fixture();
        SharedPreferences disk = context().getSharedPreferences(name, Context.MODE_PRIVATE);
        java.util.concurrent.atomic.AtomicBoolean fail =
                new java.util.concurrent.atomic.AtomicBoolean();
        // Return an uncertain outcome after Android has updated its memory cache. Disk may
        // actually have committed: callers must not infer durability from the cached value.
        SharedPreferences preferences =
                (SharedPreferences)
                        java.lang.reflect.Proxy.newProxyInstance(
                                SharedPreferences.class.getClassLoader(),
                                new Class<?>[] {SharedPreferences.class},
                                (proxy, method, args) -> {
                                    Object value = method.invoke(disk, args);
                                    if (!method.getName().equals("edit")) return value;
                                    SharedPreferences.Editor editor =
                                            (SharedPreferences.Editor) value;
                                    return java.lang.reflect.Proxy.newProxyInstance(
                                            SharedPreferences.Editor.class.getClassLoader(),
                                            new Class<?>[] {SharedPreferences.Editor.class},
                                            (editorProxy, editMethod, editArgs) -> {
                                                Object result = editMethod.invoke(editor, editArgs);
                                                if (editMethod.getName().equals("commit")
                                                        && fail.get()) return false;
                                                return result == editor ? editorProxy : result;
                                            });
                                });
        try {
            new ActivationControl(new AndroidActivationStore(preferences))
                    .activate(7, "UTC", 1000, true, true, true);
            AndroidWorkStatusStore store = new AndroidWorkStatusStore(preferences);
            assertTrue(store.publish(1, 1001, "old", WorkStatus.Code.COMPLETE, 0, 0));
            fail.set(true);
            assertFalse(store.publish(1, 1002, "attempt", WorkStatus.Code.FAILED, 0, 0));
            assertEquals("attempt", disk.getString("automaticStatusToken", null));
            expectUnavailable(() -> store.read(1));
            expectUnavailable(() -> new AndroidWorkStatusStore(preferences).read(1));
            assertFalse(store.publish(1, 1003, "attempt", WorkStatus.Code.FAILED, 0, 0));
            expectUnavailable(() -> store.read(1));
            fail.set(false);
            assertTrue(
                    new AndroidWorkStatusStore(preferences)
                            .publish(1, 1004, "attempt", WorkStatus.Code.FAILED, 0, 0));
            WorkStatus saved = store.read(1);
            assertEquals(1, saved.failures);
            assertEquals(1004, saved.at);
            assertEquals("attempt", saved.token);
        } finally {
            assertTrue(context().deleteSharedPreferences(name));
        }
    }
}
