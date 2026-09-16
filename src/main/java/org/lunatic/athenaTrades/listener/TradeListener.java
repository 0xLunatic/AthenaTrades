package org.lunatic.athenaTrades.listener;

import org.bukkit.Bukkit;
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
import org.lunatic.athenaTrades.util.AmountParser;

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

        /*
         * READY BUTTON
         */
        if (session.isReadySlot(slot, playerId)) {
            event.setCancelled(true);
            session.toggleReady(playerId);
            checkCompletion(session);
            return;
        }

        /*
         * MONEY BUTTON
         */
        if (session.isMoneySlot(slot, playerId)) {
            event.setCancelled(true);

            player.sendMessage(ChatColor.RED + "Fitur Ini Belum Aktif");

            if (!plugin.isEconomyEnabled()) {
                player.sendMessage(ChatColor.RED + "Trade uang sedang dinonaktifkan.");
                return;
            }

            if (isReady(session, playerId)) {
                player.sendMessage(ChatColor.RED + "Batalkan status siap dulu sebelum mengubah nominal.");
                return;
            }

//            plugin.getIgnoreNextClose().add(player.getUniqueId());
//
//            player.closeInventory();

//            plugin.getSignInputManager().prompt(
//                    player,
//                    "Masukkan jumlah",
//                    "Contoh: 100k",
//                    input -> {
//
//                        Double amount = AmountParser.parse(input);
//
//                        if (amount == null) {
//                            player.sendMessage(ChatColor.RED + "Jumlah tidak valid.");
//                            return;
//                        }
//
//                        if (!plugin.getEconomyManager().has(player, amount)) {
//                            player.sendMessage(ChatColor.RED + "Saldo kamu tidak mencukupi.");
//                            return;
//                        }
//
//                        session.setMoney(playerId, amount);
//                        session.resetReadyOnEdit(playerId);
//
//                        Player other = plugin.getServer()
//                                .getPlayer(session.getOtherPlayer(playerId));
//
//                        player.updateInventory();
//
//                        if (other != null) {
//                            other.updateInventory();
//                            other.sendMessage(ChatColor.YELLOW
//                                    + player.getName()
//                                    + " mengubah nominal trade.");
//                        }
//
//                        player.sendMessage(ChatColor.GREEN
//                                + "Nominal trade diubah menjadi "
//                                + plugin.getEconomyManager().format(amount));
//
//                        Bukkit.getScheduler().runTask(plugin, () -> {
//
//                            player.openInventory(session.getInventory());
//
//                            if (other != null) {
//                                other.openInventory(session.getInventory());
//                            }
//
//                        });
//                    });
//
//            return;
        }

        /*
         * Divider / slot lawan
         */
        if (!session.isPlayerSlot(slot, playerId)) {
            event.setCancelled(true);
            return;
        }

        /*
         * Sudah ready?
         */
        if (isReady(session, playerId)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Batalkan status siap dulu sebelum mengubah barang.");
            return;
        }

        /*
         * Barang berubah -> reset ready
         */
        plugin.getServer().getScheduler().runTask(plugin,
                () -> session.resetReadyOnEdit(playerId));
    }

    private void handleShiftClickIn(InventoryClickEvent event, TradeSession session, UUID playerId) {

        if (isReady(session, playerId))
            return;

        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType().isAir())
            return;

        int[] slots = playerId.equals(session.getPlayer1Id())
                ? TradeSession.PLAYER1_SLOTS
                : TradeSession.PLAYER2_SLOTS;

        for (int slot : slots) {

            if (event.getInventory().getItem(slot) == null) {

                event.getInventory().setItem(slot, clicked.clone());
                event.getClickedInventory().setItem(event.getSlot(), null);

                session.resetReadyOnEdit(playerId);
                return;
            }
        }
    }

    private boolean isReady(TradeSession session, UUID playerId) {

        if (playerId.equals(session.getPlayer1Id()))
            return session.isPlayer1Ready();

        if (playerId.equals(session.getPlayer2Id()))
            return session.isPlayer2Ready();

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

        if (!(holder instanceof TradeGUIHolder))
            return;

        for (int slot : event.getRawSlots()) {

            if (slot < event.getInventory().getSize()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {

        InventoryHolder holder = event.getInventory().getHolder();

        if (!(holder instanceof TradeGUIHolder tradeHolder))
            return;

        Player player = (Player) event.getPlayer();

        if (plugin.getIgnoreNextClose().remove(player.getUniqueId())) {
            return;
        }

        TradeSession session = tradeHolder.getSession();

        if (!session.isCompleted()) {
            plugin.getTradeManager().cancelTrade(
                    session,
                    "Trade dibatalkan karena salah satu pihak menutup GUI."
            );
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getTradeManager().handlePlayerQuit(event.getPlayer());
    }
}