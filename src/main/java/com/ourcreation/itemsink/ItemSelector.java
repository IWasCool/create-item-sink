package com.ourcreation.itemsink;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

import java.util.HashSet;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/** Registry-based selectors: player language and renamed stacks never affect prices. */
public final class ItemSelector {
    private ItemSelector() {}

    public static Predicate<ResourceLocation> parse(String text) {
        String selector = text.trim();
        if (selector.startsWith("#")) {
            ResourceLocation tag = requireId(selector.substring(1));
            var members = new HashSet<ResourceLocation>();
            BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, tag)).ifPresent(holders ->
                    holders.forEach(holder -> members.add(BuiltInRegistries.ITEM.getKey(holder.value()))));
            return members::contains;
        }
        if (selector.startsWith("regex:") || selector.startsWith("idregex:")) {
            boolean fullId = selector.startsWith("idregex:");
            String expression = selector.substring(fullId ? "idregex:".length() : "regex:".length()).trim();
            if (expression.isEmpty()) {
                throw new IllegalArgumentException((fullId ? "idregex:" : "regex:") + " needs a pattern");
            }
            Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
            return id -> pattern.matcher(fullId ? id.toString() : id.getPath()).find();
        }
        if (selector.startsWith("keyword:")) {
            String phrase = selector.substring("keyword:".length()).trim()
                    .toLowerCase(Locale.ROOT).replaceAll("\\s+", "_");
            if (phrase.isEmpty() || !phrase.matches("[a-z0-9_./-]+")) {
                throw new IllegalArgumentException("keyword: needs an item-path word or phrase");
            }
            // An underscore, slash or dash separates words in registry paths. Java's
            // \\b treats underscores as word characters and is unsuitable here.
            Pattern pattern = Pattern.compile("(?:^|[-_/])" + Pattern.quote(phrase) + "(?=$|[-_/])",
                    Pattern.CASE_INSENSITIVE);
            return id -> pattern.matcher(id.getPath()).find();
        }
        if (selector.startsWith("contains:")) {
            String phrase = selector.substring("contains:".length()).trim()
                    .toLowerCase(Locale.ROOT).replaceAll("\\s+", "_");
            if (phrase.isEmpty()) {
                throw new IllegalArgumentException("contains: needs a phrase");
            }
            return id -> id.getPath().contains(phrase);
        }
        if (selector.startsWith("namespace:")) {
            String namespace = selector.substring("namespace:".length()).trim();
            if (!namespace.matches("[a-z0-9_.-]+")) {
                throw new IllegalArgumentException("namespace: needs a valid mod id");
            }
            return id -> id.getNamespace().equals(namespace);
        }
        if (selector.indexOf('*') >= 0 || selector.indexOf('?') >= 0) {
            if (!selector.matches("[a-z0-9_.:/?*\\-]+") || selector.chars().filter(c -> c == ':').count() > 1) {
                throw new IllegalArgumentException("invalid item wildcard: " + selector);
            }
            StringBuilder regex = new StringBuilder();
            for (char character : selector.toCharArray()) {
                regex.append(switch (character) {
                    case '*' -> ".*";
                    case '?' -> ".";
                    default -> Pattern.quote(String.valueOf(character));
                });
            }
            Pattern pattern = Pattern.compile(regex.toString());
            boolean fullId = selector.indexOf(':') >= 0;
            return id -> pattern.matcher(fullId ? id.toString() : id.getPath()).matches();
        }
        ResourceLocation item = requireId(selector);
        return item::equals;
    }

    public static boolean isExactItemId(String text) {
        String selector = text.trim();
        // These prefixes also look like legal namespaces to ResourceLocation.
        return !selector.startsWith("#") && !selector.startsWith("regex:")
                && !selector.startsWith("idregex:") && !selector.startsWith("keyword:")
                && !selector.startsWith("contains:") && !selector.startsWith("namespace:")
                && ResourceLocation.tryParse(selector) != null;
    }

    private static ResourceLocation requireId(String text) {
        ResourceLocation id = text.isBlank() ? null : ResourceLocation.tryParse(text);
        if (id == null) {
            throw new IllegalArgumentException("invalid item or tag id: " + text);
        }
        return id;
    }
}
