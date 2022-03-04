package wm.vdr.leashplayers;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
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

    @EventHandler
    public void onLeash(PlayerInteractAtEntityEvent e) {
        if(!(e.getRightClicked() instanceof Player)) return;

        if(!e.getHand().equals(EquipmentSlot.HAND)) return;

        Player player = e.getPlayer();
        Player target = (Player) e.getRightClicked();

        if(!player.hasPermission("leashplayer.use")) return;

        if(leashed.contains(target)) {
            leashed.remove(target);
            //((CraftPlayer)player).getHandle().a(EnumHand.a, true);
            return;
        }

        if(!player.getInventory().getItemInMainHand().getType().equals(Material.LEAD)) return;

        LivingEntity entity = (LivingEntity) target.getWorld().spawnEntity(target.getLocation(), EntityType.ZOMBIE);
        entity.setSilent(true);
        entity.setInvisible(true);
        entity.setCollidable(false);
        entity.setInvulnerable(true);
        entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 255, false, false));
        entity.setLeashHolder(player);

        target.setAllowFlight(true);
        leashed.add(target);
        entityList.add(entity);

        player.getInventory().getItemInMainHand().setAmount(player.getInventory().getItemInMainHand().getAmount() - 1);
        //((CraftPlayer)player).getHandle().a(EnumHand.a, true);

        new BukkitRunnable() {
            public void run() {
                if(!entity.isValid() || !entity.isLeashed() || !leashed.contains(target)) {
                    leashed.remove(target);
                    entityList.remove(entity);
                    entity.remove();
                    target.setAllowFlight(false);
                    target.getWorld().dropItemNaturally(target.getLocation(), new ItemStack(Material.LEAD));
                    cancel();
                }
                Location location = target.getLocation();
                location.setX(entity.getLocation().getX());
                location.setY(entity.getLocation().getY());
                location.setZ(entity.getLocation().getZ());
                target.teleport(location, PlayerTeleportEvent.TeleportCause.UNKNOWN);
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
