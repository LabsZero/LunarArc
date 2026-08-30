package io.ampznetwork.lunararc.common.mixin.core.server;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import io.ampznetwork.lunararc.common.LunarArcServerAccess;
import io.ampznetwork.lunararc.common.bridge.EntityBridge;
import io.ampznetwork.lunararc.common.server.LunarArcCommandRouter;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandSignedPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashSet;
import java.util.Set;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {

    @Shadow
    public ServerPlayer player;

    @Unique private boolean lunararc$resyncAfterSpecialInventoryClick;

    @Inject(
            method = "broadcastChatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void lunararc$onChatBroadcast(PlayerChatMessage message, CallbackInfo ci) {
        CraftServer craftServer = LunarArcServerAccess.getCraftServer(this.player.server);
        Object bukkit = ((EntityBridge) this.player).lunararc$getBukkitEntity();
        if (!(bukkit instanceof Player bukkitPlayer)) {
            throw new IllegalStateException("ServerPlayer bridge did not expose a Bukkit Player");
        }

        Set<Player> recipients = new LinkedHashSet<>(craftServer.getOnlinePlayers());
        Set<Player> originalRecipients = Set.copyOf(recipients);
        String originalMessage = message.signedContent();
        AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(
                !this.player.server.isSameThread(), bukkitPlayer, originalMessage, recipients);
        String originalFormat = event.getFormat();
        craftServer.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            ci.cancel();
            return;
        }

        if (!originalMessage.equals(event.getMessage())
                || !originalFormat.equals(event.getFormat())
                || !originalRecipients.equals(event.getRecipients())) {
            String formatted = String.format(event.getFormat(), bukkitPlayer.getDisplayName(), event.getMessage());
            for (Player recipient : event.getRecipients()) {
                recipient.sendMessage(formatted);
            }
            craftServer.getConsoleSender().sendMessage(formatted);
            ci.cancel();
        }
    }

    @Inject(method = "handleChatCommand", at = @At("HEAD"), cancellable = true, require = 0)
    private void lunararc$routeCommandPacket(ServerboundChatCommandPacket packet, CallbackInfo ci) {
        if (!this.player.server.isSameThread()) {
            this.player.server.execute(() -> ((ServerGamePacketListenerImpl) (Object) this).handleChatCommand(packet));
            ci.cancel();
            return;
        }

        Object bukkit = ((EntityBridge) this.player).lunararc$getBukkitEntity();
        if (!(bukkit instanceof Player bukkitPlayer)) {
            return;
        }

        CraftServer craftServer = LunarArcServerAccess.getCraftServer(this.player.server);
        if (LunarArcCommandRouter.routePlayerPacket(craftServer, bukkitPlayer, packet.command()) == LunarArcCommandRouter.PacketResult.CANCEL) {
            ci.cancel();
        }
    }


    @Inject(method = "handleSignedChatCommand", at = @At("HEAD"), cancellable = true, require = 0)
    private void lunararc$routeSignedCommandPacket(ServerboundChatCommandSignedPacket packet, CallbackInfo ci) {
        if (!this.player.server.isSameThread()) {
            this.player.server.execute(() -> ((ServerGamePacketListenerImpl) (Object) this).handleSignedChatCommand(packet));
            ci.cancel();
            return;
        }
        Object bukkit = ((EntityBridge) this.player).lunararc$getBukkitEntity();
        if (!(bukkit instanceof Player bukkitPlayer)) {
            return;
        }
        CraftServer craftServer = LunarArcServerAccess.getCraftServer(this.player.server);
        if (LunarArcCommandRouter.routePlayerPacket(craftServer, bukkitPlayer, packet.command())
                == LunarArcCommandRouter.PacketResult.CANCEL) {
            ci.cancel();
        }
    }


    @Inject(method = "handlePlayerAction", at = @At("HEAD"), cancellable = true, require = 0)
    private void lunararc$onSwapHands(ServerboundPlayerActionPacket packet, CallbackInfo ci) {
        if (!this.player.server.isSameThread()) {
            this.player.server.execute(() -> ((ServerGamePacketListenerImpl) (Object) this).handlePlayerAction(packet));
            ci.cancel();
            return;
        }
        if (packet.getAction() != ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND) {
            return;
        }
        Object bukkit = ((EntityBridge) this.player).lunararc$getBukkitEntity();
        if (!(bukkit instanceof Player bukkitPlayer)) {
            return;
        }
        org.bukkit.inventory.ItemStack oldMain = bukkitPlayer.getInventory().getItemInMainHand();
        org.bukkit.inventory.ItemStack oldOff = bukkitPlayer.getInventory().getItemInOffHand();
        org.bukkit.event.player.PlayerSwapHandItemsEvent event =
                new org.bukkit.event.player.PlayerSwapHandItemsEvent(bukkitPlayer, oldOff.clone(), oldMain.clone());
        LunarArcServerAccess.getCraftServer(this.player.server).getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            ci.cancel();
            return;
        }
        if (!java.util.Objects.equals(event.getMainHandItem(), oldOff)
                || !java.util.Objects.equals(event.getOffHandItem(), oldMain)) {
            bukkitPlayer.getInventory().setItemInMainHand(event.getMainHandItem());
            bukkitPlayer.getInventory().setItemInOffHand(event.getOffHandItem());
            ci.cancel();
        }
    }


    @Inject(
            method = "handleInteract",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ServerboundInteractPacket;dispatch(Lnet/minecraft/network/protocol/game/ServerboundInteractPacket$Handler;)V"),
            cancellable = true,
            require = 0)
    private void lunararc$onInteractEntity(
            net.minecraft.network.protocol.game.ServerboundInteractPacket packet,
            CallbackInfo ci) {
        net.minecraft.world.entity.Entity target = packet.getTarget(this.player.serverLevel());
        if (target == null || target == this.player) {
            return;
        }

        Object bukkitPlayerObject = ((EntityBridge) this.player).lunararc$getBukkitEntity();
        Object bukkitTargetObject = ((EntityBridge) target).lunararc$getBukkitEntity();
        if (!(bukkitPlayerObject instanceof Player bukkitPlayer)
                || !(bukkitTargetObject instanceof org.bukkit.entity.Entity bukkitTarget)) {
            return;
        }

        final boolean[] cancelled = {false};
        packet.dispatch(new net.minecraft.network.protocol.game.ServerboundInteractPacket.Handler() {
            private org.bukkit.inventory.EquipmentSlot lunararc$slot(net.minecraft.world.InteractionHand hand) {
                return hand == net.minecraft.world.InteractionHand.OFF_HAND
                        ? org.bukkit.inventory.EquipmentSlot.OFF_HAND
                        : org.bukkit.inventory.EquipmentSlot.HAND;
            }

            @Override
            public void onInteraction(net.minecraft.world.InteractionHand hand) {
                org.bukkit.event.player.PlayerInteractEntityEvent event =
                        new org.bukkit.event.player.PlayerInteractEntityEvent(
                                bukkitPlayer, bukkitTarget, lunararc$slot(hand));
                LunarArcServerAccess.getCraftServer(ServerGamePacketListenerImplMixin.this.player.server)
                        .getPluginManager().callEvent(event);
                cancelled[0] |= event.isCancelled();
            }

            @Override
            public void onInteraction(net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.Vec3 position) {
                org.bukkit.event.player.PlayerInteractAtEntityEvent event =
                        new org.bukkit.event.player.PlayerInteractAtEntityEvent(
                                bukkitPlayer,
                                bukkitTarget,
                                new org.bukkit.util.Vector(position.x, position.y, position.z),
                                lunararc$slot(hand));
                LunarArcServerAccess.getCraftServer(ServerGamePacketListenerImplMixin.this.player.server)
                        .getPluginManager().callEvent(event);
                cancelled[0] |= event.isCancelled();
            }

            @Override
            public void onAttack() {
                // Attacks are handled by the existing entity-damage pipeline.
            }
        });

        if (cancelled[0]) {
            ci.cancel();
        }
    }

    @Inject(method = "handleAnimate", at = @At("HEAD"), cancellable = true, require = 0)
    private void lunararc$onAnimation(
            net.minecraft.network.protocol.game.ServerboundSwingPacket packet,
            CallbackInfo ci) {
        if (!this.player.server.isSameThread()) {
            this.player.server.execute(() -> ((ServerGamePacketListenerImpl) (Object) this).handleAnimate(packet));
            ci.cancel();
            return;
        }
        Object bukkit = ((EntityBridge) this.player).lunararc$getBukkitEntity();
        if (!(bukkit instanceof Player bukkitPlayer)) return;

        if (lunararc$isLeftClickAir()) {
            // CraftBukkit/Paper reports attack-air against the selected main-hand item;
            // the swing packet's hand is still preserved separately for PlayerAnimationEvent.
            org.bukkit.event.player.PlayerInteractEvent interact =
                    org.bukkit.craftbukkit.event.CraftEventFactory.callPlayerInteractEvent(
                            this.player, org.bukkit.event.block.Action.LEFT_CLICK_AIR, null, null,
                            this.player.getMainHandItem(), org.bukkit.inventory.EquipmentSlot.HAND);
            if (interact != null && (interact.isCancelled()
                    || interact.useItemInHand() == org.bukkit.event.Event.Result.DENY)) {
                ci.cancel();
                return;
            }
        }

        org.bukkit.event.player.PlayerAnimationType type =
                packet.getHand() == net.minecraft.world.InteractionHand.OFF_HAND
                        ? org.bukkit.event.player.PlayerAnimationType.OFF_ARM_SWING
                        : org.bukkit.event.player.PlayerAnimationType.ARM_SWING;
        org.bukkit.event.player.PlayerAnimationEvent event =
                new org.bukkit.event.player.PlayerAnimationEvent(bukkitPlayer, type);
        LunarArcServerAccess.getCraftServer(this.player.server).getPluginManager().callEvent(event);
        if (event.isCancelled()) ci.cancel();
    }

    private boolean lunararc$isLeftClickAir() {
        net.minecraft.world.phys.Vec3 eye = this.player.getEyePosition();
        net.minecraft.world.phys.Vec3 look = this.player.getViewVector(1.0F);
        double blockRange = this.player.blockInteractionRange();
        net.minecraft.world.phys.Vec3 blockEnd = eye.add(look.scale(blockRange));
        net.minecraft.world.phys.BlockHitResult blockHit = this.player.serverLevel().clip(
                new net.minecraft.world.level.ClipContext(eye, blockEnd,
                        net.minecraft.world.level.ClipContext.Block.OUTLINE,
                        net.minecraft.world.level.ClipContext.Fluid.NONE, this.player));
        if (blockHit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) return false;

        double entityRange = this.player.entityInteractionRange();
        net.minecraft.world.phys.Vec3 entityEnd = eye.add(look.scale(entityRange));
        net.minecraft.world.phys.AABB box = this.player.getBoundingBox().expandTowards(look.scale(entityRange)).inflate(1.0D);
        net.minecraft.world.phys.EntityHitResult entityHit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                this.player.serverLevel(), this.player, eye, entityEnd, box,
                entity -> !entity.isSpectator() && entity.isPickable() && !entity.isPassengerOfSameVehicle(this.player),
                (float) (entityRange * entityRange));
        return entityHit == null;
    }

    @Inject(method = "handleUseItem", at = @At("HEAD"), cancellable = true, require = 0)
    private void lunararc$onRightClickAir(ServerboundUseItemPacket packet, CallbackInfo ci) {
        if (!this.player.server.isSameThread()) {
            this.player.server.execute(() -> ((ServerGamePacketListenerImpl) (Object) this).handleUseItem(packet));
            ci.cancel();
            return;
        }
        net.minecraft.world.InteractionHand hand = packet.getHand();
        net.minecraft.world.item.ItemStack stack = this.player.getItemInHand(hand);
        if (stack.isEmpty() || !stack.isItemEnabled(this.player.serverLevel().enabledFeatures())) {
            return;
        }

        org.bukkit.inventory.EquipmentSlot slot = hand == net.minecraft.world.InteractionHand.OFF_HAND
                ? org.bukkit.inventory.EquipmentSlot.OFF_HAND
                : org.bukkit.inventory.EquipmentSlot.HAND;
        org.bukkit.event.player.PlayerInteractEvent event =
                org.bukkit.craftbukkit.event.CraftEventFactory.callPlayerInteractEvent(
                        this.player,
                        org.bukkit.event.block.Action.RIGHT_CLICK_AIR,
                        null,
                        null,
                        stack,
                        slot);
        if (event != null && (event.isCancelled()
                || event.useItemInHand() == org.bukkit.event.Event.Result.DENY)) {
            // Cancel only the Bukkit-visible air use. The original loader-owned
            // packet method remains intact whenever plugins permit the action.
            this.player.getInventory().setChanged();
            ci.cancel();
        }
    }

    @Inject(method = "handleSetCarriedItem", at = @At("HEAD"), cancellable = true, require = 0)
    private void lunararc$onHeldItemChange(ServerboundSetCarriedItemPacket packet, CallbackInfo ci) {
        if (!this.player.server.isSameThread()) {
            this.player.server.execute(() -> ((ServerGamePacketListenerImpl) (Object) this).handleSetCarriedItem(packet));
            ci.cancel();
            return;
        }

        int slot = packet.getSlot();
        if (slot < 0 || slot >= Inventory.getSelectionSize() || slot == this.player.getInventory().selected) {
            return;
        }
        Object bukkit = ((EntityBridge) this.player).lunararc$getBukkitEntity();
        if (!(bukkit instanceof Player bukkitPlayer)) {
            return;
        }
        org.bukkit.event.player.PlayerItemHeldEvent event = new org.bukkit.event.player.PlayerItemHeldEvent(
                bukkitPlayer, this.player.getInventory().selected, slot);
        LunarArcServerAccess.getCraftServer(this.player.server).getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            ((ServerGamePacketListenerImpl) (Object) this).send(
                    new ClientboundSetCarriedItemPacket(this.player.getInventory().selected));
            this.player.resetLastActionTime();
            ci.cancel();
        }
    }

    @Inject(method = "handlePlayerCommand", at = @At("HEAD"), cancellable = true, require = 0)
    private void lunararc$onPlayerCommandState(ServerboundPlayerCommandPacket packet, CallbackInfo ci) {
        if (!this.player.server.isSameThread()) {
            this.player.server.execute(() -> ((ServerGamePacketListenerImpl) (Object) this).handlePlayerCommand(packet));
            ci.cancel();
            return;
        }
        Object bukkit = ((EntityBridge) this.player).lunararc$getBukkitEntity();
        if (!(bukkit instanceof Player bukkitPlayer)) {
            return;
        }
        var action = packet.getAction();
        if (action == ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY
                || action == ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY) {
            boolean sneaking = action == ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY;
            org.bukkit.event.player.PlayerToggleSneakEvent event =
                    new org.bukkit.event.player.PlayerToggleSneakEvent(bukkitPlayer, sneaking);
            LunarArcServerAccess.getCraftServer(this.player.server).getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                ci.cancel();
            }
        } else if (action == ServerboundPlayerCommandPacket.Action.START_SPRINTING
                || action == ServerboundPlayerCommandPacket.Action.STOP_SPRINTING) {
            boolean sprinting = action == ServerboundPlayerCommandPacket.Action.START_SPRINTING;
            org.bukkit.event.player.PlayerToggleSprintEvent event =
                    new org.bukkit.event.player.PlayerToggleSprintEvent(bukkitPlayer, sprinting);
            LunarArcServerAccess.getCraftServer(this.player.server).getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "handlePlayerAbilities", at = @At("HEAD"), cancellable = true, require = 0)
    private void lunararc$onFlightToggle(ServerboundPlayerAbilitiesPacket packet, CallbackInfo ci) {
        if (!this.player.server.isSameThread()) {
            this.player.server.execute(() -> ((ServerGamePacketListenerImpl) (Object) this).handlePlayerAbilities(packet));
            ci.cancel();
            return;
        }
        boolean requested = packet.isFlying();
        if (requested == this.player.getAbilities().flying || !this.player.getAbilities().mayfly) {
            return;
        }
        Object bukkit = ((EntityBridge) this.player).lunararc$getBukkitEntity();
        if (!(bukkit instanceof Player bukkitPlayer)) {
            return;
        }
        org.bukkit.event.player.PlayerToggleFlightEvent event =
                new org.bukkit.event.player.PlayerToggleFlightEvent(bukkitPlayer, requested);
        LunarArcServerAccess.getCraftServer(this.player.server).getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            this.player.onUpdateAbilities();
            ci.cancel();
        }
    }

    @Inject(method = "handleSetCreativeModeSlot", at = @At("HEAD"), cancellable = true, require = 0)
    private void lunararc$onCreativeSlot(ServerboundSetCreativeModeSlotPacket packet, CallbackInfo ci) {
        if (!this.player.server.isSameThread()) {
            this.player.server.execute(() -> ((ServerGamePacketListenerImpl) (Object) this).handleSetCreativeModeSlot(packet));
            ci.cancel();
            return;
        }
        if (!this.player.gameMode.isCreative()) {
            return;
        }
        int slot = packet.slotNum();
        if (slot < 1 || slot > 45) {
            return;
        }
        Object bukkit = ((EntityBridge) this.player).lunararc$getBukkitEntity();
        if (!(bukkit instanceof org.bukkit.craftbukkit.entity.CraftPlayer craftPlayer)) {
            return;
        }
        org.bukkit.inventory.InventoryView view = craftPlayer.getOpenInventory();
        org.bukkit.event.inventory.InventoryType.SlotType slotType;
        try {
            slotType = view.getSlotType(slot);
        } catch (RuntimeException ignored) {
            slotType = org.bukkit.event.inventory.InventoryType.SlotType.CONTAINER;
        }
        org.bukkit.inventory.ItemStack proposed =
                org.bukkit.craftbukkit.inventory.CraftItemStack.asBukkitCopy(packet.itemStack());
        org.bukkit.event.inventory.InventoryCreativeEvent event =
                new org.bukkit.event.inventory.InventoryCreativeEvent(view, slotType, slot, proposed);
        LunarArcServerAccess.getCraftServer(this.player.server).getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            this.player.inventoryMenu.broadcastFullState();
            ci.cancel();
            return;
        }
        if (!java.util.Objects.equals(event.getCursor(), proposed)) {
            net.minecraft.world.item.ItemStack replacement =
                    org.bukkit.craftbukkit.inventory.CraftItemStack.asNMSCopy(event.getCursor());
            this.player.inventoryMenu.getSlot(slot).setByPlayer(replacement);
            this.player.inventoryMenu.broadcastChanges();
            ci.cancel();
        }
    }

    @Inject(method = "handleContainerClick", at = @At("HEAD"), cancellable = true, require = 0)
    private void lunararc$ensureContainerClickThread(ServerboundContainerClickPacket packet, CallbackInfo ci) {
        if (!this.player.server.isSameThread()) {
            this.player.server.execute(() -> ((ServerGamePacketListenerImpl) (Object) this).handleContainerClick(packet));
            ci.cancel();
        }
    }

    @Inject(
            method = "handleContainerClick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;clicked(IILnet/minecraft/world/inventory/ClickType;Lnet/minecraft/world/entity/player/Player;)V"),
            cancellable = true
    )
    private void lunararc$onContainerClick(ServerboundContainerClickPacket packet, CallbackInfo ci) {
        // QUICK_CRAFT is a multi-packet drag operation. It is handled against
        // AbstractContainerMenu's authoritative drag state by
        // AbstractContainerMenuMixin and must not be approximated as a click.
        if (packet.getClickType() == net.minecraft.world.inventory.ClickType.QUICK_CRAFT) {
            return;
        }

        Object bukkit = ((EntityBridge) this.player).lunararc$getBukkitEntity();
        if (!(bukkit instanceof org.bukkit.craftbukkit.entity.CraftPlayer craftPlayer)) {
            return;
        }

        org.bukkit.inventory.InventoryView view = craftPlayer.getOpenInventory();
        int rawSlot = packet.getSlotNum();
        org.bukkit.event.inventory.InventoryType.SlotType slotType = view.getSlotType(rawSlot);
        org.bukkit.event.inventory.ClickType click = lunararc$clickType(packet.getClickType(), packet.getButtonNum());
        org.bukkit.event.inventory.InventoryAction action = org.bukkit.event.inventory.InventoryAction.UNKNOWN;
        int hotbarKey = packet.getClickType() == net.minecraft.world.inventory.ClickType.SWAP ? packet.getButtonNum() : -1;
        org.bukkit.event.inventory.InventoryClickEvent event;
        if (rawSlot == 0 && view.getTopInventory() instanceof org.bukkit.inventory.CraftingInventory crafting
                && crafting.getRecipe() != null) {
            event = hotbarKey >= 0
                    ? new org.bukkit.event.inventory.CraftItemEvent(crafting.getRecipe(), view, slotType, rawSlot, click, action, hotbarKey)
                    : new org.bukkit.event.inventory.CraftItemEvent(crafting.getRecipe(), view, slotType, rawSlot, click, action);
            this.lunararc$resyncAfterSpecialInventoryClick = true;
        } else if (rawSlot == 3 && view.getTopInventory() instanceof org.bukkit.inventory.SmithingInventory smithing
                && smithing.getResult() != null) {
            event = hotbarKey >= 0
                    ? new org.bukkit.event.inventory.SmithItemEvent(view, slotType, rawSlot, click, action, hotbarKey)
                    : new org.bukkit.event.inventory.SmithItemEvent(view, slotType, rawSlot, click, action);
            this.lunararc$resyncAfterSpecialInventoryClick = true;
        } else {
            event = new org.bukkit.event.inventory.InventoryClickEvent(view, slotType, rawSlot, click, action, hotbarKey);
        }
        LunarArcServerAccess.getCraftServer(this.player.server).getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            // Vanilla suppresses remote updates immediately before invoking clicked().
            // We cancel at that invocation point, so explicitly restore update flow and
            // send authoritative contents back to the client.
            this.player.containerMenu.resumeRemoteUpdates();
            this.player.containerMenu.broadcastFullState();
            this.lunararc$resyncAfterSpecialInventoryClick = false;
            ci.cancel();
        }
    }

    @Inject(method = "handleContainerClick", at = @At("RETURN"), require = 0)
    private void lunararc$resyncSpecialInventoryClick(ServerboundContainerClickPacket packet, CallbackInfo ci) {
        if (!this.lunararc$resyncAfterSpecialInventoryClick) return;
        this.lunararc$resyncAfterSpecialInventoryClick = false;
        this.player.containerMenu.sendAllDataToRemote();
    }

    private static org.bukkit.event.inventory.ClickType lunararc$clickType(
            net.minecraft.world.inventory.ClickType type, int button) {
        return switch (type) {
            case PICKUP -> button == 1
                    ? org.bukkit.event.inventory.ClickType.RIGHT
                    : org.bukkit.event.inventory.ClickType.LEFT;
            case QUICK_MOVE -> button == 1
                    ? org.bukkit.event.inventory.ClickType.SHIFT_RIGHT
                    : org.bukkit.event.inventory.ClickType.SHIFT_LEFT;
            case SWAP -> org.bukkit.event.inventory.ClickType.NUMBER_KEY;
            case CLONE -> org.bukkit.event.inventory.ClickType.MIDDLE;
            case THROW -> button == 1
                    ? org.bukkit.event.inventory.ClickType.CONTROL_DROP
                    : org.bukkit.event.inventory.ClickType.DROP;
            case PICKUP_ALL -> org.bukkit.event.inventory.ClickType.DOUBLE_CLICK;
            case QUICK_CRAFT -> org.bukkit.event.inventory.ClickType.UNKNOWN;
        };
    }

    @WrapOperation(
            method = {"updateBookContents", "signBook"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;setItem(ILnet/minecraft/world/item/ItemStack;)V"),
            require = 0)
    private void lunararc$bookEdit(net.minecraft.world.entity.player.Inventory inventory, int slot,
                                    net.minecraft.world.item.ItemStack proposed, Operation<Void> original) {
        net.minecraft.world.item.ItemStack old = inventory.getItem(slot).copy();
        net.minecraft.world.item.ItemStack result = org.bukkit.craftbukkit.event.CraftEventFactory.handleEditBookEvent(
                this.player, slot, old, proposed);
        original.call(inventory, slot, result);
    }

    @Inject(method = "handleSelectTrade", at = @At("HEAD"), cancellable = true, require = 0)
    private void lunararc$tradeSelect(net.minecraft.network.protocol.game.ServerboundSelectTradePacket packet,
                                      CallbackInfo ci) {
        if (!this.player.server.isSameThread()) {
            this.player.server.execute(() -> ((ServerGamePacketListenerImpl) (Object) this).handleSelectTrade(packet));
            ci.cancel();
            return;
        }
        if (!(this.player.containerMenu instanceof net.minecraft.world.inventory.MerchantMenu)) return;
        Object bukkit = ((EntityBridge) this.player).lunararc$getBukkitEntity();
        if (!(bukkit instanceof org.bukkit.craftbukkit.entity.CraftPlayer craftPlayer)) return;
        org.bukkit.inventory.InventoryView rawView = craftPlayer.getOpenInventory();
        if (!(rawView instanceof org.bukkit.inventory.view.MerchantView merchantView)) return;
        org.bukkit.event.inventory.TradeSelectEvent event =
                new org.bukkit.event.inventory.TradeSelectEvent(merchantView, packet.getItem());
        LunarArcServerAccess.getCraftServer(this.player.server).getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            this.player.containerMenu.broadcastFullState();
            ci.cancel();
        }
    }

    @Inject(method = "handleContainerClose", at = @At("HEAD"), cancellable = true, require = 0)
    private void lunararc$onContainerClose(ServerboundContainerClosePacket packet, CallbackInfo ci) {
        if (!this.player.server.isSameThread()) {
            this.player.server.execute(() -> ((ServerGamePacketListenerImpl) (Object) this).handleContainerClose(packet));
            ci.cancel();
            return;
        }
        if (this.player.containerMenu == this.player.inventoryMenu) {
            return;
        }
        if (this.player.containerMenu.containerId != packet.getContainerId()) {
            return;
        }
        ((io.ampznetwork.lunararc.common.bridge.ServerPlayerInventoryBridge) this.player)
                .lunararc$setNextInventoryCloseReason(org.bukkit.event.inventory.InventoryCloseEvent.Reason.PLAYER);
    }



}
