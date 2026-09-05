package com.planetaryfactory.core.ore;

/**
 * Terra's five resources, and the item each ore block pays out (ADR-0041, amending ADR-0021).
 *
 * <p><b>The block changes and the item does not.</b> That is what makes a pack-authored ore block
 * affordable: {@code data/pack/item-map.json}, every generated recipe, ADR-0032's 1:1 ore-to-plate
 * chain and ADR-0034's default-deny sweep all name the item, and none of them can tell that the
 * block it came out of is no longer GregTech's.
 *
 * <p>Stone is the fifth and the one ADR-0021 refused. It drops {@code minecraft:cobblestone}, which
 * the item map already records as Factorio's stone -- "Factorio's stone is the MINED rock, and
 * Minecraft's mined rock is cobblestone" -- so the {@code stone-brick} chain is untouched by its
 * arrival.
 *
 * <p>The amounts are not here. They are Factorio's, they are extracted, and {@link OreCorpus} is
 * where they enter the mod.
 */
public enum OreResource {
    IRON("iron", "gtceu:raw_iron"),
    COPPER("copper", "gtceu:raw_copper"),
    COAL("coal", "minecraft:coal"),
    URANIUM("uranium", "gtceu:raw_uranium"),
    /** The fifth resource. A visually distinct ore, so a patch never reads as marked-up ground. */
    STONE("stone", "minecraft:cobblestone");

    private final String key;
    private final String drop;

    OreResource(String key, String drop) {
        this.key = key;
        this.drop = drop;
    }

    /** The corpus key and the block-id stem: {@code iron} gives {@code planetaryfactory:iron_ore}. */
    public String key() {
        return key;
    }

    public String blockName() {
        return key + "_ore";
    }

    /** The item one draw pays out. A string because nothing here should need a registry. */
    public String drop() {
        return drop;
    }

    public OreCorpus.Resource corpus() {
        return OreCorpus.get().resource(key);
    }

    public static OreResource of(String key) {
        for (OreResource resource : values()) {
            if (resource.key.equals(key)) {
                return resource;
            }
        }
        throw new IllegalArgumentException(key + " is not one of Terra's ores");
    }
}
