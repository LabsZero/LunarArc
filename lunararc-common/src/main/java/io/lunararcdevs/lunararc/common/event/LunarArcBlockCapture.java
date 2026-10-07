package io.lunararcdevs.lunararc.common.event;

import io.lunararcdevs.lunararc.common.LunarArcServerAccess;
import io.lunararcdevs.lunararc.common.bridge.EntityBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.TreeType;
import org.bukkit.craftbukkit.block.CraftBlockState;
import org.bukkit.craftbukkit.block.CraftBlockStates;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.world.StructureGrowEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

public final class LunarArcBlockCapture {
    private LunarArcBlockCapture() {}

    private static final class Capture {
        private final ServerLevel level;
        private final Map<BlockPos, CraftBlockState> states = new LinkedHashMap<>();
        private ResourceKey<ConfiguredFeature<?, ?>> feature;

        private Capture(ServerLevel level) {
            this.level = level;
        }
    }

    private static final ThreadLocal<Capture> ACTIVE = new ThreadLocal<>();
    private static final ThreadLocal<Player> BONE_MEAL = new ThreadLocal<>();
    private static int active;

    public static boolean intercept(Level level, BlockPos pos, BlockState state, int flags) {
        if (active == 0) return false;
        Capture capture = ACTIVE.get();
        if (capture == null || capture.level != level) return false;
        CraftBlockState snapshot = capture.states.get(pos);
        if (snapshot == null) {
            snapshot = CraftBlockStates.getBlockState(level, pos, flags);
            capture.states.put(pos.immutable(), snapshot);
        }
        snapshot.setData(state);
        return true;
    }

    public static BlockState read(Level level, BlockPos pos) {
        if (active == 0) return null;
        Capture capture = ACTIVE.get();
        if (capture == null || capture.level != level) return null;
        CraftBlockState snapshot = capture.states.get(pos);
        return snapshot == null ? null : snapshot.getHandle();
    }

    public static boolean fertilize(Level level, BlockPos pos, net.minecraft.world.item.ItemStack stack, BooleanSupplier action) {
        if (BlockFertilizeEvent.getHandlerList().getRegisteredListeners().length == 0 || !(level instanceof ServerLevel serverLevel)
                || ACTIVE.get() != null || !Bukkit.isPrimaryThread()) {
            return action.getAsBoolean();
        }
        Capture capture = new Capture(serverLevel);
        ACTIVE.set(capture);
        active++;
        int count = stack.getCount();
        boolean result;
        try {
            result = action.getAsBoolean();
        } finally {
            ACTIVE.remove();
            active--;
        }
        if (capture.states.isEmpty()) return result;

        Player nmsPlayer = BONE_MEAL.get();
        org.bukkit.entity.Player player = nmsPlayer != null
                && ((EntityBridge) nmsPlayer).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Player bukkit ? bukkit : null;
        BlockFertilizeEvent event = new BlockFertilizeEvent(org.bukkit.craftbukkit.block.CraftBlock.at(serverLevel, pos), player,
                new ArrayList<>(capture.states.values()));
        LunarArcServerAccess.getCraftServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            stack.setCount(count);
            return false;
        }
        for (org.bukkit.block.BlockState state : event.getBlocks()) state.update(true);
        return result;
    }

    public static void noteFeature(ResourceKey<ConfiguredFeature<?, ?>> feature) {
        Capture capture = ACTIVE.get();
        if (capture != null && feature != null) capture.feature = feature;
    }

    public static void boneMeal(Player player) {
        if (player == null) BONE_MEAL.remove();
        else BONE_MEAL.set(player);
    }

    public static boolean growTree(ServerLevel level, BlockPos pos, BooleanSupplier growth) {
        if (StructureGrowEvent.getHandlerList().getRegisteredListeners().length == 0
                || ACTIVE.get() != null || !Bukkit.isPrimaryThread()) {
            return growth.getAsBoolean();
        }
        Capture capture = new Capture(level);
        ACTIVE.set(capture);
        active++;
        boolean result;
        try {
            result = growth.getAsBoolean();
        } finally {
            ACTIVE.remove();
            active--;
        }
        if (capture.states.isEmpty()) return result;

        org.bukkit.craftbukkit.CraftWorld world = LunarArcServerAccess.getCraftWorld(level);
        Player nmsPlayer = BONE_MEAL.get();
        org.bukkit.entity.Player player = nmsPlayer != null
                && ((EntityBridge) nmsPlayer).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Player bukkit ? bukkit : null;
        List<org.bukkit.block.BlockState> blocks = new ArrayList<>(capture.states.values());
        StructureGrowEvent event = new StructureGrowEvent(new Location(world, pos.getX(), pos.getY(), pos.getZ()),
                treeType(capture.feature), nmsPlayer != null, player, blocks);
        LunarArcServerAccess.getCraftServer().getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            for (org.bukkit.block.BlockState state : event.getBlocks()) state.update(true);
        }
        return result;
    }

    private static TreeType treeType(ResourceKey<ConfiguredFeature<?, ?>> feature) {
        if (feature == null) return TreeType.TREE;
        String path = feature.location().getPath();
        int bees = path.indexOf("_bees");
        if (bees >= 0) path = path.substring(0, bees);
        return switch (path) {
            case "fancy_oak" -> TreeType.BIG_TREE;
            case "birch" -> TreeType.BIRCH;
            case "super_birch" -> TreeType.TALL_BIRCH;
            case "spruce" -> TreeType.REDWOOD;
            case "pine" -> TreeType.TALL_REDWOOD;
            case "mega_spruce", "mega_pine" -> TreeType.MEGA_REDWOOD;
            case "jungle_tree" -> TreeType.COCOA_TREE;
            case "jungle_tree_no_vine" -> TreeType.SMALL_JUNGLE;
            case "mega_jungle_tree" -> TreeType.JUNGLE;
            case "jungle_bush" -> TreeType.JUNGLE_BUSH;
            case "acacia" -> TreeType.ACACIA;
            case "dark_oak" -> TreeType.DARK_OAK;
            case "swamp_oak" -> TreeType.SWAMP;
            case "azalea_tree" -> TreeType.AZALEA;
            case "mangrove" -> TreeType.MANGROVE;
            case "tall_mangrove" -> TreeType.TALL_MANGROVE;
            case "cherry" -> TreeType.CHERRY;
            default -> TreeType.TREE;
        };
    }
}
