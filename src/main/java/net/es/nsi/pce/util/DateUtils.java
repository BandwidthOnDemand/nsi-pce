package net.es.nsi.pce.util;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * HTTP dates for Last-Modified and If-Modified-Since: RFC 1123 out, the three HTTP formats in.
 * Two-digit years count from 2000.
 */
public final class DateUtils {
    public static final String PATTERN_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";
    private static final String PATTERN_RFC1036 = "EEE, dd-MMM-yy HH:mm:ss zzz";
    private static final String PATTERN_ASCTIME = "EEE MMM d HH:mm:ss yyyy";

    private static final String[] DEFAULT_PATTERNS = { PATTERN_RFC1123, PATTERN_RFC1036, PATTERN_ASCTIME };
    private static final TimeZone GMT = TimeZone.getTimeZone("GMT");
    private static final Date TWO_DIGIT_YEAR_START;

    static {
        Calendar calendar = Calendar.getInstance(GMT, Locale.US);
        calendar.set(2000, Calendar.JANUARY, 1, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        TWO_DIGIT_YEAR_START = calendar.getTime();
    }

    private DateUtils() {
    }

    /**
     * Parses a date in any of the three HTTP formats.
     *
     * @return the date, or null if no format matches.
     */
    public static Date parseDate(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Date value may not be null");
        }
        String v = value.length() > 1 && value.startsWith("'") && value.endsWith("'")
                ? value.substring(1, value.length() - 1) : value;

        for (String pattern : DEFAULT_PATTERNS) {
            SimpleDateFormat parser = formatFor(pattern);
            parser.set2DigitYearStart(TWO_DIGIT_YEAR_START);
            ParsePosition position = new ParsePosition(0);
            Date result = parser.parse(v, position);
            if (position.getIndex() != 0) {
                return result;
            }
        }
        return null;
    }

    public static String formatDate(Date date, String pattern) {
        if (date == null || pattern == null) {
            throw new IllegalArgumentException("Date and pattern may not be null");
        }
        return formatFor(pattern).format(date);
    }

    // SimpleDateFormat is not thread-safe, so each call gets its own.
    private static SimpleDateFormat formatFor(String pattern) {
        SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
        format.setTimeZone(GMT);
        return format;
    }
}
