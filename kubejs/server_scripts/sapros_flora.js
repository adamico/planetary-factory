// Picking Yumako.
//
// The two Sapros harvests are deliberately different gestures (#22). Jellynut comes out of a
// Jellystem's trunk, so it is a loot table on a block you break -- no script needed. Yumako is
// picked off a canopy that stays standing, which is not a thing breaking a block can express,
// so it is this: right-click a fruiting leaves block, take the fruit, leave the tree.
//
// The fruit pops as an entity rather than going into the clicker's inventory, so a Create
// Deployer -- which right-clicks as a fake player and has no inventory to receive it -- harvests
// on exactly the same path a hand does.
//
// Refruiting is the block's own random tick, in `kubejs/startup_scripts/blocks.js`.
BlockEvents.rightClicked('planetaryfactory:yumako_leaves', (event) => {
  if (event.block.properties.fruiting !== 'true') {
    return;
  }

  event.block.popItem(Item.of('planetaryfactory:yumako_fresh'));
  event.block.set('planetaryfactory:yumako_leaves', { fruiting: 'false' });
  event.cancel();
});
