package org.lunatic.athenaTrades.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parse input uang seperti "1000", "1k", "2.5m", "1.2b" jadi nilai double.
 * K = ribu, M = juta, B = miliar. Case-insensitive.
 */
public final class AmountParser {

    private static final Pattern PATTERN = Pattern.compile("^([0-9]*\\.?[0-9]+)\\s*([kKmMbB]?)$");

    private AmountParser() {
    }

    /**
     * @return nilai hasil parse, atau null kalau format tidak valid / <= 0.
     */
    public static Double parse(String raw) {
        if (raw == null) return null;
        String input = raw.trim();
        if (input.isEmpty()) return null;

        Matcher matcher = PATTERN.matcher(input);
        if (!matcher.matches()) return null;

        double base;
        try {
            base = Double.parseDouble(matcher.group(1));
        } catch (NumberFormatException e) {
            return null;
        }

        String suffix = matcher.group(2).toLowerCase();
        double multiplier = switch (suffix) {
            case "k" -> 1_000d;
            case "m" -> 1_000_000d;
            case "b" -> 1_000_000_000d;
            default -> 1d;
        };

        double result = base * multiplier;
        if (result <= 0) return null;

        return result;
    }
}