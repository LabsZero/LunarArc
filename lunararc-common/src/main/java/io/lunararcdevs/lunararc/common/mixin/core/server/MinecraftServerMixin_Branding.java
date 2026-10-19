package io.lunararcdevs.lunararc.common.mixin.core.server;

import io.lunararcdevs.lunararc.common.server.LunarArcVersionInfo;
import net.minecraft.SystemReport;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin_Branding {

    private static final Logger LUNARARC_BRANDING_LOGGER = LoggerFactory.getLogger("LunarArc");

    @Inject(method = "fillSystemReport", at = @At("RETURN"), require = 0)
    private void lunararc$brandSystemReport(SystemReport report, CallbackInfoReturnable<SystemReport> cir) {
        report.setDetail("LunarArc", LunarArcVersionInfo::brandingLine);
        report.setDetail("LunarArc Paper API", LunarArcVersionInfo::paperApiVersion);
    }

    @Inject(method = "runServer", at = @At("HEAD"), require = 0)
    private void lunararc$announceBuild(CallbackInfo ci) {
        LUNARARC_BRANDING_LOGGER.info("{}", LunarArcVersionInfo.brandingLine());
        LUNARARC_BRANDING_LOGGER.info("This build is named \"{}\"", LunarArcVersionInfo.buildName());
    }

    private static boolean lunararc$auditDone;

    /**
     * Developer switch: mixins only apply to a class once it loads, so a quiet boot leaves most gameplay mixins
     * unchecked. -Dlunararc.auditMixins=true forces every mixin target to load on the first tick (after the Bukkit
     * layer exists) and reports failures. Targets that were already loaded by the audit's own ordering are reported
     * as "already classloaded" and are not real failures.
     */
    @Inject(method = "tickServer", at = @At("HEAD"), require = 0)
    private void lunararc$auditMixins(java.util.function.BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        if (lunararc$auditDone || !Boolean.getBoolean("lunararc.auditMixins")) return;
        lunararc$auditDone = true;
        try {
            org.spongepowered.asm.mixin.MixinEnvironment.getCurrentEnvironment().audit();
            LUNARARC_BRANDING_LOGGER.info("Mixin audit finished: every mixin target loaded and applied");
        } catch (Throwable failure) {
            LUNARARC_BRANDING_LOGGER.error("Mixin audit FAILED", failure);
        }
        try {
            LUNARARC_BRANDING_LOGGER.info("Adventure check: gson={} legacy={} plain={} mini={}",
                    net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson().getClass().getName(),
                    net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().getClass().getName(),
                    net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().getClass().getName(),
                    net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().getClass().getName());
        } catch (Throwable failure) {
            LUNARARC_BRANDING_LOGGER.error("Adventure check FAILED", failure);
        }
        try {
            net.kyori.adventure.text.Component probe = net.kyori.adventure.text.Component.text("probe");
            LUNARARC_BRANDING_LOGGER.info("PaperAdventure check: json={} plain={} vanilla={}",
                    io.papermc.paper.adventure.PaperAdventure.asJsonString(probe, java.util.Locale.ROOT),
                    io.papermc.paper.adventure.PaperAdventure.asPlain(probe, java.util.Locale.ROOT),
                    io.papermc.paper.adventure.PaperAdventure.asVanilla(probe).getString());
        } catch (Throwable failure) {
            LUNARARC_BRANDING_LOGGER.error("PaperAdventure check FAILED", failure);
        }
        try {
            Object bed = org.bukkit.craftbukkit.block.data.CraftBlockData.createData(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.parse("minecraft:red_bed")).defaultBlockState());
            LUNARARC_BRANDING_LOGGER.info("Block data check: white_bed -> {} (typed Bed: {})", bed.getClass().getSimpleName(), bed instanceof org.bukkit.block.data.type.Bed);
        } catch (Throwable t) {
            LUNARARC_BRANDING_LOGGER.error("Block data check failed", t);
        }
        io.lunararcdevs.lunararc.common.util.LunarArcClassProbe.run(LUNARARC_BRANDING_LOGGER);
        // Classes that were already loaded when the audit reached them cannot be audited; check them for Mixin's marker.
        for (String name : new String[]{"net.kyori.adventure.text.serializer.gson.GsonComponentSerializer",
                "net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer",
                "net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer",
                "net.kyori.adventure.text.minimessage.MiniMessage", "com.mojang.brigadier.tree.CommandNode"}) {
            try {
                Class<?> type = Class.forName(name, false, MinecraftServerMixin_Branding.class.getClassLoader());
                boolean merged = false;
                for (java.lang.reflect.Method method : type.getDeclaredMethods()) {
                    for (java.lang.annotation.Annotation annotation : method.getDeclaredAnnotations()) {
                        if (annotation.annotationType().getSimpleName().equals("MixinMerged")) merged = true;
                    }
                }
                LUNARARC_BRANDING_LOGGER.info("Mixin check {}: {}", name, merged ? "mixin merged" : "NO merged mixin methods found");
            } catch (Throwable ignored) {
                LUNARARC_BRANDING_LOGGER.info("Mixin check {}: not loadable from here", name);
            }
        }
    }
}
