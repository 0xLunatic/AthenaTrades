package org.lunatic.athenaTrades.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Trik umum untuk "input teks bebas" di Bukkit: taruh sign sementara jauh di atas
 * dunia (di luar jangkauan pandang normal), buka editornya lewat Paper API
 * (Player#openSign), tangkap isinya lewat SignChangeEvent, lalu bongkar lagi.
 *
 * WAJIB PAPER. Kalau server-nya Spigot murni, Player#openSign tidak tersedia
 * dan bagian ini perlu diganti pakai ProtocolLib atau cara lain.
 */
public class SignInputManager {

    private final Plugin plugin;
    private final Map<java.util.UUID, PendingInput> pending = new ConcurrentHashMap<>();
    private final AtomicLong counter = new AtomicLong();

    public SignInputManager(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Buka sign editor untuk player dengan baris instruksi di line 1-2,
     * lalu tangkap input mereka (dianggap ada di line 0 saat submit).
     */
    public void prompt(Player player,
                       String instructionLine1,
                       String instructionLine2,
                       Consumer<String> onSubmit) {

        Location loc = buildHiddenLocation(player);

        // buat sign sungguhan
        loc.getBlock().setType(Material.OAK_SIGN, false);

        Sign sign = (Sign) loc.getBlock().getState();

        sign.getSide(Side.FRONT).setLine(0, "");
        sign.getSide(Side.FRONT).setLine(1, instructionLine1 == null ? "" : instructionLine1);
        sign.getSide(Side.FRONT).setLine(2, instructionLine2 == null ? "" : instructionLine2);
        sign.getSide(Side.FRONT).setLine(3, "");

        sign.update(true, true);

        pending.put(player.getUniqueId(), new PendingInput(loc, onSubmit));

        Bukkit.getScheduler().runTaskLater(plugin, () -> {

            player.closeInventory();

            Bukkit.getScheduler().runTaskLater(plugin, () -> {

                player.teleport(player.getLocation());

                Bukkit.getScheduler().runTaskLater(plugin, () -> {

                    player.openSign(sign);

                }, 2L);

            }, 1L);

        }, 1L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {

            PendingInput input = pending.remove(player.getUniqueId());

            if (input != null) {
                cleanup(input.location);
            }

        }, 20L * 30);
    }

    /**
     * Dipanggil dari SignChangeEvent listener saat player submit sign.
     * @return true kalau event ini memang berasal dari prompt kita.
     */
    public boolean handleSubmit(Player player, Location signLocation, String submittedLine) {
        PendingInput input = pending.get(player.getUniqueId());
        if (input == null || !input.location.equals(signLocation)) {
            return false;
        }

        pending.remove(player.getUniqueId());
        cleanup(input.location);
        input.callback.accept(submittedLine == null ? "" : submittedLine.trim());
        return true;
    }

    private void cleanup(Location loc) {

        Bukkit.getScheduler().runTask(plugin, () -> {

            if (loc.getBlock().getType() == Material.OAK_SIGN) {
                loc.getBlock().setType(Material.AIR, false);
            }

        });

    }

    private Location buildHiddenLocation(Player player) {
        Location loc = player.getLocation().clone();

        loc.setY(player.getWorld().getMaxHeight() - 1);

        return loc;
    }

    private record PendingInput(Location location, Consumer<String> callback) {
    }
}