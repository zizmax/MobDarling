package mobdarling.mobdarling;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import javax.naming.Name;
import java.nio.ByteBuffer;
import java.util.*;

public final class MobDarling extends JavaPlugin implements Listener {

    public HashMap<UUID, ArmorStand> standTargets = new HashMap<UUID, ArmorStand>();
    public HashMap<UUID, ArrayList<Pet>> owners = new HashMap<UUID, ArrayList<Pet>>();

    enum Mood {
        AGGRESSIVE,
        PASSIVE
    }

    enum State {
        FIGHTING,
        SITTING,
        WALKING
    }

    public class Pet {
        private int id;
        private UUID owner;
        private State state;
        private Mood mood;
        private Mob mob;

        public Pet(int id, UUID owner, Mood mood, State state, Mob mob) {
            this.id = id;
            this.owner = owner;
            this.mood = mood;
            this.state = state;
            this.mob = mob;
        }

        public void setState(State state) {
            this.state = state;

            //FIXME: Appears to not work at all. What am I doing wrong??
            /*
            switch (state) {
                case FIGHTING:
                    //this.mob.setCustomName(ChatColor.RED + ChatColor.stripColor(this.mob.getCustomName()));
                    this.mob.setCustomName(ChatColor.RED + "FIGHTING");
                case SITTING:
                    this.mob.setAI(false);
                    AreaEffectCloud cloud = (AreaEffectCloud) this.mob.getWorld().spawnEntity(this.mob.getLocation(), EntityType.AREA_EFFECT_CLOUD);
                    cloud.setParticle(Particle.TOWN_AURA);
                    cloud.setRadius(0);
                    cloud.addPassenger(this.mob);
                    //this.mob.setCustomName(ChatColor.GREEN + ChatColor.stripColor(this.mob.getCustomName()));
                    this.mob.setCustomName(ChatColor.WHITE + "SITTING");

                case WALKING:
                    this.mob.setAI(true);
                    if(this.mob.getVehicle() != null) {
                        this.mob.getVehicle().remove();
                    }
                    this.mob.setTarget(standTargets.get(this.getOwner()));
                    //player.sendMessage("Targeted stand");
                    //this.mob.setCustomName(ChatColor.GREEN + ChatColor.stripColor(this.mob.getCustomName()));
                    this.mob.setCustomName(ChatColor.GREEN + "WALKING");

            }

             */
        }

        public State getState() {
            return state;
        }

        public int getId() {
            return id;
        }

        public UUID getOwner() {
            return owner;
        }

        public Mob getMob() { return mob; }

    }

    ArrayList<Pet> pets = new ArrayList<Pet>();

    public UUID getOwner(int id) {
        for (Pet pet : pets) {
            if (pet.getId() == id) {
                return pet.getOwner();
            }
        }
        return null;
    };

    public Pet getPet(int id) {
        for (Pet pet : pets) {
            if (pet.getId() == id) {
                return pet;
            }
        }
        return null;
    }

    public class UUIDDataType implements PersistentDataType<byte[], UUID> {
        // Entire class written by @LynxPlay
        // Borrowed from: https://www.spigotmc.org/threads/a-guide-to-1-14-persistentdataholder-api.371200/

        @Override
        public Class<byte[]> getPrimitiveType() {
            return byte[].class;
        }

        @Override
        public Class<UUID> getComplexType() {
            return UUID.class;
        }

        @Override
        public byte[] toPrimitive(UUID complex, PersistentDataAdapterContext context) {
            ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
            bb.putLong(complex.getMostSignificantBits());
            bb.putLong(complex.getLeastSignificantBits());
            return bb.array();
        }

        @Override
        public UUID fromPrimitive(byte[] primitive, PersistentDataAdapterContext context) {
            ByteBuffer bb = ByteBuffer.wrap(primitive);
            long firstLong = bb.getLong();
            long secondLong = bb.getLong();
            return new UUID(firstLong, secondLong);
        }
    }

    public void setFighting(Pet pet, LivingEntity entity){
        this.getLogger().info("setFighting()");
        pet.setState(State.FIGHTING);
        Mob mob = pet.getMob();
        //this.mob.setCustomName(ChatColor.RED + ChatColor.stripColor(this.mob.getCustomName()));
        mob.setCustomName(ChatColor.RED + "FIGHTING");
        mob.setTarget(entity);
        NamespacedKey key = new NamespacedKey(this, "stateKey");
        mob.getPersistentDataContainer().set(key, PersistentDataType.STRING, "FIGHTING");
    }

    public void setSitting(Pet pet){
        this.getLogger().info("setSitting()");
        pet.setState(State.SITTING);
        Mob mob = pet.getMob();
        mob.setAI(false);
        AreaEffectCloud cloud = (AreaEffectCloud) mob.getWorld().spawnEntity(mob.getLocation().add(0, 1000, 0), EntityType.AREA_EFFECT_CLOUD);
        cloud.teleport(mob.getLocation().subtract(0,1000.4, 0));
        cloud.setParticle(Particle.TOWN_AURA);
        cloud.setRadius(0);
        cloud.addPassenger(mob);
        cloud.setInvulnerable(true);
        cloud.setDuration(631138512); // ~1 year of ticks
        //this.mob.setCustomName(ChatColor.GREEN + ChatColor.stripColor(this.mob.getCustomName()));
        mob.setCustomName(ChatColor.WHITE + "SITTING");
        NamespacedKey key = new NamespacedKey(this, "stateKey");
        mob.getPersistentDataContainer().set(key, PersistentDataType.STRING, "SITTING");
    }

    public void setWalking(Pet pet){
        this.getLogger().info("setWalking()");
        pet.setState(State.WALKING);
        Mob mob = pet.getMob();
        mob.setAI(true);
        if(mob.getVehicle() != null) {
            mob.getVehicle().remove();
        }
        mob.setTarget(standTargets.get(pet.getOwner()));
        //player.sendMessage("Targeted stand");
        //this.mob.setCustomName(ChatColor.GREEN + ChatColor.stripColor(this.mob.getCustomName()));
        mob.setCustomName(ChatColor.GREEN + "WALKING");
        NamespacedKey key = new NamespacedKey(this, "stateKey");
        mob.getPersistentDataContainer().set(key, PersistentDataType.STRING, "WALKING");
    }


    //FIXME:
    //You can hit the invisible armor stand/it can block you??
    //Will continue targeting until player moves after targeted mob dies
    //Pets can target themselves
    //Armor Stands, AECs, and Pet objects may persist when gone/dead/unloaded

    //TODO:
    //Teleporting if too far away

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        this.getLogger().info("Enabled!");

    }

    @Override
    public void onDisable() {
        this.getLogger().info("Freezing all pets...");
        for (Pet pet : pets) {
            setWalking(pet);
        }
        this.getLogger().info("Disabled!");
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {

        //Send death message to pet owner
        UUID uuid = getOwner(event.getEntity().getEntityId());
        if(uuid != null) {
            getServer().getPlayer(uuid).sendMessage(event.getEntity().getCustomName() + " died!!! :(");
            Entity vehicle = event.getEntity().getVehicle();
            if (vehicle != null){
                vehicle.remove();
            }
            pets.remove(getPet(event.getEntity().getEntityId()));
        }

        //Re-target armor stand after death of target
        for(Pet pet: pets) {
            Mob mob = pet.getMob();
            if(mob.getTarget().equals(event.getEntity())) {
                setWalking(pet);
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (event.getFrom().getBlockX() != event.getTo().getBlockX()
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ()
                || event.getFrom().getBlockY() != event.getTo().getBlockY()) {

            Vector inverseVector = player.getLocation().getDirection().normalize().multiply(-5);
            Location loc = player.getLocation().add(inverseVector);
            standTargets.get(player.getUniqueId()).teleport(loc);
            if(owners.get(player.getUniqueId()) != null) {
                for (Pet pet : owners.get(player.getUniqueId())) {
                    Mob mob = pet.getMob();
                    if (pet.getState() == State.WALKING || (pet.getState() == State.FIGHTING && mob.getTarget().isDead())) {
                        setWalking(pet);
                    }
                    else if (pet.getState() == State.FIGHTING) {
                        player.sendMessage(mob.getTarget().toString());
                        player.sendMessage("Fighting. Didn't target stand");
                    }
                }
            }
        }
    }

    @EventHandler
    public void preventSkeletonShootingArmorStand(EntityShootBowEvent event) {
        if (getOwner(event.getEntity().getEntityId()) != null) {
            Mob mob = (Mob) event.getEntity();
            if (mob.getTarget() instanceof ArmorStand) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void playerHitEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            World world = player.getWorld();
            LivingEntity entity = null;
            if (event.getEntity() instanceof LivingEntity) {
                entity = (LivingEntity) event.getEntity();
            } else
                return;

            // Pet targeting
            player.sendMessage("Hit!");
            if(owners.get(player.getUniqueId()) != null) {
                for (Pet pet : owners.get(player.getUniqueId())) {
                    if (pet.getState() != State.SITTING) {
                        setFighting(pet, entity);
                        player.sendMessage(pet.getMob().getCustomName() + " targeted " + entity.getName());
                    }
                }
            }
        }
    }

    @EventHandler
    public void onImpossibleCombust(EntityCombustEvent event) {
        if(getOwner(event.getEntity().getEntityId()) != null){
            event.setCancelled(true);
        }
        if(event.getEntity().getType() == EntityType.ARMOR_STAND && event.getEntity().isInvulnerable()){
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void rightClickPet(PlayerInteractEntityEvent event) {
        if(event.getHand().equals(EquipmentSlot.HAND)) {
            Player player = event.getPlayer();
            player.sendMessage("CLICKED");
            World world = player.getWorld();
            LivingEntity entity = null;
            if (event.getRightClicked() instanceof LivingEntity) {
                entity = (LivingEntity) event.getRightClicked();
            } else
                return;
            ItemStack item = player.getInventory().getItemInMainHand();

            /*
            //Pet equipping
            if ((entity.getType() == EntityType.SKELETON || entity.getType() == EntityType.ZOMBIE) && (item.getType() == Material.GOLDEN_CHESTPLATE || item.getType() == Material.DIAMOND_CHESTPLATE)) {
                if (entity.getCustomName() != null && entity.getCustomName().contains("[PET]")) {
                    player.sendMessage("equipping");
                    entity.getEquipment().setChestplate(item);
                    player.getInventory().setItemInMainHand(null);
                    ItemStack[] armor = player.getEquipment().getArmorContents();
                    Bukkit.getServer().getScheduler().runTaskLaterAsynchronously(this, new Runnable() {
                        public void run() {
                            player.getInventory().setArmorContents(armor);
                        }
                    }, 1L);

                }
            }

             */

            //Pet taming
            if (entity.getType() == EntityType.SKELETON || entity.getType() == EntityType.ZOMBIE) {
                Mob mob = (Mob) entity;

                NamespacedKey key = new NamespacedKey(this, "ownerKey");
                PersistentDataContainer container = entity.getPersistentDataContainer();

                if(container.has(key, new UUIDDataType())){
                    // Is a pet
                    if(container.get(key, new UUIDDataType()).equals(player.getUniqueId())) {
                        // Is player's pet
                        player.sendMessage("Toggling SITTING");
                        event.setCancelled(true);
                        Pet pet = getPet(entity.getEntityId());
                        if (pet.getState() != State.SITTING) {
                            setSitting(pet);
                            return;
                        } else {
                            setWalking(pet);
                            return;
                        }


                    }
                }

                else if((entity.getType() == EntityType.SKELETON && item.getType() == Material.BONE) || (entity.getType() == EntityType.ZOMBIE && item.getType() == Material.ROTTEN_FLESH)){
                    // Is not a pet
                    Random random = new Random();
                    if (random.nextDouble() < 0.33) {
                        Chicken effectChicken = (Chicken) world.spawnEntity(entity.getLocation(), EntityType.CHICKEN);
                        effectChicken.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 200, 1, true, false));
                        effectChicken.setCollidable(false);
                        effectChicken.setSilent(true);
                        effectChicken.setAI(false);
                        effectChicken.playEffect(EntityEffect.LOVE_HEARTS);
                        effectChicken.remove();
                        entity.setCustomName(player.getName() + "'s pet " + entity.getType().name());
                        entity.setCustomNameVisible(true);
                        mob.setTarget(standTargets.get(player.getUniqueId()));
                        mob.setCanPickupItems(true);
                        mob.setSilent(true);

                        Pet pet = new Pet(mob.getEntityId(), player.getUniqueId(),  Mood.PASSIVE, State.WALKING, mob);
                        pets.add(pet);

                        setWalking(pet);

                        if(owners.get(player.getUniqueId()) == null) {
                            owners.put(player.getUniqueId(), new ArrayList<Pet>());
                            owners.get(player.getUniqueId()).add(pet);
                        }
                        else {
                            owners.get(player.getUniqueId()).add(pet);
                        }
                        // Set PersistentData using API
                        mob.getPersistentDataContainer().set(key, new UUIDDataType(), player.getUniqueId());

                    }
                }
            }
        }
    }

    @EventHandler
    public void targetWrongEntityEvent(EntityTargetEvent event) {
        if(getOwner(event.getEntity().getEntityId()) != null && event.getTarget() != null && event.getTarget().getType() != EntityType.ARMOR_STAND) {
            getServer().getLogger().info(event.getEntity().getCustomName() + " tried to target " + event.getTarget().getName());
            event.setTarget(null);
        }
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        //Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(),"kill @e[type=minecraft:armor_stand]");
        Vector inverseVector = player.getLocation().getDirection().normalize().multiply(-5);
        ArmorStand standTarget = (ArmorStand) player.getWorld().spawnEntity(player.getLocation().add(inverseVector), EntityType.ARMOR_STAND);
        standTarget.setVisible(false);
        standTarget.setInvulnerable(true);
        standTarget.setGravity(false);
        standTargets.put(player.getUniqueId(), standTarget);
    }

    @EventHandler
    public void onPlayerLogout(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        standTargets.get(player.getUniqueId()).remove();
        standTargets.remove(player.getUniqueId());
        if(owners.get(player.getUniqueId()) != null) {
            for (Pet pet : owners.get(player.getUniqueId())) {
                setSitting(pet);
                standTargets.get(player.getUniqueId()).remove();
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("mobdarling")) {
            //TODO: DO SOMETHING
            sender.sendMessage("wow");
        }
        return true;
    }

    @EventHandler
    public void onChunk(ChunkLoadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity.getType() == EntityType.SKELETON || entity.getType() == EntityType.ZOMBIE) {
                NamespacedKey ownerKey = new NamespacedKey(this, "ownerKey");
                NamespacedKey stateKey = new NamespacedKey(this, "stateKey");
                PersistentDataContainer container = entity.getPersistentDataContainer();
                if (container.has(ownerKey, new UUIDDataType())) {
                    // Is a pet
                    if(getOwner(entity.getEntityId()) == null){
                        // Is not yet in pets ArrayList
                        UUID uuid = container.get(ownerKey, new UUIDDataType());
                        State state = State.valueOf(container.get(stateKey, PersistentDataType.STRING));
                        Pet pet = new Pet(entity.getEntityId(), uuid, Mood.PASSIVE, state, (Mob) entity);
                        pets.add(pet);
                        if(owners.get(uuid) == null) {
                            owners.put(uuid, new ArrayList<Pet>());
                        }
                        owners.get(uuid).add(pet);
                        this.getLogger().severe("Loaded " + pet.getMob().getCustomName());
                    }
                }
            }
        }
    }
}
