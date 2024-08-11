package net.crashcraft.crashclaim.commands.claiming;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.aikar.taskchain.TaskChain;
import net.crashcraft.crashclaim.CrashClaim;
import net.crashcraft.crashclaim.claimobjects.Claim;
import net.crashcraft.crashclaim.claimobjects.SubClaim;
import net.crashcraft.crashclaim.commands.claiming.modes.NewClaimMode;
import net.crashcraft.crashclaim.commands.claiming.modes.NewSubClaimMode;
import net.crashcraft.crashclaim.commands.claiming.modes.ResizeClaimMode;
import net.crashcraft.crashclaim.commands.claiming.modes.ResizeSubClaimMode;
import net.crashcraft.crashclaim.config.GlobalConfig;
import net.crashcraft.crashclaim.config.GroupSettings;
import net.crashcraft.crashclaim.data.ClaimDataManager;
import net.crashcraft.crashclaim.data.ErrorType;
import net.crashcraft.crashclaim.localization.Localization;
import net.crashcraft.crashclaim.menus.SubClaimMenu;
import net.crashcraft.crashclaim.menus.permissions.SimplePermissionMenu;
import net.crashcraft.crashclaim.permissions.PermissionHelper;
import net.crashcraft.crashclaim.permissions.PermissionRoute;
import net.crashcraft.crashclaim.visualize.VisualizationManager;
import net.crashcraft.crashclaim.visualize.api.BaseVisual;
import net.crashcraft.crashclaim.visualize.api.VisualGroup;
import net.royawesome.jlibnoise.module.combiner.Max;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashMap;
import java.util.UUID;

import static org.bukkit.Bukkit.getServer;

public class ClaimCommand extends BaseCommand implements Listener {
    private final ClaimDataManager dataManager;
    private final VisualizationManager visualizationManager;
    private final HashMap<UUID, ClickState> modeMap;
    private HashMap<UUID, VerticalSubclaimParams> VerticalSubclaimPreReqs;
    private final HashMap<UUID, ClaimMode> stateMap;
    private final HashMap<UUID, Claim> claimMap;

    public class VerticalSubclaimParams{
        public int maxY;
        public int minY;

        public VerticalSubclaimParams(int _maxY, int _minY){
            maxY = _maxY;
            minY = _minY;
        }

    }

    public ClaimCommand(ClaimDataManager dataManager, VisualizationManager visualizationManager){
        this.dataManager = dataManager;
        this.visualizationManager = visualizationManager;
        this.modeMap = new HashMap<>();
        this.stateMap = new HashMap<>();
        this.claimMap = new HashMap<>();
        this.VerticalSubclaimPreReqs = new HashMap<>();

        Bukkit.getPluginManager().registerEvents(this, CrashClaim.getPlugin());
    }

    @CommandAlias("claim")
    @CommandPermission("crashclaim.user.claim")
    public void claim(Player player){
        UUID uuid = player.getUniqueId();

        if (GlobalConfig.disabled_worlds.contains(player.getWorld().getUID())){
            player.sendMessage(Localization.DISABLED_WORLD.getMessage(player));
            forceCleanup(uuid, true);
            return;
        }

        if (modeMap.containsKey(uuid)) {
            forceCleanup(uuid, true);

            visualizationManager.sendAlert(player, Localization.CLAIM__DISABLED.getMessage(player));
        } else {
            forceCleanup(uuid, true);

            modeMap.put(uuid, ClickState.CLAIM);
            visualizationManager.visualizeSurroundingClaims(player, dataManager);
            visualizationManager.sendAlert(player, Localization.CLAIM__ENABLED.getMessage(player));
            player.spigot().sendMessage(Localization.NEW_CLAIM__INFO.getMessage(player));
        }
    }

    @CommandAlias("subclaim")
    @CommandPermission("crashclaim.user.subclaim")
    public void subClaim(Player player){
        UUID uuid = player.getUniqueId();

        if (GlobalConfig.disabled_worlds.contains(player.getWorld().getUID())){
            player.sendMessage(Localization.DISABLED_WORLD.getMessage(player));
            forceCleanup(uuid, true);
            return;
        }



        if (modeMap.containsKey(uuid)) {
            forceCleanup(uuid, true);
            visualizationManager.sendAlert(player, Localization.SUBCLAIM__DISABLED.getMessage(player));
        } else {
            forceCleanup(uuid, true);

            Location location = player.getLocation();

            Claim claim = dataManager.getClaim(location.getBlockX(), location.getBlockZ(), player.getWorld().getUID());
            if (claim == null) {
                player.spigot().sendMessage(Localization.SUBCLAIM__NO_CLAIM.getMessage(player));
                return;
            }

            if (!PermissionHelper.getPermissionHelper().hasPermission(claim, uuid, PermissionRoute.MODIFY_CLAIM)) {
                player.spigot().sendMessage(Localization.SUBCLAIM__NO_PERMISSION.getMessage(player));
                return;
            }

            if (claim.isEditing()){
                player.spigot().sendMessage(Localization.SUBCLAIM__ALREADY_RESIZING.getMessage(player));
                return;
            }

            claimMap.put(uuid, claim);
            modeMap.put(uuid, ClickState.SUB_CLAIM);

            claim.setEditing(true);
            visualizationManager.visualizeSurroundingSubClaims(claim, player);

            visualizationManager.sendAlert(player, Localization.SUBCLAIM__ENABLED.getMessage(player));
            player.spigot().sendMessage(Localization.NEW_SUBCLAIM__INFO.getMessage(player));
        }
    }

    @CommandAlias("verticalsubclaim|vsubclaim")
    @CommandPermission("crashclaim.user.verticalsubclaim")
    @Syntax("<YMin>|<YMax>")
    public void verticalSubclaim(Player player, @Optional String MinY, @Optional String MaxY){

        UUID uuid = player.getUniqueId();

        int MaxYInt;
        int MinYInt;

        if (GlobalConfig.disabled_worlds.contains(player.getWorld().getUID())){
            player.sendMessage(Localization.DISABLED_WORLD.getMessage(player));
            forceCleanup(uuid, true);
            return;
        }

        if (modeMap.containsKey(uuid)) {
            forceCleanup(uuid, true);
            visualizationManager.sendAlert(player, Localization.SUBCLAIM__DISABLED.getMessage(player));
        } else {
            forceCleanup(uuid, true);
            Location location = player.getLocation();

            if (MinY != null && MaxY != null){
                try {
                    MaxYInt = Integer.parseInt(MaxY);
                    MinYInt = Integer.parseInt(MinY);
                    if (MinYInt > MaxYInt) {
                        player.sendMessage(Localization.VSUBCLAIMRESIZE__MINMAXINVERTED.getMessage(player));
                        return;
                    }

                    if (MaxYInt > 320){
                        player.sendMessage(Localization.VSUBCLAIMRESIZE__YTOOBIG.getMessage(player));
                        return;
                    }

                    if (MinYInt < -64){
                        player.sendMessage(Localization.VSUBCLAIMRESIZE__YTOOSMALL.getMessage(player));
                        return;
                    }

                    if (MaxYInt - MinYInt < 5) {
                        player.sendMessage(Localization.VSUBCLAIMRESIZE__TOOSMALL.getMessage(player));
                        return;
                    }
                    VerticalSubclaimPreReqs.put(uuid, new VerticalSubclaimParams(MaxYInt, MinYInt));

                } catch (NumberFormatException e) {
                    player.sendMessage(Localization.VSUBCLAIMRESIZE__BADPARAMS.getMessage(player));
                    return;
                }
            }

            Claim claim = dataManager.getClaim(location.getBlockX(), location.getBlockZ(), player.getWorld().getUID());
            if (claim == null) {
                player.spigot().sendMessage(Localization.SUBCLAIM__NO_CLAIM.getMessage(player));
                return;
            }

            if (!PermissionHelper.getPermissionHelper().hasPermission(claim, uuid, PermissionRoute.MODIFY_CLAIM)) {
                player.spigot().sendMessage(Localization.SUBCLAIM__NO_PERMISSION.getMessage(player));
                return;
            }

            if (claim.isEditing()){
                player.spigot().sendMessage(Localization.SUBCLAIM__ALREADY_RESIZING.getMessage(player));
                return;
            }

            claimMap.put(uuid, claim);
            modeMap.put(uuid, ClickState.V_SUB_CLAIM);

            claim.setEditing(true);
            visualizationManager.visualizeSurroundingSubClaims(claim, player);

            visualizationManager.sendAlert(player, Localization.SUBCLAIM__ENABLED.getMessage(player));
            player.spigot().sendMessage(Localization.NEW_SUBCLAIM__INFO.getMessage(player));
        }
    }

    @CommandAlias("trustsubclaim|subclaimtrust")
    @CommandPermission("crashclaim.user.subclaimtrust")
    @Syntax("<player>")
    @CommandCompletion("@Players")
    public void subclaimTrust(CommandSender sender, @Optional String Trustee){
        if (!(sender instanceof Player player)){
            return;
        }
        if (Trustee == null){
            player.sendMessage(Localization.SUBCLAIMTRUST__SPECIFY_PLAYER.getMessage(player)); //TODO tell user to specify player
            return;
        }

        UUID target = null;
        Player playerTarget = getServer().getPlayer(Trustee);
        OfflinePlayer offlineTarget = getServer().getOfflinePlayer(Trustee);

        if (playerTarget != null) {
            target = playerTarget.getUniqueId();
        }

        else if (offlineTarget.hasPlayedBefore()) {
            target = offlineTarget.getUniqueId();
        }

        else{
            player.sendMessage(Localization.SUBCLAIMTRUST__INVALID_PLAYER.getMessage(player)); //TODO tell user its an invalid player
            return;
        }

        Location location = player.getLocation();
        Claim claim = dataManager.getClaim(location.getBlockX(), location.getBlockZ(), player.getWorld().getUID());
        if (claim == null){
            player.sendMessage(Localization.SUBCLAIMTRUST__NOT_IN_SUBCLAIM.getMessage(player)); //TODO tell user they are not in a subclaim
            return;
        }

        if (claim.getOwner().equals(target)){
            player.sendMessage(Localization.SUBCLAIMTRUST__TARGET_IS_CLAIM_OWNER.getMessage(player)); //TODO tell user they cant mod claims of the owner
            return;
        }

        SubClaim subClaim = claim.getSubClaim(location.getBlockX(), location.getBlockZ(), location.getBlockY());
        if (subClaim == null){
            player.sendMessage(Localization.SUBCLAIMTRUST__NOT_IN_SUBCLAIM.getMessage(player)); //TODO tell user they are not in a subclaim
            return;
        }

        if (!PermissionHelper.getPermissionHelper().hasPermission(
            subClaim,
            player.getUniqueId(),
            PermissionRoute.MODIFY_PERMISSIONS
        )) {
            player.sendMessage(Localization.SUBCLAIMTRUST__NO_PERMISSION.getMessage(player)); //TODO tell user they have no perms
            return;
        }

        new SimplePermissionMenu(player, subClaim, target, null).open();

    }

    @CommandAlias("subclaimsettings")
    @CommandPermission("crashclaim.user.subclaimsettings")
    public void subclaimTrust(CommandSender sender){
        if (!(sender instanceof Player player)){
            return;
        }


        Location location = player.getLocation();
        Claim claim = dataManager.getClaim(location.getBlockX(), location.getBlockZ(), player.getWorld().getUID());
        if (claim == null){
            player.sendMessage(Localization.SUBCLAIMTRUST__NOT_IN_SUBCLAIM.getMessage(player)); //TODO tell user they are not in a subclaim
            return;
        }


        SubClaim subClaim = claim.getSubClaim(location.getBlockX(), location.getBlockZ(), location.getBlockY());
        if (subClaim == null){
            player.sendMessage(Localization.SUBCLAIMTRUST__NOT_IN_SUBCLAIM.getMessage(player)); //TODO tell user they are not in a subclaim
            return;
        }

        if (!PermissionHelper.getPermissionHelper().hasPermission(
            subClaim,
            player.getUniqueId(),
            PermissionRoute.MODIFY_PERMISSIONS
        )) {
            player.sendMessage(Localization.SUBCLAIMTRUST__NO_PERMISSION.getMessage(player)); //TODO tell user they have no perms
            return;
        }

        new SubClaimMenu(player, subClaim).open();

    }

    @CommandAlias("converttoverticalsubclaim|ctvs|resizeverticalsubclaim")
    @CommandPermission("crashclaim.user.subclaimconvert")
    @Syntax("<YMin>|<YMax>")
    public void ConvertToVerticalSubClaim(CommandSender sender, @Optional String YMin, @Optional String YMax) {
        if (!(sender instanceof Player player)) {
            return;
        }
        if (YMin == null || YMax == null) {
            player.sendMessage(Localization.VSUBCLAIMRESIZE__NOPARAMS.getMessage(player));
            return;
        }
        int YMaxInt;
        int YMinInt;

        try {
            YMaxInt = Integer.parseInt(YMax);
            YMinInt = Integer.parseInt(YMin);
        } catch (NumberFormatException e) {
            player.sendMessage(Localization.VSUBCLAIMRESIZE__BADPARAMS.getMessage(player));
            return;
        }

        if (YMinInt > YMaxInt) {
            player.sendMessage(Localization.VSUBCLAIMRESIZE__MINMAXINVERTED.getMessage(player));
            return;
        }

        if (YMaxInt > 320){
            player.sendMessage(Localization.VSUBCLAIMRESIZE__YTOOBIG.getMessage(player));
            return;
        }

        if (YMinInt < -64){
            player.sendMessage(Localization.VSUBCLAIMRESIZE__YTOOSMALL.getMessage(player));
            return;
        }

        if (YMaxInt - YMinInt < 5) {
            player.sendMessage(Localization.VSUBCLAIMRESIZE__TOOSMALL.getMessage(player));
            return;
        }


        Location location = player.getLocation();
        Claim claim = dataManager.getClaim(location.getBlockX(), location.getBlockZ(), player.getWorld().getUID());
        if (claim == null) {
            player.sendMessage(Localization.SUBCLAIMTRUST__NOT_IN_SUBCLAIM.getMessage(player));
            return;
        }

        SubClaim subClaim = claim.getSubClaim(location.getBlockX(), location.getBlockZ(), location.getBlockY());
        if (subClaim == null) {
            player.sendMessage(Localization.SUBCLAIMTRUST__NOT_IN_SUBCLAIM.getMessage(player));
            return;
        }

        if (!PermissionHelper.getPermissionHelper().hasPermission(
                subClaim,
                player.getUniqueId(),
                PermissionRoute.MODIFY_PERMISSIONS
        )) {
            player.sendMessage(Localization.SUBCLAIMTRUST__NO_PERMISSION.getMessage(player));
            return;
        }
        ErrorType error = dataManager.SetVerticalSize(subClaim, YMinInt, YMaxInt);
        switch (error) {
            case OVERLAP_EXISTING_SUBCLAIM:
                player.spigot().sendMessage(Localization.RESIZE_SUBCLAIM__NO_OVERLAP.getMessage(player));
                return;
            case TOO_SMALL:
                player.spigot().sendMessage(Localization.RESIZE_SUBCLAIM__MIN_SIZE.getMessage(player));
                return;
            case CANNOT_FLIP_ON_RESIZE:
                player.spigot().sendMessage(Localization.RESIZE_SUBCLAIM__CANNOT_FLIP.getMessage(player));
                return;
            case VERTICAL_SUBCLAIM_TOO_SMALL:
                player.spigot().sendMessage(Localization.NEW_VERTICAL_SUBCLAIM__MIN_AREA.getMessage(player));
                return;
            case NONE:
                player.spigot().sendMessage(Localization.VSUBCLAIMRESIZE__SUCCESS.getMessage(player));
                VisualGroup group = visualizationManager.fetchVisualGroup(player, true);
                visualizationManager.visualizeSurroundingSubClaims(claim, player);

                for (BaseVisual visual : group.getActiveVisuals()) {
                    visualizationManager.deSpawnAfter(visual, 5);
                }


        }

    }

    @EventHandler
    public void onClick(PlayerInteractEvent e){
        if (e.getHand() == null
                || !e.getHand().equals(EquipmentSlot.HAND)
                || e.getClickedBlock() == null){
            return;
        }

        click(e.getPlayer(), e.getClickedBlock().getLocation());
    }

    public void click(Player player, Location location) {
        UUID uuid = player.getUniqueId();

        if (stateMap.containsKey(uuid)){
            if (GlobalConfig.disabled_worlds.contains(player.getWorld().getUID())){
                player.sendMessage(Localization.DISABLED_WORLD.getMessage(player));
                forceCleanup(player.getUniqueId(), true);
                return;
            }

            stateMap.get(uuid).click(player, location);
            return;
        }
        if (modeMap.containsKey(uuid)){
            if (GlobalConfig.disabled_worlds.contains(player.getWorld().getUID())){
                player.sendMessage(Localization.DISABLED_WORLD.getMessage(player));
                forceCleanup(player.getUniqueId(), true);
                return;
            }
            ClickState state = modeMap.get(uuid);

            switch (state) {
                case CLAIM -> {
                    Claim claim = dataManager.getClaim(location);
                    if (claim == null) {
                        TaskChain<?> chain = CrashClaim.newChain();
                        chain.asyncFirst(() -> {
                            final int alreadyClaimed = dataManager.getNumberOwnedParentClaims(uuid);
                            final GroupSettings groupSettings = CrashClaim.getPlugin().getPluginSupport().getPlayerGroupSettings(player);

                            if (groupSettings.getMaxClaims() == -1) {
                                return true;
                            }

                            return alreadyClaimed < groupSettings.getMaxClaims();
                        }).syncLast((canClaim) -> {
                            if (!canClaim) {
                                player.sendMessage(Localization.MAX_CLAIMS_REACHED.getMessage(player));
                                forceCleanup(uuid, true);
                                return;
                            }

                            stateMap.put(uuid, new NewClaimMode(this, player, location));
                        });
                        chain.execute();
                    } else {
                        stateMap.put(uuid, new ResizeClaimMode(this, player, claim, location));
                    }
                }
                case SUB_CLAIM, V_SUB_CLAIM -> {
                    Claim parent = claimMap.get(uuid);
                    if (parent == null) {
                        return;
                    }
                    parent.setEditing(true);
                    SubClaim subClaim = parent.getSubClaim(location.getBlockX(), location.getBlockZ(), location.getBlockY());
                    if (subClaim != null && VerticalSubclaimPreReqs.get(uuid) == null) {
                        if (!PermissionHelper.getPermissionHelper().hasPermission(subClaim, uuid, PermissionRoute.MODIFY_CLAIM)) {
                            player.spigot().sendMessage(Localization.SUBCLAIM__NO_PERMISSION.getMessage(player));
                            return;
                        }

                        return;
                    }
                    stateMap.put(uuid, new NewSubClaimMode(this, player, parent, location, state, VerticalSubclaimPreReqs.get(uuid)));
                }
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e){
        forceCleanup(e.getPlayer().getUniqueId(), true);
    }

    public void forceCleanup(UUID uuid, boolean visuals){
        Claim claim = claimMap.get(uuid);
        if (claim != null){
            claim.setEditing(false);
        }

        modeMap.remove(uuid);
        stateMap.remove(uuid);
        if (VerticalSubclaimPreReqs.get(uuid) != null){
            VerticalSubclaimPreReqs.remove(uuid);
        }


        if (visuals){
            VisualGroup group = visualizationManager.fetchExistingGroup(uuid);
            if (group != null){
                group.removeAllVisuals();
            }
        }
    }

    public ClaimDataManager getDataManager() {
        return dataManager;
    }

    public VisualizationManager getVisualizationManager() {
        return visualizationManager;
    }
}
