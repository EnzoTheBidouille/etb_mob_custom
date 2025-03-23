package etb_mob_custom;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.*;

public class CustomMobLoader extends JavaPlugin implements Listener {
    private Map<String, CustomMob> customMobs = new HashMap<>();
    private Map<UUID, UUID> companionOwners = new HashMap<>();
    private Map<UUID, List<UUID>> playerCompanions = new HashMap<>();
    private Map<UUID, Long> lastDamageTime = new HashMap<>();
    private CompanionStorageManager storageManager;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
    
        loadCustomMobs();
        storageManager = new CompanionStorageManager(this);
        storageManager.loadCompanions();
        
        getCommand("pet").setExecutor(new SpawnCompanionCommand(this));
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new CompanionTeleportListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this, storageManager), this); // Ajout du nouveau listener
    
        startCompanionFollowTask();
    
        getLogger().info("CustomMobLoader a été activé avec succès!");
    }

    @Override
    public void onDisable() {
        if (storageManager != null) {
            storageManager.shutdown();
        }
        getLogger().info("CustomMobLoader a été désactivé avec succès!");
    }

    public Map<String, CustomMob> getCustomMobs() {
        return this.customMobs;
    }

    public Map<UUID, List<UUID>> getPlayerCompanions() {
        return playerCompanions;
    }

    public Map<UUID, UUID> getCompanionOwners() {
        return companionOwners;
    }

    private void loadCustomMobs() {
        File mobsFolder = new File(getDataFolder(), "mobs");
        if (!mobsFolder.exists()) {
            mobsFolder.mkdirs();
            saveDefaultMob();
        }

        if (mobsFolder.listFiles() != null) {
            for (File file : mobsFolder.listFiles()) {
                if (file.getName().endsWith(".yml")) {
                    FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                    String mobId = file.getName().replace(".yml", "");

                    CustomMob customMob = new CustomMob(
                            mobId,
                            EntityType.valueOf(config.getString("baseEntity")),
                            config.getString("name"),
                            config.getDouble("health"),
                            config.getDouble("damage"),
                            config.getDouble("speed"),
                            config.getBoolean("isCompanion", false),
                            config.getDouble("followDistance", 3.0),
                            config.getBoolean("protectOwner", true)
                    );

                    customMobs.put(mobId, customMob);
                    getLogger().info("Loaded custom mob: " + mobId);
                }
            }
        }
    }

    private void saveDefaultMob() {
        File exampleFile = new File(getDataFolder(), "mobs/example_companion.yml");
        FileConfiguration config = new YamlConfiguration();

        config.set("baseEntity", "WOLF");
        config.set("name", "§bFidèle Compagnon");
        config.set("health", 20.0);
        config.set("damage", 5.0);
        config.set("speed", 0.3);
        config.set("isCompanion", true);
        config.set("followDistance", 3.0);
        config.set("protectOwner", true);

        try {
            config.save(exampleFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Entity spawnCompanion(String mobId, Player owner) {
        CustomMob customMob = customMobs.get(mobId);
        if (customMob == null || !customMob.isCompanion()) return null;

        if (owner.getWorld() == null) return null;

        Location spawnLoc = owner.getLocation().add(1, 0, 1);
        Entity entity = spawnLoc.getWorld().spawnEntity(spawnLoc, customMob.getBaseEntity());

        if (entity instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity) entity;

            // Configuration de base avec le nouveau système de Component
            Component customName = Component.text(customMob.getName() + " §7(" + owner.getName() + ")");
            livingEntity.customName(customName);
            livingEntity.setCustomNameVisible(true);

            // Configuration des attributs
            if (livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
                livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(customMob.getHealth());
                livingEntity.setHealth(customMob.getHealth());
            }

            if (livingEntity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
                livingEntity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(customMob.getDamage());
            }

            if (livingEntity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) != null) {
                livingEntity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(customMob.getSpeed());
            }

            // Configurations spécifiques aux compagnons
            if (entity instanceof Tameable) {
                ((Tameable) entity).setOwner(owner);
                ((Tameable) entity).setTamed(true);
            }

            // Enregistrer la relation propriétaire-compagnon
            companionOwners.put(entity.getUniqueId(), owner.getUniqueId());
            playerCompanions.computeIfAbsent(owner.getUniqueId(), k -> new ArrayList<>())
                    .add(entity.getUniqueId());
        }

        return entity;
    }

    public void updateCompanionWorld(Entity companion, World newWorld) {
        if (companionOwners.containsKey(companion.getUniqueId()) && companion instanceof LivingEntity) {
            CustomMob customMob = getCustomMobFromEntity(companion);
            if (customMob != null) {
                LivingEntity livingEntity = (LivingEntity) companion;

                // Réappliquer les attributs après changement de monde
                if (livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
                    livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(customMob.getHealth());
                }
                if (livingEntity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
                    livingEntity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(customMob.getDamage());
                }
                if (livingEntity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) != null) {
                    livingEntity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(customMob.getSpeed());
                }
            }
        }
    }

    private void startCompanionFollowTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<UUID, UUID> entry : new HashMap<>(companionOwners).entrySet()) {
                    Entity companion = getServer().getEntity(entry.getKey());
                    Player owner = getServer().getPlayer(entry.getValue());

                    if (companion == null || owner == null || !owner.isOnline() ||
                            !companion.getWorld().equals(owner.getWorld())) {
                        continue;
                    }

                    if (!(companion instanceof Mob)) {
                        continue;
                    }

                    Mob mobCompanion = (Mob) companion;
                    CustomMob customMob = getCustomMobFromEntity(companion);

                    if (customMob == null) {
                        continue;
                    }

                    Location companionLoc = companion.getLocation();
                    Location ownerLoc = owner.getLocation();
                    double currentDistance = companionLoc.distance(ownerLoc);
                    double followDistance = customMob.getFollowDistance();

                    // Gestion du pathfinding en fonction de la distance
                    if (currentDistance > followDistance * 3) {
                        // Si trop loin, téléportation avec un petit offset aléatoire
                        double offsetX = (Math.random() - 0.5) * 2;
                        double offsetZ = (Math.random() - 0.5) * 2;
                        Location teleportLoc = ownerLoc.clone().add(offsetX, 0, offsetZ);
                        companion.teleport(teleportLoc);
                    } else if (currentDistance > followDistance) {
                        // Vérifier si le propriétaire est attaqué
                        if (isOwnerUnderAttack(owner)) {
                            // Chercher l'attaquant le plus proche
                            LivingEntity attacker = findNearestAttacker(owner);
                            if (attacker != null && customMob.isProtectOwner()) {
                                mobCompanion.setTarget(attacker);
                                continue;
                            }
                        }

                        // Suivre le propriétaire
                        Vector direction = owner.getLocation().getDirection();
                        Location targetLoc = owner.getLocation().subtract(direction.multiply(followDistance));

                        // Utiliser le pathfinding natif
                        mobCompanion.getPathfinder().moveTo(targetLoc, customMob.getSpeed());
                    }
                }
            }

            private boolean isOwnerUnderAttack(Player owner) {
                Long lastDamage = lastDamageTime.get(owner.getUniqueId());
                return lastDamage != null && System.currentTimeMillis() - lastDamage < 5000;
            }

            private LivingEntity findNearestAttacker(Entity owner) {
                double closestDistance = Double.MAX_VALUE;
                LivingEntity closestAttacker = null;

                for (Entity entity : owner.getNearbyEntities(10, 10, 10)) {
                    if (entity instanceof Monster && entity instanceof LivingEntity) {
                        double distance = entity.getLocation().distance(owner.getLocation());
                        if (distance < closestDistance) {
                            closestDistance = distance;
                            closestAttacker = (LivingEntity) entity;
                        }
                    }
                }
                return closestAttacker;
            }
        }.runTaskTimer(this, 0L, 5L);
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (companionOwners.containsKey(event.getEntity().getUniqueId())) {
            UUID ownerId = companionOwners.get(event.getEntity().getUniqueId());
            if (event.getTarget() != null && event.getTarget().getUniqueId().equals(ownerId)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player) {
            Player victim = (Player) event.getEntity();
            List<UUID> companions = playerCompanions.get(victim.getUniqueId());

            if (companions != null) {
                for (UUID companionId : companions) {
                    Entity companion = getServer().getEntity(companionId);
                    if (companion instanceof LivingEntity) {
                        CustomMob customMob = getCustomMobFromEntity(companion);
                        if (customMob != null && customMob.isProtectOwner()) {
                            if (companion instanceof Creature && event.getDamager() instanceof LivingEntity) {
                                ((Creature) companion).setTarget((LivingEntity) event.getDamager());
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            lastDamageTime.put(event.getEntity().getUniqueId(), System.currentTimeMillis());
        }
    }

    private CustomMob getCustomMobFromEntity(Entity entity) {
        Component customName = entity.customName();
        if (customName == null) return null;

        String nameStr = customName.toString();
        for (CustomMob mob : customMobs.values()) {
            if (nameStr.startsWith(mob.getName())) {
                return mob;
            }
        }
        return null;
    }
}