package com.planetaryfactory.core.assembler;

import com.mojang.brigadier.StringReader;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

/**
 * The one crossing between an Assembler item key and an {@code ItemStack} (ADR-0052).
 *
 * <p>{@link ItemKey} is the format and knows no Minecraft. This is the resolve half: it needs a
 * registry and a {@code HolderLookup.Provider}, so it is called only where one already exists --
 * {@code InventoryPlayerItems}, {@code RuntimeHandRecipes}, {@code RuntimePlanSource} and the
 * client's {@code PlanItems}. ADR-0038 claimed a single crossing point and by #222 there were five,
 * each doing its own {@code BuiltInRegistries.ITEM.getKey(...)} and each therefore blind to
 * components. They agree again because they all come through here.
 *
 * <p>The two directions report an absent answer differently, and each caller handles its own:
 * {@link #of} answers null for a stack the format cannot name -- a transient or unencodable
 * component -- while {@link #toStack} answers {@link ItemStack#EMPTY} for a key that resolves to
 * nothing.
 *
 * <p>A key can be well-formed and still resolve to nothing -- an id nothing is registered under, a
 * component the game does not know. Both answer {@link ItemStack#EMPTY} rather than throwing;
 * refusing such a key is {@code RuntimeHandRecipes}' job, at the graph, out loud.
 */
public final class ItemKeys {

    private static final Logger LOGGER = LogUtils.getLogger();

    private ItemKeys() {}

    /**
     * The key naming this stack's item and components, or null when the format cannot name it. The
     * count is not part of the identity.
     */
    public static String of(ItemStack stack, HolderLookup.Provider registries) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        DataComponentPatch patch = stack.getComponentsPatch();
        if (patch.isEmpty()) return id;
        List<ItemKey.Entry> entries = new ArrayList<>();
        for (Map.Entry<DataComponentType<?>, Optional<?>> entry : patch.entrySet()) {
            String component = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(entry.getKey()).toString();
            if (entry.getValue().isEmpty()) {
                entries.add(ItemKey.Entry.removed(component));
                continue;
            }
            String value = encode(entry.getKey(), entry.getValue().get(), registries);
            if (value == null) return null;
            entries.add(ItemKey.Entry.set(component, value));
        }
        return ItemKey.of(id, entries);
    }

    @SuppressWarnings("unchecked")
    private static String encode(DataComponentType<?> type, Object value, HolderLookup.Provider registries) {
        DataComponentType<Object> typed = (DataComponentType<Object>) type;
        if (typed.codec() == null) return null; // a transient component; nothing can name it
        return typed.codec()
                .encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), value)
                .resultOrPartial(error -> LOGGER.warn("Assembler: cannot name {}: {}", type, error))
                .map(Tag::toString)
                .orElse(null);
    }

    /**
     * The stack a key names, or {@link ItemStack#EMPTY} when nothing is registered under it.
     *
     * <p>Vanilla's own parser, because the key is written in vanilla's own syntax and a second
     * reader of that syntax is a second thing to keep in step with it.
     */
    public static ItemStack toStack(String key, int count, HolderLookup.Provider registries) {
        try {
            ItemParser.ItemResult parsed = new ItemParser(registries).parse(new StringReader(key));
            return new ItemStack(parsed.item(), count, parsed.components());
        } catch (Exception failure) {
            return ItemStack.EMPTY;
        }
    }

    /** The stack an {@link ItemAmount} names, at its own count. */
    public static ItemStack toStack(ItemAmount amount, HolderLookup.Provider registries) {
        return toStack(amount.item(), amount.count(), registries);
    }

    /**
     * Whether a stack is the item a key names.
     *
     * <p>Full component equality, not a subset match: a key with no patch names the pristine item
     * and nothing else (ADR-0052). Comparing against a resolved prototype rather than re-encoding
     * each stack keeps a count over thirty-six slots to one parse.
     *
     * <p>ADR-0052's rule is string equality, and this is the same question asked one step later:
     * two stacks that encode to one key resolve to one component map, and two that do not, do not.
     * Where they could part company is a component whose codec does not round-trip its own value,
     * and the answer to that is the same either way -- the recipe's own stack and the player's
     * stack both come through {@link #of}, so a key is only ever compared with a key.
     */
    public static boolean matches(ItemStack prototype, ItemStack stack) {
        return !prototype.isEmpty() && ItemStack.isSameItemSameComponents(prototype, stack);
    }
}
