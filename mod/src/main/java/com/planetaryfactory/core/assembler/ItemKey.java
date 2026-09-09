package com.planetaryfactory.core.assembler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * How the Assembler names an item: its registry id together with its data component patch, encoded
 * as one string (ADR-0052).
 *
 * <p>This is the format half, and it knows nothing about Minecraft. Building a key from an id and a
 * patch, reading one back apart, and the ordering rule are all here; turning a key into an
 * {@code ItemStack} needs a registry and a {@code HolderLookup.Provider} and lives in
 * {@code ItemKeys}. Splitting it is what keeps the format itself a unit test.
 *
 * <p>An item with an empty patch encodes to exactly its registry id, so every key written before
 * ADR-0052 -- every committed queue attachment, every test fixture -- is unchanged. A
 * component-bearing item encodes in vanilla's own item-argument syntax:
 *
 * <pre>researchd:research_pack[researchd:research_pack="planetary_factory:automation_science_pack"]</pre>
 *
 * <p>That and not a hash, because a queue attachment on disk and a refusal line in a log are both
 * read by a person, and the one thing that made #222 diagnosable was a log line naming the item.
 *
 * <p>Two keys are the same item when the strings are equal -- a deliberate divergence from
 * {@code neoforge:components}' subset match, which is not expressible as string equality and would
 * cost the Minecraft-free resolver.
 */
public final class ItemKey {

    private static final char START = '[';
    private static final char END = ']';
    private static final char SEPARATOR = ',';
    private static final char ASSIGNMENT = '=';
    private static final char REMOVED = '!';

    private ItemKey() {}

    /**
     * One entry of a patch: a component set to {@code value}, or removed when {@code value} is null.
     *
     * <p>{@code value} is the component's already-encoded SNBT. Encoding it is the Minecraft half's
     * job; ordering and framing it is this one's.
     */
    public record Entry(String component, String value) {

        public Entry {
            if (component == null || component.isBlank()) {
                throw new IllegalArgumentException("a patch entry needs a component id");
            }
        }

        public static Entry set(String component, String value) {
            if (value == null) throw new IllegalArgumentException("a set entry needs a value");
            return new Entry(component, value);
        }

        public static Entry removed(String component) {
            return new Entry(component, null);
        }

        public boolean isRemoval() {
            return value == null;
        }
    }

    /**
     * The key for an item id and its patch.
     *
     * <p>Entries are sorted by component id, because {@code DataComponentPatch} guarantees no
     * iteration order across loads and the same stack must always produce the same string. Every
     * science pack has exactly one component today, which is why skipping the sort would work
     * perfectly until the first two-component item and then produce two keys for one item.
     */
    public static String of(String id, List<Entry> patch) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("an item key needs an id");
        if (patch == null || patch.isEmpty()) return id;
        List<Entry> sorted = new ArrayList<>(patch);
        sorted.sort(Comparator.comparing(Entry::component));
        StringBuilder key = new StringBuilder(id).append(START);
        for (int i = 0; i < sorted.size(); i++) {
            Entry entry = sorted.get(i);
            if (i > 0) key.append(SEPARATOR);
            if (entry.isRemoval()) {
                key.append(REMOVED).append(entry.component());
            } else {
                key.append(entry.component()).append(ASSIGNMENT).append(entry.value());
            }
        }
        return key.append(END).toString();
    }

    /** The bare registry id, without the patch. */
    public static String itemId(String key) {
        int start = key.indexOf(START);
        return start < 0 ? key : key.substring(0, start);
    }

    /** Whether the key names anything beyond a pristine item. */
    public static boolean hasPatch(String key) {
        return key.indexOf(START) >= 0;
    }
}
