package com.planetaryfactory.core.fluid;

import java.util.Collection;

/**
 * Where an Offshore Pump may stand (ADR-0050): beside natural water, and nothing else.
 *
 * <p>The predicate is deliberately the smallest thing that can be written -- <b>one adjacent block
 * whose fluid state is a source</b>. It carries no minimum body size and no biome test, both of
 * which ADR-0050 considered and rejected, because water in Factorio is not something the player
 * hunts for and a pump that only works on an ocean would make it so here.
 *
 * <p><b>Why a blockstate is a sufficient check.</b> It is not, on its own: vanilla records nothing
 * that distinguishes a poured source from a worldgen one -- {@code BucketItem} empties as
 * {@code content.defaultFluidState().createLegacyBlock()}, byte-identical to what the generator
 * lays down. What makes this predicate sound is the rule around it. With source formation off
 * ({@link WaterConservation}) and Create's two source-placing flags off, nothing in the pack can
 * bring a source into existence, so every source block in the world is one worldgen or a structure
 * placed. Naturalness is guaranteed by construction rather than tracked.
 *
 * <p>That is the reason ADR-0050 deleted an entire chunk-attachment design that marked placed water
 * per block: it <em>failed open</em>, because its correctness rested on an exhaustive list of the
 * ways a source can be placed, and the list was never going to be exhaustive.
 *
 * <p>Free of Minecraft, so the rule can be asserted in an ordinary unit test. The caller is what
 * turns real neighbours into {@link Neighbour} values.
 */
public final class OffshorePumpSiting {

    /**
     * What one neighbouring block is, as far as siting is concerned. Deliberately three cases
     * rather than a boolean: {@link #FLOWING} is not merely "not a source" but the state ADR-0050's
     * deferred outlet block is allowed to create, and it must stay refused here for that block to
     * be a way of moving water rather than a way of making it.
     */
    public enum Neighbour {
        /** Still water: worldgen's, or a structure's. The only thing a pump may draw from. */
        SOURCE,
        /** Moving water: a dug channel, or the deferred outlet block. Never a valid site. */
        FLOWING,
        /** No water at all. */
        DRY
    }

    private OffshorePumpSiting() {
    }

    /** Whether a pump may stand against these neighbours. */
    public static boolean accepts(Collection<Neighbour> neighbours) {
        return neighbours.contains(Neighbour.SOURCE);
    }
}
