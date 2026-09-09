package com.planetaryfactory.core.felling;

import com.planetaryfactory.core.PlanetaryFactoryCore;
import com.planetaryfactory.core.mining.EngineersPick;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * A tree is one entity: mining its base fells it (#205, ADR-0051).
 *
 * <p>Two halves of one gesture, and the whole difficulty is that they have to agree. The break-speed
 * listener charges {@code amount * 0.1375s} <em>before</em> the break lands, and the break listener
 * removes the tree <em>after</em> it -- so both ask {@link TreeShape} the same question and the
 * player pays for exactly the wood that arrives. A cheaper estimate on the charging side (the trunk
 * column's height, say) would be invisible until somebody counted.
 *
 * <p>Surveying on every break-speed tick is what that costs, so the answer is cached per player and
 * invalidated the moment they look at a different block. The cache is keyed by position and not by
 * time: a tree does not change shape while it is being broken, and the one case where it does --
 * somebody else chopping it -- ends with the base block gone and the break restarting anyway.
 *
 * <p><b>The base block is vanilla's.</b> Its own break drops its own log through the ordinary loot
 * path; this class removes the <em>rest</em> of the tree and drops those at the base. Taking the
 * base over as well would mean re-implementing loot for no gain, and would put two code paths on the
 * one block a player can also break in creative.
 */
public final class TreeFelling {

    /**
     * The blocks a felling gesture treats as part of a tree.
     *
     * <p>A tag, and therefore pack data (ADR-0015): which species Terra grows is content. It is also
     * how ADR-0051's "one rule, no list to maintain" holds -- Sapros's trees join by being added to
     * it, on the day #23 decides they should.
     */
    public static final TagKey<Block> FELLABLE = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, "fellable"));

    /** One survey per player, held only while they keep looking at the same block. */
    private static final Map<UUID, Survey> SURVEYS = new ConcurrentHashMap<>();

    private TreeFelling() {
    }

    /**
     * Charge the whole tree's time on the base block.
     *
     * <p>Vanilla computes a speed for one block; a felling gesture removes many, so the speed is
     * re-solved for the stated total. Everything else vanilla applies on top -- haste, being
     * underwater, being off the ground -- still applies, which ADR-0039 already accepts for the
     * Pick's ore times.
     */
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        if (!(player.getMainHandItem().getItem() instanceof EngineersPick pick)) {
            return;
        }
        BlockPos pos = event.getPosition().orElse(null);
        if (pos == null || !event.getState().is(FELLABLE)) {
            return;
        }

        FellTree tree = surveyFor(player, pos);
        if (!tree.fells()) {
            return;
        }
        float hardness = event.getState().getDestroySpeed(player.level(), pos);
        event.setNewSpeed(FellingCost.breakSpeed(hardness, tree.amount(), pick.tier()));
    }

    /**
     * Take the rest of the tree with the base block.
     *
     * <p>Not cancelled: the base breaks as it always did, and this runs beside it. A creative break
     * still fells -- removing a tree is what a creative player is asking for -- but drops nothing,
     * the same way vanilla's own creative break does not.
     */
    public static void onBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (event.isCanceled()
                || !(player.getMainHandItem().getItem() instanceof EngineersPick)
                || !(event.getLevel() instanceof Level level)
                || level.isClientSide()) {
            return;
        }
        BlockPos base = event.getPos();
        if (!event.getState().is(FELLABLE)) {
            return;
        }

        FellTree tree = surveyFor(player, base);
        SURVEYS.remove(player.getUUID());
        if (!tree.fells()) {
            return;
        }

        boolean drops = !player.isCreative();
        Map<Item, Integer> harvest = new LinkedHashMap<>();
        for (FellPos log : tree.logs()) {
            BlockPos pos = LevelTreeSurvey.toBlockPos(log);
            if (pos.equals(base)) {
                // Vanilla's own break, and vanilla's own drop.
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (drops) {
                Item item = state.getBlock().asItem();
                if (item != null) {
                    harvest.merge(item, 1, Integer::sum);
                }
            }
            level.removeBlock(pos, false);
        }
        // Leaves go with the trunk: no drops and no decay ticks, because Factorio has no leaves and
        // vanilla's decay is a tick storm this gesture can pre-empt for free (ADR-0051).
        for (FellPos leaf : tree.leaves()) {
            level.removeBlock(LevelTreeSurvey.toBlockPos(leaf), false);
        }

        // One entity, one payout, at the block the player was actually breaking.
        for (Map.Entry<Item, Integer> entry : harvest.entrySet()) {
            Block.popResource(level, base, new ItemStack(entry.getKey(), entry.getValue()));
        }
    }

    /** The survey for this player at this block, computed once and reused while they stay on it. */
    private static FellTree surveyFor(Player player, BlockPos pos) {
        Survey cached = SURVEYS.get(player.getUUID());
        if (cached != null && cached.pos().equals(pos)) {
            return cached.tree();
        }
        FellTree tree = TreeShape.survey(
                new LevelTreeSurvey(player.level()),
                LevelTreeSurvey.toFellPos(pos),
                FellBounds.DEFAULT);
        SURVEYS.put(player.getUUID(), new Survey(pos, tree));
        return tree;
    }

    /** Forget a player's survey when they leave, so the map does not outlive the session. */
    public static void onLogout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        SURVEYS.remove(event.getEntity().getUUID());
    }

    private record Survey(BlockPos pos, FellTree tree) {
    }
}
