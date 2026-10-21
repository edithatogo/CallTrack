package com.markobl.calllogsync.slice;

/** Java interop exception: Kotlin Any.toString cannot express the legacy nullable return. */
public abstract class NullableCalendarLabel {
    protected abstract String nullableLabel();

    @Override
    public final String toString() {
        return nullableLabel();
    }
}
