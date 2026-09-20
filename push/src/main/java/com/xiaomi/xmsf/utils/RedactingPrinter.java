package com.xiaomi.xmsf.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.elvishew.xlog.printer.Printer;

import java.util.regex.Pattern;

/**
 * Blanks credential values out of a log line before it reaches logcat or the log file.
 *
 * <p>The SDK logs whole protocol messages, and the registration response carries {@code regSecret}
 * — the key used to decrypt downstream messages — in clear text. The log folder can be exported
 * and attached to a bug report, so every value is replaced with {@link #REDACTED}.
 *
 * <p>Only <em>values</em> are rewritten, never keys or surrounding structure: a line stays
 * recognisable as {@code "regSecret":"***"}, which is what debugging needs. A line without any
 * credential-looking key is returned untouched, so the common case costs one cheap scan.
 */
public class RedactingPrinter implements Printer {

    static final String REDACTED = "***";

    /** Key names carrying a credential, matched case-insensitively as a whole word. */
    private static final String KEY =
            "(?:reg[_]?sec(?:ret)?|secret|security|ssecurity"
                    + "|pass[_]?token|app[_]?token|access[_]?token|auth[_]?token|session[_]?token|token"
                    + "|app[_]?secret|client[_]?secret"
                    + "|pass[_]?wd|passwd|password|pwd)";

    /** Cheap gate: most log lines contain none of these, and skip the three rewrites below. */
    private static final Pattern LOOKS_SENSITIVE = Pattern.compile("sec|tok|pass|pwd",
            Pattern.CASE_INSENSITIVE);

    /** {@code "key":"value"} and {@code "key": "value"} — what the SDK emits for protocol JSON. */
    private static final Pattern QUOTED = Pattern.compile(
            "(\"" + KEY + "\"\\s*:\\s*\")[^\"]*(\")", Pattern.CASE_INSENSITIVE);

    /** {@code <key>value</key>} — the same field inside an XML stanza. */
    private static final Pattern TAGGED = Pattern.compile(
            "(<" + KEY + ">)[^<]*(</" + KEY + ">)", Pattern.CASE_INSENSITIVE);

    /**
     * {@code key=value}, {@code key: value} and {@code &key=value}, stopping at the separators.
     *
     * <p>Unlike the two rules above there is no delimiter telling where the value ends, so a value
     * has to be long enough to be a credential. That is what keeps prose such as
     * {@code Security: unable to open} intact — real tokens and keys are always long strings.
     */
    private static final Pattern BARE = Pattern.compile(
            "((?:^|[\\s&?,;<=])" + KEY + "\\s*[=:]\\s*)([^\\s,;&\"'<>}\\])]{8,})",
            Pattern.CASE_INSENSITIVE);

    private final Printer downstream;

    public RedactingPrinter(@NonNull Printer downstream) {
        this.downstream = downstream;
    }

    @Override
    public void println(int logLevel, String tag, String msg) {
        downstream.println(logLevel, tag, redact(msg));
    }

    /**
     * @return the message with every credential value replaced, or the message itself when there is
     * nothing that looks like one.
     */
    public static @Nullable String redact(@Nullable String msg) {
        if (msg == null || msg.isEmpty() || !LOOKS_SENSITIVE.matcher(msg).find()) {
            return msg;
        }
        String redacted = QUOTED.matcher(msg).replaceAll("$1" + REDACTED + "$2");
        redacted = TAGGED.matcher(redacted).replaceAll("$1" + REDACTED + "$2");
        return BARE.matcher(redacted).replaceAll("$1" + REDACTED);
    }
}
