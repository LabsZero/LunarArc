package io.ampznetwork.lunararc.common.server;

import io.ampznetwork.lunararc.common.config.LunarArcConfig;
import io.ampznetwork.lunararc.common.telemetry.BlockMedicReporter;
import io.ampznetwork.lunararc.i18n.TranslationManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Registers LunarArc's built-in server commands into the Bukkit command map. */
public final class LunarArcBuiltinCommands {

    private LunarArcBuiltinCommands() {}

    public static void register(org.bukkit.Server server) {
        try {
            server.getCommandMap().register("lunararc", "lunararc", new LunarArcRootCommand());
        } catch (Throwable t) {
            org.slf4j.LoggerFactory.getLogger("LunarArc")
                    .warn("[LunarArc] Could not register built-in commands: {}", t.getMessage());
        }
    }

    private static final class LunarArcRootCommand extends Command {

        LunarArcRootCommand() {
            super("lunararc",
                    "LunarArc server management commands",
                    "/lunararc upload <crash|log>",
                    List.of());
            setPermission("lunararc.admin");
        }

        private static final List<String> SUB_COMMANDS = List.of("upload");
        private static final List<String> UPLOAD_MODES = List.of("crash", "log");
        private static final List<String> CONSENT_ARGS = List.of("yes", "no");

        @Override
        public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
            if (!sender.isOp()) return List.of();
            List<String> completions = new ArrayList<>();
            if (args.length == 1) {
                String partial = args[0].toLowerCase();
                for (String s : SUB_COMMANDS) {
                    if (s.startsWith(partial)) completions.add(s);
                }
            } else if (args.length == 2 && args[0].equalsIgnoreCase("upload")) {
                String partial = args[1].toLowerCase();
                for (String s : UPLOAD_MODES) {
                    if (s.startsWith(partial)) completions.add(s);
                }
            } else if (args.length == 3 && args[0].equalsIgnoreCase("upload")
                    && LunarArcConfig.getBlockMedicConsent().equals("unset")) {
                String partial = args[2].toLowerCase();
                for (String s : CONSENT_ARGS) {
                    if (s.startsWith(partial)) completions.add(s);
                }
            }
            return completions;
        }

        @Override
        public boolean execute(CommandSender sender, String commandLabel, String[] args) {
            if (!sender.isOp()) {
                sender.sendMessage("§cYou must be an operator to use this command.");
                return true;
            }

            if (args.length == 0) {
                sender.sendMessage("§6LunarArc §7— usage: §f/lunararc upload <crash|log>");
                return true;
            }

            return switch (args[0].toLowerCase()) {
                case "upload" -> handleUpload(sender, args);
                default -> {
                    sender.sendMessage("§cUnknown sub-command. Usage: §f/lunararc upload <crash|log>");
                    yield true;
                }
            };
        }

        private boolean handleUpload(CommandSender sender, String[] args) {
            if (!LunarArcConfig.isBlockMedicEnabled()) {
                sender.sendMessage(TranslationManager.get("blockmedic.disabled"));
                return true;
            }

            // Consent gate — required on first use.
            String consent = LunarArcConfig.getBlockMedicConsent();
            if ("declined".equals(consent)) {
                sender.sendMessage(TranslationManager.get("blockmedic.declined"));
                return true;
            }
            if ("unset".equals(consent)) {
                if (args.length >= 3) {
                    String answer = args[2].toLowerCase();
                    if (answer.equals("yes")) {
                        LunarArcConfig.setBlockMedicConsent("accepted");
                        sender.sendMessage(TranslationManager.get("blockmedic.consent.accepted"));
                        // Fall through to upload logic below
                    } else if (answer.equals("no")) {
                        LunarArcConfig.setBlockMedicConsent("declined");
                        sender.sendMessage(TranslationManager.get("blockmedic.consent.rejected"));
                        return true;
                    } else {
                        sender.sendMessage(TranslationManager.get("blockmedic.consent.invalid"));
                        return true;
                    }
                } else {
                    String mode = args.length >= 2 ? args[1] : "crash";
                    String consentCmd = "/lunararc upload " + mode;
                    sender.sendMessage(Component
                            .text(TranslationManager.get("blockmedic.consent.title"))
                            .color(NamedTextColor.GOLD));
                    sender.sendMessage(TranslationManager.get("blockmedic.consent.body"));
                    sender.sendMessage(TranslationManager.get("blockmedic.consent.prompt", consentCmd));
                    return true;
                }
            }

            // Default: upload crash report; explicit "log" uploads latest.log
            boolean uploadCrash = args.length < 2 || args[1].equalsIgnoreCase("crash");

            if (uploadCrash) {
                Path crash = BlockMedicReporter.findCrashReport();
                if (crash == null) {
                    sender.sendMessage(TranslationManager.get("blockmedic.upload.no_crash"));
                    return true;
                }
                sender.sendMessage(TranslationManager.get("blockmedic.upload.uploading_crash", crash.getFileName()));
                Thread.ofVirtual().name("lunararc-blockmedic-cmd").start(() -> {
                    String url = BlockMedicReporter.uploadFileNow(crash, "manual-crash-upload");
                    if (url != null) {
                        sender.sendMessage(Component
                                .text(TranslationManager.get("blockmedic.upload.crash_success"))
                                .color(NamedTextColor.GREEN)
                                .append(Component.text(url)
                                        .color(NamedTextColor.AQUA)
                                        .clickEvent(ClickEvent.openUrl(url))
                                        .hoverEvent(HoverEvent.showText(
                                                Component.text(TranslationManager.get("blockmedic.hover.open_browser"))))));
                    } else {
                        sender.sendMessage(TranslationManager.get("blockmedic.upload.crash_failed"));
                    }
                });
            } else {
                sender.sendMessage(TranslationManager.get("blockmedic.upload.uploading_log"));
                Thread.ofVirtual().name("lunararc-blockmedic-cmd").start(() -> {
                    String url = BlockMedicReporter.uploadLogNow("manual-log-upload");
                    if (url != null) {
                        sender.sendMessage(Component
                                .text(TranslationManager.get("blockmedic.upload.log_success"))
                                .color(NamedTextColor.GREEN)
                                .append(Component.text(url)
                                        .color(NamedTextColor.AQUA)
                                        .clickEvent(ClickEvent.openUrl(url))
                                        .hoverEvent(HoverEvent.showText(
                                                Component.text(TranslationManager.get("blockmedic.hover.open_browser"))))));
                    } else {
                        sender.sendMessage(TranslationManager.get("blockmedic.upload.log_failed"));
                    }
                });
            }
            return true;
        }
    }
}
