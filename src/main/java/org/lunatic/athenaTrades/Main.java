package org.lunatic.athenaTrades;

import org.bukkit.plugin.java.JavaPlugin;
import org.lunatic.athenaTrades.blacklist.BlacklistManager;
import org.lunatic.athenaTrades.command.TradeCommand;
import org.lunatic.athenaTrades.listener.TradeListener;
import org.lunatic.athenaTrades.trade.TradeManager;

public final class Main extends JavaPlugin {

    private static Main instance;

    private TradeManager tradeManager;
    private BlacklistManager blacklistManager;

    @Override
    public void onEnable() {
        instance = this;

        this.blacklistManager = new BlacklistManager(this);
        this.blacklistManager.load();

        this.tradeManager = new TradeManager(this);

        TradeCommand tradeCommand = new TradeCommand(this);
        getCommand("trade").setExecutor(tradeCommand);
        getCommand("trade").setTabCompleter(tradeCommand);

        getServer().getPluginManager().registerEvents(new TradeListener(this), this);

        getLogger().info("AthenaTrades enabled.");
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
}