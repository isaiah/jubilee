package org.jruby.jubilee.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by isaiah on 12/25/13.
 */
public class DateUtils {

    private static final Locale LOCALE_US = Locale.US;

    private static final TimeZone GMT_ZONE = TimeZone.getTimeZone("GMT");

    private static final String RFC1123_PATTERN = "EEE, dd MMM yyyy HH:mm:ss z";
    /**
     * Thread local cache of this date format. This is technically a small memory leak, however
     * in practice it is fine, as it will only be used by server threads.
     *
     * This is the most common date format, which is why we cache it.
     */
    private static final ThreadLocal<SimpleDateFormat> RFC1123_PATTERN_FORMAT = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat df =  new SimpleDateFormat(RFC1123_PATTERN, LOCALE_US);
            df.setTimeZone(GMT_ZONE);
            return df;
        }
    };

    /**
     * Converts a date to a format suitable for use in a HTTP request
     *
     * @param date The date
     * @return The RFC-1123 formatted date
     */
    public static String toDateString(final Date date) {
        return RFC1123_PATTERN_FORMAT.get().format(date);
    }
}
