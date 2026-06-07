package io.ampznetwork.lunararc.common.server;

import io.ampznetwork.lunararc.common.config.LunarArcConfig;
import io.ampznetwork.lunararc.common.telemetry.BlockMedicReporter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;

import java.util.List;

/** Registers LunarArc's built-in server commands into the Bukkit command map. */
public final class LunarArcBuiltinCommands {

    private LunarArcBuiltinCommands() {}

    public static void register(org.bukkit.Server server) {
        try {
            var commandMap = server.getCommandMap();

            // /lunararc <subcommand>
            LunarArcRootCommand root = new LunarArcRootCommand();
            commandMap.register("lunararc", "lunararc", root);
        } catch (Throwable t) {
            // Non-fatal — best-effort only
            org.slf4j.LoggerFactory.getLogger("LunarArc")
                    .warn("[LunarArc] Could not register built-in commands: {}", t.getMessage());
        }
    }

    private static final class LunarArcRootCommand extends Command {

        LunarArcRootCommand() {
            super("lunararc",
                    "LunarArc server management commands",
                    "/lunararc <upload>",
                    List.of());
            setPermission("lunararc.admin");
        }

        @Override
        public boolean execute(CommandSender sender, String commandLabel, String[] args) {
            if (!sender.isOp()) {
                sender.sendMessage("§cYou must be an operator (OP level 4) to use this command.");
                return true;
            }

            if (args.length == 0) {
                sender.sendMessage("§6LunarArc §7— available sub-commands: §fupload");
                return true;
            }

            return switch (args[0].toLowerCase()) {
                case "upload" -> handleUpload(sender);
                default -> {
                    sender.sendMessage("§cUnknown sub-command. Usage: §f/lunararc upload");
                    yield true;
                }
            };
        }

        private boolean handleUpload(CommandSender sender) {
            if (!LunarArcConfig.isBlockMedicEnabled()) {
                sender.sendMessage("§c[BlockMedic] Log upload is disabled. "
                        + "Set §fenable_blockmedic=true §cin §flunararc.conf §cto enable it.");
                return true;
            }

            sender.sendMessage("§7[BlockMedic] Uploading latest log, please wait...");

            Thread.ofVirtual().name("lunararc-blockmedic-cmd").start(() -> {
                String url = BlockMedicReporter.uploadLogNow("manual-upload");
                if (url != null) {
                    sender.sendMessage("§a[BlockMedic] Log uploaded. View at: §f" + url);
                } else {
                    sender.sendMessage("§c[BlockMedic] Upload failed — no log file found or upload error. "
                            + "Check console for details (debug log shows paths tried).");
                }
            });
            return true;
        }
    }
}
