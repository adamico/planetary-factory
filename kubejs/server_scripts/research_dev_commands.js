// Pack-side research dev commands -- the replacement for `/researchd research unlock|remove`.
//
// Researchd's own commands enumerate and resolve through the vanilla datapack registry
// (`registryAccess().lookupOrThrow(RESEARCH_KEY)`), but every research in this pack is declared
// through KubeJS and those never land there -- they are merged into ReloadableRegistryManager's
// private `byName` map instead. So `/researchd research unlock <team> all` iterates an empty
// stream and returns silently, and the id form rejects our ids as unknown. See #77 and
// docs/upstream/researchd-commands-miss-kubejs-researches.md.
//
// This command reads the manager rather than the registry, which is the one map that has both
// halves in it, so it drives datapack- and KubeJS-declared researches alike. It is a dev
// affordance for running in-world checks -- gated at permission level 2, so it is inert for an
// ordinary player on a normal server. It acts on the caller's own team; the pack is single-team
// in practice, and a team argument would mean reimplementing Researchd's team argument type too.
//
//   /pf research list                 -- every research the manager knows, and whether it is done
//   /pf research grant <id> | all     -- complete it for the caller's team
//   /pf research revoke <id> | all    -- flip it back, firing the effect's onLock
//
// `list` is here because #77's real cost is not knowing what ids exist: the pack's researches are
// invisible to `/researchd`, so without it the only way to learn an id is to read this repo. `/pf`
// is a pack namespace rather than `/pfresearch`, so a second dev affordance has somewhere to go.
//
// Check: unchecked -- dev affordance, human on use (docs/testing/what-to-check.md). It computes
// nothing and registers nothing; it either drives the team state in-world or it does not, and the
// only way to learn which is to run it.
//
// Delete this file if Researchd ever fixes the upstream bug; nothing in the pack depends on it.
(function () {
  const ResearchdManagers = Java.loadClass(
    'com.portingdeadmods.researchd.utils.registries.ResearchdManagers'
  );
  const ResearchdRegistries = Java.loadClass('com.portingdeadmods.researchd.ResearchdRegistries');
  const ResearchTeamHelperServer = Java.loadClass(
    'com.portingdeadmods.researchd.utils.researches.ResearchTeamHelperServer'
  );
  const ResourceKey = Java.loadClass('net.minecraft.resources.ResourceKey');
  const ResourceLocation = Java.loadClass('net.minecraft.resources.ResourceLocation');
  const StringArgument = Java.loadClass('com.mojang.brigadier.arguments.StringArgumentType');
  const SharedSuggestionProvider = Java.loadClass('net.minecraft.commands.SharedSuggestionProvider');
  // `suggest` is overloaded on String[] and Iterable<String>, and a JS array matches both; the
  // overload is named explicitly rather than left to Rhino's resolution.
  const SUGGEST_STRINGS =
    'suggest(java.lang.String[],com.mojang.brigadier.suggestion.SuggestionsBuilder)';

  // Sorted, so `list` and the tab completions read the same way twice running.
  const knownResearchIds = (level) => {
    const ids = [];
    ResearchdManagers.getResearchesManager(level)
      .getByName()
      .keySet()
      .forEach((id) => ids.push(id.toString()));
    return ids.sort();
  };

  const researchKey = (id) =>
    ResourceKey.create(ResearchdRegistries.RESEARCH_KEY, ResourceLocation.parse(id));

  // Researchd's own commands resolve the player this way: the effects need a live Player for the
  // caller and nothing for anyone offline.
  const playerGetter = (player) => (uuid) => (uuid.equals(player.getUUID()) ? player : null);

  // The one shape both grant and revoke have: resolve the caller's team, resolve the ids, apply,
  // report. `apply` is handed the team, the key and the player-lookup the effects need.
  const applyToTargets = (source, ids, verb, apply) => {
    const player = source.getPlayerOrException();
    const team = ResearchTeamHelperServer.getTeamByMember(player);
    if (!team) {
      source.sendFailure(Text.red('You are not on a research team.'));
      return 0;
    }

    const known = knownResearchIds(source.getLevel());
    const targets = ids === null ? known : ids;
    const unknown = targets.filter((id) => known.indexOf(id) < 0);
    if (unknown.length > 0) {
      source.sendFailure(
        Text.red(`Unknown research '${unknown[0]}'. Run /pf research list to see what is declared.`)
      );
      return 0;
    }

    const getPlayer = playerGetter(player);
    targets.forEach((id) => apply(team, researchKey(id), getPlayer, source.getLevel()));
    team.refreshResearchStatus();

    source.sendSuccess(() => Text.green(`${verb} ${targets.length} research(es).`), true);
    return targets.length;
  };

  const grant = (source, ids) =>
    applyToTargets(source, ids, 'Granted', (team, key, getPlayer, level) => {
      // The same clock Researchd's own unlock command stamps completions with.
      const completionTime = level.getDayTime() * 50;
      team.setResearchCompleted(key, completionTime);
      // forced: the whole point is to reach a research whose parents are not done yet.
      team.onCompleteResearch(key, completionTime, true, getPlayer);
    });

  const revoke = (source, ids) =>
    applyToTargets(source, ids, 'Revoked', (team, key, getPlayer) =>
      team.onRemoveResearch(key, getPlayer)
    );

  const list = (source) => {
    const player = source.getPlayerOrException();
    const team = ResearchTeamHelperServer.getTeamByMember(player);
    if (!team) {
      source.sendFailure(Text.red('You are not on a research team.'));
      return 0;
    }

    const completed = team.getResearches();
    const ids = knownResearchIds(source.getLevel());
    source.sendSuccess(() => Text.gray(`${ids.length} research(es) declared:`), false);
    ids.forEach((id) => {
      const instance = completed.get(researchKey(id));
      const done = !!instance && instance.isResearched();
      source.sendSuccess(() => (done ? Text.green(`  [x] ${id}`) : Text.gray(`  [ ] ${id}`)), false);
    });
    return ids.length;
  };

  ServerEvents.commandRegistry((event) => {
    const { commands: Commands, arguments: Arguments } = event;

    // A fresh subtree per verb: Brigadier's builders are mutable, so handing one instance to both
    // `grant` and `revoke` would give them a single shared `executes`. `all` is a literal branch
    // rather than a magic id, which is both Brigadier's idiom and one less string to validate.
    const verbBranch = (name, run) =>
      Commands.literal(name)
        .then(Commands.literal('all').executes((context) => run(context.getSource(), null)))
        .then(
          Commands.argument('research', Arguments.STRING.create(event))
            .suggests((context, builder) =>
              SharedSuggestionProvider[SUGGEST_STRINGS](
                knownResearchIds(context.getSource().getLevel()),
                builder
              )
            )
            .executes((context) =>
              run(context.getSource(), [StringArgument.getString(context, 'research')])
            )
        );

    event.register(
      Commands.literal('pf')
        .requires((source) => source.hasPermission(2))
        .then(
          Commands.literal('research')
            .then(Commands.literal('list').executes((context) => list(context.getSource())))
            .then(verbBranch('grant', grant))
            .then(verbBranch('revoke', revoke))
        )
    );
  });
})();
