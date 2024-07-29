package wm.vdr.leashplayers;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityUnleashEvent;
import org.bukkit.event.entity.EntityUnleashEvent.UnleashReason;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class Listeners implements Listener {

    private LeashPlayers main = LeashPlayers.instance;

    List<Player> leashed = new ArrayList<>();
    List<LivingEntity> entityList = new ArrayList<>();
    List<Entity> distanceUnleash = new ArrayList<>();

    @EventHandler
    public void onUnleash(EntityUnleashEvent e) {
        if(e.getReason() == UnleashReason.PLAYER_UNLEASH) return;
        distanceUnleash.add(e.getEntity());
    }

    @EventHandler
    public void onLeash(PlayerInteractAtEntityEvent e) {
        if(!(e.getRightClicked() instanceof Player)) return;

        if(!e.getHand().equals(EquipmentSlot.HAND)) return;

        Player player = e.getPlayer();
        Player target = (Player) e.getRightClicked();

        if(!player.hasPermission("leashplayers.use")) return;
        //Gets if target is leashable
        if(!target.hasPermission("leashplayers.leashable")) return;

        if(leashed.contains(target)) {
            leashed.remove(target);
            //((CraftPlayer)player).getHandle().a(EnumHand.a, true);
            return;
        }

        if(!player.getInventory().getItemInMainHand().getType().equals(Material.LEAD)) return;

        LivingEntity entity = target.getWorld().spawn(target.getLocation(), Zombie.class, zombie -> {
            zombie.getEquipment().setItemInMainHand(null);
            zombie.getEquipment().setHelmet(null);
            zombie.getEquipment().setChestplate(null);
            zombie.getEquipment().setLeggings(null);
            zombie.getEquipment().setBoots(null);
            zombie.setCanPickupItems(false);
            zombie.setAdult();
            if(zombie.getVehicle() != null)
                zombie.getVehicle().remove();
            zombie.setSilent(true);
            zombie.setInvisible(true);
            zombie.setCollidable(false);
            zombie.getCollidableExemptions().clear();  // Make sure NO ONE can collide.
            zombie.setInvulnerable(true);
            zombie.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 255, false, false));
            zombie.setLeashHolder(player);
        });

        target.setAllowFlight(true);
        leashed.add(target);
        entityList.add(entity);

        player.getInventory().getItemInMainHand().setAmount(player.getInventory().getItemInMainHand().getAmount() - 1);
        //((CraftPlayer)player).getHandle().a(EnumHand.a, true);

        new BukkitRunnable() {
            public void run() {
                if(!target.isOnline() || !entity.isValid() || !entity.isLeashed() || !leashed.contains(target)) {
                    leashed.remove(target);
                    entityList.remove(entity);
                    entity.remove();

                    boolean allowFlight = target.getGameMode().equals(GameMode.CREATIVE);  // Fixes an error when leashed in creative mode.
                    target.setAllowFlight(allowFlight);
                    if(!distanceUnleash.contains(entity))
                        target.getWorld().dropItemNaturally(target.getLocation(), new ItemStack(Material.LEAD));
                    else
                        distanceUnleash.remove(entity);
                    cancel();
                }

                Location leashedPlayerLocation = target.getLocation();
                Location zombieLocation = entity.getLocation();

                if (leashedPlayerLocation.distanceSquared(zombieLocation) == 0) return;

                leashedPlayerLocation.setX(zombieLocation.getX());
                leashedPlayerLocation.setY(zombieLocation.getY());
                leashedPlayerLocation.setZ(zombieLocation.getZ());

                target.teleport(leashedPlayerLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
            }
        }.runTaskTimer(main,0,main.getConfig().getInt("Leashed-Check-Delay"));
    }

    @EventHandler
    public void onFlame(EntityCombustEvent e) {
        if(!(e.getEntity() instanceof LivingEntity)) return;
        if(entityList.contains((LivingEntity) e.getEntity())) e.setCancelled(true);
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if(!(e.getDamager() instanceof LivingEntity)) return;
        if(entityList.contains((LivingEntity) e.getDamager())) e.setCancelled(true);
    }
}
