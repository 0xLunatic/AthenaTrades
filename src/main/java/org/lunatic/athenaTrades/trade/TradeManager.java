package org.lunatic.athenaTrades.trade;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.lunatic.athenaTrades.Main;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TradeManager {

    private final Main plugin;

    // target -> request masuk yang menunggu di-accept/deny
    private final Map<UUID, TradeRequest> pendingRequests = new HashMap<>();
    // playerId -> session aktif yang sedang dia ikuti
    private final Map<UUID, TradeSession> activeSessions = new HashMap<>();

    public TradeManager(Main plugin) {
        this.plugin = plugin;
    }

    public boolean sendRequest(Player sender, Player target) {
        if (sender.getUniqueId().equals(target.getUniqueId())) {
            sender.sendMessage(ChatColor.RED + "Kamu tidak bisa trade dengan diri sendiri.");
            return false;
        }

        if (isInTrade(sender) || isInTrade(target)) {
            sender.sendMessage(ChatColor.RED + "Salah satu dari kalian sedang dalam trade lain.");
            return false;
        }

        if (plugin.getBlacklistManager().isBlocked(sender.getUniqueId(), target.getUniqueId())) {
            sender.sendMessage(ChatColor.RED + "Kamu tidak bisa trade dengan player ini.");
            return false;
        }

        pendingRequests.put(target.getUniqueId(), new TradeRequest(sender.getUniqueId(), target.getUniqueId()));

        sender.sendMessage(ChatColor.YELLOW + "Trade request dikirim ke " + target.getName() + ".");
        target.sendMessage(ChatColor.YELLOW + sender.getName() + " ingin trade denganmu. Ketik "
                + ChatColor.GOLD + "/trade accept" + ChatColor.YELLOW + " untuk menerima, atau "
                + ChatColor.GOLD + "/trade deny" + ChatColor.YELLOW + " untuk menolak.");
        return true;
    }

    public void acceptRequest(Player target) {
        TradeRequest request = pendingRequests.get(target.getUniqueId());
        if (request == null || request.isExpired()) {
            pendingRequests.remove(target.getUniqueId());
            target.sendMessage(ChatColor.RED + "Tidak ada trade request yang aktif.");
            return;
        }

        Player sender = plugin.getServer().getPlayer(request.getSender());
        if (sender == null || !sender.isOnline()) {
            target.sendMessage(ChatColor.RED + "Player pengirim sudah offline.");
            pendingRequests.remove(target.getUniqueId());
            return;
        }

        if (isInTrade(sender) || isInTrade(target)) {
            target.sendMessage(ChatColor.RED + "Salah satu dari kalian sudah dalam trade lain.");
            pendingRequests.remove(target.getUniqueId());
            return;
        }

        pendingRequests.remove(target.getUniqueId());
        startTrade(sender, target);
    }

    public void denyRequest(Player target) {
        TradeRequest request = pendingRequests.remove(target.getUniqueId());
        if (request == null) {
            target.sendMessage(ChatColor.RED + "Tidak ada trade request yang aktif.");
            return;
        }
        Player sender = plugin.getServer().getPlayer(request.getSender());
        target.sendMessage(ChatColor.YELLOW + "Trade request ditolak.");
        if (sender != null) {
            sender.sendMessage(ChatColor.YELLOW + target.getName() + " menolak trade request kamu.");
        }
    }

    private void startTrade(Player player1, Player player2) {
        TradeSession session = new TradeSession(player1, player2);
        activeSessions.put(player1.getUniqueId(), session);
        activeSessions.put(player2.getUniqueId(), session);

        player1.openInventory(session.getInventory());
        player2.openInventory(session.getInventory());

        player1.sendMessage(ChatColor.GREEN + "Trade dengan " + player2.getName() + " dimulai.");
        player2.sendMessage(ChatColor.GREEN + "Trade dengan " + player1.getName() + " dimulai.");
    }

    public boolean isInTrade(Player player) {
        return activeSessions.containsKey(player.getUniqueId());
    }

    public TradeSession getSession(Player player) {
        return activeSessions.get(player.getUniqueId());
    }

    public void completeTrade(TradeSession session) {
        Player p1 = plugin.getServer().getPlayer(session.getPlayer1Id());
        Player p2 = plugin.getServer().getPlayer(session.getPlayer2Id());

        if (p1 == null || p2 == null) {
            cancelTrade(session, "Salah satu player offline, trade dibatalkan.");
            return;
        }

        List<ItemStack> p1Items = session.getPlayer1Items();
        List<ItemStack> p2Items = session.getPlayer2Items();

        if (!hasSpaceFor(p1, p2Items) || !hasSpaceFor(p2, p1Items)) {
            p1.sendMessage(ChatColor.RED + "Trade dibatalkan: inventory salah satu pihak penuh.");
            p2.sendMessage(ChatColor.RED + "Trade dibatalkan: inventory salah satu pihak penuh.");
            cancelTrade(session, null);
            return;
        }

        session.clearTradeSlots();
        session.setCompleted(true);

        endSession(session);

        p2Items.forEach(item -> p1.getInventory().addItem(item));
        p1Items.forEach(item -> p2.getInventory().addItem(item));

        p1.closeInventory();
        p2.closeInventory();

        p1.sendMessage(ChatColor.GREEN + "Trade berhasil!");
        p2.sendMessage(ChatColor.GREEN + "Trade berhasil!");
    }

    private boolean hasSpaceFor(Player player, List<ItemStack> items) {
        int freeSlots = 0;
        for (ItemStack slot : player.getInventory().getStorageContents()) {
            if (slot == null || slot.getType().isAir()) {
                freeSlots++;
            }
        }
        return freeSlots >= items.size();
    }

    public void cancelTrade(TradeSession session, String reasonMessage) {
        if (session.isCompleted()) return;

        Player p1 = plugin.getServer().getPlayer(session.getPlayer1Id());
        Player p2 = plugin.getServer().getPlayer(session.getPlayer2Id());

        // Hapus session TERLEBIH DAHULU agar InventoryCloseEvent berikutnya tidak memproses lagi
        endSession(session);

        // Kembalikan item
        returnItems(p1, session.getPlayer1Items());
        returnItems(p2, session.getPlayer2Items());

        session.clearTradeSlots();
        session.setCompleted(true);

        if (reasonMessage != null) {
            if (p1 != null) p1.sendMessage(ChatColor.RED + reasonMessage);
            if (p2 != null) p2.sendMessage(ChatColor.RED + reasonMessage);
        }

        if (p1 != null && p1.getOpenInventory().getTopInventory().getHolder() instanceof TradeGUIHolder) {
            p1.closeInventory();
        }

        if (p2 != null && p2.getOpenInventory().getTopInventory().getHolder() instanceof TradeGUIHolder) {
            p2.closeInventory();
        }
    }

    private void returnItems(Player player, List<ItemStack> items) {
        if (player == null) return;
        for (ItemStack item : items) {
            var leftover = player.getInventory().addItem(item);
            leftover.values().forEach(extra -> player.getWorld().dropItemNaturally(player.getLocation(), extra));
        }
    }

    private void endSession(TradeSession session) {
        activeSessions.remove(session.getPlayer1Id());
        activeSessions.remove(session.getPlayer2Id());
    }

    public void handlePlayerQuit(Player player) {
        pendingRequests.remove(player.getUniqueId());
        TradeSession session = activeSessions.get(player.getUniqueId());
        if (session != null) {
            cancelTrade(session, "Trade dibatalkan karena " + player.getName() + " keluar.");
        }
    }

    public void shutdown() {
        for (TradeSession session : new HashSet<>(activeSessions.values())) {
            cancelTrade(session, "Trade dibatalkan: server sedang restart/shutdown.");
        }
        pendingRequests.clear();
        activeSessions.clear();
    }
}