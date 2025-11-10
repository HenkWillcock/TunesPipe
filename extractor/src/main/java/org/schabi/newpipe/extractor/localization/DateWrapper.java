package org.schabi.newpipe.extractor.localization;


import org.jetbrains.annotations.NotNull;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * A wrapper class that provides a field to describe if the date/time is precise or just an
 * approximation.
 */
public class DateWrapper implements Serializable {
    @NotNull
    private final OffsetDateTime offsetDateTime;
    private final boolean isApproximation;

    /**
     * @deprecated Use {@link #DateWrapper(OffsetDateTime)} instead.
     */
    @Deprecated
    public DateWrapper(@NotNull final Calendar calendar) {
        //noinspection deprecation
        this(calendar, false);
    }

    /**
     * @deprecated Use {@link #DateWrapper(OffsetDateTime, boolean)} instead.
     */
    @Deprecated
    public DateWrapper(@NotNull final Calendar calendar, final boolean isApproximation) {
        this(OffsetDateTime.ofInstant(calendar.toInstant(), ZoneOffset.UTC), isApproximation);
    }

    public DateWrapper(@NotNull final OffsetDateTime offsetDateTime) {
        this(offsetDateTime, false);
    }

    public DateWrapper(@NotNull final OffsetDateTime offsetDateTime,
                       final boolean isApproximation) {
        this.offsetDateTime = offsetDateTime.withOffsetSameInstant(ZoneOffset.UTC);
        this.isApproximation = isApproximation;
    }

    /**
     * @return the wrapped date/time as a {@link Calendar}.
     * @deprecated use {@link #offsetDateTime()} instead.
     */
    @Deprecated
    @NotNull
    public Calendar date() {
        return GregorianCalendar.from(offsetDateTime.toZonedDateTime());
    }

    /**
     * @return the wrapped date/time.
     */
    @NotNull
    public OffsetDateTime offsetDateTime() {
        return offsetDateTime;
    }

    /**
     * @return if the date is considered is precise or just an approximation (e.g. service only
     * returns an approximation like 2 weeks ago instead of a precise date).
     */
    public boolean isApproximation() {
        return isApproximation;
    }
}
