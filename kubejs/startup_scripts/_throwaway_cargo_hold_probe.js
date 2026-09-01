// THROWAWAY -- delete this file once #133 is answered.
//
// Not pack content. This is the smallest block that claims all four properties the wreck's
// cargo hold needs (#133), built so a single world can falsify the one that is in doubt:
// whether items put in through the right-click screen survive a save and reload.
//
//   1. Unbreakable      -- hardness(-1), the same knob `blocks.js` already turns.
//   2. Automatable      -- `inventory(...)` attaches an InventoryAttachment, which reports
//                          ITEM_HANDLER on the listed faces, so hoppers, Create funnels and
//                          GT pipes all see it. An EMPTY direction set means EVERY face:
//                          `attach` branches on `Set.isEmpty()` and the capability provider
//                          short-circuits on an empty EnumSet, answering whatever the side.
//                          It must be `[]`, not `null` -- Rhino coerces the argument through
//                          `Set.of(...)`, which NPEs on null before `attach` is ever entered.
//   3. Persistent       -- InventoryAttachment has serialize/deserialize, so KubeBlockEntity
//                          writes it into the chunk. Claimed by the jar, unverified in a world.
//   4. Player-openable  -- `rightClickOpensInventory('cargo')`, a first-class BlockEntityInfo
//                          method that looks the NAMED attachment up in the block entity's
//                          own `attachments` map and opens that object. Same object that
//                          serializes, so the screen cannot be a discarding scratch view.
//                          CustomChestMenu is not involved and is not the mechanism here.
//                          Note it writes `BlockBuilder.rightClick`, so a block using this
//                          cannot also carry a custom right-click callback.
//
// Nine slots in a 3x3 so the screen is unmistakably a chest view rather than a hopper's row.
StartupEvents.registry('block', (event) => {
  event.create('planetaryfactory:cargo_hold_probe')
    .displayName('Cargo Hold Probe')
    .texture('gcyr:block/mars_regolith')
    .soundType('metal')
    .hardness(-1)
    .resistance(3600000)
    .requiresTool(false)
    .blockEntity((be) => {
      be.inventory('cargo', [], 3, 3);
      be.rightClickOpensInventory('cargo');
    });
});
