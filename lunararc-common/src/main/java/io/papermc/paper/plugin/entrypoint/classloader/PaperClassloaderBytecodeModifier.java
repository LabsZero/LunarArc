package io.papermc.paper.plugin.entrypoint.classloader;

import io.papermc.paper.plugin.configuration.PluginMeta;

/**
 * LunarArc note: ClassloaderBytecodeModifier.Provider looks this up via ServiceLoader
 * (net.kyori.adventure.util.Services.service(...).orElseThrow()) - with no implementation
 * registered anywhere, that Optional was empty and threw the moment any paper-plugin.yml
 * plugin's class loaded through PaperSimplePluginClassLoader. Real Paper's own implementation
 * here just delegates to its internal io.papermc.paper.pluginremap.reflect.ReflectionRemapper,
 * which LunarArc doesn't have; LunarArcRemapper is LunarArc's own equivalent, already used for
 * exactly this purpose on the classic (Spigot) plugin path
 * (org.bukkit.craftbukkit.util.CraftMagicNumbers#processClass), so route the modern (Paper)
 * plugin path through the same remapper instead of introducing a second implementation.
 */
public class PaperClassloaderBytecodeModifier implements ClassloaderBytecodeModifier {

    @Override
    public byte[] modify(PluginMeta configuration, byte[] bytecode) {
        return new io.ampznetwork.lunararc.common.mod.LunarArcRemapper(true).transform(bytecode);
    }
}
