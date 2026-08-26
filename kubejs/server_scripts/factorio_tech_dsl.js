// Builds Researchd researches from Factorio's extracted technology tree.
//
// The tree's *shape* is imported rather than invented (ADR-0022): Factorio has playtested
// the order in which a factory learns things, and that ordering is worth taking. What is
// not worth taking is its content, so this file supplies the topology and the costs and
// leaves every Minecraft decision -- which item is the icon, which recipe is unlocked,
// which of our bodies gates it -- to the fromFactorio() calls in researchd.js.
//
// Every script of a type is evaluated against ONE shared topLevelScope, so a top-level
// `var` or `function` here is visible to every file that loads after it. KubeJS's `global`
// binding is NOT the way to do this -- it is an unmodifiable Java map, so writes fail and
// reads come back undefined.
//
// Load order matters and is alphabetical within server_scripts:
//   factorio_tech_data.js  (generated, defines FACTORIO_TECHS)
//   factorio_tech_dsl.js   (this file, defines fromFactorio)
//   researchd.js           (the hand-authored declarations)
// The flush below runs inside registerResearches, which fires long after all three load.

var PF_TECH_OVERRIDES = {};

/**
 * Declare that a Factorio technology exists in this pack.
 *
 * @param name  the Factorio technology name, kebab-case, exactly as in technology.json
 * @param over  the Minecraft-specific parts only:
 *   icon              item id for the research icon      (or iconPack, for a science pack)
 *   iconPack          research pack id, for pack researches
 *   unlocks           array of recipe ids the research grants
 *   unlocksDimensions array of dimension ids the research opens
 *   name              override the localised name from the data
 *   has               ['item id', count] -- the pack's form of a Factorio research trigger
 *   method            a raw ResearchMethod, for anything `has` cannot express
 *   gatedBy           extra parent research ids -- this is where OUR planet gating goes
 *   costScale         multiplier on Factorio's pack counts, default 1
 *   skip              true to declare, visibly, that this technology is deliberately absent
 *
 * `has` is checkItemPresence, not consumeItem, and the pack uses consumeItem nowhere.
 * Factorio's research triggers do not destroy what they fire on -- craft fifty iron plates
 * and you keep the plates -- so a presence check is the faithful mapping. It is also
 * monotonic: the progress latches the first time the items are held together and does not
 * regress when they are spent. Real cost belongs to the science-pack rungs, which are the
 * ones the Research Lab can be piped.
 */
function fromFactorio(name, over) {
  PF_TECH_OVERRIDES[name] = over || {};
}

function pfId(name) {
  return 'planetary_factory:' + name.replace(/-/g, '_');
}

ResearchdEvents.registerResearches((event) => {
  const techs = typeof FACTORIO_TECHS === 'undefined' ? [] : FACTORIO_TECHS;
  const overrides = PF_TECH_OVERRIDES;

  const byName = {};
  techs.forEach((t) => {
    byName[t.name] = t;
  });

  // A technology we did not declare is transparent, not terminal: dropping `automation`
  // must not orphan everything downstream of it. Resolving through to the nearest declared
  // ancestors is what keeps a partially-authored tree connected -- which matters most
  // early, when most of the 162 are still undeclared.
  const declared = (n) => {
    const o = overrides[n];
    return o && !o.skip;
  };

  const resolveParents = (tech, seen) => {
    const out = [];
    (tech.prerequisites || []).forEach((parent) => {
      if (seen[parent]) return;
      seen[parent] = true;
      if (declared(parent)) {
        out.push(pfId(parent));
      } else if (byName[parent]) {
        resolveParents(byName[parent], seen).forEach((id) => out.push(id));
      }
    });
    return out;
  };

  const missing = [];

  techs.forEach((tech) => {
    const over = overrides[tech.name];
    if (!over || over.skip) {
      if (!over) missing.push(tech.name);
      return;
    }

    const research = event.create(pfId(tech.name));

    if (over.iconPack) {
      research.iconPack(over.iconPack);
    } else if (over.icon) {
      research.icon(over.icon);
    }

    research.literalName(over.name || tech.localised_name);

    const parents = resolveParents(tech, {}).concat(over.gatedBy || []);
    if (parents.length) research.parents(parents);

    if (over.has) {
      // Space Age's research_trigger has no pack cost at all -- crafting an item, mining
      // an entity, capturing a spawner. checkItemPresence is the same idea: hold the thing,
      // and the research fires without taking it.
      research.method(ResearchMethodHelper.checkItemPresence(over.has[0], over.has[1]));
    } else if (over.method) {
      research.method(over.method);
    } else if (tech.cost_kind === 'packs' && tech.unit) {
      // consumePacks(), not consumePack() in a loop: the builder holds ONE researchMethod,
      // and consumePack() delegates straight to method(), so a second call overwrites the
      // first. 117 of the 130 pack-costed technologies need more than one pack, so looping
      // would silently cost most of the tree a single pack -- the last one listed.
      //
      // Every ingredient amount in Factorio's tree is 1, so the technology's `count` is the
      // per-pack count and consumePacks' shape maps exactly. If that ever stops being true
      // the extractor's data will say so, and this needs and(...) composition instead.
      const scale = over.costScale || 1;
      const packs = (tech.unit.ingredients || []).map((pair) => pfId(pair[0]));
      if (packs.length) {
        research.consumePacks(packs, Math.ceil(tech.unit.count * scale), tech.unit.time);
      }
    }

    // One effect object, always: researchEffect is a single field, so calling effect()
    // twice keeps only the second. Recipes collapse into one unlockRecipes(), dimensions
    // into one unlockDimensions(), and the two are joined with and().
    const effects = [];
    if ((over.unlocks || []).length) {
      effects.push(ResearchEffectHelper.unlockRecipes(over.unlocks));
    }
    if ((over.unlocksDimensions || []).length) {
      // Our planet gating is not Factorio's (ADR-0022), so this is always hand-authored --
      // nothing in the extracted data implies it.
      effects.push(ResearchEffectHelper.unlockDimensions(over.unlocksDimensions));
    }
    if (effects.length === 1) {
      research.effect(effects[0]);
    } else if (effects.length > 1) {
      research.effect(ResearchEffectHelper.and(effects));
    }
  });

  // "Which have I not done yet" is the question every authoring session opens with, and
  // the script already knows the answer.
  if (missing.length) {
    console.warn(
      '[planetary_factory] ' +
        missing.length +
        ' of ' +
        techs.length +
        ' Factorio technologies are not yet declared in researchd.js:'
    );
    console.warn('  ' + missing.join(', '));
  }
});
