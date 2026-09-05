package com.planetaryfactory.core.ore;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * The starting fields this world actually dealt, and what one of their blocks is worth.
 *
 * <p>ADR-0041 keeps the patch total as the invariant and makes the per-block amount the quotient
 * over the blocks in the field. Only the world knows the second number: the jigsaw deals one of
 * three size variants per resource, and vanilla drops an overlapping child silently, so the field
 * that got placed is the only field worth dividing by. So the blocks are counted once, at the
 * moment the starting area is stamped, and what is written down is four small records rather than
 * a counter per ore block.
 *
 * <p>This is not the parallel counter ADR-0020 refused. It holds no remaining amount and never
 * changes as the patch is mined; it is the *initial* amount's derivation, fixed at placement.
 * What is left in a given block is the block's own business ({@link OreDelta}).
 *
 * <p>A block outside every recorded field -- an outfield vein, a block someone placed by hand --
 * falls back on the same arithmetic scaled by Factorio's distance law, which is flat inside 1600
 * blocks of spawn and rises beyond it.
 */
public final class OreFields extends SavedData {

    public static final String NAME = "planetaryfactory_ore_fields";

    public static final Factory<OreFields> FACTORY =
            new Factory<>(OreFields::new, OreFields::load, null);

    private final List<Field> fields = new ArrayList<>();

    public OreFields() {
    }

    public static OreFields load(CompoundTag tag, HolderLookup.Provider registries) {
        OreFields data = new OreFields();
        ListTag list = tag.getList("fields", CompoundTag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            CompoundTag entry = list.getCompound(index);
            int[] box = entry.getIntArray("box");
            data.fields.add(new Field(
                    entry.getString("resource"),
                    new BoundingBox(box[0], box[1], box[2], box[3], box[4], box[5]),
                    entry.getInt("amount")));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Field field : fields) {
            CompoundTag entry = new CompoundTag();
            entry.putString("resource", field.resource());
            BoundingBox box = field.box();
            entry.putIntArray("box", new int[] {
                    box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()});
            entry.putInt("amount", field.amountPerBlock());
            list.add(entry);
        }
        tag.put("fields", list);
        return tag;
    }

    /**
     * Record a placed field: the resource, where it landed, and how many ore blocks it holds.
     *
     * <p>The amount is {@link OreField}'s quotient, so the arithmetic that decides it is the one
     * the unit tests cover rather than a second copy of it here. The placed record is handed back
     * so a caller wanting to report the amount reads it rather than dividing a second time.
     */
    public Field record(OreResource resource, BoundingBox box, int blocks) {
        OreField field = new OreField(resource.key(), resource.corpus().startingAmount(), blocks);
        Field placed = new Field(resource.key(), box, field.amountPerBlock());
        fields.add(placed);
        setDirty();
        return placed;
    }

    public List<Field> fields() {
        return List.copyOf(fields);
    }

    /**
     * The initial amount of the block at {@code pos}, or {@code 0} where nothing here places one.
     *
     * <p>A position inside a recorded field takes that field's amount. Anything else -- an outfield
     * vein -- takes the same resource's starting-field amount scaled by Factorio's own richness
     * term, which is 1.0 everywhere inside 1600 blocks of spawn. That keeps one arithmetic for the
     * whole planet: ADR-0020's "the starting patches and the outfield differ by size and access,
     * not by mechanic".
     */
    public int initialAmount(OreResource resource, BlockPos pos, BlockPos spawn) {
        for (Field field : fields) {
            if (field.resource().equals(resource.key()) && field.box().isInside(pos)) {
                return field.amountPerBlock();
            }
        }
        int reference = referenceAmount(resource);
        if (reference <= 0) {
            return 0;
        }
        double distance = Math.sqrt(pos.distSqr(spawn));
        return Math.max(1, (int) Math.round(reference * OreCorpus.get().distanceLaw().richnessAt(distance)));
    }

    /**
     * What one block of this resource's starting field holds, for a world that dealt one.
     *
     * <p>A resource with no starting field -- uranium, which Factorio gives none -- falls back on
     * the smallest amount any <em>other</em> resource's field recorded, rather than on nothing:
     * an outfield vein of blocks holding zero is a vein that pays out nothing at all. The smallest
     * is deliberate and not arbitrary -- uranium is the scarcest thing on the planet, so the
     * leanest field on record is the closest honest stand-in for a total Factorio never states.
     * The minimum is order-independent, so which field was stamped first does not decide it.
     */
    private int referenceAmount(OreResource resource) {
        int smallestOther = 0;
        for (Field field : fields) {
            if (field.resource().equals(resource.key())) {
                return field.amountPerBlock();
            }
            smallestOther = smallestOther == 0
                    ? field.amountPerBlock()
                    : Math.min(smallestOther, field.amountPerBlock());
        }
        return smallestOther;
    }

    /**
     * One placed field.
     *
     * @param resource the corpus key
     * @param box where the field's template landed
     * @param amountPerBlock the patch total over the blocks that were actually placed
     */
    public record Field(String resource, BoundingBox box, int amountPerBlock) {
    }
}
