package com.markobl.calllogsync.slice;

import android.content.Context;
import android.content.SharedPreferences;

/** Atomic generation-checked publication. Status never grants authority to deliver a call. */
public final class AndroidWorkStatusStore {
    private final SharedPreferences preferences;

    public AndroidWorkStatusStore(Context context) {
        this(context.getApplicationContext().getSharedPreferences("slice", Context.MODE_PRIVATE));
    }

    AndroidWorkStatusStore(SharedPreferences preferences) {
        this.preferences = preferences;
    }

    public WorkStatus read(long generation) {
        synchronized (DurableDelivery.DELIVERY_LOCK) {
            WorkStatus stored = stored();
            return stored != null && stored.generation == generation ? stored : null;
        }
    }

    public boolean publish(
            long generation,
            long at,
            String token,
            WorkStatus.Code code,
            int delivered,
            int review) {
        synchronized (DurableDelivery.DELIVERY_LOCK) {
            ActivationControl.Settings active = new AndroidActivationStore(preferences).get();
            if (active == null || active.generation != generation) return true;
            WorkStatus prior = stored();
            WorkStatus next =
                    WorkStatus.next(prior, generation, at, token, code, delivered, review);
            if (next == prior) return true;
            return preferences
                    .edit()
                    .putLong("automaticStatusGeneration", next.generation)
                    .putLong("automaticStatusAt", next.at)
                    .putString("automaticStatusToken", next.token)
                    .putString("automaticStatusCode", next.code.name())
                    .putInt("automaticStatusDelivered", next.delivered)
                    .putInt("automaticStatusReview", next.review)
                    .putLong("automaticStatusFailures", next.failures)
                    .commit();
        }
    }

    private WorkStatus stored() {
        long generation = preferences.getLong("automaticStatusGeneration", -1);
        if (generation < 1) return null;
        return new WorkStatus(
                generation,
                preferences.getLong("automaticStatusAt", 0),
                preferences.getString("automaticStatusToken", "legacy"),
                WorkStatus.Code.parse(preferences.getString("automaticStatusCode", null)),
                preferences.getInt("automaticStatusDelivered", 0),
                preferences.getInt("automaticStatusReview", 0),
                preferences.getLong("automaticStatusFailures", 0));
    }
}
