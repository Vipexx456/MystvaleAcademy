package com.ror.engine;

import com.ror.models.Gunner;
import com.ror.models.Hero;
import com.ror.models.Inventory.Inventory;
import com.ror.models.Mage;
import com.ror.models.Swordsman;
import com.ror.models.Test;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Properties;

public final class Load {
    public static final int SLOT_COUNT = 3;
    private static final Path SAVE_DIRECTORY = Path.of("saves");
    private static final String SAVE_FILE_PATTERN = "save-slot-%d.properties";
    private static final Path LEGACY_SAVE_FILE = SAVE_DIRECTORY.resolve("save.properties");
    private static final String NULL_VALUE = "__ROR_NULL__";
    private static final DateTimeFormatter SAVE_TIME_FORMAT = DateTimeFormatter
            .ofPattern("MMM d, yyyy h:mm a", Locale.ENGLISH)
            .withZone(ZoneId.systemDefault());

    private Load() {
    }

    public static boolean hasSave() {
        for (int slot = 1; slot <= SLOT_COUNT; slot++) {
            if (hasSave(slot)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasSave(int slot) {
        return Files.isRegularFile(resolveSaveFileForRead(slot));
    }

    public static Path getSaveFile() {
        return getSaveFile(1);
    }

    public static Path getSaveFile(int slot) {
        validateSlot(slot);
        return SAVE_DIRECTORY.resolve(String.format(SAVE_FILE_PATTERN, slot));
    }

    public static SlotInfo[] getSlotInfos() {
        SlotInfo[] slots = new SlotInfo[SLOT_COUNT];
        for (int slot = 1; slot <= SLOT_COUNT; slot++) {
            slots[slot - 1] = getSlotInfo(slot);
        }
        return slots;
    }

    public static SlotInfo getSlotInfo(int slot) {
        validateSlot(slot);
        Path file = resolveSaveFileForRead(slot);
        if (!Files.isRegularFile(file)) {
            return SlotInfo.empty(slot);
        }

        try {
            Properties properties = readProperties(file);
            String savedAtRaw = properties.getProperty("save.savedAt");
            String savedAtText = formatSavedAt(savedAtRaw, file);
            String heroName = nullIfBlank(properties.getProperty("hero.name"));
            String heroClass = nullIfBlank(properties.getProperty("hero.charClass"));
            int level = parseInt(properties.getProperty("hero.level"), 1);
            return SlotInfo.filled(slot, heroName, heroClass, level, savedAtText, file);
        } catch (IOException exception) {
            return SlotInfo.unreadable(slot, file);
        }
    }

    public static void saveGame(Hero hero) throws IOException {
        saveGame(hero, 1);
    }

    public static void saveGame(Hero hero, int slot) throws IOException {
        validateSlot(slot);
        if (hero == null) {
            throw new IllegalArgumentException("No hero selected to save.");
        }

        Properties properties = new Properties();
        properties.setProperty("save.version", "1");
        properties.setProperty("save.slot", String.valueOf(slot));
        properties.setProperty("save.savedAt", Instant.now().toString());
        properties.setProperty("hero.type", hero.getClass().getName());
        writeFields(properties, "hero.", Hero.class, hero);

        Inventory inventory = hero.getInventory();
        if (inventory != null) {
            writeFields(properties, "inventory.", Inventory.class, inventory);
        }

        Files.createDirectories(SAVE_DIRECTORY);
        try (OutputStream output = Files.newOutputStream(getSaveFile(slot))) {
            properties.store(output, "Mystvale Academy save file");
        }
    }

    public static Hero loadGame() throws IOException {
        return loadGame(1);
    }

    public static Hero loadGame(int slot) throws IOException {
        validateSlot(slot);
        if (!hasSave(slot)) {
            return null;
        }

        Properties properties = readProperties(resolveSaveFileForRead(slot));

        Hero hero = createHero(properties.getProperty("hero.type"), properties.getProperty("hero.charClass"));
        readFields(properties, "hero.", Hero.class, hero);

        Inventory inventory = hero.getInventory();
        if (inventory != null) {
            readFields(properties, "inventory.", Inventory.class, inventory);
        }

        return hero;
    }

    private static Properties readProperties(Path file) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
        }
        return properties;
    }

    private static Path resolveSaveFileForRead(int slot) {
        Path slotFile = getSaveFile(slot);
        if (Files.isRegularFile(slotFile)) {
            return slotFile;
        }
        if (slot == 1 && Files.isRegularFile(LEGACY_SAVE_FILE)) {
            return LEGACY_SAVE_FILE;
        }
        return slotFile;
    }

    private static void validateSlot(int slot) {
        if (slot < 1 || slot > SLOT_COUNT) {
            throw new IllegalArgumentException("Save slot must be between 1 and " + SLOT_COUNT + ".");
        }
    }

    private static String formatSavedAt(String savedAtRaw, Path file) throws IOException {
        Instant savedAt;
        if (savedAtRaw != null && !savedAtRaw.isBlank()) {
            savedAt = Instant.parse(savedAtRaw);
        } else {
            FileTime modifiedTime = Files.getLastModifiedTime(file);
            savedAt = modifiedTime.toInstant();
        }
        return SAVE_TIME_FORMAT.format(savedAt);
    }

    private static int parseInt(String raw, int fallback) {
        try {
            return raw == null ? fallback : Integer.parseInt(raw);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static String nullIfBlank(String value) {
        return value == null || value.isBlank() || NULL_VALUE.equals(value) ? null : value;
    }

    private static Hero createHero(String typeName, String charClass) {
        String key = typeName != null ? typeName : charClass;
        if (key == null) {
            return new Swordsman();
        }

        if (key.endsWith("Gunner") || key.equalsIgnoreCase("Gunner")) {
            return new Gunner();
        }
        if (key.endsWith("Mage") || key.equalsIgnoreCase("Mage")) {
            return new Mage();
        }
        if (key.endsWith("Test") || key.equalsIgnoreCase("Unknown")) {
            return new Test();
        }
        return new Swordsman();
    }

    private static void writeFields(Properties properties, String prefix, Class<?> owner, Object target) throws IOException {
        for (Field field : owner.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            Class<?> type = field.getType();
            if (!isSupportedType(type)) {
                continue;
            }

            try {
                field.setAccessible(true);
                Object value = field.get(target);
                properties.setProperty(prefix + field.getName(), value == null ? NULL_VALUE : String.valueOf(value));
            } catch (IllegalAccessException exception) {
                throw new IOException("Unable to save " + field.getName() + ".", exception);
            }
        }
    }

    private static void readFields(Properties properties, String prefix, Class<?> owner, Object target) throws IOException {
        for (Field field : owner.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            Class<?> type = field.getType();
            if (!isSupportedType(type)) {
                continue;
            }

            String raw = properties.getProperty(prefix + field.getName());
            if (raw == null) {
                continue;
            }

            try {
                field.setAccessible(true);
                if (type == int.class) {
                    field.setInt(target, Integer.parseInt(raw));
                } else if (type == long.class) {
                    field.setLong(target, Long.parseLong(raw));
                } else if (type == boolean.class) {
                    field.setBoolean(target, Boolean.parseBoolean(raw));
                } else if (type == String.class) {
                    field.set(target, NULL_VALUE.equals(raw) ? null : raw);
                }
            } catch (IllegalAccessException | NumberFormatException exception) {
                throw new IOException("Unable to load " + field.getName() + ".", exception);
            }
        }
    }

    private static boolean isSupportedType(Class<?> type) {
        return type == int.class || type == long.class || type == boolean.class || type == String.class;
    }

    public static final class SlotInfo {
        private final int slot;
        private final boolean occupied;
        private final boolean readable;
        private final String heroName;
        private final String heroClass;
        private final int level;
        private final String savedAt;
        private final Path file;

        private SlotInfo(int slot, boolean occupied, boolean readable, String heroName, String heroClass, int level,
                String savedAt, Path file) {
            this.slot = slot;
            this.occupied = occupied;
            this.readable = readable;
            this.heroName = heroName;
            this.heroClass = heroClass;
            this.level = level;
            this.savedAt = savedAt;
            this.file = file;
        }

        private static SlotInfo empty(int slot) {
            return new SlotInfo(slot, false, true, null, null, 0, null, getSaveFile(slot));
        }

        private static SlotInfo filled(int slot, String heroName, String heroClass, int level, String savedAt,
                Path file) {
            return new SlotInfo(slot, true, true, heroName, heroClass, level, savedAt, file);
        }

        private static SlotInfo unreadable(int slot, Path file) {
            return new SlotInfo(slot, true, false, null, null, 0, null, file);
        }

        public int getSlot() {
            return slot;
        }

        public boolean isOccupied() {
            return occupied;
        }

        public boolean isReadable() {
            return readable;
        }

        public String getHeroName() {
            return heroName;
        }

        public String getHeroClass() {
            return heroClass;
        }

        public int getLevel() {
            return level;
        }

        public String getSavedAt() {
            return savedAt;
        }

        public Path getFile() {
            return file;
        }

        public String getSummary() {
            if (!occupied) {
                return "Slot " + slot + ": Empty";
            }
            if (!readable) {
                return "Slot " + slot + ": Save file could not be read";
            }

            String name = heroName == null ? "Unknown Hero" : heroName;
            String className = heroClass == null ? "Unknown Class" : heroClass;
            return "Slot " + slot + ": " + name + " (" + className + "), Level " + level
                    + "\nSaved: " + savedAt;
        }
    }
}
