package com.markobl.calllogsync.slice;

import androidx.annotation.Nullable;

/** Java interop exception: Kotlin Any.toString cannot express the legacy nullable return. */
public abstract class NullableCalendarLabel {
    @Nullable
    protected abstract String nullableLabel();

    @Override
    public final String toString() {
        return nullableLabel();
    }
}
