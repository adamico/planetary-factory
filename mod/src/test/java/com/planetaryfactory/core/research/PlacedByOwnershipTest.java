package com.planetaryfactory.core.research;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Issue #74: "A machine already carrying an attachment is not re-stamped to the placing player."
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

    @Test
    void theEmptyUuidIsRecognisedHoweverItWasBuilt() {
        assertFalse(PlacedByOwnership.isOwned(UUID.fromString("00000000-0000-0000-0000-000000000000")));
    }
}
