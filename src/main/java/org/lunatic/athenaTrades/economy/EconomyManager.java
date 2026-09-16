package org.lunatic.athenaTrades.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.lunatic.athenaTrades.Main;

/**
 * Wrapper tipis di atas Vault Economy API. Plugin ini TIDAK punya sistem
 * ekonomi sendiri - saldo player diambil dari plugin economy yang kamu
 * pakai (EssentialsX, CMI, dll) lewat Vault.
 */
public class EconomyManager {

    private Economy economy;

    public boolean setup(Main plugin) {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault tidak ditemukan. Fitur trade uang di AthenaTrades dinonaktifkan.");
            return false;
        }

        RegisteredServiceProvider<Economy> provider = plugin.getServer()
                .getServicesManager().getRegistration(Economy.class);

        if (provider == null) {
            plugin.getLogger().warning("Tidak ada economy provider terdaftar di Vault. Fitur trade uang dinonaktifkan.");
            return false;
        }

        this.economy = provider.getProvider();
        return true;
    }

    public double getBalance(OfflinePlayer player) {
        return economy.getBalance(player);
    }

    public boolean has(OfflinePlayer player, double amount) {
        return economy.has(player, amount);
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public boolean deposit(OfflinePlayer player, double amount) {
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    public String format(double amount) {
        return economy.format(amount);
    }
}