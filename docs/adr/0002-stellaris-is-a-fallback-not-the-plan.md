---
status: accepted
---

# Stellaris is GCyR's fallback, not the space framework

The design document names Stellaris as the mod handling rocket launches, space stations and planet
generation. That is out of date: GCyR is the intended framework because it integrates with GregTech
directly, and Stellaris is held in reserve only for the case where building GCyR against GTCEu 7.x
(ADR-0001) proves impossible. If GCyR works, Stellaris is deleted.

## Consequences

`stellaris-1.21-neoforge-1.4.25.jar.disabled` stays disabled rather than being removed, so the
fallback remains one rename away. It was originally disabled on a misdiagnosis — it was present for
five of the twelve recorded crashes and absent for the other seven, all of which had the same
GregTech `AddonFinder` NPE, so it never caused anything. Note that it is built for `1.21`, not
`1.21.1`, which should be tested before relying on it. The design document should be corrected the
next time it is revised.
