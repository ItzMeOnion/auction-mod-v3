package com.onxon.auction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parsing and formatting helpers for DonutSMP style money (1k, 2.5m, 3b, 1t). */
public final class Money {
    private Money() {}

    /**
     * Matches "Name paid you $50M". Anchored to the start of the line so a player typing
     * that sentence in the middle of a chat message (after a rank / name prefix) is ignored.
     */
    private static final Pattern PAID_YOU = Pattern.compile(
            "^\\s*([A-Za-z0-9_.]{3,17})\\s+paid\\s+you\\s+\\$\\s*([0-9][0-9,]*(?:\\.[0-9]+)?)([kKmMbBtT]?)(?![A-Za-z])");

    public record Payment(String player, long amount) {}

    public static Payment parsePayment(String text) {
        if (text == null) return null;
        Matcher m = PAID_YOU.matcher(text);
        if (!m.find()) return null;
        long amount = parseAmount(m.group(2), m.group(3));
        if (amount <= 0) return null;
        return new Payment(m.group(1), amount);
    }

    public static long parseAmount(String number, String suffix) {
        try {
            BigDecimal value = new BigDecimal(number.replace(",", ""));
            BigDecimal mult = switch (suffix == null ? "" : suffix.toLowerCase(Locale.ROOT)) {
                case "k" -> BigDecimal.valueOf(1_000L);
                case "m" -> BigDecimal.valueOf(1_000_000L);
                case "b" -> BigDecimal.valueOf(1_000_000_000L);
                case "t" -> BigDecimal.valueOf(1_000_000_000_000L);
                default -> BigDecimal.ONE;
            };
            return value.multiply(mult).setScale(0, RoundingMode.HALF_UP).longValueExact();
        } catch (Exception e) {
            return -1;
        }
    }

    /** 1500000 -> "$1.5M" */
    public static String format(long amount) {
        if (amount >= 1_000_000_000_000L) return "$" + trim(amount / 1e12) + "T";
        if (amount >= 1_000_000_000L) return "$" + trim(amount / 1e9) + "B";
        if (amount >= 1_000_000L) return "$" + trim(amount / 1e6) + "M";
        if (amount >= 1_000L) return "$" + trim(amount / 1e3) + "K";
        return "$" + amount;
    }

    private static String trim(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    /** Accepts "45", "45s", "2m", "1m30s", "1:30". Returns seconds, or -1 if invalid. */
    public static long parseDuration(String raw) {
        if (raw == null) return -1;
        String s = raw.trim().toLowerCase(Locale.ROOT).replace(" ", "");
        if (s.isEmpty()) return -1;
        try {
            if (s.matches("\\d+")) return Long.parseLong(s);
            Matcher colon = Pattern.compile("^(\\d+):(\\d{1,2})$").matcher(s);
            if (colon.matches()) return Long.parseLong(colon.group(1)) * 60 + Long.parseLong(colon.group(2));
            Matcher ms = Pattern.compile("^(?:(\\d+)m)?(?:(\\d+)s?)?$").matcher(s);
            if (ms.matches()) {
                long m = ms.group(1) == null ? 0 : Long.parseLong(ms.group(1));
                long sec = ms.group(2) == null ? 0 : Long.parseLong(ms.group(2));
                return m * 60 + sec;
            }
        } catch (NumberFormatException ignored) {
        }
        return -1;
    }

    /** 90 -> "1m 30s" */
    public static String formatDuration(long seconds) {
        long m = seconds / 60, s = seconds % 60;
        if (m == 0) return s + "s";
        if (s == 0) return m + "m";
        return m + "m " + s + "s";
    }

    /** milliseconds -> "0:23" */
    public static String clock(long ms) {
        long total = (ms + 999) / 1000;
        if (total < 0) total = 0;
        return (total / 60) + ":" + String.format("%02d", total % 60);
    }
}
