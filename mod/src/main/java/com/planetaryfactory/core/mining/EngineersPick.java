package com.planetaryfactory.core.mining;

import com.planetaryfactory.core.PlanetaryFactoryCore;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Factorio's single mining gesture, wearing a pickaxe model (ADR-0039).
 *
 * <p><b>One tool, all block classes.</b> There is no axe, shovel, shears or hoe in this pack, so
 * this item is the correct tool for everything -- {@code requires_correct_tool_for_drops} is fixed
 * at block registration and no datapack reaches it, which is why the answer lives in the jar. It is
 * also the tool that dismantles a GregTech machine: that half is the two wrench item tags in
 * {@code kubejs/data}, not code, because GregTech and Create both ask a tag.
 *
 * <p><b>Indestructible.</b> No durability component at all, so there is no bar to read and nothing
 * to repair. Factorio's engineer never sharpens anything.
 *
 * <p>Two speeds, one behaviour: which blocks take Factorio's flat time is the block tag
 * {@link #FACTORIO_MINING_TIME}, and how fast the tool is on them is {@link PickTier}. Everything
 * outside the tag keeps vanilla's hardness spread -- ADR-0039 rejected a flat two seconds on dirt
 * as tedium rather than fidelity.
 */
public final class EngineersPick extends Item {

    /**
     * The blocks that take Factorio's flat seconds-per-item instead of vanilla's hardness spread.
     *
     * <p>A tag, and therefore pack data: Terra's resource set is a design decision (ADR-0019), and
     * this jar owns only the arithmetic that turns "two seconds" into a tool speed.
     */
    public static final TagKey<Block> FACTORIO_MINING_TIME = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE,
                                                  "factorio_mining_time"));

    /**
     * Vanilla's own iron-pickaxe speed, expressed per unit of Factorio mining speed.
     *
     * <p>{@code 12 x 0.5 = 6}, which is exactly {@code Tiers.IRON}, so the Iron Pick is a vanilla
     * iron pickaxe everywhere the tag does not reach. The Steel Pick doubles it for the same
     * reason it halves the flat time: {@code steel-axe} adds 1 to a base mining speed of 0.5, and
     * ADR-0039 records the research's outcome as "mining doubles" rather than "ore mining
     * doubles" -- Factorio's own {@code character-mining-speed} applies to everything the
     * character mines, so stopping the boost at the tag would be the divergence, not carrying it.
     *
     * <p>That does put the Steel Pick at 12, above netherite's 9. Nothing is being outclassed:
     * ADR-0034's sweep leaves the pack no other pickaxe at any tier, so vanilla's ladder is not a
     * ceiling this has to fit under -- it is a ladder the pack does not have.
     */
    private static final float VANILLA_SPEED_PER_MINING_SPEED = 12.0f;

    private final PickTier tier;

    public EngineersPick(PickTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public PickTier tier() {
        return tier;
    }

    /**
     * True for every block, which is the whole point.
     *
     * <p>Nothing else in the pack is a mining tool, so a block this returned false for would be a
     * block nobody can ever take -- and under ADR-0034's sweep there is no second tool to reach for.
     */
    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return true;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (state.is(FACTORIO_MINING_TIME)) {
            // The block's default hardness, not the state's in place: this method is handed no
            // level and no position, so there is no in-world hardness to ask for. Every block in
            // the tag has one hardness for all its states, and a block whose hardness varied by
            // state would take its default's time here.
            return MiningSpeed.forSeconds(state.getBlock().defaultDestroyTime(),
                                          tier.secondsPerResource());
        }
        return VANILLA_SPEED_PER_MINING_SPEED * tier.miningSpeed();
    }

    /**
     * The seconds, said out loud.
     *
     * <p>The number the research changes is the reason to hold the second tier at all, and a player
     * who cannot see it has no way to tell the two picks apart but the name.
     */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.planetaryfactory.engineers_pick.speed",
                                           String.format("%.1f", tier.secondsPerResource()))
                            .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.planetaryfactory.engineers_pick.universal")
                            .withStyle(ChatFormatting.DARK_GRAY));
    }

}
