package io.ampznetwork.lunararc.common.server;

import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.scoreboard.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory ScoreboardManager. Returns Proxy-based Scoreboard/Objective/Score/Team
 * objects so that any interface method (regardless of Paper 1.21.1 build 133 API surface)
 * is handled gracefully without compile-time dependency on uncertain newer methods.
 */
public class LunarArcScoreboardManager implements ScoreboardManager {

    private static final LunarArcScoreboardManager INSTANCE = new LunarArcScoreboardManager();

    public static LunarArcScoreboardManager getInstance() {
        return INSTANCE;
    }

    private final Scoreboard mainScoreboard = newScoreboard();

    @Override
    public @NotNull Scoreboard getMainScoreboard() {
        return mainScoreboard;
    }

    @Override
    public @NotNull Scoreboard getNewScoreboard() {
        return newScoreboard();
    }

    // -------------------------------------------------------------------------
    // Scoreboard factory
    // -------------------------------------------------------------------------

    static Scoreboard newScoreboard() {
        ScoreboardState state = new ScoreboardState();
        return (Scoreboard) Proxy.newProxyInstance(
                Scoreboard.class.getClassLoader(),
                new Class<?>[]{Scoreboard.class},
                new ScoreboardHandler(state));
    }

    // -------------------------------------------------------------------------
    // Shared state for a single scoreboard instance
    // -------------------------------------------------------------------------

    static class ScoreboardState {
        final Map<String, Objective> objectives = new LinkedHashMap<>();
        final Map<String, Team> teams = new LinkedHashMap<>();
        // entry -> objective name -> score value
        final Map<String, Map<String, Integer>> scores = new ConcurrentHashMap<>();

        int getScore(String entry, String objective) {
            Map<String, Integer> m = scores.get(entry);
            return m != null ? m.getOrDefault(objective, 0) : 0;
        }

        boolean hasScore(String entry, String objective) {
            Map<String, Integer> m = scores.get(entry);
            return m != null && m.containsKey(objective);
        }

        void setScore(String entry, String objective, int value) {
            scores.computeIfAbsent(entry, k -> new ConcurrentHashMap<>()).put(objective, value);
        }
    }

    // -------------------------------------------------------------------------
    // InvocationHandler for Scoreboard
    // -------------------------------------------------------------------------

    static class ScoreboardHandler implements InvocationHandler {
        final ScoreboardState state;
        ScoreboardHandler(ScoreboardState state) { this.state = state; }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            switch (method.getName()) {
                case "registerNewObjective" -> {
                    String name = (String) args[0];
                    // args[1] is either String criteria or Criteria object
                    String criteriaStr = args[1] instanceof String ? (String) args[1] : args[1].toString();
                    String displayName = args.length > 2 && args[2] != null ? args[2].toString() : name;
                    Objective obj = newObjective(state, (Scoreboard) proxy, name, criteriaStr, displayName);
                    state.objectives.put(name, obj);
                    return obj;
                }
                case "getObjective" -> {
                    if (args[0] instanceof String) return state.objectives.get(args[0]);
                    // DisplaySlot argument
                    for (Objective obj : state.objectives.values()) {
                        try {
                            if (args[0].equals(obj.getDisplaySlot())) return obj;
                        } catch (Exception ignored) {}
                    }
                    return null;
                }
                case "getObjectivesByCriteria" -> {
                    String crit = args[0] instanceof String ? (String) args[0] : args[0].toString();
                    Set<Objective> result = new LinkedHashSet<>();
                    for (Objective obj : state.objectives.values())
                        if (crit.equals(obj.getCriteria())) result.add(obj);
                    return result;
                }
                case "getObjectives" -> { return new LinkedHashSet<>(state.objectives.values()); }
                case "getEntries" -> { return new LinkedHashSet<>(state.scores.keySet()); }
                case "getScores" -> {
                    String entry = args[0] instanceof String ? (String) args[0]
                            : ((OfflinePlayer) args[0]).getName();
                    Set<Score> result = new LinkedHashSet<>();
                    Map<String, Integer> entryScores = state.scores.get(entry);
                    if (entryScores != null) {
                        for (String objName : entryScores.keySet()) {
                            Objective obj = state.objectives.get(objName);
                            if (obj != null) result.add(newScore(state, obj, entry));
                        }
                    }
                    return result;
                }
                case "resetScores" -> {
                    String entry = args[0] instanceof String ? (String) args[0]
                            : ((OfflinePlayer) args[0]).getName();
                    state.scores.remove(entry);
                    return null;
                }
                case "getPlayerTeam" -> {
                    String pname = ((OfflinePlayer) args[0]).getName();
                    for (Team t : state.teams.values()) if (t.hasEntry(pname)) return t;
                    return null;
                }
                case "getEntryTeam" -> {
                    for (Team t : state.teams.values()) if (t.hasEntry((String) args[0])) return t;
                    return null;
                }
                case "getTeam" -> { return state.teams.get(args[0]); }
                case "getTeams" -> { return new LinkedHashSet<>(state.teams.values()); }
                case "registerNewTeam" -> {
                    String tname = (String) args[0];
                    if (state.teams.containsKey(tname))
                        throw new IllegalArgumentException("Team '" + tname + "' already exists");
                    Team team = newTeam(tname);
                    state.teams.put(tname, team);
                    return team;
                }
                case "clearSlot" -> { return null; }
                case "hashCode" -> { return System.identityHashCode(proxy); }
                case "equals" -> { return proxy == args[0]; }
                case "toString" -> { return "LunarArcScoreboard"; }
                default -> {
                    Class<?> r = method.getReturnType();
                    if (r == boolean.class) return false;
                    if (r == int.class || r == long.class) return 0;
                    if (r == Set.class || r == Collection.class) return Collections.emptySet();
                    if (r == List.class) return Collections.emptyList();
                    return null;
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Objective factory
    // -------------------------------------------------------------------------

    static Objective newObjective(ScoreboardState state, Scoreboard scoreboard,
            String name, String criteria, String displayName) {
        ObjectiveState os = new ObjectiveState(name, criteria, displayName);
        return (Objective) Proxy.newProxyInstance(
                Objective.class.getClassLoader(),
                new Class<?>[]{Objective.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getName" -> { return os.name; }
                        case "getDisplayName" -> { return os.displayName; }
                        case "setDisplayName" -> { os.displayName = args[0].toString(); return null; }
                        case "displayName" -> {
                            if (args == null || args.length == 0)
                                return net.kyori.adventure.text.Component.text(os.displayName);
                            os.displayName = args[0] != null
                                    ? net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                                              .legacySection().serialize(
                                                      (net.kyori.adventure.text.Component) args[0])
                                    : "";
                            return null;
                        }
                        case "getCriteria" -> { return os.criteria; }
                        case "getTrackedCriteria" -> {
                            return Proxy.newProxyInstance(
                                    Criteria.class.getClassLoader(),
                                    new Class<?>[]{ Criteria.class },
                                    (p2, m2, a2) -> {
                                        if ("getCriteriaName".equals(m2.getName())) return os.criteria;
                                        if (m2.getReturnType() == boolean.class) return false;
                                        if (m2.getReturnType() == int.class) return 0;
                                        return null;
                                    });
                        }
                        case "isModifiable" -> { return true; }
                        case "getScoreboard" -> { return scoreboard; }
                        case "unregister" -> {
                            state.objectives.remove(os.name);
                            os.registered = false;
                            return null;
                        }
                        case "setDisplaySlot" -> { os.displaySlot = (DisplaySlot) args[0]; return null; }
                        case "getDisplaySlot" -> { return os.displaySlot; }
                        case "setRenderType" -> { os.renderType = (RenderType) args[0]; return null; }
                        case "getRenderType" -> { return os.renderType; }
                        case "getScore" -> {
                            String entry = args[0] instanceof String ? (String) args[0]
                                    : ((OfflinePlayer) args[0]).getName();
                            return newScore(state, (Objective) proxy, entry);
                        }
                        case "getScoreFor" -> {
                            String entry = ((org.bukkit.entity.Entity) args[0]).getName();
                            return newScore(state, (Objective) proxy, entry);
                        }
                        case "hasScore" -> {
                            String entry = args[0] instanceof String ? (String) args[0]
                                    : ((OfflinePlayer) args[0]).getName();
                            return state.hasScore(entry, os.name);
                        }
                        case "hashCode" -> { return System.identityHashCode(proxy); }
                        case "equals" -> { return proxy == args[0]; }
                        case "toString" -> { return "LunarArcObjective[" + os.name + "]"; }
                        default -> {
                            Class<?> r = method.getReturnType();
                            if (r == boolean.class) return false;
                            if (r == int.class) return 0;
                            return null;
                        }
                    }
                });
    }

    static class ObjectiveState {
        String name, criteria, displayName;
        DisplaySlot displaySlot;
        RenderType renderType = RenderType.INTEGER;
        boolean registered = true;
        ObjectiveState(String name, String criteria, String displayName) {
            this.name = name; this.criteria = criteria; this.displayName = displayName;
        }
    }

    // -------------------------------------------------------------------------
    // Score factory
    // -------------------------------------------------------------------------

    static Score newScore(ScoreboardState state, Objective objective, String entry) {
        return (Score) Proxy.newProxyInstance(
                Score.class.getClassLoader(),
                new Class<?>[]{Score.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getPlayer" -> {
                            return org.bukkit.Bukkit.getOfflinePlayer(entry);
                        }
                        case "getEntry" -> { return entry; }
                        case "getObjective" -> { return objective; }
                        case "getScore" -> { return state.getScore(entry, objective.getName()); }
                        case "setScore" -> { state.setScore(entry, objective.getName(), (int) args[0]); return null; }
                        case "isScoreSet" -> { return state.hasScore(entry, objective.getName()); }
                        case "getScoreboard" -> { return null; }
                        case "hashCode" -> { return System.identityHashCode(proxy); }
                        case "equals" -> { return proxy == args[0]; }
                        case "toString" -> { return "LunarArcScore[" + entry + "," + objective.getName() + "]"; }
                        default -> {
                            Class<?> r = method.getReturnType();
                            if (r == boolean.class) return false;
                            if (r == int.class) return 0;
                            return null;
                        }
                    }
                });
    }

    // -------------------------------------------------------------------------
    // Team factory
    // -------------------------------------------------------------------------

    static Team newTeam(String name) {
        TeamState ts = new TeamState(name);
        return (Team) Proxy.newProxyInstance(
                Team.class.getClassLoader(),
                new Class<?>[]{Team.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getName" -> { return ts.name; }
                        case "getDisplayName" -> { return ts.displayName; }
                        case "setDisplayName" -> { ts.displayName = (String) args[0]; return null; }
                        case "displayName" -> {
                            if (args == null || args.length == 0)
                                return net.kyori.adventure.text.Component.text(ts.displayName);
                            ts.displayName = args[0] != null
                                    ? net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                                              .legacySection().serialize((net.kyori.adventure.text.Component) args[0])
                                    : ts.name;
                            return null;
                        }
                        case "getPrefix" -> { return ts.prefix; }
                        case "setPrefix" -> { ts.prefix = (String) args[0]; return null; }
                        case "prefix" -> {
                            if (args == null || args.length == 0)
                                return net.kyori.adventure.text.Component.text(ts.prefix);
                            ts.prefix = args[0] != null
                                    ? net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                                              .legacySection().serialize((net.kyori.adventure.text.Component) args[0])
                                    : "";
                            return null;
                        }
                        case "getSuffix" -> { return ts.suffix; }
                        case "setSuffix" -> { ts.suffix = (String) args[0]; return null; }
                        case "suffix" -> {
                            if (args == null || args.length == 0)
                                return net.kyori.adventure.text.Component.text(ts.suffix);
                            ts.suffix = args[0] != null
                                    ? net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                                              .legacySection().serialize((net.kyori.adventure.text.Component) args[0])
                                    : "";
                            return null;
                        }
                        case "allowFriendlyFire" -> { return ts.allowFriendlyFire; }
                        case "setAllowFriendlyFire" -> { ts.allowFriendlyFire = (boolean) args[0]; return null; }
                        case "canSeeFriendlyInvisibles" -> { return ts.canSeeFriendlyInvisibles; }
                        case "setCanSeeFriendlyInvisibles" -> { ts.canSeeFriendlyInvisibles = (boolean) args[0]; return null; }
                        case "getNameTagVisibility" -> { return ts.nameTagVisibility; }
                        case "setNameTagVisibility" -> { ts.nameTagVisibility = args[0]; return null; }
                        case "getCollisionRule" -> { return ts.collisionRule; }
                        case "setCollisionRule" -> { ts.collisionRule = args[0]; return null; }
                        case "getColor" -> { return ts.color; }
                        case "setColor" -> { ts.color = (org.bukkit.ChatColor) args[0]; return null; }
                        case "color" -> {
                            if (args == null || args.length == 0)
                                return net.kyori.adventure.text.format.NamedTextColor.WHITE;
                            return null;
                        }
                        case "getPlayers" -> { return Collections.emptySet(); }
                        case "getEntries" -> { return Collections.unmodifiableSet(ts.entries); }
                        case "getSize" -> { return ts.entries.size(); }
                        case "getScoreboard" -> { return null; }
                        case "addPlayer" -> { ts.entries.add(((OfflinePlayer) args[0]).getName()); return null; }
                        case "removePlayer" -> { ts.entries.remove(((OfflinePlayer) args[0]).getName()); return null; }
                        case "addEntry" -> { ts.entries.add((String) args[0]); return null; }
                        case "removeEntry" -> { ts.entries.remove((String) args[0]); return null; }
                        case "hasPlayer" -> { return ts.entries.contains(((OfflinePlayer) args[0]).getName()); }
                        case "hasEntry" -> { return ts.entries.contains((String) args[0]); }
                        case "unregister" -> { ts.registered = false; return null; }
                        case "hashCode" -> { return System.identityHashCode(proxy); }
                        case "equals" -> { return proxy == args[0]; }
                        case "toString" -> { return "LunarArcTeam[" + ts.name + "]"; }
                        default -> {
                            Class<?> r = method.getReturnType();
                            if (r == boolean.class) return false;
                            if (r == int.class) return 0;
                            if (r == Set.class) return Collections.emptySet();
                            return null;
                        }
                    }
                });
    }

    static class TeamState {
        String name, displayName, prefix = "", suffix = "";
        boolean allowFriendlyFire = true, canSeeFriendlyInvisibles = true, registered = true;
        // Stored as Object: NameTagVisibility is top-level in org.bukkit.scoreboard (not Team inner);
        // CollisionRule does not exist in Paper 1.21.1 build 133 API.
        Object nameTagVisibility = resolveTopLevelEnum("org.bukkit.scoreboard.NameTagVisibility", "ALWAYS");
        Object collisionRule = null;
        org.bukkit.ChatColor color = org.bukkit.ChatColor.RESET;
        final Set<String> entries = new LinkedHashSet<>();
        TeamState(String name) { this.name = name; this.displayName = name; }
    }

    private static Object resolveTopLevelEnum(String className, String constantName) {
        try {
            Class<?> cls = Class.forName(className, true,
                    LunarArcScoreboardManager.class.getClassLoader());
            return cls.getField(constantName).get(null);
        } catch (Exception ignored) {
            return null;
        }
    }
}
