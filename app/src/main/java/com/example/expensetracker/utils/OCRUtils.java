package com.example.expensetracker.utils;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OCRUtils {

    public static Double extractAmount(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        String cleanText = text
                .replace("₹", " Rs ")
                .replace("रु", " Rs ")
                .replace("Rs.", " Rs ")
                .replace("rs.", " Rs ")
                .replace("RS", " Rs ")
                .replace("—", "-")
                .replace("–", "-");

        Pattern amountLabelPattern = Pattern.compile(
                "(?i)(amount|total\\s*amount|grand\\s*total|total)"
                        + "\\s*[:=\\-]?\\s*"
                        + "(?:rs\\s*)?"
                        + "₹?\\s*"
                        + "([0-9][0-9,]*(?:\\.[0-9]{1,2})?)"
        );

        Matcher amountLabelMatcher = amountLabelPattern.matcher(cleanText);
        while (amountLabelMatcher.find()) {
            try {
                String number = amountLabelMatcher.group(2).replace(",", "");
                double value = Double.parseDouble(number);
                if (value > 0) return value;
            } catch (Exception ignored) {}
        }

        Pattern currencyPattern = Pattern.compile(
                "(?i)(?:₹|rs\\.?|inr)\\s*"
                        + "([0-9][0-9,]*(?:\\.[0-9]{1,2})?)"
        );

        Matcher currencyMatcher = currencyPattern.matcher(cleanText);
        Double largestAmount = null;
        while (currencyMatcher.find()) {
            try {
                String number = currencyMatcher.group(1).replace(",", "");
                double value = Double.parseDouble(number);
                if (value > 0 && (largestAmount == null || value > largestAmount)) {
                    largestAmount = value;
                }
            } catch (Exception ignored) {}
        }

        if (largestAmount != null) return largestAmount;

        Pattern numberPattern = Pattern.compile("\\b([0-9]{2,}(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?)\\b");
        Matcher numberMatcher = numberPattern.matcher(cleanText);
        while (numberMatcher.find()) {
            try {
                String number = numberMatcher.group(1).replace(",", "");
                double value = Double.parseDouble(number);
                if (value >= 10 && (largestAmount == null || value > largestAmount)) {
                    largestAmount = value;
                }
            } catch (Exception ignored) {}
        }

        return largestAmount;
    }

    public static String extractDescription(String text) {
        if (text == null || text.trim().isEmpty()) return "Scanned Receipt";
        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            String clean = line.trim();
            if (clean.toLowerCase(Locale.getDefault()).startsWith("description")) {
                String description = clean.replaceFirst("(?i)description\\s*[:=\\-]?\\s*", "").trim();
                if (!description.isEmpty()) return description;
            }
        }
        for (String line : lines) {
            String clean = line.trim();
            if (clean.length() >= 3 && clean.length() <= 50 && !clean.toLowerCase().contains("amount") && !clean.matches(".*\\d{3,}.*")) {
                return clean;
            }
        }
        return "Scanned Receipt";
    }
}
