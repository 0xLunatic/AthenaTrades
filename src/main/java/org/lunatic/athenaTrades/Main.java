package org.lunatic.athenaTrades;

import org.bukkit.plugin.java.JavaPlugin;
import org.lunatic.athenaTrades.blacklist.BlacklistManager;
import org.lunatic.athenaTrades.command.TradeCommand;
import org.lunatic.athenaTrades.economy.EconomyManager;
import org.lunatic.athenaTrades.listener.SignInputListener;
import org.lunatic.athenaTrades.listener.TradeListener;
import org.lunatic.athenaTrades.trade.TradeManager;
import org.lunatic.athenaTrades.util.SignInputManager;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Main extends JavaPlugin {

    private static Main instance;

    private TradeManager tradeManager;
    private BlacklistManager blacklistManager;
    private EconomyManager economyManager;
    private SignInputManager signInputManager;


    private final Set<UUID> ignoreNextClose = ConcurrentHashMap.newKeySet();

    public Set<UUID> getIgnoreNextClose() {
        return ignoreNextClose;
    }

    @Override
    public void onEnable() {
        instance = this;

        this.blacklistManager = new BlacklistManager(this);
        this.blacklistManager.load();

        this.economyManager = new EconomyManager();
        boolean economyReady = economyManager.setup(this);
        if (!economyReady) {
            // Trade uang otomatis nonaktif kalau Vault/economy tidak ketemu,
            // tapi trade barang biasa tetap jalan seperti biasa.
            this.economyManager = null;
        }

        this.signInputManager = new SignInputManager(this);
        this.tradeManager = new TradeManager(this);

        TradeCommand tradeCommand = new TradeCommand(this);
        getCommand("trade").setExecutor(tradeCommand);
        getCommand("trade").setTabCompleter(tradeCommand);

        getServer().getPluginManager().registerEvents(new TradeListener(this), this);
        getServer().getPluginManager().registerEvents(new SignInputListener(this), this);

        getLogger().info("AthenaTrades enabled." + (economyReady ? " Trade uang aktif." : " Trade uang nonaktif (tidak ada Vault)."));
    }

    @Override
    public void onDisable() {
        if (tradeManager != null) {
            tradeManager.shutdown();
        }
        if (blacklistManager != null) {
            blacklistManager.save();
        }
        getLogger().info("AthenaTrades disabled.");
    }

    public static Main getInstance() {
        return instance;
    }

    public TradeManager getTradeManager() {
        return tradeManager;
    }

    public BlacklistManager getBlacklistManager() {
        return blacklistManager;
    }

    /**
     * Bisa null kalau Vault/economy provider tidak ditemukan saat startup.
     * Selalu cek isEconomyEnabled() dulu sebelum pakai.
     */
    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public boolean isEconomyEnabled() {
        return economyManager != null;
    }

    public SignInputManager getSignInputManager() {
        return signInputManager;
    }
}