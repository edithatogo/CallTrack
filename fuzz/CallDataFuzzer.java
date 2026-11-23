import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.markobl.calllogsync.slice.CallEvent;
import com.markobl.calllogsync.slice.CallIdentity;
import com.markobl.calllogsync.slice.CallRecord;
import com.markobl.calllogsync.slice.ImportRange;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;

/** Coverage-guided checks of provider data before it reaches calendar events. */
public final class CallDataFuzzer {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        long start = data.consumeLong(0, 4_102_444_800_000L);
        long seconds = data.consumeLong(0, 86_400);
        boolean identityAllowed = data.consumeBoolean();
        String name = "private-name:" + data.consumeString(80);
        String number = "private-number:" + data.consumeString(80);
        CallRecord call = new CallRecord("fuzz", "1", start, seconds, "Incoming", name, number, identityAllowed);
        CallEvent event = CallEvent.from(call, "UTC");
        if (event.start != start || event.end != start + (seconds == 0 ? 60_000 : seconds * 1_000)) {
            throw new AssertionError("Calendar time does not preserve the call interval");
        }
        if (!identityAllowed && (event.title.contains(name) || event.title.contains(number)
                || event.description.contains(name) || event.description.contains(number))) {
            throw new AssertionError("Restricted caller identity reached the calendar event");
        }
        if (identityAllowed && (!event.title.contains(name) || !event.description.contains(number))) {
            throw new AssertionError("Allowed caller identity was not retained");
        }
        CallRecord renamed = new CallRecord("fuzz", "1", start, seconds, "Incoming", "changed", number, identityAllowed);
        if (!CallIdentity.signature(call).equals(CallIdentity.signature(renamed))) {
            throw new AssertionError("Contact label changed the stable source signature");
        }
        if (!event.fingerprint().equals(CallEvent.from(call, "UTC").fingerprint())) {
            throw new AssertionError("Calendar event fingerprint is not deterministic");
        }

        LocalDate first = LocalDate.ofEpochDay(data.consumeInt(0, 18_000));
        LocalDate last = first.plusDays(data.consumeInt(1, 31));
        String zone = data.pickValue(new String[] {"UTC", "Australia/Brisbane", "America/New_York", "Pacific/Apia"});
        var firstInstant = first.atStartOfDay(ZoneId.of(zone));
        var lastInstant = last.atStartOfDay(ZoneId.of(zone));
        long expectedFrom = firstInstant.toInstant().toEpochMilli();
        long expectedTo = lastInstant.toInstant().toEpochMilli();
        boolean valid = firstInstant.toLocalDate().equals(first) && lastInstant.toLocalDate().equals(last)
                && expectedFrom >= 0 && expectedTo > expectedFrom;
        try {
            ImportRange range = ImportRange.parse(first.toString(), last.toString(), zone);
            if (!valid || range.from != expectedFrom || range.to != expectedTo) {
                throw new AssertionError("Import range does not preserve its half-open calendar dates");
            }
        } catch (IllegalArgumentException invalidRange) {
            if (valid) throw invalidRange;
        }
        try {
            ImportRange range = ImportRange.parse(data.consumeString(80), data.consumeString(80), "UTC");
            if (range.from < 0 || range.to <= range.from) throw new AssertionError("Invalid import range accepted");
        } catch (DateTimeException | IllegalArgumentException expectedForInvalidInput) {
            // Invalid user input must be rejected rather than producing an invalid range.
        }
    }
}
