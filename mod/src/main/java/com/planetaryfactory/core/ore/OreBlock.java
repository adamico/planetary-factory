package com.planetaryfactory.core.ore;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;

/**
 * A Terra ore block: an amount, rendered as one of Factorio's eight stages.
 *
 * <p>Pack-authored rather than GregTech's, and ADR-0041 gives the reason: GregTech registers its
 * material ore blocks in code and models them at runtime, so a stage property on <em>its</em> block
 * would mean a mixin into both its registration and its model provider, across every material and
 * stone type it registers, to get the behaviour for five.
 *
 * <p>It is still a GregTech ore in every way the rest of the pack can observe: it carries
 * {@code c:ores}, which is the tag GregTech's own Miner scans for, and it pays out GregTech's raw
 * ore item.
 *
 * <p><b>The block holds no amount.</b> The amount is derived from the position and the difference
 * is a chunk attachment ({@link OreDelta}), because a starting field is around 1150 blocks and a
 * block entity apiece is not affordable at that scale. What the blockstate carries is the
 * <em>stage</em> -- a small integer recomputed from the amount, never a second copy of it.
 */
public final class OreBlock extends Block {

    /**
     * Which sprite the block is showing: {@code 0} untouched, the last nearly gone.
     *
     * <p>Sized from the corpus rather than from an eight typed here, so a Factorio release that
     * changed its stage ladder changes this property when the extractor is re-run.
     */
    public static final IntegerProperty STAGE =
            IntegerProperty.create("stage", 0, OreCorpus.get().stageCount() - 1);

    private final OreResource resource;

    public OreBlock(OreResource resource) {
        super(Properties.of()
                .mapColor(MapColor.STONE)
                .requiresCorrectToolForDrops()
                .strength(3.0f, 3.0f)
                .sound(SoundType.STONE));
        this.resource = resource;
        registerDefaultState(stateDefinition.any().setValue(STAGE, 0));
    }

    public OreResource resource() {
        return resource;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }

    /**
     * The state showing the right sprite for what is left in the block.
     *
     * <p>One reading rendered twice, which is exactly what ADR-0020's objection to worn textures
     * required: the stage cannot disagree with the amount because it is computed from it.
     */
    public BlockState stateFor(int remaining, int initial) {
        return defaultBlockState()
                .setValue(STAGE, OreStage.stage(remaining, initial, resource.corpus().stageRatios()));
    }

    /**
     * Drop the position's delta when the block stops being this block.
     *
     * <p>Every removal goes through here -- depletion, an explosion, a creative break, a structure
     * overwriting the position -- and ADR-0041 asks for exactly that: an entry that outlived its
     * block would be inherited by the next one, which arrives part-mined with nothing to show for
     * it. A stage change is not a removal, so mining a block does not trip this.
     */
    @Override
    protected void onRemove(BlockState state, Level level,
                            BlockPos pos, BlockState replacement,
                            boolean moving) {
        if (!state.is(replacement.getBlock())) {
            OreMining.onRemoved(level, pos);
        }
        super.onRemove(state, level, pos, replacement, moving);
    }
}
