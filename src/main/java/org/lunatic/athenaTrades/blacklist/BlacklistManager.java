package org.lunatic.athenaTrades.blacklist;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.lunatic.athenaTrades.Main;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Menyimpan daftar player yang diblokir dari trade oleh masing-masing owner.
 * Blokir bersifat satu arah waktu disimpan, tapi dicek dua arah di isBlocked()
 * supaya kalau A blokir B, B juga tidak bisa mengirim trade request ke A.
 */
public class BlacklistManager {

    private final Main plugin;
    private final File file;
    private FileConfiguration config;

    // ownerId -> (blockedId -> lastKnownName)
    private final Map<UUID, Map<UUID, String>> blocked = new HashMap<>();

    public BlacklistManager(Main plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "blacklist.yml");
    }

    public void load() {
        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Gagal membuat blacklist.yml: " + e.getMessage());
            }
        }

        config = YamlConfiguration.loadConfiguration(file);
        blocked.clear();

        ConfigurationSection blocksSection = config.getConfigurationSection("blocks");
        if (blocksSection != null) {
            for (String ownerKey : blocksSection.getKeys(false)) {
                UUID ownerId = UUID.fromString(ownerKey);
                Map<UUID, String> map = new HashMap<>();

                ConfigurationSection ownerSection = blocksSection.getConfigurationSection(ownerKey);
                if (ownerSection != null) {
                    for (String blockedKey : ownerSection.getKeys(false)) {
                        String name = ownerSection.getString(blockedKey, "Unknown");
                        map.put(UUID.fromString(blockedKey), name);
                    }
                }
                blocked.put(ownerId, map);
            }
        }
    }

    public void save() {
        if (config == null) {
            config = new YamlConfiguration();
        }
        config.set("blocks", null);

        for (Map.Entry<UUID, Map<UUID, String>> entry : blocked.entrySet()) {
            for (Map.Entry<UUID, String> blockEntry : entry.getValue().entrySet()) {
                config.set("blocks." + entry.getKey() + "." + blockEntry.getKey(), blockEntry.getValue());
            }
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Gagal menyimpan blacklist.yml: " + e.getMessage());
        }
    }

    public void block(UUID owner, UUID target, String targetName) {
        blocked.computeIfAbsent(owner, k -> new HashMap<>()).put(target, targetName);
        save();
    }

    public boolean unblock(UUID owner, UUID target) {
        Map<UUID, String> map = blocked.get(owner);
        if (map == null) return false;
        boolean removed = map.remove(target) != null;
        if (removed) save();
        return removed;
    }

    public boolean isBlocked(UUID a, UUID b) {
        return isBlockedOneWay(a, b) || isBlockedOneWay(b, a);
    }

    private boolean isBlockedOneWay(UUID owner, UUID target) {
        Map<UUID, String> map = blocked.get(owner);
        return map != null && map.containsKey(target);
    }

    public List<String> getBlockedNames(UUID owner) {
        Map<UUID, String> map = blocked.get(owner);
        if (map == null) return List.of();
        return new ArrayList<>(map.values());
    }
}