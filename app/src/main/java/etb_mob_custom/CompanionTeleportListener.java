package etb_mob_custom;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class CompanionTeleportListener implements Listener {
    private final CustomMobLoader plugin;

    public CompanionTeleportListener(CustomMobLoader plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer();
        final Location to = event.getTo();

        // Exécuter la téléportation des compagnons un peu après celle du joueur
        new BukkitRunnable() {
            @Override
            public void run() {
                // Vérifier que le joueur est toujours en ligne
                if (!player.isOnline()) return;

                // Récupérer les compagnons du joueur via l'UUID
                if (!plugin.getPlayerCompanions().containsKey(player.getUniqueId())) return;
                
                for (java.util.UUID companionId : plugin.getPlayerCompanions().get(player.getUniqueId())) {
                    Entity companion = plugin.getServer().getEntity(companionId);
                    
                    if (companion != null) {
                        // Créer une position sûre près du joueur
                        Location safeLoc = findSafeLocation(to.clone());
                        
                        // Téléporter le compagnon
                        companion.teleport(safeLoc);
                        
                        // Mettre à jour les attributs du compagnon si nécessaire
                        plugin.updateCompanionWorld(companion, to.getWorld());
                        
                        // Debug log
                        plugin.getLogger().info("Companion teleported to new world with player " + player.getName());
                    }
                }
            }
        }.runTaskLater(plugin, 5L); // Attendre 5 ticks (0.25 secondes)
    }

    private Location findSafeLocation(Location baseLocation) {
        // Ajouter un léger offset aléatoire pour éviter les empilements
        double offsetX = (Math.random() - 0.5) * 2;
        double offsetZ = (Math.random() - 0.5) * 2;
        
        Location loc = baseLocation.clone().add(offsetX, 0, offsetZ);
        World world = loc.getWorld();
        
        // Trouver une position sûre en hauteur
        int maxHeight = world.getMaxHeight();
        int minHeight = world.getMinHeight();
        
        // Commencer à la position du joueur
        int y = loc.getBlockY();
        
        // Chercher vers le haut et vers le bas
        for (int offset = 0; offset < 5; offset++) {
            // Vérifier au-dessus
            if (y + offset < maxHeight) {
                Location checkUp = new Location(world, loc.getX(), y + offset, loc.getZ());
                if (isSafeLocation(checkUp)) {
                    return checkUp.add(0.5, 0, 0.5); // Centrer dans le block
                }
            }
            
            // Vérifier en-dessous
            if (y - offset > minHeight) {
                Location checkDown = new Location(world, loc.getX(), y - offset, loc.getZ());
                if (isSafeLocation(checkDown)) {
                    return checkDown.add(0.5, 0, 0.5); // Centrer dans le block
                }
            }
        }
        
        // Si aucun endroit sûr n'est trouvé, retourner la position d'origine
        return baseLocation;
    }

    private boolean isSafeLocation(Location location) {
        return location != null && 
               location.getBlock().isEmpty() && 
               location.clone().add(0, 1, 0).getBlock().isEmpty() && // Espace pour la tête
               !location.clone().subtract(0, 1, 0).getBlock().isEmpty(); // Block solide en dessous
    }
}