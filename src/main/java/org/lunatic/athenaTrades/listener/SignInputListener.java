package org.lunatic.athenaTrades.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.lunatic.athenaTrades.Main;

public class SignInputListener implements Listener {

    private final Main plugin;

    public SignInputListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        boolean handled = plugin.getSignInputManager()
                .handleSubmit(player, event.getBlock().getLocation(), event.getLine(0));

        if (handled) {
            event.setCancelled(false);
        }
    }
}