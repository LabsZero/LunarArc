package io.lunararcdevs.lunararc.common.bridge.entity;

public interface VillagerBridge {
    int lunararc$getVillagerXp();
    void lunararc$setVillagerXp(int xp);
    int lunararc$getRestocksToday();
    void lunararc$setRestocksToday(int restocks);
    /** Levels the villager up one step and unlocks that level's trades. */
    void lunararc$increaseMerchantCareer(net.minecraft.server.level.ServerLevel level);
}
