package com.markobl.calllogsync.slice;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;

/** Synthetic private identity preferences only; never queries the call provider. */
public class SourceNamespaceStoreTest {
    private Context context() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    private String fixture() {
        String token = InstrumentationRegistry.getArguments().getString("fixtureToken", "");
        if (!token.matches("[a-f0-9]{32}")) throw new IllegalArgumentException("Fixture required");
        return "namespace-" + token;
    }

    private AndroidCallLogSource source(SharedPreferences preferences) {
        Context wrapper =
                new ContextWrapper(context()) {
                    @Override
                    public Context getApplicationContext() {
                        return this;
                    }

                    @Override
                    public SharedPreferences getSharedPreferences(String name, int mode) {
                        if (!"slice".equals(name))
                            throw new AssertionError("Unexpected preferences");
                        return preferences;
                    }
                };
        return new AndroidCallLogSource(wrapper);
    }

    @Test
    public void falseIdentityCommitCannotBeReusedFromCache() {
        uncertain(false);
    }

    @Test
    public void thrownIdentityCommitCannotBeReusedFromCache() {
        uncertain(true);
    }

    private void uncertain(boolean thrown) {
        String name = fixture();
        SharedPreferences disk = context().getSharedPreferences(name, Context.MODE_PRIVATE);
        SharedPreferences fault =
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
                                                if (editMethod.getName().equals("commit")) {
                                                    if (thrown)
                                                        throw new IllegalStateException(
                                                                "fixture uncertain identity");
                                                    return false;
                                                }
                                                return result == editor ? editorProxy : result;
                                            });
                                });
        try {
            unavailable(() -> source(fault).namespace());
            assertNotNull(disk.getString("sourceNamespace", null));
            unavailable(() -> source(disk).namespace());
        } finally {
            assertTrue(context().deleteSharedPreferences(name));
        }
    }

    @Test
    public void committedNamespaceIsReusedAcrossAdapters() {
        String name = fixture();
        SharedPreferences disk = context().getSharedPreferences(name, Context.MODE_PRIVATE);
        try {
            String first = source(disk).namespace();
            assertTrue(first.startsWith("android-call-log/"));
            assertEquals(first, disk.getString("sourceNamespace", null));
            assertEquals(first, source(disk).namespace());
        } finally {
            assertTrue(context().deleteSharedPreferences(name));
        }
    }

    private static void unavailable(Runnable action) {
        try {
            action.run();
            fail("Uncertain source identity must not be reused");
        } catch (IllegalStateException expected) {
        }
    }
}
