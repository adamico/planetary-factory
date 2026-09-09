package com.planetaryfactory.core.felling;

/**
 * What stops the fill.
 *
 * <p>ADR-0051: the bound exists to cap the work a break gesture can ask of the server thread, not to
 * teach the player a rule. Over the bound the gesture fells what fits and leaves the rest standing
 * as an ordinary tree to break again -- which is the pre-ticket behaviour, so the failure mode is
 * one nobody has to have explained to them.
 *
 * <p>Three bounds rather than one, because they fail differently. {@code maxBlocks} is the work cap
 * and the only one a normal tree ever meets. {@code maxRadius} and {@code maxHeight} are shape
 * caps: a fill that has wandered forty blocks sideways has found a canopy bridge or a build, and
 * following it is wrong even when it is cheap.
 *
 * @param maxBlocks  logs plus leaves, the total the gesture may remove
 * @param maxRadius  how far horizontally from the base the fill may reach
 * @param maxHeight  how far above the base the fill may reach
 */
public record FellBounds(int maxBlocks, int maxRadius, int maxHeight) {

    /**
     * The bounds the game runs with.
     *
     * <p>A dark oak with its full canopy is a few hundred blocks and a jungle giant reaches about
     * thirty up; 512 clears both with room, while still being a number rather than "whatever the
     * chunk holds". The radius is deliberately tighter than the block cap implies -- a legitimate
     * tree is narrow, and sixteen is already twice the widest vanilla canopy.
     */
    public static final FellBounds DEFAULT = new FellBounds(512, 16, 64);
}
