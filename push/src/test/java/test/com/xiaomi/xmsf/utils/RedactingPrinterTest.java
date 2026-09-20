package test.com.xiaomi.xmsf.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.xiaomi.xmsf.utils.RedactingPrinter;

import org.junit.Test;

/**
 * The values in these samples are made up; the field names and the shapes mirror what the SDK
 * really writes into the log file. Every case either is something that has been seen in a real
 * log, or is a plausible line that must not be damaged.
 */
public class RedactingPrinterTest {

    @Test
    public void hidesRegistrationSecretInProtocolMessage() {
        String message = "From Server     : {\"regId\":\"1234567\",\"regSecret\":\"AbCdEf123+/=\","
                + "\"region\":\"China\",\"registeredAt\":1789731162364}";

        assertEquals("From Server     : {\"regId\":\"1234567\",\"regSecret\":\"***\","
                        + "\"region\":\"China\",\"registeredAt\":1789731162364}",
                RedactingPrinter.redact(message));
    }

    @Test
    public void hidesCredentialsInQuotedJson() {
        assertEquals("{\"token\": \"***\"}",
                RedactingPrinter.redact("{\"token\": \"a1b2c3d4e5\"}"));
        assertEquals("{\"token\":\"***\",\"security\":\"***\",\"passToken\":\"***\"}",
                RedactingPrinter.redact("{\"token\":\"aaa\",\"security\":\"bbb\",\"passToken\":\"ccc\"}"));
        assertEquals("ssecurity=***",
                RedactingPrinter.redact("ssecurity=QQZZ9988"));
    }

    @Test
    public void hidesCredentialsInXmlStanza() {
        assertEquals("<iq type=\"set\"><token>***</token><regSec>***</regSec></iq>",
                RedactingPrinter.redact(
                        "<iq type=\"set\"><token>abc123def</token><regSec>xy/zw==</regSec></iq>"));
    }

    @Test
    public void hidesCredentialsInKeyValuePairs() {
        assertEquals("token=***&security=***&app_token=***",
                RedactingPrinter.redact("token=abcdef123&security=zyxwvu456&app_token=qwerty789"));
        assertEquals("appToken=*** pass_token=***",
                RedactingPrinter.redact("appToken=939f7a2b1c pass_token=ppqq112233"));
        assertEquals("password: ***",
                RedactingPrinter.redact("password: hunter2hunter2"));
    }

    @Test
    public void keepsIdentifiersThatAreNotCredentials() {
        String message = "{\"regId\":\"1234567\",\"devId\":\"89abcdef\"}";

        assertEquals(message, RedactingPrinter.redact(message));
    }

    @Test
    public void leavesOrdinaryLinesAlone() {
        String message = "connect to mtalk.google.com:5222, ping interval 180000";

        assertEquals(message, RedactingPrinter.redact(message));
    }

    @Test
    public void leavesProseAfterAKeyLikeWordAlone() {
        // Short free text is not a credential: real tokens are long strings.
        assertEquals("Security: unable to open",
                RedactingPrinter.redact("Security: unable to open"));
        assertEquals("SecurityException: Cannot open the socket",
                RedactingPrinter.redact("SecurityException: Cannot open the socket"));
    }

    @Test
    public void leavesUnrelatedWordsThatOnlyLookSimilarAlone() {
        String message = "secondary node is 3, second retry in 12s, pong received";

        assertEquals(message, RedactingPrinter.redact(message));
    }

    @Test
    public void toleratesMissingMessage() {
        assertNull(RedactingPrinter.redact(null));
        assertEquals("", RedactingPrinter.redact(""));
    }
}
