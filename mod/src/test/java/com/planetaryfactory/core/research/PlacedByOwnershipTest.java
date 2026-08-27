package com.planetaryfactory.core.research;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Issue #74, both callers of the rule: "A machine already carrying an attachment is not re-stamped
 * to the placing player", and telling a real filter frame from the one Researchd pushes for a
 * machine that has no owner.
 */
class PlacedByOwnershipTest {

    @Test
    void aMachineCarryingATeamKeepsIt() {
        assertTrue(PlacedByOwnership.isOwned(UUID.fromString("6c9b4a1e-0000-4000-8000-000000000001")));
    }

    @Test
    void aMachineCarryingNothingIsStamped() {
        assertFalse(PlacedByOwnership.isOwned(null));
    }

    @Test
    void theEmptyUuidIsNotAnOwner() {
        assertFalse(
                PlacedByOwnership.isOwned(PlacedByOwnership.NO_OWNER),
                "Researchd writes this when a placer has no team, and reads it back as unowned");
    }

    /**
     * The regression behind the in-world check failing on its first run. Researchd pushes a filter
     * frame for a {@code /setblock} machine rather than pushing none, because the attachment's
     * default is the zero UUID rather than null -- so a wrapper testing only for a null frame never
     * sees the bypass it exists to report.
     */
    @Test
    void aFrameCarryingTheEmptyTeamIsNotAnOwnedMachine() {
        assertFalse(PlacedByOwnership.isOwned(PlacedByOwnership.NO_OWNER));
    }

    @Test
    void theEmptyUuidIsRecognisedHoweverItWasBuilt() {
        assertFalse(PlacedByOwnership.isOwned(UUID.fromString("00000000-0000-0000-0000-000000000000")));
    }
}
