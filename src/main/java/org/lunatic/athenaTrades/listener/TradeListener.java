package org.lunatic.athenaTrades.listener;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.lunatic.athenaTrades.Main;
import org.lunatic.athenaTrades.trade.TradeGUIHolder;
import org.lunatic.athenaTrades.trade.TradeSession;

import java.util.UUID;

public class TradeListener implements Listener {

    private final Main plugin;

    public TradeListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof TradeGUIHolder tradeHolder)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        TradeSession session = tradeHolder.getSession();
        UUID playerId = player.getUniqueId();
        int rawSlot = event.getRawSlot();

        boolean clickedInOwnInventory = rawSlot >= event.getInventory().getSize();

        if (clickedInOwnInventory) {
            // Hanya izinkan shift-click untuk memasukkan barang ke slot trade miliknya sendiri
            if (event.isShiftClick()) {
                event.setCancelled(true);
                handleShiftClickIn(event, session, playerId);
            }
            return;
        }

        int slot = rawSlot;

        if (session.isReadySlot(slot, playerId)) {
            event.setCancelled(true);
            session.toggleReady(playerId);
            checkCompletion(session);
            return;
        }

        // Klik tombol ready lawan, atau area divider/filler -> selalu dibatalkan
        if (!session.isPlayerSlot(slot, playerId)) {
            event.setCancelled(true);
            return;
        }

        // Slot milik sendiri, tapi sudah ready -> kunci dulu
        if (isReady(session, playerId)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Batalkan status siap dulu sebelum mengubah barang.");
            return;
        }

        // Barang boleh diubah bebas, tapi reset status ready kedua pihak setelah event selesai
        plugin.getServer().getScheduler().runTask(plugin, () -> session.resetReadyOnEdit(playerId));
    }

    private void handleShiftClickIn(InventoryClickEvent event, TradeSession session, UUID playerId) {
        if (isReady(session, playerId)) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        int[] slots = playerId.equals(session.getPlayer1Id()) ? TradeSession.PLAYER1_SLOTS : TradeSession.PLAYER2_SLOTS;

        for (int slot : slots) {
            if (event.getInventory().getItem(slot) == null) {
                event.getInventory().setItem(slot, clicked.clone());
                event.getClickedInventory().setItem(event.getSlot(), null);
                session.resetReadyOnEdit(playerId);
                return;
            }
        }
        // Tidak ada slot kosong tersisa di sisi trade milik player ini, item tetap di inventorynya
    }

    private boolean isReady(TradeSession session, UUID playerId) {
        if (playerId.equals(session.getPlayer1Id())) return session.isPlayer1Ready();
        if (playerId.equals(session.getPlayer2Id())) return session.isPlayer2Ready();
        return false;
    }

    private void checkCompletion(TradeSession session) {
        if (session.bothReady()) {
            plugin.getTradeManager().completeTrade(session);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof TradeGUIHolder)) return;

        // Sederhana & aman: larang drag multi-slot ke bagian mana pun di GUI trade
        for (int slot : event.getRawSlots()) {
            if (slot < event.getInventory().getSize()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        TradeSession session = plugin.getTradeManager().getSession(player);

        // Sudah tidak ada session aktif
        if (session == null) {
            return;
        }

        // Sudah selesai
        if (session.isCompleted()) {
            return;
        }

        plugin.getTradeManager().cancelTrade(
                session,
                "Trade dibatalkan karena salah satu pihak menutup GUI."
        );
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getTradeManager().handlePlayerQuit(event.getPlayer());
    }
}