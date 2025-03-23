package etb_mob_custom;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

public class CompanionStorageManager implements Listener {
    private final CustomMobLoader plugin;
    private final File companionsFile;
    private final Logger logger;

    public CompanionStorageManager(CustomMobLoader plugin) {
        this.plugin = plugin;
        this.companionsFile = new File(plugin.getDataFolder(), "companions.yml");
        this.logger = plugin.getLogger();
        
        // Démarrer la sauvegarde automatique
        startAutoSave();
        
        // Écouter les événements de sauvegarde du monde
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void startAutoSave() {
        new BukkitRunnable() {
            @Override
            public void run() {
                saveCompanions(false);
                logger.info("Sauvegarde automatique des compagnons effectuée.");
            }
        }.runTaskTimer(plugin, 6000L, 6000L); // 5 minutes (6000 ticks)
    }

    @EventHandler
    public void onWorldSave(WorldSaveEvent event) {
        saveCompanions(false);
    }

    public void saveCompanions(boolean logDetails) {
        if (!companionsFile.exists()) {
            try {
                companionsFile.createNewFile();
            } catch (Exception e) {
                logger.severe("Impossible de créer le fichier de sauvegarde: " + e.getMessage());
                return;
            }
        }

        YamlConfiguration config = new YamlConfiguration();
        int savedCount = 0;

        try {
            // Parcourir tous les joueurs et leurs compagnons
            for (Map.Entry<UUID, List<UUID>> entry : new HashMap<>(plugin.getPlayerCompanions()).entrySet()) {
                UUID ownerUUID = entry.getKey();
                List<String> validCompanions = new ArrayList<>();

                // Obtenir le nom du joueur (connecté ou non)
                String ownerName = plugin.getServer().getPlayer(ownerUUID) != null ? 
                                 plugin.getServer().getPlayer(ownerUUID).getName() : 
                                 ownerUUID.toString();

                for (UUID companionId : entry.getValue()) {
                    Entity companion = findCompanionInAllWorlds(companionId);

                    if (companion != null) {
                        // Le compagnon existe toujours
                        validCompanions.add(companionId.toString());
                        // Sauvegarder des informations supplémentaires
                        String path = ownerUUID + ".companions." + companionId;
                        config.set(path + ".world", companion.getWorld().getName());
                        config.set(path + ".location", companion.getLocation());
                        config.set(path + ".customName", companion.customName() != null ? 
                                 PlainTextComponentSerializer.plainText().serialize(companion.customName()) : "");
                        savedCount++;

                        if (logDetails) {
                            logger.info("Sauvegarde du compagnon " + companion.customName() +
                                      " pour le joueur " + ownerName);
                        }
                    }
                }

                if (!validCompanions.isEmpty()) {
                    config.set(ownerUUID + ".companionList", validCompanions);
                    config.set(ownerUUID + ".lastKnownName", ownerName);
                }
            }

            config.save(companionsFile);
            if (logDetails) {
                logger.info("Sauvegarde terminée : " + savedCount + " compagnons sauvegardés.");
            }

        } catch (Exception e) {
            logger.severe("Erreur lors de la sauvegarde des compagnons: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void loadCompanions() {
        if (!companionsFile.exists()) {
            logger.info("Aucun fichier de sauvegarde des compagnons trouvé.");
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(companionsFile);
        int loadedCount = 0;

        try {
            // Nettoyer les anciennes données
            plugin.getPlayerCompanions().clear();
            plugin.getCompanionOwners().clear();

            // Parcourir toutes les entrées
            for (String ownerUUIDStr : config.getKeys(false)) {
                UUID ownerUUID = UUID.fromString(ownerUUIDStr);
                List<String> companionIds = config.getStringList(ownerUUIDStr + ".companionList");

                if (companionIds != null && !companionIds.isEmpty()) {
                    List<UUID> companions = new ArrayList<>();

                    for (String companionIdStr : companionIds) {
                        UUID companionId = UUID.fromString(companionIdStr);

                        // Récupérer les informations supplémentaires
                        String path = ownerUUIDStr + ".companions." + companionId;
                        String worldName = config.getString(path + ".world");
                        Location location = config.getLocation(path + ".location");

                        if (worldName != null && location != null) {
                            World world = plugin.getServer().getWorld(worldName);
                            if (world != null) {
                                companions.add(companionId);
                                plugin.getCompanionOwners().put(companionId, ownerUUID);
                                loadedCount++;
                            }
                        }
                    }

                    if (!companions.isEmpty()) {
                        plugin.getPlayerCompanions().put(ownerUUID, companions);
                    }
                }
            }

            logger.info("Chargement terminé : " + loadedCount + " compagnons chargés.");

        } catch (Exception e) {
            logger.severe("Erreur lors du chargement des compagnons: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Entity findCompanionInAllWorlds(UUID companionId) {
        for (World world : plugin.getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getUniqueId().equals(companionId)) {
                    return entity;
                }
            }
        }
        return null;
    }

    public void shutdown() {
        saveCompanions(true);
    }
}