package org.Cry_Legende.sHVillagerNerf;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.World;

public class SHVillagerNerf extends JavaPlugin implements Listener {

    private boolean preventZombieVillagerCuring;
    private boolean lobotomizeVillagers;
    private boolean disableVillagerTrading;
    private boolean disableVillagerBreeding;
    private boolean preventVillagerItemPickup;
    private boolean makeVillagersSilent;
    private boolean preventProfessionChange;
    private boolean preventTradeRefresh;
    private boolean makeVillagersInvulnerable;
    private boolean preventVillagerDoorOpening;
    private boolean preventGolemSpawningByVillagers;
    private String disabledTradeMessage;

    @Override
    public void onEnable() {
        saveDefaultConfig(); // Crée config.yml s'il n'existe pas
        loadConfigOptions();
        getServer().getPluginManager().registerEvents(this, this);

        // Appliquer certaines modifications aux villageois existants au démarrage
        if (lobotomizeVillagers || makeVillagersSilent || makeVillagersInvulnerable) {
            applyToExistingVillagers();
        }

        getLogger().info("SHVillagerNerf a été activé !");
    }

    @Override
    public void onDisable() {
        // Optionnel: Inverser certains effets si nécessaire, bien que pour un "nerf" ce ne soit généralement pas le cas.
        // Par exemple, si on voulait redonner l'IA, il faudrait une logique plus complexe.
        getLogger().info("SHVillagerNerf a été désactivé !");
    }

    private void loadConfigOptions() {
        reloadConfig(); // S'assure qu'on a la dernière version
        preventZombieVillagerCuring = getConfig().getBoolean("preventZombieVillagerCuring", true);
        lobotomizeVillagers = getConfig().getBoolean("lobotomizeVillagers", false);
        disableVillagerTrading = getConfig().getBoolean("disableVillagerTrading", false);
        disableVillagerBreeding = getConfig().getBoolean("disableVillagerBreeding", false);
        preventVillagerItemPickup = getConfig().getBoolean("preventVillagerItemPickup", false);
        makeVillagersSilent = getConfig().getBoolean("makeVillagersSilent", false);
        preventProfessionChange = getConfig().getBoolean("preventProfessionChange", false);
        preventTradeRefresh = getConfig().getBoolean("preventTradeRefresh", false);
        makeVillagersInvulnerable = getConfig().getBoolean("makeVillagersInvulnerable", false);
        preventVillagerDoorOpening = getConfig().getBoolean("preventVillagerDoorOpening", false);
        preventGolemSpawningByVillagers = getConfig().getBoolean("preventGolemSpawningByVillagers", false);

        disabledTradeMessage = getConfig().getString("disabledTradeMessage", "&cLe commerce avec ce villageois est désactivé.");
        if (disabledTradeMessage != null && !disabledTradeMessage.isEmpty()) {
            disabledTradeMessage = ChatColor.translateAlternateColorCodes('&', disabledTradeMessage);
        }
    }

    private void applyToExistingVillagers() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntitiesByClass(Villager.class)) {
                Villager villager = (Villager) entity;
                if (lobotomizeVillagers) {
                    villager.setAI(false);
                }
                if (makeVillagersSilent) {
                    villager.setSilent(true);
                }
                if (makeVillagersInvulnerable) {
                    villager.setInvulnerable(true);
                }
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("shvillagernerf")) {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("shvillagernerf.reload")) {
                    loadConfigOptions();
                    // Ré-appliquer aux villageois existants si certaines options ont changé
                    // Note: si une option comme lobotomizeVillagers passe de true à false,
                    // l'IA ne sera pas restaurée par cette simple logique.
                    // Il faudrait stocker les états initiaux ou redémarrer.
                    applyToExistingVillagers();
                    sender.sendMessage(ChatColor.GREEN + "Configuration de SHVillagerNerf rechargée !");
                } else {
                    sender.sendMessage(ChatColor.RED + "Vous n'avez pas la permission d'exécuter cette commande.");
                }
                return true;
            }
        }
        return false;
    }

    // Empêche la guérison des villageois zombies
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (preventZombieVillagerCuring && event.getRightClicked() instanceof ZombieVillager) {
            ItemStack item = event.getPlayer().getInventory().getItem(event.getHand());
            if (item != null && item.getType() == Material.GOLDEN_APPLE) {
                // Vérifie si le ZombieVillager a l'effet Weakness pour être soigné
                ZombieVillager zv = (ZombieVillager) event.getRightClicked();
                if (zv.hasPotionEffect(org.bukkit.potion.PotionEffectType.WEAKNESS)) {
                    event.setCancelled(true);
                    // Optionnel: envoyer un message au joueur
                    // event.getPlayer().sendMessage(ChatColor.RED + "La guérison des villageois zombies est désactivée.");
                }
            }
        }

        // Désactive le commerce (villageois et marchand ambulant)
        if (disableVillagerTrading && (event.getRightClicked() instanceof Villager || event.getRightClicked() instanceof WanderingTrader)) {
            event.setCancelled(true);
            event.getPlayer().playSound(event.getRightClicked().getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            if (disabledTradeMessage != null && !disabledTradeMessage.isEmpty()) {
                event.getPlayer().sendMessage(disabledTradeMessage);
            }
        }
    }

    @EventHandler
    public void onEntityTransform(EntityTransformEvent event) {
        if (preventZombieVillagerCuring &&
            event.getEntityType() == EntityType.ZOMBIE_VILLAGER &&
            event.getTransformReason() == EntityTransformEvent.TransformReason.CURED) {
            event.setCancelled(true);
        }
    }

    // Lobotomie, silence, invulnérabilité pour les nouveaux villageois
    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (event.getEntity() instanceof Villager) {
            Villager villager = (Villager) event.getEntity();
            if (lobotomizeVillagers) {
                villager.setAI(false);
            }
            if (makeVillagersSilent) {
                villager.setSilent(true);
            }
            if (makeVillagersInvulnerable) {
                villager.setInvulnerable(true);
            }
        }
    }

    // Empêche la reproduction
    @EventHandler
    public void onEntityBreed(EntityBreedEvent event) {
        if (disableVillagerBreeding &&
            (event.getMother() instanceof Villager || event.getFather() instanceof Villager)) {
            event.setCancelled(true);
        }
    }

    // Empêche le ramassage d'objets
    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (preventVillagerItemPickup && event.getEntity() instanceof Villager) {
            event.setCancelled(true);
        }
    }

    // Empêche le changement/acquisition de profession
    @EventHandler
    public void onVillagerCareerChange(VillagerCareerChangeEvent event) {
        if (preventProfessionChange) {
            event.setCancelled(true);
        }
    }

    // Empêche le restockage / nouveaux échanges en montant de niveau
    @EventHandler
    public void onVillagerAcquireTrade(VillagerAcquireTradeEvent event) {
        if (preventTradeRefresh) {
            // Cet événement est déclenché lorsque le villageois acquiert un nouvel échange,
            // ce qui arrive lors du restockage ou du leveling.
            // Les échanges initiaux ne sont pas affectés car ils sont définis à la création/profession.
            event.setCancelled(true);
        }
    }

    // Rend invulnérable (alternative à setInvulnerable pour les dégâts directs)
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (makeVillagersInvulnerable && event.getEntity() instanceof Villager) {
            event.setCancelled(true);
        }
    }

    // Empêche l'ouverture des portes
    @EventHandler
    public void onEntityInteract(EntityInteractEvent event) { // Cet event est pour les entités interagissant avec des blocs
        if (preventVillagerDoorOpening && event.getEntity() instanceof Villager) {
            Material blockType = event.getBlock().getType();
            if (blockType.name().endsWith("_DOOR") && !blockType.name().contains("TRAPDOOR")) {
                event.setCancelled(true);
            }
        }
    }

    // Empêche le spawn de Golems par les villageois
    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (preventGolemSpawningByVillagers &&
            event.getEntityType() == EntityType.IRON_GOLEM &&
            event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.VILLAGE_DEFENSE) {
            event.setCancelled(true);
        }
    }
}
