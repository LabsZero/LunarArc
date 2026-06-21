package io.ampznetwork.lunararc.common.mixin.core.command;

import io.ampznetwork.lunararc.common.LunarArcPlatform;
import io.ampznetwork.lunararc.common.bridge.CommandSourceStackBridge;
import io.ampznetwork.lunararc.common.bridge.EntityBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_21_R1.CraftServer;
import org.bukkit.craftbukkit.v1_21_R1.entity.CraftPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(net.minecraft.commands.CommandSourceStack.class)
public abstract class CommandSourceStackMixin
        implements CommandSourceStackBridge,
                   io.papermc.paper.command.brigadier.CommandSourceStack {

    @Shadow public abstract net.minecraft.world.entity.Entity getEntity();
    @Shadow public abstract Vec3 getPosition();
    @Shadow public abstract net.minecraft.server.level.ServerLevel getLevel();

    @Override
    public CommandSender getSender() {
        net.minecraft.world.entity.Entity entity = getEntity();
        if (entity instanceof ServerPlayer sp) {
            org.bukkit.entity.Entity bukkit = ((EntityBridge) sp).lunararc$getBukkitEntity();
            if (bukkit instanceof CommandSender cs) return cs;
            return new CraftPlayer((CraftServer) org.bukkit.Bukkit.getServer(), sp);
        }
        return LunarArcPlatform.getServer().getConsoleSender();
    }

    @Override
    public org.bukkit.entity.Entity getExecutor() {
        net.minecraft.world.entity.Entity entity = getEntity();
        if (entity instanceof ServerPlayer sp) {
            return ((EntityBridge) sp).lunararc$getBukkitEntity();
        }
        return null;
    }

    @Override
    public org.bukkit.Location getLocation() {
        Vec3 pos = getPosition();
        try {
            String worldName = getLevel().dimension().location().getPath();
            org.bukkit.World world = org.bukkit.Bukkit.getWorld(worldName);
            return new org.bukkit.Location(world, pos.x, pos.y, pos.z);
        } catch (Throwable t) {
            return new org.bukkit.Location(null, pos.x, pos.y, pos.z);
        }
    }

    @Override
    public CommandSender lunararc$getBukkitSender() {
        return getSender();
    }
}
