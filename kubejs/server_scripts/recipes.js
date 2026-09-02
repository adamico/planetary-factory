// priority: 0
// ADR-0034's default-deny sweep, in one pass (#143).
//
// Not expand--contract per mod: this is a pre-release pack with no save-invalidation constraint
// and nothing to protect, so the sweep removes everything and `RECIPE_SURVIVORS` re-admits by
// name. The filter is the negation of the allowlist rather than a list of removals, because a
// removal list is a list against 111 jars that goes stale on every version bump -- ADR-0034
// rejected exactly that shape for the ADR and it is no better here.
//
// The priority header is load order, not the filename (ADR-0022): this file has to load AFTER
// recipe_survivors.js, which declares the allowlist at priority 10.
ServerEvents.recipes(event => {
  // A survivor names a surface; the namespace is applied here because every survivor is by
  // definition a recipe the pack authored. KubeJS ANDs the keys of one filter map and ORs the
  // list under `or`, so this reads: remove everything that is not one of ours on a named surface.
  var survivors = RECIPE_SURVIVORS.map(entry => ({ mod: 'planetaryfactory', type: entry.type }))

  event.remove({ not: { or: survivors } })

  console.info('[planetaryfactory] stock-recipe sweep: kept ' + survivors.length +
    ' surface(s) -- ' + RECIPE_SURVIVORS.map(entry => entry.surface).join(', ') +
    '; everything else removed (ADR-0034)')
})
