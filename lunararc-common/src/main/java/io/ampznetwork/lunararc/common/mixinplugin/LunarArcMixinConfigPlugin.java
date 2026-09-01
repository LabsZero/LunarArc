package io.ampznetwork.lunararc.common.mixinplugin;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Adds the {@code static} members CraftBukkit declares on Minecraft classes.
 *
 * <p>CraftBukkit adds these by patching the class outright. LunarArc cannot: the Minecraft runtime
 * belongs to NeoForge, Forge, Fabric or Quilt. For instance members a Mixin is enough - that is how
 * {@code Entity#getBukkitEntity()} is supplied - but Mixin refuses to merge a non-private static
 * method from a mixin class, and rightly so, since a static cannot be part of an implemented
 * interface. Declaring one anyway is not a degraded feature but a dead server: Mixin throws
 * InvalidMixinException during the apply phase, which on NeoForge kills the JVM inside
 * ServerModLoader.load before the server object exists.</p>
 *
 * <p>This class sits outside the configured mixin package on purpose. Classes under that
 * package are consumed by the mixin transformer rather than loaded normally, while a config
 * plugin is instantiated as an ordinary class before any mixin is applied.</p>
 *
 * <p>{@link #postApply} is the supported way through. Mixin hands the fully-applied target
 * {@link ClassNode} over for inspection, and a method added there is part of the class the JVM
 * ultimately defines - no injector, no visibility rule, and no dependence on any one loader's
 * class layout, so the same code serves all four.</p>
 *
 * <p>The members supplied here are the ones plugins reach for by name through reflection, where a
 * bytecode rewrite cannot help. {@code MinecraftServer.getServer()} is the standing example:
 * ViaVersion calls {@code ReflectionUtil.invokeStatic(MinecraftServer.class, "getServer")} and, not
 * finding it, gives up on locating the server connection - "Failed to check if ViaVersion is
 * binded" - and its Bukkit injector never installs.</p>
 */
public final class LunarArcMixinConfigPlugin implements IMixinConfigPlugin {

    private static final String MINECRAFT_SERVER = "net/minecraft/server/MinecraftServer";
    private static final String SERVER_ACCESS =
            "io/ampznetwork/lunararc/common/LunarArcServerAccess";
    private static final String GET_SERVER_DESC = "()L" + MINECRAFT_SERVER + ";";

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // Nothing here may throw. This runs inside mixin application, where an escaping exception
        // is not a failed feature but a server that never starts.
        try {
            if (!MINECRAFT_SERVER.equals(targetClass.name)) return;
            addStaticGetServer(targetClass);
        } catch (Throwable ignored) {
            // A missing accessor is a plugin-compatibility gap; a thrown one is a dead server.
        }
    }

    /**
     * Declares {@code public static MinecraftServer getServer()}, returning LunarArc's attached
     * server instance - the same thing CraftBukkit's own static returns.
     */
    private static void addStaticGetServer(ClassNode targetClass) {
        // MinecraftServer carries several mixins, so postApply runs more than once for it.
        for (MethodNode existing : targetClass.methods) {
            if ("getServer".equals(existing.name) && GET_SERVER_DESC.equals(existing.desc)) return;
        }

        MethodNode getServer = new MethodNode(
                Opcodes.ASM9,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "getServer",
                GET_SERVER_DESC,
                null,
                null);
        getServer.instructions.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC, SERVER_ACCESS, "getMinecraftServer", GET_SERVER_DESC, false));
        getServer.instructions.add(new InsnNode(Opcodes.ARETURN));
        getServer.maxStack = 1;
        getServer.maxLocals = 0;
        targetClass.methods.add(getServer);
    }

}
