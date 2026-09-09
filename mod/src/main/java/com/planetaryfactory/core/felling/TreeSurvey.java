package com.planetaryfactory.core.felling;

/**
 * Everything the fill is allowed to ask about the world.
 *
 * <p>Four questions, and the fourth is the interesting one. {@link #leafDistance} is vanilla's own
 * {@code distance} property on a leaf block -- how many steps it is from the nearest log -- and it
 * is the only thing that keeps one tree's fill out of a neighbour's canopy. Modelling it here rather
 * than in {@link TreeFelling} is what lets the "does not cross into a touching tree" claim be a unit
 * test instead of a world load.
 */
public interface TreeSurvey {

    boolean isLog(FellPos pos);

    boolean isLeaf(FellPos pos);

    /** Vanilla's {@code distance}: steps from the nearest log, 1 through 7. */
    int leafDistance(FellPos pos);

    /** A leaf that grew, rather than one a player placed -- vanilla's {@code persistent}, negated. */
    boolean isNaturalLeaf(FellPos pos);
}
