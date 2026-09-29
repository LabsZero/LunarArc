package io.lunararcdevs.lunararc.common.mixin.core.projectile;

import io.lunararcdevs.lunararc.common.bridge.FishingHookBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.Level;
import net.minecraft.tags.FluidTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FishingHook.class)
public abstract class FishingHookMixin implements FishingHookBridge {
    @Shadow private int outOfWaterTime;
    @Shadow private int timeUntilLured;
    @Shadow private int timeUntilHooked;
    @Shadow private float fishAngle;
    @Shadow private Entity hookedIn;
    @Shadow @Final private int lureSpeed;

    @Unique private int lunararc$minWaitTime = 100;
    @Unique private int lunararc$maxWaitTime = 600;
    @Unique private int lunararc$minLureTime = 20;
    @Unique private int lunararc$maxLureTime = 80;
    @Unique private float lunararc$minLureAngle = 0.0F;
    @Unique private float lunararc$maxLureAngle = 360.0F;
    @Unique private boolean lunararc$applyLure = true;
    @Unique private boolean lunararc$rainInfluenced = true;
    @Unique private boolean lunararc$skyInfluenced = true;
    @Unique private double lunararc$biteChance = -1.0D;

    @Invoker("setHookedEntity") protected abstract void lunararc$invokeSetHookedEntity(Entity entity);
    @Invoker("calculateOpenWater") protected abstract boolean lunararc$invokeCalculateOpenWater(BlockPos pos);
    @Invoker("pullEntity") protected abstract void lunararc$invokePullEntity(Entity entity);

    @Redirect(method="catchingFish", at=@At(value="INVOKE", target="Lnet/minecraft/world/level/Level;isRainingAt(Lnet/minecraft/core/BlockPos;)Z"), require=0)
    private boolean lunararc$rainInfluence(Level level, BlockPos pos) { return this.lunararc$rainInfluenced && level.isRainingAt(pos); }

    @Redirect(method="catchingFish", at=@At(value="INVOKE", target="Lnet/minecraft/world/level/Level;canSeeSky(Lnet/minecraft/core/BlockPos;)Z"), require=0)
    private boolean lunararc$skyInfluence(Level level, BlockPos pos) { return !this.lunararc$skyInfluenced || level.canSeeSky(pos); }

    @Redirect(method="catchingFish", at=@At(value="INVOKE", target="Lnet/minecraft/util/Mth;nextInt(Lnet/minecraft/util/RandomSource;II)I", ordinal=1), require=0)
    private int lunararc$lureRange(RandomSource random, int min, int max) { return Mth.nextInt(random, this.lunararc$minLureTime, this.lunararc$maxLureTime); }

    @Redirect(method="catchingFish", at=@At(value="INVOKE", target="Lnet/minecraft/util/Mth;nextInt(Lnet/minecraft/util/RandomSource;II)I", ordinal=2), require=0)
    private int lunararc$waitRange(RandomSource random, int min, int max) {
        int value = Mth.nextInt(random, this.lunararc$minWaitTime, this.lunararc$maxWaitTime);
        return this.lunararc$applyLure ? value : value + this.lureSpeed;
    }

    @Redirect(method="catchingFish", at=@At(value="INVOKE", target="Lnet/minecraft/util/Mth;nextFloat(Lnet/minecraft/util/RandomSource;FF)F", ordinal=2), require=0)
    private float lunararc$lureAngle(RandomSource random, float min, float max) { return Mth.nextFloat(random, this.lunararc$minLureAngle, this.lunararc$maxLureAngle); }

    @Unique private int lunararc$fishExp;

    @Unique
    private org.bukkit.event.player.PlayerFishEvent lunararc$fireFish(net.minecraft.world.entity.player.Player player, Entity caught,
            net.minecraft.world.item.ItemStack rod, org.bukkit.event.player.PlayerFishEvent.State state, int exp) {
        if (!(((io.lunararcdevs.lunararc.common.bridge.EntityBridge) player).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Player bukkitPlayer)
                || !(((io.lunararcdevs.lunararc.common.bridge.EntityBridge) this).lunararc$getBukkitEntity() instanceof org.bukkit.entity.FishHook hook)) {
            return null;
        }
        net.minecraft.world.InteractionHand hand = rod != null && player.getOffhandItem() == rod
                ? net.minecraft.world.InteractionHand.OFF_HAND : net.minecraft.world.InteractionHand.MAIN_HAND;
        var event = new org.bukkit.event.player.PlayerFishEvent(bukkitPlayer,
                caught == null ? null : ((io.lunararcdevs.lunararc.common.bridge.EntityBridge) caught).lunararc$getBukkitEntity(),
                hook, org.bukkit.craftbukkit.CraftEquipmentSlot.getHand(hand), state);
        if (exp >= 0) event.setExpToDrop(exp);
        org.bukkit.Bukkit.getPluginManager().callEvent(event);
        return event;
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "retrieve", cancellable = true, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/projectile/FishingHook;pullEntity(Lnet/minecraft/world/entity/Entity;)V"))
    private void lunararc$caughtEntity(net.minecraft.world.item.ItemStack rod,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Integer> cir) {
        FishingHook hook = (FishingHook) (Object) this;
        var event = lunararc$fireFish(hook.getPlayerOwner(), this.hookedIn, rod, org.bukkit.event.player.PlayerFishEvent.State.CAUGHT_ENTITY, -1);
        if (event != null && event.isCancelled()) cir.setReturnValue(0);
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "retrieve", cancellable = true, at = @At(value = "INVOKE", ordinal = 0,
            target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private void lunararc$caughtFish(net.minecraft.world.item.ItemStack rod,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Integer> cir,
            @com.llamalad7.mixinextras.sugar.Local net.minecraft.world.entity.item.ItemEntity item) {
        FishingHook hook = (FishingHook) (Object) this;
        int exp = hook.getRandom().nextInt(6) + 1;
        var event = lunararc$fireFish(hook.getPlayerOwner(), item, rod, org.bukkit.event.player.PlayerFishEvent.State.CAUGHT_FISH, exp);
        this.lunararc$fishExp = event == null ? exp : event.getExpToDrop();
        if (event != null && event.isCancelled()) cir.setReturnValue(0);
    }

    @org.spongepowered.asm.mixin.injection.ModifyArg(method = "retrieve", index = 4, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/ExperienceOrb;<init>(Lnet/minecraft/world/level/Level;DDDI)V"))
    private int lunararc$fishExpValue(int value) {
        return this.lunararc$fishExp;
    }

    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "retrieve", at = @At(value = "INVOKE", ordinal = 1,
            target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean lunararc$fishExpOrb(Level level, Entity orb, com.llamalad7.mixinextras.injector.wrapoperation.Operation<Boolean> original) {
        return this.lunararc$fishExp > 0 && original.call(level, orb);
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "retrieve", cancellable = true, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/projectile/FishingHook;discard()V"))
    private void lunararc$reelIn(net.minecraft.world.item.ItemStack rod,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Integer> cir,
            @com.llamalad7.mixinextras.sugar.Local(ordinal = 0) int result) {
        FishingHook hook = (FishingHook) (Object) this;
        org.bukkit.event.player.PlayerFishEvent.State state = hook.onGround() ? org.bukkit.event.player.PlayerFishEvent.State.IN_GROUND
                : result == 0 ? org.bukkit.event.player.PlayerFishEvent.State.REEL_IN : null;
        if (state == null) return;
        var event = lunararc$fireFish(hook.getPlayerOwner(), null, rod, state, -1);
        if (event != null && event.isCancelled()) cir.setReturnValue(0);
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "catchingFish", at = @At(value = "INVOKE", ordinal = 0, shift = At.Shift.AFTER,
            target = "Lnet/minecraft/network/syncher/SynchedEntityData;set(Lnet/minecraft/network/syncher/EntityDataAccessor;Ljava/lang/Object;)V"))
    private void lunararc$failedAttempt(BlockPos pos, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        FishingHook hook = (FishingHook) (Object) this;
        if (hook.getPlayerOwner() != null) {
            lunararc$fireFish(hook.getPlayerOwner(), null, null, org.bukkit.event.player.PlayerFishEvent.State.FAILED_ATTEMPT, 0);
        }
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "catchingFish", cancellable = true, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/projectile/FishingHook;playSound(Lnet/minecraft/sounds/SoundEvent;FF)V"))
    private void lunararc$bite(BlockPos pos, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        FishingHook hook = (FishingHook) (Object) this;
        if (hook.getPlayerOwner() == null) return;
        var event = lunararc$fireFish(hook.getPlayerOwner(), null, null, org.bukkit.event.player.PlayerFishEvent.State.BITE, -1);
        if (event != null && event.isCancelled()) ci.cancel();
    }

    @Override public int lunararc$getMinWaitTime(){return lunararc$minWaitTime;} @Override public void lunararc$setMinWaitTime(int v){lunararc$minWaitTime=v;}
    @Override public int lunararc$getMaxWaitTime(){return lunararc$maxWaitTime;} @Override public void lunararc$setMaxWaitTime(int v){lunararc$maxWaitTime=v;}
    @Override public int lunararc$getMinLureTime(){return lunararc$minLureTime;} @Override public void lunararc$setMinLureTime(int v){lunararc$minLureTime=v;}
    @Override public int lunararc$getMaxLureTime(){return lunararc$maxLureTime;} @Override public void lunararc$setMaxLureTime(int v){lunararc$maxLureTime=v;}
    @Override public float lunararc$getMinLureAngle(){return lunararc$minLureAngle;} @Override public void lunararc$setMinLureAngle(float v){lunararc$minLureAngle=v;}
    @Override public float lunararc$getMaxLureAngle(){return lunararc$maxLureAngle;} @Override public void lunararc$setMaxLureAngle(float v){lunararc$maxLureAngle=v;}
    @Override public boolean lunararc$isApplyLure(){return lunararc$applyLure;} @Override public void lunararc$setApplyLure(boolean v){lunararc$applyLure=v;}
    @Override public boolean lunararc$isRainInfluenced(){return lunararc$rainInfluenced;} @Override public void lunararc$setRainInfluenced(boolean v){lunararc$rainInfluenced=v;}
    @Override public boolean lunararc$isSkyInfluenced(){return lunararc$skyInfluenced;} @Override public void lunararc$setSkyInfluenced(boolean v){lunararc$skyInfluenced=v;}
    @Override public double lunararc$getBiteChance(){return lunararc$biteChance;} @Override public void lunararc$setBiteChance(double v){lunararc$biteChance=v;}
    @Override public int lunararc$getOutOfWaterTime(){return outOfWaterTime;}
    @Override public Entity lunararc$getHookedIn(){return hookedIn;} @Override public void lunararc$setHookedIn(Entity e){lunararc$invokeSetHookedEntity(e);}
    @Override public int lunararc$getStateOrdinal(){ FishingHook hook=(FishingHook)(Object)this; if(hookedIn!=null)return 1; return hook.level().getFluidState(hook.blockPosition()).is(FluidTags.WATER)?2:0; }
    @Override public int lunararc$getTimeUntilLured(){return timeUntilLured;} @Override public void lunararc$setTimeUntilLured(int v){timeUntilLured=v;}
    @Override public int lunararc$getTimeUntilHooked(){return timeUntilHooked;} @Override public void lunararc$setTimeUntilHooked(int v){timeUntilHooked=v;}
    @Override public void lunararc$resetFishingState(){timeUntilLured=0; timeUntilHooked=0;}
    @Override public boolean lunararc$calculateOpenWater(BlockPos p){return lunararc$invokeCalculateOpenWater(p);}
    @Override public void lunararc$pullEntity(Entity e){lunararc$invokePullEntity(e);}
}
