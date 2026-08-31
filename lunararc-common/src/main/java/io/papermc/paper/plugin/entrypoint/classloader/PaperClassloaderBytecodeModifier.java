package io.papermc.paper.plugin.entrypoint.classloader;

import io.papermc.paper.plugin.configuration.PluginMeta;

/**
 * LunarArc note: ClassloaderBytecodeModifier.Provider looks this up via ServiceLoader
 * (net.kyori.adventure.util.Services.service(...).orElseThrow()) - with no implementation
 * registered anywhere, that Optional was empty and threw the moment any paper-plugin.yml
 * plugin's class loaded through PaperSimplePluginClassLoader.
 *
 * Real Paper's own implementation here delegates to its internal
 * io.papermc.paper.pluginremap.reflect.ReflectionRemapper - a narrow ASM rewrite-rule chain
 * that only rewrites specific reflection call sites (Class.forName/getDeclaredMethod/etc. on
 * known legacy symbol names), gated off entirely when the runtime isn't reobfuscated, with its
 * own disable switch. LunarArcRemapper is NOT that: it's a wholesale Spigot<->Mojang class-symbol
 * remapper built for legacy plugins on the classic (Spigot) path
 * (org.bukkit.craftbukkit.util.CraftMagicNumbers#processClass), where the plugin's *compiled
 * bytecode* directly references old obfuscated symbol names. A modern paper-plugin.yml plugin is
 * written against Mojang-mapped Paper API already - running LunarArcRemapper's full remap over
 * it would risk rewriting already-correct symbol references into wrong ones, not help it. Until
 * a real equivalent to Paper's narrow reflection-call rewriter exists, a no-op passthrough is the
 * correct, safe choice here - this matches real Paper's own original implementation for this
 * exact class before it added that feature ("Stub, implement in future.").
 */
public class PaperClassloaderBytecodeModifier implements ClassloaderBytecodeModifier {

    @Override
    public byte[] modify(PluginMeta configuration, byte[] bytecode) {
        return bytecode;
    }
}
