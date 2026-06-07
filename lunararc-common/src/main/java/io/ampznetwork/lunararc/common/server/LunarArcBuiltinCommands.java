package io.ampznetwork.lunararc.common.server;

import io.ampznetwork.lunararc.common.config.LunarArcConfig;
import io.ampznetwork.lunararc.common.telemetry.BlockMedicReporter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.nio.file.Path;
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
                sender.sendMessage("§c[BlockMedic] Log upload is disabled. "
                        + "Set §fenable_blockmedic=true §cin §flunararc.conf §cto enable it.");
                return true;
            }

            // Default: upload crash report; explicit "log" uploads latest.log
            boolean uploadCrash = args.length < 2 || args[1].equalsIgnoreCase("crash");

            if (uploadCrash) {
                Path crash = BlockMedicReporter.findCrashReport();
                if (crash == null) {
                    sender.sendMessage("§e[BlockMedic] No crash report found in crash-reports/. "
                            + "Use §f/lunararc upload log §eto upload latest.log instead.");
                    return true;
                }
                sender.sendMessage("§7[BlockMedic] Uploading crash report §f" + crash.getFileName() + "§7...");
                Thread.ofVirtual().name("lunararc-blockmedic-cmd").start(() -> {
                    String url = BlockMedicReporter.uploadFileNow(crash, "manual-crash-upload");
                    if (url != null) {
                        sender.sendMessage("§a[BlockMedic] Crash report uploaded. View at: §f" + url);
                    } else {
                        sender.sendMessage("§c[BlockMedic] Upload failed. Check console for details.");
                    }
                });
            } else {
                // Explicit "log" mode
                sender.sendMessage("§7[BlockMedic] Uploading latest.log...");
                Thread.ofVirtual().name("lunararc-blockmedic-cmd").start(() -> {
                    String url = BlockMedicReporter.uploadLogNow("manual-log-upload");
                    if (url != null) {
                        sender.sendMessage("§a[BlockMedic] Log uploaded. View at: §f" + url);
                    } else {
                        sender.sendMessage("§c[BlockMedic] Upload failed — no log file found or upload error. "
                                + "Check console for details.");
                    }
                });
            }
            return true;
        }
    }
}
