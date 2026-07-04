package org.lunatic.athenaTrades.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.lunatic.athenaTrades.Main;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class TradeCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;

    public TradeCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Command ini hanya bisa dijalankan oleh player.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.YELLOW + "Gunakan: /trade <player> | accept | deny | block <player> | unblock <player> | blocklist");
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "accept" -> plugin.getTradeManager().acceptRequest(player);
            case "deny" -> plugin.getTradeManager().denyRequest(player);
            case "block" -> {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Gunakan: /trade block <player>");
                    return true;
                }
                handleBlock(player, args[1]);
            }
            case "unblock" -> {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Gunakan: /trade unblock <player>");
                    return true;
                }
                handleUnblock(player, args[1]);
            }
            case "blocklist" -> handleBlocklist(player);
            default -> {
                Player target = Bukkit.getPlayerExact(args[0]);
                if (target == null || !target.isOnline()) {
                    player.sendMessage(ChatColor.RED + "Player tidak ditemukan atau sedang offline.");
                    return true;
                }
                plugin.getTradeManager().sendRequest(player, target);
            }
        }
        return true;
    }

    private void handleBlock(Player player, String targetName) {
        UUID targetId = resolveOfflineOrOnline(targetName);
        if (targetId == null) {
            player.sendMessage(ChatColor.RED + "Player tidak ditemukan.");
            return;
        }
        plugin.getBlacklistManager().block(player.getUniqueId(), targetId, targetName);
        player.sendMessage(ChatColor.GREEN + targetName + " telah diblokir dari trade.");
    }

    private void handleUnblock(Player player, String targetName) {
        UUID targetId = resolveOfflineOrOnline(targetName);
        if (targetId == null) {
            player.sendMessage(ChatColor.RED + "Player tidak ditemukan.");
            return;
        }
        boolean removed = plugin.getBlacklistManager().unblock(player.getUniqueId(), targetId);
        player.sendMessage(removed
                ? ChatColor.GREEN + targetName + " telah di-unblock."
                : ChatColor.RED + targetName + " tidak ada di blocklist kamu.");
    }

    private void handleBlocklist(Player player) {
        List<String> names = plugin.getBlacklistManager().getBlockedNames(player.getUniqueId());
        if (names.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "Blocklist kamu kosong.");
            return;
        }
        player.sendMessage(ChatColor.YELLOW + "Blocklist kamu: " + ChatColor.WHITE + String.join(", ", names));
    }

    private UUID resolveOfflineOrOnline(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online.getUniqueId();

        @SuppressWarnings("deprecation")
        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        return offline.hasPlayedBefore() ? offline.getUniqueId() : null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(List.of("accept", "deny", "block", "unblock", "blocklist"));
            for (Player p : Bukkit.getOnlinePlayers()) options.add(p.getName());

            String input = args[0].toLowerCase();
            return options.stream().filter(o -> o.toLowerCase().startsWith(input)).collect(Collectors.toList());
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("block") || args[0].equalsIgnoreCase("unblock"))) {
            String input = args[1].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(input))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}