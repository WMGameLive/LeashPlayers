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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class Listeners implements Listener {

    private LeashPlayers main = LeashPlayers.instance;

    List<Player> leashed = new ArrayList<>();
    List<LivingEntity> entityList = new ArrayList<>();
    List<Entity> playerUnleash = new ArrayList<>();

    @EventHandler
    public void onUnleash(EntityUnleashEvent e) {
        if(e.getReason() != UnleashReason.PLAYER_UNLEASH || e.getReason() != UnleashReason.HOLDER_GONE) return;
        playerUnleash.add(e.getEntity());
    }

    @EventHandler
    public void onLeash(PlayerInteractAtEntityEvent e) {
        if(!(e.getRightClicked() instanceof Player)) return;

        if(!e.getHand().equals(EquipmentSlot.HAND)) return;

        Player holder = e.getPlayer();
        Player target = (Player) e.getRightClicked();

        if(!holder.hasPermission("leashplayers.use")) return;
        //Gets if target is leashable
        if(!target.hasPermission("leashplayers.leashable")) return;

        if(!holder.getInventory().getItemInMainHand().getType().equals(Material.LEAD)) return;

        LivingEntity leashedZombie = target.getWorld().spawn(target.getLocation(), Zombie.class, zombie -> {
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
            zombie.setInvulnerable(true);
            zombie.setLeashHolder(holder);
            zombie.setAI(false);
            zombie.setVisualFire(false);
        });

        target.setAllowFlight(true);
        leashed.add(target);
        entityList.add(leashedZombie);

        holder.getInventory().getItemInMainHand().setAmount(holder.getInventory().getItemInMainHand().getAmount() - 1);

        new BukkitRunnable() {
            public void run() {
                // make sure the zombie follows the player by tping it
                leashedZombie.teleport(target, PlayerTeleportEvent.TeleportCause.PLUGIN);

                Location holderLoc = holder.getLocation();
                Location targetLoc = target.getLocation();

                double distToHolder = target.getLocation().distance(holder.getLocation());
                // if the leashed player is too far, or the leasher or leashee is disconnected or the zombie is no longer valid
                // then unleash the player and remove the zombie
                if(distToHolder > main.getConfig().getDouble("Max-Leash-Distance") || !target.isOnline() || !leashedZombie.isValid() || !leashedZombie.isLeashed() || !holder.isOnline() || playerUnleash.contains(leashedZombie)) {
                    leashed.remove(target);
                    entityList.remove(leashedZombie);
                    leashedZombie.remove();
                    target.getWorld().dropItemNaturally(target.getLocation(), new ItemStack(Material.LEAD));

                    // If not in creative do not allow flight anymore
                    if(target.getGameMode() != GameMode.CREATIVE)
                        target.setAllowFlight(false);

                    cancel();
                    return;
                }

                if(distToHolder < main.getConfig().getDouble("Min-Pull-Distance"))
                    return;

                if(!leashed.contains(target))
                    return;

                double dx = (holderLoc.getX() - targetLoc.getX()) / distToHolder;
                double dy = (holderLoc.getY() - targetLoc.getY()) / distToHolder;
                double dz = (holderLoc.getZ() - targetLoc.getZ()) / distToHolder;

                double vx = Math.copySign(dx*dx*0.4d, dx);
                double vy = Math.copySign(dy*dy*0.4d, dy);
                double vz = Math.copySign(dz*dz*0.4d, dz);

                addVel(target,
                        vx,
                        vy,
                        vz);
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

    private static void addVel(LivingEntity player, double x, double y, double z) {
        Vector playerVel = player.getVelocity();

        playerVel.add(new Vector(x, y, z));

        player.setVelocity(playerVel);
    }
}
