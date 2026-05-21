package com.ror.utils;

import com.ror.models.Hero;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

public final class LeaderboardManager {
    private static final Path SAVE_DIRECTORY = Path.of("saves");
    private static final Path LEADERBOARD_FILE = SAVE_DIRECTORY.resolve("leaderboard.properties");
    private static final int MAX_ENTRIES = 10;
    private static final DateTimeFormatter COMPLETED_AT_FORMAT = DateTimeFormatter
            .ofPattern("MMM d, yyyy h:mm a", Locale.ENGLISH)
            .withZone(ZoneId.systemDefault());

    private LeaderboardManager() {
    }

    public static List<Entry> loadEntries() {
        Properties properties = new Properties();
        if (Files.isRegularFile(LEADERBOARD_FILE)) {
            try (InputStream input = Files.newInputStream(LEADERBOARD_FILE)) {
                properties.load(input);
            } catch (IOException ignored) {
                return List.of();
            }
        }

        int count = parseInt(properties.getProperty("entry.count"), 0);
        List<Entry> entries = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            Entry entry = readEntry(properties, index);
            if (entry != null) {
                entries.add(entry);
            }
        }
        entries.sort(ENTRY_ORDER);
        if (entries.size() > MAX_ENTRIES) {
            return List.copyOf(entries.subList(0, MAX_ENTRIES));
        }
        return List.copyOf(entries);
    }

    public static void recordCompletion(Hero hero, long forestMillis, long edgeMillis, long forsakenMillis, long totalMillis)
            throws IOException {
        List<Entry> entries = new ArrayList<>(loadEntries());
        entries.add(new Entry(
                hero == null ? "Unknown" : safeText(hero.getName(), "Unknown"),
                hero == null ? "Unknown" : safeText(hero.getCharClass(), "Unknown"),
                Math.max(0L, forestMillis),
                Math.max(0L, edgeMillis),
                Math.max(0L, forsakenMillis),
                Math.max(0L, totalMillis),
                Instant.now().toString()));

        entries.sort(ENTRY_ORDER);
        if (entries.size() > MAX_ENTRIES) {
            entries = new ArrayList<>(entries.subList(0, MAX_ENTRIES));
        }

        Files.createDirectories(SAVE_DIRECTORY);
        Properties properties = new Properties();
        properties.setProperty("entry.count", String.valueOf(entries.size()));
        for (int index = 0; index < entries.size(); index++) {
            writeEntry(properties, index, entries.get(index));
        }

        try (OutputStream output = Files.newOutputStream(LEADERBOARD_FILE)) {
            properties.store(output, "Mystvale Academy leaderboard");
        }
    }

    public static String formatDuration(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (hours > 0L) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%02d:%02d", minutes, seconds);
    }

    private static Entry readEntry(Properties properties, int index) {
        String prefix = "entry." + index + ".";
        String heroName = properties.getProperty(prefix + "heroName");
        String heroClass = properties.getProperty(prefix + "heroClass");
        if (heroName == null || heroName.isBlank()) {
            return null;
        }

        return new Entry(
                heroName,
                safeText(heroClass, "Unknown"),
                parseLong(properties.getProperty(prefix + "forestMillis"), 0L),
                parseLong(properties.getProperty(prefix + "edgeMillis"), 0L),
                parseLong(properties.getProperty(prefix + "forsakenMillis"), 0L),
                parseLong(properties.getProperty(prefix + "overallMillis"), 0L),
                safeText(properties.getProperty(prefix + "completedAt"), ""));
    }

    private static void writeEntry(Properties properties, int index, Entry entry) {
        String prefix = "entry." + index + ".";
        properties.setProperty(prefix + "heroName", entry.heroName());
        properties.setProperty(prefix + "heroClass", entry.heroClass());
        properties.setProperty(prefix + "forestMillis", String.valueOf(entry.forestMillis()));
        properties.setProperty(prefix + "edgeMillis", String.valueOf(entry.edgeMillis()));
        properties.setProperty(prefix + "forsakenMillis", String.valueOf(entry.forsakenMillis()));
        properties.setProperty(prefix + "overallMillis", String.valueOf(entry.overallMillis()));
        properties.setProperty(prefix + "completedAt", entry.completedAt());
    }

    private static int parseInt(String value, int fallback) {
        try {
            return value == null ? fallback : Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static long parseLong(String value, long fallback) {
        try {
            return value == null ? fallback : Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static final Comparator<Entry> ENTRY_ORDER = Comparator
            .comparingLong(Entry::overallMillis)
            .thenComparingLong(Entry::forsakenMillis)
            .thenComparingLong(Entry::edgeMillis)
            .thenComparingLong(Entry::forestMillis)
            .thenComparing(Entry::heroName, String.CASE_INSENSITIVE_ORDER);

    public record Entry(
            String heroName,
            String heroClass,
            long forestMillis,
            long edgeMillis,
            long forsakenMillis,
            long overallMillis,
            String completedAt) {

        public String completedAtDisplay() {
            if (completedAt == null || completedAt.isBlank()) {
                return "";
            }
            try {
                return COMPLETED_AT_FORMAT.format(Instant.parse(completedAt));
            } catch (Exception ignored) {
                return completedAt;
            }
        }
    }
}
