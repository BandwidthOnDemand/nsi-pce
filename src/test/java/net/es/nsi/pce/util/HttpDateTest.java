package net.es.nsi.pce.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * The Last-Modified and If-Modified-Since handling: RFC 1123 out, the three HTTP date formats in.
 * Pins the formatting and parsing the topology, description and management APIs depend on.
 */
@RunWith(Parameterized.class)
public class HttpDateTest {
    // 2026-10-14T13:05:09Z
    private static final long INSTANT = 1791983109000L;

    @Parameter(0)
    public String name;
    @Parameter(1)
    public String header;
    @Parameter(2)
    public Long expected;

    @Parameters(name = "{0}")
    public static Collection<Object[]> cases() {
        return Arrays.asList(new Object[][] {
            {"RFC 1123", "Wed, 14 Oct 2026 13:05:09 GMT", INSTANT},
            {"RFC 1123 with an offset", "Wed, 14 Oct 2026 15:05:09 GMT+02:00", INSTANT},
            {"RFC 1036", "Wednesday, 14-Oct-26 13:05:09 GMT", INSTANT},
            {"asctime", "Wed Oct 14 13:05:09 2026", INSTANT},
            {"quoted", "'Wed, 14 Oct 2026 13:05:09 GMT'", INSTANT},
            {"two-digit years count from 2000", "Wednesday, 01-Jan-98 00:00:00 GMT", 4039372800000L},
            {"not a date", "yesterday", null},
            {"empty", "", null},
        });
    }

    @Test
    public void parses() {
        Date parsed = DateUtils.parseDate(header);
        if (expected == null) {
            assertNull(parsed);
        } else {
            assertEquals(expected.longValue(), parsed.getTime());
        }
    }

    @Test
    public void formatsAsRfc1123InGmt() {
        assertEquals("Wed, 14 Oct 2026 13:05:09 GMT",
                DateUtils.formatDate(new Date(INSTANT), DateUtils.PATTERN_RFC1123));
    }

    @Test
    public void roundTripsDropsMilliseconds() {
        String formatted = DateUtils.formatDate(new Date(INSTANT + 999), DateUtils.PATTERN_RFC1123);
        assertEquals(INSTANT, DateUtils.parseDate(formatted).getTime());
    }
}
