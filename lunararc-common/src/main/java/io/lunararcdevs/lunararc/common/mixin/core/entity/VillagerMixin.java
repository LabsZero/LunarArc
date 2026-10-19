package io.lunararcdevs.lunararc.common.mixin.core.entity;

import io.lunararcdevs.lunararc.common.bridge.entity.VillagerBridge;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Villager.class)
public abstract class VillagerMixin implements VillagerBridge {
    @Shadow private int villagerXp;
    @Shadow private int numberOfRestocksToday;
    @Invoker("increaseMerchantCareer") public abstract void lunararc$invokeIncreaseMerchantCareer(net.minecraft.server.level.ServerLevel level);
    @Override public void lunararc$increaseMerchantCareer(net.minecraft.server.level.ServerLevel level) { lunararc$invokeIncreaseMerchantCareer(level); }
    @Override public int lunararc$getVillagerXp() { return this.villagerXp; }
    @Override public void lunararc$setVillagerXp(int xp) { this.villagerXp = xp; }
    @Override public int lunararc$getRestocksToday() { return this.numberOfRestocksToday; }
    @Override public void lunararc$setRestocksToday(int restocks) { this.numberOfRestocksToday = restocks; }
}
