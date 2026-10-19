package io.lunararcdevs.lunararc.common.mixin.core.events;

import com.destroystokyo.paper.event.entity.TurtleLayEggEvent;
import com.destroystokyo.paper.event.entity.TurtleStartDiggingEvent;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.lunararcdevs.lunararc.common.bridge.EntityBridge;
import io.lunararcdevs.lunararc.common.bridge.LevelBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.turtle.Turtle;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TurtleEggBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Location;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** Paper's Turtle API events around egg laying. */
@Mixin(targets = "net.minecraft.world.entity.animal.turtle.Turtle$TurtleLayEggGoal", remap = false)
public abstract class TurtleEggEventsMixin {
    @WrapOperation(method = "tick", remap = true, at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/world/entity/animal/turtle/Turtle;setLayingEgg(Z)V"))
    private void lunararc$startDigging(Turtle turtle, boolean laying, Operation<Void> original) {
        if (laying) {
            Location where = new Location(((LevelBridge) turtle.level()).lunararc$getWorld(), turtle.getX(), turtle.getY(), turtle.getZ());
            laying = new TurtleStartDiggingEvent((org.bukkit.entity.Turtle) ((EntityBridge) turtle).lunararc$getBukkitEntity(), where).callEvent();
        }
        original.call(turtle, laying);
    }

    @WrapOperation(method = "tick", remap = true, at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean lunararc$layEgg(Level level, BlockPos pos, BlockState state, Operation<Boolean> original) {
        Turtle turtle = lunararc$turtle();
        Location where = new Location(((LevelBridge) level).lunararc$getWorld(), pos.getX(), pos.getY(), pos.getZ());
        TurtleLayEggEvent event = new TurtleLayEggEvent((org.bukkit.entity.Turtle) ((EntityBridge) turtle).lunararc$getBukkitEntity(), where,
                state.getValue(TurtleEggBlock.EGGS));
        if (!event.callEvent()) return false;
        BlockState adjusted = state.setValue(TurtleEggBlock.EGGS, event.getEggCount());
        if (!CraftEventFactory.callEntityChangeBlockEvent(turtle, pos, adjusted)) return false;
        return original.call(level, pos, adjusted);
    }

    @Shadow(remap = false) @org.spongepowered.asm.mixin.Final private Turtle turtle;

    @org.spongepowered.asm.mixin.Unique
    private Turtle lunararc$turtle() {
        return this.turtle;
    }
}
