package com.ourcreation.itemsink;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableCommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Splits an existing flat config before NeoForge opens the four native config files. */
public final class ConfigMigration {
    private static final List<String> FILE_NAMES = List.of(
            "common.toml", "item_settings.toml", "original_values.toml", "keyword.toml");
    private static final List<String> KEYWORD_KEYS = List.of("createCorePricingRules", "keywordBlacklist");

    private ConfigMigration() {}

    /** Existing folder files win; missing files inherit values from the current flat config. */
    public static synchronized void migrate(Path configDirectory) {
        Path folder = configDirectory.resolve("item_sink");
        Map<Path, Path> staged = new LinkedHashMap<>();
        try {
            Files.createDirectories(folder);
            Map<Path, byte[]> originals = new LinkedHashMap<>();
            for (String name : List.of("item_sink-common.toml", "create_item_sink-common.toml")) {
                Path legacy = configDirectory.resolve(name);
                if (Files.exists(legacy)) {
                    if (!Files.isRegularFile(legacy)) throw new IOException("Legacy config is not a file: " + legacy);
                    originals.put(legacy, Files.readAllBytes(legacy));
                }
            }
            if (originals.isEmpty()) return;

            List<String> missing = new ArrayList<>();
            for (String name : FILE_NAMES) {
                Path destination = folder.resolve(name);
                if (Files.exists(destination)) readSnapshot(destination);
                else missing.add(name);
            }
            if (!missing.isEmpty()) {
                // Linked insertion order gives item_sink-common.toml precedence over the old namespace.
                Path source = originals.keySet().iterator().next();
                Map<String, CommentedConfig> sections = split(readSnapshot(source));
                for (String name : missing) {
                    Path temporary = Files.createTempFile(folder, name + ".migrating-", ".toml");
                    staged.put(folder.resolve(name), temporary);
                    writeSnapshot(temporary, sections.get(name));
                    if (!plain(sections.get(name)).equals(plain(readSnapshot(temporary)))) {
                        throw new IOException("Migrated config failed its value check: " + name);
                    }
                }
                // Publish only fully written, parsed files. A concurrently created destination wins.
                for (Map.Entry<Path, Path> entry : staged.entrySet()) {
                    try {
                        Files.move(entry.getValue(), entry.getKey());
                    } catch (FileAlreadyExistsException alreadyCreated) {
                        readSnapshot(entry.getKey());
                    }
                }
            }
            // Verify every destination before moving either original out of the config root.
            for (String name : FILE_NAMES) readSnapshot(folder.resolve(name));
            Map<Path, Path> archives = new LinkedHashMap<>();
            Path archiveFolder = folder.resolve("legacy");
            Files.createDirectories(archiveFolder);
            for (Map.Entry<Path, byte[]> original : originals.entrySet()) {
                if (!Arrays.equals(Files.readAllBytes(original.getKey()), original.getValue())) {
                    throw new IOException("Legacy config changed during migration; kept its original file: " + original.getKey());
                }
                archives.put(original.getKey(), archive(original.getKey(), original.getValue(), archiveFolder));
            }
            // Both originals have verified rollback copies before either is removed.
            for (Map.Entry<Path, Path> archive : archives.entrySet()) {
                if (!Arrays.equals(Files.readAllBytes(archive.getKey()), Files.readAllBytes(archive.getValue()))) {
                    throw new IOException("Legacy config changed before archival: " + archive.getKey());
                }
                Files.delete(archive.getKey());
            }
        } catch (IOException | RuntimeException error) {
            throw new IllegalStateException("Could not organize Item Sink configuration; legacy files are retained or backed up under "
                    + folder.resolve("legacy"), error);
        } finally {
            for (Path temporary : staged.values()) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // A completed or partially completed migration can safely be retried.
                }
            }
        }
    }

    private static Map<String, CommentedConfig> split(UnmodifiableCommentedConfig source) {
        Map<String, CommentedConfig> result = new LinkedHashMap<>();
        for (String name : FILE_NAMES) result.put(name, newConfig());
        CommentedConfig common = result.get("common.toml");
        for (String key : List.of("energyCapacity", "maxEnergyInput", "requireEnergy")) {
            copyPath(source, common, List.of("item_sink", key));
        }
        Object values = source.getRaw(List.of("item_sink", "values"));
        if (values != null && !(values instanceof UnmodifiableConfig)) {
            throw new IllegalArgumentException("item_sink.values must be a TOML table");
        }
        if (values instanceof UnmodifiableConfig table) {
            for (var entry : table.entrySet()) {
                String key = entry.getKey();
                String destination = key.equals("originalPricingRules") ? "original_values.toml"
                        : KEYWORD_KEYS.contains(key) ? "keyword.toml" : "item_settings.toml";
                copyPath(source, result.get(destination), List.of("item_sink", "values", key));
            }
        }
        return result;
    }

    private static void copyPath(UnmodifiableCommentedConfig source, CommentedConfig destination, List<String> path) {
        if (!source.contains(path)) return;
        destination.set(path, copyValue(source.getRaw(path)));
        for (int length = 1; length <= path.size(); length++) {
            List<String> part = path.subList(0, length);
            String comment = source.getComment(part);
            if (comment != null) destination.setComment(part, comment);
        }
    }

    private static CommentedConfig newConfig() {
        return CommentedConfig.of(LinkedHashMap::new, TomlFormat.instance());
    }

    private static Object copyValue(Object value) {
        if (value instanceof UnmodifiableConfig config) {
            CommentedConfig copy = newConfig();
            for (var entry : config.entrySet()) {
                List<String> path = List.of(entry.getKey());
                copy.set(path, copyValue(entry.getValue()));
                if (config instanceof UnmodifiableCommentedConfig commented) {
                    String comment = commented.getComment(path);
                    if (comment != null) copy.setComment(path, comment);
                }
            }
            return copy;
        }
        if (value instanceof List<?> list) {
            List<Object> copy = new ArrayList<>(list.size());
            for (Object element : list) copy.add(copyValue(element));
            return copy;
        }
        return value;
    }

    private static Map<String, Object> plain(UnmodifiableConfig config) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (var entry : config.entrySet()) result.put(entry.getKey(), plainValue(entry.getValue()));
        return result;
    }

    private static Object plainValue(Object value) {
        if (value instanceof UnmodifiableConfig config) return plain(config);
        if (value instanceof List<?> list) {
            List<Object> result = new ArrayList<>(list.size());
            for (Object element : list) result.add(plainValue(element));
            return result;
        }
        return value;
    }

    private static CommentedConfig readSnapshot(Path path) throws IOException {
        if (!Files.isRegularFile(path)) throw new IOException("Config is not a regular file: " + path);
        try (CommentedFileConfig file = CommentedFileConfig.builder(path, TomlFormat.instance())
                .sync().preserveInsertionOrder().build()) {
            file.load();
            return file.bulkCommentedRead(config -> (CommentedConfig) copyValue(config));
        }
    }

    private static void writeSnapshot(Path path, CommentedConfig values) {
        try (CommentedFileConfig file = CommentedFileConfig.builder(path, TomlFormat.instance())
                .sync().preserveInsertionOrder().build()) {
            file.bulkCommentedUpdate(config -> {
                for (var entry : values.entrySet()) config.set(List.of(entry.getKey()), copyValue(entry.getValue()));
                config.putAllComments(values);
            });
            file.save();
        }
    }

    private static Path archive(Path original, byte[] expected, Path folder) throws IOException {
        for (int number = 0; ; number++) {
            String suffix = number == 0 ? "" : "." + number;
            Path destination = folder.resolve(original.getFileName() + suffix);
            try {
                Files.copy(original, destination);
            } catch (FileAlreadyExistsException exists) {
                if (!Arrays.equals(Files.readAllBytes(destination), expected)) continue;
            }
            if (!Arrays.equals(Files.readAllBytes(destination), expected)) {
                throw new IOException("Legacy config backup failed its value check: " + destination);
            }
            return destination;
        }
    }
}
