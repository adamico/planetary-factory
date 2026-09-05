package com.planetaryfactory.core.ore;

import com.planetaryfactory.core.PFAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * The draw: one break gesture takes one unit, and the block stands until the count reaches zero.
 *
 * <p>This is ADR-0041's mechanic in one place. Hands and machines take from the same number, which
 * is what makes ADR-0039's "seconds per ore" literal rather than aspirational -- and it is why a
 * player's break is <em>cancelled</em> rather than allowed to destroy a block holding a thousand
 * units. The alternative, a hand break taking one unit and destroying the remainder, hands the
 * player a way to vandalise a patch for one ore.
 *
 * <p><b>A depleted block becomes stone</b>, uniformly, including for stone ore. ADR-0019 flattened
 * Terra and the fields lie flush with the topsoil; breaking to air would leave a pitted field that
 * a drill's own footprint then has to sit on.
 *
 * <p>The entry retires on any change away from the ore block, not only on depletion -- TNT, a
 * creative break, a structure overwriting it. Without that, a later ore block at the same position
 * inherits a stranger's delta and arrives part-mined, which is invisible until a fresh patch pays
 * out half of what it should.
 */
public final class OreMining {

    private OreMining() {
    }

    /**
     * A player breaking an ore block.
     *
     * <p>Cancelled for an ordinary break: either the block stands with one fewer unit in it, or it
     * is replaced with stone here. Vanilla's own loot never runs -- the table is empty on purpose
     * -- so {@link #drop} is the only payout, and it is one item. The block's whole amount is
     * never in a drop, by any route.
     *
     * <p><b>Two breaks are not draws and are let through.</b> A creative break is a build gesture,
     * not mining: cancelling it would leave a creative player unable to remove an ore block at all,
     * clicking a patch forever and spawning an item each time. And a break with the wrong tool
     * draws nothing, because vanilla's answer to mining ore bare-handed is no drop -- here that has
     * to mean the unit stays in the ground rather than being paid out for free. The wrong-tool
     * break is still cancelled, so the block survives the gesture with its amount intact; only the
     * creative one actually removes it, and {@link OreBlock#onRemove} retires the delta behind it.
     */
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockState state = event.getState();
        if (!(state.getBlock() instanceof OreBlock ore)) {
            return;
        }
        Player player = event.getPlayer();
        if (player != null && player.getAbilities().instabuild) {
            return;
        }
        event.setCanceled(true);
        if (player != null && !player.hasCorrectToolForDrops(state)) {
            return;
        }

        BlockPos pos = event.getPos();
        int initial = initialAmount(level, ore, pos);
        OreDelta delta = deltaOf(level, pos);
        OreDelta.Draw draw = delta.draw(pos.asLong(), initial);
        level.getChunk(pos).setUnsaved(true);

        if (draw.paid() > 0) {
            drop(level, pos, ore.resource());
        }
        if (draw.exhausted()) {
            // The hole argument is served by the stages and by the patch visibly shrinking; a
            // crater in a field a drill has to stand on is not.
            level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        } else {
            level.setBlockAndUpdate(pos, ore.stateFor(draw.remaining(), initial));
        }
    }

    /** What is left in the block at {@code pos}, for the HUD and for anything else that asks. */
    public static int remaining(Level level, BlockPos pos, OreBlock ore) {
        if (!(level instanceof ServerLevel server)) {
            return 0;
        }
        int initial = initialAmount(server, ore, pos);
        return deltaOf(server, pos).remaining(pos.asLong(), initial);
    }

    /** The block's initial amount: its field's quotient, or the outfield law's scaling of it. */
    public static int initialAmount(ServerLevel level, OreBlock ore, BlockPos pos) {
        OreFields fields = level.getDataStorage().computeIfAbsent(OreFields.FACTORY, OreFields.NAME);
        return fields.initialAmount(ore.resource(), pos, level.getSharedSpawnPos());
    }

    /**
     * Any other route out of being an ore block, so a delta never outlives the block it counted.
     *
     * <p>Called from {@link OreBlock#onRemove}, which is the one seam every removal goes through --
     * an explosion, a creative break, a structure overwriting the position. Depletion has already
     * retired its own entry by the time it gets here, and retiring twice costs nothing.
     */
    public static void onRemoved(Level level, BlockPos pos) {
        if (level instanceof ServerLevel server) {
            deltaOf(server, pos).retire(pos.asLong());
            server.getChunkAt(pos).setUnsaved(true);
        }
    }

    private static OreDelta deltaOf(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        return chunk.getData(PFAttachments.ORE_DELTA);
    }

    private static void drop(ServerLevel level, BlockPos pos, OreResource resource) {
        ResourceLocation id = ResourceLocation.parse(resource.drop());
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(id));
        if (stack.isEmpty()) {
            return;
        }
        level.addFreshEntity(new ItemEntity(
                level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack));
    }
}
