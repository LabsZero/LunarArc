package io.lunararcdevs.lunararc.common.mixin.core.world;

import io.lunararcdevs.lunararc.common.bridge.EntityBridge;
import io.papermc.paper.adventure.PaperAdventure;
import net.minecraft.server.network.FilteredText;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;
import org.bukkit.block.sign.Side;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.block.CraftSign;
import org.bukkit.event.block.SignChangeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Mixin(SignBlockEntity.class)
public abstract class SignBlockEntityMixin {
    public boolean isFacingFrontText(double x, double z) {
        SignBlockEntity self = (SignBlockEntity) (Object) this;
        if (self.getBlockState().getBlock() instanceof net.minecraft.world.level.block.SignBlock sign) {
            net.minecraft.world.phys.Vec3 center = sign.getSignHitboxCenterPosition(self.getBlockState());
            double dx = x - (self.getBlockPos().getX() + center.x);
            double dz = z - (self.getBlockPos().getZ() + center.z);
            float facing = sign.getYRotationDegrees(self.getBlockState());
            float toPoint = (float) (net.minecraft.util.Mth.atan2(dz, dx) * 57.2957763671875D) - 90.0F;
            return net.minecraft.util.Mth.degreesDifferenceAbs(facing, toPoint) <= 90.0F;
        }
        return false;
    }

    @Unique private SignText lunararc$originalText;

    @Inject(method = "updateSignText", at = @At("HEAD"))
    private void lunararc$captureOriginal(Player player, SignTextSlot slot, List<FilteredText> lines, CallbackInfo ci) {
        this.lunararc$originalText = ((SignBlockEntity) (Object) this).getText(slot);
    }

    @Inject(method = "updateSignText", at = @At("RETURN"))
    private void lunararc$fireSignChange(Player player, SignTextSlot slot, List<FilteredText> filtered, CallbackInfo ci) {
        SignText original = this.lunararc$originalText;
        this.lunararc$originalText = null;
        SignBlockEntity sign = (SignBlockEntity) (Object) this;
        if (original == null || !(player instanceof ServerPlayer) || !(sign.getLevel() instanceof ServerLevel level)
                || !(((EntityBridge) player).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Player bukkitPlayer)) {
            return;
        }
        SignText text = sign.getText(slot);
        if (text == original) return;
        List<net.kyori.adventure.text.Component> lines = new ArrayList<>();
        for (int i = 0; i < SignText.LINES; i++) {
            lines.add(PaperAdventure.asAdventure(text.getMessages(player.isTextFilteringEnabled()).get(i)));
        }
        SignChangeEvent event = new SignChangeEvent(CraftBlock.at(level, sign.getBlockPos()), bukkitPlayer,
                new ArrayList<>(lines), slot == SignTextSlot.FRONT ? Side.FRONT : Side.BACK);
        bukkitPlayer.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            sign.setText(original, slot);
            return;
        }
        net.minecraft.network.chat.Component[] components = CraftSign.sanitizeLines(event.lines());
        boolean changed = false;
        for (int i = 0; i < components.length; i++) {
            if (!Objects.equals(lines.get(i), event.line(i))) {
                text = text.asMutable().setLine(i, components[i]).asImmutable();
                changed = true;
            }
        }
        if (changed) sign.setText(text, slot);
    }
}
