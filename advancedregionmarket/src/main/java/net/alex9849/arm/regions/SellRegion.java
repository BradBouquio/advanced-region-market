package net.alex9849.arm.regions;

import net.alex9849.arm.AdvancedRegionMarket;
import net.alex9849.arm.Messages;
import net.alex9849.arm.Permission;
import net.alex9849.arm.events.BuyRegionEvent;
import net.alex9849.arm.exceptions.*;
import net.alex9849.arm.minifeatures.teleporter.Teleporter;
import net.alex9849.arm.regionkind.RegionKind;
import net.alex9849.arm.regions.price.Autoprice.AutoPrice;
import net.alex9849.arm.regions.price.Price;
import net.alex9849.inter.WGRegion;
import net.alex9849.signs.SignData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class SellRegion extends Region {
    private Price price;


    public SellRegion(WGRegion region, List<SignData> sellsigns, Price price, boolean sold, Region parentRegion) {
        super(region, sellsigns, sold, parentRegion);
        this.price = price;
    }

    public SellRegion(WGRegion region, World regionworld, List<SignData> sellsigns, Price price, boolean sold) {
        super(region, regionworld, sellsigns, sold);
        this.price = price;
    }

    @Override
    public void signClickAction(Player player) throws OutOfLimitExeption, AlreadySoldException, NotEnoughMoneyException, NoPermissionException {
        this.buy(player);
    }

    @Override
    public void buy(Player player) throws NoPermissionException, OutOfLimitExeption, NotEnoughMoneyException, AlreadySoldException {

        if (!player.hasPermission(Permission.MEMBER_BUY)) {
            throw new NoPermissionException(this.replaceVariables(Messages.NO_PERMISSION));
        }
        if (this.isSold()) {
            throw new AlreadySoldException(this.replaceVariables(Messages.REGION_ALREADY_SOLD));
        }
        if (!RegionKind.hasPermission(player, this.getRegionKind())) {
            throw new NoPermissionException(this.replaceVariables(Messages.NO_PERMISSIONS_TO_BUY_THIS_KIND_OF_REGION));
        }

        if (!AdvancedRegionMarket.getInstance().getLimitGroupManager().isCanBuyAnother(player, this)) {
            throw new OutOfLimitExeption(AdvancedRegionMarket.getInstance().getLimitGroupManager()
                    .getRegionBuyOutOfLimitMessage(player, this.getRegionKind()));
        }

        if (AdvancedRegionMarket.getInstance().getEcon().getBalance(player) < this.getPricePerPeriod()) {
            throw new NotEnoughMoneyException(this.replaceVariables(Messages.NOT_ENOUGH_MONEY));
        }
        BuyRegionEvent buyRegionEvent = new BuyRegionEvent(this, player);
        Bukkit.getServer().getPluginManager().callEvent(buyRegionEvent);
        if (buyRegionEvent.isCancelled()) {
            return;
        }

        AdvancedRegionMarket.getInstance().getEcon().withdrawPlayer(player, this.getPricePerPeriod());
        this.giveParentRegionOwnerMoney(this.getPricePerPeriod());
        this.setSold(player);
        if (AdvancedRegionMarket.getInstance().getPluginSettings().isTeleportAfterSellRegionBought()) {
            try {
                Teleporter.teleport(player, this, "", AdvancedRegionMarket.getInstance().getConfig().getBoolean("Other.TeleportAfterRegionBoughtCountdown"));
            } catch (NoSaveLocationException e) {
                player.sendMessage(Messages.PREFIX + this.replaceVariables(Messages.TELEPORTER_NO_SAVE_LOCATION_FOUND));
            }
        }
        player.sendMessage(Messages.PREFIX + Messages.REGION_BUYMESSAGE);
    }

    @Override
    public Price getPriceObject() {
        return this.price;
    }

    @Override
    public void setSold(OfflinePlayer player) {
        this.setSold(true);
        this.getRegion().deleteMembers();
        this.getRegion().setOwner(player);
        this.setLastLogin();

        this.updateSigns();
        this.queueSave();
    }

    @Override
    public void regionInfo(CommandSender sender) {
        super.regionInfo(sender);
        List<String> msg;

        if (sender.hasPermission(Permission.ADMIN_INFO)) {
            msg = Messages.REGION_INFO_SELLREGION_ADMIN;
        } else {
            msg = Messages.REGION_INFO_SELLREGION;
        }

        if (this.isSubregion()) {
            msg = Messages.REGION_INFO_SELLREGION_SUBREGION;
        }

        for (String s : msg) {
            sender.sendMessage(this.replaceVariables(s));
        }
    }

    public void setSellPrice(Price price) {
        this.price = price;
        this.queueSave();
        this.updateSigns();
    }

    @Override
    public double getPricePerM2PerWeek() {
        return this.getPricePerM2();
    }

    @Override
    public double getPricePerM3PerWeek() {
        return this.getPricePerM2();
    }

    @Override
    public double getPaybackMoney() {
        double money = (this.getPricePerPeriod() * this.getPaybackPercentage()) / 100;
        if (money > 0) {
            return money;
        } else {
            return 0;
        }
    }

    @Override
    protected void updateSignText(SignData signData) {
        if (this.isSold()) {
            String[] lines = new String[4];
            lines[0] = this.replaceVariables(Messages.SOLD_SIGN1);
            lines[1] = this.replaceVariables(Messages.SOLD_SIGN2);
            lines[2] = this.replaceVariables(Messages.SOLD_SIGN3);
            lines[3] = this.replaceVariables(Messages.SOLD_SIGN4);
            signData.writeLines(lines);
        } else {
            String[] lines = new String[4];
            lines[0] = this.replaceVariables(Messages.SELL_SIGN1);
            lines[1] = this.replaceVariables(Messages.SELL_SIGN2);
            lines[2] = this.replaceVariables(Messages.SELL_SIGN3);
            lines[3] = this.replaceVariables(Messages.SELL_SIGN4);
            signData.writeLines(lines);
        }

    }

    public SellType getSellType() {
        return SellType.SELL;
    }

    @Override
    public void setAutoPrice(AutoPrice autoPrice) {
        this.price = new Price(autoPrice);
    }
}
