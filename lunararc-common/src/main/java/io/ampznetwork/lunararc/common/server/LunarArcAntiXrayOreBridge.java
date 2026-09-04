package io.ampznetwork.lunararc.common.server;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.CraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Keeps {@code config/paper-world-defaults.yml}'s anti-xray {@code hidden-blocks} list current
 * with modded ores.
 *
 * <p>{@code CraftServer#loadConfigurations()} seeds that list with real Paper's own vanilla-only
 * defaults (verified against {@code patches/server/0005-Paper-config-files.patch}) before this
 * runs. This is a second, separate pass over the file it already wrote, adding any block tagged
 * {@code c:ores} - NeoForge's common convention tag every ore-adjacent mod block is expected to
 * carry - whose namespace is not {@code minecraft} and whose id is not already in the list. It
 * only ever adds; a server owner's own edits to the list, or a removal of a vanilla entry, are
 * left alone. Nothing is written back when there is nothing new to add, so a server with no
 * newly-installed ore mods does not churn this file on every boot.</p>
 *
 * <p>This must run after tags are bound (after world load, alongside
 * {@link LunarArcEssentialsItemBridge}) - {@code c:ores} membership does not resolve yet at
 * {@code CraftServer} construction time, when {@code loadConfigurations()} itself runs.</p>
 *
 * <p>Populating this list is necessary but not sufficient for anti-xray to actually do anything:
 * LunarArc does not yet implement the chunk-obfuscation engine that reads it (real Paper's
 * {@code AntiXray}/{@code ChunkPacketBlockController}). Until that exists, this only keeps the
 * config's ore list accurate for whenever it does.</p>
 */
public final class LunarArcAntiXrayOreBridge {

    private static final Logger LOGGER = LoggerFactory.getLogger(LunarArcAntiXrayOreBridge.class);
    private static final String HIDDEN_BLOCKS_PATH = "anticheat.anti-xray.hidden-blocks";
    private static final TagKey<Block> COMMON_ORES =
            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "ores"));

    private LunarArcAntiXrayOreBridge() {}

    public static void mergeModdedOres(CraftServer craftServer) {
        File file = new File("config", "paper-world-defaults.yml");
        if (!file.isFile()) return;

        YamlConfiguration config = new YamlConfiguration();
        try {
            config.load(file);
        } catch (Exception ex) {
            LOGGER.warn("Unable to read {} to merge modded ores into anti-xray's hidden-blocks list", file.getPath(), ex);
            return;
        }

        List<String> current = config.getStringList(HIDDEN_BLOCKS_PATH);
        LinkedHashSet<String> merged = new LinkedHashSet<>(current);
        int before = merged.size();

        for (ResourceLocation id : BuiltInRegistries.BLOCK.keySet()) {
            if (id.getNamespace().equals("minecraft")) continue; // vanilla ores are already seeded
            Block block = BuiltInRegistries.BLOCK.get(id);
            if (block == null || !block.builtInRegistryHolder().is(COMMON_ORES)) continue;
            merged.add(id.toString());
        }

        int added = merged.size() - before;
        if (added == 0) return;

        config.set(HIDDEN_BLOCKS_PATH, new ArrayList<>(merged));
        try {
            config.save(file);
            LOGGER.info("Added {} modded ore block(s) to anti-xray's hidden-blocks list in {}", added, file.getPath());
        } catch (Exception ex) {
            LOGGER.warn("Unable to save {} after merging modded ores", file.getPath(), ex);
        }
    }
}
