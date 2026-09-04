package com.planetaryfactory.core.mining;

/**
 * Which item abilities the Engineer's Pick performs (#168).
 *
 * <p>The answer is closed over every ability any mod defines, not only GregTech's wrench ones: the
 * Pick is the pack's only tool, so it is what gets asked, and everything undecided is a no.
 *
 * <p><b>Rotate, and nothing else.</b> Factorio has a rotate gesture ({@code R}), so a pack with no
 * way to turn a machine would be missing a mechanic rather than simplifying one. The four
 * {@code wrench_configure*} verbs are the pipe-connection half, and they are declined outright
 * rather than deferred: ADR-0017 gives fluid and item logistics to Create, GregTech's pipes left
 * with its power layer, and under ADR-0034's default-deny sweep nothing re-surfaces them. Declaring
 * those verbs would be declaring them against blocks Terra does not ship.
 *
 * <p><b>Why this is a string.</b> The answer lives here, apart from the item, because
 * {@code canPerformAction} takes an {@code ItemStack} and the pack's unit tests run with neither
 * Minecraft nor GregTech on the classpath -- which is the testing policy, not an accident. An
 * {@code ItemAbility} is a name and a name only, so the decision is expressible without either.
 *
 * <p>The consequence is that this is coupled to GTCEu's spelling rather than to its constants. That
 * is the same coupling the two wrench item tags in {@code kubejs/data} already have, and it is the
 * price of the verb being checkable at all. A GTCEu release that renamed the ability would reach us
 * as rotation quietly not working. Nothing static can catch that -- the residue is a world load, and
 * #168 records it as one rather than as the GameTest its original Checks section named, because what
 * would be under test is GregTech's own click path rather than any arithmetic of ours.
 */
public final class PickAbilities {

    /**
     * GTCEu 7.0.2's {@code GTItemAbilities.WRENCH_ROTATE}, whose name is the string it is
     * registered under.
     */
    private static final String ROTATE = "wrench_rotate";

    private PickAbilities() {
    }

    /**
     * Whether the Engineer's Pick performs the named ability.
     *
     * <p>Deliberately a closed answer: every ability any mod defines arrives here, because the Pick
     * is the only tool in the pack, and everything the pack has not decided on is a no.
     *
     * <p>Dismantle is not in this set even though the Pick dismantles machines. That verb is not
     * gated on an ability at all -- it rides the ordinary block-break path, which
     * {@link EngineersPick#isCorrectToolForDrops} already answers, reached through the two wrench
     * item tags (ADR-0039). Answering true to {@code wrench_dismantle} here would suggest the tags
     * were not what delivers it.
     */
    public static boolean grants(String abilityName) {
        return ROTATE.equals(abilityName);
    }
}
