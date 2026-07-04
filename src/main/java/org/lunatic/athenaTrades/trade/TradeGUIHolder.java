package org.lunatic.athenaTrades.trade;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Dipakai sebagai "tanda" pada Inventory Bukkit supaya listener bisa
 * membedakan GUI trade kita dengan inventory/chest biasa milik player.
 */
public class TradeGUIHolder implements InventoryHolder {

    private final TradeSession session;
    private Inventory inventory;

    public TradeGUIHolder(TradeSession session) {
        this.session = session;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public TradeSession getSession() {
        return session;
    }
}