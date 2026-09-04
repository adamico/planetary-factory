package com.planetaryfactory.core.mining;

import java.util.Set;

/**
 * Which item abilities the Engineer's Pick performs (#168).
 *
 * <p>The answer is closed over every ability any mod defines, not only GregTech's wrench ones: the
 * Pick is the pack's only tool, so it is what gets asked, and everything undecided is a no.
 *
 * <p><b>Rotate and configure, but not connect.</b> Factorio has a rotate gesture ({@code R}) and an
 * output side is half of what placing a machine means, so both are verbs the pack needs. What is
 * declined is {@code wrench_connect}, GregTech's pipe-connection verb: ADR-0017 gives fluid and item
 * logistics to Create, GregTech's pipes left with its power layer, and under ADR-0034's default-deny
 * sweep nothing re-surfaces them.
 *
 * <p><b>The configure verbs were declined once, on a misreading, and that was wrong.</b> #168
 * originally grouped all four {@code wrench_configure*} abilities with the pipes, following GTCEu's
 * lang string "Use Wrench to set Connections". They are not that. In {@code MetaMachine.onWrenchClick}
 * they set a <i>machine's</i> auto-output face -- {@code IAutoOutputItem.setOutputFacingItems} and
 * {@code IAutoOutputFluid.setOutputFacingFluids} -- which is which side the machine pushes into.
 * Create owning the belts is the reason that matters rather than a reason it does not, since a belt
 * has to be given a face to be fed from. The pipe verb is {@code wrench_connect}, a different
 * ability on the pipe block's own path, and that one really is moot here.
 *
 * <p><b>Why this is a string.</b> The answer lives here, apart from the item, because
 * {@code canPerformAction} takes an {@code ItemStack} and the pack's unit tests run with neither
 * Minecraft nor GregTech on the classpath -- which is the testing policy, not an accident. An
 * {@code ItemAbility} is a name and a name only, so the decision is expressible without either.
 *
 * <p>The consequence is that this is coupled to GTCEu's spelling rather than to its constants. That
 * is the same coupling the two wrench item tags in {@code kubejs/data} already have, and it is the
 * price of the verb being checkable at all. A GTCEu release that renamed an ability would reach us
 * as that verb quietly not working. Nothing static can catch that -- the residue is a world load,
 * and #168 records it as one rather than as the GameTest its original Checks section named, because
 * what would be under test is GregTech's own click path rather than any arithmetic of ours.
 */
public final class PickAbilities {

    /**
     * GTCEu 7.0.2's ability names, which are the strings each is registered under.
     *
     * <p>{@code wrench_configure} is the gate GregTech checks before it will read any of the three
     * below, so declaring the others without it performs nothing at all.
     */
    private static final Set<String> GRANTED = Set.of(
            "wrench_rotate",
            "wrench_configure",
            "wrench_configure_all",
            "wrench_configure_items",
            "wrench_configure_fluids");

    private PickAbilities() {
    }

    /**
     * Whether the Engineer's Pick performs the named ability.
     *
     * <p>Deliberately a closed answer: every ability any mod defines arrives here, because the Pick
     * is the only tool in the pack, and everything the pack has not decided on is a no.
     *
     * <p>Dismantle is not in the set even though the Pick dismantles machines. That verb is not
     * gated on an ability at all -- it rides the ordinary block-break path, which
     * {@link EngineersPick#isCorrectToolForDrops} already answers, reached through the two wrench
     * item tags (ADR-0039). Answering true to {@code wrench_dismantle} here would suggest the tags
     * were not what delivers it.
     */
    public static boolean grants(String abilityName) {
        return GRANTED.contains(abilityName);
    }
}
