package io.lunararcdevs.lunararc.common.mixin.core.inventory;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.lunararcdevs.lunararc.common.bridge.EntityBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnchantingTableBlock;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.enchantments.CraftEnchantment;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin implements io.lunararcdevs.lunararc.common.bridge.access.EnchantmentMenuAccessBridge {
    @Shadow @Final public int[] costs;
    @Shadow @Final public int[] enchantClue;
    @Shadow @Final public int[] levelClue;
    @Shadow @Final private Container enchantSlots;
    @Shadow @Final private net.minecraft.world.inventory.DataSlot enchantmentSeed;

    @Override public Container lunararc$getEnchantSlots() { return this.enchantSlots; }
    @Override public net.minecraft.world.inventory.DataSlot lunararc$getEnchantmentSeed() { return this.enchantmentSeed; }

    @Unique
    private org.bukkit.craftbukkit.inventory.CraftEnchantmentView lunararc$view(org.bukkit.entity.Player player) {
        return new org.bukkit.craftbukkit.inventory.CraftEnchantmentView((org.bukkit.craftbukkit.entity.CraftPlayer) player,
                (EnchantmentMenu) (Object) this, net.kyori.adventure.text.Component.translatable("container.enchant"));
    }

    @Unique private Player lunararc$viewer;
    @Unique private Player lunararc$clickPlayer;
    @Unique private Level lunararc$level;
    @Unique private BlockPos lunararc$pos;

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("TAIL"))
    private void lunararc$captureViewer(int id, Inventory inventory, ContainerLevelAccess access, CallbackInfo ci) {
        this.lunararc$viewer = inventory.player;
    }

    @WrapOperation(method = "clickMenuButton", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/inventory/ContainerLevelAccess;execute(Ljava/util/function/BiConsumer;)V"))
    private void lunararc$captureClick(ContainerLevelAccess access, BiConsumer<Level, BlockPos> action, Operation<Void> original, Player player, int id) {
        original.call(access, (BiConsumer<Level, BlockPos>) (level, pos) -> {
            this.lunararc$clickPlayer = player;
            this.lunararc$level = level;
            this.lunararc$pos = pos;
            try {
                action.accept(level, pos);
            } finally {
                this.lunararc$clickPlayer = null;
            }
        });
    }

    @Inject(method = "getEnchantmentList", at = @At("RETURN"), cancellable = true)
    private void lunararc$enchantItem(RegistryAccess registry, ItemStack stack, int slot, int cost,
            CallbackInfoReturnable<List<EnchantmentInstance>> cir) {
        Player player = this.lunararc$clickPlayer;
        this.lunararc$clickPlayer = null;
        if (player == null || !(this.lunararc$level instanceof ServerLevel level)
                || !(((EntityBridge) player).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Player bukkitPlayer)) {
            return;
        }
        Registry<Enchantment> enchantments = registry.registryOrThrow(Registries.ENCHANTMENT);
        Map<org.bukkit.enchantments.Enchantment, Integer> toAdd = new HashMap<>();
        for (EnchantmentInstance instance : cir.getReturnValue()) {
            toAdd.put(CraftEnchantment.minecraftHolderToBukkit(instance.enchantment), instance.level);
        }
        org.bukkit.enchantments.Enchantment hint = enchantments.getHolder(this.enchantClue[slot])
                .map(CraftEnchantment::minecraftHolderToBukkit).orElse(null);
        EnchantItemEvent event = new EnchantItemEvent(bukkitPlayer, lunararc$view(bukkitPlayer), CraftBlock.at(level, this.lunararc$pos),
                CraftItemStack.asCraftMirror(stack), cost, toAdd, hint, this.levelClue[slot], slot);
        org.bukkit.Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled() || (event.getExpLevelCost() > player.experienceLevel && !player.getAbilities().instabuild)
                || event.getEnchantsToAdd().isEmpty()) {
            cir.setReturnValue(List.of());
            return;
        }
        List<EnchantmentInstance> result = new ArrayList<>();
        event.getEnchantsToAdd().forEach((enchantment, lvl) ->
                result.add(new EnchantmentInstance(CraftEnchantment.bukkitToMinecraftHolder(enchantment), lvl)));
        cir.setReturnValue(result);
    }

    @ModifyExpressionValue(method = "slotsChanged", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEnchantable()Z"))
    private boolean lunararc$offerAnyItem(boolean enchantable) {
        return enchantable || this.lunararc$viewer instanceof net.minecraft.server.level.ServerPlayer;
    }

    @WrapOperation(method = "slotsChanged", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/inventory/ContainerLevelAccess;execute(Ljava/util/function/BiConsumer;)V"))
    private void lunararc$prepareEnchant(ContainerLevelAccess access, BiConsumer<Level, BlockPos> action, Operation<Void> original) {
        original.call(access, (BiConsumer<Level, BlockPos>) (level, pos) -> {
            action.accept(level, pos);
            lunararc$firePrepare(level, pos);
        });
    }

    @Unique
    private void lunararc$firePrepare(Level level, BlockPos pos) {
        ItemStack stack = this.enchantSlots.getItem(0);
        if (!(level instanceof ServerLevel serverLevel) || this.lunararc$viewer == null
                || !(((EntityBridge) this.lunararc$viewer).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Player bukkitPlayer)) {
            return;
        }
        org.bukkit.craftbukkit.inventory.CraftEnchantmentView view = lunararc$view(bukkitPlayer);
        int power = 0;
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            if (EnchantingTableBlock.isValidBookShelf(level, pos, offset)) power++;
        }
        PrepareItemEnchantEvent event = new PrepareItemEnchantEvent(bukkitPlayer, view,
                CraftBlock.at(serverLevel, pos), CraftItemStack.asCraftMirror(stack), view.getOffers(), power);
        event.setCancelled(!stack.isEnchantable());
        org.bukkit.Bukkit.getPluginManager().callEvent(event);
        view.setOffers(event.isCancelled() ? new EnchantmentOffer[3] : event.getOffers());
        ((EnchantmentMenu) (Object) this).broadcastChanges();
    }
}
