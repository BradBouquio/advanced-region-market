package net.alex9849.arm.regions;

import net.alex9849.arm.AdvancedRegionMarket;
import net.alex9849.arm.Messages;
import net.alex9849.arm.Permission;
import net.alex9849.arm.entitylimit.EntityLimit;
import net.alex9849.arm.entitylimit.EntityLimitGroup;
import net.alex9849.arm.events.BuyRegionEvent;
import net.alex9849.arm.flaggroups.FlagGroup;
import net.alex9849.arm.limitgroups.LimitGroup;
import net.alex9849.arm.minifeatures.teleporter.Teleporter;
import net.alex9849.arm.regionkind.RegionKind;
import net.alex9849.arm.regions.price.ContractPrice;
import net.alex9849.exceptions.InputException;
import net.alex9849.inter.WGRegion;
import net.alex9849.signs.SignData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class ContractRegion extends CountdownRegion {
    private boolean terminated;

    public ContractRegion(WGRegion region, World regionworld, List<SignData> contractsign, ContractPrice contractPrice, Boolean sold, Boolean inactivityReset,
                          Boolean isHotel, Boolean doBlockReset, RegionKind regionKind, FlagGroup flagGroup, Location teleportLoc, long lastreset, long lastLogin, boolean isUserResettable,
                          long payedTill, Boolean terminated, List<Region> subregions, int allowedSubregions, EntityLimitGroup entityLimitGroup,
                          HashMap<EntityLimit.LimitableEntityType, Integer> extraEntitys, int boughtExtraTotalEntitys) {
        super(region, regionworld, contractsign, contractPrice, sold, inactivityReset, isHotel, doBlockReset, regionKind, flagGroup, teleportLoc, lastreset, lastLogin, isUserResettable,
                payedTill, subregions, allowedSubregions, entityLimitGroup, extraEntitys, boughtExtraTotalEntitys);
        this.terminated = terminated;
        this.updateSigns();
    }

    @Override
    public void regionInfo(CommandSender sender) {
        super.regionInfo(sender);
        List<String> msg;

        if(sender.hasPermission(Permission.ADMIN_INFO)) {
            msg = Messages.REGION_INFO_CONTRACTREGION_ADMIN;
        } else {
            msg = Messages.REGION_INFO_CONTRACTREGION;
        }

        if(this.isSubregion()) {
            msg = Messages.REGION_INFO_CONTRACTREGION_SUBREGION;
        }

        for(String s : msg) {
            sender.sendMessage(this.getConvertedMessage(s));
        }
    }

    @Override
    public void updateRegion() {

        if(this.isSold()){
            GregorianCalendar actualtime = new GregorianCalendar();

            //If region expired and terminated
            if(this.getPayedTill() < actualtime.getTimeInMillis()){
                if(this.isTerminated()) {
                    //TODO logToConsole
                    this.automaticResetRegion(ActionReason.EXPIRED, true);
                } else {
                    List<UUID> owners = this.getRegion().getOwners();
                    if(owners.size() == 0){
                        this.extend();
                    } else {
                        OfflinePlayer oplayer = Bukkit.getOfflinePlayer(owners.get(0));
                        if(oplayer == null) {
                            this.extend();
                        } else {
                            if(AdvancedRegionMarket.getInstance().getEcon().hasAccount(oplayer)) {
                                if(AdvancedRegionMarket.getInstance().getEcon().getBalance(oplayer) < this.getPrice()) {
                                    //TODO logToConsole
                                    this.automaticResetRegion(ActionReason.INSUFFICIENT_MONEY, true);
                                } else {
                                    AdvancedRegionMarket.getInstance().getEcon().withdrawPlayer(oplayer, this.getPrice());
                                    if(this.isSubregion()) {
                                        this.giveParentRegionOwnerMoney(this.getPrice());
                                    }
                                    this.extend();
                                    if(oplayer.isOnline() && AdvancedRegionMarket.getInstance().getPluginSettings().isSendContractRegionExtendMessage()) {
                                        String sendmessage = this.getConvertedMessage(Messages.CONTRACT_REGION_EXTENDED);
                                        oplayer.getPlayer().sendMessage(Messages.PREFIX + sendmessage);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        super.updateRegion();
    }

    @Override
    public void setSold(OfflinePlayer player) {
        this.terminated = false;
        super.setSold(player);
        this.queueSave();
    }

    @Override
    protected void updateSignText(SignData signData) {
        if(this.isSold()){
            String[] lines = new String[4];
            lines[0] = this.getConvertedMessage(Messages.CONTRACT_SOLD_SIGN1);
            lines[1] = this.getConvertedMessage(Messages.CONTRACT_SOLD_SIGN2);
            lines[2] = this.getConvertedMessage(Messages.CONTRACT_SOLD_SIGN3);
            lines[3] = this.getConvertedMessage(Messages.CONTRACT_SOLD_SIGN4);
            signData.writeLines(lines);
        } else {
            String[] lines = new String[4];
            lines[0] = this.getConvertedMessage(Messages.CONTRACT_SIGN1);
            lines[1] = this.getConvertedMessage(Messages.CONTRACT_SIGN2);
            lines[2] = this.getConvertedMessage(Messages.CONTRACT_SIGN3);
            lines[3] = this.getConvertedMessage(Messages.CONTRACT_SIGN4);
            signData.writeLines(lines);
        }
    }

    @Override
    public void buy(Player player) throws InputException {

        if(!Permission.hasAnyBuyPermission(player)) {
            throw new InputException(player, Messages.NO_PERMISSION);
        }
        if(this.isSold()) {
            if(this.getRegion().hasOwner(player.getUniqueId()) || player.hasPermission(Permission.ADMIN_TERMINATE_CONTRACT)) {
                this.changeTerminated(player);
                return;
            } else {
                throw new InputException(player, Messages.REGION_ALREADY_SOLD);
            }
        }
        if(!RegionKind.hasPermission(player, this.getRegionKind())){
            throw new InputException(player, this.getConvertedMessage(Messages.NO_PERMISSIONS_TO_BUY_THIS_KIND_OF_REGION));
        }

        if(!LimitGroup.isCanBuyAnother(player, this)) {
            throw new InputException(player, LimitGroup.getRegionBuyOutOfLimitMessage(player, this.getRegionKind()));
        }
        if(AdvancedRegionMarket.getInstance().getEcon().getBalance(player) < this.getPrice()) {
            throw new InputException(player, Messages.NOT_ENOUGHT_MONEY);
        }
        BuyRegionEvent buyRegionEvent = new BuyRegionEvent(this, player);
        Bukkit.getServer().getPluginManager().callEvent(buyRegionEvent);
        if(buyRegionEvent.isCancelled()) {
            return;
        }
        AdvancedRegionMarket.getInstance().getEcon().withdrawPlayer(player, this.getPrice());
        if(this.isSubregion()) {
            this.giveParentRegionOwnerMoney(this.getPrice());
        }
        this.setSold(player);
        if(AdvancedRegionMarket.getInstance().getPluginSettings().isTeleportAfterContractRegionBought()){
            Teleporter.teleport(player, this, "", AdvancedRegionMarket.getInstance().getConfig().getBoolean("Other.TeleportAfterRegionBoughtCountdown"));
        }
        player.sendMessage(Messages.PREFIX + Messages.REGION_BUYMESSAGE);

    }

    public void changeTerminated(Player player) throws InputException {
        if(this.isTerminated()) {
            if(!LimitGroup.isInLimit(player, this)) {
                throw new InputException(player, LimitGroup.getRegionBuyOutOfLimitMessage(player, this.getRegionKind()));
            } else {
                this.setTerminated(false, player);
            }
        } else {
            this.setTerminated(true, player);
        }
    }

    public void setTerminated(Boolean bool) {
        this.setTerminated(bool, null);
    }

    public void setTerminated(Boolean bool, Player player) {
        this.terminated = bool;
        this.queueSave();
        if(player != null) {
            String sendmessage = this.getConvertedMessage(Messages.CONTRACT_REGION_CHANGE_TERMINATED);
            player.sendMessage(Messages.PREFIX + sendmessage);
        }
    }

    public String getTerminationStringLong(){
        String retMessage;
        if(this.terminated) {
            retMessage = Messages.CONTRACT_REGION_STATUS_TERMINATED_LONG;
        } else {
            retMessage = Messages.CONTRACT_REGION_STATUS_ACTIVE_LONG;
        }
        return retMessage;
    }

    public String getTerminationString(){
        if(this.terminated) {
            return Messages.CONTRACT_REGION_STATUS_TERMINATED;
        } else {
            return Messages.CONTRACT_REGION_STATUS_ACTIVE;
        }
    }

    public boolean isTerminated() {
        return this.terminated;
    }

    public double getPricePerM2PerWeek() {
        if(this.getExtendTime() == 0) {
            return Integer.MAX_VALUE;
        }
        double pricePerM2 = this.getPricePerM2();
        double msPerWeek = 1000 * 60 * 60 * 24 * 7;
        return  (msPerWeek / this.getExtendTime()) * pricePerM2;
    }

    public double getPricePerM3PerWeek() {
        if(this.getExtendTime() == 0) {
            return Integer.MAX_VALUE;
        }
        double pricePerM2 = this.getPricePerM3();
        double msPerWeek = 1000 * 60 * 60 * 24 * 7;
        return  (msPerWeek / this.getExtendTime()) * pricePerM2;
    }

    @Override
    public String getConvertedMessage(String message) {
        message = super.getConvertedMessage(message);
        if(message.contains("%status%")) message = message.replace("%status%", this.getTerminationString());
        if(message.contains("%statuslong%")) message = message.replace("%statuslong%", this.getTerminationStringLong());
        if(message.contains("%isterminated%")) message = message.replace("%isterminated%", Messages.convertYesNo(this.isTerminated()));
        return message;
    }

    public SellType getSellType() {
        return SellType.CONTRACT;
    }

    public ConfigurationSection toConfigurationSection() {
        ConfigurationSection yamlConfiguration = super.toConfigurationSection();
        yamlConfiguration.set("terminated", this.isTerminated());
        return yamlConfiguration;
    }
}
