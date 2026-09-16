package org.lunatic.athenaTrades.trade;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Satu sesi trade antara dua player, berikut GUI 54-slot yang dipakai
 * bersama oleh keduanya.
 *
 * Layout (index slot 0-53):
 *  - Baris 0        : dekorasi (glass pane)
 *  - Baris 1-3      : slot barang. Kolom 0-3 milik player1, kolom 5-8 milik player2,
 *                      kolom 4 jadi divider (glass pane merah)
 *  - Baris 4 (45-53): slot 37 = tombol uang player1, slot 43 = tombol uang player2,
 *                      slot 48/50 = tombol "ready" masing-masing
 */
public class TradeSession {

    public static final int[] PLAYER1_SLOTS = {9, 10, 11, 12, 18, 19, 20, 21, 27, 28, 29, 30};
    public static final int[] PLAYER2_SLOTS = {14, 15, 16, 17, 23, 24, 25, 26, 32, 33, 34, 35};
    public static final int PLAYER1_READY_SLOT = 48;
    public static final int PLAYER2_READY_SLOT = 50;
    public static final int PLAYER1_MONEY_SLOT = 37;
    public static final int PLAYER2_MONEY_SLOT = 43;

    private static final int[] DIVIDER_SLOTS = {13, 22, 31};
    private static final int[] FILLER_SLOTS = {
            0, 1, 2, 3, 4, 5, 6, 7, 8,
            36, 38, 39, 40, 41, 42, 44,
            45, 46, 47, 49, 51, 52, 53
    };

    private final UUID player1Id;
    private final UUID player2Id;
    private final Inventory inventory;
    private final TradeGUIHolder holder;

    private boolean player1Ready = false;
    private boolean player2Ready = false;
    private boolean completed = false;

    private double player1Money = 0;
    private double player2Money = 0;

    public TradeSession(Player player1, Player player2) {
        this.player1Id = player1.getUniqueId();
        this.player2Id = player2.getUniqueId();
        this.holder = new TradeGUIHolder(this);
        this.inventory = Bukkit.createInventory(holder, 54,
                ChatColor.DARK_GREEN + "Trade: " + player1.getName() + " <-> " + player2.getName());
        holder.setInventory(inventory);
        setupDecoration();
    }

    private void setupDecoration() {
        ItemStack pane = namedItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int slot : FILLER_SLOTS) {
            inventory.setItem(slot, pane);
        }

        ItemStack divider = namedItem(Material.RED_STAINED_GLASS_PANE, " ");
        for (int slot : DIVIDER_SLOTS) {
            inventory.setItem(slot, divider);
        }

        refreshReadyButtons();
        refreshMoneyButtons();
    }

    private ItemStack namedItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void refreshReadyButtons() {
        ItemStack notReady = namedItem(Material.RED_WOOL, ChatColor.RED + "Klik untuk siap");
        ItemStack ready = namedItem(Material.LIME_WOOL, ChatColor.GREEN + "Siap!");

        inventory.setItem(PLAYER1_READY_SLOT, player1Ready ? ready : notReady);
        inventory.setItem(PLAYER2_READY_SLOT, player2Ready ? ready : notReady);
    }

    public void refreshMoneyButtons() {
        inventory.setItem(PLAYER1_MONEY_SLOT, buildMoneyIcon(player1Money));
        inventory.setItem(PLAYER2_MONEY_SLOT, buildMoneyIcon(player2Money));
    }

    private ItemStack buildMoneyIcon(double amount) {
        String amountText = String.format("%,.2f", amount);
        if (amount <= 0) {
            ItemStack item = namedItem(Material.GOLD_NUGGET, ChatColor.YELLOW + "Klik untuk menawarkan uang");
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setLore(List.of(ChatColor.GRAY + "Belum ada uang ditawarkan"));
                item.setItemMeta(meta);
            }
            return item;
        }
        ItemStack item = namedItem(Material.GOLD_INGOT, ChatColor.GOLD + "Rp " + amountText);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setLore(List.of(
                    ChatColor.GRAY + "Klik untuk ubah jumlah",
                    ChatColor.DARK_GRAY + "Uang ini sudah ditahan (escrow)"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isMoneySlot(int slot, UUID playerId) {
        if (slot == PLAYER1_MONEY_SLOT && playerId.equals(player1Id)) return true;
        return slot == PLAYER2_MONEY_SLOT && playerId.equals(player2Id);
    }

    public double getMoney(UUID playerId) {
        if (playerId.equals(player1Id)) return player1Money;
        if (playerId.equals(player2Id)) return player2Money;
        return 0;
    }

    /**
     * Set nilai uang yang ditampilkan di GUI untuk player tertentu.
     * Penarikan/pengembalian saldo lewat economy TIDAK dilakukan di sini -
     * itu tanggung jawab TradeManager supaya TradeSession tetap murni soal state GUI.
     */
    public void setMoney(UUID playerId, double amount) {
        if (playerId.equals(player1Id)) {
            player1Money = amount;
        } else if (playerId.equals(player2Id)) {
            player2Money = amount;
        }
        refreshMoneyButtons();
    }

    public double getPlayer1Money() {
        return player1Money;
    }

    public double getPlayer2Money() {
        return player2Money;
    }

    public boolean isPlayerSlot(int slot, UUID playerId) {
        int[] slots;
        if (playerId.equals(player1Id)) {
            slots = PLAYER1_SLOTS;
        } else if (playerId.equals(player2Id)) {
            slots = PLAYER2_SLOTS;
        } else {
            return false;
        }
        for (int s : slots) {
            if (s == slot) return true;
        }
        return false;
    }

    private boolean cancelled = false;

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public boolean isReadySlot(int slot, UUID playerId) {
        if (slot == PLAYER1_READY_SLOT && playerId.equals(player1Id)) return true;
        return slot == PLAYER2_READY_SLOT && playerId.equals(player2Id);
    }

    public void toggleReady(UUID playerId) {
        if (playerId.equals(player1Id)) {
            player1Ready = !player1Ready;
        } else if (playerId.equals(player2Id)) {
            player2Ready = !player2Ready;
        }
        refreshReadyButtons();
    }

    /**
     * Dipanggil setiap kali salah satu pihak mengubah barang di slotnya.
     * Kalau ada yang sudah pernah klik ready, batalkan ready keduanya
     * supaya tidak ada yang bisa "menipu" dengan mengganti barang di detik terakhir.
     */
    public void resetReadyOnEdit(UUID editorId) {
        if (player1Ready || player2Ready) {
            player1Ready = false;
            player2Ready = false;
            refreshReadyButtons();
        }
    }

    public boolean bothReady() {
        return player1Ready && player2Ready;
    }

    public boolean isPlayer1Ready() {
        return player1Ready;
    }

    public boolean isPlayer2Ready() {
        return player2Ready;
    }

    public List<ItemStack> getPlayer1Items() {
        return getItemsFromSlots(PLAYER1_SLOTS);
    }

    public List<ItemStack> getPlayer2Items() {
        return getItemsFromSlots(PLAYER2_SLOTS);
    }

    private List<ItemStack> getItemsFromSlots(int[] slots) {
        List<ItemStack> items = new ArrayList<>();
        for (int slot : slots) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                items.add(item.clone());
            }
        }
        return items;
    }

    public void clearTradeSlots() {
        for (int slot : PLAYER1_SLOTS) inventory.setItem(slot, null);
        for (int slot : PLAYER2_SLOTS) inventory.setItem(slot, null);
    }

    public UUID getPlayer1Id() {
        return player1Id;
    }

    public UUID getPlayer2Id() {
        return player2Id;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public UUID getOtherPlayer(UUID playerId) {
        return playerId.equals(player1Id) ? player2Id : player1Id;
    }

    public boolean involves(UUID playerId) {
        return playerId.equals(player1Id) || playerId.equals(player2Id);
    }
}