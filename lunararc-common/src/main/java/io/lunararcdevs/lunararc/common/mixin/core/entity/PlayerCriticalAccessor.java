package io.lunararcdevs.lunararc.common.mixin.core.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Vanilla keeps its critical-hit rule private; Paper's HumanEntity#canCriticalAttack exposes it. */
@Mixin(Player.class)
public interface PlayerCriticalAccessor {
    @Invoker("canCriticalAttack")
    boolean lunararc$canCriticalAttack(Entity target);
}
