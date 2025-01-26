package etb_mob_custom;

import org.bukkit.entity.EntityType;

public class CustomMob {
    private final String id;
    private final EntityType baseEntity;
    private final String name;
    private final double health;
    private final double damage;
    private final double speed;
    private final boolean isCompanion;
    private final double followDistance;
    private final boolean protectOwner;
    
    public CustomMob(String id, EntityType baseEntity, String name, double health, 
                    double damage, double speed, boolean isCompanion, 
                    double followDistance, boolean protectOwner) {
        this.id = id;
        this.baseEntity = baseEntity;
        this.name = name;
        this.health = health;
        this.damage = damage;
        this.speed = speed;
        this.isCompanion = isCompanion;
        this.followDistance = followDistance;
        this.protectOwner = protectOwner;
    }
    
    // Getters
    public String getId() { return id; }
    public EntityType getBaseEntity() { return baseEntity; }
    public String getName() { return name; }
    public double getHealth() { return health; }
    public double getDamage() { return damage; }
    public double getSpeed() { return speed; }
    public boolean isCompanion() { return isCompanion; }
    public double getFollowDistance() { return followDistance; }
    public boolean isProtectOwner() { return protectOwner; }
}