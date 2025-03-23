package etb_mob_custom;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {
    private final CustomMobLoader plugin;
    private final CompanionStorageManager storageManager;

    public PlayerQuitListener(CustomMobLoader plugin, CompanionStorageManager storageManager) {
        this.plugin = plugin;
        this.storageManager = storageManager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Vérifier si le joueur a des compagnons
        if (plugin.getPlayerCompanions().containsKey(player.getUniqueId())) {
            // Sauvegarder avec des logs détaillés pour le debug
            plugin.getLogger().info("Sauvegarde des compagnons pour " + player.getName() + " (déconnexion)");
            storageManager.saveCompanions(true);
        }
    }
}